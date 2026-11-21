package com.mundosdeaxelis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.ArrayList;
import java.util.List;

/** Abre un pergamino de la crónica como un libro (la pantalla de libro de Minecraft): título y páginas salen de las traducciones. */
public final class LoreClient {
    private LoreClient() {
    }

    public static void open(String id) {
        String base = "lore.mundos_de_axelis." + id;
        List<FormattedText> pages = new ArrayList<>();
        for (int i = 1; i <= 6 && I18n.exists(base + ".p" + i); i++) {
            Component page = i == 1
                    ? Component.empty().append(Component.translatable(base + ".title").withStyle(net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.DARK_RED))
                    .append("\n\n").append(Component.translatable(base + ".p1"))
                    : Component.translatable(base + ".p" + i);
            pages.add(page);
        }
        if (pages.isEmpty()) {
            pages.add(Component.translatable(base + ".title"));
        }
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess() {
            @Override
            public int getPageCount() {
                return pages.size();
            }

            @Override
            public FormattedText getPageRaw(int index) {
                return pages.get(index);
            }
        }));
    }
}
