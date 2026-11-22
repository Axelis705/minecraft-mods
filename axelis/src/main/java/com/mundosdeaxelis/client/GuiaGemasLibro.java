package com.mundosdeaxelis.client;

import com.mundosdeaxelis.gema.Gemas;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/** Etapa 286: las páginas de la Guía del Gemista (en el idioma del jugador) en el libro de Minecraft. */
public final class GuiaGemasLibro {
    private GuiaGemasLibro() {
    }

    /** Páginas de texto «libro.mundos_de_axelis.gemas.N» (título en dorado y el resto normal). */
    public static final int PAGINAS_TEXTO = 7;

    public static List<Component> paginas() {
        List<Component> out = new ArrayList<>();
        String k = "libro.mundos_de_axelis.gemas.";
        for (int i = 1; i <= PAGINAS_TEXTO; i++) {
            out.add(Component.translatable(k + i + ".titulo").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                    .append(Component.literal("\n\n").withStyle(Style.EMPTY))
                    .append(Component.translatable(k + i).withStyle(Style.EMPTY.withColor(ChatFormatting.BLACK).withBold(false))));
        }
        // los ocho elementos, con su color: qué hacen en el arma y en la armadura (dos por página)
        Gemas.Elemento[] els = Gemas.Elemento.values();
        for (int i = 0; i < els.length; i += 2) {
            MutableComponent p = Component.empty();
            for (int j = i; j < Math.min(els.length, i + 2); j++) {
                Gemas.Elemento e = els[j];
                if (j > i) {
                    p.append(Component.literal("\n\n"));
                }
                p.append(Component.translatable("item.mundos_de_axelis.gema_" + e.id).withStyle(Style.EMPTY.withColor(oscuro(e.color)).withBold(true)))
                        .append(Component.literal(" (" + e.tier + ")\n").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.translatable("gema.mundos_de_axelis.corto." + e.id + ".arma").withStyle(ChatFormatting.BLACK))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gema.mundos_de_axelis.corto." + e.id + ".armadura").withStyle(ChatFormatting.DARK_BLUE));
            }
            out.add(p);
        }
        out.add(Component.translatable(k + "fin.titulo").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("\n\n")).append(Component.translatable(k + "fin").withStyle(ChatFormatting.BLACK)));
        return out;
    }

    /** El color del elemento, más oscuro (para que se lea sobre el papel). */
    private static int oscuro(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        return ((r * 6 / 10) << 16) | ((g * 6 / 10) << 8) | (b * 6 / 10);
    }

    public static void abrir() {
        List<Component> ps = paginas();
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess() {
            @Override
            public int getPageCount() {
                return ps.size();
            }

            @Override
            public FormattedText getPageRaw(int i) {
                return i >= 0 && i < ps.size() ? ps.get(i) : FormattedText.EMPTY;
            }
        }));
    }
}
