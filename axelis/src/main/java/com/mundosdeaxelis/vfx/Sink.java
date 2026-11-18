package com.mundosdeaxelis.vfx;

/** Recibe los cuadriláteros que genera {@link VfxGeom}: coordenadas del mundo, UV y color (r,g,b,a por vértice). */
public interface Sink {
    int ADD = 0;
    int NORMAL = 1;
    /** Opaco con profundidad (el dragoncito en 3D): se pinta antes que lo translúcido. */
    int SOLID = 2;

    void quad(int tex, int blend, double[] p12, float[] uv8, float[] col16);
}
