package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.mapa.Galeria;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Prueba extra: Encadenados de tres. Tríos al azar, cada uno en su carril de la Galería de los postes. Primero, en la sala de la mesa, eligen
 * entre los tres quién se pone qué: la <b>venda</b> (casi no ve), la <b>cinta</b> en la boca (no puede hablar ni escribir) o las
 * <b>esposas</b> (no puede tocar nada: ni palancas ni puertas). Luego se abren las rejas y cruzan el recorrido encadenados en fila con una
 * cadena corta que se enreda en los postes. El último trío en salir (o los que no salgan a tiempo) pierde.
 */
public final class TresSentidos {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":tres";
    public static final String PRUEBA = "Encadenados de tres";
    public static int segundosElegir = 90, segundos = 480, tamano = 3;
    public static double largo = 4.0;
    /** true: pierde el trío completo; false: uno al azar del trío. */
    public static boolean pierdeEquipo = true;
    /** Para las pruebas automáticas: los bots no salen solos. */
    public static boolean botsSalen = true;

    public enum Fase {
        NADA, LISTA, HABLANDO, ELEGIR, JUEGO, FIN
    }

    public enum Rol {
        NADA, VENDA, CINTA, ESPOSAS
    }

    static final class Trio {
        final int k;
        final List<UUID> ids = new ArrayList<>();
        final Map<UUID, Rol> roles = new LinkedHashMap<>();
        final List<Cuerda> cuerdas = new ArrayList<>();
        final Set<UUID> salieron = new HashSet<>();
        final Rol[] objetos = new Rol[4];
        boolean reja2;
        long termino = -1;

        Trio(int k) {
            this.k = k;
        }
    }

    private static Fase fase = Fase.NADA;
    private static final List<Trio> trios = new ArrayList<>();
    private static final Map<UUID, Long> botElige = new HashMap<>(), botSale = new HashMap<>();
    private static long t, tpEn = -1, presentarEn = -1, arrancarEn = -1;
    private static int eliminados;
    private static boolean cambio;

    private TresSentidos() {
    }

    public static void init() {
        AxelisCore.onServerTick(TresSentidos::tick);
        AxelisCore.onLogin(p -> {
            if (fase != Fase.NADA) {
                PlayerStore.Sync.toPlayer(p, KEY, estado(Plat.server(p)));
            }
        });
        // las manos encadenadas no tocan nada; la boca tapada no escribe
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> manos(e));
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock e) -> manos(e));
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> manos(e));
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> manos(e));
        MinecraftForge.EVENT_BUS.addListener((AttackEntityEvent e) -> {
            if (rol(e.getEntity().getUUID()) == Rol.ESPOSAS && fase == Fase.JUEGO) {
                e.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ServerChatEvent e) -> {
            if (fase == Fase.JUEGO && rol(e.getPlayer().getUUID()) == Rol.CINTA) {
                e.setCanceled(true);
                Plat.msg(e.getPlayer(), Component.translatable(MOD + ".tres.no_hablar").withStyle(ChatFormatting.DARK_GRAY), true);
            }
        });
    }

    public static Fase fase() {
        return fase;
    }

    public static Rol rol(UUID id) {
        for (Trio tr : trios) {
            Rol r = tr.roles.get(id);
            if (r != null) {
                return r;
            }
        }
        return Rol.NADA;
    }

    private static Trio trioDe(UUID id) {
        for (Trio tr : trios) {
            if (tr.ids.contains(id)) {
                return tr;
            }
        }
        return null;
    }

    private static void manos(PlayerInteractEvent e) {
        if (e.getLevel().isClientSide() || fase == Fase.NADA) {
            return;
        }
        UUID id = e.getEntity().getUUID();
        // en la sala de la mesa: tomar un objeto
        if (fase == Fase.ELEGIR && e instanceof PlayerInteractEvent.RightClickBlock rc && e.getEntity() instanceof ServerPlayer sp) {
            if (tomar(sp, rc.getPos())) {
                e.setCanceled(true);
            }
            return;
        }
        if (fase == Fase.JUEGO && rol(id) == Rol.ESPOSAS && e.isCancelable()) {
            e.setCanceled(true);
            if (e instanceof PlayerInteractEvent.RightClickBlock && e.getEntity() instanceof ServerPlayer sp) {
                Plat.msg(sp, Component.translatable(MOD + ".tres.no_tocar").withStyle(ChatFormatting.DARK_GRAY), true);
                sp.playNotifySound(SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.6F, 0.8F);
            }
        }
    }

    // ------------------------------------------------------------------ preparar

    /** ¿La prueba está abierta (preparada, en juego o terminada sin limpiar)? */
    public static boolean abierta() {
        return fase != Fase.NADA;
    }

    public static Component preparar(MinecraftServer s) {
        if (!Complejo.construido(s, "galeria")) {
            return Component.translatable(MOD + ".tres.sin_mapa");
        }
        if (fase != Fase.NADA && fase != Fase.LISTA && fase != Fase.FIN) {
            return Component.translatable(MOD + ".tres.ya_empezo");
        }
        Evento ev = Evento.get(s);
        List<Pieza> v = ev.vivos();
        if (v.size() < 3) {
            return Component.translatable(MOD + ".tres.pocos");
        }
        Pruebas.cerrarOtras(s, "tres");
        limpiar(s);
        Random r = new Random(s.overworld().getRandom().nextLong());
        Collections.shuffle(v, r);
        int n = Math.max(1, Math.min(Galeria.CARRILES, v.size() / Math.max(3, tamano)));
        List<List<Pieza>> dados = Pruebas.dados(ev);
        if (dados != null) {
            n = Math.min(Galeria.CARRILES, dados.size());
            for (int i = 0; i < n; i++) {
                Trio tr = new Trio(i + 1);
                trios.add(tr);
                for (Pieza p : dados.get(i)) {
                    if (tr.ids.size() < 4) {
                        tr.ids.add(p.id);
                        tr.roles.put(p.id, Rol.NADA);
                        p.equipo = tr.k - 1;
                    }
                }
            }
        } else {
            for (int i = 0; i < n; i++) {
                trios.add(new Trio(i + 1));
            }
            for (int i = 0; i < v.size() && i < n * 4; i++) {
                Trio tr = trios.get(i % n);
                tr.ids.add(v.get(i).id);
                tr.roles.put(v.get(i).id, Rol.NADA);
                v.get(i).equipo = tr.k - 1;
            }
        }
        for (Trio tr : trios) {
            for (int i = 0; i + 1 < tr.ids.size(); i++) {
                tr.cuerdas.add(new Cuerda());
            }
            // los objetos de la mesa: venda, cinta y esposas (y otra venda si son cuatro)
            List<Rol> obj = new ArrayList<>(List.of(Rol.VENDA, Rol.CINTA, Rol.ESPOSAS));
            if (tr.ids.size() > 3) {
                obj.add(Rol.VENDA);
            }
            for (int i = 0; i < 4; i++) {
                tr.objetos[i] = i < obj.size() ? obj.get(i) : null;
            }
            mesa(s, tr);
            rejas(s, tr, true);
        }
        ev.cambio(s);
        ev.prueba(s, PRUEBA);
        ev.reloj(s, 0);
        Pantallas.modo(s, "estatica", "");
        fase = Fase.LISTA;
        long now = s.overworld().getGameTime();
        Escena.fundido(s, MOD + ".tres.fundido", 80);
        tpEn = now + 22;
        presentarEn = now + 90;
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_TRES preparada trios={}", n);
        return null;
    }

    /** Pone los objetos en la mesa del trío. */
    private static void mesa(MinecraftServer s, Trio tr) {
        ServerLevel l = s.overworld();
        for (int i = 0; i < 4; i++) {
            BlockPos p = Complejo.pos(s, "tres." + tr.k + ".objeto." + i);
            if (p == null) {
                continue;
            }
            Rol o = tr.objetos[i];
            BlockState st = o == null ? Blocks.AIR.defaultBlockState() : Complejo.estado(MOD + ":objeto_" + o.name().toLowerCase() + "[facing=south]");
            l.setBlock(p, st, 3);
        }
    }

    /** Cierra (o abre) las dos rejas del carril y regresa las palancas. */
    private static void rejas(MinecraftServer s, Trio tr, boolean cerrar) {
        reja(s, Complejo.pos(s, "tres." + tr.k + ".reja"), cerrar);
        reja(s, Complejo.pos(s, "tres." + tr.k + ".reja2"), cerrar);
        if (cerrar) {
            for (String lado : new String[] {"a", "b"}) {
                BlockPos p = Complejo.pos(s, "tres." + tr.k + ".palanca." + lado);
                BlockState st = p == null ? null : s.overworld().getBlockState(p);
                if (st != null && st.hasProperty(BlockStateProperties.POWERED) && st.getValue(BlockStateProperties.POWERED)) {
                    s.overworld().setBlock(p, st.setValue(BlockStateProperties.POWERED, false), 3);
                }
            }
        }
    }

    private static void reja(MinecraftServer s, BlockPos esquina, boolean cerrar) {
        if (esquina == null) {
            return;
        }
        ServerLevel l = s.overworld();
        Block barrotes = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(MOD, "barrotes"));
        for (int dx = 0; dx < 3; dx++) {
            for (int dy = 0; dy < 3; dy++) {
                BlockPos p = esquina.offset(dx, dy, 0);
                l.setBlock(p, cerrar && barrotes != null ? barrotes.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            }
        }
        if (cerrar && barrotes != null) {
            for (int dx = -1; dx <= 3; dx++) {
                for (int dy = 0; dy < 3; dy++) {
                    BlockPos p = esquina.offset(dx, dy, 0);
                    BlockState st = l.getBlockState(p);
                    if (st.is(barrotes)) {
                        l.setBlock(p, Block.updateFromNeighbourShapes(st, l, p), 3);
                    }
                }
            }
        }
        l.playSound(null, esquina, cerrar ? SoundEvents.IRON_DOOR_CLOSE : SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1F, 0.6F);
    }

    private static void aSusSalas(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Trio tr : trios) {
            for (int i = 0; i < tr.ids.size(); i++) {
                mover(s, ev.pieza(tr.ids.get(i)), "tres." + tr.k + ".lugar." + i);
            }
        }
    }

    private static void mover(MinecraftServer s, Pieza p, String marca) {
        BlockPos b = Complejo.pos(s, marca);
        Entity e = p == null ? null : Evento.entidad(s, p);
        if (b == null || e == null) {
            return;
        }
        float yaw = Complejo.yaw(marca);
        e.fallDistance = 0;
        if (e instanceof ServerPlayer sp) {
            if (sp.isSleeping()) {
                sp.stopSleeping();
            }
            sp.teleportTo(s.overworld(), b.getX() + 0.5, b.getY(), b.getZ() + 0.5, yaw, 0F);
            sp.setGameMode(GameType.ADVENTURE);
        } else {
            e.moveTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, yaw, 0F);
            e.setYHeadRot(yaw);
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    // ------------------------------------------------------------------ elegir

    public static boolean empezar(MinecraftServer s, boolean conDiscurso) {
        if (fase != Fase.LISTA) {
            return false;
        }
        if (conDiscurso) {
            fase = Fase.HABLANDO;
            cambio = true;
            Pantallas.hablar(s, "tres", () -> {
                if (fase == Fase.HABLANDO) {
                    elegir(s);
                }
            });
        } else {
            Pantallas.callar(s);
            elegir(s);
        }
        return true;
    }

    private static void elegir(MinecraftServer s) {
        fase = Fase.ELEGIR;
        t = 0;
        Evento ev = Evento.get(s);
        ev.reloj(s, segundosElegir);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".tres.pantalla.elegir").getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".tres.chat.elegir", segundosElegir)));
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.6F);
        // cada trío habla solo con los suyos
        Map<UUID, Integer> voz = new HashMap<>();
        for (Trio tr : trios) {
            for (UUID id : tr.ids) {
                voz.put(id, tr.k);
            }
        }
        com.coleccionista.voz.Voz.separar(voz);
        Random r = new Random(s.overworld().getRandom().nextLong());
        long now = s.overworld().getGameTime();
        for (Trio tr : trios) {
            for (UUID id : tr.ids) {
                Pieza p = ev.pieza(id);
                if (p != null && p.bot) {
                    botElige.put(id, now + 60 + r.nextInt(Math.max(1, segundosElegir * 20 / 3)));
                }
            }
        }
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_TRES elegir");
    }

    /** Alguien tomó un objeto de la mesa. */
    static boolean tomar(ServerPlayer sp, BlockPos pos) {
        MinecraftServer s = Plat.server(sp);
        Trio tr = trioDe(sp.getUUID());
        if (tr == null) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            if (pos.equals(Complejo.pos(s, "tres." + tr.k + ".objeto." + i)) && tr.objetos[i] != null) {
                if (tr.roles.get(sp.getUUID()) != Rol.NADA) {
                    Plat.msg(sp, Component.translatable(MOD + ".tres.ya_tienes").withStyle(ChatFormatting.GRAY), true);
                    return true;
                }
                asignar(s, tr, sp.getUUID(), i);
                return true;
            }
        }
        return false;
    }

    private static void asignar(MinecraftServer s, Trio tr, UUID id, int i) {
        Rol r = tr.objetos[i];
        tr.objetos[i] = null;
        tr.roles.put(id, r);
        BlockPos p = Complejo.pos(s, "tres." + tr.k + ".objeto." + i);
        if (p != null) {
            s.overworld().setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            s.overworld().playSound(null, p, r == Rol.ESPOSAS ? SoundEvents.CHAIN_PLACE : SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1F, 0.8F);
        }
        Pieza pz = Evento.get(s).pieza(id);
        for (UUID o : tr.ids) {
            ServerPlayer sp = s.getPlayerList().getPlayer(o);
            if (sp != null && pz != null) {
                Plat.msg(sp, Anuncio.nombre(pz).append(Component.translatable(MOD + ".tres.tomo." + r.name().toLowerCase()).withStyle(ChatFormatting.GRAY)),
                        false);
            }
        }
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_TRES rol {} {}", pz == null ? id : pz.etiqueta(), r);
    }

    /** A quien no eligió le toca lo que sobró en su mesa. */
    private static void repartirSobrantes(MinecraftServer s) {
        for (Trio tr : trios) {
            for (UUID id : tr.ids) {
                if (tr.roles.get(id) == Rol.NADA) {
                    for (int i = 0; i < 4; i++) {
                        if (tr.objetos[i] != null) {
                            asignar(s, tr, id, i);
                            break;
                        }
                    }
                }
            }
        }
    }

    private static boolean todosEligieron() {
        for (Trio tr : trios) {
            for (Rol r : tr.roles.values()) {
                if (r == Rol.NADA) {
                    return false;
                }
            }
        }
        return !trios.isEmpty();
    }

    // ------------------------------------------------------------------ el recorrido

    private static void arrancar(MinecraftServer s) {
        repartirSobrantes(s);
        fase = Fase.JUEGO;
        t = 0;
        Evento ev = Evento.get(s);
        ev.reloj(s, segundos);
        Set<UUID> mudos = new HashSet<>();
        Random r = new Random(s.overworld().getRandom().nextLong());
        long now = s.overworld().getGameTime();
        for (Trio tr : trios) {
            rejas(s, tr, false);
            reja(s, Complejo.pos(s, "tres." + tr.k + ".reja2"), true);
            for (UUID id : tr.ids) {
                if (tr.roles.get(id) == Rol.CINTA) {
                    mudos.add(id);
                }
                Pieza p = ev.pieza(id);
                if (p != null && p.bot && botsSalen) {
                    botSale.put(id, now + 300 + r.nextInt(Math.max(1, segundos * 20 / 2)));
                }
            }
        }
        com.coleccionista.voz.Voz.mudos(mudos);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".tres.pantalla.juego").getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".tres.chat.empieza", Math.max(1, (segundos + 30) / 60))));
        Anuncio.sonido(s, SoundEvents.CHAIN_BREAK, 1F, 0.5F);
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_TRES empieza");
    }

    private static Vec3 pos(MinecraftServer s, UUID id) {
        Pieza p = Evento.get(s).pieza(id);
        Entity e = p == null ? null : Evento.entidad(s, p);
        return e == null ? null : e.position();
    }

    private static void tick(MinecraftServer s) {
        if (fase == Fase.NADA) {
            return;
        }
        long now = s.overworld().getGameTime();
        if (tpEn > 0 && now >= tpEn) {
            tpEn = -1;
            aSusSalas(s);
        }
        if (presentarEn > 0 && now >= presentarEn) {
            presentarEn = -1;
            Escena.presentacion(s, "EXTRA", MOD + ".tres.titulo", MOD + ".tres.lema");
            Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.45F);
        }
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
                    Trio tr = trioDe(b.getKey());
                    if (tr != null && tr.roles.get(b.getKey()) == Rol.NADA) {
                        for (int i = 0; i < 4; i++) {
                            if (tr.objetos[i] != null) {
                                asignar(s, tr, b.getKey(), i);
                                break;
                            }
                        }
                    }
                }
            }
            if (arrancarEn < 0 && todosEligieron()) {
                arrancarEn = now + 60;
                Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".tres.chat.listos")));
            }
            if (arrancarEn > 0 && now >= arrancarEn) {
                arrancarEn = -1;
                arrancar(s);
            }
        }
        if (fase == Fase.JUEGO) {
            t++;
            juego(s, now);
        }
        if (cambio || now % 10 == 0 && fase == Fase.JUEGO || now % 40 == 0) {
            enviar(s);
        }
    }

    private static void juego(MinecraftServer s, long now) {
        Evento ev = Evento.get(s);
        BlockPos o = Complejo.origen(s, "galeria");
        boolean quedan = false;
        for (Trio tr : trios) {
            // bots que salen solos (solo para probar)
            for (UUID id : tr.ids) {
                Long b = botSale.get(id);
                if (b != null && now >= b) {
                    botSale.remove(id);
                    for (UUID m : tr.ids) {
                        mover(s, ev.pieza(m), "tres." + tr.k + ".salida");
                    }
                }
            }
            if (tr.termino >= 0) {
                continue;
            }
            // la cadena de cada par (en fila): se enreda en los postes
            if (o != null) {
                List<double[]> postes = new ArrayList<>();
                for (double[] q : Galeria.postes(tr.k)) {
                    postes.add(new double[] {q[0] + o.getX(), q[1] + o.getZ()});
                }
                for (int i = 0; i < tr.cuerdas.size(); i++) {
                    Vec3 a = pos(s, tr.ids.get(i)), b = pos(s, tr.ids.get(i + 1));
                    if (a == null || b == null) {
                        continue;
                    }
                    Cuerda c = tr.cuerdas.get(i);
                    int antes = c.pivotes.size();
                    c.actualizar(a.x, a.z, b.x, b.z, postes, 0.45);
                    if (c.pivotes.size() != antes) {
                        cambio = true;
                    }
                    // por si alguien se escapa (lag): si se pasó mucho del largo, regresa junto a su compañero
                    if (c.largo(a.x, a.z, b.x, b.z) > largo + 4) {
                        Pieza pb = ev.pieza(tr.ids.get(i + 1));
                        Entity eb = pb == null ? null : Evento.entidad(s, pb);
                        if (eb != null) {
                            eb.teleportTo(a.x, a.y, a.z);
                            c.pivotes.clear();
                        }
                    }
                }
            }
            // la reja de las palancas: se abre con las dos jaladas a la vez
            if (!tr.reja2 && palanca(s, tr, "a") && palanca(s, tr, "b")) {
                tr.reja2 = true;
                reja(s, Complejo.pos(s, "tres." + tr.k + ".reja2"), false);
                Coleccionista.LOGGER.info("COLECCIONISTA_TRES reja2 trio={}", tr.k);
                cambio = true;
            }
            // ¿ya salieron todos?
            BlockPos salida = Complejo.pos(s, "tres." + tr.k + ".salida");
            int fuera = 0;
            for (UUID id : tr.ids) {
                Vec3 p = pos(s, id);
                Pieza pz = ev.pieza(id);
                if (pz != null && !pz.vivo() || p != null && salida != null && p.z >= salida.getZ() - 1.5) {
                    fuera++;
                }
            }
            if (fuera >= tr.ids.size()) {
                tr.termino = t;
                Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".tres.chat.salio", tr.k)));
                Anuncio.sonido(s, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.6F, 1F);
                Coleccionista.LOGGER.info("COLECCIONISTA_TRES salio trio={}", tr.k);
                cambio = true;
            } else {
                quedan = true;
            }
        }
        int sinSalir = 0;
        for (Trio tr : trios) {
            if (tr.termino < 0) {
                sinSalir++;
            }
        }
        // cuando solo falta el último, se acaba
        if (!quedan || sinSalir <= 1 && trios.size() > 1) {
            fin(s, false);
        }
    }

    private static boolean palanca(MinecraftServer s, Trio tr, String lado) {
        BlockPos p = Complejo.pos(s, "tres." + tr.k + ".palanca." + lado);
        BlockState st = p == null ? null : s.overworld().getBlockState(p);
        return st != null && st.hasProperty(BlockStateProperties.POWERED) && st.getValue(BlockStateProperties.POWERED);
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        if (fase == Fase.ELEGIR) {
            arrancar(s);
            return true;
        }
        if (fase != Fase.JUEGO) {
            return false;
        }
        fin(s, true);
        return true;
    }

    /** Termina: pierde el último trío (o los que no salieron). */
    public static void fin(MinecraftServer s, boolean porTiempo) {
        if (fase != Fase.JUEGO) {
            return;
        }
        Evento ev = Evento.get(s);
        fase = Fase.FIN;
        ev.reloj(s, 0);
        com.coleccionista.voz.Voz.juntar();
        com.coleccionista.voz.Voz.mudos(Set.of());
        List<Trio> orden = new ArrayList<>(trios);
        orden.sort((x, y) -> {
            if (x.termino < 0 && y.termino < 0) {
                return Integer.compare(x.salieron.size(), y.salieron.size());
            }
            if (x.termino < 0) {
                return -1;
            }
            if (y.termino < 0) {
                return 1;
            }
            return Long.compare(y.termino, x.termino);
        });
        // pierden todos los que no salieron; si todos salieron, el último
        List<Trio> pierden = new ArrayList<>();
        for (Trio tr : orden) {
            if (tr.termino < 0) {
                pierden.add(tr);
            }
        }
        if (pierden.isEmpty() && !orden.isEmpty() && trios.size() > 1) {
            pierden.add(orden.get(0));
        }
        Random r = new Random(s.overworld().getRandom().nextLong());
        for (Trio tr : pierden) {
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".tres.chat.pierde", tr.k)));
            List<UUID> caen = new ArrayList<>(tr.ids);
            if (!pierdeEquipo) {
                Collections.shuffle(caen, r);
                caen = caen.subList(0, 1);
            }
            for (UUID id : caen) {
                Pieza p = ev.pieza(id);
                if (p != null && p.vivo() && ev.eliminarEnPrueba(s, p, PRUEBA)) {
                    eliminados++;
                }
            }
        }
        Pantallas.modo(s, "coleccionista", "");
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_TRES fin eliminados={} tiempo={}", eliminados, porTiempo);
        enviar(s);
    }

    /** Para la prueba automática: lleva a todo el trío de esa pieza a la salida. */
    public static boolean sacar(MinecraftServer s, Pieza p) {
        Trio tr = trioDe(p.id);
        if (tr == null || fase != Fase.JUEGO) {
            return false;
        }
        for (UUID id : tr.ids) {
            mover(s, Evento.get(s).pieza(id), "tres." + tr.k + ".salida");
        }
        return true;
    }

    /** Para la prueba automática: que esa pieza tome el objeto i de su mesa. */
    public static boolean tomarComo(MinecraftServer s, Pieza p, int i) {
        Trio tr = trioDe(p.id);
        if (tr == null || fase != Fase.ELEGIR || i < 0 || i > 3 || tr.objetos[i] == null || tr.roles.get(p.id) != Rol.NADA) {
            return false;
        }
        asignar(s, tr, p.id, i);
        return true;
    }

    public static void terminar(MinecraftServer s) {
        if (fase == Fase.HABLANDO) {
            Pantallas.callar(s);
        }
        if (Complejo.construido(s, "galeria")) {
            for (Trio tr : trios) {
                rejas(s, tr, true);
                tr.objetos[0] = Rol.VENDA;
                tr.objetos[1] = Rol.CINTA;
                tr.objetos[2] = Rol.ESPOSAS;
                tr.objetos[3] = null;
                mesa(s, tr);
            }
        }
        limpiar(s);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_TRES terminada");
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase != Fase.NADA) {
            terminar(s);
        }
    }

    private static void limpiar(MinecraftServer s) {
        com.coleccionista.voz.Voz.juntar();
        com.coleccionista.voz.Voz.mudos(Set.of());
        fase = Fase.NADA;
        trios.clear();
        botElige.clear();
        botSale.clear();
        eliminados = 0;
        tpEn = -1;
        presentarEn = -1;
        arrancarEn = -1;
        t = 0;
    }

    // ------------------------------------------------------------------ a los clientes

    public static CompoundTag estado(MinecraftServer s) {
        CompoundTag c0 = new CompoundTag();
        c0.putString("fase", fase.name());
        c0.putDouble("largo", largo);
        ListTag l = new ListTag();
        for (Trio tr : trios) {
            CompoundTag x = new CompoundTag();
            x.putInt("k", tr.k);
            ListTag ms = new ListTag();
            for (UUID id : tr.ids) {
                CompoundTag m = new CompoundTag();
                m.putUUID("id", id);
                m.putString("rol", tr.roles.getOrDefault(id, Rol.NADA).name());
                ms.add(m);
            }
            x.put("miembros", ms);
            ListTag cs = new ListTag();
            for (Cuerda c : tr.cuerdas) {
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
            x.putBoolean("salio", tr.termino >= 0);
            l.add(x);
        }
        c0.put("trios", l);
        return c0;
    }

    private static void enviar(MinecraftServer s) {
        cambio = false;
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }

    /** Cuántos roles de cada tipo hay (para el registro y el panel). */
    static Map<Rol, Integer> cuenta() {
        Map<Rol, Integer> m = new EnumMap<>(Rol.class);
        for (Trio tr : trios) {
            for (Rol r : tr.roles.values()) {
                m.merge(r, 1, Integer::sum);
            }
        }
        return m;
    }
}
