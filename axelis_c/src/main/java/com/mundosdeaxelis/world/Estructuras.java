package com.mundosdeaxelis.world;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Estructuras de fondo para grabar videos, hechas solo con comandos (como WorldEdit):
 * {@code /mundos estructura <bosque|forja|hielo|torre>} la construye delante de quien escribe, mirando hacia él;
 * {@code /mundos estructura deshacer [veces]} la quita y deja el terreno como estaba (bloques, entidades de bloque y todo).
 * Los bloques propios del mod (decoraciones) se usan si existen; si no, simplemente se omiten.
 */
public final class Estructuras {
    private static final String MOD = "mundos_de_axelis";
    private static final int MAX_UNDO = 12;
    private static final int FLAGS = 2;
    private static final Map<UUID, Deque<Undo>> UNDO = new HashMap<>();

    private Estructuras() {
    }

    private enum Kind {
        BOSQUE("bosque", "Templo del bosque"),
        FORJA("forja", "Fortaleza de la forja"),
        HIELO("hielo", "Palacio de hielo"),
        TORRE("torre", "Torre en ruinas");

        final String id;
        final String title;

        Kind(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    // ------------------------------------------------------------------ comando

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("estructura");
        for (Kind k : Kind.values()) {
            root.then(Commands.literal(k.id).executes(c -> place(c.getSource(), k)));
        }
        root.then(Commands.literal("deshacer").executes(c -> undo(c.getSource(), 1))
                .then(Commands.argument("veces", IntegerArgumentType.integer(1, MAX_UNDO)).executes(c -> undo(c.getSource(), IntegerArgumentType.getInteger(c, "veces")))));
        return root;
    }

    private static UUID key(CommandSourceStack src) {
        return src.getEntity() == null ? new UUID(0, 0) : src.getEntity().getUUID();
    }

    private record Snap(BlockState old, CompoundTag be) {
    }

    private static final class Undo {
        final ResourceKey<Level> dim;
        final String title;
        final Map<BlockPos, Snap> snaps = new LinkedHashMap<>();

        Undo(ResourceKey<Level> dim, String title) {
            this.dim = dim;
            this.title = title;
        }
    }

    private static int place(CommandSourceStack src, Kind kind) {
        ServerLevel level = src.getLevel();
        Vec2 rot = src.getRotation();
        Direction fwd = Direction.fromYRot(rot.y);
        BlockPos feet = BlockPos.containing(src.getPosition());
        BlockPos origin = feet.relative(fwd, 4);
        Bp bp = new Bp(kind.ordinal() * 1000L + 17);
        switch (kind) {
            case BOSQUE -> bosque(bp);
            case FORJA -> forja(bp);
            case HIELO -> hielo(bp);
            case TORRE -> torre(bp);
            default -> {
            }
        }
        Rotation r = rotationFor(fwd);
        Undo undo = new Undo(level.dimension(), kind.title);
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        // cimientos: columnas hacia abajo hasta tocar suelo firme
        for (Bp.Found f : bp.found) {
            for (int x = f.x0; x <= f.x1; x++) {
                for (int z = f.z0; z <= f.z1; z++) {
                    BlockPos base = origin.offset(new BlockPos(x, 0, z).rotate(r));
                    for (int y = origin.getY() - 1; y > origin.getY() - 1 - 24 && y >= minY; y--) {
                        BlockPos p = new BlockPos(base.getX(), y, base.getZ());
                        BlockState cur = level.getBlockState(p);
                        boolean soft = cur.isAir() || !cur.getFluidState().isEmpty() || cur.canBeReplaced() || cur.is(BlockTags.LEAVES) || cur.is(BlockTags.LOGS);
                        if (!soft) {
                            break;
                        }
                        put(level, undo, p, f.state.rotate(r));
                    }
                }
            }
        }
        // el plano, de abajo hacia arriba (para que lo que cuelga o se apoya encuentre su apoyo)
        List<Map.Entry<BlockPos, BlockState>> entries = new ArrayList<>(bp.m.entrySet());
        entries.sort((a, b) -> Integer.compare(a.getKey().getY(), b.getKey().getY()));
        List<BlockPos> reshape = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> e : entries) {
            BlockPos p = origin.offset(e.getKey().rotate(r));
            if (p.getY() < minY || p.getY() >= maxY) {
                continue;
            }
            BlockState st = e.getValue().rotate(r);
            put(level, undo, p, st);
            Block b = st.getBlock();
            if (b instanceof StairBlock || b instanceof FenceBlock || b instanceof WallBlock || b instanceof IronBarsBlock) {
                reshape.add(p);
            }
        }
        // escaleras, vallas, muros y barrotes: ahora que todo está puesto, que se conecten bien
        for (BlockPos p : reshape) {
            BlockState cur = level.getBlockState(p);
            BlockState fixed = Block.updateFromNeighbourShapes(cur, level, p);
            if (fixed != cur) {
                level.setBlock(p, fixed, FLAGS);
            }
        }
        Deque<Undo> dq = UNDO.computeIfAbsent(key(src), k -> new ArrayDeque<>());
        dq.push(undo);
        while (dq.size() > MAX_UNDO) {
            dq.removeLast();
        }
        int n = undo.snaps.size();
        src.sendSuccess(() -> Component.literal(kind.title + " construida (" + n + " bloques). Para quitarla: /mundos estructura deshacer"), true);
        return 1;
    }

    /** Pone un bloque guardando antes lo que había (y su entidad de bloque, si tenía). */
    private static void put(ServerLevel level, Undo undo, BlockPos p, BlockState st) {
        BlockState old = level.getBlockState(p);
        if (!undo.snaps.containsKey(p)) {
            BlockEntity be = level.getBlockEntity(p);
            undo.snaps.put(p.immutable(), new Snap(old, be == null ? null : be.saveWithFullMetadata()));
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

    private static int undo(CommandSourceStack src, int times) {
        Deque<Undo> dq = UNDO.get(key(src));
        if (dq == null || dq.isEmpty()) {
            src.sendFailure(Component.literal("No hay nada que deshacer."));
            return 0;
        }
        int done = 0;
        int blocks = 0;
        while (done < times && !dq.isEmpty()) {
            Undo u = dq.pop();
            ServerLevel level = src.getServer().getLevel(u.dim);
            if (level == null) {
                continue;
            }
            // al revés: primero lo de arriba, para no dejar cosas sin apoyo
            List<Map.Entry<BlockPos, Snap>> list = new ArrayList<>(u.snaps.entrySet());
            list.sort((a, b) -> Integer.compare(b.getKey().getY(), a.getKey().getY()));
            for (Map.Entry<BlockPos, Snap> e : list) {
                BlockPos p = e.getKey();
                BlockEntity cur = level.getBlockEntity(p);
                if (cur != null) {
                    Clearable.tryClear(cur);
                }
                level.setBlock(p, e.getValue().old, FLAGS);
                if (e.getValue().be != null) {
                    BlockEntity be = level.getBlockEntity(p);
                    if (be != null) {
                        be.load(e.getValue().be);
                        be.setChanged();
                    }
                }
                blocks++;
            }
            done++;
        }
        int d = done;
        int b = blocks;
        src.sendSuccess(() -> Component.literal("Deshecho: " + d + " estructura(s), " + b + " bloques restaurados."), true);
        return done;
    }

    private static Rotation rotationFor(Direction fwd) {
        return switch (fwd) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    // ------------------------------------------------------------------ bloques

    private static BlockState S(String id) {
        ResourceLocation rl = id.indexOf(':') >= 0 ? new ResourceLocation(id) : new ResourceLocation("minecraft", id);
        return ForgeRegistries.BLOCKS.containsKey(rl) ? ForgeRegistries.BLOCKS.getValue(rl).defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    /** Un bloque propio del mod, o null si no existe (entonces se omite). */
    private static BlockState M(String name) {
        ResourceLocation rl = new ResourceLocation(MOD, name);
        return ForgeRegistries.BLOCKS.containsKey(rl) ? ForgeRegistries.BLOCKS.getValue(rl).defaultBlockState() : null;
    }

    private static <T extends Comparable<T>> BlockState with(BlockState s, Property<T> p, T v) {
        return s.hasProperty(p) ? s.setValue(p, v) : s;
    }

    private static BlockState stair(String id, Direction up) {
        return with(with(S(id), BlockStateProperties.HORIZONTAL_FACING, up), BlockStateProperties.HALF, Half.BOTTOM);
    }

    private static BlockState slab(String id, boolean top) {
        return with(S(id), BlockStateProperties.SLAB_TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    private static BlockState log(String id, Direction.Axis axis) {
        return with(S(id), BlockStateProperties.AXIS, axis);
    }

    private static BlockState leaves(String id) {
        return with(S(id), BlockStateProperties.PERSISTENT, true);
    }

    private static BlockState hang(String id) {
        return with(S(id), BlockStateProperties.HANGING, true);
    }

    private static BlockState wallBanner(String id, Direction face) {
        return with(S(id), BlockStateProperties.HORIZONTAL_FACING, face);
    }

    // ------------------------------------------------------------------ el plano

    /** Plano en coordenadas locales: x a lo ancho (centrado en 0), y hacia arriba (0 = nivel de los pies), z hacia dentro (0 = el frente). */
    private static final class Bp {
        record Found(int x0, int z0, int x1, int z1, BlockState state) {
        }

        final Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        final List<Found> found = new ArrayList<>();
        final Random r;

        Bp(long seed) {
            r = new Random(seed);
        }

        void set(int x, int y, int z, BlockState s) {
            if (s != null) {
                m.put(new BlockPos(x, y, z), s);
            }
        }

        void setIfAbsent(int x, int y, int z, BlockState s) {
            if (s != null) {
                m.putIfAbsent(new BlockPos(x, y, z), s);
            }
        }

        void box(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                    for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                        set(x, y, z, s);
                    }
                }
            }
        }

        /** Mezcla al azar: cada bloque elige uno de la lista. */
        void mix(int x0, int y0, int z0, int x1, int y1, int z1, BlockState... opts) {
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                    for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                        set(x, y, z, opts[r.nextInt(opts.length)]);
                    }
                }
            }
        }

        void air(int x0, int y0, int z0, int x1, int y1, int z1) {
            box(x0, y0, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
        }

        /** Cuatro paredes (sin techo ni piso), mezclando bloques. */
        void walls(int x0, int y0, int z0, int x1, int y1, int z1, BlockState... opts) {
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        if (x == x0 || x == x1 || z == z0 || z == z1) {
                            set(x, y, z, opts[r.nextInt(opts.length)]);
                        }
                    }
                }
            }
        }

        void found(int x0, int z0, int x1, int z1, BlockState s) {
            found.add(new Found(Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1), s));
        }

        /** Disco (lleno) o anillo en el nivel y. */
        void disc(int cx, int y, int cz, double rad, boolean ring, BlockState... opts) {
            int ri = (int) Math.ceil(rad);
            for (int x = -ri; x <= ri; x++) {
                for (int z = -ri; z <= ri; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d <= rad + 0.2 && (!ring || d >= rad - 1.0)) {
                        set(cx + x, y, cz + z, opts[r.nextInt(opts.length)]);
                    }
                }
            }
        }

        void sphere(int cx, int cy, int cz, double rad, BlockState s) {
            int ri = (int) Math.ceil(rad);
            for (int x = -ri; x <= ri; x++) {
                for (int y = -ri; y <= ri; y++) {
                    for (int z = -ri; z <= ri; z++) {
                        double d = Math.sqrt(x * x + y * y * 1.3 + z * z);
                        if (d <= rad && r.nextInt(10) > 0) {
                            setIfAbsent(cx + x, cy + y, cz + z, s);
                        }
                    }
                }
            }
        }

        /** Techo escalonado: cada capa más chica, con escaleras en el borde (suben hacia el centro) y bloque lleno por dentro. */
        void pyramid(int x0, int x1, int z0, int z1, int y, int layers, BlockState full, String stairId) {
            for (int k = 0; k < layers; k++) {
                int a = x0 + k;
                int b = x1 - k;
                int c = z0 + k;
                int d = z1 - k;
                if (a > b || c > d) {
                    break;
                }
                for (int x = a; x <= b; x++) {
                    for (int z = c; z <= d; z++) {
                        boolean edge = x == a || x == b || z == c || z == d;
                        if (!edge) {
                            set(x, y + k, z, full);
                            continue;
                        }
                        Direction dir = z == c ? Direction.SOUTH : z == d ? Direction.NORTH : x == a ? Direction.EAST : Direction.WEST;
                        set(x, y + k, z, stair(stairId, dir));
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ 1. BOSQUE

    private static void bosque(Bp b) {
        BlockState moss = S("mossy_stone_bricks");
        BlockState brick = S("stone_bricks");
        BlockState crack = S("cracked_stone_bricks");
        BlockState mossBlock = S("moss_block");
        BlockState oakLog = log("oak_log", Direction.Axis.Y);
        BlockState leaf = leaves("oak_leaves");
        BlockState azLeaf = leaves("azalea_leaves");
        BlockState flLeaf = leaves("flowering_azalea_leaves");

        b.found(-10, 0, 10, 16, S("mossy_cobblestone"));
        b.air(-13, 0, -7, 13, 26, 17);
        // jardín al frente y camino
        b.box(-13, -1, -7, 13, -1, -1, S("grass_block"));
        b.mix(-1, -1, -7, 1, -1, -1, S("dirt_path"), S("mossy_cobblestone"), S("gravel"), S("cobblestone"));
        for (int i = 0; i < 40; i++) {
            int x = b.r.nextInt(27) - 13;
            int z = -7 + b.r.nextInt(7);
            if (Math.abs(x) > 1) {
                b.set(x, 0, z, S(new String[] {"poppy", "azure_bluet", "fern", "dandelion", "short_grass", "oxeye_daisy"}[b.r.nextInt(6)]));
            }
        }
        // plataforma
        b.mix(-10, 0, 0, 10, 0, 16, moss, moss, brick, crack, mossBlock);
        for (int x = -3; x <= 3; x++) {
            b.set(x, 0, 0, stair("mossy_stone_brick_stairs", Direction.SOUTH));
        }
        b.box(-3, 0, 1, 3, 0, 1, S("mossy_stone_bricks"));
        // sala principal
        b.walls(-6, 1, 3, 6, 8, 14, moss, moss, brick, crack);
        b.box(-5, 1, 4, 5, 8, 13, Blocks.AIR.defaultBlockState());
        b.box(-5, 0, 4, 5, 0, 13, S("mossy_cobblestone"));
        b.box(-6, 8, 3, 6, 8, 14, S("mossy_stone_bricks"));
        b.air(-2, 1, 3, 2, 5, 4);
        b.box(-3, 6, 3, 3, 6, 3, S("chiseled_stone_bricks"));
        // pilares con troncos a los lados de la puerta
        for (int sx : new int[] {-4, 4}) {
            b.box(sx, 1, 2, sx, 7, 2, oakLog);
            b.set(sx, 8, 2, S("mossy_stone_brick_wall"));
            b.set(sx, 9, 2, S("lantern"));
        }
        // dintel y arco de la puerta
        b.box(-3, 1, 3, -3, 6, 3, S("chiseled_stone_bricks"));
        b.box(3, 1, 3, 3, 6, 3, S("chiseled_stone_bricks"));
        b.set(-2, 5, 3, stair("mossy_stone_brick_stairs", Direction.EAST));
        b.set(2, 5, 3, stair("mossy_stone_brick_stairs", Direction.WEST));
        b.set(0, 4, 4, hang("lantern"));
        b.set(0, 5, 4, S("mossy_stone_bricks"));
        // brillo verde del fondo visto por la puerta
        b.box(-1, 1, 13, 1, 3, 13, S("shroomlight"));
        b.set(0, 1, 7, S("moss_block"));
        // techo escalonado
        b.pyramid(-7, 7, 2, 15, 9, 7, moss, "mossy_stone_brick_stairs");
        b.set(0, 16, 8, S("shroomlight"));
        // columnas esquineras con musgo y hojas
        for (int sx : new int[] {-7, 7}) {
            for (int z : new int[] {2, 15}) {
                b.box(sx, 1, z, sx, 6, z, S("mossy_stone_bricks"));
                b.set(sx, 7, z, S("mossy_stone_brick_wall"));
                b.set(sx, 8, z, S("lantern"));
                b.sphere(sx, 8, z, 2.2, b.r.nextBoolean() ? azLeaf : flLeaf);
            }
        }
        b.sphere(-6, 11, 8, 2.6, leaf);
        b.sphere(6, 11, 9, 2.6, leaf);
        b.sphere(0, 16, 8, 2.0, azLeaf);
        // arbustos y alfombras de musgo en la plataforma
        for (int i = 0; i < 28; i++) {
            int x = b.r.nextInt(19) - 9;
            int z = b.r.nextInt(15) + 1;
            if (z < 3 && Math.abs(x) <= 3) {
                continue;
            }
            if (z >= 2 && z <= 15 && Math.abs(x) <= 7 && z >= 3 && z <= 14 && Math.abs(x) <= 6) {
                continue; // dentro de la sala
            }
            b.set(x, 1, z, b.r.nextInt(3) == 0 ? S("azalea") : S("moss_carpet"));
        }
        // dos árboles grandes a los lados
        int[][] trees = {{-12, 7}, {12, 9}, {-12, 14}, {12, 14}};
        for (int[] t : trees) {
            int h = 9 + b.r.nextInt(3);
            b.found(t[0], t[1], t[0] + 1, t[1] + 1, S("dirt"));
            b.box(t[0], 0, t[1], t[0] + 1, h, t[1] + 1, oakLog);
            b.sphere(t[0], h, t[1], 4.0, leaf);
            b.sphere(t[0] + 1, h + 1, t[1] + 1, 3.0, leaf);
        }
        // decoraciones del mod
        b.set(-5, 1, 1, M("decor_trophy_forest"));
        b.set(5, 1, 1, M("decor_trophy_forest"));
        b.set(-9, 1, 8, M("decor_glow_mushroom"));
        b.set(9, 1, 8, M("decor_glow_mushroom"));
        b.set(-8, 1, 14, M("decor_garden_bench"));
        b.set(8, 1, 14, M("decor_garden_bench"));
    }

    // ------------------------------------------------------------------ 2. FORJA

    private static void forja(Bp b) {
        BlockState bb = S("polished_blackstone_bricks");
        BlockState bbCrack = S("cracked_polished_blackstone_bricks");
        BlockState nb = S("nether_bricks");
        BlockState nbCrack = S("cracked_nether_bricks");
        BlockState black = S("blackstone");
        BlockState lava = S("lava");
        String bStair = "polished_blackstone_brick_stairs";

        b.found(-11, 0, 11, 14, black);
        b.air(-13, 0, -7, 13, 30, 15);
        // suelo de ceniza al frente y camino
        b.mix(-13, -1, -7, 13, -1, -1, black, S("basalt"), S("magma_block"), S("coarse_dirt"), S("gravel"));
        b.mix(-1, -1, -7, 1, -1, -1, S("polished_blackstone"), bb);
        // plataforma
        b.mix(-11, 0, 0, 11, 0, 14, bb, bb, bbCrack, S("polished_blackstone"), black);
        for (int x = -3; x <= 3; x++) {
            b.set(x, 0, 0, stair(bStair, Direction.SOUTH));
        }
        // canal de lava al frente, con un puente al centro (todo encerrado, la lava no se riega)
        b.box(-9, 0, 2, -3, 0, 2, lava);
        b.box(3, 0, 2, 9, 0, 2, lava);
        b.box(-10, 0, 2, -10, 0, 2, black);
        b.box(10, 0, 2, 10, 0, 2, black);
        b.box(-9, -1, 2, -3, -1, 2, black);
        b.box(3, -1, 2, 9, -1, 2, black);
        b.box(-2, 0, 2, 2, 0, 2, S("polished_blackstone"));
        // sala principal
        b.walls(-7, 1, 4, 7, 9, 13, bb, bb, bbCrack, S("polished_blackstone"));
        b.box(-6, 1, 5, 6, 9, 12, Blocks.AIR.defaultBlockState());
        b.mix(-6, 0, 5, 6, 0, 12, S("magma_block"), S("magma_block"), black);
        b.box(-7, 9, 4, 7, 9, 13, bb);
        b.air(-2, 1, 4, 2, 6, 5);
        // marco del portón: obsidiana llorona
        for (int sx : new int[] {-3, 3}) {
            b.box(sx, 1, 4, sx, 7, 4, S("obsidian"));
            b.set(sx, 3, 4, S("crying_obsidian"));
            b.set(sx, 5, 4, S("crying_obsidian"));
        }
        b.box(-3, 7, 4, 3, 7, 4, S("gilded_blackstone"));
        b.box(-2, 6, 4, 2, 6, 4, S("chiseled_polished_blackstone"));
        b.set(0, 5, 5, hang("soul_lantern"));
        b.set(0, 6, 5, S("chain"));
        b.set(-2, 4, 5, hang("lantern"));
        b.set(2, 4, 5, hang("lantern"));
        b.set(-2, 5, 5, S("chain"));
        b.set(2, 5, 5, S("chain"));
        b.set(-2, 6, 5, S("polished_blackstone"));
        b.set(2, 6, 5, S("polished_blackstone"));
        // luz del interior visible desde fuera
        b.box(-1, 1, 12, 1, 4, 12, S("shroomlight"));
        b.box(-5, 3, 5, -5, 3, 12, S("iron_bars"));
        // techo escalonado
        b.pyramid(-8, 8, 3, 14, 10, 6, bb, bStair);
        // torres de las esquinas con brasa encendida
        for (int sx : new int[] {-11, 9}) {
            for (int z : new int[] {0, 12}) {
                b.walls(sx, 1, z, sx + 2, 14, z + 2, nb, nb, nbCrack, bb);
                b.box(sx + 1, 1, z + 1, sx + 1, 13, z + 1, Blocks.AIR.defaultBlockState());
                b.box(sx, 15, z, sx + 2, 15, z + 2, nb);
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        if ((i + j) % 2 == 0 && (i != 1 || j != 1)) {
                            b.set(sx + i, 16, z + j, S("nether_brick_fence"));
                        }
                    }
                }
                b.set(sx + 1, 15, z + 1, S("netherrack"));
                b.set(sx + 1, 16, z + 1, S("fire"));
                b.set(sx + 1, 9, z, hang("lantern"));
            }
        }
        // chimeneas atrás
        for (int sx : new int[] {-5, 4}) {
            b.box(sx, 10, 11, sx + 1, 18, 12, black);
            b.box(sx, 19, 11, sx + 1, 22, 12, S("nether_bricks"));
            b.set(sx, 23, 11, S("campfire"));
            b.set(sx + 1, 23, 12, S("campfire"));
        }
        // pilares de entrada con braseros y valla baja
        for (int sx : new int[] {-6, 6}) {
            b.box(sx, 1, 1, sx, 3, 1, S("polished_blackstone_bricks"));
            b.set(sx, 4, 1, S("soul_campfire"));
        }
        for (int x = -9; x <= 9; x++) {
            if (Math.abs(x) > 3) {
                b.set(x, 1, 1, S("polished_blackstone_brick_wall"));
            }
        }
        // decoraciones del mod
        b.set(-4, 1, 3, M("decor_trophy_forge"));
        b.set(4, 1, 3, M("decor_trophy_forge"));
        b.set(-9, 1, 8, M("decor_steam_boiler"));
        b.set(9, 1, 8, M("decor_steam_boiler"));
        b.set(-2, 1, 3, M("decor_ember_bowl"));
        b.set(2, 1, 3, M("decor_ember_bowl"));
    }

    // ------------------------------------------------------------------ 3. HIELO

    private static void hielo(Bp b) {
        BlockState pack = S("packed_ice");
        BlockState blue = S("blue_ice");
        BlockState snow = S("snow_block");
        BlockState quartz = S("smooth_quartz");
        BlockState calcite = S("calcite");
        BlockState lamp = S("sea_lantern");
        BlockState glass = S("light_blue_stained_glass");

        b.found(-9, 0, 9, 16, pack);
        b.air(-14, 0, -7, 14, 30, 18);
        b.mix(-14, -1, -7, 14, -1, -1, snow, snow, pack);
        b.mix(-1, -1, -7, 1, -1, -1, pack, blue);
        // plataforma
        b.mix(-9, 0, 0, 9, 0, 16, snow, quartz, pack, calcite);
        for (int x = -3; x <= 3; x++) {
            b.set(x, 0, 0, stair("quartz_stairs", Direction.SOUTH));
        }
        // sala central
        b.walls(-5, 1, 4, 5, 11, 14, pack, pack, blue, calcite);
        b.box(-4, 1, 5, 4, 11, 13, Blocks.AIR.defaultBlockState());
        b.box(-5, 11, 4, 5, 11, 14, pack);
        for (int y : new int[] {4, 5}) {
            for (int x = -4; x <= 4; x += 2) {
                b.set(x, y, 4, glass);
                b.set(x, y, 14, glass);
            }
            for (int z = 6; z <= 12; z += 3) {
                b.set(-5, y, z, glass);
                b.set(5, y, z, glass);
            }
        }
        b.box(-3, 1, 5, 3, 1, 13, S("blue_ice"));
        b.box(-1, 1, 12, 1, 5, 13, lamp);
        // portón con arco de hielo azul
        b.air(-2, 1, 4, 2, 6, 5);
        b.box(-3, 1, 4, -3, 7, 4, blue);
        b.box(3, 1, 4, 3, 7, 4, blue);
        b.box(-3, 7, 4, 3, 7, 4, blue);
        b.set(-2, 6, 4, stair("quartz_stairs", Direction.EAST));
        b.set(2, 6, 4, stair("quartz_stairs", Direction.WEST));
        b.set(0, 6, 5, hang("soul_lantern"));
        // techo en pirámide de cuarzo
        b.pyramid(-6, 6, 3, 15, 12, 7, pack, "quartz_stairs");
        b.set(0, 19, 9, S("end_rod"));
        b.set(0, 20, 9, S("end_rod"));
        // cuatro torres redondas con tejado cónico
        int[][] towers = {{-8, 3, 15}, {8, 3, 15}, {-8, 15, 20}, {8, 15, 20}};
        for (int[] t : towers) {
            int cx = t[0];
            int cz = t[1];
            int h = t[2];
            for (int y = 1; y <= h; y++) {
                b.disc(cx, y, cz, 2.6, true, pack, pack, blue);
            }
            b.disc(cx, 0, cz, 2.6, false, snow);
            for (int y = 4; y <= h - 2; y += 4) {
                b.set(cx, y, cz - 3, glass);
                b.set(cx, y, cz + 3, glass);
                b.set(cx - 3, y, cz, glass);
                b.set(cx + 3, y, cz, glass);
            }
            b.disc(cx, h + 1, cz, 3.6, false, blue);
            b.disc(cx, h + 2, cz, 2.6, false, pack);
            b.disc(cx, h + 3, cz, 1.6, false, pack);
            b.set(cx, h + 4, cz, blue);
            b.set(cx, h + 5, cz, S("end_rod"));
            b.set(cx, h + 6, cz, S("end_rod"));
            b.set(cx, h, cz, lamp);
            b.found(cx - 3, cz - 3, cx + 3, cz + 3, pack);
        }
        // pinchos de hielo alrededor
        int[][] spikes = {{-11, 2}, {11, 2}, {-12, 8}, {12, 9}, {-11, 17}, {11, 17}, {-5, -3}, {5, -4}, {-9, -5}, {9, -5}};
        for (int[] s : spikes) {
            int h = 5 + b.r.nextInt(6);
            b.found(s[0], s[1], s[0] + 1, s[1] + 1, pack);
            b.box(s[0], 0, s[1], s[0] + 1, h, s[1] + 1, b.r.nextBoolean() ? pack : blue);
            b.box(s[0], h + 1, s[1], s[0], h + 4, s[1], blue);
            b.set(s[0], h + 5, s[1], S("snow"));
            b.set(s[0] + 1, h + 1, s[1] + 1, S("snow"));
        }
        // nieve sobre la plataforma
        for (int i = 0; i < 30; i++) {
            int x = b.r.nextInt(19) - 9;
            int z = b.r.nextInt(17);
            if (z >= 3 && z <= 15 && Math.abs(x) <= 6) {
                continue;
            }
            b.setIfAbsent(x, 1, z, S("snow"));
        }
        // decoraciones del mod
        b.set(-4, 1, 2, M("decor_aurora_lamp"));
        b.set(4, 1, 2, M("decor_aurora_lamp"));
        b.set(-7, 1, 9, M("decor_ice_spikes"));
        b.set(7, 1, 9, M("decor_ice_spikes"));
        b.set(-3, 1, -3, M("decor_aurora_fir"));
        b.set(3, 1, -3, M("decor_aurora_fir"));
        b.set(0, 1, 2, M("decor_snowman"));
    }

    // ------------------------------------------------------------------ 4. TORRE

    private static void torre(Bp b) {
        BlockState brick = S("stone_bricks");
        BlockState moss = S("mossy_stone_bricks");
        BlockState crack = S("cracked_stone_bricks");
        BlockState cobble = S("cobblestone");
        BlockState mcobble = S("mossy_cobblestone");

        b.found(-7, 0, 7, 14, cobble);
        b.air(-9, 0, -6, 9, 32, 15);
        b.mix(-9, -1, -6, 9, -1, -1, S("grass_block"), S("grass_block"), S("coarse_dirt"));
        b.mix(-1, -1, -6, 1, -1, -1, S("dirt_path"), cobble, S("gravel"));
        // patio
        b.mix(-7, 0, 0, 7, 0, 14, cobble, mcobble, brick, S("gravel"), moss);
        for (int x = -2; x <= 2; x++) {
            b.set(x, 0, 0, stair("stone_brick_stairs", Direction.SOUTH));
        }
        // la torre
        b.walls(-3, 1, 4, 3, 24, 10, brick, brick, moss, crack);
        b.air(-2, 1, 5, 2, 24, 9);
        for (int y : new int[] {8, 16}) {
            b.box(-2, y, 5, 2, y, 9, S("oak_planks"));
        }
        b.air(-1, 1, 4, 1, 4, 5);
        b.box(-2, 1, 4, -2, 5, 4, S("chiseled_stone_bricks"));
        b.box(2, 1, 4, 2, 5, 4, S("chiseled_stone_bricks"));
        b.box(-2, 5, 4, 2, 5, 4, S("chiseled_stone_bricks"));
        // ventanas estrechas con barrotes
        for (int y : new int[] {6, 11, 14, 19, 22}) {
            b.set(0, y, 4, S("iron_bars"));
            b.set(0, y, 10, S("iron_bars"));
            b.set(-3, y, 7, S("iron_bars"));
            b.set(3, y, 7, S("iron_bars"));
        }
        // almenas arriba, con partes rotas
        b.box(-4, 25, 3, 4, 25, 11, brick);
        b.air(-3, 25, 4, 3, 25, 10);
        for (int x = -4; x <= 4; x++) {
            for (int z = 3; z <= 11; z++) {
                boolean edge = x == -4 || x == 4 || z == 3 || z == 11;
                if (edge && (x + z) % 2 == 0 && b.r.nextInt(5) > 0) {
                    b.set(x, 26, z, brick);
                }
            }
        }
        b.air(2, 24, 10, 3, 27, 10);
        b.set(3, 24, 10, Blocks.AIR.defaultBlockState());
        // esquinas rotas
        b.air(-4, 25, 11, -3, 27, 11);
        // faroles y estandartes
        b.set(-3, 6, 3, S("stone_brick_wall"));
        b.set(-3, 7, 3, S("lantern"));
        b.set(3, 6, 3, S("stone_brick_wall"));
        b.set(3, 7, 3, S("lantern"));
        b.set(-2, 8, 3, wallBanner("red_wall_banner", Direction.NORTH));
        b.set(2, 8, 3, wallBanner("red_wall_banner", Direction.NORTH));
        b.set(0, 4, 5, hang("lantern"));
        // muralla baja en ruinas alrededor
        for (int x = -7; x <= 7; x++) {
            for (int z : new int[] {1, 13}) {
                if (z == 1 && Math.abs(x) <= 2) {
                    continue;
                }
                int h = 1 + b.r.nextInt(4);
                if (b.r.nextInt(5) == 0) {
                    h = 0;
                }
                for (int y = 1; y <= h; y++) {
                    b.set(x, y, z, b.r.nextBoolean() ? brick : moss);
                }
            }
        }
        for (int z = 2; z <= 12; z++) {
            for (int x : new int[] {-7, 7}) {
                int h = 1 + b.r.nextInt(4);
                if (b.r.nextInt(5) == 0) {
                    h = 0;
                }
                for (int y = 1; y <= h; y++) {
                    b.set(x, y, z, b.r.nextBoolean() ? brick : crack);
                }
            }
        }
        // torrecillas en las esquinas
        for (int sx : new int[] {-7, 6}) {
            for (int z : new int[] {1, 12}) {
                b.walls(sx, 1, z, sx + 1, 6, z + 1, brick, moss, crack);
                b.box(sx, 7, z, sx + 1, 7, z + 1, brick);
                b.set(sx, 8, z, S("stone_brick_wall"));
                b.set(sx + 1, 8, z + 1, S("lantern"));
            }
        }
        // campamento y escombros
        b.set(-4, 1, 3, S("campfire"));
        b.set(-5, 1, 3, log("spruce_log", Direction.Axis.X));
        b.set(-4, 1, 4, log("spruce_log", Direction.Axis.Z));
        b.set(4, 1, 3, S("barrel"));
        b.set(5, 1, 3, S("barrel"));
        b.set(4, 1, 4, S("hay_block"));
        for (int i = 0; i < 20; i++) {
            int x = b.r.nextInt(13) - 6;
            int z = b.r.nextInt(13) + 1;
            if (z >= 4 && z <= 10 && Math.abs(x) <= 3) {
                continue;
            }
            b.setIfAbsent(x, 1, z, b.r.nextBoolean() ? cobble : mcobble);
        }
    }
}
