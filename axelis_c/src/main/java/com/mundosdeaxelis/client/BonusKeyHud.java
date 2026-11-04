package com.mundosdeaxelis.client;

import com.mojang.math.Axis;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Aviso de "llave bonus": un cartel baja desde arriba con rebote, con rayos de luz girando detrás de una llave grande que flota,
 * destellos que orbitan, un brillo que cruza el cartel y el título que late. Los colores son los de la caja siguiente.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BonusKeyHud {
    private BonusKeyHud() {
    }

    private static final int TOTAL = 120;
    private static final int IN = 16;
    private static final int OUT = 20;

    private static long ticks;
    private static long start = -1000;
    private static int kindIdx = -1;

    public static void show(int kind) {
        if (kind < 0 || kind >= CrateKind.values().length) {
            return;
        }
        kindIdx = kind;
        start = ticks;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && !Minecraft.getInstance().isPaused()) {
            ticks++;
        }
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158F, c3 = c1 + 1.0F;
        float p = x - 1.0F;
        return 1.0F + c3 * p * p * p + c1 * p * p;
    }

    private static int argb(float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
        return a << 24 | (rgb & 0xFFFFFF);
    }

    private static int mix(int rgb, int to, float t) {
        int r = (int) (((rgb >> 16) & 255) * (1 - t) + ((to >> 16) & 255) * t);
        int gr = (int) (((rgb >> 8) & 255) * (1 - t) + ((to >> 8) & 255) * t);
        int b = (int) ((rgb & 255) * (1 - t) + (to & 255) * t);
        return r << 16 | gr << 8 | b;
    }

    public static void render(GuiGraphics g, float pt, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (kindIdx < 0 || mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        float t = (ticks - start) + pt;
        if (t < 0 || t > TOTAL) {
            return;
        }
        CrateKind k = CrateKind.values()[kindIdx];
        int col = k.color;
        Font f = mc.font;

        // entrada con rebote, salida subiendo y desvaneciéndose
        float inP = Math.min(1.0F, t / IN);
        float slide = easeOutBack(inP);
        float outP = t > TOTAL - OUT ? (t - (TOTAL - OUT)) / OUT : 0.0F;
        float alpha = (1.0F - outP) * Math.min(1.0F, t / 6.0F);
        if (alpha <= 0.01F) {
            return;
        }
        int w = 176, h = 50;
        int x = (width - w) / 2;
        int y = (int) (-h + (46 + h) * slide - outP * 30);

        // resplandor exterior que late
        float glow = 0.5F + 0.5F * (float) Math.sin(t * 0.25F);
        for (int i = 4; i >= 1; i--) {
            g.fill(x - i * 2, y - i * 2, x + w + i * 2, y + h + i * 2, argb(alpha * (0.05F + 0.03F * glow) * (5 - i) / 4.0F, col));
        }
        // panel
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, argb(alpha * 0.95F, col));
        g.fill(x, y, x + w, y + h, argb(alpha * 0.92F, 0x120A1E));
        g.fill(x + 2, y + 2, x + w - 2, y + 4, argb(alpha * 0.55F, mix(col, 0xFFFFFF, 0.3F)));
        g.fill(x + 2, y + h - 4, x + w - 2, y + h - 2, argb(alpha * 0.25F, col));
        // esquinas doradas
        int gold = 0xFFD870;
        for (int[] c : new int[][]{{x - 2, y - 2}, {x + w - 3, y - 2}, {x - 2, y + h - 3}, {x + w - 3, y + h - 3}}) {
            g.fill(c[0], c[1], c[0] + 5, c[1] + 5, argb(alpha, gold));
            g.fill(c[0] + 1, c[1] + 1, c[0] + 4, c[1] + 4, argb(alpha, 0xFFF6C8));
        }

        // contenido recortado al cartel (rayos + brillo)
        g.enableScissor(x, y, x + w, y + h);
        float kx = x + 30, ky = y + h / 2.0F;
        g.pose().pushPose();
        g.pose().translate(kx, ky, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(t * 3.2F));
        for (int i = 0; i < 10; i++) {
            g.pose().pushPose();
            g.pose().mulPose(Axis.ZP.rotationDegrees(i * 36.0F));
            float len = 34 + 6 * (float) Math.sin(t * 0.3F + i);
            g.fill(-2, 0, 2, (int) len, argb(alpha * 0.16F, mix(col, 0xFFFFFF, 0.4F)));
            g.fill(-1, 0, 1, (int) (len * 0.7F), argb(alpha * 0.22F, 0xFFFFFF));
            g.pose().popPose();
        }
        g.pose().popPose();
        // brillo que cruza el cartel
        float sw = (t - IN) / 26.0F;
        if (sw >= 0.0F && sw <= 1.0F) {
            float sx = x - 30 + (w + 60) * sw;
            g.pose().pushPose();
            g.pose().translate(sx, y + h / 2.0F, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(20.0F));
            g.fill(-6, -h, 6, h, argb(alpha * 0.20F, 0xFFFFFF));
            g.fill(-2, -h, 2, h, argb(alpha * 0.30F, 0xFFFFFF));
            g.pose().popPose();
        }
        g.disableScissor();

        // llave grande que flota y se balancea
        float bob = (float) Math.sin(t * 0.18F) * 2.5F;
        float pop = 1.0F + 0.35F * (float) Math.max(0.0, Math.sin(Math.min(1.0F, t / 14.0F) * Math.PI)) + 0.04F * glow;
        float sc = 2.4F * pop;
        ItemStack key = new ItemStack(ModItems.KEYS.get(k).get());
        g.pose().pushPose();
        g.pose().translate(kx, ky + bob, 150);
        g.pose().mulPose(Axis.ZP.rotationDegrees((float) Math.sin(t * 0.12F) * 9.0F));
        g.pose().scale(sc, sc, 1.0F);
        g.renderItem(key, -8, -8);
        g.pose().popPose();

        // destellos que orbitan la llave
        for (int i = 0; i < 6; i++) {
            float a = t * 0.11F + i * 1.0471976F;
            float r = 22 + 4 * (float) Math.sin(t * 0.2F + i * 2);
            float px = kx + (float) Math.cos(a) * r, py = ky + (float) Math.sin(a) * r * 0.75F;
            float tw = 0.5F + 0.5F * (float) Math.sin(t * 0.4F + i * 1.7F);
            int s = tw > 0.7F ? 2 : 1;
            int c = argb(alpha * (0.5F + 0.5F * tw), i % 2 == 0 ? 0xFFFFFF : mix(col, 0xFFFFFF, 0.5F));
            g.fill((int) px - s, (int) py, (int) px + s + 1, (int) py + 1, c);
            g.fill((int) px, (int) py - s, (int) px + 1, (int) py + s + 1, c);
        }

        // textos
        int tx = x + 62;
        float pulse = 1.0F + 0.06F * (float) Math.sin(t * 0.35F);
        Component title = Component.translatable("hud.mundos_de_axelis.bonus_key.title");
        g.pose().pushPose();
        g.pose().translate(tx, y + 8, 0);
        g.pose().scale(1.25F * pulse, 1.25F * pulse, 1.0F);
        g.drawString(f, title, 0, 0, argb(alpha, mix(0xFFD870, 0xFFFFFF, glow * 0.5F)), true);
        g.pose().popPose();
        Component name = Component.translatable(k.titleKey());
        Component sub = Component.translatable("hud.mundos_de_axelis.bonus_key.sub", name);
        g.drawString(f, sub, tx, y + 24, argb(alpha, mix(col, 0xFFFFFF, 0.25F)), true);
        g.drawString(f, Component.translatable("hud.mundos_de_axelis.bonus_key.hint"), tx, y + 36,
                argb(alpha * 0.8F, 0xC9BFD8), false);
    }
}
