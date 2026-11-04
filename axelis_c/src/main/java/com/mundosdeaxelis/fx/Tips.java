package com.mundosdeaxelis.fx;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Textos pequeños que salen al pasar el mouse sobre un objeto (vienen de los archivos de idioma). */
public final class Tips {
    private Tips() {
    }

    /** Añade las líneas "tip.mundos_de_axelis.<id>.1", ".2", ... (hasta {@code lines}). */
    public static void add(List<Component> tooltip, String id, int lines) {
        for (int i = 1; i <= lines; i++) {
            boolean last = i == lines && lines > 1;
            Component line = Component.translatable("tooltip." + MundosDeAxelis.MODID + "." + id + "." + i);
            tooltip.add(last ? line.copy().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)
                    : line.copy().withStyle(ChatFormatting.GRAY));
        }
    }
}
