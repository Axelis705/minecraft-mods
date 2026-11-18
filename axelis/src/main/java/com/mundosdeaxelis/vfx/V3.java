package com.mundosdeaxelis.vfx;

/** Vector 3D inmutable mínimo (sin depender de Minecraft, para poder probar la geometría fuera del juego). */
public final class V3 {
    public static final V3 ZERO = new V3(0, 0, 0);
    public static final V3 UP = new V3(0, 1, 0);
    public final double x, y, z;

    public V3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public V3 add(V3 o) {
        return new V3(x + o.x, y + o.y, z + o.z);
    }

    public V3 add(double dx, double dy, double dz) {
        return new V3(x + dx, y + dy, z + dz);
    }

    public V3 sub(V3 o) {
        return new V3(x - o.x, y - o.y, z - o.z);
    }

    public V3 mul(double s) {
        return new V3(x * s, y * s, z * s);
    }

    public double dot(V3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public V3 cross(V3 o) {
        return new V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    public double len() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public V3 norm() {
        double l = len();
        return l < 1.0E-9 ? new V3(0, 1, 0) : new V3(x / l, y / l, z / l);
    }

    public V3 lerp(V3 o, double t) {
        return new V3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t);
    }

    /** Gira este vector alrededor de un eje unitario (fórmula de Rodrigues). */
    public V3 rot(V3 axis, double ang) {
        double c = Math.cos(ang), s = Math.sin(ang);
        return mul(c).add(axis.cross(this).mul(s)).add(axis.mul(axis.dot(this) * (1 - c)));
    }

    /** Un vector perpendicular cualquiera (unitario). */
    public V3 anyPerp() {
        V3 r = Math.abs(y) < 0.95 ? UP : new V3(1, 0, 0);
        return cross(r).norm();
    }
}
