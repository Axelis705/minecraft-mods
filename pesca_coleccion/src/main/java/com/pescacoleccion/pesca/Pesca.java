package com.pescacoleccion.pesca;

import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Plat;
import com.axeliscore.ui.Rareza;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/** (Servidor) Lo que pasa al sacar un pez: el objeto con su tamaño, la colección, los récords y los avisos. */
public final class Pesca {
    private Pesca() {
    }

    /** Devuelve el pez (para que vuele al jugador o se le dé). */
    public static ItemStack pescado(ServerPlayer p, Especie e, float cm, boolean perfecto) {
        Coleccion.Resultado r = Coleccion.registrar(p, e, cm, perfecto);
        if (r.recordMundo()) {
            Libro.recordsATodos(Plat.server(p));
        }
        Libro.primerPez(p);
        ItemStack st = PezItem.crear(ModRegistry.PECES.get(e.id()).get(), e, cm, p.getGameProfile().getName(), r.recordMundo());
        Rareza rz = Rareza.of(e.rareza());
        Component nombre = Component.translatable("item." + PescaColeccion.MODID + "." + e.itemId());
        String tam = String.format(Locale.ROOT, cm >= 100 ? "%.0f cm" : "%.1f cm", cm);
        Fx.toast(p, st, Component.translatable(PescaColeccion.MODID + (r.nuevo() ? ".pesca.nuevo" : ".pelea.pescado")),
                Component.literal(nombre.getString() + " · " + tam), rz.light & 0xFFFFFF);
        if (r.recordMundo() && !r.nuevo()) {
            Fx.title(p, Component.translatable(PescaColeccion.MODID + ".pesca.record_mundo"), Component.literal(nombre.getString() + " · " + tam),
                    0xFFD24A);
        } else if (e.rareza() >= 3) {
            Fx.title(p, Component.translatable(PescaColeccion.MODID + (e.rareza() >= 4 ? ".pesca.legendario" : ".pelea.gran_pez")), nombre,
                    rz.light & 0xFFFFFF);
            Fx.flash(p, 0x60FFFFFF | (rz.light & 0xFFFFFF), 12);
        } else if (perfecto) {
            Fx.title(p, Component.translatable(PescaColeccion.MODID + ".pelea.perfecta"), nombre, 0x7CF29A);
        }
        // los encargos de Nilo que piden pescar
        com.pescacoleccion.nilo.Encargos.alPescar(p, e, cm, perfecto, r.nuevo());
        com.pescacoleccion.torneo.Torneo.alPescar(p, e, cm, perfecto);
        com.axeliscore.data.PlayerStore.save(p, PescaColeccion.MODID);
        if (r.estrellasNuevas() > 0) {
            Plat.msg(p, Component.translatable(PescaColeccion.MODID + ".pesca.estrella", nombre, Integer.bitCount(r.estrellasAhora() & 7)), true);
        }
        return st;
    }
}
