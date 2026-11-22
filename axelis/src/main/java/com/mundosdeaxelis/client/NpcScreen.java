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
 * La tienda (etapa 285) es una ventana con el retrato del habitante y lo que dice a la izquierda, tres pestañas (destinos,
 * provisiones y especiales) con la lista a la derecha (el camino al próximo habitante en dorado, lo bloqueado con candado) y, abajo,
 * el detalle de lo elegido y el botón «Comprar» (o doble clic).
 */
public class NpcScreen extends Screen {
    static final int B_NEXT = 0, B_SHOP = 1, B_HINT = 2, B_MOVE = 3, B_KIT = 4, B_FORCE = 5, B_BYE = 6, B_BACK = 7, B_BUY = 8;

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
    private long lastClick;
    private long flashUntil;
    private boolean flashGood;
    private Component footer;
    private long footerUntil;
    private final List<ItemStack> shopStacks = new ArrayList<>();
    /** Etapa 285: la tienda de este habitante (cada uno vende el camino al siguiente) y la pestaña y la oferta elegidas. */
    private final List<NpcShop.Offer> ofertas;
    private int tab = NpcShop.DESTINOS;
    private int sel = -1;

    private record Btn(int id, int x, int y, int w, int h, Component label) {
    }

    public NpcScreen(NpcOpenPacket p) {
        super(Component.translatable("gui.mundos_de_axelis.npc.title"));
        this.data = p;
        this.kind = NpcKinds.get(p.kind);
        this.ofertas = NpcShop.ofertas(p.kind);
        for (NpcShop.Offer o : ofertas) {
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
        // etapa 282: «guardGone» ahora quiere decir que acaba de llegar otro guardián con la llave
        if (data.state == Habitante.PRESO && data.guardGone && opening) {
            say(Component.translatable("npc.mundos_de_axelis." + data.kind + ".preso.sin_llave"));
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

    // ---- la tienda (etapa 285): retrato a la izquierda, pestañas y lista a la derecha, detalle y «Comprar» abajo

    private static final int ROW_H = 20;

    private int shopW() {
        return Math.min(400, width - 12);
    }

    private int shopH() {
        return Math.min(244, height - 12);
    }

    private int shopX() {
        return (width - shopW()) / 2;
    }

    private int shopY() {
        return (height - shopH()) / 2;
    }

    /** Columna derecha: x, ancho; la lista va de listY() a listY() + listH(). */
    private int rightX() {
        return shopX() + 112;
    }

    private int rightW() {
        return shopW() - 120;
    }

    private int listY() {
        return shopY() + 46;
    }

    private int listH() {
        return shopH() - 46 - 64;
    }

    /** Índices (en {@link #ofertas}) de la pestaña actual. */
    private List<Integer> visibles() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < ofertas.size(); i++) {
            if (ofertas.get(i).cat() == tab) {
                out.add(i);
            }
        }
        return out;
    }

    private int visibleRows() {
        return Math.max(1, listH() / ROW_H);
    }

    private int maxScroll() {
        return Math.max(0, visibles().size() - visibleRows());
    }

    /** Fila de la oferta i (global) con el desplazamiento actual, o -1 si no se ve. */
    private int rowOf(int i) {
        int r = visibles().indexOf(i) - scroll;
        return r >= 0 && r < visibleRows() ? r : -1;
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

    /** Pestaña bajo el ratón (-1 si ninguna). */
    private int tabAt(double mx, double my) {
        int tw = rightW() / 3;
        for (int t = 0; t < 3; t++) {
            int tx = rightX() + t * tw;
            if (mx >= tx && mx < tx + tw - 2 && my >= shopY() + 26 && my < shopY() + 42) {
                return t;
            }
        }
        return -1;
    }

    private void elegirTab(int t) {
        tab = t;
        scroll = 0;
        List<Integer> v = visibles();
        sel = v.isEmpty() ? -1 : v.get(0);
        UiFx.tab();
        rebuild();
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
            int by = shopY() + shopH() - 22;
            buttons.add(new Btn(B_BACK, shopX() + 8, by, 96, 15, Component.translatable("gui.mundos_de_axelis.npc.back")));
            buttons.add(new Btn(B_BUY, rightX() + rightW() - 104, by, 104, 15, Component.translatable("gui.mundos_de_axelis.npc.buy")));
            return;
        }
        List<Integer> ids = new ArrayList<>();
        ids.add(B_NEXT);
        switch (data.state) {
            case Habitante.PRESO -> {
                // etapa 282: sin «Forzar»: la llave la lleva siempre un guardián
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
            case B_BUY -> Component.translatable("gui.mundos_de_axelis.npc.buy");
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
                int t = tabAt(mx, my);
                if (t >= 0 && t != tab) {
                    elegirTab(t);
                    return true;
                }
                int i = cellAt(mx, my);
                if (i >= 0) {
                    if (i == sel && System.currentTimeMillis() - lastClick < 350) {
                        send(NpcService.A_BUY, i); // doble clic: compra
                    }
                    sel = i;
                    lastClick = System.currentTimeMillis();
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
                // abre en la pestaña que tenga algo (los destinos primero)
                tab = NpcShop.DESTINOS;
                if (visibles().isEmpty()) {
                    tab = NpcShop.PROVISIONES;
                }
                scroll = 0;
                List<Integer> v = visibles();
                sel = v.isEmpty() ? -1 : v.get(0);
                rebuild();
                UiFx.tab();
            }
            case B_BUY -> {
                if (sel >= 0) {
                    send(NpcService.A_BUY, sel);
                }
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
        if (mx < rightX() || mx >= rightX() + rightW() - 6 || my < listY()) {
            return -1;
        }
        int r = (int) ((my - listY()) / ROW_H);
        List<Integer> v = visibles();
        int k = r + scroll;
        return r >= 0 && r < visibleRows() && k < v.size() ? v.get(k) : -1;
    }

    /** Para la prueba: dónde está la oferta {@code id} en la tienda de este habitante (-1 si no la vende). */
    int ofertaIdx(String id) {
        for (int i = 0; i < ofertas.size(); i++) {
            if (ofertas.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /** Para la prueba: cambia de pestaña. */
    void verTab(int t) {
        elegirTab(t);
    }

    int precio(String id) {
        int i = ofertaIdx(id);
        return i < 0 ? -1 : ofertas.get(i).price();
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
            boolean on = b.id != B_BUY || sel >= 0 && data.residents >= ofertas.get(sel).needResidents();
            PanelUi.button(g, font, b.x, b.y, b.w, b.h, b.id == B_MOVE ? label(B_MOVE) : b.label, on, over, accent());
        }
        super.render(g, mx, my, pt);
        if (shop) {
            int i = cellAt(mx, my);
            if (i >= 0 && ofertas.get(i).compass() == null) {
                g.renderTooltip(font, shopStacks.get(i), mx, my);
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
        int x = shopX(), y = shopY(), w = shopW(), h = shopH();
        int ac = accent();
        long now = System.currentTimeMillis();
        PanelUi.frame(g, x, y, w, h, ac);
        Component who = kind == null ? Component.literal(data.kind) : Component.translatable(kind.nameKey());
        PanelUi.title(g, font, Component.translatable("gui.mundos_de_axelis.npc.shop_title", who), x + 8, y + 8, ac);
        // tu Esencia
        Component ess = Component.translatable("gui.mundos_de_axelis.npc.essence", TableClient.essence);
        int ew = font.width(ess);
        g.drawString(font, ess, x + w - ew - 10, y + 8, 0xFFE2B8FF, false);
        diamond(g, x + w - ew - 18, y + 11);
        PanelUi.divider(g, x + 8, y + 20, w - 16, ac);
        // ---- retrato y lo que dice
        int px = x + 8, py = y + 26, pw = 96, ph = 92;
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF000000 | ac);
        g.fillGradient(px, py, px + pw, py + ph, 0xFF1F2A22, 0xFF0A0F0B);
        Habitante npc = npc();
        if (npc != null) {
            g.enableScissor(px, py, px + pw, py + ph);
            HabitanteRenderer.gui = true;
            try {
                int cx = px + pw / 2, cy = py + ph + 30;
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx, cy, 56, (float) cx - mx, (float) (py + 26) - my, npc);
            } catch (RuntimeException ignored) {
                // si falla el retrato, la tienda sigue
            } finally {
                HabitanteRenderer.gui = false;
            }
            g.disableScissor();
        }
        Component dice = footer != null && now < footerUntil ? footer
                : Component.translatable("gui.mundos_de_axelis.npc.shop_hello." + (tab == NpcShop.DESTINOS ? "destinos" : "cosas"));
        int by = py + ph + 6;
        int bh = h - (by - y) - 30;
        g.fill(px, by, px + pw, by + bh, 0xC0101418);
        g.fill(px, by, px + pw, by + 1, 0xFF000000 | ac);
        int ly = by + 4;
        for (FormattedCharSequence l : font.split(dice, pw - 8)) {
            if (ly > by + bh - 10) {
                break;
            }
            g.drawString(font, l, px + 4, ly, 0xFFEFEADF, false);
            ly += 10;
        }
        // ---- pestañas
        String[] tabs = {"destinos", "provisiones", "especiales"};
        int tw = rightW() / 3;
        for (int t = 0; t < 3; t++) {
            int tx = rightX() + t * tw;
            boolean over = mx >= tx && mx < tx + tw - 2 && my >= y + 26 && my < y + 42;
            int n = 0;
            for (NpcShop.Offer o : ofertas) {
                n += o.cat() == t ? 1 : 0;
            }
            PanelUi.tab(g, font, tx, y + 26, tw - 2, 16, Component.translatable("gui.mundos_de_axelis.npc.tab." + tabs[t], n), t == tab, over, ac);
        }
        // ---- lista
        List<Integer> v = visibles();
        int rx = rightX(), rw = rightW() - 6;
        g.fill(rx, listY(), rx + rightW(), listY() + listH(), 0x60000000);
        if (v.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.mundos_de_axelis.npc.tab_vacia").withStyle(ChatFormatting.GRAY),
                    rx + rightW() / 2, listY() + listH() / 2 - 4, 0xFFB8AAD8);
        }
        for (int r = 0; r < visibleRows() && r + scroll < v.size(); r++) {
            int i = v.get(r + scroll);
            NpcShop.Offer o = ofertas.get(i);
            int ry = listY() + r * ROW_H;
            boolean locked = data.residents < o.needResidents();
            boolean afford = TableClient.essence >= o.price();
            boolean over = mx >= rx && mx < rx + rw && my >= ry && my < ry + ROW_H;
            int bg = i == sel ? 0xFF3A2C62 : over ? 0xFF2A2048 : (r & 1) == 0 ? 0xFF1A1330 : 0xFF161029;
            if (i == flashIdx && now < flashUntil) {
                float f = (flashUntil - now) / 600.0F;
                bg = 0xFF000000 | lerp(bg & 0xFFFFFF, flashGood ? 0x55E07A : 0xE05555, f * 0.6F);
            }
            g.fill(rx, ry, rx + rw, ry + ROW_H - 1, bg);
            if (i == sel) {
                g.fill(rx, ry, rx + 2, ry + ROW_H - 1, 0xFF000000 | ac);
            }
            if (o.siguiente()) {
                // el camino al próximo habitante: dorado
                g.fill(rx + rw - 2, ry, rx + rw, ry + ROW_H - 1, 0xFFFFD35C);
            }
            ItemStack st = shopStacks.get(i);
            g.renderItem(st, rx + 4, ry + 2);
            String nm = st.getHoverName().getString();
            int priceW = 34;
            int maxW = rw - 26 - priceW - (o.siguiente() ? 12 : 0);
            if (font.width(nm) > maxW) {
                nm = font.plainSubstrByWidth(nm, maxW - 6) + "…";
            }
            g.drawString(font, nm, rx + 24, ry + 6, locked ? 0xFF6C5E8C : o.siguiente() ? 0xFFFFE9A8 : 0xFFFFFFFF, false);
            if (o.siguiente()) {
                g.drawString(font, "★", rx + 24 + font.width(nm) + 3, ry + 6, 0xFFFFD35C, false);
            }
            if (locked) {
                lock(g, rx + rw - 14, ry + 5);
                g.fill(rx, ry, rx + rw, ry + ROW_H - 1, 0x50000000);
            } else {
                String pr = String.valueOf(o.price());
                g.drawString(font, pr, rx + rw - 8 - font.width(pr), ry + 6, afford ? 0xFFE2B8FF : 0xFFE07070, false);
                diamond(g, rx + rw - 14 - font.width(pr), ry + 9);
            }
        }
        if (maxScroll() > 0) {
            int bx = rx + rightW() - 4, bh2 = visibleRows() * ROW_H;
            g.fill(bx, listY(), bx + 2, listY() + bh2, 0x40FFFFFF);
            int th = Math.max(10, bh2 * visibleRows() / v.size());
            int ty = listY() + (bh2 - th) * scroll / maxScroll();
            g.fill(bx, ty, bx + 2, ty + th, 0xFF000000 | ac);
        }
        // ---- detalle de lo elegido
        int dy = listY() + listH() + 4;
        if (sel >= 0 && sel < ofertas.size()) {
            NpcShop.Offer o = ofertas.get(sel);
            ItemStack st = shopStacks.get(sel);
            PanelUi.slot(g, rx, dy, ac, false);
            g.renderItem(st, rx, dy);
            g.renderItemDecorations(font, st, rx, dy);
            g.drawString(font, st.getHoverName(), rx + 22, dy + 1, 0xFFFFFFFF, false);
            Component desc = descripcion(o, st);
            int ddy = dy + 12;
            for (FormattedCharSequence l : font.split(desc, rightW() - 24)) {
                if (ddy > dy + 28) {
                    break;
                }
                g.drawString(font, l, rx + 22, ddy, 0xFFB8C4B0, false);
                ddy += 9;
            }
            boolean locked = data.residents < o.needResidents();
            if (locked) {
                g.drawString(font, Component.translatable("gui.mundos_de_axelis.npc.need", o.needResidents()), rx, dy + 36, 0xFFE07070, false);
            }
        }
    }

    /** Lo que se cuenta de cada oferta: a dónde lleva la brújula (y quién está preso allí) o lo que dice el objeto. */
    private Component descripcion(NpcShop.Offer o, ItemStack st) {
        if (o.compass() != null) {
            Component lugar = Component.translatable("npc.mundos_de_axelis.place." + o.compass());
            if (o.siguiente()) {
                for (String[] r : NpcShop.RUTA) {
                    if (r[1].equals(o.compass())) {
                        return Component.translatable("gui.mundos_de_axelis.npc.desc.siguiente", lugar,
                                Component.translatable("entity.mundos_de_axelis.habitante." + r[0]));
                    }
                }
            }
            return Component.translatable("gui.mundos_de_axelis.npc.desc.brujula", lugar);
        }
        List<Component> tip = Screen.getTooltipFromItem(Minecraft.getInstance(), st);
        return tip.size() > 1 ? tip.get(1) : Component.translatable("gui.mundos_de_axelis.npc.desc." + o.id());
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
