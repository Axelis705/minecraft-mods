package com.mundosdeaxelis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Brasero del volcán (etapa 255), acertijo de fuego: clic derecho lo enciende y arde {@link #BURN} ticks (25 s). Si todos los
 * braseros del mismo canal que hay cerca arden a la vez, quedan encendidos para siempre (hasta que el volcán se reinicia) y se
 * abren las rejas de ese canal. En la sala de erupciones hay cuatro, separados por las filas de respiraderos.
 */
public class BraseroVolcanBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty SOLVED = BooleanProperty.create("solved");
    public static final IntegerProperty CHANNEL = RejaVolcanBlock.CHANNEL;
    public static final int BURN = 500;
    /** Cuánto se busca alrededor (en horizontal) a los demás braseros y a las rejas. */
    private static final int REACH = 34;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0), Block.box(1.0, 10.0, 1.0, 15.0, 14.0, 15.0));

    public BraseroVolcanBlock(Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any().setValue(LIT, false).setValue(SOLVED, false).setValue(CHANNEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT, SOLVED, CHANNEL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel sl = (ServerLevel) level;
        if (state.getValue(SOLVED)) {
            player.displayClientMessage(Component.translatable("message.mundos_de_axelis.brasero.solved"), true);
            return InteractionResult.CONSUME;
        }
        if (state.getValue(LIT)) {
            player.displayClientMessage(Component.translatable("message.mundos_de_axelis.brasero.burning"), true);
            return InteractionResult.CONSUME;
        }
        sl.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        sl.scheduleTick(pos, this, BURN);
        sl.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 14, 0.25, 0.2, 0.25, 0.04);
        int ch = state.getValue(CHANNEL);
        int[] lt = count(sl, pos, ch);
        if (lt[1] >= 2 && lt[0] >= lt[1]) {
            solve(sl, pos, ch);
        } else if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.brasero.lit", lt[0], lt[1], BURN / 20), true);
        }
        return InteractionResult.CONSUME;
    }

    /** {encendidos, total} de los braseros del canal cerca. */
    private static int[] count(ServerLevel level, BlockPos c, int ch) {
        int lit = 0, total = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -REACH; x <= REACH; x++) {
            for (int z = -REACH; z <= REACH; z++) {
                for (int y = -3; y <= 3; y++) {
                    p.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof BraseroVolcanBlock && s.getValue(CHANNEL) == ch) {
                        total++;
                        if (s.getValue(LIT)) {
                            lit++;
                        }
                    }
                }
            }
        }
        return new int[]{lit, total};
    }

    /** Todos arden a la vez: se quedan encendidos y se abren las rejas del canal. */
    private static void solve(ServerLevel level, BlockPos c, int ch) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -REACH; x <= REACH; x++) {
            for (int z = -REACH; z <= REACH; z++) {
                for (int y = -3; y <= 3; y++) {
                    p.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof BraseroVolcanBlock && s.getValue(CHANNEL) == ch) {
                        level.setBlock(p, s.setValue(LIT, true).setValue(SOLVED, true), Block.UPDATE_ALL);
                        level.sendParticles(ParticleTypes.LAVA, p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.0);
                    }
                }
            }
        }
        int opened = RejaVolcanBlock.setChannel(level, c, ch, REACH, 4, true);
        level.playSound(null, c, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.9F, 0.7F);
        for (ServerPlayer sp : level.players()) {
            if (sp.blockPosition().distSqr(c) < 48 * 48) {
                sp.displayClientMessage(Component.translatable(opened > 0 ? "message.mundos_de_axelis.brasero.open" : "message.mundos_de_axelis.brasero.all"), true);
            }
        }
    }

    /** Se apaga solo si no se resolvió el acertijo a tiempo. */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        if (state.getValue(LIT) && !state.getValue(SOLVED)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.9F);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.02);
        }
    }

    /** Lo deja apagado y sin resolver (al reiniciar el volcán). */
    public static void reset(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof BraseroVolcanBlock && (s.getValue(LIT) || s.getValue(SOLVED))) {
            level.setBlock(pos, s.setValue(LIT, false).setValue(SOLVED, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(LIT)) {
            if (r.nextInt(5) == 0) {
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 0.0, 0.01, 0.0);
            }
            return;
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.3 + r.nextDouble() * 0.4, pos.getY() + 0.95, pos.getZ() + 0.3 + r.nextDouble() * 0.4,
                    0.0, 0.02, 0.0);
        }
        if (r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0.0, 0.04, 0.0);
        }
        if (r.nextInt(8) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.8F,
                    0.9F + r.nextFloat() * 0.2F, false);
        }
    }
}
