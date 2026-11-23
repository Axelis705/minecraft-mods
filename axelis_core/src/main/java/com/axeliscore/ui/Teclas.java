package com.axeliscore.ui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Teclas propias de un mod (aparecen en Opciones → Controles y se pueden cambiar), iguales en todas las versiones. Se piden en el
 * constructor del mod (también en el servidor: ahí no hacen nada):
 * <pre>
 *     public static final Teclas.Tecla DIARIO = Teclas.nueva("mimod", "diario", 'J', () -&gt; abrirDiario());
 * </pre>
 * El código es la letra ('J') o el número de tecla de GLFW (por ejemplo 258 = Tab). Texto en el idioma: {@code key.mimod.diario} y
 * la categoría {@code key.categories.mimod}. La acción se hace en el cliente, solo cuando no hay ninguna pantalla abierta.
 */
public final class Teclas {
    /** Una tecla pedida. La de verdad (KeyMapping) la crea el cliente al arrancar. */
    public static final class Tecla {
        public final String modid, nombre;
        public final int codigo;
        public final Runnable alPulsar;
        /** (Cliente) El KeyMapping, cuando ya existe. */
        public Object mapping;

        Tecla(String modid, String nombre, int codigo, Runnable alPulsar) {
            this.modid = modid;
            this.nombre = nombre;
            this.codigo = codigo;
            this.alPulsar = alPulsar;
        }
    }

    public static final List<Tecla> TODAS = new CopyOnWriteArrayList<>();

    private Teclas() {
    }

    /** Pide una tecla con una letra o número ('J', '5'). */
    public static Tecla nueva(String modid, String nombre, char letra, Runnable alPulsar) {
        return nueva(modid, nombre, (int) Character.toUpperCase(letra), alPulsar);
    }

    /** Pide una tecla con su número de GLFW (65-90 letras, 48-57 números, 290-301 F1-F12; -1 = sin asignar). */
    public static Tecla nueva(String modid, String nombre, int codigoGlfw, Runnable alPulsar) {
        Tecla t = new Tecla(modid, nombre, codigoGlfw, alPulsar);
        TODAS.add(t);
        return t;
    }
}
