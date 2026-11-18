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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Linterna de luciérnagas: en la mano te rodean luciérnagas y ves en la oscuridad.
 * Clic derecho la enciende o la apaga.
 */
public class FireflyLanternItem extends Item {
    public FireflyLanternItem(Properties properties) {
        super(properties);
    }

    public static boolean isOn(ItemStack stack) {
        return !stack.hasTag() || !stack.getTag().getBoolean("Off");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        boolean nowOn = !isOn(stack);
        if (nowOn) {
            stack.removeTagKey("Off");
        } else {
            stack.getOrCreateTag().putBoolean("Off", true);
        }
        ServerLevel sl = sp.serverLevel();
        sp.getCooldowns().addCooldown(this, 10);
        sp.displayClientMessage(Component.translatable(nowOn ? "message.mundos_de_axelis.lantern_on"
                : "message.mundos_de_axelis.lantern_off"), true);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), nowOn ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.CANDLE_EXTINGUISH,
                SoundSource.PLAYERS, 0.9F, nowOn ? 1.6F : 1.0F);
        if (nowOn) {
            for (int i = 0; i < 16; i++) {
                double a = sl.getRandom().nextDouble() * Math.PI * 2.0;
                sl.sendParticles(Fx.dust(0xEAFF8A, 0.8F), sp.getX(), sp.getY() + 1.2, sp.getZ(), 0,
                        Math.cos(a) * 0.12, 0.03 + sl.getRandom().nextDouble() * 0.05, Math.sin(a) * 0.12, 1.0);
            }
            Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xD8FF70, 24);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer sp) || !isOn(stack)) {
            return;
        }
        if (!selected && sp.getOffhandItem() != stack) {
            return;
        }
        long t = level.getGameTime();
        ServerLevel sl = sp.serverLevel();
        if (t % 10 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 14, 0, true, false, false));
        }
        if (t % 3 == 0) {
            // Luciérnagas que orbitan despacio y titilan.
            for (int i = 0; i < 4; i++) {
                double a = t * 0.045 + i * Math.PI / 2.0 + Math.sin(t * 0.03 + i) * 0.6;
                double r = 1.1 + 0.45 * Math.sin(t * 0.021 + i * 1.7);
                double y = sp.getY() + 0.8 + 0.5 * Math.sin(t * 0.037 + i * 2.1) + i * 0.12;
                sl.sendParticles(Fx.dust(0xEAFF8A, 0.55F), sp.getX() + Math.cos(a) * r, y, sp.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
            if (sl.getRandom().nextInt(4) == 0) {
                double a = sl.getRandom().nextDouble() * Math.PI * 2.0;
                sl.sendParticles(Fx.spark(0xF4FFB0, 0.5F), sp.getX() + Math.cos(a) * 1.6, sp.getY() + 0.6 + sl.getRandom().nextDouble() * 1.2,
                        sp.getZ() + Math.sin(a) * 1.6, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "firefly_lantern", 2);
    }
}
