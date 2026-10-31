package com.pooltable.registry;

import com.pooltable.PoolTable;
import com.pooltable.block.AirHockeyBlock;
import com.pooltable.block.BowlingLaneBlock;
import com.pooltable.block.PoolTableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PoolTable.MODID);

    public static final RegistryObject<Block> POOL_TABLE = BLOCKS.register("pool_table", () -> new PoolTableBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0F, 6.0F).sound(SoundType.WOOD)
                    .noOcclusion().pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<Block> BOWLING_LANE = BLOCKS.register("bowling_lane", () -> new BowlingLaneBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(2.0F, 6.0F).sound(SoundType.WOOD)
                    .noOcclusion().pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<Block> AIR_HOCKEY = BLOCKS.register("air_hockey", () -> new AirHockeyBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F).sound(SoundType.METAL)
                    .noOcclusion().pushReaction(PushReaction.BLOCK)));

    private ModBlocks() {
    }
}
