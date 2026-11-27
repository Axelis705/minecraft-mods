package com.navidad.objeto;

import com.navidad.entidad.GanchoCadena;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * La cadena del Krampus (de su botín): pega como espada pesada y, con clic derecho, avienta el gancho. Si engancha a una criatura la
 * jala hacia ti; si es muy pesada (un jefe, un gólem) o si lo clavas en un bloque, eres tú el que sale volando hacia allá (sin daño
 * por caída; agáchate para soltarte).
 */
public class CadenaKrampus extends SwordItem {
    public CadenaKrampus(Properties p) {
        super(Tiers.IRON, 4, -2.8F, p.durability(420));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        ItemStack st = p.getItemInHand(h);
        if (!l.isClientSide) {
            Vec3 mira = p.getLookAngle();
            Vec3 desde = p.getEyePosition().add(mira.scale(0.6)).add(0, -0.25, 0);
            GanchoCadena.lanzar(l, p, desde, mira, 1.8F, 24);
            st.hurtAndBreak(1, p, q -> q.broadcastBreakEvent(h));
        }
        p.getCooldowns().addCooldown(this, 24);
        p.swing(h, true);
        return InteractionResultHolder.sidedSuccess(st, l.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack st, Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("item.navidad.cadena_krampus.ayuda").withStyle(ChatFormatting.GRAY));
    }
}
