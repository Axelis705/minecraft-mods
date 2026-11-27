package com.navidad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.navidad.Navidad;
import com.navidad.bloque.deco.Deco;
import com.navidad.entidad.RegaloCayendo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;

/**
 * El regalo que cae del trineo de Santa: el adorno de regalo (rojo, verde, azul o dorado) con el paracaídas encima
 * (models/block/paracaidas.json). El paracaídas se abre de golpe (crece con un rebote) y todo se mece colgado del domo.
 */
public class RegaloCayendoRenderer extends EntityRenderer<RegaloCayendo> {
    public static final ResourceLocation PARACAIDAS = new ResourceLocation(Navidad.MODID, "block/paracaidas");
    /** Las cajas de regalo enteras (las mismas que quedan al aterrizar). */
    public static final ResourceLocation[] CAJA = new ResourceLocation[4];

    static {
        for (int c = 0; c < 4; c++) {
            CAJA[c] = new ResourceLocation(Navidad.MODID, "block/caja_regalo_" + c);
        }
    }

    public RegaloCayendoRenderer(EntityRendererProvider.Context c) {
        super(c);
        this.shadowRadius = 0.4F;
    }

    @Override
    public void render(RegaloCayendo e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int luz) {
        Minecraft mc = Minecraft.getInstance();
        float t = e.tickCount + pt;
        ps.pushPose();
        // colgado del domo: se mece alrededor de un punto arriba
        float abierto = 0F;
        if (e.abierto() && e.abiertoDesde >= 0) {
            float k = Mth.clamp((t - e.abiertoDesde) / 7F, 0F, 1F);
            // crece con un rebote (se pasa un poco y regresa)
            abierto = k < 1F ? (float) (1 - Math.cos(k * Math.PI * 1.5) * (1 - k)) : 1F;
        }
        float meneo = abierto * Mth.sin(t * 0.07F) * 9F, meneo2 = abierto * Mth.cos(t * 0.055F) * 6F;
        ps.translate(0, 1.9, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(meneo));
        ps.mulPose(Axis.XP.rotationDegrees(meneo2));
        ps.translate(0, -1.9, 0);
        ps.mulPose(Axis.YP.rotationDegrees(e.getYRot()));
        ps.scale(0.8F, 0.8F, 0.8F);
        ps.translate(-0.5, 0, -0.5);
        mc.getBlockRenderer().getModelRenderer().renderModel(ps.last(), buf.getBuffer(Sheets.cutoutBlockSheet()), null,
                mc.getModelManager().getModel(CAJA[Math.floorMod(e.color(), 4)]), 1F, 1F, 1F, luz, OverlayTexture.NO_OVERLAY);
        if (abierto > 0.02F) {
            BakedModel m = mc.getModelManager().getModel(PARACAIDAS);
            ps.translate(0.5, 1.375, 0.5);
            ps.scale(abierto, 0.4F + 0.6F * Math.min(1F, abierto), abierto);
            ps.translate(-0.5, -1.375, -0.5);
            mc.getBlockRenderer().getModelRenderer().renderModel(ps.last(), buf.getBuffer(Sheets.cutoutBlockSheet()), null, m, 1F, 1F, 1F, luz,
                    OverlayTexture.NO_OVERLAY);
        }
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, luz);
    }

    @Override
    public ResourceLocation getTextureLocation(RegaloCayendo e) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
