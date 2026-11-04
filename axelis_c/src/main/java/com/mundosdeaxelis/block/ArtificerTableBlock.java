package com.mundosdeaxelis.block;

import com.mundosdeaxelis.client.ClientBridge;
import com.mundosdeaxelis.table.TableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;

/** La Mesa del Artífice: aquí se refina la Esencia, se suben de nivel los objetos, se compran llaves y se consulta el Archivo. */
public class ArtificerTableBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);

    public ArtificerTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @javax.annotation.Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArtificerTableBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, new SimpleMenuProvider((id, inv, p) ->
                    new TableMenu(id, inv, ContainerLevelAccess.create(level, pos), new SimpleContainerData(4)),
                    Component.translatable("block.mundos_de_axelis.artificer_table")), buf -> {
            });
        }
        return InteractionResult.CONSUME;
    }

    /** Luces flotantes alrededor del cristal; son más intensas cuanto más alto es el nivel de mesa del jugador que mira. */
    @OnlyIn(Dist.CLIENT)
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        int lv = ClientBridge.tableLevel();
        if (r.nextInt(5) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME,
                    pos.getX() + 0.8, pos.getY() + 1.1, pos.getZ() + 0.79, 0.0, 0.0, 0.0);
        }
        int n = 1 + lv / 2;
        for (int i = 0; i < n; i++) {
            if (r.nextInt(3) != 0) {
                continue;
            }
            double a = r.nextDouble() * Math.PI * 2;
            double rad = 0.25 + r.nextDouble() * 0.45;
            double x = pos.getX() + 0.5 + Math.cos(a) * rad;
            double z = pos.getZ() + 0.5 + Math.sin(a) * rad;
            double y = pos.getY() + 1.15 + r.nextDouble() * 0.6;
            level.addParticle(lv >= 3 ? net.minecraft.core.particles.ParticleTypes.END_ROD : net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    x, y, z, 0.0, 0.015 + 0.01 * lv, 0.0);
        }
        if (lv >= 2 && r.nextInt(14) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5,
                    (r.nextDouble() - 0.5) * 0.2, 0.05, (r.nextDouble() - 0.5) * 0.2);
        }
    }
}
