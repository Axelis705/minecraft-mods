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
    /** value = pistolero * 100000 + apuesta */
    public static final int DUEL_BET = 3;

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
        if (!p.level().isLoaded(m.pos) || p.distanceToSqr(m.pos.getX() + 0.5, m.pos.getY() + 0.5, m.pos.getZ() + 0.5) > 8 * 8) {
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
        } else if (m.action == DUEL_BET) {
            if (p.level().getBlockEntity(m.pos) instanceof DuelBE be) {
                if (be.phase() != DuelBE.IDLE) {
                    Plat.msg(p, Component.translatable("casino.duel.busy").withStyle(ChatFormatting.YELLOW), true);
                } else {
                    be.begin(p, m.value / 100000, m.value % 100000);
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
