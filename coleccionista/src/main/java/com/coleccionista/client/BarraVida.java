package com.coleccionista.client;

import com.axeliscore.plat.client.G;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;

/**
 * (Cliente) La barra de abajo con el estilo del evento (solo para las piezas, en supervivencia o aventura): corazones de porcelana con la
 * sangre adentro (los vacíos, rotos), panes duros con moho en lugar de la comida, una franja oscura y oxidada con manchitas de sangre, y sin
 * la barra de experiencia (en el evento no sirve). Los corazones tiemblan y destellan al recibir daño y laten cuando queda poca vida.
 */
public final class BarraVida {
    /** Corazón de 9x8 (X = lleno). */
    private static final String[] CORAZON = {
            ".XX...XX.",
            "XXXX.XXXX",
            "XXXXXXXXX",
            "XXXXXXXXX",
            ".XXXXXXX.",
            "..XXXXX..",
            "...XXX...",
            "....X...."};
    /** Pan duro de 9x7. */
    private static final String[] PAN = {
            "..XXXXX..",
            ".XXXXXXX.",
            "XXXXXXXXX",
            "XXXXXXXXX",
            "XXXXXXXXX",
            ".XXXXXXX.",
            "..XXXXX.."};
    private static float vidaAntes = -1;
    private static long golpe;

    private BarraVida() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(BarraVida::ocultar);
        com.axeliscore.fx.client.Hud.add(BarraVida::dibujar);
    }

    /** ¿Se dibuja la barra del evento? (soy pieza, el HUD del evento se ve y puedo recibir daño) */
    private static boolean activa() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.gameMode != null && mc.gameMode.canHurtPlayer() && !mc.options.hideGui && EventoCliente.yo() != null
                && EventoHud.visible();
    }

    private static void ocultar(RenderGuiOverlayEvent.Pre e) {
        if (!activa()) {
            return;
        }
        var id = e.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id()) || id.equals(VanillaGuiOverlay.FOOD_LEVEL.id())
                || id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id()) || id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id())) {
            e.setCanceled(true);
        }
    }

    private static void dibujar(G g, int w, int h) {
        if (!activa()) {
            vidaAntes = -1;
            return;
        }
        LocalPlayer p = Minecraft.getInstance().player;
        long now = System.currentTimeMillis();
        float vida = p.getHealth(), max = Math.max(1, p.getMaxHealth());
        if (vidaAntes >= 0 && vida < vidaAntes - 0.01F) {
            golpe = now;
        }
        vidaAntes = vida;
        float tGolpe = (now - golpe) / 1000F;
        boolean temblando = tGolpe < 0.45F;
        int izq = w / 2 - 91, der = w / 2 + 91, y = h - 39;
        // la franja oscura y oxidada detrás, con manchitas de sangre
        g.fillGradient(izq - 3, y - 3, der + 3, y + 11, 0x00000000, 0x88100808);
        g.fill(izq - 3, y + 10, der + 3, y + 11, 0x704A2A1C);
        mancha(g, izq + 12, y + 11, 3, 0xA0601014);
        mancha(g, izq + 61, y + 12, 2, 0x90701016);
        mancha(g, der - 30, y + 11, 3, 0x90601014);
        mancha(g, w / 2 + 4, y + 13, 2, 0x70501012);
        // gotitas que escurren de la franja
        g.fill(izq + 13, y + 11, izq + 14, y + 15, 0x90601014);
        g.fill(der - 29, y + 11, der - 28, y + 14, 0x80601014);
        // ---- los corazones
        int lleno = 0xFFB0141E, brillo = 0xFFE04048, borde = 0xFF2A0A0C;
        if (p.hasEffect(MobEffects.POISON)) {
            lleno = 0xFF5E8A1E;
            brillo = 0xFF9ACD40;
        } else if (p.hasEffect(MobEffects.WITHER)) {
            lleno = 0xFF201A1A;
            brillo = 0xFF4A4040;
        } else if (p.isFullyFrozen()) {
            lleno = 0xFF6AA8D8;
            brillo = 0xFFB8E0FF;
        }
        int total = Mth.ceil(max / 2F);
        float latido = vida <= 6 ? (float) Math.max(0, Math.sin(now / (vida <= 3 ? 90.0 : 150.0))) : 0F;
        for (int i = 0; i < Math.min(10, total); i++) {
            int x = izq + i * 8;
            int yy = y - (int) (latido * (i % 2 == 0 ? 1.5F : 1F));
            if (temblando) {
                x += (int) (Math.sin(now / 25.0 + i * 1.7) * 1.5);
                yy += (int) (Math.cos(now / 31.0 + i) * 1.2);
            }
            float v = vida - i * 2;
            int estado = v >= 2 ? 2 : v >= 1 ? 1 : 0;
            corazon(g, x, yy, estado, lleno, brillo, borde, i);
            if (temblando && tGolpe < 0.18F) {
                figura(g, CORAZON, x, yy, 0x90FFFFFF, 9);
            }
        }
        // ---- los panes duros (la comida), de derecha a izquierda como siempre
        int comida = p.getFoodData().getFoodLevel();
        boolean hambre = p.hasEffect(MobEffects.HUNGER);
        for (int i = 0; i < 10; i++) {
            int x = der - i * 8 - 9;
            int yy = y + 1;
            if (comida <= 6 && (now / 120 + i) % 10 == 0) {
                yy += 1; // con hambre tiemblan apenitas
            }
            int v = comida - i * 2;
            pan(g, x, yy, v >= 2 ? 2 : v >= 1 ? 1 : 0, hambre, i);
        }
    }

    /** Un corazón de porcelana: lleno de sangre, a medias o vacío y roto. */
    private static void corazon(G g, int x, int y, int estado, int lleno, int brillo, int borde, int semilla) {
        for (int r = 0; r < CORAZON.length; r++) {
            for (int c = 0; c < 9; c++) {
                if (CORAZON[r].charAt(c) != 'X') {
                    continue;
                }
                boolean orilla = esOrilla(CORAZON, r, c);
                int col;
                if (orilla) {
                    col = 0xFFD8CFC2; // la porcelana
                } else if (estado == 2 || estado == 1 && c <= 4) {
                    col = r == 1 && (c == 1 || c == 2) || r == 2 && c == 1 ? brillo : lleno;
                } else {
                    col = 0xFF2C2224;
                }
                g.fill(x + c, y + r, x + c + 1, y + r + 1, col);
            }
        }
        // sombra abajo de la porcelana
        g.fill(x + 3, y + 7, x + 6, y + 8, borde);
        if (estado == 0) {
            // grieta en el corazón vacío
            int k = semilla % 3;
            g.fill(x + 4 + (k == 1 ? 1 : 0), y + 1, x + 5 + (k == 1 ? 1 : 0), y + 3, 0xFF8A7E74);
            g.fill(x + 3 + k % 2, y + 3, x + 4 + k % 2, y + 5, 0xFF8A7E74);
        }
    }

    /** Un pan duro (lleno, mordido o solo la sombra), con manchitas de moho. */
    private static void pan(G g, int x, int y, int estado, boolean hambre, int semilla) {
        for (int r = 0; r < PAN.length; r++) {
            for (int c = 0; c < 9; c++) {
                if (PAN[r].charAt(c) != 'X') {
                    continue;
                }
                boolean orilla = esOrilla(PAN, r, c);
                boolean hay = estado == 2 || estado == 1 && c >= 4;
                int col;
                if (!hay) {
                    col = orilla ? 0xFF3A3030 : 0x00000000;
                } else if (orilla) {
                    col = hambre ? 0xFF3E4A1E : 0xFF4A2E14;
                } else {
                    col = r <= 1 ? (hambre ? 0xFF7A8A3A : 0xFFB48A52) : (hambre ? 0xFF5E6E2A : 0xFF946A3A);
                }
                if (col != 0) {
                    g.fill(x + c, y + r, x + c + 1, y + r + 1, col);
                }
            }
        }
        if (estado > 0) {
            // moho: dos puntitos verdosos que cambian de lugar por pan
            int a = 2 + semilla % 4, b = 4 + (semilla * 3) % 3;
            if (estado == 2 || a >= 4) {
                g.fill(x + a, y + 3, x + a + 1, y + 4, 0xFF4E5E2A);
            }
            g.fill(x + b, y + 4, x + b + 1, y + 5, 0xFF3E4E22);
        }
    }

    private static boolean esOrilla(String[] m, int r, int c) {
        int[][] d = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] k : d) {
            int rr = r + k[0], cc = c + k[1];
            if (rr < 0 || rr >= m.length || cc < 0 || cc >= m[rr].length() || m[rr].charAt(cc) != 'X') {
                return true;
            }
        }
        return false;
    }

    private static void figura(G g, String[] m, int x, int y, int col, int ancho) {
        for (int r = 0; r < m.length; r++) {
            for (int c = 0; c < ancho; c++) {
                if (m[r].charAt(c) == 'X') {
                    g.fill(x + c, y + r, x + c + 1, y + r + 1, col);
                }
            }
        }
    }

    /** Una manchita de sangre seca (un círculo irregular). */
    private static void mancha(G g, int cx, int cy, int r, int col) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)) * (dy % 2 == 0 ? 1 : 0.8));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, col);
        }
        g.fill(cx + r + 1, cy - 1, cx + r + 2, cy, col);
        g.fill(cx - r - 2, cy + 1, cx - r - 1, cy + 2, col);
    }
}
