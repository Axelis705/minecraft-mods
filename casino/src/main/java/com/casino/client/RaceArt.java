package com.casino.client;

import com.casino.game.HorseRace;
import com.casino.plat.Ids;
import com.casino.plat.client.G;
import net.minecraft.resources.ResourceLocation;

/** Colores y dibujos compartidos por la pantalla gigante, el menú de apuestas y el aviso en pantalla. */
public final class RaceArt {
    public static final ResourceLocation HORSE = Ids.casino("textures/entity/race_horse.png");
    public static final ResourceLocation SCENE = Ids.casino("textures/entity/race_scene.png");
    public static final int FW = 64;
    public static final int FH = 44;
    public static final int SHEET_W = 512;
    public static final int SHEET_H = 440;

    /** Pelajes: castaño, alazán, negro, tordo, palomino, castaño oscuro. */
    public static final int[] COAT = {0xFF82461F, 0xFFAC5C2C, 0xFF2E2828, 0xFFDEDED8, 0xFFDEB060, 0xFF563620};
    /** Sedas de los jinetes. */
    public static final int[] SILK = {0xFFDC2828, 0xFF285ADC, 0xFF28AA46, 0xFFF0D228, 0xFFF0F0F0, 0xFF1E1E22, 0xFFF0821E, 0xFF8C3CC8,
            0xFFF06EAA, 0xFF28C8D2, 0xFF96DC28, 0xFF141E5A};

    private RaceArt() {
    }

    /** Capa de la hoja de sprites: 0 base, 1 pelaje, 2-5 seda A por dibujo, 6-9 seda B por dibujo. */
    public static int layerV(int layer) {
        return layer * FH;
    }

    public static int silkA(HorseRace r, int i) {
        return SILK[r.silk1[i] % SILK.length];
    }

    public static int silkB(HorseRace r, int i) {
        return SILK[r.silk2[i] % SILK.length];
    }

    /** Si sobre ese color se lee mejor texto negro. */
    public static boolean light(int argb) {
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        return r * 0.3 + g * 0.59 + b * 0.11 > 150;
    }

    /** Dibuja al caballo i en un menú (x, y = esquina; w × h en píxeles de la interfaz). */
    public static void gui(G g, HorseRace r, int i, int frame, int x, int y, int w, int h) {
        int u = (frame & 7) * FW;
        int p = r.pattern[i] % 4;
        g.blitTint(HORSE, x, y, w, h, u, layerV(1), FW, FH, SHEET_W, SHEET_H, COAT[r.coat[i] % COAT.length]);
        g.blitTint(HORSE, x, y, w, h, u, layerV(2 + p), FW, FH, SHEET_W, SHEET_H, silkA(r, i));
        g.blitTint(HORSE, x, y, w, h, u, layerV(6 + p), FW, FH, SHEET_W, SHEET_H, silkB(r, i));
        g.blitTint(HORSE, x, y, w, h, u, layerV(0), FW, FH, SHEET_W, SHEET_H, 0xFFFFFFFF);
    }

    /** Insignia con el número del caballo (fondo de su seda). */
    public static void badge(G g, net.minecraft.client.gui.Font font, HorseRace r, int i, int x, int y, int size) {
        int a = silkA(r, i);
        int b = silkB(r, i);
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
        g.fill(x, y, x + size, y + size, a);
        g.fill(x, y + size - 3, x + size, y + size, b);
        String n = String.valueOf(i + 1);
        g.drawString(font, n, x + (size - font.width(n)) / 2 + 1, y + (size - 8) / 2, light(a) ? 0xFF101010 : 0xFFFFFFFF, false);
    }

    public static String stars(int n) {
        StringBuilder s = new StringBuilder();
        for (int k = 0; k < 5; k++) {
            s.append(k < n ? '★' : '☆');
        }
        return s.toString();
    }

    public static String time(int ticks) {
        int s = (ticks + 19) / 20;
        return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
    }
}
