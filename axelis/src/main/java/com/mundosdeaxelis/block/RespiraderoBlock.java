package com.mundosdeaxelis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

/**
 * Respiradero volcánico (dungeon del volcán): erupciones con aviso. Pasa un rato tranquilo, luego avisa (humo, chispas y un
 * retumbar de 1,5 s) y escupe una columna de fuego de 5 bloques que quema y lanza hacia arriba a quien esté encima. Cada
 * respiradero lleva su propio ritmo (los de una misma fila se pueden sincronizar con el desfase del bloque).
 * <p>Fases: 0 tranquilo, 1 aviso, 2 a 7 erupción (seis tramos de 4 ticks).
 */
public class RespiraderoBlock extends Block {
    public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 7);
    private static final int WARN_TICKS = 30;

    public RespiraderoBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any().setValue(PHASE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(PHASE);
    }

    /** Tiempo tranquilo: entre 4 y 7 s, distinto según la posición para que no erupcionen todos a la vez. */
    private static int idleTicks(BlockPos pos, RandomSource r) {
        return 80 + Math.floorMod(pos.getX() * 7 + pos.getZ() * 13, 4) * 15 + r.nextInt(20);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        // solo al ponerlo (no cuando cambia de fase él mismo: el tick ya programa el siguiente paso)
        if (!level.isClientSide && !old.is(this) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, idleTicks(pos, level.random));
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        // red de seguridad: si se perdió su ciclo (p. ej. puesto sin actualizar), lo retoma
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        int ph = state.getValue(PHASE);
        if (ph == 0) {
            // solo trabaja si hay jugadores cerca (no gasta nada en un volcán vacío)
            if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 32.0, false) == null) {
                level.scheduleTick(pos, this, 60);
                return;
            }
            level.setBlock(pos, state.setValue(PHASE, 1), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, WARN_TICKS);
            level.playSound(null, pos, SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 1.4F, 0.6F);
            level.playSound(null, pos, SoundEvents.BASALT_STEP, SoundSource.BLOCKS, 1.0F, 0.5F);
        } else if (ph == 1) {
            level.setBlock(pos, state.setValue(PHASE, 2), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, 4);
            level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.4F, 0.5F);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.2F, 0.6F);
            erupt(level, pos);
        } else if (ph < 7) {
            level.setBlock(pos, state.setValue(PHASE, ph + 1), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, 4);
            erupt(level, pos);
        } else {
            erupt(level, pos);
            level.setBlock(pos, state.setValue(PHASE, 0), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, idleTicks(pos, r));
        }
    }

    /** Un tramo de la erupción: llamas, ceniza y daño a quien esté en la columna. */
    private static void erupt(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, 20, 0.25, 1.4, 0.25, 0.04);
        level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 5.0, pos.getZ() + 0.5, 3, 0.3, 0.4, 0.3, 0.02);
        AABB col = new AABB(pos.above()).expandTowards(0, 4, 0).inflate(0.35, 0, 0.35);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, col)) {
            if (e.fireImmune() || e instanceof Player p && (p.isCreative() || p.isSpectator())) {
                continue;
            }
            e.hurt(level.damageSources().hotFloor(), 5.0F);
            e.setSecondsOnFire(4);
            e.setDeltaMovement(e.getDeltaMovement().x * 0.5, Math.max(e.getDeltaMovement().y, 0.75), e.getDeltaMovement().z * 0.5);
            e.hurtMarked = true;
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        int ph = state.getValue(PHASE);
        double x = pos.getX() + 0.5, y = pos.getY() + 1.02, z = pos.getZ() + 0.5;
        if (ph == 1) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, x + (r.nextDouble() - 0.5) * 0.6, y, z + (r.nextDouble() - 0.5) * 0.6, 0.0, 0.06, 0.0);
            }
            level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0, 0.0, 0.0);
        } else if (ph >= 2) {
            for (int i = 0; i < 6; i++) {
                level.addParticle(ParticleTypes.FLAME, x + (r.nextDouble() - 0.5) * 0.5, y, z + (r.nextDouble() - 0.5) * 0.5, 0.0, 0.35 + r.nextDouble() * 0.2, 0.0);
            }
        } else if (r.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x + (r.nextDouble() - 0.5) * 0.5, y, z + (r.nextDouble() - 0.5) * 0.5, 0.0, 0.03, 0.0);
        }
    }
}
