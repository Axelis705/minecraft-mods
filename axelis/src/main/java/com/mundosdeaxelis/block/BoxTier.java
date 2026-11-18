package com.mundosdeaxelis.block;

import net.minecraft.util.RandomSource;

/** Qué tan especial resulta una caja misteriosa. Se decide al abrirla. */
public enum BoxTier {
    COMMON("common", 0xC9C9E6, 55, 0),
    RARE("rare", 0x4FA8FF, 30, 0),
    EPIC("epic", 0xC060FF, 12, 1),
    LEGENDARY("legendary", 0xFFC93C, 3, 2);

    public final String id;
    public final int color;
    public final int weight;
    /** Objetos extra además del principal. */
    public final int extraDrops;

    BoxTier(String id, int color, int weight, int extraDrops) {
        this.id = id;
        this.color = color;
        this.weight = weight;
        this.extraDrops = extraDrops;
    }

    public static BoxTier roll(RandomSource r) {
        int total = 0;
        for (BoxTier t : values()) {
            total += t.weight;
        }
        int n = r.nextInt(total);
        for (BoxTier t : values()) {
            n -= t.weight;
            if (n < 0) {
                return t;
            }
        }
        return COMMON;
    }

    public static BoxTier byIndex(int i) {
        BoxTier[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }
}
