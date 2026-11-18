package com.mundosdeaxelis.vfx;

/** Cámara: posición y ejes (derecha, arriba, adelante) en coordenadas del mundo. */
public final class Cam {
    public final V3 pos, right, up, fwd;

    public Cam(V3 pos, V3 right, V3 up, V3 fwd) {
        this.pos = pos;
        this.right = right;
        this.up = up;
        this.fwd = fwd;
    }

    public static Cam lookAt(V3 eye, V3 target) {
        V3 f = target.sub(eye).norm();
        V3 r = f.cross(V3.UP).norm();
        V3 u = r.cross(f).norm();
        return new Cam(eye, r, u, f);
    }
}
