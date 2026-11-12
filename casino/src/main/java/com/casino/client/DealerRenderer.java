package com.casino.client;

import com.casino.plat.Ids;
import com.casino.Casino;
import com.casino.entity.DealerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.List;

/**
 * El crupier se dibuja como una persona (modelo de jugador) con su propia piel. Cuando dice un consejo levanta la mano derecha y sobre su
 * cabeza aparece un globo de texto.
 */
public class DealerRenderer extends MobRenderer<DealerEntity, PlayerModel<DealerEntity>> {
    private static final ResourceLocation TEXTURE = Ids.rl(Casino.MODID, "textures/entity/dealer.png");

    private static final ResourceLocation TEXTURE_ROULETTE = Ids.rl(Casino.MODID, "textures/entity/dealer_roulette.png");

    private static final ResourceLocation TEXTURE_POKER = Ids.rl(Casino.MODID, "textures/entity/dealer_poker.png");

    /** El modelo de jugador, pero con la mano derecha en alto mientras hay un consejo en el aire. */
    private static class DealerModel extends PlayerModel<DealerEntity> {
        DealerModel(ModelPart part) {
            super(part, false);
        }

        @Override
        public void setupAnim(DealerEntity e, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
            super.setupAnim(e, limbSwing, limbSwingAmount, age, yaw, pitch);
            if (e.getTip() >= 0) {
                this.rightArm.xRot = -2.85F + Mth.sin(age * 0.25F) * 0.1F;
                this.rightArm.zRot = 0.15F + Mth.sin(age * 0.5F) * 0.08F;
                this.rightSleeve.copyFrom(this.rightArm);
            }
        }
    }

    public DealerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DealerModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(DealerEntity entity) {
        return switch (entity.getGame()) {
            case DealerEntity.GAME_ROULETTE -> TEXTURE_ROULETTE;
            case DealerEntity.GAME_POKER -> TEXTURE_POKER;
            default -> TEXTURE;
        };
    }

    @Override
    public void render(DealerEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        super.render(e, yaw, partial, ps, buf, light);
        int tip = e.getTip();
        if (tip >= 0) {
            drawBubble(e, tip, ps, buf);
        }
    }

    private void drawBubble(DealerEntity e, int tip, PoseStack ps, MultiBufferSource buf) {
        String key = (e.getGame() == DealerEntity.GAME_ROULETTE ? "casino.tip.roulette." : e.getGame() == DealerEntity.GAME_POKER ? "casino.tip.poker." : "casino.tip.blackjack.") + tip;
        Font font = getFont();
        List<FormattedCharSequence> lines = font.split(Component.translatable(key), 130);
        ps.pushPose();
        ps.translate(0.0, e.getBbHeight() + 0.55, 0.0);
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
        ps.scale(-0.02F, -0.02F, 0.02F);
        Matrix4f m = ps.last().pose();
        int n = lines.size();
        // título dorado y, debajo, el texto (todo con fondo claro, como un globo)
        Component title = Component.translatable("casino.tip.title").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD);
        int bg = 0xF0FFF6DC;
        float y = -(n + 1) * 10.0F;
        font.drawInBatch(title, -font.width(title) / 2.0F, y, 0x000000, false, m, buf, Font.DisplayMode.NORMAL, bg, 0xF000F0);
        for (int i = 0; i < n; i++) {
            FormattedCharSequence l = lines.get(i);
            y = -(n - i) * 10.0F;
            font.drawInBatch(l, -font.width(l) / 2.0F, y, 0x202020, false, m, buf, Font.DisplayMode.NORMAL, bg, 0xF000F0);
        }
        ps.popPose();
    }
}
