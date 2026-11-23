package com.coleccionista.evento;

import com.axeliscore.data.Actions;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * Lo que piden los botones del panel del organizador. Todo se revisa aquí: solo operadores u organizadores pueden usarlo (si un jugador
 * cualquiera mandara el mensaje, no pasa nada).
 */
public final class Panel {
    public static final String ACCION = Coleccionista.MODID + ":panel";

    private Panel() {
    }

    public static void init() {
        Actions.on(ACCION, Panel::recibir);
    }

    private static void recibir(ServerPlayer p, CompoundTag d) {
        if (!Tomas.puede(p)) {
            return;
        }
        MinecraftServer s = Plat.server(p);
        Evento ev = Evento.get(s);
        String op = Nbt.s(d, "op");
        Pieza x = d.hasUUID("id") ? ev.pieza(d.getUUID("id")) : null;
        Coleccionista.LOGGER.info("COLECCIONISTA_PANEL {} {}", op, x == null ? "-" : x.nombre);
        switch (op) {
            case "eliminar" -> {
                if (x != null) {
                    ev.eliminar(s, x, Nbt.s(d, "motivo"));
                }
            }
            case "revivir" -> {
                if (x != null) {
                    ev.revivir(s, x);
                }
            }
            case "quitar" -> {
                if (x != null) {
                    ev.quitar(s, x);
                }
            }
            case "ir" -> {
                if (x != null) {
                    Entity e = Evento.entidad(s, x);
                    if (e != null && e.level() instanceof ServerLevel l) {
                        p.teleportTo(l, e.getX(), e.getY() + 1.5, e.getZ(), p.getYRot(), p.getXRot());
                    }
                }
            }
            case "inscribir_todos" -> {
                for (ServerPlayer o : s.getPlayerList().getPlayers()) {
                    if (!ev.esOrganizador(o.getUUID()) && ev.pieza(o) == null && o != p) {
                        ev.inscribir(s, o.getUUID(), o.getGameProfile().getName(), false);
                    }
                }
            }
            case "yo_organizador" -> {
                Pieza yo = ev.pieza(p);
                if (yo != null) {
                    ev.quitar(s, yo);
                }
                ev.organizador(s, p.getUUID(), true);
            }
            case "parejas" -> {
                if (ev.vivos().size() >= 2) {
                    ev.equiposAlAzar(s, 2);
                }
            }
            case "equipos" -> {
                if (ev.vivos().size() >= 2) {
                    ev.equiposAlAzar(s, Math.max(2, Math.min(10, Nbt.i(d, "n"))));
                }
            }
            case "sin_equipos" -> ev.sinEquipos(s);
            case "reloj" -> ev.reloj(s, Math.max(0, Math.min(36000, Nbt.i(d, "n"))));
            case "reloj_mas" -> ev.reloj(s, Math.max(0, Math.min(36000, Math.max(0, ev.resta(s)) + Nbt.i(d, "n"))));
            case "pausa" -> {
                if (!ev.pausar(s)) {
                    ev.reanudar(s);
                }
            }
            case "prueba" -> ev.prueba(s, Nbt.s(d, "nombre"));
            case "reiniciar" -> ev.reiniciar(s);
            case "toma_guardar" -> {
                if (!Tomas.guardar(p, Nbt.s(d, "nombre"))) {
                    Plat.msg(p, Component.translatable(Coleccionista.MODID + ".panel.tomas_llenas", Tomas.MAX), true);
                }
            }
            case "toma_borrar" -> Tomas.borrar(s, Nbt.i(d, "n"));
            case "toma_mover" -> Tomas.mover(s, Nbt.i(d, "n"), Nbt.i(d, "d"));
            default -> Coleccionista.LOGGER.warn("panel: acción desconocida {}", op);
        }
    }

    /** Para pruebas: arma el mensaje igual que el panel. */
    public static CompoundTag pedido(String op, UUID id) {
        CompoundTag t = new CompoundTag();
        t.putString("op", op);
        if (id != null) {
            t.putUUID("id", id);
        }
        return t;
    }
}
