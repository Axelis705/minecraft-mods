package com.axeliscore.plat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
//#if MC >= 26.1
//$$ import net.minecraft.client.renderer.SubmitNodeCollector;
//$$ import net.minecraft.client.renderer.entity.state.EntityRenderState;
//$$ import net.minecraft.client.renderer.state.level.CameraRenderState;
//#else
import net.minecraft.client.renderer.MultiBufferSource;
//#endif

/**
 * Renderizador sencillo para entidades propias que no son un modelo animado (avisos en el suelo, proyectiles, efectos), igual en todas
 * las versiones: {@link #snap} copia lo necesario de la entidad (en 26.x se dibuja después, sin la entidad) y {@link #draw} dibuja con
 * {@link Draw}. Siempre se dibuja aunque su centro quede fuera de la vista (los avisos grandes no desaparecen al mirar a un lado).
 */
//#if MC >= 26.1
//$$ public abstract class CoreEntityRenderer<T extends Entity, S> extends EntityRenderer<T, CoreEntityRenderer.State<S>> {
//$$     public static class State<S> extends EntityRenderState {
//$$         public S snap;
//$$     }
//#else
public abstract class CoreEntityRenderer<T extends Entity, S> extends EntityRenderer<T> {
//#endif

    protected CoreEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0F;
    }

    /** Lo que hace falta para dibujarla en este cuadro (no guardar la entidad). */
    protected abstract S snap(T e, float partial);

    /** Dibuja (la pila ya está en la posición de la entidad). */
    protected abstract void draw(S s, PoseStack ps, Draw d, int light);

    //#if MC >= 26.1
    //$$ @Override
    //$$ protected boolean affectedByCulling(T e) {
    //$$     return false;
    //$$ }
    //#else
    @Override
    public boolean shouldRender(T e, Frustum f, double x, double y, double z) {
        return true;
    }
    //#endif

    //#if MC >= 26.1
    //$$ @Override
    //$$ public State<S> createRenderState() {
    //$$     return new State<>();
    //$$ }
    //$$
    //$$ @Override
    //$$ public void extractRenderState(T e, State<S> s, float pt) {
    //$$     super.extractRenderState(e, s, pt);
    //$$     s.snap = snap(e, pt);
    //$$ }
    //$$
    //$$ @Override
    //$$ public void submit(State<S> s, PoseStack ps, SubmitNodeCollector out, CameraRenderState cam) {
    //$$     if (s.snap != null) {
    //$$         Draw d = new Draw(out);
    //$$         draw(s.snap, ps, d, s.lightCoords);
    //$$         d.flush();
    //$$     }
    //$$     super.submit(s, ps, out, cam);
    //$$ }
    //#else
    @Override
    public void render(T e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        S s = snap(e, pt);
        if (s != null) {
            Draw d = new Draw(buf);
            draw(s, ps, d, light);
            d.flush();
        }
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T e) {
        return Draw.BLANCO;
    }
    //#endif
}
