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
import com.coleccionista.mapa.Puertas;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Prueba 6: Las puertas (suerte). Varias rondas: en cada una las tres puertas cambian de color, el Coleccionista da una pista (a veces
 * miente) y todos tienen unos segundos para meterse a una cabina. Se cierran las rejas (quien no eligió, el Coleccionista lo mete en una
 * al azar), redoble... y en una de las cabinas se abre el piso: los que estaban ahí caen al foso y quedan en la colección. Si todos los
 * vivos estaban en la cabina de la trampa, esta vez no se abre (el evento nunca se queda sin nadie).
 */
public final class LasPuertas {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":puertas";
    public static final String PRUEBA = "Las puertas";
    public static int rondas = 2, segundosElegir = 20;
    /** Qué tan seguido la pista dice la verdad. */
    public static float verdad = 0.7F;

    public enum Fase {
        NADA, LISTA, HABLANDO, ELEGIR, REDOBLE, CAE, ENTRE, FIN
    }

    /** Los colores posibles: nombre (clave de idioma), concreto del marco y color del texto. */
    static final String[][] COLORES = {{"roja", "coleccionista:marco_luz_roja", "FFE0404A"}, {"azul", "coleccionista:marco_luz_azul", "FF4A7CE0"},
            {"verde", "coleccionista:marco_luz_verde", "FF7CE04A"}, {"amarilla", "coleccionista:marco_luz_amarilla", "FFE0D04A"},
            {"morada", "coleccionista:marco_luz_morada", "FFB04AE0"}, {"blanca", "coleccionista:marco_luz_blanca", "FFF0F0F0"}};

    private static Fase fase = Fase.NADA;
    private static int ronda, trampa;
    private static final int[] color = new int[3];
    private static String pista = "";
    private static int pistaColor;
    private static long t, tpEn = -1, presentarEn = -1, hasta;
    private static final Map<UUID, Long> botDecide = new HashMap<>();
    private static int eliminados;
    private static boolean cambio, perdon;

    private LasPuertas() {
    }

    public static void init() {
        AxelisCore.onServerTick(LasPuertas::tick);
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
        if (!Complejo.construido(s, "puertas")) {
            return Component.translatable(MOD + ".pue.sin_mapa");
        }
        if (fase != Fase.NADA && fase != Fase.LISTA && fase != Fase.FIN) {
            return Component.translatable(MOD + ".pue.ya_empezo");
        }
        Evento ev = Evento.get(s);
        if (ev.vivos().size() < 2) {
            return Component.translatable(MOD + ".enc.pocos");
        }
        Pruebas.cerrarOtras(s, "puertas");
        limpiar(s);
        ev.prueba(s, PRUEBA);
        ev.reloj(s, 0);
        rejas(s, false);
        pisos(s, false);
        Pantallas.modo(s, "estatica", "");
        fase = Fase.LISTA;
        long now = s.overworld().getGameTime();
        Escena.fundido(s, MOD + ".pue.fundido", 80);
        tpEn = now + 22;
        presentarEn = now + 90;
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_PUERTAS preparada rondas={}", rondas);
        return null;
    }

    private static void alVestibulo(MinecraftServer s) {
        Evento ev = Evento.get(s);
        int i = 0;
        for (Pieza p : ev.vivos()) {
            String m = "pue.lugar." + (i++ % Puertas.LUGARES);
            BlockPos b = Complejo.pos(s, m);
            Entity e = Evento.entidad(s, p);
            if (b != null && e != null) {
                mover(s, e, new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5), Complejo.yaw(m));
            }
        }
    }

    private static void mover(MinecraftServer s, Entity e, Vec3 v, float yaw) {
        e.fallDistance = 0;
        if (e instanceof ServerPlayer sp) {
            if (sp.isSleeping()) {
                sp.stopSleeping();
            }
            sp.teleportTo(s.overworld(), v.x, v.y, v.z, yaw, 0F);
            sp.setGameMode(GameType.ADVENTURE);
        } else {
            e.moveTo(v.x, v.y, v.z, yaw, 0F);
            e.setYHeadRot(yaw);
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    // ------------------------------------------------------------------ rejas, colores y pisos

    private static void rejas(MinecraftServer s, boolean abrir) {
        ServerLevel l = s.overworld();
        for (int k = 1; k <= 3; k++) {
            BlockPos b = Complejo.pos(s, "pue.puerta." + k);
            if (b == null) {
                continue;
            }
            List<BlockPos> ps = new ArrayList<>();
            for (int dx = 0; dx < Puertas.PUERTA_ANCHO; dx++) {
                for (int y = 0; y < Puertas.PUERTA_ALTO; y++) {
                    ps.add(b.offset(dx, y, 0));
                }
            }
            for (BlockPos p : ps) {
                l.setBlock(p, abrir ? Blocks.AIR.defaultBlockState() : com.coleccionista.bloque.deco.Deco.barrotes(), 3);
            }
            if (!abrir) {
                for (BlockPos p : ps) {
                    l.setBlock(p, Block.updateFromNeighbourShapes(com.coleccionista.bloque.deco.Deco.barrotes(), l, p), 3);
                }
            }
            l.playSound(null, b.offset(2, 1, 0), abrir ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.2F, 0.6F);
        }
    }

    /** Pinta el marco de cada puerta con su color de esta ronda. */
    private static void marcos(MinecraftServer s) {
        ServerLevel l = s.overworld();
        for (int k = 1; k <= 3; k++) {
            BlockPos b = Complejo.pos(s, "pue.puerta." + k);
            if (b == null) {
                continue;
            }
            BlockState st = Complejo.estado(COLORES[color[k - 1]][1]);
            // el marco va de x-1 a x+5 (la reja ocupa x..x+4) y de la base hasta un bloque arriba de la reja
            for (int y = 0; y <= Puertas.PUERTA_ALTO; y++) {
                l.setBlock(b.offset(-1, y, 0), st, 3);
                l.setBlock(b.offset(Puertas.PUERTA_ANCHO, y, 0), st, 3);
            }
            for (int dx = -1; dx <= Puertas.PUERTA_ANCHO; dx++) {
                l.setBlock(b.offset(dx, Puertas.PUERTA_ALTO, 0), st, 3);
            }
        }
    }

    /** Abre (o cierra) el piso de trampillas de una cabina; k = 0 cierra todas. */
    private static void pisos(MinecraftServer s, boolean abrir) {
        ServerLevel l = s.overworld();
        for (int k = 1; k <= 3; k++) {
            if (abrir && k != trampa) {
                continue;
            }
            BlockPos c = Complejo.pos(s, "pue.cabina." + k);
            if (c == null) {
                continue;
            }
            for (int dx = -Puertas.CABINA_MEDIO; dx <= Puertas.CABINA_MEDIO; dx++) {
                for (int dz = Puertas.CABINA_Z0 - Puertas.CABINA_Z0 - 4; dz <= Puertas.CABINA_Z1 - Puertas.CABINA_Z0 - 4; dz++) {
                    BlockPos p = c.offset(dx, -1, dz);
                    BlockState st = l.getBlockState(p);
                    if (st.getBlock() instanceof TrapDoorBlock) {
                        l.setBlock(p, st.setValue(TrapDoorBlock.OPEN, abrir), 2);
                    }
                }
            }
            if (abrir) {
                l.playSound(null, c, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 2F, 0.5F);
                l.playSound(null, c, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 0.5F);
            }
        }
    }

    /** ¿En qué cabina está? (0 = en ninguna). */
    private static int cabina(MinecraftServer s, Entity e) {
        for (int k = 1; k <= 3; k++) {
            BlockPos c = Complejo.pos(s, "pue.cabina." + k);
            if (c == null) {
                continue;
            }
            double dx = e.getX() - (c.getX() + 0.5), dz = e.getZ() - (c.getZ() + 0.5);
            if (Math.abs(dx) <= Puertas.CABINA_MEDIO + 0.5 && dz >= -4.5 && dz <= Puertas.CABINA_Z1 - Puertas.CABINA_Z0 - 3.5
                    && e.getY() >= c.getY() - 7 && e.getY() <= c.getY() + 5) {
                return k;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ las rondas

    /** El Coleccionista explica la prueba en las pantallas y al terminar empieza la primera ronda. */
    public static boolean discurso(MinecraftServer s) {
        if (fase != Fase.LISTA || ronda > 0) {
            return false;
        }
        fase = Fase.HABLANDO;
        cambio = true;
        Pantallas.hablar(s, "puertas", () -> {
            if (fase == Fase.HABLANDO) {
                fase = Fase.LISTA;
                ronda(s);
            }
        });
        return true;
    }

    /** Empieza una ronda: colores nuevos, la pista y las rejas abiertas. */
    public static boolean ronda(MinecraftServer s) {
        if (fase != Fase.LISTA && fase != Fase.ENTRE) {
            return false;
        }
        Evento ev = Evento.get(s);
        Random r = new Random(s.overworld().getRandom().nextLong());
        ronda++;
        List<Integer> cs = new ArrayList<>();
        for (int i = 0; i < COLORES.length; i++) {
            cs.add(i);
        }
        Collections.shuffle(cs, r);
        for (int k = 0; k < 3; k++) {
            color[k] = cs.get(k);
        }
        trampa = 1 + r.nextInt(3);
        // la pista: casi siempre señala la puerta de la trampa; a veces miente y señala otra
        boolean dice = r.nextFloat() < verdad;
        int sen = dice ? trampa : 1 + (trampa + r.nextInt(2)) % 3;
        pistaColor = color[sen - 1];
        String[] formas = {"pista.1", "pista.2", "pista.3", "pista.4"};
        pista = formas[r.nextInt(formas.length)];
        Component pc = Component.translatable(MOD + ".pue.color." + COLORES[pistaColor][0]);
        Component texto = Component.translatable(MOD + ".pue." + pista, pc);
        marcos(s);
        pisos(s, false);
        rejas(s, true);
        perdon = false;
        Pantallas.modo(s, "texto", texto.getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".pue.chat.ronda", ronda, rondas)));
        Anuncio.todos(s, Anuncio.coleccionista(texto));
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.7F);
        fase = Fase.ELEGIR;
        ev.reloj(s, segundosElegir);
        long now = s.overworld().getGameTime();
        for (Pieza p : ev.vivos()) {
            if (p.bot) {
                botDecide.put(p.id, now + 30 + r.nextInt(Math.max(1, segundosElegir * 20 - 60)));
            }
        }
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_PUERTAS ronda={} trampa={} pista={} dice_verdad={}", ronda, trampa, sen, dice);
        return true;
    }

    /** Se acabó el tiempo: se cierran las rejas y quien no eligió entra a una cabina al azar. Luego, el redoble. */
    public static boolean cerrar(MinecraftServer s) {
        if (fase != Fase.ELEGIR) {
            return false;
        }
        Evento ev = Evento.get(s);
        Random r = new Random(s.overworld().getRandom().nextLong());
        rejas(s, false);
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null && cabina(s, e) == 0) {
                meter(s, e, 1 + r.nextInt(3), r);
                if (e instanceof ServerPlayer sp) {
                    Plat.msg(sp, Component.translatable(MOD + ".pue.elegi_por_ti"), true);
                }
            }
        }
        botDecide.clear();
        ev.reloj(s, 0);
        fase = Fase.REDOBLE;
        hasta = t + 70;
        Pantallas.modo(s, "texto", "...");
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_PUERTAS cerradas ronda={}", ronda);
        return true;
    }

    private static void meter(MinecraftServer s, Entity e, int k, Random r) {
        BlockPos c = Complejo.pos(s, "pue.cabina." + k);
        if (c != null) {
            mover(s, e, new Vec3(c.getX() + 0.5 + (r.nextDouble() - 0.5) * 4, c.getY(), c.getZ() + 0.5 + (r.nextDouble() - 0.5) * 4), 180F);
        }
    }

    private static void abrirTrampa(MinecraftServer s) {
        Evento ev = Evento.get(s);
        // si todos los vivos están en la cabina de la trampa, esta vez no se abre
        int dentro = 0, vivos = 0;
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            vivos++;
            if (e != null && cabina(s, e) == trampa) {
                dentro++;
            }
        }
        String nombre = Component.translatable(MOD + ".pue.color." + COLORES[color[trampa - 1]][0]).getString();
        if (vivos > 0 && dentro == vivos) {
            perdon = true;
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".pue.chat.perdon")));
            Coleccionista.LOGGER.info("COLECCIONISTA_PUERTAS perdon ronda={}", ronda);
        } else {
            pisos(s, true);
        }
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".pue.pantalla.trampa", nombre).getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".pue.chat.trampa", nombre, dentro)));
        fase = Fase.CAE;
        hasta = t + 100;
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_PUERTAS trampa={} dentro={}", trampa, dentro);
    }

    /** Los que caen al foso quedan en la colección (en cuanto bajan del piso de la cabina). */
    private static void caidas(MinecraftServer s) {
        Evento ev = Evento.get(s);
        BlockPos c = Complejo.pos(s, "pue.cabina." + trampa);
        if (c == null) {
            return;
        }
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null && cabina(s, e) == trampa && e.getY() < c.getY() - 1.2) {
                e.fallDistance = 0;
                if (ev.eliminarEnPrueba(s, p, PRUEBA)) {
                    eliminados++;
                }
            }
        }
    }

    private static void finRonda(MinecraftServer s) {
        pisos(s, false);
        if (ronda >= rondas || Evento.get(s).vivos().size() <= 1) {
            fin(s);
            return;
        }
        fase = Fase.ENTRE;
        Escena.fundido(s, "", 50);
        tpEn = s.overworld().getGameTime() + 14;
        Pantallas.modo(s, "coleccionista", "");
        cambio = true;
    }

    private static void fin(MinecraftServer s) {
        fase = Fase.FIN;
        pisos(s, false);
        rejas(s, true);
        Pantallas.modo(s, "coleccionista", "");
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".pue.chat.fin", eliminados)));
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_PUERTAS fin eliminados={}", eliminados);
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        if (fase == Fase.ELEGIR) {
            cerrar(s);
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
            alVestibulo(s);
        }
        if (presentarEn > 0 && now >= presentarEn) {
            presentarEn = -1;
            Escena.presentacion(s, "X", MOD + ".pue.titulo", MOD + ".pue.lema");
            Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.45F);
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        t++;
        if (fase == Fase.ELEGIR) {
            Random r = new Random(now);
            for (Map.Entry<UUID, Long> b : new ArrayList<>(botDecide.entrySet())) {
                if (now >= b.getValue()) {
                    botDecide.remove(b.getKey());
                    Pieza p = ev.pieza(b.getKey());
                    Entity e = p == null ? null : Evento.entidad(s, p);
                    if (e != null) {
                        meter(s, e, 1 + r.nextInt(3), r);
                    }
                }
            }
        } else if (fase == Fase.REDOBLE) {
            if ((hasta - t) % 10 == 0) {
                Anuncio.sonido(s, SoundEvents.NOTE_BLOCK_BASEDRUM.get(), 1F, 0.6F + (70 - (hasta - t)) / 100F);
            }
            if (t >= hasta) {
                abrirTrampa(s);
            }
        } else if (fase == Fase.CAE) {
            if (!perdon) {
                caidas(s);
            }
            if (t >= hasta) {
                finRonda(s);
            }
        }
        if (cambio || now % 40 == 0) {
            enviar(s);
        }
    }

    public static void terminar(MinecraftServer s) {
        if (fase == Fase.HABLANDO) {
            Pantallas.callar(s);
        }
        if (Complejo.construido(s, "puertas")) {
            pisos(s, false);
            rejas(s, true);
        }
        limpiar(s);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_PUERTAS terminada");
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase != Fase.NADA) {
            terminar(s);
        }
    }

    private static void limpiar(MinecraftServer s) {
        fase = Fase.NADA;
        ronda = 0;
        trampa = 0;
        pista = "";
        botDecide.clear();
        eliminados = 0;
        tpEn = -1;
        presentarEn = -1;
        t = 0;
    }

    // ------------------------------------------------------------------ a los clientes

    public static CompoundTag estado(MinecraftServer s) {
        CompoundTag c = new CompoundTag();
        c.putString("fase", fase.name());
        c.putInt("ronda", ronda);
        c.putInt("rondas", rondas);
        if (!pista.isEmpty()) {
            c.putString("pista", pista);
            c.putString("pista_color", COLORES[pistaColor][0]);
            c.putInt("pista_rgb", (int) Long.parseLong(COLORES[pistaColor][2], 16));
        }
        for (int k = 0; k < 3; k++) {
            c.putString("color" + (k + 1), COLORES[color[k]][0]);
        }
        if (fase == Fase.CAE || fase == Fase.ENTRE || fase == Fase.FIN) {
            c.putInt("trampa", trampa);
        }
        return c;
    }

    private static void enviar(MinecraftServer s) {
        cambio = false;
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }
}
