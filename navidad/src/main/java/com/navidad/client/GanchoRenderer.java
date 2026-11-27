package com.navidad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.navidad.entidad.GanchoCadena;
import com.navidad.entidad.Krampus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * (Cliente) La cadena con gancho: los eslabones (la textura de la cadena de Minecraft, en dos planos cruzados) desde la mano de su
 * dueño hasta el gancho, y el gancho en la punta.
 */
public class GanchoRenderer extends EntityRenderer<GanchoCadena> {
    private static final ItemStack GANCHO = new ItemStack(Items.TRIPWIRE_HOOK);
    private static final ResourceLocation CADENA = new ResourceLocation("block/chain");

    public GanchoRenderer(EntityRendererProvider.Context c) {
        super(c);
    }

    @Override
    public void render(GanchoCadena g, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        Entity dueno = g.getOwner();
        Vec3 fin = g.getPosition(pt);
        if (dueno != null) {
            Vec3 d = mano(dueno, pt).subtract(fin);
            cadena(ps, buf.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), d, light);
        }
        ps.pushPose();
        ps.translate(0, 0.05, 0);
        Vec3 v = dueno == null ? Vec3.ZERO : mano(dueno, pt).subtract(fin);
        if (v.lengthSqr() > 1.0E-4) {
            float giro = (float) Math.toDegrees(Math.atan2(v.x, v.z));
            ps.mulPose(Axis.YP.rotationDegrees(giro + 180F));
        }
        ps.scale(0.9F, 0.9F, 0.9F);
        Minecraft.getInstance().getItemRenderer().renderStatic(GANCHO, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, ps, buf,
                g.level(), g.getId());
        ps.popPose();
        super.render(g, yaw, pt, ps, buf, light);
    }

    /** Dónde está la mano que avienta la cadena (en el mundo). */
    static Vec3 mano(Entity e, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (e instanceof Player p && p == mc.player && mc.options.getCameraType().isFirstPerson()) {
            Vec3 ojo = p.getEyePosition(pt);
            Vec3 mira = p.getViewVector(pt);
            Vec3 der = mira.cross(new Vec3(0, 1, 0));
            der = der.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : der.normalize();
            return ojo.add(mira.scale(0.5)).add(der.scale(0.32)).add(0, -0.28, 0);
        }
        float giro = e instanceof LivingEntity le ? Mth.rotLerp(pt, le.yBodyRotO, le.yBodyRot) : e.getYRot();
        Vec3 base = e.getPosition(pt);
        if (e instanceof Krampus) {
            Vec3 izq = Vec3.directionFromRotation(0, giro - 90F).scale(0.8);
            Vec3 frente = Vec3.directionFromRotation(0, giro).scale(0.7);
            return base.add(izq).add(frente).add(0, 1.8, 0);
        }
        Vec3 der = Vec3.directionFromRotation(0, giro + 90F).scale(0.36);
        return base.add(der).add(0, e.getBbHeight() * 0.68, 0);
    }

    private static void cadena(PoseStack ps, VertexConsumer vc, Vec3 d, int light) {
        double largo = d.length();
        if (largo < 0.05) {
            return;
        }
        Vec3 dir = d.scale(1 / largo);
        Vec3 a = dir.cross(new Vec3(0, 1, 0));
        if (a.lengthSqr() < 1.0E-4) {
            a = new Vec3(1, 0, 0);
        }
        a = a.normalize();
        Vec3 b = dir.cross(a).normalize();
        TextureAtlasSprite s = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(CADENA);
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float w = 1.5F / 16F;
        for (double t = 0; t < largo; t += 1.0) {
            double t1 = Math.min(largo, t + 1.0);
            float v0 = s.getV(0), v1 = s.getV((t1 - t) * 16);
            plano(vc, m, n, dir, a, t, t1, w, s.getU(0), s.getU(3), v0, v1, light);
            plano(vc, m, n, dir, b, t, t1, w, s.getU(3), s.getU(6), v0, v1, light);
        }
    }

    private static void plano(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 dir, Vec3 ancho, double t0, double t1, float w,
            float u0, float u1, float v0, float v1, int light) {
        Vec3 p0 = dir.scale(t0), p1 = dir.scale(t1);
        Vec3 o = ancho.scale(w);
        Vec3 nn = dir.cross(ancho);
        punto(vc, m, n, p0.subtract(o), u0, v0, nn, light);
        punto(vc, m, n, p0.add(o), u1, v0, nn, light);
        punto(vc, m, n, p1.add(o), u1, v1, nn, light);
        punto(vc, m, n, p1.subtract(o), u0, v1, nn, light);
    }

    private static void punto(VertexConsumer vc, Matrix4f m, Matrix3f n, Vec3 p, float u, float v, Vec3 nn, int light) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, (float) nn.x, (float) nn.y, (float) nn.z).endVertex();
    }

    @Override
    public boolean shouldRender(GanchoCadena g, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GanchoCadena g) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
