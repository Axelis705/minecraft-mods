package com.casino.plat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
//#if MC >= 26.1
//$$ import net.minecraft.client.renderer.SubmitNodeCollector;
//$$ import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
//$$ import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
//$$ import net.minecraft.client.renderer.state.level.CameraRenderState;
//$$ import net.minecraft.world.phys.Vec3;
//#else
import net.minecraft.client.renderer.MultiBufferSource;
//#endif

/**
 * Base de los renderizadores de mesas y máquinas: cada uno implementa {@link #draw} y aquí se conecta con la forma de dibujar de cada
 * versión (en 26.x Minecraft primero "extrae" el estado y después lo "entrega").
 */
//#if MC >= 26.1
//$$ public abstract class CasinoBER<T extends BlockEntity> implements BlockEntityRenderer<T, CasinoBER.State<T>> {
//$$     public static class State<T> extends BlockEntityRenderState {
//$$         T be;
//$$         float partial;
//$$     }
//$$
//$$     @Override
//$$     public State<T> createRenderState() {
//$$         return new State<>();
//$$     }
//$$
//$$     @Override
//$$     public void extractRenderState(T be, State<T> st, float partial, Vec3 cam, ModelFeatureRenderer.CrumblingOverlay crumbling) {
//$$         BlockEntityRenderer.super.extractRenderState(be, st, partial, cam, crumbling);
//$$         st.be = be;
//$$         st.partial = partial;
//$$     }
//$$
//$$     @Override
//$$     public void submit(State<T> st, PoseStack ps, SubmitNodeCollector out, CameraRenderState cam) {
//$$         if (st.be == null) {
//$$             return;
//$$         }
//$$         Draw d = new Draw(out);
//$$         draw(st.be, st.partial, ps, d, st.lightCoords, OverlayTexture.NO_OVERLAY);
//$$         d.flush();
//$$     }
//#else
public abstract class CasinoBER<T extends BlockEntity> implements BlockEntityRenderer<T> {
    @Override
    public void render(T be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        draw(be, partial, ps, new Draw(buf), light, overlay);
    }
//#endif

    protected abstract void draw(T be, float partial, PoseStack ps, Draw d, int light, int overlay);

    //#if MC >= 26.1
    //$$ @Override
    //$$ public boolean shouldRenderOffScreen() {
    //$$     return true;
    //$$ }
    //#else
    @Override
    public boolean shouldRenderOffScreen(T be) {
        return true;
    }
    //#endif
}
