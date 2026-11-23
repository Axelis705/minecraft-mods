package com.coleccionista.prueba;

import java.util.ArrayList;
import java.util.List;

/**
 * Una cuerda entre dos personas que se enreda en los postes (vista desde arriba, en x/z). Si uno pasa por un lado de un poste y el otro por
 * el otro, la cuerda se queda "doblada" en el poste (un pivote) y deja de alcanzar: tienen que regresar por el mismo lado para soltarla.
 * <p>
 * Funciona igual en el servidor (manda los pivotes) y en el cliente (dibuja la cuerda y frena al jugador). No usa nada de Minecraft:
 * se puede probar sola.
 */
public final class Cuerda {
    /** Un doblez de la cuerda alrededor de un poste; {@code giro} es hacia dónde da la vuelta (+1 / -1). */
    public record Pivote(double x, double z, int giro) {
    }

    /** Los dobleces, del extremo A al extremo B. */
    public final List<Pivote> pivotes = new ArrayList<>();

    /** El "giro" de la vuelta P → C → Q (producto cruz en x/z). */
    static double cruz(double px, double pz, double cx, double cz, double qx, double qz) {
        return (cx - px) * (qz - cz) - (cz - pz) * (qx - cx);
    }

    /** Distancia del punto C al segmento P-Q. */
    static double distSeg(double px, double pz, double qx, double qz, double cx, double cz) {
        double dx = qx - px, dz = qz - pz;
        double l2 = dx * dx + dz * dz;
        double t = l2 < 1e-9 ? 0 : Math.max(0, Math.min(1, ((cx - px) * dx + (cz - pz) * dz) / l2));
        double x = px + t * dx, z = pz + t * dz;
        return Math.hypot(cx - x, cz - z);
    }

    private double ultAx = Double.NaN, ultAz, ultBx, ultBz;

    /**
     * Pone la cuerda al día con los extremos nuevos. {@code postes}: centros {x, z} de los postes donde se puede enredar. Se enreda cuando
     * un tramo "barre" un poste (el poste pasa de un lado de la cuerda al otro) y se suelta cuando el doblez queda derecho otra vez.
     */
    public void actualizar(double ax, double az, double bx, double bz, List<double[]> postes, double radio) {
        if (Double.isNaN(ultAx) || Math.hypot(ax - ultAx, az - ultAz) > 3 || Math.hypot(bx - ultBx, bz - ultBz) > 3) {
            // la primera vez, o si alguien se teletransportó: la cuerda empieza derecha
            pivotes.clear();
            ultAx = ax;
            ultAz = az;
            ultBx = bx;
            ultBz = bz;
            return;
        }
        // primero se mueve A (con B donde estaba) y luego B
        mover(true, ultAx, ultAz, ax, az, postes);
        ultAx = ax;
        ultAz = az;
        mover(false, ultBx, ultBz, bx, bz, postes);
        ultBx = bx;
        ultBz = bz;
        while (pivotes.size() > 12) {
            pivotes.remove(pivotes.size() / 2);
        }
    }

    /** Un extremo se mueve de (ox, oz) a (nx, nz): primero se sueltan los dobleces que quedaron derechos, luego se enreda en lo que barrió. */
    private void mover(boolean esA, double ox, double oz, double nx, double nz, List<double[]> postes) {
        for (int n = 0; n < 16 && !pivotes.isEmpty(); n++) {
            int i = esA ? 0 : pivotes.size() - 1;
            Pivote pv = pivotes.get(i);
            double[] sig = vecino(esA, i);
            // el giro siempre se mide en el sentido A -> B
            double t = esA ? cruz(nx, nz, pv.x, pv.z, sig[0], sig[1]) : cruz(sig[0], sig[1], pv.x, pv.z, nx, nz);
            if (t * pv.giro < 0) {
                pivotes.remove(i);
            } else {
                break;
            }
        }
        for (int n = 0; n < 4; n++) {
            double[] f = fijo(esA);
            double[] mejor = null;
            double md = Double.MAX_VALUE;
            for (double[] c : postes) {
                if (Math.hypot(c[0] - f[0], c[1] - f[1]) < 0.05) {
                    continue;
                }
                double s0 = cruz(f[0], f[1], c[0], c[1], ox, oz), s1 = cruz(f[0], f[1], c[0], c[1], nx, nz);
                if (s0 == 0 || Math.signum(s0) == Math.signum(s1)) {
                    continue;
                }
                // el poste tiene que quedar en lo que barrió el tramo: delante del punto fijo y no más lejos que el extremo
                double df = Math.hypot(c[0] - f[0], c[1] - f[1]);
                double dir0 = (c[0] - f[0]) * (ox - f[0]) + (c[1] - f[1]) * (oz - f[1]);
                double dir1 = (c[0] - f[0]) * (nx - f[0]) + (c[1] - f[1]) * (nz - f[1]);
                if (dir0 <= 0 && dir1 <= 0 || df > Math.max(Math.hypot(ox - f[0], oz - f[1]), Math.hypot(nx - f[0], nz - f[1]))) {
                    continue;
                }
                if (df < md) {
                    md = df;
                    mejor = c;
                }
            }
            if (mejor == null) {
                break;
            }
            // hacia dónde se dobla (en el sentido A -> B): sale del lado en el que estaba el extremo antes de barrer el poste
            int s0 = (int) Math.signum(cruz(f[0], f[1], mejor[0], mejor[1], ox, oz));
            int g = esA ? s0 : -s0;
            Pivote pv = new Pivote(mejor[0], mejor[1], g == 0 ? 1 : g);
            if (esA) {
                pivotes.add(0, pv);
            } else {
                pivotes.add(pv);
            }
            // lo que falta del movimiento se revisa contra el doblez nuevo
            ox = mejor[0] + (ox - mejor[0]) * 0.999;
            oz = mejor[1] + (oz - mejor[1]) * 0.999;
        }
    }

    /** El punto fijo del tramo del extremo (el doblez más cercano o el otro extremo). */
    private double[] fijo(boolean esA) {
        if (pivotes.isEmpty()) {
            return esA ? new double[] {ultBx, ultBz} : new double[] {ultAx, ultAz};
        }
        Pivote p = esA ? pivotes.get(0) : pivotes.get(pivotes.size() - 1);
        return new double[] {p.x, p.z};
    }

    /** El punto del otro lado del doblez i (hacia el otro extremo). */
    private double[] vecino(boolean esA, int i) {
        if (esA) {
            return i + 1 < pivotes.size() ? new double[] {pivotes.get(i + 1).x, pivotes.get(i + 1).z} : new double[] {ultBx, ultBz};
        }
        return i - 1 >= 0 ? new double[] {pivotes.get(i - 1).x, pivotes.get(i - 1).z} : new double[] {ultAx, ultAz};
    }

    /** El largo de la cuerda pasando por todos los dobleces. */
    public double largo(double ax, double az, double bx, double bz) {
        double l = 0, x = ax, z = az;
        for (Pivote p : pivotes) {
            l += Math.hypot(p.x - x, p.z - z);
            x = p.x;
            z = p.z;
        }
        return l + Math.hypot(bx - x, bz - z);
    }

    /**
     * Para el extremo A: el punto al que está "atado" (el primer doblez o el otro extremo) y cuánto se puede alejar de él sin pasarse del
     * largo máximo. Devuelve {x, z, radio}.
     */
    public double[] anclaDeA(double ax, double az, double bx, double bz, double max) {
        double total = largo(ax, az, bx, bz);
        double x = pivotes.isEmpty() ? bx : pivotes.get(0).x, z = pivotes.isEmpty() ? bz : pivotes.get(0).z;
        double propio = Math.hypot(x - ax, z - az);
        return new double[] {x, z, Math.max(0.5, max - (total - propio))};
    }

    /** Lo mismo para el extremo B. */
    public double[] anclaDeB(double ax, double az, double bx, double bz, double max) {
        double total = largo(ax, az, bx, bz);
        double x = pivotes.isEmpty() ? ax : pivotes.get(pivotes.size() - 1).x, z = pivotes.isEmpty() ? az : pivotes.get(pivotes.size() - 1).z;
        double propio = Math.hypot(x - bx, z - bz);
        return new double[] {x, z, Math.max(0.5, max - (total - propio))};
    }

    /** Prueba sin Minecraft: dos personas rodean un poste por lados distintos y la cuerda se dobla; al regresar, se suelta. */
    public static void main(String[] a) {
        Cuerda c = new Cuerda();
        List<double[]> postes = List.of(new double[] {0, 0});
        // A en (-2, -1) y B en (-2, 1): la cuerda pasa al oeste del poste, sin tocarlo
        c.actualizar(-2, -1, -2, 1, postes, 0.4);
        c.actualizar(-2, -1, -2, 1, postes, 0.4);
        check(c.pivotes.isEmpty(), "sin dobleces al principio");
        // B camina al este por el norte del poste (z = 1) hasta (2, 1): la cuerda A-B cruza el poste y se dobla
        for (double x = -2; x <= 2; x += 0.25) {
            c.actualizar(-2, -1, x, 1, postes, 0.4);
        }
        // A camina al este por el sur (z = -1) hasta (2, -1): la cuerda queda enredada en el poste
        for (double x = -2; x <= 2; x += 0.25) {
            c.actualizar(x, -1, 2, 1, postes, 0.4);
        }
        check(c.pivotes.size() == 1, "se dobló en el poste (" + c.pivotes.size() + ")");
        double l = c.largo(2, -1, 2, 1);
        check(l > 4, "el largo pasa por el poste: " + l);
        // A regresa por el mismo lado: se suelta
        for (double x = 2; x >= -2; x -= 0.25) {
            c.actualizar(x, -1, 2, 1, postes, 0.4);
        }
        c.actualizar(-2, -0.5, 2, 1, postes, 0.4);
        check(c.pivotes.isEmpty(), "al regresar por el mismo lado se soltó (" + c.pivotes.size() + ")");
        // dos postes en fila: B rodea los dos (zigzag) y luego regresa
        Cuerda d = new Cuerda();
        List<double[]> dos = List.of(new double[] {0, 0}, new double[] {0, 3});
        d.actualizar(-2, -1, -2, 1, dos, 0.4);
        double[][] ruta = {{-2, 1}, {3, 1}, {3, 4}, {-2, 4}};
        double bx0 = -2, bz0 = 1;
        for (double[] r : ruta) {
            for (int i = 1; i <= 10; i++) {
                double bx = bx0 + (r[0] - bx0) * i / 10, bz = bz0 + (r[1] - bz0) * i / 10;
                d.actualizar(-2, -1, bx, bz, dos, 0.4);
            }
            bx0 = r[0];
            bz0 = r[1];
        }
        check(d.pivotes.size() == 2, "zigzag entre dos postes: dos dobleces (" + d.pivotes.size() + ")");
        for (int k = ruta.length - 2; k >= 0; k--) {
            double[] r = ruta[k];
            for (int i = 1; i <= 10; i++) {
                double bx = bx0 + (r[0] - bx0) * i / 10, bz = bz0 + (r[1] - bz0) * i / 10;
                d.actualizar(-2, -1, bx, bz, dos, 0.4);
            }
            bx0 = r[0];
            bz0 = r[1];
        }
        check(d.pivotes.isEmpty(), "regresando por el mismo camino se suelta todo (" + d.pivotes.size() + ")");
        System.out.println("CUERDA_OK");
    }

    private static void check(boolean ok, String m) {
        if (!ok) {
            throw new AssertionError(m);
        }
        System.out.println("ok: " + m);
    }
}
