package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Plat;
import com.axeliscore.vestir.Vestuario;
import com.coleccionista.Coleccionista;
import com.coleccionista.bot.PiezaBot;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Cabina;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Cuerpos;
import com.coleccionista.evento.Escopeta;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Objetivo;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.evento.Silla;
import com.coleccionista.mapa.CajaMusica;
import com.coleccionista.mapa.Pasaje;
import com.coleccionista.registry.ModRegistry;
import com.coleccionista.voz.Voz;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Capítulo 3 · La caja de música (El traidor, rehecho). Llegan por el pasillo de servicio al salón; el Coleccionista explica y cada pieza
 * busca una góndola (abiertas, alrededor del salón) y se sienta sola. Cuando ya están todos sentados (o se acaba el tiempo) se cierran las
 * correas y se va la luz: en lo oscuro cada pieza recibe un símbolo en la espalda (círculo, cuadrado, triángulo o rombo: el suyo no lo puede
 * ver), su máscara y su papel (hay dos traidores que se conocen), y la caja los cambia de lugar. Luego, en seis turnos con apagones entre
 * ellos, van de dos en dos en las góndolas: el de atrás le ve la espalda al de adelante y le dice (en voz alta: cada góndola es un grupo
 * privado de voz, con la voz cambiada) qué símbolo lleva; los traidores pueden mentir. Dos turnos por pareja (cambian de asiento) y tres
 * parejas distintas.
 * <p>
 * Al final, otro apagón: todos aparecen sentados y atados en círculo en el escenario del centro, sin máscaras. Se abre la compuerta del
 * piso y sube la escopeta, girando. Se bajan las luces. La escopeta amaga, gira despacio y apunta a cada pieza (con un foco encima): dice
 * su símbolo (y lo marca); el organizador (que ve el símbolo real) decide: <b>Bien</b> o <b>Bala</b> ({@code /bala <pieza>}). Pase lo que
 * pase, primero viene la misma tensión (el zumbido, el latido, la carga): al final, o el disparo (y la silla se va de espaldas con la
 * pieza) o un clic en vacío. Si no hay organizador (o con {@code auto}), decide solo según lo que marcó. Después, el traidor que no hizo
 * caer a ninguno de los que tuvo enfrente se lleva la última bala. Una sola vez: los que sobreviven, sobreviven.
 */
public final class Caja {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":caja";
    public static final String KEY_MIO = MOD + ":caja_mio";
    public static final String KEY_ORG = MOD + ":caja_org";
    public static final String ACCION = MOD + ":caja_accion";
    public static final ResourceLocation ETIQUETA = new ResourceLocation(MOD, "caja_etiqueta");
    public static final ResourceLocation MASCARA = new ResourceLocation(MOD, "caja_mascara");
    public static final String PRUEBA = "La caja de música";
    public static final String[] SIMBOLOS = {"circulo", "cuadrado", "triangulo", "rombo"};
    public static final int TURNOS = 6;
    /** Ajustes (los cambia el organizador con /evento caja ...). */
    public static int segundosTurno = 45, segundosElegir = 25, traidores = 2, segundosSentarse = 75;
    /** Decide solo (según lo que marcó cada pieza) aunque haya organizador. */
    public static boolean auto;
    /** Para las pruebas: nunca decide sola (espera a /evento caja bien o bala). */
    public static boolean esperar;
    /** Para las pruebas: quiénes van primero en la revisión (en ese orden). */
    public static final List<UUID> primero = new ArrayList<>();
    /** Al terminar La mascota, pasan solos a la caja. */
    public static boolean encadenar = true;
    public static int esperaPasillo = 1200, esperaSalon = 900;
    /** Los tonos de voz para disfrazar (más grave o más aguda). */
    private static final float[] TONOS = {0.72F, 0.8F, 1.3F, 1.42F};

    public enum Fase {
        NADA, PASILLO, LLEGANDO, HABLANDO, SENTARSE, ROLES, APAGON, TURNO, CIRCULO, REVISION, TRAIDORES, FIN
    }

    /** Lo que pasa con la pieza apuntada: el arma gira, espera la respuesta, o (tras la misma tensión) dispara o hace clic. */
    enum Paso {
        GIRAR, ESPERA, BIEN, BALA
    }

    private static Fase fase = Fase.NADA;
    /** Si esta vez se llega caminando hacia adelante (por el pasillo de la sala anterior a la puerta de atrás del de servicio). */
    private static boolean adelante;
    /** Reloj propio (ticks, se detiene con la pausa del evento). */
    private static long t, desde;
    private static int turno = -1, apagones;
    private static long apagonMedio, apagonFin, turnoFin, susurroEn, sentarseFin;
    private static int sentados, total;
    private static Fase trasApagon = Fase.TURNO;
    private static final Random R = new Random();

    private static final Map<UUID, Integer> simbolo = new LinkedHashMap<>();
    private static final List<UUID> traidoresIds = new ArrayList<>();
    /** Las tres rondas: cada una, sus parejas ({a, b}; b null si va solo) y la góndola de cada pareja. */
    private static final List<List<UUID[]>> rondas = new ArrayList<>();
    private static final List<int[]> gondolasRonda = new ArrayList<>();
    /** Los asientos de las góndolas: [góndola][0 atrás, 1 adelante]. */
    private static final Silla[][] asientos = new Silla[CajaMusica.GONDOLAS][2];
    /** Dónde está cada pieza en este turno: {góndola, lado} (lado 0 atrás, 1 adelante, 2 solo). */
    private static final Map<UUID, int[]> lugar = new HashMap<>();
    /** Lo que le dijeron a cada pieza (para que los bots "decidan"; de los humanos se supone). */
    private static final Map<UUID, List<Integer>> dichos = new HashMap<>();
    /** A quién le dijo cada traidor (estando atrás). */
    private static final Map<UUID, Set<UUID>> oyentes = new HashMap<>();

    // la revisión
    private static final Map<UUID, Silla> circulo = new LinkedHashMap<>();
    private static final List<UUID> orden = new ArrayList<>();
    private static final List<UUID> pendientesTraidores = new ArrayList<>();
    private static int idx;
    private static Paso paso = Paso.GIRAR;
    private static long pasoDesde;
    private static final Map<UUID, Integer> eligio = new HashMap<>();
    private static final Set<UUID> fusilados = new HashSet<>();
    private static Escopeta arma;
    /** El foco (una luz fuerte) sobre la pieza apuntada, y si las luces del salón están bajas. */
    private static BlockPos foco;
    private static boolean lucesBajas;
    /** El amago: antes de apuntarle a quien sigue, a veces le apunta un momento a otro. 0 sin decidir, 1 va al falso, 2 se queda, 3 va al bueno. */
    private static int amagoEtapa;
    private static long amagoHasta;
    private static UUID amago;
    /** Los bots, al sentarse: desde cuándo caminan y a qué asiento ({góndola, lado}). */
    private static final Map<UUID, Long> botDesde = new HashMap<>();
    private static final Map<UUID, int[]> botVa = new HashMap<>();
    private static final List<BlockPos> sangre = new ArrayList<>();
    private static int balas, salvados;
    /** Cuándo pasar al capítulo 4 (0: no). */
    private static long aSorpresa;
    /** Cuántos grados a cada lado puede voltear la cabeza el de adelante de una góndola. */
    public static final float LIMITE_FRENTE = 50F;
    /** Las cortinas de las góndolas: cuándo se empezaron a correr (o -1) y si se cierran o se abren. */
    private static long cortinasEn = -1;
    private static boolean cortinasCerrar;
    private static final List<BlockPos> cortinas = new ArrayList<>();

    private Caja() {
    }

    public static void init() {
        AxelisCore.onServerTick(Caja::tick);
        AxelisCore.onLogin(p -> {
            PlayerStore.Sync.toPlayer(p, KEY, estado());
            if (fase != Fase.NADA) {
                mio(Plat.server(p), p);
                org(Plat.server(p));
            }
        });
        Actions.on(ACCION, Caja::accion);
        // para sentarse basta con darle clic derecho al sillón o al banquito (el asiento de verdad es invisible)
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (fase != Fase.SENTARSE || e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer sp)) {
                return;
            }
            int[] a = asientoEn(e.getPos());
            if (a != null) {
                e.setCanceled(true);
                intentarSentar(sp, a);
            }
        });
        // en la caja nadie se lastima: aquí solo se muere de un balazo
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

    /** ¿Las piezas están en la parte "a ciegas" (máscaras y sin nombres)? */
    public static boolean anonimo() {
        return fase == Fase.ROLES || fase == Fase.APAGON && trasApagon != Fase.CIRCULO || fase == Fase.TURNO;
    }

    // ------------------------------------------------------------------ del pasillo de la casa a la caja

    /** Al terminar La mascota: se abren las puertas del pasillo de la casa y el Coleccionista los llama. */
    public static Component llamar(MinecraftServer s) {
        if (!Complejo.construido(s, "caja")) {
            return Component.translatable(MOD + ".caja.sin_mapa");
        }
        if (!Complejo.construido(s, "mansion")) {
            return llegar(s);
        }
        Pruebas.cerrarOtras(s, "caja");
        limpiarTodo(s);
        adelante = Adelante.hay(s, "caja");
        if (adelante) {
            Adelante.abrir(s, "caja", "pas_caj", true);
        } else {
            puertas(s, "pas_man", true);
        }
        fase = Fase.PASILLO;
        desde = t;
        Evento ev = Evento.get(s);
        ev.prueba(s, Component.translatable(MOD + (adelante ? ".adelante.prueba" : ".caja.prueba_pasillo")).getString());
        ev.reloj(s, 0);
        Anuncio.titulo(s, Component.empty(), Component.translatable(MOD + (adelante ? ".adelante.titulo" : ".caja.al_pasillo")));
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA llamar");
        return null;
    }

    /** Los vivos pasan (sin que se note) del pasillo de la casa al de la caja y se abren las puertas al salón. */
    public static Component llegar(MinecraftServer s) {
        if (!Complejo.construido(s, "caja")) {
            return Component.translatable(MOD + ".caja.sin_mapa");
        }
        if (fase == Fase.NADA) {
            Pruebas.cerrarOtras(s, "caja");
            limpiarTodo(s);
        }
        Evento ev = Evento.get(s);
        boolean ad = Adelante.hay(s, "caja");
        List<Entity> enPasillo = new ArrayList<>(), fuera = new ArrayList<>();
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null) {
                continue;
            }
            if (ad && Cabina.dentro(s, "pas_caj", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                continue; // ya llegó caminando
            }
            if (Complejo.construido(s, "mansion") && Cabina.dentro(s, "pas_man", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                enPasillo.add(e);
            } else {
                fuera.add(e);
            }
        }
        if (!enPasillo.isEmpty()) {
            Cabina.trasladar(s, "pas_man", "pas_caj", enPasillo, List.of());
        }
        BlockPos c = Complejo.pos(s, "pas_caj.centro");
        int n = 0;
        for (Entity e : fuera) {
            if (c == null) {
                break;
            }
            ir(s, e, c.getX() + 0.5 - 1.5 + (n % 3) * 1.5, c.getY(), c.getZ() + 0.5 - 1 + (n / 3 % 4), 270F);
            n++;
        }
        puertas(s, "pas_man", false);
        puertas(s, "pas_caj", true);
        fase = Fase.LLEGANDO;
        desde = t;
        ev.prueba(s, Component.translatable(MOD + ".caja.prueba_llegar").getString());
        ev.reloj(s, 0);
        Pantallas.modo(s, "apagada", "");
        musica(s, 1.2F);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA llegan pasillo={} fuera={}", enPasillo.size(), fuera.size());
        return null;
    }

    private static void puertas(MinecraftServer s, String pasillo, boolean abrir) {
        puerta(s, pasillo + ".izq", abrir);
        puerta(s, pasillo + ".der", abrir);
    }

    private static void puerta(MinecraftServer s, String marca, boolean abrir) {
        ServerLevel l = s.overworld();
        for (String h : new String[] {".a", ".b"}) {
            BlockPos p = Complejo.pos(s, marca + h);
            if (p == null) {
                continue;
            }
            for (BlockPos q : new BlockPos[] {p, p.above()}) {
                BlockState st = l.getBlockState(q);
                if (st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
                        && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN) != abrir) {
                    l.setBlock(q, st.setValue(net.minecraft.world.level.block.DoorBlock.OPEN, abrir), 2 | 16);
                }
            }
        }
    }

    /** ¿Ya entró al salón (pasó las puertas del pasillo)? */
    private static boolean enSalon(MinecraftServer s, Entity e) {
        BlockPos o = Complejo.origen(s, "caja");
        if (o == null) {
            return false;
        }
        double x = e.getX() - o.getX(), z = e.getZ() - o.getZ();
        return x >= CajaMusica.HX0 && x <= CajaMusica.HX1 + 1 && z >= CajaMusica.HZ0 && z <= CajaMusica.HZ1 + 1;
    }

    // ------------------------------------------------------------------ el discurso y los papeles

    private static void hablar(MinecraftServer s) {
        fase = Fase.HABLANDO;
        desde = t;
        BlockPos c = Complejo.pos(s, "caja.entrada");
        for (Pieza p : Evento.get(s).vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null && c != null && !enSalon(s, e)) {
                ir(s, e, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 270F);
            }
        }
        puertas(s, "pas_caj", false);
        Escena.presentacion(s, "III", MOD + ".caja.titulo", MOD + ".caja.lema");
        Pantallas.soloGrande(s, true);
        Pantallas.hablar(s, "caja", () -> sentarse(s));
        Evento.get(s).prueba(s, PRUEBA);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA habla");
    }

    // ------------------------------------------------------------------ a sentarse

    /** Se acabó el discurso: aparecen los asientos de las góndolas y cada quien busca uno y se sienta (los bots caminan solos). */
    public static void sentarse(MinecraftServer s) {
        if (fase != Fase.HABLANDO && fase != Fase.LLEGANDO) {
            return;
        }
        if (fase == Fase.HABLANDO) {
            Pantallas.callar(s);
        }
        crearAsientos(s);
        fase = Fase.SENTARSE;
        desde = t;
        sentarseFin = t + segundosSentarse * 20L;
        botDesde.clear();
        botVa.clear();
        Evento ev = Evento.get(s);
        int n = 0;
        for (Pieza p : ev.vivos()) {
            if (p.bot) {
                botDesde.put(p.id, t + 30 + R.nextInt(170));
            }
            n++;
        }
        sentados = 0;
        total = n;
        puertas(s, "pas_caj", false);
        ev.reloj(s, segundosSentarse);
        ev.prueba(s, Component.translatable(MOD + ".caja.prueba_sentarse").getString());
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".caja.pantalla.sentarse").getString());
        Anuncio.titulo(s, Component.empty(), Component.translatable(MOD + ".caja.sientense"));
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA sentarse piezas={}", n);
    }

    /** Los 24 asientos de las góndolas (invisibles: el mueble es del mapa). */
    private static void crearAsientos(MinecraftServer s) {
        ServerLevel l = s.overworld();
        for (int g = 0; g < CajaMusica.GONDOLAS; g++) {
            for (int lado = 0; lado < 2; lado++) {
                if (asientos[g][lado] != null && asientos[g][lado].isAlive()) {
                    continue;
                }
                String m = "caja.gon." + g + (lado == 0 ? ".atras" : ".frente");
                BlockPos b = Complejo.pos(s, m);
                Silla si = ModRegistry.SILLA.get().create(l);
                if (b == null || si == null) {
                    continue;
                }
                float yaw = Complejo.yaw(m);
                si.moveTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, yaw, 0F);
                si.setYHeadRot(yaw);
                si.grupo = "caja";
                si.oculta(true);
                // el de adelante no se puede dar la vuelta (solo voltear un poco a los lados); el de atrás, libre
                si.limite(lado == 1 ? LIMITE_FRENTE : 0F);
                l.addFreshEntity(si);
                asientos[g][lado] = si;
            }
        }
    }

    /** ¿Ese bloque es un asiento de góndola (el sillón, su respaldo o el banquito)? Devuelve {góndola, lado}. */
    private static int[] asientoEn(BlockPos p) {
        for (int g = 0; g < asientos.length; g++) {
            for (int lado = 0; lado < 2; lado++) {
                Silla si = asientos[g][lado];
                if (si != null && si.isAlive() && (si.blockPosition().equals(p) || si.blockPosition().equals(p.below()))) {
                    return new int[] {g, lado};
                }
            }
        }
        return null;
    }

    /** ¿Puede sentarse en una góndola? (solo mientras buscan asiento, y si sigue viva). */
    public static boolean puedeSentarse(ServerPlayer sp) {
        Pieza p = Evento.get(sp.server).pieza(sp);
        return fase == Fase.SENTARSE && p != null && p.vivo();
    }

    private static void intentarSentar(ServerPlayer sp, int[] a) {
        Silla si = asientos[a[0]][a[1]];
        if (si == null || !si.isAlive() || sp.getVehicle() == si || !puedeSentarse(sp)) {
            return;
        }
        if (si.isVehicle()) {
            Plat.msg(sp, Component.translatable(MOD + ".caja.ocupado").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        sp.startRiding(si, true);
        sp.level().playSound(null, si.blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 0.7F, 0.7F);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA sienta {} gondola={} {}", sp.getGameProfile().getName(), a[0] + 1, a[1] == 0 ? "atras" : "frente");
    }

    /** ¿En qué asiento va sentado? ({góndola, lado} o null). */
    private static int[] sentadoEn(Entity e) {
        if (!(e.getVehicle() instanceof Silla v)) {
            return null;
        }
        for (int g = 0; g < asientos.length; g++) {
            for (int lado = 0; lado < 2; lado++) {
                if (asientos[g][lado] == v) {
                    return new int[] {g, lado};
                }
            }
        }
        return null;
    }

    /**
     * Un asiento libre para alguien que no se ha sentado: de preferencia junto a alguien que va solo (para que nadie se quede sin pareja), si
     * no, en una góndola vacía (el de atrás). No toma los que ya escogió algún bot que va caminando.
     */
    private static int[] elegirAsiento() {
        List<int[]> conUno = new ArrayList<>(), vacias = new ArrayList<>();
        for (int g = 0; g < asientos.length; g++) {
            int ocupados = 0;
            int libre = -1;
            for (int lado = 0; lado < 2; lado++) {
                Silla si = asientos[g][lado];
                if (si == null || !si.isAlive()) {
                    ocupados = 9;
                    break;
                }
                if (si.isVehicle() || reservado(g, lado)) {
                    ocupados++;
                } else {
                    libre = lado;
                }
            }
            if (ocupados == 1 && libre >= 0) {
                conUno.add(new int[] {g, libre});
            } else if (ocupados == 0) {
                vacias.add(new int[] {g, 0});
            }
        }
        if (!conUno.isEmpty()) {
            return conUno.get(R.nextInt(conUno.size()));
        }
        return vacias.isEmpty() ? null : vacias.get(R.nextInt(vacias.size()));
    }

    private static boolean reservado(int g, int lado) {
        for (int[] v : botVa.values()) {
            if (v[0] == g && v[1] == lado) {
                return true;
            }
        }
        return false;
    }

    /** Mientras buscan asiento: cuenta a los sentados y lleva a los bots (caminando) a su góndola. */
    private static void sentarseTick(MinecraftServer s) {
        Evento ev = Evento.get(s);
        int n = 0, sent = 0;
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null) {
                continue;
            }
            n++;
            if (sentadoEn(e) != null) {
                sent++;
                botVa.remove(p.id);
                continue;
            }
            Long d = botDesde.get(p.id);
            if (!p.bot || d == null || t < d) {
                continue;
            }
            int[] va = botVa.get(p.id);
            if (va == null || asientos[va[0]][va[1]] == null || asientos[va[0]][va[1]].isVehicle()) {
                botVa.remove(p.id);
                va = elegirAsiento();
                if (va == null) {
                    continue;
                }
                botVa.put(p.id, va);
            }
            BlockPos ent = Complejo.pos(s, "caja.gon." + va[0] + ".entrada");
            Silla si = asientos[va[0]][va[1]];
            double dist = ent == null ? 0 : e.distanceToSqr(ent.getX() + 0.5, ent.getY(), ent.getZ() + 0.5);
            if (dist < 2.5 * 2.5 || t - d > 220) {
                sentar(s, e, si);
                botVa.remove(p.id);
                sent++;
            } else if (e instanceof PiezaBot b && ent != null && (t - d) % 20 == 0) {
                b.getNavigation().moveTo(ent.getX() + 0.5, ent.getY(), ent.getZ() + 0.5, 1.0);
            }
        }
        boolean cambio = sent != sentados || n != total;
        sentados = sent;
        total = n;
        if (cambio) {
            enviar(s);
        }
        if (n > 0 && sent >= n || t >= sentarseFin) {
            todosSentados(s);
        }
    }

    /** Ya se sentaron (o se acabó el tiempo: a los que faltan los sienta la caja). Se cierran las correas y se va la luz. */
    private static void todosSentados(MinecraftServer s) {
        if (fase != Fase.SENTARSE) {
            return;
        }
        Evento ev = Evento.get(s);
        int auto = 0;
        botVa.clear();
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null || sentadoEn(e) != null) {
                continue;
            }
            int[] a = elegirAsiento();
            if (a == null) {
                continue;
            }
            sentar(s, e, asientos[a[0]][a[1]]);
            auto++;
        }
        for (Silla[] gg : asientos) {
            for (Silla si : gg) {
                if (si != null && si.isVehicle()) {
                    si.cerrar(true, true);
                }
            }
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA sentados auto={}", auto);
        correrCortinas(true);
        apagar(s, Fase.ROLES);
    }

    /** Para el organizador (y las pruebas): de donde vaya, directo a dar los papeles (sentando a todos). */
    public static void roles(MinecraftServer s) {
        if (fase == Fase.HABLANDO || fase == Fase.LLEGANDO) {
            sentarse(s);
        }
        if (fase == Fase.SENTARSE) {
            todosSentados(s);
        }
    }

    /** En lo oscuro: cada pieza recibe su símbolo (en la espalda), su máscara y su papel; dos traidores (que se conocen). */
    private static void asignarRoles(MinecraftServer s) {
        Evento ev = Evento.get(s);
        List<Pieza> v = new ArrayList<>(ev.vivos());
        simbolo.clear();
        traidoresIds.clear();
        Collections.shuffle(v, R);
        for (Pieza p : v) {
            int k = R.nextInt(SIMBOLOS.length);
            simbolo.put(p.id, k);
            CompoundTag d = new CompoundTag();
            d.putInt("s", k);
            Vestuario.poner(s, p.id, ETIQUETA, d);
            Vestuario.poner(s, p.id, MASCARA, new CompoundTag());
        }
        int nt = v.size() >= 6 ? traidores : v.size() >= 3 ? 1 : 0;
        for (int i = 0; i < nt && i < v.size(); i++) {
            traidoresIds.add(v.get(i).id);
        }
        StringBuilder sb = new StringBuilder();
        for (UUID id : traidoresIds) {
            Pieza p = ev.pieza(id);
            sb.append(p == null ? id : p.nombre).append(' ');
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA roles piezas={} traidores={} [{}]", v.size(), traidoresIds.size(), sb.toString().trim());
    }

    /** Se prende la luz: cada quien ya está en su lugar del primer turno y ve su carta (con su papel). */
    private static void mostrarRoles(MinecraftServer s) {
        fase = Fase.ROLES;
        desde = t;
        Evento.get(s).prueba(s, PRUEBA);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".caja.pantalla.roles").getString());
        for (Pieza p : Evento.get(s).vivos()) {
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                mio(s, sp);
                sp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.MASTER, 1F, 1F);
            }
        }
        org(s);
        enviar(s);
    }

    // ------------------------------------------------------------------ las parejas y los turnos

    /** Tres rondas de parejas sin repetir (si se puede); el impar va solo (cada vez uno distinto). */
    private static void parejas(List<UUID> ids) {
        rondas.clear();
        gondolasRonda.clear();
        Set<String> usadas = new HashSet<>();
        Set<UUID> yaSolos = new HashSet<>();
        for (int r = 0; r < 3; r++) {
            List<UUID[]> mejor = null;
            int peor = Integer.MAX_VALUE;
            for (int intento = 0; intento < 300 && peor > 0; intento++) {
                List<UUID> l = new ArrayList<>(ids);
                Collections.shuffle(l, R);
                // el que va solo: alguien que no haya ido solo
                if (l.size() % 2 == 1) {
                    for (int i = 0; i < l.size(); i++) {
                        if (!yaSolos.contains(l.get(i))) {
                            l.add(l.remove(i));
                            break;
                        }
                    }
                }
                List<UUID[]> ps = new ArrayList<>();
                int rep = 0;
                for (int i = 0; i + 1 < l.size(); i += 2) {
                    ps.add(new UUID[] {l.get(i), l.get(i + 1)});
                    if (usadas.contains(par(l.get(i), l.get(i + 1)))) {
                        rep++;
                    }
                }
                if (l.size() % 2 == 1) {
                    ps.add(new UUID[] {l.get(l.size() - 1), null});
                }
                if (rep < peor) {
                    peor = rep;
                    mejor = ps;
                }
            }
            if (mejor == null) {
                mejor = new ArrayList<>();
            }
            for (UUID[] p : mejor) {
                if (p[1] == null) {
                    yaSolos.add(p[0]);
                } else {
                    usadas.add(par(p[0], p[1]));
                }
            }
            rondas.add(mejor);
            // el carrusel "gira": cada ronda, otras góndolas
            List<Integer> gs = new ArrayList<>();
            for (int g = 0; g < CajaMusica.GONDOLAS; g++) {
                gs.add(g);
            }
            Collections.shuffle(gs, R);
            int[] asig = new int[mejor.size()];
            for (int i = 0; i < asig.length; i++) {
                asig[i] = i < gs.size() ? gs.get(i) : -1;
            }
            gondolasRonda.add(asig);
        }
    }

    private static String par(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    /** En el primer apagón: las parejas de las tres rondas y la voz cambiada de cada quien (toda la prueba). */
    private static void prepararTurnos(MinecraftServer s) {
        Evento ev = Evento.get(s);
        List<UUID> ids = new ArrayList<>();
        for (Pieza p : ev.vivos()) {
            if (simbolo.containsKey(p.id)) {
                ids.add(p.id);
            }
        }
        parejas(ids);
        dichos.clear();
        oyentes.clear();
        Map<UUID, Float> tonos = new HashMap<>();
        for (UUID id : ids) {
            tonos.put(id, TONOS[R.nextInt(TONOS.length)]);
        }
        Voz.tonos(tonos);
        turno = -1;
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA turnos piezas={} parejas={}", ids.size(), rondas.isEmpty() ? 0 : rondas.get(0).size());
    }

    /** Se apaga la luz: a la mitad del apagón se mueven las piezas (nadie ve nada) y al final empieza lo que sigue. */
    private static void apagar(MinecraftServer s, Fase despues) {
        fase = Fase.APAGON;
        trasApagon = despues;
        desde = t;
        apagones++;
        int dura = despues == Fase.CIRCULO ? 100 : 70;
        apagonMedio = t + dura / 2;
        apagonFin = t + dura;
        Evento.get(s).reloj(s, 0);
        // el mecanismo de la caja: se le da cuerda (clic, clic, clic) y la musiquita se tuerce
        for (Pieza p : Evento.get(s).vivos()) {
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                sp.playNotifySound(SoundEvents.LEVER_CLICK, SoundSource.MASTER, 0.9F, 0.5F);
                sp.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.MASTER, 0.6F, 0.5F);
            }
        }
        CompoundTag d = new CompoundTag();
        d.putInt("n", apagones);
        d.putInt("dura", dura);
        PlayerStore.Sync.toAll(s, KEY + "_apagon", d);
        enviar(s);
    }

    /** A la mitad del apagón: el siguiente turno (o el círculo). */
    private static void enLaOscuridad(MinecraftServer s) {
        if (trasApagon == Fase.CIRCULO) {
            alCirculo(s);
            return;
        }
        if (trasApagon == Fase.ROLES) {
            asignarRoles(s);
            prepararTurnos(s);
        }
        turno++;
        acomodar(s);
    }

    /** Sienta a cada quien en su lugar de este turno. */
    private static void acomodar(MinecraftServer s) {
        lugar.clear();
        int r = Math.min(2, turno / 2), h = turno % 2;
        List<UUID[]> ps = rondas.size() > r ? rondas.get(r) : List.of();
        int[] gs = gondolasRonda.size() > r ? gondolasRonda.get(r) : new int[0];
        Map<UUID, Integer> grupos = new HashMap<>();
        for (int i = 0; i < ps.size(); i++) {
            UUID[] par = ps.get(i);
            int g = i < gs.length ? gs[i] : -1;
            if (g < 0) {
                continue;
            }
            if (par[1] == null) {
                lugar.put(par[0], new int[] {g, 2});
            } else {
                UUID frente = h == 0 ? par[0] : par[1], atras = h == 0 ? par[1] : par[0];
                lugar.put(frente, new int[] {g, 1});
                lugar.put(atras, new int[] {g, 0});
            }
            grupos.put(par[0], g + 1);
            if (par[1] != null) {
                grupos.put(par[1], g + 1);
            }
        }
        Voz.separar(grupos);
        for (Silla[] gg : asientos) {
            for (Silla si : gg) {
                if (si != null) {
                    si.soltar();
                    si.cerrar(false, false);
                }
            }
        }
        Evento ev = Evento.get(s);
        for (Map.Entry<UUID, int[]> e : lugar.entrySet()) {
            Pieza p = ev.pieza(e.getKey());
            Entity ent = p == null ? null : Evento.entidad(s, p);
            if (ent != null) {
                sentar(s, ent, asiento(e.getValue()));
            }
        }
        int montados = 0;
        for (Silla[] gg : asientos) {
            for (Silla si : gg) {
                if (si != null && si.isVehicle()) {
                    si.cerrar(true, false);
                    montados += si.getPassengers().size();
                }
            }
        }
        for (Pieza p : ev.vivos()) {
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                mio(s, sp);
            }
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA turno {} ronda={} sentados={} montados={}", turno + 1, r + 1, lugar.size(), montados);
    }

    private static Silla asiento(int[] lu) {
        int g = lu[0], lado = lu[1] == 0 ? 0 : 1;
        return g >= 0 && g < asientos.length ? asientos[g][lado] : null;
    }

    private static void sentar(MinecraftServer s, Entity e, Silla si) {
        if (si == null || !si.isAlive()) {
            return;
        }
        if (e.getVehicle() == si) {
            return;
        }
        if (e.getVehicle() instanceof Silla vieja) {
            vieja.soltar();
        }
        e.stopRiding();
        e.fallDistance = 0;
        if (e instanceof ServerPlayer sp) {
            sp.teleportTo(s.overworld(), si.getX(), si.getY() + 0.3, si.getZ(), si.getYRot(), 0F);
        } else {
            if (e instanceof PiezaBot b) {
                b.getNavigation().stop();
            }
            e.moveTo(si.getX(), si.getY() + 0.3, si.getZ(), si.getYRot(), 0F);
        }
        e.startRiding(si, true);
    }

    /** Se prende la luz: empieza el turno. */
    private static void empezarTurno(MinecraftServer s) {
        fase = Fase.TURNO;
        desde = t;
        turnoFin = t + segundosTurno * 20L;
        susurroEn = t + 50 + R.nextInt(80);
        Evento ev = Evento.get(s);
        ev.reloj(s, segundosTurno);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".caja.pantalla.turno", turno + 1, TURNOS).getString());
        // quién le dice a quién (el de atrás al de adelante)
        for (Map.Entry<UUID, int[]> e : lugar.entrySet()) {
            if (e.getValue()[1] != 0) {
                continue;
            }
            UUID atras = e.getKey(), frente = quienEnfrente(e.getValue()[0]);
            if (frente == null) {
                continue;
            }
            if (traidoresIds.contains(atras)) {
                oyentes.computeIfAbsent(atras, k -> new HashSet<>()).add(frente);
            }
            // lo que "le dijo" (de los bots es exacto; de las personas se supone: un traidor miente)
            Integer real = simbolo.get(frente);
            if (real != null) {
                int dicho = traidoresIds.contains(atras) ? otroSimbolo(real) : real;
                dichos.computeIfAbsent(frente, k -> new ArrayList<>()).add(dicho);
            }
        }
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA luz turno={}", turno + 1);
    }

    private static int otroSimbolo(int real) {
        int k = R.nextInt(SIMBOLOS.length - 1);
        return k >= real ? k + 1 : k;
    }

    private static UUID quienEnfrente(int g) {
        for (Map.Entry<UUID, int[]> e : lugar.entrySet()) {
            if (e.getValue()[0] == g && e.getValue()[1] == 1) {
                return e.getKey();
            }
        }
        return null;
    }

    private static UUID quienAtras(int g) {
        for (Map.Entry<UUID, int[]> e : lugar.entrySet()) {
            if (e.getValue()[0] == g && e.getValue()[1] == 0) {
                return e.getKey();
            }
        }
        return null;
    }

    /** Los bots que van atrás le "susurran" al de adelante (si es persona) el símbolo que le ven (o uno falso, si son traidores). */
    private static void susurrar(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Map.Entry<UUID, int[]> e : lugar.entrySet()) {
            if (e.getValue()[1] != 0) {
                continue;
            }
            Pieza atras = ev.pieza(e.getKey());
            UUID fid = quienEnfrente(e.getValue()[0]);
            Pieza frente = fid == null ? null : ev.pieza(fid);
            if (atras == null || !atras.bot || frente == null || !(Evento.entidad(s, frente) instanceof ServerPlayer sp)) {
                continue;
            }
            List<Integer> l = dichos.get(fid);
            int dicho = l == null || l.isEmpty() ? simbolo.getOrDefault(fid, 0) : l.get(l.size() - 1);
            Component sim = Component.translatable(MOD + ".caja.simbolo." + SIMBOLOS[dicho]);
            Plat.msg(sp, Component.translatable(MOD + ".caja.susurro", sim).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            sp.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1F, 1.4F);
            CompoundTag d = new CompoundTag();
            d.putString("simbolo", SIMBOLOS[dicho]);
            d.putInt("n", (int) t);
            PlayerStore.Sync.toPlayer(sp, KEY + "_susurro", d);
        }
    }

    /** Se acabó el tiempo del turno: apagón y al siguiente (o al círculo). */
    private static void finTurno(MinecraftServer s) {
        if (turno + 1 < TURNOS) {
            apagar(s, Fase.TURNO);
        } else {
            apagar(s, Fase.CIRCULO);
        }
    }

    // ------------------------------------------------------------------ las cortinas de las góndolas

    /** Empieza a correr las cortinas de todas las góndolas (se cierran de las orillas al centro, como en un vestidor). */
    private static void correrCortinas(boolean cerrar) {
        cortinasEn = t;
        cortinasCerrar = cerrar;
    }

    private static void cortinasTick(MinecraftServer s) {
        if (cortinasEn < 0) {
            return;
        }
        long dt = t - cortinasEn;
        // primero las dos orillas, luego el centro
        if (dt == 0) {
            cortinas(s, cortinasCerrar, 0);
        } else if (dt == 5) {
            cortinas(s, cortinasCerrar, 1);
            cortinasEn = -1;
            Coleccionista.LOGGER.info("COLECCIONISTA_CAJA cortinas {} {}", cortinasCerrar ? "cerradas" : "abiertas", cortinas.size());
        }
    }

    /**
     * Cierra o abre la cortina de terciopelo en la entrada de cada góndola (dos cortinas altas una sobre otra, de 3 de ancho, justo afuera:
     * tapan del piso al toldo). {@code paso} 0: las orillas, 1: el centro, -1: todo de una vez (sin sonido).
     */
    private static void cortinas(MinecraftServer s, boolean cerrar, int paso) {
        ServerLevel l = s.overworld();
        Block b = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation(MOD, "cortina"));
        if (!cerrar) {
            if (paso != 0 && b != null) {
                // las que puso esta vez y, por si el servidor se reinició a la mitad, las de todas las entradas
                List<BlockPos> todas = new ArrayList<>(cortinas);
                int[][] gs = CajaMusica.gondolas();
                for (int g = 0; g < gs.length; g++) {
                    BlockPos ent = Complejo.pos(s, "caja.gon." + g + ".entrada");
                    if (ent == null) {
                        continue;
                    }
                    int yaw = gs[g][2];
                    int dx = yaw == 270 ? 1 : yaw == 90 ? -1 : 0, dz = yaw == 180 ? -1 : yaw == 0 ? 1 : 0;
                    int lx = dz != 0 ? 1 : 0, lz = dx != 0 ? 1 : 0;
                    for (int i = -1; i <= 1; i++) {
                        for (int y = 0; y <= 3; y++) {
                            todas.add(ent.offset(-dx + i * lx, y, -dz + i * lz));
                        }
                    }
                }
                int quitadas = 0;
                for (BlockPos q : todas) {
                    if (l.getBlockState(q).is(b)) {
                        l.setBlock(q, Blocks.AIR.defaultBlockState(), 2 | 16);
                        quitadas++;
                    }
                }
                if (quitadas > 0 && paso >= 0) {
                    sonidoCortinas(s);
                }
                cortinas.clear();
            }
            return;
        }
        if (b == null || b == Blocks.AIR) {
            return;
        }
        int[][] gs = CajaMusica.gondolas();
        for (int g = 0; g < gs.length; g++) {
            BlockPos ent = Complejo.pos(s, "caja.gon." + g + ".entrada");
            if (ent == null) {
                continue;
            }
            int yaw = gs[g][2];
            // hacia la pared de la góndola; la cortina va un bloque afuera y mira hacia el salón (su tela queda en la entrada)
            int dx = yaw == 270 ? 1 : yaw == 90 ? -1 : 0, dz = yaw == 180 ? -1 : yaw == 0 ? 1 : 0;
            net.minecraft.core.Direction afuera = net.minecraft.core.Direction.fromYRot(yaw).getOpposite();
            int lx = dz != 0 ? 1 : 0, lz = dx != 0 ? 1 : 0;
            for (int i = -1; i <= 1; i++) {
                if ((paso == 0 && i == 0) || (paso == 1 && i != 0)) {
                    continue;
                }
                for (int y = 0; y <= 2; y += 2) {
                    BlockPos lo = ent.offset(-dx + i * lx, y, -dz + i * lz);
                    if (!l.getBlockState(lo).isAir() || !l.getBlockState(lo.above()).isAir()) {
                        continue;
                    }
                    BlockState st = b.defaultBlockState();
                    if (st.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
                        st = st.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, afuera);
                    }
                    BlockState arriba = st;
                    if (st.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                        st = st.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,
                                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
                        arriba = st.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF,
                                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
                    }
                    l.setBlock(lo, st, 2 | 16);
                    l.setBlock(lo.above(), arriba, 2 | 16);
                    cortinas.add(lo);
                    cortinas.add(lo.above());
                }
            }
        }
        if (paso >= 0) {
            sonidoCortinas(s);
        }
    }

    private static void sonidoCortinas(MinecraftServer s) {
        for (Pieza p : Evento.get(s).vivos()) {
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                sp.playNotifySound(SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.9F, 0.7F);
                sp.playNotifySound(SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.BLOCKS, 0.8F, 0.6F);
            }
        }
    }

    // ------------------------------------------------------------------ el círculo y el arma

    private static void alCirculo(MinecraftServer s) {
        Evento ev = Evento.get(s);
        ServerLevel l = s.overworld();
        // fuera las góndolas, las máscaras y las voces raras
        for (Silla[] gg : asientos) {
            for (int i = 0; i < gg.length; i++) {
                if (gg[i] != null) {
                    gg[i].soltar();
                    gg[i].discard();
                    gg[i] = null;
                }
            }
        }
        lugar.clear();
        cortinas(s, false, -1);
        Voz.juntar();
        Voz.tonos(Map.of());
        Vestuario.quitarDeTodos(s, MASCARA);
        BlockPos c = Complejo.pos(s, "caja.escenario");
        List<Pieza> v = new ArrayList<>();
        for (Pieza p : ev.vivos()) {
            if (simbolo.containsKey(p.id)) {
                v.add(p);
            }
        }
        Collections.shuffle(v, R);
        circulo.clear();
        orden.clear();
        int n = v.size();
        for (int i = 0; i < n && c != null; i++) {
            double a = Math.PI * 2 * i / Math.max(1, n);
            double x = c.getX() + 0.5 + Math.sin(a) * CajaMusica.RADIO_CIRCULO, z = c.getZ() + 0.5 + Math.cos(a) * CajaMusica.RADIO_CIRCULO;
            // mirando al centro
            float yaw = (float) Math.toDegrees(Math.atan2(-(c.getX() + 0.5 - x), c.getZ() + 0.5 - z));
            Silla si = ModRegistry.SILLA.get().create(l);
            if (si == null) {
                continue;
            }
            si.moveTo(x, c.getY(), z, yaw, 0F);
            si.setYHeadRot(yaw);
            si.grupo = "caja";
            l.addFreshEntity(si);
            Pieza p = v.get(i);
            circulo.put(p.id, si);
            orden.add(p.id);
            Entity e = Evento.entidad(s, p);
            if (e != null) {
                sentar(s, e, si);
            }
            si.cerrar(true, false);
        }
        // la escopeta: guardada en el foso, parada (apuntando al techo), con la culata hacia un lado
        BlockPos ap = Complejo.pos(s, "caja.arma");
        if (ap != null && arma == null) {
            Escopeta a = ModRegistry.ESCOPETA.get().create(l);
            if (a != null) {
                a.moveTo(ap.getX() + 0.5, ap.getY(), ap.getZ() + 0.5, 90F, -90F);
                l.addFreshEntity(a);
                arma = a;
            }
        }
        for (Pieza p : ev.vivos()) {
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                mio(s, sp);
            }
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA circulo piezas={}", circulo.size());
    }

    /** Se prende la luz en el escenario: el Coleccionista habla, se abre la compuerta y sube la escopeta (ver el tick del círculo). */
    private static void enCirculo(MinecraftServer s) {
        fase = Fase.CIRCULO;
        desde = t;
        Pantallas.modo(s, "apagada", "");
        Pantallas.hablar(s, "caja_revision", () -> {
            if (fase == Fase.CIRCULO) {
                revisar(s);
            }
        });
        enviar(s);
    }

    /** Abre o cierra la compuerta del piso (las 9 trampillas). */
    private static void compuerta(MinecraftServer s, boolean abrir) {
        ServerLevel l = s.overworld();
        int cambian = 0;
        for (int k = 0; k < 9; k++) {
            BlockPos p = Complejo.pos(s, "caja.compuerta." + k);
            if (p == null) {
                continue;
            }
            BlockState st = l.getBlockState(p);
            if (st.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock
                    && st.getValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN) != abrir) {
                l.setBlock(p, st.setValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN, abrir), 2);
                cambian++;
            }
        }
        if (cambian == 0) {
            return;
        }
        BlockPos c = Complejo.pos(s, "caja.escenario");
        if (c != null) {
            l.playSound(null, c, propio("caja_compuerta"), SoundSource.HOSTILE, 2.5F, abrir ? 1F : 0.8F);
            l.playSound(null, c, abrir ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.HOSTILE, 1.5F, 0.5F);
            if (abrir) {
                l.sendParticles(ParticleTypes.CLOUD, c.getX() + 0.5, c.getY() + 0.1, c.getZ() + 0.5, 30, 0.8, 0.1, 0.8, 0.02);
                l.sendParticles(ParticleTypes.LARGE_SMOKE, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 12, 0.6, 0.1, 0.6, 0.01);
            }
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA compuerta {}", abrir ? "abre" : "cierra");
    }

    /** Baja (o vuelve a subir) las luces del salón: en la revisión casi todo queda a oscuras, menos el foco. */
    private static void luces(MinecraftServer s, boolean bajas) {
        if (lucesBajas == bajas) {
            return;
        }
        lucesBajas = bajas;
        ServerLevel l = s.overworld();
        for (int i = 0; i < 400; i++) {
            BlockPos p = Complejo.pos(s, "caja.luz." + i);
            if (p == null) {
                break;
            }
            int nivel = Math.round(Complejo.yaw("caja.luz." + i));
            // el salón casi a oscuras; las del escenario (13) se quedan a media luz
            int n = bajas ? (nivel >= 13 ? 8 : Math.max(2, nivel / 4)) : net.minecraft.util.Mth.clamp(nivel, 0, 15);
            BlockState st = l.getBlockState(p);
            if (st.isAir() || st.is(Blocks.LIGHT)) {
                l.setBlock(p, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, n), 2);
            }
        }
        // una luz junto a la escopeta (para que siempre se vea, aunque todo lo demás esté oscuro)
        BlockPos esc = Complejo.pos(s, "caja.escenario");
        if (esc != null) {
            BlockPos q = esc.above(4);
            BlockState st = l.getBlockState(q);
            if (bajas && st.isAir()) {
                l.setBlock(q, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 11), 2);
            } else if (!bajas && st.is(Blocks.LIGHT)) {
                l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        if (bajas) {
            for (Pieza p : Evento.get(s).vivos()) {
                if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                    sp.playNotifySound(SoundEvents.BEACON_DEACTIVATE, SoundSource.MASTER, 0.8F, 0.5F);
                }
            }
        }
    }

    /** El foco: una luz fuerte arriba de la silla de la pieza apuntada (null: se apaga). */
    private static void foco(MinecraftServer s, Entity e) {
        ServerLevel l = s.overworld();
        if (foco != null && l.getBlockState(foco).is(Blocks.LIGHT)) {
            l.setBlock(foco, Blocks.AIR.defaultBlockState(), 2);
        }
        foco = null;
        if (e == null) {
            return;
        }
        BlockPos p = BlockPos.containing(e.getX(), (e.getVehicle() != null ? e.getVehicle().getY() : e.getY()) + 3, e.getZ());
        if (l.getBlockState(p).isAir()) {
            l.setBlock(p, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15), 2);
            foco = p;
            l.playSound(null, p, SoundEvents.LEVER_CLICK, SoundSource.HOSTILE, 1F, 0.6F);
        }
    }

    private static net.minecraft.sounds.SoundEvent propio(String id) {
        return net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD, id));
    }

    private static void revisar(MinecraftServer s) {
        fase = Fase.REVISION;
        desde = t;
        idx = -1;
        if (arma != null && arma.guardada()) {
            compuerta(s, true);
            arma.subir();
        }
        luces(s, true);
        for (int i = primero.size() - 1; i >= 0; i--) {
            if (orden.remove(primero.get(i))) {
                orden.add(0, primero.get(i));
            }
        }
        primero.clear();
        Evento.get(s).prueba(s, Component.translatable(MOD + ".caja.prueba_revision").getString());
        siguiente(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA revision piezas={}", orden.size());
    }

    /** La siguiente pieza del círculo (o los traidores, o el final). */
    private static void siguiente(MinecraftServer s) {
        Evento ev = Evento.get(s);
        while (true) {
            idx++;
            List<UUID> l = fase == Fase.TRAIDORES ? pendientesTraidores : orden;
            if (idx >= l.size()) {
                if (fase == Fase.REVISION) {
                    aTraidores(s);
                } else {
                    terminar(s);
                }
                return;
            }
            Pieza p = ev.pieza(l.get(idx));
            if (p != null && p.vivo()) {
                break;
            }
        }
        paso = Paso.GIRAR;
        pasoDesde = t;
        amagoEtapa = 0;
        amago = null;
        foco(s, null);
        enviar(s);
        org(s);
    }

    /** La escopeta ya está arriba: a veces primero le apunta a otro (un momento), y luego al que sigue. */
    private static void empezarApunte(MinecraftServer s, UUID id) {
        Evento ev = Evento.get(s);
        Entity real = Evento.entidad(s, ev.pieza(id));
        List<Entity> otros = new ArrayList<>();
        for (UUID o : circulo.keySet()) {
            Pieza p = ev.pieza(o);
            Entity e = p == null || !p.vivo() || o.equals(id) ? null : Evento.entidad(s, p);
            if (e != null) {
                otros.add(e);
            }
        }
        if (!otros.isEmpty() && R.nextFloat() < 0.6F) {
            Entity f = otros.get(R.nextInt(otros.size()));
            amago = f.getUUID();
            arma.apuntarA(f.getEyePosition());
            amagoEtapa = 1;
        } else if (real != null) {
            arma.apuntarA(real.getEyePosition());
            amagoEtapa = 3;
        } else {
            amagoEtapa = 3;
        }
    }

    /** La pieza a la que apunta el arma ahora (null: a nadie). */
    public static UUID apuntado() {
        List<UUID> l = fase == Fase.TRAIDORES ? pendientesTraidores : fase == Fase.REVISION ? orden : null;
        return l == null || idx < 0 || idx >= l.size() ? null : l.get(idx);
    }

    /** ¿Hay alguien que decida (un organizador conectado que no está jugando)? */
    private static boolean hayJuez(MinecraftServer s) {
        if (auto) {
            return false;
        }
        Evento ev = Evento.get(s);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            Pieza pz = ev.pieza(p);
            if ((pz == null || !pz.vivo()) && (ev.esOrganizador(p.getUUID()) || Plat.isOp(p))) {
                return true;
            }
        }
        return false;
    }

    /** Lo que decidió el organizador (o la prueba sola) para la pieza apuntada. */
    public static boolean decidir(MinecraftServer s, boolean bala) {
        if (fase != Fase.REVISION && fase != Fase.TRAIDORES || paso != Paso.ESPERA && paso != Paso.GIRAR) {
            return false;
        }
        UUID id = apuntado();
        if (id == null) {
            return false;
        }
        paso = bala ? Paso.BALA : Paso.BIEN;
        pasoDesde = t;
        Evento ev = Evento.get(s);
        Pieza p = ev.pieza(id);
        // pase lo que pase, primero la misma tensión (nadie sabe qué viene): el zumbido que sube y el latido
        sonidoArma(s, propio("caja_tension"), 3F, 1F);
        sinElegir(s, id);
        enviar(s);
        org(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA decide {} {}", p == null ? id : p.nombre, bala ? "bala" : "bien");
        return true;
    }

    /** {@code /bala <pieza>}: si es a la que apunta el arma, bala; si no, avisa a quién apunta. */
    public static Component bala(MinecraftServer s, Pieza p) {
        UUID id = apuntado();
        if (id == null) {
            return Component.translatable(MOD + ".caja.cmd.nadie");
        }
        if (p != null && !p.id.equals(id)) {
            Pieza a = Evento.get(s).pieza(id);
            return Component.translatable(MOD + ".caja.cmd.apunta_a", a == null ? "?" : a.nombre);
        }
        return decidir(s, true) ? null : Component.translatable(MOD + ".caja.cmd.espera");
    }

    private static void sinElegir(MinecraftServer s, UUID id) {
        Pieza p = Evento.get(s).pieza(id);
        if (p != null && Evento.entidad(s, p) instanceof ServerPlayer sp) {
            mio(s, sp);
        }
    }

    /** ¡Bang! Fogonazo, sangre, y la silla se va de espaldas con la pieza (si no se trabó el arma por ser de las últimas). */
    private static void disparar(MinecraftServer s, UUID id) {
        Evento ev = Evento.get(s);
        Pieza p = ev.pieza(id);
        Entity e = p == null ? null : Evento.entidad(s, p);
        ServerLevel l = s.overworld();
        boolean traidor = traidoresIds.contains(id);
        String motivo = Component.translatable(MOD + (traidor ? ".caja.motivo_traidor" : ".caja.motivo")).getString();
        if (e != null && e.getVehicle() instanceof Silla) {
            Cuerpos.EN_SILLA.add(id);
        }
        if (p != null && ev.eliminarEnPrueba(s, p, motivo)) {
            balas++;
            fusilados.add(id);
            if (arma != null) {
                arma.disparar();
                Vec3 m = arma.punta();
                l.sendParticles(ParticleTypes.FLASH, m.x, m.y, m.z, 1, 0, 0, 0, 0);
                l.sendParticles(ParticleTypes.LARGE_SMOKE, m.x, m.y, m.z, 14, 0.15, 0.15, 0.15, 0.03);
                l.sendParticles(ParticleTypes.SMOKE, m.x, m.y, m.z, 20, 0.3, 0.2, 0.3, 0.05);
            }
            sonidoArma(s, propio("caja_disparo"), 6F, 1F);
            sonidoArma(s, SoundEvents.GENERIC_EXPLODE, 1.2F, 1.6F);
            if (e != null) {
                Vec3 c = e.getEyePosition();
                DustParticleOptions rojo = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.03F), 1.6F);
                DustParticleOptions oscuro = new DustParticleOptions(new Vector3f(0.32F, 0.0F, 0.02F), 2.2F);
                l.sendParticles(rojo, c.x, c.y - 0.1, c.z, 70, 0.35, 0.35, 0.35, 0.05);
                l.sendParticles(oscuro, c.x, c.y - 0.6, c.z, 40, 0.5, 0.4, 0.5, 0.02);
                l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), c.x, c.y - 0.3, c.z, 50,
                        0.3, 0.3, 0.3, 0.25);
                mancharPiso(s, e.blockPosition());
            }
            for (Pieza v : ev.vivos()) {
                if (Evento.entidad(s, v) instanceof ServerPlayer sp) {
                    Fx.shake(sp, 1.2F, 12);
                }
            }
            for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
                if (e != null && sp.distanceToSqr(e) < 40 * 40) {
                    Fx.flash(sp, 0x30FFE8B0, 3);
                }
            }
            Coleccionista.LOGGER.info("COLECCIONISTA_CAJA bang {} {}{}", p.etiqueta(), p.nombre, traidor ? " (traidor)" : "");
        } else {
            // se trabó: quedan muy pocos para lo que sigue (o ya no estaba)
            Cuerpos.EN_SILLA.remove(id);
            if (arma != null) {
                arma.clic();
            }
            salvados++;
            sonidoArma(s, SoundEvents.DISPENSER_FAIL, 1.6F, 0.7F);
            sonidoArma(s, SoundEvents.CHAIN_BREAK, 1.2F, 0.6F);
            if (e instanceof ServerPlayer sp) {
                Plat.msg(sp, Component.translatable(MOD + ".caja.trabo").withStyle(ChatFormatting.GOLD), false);
            }
            Coleccionista.LOGGER.info("COLECCIONISTA_CAJA trabo {}", p == null ? id : p.nombre);
        }
    }

    /** Unas manchas rojas en el piso detrás de la silla (se quitan al limpiar). */
    private static void mancharPiso(MinecraftServer s, BlockPos b) {
        ServerLevel l = s.overworld();
        BlockState st = Complejo.estado("coleccionista:sangre[facing=" + new String[] {"north", "east", "south", "west"}[R.nextInt(4)] + "]");
        if (st == null) {
            return;
        }
        BlockPos c = Complejo.pos(s, "caja.escenario");
        for (int i = 0; i < 3; i++) {
            BlockPos q = i == 0 ? b : b.offset(R.nextInt(3) - 1, 0, R.nextInt(3) - 1);
            if (c != null && i > 0) {
                // un poco hacia afuera del círculo (detrás de la silla)
                double dx = q.getX() - c.getX(), dz = q.getZ() - c.getZ();
                q = q.offset((int) Math.signum(dx), 0, (int) Math.signum(dz));
            }
            if (l.getBlockState(q).isAir() && l.getBlockState(q.below()).isSolidRender(l, q.below())) {
                l.setBlock(q, st, 2);
                sangre.add(q);
            }
        }
    }

    private static void sonidoArma(MinecraftServer s, net.minecraft.sounds.SoundEvent e, float vol, float tono) {
        BlockPos p = arma != null && arma.arriba() ? BlockPos.containing(arma.canon()) : Complejo.pos(s, "caja.escenario");
        if (p != null) {
            s.overworld().playSound(null, p, e, SoundSource.HOSTILE, vol, tono);
        }
    }

    /** Se revisó a todos: ahora los traidores que no engañaron a nadie. */
    private static void aTraidores(MinecraftServer s) {
        Evento ev = Evento.get(s);
        pendientesTraidores.clear();
        for (UUID tr : traidoresIds) {
            Pieza p = ev.pieza(tr);
            if (p == null || !p.vivo()) {
                continue;
            }
            boolean engano = false;
            for (UUID o : oyentes.getOrDefault(tr, Set.of())) {
                if (fusilados.contains(o)) {
                    engano = true;
                    break;
                }
            }
            if (!engano) {
                pendientesTraidores.add(tr);
            }
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA traidores sin_enganar={}", pendientesTraidores.size());
        if (pendientesTraidores.isEmpty()) {
            terminar(s);
            return;
        }
        fase = Fase.TRAIDORES;
        idx = -1;
        desde = t;
        paso = Paso.GIRAR;
        pasoDesde = t + 100000;
        if (arma != null) {
            // se voltea despacio, apuntando al piso: busca a sus traidores
            arma.apuntar(arma.getYRot() + 180F, 35F);
        }
        foco(s, null);
        enviar(s);
        org(s);
        Pantallas.hablar(s, "caja_traidores", () -> {
            if (fase == Fase.TRAIDORES) {
                siguiente(s);
            }
        });
    }

    /** Se acabó: sube el arma, se dicen los traidores, se sueltan las correas. */
    public static boolean terminar(MinecraftServer s) {
        if (fase == Fase.NADA || fase == Fase.FIN) {
            return false;
        }
        Pantallas.callar(s);
        fase = Fase.FIN;
        desde = t;
        Evento ev = Evento.get(s);
        ev.reloj(s, 0);
        if (arma != null) {
            arma.bajar();
        }
        foco(s, null);
        luces(s, false);
        for (Silla si : circulo.values()) {
            si.cerrar(false);
        }
        Voz.juntar();
        Voz.tonos(Map.of());
        Vestuario.quitarDeTodos(s, MASCARA);
        StringBuilder nombres = new StringBuilder();
        for (UUID id : traidoresIds) {
            Pieza p = ev.pieza(id);
            if (p != null) {
                nombres.append(nombres.length() > 0 ? " · " : "").append(p.nombre);
            }
        }
        // a cada quien, cuál era su símbolo (ya no importa: la música se acabó)
        for (Map.Entry<UUID, Integer> e : simbolo.entrySet()) {
            Pieza p = ev.pieza(e.getKey());
            if (p != null && Evento.entidad(s, p) instanceof ServerPlayer sp) {
                Plat.msg(sp, Component.translatable(MOD + ".caja.era", Component.translatable(MOD + ".caja.simbolo." + SIMBOLOS[e.getValue()]))
                        .withStyle(ChatFormatting.GRAY), false);
            }
        }
        String tr = nombres.toString();
        Pantallas.hablar(s, "caja_fin", () -> {
            Evento.get(s).prueba(s, Component.translatable(MOD + ".caja.fin_capitulo").getString());
            Coleccionista.LOGGER.info("COLECCIONISTA_CAPITULO3 fin vivos={}", Evento.get(s).vivos().size());
            // "los que siguen enteros…": el capítulo 4 (se abre el pasillo hacia el teatrito de Resortes)
            if (Sorpresa.encadenar && Complejo.construido(s, "sorpresa") && fase == Fase.FIN) {
                aSorpresa = t + 60;
            }
        });
        Anuncio.titulo(s, Component.translatable(MOD + ".caja.fin.titulo"), Component.translatable(MOD + ".caja.fin.traidores", tr));
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".caja.fin.traidores", tr)));
        for (Pieza p : ev.piezas()) {
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                mio(s, sp);
            }
        }
        enviar(s);
        org(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA fin balas={} salvados={} vivos={} traidores={}", balas, salvados, ev.vivos().size(), tr);
        return true;
    }

    // ------------------------------------------------------------------ cada tick

    private static void tick(MinecraftServer s) {
        if (fase == Fase.NADA) {
            return;
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        t++;
        cortinasTick(s);
        switch (fase) {
            case PASILLO -> {
                String pas = adelante ? "pas_caj" : "pas_man";
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
                        // los bots (solo en las pruebas) caminan solos
                        if (p.bot && !dentro && t - desde >= 60) {
                            BlockPos c = Complejo.pos(s, pas + ".centro");
                            if (c != null) {
                                ir(s, e, c.getX() + 0.5 + R.nextInt(3) - 1, c.getY(), c.getZ() + 0.5 + R.nextInt(3) - 1, 90F);
                                dentro = true;
                            }
                        }
                        todos &= dentro;
                    }
                    if (n > 0 && todos || t - desde >= esperaPasillo) {
                        // los que se quedaron afuera llegan con los demás; se cierra la puerta y pasan
                        BlockPos c = Complejo.pos(s, pas + ".centro");
                        for (Pieza p : ev.vivos()) {
                            Entity e = Evento.entidad(s, p);
                            if (e != null && c != null && !Cabina.dentro(s, pas, e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                                ir(s, e, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 90F);
                            }
                        }
                        if (adelante) {
                            Adelante.abrir(s, "caja", "pas_caj", false);
                        } else {
                            puertas(s, pas, false);
                        }
                        fase = Fase.LLEGANDO;
                        desde = t + 1000000; // espera a que se cierren las puertas
                        apagonFin = t + (adelante ? 30 : 50);
                    }
                }
                break;
            }
            case LLEGANDO -> {
                if (desde > t && t >= apagonFin) {
                    // se va la luz un momento y, en lo oscuro, pasan al pasillo de la caja
                    desde = t + 2000000;
                    apagonFin = Long.MAX_VALUE;
                    if (adelante) {
                        llegar(s);
                    } else {
                        com.coleccionista.evento.Apagon.hacer(s, s.getPlayerList().getPlayers(), 60, () -> llegar(s));
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
                        if (p.bot && !enSalon(s, e) && t - desde >= 60) {
                            BlockPos c = Complejo.pos(s, "caja.entrada");
                            if (c != null) {
                                ir(s, e, c.getX() + 0.5 + R.nextInt(3), c.getY(), c.getZ() + 0.5 - 2 + R.nextInt(5), 270F);
                            }
                        }
                        todos &= enSalon(s, e);
                    }
                    if (n > 0 && todos || t - desde >= esperaSalon) {
                        hablar(s);
                    }
                }
            }
            case SENTARSE -> {
                if (t % 10 == 0) {
                    sentarseTick(s);
                }
            }
            case ROLES -> {
                if (t % 20 == 0) {
                    vigilarAsientos(s);
                }
                if (t - desde >= 170) {
                    empezarTurno(s);
                }
            }
            case APAGON -> {
                if (t == apagonMedio) {
                    enLaOscuridad(s);
                }
                if (t >= apagonFin) {
                    if (trasApagon == Fase.CIRCULO) {
                        enCirculo(s);
                    } else if (trasApagon == Fase.ROLES) {
                        mostrarRoles(s);
                    } else {
                        empezarTurno(s);
                    }
                }
            }
            case TURNO -> {
                if (t == susurroEn) {
                    susurrar(s);
                }
                if (t % 20 == 0) {
                    vigilarAsientos(s);
                }
                int resta = (int) ((turnoFin - t) / 20);
                if (resta <= 5 && resta >= 0 && (turnoFin - t) % 20 == 0) {
                    for (Pieza p : ev.vivos()) {
                        if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                            sp.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.MASTER, 0.5F, 0.5F + 0.1F * resta);
                        }
                    }
                }
                if (t >= turnoFin) {
                    finTurno(s);
                }
            }
            case CIRCULO -> {
                if (t % 20 == 0) {
                    vigilarCirculo(s);
                }
                // se abre la compuerta y sube la escopeta (girando) mientras habla el Coleccionista
                if (t - desde == 50) {
                    compuerta(s, true);
                }
                if (t - desde == 75 && arma != null) {
                    arma.subir();
                }
            }
            case REVISION, TRAIDORES -> {
                if (t % 20 == 0) {
                    vigilarCirculo(s);
                }
                revisionTick(s);
            }
            case FIN -> {
                if (aSorpresa > 0 && t >= aSorpresa) {
                    aSorpresa = 0;
                    // primero se suelta a todos de sus sillas (el círculo), luego llama la caja sorpresa
                    for (Silla si : circulo.values()) {
                        si.soltar();
                    }
                    Sorpresa.llamar(s);
                    return;
                }
                if (arma != null && arma.guardada() && t - desde > Escopeta.TICKS_BAJAR + 10) {
                    arma.discard();
                    arma = null;
                    compuerta(s, false);
                }
            }
            default -> {
            }
        }
        if (t % 40 == 0) {
            enviar(s);
        }
    }

    private static void revisionTick(MinecraftServer s) {
        UUID id = apuntado();
        if (id == null) {
            return;
        }
        long dt = t - pasoDesde;
        switch (paso) {
            case GIRAR -> {
                boolean listo;
                if (arma == null) {
                    listo = true;
                } else if (!arma.arriba()) {
                    listo = false;
                } else if (amagoEtapa == 0) {
                    empezarApunte(s, id);
                    listo = false;
                } else if (amagoEtapa == 1) {
                    // llegó al falso: se queda un momento (al otro se le para el corazón)
                    if (arma.apuntando()) {
                        amagoEtapa = 2;
                        amagoHasta = t + 25 + R.nextInt(25);
                        Pieza f = amago == null ? null : Evento.get(s).pieza(amago);
                        if (f != null && Evento.entidad(s, f) instanceof ServerPlayer sp) {
                            sp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.MASTER, 1F, 1.3F);
                        }
                    }
                    listo = false;
                } else if (amagoEtapa == 2) {
                    if (t >= amagoHasta) {
                        Entity e = Evento.entidad(s, Evento.get(s).pieza(id));
                        if (e != null) {
                            arma.apuntarA(e.getEyePosition());
                        }
                        amagoEtapa = 3;
                    }
                    listo = false;
                } else {
                    listo = arma.apuntando();
                }
                if (listo && dt >= 20) {
                    foco(s, Evento.entidad(s, Evento.get(s).pieza(id)));
                    paso = Paso.ESPERA;
                    pasoDesde = t;
                    eligio.remove(id);
                    Pieza p = Evento.get(s).pieza(id);
                    if (p != null && Evento.entidad(s, p) instanceof ServerPlayer sp) {
                        mio(s, sp);
                        sp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.MASTER, 1F, 1.2F);
                    }
                    if (p != null && p.bot && fase == Fase.REVISION) {
                        eligio.put(id, respuestaBot(id));
                    }
                    enviar(s);
                    org(s);
                }
            }
            case ESPERA -> {
                // sin juez: decide solo cuando ya marcó (o se le acabó el tiempo: callarse cuenta como error)
                if (!hayJuez(s) && !esperar) {
                    boolean traidor = traidoresIds.contains(id);
                    if (fase == Fase.TRAIDORES && dt >= 60) {
                        decidir(s, true);
                    } else if (fase == Fase.REVISION) {
                        Integer e = eligio.get(id);
                        if (e != null && dt >= 50 || dt >= segundosElegir * 20L) {
                            boolean bien = traidor || e != null && e.equals(simbolo.get(id));
                            decidir(s, !bien);
                        }
                    }
                } else if (t % 20 == 0) {
                    org(s);
                }
            }
            case BIEN, BALA -> {
                // la misma tensión para los dos: el zumbido y el latido, la carga (los cañones se quiebran y se cierran)... y al final
                if (dt == 64 && arma != null) {
                    arma.cargar();
                    sonidoArma(s, propio("caja_cerrojo"), 2F, 1F);
                }
                if (dt == 104) {
                    if (paso == Paso.BALA) {
                        disparar(s, id);
                    } else {
                        // clic: los martillos caen y no sale nada
                        if (arma != null) {
                            arma.clic();
                        }
                        sonidoArma(s, SoundEvents.DISPENSER_FAIL, 2F, 0.8F);
                        sonidoArma(s, SoundEvents.LEVER_CLICK, 1.5F, 1.4F);
                        Pieza p = Evento.get(s).pieza(id);
                        if (p != null && Evento.entidad(s, p) instanceof ServerPlayer sp) {
                            Plat.msg(sp, Component.translatable(MOD + ".caja.bien").withStyle(ChatFormatting.GREEN), true);
                        }
                        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA clic {}", p == null ? id : p.nombre);
                    }
                }
                if (dt >= (paso == Paso.BALA ? 190 : 150)) {
                    siguiente(s);
                }
            }
            default -> {
            }
        }
    }

    /** Un bot "decide" su símbolo: el que más le dijeron (si no le dijeron nada, al azar). */
    private static int respuestaBot(UUID id) {
        List<Integer> l = dichos.getOrDefault(id, List.of());
        int[] c = new int[SIMBOLOS.length];
        for (int k : l) {
            c[k]++;
        }
        int mejor = R.nextInt(SIMBOLOS.length), n = -1;
        for (int k = 0; k < c.length; k++) {
            if (c[k] > n) {
                n = c[k];
                mejor = k;
            }
        }
        return n <= 0 ? R.nextInt(SIMBOLOS.length) : mejor;
    }

    /** Nadie se baja de su góndola (y quien se reconecta vuelve a su asiento). */
    private static void vigilarAsientos(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Map.Entry<UUID, int[]> e : lugar.entrySet()) {
            Pieza p = ev.pieza(e.getKey());
            Entity ent = p == null || !p.vivo() ? null : Evento.entidad(s, p);
            Silla si = asiento(e.getValue());
            if (ent != null && si != null && ent.getVehicle() != si) {
                sentar(s, ent, si);
                si.cerrar(true, false);
            }
        }
    }

    private static void vigilarCirculo(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Map.Entry<UUID, Silla> e : circulo.entrySet()) {
            Pieza p = ev.pieza(e.getKey());
            Entity ent = p == null || !p.vivo() ? null : Evento.entidad(s, p);
            if (ent != null && ent.getVehicle() != e.getValue() && e.getValue().isAlive()) {
                sentar(s, ent, e.getValue());
                e.getValue().cerrar(true, false);
            }
        }
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        if (fase == Fase.SENTARSE) {
            todosSentados(s);
            return true;
        }
        if (fase == Fase.TURNO) {
            finTurno(s);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ lo que mandan los clientes

    private static void accion(ServerPlayer p, CompoundTag d) {
        MinecraftServer s = p.server;
        Evento ev = Evento.get(s);
        String que = d.getString("que");
        if ("simbolo".equals(que)) {
            UUID id = apuntado();
            if (id == null || !id.equals(p.getUUID()) || paso != Paso.ESPERA) {
                return;
            }
            int k = d.getInt("s");
            if (k < 0 || k >= SIMBOLOS.length) {
                return;
            }
            eligio.put(id, k);
            mio(s, p);
            org(s);
            Coleccionista.LOGGER.info("COLECCIONISTA_CAJA marca {} {}", p.getGameProfile().getName(), SIMBOLOS[k]);
            return;
        }
        // lo demás, solo el organizador (que no esté jugando)
        Pieza pz = ev.pieza(p);
        if (pz != null && pz.vivo() || !(ev.esOrganizador(p.getUUID()) || Plat.isOp(p))) {
            return;
        }
        if ("bien".equals(que)) {
            decidir(s, false);
        } else if ("bala".equals(que)) {
            decidir(s, true);
        } else if ("auto".equals(que)) {
            auto = d.getBoolean("si");
            org(s);
        }
    }

    // ------------------------------------------------------------------ el estado para los clientes

    public static CompoundTag estado() {
        CompoundTag t0 = new CompoundTag();
        t0.putString("fase", fase.name());
        t0.putInt("turno", turno + 1);
        t0.putInt("turnos", TURNOS);
        t0.putBoolean("anonimo", anonimo());
        UUID a = apuntado();
        if (a != null) {
            t0.putUUID("apuntado", a);
            t0.putString("paso", paso == Paso.BIEN || paso == Paso.BALA ? "TENSION" : paso.name());
        }
        if (fase == Fase.SENTARSE) {
            t0.putInt("sentados", sentados);
            t0.putInt("total", total);
        }
        return t0;
    }

    private static void enviar(MinecraftServer s) {
        PlayerStore.Sync.toAll(s, KEY, estado());
    }

    /** Lo de cada pieza: su papel, con quién (si es traidor), dónde va sentada y si le toca decir su símbolo. */
    private static void mio(MinecraftServer s, ServerPlayer sp) {
        CompoundTag d = new CompoundTag();
        // (la carta se ve al prenderse la luz, no en el apagón)
        if (fase != Fase.NADA && simbolo.containsKey(sp.getUUID()) && !(fase == Fase.APAGON && trasApagon == Fase.ROLES)) {
            boolean tr = traidoresIds.contains(sp.getUUID());
            d.putString("rol", tr ? "traidor" : "inocente");
            if (tr) {
                StringBuilder otros = new StringBuilder();
                for (UUID o : traidoresIds) {
                    if (!o.equals(sp.getUUID())) {
                        Pieza p = Evento.get(s).pieza(o);
                        otros.append(otros.length() > 0 ? ", " : "").append(p == null ? "?" : p.nombre);
                    }
                }
                d.putString("otro", otros.toString());
            }
            int[] lu = lugar.get(sp.getUUID());
            if (lu != null) {
                d.putInt("gondola", lu[0] + 1);
                d.putString("lado", lu[1] == 0 ? "atras" : lu[1] == 1 ? "frente" : "solo");
            }
            UUID a = apuntado();
            if (a != null && a.equals(sp.getUUID()) && paso == Paso.ESPERA && fase == Fase.REVISION) {
                d.putBoolean("elige", true);
                d.putInt("segundos", segundosElegir);
            }
            Integer e = eligio.get(sp.getUUID());
            if (e != null) {
                d.putInt("eligio", e);
            }
        }
        PlayerStore.Sync.toPlayer(sp, KEY_MIO, d);
    }

    /** Lo que ve el organizador (que no juega): a quién apunta el arma, su símbolo real y lo que marcó. */
    private static void org(MinecraftServer s) {
        Evento ev = Evento.get(s);
        CompoundTag d = new CompoundTag();
        UUID a = apuntado();
        if (a != null) {
            Pieza p = ev.pieza(a);
            d.putString("nombre", p == null ? "?" : p.nombre);
            d.putString("etiqueta", p == null ? "" : p.etiqueta());
            Integer k = simbolo.get(a);
            d.putString("real", k == null ? "" : SIMBOLOS[k]);
            Integer e = eligio.get(a);
            d.putString("marco", e == null ? "" : SIMBOLOS[e]);
            d.putBoolean("traidor", traidoresIds.contains(a));
            d.putBoolean("fase_traidores", fase == Fase.TRAIDORES);
            d.putString("paso", paso.name());
            d.putInt("i", idx + 1);
            d.putInt("de", fase == Fase.TRAIDORES ? pendientesTraidores.size() : orden.size());
        }
        d.putString("fase", fase.name());
        d.putBoolean("auto", auto);
        d.putBoolean("juez", hayJuez(s));
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            Pieza pz = ev.pieza(p);
            if ((pz == null || !pz.vivo()) && (ev.esOrganizador(p.getUUID()) || Plat.isOp(p))) {
                PlayerStore.Sync.toPlayer(p, KEY_ORG, d);
            }
        }
    }

    public static String resumen() {
        return fase.name() + " turno=" + (turno + 1) + " traidores=" + traidoresIds.size() + " balas=" + balas;
    }

    /** A dónde ir: al pasillo (desde la casa) y luego al salón. */
    public static Objetivo.Meta objetivo(MinecraftServer s, ServerPlayer sp) {
        if (fase == Fase.PASILLO && adelante) {
            Objetivo.Meta m = Adelante.meta(s, sp, "caja", "pas_caj");
            if (m != null) {
                return m;
            }
        } else if (fase == Fase.PASILLO && !Cabina.dentro(s, "pas_man", sp, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
            return Objetivo.Meta.en(Complejo.pos(s, "pas_man.centro"), 1.5, MOD + ".obj.pasillo");
        }
        if (fase == Fase.LLEGANDO && desde <= t && !enSalon(s, sp)) {
            return Objetivo.Meta.en(Complejo.pos(s, "caja.centro"), 1.5, MOD + ".obj.carrusel");
        }
        if (fase == Fase.SENTARSE && sentadoEn(sp) == null) {
            // la góndola con asiento libre más cercana (de preferencia donde ya hay alguien solo)
            BlockPos mejor = null;
            double dm = Double.MAX_VALUE;
            for (int g = 0; g < asientos.length; g++) {
                boolean libre = false, uno = false;
                for (Silla si : asientos[g]) {
                    if (si != null && si.isAlive()) {
                        libre |= !si.isVehicle();
                        uno |= si.isVehicle();
                    }
                }
                BlockPos b = Complejo.pos(s, "caja.gon." + g + ".entrada");
                if (!libre || b == null) {
                    continue;
                }
                double d = sp.distanceToSqr(b.getX() + 0.5, b.getY(), b.getZ() + 0.5) * (uno ? 0.4 : 1.0);
                if (d < dm) {
                    dm = d;
                    mejor = b;
                }
            }
            return mejor == null ? null : Objetivo.Meta.en(mejor, 1.2, MOD + ".obj.gondola");
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

    /** La musiquita de la caja (unas notas de campanita, despacio). */
    private static void musica(MinecraftServer s, float vol) {
        BlockPos c = Complejo.pos(s, "caja.centro");
        if (c == null) {
            return;
        }
        float[] notas = {1.0F, 1.19F, 1.5F, 1.33F, 1.19F, 1.0F};
        for (int i = 0; i < notas.length; i++) {
            s.overworld().playSound(null, c, SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.RECORDS, vol, notas[i] * 0.5F);
        }
    }

    /** Quita todo lo de la caja: asientos, sillas, arma, máscaras, etiquetas, sangre, voces. */
    private static void limpiarTodo(MinecraftServer s) {
        cortinas(s, false, -1);
        cortinasEn = -1;
        for (Silla[] gg : asientos) {
            for (int i = 0; i < gg.length; i++) {
                if (gg[i] != null) {
                    gg[i].soltar();
                    gg[i].discard();
                    gg[i] = null;
                }
            }
        }
        for (Silla si : circulo.values()) {
            si.soltar();
            si.discard();
        }
        circulo.clear();
        // también las que hayan quedado de antes (si el servidor se reinició a la mitad)
        ServerLevel l = s.overworld();
        BlockPos o = Complejo.origen(s, "caja");
        if (o != null) {
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(o.getX(), o.getY() - 2, o.getZ() - 2, o.getX() + 66, o.getY() + 16,
                    o.getZ() + 46);
            for (Silla si : l.getEntitiesOfClass(Silla.class, box, x -> "caja".equals(x.grupo) || x.oculta())) {
                si.soltar();
                si.discard();
            }
            for (Escopeta a : l.getEntitiesOfClass(Escopeta.class, box)) {
                a.discard();
            }
        }
        if (arma != null) {
            arma.discard();
            arma = null;
        }
        if (o != null) {
            compuerta(s, false);
            lucesBajas = true;
            luces(s, false);
            foco(s, null);
        }
        for (BlockPos q : sangre) {
            if (l.getBlockState(q).getBlock() != Blocks.AIR) {
                l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        sangre.clear();
        Vestuario.quitarDeTodos(s, ETIQUETA);
        Vestuario.quitarDeTodos(s, MASCARA);
        Voz.juntar();
        Voz.tonos(Map.of());
        simbolo.clear();
        traidoresIds.clear();
        rondas.clear();
        gondolasRonda.clear();
        lugar.clear();
        dichos.clear();
        oyentes.clear();
        orden.clear();
        pendientesTraidores.clear();
        eligio.clear();
        fusilados.clear();
        botDesde.clear();
        botVa.clear();
        Cuerpos.EN_SILLA.clear();
        aSorpresa = 0;
        turno = -1;
        idx = -1;
        balas = 0;
        salvados = 0;
        sentados = 0;
        total = 0;
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase == Fase.HABLANDO || fase == Fase.CIRCULO || fase == Fase.TRAIDORES || fase == Fase.FIN) {
            Pantallas.callar(s);
        }
        if (fase != Fase.NADA) {
            Pantallas.modo(s, "apagada", "");
        }
        limpiarTodo(s);
        fase = Fase.NADA;
        enviar(s);
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            PlayerStore.Sync.toPlayer(sp, KEY_MIO, new CompoundTag());
            PlayerStore.Sync.toPlayer(sp, KEY_ORG, new CompoundTag());
        }
    }

    /** Para el organizador: salta al siguiente paso. */
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
                sentarse(s);
                return true;
            }
            case SENTARSE -> {
                todosSentados(s);
                return true;
            }
            case ROLES -> {
                empezarTurno(s);
                return true;
            }
            case TURNO -> {
                finTurno(s);
                return true;
            }
            case CIRCULO -> {
                Pantallas.callar(s);
                revisar(s);
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
