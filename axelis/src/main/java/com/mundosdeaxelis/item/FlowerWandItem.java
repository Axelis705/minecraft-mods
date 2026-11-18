package com.mundosdeaxelis.item;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/** Varita de flores: una ola de pétalos hace crecer los cultivos y florecer el pasto. */
public class FlowerWandItem extends Item {
    private static final int RADIUS = 6;
    private static final Block[] FLOWERS = {
            Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.RED_TULIP,
            Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER,
            Blocks.LILY_OF_THE_VALLEY
    };

    public FlowerWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel sl) || !(ctx.getPlayer() instanceof ServerPlayer sp)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        BlockPos center = ctx.getClickedPos();
        ItemStack stack = ctx.getItemInHand();
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(ctx.getHand()));
        sp.getCooldowns().addCooldown(this, 30);
        RandomSource r = sl.getRandom();
        double cx = center.getX() + 0.5;
        double cy = center.getY() + 1.0;
        double cz = center.getZ() + 0.5;

        Fx.sound(sl, cx, cy, cz, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.5F);
        Fx.sound(sl, cx, cy, cz, SoundEvents.FLOWERING_AZALEA_PLACE, SoundSource.PLAYERS, 1.0F, 0.9F);
        Fx.screen(sp, ScreenFxPacket.PETALS, 0xFFA8CC, 70);

        // Ola que se expande desde el punto tocado.
        for (int ring = 1; ring <= RADIUS; ring++) {
            final int rr = ring;
            Scheduler.runLater(ring * 2, () -> {
                Fx.ring(sl, Fx.petal(Fx.rainbow(sl.getRandom().nextFloat() * 0.15F + 0.9F), 1.2F), cx, cy + 0.2, cz, rr, 10 + rr * 6, rr);
                Fx.ring(sl, Fx.spark(0xFFE3F0, 0.7F), cx, cy + 0.1, cz, rr, 6 + rr * 3, -rr);
            });
        }
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > RADIUS + 0.4) {
                    continue;
                }
                for (int dy = 2; dy >= -2; dy--) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState st = sl.getBlockState(p);
                    int delay = 2 + (int) (dist * 2.0);
                    if (st.getBlock() instanceof CropBlock) {
                        Scheduler.runLater(delay, () -> {
                            BoneMealItem.growCrop(new ItemStack(net.minecraft.world.item.Items.BONE_MEAL), sl, p);
                            sl.sendParticles(Fx.spark(0xB8FFB0, 0.9F), p.getX() + 0.5, p.getY() + 0.7, p.getZ() + 0.5, 3, 0.25, 0.2, 0.25, 0.0);
                        });
                        break;
                    }
                    if (st.is(BlockTags.DIRT) && sl.getBlockState(p.above()).isAir() && r.nextFloat() < 0.4F) {
                        final Block flower = FLOWERS[r.nextInt(FLOWERS.length)];
                        Scheduler.runLater(delay, () -> {
                            BlockState fs = flower.defaultBlockState();
                            if (sl.getBlockState(p.above()).isAir() && fs.canSurvive(sl, p.above())) {
                                sl.setBlock(p.above(), fs, 3);
                                sl.sendParticles(Fx.petal(0, 1.0F), p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, 0,
                                        (sl.getRandom().nextDouble() - 0.5) * 0.1, 0.12, (sl.getRandom().nextDouble() - 0.5) * 0.1, 1.0);
                                sl.sendParticles(Fx.spark(0xFFFFFF, 0.6F), p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 1, 0.2, 0.1, 0.2, 0.0);
                            }
                        });
                        break;
                    }
                }
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "flower_wand", 2);
    }
}
