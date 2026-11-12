package com.casino.registry;

import com.casino.Casino;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Casino.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.casino"))
            .icon(() -> new ItemStack(ModItems.CHIP_500.get()))
            .displayItems((params, out) -> {
                out.accept(ModItems.CASINO_GUIDE.get());
                out.accept(ModItems.LUCKY_WHEEL.get());
                out.accept(ModItems.PLINKO.get());
                out.accept(ModItems.SLOT.get());
                out.accept(ModItems.BLACKJACK.get());
                out.accept(ModItems.ROULETTE.get());
                out.accept(ModItems.POKER.get());
                out.accept(ModItems.SCRATCH_MACHINE.get());
                out.accept(ModItems.SCRATCH_CARD.get());
                out.accept(ModItems.CASHIER.get());
                out.accept(ModItems.VIP_SILVER.get());
                out.accept(ModItems.VIP_GOLD.get());
                out.accept(com.casino.diamond.DiamondReg.ELEVATOR_ITEM.get());
                out.accept(com.casino.diamond.DiamondReg.PRIZE_SHOP_ITEM.get());
                out.accept(ModItems.CHIP_1.get());
                out.accept(ModItems.CHIP_5.get());
                out.accept(ModItems.CHIP_25.get());
                out.accept(ModItems.CHIP_100.get());
                out.accept(ModItems.CHIP_500.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}
