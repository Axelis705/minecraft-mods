package com.mundosdeaxelis.client;

import com.mundosdeaxelis.block.CrateBlockEntity;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.crate.CratePools;
import com.mundosdeaxelis.crate.CrateRoll;
import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Lado cliente del crate: chispas de reposo, sonido de los "tic" de la ruleta y partículas mientras gira (para todos los jugadores). */
public final class CrateClient {
    private CrateClient() {
    }

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(CrateUi.r(rgb), CrateUi.g(rgb), CrateUi.b(rgb)), size);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CrateBlockEntity be) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) {
            return;
        }
        Vec3 c = Vec3.atCenterOf(pos);
        double d2 = me.distanceToSqr(c);
        if (d2 > 48 * 48) {
            return;
        }
        RandomSource r = level.random;
        CrateKind kind = be.kind();
        long gt = level.getGameTime();
        if (!be.rolling()) {
            if (d2 < 24 * 24 && gt % 5 == 0) {
                double a = r.nextDouble() * Math.PI * 2;
                double rad = 0.35 + r.nextDouble() * 0.2;
                level.addParticle(dust(kind.color, 0.9F), c.x + Math.cos(a) * rad, pos.getY() + 0.9, c.z + Math.sin(a) * rad,
                        0, 0.02 + r.nextDouble() * 0.02, 0);
                if (r.nextInt(4) == 0) {
                    level.addParticle(ParticleTypes.END_ROD, c.x + (r.nextDouble() - 0.5) * 0.8, pos.getY() + 1.0,
                            c.z + (r.nextDouble() - 0.5) * 0.8, 0, 0.01, 0);
                }
            }
            return;
        }
        long el = gt - be.start;
        if (el < 0 || el > CrateRoll.TOTAL + 5) {
            return;
        }
        // quien abre ya oye los sonidos en su pantalla; los demás los oyen desde el bloque
        boolean mine = Minecraft.getInstance().screen instanceof CrateRollScreen s && s.data.pos.equals(pos);
        Item[] strip = be.strip();
        int slot = Mth.clamp((int) Math.round(CrateRoll.pos(Math.min(el, CrateRoll.ROLL), be.seed)), 0, strip.length - 1);
        Tier cur = el < CrateRoll.ROLL ? CratePools.tierOf(be.kind(), strip[slot]) : CratePools.tierOf(be.kind(), be.winnerItem());
        if (!mine && d2 < 32 * 32) {
            if (!be.spinSoundDone) {
                be.spinSoundDone = true;
                level.playLocalSound(c.x, c.y, c.z, ModSounds.CRATE_SPIN.get(), SoundSource.BLOCKS, 0.8F, 1.0F, false);
            }
            if (el <= CrateRoll.ROLL && slot != be.lastSlot) {
                be.lastSlot = slot;
                float pr = el / (float) CrateRoll.ROLL;
                level.playLocalSound(c.x, c.y, c.z, ModSounds.CRATE_TICK.get(), SoundSource.BLOCKS, 0.5F,
                        0.85F + 0.35F * pr, false);
            }
        }
        // columna de partículas del color de lo que cae bajo la flecha
        for (int i = 0; i < 2; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            level.addParticle(dust(cur.color, 1.2F), c.x + Math.cos(a) * 0.45, pos.getY() + 0.9 + r.nextDouble() * 0.3,
                    c.z + Math.sin(a) * 0.45, 0, 0.08 + r.nextDouble() * 0.05, 0);
        }
        if (el == CrateRoll.ROLL) {
            int n = 10 + cur.ordinal() * 10;
            for (int i = 0; i < n; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                double sp = 0.1 + r.nextDouble() * 0.25;
                level.addParticle(i % 3 == 0 ? ParticleTypes.END_ROD : dust(cur.color, 1.4F),
                        c.x, pos.getY() + 1.2, c.z, Math.cos(a) * sp, 0.1 + r.nextDouble() * 0.2, Math.sin(a) * sp);
            }
        }
    }
}
