package com.navidad.arbol;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Un adorno para el Pino de Navidad: con clic derecho en el árbol se cuelga (donde hiciste clic). */
public class AdornoItem extends Item {
    private final String id;

    public AdornoItem(Properties p, String id) {
        super(p);
        this.id = id;
    }

    public Adornos.Def def() {
        return Adornos.de(id);
    }

    @Override
    public Component getName(ItemStack st) {
        Adornos.Def d = def();
        Component n = super.getName(st);
        return d == null ? n : n.copy().withStyle(d.rareza().color);
    }

    @Override
    public void appendHoverText(ItemStack st, Level l, List<Component> t, TooltipFlag f) {
        Adornos.Def d = def();
        if (d == null) {
            return;
        }
        t.add(Component.translatable("navidad.adorno.rareza." + d.rareza().name().toLowerCase(java.util.Locale.ROOT))
                .append(Component.literal(" · ")).append(Component.translatable("navidad.adorno.puntos", d.puntos()))
                .withStyle(d.rareza().color));
        String ayuda = switch (d.tipo()) {
            case LUCES -> "navidad.adorno.ayuda_luces";
            case GUIRNALDA -> "navidad.adorno.ayuda_guirnalda";
            case PUNTA -> "navidad.adorno.ayuda_punta";
            default -> "navidad.adorno.ayuda";
        };
        t.add(Component.translatable(ayuda).withStyle(ChatFormatting.GRAY));
    }
}
