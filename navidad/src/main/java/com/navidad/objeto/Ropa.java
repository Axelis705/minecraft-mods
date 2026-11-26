package com.navidad.objeto;

import com.axeliscore.item.CoreItem;
import com.axeliscore.plat.Plat;
import com.axeliscore.vestir.Vestuario;
import com.navidad.Navidad;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Ropa navideña: con clic derecho te la pones o te la quitas (se dibuja encima de tu skin con el vestuario del Core y la ven todos).
 * Solo un gorro y un suéter (o traje) a la vez: al ponerte otro del mismo grupo, el anterior se quita solo.
 */
public class Ropa extends CoreItem {
    /** Los grupos: lo que va en la cabeza y lo que va en el cuerpo. */
    public static final List<String> CABEZA = List.of("gorro_santa", "gorro_duende", "cuernos_reno");
    public static final List<String> CUERPO = List.of("sueter_reno", "sueter_pinos", "sueter_copos", "traje_santa");

    private final String prenda;

    public Ropa(Properties p, String prenda) {
        super(p);
        this.prenda = prenda;
    }

    public static ResourceLocation id(String prenda) {
        return new ResourceLocation(Navidad.MODID, prenda);
    }

    @Override
    protected void tooltip(ItemStack stack, Consumer<Component> add) {
        add.accept(Component.translatable("navidad.ropa.ayuda").withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected boolean onUse(Level level, Player p, InteractionHand hand) {
        if (p instanceof ServerPlayer sp) {
            alternar(Plat.server(sp), sp, prenda);
        }
        return true;
    }

    /** Se la pone (quitando las de su grupo) o se la quita si ya la tenía. Devuelve true si quedó puesta. */
    public static boolean alternar(MinecraftServer s, ServerPlayer sp, String prenda) {
        UUID u = sp.getUUID();
        ResourceLocation id = id(prenda);
        if (Vestuario.tiene(s, u, id)) {
            Vestuario.quitar(s, u, id);
            sp.displayClientMessage(Component.translatable("navidad.ropa.quitada", Component.translatable("item.navidad." + prenda)), true);
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.8F, 0.8F);
            return false;
        }
        poner(s, sp, prenda);
        sp.displayClientMessage(Component.translatable("navidad.ropa.puesta", Component.translatable("item.navidad." + prenda)), true);
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    public static void poner(MinecraftServer s, ServerPlayer sp, String prenda) {
        UUID u = sp.getUUID();
        List<String> grupo = CABEZA.contains(prenda) ? CABEZA : CUERPO;
        for (String otra : grupo) {
            if (!otra.equals(prenda) && Vestuario.tiene(s, u, id(otra))) {
                Vestuario.quitar(s, u, id(otra));
            }
        }
        // el traje de Santa trae su gorro
        if (prenda.equals("traje_santa")) {
            for (String otra : CABEZA) {
                if (Vestuario.tiene(s, u, id(otra))) {
                    Vestuario.quitar(s, u, id(otra));
                }
            }
        }
        Vestuario.poner(s, u, id(prenda), null);
    }
}
