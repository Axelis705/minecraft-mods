package com.mundosdeaxelis.item;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pincel arcoíris: clic derecho en lana, concreto, vidrio, terracota o alfombra para pasar al siguiente color.
 * Agachado: pinta todo el conjunto conectado del mismo material.
 */
public class RainbowBrushItem extends Item {
    private static final Block[][] FAMILIES = {
            {Blocks.WHITE_WOOL, Blocks.ORANGE_WOOL, Blocks.MAGENTA_WOOL, Blocks.LIGHT_BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL,
                    Blocks.PINK_WOOL, Blocks.GRAY_WOOL, Blocks.LIGHT_GRAY_WOOL, Blocks.CYAN_WOOL, Blocks.PURPLE_WOOL, Blocks.BLUE_WOOL,
                    Blocks.BROWN_WOOL, Blocks.GREEN_WOOL, Blocks.RED_WOOL, Blocks.BLACK_WOOL},
            {Blocks.WHITE_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.YELLOW_CONCRETE,
                    Blocks.LIME_CONCRETE, Blocks.PINK_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.CYAN_CONCRETE,
                    Blocks.PURPLE_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.BROWN_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.RED_CONCRETE,
                    Blocks.BLACK_CONCRETE},
            {Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA,
                    Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA,
                    Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
                    Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA},
            {Blocks.WHITE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS,
                    Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS, Blocks.PINK_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS,
                    Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS,
                    Blocks.BROWN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS, Blocks.RED_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS},
            {Blocks.WHITE_CARPET, Blocks.ORANGE_CARPET, Blocks.MAGENTA_CARPET, Blocks.LIGHT_BLUE_CARPET, Blocks.YELLOW_CARPET,
                    Blocks.LIME_CARPET, Blocks.PINK_CARPET, Blocks.GRAY_CARPET, Blocks.LIGHT_GRAY_CARPET, Blocks.CYAN_CARPET,
                    Blocks.PURPLE_CARPET, Blocks.BLUE_CARPET, Blocks.BROWN_CARPET, Blocks.GREEN_CARPET, Blocks.RED_CARPET,
                    Blocks.BLACK_CARPET},
    };
    private static final int MAX_BLOCKS = 80;

    public RainbowBrushItem(Properties properties) {
        super(properties);
    }

    private static int family(Block b) {
        for (int f = 0; f < FAMILIES.length; f++) {
            for (Block c : FAMILIES[f]) {
                if (c == b) {
                    return f;
                }
            }
        }
        return -1;
    }

    private static int index(int fam, Block b) {
        for (int i = 0; i < 16; i++) {
            if (FAMILIES[fam][i] == b) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel sl) || !(ctx.getPlayer() instanceof ServerPlayer sp)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        BlockPos start = ctx.getClickedPos();
        BlockState st = sl.getBlockState(start);
        int fam = family(st.getBlock());
        if (fam < 0) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.brush_none"), true);
            return InteractionResult.FAIL;
        }
        int next = (index(fam, st.getBlock()) + 1) % 16;
        Block target = FAMILIES[fam][next];
        Block source = st.getBlock();
        Set<BlockPos> done = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        done.add(start);
        int count = 0;
        while (!queue.isEmpty() && count < (sp.isShiftKeyDown() ? MAX_BLOCKS : 1)) {
            BlockPos p = queue.poll();
            sl.setBlock(p, target.defaultBlockState(), 3);
            count++;
            int rgb = DyeColor.byId(next).getTextColor();
            sl.sendParticles(Fx.spark(rgb, 0.9F), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.45, 0.45, 0.45, 0.0);
            sl.sendParticles(Fx.dust(rgb, 0.8F), p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 0, 0.0, 0.08, 0.0, 1.0);
            if (sp.isShiftKeyDown()) {
                for (Direction d : Direction.values()) {
                    BlockPos n = p.relative(d);
                    if (!done.contains(n) && sl.getBlockState(n).getBlock() == source) {
                        done.add(n);
                        queue.add(n);
                    }
                }
            }
        }
        ItemStack stack = ctx.getItemInHand();
        stack.hurtAndBreak(sp.isShiftKeyDown() ? 2 : 1, sp, pl -> pl.broadcastBreakEvent(ctx.getHand()));
        Fx.sound(sl, start.getX() + 0.5, start.getY() + 0.5, start.getZ() + 0.5, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.8F + next * 0.05F);
        Fx.sound(sl, start.getX() + 0.5, start.getY() + 0.5, start.getZ() + 0.5, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 0.8F, 1.2F);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "rainbow_brush", 2);
    }
}
