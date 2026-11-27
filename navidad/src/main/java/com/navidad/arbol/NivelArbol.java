package com.navidad.arbol;

import com.navidad.ArbolSanta;
import com.navidad.bloque.deco.Deco;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/**
 * Qué tan bonito está tu árbol: los puntos de sus adornos, uno más por cada adorno distinto y los adornos navideños de tu casa
 * alrededor (hasta {@link #MAX_CASA}). Con los puntos sube de nivel (de "Arbolito" a "Legendario") y entre más nivel, más y mejores
 * regalos deja Santa cada mañana.
 */
public final class NivelArbol {
    /** Desde cuántos puntos empieza cada nivel (1 a 6). */
    public static final int[] DESDE = {0, 10, 25, 45, 70, 100};
    /** Lo más que cuentan los adornos de la casa. */
    public static final int MAX_CASA = 25;

    private NivelArbol() {
    }

    /** El estado de un árbol: puntos por adornos, por variedad y por la casa; total, nivel y cuántos faltan para el siguiente. */
    public record Estado(int adornos, int variedad, int casa, int total, int nivel, int colgados, int luces, int guirnaldas, String punta,
            CompoundTag dibujo) {
        public int siguiente() {
            return nivel >= DESDE.length ? -1 : DESDE[nivel];
        }

        public CompoundTag tag() {
            CompoundTag t = new CompoundTag();
            t.putInt("adornos", adornos);
            t.putInt("variedad", variedad);
            t.putInt("casa", casa);
            t.putInt("total", total);
            t.putInt("nivel", nivel);
            t.putInt("colgados", colgados);
            t.putInt("luces", luces);
            t.putInt("guirnaldas", guirnaldas);
            t.putString("punta", punta);
            if (dibujo != null) {
                t.put("dibujo", dibujo);
            }
            return t;
        }
    }

    public static int nivel(int puntos) {
        int n = 1;
        for (int i = 0; i < DESDE.length; i++) {
            if (puntos >= DESDE[i]) {
                n = i + 1;
            }
        }
        return n;
    }

    /** Lo que valen los árboles de adorno que ya vienen adornados (no se les puede colgar nada). */
    private static int base(Block b) {
        if (b == Deco.block("arbol_dorado")) {
            return 14;
        }
        if (b == Deco.block("arbol_navidad") || b == Deco.block("arbol_nevado")) {
            return 10;
        }
        return 2;
    }

    public static Estado de(ServerLevel l, BlockPos arbol) {
        int adornos, variedad = 0, colgados = 0, luces = 0, guirnaldas = 0;
        String punta = "";
        CompoundTag dibujo = null;
        if (l.getBlockEntity(arbol) instanceof PinoBE be) {
            dibujo = new CompoundTag();
            be.escribir(dibujo);
            adornos = be.puntosAdornos();
            variedad = be.variedad();
            colgados = be.colgados();
            luces = be.luces;
            guirnaldas = be.guirnaldas;
            punta = be.punta;
        } else {
            adornos = base(l.getBlockState(arbol).getBlock());
        }
        int casa = Math.min(MAX_CASA, ArbolSanta.adornos(l, arbol));
        int total = adornos + variedad + casa;
        return new Estado(adornos, variedad, casa, total, nivel(total), colgados, luces, guirnaldas, punta, dibujo);
    }

    public static Component nombre(int nivel) {
        return Component.translatable("navidad.arbol.nivel." + nivel);
    }

    /** ★★★☆☆☆ */
    public static String estrellas(int nivel) {
        return "★".repeat(nivel) + "☆".repeat(DESDE.length - nivel);
    }

    /** El resumen que sale al tocar el árbol con la mano vacía. */
    public static MutableComponent resumen(Estado e) {
        MutableComponent c = Component.literal(estrellas(e.nivel()) + " ").withStyle(ChatFormatting.GOLD)
                .append(nombre(e.nivel()).copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(" · ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("navidad.arbol.puntos", e.total(), e.adornos(), e.variedad(), e.casa()).withStyle(ChatFormatting.WHITE));
        if (e.siguiente() > 0) {
            c.append(Component.literal(" · ").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable("navidad.arbol.faltan", e.siguiente() - e.total()).withStyle(ChatFormatting.AQUA));
        }
        return c;
    }
}
