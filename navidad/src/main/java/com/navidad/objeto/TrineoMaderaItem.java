package com.navidad.objeto;

import com.navidad.entidad.TrineoMadera;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** El trineo de madera en la mano: clic derecho en el suelo para ponerlo (mirando hacia donde miras). */
public class TrineoMaderaItem extends Item {
    public TrineoMaderaItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext c) {
        Level l = c.getLevel();
        BlockPos bp = c.getClickedPos();
        if (!l.getBlockState(bp).canBeReplaced()) {
            bp = bp.relative(c.getClickedFace());
        }
        Vec3 pos = Vec3.atBottomCenterOf(bp);
        TrineoMadera t = new TrineoMadera(ModRegistry.TRINEO_MADERA.get(), l);
        t.moveTo(pos.x, pos.y, pos.z, c.getRotation(), 0F);
        if (!l.noCollision(t, t.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        if (!l.isClientSide) {
            l.addFreshEntity(t);
            l.playSound(null, bp, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1F, 0.9F);
            l.gameEvent(c.getPlayer(), GameEvent.ENTITY_PLACE, pos);
            Player p = c.getPlayer();
            if (p == null || !p.getAbilities().instabuild) {
                c.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        out.add(Component.translatable("navidad.trineo_madera.ayuda").withStyle(ChatFormatting.GRAY));
        out.add(Component.translatable("navidad.trineo_madera.ayuda2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
