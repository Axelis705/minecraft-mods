package com.navidad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.navidad.bloque.deco.Deco;
import com.navidad.entidad.TrineoSanta;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** El trineo de Santa en el cielo: el trineo de adorno en grande y cuatro renos de adorno galopando adelante (siempre iluminados). */
public class TrineoRenderer extends EntityRenderer<TrineoSanta> {
    public TrineoRenderer(EntityRendererProvider.Context c) {
        super(c);
    }

    @Override
    public void render(TrineoSanta e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int luz) {
        var br = Minecraft.getInstance().getBlockRenderer();
        Block trineo = Deco.block("trineo"), reno = Deco.block("reno_adorno");
        if (trineo == null || reno == null) {
            return;
        }
        Vec3 v = e.getDeltaMovement();
        float rumbo = (float) Math.toDegrees(Math.atan2(-v.x, v.z));
        int brillo = LightTexture.FULL_BRIGHT;
        float t = e.tickCount + pt;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-rumbo + 180));
        ps.scale(2.4F, 2.4F, 2.4F);
        ps.pushPose();
        ps.translate(-0.5, 0, -0.5);
        br.renderSingleBlock(trineo.defaultBlockState(), ps, buf, brillo, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        for (int i = 0; i < 4; i++) {
            ps.pushPose();
            float galope = Mth.sin(t * 0.5F + i * 1.3F) * 0.08F;
            ps.translate((i % 2 == 0 ? -0.35 : 0.35) - 0.5, 0.15 + galope, -1.3 - (i / 2) * 1.0 - 0.5);
            br.renderSingleBlock(reno.defaultBlockState(), ps, buf, brillo, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, luz);
    }

    @Override
    public ResourceLocation getTextureLocation(TrineoSanta e) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
