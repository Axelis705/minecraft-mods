package com.casino.worldgen;

import com.casino.block.BlackjackBlock;
import com.casino.block.CashierBlock;
import com.casino.block.LuckyWheelBlock;
import com.casino.block.PokerBlock;
import com.casino.block.RouletteBlock;
import com.casino.block.ScratchMachineBlock;
import com.casino.block.SlotBlock;
import com.casino.game.CasinoPlan;
import com.casino.game.CasinoPlan.Cell;
import com.casino.game.CasinoPlan.Mach;
import com.casino.game.CasinoPlan.Mat;
import com.casino.registry.ModBlocks;
import com.casino.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.StructureManager;

/**
 * La única pieza del casino: construye todo el edificio (ver CasinoPlan) y lo recorta por chunk, así que se puede generar en pedazos
 * mientras el mundo se va creando. Primero limpia el aire encima del piso (árboles y lomas), rellena debajo hasta tocar suelo, y después
 * coloca paredes, techo, cartel y las máquinas con todos sus bloques.
 */
public class CasinoPiece extends StructurePiece {
    public static final int FOUNDATION = 6;
    private static final Direction[] DIRS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private final int rot;
    private final int y0;

    public CasinoPiece(int rot, int y0, BoundingBox box) {
        super(ModStructures.CASINO_PIECE.get(), 0, box);
        this.rot = rot;
        this.y0 = y0;
    }

    public CasinoPiece(CompoundTag tag) {
        super(ModStructures.CASINO_PIECE.get(), tag);
        this.rot = tag.getInt("Rot");
        this.y0 = tag.getInt("Y0");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("Rot", rot);
        tag.putInt("Y0", y0);
    }

    /** De coordenadas del plano (x, z) a coordenadas del mundo, dado el giro y la esquina mínima de la caja. */
    public static int[] toWorld(int rot, int minX, int minZ, int x, int z) {
        int w = CasinoPlan.W;
        int d = CasinoPlan.D;
        return switch (rot & 3) {
            case 0 -> new int[]{minX + x, minZ + z};
            case 1 -> new int[]{minX + d - 1 - z, minZ + x};
            case 2 -> new int[]{minX + w - 1 - x, minZ + d - 1 - z};
            default -> new int[]{minX + z, minZ + w - 1 - x};
        };
    }

    /** Construye el casino entero de una vez (para el comando /casino build): carga los chunks, coloca todo y hace aparecer a los crupieres. */
    public static void buildNow(net.minecraft.server.level.ServerLevel level, BlockPos corner, int rot) {
        boolean odd = (rot & 1) == 1;
        int sx = odd ? CasinoPlan.D : CasinoPlan.W;
        int sz = odd ? CasinoPlan.W : CasinoPlan.D;
        BoundingBox box = new BoundingBox(corner.getX(), corner.getY() - FOUNDATION, corner.getZ(), corner.getX() + sx - 1, corner.getY() + CasinoPlan.TOP_Y, corner.getZ() + sz - 1);
        CasinoPiece piece = new CasinoPiece(rot, corner.getY(), box);
        ChunkPos a = new ChunkPos(box.minX() >> 4, box.minZ() >> 4);
        ChunkPos z = new ChunkPos(box.maxX() >> 4, box.maxZ() >> 4);
        for (int cx = a.x; cx <= z.x; cx++) {
            for (int cz = a.z; cz <= z.z; cz++) {
                level.getChunk(cx, cz);
                BoundingBox cb = new BoundingBox(cx << 4, level.getMinBuildHeight(), cz << 4, (cx << 4) + 15, level.getMaxBuildHeight(), (cz << 4) + 15);
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), cb, new ChunkPos(cx, cz), BlockPos.ZERO);
            }
        }
        piece.spawnDealers(level);
    }

    private void spawnDealers(net.minecraft.server.level.ServerLevel level) {
        for (Object[] m : CasinoPlan.MACHINES) {
            BlockPos pos = world((int) m[1], 1, (int) m[2]);
            switch ((Mach) m[0]) {
                case BLACKJACK -> {
                    if (level.getBlockEntity(pos) instanceof com.casino.block.BlackjackBE be) {
                        be.spawnDealer(level);
                    }
                }
                case POKER -> {
                    if (level.getBlockEntity(pos) instanceof com.casino.block.PokerBE be) {
                        be.spawnDealer(level);
                    }
                }
                case ROULETTE -> {
                    if (level.getBlockEntity(pos) instanceof com.casino.block.RouletteBE be) {
                        be.spawnDealer(level);
                    }
                }
                default -> {
                }
            }
        }
    }

    private BlockPos world(int x, int y, int z) {
        int[] p = toWorld(rot, boundingBox.minX(), boundingBox.minZ(), x, z);
        return new BlockPos(p[0], y0 + y, p[1]);
    }

    private static BlockState block(Mat m) {
        return switch (m) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case FLOOR_A -> Blocks.BLACK_CONCRETE.defaultBlockState();
            case FLOOR_B -> Blocks.RED_CONCRETE.defaultBlockState();
            case PATH -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            case CARPET -> Blocks.RED_CARPET.defaultBlockState();
            case WALL, ROOF -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            case TRIM, LETTER -> Blocks.GOLD_BLOCK.defaultBlockState();
            case GLASS -> Blocks.RED_STAINED_GLASS.defaultBlockState();
            case LAMP -> Blocks.GLOWSTONE.defaultBlockState();
            case SIGN_BG -> Blocks.BLACK_CONCRETE.defaultBlockState();
            case LANTERN -> Blocks.LANTERN.defaultBlockState();
        };
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator gen, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        BoundingBox b = boundingBox;
        int minX = Math.max(b.minX(), chunkBox.minX());
        int maxX = Math.min(b.maxX(), chunkBox.maxX());
        int minZ = Math.max(b.minZ(), chunkBox.minZ());
        int maxZ = Math.min(b.maxZ(), chunkBox.maxZ());
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState fill = Blocks.STONE_BRICKS.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // aire limpio (árboles, lomas) sobre el piso
                for (int y = y0 + 1; y <= y0 + CasinoPlan.TOP_Y; y++) {
                    p.set(x, y, z);
                    if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, air, 2);
                    }
                }
                // relleno debajo hasta tocar suelo firme
                for (int y = y0; y >= y0 - FOUNDATION; y--) {
                    p.set(x, y, z);
                    BlockState cur = level.getBlockState(p);
                    if (y < y0 && cur.canOcclude() && cur.getFluidState().isEmpty() && !cur.is(net.minecraft.tags.BlockTags.LEAVES) && !cur.is(net.minecraft.tags.BlockTags.LOGS)) {
                        break;
                    }
                    level.setBlock(p, fill, 2);
                }
            }
        }
        CasinoPlan.build(new CasinoPlan.Sink() {
            @Override
            public void mat(int x, int y, int z, Mat m) {
                BlockPos pos = world(x, y, z);
                if (chunkBox.isInside(pos) && b.isInside(pos)) {
                    level.setBlock(pos, block(m), 2);
                }
            }

            @Override
            public void machine(Mach k, int x, int y, int z, int dir) {
                place(level, chunkBox, k, x, y, z, dir);
            }
        });
    }

    private void place(WorldGenLevel level, BoundingBox chunkBox, Mach k, int x, int y, int z, int dir) {
        Direction f = DIRS[(dir + rot) & 3];
        Block block = switch (k) {
            case BLACKJACK -> ModBlocks.BLACKJACK.get();
            case POKER -> ModBlocks.POKER.get();
            case ROULETTE -> ModBlocks.ROULETTE.get();
            case SLOT -> ModBlocks.SLOT.get();
            case SCRATCH -> ModBlocks.SCRATCH_MACHINE.get();
            case CASHIER -> ModBlocks.CASHIER.get();
            case WHEEL -> ModBlocks.LUCKY_WHEEL.get();
            case PLINKO -> ModBlocks.PLINKO.get();
        };
        for (Cell c : CasinoPlan.cells(k, x, y, z, dir)) {
            BlockPos pos = world(c.x(), c.y(), c.z());
            if (!chunkBox.isInside(pos) || !boundingBox.isInside(pos)) {
                continue;
            }
            BlockState st = block.defaultBlockState();
            switch (k) {
                case BLACKJACK -> st = st.setValue(BlackjackBlock.FACING, f).setValue(BlackjackBlock.CELL, c.a());
                case POKER -> st = st.setValue(PokerBlock.FACING, f).setValue(PokerBlock.CELL, c.a());
                case ROULETTE -> st = st.setValue(RouletteBlock.FACING, f).setValue(RouletteBlock.CELL, c.a());
                case SLOT -> st = st.setValue(SlotBlock.FACING, f).setValue(SlotBlock.HALF, c.a() == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
                case SCRATCH -> st = st.setValue(ScratchMachineBlock.FACING, f).setValue(ScratchMachineBlock.HALF, c.a() == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
                case CASHIER -> st = st.setValue(CashierBlock.FACING, f);
                case WHEEL, PLINKO -> st = st.setValue(LuckyWheelBlock.FACING, f).setValue(LuckyWheelBlock.PX, c.a()).setValue(LuckyWheelBlock.PY, c.b());
            }
            level.setBlock(pos, st, 2);
        }
    }
}
