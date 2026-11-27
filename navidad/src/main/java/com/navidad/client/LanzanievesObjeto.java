package com.navidad.client;

import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.anim.client.GeoAssets;
import com.axeliscore.anim.client.GeoRenderer;
import com.axeliscore.plat.client.Draw;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.navidad.Navidad;
import com.navidad.nieve.Lanzanieves;
import com.navidad.nieve.Municion;
import com.navidad.nieve.TanqueNieve;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.HashMap;
import java.util.Map;

/**
 * (Cliente) Cómo se ve el lanzanieves: el modelo 3D con los cañones girando (toman vuelo al apretar, frenan poco a poco al soltar y van
 * al revés cuando aspira nieve), el culatazo y el destello en cada tiro, la aguja del manómetro y la nieve de la ventanita según lo que
 * queda en el tanque. Se agarra con las dos manos; en primera persona da un golpecito hacia atrás en cada disparo.
 */
public final class LanzanievesObjeto extends BlockEntityWithoutLevelRenderer {
    public static final ResourceLocation GEO = new ResourceLocation(Navidad.MODID, "geo/lanzanieves.geo.json");
    public static final ResourceLocation TEX = new ResourceLocation(Navidad.MODID, "textures/item/lanzanieves_3d.png");
    /** Quién se está dibujando (lo pone el evento de dibujo de entidades; null = el jugador de este cliente). */
    public static LivingEntity portador;
    private static final Map<Integer, float[]> GIROS = new HashMap<>();
    private static IClientItemExtensions ext;
    private static Geo geoVisto;
    private static Pose pose;
    private static GeoRenderer.Frame frame;

    private LanzanievesObjeto() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static IClientItemExtensions extension() {
        if (ext == null) {
            ext = new IClientItemExtensions() {
                private BlockEntityWithoutLevelRenderer r;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (r == null) {
                        r = new LanzanievesObjeto();
                    }
                    return r;
                }

                @Override
                public HumanoidModel.ArmPose getArmPose(LivingEntity e, InteractionHand h, ItemStack st) {
                    return HumanoidModel.ArmPose.CROSSBOW_HOLD;
                }

                @Override
                public boolean applyForgeHandTransform(PoseStack ps, LocalPlayer p, HumanoidArm arm, ItemStack st, float pt, float equip,
                        float swing) {
                    int lado = arm == HumanoidArm.RIGHT ? 1 : -1;
                    float culatazo = culatazo(p, pt);
                    float c = p.isUsingItem() ? 1 : 0;
                    ps.translate(lado * 0.38F, -0.25F + equip * -0.6F, -0.86F + culatazo * 0.07F);
                    ps.mulPose(Axis.XP.rotationDegrees(culatazo * 4F + c * Mth.sin((p.tickCount + pt) * 0.9F) * 0.4F));
                    return true;
                }
            };
        }
        return ext;
    }

    /** Qué tanto retrocede por el último tiro (1 justo al disparar, 0 al rato). */
    static float culatazo(LivingEntity e, float pt) {
        if (!(e instanceof Player p) || !p.isUsingItem() || !(p.getUseItem().getItem() instanceof Lanzanieves) || p.isShiftKeyDown()
                || Lanzanieves.aspirando(p)) {
            return 0F;
        }
        int t = p.getTicksUsingItem();
        if (t < Lanzanieves.ARRANQUE) {
            return 0F;
        }
        float desde = (t - Lanzanieves.ARRANQUE) % Lanzanieves.CADA + pt;
        return Math.max(0F, 1F - desde / 2F);
    }

    /** El giro de los cañones de alguien (sube de velocidad al apretar, frena al soltar). */
    private static float giro(LivingEntity e) {
        Minecraft mc = Minecraft.getInstance();
        float[] g = GIROS.computeIfAbsent(e == null ? -1 : e.getId(), k -> new float[]{0, 0, System.nanoTime() / 1e9F});
        float ahora = System.nanoTime() / 1e9F;
        float dt = Math.min(0.1F, ahora - g[2]);
        g[2] = ahora;
        float objetivo = 0;
        if (e instanceof Player p && p.isUsingItem() && p.getUseItem().getItem() instanceof Lanzanieves) {
            objetivo = p.isShiftKeyDown() ? 3F : Lanzanieves.aspirando(p) ? -9F : 14F;
        }
        g[1] += (objetivo - g[1]) * Math.min(1, dt * (objetivo == 0 ? 1.6F : 5F));
        g[0] += g[1] * dt;
        if (GIROS.size() > 64) {
            GIROS.clear();
        }
        return g[0];
    }

    private static boolean preparar() {
        Geo geo = GeoAssets.geo(GEO);
        if (geo == null || geo.bones.length == 0) {
            return false;
        }
        if (geo != geoVisto) {
            geoVisto = geo;
            pose = new Pose(geo.bones.length);
            frame = new GeoRenderer.Frame(geo);
        }
        return true;
    }

    @Override
    public void renderByItem(ItemStack st, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buf, int luz, int overlay) {
        if (!preparar()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LivingEntity quien = ctx.firstPerson() ? mc.player : portador;
        boolean enMano = quien != null && (ctx.firstPerson() || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        float pt = mc.getFrameTime();
        Geo geo = geoVisto;
        pose.reset();
        float carga = 0.6F;
        float culata = 0F;
        boolean cargando = false;
        if (enMano) {
            culata = culatazo(quien, pt);
            if (quien instanceof Player p) {
                ItemStack t = Municion.tanque(p);
                int total = Municion.total(p);
                carga = Mth.clamp(total / (float) TanqueNieve.LLENO, 0F, 1F);
                if (t.isEmpty() && total > 0) {
                    carga = Mth.clamp(total / 64F, 0, 1);
                }
                cargando = p.isUsingItem() && p.isShiftKeyDown() && p.getUseItem().getItem() instanceof Lanzanieves;
            }
            int b = geo.bone("canones");
            if (b >= 0) {
                pose.addRot(b, 0, 0, giro(quien));
            }
        }
        int cuerpo = geo.bone("cuerpo");
        if (cuerpo >= 0 && culata > 0) {
            pose.addPos(cuerpo, 0, 0, culata * 0.8F);
        }
        int aguja = geo.bone("aguja");
        if (aguja >= 0) {
            pose.addRot(aguja, Mth.lerp(carga, 1.1F, -1.1F), 0, 0);
        }
        int nieve = geo.bone("nieve_arma");
        if (nieve >= 0) {
            pose.mulScale(nieve, 1, Math.max(0.08F, carga), 1);
        }
        int boca = geo.bone("boca");
        if (boca >= 0) {
            float brillo = cargando && quien != null ? 0.5F + 0.5F * Mth.sin((quien.tickCount + pt) * 0.8F) : culata;
            int v = (int) (90 + 165 * Mth.clamp(brillo, 0, 1));
            pose.tinte(boca, 0xFF000000 | v << 16 | v << 8 | v);
        }
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.scale(-1F, -1F, 1F);
        ps.translate(0F, -1.501F, 0F);
        GeoRenderer.compute(geo, pose, ps, frame);
        Draw d = new Draw(buf);
        GeoRenderer.draw(geo, pose, frame, d.cutout(TEX), GeoRenderer.Pass.NORMAL, luz, overlay, 0xFFFFFFFF);
        GeoRenderer.draw(geo, pose, frame, d.glow(TEX), GeoRenderer.Pass.GLOW, LightTexture.FULL_BRIGHT, overlay, 0xFFFFFFFF);
        ps.popPose();
    }
}
