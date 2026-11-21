package com.mundosdeaxelis.npc;

import java.util.List;
import javax.annotation.Nullable;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Habitante;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Semilla de casa (etapa 257, bloque desde la etapa 260): te la da un habitante al rescatarlo. Se pone en el suelo como un bloque,
 * cerca de tu hogar: sale un holograma con su casa entera y su tamaño, para ver si cabe antes de construir. Con clic derecho en ella
 * la casa crece (con la puerta mirando hacia donde estabas al ponerla) y el habitante se muda.
 */
public class SemillaCasaItem extends BlockItem {
    public SemillaCasaItem(Block block) {
        super(block, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        NpcKinds.Kind k = NpcKinds.get(NpcService.KitKind.of(stack));
        return k == null ? super.getName(stack) : Component.translatable("item.mundos_de_axelis.semilla_casa.de", Component.translatable(k.nameKey()));
    }

    @Override
    public String getDescriptionId() {
        return "item.mundos_de_axelis.semilla_casa";
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        if (!level.isClientSide && level instanceof ServerLevel sl) {
            String kind = NpcService.KitKind.of(ctx.getItemInHand());
            String err = null;
            if (NpcKinds.get(kind) == null) {
                err = "message.mundos_de_axelis.casa.unknown";
            } else if (sl.dimension().location().getNamespace().equals(MundosDeAxelis.MODID)) {
                err = "message.mundos_de_axelis.casa.dimension";
            } else {
                int st = Vecinos.state(sl.getServer(), kind);
                if (st == Habitante.PRESO) {
                    err = "message.mundos_de_axelis.casa.preso";
                } else if (st == Habitante.VECINO) {
                    err = "message.mundos_de_axelis.casa.ya_tiene";
                }
            }
            if (err != null) {
                if (ctx.getPlayer() != null) {
                    ctx.getPlayer().displayClientMessage(Component.translatable(err).withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.FAIL;
            }
        }
        return super.place(ctx);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.semilla_casa").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.semilla_casa.use").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.semilla_casa.grow").withStyle(ChatFormatting.AQUA));
    }
}
