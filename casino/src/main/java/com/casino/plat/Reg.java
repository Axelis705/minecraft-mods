package com.casino.plat;

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

/** Propiedades de bloques y objetos. Desde 1.21.2 cada uno debe llevar su id desde que se crea. */
public final class Reg {
    private Reg() {
    }

    public static BlockBehaviour.Properties block(String name) {
        //#if MC >= 1.21.2
        //$$ return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, Ids.casino(name)));
        //#else
        return BlockBehaviour.Properties.of();
        //#endif
    }

    public static Item.Properties item(String name) {
        //#if MC >= 1.21.2
        //$$ return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Ids.casino(name)));
        //#else
        return new Item.Properties();
        //#endif
    }

    /** Para el objeto de un bloque: usa el nombre del bloque. */
    public static Item.Properties blockItem(String name) {
        //#if MC >= 1.21.2
        //$$ return item(name).useBlockDescriptionPrefix();
        //#else
        return item(name);
        //#endif
    }

    public static <T extends BlockEntity> BlockEntityType<T> blockEntity(BlockEntityType.BlockEntitySupplier<T> factory, Block... blocks) {
        //#if MC >= 1.21.2
        //$$ return new BlockEntityType<>(factory, java.util.Set.of(blocks));
        //#else
        return BlockEntityType.Builder.of(factory, blocks).build(null);
        //#endif
    }

    public static <T extends Entity> EntityType<T> entity(String name, EntityType.Builder<T> b) {
        //#if MC >= 1.21.2
        //$$ return b.build(ResourceKey.create(Registries.ENTITY_TYPE, Ids.casino(name)));
        //#else
        return b.build(name);
        //#endif
    }
}
