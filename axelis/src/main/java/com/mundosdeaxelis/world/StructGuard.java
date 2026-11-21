package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Estructuras que no se pueden picar (etapa 256): cada estructura del mod que se construye (con el comando o sola en el mundo) se
 * apunta aquí, y nadie en supervivencia puede romper sus bloques (los del plano: paredes, suelos, adornos, la Piedra de Portal...);
 * el terreno de alrededor sí. Las explosiones tampoco los rompen. En creativo se puede todo, para poder editar y probar. Las
 * dungeons y arenas (sus dimensiones) ya estaban protegidas por {@link com.mundosdeaxelis.event.HubProtection}.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class StructGuard extends SavedData {
    private static final String NAME = "mundos_estructuras_protegidas";

    record Placed(int id, String design, ResourceKey<Level> dim, BlockPos origin, Rotation rot, BoundingBox box) {
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

    /** Forma de un diseño: posiciones del plano con bloque (no aire) y sus límites. */
    record Shape(LongOpenHashSet solid, int x0, int y0, int z0, int x1, int y1, int z1) {
        static Shape of(StructBp bp) {
            LongOpenHashSet s = new LongOpenHashSet();
            int ax = Integer.MAX_VALUE, ay = ax, az = ax, bx = Integer.MIN_VALUE, by = bx, bz = bx;
            for (Map.Entry<Long, String> e : bp.m.entrySet()) {
                if (e.getValue().equals("air") || e.getValue().equals("cave_air")) {
                    continue;
                }
                long k = e.getKey();
                s.add(k);
                int x = StructBp.kx(k), y = StructBp.ky(k), z = StructBp.kz(k);
                ax = Math.min(ax, x);
                ay = Math.min(ay, y);
                az = Math.min(az, z);
                bx = Math.max(bx, x);
                by = Math.max(by, y);
                bz = Math.max(bz, z);
            }
            s.trim();
            return new Shape(s, ax, ay, az, bx, by, bz);
        }
    }

    private static final Map<String, Shape> SHAPES = new HashMap<>();
    private static final Map<String, CompletableFuture<Shape>> JOBS = new HashMap<>();

    /** Cuando ya se tiene el plano a mano (al construir), su forma queda lista sin esperar. */
    static void prime(String design, StructBp bp) {
        synchronized (SHAPES) {
            SHAPES.computeIfAbsent(design, d -> Shape.of(bp));
        }
    }

    /** La forma de un diseño, si ya está (si no, se empieza a calcular aparte y mientras tanto se protege toda la caja). */
    private static Shape shape(String design) {
        synchronized (SHAPES) {
            Shape s = SHAPES.get(design);
            if (s != null) {
                return s;
            }
            CompletableFuture<Shape> f = JOBS.computeIfAbsent(design, d -> CompletableFuture.supplyAsync(() -> Shape.of(StructDesigns.make(d))));
            if (f.isDone()) {
                JOBS.remove(design);
                try {
                    s = f.join();
                    SHAPES.put(design, s);
                    return s;
                } catch (Exception ex) {
                    MundosDeAxelis.LOGGER.error("No se pudo preparar la forma de la estructura {}", design, ex);
                }
            }
            return null;
        }
    }

    private final List<Placed> list = new ArrayList<>();
    private int nextId = 1;

    public StructGuard() {
    }

    public static StructGuard get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(StructGuard::load, StructGuard::new, NAME);
    }

    // ================================================================== alta y baja

    /** Apunta una estructura recién construida. Devuelve su número (para quitarla al deshacer). */
    public static int add(ServerLevel level, String design, BlockPos origin, Rotation rot, StructBp bp) {
        prime(design, bp);
        Shape sh = shape(design);
        StructGuard g = get(level.getServer());
        BlockPos a = StructPlacer.world(origin, rot, sh.x0(), sh.y0(), sh.z0());
        BlockPos b = StructPlacer.world(origin, rot, sh.x1(), sh.y1(), sh.z1());
        Placed p = new Placed(g.nextId++, design, level.dimension(), origin.immutable(), rot, BoundingBox.fromCorners(a, b));
        g.list.add(p);
        g.setDirty();
        return p.id();
    }

    public static void remove(MinecraftServer server, int id) {
        StructGuard g = get(server);
        if (g.list.removeIf(p -> p.id() == id)) {
            g.setDirty();
        }
    }

    /** Centros de las estructuras de un diseño ya construidas en esa dimensión (para las pistas y brújulas de los habitantes). */
    public static List<BlockPos> centers(MinecraftServer server, String design, ResourceKey<Level> dim) {
        List<BlockPos> out = new ArrayList<>();
        for (Placed p : get(server).list) {
            if (p.design().equals(design) && p.dim() == dim) {
                out.add(p.box().getCenter());
            }
        }
        return out;
    }

    /** ¿Hay alguna estructura del mod a menos de {@code dist} bloques (en horizontal) de {@code pos}? (etapa 262: que no se pisen) */
    public static boolean near(ServerLevel level, BlockPos pos, int dist) {
        for (Placed p : get(level.getServer()).list) {
            if (p.dim() != level.dimension()) {
                continue;
            }
            BoundingBox bb = p.box();
            int dx = Math.max(0, Math.max(bb.minX() - pos.getX(), pos.getX() - bb.maxX()));
            int dz = Math.max(0, Math.max(bb.minZ() - pos.getZ(), pos.getZ() - bb.maxZ()));
            if (dx * dx + dz * dz < dist * dist) {
                return true;
            }
        }
        return false;
    }

    /**
     * Etapa 269: el tier más alto de las estructuras del mod cuya caja contiene ese punto (0 = ninguna, o una casa de habitante).
     * Sirve para sellar los cofres y barriles y para frenar a quien aún no venció al jefe anterior.
     */
    public static int tierAt(ServerLevel level, BlockPos pos) {
        int best = 0;
        for (Placed p : get(level.getServer()).list) {
            if (p.dim() == level.dimension() && p.box().isInside(pos)) {
                best = Math.max(best, Sellos.designTier(p.design()));
            }
        }
        return best;
    }

    /** ¿Ese bloque es parte de una estructura protegida? */
    public static boolean protectedAt(ServerLevel level, BlockPos pos) {
        StructGuard g = get(level.getServer());
        for (Placed p : g.list) {
            if (p.dim() != level.dimension() || !p.box().isInside(pos)) {
                continue;
            }
            Shape s = shape(p.design());
            if (s == null) {
                return true; // aún se calcula la forma: mientras tanto, toda la caja
            }
            BlockPos l = p.local(pos);
            if (s.solid().contains(StructBp.key(l.getX(), l.getY(), l.getZ()))) {
                return true;
            }
        }
        return false;
    }

    // ================================================================== eventos

    private static final Map<UUID, Long> WARNED = new HashMap<>();

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        Player pl = e.getPlayer();
        if (pl == null || pl.isCreative() || !(e.getLevel() instanceof ServerLevel sl)) {
            return;
        }
        if (protectedAt(sl, e.getPos())) {
            e.setCanceled(true);
            long now = sl.getGameTime();
            Long last = WARNED.get(pl.getUUID());
            if (last == null || now - last > 40) {
                WARNED.put(pl.getUUID(), now);
                pl.displayClientMessage(Component.translatable("message.mundos_de_axelis.estructura.protegida"), true);
            }
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate e) {
        if (e.getLevel() instanceof ServerLevel sl && !get(sl.getServer()).list.isEmpty()) {
            e.getAffectedBlocks().removeIf(p -> protectedAt(sl, p));
        }
    }

    /** Al cerrar el servidor se olvidan los avisos y las formas calculadas (se recalculan en el siguiente). */
    public static void clearRuntime() {
        WARNED.clear();
        synchronized (SHAPES) {
            SHAPES.clear();
            JOBS.clear();
        }
    }

    // ================================================================== guardar

    public static StructGuard load(CompoundTag tag) {
        StructGuard g = new StructGuard();
        g.nextId = Math.max(1, tag.getInt("NextId"));
        for (Tag t : tag.getList("Placed", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            int[] bb = c.getIntArray("Box");
            if (bb.length != 6) {
                continue;
            }
            g.list.add(new Placed(c.getInt("Id"), c.getString("Design"),
                    ResourceKey.create(Registries.DIMENSION, new ResourceLocation(c.getString("Dim"))), BlockPos.of(c.getLong("Origin")),
                    Rotation.values()[Mth.clamp(c.getInt("Rot"), 0, 3)], new BoundingBox(bb[0], bb[1], bb[2], bb[3], bb[4], bb[5])));
        }
        return g;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("NextId", nextId);
        ListTag l = new ListTag();
        for (Placed p : list) {
            CompoundTag c = new CompoundTag();
            c.putInt("Id", p.id());
            c.putString("Design", p.design());
            c.putString("Dim", p.dim().location().toString());
            c.putLong("Origin", p.origin().asLong());
            c.putInt("Rot", p.rot().ordinal());
            BoundingBox b = p.box();
            c.putIntArray("Box", new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()});
            l.add(c);
        }
        tag.put("Placed", l);
        return tag;
    }
}
