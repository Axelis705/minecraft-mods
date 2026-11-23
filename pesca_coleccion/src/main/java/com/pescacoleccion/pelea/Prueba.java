package com.pescacoleccion.pelea;

import com.axeliscore.fx.Fx;
import com.axeliscore.ui.Rareza;
import com.pescacoleccion.PescaColeccion;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Comando de prueba de la pelea (mientras llegan los peces de verdad): /pesca_coleccion pelea &lt;rareza&gt; [facil]. */
public final class Prueba {
    private static final Item[] PREMIO = {Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH, Items.HEART_OF_THE_SEA,
            Items.NAUTILUS_SHELL};

    private Prueba() {
    }

    public static int pelea(CommandSourceStack src, int rareza, boolean facil) {
        if (!(src.getEntity() instanceof ServerPlayer p)) {
            src.sendFailure(Component.literal("Solo un jugador puede pelear con un pez"));
            return 0;
        }
        PeleaSim.Params params = Niveles.base(rareza, p.getRandom().nextLong());
        Niveles.cana(params, 0.40F, 1.0F, 100, 10);
        if (facil) {
            Niveles.facil(params);
        }
        Peleas.start(p, params, PescaColeccion.MODID + ".prueba.pez_" + rareza, rareza, sim -> {
            if (sim.result == PeleaSim.PESCADO) {
                ItemStack st = premio(rareza);
                ItemStack shown = st.copy();
                com.axeliscore.plat.Plat.give(p, st);
                avisar(p, rareza, shown, sim.flawless());
            }
        });
        return 1;
    }

    /** El premio de prueba por rareza (mientras llegan los peces de verdad). */
    public static ItemStack premio(int rareza) {
        return new ItemStack(PREMIO[Math.max(0, Math.min(rareza, PREMIO.length - 1))]);
    }

    /** Aviso de pez sacado (y título si es grande o se sacó perfecto). */
    public static void avisar(ServerPlayer p, int rareza, ItemStack st, boolean perfecto) {
        Rareza r = Rareza.of(rareza);
        Fx.toast(p, st, Component.translatable(PescaColeccion.MODID + ".pelea.pescado"),
                Component.translatable(PescaColeccion.MODID + ".prueba.pez_" + rareza), r.light & 0xFFFFFF);
        if (rareza >= 3 || perfecto) {
            Fx.title(p, Component.translatable(PescaColeccion.MODID + (perfecto ? ".pelea.perfecta" : ".pelea.gran_pez")),
                    Component.translatable(PescaColeccion.MODID + ".prueba.pez_" + rareza), r.light & 0xFFFFFF);
        }
    }
}
