package com.casino.net;

import com.casino.plat.Plat;
import com.casino.plat.ItemTag;
import com.casino.chip.Chips;
import com.casino.game.Prize;
import com.casino.game.ScratchCards;
import com.casino.item.ScratchCardItem;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

/**
 * Cliente -> servidor: el jugador descubrió una casilla de su boleto. El servidor la anota en el boleto que tiene en la mano y, cuando ya
 * están las 6, cobra el premio (una sola vez) y deja el boleto marcado como cobrado.
 */
public final class ScratchCardPacket {
    public final boolean offHand;
    public final int cell;

    public ScratchCardPacket(boolean offHand, int cell) {
        this.offHand = offHand;
        this.cell = cell;
    }

    public static void encode(ScratchCardPacket m, FriendlyByteBuf b) {
        b.writeBoolean(m.offHand);
        b.writeVarInt(m.cell);
    }

    public static ScratchCardPacket decode(FriendlyByteBuf b) {
        return new ScratchCardPacket(b.readBoolean(), b.readVarInt());
    }

    public static void handle(ScratchCardPacket m, ServerPlayer p) {
        if (p == null || m.cell < 0 || m.cell >= ScratchCards.SPOTS) {
            return;
        }
        ItemStack st = p.getItemInHand(m.offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(st.getItem() instanceof ScratchCardItem) || !ScratchCardItem.hasCard(st) || ScratchCardItem.claimed(st)) {
            return;
        }
        int mask = ScratchCardItem.mask(st) | (1 << m.cell);
        ItemTag.edit(st, t -> t.putInt(ScratchCardItem.MASK, mask));
        if (mask != ScratchCardItem.ALL) {
            return;
        }
        ItemTag.edit(st, t -> t.putBoolean(ScratchCardItem.CLAIMED, true));
        int prize = ScratchCards.prizeOf(ScratchCardItem.symbols(st));
        if (prize > 0) {
            boolean big = prize >= 250;
            Prize pr = new Prize(Component.translatable("casino.blackjack.banner", prize), new ArrayList<>(Chips.stacksFor(prize)), 0, -1, big);
            pr.deliver(p);
            CasinoNet.sendTo(p, new WheelWinPacket(pr));
            p.level().playSound(null, p.blockPosition(), prize >= 250 ? ModSounds.WHEEL_JACKPOT.get() : ModSounds.WHEEL_WIN.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            Plat.msg(p, Component.translatable("casino.scratch.won", prize).withStyle(ChatFormatting.GOLD), true);
            if (prize >= 1000) {
                Component msg = Component.translatable("casino.scratch.won_big", Component.literal(p.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW),
                        Component.literal(String.valueOf(prize)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY);
                for (ServerPlayer o : Plat.server(p).getPlayerList().getPlayers()) {
                    o.sendSystemMessage(msg);
                }
            }
        } else {
            Plat.msg(p, Component.translatable("casino.scratch.lost").withStyle(ChatFormatting.GRAY), true);
        }
    }
}
