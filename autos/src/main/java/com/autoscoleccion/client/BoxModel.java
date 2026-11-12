package com.autoscoleccion.client;

import com.autoscoleccion.game.Boxes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * La caja de cartón de cada serie, hecha con caras de color para poder animarla: cuerpo con franja diagonal, etiqueta y ventana, cinta
 * en la tapa y cuatro solapas que se abren. Medidas en píxeles: 12 de ancho (x), 7 de fondo (z), 8 de alto (y), apoyada en y = 0.
 */
public final class BoxModel {
    public static final float W = 12, D = 7, H = 8;

    private BoxModel() {
    }

    private static int shade(int rgb, float k) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * k));
        int g = Math.min(255, Math.round(((rgb >> 8) & 255) * k));
        int b = Math.min(255, Math.round((rgb & 255) * k));
        return (r << 16) | (g << 8) | b;
    }

    /**
     * @param open  0 = cerrada, 1 = solapas abiertas del todo
     * @param tape  cuánto de la cinta ya se despegó (0 = nada, 1 = toda)
     * @param glow  color de la luz que sale de adentro (-1 = nada) y su fuerza
     * @param pull  dónde está la mano que jala la cinta (x, y en la caja) o null
     */
    public static void render(int box, PoseStack ps, MultiBufferSource buf, int light, float open, float tape, int glow, float glowK, float[] pull) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(CarModel.WHITE));
        int main = Boxes.COLOR[box];
        int second = Boxes.COLOR2[box];
        int card = 0xC9A577;
        float x0 = -W / 2, x1 = W / 2, z0 = -D / 2, z1 = D / 2;
        // cuerpo: cuatro paredes y fondo (por fuera el color de la serie, por dentro cartón)
        CarModel.quad(vc, ps, p(x0, 0, z1), p(x1, 0, z1), p(x1, H, z1), p(x0, H, z1), main, 255, light);
        CarModel.quad(vc, ps, p(x1, 0, z0), p(x0, 0, z0), p(x0, H, z0), p(x1, H, z0), shade(main, 0.85F), 255, light);
        CarModel.quad(vc, ps, p(x1, 0, z1), p(x1, 0, z0), p(x1, H, z0), p(x1, H, z1), shade(main, 0.92F), 255, light);
        CarModel.quad(vc, ps, p(x0, 0, z0), p(x0, 0, z1), p(x0, H, z1), p(x0, H, z0), shade(main, 0.92F), 255, light);
        CarModel.quad(vc, ps, p(x0, 0, z0), p(x1, 0, z0), p(x1, 0, z1), p(x0, 0, z1), shade(main, 0.5F), 255, light);
        float e = 0.08F;
        CarModel.quad(vc, ps, p(x0 + e, e, z1 - e), p(x0 + e, H, z1 - e), p(x1 - e, H, z1 - e), p(x1 - e, e, z1 - e), card, 255, light);
        CarModel.quad(vc, ps, p(x0 + e, e, z0 + e), p(x1 - e, e, z0 + e), p(x1 - e, H, z0 + e), p(x0 + e, H, z0 + e), shade(card, 0.8F), 255, light);
        CarModel.quad(vc, ps, p(x1 - e, e, z0 + e), p(x1 - e, e, z1 - e), p(x1 - e, H, z1 - e), p(x1 - e, H, z0 + e), shade(card, 0.9F), 255, light);
        CarModel.quad(vc, ps, p(x0 + e, e, z1 - e), p(x0 + e, e, z0 + e), p(x0 + e, H, z0 + e), p(x0 + e, H, z1 - e), shade(card, 0.9F), 255, light);
        CarModel.quad(vc, ps, p(x0 + e, e, z1 - e), p(x1 - e, e, z1 - e), p(x1 - e, e, z0 + e), p(x0 + e, e, z0 + e), shade(card, 0.6F), 255, light);
        // decoración del frente y de atrás: franja diagonal, ventana y etiqueta
        for (int side = -1; side <= 1; side += 2) {
            float z = side * (D / 2 + 0.02F);
            CarModel.quad(vc, ps, p(x0, 0.6F, z), p(x0 + 4.5F, 0.6F, z), p(x1, H - 2.4F, z), p(x1 - 4.5F, H - 2.4F, z), second, 255, light);
            CarModel.quad(vc, ps, p(x0, 0, z), p(x1, 0, z), p(x1, 0.6F, z), p(x0, 0.6F, z), shade(second, 0.7F), 255, light);
            float zz = side * (D / 2 + 0.04F);
            CarModel.quad(vc, ps, p(x0 + 1.0F, 2.0F, zz), p(x0 + 7.0F, 2.0F, zz), p(x0 + 7.0F, 5.6F, zz), p(x0 + 1.0F, 5.6F, zz), 0x1B2433, 255, light);
            CarModel.quad(vc, ps, p(x0 + 1.4F, 4.6F, zz + side * 0.01F), p(x0 + 4.6F, 5.4F, zz + side * 0.01F), p(x0 + 3.2F, 5.4F, zz + side * 0.01F),
                    p(x0 + 1.4F, 5.0F, zz + side * 0.01F), 0x5A7091, 255, light);
            CarModel.quad(vc, ps, p(x1 - 4.6F, H - 1.9F, zz), p(x1 - 0.6F, H - 1.9F, zz), p(x1 - 0.6F, H - 0.6F, zz), p(x1 - 4.6F, H - 0.6F, zz), 0xF6F2E8, 255,
                    CarModel.FULL);
            for (int s = 0; s <= box; s++) {
                float sx = x1 - 4.1F + s * 1.2F;
                CarModel.quad(vc, ps, p(sx, H - 1.6F, zz + side * 0.01F), p(sx + 0.8F, H - 1.6F, zz + side * 0.01F), p(sx + 0.8F, H - 0.9F, zz + side * 0.01F),
                        p(sx, H - 0.9F, zz + side * 0.01F), Boxes.COLOR[box], 255, CarModel.FULL);
            }
        }
        // luz que sale de adentro
        if (glow >= 0 && glowK > 0.01F) {
            int a = (int) (Math.min(1, glowK) * 255);
            CarModel.quad(vc, ps, p(x0 + 0.1F, H - 0.3F, z0 + 0.1F), p(x1 - 0.1F, H - 0.3F, z0 + 0.1F), p(x1 - 0.1F, H - 0.3F, z1 - 0.1F),
                    p(x0 + 0.1F, H - 0.3F, z1 - 0.1F), shade(glow, 0.4F + 0.8F * Math.min(1, glowK)), a, CarModel.FULL);
        }
        // solapas: las cortas (en los extremos) abren primero, las largas después
        float o1 = clamp(open * 1.6F);
        float o2 = clamp(open * 1.6F - 0.45F);
        double aShort = Math.toRadians(115 * o1);
        double aLong = Math.toRadians(205 * o2);
        float sl = 3.0F;
        // extremos (bisagra en x = ±W/2)
        for (int side = -1; side <= 1; side += 2) {
            float hx = side * W / 2;
            float c = (float) Math.cos(aShort), s = (float) Math.sin(aShort);
            float tx = hx - side * sl * c, ty = H + sl * s;
            CarModel.quad(vc, ps, p(hx, H + 0.01F, z0 + 0.2F), p(hx, H + 0.01F, z1 - 0.2F), p(tx, ty + 0.01F, z1 - 0.2F), p(tx, ty + 0.01F, z0 + 0.2F),
                    shade(main, 0.95F), 255, light);
        }
        // largas (bisagra en z = ±D/2); la cinta va encima de las dos
        for (int side = -1; side <= 1; side += 2) {
            float hz = side * D / 2;
            float len = D / 2;
            float c = (float) Math.cos(aLong), s = (float) Math.sin(aLong);
            float tz = hz - side * len * c, ty = H + len * s;
            CarModel.quad(vc, ps, p(x0, H + 0.05F, hz), p(x1, H + 0.05F, hz), p(x1, ty + 0.05F, tz), p(x0, ty + 0.05F, tz), main, 255, light);
            CarModel.quad(vc, ps, p(x0, H + 0.12F, hz), p(x1, H + 0.12F, hz), p(x1, ty + 0.12F, tz), p(x0, ty + 0.12F, tz), shade(second, 1.0F), 255, light);
            if (side == 1 && tape < 0.995F && o2 < 0.02F) {
                tape(vc, ps, light, box, tape, pull);
            }
        }
    }

    private static final int TAPE = 0xF1E4C2, TAPE_EDGE = 0xD8C79C, TAPE_STICKY = 0xC9B78C, TAPE_TAB = 0xFFF6DE;

    /**
     * La cinta: la parte que sigue pegada (con orillas, brillo y la marca de la serie impresa) y la parte despegada, que es una tira
     * curva de verdad: si el jugador la está jalando va hacia su mano ({@code pull}, en coordenadas de la caja), si la soltó queda
     * enroscada hacia atrás.
     */
    private static void tape(VertexConsumer vc, PoseStack ps, int light, int box, float tape, float[] pull) {
        float tw = 0.9F;
        float x0 = -W / 2, x1 = W / 2;
        float y = H + 0.16F;
        float xs = x0 + W * Math.max(0, tape);
        // pegada arriba
        if (xs < x1) {
            CarModel.quad(vc, ps, p(xs, y, -tw), p(x1, y, -tw), p(x1, y, tw), p(xs, y, tw), TAPE, 255, light);
            CarModel.quad(vc, ps, p(xs, y + 0.01F, -tw), p(x1, y + 0.01F, -tw), p(x1, y + 0.01F, -tw + 0.1F), p(xs, y + 0.01F, -tw + 0.1F), TAPE_EDGE, 255, light);
            CarModel.quad(vc, ps, p(xs, y + 0.01F, tw - 0.1F), p(x1, y + 0.01F, tw - 0.1F), p(x1, y + 0.01F, tw), p(xs, y + 0.01F, tw), TAPE_EDGE, 255, light);
            CarModel.quad(vc, ps, p(xs, y + 0.012F, -0.18F), p(x1, y + 0.012F, -0.18F), p(x1, y + 0.012F, -0.06F), p(xs, y + 0.012F, -0.06F), 0xFFFAEC, 255,
                    light);
            // marcas impresas del color de la serie
            for (float mx = x0 + 0.6F; mx < x1 - 0.5F; mx += 1.6F) {
                if (mx < xs + 0.1F) {
                    continue;
                }
                float mx1 = Math.min(x1 - 0.2F, mx + 0.7F);
                CarModel.quad(vc, ps, p(mx, y + 0.015F, -0.4F), p(mx1, y + 0.015F, -0.4F), p(mx1, y + 0.015F, 0.4F), p(mx, y + 0.015F, 0.4F),
                        Boxes.COLOR[box], 255, light);
            }
        }
        // colita que baja por el costado de enfrente (x1)
        CarModel.quad(vc, ps, p(x1 + 0.02F, H - 2.0F, tw), p(x1 + 0.02F, y, tw), p(x1 + 0.02F, y, -tw), p(x1 + 0.02F, H - 2.0F, -tw), TAPE, 255, light);
        if (tape <= 0.0F && pull == null) {
            // aún sin tocar: colita en x0 con la pestaña doblada para agarrarla
            CarModel.quad(vc, ps, p(x0 - 0.02F, H - 2.0F, -tw), p(x0 - 0.02F, y, -tw), p(x0 - 0.02F, y, tw), p(x0 - 0.02F, H - 2.0F, tw), TAPE, 255, light);
            CarModel.quad(vc, ps, p(x0 - 0.04F, H - 2.0F, -tw), p(x0 - 0.04F, H - 1.5F, -tw), p(x0 - 0.6F, H - 2.3F, -tw * 0.8F),
                    p(x0 - 0.6F, H - 2.6F, -tw * 0.8F), TAPE_TAB, 255, CarModel.FULL);
            CarModel.quad(vc, ps, p(x0 - 0.04F, H - 2.0F, tw), p(x0 - 0.6F, H - 2.6F, tw * 0.8F), p(x0 - 0.6F, H - 2.3F, tw * 0.8F), p(x0 - 0.04F, H - 1.5F, tw),
                    TAPE_TAB, 255, CarModel.FULL);
            return;
        }
        // la tira despegada: curva de Bézier desde donde sigue pegada hasta la punta
        float len = W * Math.max(0, tape) + 2.2F;
        float ex, ey, cx, cy;
        if (pull != null) {
            ex = pull[0];
            ey = pull[1];
            float dx = ex - xs, dy = ey - y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            float max = len + 1.0F;
            if (d > max) {
                ex = xs + dx / d * max;
                ey = y + dy / d * max;
            }
            ey = Math.max(ey, y + 0.3F);
            cx = xs + (ex - xs) * 0.15F;
            cy = Math.max(y, ey) + 0.6F + Math.abs(ex - xs) * 0.12F;
        } else {
            // suelta: se enrosca hacia atrás sobre la parte ya despegada
            float l = Math.min(3.2F, 0.8F + len * 0.25F);
            ex = xs - l;
            ey = y + l * 0.55F;
            cx = xs + 0.2F;
            cy = y + l * 0.9F;
        }
        int n = 16;
        float[] px = new float[n + 1], py = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n, u = 1 - t;
            px[i] = u * u * xs + 2 * u * t * cx + t * t * ex;
            py[i] = u * u * y + 2 * u * t * cy + t * t * ey;
        }
        for (int i = 0; i < n; i++) {
            float tx = px[i + 1] - px[i], ty = py[i + 1] - py[i];
            float l = (float) Math.sqrt(tx * tx + ty * ty);
            if (l < 1e-4F) {
                continue;
            }
            float nx = -ty / l * 0.02F, ny = tx / l * 0.02F;
            float w0 = tw * (1 - 0.12F * i / n), w1 = tw * (1 - 0.12F * (i + 1) / n);
            // cara de afuera (lisa) y cara de adentro (pegajosa, más opaca)
            CarModel.quad(vc, ps, p(px[i] + nx, py[i] + ny, -w0), p(px[i + 1] + nx, py[i + 1] + ny, -w1), p(px[i + 1] + nx, py[i + 1] + ny, w1),
                    p(px[i] + nx, py[i] + ny, w0), i % 4 == 1 ? 0xFFF8E6 : TAPE, 255, light);
            CarModel.quad(vc, ps, p(px[i] - nx, py[i] - ny, w0), p(px[i + 1] - nx, py[i + 1] - ny, w1), p(px[i + 1] - nx, py[i + 1] - ny, -w1),
                    p(px[i] - nx, py[i] - ny, -w0), TAPE_STICKY, 255, light);
        }
        // pestaña en la punta
        float tx = ex - px[n - 2], ty = ey - py[n - 2];
        float l = Math.max(1e-4F, (float) Math.sqrt(tx * tx + ty * ty));
        float fx = ex + tx / l * 0.45F, fy = ey + ty / l * 0.45F;
        float w = tw * 0.88F;
        CarModel.quad(vc, ps, p(ex, ey, -w), p(fx, fy, -w * 0.8F), p(fx, fy, w * 0.8F), p(ex, ey, w), TAPE_TAB, 255, CarModel.FULL);
    }

    private static float clamp(float v) {
        return Math.max(0, Math.min(1, v));
    }

    private static float[] p(float x, float y, float z) {
        return new float[] {x, y, z};
    }
}
