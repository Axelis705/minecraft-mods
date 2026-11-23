package com.axeliscore.npc;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Nbt;
import com.axeliscore.ui.CoreScreens;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * (Servidor) Lo que el jugador pide desde la ventana de un NPC o desde el diario: aceptar y entregar misiones, comprar y vender, elegir
 * qué misión seguir y abandonar. El servidor comprueba todo (que el NPC esté cerca, que la misión sea suya, que alcance el dinero).
 */
public final class NpcAcciones {
    /** Pantallas (registradas en el cliente). */
    public static final String PANTALLA = AxelisCore.MODID + ":npc", DIARIO = AxelisCore.MODID + ":diario";
    /** Datos de la ventana abierta: npc, ent (id de la entidad) y saludo. */
    public static final String ABIERTO = AxelisCore.MODID + ":npc_abierto";
    public static final String ACEPTAR = AxelisCore.MODID + ":npc_aceptar", ENTREGAR = AxelisCore.MODID + ":npc_entregar",
            TIENDA = AxelisCore.MODID + ":npc_tienda", SEGUIR = AxelisCore.MODID + ":mision_seguir",
            ABANDONAR = AxelisCore.MODID + ":mision_abandonar";
    private static boolean iniciado;

    private NpcAcciones() {
    }

    public static synchronized void init() {
        if (iniciado) {
            return;
        }
        iniciado = true;
        Actions.on(ACEPTAR, (p, d) -> {
            Npc n = cerca(p, d);
            Mision m = Misiones.porId(Nbt.s(d, "mision"));
            if (n != null && m != null && m.npc.equals(n.defId()) && Misiones.aceptar(p, m.id)) {
                n.playAction("nod");
            }
        });
        Actions.on(ENTREGAR, (p, d) -> {
            Npc n = cerca(p, d);
            Mision m = Misiones.porId(Nbt.s(d, "mision"));
            if (n != null && m != null && m.npc.equals(n.defId())) {
                if (Misiones.entregar(p, m.id)) {
                    n.playAction("cheer");
                } else {
                    n.playAction("shake_head");
                }
            }
        });
        Actions.on(TIENDA, (p, d) -> {
            Npc n = cerca(p, d);
            NpcDef def = n == null ? null : n.def();
            if (def == null || def.tienda == null) {
                return;
            }
            int hechas = def.tienda.usar(p, def.id, Nbt.i(d, "oferta"), Math.max(1, Math.min(64, Nbt.i(d, "veces"))));
            n.playAction(hechas > 0 ? "nod" : "shake_head");
        });
        Actions.on(SEGUIR, (p, d) -> Misiones.seguir(p, Nbt.s(d, "mision")));
        Actions.on(ABANDONAR, (p, d) -> Misiones.abandonar(p, Nbt.s(d, "mision")));
    }

    /** El NPC de la ventana, si sigue cerca y es el mismo. */
    static Npc cerca(ServerPlayer p, CompoundTag d) {
        Entity e = p.level().getEntity(Nbt.i(d, "ent"));
        if (e instanceof Npc n && n.isAlive() && n.distanceToSqr(p) < 10 * 10 && n.defId().equals(Nbt.s(d, "npc"))) {
            n.hablandoCon(p);
            return n;
        }
        return null;
    }

    /** Abre la ventana del NPC (al hacerle clic). También cuenta para las misiones de "hablar con". */
    public static void abrir(ServerPlayer p, Npc n) {
        NpcDef d = n.def();
        if (d == null) {
            return;
        }
        Misiones.alHablar(p, d.id);
        CompoundTag t = new CompoundTag();
        t.putString("npc", d.id);
        t.putInt("ent", n.getId());
        t.putString("saludo", saludo(p, d, n.getRandom().nextInt(1000)));
        PlayerStore.Sync.toPlayer(p, ABIERTO, t);
        CoreScreens.open(p, PANTALLA);
    }

    /** Uno de sus saludos: con más amistad puede decir también los del final de la lista. */
    static String saludo(ServerPlayer p, NpcDef d, int azar) {
        if (d.saludos.isEmpty()) {
            return "axelis_core.npc.hola";
        }
        int nivel = Amistad.nivel(p, d.id);
        int n = d.saludos.size();
        int hasta = Math.max(1, Math.min(n, (int) Math.ceil(n * (nivel + 1) / (double) Math.max(1, d.niveles()))));
        return d.saludos.get(azar % hasta);
    }
}
