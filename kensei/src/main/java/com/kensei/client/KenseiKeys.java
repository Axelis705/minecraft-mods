package com.kensei.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Controles: clic izquierdo = ataque, clic derecho (sostenido) = cubrirse, clic central = fijar objetivo. Estas teclas son el resto.
 * Los nombres internos son nuevos a propósito: así los valores de abajo se aplican aunque el juego ya guardara las teclas viejas.
 */
public final class KenseiKeys {
    /** Esquive (dash): Alt izquierdo, a mano del pulgar mientras se camina con WASD. */
    public static final KeyMapping DODGE = new KeyMapping("key.kensei.dash", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.categories.kensei");

    public static final KeyMapping HEAVY = new KeyMapping("key.kensei.critical", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.kensei");

    /** Clic central: fija al enemigo (y lo suelta si ya hay uno fijado). */
    public static final KeyMapping LOCK = new KeyMapping("key.kensei.target", KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, "key.categories.kensei");

    public static final KeyMapping NEXT_TARGET = new KeyMapping("key.kensei.target_next", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "key.categories.kensei");

    /** Desafiar a un duelo al jugador al que apuntas (o fijado), o aceptar un desafío que te hicieron. */
    public static final KeyMapping DUEL = new KeyMapping("key.kensei.duel", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.kensei");

    private KenseiKeys() {
    }
}
