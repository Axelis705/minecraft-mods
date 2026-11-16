package com.casino.client;

import java.lang.reflect.Method;

/**
 * Qué letra o número es una tecla. Se acepta la tecla por su lugar (teclado de EE. UU.) o por lo que dice en el teclado del jugador
 * (así funciona igual con teclados en español, francés o alemán).
 */
final class KeyChars {
    /** GLFW se llama por reflexión: en algunas versiones del juego no está en el classpath de compilación (sí al jugar). */
    private static final Method KEY_NAME;
    private static final Method CONTEXT;
    private static final Method GET_KEY;

    static {
        Method a = null, b = null, c = null;
        try {
            Class<?> g = Class.forName("org.lwjgl.glfw.GLFW");
            a = g.getMethod("glfwGetKeyName", int.class, int.class);
            b = g.getMethod("glfwGetCurrentContext");
            c = g.getMethod("glfwGetKey", long.class, int.class);
        } catch (Throwable ignored) {
            // sin GLFW: solo por el lugar de la tecla
        }
        KEY_NAME = a;
        CONTEXT = b;
        GET_KEY = c;
    }

    private KeyChars() {
    }

    /** Letra o número por el lugar de la tecla (0 si no es una). */
    static char byCode(int key) {
        if ((key >= 65 && key <= 90) || (key >= 48 && key <= 57)) {
            return (char) key;
        }
        if (key >= 320 && key <= 329) {
            return (char) ('0' + key - 320);   // teclado numérico
        }
        return 0;
    }

    /** Letra o número según la distribución del teclado del jugador (0 si no es una o no se sabe). */
    static char byName(int key, int scan) {
        try {
            if (KEY_NAME == null) {
                return 0;
            }
            String n = (String) KEY_NAME.invoke(null, key, scan);
            if (n != null && n.length() == 1) {
                char c = Character.toUpperCase(n.charAt(0));
                if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                    return c;
                }
            }
        } catch (Throwable ignored) {
            // sin GLFW listo: solo por el lugar
        }
        return 0;
    }

    /** ¿Es una letra o un número? */
    static boolean letterOrDigit(int key, int scan) {
        return byCode(key) != 0 || byName(key, scan) != 0;
    }

    /** ¿Esta tecla es la letra o número c? */
    static boolean is(int key, int scan, char c) {
        return c != 0 && (byCode(key) == c || byName(key, scan) == c);
    }

    /** La ventana del juego (0 si no se puede saber). */
    static long window() {
        try {
            return CONTEXT == null ? 0 : (Long) CONTEXT.invoke(null);
        } catch (Throwable t) {
            return 0;
        }
    }

    /** ¿Está apretada ahora mismo? (false si no se puede saber) */
    static boolean down(long window, int key) {
        if (window == 0 || GET_KEY == null) {
            return false;
        }
        try {
            return (Integer) GET_KEY.invoke(null, window, key) == 1;   // GLFW_PRESS
        } catch (Throwable t) {
            return false;
        }
    }
}
