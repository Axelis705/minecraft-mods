package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Plat;
import com.axeliscore.ui.CoreScreens;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.mapa.Salon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Prueba 3: Compartir o robar. Parejas al azar, frente a frente en una mesa del Salón. Primero hablan (un minuto, por voz) y luego cada
 * uno elige en secreto: COMPARTIR o ROBAR.
 * <ul>
 *     <li>Los dos comparten: siguen los dos.</li>
 *     <li>Uno roba: sigue el que robó; el otro queda en la colección.</li>
 *     <li>Los dos roban: quedan los dos en la colección.</li>
 * </ul>
 * Quien no elige, comparte. Si son impares, uno al azar queda perdonado (se sienta junto a la pantalla). Las mesas se revelan una por una
 * en la pantalla gigante y en la pantalla de todos (cartas que se voltean), para que se vea bien en la transmisión.
 */
public final class Compartir {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":compartir";
    /** A cada jugador: si le toca elegir y qué eligió. */
    public static final String KEY_MIA = MOD + ":compartir_mia";
    public static final String ACCION = MOD + ":eleccion";
    public static final String PANTALLA = MOD + ":eleccion";
    public static final String PRUEBA = "Compartir o robar";
    public static int segundosHablar = 60, segundosElegir = 20;
    /** Ticks que dura cada revelación y cuándo se aplica el resultado (se puede acelerar para las pruebas). */
    public static int REVELA = 150, RESULTADO = 95;
    /** ¿Los bots eligen solos? (las pruebas automáticas lo apagan para decidir ellas) */
    public static boolean botsEligen = true;

    public enum Fase {
        NADA, LISTA, HABLANDO_COLECCIONISTA, HABLAR, ELEGIR, REVELAR, FIN
    }

    /** Una mesa: sus dos piezas y lo que eligió cada una (null = todavía no; true = robar). */
    static final class Mesa {
        final int n;
        final UUID a, b;
        Boolean ra, rb;
        boolean revelada;

        Mesa(int n, UUID a, UUID b) {
            this.n = n;
            this.a = a;
            this.b = b;
        }
    }

    private static Fase fase = Fase.NADA;
    private static final List<Mesa> mesas = new ArrayList<>();
    private static UUID perdonado;
    private static int revelando = -1;
    private static long t, revelaDesde, tpEn = -1, presentarEn = -1;
    private static final Map<UUID, Long> botDecide = new HashMap<>();
    private static int eliminados;
    private static boolean cambio;

    private Compartir() {
    }

    public static void init() {
        AxelisCore.onServerTick(Compartir::tick);
        AxelisCore.onLogin(p -> {
            if (fase != Fase.NADA) {
                PlayerStore.Sync.toPlayer(p, KEY, estado(Plat.server(p)));
                mia(p);
            }
        });
        Actions.on(ACCION, Compartir::elegir);
    }

    public static Fase fase() {
        return fase;
    }

    // ------------------------------------------------------------------ preparar

    public static Component preparar(MinecraftServer s) {
        if (!Complejo.construido(s, "salon")) {
            return Component.translatable(MOD + ".com.sin_mapa");
        }
        if (fase != Fase.NADA && fase != Fase.LISTA && fase != Fase.FIN) {
            return Component.translatable(MOD + ".com.ya_empezo");
        }
        Evento ev = Evento.get(s);
        List<Pieza> v = ev.vivos();
        if (v.size() < 2) {
            return Component.translatable(MOD + ".enc.pocos");
        }
        limpiar(s);
        Collections.shuffle(v, new Random(s.overworld().getRandom().nextLong()));
        if (v.size() % 2 == 1) {
            perdonado = v.remove(v.size() - 1).id;
        }
        // las parejas de esta prueba son nuevas (al azar) y se guardan como equipos para el HUD
        for (int i = 0; i + 1 < v.size() && mesas.size() < Salon.MESAS; i += 2) {
            Mesa m = new Mesa(mesas.size() + 1, v.get(i).id, v.get(i + 1).id);
            v.get(i).equipo = m.n - 1;
            v.get(i + 1).equipo = m.n - 1;
            mesas.add(m);
        }
        if (perdonado != null) {
            Pieza pp = ev.pieza(perdonado);
            if (pp != null) {
                pp.equipo = -1;
            }
        }
        ev.cambio(s);
        ev.prueba(s, PRUEBA);
        ev.reloj(s, 0);
        Pantallas.modo(s, "estatica", "");
        fase = Fase.LISTA;
        long now = s.overworld().getGameTime();
        Escena.fundido(s, MOD + ".com.fundido", 80);
        tpEn = now + 22;
        presentarEn = now + 90;
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_COMPARTIR preparada mesas={} perdonado={}", mesas.size(), perdonado != null);
        return null;
    }

    private static void alSusLugares(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Mesa m : mesas) {
            llevar(s, ev, m.a, "sal.mesa." + m.n + ".a");
            llevar(s, ev, m.b, "sal.mesa." + m.n + ".b");
        }
        if (perdonado != null) {
            llevar(s, ev, perdonado, "sal.perdonado");
            Pieza p = ev.pieza(perdonado);
            if (p != null) {
                Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".com.chat.perdonado", Anuncio.nombre(p))));
            }
        }
    }

    private static void llevar(MinecraftServer s, Evento ev, UUID id, String marca) {
        Pieza p = ev.pieza(id);
        BlockPos b = Complejo.pos(s, marca);
        Entity e = p == null ? null : Evento.entidad(s, p);
        if (e == null || b == null) {
            return;
        }
        float yaw = Complejo.yaw(marca);
        Vec3 v = new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        if (e instanceof ServerPlayer sp) {
            if (sp.isSleeping()) {
                sp.stopSleeping();
            }
            sp.teleportTo(s.overworld(), v.x, v.y, v.z, yaw, 10F);
            sp.setGameMode(GameType.ADVENTURE);
        } else {
            e.moveTo(v.x, v.y, v.z, yaw, 0F);
            e.setYHeadRot(yaw);
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    // ------------------------------------------------------------------ fases

    /** El Coleccionista explica (o de una vez): luego un minuto para hablar. */
    public static boolean empezar(MinecraftServer s, boolean conDiscurso) {
        if (fase != Fase.LISTA) {
            return false;
        }
        if (conDiscurso) {
            fase = Fase.HABLANDO_COLECCIONISTA;
            cambio = true;
            Pantallas.hablar(s, "compartir", () -> {
                if (fase == Fase.HABLANDO_COLECCIONISTA) {
                    hablar(s);
                }
            });
        } else {
            Pantallas.callar(s);
            hablar(s);
        }
        return true;
    }

    private static void hablar(MinecraftServer s) {
        fase = Fase.HABLAR;
        Evento.get(s).reloj(s, segundosHablar);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".com.pantalla.hablen").getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".com.chat.hablen", segundosHablar)));
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 0.8F, 0.8F);
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_COMPARTIR hablar");
    }

    /** Se acaba la plática: cada quien elige en secreto. */
    public static boolean elegirYa(MinecraftServer s) {
        if (fase != Fase.HABLAR && fase != Fase.LISTA && fase != Fase.HABLANDO_COLECCIONISTA) {
            return false;
        }
        Pantallas.callar(s);
        fase = Fase.ELEGIR;
        Evento ev = Evento.get(s);
        ev.reloj(s, segundosElegir);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".com.pantalla.elijan").getString());
        Random r = new Random(s.overworld().getRandom().nextLong());
        long now = s.overworld().getGameTime();
        for (Mesa m : mesas) {
            for (UUID id : new UUID[] {m.a, m.b}) {
                Pieza p = ev.pieza(id);
                if (p == null || !p.vivo()) {
                    continue;
                }
                if (p.bot && botsEligen) {
                    botDecide.put(id, now + 40 + r.nextInt(Math.max(1, segundosElegir * 20 - 80)));
                } else {
                    ServerPlayer sp = s.getPlayerList().getPlayer(id);
                    if (sp != null) {
                        CoreScreens.open(sp, PANTALLA);
                    }
                }
            }
        }
        Anuncio.sonido(s, SoundEvents.UI_BUTTON_CLICK.get(), 1F, 0.5F);
        cambio = true;
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mia(sp);
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_COMPARTIR elegir");
        return true;
    }

    /** La elección de un jugador (desde su pantalla). Una vez elegido no se cambia. */
    private static void elegir(ServerPlayer sp, CompoundTag d) {
        if (fase != Fase.ELEGIR) {
            return;
        }
        Mesa m = mesaDe(sp.getUUID());
        if (m == null) {
            return;
        }
        boolean robar = d.getBoolean("robar");
        if (m.a.equals(sp.getUUID()) && m.ra == null) {
            m.ra = robar;
        } else if (m.b.equals(sp.getUUID()) && m.rb == null) {
            m.rb = robar;
        } else {
            return;
        }
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.PLAYERS, 1F, 0.8F);
        Coleccionista.LOGGER.info("COLECCIONISTA_ELECCION mesa={} {}", m.n, Evento.get(Plat.server(sp)).pieza(sp.getUUID()).etiqueta());
        mia(sp);
        cambio = true;
    }

    /** El organizador (o la prueba automática) elige por una pieza. */
    public static boolean forzar(MinecraftServer s, Pieza p, boolean robar) {
        Mesa m = fase == Fase.ELEGIR ? mesaDe(p.id) : null;
        if (m == null) {
            return false;
        }
        if (m.a.equals(p.id)) {
            m.ra = robar;
        } else {
            m.rb = robar;
        }
        botDecide.remove(p.id);
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_ELECCION mesa={} {} (forzada)", m.n, p.etiqueta());
        return true;
    }

    private static Mesa mesaDe(UUID id) {
        for (Mesa m : mesas) {
            if (m.a.equals(id) || m.b.equals(id)) {
                return m;
            }
        }
        return null;
    }

    private static boolean todosEligieron(Evento ev) {
        for (Mesa m : mesas) {
            if (m.ra == null && vivo(ev, m.a) || m.rb == null && vivo(ev, m.b)) {
                return false;
            }
        }
        return true;
    }

    private static boolean vivo(Evento ev, UUID id) {
        Pieza p = ev.pieza(id);
        return p != null && p.vivo();
    }

    /** Se acabó el tiempo de elegir (o todos eligieron): quien no eligió, comparte; empiezan las revelaciones. */
    public static boolean revelarYa(MinecraftServer s) {
        if (fase != Fase.ELEGIR) {
            return false;
        }
        for (Mesa m : mesas) {
            if (m.ra == null) {
                m.ra = false;
            }
            if (m.rb == null) {
                m.rb = false;
            }
        }
        fase = Fase.REVELAR;
        revelando = -1;
        revelaDesde = t - REVELA + 30;
        Evento.get(s).reloj(s, 0);
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mia(sp);
        }
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_COMPARTIR revelar");
        return true;
    }

    private static void resultado(MinecraftServer s, Mesa m) {
        Evento ev = Evento.get(s);
        Pieza a = ev.pieza(m.a), b = ev.pieza(m.b);
        boolean ra = Boolean.TRUE.equals(m.ra), rb = Boolean.TRUE.equals(m.rb);
        List<Pieza> caen = new ArrayList<>();
        if (ra && !rb && b != null) {
            caen.add(b);
        } else if (rb && !ra && a != null) {
            caen.add(a);
        } else if (ra && rb) {
            if (a != null) {
                caen.add(a);
            }
            if (b != null) {
                caen.add(b);
            }
        }
        caen.removeIf(p -> !p.vivo());
        // nunca se queda el evento sin nadie
        if (!caen.isEmpty() && caen.size() >= ev.vivos().size()) {
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".com.chat.perdon")));
            caen.clear();
        }
        String clave = !ra && !rb ? "comparten" : ra && rb ? "roban" : "uno";
        Coleccionista.LOGGER.info("COLECCIONISTA_MESA {} {} caen={}", m.n, clave, caen.size());
        for (Pieza p : caen) {
            ev.eliminar(s, p, PRUEBA);
            eliminados++;
        }
        Anuncio.sonido(s, clave.equals("comparten") ? SoundEvents.PLAYER_LEVELUP : SoundEvents.BELL_BLOCK, 0.7F, clave.equals("comparten") ? 1.2F : 0.5F);
    }

    private static void fin(MinecraftServer s) {
        fase = Fase.FIN;
        revelando = -1;
        Pantallas.modo(s, "coleccionista", "");
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".com.chat.fin", eliminados)));
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_COMPARTIR fin eliminados={}", eliminados);
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        if (fase == Fase.HABLAR) {
            elegirYa(s);
            return true;
        }
        if (fase == Fase.ELEGIR) {
            revelarYa(s);
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
            alSusLugares(s);
        }
        if (presentarEn > 0 && now >= presentarEn) {
            presentarEn = -1;
            Escena.presentacion(s, "III", MOD + ".com.titulo", MOD + ".com.lema");
            Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.45F);
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        t++;
        if (fase == Fase.ELEGIR) {
            // los bots eligen solos (40 % roba)
            for (Map.Entry<UUID, Long> b : new ArrayList<>(botDecide.entrySet())) {
                if (now >= b.getValue()) {
                    botDecide.remove(b.getKey());
                    Mesa m = mesaDe(b.getKey());
                    boolean robar = s.overworld().getRandom().nextFloat() < 0.4F;
                    if (m != null && m.a.equals(b.getKey()) && m.ra == null) {
                        m.ra = robar;
                    } else if (m != null && m.b.equals(b.getKey()) && m.rb == null) {
                        m.rb = robar;
                    }
                    cambio = true;
                }
            }
            if (todosEligieron(ev)) {
                revelarYa(s);
            }
        }
        if (fase == Fase.REVELAR) {
            long dt = t - revelaDesde;
            if (revelando >= 0 && dt == RESULTADO) {
                Mesa m = mesas.get(revelando);
                m.revelada = true;
                resultado(s, m);
                cambio = true;
            }
            if (dt >= REVELA) {
                revelando++;
                // las mesas que ya no tienen a nadie vivo no se revelan
                while (revelando < mesas.size() && !vivo(ev, mesas.get(revelando).a) && !vivo(ev, mesas.get(revelando).b)) {
                    revelando++;
                }
                if (revelando >= mesas.size()) {
                    fin(s);
                } else {
                    revelaDesde = t;
                    Mesa m = mesas.get(revelando);
                    Pieza a = ev.pieza(m.a), b = ev.pieza(m.b);
                    Pantallas.modo(s, "texto", Component.translatable(MOD + ".com.pantalla.mesa", m.n, a == null ? "?" : a.etiqueta(),
                            b == null ? "?" : b.etiqueta()).getString());
                    Anuncio.sonido(s, SoundEvents.WARDEN_HEARTBEAT, 1F, 1F);
                    cambio = true;
                }
            }
        }
        if (cambio || now % 40 == 0) {
            enviar(s);
        }
    }

    // ------------------------------------------------------------------ terminar

    public static void terminar(MinecraftServer s) {
        if (fase == Fase.HABLANDO_COLECCIONISTA) {
            Pantallas.callar(s);
        }
        limpiar(s);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_COMPARTIR terminada");
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase != Fase.NADA) {
            terminar(s);
        }
    }

    private static void limpiar(MinecraftServer s) {
        fase = Fase.NADA;
        mesas.clear();
        perdonado = null;
        revelando = -1;
        botDecide.clear();
        eliminados = 0;
        tpEn = -1;
        presentarEn = -1;
        t = 0;
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mia(sp);
        }
    }

    // ------------------------------------------------------------------ a los clientes

    private static CompoundTag pieza(Evento ev, UUID id, Boolean robo, boolean mostrar) {
        CompoundTag c = new CompoundTag();
        c.putUUID("id", id);
        Pieza p = ev.pieza(id);
        c.putInt("numero", p == null ? 0 : p.numero);
        c.putString("nombre", p == null ? "?" : p.nombre);
        c.putBoolean("eligio", robo != null);
        c.putBoolean("vivo", p != null && p.vivo());
        if (mostrar && robo != null) {
            c.putBoolean("robo", robo);
        }
        return c;
    }

    public static CompoundTag estado(MinecraftServer s) {
        CompoundTag t0 = new CompoundTag();
        t0.putString("fase", fase.name());
        Evento ev = Evento.get(s);
        ListTag l = new ListTag();
        int eligieron = 0, total = 0;
        for (int i = 0; i < mesas.size(); i++) {
            Mesa m = mesas.get(i);
            boolean mostrar = m.revelada || fase == Fase.REVELAR && i == revelando;
            CompoundTag c = new CompoundTag();
            c.putInt("n", m.n);
            c.put("a", pieza(ev, m.a, m.ra, mostrar));
            c.put("b", pieza(ev, m.b, m.rb, mostrar));
            c.putBoolean("revelada", m.revelada);
            l.add(c);
            for (Boolean r : new Boolean[] {m.ra, m.rb}) {
                total++;
                if (r != null) {
                    eligieron++;
                }
            }
        }
        t0.put("mesas", l);
        t0.putInt("eligieron", eligieron);
        t0.putInt("total", total);
        if (perdonado != null) {
            t0.putUUID("perdonado", perdonado);
        }
        if (fase == Fase.REVELAR && revelando >= 0 && revelando < mesas.size()) {
            t0.putInt("revelando", revelando);
            t0.putLong("revela_t", t - revelaDesde);
        }
        return t0;
    }

    /** Lo de cada jugador: si le toca elegir ahora y qué eligió (solo él lo sabe). */
    private static void mia(ServerPlayer sp) {
        CompoundTag c = new CompoundTag();
        Mesa m = fase == Fase.ELEGIR ? mesaDe(sp.getUUID()) : null;
        Pieza p = Evento.get(Plat.server(sp)).pieza(sp.getUUID());
        c.putBoolean("elige", m != null && p != null && p.vivo());
        if (m != null) {
            Boolean r = m.a.equals(sp.getUUID()) ? m.ra : m.rb;
            c.putString("eleccion", r == null ? "" : r ? "robar" : "compartir");
            UUID otro = m.a.equals(sp.getUUID()) ? m.b : m.a;
            Pieza o = Evento.get(Plat.server(sp)).pieza(otro);
            c.putString("pareja", o == null ? "?" : "Pieza " + o.etiqueta() + " · " + o.nombre);
        }
        PlayerStore.Sync.toPlayer(sp, KEY_MIA, c);
    }

    private static void enviar(MinecraftServer s) {
        cambio = false;
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }

    /** Para el chat del organizador. */
    public static Component resumen() {
        return Component.literal(fase.name() + " · mesas " + mesas.size()).withStyle(ChatFormatting.GRAY);
    }
}
