package com.coleccionista.prueba;

import com.coleccionista.Coleccionista;
import com.coleccionista.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/**
 * La llave del grillete de una pieza (Encadenados). Cada llave abre un solo grillete: el de su dueño ("Llave · Pieza 07"). Se usa con
 * clic derecho; si es de tu pareja, lánzasela con la tecla Q y vuela hacia ella.
 */
public class LlaveGrillete extends Item {
    private static final String MOD = Coleccionista.MODID;

    public LlaveGrillete(Properties p) {
        super(p);
    }

    public static ItemStack crear(UUID dueno, int numero) {
        ItemStack st = new ItemStack(ModRegistry.LLAVE.get());
        CompoundTag t = st.getOrCreateTag();
        t.putUUID("dueno", dueno);
        t.putInt("pieza", numero);
        return st;
    }

    public static UUID dueno(ItemStack st) {
        CompoundTag t = st.getTag();
        return t != null && t.hasUUID("dueno") ? t.getUUID("dueno") : null;
    }

    public static int pieza(ItemStack st) {
        CompoundTag t = st.getTag();
        return t == null ? 0 : t.getInt("pieza");
    }

    @Override
    public Component getName(ItemStack st) {
        int n = pieza(st);
        return n <= 0 ? super.getName(st) : Component.translatable(MOD + ".item.llave_de", String.format("%02d", n));
    }

    @Override
    public void appendHoverText(ItemStack st, Level level, List<Component> l, TooltipFlag f) {
        l.add(Component.translatable(MOD + ".item.llave_ayuda").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        if (!level.isClientSide && p instanceof ServerPlayer sp && Encadenados.usarLlave(sp, st)) {
            st.shrink(1);
            return InteractionResultHolder.consume(st);
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }
}
