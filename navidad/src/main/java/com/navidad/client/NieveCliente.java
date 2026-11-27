package com.navidad.client;

import com.navidad.Navidad;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * En temporada navideña caen copitos de nieve alrededor de quien está a cielo abierto (solo se ve; no cambia el clima): más en lo frío,
 * poquitos en lo templado y nada en el desierto, la sabana o las tierras baldías.
 */
@Mod.EventBusSubscriber(modid = Navidad.MODID, value = Dist.CLIENT)
public final class NieveCliente {
    private NieveCliente() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || mc.level.isRaining() || !Montar.temporada()) {
            return;
        }
        if (!mc.level.dimensionType().hasSkyLight() || mc.level.dimensionType().hasCeiling()) {
            return;
        }
        // en el desierto, la sabana o las tierras baldías no cae; en lo frío cae más
        float temp = mc.level.getBiome(mc.player.blockPosition()).value().getBaseTemperature();
        if (temp >= 1.0F) {
            return;
        }
        int copos = temp < 0.3F ? 6 : temp < 0.7F ? 3 : 2;
        RandomSource r = mc.level.random;
        double px = mc.player.getX(), py = mc.player.getEyeY(), pz = mc.player.getZ();
        for (int i = 0; i < copos; i++) {
            double x = px + (r.nextDouble() - 0.5) * 28, y = py + 4 + r.nextDouble() * 10, z = pz + (r.nextDouble() - 0.5) * 28;
            BlockPos b = BlockPos.containing(x, y, z);
            if (!mc.level.canSeeSky(b)) {
                continue;
            }
            mc.level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, (r.nextDouble() - 0.5) * 0.03, -0.06 - r.nextDouble() * 0.04,
                    (r.nextDouble() - 0.5) * 0.03);
        }
    }
}
