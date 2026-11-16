package com.casino.west;

import com.casino.chip.Chips;
import com.casino.game.BullRide;
import com.casino.plat.Plat;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/** Jugador -> servidor: apostar en el toro o en la galería de tiro, y las teclas del jinete (A/D). El servidor lo revisa todo. */
public final class WestPacket {
    public static final int BULL_BET = 0;
    public static final int GALLERY_BET = 1;
    public static final int BULL_INPUT = 2;
    /** Dados del mentiroso: apostar para empezar, subir (value = cantidad * 10 + cara) y gritar "¡mentiroso!". */
    public static final int DICE_BET = 4;
    public static final int DICE_BID = 5;
    public static final int DICE_CALL = 6;
    /** Retos (duelo y vencidas): contra NPC (value = rival * 100000 + apuesta), contra jugador (value = apuesta), aceptar, cancelar,
     *  rendirse; duelo: tecla (value = ms * 256 + letra); vencidas: golpes (value = cuántos). */
    public static final int MATCH_NPC = 7;
    public static final int MATCH_PVP = 8;
    public static final int MATCH_ACCEPT = 9;
    public static final int MATCH_CANCEL = 10;
    public static final int MATCH_GIVE_UP = 11;
    public static final int DUEL_KEY = 12;
    public static final int ARM_TAPS = 13;

    public final int action;
    public final BlockPos pos;
    public final int value;

    public WestPacket(int action, BlockPos pos, int value) {
        this.action = action;
        this.pos = pos;
        this.value = value;
    }

    public static void encode(WestPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.action);
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.value);
    }

    public static WestPacket decode(FriendlyByteBuf b) {
        return new WestPacket(b.readVarInt(), b.readBlockPos(), b.readVarInt());
    }

    public static void handle(WestPacket m, ServerPlayer p) {
        if (p == null) {
            return;
        }
        if (m.action == BULL_INPUT) {
            if (p.getVehicle() instanceof BullRideEntity r) {
                r.setInput(m.value);
            }
            return;
        }
        // quien juega un reto puede estar lejos de la campana (el duelista del otro lado de la calle)
        double reach = m.action == DUEL_KEY || m.action == ARM_TAPS || m.action == MATCH_GIVE_UP ? 40 : 8;
        if (!p.level().isLoaded(m.pos) || p.distanceToSqr(m.pos.getX() + 0.5, m.pos.getY() + 0.5, m.pos.getZ() + 0.5) > reach * reach) {
            return;
        }
        if (m.action == BULL_BET) {
            if (!(p.level().getBlockState(m.pos).getBlock() instanceof WestBlocks.Bull) || !BullRide.validBet(m.value)) {
                return;
            }
            if (!p.level().getEntitiesOfClass(BullRideEntity.class, new net.minecraft.world.phys.AABB(m.pos).inflate(1.5)).isEmpty()) {
                Plat.msg(p, Component.translatable("casino.bull.busy").withStyle(ChatFormatting.YELLOW), true);
                return;
            }
            if (!Chips.take(p, m.value)) {
                Plat.msg(p, Component.translatable("casino.plinko.no_chips", m.value).withStyle(ChatFormatting.RED), true);
                p.level().playSound(null, m.pos, ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
                return;
            }
            if (!BullRideEntity.start(p, m.pos, m.value)) {
                Chips.give(p, m.value);
            }
        } else if (m.action == DICE_BET || m.action == DICE_BID || m.action == DICE_CALL) {
            if (p.level().getBlockEntity(m.pos) instanceof DiceBE be) {
                if (m.action == DICE_BET) {
                    if (be.phase() != DiceBE.IDLE) {
                        Plat.msg(p, Component.translatable("casino.dice.busy", be.playerName()).withStyle(ChatFormatting.YELLOW), true);
                    } else {
                        be.begin(p, m.value);
                    }
                } else if (m.action == DICE_BID) {
                    be.playerBid(p, m.value / 10, m.value % 10);
                } else {
                    be.playerCall(p);
                }
            }
        } else if (m.action >= MATCH_NPC && m.action <= ARM_TAPS) {
            if (!(p.level().getBlockEntity(m.pos) instanceof MatchBE be)) {
                return;
            }
            switch (m.action) {
                case MATCH_NPC -> be.challengeNpc(p, m.value / 100000, m.value % 100000);
                case MATCH_PVP -> be.challengePvp(p, m.value);
                case MATCH_ACCEPT -> be.accept(p);
                case MATCH_CANCEL -> be.cancel(p);
                case MATCH_GIVE_UP -> be.giveUp(p);
                case DUEL_KEY -> {
                    if (be instanceof DuelBE d) {
                        d.press(p, (char) (m.value & 0xFF), m.value >>> 8);
                    }
                }
                default -> {
                    if (be instanceof ArmBE a) {
                        a.taps(p, m.value);
                    }
                }
            }
        } else if (m.action == GALLERY_BET) {
            if (p.level().getBlockEntity(m.pos) instanceof GalleryBE be) {
                if (be.playing()) {
                    Plat.msg(p, Component.translatable("casino.gallery.busy", be.shooterName()).withStyle(ChatFormatting.YELLOW), true);
                } else {
                    be.begin(p, m.value);
                }
            }
        }
    }
}
