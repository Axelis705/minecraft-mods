package com.coleccionista.hotel;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import com.coleccionista.bloque.deco.DecoBlock;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Cabina;
import com.coleccionista.evento.Capitulo1;
import com.coleccionista.evento.Celdas;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Cuerpos;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.evento.Pistas;
import com.coleccionista.mapa.Hotel;
import com.coleccionista.mapa.SalaGrande;
import com.coleccionista.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
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
 * El hotel: el primer juego del capítulo 1, cada quien solo, antes de formar equipos.
 * <ol>
 *   <li>En la sala grande el Coleccionista presenta la torre ("uno por persona") y se abren sus 32 ascensores angostos. Cada quien se
 *   mete a uno (si se meten dos, no cierra); al estar todos (o al acabarse el tiempo: a los que se quedaron afuera los mete él), bajan.</li>
 *   <li>Mientras el ascensor baja, explica las reglas. Cada ascensor llega a un lugar distinto del hotel abandonado (diez pisos).</li>
 *   <li>Diez minutos para encontrar la única salida: una de las 20 puertas de servicio (al azar en cada partida). Tres puertas tienen
 *   la trampa de la escopeta (quien abre pierde la cabeza); las demás son clósets. Pistas en las paredes ("más arriba", "en este piso"),
 *   el hilo de las puertas con trampa y la luz de colores de la salida. No hay regeneración: solo los botiquines curan.</li>
 *   <li>Peligros: los dos Botones (no se les puede hacer daño), las ratas de cuerda (con un tubo se acaban de un golpe), las tablas con
 *   clavos, los charcos con electricidad y los candelabros que se caen.</li>
 *   <li>La salida lleva a un tubo de colores; se cae a la sala de espera, donde los monitores muestran a los que siguen adentro (clic en
 *   uno: se ve desde sus ojos).</li>
 *   <li>Al acabarse el tiempo, los que siguen adentro se quedan en el hotel (eliminados); si el organizador lo pidió, el último en salir
 *   también. Los demás se duermen con el gas y despiertan en la sala grande: ahí se forman los equipos.</li>
 * </ol>
 * El organizador lo controla desde el panel (línea de tiempo) o con {@code /evento hotel ...}.
 */
public final class JuegoHotel {
    private static final String MOD = Coleccionista.MODID;
    public static final String PRUEBA = "El hotel";
    public static final String KEY_PISTAS = MOD + ":hotel_pistas";

    /** ¿La sala grande pasa primero por el hotel? (el organizador lo puede apagar). */
    public static boolean usar = true;
    /** Duración del juego, tiempo para meterse a los ascensores (s) y lo que tarda en bajar el ascensor (ticks). */
    public static int segundos = 600, entrarSeg = 60, viajeTicks = 900;
    /** ¿El último en salir también se queda? (lo decide el organizador en el momento). */
    public static boolean ultimoCae = false;
    public static int trampas = 3, cuantosBotones = 2;

    public enum Fase {
        NADA, HABLANDO, ASCENSORES, BAJANDO, JUEGO, FIN
    }

    private static Fase fase = Fase.NADA;
    private static boolean hecho;
    private static long faseDesde, cierraEn = -1;
    private static int salida = -1, viajes, llegados;
    private static final Set<Integer> muertes = new HashSet<>();
    private static final Set<Integer> disparadas = new HashSet<>();
    private static final Map<Integer, Integer> puertas = new HashMap<>(), trampasEnt = new HashMap<>(), ratas = new HashMap<>();
    private static final Map<Integer, Long> ratasMuertas = new HashMap<>(), cierraPuerta = new HashMap<>();
    private static final Map<Integer, Object[]> disparo = new HashMap<>();
    private static final List<Integer> botones = new ArrayList<>(), monitores = new ArrayList<>();
    private static final Map<UUID, Integer> cabina = new HashMap<>();
    private static final List<UUID> salieron = new ArrayList<>();
    private static final Map<UUID, UUID> mirando = new HashMap<>();
    private static final Map<UUID, Vec3> antesDeMirar = new HashMap<>();
    private static final Map<UUID, Long> pinchado = new HashMap<>();
    private static final Map<UUID, Integer> pisoDe = new HashMap<>();
    private static final Set<Integer> caidos = new HashSet<>();
    private static final Map<Integer, Long> caeEn = new HashMap<>();
    private static final Map<Integer, long[]> charcos = new HashMap<>();
    private static Boolean regenAntes;
    private static ListTag pistas = new ListTag();
    /** El pizarrón del orden de salida (sala de espera): lo que se escribe y cada renglón {nombre, segundos (-1: cayó en una trampa), ¿cae?}. */
    private static ListTag tablero = new ListTag();
    private static final List<Object[]> orden = new ArrayList<>();
    /** Al bajar: a qué piso llega cada quien (para el letrero del elevador). */
    private static final Map<UUID, Integer> destino = new HashMap<>();
    /** Los monitores de quien acaba de caer en una trampa: "señal perdida" unos segundos ({cuándo, nombre}). */
    private static final Map<UUID, Object[]> perdida = new HashMap<>();
    /** Las lámparas que apagó el apagón del último minuto (para volverlas a prender). */
    private static final Set<BlockPos> apagadas = new HashSet<>();
    private static int ultimoTic = -1;
    private static final Random R = new Random();

    private JuegoHotel() {
    }

    public static void init() {
        AxelisCore.onServerTick(JuegoHotel::tick);
        AxelisCore.onLogin(p -> PlayerStore.Sync.toPlayer(p, KEY_PISTAS, estadoPistas()));
        MinecraftForge.EVENT_BUS.addListener(JuegoHotel::clic);
        MinecraftForge.EVENT_BUS.addListener(JuegoHotel::golpe);
    }

    public static Fase fase() {
        return fase;
    }

    /** ¿Toca el hotel ahora (al juntarse todos en la sala)? */
    public static boolean toca(MinecraftServer s) {
        return usar && !hecho && fase == Fase.NADA && listo(s);
    }

    /** ¿Está construido todo lo que necesita? */
    public static boolean listo(MinecraftServer s) {
        return Complejo.construido(s, "hotel") && Complejo.construido(s, "hotel_espera") && Complejo.construido(s, "sala");
    }

    private static ServerLevel nivel(MinecraftServer s) {
        return s.overworld();
    }

    private static long ahora(MinecraftServer s) {
        return s.overworld().getGameTime();
    }

    private static void fase(MinecraftServer s, Fase f) {
        fase = f;
        faseDesde = ahora(s);
    }

    // ------------------------------------------------------------------ 1. la torre de la sala grande

    /** El Coleccionista presenta el hotel y se abren los ascensores de una persona. */
    public static Component empezar(MinecraftServer s) {
        if (!listo(s)) {
            return Component.translatable(MOD + ".hotel.sin_mapa");
        }
        if (fase != Fase.NADA && fase != Fase.FIN) {
            return Component.translatable(MOD + ".hotel.ya");
        }
        limpiarTodo(s);
        fase(s, Fase.HABLANDO);
        Capitulo1.alEmpezarHotel();
        Evento.get(s).prueba(s, PRUEBA);
        Capitulo1.botsALaSala(s);
        if (Celdas.portonAbierto(s)) {
            Celdas.porton(s, false);
        }
        preparar(s);
        Pantallas.soloGrande(s, true);
        Pantallas.hablar(s, "hotel", () -> abrir(s));
        // solo los organizadores saben cuál es la salida (para narrar)
        Anuncio.organizadores(s, Component.translatable(MOD + ".hotel.org.salida", salida(), trampas()));
        Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL empieza salida={} muertes={}", salida, muertes);
        return null;
    }

    /** Se abren los 32 ascensores: cada quien a uno. */
    public static void abrir(MinecraftServer s) {
        if (fase != Fase.HABLANDO && fase != Fase.NADA) {
            return;
        }
        Pantallas.callar(s);
        fase(s, Fase.ASCENSORES);
        botsDentro = false;
        cabina.clear();
        for (int n = 1; n <= SalaGrande.INDIVIDUALES; n++) {
            Cabina.puerta(s, "hotel_s" + n, true);
        }
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.8F);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".hotel.pantalla").getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".hotel.chat.entren", entrarSeg)));
        Evento.get(s).reloj(s, entrarSeg);
        Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL ascensores");
    }

    private static List<Entity> vivos(MinecraftServer s) {
        List<Entity> l = new ArrayList<>();
        for (Pieza p : Evento.get(s).vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null) {
                l.add(e);
            }
        }
        return l;
    }

    /** Quién está en qué ascensor de la torre (los de afuera no salen). */
    private static Map<Integer, List<Entity>> ocupacion(MinecraftServer s) {
        Map<Integer, List<Entity>> m = new LinkedHashMap<>();
        for (Entity e : vivos(s)) {
            for (int n = 1; n <= SalaGrande.INDIVIDUALES; n++) {
                if (Cabina.dentro(s, "hotel_s" + n, e, 1, Cabinas3.ALTO, 1)) {
                    m.computeIfAbsent(n, k -> new ArrayList<>()).add(e);
                    break;
                }
            }
        }
        return m;
    }

    /** (Para no confundir con las de equipo.) */
    private static final class Cabinas3 {
        static final int ALTO = com.coleccionista.mapa.Cabinas.IND_ALTO;
    }

    private static void tickAscensores(MinecraftServer s, long now) {
        if (now % 10 != 0) {
            return;
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        // los bots (solo en las pruebas) se meten solos a los ascensores libres
        if (!botsDentro && now - faseDesde >= 40) {
            botsDentro = true;
            botsSuben(s);
        }
        Map<Integer, List<Entity>> m = ocupacion(s);
        ultimaOcupacion = m;
        int solos = 0;
        for (int n = 1; n <= SalaGrande.INDIVIDUALES; n++) {
            List<Entity> d = m.get(n);
            boolean uno = d != null && d.size() == 1;
            Cabina.puerta(s, "hotel_s" + n, !uno);
            if (uno) {
                solos++;
            } else if (d != null) {
                for (Entity e : d) {
                    if (e instanceof ServerPlayer sp) {
                        Plat.msg(sp, Component.translatable(MOD + ".hotel.uno").withStyle(ChatFormatting.RED), true);
                    }
                }
            }
        }
        int vivos = vivos(s).size();
        for (Entity e : vivos(s)) {
            boolean dentro = false;
            for (List<Entity> l : m.values()) {
                dentro |= l.contains(e);
            }
            if (!dentro && e instanceof ServerPlayer sp) {
                Plat.msg(sp, Component.translatable(MOD + ".hotel.entra").withStyle(ChatFormatting.GOLD), true);
            }
        }
        boolean todos = vivos > 0 && solos >= vivos;
        if (todos && cierraEn < 0) {
            cierraEn = now + 60;
        }
        if (!todos) {
            cierraEn = -1;
        }
        if (cierraEn > 0 && now >= cierraEn || ev.resta(s) == 0) {
            bajar(s);
        }
    }

    private static Map<Integer, List<Entity>> ultimaOcupacion = new HashMap<>();
    private static boolean botsDentro;

    /** El indicador de "ir acá": mientras se abren los ascensores, a quien no está en uno, el libre más cercano. */
    public static com.coleccionista.evento.Objetivo.Meta objetivo(MinecraftServer s, ServerPlayer sp) {
        if (fase != Fase.ASCENSORES) {
            return null;
        }
        for (List<Entity> l : ultimaOcupacion.values()) {
            if (l.size() == 1 && l.contains(sp)) {
                return null;
            }
        }
        BlockPos mejor = null;
        double md = Double.MAX_VALUE;
        for (int n = 1; n <= SalaGrande.INDIVIDUALES; n++) {
            if (ultimaOcupacion.containsKey(n)) {
                continue;
            }
            AABB c = Cabina.caja(s, "hotel_s" + n, 1, Cabinas3.ALTO, 1);
            if (c == null) {
                continue;
            }
            double d = c.getCenter().distanceToSqr(sp.position());
            if (d < md) {
                md = d;
                mejor = BlockPos.containing(c.getCenter().x, c.minY, c.getCenter().z);
            }
        }
        return mejor == null ? null : com.coleccionista.evento.Objetivo.Meta.en(mejor, 1.2, MOD + ".obj.hotel");
    }

    private static void botsSuben(MinecraftServer s) {
        Map<Integer, List<Entity>> m = ocupacion(s);
        int n = 1;
        for (Pieza p : Evento.get(s).vivos()) {
            if (!p.bot || !(Evento.entidad(s, p) instanceof com.coleccionista.bot.PiezaBot b)) {
                continue;
            }
            boolean ya = false;
            for (List<Entity> l : m.values()) {
                ya |= l.contains(b);
            }
            if (ya) {
                continue;
            }
            while (n <= SalaGrande.INDIVIDUALES && m.containsKey(n)) {
                n++;
            }
            if (n > SalaGrande.INDIVIDUALES) {
                return;
            }
            meter(s, b, n);
            m.put(n, new ArrayList<>(List.of(b)));
        }
    }

    /** Pone a alguien dentro del ascensor N de la torre. */
    private static void meter(MinecraftServer s, Entity e, int n) {
        net.minecraft.world.phys.AABB c = Cabina.caja(s, "hotel_s" + n, 1, Cabinas3.ALTO, 1);
        if (c == null) {
            return;
        }
        Vec3 p = c.getCenter();
        float yaw = Complejo.yaw("cab.hotel_s" + n + ".origen") + 180F;
        if (e instanceof ServerPlayer sp) {
            sp.teleportTo(s.overworld(), p.x, c.minY + 0.05, p.z, yaw, 0F);
        } else {
            if (e instanceof com.coleccionista.bot.PiezaBot b) {
                b.getNavigation().stop();
            }
            e.moveTo(p.x, c.minY + 0.05, p.z, yaw, 0F);
        }
    }

    /** Se cierran las puertas y bajan (a los que se quedaron afuera los mete el Coleccionista en los ascensores vacíos). */
    public static void bajar(MinecraftServer s) {
        if (fase != Fase.ASCENSORES && fase != Fase.HABLANDO) {
            return;
        }
        Pantallas.callar(s);
        Map<Integer, List<Entity>> m = ocupacion(s);
        List<Entity> fuera = new ArrayList<>();
        for (Entity e : vivos(s)) {
            boolean ya = false;
            for (List<Entity> l : m.values()) {
                ya |= l.size() == 1 && l.contains(e);
            }
            if (!ya) {
                fuera.add(e);
            }
        }
        // los que estaban de dos en dos o afuera: cada uno a un ascensor vacío
        for (Map.Entry<Integer, List<Entity>> en : new ArrayList<>(m.entrySet())) {
            if (en.getValue().size() > 1) {
                m.remove(en.getKey());
            }
        }
        int n = 1;
        for (Entity e : fuera) {
            while (n <= SalaGrande.INDIVIDUALES && m.containsKey(n)) {
                n++;
            }
            if (n > SalaGrande.INDIVIDUALES) {
                break;
            }
            meter(s, e, n);
            m.put(n, new ArrayList<>(List.of(e)));
            if (e instanceof ServerPlayer sp) {
                Plat.msg(sp, Component.translatable(MOD + ".hotel.te_meti").withStyle(ChatFormatting.GOLD), true);
            }
        }
        // a dónde llega cada quien: repartidos por los pisos (uno de cada piso por turnos)
        List<Integer> destinos = repartir(m.size());
        fase(s, Fase.BAJANDO);
        Evento.get(s).reloj(s, 0);
        viajes = 0;
        llegados = 0;
        int i = 0;
        for (Map.Entry<Integer, List<Entity>> en : m.entrySet()) {
            Entity e = en.getValue().get(0);
            cabina.put(e.getUUID(), en.getKey());
            int d = destinos.get(i++ % destinos.size());
            Plano3 mk = marca("hotel.cab." + d);
            if (mk != null) {
                destino.put(e.getUUID(), Hotel.piso(mk.y() - 1));
            }
            if (Cabina.viajar(s, "hotel_s" + en.getKey(), "hotel_h" + d, Cabina.Tipo.ELEVADOR, viajeTicks, 1, Cabinas3.ALTO, 1,
                    List.of(e), () -> llego(s))) {
                viajes++;
            }
        }
        Pantallas.hablar(s, "hotel_ascensor", () -> {
        });
        Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL bajan viajes={} afuera={}", viajes, fuera.size());
        if (viajes == 0) {
            empezarJuego(s);
        }
    }

    /** Los ascensores de llegada, repartidos: uno de cada piso por turnos (al azar), así cada quien empieza en un lugar distinto. */
    private static List<Integer> repartir(int cuantos) {
        Map<Integer, List<Integer>> porPiso = new HashMap<>();
        for (int k = 0; k < 64; k++) {
            Plano3 m = marca("hotel.cab." + k);
            if (m == null) {
                break;
            }
            porPiso.computeIfAbsent(Hotel.piso(m.y - 1), x -> new ArrayList<>()).add(k);
        }
        List<Integer> pisos = new ArrayList<>(porPiso.keySet());
        Collections.shuffle(pisos, R);
        for (List<Integer> l : porPiso.values()) {
            Collections.shuffle(l, R);
        }
        List<Integer> out = new ArrayList<>();
        boolean quedan = true;
        while (out.size() < Math.max(1, cuantos) && quedan) {
            quedan = false;
            for (int p : pisos) {
                List<Integer> l = porPiso.get(p);
                if (!l.isEmpty()) {
                    out.add(l.remove(0));
                    quedan = true;
                }
            }
        }
        return out;
    }

    private record Plano3(int x, int y, int z, float yaw) {
    }

    /** Una marca del plano del hotel (en coordenadas del plano), o null. */
    private static Plano3 marca(String n) {
        Complejo.Zona z = Complejo.zona(n.startsWith("hotel.monitor") || n.startsWith("hotel.espera") || n.startsWith("hotel.tobogan")
                ? "hotel_espera" : "hotel");
        com.coleccionista.mapa.Plano.Marca m = z == null ? null : z.plano().marcas.get(n);
        return m == null ? null : new Plano3(m.x(), m.y(), m.z(), m.yaw());
    }

    private static void llego(MinecraftServer s) {
        llegados++;
        if (fase == Fase.BAJANDO) {
            empezarJuego(s);
        }
    }

    // ------------------------------------------------------------------ 2. preparar el hotel (antes de que bajen)

    /** Deja el hotel listo para una partida: elige la salida y las trampas, pone las puertas, los clósets, los tubos y las pistas. */
    private static void preparar(MinecraftServer s) {
        ServerLevel l = nivel(s);
        quitarEntidades(s);
        int n = Hotel.puertas > 0 ? Hotel.puertas : 20;
        salida = R.nextInt(n);
        muertes.clear();
        disparadas.clear();
        // una trampa casi siempre en el mismo piso que la salida (la otra puerta de ese piso): hay que fijarse en el detalle
        int hermana = salida % 2 == 0 ? salida + 1 : salida - 1;
        if (hermana < n && R.nextInt(10) < 6) {
            muertes.add(hermana);
        }
        while (muertes.size() < Math.min(trampas, n - 1)) {
            int k = R.nextInt(n);
            if (k != salida) {
                muertes.add(k);
            }
        }
        for (int k = 0; k < n; k++) {
            puerta(s, l, k);
        }
        // los tubos, los botiquines, los candelabros y los charcos, como nuevos
        for (int k = 0; k < 64; k++) {
            BlockPos t = Complejo.pos(s, "hotel.tubo." + k);
            if (t == null) {
                break;
            }
            l.setBlock(t, Complejo.estado(MOD + ":tubo_suelo[facing=" + new String[] {"north", "east", "south", "west"}[R.nextInt(4)] + "]"), 2);
        }
        for (int k = 0; k < 64; k++) {
            BlockPos b = Complejo.pos(s, "hotel.bot." + k);
            if (b == null) {
                break;
            }
            BlockState st = l.getBlockState(b);
            if (id(st).equals("botiquin_vacio")) {
                l.setBlock(b, Complejo.estado(MOD + ":botiquin[facing=" + st.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)
                        .getName() + "]"), 2);
            }
        }
        caidos.clear();
        caeEn.clear();
        for (int k = 0; k < 64; k++) {
            BlockPos c = Complejo.pos(s, "hotel.cand." + k);
            if (c == null) {
                break;
            }
            l.setBlock(c.above(3), Complejo.estado(MOD + ":arana"), 2);
            l.setBlock(c, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        charcos.clear();
        // lo que haya quedado tirado (tubos soltados)
        BlockPos o = Complejo.origen(s, "hotel");
        if (o != null) {
            for (ItemEntity it : l.getEntitiesOfClass(ItemEntity.class, new AABB(o).inflate(0).expandTowards(51, 52, 51))) {
                it.discard();
            }
        }
        escribirTablero(s);
        escribirPistas(s);
        ponerMonitores(s);
        encenderTodo(s);
    }

    /** Por si un apagón se quedó a medias (se cerró el servidor justo entonces): todas las lámparas del hotel, prendidas. */
    private static void encenderTodo(MinecraftServer s) {
        BlockPos o = Complejo.origen(s, "hotel");
        if (o == null) {
            return;
        }
        ServerLevel l = nivel(s);
        int n = 0;
        for (BlockPos q : BlockPos.betweenClosed(o, o.offset(50, Hotel.PISOS * Hotel.ALTO_PISO, 50))) {
            BlockState st = l.getBlockState(q);
            if (st.hasProperty(DecoBlock.ENCENDIDA) && st.hasProperty(DecoBlock.FALLA) && !st.getValue(DecoBlock.ENCENDIDA)) {
                l.setBlock(q, st.setValue(DecoBlock.ENCENDIDA, true), net.minecraft.world.level.block.Block.UPDATE_ALL);
                n++;
            }
        }
        if (n > 0) {
            Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL lamparas prendidas otra vez={}", n);
        }
    }

    /** Una puerta de servicio: su hoja (con el detalle que le toca) y lo que hay detrás. */
    private static void puerta(MinecraftServer s, ServerLevel l, int k) {
        BlockPos p = Complejo.pos(s, "hotel.serv." + k);
        if (p == null) {
            return;
        }
        float yaw = Complejo.yaw("hotel.serv." + k);
        l.setBlock(p, net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState(), 2);
        l.setBlock(p.above(), net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState(), 2);
        int tipo = k == salida ? PuertaHotel.SALIDA : muertes.contains(k) ? PuertaHotel.TRAMPA : PuertaHotel.NORMAL;
        // el clóset: piso de concreto (o la boca del tobogán) y vacío hasta el techo
        for (int i = 0; i < 4; i++) {
            BlockPos c = Complejo.pos(s, "hotel.serv." + k + ".k" + i);
            if (c == null) {
                continue;
            }
            String piso = tipo == PuertaHotel.SALIDA ? MOD + ":boca_tobogan[facing=" + hacia(Complejo.yaw("hotel.serv." + k + ".k" + i)) + "]"
                    : MOD + ":concreto_piso";
            l.setBlock(c, Complejo.estado(piso), 2);
            for (int y = 1; y <= 4; y++) {
                l.setBlock(c.above(y), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }
        if (tipo == PuertaHotel.SALIDA) {
            BlockPos c = Complejo.pos(s, "hotel.serv." + k + ".k0");
            if (c != null) {
                l.setBlock(c.above(3), Complejo.estado("minecraft:light[level=10]"), 2);
            }
        } else if (tipo == PuertaHotel.NORMAL) {
            // trastes: una maleta vieja, papeles, una telaraña
            BlockPos a = Complejo.pos(s, "hotel.serv." + k + ".k2"), b = Complejo.pos(s, "hotel.serv." + k + ".k3");
            String mira = hacia(yaw + 180F);
            if (a != null && R.nextInt(3) > 0) {
                l.setBlock(a.above(), Complejo.estado(MOD + ":maleta[facing=" + mira + "]"), 2);
            }
            if (b != null) {
                l.setBlock(b.above(), Complejo.estado(MOD + ":papeles[facing=" + mira + "]"), 2);
                l.setBlock(b.above(3), Complejo.estado(MOD + ":telarana"), 2);
            }
        } else {
            BlockPos f = Complejo.pos(s, "hotel.serv." + k + ".fondo");
            TrampaEscopeta t = ModRegistry.TRAMPA_ESCOPETA.get().create(l);
            if (f != null && t != null) {
                float fy = Complejo.yaw("hotel.serv." + k + ".fondo");
                t.moveTo(f.getX() + 0.5, f.getY(), f.getZ() + 0.5, fy, 0F);
                t.setYRot(fy);
                t.setYHeadRot(fy);
                l.addFreshEntity(t);
                trampasEnt.put(k, t.getId());
            }
        }
        PuertaHotel h = ModRegistry.PUERTA_HOTEL.get().create(l);
        if (h != null) {
            h.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw + 180F, 0F);
            h.setYRot(yaw + 180F);
            h.numero = k;
            h.reiniciar(tipo);
            l.addFreshEntity(h);
            puertas.put(k, h.getId());
        }
    }

    private static String hacia(float yaw) {
        int y = Math.floorMod(Math.round(yaw), 360);
        return y == 0 ? "south" : y == 90 ? "west" : y == 180 ? "north" : "east";
    }

    private static String id(BlockState st) {
        return BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath();
    }

    // ------------------------------------------------------------------ 3. el juego

    private static void empezarJuego(MinecraftServer s) {
        if (fase == Fase.JUEGO) {
            return;
        }
        fase(s, Fase.JUEGO);
        Evento ev = Evento.get(s);
        ev.reloj(s, segundos);
        GameRules.BooleanValue regen = s.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION);
        if (regenAntes == null) {
            regenAntes = regen.get();
        }
        regen.set(false, s);
        ServerLevel l = nivel(s);
        // los Botones, lejos uno del otro, y una rata en cada nido
        List<Integer> pisos = new ArrayList<>(List.of(3, 4, 5, 7, 8, 9));
        Collections.shuffle(pisos, R);
        int puestos = 0;
        for (int pi : pisos) {
            if (puestos >= cuantosBotones) {
                break;
            }
            boolean cerca = false;
            for (int id : botones) {
                if (l.getEntity(id) instanceof Botones b && Math.abs(b.piso - pi) < 3) {
                    cerca = true;
                }
            }
            if (cerca) {
                continue;
            }
            Botones b = ModRegistry.BOTONES.get().create(l);
            if (b != null) {
                b.base = Complejo.origen(s, "hotel");
                b.piso = pi;
                l.addFreshEntity(b);
                b.irAPiso(pi);
                botones.add(b.getId());
                puestos++;
            }
        }
        for (int k = 0; k < 64; k++) {
            if (Complejo.pos(s, "hotel.rata." + k) == null) {
                break;
            }
            rata(s, k);
        }
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.5F);
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".hotel.chat.empieza", segundos / 60)));
        Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL juego salida={} ({}) botones={} llegaron={}", salida, Hotel.codigo(salida), botones.size(),
                llegados);
    }

    private static void rata(MinecraftServer s, int k) {
        BlockPos p = Complejo.pos(s, "hotel.rata." + k);
        ServerLevel l = nivel(s);
        Rata r = p == null ? null : ModRegistry.RATA.get().create(l);
        if (r == null) {
            return;
        }
        r.nido = k;
        r.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, R.nextFloat() * 360F, 0F);
        l.addFreshEntity(r);
        ratas.put(k, r.getId());
    }

    /** ¿Esta persona sigue jugando en el hotel (para el Botones y las ratas)? */
    public static boolean presaValida(ServerPlayer p) {
        if (fase != Fase.JUEGO || p.isSpectator() || p.isCreative() || salieron.contains(p.getUUID()) || !p.isAlive()) {
            return false;
        }
        MinecraftServer s = p.getServer();
        Pieza x = s == null ? null : Evento.get(s).pieza(p);
        return x != null && x.vivo() && enHotel(s, p);
    }

    /** ¿Está dentro del hotel? */
    public static boolean enHotel(MinecraftServer s, Entity e) {
        BlockPos o = Complejo.origen(s, "hotel");
        if (o == null) {
            return false;
        }
        double x = e.getX() - o.getX(), y = e.getY() - o.getY(), z = e.getZ() - o.getZ();
        return x >= 0 && x <= 51 && z >= 0 && z <= 51 && y >= -1 && y <= 51;
    }

    private record Despues(long cuando, Runnable que) {
    }

    private static final List<Despues> DESPUES = new ArrayList<>();

    /** Corre algo dentro de {@code ticks}. */
    private static void later(MinecraftServer s, int ticks, Runnable r) {
        DESPUES.add(new Despues(ahora(s) + ticks, r));
    }

    private static void tick(MinecraftServer s) {
        if (!DESPUES.isEmpty()) {
            long t = ahora(s);
            for (Despues d : new ArrayList<>(DESPUES)) {
                if (t >= d.cuando()) {
                    DESPUES.remove(d);
                    d.que().run();
                }
            }
        }
        if (fase == Fase.NADA) {
            return;
        }
        long now = ahora(s);
        switch (fase) {
            case ASCENSORES -> tickAscensores(s, now);
            case BAJANDO -> {
                tickBajando(s, now);
                // por si algún viaje no llegó (se reinició algo): a los 60 s empieza igual
                if (now - faseDesde > viajeTicks + 1200) {
                    empezarJuego(s);
                }
            }
            case JUEGO -> tickJuego(s, now);
            default -> {
            }
        }
        if (!mirando.isEmpty()) {
            tickMirando(s);
        }
    }

    /**
     * Mientras bajan: el letrero del elevador (en la barra de abajo) cuenta los pisos desde el 13 hasta el de cada quien, y a la mitad del
     * viaje se va la luz un instante (ahí, sin que se note, pasan a la cabina del hotel) y el número se vuelve loco.
     */
    private static void tickBajando(MinecraftServer s, long now) {
        long t = now - faseDesde - 20;   // (la puerta tarda 1 s en cerrar)
        int mitad = viajeTicks / 2;
        if (t < 0 || t > viajeTicks + 20) {
            return;
        }
        boolean loco = t >= mitad - 4 && t <= mitad + 16;
        for (Map.Entry<UUID, Integer> d : destino.entrySet()) {
            ServerPlayer sp = s.getPlayerList().getPlayer(d.getKey());
            if (sp == null) {
                continue;
            }
            if (t == mitad - 4) {
                Fx.flash(sp, 0xF0000000, 24);
                sp.playNotifySound(SoundEvents.BEACON_DEACTIVATE, SoundSource.MASTER, 0.6F, 0.7F);
            }
            if (now % 5 != 0) {
                continue;
            }
            int dest = d.getValue();
            Component n;
            if (loco) {
                n = Component.literal("13").withStyle(ChatFormatting.OBFUSCATED);
            } else {
                double q = Math.min(1.0, t / (double) viajeTicks);
                n = Component.literal(String.valueOf(Math.round(dest + (13 - dest) * (1 - q))));
            }
            Plat.msg(sp, Component.translatable(MOD + ".hotel.bajando", n).withStyle(t >= viajeTicks ? ChatFormatting.GOLD : ChatFormatting.RED),
                    true);
        }
    }

    private static void tickJuego(MinecraftServer s, long now) {
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        ServerLevel l = nivel(s);
        // las trampas que ya se jalaron: ¡pum!
        for (Map.Entry<Integer, Object[]> d : new ArrayList<>(disparo.entrySet())) {
            if (now >= (long) d.getValue()[0]) {
                disparo.remove(d.getKey());
                disparar(s, d.getKey(), (UUID) d.getValue()[1]);
            }
        }
        // la puerta de la salida se cierra sola un rato después
        for (Map.Entry<Integer, Long> c : new ArrayList<>(cierraPuerta.entrySet())) {
            if (now >= c.getValue()) {
                if (alguienEnLaPuerta(s, c.getKey())) {
                    cierraPuerta.put(c.getKey(), now + 20);
                    continue;
                }
                cierraPuerta.remove(c.getKey());
                PuertaHotel h = puerta(l, c.getKey());
                if (h != null) {
                    h.cerrar();
                    l.playSound(null, h.blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1F, 0.6F);
                }
                barrera(s, c.getKey(), true);
            }
        }
        List<Entity> adentro = new ArrayList<>();
        for (Entity e : vivos(s)) {
            if (!salieron.contains(e.getUUID()) && enHotel(s, e)) {
                adentro.add(e);
            }
        }
        // ¿alguien cayó por el tobogán?
        for (Entity e : adentro) {
            if (enLaSalida(s, e)) {
                salir(s, e);
            }
        }
        // las tablas con clavos
        if (now % 5 == 0) {
            for (Entity e : adentro) {
                if (id(l.getBlockState(e.blockPosition())).equals("puas")) {
                    Long u = pinchado.get(e.getUUID());
                    if (u == null || now - u > 20) {
                        pinchado.put(e.getUUID(), now);
                        e.hurt(l.damageSources().cactus(), 2F);
                        if (e instanceof net.minecraft.world.entity.LivingEntity le) {
                            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
                        }
                        l.playSound(null, e.blockPosition(), SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.BLOCKS, 1F, 0.6F);
                    }
                }
            }
        }
        // al cambiar de piso, el número del piso (como el letrero de un elevador)
        if (now % 10 == 0) {
            BlockPos o = Complejo.origen(s, "hotel");
            for (Entity e : adentro) {
                if (o != null && e instanceof ServerPlayer sp) {
                    int piso = Hotel.piso((int) Math.floor(e.getY() + 0.2 - o.getY()));
                    Integer antes = pisoDe.put(sp.getUUID(), piso);
                    if (antes == null || antes != piso) {
                        Plat.msg(sp, Component.translatable(MOD + ".hotel.piso", piso).withStyle(ChatFormatting.GOLD), true);
                    }
                }
            }
        }
        charcos(s, l, now, adentro);
        candelabros(s, l, now, adentro);
        // las ratas que se acabaron vuelven a salir de su nido (a los 45 s)
        if (now % 20 == 0) {
            for (int k = 0; k < 64; k++) {
                if (Complejo.pos(s, "hotel.rata." + k) == null) {
                    break;
                }
                Integer id = ratas.get(k);
                boolean viva = id != null && l.getEntity(id) instanceof Rata r && r.isAlive();
                if (!viva) {
                    Long m = ratasMuertas.get(k);
                    if (m == null) {
                        ratasMuertas.put(k, now);
                    } else if (now - m > 900) {
                        ratasMuertas.remove(k);
                        rata(s, k);
                    }
                }
            }
        }
        // los Botones cambian de piso si en el suyo no hay nadie
        if (now % 400 == 0) {
            moverBotones(s, l, adentro);
        }
        if (now % 10 == 0) {
            monitores(s, l, adentro);
        }
        // el Coleccionista ayuda un poquito (si no, casi nadie saldría): a los 5 minutos de que acabe, si es piso par o impar; a los 2,
        // el piso (las dos puertas de ese piso: una puede ser la trampa)
        int resta = ev.resta(s);
        if (ayudas && segundos >= 360 && resta == 300 && !ayudaDada.contains(300)) {
            ayudaDada.add(300);
            int ps = salida / 2 + 1;
            Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 1.2F);
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".hotel.chat.ayuda_par",
                    Component.translatable(MOD + (ps % 2 == 0 ? ".hotel.par" : ".hotel.impar")))));
            Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL ayuda par");
        }
        if (ayudas && segundos >= 180 && resta == 120 && !ayudaDada.contains(120)) {
            ayudaDada.add(120);
            Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 1.2F);
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".hotel.chat.ayuda_piso", salida / 2 + 1)));
            Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL ayuda piso");
        }
        // el último minuto: el Coleccionista avisa y se va la luz alrededor de cada quien; los últimos 30 s, el reloj
        if (segundos >= 90 && resta == 60 && !ayudaDada.contains(60)) {
            ayudaDada.add(60);
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".hotel.chat.minuto")));
            apagon(s, l, adentro);
            Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL ultimo minuto apagadas={}", apagadas.size());
        }
        if (resta > 0 && resta <= 30 && resta != ultimoTic) {
            ultimoTic = resta;
            for (Entity e : adentro) {
                if (e instanceof ServerPlayer sp) {
                    sp.playNotifySound(ModRegistry.HOTEL_RELOJ.get(), SoundSource.MASTER, resta <= 10 ? 1F : 0.6F, resta % 2 == 0 ? 1F : 0.82F);
                }
            }
        }
        if (now % 20 == 0) {
            ruidos(s, l, adentro);
        }
        // ¿se acabó?
        boolean nadie = adentro.isEmpty() && now - faseDesde > 100;
        if (ev.resta(s) == 0 || nadie) {
            fin(s);
        }
    }

    /** El apagón del último minuto: las lámparas cerca de cada quien se apagan 2 s (con su golpe de luz que se corta) y vuelven. */
    private static void apagon(MinecraftServer s, ServerLevel l, List<Entity> adentro) {
        Set<BlockPos> ps = new HashSet<>();
        for (Entity e : adentro) {
            BlockPos c = e.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-14, -2, -14), c.offset(14, 3, 14))) {
                BlockState st = l.getBlockState(q);
                if (st.hasProperty(DecoBlock.ENCENDIDA) && st.hasProperty(DecoBlock.FALLA) && st.getValue(DecoBlock.ENCENDIDA)) {
                    ps.add(q.immutable());
                }
            }
            if (e instanceof ServerPlayer sp) {
                sp.playNotifySound(SoundEvents.BEACON_DEACTIVATE, SoundSource.MASTER, 1F, 0.6F);
                // (las luces escondidas del techo no se tocan: la oscuridad la pone el efecto, que se quita solo)
                sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            }
        }
        for (BlockPos q : ps) {
            l.setBlock(q, l.getBlockState(q).setValue(DecoBlock.ENCENDIDA, false), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        }
        apagadas.addAll(ps);
        later(s, 40, () -> prender(s, true));
    }

    /** Vuelve a prender lo que apagó el apagón. */
    private static void prender(MinecraftServer s, boolean sonido) {
        if (apagadas.isEmpty()) {
            return;
        }
        ServerLevel l = nivel(s);
        for (BlockPos q : apagadas) {
            BlockState st = l.getBlockState(q);
            if (st.hasProperty(DecoBlock.ENCENDIDA) && !st.getValue(DecoBlock.ENCENDIDA)) {
                l.setBlock(q, st.setValue(DecoBlock.ENCENDIDA, true), net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
        }
        if (sonido) {
            for (ServerPlayer sp : l.players()) {
                if (enHotel(s, sp)) {
                    sp.playNotifySound(SoundEvents.BEACON_ACTIVATE, SoundSource.MASTER, 0.7F, 0.8F);
                }
            }
        }
        apagadas.clear();
    }

    /** ¿El Coleccionista da pistas por la bocina a los 5 y a los 2 minutos del final? */
    public static boolean ayudas = true;
    private static final Set<Integer> ayudaDada = new HashSet<>();
    private static final Map<UUID, Long> ruidoEn = new HashMap<>();

    /**
     * Los ruidos del hotel: a cada quien, de vez en cuando, algo que suena cerca (pero no se ve): una puerta que azotan, pasos, el timbre
     * de recepción a lo lejos (aunque el Botones no esté), y lo de cada piso: el piano del salón, las lavadoras, el fuego del piso quemado,
     * las goteras del inundado, el viento en el ático.
     */
    private static void ruidos(MinecraftServer s, ServerLevel l, List<Entity> adentro) {
        BlockPos o = Complejo.origen(s, "hotel");
        if (o == null) {
            return;
        }
        long now = ahora(s);
        for (Entity e : adentro) {
            if (!(e instanceof ServerPlayer sp)) {
                continue;
            }
            Long t = ruidoEn.get(sp.getUUID());
            if (t == null) {
                ruidoEn.put(sp.getUUID(), now + 100 + R.nextInt(200));
                continue;
            }
            if (now < t) {
                continue;
            }
            ruidoEn.put(sp.getUUID(), now + 160 + R.nextInt(300));
            int piso = Hotel.piso((int) Math.floor(sp.getY() + 0.2 - o.getY()));
            double ang = R.nextDouble() * Math.PI * 2, d = 6 + R.nextDouble() * 8;
            Vec3 p = sp.position().add(Math.cos(ang) * d, 1, Math.sin(ang) * d);
            net.minecraft.sounds.SoundEvent ev;
            float vol = 0.8F, tono = 0.8F + R.nextFloat() * 0.3F;
            int cual = R.nextInt(4);
            if (cual == 0) {
                ev = switch (piso) {
                    case 2 -> SoundEvent3.propio("piano_" + R.nextInt(8));
                    case 4 -> SoundEvents.PISTON_CONTRACT;
                    case 5 -> SoundEvents.FIRE_AMBIENT;
                    case 6, 8 -> SoundEvents.POINTED_DRIPSTONE_DRIP_WATER;
                    case 10 -> SoundEvents.ELYTRA_FLYING;
                    default -> SoundEvents.WOODEN_DOOR_CLOSE;
                };
                if (piso == 4) {
                    tono = 0.5F;
                }
                if (piso == 10) {
                    vol = 0.3F;
                    tono = 0.5F;
                }
            } else if (cual == 1) {
                ev = SoundEvents.WOODEN_DOOR_CLOSE;
                tono = 0.6F + R.nextFloat() * 0.2F;
            } else if (cual == 2) {
                ev = SoundEvents.WOOD_STEP;
                vol = 0.6F;
            } else {
                ev = R.nextInt(3) == 0 ? ModRegistry.HOTEL_TIMBRE.get() : SoundEvents.CHAIN_STEP;
                vol = 0.35F;
            }
            l.playSound(null, p.x, p.y, p.z, ev, SoundSource.AMBIENT, vol, tono);
        }
    }

    /** (Un sonido propio del mod sin registrar, por su nombre.) */
    private static final class SoundEvent3 {
        static net.minecraft.sounds.SoundEvent propio(String id) {
            return net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation(MOD, id));
        }
    }

    private static PuertaHotel puerta(ServerLevel l, int k) {
        Integer id = puertas.get(k);
        return id != null && l.getEntity(id) instanceof PuertaHotel h ? h : null;
    }

    /** ¿Está parado en la boca del tobogán (el clóset de la salida)? */
    private static boolean enLaSalida(MinecraftServer s, Entity e) {
        if (salida < 0) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            BlockPos c = Complejo.pos(s, "hotel.serv." + salida + ".k" + i);
            if (c != null && e.getBlockX() == c.getX() && e.getBlockZ() == c.getZ() && e.getY() >= c.getY() - 0.5
                    && e.getY() < c.getY() + 0.9) {
                return true;
            }
        }
        return false;
    }

    /** Cae por el tobogán: aparece arriba del tubo de colores y cae a la sala de espera. */
    public static void salir(MinecraftServer s, Entity e) {
        if (salieron.contains(e.getUUID())) {
            return;
        }
        BlockPos t = Complejo.pos(s, "hotel.tobogan.arriba");
        if (t == null) {
            return;
        }
        salieron.add(e.getUUID());
        Pieza p = Evento.get(s).pieza(e);
        int n = salieron.size();
        if (e instanceof ServerPlayer sp) {
            quitarTubos(sp);
            sp.teleportTo(s.overworld(), t.getX() + 0.5, t.getY(), t.getZ() + 0.5, R.nextFloat() * 360F, 60F);
            sp.setDeltaMovement(0, -0.6, 0);
            sp.hurtMarked = true;
            sp.playNotifySound(ModRegistry.HOTEL_TOBOGAN.get(), SoundSource.MASTER, 1F, 1F);
            Fx.shake(sp, 0.6F, 50);
            Plat.msg(sp, Component.translatable(MOD + ".hotel.saliste", n).withStyle(ChatFormatting.GREEN), true);
        } else {
            e.moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, 0F, 0F);
        }
        if (p != null) {
            Anuncio.todos(s, Component.translatable(MOD + ".hotel.chat.salio", Anuncio.nombre(p), n).withStyle(ChatFormatting.GREEN));
        }
        // al pizarrón de la sala de espera, con su tiempo
        anotar(s, p != null ? p.nombre : e.getName().getString(), fase == Fase.JUEGO ? (int) ((ahora(s) - faseDesde) / 20) : 0);
        // al caer en el colchón: confeti de colores (de fiesta de niños... en este lugar)
        later(s, 48, () -> confeti(s));
        // la puerta se cierra detrás de él
        cierraPuerta.put(salida, ahora(s) + 50);
        Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL salio {} n={}", p == null ? e.getName().getString() : p.nombre, n);
    }

    private static void confeti(MinecraftServer s) {
        BlockPos c = Complejo.pos(s, "hotel.espera.colchon");
        if (c == null) {
            return;
        }
        ServerLevel l = nivel(s);
        float[][] colores = {{0.95F, 0.2F, 0.2F}, {1F, 0.85F, 0.15F}, {0.3F, 0.85F, 0.3F}, {0.25F, 0.6F, 1F}, {0.9F, 0.4F, 0.9F}};
        for (float[] k : colores) {
            l.sendParticles(new DustParticleOptions(new Vector3f(k[0], k[1], k[2]), 1.2F), c.getX() + 0.5, c.getY() + 1.2, c.getZ() + 0.5, 18,
                    1.4, 0.8, 1.4, 0.05);
        }
        l.playSound(null, c, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    private static void quitarTubos(ServerPlayer sp) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            if (sp.getInventory().getItem(i).is(ModRegistry.TUBO.get())) {
                sp.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }

    // ------------------------------------------------------------------ las puertas

    /** Clic derecho en el hueco de una puerta de servicio, en un tubo suelto o en un botiquín. */
    private static void clic(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || fase != Fase.JUEGO) {
            return;
        }
        MinecraftServer s = sp.getServer();
        ServerLevel l = sp.serverLevel();
        BlockPos p = e.getPos();
        BlockState st = l.getBlockState(p);
        String id = id(st);
        if (st.is(net.minecraft.world.level.block.Blocks.BARRIER)) {
            for (int k = 0; k < 64; k++) {
                BlockPos d = Complejo.pos(s, "hotel.serv." + k);
                if (d == null) {
                    break;
                }
                if (d.equals(p) || d.above().equals(p)) {
                    e.setCanceled(true);
                    if (presaValida(sp)) {
                        abrir(s, sp, k);
                    }
                    return;
                }
            }
        } else if (id.equals("tubo_suelo") && presaValida(sp)) {
            e.setCanceled(true);
            if (sp.getInventory().contains(new ItemStack(ModRegistry.TUBO.get()))) {
                Plat.msg(sp, Component.translatable(MOD + ".hotel.ya_tubo").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            l.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            sp.getInventory().add(new ItemStack(ModRegistry.TUBO.get()));
            l.playSound(null, p, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1F, 0.7F);
            Plat.msg(sp, Component.translatable(MOD + ".hotel.tubo").withStyle(ChatFormatting.YELLOW), true);
        } else if (id.equals("botiquin") && presaValida(sp)) {
            e.setCanceled(true);
            sp.heal(8F);
            l.setBlock(p, Complejo.estado(MOD + ":botiquin_vacio[facing=" + st.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)
                    .getName() + "]"), 3);
            l.playSound(null, p, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1F, 1.2F);
            l.sendParticles(ParticleTypes.HEART, sp.getX(), sp.getY() + 1.8, sp.getZ(), 4, 0.3, 0.2, 0.3, 0);
            Plat.msg(sp, Component.translatable(MOD + ".hotel.curado").withStyle(ChatFormatting.GREEN), true);
        }
    }

    /** Alguien abre la puerta K. */
    public static void abrir(MinecraftServer s, Entity quien, int k) {
        ServerLevel l = nivel(s);
        PuertaHotel h = puerta(l, k);
        if (h == null || !h.cerrada()) {
            return;
        }
        if (k == salida) {
            h.abrir(false);
            l.playSound(null, h.blockPosition(), ModRegistry.HOTEL_RECHINIDO.get(), SoundSource.BLOCKS, 1.2F, 1.1F);
            cierraPuerta.put(k, ahora(s) + 200);
            later(s, 22, () -> barrera(s, k, false));
        } else if (muertes.contains(k) && !disparadas.contains(k)) {
            // el hilo se tensa mientras abre... y a la mitad, ¡pum!
            h.abrir(false);
            l.playSound(null, h.blockPosition(), ModRegistry.HOTEL_RECHINIDO.get(), SoundSource.BLOCKS, 1.2F, 0.9F);
            disparadas.add(k);
            disparo.put(k, new Object[] {ahora(s) + 13, quien.getUUID()});
            later(s, 30, () -> barrera(s, k, false));
        } else {
            h.abrir(false);
            l.playSound(null, h.blockPosition(), ModRegistry.HOTEL_RECHINIDO.get(), SoundSource.BLOCKS, 1.2F, 0.95F + R.nextFloat() * 0.15F);
            later(s, 22, () -> barrera(s, k, false));
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL abre {} puerta={} ({}) tipo={}", quien.getName().getString(), k, Hotel.codigo(k), h.tipo());
    }

    /** Pone o quita la barrera del hueco de la puerta K (con la hoja abierta se puede pasar). */
    private static void barrera(MinecraftServer s, int k, boolean poner) {
        if (fase != Fase.JUEGO && poner) {
            return;
        }
        BlockPos p = Complejo.pos(s, "hotel.serv." + k);
        if (p == null) {
            return;
        }
        ServerLevel l = nivel(s);
        BlockState b = poner ? net.minecraft.world.level.block.Blocks.BARRIER.defaultBlockState()
                : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        l.setBlock(p, b, 2);
        l.setBlock(p.above(), b, 2);
    }

    /** ¿Hay alguien parado en el hueco de la puerta K? (para no cerrarle la puerta encima). */
    private static boolean alguienEnLaPuerta(MinecraftServer s, int k) {
        BlockPos p = Complejo.pos(s, "hotel.serv." + k);
        return p != null && !nivel(s).getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new AABB(p).expandTowards(0, 1, 0).inflate(0.1), e -> !e.isSpectator()).isEmpty();
    }

    /** ¡Pum! La escopeta le vuela la cabeza a quien abrió (solo a él) y cae de espaldas, sin cabeza. */
    private static void disparar(MinecraftServer s, int k, UUID quien) {
        ServerLevel l = nivel(s);
        Integer tid = trampasEnt.get(k);
        Entity t = tid == null ? null : l.getEntity(tid);
        if (t instanceof TrampaEscopeta te) {
            te.disparar();
        }
        BlockPos p = Complejo.pos(s, "hotel.serv." + k);
        Vec3 donde = p == null ? Vec3.ZERO : Vec3.atCenterOf(p);
        l.playSound(null, donde.x, donde.y, donde.z, ModRegistry.HOTEL_DISPARO.get(), SoundSource.HOSTILE, 3F, 1F);
        l.sendParticles(ParticleTypes.LARGE_SMOKE, donde.x, donde.y + 0.7, donde.z, 12, 0.2, 0.2, 0.2, 0.02);
        Evento ev = Evento.get(s);
        Pieza x = ev.pieza(quien);
        Entity e = x == null ? null : Evento.entidad(s, x);
        if (e == null) {
            return;
        }
        // la cabeza: sangre y pedazos para atrás, y el eco del disparo por todo el piso
        Vec3 c = e.getEyePosition();
        Vec3 atras = e.position().subtract(donde).normalize();
        com.axeliscore.fx.Efectos.lanzar(l, MOD + ":cabeza", c, atras, 1F);
        l.sendParticles(new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.02F), 1.6F), c.x, c.y, c.z, 60, 0.25, 0.25, 0.25, 0.4);
        l.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                net.minecraft.world.level.block.Blocks.BONE_BLOCK.defaultBlockState()), c.x, c.y, c.z, 14, 0.2, 0.2, 0.2, 0.3);
        for (ServerPlayer o : l.players()) {
            if (o.distanceToSqr(c) < 30 * 30) {
                Fx.shake(o, o == e ? 2.5F : 0.8F, o == e ? 20 : 8);
            }
        }
        if (e instanceof ServerPlayer sp) {
            Fx.flash(sp, 0xFFB01010, 14);
        }
        // los demás, lejos, oyen el eco sordo del disparo por todo el hotel (y no saben dónde fue)
        for (ServerPlayer o : l.players()) {
            if (o != e && o.distanceToSqr(c) >= 30 * 30 && enHotel(s, o)) {
                o.playNotifySound(ModRegistry.HOTEL_DISPARO.get(), SoundSource.HOSTILE, 0.35F, 0.55F);
                Fx.shake(o, 0.2F, 6);
            }
        }
        Cuerpos.SIN_CABEZA.add(quien);
        boolean muerto = ev.eliminarEnPrueba(s, x, PRUEBA);
        if (!muerto) {
            // (se salvó porque ya no quedan suficientes para la final: solo la herida)
            Cuerpos.SIN_CABEZA.remove(quien);
            e.hurt(l.damageSources().explosion(null, null), 10F);
        } else {
            // su monitor se queda un momento sin señal, y al pizarrón con una cruz
            perdida.put(quien, new Object[] {ahora(s), x.nombre});
            anotar(s, x.nombre, -1);
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL disparo puerta={} victima={} eliminado={}", k, x.nombre, muerto);
    }

    // ------------------------------------------------------------------ las demás trampas

    /** Los charcos con electricidad: chispean un momento (aviso) y luego dan toques (a quien esté parado en ellos). */
    private static void charcos(MinecraftServer s, ServerLevel l, long now, List<Entity> adentro) {
        if (now % 2 != 0) {
            return;
        }
        for (int k = 0; k < 64; k++) {
            BlockPos c = Complejo.pos(s, "hotel.charco." + k);
            if (c == null) {
                break;
            }
            long[] e = charcos.computeIfAbsent(k, x -> new long[] {now + 60 + R.nextInt(200)});
            long t = now - e[0];
            if (t < 0) {
                continue;
            }
            if (t < 30) {
                if (t % 6 == 0) {
                    l.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.getX() + 0.5, c.getY() + 0.1, c.getZ() + 0.5, 6, 0.35, 0.05, 0.35, 0.05);
                }
                if (t == 0) {
                    l.playSound(null, c, ModRegistry.CHISPA.get(), SoundSource.BLOCKS, 0.7F, 1.3F);
                }
            } else if (t < 50) {
                if (t % 4 == 0) {
                    l.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.getX() + 0.5, c.getY() + 0.3, c.getZ() + 0.5, 20, 0.4, 0.3, 0.4, 0.2);
                    l.playSound(null, c, ModRegistry.CHISPA.get(), SoundSource.BLOCKS, 1F, 0.8F + R.nextFloat() * 0.4F);
                }
                if (t == 30) {
                    for (Entity x : adentro) {
                        if (x.blockPosition().equals(c)) {
                            x.hurt(l.damageSources().lightningBolt(), 3F);
                            if (x instanceof ServerPlayer sp) {
                                Fx.flash(sp, 0x8890B0FF, 8);
                                Fx.shake(sp, 0.8F, 10);
                            }
                        }
                    }
                }
            } else {
                e[0] = now + 100 + R.nextInt(220);
            }
        }
    }

    /** Los candelabros: al pasar abajo, cruje la cadena… y se cae (con el golpe de la caída). */
    private static void candelabros(MinecraftServer s, ServerLevel l, long now, List<Entity> adentro) {
        for (int k = 0; k < 64; k++) {
            BlockPos c = Complejo.pos(s, "hotel.cand." + k);
            if (c == null) {
                break;
            }
            if (caidos.contains(k)) {
                continue;
            }
            Long cae = caeEn.get(k);
            if (cae == null) {
                for (Entity e : adentro) {
                    double dx = e.getX() - (c.getX() + 0.5), dz = e.getZ() - (c.getZ() + 0.5);
                    if (dx * dx + dz * dz < 1.6 && Math.abs(e.getY() - c.getY()) < 1.5) {
                        caeEn.put(k, now + 12);
                        l.playSound(null, c.above(3), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.2F, 0.6F);
                        l.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.FALLING_DUST,
                                net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState()), c.getX() + 0.5, c.getY() + 3.2, c.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0);
                        break;
                    }
                }
            } else if (now >= cae) {
                caidos.add(k);
                caeEn.remove(k);
                BlockPos a = c.above(3);
                BlockState st = l.getBlockState(a);
                if (!st.isAir()) {
                    FallingBlockEntity f = FallingBlockEntity.fall(l, a, st);
                    f.setHurtsEntities(2.5F, 8);
                    f.dropItem = false;
                }
                l.playSound(null, c, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.4F, 0.6F);
            }
        }
    }

    private static void moverBotones(MinecraftServer s, ServerLevel l, List<Entity> adentro) {
        Map<Integer, Integer> gente = new HashMap<>();
        BlockPos o = Complejo.origen(s, "hotel");
        if (o == null) {
            return;
        }
        for (Entity e : adentro) {
            gente.merge(Hotel.piso((int) Math.floor(e.getY() - o.getY())), 1, Integer::sum);
        }
        List<Botones> bs = new ArrayList<>();
        for (int id : botones) {
            if (l.getEntity(id) instanceof Botones b) {
                bs.add(b);
            }
        }
        for (Botones b : bs) {
            if (b.estado() != Botones.Estado.PATRULLA || gente.getOrDefault(b.piso, 0) > 0 && R.nextInt(3) > 0) {
                continue;
            }
            int mejor = -1, max = 0;
            for (Map.Entry<Integer, Integer> g : gente.entrySet()) {
                boolean ocupado = false;
                for (Botones x : bs) {
                    ocupado |= x != b && x.piso == g.getKey();
                }
                if (!ocupado && g.getKey() != b.piso && g.getValue() > max) {
                    max = g.getValue();
                    mejor = g.getKey();
                }
            }
            if (mejor > 0) {
                b.irAPiso(mejor);
            }
        }
    }

    // ------------------------------------------------------------------ la sala de espera: los monitores

    private static void ponerMonitores(MinecraftServer s) {
        ServerLevel l = nivel(s);
        monitores.clear();
        for (int i = 0; i < Hotel.MONITORES; i++) {
            BlockPos p = Complejo.pos(s, "hotel.monitor." + i);
            Monitor m = p == null ? null : ModRegistry.MONITOR.get().create(l);
            if (m == null) {
                continue;
            }
            m.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0F, 0F);
            m.numero(i + 1);
            l.addFreshEntity(m);
            monitores.add(m.getId());
        }
    }

    private record EnPantalla(UUID id, String nombre, String linea, float vida) {
    }

    private static void monitores(MinecraftServer s, ServerLevel l, List<Entity> adentro) {
        Evento ev = Evento.get(s);
        BlockPos o = Complejo.origen(s, "hotel");
        List<EnPantalla> ps = new ArrayList<>();
        for (Entity p : adentro) {
            int piso = o == null ? 0 : Hotel.piso((int) Math.floor(p.getY() - o.getY()));
            float vida = p instanceof net.minecraft.world.entity.LivingEntity le ? le.getHealth() : 20F;
            ps.add(new EnPantalla(p.getUUID(), nombre(ev, p), String.valueOf(piso), vida));
        }
        // quien acaba de caer en una trampa: su monitor dice "señal perdida" unos 4 s
        long ya = ahora(s);
        perdida.entrySet().removeIf(en -> ya - (long) en.getValue()[0] > 80);
        for (Map.Entry<UUID, Object[]> en : perdida.entrySet()) {
            ps.add(new EnPantalla(en.getKey(), (String) en.getValue()[1], Monitor.PERDIDA, 0F));
        }
        ps.sort((a, b) -> a.nombre().compareToIgnoreCase(b.nombre()));
        // si hay más de los que caben, se van turnando cada 5 s
        int corre = ps.size() > monitores.size() ? (int) (ya / 100) * monitores.size() : 0;
        for (int i = 0; i < monitores.size(); i++) {
            if (!(l.getEntity(monitores.get(i)) instanceof Monitor m)) {
                continue;
            }
            if (ps.isEmpty() || i >= ps.size() && corre == 0) {
                m.mostrar(null, "", "", 0F);
                continue;
            }
            EnPantalla p = ps.get((i + corre) % ps.size());
            m.mostrar(p.id(), p.nombre(), p.linea(), p.vida());
        }
    }

    private static String nombre(Evento ev, Entity e) {
        Pieza p = ev.pieza(e);
        return p != null ? p.nombre : e.getName().getString();
    }

    /** Clic en un monitor: se ve desde los ojos de esa persona (agacharse para volver). */
    public static void espectar(ServerPlayer quien, UUID objetivo) {
        MinecraftServer s = quien.getServer();
        if (s == null) {
            return;
        }
        ServerPlayer t = objetivo == null ? null : s.getPlayerList().getPlayer(objetivo);
        if (t == null || !presaValida(t) || quien == t) {
            Plat.msg(quien, Component.translatable(MOD + ".hotel.monitor.nadie").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (!salieron.contains(quien.getUUID()) && !Evento.get(s).esOrganizador(quien.getUUID())) {
            return;
        }
        if (!mirando.containsKey(quien.getUUID())) {
            antesDeMirar.put(quien.getUUID(), quien.position());
        }
        mirando.put(quien.getUUID(), objetivo);
        quien.setGameMode(GameType.SPECTATOR);
        quien.setCamera(t);
        Plat.msg(quien, Component.translatable(MOD + ".hotel.monitor.viendo", t.getGameProfile().getName()).withStyle(ChatFormatting.AQUA), true);
        Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL mira {} -> {}", quien.getGameProfile().getName(), t.getGameProfile().getName());
    }

    private static void tickMirando(MinecraftServer s) {
        for (Map.Entry<UUID, UUID> m : new ArrayList<>(mirando.entrySet())) {
            ServerPlayer w = s.getPlayerList().getPlayer(m.getKey());
            ServerPlayer t = s.getPlayerList().getPlayer(m.getValue());
            if (w == null) {
                mirando.remove(m.getKey());
                continue;
            }
            boolean sigue = fase == Fase.JUEGO && t != null && presaValida(t) && w.getCamera() == t && w.isSpectator();
            if (!sigue) {
                volver(w);
            } else if (s.overworld().getGameTime() % 40 == 0) {
                Plat.msg(w, Component.translatable(MOD + ".hotel.monitor.viendo", t.getGameProfile().getName()).withStyle(ChatFormatting.AQUA), true);
            }
        }
    }

    /** Deja de ver por los ojos de alguien: de vuelta a su lugar en la sala de espera. */
    private static void volver(ServerPlayer w) {
        mirando.remove(w.getUUID());
        Vec3 a = antesDeMirar.remove(w.getUUID());
        w.setCamera(w);
        Pieza p = w.getServer() == null ? null : Evento.get(w.getServer()).pieza(w);
        boolean vivo = p != null && p.vivo();
        if (vivo || p == null) {
            w.setGameMode(GameType.ADVENTURE);
        }
        if (a != null) {
            w.teleportTo(w.serverLevel(), a.x, a.y, a.z, w.getYRot(), 0F);
        }
    }

    // ------------------------------------------------------------------ las pistas en las paredes

    private static CompoundTag estadoPistas() {
        CompoundTag t = new CompoundTag();
        t.putBoolean("reveladas", !pistas.isEmpty() || !tablero.isEmpty());
        ListTag l = pistas.copy();
        l.addAll(tablero.copy());
        t.put("l", l);
        return t;
    }

    /** Un renglón más en el pizarrón (segundos -1: cayó en una trampa). */
    private static void anotar(MinecraftServer s, String nombre, int seg) {
        orden.add(new Object[] {nombre, seg, false});
        escribirTablero(s);
        syncPistas(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL tablero renglones={} escritos={}", orden.size(), tablero.size());
    }

    /**
     * El pizarrón del orden de salida, en la pared oeste de la sala de espera, escrito con gis: arriba el título y luego quién salió (con
     * su tiempo) y quién cayó (con una cruz, en rojo), en tres columnas de 11. Se ve como las pistas (el cliente lo traduce).
     */
    private static void escribirTablero(MinecraftServer s) {
        ListTag lista = new ListTag();
        BlockPos p = Complejo.pos(s, "hotel.espera.tablero");
        if (p != null) {
            double x = p.getX() + 0.006, zc = p.getZ() + 0.5;
            lista.add(gis(x, p.getY() + 2.72, zc, "@" + MOD + ".hotel.tablero#0", 0.022F, Pistas.TIZA));
            int fila = 0, n = 0;
            for (Object[] o : orden) {
                int seg = (int) o[1], col = fila / 11, r = fila % 11;
                if (col > 2) {
                    break;
                }
                String t = seg < 0 ? "✗ " + o[0] : (++n) + ". " + o[0] + "  " + seg / 60 + ":" + String.format("%02d", seg % 60);
                // (mirando al pizarrón, la izquierda es +z)
                lista.add(gis(x, p.getY() + 2.42 - r * 0.21, zc + 3.4 - col * 3.4, t, 0.016F,
                        seg < 0 || (boolean) o[2] ? Pistas.ROJO : Pistas.TIZA));
                fila++;
            }
        }
        tablero = lista;
    }

    private static CompoundTag gis(double x, double y, double z, String texto, float esc, int estilo) {
        CompoundTag t = new CompoundTag();
        t.putInt("p", Pistas.PARED);
        t.putFloat("a", 90F);
        t.putDouble("x", x);
        t.putDouble("y", y);
        t.putDouble("z", z);
        t.putFloat("s", esc);
        t.putInt("e", estilo);
        t.putFloat("g", (R.nextFloat() - 0.5F) * 2.5F);
        t.putString("t", texto);
        return t;
    }

    private static void syncPistas(MinecraftServer s) {
        PlayerStore.Sync.toAll(s, KEY_PISTAS, estadoPistas());
    }

    /**
     * Escribe las pistas: en cada piso, una dice si la salida está más arriba, más abajo o "en este piso"; las otras son consejos (el
     * hilo, la luz de colores, los tubos, el timbre). Van en la pared más cercana a su marca (si no hay, en el piso), con gis o en rojo.
     */
    private static void escribirPistas(MinecraftServer s) {
        ServerLevel l = nivel(s);
        ListTag lista = new ListTag();
        int pisoSalida = salida / 2 + 1;
        String[] consejos = {"hotel.pista.hilo", "hotel.pista.luz", "hotel.pista.agachate", "hotel.pista.tubo", "hotel.pista.correr",
                "hotel.pista.timbre", "hotel.pista.cuartos", "hotel.pista.botiquin"};
        Map<Integer, Integer> porPiso = new HashMap<>();
        int c = 0;
        BlockPos o = Complejo.origen(s, "hotel");
        for (int k = 0; k < 64 && o != null; k++) {
            BlockPos p = Complejo.pos(s, "hotel.nota." + k);
            if (p == null) {
                break;
            }
            int piso = Hotel.piso(p.getY() - 1 - o.getY());
            int cual = porPiso.merge(piso, 1, Integer::sum);
            // (se manda la clave: cada quien la lee en su idioma; "@clave#renglón")
            String clave;
            int estilo;
            // las dos primeras de cada piso dicen hacia dónde está la salida (una en rojo y otra en gis); las demás son consejos
            if (cual <= 2) {
                clave = piso < pisoSalida ? "hotel.pista.arriba" : piso > pisoSalida ? "hotel.pista.abajo" : "hotel.pista.aqui";
                estilo = cual == 1 ? Pistas.ROJO : Pistas.TIZA;
            } else {
                clave = consejos[c++ % consejos.length];
                estilo = Pistas.TIZA;
            }
            String[] lineas = {"@" + MOD + "." + clave + "#0", "@" + MOD + "." + clave + "#1"};
            escribir(l, lista, p, lineas, estilo);
        }
        pistas = lista;
        syncPistas(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL pistas={} piso_salida={}", lista.size(), pisoSalida);
    }

    private static void escribir(ServerLevel l, ListTag lista, BlockPos p, String[] lineas, int estilo) {
        // la pared más cercana (a la altura del pecho)
        int[][] d = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
        float[] ang = {0F, 180F, 270F, 90F};
        int lado = -1;
        for (int i = 0; i < 4; i++) {
            BlockPos w = p.offset(d[i][0], 1, d[i][1]);
            if (l.getBlockState(w).isCollisionShapeFullBlock(l, w)) {
                lado = i;
                break;
            }
        }
        for (int j = 0; j < lineas.length; j++) {
            CompoundTag t = new CompoundTag();
            double x, y, z;
            if (lado >= 0) {
                x = p.getX() + 0.5 + d[lado][0] * (0.5 - 0.006);
                z = p.getZ() + 0.5 + d[lado][1] * (0.5 - 0.006);
                y = p.getY() + 1.45 - j * 0.22;
                t.putInt("p", Pistas.PARED);
                t.putFloat("a", ang[lado]);
            } else {
                x = p.getX() + 0.5;
                z = p.getZ() + 0.5 + j * 0.22;
                y = p.getY() + 0.006;
                t.putInt("p", Pistas.PISO);
                t.putFloat("a", 0F);
            }
            t.putDouble("x", x);
            t.putDouble("y", y);
            t.putDouble("z", z);
            t.putFloat("s", 0.016F);
            t.putInt("e", estilo);
            t.putFloat("g", (R.nextFloat() - 0.5F) * 8F);
            t.putString("t", lineas[j].trim());
            lista.add(t);
        }
    }

    // ------------------------------------------------------------------ el tubo no le pega a nadie más que a las ratas

    private static void golpe(LivingAttackEvent e) {
        if (fase != Fase.JUEGO || !(e.getSource().getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        MinecraftServer s = sp.getServer();
        if (s == null || !enHotel(s, sp) && !enHotel(s, e.getEntity())) {
            return;
        }
        // en el hotel no hay golpes entre jugadores, y al Botones no se le hace nada
        if (e.getEntity() instanceof net.minecraft.world.entity.player.Player || e.getEntity() instanceof com.coleccionista.bot.PiezaBot) {
            e.setCanceled(true);
            return;
        }
        if (e.getEntity() instanceof Rata r && sp.getMainHandItem().is(ModRegistry.TUBO.get()) && e.getAmount() < 20F) {
            // con el tubo, de un golpe
            e.setCanceled(true);
            r.hurt(sp.damageSources().playerAttack(sp), 20F);
            sp.level().playSound(null, r.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 1.8F);
        }
    }

    // ------------------------------------------------------------------ 4. el final

    /** Se acabó (el tiempo, o el organizador lo terminó): los que siguen adentro se quedan; los demás regresan dormidos a la sala. */
    public static boolean fin(MinecraftServer s) {
        if (fase != Fase.JUEGO && fase != Fase.BAJANDO && fase != Fase.ASCENSORES) {
            return false;
        }
        fase(s, Fase.FIN);
        Evento ev = Evento.get(s);
        ev.reloj(s, 0);
        for (Map.Entry<UUID, UUID> m : new ArrayList<>(mirando.entrySet())) {
            ServerPlayer w = s.getPlayerList().getPlayer(m.getKey());
            if (w != null) {
                volver(w);
            }
        }
        int quedaron = 0;
        for (Pieza p : ev.vivos()) {
            if (!salieron.contains(p.id)) {
                if (ev.eliminarEnPrueba(s, p, PRUEBA)) {
                    quedaron++;
                }
            }
        }
        if (ultimoCae && salieron.size() >= 2) {
            Pieza u = ev.pieza(salieron.get(salieron.size() - 1));
            if (u != null && u.vivo()) {
                Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".hotel.chat.ultimo", Anuncio.nombre(u))));
                ev.eliminarEnPrueba(s, u, PRUEBA);
                // en el pizarrón, su renglón se pone rojo
                for (int i = orden.size() - 1; i >= 0; i--) {
                    Object[] o = orden.get(i);
                    if ((int) o[1] >= 0 && u.nombre.equals(o[0])) {
                        o[2] = true;
                        break;
                    }
                }
                escribirTablero(s);
            }
        }
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".hotel.chat.fin", quedaron)));
        quitarEntidades(s);
        restaurarRegen(s);
        prender(s, false);
        // (las pistas del hotel se borran; el pizarrón se queda hasta que despierten)
        pistas = new ListTag();
        syncPistas(s);
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            quitarTubos(sp);
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL fin salieron={} quedaron={} ultimo_cae={}", salieron.size(), quedaron, ultimoCae);
        Pantallas.hablar(s, "hotel_fin", () -> dormir(s));
        return true;
    }

    /** El gas dulce: todos se duermen en la sala de espera… */
    private static void dormir(MinecraftServer s) {
        for (Pieza p : Evento.get(s).vivos()) {
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                Fx.flash(sp, 0xFF000000, 90);
                sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4, false, false));
                sp.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.MASTER, 0.8F, 0.5F);
            }
        }
        BlockPos c = Complejo.pos(s, "hotel.espera.centro");
        if (c != null) {
            nivel(s).sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.getX() + 0.5, c.getY() + 2, c.getZ() - 5, 80, 6, 1.5, 4, 0.01);
        }
        later(s, 50, () -> despertar(s));
    }

    /** …y despiertan en la sala grande: ahora sí, equipos. */
    private static void despertar(MinecraftServer s) {
        BlockPos c = Complejo.pos(s, "sala.centro");
        int n = 0;
        for (Pieza p : Evento.get(s).vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null || c == null) {
                continue;
            }
            double x = c.getX() + 0.5 - (n % 6) * 1.5, z = c.getZ() + 0.5 + (n / 6 - 2) * 1.5;
            if (e instanceof ServerPlayer sp) {
                sp.teleportTo(s.overworld(), x, c.getY(), z, 270F, 0F);
                sp.setHealth(sp.getMaxHealth());
                sp.getFoodData().setFoodLevel(20);
                sp.removeAllEffects();
            } else {
                e.moveTo(x, c.getY(), z, 270F, 0F);
            }
            n++;
        }
        hecho = true;
        fase(s, Fase.NADA);
        orden.clear();
        tablero = new ListTag();
        syncPistas(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL regreso vivos={}", n);
        later(s, 40, () -> Capitulo1.equipos(s, "hotel_regreso"));
    }

    private static void restaurarRegen(MinecraftServer s) {
        if (regenAntes != null) {
            s.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(regenAntes, s);
            regenAntes = null;
        }
    }

    /** Quita lo que puso el hotel (puertas, trampas, Botones, carritos, ratas y monitores). */
    private static void quitarEntidades(MinecraftServer s) {
        for (Entity e : toList(nivel(s).getAllEntities())) {
            if (e instanceof PuertaHotel || e instanceof TrampaEscopeta || e instanceof Botones || e instanceof Carrito || e instanceof Rata
                    || e instanceof Monitor) {
                e.discard();
            }
        }
        puertas.clear();
        trampasEnt.clear();
        botones.clear();
        ratas.clear();
        ratasMuertas.clear();
        monitores.clear();
        disparo.clear();
        cierraPuerta.clear();
    }

    private static List<Entity> toList(Iterable<Entity> it) {
        List<Entity> l = new ArrayList<>();
        for (Entity e : it) {
            l.add(e);
        }
        return l;
    }

    private static void limpiarTodo(MinecraftServer s) {
        salieron.clear();
        cabina.clear();
        pinchado.clear();
        pisoDe.clear();
        ayudaDada.clear();
        ruidoEn.clear();
        caidos.clear();
        caeEn.clear();
        charcos.clear();
        destino.clear();
        perdida.clear();
        orden.clear();
        tablero = new ListTag();
        ultimoTic = -1;
        prender(s, false);
        cierraEn = -1;
        viajes = 0;
        llegados = 0;
    }

    /** Todo como antes (al reiniciar el evento o con {@code /evento hotel limpiar}). */
    public static void reiniciar(MinecraftServer s) {
        for (Map.Entry<UUID, UUID> m : new ArrayList<>(mirando.entrySet())) {
            ServerPlayer w = s.getPlayerList().getPlayer(m.getKey());
            if (w != null) {
                volver(w);
            }
        }
        mirando.clear();
        quitarEntidades(s);
        restaurarRegen(s);
        limpiarTodo(s);
        pistas = new ListTag();
        syncPistas(s);
        hecho = false;
        salida = -1;
        muertes.clear();
        fase = Fase.NADA;
    }

    // ------------------------------------------------------------------ para el panel y las pruebas

    public static String resumen() {
        return fase.name() + (salida >= 0 ? " salida=" + Hotel.codigo(salida) : "") + " trampas=" + codigos(muertes) + " salieron="
                + salieron.size() + " ultimoCae=" + ultimoCae + (hecho ? " (ya se jugó)" : "");
    }

    private static String codigos(Set<Integer> s) {
        List<String> l = new ArrayList<>();
        for (int k : s) {
            l.add(Hotel.codigo(k));
        }
        Collections.sort(l);
        return String.join(",", l);
    }

    /** El código de la salida ("7B") o "" (para el panel: el organizador sabe cuál es). */
    public static String salida() {
        return salida >= 0 ? Hotel.codigo(salida) : "";
    }

    public static String trampas() {
        return codigos(muertes);
    }

    public static int salieron() {
        return salieron.size();
    }

    /** Para probar: cuántas lámparas tiene apagadas el apagón del último minuto (0 si no hay apagón). */
    public static int apagadas() {
        return apagadas.size();
    }

    /** Para probar: la puerta de la salida o la de una trampa (-1 si no hay). */
    public static int puertaDe(boolean trampa) {
        if (!trampa) {
            return salida;
        }
        return muertes.isEmpty() ? -1 : muertes.iterator().next();
    }

    /** Para probar: lleva a alguien frente a la puerta K (del lado del pasillo). */
    public static boolean frente(MinecraftServer s, Entity e, int k) {
        BlockPos p = Complejo.pos(s, "hotel.serv." + k);
        if (p == null) {
            return false;
        }
        float yaw = Complejo.yaw("hotel.serv." + k);
        double r = Math.toRadians(yaw);
        double x = p.getX() + 0.5 + Math.sin(r) * 1.2, z = p.getZ() + 0.5 - Math.cos(r) * 1.2;
        if (e instanceof ServerPlayer sp) {
            sp.teleportTo(s.overworld(), x, p.getY(), z, yaw, 0F);
        } else {
            e.moveTo(x, p.getY(), z, yaw, 0F);
        }
        return true;
    }
}
