package com.mundosdeaxelis.world;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mundosdeaxelis.MundosDeAxelis;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registro de los volcanes del mundo (etapa 255), guardado con el mundo:
 * <ul>
 * <li><b>Reaparecer para volver a jugar</b>: si nadie se acerca a un volcán en 10 minutos, la próxima vez que alguien llegue
 * vuelven sus enemigos (y la Gran Salamandra), se cierran sus rejas y se apagan los braseros; los cofres se rellenan como mucho
 * una vez por hora (cada jugador, su botín nuevo).</li>
 * <li><b>Música</b>: dentro de la dungeon (bajo la ladera) suena la música del volcán; fuera, la normal.</li>
 * <li><b>Volcanes que aparecen solos</b>: el mundo normal se divide en regiones de 1024 bloques; en algunas (más o menos una de
 * cada tres, siempre las mismas para la misma semilla) hay un volcán, lejos del punto de aparición. Se construye poco a poco cuando
 * alguien pasa a unos 300 bloques, si el terreno sirve (sin agua, no muy desigual, sin aldeas y sin que nadie viva allí).</li>
 * </ul>
 * Comandos: {@code /mundos volcan cerca | reiniciar | generar | natural <sí/no>}.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Volcanes extends SavedData {
    private static final String NAME = "mundos_volcanes";
    /** Marcas en los enemigos: de qué volcán son y su número en la lista del plano. */
    static final String TAG = "MundosVolcan";
    static final String TAG_I = "MundosVolcanI";
    static final int REGION = 1024;
    static final int MIN_SPAWN_DIST = 1200;
    static final long ABSENT = 12000L;
    static final long LOOT_EVERY = 72000L;
    /** Lejos y cerca para la vuelta de los enemigos (en bloques desde el centro del volcán). */
    static final double FAR = 160, NEAR = 64;
    static final int BUILD_TRIGGER = 320;
    private static final int TICKET_RADIUS = 7;
    private static final TicketType<ChunkPos> TICKET = TicketType.create("mundos_volcan", Comparator.comparingLong(ChunkPos::toLong), 20 * 60 * 5);

    static final class V {
        int id;
        ResourceKey<Level> dim;
        BlockPos origin;
        Rotation rot;
        boolean natural;
        boolean present = true;
        long absentSince;
        long lastLoot;

        BlockPos center() {
            return StructPlacer.world(origin, rot, 0, 0, DesignVolcan.CZ);
        }

        BlockPos world(int x, int y, int z) {
            return StructPlacer.world(origin, rot, x, y, z);
        }

        /** Del mundo a coordenadas del plano. */
        BlockPos local(BlockPos p) {
            BlockPos d = p.subtract(origin);
            Rotation inv = switch (rot) {
                case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
                case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
                default -> rot;
            };
            return d.rotate(inv);
        }
    }

    private final List<V> list = new ArrayList<>();
    /** Regiones ya vistas: -1/ausente sin decidir, 0 sin volcán, 1 construido, 2 terreno no válido, 3 + k sitio k pendiente. */
    private final Map<Long, Integer> regions = new HashMap<>();
    private int nextId = 1;
    private boolean natural = true;

    public Volcanes() {
    }

    public static Volcanes get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Volcanes::load, Volcanes::new, NAME);
    }

    // ================================================================== datos del plano (enemigos, rejas, braseros, cofres)

    /** Lo que hace falta del plano para reiniciar un volcán, sin guardar el plano entero. */
    record Info(List<StructBp.Mob> mobs, List<Long> gates, List<Long> braziers, List<Long> chests) {
        static Info of(StructBp bp) {
            List<Long> g = new ArrayList<>();
            List<Long> br = new ArrayList<>();
            List<Long> ch = new ArrayList<>();
            for (Map.Entry<Long, String> e : bp.m.entrySet()) {
                String s = e.getValue();
                if (s.startsWith("mundos_de_axelis:reja_volcan")) {
                    g.add(e.getKey());
                } else if (s.startsWith("mundos_de_axelis:brasero_volcan")) {
                    br.add(e.getKey());
                } else if (s.startsWith("mundos_de_axelis:vault_chest")) {
                    ch.add(e.getKey());
                }
            }
            return new Info(List.copyOf(bp.mobs), g, br, ch);
        }
    }

    private static volatile Info info;
    private static CompletableFuture<Info> infoJob;

    /** Cuando ya se tiene el plano a mano (al construir), sus datos quedan listos sin esperar. */
    static void prime(StructBp bp) {
        if (info == null) {
            info = Info.of(bp);
        }
    }

    /** Los datos del plano, si ya están (se calculan aparte la primera vez para no trabar el juego). */
    static Info info() {
        if (info != null) {
            return info;
        }
        if (infoJob == null) {
            infoJob = CompletableFuture.supplyAsync(() -> Info.of(StructDesigns.make("volcan")));
        }
        if (infoJob.isDone()) {
            try {
                info = infoJob.join();
            } catch (Exception ex) {
                MundosDeAxelis.LOGGER.error("No se pudo preparar el plano del volcán", ex);
                infoJob = null;
            }
        }
        return info;
    }

    // ================================================================== alta y baja

    /** Marca (antes de entrar al mundo) cada enemigo del volcán con su número en la lista del plano. */
    public static void tagMob(int index, Mob mob) {
        mob.getPersistentData().putInt(TAG_I, index);
    }

    /** Apunta un volcán recién construido (y marca sus enemigos con su número). Devuelve su número. */
    public static int register(ServerLevel level, BlockPos origin, Rotation rot, boolean natural, List<UUID> mobs) {
        Volcanes vs = get(level.getServer());
        V v = new V();
        v.id = vs.nextId++;
        v.dim = level.dimension();
        v.origin = origin.immutable();
        v.rot = rot;
        v.natural = natural;
        v.present = true;
        v.lastLoot = level.getGameTime();
        vs.list.add(v);
        for (UUID id : mobs) {
            if (id != null && level.getEntity(id) instanceof Mob m) {
                m.getPersistentData().putInt(TAG, v.id);
            }
        }
        vs.setDirty();
        return v.id;
    }

    /** Lo quita del registro, junto con los enemigos que tenga (también los que volvieron después). */
    public static void unregister(MinecraftServer server, int id) {
        Volcanes vs = get(server);
        for (V v : vs.list) {
            ServerLevel level = v.id == id ? server.getLevel(v.dim) : null;
            if (level != null) {
                for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(v.center()).inflate(100, 120, 100),
                        m -> m.getPersistentData().getInt(TAG) == id)) {
                    m.discard();
                }
            }
        }
        if (vs.list.removeIf(v -> v.id == id)) {
            vs.setDirty();
        }
    }

    // ================================================================== guardar

    public static Volcanes load(CompoundTag tag) {
        Volcanes vs = new Volcanes();
        vs.nextId = Math.max(1, tag.getInt("NextId"));
        vs.natural = !tag.contains("Natural") || tag.getBoolean("Natural");
        for (Tag t : tag.getList("Volcanes", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            V v = new V();
            v.id = c.getInt("Id");
            v.dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(c.getString("Dim")));
            v.origin = BlockPos.of(c.getLong("Origin"));
            v.rot = Rotation.values()[Mth.clamp(c.getInt("Rot"), 0, 3)];
            v.natural = c.getBoolean("Natural");
            v.present = c.getBoolean("Present");
            v.absentSince = c.getLong("AbsentSince");
            v.lastLoot = c.getLong("LastLoot");
            vs.list.add(v);
        }
        CompoundTag r = tag.getCompound("Regions");
        for (String k : r.getAllKeys()) {
            try {
                vs.regions.put(Long.parseLong(k), r.getInt(k));
            } catch (NumberFormatException ignored) {
            }
        }
        return vs;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("NextId", nextId);
        tag.putBoolean("Natural", natural);
        ListTag l = new ListTag();
        for (V v : list) {
            CompoundTag c = new CompoundTag();
            c.putInt("Id", v.id);
            c.putString("Dim", v.dim.location().toString());
            c.putLong("Origin", v.origin.asLong());
            c.putInt("Rot", v.rot.ordinal());
            c.putBoolean("Natural", v.natural);
            c.putBoolean("Present", v.present);
            c.putLong("AbsentSince", v.absentSince);
            c.putLong("LastLoot", v.lastLoot);
            l.add(c);
        }
        tag.put("Volcanes", l);
        CompoundTag r = new CompoundTag();
        for (Map.Entry<Long, Integer> e : regions.entrySet()) {
            r.putInt(Long.toString(e.getKey()), e.getValue());
        }
        tag.put("Regions", r);
        return tag;
    }

    // ================================================================== cada segundo: música, vuelta de enemigos, volcanes nuevos

    private static final Map<UUID, Integer> ZONE = new HashMap<>();

    /** Al cerrar el servidor: se olvida lo que estaba en marcha (la música de cada jugador, la obra a medias). */
    public static void clearRuntime() {
        ZONE.clear();
        job = null;
        info = null;
        infoJob = null;
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        ZONE.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = e.getServer();
        if (job != null) {
            job.tick(server);
        }
        int t = server.getTickCount();
        if (t % 20 != 7) {
            return;
        }
        Volcanes vs = get(server);
        vs.music(server);
        if (t % 40 == 7) {
            for (V v : new ArrayList<>(vs.list)) {
                ServerLevel level = server.getLevel(v.dim);
                if (level != null) {
                    vs.visit(level, v);
                }
            }
        }
        if (t % 100 == 7 && vs.natural && job == null) {
            vs.plan(server);
        }
    }

    /** Avisa a cada jugador cuando entra o sale de la dungeon de un volcán (para la música). */
    private void music(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            int zone = 0;
            for (V v : list) {
                if (v.dim != p.level().dimension() || v.center().distSqr(p.blockPosition()) > 130 * 130) {
                    continue;
                }
                BlockPos l = v.local(p.blockPosition());
                if (DesignVolcan.inside(l.getX(), l.getY(), l.getZ())) {
                    zone = 1;
                    break;
                }
            }
            Integer was = ZONE.get(p.getUUID());
            if (was == null || was != zone) {
                ZONE.put(p.getUUID(), zone);
                com.mundosdeaxelis.network.ModNetwork.sendTo(p, new com.mundosdeaxelis.network.VolcanZonaPacket(zone));
            }
        }
    }

    /** Si nadie se acercó en 10 minutos, al llegar alguien el volcán se reinicia. */
    private void visit(ServerLevel level, V v) {
        BlockPos c = v.center();
        boolean far = false;
        boolean near = false;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) {
                continue;
            }
            double d2 = p.blockPosition().distSqr(c);
            far |= d2 < FAR * FAR;
            near |= d2 < NEAR * NEAR;
        }
        long now = level.getGameTime();
        if (!far) {
            if (v.present) {
                v.present = false;
                v.absentSince = now;
                setDirty();
            }
            return;
        }
        if (near && !v.present) {
            v.present = true;
            setDirty();
            if (now - v.absentSince >= ABSENT) {
                reset(level, v, false);
            }
        }
    }

    /** Vuelven los enemigos que faltan, se cierran las rejas, se apagan los braseros y (si toca) se rellenan los cofres. */
    int reset(ServerLevel level, V v, boolean forceLoot) {
        Info in = info();
        if (in == null) {
            v.present = false; // todavía se está preparando el plano: se reintentará en la próxima vuelta
            return -1;
        }
        long now = level.getGameTime();
        // quiénes siguen vivos
        BlockPos c = v.center();
        Set<Integer> alive = new HashSet<>();
        AABB box = new AABB(c).inflate(80, 90, 80);
        for (Mob m : level.getEntitiesOfClass(Mob.class, box, m -> m.getPersistentData().getInt(TAG) == v.id)) {
            if (m.isAlive()) {
                alive.add(m.getPersistentData().getInt(TAG_I));
                m.setHealth(m.getMaxHealth());
            }
        }
        int back = 0;
        for (int i = 0; i < in.mobs().size(); i++) {
            if (alive.contains(i)) {
                continue;
            }
            StructBp.Mob m = in.mobs().get(i);
            BlockPos p = v.world(m.x(), m.y(), m.z());
            if (!level.isLoaded(p) || !level.areEntitiesLoaded(ChunkPos.asLong(p))) {
                continue;
            }
            final int idx = i;
            UUID id = StructPlacer.spawn(level, m, p, i, (k, mob) -> {
                mob.getPersistentData().putInt(TAG, v.id);
                mob.getPersistentData().putInt(TAG_I, idx);
            });
            if (id != null) {
                back++;
            }
        }
        // rejas cerradas y braseros apagados
        for (long k : in.gates()) {
            BlockPos p = v.world(StructBp.kx(k), StructBp.ky(k), StructBp.kz(k));
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof com.mundosdeaxelis.block.RejaVolcanBlock && s.getValue(com.mundosdeaxelis.block.RejaVolcanBlock.OPEN)) {
                level.setBlock(p, s.setValue(com.mundosdeaxelis.block.RejaVolcanBlock.OPEN, false), net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
        }
        for (long k : in.braziers()) {
            com.mundosdeaxelis.block.BraseroVolcanBlock.reset(level, v.world(StructBp.kx(k), StructBp.ky(k), StructBp.kz(k)));
        }
        boolean loot = forceLoot || now - v.lastLoot >= LOOT_EVERY;
        if (loot) {
            for (long k : in.chests()) {
                BlockPos p = v.world(StructBp.kx(k), StructBp.ky(k), StructBp.kz(k));
                if (level.getBlockEntity(p) instanceof com.mundosdeaxelis.block.VaultChestBlockEntity be) {
                    be.resetLoot();
                }
            }
            v.lastLoot = now;
        }
        setDirty();
        if (back > 0) {
            for (ServerPlayer p : level.players()) {
                if (p.blockPosition().distSqr(c) < FAR * FAR) {
                    p.displayClientMessage(Component.translatable(loot ? "message.mundos_de_axelis.volcan.reset_loot" : "message.mundos_de_axelis.volcan.reset"), true);
                }
            }
            level.playSound(null, c, com.mundosdeaxelis.registry.ModSounds.VOLCAN_RETUMBO.get(), net.minecraft.sounds.SoundSource.AMBIENT, 3.0F, 0.7F);
        }
        return back;
    }

    // ================================================================== volcanes que aparecen solos

    private static long regionKey(int rx, int rz) {
        return ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
    }

    /** El sitio k (0..2) de una región: centro del volcán y giro. Siempre el mismo para la misma semilla. */
    static BlockPos site(long seed, int rx, int rz, int k) {
        java.util.Random r = new java.util.Random(seed ^ rx * 341873128712L ^ rz * 132897987541L ^ 0x5EED7011C4A0L ^ k * 0x9E3779B97F4A7C15L);
        int x = rx * REGION + 180 + r.nextInt(REGION - 360);
        int z = rz * REGION + 180 + r.nextInt(REGION - 360);
        return new BlockPos(x, 0, z);
    }

    private static boolean hasVolcano(long seed, int rx, int rz) {
        java.util.Random r = new java.util.Random(seed * 31 + rx * 0x2545F4914F6CDD1DL + rz * 0x9E3779B97F4A7C15L);
        return r.nextInt(100) < 35;
    }

    private static Rotation rotOf(BlockPos site) {
        return Rotation.values()[Math.floorMod(site.getX() * 31 + site.getZ() * 17, 4)];
    }

    /** Mira las regiones alrededor de cada jugador: decide si tienen volcán y, si alguien pasa cerca, lo empieza a construir. */
    private void plan(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        long seed = ow.getSeed();
        BlockPos spawn = ow.getSharedSpawnPos();
        for (ServerPlayer p : ow.players()) {
            int prx = Math.floorDiv(p.getBlockX(), REGION), prz = Math.floorDiv(p.getBlockZ(), REGION);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int rx = prx + dx, rz = prz + dz;
                    long key = regionKey(rx, rz);
                    Integer st = regions.get(key);
                    if (st == null) {
                        st = hasVolcano(seed, rx, rz) ? 3 : 0;
                        regions.put(key, st);
                        setDirty();
                    }
                    if (st < 3) {
                        continue;
                    }
                    int k = st - 3;
                    BlockPos s = site(seed, rx, rz, k);
                    if (Math.hypot(s.getX() - spawn.getX(), s.getZ() - spawn.getZ()) < MIN_SPAWN_DIST) {
                        nextCandidate(key, k);
                        continue;
                    }
                    double d = Math.hypot(p.getX() - s.getX(), p.getZ() - s.getZ());
                    if (d < BUILD_TRIGGER) {
                        job = new Job(ow, s, rotOf(s), key, k, null);
                        return;
                    }
                }
            }
        }
    }

    private void nextCandidate(long key, int k) {
        regions.put(key, k + 1 < 3 ? 3 + k + 1 : 2);
        setDirty();
    }

    /** Obra en marcha (una sola a la vez): cargar el terreno, comprobarlo y construir unos milisegundos por tick. */
    private static Job job;

    private static final class Job {
        final ServerLevel level;
        final BlockPos center;
        final Rotation rot;
        final long region;
        final int cand;
        final CommandSourceStack src;
        final ChunkPos cpos;
        int stage = 0;
        int wait = 0;
        CompletableFuture<StructBp> bp;
        StructPlacer placer;
        BlockPos origin;
        long started = System.currentTimeMillis();
        /** Sin comprobar el terreno: el suelo del volcán a la altura de {@link #center} (prueba). */
        boolean skipCheck;

        Job(ServerLevel level, BlockPos center, Rotation rot, long region, int cand, CommandSourceStack src) {
            this.level = level;
            this.center = center;
            this.rot = rot;
            this.region = region;
            this.cand = cand;
            this.src = src;
            this.cpos = new ChunkPos(center);
            level.getChunkSource().addRegionTicket(TICKET, cpos, TICKET_RADIUS, cpos);
            this.bp = CompletableFuture.supplyAsync(() -> StructDesigns.make("volcan"));
            say("Volcán: cargando el terreno en " + center.getX() + " " + center.getZ() + "...");
        }

        void say(String s) {
            MundosDeAxelis.LOGGER.info("[Volcanes] {}", s);
            if (src != null) {
                src.sendSuccess(() -> Component.literal(s), false);
            }
        }

        void end(MinecraftServer server, boolean ok, String why) {
            level.getChunkSource().removeRegionTicket(TICKET, cpos, TICKET_RADIUS, cpos);
            Volcanes vs = get(server);
            if (region != Long.MIN_VALUE) {
                if (ok) {
                    vs.regions.put(region, 1);
                    vs.setDirty();
                } else {
                    vs.nextCandidate(region, cand);
                }
            }
            say(ok ? "VOLCAN_NATURAL_OK: volcán construido en " + center.getX() + " " + origin.getY() + " " + center.getZ()
                    + " (" + (System.currentTimeMillis() - started) / 1000 + " s)" : "Volcán: aquí no se puede (" + why + ")");
            job = null;
        }

        void tick(MinecraftServer server) {
            switch (stage) {
                case 0 -> {
                    // esperar a que estén cargados todos los trozos del terreno
                    boolean all = true;
                    for (int dx = -TICKET_RADIUS; dx <= TICKET_RADIUS && all; dx++) {
                        for (int dz = -TICKET_RADIUS; dz <= TICKET_RADIUS && all; dz++) {
                            all = level.getChunkSource().hasChunk(cpos.x + dx, cpos.z + dz);
                        }
                    }
                    if (all && bp.isDone()) {
                        stage = 1;
                    } else if (++wait > 20 * 90) {
                        end(server, false, "el terreno tardó demasiado en cargar");
                    }
                }
                case 1 -> {
                    int[] y = {center.getY()};
                    String why = skipCheck ? null : check(level, center, y);
                    if (why != null) {
                        end(server, false, why);
                        return;
                    }
                    origin = center.atY(y[0]).offset(new BlockPos(0, 0, -DesignVolcan.CZ).rotate(rot));
                    StructBp plan = bp.join();
                    prime(plan);
                    placer = new StructPlacer(level, plan, origin, rot, null).onMob(Volcanes::tagMob);
                    stage = 2;
                    say("Volcán: construyendo...");
                }
                case 2 -> {
                    if (placer.step(6_000_000L)) {
                        register(level, origin, rot, true, placer.mobs);
                        end(server, true, null);
                    }
                }
                default -> job = null;
            }
        }
    }

    /**
     * ¿Sirve el terreno? null si sí (y en {@code outY} la altura del suelo del volcán); si no, el motivo: agua, muy desigual,
     * una aldea cerca, alguien vive allí, o bioma de mar, río o playa.
     */
    static String check(ServerLevel level, BlockPos center, int[] outY) {
        int r = DesignVolcan.RB + 16;
        List<Integer> hs = new ArrayList<>();
        int water = 0;
        for (int i = -4; i <= 4; i++) {
            for (int j = -4; j <= 4; j++) {
                int x = center.getX() + i * r / 4, z = center.getZ() + j * r / 4;
                if ((double) i * i + j * j > 18) {
                    continue;
                }
                int g = StructPlacer.groundY(level, x, z);
                hs.add(g);
                if (!level.getFluidState(new BlockPos(x, g + 1, z)).isEmpty()) {
                    water++;
                }
                if (level.structureManager().getStructureWithPieceAt(new BlockPos(x, g, z), StructureTags.VILLAGE).isValid()) {
                    return "hay una aldea";
                }
            }
        }
        if (water > 5) {
            return "hay agua";
        }
        if (hs.get(hs.size() / 2) - 14 < level.getMinBuildHeight() + 2) {
            return "muy cerca del fondo del mundo (la dungeon no cabría)";
        }
        hs.sort(Integer::compare);
        int lo = hs.get(hs.size() / 10), hi = hs.get(hs.size() * 9 / 10);
        if (hi - lo > 18) {
            return "terreno muy desigual (" + (hi - lo) + " bloques)";
        }
        var biome = level.getBiome(center.atY(hs.get(hs.size() / 2)));
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH)) {
            return "mar, río o playa";
        }
        ChunkPos cp = new ChunkPos(center);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                net.minecraft.world.level.chunk.LevelChunk ch = level.getChunkSource().getChunkNow(cp.x + dx, cp.z + dz);
                if (ch != null && ch.getInhabitedTime() > 24000L) {
                    return "alguien vive cerca";
                }
            }
        }
        outY[0] = hs.get(hs.size() / 2) + 1;
        return null;
    }

    // ================================================================== comandos

    private static V nearest(ServerLevel level, BlockPos p) {
        V best = null;
        double bd = Double.MAX_VALUE;
        for (V v : get(level.getServer()).list) {
            if (v.dim != level.dimension()) {
                continue;
            }
            double d = v.center().distSqr(p);
            if (d < bd) {
                bd = d;
                best = v;
            }
        }
        return best;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("volcan")
                .then(Commands.literal("cerca").executes(c -> near(c.getSource())))
                .then(Commands.literal("reiniciar").executes(c -> forceReset(c.getSource())))
                .then(Commands.literal("generar").executes(c -> generate(c.getSource(), false))
                        .then(Commands.literal("aqui").executes(c -> generate(c.getSource(), true))))
                .then(Commands.literal("natural").then(Commands.argument("activo", BoolArgumentType.bool())
                        .executes(c -> setNatural(c.getSource(), BoolArgumentType.getBool(c, "activo")))));
    }

    private static int near(CommandSourceStack src) {
        BlockPos p = BlockPos.containing(src.getPosition());
        Volcanes vs = get(src.getServer());
        StringBuilder sb = new StringBuilder("Volcanes apuntados: " + vs.list.size() + (vs.natural ? " (aparecen solos)" : " (no aparecen solos)"));
        V v = nearest(src.getLevel(), p);
        if (v != null) {
            BlockPos c = v.center();
            sb.append(". El más cercano: ").append(c.getX()).append(" ").append(v.origin.getY()).append(" ").append(c.getZ())
                    .append(" (a ").append((int) Math.sqrt(c.distSqr(p))).append(" bloques").append(v.natural ? ", natural" : ", de comando").append(")");
        }
        if (src.getLevel().dimension() == Level.OVERWORLD) {
            long seed = src.getLevel().getSeed();
            int rx = Math.floorDiv(p.getX(), REGION), rz = Math.floorDiv(p.getZ(), REGION);
            Integer st = vs.regions.get(regionKey(rx, rz));
            if (st == null) {
                st = hasVolcano(seed, rx, rz) ? 3 : 0;
            }
            sb.append(". Esta región: ").append(switch (st) {
                case 0 -> "sin volcán";
                case 1 -> "su volcán ya está";
                case 2 -> "el terreno no servía";
                default -> {
                    BlockPos s = site(seed, rx, rz, st - 3);
                    yield "volcán pendiente cerca de " + s.getX() + " " + s.getZ();
                }
            });
        }
        String msg = sb.toString();
        src.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int forceReset(CommandSourceStack src) {
        V v = nearest(src.getLevel(), BlockPos.containing(src.getPosition()));
        if (v == null) {
            src.sendFailure(Component.literal("No hay ningún volcán en esta dimensión."));
            return 0;
        }
        int back = get(src.getServer()).reset(src.getLevel(), v, true);
        if (back < 0) {
            src.sendFailure(Component.literal("El plano del volcán aún se está preparando: prueba otra vez en unos segundos."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("PRUEBA_VOLCAN_REINICIO: volvieron " + back + " enemigos, rejas cerradas, braseros apagados y cofres rellenos."), true);
        return 1;
    }

    /**
     * Construye un volcán "natural" (poco a poco, como los que aparecen solos): sin {@code aqui}, a unos 230 bloques hacia donde se
     * mira y con las mismas comprobaciones del terreno; con {@code aqui}, con el centro donde se está y sin comprobar nada (pruebas).
     */
    private static int generate(CommandSourceStack src, boolean here) {
        if (job != null) {
            src.sendFailure(Component.literal("Ya hay un volcán construyéndose."));
            return 0;
        }
        ServerLevel level = src.getLevel();
        BlockPos center;
        if (here) {
            center = BlockPos.containing(src.getPosition());
        } else {
            net.minecraft.world.phys.Vec3 look = net.minecraft.world.phys.Vec3.directionFromRotation(0, src.getRotation().y);
            BlockPos c = BlockPos.containing(src.getPosition().add(look.scale(230)));
            center = new BlockPos(c.getX(), 0, c.getZ());
        }
        job = new Job(level, center, rotOf(center), Long.MIN_VALUE, 0, src);
        job.skipCheck = here;
        return 1;
    }

    private static int setNatural(CommandSourceStack src, boolean on) {
        Volcanes vs = get(src.getServer());
        vs.natural = on;
        vs.setDirty();
        src.sendSuccess(() -> Component.literal(on ? "Los volcanes aparecerán solos en el mundo." : "Los volcanes ya no aparecerán solos (los que ya hay se quedan)."), true);
        return 1;
    }
}
