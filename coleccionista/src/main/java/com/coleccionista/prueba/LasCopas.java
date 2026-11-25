package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.mapa.Final;
import com.coleccionista.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * La final: Las copas. Las últimas piezas (2 a 4) alrededor de la mesa redonda de la Cámara final. Sobre la mesa hay copas (dos más
 * que piezas) y solo una está envenenada. Por turnos, cada pieza bebe una copa (clic derecho). Quien bebe el veneno queda
 * en la colección; si quedan varios, el Coleccionista vuelve a servir. La última pieza en pie gana: "la última pieza".
 */
public final class LasCopas {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":copas";
    public static final String PRUEBA = "Las copas";
    public static int segundosTurno = 25;

    public enum Fase {
        NADA, LISTA, HABLANDO, TURNO, BEBE, SIRVE, FIN
    }

    private static Fase fase = Fase.NADA;
    private static final List<UUID> orden = new ArrayList<>();
    private static final List<BlockPos> copas = new ArrayList<>();
    private static int turno, veneno = -1, bebida = -1, ronda;
    private static UUID quienBebe;
    private static long t, hasta, tpEn = -1, presentarEn = -1, botEn = -1;
    private static boolean cambio;

    private LasCopas() {
    }

    public static void init() {
        AxelisCore.onServerTick(LasCopas::tick);
        AxelisCore.onLogin(p -> {
            if (fase != Fase.NADA) {
                PlayerStore.Sync.toPlayer(p, KEY, estado(Plat.server(p)));
            }
        });
    }

    public static Fase fase() {
        return fase;
    }

    // ------------------------------------------------------------------ preparar

    /** ¿La prueba está abierta (preparada, en juego o terminada sin limpiar)? */
    public static boolean abierta() {
        return fase != Fase.NADA;
    }

    public static Component preparar(MinecraftServer s) {
        if (!Complejo.construido(s, "final")) {
            return Component.translatable(MOD + ".cop.sin_mapa");
        }
        if (fase != Fase.NADA && fase != Fase.LISTA && fase != Fase.FIN) {
            return Component.translatable(MOD + ".cop.ya_empezo");
        }
        Evento ev = Evento.get(s);
        List<Pieza> v = ev.vivos();
        if (v.size() < 2) {
            return Component.translatable(MOD + ".enc.pocos");
        }
        if (v.size() > Final.LUGARES) {
            return Component.translatable(MOD + ".cop.muchos", v.size(), Final.LUGARES);
        }
        Pruebas.cerrarOtras(s, "copas");
        limpiar(s);
        Collections.shuffle(v, new Random(s.overworld().getRandom().nextLong()));
        for (Pieza p : v) {
            orden.add(p.id);
            p.equipo = -1;
        }
        ev.cambio(s);
        ev.prueba(s, PRUEBA);
        ev.reloj(s, 0);
        servir(s, false);
        Pantallas.modo(s, "estatica", "");
        fase = Fase.LISTA;
        long now = s.overworld().getGameTime();
        Escena.fundido(s, MOD + ".cop.fundido", 90);
        tpEn = now + 22;
        presentarEn = now + 100;
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_COPAS preparada piezas={}", orden.size());
        return null;
    }

    private static void aLaMesa(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (int i = 0; i < orden.size(); i++) {
            Pieza p = ev.pieza(orden.get(i));
            Entity e = p == null ? null : Evento.entidad(s, p);
            String m = "fin.lugar." + i;
            BlockPos b = Complejo.pos(s, m);
            if (e == null || b == null) {
                continue;
            }
            float yaw = Complejo.yaw(m);
            if (e instanceof ServerPlayer sp) {
                if (sp.isSleeping()) {
                    sp.stopSleeping();
                }
                sp.teleportTo(s.overworld(), b.getX() + 0.5, b.getY(), b.getZ() + 0.5, yaw, 20F);
                sp.setGameMode(GameType.ADVENTURE);
            } else {
                e.moveTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, yaw, 0F);
                e.setYHeadRot(yaw);
            }
        }
    }

    /** Pone las copas en la mesa (dos más que piezas vivas) y envenena una al azar. */
    private static void servir(MinecraftServer s, boolean sonido) {
        ServerLevel l = s.overworld();
        quitarCopas(s);
        int vivos = 0;
        Evento ev = Evento.get(s);
        for (UUID id : orden) {
            Pieza p = ev.pieza(id);
            if (p != null && p.vivo()) {
                vivos++;
            }
        }
        int n = Math.min(Final.COPAS, Math.max(3, vivos + 2));
        List<Integer> lugares = new ArrayList<>();
        for (int i = 0; i < Final.COPAS; i++) {
            lugares.add(i);
        }
        Random r = new Random(l.getRandom().nextLong());
        Collections.shuffle(lugares, r);
        for (int i = 0; i < n; i++) {
            BlockPos b = Complejo.pos(s, "fin.copa." + lugares.get(i));
            if (b != null) {
                l.setBlock(b, ModRegistry.COPA.get().defaultBlockState(), 3);
                copas.add(b);
                if (sonido) {
                    l.sendParticles(ParticleTypes.SMOKE, b.getX() + 0.5, b.getY() + 0.6, b.getZ() + 0.5, 4, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }
        veneno = r.nextInt(Math.max(1, copas.size()));
        if (sonido) {
            Anuncio.sonido(s, SoundEvents.BOTTLE_FILL, 1F, 0.6F);
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_COPAS servidas={} ronda={}", copas.size(), ronda);
    }

    private static void quitarCopas(MinecraftServer s) {
        ServerLevel l = s.overworld();
        for (BlockPos b : copas) {
            if (l.getBlockState(b).is(ModRegistry.COPA.get())) {
                l.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        copas.clear();
        veneno = -1;
    }

    // ------------------------------------------------------------------ turnos

    public static boolean empezar(MinecraftServer s, boolean conDiscurso) {
        if (fase != Fase.LISTA) {
            return false;
        }
        if (conDiscurso) {
            fase = Fase.HABLANDO;
            cambio = true;
            Pantallas.hablar(s, "copas", () -> {
                if (fase == Fase.HABLANDO) {
                    primerTurno(s);
                }
            });
        } else {
            Pantallas.callar(s);
            primerTurno(s);
        }
        return true;
    }

    private static void primerTurno(MinecraftServer s) {
        turno = -1;
        ronda = 1;
        siguienteTurno(s);
    }

    private static boolean vivo(Evento ev, UUID id) {
        Pieza p = ev.pieza(id);
        return p != null && p.vivo();
    }

    private static void siguienteTurno(MinecraftServer s) {
        Evento ev = Evento.get(s);
        if (ev.vivos().size() <= 1 || ev.fase() == Evento.Fase.FIN) {
            fin(s);
            return;
        }
        for (int i = 0; i < orden.size(); i++) {
            turno = (turno + 1) % orden.size();
            if (vivo(ev, orden.get(turno))) {
                break;
            }
        }
        fase = Fase.TURNO;
        UUID id = orden.get(turno);
        Pieza p = ev.pieza(id);
        ev.reloj(s, segundosTurno);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".cop.pantalla.turno", p == null ? "?" : p.etiqueta()).getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".cop.chat.turno", p == null ? Component.literal("?")
                : Anuncio.nombre(p))));
        Anuncio.sonido(s, SoundEvents.NOTE_BLOCK_BELL.get(), 0.8F, 0.6F);
        botEn = p != null && p.bot ? s.overworld().getGameTime() + 50 + s.overworld().getRandom().nextInt(60) : -1;
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_COPAS turno {}", p == null ? "?" : p.etiqueta());
    }

    /** Clic derecho en una copa: si es tu turno, la bebes. */
    public static void beber(ServerPlayer sp, BlockPos pos) {
        if (fase != Fase.TURNO || turno < 0 || !orden.get(turno).equals(sp.getUUID())) {
            if (fase == Fase.TURNO) {
                Plat.msg(sp, Component.translatable(MOD + ".cop.no_turno").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        int i = copas.indexOf(pos);
        if (i >= 0) {
            beberCopa(Plat.server(sp), sp.getUUID(), i);
        }
    }

    private static void beberCopa(MinecraftServer s, UUID id, int i) {
        ServerLevel l = s.overworld();
        BlockPos b = copas.get(i);
        l.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
        l.playSound(null, b, SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.2F, 0.8F);
        bebida = i;
        quienBebe = id;
        fase = Fase.BEBE;
        hasta = t + 70;
        Evento.get(s).reloj(s, 0);
        Pieza p = Evento.get(s).pieza(id);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".cop.pantalla.bebio", p == null ? "?" : p.etiqueta()).getString());
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_COPAS bebe {} copa={}", p == null ? "?" : p.etiqueta(), i);
    }

    /** Se acabó su tiempo: el Coleccionista elige la copa por él. */
    private static void copaAlAzar(MinecraftServer s, boolean avisar) {
        List<Integer> quedan = new ArrayList<>();
        for (int i = 0; i < copas.size(); i++) {
            if (s.overworld().getBlockState(copas.get(i)).is(ModRegistry.COPA.get())) {
                quedan.add(i);
            }
        }
        if (quedan.isEmpty()) {
            servir(s, true);
            for (int i = 0; i < copas.size(); i++) {
                quedan.add(i);
            }
        }
        UUID id = orden.get(turno);
        if (avisar) {
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".cop.chat.elijo")));
        }
        beberCopa(s, id, quedan.get(s.overworld().getRandom().nextInt(quedan.size())));
    }

    private static void resultado(MinecraftServer s) {
        Evento ev = Evento.get(s);
        Pieza p = ev.pieza(quienBebe);
        boolean muere = bebida == veneno;
        Entity e = p == null ? null : Evento.entidad(s, p);
        if (muere && p != null) {
            Coleccionista.LOGGER.info("COLECCIONISTA_COPAS veneno {}", p.etiqueta());
            if (e != null) {
                s.overworld().sendParticles(ParticleTypes.ENTITY_EFFECT, e.getX(), e.getY() + 1, e.getZ(), 30, 0.3, 0.5, 0.3, 1);
            }
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".cop.chat.veneno", Anuncio.nombre(p))));
            ev.eliminar(s, p, PRUEBA);
            if (ev.vivos().size() <= 1) {
                fin(s);
                return;
            }
            // el Coleccionista vuelve a servir
            ronda++;
            fase = Fase.SIRVE;
            hasta = t + 80;
            cambio = true;
            return;
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_COPAS a_salvo {}", p == null ? "?" : p.etiqueta());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".cop.chat.salvo", p == null ? Component.literal("?")
                : Anuncio.nombre(p))));
        Anuncio.sonido(s, SoundEvents.AMETHYST_BLOCK_CHIME, 1F, 1F);
        siguienteTurno(s);
    }

    private static void fin(MinecraftServer s) {
        fase = Fase.FIN;
        quitarCopas(s);
        Evento ev = Evento.get(s);
        ev.reloj(s, 0);
        Pantallas.modo(s, "coleccionista", "");
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_COPAS fin vivos={}", ev.vivos().size());
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        if (fase == Fase.TURNO) {
            copaAlAzar(s, true);
            return true;
        }
        return false;
    }

    private static void tick(MinecraftServer s) {
        if (fase == Fase.NADA) {
            return;
        }
        long now = s.overworld().getGameTime();
        if (tpEn > 0 && now >= tpEn) {
            tpEn = -1;
            aLaMesa(s);
        }
        if (presentarEn > 0 && now >= presentarEn) {
            presentarEn = -1;
            Escena.presentacion(s, "VIII", MOD + ".cop.titulo", MOD + ".cop.lema");
            Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.4F);
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        t++;
        if (fase == Fase.TURNO && botEn > 0 && now >= botEn) {
            botEn = -1;
            copaAlAzar(s, false);
        } else if (fase == Fase.BEBE) {
            if ((hasta - t) % 14 == 0 && hasta - t > 0) {
                Anuncio.sonido(s, SoundEvents.WARDEN_HEARTBEAT, 1F, 1F);
            }
            if (t >= hasta) {
                resultado(s);
            }
        } else if (fase == Fase.SIRVE && t >= hasta) {
            servir(s, true);
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".cop.chat.sirve")));
            siguienteTurno(s);
        }
        if (cambio || now % 40 == 0) {
            enviar(s);
        }
    }

    // ------------------------------------------------------------------ terminar

    /** El organizador (o la prueba automática) hace que beba quien tiene el turno (una copa al azar). */
    public static boolean beberYa(MinecraftServer s) {
        if (fase != Fase.TURNO) {
            return false;
        }
        copaAlAzar(s, false);
        return true;
    }

    public static void terminar(MinecraftServer s) {
        if (fase == Fase.HABLANDO) {
            Pantallas.callar(s);
        }
        if (Complejo.construido(s, "final")) {
            quitarCopas(s);
        }
        limpiar(s);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_COPAS terminada");
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase != Fase.NADA) {
            terminar(s);
        }
    }

    private static void limpiar(MinecraftServer s) {
        fase = Fase.NADA;
        orden.clear();
        copas.clear();
        turno = -1;
        veneno = -1;
        bebida = -1;
        ronda = 0;
        quienBebe = null;
        tpEn = -1;
        presentarEn = -1;
        botEn = -1;
        t = 0;
    }

    // ------------------------------------------------------------------ a los clientes

    public static CompoundTag estado(MinecraftServer s) {
        CompoundTag c = new CompoundTag();
        c.putString("fase", fase.name());
        c.putInt("ronda", ronda);
        if (turno >= 0 && turno < orden.size()) {
            c.putUUID("turno", orden.get(turno));
            Pieza p = Evento.get(s).pieza(orden.get(turno));
            c.putString("turno_nombre", p == null ? "?" : "Pieza " + p.etiqueta() + " · " + p.nombre);
        }
        int quedan = 0;
        for (BlockPos b : copas) {
            if (s.overworld().getBlockState(b).is(ModRegistry.COPA.get())) {
                quedan++;
            }
        }
        c.putInt("copas", quedan);
        if (quienBebe != null) {
            Pieza p = Evento.get(s).pieza(quienBebe);
            c.putString("bebe_nombre", p == null ? "?" : "Pieza " + p.etiqueta() + " · " + p.nombre);
        }
        return c;
    }

    private static void enviar(MinecraftServer s) {
        cambio = false;
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }
}
