package com.pescacoleccion.nilo;

import com.axeliscore.data.Config;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import com.axeliscore.ui.CoreScreens;
import com.axeliscore.ui.Dialogs;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/**
 * (Servidor) Nilo en el mundo: cuándo llega (la primera vez que alguien saca un pez), dónde vive (su muelle), qué dice al conocerlo y
 * los comandos para moverlo. Hay un solo Nilo por mundo; sus datos van con el mundo (WorldStore).
 */
public final class NiloMundo {
    private static final String MOD = PescaColeccion.MODID;
    public static final ResourceLocation PIEL = Ids.rl(MOD, "textures/entity/nilo.png");

    /** Donde vive: dónde se para a pescar, hacia dónde mira y dónde cae su anzuelo. */
    public record Casa(BlockPos parado, float yaw, BlockPos agua) {
    }

    private NiloMundo() {
    }

    /** ¿Nilo llega solo la primera vez que alguien pesca? (config/pesca_coleccion.json) */
    public static boolean activo() {
        return Config.of(MOD).bool("nilo", true, "Nilo, el pescador, llega la primera vez que alguien saca un pez y arma su muelle ahí");
    }

    private static CompoundTag datos(MinecraftServer s) {
        CompoundTag g = WorldStore.get(s, MOD).global();
        if (!Nbt.has(g, "nilo")) {
            g.put("nilo", new CompoundTag());
        }
        return Nbt.c(g, "nilo");
    }

    private static void guardar(MinecraftServer s) {
        WorldStore.get(s, MOD).dirty();
    }

    private static String dim(ServerLevel l) {
        return l.dimension().toString();
    }

    /** Su casa en este mundo (null si no tiene o si vive en otra dimensión). */
    public static Casa casa(ServerLevel l) {
        CompoundTag n = datos(l.getServer());
        if (!Nbt.has(n, "x") || !dim(l).equals(Nbt.s(n, "dim"))) {
            return null;
        }
        return new Casa(new BlockPos(Nbt.i(n, "x"), Nbt.i(n, "y"), Nbt.i(n, "z")), Nbt.f(n, "yaw"),
                new BlockPos(Nbt.i(n, "ax"), Nbt.i(n, "ay"), Nbt.i(n, "az")));
    }

    private static void ponerCasa(ServerLevel l, BlockPos parado, float yaw, BlockPos agua) {
        CompoundTag n = datos(l.getServer());
        n.putInt("x", parado.getX());
        n.putInt("y", parado.getY());
        n.putInt("z", parado.getZ());
        n.putFloat("yaw", yaw);
        n.putInt("ax", agua.getX());
        n.putInt("ay", agua.getY());
        n.putInt("az", agua.getZ());
        n.putString("dim", dim(l));
        guardar(l.getServer());
    }

    /** ¿Es este el Nilo del mundo? Si no hay ninguno anotado (por ejemplo, uno invocado con /summon), se queda él y vive donde está. */
    static boolean esElNilo(ServerLevel l, Nilo n) {
        CompoundTag d = datos(l.getServer());
        if (!Nbt.hasUuid(d, "uuid") || !Nbt.b(d, "vivo")) {
            Nbt.putUuid(d, "uuid", n.getUUID());
            d.putBoolean("vivo", true);
            d.putBoolean("llego", true);
            if (casa(l) == null) {
                ponerCasa(l, n.blockPosition(), n.getYRot(), n.blockPosition().relative(n.getDirection(), 4));
            }
            guardar(l.getServer());
            return true;
        }
        return n.getUUID().equals(Nbt.uuid(d, "uuid"));
    }

    static void murio(ServerLevel l, Nilo n) {
        CompoundTag d = datos(l.getServer());
        if (n.getUUID().equals(Nbt.uuid(d, "uuid"))) {
            d.putBoolean("vivo", false);
            guardar(l.getServer());
        }
    }

    /** Se sacó un pez (con el corcho en {@code agua}): si Nilo todavía no ha llegado a este mundo, llega. */
    public static void alPescar(ServerPlayer p, BlockPos agua) {
        if (!activo() || Nbt.b(datos(Plat.server(p)), "vivo")) {
            return;
        }
        llamar(p, agua);
    }

    /** Nilo llega caminando y arma su muelle en la orilla más cercana a {@code agua}. */
    public static Nilo llamar(ServerPlayer p, BlockPos agua) {
        ServerLevel l = (ServerLevel) p.level();
        Muelle.Sitio s = agua != null ? Muelle.buscar(l, agua, p.blockPosition()) : null;
        BlockPos orilla;
        Direction atras;
        if (s != null) {
            orilla = s.orilla().above();
            atras = s.hacia().getOpposite();
            ponerCasa(l, s.parado(), s.hacia().toYRot(), s.agua());
        } else {
            orilla = p.blockPosition();
            atras = p.getDirection().getOpposite();
            BlockPos a = agua != null ? agua : orilla.relative(p.getDirection(), 4);
            ponerCasa(l, orilla.relative(p.getDirection().getClockWise()), p.getDirection().toYRot(), a);
        }
        BlockPos spawn = lejos(l, orilla, atras);
        Nilo n = Plat.create(ModRegistry.NILO.get(), l);
        n.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, atras.getOpposite().toYRot(), 0);
        l.addFreshEntity(n);
        l.sendParticles(ParticleTypes.POOF, spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5, 14, 0.3, 0.6, 0.3, 0.02);
        CompoundTag d = datos(l.getServer());
        Nbt.putUuid(d, "uuid", n.getUUID());
        d.putBoolean("vivo", true);
        d.putBoolean("llego", true);
        d.putBoolean("muelle", s != null);
        guardar(l.getServer());
        n.llegar(p, orilla, s != null ? s.piezas() : List.of());
        return n;
    }

    /** Un lugar en tierra firme unos bloques tierra adentro, para que se le vea llegar caminando. */
    private static BlockPos lejos(ServerLevel l, BlockPos orilla, Direction atras) {
        for (int dist : new int[] {10, 8, 6, 4}) {
            for (int lado : new int[] {0, 2, -2, 4, -4}) {
                BlockPos c = orilla.relative(atras, dist).relative(atras.getClockWise(), lado);
                int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ());
                BlockPos pie = new BlockPos(c.getX(), y, c.getZ());
                if (Math.abs(y - orilla.getY()) <= 3 && l.getBlockState(pie.below()).getFluidState().isEmpty()
                        && l.getBlockState(pie).isAir() && l.getBlockState(pie.above()).isAir()) {
                    return pie;
                }
            }
        }
        return orilla;
    }

    /** Llegó a la orilla: saluda a quien lo vio llegar. */
    static void saludo(ServerPlayer p, Nilo n, boolean muelle) {
        CompoundTag nd = Encargos.datos(p);
        nd.putBoolean("conoce", true);
        PlayerStore.save(p, MOD);
        Dialogs.show(p, n.getDisplayName(), PIEL, Component.translatable(MOD + ".nilo.llega" + (muelle ? "" : "_sin_muelle")),
                List.of(Component.translatable(MOD + ".nilo.llega_si"), Component.translatable(MOD + ".nilo.llega_que")), Dialogs.THEME_MAR, c -> {
                    if (c == 1) {
                        Dialogs.show(p, n.getDisplayName(), PIEL, Component.translatable(MOD + ".nilo.explica"), List.of(),
                                Dialogs.THEME_MAR, x -> {
                                });
                    }
                });
    }

    static void muelleListo(ServerLevel l) {
        Casa c = casa(l);
        if (c == null) {
            return;
        }
        l.playSound(null, c.parado(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6F, 1.2F);
        for (ServerPlayer p : l.players()) {
            if (p.blockPosition().distSqr(c.parado()) < 48 * 48) {
                Plat.msg(p, Component.translatable(MOD + ".nilo.muelle_listo"), true);
            }
        }
    }

    /** Le hablan: si no lo conoce, se presenta; si ya, abre su ventana. */
    public static void hablar(ServerPlayer p, Nilo n) {
        CompoundTag nd = Encargos.datos(p);
        Encargos.preparar(p);
        if (!Nbt.b(nd, "conoce")) {
            nd.putBoolean("conoce", true);
            PlayerStore.save(p, MOD);
            Dialogs.show(p, n.getDisplayName(), PIEL, Component.translatable(MOD + ".nilo.hola_nuevo"),
                    List.of(Component.translatable(MOD + ".nilo.ver_encargos")), Dialogs.THEME_MAR, c -> {
                        if (c >= 0) {
                            CoreScreens.open(p, MOD + ":nilo");
                        }
                    });
            return;
        }
        PlayerStore.save(p, MOD);
        CoreScreens.open(p, MOD + ":nilo");
    }

    /** El agua más cercana al jugador (la de arriba de cada columna); null si no hay en 12 bloques. */
    public static BlockPos aguaCerca(ServerPlayer p) {
        ServerLevel l = (ServerLevel) p.level();
        BlockPos c = p.blockPosition();
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (int dy = -4; dy <= 2; dy++) {
            for (int dx = -12; dx <= 12; dx++) {
                for (int dz = -12; dz <= 12; dz++) {
                    BlockPos b = c.offset(dx, dy, dz);
                    if (l.getBlockState(b).is(net.minecraft.world.level.block.Blocks.WATER)
                            && !l.getBlockState(b.above()).is(net.minecraft.world.level.block.Blocks.WATER)) {
                        double d = b.distSqr(c);
                        if (d < bd) {
                            bd = d;
                            best = b;
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Comando: Nilo vive aquí (sin muelle), mirando hacia donde mira el jugador. */
    public static void aqui(ServerPlayer p) {
        ServerLevel l = (ServerLevel) p.level();
        float yaw = Mth.wrapDegrees(p.getYRot());
        BlockPos agua = p.blockPosition().relative(p.getDirection(), 4);
        ponerCasa(l, p.blockPosition(), yaw, agua);
    }
}
