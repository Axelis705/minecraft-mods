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
import com.casino.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Colocar una máquina entera (todos sus bloques) desde cualquier estructura: el casino clásico o las plantillas. */
public final class Machines {
    public static final Direction[] DIRS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private Machines() {
    }

    public static Block block(Mach k) {
        return switch (k) {
            case BLACKJACK -> ModBlocks.BLACKJACK.get();
            case POKER -> ModBlocks.POKER.get();
            case ROULETTE -> ModBlocks.ROULETTE.get();
            case SLOT -> ModBlocks.SLOT.get();
            case SCRATCH -> ModBlocks.SCRATCH_MACHINE.get();
            case CASHIER -> ModBlocks.CASHIER.get();
            case WHEEL -> ModBlocks.LUCKY_WHEEL.get();
            case PLINKO -> ModBlocks.PLINKO.get();
        };
    }

    /** El estado de un bloque de la máquina (f = hacia donde mira su frente en el mundo). */
    public static BlockState state(Mach k, Direction f, Cell c) {
        BlockState st = block(k).defaultBlockState();
        return switch (k) {
            case BLACKJACK -> st.setValue(BlackjackBlock.FACING, f).setValue(BlackjackBlock.CELL, c.a());
            case POKER -> st.setValue(PokerBlock.FACING, f).setValue(PokerBlock.CELL, c.a());
            case ROULETTE -> st.setValue(RouletteBlock.FACING, f).setValue(RouletteBlock.CELL, c.a());
            case SLOT -> st.setValue(SlotBlock.FACING, f).setValue(SlotBlock.HALF, c.a() == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
            case SCRATCH -> st.setValue(ScratchMachineBlock.FACING, f).setValue(ScratchMachineBlock.HALF, c.a() == 0 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
            case CASHIER -> st.setValue(CashierBlock.FACING, f);
            case WHEEL, PLINKO -> st.setValue(LuckyWheelBlock.FACING, f).setValue(LuckyWheelBlock.PX, c.a()).setValue(LuckyWheelBlock.PY, c.b());
        };
    }

    /**
     * Pone la máquina con su bloque principal en anchor. localDir es la dirección del frente en la plantilla (0 norte, 1 este, 2 sur,
     * 3 oeste) y rot el giro de la plantilla. Solo toca los bloques dentro de clip (la generación va por chunks).
     */
    public static void place(LevelAccessor level, BoundingBox clip, Mach k, int localDir, Rotation rot, BlockPos anchor) {
        Direction f = rot.rotate(DIRS[localDir & 3]);
        for (Cell c : CasinoPlan.cells(k, 0, 0, 0, localDir & 3)) {
            BlockPos pos = anchor.offset(StructureTemplate.transform(new BlockPos(c.x(), c.y(), c.z()), Mirror.NONE, rot, BlockPos.ZERO));
            if (clip == null || clip.isInside(pos)) {
                level.setBlock(pos, state(k, f, c), 2);
            }
        }
    }

    /** Hace aparecer al crupier de la mesa (si la máquina tiene uno). */
    public static void spawnDealer(ServerLevel level, BlockPos master) {
        var be = level.getBlockEntity(master);
        if (be instanceof com.casino.block.BlackjackBE b) {
            b.spawnDealer(level);
        } else if (be instanceof com.casino.block.PokerBE b) {
            b.spawnDealer(level);
        } else if (be instanceof com.casino.block.RouletteBE b) {
            b.spawnDealer(level);
        }
    }
}
