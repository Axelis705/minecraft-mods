import com.autoscoleccion.game.CarDef;
import com.autoscoleccion.game.CarMesh;
import com.autoscoleccion.game.Cars;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Vista previa de los 30 cochecitos sin abrir Minecraft: dibuja la misma geometría de CarMesh con un rasterizador sencillo (z-buffer,
 * luz difusa parecida a la del juego) en una hoja de 6 x 5. Uso: java VistaPrevia salida.png [yaw] [pitch]
 */
public class VistaPrevia {
    static final int CELL = 240;

    public static void main(String[] a) throws Exception {
        String out = a.length > 0 ? a[0] : "autos.png";
        double yaw = a.length > 1 ? Math.toRadians(Double.parseDouble(a[1])) : Math.toRadians(-35);
        double pitch = a.length > 2 ? Math.toRadians(Double.parseDouble(a[2])) : Math.toRadians(24);
        int cols = 6;
        int rows = 5;
        BufferedImage img = new BufferedImage(cols * CELL, rows * (CELL + 30), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(24, 26, 34));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        for (CarDef c : Cars.all()) {
            int i = c.index();
            int ox = (i % cols) * CELL;
            int oy = (i / cols) * (CELL + 30);
            draw(img, c, ox, oy, yaw, pitch);
            g.setColor(new Color(c.rarity.rgb));
            g.drawString(c.number + ". " + c.name, ox + 8, oy + CELL + 20);
        }
        ImageIO.write(img, "png", new File(out));
        System.out.println("ok " + out);
    }

    static void draw(BufferedImage img, CarDef c, int ox, int oy, double yaw, double pitch) {
        List<float[][]> quads = new ArrayList<>();
        List<int[]> info = new ArrayList<>();
        CarMesh.build(c.style, (p0, p1, p2, p3, rgb, em) -> {
            quads.add(new float[][] {p0.clone(), p1.clone(), p2.clone(), p3.clone()});
            info.add(new int[] {rgb, em ? 1 : 0});
        }, 3.0F, CarMesh.MODE_NORMAL);
        double cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch);
        double scale = CELL / 21.0;
        float[] zbuf = new float[CELL * CELL];
        java.util.Arrays.fill(zbuf, Float.NEGATIVE_INFINITY);
        // luz como la de Minecraft (dos direcciones) + ambiente
        double[] l1 = norm(0.2, 1.0, -0.7);
        double[] l2 = norm(-0.2, 1.0, 0.7);
        for (int q = 0; q < quads.size(); q++) {
            float[][] p = quads.get(q);
            double[][] v = new double[4][];
            for (int k = 0; k < 4; k++) {
                double x = p[k][0], y = p[k][1] - 3.0, z = p[k][2];
                double x1 = x * cy - z * sy;
                double z1 = x * sy + z * cy;
                double y2 = y * cp - z1 * sp;
                double z2 = y * sp + z1 * cp;
                v[k] = new double[] {CELL / 2.0 + x1 * scale, CELL / 2.0 - y2 * scale, z2};
            }
            double[] n = cross(sub(p[1], p[0]), sub(p[3], p[0]));
            if (len(n) < 1e-9) {
                n = cross(sub(p[2], p[1]), sub(p[0], p[1]));
            }
            if (len(n) < 1e-9) {
                continue;
            }
            n = norm(n[0], n[1], n[2]);
            double lit = info.get(q)[1] == 1 ? 1.0 : Math.min(1.0, 0.4 + 0.6 * (Math.max(0, dot(n, l1)) + Math.max(0, dot(n, l2))) * 0.75);
            int rgb = info.get(q)[0];
            int r = (int) Math.min(255, ((rgb >> 16) & 255) * lit);
            int gg = (int) Math.min(255, ((rgb >> 8) & 255) * lit);
            int b = (int) Math.min(255, (rgb & 255) * lit);
            int col = (r << 16) | (gg << 8) | b;
            tri(img, zbuf, ox, oy, v[0], v[1], v[2], col);
            tri(img, zbuf, ox, oy, v[0], v[2], v[3], col);
        }
    }

    static void tri(BufferedImage img, float[] zb, int ox, int oy, double[] a, double[] b, double[] c, int col) {
        int minX = (int) Math.max(0, Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
        int maxX = (int) Math.min(CELL - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
        int minY = (int) Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
        int maxY = (int) Math.min(CELL - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
        double area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
        if (Math.abs(area) < 1e-9) {
            return;
        }
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double px = x + 0.5, py = y + 0.5;
                double w0 = ((b[0] - px) * (c[1] - py) - (b[1] - py) * (c[0] - px)) / area;
                double w1 = ((c[0] - px) * (a[1] - py) - (c[1] - py) * (a[0] - px)) / area;
                double w2 = 1 - w0 - w1;
                if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) {
                    continue;
                }
                float z = (float) (w0 * a[2] + w1 * b[2] + w2 * c[2]);
                int i = y * CELL + x;
                if (z > zb[i]) {
                    zb[i] = z;
                    img.setRGB(ox + x, oy + y, col);
                }
            }
        }
    }

    static double[] sub(float[] a, float[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double len(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] norm(double x, double y, double z) {
        double l = Math.sqrt(x * x + y * y + z * z);
        return new double[] {x / l, y / l, z / l};
    }
}
