package com.mundosdeaxelis.crate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Rarity;

/**
 * Las ocho rarezas del mod (de menor a mayor: Común, Poco común, Rara, Épica, Legendaria, Mítica, Estelar y Celestial). Valen para reliquias, compañeros y equipo, y son las mismas que usan los crates.
 * {@code power} escala la potencia de las habilidades y {@code cooldown} el enfriamiento (más rara = más fuerte y más rápida).
 */
public enum Tier {
    COMMON("common", 0xC9D1D9, 40, 0.90F, 1.15F, Rarity.COMMON),
    UNCOMMON("uncommon", 0x6BE08A, 28, 1.00F, 1.05F, Rarity.UNCOMMON),
    RARE("rare", 0x4FA8FF, 18, 1.05F, 1.00F, Rarity.RARE),
    EPIC("epic", 0xC060FF, 9, 1.15F, 0.95F, Rarity.EPIC),
    LEGENDARY("legendary", 0xFFC93C, 4, 1.30F, 0.85F, Rarity.EPIC),
    MYTHIC("mythic", 0xFF4F8B, 1, 1.50F, 0.70F, Rarity.EPIC),
    /** Rareza del Cielo: por encima de la Mítica. No sale en el sorteo general ({@link #roll}), solo en las cajas altas. */
    STELLAR("stellar", 0x8F7BFF, 0, 1.70F, 0.62F, Rarity.EPIC),
    /** La rareza más alta de todas, la del Abismo. */
    CELESTIAL("celestial", 0x8CF6FF, 0, 1.90F, 0.55F, Rarity.EPIC);

    public final String id;
    public final int color;
    /** Peso al sortear en un crate (la suma de las seis es 100). */
    public final int weight;
    public final float power;
    public final float cooldown;
    public final Rarity rarity;

    Tier(String id, int color, int weight, float power, float cooldown, Rarity rarity) {
        this.id = id;
        this.color = color;
        this.weight = weight;
        this.power = power;
        this.cooldown = cooldown;
        this.rarity = rarity;
    }

    /**
     * Armadura base de una mascota sin caja conocida: 1 punto. La armadura real depende de la caja de la que sale la mascota
     * (2N-1 en la caja N y 2N su estrella): ver {@code CratePools.petArmor(String)}. Siempre números enteros.
     */
    public int petArmor() {
        return 1;
    }

    public Style style() {
        Style s = Style.EMPTY.withColor(TextColor.fromRgb(color));
        return ordinal() >= LEGENDARY.ordinal() ? s.withBold(true) : s;
    }

    /** Nombre traducido de la rareza ("Épica"). */
    public MutableComponent label() {
        return Component.translatable("tier.mundos_de_axelis." + id).withStyle(style());
    }

    /** Aplica el color de la rareza al nombre de un objeto. */
    public MutableComponent paint(Component name) {
        return name.copy().withStyle(style());
    }

    public static Tier byIndex(int i) {
        Tier[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }

    /** Rareza al azar según los pesos. */
    public static Tier roll(RandomSource r) {
        int total = 0;
        for (Tier t : values()) {
            total += t.weight;
        }
        int n = r.nextInt(total);
        for (Tier t : values()) {
            n -= t.weight;
            if (n < 0) {
                return t;
            }
        }
        return COMMON;
    }
}
