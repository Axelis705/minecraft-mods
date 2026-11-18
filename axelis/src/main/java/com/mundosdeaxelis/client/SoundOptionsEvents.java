package com.mundosdeaxelis.client;

import com.mundosdeaxelis.ClientConfig;
import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

/**
 * Volumen de las cajas: un deslizador «Cajas» en Escape → Opciones → Música y sonidos, y su efecto sobre los sonidos
 * {@code crate_*} (ruleta, cerradura y premio).
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SoundOptionsEvents {
    private SoundOptionsEvents() {
    }

    public static float crateFactor() {
        return ClientConfig.CRATE_VOLUME.get() / 100.0F;
    }

    /** Un sonido igual al original pero con el volumen multiplicado (el volumen real se lee al reproducirlo, cuando ya está resuelto). */
    private record Scaled(SoundInstance in) implements SoundInstance {
        @Override
        public ResourceLocation getLocation() {
            return in.getLocation();
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            return in.resolve(manager);
        }

        @Override
        public Sound getSound() {
            return in.getSound();
        }

        @Override
        public SoundSource getSource() {
            return in.getSource();
        }

        @Override
        public boolean isLooping() {
            return in.isLooping();
        }

        @Override
        public boolean isRelative() {
            return in.isRelative();
        }

        @Override
        public int getDelay() {
            return in.getDelay();
        }

        @Override
        public float getVolume() {
            return in.getVolume() * crateFactor(); // se lee en vivo: el volumen cambia también en los sonidos que ya están sonando
        }

        @Override
        public float getPitch() {
            return in.getPitch();
        }

        @Override
        public double getX() {
            return in.getX();
        }

        @Override
        public double getY() {
            return in.getY();
        }

        @Override
        public double getZ() {
            return in.getZ();
        }

        @Override
        public Attenuation getAttenuation() {
            return in.getAttenuation();
        }

        @Override
        public boolean canStartSilent() {
            return in.canStartSilent();
        }

        @Override
        public boolean canPlaySound() {
            return in.canPlaySound();
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary lib, Sound sound, boolean looping) {
            return in.getStream(lib, sound, looping);
        }
    }

    @SubscribeEvent
    public static void onPlay(PlaySoundEvent e) {
        SoundInstance s = e.getSound();
        if (s == null) {
            return;
        }
        ResourceLocation rl = s.getLocation();
        if (!MundosDeAxelis.MODID.equals(rl.getNamespace()) || !rl.getPath().startsWith("crate_") || s instanceof Scaled) {
            return;
        }
        // siempre se envuelve (aunque el volumen sea 100 %): así un cambio posterior del deslizador afecta a lo que ya está sonando
        e.setSound(new Scaled(s));
    }

    /** Añade el deslizador «Cajas» a la pantalla de sonidos de Minecraft. */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof SoundOptionsScreen)) {
            return;
        }
        for (GuiEventListener l : e.getScreen().children()) {
            if (l instanceof OptionsList list) {
                list.addSmall(crateOption(), null);
                return;
            }
        }
    }

    private static OptionInstance<Double> crateOption() {
        return new OptionInstance<Double>("options.mundos_de_axelis.crate_volume", OptionInstance.noTooltip(),
                (caption, value) -> value == 0.0D ? CommonComponents.optionNameValue(caption, CommonComponents.OPTION_OFF)
                        : Component.translatable("options.percent_value", caption, (int) Math.round(value * 100.0D)),
                OptionInstance.UnitDouble.INSTANCE, ClientConfig.CRATE_VOLUME.get() / 100.0D,
                v -> {
                    ClientConfig.CRATE_VOLUME.set((int) Math.round(v * 100.0D));
                    // Minecraft recalcula el volumen de todo lo que está sonando (el deslizador se aplica al instante)
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    mc.getSoundManager().updateSourceVolume(SoundSource.BLOCKS, mc.options.getSoundSourceVolume(SoundSource.BLOCKS));
                });
    }
}
