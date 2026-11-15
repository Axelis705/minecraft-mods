package com.casino.client;

import com.casino.data.Membership;
import com.casino.net.LevelChestPacket;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * El cofre de premio al subir de nivel de socio: cae, tiembla cada vez más, se abre con un destello y rayos de luz del color del
 * nivel, salta confeti y los premios salen volando uno por uno a su lugar. Clic para adelantar; cuando todo cayó, clic para cerrar.
 * (Lo que se ve ya está en el inventario: la pantalla es solo el festejo.)
 */
public final class LevelChestScreen extends CasinoScreen {
    private static final int DROP = 8;
    private static final int OPEN = 34;
    private static final int FIRST = 42;
    private static final int GAP = 5;
    private static final int FLIGHT = 14;
    private static final int PER_ROW = 8;
    private static final int SLOT = 26;

    private final LevelChestPacket d;
    private final RandomSource rnd = RandomSource.create();
    private final List<float[]> confetti = new ArrayList<>();
    private int age;
    private int landed;

    public LevelChestScreen(LevelChestPacket d) {
        super(Component.translatable("casino.chest.title"));
        this.d = d;
    }

    private int end() {
        return FIRST + Math.max(0, d.items.size() - 1) * GAP + FLIGHT;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void sound(SoundEvent s, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
    }

    @Override
    public void tick() {
        age++;
        if (age == DROP) {
            sound(SoundEvents.WOOD_PLACE, 0.8F, 0.8F);
        }
        if (age > DROP && age < OPEN && age % 4 == 0) {
            sound(SoundEvents.CHEST_LOCKED, 1.4F + (age - DROP) * 0.02F, 0.25F);
        }
        if (age == OPEN) {
            sound(SoundEvents.CHEST_OPEN, 1.0F, 1.0F);
            sound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.7F);
            burst(70);
        }
        int lands = 0;
        for (int i = 0; i < d.items.size(); i++) {
            if (age >= FIRST + i * GAP + FLIGHT) {
                lands++;
            }
        }
        while (landed < lands) {
            sound(SoundEvents.ITEM_PICKUP, 0.8F + landed * 0.07F, 0.6F);
            landed++;
        }
        if (age == end() + 2) {
            sound(SoundEvents.PLAYER_LEVELUP, 1.2F, 0.5F);
        }
        // confeti: x, y, vx, vy, color, vida
        for (int i = confetti.size() - 1; i >= 0; i--) {
            float[] c = confetti.get(i);
            c[0] += c[2];
            c[1] += c[3];
            c[2] *= 0.97F;
            c[3] = c[3] * 0.97F + 0.18F;
            c[5]--;
            if (c[5] <= 0) {
                confetti.remove(i);
            }
        }
    }

    private void burst(int n) {
        int col = Membership.COLOR[Math.min(4, Math.max(0, d.level))];
        int[] pal = {0xFF000000 | col, 0xFFFFE680, 0xFFFFFFFF, 0xFFFF7AB0, 0xFF7CE8FF};
        float cx = width / 2F, cy = chestY() - 12;
        for (int i = 0; i < n; i++) {
            double a = -Math.PI / 2 + (rnd.nextDouble() - 0.5) * 2.4;
            float sp = 3 + rnd.nextFloat() * 6;
            confetti.add(new float[]{cx + (rnd.nextFloat() - 0.5F) * 40, cy, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp,
                    Float.intBitsToFloat(pal[rnd.nextInt(pal.length)]), 40 + rnd.nextInt(40)});
        }
    }

    private int chestY() {
        return height / 2 - 4;
    }

    @Override
    protected boolean onClick(double x, double y, int b) {
        if (age < end()) {
            if (age < OPEN) {
                age = OPEN - 1;
            } else {
                age = end();
                landed = d.items.size();
            }
        } else {
            onClose();
        }
        return true;
    }

    private static float ease(float t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    private int slotX(int i) {
        int row = i / PER_ROW;
        int inRow = Math.min(PER_ROW, d.items.size() - row * PER_ROW);
        return width / 2 - inRow * SLOT / 2 + (i % PER_ROW) * SLOT + 5;
    }

    private int slotY(int i) {
        return chestY() + 46 + (i / PER_ROW) * SLOT;
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        float t = age + pt;
        int lvl = Math.min(4, Math.max(0, d.level));
        int col = Membership.COLOR[lvl];
        int cx = width / 2, cy = chestY();
        g.fillGradient(0, 0, width, height, 0x60000000, 0xA0000000);

        // ---- rayos de luz detrás del cofre
        if (t > OPEN) {
            float grow = ease((t - OPEN) / 14F);
            g.push();
            g.translate(cx, cy - 10, 0);
            g.rotate(t * 0.012F);
            for (int i = 0; i < 14; i++) {
                g.push();
                g.rotate((float) (Math.PI * 2 * i / 14));
                int len = (int) (150 * grow);
                int a = (int) (0x70 * grow);
                g.fillGradient(-5, 0, 5, len, a << 24 | col, 0x04000000 | col);
                g.pop();
            }
            g.pop();
            // resplandor
            for (int r = 5; r > 0; r--) {
                int s = (int) (r * 12 * grow);
                g.fill(cx - s, cy - 10 - s, cx + s, cy - 10 + s, (int) (0x16 * grow) << 24 | col);
            }
        }

        // ---- el cofre
        float drop = ease(t / DROP);
        int bounce = t < DROP + 6 && t > DROP ? (int) (-4 * Math.sin(Math.PI * (t - DROP) / 6)) : 0;
        int oy = (int) (-(1 - drop) * (height / 2F + 40)) + bounce;
        int shake = 0;
        if (t > DROP && t < OPEN) {
            float amp = 0.5F + 3F * (t - DROP) / (OPEN - DROP);
            shake = (int) Math.round(Math.sin(t * 2.4) * amp);
        }
        int bx = cx - 32 + shake, by = cy + oy;
        int wood = 0xFF7A4A22, dark = 0xFF4E2E12, light = 0xFF9A6434;
        int metal = 0xFF000000 | col, metalDark = 0xFF000000 | (col >> 1 & 0x7F7F7F);
        // cuerpo
        g.fill(bx - 1, by - 1, bx + 65, by + 31, 0xFF1A0E04);
        g.fill(bx, by, bx + 64, by + 30, wood);
        for (int i = 0; i < 3; i++) {
            g.fill(bx, by + 9 + i * 9, bx + 64, by + 10 + i * 9, dark);
        }
        g.fill(bx, by, bx + 64, by + 2, light);
        g.fill(bx, by, bx + 5, by + 30, metal);
        g.fill(bx + 59, by, bx + 64, by + 30, metal);
        g.fill(bx, by + 27, bx + 64, by + 30, metalDark);
        // tapa (se levanta e inclina al abrir)
        float open = t > OPEN ? ease((t - OPEN) / 8F) : 0;
        g.push();
        g.translate(bx, by, 0);
        g.translate(0, -16 * open - 2 * open, 0);
        g.rotate(-0.28F * open);
        g.fill(-1, -17, 65, 1, 0xFF1A0E04);
        g.fill(0, -16, 64, 0, wood);
        g.fill(0, -16, 64, -13, light);
        g.fill(0, -7, 64, -6, dark);
        g.fill(0, -16, 5, 0, metal);
        g.fill(59, -16, 64, 0, metal);
        g.fill(0, -2, 64, 0, metalDark);
        // cerradura
        if (open < 0.5F) {
            g.fill(28, -4, 36, 6, 0xFFFFD45A);
            g.fill(31, -1, 33, 3, 0xFF3A2808);
        }
        g.pop();
        // interior iluminado
        if (open > 0) {
            g.fill(bx + 5, by - 1, bx + 59, by + 3, (int) (0xC0 * open) << 24 | 0xFFF2B0);
        }

        // ---- confeti
        for (float[] c : confetti) {
            int a = (int) Math.min(255, c[5] * 8);
            int cc = Float.floatToRawIntBits(c[4]);
            g.fill((int) c[0], (int) c[1], (int) c[0] + 2, (int) c[1] + 3, a << 24 | (cc & 0xFFFFFF));
        }

        // ---- título
        if (t > OPEN) {
            float pop = ease((t - OPEN) / 6F);
            float sc = 2.6F - 0.6F * pop;
            Component title = Component.translatable("casino.chest.level", Component.translatable("casino.member.level_" + Membership.LEVELS[lvl]))
                    .withStyle(ChatFormatting.BOLD);
            g.push();
            g.translate(cx, cy - 74, 0);
            g.scale(sc, sc, 1);
            int tw = font.width(title);
            g.drawString(font, title, -tw / 2, -4, 0xFF000000 | col, true);
            g.pop();
            String stars = "★".repeat(lvl + 1);
            g.drawString(font, stars, cx - font.width(stars) / 2, cy - 56, 0xFFFFE680, true);
            Component sub = Component.translatable("casino.chest.sub");
            g.drawString(font, sub, cx - font.width(sub) / 2, cy - 44, 0xFFE0DCEC, true);
        }

        // ---- casillas y premios que salen volando
        List<Component> tip = null;
        for (int i = 0; i < d.items.size(); i++) {
            int sx = slotX(i), sy = slotY(i);
            float start = FIRST + i * GAP;
            if (t < start) {
                continue;
            }
            float f = Math.min(1, (t - start) / FLIGHT);
            boolean done = f >= 1;
            g.fill(sx - 4, sy - 4, sx + 20, sy + 20, (int) (0xB0 * Math.min(1, f * 2)) << 24 | 0x15121F);
            if (done) {
                g.fill(sx - 4, sy - 4, sx + 20, sy - 3, metalDark);
            }
            ItemStack st = d.items.get(i);
            float px = (cx - 8) + (sx - (cx - 8)) * ease(f);
            float py = (cy - 14) + (sy - (cy - 14)) * f - (float) Math.sin(Math.PI * f) * 46;
            float s = 1 + 0.6F * (float) Math.sin(Math.PI * f);
            g.push();
            g.translate(px + 8, py + 8, 0);
            g.scale(s, s, 1);
            g.renderItem(st, -8, -8);
            if (done) {
                g.renderItemDecorations(font, st, -8, -8);
            }
            g.pop();
            if (done && mx >= sx - 4 && mx < sx + 20 && my >= sy - 4 && my < sy + 20) {
                tip = new ArrayList<>();
                tip.add(st.getHoverName());
                if (st.getCount() > 1) {
                    tip.add(Component.literal("x" + st.getCount()).withStyle(ChatFormatting.GRAY));
                }
            }
        }

        if (t > end() + 4 && (age / 10) % 2 == 0) {
            Component c = Component.translatable("casino.chest.close");
            g.drawString(font, c, cx - font.width(c) / 2, height - 24, 0xFFC8C0DC, true);
        }
        if (tip != null) {
            g.renderTooltip(font, tip, mx, my);
        }
    }
}
