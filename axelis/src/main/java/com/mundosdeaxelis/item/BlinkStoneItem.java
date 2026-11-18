package com.mundosdeaxelis.item;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import com.mundosdeaxelis.network.ScreenFxPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Piedra de Destello: te teletransporta una distancia corta hacia donde miras, dejando una estela de luz. */
public class BlinkStoneItem extends Item {
    private static final double DIST = 11.0;

    public BlinkStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel sl = sp.serverLevel();
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        Vec3 end = eye.add(look.scale(DIST));
        var hit = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        double reach = hit.getType() == HitResult.Type.MISS ? DIST : Math.max(0.0, eye.distanceTo(hit.getLocation()) - 0.6);
        Vec3 from = sp.position();
        Vec3 dest = null;
        for (double d = reach; d >= 1.5; d -= 0.5) {
            Vec3 p = eye.add(look.scale(d)).subtract(0.0, sp.getEyeHeight(), 0.0);
            if (sl.noCollision(sp, sp.getBoundingBox().move(p.x - sp.getX(), p.y - sp.getY(), p.z - sp.getZ()))) {
                dest = p;
                break;
            }
        }
        if (dest == null) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.blink_blocked"), true);
            sp.getCooldowns().addCooldown(this, 10);
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 0.8F, 0.6F);
            return InteractionResultHolder.fail(stack);
        }
        sp.getCooldowns().addCooldown(this, 20 * 3);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        // Estela entre el origen y el destino, con colores de arcoíris.
        Vec3 delta = dest.subtract(from);
        int steps = (int) Math.max(6.0, delta.length() * 3.0);
        for (int i = 0; i <= steps; i++) {
            float u = i / (float) steps;
            Vec3 p = from.add(delta.scale(u));
            sl.sendParticles(Fx.dust(Fx.rainbow(u), 1.0F), p.x, p.y + 1.0, p.z, 1, 0.1, 0.4, 0.1, 0.0);
            if (i % 3 == 0) {
                sl.sendParticles(Fx.spark(0xFFFFFF, 0.7F), p.x, p.y + 1.0, p.z, 1, 0.15, 0.4, 0.15, 0.0);
            }
        }
        Fx.ring(sl, Fx.spark(0xE9C8FF, 1.0F), from.x, from.y + 0.2, from.z, 0.9, 14, 0);
        Fx.sound(sl, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.6F);
        sp.teleportTo(dest.x, dest.y, dest.z);
        sp.fallDistance = 0.0F;
        Fx.ring(sl, Fx.spark(0xE9C8FF, 1.0F), dest.x, dest.y + 0.2, dest.z, 0.9, 14, 0);
        Fx.sound(sl, dest.x, dest.y, dest.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.8F);
        Fx.screen(sp, ScreenFxPacket.SPEED, 0xFFFFFF, 12);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "blink_stone", 2);
    }
}
