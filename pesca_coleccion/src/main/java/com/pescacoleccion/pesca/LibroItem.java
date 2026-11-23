package com.pescacoleccion.pesca;

import com.axeliscore.item.CoreItem;
import com.axeliscore.plat.Net;
import com.axeliscore.ui.InfoCard;
import com.axeliscore.ui.Rareza;
import com.pescacoleccion.PescaColeccion;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** El libro de pesca: la colección, las mejoras para siempre, las cañas que se ganan y los récords del mundo. */
public class LibroItem extends CoreItem {
    public LibroItem(Properties props) {
        super(props);
    }

    @Override
    protected boolean onUse(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            Net.onClient(() -> com.axeliscore.ui.ScreenRegistry.open(PescaColeccion.MODID + ":libro"));
        }
        return true;
    }

    @Override
    public InfoCard card(ItemStack stack, boolean detail) {
        return new InfoCard(stack.getHoverName()).rareza(Rareza.RARO).icon(stack)
                .text(Component.translatable(PescaColeccion.MODID + ".libro.ayuda")).hint(Component.translatable(PescaColeccion.MODID + ".libro.abrir"));
    }
}
