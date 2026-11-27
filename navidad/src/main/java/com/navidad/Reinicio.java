package com.navidad;

import com.axeliscore.data.WorldStore;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /navidad reiniciar libro|santa|todo [jugadores]}: para volver a empezar (probar, jugar otra temporada con amigos...).
 * <ul>
 * <li><b>libro</b>: borra las tareas y los premios reclamados del Libro de Navidad (empieza de cero) y te da un libro si no traes.</li>
 * <li><b>santa</b>: Santa vuelve a pasar hoy (en unos segundos, cuando estés afuera), los calcetines se vuelven a llenar, puedes
 * mandarle otra carta hoy y las galletas cuentan otra vez.</li>
 * <li><b>todo</b>: las dos cosas y además la bienvenida de Santa otra vez (pino, esferas y libro).</li>
 * </ul>
 */
public final class Reinicio {
    private Reinicio() {
    }

    public static void libro(ServerPlayer p) {
        WorldStore w = WorldStore.get(p.server, Navidad.MODID);
        CompoundTag yo = w.player(p.getUUID());
        yo.remove("libro");
        w.dirty();
        boolean trae = p.getInventory().contains(new ItemStack(ModRegistry.LIBRO.get()));
        if (!trae) {
            ItemStack libro = new ItemStack(ModRegistry.LIBRO.get());
            if (!p.getInventory().add(libro)) {
                p.drop(libro, false);
            }
        }
        com.navidad.libro.Libro.sync(p);
        p.sendSystemMessage(Component.translatable("navidad.reinicio.libro").withStyle(ChatFormatting.GOLD));
        Navidad.LOGGER.info("NAVIDAD_REINICIO libro {}", p.getGameProfile().getName());
    }

    public static void santa(ServerPlayer p) {
        WorldStore w = WorldStore.get(p.server, Navidad.MODID);
        CompoundTag yo = w.player(p.getUUID());
        long dia = p.serverLevel().getDayTime() / 24000L, hora = p.serverLevel().getDayTime() % 24000L;
        yo.putLong("santaDia", dia);
        yo.putInt("santaVino", 0);
        // en unos segundos (si ya es muy noche, apenas le da tiempo: que sea ya)
        yo.putLong("santaHora", Math.min(hora + 200, 23000));
        for (String k : new String[]{"calcetinDia", "calcetinCuando", "cartaDia", "galletasTick", "galletasCuando"}) {
            yo.remove(k);
        }
        w.dirty();
        p.sendSystemMessage(Component.translatable("navidad.reinicio.santa").withStyle(ChatFormatting.GOLD));
        Navidad.LOGGER.info("NAVIDAD_REINICIO santa {}", p.getGameProfile().getName());
    }

    public static void todo(ServerPlayer p) {
        WorldStore w = WorldStore.get(p.server, Navidad.MODID);
        CompoundTag yo = w.player(p.getUUID());
        yo.remove("bienvenida");
        w.dirty();
        libro(p);
        santa(p);
        // la bienvenida (y el libro) llegan al momento, como la primera vez que entraste
        Fiestas.alEntrar(p);
    }
}
