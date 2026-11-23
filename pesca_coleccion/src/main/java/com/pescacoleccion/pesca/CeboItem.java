package com.pescacoleccion.pesca;

import com.axeliscore.item.CoreItem;
import com.axeliscore.plat.Plat;
import com.axeliscore.ui.InfoCard;
import com.pescacoleccion.PescaColeccion;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/** Una carnada (ver {@link Cebo}): se lleva en la otra mano o en el inventario y el pez se la come cuando pica. */
public class CeboItem extends CoreItem {
    private static final String MOD = PescaColeccion.MODID;
    public final Cebo cebo;

    public CeboItem(Properties props, Cebo cebo) {
        super(props);
        this.cebo = cebo;
    }

    @Override
    public InfoCard card(ItemStack stack, boolean detail) {
        InfoCard c = new InfoCard(stack.getHoverName()).rareza(cebo.rareza).icon(stack)
                .subtitle(Component.translatable(MOD + ".cebo.tipo"));
        c.bar(Component.translatable(MOD + ".cebo.rapido"), (1 - cebo.espera) / 0.4F, 0xFF6FB7FF, Math.round((1 / cebo.espera - 1) * 100) + "%");
        if (cebo.suerte > 0) {
            c.stars(Component.translatable(MOD + ".cana.suerte"), cebo.suerte, 2);
        }
        c.text(Component.translatable(MOD + ".cebo." + cebo.id + ".que"), 0xFFE8F4FF);
        if (detail) {
            c.divider().pair(Component.translatable(MOD + ".cebo.conseguir"), Component.translatable(MOD + ".cebo." + cebo.id + ".como"));
            c.text(Component.translatable(MOD + ".cebo.como"), 0xFF9AA3BC);
        } else {
            c.hint(Component.translatable("axelis_core.info.mas"));
        }
        return c;
    }

    @Override
    protected void tooltip(ItemStack stack, Consumer<Component> add) {
        add.accept(Component.translatable(MOD + ".cebo." + cebo.id + ".que"));
        add.accept(Component.translatable(MOD + ".cebo.como"));
    }

    /** La carnada que usaría el jugador (primero la de la otra mano, luego la primera del inventario); null si no lleva. */
    public static Cebo buscar(Player p) {
        if (p.getOffhandItem().getItem() instanceof CeboItem ci) {
            return ci.cebo;
        }
        for (ItemStack st : Plat.mainItems(p.getInventory())) {
            if (st.getItem() instanceof CeboItem ci) {
                return ci.cebo;
            }
        }
        return null;
    }

    /** Cuántas carnadas de ese tipo lleva. */
    public static int contar(Player p, Cebo c) {
        int n = 0;
        for (ItemStack st : Plat.mainAndOffhand(p.getInventory())) {
            if (st.getItem() instanceof CeboItem ci && ci.cebo == c) {
                n += st.getCount();
            }
        }
        return n;
    }

    /** Gasta una carnada de ese tipo (primero la de la otra mano). En creativo no se gasta. */
    public static boolean gastar(Player p, Cebo c) {
        if (p.getAbilities().instabuild) {
            return true;
        }
        ItemStack off = p.getOffhandItem();
        if (off.getItem() instanceof CeboItem ci && ci.cebo == c) {
            off.shrink(1);
            return true;
        }
        for (ItemStack st : Plat.mainItems(p.getInventory())) {
            if (st.getItem() instanceof CeboItem ci && ci.cebo == c) {
                st.shrink(1);
                return true;
            }
        }
        return false;
    }
}
