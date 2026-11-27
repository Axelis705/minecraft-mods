package com.navidad.objeto;

import com.axeliscore.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * El regalo misterioso: mantén clic derecho para desenvolverlo (la caja tiembla cada vez más y se oye el papel); al terminar revienta
 * en confeti y te dice qué te tocó (común, raro o épico).
 */
public class RegaloMisterioso extends Item {
    public static final int DURA = 22;

    public RegaloMisterioso(Properties p) {
        super(p);
    }

    @Override
    public int getUseDuration(ItemStack s) {
        return DURA;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack s) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        p.startUsingItem(h);
        return InteractionResultHolder.consume(p.getItemInHand(h));
    }

    @Override
    public void onUseTick(Level l, LivingEntity e, ItemStack s, int queda) {
        int t = DURA - queda;
        if (!l.isClientSide && (t == 2 || t == 8 || t == 13 || t == 17)) {
            l.playSound(null, e.blockPosition(), t < 10 ? SoundEvents.BOOK_PAGE_TURN : SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 0.8F,
                    0.7F + t * 0.04F);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack s, Level l, LivingEntity e) {
        if (!l.isClientSide && e instanceof ServerPlayer sp) {
            Premios.Rareza rz = Premios.rareza(l.random);
            ItemStack premio = Premios.sacar(rz, l.random);
            l.playSound(null, sp.blockPosition(), com.navidad.registry.ModRegistry.REGALO_ABRE.get(), SoundSource.PLAYERS, 1F, 1F);
            Premios.dar(sp, premio, rz, "navidad.regalo.te_toco");
            int r = rz == Premios.Rareza.EPICO ? 2 : rz == Premios.Rareza.RARO ? 1 : 0;
            Fx.toast(sp, premio, Component.translatable("navidad.caja.toast." + r), Component.literal(premio.getCount() + "× ")
                    .append(premio.getHoverName()), r == 2 ? 0xD070FF : r == 1 ? 0x60E0FF : 0xFFD84A);
            Vec3 f = Vec3.directionFromRotation(0, sp.getYRot()).scale(1.2);
            Premios.confeti(sp.serverLevel(), sp.getX() + f.x, sp.getEyeY() - 0.3, sp.getZ() + f.z, r > 0);
            com.navidad.libro.Libro.evento(sp, "regalo_abierto", 1);
            if (!sp.getAbilities().instabuild) {
                s.shrink(1);
            }
            sp.getCooldowns().addCooldown(this, 8);
        }
        return s;
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.translatable("navidad.regalo.ayuda").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c) {
        c.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(com.mojang.blaze3d.vertex.PoseStack ps, net.minecraft.client.player.LocalPlayer p,
                    HumanoidArm arm, ItemStack st, float pt, float equip, float swing) {
                int lado = arm == HumanoidArm.RIGHT ? 1 : -1;
                ps.translate(lado * 0.56F, -0.52F + equip * -0.6F, -0.72F);
                if (p.isUsingItem() && p.getUseItem() == st) {
                    // lo acerca al frente y tiembla cada vez más fuerte
                    float k = Mth.clamp((p.getTicksUsingItem() + pt) / DURA, 0, 1);
                    ps.translate(-lado * 0.22F * Math.min(1, k * 3), 0.1F * Math.min(1, k * 3), 0.1F * Math.min(1, k * 3));
                    float tiembla = k * k * 9F;
                    float tt = (p.tickCount + pt) * 2.6F;
                    ps.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(Mth.sin(tt) * tiembla));
                    ps.mulPose(com.mojang.math.Axis.XP.rotationDegrees(Mth.cos(tt * 1.3F) * tiembla * 0.6F));
                    float s = 1 + 0.08F * k * Mth.sin(tt * 0.5F);
                    ps.scale(s, s, s);
                }
                return true;
            }
        });
    }
}
