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
 *     <li>5 minutos de espera en la celda: todavía no hay nada (ni pistas ni notas) y el candado está muerto.</li>
 *     <li>Al acabar el reloj hay un apagón; cuando vuelve la luz, los tres números de cada celda ya están escondidos en ella
 *     ({@link Pistas}). Habla el Coleccionista en la pantalla chica de cada celda (en el patio no hay pantallas grandes); al terminar,
 *     los candados funcionan y corre el reloj de 2 minutos para salir. Cuando sale el último, el reloj baja a 10 segundos; al llegar a
 *     cero se abren solas las celdas que sigan cerradas y sube el portón ({@link Celdas#finSalida}). En la sala grande pide equipos
 *     ({@link Capitulo1}).</li>
 * </ol>
 */
public final class Despertar {
    private static final String MOD = Coleccionista.MODID;
    /** A cada jugador: su cinemática (tipo, puntos de cámara y hacia dónde mira). */
    public static final String KEY_CINE = MOD + ":cine";
    public static final String PRUEBA = "El despertar";
    /** Segundos para investigar antes de que hable el Coleccionista (Axelis pidió 5 minutos). */
    public static int segundosInvestigar = 300;
    /** Segundos para salir de la celda cuando ya funcionan los candados (Axelis pidió 2 minutos). */
    public static int segundosSalir = 120;
    private static long levantarEn = -1, cineEn = -1, apagonFin = -1, hablarEn = -1;
    /** Corre el reloj para salir de las celdas (al acabarse, se abren solas y sube el portón). */
    private static boolean esperandoSalida;
    /** Cuánto dura el apagón antes de que aparezcan las pistas (ticks). */
    private static final int APAGON = 60;
    private static boolean esperandoDiscurso;

    private Despertar() {
    }

    public static void init() {
        AxelisCore.onServerTick(Despertar::tick);
        AxelisCore.onLogin(Despertar::alEntrar);
        MinecraftForge.EVENT_BUS.addListener(Despertar::interactuar);
        MinecraftForge.EVENT_BUS.addListener(Despertar::interactuarEntidad);
        MinecraftForge.EVENT_BUS.addListener(Despertar::dormir);
    }

    /**
     * Las piezas no se acuestan solas en el catre (el evento las acuesta en la sala de espera): si se pudiera, se quedarían dormidas sin
     * poder levantarse a media prueba.
     */
    private static void dormir(net.minecraftforge.event.entity.player.PlayerSleepInBedEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && Evento.get(p.server).pieza(p) != null) {
            e.setResult(net.minecraft.world.entity.player.Player.BedSleepingProblem.OTHER_PROBLEM);
            Plat.msg(p, Component.translatable(MOD + ".cama.no").withStyle(net.minecraft.ChatFormatting.DARK_GRAY,
                    net.minecraft.ChatFormatting.ITALIC), true);
        }
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
        // sin coordenadas en F3: en los laberintos y las cloacas nadie se orienta con números
        g.getRule(GameRules.RULE_REDUCEDDEBUGINFO).set(true, s);
        s.overworld().setDayTime(18000);
        Celdas.luces(s, false);
        Celdas.puertas(s, 0, false);
        Celdas.portonCerrado(s);
        Celdas.prepararPistas(s);
        Pantallas.soloGrande(s, false);
        Pantallas.modo(s, "apagada", "");
        ev.sala = true;
        ev.fase = Evento.Fase.ESPERA;
        ev.prueba = "";
        ev.relojFin = 0;
        for (Pieza p : ev.vivos()) {
            Celdas.asignar(s, ev, p);
            Uniforme.poner(s, p);
            Celdas.llevar(s, p, true);
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                sp.setGameMode(GameType.ADVENTURE);
            }
        }
        ev.cambio(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_SALA_ESPERA piezas={}", ev.vivos().size());
    }

    /**
     * Al entrar al servidor durante la sala de espera: se inscribe (si no es operador ni organizador) y va a su celda. Con la inscripción
     * automática, si el evento todavía no empieza (nadie ha despertado), la sala de espera se abre sola con el primero que entra.
     */
    private static void alEntrar(ServerPlayer p) {
        MinecraftServer s = Plat.server(p);
        Evento ev = Evento.get(s);
        if (!ev.sala) {
            boolean organiza = Plat.isOp(p) || Plat.isHost(p) || ev.esOrganizador(p.getUUID());
            if (!ev.autoInscribir || organiza || !ev.sinEmpezar() || !Complejo.construido(s)) {
                return;
            }
            Coleccionista.LOGGER.info("COLECCIONISTA_AUTO_SALA por {}", p.getGameProfile().getName());
            esperar(s);
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

    /**
     * Lo llama el evento cuando se acaba el reloj: si era el del despertar, el apagón; si era el de salir de las celdas, se abren las que
     * sigan cerradas y sube el portón. Devuelve si lo atendió.
     */
    static boolean alAcabarReloj(MinecraftServer s) {
        if (esperandoSalida) {
            esperandoSalida = false;
            Celdas.finSalida(s);
            return true;
        }
        Evento ev = Evento.get(s);
        if (!esperandoDiscurso || !PRUEBA.equals(ev.prueba)) {
            return false;
        }
        discurso(s);
        return true;
    }

    /** Ya se abrió todo (por el reloj, porque salieron todas o porque el organizador lo pidió): el reloj para salir ya no es suyo. */
    static void yaSalieron() {
        esperandoSalida = false;
    }

    /** ¿Está corriendo el reloj para salir de las celdas? */
    public static boolean saliendo() {
        return esperandoSalida;
    }

    /**
     * Fin de la espera (o el organizador la corta): si las pistas no han salido, primero el apagón (y al volver la luz ya están escritas y
     * los candados funcionan); si ya salieron, los candados funcionan ya.
     */
    public static void discurso(MinecraftServer s) {
        esperandoDiscurso = false;
        if (apagonFin > 0 || hablarEn > 0 || esperandoSalida) {
            return;
        }
        if (!Pistas.reveladas(s)) {
            apagon(s);
            return;
        }
        hablar(s);
    }

    /** Se va la luz de todo el ala unos segundos. */
    private static void apagon(MinecraftServer s) {
        Celdas.luces(s, false);
        Anuncio.sonido(s, SoundEvents.BEACON_DEACTIVATE, 1F, 0.5F);
        Anuncio.sonido(s, com.coleccionista.registry.ModRegistry.CHISPA.get(), 1F, 0.7F);
        Anuncio.sonido(s, SoundEvents.AMBIENT_CAVE.get(), 0.9F, 0.7F);
        apagonFin = s.overworld().getGameTime() + APAGON;
        Coleccionista.LOGGER.info("COLECCIONISTA_APAGON");
    }

    /** Vuelve la luz: ya están las pistas en cada celda (y la hoja del diario en el baúl) y los candados funcionan. */
    private static void vuelveLaLuz(MinecraftServer s) {
        Pistas.revelar(s);
        Celdas.llenarBaules(s);
        Celdas.luces(s, true);
        Anuncio.sonido(s, SoundEvents.IRON_TRAPDOOR_CLOSE, 1F, 0.5F);
        Anuncio.sonido(s, SoundEvents.BEACON_ACTIVATE, 1F, 0.6F);
        hablarEn = s.overworld().getGameTime() + 50;
    }

    /** La bienvenida en la pantalla chica de cada celda; al terminar, los candados funcionan. */
    private static void hablar(MinecraftServer s) {
        Pantallas.soloGrande(s, false);
        Pantallas.hablar(s, "bienvenida", () -> candadosListos(s));
    }

    /** Los candados ya funcionan y corre el reloj para salir. */
    private static void candadosListos(MinecraftServer s) {
        Celdas.candados(s, true);
        Evento ev = Evento.get(s);
        ev.prueba(s, Component.translatable(MOD + ".prueba.salir").getString());
        ev.reloj(s, segundosSalir);
        esperandoSalida = true;
        Anuncio.titulo(s, Component.translatable(MOD + ".salir.titulo"), Component.translatable(MOD + ".salir.sub", segundosSalir / 60));
        Anuncio.sonido(s, SoundEvents.ENDER_EYE_DEATH, 0.8F, 0.6F);
        Anuncio.sonido(s, SoundEvents.IRON_TRAPDOOR_OPEN, 0.8F, 0.5F);
        Coleccionista.LOGGER.info("COLECCIONISTA_CANDADOS activos segundos={}", segundosSalir);
    }

    private static void tick(MinecraftServer s) {
        Evento ev = Evento.get(s);
        long now = s.overworld().getGameTime();
        if (cineEn > 0 && now >= cineEn) {
            cineEn = -1;
            despiertan(s);
        }
        if (apagonFin > 0 && now >= apagonFin) {
            apagonFin = -1;
            vuelveLaLuz(s);
        }
        if (hablarEn > 0 && now >= hablarEn) {
            hablarEn = -1;
            hablar(s);
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
        apagonFin = -1;
        hablarEn = -1;
        esperandoDiscurso = false;
        esperandoSalida = false;
    }
}
