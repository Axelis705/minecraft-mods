package com.axeliscore.vestir.client;

import com.axeliscore.plat.client.Draw;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
//#if MC >= 26.1
//$$ import net.minecraft.client.model.player.PlayerModel;
//$$ import net.minecraft.client.renderer.SubmitNodeCollector;
//$$ import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//$$ import net.minecraft.world.entity.Entity;
//#else
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
//#endif

/**
 * (Cliente) La capa que dibuja el {@link com.axeliscore.vestir.Vestuario} encima de cada jugador (se agrega a los dos modelos de jugador:
 * brazos normales y delgados). Sigue cada parte del cuerpo tal como la acomodó Minecraft en ese cuadro (caminar, agacharse, golpear...).
 */
//#if MC >= 26.1
//$$ public class CapaVestuario extends RenderLayer<AvatarRenderState, PlayerModel> {
//$$     public CapaVestuario(RenderLayerParent<AvatarRenderState, PlayerModel> p) {
//$$         super(p);
//$$     }
//$$
//$$     @Override
//$$     public void submit(PoseStack ps, SubmitNodeCollector out, int light, AvatarRenderState s, float yRot, float xRot) {
//$$         Minecraft mc = Minecraft.getInstance();
//$$         Entity e = mc.level == null ? null : mc.level.getEntity(s.id);
//$$         if (e == null || s.isInvisible || s.isSpectator) {
//$$             return;
//$$         }
//$$         PlayerModel m = getParentModel();
//$$         Draw d = new Draw(out);
//$$         VestuarioCliente.dibujarJugador(e.getUUID(), e, VestuarioCliente.esDelgado(s.skin), partes(m), ps, d, light,
//$$                 LivingEntityRenderer.getOverlayCoords(s, 0F), 1F, false);
//$$         d.flush();
//$$     }
//$$
//$$     static ModelPart[] partes(PlayerModel m) {
//$$         return new ModelPart[] {m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};
//$$     }
//$$ }
//#else
public class CapaVestuario extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public CapaVestuario(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> p) {
        super(p);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer e, float limb, float limbAmount, float pt, float age,
            float yaw, float pitch) {
        if (VestuarioCliente.de(e.getUUID()).isEmpty() || e.isSpectator()) {
            return;
        }
        float alfa = 1F;
        if (e.isInvisible()) {
            // invisible: nada para quien no lo ve; casi transparente para quien sí (como el cuerpo)
            if (Minecraft.getInstance().player == null || e.isInvisibleTo(Minecraft.getInstance().player)) {
                return;
            }
            alfa = 0.15F;
        }
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        Draw d = new Draw(buf);
        VestuarioCliente.dibujarJugador(e.getUUID(), e, VestuarioCliente.esDelgado(e), partes(m), ps, d, light,
                LivingEntityRenderer.getOverlayCoords(e, 0F), alfa, false);
        d.flush();
    }

    static ModelPart[] partes(PlayerModel<?> m) {
        return new ModelPart[] {m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};
    }
}
//#endif
