package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.plat.Nbt;
import com.coleccionista.Coleccionista;
import com.coleccionista.mapa.AlaCeldas;
import com.coleccionista.mapa.Plano;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * El mapa del evento en el overworld: dónde se construyó (el origen) y las marcas de los planos (celdas, pantallas, puertas...). La
 * construcción se hace por partes (unos miles de bloques por tick) para que el servidor no se trabe.
 */
public final class Complejo {
    private static final String MOD = Coleccionista.MODID;
    private static final int POR_TICK = 6000;
    private static final long SEMILLA = 1234L;
    private static Plano celdas;
    private static final Map<String, BlockState> CACHE = new HashMap<>();
    private static final Deque<Trabajo> COLA = new ArrayDeque<>();

    private record Trabajo(ServerLevel level, BlockPos origen, List<long[]> pasos, Plano plano, int[] hecho, Runnable fin) {
    }

    private Complejo() {
    }

    public static void init() {
        AxelisCore.onServerTick(Complejo::tick);
    }

    /** El plano del Ala de celdas (siempre el mismo: la semilla es fija). */
    public static Plano planoCeldas() {
        if (celdas == null) {
            celdas = AlaCeldas.crear(SEMILLA);
        }
        return celdas;
    }

    private static CompoundTag datos(MinecraftServer s) {
        CompoundTag g = WorldStore.get(s, MOD).global();
        if (!g.contains("complejo")) {
            g.put("complejo", new CompoundTag());
        }
        return g.getCompound("complejo");
    }

    public static boolean construido(MinecraftServer s) {
        return datos(s).getBoolean("celdas");
    }

    public static BlockPos origen(MinecraftServer s) {
        CompoundTag d = datos(s);
        return new BlockPos(Nbt.i(d, "ox"), Nbt.i(d, "oy"), Nbt.i(d, "oz"));
    }

    /** Una marca del Ala de celdas en el mundo (null si no existe o no se construyó). */
    public static BlockPos pos(MinecraftServer s, String marca) {
        Plano.Marca m = planoCeldas().marcas.get(marca);
        if (m == null || !construido(s)) {
            return null;
        }
        return origen(s).offset(m.x(), m.y(), m.z());
    }

    public static float yaw(String marca) {
        Plano.Marca m = planoCeldas().marcas.get(marca);
        return m == null ? 0 : m.yaw();
    }

    // ------------------------------------------------------------------ construir

    /** Construye el Ala de celdas con su piso en {@code origen} (la esquina oeste). Llama a {@code fin} al terminar. */
    public static void construir(ServerLevel level, BlockPos origen, Runnable fin) {
        Plano p = planoCeldas();
        List<long[]> pasos = new ArrayList<>();
        // primero se vacía la caja (con un margen) y luego se ponen los bloques
        for (int x = p.x0 - 1; x <= p.x1 + 1; x++) {
            for (int z = p.z0 - 1; z <= p.z1 + 1; z++) {
                for (int y = p.y0; y <= p.y1 + 3; y++) {
                    pasos.add(new long[] {x, y, z, 0});
                }
            }
        }
        for (long k : p.m.keySet()) {
            pasos.add(new long[] {Plano.kx(k), Plano.ky(k), Plano.kz(k), 1});
        }
        CompoundTag d = datos(level.getServer());
        d.putInt("ox", origen.getX());
        d.putInt("oy", origen.getY());
        d.putInt("oz", origen.getZ());
        d.putBoolean("celdas", false);
        WorldStore.get(level.getServer(), MOD).dirty();
        COLA.add(new Trabajo(level, origen, pasos, p, new int[] {0}, () -> {
            d.putBoolean("celdas", true);
            WorldStore.get(level.getServer(), MOD).dirty();
            Coleccionista.LOGGER.info("COLECCIONISTA_CONSTRUIDO celdas bloques={}", p.m.size());
            fin.run();
        }));
    }

    private static void tick(MinecraftServer s) {
        Trabajo t = COLA.peek();
        if (t == null) {
            return;
        }
        int n = 0;
        List<long[]> pasos = t.pasos;
        while (t.hecho[0] < pasos.size() && n < POR_TICK) {
            long[] q = pasos.get(t.hecho[0]++);
            BlockPos p = t.origen.offset((int) q[0], (int) q[1], (int) q[2]);
            if (q[3] == 0) {
                // vaciar sin dejar caer nada (sin actualizar vecinos)
                BlockEntity be = t.level.getBlockEntity(p);
                if (be != null) {
                    Clearable.tryClear(be);
                }
                t.level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
            } else {
                long k = Plano.key((int) q[0], (int) q[1], (int) q[2]);
                BlockState st = estado(t.plano.m.get(k));
                if (st != null) {
                    t.level.setBlock(p, st, 2 | 16);
                    String nbt = t.plano.nbt.get(k);
                    if (nbt != null) {
                        datosBloque(t.level, p, nbt);
                    }
                }
            }
            n++;
        }
        if (t.hecho[0] >= pasos.size()) {
            COLA.poll();
            t.fin.run();
        }
    }

    private static void datosBloque(Level l, BlockPos p, String snbt) {
        BlockEntity be = l.getBlockEntity(p);
        if (be == null) {
            return;
        }
        try {
            CompoundTag t = be.saveWithoutMetadata();
            t.merge(TagParser.parseTag(snbt));
            be.load(t);
            be.setChanged();
            l.sendBlockUpdated(p, be.getBlockState(), be.getBlockState(), 3);
        } catch (Exception e) {
            Coleccionista.LOGGER.warn("datos de bloque mal escritos en {}: {}", p, e.getMessage());
        }
    }

    /** {@code oak_stairs[facing=north]} → el bloque (null si no existe). */
    public static BlockState estado(String spec) {
        if (spec == null) {
            return null;
        }
        return CACHE.computeIfAbsent(spec, k -> {
            String full = k.indexOf(':') >= 0 && k.indexOf(':') < (k.indexOf('[') < 0 ? k.length() : k.indexOf('[')) ? k : "minecraft:" + k;
            try {
                return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), full, false).blockState();
            } catch (Exception e) {
                Coleccionista.LOGGER.warn("bloque desconocido en el plano: {}", k);
                return Blocks.AIR.defaultBlockState();
            }
        });
    }

    public static boolean construyendo() {
        return !COLA.isEmpty();
    }

    /** Para el mensaje de avance. */
    public static Component avance() {
        Trabajo t = COLA.peek();
        if (t == null) {
            return Component.empty();
        }
        return Component.translatable(MOD + ".cmd.construyendo", (int) (100L * t.hecho[0] / Math.max(1, t.pasos.size())));
    }
}
