package com.mundosdeaxelis.world;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.BossTitlePacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Efectos de combate compartidos por los jefes: cambio de fase espectacular, etc. */
public final class BossFx {
    private BossFx() {
    }

    /**
     * Derrota del jefe (dura unos 3 segundos, hasta que sale la pantalla de resultados): impacto, destello, ondas de choque en
     * cascada, escombros que salen disparados, columna de luz que sube y un cartel de victoria.
     */
    public static void defeat(ServerLevel sl, Vec3 at, int rgb, int rgb2, ParticleOptions burst, net.minecraft.world.level.block.state.BlockState debris,
                              String title, String subtitle) {
        sl.playSound(null, at.x, at.y + 2.0, at.z, ModSounds.BOSS_IMPACT.get(), SoundSource.HOSTILE, 6.0F, 0.8F);
        Vfx.ring(sl, at.add(0, 0.2, 0), rgb2, 20.0, 30);
        Vfx.flash(sl, at.add(0, 2.5, 0), 0xFFFFFF, 10.0, 18);
        Vfx.column(sl, at.add(0, 0.1, 0), rgb2, 1.6, 26.0, 60);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(at) <= 70.0 * 70.0) {
                Fx.screen(p, ScreenFxPacket.SHAKE, 38, 60);
                Fx.screen(p, ScreenFxPacket.FLASH, 0xFFFFFF, 14);
            }
        }
        Scheduler.runLater(10, () -> {
            sl.playSound(null, at.x, at.y + 2.0, at.z, ModSounds.BOSS_SHATTER.get(), SoundSource.HOSTILE, 5.0F, 0.9F);
            sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, debris),
                    at.x, at.y + 2.0, at.z, 160, 2.2, 1.8, 2.2, 0.45);
            sl.sendParticles(burst, at.x, at.y + 2.0, at.z, 90, 2.5, 2.0, 2.5, 0.3);
            Vfx.ring(sl, at.add(0, 0.2, 0), rgb, 26.0, 30);
        });
        Scheduler.runLater(22, () -> {
            Vfx.ring(sl, at.add(0, 0.2, 0), rgb2, 32.0, 32);
            Vfx.dome(sl, at.add(0, 2.0, 0), rgb, 6.0, 20, false);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2.0 * i / 8;
                Vfx.orb(sl, at.add(Math.cos(a) * 2.0, 1.0, Math.sin(a) * 2.0), rgb2, 0.7, 36, true);
            }
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(at) <= 70.0 * 70.0) {
                    ModNetwork.sendTo(p, new BossTitlePacket(title, subtitle, rgb2, 52));
                }
            }
        });
        Scheduler.runLater(36, () -> {
            sl.playSound(null, at.x, at.y + 2.0, at.z, ModSounds.BOSS_TITLE_FORGE.get(), SoundSource.HOSTILE, 2.0F, 1.5F);
            sl.sendParticles(burst, at.x, at.y + 3.0, at.z, 50, 1.5, 2.5, 1.5, 0.15);
        });
    }

    /**
     * Cambio de fase: golpe de sonido, la pantalla tiembla y destella, círculo en el suelo, columna de luz sobre el jefe, dos
     * ondas de choque y un cartel con el número de fase.
     */
    public static void phase(ServerLevel sl, Vec3 at, int phase, int rgb, int rgb2, String title, String subtitle, ParticleOptions burst) {
        sl.playSound(null, at.x, at.y + 2.0, at.z, ModSounds.BOSS_PHASE.get(), SoundSource.HOSTILE, 5.0F, phase >= 3 ? 0.85F : 1.0F);
        Vfx.rune(sl, at.add(0, 0.1, 0), rgb, 7.0 + phase, 50);
        Vfx.column(sl, at.add(0, 0.1, 0), rgb2, 1.4, 14.0, 34);
        Vfx.ring(sl, at.add(0, 0.2, 0), rgb2, 16.0 + phase * 3.0, 28);
        Scheduler.runLater(6, () -> Vfx.ring(sl, at.add(0, 0.2, 0), rgb, 22.0 + phase * 3.0, 30));
        Vfx.dome(sl, at.add(0, 1.0, 0), rgb, 4.5, 16, false);
        sl.sendParticles(burst, at.x, at.y + 1.5, at.z, 90, 2.5, 1.5, 2.5, 0.25);
        int strength = phase >= 3 ? 40 : 28;
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(at) > 60.0 * 60.0) {
                continue;
            }
            Fx.screen(p, ScreenFxPacket.SHAKE, strength, 40);
            Fx.screen(p, ScreenFxPacket.FLASH, rgb2, 9);
            ModNetwork.sendTo(p, new BossTitlePacket(title, subtitle, rgb2, 64));
        }
    }
}
