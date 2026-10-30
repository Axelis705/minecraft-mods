package com.kensei.client;

import com.kensei.KenseiConfig;
import com.kensei.combat.KatanaUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * El panel de combate, arriba a la izquierda (estilo Naruto Storm): cara del jugador, barra de vida con estela de daño, los dashes que
 * se recargan solos en reposo, la espera del crítico y el combo. Arriba al centro va el enemigo fijado. Solo se ve con una katana en la mano.
 */
public final class KatanaHud {
    private static final int X = 8;
    private static final int Y = 8;
    private static final int FACE = 32;
    private static final int BAR_X = X + FACE + 8;
    private static final int HP_W = 132;
    private static final int HP_H = 9;
    private static final int SKEW = 6;
    private static final int PIP_W = 34;
    private static final int PIP_H = 7;
    private static final int PIP_GAP = 4;

    private KatanaHud() {
    }

    /** Un rectángulo inclinado (paralelogramo): cada fila se corre un poco, de abajo hacia arriba. {@code fillW} = cuánto se rellena. */
    private static void slant(GuiGraphics g, int x, int y, int fillW, int h, int skew, int color) {
        if (fillW <= 0) {
            return;
        }
        for (int r = 0; r < h; r++) {
            int off = h <= 1 ? 0 : Math.round(skew * (h - 1 - r) / (float) (h - 1));
            g.fill(x + off, y + r, x + off + fillW, y + r + 1, color);
        }
    }

    private static int hpColor(float f) {
        int r;
        int gr;
        if (f > 0.5F) {
            float k = (f - 0.5F) / 0.5F;
            r = Math.round(Mth.lerp(k, 235, 70));
            gr = Math.round(Mth.lerp(k, 200, 215));
        } else {
            float k = f / 0.5F;
            r = Math.round(Mth.lerp(k, 225, 235));
            gr = Math.round(Mth.lerp(k, 50, 200));
        }
        return 0xFF000000 | (r << 16) | (gr << 8) | 0x3C;
    }

    /** ¿Se está dibujando tu panel (arriba a la izquierda) ahora mismo? Solo con una katana en la mano. */
    public static boolean drawsPanel(LocalPlayer p) {
        return KatanaUtil.holdsKatana(p);
    }

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui || !KatanaUtil.holdsKatana(p)) {
            return;
        }
        long ms = System.currentTimeMillis();
        long fl = ms - ClientState.counterFlashMillis;
        if (fl >= 0 && fl < 260) {
            int a = (int) (70 * (1.0F - fl / 260.0F));
            g.fill(0, 0, w, h, (a << 24) | 0xE8F4FF); // destello al hacer un parry o esquive perfecto
        }
        ScreenFx.render(g, w, h);
        ClientLock.renderMarker(g, w, h, partialTick);
        TeamMarks.render(g, w, h, partialTick);
        DamageNumbers.render(g, w, h);
        if (!ClientDuel.active()) {
            renderTarget(mc, g, w);
        }

        // ---- cara con marco
        g.fill(X - 2, Y - 2, X + FACE + 2, Y + FACE + 2, 0xFF0B0D12);
        g.fill(X - 1, Y - 1, X + FACE + 1, Y + FACE + 1, 0xFF2E7F9C);
        g.fill(X, Y, X + FACE, Y + FACE, 0xFF161A22);
        PlayerFaceRenderer.draw(g, p.getSkinTextureLocation(), X + 2, Y + 2, FACE - 4);

        // ---- nombre y cifra de vida
        float max = Math.max(1.0F, p.getMaxHealth());
        float hp = Math.max(0.0F, p.getHealth());
        g.drawString(mc.font, p.getName(), BAR_X, Y - 1, 0xFFFFFFFF, true);
        String num = (int) Math.ceil(hp) + "/" + (int) Math.ceil(max);
        g.drawString(mc.font, num, BAR_X + HP_W + SKEW - mc.font.width(num), Y - 1, 0xFFE8E8E8, true);

        // ---- barra de vida
        int hy = Y + 10;
        slant(g, BAR_X - 1, hy - 1, HP_W + 2, HP_H + 2, SKEW, 0xFF0B0D12);
        slant(g, BAR_X, hy, HP_W, HP_H, SKEW, 0xFF2A1518);
        float f = Mth.clamp(hp / max, 0.0F, 1.0F);
        float gf = Mth.clamp(ClientState.healthGhost / max, 0.0F, 1.0F);
        if (gf > f) {
            slant(g, BAR_X, hy, Math.round(HP_W * gf), HP_H, SKEW, 0xFFF2F2F2);
        }
        int col = hpColor(f);
        if (p.hasEffect(net.minecraft.world.effect.MobEffects.POISON)) {
            col = 0xFF8AA63A;
        } else if (p.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)) {
            col = 0xFF4A4A4A;
        }
        slant(g, BAR_X, hy, Math.round(HP_W * f), HP_H, SKEW, col);
        slant(g, BAR_X, hy, Math.round(HP_W * f), 2, SKEW, 0x55FFFFFF);
        float abs = p.getAbsorptionAmount();
        if (abs > 0.0F) {
            slant(g, BAR_X, hy + HP_H - 2, Math.round(HP_W * Mth.clamp(abs / max, 0.0F, 1.0F)), 2, SKEW, 0xFFFFD84A);
        }

        // ---- estamina (se gasta al atacar y bloquear)
        int sy = hy + HP_H + 3;
        if (KenseiConfig.STAMINA_ENABLED.get()) {
            staminaBar(g, mc, BAR_X, sy, HP_W - 8, ms);
        }
        // ---- dashes (cada uno vale 100 de energía y se recarga solo en reposo)
        int dashes = Math.max(1, Math.round(ClientState.max() / 100.0F));
        int py = hy + HP_H + (KenseiConfig.STAMINA_ENABLED.get() ? 12 : 5);
        boolean flash = ms - ClientState.lastGainMillis < 260;
        for (int i = 0; i < dashes; i++) {
            int x = BAR_X + i * (PIP_W + PIP_GAP);
            float pf = Mth.clamp((ClientState.shownEnergy - i * 100.0F) / 100.0F, 0.0F, 1.0F);
            slant(g, x - 1, py - 1, PIP_W + 2, PIP_H + 2, 4, 0xFF0B0D12);
            slant(g, x, py, PIP_W, PIP_H, 4, 0xFF1D2733);
            int fw = Math.round(PIP_W * pf);
            if (fw > 0) {
                int c;
                if (pf >= 0.999F) {
                    int pulse = (int) (34 * (0.5 + 0.5 * Math.sin(ms / 170.0)));
                    c = 0xFF000000 | ((0x2C + pulse) << 16) | ((0xC8 + pulse / 2) << 8) | 0xFF;
                    if (flash) {
                        c = 0xFFFFFFFF;
                    }
                } else {
                    c = 0xFF1B7FA6;
                }
                slant(g, x, py, fw, PIP_H, 4, c);
                slant(g, x, py, fw, 1, 4, 0x66FFFFFF);
            }
        }
        Component dk = KenseiKeys.DODGE.getTranslatedKeyMessage();
        g.drawString(mc.font, dk, BAR_X + dashes * (PIP_W + PIP_GAP) + 4, py, ClientState.shownEnergy >= 100.0F ? 0xFF6AD8FF : 0xFF6E7B86, true);

        // ---- espera del crítico
        int cy = py + PIP_H + 5;
        int cw = 56;
        float ready = ClientState.heavyCooldown <= 0 ? 1.0F
                : 1.0F - Math.min(1.0F, ClientState.heavyCooldown / (float) (KenseiConfig.HEAVY_COOLDOWN.get() + KenseiConfig.HEAVY_WINDUP.get()));
        slant(g, BAR_X - 1, cy - 1, cw + 2, 5, 3, 0xFF0B0D12);
        slant(g, BAR_X, cy, cw, 3, 3, 0xFF2B2B33);
        slant(g, BAR_X, cy, Math.round(cw * ready), 3, 3, ClientState.heavyCooldown <= 0 ? 0xFFE8B030 : 0xFF8A6A20);
        Component rk = KenseiKeys.HEAVY.getTranslatedKeyMessage();
        g.drawString(mc.font, rk, BAR_X + cw + 8, cy - 3, ClientState.heavyCooldown <= 0 ? 0xFFE8B030 : 0xFF8A8A8A, true);

        // ---- combo y guardia, al lado
        int tx = BAR_X + cw + 8 + mc.font.width(rk) + 10;
        if (ClientState.combo > 0) {
            g.drawString(mc.font, Component.translatable("kensei.hud.combo", ClientState.combo), tx, cy - 3, 0xFFFFFFFF, true);
        }
        if (KatanaUtil.guarding(p)) {
            Component txt = Component.translatable("kensei.hud.guard");
            g.drawString(mc.font, txt, BAR_X, cy + 8, 0xFF8FD8FF, true);
        }

        // ---- aviso de contraataque, bajo la mira
        if (ClientState.counterTicks > 0) {
            Component txt = Component.translatable("kensei.hud.counter");
            int tw = mc.font.width(txt);
            int by = h / 2 + 30;
            int c2 = ((ms / 120) % 2 == 0) ? 0xFFFFA928 : 0xFFFFE08A;
            g.drawString(mc.font, txt, w / 2 - tw / 2, by, c2, true);
            int bw = 60;
            float left = Math.min(1.0F, ClientState.counterTicks / 40.0F);
            g.fill(w / 2 - bw / 2, by + 10, w / 2 - bw / 2 + Math.round(bw * left), by + 12, 0xFFFFA928);
        }
    }

    /** La barra de estamina: verde llena, amarilla a la mitad y roja al final; parpadea si estás agotado o intentas atacar sin ella. */
    private static void staminaBar(GuiGraphics g, Minecraft mc, int x, int y, int w, long ms) {
        float f = Mth.clamp(ClientState.shownStamina / ClientState.staminaMax, 0.0F, 1.0F);
        boolean tired = ClientState.exhaustTicks > 0;
        boolean denied = ms - ClientState.noStaminaMillis < 300;
        slant(g, x - 1, y - 1, w + 2, 6, 3, denied ? 0xFFFF5A4A : 0xFF0B0D12);
        slant(g, x, y, w, 4, 3, 0xFF1B2218);
        int col;
        if (tired) {
            col = (ms / 110) % 2 == 0 ? 0xFFE03A30 : 0xFF7A2420;
        } else if (f > 0.5F) {
            col = 0xFF4CC36A;
        } else if (f > 0.25F) {
            col = 0xFFE0C040;
        } else {
            col = 0xFFE0703A;
        }
        slant(g, x, y, Math.round(w * f), 4, 3, col);
        slant(g, x, y, Math.round(w * f), 1, 3, 0x55FFFFFF);
        if (tired) {
            g.drawString(mc.font, Component.translatable("kensei.hud.exhausted"), x + w + 8, y - 2, 0xFFFF6A5A, true);
        }
    }

    /** Nombre y vida del enemigo fijado (arriba al centro). */
    private static void renderTarget(Minecraft mc, GuiGraphics g, int w) {
        net.minecraft.world.entity.LivingEntity t = ClientLock.target();
        if (t == null) {
            return;
        }
        int bw = 130;
        int x = w / 2 - bw / 2;
        int y = 24;
        float f = Math.max(0.0F, Math.min(1.0F, t.getHealth() / Math.max(1.0F, t.getMaxHealth())));
        Component name = t.getDisplayName();
        g.drawString(mc.font, name, w / 2 - mc.font.width(name) / 2, y - 12, 0xFFFFFFFF, true);
        slant(g, x - 1, y - 1, bw + 2, 8, SKEW - 2, 0xFF101010);
        slant(g, x, y, bw, 6, SKEW - 2, 0xFF3A1A1A);
        slant(g, x, y, Math.round(bw * f), 6, SKEW - 2, 0xFFC83030);
        slant(g, x, y, Math.round(bw * f), 1, SKEW - 2, 0x55FFFFFF);
        String hp = (int) Math.ceil(t.getHealth()) + "/" + (int) Math.ceil(t.getMaxHealth());
        g.drawString(mc.font, hp, w / 2 - mc.font.width(hp) / 2, y + 9, 0xFFDDDDDD, true);
    }
}
