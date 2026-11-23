package com.axeliscore.ui;

/**
 * Colores de la interfaz (ARGB). Cada mod puede usar uno de los listos o crear el suyo con {@link #of}.
 */
public final class Theme {
    public final int bgTop, bgBottom, border, borderLight, accent, accent2, text, textDim, title, shadow, button, buttonHover, buttonText,
            good, bad;
    /** Marco con textura alrededor de los paneles (null = el borde dibujado de siempre). Ver {@link Ui#frame}. */
    public final net.minecraft.resources.ResourceLocation frame;

    public Theme(int bgTop, int bgBottom, int border, int borderLight, int accent, int accent2, int text, int textDim, int title, int shadow,
            int button, int buttonHover, int buttonText, int good, int bad) {
        this(bgTop, bgBottom, border, borderLight, accent, accent2, text, textDim, title, shadow, button, buttonHover, buttonText, good, bad,
                null);
    }

    public Theme(int bgTop, int bgBottom, int border, int borderLight, int accent, int accent2, int text, int textDim, int title, int shadow,
            int button, int buttonHover, int buttonText, int good, int bad, net.minecraft.resources.ResourceLocation frame) {
        this.frame = frame;
        this.bgTop = bgTop;
        this.bgBottom = bgBottom;
        this.border = border;
        this.borderLight = borderLight;
        this.accent = accent;
        this.accent2 = accent2;
        this.text = text;
        this.textDim = textDim;
        this.title = title;
        this.shadow = shadow;
        this.button = button;
        this.buttonHover = buttonHover;
        this.buttonText = buttonText;
        this.good = good;
        this.bad = bad;
    }

    /** Noche: azul oscuro con detalles dorados (la de siempre del Core). */
    public static final Theme NOCHE = new Theme(0xF0202638, 0xF0141824, 0xFF07090F, 0xFF4A5677, 0xFFE8B84A, 0xFF5FD0FF, 0xFFE8ECF6,
            0xFF9AA3BC, 0xFFFFE6A0, 0x80000000, 0xFF323C58, 0xFF46557E, 0xFFFFFFFF, 0xFF6EE07A, 0xFFFF6A6A, marco("noche"));
    /** Pergamino: papel claro y tinta café (diálogos, libros, mapas). */
    public static final Theme PERGAMINO = new Theme(0xF5F2E4C4, 0xF5E2CC9C, 0xFF5A3A1C, 0xFFFFF6DA, 0xFFB5651D, 0xFF2E7D4F, 0xFF3A2614,
            0xFF7A5A3A, 0xFF5A2A08, 0x70000000, 0xFFD8B880, 0xFFE8CC98, 0xFF3A2614, 0xFF2E7D32, 0xFFB71C1C, marco("pergamino"));
    /** Mar: turquesa y espuma (pesca, agua). */
    public static final Theme MAR = new Theme(0xF0123A4A, 0xF00A2230, 0xFF04121A, 0xFF3E8FA3, 0xFF7FF0E0, 0xFFFFD166, 0xFFEAFBFF,
            0xFF8FB8C4, 0xFFB8FFF4, 0x80000000, 0xFF1D5568, 0xFF2A7088, 0xFFFFFFFF, 0xFF7CF29A, 0xFFFF7B7B, marco("mar"));
    /** Madera: café cálido con clavos de latón (talleres, tiendas, muelles). */
    public static final Theme MADERA = new Theme(0xF23A2A1C, 0xF2241910, 0xFF140C05, 0xFF7A5634, 0xFFF0C060, 0xFF9BE08A, 0xFFF6EBDD,
            0xFFBFA88C, 0xFFFFDE9A, 0x80000000, 0xFF5A3F26, 0xFF74522F, 0xFFFFFFFF, 0xFF8EE07A, 0xFFFF7A6A, marco("madera"));
    /** Piedra: gris con musgo (mazmorras, ruinas). */
    public static final Theme PIEDRA = new Theme(0xF02E3034, 0xF01C1D20, 0xFF0C0C0E, 0xFF6A6C72, 0xFF9FD86E, 0xFFFFD166, 0xFFEDEEF0,
            0xFFA4A7AE, 0xFFD8F5B8, 0x80000000, 0xFF44474E, 0xFF585C66, 0xFFFFFFFF, 0xFF8EE07A, 0xFFFF7A6A, marco("piedra"));

    private static net.minecraft.resources.ResourceLocation marco(String name) {
        return com.axeliscore.plat.Ids.core("textures/gui/marco_" + name + ".png");
    }

    /** El mismo tema con otro marco (null = sin marco). */
    public Theme withFrame(net.minecraft.resources.ResourceLocation f) {
        return new Theme(bgTop, bgBottom, border, borderLight, accent, accent2, text, textDim, title, shadow, button, buttonHover, buttonText,
                good, bad, f);
    }

    /** Un tema a partir de un color de fondo y uno de acento (lo demás se calcula). */
    public static Theme of(int bg, int accent) {
        int top = Ui.mix(bg, 0xFFFFFFFF, 0.06F) | 0xF0000000, bottom = Ui.mix(bg, 0xFF000000, 0.25F) | 0xF0000000;
        return new Theme(top, bottom, Ui.mix(bg, 0xFF000000, 0.7F), Ui.mix(bg, 0xFFFFFFFF, 0.3F), accent, Ui.mix(accent, 0xFFFFFFFF, 0.4F),
                0xFFF2F2F2, 0xFFA8A8A8, Ui.mix(accent, 0xFFFFFFFF, 0.5F), 0x80000000, Ui.mix(bg, 0xFFFFFFFF, 0.15F),
                Ui.mix(bg, 0xFFFFFFFF, 0.28F), 0xFFFFFFFF, 0xFF6EE07A, 0xFFFF6A6A);
    }
}
