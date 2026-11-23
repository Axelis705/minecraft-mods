package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

/**
 * La sala de espera y el despertar.
 * <ol>
 *     <li>Sala de espera: cada amigo que entra va a su celda, acostado en la cama, con la pantalla en negro ("Esperando jugadores") y sin
 *     poder tocar nada. Las luces de las celdas están apagadas.</li>
 *     <li>El "¡pum!" del organizador ({@code /evento despertar}): golpe de electricidad, se prenden las luces y cada uno ve su cinemática
 *     (la cámara arriba en la esquina, viéndose acostado, se acerca y pasa a sus ojos con un parpadeo). Luego se levanta.</li>
 *     <li>10 minutos para investigar la celda (notas, diario, pistas). El candado está muerto.</li>
 *     <li>Al acabar el reloj se prenden las pantallas y habla el Coleccionista; al terminar, los candados funcionan.</li>
 * </ol>
 */
public final class Despertar {
    private static final String MOD = Coleccionista.MODID;
    /** A cada jugador: su cinemática (tipo, puntos de cámara y hacia dónde mira). */
    public static final String KEY_CINE = MOD + ":cine";
    public static final String PRUEBA = "El despertar";
    /** Segundos para investigar antes de que hable el Coleccionista (Axelis pidió 10 minutos). */
    public static int segundosInvestigar = 600;
    private static long levantarEn = -1, cineEn = -1;
    private static boolean esperandoDiscurso;

    private Despertar() {
    }

    public static void init() {
        AxelisCore.onServerTick(Despertar::tick);
        AxelisCore.onLogin(Despertar::alEntrar);
        MinecraftForge.EVENT_BUS.addListener(Despertar::interactuar);
        MinecraftForge.EVENT_BUS.addListener(Despertar::interactuarEntidad);
    }

    private static boolean enSala(MinecraftServer s) {
        return Evento.get(s).sala;
    }

    // ------------------------------------------------------------------ sala de espera

    /** Prepara la sala de espera: reglas del mundo, luces apagadas, puertas cerradas, pistas listas y cada pieza a su celda. */
    public static void esperar(MinecraftServer s) {
        Evento ev = Evento.get(s);
        GameRules g = s.getGameRules();
        g.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, s);
        g.getRule(GameRules.RULE_DAYLIGHT).set(false, s);
        g.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, s);
        g.getRule(GameRules.RULE_KEEPINVENTORY).set(true, s);
        g.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, s);
        g.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, s);
        g.getRule(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE).set(101, s);
        g.getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(true, s);
        s.overworld().setDayTime(18000);
        Celdas.luces(s, false);
        Celdas.puertas(s, 0, false);
        Celdas.prepararPistas(s);
        Pantallas.modo(s, "apagada", "");
        ev.sala = true;
        ev.fase = Evento.Fase.ESPERA;
        ev.prueba = "";
        ev.relojFin = 0;
        for (Pieza p : ev.vivos()) {
            Celdas.asignar(s, ev, p);
            Celdas.llevar(s, p, true);
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                sp.setGameMode(GameType.ADVENTURE);
            }
        }
        ev.cambio(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_SALA_ESPERA piezas={}", ev.vivos().size());
    }

    /** Al entrar al servidor durante la sala de espera: se inscribe (si no es operador ni organizador) y va a su celda. */
    private static void alEntrar(ServerPlayer p) {
        MinecraftServer s = Plat.server(p);
        Evento ev = Evento.get(s);
        if (!ev.sala) {
            return;
        }
        Pieza x = ev.pieza(p);
        if (x == null) {
            if (Plat.isOp(p) || Plat.isHost(p) || ev.esOrganizador(p.getUUID())) {
                return;
            }
            x = ev.inscribir(s, p.getUUID(), p.getGameProfile().getName(), false);
        }
        if (x.vivo()) {
            Celdas.asignar(s, ev, x);
            p.setGameMode(GameType.ADVENTURE);
            Celdas.llevar(s, x, true);
            ev.cambio(s);
        }
    }

    /** En la sala de espera nadie toca nada. */
    private static void interactuar(PlayerInteractEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.isCancelable() && enSala(p.server) && Evento.get(p.server).pieza(p) != null) {
            e.setCanceled(true);
            if (e instanceof PlayerInteractEvent.RightClickBlock rb) {
                rb.setUseBlock(Event.Result.DENY);
            }
        }
    }

    private static void interactuarEntidad(PlayerInteractEvent.EntityInteract e) {
        if (e.getEntity() instanceof ServerPlayer p && enSala(p.server) && Evento.get(p.server).pieza(p) != null) {
            e.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ el ¡pum!

    /** El organizador despierta a todos. */
    public static boolean despertar(MinecraftServer s) {
        Evento ev = Evento.get(s);
        if (!ev.sala || cineEn > 0) {
            return false;
        }
        // ¡pum!: las luces se prenden mientras todos siguen a oscuras; la cinemática empieza un momento después (así ya hay luz)
        Celdas.luces(s, true);
        Anuncio.sonido(s, SoundEvents.IRON_TRAPDOOR_CLOSE, 1F, 0.5F);
        Anuncio.sonido(s, SoundEvents.BEACON_ACTIVATE, 1F, 0.6F);
        cineEn = s.overworld().getGameTime() + 30;
        return true;
    }

    private static void despiertan(MinecraftServer s) {
        Evento ev = Evento.get(s);
        ev.sala = false;
        long now = s.overworld().getGameTime();
        for (Pieza p : ev.vivos()) {
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                PlayerStore.Sync.toPlayer(sp, KEY_CINE, cine(s, p, now));
            }
        }
        levantarEn = now + 130;
        esperandoDiscurso = true;
        ev.prueba(s, PRUEBA);
        ev.reloj(s, segundosInvestigar);
        ev.cambio(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_DESPERTAR piezas={}", ev.vivos().size());
    }

    /** La cinemática del despertar de una pieza: dos puntos de cámara en su celda y la cama a la que miran. */
    private static CompoundTag cine(MinecraftServer s, Pieza p, long now) {
        CompoundTag t = new CompoundTag();
        t.putString("tipo", "despertar");
        t.putLong("cuando", now);
        t.put("a", punto(Complejo.pos(s, "celda." + p.celda + ".cam1"), 0.5, 0.6, 0.5));
        t.put("b", punto(Complejo.pos(s, "celda." + p.celda + ".cam2"), 0.5, 0.2, 0.5));
        t.put("mira", punto(Complejo.pos(s, "celda." + p.celda + ".cama"), 0.5, 0.6, 0.5));
        return t;
    }

    private static ListTag punto(BlockPos b, double dx, double dy, double dz) {
        ListTag l = new ListTag();
        if (b == null) {
            return l;
        }
        l.add(DoubleTag.valueOf(b.getX() + dx));
        l.add(DoubleTag.valueOf(b.getY() + dy));
        l.add(DoubleTag.valueOf(b.getZ() + dz));
        return l;
    }

    /** Lo llama el evento cuando se acaba el reloj: si era el del despertar, empieza el discurso. Devuelve si lo atendió. */
    static boolean alAcabarReloj(MinecraftServer s) {
        Evento ev = Evento.get(s);
        if (!esperandoDiscurso || !PRUEBA.equals(ev.prueba)) {
            return false;
        }
        discurso(s);
        return true;
    }

    /** Se prenden las pantallas y habla el Coleccionista; al terminar, los candados funcionan. */
    public static void discurso(MinecraftServer s) {
        esperandoDiscurso = false;
        Pantallas.hablar(s, "bienvenida", () -> {
            Celdas.candados(s, true);
            Evento ev = Evento.get(s);
            ev.prueba(s, Component.translatable(MOD + ".prueba.salir").getString());
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".chat.candados")));
            Anuncio.sonido(s, SoundEvents.ENDER_EYE_DEATH, 0.8F, 0.6F);
            Coleccionista.LOGGER.info("COLECCIONISTA_CANDADOS activos");
        });
    }

    private static void tick(MinecraftServer s) {
        Evento ev = Evento.get(s);
        long now = s.overworld().getGameTime();
        if (cineEn > 0 && now >= cineEn) {
            cineEn = -1;
            despiertan(s);
        }
        if (levantarEn > 0 && now >= levantarEn) {
            levantarEn = -1;
            for (Pieza p : ev.vivos()) {
                Celdas.llevar(s, p, false);
            }
        }
        // en la sala de espera, quien se levante de la cama vuelve a acostarse
        if (ev.sala && now % 20 == 0) {
            for (Pieza p : ev.vivos()) {
                if (Evento.entidad(s, p) instanceof ServerPlayer sp && !sp.isSleeping()) {
                    Celdas.llevar(s, p, true);
                }
            }
        }
    }

    /** Al reiniciar el evento. */
    static void reiniciar() {
        levantarEn = -1;
        cineEn = -1;
        esperandoDiscurso = false;
    }
}
