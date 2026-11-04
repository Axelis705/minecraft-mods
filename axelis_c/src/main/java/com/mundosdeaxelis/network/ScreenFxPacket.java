package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: efecto con texturas sobre la pantalla (destello, burbujas, confeti...). */
public class ScreenFxPacket {
    public static final int FLASH = 0;
    public static final int VIGNETTE = 1;
    public static final int BUBBLES = 2;
    public static final int CONFETTI = 3;
    public static final int HEARTS = 4;
    public static final int STARS = 5;
    public static final int SPEED = 6;
    public static final int SUNRISE = 7;
    public static final int PETALS = 8;
    /** Sacudida de cámara (rgb = fuerza en décimas de grado). */
    public static final int SHAKE = 9;
    /** Bordes oscuros (sombras, piedra, vacío). */
    public static final int DARK = 10;
    /** Ventisca: velo blanco azulado con ráfagas de nieve (se queda y se desvanece). */
    public static final int WHITEOUT = 11;

    private final int kind;
    private final int rgb;
    private final int ticks;

    public ScreenFxPacket(int kind, int rgb, int ticks) {
        this.kind = kind;
        this.rgb = rgb;
        this.ticks = ticks;
    }

    public static void encode(ScreenFxPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.kind);
        buf.writeInt(msg.rgb);
        buf.writeVarInt(msg.ticks);
    }

    public static ScreenFxPacket decode(FriendlyByteBuf buf) {
        return new ScreenFxPacket(buf.readVarInt(), buf.readInt(), buf.readVarInt());
    }

    public static void handle(ScreenFxPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        final int k = msg.kind;
        final int c = msg.rgb;
        final int t = msg.ticks;
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.screenFx(k, c, t)));
        context.setPacketHandled(true);
    }
}
