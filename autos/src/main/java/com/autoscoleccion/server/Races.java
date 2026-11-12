package com.autoscoleccion.server;

import com.autoscoleccion.AutosMod;
import com.autoscoleccion.block.TrackBlock;
import com.autoscoleccion.entity.RcCarEntity;
import com.autoscoleccion.game.CarStats;
import com.autoscoleccion.game.Cars;
import com.autoscoleccion.game.TrackLayout;
import com.autoscoleccion.net.AutosNet;
import com.autoscoleccion.net.RaceActionPacket;
import com.autoscoleccion.net.RaceStatePacket;
import com.autoscoleccion.net.TrackInfoPacket;
import com.autoscoleccion.registry.ModEntities;
import com.autoscoleccion.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Las carreras en las mini pistas. Cada bloque de pista tiene su "sala": quién se anotó (y con qué auto), cuántas vueltas y cuántos
 * bots. Al empezar, cada jugador sale con su auto en la parrilla y los bots los maneja el servidor. El servidor sigue el avance de cada
 * auto por la pista, cuenta vueltas y posiciones, y manda a cada corredor cómo va.
 */
@Mod.EventBusSubscriber(modid = AutosMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Races {
    public static final int MAX_BOTS = 7;
    public static final int MAX_LAPS = 9;
    private static final int COUNTDOWN = 70;
    private static final int RESULTS = 200;
    /** Qué tan lejos del bloque puede estar un jugador para correr (su auto no puede alejarse más de 64 de él). */
    private static final double JOIN_RANGE = 40;
    private static final String[] BOT_NAMES = {"Bot Rayo", "Bot Turbo", "Bot Chispa", "Bot Neón", "Bot Pistón", "Bot Nitro", "Bot Drift"};

    private static final Map<String, Lobby> LOBBIES = new HashMap<>();

    private Races() {
    }

    static final class Lobby {
        final ResourceKey<Level> dim;
        final BlockPos pos;
        int laps = 3;
        int bots = 3;
        final LinkedHashMap<UUID, Integer> joined = new LinkedHashMap<>();
        Race race;

        Lobby(ResourceKey<Level> dim, BlockPos pos) {
            this.dim = dim;
            this.pos = pos;
        }
    }

    static final class Racer {
        UUID player;
        String name;
        int car;
        int entity = -1;
        float lastS;
        float progress;
        int lap = 1;
        int finishTick = -1;
        boolean out;
        float offset;
        int stuck;
        int lastSent = -1;
    }

    static final class Race {
        final Lobby lobby;
        final Direction facing;
        final TrackLayout layout;
        final List<Racer> racers = new ArrayList<>();
        int phase;
        int timer = COUNTDOWN;
        int ticks;
        int firstFinish = -1;

        Race(Lobby lobby, Direction facing, TrackLayout layout) {
            this.lobby = lobby;
            this.facing = facing;
            this.layout = layout;
        }
    }

    private static String key(ResourceKey<Level> dim, BlockPos pos) {
        return dim.location() + "@" + pos.asLong();
    }

    private static Lobby lobby(Level level, BlockPos pos) {
        return LOBBIES.computeIfAbsent(key(level.dimension(), pos), k -> new Lobby(level.dimension(), pos.immutable()));
    }

    // -------------------------------------------------------------------------------------------------------------- la pantalla

    public static void open(ServerPlayer sp, BlockPos pos) {
        send(sp, sp.serverLevel(), pos, true);
    }

    private static void send(ServerPlayer sp, ServerLevel level, BlockPos pos, boolean open) {
        BlockState st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof TrackBlock)) {
            return;
        }
        Lobby l = lobby(level, pos);
        List<String> names = new ArrayList<>();
        l.joined.forEach((id, car) -> {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p != null) {
                names.add(p.getGameProfile().getName() + " · " + Cars.get(car).name);
            }
        });
        AutosNet.toPlayer(sp, new TrackInfoPacket(pos, st.getValue(TrackBlock.BUILT), st.getValue(TrackBlock.THEME), l.laps, l.bots, l.race != null, names, open));
    }

    /** Avisa a los que están cerca (por si tienen la pantalla abierta). */
    private static void broadcast(ServerLevel level, BlockPos pos) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 48 * 48) {
                send(p, level, pos, false);
            }
        }
    }

    public static void action(ServerPlayer sp, BlockPos pos, int action, int arg) {
        ServerLevel level = sp.serverLevel();
        if (!(level.getBlockState(pos).getBlock() instanceof TrackBlock) || sp.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) > JOIN_RANGE * JOIN_RANGE) {
            return;
        }
        BlockState st = level.getBlockState(pos);
        Lobby l = lobby(level, pos);
        switch (action) {
            case RaceActionPacket.BUILD -> {
                if (!st.getValue(TrackBlock.BUILT) && sp.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 12 * 12) {
                    int n = TrackBuilder.build(level, pos, st.getValue(TrackBlock.FACING), st.getValue(TrackBlock.THEME));
                    level.setBlock(pos, st.setValue(TrackBlock.BUILT, true), 3);
                    level.playSound(null, pos, ModSounds.get("result"), SoundSource.BLOCKS, 0.8F, 1.0F);
                    sp.sendSystemMessage(Component.literal("§a¡Mini pista lista! §7(" + n + " bloques)"));
                }
            }
            case RaceActionPacket.JOIN -> {
                if (l.race != null) {
                    sp.displayClientMessage(Component.literal("§eYa hay una carrera en curso."), true);
                } else if (arg >= 0 && arg < Cars.COUNT && AutosData.has(sp, arg)) {
                    l.joined.put(sp.getUUID(), arg);
                }
            }
            case RaceActionPacket.THEME -> {
                if (!st.getValue(TrackBlock.BUILT)) {
                    level.setBlock(pos, st.setValue(TrackBlock.THEME, Mth.clamp(arg, 0, TrackLayout.ALL.length - 1)), 3);
                }
            }
            case RaceActionPacket.LEAVE -> l.joined.remove(sp.getUUID());
            case RaceActionPacket.LAPS -> l.laps = Mth.clamp(arg, 1, MAX_LAPS);
            case RaceActionPacket.BOTS -> l.bots = Mth.clamp(arg, 0, MAX_BOTS);
            case RaceActionPacket.START -> start(sp, level, l, st);
            case RaceActionPacket.CANCEL -> {
                if (l.race != null) {
                    finish(level, l.race, true);
                }
            }
            default -> {
            }
        }
        broadcast(level, pos);
    }

    public static void removed(Level level, BlockPos pos) {
        Lobby l = LOBBIES.remove(key(level.dimension(), pos));
        if (l != null && l.race != null && level instanceof ServerLevel sl) {
            finish(sl, l.race, true);
        }
    }

    // -------------------------------------------------------------------------------------------------------------- salida

    /** Punto de la parrilla número i: x, z (mundo), rumbo. */
    private static double[] gridSlot(Lobby l, Direction facing, TrackLayout t, int i) {
        float s = -2.2F - (i / 2) * 2.4F;
        float lat = (i % 2 == 0 ? -1.2F : 1.2F);
        float[] a = t.at(s);
        // normal hacia la derecha de la dirección de carrera
        float nx = a[3], nz = -a[2];
        double[] w = TrackBuilder.toWorld(l.pos, facing, a[0] + nx * lat, a[1] + nz * lat);
        return new double[] {w[0], w[1], TrackBuilder.yaw(facing, a[2], a[3]), s};
    }

    private static void start(ServerPlayer sp, ServerLevel level, Lobby l, BlockState st) {
        if (l.race != null) {
            return;
        }
        if (!st.getValue(TrackBlock.BUILT)) {
            sp.displayClientMessage(Component.literal("§ePrimero construye la pista."), true);
            return;
        }
        if (l.joined.isEmpty()) {
            sp.displayClientMessage(Component.literal("§eAnótate con un auto antes de empezar."), true);
            return;
        }
        Direction facing = st.getValue(TrackBlock.FACING);
        Race race = new Race(l, facing, TrackLayout.ALL[st.getValue(TrackBlock.THEME)]);
        int slot = 0;
        double y = l.pos.getY() + 0.05;
        for (Map.Entry<UUID, Integer> e : l.joined.entrySet()) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null || p.level() != level || p.distanceToSqr(l.pos.getX(), l.pos.getY(), l.pos.getZ()) > 24 * 24) {
                continue;
            }
            double[] g = gridSlot(l, facing, race.layout, slot);
            RcCarEntity car = Driving.startAt(p, e.getValue(), g[0], y, g[1], (float) g[2]);
            if (car == null) {
                continue;
            }
            Racer r = new Racer();
            r.player = p.getUUID();
            r.name = p.getGameProfile().getName();
            r.car = e.getValue();
            r.entity = car.getId();
            r.progress = (float) g[3];
            r.lastS = race.layout.length + (float) g[3];
            race.racers.add(r);
            slot++;
        }
        if (race.racers.isEmpty()) {
            sp.displayClientMessage(Component.literal("§eAcérquense a la pista para correr."), true);
            return;
        }
        RandomSource rnd = level.getRandom();
        for (int b = 0; b < l.bots; b++) {
            double[] g = gridSlot(l, facing, race.layout, slot);
            RcCarEntity car = ModEntities.RC_CAR.get().create(level);
            if (car == null) {
                break;
            }
            // los bots usan autos parecidos a los de los jugadores (ni muy lentos ni imposibles)
            int ref = race.racers.get(b % race.racers.size()).car;
            int pick = rnd.nextInt(Cars.COUNT);
            for (int tries = 0; tries < 20 && Math.abs(Cars.get(pick).rarity.stars - Cars.get(ref).rarity.stars) > 1; tries++) {
                pick = rnd.nextInt(Cars.COUNT);
            }
            car.moveTo(g[0], y, g[1], (float) g[2], 0);
            car.setCar(pick);
            car.bot = true;
            car.topMul = 0.82F + rnd.nextFloat() * 0.14F;
            level.addFreshEntity(car);
            Racer r = new Racer();
            r.name = BOT_NAMES[b % BOT_NAMES.length];
            r.car = pick;
            r.entity = car.getId();
            r.progress = (float) g[3];
            r.lastS = race.layout.length + (float) g[3];
            r.offset = (rnd.nextFloat() - 0.5F) * 1.6F;
            race.racers.add(r);
            slot++;
        }
        l.race = race;
        level.playSound(null, l.pos, ModSounds.get("machine_start"), SoundSource.BLOCKS, 0.8F, 1.0F);
        level.setBlock(l.pos, st.setValue(TrackBlock.LIT, true), 3);
    }

    // -------------------------------------------------------------------------------------------------------------- en carrera

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.START || LOBBIES.isEmpty()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (Lobby l : new ArrayList<>(LOBBIES.values())) {
            if (l.race == null) {
                continue;
            }
            ServerLevel level = server.getLevel(l.dim);
            if (level == null || !(level.getBlockState(l.pos).getBlock() instanceof TrackBlock)) {
                if (level != null) {
                    finish(level, l.race, true);
                }
                l.race = null;
                continue;
            }
            tick(level, l.race);
        }
    }

    private static void tick(ServerLevel level, Race race) {
        TrackLayout t = race.layout;
        race.ticks++;
        if (race.phase == 0) {
            race.timer--;
            if (race.timer <= 0) {
                race.phase = 1;
                race.ticks = 0;
            }
        } else if (race.phase == 2) {
            race.timer--;
            if (race.timer <= 0) {
                finish(level, race, false);
                return;
            }
        }
        float total = race.lobby.laps * t.length;
        for (Racer r : race.racers) {
            if (r.out || r.finishTick >= 0) {
                continue;
            }
            Entity ent = level.getEntity(r.entity);
            boolean alive = ent instanceof RcCarEntity && ent.isAlive();
            if (r.player != null && (!alive || Driving.active(r.player) != r.entity)) {
                r.out = true;
                continue;
            }
            if (!alive) {
                r.out = true;
                continue;
            }
            RcCarEntity car = (RcCarEntity) ent;
            float[] loc = TrackBuilder.toLocal(race.lobby.pos, race.facing, car.getX(), car.getZ());
            float[] nr = t.nearestNear(loc[0], loc[1], r.lastS, 12);
            float ds = nr[0] - r.lastS;
            if (ds > t.length / 2) {
                ds -= t.length;
            } else if (ds < -t.length / 2) {
                ds += t.length;
            }
            if (Math.abs(ds) < 8) {
                if (race.phase == 1) {
                    r.progress += ds;
                }
                r.lastS = nr[0];
            }
            r.lap = Mth.clamp((int) Math.floor(r.progress / t.length) + 1, 1, race.lobby.laps);
            if (race.phase == 1 && r.progress >= total) {
                r.finishTick = race.ticks;
                if (race.firstFinish < 0) {
                    race.firstFinish = race.ticks;
                }
                if (r.player != null) {
                    ServerPlayer p = level.getServer().getPlayerList().getPlayer(r.player);
                    if (p != null) {
                        level.playSound(null, p.blockPosition(), ModSounds.get("result"), SoundSource.PLAYERS, 0.8F, 1.0F);
                    }
                }
            }
            if (car.bot) {
                botDrive(race, r, car, nr[0], loc);
            }
        }
        // ¿se acabó? todos los jugadores llegaron (o se salieron), o pasó mucho tiempo
        if (race.phase == 1) {
            boolean humansLeft = false;
            boolean anyHuman = false;
            for (Racer r : race.racers) {
                if (r.player != null) {
                    anyHuman = true;
                    humansLeft |= !r.out && r.finishTick < 0;
                }
            }
            boolean late = race.firstFinish >= 0 && race.ticks - race.firstFinish > 20 * 30;
            if (!anyHuman || !humansLeft || late || race.ticks > 20 * 60 * 8) {
                race.phase = 2;
                race.timer = RESULTS;
            }
        }
        if (race.ticks % 4 == 0 || race.phase != 1) {
            report(level, race);
        }
    }

    /** El bot mira un punto más adelante en la pista y gira hacia él; frena antes de las curvas cerradas. */
    private static void botDrive(Race race, Racer r, RcCarEntity car, float s, float[] loc) {
        TrackLayout t = race.layout;
        if (race.phase != 1 || r.finishTick >= 0) {
            car.throttle = 0;
            car.steerIn = 0;
            car.handbrake = race.phase == 0;
            return;
        }
        CarStats st = CarStats.of(car.def());
        float sp = Math.abs(car.speed);
        float look = 2.6F + sp * 7.0F;
        float[] a = t.at(s + look);
        float lat = r.offset * 0.7F;
        float tx = a[0] + a[3] * lat, tz = a[1] - a[2] * lat;
        float wantYaw = TrackBuilder.yaw(race.facing, tx - loc[0], tz - loc[1]);
        float diff = Mth.wrapDegrees(wantYaw - car.getYRot());
        car.steerIn = Mth.clamp(diff / 22.0F, -1, 1);
        // curva que viene: ángulo entre la dirección de ahora y la de más adelante
        float[] now = t.at(s), ahead = t.at(s + 6 + sp * 10);
        float bend = (float) Math.toDegrees(Math.acos(Mth.clamp(now[2] * ahead[2] + now[3] * ahead[3], -1, 1)));
        float frac = sp / (st.top * car.topMul);
        if (bend > 75 && frac > 0.55F) {
            car.throttle = -1;
        } else if (bend > 45 && frac > 0.72F) {
            car.throttle = 0;
        } else {
            car.throttle = 1;
        }
        car.handbrake = false;
        // atorado contra el muro: vuelve al centro de la pista
        if (sp < 0.03F) {
            r.stuck++;
        } else {
            r.stuck = 0;
        }
        if (r.stuck > 40) {
            r.stuck = 0;
            float[] c = t.at(s);
            double[] w = TrackBuilder.toWorld(race.lobby.pos, race.facing, c[0], c[1]);
            car.moveTo(w[0], race.lobby.pos.getY() + 0.05, w[1], TrackBuilder.yaw(race.facing, c[2], c[3]), 0);
            car.speed = 0;
        }
    }

    private static List<Racer> standings(Race race) {
        List<Racer> list = new ArrayList<>(race.racers);
        list.sort(Comparator.<Racer>comparingInt(r -> r.finishTick >= 0 ? 0 : r.out ? 2 : 1).thenComparingInt(r -> r.finishTick >= 0 ? r.finishTick : 0)
                .thenComparing(r -> -r.progress));
        return list;
    }

    private static String time(int ticks) {
        int ms = ticks * 50;
        return String.format(Locale.ROOT, "%d:%02d.%d", ms / 60000, (ms / 1000) % 60, (ms / 100) % 10);
    }

    private static void report(ServerLevel level, Race race) {
        List<Racer> st = standings(race);
        List<String> board = new ArrayList<>();
        for (int i = 0; i < st.size() && i < 8; i++) {
            Racer r = st.get(i);
            String tail = r.finishTick >= 0 ? time(r.finishTick) : r.out && race.phase == 2 && r.player != null ? "fuera" : "vuelta " + r.lap;
            board.add((i + 1) + ". " + r.name + "  " + tail);
        }
        for (int i = 0; i < st.size(); i++) {
            Racer r = st.get(i);
            if (r.player == null) {
                continue;
            }
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(r.player);
            if (p == null) {
                continue;
            }
            if (r.out) {
                // se salió de la carrera (guardó el auto): se le avisa una sola vez
                if (r.lastSent != -2) {
                    r.lastSent = -2;
                    AutosNet.toPlayer(p, new RaceStatePacket(-1, 0, 0, 0, 0, 0, 0, false, List.of(), -1));
                }
                continue;
            }
            AutosNet.toPlayer(p, new RaceStatePacket(race.phase, race.timer, r.lap, race.lobby.laps, i + 1, st.size(),
                    (r.finishTick >= 0 ? r.finishTick : race.phase == 0 ? 0 : race.ticks) * 50, r.finishTick >= 0, board, i));
        }
    }

    /** Termina la carrera: quita los bots y avisa a los jugadores. */
    private static void finish(ServerLevel level, Race race, boolean cancelled) {
        for (Racer r : race.racers) {
            Entity ent = level.getEntity(r.entity);
            if (r.player == null && ent != null) {
                ent.discard();
            }
            if (r.player != null) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(r.player);
                if (p != null) {
                    AutosNet.toPlayer(p, new RaceStatePacket(-1, 0, 0, 0, 0, 0, 0, false, List.of(), -1));
                    if (cancelled) {
                        p.displayClientMessage(Component.literal("§eLa carrera se canceló."), true);
                    }
                }
            }
        }
        race.lobby.race = null;
        BlockState st = level.getBlockState(race.lobby.pos);
        if (st.getBlock() instanceof TrackBlock) {
            level.setBlock(race.lobby.pos, st.setValue(TrackBlock.LIT, false), 3);
        }
        broadcast(level, race.lobby.pos);
    }

    /** ¿Sigue en carrera este bot? */
    public static boolean botAlive(RcCarEntity car) {
        for (Lobby l : LOBBIES.values()) {
            if (l.race != null && l.race.lobby.dim == car.level().dimension()) {
                for (Racer r : l.race.racers) {
                    if (r.player == null && r.entity == car.getId()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** El jugador se fue: ya no está anotado en ninguna sala. */
    public static void forget(UUID player) {
        for (Lobby l : LOBBIES.values()) {
            l.joined.remove(player);
        }
    }
}
