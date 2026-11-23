package com.pescacoleccion.pesca;

import com.axeliscore.item.CoreItem;
import com.axeliscore.ui.InfoCard;
import com.pescacoleccion.pelea.Peleas;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Una caña de pescar del mod: clic derecho lanza el corcho; con el corcho afuera, clic derecho tira (en la picada buena, ¡pelea!). */
public class CanaItem extends CoreItem {
    public final Cana cana;

    public CanaItem(Properties props, Cana cana) {
        super(props);
        this.cana = cana;
    }

    @Override
    protected boolean onUse(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
            return true;
        }
        Anzuelo a = Anzuelo.activo(sp);
        if (a != null) {
            a.tirar(sp);
        } else if (!Peleas.peleando(sp)) {
            Anzuelo.lanzar(sp, cana);
        }
        return true;
    }

    @Override
    public InfoCard card(ItemStack stack, boolean detail) {
        InfoCard c = new InfoCard(stack.getHoverName()).rareza(cana.rareza).icon(stack)
                .subtitle(Component.translatable("pesca_coleccion.cana.tipo"));
        c.bar(Component.translatable("pesca_coleccion.cana.control"), (cana.zone - 0.3F) / 0.32F, 0xFF7CE38B, Math.round(cana.zone * 200) + "%");
        c.bar(Component.translatable("pesca_coleccion.cana.hilo"), cana.lineHp / 170F, 0xFFE8F4FF, String.valueOf(Math.round(cana.lineHp)));
        c.bar(Component.translatable("pesca_coleccion.cana.fuerza"), cana.damage / 16F, 0xFFFFB860, String.valueOf(cana.damage));
        c.bar(Component.translatable("pesca_coleccion.cana.cebo"), (1.2F - cana.espera) / 0.65F, 0xFF6FB7FF,
                Math.round((1 / cana.espera - 1) * 100) + "%");
        c.stars(Component.translatable("pesca_coleccion.cana.suerte"), cana.suerte, 4);
        if (detail) {
            c.divider().text(Component.translatable("pesca_coleccion.cana.ayuda"));
        } else {
            c.hint(Component.translatable("axelis_core.info.mas"));
        }
        return c;
    }

    @Override
    protected void tooltip(ItemStack stack, Consumer<Component> add) {
        add.accept(Component.translatable("pesca_coleccion.cana.ayuda"));
    }
}
