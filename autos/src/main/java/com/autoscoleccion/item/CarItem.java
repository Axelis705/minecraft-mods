package com.autoscoleccion.item;

import com.autoscoleccion.client.CarItemRenderer;
import com.autoscoleccion.game.Boxes;
import com.autoscoleccion.game.CarDef;
import com.autoscoleccion.game.Cars;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

/** Un cochecito de colección. Se dibuja en 3D (en la mano, en el inventario y tirado) con el mismo modelo que la vitrina. */
public class CarItem extends Item {
    public final int car;

    public CarItem(int car, Properties props) {
        super(props);
        this.car = car;
    }

    public CarDef def() {
        return Cars.get(car);
    }

    @Override
    public Component getName(ItemStack stack) {
        CarDef c = def();
        return Component.literal(c.name).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(c.rarity.rgb)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        CarDef c = def();
        tip.add(Component.literal("★".repeat(c.rarity.stars) + "☆".repeat(5 - c.rarity.stars) + "  " + c.rarity.label)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(c.rarity.rgb))));
        tip.add(Component.literal("Nº " + c.number + "/" + Cars.COUNT + " · Serie " + Boxes.NAME[c.line]).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal(c.blurb).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return CarItemRenderer.get();
            }
        });
    }
}
