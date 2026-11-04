package com.mundosdeaxelis.cinematic;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * Un punto de la trayectoria de cámara: en el tick indicado la cámara está en "pos" y mira hacia "target".
 * Entre dos puntos la cámara se mueve con una curva suave (Catmull-Rom).
 */
public record Keyframe(int tick, Vec3 pos, Vec3 target) {

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(tick);
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        buf.writeDouble(target.x);
        buf.writeDouble(target.y);
        buf.writeDouble(target.z);
    }

    public static Keyframe read(FriendlyByteBuf buf) {
        int tick = buf.readVarInt();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 target = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new Keyframe(tick, pos, target);
    }

    public Keyframe offset(Vec3 origin) {
        return new Keyframe(tick, pos.add(origin), target.add(origin));
    }
}
