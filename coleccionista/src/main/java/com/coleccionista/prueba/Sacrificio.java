package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Plat;
import com.axeliscore.ui.CoreScreens;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.mapa.Capilla;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Prueba 5: El sacrificio. Equipos al azar, cada uno encerrado en un cuarto de la Capilla. La reja solo se abre si alguien se queda: quien
 * se pare 5 segundos en el altar se ofrece (lo encierra una jaula), la reja se abre y los demás tienen 10 segundos para salir. Si nadie
 * se ofrece antes de que acabe el tiempo, votan en secreto; quien tenga más votos se queda (empate: al azar). Quien se queda termina en la
 * vitrina del pasillo: la pieza nueva de la colección. Los equipos de una sola pieza se salvan solos.
 */
public final class Sacrificio {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":sacrificio";
    public static final String ACCION = MOD + ":voto";
    public static final String PANTALLA = MOD + ":voto";
    public static final String PRUEBA = "El sacrificio";
    public static int segundosOfrecer = 90, segundosVotar = 25, tamano = 4;
    /** ¿Los bots se ofrecen solos a veces? (para las pruebas automáticas se apaga) */
    public static boolean botsSeOfrecen = true;
    static final int ALTAR = 100, SALIR = 200;

    public enum Fase {
        NADA, LISTA, HABLANDO, OFRECER, VOTAR, FIN
    }

    enum Estado {
        ESPERA, SALIENDO, RESUELTO
    }

    static final class Cuarto {
        final int k;
        final List<UUID> ids = new ArrayList<>();
        Estado estado = Estado.ESPERA;
        UUID queda, enAltar;
        boolean ofrecido;
        int altarTicks;
        long saleHasta;
        final Map<UUID, UUID> votos = new HashMap<>();

        Cuarto(int k) {
            this.k = k;
        }
    }

    private static Fase fase = Fase.NADA;
    private static final List<Cuarto> cuartos = new ArrayList<>();
    private static final Map<UUID, Long> botEn = new HashMap<>();
    private static long t, tpEn = -1, presentarEn = -1;
    private static int eliminados;
    private static boolean cambio;

    private Sacrificio() {
    }

    public static void init() {
        AxelisCore.onServerTick(Sacrificio::tick);
        AxelisCore.onLogin(p -> {
            if (fase != Fase.NADA) {
                PlayerStore.Sync.toPlayer(p, KEY, estado(Plat.server(p)));
            }
        });
        Actions.on(ACCION, Sacrificio::votar);
    }

    public static Fase fase() {
        return fase;
    }

    // ------------------------------------------------------------------ preparar

    /** ¿La prueba está abierta (preparada, en juego o terminada sin limpiar)? */
    public static boolean abierta() {
        return fase != Fase.NADA;
    }

    public static Component preparar(MinecraftServer s) {
        if (!Complejo.construido(s, "capilla")) {
            return Component.translatable(MOD + ".sac.sin_mapa");
        }
        if (fase != Fase.NADA && fase != Fase.LISTA && fase != Fase.FIN) {
            return Component.translatable(MOD + ".sac.ya_empezo");
        }
        Evento ev = Evento.get(s);
        List<Pieza> v = ev.vivos();
        if (v.size() < 2) {
            return Component.translatable(MOD + ".enc.pocos");
        }
        Pruebas.cerrarOtras(s, "sacrificio");
        limpiar(s);
        // equipos al azar del tamaño pedido (como mucho 6 cuartos: si son muchos, los equipos crecen)
        int n = Math.max(1, Math.min(Capilla.CUARTOS, v.size() / Math.max(2, tamano)));
        List<List<Pieza>> dados = Pruebas.dados(ev);
        if (dados != null) {
            n = Math.min(Capilla.CUARTOS, dados.size());
            for (int i = 0; i < n; i++) {
                Cuarto c = new Cuarto(i + 1);
                cuartos.add(c);
                for (Pieza p : dados.get(i)) {
                    if (c.ids.size() < Capilla.LUGARES) {
                        c.ids.add(p.id);
                        p.equipo = c.k - 1;
                    }
                }
            }
        } else {
            Collections.shuffle(v, new Random(s.overworld().getRandom().nextLong()));
            for (int i = 0; i < n; i++) {
                cuartos.add(new Cuarto(i + 1));
            }
            for (int i = 0; i < v.size(); i++) {
                Cuarto c = cuartos.get(i % n);
                if (c.ids.size() < Capilla.LUGARES) {
                    c.ids.add(v.get(i).id);
                    v.get(i).equipo = c.k - 1;
                }
            }
        }
        ev.cambio(s);
        ev.prueba(s, PRUEBA);
        ev.reloj(s, 0);
        for (int k = 1; k <= Capilla.CUARTOS; k++) {
            reja(s, k, false);
            jaula(s, k, false);
        }
        Pantallas.modo(s, "estatica", "");
        fase = Fase.LISTA;
        long now = s.overworld().getGameTime();
        Escena.fundido(s, MOD + ".sac.fundido", 80);
        tpEn = now + 22;
        presentarEn = now + 90;
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SACRIFICIO preparada cuartos={}", n);
        return null;
    }

    private static void aLosCuartos(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Cuarto c : cuartos) {
            for (int i = 0; i < c.ids.size(); i++) {
                Pieza p = ev.pieza(c.ids.get(i));
                String m = "cap.cuarto." + c.k + ".lugar." + i;
                mover(s, p, m, 0, 0);
            }
            // equipos de una sola pieza: se salvan solos
            if (vivos(ev, c).size() <= 1) {
                c.estado = Estado.RESUELTO;
                reja(s, c.k, true);
            }
        }
    }

    private static void mover(MinecraftServer s, Pieza p, String marca, double dx, double dz) {
        BlockPos b = Complejo.pos(s, marca);
        Entity e = p == null ? null : Evento.entidad(s, p);
        if (b == null || e == null) {
            return;
        }
        float yaw = Complejo.yaw(marca);
        double x = b.getX() + 0.5 + dx, y = b.getY(), z = b.getZ() + 0.5 + dz;
        e.fallDistance = 0;
        if (e instanceof ServerPlayer sp) {
            if (sp.isSleeping()) {
                sp.stopSleeping();
            }
            sp.teleportTo(s.overworld(), x, y, z, yaw, 0F);
            sp.setGameMode(GameType.ADVENTURE);
        } else {
            e.moveTo(x, y, z, yaw, 0F);
            e.setYHeadRot(yaw);
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static List<UUID> vivos(Evento ev, Cuarto c) {
        List<UUID> l = new ArrayList<>();
        for (UUID id : c.ids) {
            Pieza p = ev.pieza(id);
            if (p != null && p.vivo()) {
                l.add(id);
            }
        }
        return l;
    }

    // ------------------------------------------------------------------ rejas y jaulas

    private static void reja(MinecraftServer s, int k, boolean abrir) {
        BlockPos b = Complejo.pos(s, "cap.cuarto." + k + ".reja");
        if (b == null) {
            return;
        }
        ServerLevel l = s.overworld();
        List<BlockPos> ps = new ArrayList<>();
        for (int dx = 0; dx < Capilla.REJA_ANCHO; dx++) {
            for (int y = 0; y < Capilla.REJA_ALTO; y++) {
                ps.add(b.offset(dx, y, 0));
            }
        }
        barras(l, ps, !abrir);
        l.playSound(null, b.offset(1, 0, 0), abrir ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1F, 0.6F);
    }

    /** La jaula del altar: barrotes alrededor de la placa (dos de alto). */
    private static void jaula(MinecraftServer s, int k, boolean cerrar) {
        BlockPos a = Complejo.pos(s, "cap.cuarto." + k + ".altar");
        if (a == null) {
            return;
        }
        List<BlockPos> ps = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                for (int y = 0; y < 2; y++) {
                    ps.add(a.offset(dx, y, dz));
                }
            }
        }
        barras(s.overworld(), ps, cerrar);
        if (cerrar) {
            s.overworld().playSound(null, a, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 0.6F);
        }
    }

    private static void barras(ServerLevel l, List<BlockPos> ps, boolean poner) {
        for (BlockPos p : ps) {
            l.setBlock(p, poner ? com.coleccionista.bloque.deco.Deco.barrotes() : Blocks.AIR.defaultBlockState(), 3);
        }
        if (poner) {
            for (BlockPos p : ps) {
                l.setBlock(p, Block.updateFromNeighbourShapes(com.coleccionista.bloque.deco.Deco.barrotes(), l, p), 3);
            }
        }
    }

    // ------------------------------------------------------------------ fases

    public static boolean empezar(MinecraftServer s, boolean conDiscurso) {
        if (fase != Fase.LISTA) {
            return false;
        }
        if (conDiscurso) {
            fase = Fase.HABLANDO;
            cambio = true;
            Pantallas.hablar(s, "sacrificio", () -> {
                if (fase == Fase.HABLANDO) {
                    ofrecer(s);
                }
            });
        } else {
            Pantallas.callar(s);
            ofrecer(s);
        }
        return true;
    }

    private static void ofrecer(MinecraftServer s) {
        fase = Fase.OFRECER;
        java.util.Map<UUID, Integer> voz = new java.util.HashMap<>();
        for (var g : cuartos) {
            for (UUID id : g.ids) {
                voz.put(id, g.k);
            }
        }
        com.coleccionista.voz.Voz.separar(voz);
        Evento ev = Evento.get(s);
        ev.reloj(s, segundosOfrecer);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".sac.pantalla.ofrecer").getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".sac.chat.ofrecer", segundosOfrecer)));
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.6F);
        Random r = new Random(s.overworld().getRandom().nextLong());
        long now = s.overworld().getGameTime();
        for (Cuarto c : cuartos) {
            for (UUID id : vivos(ev, c)) {
                Pieza p = ev.pieza(id);
                if (p.bot && botsSeOfrecen && r.nextFloat() < 0.35F) {
                    botEn.put(id, now + 60 + r.nextInt(Math.max(1, segundosOfrecer * 20 - 200)));
                }
            }
        }
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SACRIFICIO ofrecer");
    }

    /** Se acabó el tiempo para ofrecerse: los cuartos sin voluntario votan. */
    public static boolean votarYa(MinecraftServer s) {
        if (fase != Fase.OFRECER) {
            return false;
        }
        Evento ev = Evento.get(s);
        fase = Fase.VOTAR;
        ev.reloj(s, segundosVotar);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".sac.pantalla.votar").getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".sac.chat.votar")));
        Random r = new Random(s.overworld().getRandom().nextLong());
        long now = s.overworld().getGameTime();
        botEn.clear();
        for (Cuarto c : cuartos) {
            if (c.estado != Estado.ESPERA) {
                continue;
            }
            for (UUID id : vivos(ev, c)) {
                Pieza p = ev.pieza(id);
                if (p.bot) {
                    botEn.put(id, now + 30 + r.nextInt(Math.max(1, segundosVotar * 20 - 80)));
                } else {
                    ServerPlayer sp = s.getPlayerList().getPlayer(id);
                    if (sp != null) {
                        CoreScreens.open(sp, PANTALLA);
                    }
                }
            }
        }
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SACRIFICIO votar");
        return true;
    }

    private static Cuarto cuartoDe(UUID id) {
        for (Cuarto c : cuartos) {
            if (c.ids.contains(id)) {
                return c;
            }
        }
        return null;
    }

    private static void votar(ServerPlayer sp, CompoundTag d) {
        if (fase != Fase.VOTAR || !d.hasUUID("id")) {
            return;
        }
        Cuarto c = cuartoDe(sp.getUUID());
        UUID por = d.getUUID("id");
        if (c == null || c.estado != Estado.ESPERA || !c.ids.contains(por) || c.votos.containsKey(sp.getUUID())) {
            return;
        }
        c.votos.put(sp.getUUID(), por);
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1F, 0.7F);
        Coleccionista.LOGGER.info("COLECCIONISTA_VOTO cuarto={}", c.k);
        cambio = true;
    }

    /** Cuenta los votos de un cuarto: el más votado se queda (empate o sin votos: al azar entre los vivos). */
    private static UUID conteo(Evento ev, Cuarto c, Random r) {
        Map<UUID, Integer> cuenta = new HashMap<>();
        List<UUID> vivos = vivos(ev, c);
        for (UUID v : c.votos.values()) {
            if (vivos.contains(v)) {
                cuenta.merge(v, 1, Integer::sum);
            }
        }
        int max = cuenta.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<UUID> top = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : cuenta.entrySet()) {
            if (e.getValue() == max) {
                top.add(e.getKey());
            }
        }
        if (top.isEmpty()) {
            top = vivos;
        }
        return top.isEmpty() ? null : top.get(r.nextInt(top.size()));
    }

    private static void finVotacion(MinecraftServer s) {
        Evento ev = Evento.get(s);
        Random r = new Random(s.overworld().getRandom().nextLong());
        for (Cuarto c : cuartos) {
            if (c.estado == Estado.ESPERA) {
                UUID q = conteo(ev, c, r);
                if (q != null) {
                    quedarse(s, c, q, false);
                }
            }
        }
        ev.reloj(s, 0);
        cambio = true;
    }

    /** Alguien se queda en el cuarto: jaula en el altar, se abre la reja y los demás tienen 10 segundos para salir. */
    private static void quedarse(MinecraftServer s, Cuarto c, UUID id, boolean ofrecido) {
        Evento ev = Evento.get(s);
        Pieza p = ev.pieza(id);
        c.queda = id;
        c.ofrecido = ofrecido;
        c.estado = Estado.SALIENDO;
        c.saleHasta = t + SALIR;
        mover(s, p, "cap.cuarto." + c.k + ".altar", 0, 0);
        // nadie más se queda atrapado en los barrotes de la jaula: los que estén junto al altar se hacen para atrás
        BlockPos a = Complejo.pos(s, "cap.cuarto." + c.k + ".altar");
        if (a != null) {
            for (UUID otro : vivos(ev, c)) {
                Entity e = otro.equals(id) ? null : Evento.entidad(s, ev.pieza(otro));
                if (e != null && Math.abs(e.getX() - (a.getX() + 0.5)) < 2 && Math.abs(e.getZ() - (a.getZ() + 0.5)) < 2) {
                    mover(s, ev.pieza(otro), "cap.cuarto." + c.k + ".lugar.1", 0, 0);
                }
            }
        }
        jaula(s, c.k, true);
        reja(s, c.k, true);
        if (p != null) {
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + (ofrecido ? ".sac.chat.ofrecido" : ".sac.chat.votado"), c.k,
                    Anuncio.nombre(p))));
        }
        Anuncio.sonido(s, SoundEvents.BELL_RESONATE, 0.8F, 0.6F);
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SACRIFICIO cuarto={} queda={} ofrecido={}", c.k, p == null ? "?" : p.etiqueta(), ofrecido);
    }

    /** Se cierra la reja: quien se quedó va a su vitrina (y a la colección); los lentos salen empujados. */
    private static void sacrificar(MinecraftServer s, Cuarto c) {
        Evento ev = Evento.get(s);
        for (UUID id : vivos(ev, c)) {
            if (id.equals(c.queda)) {
                continue;
            }
            Pieza p = ev.pieza(id);
            Entity e = Evento.entidad(s, p);
            BlockPos r = Complejo.pos(s, "cap.cuarto." + c.k + ".reja");
            if (e != null && r != null && e.getZ() < r.getZ() + 1) {
                mover(s, p, "cap.cuarto." + c.k + ".pasillo", (s.overworld().getRandom().nextDouble() - 0.5) * 2, 0);
            }
        }
        reja(s, c.k, false);
        jaula(s, c.k, false);
        Pieza q = ev.pieza(c.queda);
        if (q != null && q.vivo()) {
            mover(s, q, "cap.cuarto." + c.k + ".vitrina", 0, 0);
            if (ev.eliminarEnPrueba(s, q, PRUEBA)) {
                eliminados++;
            }
        }
        c.estado = Estado.RESUELTO;
        cambio = true;
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        if (fase == Fase.OFRECER) {
            votarYa(s);
            return true;
        }
        if (fase == Fase.VOTAR) {
            finVotacion(s);
            return true;
        }
        return false;
    }

    private static void tick(MinecraftServer s) {
        if (fase == Fase.NADA) {
            return;
        }
        long now = s.overworld().getGameTime();
        if (tpEn > 0 && now >= tpEn) {
            tpEn = -1;
            aLosCuartos(s);
        }
        if (presentarEn > 0 && now >= presentarEn) {
            presentarEn = -1;
            Escena.presentacion(s, "VI", MOD + ".sac.titulo", MOD + ".sac.lema");
            Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.45F);
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        t++;
        Random r = new Random(now);
        if (fase == Fase.OFRECER) {
            for (Cuarto c : cuartos) {
                if (c.estado != Estado.ESPERA) {
                    continue;
                }
                // ¿alguien parado en el altar?
                BlockPos a = Complejo.pos(s, "cap.cuarto." + c.k + ".altar");
                UUID en = null;
                if (a != null) {
                    AABB caja = new AABB(a.getX(), a.getY(), a.getZ(), a.getX() + 1, a.getY() + 0.4, a.getZ() + 1);
                    for (UUID id : vivos(ev, c)) {
                        Entity e = Evento.entidad(s, ev.pieza(id));
                        if (e != null && e.getBoundingBox().intersects(caja)) {
                            en = id;
                            break;
                        }
                    }
                }
                // bots que se ofrecen: caminan (se teletransportan) al altar
                for (UUID id : vivos(ev, c)) {
                    Long cu = botEn.get(id);
                    if (cu != null && now >= cu) {
                        botEn.remove(id);
                        mover(s, ev.pieza(id), "cap.cuarto." + c.k + ".altar", 0, 0);
                    }
                }
                if (en != null && en.equals(c.enAltar)) {
                    c.altarTicks++;
                    if (c.altarTicks % 20 == 0) {
                        s.overworld().playSound(null, a, SoundEvents.NOTE_BLOCK_BASEDRUM.get(), SoundSource.BLOCKS, 1F, 0.5F + c.altarTicks / 200F);
                    }
                    if (c.altarTicks >= ALTAR) {
                        quedarse(s, c, en, true);
                    }
                } else if (en != c.enAltar) {
                    c.enAltar = en;
                    c.altarTicks = 0;
                    cambio = true;
                }
            }
        } else if (fase == Fase.VOTAR) {
            for (Map.Entry<UUID, Long> b : new ArrayList<>(botEn.entrySet())) {
                if (now >= b.getValue()) {
                    botEn.remove(b.getKey());
                    Cuarto c = cuartoDe(b.getKey());
                    if (c != null && c.estado == Estado.ESPERA) {
                        List<UUID> vs = vivos(ev, c);
                        vs.remove(b.getKey());
                        if (!vs.isEmpty()) {
                            c.votos.put(b.getKey(), vs.get(r.nextInt(vs.size())));
                            cambio = true;
                        }
                    }
                }
            }
            // si ya votaron todos los de un cuarto, no se espera al reloj
            boolean todos = true;
            for (Cuarto c : cuartos) {
                if (c.estado == Estado.ESPERA && c.votos.size() < vivos(ev, c).size()) {
                    todos = false;
                }
            }
            if (todos) {
                finVotacion(s);
            }
        }
        // cuartos saliendo: se acaba el tiempo para salir
        boolean pendientes = false;
        for (Cuarto c : cuartos) {
            if (c.estado == Estado.SALIENDO && t >= c.saleHasta) {
                sacrificar(s, c);
            }
            if (c.estado != Estado.RESUELTO) {
                pendientes = true;
            }
        }
        if (!pendientes && (fase == Fase.OFRECER || fase == Fase.VOTAR)) {
            fin(s);
        }
        if (cambio || now % 20 == 0) {
            enviar(s);
        }
    }

    private static void fin(MinecraftServer s) {
        fase = Fase.FIN;
        com.coleccionista.voz.Voz.juntar();
        Evento.get(s).reloj(s, 0);
        Pantallas.modo(s, "coleccionista", "");
        for (Cuarto c : cuartos) {
            reja(s, c.k, true);
        }
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".sac.chat.fin", eliminados)));
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_SACRIFICIO fin eliminados={}", eliminados);
    }

    public static void terminar(MinecraftServer s) {
        if (fase == Fase.HABLANDO) {
            Pantallas.callar(s);
        }
        if (Complejo.construido(s, "capilla")) {
            for (int k = 1; k <= Capilla.CUARTOS; k++) {
                jaula(s, k, false);
                reja(s, k, true);
            }
        }
        limpiar(s);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_SACRIFICIO terminada");
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase != Fase.NADA) {
            terminar(s);
        }
    }

    private static void limpiar(MinecraftServer s) {
        com.coleccionista.voz.Voz.juntar();
        fase = Fase.NADA;
        cuartos.clear();
        botEn.clear();
        eliminados = 0;
        tpEn = -1;
        presentarEn = -1;
        t = 0;
    }

    // ------------------------------------------------------------------ a los clientes

    public static CompoundTag estado(MinecraftServer s) {
        CompoundTag c0 = new CompoundTag();
        c0.putString("fase", fase.name());
        Evento ev = Evento.get(s);
        ListTag l = new ListTag();
        for (Cuarto c : cuartos) {
            CompoundTag x = new CompoundTag();
            x.putInt("k", c.k);
            x.putString("estado", c.estado.name());
            ListTag ms = new ListTag();
            for (UUID id : c.ids) {
                Pieza p = ev.pieza(id);
                CompoundTag m = new CompoundTag();
                m.putUUID("id", id);
                m.putInt("numero", p == null ? 0 : p.numero);
                m.putString("nombre", p == null ? "?" : p.nombre);
                m.putBoolean("vivo", p != null && p.vivo());
                m.putBoolean("voto", c.votos.containsKey(id));
                ms.add(m);
            }
            x.put("miembros", ms);
            if (c.queda != null) {
                x.putUUID("queda", c.queda);
            }
            if (c.enAltar != null && c.estado == Estado.ESPERA) {
                x.putUUID("altar", c.enAltar);
                x.putInt("altar_t", c.altarTicks);
            }
            if (c.estado == Estado.SALIENDO) {
                x.putInt("sale", (int) Math.max(0, (c.saleHasta - t) / 20));
            }
            l.add(x);
        }
        c0.put("cuartos", l);
        return c0;
    }

    private static void enviar(MinecraftServer s) {
        cambio = false;
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }
}
