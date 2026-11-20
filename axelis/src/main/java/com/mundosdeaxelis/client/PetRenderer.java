package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Mascota;
import com.mundosdeaxelis.relic.Companions;
import com.mundosdeaxelis.relic.PetKinds;
import com.mundosdeaxelis.vfx.Cam;
import com.mundosdeaxelis.vfx.Creature3D;
import com.mundosdeaxelis.vfx.Sink;
import com.mundosdeaxelis.vfx.V3;
import com.mundosdeaxelis.vfx.VfxData;
import com.mundosdeaxelis.vfx.VfxTex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Dibuja una {@link Mascota} (etapa 254) con su modelo 3D de siempre ({@link Creature3D}), pero como cualquier criatura del juego:
 * con la luz del sitio donde está (de noche o en una cueva se oscurece; lo que brilla sigue brillando), la sombra redonda del
 * juego, el parpadeo rojo al recibir un golpe y su nombre encima cuando la miras. También la dibuja en su menú.
 */
public class PetRenderer extends EntityRenderer<Mascota> {
    /** Lo pone {@link PetScreen} mientras dibuja la vista previa del menú (otra cámara, sin nombre encima). */
    static boolean gui;

    private static final ResourceLocation[] TEX = new ResourceLocation[VfxTex.COUNT];

    static {
        for (int i = 0; i < VfxTex.COUNT; i++) {
            TEX[i] = new ResourceLocation(MundosDeAxelis.MODID, "textures/vfx/" + VfxTex.FILE[i] + ".png");
        }
    }

    public PetRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.3F;
        this.shadowStrength = 0.8F;
    }

    /**
     * Tipos de dibujo de entidad iguales a los del juego, pero con la textura filtrada suave (los ojos de las mascotas se ven
     * redondos de cerca; el filtro normal de entidades los dejaba con dientes de sierra).
     */
    static final class Types extends RenderType {
        private Types() {
            super("mundos_pet_dummy", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, false, () -> {
            }, () -> {
            });
        }

        static final java.util.function.Function<ResourceLocation, RenderType> SOLID = net.minecraft.Util.memoize(tex -> RenderType.create(
                "mundos_pet_solid", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                        .setTextureState(new TextureStateShard(tex, true, false))
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(true)));

        static final java.util.function.Function<ResourceLocation, RenderType> TRANS = net.minecraft.Util.memoize(tex -> RenderType.create(
                "mundos_pet_trans", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(tex, true, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(true)));
    }

    private static double ease(double x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    @Override
    public void render(Mascota e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        PetKinds.Kind k = e.kind();
        Companions.Spec spec = e.spec();
        if (k == null || spec == null) {
            return;
        }
        this.shadowRadius = Mth.clamp(k.w * 0.62F, 0.22F, 0.6F);
        VfxData d = e.anim;
        d.entity = true;
        d.b = spec.species();
        d.size = (float) spec.size();
        d.id = e.getId();
        d.rgb = 0xFFFFFF;
        d.light = 1F;
        if (!d.yawInit) {
            d.yawInit = true;
            d.x = d.px = e.getX();
            d.y = d.py = e.getY();
            d.z = d.pz = e.getZ();
            d.yaw = d.pyaw = -Math.toRadians(e.yBodyRot);
        }
        float body = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
        float head = Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot);
        float pitch = Mth.lerp(pt, e.xRotO, e.getXRot());
        d.eYaw = -Math.toRadians(body);
        d.eLookYaw = -Math.toRadians(Mth.wrapDegrees(head - body));
        d.eLookPitch = Math.toRadians(pitch);
        d.eLook = 1.0;
        // truco: da una vuelta entera
        d.eYawOff = e.trickT > 0 ? -2 * Math.PI * ease(1 - (e.trickT - pt) / 22.0) : 0;
        double age = e.tickCount + pt;
        d.life = e.leaveAge >= 0 ? (int) (e.leaveAge + 9) : 0;
        // al salir del agua se sacude
        d.eShake = e.shakeT > 0 ? Mth.clamp(1 - (e.shakeT - pt) / 30.0, 0, 1) : -1;
        boolean dying = e.deathTime > 0;
        if (dying) {
            // se cansó: se queda dormida (zetas) y se encoge antes de volver a su figurita
            d.mIdle = Creature3D.SLEEP_T + 60;
            d.life = e.tickCount - e.deathTime + 20;
        }

        Cam cam;
        if (gui) {
            // en el menú la criatura mira hacia quien mira la pantalla (hacia -Z del modelo)
            cam = new Cam(new V3(0, 0.45, -4.0), new V3(-1, 0, 0), new V3(0, 1, 0), new V3(0, 0, 1));
        } else {
            cam = MagmaRenderers.cam(this.entityRenderDispatcher.camera, e, pt);
        }
        final int overlay = dying ? OverlayTexture.NO_OVERLAY : LivingEntityRenderer.getOverlayCoords(e, 0.0F);
        final Matrix4f pose = ps.last().pose();
        final Matrix3f nrm = ps.last().normal();
        final int lit = light;
        Sink s = new Sink() {
            @Override
            public void quad(int tex, int blend, double[] p, float[] uv, float[] col) {
                put(tex, blend, p, uv, col, false);
            }

            @Override
            public void quadE(int tex, int blend, double[] p, float[] uv, float[] col, boolean emissive) {
                put(tex, blend, p, uv, col, emissive);
            }

            private void put(int tex, int blend, double[] p, float[] uv, float[] col, boolean emissive) {
                ResourceLocation rl = tex >= 0 && tex < TEX.length ? TEX[tex] : TEX[VfxTex.CREATURE3D];
                boolean add = blend == Sink.ADD;
                RenderType rt = add ? MagmaRenderers.Glow.TYPE.apply(rl) : (blend == Sink.SOLID ? Types.SOLID.apply(rl) : Types.TRANS.apply(rl));
                VertexConsumer vc = buf.getBuffer(rt);
                int li = add || emissive ? LightTexture.FULL_BRIGHT : lit;
                int ov = add ? OverlayTexture.NO_OVERLAY : overlay;
                for (int q = 0; q < 4; q++) {
                    vc.vertex(pose, (float) p[q * 3], (float) p[q * 3 + 1], (float) p[q * 3 + 2])
                            .color(Mth.clamp(col[q * 4], 0F, 1F), Mth.clamp(col[q * 4 + 1], 0F, 1F), Mth.clamp(col[q * 4 + 2], 0F, 1F),
                                    Mth.clamp(col[q * 4 + 3], 0F, 1F))
                            .uv(uv[q * 2], uv[q * 2 + 1]).overlayCoords(ov).uv2(li).normal(nrm, 0F, 1F, 0F).endVertex();
                }
            }
        };
        Creature3D.emitPet(d, age, pt, new V3(0, -k.yOff, 0), cam, s);
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    protected boolean shouldShowName(Mascota e) {
        if (gui || !e.hasCustomName() || e.leaving() || e.isDeadOrDying()) {
            return false;
        }
        return e.isCustomNameVisible() || (e == this.entityRenderDispatcher.crosshairPickEntity && Minecraft.renderNames());
    }

    @Override
    public ResourceLocation getTextureLocation(Mascota e) {
        return TEX[VfxTex.CREATURE3D];
    }
}
