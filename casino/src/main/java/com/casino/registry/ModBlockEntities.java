package com.casino.registry;

import com.casino.Casino;
import com.casino.block.LuckyWheelBE;
import com.casino.block.PlinkoBE;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Casino.MODID);

    public static final RegistryObject<BlockEntityType<LuckyWheelBE>> LUCKY_WHEEL = TYPES.register("lucky_wheel",
            () -> BlockEntityType.Builder.of(LuckyWheelBE::new, ModBlocks.LUCKY_WHEEL.get()).build(null));

    public static final RegistryObject<BlockEntityType<PlinkoBE>> PLINKO = TYPES.register("plinko",
            () -> BlockEntityType.Builder.of(PlinkoBE::new, ModBlocks.PLINKO.get()).build(null));

    private ModBlockEntities() {
    }
}
