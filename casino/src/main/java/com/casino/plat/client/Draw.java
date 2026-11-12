package com.casino.plat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
//#if MC >= 26.1
//$$ import net.minecraft.client.renderer.SubmitNodeCollector;
//$$ import net.minecraft.client.renderer.item.ItemStackRenderState;
//$$ import net.minecraft.client.renderer.rendertype.RenderType;
//$$ import net.minecraft.client.renderer.rendertype.RenderTypes;
//$$ import net.minecraft.network.chat.Style;
//$$ import net.minecraft.util.FormattedCharSequence;
//$$ import java.util.ArrayList;
//$$ import java.util.List;
//#else
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
//#endif

/**
 * Dónde se dibuja una mesa o máquina del casino: pinceles con textura ({@link #cutout}, {@link #glow}), texto y objetos. Esconde que en
 * 1.20/1.21 se dibuja directo y en 26.x se "entrega" todo al final del cuadro.
 */
public final class Draw {
    //#if MC >= 26.1
    //$$ private final SubmitNodeCollector out;
    //$$ private final List<Object[]> batches = new ArrayList<>();
    //$$
    //$$ public Draw(SubmitNodeCollector out) {
    //$$     this.out = out;
    //$$ }
    //$$
    //$$ private VB rec(RenderType t) {
    //$$     VB vb = new VB(null);
    //$$     batches.add(new Object[] {t, vb});
    //$$     return vb;
    //$$ }
    //$$
    //$$ /** Manda todo lo grabado (lo llama la base del renderizador al terminar). */
    //$$ public void flush() {
    //$$     PoseStack id = new PoseStack();
    //$$     for (Object[] b : batches) {
    //$$         VB vb = (VB) b[1];
    //$$         if (vb.n > 0) {
    //$$             out.submitCustomGeometry(id, (RenderType) b[0], (pose, vc) -> vb.replay(vc));
    //$$         }
    //$$     }
    //$$     batches.clear();
    //$$ }
    //$$
    //$$ public VB cutout(ResourceLocation tex) {
    //$$     return rec(RenderTypes.entityCutout(tex));
    //$$ }
    //$$
    //$$ public VB glow(ResourceLocation tex) {
    //$$     return rec(RenderTypes.eyes(tex));
    //$$ }
    //$$
    //$$ public VB translucent(ResourceLocation tex) {
    //$$     return rec(RenderTypes.entityTranslucent(tex));
    //$$ }
    //$$
    //$$ public void text(PoseStack ps, net.minecraft.network.chat.Component c, float x, float y, int color, int bg, int light) {
    //$$     out.submitText(ps, x, y, c.getVisualOrderText(), false, Font.DisplayMode.NORMAL, light, argb(color), bg, 0);
    //$$ }
    //$$
    //$$ public void text(PoseStack ps, String s, float x, float y, int color, int bg, int light) {
    //$$     out.submitText(ps, x, y, FormattedCharSequence.forward(s, Style.EMPTY), false, Font.DisplayMode.NORMAL, light, argb(color), bg, 0);
    //$$ }
    //$$
    //$$ public void item(ItemStack st, ItemDisplayContext ctx, PoseStack ps, int light, Level level, int seed) {
    //$$     ItemStackRenderState state = new ItemStackRenderState();
    //$$     Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, st, ctx, level, null, seed);
    //$$     state.submit(ps, out, light, OverlayTexture.NO_OVERLAY, 0);
    //$$ }
    //#else
    private final MultiBufferSource buf;

    public Draw(MultiBufferSource buf) {
        this.buf = buf;
    }

    public void flush() {
    }

    /** OJO: pedir el pincel justo antes de usarlo (Minecraft termina el anterior al pedir otro). */
    public VB cutout(ResourceLocation tex) {
        return new VB(buf.getBuffer(RenderType.entityCutoutNoCull(tex)));
    }

    public VB glow(ResourceLocation tex) {
        return new VB(buf.getBuffer(RenderType.eyes(tex)));
    }

    public VB translucent(ResourceLocation tex) {
        return new VB(buf.getBuffer(RenderType.entityTranslucent(tex)));
    }

    public void text(PoseStack ps, net.minecraft.network.chat.Component c, float x, float y, int color, int bg, int light) {
        Minecraft.getInstance().font.drawInBatch(c, x, y, argb(color), false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, bg, light);
    }

    public void text(PoseStack ps, String s, float x, float y, int color, int bg, int light) {
        Minecraft.getInstance().font.drawInBatch(s, x, y, argb(color), false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, bg, light);
    }

    public void item(ItemStack st, ItemDisplayContext ctx, PoseStack ps, int light, Level level, int seed) {
        Minecraft.getInstance().getItemRenderer().renderStatic(st, ctx, light, OverlayTexture.NO_OVERLAY, ps, buf, level, seed);
    }
    //#endif

    /** Si el color no trae opacidad, se pone opaco (en 1.21+ un alfa de 0 sería invisible). */
    static int argb(int c) {
        return (c & 0xFC000000) == 0 ? c | 0xFF000000 : c;
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    /** Giro de la cámara (para cosas que siempre miran al jugador). */
    public static org.joml.Quaternionf camRot() {
        //#if MC >= 26.1
        //$$ return Minecraft.getInstance().gameRenderer.mainCamera().rotation();
        //#else
        return Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        //#endif
    }

    /** Posición de la cámara en el mundo. */
    public static net.minecraft.world.phys.Vec3 camPos() {
        //#if MC >= 26.1
        //$$ return Minecraft.getInstance().gameRenderer.mainCamera().position();
        //#else
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        //#endif
    }
}
