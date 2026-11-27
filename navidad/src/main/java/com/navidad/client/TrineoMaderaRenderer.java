package com.navidad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.navidad.Navidad;
import com.navidad.entidad.TrineoMadera;
import com.navidad.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;

/** El trineo de madera: el modelo hecho con el kit de adornos (models/block/trineo_madera.json), girado hacia donde va. */
public class TrineoMaderaRenderer extends EntityRenderer<TrineoMadera> {
    public static final ResourceLocation MODELO = new ResourceLocation(Navidad.MODID, "block/trineo_madera");

    public TrineoMaderaRenderer(EntityRendererProvider.Context c) {
        super(c);
        this.shadowRadius = 0.6F;
    }

    @Override
    public void render(TrineoMadera e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int luz) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel m = mc.getModelManager().getModel(MODELO);
        ps.pushPose();
        float rumbo = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(180F - rumbo));
        // se ladea hacia adentro de la curva y tiembla cuando le pegan
        ps.mulPose(Axis.ZP.rotationDegrees(-e.ladeo(pt)));
        float g = e.golpe(pt);
        if (g > 0) {
            ps.mulPose(Axis.ZP.rotationDegrees(Mth.sin(g * 1.4F) * g * 0.9F));
        }
        ps.translate(-0.5, 0, -0.5);
        mc.getBlockRenderer().getModelRenderer().renderModel(ps.last(), buf.getBuffer(Sheets.cutoutBlockSheet()), null, m, 1F, 1F, 1F, luz,
                OverlayTexture.NO_OVERLAY);
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, luz);
    }

    @Override
    public ResourceLocation getTextureLocation(TrineoMadera e) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    /** Arranca el sonido de deslizarse de un trineo que acaba de aparecer en el mundo. */
    static void sonar(TrineoMadera t) {
        Minecraft.getInstance().getSoundManager().play(new Desliza(t));
    }

    /** El roce de los patines: sigue al trineo, en silencio si está quieto; en el hielo suena más agudo. */
    static final class Desliza extends AbstractTickableSoundInstance {
        private final TrineoMadera t;

        Desliza(TrineoMadera t) {
            super(ModRegistry.TRINEO_DESLIZA.get(), SoundSource.NEUTRAL, RandomSource.create());
            this.t = t;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0001F;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.x = t.getX();
            this.y = t.getY();
            this.z = t.getZ();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (t.isRemoved()) {
                stop();
                return;
            }
            x = t.getX();
            y = t.getY();
            z = t.getZ();
            double r = t.onGround() && t.suelo().resbala() ? t.rapidez() : t.onGround() ? t.rapidez() * 0.4 : 0;
            float objetivo = r < 0.03 ? 0F : (float) Mth.clamp(r * 1.3, 0.05, 1.0);
            volume = Mth.lerp(0.35F, volume, objetivo);
            boolean hielo = t.suelo() == TrineoMadera.Suelo.HIELO || t.suelo() == TrineoMadera.Suelo.HIELO_AZUL;
            pitch = (float) Mth.clamp((hielo ? 1.25 : 0.85) + r * 0.4, 0.5, 2.0);
        }
    }
}
