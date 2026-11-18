package com.mundosdeaxelis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mundosdeaxelis.network.BossResultPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;

/**
 * Resultado de vencer a un jefe en una sola pantalla (reemplaza a los mensajes de chat): tu parte del daño y la probabilidad de
 * extras, lo que recibiste (aparece de a poco, con un sonido por cada objeto), si el mundo subió de nivel y si se apuntó un avance
 * nuevo en tu Archivo. Se cierra con «Continuar» o Esc; no pausa el juego.
 */
public class BossResultScreen extends Screen {
    private final BossResultPacket data;
    private final BossLore.Lore lore;
    private long openedAt;
    private int revealed;
    private int left, top, pw, ph;

    public BossResultScreen(BossResultPacket data) {
        super(Component.literal("Resultado"));
        this.data = data;
        this.lore = BossLore.of(data.stage);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int rows() {
        return Math.max(1, data.rewards.size());
    }

    /** Alto de la sección «No te tocaron» (0 si no hay nada que mostrar). */
    private int missHeight() {
        return data.eligible && !data.missed.isEmpty() ? 16 + data.missed.size() * 14 : 0;
    }

    @Override
    protected void init() {
        UiFx.open();
        openedAt = net.minecraft.Util.getMillis();
        revealed = 0;
        pw = Math.min(this.width - 12, 300);
        int extra = (data.tierLevel > 0 ? 24 : 0) + (data.archiveNew ? 14 : 0);
        ph = Math.min(this.height - 8, 88 + rows() * 18 + missHeight() + extra + 34);
        left = (this.width - pw) / 2;
        top = (this.height - ph) / 2;
    }

    private static Item itemOf(String id) {
        if (id == null || id.isEmpty()) {
            return Items.AIR;
        }
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return it == null ? Items.AIR : it;
    }

    private boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int bw = 110;
        if (button == 0 && over(mx, my, left + pw / 2 - bw / 2, top + ph - 28, bw, 20)) {
            UiFx.click();
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.fillGradient(0, 0, this.width, this.height, 0x70000000, 0x90000000);
        int acc = lore.accent();
        PanelUi.frame(g, left, top, pw, ph, acc);
        int cx = left + pw / 2;
        Component head = Component.literal("¡" + data.boss + " derrotado!").withStyle(ChatFormatting.BOLD);
        g.drawCenteredString(this.font, head, cx, top + 10, 0xFF000000 | acc);
        PanelUi.divider(g, left + 20, top + 24, pw - 40, acc);

        // tu parte del daño
        int y = top + 32;
        g.drawCenteredString(this.font, Component.literal("Tu parte del daño: " + data.pct + " %").withStyle(ChatFormatting.AQUA), cx, y, 0xFFFFFF);
        int bx = left + 30, bw = pw - 60;
        g.fill(bx, y + 12, bx + bw, y + 18, 0xFF000000);
        g.fill(bx + 1, y + 13, bx + 1 + (int) ((bw - 2) * Mth.clamp(data.pct / 100.0F, 0.0F, 1.0F)), y + 17, 0xFF000000 | acc);
        y += 24;
        if (!data.eligible) {
            for (FormattedCharSequence l : this.font.split(Component.literal("Hiciste muy poco daño al jefe: esta vez no recibes recompensa. Pelea más la próxima vez.")
                    .withStyle(ChatFormatting.YELLOW), pw - 40)) {
                g.drawCenteredString(this.font, l, cx, y, 0xFFFFFF);
                y += 10;
            }
        } else {
            g.drawCenteredString(this.font, Component.literal("Probabilidad de extras: " + data.chance + " %").withStyle(ChatFormatting.GRAY), cx, y, 0xFFFFFF);
            y += 14;
            // objetos recibidos: aparecen uno a uno
            int due = (int) ((net.minecraft.Util.getMillis() - openedAt - 350L) / 280L) + 1;
            due = Mth.clamp(due, 0, data.rewards.size());
            if (due > revealed) {
                revealed = due;
                UiFx.success();
            }
            ItemStack hovered = ItemStack.EMPTY;
            for (int i = 0; i < data.rewards.size(); i++) {
                int ry = y + i * 18;
                BossResultPacket.Got r = data.rewards.get(i);
                if (i >= revealed) {
                    PanelUi.slot(g, left + 24, ry, acc, true);
                    continue;
                }
                long age = net.minecraft.Util.getMillis() - openedAt - 350L - i * 280L;
                float pop = Mth.clamp(age / 200.0F, 0.0F, 1.0F);
                PanelUi.slot(g, left + 24, ry, r.sure() ? 0x7BD66B : 0xFFD35C, false);
                Item it = itemOf(r.item());
                Component name;
                if (it == Items.AIR) {
                    g.drawString(this.font, "◆", left + 29, ry + 4, 0xFF000000 | 0xFFD35C, false);
                    name = Component.literal("Esencia  +" + r.count()).withStyle(ChatFormatting.GOLD);
                } else {
                    ItemStack st = new ItemStack(it, Math.max(1, r.count()));
                    g.renderItem(st, left + 24, ry);
                    g.renderItemDecorations(this.font, st, left + 24, ry);
                    name = st.getHoverName().copy().withStyle(ChatFormatting.WHITE);
                    if (over(mx, my, left + 24, ry, 16, 16)) {
                        hovered = st;
                    }
                }
                MutableComponent line = name.copy();
                g.drawString(this.font, line, left + 48, ry + 4, 0xFFFFFFFF, false);
                String tag = r.sure() ? "Segura" : "Extra";
                g.drawString(this.font, tag, left + pw - 24 - this.font.width(tag), ry + 4,
                        0xFF000000 | (r.sure() ? 0x7BD66B : 0xFFD35C), false);
                if (pop < 1.0F) {
                    g.fill(left + 22, ry - 1, left + pw - 22, ry + 17, ((int) ((1.0F - pop) * 90) << 24) | 0xFFFFFF);
                }
            }
            y += rows() * 18 + 4;
            // premios de suerte que no salieron (aparecen cuando ya se mostró todo lo recibido)
            if (!data.missed.isEmpty() && revealed >= data.rewards.size()) {
                g.drawCenteredString(this.font, Component.literal("No te tocaron").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD), cx, y, 0xFFFFFF);
                y += 12;
                for (BossResultPacket.Miss m : data.missed) {
                    Component name;
                    Item it = itemOf(m.item());
                    int ix = left + 26;
                    if (m.kind() == 1) {
                        g.drawString(this.font, "◆", ix + 2, y + 2, 0xFF000000 | 0x8A7A4A, false);
                        name = Component.literal("Esencia");
                    } else if (it == Items.AIR) {
                        name = Component.literal("?");
                    } else {
                        ItemStack st = new ItemStack(it);
                        var pose = g.pose();
                        pose.pushPose();
                        pose.translate(ix, y, 0);
                        pose.scale(0.75F, 0.75F, 1.0F);
                        g.renderItem(st, 0, 0);
                        pose.popPose();
                        // velo gris encima: el objeto no lo conseguiste
                        g.fill(ix, y, ix + 12, y + 12, 0x88202028);
                        name = m.kind() == 2 ? Component.literal("Materiales extra") : st.getHoverName();
                        if (m.kind() == 0 && over(mx, my, ix, y, 12, 12)) {
                            hovered = st;
                        }
                    }
                    g.drawString(this.font, name.copy().withStyle(ChatFormatting.DARK_GRAY), left + 44, y + 2, 0xFFFFFFFF, false);
                    String pc = m.pct() + " %";
                    g.drawString(this.font, pc, left + pw - 24 - this.font.width(pc), y + 2, 0xFF000000 | 0x8890A0, false);
                    y += 14;
                }
                y += 4;
            }
            if (hovered != ItemStack.EMPTY && !hovered.isEmpty()) {
                g.renderTooltip(this.font, hovered, mx, my);
            }
        }
        // avisos del mundo y del Archivo
        if (data.tierLevel > 0) {
            for (FormattedCharSequence l : this.font.split(Component.literal("El mundo se vuelve más peligroso: nivel " + data.tierLevel
                    + " (los monstruos tienen +" + data.tierPct + " % de vida y de daño).").withStyle(ChatFormatting.LIGHT_PURPLE), pw - 40)) {
                g.drawCenteredString(this.font, l, cx, y, 0xFFFFFF);
                y += 10;
            }
            y += 4;
        }
        if (data.archiveNew) {
            g.drawCenteredString(this.font, Component.literal("Avance anotado en tu Archivo: ya puedes ver más cajas.").withStyle(ChatFormatting.LIGHT_PURPLE),
                    cx, y, 0xFFFFFF);
        }
        int bwd = 110;
        PanelUi.button(g, this.font, cx - bwd / 2, top + ph - 28, bwd, 20, Component.literal("Continuar"), true,
                over(mx, my, cx - bwd / 2, top + ph - 28, bwd, 20), acc);
        g.drawCenteredString(this.font, Component.literal("Volverás a la superficie en unos segundos").withStyle(ChatFormatting.DARK_GRAY),
                cx, top + ph - 40, 0xFFFFFF);
        super.render(g, mx, my, pt);
    }
}
