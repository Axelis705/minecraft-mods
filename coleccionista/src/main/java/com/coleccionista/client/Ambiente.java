package com.coleccionista.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/**
 * (Cliente) Ruidos del edificio durante el evento: golpes de metal lejanos, cadenas, una puerta que se cierra, goteras, el edificio que
 * cruje... Salen de vez en cuando desde algún lugar a 8-20 bloques del jugador, bajitos, para que nadie se sienta solo del todo.
 */
public final class Ambiente {
    private static int espera = 200;

    private Ambiente() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                tick();
            }
        });
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused() || !EventoCliente.activo()) {
            return;
        }
        if (--espera > 0) {
            return;
        }
        RandomSource r = mc.level.random;
        espera = 160 + r.nextInt(360);
        SoundEvent[] sonidos = {SoundEvents.IRON_DOOR_CLOSE, SoundEvents.CHAIN_STEP, SoundEvents.CHAIN_BREAK, SoundEvents.IRON_TRAPDOOR_CLOSE,
                SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundEvents.WOOD_STEP, SoundEvents.ANVIL_LAND, SoundEvents.AMBIENT_CAVE.get(),
                SoundEvents.AMBIENT_CAVE.get(), SoundEvents.GRINDSTONE_USE, SoundEvents.CHEST_LOCKED};
        SoundEvent s = sonidos[r.nextInt(sonidos.length)];
        double ang = r.nextDouble() * Math.PI * 2, d = 8 + r.nextDouble() * 12;
        double x = mc.player.getX() + Math.cos(ang) * d, y = mc.player.getY() + r.nextInt(5) - 1, z = mc.player.getZ() + Math.sin(ang) * d;
        float vol = s == SoundEvents.ANVIL_LAND ? 0.12F : s == SoundEvents.AMBIENT_CAVE.get() ? 0.5F : 0.35F;
        float tono = 0.45F + r.nextFloat() * 0.35F;
        mc.getSoundManager().play(new SimpleSoundInstance(s, SoundSource.AMBIENT, vol, tono, r, x, y, z));
    }
}
