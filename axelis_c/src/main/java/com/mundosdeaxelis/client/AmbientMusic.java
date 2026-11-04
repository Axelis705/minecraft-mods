package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Música ambiental de cada dungeon. Suena en bucle con la barra de
 * volumen de "Música", entra y sale con un fundido, y apaga la música normal de Minecraft en esos mundos.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AmbientMusic {
    private static TrackSound current;
    /** ¿Hay un jefe cerca? (se revisa cada medio segundo). */
    private static boolean bossAround = false;

    private AmbientMusic() {
    }

    private static SoundEvent wanted(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim) {
        if (ModDimensions.isForest(dim)) {
            return bossAround ? ModSounds.MUSIC_FOREST_BOSS.get() : ModSounds.MUSIC_FOREST_DUNGEON.get();
        }
        if (ModDimensions.isForge(dim)) {
            return bossAround ? ModSounds.MUSIC_FORGE_BOSS.get() : ModSounds.MUSIC_FORGE_DUNGEON.get();
        }
        return null;
    }

    /** Apaga toda la música de fondo de Minecraft (menú y juego): solo suena la del mod. Los discos no se tocan. */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound != null && sound.getSource() == SoundSource.MUSIC
                && "minecraft".equals(sound.getLocation().getNamespace())) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getGameTime() % 10 == 0) {
            boolean found = false;
            if (ModDimensions.isForest(mc.level.dimension())) {
                for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                    if (e instanceof com.mundosdeaxelis.entity.ReyBrote && e.isAlive()) {
                        found = true;
                        break;
                    }
                }
            }
            if (ModDimensions.isForge(mc.level.dimension())) {
                for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                    if (e instanceof com.mundosdeaxelis.entity.GranHerrero && e.isAlive()) {
                        found = true;
                        break;
                    }
                }
            }
            bossAround = found;
        }
        SoundEvent want = (mc.level == null || mc.player == null) ? null : wanted(mc.level.dimension());
        if (want != null) {
            mc.getMusicManager().stopPlaying();
        }
        if (current != null && (current.isStopped() || current.event != want)) {
            current.fadeOut();
            current = null;
        }
        if (current == null && want != null) {
            current = new TrackSound(want);
            mc.getSoundManager().play(current);
        }
    }

    /** Una pista en bucle, sin posición (se oye igual en todas partes), con fundido de entrada y de salida. */
    private static final class TrackSound extends AbstractTickableSoundInstance {
        private static final float TARGET = 0.55F;
        final SoundEvent event;
        private boolean fading = false;

        TrackSound(SoundEvent event) {
            super(event, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.event = event;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        void fadeOut() {
            this.fading = true;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) {
                stop();
                return;
            }
            if (fading) {
                this.volume -= 0.02F;
                if (this.volume <= 0.0F) {
                    stop();
                }
            } else if (this.volume < TARGET) {
                this.volume = Math.min(TARGET, this.volume + 0.006F);
            }
        }
    }
}
