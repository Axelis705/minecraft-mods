package com.mundosdeaxelis.npc;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Semilla de casa (etapa 257): te la da un habitante al rescatarlo. Usada sobre el suelo hace crecer su casa delante de ti (con la
 * puerta mirándote) y el habitante se muda a ella. Necesita un sitio despejado y no muy desigual de unos 9 × 10.
 */
public class SemillaCasaItem extends Item {
    public SemillaCasaItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
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
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(ctx.getPlayer() instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        BlockPos base = ctx.getClickedFace() == Direction.UP ? ctx.getClickedPos() : ctx.getClickedPos().relative(ctx.getClickedFace()).below();
        BlockState bs = level.getBlockState(base);
        if (bs.canBeReplaced() && bs.getFluidState().isEmpty()) {
            base = base.below(); // pasto alto, flores, nieve: la casa se apoya en el suelo de debajo
        }
        String kind = NpcService.KitKind.of(ctx.getItemInHand());
        String err = NpcService.plantHouse(sp.serverLevel(), sp, kind, base.above(), sp.getDirection());
        if (err != null) {
            sp.displayClientMessage(Component.translatable(err).withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (!sp.getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.casa.lista", Component.translatable(NpcKinds.get(kind).nameKey()))
                .withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.semilla_casa").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.semilla_casa.use").withStyle(ChatFormatting.AQUA));
    }
}
