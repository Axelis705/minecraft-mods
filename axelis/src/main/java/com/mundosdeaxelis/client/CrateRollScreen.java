package com.mundosdeaxelis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.crate.CratePools;
import com.mundosdeaxelis.crate.CrateRoll;
import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.network.CrateOpenPacket;
import com.mundosdeaxelis.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La ruleta a pantalla completa (estilo caja de CS:GO): una tira de objetos que pasa bajo una flecha, frena poco a poco y
 * revela el premio con una tarjeta, luces y partículas según su rareza. Es una función pura del tiempo (CrateRoll), igual
 * que el holograma que ven los demás jugadores.
 */
public class CrateRollScreen extends Screen {
    private static final class Spark {
        float x, y, vx, vy, life, max, size, rot, vr;
        int color;
    }

    public final CrateOpenPacket data;
    private final CrateKind kind;
    private final Item[] strip;
    private final Item prize;
    private final Tier prizeTier;
    private final List<Spark> sparks = new ArrayList<>();
    private final Random rnd = new Random();
    private int ticks;
    private int lastSlot = -1;
    private boolean revealed;
    private float revealAt = -1;
    private int curColor = 0xFFFFFF;
    private float shake;

    public CrateRollScreen(CrateOpenPacket data) {
        super(Component.translatable(CrateKind.byIndex(data.kind).titleKey()));
        this.data = data;
        this.kind = CrateKind.byIndex(data.kind);
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(data.winner));
        this.prize = it == null ? Items.AIR : it;
        this.prizeTier = CratePools.tierOf(kind, prize);
        this.strip = CratePools.strip(kind, data.seed, CrateRoll.LEN, CrateRoll.WIN, prize);
    }

    @Override
    protected void init() {
        super.init();
        if (ticks == 0) {
            play(ModSounds.CRATE_SPIN.get(), 1.0F, 0.9F);
        }
    }

    private void play(net.minecraft.sounds.SoundEvent ev, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, vol));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private float cellSize() {
        return Mth.clamp(this.height / 5.0F, 44.0F, 78.0F);
    }

    @Override
    public void tick() {
        ticks++;
        float cs = cellSize();
        float step = cs + 6;
        float cx = this.width / 2.0F, cy = this.height * 0.44F;
        float time = ticks;
        double pos = CrateRoll.pos(Math.min(time, CrateRoll.ROLL), data.seed);
        int slot = (int) Math.round(pos);
        slot = Mth.clamp(slot, 0, strip.length - 1);
        if (time <= CrateRoll.ROLL) {
            if (slot != lastSlot) {
                lastSlot = slot;
                float pr = time / (float) CrateRoll.ROLL;
                play(ModSounds.CRATE_TICK.get(), 0.85F + 0.35F * pr + rnd.nextFloat() * 0.05F, 0.55F);
            }
            curColor = CratePools.tierOf(kind, strip[slot]).color;
            if (ticks % 2 == 0) {
                spawn(cx + (rnd.nextFloat() - 0.5F) * cs * 1.4F, cy + cs * 0.5F, 1, curColor, 0.6F);
            }
        } else {
            curColor = prizeTier.color;
            if (!revealed) {
                revealed = true;
                revealAt = ticks;
                shake = 3 + prizeTier.ordinal() * 3;
                int n = 18 + prizeTier.ordinal() * 16;
                spawn(cx, cy, n, prizeTier.color, 2.2F + prizeTier.ordinal() * 0.5F);
                if (prizeTier.ordinal() >= Tier.EPIC.ordinal()) {
                    spawn(cx, cy, n, 0xFFFFFF, 3.2F);
                }
            } else if (ticks % 3 == 0 && prizeTier.ordinal() >= Tier.RARE.ordinal()) {
                spawn(cx + (rnd.nextFloat() - 0.5F) * 220, cy - 80, 1, prizeTier.color, 0.9F);
            }
        }
        if (shake > 0) {
            shake *= 0.82F;
        }
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            s.x += s.vx;
            s.y += s.vy;
            s.vy += 0.05F;
            s.vx *= 0.97F;
            s.rot += s.vr;
            if (++s.life >= s.max) {
                sparks.remove(i);
            }
        }
        if (ticks >= CrateRoll.TOTAL + 16) {
            onClose();
        }
    }

    private void spawn(float x, float y, int n, int color, float speed) {
        for (int i = 0; i < n; i++) {
            Spark s = new Spark();
            double a = rnd.nextDouble() * Math.PI * 2;
            float sp = (0.3F + rnd.nextFloat()) * speed;
            s.x = x;
            s.y = y;
            s.vx = (float) Math.cos(a) * sp;
            s.vy = (float) Math.sin(a) * sp - (n == 1 ? 1.4F : 0.6F);
            s.max = 24 + rnd.nextInt(26);
            s.size = 6 + rnd.nextFloat() * 10;
            s.rot = rnd.nextFloat() * 360;
            s.vr = (rnd.nextFloat() - 0.5F) * 12;
            s.color = rnd.nextInt(4) == 0 ? 0xFFFFFF : color;
            sparks.add(s);
        }
    }

    // ------------------------------------------------------------------ dibujo

    private void cell(GuiGraphics g, int[] c, float x, float y, float w, float h, int rgb, float a) {
        g.setColor(CrateUi.r(rgb), CrateUi.g(rgb), CrateUi.b(rgb), a);
        g.blit(CrateUi.ATLAS, Math.round(x), Math.round(y), Math.round(w), Math.round(h), c[0], c[1], 32, 32,
                CrateUi.ATLAS_W, CrateUi.ATLAS_H);
        g.setColor(1, 1, 1, 1);
    }

    /** Rectángulo oscuro con esquinas suaves, para que los textos se lean sobre cualquier fondo. */
    private static void pill(GuiGraphics g, int x, int y, int w, int h, float alpha) {
        int a = ((int) Mth.clamp(alpha, 0, 255)) << 24;
        g.fill(x + 2, y, x + w - 2, y + h, a);
        g.fill(x, y + 2, x + w, y + h - 2, a);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, a);
    }

    private void additive() {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    }

    private void normalBlend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private void tex(GuiGraphics g, ResourceLocation t, float cx, float cy, float size, float rot, int rgb, float a) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(rot));
        g.setColor(CrateUi.r(rgb), CrateUi.g(rgb), CrateUi.b(rgb), a);
        int s = Math.round(size);
        g.blit(t, -s / 2, -s / 2, 0, 0, s, s, s, s);
        g.setColor(1, 1, 1, 1);
        g.pose().popPose();
    }

    private void item(GuiGraphics g, Item it, float cx, float cy, float px, float wobble) {
        ItemStack st = new ItemStack(it);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        if (wobble != 0) {
            g.pose().mulPose(Axis.ZP.rotationDegrees(wobble));
        }
        float s = px / 16.0F;
        g.pose().scale(s, s, 1.0F);
        g.renderItem(st, -8, -8);
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float time = ticks + pt;
        float cs = cellSize();
        float step = cs + 6;
        float cx = this.width / 2.0F, cy = this.height * 0.44F;
        float appear = Mth.clamp(time / 8.0F, 0, 1);
        double pos = CrateRoll.pos(Math.min(time, CrateRoll.ROLL), data.seed);
        float rev = time > CrateRoll.ROLL ? time - CrateRoll.ROLL : 0;
        float revU = Mth.clamp(rev / CrateRoll.REVEAL, 0, 1);

        g.pose().pushPose();
        if (shake > 0.05F) {
            g.pose().translate((rnd.nextFloat() - 0.5F) * shake, (rnd.nextFloat() - 0.5F) * shake, 0);
        }

        // fondo: casi transparente (un velo oscuro grande se veía gris sucio sobre el cielo de día).
        // El contraste lo dan la franja de la ruleta y unas "píldoras" suaves detrás de los textos.
        int bgTop = (int) (0x1C * appear) << 24 | 0x04040C;
        int bgBot = (int) (0x48 * appear) << 24 | 0x0C0614;
        g.fillGradient(0, 0, this.width, this.height, bgTop, bgBot);
        // viñeta lateral suave (degradado horizontal hecho con tiras, porque fillGradient solo es vertical)
        int vw = Math.max(24, this.width / 8);
        int strips = 12;
        for (int i = 0; i < strips; i++) {
            float t = 1.0F - i / (float) strips;
            int a = (int) (0x26 * appear * t * t) << 24;
            int x0 = i * vw / strips, x1 = (i + 1) * vw / strips;
            g.fill(x0, 0, x1, this.height, a);
            g.fill(this.width - x1, 0, this.width - x0, this.height, a);
        }
        normalBlend();

        // rayos giratorios detrás de todo, del color de lo que cae bajo la flecha
        float rays = (0.35F + 0.65F * Mth.clamp(rev / 14.0F, 0, 1)) * appear;
        float rsize = Math.max(this.width, this.height) * (0.9F + 0.5F * revU);
        additive();
        tex(g, CrateUi.BURST, cx, cy, rsize, time * 0.8F, curColor, 0.60F * rays);
        tex(g, CrateUi.BURST, cx, cy, rsize * 0.7F, -time * 1.3F, 0xFFFFFF, 0.22F * rays);
        normalBlend();

        // título
        pill(g, (int) cx - 90, (int) (this.height * 0.10F) - 6, 180, 40, 0x78 * appear);
        g.pose().pushPose();
        g.pose().translate(cx, this.height * 0.10F, 0);
        g.pose().scale(2.0F, 2.0F, 1.0F);
        g.drawCenteredString(this.font, Component.translatable(kind.titleKey()), 0, 0, kind.color);
        g.pose().popPose();
        Component sub = revealed
                ? Component.translatable("crate.mundos_de_axelis.prize")
                : Component.translatable("crate.mundos_de_axelis.opening");
        g.drawCenteredString(this.font, sub, (int) cx, (int) (this.height * 0.10F + 22), 0xCCCCCC);

        // franja de la ruleta
        float bandA = appear * (1.0F - 0.55F * Mth.clamp(rev / 10.0F, 0, 1));
        float top = cy - cs / 2 - 14, bot = cy + cs / 2 + 14;
        int bandCol = CrateUi.lerp(0x05050C, curColor, 0.16F);
        g.fillGradient(0, (int) top, this.width, (int) bot, ((int) (0x9A * bandA) << 24) | bandCol, ((int) (0x70 * bandA) << 24) | bandCol);
        int lineA = (int) (200 * bandA);
        g.fill(0, (int) top - 1, this.width, (int) top, (lineA << 24) | (curColor & 0xFFFFFF));
        g.fill(0, (int) bot, this.width, (int) bot + 1, (lineA << 24) | (curColor & 0xFFFFFF));
        additive();
        cell(g, CrateUi.BAR, 0, top - 14, this.width, 28, curColor, 0.55F * bandA);
        cell(g, CrateUi.BAR, 0, bot - 14, this.width, 28, curColor, 0.55F * bandA);
        // resplandor central: la ruleta "enciende" lo que pasa bajo la flecha
        cell(g, CrateUi.GLOW, cx - this.width * 0.34F, top - cs * 0.35F, this.width * 0.68F, (bot - top) + cs * 0.7F, curColor, 0.42F * bandA);
        cell(g, CrateUi.GLOW, cx - cs * 1.6F, top - cs * 0.2F, cs * 3.2F, (bot - top) + cs * 0.4F, 0xFFFFFF, 0.22F * bandA);
        normalBlend();

        // casillas
        float half = this.width / 2.0F + cs;
        int first = Math.max(0, (int) Math.floor(pos - half / step) - 1);
        int last = Math.min(strip.length - 1, (int) Math.ceil(pos + half / step) + 1);
        List<Runnable> itemPass = new ArrayList<>();
        float stripFade = 1.0F - Mth.clamp((rev - 2) / 8.0F, 0, 1);
        for (int i = first; i <= last; i++) {
            float d = (float) (i - pos);
            float x = cx + d * step;
            float edge = Mth.clamp((this.width / 2.0F - Math.abs(x - cx)) / (cs * 2.0F), 0, 1);
            float close = 1.0F - Math.min(1.0F, Math.abs(d));
            boolean isWinner = i == CrateRoll.WIN && revealed;
            float fade = appear * edge * (isWinner ? 1.0F : stripFade);
            if (fade <= 0.01F) {
                continue;
            }
            Tier t = CratePools.tierOf(kind, strip[i]);
            float sc = 1.0F + 0.14F * close * close;
            float sz = cs * sc;
            int col = t.color;
            // luz propia del color de la rareza detrás de cada casilla (más fuerte en las raras y cerca de la flecha)
            additive();
            float gl = (0.20F + 0.07F * t.ordinal() + 0.38F * close) * fade;
            cell(g, CrateUi.GLOW, x - sz * 0.85F, cy - sz * 0.85F, sz * 1.7F, sz * 1.7F, col, gl);
            normalBlend();
            // fondo de gema: color de la rareza oscurecido, y encima el cristal claro
            cell(g, CrateUi.PANEL, x - sz / 2, cy - sz / 2, sz, sz, CrateUi.lerp(0x0A0812, col, 0.46F + 0.10F * close), 0.92F * fade);
            cell(g, CrateUi.SLOT, x - sz / 2, cy - sz / 2, sz, sz, CrateUi.lerp(0xEEF2FF, col, 0.55F * (1.0F - 0.4F * close)), fade);
            final Item it = strip[i];
            final float fx = x, fs = cs * 0.66F * sc;
            final float fadeF = fade;
            itemPass.add(() -> {
                if (fadeF > 0.15F) {
                    item(g, it, fx, cy, fs, 0);
                }
            });
        }
        // la flecha y la línea central
        float pa = appear * (1 - 0.5F * Mth.clamp(rev / 10.0F, 0, 1));
        g.fill((int) cx - 1, (int) top, (int) cx + 1, (int) bot, ((int) (170 * pa) << 24) | 0xFFE680);
        float bob = Mth.sin(time * 0.4F) * 1.5F;
        cell(g, CrateUi.POINTER, cx - 12, top - 22 + bob, 24, 24, 0xFFD966, pa);
        g.pose().pushPose();
        g.pose().translate(0, bot + 22 - bob, 0);
        g.pose().translate(cx, 0, 0);
        g.pose().scale(1, -1, 1);
        cell(g, CrateUi.POINTER, -12, -12, 24, 24, 0xFFD966, pa);
        g.pose().popPose();

        for (Runnable r : itemPass) {
            r.run();
        }

        // leyenda de probabilidades abajo
        float la = appear * (1.0F - Mth.clamp(rev / 8.0F, 0, 1));
        if (la > 0.02F) {
            java.util.List<Tier> shown = new ArrayList<>();
            for (Tier t : Tier.values()) {
                if (kind.weight(t) > 0) {
                    shown.add(t);
                }
            }
            int n = shown.size();
            int cw = 78;
            int x0 = (int) (cx - n * cw / 2.0F);
            int ly = (int) (this.height * 0.80F);
            pill(g, x0 - 8, ly - 6, n * cw + 16, 28, 0x78 * la);
            for (int i = 0; i < n; i++) {
                Tier t = shown.get(i);
                int x = x0 + i * cw;
                int a = (int) (255 * la);
                g.fill(x + 2, ly, x + 10, ly + 8, (a << 24) | t.color);
                g.drawString(this.font, Component.translatable("tier.mundos_de_axelis." + t.id), x + 14, ly, (a << 24) | 0xCCCCCC, false);
                g.drawString(this.font, Component.literal(String.format("%.1f%%", kind.weight(t) / 10.0)), x + 14, ly + 10, (a << 24) | (t.color & 0xFFFFFF), false);
            }
        }

        // tarjeta del premio
        if (rev > 0) {
            float u = Mth.clamp(rev / 14.0F, 0, 1);
            float back = 1.0F + 2.70158F * (float) Math.pow(u - 1, 3) + 1.70158F * (float) Math.pow(u - 1, 2);
            float cardW = Math.min(this.width * 0.8F, 230), cardH = 172;
            float k = Mth.clamp(back, 0, 1.25F);
            float cyC = cy + 6;
            g.pose().pushPose();
            g.pose().translate(cx, cyC, 0);
            g.pose().scale(k, k, 1);
            // halo detrás
            additive();
            tex(g, CrateUi.HALO, 0, -10, cardH * 1.9F, time * 1.5F, prizeTier.color, 0.5F * u);
            normalBlend();
            cell(g, CrateUi.PANEL, -cardW / 2, -cardH / 2, cardW, cardH, CrateUi.lerp(prizeTier.color, 0x0C0C18, 0.55F), 0.97F * u);
            g.pose().popPose();
            // objeto grande girando suavemente (balanceo)
            float bigPx = 64 + 8 * Mth.sin(time * 0.15F);
            g.pose().pushPose();
            g.pose().translate(0, 0, 100);
            item(g, prize, cx, cyC - 26 * k, bigPx * k, Mth.sin(time * 0.12F) * 7);
            g.pose().popPose();
            // textos (por encima de los objetos)
            g.pose().pushPose();
            g.pose().translate(0, 0, 320);
            float ta = Mth.clamp((rev - 6) / 8.0F, 0, 1);
            if (ta > 0.02F) {
                int a = ((int) (255 * ta)) << 24;
                g.pose().pushPose();
                g.pose().translate(cx, cyC + 26, 0);
                g.pose().scale(2.0F, 2.0F, 1.0F);
                g.drawCenteredString(this.font, prizeTier.label(), 0, 0, a | 0xFFFFFF);
                g.pose().popPose();
                Component nm = prizeTier.paint(CratePools.label(CratePools.stack(kind, prize, data.seed)));
                g.drawCenteredString(this.font, nm, (int) cx, (int) (cyC + 50), a | 0xFFFFFF);
                if (rev > CrateRoll.REVEAL * 0.6F) {
                    g.drawCenteredString(this.font, Component.translatable("crate.mundos_de_axelis.added"),
                            (int) cx, (int) (cyC + 66), a | 0xBBBBBB);
                }
            }
            g.pose().popPose();
        }

        // chispas y estrellas
        additive();
        g.pose().pushPose();
        g.pose().translate(0, 0, 340);
        for (Spark s : sparks) {
            float lf = 1.0F - s.life / s.max;
            g.pose().pushPose();
            g.pose().translate(s.x, s.y, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(s.rot));
            float sz = s.size * (0.4F + 0.6F * lf);
            g.setColor(CrateUi.r(s.color), CrateUi.g(s.color), CrateUi.b(s.color), Math.min(1.0F, lf * 1.6F));
            g.blit(CrateUi.ATLAS, Math.round(-sz / 2), Math.round(-sz / 2), Math.round(sz), Math.round(sz),
                    CrateUi.STAR[0], CrateUi.STAR[1], 32, 32, CrateUi.ATLAS_W, CrateUi.ATLAS_H);
            g.setColor(1, 1, 1, 1);
            g.pose().popPose();
        }
        g.pose().popPose();
        normalBlend();

        // destello blanco al revelar
        if (rev > 0 && rev < 10) {
            float fa = (1 - rev / 10.0F);
            int col = CrateUi.lerp(0xFFFFFF, prizeTier.color, 0.4F);
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            g.fill(0, 0, this.width, this.height, ((int) (150 * fa * (0.4F + 0.12F * prizeTier.ordinal())) << 24) | col);
            g.pose().popPose();
        }

        Component escText = Component.translatable("crate.mundos_de_axelis.esc");
        int ew = this.font.width(escText);
        pill(g, (int) cx - ew / 2 - 6, this.height - 54, ew + 12, 14, 0x70 * appear);
        g.drawCenteredString(this.font, escText, (int) cx, this.height - 51, 0xCCFFFFFF);
        CrateUi.watermark(g, this.font, this.width - 6, this.height - 4, 0.40F * appear);
        g.pose().popPose();
    }
}
