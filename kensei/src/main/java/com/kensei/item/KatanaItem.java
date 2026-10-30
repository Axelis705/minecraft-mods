package com.kensei.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ToolActions;

/**
 * Katana: se puede sostener el clic derecho para bloquear. Si el golpe llega en los primeros ticks del bloqueo es un parry perfecto
 * (la lógica está en {@code combat.CombatEvents}). No hace barrido: el combate es de uno contra uno.
 */
public class KatanaItem extends SwordItem {
    /** Espera tras soltar el bloqueo, para que no se pueda repetir el parry apretando el botón sin parar. */
    private static final int GUARD_COOLDOWN = 5;

    public KatanaItem(Tier tier, int damage, float speed, Properties props) {
        super(tier, damage, speed, props);
    }

    /** Sin brillo de encantamiento permanente: teñía de morado el acero y la vaina. Solo brilla si de verdad está encantada. */

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.kensei.katana.lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.kensei.katana.controls1").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.kensei.katana.controls2").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.kensei.katana.controls3").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            player.getCooldowns().addCooldown(this, GUARD_COOLDOWN);
        }
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        if (action == ToolActions.SWORD_SWEEP) {
            return false;
        }
        return super.canPerformAction(stack, action);
    }

    /** Postura de brazo propia de la katana (solo en el cliente). */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return com.kensei.client.KatanaPoses.get();
            }

            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.player.LocalPlayer player,
                    net.minecraft.world.entity.HumanoidArm arm, ItemStack stack, float partialTick, float equipProcess, float swingProcess) {
                return com.kensei.client.FirstPersonAnim.apply(poseStack, player, arm, partialTick, equipProcess, swingProcess);
            }
        });
    }
}
