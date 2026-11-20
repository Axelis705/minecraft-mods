package com.mundosdeaxelis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Fumarola: solo echa el humo (en el cliente). No guarda nada. */
public class FumarolaBlockEntity extends BlockEntity {
    public FumarolaBlockEntity(BlockPos pos, BlockState state) {
        super(com.mundosdeaxelis.registry.ModBlockEntities.FUMAROLA.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, FumarolaBlockEntity be) {
        RandomSource r = level.random;
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }
        // cada fumarola "respira": ratos de más vapor y ratos de menos
        double breath = 0.5 + 0.5 * Math.sin((level.getGameTime() + pos.getX() * 13L + pos.getZ() * 7L) * 0.05);
        if (r.nextFloat() < 0.08 + 0.14 * breath) {
            level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.4,
                    pos.getY() + 1.0, pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.4, 0.0, 0.05 + 0.03 * breath, 0.0);
        }
        if (r.nextFloat() < 0.12) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.3 + r.nextDouble() * 0.4, pos.getY() + 1.05, pos.getZ() + 0.3 + r.nextDouble() * 0.4,
                    0.0, 0.06 + r.nextDouble() * 0.05, 0.0);
        }
        if (r.nextFloat() < 0.04) {
            level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + r.nextDouble(), pos.getY() + 1.2, pos.getZ() + r.nextDouble(), 0.0, 0.02, 0.0);
        }
    }
}
