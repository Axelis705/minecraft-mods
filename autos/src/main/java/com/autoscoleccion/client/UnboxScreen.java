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
    private int clicks;
    private long lastClick = -10000;
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
            clicks++;
            lastClick = now;
            Sfx.play("box_shake", 0.9F + clicks * 0.1F, 0.8F);
            Sfx.play("tape", 0.8F + clicks * 0.15F, 0.7F);
            if (clicks >= 3) {
                openAt = now;
                Sfx.play("flaps", 1.0F, 0.9F);
            }
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

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        advance();
        return true;
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
        float tape = openAt >= 0 ? 0 : 1 - clicks / 3.0F;
        float glowK = e < 0 ? 0.08F * clicks : Math.min(1, e / 500.0F);
        g.flush();
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, boxY, 300);
        ps.scale(boxScale, boxScale, -boxScale);
        ps.mulPose(Axis.ZP.rotationDegrees(180 + wob));
        ps.mulPose(Axis.XP.rotationDegrees(-24));
        ps.mulPose(Axis.YP.rotationDegrees(-28 + (float) Math.sin(now / 1400.0) * 6));
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

        // ---- textos de ayuda
        if (openAt < 0) {
            if (age > DROP) {
                float k = 0.6F + 0.4F * (float) Math.sin(now / 200.0);
                Gfx.text(g, font, "¡Haz clic para romper la cinta!  " + clicks + "/3", cx, H - 34, 1.2F, Gfx.argb(0xFFFFFF, k));
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
