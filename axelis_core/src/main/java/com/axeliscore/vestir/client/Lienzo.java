package com.axeliscore.vestir.client;

import com.axeliscore.plat.client.Draw;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Para dibujar encima de una prenda (lo usa un {@link Prenda.Decorador}): cada parte del cuerpo con su posición de este cuadro. Las
 * coordenadas son píxeles del modelo de jugador, como en Minecraft (y hacia abajo): en el cuerpo, el pecho está en z = -2 (y la espalda
 * en z = 2), los hombros en y = 0 y la cintura en y = 12; el centro es x = 0.
 */
public final class Lienzo {
    private final Matrix4f[] origen;
    private final Matrix3f[] origenN;
    private final PoseStack ps;
    private final Draw draw;
    private final int luz;
    private final float inflar;

    Lienzo(Matrix4f[] origen, Matrix3f[] origenN, PoseStack ps, Draw draw, int luz, float inflar) {
        this.origen = origen;
        this.origenN = origenN;
        this.ps = ps;
        this.draw = draw;
        this.luz = luz;
        this.inflar = inflar;
    }

    /** ¿Esa parte se está dibujando? (en primera persona solo hay un brazo) */
    public boolean hay(Prenda.Parte p) {
        return origen[p.ordinal()] != null;
    }

    public Draw draw() {
        return draw;
    }

    public int luz() {
        return luz;
    }

    /** Cuánto está inflada la prenda (para dibujar justo sobre su superficie). */
    public float inflado() {
        return inflar;
    }

    /**
     * Pone la pila en el espacio de una parte (dentro de un push que hay que cerrar con {@link PoseStack#popPose()}); null si esa parte no
     * se está dibujando. Unidades: bloques (1/16 = un píxel).
     */
    public PoseStack en(Prenda.Parte p) {
        if (!hay(p)) {
            return null;
        }
        ps.pushPose();
        ps.last().pose().set(origen[p.ordinal()]);
        ps.last().normal().set(origenN[p.ordinal()]);
        return ps;
    }

    /**
     * Un texto plano sobre una parte, centrado en (x, y, z) píxeles del modelo. {@code espalda}: se lee desde atrás (en la espalda o la
     * parte de atrás de una pierna); si no, desde el frente. {@code escala}: 1 = un píxel del modelo por píxel de letra.
     */
    public void texto(Prenda.Parte p, String s, float x, float y, float z, float escala, int color, boolean espalda) {
        PoseStack st = en(p);
        if (st == null) {
            return;
        }
        st.translate(x / 16F, y / 16F, z / 16F);
        float e = escala / 16F;
        // en el espacio del modelo (x e y volteados respecto al mundo) así queda derecho y sin espejo
        if (espalda) {
            st.scale(-e, e, e);
        } else {
            st.scale(e, e, -e);
        }
        draw.text(st, s, -Draw.font().width(s) / 2F + 0.5F, -3.5F, color, 0, luz);
        st.popPose();
    }
}
