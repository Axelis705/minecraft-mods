package com.pescacoleccion.pesca;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** (Servidor) Lo del libro de pesca: mandar los récords del mundo, reclamar cañas y dar el libro con el primer pez. */
public final class Libro {
    public static final String RECORDS = PescaColeccion.MODID + ":records";

    private Libro() {
    }

    public static void init() {
        AxelisCore.onLogin(Libro::enviarRecords);
        Actions.on(PescaColeccion.MODID + ":reclamar_cana", (p, data) -> reclamar(p, Nbt.i(data, "cana")));
    }

    public static void enviarRecords(ServerPlayer p) {
        MinecraftServer s = Plat.server(p);
        if (s != null) {
            PlayerStore.Sync.toPlayer(p, RECORDS, Nbt.c(WorldStore.get(s, PescaColeccion.MODID).global(), "records"));
        }
    }

    /** Cuando cambia un récord, se avisa a todos (para sus libros). */
    public static void recordsATodos(MinecraftServer s) {
        PlayerStore.Sync.toAll(s, RECORDS, Nbt.c(WorldStore.get(s, PescaColeccion.MODID).global(), "records"));
    }

    private static void reclamar(ServerPlayer p, int i) {
        if (i <= 0 || i >= Cana.values().length) {
            return;
        }
        Cana c = Cana.values()[i];
        CompoundTag d = PlayerStore.of(p, PescaColeccion.MODID);
        if (!Mejoras.puedeReclamar(d, c)) {
            return;
        }
        d.putInt("canas", Nbt.i(d, "canas") | (1 << i));
        PlayerStore.save(p, PescaColeccion.MODID);
        ItemStack st = new ItemStack(ModRegistry.CANAS.get(c).get());
        Plat.give(p, st.copy());
        Fx.toast(p, st, Component.translatable(PescaColeccion.MODID + ".libro.cana_nueva"), st.getHoverName(), c.rareza.light & 0xFFFFFF);
    }

    /** La primera vez que pesca algo, recibe el libro. */
    public static void primerPez(ServerPlayer p) {
        CompoundTag d = PlayerStore.of(p, PescaColeccion.MODID);
        if (Nbt.b(d, "libro")) {
            return;
        }
        d.putBoolean("libro", true);
        PlayerStore.save(p, PescaColeccion.MODID);
        Plat.give(p, new ItemStack(ModRegistry.LIBRO.get()));
        Plat.msg(p, Component.translatable(PescaColeccion.MODID + ".libro.recibido"), false);
    }
}
