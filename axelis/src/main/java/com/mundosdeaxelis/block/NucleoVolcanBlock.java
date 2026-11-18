package com.mundosdeaxelis.block;

import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.vfx.VfxType;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Corazón del volcán (estructura del volcán, en el centro del lago de lava falsa): se ve igual que la lava falsa, pero cada uno o
 * dos minutos, si hay alguien cerca, el volcán entra en erupción: retumba, tiembla la pantalla, sale una columna de fuego y humo y
 * caen bolas de fuego por las laderas. Es solo adorno: no hace daño ni rompe nada.
 */
public class NucleoVolcanBlock extends LavaFalsaBlock {
    private static final int LAVA = 0xFF6A1A;

    public NucleoVolcanBlock(Properties props) {
        super(props);
    }

    private static int idle(RandomSource r) {
        return 1200 + r.nextInt(1200);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide && !old.is(this) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            // la primera, pronto (para que se vea al llegar)
            level.scheduleTick(pos, this, 400 + level.random.nextInt(400));
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, idle(r));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        level.scheduleTick(pos, this, idle(r));
        boolean near = false;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 140 * 140) {
                near = true;
                break;
            }
        }
        if (near) {
            erupt(level, pos, r);
        }
    }

    /** La erupción: retumbar, temblor, columna de fuego y bolas de fuego que caen por las laderas. */
    public static void erupt(ServerLevel level, BlockPos pos, RandomSource r) {
        Vec3 top = Vec3.atCenterOf(pos).add(0, 1.0, 0);
        level.playSound(null, top.x, top.y, top.z, com.mundosdeaxelis.registry.ModSounds.VOLCAN_RETUMBO.get(), SoundSource.AMBIENT, 8.0F, 1.0F);
        level.playSound(null, top.x, top.y, top.z, SoundEvents.LAVA_AMBIENT, SoundSource.AMBIENT, 6.0F, 0.5F);
        level.playSound(null, top.x, top.y, top.z, com.mundosdeaxelis.registry.ModSounds.CORAZON_LATIDO.get(), SoundSource.AMBIENT, 6.0F, 0.7F);
        com.mundosdeaxelis.relic.RelicFx.shakeNear(level, top, 110, 1.2F, 30);
        Vfx.make(level, VfxType.COLUMN).at(top).rgb(LAVA).size(4.0).a(30.0).life(50).range(RANGE).send();
        for (int k = 0; k < 8; k++) {
            final int kk = k;
            Scheduler.runLater(k * 4, () -> {
                far(level, top, ParticleTypes.LAVA, top.x, top.y + 1, top.z, 30, 3.0, 1.0, 3.0, 0.0);
                far(level, top, ParticleTypes.LARGE_SMOKE, top.x, top.y + 6 + kk * 2, top.z, 30, 3.0, 2.0, 3.0, 0.05);
                far(level, top, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, top.x, top.y + 4 + kk * 3, top.z, 6, 2.0, 2.0, 2.0, 0.02);
                far(level, top, ParticleTypes.FLAME, top.x, top.y + 2, top.z, 40, 2.5, 3.0, 2.5, 0.08);
            });
        }
        int n = 7 + r.nextInt(5);
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = 16 + r.nextDouble() * 34;
            int x = (int) Math.round(top.x + Math.cos(a) * d), z = (int) Math.round(top.z + Math.sin(a) * d);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            Vec3 land = new Vec3(x + 0.5, y, z + 0.5);
            Scheduler.runLater(6 + i * 3, () -> lob(level, top, land, 34 + r.nextInt(14), 22 + r.nextDouble() * 14));
        }
    }

    /** Hasta dónde se ve la erupción (las partículas normales solo llegan a 32 bloques). */
    private static final double RANGE = 180.0;

    /** Partículas que se ven de lejos: se mandan "forzadas" a cada jugador a menos de {@link #RANGE}. */
    private static void far(ServerLevel level, Vec3 c, net.minecraft.core.particles.ParticleOptions p, double x, double y, double z, int n,
                            double dx, double dy, double dz, double speed) {
        for (ServerPlayer sp : level.players()) {
            if (sp.distanceToSqr(c) < RANGE * RANGE) {
                level.sendParticles(sp, p, true, x, y, z, n, dx, dy, dz, speed);
            }
        }
    }

    private static void lob(ServerLevel level, Vec3 a, Vec3 b, int ticks, double arc) {
        int id = Vfx.make(level, VfxType.ORB).at(a).rgb(LAVA).size(0.7).life(ticks + 6).flags(VfxType.F_TRAIL).range(RANGE).send();
        for (int t = 1; t <= ticks; t++) {
            final double u = t / (double) ticks;
            final boolean last = t == ticks;
            Scheduler.runLater(t, () -> {
                Vec3 p = a.lerp(b, u).add(0, arc * 4 * u * (1 - u), 0);
                Vfx.make(level, VfxType.ORB).at(p).rgb(LAVA).size(0.7).flags(VfxType.F_TRAIL).life(0).update(id);
                if (last) {
                    Vfx.remove(level, id);
                    far(level, b, ParticleTypes.LAVA, b.x, b.y + 0.3, b.z, 12, 0.6, 0.2, 0.6, 0.0);
                    far(level, b, ParticleTypes.LARGE_SMOKE, b.x, b.y + 0.5, b.z, 10, 0.6, 0.4, 0.6, 0.02);
                    far(level, b, ParticleTypes.FLAME, b.x, b.y + 0.4, b.z, 16, 0.6, 0.3, 0.6, 0.05);
                    level.playSound(null, b.x, b.y, b.z, SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 1.6F, 0.8F);
                }
            });
        }
    }
}
