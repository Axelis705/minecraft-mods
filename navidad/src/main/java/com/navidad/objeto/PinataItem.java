package com.navidad.objeto;

import com.navidad.entidad.Pinata;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** La piñata para colgar: clic derecho debajo de un techo (o sobre cualquier bloque: queda flotando a la altura de la cabeza). */
public class PinataItem extends Item {
    public PinataItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext c) {
        Level l = c.getLevel();
        if (l.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos b = c.getClickedPos();
        double x = b.getX() + 0.5, z = b.getZ() + 0.5, y;
        if (c.getClickedFace() == Direction.DOWN) {
            y = b.getY() - 3.2;
        } else {
            BlockPos r = b.relative(c.getClickedFace());
            x = r.getX() + 0.5;
            z = r.getZ() + 0.5;
            y = r.getY() + (c.getClickedFace() == Direction.UP ? 0.3 : -1.0);
        }
        Pinata p = ModRegistry.PINATA_ENT.get().create(l);
        if (p == null) {
            return InteractionResult.FAIL;
        }
        p.moveTo(x, y, z, c.getRotation(), 0);
        l.addFreshEntity(p);
        l.playSound(null, b, ModRegistry.CANCION_PINATA.get(), SoundSource.RECORDS, 1.2F, 1F);
        if (c.getPlayer() != null && !c.getPlayer().getAbilities().instabuild) {
            c.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.translatable("navidad.pinata.ayuda").withStyle(ChatFormatting.GRAY));
    }
}
