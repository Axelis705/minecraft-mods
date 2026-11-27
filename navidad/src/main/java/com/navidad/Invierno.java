package com.navidad;

import com.axeliscore.AxelisCore;
import com.axeliscore.vestir.Vestuario;
import com.navidad.objeto.Ropa;
import com.navidad.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Cosas del invierno que pasan solas cada tick: los patines de hielo (en el hielo vas mucho más rápido y suena el filo; fuera del hielo
 * caminas torpe, como con patines de verdad).
 */
public final class Invierno {
    private static final UUID PATINES = UUID.fromString("6f1c1d8e-25a1-4c7e-9a0b-7d21c2b5e1a4");
    private static final ResourceLocation PRENDA = Ropa.id("patines");
    /** En el hielo: +55% de velocidad; en la tierra: -35%. */
    public static final double HIELO = 0.55, TIERRA = -0.35;

    private Invierno() {
    }

    public static void init() {
        AxelisCore.onServerTick(Invierno::tick);
    }

    static void tick(MinecraftServer s) {
        if (s.getTickCount() % 2 != 0) {
            return;
        }
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            patines(s, p);
        }
    }

    private static void patines(MinecraftServer s, ServerPlayer p) {
        AttributeInstance vel = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (vel == null) {
            return;
        }
        boolean puestos = !p.isSpectator() && !p.isPassenger() && Vestuario.tiene(s, p.getUUID(), PRENDA);
        double quiero = 0;
        boolean enHielo = false;
        if (puestos && p.onGround()) {
            BlockState b = p.level().getBlockState(BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ()));
            enHielo = b.is(BlockTags.ICE);
            quiero = enHielo ? HIELO : TIERRA;
        } else if (puestos) {
            AttributeModifier m = vel.getModifier(PATINES);
            quiero = m == null ? 0 : m.getAmount();
        }
        AttributeModifier ya = vel.getModifier(PATINES);
        if (ya == null || ya.getAmount() != quiero) {
            if (ya != null) {
                vel.removeModifier(PATINES);
            }
            if (quiero != 0) {
                vel.addTransientModifier(new AttributeModifier(PATINES, "navidad_patines", quiero, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        // el filo raspa el hielo y salta escarcha cuando vas rápido
        if (enHielo) {
            double dx = p.getX() - p.xOld, dz = p.getZ() - p.zOld;
            double rap = Math.sqrt(dx * dx + dz * dz);
            if (rap > 0.12) {
                if (p.tickCount % 12 < 2) {
                    p.level().playSound(null, p.getX(), p.getY(), p.getZ(), ModRegistry.PATIN.get(), SoundSource.PLAYERS,
                            (float) Math.min(0.8, 0.3 + rap), 0.9F + p.getRandom().nextFloat() * 0.25F);
                }
                p.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 0.05, p.getZ(), 2, 0.15, 0.02, 0.15, 0.01);
            }
        }
    }
}
