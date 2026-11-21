package com.mundosdeaxelis.world;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Estructuras de fondo para grabar videos, hechas solo con comandos (como WorldEdit):
 * {@code /mundos estructura <templo|santuario|fundicion|fragua|ciudadela|fortin|fortaleza|torre|volcan>} la construye delante de quien escribe, mirando hacia él;
 * {@code /mundos estructura deshacer [veces]} la quita y deja el terreno como estaba (bloques, entidades de bloque y todo).
 * Los bloques propios del mod (decoraciones) se usan si existen; si no, simplemente se omiten.
 */
public final class Estructuras {
    private static final String MOD = "mundos_de_axelis";
    private static final int MAX_UNDO = 12;
    /**
     * Solo avisa a los jugadores: sin avisar a los vecinos (16) y sin soltar objetos (32). Con solo "2", cada bloque que se coloca
     * revisa a sus vecinos y las antorchas, farolas, alfombras o trampillas que todavía no tenían su apoyo se rompían y dejaban
     * objetos tirados por todo el suelo.
     */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final Map<UUID, Deque<Undo>> UNDO = new HashMap<>();

    /** Al cerrar el servidor (o abrir otro mundo en el mismo juego) se olvida lo que quedó de la sesión anterior. */
    public static void clearRuntime() {
        UNDO.clear();
    }

    private Estructuras() {
    }

    enum Kind {
        /** Etapa 262: la estructura grande del bosque, con la Piedra de Portal: Naturaleza al final del recorrido. */
        TEMPLO("templo", "Gran Templo del Bosque", "bosque"),
        /** La estructura chica del bosque (etapa 256): termina en la celda de Ruda. */
        SANTUARIO("santuario", "Santuario del Bosque", "santuario"),
        /** Etapa 266: la estructura grande de la forja: la Piedra de Portal en la isla de los hornos y la celda de Hilda. */
        FUNDICION("fundicion", "Gran Fundición", "forja"),
        /** La estructura chica de la forja (etapa 259): termina en el tesoro del altar. */
        FRAGUA("fragua", "Fragua Olvidada", "fragua"),
        /** Etapa 264: la estructura grande del hielo, con la Piedra de Portal: Hielo en la cripta, al final de las mazmorras. */
        CIUDADELA("ciudadela", "Ciudadela de cristal", "hielo"),
        /** La estructura chica del hielo (etapa 258): termina en la celda de Bruno. */
        FORTIN("fortin", "Fortín de Escarcha", "fortin"),
        /** Etapa 265: la estructura grande de la magia: la Piedra de Portal: Torre arriba del todo y la celda de Teo en la cripta. */
        FORTALEZA("fortaleza", "Fortaleza del Archimago", "torre"),
        /** La estructura chica de la magia (etapa 261): termina en el tesoro del observatorio. */
        TORRE("torre", "Torre Arcana", "torre_arcana"),
        VOLCAN("volcan", "El Volcán", "volcan");

        final String id;
        final String title;
        /** Nombre del diseño en {@link StructDesigns#make}. */
        final String design;

        Kind(String id, String title, String design) {
            this.id = id;
            this.title = title;
            this.design = design;
        }
    }

    // ------------------------------------------------------------------ comando

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("estructura");
        for (Kind k : Kind.values()) {
            root.then(Commands.literal(k.id).executes(c -> place(c.getSource(), k)));
        }
        root.then(Commands.literal("deshacer").executes(c -> undo(c.getSource(), 1))
                .then(Commands.argument("veces", IntegerArgumentType.integer(1, MAX_UNDO)).executes(c -> undo(c.getSource(), IntegerArgumentType.getInteger(c, "veces")))));
        return root;
    }

    private static UUID key(CommandSourceStack src) {
        return src.getEntity() == null ? new UUID(0, 0) : src.getEntity().getUUID();
    }

    private static final class Undo {
        final ResourceKey<Level> dim;
        final String title;
        final Map<BlockPos, StructPlacer.Snap> snaps = new LinkedHashMap<>();
        /** Enemigos puestos con la estructura (al deshacer se quitan también). */
        final List<UUID> mobs = new ArrayList<>();
        /** Volcán apuntado en el registro (para quitarlo al deshacer); -1 si no es un volcán. */
        int volcano = -1;
        /** Número en {@link StructGuard} (la protección contra picar); -1 si no hay. */
        int guard = -1;

        Undo(ResourceKey<Level> dim, String title) {
            this.dim = dim;
            this.title = title;
        }
    }

    private static int place(CommandSourceStack src, Kind kind) {
        ServerLevel level = src.getLevel();
        Vec2 rot = src.getRotation();
        Direction fwd = Direction.fromYRot(rot.y);
        BlockPos feet = BlockPos.containing(src.getPosition());
        BlockPos origin = feet.relative(fwd, 16);
        StructBp bp = StructDesigns.make(kind.design);
        Rotation r = rotationFor(fwd);
        Undo undo = new Undo(level.dimension(), kind.title);
        StructPlacer placer = new StructPlacer(level, bp, origin, r, undo.snaps);
        if (kind == Kind.VOLCAN) {
            placer.onMob(Volcanes::tagMob);
            Volcanes.prime(bp);
        }
        placer.runAll();
        for (UUID id : placer.mobs) {
            if (id != null) {
                undo.mobs.add(id);
            }
        }
        if (kind == Kind.VOLCAN) {
            undo.volcano = Volcanes.register(level, origin, r, false, placer.mobs);
        }
        // nadie la puede picar en supervivencia (etapa 256)
        undo.guard = StructGuard.add(level, kind.design, origin, r, bp);
        Deque<Undo> dq = UNDO.computeIfAbsent(key(src), k -> new ArrayDeque<>());
        dq.push(undo);
        while (dq.size() > MAX_UNDO) {
            dq.removeLast();
        }
        int n = undo.snaps.size();
        int removed = placer.removed;
        int mobs = undo.mobs.size();
        // unos pocos pueden quedar por culpa del terreno de prueba (un mundo plano donde la estructura queda en el aire): se toleran 3
        String extra = removed > 3 ? "SIN APOYO " + removed : "apoyo ok";
        String foes = mobs > 0 ? ", " + mobs + " enemigos" : "";
        String trees = placer.treesCut + placer.treesCharred > 0 ? ", árboles quitados " + placer.treesCut + " y quemados " + placer.treesCharred : "";
        src.sendSuccess(() -> Component.literal(kind.title + " construida [" + extra + "] (" + n + " bloques" + foes + trees + "). Para quitarla: /mundos estructura deshacer"), true);
        return 1;
    }

    private static int undo(CommandSourceStack src, int times) {
        Deque<Undo> dq = UNDO.get(key(src));
        if (dq == null || dq.isEmpty()) {
            src.sendFailure(Component.literal("No hay nada que deshacer."));
            return 0;
        }
        int done = 0;
        int blocks = 0;
        while (done < times && !dq.isEmpty()) {
            Undo u = dq.pop();
            ServerLevel level = src.getServer().getLevel(u.dim);
            if (level == null) {
                continue;
            }
            if (u.volcano >= 0) {
                Volcanes.unregister(src.getServer(), u.volcano);
            }
            if (u.guard >= 0) {
                StructGuard.remove(src.getServer(), u.guard);
            }
            for (UUID id : u.mobs) {
                net.minecraft.world.entity.Entity e = level.getEntity(id);
                // un habitante ya rescatado no es parte de la estructura: se queda
                if (e != null && !(e instanceof com.mundosdeaxelis.entity.Habitante h && h.state() != com.mundosdeaxelis.entity.Habitante.PRESO)) {
                    e.discard();
                }
            }
            // al revés: primero lo de arriba, para no dejar cosas sin apoyo
            List<Map.Entry<BlockPos, StructPlacer.Snap>> list = new ArrayList<>(u.snaps.entrySet());
            list.sort((a, b) -> Integer.compare(b.getKey().getY(), a.getKey().getY()));
            for (Map.Entry<BlockPos, StructPlacer.Snap> e : list) {
                BlockPos p = e.getKey();
                BlockEntity cur = level.getBlockEntity(p);
                if (cur != null) {
                    Clearable.tryClear(cur);
                }
                level.setBlock(p, e.getValue().old(), FLAGS);
                if (e.getValue().be() != null) {
                    BlockEntity be = level.getBlockEntity(p);
                    if (be != null) {
                        be.load(e.getValue().be());
                        be.setChanged();
                    }
                }
                blocks++;
            }
            done++;
        }
        int d = done;
        int b = blocks;
        src.sendSuccess(() -> Component.literal("Deshecho: " + d + " estructura(s), " + b + " bloques restaurados."), true);
        return done;
    }

    private static Rotation rotationFor(Direction fwd) {
        return switch (fwd) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    // ------------------------------------------------------------------ bloques

    /** Se usa también desde el cliente (holograma de la Semilla de casa): por eso admite varios hilos a la vez. */
    private static final Map<String, BlockState> CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final BlockState NONE = Blocks.BARRIER.defaultBlockState();

    /** Convierte un texto como {@code oak_stairs[facing=north,half=bottom]} en un bloque; devuelve null si ese bloque no existe. */
    static BlockState state(String spec) {
        BlockState c = CACHE.get(spec);
        if (c != null) {
            return c == NONE ? null : c;
        }
        String name = spec;
        String props = "";
        int k = spec.indexOf('[');
        if (k >= 0) {
            name = spec.substring(0, k);
            props = spec.substring(k + 1, spec.length() - 1);
        }
        ResourceLocation rl = name.indexOf(':') >= 0 ? new ResourceLocation(name) : new ResourceLocation("minecraft", name);
        BlockState st = null;
        if (ForgeRegistries.BLOCKS.containsKey(rl)) {
            st = ForgeRegistries.BLOCKS.getValue(rl).defaultBlockState();
            for (String kv : props.split(",")) {
                int e = kv.indexOf('=');
                if (e > 0) {
                    Property<?> p = st.getBlock().getStateDefinition().getProperty(kv.substring(0, e).trim());
                    if (p != null) {
                        st = withProp(st, p, kv.substring(e + 1).trim());
                    }
                }
            }
        }
        CACHE.put(spec, st == null ? NONE : st);
        return st;
    }

    private static <T extends Comparable<T>> BlockState withProp(BlockState s, Property<T> p, String v) {
        return p.getValue(v).map(x -> s.setValue(p, x)).orElse(s);
    }
}
