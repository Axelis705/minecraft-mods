package com.mundosdeaxelis.item;

import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Imán de estrellas: atrae hacia ti todo lo que haya tirado y la experiencia cercana. */
public class StarMagnetItem extends Item {
    private static final double RADIUS = 14.0;

    public StarMagnetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel sl = sp.serverLevel();
        sp.getCooldowns().addCooldown(this, 80);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0x5AA0FF, 36);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.8F);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);

        // Ondas que se cierran hacia el jugador, y durante 2 segundos todo se desliza hacia él.
        for (int i = 0; i < 40; i++) {
            final int k = i;
            Scheduler.runLater(i + 1, () -> {
                if (!sp.isAlive() || sp.hasDisconnected()) {
                    return;
                }
                ServerLevel w = sp.serverLevel();
                if (k % 8 == 0) {
                    double r = RADIUS * (1.0 - k / 44.0);
                    Fx.ring(w, Fx.spark(0x9CD0FF, 1.0F), sp.getX(), sp.getY() + 0.2, sp.getZ(), r, 24, k * 0.2);
                    Fx.ring(w, Fx.dust(0x5AA0FF, 1.0F), sp.getX(), sp.getY() + 0.5, sp.getZ(), r * 0.8, 24, -k * 0.2);
                }
                Vec3 target = sp.position().add(0, 0.6, 0);
                AABB box = sp.getBoundingBox().inflate(RADIUS);
                for (Entity e : w.getEntities((Entity) null, box, ent -> ent instanceof ItemEntity || ent instanceof ExperienceOrb)) {
                    Vec3 d = target.subtract(e.position());
                    double dist = d.length();
                    if (dist < 0.6) {
                        continue;
                    }
                    Vec3 pull = d.scale(1.0 / dist).scale(Math.min(0.9, 0.25 + 0.05 * (RADIUS - dist) + 0.2));
                    e.setDeltaMovement(pull);
                    e.hurtMarked = true;
                    if (k % 2 == 0) {
                        w.sendParticles(Fx.spark(0xBFE0FF, 0.7F), e.getX(), e.getY() + 0.3, e.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
                    }
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "star_magnet", 2);
    }
}
