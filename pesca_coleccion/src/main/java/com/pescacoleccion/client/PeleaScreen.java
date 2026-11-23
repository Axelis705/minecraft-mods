package com.pescacoleccion.client;

import com.axeliscore.plat.client.CoreScreen;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Rareza;
import com.axeliscore.ui.Theme;
import com.axeliscore.ui.Ui;
import com.pescacoleccion.pelea.PeleaSim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * (Cliente) La pelea con el pez: abajo de la pantalla, sin tapar el agua. Se juega en vivo con {@link PeleaSim} y al terminar manda las
 * teclas al servidor (que la vuelve a jugar para decidir).
 * <ul>
 *     <li>Arriba: hilo y energía del pez.</li>
 *     <li>Aguja: A y D (mantener) contra el tirón del pez; hay que quedarse en la zona verde.</li>
 *     <li>Carril: flechas que llegan a la línea dorada al ritmo; ← ↑ ↓ → (las rojas no se aprietan).</li>
 *     <li>Salto: anillo que se cierra; Espacio justo cuando toca la marca.</li>
 *     <li>Final: ↑ y ↓ (o W y S) alternando rápido.</li>
 * </ul>
 */
public class PeleaScreen extends CoreScreen {
    private static final int PW = 300, PH = 112;
    private static final int[] KEY_COLOR = {0xFF5FB7FF, 0xFF7CE38B, 0xFFFFC14D, 0xFFFF7AC8};
    /** Flecha hacia arriba de 7x7 (las demás se giran). */
    private static final String[] ARROW = {"...#...", "..###..", ".#####.", "#######", "..###..", "..###..", "..###.."};

    private final PeleaSim sim;
    private final int fightId;
    private final Component fishName;
    private final Rareza rareza;
    private final Consumer<int[]> onEnd;
    private final long start = System.nanoTime();
    private final List<int[]> events = new ArrayList<>();
    private final boolean[] held = new boolean[16];
    private boolean sent;
    private long endShownAt = -1;
    private int px, py;
    private float shake;
    private int lastCombo, lastMisses, lastPerfects, lastGoods, lastTraps, lastJumpsOk, lastJumpsFailed, lastPhase;

    /** Textos que suben y se desvanecen ("¡Perfecto!", "Bien", "Fallo"...). */
    private record Pop(String text, int color, long at, int x) {
    }

    private final List<Pop> pops = new ArrayList<>();

    public PeleaScreen(int fightId, PeleaSim.Params params, Component fishName, Rareza rareza, Consumer<int[]> onEnd) {
        super(Component.translatable("pesca_coleccion.pelea.titulo"));
        this.fightId = fightId;
        this.sim = new PeleaSim(params);
        this.fishName = fishName;
        this.rareza = rareza;
        this.onEnd = onEnd;
    }

    public int fightId() {
        return fightId;
    }

    /** Para las pruebas: la simulación que se está jugando. */
    public PeleaSim sim() {
        return sim;
    }

    private int nowMs() {
        return (int) ((System.nanoTime() - start) / 1_000_000L);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        px = (width - PW) / 2;
        py = height - PH - 46;
    }

    // ------------------------------------------------------------------ teclas

    private static int map(int key) {
        return switch (key) {
            case 263 -> PeleaSim.LEFT;
            case 265 -> PeleaSim.UP;
            case 264 -> PeleaSim.DOWN;
            case 262 -> PeleaSim.RIGHT;
            case 65 -> PeleaSim.A;
            case 68 -> PeleaSim.D;
            case 32 -> PeleaSim.SPACE;
            case 87 -> PeleaSim.W;
            case 83 -> PeleaSim.S;
            default -> -1;
        };
    }

    private void feed(int k, boolean down) {
        if (sim.result != PeleaSim.PELEANDO) {
            return;
        }
        int ms = nowMs();
        if (!events.isEmpty() && ms < events.get(events.size() - 1)[0]) {
            ms = events.get(events.size() - 1)[0];
        }
        events.add(new int[] {ms, k, down ? 1 : 0});
        sim.input(ms, k, down);
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        if (key == 256) {   // Esc: soltar el pez
            giveUp();
            return true;
        }
        int k = map(key);
        if (k < 0) {
            return true;
        }
        if (held[k]) {
            return true;   // tecla que se repite sola al mantenerla: no cuenta
        }
        held[k] = true;
        feed(k, true);
        return true;
    }

    @Override
    protected boolean onKeyRelease(int key, int scan, int mods) {
        int k = map(key);
        if (k < 0) {
            return false;
        }
        held[k] = false;
        if (k == PeleaSim.A || k == PeleaSim.D) {
            feed(k, false);
        }
        return true;
    }

    private void giveUp() {
        if (!sent) {
            sent = true;
            onEnd.accept(null);
        }
        onClose();
    }

    // ------------------------------------------------------------------ sonidos

    private static void sound(net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> s, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s.value(), pitch, vol));
    }

    private void feedback() {
        int hitX = px + 74;
        long t = System.currentTimeMillis();
        if (sim.perfects > lastPerfects) {
            pops.add(new Pop("¡Perfecto!", 0xFFFFE680, t, hitX));
            sound(SoundEvents.NOTE_BLOCK_BELL, 1.4F + Math.min(0.6F, sim.combo * 0.03F), 0.5F);
        } else if (sim.goods > lastGoods) {
            pops.add(new Pop("Bien", 0xFF9BE8FF, t, hitX));
            sound(SoundEvents.NOTE_BLOCK_PLING, 1.1F + Math.min(0.5F, sim.combo * 0.03F), 0.4F);
        }
        if (sim.misses > lastMisses) {
            pops.add(new Pop("Fallo", 0xFFFF7A6A, t, hitX));
            sound(SoundEvents.NOTE_BLOCK_BASS, 0.6F, 0.6F);
            shake = 1;
        }
        if (sim.traps > lastTraps) {
            pops.add(new Pop("¡Trampa!", 0xFFFF4A4A, t, hitX));
            sound(SoundEvents.NOTE_BLOCK_DIDGERIDOO, 0.6F, 0.7F);
            shake = 1.4F;
        }
        if (sim.jumpsOk > lastJumpsOk) {
            pops.add(new Pop("¡Salto!", 0xFF7CF29A, t, px + PW / 2));
            sound(SoundEvents.NOTE_BLOCK_CHIME, 1.2F, 0.7F);
        }
        if (sim.jumpsFailed > lastJumpsFailed) {
            pops.add(new Pop("Se soltó un poco", 0xFFFF9A6A, t, px + PW / 2));
            shake = 1.2F;
        }
        if (sim.phase != lastPhase) {
            if (sim.phase == PeleaSim.SALTO) {
                sound(SoundEvents.NOTE_BLOCK_XYLOPHONE, 1.6F, 0.6F);
            } else if (sim.phase == PeleaSim.FINAL) {
                sound(SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE, 1.0F, 0.8F);
            }
        }
        lastPerfects = sim.perfects;
        lastGoods = sim.goods;
        lastMisses = sim.misses;
        lastTraps = sim.traps;
        lastJumpsOk = sim.jumpsOk;
        lastJumpsFailed = sim.jumpsFailed;
        lastPhase = sim.phase;
        lastCombo = sim.combo;
        pops.removeIf(p -> t - p.at() > 700);
    }

    // ------------------------------------------------------------------ dibujo

    /** Solo para las pruebas automáticas: la pelea se juega sola (como un jugador muy bueno) por el mismo camino que las teclas. */
    public static boolean autoplay;
    private int autoNote, autoMash;
    private boolean autoUp;

    private void autoPlay() {
        int t = nowMs();
        // flechas a su tiempo exacto (antes de avanzar la simulación, para que no se den por falladas si los cuadros van lentos)
        while (autoNote < sim.notes.size() && sim.notes.get(autoNote).time <= t) {
            PeleaSim.Note n = sim.notes.get(autoNote++);
            if (!n.trap && n.judged == 0 && sim.phase == PeleaSim.TIRON_RITMO) {
                feedAt(n.time, n.key, true);
            }
        }
        // salto: Espacio justo en la marca
        if (sim.phase == PeleaSim.SALTO && !sim.jumpTried) {
            int target = sim.jumpTimes[sim.jumpIndex] + PeleaSim.JUMP_LEN;
            if (t >= target) {
                feedAt(target, PeleaSim.SPACE, true);
            }
        }
        // aguja al centro
        boolean wantA = sim.needle > 0.08F, wantD = sim.needle < -0.08F;
        if (wantA != held[PeleaSim.A]) {
            held[PeleaSim.A] = wantA;
            feed(PeleaSim.A, wantA);
        }
        if (wantD != held[PeleaSim.D]) {
            held[PeleaSim.D] = wantD;
            feed(PeleaSim.D, wantD);
        }
        if (sim.phase == PeleaSim.FINAL && t - autoMash >= 110) {
            autoMash = t;
            feed(autoUp ? PeleaSim.UP : PeleaSim.DOWN, true);
            autoUp = !autoUp;
        }
    }

    private void feedAt(int ms, int k, boolean down) {
        if (sim.result != PeleaSim.PELEANDO) {
            return;
        }
        if (!events.isEmpty() && ms < events.get(events.size() - 1)[0]) {
            ms = events.get(events.size() - 1)[0];
        }
        events.add(new int[] {ms, k, down ? 1 : 0});
        sim.input(ms, k, down);
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        if (sim.result == PeleaSim.PELEANDO) {
            if (autoplay) {
                autoPlay();
            }
            sim.advanceTo(nowMs());
        }
        feedback();
        if (sim.result != PeleaSim.PELEANDO && !sent) {
            sent = true;
            endShownAt = System.currentTimeMillis();
            int[] flat = new int[events.size() * 3];
            for (int i = 0; i < events.size(); i++) {
                System.arraycopy(events.get(i), 0, flat, i * 3, 3);
            }
            onEnd.accept(flat);
            if (sim.result == PeleaSim.PESCADO) {
                sound(SoundEvents.NOTE_BLOCK_BELL, 2F, 0.8F);
            } else {
                sound(SoundEvents.NOTE_BLOCK_BASS, 0.5F, 0.9F);
            }
        }
        if (endShownAt > 0 && System.currentTimeMillis() - endShownAt > 1600) {
            onClose();
            return;
        }
        shake = Math.max(0, shake - 0.06F);
        Theme th = Theme.MAR;
        g.push();
        if (shake > 0) {
            g.translate(Math.sin(System.nanoTime() / 2.0e7) * shake * 2, 0, 0);
        }
        Ui.panel(g, px, py, PW, PH, th, 1F);
        // título: el pez (sin nombre hasta sacarlo) y su rareza
        String title = sim.result == PeleaSim.PESCADO ? fishName.getString() : "???";
        g.drawString(Ui.font(), title, px + 10, py + 7, rareza.light, true);
        String rz = rareza.nombre().getString();
        g.drawString(Ui.font(), rz, px + PW - 10 - Ui.font().width(rz), py + 7, rareza.light, false);
        // hilo y energía
        int bw = 120;
        g.drawString(Ui.font(), "Hilo", px + 10, py + 20, th.textDim, false);
        Ui.bar(g, px + 36, py + 21, bw - 26, 6, sim.hp / sim.p.lineHp, sim.hp / sim.p.lineHp < 0.3F ? 0xFFFF7A6A : 0xFFE8F4FF,
                sim.hp / sim.p.lineHp < 0.3F ? 0xFFB02A2A : 0xFF6FA8C8, sim.now / 1000F);
        g.drawString(Ui.font(), "Pez", px + PW - 10 - bw, py + 20, th.textDim, false);
        Ui.bar(g, px + PW - 10 - bw + 22, py + 21, bw - 22, 6, sim.stamina / sim.p.stamina, 0xFFFFB860, 0xFFD0602A, sim.now / 1000F);
        drawNeedle(g, th);
        drawLane(g, th);
        if (sim.phase == PeleaSim.SALTO) {
            drawJump(g);
        }
        if (sim.phase == PeleaSim.FINAL) {
            drawFinal(g);
        }
        // racha y textos
        if (sim.combo >= 3) {
            String c = "x" + sim.combo;
            g.drawString(Ui.font(), c, px + PW - 12 - 4 * 15 - 8 - Ui.font().width(c), py + PH - 15, 0xFFFFE680, true);
        }
        long t = System.currentTimeMillis();
        for (Pop p : pops) {
            float k = (t - p.at()) / 700F;
            int y = py + 47 - (int) (k * 8);
            int w = Ui.font().width(p.text());
            g.drawString(Ui.font(), p.text(), p.x() - w / 2, y, Ui.alpha(p.color(), 1 - k * k), true);
        }
        if (endShownAt > 0) {
            drawResult(g);
        }
        g.pop();
    }

    /** La aguja del tirón con la zona verde y las teclas A y D a los lados. */
    private void drawNeedle(G g, Theme th) {
        int x0 = px + 34, x1 = px + PW - 34, y = py + 33, h = 10, cx = (x0 + x1) / 2, half = (x1 - x0) / 2;
        g.fill(x0 - 1, y - 1, x1 + 1, y + h + 1, 0xFF04121A);
        // rojo en las orillas, verde al centro
        int zw = Math.round(half * sim.zone());
        g.fillGradient(x0, y, x1, y + h, 0xFF5A1E1E, 0xFF3A1010);
        g.fillGradient(cx - zw, y, cx + zw, y + h, 0xFF3FAF5A, 0xFF1E7A38);
        g.fill(cx - zw, y, cx + zw, y + 1, 0x60FFFFFF);
        g.fill(cx, y + 2, cx + 1, y + h - 2, 0x60FFFFFF);
        // hacia dónde jala el pez
        float pull = sim.phase == PeleaSim.TIRON_RITMO ? sim.pullAt(sim.now) : 0;
        int arrows = Math.min(3, Math.round(Math.abs(pull) * 3.5F));
        for (int i = 0; i < arrows; i++) {
            int ax = pull > 0 ? cx + zw + 6 + i * 6 : cx - zw - 10 - i * 6;
            chevron(g, ax, y + 2, pull > 0, Ui.alpha(0xFFFFB860, 0.5F + 0.15F * i));
        }
        // aguja
        int nx = cx + Math.round(sim.needle * half);
        boolean ok = sim.inZone();
        g.fill(nx - 2, y - 3, nx + 2, y + h + 3, 0xFF04121A);
        g.fill(nx - 1, y - 2, nx + 1, y + h + 2, ok ? 0xFFFFFFFF : 0xFFFF6A6A);
        keyCap(g, x0 - 22, y - 2, "A", held[PeleaSim.A], th);
        keyCap(g, x1 + 8, y - 2, "D", held[PeleaSim.D], th);
    }

    private void chevron(G g, int x, int y, boolean right, int color) {
        for (int i = 0; i < 3; i++) {
            int dx = right ? i : 2 - i;
            g.fill(x + dx, y + i, x + dx + 2, y + i + 1, color);
            g.fill(x + dx, y + 5 - i, x + dx + 2, y + 6 - i, color);
        }
    }

    private void keyCap(G g, int x, int y, String label, boolean down, Theme th) {
        int dy = down ? 1 : 0;
        Ui.round(g, x, y + 1, 14, 14, 0x80000000);
        Ui.round(g, x, y + dy, 14, 13, down ? th.accent : 0xFF2A7088);
        g.fill(x + 1, y + dy + 1, x + 13, y + dy + 2, 0x40FFFFFF);
        g.drawString(Ui.font(), label, x + 7 - Ui.font().width(label) / 2, y + 3 + dy, down ? 0xFF04121A : 0xFFFFFFFF, false);
    }

    /** El carril de las flechas con la línea dorada. */
    private void drawLane(G g, Theme th) {
        int x0 = px + 12, x1 = px + PW - 12, y = py + 56, h = 28;
        int hitX = px + 46;
        Ui.inset(g, x0, y, x1 - x0, h, th);
        // marcas de compás
        int beat = 60000 / Math.max(30, sim.p.bpm);
        for (int k = -1; k < 12; k++) {
            int bt = (sim.now / beat + k) * beat;
            int bx = hitX + (int) ((bt - sim.now) / (float) sim.p.approachMs * (x1 - 10 - hitX));
            if (bx > x0 + 2 && bx < x1 - 2) {
                g.fill(bx, y + 3, bx + 1, y + h - 3, 0x18FFFFFF);
            }
        }
        // línea donde hay que apretar
        g.fill(hitX - 9, y + 2, hitX + 10, y + h - 2, 0x30FFE680);
        g.fill(hitX, y + 1, hitX + 1, y + h - 1, 0xFFFFE680);
        g.enableScissor(x0 + 1, y + 1, x1 - 1, y + h - 1);
        for (PeleaSim.Note n : sim.notes) {
            float f = (n.time - sim.now) / (float) sim.p.approachMs;
            if (f > 1.05F) {
                break;
            }
            if (f < -0.25F) {
                continue;
            }
            int nx = hitX + (int) (f * (x1 - 10 - hitX));
            float a = 1;
            if (n.judged == 1 || n.judged == 2) {
                // acertada: se hace grande y se va
                float k = Math.min(1, (sim.now - n.judgedAt) / 160F);
                if (k >= 1) {
                    continue;
                }
                a = 1 - k;
            } else if (n.judged == 3 || n.judged == 5) {
                a = 0.35F;
            } else if (n.judged == 4) {
                continue;
            }
            if (n.ghost && n.judged == 0 && f < 0.45F) {
                a *= Math.max(0, (f - 0.12F) / 0.33F);   // flechas fantasma: se borran antes de llegar
            }
            note(g, nx, y + h / 2, n, a);
        }
        g.disableScissor();
        // teclas de las flechas abajo a la derecha (se encienden al apretar)
        int kx = px + PW - 12 - 4 * 15, ky = py + PH - 18;
        int[] order = {PeleaSim.LEFT, PeleaSim.UP, PeleaSim.DOWN, PeleaSim.RIGHT};
        for (int i = 0; i < 4; i++) {
            if (sim.p.keys < 4 && (order[i] == PeleaSim.UP || order[i] == PeleaSim.DOWN)) {
                continue;
            }
            int bx = kx + i * 15;
            Ui.round(g, bx, ky, 13, 13, held[order[i]] ? KEY_COLOR[order[i]] : 0x60000000);
            arrow(g, bx + 3, ky + 3, order[i], held[order[i]] ? 0xFF04121A : Ui.alpha(KEY_COLOR[order[i]], 0.8F), 1);
        }
        if (sim.now < 6000 || sim.combo < 2) {
            g.drawString(Ui.font(), "A/D: aguja verde · ← ↑ ↓ →: flechas", px + 12, py + PH - 15, 0xFF8FB8C4, false);
        }
    }

    private void note(G g, int x, int cy, PeleaSim.Note n, float a) {
        int col = n.trap ? 0xFFE0524A : KEY_COLOR[n.key];
        int s = 16;
        int y = cy - s / 2;
        Ui.round(g, x - s / 2 - 1, y - 1, s + 2, s + 2, Ui.alpha(0xFF04121A, a));
        g.fillGradient(x - s / 2, y, x + s / 2, y + s, Ui.alpha(Ui.mix(col, 0xFFFFFFFF, 0.2F), a), Ui.alpha(Ui.mix(col, 0xFF000000, 0.25F), a));
        g.fill(x - s / 2 + 1, y + 1, x + s / 2 - 1, y + 2, Ui.alpha(0xFFFFFFFF, a * 0.4F));
        arrow(g, x - 4, y + 4, n.key, Ui.alpha(0xFF04121A, a), 1);
        if (n.trap) {
            // una X encima: no se aprieta
            for (int i = 0; i < 10; i++) {
                g.fill(x - 5 + i, y + 3 + i, x - 4 + i, y + 4 + i, Ui.alpha(0xFFFFFFFF, a * 0.85F));
                g.fill(x + 4 - i, y + 3 + i, x + 5 - i, y + 4 + i, Ui.alpha(0xFFFFFFFF, a * 0.85F));
            }
        }
    }

    /** Flecha de 7x7 (girada según la tecla). */
    private void arrow(G g, int x, int y, int key, int color, int scale) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (ARROW[r].charAt(c) != '#') {
                    continue;
                }
                int ax, ay;
                switch (key) {
                    case PeleaSim.DOWN -> { ax = c; ay = 6 - r; }
                    case PeleaSim.LEFT -> { ax = r; ay = c; }
                    case PeleaSim.RIGHT -> { ax = 6 - r; ay = c; }
                    default -> { ax = c; ay = r; }
                }
                g.fill(x + ax * scale, y + ay * scale, x + (ax + 1) * scale, y + (ay + 1) * scale, color);
            }
        }
    }

    /** El salto: el anillo se cierra hacia la marca; Espacio cuando la toca. */
    private void drawJump(G g) {
        float k = sim.jumpProgress();
        int cx = px + PW / 2, cy = py + 70;
        g.fill(px + 12, py + 56, px + PW - 12, py + 84, 0xC004121A);
        int target = 9;
        int r = target + Math.round(Math.max(0, 1 - k) * 34);
        ring(g, cx, cy, target, 0xFFFFE680);
        ring(g, cx, cy, target + 1, 0xFFFFE680);
        ring(g, cx, cy, r, k <= 1 ? 0xFF7CF29A : 0xFFFF7A6A);
        ring(g, cx, cy, r + 1, k <= 1 ? 0xFF7CF29A : 0xFFFF7A6A);
        String s = "¡Salta! ESPACIO";
        g.drawString(Ui.font(), s, cx - Ui.font().width(s) / 2 + 60, cy - 4, 0xFFFFFFFF, true);
    }

    private void ring(G g, int cx, int cy, int r, int color) {
        int n = Math.max(12, r * 6);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    /** El final: barra que se llena alternando ↑ y ↓. */
    private void drawFinal(G g) {
        int x0 = px + 12, y = py + 56;
        g.fill(x0, y, px + PW - 12, y + 28, 0xD004121A);
        String s = "¡Sácalo! alterna ↑ ↓";
        g.drawString(Ui.font(), s, px + PW / 2 - Ui.font().width(s) / 2, y + 3, 0xFFFFE680, true);
        Ui.bar(g, x0 + 20, y + 16, PW - 64, 7, sim.reel, 0xFF7CF29A, 0xFF2E8B3A, sim.now / 1000F);
        boolean upNext = sim.lastMashKey != 0;
        arrow(g, px + PW - 34, y + 16, PeleaSim.UP, upNext ? 0xFFFFFFFF : 0x60FFFFFF, 1);
        arrow(g, px + PW - 24, y + 16, PeleaSim.DOWN, !upNext ? 0xFFFFFFFF : 0x60FFFFFF, 1);
    }

    private void drawResult(G g) {
        String s;
        int c;
        switch (sim.result) {
            case PeleaSim.PESCADO -> {
                s = sim.flawless() ? "¡Perfecto! Lo sacaste sin errores" : "¡Lo sacaste!";
                c = 0xFF7CF29A;
            }
            case PeleaSim.HILO_ROTO -> {
                s = "Se rompió el hilo";
                c = 0xFFFF7A6A;
            }
            default -> {
                s = "Se escapó";
                c = 0xFFFFB860;
            }
        }
        float k = Math.min(1, (System.currentTimeMillis() - endShownAt) / 200F);
        int w = Ui.font().width(s) + 20;
        int x = px + (PW - w) / 2, y = py - 22;
        g.fill(x, y, x + w, y + 16, Ui.alpha(0xE004121A, k));
        g.fill(x, y, x + w, y + 1, Ui.alpha(c, k));
        g.drawString(Ui.font(), s, x + 10, y + 4, Ui.alpha(c, k), true);
    }
}
