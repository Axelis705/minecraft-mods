package com.axeliscore.net;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Textos para mandar por la red sin depender de cómo cada versión guarda los Component: una clave de traducción con sus argumentos
 * (se traduce en el idioma de cada jugador) o un texto tal cual.
 */
public final class Txt {
    private static final char SEP = '\u0001';

    private Txt() {
    }

    /** Del servidor: un Component traducible (con argumentos de texto) o literal. */
    public static String pack(Component c) {
        if (c == null) {
            return "";
        }
        if (c.getContents() instanceof TranslatableContents t) {
            StringBuilder sb = new StringBuilder("#").append(t.getKey());
            for (Object a : t.getArgs()) {
                sb.append(SEP).append(a instanceof Component ac ? ac.getString() : String.valueOf(a));
            }
            return sb.toString();
        }
        return "=" + c.getString();
    }

    /** En el cliente: de vuelta a Component. */
    public static Component unpack(String s) {
        if (s == null || s.isEmpty()) {
            return Component.empty();
        }
        if (s.charAt(0) == '#') {
            String[] p = s.substring(1).split(String.valueOf(SEP), -1);
            Object[] args = new Object[p.length - 1];
            System.arraycopy(p, 1, args, 0, args.length);
            return Component.translatable(p[0], args);
        }
        return Component.literal(s.charAt(0) == '=' ? s.substring(1) : s);
    }
}
