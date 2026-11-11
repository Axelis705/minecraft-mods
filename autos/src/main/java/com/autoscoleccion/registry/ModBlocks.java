package com.autoscoleccion.registry;

import com.autoscoleccion.AutosMod;
import com.autoscoleccion.block.DisplayCaseBlock;
import com.autoscoleccion.block.VendingMachineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AutosMod.MODID);

    public static final RegistryObject<Block> VENDING_MACHINE = BLOCKS.register("maquina_expendedora",
            () -> new VendingMachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F, 6.0F)
                    .sound(SoundType.METAL).lightLevel(s -> 9).noOcclusion().requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> DISPLAY_CASE = BLOCKS.register("vitrina",
            () -> new DisplayCaseBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5F)
                    .sound(SoundType.GLASS).lightLevel(s -> 6).noOcclusion()));

    private ModBlocks() {
    }
}
