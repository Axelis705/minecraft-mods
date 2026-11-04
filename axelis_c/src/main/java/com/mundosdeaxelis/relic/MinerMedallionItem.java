package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

/** Medallón del Minero: prisa y vista nocturna bajo tierra; activo: sonar que marca las menas cercanas. */
public class MinerMedallionItem extends RelicItem {
    private static final int R = 12;

    public MinerMedallionItem() {
        super("miner_medallion", 0xFFD36B, 20 * 25, Rarity.RARE);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sp.getY() < 56 && !sl.canSeeSky(sp.blockPosition())) {
            sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 0, true, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, false));
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        BlockPos c = sp.blockPosition();
        List<BlockPos> ores = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-R, -R, -R), c.offset(R, R, R))) {
            if (sl.getBlockState(p).is(Tags.Blocks.ORES)) {
                ores.add(p.immutable());
            }
        }
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.3F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xFFD36B, 40);
        for (int i = 1; i <= R; i++) {
            final int r = i;
            Scheduler.runLater(i, () -> Fx.ring(sl, Fx.dust(0xFFD36B, 1.0F), sp.getX(), sp.getY() + 0.2, sp.getZ(), r, 8 + r * 3, 0));
        }
        int n = Math.min(ores.size(), 40);
        for (int k = 0; k < n; k++) {
            BlockPos p = ores.get(k);
            double d = Math.sqrt(p.distSqr(c));
            for (int rep = 0; rep < 8; rep++) {
                Scheduler.runLater((int) d + rep * 12, () -> {
                    if (sp.isAlive() && !sp.hasDisconnected()) {
                        sl.sendParticles(sp, Fx.spark(0xFFE680, 1.3F), true, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
                    }
                });
            }
        }
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.miner_found", ores.size()), true);
        return true;
    }
}
