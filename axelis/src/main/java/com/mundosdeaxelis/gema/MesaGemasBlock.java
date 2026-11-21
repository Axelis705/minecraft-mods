package com.mundosdeaxelis.gema;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Mesa de Gemas (etapa 271): se crea en la Mesa del Artífice; aquí se infunden las gemas vacías y se engarzan en las armas. */
public class MesaGemasBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 13, 16);

    public MesaGemasBlock(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            com.mundosdeaxelis.table.Essence.sync(sp); // la pantalla muestra la Esencia al día
            net.minecraftforge.network.NetworkHooks.openScreen(sp, new SimpleMenuProvider((id, inv, p) ->
                    new GemTableMenu(id, inv, ContainerLevelAccess.create(level, pos)), Component.translatable("block.mundos_de_axelis.mesa_gemas")));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack st, @javax.annotation.Nullable BlockGetter level, java.util.List<Component> tip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.mundos_de_axelis.mesa_gemas").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState st, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5,
                    (r.nextDouble() - 0.5) * 1.2, -0.3 - r.nextDouble() * 0.4, (r.nextDouble() - 0.5) * 1.2);
        }
    }
}
