package com.navidad.client;

import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.anim.client.GeoAssets;
import com.axeliscore.anim.client.GeoRenderer;
import com.axeliscore.plat.client.Draw;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.navidad.Navidad;
import com.navidad.nieve.Lanzanieves;
import com.navidad.nieve.TanqueNieve;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * (Cliente) El tanque de nieve en la espalda de quien lo trae puesto (sigue al cuerpo al agacharse o caminar), con la nieve de su
 * ventanita según lo que le queda; y si además lleva el lanzanieves en la mano, la manguera de bastón de caramelo (roja y blanca) que
 * baja del tanque y llega hasta el arma, colgando un poquito.
 */
public class TanqueCapa extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation GEO = new ResourceLocation(Navidad.MODID, "geo/tanque_nieve.geo.json");
    public static final ResourceLocation TEX = new ResourceLocation(Navidad.MODID, "textures/entity/tanque_nieve.png");
    private Geo geoVisto;
    private Pose pose;
    private GeoRenderer.Frame frame;

    public TanqueCapa(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre) {
        super(padre);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int luz, AbstractClientPlayer p, float limb, float limbAmount, float pt, float edad,
            float yaw, float pitch) {
        ItemStack t = p.getItemBySlot(EquipmentSlot.CHEST);
        if (!(t.getItem() instanceof TanqueNieve) || p.isInvisible()) {
            return;
        }
        Geo geo = GeoAssets.geo(GEO);
        if (geo == null || geo.bones.length == 0) {
            return;
        }
        if (geo != geoVisto) {
            geoVisto = geo;
            pose = new Pose(geo.bones.length);
            frame = new GeoRenderer.Frame(geo);
        }
        pose.reset();
        int nieve = geo.bone("nieve");
        if (nieve >= 0) {
            pose.mulScale(nieve, 1, Math.max(0.04F, Math.min(1F, TanqueNieve.carga(t) / (float) TanqueNieve.LLENO)), 1);
        }
        ModelPart cuerpo = getParentModel().body;
        ps.pushPose();
        cuerpo.translateAndRotate(ps);
        Matrix4f[] base = {new Matrix4f(ps.last().pose())};
        Matrix3f[] baseN = {new Matrix3f(ps.last().normal())};
        ps.popPose();
        GeoRenderer.computeRaices(geo, pose, base, baseN, frame);
        Draw d = new Draw(buf);
        GeoRenderer.draw(geo, pose, frame, d.cutout(TEX), GeoRenderer.Pass.NORMAL, luz, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        GeoRenderer.draw(geo, pose, frame, d.glow(TEX), GeoRenderer.Pass.GLOW, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        // la manguera hasta el lanzanieves
        boolean derecha = p.getMainArm() == HumanoidArm.RIGHT;
        if (p.getMainHandItem().getItem() instanceof Lanzanieves) {
            manguera(ps, buf, luz, cuerpo, derecha ? getParentModel().rightArm : getParentModel().leftArm, derecha);
        }
    }

    /** Un punto de una parte del cuerpo (en píxeles de modelo, relativo a su eje) en el espacio del modelo. */
    private static Vector3f punto(ModelPart parte, float x, float y, float z) {
        PoseStack tmp = new PoseStack();
        parte.translateAndRotate(tmp);
        Vector4f v = tmp.last().pose().transform(new Vector4f(x / 16F, y / 16F, z / 16F, 1F));
        return new Vector3f(v.x(), v.y(), v.z());
    }

    private static void manguera(PoseStack ps, MultiBufferSource buf, int luz, ModelPart cuerpo, ModelPart brazo, boolean derecha) {
        // de la boquilla del tanque (abajo, del lado de la mano del arma) a la mano
        Vector3f a = punto(cuerpo, derecha ? -2.7F : 2.7F, 14.6F, 4.7F);
        Vector3f b = punto(brazo, derecha ? -1F : 1F, 10.5F, 1.5F);
        Vector3f c = new Vector3f((a.x + b.x) / 2F, Math.max(a.y, b.y) + 0.32F, (a.z + b.z) / 2F + 0.18F);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(Draw.BLANCO));
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        int partes = 14;
        float r = 0.034F;
        Vector3f antes = null;
        Vector3f[] anillo0 = null;
        for (int i = 0; i <= partes; i++) {
            float k = i / (float) partes;
            float u = 1 - k;
            Vector3f q = new Vector3f(u * u * a.x + 2 * u * k * c.x + k * k * b.x, u * u * a.y + 2 * u * k * c.y + k * k * b.y,
                    u * u * a.z + 2 * u * k * c.z + k * k * b.z);
            if (antes != null) {
                Vector3f dir = new Vector3f(q).sub(antes).normalize();
                Vector3f ref = Math.abs(dir.y) < 0.9F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
                Vector3f s1 = new Vector3f(dir).cross(ref).normalize().mul(r);
                Vector3f s2 = new Vector3f(dir).cross(s1).normalize().mul(r);
                Vector3f[] anillo = {new Vector3f(q).add(s1), new Vector3f(q).add(s2), new Vector3f(q).sub(s1), new Vector3f(q).sub(s2)};
                if (anillo0 == null) {
                    anillo0 = new Vector3f[]{new Vector3f(antes).add(s1), new Vector3f(antes).add(s2), new Vector3f(antes).sub(s1),
                            new Vector3f(antes).sub(s2)};
                }
                boolean blanco = i % 2 == 0;
                int cr = blanco ? 244 : 214, cg = blanco ? 244 : 30, cb = blanco ? 248 : 42;
                for (int j = 0; j < 4; j++) {
                    Vector3f p0 = anillo0[j], p1 = anillo0[(j + 1) % 4], p2 = anillo[(j + 1) % 4], p3 = anillo[j];
                    Vector3f nn = new Vector3f(p0).add(p3).mul(0.5F).sub(antes).normalize();
                    for (Vector3f v : new Vector3f[]{p0, p1, p2, p3}) {
                        vc.vertex(m, v.x, v.y, v.z).color(cr, cg, cb, 255).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz)
                                .normal(n, nn.x, nn.y, nn.z).endVertex();
                    }
                }
                anillo0 = anillo;
            }
            antes = q;
        }
    }
}
