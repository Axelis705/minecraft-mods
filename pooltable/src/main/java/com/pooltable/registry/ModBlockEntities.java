package com.pooltable.registry;

import com.pooltable.PoolTable;
import com.pooltable.block.BowlingLaneBE;
import com.pooltable.block.PoolTableBE;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PoolTable.MODID);

    public static final RegistryObject<BlockEntityType<PoolTableBE>> POOL_TABLE = TYPES.register("pool_table",
            () -> BlockEntityType.Builder.of(PoolTableBE::new, ModBlocks.POOL_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<BowlingLaneBE>> BOWLING_LANE = TYPES.register("bowling_lane",
            () -> BlockEntityType.Builder.of(BowlingLaneBE::new, ModBlocks.BOWLING_LANE.get()).build(null));

    private ModBlockEntities() {
    }
}
