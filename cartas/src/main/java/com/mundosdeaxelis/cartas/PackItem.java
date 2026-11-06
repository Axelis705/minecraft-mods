package com.mundosdeaxelis.cartas;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Un sobre de cartas: clic derecho para abrirlo. Las cartas se entregan al instante; la animacion es solo vista. */
public class PackItem extends Item {
    public final PackDef def;

    public PackItem(PackDef def) {
        super(new Item.Properties().stacksTo(16));
        this.def = def;
    }

    @Override
    public boolean isFoil(ItemStack s) {
        return def.min.atLeast(CardRarity.EPICA);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            open(sp, def, new Random(sp.getRandom().nextLong()));
            if (!sp.getAbilities().instabuild) {
                st.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    /** Sortea, entrega y manda la animacion al cliente. */
    public static List<PackRoller.Pull> open(ServerPlayer sp, PackDef def, Random rnd) {
        List<PackRoller.Pull> pulls = PackRoller.roll(def, rnd);
        AlbumCap cap = AlbumCaps.of(sp);
        List<String> ids = new ArrayList<>();
        List<Boolean> holo = new ArrayList<>();
        List<Boolean> isNew = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (PackRoller.Pull p : pulls) {
            String key = p.card().id + (p.holo() ? "#h" : "");
            boolean nuevo = !cap.has(p.card().id, p.holo()) && CartasItems.countInInventory(sp, p.card(), p.holo()) == 0 && !seen.contains(key);
            seen.add(key);
            ids.add(p.card().id);
            holo.add(p.holo());
            isNew.add(nuevo);
            ItemStack stack = CartasItems.stack(p.card(), p.holo(), 1);
            if (!sp.getInventory().add(stack)) {
                sp.drop(stack, false);
            }
        }
        CartasNet.toPlayer(sp, new CartasNet.PackOpen(def.id, ids, holo, isNew));
        return pulls;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(def.count + " cartas").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Al menos una " + def.min.label.toLowerCase() + " o mejor").withStyle(Style.EMPTY.withColor(def.min.color)));
        tip.add(Component.literal(String.format("Holográfica: %.0f %% por carta", def.holo * 100)).withStyle(Style.EMPTY.withColor(0x7FE8FF)));
        tip.add(Component.literal("Clic derecho para abrir").withStyle(ChatFormatting.DARK_GRAY));
    }
}
