package com.autoscoleccion.item;

import com.autoscoleccion.net.AutosNet;
import com.autoscoleccion.net.CollectionPacket;
import com.autoscoleccion.registry.ModSounds;
import com.autoscoleccion.server.AutosData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** El garaje: clic derecho abre tu colección (los que tienes a color y los que faltan como silueta). */
public class GarageBookItem extends Item {
    public GarageBookItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            level.playSound(null, sp.blockPosition(), ModSounds.get("garage"), SoundSource.PLAYERS, 0.8F, 1.0F);
            AutosNet.toPlayer(sp, new CollectionPacket(AutosData.counts(sp), true));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Clic derecho: ver tu colección de autos").withStyle(ChatFormatting.GRAY));
    }
}
