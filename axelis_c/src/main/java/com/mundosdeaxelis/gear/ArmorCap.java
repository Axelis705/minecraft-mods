package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Quita el tope de 30 puntos de la armadura de Minecraft. El tope vive en el campo {@code maxValue} del atributo
 * {@code Attributes.ARMOR} (un {@link RangedAttribute}); como es el mismo objeto en cliente y servidor, basta con subirlo una vez
 * al arrancar. El campo se busca por tipo y valor (el que vale 30) para no depender del nombre ofuscado.
 *
 * <p>Ojo: la fórmula de daño de Minecraft solo cuenta 20 puntos "efectivos" (80 % de reducción), pero esos 20 se alcanzan con
 * menos daño cuanto mayor es la armadura, así que los puntos extra sirven sobre todo contra golpes fuertes (jefes).
 */
public final class ArmorCap {
    /** Nuevo tope de la armadura. */
    public static final double MAX = 1024.0D;

    private ArmorCap() {
    }

    public static void raise() {
        Attribute attr = Attributes.ARMOR;
        if (!(attr instanceof RangedAttribute ranged)) {
            MundosDeAxelis.LOGGER.warn("Armadura: el atributo no es un RangedAttribute; no se pudo quitar el tope de 30.");
            return;
        }
        try {
            for (Field f : RangedAttribute.class.getDeclaredFields()) {
                if (f.getType() == double.class && !Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    if (f.getDouble(ranged) == 30.0D) {
                        f.setDouble(ranged, MAX);
                        MundosDeAxelis.LOGGER.info("Armadura: tope subido de 30 a {}.", (int) MAX);
                        return;
                    }
                }
            }
            MundosDeAxelis.LOGGER.warn("Armadura: no se encontró el campo del tope (30); sigue en el valor normal.");
        } catch (ReflectiveOperationException | RuntimeException ex) {
            MundosDeAxelis.LOGGER.warn("Armadura: no se pudo quitar el tope de 30: {}", ex.toString());
        }
    }
}
