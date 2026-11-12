package com.casino.client;

import com.casino.decor.SeatEntity;
import com.casino.diamond.RaceBE;
import com.casino.game.HorseRace;
import com.casino.plat.Ids;
import com.casino.registry.ModSounds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Lo que pasa en el cliente con las carreras: la tecla de apostar (B), saber si el jugador está sentado frente a una pantalla, los
 * galopes que se oyen durante la carrera y el aviso a un lado de la pantalla.
 */
public final class RaceClient {
    //#if MC >= 26.1
    //$$ public static final KeyMapping BET = new KeyMapping("key.casino.race_bet", 66, KeyMapping.Category.register(Ids.casino("main")));
    //#else
    public static final KeyMapping BET = new KeyMapping("key.casino.race_bet", 66, "key.categories.casino");
    //#endif

    /** Pantallas vistas hace poco (posición → último tick en que se vio). */
    private static final Map<BlockPos, Long> SEEN = new HashMap<>();
    private static long lastTick = -1;
    private static long nextGallop;
    /** La pantalla frente a la que está sentado el jugador (o null). */
    private static RaceBE active;

    private RaceClient() {
    }

    public static Component keyName() {
        return BET.getTranslatedKeyMessage();
    }

    public static RaceBE active() {
        return active;
    }

    /** Lo llama cada pantalla de carreras en cada tick del cliente; el trabajo se hace una vez por tick. */
    public static void seen(RaceBE be) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || be.getLevel() != mc.level) {
            return;
        }
        long now = mc.level.getGameTime();
        SEEN.put(be.getBlockPos().immutable(), now);
        if (now == lastTick) {
            return;
        }
        lastTick = now;
        tick(mc, now);
    }

    private static void tick(Minecraft mc, long now) {
        var pl = mc.player;
        SEEN.values().removeIf(t -> now - t > 40);
        active = null;
        if (pl == null) {
            return;
        }
        // la pantalla más cercana
        RaceBE best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : SEEN.keySet()) {
            if (mc.level.getBlockEntity(p) instanceof RaceBE be) {
                double d = pl.distanceToSqr(be.center());
                if (d < bd) {
                    bd = d;
                    best = be;
                }
            }
        }
        boolean seated = pl.getVehicle() instanceof SeatEntity;
        // sentado en la misma sala (no en el piso de arriba ni al otro lado del casino)
        if (best != null && seated && bd < 24 * 24 && Math.abs(pl.getY() - best.center().y) < 8) {
            active = best;
        }
        boolean pressed = false;
        while (BET.consumeClick()) {
            pressed = true;
        }
        if (pressed && active != null && ClientScreens.none()) {
            RaceBetScreen.open(active);
        }
        // galopes durante la carrera
        if (best != null && bd < 40 * 40 && best.phase(now) == RaceBE.Phase.RUN && now >= nextGallop) {
            double t = best.raceSeconds(now, 0);
            HorseRace r = best.race();
            if (t > 0 && t < r.finish[r.order[HorseRace.N - 1]]) {
                Vec3 c = best.center();
                float vol = (float) Math.max(0.15, 0.7 - Math.sqrt(bd) / 60);
                mc.getSoundManager().play(new SimpleSoundInstance(ModSounds.RACE_GALLOP.get(), SoundSource.BLOCKS, vol,
                        0.9F + mc.level.getRandom().nextFloat() * 0.25F, RandomSource.create(now), c.x, c.y, c.z));
                nextGallop = now + 4 + mc.level.getRandom().nextInt(3);
            }
        }
    }
}
