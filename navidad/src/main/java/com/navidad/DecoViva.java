package com.navidad;

import com.navidad.bloque.deco.Deco;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Adornos que responden al clic derecho: la esfera de nieve se sacude (nieva adentro), las campanas suenan, el cascanueces truena, los
 * regalos de adorno suenan a vacío, y la taza de chocolate y la olla de ponche se pueden probar (quitan el frío, una vez cada medio minuto).
 * Agachado, el clic derecho pone bloques normales junto a ellos.
 */
public final class DecoViva {
    private static final Map<UUID, Integer> SORBO = new HashMap<>();
    /** Cuántas veces respondió un adorno (para la prueba con fotos). */
    public static int usos;

    private DecoViva() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(DecoViva::alUsar);
    }

    private static boolean es(Block b, String... ids) {
        for (String id : ids) {
            if (Deco.block(id) == b) {
                return true;
            }
        }
        return false;
    }

    static void alUsar(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || e.getEntity().isSecondaryUseActive()) {
            return;
        }
        Block b = e.getLevel().getBlockState(e.getPos()).getBlock();
        String que = es(b, "esfera_nieve") ? "esfera" : es(b, "campanas") ? "campanas" : es(b, "cascanueces") ? "cascanueces"
                : es(b, "regalo_rojo", "regalo_verde", "regalo_azul", "regalo_dorado", "pila_regalos") ? "regalo"
                : es(b, "taza_chocolate", "olla_ponche") ? "bebida" : null;
        if (que == null) {
            return;
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (e.getEntity() instanceof ServerPlayer p) {
            usar(p, p.serverLevel(), e.getPos(), que);
        }
    }

    private static void usar(ServerPlayer p, ServerLevel l, BlockPos pos, String que) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        float tono = 0.9F + l.getRandom().nextFloat() * 0.4F;
        switch (que) {
            case "esfera" -> {
                l.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 0.1, z, 24, 0.18, 0.2, 0.18, 0.01);
                l.sendParticles(ParticleTypes.END_ROD, x, y + 0.2, z, 3, 0.15, 0.15, 0.15, 0.0);
                l.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2F, tono + 0.3F);
            }
            case "campanas" -> {
                l.playSound(null, pos, ModRegistry.CASCABEL.get(), SoundSource.BLOCKS, 1F, tono);
                l.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.7F, tono + 0.2F);
                l.sendParticles(ParticleTypes.NOTE, x, y + 0.6, z, 1, 0, 0, 0, l.getRandom().nextInt(24) / 24.0);
            }
            case "cascanueces" -> {
                l.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1F, 0.6F);
                l.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.5F, 1.6F);
                l.sendParticles(ParticleTypes.CRIT, x, y + 0.6, z, 6, 0.1, 0.1, 0.1, 0.1);
            }
            case "regalo" -> {
                l.playSound(null, pos, SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.BLOCKS, 0.8F, tono);
                p.displayClientMessage(Component.translatable("navidad.deco.regalo_vacio").withStyle(ChatFormatting.GRAY), true);
            }
            case "bebida" -> {
                Integer ya = SORBO.get(p.getUUID());
                if (ya != null && p.server.getTickCount() - ya < 600) {
                    p.displayClientMessage(Component.translatable("navidad.deco.bebida_espera").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                SORBO.put(p.getUUID(), p.server.getTickCount());
                p.setTicksFrozen(0);
                p.clearFire();
                p.getFoodData().eat(1, 0.3F);
                l.playSound(null, pos, SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.8F, 1.1F);
                l.sendParticles(ParticleTypes.CLOUD, x, y + 0.4, z, 4, 0.1, 0.1, 0.1, 0.01);
                p.displayClientMessage(Component.translatable("navidad.deco.bebida").withStyle(ChatFormatting.GOLD), true);
            }
            default -> {
            }
        }
        usos++;
    }
}
