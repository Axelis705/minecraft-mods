package com.pescacoleccion.torneo;

import com.axeliscore.plat.ItemTag;
import com.axeliscore.plat.Nbt;
import com.axeliscore.ui.InfoCard;
import com.axeliscore.ui.Rareza;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Las copas de torneo: cómo se crean (con su ganador) y su cuadro de información. */
public final class Copas {
    private static final String MOD = PescaColeccion.MODID;
    private static final Rareza[] COLOR = {Rareza.LEGENDARIO, Rareza.COMUN, Rareza.EPICO};

    private Copas() {
    }

    /** Una copa del lugar {@code lugar} (0 oro, 1 plata, 2 bronce) con quién la ganó, el modo, lo que hizo y el día. */
    public static ItemStack crear(int lugar, String ganador, String modo, String marca, long dia, int jugadores) {
        ItemStack st = new ItemStack(switch (lugar) {
            case 0 -> ModRegistry.COPA_ORO_ITEM.get();
            case 1 -> ModRegistry.COPA_PLATA_ITEM.get();
            default -> ModRegistry.COPA_BRONCE_ITEM.get();
        });
        ItemTag.edit(st, t -> {
            t.putString("g", ganador);
            t.putString("m", modo);
            t.putString("p", marca);
            t.putLong("d", dia);
            t.putInt("j", jugadores);
        });
        return st;
    }

    /** Llena el cuadro de una copa. */
    public static void llenar(InfoCard card, CompoundTag info, int lugar) {
        card.rareza(COLOR[Math.max(0, Math.min(2, lugar))]);
        card.subtitle(Component.translatable(MOD + ".copa.lugar." + lugar));
        String g = Nbt.s(info, "g");
        if (g.isEmpty()) {
            card.text(Component.translatable(MOD + ".copa.sin_dueno"));
            return;
        }
        card.pair(Component.translatable(MOD + ".copa.ganador"), Component.literal(g));
        card.pair(Component.translatable(MOD + ".copa.modo"), Component.translatable(MOD + ".torneo.modo." + Nbt.s(info, "m")));
        card.pair(Component.translatable(MOD + ".copa.marca"), Component.literal(Nbt.s(info, "p")));
        card.pair(Component.translatable(MOD + ".copa.dia"), Component.literal(String.valueOf(Nbt.l(info, "d") + 1)));
        card.pair(Component.translatable(MOD + ".copa.jugadores"), Component.literal(String.valueOf(Nbt.i(info, "j"))));
    }

    /** Cuadro del objeto copa (en el inventario). */
    public static InfoCard card(ItemStack st, int lugar) {
        InfoCard c = new InfoCard(st.getHoverName()).icon(st);
        llenar(c, ItemTag.get(st), lugar);
        return c;
    }
}
