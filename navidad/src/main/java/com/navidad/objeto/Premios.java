package com.navidad.objeto;

import com.navidad.Navidad;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Los premios de los regalos misteriosos y del calendario de Adviento (con su rareza, su confeti y su sonido). */
public final class Premios {
    public enum Rareza {
        COMUN(ChatFormatting.WHITE), RARO(ChatFormatting.AQUA), EPICO(ChatFormatting.LIGHT_PURPLE);

        public final ChatFormatting color;

        Rareza(ChatFormatting c) {
            this.color = c;
        }
    }

    /** Un premio: el objeto (id), de cuántos a cuántos y qué tan seguido sale (peso). */
    public record Premio(String id, int min, int max, int peso) {
    }

    private static final List<Premio> COMUNES = List.of(
            new Premio("navidad:galleta_jengibre", 3, 8, 10), new Premio("navidad:baston_caramelo", 2, 5, 10),
            new Premio("navidad:chocolate_caliente", 1, 3, 8), new Premio("navidad:ponche", 1, 3, 8), new Premio("navidad:dulce", 6, 14, 8),
            new Premio("navidad:tamal", 2, 4, 6), new Premio("navidad:bunuelo", 2, 4, 6), new Premio("minecraft:snowball", 12, 16, 6),
            new Premio("navidad:esfera_roja", 2, 4, 5), new Premio("navidad:esfera_dorada", 2, 4, 5), new Premio("navidad:velas_navidad", 1, 2, 4),
            new Premio("navidad:regalo_rojo", 1, 2, 4), new Premio("navidad:luces_colgantes", 2, 4, 5), new Premio("minecraft:emerald", 2, 5, 6),
            new Premio("minecraft:gold_ingot", 2, 5, 5), new Premio("minecraft:cookie", 8, 16, 4), new Premio("navidad:nochebuena", 1, 2, 4));
    private static final List<Premio> RAROS = List.of(
            new Premio("minecraft:diamond", 1, 3, 8), new Premio("minecraft:golden_apple", 1, 3, 7), new Premio("minecraft:experience_bottle", 6, 12, 6),
            new Premio("navidad:arbol_navidad", 1, 1, 5), new Premio("navidad:pavo_navideno", 1, 2, 6), new Premio("navidad:pinata", 1, 1, 5),
            new Premio("navidad:muneco_nieve", 1, 1, 4), new Premio("navidad:trineo", 1, 1, 3), new Premio("navidad:gorro_santa", 1, 1, 5),
            new Premio("navidad:sueter_reno", 1, 1, 4), new Premio("navidad:sueter_pinos", 1, 1, 4), new Premio("navidad:disco_campanas", 1, 1, 3),
            new Premio("navidad:disco_navidad", 1, 1, 3), new Premio("minecraft:name_tag", 1, 2, 3));
    private static final List<Premio> EPICOS = List.of(
            new Premio("minecraft:enchanted_golden_apple", 1, 1, 6), new Premio("minecraft:totem_of_undying", 1, 1, 5),
            new Premio("minecraft:diamond", 5, 9, 6), new Premio("minecraft:netherite_scrap", 1, 2, 3), new Premio("navidad:arbol_dorado", 1, 1, 4),
            new Premio("navidad:disco_noche_paz", 1, 1, 4), new Premio("navidad:traje_santa", 1, 1, 3));

    private Premios() {
    }

    public static Rareza rareza(RandomSource r) {
        float x = r.nextFloat();
        return x < 0.08F ? Rareza.EPICO : x < 0.36F ? Rareza.RARO : Rareza.COMUN;
    }

    public static ItemStack sacar(Rareza rz, RandomSource r) {
        List<Premio> l = rz == Rareza.EPICO ? EPICOS : rz == Rareza.RARO ? RAROS : COMUNES;
        for (int intento = 0; intento < 8; intento++) {
            int total = l.stream().mapToInt(Premio::peso).sum();
            int k = r.nextInt(total);
            for (Premio p : l) {
                k -= p.peso();
                if (k < 0) {
                    ItemStack st = pila(p.id(), p.min() + r.nextInt(p.max() - p.min() + 1));
                    if (!st.isEmpty()) {
                        return st;
                    }
                    break;
                }
            }
        }
        return new ItemStack(Items.COOKIE, 8);
    }

    public static ItemStack pila(String id, int n) {
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return it == null || it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it, Math.max(1, Math.min(n, it.getMaxStackSize())));
    }

    /** Da el premio (o lo tira al piso si no cabe), con confeti y aviso. */
    public static void dar(ServerPlayer p, ItemStack st, Rareza rz, String clave) {
        ServerLevel l = p.serverLevel();
        Component nombre = Component.literal(st.getCount() + "× ").append(st.getHoverName()).withStyle(rz.color, ChatFormatting.BOLD);
        p.displayClientMessage(Component.translatable(clave, nombre), false);
        if (!p.getInventory().add(st.copy())) {
            p.drop(st.copy(), false);
        }
        confeti(l, p.getX(), p.getY() + 1.2, p.getZ(), rz == Rareza.EPICO);
        l.playSound(null, p.blockPosition(), rz == Rareza.EPICO ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, rz == Rareza.EPICO ? 0.8F : 0.6F, rz == Rareza.COMUN ? 1.4F : 1.0F);
        l.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.PLAYERS, 1F, 1.6F);
    }

    public static void confeti(ServerLevel l, double x, double y, double z, boolean grande) {
        com.axeliscore.fx.Efectos.lanzar(l, Navidad.MODID + (grande ? ":confeti_grande" : ":confeti"), new net.minecraft.world.phys.Vec3(x, y, z));
    }
}
