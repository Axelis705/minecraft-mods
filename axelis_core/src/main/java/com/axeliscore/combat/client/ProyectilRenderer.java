package com.axeliscore.combat.client;

import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.anim.client.GeoAssets;
import com.axeliscore.anim.client.GeoRenderer;
import com.axeliscore.combat.Proyectil;
import com.axeliscore.combat.ProyectilTipo;
import com.axeliscore.plat.Plat;
import com.axeliscore.plat.client.CoreEntityRenderer;
import com.axeliscore.plat.client.Draw;
import com.axeliscore.plat.client.VB;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** Dibuja un {@link Proyectil} según su tipo: orbe con halo (brilla), objeto que gira o modelo de Blockbench. */
public class ProyectilRenderer extends CoreEntityRenderer<Proyectil, ProyectilRenderer.Snap> {
    public static final class Snap {
        ProyectilTipo tipo;
        float yaw, pitch, edad;
        ItemStack item = ItemStack.EMPTY;
        int seed;
    }

    private static final Map<String, ItemStack> ITEMS = new HashMap<>();
    private final Map<Geo, GeoRenderer.Frame> frames = new IdentityHashMap<>();
    private final Map<Geo, Pose> poses = new IdentityHashMap<>();

    public ProyectilRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected Snap snap(Proyectil e, float pt) {
        ProyectilTipo t = e.tipo();
        if (t == null) {
            return null;
        }
        Snap s = new Snap();
        s.tipo = t;
        s.yaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        s.pitch = Mth.lerp(pt, e.xRotO, e.getXRot());
        s.edad = e.tickCount + pt;
        s.seed = e.getId();
        if ("objeto".equals(t.aspecto)) {
            s.item = ITEMS.computeIfAbsent(t.objeto, id -> new ItemStack(Plat.item(id)));
        }
        return s;
    }

    @Override
    protected void draw(Snap s, PoseStack ps, Draw d, int light) {
        ProyectilTipo t = s.tipo;
        switch (t.aspecto) {
            case "objeto" -> {
                if (s.item.isEmpty()) {
                    return;
                }
                ps.pushPose();
                ps.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-s.yaw)));
                ps.mulPose(new Quaternionf().rotationX((float) Math.toRadians(s.pitch)));
                if (t.gira != 0) {
                    ps.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(s.edad * t.gira)));
                }
                ps.scale(t.tam * 2, t.tam * 2, t.tam * 2);
                d.item(s.item, ItemDisplayContext.GROUND, ps, light, Minecraft.getInstance().level, s.seed);
                ps.popPose();
            }
            case "modelo" -> modelo(s, ps, d, light);
            default -> orbe(s, ps, d);
        }
    }

    /** Esfera de luz: halo grande y suave más un centro casi blanco, siempre de frente a la cámara. */
    private static void orbe(Snap s, PoseStack ps, Draw d) {
        ProyectilTipo t = s.tipo;
        int c = t.color;
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        float pulse = 1F + 0.08F * (float) Math.sin(s.edad * 0.9F);
        ps.pushPose();
        ps.translate(0, t.tam * 0.25F, 0);
        ps.mulPose(Draw.camRot());
        VB vb = d.glow(Draw.ORBE);
        PoseStack.Pose p = ps.last();
        quad(vb, p, t.tam * 1.9F * pulse, r, g, b, 200);
        quad(vb, p, t.tam * 0.9F * pulse, Math.min(255, r + 120), Math.min(255, g + 120), Math.min(255, b + 120), 255);
        ps.popPose();
    }

    private static void quad(VB vb, PoseStack.Pose p, float size, int r, int g, int b, int a) {
        float h = size / 2;
        int light = 0xF000F0, ov = OverlayTexture.NO_OVERLAY;
        vb.v(p, -h, -h, 0, r, g, b, a, 0, 1, ov, light, 0, 0, 1);
        vb.v(p, h, -h, 0, r, g, b, a, 1, 1, ov, light, 0, 0, 1);
        vb.v(p, h, h, 0, r, g, b, a, 1, 0, ov, light, 0, 0, 1);
        vb.v(p, -h, h, 0, r, g, b, a, 0, 0, ov, light, 0, 0, 1);
        // por detrás también (por si el pincel no dibuja las caras de atrás)
        vb.v(p, -h, h, 0, r, g, b, a, 0, 0, ov, light, 0, 0, -1);
        vb.v(p, h, h, 0, r, g, b, a, 1, 0, ov, light, 0, 0, -1);
        vb.v(p, h, -h, 0, r, g, b, a, 1, 1, ov, light, 0, 0, -1);
        vb.v(p, -h, -h, 0, r, g, b, a, 0, 1, ov, light, 0, 0, -1);
    }

    /** Modelo de Blockbench quieto (gira hacia donde vuela y, si se pide, sobre sí mismo). */
    private void modelo(Snap s, PoseStack ps, Draw d, int light) {
        ProyectilTipo t = s.tipo;
        if (t.geo == null || t.textura == null) {
            return;
        }
        Geo geo = GeoAssets.geo(t.geo);
        if (geo == null) {
            return;
        }
        Pose pose = poses.computeIfAbsent(geo, k -> new Pose(k.bones.length));
        ps.pushPose();
        ps.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180F - s.yaw)));
        ps.mulPose(new Quaternionf().rotationX((float) Math.toRadians(-s.pitch)));
        if (t.gira != 0) {
            ps.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(s.edad * t.gira)));
        }
        ps.scale(-1F, -1F, 1F);
        ps.scale(t.tam, t.tam, t.tam);
        ps.translate(0F, -1.501F, 0F);
        GeoRenderer.Frame f = frames.computeIfAbsent(geo, GeoRenderer.Frame::new);
        GeoRenderer.compute(geo, pose, ps, f);
        GeoRenderer.draw(geo, pose, f, d.cutout(t.textura), GeoRenderer.Pass.NORMAL, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        GeoRenderer.draw(geo, pose, f, d.glow(t.textura), GeoRenderer.Pass.GLOW, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        ps.popPose();
    }
}
