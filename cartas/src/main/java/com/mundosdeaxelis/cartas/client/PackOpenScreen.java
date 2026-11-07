package com.mundosdeaxelis.cartas.client;

import com.mundosdeaxelis.cartas.CardDef;
import com.mundosdeaxelis.cartas.CardRarity;
import com.mundosdeaxelis.cartas.Cartas;
import com.mundosdeaxelis.cartas.Cards;
import com.mundosdeaxelis.cartas.CartasNet;
import com.mundosdeaxelis.cartas.PackDef;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * Apertura de un sobre: se mantiene el clic para rasgarlo, estalla en luz y las cartas se voltean una a una
 * (en el orden en que salieron, al azar). Las cartas llegan al inventario cuando termina la apertura: al voltear la
 * ultima carta (o al cerrar la pantalla antes) se le pide al servidor que las entregue.
 */
public final class PackOpenScreen extends Screen {
    private static final int VW = 360;
    private static final int VH = 250;
    private static final int CW = 52;
    private static final int CH = 78;
    private static final int GAP = 6;

    private enum Phase { TEAR, BURST, REVEAL }

    private static final class Spark {
        float x, y, vx, vy, life, max;
        int rgb;
        boolean rainbow;
    }

    private final PackDef def;
    private final List<CardDef> cards = new ArrayList<>();
    private final List<Boolean> holo = new ArrayList<>();
    private final List<Boolean> isNew = new ArrayList<>();
    private final float[] flip;
    private final boolean[] done;
    private final boolean[] started;
    private final List<Spark> sparks = new ArrayList<>();
    private final Random rnd = new Random();
    private final ResourceLocation packTex;

    private Phase phase = Phase.TEAR;
    private boolean holding;
    private float progress;
    private float tearSound;
    private float burst;
    private float flash;
    private float revealClock;
    private boolean auto;
    private float autoClock;
    private float time;
    private float shake;
    private int hover = -1;

    private long lastNs = System.nanoTime();
    private float sc = 1f;
    private float ox;
    private float oy;

    public PackOpenScreen(CartasNet.PackOpen m) {
        super(Component.literal("Sobre"));
        PackDef p = Cards.pack(m.pack());
        this.def = p != null ? p : Cards.PACKS.get(0);
        for (int i = 0; i < m.ids().size(); i++) {
            CardDef d = Cards.get(m.ids().get(i));
            if (d != null) {
                cards.add(d);
                holo.add(m.holo().get(i));
                isNew.add(m.isNew().get(i));
            }
        }
        flip = new float[cards.size()];
        done = new boolean[cards.size()];
        started = new boolean[cards.size()];
        packTex = new ResourceLocation(Cartas.MOD_ID, "textures/gui/cartas/sobre_" + def.id + ".png");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float cardX(int i) {
        int n = cards.size();
        float total = n * CW + (n - 1) * GAP;
        return (VW - total) / 2f + i * (CW + GAP);
    }

    private static final float CARD_Y = 76f;

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        long now = System.nanoTime();
        float dt = Mth.clamp((now - lastNs) / 1e9f, 0.0001f, 0.05f);
        lastNs = now;
        time += dt;
        shake = Math.max(0f, shake - dt);
        flash = Math.max(0f, flash - dt * 1.6f);
        update(dt);

        renderBackground(g);
        sc = Math.min(width / (float) VW, height / (float) VH) * 0.96f;
        ox = (width - VW * sc) / 2f;
        oy = (height - VH * sc) / 2f;
        float vx = (mx - ox) / sc;
        float vy = (my - oy) / sc;

        g.pose().pushPose();
        g.pose().translate(ox, oy, 0f);
        g.pose().scale(sc, sc, 1f);
        if (shake > 0f) {
            g.pose().translate(Mth.sin(time * 100f) * shake * 4f, Mth.cos(time * 83f) * shake * 3f, 0f);
        }
        if (phase == Phase.TEAR) {
            drawPack(g, vx, vy);
        } else if (phase == Phase.BURST) {
            drawPack(g, vx, vy);
        } else {
            drawCards(g, vx, vy);
        }
        drawSparks(g);
        if (flash > 0f) {
            g.fill(-200, -200, VW + 200, VH + 200, ((int) (Mth.clamp(flash, 0f, 1f) * 235) << 24) | 0xFFFFFF);
        }
        g.pose().popPose();
    }

    // ------------------------------------------------------------------ logica

    private boolean overPack(float vx, float vy) {
        return vx >= VW / 2f - 64 && vx < VW / 2f + 64 && vy >= 30 && vy < 222;
    }

    private void update(float dt) {
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            s.life -= dt;
            if (s.life <= 0f) {
                sparks.remove(i);
                continue;
            }
            s.vy += 120f * dt;
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.vx *= 1f - dt;
        }
        switch (phase) {
            case TEAR -> {
                if (holding) {
                    progress = Math.min(1f, progress + dt * 0.85f);
                    tearSound -= dt;
                    if (tearSound <= 0f) {
                        tearSound = 0.11f;
                        CartasClient.play(SoundEvents.BOOK_PAGE_TURN, 1.3f + progress * 0.9f, 0.35f);
                    }
                    if (progress >= 1f) {
                        phase = Phase.BURST;
                        burst = 0f;
                        flash = 1f;
                        shake = 0.35f;
                        CartasClient.play(SoundEvents.FIREWORK_ROCKET_BLAST, 1.1f, 0.8f);
                        CartasClient.play(SoundEvents.ENCHANTMENT_TABLE_USE, 1.4f, 0.8f);
                        explode(VW / 2f, 110f, def.color, false, 70);
                    }
                } else {
                    progress = Math.max(0f, progress - dt * 0.55f);
                }
            }
            case BURST -> {
                burst += dt;
                if (burst > 0.55f) {
                    phase = Phase.REVEAL;
                    revealClock = 0f;
                }
            }
            case REVEAL -> {
                revealClock += dt;
                autoClock -= dt;
                if (auto && autoClock <= 0f) {
                    for (int i = 0; i < cards.size(); i++) {
                        if (!started[i]) {
                            startFlip(i);
                            autoClock = 0.28f;
                            break;
                        }
                    }
                }
                for (int i = 0; i < cards.size(); i++) {
                    if (started[i] && !done[i]) {
                        flip[i] = Math.min(1f, flip[i] + dt / 0.5f);
                        if (flip[i] >= 1f) {
                            done[i] = true;
                            landed(i);
                            if (allDone()) {
                                claim();
                            }
                        }
                    }
                }
            }
            default -> {
            }
        }
    }

    private void startFlip(int i) {
        if (started[i]) {
            return;
        }
        started[i] = true;
        CartasClient.play(SoundEvents.BOOK_PAGE_TURN, 1.5f, 0.5f);
    }

    private void landed(int i) {
        CardDef d = cards.get(i);
        boolean h = holo.get(i);
        float cx = cardX(i) + CW / 2f;
        float cy = CARD_Y + CH / 2f;
        CartasClient.play(SoundEvents.BOOK_PUT, 1.4f, 0.5f);
        int r = d.rarity.ordinal();
        explode(cx, cy, d.rarity.color, false, 8 + r * 14);
        if (d.rarity.atLeast(CardRarity.RARA)) {
            CartasClient.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f + r * 0.15f, 0.8f);
        }
        if (d.rarity.atLeast(CardRarity.LEGENDARIA)) {
            CartasClient.play(SoundEvents.PLAYER_LEVELUP, 1.3f, 0.45f);
            shake = 0.25f;
        }
        if (d.rarity == CardRarity.MITICA) {
            CartasClient.play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.7f);
            CartasClient.play(SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 0.6f);
            flash = 0.5f;
        }
        if (h) {
            CartasClient.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.3f, 0.9f);
            explode(cx, cy, 0x7FE8FF, true, 40);
        }
    }

    private void explode(float x, float y, int rgb, boolean rainbow, int n) {
        for (int i = 0; i < n; i++) {
            Spark s = new Spark();
            float a = rnd.nextFloat() * Mth.TWO_PI;
            float v = 30f + rnd.nextFloat() * 110f;
            s.x = x;
            s.y = y;
            s.vx = Mth.cos(a) * v;
            s.vy = Mth.sin(a) * v - 30f;
            s.max = s.life = 0.6f + rnd.nextFloat() * 0.8f;
            s.rgb = rgb;
            s.rainbow = rainbow;
            sparks.add(s);
        }
    }

    private boolean claimed;

    /** Pide al servidor las cartas del sobre (una sola vez). */
    private void claim() {
        if (!claimed) {
            claimed = true;
            CartasNet.CHANNEL.sendToServer(new CartasNet.ClaimPack());
        }
    }

    @Override
    public void removed() {
        // si cierran la pantalla antes de terminar, las cartas se entregan igual
        claim();
        super.removed();
    }

    private boolean allDone() {
        for (boolean b : done) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ dibujo

    private void drawPack(GuiGraphics g, float vx, float vy) {
        float cx = VW / 2f;
        g.drawCenteredString(font, def.name, (int) cx, 10, 0xFF000000 | def.color);
        float sx = phase == Phase.BURST ? Math.max(0f, 1f - burst * 4f) : 1f;
        float shk = progress * progress * 3.5f;
        float ox2 = Mth.sin(time * 70f) * shk;
        float oy2 = Mth.cos(time * 57f) * shk * 0.6f;
        float scale = 2f * (1f + progress * 0.06f) * (overPack(vx, vy) && phase == Phase.TEAR ? 1.03f : 1f);
        float w = 64 * scale;
        float h = 96 * scale;
        float x = cx - w / 2f + ox2;
        float y = 126f - h / 2f + oy2;
        if (phase == Phase.TEAR) {
            CardRender.glow(g, x, y, w, h, def.color, 0.3f + progress * 1.6f);
        }
        if (sx > 0f) {
            g.pose().pushPose();
            g.pose().translate(cx, 126f, 0f);
            g.pose().scale(sx, 1f, 1f);
            g.pose().translate(-cx, -126f, 0f);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            g.pose().pushPose();
            g.pose().translate(x, y, 0f);
            g.pose().scale(scale, scale, 1f);
            g.blit(packTex, 0, 0, 0f, 0f, 64, 96, 64, 96);
            g.pose().popPose();
            // la linea de rasgado, de izquierda a derecha, con un brillo que la recorre
            if (progress > 0f) {
                float ty = y + h * 0.17f;
                float len = w * progress;
                g.fill((int) x, (int) ty - 1, (int) (x + len), (int) ty + 2, 0xFF1A1208);
                g.fill((int) x, (int) ty, (int) (x + len), (int) ty + 1, 0xFFFFF4D8);
                float tipx = x + len;
                g.fill((int) tipx - 2, (int) ty - 3, (int) tipx + 3, (int) ty + 4, 0xC0FFFFFF);
            }
            g.pose().popPose();
        }
        if (phase == Phase.TEAR) {
            g.drawCenteredString(font, "Mantén pulsado el clic sobre el sobre para abrirlo", (int) cx, 232, 0xFFE8DCC8);
        }
    }

    private void drawCards(GuiGraphics g, float vx, float vy) {
        float cx = VW / 2f;
        g.drawCenteredString(font, def.name, (int) cx, 10, 0xFF000000 | def.color);
        hover = -1;
        for (int i = 0; i < cards.size(); i++) {
            float delay = i * 0.09f;
            float t = Mth.clamp((revealClock - delay) / 0.45f, 0f, 1f);
            float e = 1f - (float) Math.pow(1f - t, 3);
            float x = Mth.lerp(e, cx - CW / 2f, cardX(i));
            float y = Mth.lerp(e, 110f, CARD_Y) - Mth.sin(e * Mth.PI) * 14f;
            if (t <= 0f) {
                continue;
            }
            boolean hov = t >= 1f && !started[i] && vx >= x && vx < x + CW && vy >= y && vy < y + CH;
            if (hov) {
                hover = i;
                y -= 4f;
            }
            CardDef d = cards.get(i);
            boolean h = holo.get(i);
            float fl = flip[i];
            if (done[i]) {
                float pulse = d.rarity == CardRarity.MITICA ? 0.8f + 0.4f * Mth.sin(time * 4f) : 1f;
                if (d.rarity.atLeast(CardRarity.RARA)) {
                    CardRender.glow(g, x, y, CW, CH, h ? Mth.hsvToRgb((time * 0.3f) % 1f, 0.5f, 1f) : d.rarity.color, (0.3f + d.rarity.ordinal() * 0.35f) * pulse);
                }
            } else if (hov) {
                CardRender.glow(g, x, y, CW, CH, def.color, 0.6f);
            }
            float sxs = Math.max(0.02f, Math.abs(Mth.cos(fl * Mth.PI)));
            g.pose().pushPose();
            g.pose().translate(x + CW / 2f, y + CH / 2f, 0f);
            g.pose().scale(sxs, 1f, 1f);
            if (fl < 0.5f) {
                CardRender.back(g, -CW / 2f, -CH / 2f, CW, CH, 1f);
            } else {
                CardRender.face(g, d, -CW / 2f, -CH / 2f, CW, CH);
                if (h) {
                    CardRender.holo(g, -CW / 2f, -CH / 2f, CW, CH, time, (d.index * 0.137f) % 1f);
                }
            }
            g.pose().popPose();
            if (done[i]) {
                g.pose().pushPose();
                g.pose().translate(x + CW / 2f, y + CH + 5f, 0f);
                // el nombre se encoge para caber bajo su carta y no pisar al de al lado
                float lw = CW + GAP - 2f;
                int wide = Math.max(font.width(d.name), Math.max(font.width(d.rarity.label), h ? font.width("✦ Holo") : 0));
                float ls = Math.min(0.8f, lw / Math.max(1, wide));
                g.pose().scale(ls, ls, 1f);
                g.drawCenteredString(font, d.name, 0, 0, 0xFF000000 | d.rarity.color);
                g.drawCenteredString(font, d.rarity.label, 0, 10, 0xFFB8B8C0);
                if (h) {
                    g.drawCenteredString(font, "✦ Holo", 0, 20, 0xFF7FE8FF);
                }
                g.pose().popPose();
                if (isNew.get(i)) {
                    float bob = Mth.sin(time * 5f + i) * 1.5f;
                    g.fill((int) x - 3, (int) (y - 9 + bob), (int) x + 33, (int) (y + 1 + bob), 0xFFE0402A);
                    g.drawString(font, "¡NUEVA!", (int) x, (int) (y - 8 + bob), 0xFFFFFFFF, false);
                }
            }
        }
        boolean all = allDone();
        if (revealClock > 1.0f) {
            String msg = all ? "Clic o Esc para cerrar · las cartas ya están en tu inventario" : "Haz clic en cada carta para voltearla · Espacio: revelar todas";
            int a = all ? (int) (170 + 85 * Mth.sin(time * 3f)) : 255;
            g.drawCenteredString(font, msg, (int) cx, 232, (Mth.clamp(a, 4, 255) << 24) | 0xE8DCC8);
        }
    }

    private void drawSparks(GuiGraphics g) {
        for (Spark s : sparks) {
            float f = s.life / s.max;
            int rgb = s.rainbow ? Mth.hsvToRgb((time * 0.9f + s.x * 0.01f) % 1f, 0.6f, 1f) : s.rgb;
            int a = (int) (Mth.clamp(f * 1.5f, 0f, 1f) * 255);
            int sz = f > 0.5f ? 2 : 1;
            g.fill((int) s.x, (int) s.y, (int) s.x + sz, (int) s.y + sz, (a << 24) | rgb);
        }
    }

    // ------------------------------------------------------------------ entrada

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        float vx = (float) ((mx - ox) / sc);
        float vy = (float) ((my - oy) / sc);
        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        }
        if (phase == Phase.TEAR && overPack(vx, vy)) {
            holding = true;
            return true;
        }
        if (phase == Phase.REVEAL && revealClock > 0.8f) {
            if (hover >= 0) {
                startFlip(hover);
                return true;
            }
            if (allDone()) {
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) {
            holding = false;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if ((key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) && phase == Phase.REVEAL) {
            if (allDone()) {
                onClose();
            } else {
                auto = true;
            }
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
