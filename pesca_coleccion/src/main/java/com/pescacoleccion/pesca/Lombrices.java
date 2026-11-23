package com.pescacoleccion.pesca;

import com.axeliscore.AxelisCore;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Lombrices: a veces salen al cavar tierra, pasto o tierra de cultivo (más si llueve encima). */
public final class Lombrices {
    /** Probabilidad de que salga una (con lluvia y sin lluvia). */
    public static final float CON_LLUVIA = 0.22F, SIN_LLUVIA = 0.07F;

    private static boolean aviso;

    private Lombrices() {
    }

    public static void init() {
        AxelisCore.onBreak(Lombrices::alRomper);
    }

    /** ¿De este bloque pueden salir lombrices? */
    public static boolean tierra(BlockState st) {
        return st.is(BlockTags.DIRT) || st.is(Blocks.FARMLAND);
    }

    static void alRomper(ServerPlayer p, ServerLevel level, BlockPos pos, BlockState state) {
        if (p.isCreative() || p.isSpectator() || !tierra(state)) {
            return;
        }
        boolean lluvia = level.isRainingAt(pos.above());
        float chance = lluvia ? CON_LLUVIA : SIN_LLUVIA;
        if (!aviso) {
            aviso = true;
            com.pescacoleccion.PescaColeccion.LOGGER.info("PESCA_LOMBRICES primera tierra cavada (lluvia={})", lluvia);
        }
        if (level.getRandom().nextFloat() < chance) {
            Block.popResource(level, pos, new ItemStack(ModRegistry.CEBOS.get(Cebo.LOMBRIZ).get()));
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_HIT, SoundSource.BLOCKS, 0.5F, 1.7F);
        }
    }
}
