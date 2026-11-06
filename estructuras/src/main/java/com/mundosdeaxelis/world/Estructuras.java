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
import net.minecraft.world.level.block.state.properties.Property;
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
    /**
     * Solo avisa a los jugadores: sin avisar a los vecinos (16) y sin soltar objetos (32). Con solo "2", cada bloque que se coloca
     * revisa a sus vecinos y las antorchas, farolas, alfombras o trampillas que todavía no tenían su apoyo se rompían y dejaban
     * objetos tirados por todo el suelo.
     */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Map<UUID, Deque<Undo>> UNDO = new HashMap<>();

    private Estructuras() {
    }

    private enum Kind {
        BOSQUE("bosque", "Gran templo del bosque"),
        FORJA("forja", "Gran fundición de la forja"),
        HIELO("hielo", "Ciudadela de cristal"),
        TORRE("torre", "Fortaleza en ruinas");

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
        BlockPos origin = feet.relative(fwd, 16);
        StructBp bp = StructDesigns.make(kind.id);
        Rotation r = rotationFor(fwd);
        Undo undo = new Undo(level.dimension(), kind.title);
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        // cimientos: columnas hacia abajo hasta tocar suelo firme
        for (StructBp.Found f : bp.found) {
            for (int x = f.x0(); x <= f.x1(); x++) {
                for (int z = f.z0(); z <= f.z1(); z++) {
                    BlockPos base = origin.offset(new BlockPos(x, 0, z).rotate(r));
                    for (int y = origin.getY() - 1; y > origin.getY() - 1 - 24 && y >= minY; y--) {
                        BlockPos p = new BlockPos(base.getX(), y, base.getZ());
                        BlockState cur = level.getBlockState(p);
                        boolean soft = cur.isAir() || !cur.getFluidState().isEmpty() || cur.canBeReplaced() || cur.is(BlockTags.LEAVES) || cur.is(BlockTags.LOGS);
                        if (!soft) {
                            break;
                        }
                        BlockState fs = state(f.state());
                        if (fs != null) {
                            put(level, undo, p, fs.rotate(r));
                        }
                    }
                }
            }
        }
        // el plano, de abajo hacia arriba (para que lo que cuelga o se apoya encuentre su apoyo)
        List<Map.Entry<Long, String>> entries = new ArrayList<>(bp.m.entrySet());
        entries.sort((a1, b1) -> Integer.compare(StructBp.ky(a1.getKey()), StructBp.ky(b1.getKey())));
        List<BlockPos> reshape = new ArrayList<>();
        for (Map.Entry<Long, String> e : entries) {
            long key = e.getKey();
            BlockPos local = new BlockPos(StructBp.kx(key), StructBp.ky(key), StructBp.kz(key));
            BlockPos p = origin.offset(local.rotate(r));
            if (p.getY() < minY || p.getY() >= maxY) {
                continue;
            }
            BlockState st0 = state(e.getValue());
            if (st0 == null) {
                continue;
            }
            BlockState st = st0.rotate(r);
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

    private static final Map<String, BlockState> CACHE = new HashMap<>();
    private static final BlockState NONE = Blocks.BARRIER.defaultBlockState();

    /** Convierte un texto como {@code oak_stairs[facing=north,half=bottom]} en un bloque; devuelve null si ese bloque no existe. */
    private static BlockState state(String spec) {
        BlockState c = CACHE.get(spec);
        if (c != null) {
            return c == NONE ? null : c;
        }
        String name = spec;
        String props = "";
        int k = spec.indexOf('[');
        if (k >= 0) {
            name = spec.substring(0, k);
            props = spec.substring(k + 1, spec.length() - 1);
        }
        ResourceLocation rl = name.indexOf(':') >= 0 ? new ResourceLocation(name) : new ResourceLocation("minecraft", name);
        BlockState st = null;
        if (ForgeRegistries.BLOCKS.containsKey(rl)) {
            st = ForgeRegistries.BLOCKS.getValue(rl).defaultBlockState();
            for (String kv : props.split(",")) {
                int e = kv.indexOf('=');
                if (e > 0) {
                    Property<?> p = st.getBlock().getStateDefinition().getProperty(kv.substring(0, e).trim());
                    if (p != null) {
                        st = withProp(st, p, kv.substring(e + 1).trim());
                    }
                }
            }
        }
        CACHE.put(spec, st == null ? NONE : st);
        return st;
    }

    private static <T extends Comparable<T>> BlockState withProp(BlockState s, Property<T> p, String v) {
        return p.getValue(v).map(x -> s.setValue(p, x)).orElse(s);
    }
}
