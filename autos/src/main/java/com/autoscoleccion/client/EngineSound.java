package com.autoscoleccion.client;

import com.autoscoleccion.entity.RcCarEntity;
import com.autoscoleccion.game.CarStats;
import com.autoscoleccion.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Motor del auto a control remoto: un sonido en bucle que sube de tono con la velocidad. */
public class EngineSound extends AbstractTickableSoundInstance {
    private final RcCarEntity car;
    private boolean finished;

    public EngineSound(RcCarEntity car) {
        super(ModSounds.get("engine"), SoundSource.PLAYERS, RandomSource.create());
        this.car = car;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.35F;
        this.pitch = 0.6F;
        this.x = car.getX();
        this.y = car.getY();
        this.z = car.getZ();
    }

    public void finish() {
        finished = true;
    }

    @Override
    public void tick() {
        if (finished || car.isRemoved()) {
            stop();
            return;
        }
        x = car.getX();
        y = car.getY();
        z = car.getZ();
        float top = CarStats.of(car.def()).top;
        float f = Math.min(1.0F, Math.abs(car.speed) / top);
        float rev = car.throttle != 0 ? 0.15F : 0;
        pitch = 0.6F + 1.3F * f + rev;
        volume = 0.25F + 0.35F * f + (car.throttle != 0 ? 0.1F : 0);
    }
}
