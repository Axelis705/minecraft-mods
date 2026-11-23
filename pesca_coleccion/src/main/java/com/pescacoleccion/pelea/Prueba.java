package com.pescacoleccion.pelea;

import com.pescacoleccion.PescaColeccion;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Comando de prueba de la pelea (mientras llegan los peces de verdad): /pesca_coleccion pelea &lt;rareza&gt; [facil]. */
public final class Prueba {
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
        com.pescacoleccion.pesca.Especie e = null;
        for (com.pescacoleccion.pesca.Especie x : com.pescacoleccion.pesca.Especies.all()) {
            if (x.rareza() == Math.min(4, rareza)) {
                e = x;
                break;
            }
        }
        com.pescacoleccion.pesca.Especie esp = e;
        float cm = esp.tamano(p.getRandom().nextDouble());
        Peleas.start(p, params, "item." + PescaColeccion.MODID + "." + esp.itemId(), rareza, sim -> {
            if (sim.result == PeleaSim.PESCADO) {
                com.axeliscore.plat.Plat.give(p, com.pescacoleccion.pesca.Pesca.pescado(p, esp, cm, sim.flawless()));
            }
        });
        return 1;
    }
}
