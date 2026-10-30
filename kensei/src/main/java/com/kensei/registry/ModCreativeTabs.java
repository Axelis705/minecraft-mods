package com.kensei.registry;

import com.kensei.Kensei;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Kensei.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.kensei"))
            .icon(() -> new ItemStack(ModItems.KATANA.get()))
            .displayItems((params, out) -> {
                for (RegistryObject<Item> k : ModItems.WEAPONS) {
                    out.accept(k.get());
                }
                out.accept(ModItems.RIVAL_EGG.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}
