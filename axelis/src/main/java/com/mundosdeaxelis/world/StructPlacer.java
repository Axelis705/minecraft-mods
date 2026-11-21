package com.mundosdeaxelis.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Coloca un {@link StructBp} en el mundo (etapa 255). En orden:
 * <ol>
 * <li>cimientos rectangulares (diseños antiguos);</li>
 * <li>mezcla con el terreno ({@link StructBp.Blend}): rampas suaves hasta el terreno de alrededor, se quitan los árboles que
 * estorban y los de la zona de ceniza quedan quemados (tronco oscuro, sin hojas);</li>
 * <li>el plano, de abajo hacia arriba (para que lo que cuelga o se apoya encuentre su apoyo);</li>
 * <li>lo que se apoya sobre la superficie real ({@link StructBp.Anchor});</li>
 * <li>escaleras, vallas, muros y barrotes conectados; lo que no se sostiene se quita sin soltar objetos;</li>
 * <li>los datos de los cofres ({@link StructBp#nbt}) y los enemigos.</li>
 * </ol>
 * Puede hacerse de golpe ({@link #runAll()}, el comando) o unos milisegundos por tick ({@link #step(long)}, cuando el volcán
 * aparece solo en el mundo y no debe trabar el juego).
 */
public final class StructPlacer {
    /** Lo que había antes en una posición (para deshacer). */
    public record Snap(BlockState old, CompoundTag be) {
    }

    /** Marca de los enemigos del plano (ver {@link StructBp.Mob#tag}). */
    public static final String TAG = "MundosTag";

    /** Solo avisa a los jugadores: sin avisar a los vecinos y sin soltar objetos (ver {@link Estructuras}). */
    static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final int P_FOUND = 0, P_BLEND = 1, P_BLOCKS = 2, P_ANCHORS = 3, P_RESHAPE = 4, P_SUPPORT = 5, P_NBT = 6, P_MOBS = 7, P_DONE = 8;
    private static final int TREE_LIMIT = 900;

    private final ServerLevel level;
    private final StructBp bp;
    private final BlockPos origin;
    private final Rotation rot;
    private final Map<BlockPos, Snap> snaps;
    private final int minY;
    private final int maxY;
    /** Altura del mundo donde queda la superficie de la estructura (la capa y = -1 del plano). */
    private final int oy;
    private BiConsumer<Integer, net.minecraft.world.entity.Mob> onMob = (i, m) -> {
    };

    private int phase = P_FOUND;
    private int idx = 0;
    private int sub = 0;
    private List<Long> blendKeys;
    private List<Map.Entry<Long, String>> entries;
    private final Map<Long, Integer> surface = new HashMap<>();
    private final List<BlockPos> placed = new ArrayList<>();
    private final List<BlockPos> reshape = new ArrayList<>();
    private final Set<BlockPos> treeSeen = new HashSet<>();
    private int supportPass = 0;
    private int supportChanged = 0;
    private final Map<String, Integer> unsupported = new java.util.TreeMap<>();
    /** Bloques quitados por no poder sostenerse. */
    public int removed = 0;
    /** Árboles quitados y árboles quemados (para el informe). */
    public int treesCut = 0;
    public int treesCharred = 0;
    /** Enemigos puestos (en el orden de {@link StructBp#mobs}; null si ese no se pudo poner). */
    public final List<UUID> mobs = new ArrayList<>();

    public StructPlacer(ServerLevel level, StructBp bp, BlockPos origin, Rotation rot, Map<BlockPos, Snap> snaps) {
        this.level = level;
        this.bp = bp;
        this.origin = origin;
        this.rot = rot;
        this.snaps = snaps;
        this.minY = level.getMinBuildHeight();
        this.maxY = level.getMaxBuildHeight();
        this.oy = origin.getY() - 1;
    }

    /** Se llama con cada enemigo antes de añadirlo al mundo (índice en {@link StructBp#mobs}). */
    public StructPlacer onMob(BiConsumer<Integer, net.minecraft.world.entity.Mob> f) {
        this.onMob = f;
        return this;
    }

    public static BlockPos world(BlockPos origin, Rotation rot, int x, int y, int z) {
        return origin.offset(new BlockPos(x, y, z).rotate(rot));
    }

    /** El bloque que pide el plano en esa posición, ya girado (null si el texto no es un bloque conocido). */
    public static net.minecraft.world.level.block.state.BlockState planned(String spec, Rotation rot) {
        net.minecraft.world.level.block.state.BlockState st = Estructuras.state(spec);
        return st == null ? null : st.rotate(rot);
    }

    public BlockPos world(int x, int y, int z) {
        return world(origin, rot, x, y, z);
    }

    public boolean done() {
        return phase == P_DONE;
    }

    /** Todo de una vez. */
    public void runAll() {
        while (!step(Long.MAX_VALUE)) {
            // nada
        }
    }

    /** Avanza hasta gastar {@code budgetNanos}; devuelve true al terminar. */
    public boolean step(long budgetNanos) {
        long end = budgetNanos == Long.MAX_VALUE ? Long.MAX_VALUE : System.nanoTime() + budgetNanos;
        int n = 0;
        while (phase != P_DONE) {
            if ((++n & 63) == 0 && System.nanoTime() > end) {
                return false;
            }
            switch (phase) {
                case P_FOUND -> stepFound();
                case P_BLEND -> stepBlend();
                case P_BLOCKS -> stepBlock();
                case P_ANCHORS -> stepAnchor();
                case P_RESHAPE -> stepReshape();
                case P_SUPPORT -> stepSupport();
                case P_NBT -> stepNbt();
                case P_MOBS -> stepMobs();
                default -> phase = P_DONE;
            }
        }
        if (!unsupported.isEmpty()) {
            com.mojang.logging.LogUtils.getLogger().info("[Estructuras] sin apoyo quitados: {}", unsupported);
        }
        return true;
    }

    private void next() {
        phase++;
        idx = 0;
        sub = 0;
    }

    // ------------------------------------------------------------------ cimientos (rectángulos de los diseños antiguos)

    private void stepFound() {
        if (idx >= bp.found.size()) {
            next();
            return;
        }
        StructBp.Found f = bp.found.get(idx++);
        BlockState fs = Estructuras.state(f.state());
        if (fs == null) {
            return;
        }
        for (int x = f.x0(); x <= f.x1(); x++) {
            for (int z = f.z0(); z <= f.z1(); z++) {
                BlockPos base = world(x, 0, z);
                for (int y = origin.getY() - 1; y > origin.getY() - 1 - 24 && y >= minY; y--) {
                    BlockPos p = new BlockPos(base.getX(), y, base.getZ());
                    BlockState cur = level.getBlockState(p);
                    boolean soft = cur.isAir() || !cur.getFluidState().isEmpty() || cur.canBeReplaced() || cur.is(BlockTags.LEAVES) || cur.is(BlockTags.LOGS);
                    if (!soft) {
                        break;
                    }
                    put(p, fs.rotate(rot));
                }
            }
        }
    }

    // ------------------------------------------------------------------ mezcla con el terreno

    /** Lo que no es "suelo": aire, plantas, troncos, hojas, nieve, setas gigantes, cactus, bambú, caña... */
    static boolean vegetation(BlockState s) {
        if (s.isAir() || s.canBeReplaced() || s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES)) {
            return true;
        }
        return s.is(Blocks.CACTUS) || s.is(Blocks.BAMBOO) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.MUSHROOM_STEM)
                || s.is(Blocks.BROWN_MUSHROOM_BLOCK) || s.is(Blocks.RED_MUSHROOM_BLOCK) || s.is(Blocks.BEE_NEST) || s.is(Blocks.COCOA)
                || s.is(Blocks.SNOW) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.PUMPKIN) || s.is(Blocks.MELON) || s.is(Blocks.BIG_DRIPLEAF)
                || s.is(Blocks.BIG_DRIPLEAF_STEM) || s.is(Blocks.MOSS_CARPET) || s.is(Blocks.AZALEA) || s.is(Blocks.FLOWERING_AZALEA);
    }

    /** Altura del suelo de verdad en la columna (bajo árboles, plantas y nieve; el fondo si hay agua). */
    public static int groundY(ServerLevel level, int x, int z) {
        int min = level.getMinBuildHeight();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y, z);
        while (y > min) {
            p.setY(y);
            BlockState s = level.getBlockState(p);
            if (!vegetation(s) && s.getFluidState().isEmpty()) {
                break;
            }
            y--;
        }
        return y;
    }

    private static int mix(int x, int y, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (int) (h & 0x7FFFFFFF);
    }

    /** "a|b|c": uno de ellos según la posición (siempre el mismo para la misma posición). */
    private static BlockState pickState(String pal, int x, int y, int z) {
        if (pal == null) {
            return null;
        }
        if (pal.indexOf('|') < 0) {
            return Estructuras.state(pal);
        }
        String[] o = pal.split("\\|");
        return Estructuras.state(o[mix(x, y, z) % o.length]);
    }

    private void stepBlend() {
        if (blendKeys == null) {
            blendKeys = new ArrayList<>(bp.blend.keySet());
        }
        if (idx >= blendKeys.size()) {
            next();
            return;
        }
        long key = blendKeys.get(idx++);
        StructBp.Blend bl = bp.blend.get(key);
        BlockPos wc = world(StructBp.kx(key), 0, StructBp.kz(key));
        int wx = wc.getX(), wz = wc.getZ();
        int ground = groundY(level, wx, wz);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(wx, ground, wz);
        BlockState natural = level.getBlockState(p);
        boolean clear = bl.w() < 0.999F;
        int yt = clear ? oy + (int) Math.round((ground - oy) * (double) bl.w()) : ground;
        yt = Math.max(minY + 1, Math.min(maxY - 2, yt));
        int topY = Math.max(ground, level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1);
        // árboles: en la zona que se aplana se quitan enteros; en la ceniza se quedan quemados (sin hojas)
        for (int y = ground + 1; y <= topY && y < maxY; y++) {
            p.setY(y);
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.LOGS) && !treeSeen.contains(p)) {
                tree(p.immutable(), clear);
            }
        }
        if (clear) {
            for (int y = Math.max(yt + 1, minY); y <= topY && y < maxY; y++) {
                p.setY(y);
                if (!level.getBlockState(p).isAir()) {
                    put(p.immutable(), Blocks.AIR.defaultBlockState());
                }
            }
        } else {
            // solo las plantas que tapan la ceniza
            for (int y = ground + 1; y <= ground + 3 && y < maxY; y++) {
                p.setY(y);
                BlockState s = level.getBlockState(p);
                if (s.isAir() || !s.getFluidState().isEmpty() || !(s.canBeReplaced() || s.is(Blocks.SNOW) || s.is(Blocks.MOSS_CARPET))) {
                    break;
                }
                put(p.immutable(), Blocks.AIR.defaultBlockState());
            }
        }
        // relleno hasta la nueva superficie (si queda más alta que el terreno)
        boolean grassy = natural.is(Blocks.GRASS_BLOCK) || natural.is(Blocks.PODZOL) || natural.is(Blocks.MYCELIUM) || natural.is(Blocks.SNOW_BLOCK);
        BlockState fillNatural = grassy || natural.isAir() || !natural.getFluidState().isEmpty() ? Blocks.DIRT.defaultBlockState() : natural;
        for (int y = ground + 1; y < yt; y++) {
            BlockState f = bl.sub() != null ? pickState(bl.sub(), wx, y, wz) : fillNatural;
            if (f != null) {
                put(new BlockPos(wx, y, wz), f);
            }
        }
        BlockState top = bl.top() != null ? pickState(bl.top(), wx, yt, wz)
                : (yt != ground ? (natural.isAir() || !natural.getFluidState().isEmpty() ? Blocks.DIRT.defaultBlockState() : natural) : null);
        if (top != null) {
            put(new BlockPos(wx, yt, wz), top.rotate(rot));
        }
        surface.put(key, yt);
    }

    /** Quita un árbol entero (tronco y hojas) o lo deja quemado: tronco oscuro y sin hojas. */
    private void tree(BlockPos start, boolean remove) {
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(start);
        treeSeen.add(start);
        int n = 0;
        BlockState charred = Blocks.DARK_OAK_LOG.defaultBlockState();
        while (!q.isEmpty() && n < TREE_LIMIT) {
            BlockPos c = q.poll();
            BlockState s = level.getBlockState(c);
            boolean log = s.is(BlockTags.LOGS);
            boolean leaf = s.is(BlockTags.LEAVES) && !(s.hasProperty(LeavesBlock.PERSISTENT) && s.getValue(LeavesBlock.PERSISTENT));
            if (!log && !leaf && !c.equals(start)) {
                continue;
            }
            n++;
            if (log && !remove) {
                BlockState ch = charred;
                if (s.hasProperty(BlockStateProperties.AXIS)) {
                    ch = ch.setValue(BlockStateProperties.AXIS, s.getValue(BlockStateProperties.AXIS));
                }
                put(c, ch);
            } else {
                put(c, Blocks.AIR.defaultBlockState());
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        // las hojas solo se siguen por las caras; los troncos también en diagonal (ramas de acacia)
                        if (!log && Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1) {
                            continue;
                        }
                        BlockPos o = c.offset(dx, dy, dz);
                        if (Math.abs(o.getX() - start.getX()) > 9 || Math.abs(o.getZ() - start.getZ()) > 9 || o.getY() < start.getY() - 1
                                || o.getY() >= maxY || treeSeen.contains(o)) {
                            continue;
                        }
                        BlockState os = level.getBlockState(o);
                        if (os.is(BlockTags.LOGS) || os.is(BlockTags.LEAVES)) {
                            treeSeen.add(o);
                            q.add(o);
                        }
                    }
                }
            }
        }
        if (remove) {
            treesCut++;
        } else {
            treesCharred++;
        }
    }

    // ------------------------------------------------------------------ el plano

    private void stepBlock() {
        if (entries == null) {
            entries = new ArrayList<>(bp.m.entrySet());
            entries.sort((a1, b1) -> Integer.compare(StructBp.ky(a1.getKey()), StructBp.ky(b1.getKey())));
        }
        if (idx >= entries.size()) {
            entries = null;
            next();
            return;
        }
        Map.Entry<Long, String> e = entries.get(idx++);
        long key = e.getKey();
        BlockPos p = world(StructBp.kx(key), StructBp.ky(key), StructBp.kz(key));
        placeAt(p, e.getValue());
    }

    private void placeAt(BlockPos p, String spec) {
        if (p.getY() < minY || p.getY() >= maxY) {
            return;
        }
        BlockState st0 = Estructuras.state(spec);
        if (st0 == null) {
            return;
        }
        BlockState st = st0.rotate(rot);
        put(p, st);
        placed.add(p);
        Block b = st.getBlock();
        if (b instanceof StairBlock || b instanceof FenceBlock || b instanceof WallBlock || b instanceof IronBarsBlock) {
            reshape.add(p);
        }
    }

    private void stepAnchor() {
        if (idx >= bp.anchors.size()) {
            next();
            return;
        }
        StructBp.Anchor a = bp.anchors.get(idx++);
        Integer sy = surface.get(StructBp.key(a.x(), 0, a.z()));
        BlockPos wc = world(a.x(), 0, a.z());
        int y0 = sy != null ? sy : groundY(level, wc.getX(), wc.getZ());
        for (Map.Entry<Long, String> e : a.m().entrySet()) {
            long k = e.getKey();
            BlockPos off = world(BlockPos.ZERO, rot, StructBp.kx(k), 0, StructBp.kz(k));
            placeAt(new BlockPos(wc.getX() + off.getX(), y0 + 1 + StructBp.ky(k), wc.getZ() + off.getZ()), e.getValue());
        }
    }

    // ------------------------------------------------------------------ remates

    private void stepReshape() {
        if (idx >= reshape.size()) {
            next();
            return;
        }
        BlockPos p = reshape.get(idx++);
        BlockState cur = level.getBlockState(p);
        BlockState fixed = Block.updateFromNeighbourShapes(cur, level, p);
        if (fixed != cur) {
            level.setBlock(p, fixed, FLAGS);
        }
    }

    /**
     * Red de seguridad: lo que no pueda sostenerse (nenúfar sin agua, antorcha sin muro, alfombra en el aire...) se quita ahora, en
     * silencio; si no, más tarde se rompería solo y dejaría el objeto tirado. Se repite (hasta 6 pasadas) por si al quitar uno otro
     * se queda sin apoyo.
     */
    private void stepSupport() {
        if (idx >= placed.size()) {
            supportPass++;
            if (supportChanged == 0 || supportPass >= 6) {
                next();
                return;
            }
            supportChanged = 0;
            idx = 0;
            return;
        }
        BlockPos p = placed.get(idx++);
        BlockState cur = level.getBlockState(p);
        if (cur.isAir() || cur.canSurvive(level, p)) {
            return;
        }
        unsupported.merge(String.valueOf(ForgeRegistries.BLOCKS.getKey(cur.getBlock())), 1, Integer::sum);
        level.setBlock(p, cur.getFluidState().createLegacyBlock(), FLAGS);
        supportChanged++;
        removed++;
    }

    private List<Map.Entry<Long, String>> nbtList;

    private void stepNbt() {
        if (nbtList == null) {
            nbtList = new ArrayList<>(bp.nbt.entrySet());
        }
        if (idx >= nbtList.size()) {
            nbtList = null;
            next();
            return;
        }
        Map.Entry<Long, String> e = nbtList.get(idx++);
        long k = e.getKey();
        BlockPos p = world(StructBp.kx(k), StructBp.ky(k), StructBp.kz(k));
        BlockEntity be = level.getBlockEntity(p);
        if (be == null) {
            return;
        }
        try {
            CompoundTag add = TagParser.parseTag(e.getValue());
            CompoundTag full = be.saveWithoutMetadata();
            full.merge(add);
            be.load(full);
            be.setChanged();
        } catch (Exception ex) {
            com.mojang.logging.LogUtils.getLogger().warn("[Estructuras] datos de bloque no válidos {}: {}", e.getValue(), ex.getMessage());
        }
    }

    private void stepMobs() {
        if (idx >= bp.mobs.size()) {
            next();
            return;
        }
        int i = idx++;
        StructBp.Mob m = bp.mobs.get(i);
        mobs.add(spawn(level, m, world(m.x(), m.y(), m.z()), i, onMob));
    }

    /** Pone un enemigo del plano (persistente) en {@code p}; devuelve su UUID o null. */
    static UUID spawn(ServerLevel level, StructBp.Mob m, BlockPos p, int index, BiConsumer<Integer, net.minecraft.world.entity.Mob> hook) {
        net.minecraft.world.entity.EntityType<?> type = ForgeRegistries.ENTITY_TYPES
                .getValue(new ResourceLocation(com.mundosdeaxelis.MundosDeAxelis.MODID, m.kind()));
        if (type == null) {
            return null;
        }
        net.minecraft.world.entity.Entity e = type.create(level);
        if (!(e instanceof net.minecraft.world.entity.Mob mob)) {
            return null;
        }
        mob.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), net.minecraft.world.entity.MobSpawnType.STRUCTURE, null, null);
        mob.setPersistenceRequired();
        if (m.tag() != null) {
            mob.getPersistentData().putString(TAG, m.tag());
            if (mob instanceof com.mundosdeaxelis.entity.Habitante h) {
                if (!h.prisonerAllowed(level, m.tag())) {
                    return null; // ese NPC ya fue rescatado en este mundo: la celda queda vacía
                }
                h.setup(m.tag(), com.mundosdeaxelis.entity.Habitante.PRESO, p);
            }
        }
        if (hook != null) {
            hook.accept(index, mob);
        }
        // etapa 275: a veces un enemigo sale como élite
        Elites.maybe(mob, level.random);
        return level.addFreshEntity(mob) ? mob.getUUID() : null;
    }

    // ------------------------------------------------------------------ poner un bloque

    /** Pone un bloque guardando antes lo que había (y su entidad de bloque, si tenía) cuando se puede deshacer. */
    private void put(BlockPos p, BlockState st) {
        if (p.getY() < minY || p.getY() >= maxY) {
            return;
        }
        BlockState old = level.getBlockState(p);
        if (snaps != null && !snaps.containsKey(p)) {
            BlockEntity be = level.getBlockEntity(p);
            snaps.put(p.immutable(), new Snap(old, be == null ? null : be.saveWithFullMetadata()));
        }
        if (old == st) {
            return;
        }
        BlockEntity be = level.getBlockEntity(p);
        if (be != null) {
            Clearable.tryClear(be);
        }
        level.setBlock(p, st, FLAGS);
    }
}
