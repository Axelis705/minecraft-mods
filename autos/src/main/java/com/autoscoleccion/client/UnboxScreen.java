package com.autoscoleccion.client;

import com.autoscoleccion.game.Boxes;
import com.autoscoleccion.game.CarDef;
import com.autoscoleccion.game.CarMesh;
import com.autoscoleccion.game.Cars;
import com.autoscoleccion.game.Rarity;
import com.autoscoleccion.net.AutosNet;
import com.autoscoleccion.net.ClaimPacket;
import com.autoscoleccion.net.UnboxPacket;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Abrir una caja, como en los juegos de colección: la caja cae y rebota, con tres clics se rompe la cinta, se abren las solapas, sale
 * una luz del color de la rareza, las estrellas aparecen una por una, el auto sube girando como silueta y con un destello se revela a
 * color con su tarjeta (y "¡NUEVO!" si no lo tenías). Los épicos y legendarios llevan confeti y temblor.
 */
public class UnboxScreen extends Screen {
    private static final int DROP = 750;
    private static final int OPEN = 700;
    private static final int REVEAL = 2300;

    private final UnboxPacket m;
    private final CarDef car;
    private final long t0 = System.currentTimeMillis();
    private final int hadBefore;
    /** Cuánto de la cinta ya se despegó (0 a 1). */
    private float peel;
    private float lastRip;
    private boolean grabbing;
    private double mouseX, mouseY;
    private long lastClick = -10000;
    /** Transformación de la caja en pantalla (para saber dónde está la cinta). */
    private final org.joml.Matrix4f boxMat = new org.joml.Matrix4f();
    private boolean boxMatSet;
    private final List<float[]> scraps = new ArrayList<>();
    private long openAt = -1;
    private boolean claimed;
    private boolean landed;
    private int starsShown;
    private boolean revealedSfx;
    private final Random rnd = new Random();
    private final List<float[]> confetti = new ArrayList<>();
    private long lastFrame = System.currentTimeMillis();

    public UnboxScreen(UnboxPacket m) {
        super(Component.literal("Abrir caja"));
        this.m = m;
        this.car = Cars.get(Math.max(0, Math.min(Cars.COUNT - 1, m.car())));
        this.hadBefore = AutosClient.counts[car.index()];
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private long sinceOpen(long now) {
        return openAt < 0 ? -1 : now - openAt;
    }

    private void advance() {
        long now = System.currentTimeMillis();
        if (now - t0 < DROP) {
            return;
        }
        if (openAt < 0) {
            // con teclado: despega un buen pedazo de cinta de golpe
            tearTo(Math.min(1, peel + 0.34F));
            return;
        }
        long e = now - openAt;
        if (e < REVEAL) {
            openAt = now - REVEAL;
            starsShown = car.rarity.stars;
            return;
        }
        if (e > REVEAL + 500) {
            onClose();
        }
    }

    // ---------------------------------------------------------------------------------------------------- la cinta

    private float[] project(float x, float y, float z) {
        org.joml.Vector4f v = new org.joml.Vector4f(x, y, z, 1).mul(boxMat);
        return new float[] {v.x, v.y};
    }

    /** Punta de la cinta para agarrarla y el principio/fin del tramo de arriba, en pantalla. */
    private float[][] tapePoints() {
        float x0 = -BoxModel.W / 2, x1 = BoxModel.W / 2, h = BoxModel.H + 0.16F;
        return new float[][] {project(x0 - 0.3F, BoxModel.H - 2.2F, 0), project(x0, h, 0), project(x1, h, 0)};
    }

    /** Despega la cinta hasta {@code to}: sonido de rasgado, papelitos y, al final, se abre la caja. */
    private void tearTo(float to) {
        if (to <= peel || openAt >= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        float old = peel;
        peel = to;
        if (peel - lastRip > 0.05F || old == 0) {
            lastRip = peel;
            Sfx.play("tape", 0.75F + peel * 0.6F, 0.55F);
            lastClick = now;
        }
        if (boxMatSet) {
            float[][] tp = tapePoints();
            float sx = tp[1][0] + (tp[2][0] - tp[1][0]) * peel, sy = tp[1][1] + (tp[2][1] - tp[1][1]) * peel;
            for (int i = 0; i < 3; i++) {
                scraps.add(new float[] {sx, sy, (rnd.nextFloat() - 0.5F) * 80, -40 - rnd.nextFloat() * 60, 1.0F, rnd.nextFloat() * 6.28F});
            }
        }
        if (peel >= 0.97F) {
            peel = 1;
            grabbing = false;
            openAt = now;
            Sfx.play("box_shake", 1.1F, 0.8F);
            Sfx.play("flaps", 1.0F, 0.9F);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        mouseX = x;
        mouseY = y;
        if (openAt < 0) {
            if (System.currentTimeMillis() - t0 < DROP || !boxMatSet) {
                return true;
            }
            float[][] tp = tapePoints();
            // se agarra la pestaña (al principio) o la orilla de lo que ya se despegó
            float gx = peel <= 0 ? tp[0][0] : tp[1][0] + (tp[2][0] - tp[1][0]) * peel;
            float gy = peel <= 0 ? tp[0][1] : tp[1][1] + (tp[2][1] - tp[1][1]) * peel;
            float r = Math.max(14, Math.min(width, height) / 18.0F);
            if ((x - gx) * (x - gx) + (y - gy) * (y - gy) <= r * r) {
                grabbing = true;
                Sfx.play("key", 0.8F, 0.5F);
            }
            return true;
        }
        advance();
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        mouseX = x;
        mouseY = y;
        if (grabbing && openAt < 0 && boxMatSet) {
            float[][] tp = tapePoints();
            float ax = tp[1][0], ay = tp[1][1], bx = tp[2][0], by = tp[2][1];
            float vx = bx - ax, vy = by - ay;
            float t = (float) (((x - ax) * vx + (y - ay) * vy) / Math.max(1e-3, vx * vx + vy * vy));
            // al principio basta con jalar la pestaña hacia arriba
            if (peel <= 0 && y < tp[0][1] - 6) {
                t = Math.max(t, 0.02F);
            }
            tearTo(Math.max(0, Math.min(1, t)));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        grabbing = false;
        return true;
    }

    @Override
    public void mouseMoved(double x, double y) {
        mouseX = x;
        mouseY = y;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_E) {
            advance();
        }
        return true;
    }

    @Override
    public void onClose() {
        if (!claimed) {
            claimed = true;
            AutosNet.toServer(new ClaimPacket(ClaimPacket.CLAIM_CAR));
        }
        super.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1F, (now - lastFrame) / 1000.0F);
        lastFrame = now;
        long age = now - t0;
        long e = sinceOpen(now);
        boolean revealed = e >= REVEAL;
        Rarity r = car.rarity;
        int boxCol = Boxes.COLOR[m.box()];
        int rarCol = r.rgb;
        int W = width, H = height;
        float cx = W / 2.0F, cy = H / 2.0F;

        // sonidos de la línea de tiempo
        if (!landed && age >= DROP * 0.62F) {
            landed = true;
            Sfx.play("box_drop", 1.0F, 0.9F);
        }
        if (e >= 0) {
            int want = (int) Math.max(0, Math.min(r.stars, (e - 900) / 260 + 1));
            if (e < 900) {
                want = 0;
            }
            while (starsShown < want) {
                starsShown++;
                Sfx.play("sparkle", 0.8F + starsShown * 0.12F, 0.8F);
            }
            if (revealed && !revealedSfx) {
                revealedSfx = true;
                Sfx.play(switch (r) {
                    case LEGENDARIO -> "reveal_legend";
                    case EPICO -> "reveal_epic";
                    case RARO -> "reveal_rare";
                    default -> "reveal_common";
                }, 1.0F, 1.0F);
                if (m.isNew()) {
                    Sfx.play("new_car", 1.0F, 0.9F);
                }
                int n = r == Rarity.LEGENDARIO ? 160 : r == Rarity.EPICO ? 90 : r == Rarity.RARO ? 40 : 18;
                for (int i = 0; i < n; i++) {
                    double a = -Math.PI / 2 + (rnd.nextDouble() - 0.5) * 2.4;
                    float sp = 120 + rnd.nextFloat() * 260;
                    int[] pal = {rarCol, 0xFFFFFF, boxCol, Boxes.COLOR2[m.box()]};
                    confetti.add(new float[] {cx, cy - 10, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp, 2.6F, pal[rnd.nextInt(4)],
                            rnd.nextFloat() * 6.28F, 2 + rnd.nextFloat() * 3});
                }
            }
        }

        // temblor de pantalla para épicos y legendarios justo antes de revelar
        g.pose().pushPose();
        if (r.stars >= 4 && e > 1400 && e < REVEAL + 300) {
            float k = r.stars == 5 ? 3.0F : 1.6F;
            g.pose().translate((rnd.nextFloat() - 0.5F) * k, (rnd.nextFloat() - 0.5F) * k, 0);
        }

        // ---- fondo: oscuro con un foco del color de la caja (luego de la rareza)
        int light = revealed ? rarCol : e >= 0 ? Gfx.mix(boxCol, rarCol, Math.min(1, e / 1600.0F)) : boxCol;
        Gfx.begin(g, false);
        Gfx.vgrad(0, 0, W, H, 0xF0040409, 0xF0100A18);
        Gfx.end();
        Gfx.begin(g, true);
        Gfx.blob(cx, cy, Math.max(W, H) * 0.55F, Gfx.argb(light, revealed ? 0.35F : 0.18F));
        // rayos de luz que giran
        if (e > 250) {
            float k = Math.min(1, (e - 250) / 900.0F) * (revealed ? 1.0F : 0.7F);
            int rays = r == Rarity.LEGENDARIO ? 18 : r == Rarity.EPICO ? 14 : 10;
            float len = Math.max(W, H) * 0.8F;
            for (int layer = 0; layer < (r.stars >= 4 ? 2 : 1); layer++) {
                double rot = (now / (layer == 0 ? 2600.0 : -3900.0));
                for (int i = 0; i < rays; i++) {
                    double a = rot + i * Math.PI * 2 / rays;
                    double w = 0.09 + 0.03 * Math.sin(now / 300.0 + i);
                    float c0 = (float) Math.cos(a - w), s0 = (float) Math.sin(a - w), c1 = (float) Math.cos(a + w), s1 = (float) Math.sin(a + w);
                    int c = Gfx.argb(layer == 0 ? light : 0xFFFFFF, 0.32F * k);
                    Gfx.quad(cx, cy, c, cx + c0 * len, cy + s0 * len, Gfx.argb(light, 0), cx + c1 * len, cy + s1 * len, Gfx.argb(light, 0), cx, cy, c);
                }
            }
        }
        Gfx.end();

        // ---- la caja
        float S = Math.min(W, H) / 26.0F;
        float drop = Math.min(1, age / (float) DROP);
        float boxY = cy + 30 - (1 - bounce(drop)) * (H * 0.7F);
        float boxScale = S;
        if (revealed) {
            float k = Gfx.ease((e - REVEAL) / 600.0F);
            boxY += k * H * 0.32F;
            boxScale *= 1 - 0.35F * k;
        }
        float wob = 0;
        long sc = now - lastClick;
        if (sc < 450) {
            wob = (float) Math.sin(sc * 0.06) * 9 * (1 - sc / 450.0F);
        } else if (openAt < 0 && age > DROP) {
            wob = (float) Math.sin(now / 160.0) * 1.2F;
        }
        float open = e < 0 ? 0 : Math.min(1, e / (float) OPEN);
        float tape = openAt >= 0 ? 1 : peel;
        float glowK = e < 0 ? 0.25F * peel : Math.min(1, e / 500.0F);
        g.flush();
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, boxY, 300);
        ps.scale(boxScale, boxScale, -boxScale);
        ps.mulPose(Axis.ZP.rotationDegrees(180 + wob));
        ps.mulPose(Axis.XP.rotationDegrees(-24));
        ps.mulPose(Axis.YP.rotationDegrees(-28 + (float) Math.sin(now / 1400.0) * 6));
        boxMat.set(ps.last().pose());
        boxMatSet = true;
        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource buf = g.bufferSource();
        BoxModel.render(m.box(), ps, buf, CarModel.FULL, open, tape, light, glowK);
        buf.endBatch();
        Lighting.setupFor3DItems();
        ps.popPose();

        // ---- el auto que sube
        if (e > 600) {
            float rise = Gfx.ease((e - 600) / 1300.0F);
            float carSize = S * (0.45F + 0.55F * rise) * (revealed ? 1.15F : 1.0F);
            float carY = boxY - S * 7 * rise - (revealed ? 18 : 0);
            float yaw = (now - openAt) * 0.09F;
            CarModel.renderInGui(g, car.style, cx, carY, 420, carSize, yaw, 18, revealed ? CarMesh.MODE_NORMAL : CarMesh.MODE_SILHOUETTE,
                    (now % 100000) / 50.0F);
            if (!revealed) {
                Gfx.begin(g, true);
                Gfx.blob(cx, carY, carSize * 9, Gfx.argb(light, 0.25F * rise));
                Gfx.end();
            }
            // estrellas
            float starY = carY - carSize * 6.5F - 12;
            for (int i = 0; i < r.stars; i++) {
                float sx = cx + (i - (r.stars - 1) / 2.0F) * 20;
                boolean on = i < starsShown;
                float pop = on ? Gfx.back(Math.min(1, (e - 900 - i * 260) / 220.0F)) : 0.7F;
                if (revealed) {
                    pop = 1;
                }
                Gfx.text(g, font, "★", sx, starY, 2.0F * Math.max(0.6F, pop), on ? 0xFF000000 | rarCol : 0x60FFFFFF);
            }
        }

        // ---- destello al revelar
        if (e >= REVEAL && e < REVEAL + 600) {
            float k = 1 - (e - REVEAL) / 600.0F;
            Gfx.begin(g, false);
            Gfx.rect(0, 0, W, H, Gfx.argb(0xFFFFFF, k * k));
            Gfx.end();
        }

        // ---- tarjeta con el nombre
        if (revealed) {
            // la tarjeta va por delante del auto (el auto se dibuja en 3D y tapaba el nombre)
            g.pose().pushPose();
            g.pose().translate(0, 0, 900);
            card(g, now, e);
            g.pose().popPose();
        }

        // ---- la cinta despegada (en 2D, de la orilla pegada hasta el mouse) y papelitos
        if (openAt < 0 && boxMatSet && age > DROP) {
            float[][] tp = tapePoints();
            float ex = tp[1][0] + (tp[2][0] - tp[1][0]) * peel, ey = tp[1][1] + (tp[2][1] - tp[1][1]) * peel;
            float tw = Math.max(3, S * 0.75F);
            Gfx.begin(g, false);
            if (grabbing) {
                float hx = (float) mouseX, hy = (float) mouseY;
                // curva suave: del borde pegado sube un poco y luego va a la mano
                float px = ex, py = ey;
                for (int i = 1; i <= 10; i++) {
                    float t = i / 10.0F;
                    float qx = ex + (hx - ex) * t, qy = ey + (hy - ey) * t - (float) Math.sin(t * Math.PI) * tw * 1.5F;
                    int col = 0xFF000000 | Gfx.mix(0xE9DCB8, 0xFFF6DE, t);
                    Gfx.line(px, py, qx, qy, tw, col);
                    px = qx;
                    py = qy;
                }
                Gfx.circle(hx, hy, tw * 0.75F, 0xFFFFF6DE);
            } else if (peel > 0) {
                // colita enrollada que quedó levantada
                Gfx.line(ex, ey, ex - tw * 1.2F, ey - tw * 2.2F, tw, 0xFFE9DCB8);
                Gfx.circle(ex - tw * 1.2F, ey - tw * 2.2F, tw * 0.7F, 0xFFFFF6DE);
            }
            Gfx.end();
            if (!grabbing) {
                // aro que late donde hay que agarrar
                float gx = peel <= 0 ? tp[0][0] : ex, gy = peel <= 0 ? tp[0][1] : ey;
                float pulse = (now % 1100) / 1100.0F;
                Gfx.begin(g, true);
                Gfx.arc(gx, gy, 6 + pulse * 14, 8 + pulse * 14, 0, Math.PI * 2, Gfx.argb(0xFFFFFF, 0.8F * (1 - pulse)), Gfx.argb(0xFFFFFF, 0), 28);
                Gfx.blob(gx, gy, 10, Gfx.argb(0xFFF4D8, 0.35F));
                Gfx.end();
            }
        }
        if (!scraps.isEmpty()) {
            Gfx.begin(g, false);
            for (int i = scraps.size() - 1; i >= 0; i--) {
                float[] p = scraps.get(i);
                p[3] += 260 * dt;
                p[0] += p[2] * dt;
                p[1] += p[3] * dt;
                p[4] -= dt * 1.2F;
                p[5] += dt * 8;
                if (p[4] <= 0) {
                    scraps.remove(i);
                    continue;
                }
                float c = (float) Math.cos(p[5]) * 2.2F, sn = (float) Math.sin(p[5]) * 1.4F;
                int col = Gfx.argb(0xF2E6C6, Math.min(1, p[4]));
                Gfx.quad(p[0] - c, p[1] - sn, col, p[0] + sn, p[1] - c, col, p[0] + c, p[1] + sn, col, p[0] - sn, p[1] + c, col);
            }
            Gfx.end();
        }

        // ---- textos de ayuda
        if (openAt < 0) {
            if (age > DROP) {
                float k = 0.6F + 0.4F * (float) Math.sin(now / 200.0);
                String msg = grabbing ? "¡Sigue jalando!  " + Math.round(peel * 100) + "%" : peel > 0 ? "¡Agarra la cinta y sigue jalando!" : "¡Agarra la punta de la cinta y arrástrala!";
                Gfx.text(g, font, msg, cx, H - 34, 1.2F, Gfx.argb(0xFFFFFF, k));
                Gfx.text(g, font, "(o pulsa Espacio)", cx, H - 20, 0.7F, 0x70FFFFFF);
            }
            Gfx.text(g, font, "Caja " + Boxes.NAME[m.box()], cx, 16, 1.6F, 0xFF000000 | boxCol);
            Gfx.text(g, font, Boxes.TAGLINE[m.box()], cx, 34, 0.9F, 0xC0FFFFFF);
        } else if (!revealed) {
            Gfx.text(g, font, "clic para saltar", cx, H - 14, 0.7F, 0x60FFFFFF);
        } else if (e > REVEAL + 500) {
            Gfx.text(g, font, "Clic para guardarlo en tu garaje", cx, H - 14, 0.85F, Gfx.argb(0xFFFFFF, 0.6F + 0.4F * (float) Math.sin(now / 220.0)));
        }

        confetti(g, dt);
        g.pose().popPose();
    }

    private static float bounce(float t) {
        if (t < 0.62F) {
            float k = t / 0.62F;
            return k * k;
        }
        float k = (t - 0.62F) / 0.38F;
        return 1 - (float) Math.sin(k * Math.PI) * 0.12F * (1 - k);
    }

    private void card(GuiGraphics g, long now, long e) {
        Rarity r = car.rarity;
        float k = Gfx.back(Math.min(1, (e - REVEAL - 150) / 450.0F));
        if (k <= 0) {
            return;
        }
        float cw = Math.min(width - 30, 260), ch = 74;
        float x = width / 2.0F - cw / 2, y = height - 22 - ch + (1 - k) * 120;
        Gfx.begin(g, false);
        Gfx.round(x, y, x + cw, y + ch, 7, 0xF0121220);
        Gfx.hgrad(x + 2, y + 2, x + cw - 2, y + 18, Gfx.argb(r.rgb, 0.85F), Gfx.argb(r.rgb, 0.15F));
        Gfx.end();
        Gfx.begin(g, true);
        Gfx.glow(x, y, x + cw, y + ch, Gfx.argb(r.rgb, 0.5F), 7);
        Gfx.end();
        Gfx.textL(g, font, r.label.toUpperCase() + "  ·  Nº " + car.number + "/" + Cars.COUNT, x + 8, y + 6, 0.8F, 0xFF101018);
        Gfx.textL(g, font, car.name, x + 8, y + 22, 1.6F, 0xFF000000 | r.rgb);
        List<FormattedCharSequence> lines = font.split(Component.literal(car.blurb), (int) ((cw - 16) / 0.8F));
        float ly = y + 42;
        for (FormattedCharSequence l : lines) {
            g.pose().pushPose();
            g.pose().translate(x + 8, ly, 0);
            g.pose().scale(0.8F, 0.8F, 1);
            g.drawString(font, l, 0, 0, 0xFFCFCFE0, true);
            g.pose().popPose();
            ly += 9;
        }
        Gfx.textL(g, font, "Serie " + Boxes.NAME[car.line], x + 8, y + ch - 10, 0.7F, 0xFF9A9AC0);
        // insignia
        g.pose().pushPose();
        g.pose().translate(x + cw - 34, y + 4, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(12 + (float) Math.sin(now / 180.0) * 4));
        if (m.isNew()) {
            float s = 1.0F + 0.08F * (float) Math.sin(now / 120.0);
            g.pose().scale(s, s, 1);
            Gfx.begin(g, false);
            Gfx.round(-26, -9, 26, 9, 4, 0xFFFF3A6A);
            Gfx.end();
            Gfx.text(g, font, "¡NUEVO!", 0, -4, 1.0F, 0xFFFFFFFF);
        } else {
            Gfx.begin(g, false);
            Gfx.round(-30, -9, 30, 9, 4, 0xFF34345A);
            Gfx.end();
            Gfx.text(g, font, "Repetido x" + (hadBefore + 1), 0, -4, 0.8F, 0xFFE0E0F0);
        }
        g.pose().popPose();
    }

    private void confetti(GuiGraphics g, float dt) {
        if (confetti.isEmpty()) {
            return;
        }
        Gfx.begin(g, false);
        for (int i = confetti.size() - 1; i >= 0; i--) {
            float[] c = confetti.get(i);
            c[0] += c[2] * dt;
            c[1] += c[3] * dt;
            c[3] += 260 * dt;
            c[2] *= 1 - 0.8F * dt;
            c[4] -= dt;
            c[6] += dt * 8;
            if (c[4] <= 0 || c[1] > height + 10) {
                confetti.remove(i);
                continue;
            }
            float w = c[7] * Math.abs((float) Math.cos(c[6])), h = c[7] * 0.6F;
            Gfx.rect(c[0] - w, c[1] - h, c[0] + w, c[1] + h, Gfx.argb((int) c[5], Math.min(1, c[4])));
        }
        Gfx.end();
    }
}
