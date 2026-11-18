package com.mundosdeaxelis.item;

import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Relicario del corazón: mantenlo y te lleva a casa (tu cama, o el punto de inicio del mundo). */
public class HeartLocketItem extends Item {
    private static final int CHARGE = 70;

    public HeartLocketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS, 0.8F, 1.6F);
            if (player instanceof ServerPlayer sp) {
                Fx.screen(sp, ScreenFxPacket.HEARTS, 0xFF6FA8, CHARGE + 6);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return CHARGE;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    /** Mientras lo mantienes: un remolino de corazones que se aprieta alrededor de ti. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        int t = CHARGE - remaining;
        double r = 1.4 - 0.9 * (t / (double) CHARGE);
        Fx.helix(sl, Fx.heart(0xFF6FA8, 0.9F), entity.getX(), entity.getY() + 0.1, entity.getZ(), r, 2.0, 3, t * 0.25);
        if (t % 4 == 0) {
            sl.sendParticles(Fx.spark(0xFFD0E4, 0.9F), entity.getX(), entity.getY() + 1.0, entity.getZ(), 2, r * 0.6, 0.8, r * 0.6, 0.0);
        }
        if (t % 6 == 0) {
            Fx.ring(sl, Fx.dust(0xFF5A9E, 1.1F), entity.getX(), entity.getY() + 0.1 + (t % 24) / 12.0, entity.getZ(), r, 14, t * 0.3);
            sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.NOTE_BLOCK_CHIME.get(),
                    SoundSource.PLAYERS, 0.6F, 0.7F + t / (float) CHARGE);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel) || !(entity instanceof ServerPlayer sp)) {
            return stack;
        }
        MinecraftServer server = sp.getServer();
        if (server == null) {
            return stack;
        }
        ServerLevel from = sp.serverLevel();
        burst(from, sp.getX(), sp.getY(), sp.getZ());

        ServerLevel dest = server.getLevel(sp.getRespawnDimension());
        BlockPos spot = sp.getRespawnPosition();
        if (dest == null || spot == null) {
            dest = server.overworld();
            spot = dest.getSharedSpawnPos();
        }
        sp.teleportTo(dest, spot.getX() + 0.5, spot.getY() + 1.0, spot.getZ() + 0.5, sp.getYRot(), sp.getXRot());
        sp.fallDistance = 0.0F;
        burst(dest, sp.getX(), sp.getY(), sp.getZ());
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.locket_home"), true);
        Fx.screen(sp, ScreenFxPacket.FLASH, 0xFFC2D6, 26);
        sp.getCooldowns().addCooldown(this, 20 * 45);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(net.minecraft.world.InteractionHand.MAIN_HAND));
        return stack;
    }

    private static void burst(ServerLevel l, double x, double y, double z) {
        l.sendParticles(Fx.heart(0xFF6FA8, 1.2F), x, y + 1.0, z, 16, 0.7, 0.8, 0.7, 0.05);
        l.sendParticles(Fx.spark(0xFFFFFF, 1.3F), x, y + 1.0, z, 20, 0.5, 0.9, 0.5, 0.0);
        Fx.rainbowRing(l, x, y + 0.2, z, 1.3, 30, 0);
        Fx.confetti(l, x, y + 0.4, z, 16, 1.4);
        l.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.5F);
        l.playSound(null, x, y, z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "heart_locket", 2);
    }
}
