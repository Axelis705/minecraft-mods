package com.axeliscore.anim;

import java.util.Arrays;

/**
 * La postura de un modelo en un instante: para cada hueso, giro extra (radianes, se suma al de reposo), desplazamiento (píxeles en el
 * espacio de Java) y escala. Es lo que calcula el {@link Animator} y lo que dibuja el renderizador.
 */
public final class Pose {
    public final int n;
    public final float[] rot, pos, scl;
    public final boolean[] hidden;
    /** Corrimiento de la textura de cada hueso (píxeles): para ojos, bocas o fuego por cuadros. */
    public final float[] du, dv;
    /** Color que multiplica a cada hueso (ARGB; 0xFFFFFFFF = normal): para brillar, apagarse o volverse transparente. */
    public final int[] tinte;

    public Pose(int bones) {
        n = bones;
        rot = new float[bones * 3];
        pos = new float[bones * 3];
        scl = new float[bones * 3];
        hidden = new boolean[bones];
        du = new float[bones];
        dv = new float[bones];
        tinte = new int[bones];
        reset();
    }

    public void reset() {
        Arrays.fill(rot, 0);
        Arrays.fill(pos, 0);
        Arrays.fill(scl, 1);
        Arrays.fill(hidden, false);
        Arrays.fill(du, 0);
        Arrays.fill(dv, 0);
        Arrays.fill(tinte, 0xFFFFFFFF);
    }

    public void copyFrom(Pose o) {
        System.arraycopy(o.rot, 0, rot, 0, rot.length);
        System.arraycopy(o.pos, 0, pos, 0, pos.length);
        System.arraycopy(o.scl, 0, scl, 0, scl.length);
        System.arraycopy(o.hidden, 0, hidden, 0, hidden.length);
        System.arraycopy(o.du, 0, du, 0, du.length);
        System.arraycopy(o.dv, 0, dv, 0, dv.length);
        System.arraycopy(o.tinte, 0, tinte, 0, tinte.length);
    }

    /** Corre la textura de un hueso (píxeles). */
    public void uv(int bone, float u, float v) {
        if (bone >= 0) {
            du[bone] = u;
            dv[bone] = v;
        }
    }

    /** Color de un hueso (ARGB que multiplica). */
    public void tinte(int bone, int argb) {
        if (bone >= 0) {
            tinte[bone] = argb;
        }
    }

    /** Mezcla hacia otra postura: w = 0 se queda igual, w = 1 queda como la otra. */
    public void blendTo(Pose o, float w, boolean[] mask) {
        if (w <= 0) {
            return;
        }
        for (int b = 0; b < n; b++) {
            if (mask != null && !mask[b]) {
                continue;
            }
            for (int k = b * 3; k < b * 3 + 3; k++) {
                rot[k] += (o.rot[k] - rot[k]) * w;
                pos[k] += (o.pos[k] - pos[k]) * w;
                scl[k] += (o.scl[k] - scl[k]) * w;
            }
            if (w >= 0.5F) {
                hidden[b] = o.hidden[b];
            }
        }
    }

    /** Mezcla con un peso por hueso (máscara suave). */
    public void blendTo(Pose o, float w, float[] pesos) {
        if (w <= 0) {
            return;
        }
        for (int b = 0; b < n; b++) {
            float wb = w * pesos[b];
            if (wb <= 0) {
                continue;
            }
            for (int k = b * 3; k < b * 3 + 3; k++) {
                rot[k] += (o.rot[k] - rot[k]) * wb;
                pos[k] += (o.pos[k] - pos[k]) * wb;
                scl[k] += (o.scl[k] - scl[k]) * wb;
            }
            if (wb >= 0.5F) {
                hidden[b] = o.hidden[b];
            }
        }
    }

    public void addRot(int bone, float x, float y, float z) {
        if (bone < 0) {
            return;
        }
        rot[bone * 3] += x;
        rot[bone * 3 + 1] += y;
        rot[bone * 3 + 2] += z;
    }

    public void setRot(int bone, float x, float y, float z) {
        if (bone < 0) {
            return;
        }
        rot[bone * 3] = x;
        rot[bone * 3 + 1] = y;
        rot[bone * 3 + 2] = z;
    }

    public void addPos(int bone, float x, float y, float z) {
        if (bone < 0) {
            return;
        }
        pos[bone * 3] += x;
        pos[bone * 3 + 1] += y;
        pos[bone * 3 + 2] += z;
    }

    public void mulScale(int bone, float x, float y, float z) {
        if (bone < 0) {
            return;
        }
        scl[bone * 3] *= x;
        scl[bone * 3 + 1] *= y;
        scl[bone * 3 + 2] *= z;
    }

    public void hide(int bone, boolean h) {
        if (bone >= 0) {
            hidden[bone] = h;
        }
    }
}
