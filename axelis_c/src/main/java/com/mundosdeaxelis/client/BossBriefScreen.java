package com.mundosdeaxelis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mundosdeaxelis.network.BossBriefPacket;
import com.mundosdeaxelis.network.BossStartPacket;
import com.mundosdeaxelis.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ficha del jefe, dividida en páginas (pestañas) para que no se sature de texto. Dos modos:
 * <ul>
 *   <li><b>Entrada</b> (al usar la Piedra de Portal): «Preparación» (equipo recomendado y grupo), «La dungeon» (qué vas a encontrar)
 *       y «Premios». El jefe y los premios salen como «?» en negro hasta que ese jugador venza al jefe una vez. Botones «Cancelar» y
 *       «Comenzar» (activo a los 5 s).</li>
 *   <li><b>Antesala</b> (ya dentro, antes de la pelea): «El jefe» (quién es y sus números), «Ataques» y «Consejos», con «Entendido» y
 *       «Salir de la dungeon» (pide un segundo clic).</li>
 * </ul>
 * Los textos de cada jefe salen de {@link BossLore}, así que sirve para todas las dungeons.
 */
public class BossBriefScreen extends Screen {
    private static final long WAIT_MS = 5000L;

    private final BossBriefPacket data;
    private final BossLore.Lore lore;
    private final boolean entry;
    private long openedAt;
    private LivingEntity preview;
    private int tab;
    private float scroll;
    private float scrollTarget;
    private int left, top, pw, ph, rx, rw, ry, ry2, lx, lw, by, tabY;
    private boolean lastReady;
    /** Momento en que se pulsó «Salir de la dungeon» una vez (hay que pulsar otra para confirmar). */
    private long leaveArmedAt = -1L;

    /** Una sección de texto de la columna derecha. */
    private record Section(String header, int color, List<Component> lines) {
    }

    public BossBriefScreen(BossBriefPacket data) {
        super(Component.literal("Jefe"));
        this.data = data;
        this.lore = BossLore.of(data.stage);
        this.entry = data.mode == BossBriefPacket.ENTRY;
    }

    private int accent() {
        return lore.accent();
    }

    /** ¿Se muestra al jefe de verdad? (en la antesala siempre; al entrar, solo si ya lo venciste). */
    private boolean revealed() {
        return !entry || data.known;
    }

    private String[] tabNames() {
        return entry ? new String[]{"Preparación", "La dungeon", "Premios"} : new String[]{"El jefe", "Ataques", "Consejos"};
    }

    @Override
    protected void init() {
        UiFx.open();
        openedAt = net.minecraft.Util.getMillis();
        pw = Math.min(this.width - 12, 440);
        ph = Math.min(this.height - 8, 290);
        left = (this.width - pw) / 2;
        top = (this.height - ph) / 2;
        lx = left + 12;
        lw = 128;
        rx = lx + lw + 10;
        rw = left + pw - 12 - rx;
        tabY = top + 36;
        ry = tabY + 22;
        by = top + ph - 30;
        ry2 = by - 8;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && revealed()) {
            preview = BossLore.entity(data.stage, mc.level);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean ready() {
        return !entry || net.minecraft.Util.getMillis() - openedAt >= WAIT_MS;
    }

    private int secondsLeft() {
        long l = WAIT_MS - (net.minecraft.Util.getMillis() - openedAt);
        return (int) Math.max(0, Math.ceil(l / 1000.0D));
    }

    // ------------------------------------------------------------------ contenido

    private static String hearts(float v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static Component white(String s) {
        return Component.literal(s).withStyle(ChatFormatting.WHITE);
    }

    private static Component gray(String s) {
        return Component.literal(s).withStyle(ChatFormatting.GRAY);
    }

    private static Component bullet(String head, ChatFormatting hc, String text) {
        return Component.literal(head + ": ").withStyle(hc).append(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    private List<Section> sections() {
        List<Section> out = new ArrayList<>();
        int n = data.party.size();
        if (entry) {
            if (tab == 0) {
                List<Component> rec = new ArrayList<>();
                rec.add(Component.literal("Armadura: set ").append(data.setB.copy()).append(Component.literal(" (" + data.armorB + " puntos)."))
                        .withStyle(ChatFormatting.WHITE));
                rec.add(white("Espada de unos " + data.swordB + " de daño o mejor."));
                rec.add(Component.literal("Con el set ").append(data.setA.copy()).append(Component.literal(" se puede, pero recibes más daño."))
                        .withStyle(ChatFormatting.GRAY));
                out.add(new Section("Equipo recomendado", 0x6FD0FF, rec));
                List<Component> bring = new ArrayList<>();
                bring.add(white("Comida, pociones y mascotas con armadura."));
                bring.add(gray("Las reliquias de combate ayudan mucho."));
                out.add(new Section("Lleva contigo", 0xFFD35C, bring));
                List<Component> grp = new ArrayList<>();
                grp.add(white("Cada jugador extra: +" + data.hpPerExtraPct + " % de vida y +" + data.dmgPerExtraPct + " % de daño del jefe."));
                grp.add(gray("Viajan " + n + ": vida del jefe " + data.hpNow + (data.dmgBonusPct > 0 ? ", +" + data.dmgBonusPct + " % de daño." : ", daño normal.")));
                out.add(new Section("Con más jugadores", 0xFF7B6B, grp));
            } else if (tab == 1) {
                List<Component> how = new ArrayList<>();
                how.add(white("Distinta cada vez; más larga con más jugadores."));
                how.add(gray("Puertas cerradas hasta limpiar cada sala. Mini-jefe: " + lore.miniBoss() + "."));
                out.add(new Section("Cómo es", 0xB48CFF, how));
                List<Component> rooms = new ArrayList<>();
                rooms.add(bullet("Cámara de la Llave", ChatFormatting.AQUA, "una llave para todos."));
                rooms.add(bullet("Cripta", ChatFormatting.AQUA, "cofre sellado (gasta una llave)."));
                rooms.add(bullet("Puente de losas", ChatFormatting.AQUA, "una losa por fila es falsa."));
                rooms.add(bullet("Prueba de runas", ChatFormatting.AQUA, "repite la secuencia."));
                out.add(new Section("Salas opcionales", 0x6FD0FF, rooms));
                List<Component> loot = new ArrayList<>();
                loot.add(white("Cada jugador tiene su propio botín."));
                loot.add(gray("Puedes salir antes del jefe, sin sus premios."));
                out.add(new Section("Botín y salida", 0x7BD66B, loot));
            }
        } else if (tab == 0) {
            out.add(new Section("Quién es", accent(), List.of(white(lore.desc()))));
            List<Component> st = new ArrayList<>();
            st.add(white("Vida: " + data.hpNow + (n > 1 ? " (base " + data.hpBase + ")" : "") + "."));
            st.add(white("Su golpe más fuerte (" + lore.strongHit() + ") quita " + hearts(data.strongB) + " corazones con el set B completo y "
                    + hearts(data.strongA) + " con el A."));
            st.add(Component.literal("Calculado contra lo mejor de la caja de esta etapa (set B, espada, mascotas y reliquias). No cuenta encantamientos ni pociones.")
                    .withStyle(ChatFormatting.DARK_GRAY));
            out.add(new Section("Sus números", 0xFFD35C, st));
            out.add(new Section("Este grupo", 0xFF7B6B, List.of(gray(n + (n == 1 ? " jugador" : " jugadores") + " · "
                    + (data.dmgBonusPct > 0 ? "daño del jefe +" + data.dmgBonusPct + " %." : "daño del jefe normal.")))));
        } else if (tab == 1) {
            List<Component> atk = new ArrayList<>();
            for (BossLore.Attack a : lore.attacks()) {
                atk.add(bullet(a.name(), a.danger() ? ChatFormatting.RED : ChatFormatting.GOLD, a.text()));
            }
            if (atk.isEmpty()) {
                atk.add(gray("Todavía no se sabe nada de sus ataques."));
            }
            out.add(new Section("Ataques", 0xFF7B6B, atk));
        } else {
            List<Component> tips = new ArrayList<>();
            for (String t : lore.tips()) {
                tips.add(white("• " + t));
            }
            out.add(new Section("Consejos", 0x7BD66B, tips));
            out.add(new Section("¿No te sientes preparado?", 0xFFD35C, List.of(
                    gray("Pulsa «Salir de la dungeon»: sales sin los premios del jefe y puedes volver a entrar."))));
        }
        return out;
    }

    private boolean rewardsTab() {
        return entry && tab == 2;
    }

    private Component rewardsHeader() {
        return revealed()
                ? gray("«Segura» siempre cae. Las demás dependen de la suerte y de cuánto daño le hagas al jefe.")
                : gray("Aún no conoces sus premios. Véncelo una vez para descubrirlos.");
    }

    private int contentHeight(List<Section> secs) {
        int h = 0;
        if (rewardsTab()) {
            h += this.font.split(rewardsHeader(), rw - 10).size() * 10 + 8;
            return h + data.rewards.size() * 18 + 4;
        }
        for (Section s : secs) {
            h += 14;
            for (Component c : s.lines) {
                h += this.font.split(c, rw - 10).size() * 10 + 2;
            }
            h += 6;
        }
        return h;
    }

    // ------------------------------------------------------------------ entrada

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, contentHeight(sections()) - (ry2 - ry));
        scrollTarget = Mth.clamp(scrollTarget - (float) delta * 22.0F, 0, max);
        return true;
    }

    private boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int tabW() {
        return Math.min(96, (rw - 4) / 3);
    }

    private int leaveW() {
        return 150;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int tw = tabW();
            for (int i = 0; i < 3; i++) {
                if (over(mx, my, rx + i * (tw + 2), tabY, tw, 16)) {
                    if (tab != i) {
                        tab = i;
                        scroll = 0;
                        scrollTarget = 0;
                        UiFx.tab();
                    }
                    return true;
                }
            }
            int bw = 120;
            int rightX = left + pw - 12 - bw;
            if (entry) {
                int cancelX = rightX - bw - 10;
                if (over(mx, my, rightX, by, bw, 22) && ready()) {
                    UiFx.success();
                    ModNetwork.CHANNEL.sendToServer(new BossStartPacket(data.pos));
                    onClose();
                    return true;
                }
                if (over(mx, my, rightX, by, bw, 22)) {
                    UiFx.error();
                    return true;
                }
                if (over(mx, my, cancelX, by, bw, 22)) {
                    UiFx.click();
                    onClose();
                    return true;
                }
            } else {
                if (over(mx, my, rightX, by, bw, 22)) {
                    UiFx.click();
                    onClose();
                    return true;
                }
                int leaveX = left + 12;
                if (over(mx, my, leaveX, by, leaveW(), 22)) {
                    long now = net.minecraft.Util.getMillis();
                    if (leaveArmedAt > 0 && now - leaveArmedAt < 4000L) {
                        UiFx.success();
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.connection.sendCommand("salir_dungeon confirmar");
                        }
                        onClose();
                    } else {
                        leaveArmedAt = now;
                        UiFx.click();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    // ------------------------------------------------------------------ dibujo

    private static int rgb(int c) {
        return 0xFF000000 | c;
    }

    private static Item itemOf(String id) {
        if (id == null || id.isEmpty()) {
            return Items.AIR;
        }
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return it == null ? Items.AIR : it;
    }

    private static String tagName(int tag) {
        return switch (tag) {
            case 0 -> "Segura";
            case 1 -> "Probable";
            case 2 -> "Posible";
            default -> "Rara";
        };
    }

    private static int tagColor(int tag) {
        return switch (tag) {
            case 0 -> 0x7BD66B;
            case 1 -> 0xFFD35C;
            case 2 -> 0xFF9A3C;
            default -> 0xC070FF;
        };
    }

    /** Ranura oculta: relieve gris con un «?» negro. */
    private void hiddenSlot(GuiGraphics g, int x, int y) {
        PanelUi.slot(g, x, y, 0x6C6C7C, false);
        g.fillGradient(x, y, x + 16, y + 16, 0xFF6A6A76, 0xFF40404A);
        g.drawString(this.font, "?", x + 6, y + 5, 0xFF000000, false);
        g.drawString(this.font, "?", x + 7, y + 5, 0xFF000000, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        scroll = Mth.lerp(0.35F, scroll, scrollTarget);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int bgTop = (lore.bgTop() & 0xFFFFFF) >> 1 & 0x7F7F7F;
        g.fillGradient(0, 0, this.width, this.height, 0x90000000 | bgTop, 0xB0000000 | ((lore.bgBottom() & 0xFFFFFF) >> 1 & 0x7F7F7F));
        PanelUi.frame(g, left, top, pw, ph, accent());
        String title = revealed() ? lore.name() : "? ? ?";
        PanelUi.title(g, this.font, Component.literal(title), left + 14, top + 10, accent());
        g.drawString(this.font, Component.literal((revealed() ? lore.kind() : lore.dungeon()) + " · Etapa " + data.stage
                + (entry ? "" : " · Antesala")).withStyle(ChatFormatting.GRAY), left + 14, top + 23, 0xFFB8C8B0, false);

        // ---- columna izquierda: retrato
        int lh = ry2 - tabY;
        g.fill(lx - 1, tabY - 1, lx + lw + 1, tabY + lh + 1, 0xFF0A0A0C);
        g.fillGradient(lx, tabY, lx + lw, tabY + lh, lore.bgTop(), lore.bgBottom());
        int cx = lx + lw / 2;
        if (revealed() && preview != null) {
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx, tabY + 108, data.stage == 2 ? 17 : 15, cx - mx, tabY + 44 - my, preview);
            } catch (RuntimeException e) {
                preview = null;
            }
        } else {
            // jefe sin descubrir: silueta negra con un «?» enorme que late un poco
            double pulse = 0.5D + 0.5D * Math.sin(UiFx.time() * 2.0D);
            g.fillGradient(lx + 8, tabY + 8, lx + lw - 8, tabY + 110, 0x40000000, 0xA0000000);
            g.pose().pushPose();
            g.pose().translate(cx, tabY + 40, 0);
            float sc = 6.0F + (float) pulse * 0.4F;
            g.pose().scale(sc, sc, 1.0F);
            g.drawString(this.font, "?", -2, -4, 0x30FFFFFF, false);
            g.drawString(this.font, "?", -3, -4, 0xFF000000, false);
            g.drawString(this.font, "?", -2, -4, 0xFF000000, false);
            g.pose().popPose();
        }
        int ty = tabY + 116;
        if (entry) {
            g.drawCenteredString(this.font, Component.literal("VIAJAN").withStyle(ChatFormatting.RED), cx, ty, 0xFFFFFF);
            int shown = Math.min(data.party.size(), 6);
            for (int i = 0; i < shown; i++) {
                String nm = data.party.get(i);
                if (nm.length() > 16) {
                    nm = nm.substring(0, 15) + ".";
                }
                g.drawCenteredString(this.font, Component.literal(nm).withStyle(ChatFormatting.WHITE), cx, ty + 12 + i * 10, 0xFFFFFF);
            }
            if (data.party.size() > shown) {
                g.drawCenteredString(this.font, Component.literal("+" + (data.party.size() - shown)).withStyle(ChatFormatting.GRAY), cx, ty + 12 + shown * 10, 0xFFFFFF);
            }
        } else {
            g.drawCenteredString(this.font, Component.literal("VIDA").withStyle(ChatFormatting.GREEN), cx, ty, 0xFFFFFF);
            g.drawCenteredString(this.font, Component.literal(String.valueOf(data.hpNow)).withStyle(ChatFormatting.WHITE), cx, ty + 10, 0xFFFFFF);
            g.drawCenteredString(this.font, Component.literal("GOLPE FUERTE").withStyle(ChatFormatting.GOLD), cx, ty + 26, 0xFFFFFF);
            g.drawCenteredString(this.font, Component.literal("Set B: " + hearts(data.strongB) + " ♥").withStyle(ChatFormatting.WHITE), cx, ty + 36, 0xFFFFFF);
            g.drawCenteredString(this.font, Component.literal("Set A: " + hearts(data.strongA) + " ♥").withStyle(ChatFormatting.GRAY), cx, ty + 46, 0xFFFFFF);
        }

        // ---- pestañas
        String[] names = tabNames();
        int tw = tabW();
        for (int i = 0; i < 3; i++) {
            int tx = rx + i * (tw + 2);
            PanelUi.tab(g, this.font, tx, tabY, tw, 16, Component.literal(names[i]), tab == i, over(mx, my, tx, tabY, tw, 16), accent());
        }

        // ---- contenido de la página (con desplazamiento si no cabe)
        List<Section> secs = sections();
        int content = contentHeight(secs);
        int max = Math.max(0, content - (ry2 - ry));
        scrollTarget = Mth.clamp(scrollTarget, 0, max);
        ItemStack hovered = ItemStack.EMPTY;
        g.enableScissor(rx, ry, rx + rw, ry2);
        int y = ry - Math.round(scroll);
        if (rewardsTab()) {
            for (FormattedCharSequence line : this.font.split(rewardsHeader(), rw - 10)) {
                g.drawString(this.font, line, rx + 6, y, 0xFFB8C8B0, false);
                y += 10;
            }
            y += 8;
            for (BossBriefPacket.Reward r : data.rewards) {
                if (!revealed()) {
                    hiddenSlot(g, rx + 6, y);
                    g.drawString(this.font, "? ? ? ? ?", rx + 30, y + 4, 0xFF4A4A56, false);
                } else {
                    PanelUi.slot(g, rx + 6, y, tagColor(r.tag()), false);
                    Item it = itemOf(r.item());
                    ItemStack st = it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it);
                    Component name;
                    if (st.isEmpty()) {
                        g.drawString(this.font, "◆", rx + 11, y + 4, 0xFF000000 | 0xFFD35C, false);
                        name = Component.literal("Esencia").withStyle(ChatFormatting.WHITE);
                    } else {
                        g.renderItem(st, rx + 6, y);
                        name = st.getHoverName();
                        if (over(mx, my, rx + 6, y, 16, 16) && my >= ry && my < ry2) {
                            hovered = st;
                        }
                    }
                    MutableComponent label = name.copy().withStyle(ChatFormatting.WHITE);
                    if (!r.amount().isEmpty()) {
                        label.append(Component.literal("  " + r.amount()).withStyle(ChatFormatting.GRAY));
                    }
                    g.drawString(this.font, label, rx + 30, y + 4, 0xFFFFFFFF, false);
                    String tg = tagName(r.tag());
                    g.drawString(this.font, tg, rx + rw - 12 - this.font.width(tg), y + 4, rgb(tagColor(r.tag())), false);
                }
                y += 18;
            }
        } else {
            for (Section s : secs) {
                g.fill(rx, y + 2, rx + 3, y + 11, rgb(s.color));
                g.drawString(this.font, s.header.toUpperCase(Locale.ROOT), rx + 8, y + 3, rgb(s.color), false);
                y += 14;
                for (Component c : s.lines) {
                    for (FormattedCharSequence line : this.font.split(c, rw - 10)) {
                        g.drawString(this.font, line, rx + 8, y, 0xFFE8EEE4, false);
                        y += 10;
                    }
                    y += 2;
                }
                y += 6;
            }
        }
        g.disableScissor();
        if (max > 0) {
            int trackH = ry2 - ry;
            int barH = Math.max(14, trackH * trackH / (trackH + max));
            int barY = ry + (int) ((trackH - barH) * (scroll / max));
            g.fill(rx + rw - 3, ry, rx + rw - 1, ry2, 0x40FFFFFF);
            g.fill(rx + rw - 3, barY, rx + rw - 1, barY + barH, 0xC0FFFFFF);
            if (scroll < max - 2) {
                g.drawCenteredString(this.font, "▼", rx + rw / 2, ry2 - 9, 0xFFB8C8B0);
            }
        }

        // ---- botones
        int bw = 120;
        int rightX = left + pw - 12 - bw;
        if (entry) {
            int cancelX = rightX - bw - 10;
            boolean ready = ready();
            PanelUi.button(g, this.font, cancelX, by, bw, 22, Component.literal("Cancelar"), true, over(mx, my, cancelX, by, bw, 22), 0xB89CD8);
            Component label = ready ? Component.literal("Comenzar") : Component.literal("Comenzar (" + secondsLeft() + ")");
            PanelUi.button(g, this.font, rightX, by, bw, 22, label, ready, ready && over(mx, my, rightX, by, bw, 22), accent());
            if (!ready) {
                float frac = (net.minecraft.Util.getMillis() - openedAt) / (float) WAIT_MS;
                g.fill(rightX + 2, by + 19, rightX + 2 + (int) ((bw - 4) * Mth.clamp(frac, 0.0F, 1.0F)), by + 21, rgb(accent()));
            } else if (!lastReady) {
                UiFx.success();
            }
            lastReady = ready;
            g.drawString(this.font, Component.literal("Esc para salir sin entrar").withStyle(ChatFormatting.DARK_GRAY),
                    left + 14, by + 7, 0xFF7A8A78, false);
        } else {
            boolean armed = leaveArmedAt > 0 && net.minecraft.Util.getMillis() - leaveArmedAt < 4000L;
            int leaveX = left + 12;
            PanelUi.button(g, this.font, leaveX, by, leaveW(), 22,
                    Component.literal(armed ? "¿Seguro? Pulsa otra vez" : "Salir de la dungeon"), true, over(mx, my, leaveX, by, leaveW(), 22),
                    armed ? 0xFF5A5A : 0xB89CD8);
            PanelUi.button(g, this.font, rightX, by, bw, 22, Component.literal("Entendido"), true, over(mx, my, rightX, by, bw, 22), accent());
            int hintX = leaveX + leaveW() + 10;
            int hintW = rightX - 10 - hintX;
            String hint = "Esc cierra · /ficha_jefe la vuelve a abrir";
            if (this.font.width(hint) > hintW) hint = "Esc cierra · /ficha_jefe";
            if (this.font.width(hint) > hintW) hint = "/ficha_jefe";
            if (this.font.width(hint) <= hintW) {
                g.drawString(this.font, Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY),
                        hintX + (hintW - this.font.width(hint)) / 2, by + 7, 0xFF7A8A78, false);
            }
        }
        super.render(g, mx, my, pt);
        if (!hovered.isEmpty()) {
            g.renderTooltip(this.font, hovered, mx, my);
        }
    }
}
