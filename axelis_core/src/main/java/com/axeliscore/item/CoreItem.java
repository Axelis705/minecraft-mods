package com.axeliscore.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/**
 * Objeto base de los mods del Core: la ayuda al pasar el ratón y el clic derecho se escriben una sola vez y funcionan igual en
 * todas las versiones (cada versión de Minecraft cambió cómo se llaman).
 * <pre>
 *     public class LibroItem extends CoreItem {
 *         protected void tooltip(ItemStack st, Consumer&lt;Component&gt; add) { add.accept(Component.translatable("...")); }
 *         protected boolean onUse(Level level, Player p, InteractionHand hand) { ...; return true; }
 *     }
 * </pre>
 */
public class CoreItem extends Item {
    public CoreItem(Properties props) {
        super(props);
    }

    private net.minecraft.resources.ResourceLocation geo3d, tex3d;

    /**
     * Se dibuja con un modelo de Blockbench (geo.json; en la carpeta del estudio también .bbmodel) en la mano, en el suelo, en los marcos
     * y en el inventario. Recursos: en 1.20.1 y 1.21.1 el modelo del objeto es {@code {"parent": "axelis_core:item/modelo_3d"}}; en 26.x
     * {@code assets/mod/items/nombre.json} usa {@code {"type": "minecraft:special", "base": "axelis_core:item/modelo_3d_base", "model":
     * {"type": "axelis_core:geo", "geo": "...", "textura": "..."}}}. Ver {@link com.axeliscore.item.client.ObjetoModelo}.
     */
    public CoreItem modelo3d(net.minecraft.resources.ResourceLocation geo, net.minecraft.resources.ResourceLocation textura) {
        this.geo3d = geo;
        this.tex3d = textura;
        return this;
    }

    /** El modelo 3D (null si se dibuja normal). */
    public net.minecraft.resources.ResourceLocation geo3d() {
        return geo3d;
    }

    public net.minecraft.resources.ResourceLocation textura3d() {
        return tex3d;
    }

    //#if MC < 26.1
    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c) {
        if (geo3d != null) {
            c.accept(com.axeliscore.item.client.ObjetoModelo.extension());
        }
    }
    //#endif

    /**
     * Cuadro de información al pasar el ratón (en cualquier pantalla). null = la ayuda normal de Minecraft con {@link #tooltip}.
     * {@code detail} es true con Shift apretado.
     */
    public com.axeliscore.ui.InfoCard card(ItemStack stack, boolean detail) {
        return null;
    }

    /** Líneas de ayuda debajo del nombre. */
    protected void tooltip(ItemStack stack, Consumer<Component> add) {
    }

    /** Clic derecho con el objeto (en el cliente y en el servidor). true = se usó (el brazo se mueve y no hace lo normal). */
    protected boolean onUse(Level level, Player player, InteractionHand hand) {
        return false;
    }

    //#if MC >= 1.21.2
    //$$ @Override
    //$$ public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //$$     return onUse(level, player, hand) ? InteractionResult.SUCCESS : super.use(level, player, hand);
    //$$ }
    //#else
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (onUse(level, player, hand)) {
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
        }
        return super.use(level, player, hand);
    }
    //#endif

    //#if MC >= 1.21.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
    //$$         Consumer<Component> add, net.minecraft.world.item.TooltipFlag flag) {
    //$$     tooltip(stack, add);
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
    //$$     tooltip(stack, tip::add);
    //$$ }
    //#else
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip(stack, tip::add);
    }
    //#endif
}
