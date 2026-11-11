package com.autoscoleccion.registry;

import com.autoscoleccion.AutosMod;
import com.autoscoleccion.block.DisplayCaseBlockEntity;
import com.autoscoleccion.block.VendingMachineBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AutosMod.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<VendingMachineBlockEntity>> VENDING_MACHINE = BLOCK_ENTITIES.register("maquina_expendedora",
            () -> BlockEntityType.Builder.of(VendingMachineBlockEntity::new, ModBlocks.VENDING_MACHINE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<DisplayCaseBlockEntity>> DISPLAY_CASE = BLOCK_ENTITIES.register("vitrina",
            () -> BlockEntityType.Builder.of(DisplayCaseBlockEntity::new, ModBlocks.DISPLAY_CASE.get()).build(null));

    private ModBlockEntities() {
    }
}
