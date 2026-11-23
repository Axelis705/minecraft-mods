package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.plat.Nbt;
import com.coleccionista.Coleccionista;
import com.coleccionista.mapa.AlaCeldas;
import com.coleccionista.mapa.Cloacas;
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
 * El mapa del evento en el overworld: dónde se construyó (el origen) y las marcas de los planos (celdas, pantallas, puertas...). El
 * mapa tiene varias zonas, cada una con su plano y su lugar respecto al origen: el Ala de celdas (en el origen) y las Cloacas de
 * "Encadenados" (al este, pegadas al portón de salida del Ala). La construcción se hace por partes (unos miles de bloques por tick)
 * para que el servidor no se trabe.
 */
public final class Complejo {
    private static final String MOD = Coleccionista.MODID;
    private static final int POR_TICK = 6000;
    private static final long SEMILLA = 1234L;
    private static Plano celdas;
    private static List<Zona> zonas;
    private static final Map<String, BlockState> CACHE = new HashMap<>();
    private static final Deque<Trabajo> COLA = new ArrayDeque<>();

    private record Trabajo(ServerLevel level, BlockPos origen, List<long[]> pasos, Plano plano, int[] hecho, Runnable fin) {
    }

    /**
     * Una zona del mapa: su plano y dónde queda respecto al origen. {@code margen}: cuánto se limpia alrededor antes de construir (0 si
     * está pegada a otra zona, para no borrarle la pared).
     */
    public record Zona(String id, Plano plano, int dx, int dy, int dz, int margen) {
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

    /** Las zonas del mapa, en el orden en que se construyen. */
    public static List<Zona> zonas() {
        if (zonas == null) {
            List<Zona> l = new ArrayList<>();
            l.add(new Zona("celdas", planoCeldas(), 0, 0, 0, 1));
            // las Cloacas empiezan justo al este del portón del Ala (x 44): su pasillo (x local -5) queda en x 45
            l.add(new Zona("cloacas", Cloacas.crear(SEMILLA), 50, 0, -11, 0));
            // el Salón de las mesas, pegado al este de las Cloacas (su pared oeste junto al Refugio)
            l.add(new Zona("salon", com.coleccionista.mapa.Salon.crear(SEMILLA), 163, 0, -10, 0));
            // la Sala de las puertas, al norte del Ala de celdas (elevada: su piso queda 6 bloques arriba)
            l.add(new Zona("puertas", com.coleccionista.mapa.Puertas.crear(SEMILLA), 2, 0, -50, 0));
            // la Cámara final (Las copas), al norte de la Sala de las puertas
            l.add(new Zona("final", com.coleccionista.mapa.Final.crear(SEMILLA), 7, 0, -84, 0));
            // la Capilla (El sacrificio), al sur del Ala de celdas
            l.add(new Zona("capilla", com.coleccionista.mapa.Capilla.crear(SEMILLA), -10, 0, 40, 0));
            // el Archivo (El traidor), al sur de la Capilla
            l.add(new Zona("archivo", com.coleccionista.mapa.Archivo.crear(SEMILLA), -10, 0, 66, 0));
            // los Laberintos (A ciegas), al oeste del Ala de celdas
            l.add(new Zona("laberintos", com.coleccionista.mapa.Laberintos.crear(SEMILLA), -96, 0, 0, 0));
            zonas = l;
        }
        return zonas;
    }

    public static Zona zona(String id) {
        for (Zona z : zonas()) {
            if (z.id().equals(id)) {
                return z;
            }
        }
        return null;
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

    /** ¿Ya se construyó esa zona? */
    public static boolean construido(MinecraftServer s, String zona) {
        return datos(s).getBoolean(zona);
    }

    /** Dónde empieza una zona en el mundo (null si no se construyó). */
    public static BlockPos origen(MinecraftServer s, String zona) {
        Zona z = zona(zona);
        if (z == null || !construido(s, zona)) {
            return null;
        }
        return origen(s).offset(z.dx(), z.dy(), z.dz());
    }

    public static BlockPos origen(MinecraftServer s) {
        CompoundTag d = datos(s);
        return new BlockPos(Nbt.i(d, "ox"), Nbt.i(d, "oy"), Nbt.i(d, "oz"));
    }

    /** Una marca de cualquier zona en el mundo (null si no existe o esa zona no se construyó). */
    public static BlockPos pos(MinecraftServer s, String marca) {
        for (Zona z : zonas()) {
            Plano.Marca m = z.plano().marcas.get(marca);
            if (m != null) {
                if (!construido(s, z.id())) {
                    return null;
                }
                return origen(s).offset(z.dx() + m.x(), z.dy() + m.y(), z.dz() + m.z());
            }
        }
        return null;
    }

    public static float yaw(String marca) {
        for (Zona z : zonas()) {
            Plano.Marca m = z.plano().marcas.get(marca);
            if (m != null) {
                return m.yaw();
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ construir

    /**
     * Construye todo el mapa con el piso del Ala de celdas en {@code origen} (la esquina oeste). Llama a {@code fin} al terminar.
     */
    public static void construir(ServerLevel level, BlockPos origen, Runnable fin) {
        CompoundTag d = datos(level.getServer());
        d.putInt("ox", origen.getX());
        d.putInt("oy", origen.getY());
        d.putInt("oz", origen.getZ());
        List<Zona> zs = zonas();
        for (int i = 0; i < zs.size(); i++) {
            Zona z = zs.get(i);
            d.putBoolean(z.id(), false);
            boolean ultima = i == zs.size() - 1;
            encolar(level, origen.offset(z.dx(), z.dy(), z.dz()), z, ultima ? fin : null);
        }
        WorldStore.get(level.getServer(), MOD).dirty();
    }

    /** Vuelve a construir una sola zona (con el origen que ya tiene el mapa). */
    public static boolean reconstruir(ServerLevel level, String id, Runnable fin) {
        Zona z = zona(id);
        if (z == null || !construido(level.getServer())) {
            return false;
        }
        datos(level.getServer()).putBoolean(id, false);
        encolar(level, origen(level.getServer()).offset(z.dx(), z.dy(), z.dz()), z, fin);
        return true;
    }

    private static void encolar(ServerLevel level, BlockPos base, Zona z, Runnable fin) {
        Plano p = z.plano();
        int mg = z.margen();
        List<long[]> pasos = new ArrayList<>();
        // primero se vacía la caja (con su margen) y luego se ponen los bloques
        for (int x = p.x0 - mg; x <= p.x1 + mg; x++) {
            for (int zz = p.z0 - mg; zz <= p.z1 + mg; zz++) {
                for (int y = p.y0; y <= p.y1 + 3; y++) {
                    pasos.add(new long[] {x, y, zz, 0});
                }
            }
        }
        for (long k : p.m.keySet()) {
            pasos.add(new long[] {Plano.kx(k), Plano.ky(k), Plano.kz(k), 1});
        }
        COLA.add(new Trabajo(level, base, pasos, p, new int[] {0}, () -> {
            datos(level.getServer()).putBoolean(z.id(), true);
            WorldStore.get(level.getServer(), MOD).dirty();
            Coleccionista.LOGGER.info("COLECCIONISTA_CONSTRUIDO {} bloques={}", z.id(), p.m.size());
            if (fin != null) {
                fin.run();
            }
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
        // el avance de todo lo que falta (todas las zonas en la cola)
        long hecho = 0, total = 0;
        for (Trabajo w : COLA) {
            hecho += w.hecho[0];
            total += w.pasos.size();
        }
        return Component.translatable(MOD + ".cmd.construyendo", (int) (100L * hecho / Math.max(1, total)));
    }
}
