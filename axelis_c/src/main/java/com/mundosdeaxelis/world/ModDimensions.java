package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class ModDimensions {

    /** La arena del Rey Brote (jefe de la etapa 1): data/mundos_de_axelis/dimension/boss_forest.json */
    public static final ResourceKey<Level> BOSS_FOREST =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MundosDeAxelis.MODID, "boss_forest"));

    /** La arena del Gran Herrero (jefe de la etapa 2): data/mundos_de_axelis/dimension/boss_forge.json */
    public static final ResourceKey<Level> BOSS_FORGE =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MundosDeAxelis.MODID, "boss_forge"));

    /** La sala del Lobo Ártico (jefe de la etapa 3): data/mundos_de_axelis/dimension/boss_ice.json */
    public static final ResourceKey<Level> BOSS_ICE =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MundosDeAxelis.MODID, "boss_ice"));

    /** Cuántas copias de cada dungeon puede haber a la vez (cada grupo que pulsa «Comenzar» usa una copia libre). */
    public static final int COPIES = 3;

    /** Las copias de la dungeon del Bosque: la original y {@code boss_forest_2}, {@code boss_forest_3}... (cada una con su json). */
    public static final List<ResourceKey<Level>> FOREST_COPIES = copies(BOSS_FOREST, "boss_forest");
    public static final List<ResourceKey<Level>> FORGE_COPIES = copies(BOSS_FORGE, "boss_forge");
    public static final List<ResourceKey<Level>> ICE_COPIES = copies(BOSS_ICE, "boss_ice");

    private static List<ResourceKey<Level>> copies(ResourceKey<Level> first, String name) {
        List<ResourceKey<Level>> out = new ArrayList<>();
        out.add(first);
        for (int i = 2; i <= COPIES; i++) {
            out.add(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(MundosDeAxelis.MODID, name + "_" + i)));
        }
        return List.copyOf(out);
    }

    public static boolean isForest(ResourceKey<Level> k) {
        return FOREST_COPIES.contains(k);
    }

    public static boolean isForge(ResourceKey<Level> k) {
        return FORGE_COPIES.contains(k);
    }

    public static boolean isIce(ResourceKey<Level> k) {
        return ICE_COPIES.contains(k);
    }

    /** ¿Es una copia de alguna dungeon de jefe? */
    public static boolean isBoss(ResourceKey<Level> k) {
        return isForest(k) || isForge(k) || isIce(k);
    }

    /** Las copias de la dungeon de una etapa (1 Bosque, 2 Forja, 3 Hielo). */
    public static List<ResourceKey<Level>> copiesOf(int stage) {
        return stage == 3 ? ICE_COPIES : stage == 2 ? FORGE_COPIES : FOREST_COPIES;
    }

    private ModDimensions() {
    }
}
