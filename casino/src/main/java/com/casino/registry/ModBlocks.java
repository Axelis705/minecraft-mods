package com.casino.registry;

import com.casino.Casino;
import com.casino.block.BlackjackBlock;
import com.casino.block.CashierBlock;
import com.casino.block.LuckyWheelBlock;
import com.casino.block.PlinkoBlock;
import com.casino.block.PokerBlock;
import com.casino.block.RouletteBlock;
import com.casino.block.SlotBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Casino.MODID);

    public static final RegistryObject<Block> CASHIER = BLOCKS.register("cashier", () -> new CashierBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0F, 8.0F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> 8)));
    public static final RegistryObject<Block> LUCKY_WHEEL = BLOCKS.register("lucky_wheel", () -> new LuckyWheelBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0F, 8.0F).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(PushReaction.BLOCK).lightLevel(s -> 10)));

    public static final RegistryObject<Block> PLINKO = BLOCKS.register("plinko", () -> new PlinkoBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 8.0F).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(PushReaction.BLOCK).lightLevel(s -> 10)));

    public static final RegistryObject<Block> SLOT = BLOCKS.register("slot", () -> new SlotBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F, 8.0F).sound(SoundType.METAL).noOcclusion()
                    .pushReaction(PushReaction.BLOCK).lightLevel(s -> 10)));

    public static final RegistryObject<Block> BLACKJACK = BLOCKS.register("blackjack", () -> new BlackjackBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(3.0F, 8.0F).sound(SoundType.WOOD).noOcclusion()
                    .pushReaction(PushReaction.BLOCK).lightLevel(s -> 6)));

    public static final RegistryObject<Block> ROULETTE = BLOCKS.register("roulette", () -> new RouletteBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F, 8.0F).sound(SoundType.WOOD).noOcclusion()
                    .pushReaction(PushReaction.BLOCK).lightLevel(s -> 6)));

    public static final RegistryObject<Block> POKER = BLOCKS.register("poker", () -> new PokerBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(3.0F, 8.0F).sound(SoundType.WOOD).noOcclusion()
                    .pushReaction(PushReaction.BLOCK).lightLevel(s -> 6)));

    private ModBlocks() {
    }
}
