package com.celular;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La luz de la linterna: un bloque invisible, sin choque, que cuenta como aire y alumbra. El servidor lo mueve a donde apuntas;
 * si alguna se queda olvidada (el servidor se cerró de golpe, por ejemplo), se borra sola con el tiempo.
 */
public class FlashLightBlock extends Block {
    public FlashLightBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!PhoneUse.lightInUse(level, pos)) {
            level.removeBlock(pos, false);
        }
    }
}
