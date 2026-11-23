package com.pescacoleccion.pesca;

import com.axeliscore.item.CoreItem;
import com.axeliscore.plat.ItemTag;
import com.axeliscore.plat.Nbt;
import com.axeliscore.ui.InfoCard;
import com.axeliscore.ui.Rareza;
import com.pescacoleccion.PescaColeccion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/** Un pez de la colección. Cada ejemplar guarda su tamaño, su peso, quién lo pescó y si fue récord del mundo. */
public class PezItem extends CoreItem {
    /** (Cliente) Estrellas de maestría que tiene el jugador en esta especie; lo pone el cliente al arrancar. */
    public static ToIntFunction<String> estrellas = id -> -1;

    public final String especie;

    public PezItem(Properties props, String especie) {
        super(props);
        this.especie = especie;
    }

    public Especie especie() {
        return Especies.get(especie);
    }

    /** Un ejemplar pescado. */
    public static ItemStack crear(net.minecraft.world.item.Item item, Especie e, float cm, String por, boolean record) {
        ItemStack st = new ItemStack(item);
        ItemTag.edit(st, t -> {
            t.putFloat("cm", cm);
            t.putFloat("kg", e.peso(cm));
            t.putString("por", por);
            if (record) {
                t.putBoolean("rec", true);
            }
        });
        return st;
    }

    /** El tamaño de un ejemplar (0 si no tiene). */
    public static float cm(ItemStack st) {
        return Nbt.f(ItemTag.get(st), "cm");
    }

    /** ¿Es de los grandes de su especie (la cuarta parte más grande)? */
    public static boolean grande(ItemStack st, Especie e) {
        return cm(st) >= e.cmMin() + (e.cmMax() - e.cmMin()) * 0.75F;
    }

    private static String num(float v) {
        return v >= 100 ? String.format(Locale.ROOT, "%.0f", v) : String.format(Locale.ROOT, "%.1f", v);
    }

    /** "Ríos · De noche · Luna llena" */
    public static Component donde(Especie e) {
        StringBuilder sb = new StringBuilder();
        sb.append(Component.translatable(PescaColeccion.MODID + ".lugar." + e.lugar().get(0)).getString());
        sb.append(" · ").append(Component.translatable(PescaColeccion.MODID + ".hora." + e.hora().get(0)).getString());
        if (!"cualquiera".equals(e.clima())) {
            sb.append(" · ").append(Component.translatable(PescaColeccion.MODID + ".clima." + e.clima()).getString());
        }
        if (!"cualquiera".equals(e.luna())) {
            sb.append(" · ").append(Component.translatable(PescaColeccion.MODID + ".luna." + e.luna()).getString());
        }
        return Component.literal(sb.toString());
    }

    @Override
    public InfoCard card(ItemStack stack, boolean detail) {
        Especie e = especie();
        if (e == null) {
            return null;
        }
        Rareza r = Rareza.of(e.rareza());
        InfoCard c = new InfoCard(stack.getHoverName()).rareza(r).icon(stack).subtitle(donde(e)).width(190);
        CompoundTag t = ItemTag.get(stack);
        float cm = Nbt.f(t, "cm");
        if (cm > 0) {
            float rel = (cm - e.cmMin()) / Math.max(1, e.cmMax() - e.cmMin());
            c.bar(Component.translatable(PescaColeccion.MODID + ".pez.tamano"), rel, r.light, num(cm) + " cm");
            c.pair(Component.translatable(PescaColeccion.MODID + ".pez.peso"), Component.literal(num(Nbt.f(t, "kg")) + " kg"));
            String por = Nbt.s(t, "por");
            if (!por.isEmpty()) {
                c.pair(Component.translatable(PescaColeccion.MODID + ".pez.por"), Component.literal(por));
            }
            if (Nbt.b(t, "rec")) {
                c.text(Component.translatable(PescaColeccion.MODID + ".pez.record"), 0xFFFFD24A);
            }
        } else {
            c.pair(Component.translatable(PescaColeccion.MODID + ".pez.tamanos"), Component.literal(num(e.cmMin()) + " – " + num(e.cmMax()) + " cm"));
        }
        int st = estrellas.applyAsInt(e.id());
        if (st >= 0) {
            c.stars(Component.translatable(PescaColeccion.MODID + ".pez.maestria"), Integer.bitCount(st & 7), 3);
        }
        if (detail) {
            c.divider().text(Component.translatable(PescaColeccion.MODID + ".pez." + e.id() + ".historia"));
        } else {
            c.hint(Component.translatable("axelis_core.info.mas"));
        }
        return c;
    }

    @Override
    protected void tooltip(ItemStack stack, Consumer<Component> add) {
        float cm = Nbt.f(ItemTag.get(stack), "cm");
        if (cm > 0) {
            add.accept(Component.literal(num(cm) + " cm"));
        }
    }
}
