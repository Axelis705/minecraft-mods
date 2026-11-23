package com.axeliscore.anim;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Un modelo hecho de huesos y cubos (se carga de un .geo.json de Blockbench con {@link GeoLoader}).
 * <p>
 * Todo está en el espacio de los modelos de Minecraft Java: píxeles (16 = un bloque), la y crece hacia ABAJO, el origen está a 24
 * píxeles sobre los pies. Así un modelo de Blockbench y uno hecho a mano con ModelPart se ven igual. Los ángulos van en radianes.
 */
public final class Geo {
    public final String id;
    public final int texW, texH;
    public final Bone[] bones;
    /** Huesos sin padre, en orden. */
    public final int[] roots;
    private final Map<String, Integer> index;
    /** Texturas por cuadros ("axelis_frames" en el geo). */
    public final java.util.List<Cuadros> cuadros = new java.util.ArrayList<>();
    /** Huesos que laten de brillo ("axelis_pulse"). */
    public final java.util.List<Pulso> pulsos = new java.util.ArrayList<>();

    /** Un hueso cuya textura cambia por cuadros: cada cuadro corre la textura (du, dv) píxeles. */
    public record Cuadros(int bone, int frames, float fps, float du, float dv) {
    }

    /** Un hueso que late: su brillo va de min a max, {@code veces} por segundo. */
    public record Pulso(int bone, float min, float max, float veces) {
    }

    public Geo(String id, int texW, int texH, Bone[] bones) {
        this.id = id;
        this.texW = texW;
        this.texH = texH;
        this.bones = bones;
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < bones.length; i++) {
            m.put(bones[i].name, i);
        }
        this.index = Collections.unmodifiableMap(m);
        int nr = 0;
        for (Bone b : bones) {
            if (b.parent < 0) {
                nr++;
            }
        }
        roots = new int[nr];
        nr = 0;
        for (int i = 0; i < bones.length; i++) {
            if (bones[i].parent < 0) {
                roots[nr++] = i;
            }
        }
        for (int i = 0; i < bones.length; i++) {
            int n = 0;
            for (Bone b : bones) {
                if (b.parent == i) {
                    n++;
                }
            }
            bones[i].children = new int[n];
            n = 0;
            for (int j = 0; j < bones.length; j++) {
                if (bones[j].parent == i) {
                    bones[i].children[n++] = j;
                }
            }
        }
    }

    /** Índice de un hueso por nombre (-1 si no existe). */
    public int bone(String name) {
        Integer i = index.get(name);
        return i == null ? -1 : i;
    }

    public Map<String, Integer> boneIndex() {
        return index;
    }

    /** ¿Es {@code b} el hueso {@code ancestor} o cuelga de él? */
    public boolean isUnder(int b, int ancestor) {
        while (b >= 0) {
            if (b == ancestor) {
                return true;
            }
            b = bones[b].parent;
        }
        return false;
    }

    public static final class Bone {
        public final String name;
        public final int parent;
        /** Punto de giro (espacio del modelo). */
        public final float px, py, pz;
        /** Giro de reposo (radianes). */
        public final float rx, ry, rz;
        public final List<Cube> cubes;
        /** Se dibuja con brillo propio (sin sombra, como los ojos de las arañas). */
        public final boolean glow;
        /** Hijos (lo llena Geo). */
        public int[] children = new int[0];

        public Bone(String name, int parent, float px, float py, float pz, float rx, float ry, float rz, List<Cube> cubes, boolean glow) {
            this.name = name;
            this.parent = parent;
            this.px = px;
            this.py = py;
            this.pz = pz;
            this.rx = rx;
            this.ry = ry;
            this.rz = rz;
            this.cubes = cubes;
            this.glow = glow;
        }
    }

    /** Un cubo (espacio del modelo, no relativo al hueso). */
    public static final class Cube {
        /** Esquina menor y tamaño (píxeles). */
        public final float x0, y0, z0, sx, sy, sz;
        public final float inflate;
        /** Coordenadas de textura (estilo caja: como en ModelPart). Si {@link #faces} no es null, se usan esas. */
        public final float u, v;
        public final boolean mirror;
        /** Giro propio del cubo (Blockbench permite cubos girados) alrededor de (cpx, cpy, cpz). */
        public final float crx, cry, crz, cpx, cpy, cpz;
        /** UV por cara (orden: abajo, arriba, oeste, norte, este, sur; null si se usa la caja); cada una u0, v0, u1, v1. */
        public final float[][] faces;
        /**
         * Doblez suave (codos y rodillas): la parte del cubo por debajo de {@link #bendY} sigue al hueso {@link #bendBone} y la franja
         * del medio se dobla a la mitad, sin huecos. -1 si el cubo no se dobla.
         */
        public int bendBone = -1;
        public float bendY;

        public Cube(float x0, float y0, float z0, float sx, float sy, float sz, float inflate, float u, float v, boolean mirror, float crx,
                float cry, float crz, float cpx, float cpy, float cpz, float[][] faces) {
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.inflate = inflate;
            this.u = u;
            this.v = v;
            this.mirror = mirror;
            this.crx = crx;
            this.cry = cry;
            this.crz = crz;
            this.cpx = cpx;
            this.cpy = cpy;
            this.cpz = cpz;
            this.faces = faces;
        }

        public boolean rotated() {
            return crx != 0 || cry != 0 || crz != 0;
        }
    }
}
