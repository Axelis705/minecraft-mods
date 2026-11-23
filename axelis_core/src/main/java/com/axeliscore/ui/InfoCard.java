package com.axeliscore.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Un cuadro de información: lo que se ve al pasar el ratón sobre un objeto, al mirar un personaje en el mundo o en las pantallas.
 * Solo guarda los datos (se puede armar en código común); lo dibuja {@code ui.client.Cards}.
 * <pre>
 *     InfoCard c = new InfoCard(Component.literal("Trucha arcoíris")).rareza(Rareza.RARO).icon(stack)
 *             .subtitle(Component.literal("Ríos · de día"))
 *             .text(Component.literal("Salta contra la corriente."))
 *             .bar(Component.literal("Fuerza"), 0.6F, 0xFF6FB7FF, "6/10")
 *             .stars(2, 3)
 *             .hint(Component.literal("Shift: más información"));
 * </pre>
 */
public final class InfoCard {
    public enum Kind {
        TEXT, BAR, PAIR, DIVIDER, STARS, HINT
    }

    /** Una parte del cuadro. */
    public static final class Part {
        public final Kind kind;
        public final Component a, b;
        public final float value;
        public final int color, n, max;
        public final String valueText;

        Part(Kind kind, Component a, Component b, float value, int color, int n, int max, String valueText) {
            this.kind = kind;
            this.a = a;
            this.b = b;
            this.value = value;
            this.color = color;
            this.n = n;
            this.max = max;
            this.valueText = valueText;
        }
    }

    public Component title;
    public Component subtitle;
    public Rareza rareza;
    /** Etiqueta al lado del título (por ejemplo "Jefe"); si es null se usa el nombre de la rareza. */
    public Component tag;
    public ItemStack icon = ItemStack.EMPTY;
    /** Dibujo en vez de objeto (textura cuadrada). */
    public ResourceLocation image;
    public int imageSize = 16;
    /** Color del borde y del título si no hay rareza. */
    public int color = 0xFFE8B84A;
    public final List<Part> parts = new ArrayList<>();
    /** Ancho máximo del texto. */
    public int maxWidth = 200;

    public InfoCard(Component title) {
        this.title = title;
    }

    public InfoCard subtitle(Component s) {
        this.subtitle = s;
        return this;
    }

    public InfoCard rareza(Rareza r) {
        this.rareza = r;
        if (r != null) {
            this.color = r.light;
        }
        return this;
    }

    public InfoCard tag(Component t) {
        this.tag = t;
        return this;
    }

    public InfoCard color(int argb) {
        this.color = argb;
        return this;
    }

    public InfoCard icon(ItemStack st) {
        this.icon = st == null ? ItemStack.EMPTY : st;
        return this;
    }

    public InfoCard image(ResourceLocation tex, int size) {
        this.image = tex;
        this.imageSize = size;
        return this;
    }

    public InfoCard width(int max) {
        this.maxWidth = max;
        return this;
    }

    /** Párrafo (se corta en renglones solo). */
    public InfoCard text(Component c) {
        parts.add(new Part(Kind.TEXT, c, null, 0, 0, 0, 0, null));
        return this;
    }

    public InfoCard text(Component c, int argb) {
        parts.add(new Part(Kind.TEXT, c, null, 0, argb, 0, 0, null));
        return this;
    }

    /** Barra con nombre, de 0 a 1, y el número a la derecha. */
    public InfoCard bar(Component label, float frac, int argb, String valueText) {
        parts.add(new Part(Kind.BAR, label, null, frac, argb, 0, 0, valueText));
        return this;
    }

    /** "Nombre ........ valor". */
    public InfoCard pair(Component label, Component value) {
        parts.add(new Part(Kind.PAIR, label, value, 0, 0, 0, 0, null));
        return this;
    }

    public InfoCard divider() {
        parts.add(new Part(Kind.DIVIDER, null, null, 0, 0, 0, 0, null));
        return this;
    }

    /** Estrellas: {@code n} de {@code max} encendidas (con texto opcional al lado). */
    public InfoCard stars(int n, int max) {
        parts.add(new Part(Kind.STARS, null, null, 0, 0, n, max, null));
        return this;
    }

    public InfoCard stars(Component label, int n, int max) {
        parts.add(new Part(Kind.STARS, label, null, 0, 0, n, max, null));
        return this;
    }

    /** Pista abajo, en gris (por ejemplo "Shift: más información" o "Clic derecho: hablar"). */
    public InfoCard hint(Component c) {
        parts.add(new Part(Kind.HINT, c, null, 0, 0, 0, 0, null));
        return this;
    }

    /** Color del borde (el de la rareza si tiene). */
    public int border() {
        return rareza != null ? rareza.light : color;
    }

    public boolean isEmpty() {
        return title == null && parts.isEmpty();
    }
}
