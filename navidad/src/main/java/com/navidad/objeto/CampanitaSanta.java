package com.navidad.objeto;

import com.navidad.Navidad;
import com.navidad.entidad.Krampus;
import com.navidad.entidad.Krampusillo;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * La campanita dorada de Santa: su sonido claro no lo aguantan los del Krampus. Al tocarla, el Krampus que esté cerca (16 bloques) se
 * queda a la vista un rato, brillando, y se aturde; los krampusillos (12 bloques) sueltan lo que se robaron y huyen. Hay que esperar 15
 * segundos para volver a tocarla.
 */
public class CampanitaSanta extends Item {
    public static int toques;

    public CampanitaSanta(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        ItemStack st = p.getItemInHand(h);
        p.getCooldowns().addCooldown(this, 300);
        if (l instanceof ServerLevel sl) {
            tocar(sl, p);
        }
        p.swing(h, true);
        return InteractionResultHolder.sidedSuccess(st, l.isClientSide);
    }

    /** (Servidor) Suena: revela al Krampus y espanta a los krampusillos. */
    public static void tocar(ServerLevel sl, Player p) {
        toques++;
        sl.playSound(null, p.blockPosition(), ModRegistry.CAMPANITA.get(), SoundSource.PLAYERS, 2F, 1F);
        com.axeliscore.fx.Efectos.lanzar(sl, Navidad.MODID + ":campanita", p.position().add(0, 1.2, 0));
        int k = 0, d = 0;
        for (Krampus kr : sl.getEntitiesOfClass(Krampus.class, p.getBoundingBox().inflate(16))) {
            kr.revelar(160);
            k++;
        }
        for (Krampusillo ki : sl.getEntitiesOfClass(Krampusillo.class, p.getBoundingBox().inflate(12))) {
            ki.espantar();
            d++;
        }
        if (k > 0) {
            p.displayClientMessage(Component.translatable("item.navidad.campanita_santa.krampus").withStyle(ChatFormatting.GOLD), true);
        } else if (d > 0) {
            p.displayClientMessage(Component.translatable("item.navidad.campanita_santa.diablitos").withStyle(ChatFormatting.GOLD), true);
        }
        Navidad.LOGGER.info("NAVIDAD_CAMPANITA {} krampus={} krampusillos={}", p.getName().getString(), k, d);
    }

    @Override
    public void appendHoverText(ItemStack st, Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("item.navidad.campanita_santa.ayuda").withStyle(ChatFormatting.GRAY));
    }
}
