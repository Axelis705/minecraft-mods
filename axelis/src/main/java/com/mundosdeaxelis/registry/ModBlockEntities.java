package com.mundosdeaxelis.registry;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MundosDeAxelis.MODID);


    public static final RegistryObject<BlockEntityType<com.mundosdeaxelis.block.CrateBlockEntity>> CRATE = BLOCK_ENTITIES.register("crate",
            () -> BlockEntityType.Builder.of(com.mundosdeaxelis.block.CrateBlockEntity::new,
                    ModBlocks.CRATES.values().stream().map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<com.mundosdeaxelis.block.ArtificerTableBlockEntity>> ARTIFICER_TABLE = BLOCK_ENTITIES.register("artificer_table",
            () -> BlockEntityType.Builder.of(com.mundosdeaxelis.block.ArtificerTableBlockEntity::new, ModBlocks.ARTIFICER_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.mundosdeaxelis.block.GraveBlockEntity>> GRAVE = BLOCK_ENTITIES.register("grave",
            () -> BlockEntityType.Builder.of(com.mundosdeaxelis.block.GraveBlockEntity::new, ModBlocks.GRAVE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.mundosdeaxelis.block.VaultChestBlockEntity>> VAULT = BLOCK_ENTITIES.register("vault",
            () -> BlockEntityType.Builder.of(com.mundosdeaxelis.block.VaultChestBlockEntity::new, ModBlocks.VAULT_CHEST.get(), ModBlocks.VAULT_CHEST_SEALED.get(), ModBlocks.VAULT_CHEST_LOCKED.get(),
                    ModBlocks.VAULT_CHEST_FORGE.get(), ModBlocks.VAULT_CHEST_SEALED_FORGE.get(), ModBlocks.VAULT_CHEST_LOCKED_FORGE.get()).build(null));

    private ModBlockEntities() {
    }
}
