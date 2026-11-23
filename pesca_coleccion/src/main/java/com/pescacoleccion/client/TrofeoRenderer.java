package com.pescacoleccion.client;

import com.axeliscore.plat.Ids;
import com.axeliscore.plat.client.CoreBER;
import com.axeliscore.plat.client.Draw;
import com.axeliscore.plat.client.VB;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.deco.TrofeoBE;
import com.pescacoleccion.deco.TrofeoBlock;
import com.pescacoleccion.pesca.Especie;
import com.pescacoleccion.pesca.PezItem;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/** (Cliente) Dibuja el pez montado en la placa (con su sombra) y su nombre y tamaño en la plaquita de latón. */
public class TrofeoRenderer extends CoreBER<TrofeoBE> {
    public TrofeoRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    static ResourceLocation arte(Especie e) {
        return Ids.rl(PescaColeccion.MODID, "textures/gui/peces/" + e.id() + ".png");
    }

    /** Un cuadro con la textura entera, mirando hacia -z (al frente de la placa); {@code espejo} lo voltea de lado. */
    static void cuadro(VB vb, PoseStack.Pose p, float x0, float y0, float x1, float y1, float z, int light, int r, int g, int b, int a,
            boolean espejo) {
        float u0 = espejo ? 1 : 0, u1 = espejo ? 0 : 1;
        vb.v(p, x1, y0, z, r, g, b, a, u0, 1, OverlayTexture.NO_OVERLAY, light, 0, 0, -1);
        vb.v(p, x0, y0, z, r, g, b, a, u1, 1, OverlayTexture.NO_OVERLAY, light, 0, 0, -1);
        vb.v(p, x0, y1, z, r, g, b, a, u1, 0, OverlayTexture.NO_OVERLAY, light, 0, 0, -1);
        vb.v(p, x1, y1, z, r, g, b, a, u0, 0, OverlayTexture.NO_OVERLAY, light, 0, 0, -1);
    }

    @Override
    protected void draw(TrofeoBE be, float partial, PoseStack ps, Draw d, int light, int overlay) {
        if (be.pez.isEmpty() || !(be.pez.getItem() instanceof PezItem pz) || pz.especie() == null) {
            return;
        }
        Especie e = pz.especie();
        Direction f = be.getBlockState().getValue(TrofeoBlock.FACING);
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-(f.toYRot() + 180F)));
        ResourceLocation tex = arte(e);
        float s = 0.36F, yc = 0.1F;
        // sombra suave sobre la madera y el pez encima (un poco salido de la placa)
        VB sombra = d.translucent(tex);
        cuadro(sombra, ps.last(), -s + 0.025F, yc - s - 0.025F, s + 0.025F, yc + s - 0.025F, 0.392F, light, 0, 0, 0, 110, false);
        VB pez = d.cutout(tex);
        cuadro(pez, ps.last(), -s, yc - s, s, yc + s, 0.386F, light, 255, 255, 255, 255, false);
        // plaquita de latón: nombre y tamaño
        float cm = PezItem.cm(be.pez);
        String txt = be.pez.getHoverName().getString() + (cm > 0 ? String.format(Locale.ROOT, cm >= 100 ? " · %.0f cm" : " · %.1f cm", cm) : "");
        int w = Draw.font().width(txt);
        float k = Math.min(0.0055F, 0.66F / Math.max(1, w));
        ps.pushPose();
        ps.translate(0, -0.33F, 0.395F);
        ps.mulPose(Axis.YP.rotationDegrees(180));
        ps.scale(k, -k, k);
        d.text(ps, txt, -w / 2F, -4, 0xFF2A1A08, 0, light);
        ps.popPose();
        ps.popPose();
    }
}
