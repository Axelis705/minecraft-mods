package com.axeliscore.anim.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import com.axeliscore.anim.Geo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * (Cliente) Dónde está en el mundo el pivote de un hueso de una entidad animada del Core (con su animación de este momento): para que
 * los efectos, las estelas o lo que sea salgan de la mano, la boca o la punta de una cola.
 */
public final class Huesos {
    private static final java.util.Map<Geo, GeoRenderer.Frame> FRAMES = new java.util.IdentityHashMap<>();

    private Huesos() {
    }

    /** Posición del hueso (o null si la entidad no es del Core, no tiene ese hueso o todavía no se dibujó). */
    public static Vec3 posicion(Entity e, String hueso, float pt) {
        if (!(e instanceof Animated an) || hueso == null || hueso.isEmpty()) {
            return null;
        }
        AnimHost h = an.axHost();
        if (h == null || !h.ready()) {
            return null;
        }
        Geo geo = h.geo();
        int b = geo.bone(hueso);
        if (b < 0) {
            return null;
        }
        if (FRAMES.size() > 32) {
            FRAMES.clear();
        }
        GeoRenderer.Frame f = FRAMES.computeIfAbsent(geo, GeoRenderer.Frame::new);
        double ex = Mth.lerp(pt, e.xo, e.getX()), ey = Mth.lerp(pt, e.yo, e.getY()), ez = Mth.lerp(pt, e.zo, e.getZ());
        float yaw = e instanceof LivingEntity le ? Mth.rotLerp(pt, le.yBodyRotO, le.yBodyRot) : Mth.rotLerp(pt, e.yRotO, e.getYRot());
        if (e == EstudioEditor.objetivo) {
            yaw = EstudioEditor.giro(yaw);
        }
        float sc = 1F;
        var r = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);
        if (r instanceof GeoEntityRenderer<?> g) {
            sc = g.escalaPara(e);
        }
        PoseStack ps = new PoseStack();
        ps.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180F - yaw)));
        ps.scale(-1F, -1F, 1F);
        ps.scale(sc, sc, sc);
        ps.translate(0F, -1.501F, 0F);
        GeoRenderer.compute(geo, h.pose(), ps, f);
        Vector3f p = f.world[b].transformPosition(new Vector3f());
        return new Vec3(ex + p.x, ey + p.y, ez + p.z);
    }
}
