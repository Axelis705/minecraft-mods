package com.axeliscore.item;

import com.axeliscore.plat.Ids;
import com.axeliscore.vestir.Vestuario;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** (Muestra) Un objeto con modelo 3D que además se puede poner: clic derecho y el sombrero pasa a la cabeza (Vestuario). */
public class SombreroDemo extends CoreItem {
    public SombreroDemo(Properties p) {
        super(p);
    }

    @Override
    protected void tooltip(ItemStack stack, Consumer<Component> add) {
        add.accept(Component.literal("Clic derecho: ponérselo o quitárselo"));
    }

    @Override
    protected boolean onUse(Level level, Player p, InteractionHand hand) {
        if (p instanceof ServerPlayer sp) {
            var id = Ids.core("sombrero");
            var s = com.axeliscore.plat.Plat.server(sp);
            if (Vestuario.tiene(s, sp.getUUID(), id)) {
                Vestuario.quitar(s, sp.getUUID(), id);
            } else {
                Vestuario.poner(s, sp.getUUID(), id, null);
            }
        }
        return true;
    }
}
