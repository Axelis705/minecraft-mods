package com.mundosdeaxelis.world;

import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.table.Essence;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Etapa 269: las crónicas. Cada tier tiene 3 pergaminos que cuentan qué pasó en sus estructuras (como los libros perdidos de
 * Stardew o los diarios de Hollow Knight). Salen en los cofres (uno que aún no hayas leído). Al leer uno por primera vez queda
 * apuntado en tu Archivo; al completar los 3 de un tier recibes un premio: Esencia y un Fragmento de corazón.
 */
public final class Cronicas {
    private Cronicas() {
    }

    public static final int PER_TIER = 3;
    /** Nombre del tier en los ids de los pergaminos (bosque_1 … magia_3). */
    private static final String[] TIER = {"", "bosque", "forja", "hielo", "fuego", "magia"};

    /** Tier de un pergamino (1 a 5) o 0 si el id no es válido. */
    public static int tierOf(String id) {
        for (int t = 1; t < TIER.length; t++) {
            if (id.startsWith(TIER[t] + "_")) {
                return t;
            }
        }
        return 0;
    }

    /** Los ids de los pergaminos de un tier (vacío si ese tier aún no tiene historia). */
    public static List<String> of(int tier) {
        List<String> out = new ArrayList<>();
        if (tier >= 1 && tier < TIER.length) {
            for (int i = 1; i <= PER_TIER; i++) {
                out.add(TIER[tier] + "_" + i);
            }
        }
        return out;
    }

    public static ItemStack scroll(String id) {
        ItemStack st = new ItemStack(ModItems.LORE_SCROLL.get());
        st.getOrCreateTag().putString("Pergamino", id);
        return st;
    }

    public static String idOf(ItemStack st) {
        return st.hasTag() ? st.getTag().getString("Pergamino") : "";
    }

    /** Un pergamino del tier que ese jugador aún no ha leído (al azar), o vacío si ya los leyó todos. */
    public static ItemStack unread(int tier, Set<String> archive, RandomSource r) {
        List<String> left = new ArrayList<>();
        for (String id : of(tier)) {
            if (archive == null || !archive.contains("lore_" + id)) {
                left.add(id);
            }
        }
        return left.isEmpty() ? ItemStack.EMPTY : scroll(left.get(r.nextInt(left.size())));
    }

    /** El jugador leyó ese pergamino (en el servidor): lo apunta y, si completó la crónica del tier, le da el premio. */
    public static void read(ServerPlayer sp, String id) {
        int tier = tierOf(id);
        if (tier == 0 || !Essence.discover(sp, "lore_" + id)) {
            return;
        }
        Set<String> arch = Essence.archive(sp);
        int done = 0;
        for (String o : of(tier)) {
            if (arch.contains("lore_" + o)) {
                done++;
            }
        }
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.9F);
        if (done < PER_TIER) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.cronica.nueva", Component.translatable("cronica.mundos_de_axelis." + tier),
                    done, PER_TIER).withStyle(ChatFormatting.YELLOW), true);
            Essence.sync(sp);
            return;
        }
        int gain = 120 * tier;
        Essence.add(sp, gain);
        Essence.sync(sp);
        ItemStack heart = new ItemStack(ModItems.HEART_FRAGMENT.get());
        if (!sp.getInventory().add(heart)) {
            sp.drop(heart, false);
        }
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
        sp.serverLevel().sendParticles(ParticleTypes.ENCHANT, sp.getX(), sp.getY() + 1.5, sp.getZ(), 60, 0.6, 0.8, 0.6, 0.6);
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.cronica.completa", Component.translatable("cronica.mundos_de_axelis." + tier), gain)
                .withStyle(ChatFormatting.GOLD), false);
    }
}
