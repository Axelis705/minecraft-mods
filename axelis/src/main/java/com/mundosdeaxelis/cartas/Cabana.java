package com.mundosdeaxelis.cartas;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.Container;

/**
 * Una cabanita de troncos para grabar la apertura de sobres: mesita con los sobres encima, chimenea, banca, cama, libreros,
 * fogata afuera con troncos para sentarse, farolitos, flores y un perro sentado junto al fuego.
 *
 * Se construye en coordenadas locales (x = a tu derecha, z = hacia adelante, y = altura sobre tus pies) mirando hacia donde
 * miraba el jugador; la puerta queda de frente a el.
 */
public final class Cabana {
    private final ServerLevel lv;
    private final BlockPos o;
    private final Direction f;
    private final Direction r;

    private Cabana(ServerLevel lv, BlockPos origin, Direction facing) {
        this.lv = lv;
        this.o = origin;
        this.f = facing;
        this.r = facing.getClockWise();
    }

    /** Construye la cabana frente al jugador. Devuelve un texto con lo que hizo. */
    public static String build(ServerPlayer sp) {
        return build(sp.serverLevel(), sp.blockPosition(), sp.getDirection(), sp);
    }

    /** Igual, pero en un lugar y direccion dados; {@code owner} puede ser null (el perro queda sin dueno). */
    public static String build(ServerLevel lv, BlockPos origin, Direction face, ServerPlayer owner) {
        Cabana c = new Cabana(lv, origin, face);
        c.load();
        c.terrain();
        c.shell();
        c.roof();
        c.fireplace();
        c.furniture();
        c.yard();
        c.entities(owner);
        return "Cabaña construida frente a ti (la puerta mira hacia ti). El cofre de la derecha trae sobres de todos los tipos.";
    }

    // ------------------------------------------------------------ ayudas de coordenadas

    /** Carga los trozos del mundo que va a tocar la construccion. */
    private void load() {
        BlockPos a = at(-11, 0, -6);
        BlockPos b = at(11, 0, 18);
        for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++) {
            for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) {
                lv.getChunk(cx, cz);
            }
        }
    }

    /** Para la prueba automatica: comprueba que quedo lo importante (puerta, fuego, cofre con sobres, marcos y perro). */
    public static String check(ServerLevel lv, BlockPos origin, Direction face) {
        Cabana c = new Cabana(lv, origin, face);
        List<String> bad = new ArrayList<>();
        if (!(lv.getBlockState(c.at(0, 0, 4)).getBlock() instanceof DoorBlock)) {
            bad.add("puerta");
        }
        if (!lv.getBlockState(c.at(0, 0, 12)).is(Blocks.CAMPFIRE)) {
            bad.add("fogata de la chimenea");
        }
        if (!lv.getBlockState(c.at(8, 0, 5)).is(Blocks.CAMPFIRE)) {
            bad.add("fogata de afuera");
        }
        if (!(lv.getBlockEntity(c.at(4, 0, 5)) instanceof Container ch) || ch.getItem(0).isEmpty()) {
            bad.add("cofre con sobres");
        }
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(c.at(-8, 0, 0)).minmax(new net.minecraft.world.phys.AABB(c.at(8, 6, 14)));
        if (lv.getEntitiesOfClass(ItemFrame.class, box).size() < 6) {
            bad.add("marcos");
        }
        if (lv.getEntitiesOfClass(Wolf.class, box).isEmpty()) {
            bad.add("perro");
        }
        if (!lv.getBlockState(c.at(0, 9, 8)).is(Blocks.DARK_OAK_PLANKS)) {
            bad.add("techo");
        }
        return bad.isEmpty() ? "" : String.join(", ", bad);
    }

    private BlockPos at(int x, int y, int z) {
        return o.offset(r.getStepX() * x + f.getStepX() * z, y, r.getStepZ() * x + f.getStepZ() * z);
    }

    /** Una direccion local (NORTH = hacia ti, SOUTH = hacia el fondo, EAST = derecha, WEST = izquierda) en direccion del mundo. */
    private Direction w(Direction local) {
        return switch (local) {
            case NORTH -> f.getOpposite();
            case SOUTH -> f;
            case EAST -> r;
            default -> r.getOpposite();
        };
    }

    private Direction.Axis axisX() {
        return r.getAxis();
    }

    private Direction.Axis axisZ() {
        return f.getAxis();
    }

    private void set(int x, int y, int z, BlockState s) {
        lv.setBlock(at(x, y, z), s, 3);
    }

    private void set(int x, int y, int z, Block b) {
        set(x, y, z, b.defaultBlockState());
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, b);
                }
            }
        }
    }

    private BlockState stairs(Block b, Direction localFacing, boolean top) {
        return b.defaultBlockState().setValue(StairBlock.FACING, w(localFacing)).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private BlockState log(Block b, Direction.Axis axis) {
        return b.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    // ------------------------------------------------------------ terreno

    private boolean replaceable(BlockPos p) {
        BlockState s = lv.getBlockState(p);
        return s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty();
    }

    private void terrain() {
        for (int x = -10; x <= 10; x++) {
            for (int z = -5; z <= 17; z++) {
                for (int y = 14; y >= 0; y--) {
                    set(x, y, z, Blocks.AIR);
                }
                set(x, -1, z, Blocks.GRASS_BLOCK);
                for (int y = -2; y >= -9; y--) {
                    if (replaceable(at(x, y, z))) {
                        set(x, y, z, Blocks.DIRT);
                    } else {
                        break;
                    }
                }
            }
        }
        // caminito de tierra hasta la puerta
        for (int z = -5; z <= 3; z++) {
            for (int x = -1; x <= 1; x++) {
                set(x, -1, z, Blocks.DIRT_PATH);
            }
        }
    }

    // ------------------------------------------------------------ paredes

    private void shell() {
        // piso
        fill(-5, -1, 4, 5, -1, 12, Blocks.SPRUCE_PLANKS);
        fill(-5, -2, 4, 5, -2, 12, Blocks.DIRT);
        // base de piedra y paredes de troncos (y = 0 piedra, 1..3 troncos)
        for (int x = -5; x <= 5; x++) {
            for (int z : new int[] {4, 12}) {
                set(x, 0, z, Blocks.COBBLESTONE);
                for (int y = 1; y <= 3; y++) {
                    set(x, y, z, log(Blocks.SPRUCE_LOG, axisX()));
                }
            }
        }
        for (int z = 4; z <= 12; z++) {
            for (int x : new int[] {-5, 5}) {
                set(x, 0, z, Blocks.COBBLESTONE);
                for (int y = 1; y <= 3; y++) {
                    set(x, y, z, log(Blocks.SPRUCE_LOG, axisZ()));
                }
            }
        }
        // esquinas verticales
        for (int x : new int[] {-5, 5}) {
            for (int z : new int[] {4, 12}) {
                for (int y = 0; y <= 3; y++) {
                    set(x, y, z, y == 0 ? Blocks.COBBLESTONE.defaultBlockState() : log(Blocks.STRIPPED_SPRUCE_LOG, Direction.Axis.Y));
                }
            }
        }
        // interior despejado
        for (int x = -4; x <= 4; x++) {
            for (int z = 5; z <= 11; z++) {
                for (int y = 0; y <= 3; y++) {
                    set(x, y, z, Blocks.AIR);
                }
            }
        }
        // ventanas
        for (int y = 1; y <= 2; y++) {
            for (int x : new int[] {-3, -2, 2, 3}) {
                set(x, y, 4, Blocks.GLASS_PANE);
            }
            for (int x : new int[] {-3, 3}) {
                set(x, y, 12, Blocks.GLASS_PANE);
            }
            for (int z : new int[] {9, 10}) {
                set(-5, y, z, Blocks.GLASS_PANE);
            }
            for (int z : new int[] {6, 7}) {
                set(5, y, z, Blocks.GLASS_PANE);
            }
        }
        // puerta (de frente a ti)
        Direction out = w(Direction.NORTH);
        BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, out).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.OPEN, false);
        set(0, 0, 4, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(0, 1, 4, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // porche: tablones, postes con farolito
        fill(-2, -1, 1, 2, -1, 3, Blocks.SPRUCE_PLANKS);
        for (int x : new int[] {-2, 2}) {
            for (int z : new int[] {1, 3}) {
                set(x, 0, z, Blocks.SPRUCE_FENCE);
                set(x, 1, z, Blocks.LANTERN);
            }
            set(x, 0, 2, Blocks.SPRUCE_FENCE);
        }
        // farolitos del camino
        for (int x : new int[] {-2, 2}) {
            set(x, 0, -3, Blocks.SPRUCE_FENCE);
            set(x, 1, -3, Blocks.LANTERN);
        }
    }

    // ------------------------------------------------------------ techo

    private void roof() {
        BlockState roofStairL = stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST, false);
        BlockState roofStairR = stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST, false);
        for (int z = 3; z <= 13; z++) {
            for (int k = 0; k <= 5; k++) {
                set(-6 + k, 4 + k, z, roofStairL);
                set(6 - k, 4 + k, z, roofStairR);
            }
            for (int k = 0; k <= 4; k++) {
                set(-(5 - k), 4 + k, z, Blocks.DARK_OAK_PLANKS);
                set(5 - k, 4 + k, z, Blocks.DARK_OAK_PLANKS);
            }
            set(0, 9, z, Blocks.DARK_OAK_PLANKS);
        }
        // triangulos de frente y fondo
        for (int y = 4; y <= 8; y++) {
            int half = 4 - (y - 4);
            for (int x = -half; x <= half; x++) {
                set(x, y, 4, Blocks.SPRUCE_PLANKS);
                set(x, y, 12, Blocks.SPRUCE_PLANKS);
            }
        }
        // ventanita redonda de la fachada
        set(0, 6, 4, Blocks.GLASS_PANE);
        // vigas
        for (int z = 5; z <= 10; z++) {
            set(0, 3, z, log(Blocks.SPRUCE_LOG, axisZ()));
        }
        for (int x = -4; x <= 4; x++) {
            set(x, 3, 6, log(Blocks.SPRUCE_LOG, axisX()));
            set(x, 3, 9, log(Blocks.SPRUCE_LOG, axisX()));
        }
    }

    // ------------------------------------------------------------ chimenea

    private void fireplace() {
        for (int y = 0; y <= 11; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = 11; z <= 13; z++) {
                    set(x, y, z, y < 2 ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS);
                }
            }
        }
        // hueco del fuego y tiro de la chimenea
        set(0, 0, 12, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        for (int y = 1; y <= 11; y++) {
            set(0, y, 12, Blocks.AIR);
        }
        set(0, 0, 11, Blocks.AIR);
        set(0, 1, 11, Blocks.AIR);
        set(0, 0, 12, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        // repisa
        set(-1, 2, 10, stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
        set(0, 2, 10, stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
        set(1, 2, 10, stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
        // piso de piedra frente al fuego
        fill(-2, -1, 10, 2, -1, 11, Blocks.STONE_BRICKS);
    }

    // ------------------------------------------------------------ muebles

    private void furniture() {
        // alfombra roja bajo la mesa
        for (int x = -2; x <= 2; x++) {
            for (int z = 5; z <= 9; z++) {
                set(x, 0, z, Blocks.RED_CARPET);
            }
        }
        // la mesita (losas arriba) con una maceta y un farolito
        BlockState top = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        for (int x = -1; x <= 1; x++) {
            for (int z = 7; z <= 8; z++) {
                set(x, 0, z, top);
            }
        }
        set(-1, 1, 7, Blocks.POTTED_POPPY);
        set(1, 1, 7, Blocks.LANTERN);
        // banca de tres lugares de tu lado y dos banquitos enfrente
        for (int x = -1; x <= 1; x++) {
            set(x, 0, 6, stairs(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
        }
        set(-1, 0, 9, stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
        set(1, 0, 9, stairs(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
        // farolitos colgando de la viga
        for (int z : new int[] {6, 9}) {
            set(0, 2, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        // libreros
        for (int z = 5; z <= 7; z++) {
            set(-4, 0, z, Blocks.BOOKSHELF);
            set(-4, 1, z, Blocks.BOOKSHELF);
        }
        // cama roja
        Direction bedDir = w(Direction.SOUTH);
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, bedDir);
        lv.setBlock(at(-4, 0, 9), bed.setValue(BedBlock.PART, BedPart.FOOT), 3);
        lv.setBlock(at(-4, 0, 10), bed.setValue(BedBlock.PART, BedPart.HEAD), 3);
        // camita del perro
        for (int x = -3; x <= -2; x++) {
            for (int z = 10; z <= 11; z++) {
                set(x, 0, z, Blocks.BROWN_CARPET);
            }
        }
        // cofre con sobres
        set(4, 0, 5, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, w(Direction.WEST)));
        BlockEntity be = lv.getBlockEntity(at(4, 0, 5));
        if (be instanceof Container ch) {
            int slot = 0;
            for (PackDef p : Cards.PACKS) {
                for (int i = 0; i < 2 && slot < ch.getContainerSize(); i++) {
                    ch.setItem(slot++, CartasItems.pack(p, 16));
                }
            }
        }
    }

    // ------------------------------------------------------------ jardin y fogata

    private void yard() {
        // fogata con troncos para sentarse
        set(8, 0, 5, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        set(8, -1, 5, Blocks.DIRT);
        set(8, 0, 3, log(Blocks.STRIPPED_OAK_LOG, axisX()));
        set(8, 0, 7, log(Blocks.STRIPPED_OAK_LOG, axisX()));
        set(10, 0, 5, log(Blocks.STRIPPED_OAK_LOG, axisZ()));
        set(6, 0, 5, log(Blocks.STRIPPED_OAK_LOG, axisZ()));
        for (int x = 7; x <= 9; x++) {
            for (int z = 4; z <= 6; z++) {
                if (x != 8 || z != 5) {
                    set(x, -1, z, Blocks.COARSE_DIRT);
                }
            }
        }
        // arbustos
        int[][] bushes = {{-8, 2}, {-9, 3}, {-8, 9}, {-9, 10}, {9, 11}, {8, 12}, {-7, 15}, {7, 15}, {0, 15}};
        for (int[] b : bushes) {
            set(b[0], 0, b[1], Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
            if ((b[0] + b[1]) % 2 == 0) {
                set(b[0], 1, b[1], Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
            }
        }
        // flores y pasto
        Block[] plants = {Blocks.POPPY, Blocks.DANDELION, Blocks.AZURE_BLUET, Blocks.CORNFLOWER, Blocks.FERN, Blocks.GRASS, Blocks.GRASS};
        Random rnd = new Random(o.asLong());
        for (int x = -10; x <= 10; x++) {
            for (int z = -5; z <= 17; z++) {
                boolean inCabin = x >= -7 && x <= 7 && z >= 0 && z <= 14;
                boolean campArea = x >= 5 && x <= 11 && z >= 2 && z <= 8;
                boolean path = Math.abs(x) <= 2 && z <= 3;
                if (inCabin && !(x < -5 || x > 5) && z >= 0 || campArea || path) {
                    continue;
                }
                if (lv.getBlockState(at(x, 0, z)).isAir() && lv.getBlockState(at(x, -1, z)).is(Blocks.GRASS_BLOCK) && rnd.nextInt(100) < 14) {
                    set(x, 0, z, plants[rnd.nextInt(plants.length)]);
                }
            }
        }
    }

    // ------------------------------------------------------------ entidades

    private void entities(ServerPlayer sp) {
        // el perro, sentado junto al fuego
        Wolf wolf = EntityType.WOLF.create(lv);
        if (wolf != null) {
            BlockPos p = at(-2, 0, 11);
            wolf.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, w(Direction.SOUTH).toYRot(), 0f);
            if (sp != null) {
                wolf.tame(sp);
            }
            wolf.setOrderedToSit(true);
            wolf.setInSittingPose(true);
            wolf.setCollarColor(DyeColor.RED);
            wolf.setCustomName(Component.literal("Canela"));
            wolf.setPersistenceRequired();
            lv.addFreshEntity(wolf);
        }
        // los tres sobres sobre la mesa
        List<PackDef> packs = new ArrayList<>(Cards.PACKS);
        for (int i = 0; i < 3 && i < packs.size(); i++) {
            frame(at(-1 + i, 1, 8), Direction.UP, CartasItems.pack(packs.get(i), 1));
        }
        // las tres cartas mas raras en la pared derecha
        List<CardDef> best = new ArrayList<>(Cards.ALL);
        best.sort((a, b) -> Integer.compare(b.rarity.ordinal(), a.rarity.ordinal()));
        Direction wallIn = w(Direction.WEST);
        for (int i = 0; i < 3 && i < best.size(); i++) {
            frame(at(4, 1, 8 + i), wallIn, CartasItems.stack(best.get(i), i == 0, 1));
        }
    }

    private void frame(BlockPos p, Direction dir, ItemStack item) {
        ItemFrame fr = new ItemFrame(lv, p, dir);
        fr.setItem(item);
        lv.addFreshEntity(fr);
    }
}
