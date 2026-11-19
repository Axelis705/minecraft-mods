package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Efectos de movimiento de las criaturas, solo en el cliente: polvo del bloque que pisan en cada paso (más en las pesadas),
 * nube al aterrizar, chispas u hojas al recibir un golpe, estelas de los voladores, partículas de ambiente y el estallido al
 * morir. Cada criatura dice cuáles con su {@link CreatureStyle}.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, value = Dist.CLIENT)
public final class MotionFx {
    /** Por criatura: último paso, último golpe, ya hizo el estallido de muerte, último aterrizaje con polvo. */
    private static final Map<LivingEntity, int[]> STATE = new WeakHashMap<>();
    private static ClientLevel lastLevel;

    private MotionFx() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != lastLevel) {
            STATE.clear();
            Motion.clear();
            lastLevel = mc.level;
        }
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        int fxLevel = mc.options.particles().get().getId(); // 0 todas, 1 menos, 2 mínimas
        for (Entity en : mc.level.entitiesForRendering()) {
            if (!(en instanceof LivingEntity le) || en instanceof Player || le.isInvisible()) {
                continue;
            }
            CreatureStyle st = CreatureStyles.find(le);
            if (st == null || le.distanceToSqr(cam) > 48 * 48) {
                continue;
            }
            Motion m = Motion.of(le);
            int[] s = STATE.computeIfAbsent(le, k -> new int[]{Integer.MIN_VALUE, k.hurtTime, 0, -100});
            if (fxLevel >= 2) {
                continue;
            }
            tickOne(mc.level, le, st, m, s, fxLevel == 1);
        }
    }

    private static void tickOne(ClientLevel level, LivingEntity le, CreatureStyle st, Motion m, int[] s, boolean fewer) {
        RandomSource r = le.getRandom();
        boolean alive = le.deathTime <= 0 && le.isAlive();
        // pasos
        int idx = Mth.floor(le.walkAnimation.position() * st.stepFreq / Anim.PI);
        if (s[0] != idx) {
            if (s[0] != Integer.MIN_VALUE && st.dust && alive && le.onGround() && le.walkAnimation.speed() > 0.18F) {
                step(level, le, st, idx, r, fewer);
            }
            s[0] = idx;
        }
        // aterrizaje
        if (m.landedTick == le.tickCount && s[3] != le.tickCount && st.dust && m.landed > 0.15F) {
            s[3] = le.tickCount;
            BlockState bs = below(level, le);
            if (bs != null) {
                int n = (int) (6 + m.landed * (st.heavy ? 22 : 12)) / (fewer ? 2 : 1);
                double rad = le.getBbWidth() * 0.6;
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n + r.nextDouble() * 0.3;
                    level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, bs), le.getX() + Math.cos(a) * rad, le.getY() + 0.05,
                            le.getZ() + Math.sin(a) * rad, Math.cos(a) * 0.18, 0.06 + r.nextDouble() * 0.08, Math.sin(a) * 0.18);
                }
            }
        }
        // golpe recibido
        if (le.hurtTime > s[1] && st.hurtFx != null) {
            ParticleOptions p = st.hurtFx.get();
            Vec3 c = le.position().add(0, le.getBbHeight() * 0.6, 0);
            int n = fewer ? 4 : 9;
            for (int i = 0; i < n; i++) {
                double vx = (r.nextDouble() - 0.5) * 0.35, vy = r.nextDouble() * 0.25, vz = (r.nextDouble() - 0.5) * 0.35;
                level.addParticle(p, c.x + vx, c.y + (r.nextDouble() - 0.5) * le.getBbHeight() * 0.5, c.z + vz, vx, vy, vz);
            }
        }
        s[1] = le.hurtTime;
        // estela
        if (st.trailFx != null && alive && le.tickCount % st.trailEvery == 0 && (st.trailWhenIdle || m.speed(1.0F) > 0.04F)) {
            ParticleOptions p = st.trailFx.get();
            double y = le.getY() + le.getBbHeight() * 0.45;
            level.addParticle(p, le.getX() + (r.nextDouble() - 0.5) * le.getBbWidth() * 0.6, y + (r.nextDouble() - 0.5) * 0.2,
                    le.getZ() + (r.nextDouble() - 0.5) * le.getBbWidth() * 0.6, 0.0, 0.01, 0.0);
        }
        // ambiente
        if (st.ambientFx != null && alive && !fewer && (le.tickCount + le.getId()) % st.ambientEvery == 0) {
            ParticleOptions p = st.ambientFx.get();
            level.addParticle(p, le.getX() + (r.nextDouble() - 0.5) * le.getBbWidth() * 1.2, le.getY() + r.nextDouble() * le.getBbHeight(),
                    le.getZ() + (r.nextDouble() - 0.5) * le.getBbWidth() * 1.2, 0.0, 0.015, 0.0);
        }
        // muerte
        if (le.deathTime > 0 && s[2] == 0) {
            s[2] = 1;
            if (st.deathFx != null) {
                ParticleOptions p = st.deathFx.get();
                Vec3 c = le.position().add(0, le.getBbHeight() * 0.5, 0);
                int n = fewer ? st.deathCount / 2 : st.deathCount;
                for (int i = 0; i < n; i++) {
                    double vx = (r.nextDouble() - 0.5) * 0.3, vy = r.nextDouble() * 0.22, vz = (r.nextDouble() - 0.5) * 0.3;
                    level.addParticle(p, c.x + vx * 2, c.y + (r.nextDouble() - 0.5) * le.getBbHeight() * 0.7, c.z + vz * 2, vx, vy, vz);
                }
            }
        }
    }

    private static BlockState below(ClientLevel level, LivingEntity le) {
        BlockState bs = level.getBlockState(BlockPos.containing(le.getX(), le.getY() - 0.2, le.getZ()));
        if (bs.isAir() || bs.getRenderShape() == RenderShape.INVISIBLE) {
            return null;
        }
        return bs;
    }

    private static void step(ClientLevel level, LivingEntity le, CreatureStyle st, int idx, RandomSource r, boolean fewer) {
        BlockState bs = below(level, le);
        if (bs == null) {
            return;
        }
        float yaw = le.yBodyRot * Anim.DEG;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double rx = -fz, rz = fx;
        double side = ((idx & 1) == 0 ? 1 : -1) * st.footSpread * le.getBbWidth();
        double x = le.getX() + rx * side + fx * 0.12, z = le.getZ() + rz * side + fz * 0.12;
        int n = st.heavy ? 7 : 2;
        if (fewer) {
            n = Math.max(1, n / 2);
        }
        float spd = Math.min(1.0F, le.walkAnimation.speed());
        for (int i = 0; i < n; i++) {
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, bs), x + (r.nextDouble() - 0.5) * 0.25, le.getY() + 0.05,
                    z + (r.nextDouble() - 0.5) * 0.25, (r.nextDouble() - 0.5) * 0.12 - fx * 0.05 * spd, 0.05 + r.nextDouble() * 0.09,
                    (r.nextDouble() - 0.5) * 0.12 - fz * 0.05 * spd);
        }
        if (st.heavy && !fewer) {
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3 + r.nextDouble() * 0.4;
                level.addParticle(ParticleTypes.POOF, x + Math.cos(a) * 0.35, le.getY() + 0.1, z + Math.sin(a) * 0.35, Math.cos(a) * 0.03, 0.01,
                        Math.sin(a) * 0.03);
            }
        }
    }
}
