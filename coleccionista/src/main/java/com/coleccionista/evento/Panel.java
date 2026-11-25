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
            case "disfraz" -> {
                boolean si = !ev.disfraz.contains(p.getUUID());
                ev.disfraz(s, p.getUUID(), si);
                p.setGameMode(si ? net.minecraft.world.level.GameType.CREATIVE : net.minecraft.world.level.GameType.SPECTATOR);
            }
            case "altavoz" -> {
                boolean si = !com.coleccionista.voz.Voz.altavoz(p.getUUID());
                com.coleccionista.voz.Voz.altavoz(p.getUUID(), si);
                Plat.msg(p, Component.translatable(Coleccionista.MODID + (!com.coleccionista.voz.VozPuente.disponible ? ".voz.no_instalado"
                        : si ? ".voz.altavoz_si" : ".voz.altavoz_no")), true);
            }
            case "chat" -> Chat.cerrar(s, !Chat.cerrado(s));
            case "sala" -> {
                if (Complejo.construido(s)) {
                    Despertar.esperar(s);
                }
            }
            case "despertar" -> Despertar.despertar(s);
            case "salida" -> {
                if (!Celdas.hablarSalida(s)) {
                    Plat.msg(p, Component.translatable(Coleccionista.MODID + ".cmd.salida_espera"), true);
                }
            }
            case "porton" -> {
                Celdas.porton(s, true);
                Capitulo1.alAbrirPorton(s);
            }
            case "cap1_equipos" -> {
                Component no = Capitulo1.equipos(s);
                if (no != null) {
                    Plat.msg(p, no, true);
                }
            }
            case "cap1_bajar" -> {
                if (!Capitulo1.bajar(s, true)) {
                    Plat.msg(p, Component.translatable(Coleccionista.MODID + ".cap1.no_bajan"), true);
                }
            }
            case "sen_saltar" -> {
                if (!com.coleccionista.prueba.Sentidos.saltar(s)) {
                    Plat.msg(p, Component.translatable(Coleccionista.MODID + ".sen.no"), true);
                }
            }
            case "sen_terminar" -> {
                if (!com.coleccionista.prueba.Sentidos.terminarYa(s)) {
                    Plat.msg(p, Component.translatable(Coleccionista.MODID + ".sen.no"), true);
                }
            }
            case "sen_limpiar" -> {
                com.coleccionista.prueba.Sentidos.reiniciar(s);
                Capitulo1.reiniciar(s);
            }
            case "auto_inscripcion" -> {
                ev.autoInscribir = d.getBoolean("si");
                ev.cambio(s);
            }
            case "discurso" -> Despertar.discurso(s);
            case "llevar" -> {
                Component no = Montacargas.viajar(s, Nbt.s(d, "zona"));
                if (no != null) {
                    Plat.msg(p, no, false);
                }
            }
            case "abrir_celdas" -> Celdas.puertas(s, 0, true);
            // ---- Encadenados
            case "enc_preparar" -> {
                Component e = com.coleccionista.prueba.Encadenados.preparar(s);
                if (e != null) {
                    Plat.msg(p, e, true);
                }
            }
            case "enc_empezar" -> com.coleccionista.prueba.Encadenados.empezar(s, true);
            case "enc_ya" -> com.coleccionista.prueba.Encadenados.empezar(s, false);
            case "enc_soltar" -> com.coleccionista.prueba.Encadenados.soltarTodos(s);
            case "enc_cerrar" -> com.coleccionista.prueba.Encadenados.cerrar(s, false);
            case "enc_terminar" -> com.coleccionista.prueba.Encadenados.terminar(s);
            // ---- Compartir o robar
            case "com_preparar" -> {
                Component e = com.coleccionista.prueba.Compartir.preparar(s);
                if (e != null) {
                    Plat.msg(p, e, true);
                }
            }
            case "com_empezar" -> com.coleccionista.prueba.Compartir.empezar(s, true);
            case "com_ya" -> com.coleccionista.prueba.Compartir.empezar(s, false);
            case "com_elegir" -> com.coleccionista.prueba.Compartir.elegirYa(s);
            case "com_revelar" -> com.coleccionista.prueba.Compartir.revelarYa(s);
            case "com_terminar" -> com.coleccionista.prueba.Compartir.terminar(s);
            // ---- Las puertas
            case "pue_preparar" -> {
                Component e = com.coleccionista.prueba.LasPuertas.preparar(s);
                if (e != null) {
                    Plat.msg(p, e, true);
                }
            }
            case "pue_discurso" -> com.coleccionista.prueba.LasPuertas.discurso(s);
            case "pue_ronda" -> com.coleccionista.prueba.LasPuertas.ronda(s);
            case "pue_cerrar" -> com.coleccionista.prueba.LasPuertas.cerrar(s);
            case "pue_terminar" -> com.coleccionista.prueba.LasPuertas.terminar(s);
            // ---- Las copas (la final)
            case "cop_preparar" -> {
                Component e = com.coleccionista.prueba.LasCopas.preparar(s);
                if (e != null) {
                    Plat.msg(p, e, true);
                }
            }
            case "cop_empezar" -> com.coleccionista.prueba.LasCopas.empezar(s, true);
            case "cop_ya" -> com.coleccionista.prueba.LasCopas.empezar(s, false);
            case "cop_beber" -> com.coleccionista.prueba.LasCopas.beberYa(s);
            case "cop_terminar" -> com.coleccionista.prueba.LasCopas.terminar(s);
            // ---- El sacrificio
            case "sac_preparar" -> {
                Component e = com.coleccionista.prueba.Sacrificio.preparar(s);
                if (e != null) {
                    Plat.msg(p, e, true);
                }
            }
            case "sac_empezar" -> com.coleccionista.prueba.Sacrificio.empezar(s, true);
            case "sac_ya" -> com.coleccionista.prueba.Sacrificio.empezar(s, false);
            case "sac_votar" -> com.coleccionista.prueba.Sacrificio.votarYa(s);
            case "sac_terminar" -> com.coleccionista.prueba.Sacrificio.terminar(s);
            // ---- El traidor
            case "tra_preparar" -> {
                Component e = com.coleccionista.prueba.Traidor.preparar(s);
                if (e != null) {
                    Plat.msg(p, e, true);
                }
            }
            case "tra_empezar" -> com.coleccionista.prueba.Traidor.empezar(s, true);
            case "tra_ya" -> com.coleccionista.prueba.Traidor.empezar(s, false);
            case "tra_cortar" -> com.coleccionista.prueba.Traidor.cortar(s);
            case "tra_terminar" -> com.coleccionista.prueba.Traidor.terminar(s);
            // ---- Encadenados de tres (extra)
            case "tres_preparar" -> {
                Component e = com.coleccionista.prueba.TresSentidos.preparar(s);
                if (e != null) {
                    Plat.msg(p, e, true);
                }
            }
            case "tres_empezar" -> com.coleccionista.prueba.TresSentidos.empezar(s, true);
            case "tres_ya" -> com.coleccionista.prueba.TresSentidos.empezar(s, false);
            case "tres_arrancar" -> com.coleccionista.prueba.TresSentidos.alAcabarReloj(s);
            case "tres_terminar" -> com.coleccionista.prueba.TresSentidos.terminar(s);
            // ---- A ciegas
            case "cie_preparar" -> {
                Component e = com.coleccionista.prueba.ACiegas.preparar(s);
                if (e != null) {
                    Plat.msg(p, e, true);
                }
            }
            case "cie_empezar" -> com.coleccionista.prueba.ACiegas.empezar(s, true);
            case "cie_ya" -> com.coleccionista.prueba.ACiegas.empezar(s, false);
            case "cie_cortar" -> com.coleccionista.prueba.ACiegas.fin(s, true);
            case "cie_terminar" -> com.coleccionista.prueba.ACiegas.terminar(s);
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
