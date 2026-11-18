package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.registry.ModSounds;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

/** Reproduce los sonidos propios de cada reliquia ("star_bow", "charge" -> relic_star_bow_charge.ogg). */
public final class RelicSounds {
    private RelicSounds() {
    }

    /** El sonido, o null si esa reliquia no tiene ese tipo. */
    public static SoundEvent get(String relic, String kind) {
        RegistryObject<SoundEvent> o = ModSounds.RELIC_KEYED.get(relic + "." + kind);
        return o == null ? null : o.get();
    }

    public static boolean has(String relic, String kind) {
        return ModSounds.RELIC_KEYED.containsKey(relic + "." + kind);
    }

    public static void play(ServerLevel sl, double x, double y, double z, String relic, String kind, float volume, float pitch) {
        SoundEvent e = get(relic, kind);
        if (e != null) {
            sl.playSound(null, x, y, z, e, SoundSource.PLAYERS, volume, pitch);
        }
    }

    public static void play(ServerLevel sl, Vec3 p, String relic, String kind, float volume, float pitch) {
        play(sl, p.x, p.y, p.z, relic, kind, volume, pitch);
    }

    public static void play(ServerLevel sl, Entity e, String relic, String kind, float volume, float pitch) {
        play(sl, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), relic, kind, volume, pitch);
    }

    /** Variante con un poco de variación de tono al azar (para golpes repetidos). */
    public static void playVaried(ServerLevel sl, Vec3 p, String relic, String kind, float volume) {
        play(sl, p, relic, kind, volume, 0.92F + sl.random.nextFloat() * 0.16F);
    }

    /** Corta un sonido largo (por ejemplo la carga) cuando el jugador suelta o cancela. */
    public static void stop(ServerPlayer sp, String relic, String kind) {
        if (sp.connection != null) {
            sp.connection.send(new ClientboundStopSoundPacket(new ResourceLocation(MundosDeAxelis.MODID, "relic_" + relic + "_" + kind), SoundSource.PLAYERS));
        }
    }
}
