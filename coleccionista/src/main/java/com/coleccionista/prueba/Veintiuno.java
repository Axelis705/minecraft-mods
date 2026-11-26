package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import com.coleccionista.bot.PiezaBot;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Apagon;
import com.coleccionista.evento.Cabina;
import com.coleccionista.evento.Cadaver;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Cuerpos;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Maquina;
import com.coleccionista.evento.Objetivo;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.evento.Silla;
import com.coleccionista.mapa.Pasaje;
import com.coleccionista.mapa.SalaVeintiuno;
import com.coleccionista.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * La final (capítulo 7) · Veintiuno. Del salón de baile pasan a la mesa de juego del Coleccionista. Los dos finalistas se sientan frente a
 * frente con la mano izquierda metida en una máquina de latón y juegan veintiuno con once cartas (del 1 al 11, una de cada una):
 * <ul>
 *     <li>a cada uno le tocan dos: una tapada (solo la ve él) y una a la vista;</li>
 *     <li>por turnos, cada quien pide carta o se planta; cuando los dos se plantan seguidos, se voltean las tapadas;</li>
 *     <li>gana quien quede más cerca de 21 sin pasarse; si los dos se pasan, el que se pasó menos; si empatan, nadie pierde;</li>
 *     <li>cada uno tiene dos comodines de una sola vez: <b>Espiar</b> (ver la tapada del otro) y <b>Regresar</b> (devolver al mazo su
 *     última carta);</li>
 *     <li>quien pierde una ronda, la máquina le aprieta; a la segunda, chispas; a la tercera, ¡pum!: pierde el brazo (y la final).</li>
 * </ul>
 * Si llegan más de dos, el ganador se queda en la mesa y entra el siguiente.
 */
public final class Veintiuno {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":veintiuno";
    public static final String KEY_MIO = MOD + ":veintiuno_mio";
    public static final String ACCION = MOD + ":veintiuno_accion";
    public static final String PRUEBA = "Veintiuno";
    /** Ajustes: cuántas rondas perdidas cortan, los segundos de cada turno, si hay comodines. */
    public static int golpesMax = 3, segundosTurno = 25;
    public static int esperaPasillo = 1200, esperaSalon = 900;
    public static boolean comodines = true;
    /** ¿Al empezar la final, el organizador se disfraza del Coleccionista y se sienta en el trono? (se puede levantar y caminar) */
    public static boolean trono = true;
    /** La silla invisible del trono y a quién se le cambió el modo de juego para que se vea (para regresárselo). */
    private static Silla sillaTrono;
    private static final Map<UUID, net.minecraft.world.level.GameType> modoAntes = new HashMap<>();
    /** Al terminar el piano, pasan solos a la mesa de juego; {@code rapido}: los bots juegan al instante. */
    public static boolean encadenar = true, rapido;
    /** Para las pruebas: el mazo en este orden (las primeras cuatro son la tapada y la visible de cada uno), en lugar de barajar. */
    public static List<Integer> mazoFijo;
    public static final int CARTAS = 11, META = 21;
    /** Cuánto dura repartir, voltear las tapadas y cada castigo (ticks). */
    public static final int T_REPARTE = 44, T_REVELA = 80, T_CASTIGO = 90, T_CORTE = 110;

    public enum Fase {
        NADA, PASILLO, LLEGANDO, HABLANDO, SENTAR, REGLAS, REPARTE, TURNO, REVELA, CASTIGO, FIN
    }

    private static Fase fase = Fase.NADA;
    /** Si esta vez se llega caminando hacia adelante (por el pasillo de la sala anterior a la puerta de atrás del de servicio). */
    private static boolean adelante;
    private static long t, desde, apagonFin, turnoDesde;
    private static final Random R = new Random();
    private static MinecraftServer srv;

    /** Los dos de la mesa (lado 0 al oeste, 1 al este) y los que esperan su turno de retar al ganador. */
    private static final UUID[] jugador = new UUID[2];
    private static final List<UUID> cola = new ArrayList<>();
    private static final Silla[] silla = new Silla[2];
    private static final Maquina[] maquina = new Maquina[2];
    private static final List<Integer> mazo = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private static final List<Integer>[] mano = new List[] {new ArrayList<Integer>(), new ArrayList<Integer>()};
    private static final boolean[] plantado = new boolean[2], espiar = new boolean[2], regresar = new boolean[2], espio = new boolean[2];
    private static final int[] golpes = new int[2];
    private static int turno, ronda, duelo, pasesSeguidos, perdedor = -1;
    private static String resultado = "";
    /** Lo último que pasó (para el HUD): quién, qué y cuándo. */
    private static String accion = "";
    private static int accionDe = -1;
    private static long accionEn, botEn = -1;
    private static boolean reglasDichas, cortado;
    private static long aFin;

    private Veintiuno() {
    }

    public static void init() {
        AxelisCore.onServerTick(Veintiuno::tick);
        AxelisCore.onLogin(p -> {
            PlayerStore.Sync.toPlayer(p, KEY, estado());
            mio(Plat.server(p), p);
        });
        Actions.on(ACCION, Veintiuno::recibir);
        // nadie se lastima aquí: lo único que duele es la máquina
        MinecraftForge.EVENT_BUS.addListener((LivingHurtEvent e) -> {
            if (fase != Fase.NADA && fase != Fase.PASILLO && e.getEntity() instanceof ServerPlayer) {
                e.setCanceled(true);
            }
        });
    }

    public static Fase fase() {
        return fase;
    }

    public static boolean abierta() {
        return fase != Fase.NADA;
    }

    public static boolean puedeSentarse(ServerPlayer sp, Silla si) {
        return false;
    }

    // ------------------------------------------------------------------ del salón de baile a la mesa de juego

    public static Component llamar(MinecraftServer s) {
        if (!Complejo.construido(s, "veintiuno")) {
            return Component.translatable(MOD + ".v21.sin_mapa");
        }
        if (!Complejo.construido(s, "piano")) {
            return llegar(s);
        }
        Pruebas.cerrarOtras(s, "veintiuno");
        limpiarTodo(s);
        adelante = Adelante.hay(s, "veintiuno");
        if (adelante) {
            Adelante.abrir(s, "veintiuno", "pas_veintiuno", true);
        } else {
            puertas(s, "pas_piano", true);
        }
        fase = Fase.PASILLO;
        desde = t;
        Evento ev = Evento.get(s);
        ev.prueba(s, Component.translatable(MOD + (adelante ? ".adelante.prueba" : ".caja.prueba_pasillo")).getString());
        ev.reloj(s, 0);
        Anuncio.titulo(s, Component.empty(), Component.translatable(MOD + (adelante ? ".adelante.titulo" : ".caja.al_pasillo")));
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_V21 llamar");
        return null;
    }

    public static Component llegar(MinecraftServer s) {
        if (!Complejo.construido(s, "veintiuno")) {
            return Component.translatable(MOD + ".v21.sin_mapa");
        }
        if (fase == Fase.NADA) {
            Pruebas.cerrarOtras(s, "veintiuno");
            limpiarTodo(s);
        }
        Evento ev = Evento.get(s);
        boolean ad = Adelante.hay(s, "veintiuno");
        List<Entity> enPasillo = new ArrayList<>(), fuera = new ArrayList<>();
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null) {
                continue;
            }
            if (ad && Cabina.dentro(s, "pas_veintiuno", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                continue; // ya llegó caminando
            }
            if (Complejo.construido(s, "piano") && Cabina.dentro(s, "pas_piano", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                enPasillo.add(e);
            } else {
                fuera.add(e);
            }
        }
        if (!enPasillo.isEmpty()) {
            Cabina.trasladar(s, "pas_piano", "pas_veintiuno", enPasillo, List.of());
        }
        BlockPos c = Complejo.pos(s, "pas_veintiuno.centro");
        int n = 0;
        for (Entity e : fuera) {
            if (c == null) {
                break;
            }
            ir(s, e, c.getX() + 0.5 - 1.5 + (n % 3) * 1.5, c.getY(), c.getZ() + 0.5 - 1 + (n / 3 % 4), 270F);
            n++;
        }
        puertas(s, "pas_piano", false);
        puertas(s, "pas_veintiuno", true);
        fase = Fase.LLEGANDO;
        desde = t;
        ev.prueba(s, Component.translatable(MOD + ".v21.prueba").getString());
        ev.reloj(s, 0);
        Pantallas.modo(s, "apagada", "");
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_V21 llegan pasillo={} fuera={}", enPasillo.size(), fuera.size());
        return null;
    }

    private static void puertas(MinecraftServer s, String pasillo, boolean abrir) {
        for (String lado : new String[] {".izq", ".der"}) {
            for (String h : new String[] {".a", ".b"}) {
                BlockPos p = Complejo.pos(s, pasillo + lado + h);
                if (p == null) {
                    continue;
                }
                for (BlockPos q : new BlockPos[] {p, p.above()}) {
                    BlockState st = s.overworld().getBlockState(q);
                    if (st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
                            && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN) != abrir) {
                        s.overworld().setBlock(q, st.setValue(net.minecraft.world.level.block.DoorBlock.OPEN, abrir), 2 | 16);
                    }
                }
            }
        }
    }

    private static boolean enCuarto(MinecraftServer s, Entity e) {
        BlockPos o = Complejo.origen(s, "veintiuno");
        if (o == null) {
            return false;
        }
        double x = e.getX() - o.getX(), z = e.getZ() - o.getZ();
        return x >= SalaVeintiuno.HX0 && x <= SalaVeintiuno.HX1 + 1 && z >= SalaVeintiuno.HZ0 && z <= SalaVeintiuno.HZ1 + 1;
    }

    // ------------------------------------------------------------------ el discurso, las sillas y las máquinas

    /**
     * El organizador (el que no juega) se pone el traje del Coleccionista y se sienta en su trono, frente a la mesa. Se puede levantar
     * (Shift) y caminar alrededor para ponerlos nerviosos; mientras lleve el traje, su voz se oye más grave. Si estaba en espectador, pasa a
     * aventura para que lo vean (al limpiar se le regresa su modo).
     */
    public static int alTrono(MinecraftServer s) {
        if (!trono) {
            return 0;
        }
        Evento ev = Evento.get(s);
        BlockPos b = Complejo.pos(s, "v21.trono");
        if (b == null) {
            return 0;
        }
        int n = 0;
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            if (!ev.esOrganizador(sp.getUUID()) || ev.pieza(sp.getUUID()) != null) {
                continue;
            }
            ev.disfraz(s, sp.getUUID(), true);
            if (sp.isSpectator()) {
                modoAntes.putIfAbsent(sp.getUUID(), sp.gameMode.getGameModeForPlayer());
                sp.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
            }
            if (n == 0) {
                ServerLevel l = s.overworld();
                if (sillaTrono == null || !sillaTrono.isAlive()) {
                    sillaTrono = ModRegistry.SILLA.get().create(l);
                    if (sillaTrono != null) {
                        float yaw = Complejo.yaw("v21.trono");
                        sillaTrono.moveTo(b.getX() + 0.5, b.getY() + 0.12, b.getZ() + 0.5, yaw, 0F);
                        sillaTrono.setYHeadRot(yaw);
                        sillaTrono.grupo = "trono";
                        sillaTrono.oculta(true);
                        l.addFreshEntity(sillaTrono);
                    }
                }
                if (sillaTrono != null) {
                    sp.stopRiding();
                    sp.teleportTo(l, sillaTrono.getX(), sillaTrono.getY() + 0.3, sillaTrono.getZ(), sillaTrono.getYRot(), 0F);
                    sp.startRiding(sillaTrono, true);
                }
            }
            n++;
            Coleccionista.LOGGER.info("COLECCIONISTA_V21 trono {}", sp.getGameProfile().getName());
        }
        return n;
    }

    private static void hablar(MinecraftServer s) {
        fase = Fase.HABLANDO;
        desde = t;
        BlockPos c = Complejo.pos(s, "v21.entrada");
        for (Pieza p : Evento.get(s).vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null && c != null && !enCuarto(s, e)) {
                ir(s, e, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 270F);
            }
        }
        puertas(s, "pas_veintiuno", false);
        crearMaquinas(s);
        alTrono(s);
        Escena.presentacion(s, "VII", MOD + ".v21.titulo", MOD + ".v21.lema");
        Pantallas.soloGrande(s, true);
        Pantallas.hablar(s, "veintiuno", () -> {
            if (fase == Fase.HABLANDO) {
                sentar(s, true);
            }
        });
        Evento.get(s).prueba(s, PRUEBA);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_V21 habla");
    }

    /** Dónde va la silla del lado l (un poco metida bajo la mesa): x, y, z, yaw. */
    private static double[] lugar(MinecraftServer s, int l) {
        String m = "v21.silla." + l;
        BlockPos b = Complejo.pos(s, m);
        if (b == null) {
            return null;
        }
        float yaw = Complejo.yaw(m);
        double r = Math.toRadians(yaw);
        double fx = -Math.sin(r), fz = Math.cos(r);
        return new double[] {b.getX() + 0.5 + fx * 0.3, b.getY(), b.getZ() + 0.5 + fz * 0.3, yaw};
    }

    /**
     * Dónde va la máquina respecto al lugar de la silla (bloques hacia adelante y a la izquierda): justo debajo del antebrazo izquierdo
     * estirado sobre la mesa (con la hoja a la mitad del brazo).
     */
    public static final double MAQ_FRENTE = 0.556, MAQ_LADO = 0.348;

    /** La máquina va debajo del antebrazo izquierdo de cada uno, sobre la mesa (el brazo queda encima). */
    public static Vec3 posMaquina(double[] silla) {
        double r = Math.toRadians(silla[3]);
        double fx = -Math.sin(r), fz = Math.cos(r);
        // a la izquierda de quien mira hacia (fx, fz) está (fz, -fx)
        double ix = fz, iz = -fx;
        return new Vec3(silla[0] + fx * MAQ_FRENTE + ix * MAQ_LADO, silla[1] - 1 + SalaVeintiuno.TAPETE, silla[2] + fz * MAQ_FRENTE + iz * MAQ_LADO);
    }

    private static void crearMaquinas(MinecraftServer s) {
        ServerLevel l = s.overworld();
        BlockPos o = Complejo.origen(s, "veintiuno");
        for (int k = 0; k < 2; k++) {
            if (maquina[k] != null && maquina[k].isAlive()) {
                continue;
            }
            double[] c = lugar(s, k);
            Maquina m = c == null || o == null ? null : ModRegistry.MAQUINA.get().create(l);
            if (m == null) {
                continue;
            }
            Vec3 p = posMaquina(c);
            m.moveTo(p.x, p.y, p.z, (float) c[3], 0F);
            m.setYHeadRot((float) c[3]);
            m.setYBodyRot((float) c[3]);
            l.addFreshEntity(m);
            maquina[k] = m;
        }
    }

    /** Quiénes se sientan: los dos primeros (al azar la primera vez); el resto espera en la cola. */
    private static void sentar(MinecraftServer s, boolean apagon) {
        Pantallas.callar(s);
        fase = Fase.SENTAR;
        desde = t;
        apagonFin = t + (apagon ? 70 : 10);
        if (apagon) {
            Apagon.hacer(s, s.getPlayerList().getPlayers(), 60, () -> acomodar(s));
        } else {
            acomodar(s);
        }
        enviar(s);
    }

    private static void acomodar(MinecraftServer s) {
        Evento ev = Evento.get(s);
        ServerLevel l = s.overworld();
        if (jugador[0] == null && jugador[1] == null) {
            List<Pieza> v = new ArrayList<>(ev.vivos());
            Collections.shuffle(v, R);
            cola.clear();
            for (Pieza p : v) {
                cola.add(p.id);
            }
        }
        // los lugares vacíos los toman los que siguen en la cola
        for (int k = 0; k < 2; k++) {
            if (jugador[k] == null && !cola.isEmpty()) {
                jugador[k] = cola.remove(0);
                golpes[k] = 0;
                espiar[k] = comodines;
                regresar[k] = comodines;
            }
        }
        crearMaquinas(s);
        for (int k = 0; k < 2; k++) {
            double[] c = lugar(s, k);
            if (c == null) {
                continue;
            }
            // quita el cuerpo del que perdió antes (en el apagón se lo llevan)
            for (Cadaver cd : l.getEntitiesOfClass(Cadaver.class, new AABB(c[0] - 1, c[1] - 1, c[2] - 1, c[0] + 1, c[1] + 2, c[2] + 1))) {
                cd.discard();
            }
            if (silla[k] == null || !silla[k].isAlive()) {
                Silla si = ModRegistry.SILLA.get().create(l);
                if (si == null) {
                    continue;
                }
                si.moveTo(c[0], c[1], c[2], (float) c[3], 0F);
                si.setYHeadRot((float) c[3]);
                si.grupo = "veintiuno";
                l.addFreshEntity(si);
                silla[k] = si;
            }
            Pieza p = jugador[k] == null ? null : ev.pieza(jugador[k]);
            Entity e = p == null ? null : Evento.entidad(s, p);
            if (e != null && e.getVehicle() != silla[k]) {
                sentar(s, e, silla[k]);
                silla[k].cerrar(true, false);
            }
            if (maquina[k] != null) {
                maquina[k].golpes(golpes[k]);
                if (maquina[k].estado() == Maquina.ABIERTA || maquina[k].estado() == Maquina.CORTA) {
                    maquina[k].estado(Maquina.CERRADA);
                }
                // en el apagón se llevan el brazo cortado del anterior: ahora es el brazo del que se sienta
                maquina[k].dueno(jugador[k]);
            }
        }
        // los que esperan, de pie a los lados de la mesa (miran)
        BlockPos m = Complejo.pos(s, "v21.mesa");
        int n = 0;
        for (UUID id : cola) {
            Pieza p = ev.pieza(id);
            Entity e = p == null ? null : Evento.entidad(s, p);
            if (e != null && m != null) {
                double z = n % 2 == 0 ? -3.2 : 4.2;
                ir(s, e, m.getX() + 0.5 - 1 + (n / 2) * 1.2, m.getY() - 1, m.getZ() + z, n % 2 == 0 ? 0F : 180F);
                n++;
            }
        }
        duelo++;
        ronda = 0;
        s.overworld().playSound(null, m == null ? 0 : m.getX(), m == null ? 0 : m.getY(), m == null ? 0 : m.getZ(), propio("maquina_cierra"),
                SoundSource.HOSTILE, 2.5F, 1F);
        Coleccionista.LOGGER.info("COLECCIONISTA_V21 sentados duelo={} {} contra {} esperan={}", duelo, nombre(s, 0), nombre(s, 1), cola.size());
    }

    private static String nombre(MinecraftServer s, int l) {
        Pieza p = jugador[l] == null ? null : Evento.get(s).pieza(jugador[l]);
        return p == null ? "?" : p.nombre;
    }

    private static void reglas(MinecraftServer s) {
        fase = Fase.REGLAS;
        desde = t;
        reglasDichas = true;
        Pantallas.soloGrande(s, true);
        Pantallas.hablar(s, "veintiuno_reglas", () -> {
            if (fase == Fase.REGLAS) {
                nuevaRonda(s);
            }
        });
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_V21 reglas");
    }

    // ------------------------------------------------------------------ las rondas

    private static SoundEvent propio(String id) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD, id));
    }

    private static void sonidoMesa(MinecraftServer s, SoundEvent e, float vol, float tono) {
        BlockPos m = Complejo.pos(s, "v21.mesa");
        if (m != null) {
            s.overworld().playSound(null, m.getX() + 0.5, m.getY(), m.getZ() + 0.5, e, SoundSource.HOSTILE, vol, tono);
        }
    }

    private static void nuevaRonda(MinecraftServer s) {
        Pantallas.callar(s);
        ronda++;
        mazo.clear();
        if (mazoFijo != null && !mazoFijo.isEmpty()) {
            mazo.addAll(mazoFijo);
            for (int c = 1; c <= CARTAS; c++) {
                if (!mazo.contains(c)) {
                    mazo.add(c);
                }
            }
        } else {
            for (int c = 1; c <= CARTAS; c++) {
                mazo.add(c);
            }
            Collections.shuffle(mazo, R);
        }
        for (int k = 0; k < 2; k++) {
            mano[k].clear();
            plantado[k] = false;
            espio[k] = false;
        }
        // la tapada y la visible de cada uno (como se reparte: una y una)
        mano[0].add(mazo.remove(0));
        mano[1].add(mazo.remove(0));
        mano[0].add(mazo.remove(0));
        mano[1].add(mazo.remove(0));
        // empieza uno y la siguiente ronda el otro
        turno = (ronda + duelo) % 2;
        pasesSeguidos = 0;
        perdedor = -1;
        resultado = "";
        accion = "";
        accionDe = -1;
        fase = Fase.REPARTE;
        desde = t;
        Evento.get(s).reloj(s, 0);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".v21.pantalla.ronda", ronda).getString());
        enviarTodo(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_V21 ronda {} {}={}+{} {}={}+{}", ronda, nombre(s, 0), mano[0].get(0), mano[0].get(1), nombre(s, 1),
                mano[1].get(0), mano[1].get(1));
    }

    public static int total(int l) {
        int n = 0;
        for (int c : mano[l]) {
            n += c;
        }
        return n;
    }

    private static int visible(int l) {
        int n = 0;
        for (int i = 1; i < mano[l].size(); i++) {
            n += mano[l].get(i);
        }
        return n;
    }

    private static void empezarTurno(MinecraftServer s) {
        fase = Fase.TURNO;
        turnoDesde = t;
        botEn = -1;
        Evento.get(s).reloj(s, segundosTurno);
        enviarTodo(s);
    }

    /** Pide carta, se planta o usa un comodín (lo manda el cliente, un bot o un comando). */
    public static boolean jugar(MinecraftServer s, int l, String que) {
        if (fase != Fase.TURNO || l != turno || jugador[l] == null) {
            return false;
        }
        switch (que) {
            case "pedir" -> {
                if (mazo.isEmpty()) {
                    return jugar(s, l, "plantarse");
                }
                int c = mazo.remove(0);
                mano[l].add(c);
                plantado[l] = false;
                pasesSeguidos = 0;
                hecho(s, l, "pedir");
                sonidoMesa(s, propio("carta"), 1.4F, 0.9F + R.nextFloat() * 0.2F);
                Coleccionista.LOGGER.info("COLECCIONISTA_V21 pide {} carta={} total={}", nombre(s, l), c, total(l));
                siguienteTurno(s);
            }
            case "plantarse" -> {
                plantado[l] = true;
                pasesSeguidos++;
                hecho(s, l, "plantarse");
                sonidoMesa(s, SoundEvents.WOOD_HIT, 0.8F, 0.7F);
                Coleccionista.LOGGER.info("COLECCIONISTA_V21 planta {} total={}", nombre(s, l), total(l));
                if (pasesSeguidos >= 2) {
                    revelar(s);
                } else {
                    siguienteTurno(s);
                }
            }
            case "espiar" -> {
                if (!espiar[l] || espio[l]) {
                    return false;
                }
                espiar[l] = false;
                espio[l] = true;
                hecho(s, l, "espiar");
                sonidoMesa(s, SoundEvents.ENDER_EYE_DEATH, 1F, 1.2F);
                Coleccionista.LOGGER.info("COLECCIONISTA_V21 espia {}", nombre(s, l));
                enviarTodo(s);
            }
            case "regresar" -> {
                if (!regresar[l] || mano[l].size() <= 2) {
                    return false;
                }
                regresar[l] = false;
                int c = mano[l].remove(mano[l].size() - 1);
                mazo.add(R.nextInt(mazo.size() + 1), c);
                hecho(s, l, "regresar");
                sonidoMesa(s, propio("carta"), 1.4F, 0.7F);
                Coleccionista.LOGGER.info("COLECCIONISTA_V21 regresa {} carta={} total={}", nombre(s, l), c, total(l));
                enviarTodo(s);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void hecho(MinecraftServer s, int l, String que) {
        accion = que;
        accionDe = l;
        accionEn = s.overworld().getGameTime();
    }

    private static void siguienteTurno(MinecraftServer s) {
        turno = 1 - turno;
        empezarTurno(s);
    }

    /** Los dos se plantaron: se voltean las tapadas y se ve quién gana. */
    private static void revelar(MinecraftServer s) {
        fase = Fase.REVELA;
        desde = t;
        Evento.get(s).reloj(s, 0);
        int a = total(0), b = total(1);
        boolean pa = a > META, pb = b > META;
        int gana;
        if (pa && pb) {
            gana = a == b ? -1 : a < b ? 0 : 1;
        } else if (pa) {
            gana = 1;
        } else if (pb) {
            gana = 0;
        } else {
            gana = a == b ? -1 : a > b ? 0 : 1;
        }
        perdedor = gana < 0 ? -1 : 1 - gana;
        resultado = gana < 0 ? "empate" : "gana";
        sonidoMesa(s, propio("carta"), 1.6F, 0.6F);
        sonidoMesa(s, SoundEvents.WARDEN_HEARTBEAT, 1.4F, 1F);
        enviarTodo(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_V21 revela ronda={} {}={} {}={} {}", ronda, nombre(s, 0), a, nombre(s, 1), b,
                gana < 0 ? "empate" : "pierde " + nombre(s, perdedor));
    }

    /** La máquina del que perdió la ronda: aprieta, chispas o corta. */
    private static void castigo(MinecraftServer s) {
        fase = Fase.CASTIGO;
        desde = t;
        int l = perdedor;
        golpes[l]++;
        Maquina m = maquina[l];
        int g = golpes[l];
        cortado = g >= golpesMax;
        int estado = cortado ? Maquina.CORTA : g >= golpesMax - 1 && golpesMax >= 3 ? Maquina.CHISPAS : Maquina.APRIETA;
        if (m != null) {
            m.golpes(g);
            m.estado(estado);
            m.dueno(jugador[l]);
            ServerLevel lv = s.overworld();
            String son = estado == Maquina.CORTA ? "maquina_pum" : estado == Maquina.CHISPAS ? "maquina_chispas" : "maquina_aprieta";
            lv.playSound(null, m.getX(), m.getY(), m.getZ(), propio(son), SoundSource.HOSTILE, 3F, 1F);
        }
        Pieza p = Evento.get(s).pieza(jugador[l]);
        Entity e = p == null ? null : Evento.entidad(s, p);
        if (e instanceof ServerPlayer sp) {
            Fx.shake(sp, estado == Maquina.APRIETA ? 0.5F : 0.8F, estado == Maquina.APRIETA ? 14 : 22);
        }
        if (e != null && estado != Maquina.CORTA) {
            s.overworld().playSound(null, e.getX(), e.getY() + 1, e.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.5F, 0.8F);
        }
        enviarTodo(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_V21 castigo {} golpes={}/{} {}", nombre(s, l), g, golpesMax,
                estado == Maquina.CORTA ? "corta" : estado == Maquina.CHISPAS ? "chispas" : "aprieta");
    }

    private static void castigoTick(MinecraftServer s, long dt) {
        int l = perdedor;
        Maquina m = l < 0 ? null : maquina[l];
        ServerLevel lv = s.overworld();
        if (m != null && m.estado() == Maquina.CHISPAS && dt < 44 && dt % 3 == 0) {
            lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.getX(), m.getY() + 0.4, m.getZ(), 6, 0.3, 0.25, 0.3, 0.12);
            if (dt % 9 == 0) {
                lv.sendParticles(ParticleTypes.SMOKE, m.getX(), m.getY() + 0.5, m.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
            }
        }
        if (m != null && m.estado() == Maquina.CORTA) {
            if (dt == Maquina.T_CAE) {
                // ¡pum!: sangre por todos lados
                DustParticleOptions sangre = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.04F), 1.6F);
                lv.sendParticles(sangre, m.getX(), m.getY() + 0.4, m.getZ(), 60, 0.45, 0.35, 0.45, 0.05);
                lv.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), m.getX(), m.getY() + 0.3,
                        m.getZ(), 30, 0.3, 0.2, 0.3, 0.15);
                Pieza p = Evento.get(s).pieza(jugador[l]);
                Entity e = p == null ? null : Evento.entidad(s, p);
                if (e != null) {
                    lv.playSound(null, e.getX(), e.getY() + 1, e.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 2F, 0.6F);
                }
                for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
                    Fx.shake(sp, 1.0F, 14);
                }
                // la mancha en la mesa
                BlockPos b = BlockPos.containing(m.getX(), m.getY() + 0.1, m.getZ());
                if (lv.getBlockState(b).isAir()) {
                    BlockState st = Complejo.estado(MOD + ":sangre");
                    if (st != null) {
                        lv.setBlock(b, st, 2);
                    }
                }
            }
            if (dt > Maquina.T_CAE && dt < Maquina.T_CAE + 40 && dt % 4 == 0) {
                lv.sendParticles(new DustParticleOptions(new Vector3f(0.5F, 0.02F, 0.03F), 1.0F), m.getX(), m.getY() + 0.15, m.getZ(), 3, 0.25,
                        0.02, 0.25, 0.0);
            }
            // se desangra: del muñón (a la mitad del brazo) gotea sangre hasta el piso mientras se queda quieto
            double[] c = lugar(s, l);
            if (c != null && dt > Maquina.T_CAE && dt < Maquina.T_CAE + 95 && dt % 3 == 0) {
                double r = Math.toRadians(c[3]);
                double fx = -Math.sin(r), fz = Math.cos(r), ix = fz, iz = -fx;
                double adelante = dt < Maquina.T_CAE + 45 ? 0.42 : 0.2;
                lv.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                        c[0] + fx * adelante + ix * 0.36, c[1] + 1.15, c[2] + fz * adelante + iz * 0.36, 2, 0.04, 0.02, 0.04, 0.0);
            }
        }
    }

    /** Se acabó el castigo: si no cortó, otra ronda; si cortó, el que perdió sale de la final (y entra el siguiente, o se acaba). */
    private static void despuesCastigo(MinecraftServer s) {
        int l = perdedor;
        if (!cortado) {
            if (maquina[l] != null) {
                maquina[l].estado(Maquina.CERRADA);
            }
            nuevaRonda(s);
            return;
        }
        Evento ev = Evento.get(s);
        Pieza p = ev.pieza(jugador[l]);
        if (p != null) {
            Cuerpos.MANCO.add(p.id);
            ev.eliminar(s, p, Component.translatable(MOD + ".v21.motivo").getString());
        }
        jugador[l] = null;
        silla[l] = null;
        int g = 1 - l;
        if (maquina[g] != null) {
            maquina[g].estado(Maquina.ABIERTA);
            s.overworld().playSound(null, maquina[g].getX(), maquina[g].getY(), maquina[g].getZ(), propio("maquina_abre"), SoundSource.HOSTILE,
                    2F, 1F);
        }
        cola.removeIf(id -> {
            Pieza q = ev.pieza(id);
            return q == null || !q.vivo();
        });
        if (!cola.isEmpty()) {
            // entra el siguiente: el ganador se queda en la mesa (con sus golpes)
            Coleccionista.LOGGER.info("COLECCIONISTA_V21 siguiente esperan={}", cola.size());
            sentar(s, true);
            return;
        }
        fin(s);
    }

    // ------------------------------------------------------------------ los bots

    private static void botTick(MinecraftServer s) {
        Pieza p = jugador[turno] == null ? null : Evento.get(s).pieza(jugador[turno]);
        if (p == null || !p.bot) {
            return;
        }
        if (botEn < 0) {
            botEn = t + (rapido ? 4 : 30 + R.nextInt(40));
            return;
        }
        if (t < botEn) {
            return;
        }
        botEn = t + (rapido ? 4 : 20 + R.nextInt(20));
        int l = turno, yo = total(l), otro = visible(1 - l);
        if (espio[l]) {
            otro = total(1 - l);
        }
        // un comodín de vez en cuando
        if (!rapido && espiar[l] && !espio[l] && R.nextFloat() < 0.35F) {
            jugar(s, l, "espiar");
            return;
        }
        if (!rapido && regresar[l] && yo > META && mano[l].size() > 2) {
            jugar(s, l, "regresar");
            return;
        }
        boolean pide = yo <= 13 || yo <= 16 && (otro >= yo - 1 || R.nextFloat() < 0.4F) || yo < otro && yo <= 18;
        jugar(s, l, pide && yo < META ? "pedir" : "plantarse");
    }

    // ------------------------------------------------------------------ cada tick

    private static void tick(MinecraftServer s) {
        srv = s;
        if (fase == Fase.NADA) {
            return;
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        t++;
        long dt = t - desde;
        switch (fase) {
            case PASILLO -> {
                String pas = adelante ? "pas_veintiuno" : "pas_piano";
                if (t % 10 == 0) {
                    boolean todos = true;
                    int n = 0;
                    for (Pieza p : ev.vivos()) {
                        Entity e = Evento.entidad(s, p);
                        if (e == null) {
                            continue;
                        }
                        n++;
                        boolean dentro = Cabina.dentro(s, pas, e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO);
                        if (p.bot && !dentro && dt >= 60) {
                            BlockPos c = Complejo.pos(s, pas + ".centro");
                            if (c != null) {
                                ir(s, e, c.getX() + 0.5 + R.nextInt(3) - 1, c.getY(), c.getZ() + 0.5 + R.nextInt(3) - 1, 90F);
                                dentro = true;
                            }
                        }
                        todos &= dentro;
                    }
                    if (n > 0 && todos || dt >= esperaPasillo) {
                        BlockPos c = Complejo.pos(s, pas + ".centro");
                        for (Pieza p : ev.vivos()) {
                            Entity e = Evento.entidad(s, p);
                            if (e != null && c != null && !Cabina.dentro(s, pas, e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                                ir(s, e, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 90F);
                            }
                        }
                        if (adelante) {
                            Adelante.abrir(s, "veintiuno", "pas_veintiuno", false);
                        } else {
                            puertas(s, pas, false);
                        }
                        fase = Fase.LLEGANDO;
                        desde = t + 1000000;
                        apagonFin = t + (adelante ? 30 : 50);
                    }
                }
            }
            case LLEGANDO -> {
                if (desde > t && t >= apagonFin) {
                    desde = t + 2000000;
                    apagonFin = Long.MAX_VALUE;
                    if (adelante) {
                        llegar(s);
                    } else {
                        Apagon.hacer(s, s.getPlayerList().getPlayers(), 60, () -> llegar(s));
                    }
                    break;
                }
                if (desde <= t && t % 10 == 0) {
                    boolean todos = true;
                    int n = 0;
                    for (Pieza p : ev.vivos()) {
                        Entity e = Evento.entidad(s, p);
                        if (e == null) {
                            continue;
                        }
                        n++;
                        if (p.bot && !enCuarto(s, e) && dt >= 60) {
                            BlockPos c = Complejo.pos(s, "v21.entrada");
                            if (c != null) {
                                ir(s, e, c.getX() + 0.5 + R.nextInt(3), c.getY(), c.getZ() + 0.5 - 2 + R.nextInt(5), 270F);
                            }
                        }
                        todos &= enCuarto(s, e);
                    }
                    if (n > 0 && todos || dt >= esperaSalon) {
                        hablar(s);
                    }
                }
            }
            case SENTAR -> {
                if (t >= apagonFin) {
                    if (jugador[0] == null || jugador[1] == null) {
                        fin(s);
                    } else if (!reglasDichas) {
                        reglas(s);
                    } else {
                        nuevaRonda(s);
                    }
                }
            }
            case REPARTE -> {
                // las cuatro cartas, una por una
                if (dt > 0 && dt <= 4 * 8 && dt % 8 == 0) {
                    sonidoMesa(s, propio("carta"), 1.4F, 0.9F + R.nextFloat() * 0.2F);
                }
                if (dt >= T_REPARTE) {
                    empezarTurno(s);
                }
            }
            case TURNO -> {
                // se le acabó el tiempo: se planta
                if (t - turnoDesde > segundosTurno * 20L) {
                    Coleccionista.LOGGER.info("COLECCIONISTA_V21 tiempo {}", nombre(s, turno));
                    jugar(s, turno, "plantarse");
                } else {
                    botTick(s);
                }
            }
            case REVELA -> {
                if (dt >= T_REVELA) {
                    if (perdedor < 0) {
                        nuevaRonda(s);
                    } else {
                        castigo(s);
                    }
                }
            }
            case CASTIGO -> {
                castigoTick(s, dt);
                if (dt >= (cortado ? T_CORTE : T_CASTIGO)) {
                    despuesCastigo(s);
                }
            }
            case FIN -> {
                if (aFin > 0 && t >= aFin) {
                    aFin = 0;
                    for (int k = 0; k < 2; k++) {
                        if (silla[k] != null) {
                            silla[k].cerrar(false, false);
                            silla[k].soltar();
                        }
                    }
                    Coleccionista.LOGGER.info("COLECCIONISTA_V21 sueltan");
                }
            }
            default -> {
            }
        }
        if (t % 20 == 0) {
            vigilar(s);
            enviar(s);
        }
    }

    /** Si alguien se para (o se reconecta), de vuelta a su silla; si un finalista se desconectó y es su turno, se planta solo. */
    private static void vigilar(MinecraftServer s) {
        if (fase.ordinal() < Fase.REGLAS.ordinal() || fase == Fase.FIN) {
            return;
        }
        Evento ev = Evento.get(s);
        for (int k = 0; k < 2; k++) {
            Pieza p = jugador[k] == null ? null : ev.pieza(jugador[k]);
            Entity e = p == null || !p.vivo() ? null : Evento.entidad(s, p);
            if (e != null && silla[k] != null && silla[k].isAlive() && e.getVehicle() != silla[k]) {
                sentar(s, e, silla[k]);
                silla[k].cerrar(true, false);
            }
        }
    }

    // ------------------------------------------------------------------ el final

    public static boolean terminar(MinecraftServer s) {
        if (fase == Fase.NADA || fase == Fase.FIN) {
            return false;
        }
        fin(s);
        return true;
    }

    private static void fin(MinecraftServer s) {
        Pantallas.callar(s);
        fase = Fase.FIN;
        desde = t;
        Evento ev = Evento.get(s);
        ev.reloj(s, 0);
        Pieza g = null;
        for (int k = 0; k < 2; k++) {
            Pieza p = jugador[k] == null ? null : ev.pieza(jugador[k]);
            if (p != null && p.vivo()) {
                g = p;
                if (maquina[k] != null && maquina[k].estado() != Maquina.ABIERTA) {
                    maquina[k].estado(Maquina.ABIERTA);
                }
            }
        }
        String quien = g == null ? "?" : g.nombre;
        Pantallas.hablar(s, "veintiuno_fin", () -> {
            Evento.get(s).prueba(s, Component.translatable(MOD + ".v21.fin_capitulo").getString());
            Coleccionista.LOGGER.info("COLECCIONISTA_CAPITULO7 fin vivos={}", Evento.get(s).vivos().size());
            if (fase == Fase.FIN) {
                aFin = t + 20;
            }
        });
        enviarTodo(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_V21 fin ganador={} duelos={}", quien, duelo);
    }

    // ------------------------------------------------------------------ lo que mandan los clientes

    private static void recibir(ServerPlayer p, CompoundTag d) {
        MinecraftServer s = p.server;
        int l = p.getUUID().equals(jugador[0]) ? 0 : p.getUUID().equals(jugador[1]) ? 1 : -1;
        if (l < 0) {
            return;
        }
        jugar(s, l, d.getString("que"));
    }

    // ------------------------------------------------------------------ el estado para los clientes

    private static void silla(CompoundTag e, String k, double[] c) {
        if (c == null) {
            return;
        }
        ListTag l = new ListTag();
        for (double v : c) {
            l.add(net.minecraft.nbt.DoubleTag.valueOf(v));
        }
        e.put(k, l);
    }

    private static int[] arr(List<Integer> l, int desdeI) {
        int[] a = new int[Math.max(0, l.size() - desdeI)];
        for (int i = desdeI; i < l.size(); i++) {
            a[i - desdeI] = l.get(i);
        }
        return a;
    }

    public static CompoundTag estado() {
        CompoundTag e = new CompoundTag();
        e.putString("fase", fase.name());
        e.putInt("ronda", ronda);
        e.putInt("duelo", duelo);
        e.putInt("turno", turno);
        e.putInt("golpesMax", golpesMax);
        e.putBoolean("comodines", comodines);
        e.putString("resultado", resultado);
        e.putInt("perdedor", perdedor);
        e.putInt("mazo", mazo.size());
        e.putString("accion", accion);
        e.putInt("accionDe", accionDe);
        e.putLong("accionEn", accionEn);
        e.putBoolean("cortado", cortado);
        if (srv != null) {
            long gt = srv.overworld().getGameTime();
            e.putLong("desde", gt - (t - desde));
            e.putLong("turnoDesde", gt - (t - turnoDesde));
            e.putInt("segundosTurno", segundosTurno);
            Evento ev = Evento.get(srv);
            for (int k = 0; k < 2; k++) {
                silla(e, "silla" + k, lugar(srv, k));
                if (jugador[k] == null) {
                    continue;
                }
                Pieza p = ev.pieza(jugador[k]);
                e.putUUID("id" + k, jugador[k]);
                e.putString("nombre" + k, p == null ? "?" : p.nombre);
                e.putBoolean("vivo" + k, p != null && p.vivo());
                e.put("visibles" + k, new IntArrayTag(arr(mano[k], 1)));
                e.putBoolean("tieneOculta" + k, !mano[k].isEmpty());
                e.putBoolean("plantado" + k, plantado[k]);
                e.putInt("golpes" + k, golpes[k]);
                e.putBoolean("espiar" + k, espiar[k]);
                e.putBoolean("regresar" + k, regresar[k]);
                e.putBoolean("espio" + k, espio[k]);
                e.putInt("cartas" + k, mano[k].size());
                // las tapadas se ven al voltearlas (y en el castigo y al final)
                if ((fase == Fase.REVELA || fase == Fase.CASTIGO || fase == Fase.FIN) && !mano[k].isEmpty()) {
                    e.putInt("oculta" + k, mano[k].get(0));
                    e.putInt("total" + k, total(k));
                }
            }
            ListTag c = new ListTag();
            for (UUID id : cola) {
                Pieza p = ev.pieza(id);
                c.add(net.minecraft.nbt.StringTag.valueOf(p == null ? "?" : p.nombre));
            }
            e.put("cola", c);
        }
        return e;
    }

    /** Lo de cada quien: su lado, su carta tapada (y la del otro si la espió) y si le toca. */
    private static void mio(MinecraftServer s, ServerPlayer sp) {
        CompoundTag d = new CompoundTag();
        int l = sp.getUUID().equals(jugador[0]) ? 0 : sp.getUUID().equals(jugador[1]) ? 1 : -1;
        if (fase != Fase.NADA && l >= 0) {
            d.putInt("lado", l);
            if (!mano[l].isEmpty()) {
                d.putInt("oculta", mano[l].get(0));
                d.putInt("total", total(l));
            }
            if (espio[l] && !mano[1 - l].isEmpty()) {
                d.putInt("espia", mano[1 - l].get(0));
            }
            d.putBoolean("turno", fase == Fase.TURNO && turno == l);
        }
        PlayerStore.Sync.toPlayer(sp, KEY_MIO, d);
    }

    private static void enviar(MinecraftServer s) {
        srv = s;
        PlayerStore.Sync.toAll(s, KEY, estado());
    }

    private static void enviarTodo(MinecraftServer s) {
        enviar(s);
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mio(s, sp);
        }
    }

    public static String resumen() {
        return fase.name() + " duelo=" + duelo + " ronda=" + ronda + " turno=" + turno + " golpes=" + golpes[0] + "/" + golpes[1] + " total="
                + (mano[0].isEmpty() ? "-" : total(0)) + "/" + (mano[1].isEmpty() ? "-" : total(1)) + " mazo=" + mazo.size();
    }

    public static Objetivo.Meta objetivo(MinecraftServer s, ServerPlayer sp) {
        if (fase == Fase.PASILLO && adelante) {
            Objetivo.Meta m = Adelante.meta(s, sp, "veintiuno", "pas_veintiuno");
            if (m != null) {
                return m;
            }
        } else if (fase == Fase.PASILLO && !Cabina.dentro(s, "pas_piano", sp, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
            return Objetivo.Meta.en(Complejo.pos(s, "pas_piano.centro"), 1.5, MOD + ".obj.pasillo");
        }
        if (fase == Fase.LLEGANDO && desde <= t && !enCuarto(s, sp)) {
            return Objetivo.Meta.en(Complejo.pos(s, "v21.entrada"), 1.5, MOD + ".obj.mesa_juego");
        }
        return null;
    }

    // ------------------------------------------------------------------ utilidades

    private static void ir(MinecraftServer s, Entity e, double x, double y, double z, float yaw) {
        e.stopRiding();
        e.fallDistance = 0;
        if (e instanceof ServerPlayer sp) {
            sp.teleportTo(s.overworld(), x, y, z, yaw, 0F);
        } else {
            if (e instanceof PiezaBot b) {
                b.getNavigation().stop();
            }
            e.moveTo(x, y, z, yaw, 0F);
        }
    }

    private static void sentar(MinecraftServer s, Entity e, Silla si) {
        if (si == null || !si.isAlive() || e.getVehicle() == si) {
            return;
        }
        if (e.getVehicle() instanceof Silla vieja) {
            vieja.soltar();
        }
        e.stopRiding();
        e.fallDistance = 0;
        if (e instanceof ServerPlayer sp) {
            sp.teleportTo(s.overworld(), si.getX(), si.getY() + 0.3, si.getZ(), si.getYRot(), 20F);
        } else {
            if (e instanceof PiezaBot b) {
                b.getNavigation().stop();
            }
            e.moveTo(si.getX(), si.getY() + 0.3, si.getZ(), si.getYRot(), 0F);
        }
        e.startRiding(si, true);
    }

    /** Quita todo: sillas, máquinas, la sangre y los cuerpos de la mesa. */
    private static void limpiarTodo(MinecraftServer s) {
        ServerLevel l = s.overworld();
        if (sillaTrono != null) {
            sillaTrono.soltar();
            sillaTrono.discard();
            sillaTrono = null;
        }
        for (Map.Entry<UUID, net.minecraft.world.level.GameType> e : modoAntes.entrySet()) {
            ServerPlayer sp = s.getPlayerList().getPlayer(e.getKey());
            if (sp != null) {
                sp.setGameMode(e.getValue());
            }
        }
        modoAntes.clear();
        for (int k = 0; k < 2; k++) {
            if (silla[k] != null) {
                silla[k].soltar();
                silla[k].discard();
                silla[k] = null;
            }
            if (maquina[k] != null) {
                maquina[k].discard();
                maquina[k] = null;
            }
            jugador[k] = null;
            mano[k].clear();
            golpes[k] = 0;
            plantado[k] = espiar[k] = regresar[k] = espio[k] = false;
        }
        BlockPos o = Complejo.origen(s, "veintiuno");
        if (o != null) {
            AABB box = new AABB(o.getX(), o.getY() - 2, o.getZ() - 2, o.getX() + 52, o.getY() + 16, o.getZ() + 46);
            for (Maquina m : l.getEntitiesOfClass(Maquina.class, box)) {
                m.discard();
            }
            for (Silla si : l.getEntitiesOfClass(Silla.class, box)) {
                if ("veintiuno".equals(si.grupo) || "trono".equals(si.grupo)) {
                    si.soltar();
                    si.discard();
                }
            }
            BlockPos m = Complejo.pos(s, "v21.mesa");
            if (m != null) {
                for (Cadaver c : l.getEntitiesOfClass(Cadaver.class, new AABB(m).inflate(4, 2, 3))) {
                    c.discard();
                }
                BlockState sangre = Complejo.estado(MOD + ":sangre");
                for (BlockPos q : BlockPos.betweenClosed(m.offset(-3, 0, -2), m.offset(3, 0, 2))) {
                    if (sangre != null && l.getBlockState(q).is(sangre.getBlock())) {
                        l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        cola.clear();
        mazo.clear();
        ronda = 0;
        duelo = 0;
        turno = 0;
        pasesSeguidos = 0;
        perdedor = -1;
        resultado = "";
        accion = "";
        accionDe = -1;
        reglasDichas = false;
        cortado = false;
        aFin = 0;
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            PlayerStore.Sync.toPlayer(sp, KEY_MIO, new CompoundTag());
        }
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase == Fase.HABLANDO || fase == Fase.REGLAS || fase == Fase.FIN) {
            Pantallas.callar(s);
        }
        if (fase != Fase.NADA) {
            Pantallas.modo(s, "apagada", "");
        }
        limpiarTodo(s);
        fase = Fase.NADA;
        enviar(s);
    }

    public static boolean saltar(MinecraftServer s) {
        switch (fase) {
            case PASILLO -> {
                llegar(s);
                return true;
            }
            case LLEGANDO -> {
                hablar(s);
                return true;
            }
            case HABLANDO -> {
                sentar(s, true);
                return true;
            }
            case REGLAS -> {
                nuevaRonda(s);
                return true;
            }
            case REPARTE -> {
                desde = t - T_REPARTE;
                return true;
            }
            case TURNO -> {
                return jugar(s, turno, "plantarse");
            }
            case REVELA -> {
                desde = t - T_REVELA;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        return fase == Fase.TURNO;
    }

    /** {@code /evento veintiuno accion <lado> <que>}: para las pruebas. */
    public static boolean accion(MinecraftServer s, int l, String que) {
        return jugar(s, l, que);
    }
}
