package com.casino.net;

import com.casino.plat.Plat;
import com.casino.block.ScratchMachineBlock;
import com.casino.chip.Chips;
import com.casino.game.ScratchCards;
import com.casino.item.ScratchCardItem;
import com.casino.registry.ModItems;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

/** Cliente -> servidor: comprar boletos en la máquina de rasca y gana. El servidor comprueba distancia, cantidad y fichas, y sortea cada boleto. */
public final class ScratchBuyPacket {
    public final BlockPos pos;
    public final int count;

    public ScratchBuyPacket(BlockPos pos, int count) {
        this.pos = pos;
        this.count = count;
    }

    public static void encode(ScratchBuyPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.count);
    }

    public static ScratchBuyPacket decode(FriendlyByteBuf b) {
        return new ScratchBuyPacket(b.readBlockPos(), b.readVarInt());
    }

    public static void handle(ScratchBuyPacket m, ServerPlayer p) {
        if (p == null || !p.level().isLoaded(m.pos) || !ScratchCards.validQuantity(m.count)) {
            return;
        }
        if (!(p.level().getBlockState(m.pos).getBlock() instanceof ScratchMachineBlock)
                || p.distanceToSqr(m.pos.getX() + 0.5, m.pos.getY() + 1.0, m.pos.getZ() + 0.5) > 8 * 8) {
            return;
        }
        int cost = ScratchCards.PRICE * m.count;
        if (!Chips.take(p, cost)) {
            Plat.msg(p, Component.translatable("casino.plinko.no_chips", cost).withStyle(ChatFormatting.RED), true);
            p.level().playSound(null, m.pos, ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            return;
        }
        Random rnd = new Random();
        for (int i = 0; i < m.count; i++) {
            ItemStack card = ScratchCardItem.create(ModItems.SCRATCH_CARD.get(), rnd);
            net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(p, card);
        }
        p.level().playSound(null, m.pos, ModSounds.COINS.get(), SoundSource.BLOCKS, 0.7F, 1.2F);
        Plat.msg(p, Component.translatable("casino.scratch.bought", m.count, cost).withStyle(ChatFormatting.GREEN), true);
    }
}
