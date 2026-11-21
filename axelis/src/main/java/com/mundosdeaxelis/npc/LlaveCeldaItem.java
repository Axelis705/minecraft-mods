package com.mundosdeaxelis.npc;

import com.mundosdeaxelis.entity.Habitante;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Llave de la celda (etapa 257): la lleva el Raizal que guarda el altar del Santuario del Bosque. Se usa en la puerta de hierro de
 * la celda (o directamente en la persona presa, a través de los barrotes) y la libera. Se gasta.
 */
public class LlaveCeldaItem extends Item {
    public LlaveCeldaItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockState st = level.getBlockState(ctx.getClickedPos());
        if (!(st.getBlock() instanceof DoorBlock)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(ctx.getPlayer() instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        BlockPos door = st.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? ctx.getClickedPos().below() : ctx.getClickedPos();
        Habitante h = NpcService.prisonerNear((ServerLevel) level, door, 7.0D);
        if (h == null) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.llave.nadie"), true);
            return InteractionResult.FAIL;
        }
        // etapa 269: las celdas de un tier que aún no te toca están selladas (la llave no se gasta)
        int tier = com.mundosdeaxelis.world.Sellos.npcTier(h.kindId());
        if (!com.mundosdeaxelis.world.Sellos.canLoot(sp, tier)) {
            com.mundosdeaxelis.world.Sellos.deny(sp, door, tier, "celda");
            return InteractionResult.FAIL;
        }
        if (!sp.getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        NpcService.free(sp, h, door, NpcService.R_RESCUED);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.llave_celda").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.llave_celda.use").withStyle(ChatFormatting.AQUA));
    }
}
