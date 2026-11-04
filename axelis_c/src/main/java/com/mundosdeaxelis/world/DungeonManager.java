package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Brote;
import com.mundosdeaxelis.entity.Espinoso;
import com.mundosdeaxelis.entity.GuardianMusgo;
import com.mundosdeaxelis.entity.Polinizador;
import com.mundosdeaxelis.entity.Raizal;
import com.mundosdeaxelis.entity.ReyBrote;
import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Las reglas de la dungeon procedural del Rey Brote (la obra está en {@link ForestDungeon}, el plano en {@link DungeonLayout}).
 *
 * <ul>
 * <li>Salas de combate (camino principal, mini-jefe y Cámara de la Llave): al entrar alguien suena un cuerno y a los 3 s las puertas
 * de esa sala se cierran; salen oleadas (según el tema de la sala y el número de jugadores) y al acabar se abren.</li>
 * <li>Cámara de la Llave: al vencerla cada jugador recibe una Llave del Santuario (abre cofres sellados de las criptas).</li>
 * <li>Puente de losas (estilo Juego del Calamar): de 5 a 12 filas según los jugadores, en cada una una losa es falsa y se rompe para siempre al pisarla; premio en un cofre.</li>
 * <li>Prueba de runas: hay que repetir una secuencia de cristales; abre la cámara del tesoro.</li>
 * <li>Antesala: cofres de provisiones y cuenta atrás hasta la puerta del jefe; el Rey Brote aparece al pisar la arena.</li>
 * </ul>
 * Si no queda nadie en la dimensión, todo se reinicia.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class DungeonManager {
    public static final int STAGE = 1;
    private static final String ROOM_KEY = "mdc_room";
    private static final int Y = ForestDungeon.Y;
    private static final int HALF = ForestDungeon.HALF;

    private enum St {IDLE, PRE, ACTIVE, CLEARED}

    private enum Kind {BROTE, ESPINOSO, POLINIZADOR, RAIZAL, RAIZAL_ELITE, GUARDIAN}

    private record Spawn(Kind kind, int count) {
    }

    private enum Rg {IDLE, SHOW, INPUT, FAIL, DONE}

    private static final class Room {
        final DungeonLayout.Cell cell;
        St st = St.IDLE;
        int wave = 0;
        int timer = 0;
        List<Spawn[]> waves = new ArrayList<>();
        boolean hinted = false;
        // prueba de runas
        Rg rg = Rg.IDLE;
        int[] seq = new int[0];
        int progress = 0;
        int tick = 0;
        int away = 0;
        final int[] lit = new int[4];

        Room(DungeonLayout.Cell cell) {
            this.cell = cell;
        }

        AABB box() {
            int h = ForestDungeon.height(cell);
            return new AABB(cell.cx() - HALF - 1, Y - 1, cell.cz() - HALF - 1, cell.cx() + HALF + 2, Y + h + 2, cell.cz() + HALF + 2);
        }
    }

    private DungeonLayout layout = null;
    private final Map<Integer, Room> rooms = new HashMap<>();
    private boolean dirty = false;
    private boolean anteStarted = false;
    /** Jugadores a los que ya se les abrió la ficha del jefe al llegar a la antesala. */
    private final java.util.Set<java.util.UUID> anteSeen = new java.util.HashSet<>();
    private int anteTimer = 0;
    private boolean bossGateOpen = false;
    private boolean bossSpawned = false;
    private int emptyTicks = 0;
    private final Map<BlockPos, Integer> crumbling = new HashMap<>();
    private final Map<BlockPos, Integer> restoring = new HashMap<>();

    private static final Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, DungeonManager> INSTANCES = new HashMap<>();

    private DungeonManager() {
    }

    /** El estado de la dungeon de esta copia (cada copia de dimensión tiene el suyo). */
    public static DungeonManager of(net.minecraft.world.level.Level level) {
        return INSTANCES.computeIfAbsent(level.dimension(), k -> new DungeonManager());
    }

    public DungeonLayout layout() {
        return layout;
    }

    public boolean hasLayout() {
        return layout != null;
    }

    /** ¿Hay una pelea en marcha en la sala donde está este bloque? (no se puede forzar una cerradura en pleno combate) */
    public boolean roomBusy(ServerLevel level, BlockPos pos) {
        if (layout == null) {
            return false;
        }
        for (Room r : rooms.values()) {
            if ((r.st == St.ACTIVE || r.st == St.PRE) && r.box().contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                return true;
            }
        }
        return false;
    }

    private Room room(DungeonLayout.Cell c) {
        return rooms.computeIfAbsent(c.id, k -> new Room(c));
    }

    // ------------------------------------------------------------------ inicio y reinicio

    /** Empieza el seguimiento de una dungeon recién construida. */
    public void begin(ServerLevel level, DungeonLayout lay) {
        clearRuntime(level);
        layout = lay;
        for (DungeonLayout.Cell c : lay.cells) {
            rooms.put(c.id, new Room(c));
            if (c.isCombat()) {
                ForestDungeon.lockExits(level, lay, c, true); // la salida está siempre cerrada hasta vencer la sala
            }
        }
    }

    private void clearRuntime(ServerLevel level) {
        DungeonReset.bump(level); // todo enemigo anterior (incluso el guardado en zonas descargadas) pasa a ser viejo y se borra al cargarse
        for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(-130, 0, -60, 130, 192, 520), x -> !(x instanceof net.minecraft.world.entity.player.Player))) {
            if (e instanceof Mob m && (m.getTags().contains(Brote.DUNGEON_TAG) || m instanceof ReyBrote || m instanceof net.minecraft.world.entity.monster.Slime)) {
                m.discard();
            } else if (e instanceof Mob m && !(m instanceof net.minecraft.world.entity.monster.Enemy) && !m.hasCustomName()
                    && !(m instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame())
                    && net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(m.getType()).getNamespace().equals("minecraft")) {
                m.discard(); // animales que hubieran salido antes de la protección
            } else if (e instanceof AbstractArrow || e instanceof net.minecraft.world.entity.item.ItemEntity || e instanceof net.minecraft.world.entity.ExperienceOrb) {
                e.discard();
            }
        }
        rooms.clear();
        crumbling.clear();
        restoring.clear();
        fakeCache.clear();
        dirty = false;
        anteStarted = false;
        anteSeen.clear();
        anteTimer = 0;
        bossGateOpen = false;
        bossSpawned = false;
        emptyTicks = 0;
    }

    /** Quita mobs y olvida la dungeon (la obra se borra al construir la siguiente). */
    public void reset(ServerLevel level) {
        clearRuntime(level);
        layout = null;
    }

    /** Para pruebas: salta todas las salas (puertas abiertas) y deja el camino libre hasta la arena. */
    public void skipAll(ServerLevel level) {
        if (layout == null) {
            return;
        }
        for (DungeonLayout.Cell c : layout.cells) {
            Room r = room(c);
            if (c.isCombat()) {
                r.st = St.CLEARED;
                ForestDungeon.lock(level, c, false);
            }
        }
        dirty = true;
        anteStarted = true;
        bossGateOpen = true;
        ForestDungeon.setBossGate(level, layout, false);
    }

    // ------------------------------------------------------------------ entrada

    // ------------------------------------------------------------------ minimapa

    /** El plano para el minimapa de los jugadores (salas despejadas incluidas). */
    public com.mundosdeaxelis.network.DungeonMapPacket mapPacket() {
        List<int[]> out = new ArrayList<>();
        if (layout == null) {
            return com.mundosdeaxelis.network.DungeonMapPacket.off();
        }
        for (DungeonLayout.Cell c : layout.cells) {
            Room r = room(c);
            boolean done = c.isCombat() ? r.st == St.CLEARED : c.type == DungeonLayout.Type.RUNES && r.rg == Rg.DONE;
            int doors = (c.door[0] ? 1 : 0) | (c.door[1] ? 2 : 0) | (c.door[2] ? 4 : 0) | (c.door[3] ? 8 : 0);
            out.add(new int[]{c.id, c.col, c.row, c.type.ordinal(), doors, c.order >= 0 ? 1 : 0, done ? 1 : 0, ForestDungeon.rectAxis(c)});
        }
        return new com.mundosdeaxelis.network.DungeonMapPacket(true, out);
    }

    public void sendMap(ServerPlayer p) {
        com.mundosdeaxelis.network.ModNetwork.sendTo(p, mapPacket());
    }

    /** Dónde aparece quien entra: el claro (principio), una sala ya despejada del camino, la antesala o la arena (jefe en marcha). */
    public Vec3 entryPoint(ServerLevel level, int i) {
        double dx = i % 2 == 0 ? -1.5 : 1.5;
        double dz = (i / 2) * 1.5;
        if (layout == null || bossSpawned) {
            return ForestArena.arrival(i);
        }
        DungeonLayout.Cell target = layout.start();
        if (dirty) {
            target = layout.ante();
            List<DungeonLayout.Cell> p = layout.path;
            for (int k = p.size() - 2; k >= 1; k--) {
                if (room(p.get(k)).st != St.CLEARED) {
                    // si la pelea ya empezó se entra a ayudar; si no, se espera delante de la sala
                    target = room(p.get(k)).st == St.ACTIVE ? p.get(k) : p.get(k + 1);
                    break;
                }
            }
        }
        boolean center = target.type == DungeonLayout.Type.START || target.type == DungeonLayout.Type.ANTE;
        return new Vec3(target.cx() + 0.5 + dx, Y + 1, target.cz() + 0.5 + (center ? dz : 7.0 + dz));
    }

    /** ¿Hay una dungeon en marcha (todavía sin jefe)? */
    public boolean isInProgress() {
        return layout != null && !bossSpawned;
    }

    // ------------------------------------------------------------------ tick

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.getServer() == null) {
            return;
        }
        for (net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key : ModDimensions.FOREST_COPIES) {
            ServerLevel l = e.getServer().getLevel(key);
            if (l != null) {
                of(l).tick(l);
            }
        }
    }

    private void tick(ServerLevel level) {
        if (layout == null) {
            return;
        }
        if (level == null || ForestDungeon.isBuilding(level)) {
            return;
        }
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isSpectator()) {
                players.add(p);
            }
        }
        if (players.isEmpty()) {
            if (++emptyTicks > 40) { // al quedar vacía (muerte, salida...) se reinicia casi al instante: jefe fuera y dungeon nueva
                reset(level);
            }
            return;
        }
        emptyTicks = 0;
        // las losas falsas se vigilan en cada tick: con el filtro de 5 ticks un jugador corriendo cruzaba la losa sin romperla
        tickCrumble(level, players);
        if (level.getGameTime() % 5 != 0) {
            return;
        }
        if (level.getGameTime() % 40 == 0) {
            com.mundosdeaxelis.network.DungeonMapPacket mp = mapPacket();
            for (ServerPlayer q : players) {
                com.mundosdeaxelis.network.ModNetwork.sendTo(q, mp);
            }
        }
        for (DungeonLayout.Cell c : layout.cells) {
            switch (c.type) {
                case COMBAT, ELITE, KEY -> tickCombat(level, room(c), players);
                case RUNES -> tickRunes(level, room(c), players);
                case PARKOUR, CRYPT -> hint(level, room(c), players);
                default -> {
                }
            }
        }
        tickAnte(level, players);
        tickBoss(level, players);
    }

    private boolean inCell(DungeonLayout.Cell c, ServerPlayer p) {
        if (c.type == DungeonLayout.Type.PARKOUR && c.entry >= 0) {
            return ForestDungeon.inParkourRoom(layout, c, p.getX(), p.getZ()) && p.getY() < Y + 14 && p.getY() > Y - 8;
        }
        return Math.abs(p.getX() - (c.cx() + 0.5)) <= HALF + 0.5 && Math.abs(p.getZ() - (c.cz() + 0.5)) <= HALF + 0.5
                && p.getY() < Y + 14 && p.getY() > Y - 8;
    }

    private List<ServerPlayer> inside(DungeonLayout.Cell c, List<ServerPlayer> players) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : players) {
            if (inCell(c, p)) {
                out.add(p);
            }
        }
        return out;
    }

    private void say(List<ServerPlayer> to, String text, ChatFormatting color) {
        for (ServerPlayer q : to) {
            q.displayClientMessage(Component.literal(text).withStyle(color), true);
        }
    }

    // ------------------------------------------------------------------ salas laterales con aviso

    private void hint(ServerLevel level, Room r, List<ServerPlayer> players) {
        if (r.hinted) {
            return;
        }
        List<ServerPlayer> in = inside(r.cell, players);
        if (in.isEmpty()) {
            return;
        }
        r.hinted = true;
        if (r.cell.type == DungeonLayout.Type.PARKOUR) {
            for (ServerPlayer q : in) {
                Titles.hint(q, "Puente de losas", "En cada fila hay una losa falsa: elige bien");
            }
        } else {
            for (ServerPlayer q : in) {
                Titles.hint(q, "Cripta sellada", "Abre el cofre con una Llave del Santuario");
            }
        }
    }

    // ------------------------------------------------------------------ salas de combate

    private String nameOf(DungeonLayout.Cell c) {
        return switch (c.type) {
            case ELITE -> "Cámara del Guardián";
            case KEY -> "Cámara de la Llave";
            default -> switch (c.theme) {
                case ROOTS -> "Sala de las Raíces";
                case SPINES -> "Galería de Espinas";
                case NURSERY -> "El Vivero";
                case POLLEN -> "Jardín de Polen";
                case RAIZAL -> "Cámara de los Raizales";
            };
        };
    }

    private String hintOf(DungeonLayout.Cell c) {
        return switch (c.type) {
            case ELITE -> "El Guardián golpea el suelo con aviso: salta cuando baje los puños. Con poca vida se enfurece.";
            case KEY -> "Un Raizal mayor guarda una llave. Cuidado con las raíces del suelo.";
            default -> switch (c.theme) {
                case ROOTS -> "Brotes: corren hacia ti. Muévete y no te dejes rodear.";
                case SPINES -> "Los Espinosos de las repisas disparan: sal de su línea o rómpelos.";
                case NURSERY -> "Brotes abajo y Espinosos en las macetas: usa los pilares para cubrirte.";
                case POLLEN -> "Los Polinizadores vuelan y duermen con sus flechas: mátalos pronto.";
                case RAIZAL -> "Los Raizales clavan raíces en el suelo: salta o aléjate cuando brillen.";
            };
        };
    }

    private int cnt(int base, int n, float perPlayer) {
        return Math.max(1, Math.round(base * 1.35F * (1.0F + perPlayer * (n - 1))));
    }

    /** Enemigos chicos: sin el multiplicador x1.35 y con tope, para que ninguna sala se llene de bichitos. */
    private int small(int base, int n) {
        return Math.max(1, Math.min(6 + (n - 1), Math.round(base * (1.0F + 0.35F * (n - 1)))));
    }

    private void buildWaves(Room r, int n) {
        r.waves.clear();
        DungeonLayout.Cell c = r.cell;
        if (c.type == DungeonLayout.Type.ELITE) {
            List<Spawn> w = new ArrayList<>();
            w.add(new Spawn(Kind.GUARDIAN, 1));
            w.add(new Spawn(Kind.RAIZAL, 1 + (n - 1) / 2));
            r.waves.add(w.toArray(new Spawn[0]));
        } else if (c.type == DungeonLayout.Type.KEY) {
            r.waves.add(new Spawn[]{new Spawn(Kind.RAIZAL_ELITE, 1 + (n - 1) / 3), new Spawn(Kind.BROTE, small(3, n))});
            r.waves.add(new Spawn[]{new Spawn(Kind.POLINIZADOR, cnt(2, n, 0.4F)), new Spawn(Kind.BROTE, small(3, n))});
        } else {
            switch (c.theme) {
                case ROOTS -> {
                    r.waves.add(new Spawn[]{new Spawn(Kind.BROTE, small(4, n))});
                    r.waves.add(new Spawn[]{new Spawn(Kind.BROTE, small(5, n))});
                    r.waves.add(new Spawn[]{new Spawn(Kind.BROTE, small(6, n)), new Spawn(Kind.RAIZAL, cnt(1, n, 0.35F))});
                }
                case SPINES -> {
                    r.waves.add(new Spawn[]{new Spawn(Kind.ESPINOSO, Math.min(8, cnt(4, n, 0.35F)))});
                    r.waves.add(new Spawn[]{new Spawn(Kind.ESPINOSO, Math.min(8, cnt(5, n, 0.35F))), new Spawn(Kind.BROTE, small(3, n))});
                }
                case NURSERY -> {
                    r.waves.add(new Spawn[]{new Spawn(Kind.BROTE, small(4, n)), new Spawn(Kind.ESPINOSO, 3)});
                    r.waves.add(new Spawn[]{new Spawn(Kind.BROTE, small(5, n)), new Spawn(Kind.ESPINOSO, 4)});
                    r.waves.add(new Spawn[]{new Spawn(Kind.BROTE, small(7, n)), new Spawn(Kind.ESPINOSO, 5)});
                }
                case POLLEN -> {
                    r.waves.add(new Spawn[]{new Spawn(Kind.POLINIZADOR, cnt(3, n, 0.4F))});
                    r.waves.add(new Spawn[]{new Spawn(Kind.POLINIZADOR, cnt(4, n, 0.4F)), new Spawn(Kind.BROTE, small(3, n))});
                    r.waves.add(new Spawn[]{new Spawn(Kind.POLINIZADOR, cnt(5, n, 0.4F)), new Spawn(Kind.BROTE, small(4, n))});
                }
                case RAIZAL -> {
                    r.waves.add(new Spawn[]{new Spawn(Kind.RAIZAL, cnt(2, n, 0.4F))});
                    r.waves.add(new Spawn[]{new Spawn(Kind.RAIZAL, cnt(3, n, 0.4F)), new Spawn(Kind.BROTE, small(3, n))});
                }
            }
        }
    }

    /** Salas normales: una sola oleada (la más grande). Con cofre con cerradura: dos como máximo. La Cámara de la Llave ya trae dos; el mini-jefe, una. */
    private void trimWaves(Room r) {
        if (r.cell.type != DungeonLayout.Type.COMBAT || r.waves.size() <= 1) {
            return;
        }
        boolean special = ForestDungeon.lockLevelFor(layout, r.cell) > 0;
        Spawn[] last = r.waves.get(r.waves.size() - 1);
        if (special) {
            Spawn[] first = r.waves.get(0);
            r.waves.clear();
            r.waves.add(first);
            r.waves.add(last);
        } else {
            r.waves.clear();
            r.waves.add(last);
        }
    }

    private void tickCombat(ServerLevel level, Room r, List<ServerPlayer> players) {
        DungeonLayout.Cell c = r.cell;
        switch (r.st) {
            case IDLE -> {
                List<ServerPlayer> in = inside(c, players);
                if (in.isEmpty()) {
                    r.timer = 0;
                } else if (in.size() < players.size()) {
                    // la sala no empieza hasta que estén todos dentro: la salida sigue cerrada y la entrada abierta
                    if (r.timer++ % 4 == 0) {
                        say(in, "Esperando al resto del equipo (" + in.size() + "/" + players.size() + ") para cerrar las puertas...", ChatFormatting.YELLOW);
                    }
                } else {
                    r.st = St.PRE;
                    r.timer = 8; // 8 pasos de 5 ticks = 2 s
                    dirty = true;
                    for (ServerPlayer q : players) {
                        Titles.hint(q, nameOf(c), "La puerta de atrás se cerrará...");
                    }
                    say(in, hintOf(c), ChatFormatting.YELLOW);
                    level.playSound(null, in.get(0).blockPosition(), ModSounds.DUNGEON_ROOM_START.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
                }
            }
            case PRE -> {
                if (inside(c, players).size() < players.size()) {
                    r.st = St.IDLE; // alguien salió antes de cerrar
                    r.timer = 0;
                    return;
                }
                if (--r.timer <= 0) {
                    ForestDungeon.lockEntry(level, layout, c, true);
                    level.playSound(null, new BlockPos(c.cx(), Y + 2, c.cz()), ModSounds.DUNGEON_DOOR_CLOSE.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
                    r.st = St.ACTIVE;
                    r.wave = 0;
                    buildWaves(r, Math.max(1, players.size()));
                    trimWaves(r);
                    for (Mob old : level.getEntitiesOfClass(Mob.class, r.box(), m -> m.getTags().contains(Brote.DUNGEON_TAG))) {
                        old.discard();
                    }
                    spawnWave(level, r, players);
                    r.timer = 0;
                }
            }
            case ACTIVE -> {
                if (r.timer > 0) {
                    if (--r.timer == 0) {
                        spawnWave(level, r, players);
                    }
                    return;
                }
                if (alive(level, r) == 0) {
                    if (r.wave + 1 < r.waves.size()) {
                        r.wave++;
                        r.timer = 8; // 40 ticks
                        say(players, "Oleada " + (r.wave + 1) + " de " + r.waves.size(), ChatFormatting.GOLD);
                    } else {
                        clear(level, r, players);
                    }
                }
            }
            default -> {
            }
        }
    }

    private void clear(ServerLevel level, Room r, List<ServerPlayer> players) {
        r.st = St.CLEARED;
        DungeonLayout.Cell c = r.cell;
        for (Mob old : level.getEntitiesOfClass(Mob.class, r.box(), m -> m.getTags().contains(Brote.DUNGEON_TAG))) {
            old.discard(); // los Brotes que invocó el Guardián
        }
        ForestDungeon.lock(level, c, false);
        level.playSound(null, new BlockPos(c.cx(), Y + 2, c.cz()), ModSounds.DUNGEON_DOOR_OPEN.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
        for (ServerPlayer q : players) {
            Titles.hint(q, "Sala despejada", nameOf(c));
            level.playSound(null, q.blockPosition(), ModSounds.DUNGEON_ROOM_CLEAR.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
        }
        // comida de la dungeon: cada jugador recibe una Baya del Santuario y, a veces, una Bellota (repara armadura)
        for (ServerPlayer q : players) {
            ItemStack food = new ItemStack(ModItems.SANCTUARY_BERRY.get(), 1);
            if (!q.getInventory().add(food)) {
                q.drop(food, false);
            }
            Titles.loot(q, ModItems.SANCTUARY_BERRY.get(), 1);
            if (c.type != DungeonLayout.Type.COMBAT || level.random.nextInt(100) < 55) {
                ItemStack acorn = new ItemStack(ModItems.ACORN.get(), 1);
                if (!q.getInventory().add(acorn)) {
                    q.drop(acorn, false);
                }
                Titles.loot(q, ModItems.ACORN.get(), 1);
            }
        }
        // farmeo: cada sala despejada da a cada jugador, la mitad de las veces, 1 material de la etapa (rotando entre los que pide la receta)
        java.util.List<com.mundosdeaxelis.crate.CrateForge.Need> needs = com.mundosdeaxelis.crate.CrateForge.mats(com.mundosdeaxelis.crate.CrateKind.byIndex(STAGE));
        if (!needs.isEmpty()) {
            for (ServerPlayer q : players) {
                if (c.type == DungeonLayout.Type.COMBAT && level.random.nextBoolean()) {
                    continue;
                }
                var mat = needs.get(level.random.nextInt(needs.size())).mat();
                ItemStack drop = new ItemStack(ModItems.MATS.get(mat).get(), 1);
                if (!q.getInventory().add(drop)) {
                    q.drop(drop, false);
                }
                Titles.loot(q, ModItems.MATS.get(mat).get(), 1);
            }
        }
        if (c.type == DungeonLayout.Type.KEY) {
            for (ServerPlayer q : players) {
                ItemStack key = new ItemStack(ModItems.DUNGEON_KEY.get(), 1);
                if (!q.getInventory().add(key)) {
                    q.drop(key, false);
                }
                level.playSound(null, q.blockPosition(), ModSounds.KEY_GET.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                Titles.loot(q, ModItems.DUNGEON_KEY.get(), 1);
            }
        }
    }

    private int alive(ServerLevel level, Room r) {
        return level.getEntitiesOfClass(Mob.class, r.box(), m -> m.isAlive() && m.getTags().contains(Brote.DUNGEON_TAG)
                && m.getPersistentData().getInt(ROOM_KEY) == r.cell.id + 1).size();
    }

    private void tag(Mob m, Room r) {
        m.addTag(Brote.DUNGEON_TAG);
        m.setPersistenceRequired(); // los de la dungeon no desaparecen; los salvajes del mundo sí
        m.getPersistentData().putInt(ROOM_KEY, r.cell.id + 1);
    }

    private ServerPlayer nearest(Vec3 at, List<ServerPlayer> players) {
        ServerPlayer best = null;
        double bd = Double.MAX_VALUE;
        for (ServerPlayer p : players) {
            double d = p.distanceToSqr(at);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    private void spawnWave(ServerLevel level, Room r, List<ServerPlayer> players) {
        DungeonLayout.Cell c = r.cell;
        int n = Math.max(1, players.size());
        Spawn[] wave = r.waves.get(r.wave);
        level.playSound(null, new BlockPos(c.cx(), Y + 2, c.cz()), ModSounds.DUNGEON_WAVE.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
        List<double[]> pts = new ArrayList<>(ForestDungeon.groundPoints(c));
        Collections.shuffle(pts);
        List<double[]> perch = ForestDungeon.perchPoints(c);
        int pi = 0;
        int perchStart = r.wave % 2 == 0 ? 0 : 2;
        int perchUsed = 0;
        for (Spawn sp : wave) {
            for (int i = 0; i < sp.count; i++) {
                double[] p = pts.get(pi++ % pts.size());
                double jx = (level.random.nextDouble() - 0.5) * 0.6;
                double jz = (level.random.nextDouble() - 0.5) * 0.6;
                Mob made = null;
                switch (sp.kind) {
                    case BROTE -> {
                        Brote b = ModEntities.BROTE.get().create(level);
                        if (b != null) {
                            b.moveTo(p[0] + jx, p[1], p[2] + jz, level.random.nextFloat() * 360.0F, 0.0F);
                            b.scaleTo(STAGE, n);
                            made = b;
                        }
                    }
                    case ESPINOSO -> {
                        // cada torreta ocupa una repisa distinta: si ya no quedan repisas libres, no se crea otra encima
                        if (perch.isEmpty() || perchUsed >= perch.size()) {
                            continue;
                        }
                        double[] q = perch.get((perchStart + perchUsed++) % perch.size());
                        Espinoso e = ModEntities.ESPINOSO.get().create(level);
                        if (e != null) {
                            e.moveTo(q[0], q[1], q[2], 0.0F, 0.0F);
                            e.scaleTo(STAGE, n);
                            made = e;
                        }
                    }
                    case POLINIZADOR -> {
                        Polinizador z = ModEntities.POLINIZADOR.get().create(level);
                        if (z != null) {
                            z.moveTo(p[0] + jx, Y + 4.5, p[2] + jz, level.random.nextFloat() * 360.0F, 0.0F);
                            z.scaleTo(STAGE, n);
                            made = z;
                        }
                    }
                    case RAIZAL, RAIZAL_ELITE -> {
                        Raizal z = ModEntities.RAIZAL.get().create(level);
                        if (z != null) {
                            z.moveTo(p[0] + jx, p[1], p[2] + jz, level.random.nextFloat() * 360.0F, 0.0F);
                            z.scaleTo(STAGE, n, sp.kind == Kind.RAIZAL_ELITE);
                            made = z;
                        }
                    }
                    case GUARDIAN -> {
                        GuardianMusgo g = ModEntities.GUARDIAN_MUSGO.get().create(level);
                        if (g != null) {
                            g.moveTo(c.cx() + 0.5, Y + 1, c.cz() + 0.5, 0.0F, 0.0F);
                            g.scaleTo(STAGE, n);
                            made = g;
                            for (ServerPlayer q : players) {
                                Titles.show(q, "Guardián de Musgo", "El guardián del Santuario despierta");
                            }
                        }
                    }
                }
                if (made != null) {
                    settle(level, made, pts, sp.kind == Kind.POLINIZADOR, c);
                    tag(made, r);
                    ServerPlayer tgt = nearest(made.position(), players);
                    if (tgt != null && sp.kind != Kind.ESPINOSO) {
                        made.setTarget(tgt);
                    }
                    level.addFreshEntity(made);
                    burst(level, made.position());
                }
                if (sp.kind == Kind.GUARDIAN) {
                    break;
                }
            }
        }
    }

    /** ¿Cabe la criatura en ese punto sin tocar ningún bloque (y, si camina, con suelo debajo)? */
    private boolean fits(ServerLevel level, Mob m, double x, double y, double z, boolean fly) {
        net.minecraft.world.phys.AABB box = m.getType().getAABB(x, y, z).inflate(0.06, 0.0, 0.06);
        if (!level.noCollision(box)) {
            return false;
        }
        return fly || level.getBlockState(BlockPos.containing(x, y - 0.5, z)).blocksMotion();
    }

    /**
     * Comprueba el punto de aparición: si la criatura quedaría dentro de una pared, un arbusto o un adorno, la sube o la
     * cambia a otro punto libre de la sala (y, en último caso, al centro). Así nunca aparecen enemigos empotrados.
     */
    private void settle(ServerLevel level, Mob m, List<double[]> alt, boolean fly, DungeonLayout.Cell c) {
        double x = m.getX(), y = m.getY(), z = m.getZ();
        if (fits(level, m, x, y, z, fly)) {
            return;
        }
        for (int dy = 1; dy <= 3; dy++) {
            if (fits(level, m, x, y + dy, z, fly)) {
                m.moveTo(x, y + dy, z, m.getYRot(), 0.0F);
                return;
            }
        }
        for (double[] q : alt) {
            double qy = fly ? Y + 4.5 : q[1];
            if (fits(level, m, q[0], qy, q[2], fly)) {
                m.moveTo(q[0], qy, q[2], m.getYRot(), 0.0F);
                return;
            }
        }
        for (int ring = 0; ring <= 6; ring += 2) {
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                double qx = c.cx() + 0.5 + Math.cos(a) * ring, qz = c.cz() + 0.5 + Math.sin(a) * ring;
                double qy = fly ? Y + 4.5 : Y + 1;
                if (fits(level, m, qx, qy, qz, fly)) {
                    m.moveTo(qx, qy, qz, m.getYRot(), 0.0F);
                    return;
                }
            }
        }
    }

    private void burst(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, at.x, at.y + 0.6, at.z, 14, 0.4, 0.5, 0.4, 0.02);
        level.sendParticles(ParticleTypes.COMPOSTER, at.x, at.y + 0.4, at.z, 10, 0.4, 0.3, 0.4, 0.05);
    }

    // ------------------------------------------------------------------ prueba de saltos

    private final Map<Integer, Map<BlockPos, Integer>> fakeCache = new HashMap<>();

    private record Stamp(Vec3 pos, long tick) {
    }

    private final Map<java.util.UUID, Stamp> lastSeen = new HashMap<>();

    private Map<BlockPos, Integer> fakesOf(DungeonLayout.Cell c) {
        return fakeCache.computeIfAbsent(c.id, k -> ForestDungeon.parkourFakes(layout, c));
    }

    private void tickCrumble(ServerLevel level, List<ServerPlayer> players) {
        // detectar losas falsas pisadas: se rompe solo esa losa (y se queda rota para quien venga después)
        for (DungeonLayout.Cell c : layout.cells) {
            if (c.type != DungeonLayout.Type.PARKOUR) {
                continue;
            }
            Map<BlockPos, Integer> fakes = fakesOf(c);
            for (ServerPlayer p : inside(c, players)) {
                // Se rompe un poco ANTES de que la pises: miramos dónde van a estar tus pies en los próximos 3 ticks (por tu velocidad) y
                // contamos desde 0,6 bloques por encima de la losa (un salto por encima, más alto, la deja intacta). Así, aunque el cliente
                // vaya un poco por detrás del servidor, no llegas a sentirla sólida.
                var bb = p.getBoundingBox();
                Stamp st = lastSeen.get(p.getUUID());
                long now = level.getGameTime();
                double vx = 0.0;
                double vz = 0.0;
                if (st != null && st.tick() == now - 1) {
                    vx = p.getX() - st.pos().x;
                    vz = p.getZ() - st.pos().z;
                    double sp = Math.hypot(vx, vz);
                    if (sp > 0.6) {
                        vx *= 0.6 / sp;
                        vz *= 0.6 / sp;
                    }
                }
                lastSeen.put(p.getUUID(), new Stamp(p.position(), now));
                BlockPos hit = null;
                double feet = p.getY();
                if (feet <= ForestDungeon.Y + 1.0 + 0.6 && feet >= ForestDungeon.Y + 1.0 - 0.3) {
                    search:
                    for (int k = 0; k <= 3; k++) {
                        for (double px : new double[]{bb.minX + 0.02, bb.maxX - 0.02}) {
                            for (double pz : new double[]{bb.minZ + 0.02, bb.maxZ - 0.02}) {
                                BlockPos under = BlockPos.containing(px + vx * k, ForestDungeon.Y + 0.5, pz + vz * k);
                                Integer id = fakes.get(under);
                                if (id != null && level.getBlockState(under).is(ForestDungeon.padBlock().getBlock()) && !crumbling.containsKey(under)) {
                                    hit = under;
                                    break search;
                                }
                            }
                        }
                    }
                }
                if (hit == null) {
                    continue;
                }
                Integer id = fakes.get(hit);
                for (Map.Entry<BlockPos, Integer> f : fakes.entrySet()) {
                    if (f.getValue().equals(id) && level.getBlockState(f.getKey()).is(ForestDungeon.padBlock().getBlock())) {
                        crumbling.put(f.getKey(), 1); // se rompe en este mismo tick: corriendo o saltando, la losa falsa cae al primer contacto
                    }
                }
                level.playSound(null, hit, ModSounds.CRUMBLE.get(), SoundSource.BLOCKS, 0.9F, 1.4F);
                level.sendParticles(ParticleTypes.CRIT, hit.getX() + 0.5, hit.getY() + 1.0, hit.getZ() + 0.5, 8, 0.4, 0.05, 0.4, 0.02);
                p.displayClientMessage(Component.literal("¡Losa falsa!").withStyle(ChatFormatting.RED), true);
            }
        }
        // romper
        List<BlockPos> done = new ArrayList<>();
        for (Map.Entry<BlockPos, Integer> en : crumbling.entrySet()) {
            int left = en.getValue() - 1;
            if (left <= 0) {
                BlockPos q = en.getKey();
                level.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.POOF, q.getX() + 0.5, q.getY() + 0.8, q.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.02);
                done.add(q); // las losas falsas no vuelven: así el resto del equipo aprende el camino
            } else {
                en.setValue(left);
                if (left == 2) {
                    BlockPos q = en.getKey();
                    level.sendParticles(ParticleTypes.CRIT, q.getX() + 0.5, q.getY() + 1.0, q.getZ() + 0.5, 3, 0.3, 0.05, 0.3, 0.01);
                }
            }
        }
        for (BlockPos q : done) {
            crumbling.remove(q);
            level.playSound(null, q, ModSounds.CRUMBLE.get(), SoundSource.BLOCKS, 0.9F, 0.9F);
        }
        // volver a poner
        List<BlockPos> back = new ArrayList<>();
        for (Map.Entry<BlockPos, Integer> en : restoring.entrySet()) {
            int left = en.getValue() - 1;
            if (left <= 0) {
                back.add(en.getKey());
            } else {
                en.setValue(left);
            }
        }
        for (BlockPos q : back) {
            restoring.remove(q);
            if (level.getBlockState(q).isAir()) {
                level.setBlock(q, ForestDungeon.padBlock(), 2);
            }
        }
    }

    // ------------------------------------------------------------------ prueba de runas

    private int seqLength() {
        return 4 + Math.min(3, (layout.players - 1) / 2);
    }

    private void allCrystals(ServerLevel level, Room r, boolean on) {
        for (int i = 0; i < 4; i++) {
            ForestDungeon.setCrystal(level, r.cell, i, on);
            r.lit[i] = 0;
        }
    }

    private void tickRunes(ServerLevel level, Room r, List<ServerPlayer> players) {
        DungeonLayout.Cell c = r.cell;
        List<ServerPlayer> in = inside(c, players);
        if (r.rg == Rg.DONE) {
            return;
        }
        // cristales que se apagan solos tras un toque
        if (r.rg == Rg.INPUT) {
            for (int i = 0; i < 4; i++) {
                if (r.lit[i] > 0 && --r.lit[i] == 0) {
                    ForestDungeon.setCrystal(level, c, i, false);
                }
            }
        }
        if (in.isEmpty()) {
            if (r.rg != Rg.IDLE && ++r.away > 40) {
                r.rg = Rg.IDLE;
                allCrystals(level, r, false);
                r.away = 0;
            }
            return;
        }
        r.away = 0;
        switch (r.rg) {
            case IDLE -> {
                Random rnd = new Random(c.seed * 31L + 7L);
                r.seq = new int[seqLength()];
                for (int i = 0; i < r.seq.length; i++) {
                    r.seq[i] = rnd.nextInt(4);
                }
                r.rg = Rg.SHOW;
                r.tick = -8;
                for (ServerPlayer q : in) {
                    Titles.hint(q, "Prueba de runas", "Mira la secuencia y repítela");
                }
            }
            case SHOW -> {
                int t = r.tick++;
                if (t < 0) {
                    return;
                }
                int k = t / 6;
                int rem = t % 6;
                if (k < r.seq.length) {
                    if (rem == 0) {
                        ForestDungeon.setCrystal(level, c, r.seq[k], true);
                        BlockPos p = ForestDungeon.pedestals(c)[r.seq[k]];
                        level.playSound(null, p, ModSounds.rune(r.seq[k]).get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                        level.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.02);
                    } else if (rem == 4) {
                        ForestDungeon.setCrystal(level, c, r.seq[k], false);
                    }
                } else if (t >= r.seq.length * 6 + 3) {
                    allCrystals(level, r, false);
                    r.rg = Rg.INPUT;
                    r.progress = 0;
                    say(in, "Tu turno: toca los cristales en el mismo orden (" + r.seq.length + ")", ChatFormatting.AQUA);
                }
            }
            case FAIL -> {
                if (--r.timer <= 0) {
                    allCrystals(level, r, false);
                    r.rg = Rg.SHOW;
                    r.tick = -4;
                }
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel() instanceof ServerLevel l && !l.isClientSide() && ModDimensions.isForest(l.dimension())) {
            of(l).rightClick(e);
        }
    }

    private void rightClick(PlayerInteractEvent.RightClickBlock e) {
        if (layout == null || e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND
                || !(e.getEntity() instanceof ServerPlayer sp) || !(e.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = e.getPos();
        for (DungeonLayout.Cell c : layout.cells) {
            if (c.type != DungeonLayout.Type.RUNES || Math.abs(pos.getX() - c.cx()) > HALF || Math.abs(pos.getZ() - c.cz()) > HALF) {
                continue;
            }
            BlockPos[] ped = ForestDungeon.pedestals(c);
            for (int i = 0; i < 4; i++) {
                if (pos.equals(ped[i]) || pos.equals(ped[i].below())) {
                    e.setCanceled(true);
                    e.setCancellationResult(InteractionResult.SUCCESS);
                    touch(level, room(c), i, sp);
                    return;
                }
            }
        }
    }

    private void touch(ServerLevel level, Room r, int i, ServerPlayer sp) {
        DungeonLayout.Cell c = r.cell;
        switch (r.rg) {
            case DONE -> {
                sp.displayClientMessage(Component.literal("Ya superaste esta prueba.").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            case INPUT -> {
            }
            default -> {
                sp.displayClientMessage(Component.literal("Espera: mira la secuencia.").withStyle(ChatFormatting.GRAY), true);
                return;
            }
        }
        BlockPos p = ForestDungeon.pedestals(c)[i];
        ForestDungeon.setCrystal(level, c, i, true);
        r.lit[i] = 3;
        level.playSound(null, p, ModSounds.rune(i).get(), SoundSource.BLOCKS, 1.5F, 1.0F);
        if (r.seq[r.progress] == i) {
            r.progress++;
            sp.displayClientMessage(Component.literal(r.progress + " / " + r.seq.length).withStyle(ChatFormatting.GREEN), true);
            if (r.progress >= r.seq.length) {
                r.rg = Rg.DONE;
                allCrystals(level, r, true);
                ForestDungeon.setRuneGate(level, c, false);
                level.playSound(null, p, ModSounds.RUNE_OK.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
                for (ServerPlayer q : level.players()) {
                    if (inCell(c, q)) {
                        Titles.hint(q, "Prueba superada", "La cámara del tesoro se abre");
                    }
                }
            }
        } else {
            r.rg = Rg.FAIL;
            r.timer = 8;
            level.playSound(null, p, ModSounds.RUNE_FAIL.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
            level.sendParticles(ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
            sp.displayClientMessage(Component.literal("Secuencia incorrecta. Mira otra vez.").withStyle(ChatFormatting.RED), true);
        }
    }

    // ------------------------------------------------------------------ antesala y jefe

    private void tickAnte(ServerLevel level, List<ServerPlayer> players) {
        if (bossGateOpen) {
            return;
        }
        DungeonLayout.Cell a = layout.ante();
        if (!anteStarted) {
            if (!inside(a, players).isEmpty()) {
                anteStarted = true;
                anteTimer = 20 * 4; // 20 s en pasos de 5 ticks
                for (ServerPlayer q : players) {
                    Titles.hint(q, "Antesala", "Cada uno tiene sus provisiones: coge lo tuyo");
                }
                BossSkip.offer(players);
            }
            return;
        }
        // cada jugador que llega a la antesala recibe la ficha del jefe (los que siguen peleando en otras salas no)
        for (ServerPlayer q : inside(a, players)) {
            if (anteSeen.add(q.getUUID())) {
                BossSkip.dossier(q, players);
            }
        }
        anteTimer--;
        if (anteTimer > 0 && anteTimer % 20 == 0) {
            say(players, "La puerta del jefe se abre en " + (anteTimer / 4) + " s", ChatFormatting.GOLD);
        }
        if (anteTimer <= 0) {
            bossGateOpen = true;
            ForestDungeon.setBossGate(level, layout, false);
            level.playSound(null, new BlockPos(a.cx(), Y + 2, a.cz() - ForestDungeon.WALL), ModSounds.DUNGEON_BOSS_GATE.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
            for (ServerPlayer q : players) {
                Titles.show(q, "La puerta del jefe se abrió", "El Rey Brote te espera");
            }
            BossSkip.remind(players);
        }
    }

    private void tickBoss(ServerLevel level, List<ServerPlayer> players) {
        if (!bossGateOpen || bossSpawned) {
            return;
        }
        for (ServerPlayer p : players) {
            if (p.getZ() <= 29.5 && Math.hypot(p.getX(), p.getZ()) <= 31.0) {
                bossSpawned = true;
                BossIntro.play(level, players, BossIntro.Kind.FOREST);
                return;
            }
        }
    }
}
