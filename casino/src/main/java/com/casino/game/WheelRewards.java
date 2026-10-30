package com.casino.game;

import com.casino.chip.Chips;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Convierte una casilla de la ruleta en el premio real (objetos, fichas, experiencia). */
public final class WheelRewards {
    private WheelRewards() {
    }

    private static Prize items(Component title, int slice, boolean big, ItemStack... stacks) {
        return new Prize(title, new ArrayList<>(List.of(stacks)), 0, slice, big);
    }

    private static Component countName(ItemStack s) {
        return Component.literal(s.getCount() + " x ").append(s.getHoverName());
    }

    /** El premio de una casilla. {@code podium} es el premio gordo actual del podio. */
    public static Prize build(int slice, ItemStack podium, RandomSource rnd) {
        WheelLayout.Slice sl = WheelLayout.SLICES[slice];
        boolean big = WheelLayout.isBig(slice);
        int n = sl.amount();
        switch (sl.kind()) {
            case PODIUM: {
                ItemStack p = podium.copy();
                return items(Component.translatable("casino.prize.podium", countName(p)), slice, true, p);
            }
            case MYSTERY:
                return mystery(slice, rnd);
            case CHIPS:
                return new Prize(Component.translatable("casino.prize.chips", n), Chips.stacksFor(n), 0, slice, big);
            case EMERALD: {
                ItemStack s = new ItemStack(Items.EMERALD, n);
                return items(countName(s), slice, big, s);
            }
            case XP:
                return new Prize(Component.translatable("casino.prize.xp", n), new ArrayList<>(), n, slice, big);
            case GOLD: {
                ItemStack s = new ItemStack(Items.GOLD_INGOT, n);
                return items(countName(s), slice, big, s);
            }
            case DIAMOND: {
                ItemStack s = new ItemStack(Items.DIAMOND, n);
                return items(countName(s), slice, big, s);
            }
            case FOOD: {
                ItemStack s = new ItemStack(Items.GOLDEN_CARROT, n);
                return items(countName(s), slice, big, s);
            }
            default: {
                ItemStack s = new ItemStack(Items.GOLDEN_APPLE, n);
                return items(countName(s), slice, big, s);
            }
        }
    }

    /** Premio misterioso: una de ocho sorpresas, todas valiosas. */
    private static Prize mystery(int slice, RandomSource rnd) {
        Prize inner;
        switch (rnd.nextInt(8)) {
            case 0 -> inner = new Prize(Component.translatable("casino.prize.chips", 750), Chips.stacksFor(750), 0, slice, true);
            case 1 -> {
                ItemStack s = new ItemStack(Items.DIAMOND, 6);
                inner = items(countName(s), slice, true, s);
            }
            case 2 -> {
                ItemStack s = new ItemStack(Items.EMERALD, 48);
                inner = items(countName(s), slice, true, s);
            }
            case 3 -> inner = new Prize(Component.translatable("casino.prize.xp", 30), new ArrayList<>(), 30, slice, true);
            case 4 -> {
                ItemStack s = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1);
                inner = items(countName(s), slice, true, s);
            }
            case 5 -> {
                ItemStack s = new ItemStack(Items.TOTEM_OF_UNDYING, 1);
                inner = items(countName(s), slice, true, s);
            }
            case 6 -> {
                ItemStack s = new ItemStack(Items.NETHERITE_INGOT, 1);
                inner = items(countName(s), slice, true, s);
            }
            default -> {
                ItemStack s = new ItemStack(Items.GOLD_BLOCK, 4);
                inner = items(countName(s), slice, true, s);
            }
        }
        return new Prize(Component.translatable("casino.prize.mystery", inner.title), inner.items, inner.xpLevels, slice, true);
    }
}
