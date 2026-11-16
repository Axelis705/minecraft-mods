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
//$$         int anim;
//$$         float animT;
//$$         float talk = -1;
//$$         DealerMotion motion;
//$$         int variant;
//$$         int name;
//$$     }
//#elif MC >= 1.21.2
//$$ public class DealerRenderer extends MobRenderer<DealerEntity, DealerRenderer.State, DealerRenderer.DealerModel> {
//$$     public static class State extends PlayerRenderState {
//$$         int tip = -1;
//$$         int game;
//$$         float age;
//$$         int anim;
//$$         float animT;
//$$         float talk = -1;
//$$         DealerMotion motion;
//$$         int variant;
//$$         int name;
//$$     }
//#else
public class DealerRenderer extends MobRenderer<DealerEntity, PlayerModel<DealerEntity>> {
//#endif
    private static final ResourceLocation TEXTURE = Ids.rl(Casino.MODID, "textures/entity/dealer.png");
    private static final ResourceLocation TEXTURE_ROULETTE = Ids.rl(Casino.MODID, "textures/entity/dealer_roulette.png");
    private static final ResourceLocation TEXTURE_POKER = Ids.rl(Casino.MODID, "textures/entity/dealer_poker.png");
    private static final ResourceLocation TEXTURE_GUARD = Ids.rl(Casino.MODID, "textures/entity/dealer_guard.png");
    private static final ResourceLocation TEXTURE_BARTENDER = Ids.rl(Casino.MODID, "textures/entity/dealer_bartender.png");
    private static final ResourceLocation TEXTURE_WHEEL = Ids.rl(Casino.MODID, "textures/entity/dealer_wheel.png");
    private static final ResourceLocation TEXTURE_SWAT = Ids.rl(Casino.MODID, "textures/entity/dealer_swat.png");
    private static final ResourceLocation TEXTURE_FIXER = Ids.rl(Casino.MODID, "textures/entity/dealer_fixer.png");
    /** Textura blanca lisa: el globo se arma con rectángulos de colores encima. */
    private static final ResourceLocation WHITE = Ids.rl(Casino.MODID, "textures/entity/bubble.png");

    private static final ResourceLocation[] TEXTURE_PATRON = new ResourceLocation[DealerEntity.PATRON_SKINS];
    private static final ResourceLocation TEXTURE_SHERIFF = Ids.rl(Casino.MODID, "textures/entity/dealer_west_sheriff.png");
    private static final ResourceLocation TEXTURE_WEST_BARTENDER = Ids.rl(Casino.MODID, "textures/entity/dealer_west_bartender.png");
    private static final ResourceLocation TEXTURE_WEST_DEALER = Ids.rl(Casino.MODID, "textures/entity/dealer_west_dealer.png");
    private static final ResourceLocation TEXTURE_WEST_HOST = Ids.rl(Casino.MODID, "textures/entity/dealer_west_host.png");
    private static final ResourceLocation[] TEXTURE_WEST_PATRON = new ResourceLocation[DealerEntity.WEST_PATRON_SKINS];

    static {
        for (int i = 0; i < TEXTURE_PATRON.length; i++) {
            TEXTURE_PATRON[i] = Ids.rl(Casino.MODID, "textures/entity/dealer_patron_" + i + ".png");
        }
    }

    private static ResourceLocation texture(int game, int variant) {
        if (variant >= DealerEntity.WEST) {
            return switch (game) {
                case DealerEntity.GAME_GUARD -> TEXTURE_SHERIFF;
                case DealerEntity.GAME_BARTENDER -> TEXTURE_WEST_BARTENDER;
                case DealerEntity.GAME_WHEEL -> TEXTURE_WEST_HOST;
                case DealerEntity.GAME_PATRON, DealerEntity.GAME_DUELIST -> {
                    int i = Math.floorMod(variant - DealerEntity.WEST, TEXTURE_WEST_PATRON.length);
                    if (TEXTURE_WEST_PATRON[i] == null) {
                        TEXTURE_WEST_PATRON[i] = Ids.rl(Casino.MODID, "textures/entity/dealer_west_patron_" + i + ".png");
                    }
                    yield TEXTURE_WEST_PATRON[i];
                }
                default -> TEXTURE_WEST_DEALER;
            };
        }
        if (game == DealerEntity.GAME_FIXER) {
            return TEXTURE_FIXER;
        }
        if (game == DealerEntity.GAME_PATRON) {
            return TEXTURE_PATRON[Math.floorMod(variant, TEXTURE_PATRON.length)];
        }
        if (game == DealerEntity.GAME_SECURITY) {
            return variant == 1 ? TEXTURE_SWAT : TEXTURE_GUARD;
        }
        return switch (game) {
            case DealerEntity.GAME_ROULETTE -> TEXTURE_ROULETTE;
            case DealerEntity.GAME_POKER -> TEXTURE_POKER;
            case DealerEntity.GAME_GUARD -> TEXTURE_GUARD;
            case DealerEntity.GAME_BARTENDER -> TEXTURE_BARTENDER;
            case DealerEntity.GAME_WHEEL -> TEXTURE_WHEEL;
            default -> TEXTURE;
        };
    }

    private static String role(int game) {
        return switch (game) {
            case DealerEntity.GAME_ROULETTE -> "roulette";
            case DealerEntity.GAME_POKER -> "poker";
            case DealerEntity.GAME_GUARD -> "guard";
            case DealerEntity.GAME_BARTENDER -> "bartender";
            case DealerEntity.GAME_WHEEL -> "wheel";
            case DealerEntity.GAME_PATRON -> "patron";
            case DealerEntity.GAME_SECURITY -> "security";
            case DealerEntity.GAME_FIXER -> "fixer";
            case DealerEntity.GAME_DUELIST -> "duelist";
            default -> "blackjack";
        };
    }

    private static String tipKey(int game, int tip) {
        return "casino.tip." + role(game) + "." + (tip >= DealerEntity.SAY ? "say_" + (tip - DealerEntity.SAY) : String.valueOf(tip));
    }

    /** En el saloon los personajes tienen sus propias frases ("casino.tip.guard_west.3"...); si falta alguna, usan la normal. */
    private static String tipKey(int game, int variant, int tip) {
        if (variant >= DealerEntity.WEST) {
            String k = "casino.tip." + role(game) + "_west." + (tip >= DealerEntity.SAY ? "say_" + (tip - DealerEntity.SAY) : String.valueOf(tip));
            if (net.minecraft.locale.Language.getInstance().has(k)) {
                return k;
            }
        }
        return tipKey(game, tip);
    }

    private static String nameKey(int game, int variant, int nameIdx) {
        if (game == DealerEntity.GAME_DUELIST) {
            return "casino.npc.duelist_" + variant;
        }
        if (game == DealerEntity.GAME_PATRON) {
            // los del Oeste tienen nombre fijo por piel; los del Diamond, uno de la lista de su piel (señor, señora, don, doña)
            return variant >= DealerEntity.WEST ? "casino.npc.patron_" + variant : DealerEntity.patronNameKey(variant, nameIdx);
        }
        String k = "casino.npc." + role(game) + (variant >= DealerEntity.WEST ? "_west" : "");
        return net.minecraft.locale.Language.getInstance().has(k) ? k : "casino.npc." + role(game);
    }

    /** Color de la franja de arriba del globo, según quién habla. */
    private static int headerColor(int game) {
        return switch (game) {
            case DealerEntity.GAME_ROULETTE -> 0x8E1B22;
            case DealerEntity.GAME_POKER -> 0x223C8A;
            case DealerEntity.GAME_GUARD -> 0x1C1C26;
            case DealerEntity.GAME_BARTENDER -> 0x5A2A86;
            case DealerEntity.GAME_WHEEL -> 0xA8326B;
            case DealerEntity.GAME_PATRON -> 0x23707E;
            case DealerEntity.GAME_SECURITY -> 0x2A2A30;
            case DealerEntity.GAME_FIXER -> 0x4A3A22;
            case DealerEntity.GAME_DUELIST -> 0x7A3A14;
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
            case DealerEntity.GAME_WHEEL -> "\u2726";
            case DealerEntity.GAME_PATRON -> "\u266A";
            case DealerEntity.GAME_SECURITY -> "\u2605";
            case DealerEntity.GAME_FIXER -> "\u2620";
            case DealerEntity.GAME_DUELIST -> "\u2605";
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
    //$$         DealerAnimator.apply(s.game, s.anim, s.animT, s.ageInTicks, s.talk, this.head, this.body, this.rightArm, this.leftArm, s.motion);
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
    //$$         if (s instanceof State d) {
    //$$             DealerAnimator.apply(d.game, d.anim, d.animT, s.ageInTicks, d.talk, this.head, this.body, this.rightArm, this.leftArm, d.motion);
    //$$         }
    //$$     }
    //$$ }
    //$$
    //#endif
    //#if MC >= 1.21.2
    //$$ public DealerRenderer(EntityRendererProvider.Context ctx) {
    //$$     super(ctx, new DealerModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.45F);
    //$$     addLayer(new PropLayer(this));
    //$$ }
    //$$
    //$$ /** Lo que tiene en las manos (carta, bola, botella, vaso...), pegado a la palma. */
    //$$ private static final class PropLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<State, DealerModel> {
    //$$     PropLayer(net.minecraft.client.renderer.entity.RenderLayerParent<State, DealerModel> p) {
    //$$         super(p);
    //$$     }
    //$$
    //#if MC >= 26.1
    //$$     @Override
    //$$     public void submit(PoseStack ps, SubmitNodeCollector out, int light, State s, float yRot, float xRot) {
    //$$         int[] hat = DealerProps.hat(s.game, s.variant);
    //$$         if (hat[0] == 0 && (s.motion == null || s.motion.propR == 0 && s.motion.propL == 0)) {
    //$$             return;
    //$$         }
    //$$         com.casino.plat.client.Draw d = new com.casino.plat.client.Draw(out);
    //$$         if (s.motion != null) {
    //$$             DealerProps.draw(d, ps, getParentModel().rightArm, true, s.motion.propR, s.motion.tiltR, light);
    //$$             DealerProps.draw(d, ps, getParentModel().leftArm, false, s.motion.propL, 0, light);
    //$$         }
    //$$         DealerProps.drawHat(d, ps, getParentModel().head, hat, light);
    //$$         d.flush();
    //$$     }
    //#else
    //$$     @Override
    //$$     public void render(PoseStack ps, MultiBufferSource buf, int light, State s, float yRot, float xRot) {
    //$$         int[] hat = DealerProps.hat(s.game, s.variant);
    //$$         if (hat[0] == 0 && (s.motion == null || s.motion.propR == 0 && s.motion.propL == 0)) {
    //$$             return;
    //$$         }
    //$$         com.casino.plat.client.Draw d = new com.casino.plat.client.Draw(buf);
    //$$         if (s.motion != null) {
    //$$             DealerProps.draw(d, ps, getParentModel().rightArm, true, s.motion.propR, s.motion.tiltR, light);
    //$$             DealerProps.draw(d, ps, getParentModel().leftArm, false, s.motion.propL, 0, light);
    //$$         }
    //$$         DealerProps.drawHat(d, ps, getParentModel().head, hat, light);
    //$$     }
    //#endif
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
    //$$     s.anim = e.animId;
    //$$     s.animT = e.tickCount - e.animStart + partial;
    //$$     s.talk = e.getTip() >= 0 ? e.tipAge + partial : -1F;
    //$$     s.motion = DealerMotion.compute(e, partial);
    //$$     s.variant = e.getVariant();
    //$$     s.name = e.getNameIndex();
    //$$     // sentado de verdad (piernas al frente): MobRenderer no llena esto como el dibujante de humanos
    //$$     s.isPassenger = e.isPassenger();
    //$$ }
    //$$
    //$$ @Override
    //$$ public ResourceLocation getTextureLocation(State s) {
    //$$     return texture(s.game, s.variant);
    //$$ }
    //#else
    /** El modelo de jugador, pero con la mano derecha en alto mientras hay un consejo en el aire. */
    private static class DealerModel extends PlayerModel<DealerEntity> {
        DealerModel(ModelPart part) {
            super(part, false);
        }

        @Override
        public void setupAnim(DealerEntity e, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
            // en 1.20 Minecraft no regresa el giro de lado de la cabeza (zRot) a 0 en cada cuadro: sin esto se iba acumulando
            this.head.zRot = 0F;
            this.body.xRot = 0F;
            super.setupAnim(e, limbSwing, limbSwingAmount, age, yaw, pitch);
            float frac = age - e.tickCount;
            DealerMotion m = DealerMotion.compute(e, frac);
            e.clientMotion = m;
            DealerAnimator.apply(e.getGame(), e.animId, age - e.animStart, age, e.getTip() >= 0 ? e.tipAge + frac : -1F, this.head, this.body,
                    this.rightArm, this.leftArm, m);
            this.rightSleeve.copyFrom(this.rightArm);
            this.leftSleeve.copyFrom(this.leftArm);
            this.hat.copyFrom(this.head);
            this.jacket.copyFrom(this.body);
        }
    }

    public DealerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DealerModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.45F);
        addLayer(new PropLayer(this));
    }

    /** Lo que tiene en las manos (carta, bola, botella, vaso...), pegado a la palma. */
    private static final class PropLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<DealerEntity, PlayerModel<DealerEntity>> {
        PropLayer(net.minecraft.client.renderer.entity.RenderLayerParent<DealerEntity, PlayerModel<DealerEntity>> p) {
            super(p);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, DealerEntity e, float limb, float limbAmount, float partial, float age,
                float yaw, float pitch) {
            int[] hat = DealerProps.hat(e.getGame(), e.getVariant());
            DealerMotion m = e.clientMotion instanceof DealerMotion mm ? mm : null;
            if (hat[0] == 0 && (m == null || m.propR == 0 && m.propL == 0)) {
                return;
            }
            com.casino.plat.client.Draw d = new com.casino.plat.client.Draw(buf);
            if (m != null) {
                DealerProps.draw(d, ps, getParentModel().rightArm, true, m.propR, m.tiltR, light);
                DealerProps.draw(d, ps, getParentModel().leftArm, false, m.propL, 0, light);
            }
            DealerProps.drawHat(d, ps, getParentModel().head, hat, light);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(DealerEntity entity) {
        return texture(entity.getGame(), entity.getVariant());
    }
    //#endif

    //#if MC >= 26.1
    //$$ @Override
    //$$ public void submit(State s, PoseStack ps, SubmitNodeCollector out, CameraRenderState cam) {
    //$$     super.submit(s, ps, out, cam);
    //$$     if (s.tip >= 0) {
    //$$         com.casino.plat.client.Draw d = new com.casino.plat.client.Draw(out);
    //$$         bubble(d, s.game, s.variant, s.name, s.tip, s.age, s.boundingBoxHeight, cam.orientation, ps);
    //$$         d.flush();
    //$$     }
    //$$ }
    //#elif MC >= 1.21.2
    //$$ @Override
    //$$ public void render(State s, PoseStack ps, MultiBufferSource buf, int light) {
    //$$     super.render(s, ps, buf, light);
    //$$     if (s.tip >= 0) {
    //$$         bubble(new com.casino.plat.client.Draw(buf), s.game, s.variant, s.name, s.tip, s.age, s.boundingBoxHeight, this.entityRenderDispatcher.cameraOrientation(), ps);
    //$$     }
    //$$ }
    //#else
    @Override
    public void render(DealerEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        super.render(e, yaw, partial, ps, buf, light);
        int tip = e.getTip();
        if (tip >= 0) {
            bubble(new com.casino.plat.client.Draw(buf), e.getGame(), e.getVariant(), e.getNameIndex(), tip, e.tipAge + partial, e.getBbHeight(), this.entityRenderDispatcher.cameraOrientation(), ps);
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
    private static void bubble(com.casino.plat.client.Draw d, int game, int variant, int nameIdx, int tip, float age, float height, Quaternionf camera, PoseStack ps) {
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        String text = Component.translatable(tipKey(game, variant, tip)).getString();
        List<String> lines = wrap(font, text, WRAP);
        String name = icon(game) + " " + Component.translatable(nameKey(game, variant, nameIdx)).getString();
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
        // el globo se acerca a la cámara (hasta 1.4 bloques) para quedar delante de lo que tiene alrededor (la rueda, repisas,
        // columnas); se agranda en la misma proporción, así en pantalla se ve igual pero ya no lo tapan
        org.joml.Matrix4f pm = ps.last().pose();
        org.joml.Vector3f at = pm.transformPosition(0, 0, 0, new org.joml.Vector3f());
        float dist = at.length();
        float pull = Math.min(1.4F, Math.max(0F, (dist - 1.2F) * 0.5F));
        if (pull > 0.01F) {
            org.joml.Vector3f toCam = new org.joml.Matrix4f(pm).invert().transformDirection(new org.joml.Vector3f(at).negate().normalize());
            ps.translate(toCam.x * pull, toCam.y * pull, toCam.z * pull);
            scale *= (dist - pull) / dist;
        }
        ps.mulPose(camera);
        ps.scale(scale, scale, scale);
        // el globo: rectángulos sin escalar (en bloques) para que la luz sea pareja; se dibujan un poco detrás del texto
        float sx = 0.02F * com.casino.plat.client.Draw.camFlipX();
        float sy = -0.02F;
        org.joml.Vector3f up = new org.joml.Quaternionf(camera).conjugate().transform(new org.joml.Vector3f(0, 1, 0));
        // ¿hacia dónde queda "atrás" (lejos de la cámara)? Se mide en vez de suponerlo: la cámara está en el origen de esta pose
        org.joml.Matrix4f m = ps.last().pose();
        float near0 = m.transformPosition(0, 0, 0, new org.joml.Vector3f()).length();
        float near1 = m.transformPosition(0, 0, 1, new org.joml.Vector3f()).length();
        float back = near1 < near0 ? -0.02F : 0.02F;
        Box b = new Box(d.translucent(WHITE), ps.last(), sx, sy, up, fade, back);
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
        private final float sx, sy, fade, back;
        private final org.joml.Vector3f n;

        Box(com.casino.plat.client.VB vb, PoseStack.Pose pose, float sx, float sy, org.joml.Vector3f up, float fade, float back) {
            this.back = back;
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
            float zz = z * back;
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
