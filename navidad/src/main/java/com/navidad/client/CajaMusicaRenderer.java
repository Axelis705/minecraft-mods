package com.navidad.client;

import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.anim.client.GeoAssets;
import com.axeliscore.anim.client.GeoRenderer;
import com.axeliscore.plat.client.Draw;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.navidad.Navidad;
import com.navidad.musica.CajaMusicaBE;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * (Cliente) Dibuja la caja de música: la tapa que se abre hacia atrás (con un rebotito al final), el pinito que sale desde adentro
 * creciendo y gira sobre su espejo, la estrella que brilla y la manivela que da vueltas.
 */
public class CajaMusicaRenderer implements BlockEntityRenderer<CajaMusicaBE> {
    public static final ResourceLocation GEO = new ResourceLocation(Navidad.MODID, "geo/caja_musica.geo.json");
    public static final ResourceLocation TEX = new ResourceLocation(Navidad.MODID, "textures/block/caja_musica.png");
    private static Geo geoVisto;
    private static Pose pose;
    private static GeoRenderer.Frame frame;

    public CajaMusicaRenderer(BlockEntityRendererProvider.Context c) {
    }

    @Override
    public void render(CajaMusicaBE be, float pt, PoseStack ps, MultiBufferSource buf, int luz, int overlay) {
        Direction d = be.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? be.getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        dibujar(ps, buf, luz, overlay, d, Mth.lerp(pt, be.abiertaAntes, be.abierta), Mth.lerp(pt, be.giroAntes, be.giro));
    }

    /** También la usa el objeto (cerrada). */
    static void dibujar(PoseStack ps, MultiBufferSource buf, int luz, int overlay, Direction d, float abierta, float giro) {
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
        // la tapa: sale rápido y rebota tantito al llegar
        float k = Mth.clamp(abierta, 0, 1);
        float suave = k < 1 ? 1 - (1 - k) * (1 - k) * (1 - k) : 1;
        float rebote = k > 0.85F ? Mth.sin((k - 0.85F) / 0.15F * Mth.PI) * 0.06F : 0F;
        int tapa = geo.bone("tapa");
        if (tapa >= 0) {
            pose.addRot(tapa, -(105F * suave + rebote * 40F) * Mth.DEG_TO_RAD, 0, 0);
        }
        // el pinito sale de adentro creciendo cuando la tapa ya va abierta
        int esc = geo.bone("escenario");
        if (esc >= 0) {
            float sale = Mth.clamp((k - 0.35F) / 0.55F, 0, 1);
            if (sale <= 0.01F) {
                pose.hide(esc, true);
            } else {
                float s = sale * sale * (3 - 2 * sale);
                pose.mulScale(esc, s, s, s);
                pose.addRot(esc, 0, giro * Mth.DEG_TO_RAD, 0);
            }
        }
        int man = geo.bone("manivela");
        if (man >= 0) {
            pose.addRot(man, giro * 2.2F * Mth.DEG_TO_RAD, 0, 0);
        }
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(180F - d.toYRot()));   // el frente del modelo (-z) mira hacia donde apunta la caja
        ps.translate(-0.5F, 0F, -0.5F);
        ps.translate(0.5F, 0F, 0.5F);
        ps.scale(-1F, -1F, 1F);
        ps.translate(0F, -1.501F, 0F);
        GeoRenderer.compute(geo, pose, ps, frame);
        Draw dr = new Draw(buf);
        GeoRenderer.draw(geo, pose, frame, dr.cutout(TEX), GeoRenderer.Pass.NORMAL, luz, overlay, 0xFFFFFFFF);
        GeoRenderer.draw(geo, pose, frame, dr.glow(TEX), GeoRenderer.Pass.GLOW, LightTexture.FULL_BRIGHT, overlay, 0xFFFFFFFF);
        ps.popPose();
    }
}
