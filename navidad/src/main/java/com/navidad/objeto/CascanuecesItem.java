package com.navidad.objeto;

import com.navidad.entidad.Cascanueces;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** La figura del Cascanueces guardián: clic derecho en el piso para ponerlo de guardia ahí. */
public class CascanuecesItem extends Item {
    public CascanuecesItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext c) {
        Level l = c.getLevel();
        if (!(l instanceof ServerLevel sl)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = c.getClickedPos().relative(c.getClickedFace());
        if (!l.getBlockState(pos).canBeReplaced() || !l.getBlockState(pos.above()).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        Cascanueces g = ModRegistry.CASCANUECES.get().create(sl);
        Player p = c.getPlayer();
        if (g == null || p == null) {
            return InteractionResult.FAIL;
        }
        float yaw = p.getYRot() + 180F;
        g.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        g.setYHeadRot(yaw);
        g.setYBodyRot(yaw);
        g.ponerEn(p, pos);
        sl.addFreshEntity(g);
        g.gesto("salute");
        sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 12, 0.3, 0.6, 0.3, 0.02);
        l.playSound(null, pos, SoundEvents.ARMOR_STAND_PLACE, SoundSource.NEUTRAL, 1F, 1.2F);
        l.playSound(null, pos, ModRegistry.CASCANUECES_CLAC.get(), SoundSource.NEUTRAL, 1F, 1.1F);
        p.displayClientMessage(Component.translatable("navidad.cascanueces.puesto").withStyle(ChatFormatting.GOLD), true);
        if (!p.getAbilities().instabuild) {
            c.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.translatable("navidad.cascanueces.ayuda1").withStyle(ChatFormatting.GRAY));
        t.add(Component.translatable("navidad.cascanueces.ayuda2").withStyle(ChatFormatting.GRAY));
    }
}
