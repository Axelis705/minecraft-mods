package com.navidad.bloque;

import com.navidad.Navidad;
import com.navidad.entidad.Krampus;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * El altar del Krampus (en el centro de su guarida): un pedestal de piedra negra con cadenas y un tazón de carbón. De noche, si le das
 * la campana del Krampus o 16 carbones, lo llama. De día solo humea.
 */
public class AltarKrampus extends Block {
    private static final VoxelShape FORMA = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(4, 3, 4, 12, 11, 12), Block.box(1, 11, 1, 15, 15, 15));
    public static final int CARBONES = 16;

    public AltarKrampus(Properties p) {
        super(p);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return FORMA;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level l, BlockPos pos, Player p, InteractionHand h, BlockHitResult hit) {
        if (l.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel sl = (ServerLevel) l;
        ItemStack st = p.getItemInHand(h);
        boolean campana = st.is(ModRegistry.CAMPANA_KRAMPUS.get());
        boolean carbon = st.is(Items.COAL) && st.getCount() >= CARBONES || st.is(Items.COAL_BLOCK) && st.getCount() >= 2;
        if (!campana && !carbon) {
            p.displayClientMessage(Component.translatable("navidad.altar.ayuda", CARBONES).withStyle(ChatFormatting.DARK_RED), true);
            return InteractionResult.CONSUME;
        }
        long hora = sl.getDayTime() % 24000L;
        if (hora < 13000 || hora > 23000) {
            p.displayClientMessage(Component.translatable("navidad.altar.dia").withStyle(ChatFormatting.GRAY), true);
            sl.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.01);
            return InteractionResult.CONSUME;
        }
        if (!sl.getEntitiesOfClass(Krampus.class, new AABB(pos).inflate(64)).isEmpty()) {
            p.displayClientMessage(Component.translatable("navidad.altar.ya").withStyle(ChatFormatting.DARK_RED), true);
            return InteractionResult.CONSUME;
        }
        if (!p.getAbilities().instabuild) {
            st.shrink(campana ? 1 : st.is(Items.COAL_BLOCK) ? 2 : CARBONES);
        }
        llamar(sl, pos, p);
        return InteractionResult.CONSUME;
    }

    /** Llama al Krampus a unos 8 bloques del altar, del lado contrario de quien lo llamó. */
    public static Krampus llamar(ServerLevel sl, BlockPos pos, Player p) {
        sl.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2F, 0.4F);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 30, 0.3, 0.2, 0.3, 0.03);
        Vec3 d = p == null ? new Vec3(0, 0, 1) : new Vec3(pos.getX() + 0.5 - p.getX(), 0, pos.getZ() + 0.5 - p.getZ());
        d = d.lengthSqr() < 1.0E-3 ? new Vec3(0, 0, 1) : d.normalize();
        int x = (int) Math.floor(pos.getX() + 0.5 + d.x * 7), z = (int) Math.floor(pos.getZ() + 0.5 + d.z * 7);
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (Math.abs(y - pos.getY()) > 5) {
            y = pos.getY();
        }
        Navidad.LOGGER.info("NAVIDAD_ALTAR llama al Krampus en {}", pos.toShortString());
        return Krampus.llamar(sl, new Vec3(x + 0.5, y, z + 0.5), p, true);
    }

    @Override
    public void animateTick(BlockState s, Level l, BlockPos pos, RandomSource r) {
        if (r.nextInt(2) == 0) {
            l.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.3 + r.nextDouble() * 0.4, pos.getY() + 0.95, pos.getZ() + 0.3 + r.nextDouble() * 0.4,
                    0, 0.02, 0);
        }
        if (r.nextInt(5) == 0) {
            l.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 0, 0.01, 0);
        }
    }
}
