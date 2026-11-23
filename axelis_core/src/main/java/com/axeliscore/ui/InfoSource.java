package com.axeliscore.ui;

/**
 * Algo que muestra un cuadro de información al mirarlo en el mundo (personajes, criaturas, bloques). Se llama en el cliente cada vez
 * que hace falta: llenar {@code card} con lo que se quiera mostrar. {@code detail} es true con Shift apretado.
 */
public interface InfoSource {
    void axInfo(InfoCard card, boolean detail);
}
