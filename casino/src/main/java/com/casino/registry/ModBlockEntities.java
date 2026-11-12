package com.casino.registry;

import com.casino.plat.Reg;
import com.casino.Casino;
import com.casino.block.BlackjackBE;
import com.casino.block.LuckyWheelBE;
import com.casino.block.PlinkoBE;
import com.casino.block.PokerBE;
import com.casino.block.RouletteBE;
import com.casino.block.SlotBE;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Casino.MODID);

    public static final RegistryObject<BlockEntityType<LuckyWheelBE>> LUCKY_WHEEL = TYPES.register("lucky_wheel",
            () -> Reg.blockEntity(LuckyWheelBE::new, ModBlocks.LUCKY_WHEEL.get()));

    public static final RegistryObject<BlockEntityType<PlinkoBE>> PLINKO = TYPES.register("plinko",
            () -> Reg.blockEntity(PlinkoBE::new, ModBlocks.PLINKO.get()));

    public static final RegistryObject<BlockEntityType<SlotBE>> SLOT = TYPES.register("slot",
            () -> Reg.blockEntity(SlotBE::new, ModBlocks.SLOT.get()));

    public static final RegistryObject<BlockEntityType<BlackjackBE>> BLACKJACK = TYPES.register("blackjack",
            () -> Reg.blockEntity(BlackjackBE::new, ModBlocks.BLACKJACK.get()));

    public static final RegistryObject<BlockEntityType<PokerBE>> POKER = TYPES.register("poker",
            () -> Reg.blockEntity(PokerBE::new, ModBlocks.POKER.get()));

    public static final RegistryObject<BlockEntityType<RouletteBE>> ROULETTE = TYPES.register("roulette",
            () -> Reg.blockEntity(RouletteBE::new, ModBlocks.ROULETTE.get()));

    private ModBlockEntities() {
    }
}
