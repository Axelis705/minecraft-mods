package com.axeliscore.npc;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * (Servidor) La amistad de cada jugador con cada NPC: puntos, nivel (según {@link NpcDef#amistad}) y regalos al subir de nivel. Se
 * guarda con el jugador y el cliente la ve en {@code ClientData.player("axelis_core")} → "amistad".
 */
public final class Amistad {
    private Amistad() {
    }

    static CompoundTag datos(ServerPlayer p) {
        CompoundTag d = PlayerStore.of(p, AxelisCore.MODID);
        if (!d.contains("amistad")) {
            d.put("amistad", new CompoundTag());
        }
        return Nbt.c(d, "amistad");
    }

    public static int puntos(ServerPlayer p, String npc) {
        return Nbt.i(datos(p), npc);
    }

    public static int nivel(ServerPlayer p, String npc) {
        NpcDef d = NpcDef.porId(npc);
        return d == null ? 0 : d.nivelDe(puntos(p, npc));
    }

    /** Suma (o resta) amistad; al subir de nivel avisa y da el regalo de ese nivel. */
    public static void sumar(ServerPlayer p, String npc, int n) {
        NpcDef d = NpcDef.porId(npc);
        int antes = puntos(p, npc);
        int ahora = Math.max(0, antes + n);
        datos(p).putInt(npc, ahora);
        PlayerStore.save(p, AxelisCore.MODID);
        if (d == null) {
            return;
        }
        int na = d.nivelDe(antes), nn = d.nivelDe(ahora);
        for (int lv = na + 1; lv <= nn; lv++) {
            Fx.toast(p, new ItemStack(Plat.item("minecraft:poppy")), Component.translatable("axelis_core.amistad.sube", d.nombre()),
                    d.nombreNivel(lv), 0xFF7AB8);
            var r = d.regalos.get(lv);
            if (r != null) {
                r.accept(p);
            }
            AxelisCore.LOGGER.info("AXCORE_AMISTAD {} nivel={}", npc, lv);
        }
    }
}
