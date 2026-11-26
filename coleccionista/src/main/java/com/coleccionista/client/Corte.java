package com.coleccionista.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Quaternionf;

/**
 * (Cliente) El brazo cortado de la final (veintiuno), igual para el finalista sentado, para su cuerpo y para la máquina:
 * <ul>
 * <li>la pose del brazo izquierdo estirado sobre la máquina ({@link #INCLINA}, {@link #AX}...);</li>
 * <li>{@link #munon}: lo que le queda (la mitad de arriba del brazo, del hombro a la mitad) con el corte rojo en la punta;</li>
 * <li>{@link #antebrazo}: la otra mitad (de la mitad del brazo a la mano), que se queda en la máquina con su corte rojo;</li>
 * <li>{@link #MUERTO_INCLINA} y compañía: cómo se queda al morir (echado hacia atrás en la silla, la cabeza colgando para atrás, los brazos
 * colgando).</li>
 * </ul>
 * El brazo de Minecraft mide 12 px (de y -2 a 10 desde el hombro): se corta en y 4.
 */
public final class Corte {
    /** Cuánto se inclina el cuerpo hacia la mesa (grados; negativo: hacia adelante) y el giro del brazo izquierdo sobre la máquina. */
    public static final float INCLINA = -9F, AX = -1.72F, AY = 0.02F, AZ = -0.02F;
    /** Muerto: echado hacia atrás (grados), la cabeza y los brazos. */
    public static final float MUERTO_INCLINA = 22F, MUERTO_CABEZA = -0.6F, MUERTO_BRAZO = -0.35F;
    private static final float CORTE = 4F;

    private Corte() {
    }

    /** La cabeza del muerto, un poco distinta para cada quien ({@code v} 0..2). */
    public static float[] cabezaMuerto(int v) {
        float lado = v % 2 == 0 ? 1F : -1F;
        return new float[] {MUERTO_CABEZA - 0.05F * v, 0.25F * lado, 0.12F * lado};
    }

    /**
     * Dibuja el muñón: la mitad de arriba del brazo izquierdo (y su manga), ya colocado por {@code m.leftArm}, y el corte rojo en la punta. La
     * pila está en la raíz del modelo (como en {@code renderToBuffer}). Quien llama debe haber ocultado el brazo entero antes.
     */
    public static void munon(PlayerModel<?> m, PoseStack ps, VertexConsumer vc, int luz) {
        // del hombro (y -2) a la mitad (y 4): escalado a la mitad va de y -1 a 5, así que se recorre -1 px
        pedazo(m.leftArm, ps, vc, luz, -1F, 0.5F, 1F, 1F, 1F, 1F);
        pedazo(m.leftSleeve, ps, vc, luz, -1F, 0.5F, 1F, 1F, 1F, 1F);
        rebanada(m.leftArm, ps, vc, luz, CORTE - 0.45F);
    }

    /**
     * Dibuja la mitad de abajo del brazo izquierdo (de la mitad a la mano) donde está {@code m.leftArm} (que debe estar en la pose de la
     * máquina), con el corte rojo en su orilla de arriba.
     */
    public static void antebrazo(PlayerModel<?> m, PoseStack ps, VertexConsumer vc, int luz) {
        // escalado a la mitad va de y -1 a 5; se recorre 5 px: de y 4 (el corte) a 10 (la mano)
        pedazo(m.leftArm, ps, vc, luz, 5F, 0.5F, 1F, 1F, 1F, 1F);
        pedazo(m.leftSleeve, ps, vc, luz, 5F, 0.5F, 1F, 1F, 1F, 1F);
        rebanada(m.leftArm, ps, vc, luz, CORTE);
    }

    /** Una rebanada roja del brazo (medio px de grueso) que empieza en {@code y} px desde el hombro: el corte. */
    private static void rebanada(ModelPart a, PoseStack ps, VertexConsumer vc, int luz, float y) {
        // escalada a 0.04 va de y -0.08 a 0.4 px; se recorre para que empiece en y (y un poquito más ancha, para tapar la orilla)
        pedazo(a, ps, vc, luz, y + 0.08F, 0.04F, 1.05F, 0.5F, 0.03F, 0.04F);
    }

    /**
     * Dibuja un pedazo de la pieza: en su lugar y con su giro, recorrida {@code dy} px a lo largo de ella y escalada a lo largo
     * {@code ys} (y a lo ancho {@code xz}), con ese color.
     */
    private static void pedazo(ModelPart a, PoseStack ps, VertexConsumer vc, int luz, float dy, float ys, float xz, float r, float g, float b) {
        float x = a.x, y = a.y, z = a.z, xr = a.xRot, yr = a.yRot, zr = a.zRot, sx = a.xScale, sy = a.yScale, sz = a.zScale;
        boolean vis = a.visible;
        ps.pushPose();
        ps.translate(x / 16F, y / 16F, z / 16F);
        ps.mulPose(new Quaternionf().rotationZYX(zr, yr, xr));
        ps.translate(0, dy / 16F, 0);
        ps.scale(xz, ys, xz);
        a.x = a.y = a.z = 0F;
        a.xRot = a.yRot = a.zRot = 0F;
        a.xScale = a.yScale = a.zScale = 1F;
        a.visible = true;
        a.render(ps, vc, luz, OverlayTexture.NO_OVERLAY, r, g, b, 1F);
        a.x = x;
        a.y = y;
        a.z = z;
        a.xRot = xr;
        a.yRot = yr;
        a.zRot = zr;
        a.xScale = sx;
        a.yScale = sy;
        a.zScale = sz;
        a.visible = vis;
        ps.popPose();
    }
}
