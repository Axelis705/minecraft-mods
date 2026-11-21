package com.mundosdeaxelis.client;

import com.mundosdeaxelis.entity.Habitante;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.NpcActionPacket;
import com.mundosdeaxelis.network.NpcOpenPacket;
import com.mundosdeaxelis.npc.NpcKinds;
import com.mundosdeaxelis.npc.NpcService;
import com.mundosdeaxelis.npc.NpcShop;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Ventana de un habitante (etapa 257). Abajo, un cuadro de diálogo de juego de rol: su retrato en 3D (te sigue con la mirada),
 * su nombre, el texto que se va escribiendo letra a letra (con un "bip" suave) y los botones según cómo esté:
 * <ul>
 * <li>Presa: seguir hablando, forzar la cerradura (si la llave se perdió) y adiós.</li>
 * <li>Libre: seguir hablando, pedir otra semilla de casa y adiós.</li>
 * <li>Vecina: seguir hablando, tienda, pistas, mudarse y adiós.</li>
 * </ul>
 * La tienda es un panel en el centro con las ofertas (las bloqueadas en gris con cuántos vecinos faltan) y tu Esencia.
 */
public class NpcScreen extends Screen {
    static final int B_NEXT = 0, B_SHOP = 1, B_HINT = 2, B_MOVE = 3, B_KIT = 4, B_FORCE = 5, B_BYE = 6, B_BACK = 7;

    private NpcOpenPacket data;
    private final NpcKinds.Kind kind;
    private boolean shop;
    private Component line = Component.empty();
    private float shown;
    private int lineIdx;
    private String stage = "";
    private int hintN;
    private long moveConfirm;
    private final List<Btn> buttons = new ArrayList<>();
    // tienda
    private int flashIdx = -1;
    private long flashUntil;
    private boolean flashGood;
    private Component footer;
    private long footerUntil;
    private final List<ItemStack> shopStacks = new ArrayList<>();

    private record Btn(int id, int x, int y, int w, int h, Component label) {
    }

    public NpcScreen(NpcOpenPacket p) {
        super(Component.translatable("gui.mundos_de_axelis.npc.title"));
        this.data = p;
        this.kind = NpcKinds.get(p.kind);
        for (NpcShop.Offer o : NpcShop.OFFERS) {
            ItemStack s = o.stack();
            if (o.compass() != null) {
                s.setHoverName(Component.translatable("item.mundos_de_axelis.brujula_npc", Component.translatable("npc.mundos_de_axelis.place." + o.compass()))
                        .withStyle(st -> st.withItalic(false).withColor(0x9FE07A)));
            }
            shopStacks.add(s);
        }
        startStage(true);
        react(p, true);
    }

    /** Llega otra respuesta del servidor para el mismo habitante. */
    public boolean accepts(NpcOpenPacket p) {
        return p.entityId == data.entityId;
    }

    public void update(NpcOpenPacket p) {
        boolean stateChanged = p.state != data.state;
        data = p;
        if (stateChanged) {
            startStage(false);
        }
        react(p, false);
        rebuild();
    }

    private Habitante npc() {
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(data.entityId);
        return e instanceof Habitante h ? h : null;
    }

    // ================================================================== texto

    private String stageName() {
        return switch (data.state) {
            case Habitante.PRESO -> "preso";
            case Habitante.LIBRE -> "libre";
            default -> "vecino";
        };
    }

    private int stageLines() {
        if (kind == null) {
            return 1;
        }
        return switch (data.state) {
            case Habitante.PRESO -> kind.presoLines();
            case Habitante.LIBRE -> kind.libreLines();
            default -> kind.vecinoLines();
        };
    }

    private void startStage(boolean opening) {
        stage = stageName();
        int n = stageLines();
        // de vecina empieza por una frase distinta cada vez; presa y libre cuentan su historia en orden
        lineIdx = data.state == Habitante.VECINO && opening ? (int) (Math.random() * n) : 0;
        if (data.state == Habitante.PRESO && data.guardGone && opening) {
            say(Component.translatable("npc.mundos_de_axelis." + data.kind + ".preso.sin_llave", NpcService.FORCE_IRON));
            lineIdx = -1;
            return;
        }
        if (data.state == Habitante.LIBRE && opening) {
            // ya rescatada: lo que dice cuando vuelves a hablarle
            say(Component.translatable("npc.mundos_de_axelis." + data.kind + ".libre.espera"));
            lineIdx = -1;
            return;
        }
        sayLine();
    }

    private void sayLine() {
        if (kind == null) {
            return;
        }
        say(Component.translatable(kind.line(stage, Math.floorMod(lineIdx, stageLines()))));
    }

    private void say(Component c) {
        line = c;
        shown = 0;
    }

    private void react(NpcOpenPacket p, boolean opening) {
        String k = "npc.mundos_de_axelis." + p.kind + ".";
        switch (p.result) {
            case NpcService.R_RESCUED, NpcService.R_FORCED -> {
                shop = false;
                stage = "libre";
                lineIdx = 0;
                sayLine();
                UiFx.success();
                Component who = kind == null ? Component.literal(p.kind) : Component.translatable(kind.nameKey());
                NoticeHud.push(Component.translatable("gui.mundos_de_axelis.npc.rescued", who).withStyle(ChatFormatting.BOLD),
                        Component.translatable("gui.mundos_de_axelis.npc.rescued.sub"), kind == null ? 0x7DC45A : kind.accent(), 120);
            }
            case NpcService.R_BOUGHT -> {
                flash(p.extra, true);
                footer(Component.translatable("gui.mundos_de_axelis.npc.bought", shopStacks.get(Mth.clamp(p.extra, 0, shopStacks.size() - 1)).getHoverName()), true);
                UiFx.success();
            }
            case NpcService.R_NO_ESSENCE -> {
                flash(p.extra, false);
                footer(Component.translatable("gui.mundos_de_axelis.npc.no_essence"), false);
                UiFx.error();
            }
            case NpcService.R_LOCKED -> {
                flash(p.extra, false);
                footer(Component.translatable("gui.mundos_de_axelis.npc.locked"), false);
                UiFx.error();
            }
            case NpcService.R_NONE_FOUND -> {
                flash(p.extra, false);
                footer(Component.translatable("gui.mundos_de_axelis.npc.none_found", p.msg == null ? Component.empty() : p.msg), false);
                UiFx.error();
            }
            case NpcService.R_KIT_GIVEN -> {
                say(Component.translatable(k + "kit"));
                UiFx.success();
            }
            case NpcService.R_HAS_KIT -> say(Component.translatable(k + "ya_kit"));
            case NpcService.R_MOVED -> {
                shop = false;
                say(Component.translatable(k + "mudanza"));
                lineIdx = -1;
            }
            case NpcService.R_HINT -> {
                shop = false;
                if (p.msg != null) {
                    say(p.msg);
                }
            }
            case NpcService.R_NEED_IRON -> {
                say(Component.translatable(k + "falta_hierro", p.extra));
                UiFx.error();
            }
            case NpcService.R_NO_HOUSE -> {
                shop = false;
                say(Component.translatable(k + "sin_casa"));
            }
            default -> {
            }
        }
    }

    private void flash(int idx, boolean good) {
        flashIdx = idx;
        flashGood = good;
        flashUntil = System.currentTimeMillis() + 600;
    }

    private void footer(Component c, boolean good) {
        footer = c.copy().withStyle(good ? ChatFormatting.GREEN : ChatFormatting.RED);
        footerUntil = System.currentTimeMillis() + 2600;
    }

    private void send(int action, int arg) {
        ModNetwork.CHANNEL.sendToServer(new NpcActionPacket(data.entityId, action, arg));
    }

    // ================================================================== disposición

    private int boxW() {
        return Math.min(360, width - 20);
    }

    private int boxH() {
        return 96;
    }

    private int boxX() {
        return (width - boxW()) / 2;
    }

    private int boxY() {
        return height - boxH() - 12;
    }

    private static final int SHOP_W = 300, CELL_W = 140, CELL_H = 24;

    private int shopRows() {
        return (NpcShop.OFFERS.size() + 1) / 2;
    }

    /** Filas que caben en la pantalla (con la interfaz grande no caben todas: el resto se ve con la rueda del ratón). */
    private int visibleRows() {
        int fit = (height - 8 - 30 - 34) / (CELL_H + 2);
        return Math.max(2, Math.min(shopRows(), fit));
    }

    private int maxScroll() {
        return Math.max(0, shopRows() - visibleRows());
    }

    private int shopH() {
        return 30 + visibleRows() * (CELL_H + 2) + 34;
    }

    /** Dónde se dibuja la oferta i (null si con el desplazamiento actual no se ve). */
    private int[] cellPos(int i) {
        int row = i / 2 - scroll;
        if (row < 0 || row >= visibleRows()) {
            return null;
        }
        return new int[]{shopX() + 8 + (i % 2) * (CELL_W + 4), shopY() + 30 + row * (CELL_H + 2)};
    }

    private int scroll;

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (shop && maxScroll() > 0) {
            scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private int shopX() {
        return (width - SHOP_W) / 2;
    }

    private int shopY() {
        return Math.max(4, (height - shopH()) / 2);
    }

    @Override
    protected void init() {
        super.init();
        UiFx.open();
        rebuild();
    }

    private void rebuild() {
        buttons.clear();
        if (shop) {
            buttons.add(new Btn(B_BACK, shopX() + SHOP_W / 2 - 40, shopY() + shopH() - 22, 80, 15, Component.translatable("gui.mundos_de_axelis.npc.back")));
            return;
        }
        List<Integer> ids = new ArrayList<>();
        ids.add(B_NEXT);
        switch (data.state) {
            case Habitante.PRESO -> {
                if (data.guardGone) {
                    ids.add(B_FORCE);
                }
            }
            case Habitante.LIBRE -> ids.add(B_KIT);
            default -> {
                ids.add(B_SHOP);
                ids.add(B_HINT);
                ids.add(B_MOVE);
            }
        }
        ids.add(B_BYE);
        int x0 = boxX() + 82, x1 = boxX() + boxW() - 8;
        int gap = 3;
        int w = (x1 - x0 - gap * (ids.size() - 1)) / ids.size();
        int y = boxY() + boxH() - 21;
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i);
            buttons.add(new Btn(id, x0 + i * (w + gap), y, w, 15, label(id)));
        }
    }

    private Component label(int id) {
        return switch (id) {
            case B_NEXT -> Component.translatable("gui.mundos_de_axelis.npc.next");
            case B_SHOP -> Component.translatable("gui.mundos_de_axelis.npc.shop");
            case B_HINT -> Component.translatable("gui.mundos_de_axelis.npc.hint");
            case B_MOVE -> Component.translatable(System.currentTimeMillis() < moveConfirm ? "gui.mundos_de_axelis.npc.move_sure" : "gui.mundos_de_axelis.npc.move");
            case B_KIT -> Component.translatable("gui.mundos_de_axelis.npc.kit");
            case B_FORCE -> Component.translatable("gui.mundos_de_axelis.npc.force", NpcService.FORCE_IRON);
            case B_BACK -> Component.translatable("gui.mundos_de_axelis.npc.back");
            default -> Component.translatable("gui.mundos_de_axelis.npc.bye");
        };
    }

    // ================================================================== entrada

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (Btn b : buttons) {
                if (mx >= b.x && mx < b.x + b.w && my >= b.y && my < b.y + b.h) {
                    press(b.id);
                    return true;
                }
            }
            if (shop) {
                int i = cellAt(mx, my);
                if (i >= 0) {
                    send(NpcService.A_BUY, i);
                    UiFx.click();
                    return true;
                }
            } else if (inText(mx, my)) {
                advance();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!shop && (key == 32 || key == 257 || key == 335)) { // espacio o enter: seguir
            advance();
            return true;
        }
        if (shop && key == 256) {
            shop = false;
            rebuild();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    /** Si el texto aún se escribe, se muestra entero; si no, la siguiente frase. */
    private void advance() {
        if (shown < line.getString().length()) {
            shown = line.getString().length();
            return;
        }
        lineIdx = lineIdx < 0 ? 0 : lineIdx + 1;
        stage = stageName();
        sayLine();
        send(NpcService.A_TALK, 0);
        UiFx.click();
    }

    void press(int id) {
        UiFx.click();
        switch (id) {
            case B_NEXT -> advance();
            case B_SHOP -> {
                shop = true;
                send(NpcService.A_TALK, 0);
                rebuild();
                UiFx.tab();
            }
            case B_HINT -> send(NpcService.A_HINT, hintN++);
            case B_MOVE -> {
                if (System.currentTimeMillis() < moveConfirm) {
                    moveConfirm = 0;
                    send(NpcService.A_MOVE, 0);
                } else {
                    moveConfirm = System.currentTimeMillis() + 3000;
                    say(Component.translatable("npc.mundos_de_axelis." + data.kind + ".mudanza.seguro"));
                }
                rebuild();
            }
            case B_KIT -> send(NpcService.A_KIT, 0);
            case B_FORCE -> send(NpcService.A_FORCE, 0);
            case B_BACK -> {
                shop = false;
                rebuild();
            }
            default -> onClose();
        }
    }

    // para la prueba automática
    String lineText() {
        return line.getString();
    }

    boolean inShop() {
        return shop;
    }

    int state() {
        return data.state;
    }

    void buy(int idx) {
        send(NpcService.A_BUY, idx);
    }

    private boolean inText(double mx, double my) {
        return mx >= boxX() + 80 && mx < boxX() + boxW() && my >= boxY() && my < boxY() + boxH() - 24;
    }

    private int cellAt(double mx, double my) {
        for (int i = 0; i < NpcShop.OFFERS.size(); i++) {
            int[] p = cellPos(i);
            if (p == null) {
                continue;
            }
            int cx = p[0], cy = p[1];
            if (mx >= cx && mx < cx + CELL_W && my >= cy && my < cy + CELL_H) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void tick() {
        super.tick();
        Habitante h = npc();
        Minecraft mc = Minecraft.getInstance();
        if (h == null || !h.isAlive() || mc.player == null || mc.player.distanceToSqr(h) > 12 * 12) {
            onClose();
            return;
        }
        if (moveConfirm != 0 && System.currentTimeMillis() > moveConfirm) {
            moveConfirm = 0;
            rebuild();
        }
        // el texto se escribe solo, con un "bip" suave cada pocas letras
        int len = line.getString().length();
        if (shown < len) {
            int before = (int) shown;
            shown = Math.min(len, shown + 2.2F);
            if ((int) shown / 3 != before / 3 && mc.getSoundManager() != null) {
                float pitch = 1.35F + (float) Math.random() * 0.3F;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT.value(), pitch, 0.09F));
            }
        }
    }

    @Override
    public void onClose() {
        send(NpcService.A_CLOSE, 0);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ================================================================== dibujo

    private int accent() {
        return kind == null ? 0x7DC45A : kind.accent();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        if (shop) {
            renderShop(g, mx, my);
        } else {
            renderTalk(g, mx, my);
        }
        for (Btn b : buttons) {
            boolean over = mx >= b.x && mx < b.x + b.w && my >= b.y && my < b.y + b.h;
            PanelUi.button(g, font, b.x, b.y, b.w, b.h, b.id == B_MOVE ? label(B_MOVE) : b.label, true, over, accent());
        }
        super.render(g, mx, my, pt);
        if (shop) {
            int i = cellAt(mx, my);
            if (i >= 0) {
                List<Component> tip = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), shopStacks.get(i)));
                NpcShop.Offer o = NpcShop.OFFERS.get(i);
                tip.add(Component.translatable("gui.mundos_de_axelis.npc.price", o.price()).withStyle(ChatFormatting.LIGHT_PURPLE));
                if (data.residents < o.needResidents()) {
                    tip.add(Component.translatable("gui.mundos_de_axelis.npc.need", o.needResidents()).withStyle(ChatFormatting.RED));
                }
                g.renderComponentTooltip(font, tip, mx, my);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        // sin oscurecer toda la pantalla en la charla: solo un degradado abajo, para seguir viendo el mundo
        if (shop) {
            super.renderBackground(g);
        } else {
            g.fillGradient(0, height - boxH() - 50, width, height, 0x00000000, 0xA0000000);
        }
    }

    private void renderTalk(GuiGraphics g, int mx, int my) {
        int x = boxX(), y = boxY(), w = boxW(), h = boxH();
        int ac = accent();
        PanelUi.frame(g, x, y, w, h, ac);
        // retrato
        int px = x + 7, py = y + 7, pw = 66, ph = h - 14;
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF000000 | ac);
        g.fillGradient(px, py, px + pw, py + ph, 0xFF1C2A1A, 0xFF0B120A);
        Habitante npc = npc();
        if (npc != null) {
            g.enableScissor(px, py, px + pw, py + ph);
            HabitanteRenderer.gui = true;
            try {
                int cx = px + pw / 2, cy = py + ph + 40;
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx, cy, 44, (float) cx - mx, (float) (py + 22) - my, npc);
            } catch (RuntimeException ignored) {
                // si algo falla al dibujar el retrato, la ventana sigue
            } finally {
                HabitanteRenderer.gui = false;
            }
            g.disableScissor();
        }
        // nombre y estado
        Component name = kind == null ? Component.literal(data.kind) : Component.translatable(kind.nameKey());
        PanelUi.title(g, font, name, x + 82, y + 7, ac);
        Component chip = Component.translatable("gui.mundos_de_axelis.npc.state." + stageName());
        int cw = font.width(chip) + 8;
        int chx = x + w - cw - 8;
        g.fill(chx, y + 6, chx + cw, y + 17, 0x60000000 | (ac & 0xFFFFFF));
        g.drawString(font, chip, chx + 4, y + 8, 0xFFFFFFFF, false);
        PanelUi.divider(g, x + 82, y + 20, w - 90, ac);
        // texto escribiéndose
        String full = line.getString();
        String part = full.substring(0, Mth.clamp((int) shown, 0, full.length()));
        List<FormattedCharSequence> lines = font.split(Component.literal(part), w - 92);
        int ty = y + 26;
        for (int i = 0; i < lines.size() && i < 5; i++) {
            g.drawString(font, lines.get(i), x + 84, ty + i * 10, 0xFFEFEADF, false);
        }
        if (shown >= full.length() && (System.currentTimeMillis() / 400) % 2 == 0) {
            g.drawString(font, "▼", x + w - 14, y + h - 32, 0xFF000000 | ac, false); // flecha: hay más
        }
    }

    private void renderShop(GuiGraphics g, int mx, int my) {
        int x = shopX(), y = shopY(), h = shopH();
        int ac = accent();
        PanelUi.frame(g, x, y, SHOP_W, h, ac);
        Component title = Component.translatable("gui.mundos_de_axelis.npc.shop_title",
                kind == null ? Component.literal(data.kind) : Component.translatable(kind.nameKey()));
        PanelUi.title(g, font, title, x + 8, y + 8, ac);
        // Esencia del jugador
        Component ess = Component.translatable("gui.mundos_de_axelis.npc.essence", TableClient.essence);
        int ew = font.width(ess);
        g.drawString(font, ess, x + SHOP_W - ew - 10, y + 8, 0xFFE2B8FF, false);
        diamond(g, x + SHOP_W - ew - 18, y + 11);
        PanelUi.divider(g, x + 8, y + 22, SHOP_W - 16, ac);
        long now = System.currentTimeMillis();
        for (int i = 0; i < NpcShop.OFFERS.size(); i++) {
            NpcShop.Offer o = NpcShop.OFFERS.get(i);
            int[] cp = cellPos(i);
            if (cp == null) {
                continue;
            }
            int cx = cp[0], cy = cp[1];
            boolean locked = data.residents < o.needResidents();
            boolean afford = TableClient.essence >= o.price();
            boolean over = mx >= cx && mx < cx + CELL_W && my >= cy && my < cy + CELL_H;
            int bg = locked ? 0xFF14101E : over ? 0xFF3A2C62 : 0xFF221836;
            if (i == flashIdx && now < flashUntil) {
                float f = (flashUntil - now) / 600.0F;
                int col = flashGood ? 0x55E07A : 0xE05555;
                bg = 0xFF000000 | lerp(bg & 0xFFFFFF, col, f * 0.6F);
            }
            g.fill(cx, cy, cx + CELL_W, cy + CELL_H, locked ? 0xFF2A2238 : over ? 0xFF000000 | ac : 0xFF3B2B66);
            g.fill(cx + 1, cy + 1, cx + CELL_W - 1, cy + CELL_H - 1, bg);
            PanelUi.slot(g, cx + 4, cy + 4, ac, false);
            ItemStack st = shopStacks.get(i);
            g.renderItem(st, cx + 4, cy + 4);
            g.renderItemDecorations(font, st, cx + 4, cy + 4);
            String nm = st.getHoverName().getString();
            int maxW = CELL_W - 28;
            if (font.width(nm) > maxW) {
                nm = font.plainSubstrByWidth(nm, maxW - 6) + "…";
            }
            g.drawString(font, nm, cx + 24, cy + 3, locked ? 0xFF6C5E8C : 0xFFFFFFFF, false);
            if (locked) {
                g.drawString(font, Component.translatable("gui.mundos_de_axelis.npc.need_short", o.needResidents()), cx + 24, cy + 13, 0xFF8C6A6A, false);
                if (i == flashIdx && now < flashUntil) {
                    g.fill(cx + 1, cy + 1, cx + CELL_W - 1, cy + CELL_H - 1, 0x30E05555);
                }
                g.fill(cx + 1, cy + 1, cx + CELL_W - 1, cy + CELL_H - 1, 0x50000000);
                lock(g, cx + CELL_W - 12, cy + 7);
            } else {
                diamond(g, cx + 25, cy + 16);
                g.drawString(font, String.valueOf(o.price()), cx + 32, cy + 13, afford ? 0xFFE2B8FF : 0xFFE07070, false);
            }
        }
        if (maxScroll() > 0) {
            int bx = x + SHOP_W - 5, by = y + 30, bh = visibleRows() * (CELL_H + 2) - 2;
            g.fill(bx, by, bx + 2, by + bh, 0x40FFFFFF);
            int th = Math.max(10, bh * visibleRows() / shopRows());
            int ty = by + (bh - th) * scroll / maxScroll();
            g.fill(bx, ty, bx + 2, ty + th, 0xFF000000 | ac);
        }
        int fy = y + h - 36;
        if (footer != null && now < footerUntil) {
            g.drawCenteredString(font, footer, x + SHOP_W / 2, fy, 0xFFFFFFFF);
        } else {
            g.drawCenteredString(font, Component.translatable("gui.mundos_de_axelis.npc.shop_hint", data.residents)
                    .withStyle(ChatFormatting.GRAY), x + SHOP_W / 2, fy, 0xFFB8AAD8);
        }
    }

    private static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0, 1);
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int gg = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (gg << 8) | bl;
    }

    /** Rombo morado: el símbolo de la Esencia. */
    private static void diamond(GuiGraphics g, int cx, int cy) {
        g.fill(cx - 1, cy - 3, cx + 2, cy + 4, 0xFF6A2FB0);
        g.fill(cx - 3, cy - 1, cx + 4, cy + 2, 0xFF6A2FB0);
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFB57CFF);
        g.fill(cx, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
    }

    private static void lock(GuiGraphics g, int x, int y) {
        g.fill(x, y + 4, x + 8, y + 10, 0xFF8C7AA8);
        g.fill(x + 1, y, x + 2, y + 4, 0xFF8C7AA8);
        g.fill(x + 6, y, x + 7, y + 4, 0xFF8C7AA8);
        g.fill(x + 1, y, x + 7, y + 1, 0xFF8C7AA8);
        g.fill(x + 3, y + 6, x + 5, y + 8, 0xFF2A2238);
    }
}
