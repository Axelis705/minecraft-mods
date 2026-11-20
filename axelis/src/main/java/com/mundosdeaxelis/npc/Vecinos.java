package com.mundosdeaxelis.npc;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Habitante;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Quién vive dónde (etapa 257), guardado con el mundo: por cada habitante, si sigue preso, si ya está libre (sin casa) o si es
 * vecino (con su casa), qué entidad es la de verdad y dónde está su casa. Hay uno de cada tipo por mundo: si lo rescatas en un
 * santuario, en los demás la celda queda vacía. Si muere (solo con /kill o cayendo al vacío) vuelve a aparecer en su sitio.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Vecinos extends SavedData {
    private static final String NAME = "mundos_vecinos";

    public static final class Entry {
        public int state = Habitante.PRESO;
        /** La entidad que vale (null = se perdió; vuelve a aparecer en {@link #spot}). */
        public UUID uuid;
        public ResourceKey<Level> dim = Level.OVERWORLD;
        /** Dónde se queda (libre: fuera de la celda; vecino: dentro de su casa). */
        public BlockPos spot;
        /** Casa (solo vecino): origen y giro del plano, para desarmarla al mudarse. */
        public BlockPos house;
        public Rotation rot = Rotation.NONE;
    }

    private final Map<String, Entry> map = new HashMap<>();

    public Vecinos() {
    }

    public static Vecinos get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Vecinos::load, Vecinos::new, NAME);
    }

    public static Entry entry(MinecraftServer server, String kind) {
        return get(server).map.get(kind);
    }

    public static int state(MinecraftServer server, String kind) {
        Entry e = entry(server, kind);
        return e == null ? Habitante.PRESO : e.state;
    }

    /** Cuántos habitantes viven ya en su casa (desbloquean cosas de la tienda). */
    public static int residents(MinecraftServer server) {
        int n = 0;
        for (Entry e : get(server).map.values()) {
            if (e.state == Habitante.VECINO) {
                n++;
            }
        }
        return n;
    }

    public static void set(MinecraftServer server, String kind, int state, UUID uuid, ResourceKey<Level> dim, BlockPos spot, BlockPos house, Rotation rot) {
        Vecinos v = get(server);
        Entry e = v.map.computeIfAbsent(kind, k -> new Entry());
        e.state = state;
        e.uuid = uuid;
        e.dim = dim;
        e.spot = spot == null ? null : spot.immutable();
        e.house = house == null ? null : house.immutable();
        e.rot = rot == null ? Rotation.NONE : rot;
        v.setDirty();
    }

    /** La entidad de ese habitante murió: aparecerá otra en su sitio. */
    public static void lost(MinecraftServer server, String kind, UUID uuid) {
        Vecinos v = get(server);
        Entry e = v.map.get(kind);
        if (e != null && uuid.equals(e.uuid)) {
            e.uuid = null;
            v.setDirty();
        }
    }

    /** La entidad viva de ese habitante, si está cargada. */
    public static Habitante find(MinecraftServer server, String kind) {
        Entry e = entry(server, kind);
        if (e == null || e.uuid == null) {
            return null;
        }
        ServerLevel l = server.getLevel(e.dim);
        Entity ent = l == null ? null : l.getEntity(e.uuid);
        return ent instanceof Habitante h && h.isAlive() ? h : null;
    }

    // ================================================================== reaparecer si se perdió

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || ev.getServer().getTickCount() % 200 != 37) {
            return;
        }
        MinecraftServer server = ev.getServer();
        Vecinos v = get(server);
        for (Map.Entry<String, Entry> me : v.map.entrySet()) {
            Entry e = me.getValue();
            if (e.state == Habitante.PRESO || e.uuid != null || e.spot == null) {
                continue;
            }
            ServerLevel l = server.getLevel(e.dim);
            if (l == null || !l.isLoaded(e.spot) || !l.shouldTickBlocksAt(e.spot)) {
                continue;
            }
            Habitante h = Habitante.create(l, me.getKey(), e.state, e.spot);
            if (h != null) {
                e.uuid = h.getUUID();
                v.setDirty();
                NpcService.poof(l, h.position(), true);
            }
        }
    }

    // ================================================================== comandos (pruebas)

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("vecinos")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("estado").executes(c -> {
                    MinecraftServer server = c.getSource().getServer();
                    StringBuilder sb = new StringBuilder("Vecinos: ");
                    for (NpcKinds.Kind k : NpcKinds.ALL.values()) {
                        Entry e = entry(server, k.id());
                        sb.append(k.id()).append(" = ");
                        if (e == null || e.state == Habitante.PRESO) {
                            sb.append("preso");
                        } else {
                            sb.append(e.state == Habitante.LIBRE ? "libre" : "vecino");
                            if (e.spot != null) {
                                sb.append(" en ").append(e.spot.getX()).append(' ').append(e.spot.getY()).append(' ').append(e.spot.getZ());
                            }
                            sb.append(e.uuid == null ? " (reaparecerá)" : "");
                        }
                        sb.append(". ");
                    }
                    String msg = sb.toString().trim();
                    c.getSource().sendSuccess(() -> Component.literal(msg), false);
                    return 1;
                }))
                // pruebas: rescatar al preso más cercano, plantar la casa aquí (mirando a donde mira el comando) y mudarse
                .then(Commands.literal("liberar").executes(c -> {
                    ServerLevel l = c.getSource().getLevel();
                    Habitante h = NpcService.prisonerNear(l, BlockPos.containing(c.getSource().getPosition()), 64);
                    if (h == null) {
                        c.getSource().sendFailure(Component.literal("No hay ningún preso cerca."));
                        return 0;
                    }
                    NpcService.free(c.getSource().getPlayer(), h, null, NpcService.R_RESCUED);
                    c.getSource().sendSuccess(() -> Component.literal("VECINOS_LIBERADO " + h.kindId()), true);
                    return 1;
                }))
                .then(Commands.literal("plantar").executes(c -> {
                    ServerLevel l = c.getSource().getLevel();
                    net.minecraft.core.Direction facing = net.minecraft.core.Direction.fromYRot(c.getSource().getRotation().y);
                    String kind = NpcKinds.RUDA.id();
                    String err = NpcService.plantHouse(l, null, kind, BlockPos.containing(c.getSource().getPosition()), facing);
                    if (err != null) {
                        c.getSource().sendFailure(Component.translatable(err));
                        return 0;
                    }
                    c.getSource().sendSuccess(() -> Component.literal("VECINOS_CASA_OK " + kind), true);
                    return 1;
                }))
                .then(Commands.literal("mudar").executes(c -> {
                    Habitante h = find(c.getSource().getServer(), NpcKinds.RUDA.id());
                    if (h == null || !NpcService.moveOut(null, h)) {
                        c.getSource().sendFailure(Component.literal("No hay ningún vecino con casa cargado."));
                        return 0;
                    }
                    c.getSource().sendSuccess(() -> Component.literal("VECINOS_MUDADO " + h.kindId()), true);
                    return 1;
                }))
                .then(Commands.literal("reiniciar").executes(c -> {
                    MinecraftServer server = c.getSource().getServer();
                    int n = 0;
                    for (ServerLevel l : server.getAllLevels()) {
                        for (Entity ent : l.getAllEntities()) {
                            if (ent instanceof Habitante h && h.state() != Habitante.PRESO) {
                                h.discard();
                                n++;
                            }
                        }
                    }
                    Vecinos v = get(server);
                    v.map.clear();
                    v.setDirty();
                    int removed = n;
                    c.getSource().sendSuccess(() -> Component.literal("Vecinos reiniciados (todos vuelven a estar presos; " + removed
                            + " quitados). Las celdas de los santuarios nuevos vuelven a tener preso."), true);
                    return 1;
                }));
    }

    // ================================================================== guardar

    public static Vecinos load(CompoundTag tag) {
        Vecinos v = new Vecinos();
        CompoundTag all = tag.getCompound("Npcs");
        for (String k : all.getAllKeys()) {
            CompoundTag c = all.getCompound(k);
            Entry e = new Entry();
            e.state = Mth.clamp(c.getInt("State"), 0, 2);
            e.uuid = c.hasUUID("Uuid") ? c.getUUID("Uuid") : null;
            e.dim = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(c.getString("Dim").isEmpty() ? "minecraft:overworld" : c.getString("Dim")));
            e.spot = c.contains("Spot") ? BlockPos.of(c.getLong("Spot")) : null;
            e.house = c.contains("House") ? BlockPos.of(c.getLong("House")) : null;
            e.rot = Rotation.values()[Mth.clamp(c.getInt("Rot"), 0, 3)];
            v.map.put(k, e);
        }
        return v;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag all = new CompoundTag();
        for (Map.Entry<String, Entry> me : map.entrySet()) {
            Entry e = me.getValue();
            CompoundTag c = new CompoundTag();
            c.putInt("State", e.state);
            if (e.uuid != null) {
                c.putUUID("Uuid", e.uuid);
            }
            c.putString("Dim", e.dim.location().toString());
            if (e.spot != null) {
                c.putLong("Spot", e.spot.asLong());
            }
            if (e.house != null) {
                c.putLong("House", e.house.asLong());
            }
            c.putInt("Rot", e.rot.ordinal());
            all.put(me.getKey(), c);
        }
        tag.put("Npcs", all);
        return tag;
    }
}
