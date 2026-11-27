package com.navidad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.navidad.Navidad;
import com.navidad.bloque.deco.Deco;
import com.navidad.entidad.Reno;
import com.navidad.entidad.TrineoSanta;
import com.navidad.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * El trineo de Santa en el cielo: el trineo de adorno en grande y, adelante, cuatro renos de verdad (el mismo modelo animado de la
 * criatura) galopando en el aire, siempre iluminados. Si algo falla con los renos animados, se dibujan los renos de adorno.
 */
public class TrineoRenderer extends EntityRenderer<TrineoSanta> {
    private final Reno[] renos = new Reno[4];
    private Level nivel;
    private int ultimoTick = -1;
    private boolean renosMalos;

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
        ps.pushPose();
        ps.scale(2.4F, 2.4F, 2.4F);
        ps.translate(-0.5, 0, -0.5);
        br.renderSingleBlock(trineo.defaultBlockState(), ps, buf, brillo, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        if (renosMalos || !renosVivos(e, rumbo, t, pt, ps, buf, brillo)) {
            ps.scale(2.4F, 2.4F, 2.4F);
            for (int i = 0; i < 4; i++) {
                ps.pushPose();
                float galope = Mth.sin(t * 0.5F + i * 1.3F) * 0.08F;
                ps.translate((i % 2 == 0 ? -0.35 : 0.35) - 0.5, 0.15 + galope, -1.3 - (i / 2) * 1.0 - 0.5);
                br.renderSingleBlock(reno.defaultBlockState(), ps, buf, brillo, OverlayTexture.NO_OVERLAY);
                ps.popPose();
            }
        }
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, luz);
    }

    /** Los renos animados, en pares, jalando el trineo. Devuelve false si no se pudieron dibujar. */
    private boolean renosVivos(TrineoSanta e, float rumbo, float t, float pt, PoseStack ps, MultiBufferSource buf, int brillo) {
        try {
            if (renos[0] == null || nivel != e.level()) {
                nivel = e.level();
                for (int i = 0; i < renos.length; i++) {
                    renos[i] = ModRegistry.RENO.get().create(nivel);
                    if (renos[i] == null) {
                        return false;
                    }
                }
            }
            boolean nuevoTick = e.tickCount != ultimoTick;
            ultimoTick = e.tickCount;
            var disp = Minecraft.getInstance().getEntityRenderDispatcher();
            for (int i = 0; i < renos.length; i++) {
                Reno r = renos[i];
                r.setPos(e.getX(), e.getY(), e.getZ());
                r.tickCount = e.tickCount + i * 7;
                if (nuevoTick) {
                    r.walkAnimation.update(1.1F, 0.4F);
                }
                r.setYRot(rumbo);
                r.yRotO = rumbo;
                r.setYBodyRot(rumbo);
                r.yBodyRotO = rumbo;
                r.setYHeadRot(rumbo);
                r.yHeadRotO = rumbo;
                EntityRenderer<? super Reno> rr = disp.getRenderer(r);
                ps.pushPose();
                // en el espacio del trineo (ya girado): -z es hacia adelante
                float galope = Mth.sin(t * 0.45F + i * 1.1F) * 0.12F;
                ps.translate(i % 2 == 0 ? -0.75 : 0.75, 0.55 + galope, -3.2 - (i / 2) * 2.3);
                ps.mulPose(Axis.YP.rotationDegrees(rumbo - 180));
                rr.render(r, rumbo, pt, ps, buf, brillo);
                ps.popPose();
            }
            return true;
        } catch (RuntimeException ex) {
            renosMalos = true;
            Navidad.LOGGER.warn("NAVIDAD_TRINEO renos animados no: {}", ex.toString());
            return false;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(TrineoSanta e) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
