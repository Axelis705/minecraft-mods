package com.pooltable.registry;

import com.pooltable.PoolTable;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PoolTable.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.pooltable"))
            .icon(() -> new ItemStack(ModItems.POOL_CUE.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.POOL_TABLE.get());
                out.accept(ModItems.POOL_CUE.get());
                out.accept(ModItems.BOWLING_LANE.get());
                out.accept(ModItems.BOWLING_BALL.get());
                out.accept(ModItems.AIR_HOCKEY.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}
