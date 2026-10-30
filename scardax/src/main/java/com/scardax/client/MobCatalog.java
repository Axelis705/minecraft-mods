package com.scardax.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * La lista de todos los mobs que hay en el juego (de todos los mods), separados por mod y por categoría. Se detectan creando un
 * ejemplar de cada tipo (sin ponerlo en el mundo): solo cuentan los que de verdad son mobs. Se guarda hasta que cambias de mundo.
 */
public final class MobCatalog {
    private static List<Entry> cache = null;
    private static Level cacheLevel = null;

    private MobCatalog() {
    }

    public static final class Entry {
        public final ResourceLocation id;
        public final EntityType<?> type;
        public final String modId;
        public final String modName;
        public final MobCategory category;
        public final Component name;
        public final String search;
        /** Un ejemplar para dibujar el icono (null si no se pudo crear). */
        public LivingEntity living;
        /** El icono falló al dibujarse: se usa el huevo de aparición o una marca. */
        public boolean broken;
        public final ItemStack egg;

        Entry(ResourceLocation id, EntityType<?> type, String modName, LivingEntity living) {
            this.id = id;
            this.type = type;
            this.modId = id.getNamespace();
            this.modName = modName;
            this.category = type.getCategory();
            this.name = type.getDescription();
            this.living = living;
            SpawnEggItem e = ForgeSpawnEggItem.fromEntityType(type);
            this.egg = e == null ? ItemStack.EMPTY : new ItemStack(e);
            this.search = (name.getString() + " " + id + " " + modName).toLowerCase();
        }
    }

    private static String modName(String modId) {
        if (modId.equals("minecraft")) {
            return "Minecraft";
        }
        return ModList.get().getModContainerById(modId).map(c -> c.getModInfo().getDisplayName()).orElse(modId);
    }

    public static List<Entry> get() {
        Level lvl = Minecraft.getInstance().level;
        if (cache != null && cacheLevel == lvl) {
            return cache;
        }
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<?, EntityType<?>> en : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            EntityType<?> type = en.getValue();
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (id == null || !type.canSummon()) {
                continue;
            }
            Entity ent = null;
            if (lvl != null) {
                try {
                    ent = type.create(lvl);
                } catch (Throwable t) {
                    ent = null;
                }
            }
            if (ent != null && !(ent instanceof Mob)) {
                continue;
            }
            if (ent == null && type.getCategory() == MobCategory.MISC) {
                continue;
            }
            out.add(new Entry(id, type, modName(id.getNamespace()), ent instanceof LivingEntity l ? l : null));
        }
        out.sort(Comparator.comparing((Entry e) -> e.modName.toLowerCase()).thenComparing(e -> e.name.getString().toLowerCase()));
        cache = out;
        cacheLevel = lvl;
        return out;
    }
}
