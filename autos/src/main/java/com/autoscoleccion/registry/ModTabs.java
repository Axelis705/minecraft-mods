package com.autoscoleccion.registry;

import com.autoscoleccion.AutosMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AutosMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.autos_coleccion"))
            .icon(() -> new ItemStack(ModItems.CARS.get(20).get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.VENDING_MACHINE.get());
                out.accept(ModItems.DISPLAY_CASE.get());
                out.accept(ModItems.GARAGE_BOOK.get());
                ModItems.BOXES.forEach(b -> out.accept(b.get()));
                ModItems.CARS.forEach(c -> out.accept(c.get()));
            })
            .build());

    private ModTabs() {
    }
}
