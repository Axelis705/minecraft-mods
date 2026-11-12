package com.casino.net;

import com.casino.decor.NeonLetterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/** Cliente -> servidor: cambiar la letra y el color de una letra de neón. El servidor comprueba distancia y bloque. */
public final class NeonPacket {
    public final BlockPos pos;
    public final int glyph;
    public final int color;

    public NeonPacket(BlockPos pos, int glyph, int color) {
        this.pos = pos;
        this.glyph = glyph;
        this.color = color;
    }

    public static void encode(NeonPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.glyph);
        b.writeVarInt(m.color);
    }

    public static NeonPacket decode(FriendlyByteBuf b) {
        return new NeonPacket(b.readBlockPos(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(NeonPacket m, ServerPlayer p) {
        if (p == null || !p.level().isLoaded(m.pos) || p.distanceToSqr(m.pos.getX() + 0.5, m.pos.getY() + 0.5, m.pos.getZ() + 0.5) > 12 * 12) {
            return;
        }
        BlockState s = p.level().getBlockState(m.pos);
        if (!(s.getBlock() instanceof NeonLetterBlock)) {
            return;
        }
        int g = Math.max(0, Math.min(NeonLetterBlock.CHARS.length() - 1, m.glyph));
        int c = Math.max(0, Math.min(NeonLetterBlock.COLORS.length - 1, m.color));
        p.level().setBlock(m.pos, s.setValue(NeonLetterBlock.GLYPH, g).setValue(NeonLetterBlock.COLOR, c), 3);
    }
}
