package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import com.coleccionista.bot.PiezaBot;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Cuerpos;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.evento.Silla;
import com.coleccionista.mapa.Circuito;
import com.coleccionista.mapa.SalaVitrina;
import com.coleccionista.prueba.TresSentidos.Rol;
import com.coleccionista.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
 * los demás. En la mesa cada uno toma un objeto: la <b>venda</b> (no ve), la <b>cinta</b> (no habla ni escribe) o las <b>esposas</b> (no
 * toca nada); en parejas no hay cinta. Encadenados en fila cruzan los postes, el puente, las baldosas y las palancas hasta la salida, donde
 * se sientan en unas sillas que se los llevan. Los que salieron llegan a la galería; el <b>último equipo</b> no lo sabe, pero sus sillas los
 * llevan a la vitrina, donde la cera los vuelve piezas de la colección frente a todos.
 */
public final class Sentidos {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":sentidos";
    /** A cada víctima de la vitrina: desde cuándo sube la cera (para su pantalla). */
    public static final String KEY_CERA = MOD + ":cera";
    public static final String PRUEBA = "Los sentidos";
    public static int segundosElegir = 60, segundos = 900;
    public static double largo = 4.0;
    /** Para las pruebas automáticas: los equipos de puros bots llegan solos a la salida y se sientan. */
    public static boolean botsSalen = true;
    public static int botMin = 20, botMax = 60;

    public enum Fase {
        NADA, LISTA, HABLANDO, ELEGIR, JUEGO, FIN
    }

    public enum Etapa {
        MESA, POSTES, BALDOSAS, PALANCAS, SALIDA, SENTADOS, FUERA
    }

    static final class Equipo {
        final int k;
        final List<UUID> ids = new ArrayList<>();
        final Map<UUID, Rol> roles = new LinkedHashMap<>();
        final List<Cuerda> cuerdas = new ArrayList<>();
        final Rol[] objetos = new Rol[3];
        Etapa etapa = Etapa.MESA;
        long termino = -1, sentadosEn = -1, verHasta = -1, trampaHasta = -1, botListo = -1;
        /** En qué paso va el viaje de las sillas (0 cortina, 1 ruedan, 2 negro, 3 del otro lado). */
        int paso;
        boolean perdio, vistoCamino;
        int[] trampa;
        final int[] camino = new int[Circuito.BALD_FILAS];
        final boolean[] patron = new boolean[Circuito.PALANCAS];
        final List<Silla> sillas = new ArrayList<>();

        Equipo(int k) {
            this.k = k;
        }

        String pre() {
            return "circ." + k + ".";
        }
    }

    /** Una víctima de la vitrina: quién, en qué silla y en qué vitrina. */
    record Victima(UUID id, Silla silla, int caja) {
    }

    private static Fase fase = Fase.NADA;
    private static final List<Equipo> equipos = new ArrayList<>();
    private static final Map<UUID, Long> botElige = new HashMap<>();
    private static long t;
    private static boolean cambio;
    // la vitrina
    private static final List<Victima> victimas = new ArrayList<>();
    private static int cajas;
    private static long vitT0 = -1, ceraT0 = -1, negroT0 = -1;
    private static int capas;
    /** Los pasos de la vitrina que ya pasaron (cada uno una sola vez). */
    private static boolean vitCerrada, ceraAviso, negro, estatuas;

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
     * baldosas y el tablero de las palancas al azar, la cortina de la salida cerrada. Devuelve null si se pudo.
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
            // el camino de las baldosas: una por fila, de lado a lado sin saltar
            int c = r.nextInt(Circuito.BALD_ANCHO);
            for (int f = 0; f < Circuito.BALD_FILAS; f++) {
                eq.camino[f] = c;
                c = Math.max(0, Math.min(Circuito.BALD_ANCHO - 1, c + r.nextInt(3) - 1));
            }
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

    /** Deja el circuito del equipo como nuevo. */
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
        baldosas(s, eq, false, null);
        BlockState apagada = Complejo.estado(MOD + ":marco_luz_apagado");
        for (int j = 0; j < Circuito.PALANCAS; j++) {
            BlockPos p = Complejo.pos(s, eq.pre() + "palanca." + j);
            BlockState st = p == null ? null : l.getBlockState(p);
            if (st != null && st.hasProperty(BlockStateProperties.POWERED) && st.getValue(BlockStateProperties.POWERED)) {
                l.setBlock(p, st.setValue(BlockStateProperties.POWERED, false), 3);
            }
            BlockPos q = Complejo.pos(s, eq.pre() + "tablero." + j);
            if (q != null) {
                l.setBlock(q, apagada, 3);
            }
        }
        cortina(s, eq, true);
    }

    /** Las dos hojas de una puerta del circuito. */
    private static void puerta(MinecraftServer s, String marca, boolean abrir) {
        ServerLevel l = s.overworld();
        boolean cambio = false;
        for (String h : new String[] {".a", ".b"}) {
            BlockPos p = Complejo.pos(s, marca + h);
            if (p == null) {
                continue;
            }
            for (BlockPos q : new BlockPos[] {p, p.above()}) {
                BlockState st = l.getBlockState(q);
                if (st.getBlock() instanceof DoorBlock && st.getValue(DoorBlock.OPEN) != abrir) {
                    l.setBlock(q, st.setValue(DoorBlock.OPEN, abrir), 2 | 16);
                    cambio = true;
                }
            }
            if (cambio && h.equals(".a")) {
                l.playSound(null, p, abrir ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1F, 0.6F);
            }
        }
    }

    /** La cortina de barrotes de la salida (cerrada o abierta). */
    private static void cortina(MinecraftServer s, Equipo eq, boolean cerrada) {
        ServerLevel l = s.overworld();
        BlockState bar = Complejo.estado(MOD + ":barrotes");
        List<BlockPos> ps = new ArrayList<>();
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 3; y++) {
                BlockPos p = Complejo.pos(s, eq.pre() + "cortina." + x + "." + y);
                if (p != null) {
                    l.setBlock(p, cerrada ? bar : Blocks.AIR.defaultBlockState(), 2 | 16);
                    ps.add(p);
                }
            }
        }
        for (BlockPos p : ps) {
            BlockState st = l.getBlockState(p);
            if (cerrada) {
                l.setBlock(p, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(st, l, p), 2 | 16);
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
                BlockState st = roja != null && roja[0] == c && roja[1] == f ? rojo : camino && eq.camino[f] == c ? verde : off;
                BlockPos p = b.offset(c, 0, f);
                if (l.getBlockState(p) != st) {
                    l.setBlock(p, st, 3);
                }
            }
        }
    }

    // ------------------------------------------------------------------ llegada, discurso y la mesa

    /** Ya llegaron los ascensores a todos los circuitos: habla el Coleccionista (en las pantallas chicas de cada taller). */
    public static void alLlegar(MinecraftServer s, boolean conDiscurso) {
        if (fase != Fase.LISTA) {
            return;
        }
        Pantallas.soloGrande(s, false);
        Escena.presentacion(s, "I", MOD + ".sen.titulo", MOD + ".sen.lema");
        if (conDiscurso) {
            fase = Fase.HABLANDO;
            cambio = true;
            Pantallas.hablar(s, "sentidos", () -> {
                if (fase == Fase.HABLANDO) {
                    elegir(s);
                }
            });
        } else {
            elegir(s);
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS llegaron");
    }

    /** Para el organizador: corta el discurso y pasa a la mesa (o de la mesa al recorrido). */
    public static boolean saltar(MinecraftServer s) {
        if (fase == Fase.LISTA) {
            alLlegar(s, false);
            return true;
        }
        if (fase == Fase.HABLANDO) {
            Pantallas.callar(s);
            elegir(s);
            return true;
        }
        if (fase == Fase.ELEGIR) {
            arrancar(s);
            return true;
        }
        return false;
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

    /** Cada equipo solo se oye entre sí (los que ya salieron se oyen en la galería); la cinta corta el micrófono. */
    private static void actualizarVoz(MinecraftServer s) {
        Map<UUID, Integer> voz = new HashMap<>();
        Set<UUID> mudos = new HashSet<>();
        for (Equipo eq : equipos) {
            boolean fuera = eq.etapa.ordinal() >= Etapa.SENTADOS.ordinal() && !eq.perdio;
            for (UUID id : eq.ids) {
                voz.put(id, fuera ? 99 : eq.k);
                if (!fuera && fase == Fase.JUEGO && eq.roles.get(id) == Rol.CINTA) {
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
        if (fase == Fase.JUEGO || fase == Fase.FIN) {
            t++;
            for (Equipo eq : equipos) {
                circuito(s, eq, now);
            }
            if (fase == Fase.JUEGO) {
                revisarFin(s);
            }
            vitrina(s, now);
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
                // el puente: si alguien cae al foso, todo el equipo regresa al principio del puente
                for (Entity e : ents) {
                    if (!juega(e)) {
                        continue;
                    }
                    double lz = e.getZ() - o.getZ(), ly = e.getY() - o.getY();
                    if (ly < -0.6 && lz >= Circuito.Z_PUENTE0 - 0.5 && lz <= Circuito.Z_PUENTE1 + 1) {
                        cayo(s, eq, e, "ck.puente", MOD + ".sen.cayo");
                        return;
                    }
                }
                if (todosMas(ents, o, Circuito.Z_PUENTE1 + 1.2)) {
                    eq.etapa = Etapa.BALDOSAS;
                    eq.verHasta = now + 120;
                    eq.vistoCamino = true;
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
                    alaSalida(s, eq);
                }
            }
            case SALIDA -> salidaTick(s, eq, ents, now);
            case SENTADOS -> sentadosTick(s, eq, now);
            default -> {
            }
        }
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

    private static void cayo(MinecraftServer s, Equipo eq, Entity quien, String ck, String aviso) {
        volver(s, eq, ck);
        for (UUID id : eq.ids) {
            if (ent(s, id) instanceof ServerPlayer sp) {
                if (sp.getHealth() > 6) {
                    sp.hurt(sp.damageSources().magic(), 2F);
                }
                sp.playNotifySound(SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1F, 0.6F);
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
        // la palanca de "ver otra vez" (la jala quien tenga manos): enseña el camino 3 segundos y se regresa sola
        BlockPos ver = Complejo.pos(s, eq.pre() + "ver");
        BlockState vs = ver == null ? null : s.overworld().getBlockState(ver);
        if (vs != null && vs.hasProperty(BlockStateProperties.POWERED) && vs.getValue(BlockStateProperties.POWERED)) {
            s.overworld().setBlock(ver, vs.setValue(BlockStateProperties.POWERED, false), 3);
            eq.verHasta = now + 60;
            baldosas(s, eq, true, null);
            sonar(s, eq, SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.3F);
        }
        // ¿alguien pisó una baldosa mala?
        BlockPos b = Complejo.pos(s, eq.pre() + "baldosa.0.0");
        if (b != null && eq.trampaHasta < 0) {
            for (Entity e : ents) {
                if (!juega(e)) {
                    continue;
                }
                double ly = e.getY() - b.getY();
                int c = (int) Math.floor(e.getX() - b.getX()), f = (int) Math.floor(e.getZ() - b.getZ());
                if (ly < 0.9 || ly > 1.3 || !e.onGround() || f < 0 || f >= Circuito.BALD_FILAS || c < 0 || c >= Circuito.BALD_ANCHO) {
                    continue;
                }
                if (eq.camino[f] != c) {
                    eq.trampa = new int[] {c, f};
                    eq.trampaHasta = now + 30;
                    baldosas(s, eq, false, eq.trampa);
                    BlockPos p = b.offset(c, 1, f);
                    s.overworld().sendParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 0.2, p.getZ() + 0.5, 30, 0.3, 0.4, 0.3, 0.05);
                    s.overworld().playSound(null, p, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1F, 0.6F);
                    cayo(s, eq, e, "ck.baldosas", MOD + ".sen.trampa");
                    return;
                }
            }
        }
        if (todosMas(ents, o, Circuito.Z_BALD0 + Circuito.BALD_FILAS + 0.3)) {
            puerta(s, eq.pre() + "puerta3", true);
            eq.etapa = Etapa.PALANCAS;
            baldosas(s, eq, false, null);
            // el tablero: verde arriba, rojo abajo
            BlockState verde = Complejo.estado(MOD + ":marco_luz_verde"), rojo = Complejo.estado(MOD + ":marco_luz_roja");
            for (int j = 0; j < Circuito.PALANCAS; j++) {
                BlockPos q = Complejo.pos(s, eq.pre() + "tablero." + j);
                if (q != null) {
                    s.overworld().setBlock(q, eq.patron[j] ? verde : rojo, 3);
                }
            }
            avisar(s, eq, MOD + ".sen.palancas");
            cambio = true;
            Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS palancas equipo={}", eq.k);
        }
    }

    /** Se abrió la última puerta: aparecen las sillas de la salida (una por persona). */
    private static void alaSalida(MinecraftServer s, Equipo eq) {
        eq.etapa = Etapa.SALIDA;
        ServerLevel l = s.overworld();
        for (Silla si : eq.sillas) {
            si.discard();
        }
        eq.sillas.clear();
        for (int i = 0; i < eq.ids.size() && i < 3; i++) {
            BlockPos p = Complejo.pos(s, eq.pre() + "silla." + i);
            Silla si = ModRegistry.SILLA.get().create(l);
            if (p == null || si == null) {
                continue;
            }
            si.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0F, 0F);
            si.grupo = "circ." + eq.k;
            l.addFreshEntity(si);
            eq.sillas.add(si);
        }
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
        // las correas se cierran: ¿son los últimos?
        int faltan = 0;
        for (Equipo o : equipos) {
            if (o != eq && o.etapa.ordinal() < Etapa.SENTADOS.ordinal()) {
                faltan++;
            }
        }
        eq.perdio = faltan == 0 && equipos.size() > 1;
        eq.etapa = Etapa.SENTADOS;
        eq.termino = t;
        eq.sentadosEn = now;
        for (Silla si : eq.sillas) {
            si.cerrar(true);
        }
        sonar(s, eq, SoundEvents.IRON_TRAPDOOR_CLOSE, 1F, 0.5F);
        if (!eq.perdio) {
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".sen.chat.salio", eq.k)));
            Anuncio.sonido(s, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.5F, 1F);
        }
        actualizarVoz(s);
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS sentados equipo={} ultimo={}", eq.k, eq.perdio);
    }

    /** Las sillas se los llevan por el túnel; a negro, y del otro lado: la galería (o la vitrina, si eran los últimos). */
    private static void sentadosTick(MinecraftServer s, Equipo eq, long now) {
        // cada paso una sola vez, en cuanto llega su momento (aunque se haya brincado un tick o hubo pausa)
        long dt = now - eq.sentadosEn;
        if (eq.paso == 0 && dt >= 20) {
            eq.paso = 1;
            cortina(s, eq, false);
            sonar(s, eq, SoundEvents.IRON_DOOR_OPEN, 1F, 0.5F);
        }
        if (eq.paso == 1 && dt >= 30) {
            eq.paso = 2;
            for (Silla si : eq.sillas) {
                si.irA(si.position().add(0, 0, 9), 0.11);
            }
        }
        if (eq.paso == 2 && dt >= 92) {
            eq.paso = 3;
            List<ServerPlayer> ps = new ArrayList<>();
            for (UUID id : eq.ids) {
                if (ent(s, id) instanceof ServerPlayer sp) {
                    ps.add(sp);
                }
            }
            Escena.fundido(ps, "", 70);
        }
        if (eq.paso == 3 && dt >= 115) {
            eq.paso = 4;
            if (eq.perdio) {
                aLaVitrina(s, eq);
            } else {
                aLaGaleria(s, eq);
            }
            cortina(s, eq, true);
            eq.etapa = Etapa.FUERA;
            cambio = true;
        }
    }

    /** Pasa a alguien sentado de una silla a otro lugar, sentado en la misma silla. */
    private static void llevarSentado(MinecraftServer s, Silla si, BlockPos dest, float yaw) {
        List<Entity> pas = new ArrayList<>(si.getPassengers());
        si.soltar();
        si.irA(null, 0);
        si.moveTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, yaw, 0F);
        si.setYHeadRot(yaw);
        for (Entity e : pas) {
            e.fallDistance = 0;
            if (e instanceof ServerPlayer sp) {
                sp.teleportTo(s.overworld(), dest.getX() + 0.5, dest.getY() + 0.5, dest.getZ() + 0.5, yaw, 0F);
            } else {
                e.moveTo(dest.getX() + 0.5, dest.getY() + 0.5, dest.getZ() + 0.5, yaw, 0F);
            }
            e.startRiding(si, true);
        }
    }

    private static void aLaGaleria(MinecraftServer s, Equipo eq) {
        for (int i = 0; i < eq.sillas.size(); i++) {
            Silla si = eq.sillas.get(i);
            BlockPos d = Complejo.pos(s, "vit.llega." + eq.k + "." + i);
            if (d == null) {
                continue;
            }
            llevarSentado(s, si, d, 0F);
            si.grupo = "galeria";
            si.cerrar(false);
        }
        for (UUID id : eq.ids) {
            Entity e = ent(s, id);
            if (e != null && !e.isPassenger()) {
                BlockPos d = Complejo.pos(s, "vit.centro");
                if (d != null) {
                    e.teleportTo(d.getX() + 0.5, d.getY(), d.getZ() + 0.5);
                }
            }
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS galeria equipo={}", eq.k);
    }

    /** El último equipo: cada silla a su vitrina (con la correa puesta). */
    private static void aLaVitrina(MinecraftServer s, Equipo eq) {
        for (Silla si : eq.sillas) {
            if (cajas >= SalaVitrina.VITRINAS) {
                break;
            }
            BlockPos d = Complejo.pos(s, "vit.caja." + cajas);
            if (d == null) {
                continue;
            }
            Entity quien = si.getPassengers().isEmpty() ? null : si.getPassengers().get(0);
            llevarSentado(s, si, d, 180F);
            si.grupo = "vitrina";
            si.cerrar(true);
            if (quien != null) {
                victimas.add(new Victima(quien.getUUID(), si, cajas));
                if (quien instanceof ServerPlayer sp) {
                    sp.setInvulnerable(true);
                }
            }
            cajas++;
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS vitrina equipo={} victimas={}", eq.k, victimas.size());
        if (vitT0 < 0) {
            vitT0 = s.overworld().getGameTime();
            vitCerrada = false;
            ceraAviso = false;
            negro = false;
            estatuas = false;
            capas = 0;
        }
    }

    /** ¿Ya terminaron todos? Al acabar el tiempo, los que no salieron pierden (sus sillas van directo a la vitrina). */
    private static void revisarFin(MinecraftServer s) {
        for (Equipo eq : equipos) {
            if (eq.etapa != Etapa.FUERA) {
                return;
            }
        }
        fase = Fase.FIN;
        Evento.get(s).reloj(s, 0);
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS todos fuera victimas={}", victimas.size());
        if (victimas.isEmpty()) {
            finCapitulo(s);
        }
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

    /** Se acabó el tiempo (o lo pide el organizador): los equipos que no salieron pierden y van sentados directo a la vitrina. */
    public static boolean terminarYa(MinecraftServer s) {
        if (fase != Fase.JUEGO) {
            return false;
        }
        ServerLevel l = s.overworld();
        List<Equipo> faltan = new ArrayList<>();
        for (Equipo eq : equipos) {
            if (eq.etapa.ordinal() < Etapa.SENTADOS.ordinal()) {
                faltan.add(eq);
            }
        }
        // si todos los demás ya salieron y solo falta uno, ese es el último; si faltan varios, pierden todos
        for (Equipo eq : faltan) {
            eq.perdio = true;
            eq.etapa = Etapa.FUERA;
            eq.termino = t;
            for (Silla si : eq.sillas) {
                si.discard();
            }
            eq.sillas.clear();
            List<ServerPlayer> ps = new ArrayList<>();
            for (UUID id : eq.ids) {
                Entity e = ent(s, id);
                if (e == null) {
                    continue;
                }
                if (e instanceof ServerPlayer sp) {
                    ps.add(sp);
                }
                Silla si = ModRegistry.SILLA.get().create(l);
                if (si == null) {
                    continue;
                }
                si.moveTo(e.getX(), e.getY(), e.getZ(), 0F, 0F);
                l.addFreshEntity(si);
                e.startRiding(si, true);
                si.cerrar(true);
                eq.sillas.add(si);
            }
            Escena.fundido(ps, "", 70);
            aLaVitrina(s, eq);
        }
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".sen.chat.tiempo")));
        actualizarVoz(s);
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS tiempo pierden={}", faltan.size());
        revisarFin(s);
        return true;
    }

    // ------------------------------------------------------------------ la vitrina

    private static void vitrina(MinecraftServer s, long now) {
        if (vitT0 < 0 || fase != Fase.FIN || victimas.isEmpty()) {
            return;
        }
        ServerLevel l = s.overworld();
        long dt = now - vitT0;
        if (!vitCerrada && dt >= 40) {
            vitCerrada = true;
            // se cierra el frente de cada vitrina, se prenden sus reflectores y la placa dice quién es
            BlockState vidrio = Complejo.estado(MOD + ":vidrio_armado");
            Evento ev = Evento.get(s);
            for (Victima v : victimas) {
                BlockPos f = Complejo.pos(s, "vit.frente." + v.caja);
                if (f != null) {
                    for (int y = 0; y < 3; y++) {
                        l.setBlock(f.above(y), vidrio, 3);
                    }
                    l.playSound(null, f, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1F, 0.6F);
                }
                luz(s, "vit.luz." + v.caja, true);
                BlockPos pl = Complejo.pos(s, "vit.placa." + v.caja);
                Pieza p = ev.pieza(v.id);
                if (pl != null && p != null && l.getBlockEntity(pl) instanceof com.coleccionista.bloque.deco.TextoBE be) {
                    be.escribir(0xD9A93A, "PIEZA " + p.etiqueta(), p.nombre);
                }
            }
            Pantallas.soloGrande(s, true);
            Pantallas.hablar(s, "vitrina", () -> ceraT0 = s.overworld().getGameTime());
            Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.4F);
            Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS vitrina cierra");
        }
        if (ceraT0 > 0) {
            long ct = now - ceraT0;
            if (!ceraAviso) {
                ceraAviso = true;
                for (Victima v : victimas) {
                    ServerPlayer sp = s.getPlayerList().getPlayer(v.id);
                    if (sp != null) {
                        CompoundTag t = new CompoundTag();
                        t.putLong("desde", now);
                        t.putInt("dura", 140);
                        PlayerStore.Sync.toPlayer(sp, KEY_CERA, t);
                    }
                }
                Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS cera");
            }
            // la cera cae de la boquilla y va subiendo, capa por capa
            for (Victima v : victimas) {
                BlockPos b = Complejo.pos(s, "vit.boquilla." + v.caja), c = Complejo.pos(s, "vit.caja." + v.caja);
                if (b == null || c == null) {
                    continue;
                }
                if (ct < 140 && ct % 2 == 0) {
                    l.sendParticles(ParticleTypes.FALLING_HONEY, b.getX() + 0.5, b.getY() + 0.8, b.getZ() + 0.5, 3, 0.15, 0.05, 0.15, 0);
                    l.sendParticles(ParticleTypes.DRIPPING_HONEY, b.getX() + 0.5, b.getY() + 0.9, b.getZ() + 0.5, 2, 0.2, 0.05, 0.2, 0);
                }
                if (ct % 9 == 0 && ct < 140) {
                    l.playSound(null, c, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.8F, 0.6F);
                    l.playSound(null, c, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.5F, 0.6F);
                }
                if (ct % 25 == 0 && ct < 140) {
                    l.playSound(null, c, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.9F, 0.6F);
                    ServerPlayer sp = s.getPlayerList().getPlayer(v.id);
                    if (sp != null) {
                        sp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1F, 1F);
                    }
                }
            }
            if (capas < 4 && ct >= 30L * (capas + 1)) {
                capas++;
                BlockState miel = Blocks.HONEY_BLOCK.defaultBlockState();
                for (Victima v : victimas) {
                    BlockPos c = Complejo.pos(s, "vit.caja." + v.caja);
                    if (c != null) {
                        l.setBlock(c.above(capas - 1), miel, 2 | 16);
                    }
                }
            }
            if (!negro && ct >= 165) {
                negro = true;
                // a negro para todos los que miran; en medio, las víctimas se vuelven estatuas
                Escena.fundido(s, "", 60);
                Anuncio.sonido(s, SoundEvents.BEACON_DEACTIVATE, 1F, 0.5F);
                negroT0 = now;
            }
        }
        if (negroT0 > 0 && !estatuas && now - negroT0 >= 25) {
            estatuas = true;
            Evento ev = Evento.get(s);
            int muertos = 0;
            for (Victima v : victimas) {
                Pieza p = ev.pieza(v.id);
                if (p == null || !p.vivo()) {
                    continue;
                }
                v.silla.cerrar(false);
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
            Coleccionista.LOGGER.info("COLECCIONISTA_SENTIDOS estatuas={}", muertos);
        }
        if (negroT0 > 0 && now - negroT0 >= 70) {
            negroT0 = -1;
            ceraT0 = -1;
            vitT0 = -1;
            Pantallas.hablar(s, "vitrina_fin", () -> finCapitulo(s));
        }
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

    private static void finCapitulo(MinecraftServer s) {
        Evento ev = Evento.get(s);
        ev.prueba(s, Component.translatable(MOD + ".sen.fin_capitulo").getString());
        com.coleccionista.voz.Voz.juntar();
        com.coleccionista.voz.Voz.mudos(Set.of());
        Pantallas.modo(s, "coleccionista", "");
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_CAPITULO1 fin vivos={}", ev.vivos().size());
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
        BlockState verde = Complejo.estado(MOD + ":marco_luz_verde"), rojo = Complejo.estado(MOD + ":marco_luz_roja");
        for (Equipo eq : equipos) {
            baldosas(s, eq, true, null);
            if (eq.etapa == Etapa.BALDOSAS) {
                eq.verHasta = now + 100;
            }
            for (int j = 0; j < Circuito.PALANCAS; j++) {
                BlockPos q = Complejo.pos(s, eq.pre() + "tablero." + j);
                if (q != null) {
                    s.overworld().setBlock(q, eq.patron[j] ? verde : rojo, 3);
                }
            }
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

    /** Quita la cera, abre los frentes de las vitrinas y apaga sus luces. */
    public static void vitrinaComoNueva(MinecraftServer s) {
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
            BlockPos pl = Complejo.pos(s, "vit.placa." + n);
            if (pl != null && l.getBlockEntity(pl) instanceof com.coleccionista.bloque.deco.TextoBE be) {
                be.escribir(0xD9A93A, "PIEZA", "—");
            }
        }
    }

    private static void limpiar(MinecraftServer s) {
        com.coleccionista.voz.Voz.juntar();
        com.coleccionista.voz.Voz.mudos(Set.of());
        for (Equipo eq : equipos) {
            for (Silla si : eq.sillas) {
                if (!si.grupo.equals("galeria") && !si.grupo.equals("vitrina")) {
                    si.soltar();
                    si.discard();
                }
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
        victimas.clear();
        cajas = 0;
        capas = 0;
        vitT0 = -1;
        ceraT0 = -1;
        negroT0 = -1;
        vitCerrada = false;
        ceraAviso = false;
        negro = false;
        estatuas = false;
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
