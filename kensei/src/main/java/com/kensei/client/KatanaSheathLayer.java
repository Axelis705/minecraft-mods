package com.kensei.client;

import com.kensei.combat.KatanaUtil;
import com.kensei.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * La katana guardada en su vaina, cruzada en la espalda del jugador cuando la lleva en el inventario pero no en la mano. El cliente solo
 * conoce el inventario del jugador local, así que por ahora se ve en uno mismo (en tercera persona).
 */
public class KatanaSheathLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ResourceLocation MODEL = new ResourceLocation("kensei", "item/katana_sheathed");
    private static ItemStack stack;

    public KatanaSheathLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private static boolean carriesKatana(AbstractClientPlayer p) {
        Minecraft mc = Minecraft.getInstance();
        if (p != mc.player || KatanaUtil.holdsKatana(p)) {
            return false;
        }
        for (ItemStack s : p.getInventory().items) {
            if (s.is(ModItems.KATANA.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffer, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (p.isInvisible() || p.isSpectator() || !carriesKatana(p)) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(MODEL);
        ps.pushPose();
        this.getParentModel().body.translateAndRotate(ps);
        // pegada a la espalda, en diagonal: el mango queda abajo a un lado y la punta sube por el hombro contrario
        ps.translate(0.0F, 0.36F, 0.2F);
        ps.mulPose(Axis.ZP.rotationDegrees(38.0F));
        ps.scale(0.5F, -0.5F, -0.5F);
        ps.translate(0.0F, -0.54F, 0.0F);
        if (stack == null) {
            stack = new ItemStack(ModItems.KATANA.get());
        }
        Minecraft.getInstance().getItemRenderer().render(stack, ItemDisplayContext.NONE, false, ps, buffer, light, OverlayTexture.NO_OVERLAY, model);
        ps.popPose();
    }
}
