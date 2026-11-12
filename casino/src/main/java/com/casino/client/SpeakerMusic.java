package com.casino.client;

import com.casino.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Una sola pista de jazz para todas las bocinas cercanas: el volumen depende de la bocina más cercana. */
public final class SpeakerMusic {
    /** Hasta dónde se oye (bloques). */
    private static final double RANGE = 22.0;
    private static final Map<BlockPos, Long> SEEN = new HashMap<>();
    private static Lounge current;

    private SpeakerMusic() {
    }

    /** Lo llama cada bocina prendida, cada tick. */
    public static void seen(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        SEEN.put(pos.immutable(), mc.level.getGameTime());
        if (current == null || current.isStopped() || !mc.getSoundManager().isActive(current)) {
            if (nearest() < RANGE) {
                current = new Lounge();
                mc.getSoundManager().play(current);
            }
        }
    }

    /** Distancia a la bocina más cercana que avisó hace poco (y limpia las viejas). */
    private static double nearest() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return Double.MAX_VALUE;
        }
        long now = mc.level.getGameTime();
        double best = Double.MAX_VALUE;
        for (Iterator<Map.Entry<BlockPos, Long>> it = SEEN.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Long> e = it.next();
            if (now - e.getValue() > 5 || now < e.getValue()) {
                it.remove();
                continue;
            }
            BlockPos p = e.getKey();
            double d = Math.sqrt(mc.player.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5));
            best = Math.min(best, d);
        }
        return best;
    }

    private static final class Lounge extends AbstractTickableSoundInstance {
        private float target;

        Lounge() {
            super(ModSounds.MUSIC_LOUNGE.get(), SoundSource.RECORDS, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.01F;
        }

        @Override
        public void tick() {
            double d = nearest();
            if (d >= RANGE) {
                volume = Math.max(0.0F, volume - 0.05F);
                if (volume <= 0.0F) {
                    stop();
                }
                return;
            }
            // más fuerte cerca, se apaga suave hacia el borde
            target = (float) (0.75 * Math.pow(1.0 - d / RANGE, 1.4));
            volume += (target - volume) * 0.15F;
            volume = Math.max(0.001F, volume);
        }
    }
}
