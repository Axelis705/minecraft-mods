package com.casino.client;

import com.casino.Casino;
import com.casino.entity.DealerEntity;
import com.casino.plat.Ids;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
//#if MC >= 26.1
//$$ import net.minecraft.client.model.player.PlayerModel;
//$$ import net.minecraft.client.renderer.SubmitNodeCollector;
//$$ import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//$$ import net.minecraft.client.renderer.state.level.CameraRenderState;
//#elif MC >= 1.21.2
//$$ import net.minecraft.client.model.PlayerModel;
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//$$ import net.minecraft.client.renderer.entity.state.PlayerRenderState;
//#else
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
//#endif

import java.util.List;

/**
 * El crupier se dibuja como una persona (modelo de jugador) con su propia piel. Cuando dice un consejo levanta la mano derecha y sobre su
 * cabeza aparece un globo de texto. Desde 1.21.2 Minecraft dibuja a partir de un "estado" copiado de la entidad; desde 26.x lo entrega.
 */
//#if MC >= 26.1
//$$ public class DealerRenderer extends MobRenderer<DealerEntity, DealerRenderer.State, DealerRenderer.DealerModel> {
//$$     public static class State extends AvatarRenderState {
//$$         int tip = -1;
//$$         int game;
//$$     }
//#elif MC >= 1.21.2
//$$ public class DealerRenderer extends MobRenderer<DealerEntity, DealerRenderer.State, DealerRenderer.DealerModel> {
//$$     public static class State extends PlayerRenderState {
//$$         int tip = -1;
//$$         int game;
//$$     }
//#else
public class DealerRenderer extends MobRenderer<DealerEntity, PlayerModel<DealerEntity>> {
//#endif
    private static final ResourceLocation TEXTURE = Ids.rl(Casino.MODID, "textures/entity/dealer.png");
    private static final ResourceLocation TEXTURE_ROULETTE = Ids.rl(Casino.MODID, "textures/entity/dealer_roulette.png");
    private static final ResourceLocation TEXTURE_POKER = Ids.rl(Casino.MODID, "textures/entity/dealer_poker.png");
    private static final ResourceLocation TEXTURE_GUARD = Ids.rl(Casino.MODID, "textures/entity/dealer_guard.png");

    private static ResourceLocation texture(int game) {
        return switch (game) {
            case DealerEntity.GAME_ROULETTE -> TEXTURE_ROULETTE;
            case DealerEntity.GAME_POKER -> TEXTURE_POKER;
            case DealerEntity.GAME_GUARD -> TEXTURE_GUARD;
            default -> TEXTURE;
        };
    }

    private static String tipKey(int game, int tip) {
        return (game == DealerEntity.GAME_ROULETTE ? "casino.tip.roulette." : game == DealerEntity.GAME_POKER ? "casino.tip.poker."
                : game == DealerEntity.GAME_GUARD ? "casino.tip.guard."
                : "casino.tip.blackjack.") + tip;
    }

    //#if MC >= 1.21.2
    //$$ /** El modelo de jugador, pero con la mano derecha en alto mientras hay un consejo en el aire. */
    //$$ public static class DealerModel extends PlayerModel {
    //$$     DealerModel(ModelPart part) {
    //$$         super(part, false);
    //$$     }
    //$$
    //$$     @Override
    //#if MC >= 26.1
    //$$     public void setupAnim(AvatarRenderState s) {
    //#else
    //$$     public void setupAnim(PlayerRenderState s) {
    //#endif
    //$$         super.setupAnim(s);
    //$$         if (s instanceof State d && d.tip >= 0) {
    //$$             this.rightArm.xRot = -2.85F + Mth.sin(s.ageInTicks * 0.25F) * 0.1F;
    //$$             this.rightArm.zRot = 0.15F + Mth.sin(s.ageInTicks * 0.5F) * 0.08F;
    //$$         }
    //$$     }
    //$$ }
    //$$
    //$$ public DealerRenderer(EntityRendererProvider.Context ctx) {
    //$$     super(ctx, new DealerModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.45F);
    //$$ }
    //$$
    //$$ @Override
    //$$ public State createRenderState() {
    //$$     return new State();
    //$$ }
    //$$
    //$$ @Override
    //$$ public void extractRenderState(DealerEntity e, State s, float partial) {
    //$$     super.extractRenderState(e, s, partial);
    //$$     s.tip = e.getTip();
    //$$     s.game = e.getGame();
    //#if MC >= 26.1
    //$$     // la ropa del crupier está en la capa exterior de la piel (chaqueta, mangas, sombrero): en 26.x viene apagada
    //$$     s.showHat = true;
    //$$     s.showJacket = true;
    //$$     s.showLeftSleeve = true;
    //$$     s.showRightSleeve = true;
    //$$     s.showLeftPants = true;
    //$$     s.showRightPants = true;
    //#endif
    //$$ }
    //$$
    //$$ @Override
    //$$ public ResourceLocation getTextureLocation(State s) {
    //$$     return texture(s.game);
    //$$ }
    //#else
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
        return texture(entity.getGame());
    }
    //#endif

    //#if MC >= 26.1
    //$$ @Override
    //$$ public void submit(State s, PoseStack ps, SubmitNodeCollector out, CameraRenderState cam) {
    //$$     super.submit(s, ps, out, cam);
    //$$     if (s.tip >= 0) {
    //$$         bubble(s.game, s.tip, s.boundingBoxHeight, cam.orientation, ps, (line, x, y, color, bg) ->
    //$$                 out.submitText(ps, x, y, line, false, Font.DisplayMode.NORMAL, 0xF000F0, color, bg, 0));
    //$$     }
    //$$ }
    //#elif MC >= 1.21.2
    //$$ @Override
    //$$ public void render(State s, PoseStack ps, MultiBufferSource buf, int light) {
    //$$     super.render(s, ps, buf, light);
    //$$     if (s.tip >= 0) {
    //$$         bubble(s.game, s.tip, s.boundingBoxHeight, this.entityRenderDispatcher.cameraOrientation(), ps, (line, x, y, color, bg) ->
    //$$                 getFont().drawInBatch(line, x, y, color, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, bg, 0xF000F0));
    //$$     }
    //$$ }
    //#else
    @Override
    public void render(DealerEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        super.render(e, yaw, partial, ps, buf, light);
        int tip = e.getTip();
        if (tip >= 0) {
            bubble(e.getGame(), tip, e.getBbHeight(), this.entityRenderDispatcher.cameraOrientation(), ps, (line, x, y, color, bg) ->
                    getFont().drawInBatch(line, x, y, color, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, bg, 0xF000F0));
        }
    }
    //#endif

    private interface Line {
        void draw(FormattedCharSequence line, float x, float y, int color, int bg);
    }

    /** Globo con un título dorado y, debajo, el consejo, todo con fondo claro y mirando a la cámara. */
    private static void bubble(int game, int tip, float height, Quaternionf camera, PoseStack ps, Line out) {
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(Component.translatable(tipKey(game, tip)), 130);
        ps.pushPose();
        ps.translate(0.0, height + 0.55, 0.0);
        ps.mulPose(camera);
        ps.scale(-0.02F, -0.02F, 0.02F);
        int n = lines.size();
        Component title = Component.translatable("casino.tip.title").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD);
        int bg = 0xF0FFF6DC;
        FormattedCharSequence t = title.getVisualOrderText();
        out.draw(t, -font.width(t) / 2.0F, -(n + 1) * 10.0F, 0xFF000000, bg);
        for (int i = 0; i < n; i++) {
            FormattedCharSequence l = lines.get(i);
            out.draw(l, -font.width(l) / 2.0F, -(n - i) * 10.0F, 0xFF202020, bg);
        }
        ps.popPose();
    }
}
