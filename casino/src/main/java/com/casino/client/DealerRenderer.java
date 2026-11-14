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
//$$ import net.minecraft.client.model.HumanoidModel;
//$$ import net.minecraft.client.renderer.SubmitNodeCollector;
//$$ import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
//$$ import net.minecraft.client.renderer.state.level.CameraRenderState;
//#elif MC >= 1.21.2
//$$ import net.minecraft.client.model.PlayerModel;
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//$$ import net.minecraft.client.renderer.entity.state.PlayerRenderState;
//#else
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
//#endif

import java.util.ArrayList;
import java.util.List;

/**
 * El crupier se dibuja como una persona (modelo de jugador) con su propia piel. Cuando dice un consejo levanta la mano derecha y sobre su
 * cabeza aparece un globo de texto. Desde 1.21.2 Minecraft dibuja a partir de un "estado" copiado de la entidad; desde 26.x lo entrega.
 */
//#if MC >= 26.1
//$$ // En 26.x el estado NO puede ser AvatarRenderState: Minecraft manda esos estados al dibujante de jugadores (piel de Steve).
//$$ public class DealerRenderer extends MobRenderer<DealerEntity, DealerRenderer.State, DealerRenderer.DealerModel> {
//$$     public static class State extends HumanoidRenderState {
//$$         int tip = -1;
//$$         int game;
//$$         float age;
//$$     }
//#elif MC >= 1.21.2
//$$ public class DealerRenderer extends MobRenderer<DealerEntity, DealerRenderer.State, DealerRenderer.DealerModel> {
//$$     public static class State extends PlayerRenderState {
//$$         int tip = -1;
//$$         int game;
//$$         float age;
//$$     }
//#else
public class DealerRenderer extends MobRenderer<DealerEntity, PlayerModel<DealerEntity>> {
//#endif
    private static final ResourceLocation TEXTURE = Ids.rl(Casino.MODID, "textures/entity/dealer.png");
    private static final ResourceLocation TEXTURE_ROULETTE = Ids.rl(Casino.MODID, "textures/entity/dealer_roulette.png");
    private static final ResourceLocation TEXTURE_POKER = Ids.rl(Casino.MODID, "textures/entity/dealer_poker.png");
    private static final ResourceLocation TEXTURE_GUARD = Ids.rl(Casino.MODID, "textures/entity/dealer_guard.png");
    private static final ResourceLocation TEXTURE_BARTENDER = Ids.rl(Casino.MODID, "textures/entity/dealer_bartender.png");
    /** Textura blanca lisa: el globo se arma con rectángulos de colores encima. */
    private static final ResourceLocation WHITE = Ids.rl(Casino.MODID, "textures/entity/bubble.png");

    private static ResourceLocation texture(int game) {
        return switch (game) {
            case DealerEntity.GAME_ROULETTE -> TEXTURE_ROULETTE;
            case DealerEntity.GAME_POKER -> TEXTURE_POKER;
            case DealerEntity.GAME_GUARD -> TEXTURE_GUARD;
            case DealerEntity.GAME_BARTENDER -> TEXTURE_BARTENDER;
            default -> TEXTURE;
        };
    }

    private static String role(int game) {
        return switch (game) {
            case DealerEntity.GAME_ROULETTE -> "roulette";
            case DealerEntity.GAME_POKER -> "poker";
            case DealerEntity.GAME_GUARD -> "guard";
            case DealerEntity.GAME_BARTENDER -> "bartender";
            default -> "blackjack";
        };
    }

    private static String tipKey(int game, int tip) {
        return "casino.tip." + role(game) + "." + (tip >= DealerEntity.SAY ? "say_" + (tip - DealerEntity.SAY) : String.valueOf(tip));
    }

    /** Color de la franja de arriba del globo, según quién habla. */
    private static int headerColor(int game) {
        return switch (game) {
            case DealerEntity.GAME_ROULETTE -> 0x8E1B22;
            case DealerEntity.GAME_POKER -> 0x223C8A;
            case DealerEntity.GAME_GUARD -> 0x1C1C26;
            case DealerEntity.GAME_BARTENDER -> 0x5A2A86;
            default -> 0x1E6B3A;
        };
    }

    /** Símbolo junto al nombre: un palo de la baraja (o una estrella el guardia). */
    private static String icon(int game) {
        return switch (game) {
            case DealerEntity.GAME_ROULETTE -> "\u2666";
            case DealerEntity.GAME_POKER -> "\u2663";
            case DealerEntity.GAME_GUARD -> "\u2605";
            case DealerEntity.GAME_BARTENDER -> "\u2665";
            default -> "\u2660";
        };
    }

    //#if MC >= 26.1
    //$$ /** Modelo humano con la malla del jugador (mangas, chaqueta y pantalón son hijos de cada extremidad); mano en alto con consejo. */
    //$$ public static class DealerModel extends HumanoidModel<State> {
    //$$     DealerModel(ModelPart part) {
    //$$         super(part);
    //$$     }
    //$$
    //$$     @Override
    //$$     public void setupAnim(State s) {
    //$$         super.setupAnim(s);
    //$$         if (s.tip >= 0) {
    //$$             this.rightArm.xRot = -2.85F + Mth.sin(s.ageInTicks * 0.25F) * 0.1F;
    //$$             this.rightArm.zRot = 0.15F + Mth.sin(s.ageInTicks * 0.5F) * 0.08F;
    //$$         }
    //$$     }
    //$$ }
    //$$
    //#elif MC >= 1.21.2
    //$$ /** El modelo de jugador, pero con la mano derecha en alto mientras hay un consejo en el aire. */
    //$$ public static class DealerModel extends PlayerModel {
    //$$     DealerModel(ModelPart part) {
    //$$         super(part, false);
    //$$     }
    //$$
    //$$     @Override
    //$$     public void setupAnim(PlayerRenderState s) {
    //$$         super.setupAnim(s);
    //$$         if (s instanceof State d && d.tip >= 0) {
    //$$             this.rightArm.xRot = -2.85F + Mth.sin(s.ageInTicks * 0.25F) * 0.1F;
    //$$             this.rightArm.zRot = 0.15F + Mth.sin(s.ageInTicks * 0.5F) * 0.08F;
    //$$         }
    //$$     }
    //$$ }
    //$$
    //#endif
    //#if MC >= 1.21.2
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
    //$$     s.age = e.tipAge + partial;
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
    //$$         com.casino.plat.client.Draw d = new com.casino.plat.client.Draw(out);
    //$$         bubble(d, s.game, s.tip, s.age, s.boundingBoxHeight, cam.orientation, ps);
    //$$         d.flush();
    //$$     }
    //$$ }
    //#elif MC >= 1.21.2
    //$$ @Override
    //$$ public void render(State s, PoseStack ps, MultiBufferSource buf, int light) {
    //$$     super.render(s, ps, buf, light);
    //$$     if (s.tip >= 0) {
    //$$         bubble(new com.casino.plat.client.Draw(buf), s.game, s.tip, s.age, s.boundingBoxHeight, this.entityRenderDispatcher.cameraOrientation(), ps);
    //$$     }
    //$$ }
    //#else
    @Override
    public void render(DealerEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        super.render(e, yaw, partial, ps, buf, light);
        int tip = e.getTip();
        if (tip >= 0) {
            bubble(new com.casino.plat.client.Draw(buf), e.getGame(), tip, e.tipAge + partial, e.getBbHeight(), this.entityRenderDispatcher.cameraOrientation(), ps);
        }
    }
    //#endif

    /** Ancho máximo del texto del globo (en píxeles de letra). */
    private static final int WRAP = 150;

    /** Corta el texto en renglones que quepan en el ancho (por palabras). */
    private static List<String> wrap(Font font, String text, int max) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String w : text.split(" ")) {
            String probe = line.length() == 0 ? w : line + " " + w;
            if (line.length() > 0 && font.width(probe) > max) {
                out.add(line.toString());
                line = new StringBuilder(w);
            } else {
                line = new StringBuilder(probe);
            }
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
        return out;
    }

    /**
     * Globo de diálogo: tarjeta con esquinas redondeadas, borde oscuro con filo dorado, sombra, franja de color con el símbolo y el
     * nombre de quien habla (y "Consejo" a la derecha si es un consejo), el texto que se va escribiendo letra por letra y una colita que
     * apunta a la cabeza. Aparece con un pequeño salto, flota suave y se desvanece al final. Siempre mira a la cámara.
     */
    private static void bubble(com.casino.plat.client.Draw d, int game, int tip, float age, float height, Quaternionf camera, PoseStack ps) {
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        String text = Component.translatable(tipKey(game, tip)).getString();
        List<String> lines = wrap(font, text, WRAP);
        String name = icon(game) + " " + Component.translatable("casino.npc." + role(game)).getString();
        boolean isTip = tip < DealerEntity.SAY;
        String tag = isTip ? Component.translatable("casino.tip.title").getString() : "";
        int textW = 0;
        for (String l : lines) {
            textW = Math.max(textW, font.width(l));
        }
        int headW = font.width(name) + (isTip ? (int) (font.width(tag) * 0.75F) + 10 : 0);
        int w = Math.max(Math.max(textW, headW) + 14, 70);
        int n = lines.size();
        int head = 13;
        int h = head + 5 + n * 10 + 3;
        // animación: salto al aparecer, flotar y desvanecer al final
        float pop = Math.min(1F, age / 4F);
        float scale = pop < 1F ? 0.6F + 0.4F * pop + 0.08F * Mth.sin(pop * (float) Math.PI) : 1F;
        float fade = Mth.clamp((DealerEntity.TIP_TICKS - age) / 12F, 0F, 1F) * Mth.clamp(age / 3F, 0F, 1F);
        if (fade <= 0.02F) {
            return;
        }
        float bob = Mth.sin(age * 0.12F) * 0.025F;

        ps.pushPose();
        ps.translate(0.0, height + 0.42 + bob, 0.0);
        ps.mulPose(camera);
        ps.scale(scale, scale, scale);
        // el globo: rectángulos sin escalar (en bloques) para que la luz sea pareja; se dibujan un poco detrás del texto
        float sx = 0.02F * com.casino.plat.client.Draw.camFlipX();
        float sy = -0.02F;
        org.joml.Vector3f up = new org.joml.Quaternionf(camera).conjugate().transform(new org.joml.Vector3f(0, 1, 0));
        Box b = new Box(d.translucent(WHITE), ps.last(), sx, sy, up, fade);
        int x0 = -w / 2, x1 = w - w / 2, y0 = -h, y1 = 0;
        int ink = 0x2A1C0E, gold = 0xD9A93A, paper = 0xFFF7E4, band = headerColor(game);
        // sombra
        b.round(x0 + 2, y0 + 2, x1 + 2, y1 + 2, 0x000000, 0.35F, 1.3F);
        // borde oscuro, filo dorado y papel
        b.round(x0, y0, x1, y1, ink, 1F, 1.2F);
        b.round(x0 + 1, y0 + 1, x1 - 1, y1 - 1, gold, 1F, 1.1F);
        b.round(x0 + 2, y0 + 2, x1 - 2, y1 - 2, paper, 1F, 1.0F);
        // franja con el nombre (y su línea dorada abajo)
        b.rect(x0 + 2, y0 + 3, x1 - 2, y0 + head, band, 1F, 0.9F);
        b.rect(x0 + 3, y0 + 2, x1 - 3, y0 + 3, band, 1F, 0.9F);
        b.rect(x0 + 2, y0 + head, x1 - 2, y0 + head + 1, gold, 1F, 0.9F);
        // colita hacia la cabeza
        b.rect(-5, y1 - 2, 5, y1 - 1, paper, 1F, 0.95F);
        for (int i = 0; i < 6; i++) {
            b.rect(-6 + i, y1 - 1 + i, 6 - i, y1 + i, ink, 1F, 1.2F);
            if (i < 5) {
                b.rect(-5 + i, y1 - 1 + i, 5 - i, y1 + i, paper, 1F, 0.95F);
            }
        }

        // textos (escala de letra)
        ps.scale(sx, sy, 0.02F);
        int a = Math.max(0x10, (int) (fade * 255)) << 24;
        d.text(ps, name, x0 + 6, y0 + 3, a | 0xFFFFFF, 0, 0xF000F0);
        if (isTip) {
            ps.pushPose();
            ps.translate(x1 - 5, y0 + 4.5F, 0);
            ps.scale(0.75F, 0.75F, 1F);
            d.text(ps, tag, -font.width(tag), 0, a | 0xF2CF6B, 0, 0xF000F0);
            ps.popPose();
        }
        // texto letra por letra (unas 30 por segundo); el globo ya tiene su tamaño final desde el principio
        int shown = (int) (age * 1.6F);
        int lx = -textW / 2;
        for (int i = 0; i < n; i++) {
            String l = lines.get(i);
            int ly = y0 + head + 5 + i * 10;
            if (shown < l.length()) {
                String part = l.substring(0, Math.max(0, shown));
                if (!part.isEmpty()) {
                    d.text(ps, part, lx, ly, a | 0x2B2118, 0, 0xF000F0);
                }
                // cursor parpadeando mientras escribe
                if (((int) (age / 4)) % 2 == 0) {
                    d.text(ps, "_", lx + font.width(part), ly, a | 0x9A8466, 0, 0xF000F0);
                }
                break;
            }
            d.text(ps, l, lx, ly, a | 0x2B2118, 0, 0xF000F0);
            shown -= l.length() + 1;
        }
        ps.popPose();
    }

    /** Pinta rectángulos planos del globo, de cara a la cámara, en coordenadas de "píxel de letra". */
    private static final class Box {
        private final com.casino.plat.client.VB vb;
        private final PoseStack.Pose pose;
        private final float sx, sy, fade;
        private final org.joml.Vector3f n;

        Box(com.casino.plat.client.VB vb, PoseStack.Pose pose, float sx, float sy, org.joml.Vector3f up, float fade) {
            this.vb = vb;
            this.pose = pose;
            this.sx = sx;
            this.sy = sy;
            this.n = up;
            this.fade = fade;
        }

        /** z: qué tan atrás del texto (más grande = más atrás). */
        void rect(float x0, float y0, float x1, float y1, int rgb, float alpha, float z) {
            int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255, a = (int) (255 * alpha * fade);
            float zz = z * 0.02F;
            int ov = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
            int light = 0xF000F0;
            vb.v(pose, x0 * sx, y0 * sy, zz, r, g, b, a, 0.25F, 0.25F, ov, light, n.x, n.y, n.z);
            vb.v(pose, x0 * sx, y1 * sy, zz, r, g, b, a, 0.25F, 0.75F, ov, light, n.x, n.y, n.z);
            vb.v(pose, x1 * sx, y1 * sy, zz, r, g, b, a, 0.75F, 0.75F, ov, light, n.x, n.y, n.z);
            vb.v(pose, x1 * sx, y0 * sy, zz, r, g, b, a, 0.75F, 0.25F, ov, light, n.x, n.y, n.z);
        }

        /** Rectángulo con las esquinas recortadas un píxel (se ve redondeado). */
        void round(float x0, float y0, float x1, float y1, int rgb, float alpha, float z) {
            rect(x0 + 1, y0, x1 - 1, y1, rgb, alpha, z);
            rect(x0, y0 + 1, x0 + 1, y1 - 1, rgb, alpha, z);
            rect(x1 - 1, y0 + 1, x1, y1 - 1, rgb, alpha, z);
        }
    }
}
