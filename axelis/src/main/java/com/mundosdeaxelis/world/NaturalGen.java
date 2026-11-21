package com.mundosdeaxelis.world;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mundosdeaxelis.MundosDeAxelis;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Estructuras que aparecen solas en el mundo normal (etapas 255-256). Cada tipo ({@link Spec}) divide el mundo en regiones; en
 * algunas (siempre las mismas para la misma semilla) hay un sitio. Cuando alguien pasa cerca, se carga el terreno, se comprueba
 * que sirva (sin agua, no muy desigual, sin aldeas, sin que nadie viva allí, bioma adecuado y hondura suficiente) y se construye
 * poco a poco (unos milisegundos por tick). Si no sirve, se prueba otro sitio de la región (hasta 3).
 * <ul>
 * <li><b>Volcán</b>: regiones de 1024, una de cada tres más o menos, a más de 1200 bloques del punto de aparición.</li>
 * <li><b>Santuario del Bosque</b>: regiones de 640, más de la mitad; la región del punto de aparición siempre tiene uno (es la
 * primera estructura), a partir de 200 bloques; en bosques, taigas, llanuras o praderas.</li>
 * </ul>
 * Comandos: {@code /mundos natural cerca | generar <tipo> [aqui] | activar <tipo> <sí/no>}.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class NaturalGen extends SavedData {
    private static final String NAME = "mundos_naturales";
    private static final TicketType<ChunkPos> TICKET = TicketType.create("mundos_natural", Comparator.comparingLong(ChunkPos::toLong), 20 * 60 * 5);

    /** Qué hacer cuando una estructura natural queda construida (además de protegerla). */
    @FunctionalInterface
    public interface Built {
        void done(ServerLevel level, BlockPos origin, Rotation rot, StructBp bp, List<UUID> mobs);
    }

    /**
     * Un tipo de estructura natural. {@code centerZ}: z local del centro de la obra; {@code radius}: radio que ocupa (para
     * comprobar el terreno); {@code maxRange}: desnivel máximo admitido; {@code depth}: cuánto baja la obra bajo el suelo;
     * {@code ticket}: radio en trozos que se carga; {@code trigger}: a qué distancia de un jugador se empieza.
     */
    public record Spec(String id, String design, String title, int region, int chance, int minSpawn, long salt, int centerZ, int radius,
                       int maxRange, int depth, int ticket, int trigger, boolean spawnRegionAlways, Predicate<Holder<Biome>> biome, Built built) {
    }

    private static boolean land(Holder<Biome> b) {
        return !b.is(BiomeTags.IS_OCEAN) && !b.is(BiomeTags.IS_RIVER) && !b.is(BiomeTags.IS_BEACH);
    }

    public static final Spec VOLCAN = new Spec("volcan", "volcan", "Volcán", 1024, 35, 1200, 0L, DesignVolcan.CZ, DesignVolcan.RB + 16, 18, 14, 7, 320,
            false, NaturalGen::land, (l, o, r, bp, mobs) -> Volcanes.register(l, o, r, true, mobs));

    /**
     * Etapa 262: el Gran Templo del Bosque (la estructura grande del tier 1, con la Piedra de Portal: Naturaleza). Como es la
     * primera, la región del punto de aparición siempre tiene uno. Su id sigue siendo "bosque": a él apuntan las pistas y brújulas.
     */
    public static final Spec BOSQUE = new Spec("bosque", "bosque", "Gran Templo del Bosque", 896, 50, 260, 0x7E3A1C55L, 50, 64, 14, 13, 5, 300,
            true, b -> land(b) && (b.is(BiomeTags.IS_FOREST) || b.is(Biomes.PLAINS) || b.is(Biomes.MEADOW)), (l, o, r, bp, mobs) -> {
    });

    /** El Santuario del Bosque (la estructura chica del tier 1, con Ruda). También siempre en la región del punto de aparición. */
    public static final Spec SANTUARIO = new Spec("santuario", "santuario", "Santuario del Bosque", 640, 55, 200, 0x5A71C0A1L, 18, 30, 9, 4, 4, 220,
            true, b -> land(b) && (b.is(BiomeTags.IS_FOREST) || b.is(BiomeTags.IS_TAIGA) || b.is(Biomes.PLAINS) || b.is(Biomes.SUNFLOWER_PLAINS)
            || b.is(Biomes.MEADOW)), (l, o, r, bp, mobs) -> {
    });

    /**
     * Etapa 264: la Ciudadela de cristal (la estructura grande del tier del hielo, con la Piedra de Portal: Hielo en su cripta), en
     * llanuras nevadas, taigas nevadas y picos de hielo. Su id es "hielo": a ella apuntan las pistas y brújulas del hielo.
     */
    public static final Spec HIELO = new Spec("hielo", "hielo", "Ciudadela de cristal", 960, 45, 450, 0x3C17DA05L, 34, 58, 12, 14, 6, 300,
            false, b -> land(b) && (b.is(Biomes.SNOWY_PLAINS) || b.is(Biomes.SNOWY_TAIGA) || b.is(Biomes.ICE_SPIKES)),
            (l, o, r, bp, mobs) -> {
            });

    /** Etapa 258: el Fortín de Escarcha (la estructura chica del hielo, con Bruno), en llanuras y taigas nevadas, picos de hielo y arboledas. */
    public static final Spec FORTIN = new Spec("fortin", "fortin", "Fortín de Escarcha", 768, 50, 300, 0x1CE5F0A7L, 27, 34, 9, 4, 4, 220,
            false, b -> land(b) && (b.is(Biomes.SNOWY_PLAINS) || b.is(Biomes.SNOWY_TAIGA) || b.is(Biomes.ICE_SPIKES) || b.is(Biomes.GROVE)),
            (l, o, r, bp, mobs) -> {
            });

    /** Etapa 259: la Fragua Olvidada, en las tierras baldías (badlands), colinas azotadas por el viento y mesetas de sabana. */
    public static final Spec FORJA = new Spec("forja", "fragua", "Fragua Olvidada", 704, 50, 250, 0xF06A2B19L, 23, 30, 10, 4, 4, 220,
            false, b -> land(b) && (b.is(BiomeTags.IS_BADLANDS) || b.is(Biomes.WINDSWEPT_HILLS) || b.is(Biomes.WINDSWEPT_GRAVELLY_HILLS)
            || b.is(Biomes.WINDSWEPT_FOREST) || b.is(Biomes.WINDSWEPT_SAVANNA) || b.is(Biomes.SAVANNA_PLATEAU)),
            (l, o, r, bp, mobs) -> {
            });

    private static boolean magic(Holder<Biome> b) {
        return land(b) && (b.is(Biomes.DARK_FOREST) || b.is(Biomes.BIRCH_FOREST) || b.is(Biomes.OLD_GROWTH_BIRCH_FOREST)
                || b.is(Biomes.OLD_GROWTH_PINE_TAIGA) || b.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA) || b.is(Biomes.CHERRY_GROVE));
    }

    /**
     * Etapa 265: la Fortaleza del Archimago (la estructura grande de la magia: la Piedra de Portal: Torre en el observatorio y Teo en
     * la cripta). Su id es "torre": a ella apuntan las pistas y brújulas de la torre.
     */
    public static final Spec TORRE = new Spec("torre", "torre", "Fortaleza del Archimago", 1024, 40, 600, 0x2B91E6D3L, 34, 52, 10, 8, 6, 320,
            false, NaturalGen::magic, (l, o, r, bp, mobs) -> {
            });

    /** Etapa 261: la Torre Arcana (la estructura chica de la magia, con el tesoro), en los mismos biomas. */
    public static final Spec ARCANA = new Spec("arcana", "torre_arcana", "Torre Arcana", 832, 45, 350, 0x70AA3C61L, 24, 31, 10, 4, 4, 220,
            false, NaturalGen::magic, (l, o, r, bp, mobs) -> {
            });

    public static final List<Spec> SPECS = List.of(BOSQUE, SANTUARIO, VOLCAN, HIELO, FORTIN, FORJA, TORRE, ARCANA);

    static Spec byId(String id) {
        for (Spec s : SPECS) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    // ================================================================== datos guardados

    /** Por tipo: regiones vistas (0 sin sitio, 1 construido, 2 terreno no válido, 3 + k sitio k pendiente). */
    private final Map<String, Map<Long, Integer>> regions = new HashMap<>();
    private final Map<String, Boolean> enabled = new HashMap<>();
    private boolean migrated;
    /** Etapa 262: antes el id "bosque" era el santuario; ahora es el templo. Las regiones vistas pasan al id "santuario". */
    private boolean migrated262;
    /** Etapa 264: antes el id "hielo" era el fortín; ahora es la ciudadela. Las regiones vistas pasan al id "fortin". */
    private boolean migrated264;
    /** Etapa 265: antes el id "torre" era la Torre Arcana; ahora es la Fortaleza. Las regiones vistas pasan al id "arcana". */
    private boolean migrated265;

    public NaturalGen() {
    }

    public static NaturalGen get(MinecraftServer server) {
        NaturalGen g = server.overworld().getDataStorage().computeIfAbsent(NaturalGen::load, NaturalGen::new, NAME);
        if (!g.migrated) {
            // las regiones y el interruptor del volcán estaban antes en el registro de volcanes (etapa 255)
            g.migrated = true;
            Volcanes vs = Volcanes.get(server);
            Map<Long, Integer> old = vs.takeOldRegions();
            if (!old.isEmpty()) {
                g.map(VOLCAN).putAll(old);
            }
            Boolean on = vs.takeOldNatural();
            if (on != null) {
                g.enabled.put(VOLCAN.id(), on);
            }
            g.setDirty();
        }
        if (!g.migrated262) {
            g.migrated262 = true;
            Map<Long, Integer> old = g.regions.remove(BOSQUE.id());
            if (old != null && !g.regions.containsKey(SANTUARIO.id())) {
                g.regions.put(SANTUARIO.id(), old);
            }
            Boolean on = g.enabled.remove(BOSQUE.id());
            if (on != null) {
                g.enabled.put(SANTUARIO.id(), on);
            }
            g.setDirty();
        }
        if (!g.migrated264) {
            g.migrated264 = true;
            Map<Long, Integer> old = g.regions.remove(HIELO.id());
            if (old != null && !g.regions.containsKey(FORTIN.id())) {
                g.regions.put(FORTIN.id(), old);
            }
            Boolean on = g.enabled.remove(HIELO.id());
            if (on != null) {
                g.enabled.put(FORTIN.id(), on);
            }
            g.setDirty();
        }
        if (!g.migrated265) {
            g.migrated265 = true;
            Map<Long, Integer> old = g.regions.remove(TORRE.id());
            if (old != null && !g.regions.containsKey(ARCANA.id())) {
                g.regions.put(ARCANA.id(), old);
            }
            Boolean on = g.enabled.remove(TORRE.id());
            if (on != null) {
                g.enabled.put(ARCANA.id(), on);
            }
            g.setDirty();
        }
        return g;
    }

    Map<Long, Integer> map(Spec s) {
        return regions.computeIfAbsent(s.id(), k -> new HashMap<>());
    }

    public boolean enabled(Spec s) {
        return enabled.getOrDefault(s.id(), true);
    }

    public static NaturalGen load(CompoundTag tag) {
        NaturalGen g = new NaturalGen();
        g.migrated = tag.getBoolean("Migrated");
        g.migrated262 = tag.getBoolean("Migrated262");
        g.migrated264 = tag.getBoolean("Migrated264");
        g.migrated265 = tag.getBoolean("Migrated265");
        CompoundTag on = tag.getCompound("Enabled");
        for (String k : on.getAllKeys()) {
            g.enabled.put(k, on.getBoolean(k));
        }
        CompoundTag all = tag.getCompound("Regions");
        for (String id : all.getAllKeys()) {
            CompoundTag r = all.getCompound(id);
            Map<Long, Integer> m = new HashMap<>();
            for (String k : r.getAllKeys()) {
                try {
                    m.put(Long.parseLong(k), r.getInt(k));
                } catch (NumberFormatException ignored) {
                }
            }
            g.regions.put(id, m);
        }
        return g;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Migrated", migrated);
        tag.putBoolean("Migrated262", migrated262);
        tag.putBoolean("Migrated264", migrated264);
        tag.putBoolean("Migrated265", migrated265);
        CompoundTag on = new CompoundTag();
        enabled.forEach(on::putBoolean);
        tag.put("Enabled", on);
        CompoundTag all = new CompoundTag();
        for (Map.Entry<String, Map<Long, Integer>> e : regions.entrySet()) {
            CompoundTag r = new CompoundTag();
            for (Map.Entry<Long, Integer> f : e.getValue().entrySet()) {
                r.putInt(Long.toString(f.getKey()), f.getValue());
            }
            all.put(e.getKey(), r);
        }
        tag.put("Regions", all);
        return tag;
    }

    // ================================================================== sitios

    static long regionKey(int rx, int rz) {
        return ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
    }

    /** El sitio k (0..2) de una región: centro de la obra. Siempre el mismo para la misma semilla. */
    static BlockPos site(Spec s, long seed, int rx, int rz, int k) {
        java.util.Random r = new java.util.Random(seed ^ rx * 341873128712L ^ rz * 132897987541L ^ 0x5EED7011C4A0L ^ k * 0x9E3779B97F4A7C15L ^ s.salt());
        int margin = s.region() * 180 / 1024;
        int x = rx * s.region() + margin + r.nextInt(s.region() - 2 * margin);
        int z = rz * s.region() + margin + r.nextInt(s.region() - 2 * margin);
        return new BlockPos(x, 0, z);
    }

    static boolean hasSite(Spec s, long seed, int rx, int rz, BlockPos spawn) {
        if (s.spawnRegionAlways() && Math.floorDiv(spawn.getX(), s.region()) == rx && Math.floorDiv(spawn.getZ(), s.region()) == rz) {
            return true;
        }
        java.util.Random r = new java.util.Random(seed * 31 + rx * 0x2545F4914F6CDD1DL + rz * 0x9E3779B97F4A7C15L + s.salt());
        return r.nextInt(100) < s.chance();
    }

    static Rotation rotOf(BlockPos site) {
        return Rotation.values()[Math.floorMod(site.getX() * 31 + site.getZ() * 17, 4)];
    }

    /**
     * La estructura de ese tipo más cercana a {@code from} en el mundo normal: una ya construida o el sitio donde aparecerá
     * (mirando hasta 2 regiones alrededor). Null si no hay ninguna. Para las pistas y brújulas de los habitantes (etapa 257).
     */
    public static BlockPos nearest(MinecraftServer server, String id, BlockPos from) {
        Spec s = byId(id);
        if (s == null) {
            return null;
        }
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos c : StructGuard.centers(server, s.design(), net.minecraft.world.level.Level.OVERWORLD)) {
            double d = Math.hypot(c.getX() - from.getX(), c.getZ() - from.getZ());
            if (d < bd) {
                bd = d;
                best = c;
            }
        }
        NaturalGen g = get(server);
        if (!g.enabled(s)) {
            return best;
        }
        ServerLevel ow = server.overworld();
        long seed = ow.getSeed();
        BlockPos spawn = ow.getSharedSpawnPos();
        Map<Long, Integer> regs = g.map(s);
        int rx0 = Math.floorDiv(from.getX(), s.region()), rz0 = Math.floorDiv(from.getZ(), s.region());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int rx = rx0 + dx, rz = rz0 + dz;
                Integer st = regs.get(regionKey(rx, rz));
                if (st == null) {
                    st = hasSite(s, seed, rx, rz, spawn) ? 3 : 0;
                }
                if (st < 3) {
                    continue; // sin sitio, ya construida (está en StructGuard) o el terreno no servía
                }
                for (int k = st - 3; k < 3; k++) {
                    BlockPos c = site(s, seed, rx, rz, k);
                    if (Math.hypot(c.getX() - spawn.getX(), c.getZ() - spawn.getZ()) < s.minSpawn()) {
                        continue;
                    }
                    double d = Math.hypot(c.getX() - from.getX(), c.getZ() - from.getZ());
                    if (d < bd) {
                        bd = d;
                        best = new BlockPos(c.getX(), ow.getSeaLevel(), c.getZ());
                    }
                    break;
                }
            }
        }
        return best;
    }

    /** Estado de la región donde está {@code p} para un tipo, en palabras. */
    String describe(Spec s, ServerLevel ow, BlockPos p) {
        long seed = ow.getSeed();
        int rx = Math.floorDiv(p.getX(), s.region()), rz = Math.floorDiv(p.getZ(), s.region());
        Integer st = map(s).get(regionKey(rx, rz));
        if (st == null) {
            st = hasSite(s, seed, rx, rz, ow.getSharedSpawnPos()) ? 3 : 0;
        }
        return switch (st) {
            case 0 -> "sin " + s.title();
            case 1 -> "su " + s.title() + " ya está";
            case 2 -> "el terreno no servía";
            default -> {
                BlockPos b = site(s, seed, rx, rz, st - 3);
                yield s.title() + " pendiente cerca de " + b.getX() + " " + b.getZ();
            }
        };
    }

    // ================================================================== cada 5 s: decidir regiones y empezar obras

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = e.getServer();
        if (job != null) {
            job.tick(server);
            return;
        }
        if (server.getTickCount() % 100 != 13) {
            return;
        }
        NaturalGen g = get(server);
        for (Spec s : SPECS) {
            if (g.enabled(s) && g.plan(server, s)) {
                return;
            }
        }
    }

    private boolean plan(MinecraftServer server, Spec s) {
        ServerLevel ow = server.overworld();
        long seed = ow.getSeed();
        BlockPos spawn = ow.getSharedSpawnPos();
        Map<Long, Integer> regs = map(s);
        for (ServerPlayer p : ow.players()) {
            int prx = Math.floorDiv(p.getBlockX(), s.region()), prz = Math.floorDiv(p.getBlockZ(), s.region());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int rx = prx + dx, rz = prz + dz;
                    long key = regionKey(rx, rz);
                    Integer st = regs.get(key);
                    if (st == null) {
                        st = hasSite(s, seed, rx, rz, spawn) ? 3 : 0;
                        regs.put(key, st);
                        setDirty();
                    }
                    if (st < 3) {
                        continue;
                    }
                    int k = st - 3;
                    BlockPos site = site(s, seed, rx, rz, k);
                    if (Math.hypot(site.getX() - spawn.getX(), site.getZ() - spawn.getZ()) < s.minSpawn()) {
                        nextCandidate(s, key, k);
                        continue;
                    }
                    if (Math.hypot(p.getX() - site.getX(), p.getZ() - site.getZ()) < s.trigger()) {
                        job = new Job(s, ow, site, rotOf(site), key, k, null, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void nextCandidate(Spec s, long key, int k) {
        map(s).put(key, k + 1 < 3 ? 3 + k + 1 : 2);
        setDirty();
    }

    // ================================================================== la obra

    private static Job job;

    /** Al cerrar el servidor se olvida la obra a medias. */
    public static void clearRuntime() {
        job = null;
    }

    private static final class Job {
        final Spec spec;
        final ServerLevel level;
        final BlockPos center;
        final Rotation rot;
        final long region;
        final int cand;
        final CommandSourceStack src;
        final boolean skipCheck;
        final ChunkPos cpos;
        final long started = System.currentTimeMillis();
        final CompletableFuture<StructBp> bp;
        int stage = 0;
        int wait = 0;
        StructPlacer placer;
        StructBp plan;
        BlockPos origin;

        Job(Spec spec, ServerLevel level, BlockPos center, Rotation rot, long region, int cand, CommandSourceStack src, boolean skipCheck) {
            this.spec = spec;
            this.level = level;
            this.center = center;
            this.rot = rot;
            this.region = region;
            this.cand = cand;
            this.src = src;
            this.skipCheck = skipCheck;
            this.cpos = new ChunkPos(center);
            level.getChunkSource().addRegionTicket(TICKET, cpos, spec.ticket(), cpos);
            this.bp = CompletableFuture.supplyAsync(() -> StructDesigns.make(spec.design()));
            say(spec.title() + ": cargando el terreno en " + center.getX() + " " + center.getZ() + "...");
        }

        void say(String s) {
            MundosDeAxelis.LOGGER.info("[Estructuras naturales] {}", s);
            if (src != null) {
                src.sendSuccess(() -> Component.literal(s), false);
            }
        }

        void end(MinecraftServer server, boolean ok, String why) {
            level.getChunkSource().removeRegionTicket(TICKET, cpos, spec.ticket(), cpos);
            NaturalGen g = get(server);
            if (region != Long.MIN_VALUE) {
                if (ok) {
                    g.map(spec).put(region, 1);
                    g.setDirty();
                } else {
                    g.nextCandidate(spec, region, cand);
                }
            }
            String tag = spec.id().toUpperCase(java.util.Locale.ROOT) + "_NATURAL_OK";
            say(ok ? tag + ": " + spec.title() + " construido en " + center.getX() + " " + origin.getY() + " " + center.getZ()
                    + " (" + (System.currentTimeMillis() - started) / 1000 + " s)" : spec.title() + ": aquí no se puede (" + why + ")");
            job = null;
        }

        void tick(MinecraftServer server) {
            switch (stage) {
                case 0 -> {
                    boolean all = true;
                    for (int dx = -spec.ticket(); dx <= spec.ticket() && all; dx++) {
                        for (int dz = -spec.ticket(); dz <= spec.ticket() && all; dz++) {
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
                    String why = skipCheck ? null : check(level, spec, center, y);
                    if (why != null) {
                        end(server, false, why);
                        return;
                    }
                    origin = center.atY(y[0]).offset(new BlockPos(0, 0, -spec.centerZ()).rotate(rot));
                    plan = bp.join();
                    if (spec == VOLCAN) {
                        Volcanes.prime(plan);
                    }
                    placer = new StructPlacer(level, plan, origin, rot, null);
                    if (spec == VOLCAN) {
                        placer.onMob(Volcanes::tagMob);
                    }
                    stage = 2;
                    say(spec.title() + ": construyendo...");
                }
                case 2 -> {
                    if (placer.step(6_000_000L)) {
                        StructGuard.add(level, spec.design(), origin, rot, plan);
                        spec.built().done(level, origin, rot, plan, placer.mobs);
                        end(server, true, null);
                    }
                }
                default -> job = null;
            }
        }
    }

    /**
     * ¿Sirve el terreno? null si sí (y en {@code outY} la altura del suelo de la obra); si no, el motivo: agua, muy desigual, muy
     * cerca del fondo del mundo, una aldea cerca, alguien vive allí, o un bioma que no le va.
     */
    static String check(ServerLevel level, Spec s, BlockPos center, int[] outY) {
        int r = s.radius();
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
        hs.sort(Integer::compare);
        int lo = hs.get(hs.size() / 10), hi = hs.get(hs.size() * 9 / 10);
        if (hi - lo > s.maxRange()) {
            return "terreno muy desigual (" + (hi - lo) + " bloques)";
        }
        int mid = hs.get(hs.size() / 2);
        if (mid - s.depth() < level.getMinBuildHeight() + 2) {
            return "muy cerca del fondo del mundo (no cabría)";
        }
        Holder<Biome> biome = level.getBiome(center.atY(mid));
        if (!s.biome().test(biome)) {
            return "bioma que no le va";
        }
        if (StructGuard.near(level, center, r + 48)) {
            return "hay otra estructura cerca";
        }
        ChunkPos cp = new ChunkPos(center);
        int cr = Math.max(1, r / 16 + 1);
        for (int dx = -cr; dx <= cr; dx++) {
            for (int dz = -cr; dz <= cr; dz++) {
                net.minecraft.world.level.chunk.LevelChunk ch = level.getChunkSource().getChunkNow(cp.x + dx, cp.z + dz);
                if (ch != null && ch.getInhabitedTime() > 24000L) {
                    return "alguien vive cerca";
                }
            }
        }
        outY[0] = mid + 1;
        return null;
    }

    // ================================================================== comandos

    /**
     * Construye una estructura "natural" (poco a poco, como las que aparecen solas): sin {@code aqui}, a unos 230 bloques hacia
     * donde se mira y con las comprobaciones del terreno; con {@code aqui}, con el centro donde se está y sin comprobar nada.
     */
    public static int generate(CommandSourceStack src, Spec s, boolean here) {
        if (job != null) {
            src.sendFailure(Component.literal("Ya hay una estructura construyéndose."));
            return 0;
        }
        BlockPos center;
        if (here) {
            center = BlockPos.containing(src.getPosition());
        } else {
            net.minecraft.world.phys.Vec3 look = net.minecraft.world.phys.Vec3.directionFromRotation(0, src.getRotation().y);
            BlockPos c = BlockPos.containing(src.getPosition().add(look.scale(230)));
            center = new BlockPos(c.getX(), 0, c.getZ());
        }
        job = new Job(s, src.getLevel(), center, rotOf(center), Long.MIN_VALUE, 0, src, here);
        return 1;
    }

    public static int setEnabled(CommandSourceStack src, Spec s, boolean on) {
        NaturalGen g = get(src.getServer());
        g.enabled.put(s.id(), on);
        g.setDirty();
        src.sendSuccess(() -> Component.literal(on ? s.title() + ": aparecerá solo en el mundo." : s.title() + ": ya no aparecerá solo (lo que ya hay se queda)."), true);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        LiteralArgumentBuilder<CommandSourceStack> gen = Commands.literal("generar");
        LiteralArgumentBuilder<CommandSourceStack> act = Commands.literal("activar");
        for (Spec s : SPECS) {
            gen.then(Commands.literal(s.id()).executes(c -> generate(c.getSource(), s, false))
                    .then(Commands.literal("aqui").executes(c -> generate(c.getSource(), s, true))));
            act.then(Commands.literal(s.id()).then(Commands.argument("activo", BoolArgumentType.bool())
                    .executes(c -> setEnabled(c.getSource(), s, BoolArgumentType.getBool(c, "activo")))));
        }
        return Commands.literal("natural")
                .then(Commands.literal("cerca").executes(c -> near(c.getSource())))
                .then(gen)
                .then(act);
    }

    private static int near(CommandSourceStack src) {
        ServerLevel ow = src.getServer().overworld();
        NaturalGen g = get(src.getServer());
        BlockPos p = BlockPos.containing(src.getPosition());
        StringBuilder sb = new StringBuilder();
        for (Spec s : SPECS) {
            sb.append(s.title()).append(g.enabled(s) ? "" : " (desactivado)").append(": ").append(g.describe(s, ow, p)).append(". ");
        }
        String msg = sb.toString().trim();
        src.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }
}
