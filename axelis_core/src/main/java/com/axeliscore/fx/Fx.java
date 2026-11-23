package com.axeliscore.fx;

import com.axeliscore.net.CoreNet;
import com.axeliscore.net.Txt;
import com.axeliscore.plat.Plat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Efectos para un jugador, desde el servidor: temblor de cámara, destello, viñeta, título grande y avisos. Ejemplos:
 * <pre>
 *     Fx.shake(p, 0.8F, 20);                                   // temblor fuerte un segundo
 *     Fx.flash(p, 0xFFFFFFFF, 8);                              // destello blanco
 *     Fx.title(p, Component.literal("¡Récord!"), Component.literal("42 cm"), 0xFFD166);
 *     Fx.toast(p, new ItemStack(Items.COD), Component.literal("¡Nuevo pez!"), Component.literal("Bacalao"), 0x7FF0E0);
 * </pre>
 */
public final class Fx {
    public static final int SHAKE = 0, FLASH = 1, VIGNETTE = 2;

    private Fx() {
    }

    /** Temblor: {@code strength} 0.2 suave … 1.5 muy fuerte. */
    public static void shake(ServerPlayer p, float strength, int ticks) {
        CoreNet.NET.toPlayer(p, new CoreNet.FxMsg(SHAKE, 0, strength, ticks));
    }

    /** Temblor para todos los jugadores cerca de algo (más suave cuanto más lejos). */
    public static void shakeNear(Entity at, double radius, float strength, int ticks) {
        for (var pl : at.level().players()) {
            if (pl instanceof ServerPlayer sp) {
                double d = sp.distanceTo(at);
                if (d <= radius) {
                    shake(sp, strength * (float) (1 - d / radius * 0.7), ticks);
                }
            }
        }
    }

    /** Destello de un color (ARGB; la opacidad es la del inicio y se desvanece). */
    public static void flash(ServerPlayer p, int argb, int ticks) {
        CoreNet.NET.toPlayer(p, new CoreNet.FxMsg(FLASH, argb, 1F, ticks));
    }

    /** Bordes oscurecidos (o de un color) que se desvanecen. */
    public static void vignette(ServerPlayer p, int argb, int ticks) {
        CoreNet.NET.toPlayer(p, new CoreNet.FxMsg(VIGNETTE, argb, 1F, ticks));
    }

    /** Título grande en el centro con subtítulo (aparece con un salto y se desvanece). */
    public static void title(ServerPlayer p, Component title, Component sub, int rgb) {
        CoreNet.NET.toPlayer(p, new CoreNet.TitleMsg(Txt.pack(title), Txt.pack(sub), rgb, 60));
    }

    /** Aviso que entra por la derecha con el dibujo de un objeto. */
    public static void toast(ServerPlayer p, ItemStack icon, Component title, Component text, int rgb) {
        CoreNet.NET.toPlayer(p, new CoreNet.ToastMsg(icon.isEmpty() ? "" : Plat.itemId(icon.getItem()), Txt.pack(title), Txt.pack(text), rgb));
    }
}
