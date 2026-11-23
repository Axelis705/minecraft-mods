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
import com.coleccionista.mapa.Cloacas;
import com.coleccionista.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.item.ItemTossEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Prueba 1: Encadenados. Las parejas despiertan encadenadas a la pared de la Sala de los grilletes y atadas entre sí con una cadena
 * corta. La llave de cada uno está en el barril de su pareja: hay que pedírsela y que te la lance. Luego cruzan las Cloacas juntos
 * (compuertas que necesitan dos personas, tres tuberías de las que solo una sirve, un puente angosto) mientras el gas avanza por
 * detrás. Solo hay lugar en el Refugio para algunas parejas: cuando se llena, la reja se cierra y las demás quedan en la colección.
 * <p>
 * La cadena la mantiene cada cliente para su propio jugador (así se siente suave); el servidor solo jala a los bots y regresa a quien se
 * haya separado de más (por lag). El gas, las compuertas, las llaves y quién se salva los decide el servidor.
 */
public final class Encadenados {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":encadenados";
    /** A cada jugador: cuánto aire le queda en el gas (0..1). */
    public static final String KEY_AIRE = MOD + ":aire";
    public static final String PRUEBA = "Encadenados";
    /** Largo de la cadena entre la pareja y radio del grillete de la pared (bloques). */
    public static final double LARGO = 4.5, RADIO = 1.3;
    /** Ticks seguidos en el gas antes de caer (10 s). */
    public static final int GAS_MAX = 200;
    /** Lo que dura la prueba y cuánto tarda en salir el gas (segundos); el cupo (-1 = automático). */
    public static int segundos = 600, gracia = 45, cupoFijo = -1;

    public enum Fase {
        NADA, LISTA, HABLANDO, JUEGO, CIERRE, FIN
    }

    private static Fase fase = Fase.NADA;
    /** Integrantes de cada equipo, por número de equipo (en el orden de la cadena). */
    private static final Map<Integer, List<UUID>> equipos = new LinkedHashMap<>();
    private static final Map<UUID, Integer> lugares = new HashMap<>();
    /** Quién sigue encadenado a la pared: el punto del grillete y la argolla (para dibujar la cadena). */
    private static final Map<UUID, Vec3> grilletes = new HashMap<>(), argollas = new HashMap<>();
    private static final List<Integer> salvados = new ArrayList<>();
    private static final Map<UUID, Integer> enGas = new HashMap<>(), lejos = new HashMap<>(), aireEnviado = new HashMap<>();
    private static final boolean[] abierta = new boolean[3];
    private static final long[] abiertaHasta = new long[3];
    private static final boolean[][] pisadas = new boolean[3][2];
    private static final Deque<UUID> porEliminar = new ArrayDeque<>();
    private static int cupo, tuboBueno, eliminadosPrueba;
    /** Tiempo de juego (ticks desde que se abrió la reja, sin contar la pausa). */
    private static long t;
    private static long tpEn = -1, presentarEn = -1, proximaElim, cierreT;
    private static double gasX0, gasVel, gasCierreDesde = Double.NaN;
    private static boolean cambio;

    private Encadenados() {
    }

    public static void init() {
        AxelisCore.onServerTick(Encadenados::tick);
        AxelisCore.onLogin(p -> {
            if (fase != Fase.NADA) {
                PlayerStore.Sync.toPlayer(p, KEY, estado(Plat.server(p)));
            }
        });
        MinecraftForge.EVENT_BUS.addListener(Encadenados::alTirar);
    }

    public static Fase fase() {
        return fase;
    }

    public static boolean activa() {
        return fase != Fase.NADA;
    }

    // ------------------------------------------------------------------ lugares del mapa

    private static BlockPos marca(MinecraftServer s, String m) {
        return Complejo.pos(s, m);
    }

    private static BlockPos zona(MinecraftServer s) {
        return Complejo.origen(s, "cloacas");
    }

    /** La x del mundo de una x local de las Cloacas. */
    private static double x(MinecraftServer s, double xLocal) {
        BlockPos o = zona(s);
        return o == null ? 0 : o.getX() + xLocal;
    }

    /** ¿Está dentro de las Cloacas? */
    private static boolean dentro(MinecraftServer s, Entity e) {
        BlockPos o = zona(s);
        if (o == null) {
            return false;
        }
        double lx = e.getX() - o.getX(), ly = e.getY() - o.getY(), lz = e.getZ() - o.getZ();
        return lx >= Cloacas.X0 && lx <= Cloacas.X1 + 1 && lz >= Cloacas.Z0 && lz <= Cloacas.Z1 + 1 && ly >= Cloacas.Y0 - 2 && ly <= Cloacas.Y1 + 2;
    }

    private static boolean enRefugio(MinecraftServer s, Entity e) {
        BlockPos o = zona(s);
        if (o == null) {
            return false;
        }
        double lx = e.getX() - o.getX(), lz = e.getZ() - o.getZ(), ly = e.getY() - o.getY();
        return lx >= Cloacas.REFUGIO_X && lx <= 112 && lz >= 6 && lz <= 23 && ly >= 0 && ly <= 7;
    }

    private static double gasX(MinecraftServer s) {
        if (fase == Fase.CIERRE && !Double.isNaN(gasCierreDesde)) {
            // al cerrar, el gas llena todo hasta la reja en tres segundos
            return gasCierreDesde + (x(s, Cloacas.GAS_FIN) - gasCierreDesde) * Mth.clamp((t - cierreT) / 60.0, 0, 1);
        }
        long dt = t - gracia * 20L;
        if (fase != Fase.JUEGO || dt <= 0) {
            return gasX0;
        }
        return Math.min(x(s, Cloacas.GAS_FIN), gasX0 + dt * gasVel);
    }

    private static boolean gasActivo() {
        return fase == Fase.CIERRE || fase == Fase.JUEGO && t > gracia * 20L;
    }

    // ------------------------------------------------------------------ preparar y empezar

    private static Map<Integer, List<Pieza>> agrupar(Evento ev) {
        Map<Integer, List<Pieza>> m = new TreeMap<>();
        for (Pieza p : ev.vivos()) {
            m.computeIfAbsent(p.equipo, k -> new ArrayList<>()).add(p);
        }
        for (List<Pieza> l : m.values()) {
            l.sort(Comparator.comparingInt(p -> p.numero));
        }
        return m;
    }

    /**
     * Lleva a todas las piezas (con un fundido a negro) a la Sala de los grilletes: cada pareja junta, encadenadas a la pared, con la
     * llave de su pareja en su barril. Si no hay parejas, las arma. Devuelve el error, o null si salió bien.
     */
    public static Component preparar(MinecraftServer s) {
        if (!Complejo.construido(s, "cloacas")) {
            return Component.translatable(MOD + ".enc.sin_mapa");
        }
        if (fase == Fase.HABLANDO || fase == Fase.JUEGO || fase == Fase.CIERRE) {
            return Component.translatable(MOD + ".enc.ya_empezo");
        }
        Evento ev = Evento.get(s);
        if (ev.vivos().size() < 2) {
            return Component.translatable(MOD + ".enc.pocos");
        }
        limpiar(s, false);
        Map<Integer, List<Pieza>> grupos = agrupar(ev);
        boolean armar = grupos.containsKey(-1) || grupos.values().stream().anyMatch(l -> l.size() < 2);
        if (armar) {
            ev.equiposAlAzar(s, 2);
            grupos = agrupar(ev);
        }
        int lugar = 0;
        for (Map.Entry<Integer, List<Pieza>> e : grupos.entrySet()) {
            List<UUID> ids = new ArrayList<>();
            for (Pieza p : e.getValue()) {
                if (lugar >= Cloacas.LUGARES) {
                    break;
                }
                lugares.put(p.id, lugar++);
                ids.add(p.id);
            }
            if (!ids.isEmpty()) {
                equipos.put(e.getKey(), ids);
            }
        }
        // las llaves cruzadas: en el barril de cada uno está la llave del siguiente de su equipo
        ServerLevel l = s.overworld();
        for (List<UUID> ids : equipos.values()) {
            for (int j = 0; j < ids.size(); j++) {
                UUID dueno = ids.get((j + 1) % ids.size());
                Pieza pd = ev.pieza(dueno);
                BlockPos b = marca(s, "enc.lugar." + lugares.get(ids.get(j)) + ".barril");
                if (b != null && l.getBlockEntity(b) instanceof BarrelBlockEntity barril) {
                    barril.clearContent();
                    barril.setItem(13, LlaveGrillete.crear(dueno, pd == null ? 0 : pd.numero));
                    barril.setChanged();
                }
            }
        }
        // rejas: la de la sala cerrada, compuertas cerradas, una sola tubería abierta, la salida abierta
        reja(s, "enc.reja_inicio", Cloacas.INICIO_ANCHO, Cloacas.INICIO_ALTO, false);
        for (int k = 1; k <= 2; k++) {
            reja(s, "enc.compuerta." + k, Cloacas.COMPUERTA_ANCHO, Cloacas.COMPUERTA_ALTO, false);
            abierta[k] = false;
        }
        tuboBueno = 1 + l.getRandom().nextInt(3);
        for (int i = 1; i <= 3; i++) {
            reja(s, "enc.tubo." + i, Cloacas.TUBO_ANCHO, Cloacas.TUBO_ALTO, i == tuboBueno);
        }
        reja(s, "enc.reja_salida", Cloacas.SALIDA_ANCHO, Cloacas.SALIDA_ALTO, true);
        int n = equipos.size();
        cupo = cupoFijo > 0 ? Math.min(cupoFijo, n) : n <= 1 ? 1 : Math.max(1, n - Math.max(1, Math.round(n * 0.25F)));
        ev.prueba(s, PRUEBA);
        ev.reloj(s, 0);
        Pantallas.modo(s, "estatica", "");
        t = 0;
        gasX0 = x(s, Cloacas.GAS_INICIO) - 2;
        gasVel = 0;
        fase = Fase.LISTA;
        long now = s.overworld().getGameTime();
        Escena.fundido(s, MOD + ".enc.fundido", 80);
        tpEn = now + 22;
        presentarEn = now + 90;
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_ENCADENADOS preparada equipos={} cupo={} tubo={}", n, cupo, tuboBueno);
        return null;
    }

    /** Pone a cada pieza en su lugar, encadenada a la pared. */
    private static void alSusLugares(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Map.Entry<UUID, Integer> e : lugares.entrySet()) {
            Pieza p = ev.pieza(e.getKey());
            if (p == null || !p.vivo()) {
                continue;
            }
            String m = "enc.lugar." + e.getValue();
            BlockPos b = marca(s, m), a = marca(s, m + ".argolla");
            if (b == null) {
                continue;
            }
            Vec3 v = new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
            grilletes.put(p.id, v);
            if (a != null) {
                float ay = Complejo.yaw(m + ".argolla");
                double fx = -Mth.sin(ay * Mth.DEG_TO_RAD), fz = Mth.cos(ay * Mth.DEG_TO_RAD);
                argollas.put(p.id, new Vec3(a.getX() + 0.5 - fx * 0.28, a.getY() + 0.35, a.getZ() + 0.5 - fz * 0.28));
            }
            Entity en = Evento.entidad(s, p);
            if (en != null) {
                mover(s, en, v, Complejo.yaw(m));
                if (en instanceof ServerPlayer sp) {
                    sp.setGameMode(GameType.ADVENTURE);
                    sp.setHealth(sp.getMaxHealth());
                    sp.getFoodData().setFoodLevel(20);
                }
            }
        }
        cambio = true;
    }

    private static void mover(MinecraftServer s, Entity e, Vec3 v, float yaw) {
        if (e instanceof ServerPlayer sp) {
            if (sp.isSleeping()) {
                sp.stopSleeping();
            }
            sp.teleportTo(s.overworld(), v.x, v.y, v.z, yaw, 0F);
        } else {
            e.moveTo(v.x, v.y, v.z, yaw, 0F);
            e.setYHeadRot(yaw);
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** El Coleccionista explica la prueba en las pantallas y al terminar se abre la reja (o de una vez, sin discurso). */
    public static boolean empezar(MinecraftServer s, boolean conDiscurso) {
        if (fase != Fase.LISTA) {
            return false;
        }
        if (conDiscurso) {
            fase = Fase.HABLANDO;
            cambio = true;
            Pantallas.hablar(s, "encadenados", () -> arrancar(s));
        } else {
            Pantallas.callar(s);
            arrancar(s);
        }
        return true;
    }

    private static void arrancar(MinecraftServer s) {
        if (fase != Fase.LISTA && fase != Fase.HABLANDO) {
            return;
        }
        fase = Fase.JUEGO;
        t = 0;
        reja(s, "enc.reja_inicio", Cloacas.INICIO_ANCHO, Cloacas.INICIO_ALTO, true);
        Evento ev = Evento.get(s);
        ev.reloj(s, segundos);
        // el gas llega a la reja de la salida justo cuando se acaba el tiempo
        long ticks = Math.max(200, segundos * 20L - gracia * 20L);
        gasVel = (x(s, Cloacas.GAS_FIN) - gasX0) / ticks;
        Pantallas.modo(s, "coleccionista", "");
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".enc.chat.empieza", cupo, equipos.size(), gracia)));
        Anuncio.sonido(s, SoundEvents.BELL_RESONATE, 1F, 0.6F);
        Anuncio.sonido(s, SoundEvents.IRON_DOOR_OPEN, 1F, 0.5F);
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_ENCADENADOS empieza segundos={} gracia={}", segundos, gracia);
    }

    // ------------------------------------------------------------------ llaves

    /** Usar una llave (clic derecho): si es la tuya y estás encadenado, quedas libre. Devuelve si se gastó. */
    public static boolean usarLlave(ServerPlayer sp, ItemStack st) {
        MinecraftServer s = Plat.server(sp);
        UUID dueno = LlaveGrillete.dueno(st);
        if (fase == Fase.NADA || dueno == null) {
            Plat.msg(sp, Component.translatable(MOD + ".enc.llave_vieja").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (!grilletes.containsKey(sp.getUUID())) {
            Plat.msg(sp, Component.translatable(MOD + ".enc.ya_libre").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (!dueno.equals(sp.getUUID())) {
            Plat.msg(sp, Component.translatable(MOD + ".enc.llave_ajena", String.format("%02d", LlaveGrillete.pieza(st)))
                    .withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        soltar(s, sp.getUUID());
        return true;
    }

    /** Quita el grillete de la pared (la cadena con su pareja sigue). */
    public static boolean soltar(MinecraftServer s, UUID id) {
        if (grilletes.remove(id) == null) {
            return false;
        }
        argollas.remove(id);
        Pieza p = Evento.get(s).pieza(id);
        Entity e = p == null ? null : Evento.entidad(s, p);
        if (e != null) {
            e.level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1F, 0.8F);
            e.level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 0.8F, 0.6F);
            if (e instanceof ServerPlayer sp) {
                Plat.msg(sp, Component.translatable(MOD + ".enc.libre").withStyle(ChatFormatting.GREEN), true);
                quitarLlaves(sp);
            }
        }
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_GRILLETE {} libre", p == null ? "?" : p.etiqueta());
        return true;
    }

    public static int soltarTodos(MinecraftServer s) {
        int n = 0;
        for (UUID id : new ArrayList<>(grilletes.keySet())) {
            if (soltar(s, id)) {
                n++;
            }
        }
        return n;
    }

    private static void quitarLlaves(ServerPlayer sp) {
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ModRegistry.LLAVE.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /** Al tirar una llave (tecla Q) que es de otro: vuela hacia su dueño. */
    private static void alTirar(ItemTossEvent e) {
        if (fase == Fase.NADA || !(e.getPlayer() instanceof ServerPlayer sp)) {
            return;
        }
        ItemEntity it = e.getEntity();
        UUID dueno = LlaveGrillete.dueno(it.getItem());
        if (!it.getItem().is(ModRegistry.LLAVE.get()) || dueno == null || dueno.equals(sp.getUUID())) {
            return;
        }
        MinecraftServer s = Plat.server(sp);
        Pieza p = Evento.get(s).pieza(dueno);
        Entity dest = p == null ? null : Evento.entidad(s, p);
        if (dest == null) {
            return;
        }
        Vec3 d = dest.position().add(0, 0.6, 0).subtract(it.position());
        double h = Math.hypot(d.x, d.z);
        int ticks = (int) Mth.clamp(h * 3.0, 8, 24);
        it.setDeltaMovement(d.x / (ticks * 0.9), 0.04 * ticks / 2 + d.y / ticks, d.z / (ticks * 0.9));
        it.setPickUpDelay(12);
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 1F, 1.4F);
        Coleccionista.LOGGER.info("COLECCIONISTA_LLAVE lanzada a {}", p.etiqueta());
    }

    /** Las llaves que se quedan tiradas ruedan hasta su dueño (para que nadie se quede atorado por un mal tiro). */
    private static void llavesTiradas(MinecraftServer s) {
        BlockPos o = zona(s);
        if (o == null) {
            return;
        }
        AABB caja = new AABB(o.getX() + Cloacas.X0, o.getY() + Cloacas.Y0 - 2, o.getZ() + Cloacas.Z0, o.getX() + 20, o.getY() + Cloacas.Y1 + 2,
                o.getZ() + Cloacas.Z1 + 1);
        Evento ev = Evento.get(s);
        for (ItemEntity it : s.overworld().getEntitiesOfClass(ItemEntity.class, caja, i -> i.getItem().is(ModRegistry.LLAVE.get()))) {
            UUID dueno = LlaveGrillete.dueno(it.getItem());
            Pieza p = dueno == null ? null : ev.pieza(dueno);
            if (p == null || !p.vivo() || !grilletes.containsKey(dueno)) {
                it.discard();
                continue;
            }
            Entity d = Evento.entidad(s, p);
            if (d != null && it.getAge() > 60 && it.distanceTo(d) > 1.2) {
                it.teleportTo(d.getX(), d.getY() + 0.4, d.getZ());
                it.setDeltaMovement(Vec3.ZERO);
                it.setNoPickUpDelay();
            }
        }
    }

    // ------------------------------------------------------------------ rejas y compuertas

    /** Pone (o quita) una reja de hierro de {@code ancho} (hacia el sur) por {@code alto} desde una marca. */
    private static void reja(MinecraftServer s, String marca, int ancho, int alto, boolean abrir) {
        BlockPos b = marca(s, marca);
        if (b == null) {
            return;
        }
        ServerLevel l = s.overworld();
        List<BlockPos> ps = new ArrayList<>();
        for (int dz = 0; dz < ancho; dz++) {
            for (int y = 0; y < alto; y++) {
                ps.add(b.offset(0, y, dz));
            }
        }
        for (BlockPos p : ps) {
            l.setBlock(p, abrir ? Blocks.AIR.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState(), 3);
        }
        if (!abrir) {
            // que los barrotes se unan entre sí y con las paredes
            for (BlockPos p : ps) {
                BlockState st = Block.updateFromNeighbourShapes(Blocks.IRON_BARS.defaultBlockState(), l, p);
                l.setBlock(p, st, 3);
            }
        }
    }

    /** ¿Hay alguien en el hueco de una reja? (para no cerrarla encima de nadie) */
    private static List<Entity> enReja(MinecraftServer s, String marca, int ancho, int alto) {
        BlockPos b = marca(s, marca);
        if (b == null) {
            return List.of();
        }
        AABB caja = new AABB(b.getX(), b.getY(), b.getZ(), b.getX() + 1, b.getY() + alto, b.getZ() + ancho);
        return s.overworld().getEntities((Entity) null, caja, en -> en.isAlive() && !en.isSpectator() && !(en instanceof ItemEntity));
    }

    private static boolean pisada(MinecraftServer s, BlockPos placa, List<Entity> gente) {
        if (placa == null) {
            return false;
        }
        AABB caja = new AABB(placa.getX(), placa.getY(), placa.getZ(), placa.getX() + 1, placa.getY() + 0.3, placa.getZ() + 1);
        for (Entity e : gente) {
            if (e.getBoundingBox().intersects(caja)) {
                return true;
            }
        }
        return false;
    }

    private static void compuertas(MinecraftServer s, List<Entity> gente) {
        for (int k = 1; k <= 2; k++) {
            boolean a = pisada(s, marca(s, "enc.placa." + k + ".a"), gente), b = pisada(s, marca(s, "enc.placa." + k + ".b"), gente);
            if (a != pisadas[k][0] || b != pisadas[k][1]) {
                pisadas[k][0] = a;
                pisadas[k][1] = b;
                cambio = true;
            }
            String m = "enc.compuerta." + k;
            if (a && b) {
                abiertaHasta[k] = t + 120;
                if (!abierta[k]) {
                    abierta[k] = true;
                    reja(s, m, Cloacas.COMPUERTA_ANCHO, Cloacas.COMPUERTA_ALTO, true);
                    sonidoEn(s, m, SoundEvents.IRON_DOOR_OPEN, 0.7F);
                    sonidoEn(s, m, SoundEvents.PISTON_CONTRACT, 0.6F);
                    cambio = true;
                    Coleccionista.LOGGER.info("COLECCIONISTA_COMPUERTA {} abierta", k);
                }
            } else if (abierta[k] && t > abiertaHasta[k]) {
                if (!enReja(s, m, Cloacas.COMPUERTA_ANCHO, Cloacas.COMPUERTA_ALTO).isEmpty()) {
                    abiertaHasta[k] = t + 10;
                } else {
                    abierta[k] = false;
                    reja(s, m, Cloacas.COMPUERTA_ANCHO, Cloacas.COMPUERTA_ALTO, false);
                    sonidoEn(s, m, SoundEvents.IRON_DOOR_CLOSE, 0.7F);
                    sonidoEn(s, m, SoundEvents.PISTON_EXTEND, 0.6F);
                    cambio = true;
                    Coleccionista.LOGGER.info("COLECCIONISTA_COMPUERTA {} cerrada", k);
                }
            }
        }
    }

    private static void sonidoEn(MinecraftServer s, String marca, net.minecraft.sounds.SoundEvent e, float tono) {
        BlockPos b = marca(s, marca);
        if (b != null) {
            s.overworld().playSound(null, b, e, SoundSource.BLOCKS, 1.2F, tono);
        }
    }

    // ------------------------------------------------------------------ cada tick

    private static void tick(MinecraftServer s) {
        if (fase == Fase.NADA) {
            return;
        }
        Evento ev = Evento.get(s);
        long now = s.overworld().getGameTime();
        if (tpEn > 0 && now >= tpEn) {
            tpEn = -1;
            alSusLugares(s);
        }
        if (presentarEn > 0 && now >= presentarEn) {
            presentarEn = -1;
            Escena.presentacion(s, "I", MOD + ".enc.titulo", MOD + ".enc.lema");
            Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.45F);
        }
        if (ev.fase() == Evento.Fase.PAUSA) {
            if (cambio) {
                enviar(s);
            }
            return;
        }
        if (fase == Fase.JUEGO || fase == Fase.CIERRE) {
            t++;
        }
        Map<UUID, Entity> gente = new HashMap<>();
        for (List<UUID> ids : equipos.values()) {
            for (UUID id : ids) {
                Pieza p = ev.pieza(id);
                if (p != null && p.vivo()) {
                    Entity e = Evento.entidad(s, p);
                    if (e != null && e.level() == s.overworld()) {
                        gente.put(id, e);
                    }
                }
            }
        }
        if (fase != Fase.FIN && tpEn < 0) {
            fisica(s, gente);
        }
        if (fase == Fase.JUEGO || fase == Fase.CIERRE) {
            if (fase == Fase.JUEGO) {
                compuertas(s, new ArrayList<>(gente.values()));
                salvar(s, ev, gente);
            }
            gas(s, ev, gente);
            if (now % 20 == 0) {
                llavesTiradas(s);
            }
        } else if (fase == Fase.LISTA || fase == Fase.HABLANDO) {
            if (now % 20 == 0) {
                llavesTiradas(s);
            }
        }
        if (fase == Fase.CIERRE && t >= proximaElim) {
            UUID id = porEliminar.poll();
            if (id == null) {
                fin(s);
            } else {
                Pieza p = ev.pieza(id);
                if (p != null && p.vivo()) {
                    if (ev.eliminarEnPrueba(s, p, PRUEBA)) {
                        eliminadosPrueba++;
                    }
                }
                proximaElim = t + 30;
            }
        }
        if (cambio || now % 40 == 0) {
            enviar(s);
        }
    }

    /** La cadena y los grilletes: jala a los bots y regresa a quien se separó de más. */
    private static void fisica(MinecraftServer s, Map<UUID, Entity> gente) {
        for (Map.Entry<UUID, Vec3> g : grilletes.entrySet()) {
            Entity e = gente.get(g.getKey());
            if (e == null) {
                continue;
            }
            double d = e.position().distanceTo(g.getValue());
            if (d > RADIO) {
                if (!(e instanceof ServerPlayer)) {
                    tirar(e, g.getValue(), d - RADIO);
                }
                int n = lejos.merge(e.getUUID(), 1, Integer::sum);
                if (d > RADIO + 6 || d > RADIO + 2.5 && n > 20) {
                    mover(s, e, g.getValue(), e.getYRot());
                    lejos.remove(e.getUUID());
                }
            } else {
                lejos.remove(e.getUUID());
            }
        }
        for (Map.Entry<Integer, List<UUID>> eq : equipos.entrySet()) {
            if (salvados.contains(eq.getKey())) {
                continue;
            }
            List<Entity> l = new ArrayList<>();
            for (UUID id : eq.getValue()) {
                Entity e = gente.get(id);
                if (e != null) {
                    l.add(e);
                }
            }
            for (int i = 0; i + 1 < l.size(); i++) {
                Entity a = l.get(i), b = l.get(i + 1);
                double d = a.distanceTo(b);
                if (d <= LARGO) {
                    continue;
                }
                boolean botA = !(a instanceof ServerPlayer), botB = !(b instanceof ServerPlayer);
                if (botA && !grilletes.containsKey(a.getUUID())) {
                    tirar(a, b.position(), (d - LARGO) * (botB ? 0.5 : 1));
                }
                if (botB && !grilletes.containsKey(b.getUUID())) {
                    tirar(b, a.position(), (d - LARGO) * (botA ? 0.5 : 1));
                }
                // muy separados (lag o un salto raro): el que va adelante regresa con el otro
                int n = d > LARGO + 3.5 ? lejosPar.merge(a.getUUID(), 1, Integer::sum) : 0;
                if (n == 0) {
                    lejosPar.remove(a.getUUID());
                }
                if (d > LARGO + 10 || n > 30) {
                    lejosPar.remove(a.getUUID());
                    Entity ade, atr;
                    if (grilletes.containsKey(a.getUUID())) {
                        ade = b;
                        atr = a;
                    } else if (grilletes.containsKey(b.getUUID())) {
                        ade = a;
                        atr = b;
                    } else {
                        ade = a.getX() > b.getX() ? a : b;
                        atr = ade == a ? b : a;
                    }
                    Vec3 hacia = ade.position().subtract(atr.position());
                    Vec3 v = atr.position().add(hacia.normalize().scale(Math.min(2.0, hacia.length())));
                    mover(s, ade, new Vec3(v.x, atr.getY(), v.z), ade.getYRot());
                }
            }
        }
    }

    private static final Map<UUID, Integer> lejosPar = new HashMap<>();

    private static void tirar(Entity e, Vec3 hacia, double exceso) {
        Vec3 d = hacia.subtract(e.position());
        double len = d.length();
        if (len < 1e-4) {
            return;
        }
        Vec3 dir = d.scale(1 / len);
        Vec3 v = e.getDeltaMovement();
        double vr = v.dot(dir);
        if (vr < 0) {
            v = v.subtract(dir.scale(vr));
        }
        double f = Math.min(exceso * 0.3, 0.6);
        e.setDeltaMovement(v.add(dir.x * f, Math.max(0, dir.y * f), dir.z * f));
        e.hasImpulse = true;
    }

    // ------------------------------------------------------------------ el gas

    private static void gas(MinecraftServer s, Evento ev, Map<UUID, Entity> gente) {
        if (!gasActivo()) {
            return;
        }
        double gx = gasX(s);
        for (Map.Entry<UUID, Entity> en : gente.entrySet()) {
            Entity e = en.getValue();
            UUID id = en.getKey();
            Integer suyo = equipoDe(id);
            if (suyo != null && salvados.contains(suyo)) {
                continue; // los que ya se salvaron no corren peligro (aunque se asomen)
            }
            boolean dentroGas = dentro(s, e) && e.getX() < gx && !enRefugio(s, e);
            int n = enGas.getOrDefault(id, 0);
            n = dentroGas ? n + 1 : Math.max(0, n - 3);
            enGas.put(id, n);
            if (dentroGas && e instanceof ServerPlayer sp) {
                if (n % 20 == 1) {
                    sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false, false));
                }
                if (n % 30 == 5) {
                    sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_HURT_DROWN, SoundSource.PLAYERS, 0.7F,
                            0.7F + sp.getRandom().nextFloat() * 0.2F);
                }
            }
            if (e instanceof ServerPlayer sp) {
                int aire = (int) (100 * (1 - Math.min(1, n / (float) GAS_MAX)));
                Integer antes = aireEnviado.get(id);
                if (antes == null || antes != aire && (Math.abs(antes - aire) >= 2 || aire == 100 || aire == 0)) {
                    aireEnviado.put(id, aire);
                    CompoundTag a = new CompoundTag();
                    a.putInt("aire", aire);
                    PlayerStore.Sync.toPlayer(sp, KEY_AIRE, a);
                }
            }
            if (n >= GAS_MAX && fase == Fase.JUEGO) {
                // el gas se lleva a toda la pareja (están encadenados)... salvo que con eso no quedara nadie vivo en el evento
                Pieza p = ev.pieza(id);
                Integer eq = p == null ? null : equipoDe(id);
                Coleccionista.LOGGER.info("COLECCIONISTA_GAS {}", p == null ? "?" : p.etiqueta());
                if (eq != null && vivosFuera(ev, eq) == 0) {
                    for (UUID otro : equipos.get(eq)) {
                        enGas.put(otro, GAS_MAX / 2);
                    }
                    Coleccionista.LOGGER.info("COLECCIONISTA_GAS perdonados (no quedaría nadie)");
                    continue;
                }
                if (eq != null) {
                    for (UUID otro : equipos.get(eq)) {
                        Pieza q = ev.pieza(otro);
                        if (q != null && q.vivo()) {
                            if (ev.eliminarEnPrueba(s, q, PRUEBA)) {
                                eliminadosPrueba++;
                            }
                        }
                        enGas.remove(otro);
                    }
                }
                cambio = true;
            }
        }
    }

    /** Cuántas piezas vivas hay fuera de ese equipo. */
    private static int vivosFuera(Evento ev, int eq) {
        int n = 0;
        List<UUID> mios = equipos.getOrDefault(eq, List.of());
        for (Pieza p : ev.vivos()) {
            if (!mios.contains(p.id)) {
                n++;
            }
        }
        return n;
    }

    private static Integer equipoDe(UUID id) {
        for (Map.Entry<Integer, List<UUID>> e : equipos.entrySet()) {
            if (e.getValue().contains(id)) {
                return e.getKey();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ salvarse, cerrar, terminar

    private static void salvar(MinecraftServer s, Evento ev, Map<UUID, Entity> gente) {
        boolean quedan = false;
        for (Map.Entry<Integer, List<UUID>> eq : equipos.entrySet()) {
            if (salvados.contains(eq.getKey())) {
                continue;
            }
            List<Pieza> vivos = new ArrayList<>();
            for (UUID id : eq.getValue()) {
                Pieza p = ev.pieza(id);
                if (p != null && p.vivo()) {
                    vivos.add(p);
                }
            }
            if (vivos.isEmpty()) {
                continue;
            }
            quedan = true;
            boolean todos = true;
            for (Pieza p : vivos) {
                Entity e = gente.get(p.id);
                if (e == null || !enRefugio(s, e)) {
                    todos = false;
                    break;
                }
            }
            if (!todos) {
                continue;
            }
            salvados.add(eq.getKey());
            for (Pieza p : vivos) {
                grilletes.remove(p.id);
                enGas.remove(p.id);
            }
            MutableComponent nombres = Component.empty();
            for (int i = 0; i < vivos.size(); i++) {
                nombres.append(i == 0 ? Component.empty() : Component.literal(" y ").withStyle(ChatFormatting.GRAY)).append(Anuncio.nombre(vivos.get(i)));
            }
            int libres = Math.max(0, cupo - salvados.size());
            Anuncio.todos(s, Component.literal("✔ ").withStyle(ChatFormatting.GREEN).append(nombres)
                    .append(Component.translatable(MOD + ".enc.chat.salvados", libres).withStyle(ChatFormatting.GRAY)));
            Anuncio.sonido(s, SoundEvents.PLAYER_LEVELUP, 0.6F, 0.7F);
            cambio = true;
            Coleccionista.LOGGER.info("COLECCIONISTA_SALVADOS equipo={} {}/{}", eq.getKey() + 1, salvados.size(), cupo);
            if (salvados.size() >= cupo) {
                cerrar(s, false);
                return;
            }
        }
        if (!quedan) {
            fin(s);
        }
    }

    /** Ya no hay lugar (o se acabó el tiempo): la reja se cierra, el gas lo llena todo y los que quedaron afuera caen uno por uno. */
    public static void cerrar(MinecraftServer s, boolean porTiempo) {
        if (fase != Fase.JUEGO) {
            return;
        }
        Evento ev = Evento.get(s);
        // nadie se queda atorado en la reja: el que esté en el hueco y no se salvó, para atrás
        for (Entity e : enReja(s, "enc.reja_salida", Cloacas.SALIDA_ANCHO, Cloacas.SALIDA_ALTO)) {
            Pieza p = ev.pieza(e.getUUID());
            if (p != null) {
                mover(s, e, e.position().add(-1.6, 0, 0), e.getYRot());
            }
        }
        gasCierreDesde = Math.min(gasX(s), x(s, Cloacas.GAS_FIN));
        cierreT = t;
        fase = Fase.CIERRE;
        reja(s, "enc.reja_salida", Cloacas.SALIDA_ANCHO, Cloacas.SALIDA_ALTO, false);
        sonidoEn(s, "enc.reja_salida", SoundEvents.IRON_DOOR_CLOSE, 0.5F);
        Anuncio.sonido(s, SoundEvents.ELDER_GUARDIAN_CURSE, 0.6F, 0.6F);
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + (porTiempo ? ".enc.chat.tiempo" : ".enc.chat.cierre"))));
        // los que quedaron afuera: primero los más atrasados
        List<Pieza> fuera = new ArrayList<>();
        for (Map.Entry<Integer, List<UUID>> eq : equipos.entrySet()) {
            if (salvados.contains(eq.getKey())) {
                continue;
            }
            for (UUID id : eq.getValue()) {
                Pieza p = ev.pieza(id);
                if (p != null && p.vivo()) {
                    fuera.add(p);
                }
            }
        }
        fuera.sort(Comparator.comparingDouble(p -> {
            Entity e = Evento.entidad(s, p);
            return e == null ? -1e9 : e.getX();
        }));
        // nunca se queda el evento sin nadie: si nadie llegó, se perdona a la pareja que llegó más lejos
        if (!fuera.isEmpty() && fuera.size() == ev.vivos().size()) {
            Integer eq = equipoDe(fuera.get(fuera.size() - 1).id);
            if (eq != null) {
                fuera.removeIf(p -> equipos.get(eq).contains(p.id));
                Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".enc.chat.perdon")));
            }
        }
        porEliminar.clear();
        for (Pieza p : fuera) {
            porEliminar.add(p.id);
        }
        proximaElim = t + 60;
        ev.reloj(s, 0);
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_ENCADENADOS cierre fuera={} tiempo={}", fuera.size(), porTiempo);
    }

    private static void fin(MinecraftServer s) {
        fase = Fase.FIN;
        grilletes.clear();
        argollas.clear();
        porEliminar.clear();
        Evento ev = Evento.get(s);
        ev.reloj(s, 0);
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".enc.chat.fin", salvados.size())));
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.6F);
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_ENCADENADOS fin salvados={} eliminados={}", salvados.size(), eliminadosPrueba);
    }

    /** Lo llama el evento cuando se acaba el reloj. Devuelve si lo atendió. */
    public static boolean alAcabarReloj(MinecraftServer s) {
        if (fase != Fase.JUEGO) {
            return false;
        }
        cerrar(s, true);
        return true;
    }

    /** El organizador termina la prueba (sin eliminar a nadie): quita cadenas, abre todo y borra las llaves. */
    public static void terminar(MinecraftServer s) {
        if (fase == Fase.HABLANDO) {
            Pantallas.callar(s);
        }
        limpiar(s, true);
        Coleccionista.LOGGER.info("COLECCIONISTA_ENCADENADOS terminada");
    }

    /** Al reiniciar el evento. */
    public static void reiniciar(MinecraftServer s) {
        if (fase != Fase.NADA) {
            limpiar(s, true);
        }
    }

    private static void limpiar(MinecraftServer s, boolean abrirTodo) {
        fase = Fase.NADA;
        equipos.clear();
        lugares.clear();
        grilletes.clear();
        argollas.clear();
        salvados.clear();
        enGas.clear();
        lejos.clear();
        lejosPar.clear();
        aireEnviado.clear();
        porEliminar.clear();
        eliminadosPrueba = 0;
        tpEn = -1;
        presentarEn = -1;
        gasCierreDesde = Double.NaN;
        t = 0;
        for (int k = 0; k < 3; k++) {
            abierta[k] = false;
            pisadas[k][0] = pisadas[k][1] = false;
        }
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            quitarLlaves(p);
            CompoundTag a = new CompoundTag();
            a.putInt("aire", 100);
            PlayerStore.Sync.toPlayer(p, KEY_AIRE, a);
        }
        if (abrirTodo && Complejo.construido(s, "cloacas")) {
            reja(s, "enc.reja_inicio", Cloacas.INICIO_ANCHO, Cloacas.INICIO_ALTO, true);
            for (int k = 1; k <= 2; k++) {
                reja(s, "enc.compuerta." + k, Cloacas.COMPUERTA_ANCHO, Cloacas.COMPUERTA_ALTO, true);
            }
            for (int i = 1; i <= 3; i++) {
                reja(s, "enc.tubo." + i, Cloacas.TUBO_ANCHO, Cloacas.TUBO_ALTO, true);
            }
            reja(s, "enc.reja_salida", Cloacas.SALIDA_ANCHO, Cloacas.SALIDA_ALTO, true);
            ServerLevel l = s.overworld();
            for (int n = 0; n < Cloacas.LUGARES; n++) {
                BlockPos b = marca(s, "enc.lugar." + n + ".barril");
                if (b != null && l.getBlockEntity(b) instanceof BarrelBlockEntity barril) {
                    barril.clearContent();
                }
            }
        }
        enviar(s);
    }

    // ------------------------------------------------------------------ para el organizador y las pruebas

    /** Lleva a una pieza y a su equipo a una marca (para probar sin jugar). */
    public static boolean ir(MinecraftServer s, Pieza p, String marca) {
        BlockPos b = marca(s, marca);
        if (b == null) {
            return false;
        }
        Integer eq = equipoDe(p.id);
        List<UUID> ids = eq == null ? List.of(p.id) : equipos.get(eq);
        Evento ev = Evento.get(s);
        int i = 0;
        for (UUID id : ids) {
            Pieza q = ev.pieza(id);
            Entity e = q == null || !q.vivo() ? null : Evento.entidad(s, q);
            if (e == null) {
                continue;
            }
            Vec3 destino = new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
            // su pareja va a un lado (hacia atrás, al oeste), salvo que ya esté a menos de una cadena de ahí
            if (!id.equals(p.id) && e.position().distanceTo(destino) <= LARGO) {
                continue;
            }
            grilletes.remove(id);
            argollas.remove(id);
            double dx = id.equals(p.id) ? 0 : -1.2 * (++i);
            mover(s, e, destino.add(dx, 0, 0), Complejo.yaw(marca));
        }
        cambio = true;
        return true;
    }

    public static int cupo() {
        return cupo;
    }

    public static int tuboBueno() {
        return tuboBueno;
    }

    // ------------------------------------------------------------------ a los clientes

    public static CompoundTag estado(MinecraftServer s) {
        CompoundTag t0 = new CompoundTag();
        t0.putString("fase", fase.name());
        t0.putDouble("largo", LARGO);
        t0.putDouble("radio", RADIO);
        t0.putInt("cupo", cupo);
        t0.putInt("salvados", salvados.size());
        t0.putInt("equipos", equipos.size());
        if (fase == Fase.NADA) {
            return t0;
        }
        Evento ev = Evento.get(s);
        ListTag cad = new ListTag();
        if (fase != Fase.FIN) {
            for (Map.Entry<Integer, List<UUID>> eq : equipos.entrySet()) {
                if (salvados.contains(eq.getKey())) {
                    continue;
                }
                List<UUID> vivos = new ArrayList<>();
                for (UUID id : eq.getValue()) {
                    Pieza p = ev.pieza(id);
                    if (p != null && p.vivo()) {
                        vivos.add(id);
                    }
                }
                for (int i = 0; i + 1 < vivos.size(); i++) {
                    CompoundTag c = new CompoundTag();
                    c.putUUID("a", vivos.get(i));
                    c.putUUID("b", vivos.get(i + 1));
                    c.putInt("equipo", eq.getKey());
                    cad.add(c);
                }
            }
        }
        t0.put("cadenas", cad);
        ListTag gr = new ListTag();
        for (Map.Entry<UUID, Vec3> g : grilletes.entrySet()) {
            Pieza p = ev.pieza(g.getKey());
            if (p == null || !p.vivo()) {
                continue;
            }
            CompoundTag c = new CompoundTag();
            c.putUUID("id", g.getKey());
            c.putDouble("x", g.getValue().x);
            c.putDouble("y", g.getValue().y);
            c.putDouble("z", g.getValue().z);
            Vec3 a = argollas.get(g.getKey());
            if (a != null) {
                c.putDouble("ax", a.x);
                c.putDouble("ay", a.y);
                c.putDouble("az", a.z);
            }
            // quién tiene su llave (su pareja: la anterior en la cadena)
            Integer eq = equipoDe(g.getKey());
            if (eq != null) {
                List<UUID> ids = equipos.get(eq);
                int j = ids.indexOf(g.getKey());
                Pieza tiene = ev.pieza(ids.get((j - 1 + ids.size()) % ids.size()));
                c.putInt("tiene", tiene == null ? 0 : tiene.numero);
            }
            gr.add(c);
        }
        t0.put("grilletes", gr);
        BlockPos o = zona(s);
        if (o != null) {
            CompoundTag g = new CompoundTag();
            g.putBoolean("on", gasActivo());
            g.putDouble("x", gasX(s));
            g.putDouble("vel", fase == Fase.JUEGO && t > gracia * 20L ? gasVel : 0);
            g.putDouble("fin", x(s, Cloacas.GAS_FIN));
            g.putInt("x0", o.getX() + Cloacas.X0);
            g.putInt("z0", o.getZ() + 1);
            g.putInt("z1", o.getZ() + Cloacas.Z1);
            g.putInt("y0", o.getY());
            g.putInt("y1", o.getY() + 8);
            g.putInt("gracia", (int) Math.max(0, (gracia * 20L - t) / 20));
            t0.put("gas", g);
        }
        ListTag pl = new ListTag();
        for (int k = 1; k <= 2; k++) {
            for (int j = 0; j < 2; j++) {
                BlockPos b = marca(s, "enc.placa." + k + "." + (j == 0 ? "a" : "b"));
                if (b == null) {
                    continue;
                }
                CompoundTag c = new CompoundTag();
                c.putInt("x", b.getX());
                c.putInt("y", b.getY());
                c.putInt("z", b.getZ());
                c.putBoolean("on", pisadas[k][j]);
                c.putBoolean("abierta", abierta[k]);
                pl.add(c);
            }
        }
        t0.put("placas", pl);
        ListTag sv = new ListTag();
        for (int e : salvados) {
            for (UUID id : equipos.getOrDefault(e, List.of())) {
                CompoundTag c = new CompoundTag();
                c.putUUID("id", id);
                sv.add(c);
            }
        }
        t0.put("a_salvo", sv);
        return t0;
    }

    private static void enviar(MinecraftServer s) {
        cambio = false;
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }
}
