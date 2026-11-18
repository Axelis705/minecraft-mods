package com.mundosdeaxelis.network;

import com.mundosdeaxelis.cinematic.Keyframe;
import com.mundosdeaxelis.cinematic.Subtitle;
import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Servidor -> cliente: empieza una cinemática (trayectoria de cámara y subtítulos incluidos). */
public class StartCinematicPacket {
    public final int duration;
    public final int fadeOut;
    public final boolean bars;
    public final List<Keyframe> frames;
    public final List<Subtitle> subtitles;

    public StartCinematicPacket(int duration, int fadeOut, boolean bars, List<Keyframe> frames, List<Subtitle> subtitles) {
        this.duration = duration;
        this.fadeOut = fadeOut;
        this.bars = bars;
        this.frames = frames;
        this.subtitles = subtitles;
    }

    public static void encode(StartCinematicPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.duration);
        buf.writeVarInt(msg.fadeOut);
        buf.writeBoolean(msg.bars);
        buf.writeVarInt(msg.frames.size());
        for (Keyframe k : msg.frames) {
            k.write(buf);
        }
        buf.writeVarInt(msg.subtitles.size());
        for (Subtitle s : msg.subtitles) {
            s.write(buf);
        }
    }

    public static StartCinematicPacket decode(FriendlyByteBuf buf) {
        int duration = buf.readVarInt();
        int fadeOut = buf.readVarInt();
        boolean bars = buf.readBoolean();
        int nFrames = buf.readVarInt();
        List<Keyframe> frames = new ArrayList<>();
        for (int i = 0; i < nFrames; i++) {
            frames.add(Keyframe.read(buf));
        }
        int nSubs = buf.readVarInt();
        List<Subtitle> subs = new ArrayList<>();
        for (int i = 0; i < nSubs; i++) {
            subs.add(Subtitle.read(buf));
        }
        return new StartCinematicPacket(duration, fadeOut, bars, frames, subs);
    }

    public static void handle(StartCinematicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.startCinematic(msg)));
        context.setPacketHandled(true);
    }
}
