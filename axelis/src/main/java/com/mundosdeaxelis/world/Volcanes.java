package com.mundosdeaxelis.world;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mundosdeaxelis.MundosDeAxelis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registro de los volcanes del mundo (etapa 255), guardado con el mundo:
 * <ul>
 * <li><b>Reaparecer para volver a jugar</b>: si nadie se acerca a un volcán en 10 minutos, la próxima vez que alguien llegue
 * vuelven sus enemigos (y la Gran Salamandra), se cierran sus rejas y se apagan los braseros; los cofres se rellenan como mucho
 * una vez por hora (cada jugador, su botín nuevo).</li>
 * <li><b>Música</b>: dentro de la dungeon (bajo la ladera) suena la música del volcán; fuera, la normal.</li>
 * <li><b>Volcanes que aparecen solos</b>: los decide y construye {@link NaturalGen} (al terminar, se apuntan aquí).</li>
 * </ul>
 * Comandos: {@code /mundos volcan cerca | reiniciar | generar | natural <sí/no>}.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Volcanes extends SavedData {
    private static final String NAME = "mundos_volcanes";
    /** Marcas en los enemigos: de qué volcán son y su número en la lista del plano. */
    static final String TAG = "MundosVolcan";
    static final String TAG_I = "MundosVolcanI";
    static final long ABSENT = 12000L;
    static final long LOOT_EVERY = 72000L;
    /** Lejos y cerca para la vuelta de los enemigos (en bloques desde el centro del volcán). */
    static final double FAR = 160, NEAR = 64;

    static final class V {
        int id;
        ResourceKey<Level> dim;
        BlockPos origin;
        Rotation rot;
        boolean natural;
        boolean present = true;
        long absentSince;
        long lastLoot;

        BlockPos center() {
            return StructPlacer.world(origin, rot, 0, 0, DesignVolcan.CZ);
        }

        BlockPos world(int x, int y, int z) {
            return StructPlacer.world(origin, rot, x, y, z);
        }

        /** Del mundo a coordenadas del plano. */
        BlockPos local(BlockPos p) {
            BlockPos d = p.subtract(origin);
            Rotation inv = switch (rot) {
                case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
                case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
                default -> rot;
            };
            return d.rotate(inv);
        }
    }

    private final List<V> list = new ArrayList<>();
    /** Regiones y su interruptor de la etapa 255: ahora viven en {@link NaturalGen} (se pasan allí una vez). */
    private final Map<Long, Integer> regions = new HashMap<>();
    private int nextId = 1;
    private Boolean natural;

    Map<Long, Integer> takeOldRegions() {
        Map<Long, Integer> m = new HashMap<>(regions);
        if (!regions.isEmpty()) {
            regions.clear();
            setDirty();
        }
        return m;
    }

    Boolean takeOldNatural() {
        Boolean b = natural;
        if (natural != null) {
            natural = null;
            setDirty();
        }
        return b;
    }

    public Volcanes() {
    }

    public static Volcanes get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Volcanes::load, Volcanes::new, NAME);
    }

    // ================================================================== datos del plano (enemigos, rejas, braseros, cofres)

    /** Lo que hace falta del plano para reiniciar un volcán, sin guardar el plano entero. */
    record Info(List<StructBp.Mob> mobs, List<Long> gates, List<Long> braziers, List<Long> chests, List<Long> altars) {
        static Info of(StructBp bp) {
            List<Long> g = new ArrayList<>();
            List<Long> br = new ArrayList<>();
            List<Long> ch = new ArrayList<>();
            List<Long> al = new ArrayList<>();
            for (Map.Entry<Long, String> e : bp.m.entrySet()) {
                String s = e.getValue();
                if (s.startsWith("mundos_de_axelis:reja_volcan")) {
                    g.add(e.getKey());
                } else if (s.startsWith("mundos_de_axelis:brasero_volcan")) {
                    br.add(e.getKey());
                } else if (s.startsWith("mundos_de_axelis:vault_chest")) {
                    ch.add(e.getKey());
                } else if (s.startsWith("mundos_de_axelis:altar_maldito")) {
                    al.add(e.getKey());
                }
            }
            return new Info(List.copyOf(bp.mobs), g, br, ch, al);
        }
    }

    private static volatile Info info;
    private static CompletableFuture<Info> infoJob;

    /** Cuando ya se tiene el plano a mano (al construir), sus datos quedan listos sin esperar. */
    static void prime(StructBp bp) {
        if (info == null) {
            info = Info.of(bp);
        }
    }

    /** Los datos del plano, si ya están (se calculan aparte la primera vez para no trabar el juego). */
    static Info info() {
        if (info != null) {
            return info;
        }
        if (infoJob == null) {
            infoJob = CompletableFuture.supplyAsync(() -> Info.of(StructDesigns.make("volcan")));
        }
        if (infoJob.isDone()) {
            try {
                info = infoJob.join();
            } catch (Exception ex) {
                MundosDeAxelis.LOGGER.error("No se pudo preparar el plano del volcán", ex);
                infoJob = null;
            }
        }
        return info;
    }

    // ================================================================== alta y baja

    /** Marca (antes de entrar al mundo) cada enemigo del volcán con su número en la lista del plano. */
    /** Etapa 269: ¿hay un volcán del mod a menos de {@code r} bloques (en horizontal) de ese punto? */
    public static boolean near(ServerLevel level, BlockPos pos, int r) {
        for (V v : get(level.getServer()).list) {
            if (v.dim == level.dimension()) {
                BlockPos c = v.center();
                long dx = c.getX() - pos.getX(), dz = c.getZ() - pos.getZ();
                if (dx * dx + dz * dz <= (long) r * r) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void tagMob(int index, Mob mob) {
        mob.getPersistentData().putInt(TAG_I, index);
    }

    /** Apunta un volcán recién construido (y marca sus enemigos con su número). Devuelve su número. */
    public static int register(ServerLevel level, BlockPos origin, Rotation rot, boolean natural, List<UUID> mobs) {
        Volcanes vs = get(level.getServer());
        V v = new V();
        v.id = vs.nextId++;
        v.dim = level.dimension();
        v.origin = origin.immutable();
        v.rot = rot;
        v.natural = natural;
        v.present = true;
        v.lastLoot = level.getGameTime();
        vs.list.add(v);
        for (UUID id : mobs) {
            if (id != null && level.getEntity(id) instanceof Mob m) {
                m.getPersistentData().putInt(TAG, v.id);
            }
        }
        vs.setDirty();
        return v.id;
    }

    /** Lo quita del registro, junto con los enemigos que tenga (también los que volvieron después). */
    public static void unregister(MinecraftServer server, int id) {
        Volcanes vs = get(server);
        for (V v : vs.list) {
            ServerLevel level = v.id == id ? server.getLevel(v.dim) : null;
            if (level != null) {
                // los habitantes ya rescatados (Ignacio, etapa 263) se quedan: ya no son parte del volcán
                for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(v.center()).inflate(100, 120, 100),
                        m -> m.getPersistentData().getInt(TAG) == id && !(m instanceof com.mundosdeaxelis.entity.Habitante h
                                && h.state() != com.mundosdeaxelis.entity.Habitante.PRESO))) {
                    m.discard();
                }
            }
        }
        if (vs.list.removeIf(v -> v.id == id)) {
            vs.setDirty();
        }
    }

    // ================================================================== guardar

    public static Volcanes load(CompoundTag tag) {
        Volcanes vs = new Volcanes();
        vs.nextId = Math.max(1, tag.getInt("NextId"));
        vs.natural = tag.contains("Natural") ? tag.getBoolean("Natural") : null;
        for (Tag t : tag.getList("Volcanes", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            V v = new V();
            v.id = c.getInt("Id");
            v.dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(c.getString("Dim")));
            v.origin = BlockPos.of(c.getLong("Origin"));
            v.rot = Rotation.values()[Mth.clamp(c.getInt("Rot"), 0, 3)];
            v.natural = c.getBoolean("Natural");
            v.present = c.getBoolean("Present");
            v.absentSince = c.getLong("AbsentSince");
            v.lastLoot = c.getLong("LastLoot");
            vs.list.add(v);
        }
        CompoundTag r = tag.getCompound("Regions");
        for (String k : r.getAllKeys()) {
            try {
                vs.regions.put(Long.parseLong(k), r.getInt(k));
            } catch (NumberFormatException ignored) {
            }
        }
        return vs;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("NextId", nextId);
        if (natural != null) {
            tag.putBoolean("Natural", natural);
        }
        ListTag l = new ListTag();
        for (V v : list) {
            CompoundTag c = new CompoundTag();
            c.putInt("Id", v.id);
            c.putString("Dim", v.dim.location().toString());
            c.putLong("Origin", v.origin.asLong());
            c.putInt("Rot", v.rot.ordinal());
            c.putBoolean("Natural", v.natural);
            c.putBoolean("Present", v.present);
            c.putLong("AbsentSince", v.absentSince);
            c.putLong("LastLoot", v.lastLoot);
            l.add(c);
        }
        tag.put("Volcanes", l);
        CompoundTag r = new CompoundTag();
        for (Map.Entry<Long, Integer> e : regions.entrySet()) {
            r.putInt(Long.toString(e.getKey()), e.getValue());
        }
        tag.put("Regions", r);
        return tag;
    }

    // ================================================================== cada segundo: música, vuelta de enemigos, volcanes nuevos

    private static final Map<UUID, Integer> ZONE = new HashMap<>();

    /** Al cerrar el servidor: se olvida lo que estaba en marcha (la música de cada jugador). */
    public static void clearRuntime() {
        ZONE.clear();
        HINTS.clear();
        DESPERTO.clear();
        info = null;
        infoJob = null;
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        ZONE.remove(e.getEntity().getUUID());
        HINTS.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = e.getServer();
        int t = server.getTickCount();
        if (t % 20 != 7) {
            return;
        }
        Volcanes vs = get(server);
        vs.music(server);
        vs.primeraVista(server);
        if (t % 40 == 7) {
            for (V v : new ArrayList<>(vs.list)) {
                ServerLevel level = server.getLevel(v.dim);
                if (level != null) {
                    vs.visit(level, v);
                }
            }
        }
    }

    // ================================================================== etapa 287: la primera vez que lo ves, ruge

    /** En el jugador (sobrevive a la muerte): los volcanes que ya vio despertar. */
    static final String VISTOS = "mdc_volcanes_vistos";
    /** Cuándo fue el último gran despertar de cada volcán (para no encadenar varios si llegan varios jugadores a la vez). */
    private static final Map<Integer, Long> DESPERTO = new HashMap<>();

    static boolean visto(ServerPlayer p, int id) {
        for (int i : p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).getIntArray(VISTOS)) {
            if (i == id) {
                return true;
            }
        }
        return false;
    }

    static void marcarVisto(ServerPlayer p, int id) {
        CompoundTag per = p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        int[] a = per.getIntArray(VISTOS);
        int[] b = java.util.Arrays.copyOf(a, a.length + 1);
        b[a.length] = id;
        per.putIntArray(VISTOS, b);
        p.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, per);
    }

    /** Punto que hay que ver: en el aire, sobre el centro del cráter (donde sube la columna de fuego; el borde no lo tapa). */
    static BlockPos cima(V v) {
        return v.world(0, DesignVolcan.H + 26, DesignVolcan.CZ);
    }

    /** Para la prueba con el cliente: ¿ese jugador ya vio despertar algún volcán? */
    public static boolean vioAlguno(ServerPlayer p) {
        return p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).getIntArray(VISTOS).length > 0;
    }

    /**
     * ¿Está mirando ese jugador hacia el cráter y sin nada que lo tape? (a menos de 230 bloques, con el cráter dentro de la vista y
     * sin bloques opacos entre sus ojos y la cima; si el camino pasa por terreno sin cargar, cuenta como que no lo ve).
     */
    static boolean loVe(ServerPlayer p, BlockPos cima) {
        net.minecraft.world.phys.Vec3 eye = p.getEyePosition();
        net.minecraft.world.phys.Vec3 to = net.minecraft.world.phys.Vec3.atCenterOf(cima).subtract(eye);
        double dist = to.length();
        if (dist < 6 || dist > 230) {
            return false;
        }
        net.minecraft.world.phys.Vec3 dir = to.scale(1.0 / dist);
        if (p.getLookAngle().dot(dir) < 0.77) { // unos 40° alrededor del centro de la vista
            return false;
        }
        ServerLevel level = p.serverLevel();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (double d = 1.0; d < dist - 4; d += 0.8) {
            m.set(eye.x + dir.x * d, eye.y + dir.y * d, eye.z + dir.z * d);
            if (!level.hasChunkAt(m)) {
                return false;
            }
            if (level.getBlockState(m).canOcclude()) {
                return false;
            }
        }
        return true;
    }

    /** Cada segundo: quien ve por primera vez un volcán lo ve despertar con una gran erupción. */
    private void primeraVista(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || !p.isAlive()) {
                continue;
            }
            for (V v : list) {
                if (v.dim != p.level().dimension()) {
                    continue;
                }
                BlockPos c = v.center();
                double dx = c.getX() + 0.5 - p.getX(), dz = c.getZ() + 0.5 - p.getZ();
                if (dx * dx + dz * dz > 230 * 230 || visto(p, v.id)) {
                    continue;
                }
                BlockPos l = v.local(p.blockPosition());
                if (DesignVolcan.inside(l.getX(), l.getY(), l.getZ()) || !p.serverLevel().isLoaded(cima(v)) || !loVe(p, cima(v))) {
                    continue;
                }
                marcarVisto(p, v.id);
                despertar(p.serverLevel(), v, p);
            }
        }
    }

    /** El gran despertar: tres erupciones seguidas y un título para quien lo vio. */
    static void despertar(ServerLevel level, V v, ServerPlayer quien) {
        quien.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 70, 25));
        quien.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                Component.translatable("title.mundos_de_axelis.volcan.despierta.sub").withStyle(net.minecraft.ChatFormatting.GOLD)));
        quien.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                Component.translatable("title.mundos_de_axelis.volcan.despierta").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD)));
        long now = level.getGameTime();
        Long last = DESPERTO.get(v.id);
        if (last != null && now - last < 300) {
            return; // ya está rugiendo por otro jugador: no se encadenan más
        }
        DESPERTO.put(v.id, now);
        BlockPos core = v.world(0, DesignVolcan.LAVA_Y, DesignVolcan.CZ);
        for (int i = 0; i < 3; i++) {
            Scheduler.runLater(10 + i * 50, () -> com.mundosdeaxelis.block.NucleoVolcanBlock.erupt(level, core, level.random));
        }
    }

    /** Cuándo vio cada jugador cada pista por última vez (para no repetirlas cada segundo). */
    private static final Map<UUID, long[]> HINTS = new HashMap<>();
    static final long HINT_EVERY = 20L * 120;

    /**
     * Avisa a cada jugador cuando entra o sale de la dungeon de un volcán (para la música) y le da las pistas de la etapa 263: en el
     * cráter, que se tire al lago; junto al altar, que la Piedra de regreso lo saca.
     */
    private void music(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            int zone = 0;
            for (V v : list) {
                if (v.dim != p.level().dimension() || v.center().distSqr(p.blockPosition()) > 130 * 130) {
                    continue;
                }
                BlockPos l = v.local(p.blockPosition());
                if (DesignVolcan.inside(l.getX(), l.getY(), l.getZ())) {
                    zone = 1;
                    if (Math.abs(l.getY() - DesignVolcan.F_SANCT) <= 2 && Math.hypot(l.getX() - DesignVolcan.RET_X, l.getZ() - DesignVolcan.RET_Z) <= 5) {
                        hint(p, 1, "message.mundos_de_axelis.volcan.regreso_pista");
                    }
                    // etapa 287: en la sala de erupciones, con su reja cerrada, se avisa de que la abre el Altar Maldito
                    if (Math.abs(l.getY() - DesignVolcan.F_ERUPT) <= 2 && l.getX() >= -16 && l.getX() <= 14 && l.getZ() >= 66 && l.getZ() <= 86) {
                        BlockState g = p.level().getBlockState(v.world(DesignVolcan.GATE_ALTAR_X, DesignVolcan.F_ERUPT, DesignVolcan.GATE_ALTAR_Z));
                        if (g.getBlock() instanceof com.mundosdeaxelis.block.RejaVolcanBlock && !g.getValue(com.mundosdeaxelis.block.RejaVolcanBlock.OPEN)) {
                            hint(p, 2, "message.mundos_de_axelis.volcan.altar_pista");
                        }
                    }
                    break;
                }
                if (!p.isSpectator() && l.getY() >= DesignVolcan.LAVA_Y && l.getY() <= DesignVolcan.H + 4
                        && Math.hypot(l.getX(), l.getZ() - DesignVolcan.CZ) <= DesignVolcan.RR + 1) {
                    hint(p, 0, "message.mundos_de_axelis.volcan.salta_pista");
                }
            }
            Integer was = ZONE.get(p.getUUID());
            if (was == null || was != zone) {
                ZONE.put(p.getUUID(), zone);
                com.mundosdeaxelis.network.ModNetwork.sendTo(p, new com.mundosdeaxelis.network.VolcanZonaPacket(zone));
            }
        }
    }

    private static void hint(ServerPlayer p, int which, String key) {
        long now = p.serverLevel().getGameTime();
        long[] last = HINTS.computeIfAbsent(p.getUUID(), u -> new long[]{Long.MIN_VALUE / 2, Long.MIN_VALUE / 2, Long.MIN_VALUE / 2});
        if (now - last[which] >= HINT_EVERY) {
            last[which] = now;
            p.displayClientMessage(Component.translatable(key), true);
        }
    }

    /**
     * La Piedra de regreso (etapa 263): lleva a quien la usa a la entrada del volcán donde está (al pie del camino, mirando a la
     * montaña). Si la piedra no es de ningún volcán apuntado, avisa y no hace nada.
     */
    public static void goBack(ServerPlayer sp, BlockPos stone) {
        ServerLevel level = sp.serverLevel();
        V v = nearest(level, stone);
        if (v == null || v.center().distSqr(stone) > 130 * 130) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.volcan.regreso_sin"), true);
            return;
        }
        BlockPos door = v.world(0, 0, -2);
        BlockPos c = v.center();
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, door.getX(), door.getZ());
        float yaw = (float) Math.toDegrees(Math.atan2(-(c.getX() - door.getX()), c.getZ() - door.getZ()));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, sp.getX(), sp.getY() + 1.0, sp.getZ(), 40, 0.4, 0.9, 0.4, 0.05);
        level.playSound(null, sp.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.7F);
        sp.stopRiding();
        sp.teleportTo(level, door.getX() + 0.5, y, door.getZ() + 0.5, yaw, 0.0F);
        sp.resetFallDistance();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, door.getX() + 0.5, y + 1.0, door.getZ() + 0.5, 40, 0.4, 0.9, 0.4, 0.05);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, door.getX() + 0.5, y + 0.5, door.getZ() + 0.5, 12, 0.5, 0.3, 0.5, 0.02);
        level.playSound(null, BlockPos.containing(door.getX() + 0.5, y, door.getZ() + 0.5), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.9F);
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.volcan.regreso"), true);
    }

    /** Si nadie se acercó en 10 minutos, al llegar alguien el volcán se reinicia. */
    private void visit(ServerLevel level, V v) {
        BlockPos c = v.center();
        boolean far = false;
        boolean near = false;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) {
                continue;
            }
            double d2 = p.blockPosition().distSqr(c);
            far |= d2 < FAR * FAR;
            near |= d2 < NEAR * NEAR;
        }
        long now = level.getGameTime();
        if (!far) {
            if (v.present) {
                v.present = false;
                v.absentSince = now;
                setDirty();
            }
            return;
        }
        if (near && !v.present) {
            v.present = true;
            setDirty();
            if (now - v.absentSince >= ABSENT) {
                reset(level, v, false);
            }
        }
    }

    /** Vuelven los enemigos que faltan, se cierran las rejas, se apagan los braseros y (si toca) se rellenan los cofres. */
    int reset(ServerLevel level, V v, boolean forceLoot) {
        Info in = info();
        if (in == null) {
            v.present = false; // todavía se está preparando el plano: se reintentará en la próxima vuelta
            return -1;
        }
        long now = level.getGameTime();
        // quiénes siguen vivos
        BlockPos c = v.center();
        Set<Integer> alive = new HashSet<>();
        AABB box = new AABB(c).inflate(80, 90, 80);
        for (Mob m : level.getEntitiesOfClass(Mob.class, box, m -> m.getPersistentData().getInt(TAG) == v.id)) {
            if (m.isAlive()) {
                alive.add(m.getPersistentData().getInt(TAG_I));
                m.setHealth(m.getMaxHealth());
            }
        }
        int back = 0;
        for (int i = 0; i < in.mobs().size(); i++) {
            if (alive.contains(i)) {
                continue;
            }
            StructBp.Mob m = in.mobs().get(i);
            BlockPos p = v.world(m.x(), m.y(), m.z());
            if (!level.isLoaded(p) || !level.areEntitiesLoaded(ChunkPos.asLong(p))) {
                continue;
            }
            final int idx = i;
            UUID id = StructPlacer.spawn(level, m, p, i, (k, mob) -> {
                mob.getPersistentData().putInt(TAG, v.id);
                mob.getPersistentData().putInt(TAG_I, idx);
            });
            if (id != null) {
                back++;
            }
        }
        // rejas cerradas y braseros apagados
        for (long k : in.gates()) {
            BlockPos p = v.world(StructBp.kx(k), StructBp.ky(k), StructBp.kz(k));
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof com.mundosdeaxelis.block.RejaVolcanBlock && s.getValue(com.mundosdeaxelis.block.RejaVolcanBlock.OPEN)) {
                level.setBlock(p, s.setValue(com.mundosdeaxelis.block.RejaVolcanBlock.OPEN, false), net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
        }
        for (long k : in.braziers()) {
            com.mundosdeaxelis.block.BraseroVolcanBlock.reset(level, v.world(StructBp.kx(k), StructBp.ky(k), StructBp.kz(k)));
        }
        // etapa 287: la reja de la sala de erupciones vuelve a cerrarse; su Altar Maldito queda listo para abrirla otra vez
        for (long k : in.altars()) {
            BlockPos p = v.world(StructBp.kx(k), StructBp.ky(k), StructBp.kz(k));
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof com.mundosdeaxelis.block.AltarMalditoBlockEntity be
                    && be.state() == com.mundosdeaxelis.block.AltarMalditoBlockEntity.RESTING) {
                be.recharge();
            }
        }
        boolean loot = forceLoot || now - v.lastLoot >= LOOT_EVERY;
        if (loot) {
            for (long k : in.chests()) {
                BlockPos p = v.world(StructBp.kx(k), StructBp.ky(k), StructBp.kz(k));
                if (level.getBlockEntity(p) instanceof com.mundosdeaxelis.block.VaultChestBlockEntity be) {
                    be.resetLoot();
                }
            }
            v.lastLoot = now;
        }
        setDirty();
        if (back > 0) {
            for (ServerPlayer p : level.players()) {
                if (p.blockPosition().distSqr(c) < FAR * FAR) {
                    p.displayClientMessage(Component.translatable(loot ? "message.mundos_de_axelis.volcan.reset_loot" : "message.mundos_de_axelis.volcan.reset"), true);
                }
            }
            level.playSound(null, c, com.mundosdeaxelis.registry.ModSounds.VOLCAN_RETUMBO.get(), net.minecraft.sounds.SoundSource.AMBIENT, 3.0F, 0.7F);
        }
        return back;
    }

    // ================================================================== comandos

    /**
     * Prueba de la etapa 287 sobre el volcán más cercano (recién construido): la reja de la sala de erupciones está cerrada, la abre
     * vencer las tres oleadas del Altar Maldito, al reiniciar el volcán se cierra y el altar queda listo; y la «primera vista»: quien
     * mira hacia el cráter (y no tiene nada delante) lo ve, de espaldas no, y solo despierta una vez por jugador.
     */
    /** Etapa 287: revisa en el plano la bajada al muelle, el cartel y la reja del altar (ver {@link DesignVolcan#revisarBajada}). */
    public static List<String> revisarBajada() {
        return DesignVolcan.revisarBajada(StructDesigns.make("volcan"));
    }

    public static List<String> prueba287(ServerLevel sl, net.minecraftforge.common.util.FakePlayer fp) {
        List<String> fails = new ArrayList<>();
        V v = nearest(sl, fp.blockPosition());
        if (v == null) {
            fails.add("no hay volcán");
            return fails;
        }
        BlockPos gate = v.world(DesignVolcan.GATE_ALTAR_X, DesignVolcan.F_ERUPT, DesignVolcan.GATE_ALTAR_Z);
        java.util.function.Supplier<Boolean> abierta = () -> {
            BlockState g = sl.getBlockState(gate);
            return g.getBlock() instanceof com.mundosdeaxelis.block.RejaVolcanBlock && g.getValue(com.mundosdeaxelis.block.RejaVolcanBlock.OPEN);
        };
        BlockState g0 = sl.getBlockState(gate);
        if (!(g0.getBlock() instanceof com.mundosdeaxelis.block.RejaVolcanBlock) || g0.getValue(com.mundosdeaxelis.block.RejaVolcanBlock.CHANNEL) != 3 || abierta.get()) {
            fails.add("la reja del altar no está cerrada en su sitio (" + g0 + ")");
        }
        int[] a = StructKit.altars("volcan")[0];
        BlockPos ap = v.world(a[0], a[1], a[2]);
        if (!(sl.getBlockEntity(ap) instanceof com.mundosdeaxelis.block.AltarMalditoBlockEntity be)) {
            fails.add("sin altar en la sala de erupciones");
            return fails;
        }
        fp.setGameMode(net.minecraft.world.level.GameType.CREATIVE); // sin sellos de por medio
        fp.moveTo(ap.getX() + 0.5, ap.getY(), ap.getZ() + 3.5);
        List<ServerPlayer> near = List.of(fp);
        be.recharge();
        be.activate(fp);
        for (int w = 1; w <= com.mundosdeaxelis.block.AltarMalditoBlockEntity.WAVES && fails.isEmpty(); w++) {
            for (int t = 0; t < 120 && be.wave() < w; t++) {
                be.tick(sl, near);
            }
            if (abierta.get()) {
                fails.add("la reja se abre antes de terminar (oleada " + w + ")");
            }
            for (UUID id : new ArrayList<>(be.mobs())) {
                net.minecraft.world.entity.Entity e = sl.getEntity(id);
                if (e != null) {
                    e.discard();
                }
            }
            be.tick(sl, near);
        }
        be.tick(sl, near);
        if (fails.isEmpty() && !abierta.get()) {
            fails.add("vencer las oleadas no abre la reja (estado " + be.state() + ")");
        }
        sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(ap).inflate(8)).forEach(net.minecraft.world.entity.Entity::discard);
        get(sl.getServer()).reset(sl, v, false);
        if (abierta.get()) {
            fails.add("al reiniciar el volcán la reja sigue abierta");
        }
        if (be.state() != com.mundosdeaxelis.block.AltarMalditoBlockEntity.IDLE) {
            fails.add("al reiniciar el volcán el altar no queda listo (" + be.state() + ")");
        }
        fp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        // la primera vista: flotando sobre la ladera, mirando al cráter
        BlockPos c = cima(v);
        BlockPos from = v.world(0, DesignVolcan.H + 8, DesignVolcan.CZ - 40);
        fp.moveTo(from.getX() + 0.5, from.getY(), from.getZ() + 0.5);
        net.minecraft.world.phys.Vec3 d = net.minecraft.world.phys.Vec3.atCenterOf(c).subtract(fp.getEyePosition());
        fp.setYRot((float) (Mth.atan2(d.z, d.x) * 180.0 / Math.PI) - 90.0F);
        fp.setXRot((float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180.0 / Math.PI));
        if (!loVe(fp, c)) {
            fails.add("mirando al cráter no lo ve");
        }
        fp.setYRot(fp.getYRot() + 180.0F);
        if (loVe(fp, c)) {
            fails.add("de espaldas lo ve");
        }
        fp.setYRot(fp.getYRot() - 180.0F);
        BlockPos wall = BlockPos.containing(fp.getEyePosition().add(d.normalize().scale(3)));
        BlockState was = sl.getBlockState(wall);
        sl.setBlock(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        if (loVe(fp, c)) {
            fails.add("con una piedra delante lo ve");
        }
        sl.setBlock(wall, was, 2);
        CompoundTag per = fp.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        per.remove(VISTOS);
        fp.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, per);
        if (visto(fp, v.id)) {
            fails.add("ya lo había visto");
        }
        marcarVisto(fp, v.id);
        if (!visto(fp, v.id)) {
            fails.add("no se apunta que lo vio");
        }
        DESPERTO.remove(v.id);
        despertar(sl, v, fp);
        if (!DESPERTO.containsKey(v.id)) {
            fails.add("no despierta");
        }
        per = fp.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        per.remove(VISTOS);
        fp.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, per);
        return fails;
    }

    private static V nearest(ServerLevel level, BlockPos p) {
        V best = null;
        double bd = Double.MAX_VALUE;
        for (V v : get(level.getServer()).list) {
            if (v.dim != level.dimension()) {
                continue;
            }
            double d = v.center().distSqr(p);
            if (d < bd) {
                bd = d;
                best = v;
            }
        }
        return best;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("volcan")
                .then(Commands.literal("cerca").executes(c -> near(c.getSource())))
                .then(Commands.literal("reiniciar").executes(c -> forceReset(c.getSource())))
                .then(Commands.literal("generar").executes(c -> NaturalGen.generate(c.getSource(), NaturalGen.VOLCAN, false))
                        .then(Commands.literal("aqui").executes(c -> NaturalGen.generate(c.getSource(), NaturalGen.VOLCAN, true))))
                .then(Commands.literal("natural").then(Commands.argument("activo", BoolArgumentType.bool())
                        .executes(c -> NaturalGen.setEnabled(c.getSource(), NaturalGen.VOLCAN, BoolArgumentType.getBool(c, "activo")))));
    }

    private static int near(CommandSourceStack src) {
        BlockPos p = BlockPos.containing(src.getPosition());
        Volcanes vs = get(src.getServer());
        NaturalGen g = NaturalGen.get(src.getServer());
        StringBuilder sb = new StringBuilder("Volcanes apuntados: " + vs.list.size() + (g.enabled(NaturalGen.VOLCAN) ? " (aparecen solos)" : " (no aparecen solos)"));
        V v = nearest(src.getLevel(), p);
        if (v != null) {
            BlockPos c = v.center();
            sb.append(". El más cercano: ").append(c.getX()).append(" ").append(v.origin.getY()).append(" ").append(c.getZ())
                    .append(" (a ").append((int) Math.sqrt(c.distSqr(p))).append(" bloques").append(v.natural ? ", natural" : ", de comando").append(")");
        }
        if (src.getLevel().dimension() == Level.OVERWORLD) {
            sb.append(". Esta región: ").append(g.describe(NaturalGen.VOLCAN, src.getLevel(), p));
        }
        String msg = sb.toString();
        src.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int forceReset(CommandSourceStack src) {
        V v = nearest(src.getLevel(), BlockPos.containing(src.getPosition()));
        if (v == null) {
            src.sendFailure(Component.literal("No hay ningún volcán en esta dimensión."));
            return 0;
        }
        int back = get(src.getServer()).reset(src.getLevel(), v, true);
        if (back < 0) {
            src.sendFailure(Component.literal("El plano del volcán aún se está preparando: prueba otra vez en unos segundos."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("PRUEBA_VOLCAN_REINICIO: volvieron " + back + " enemigos, rejas cerradas, braseros apagados y cofres rellenos."), true);
        return 1;
    }
}
