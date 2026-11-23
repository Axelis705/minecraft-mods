package com.axeliscore.plat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
//#if MC >= 1.21.2
//$$ import net.minecraft.core.registries.Registries;
//$$ import net.minecraft.resources.ResourceKey;
//#endif

/**
 * Propiedades de bloques, objetos y entidades, igual en todas las versiones. Desde 1.21.2 cada uno debe llevar su id desde que se crea,
 * por eso todo pide el namespace (el id del mod) y el nombre.
 */
public final class Reg {
    private Reg() {
    }

    public static BlockBehaviour.Properties block(String ns, String name) {
        //#if MC >= 1.21.2
        //$$ return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Ids.rl(ns, name)));
        //#else
        return BlockBehaviour.Properties.of();
        //#endif
    }

    public static Item.Properties item(String ns, String name) {
        //#if MC >= 1.21.2
        //$$ return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Ids.rl(ns, name)));
        //#else
        return new Item.Properties();
        //#endif
    }

    /** Para el objeto de un bloque: usa el nombre del bloque. */
    public static Item.Properties blockItem(String ns, String name) {
        //#if MC >= 1.21.2
        //$$ return item(ns, name).useBlockDescriptionPrefix();
        //#else
        return item(ns, name);
        //#endif
    }

    public static <T extends BlockEntity> BlockEntityType<T> blockEntity(BlockEntityType.BlockEntitySupplier<T> factory, Block... blocks) {
        //#if MC >= 1.21.2
        //$$ return new BlockEntityType<>(factory, java.util.Set.of(blocks));
        //#else
        return BlockEntityType.Builder.of(factory, blocks).build(null);
        //#endif
    }

    public static <T extends Entity> EntityType<T> entity(String ns, String name, EntityType.Builder<T> b) {
        //#if MC >= 1.21.2
        //$$ return b.build(ResourceKey.create(Registries.ENTITY_TYPE, Ids.rl(ns, name)));
        //#else
        return b.build(name);
        //#endif
    }
}
