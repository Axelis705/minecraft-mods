package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import com.coleccionista.bot.PiezaBot;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Cabina;
import com.coleccionista.evento.Cadaver;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Cuerpos;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Objetivo;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.evento.Silla;
import com.coleccionista.mapa.Circuito;
import com.coleccionista.mapa.Salidas;
import com.coleccionista.mapa.SalaVitrina;
import com.coleccionista.prueba.TresSentidos.Rol;
import com.coleccionista.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Capítulo 1 · Los sentidos: cada equipo (de 2 o de 3) llega en su ascensor a su propio circuito (el taller, ver {@link Circuito}), lejos de
 * los demás. Cuando están todos fuera del ascensor habla el Coleccionista. En la mesa cada uno toma un objeto: la <b>venda</b> (no ve),
 * la <b>cinta</b> (no habla ni escribe) o las <b>esposas</b> (no toca nada); en parejas no hay cinta. Encadenados cruzan los pasillos, los
 * postes, la pasarela, las baldosas y las palancas hasta la salida, donde se sientan y esperan a los demás (en el circuito nadie se puede
 * lastimar ni morir).
 * <p>
 * El final, en el teatro de la colección ({@link SalaVitrina}): los que se salvaron pasan sentados (sin que se note) a una copia de su
 * cuarto en el teatro y se les abre la puerta frente a un telón. Al <b>último equipo</b> se le llena el cuarto de gas: se desmaya y
 * despierta sentado en una vitrina del escenario. Se abre el telón, cae la cera, se vuelven estatuas de piedra y caen frente a todos.
 */
public final class Sentidos {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":sentidos";
    /** A cada víctima de la vitrina: desde cuándo sube la cera (para su pantalla). */
    public static final String KEY_CERA = MOD + ":cera";
    /** Al último equipo: el gas del cuarto (desmayo) y luego el despertar en la vitrina. */
    public static final String KEY_GAS = MOD + ":desmayo";
    public static final String PRUEBA = "Los sentidos";
    public static int segundosElegir = 60, segundos = 900;
    public static double largo = 4.0;
    /** Para las pruebas automáticas: los equipos de puros bots llegan solos a la salida y se sientan. */
    public static boolean botsSalen = true;
    public static int botMin = 90, botMax = 240;
    /** Para probar con un solo equipo: si es el único, también pierde (así se ve el final de la vitrina). */
    public static boolean pierdeSolo = false;
    /** Cuánto espera el Coleccionista a que salgan todos del ascensor antes de hablar de todos modos (ticks). */
    public static int esperaTaller = 600;

    public enum Fase {
        NADA, LISTA, ESPERA, HABLANDO, ELEGIR, JUEGO, FINAL, FIN
    }

    public enum Etapa {
        MESA, POSTES, PASARELA, BALDOSAS, PALANCAS, SALIDA, SENTADOS, FUERA
    }

    static final class Equipo {
        final int k;
        final List<UUID> ids = new ArrayList<>();
        final Map<UUID, Rol> roles = new LinkedHashMap<>();
        final List<Cuerda> cuerdas = new ArrayList<>();
        final Rol[] objetos = new Rol[3];
        Etapa etapa = Etapa.MESA;
        long termino = -1, verHasta = -1, trampaHasta = -1, botListo = -1;
        boolean perdio, avisoEspera;
        int[] trampa;
        /** El camino de las baldosas: qué baldosas son seguras [fila][columna] (nunca en diagonal). */
        final boolean[][] seguro = new boolean[Circuito.BALD_FILAS][Circuito.BALD_ANCHO];
        final boolean[] patron = new boolean[Circuito.PALANCAS];
        boolean tablero;
        final List<Silla> sillas = new ArrayList<>();

        Equipo(int k) {
            this.k = k;
        }

        String pre() {
            return "circ." + k + ".";
        }

        String cuarto() {
            return "sal_circ_" + k;
        }

        String palco() {
            return "sal_tea_" + k;
        }
    }

    /** Una víctima de la vitrina: quién, en qué silla y en qué vitrina. */
    record Victima(UUID id, Silla silla, int caja) {
    }

    private static Fase fase = Fase.NADA;
    private static final List<Equipo> equipos = new ArrayList<>();
    private static final Map<UUID, Long> botElige = new HashMap<>();
    /** En qué punto del camino (el indicador de "ir acá") va cada quien. */
    private static final Map<UUID, Integer> paso = new HashMap<>();
    private static long t, esperaDesde;
    private static boolean cambio;
    // el final
    private static final List<Victima> victimas = new ArrayList<>();
    private static int cajas;
    private static long finT0 = -1, telonT0 = -1, ceraT0 = -1, finalFin = -1;
    private static int telonPaso, capas;
    private static boolean palcos, levantan, gas, aVitrinas, despiertan, telon, hablo, ceraAviso, endurecida, rotas, caen, golpe,
            salidaAbierta, antesala;
    private static final List<Cadaver> estatuas = new ArrayList<>();

    private Sentidos() {
    }

    public static void init() {
        AxelisCore.onServerTick(Sentidos::tick);
        AxelisCore.onLogin(p -> {
            if (fase != Fase.NADA) {
                PlayerStore.Sync.toPlayer(p, KEY, estado(Plat.server(p)));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> manos(e));
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock e) -> manos(e));
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> manos(e));
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> manos(e));
        MinecraftForge.EVENT_BUS.addListener((AttackEntityEvent e) -> {
            if (fase == Fase.JUEGO && rol(e.getEntity().getUUID()) == Rol.ESPOSAS) {
                e.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ServerChatEvent e) -> {
            if (fase == Fase.JUEGO && rol(e.getPlayer().getUUID()) == Rol.CINTA && !salio(e.getPlayer().getUUID())) {
                e.setCanceled(true);
                Plat.msg(e.getPlayer(), Component.translatable(MOD + ".tres.no_hablar").withStyle(ChatFormatting.DARK_GRAY), true);
            }
        });
        // en todo el capítulo (circuito, cuartos y teatro) nadie se lastima: aquí se muere solo en la vitrina
        MinecraftForge.EVENT_BUS.addListener((LivingHurtEvent e) -> {
            if (fase != Fase.NADA && e.getEntity() instanceof ServerPlayer sp && equipoDe(sp.getUUID()) != null) {
                e.setCanceled(true);
            }
        });
        Silla.permiso = Sentidos::puedeSentarse;
    }

    public static Fase fase() {
        return fase;
    }

    public static boolean abierta() {
        return fase != Fase.NADA;
    }

    public static Rol rol(UUID id) {
        Equipo eq = equipoDe(id);
        return eq == null ? Rol.NADA : eq.roles.getOrDefault(id, Rol.NADA);
    }

    private static boolean salio(UUID id) {
        Equipo eq = equipoDe(id);
        return eq != null && eq.etapa.ordinal() >= Etapa.SENTADOS.ordinal();
    }

    private static Equipo equipoDe(UUID id) {
        for (Equipo eq : equipos) {
            if (eq.ids.contains(id)) {
                return eq;
            }
        }
        return null;
    }

    /** ¿Ese jugador se puede sentar en esa silla? (las del circuito, solo los de su equipo al llegar a la salida; las demás, cualquiera) */
    private static boolean puedeSentarse(ServerPlayer sp, Silla s) {
        if (!s.grupo.startsWith("circ.")) {
            return !s.grupo.equals("vitrina");
        }
        Equipo eq = equipoDe(sp.getUUID());
        return eq != null && s.grupo.equals("circ." + eq.k) && eq.etapa == Etapa.SALIDA;
    }

    private static void manos(PlayerInteractEvent e) {
        if (e.getLevel().isClientSide() || fase == Fase.NADA) {
            return;
        }
        UUID id = e.getEntity().getUUID();
        if (fase == Fase.ELEGIR && e instanceof PlayerInteractEvent.RightClickBlock rc && e.getEntity() instanceof ServerPlayer sp) {
            if (tomar(sp, rc.getPos())) {
                e.setCanceled(true);
            }
            return;
        }
        // sentarse en la silla de la salida sí se puede con las esposas puestas
        if (e instanceof PlayerInteractEvent.EntityInteract ei && ei.getTarget() instanceof Silla) {
            return;
        }
        if (fase == Fase.JUEGO && rol(id) == Rol.ESPOSAS && !salio(id) && e.isCancelable()) {
            e.setCanceled(true);
            if (e instanceof PlayerInteractEvent.RightClickBlock && e.getEntity() instanceof ServerPlayer sp) {
                Plat.msg(sp, Component.translatable(MOD + ".tres.no_tocar").withStyle(ChatFormatting.DARK_GRAY), true);
                sp.playNotifySound(SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.6F, 0.8F);
            }
        }
    }

    // ------------------------------------------------------------------ preparar (al bajar los ascensores)

    /**
     * Prepara un circuito por equipo (el equipo i va al circuito i+1): la mesa con sus objetos, las puertas cerradas, el camino de las
     * baldosas y el tablero de las palancas al azar. Devuelve null si se pudo.
     */
    public static Component preparar(MinecraftServer s, List<List<Pieza>> grupos) {
        if (!Complejo.construido(s, "circ_1")) {
            return Component.translatable(MOD + ".sen.sin_mapa");
        }
        if (grupos.isEmpty()) {
            return Component.translatable(MOD + ".sen.sin_equipos");
        }
        Pruebas.cerrarOtras(s, "sentidos");
        limpiar(s);
        teatroComoNuevo(s);
        Evento ev = Evento.get(s);
        Random r = new Random(s.overworld().getRandom().nextLong());
        int n = Math.min(Circuito.CIRCUITOS, grupos.size());
        Map<UUID, Integer> voz = new HashMap<>();
        for (int i = 0; i < n; i++) {
            Equipo eq = new Equipo(i + 1);
            for (Pieza p : grupos.get(i)) {
                eq.ids.add(p.id);
                eq.roles.put(p.id, Rol.NADA);
                p.equipo = i;
                voz.put(p.id, eq.k);
            }
            for (int j = 0; j + 1 < eq.ids.size(); j++) {
                eq.cuerdas.add(new Cuerda());
            }
            // en parejas no hay cinta
            eq.objetos[0] = Rol.VENDA;
            eq.objetos[1] = eq.ids.size() >= 3 ? Rol.CINTA : null;
            eq.objetos[2] = Rol.ESPOSAS;
            caminoBaldosas(eq, r);
            // el tablero: al menos dos arriba y dos abajo
            int arriba;
            do {
                arriba = 0;
                for (int j = 0; j < Circuito.PALANCAS; j++) {
                    eq.patron[j] = r.nextBoolean();
                    arriba += eq.patron[j] ? 1 : 0;
                }
            } while (arriba < 2 || arriba > Circuito.PALANCAS - 2);
            equipos.add(eq);
            reponer(s, eq);
        }
        com.coleccionista.voz.Voz.separar(voz);
        ev.prueba(s, PRUEBA);
        ev.reloj(s, 0);
        ev.cambio(s);
        fase = Fase.LISTA;
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS preparada equipos={}", n);
        return null;
    }

    /**
     * El camino seguro de las baldosas: de la orilla de arriba a la de abajo, pasando siempre a la baldosa de enfrente o a la de al lado
     * (nunca en diagonal). En algunas filas el camino da un paso de lado (dos o tres baldosas seguidas en la misma fila).
     */
    static void caminoBaldosas(Equipo eq, Random r) {
        int w = Circuito.BALD_ANCHO, c = r.nextInt(w), vueltas = 0;
        for (boolean[] fila : eq.seguro) {
            java.util.Arrays.fill(fila, false);
        }
        for (int f = 0; f < Circuito.BALD_FILAS; f++) {
            eq.seguro[f][c] = true;
            // la primera y la última fila no se mueven de lado; en las demás, a veces sí (al menos 4 veces en todo el campo)
            boolean puede = f > 0 && f < Circuito.BALD_FILAS - 1;
            int faltan = Circuito.BALD_FILAS - 1 - f;
            if (puede && (r.nextFloat() < 0.55F || 4 - vueltas >= faltan)) {
                int d;
                do {
                    d = (r.nextBoolean() ? 1 : -1) * (1 + r.nextInt(3));
                } while (c + d < 0 || c + d >= w);
                int a = Math.min(c, c + d), b = Math.max(c, c + d);
                for (int x = a; x <= b; x++) {
                    eq.seguro[f][x] = true;
                }
                c += d;
                vueltas++;
            }
        }
    }

    /** Deja el circuito (y el cuarto del teatro) del equipo como nuevos. */
    private static void reponer(MinecraftServer s, Equipo eq) {
        ServerLevel l = s.overworld();
        for (int i = 0; i < 3; i++) {
            BlockPos p = Complejo.pos(s, eq.pre() + "objeto." + i);
            if (p != null) {
                Rol o = eq.objetos[i];
                l.setBlock(p, o == null ? Blocks.AIR.defaultBlockState()
                        : Complejo.estado(MOD + ":objeto_" + o.name().toLowerCase() + "[facing=north]"), 3);
            }
        }
        for (String d : new String[] {"puerta1", "puerta3", "puerta4"}) {
            puerta(s, eq.pre() + d, false);
        }
        puerta(s, eq.cuarto() + ".entrada", true);
        Cabina.puerta(s, eq.cuarto(), false);
        puerta(s, eq.palco() + ".entrada", false);
        Cabina.puerta(s, eq.palco(), false);
        baldosas(s, eq, false, null);
        for (int j = 0; j < Circuito.PALANCAS; j++) {
            BlockPos p = Complejo.pos(s, eq.pre() + "palanca." + j);
            BlockState st = p == null ? null : l.getBlockState(p);
            if (st != null && st.hasProperty(BlockStateProperties.POWERED) && st.getValue(BlockStateProperties.POWERED)) {
                l.setBlock(p, st.setValue(BlockStateProperties.POWERED, false), 3);
            }
        }
        tablero(s, eq, false);
    }

    /** Las dos hojas de una puerta ({@code marca.a} y {@code marca.b}). */
    private static void puerta(MinecraftServer s, String marca, boolean abrir) {
        ServerLevel l = s.overworld();
        for (String h : new String[] {".a", ".b"}) {
            BlockPos p = Complejo.pos(s, marca + h);
            if (p == null) {
                continue;
            }
            for (BlockPos q : new BlockPos[] {p, p.above()}) {
                BlockState st = l.getBlockState(q);
                if (st.getBlock() instanceof DoorBlock && st.getValue(DoorBlock.OPEN) != abrir) {
                    l.setBlock(q, st.setValue(DoorBlock.OPEN, abrir), 2 | 16);
                }
            }
        }
    }

    /** Las baldosas: apagadas, el camino en verde, o una trampa en rojo. */
    private static void baldosas(MinecraftServer s, Equipo eq, boolean camino, int[] roja) {
        ServerLevel l = s.overworld();
        BlockPos b = Complejo.pos(s, eq.pre() + "baldosa.0.0");
        if (b == null) {
            return;
        }
        BlockState off = Complejo.estado(MOD + ":marco_luz_apagado"), verde = Complejo.estado(MOD + ":marco_luz_verde"),
                rojo = Complejo.estado(MOD + ":marco_luz_roja");
        for (int f = 0; f < Circuito.BALD_FILAS; f++) {
            for (int c = 0; c < Circuito.BALD_ANCHO; c++) {
                BlockState st = roja != null && roja[0] == c && roja[1] == f ? rojo : camino && eq.seguro[f][c] ? verde : off;
                BlockPos p = b.offset(c, 0, f);
                if (l.getBlockState(p) != st) {
                    l.setBlock(p, st, 3);
                }
            }
        }
    }

    /** El tablero de la casilla de las palancas: prendido (verde arriba, rojo abajo) solo si alguien del equipo está adentro. */
    private static void tablero(MinecraftServer s, Equipo eq, boolean on) {
        ServerLevel l = s.overworld();
        BlockState off = Complejo.estado(MOD + ":marco_luz_apagado"), verde = Complejo.estado(MOD + ":marco_luz_verde"),
                rojo = Complejo.estado(MOD + ":marco_luz_roja");
        for (int j = 0; j < Circuito.PALANCAS; j++) {
            BlockPos q = Complejo.pos(s, eq.pre() + "tablero." + j);
            BlockState st = !on ? off : eq.patron[j] ? verde : rojo;
            if (q != null && l.getBlockState(q) != st) {
                l.setBlock(q, st, 3);
            }
        }
        eq.tablero = on;
    }

    // ------------------------------------------------------------------ llegada, discurso y la mesa

    /** Ya llegaron los ascensores: el Coleccionista espera a que todos salgan al taller (o lo habla ya, sin discurso). */
    public static void alLlegar(MinecraftServer s, boolean conDiscurso) {
        if (fase != Fase.LISTA) {
            return;
        }
        Pantallas.soloGrande(s, false);
        // los bots (solo en las pruebas) salen solos del ascensor a la mesa
        for (Equipo eq : equipos) {
            for (int i = 0; i < eq.ids.size(); i++) {
                Pieza p = Evento.get(s).pieza(eq.ids.get(i));
                BlockPos b = Complejo.pos(s, eq.pre() + "lugar." + Math.min(i, 2));
                if (p != null && p.bot && b != null && Evento.entidad(s, p) instanceof PiezaBot bot) {
                    bot.getNavigation().stop();
                    bot.moveTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 0F, 0F);
                }
            }
        }
        if (conDiscurso) {
            fase = Fase.ESPERA;
            esperaDesde = s.overworld().getGameTime();
            cambio = true;
        } else {
            elegir(s);
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS llegaron");
    }

    /** Todos salieron del ascensor al taller: el título del capítulo y habla el Coleccionista. */
    private static void hablar(MinecraftServer s) {
        fase = Fase.HABLANDO;
        cambio = true;
        Escena.presentacion(s, "I", MOD + ".sen.titulo", MOD + ".sen.lema");
        Pantallas.hablar(s, "sentidos", () -> {
            if (fase == Fase.HABLANDO) {
                elegir(s);
            }
        });
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS habla");
    }

    /** ¿Ya están todos (los vivos de cada equipo) en el taller, fuera del ascensor? */
    private static boolean todosEnTaller(MinecraftServer s) {
        for (Equipo eq : equipos) {
            BlockPos a = Complejo.pos(s, eq.pre() + "taller.a"), b = Complejo.pos(s, eq.pre() + "taller.b");
            if (a == null || b == null) {
                continue;
            }
            for (UUID id : eq.ids) {
                Entity e = ent(s, id);
                if (e == null) {
                    continue;
                }
                if (e.getX() < a.getX() || e.getX() > b.getX() + 1 || e.getZ() < a.getZ() || e.getZ() > b.getZ() + 1) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Para el organizador: corta el discurso y pasa a la mesa (o de la mesa al recorrido, o adelanta el final). */
    public static boolean saltar(MinecraftServer s) {
        switch (fase) {
            case LISTA -> {
                alLlegar(s, false);
                return true;
            }
            case ESPERA -> {
                hablar(s);
                return true;
            }
            case HABLANDO -> {
                Pantallas.callar(s);
                elegir(s);
                return true;
            }
            case ELEGIR -> {
                arrancar(s);
                return true;
            }
            case FINAL -> {
                // el telón ya, aunque falte alguien por salir de su cuarto
                if (!telon && aVitrinas) {
                    telonT0 = s.overworld().getGameTime();
                    telon = true;
                    return true;
                }
                return false;
            }
            case FIN -> {
                cerrarSalida(s);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static void elegir(MinecraftServer s) {
        fase = Fase.ELEGIR;
        t = 0;
        Evento ev = Evento.get(s);
        ev.reloj(s, segundosElegir);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".sen.pantalla.elegir").getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".sen.chat.elegir", segundosElegir)));
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.6F);
        Random r = new Random(s.overworld().getRandom().nextLong());
        long now = s.overworld().getGameTime();
        for (Equipo eq : equipos) {
            for (UUID id : eq.ids) {
                Pieza p = ev.pieza(id);
                if (p != null && p.bot) {
                    botElige.put(id, now + 40 + r.nextInt(Math.max(1, segundosElegir * 20 / 4)));
                }
            }
        }
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS elegir");
    }

    /** Alguien tomó un objeto de su mesa. */
    static boolean tomar(ServerPlayer sp, BlockPos pos) {
        MinecraftServer s = Plat.server(sp);
        Equipo eq = equipoDe(sp.getUUID());
        if (eq == null) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            if (pos.equals(Complejo.pos(s, eq.pre() + "objeto." + i)) && eq.objetos[i] != null) {
                if (eq.roles.get(sp.getUUID()) != Rol.NADA) {
                    Plat.msg(sp, Component.translatable(MOD + ".tres.ya_tienes").withStyle(ChatFormatting.GRAY), true);
                    return true;
                }
                asignar(s, eq, sp.getUUID(), i);
                return true;
            }
        }
        return false;
    }

    private static void asignar(MinecraftServer s, Equipo eq, UUID id, int i) {
        Rol r = eq.objetos[i];
        eq.objetos[i] = null;
        eq.roles.put(id, r);
        BlockPos p = Complejo.pos(s, eq.pre() + "objeto." + i);
        if (p != null) {
            s.overworld().setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            s.overworld().playSound(null, p, r == Rol.ESPOSAS ? SoundEvents.CHAIN_PLACE : SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1F, 0.8F);
        }
        Pieza pz = Evento.get(s).pieza(id);
        for (UUID o : eq.ids) {
            ServerPlayer sp = s.getPlayerList().getPlayer(o);
            if (sp != null && pz != null) {
                Plat.msg(sp, Anuncio.nombre(pz).append(Component.translatable(MOD + ".tres.tomo." + r.name().toLowerCase()).withStyle(ChatFormatting.GRAY)),
                        false);
            }
        }
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS rol {} {}", pz == null ? id : pz.etiqueta(), r);
    }

    /** Para la prueba automática: que esa pieza tome el objeto i de su mesa. */
    public static boolean tomarComo(MinecraftServer s, Pieza p, int i) {
        Equipo eq = equipoDe(p.id);
        if (eq == null || fase != Fase.ELEGIR || i < 0 || i > 2 || eq.objetos[i] == null || eq.roles.get(p.id) != Rol.NADA) {
            return false;
        }
        asignar(s, eq, p.id, i);
        return true;
    }

    private static boolean todosEligieron() {
        for (Equipo eq : equipos) {
            for (Rol r : eq.roles.values()) {
                if (r == Rol.NADA) {
                    return false;
                }
            }
        }
        return !equipos.isEmpty();
    }

    // ------------------------------------------------------------------ el recorrido

    private static void arrancar(MinecraftServer s) {
        // a quien no eligió le toca lo que sobró en su mesa
        for (Equipo eq : equipos) {
            for (UUID id : eq.ids) {
                if (eq.roles.get(id) == Rol.NADA) {
                    for (int i = 0; i < 3; i++) {
                        if (eq.objetos[i] != null) {
                            asignar(s, eq, id, i);
                            break;
                        }
                    }
                }
            }
        }
        fase = Fase.JUEGO;
        t = 0;
        Evento ev = Evento.get(s);
        ev.reloj(s, segundos);
        Random r = new Random(s.overworld().getRandom().nextLong());
        long now = s.overworld().getGameTime();
        for (Equipo eq : equipos) {
            puerta(s, eq.pre() + "puerta1", true);
            eq.etapa = Etapa.POSTES;
            boolean soloBots = true;
            for (UUID id : eq.ids) {
                Pieza p = ev.pieza(id);
                soloBots &= p != null && p.bot;
            }
            if (soloBots && botsSalen) {
                eq.botListo = now + 20L * (botMin + r.nextInt(Math.max(1, botMax - botMin)));
            }
        }
        actualizarVoz(s);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".sen.pantalla.juego").getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".sen.chat.empieza", Math.max(1, (segundos + 30) / 60))));
        Anuncio.sonido(s, SoundEvents.CHAIN_BREAK, 1F, 0.5F);
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS empieza");
    }

    /** Cada equipo solo se oye entre sí; la cinta corta el micrófono. Desde que se abre el telón, todos se oyen. */
    private static void actualizarVoz(MinecraftServer s) {
        Map<UUID, Integer> voz = new HashMap<>();
        Set<UUID> mudos = new HashSet<>();
        for (Equipo eq : equipos) {
            boolean juntos = telon || fase == Fase.FIN;
            for (UUID id : eq.ids) {
                voz.put(id, juntos ? 99 : eq.k);
                if (fase == Fase.JUEGO && !salio(id) && eq.roles.get(id) == Rol.CINTA) {
                    mudos.add(id);
                }
            }
        }
        com.coleccionista.voz.Voz.separar(voz);
        com.coleccionista.voz.Voz.mudos(mudos);
    }

    /** ¿Cuenta para las trampas, las caídas y la cadena? (el organizador probando en espectador o creativo, no) */
    private static boolean juega(Entity e) {
        return !(e instanceof ServerPlayer sp) || !sp.isSpectator() && !sp.isCreative();
    }

    private static Entity ent(MinecraftServer s, UUID id) {
        Pieza p = Evento.get(s).pieza(id);
        return p == null || !p.vivo() ? null : Evento.entidad(s, p);
    }

    private static void tick(MinecraftServer s) {
        if (fase == Fase.NADA) {
            return;
        }
        long now = s.overworld().getGameTime();
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            if (cambio) {
                enviar(s);
            }
            return;
        }
        if (fase == Fase.ESPERA && now % 10 == 0 && (todosEnTaller(s) || now - esperaDesde >= esperaTaller)) {
            hablar(s);
        }
        if (fase == Fase.ELEGIR) {
            for (Map.Entry<UUID, Long> b : new ArrayList<>(botElige.entrySet())) {
                if (now >= b.getValue()) {
                    botElige.remove(b.getKey());
                    Equipo eq = equipoDe(b.getKey());
                    if (eq != null && eq.roles.get(b.getKey()) == Rol.NADA) {
                        for (int i = 0; i < 3; i++) {
                            if (eq.objetos[i] != null) {
                                asignar(s, eq, b.getKey(), i);
                                break;
                            }
                        }
                    }
                }
            }
            if (todosEligieron()) {
                Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".tres.chat.listos")));
                arrancar(s);
            }
        }
        if (fase == Fase.JUEGO) {
            t++;
            for (Equipo eq : equipos) {
                circuito(s, eq, now);
            }
            revisarFin(s);
        }
        if (fase == Fase.FINAL || fase == Fase.FIN) {
            t++;
            elFinal(s, now);
        }
        if (cambio || now % 10 == 0 && fase == Fase.JUEGO || now % 40 == 0) {
            enviar(s);
        }
    }

    /** Lo que pasa en el circuito de un equipo: la cadena, las caídas, las baldosas, las palancas y la salida. */
    private static void circuito(MinecraftServer s, Equipo eq, long now) {
        BlockPos o = Complejo.origen(s, "circ_" + eq.k);
        if (o == null || eq.etapa == Etapa.FUERA || eq.etapa == Etapa.MESA) {
            return;
        }
        List<Entity> ents = new ArrayList<>();
        for (UUID id : eq.ids) {
            Entity e = ent(s, id);
            if (e != null) {
                ents.add(e);
            }
        }
        // la cadena (mientras no estén sentados): se enreda en los postes
        if (eq.etapa.ordinal() < Etapa.SENTADOS.ordinal()) {
            List<double[]> postes = new ArrayList<>();
            for (double[] q : Circuito.postes()) {
                postes.add(new double[] {q[0] + o.getX(), q[1] + o.getZ()});
            }
            for (int i = 0; i < eq.cuerdas.size(); i++) {
                Entity a = ent(s, eq.ids.get(i)), b = ent(s, eq.ids.get(i + 1));
                if (a == null || b == null) {
                    continue;
                }
                Cuerda c = eq.cuerdas.get(i);
                int antes = c.pivotes.size();
                c.actualizar(a.getX(), a.getZ(), b.getX(), b.getZ(), postes, 0.45);
                if (c.pivotes.size() != antes) {
                    cambio = true;
                }
                // por si alguien se escapa (lag): si se pasó mucho del largo, regresa junto a su compañero
                if (c.largo(a.getX(), a.getZ(), b.getX(), b.getZ()) > largo + 5 && !b.isPassenger() && juega(a) && juega(b)) {
                    b.teleportTo(a.getX(), a.getY(), a.getZ());
                    c.pivotes.clear();
                }
            }
        }
        // red de seguridad: nadie se queda abajo del piso (si algo falla, regresa al último punto seguro con su equipo)
        if (now % 10 == 0 && eq.etapa.ordinal() < Etapa.SENTADOS.ordinal()) {
            for (Entity e : ents) {
                if (juega(e) && e.getY() - o.getY() < -3.5) {
                    volver(s, eq, ck(eq));
                    Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS red equipo={}", eq.k);
                    break;
                }
            }
        }
        // bots en un equipo con personas (para probar con pocos): van pegados a la persona más cercana de su equipo y nunca la hacen
        // perder (no pisan baldosas malas ni caen a la zanja)
        if (now % 20 == 0 && eq.etapa.ordinal() < Etapa.SALIDA.ordinal()) {
            seguirPersona(eq, ents);
        }
        // bots: llegan solos a la salida (solo para las pruebas)
        if (eq.botListo > 0 && now >= eq.botListo && eq.etapa.ordinal() < Etapa.SALIDA.ordinal()) {
            eq.botListo = -1;
            saltarA(s, eq, Etapa.SALIDA);
        }
        if (now % 2 != 0) {
            return;
        }
        switch (eq.etapa) {
            case POSTES -> {
                if (todosMas(ents, o, Circuito.Z_PASARELA)) {
                    eq.etapa = Etapa.PASARELA;
                    avisar(s, eq, MOD + ".sen.pasarela");
                    cambio = true;
                    Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS pasarela equipo={}", eq.k);
                }
            }
            case PASARELA -> {
                // si alguien cae a la zanja (no se lastima), todo el equipo regresa al principio de la pasarela
                for (Entity e : ents) {
                    if (!juega(e) || acompanante(eq, e, ents)) {
                        continue;
                    }
                    double lx = e.getX() - o.getX(), lz = e.getZ() - o.getZ(), ly = e.getY() - o.getY();
                    if (ly < 0.4 && lx >= Circuito.ZANJA_X0 && lx <= Circuito.ZANJA_X1 + 1 && lz >= Circuito.ZANJA_Z0 - 0.3
                            && lz <= Circuito.ZANJA_Z1 + 1.3) {
                        cayo(s, eq, "ck.pasarela", MOD + ".sen.cayo");
                        return;
                    }
                }
                if (todosMas(ents, o, Circuito.Z_PASARELA_FIN)) {
                    eq.etapa = Etapa.BALDOSAS;
                    eq.verHasta = now + 120;
                    baldosas(s, eq, true, null);
                    avisar(s, eq, MOD + ".sen.baldosas");
                    sonar(s, eq, SoundEvents.BEACON_POWER_SELECT, 1F, 1.2F);
                    cambio = true;
                    Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS baldosas equipo={}", eq.k);
                }
            }
            case BALDOSAS -> baldosasTick(s, eq, ents, o, now);
            case PALANCAS -> {
                if (now % 10 != 0) {
                    return;
                }
                // el tablero solo se ve si alguien del equipo está dentro de la casilla
                boolean dentro = false;
                for (Entity e : ents) {
                    double lx = e.getX() - o.getX(), lz = e.getZ() - o.getZ();
                    dentro |= lx >= Circuito.CAS_X0 && lx <= Circuito.CAS_X1 + 1 && lz >= Circuito.CAS_Z0 && lz <= Circuito.CAS_Z1 + 1;
                }
                if (dentro != eq.tablero) {
                    tablero(s, eq, dentro);
                    if (dentro) {
                        sonar(s, eq, SoundEvents.BEACON_POWER_SELECT, 0.5F, 1.6F);
                    }
                }
                boolean bien = true;
                for (int j = 0; j < Circuito.PALANCAS; j++) {
                    BlockPos p = Complejo.pos(s, eq.pre() + "palanca." + j);
                    BlockState st = p == null ? null : s.overworld().getBlockState(p);
                    boolean arriba = st != null && st.hasProperty(BlockStateProperties.POWERED) && st.getValue(BlockStateProperties.POWERED);
                    bien &= arriba == eq.patron[j];
                }
                if (bien) {
                    puerta(s, eq.pre() + "puerta4", true);
                    sonar(s, eq, SoundEvents.PLAYER_LEVELUP, 0.6F, 0.6F);
                    sonar(s, eq, SoundEvents.IRON_DOOR_OPEN, 1F, 0.6F);
                    alaSalida(s, eq);
                }
            }
            case SALIDA -> salidaTick(s, eq, ents, now);
            case SENTADOS -> {
                if (!eq.avisoEspera && now - eq.termino > 60) {
                    eq.avisoEspera = true;
                    avisar(s, eq, MOD + ".sen.esperen");
                }
            }
            default -> {
            }
        }
    }

    /** Los bots del equipo se juntan con la persona más cercana del equipo (si hay alguna). */
    private static void seguirPersona(Equipo eq, List<Entity> ents) {
        List<Entity> personas = new ArrayList<>();
        for (Entity e : ents) {
            if (e instanceof ServerPlayer sp && juega(sp)) {
                personas.add(e);
            }
        }
        if (personas.isEmpty()) {
            return;
        }
        int k = 0;
        for (Entity e : ents) {
            if (!(e instanceof PiezaBot b) || b.isPassenger()) {
                continue;
            }
            Entity cerca = personas.get(0);
            for (Entity p : personas) {
                if (p.distanceToSqr(b) < cerca.distanceToSqr(b)) {
                    cerca = p;
                }
            }
            if (cerca.distanceToSqr(b) > 9 || Math.abs(cerca.getY() - b.getY()) > 1.5) {
                double r = Math.toRadians(cerca.getYRot() + 180 + (k % 2 == 0 ? 25 : -25));
                b.getNavigation().stop();
                b.moveTo(cerca.getX() - Math.sin(r) * 0.6, cerca.getY(), cerca.getZ() + Math.cos(r) * 0.6, cerca.getYRot(), 0F);
                b.setDeltaMovement(Vec3.ZERO);
                k++;
            }
        }
    }

    /** ¿Ese bot va con una persona de su equipo? (entonces no cuenta para caer ni para pisar baldosas). */
    private static boolean acompanante(Equipo eq, Entity e, List<Entity> ents) {
        if (!(e instanceof PiezaBot)) {
            return false;
        }
        for (Entity o : ents) {
            if (o instanceof ServerPlayer sp && juega(sp)) {
                return true;
            }
        }
        return false;
    }

    /** El último punto seguro según en qué va el equipo. */
    private static String ck(Equipo eq) {
        return switch (eq.etapa) {
            case PASARELA -> "ck.pasarela";
            case BALDOSAS -> "ck.baldosas";
            case PALANCAS, SALIDA -> "ck.palancas";
            default -> "ck.postes";
        };
    }

    /** ¿Todos los del equipo ya pasaron de esa z (local)? */
    private static boolean todosMas(List<Entity> ents, BlockPos o, double z) {
        if (ents.isEmpty()) {
            return false;
        }
        for (Entity e : ents) {
            if (e.getZ() - o.getZ() < z) {
                return false;
            }
        }
        return true;
    }

    /** Alguien se equivocó (cayó a la zanja o pisó una baldosa mala): todo el equipo regresa al punto seguro, sin lastimarse. */
    private static void cayo(MinecraftServer s, Equipo eq, String ck, String aviso) {
        volver(s, eq, ck);
        for (UUID id : eq.ids) {
            if (ent(s, id) instanceof ServerPlayer sp) {
                sp.playNotifySound(SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1F, 0.6F);
                Fx.shake(sp, 0.6F, 8);
            }
        }
        avisar(s, eq, aviso);
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS cayo equipo={} ck={}", eq.k, ck);
    }

    /** Todo el equipo a una marca del circuito, en fila y mirando al frente; la cadena queda derecha. */
    private static void volver(MinecraftServer s, Equipo eq, String ck) {
        BlockPos b = Complejo.pos(s, eq.pre() + ck);
        if (b == null) {
            return;
        }
        for (int i = 0; i < eq.ids.size(); i++) {
            Entity e = ent(s, eq.ids.get(i));
            if (e == null || e.isPassenger()) {
                continue;
            }
            double x = b.getX() + 0.5 + (i - (eq.ids.size() - 1) / 2.0) * 1.2, y = b.getY(), z = b.getZ() + 0.5;
            e.fallDistance = 0;
            e.setDeltaMovement(Vec3.ZERO);
            if (e instanceof ServerPlayer sp) {
                sp.teleportTo(s.overworld(), x, y, z, 0F, 0F);
            } else {
                e.moveTo(x, y, z, 0F, 0F);
            }
        }
        for (Cuerda c : eq.cuerdas) {
            c.pivotes.clear();
        }
        cambio = true;
    }

    private static void baldosasTick(MinecraftServer s, Equipo eq, List<Entity> ents, BlockPos o, long now) {
        if (eq.verHasta > 0 && now >= eq.verHasta) {
            eq.verHasta = -1;
            baldosas(s, eq, false, null);
            sonar(s, eq, SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.4F);
        }
        if (eq.trampaHasta > 0 && now >= eq.trampaHasta) {
            eq.trampaHasta = -1;
            eq.trampa = null;
            baldosas(s, eq, eq.verHasta > 0, null);
        }
        // la palanca de "ver otra vez" (la jala quien tenga manos): enseña el camino 4 segundos y se regresa sola
        BlockPos ver = Complejo.pos(s, eq.pre() + "ver");
        BlockState vs = ver == null ? null : s.overworld().getBlockState(ver);
        if (vs != null && vs.hasProperty(BlockStateProperties.POWERED) && vs.getValue(BlockStateProperties.POWERED)) {
            s.overworld().setBlock(ver, vs.setValue(BlockStateProperties.POWERED, false), 3);
            eq.verHasta = now + 80;
            baldosas(s, eq, true, null);
            sonar(s, eq, SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.3F);
        }
        // ¿alguien pisó una baldosa mala?
        BlockPos b = Complejo.pos(s, eq.pre() + "baldosa.0.0");
        if (b != null && eq.trampaHasta < 0) {
            for (Entity e : ents) {
                if (!juega(e) || acompanante(eq, e, ents)) {
                    continue;
                }
                double ly = e.getY() - b.getY();
                int c = (int) Math.floor(e.getX() - b.getX()), f = (int) Math.floor(e.getZ() - b.getZ());
                if (ly < 0.9 || ly > 1.3 || !e.onGround() || f < 0 || f >= Circuito.BALD_FILAS || c < 0 || c >= Circuito.BALD_ANCHO) {
                    continue;
                }
                if (!eq.seguro[f][c]) {
                    eq.trampa = new int[] {c, f};
                    eq.trampaHasta = now + 30;
                    baldosas(s, eq, false, eq.trampa);
                    BlockPos p = b.offset(c, 1, f);
                    s.overworld().sendParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 0.2, p.getZ() + 0.5, 30, 0.3, 0.4, 0.3, 0.05);
                    s.overworld().playSound(null, p, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1F, 0.6F);
                    cayo(s, eq, "ck.baldosas", MOD + ".sen.trampa");
                    return;
                }
            }
        }
        if (todosMas(ents, o, Circuito.Z_BALD_FIN)) {
            puerta(s, eq.pre() + "puerta3", true);
            eq.etapa = Etapa.PALANCAS;
            baldosas(s, eq, false, null);
            tablero(s, eq, false);
            avisar(s, eq, MOD + ".sen.palancas");
            cambio = true;
            Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS palancas equipo={}", eq.k);
        }
    }

    /** Una silla en su lugar (el punto local u, w de un cuarto de salida o de su copia del teatro). */
    private static Silla silla(MinecraftServer s, String cuarto, int i, String grupo) {
        ServerLevel l = s.overworld();
        BlockPos o = Complejo.pos(s, "cab." + cuarto + ".origen");
        Silla si = ModRegistry.SILLA.get().create(l);
        if (o == null || si == null) {
            return null;
        }
        Vec3 p = local(o, Complejo.yaw("cab." + cuarto + ".origen"), Salidas.silla(i)[0], Salidas.silla(i)[1]);
        float yaw = Complejo.yaw("cab." + cuarto + ".origen");
        si.moveTo(p.x, o.getY(), p.z, yaw, 0F);
        si.setYHeadRot(yaw);
        si.grupo = grupo;
        l.addFreshEntity(si);
        return si;
    }

    private static Vec3 local(BlockPos o, float yaw, double u, double w) {
        double r = Math.toRadians(yaw), c = Math.round(Math.cos(r)), sn = Math.round(Math.sin(r));
        return new Vec3(o.getX() + u * c - w * sn, o.getY(), o.getZ() + u * sn + w * c);
    }

    /** Se abrió la última puerta: aparecen las sillas en el cuarto de la salida (una por persona). */
    private static void alaSalida(MinecraftServer s, Equipo eq) {
        eq.etapa = Etapa.SALIDA;
        for (Silla si : eq.sillas) {
            si.discard();
        }
        eq.sillas.clear();
        for (int i = 0; i < eq.ids.size() && i < 3; i++) {
            Silla si = silla(s, eq.cuarto(), i, "circ." + eq.k);
            if (si != null) {
                eq.sillas.add(si);
            }
        }
        puerta(s, eq.cuarto() + ".entrada", true);
        avisar(s, eq, MOD + ".sen.salida");
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS salida equipo={} sillas={}", eq.k, eq.sillas.size());
    }

    /** Para el organizador y las pruebas: lleva al equipo directo a una etapa (abre las puertas de antes). */
    static void saltarA(MinecraftServer s, Equipo eq, Etapa e) {
        if (eq.etapa.ordinal() >= e.ordinal()) {
            return;
        }
        for (String d : new String[] {"puerta1", "puerta3", "puerta4"}) {
            puerta(s, eq.pre() + d, true);
        }
        if (e == Etapa.SALIDA) {
            volver(s, eq, "salida");
            alaSalida(s, eq);
        }
    }

    private static void salidaTick(MinecraftServer s, Equipo eq, List<Entity> ents, long now) {
        // los bots se sientan solos
        for (Entity e : ents) {
            if (e instanceof PiezaBot && !e.isPassenger() && now % 20 == 0) {
                for (Silla si : eq.sillas) {
                    if (si.isAlive() && !si.isVehicle()) {
                        e.startRiding(si, true);
                        break;
                    }
                }
            }
        }
        // ¿ya están todos sentados?
        int sentados = 0;
        for (Entity e : ents) {
            if (e.getVehicle() instanceof Silla si && eq.sillas.contains(si)) {
                sentados++;
            }
        }
        if (ents.isEmpty() || sentados < ents.size()) {
            return;
        }
        // las correas se cierran y la puerta de atrás también: ¿son los últimos?
        int faltan = 0;
        for (Equipo o : equipos) {
            if (o != eq && o.etapa.ordinal() < Etapa.SENTADOS.ordinal()) {
                faltan++;
            }
        }
        eq.perdio = faltan == 0 && (equipos.size() > 1 || pierdeSolo || Evento.get(s).pocos);
        eq.etapa = Etapa.SENTADOS;
        eq.termino = now;
        for (Silla si : eq.sillas) {
            si.cerrar(true);
        }
        puerta(s, eq.cuarto() + ".entrada", false);
        sonar(s, eq, SoundEvents.IRON_TRAPDOOR_CLOSE, 1F, 0.5F);
        if (!eq.perdio) {
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".sen.chat.salio", eq.k)));
            Anuncio.sonido(s, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.5F, 1F);
        }
        actualizarVoz(s);
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS sentados equipo={} ultimo={}", eq.k, eq.perdio);
    }

    /** Pasa a alguien sentado de una silla a otro lugar, sentado en la misma silla. */
    private static void llevarSentado(MinecraftServer s, Silla si, Vec3 dest, float yaw) {
        List<Entity> pas = new ArrayList<>(si.getPassengers());
        boolean cerrada = si.cerrada();
        si.soltar();
        si.irA(null, 0);
        si.moveTo(dest.x, dest.y, dest.z, yaw, 0F);
        si.setYHeadRot(yaw);
        for (Entity e : pas) {
            e.fallDistance = 0;
            if (e instanceof ServerPlayer sp) {
                sp.teleportTo(s.overworld(), dest.x, dest.y + 0.5, dest.z, yaw, 0F);
            } else {
                e.moveTo(dest.x, dest.y + 0.5, dest.z, yaw, 0F);
            }
            e.startRiding(si, true);
        }
        si.cerrar(cerrada, false);
    }

    /** ¿Ya terminaron todos (sentados en su cuarto)? Entonces empieza el final. */
    private static void revisarFin(MinecraftServer s) {
        if (fase != Fase.JUEGO) {
            return;
        }
        for (Equipo eq : equipos) {
            if (eq.etapa.ordinal() < Etapa.SENTADOS.ordinal()) {
                return;
            }
        }
        iniciarFinal(s);
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        if (fase == Fase.ELEGIR) {
            arrancar(s);
            return true;
        }
        if (fase != Fase.JUEGO) {
            return false;
        }
        terminarYa(s);
        return true;
    }

    /**
     * Se acabó el tiempo (o lo pide el organizador): los equipos que no llegaron pierden. Se les va la vista un momento y aparecen sentados
     * en su cuarto de la salida (ahí les llega el gas, como al último equipo).
     */
    public static boolean terminarYa(MinecraftServer s) {
        if (fase != Fase.JUEGO) {
            return false;
        }
        List<Equipo> faltan = new ArrayList<>();
        for (Equipo eq : equipos) {
            if (eq.etapa.ordinal() < Etapa.SENTADOS.ordinal()) {
                faltan.add(eq);
            }
        }
        // solo pierde UN equipo: el que va más atrás (si ya hay un último sentado, nadie más pierde); los demás que no llegaron se
        // salvan y aparecen sentados en su salida como los que sí llegaron
        boolean yaHayUltimo = false;
        for (Equipo eq : equipos) {
            yaHayUltimo |= eq.perdio;
        }
        Equipo peor = null;
        if (!yaHayUltimo && (equipos.size() > 1 || pierdeSolo || Evento.get(s).pocos)) {
            for (Equipo eq : faltan) {
                if (peor == null || avance(s, eq) < avance(s, peor)) {
                    peor = eq;
                }
            }
        }
        for (Equipo eq : faltan) {
            eq.perdio = eq == peor;
            eq.etapa = Etapa.SENTADOS;
            eq.termino = s.overworld().getGameTime();
            for (Silla si : eq.sillas) {
                si.discard();
            }
            eq.sillas.clear();
            List<ServerPlayer> ps = new ArrayList<>();
            for (int i = 0; i < eq.ids.size(); i++) {
                Entity e = ent(s, eq.ids.get(i));
                Silla si = silla(s, eq.cuarto(), Math.min(i, 2), "circ." + eq.k);
                if (e == null || si == null) {
                    continue;
                }
                if (e instanceof ServerPlayer sp) {
                    ps.add(sp);
                }
                eq.sillas.add(si);
                if (e.isPassenger()) {
                    e.stopRiding();
                }
                if (e instanceof ServerPlayer sp) {
                    sp.teleportTo(s.overworld(), si.getX(), si.getY() + 0.5, si.getZ(), si.getYRot(), 0F);
                } else {
                    e.moveTo(si.getX(), si.getY() + 0.5, si.getZ(), si.getYRot(), 0F);
                }
                e.startRiding(si, true);
                si.cerrar(true);
            }
            puerta(s, eq.cuarto() + ".entrada", false);
            Escena.fundido(ps, "", 50);
        }
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".sen.chat.tiempo")));
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS tiempo faltaban={} pierde={}", faltan.size(), peor == null ? "-" : peor.k);
        revisarFin(s);
        return true;
    }

    private static net.minecraft.nbt.ListTag punto(double x, double y, double z) {
        net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag();
        l.add(net.minecraft.nbt.DoubleTag.valueOf(x));
        l.add(net.minecraft.nbt.DoubleTag.valueOf(y));
        l.add(net.minecraft.nbt.DoubleTag.valueOf(z));
        return l;
    }

    /** Qué tan lejos llegó un equipo (la etapa y, dentro de ella, lo que avanzó el más atrasado). */
    private static double avance(MinecraftServer s, Equipo eq) {
        BlockPos o = Complejo.origen(s, "circ_" + eq.k);
        double z = 1000;
        for (UUID id : eq.ids) {
            Entity e = ent(s, id);
            if (e != null && o != null) {
                z = Math.min(z, e.getZ() - o.getZ());
            }
        }
        return eq.etapa.ordinal() * 1000 + (z >= 1000 ? 0 : z);
    }

    // ------------------------------------------------------------------ el final: el teatro de la colección

    private static void iniciarFinal(MinecraftServer s) {
        fase = Fase.FINAL;
        finT0 = s.overworld().getGameTime();
        Evento.get(s).reloj(s, 0);
        Evento.get(s).prueba(s, Component.translatable(MOD + ".sen.prueba_final").getString());
        Pantallas.modo(s, "apagada", "");
        palcos = levantan = gas = aVitrinas = despiertan = telon = hablo = ceraAviso = endurecida = rotas = caen = golpe = salidaAbierta = antesala = false;
        telonT0 = ceraT0 = finalFin = -1;
        telonPaso = 0;
        capas = 0;
        cajas = 0;
        victimas.clear();
        estatuas.clear();
        cambio = true;
        int pierden = 0;
        for (Equipo eq : equipos) {
            pierden += eq.perdio ? 1 : 0;
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS final equipos={} pierden={}", equipos.size(), pierden);
    }

    /** El final, paso a paso (cada paso una sola vez, en cuanto llega su momento). */
    private static void elFinal(MinecraftServer s, long now) {
        long dt = now - finT0;
        ServerLevel l = s.overworld();
        // 1) los que se salvaron pasan (sentados, sin que se note) a su cuarto del teatro; un momento después se abre la puerta
        if (!palcos && dt >= 40) {
            palcos = true;
            for (Equipo eq : equipos) {
                if (!eq.perdio) {
                    List<Entity> van = new ArrayList<>();
                    for (UUID id : eq.ids) {
                        Entity e = ent(s, id);
                        if (e != null) {
                            van.add(e);
                        }
                    }
                    Cabina.trasladar(s, eq.cuarto(), eq.palco(), van, eq.sillas);
                    for (Silla si : eq.sillas) {
                        si.grupo = "teatro";
                    }
                    eq.etapa = Etapa.FUERA;
                }
            }
            Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS palcos");
        }
        if (!levantan && dt >= 80) {
            levantan = true;
            for (Equipo eq : equipos) {
                if (!eq.perdio) {
                    for (Silla si : eq.sillas) {
                        si.cerrar(false);
                    }
                    Cabina.puerta(s, eq.palco(), true);
                    botsALaSala(s, eq);
                    avisar(s, eq, MOD + ".sen.final.salgan");
                    sonar(s, eq, SoundEvents.IRON_TRAPDOOR_OPEN, 1F, 0.6F);
                }
            }
        }
        // 2) al último equipo se le llena el cuarto de gas: se desmaya
        if (!gas && dt >= 40) {
            gas = true;
            for (Equipo eq : equipos) {
                if (eq.perdio) {
                    for (UUID id : eq.ids) {
                        ServerPlayer sp = s.getPlayerList().getPlayer(id);
                        if (sp != null) {
                            CompoundTag g = new CompoundTag();
                            g.putString("fase", "gas");
                            g.putLong("desde", now);
                            g.putInt("dura", 150);
                            PlayerStore.Sync.toPlayer(sp, KEY_GAS, g);
                        }
                    }
                }
            }
            Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS gas");
        }
        if (gas && !aVitrinas && dt < 200 && dt % 3 == 0) {
            for (Equipo eq : equipos) {
                if (!eq.perdio) {
                    continue;
                }
                for (int n = 0; n < 4; n++) {
                    BlockPos v = Complejo.pos(s, eq.cuarto() + ".ventila." + n);
                    if (v == null) {
                        continue;
                    }
                    for (int i = 0; i < 4; i++) {
                        l.sendParticles(ParticleTypes.ENTITY_EFFECT, v.getX() + 0.5 + (l.random.nextDouble() - 0.5) * 2, v.getY() - 0.3
                                - l.random.nextDouble() * 2.5, v.getZ() + 0.5 + (l.random.nextDouble() - 0.5) * 2, 0, 0.42, 0.72, 0.22, 1);
                    }
                    if (dt % 12 == 0) {
                        l.playSound(null, v, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 0.45F + l.random.nextFloat() * 0.1F);
                    }
                }
            }
        }
        // 3) dormidos: a las vitrinas del escenario (sentados, con la correa); el frente se cierra
        if (!aVitrinas && dt >= 210) {
            aVitrinas = true;
            aLasVitrinas(s);
        }
        // 4) despiertan en la vitrina, con el mensajito
        if (!despiertan && dt >= 250) {
            despiertan = true;
            for (Victima v : victimas) {
                ServerPlayer sp = s.getPlayerList().getPlayer(v.id);
                if (sp != null) {
                    CompoundTag g = new CompoundTag();
                    g.putString("fase", "despierta");
                    g.putLong("desde", now);
                    g.putInt("dura", 160);
                    PlayerStore.Sync.toPlayer(sp, KEY_GAS, g);
                }
            }
        }
        // 5) el telón: cuando ya salieron todos de su cuarto (o pasó un buen rato)
        if (aVitrinas && !telon && dt >= 450 && (todosEnLaSala(s) || dt >= 1200)) {
            telon = true;
            telonT0 = now;
        }
        if (telon && telonT0 > 0) {
            abrirTelon(s, now);
        }
        // 6) la cera (o, si no hay nadie en las vitrinas, directo a la salida)
        if (hablo && ceraT0 > 0) {
            cera(s, now);
        }
        if (finalFin < 0 && telon && telonPaso > (SalaVitrina.TELON_X1 - SalaVitrina.TELON_X0) / 2 + 2 && victimas.isEmpty() && !salidaAbierta) {
            abrirSalida(s);
        }
        if (fase == Fase.FIN && now % 10 == 0) {
            vigilarSalida(s);
        }
    }

    /** Los bots que se salvaron (solo en pruebas) se levantan y bajan a la sala, frente al vidrio, cada uno en su lugar. */
    private static void botsALaSala(MinecraftServer s, Equipo eq) {
        BlockPos o = Complejo.origen(s, "vitrina");
        if (o == null) {
            return;
        }
        int i = 0;
        for (UUID id : eq.ids) {
            if (ent(s, id) instanceof PiezaBot b) {
                b.stopRiding();
                double x = o.getX() + 8 + ((eq.k * 3 + i) * 3) % 40, z = o.getZ() + 34.5;
                b.moveTo(x + 0.5, o.getY() + 1, z, 0F, 0F);
                i++;
            }
        }
    }

    /** ¿Ya salieron todos los que se salvaron de su cuarto del teatro (al palco o a la sala)? */
    private static boolean todosEnLaSala(MinecraftServer s) {
        BlockPos o = Complejo.origen(s, "vitrina");
        if (o == null) {
            return true;
        }
        for (Equipo eq : equipos) {
            if (eq.perdio) {
                continue;
            }
            for (UUID id : eq.ids) {
                Entity e = ent(s, id);
                if (e != null && (e.isPassenger() || e.getZ() - o.getZ() < 10.2)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** El último equipo (dormido): cada silla a su vitrina, con la correa puesta, y se cierra el frente con su placa. */
    private static void aLasVitrinas(MinecraftServer s) {
        ServerLevel l = s.overworld();
        Evento ev = Evento.get(s);
        BlockState vidrio = Complejo.estado(MOD + ":vidrio_armado");
        for (Equipo eq : equipos) {
            if (!eq.perdio) {
                continue;
            }
            for (Silla si : eq.sillas) {
                if (cajas >= SalaVitrina.VITRINAS) {
                    break;
                }
                int n = SalaVitrina.orden(cajas);
                BlockPos d = Complejo.pos(s, "vit.caja." + n);
                if (d == null || si.getPassengers().isEmpty()) {
                    continue;
                }
                Entity quien = si.getPassengers().get(0);
                llevarSentado(s, si, Vec3.atBottomCenterOf(d), 180F);
                si.grupo = "vitrina";
                si.cerrar(true);
                victimas.add(new Victima(quien.getUUID(), si, n));
                if (quien instanceof ServerPlayer sp) {
                    sp.setInvulnerable(true);
                }
                BlockPos f = Complejo.pos(s, "vit.frente." + n);
                if (f != null) {
                    for (int y = 0; y < 3; y++) {
                        l.setBlock(f.above(y), vidrio, 3);
                    }
                }
                BlockPos pl = Complejo.pos(s, "vit.placa." + n);
                Pieza p = ev.pieza(quien.getUUID());
                if (pl != null && p != null && l.getBlockEntity(pl) instanceof com.coleccionista.bloque.deco.TextoBE be) {
                    be.escribir(0xD9A93A, "PIEZA " + p.etiqueta(), p.nombre);
                }
                cajas++;
            }
            eq.etapa = Etapa.FUERA;
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS vitrina victimas={}", victimas.size());
    }

    /** El telón se abre del centro hacia los lados (una columna de cada lado cada 2 ticks); al terminar se prenden las vitrinas y habla. */
    private static void abrirTelon(MinecraftServer s, long now) {
        BlockPos o = Complejo.origen(s, "vitrina");
        if (o == null) {
            return;
        }
        ServerLevel l = s.overworld();
        int mitad = (SalaVitrina.TELON_X1 - SalaVitrina.TELON_X0) / 2 + 1;
        if (telonPaso == 0) {
            actualizarVoz(s);
            Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 0.8F, 0.5F);
            Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS telon");
        }
        if (telonPaso <= mitad && (now - telonT0) % 2 == 0) {
            int c0 = (SalaVitrina.TELON_X0 + SalaVitrina.TELON_X1) / 2;
            for (int x : new int[] {c0 - telonPaso, c0 + 1 + telonPaso}) {
                if (x < SalaVitrina.TELON_X0 || x > SalaVitrina.TELON_X1) {
                    continue;
                }
                for (int y = 1; y <= SalaVitrina.TELON_Y1; y++) {
                    l.setBlock(o.offset(x, y, SalaVitrina.TELON_Z), Blocks.AIR.defaultBlockState(), 2 | 16);
                }
                if (telonPaso % 3 == 0) {
                    l.playSound(null, o.offset(x, 5, SalaVitrina.TELON_Z), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.BLOCKS, 1F, 0.6F);
                }
            }
            telonPaso++;
        }
        if (telonPaso == mitad + 1 && !hablo) {
            telonPaso++;
            hablo = true;
            // los reflectores de cada vitrina con alguien
            for (Victima v : victimas) {
                luz(s, "vit.luz." + v.caja, true);
                BlockPos fo = Complejo.pos(s, "vit.foco." + v.caja);
                if (fo != null) {
                    l.setBlock(fo, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15), 3);
                }
            }
            if (!victimas.isEmpty()) {
                Pantallas.soloGrande(s, true);
                Pantallas.hablar(s, "vitrina", () -> ceraT0 = s.overworld().getGameTime());
            }
        }
    }

    /** La cera sube capa por capa en cada vitrina; se endurece en piedra; se rompen los vidrios y las estatuas caen frente a todos. */
    private static void cera(MinecraftServer s, long now) {
        ServerLevel l = s.overworld();
        long ct = now - ceraT0;
        final int dura = 160, total = 24;
        BlockState capa = Complejo.estado(MOD + ":cera");
        if (!ceraAviso) {
            ceraAviso = true;
            for (Victima v : victimas) {
                ServerPlayer sp = s.getPlayerList().getPlayer(v.id);
                if (sp != null) {
                    CompoundTag g = new CompoundTag();
                    g.putLong("desde", now);
                    g.putInt("dura", dura);
                    PlayerStore.Sync.toPlayer(sp, KEY_CERA, g);
                }
            }
            Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS cera");
        }
        if (ct <= dura) {
            int quiero = (int) Math.min(total, ct * total / dura + 1);
            for (Victima v : victimas) {
                BlockPos b = Complejo.pos(s, "vit.boquilla." + v.caja), c = Complejo.pos(s, "vit.caja." + v.caja);
                if (b == null || c == null) {
                    continue;
                }
                if (ct % 2 == 0) {
                    l.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, capa), b.getX() + 0.5, b.getY() + 0.8, b.getZ() + 0.5, 4,
                            0.18, 0.05, 0.18, 0);
                }
                if (ct % 9 == 0) {
                    l.playSound(null, c, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.6F, 0.5F);
                    l.playSound(null, c, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.5F, 0.5F);
                }
                if (ct % 25 == 0) {
                    l.playSound(null, c, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.6F);
                    ServerPlayer sp = s.getPlayerList().getPlayer(v.id);
                    if (sp != null) {
                        sp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1F, 1F);
                    }
                }
                for (int n = capas + 1; n <= quiero; n++) {
                    int alto = (n - 1) / 8, k = (n - 1) % 8 + 1;
                    l.setBlock(c.above(alto), capa.setValue(com.coleccionista.bloque.deco.DecoCapas.CAPAS, k), 2 | 16);
                }
            }
            capas = Math.max(capas, quiero);
        }
        // se endurece: la cera se vuelve piedra con su forma (las estatuas) y la pieza pasa a la colección
        if (!endurecida && ct >= dura + 20) {
            endurecida = true;
            Evento ev = Evento.get(s);
            int muertos = 0;
            for (Victima v : victimas) {
                BlockPos c = Complejo.pos(s, "vit.caja." + v.caja);
                if (c != null) {
                    for (int y = 0; y < 3; y++) {
                        l.setBlock(c.above(y), Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                    l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), c.getX() + 0.5, c.getY() + 1.2,
                            c.getZ() + 0.5, 60, 0.4, 0.8, 0.4, 0.05);
                    l.playSound(null, c, SoundEvents.DEEPSLATE_PLACE, SoundSource.BLOCKS, 1.2F, 0.5F);
                    l.playSound(null, c, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1F, 0.5F);
                }
                Pieza p = ev.pieza(v.id);
                if (p == null || !p.vivo()) {
                    continue;
                }
                Cuerpos.ESTATUAS.add(v.id);
                Entity e = Evento.entidad(s, p);
                if (ev.eliminarEnPrueba(s, p, PRUEBA)) {
                    muertos++;
                } else {
                    Cuerpos.ESTATUAS.remove(v.id);
                }
                if (e instanceof ServerPlayer sp) {
                    sp.setInvulnerable(false);
                }
            }
            // las estatuas que quedaron (para hacerlas caer)
            for (Victima v : victimas) {
                BlockPos c = Complejo.pos(s, "vit.caja." + v.caja);
                if (c == null) {
                    continue;
                }
                for (Cadaver cd : l.getEntitiesOfClass(Cadaver.class, new net.minecraft.world.phys.AABB(c).inflate(1.2, 1.5, 1.2))) {
                    if (cd.estatua() && !estatuas.contains(cd)) {
                        estatuas.add(cd);
                    }
                }
            }
            // cada uno se ve a sí mismo hecho piedra (la cámara frente a su vitrina) mientras se rompe el vidrio y la estatua cae
            for (Victima v : victimas) {
                ServerPlayer sp = s.getPlayerList().getPlayer(v.id);
                BlockPos c = Complejo.pos(s, "vit.caja." + v.caja);
                if (sp != null && c != null) {
                    CompoundTag t = new CompoundTag();
                    t.putString("tipo", "estatua");
                    t.put("a", punto(c.getX() + 0.5, c.getY() + 2.3, c.getZ() - 5.5));
                    t.put("b", punto(c.getX() + 0.5, c.getY() + 1.7, c.getZ() - 3.6));
                    t.put("mira", punto(c.getX() + 0.5, c.getY() + 0.9, c.getZ() + 0.2));
                    PlayerStore.Sync.toPlayer(sp, com.coleccionista.evento.Despertar.KEY_CINE, t);
                }
            }
            Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS estatuas={}", muertos);
        }
        // los vidrios del frente revientan
        if (!rotas && ct >= dura + 50) {
            rotas = true;
            for (Victima v : victimas) {
                BlockPos f = Complejo.pos(s, "vit.frente." + v.caja);
                if (f == null) {
                    continue;
                }
                for (int y = 0; y < 3; y++) {
                    l.setBlock(f.above(y), Blocks.AIR.defaultBlockState(), 3);
                    l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Complejo.estado(MOD + ":vidrio_armado")), f.getX() + 0.5,
                            f.getY() + y + 0.5, f.getZ() + 0.5, 25, 0.3, 0.3, 0.1, 0.1);
                }
                l.playSound(null, f, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.4F, 0.6F);
            }
        }
        // y las estatuas caen de frente, hacia el público
        if (!caen && ct >= dura + 62) {
            caen = true;
            for (Victima v : victimas) {
                v.silla.soltar();
                v.silla.discard();
            }
            for (Cadaver cd : estatuas) {
                cd.caer((int) now);
            }
            Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS caen={}", estatuas.size());
        }
        if (caen && !golpe && ct >= dura + 72) {
            golpe = true;
            for (Cadaver cd : estatuas) {
                l.playSound(null, cd.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.9F, 0.4F);
                l.playSound(null, cd.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.2F, 0.6F);
                l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), cd.getX(), cd.getY() + 0.2, cd.getZ() - 1,
                        50, 0.6, 0.1, 0.6, 0.1);
            }
            for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
                Fx.shake(sp, 0.5F, 8);
            }
        }
        if (!salidaAbierta && ct >= dura + 110) {
            Pantallas.hablar(s, "vitrina_fin", () -> abrirSalida(s));
            salidaAbierta = true;
        }
    }

    /** Se abre la salida del teatro (hacia la antesala). */
    private static void abrirSalida(MinecraftServer s) {
        salidaAbierta = true;
        fase = Fase.FIN;
        finalFin = s.overworld().getGameTime();
        puerta(s, "vit.salida", true);
        BlockPos p = Complejo.pos(s, "vit.salida.a");
        if (p != null) {
            s.overworld().playSound(null, p, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.2F, 0.5F);
        }
        Evento.get(s).prueba(s, Component.translatable(MOD + ".sen.prueba_salida").getString());
        Pantallas.modo(s, "apagada", "");
        BlockPos a = Complejo.pos(s, "vit.antesala");
        int n = 0;
        for (Pieza pz : Evento.get(s).vivos()) {
            if (pz.bot && a != null && Evento.entidad(s, pz) instanceof PiezaBot bot) {
                bot.stopRiding();
                bot.moveTo(a.getX() + 0.5 + n % 4, a.getY(), a.getZ() - 1.5 + n / 4, 90F, 0F);
                n++;
            }
        }
        com.coleccionista.voz.Voz.juntar();
        com.coleccionista.voz.Voz.mudos(Set.of());
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS salida_teatro");
    }

    /** Cuando ya pasaron todos los vivos a la antesala, la puerta se cierra detrás y habla la pantalla de la antesala. */
    private static void vigilarSalida(MinecraftServer s) {
        if (antesala) {
            return;
        }
        BlockPos o = Complejo.origen(s, "vitrina");
        if (o == null) {
            cerrarSalida(s);
            return;
        }
        for (Pieza p : Evento.get(s).vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null && e.getX() - o.getX() < SalaVitrina.SALIDA_X + 1.6) {
                return;
            }
        }
        cerrarSalida(s);
    }

    private static void cerrarSalida(MinecraftServer s) {
        if (antesala) {
            return;
        }
        antesala = true;
        puerta(s, "vit.salida", false);
        BlockPos p = Complejo.pos(s, "vit.salida.a");
        if (p != null) {
            s.overworld().playSound(null, p, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.2F, 0.5F);
        }
        Pantallas.soloGrande(s, true);
        Pantallas.hablar(s, "antesala", () -> finCapitulo(s));
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS antesala");
    }

    private static void luz(MinecraftServer s, String marca, boolean on) {
        BlockPos p = Complejo.pos(s, marca);
        if (p == null) {
            return;
        }
        BlockState st = s.overworld().getBlockState(p);
        if (st.hasProperty(com.coleccionista.bloque.deco.DecoBlock.ENCENDIDA) && st.getValue(com.coleccionista.bloque.deco.DecoBlock.ENCENDIDA) != on) {
            s.overworld().setBlock(p, st.setValue(com.coleccionista.bloque.deco.DecoBlock.ENCENDIDA, on), 3);
        }
    }

    /** Fin del capítulo 1: las sillas del teatro se quitan y todos se oyen otra vez. (Aquí empieza el capítulo 2.) */
    private static void finCapitulo(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Equipo eq : equipos) {
            for (Silla si : eq.sillas) {
                si.cerrar(false);
                si.soltar();
                si.discard();
            }
            eq.sillas.clear();
        }
        ev.prueba(s, Component.translatable(MOD + ".sen.fin_capitulo").getString());
        com.coleccionista.voz.Voz.juntar();
        com.coleccionista.voz.Voz.mudos(Set.of());
        Pantallas.modo(s, "coleccionista", "");
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_CAPITULO1 fin vivos={}", ev.vivos().size());
        // y sigue la casa: del pasillo pasan (sin que se note) al de la mansión
        if (Mascota.auto && Complejo.construido(s, "mansion")) {
            Mascota.llegar(s);
        }
    }

    // ------------------------------------------------------------------ el indicador de "ir acá"

    /** A dónde tiene que ir esa pieza ahora en el capítulo (la siguiente vuelta del camino, una silla, el telón o la salida). */
    public static Objetivo.Meta objetivo(MinecraftServer s, ServerPlayer sp) {
        Equipo eq = equipoDe(sp.getUUID());
        if (eq == null || fase == Fase.NADA) {
            return null;
        }
        if (fase == Fase.ELEGIR && eq.roles.getOrDefault(sp.getUUID(), Rol.NADA) == Rol.NADA) {
            Objetivo.Meta m = null;
            double dm = Double.MAX_VALUE;
            for (int i = 0; i < 3; i++) {
                BlockPos p = eq.objetos[i] == null ? null : Complejo.pos(s, eq.pre() + "objeto." + i);
                if (p != null && p.distToCenterSqr(sp.position()) < dm) {
                    dm = p.distToCenterSqr(sp.position());
                    m = Objetivo.Meta.en(p, 0.6, MOD + ".obj.objeto");
                }
            }
            return m;
        }
        if (fase == Fase.JUEGO && eq.etapa.ordinal() >= Etapa.POSTES.ordinal() && eq.etapa.ordinal() < Etapa.SALIDA.ordinal()) {
            BlockPos o = Complejo.origen(s, "circ_" + eq.k);
            if (o == null) {
                return null;
            }
            List<double[]> cam = Circuito.camino();
            int i = paso.getOrDefault(sp.getUUID(), 0);
            while (i < cam.size() - 1) {
                double[] q = cam.get(i);
                double dx = sp.getX() - (o.getX() + q[0]), dz = sp.getZ() - (o.getZ() + q[2]);
                if (dx * dx + dz * dz > 9) {
                    break;
                }
                i++;
            }
            paso.put(sp.getUUID(), i);
            double[] q = cam.get(i);
            return new Objetivo.Meta(o.getX() + q[0], o.getY() + q[1], o.getZ() + q[2], MOD + ".obj.sigue");
        }
        if (fase == Fase.JUEGO && eq.etapa == Etapa.SALIDA && !sp.isPassenger()) {
            for (Silla si : eq.sillas) {
                if (si.isAlive() && !si.isVehicle()) {
                    return new Objetivo.Meta(si.getX(), si.getY() + 1, si.getZ(), MOD + ".obj.silla");
                }
            }
        }
        if (fase == Fase.FINAL && levantan && !eq.perdio && !sp.isPassenger()) {
            BlockPos o = Complejo.origen(s, "vitrina");
            if (o != null && sp.getZ() - o.getZ() < 10.2) {
                return new Objetivo.Meta(o.getX() + 28, o.getY() + 5, o.getZ() + 38.5, MOD + ".obj.telon");
            }
        }
        if (fase == Fase.FIN && !antesala) {
            BlockPos a = Complejo.pos(s, "vit.salida.a");
            BlockPos o = Complejo.origen(s, "vitrina");
            if (a != null && o != null && sp.getX() - o.getX() < SalaVitrina.SALIDA_X + 1.6) {
                return new Objetivo.Meta(a.getX() + 0.5, a.getY() + 1.5, a.getZ() + 1, MOD + ".obj.salida");
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ avisos

    private static void avisar(MinecraftServer s, Equipo eq, String clave) {
        for (UUID id : eq.ids) {
            ServerPlayer sp = s.getPlayerList().getPlayer(id);
            if (sp != null) {
                Plat.msg(sp, Component.translatable(clave).withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    private static void sonar(MinecraftServer s, Equipo eq, net.minecraft.sounds.SoundEvent e, float vol, float tono) {
        for (UUID id : eq.ids) {
            ServerPlayer sp = s.getPlayerList().getPlayer(id);
            if (sp != null) {
                sp.playNotifySound(e, SoundSource.BLOCKS, vol, tono);
            }
        }
    }

    // ------------------------------------------------------------------ para el organizador y las pruebas

    /** El equipo de esa pieza, directo a la salida (para probar). */
    public static boolean aLaSalida(MinecraftServer s, Pieza p) {
        Equipo eq = equipoDe(p.id);
        if (eq == null || fase != Fase.JUEGO) {
            return false;
        }
        saltarA(s, eq, Etapa.SALIDA);
        return true;
    }

    /** Para revisar (y para las fotos): enciende en todos los circuitos el camino de las baldosas (5 s si están ahí) y el tablero. */
    public static void mostrar(MinecraftServer s) {
        long now = s.overworld().getGameTime();
        for (Equipo eq : equipos) {
            baldosas(s, eq, true, null);
            if (eq.etapa == Etapa.BALDOSAS) {
                eq.verHasta = now + 100;
            }
            tablero(s, eq, true);
        }
    }

    /** Cuántos equipos hay y en qué va cada uno (para el panel y el registro). */
    public static String resumen() {
        StringBuilder sb = new StringBuilder(fase.name());
        for (Equipo eq : equipos) {
            sb.append(' ').append(eq.k).append('=').append(eq.etapa.name().toLowerCase()).append(eq.perdio ? "!" : "");
        }
        return sb.toString();
    }

    public static void terminar(MinecraftServer s) {
        if (fase == Fase.HABLANDO) {
            Pantallas.callar(s);
        }
        limpiar(s);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS terminada");
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase != Fase.NADA) {
            terminar(s);
        }
        vitrinaComoNueva(s);
    }

    /** Deja el teatro como nuevo: el telón cerrado, las vitrinas vacías y abiertas, la salida cerrada. */
    public static void vitrinaComoNueva(MinecraftServer s) {
        teatroComoNuevo(s);
    }

    private static void teatroComoNuevo(MinecraftServer s) {
        if (!Complejo.construido(s, "vitrina")) {
            return;
        }
        ServerLevel l = s.overworld();
        for (int n = 0; n < SalaVitrina.VITRINAS; n++) {
            BlockPos c = Complejo.pos(s, "vit.caja." + n), f = Complejo.pos(s, "vit.frente." + n);
            for (int y = 0; y < 4 && c != null; y++) {
                l.setBlock(c.above(y), Blocks.AIR.defaultBlockState(), 3);
            }
            for (int y = 0; y < 3 && f != null; y++) {
                l.setBlock(f.above(y), Blocks.AIR.defaultBlockState(), 3);
            }
            luz(s, "vit.luz." + n, false);
            BlockPos fo = Complejo.pos(s, "vit.foco." + n);
            if (fo != null) {
                l.setBlock(fo, Blocks.AIR.defaultBlockState(), 3);
            }
            BlockPos pl = Complejo.pos(s, "vit.placa." + n);
            if (pl != null && l.getBlockEntity(pl) instanceof com.coleccionista.bloque.deco.TextoBE be) {
                be.escribir(0xD9A93A, "PIEZA", "—");
            }
        }
        BlockPos o = Complejo.origen(s, "vitrina");
        if (o != null) {
            BlockState tel = Complejo.estado(MOD + ":telon"), borde = Complejo.estado(MOD + ":telon_borde");
            for (int x = SalaVitrina.TELON_X0; x <= SalaVitrina.TELON_X1; x++) {
                for (int y = 1; y <= SalaVitrina.TELON_Y1; y++) {
                    BlockPos q = o.offset(x, y, SalaVitrina.TELON_Z);
                    BlockState want = y == 1 ? borde : tel;
                    if (l.getBlockState(q).getBlock() != want.getBlock()) {
                        l.setBlock(q, want, 2 | 16);
                    }
                }
            }
        }
        puerta(s, "vit.salida", false);
        for (int n = 1; n <= SalaVitrina.CUARTOS; n++) {
            Cabina.puerta(s, "sal_tea_" + n, false);
        }
        // las estatuas de la función anterior
        if (o != null) {
            for (Cadaver c : l.getEntitiesOfClass(Cadaver.class, new net.minecraft.world.phys.AABB(o, o.offset(SalaVitrina.ANTESALA_X1 + 2, 18, 61)))) {
                if (c.estatua()) {
                    c.discard();
                }
            }
        }
    }

    private static void limpiar(MinecraftServer s) {
        com.coleccionista.voz.Voz.juntar();
        com.coleccionista.voz.Voz.mudos(Set.of());
        // todas las sillas fuera (también las del teatro y la vitrina): nadie se queda sentado para la siguiente prueba
        for (Equipo eq : equipos) {
            for (Silla si : eq.sillas) {
                si.soltar();
                si.discard();
            }
        }
        for (Victima v : victimas) {
            ServerPlayer sp = s.getPlayerList().getPlayer(v.id);
            if (sp != null) {
                sp.setInvulnerable(false);
            }
        }
        fase = Fase.NADA;
        equipos.clear();
        botElige.clear();
        paso.clear();
        victimas.clear();
        estatuas.clear();
        cajas = 0;
        capas = 0;
        finT0 = telonT0 = ceraT0 = finalFin = -1;
        telonPaso = 0;
        palcos = levantan = gas = aVitrinas = despiertan = telon = hablo = ceraAviso = endurecida = rotas = caen = golpe = salidaAbierta = antesala = false;
        t = 0;
        cambio = true;
    }

    /** Quita todas las sillas que haya en el mundo (al reiniciar el evento). */
    public static void quitarSillas(MinecraftServer s) {
        for (Entity e : s.overworld().getAllEntities()) {
            if (e instanceof Silla si) {
                si.soltar();
                si.discard();
            }
        }
    }

    // ------------------------------------------------------------------ a los clientes (el mismo formato que Encadenados de tres)

    public static CompoundTag estado(MinecraftServer s) {
        CompoundTag c0 = new CompoundTag();
        c0.putString("fase", fase.name());
        c0.putDouble("largo", largo);
        ListTag l = new ListTag();
        for (Equipo eq : equipos) {
            CompoundTag x = new CompoundTag();
            x.putInt("k", eq.k);
            x.putString("etapa", eq.etapa.name());
            ListTag ms = new ListTag();
            for (UUID id : eq.ids) {
                CompoundTag m = new CompoundTag();
                m.putUUID("id", id);
                m.putString("rol", eq.roles.getOrDefault(id, Rol.NADA).name());
                ms.add(m);
            }
            x.put("miembros", ms);
            ListTag cs = new ListTag();
            for (Cuerda c : eq.cuerdas) {
                ListTag pv = new ListTag();
                for (Cuerda.Pivote p : c.pivotes) {
                    CompoundTag q = new CompoundTag();
                    q.putDouble("x", p.x());
                    q.putDouble("z", p.z());
                    q.putInt("g", p.giro());
                    pv.add(q);
                }
                CompoundTag cc = new CompoundTag();
                cc.put("p", pv);
                cs.add(cc);
            }
            x.put("cuerdas", cs);
            x.putBoolean("salio", eq.etapa.ordinal() >= Etapa.SENTADOS.ordinal());
            x.putBoolean("perdio", eq.perdio && fase.ordinal() >= Fase.FINAL.ordinal());
            l.add(x);
        }
        c0.put("trios", l);
        return c0;
    }

    private static void enviar(MinecraftServer s) {
        cambio = false;
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }
}
