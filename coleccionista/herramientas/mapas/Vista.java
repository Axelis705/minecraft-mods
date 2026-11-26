package com.coleccionista.mapa;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dibuja un {@link Plano} sin Minecraft, para revisarlo con los ojos antes de construirlo:
 * <ul>
 * <li>{@link #planta}: vista desde arriba de un piso (corta todo lo que está arriba de {@code yTecho}): cada bloque con su color, en verde
 * donde se puede caminar (se llega desde la entrada), en rojo donde se puede estar pero no se llega, puertas en amarillo, marcas en blanco y
 * los problemas del {@link Revisor} en morado;</li>
 * <li>{@link #iso}: vista en 3D (isométrica, de una esquina) cortada a una altura: se ven los cuartos por dentro, como una casa de muñecas.</li>
 * </ul>
 * Los colores de los bloques del complejo salen del promedio de sus texturas; los de Minecraft, de su nombre.
 */
public final class Vista {
    private final Plano p;
    private final Revisor r;
    private final Path texturas;
    private final Map<String, Color> colores = new HashMap<>();

    public Vista(Plano p, Revisor r, Path texturas) {
        this.p = p;
        this.r = r;
        this.texturas = texturas;
    }

    // ------------------------------------------------------------------ colores

    private Color color(String s) {
        int c = s.indexOf('[');
        String id = c >= 0 ? s.substring(0, c) : s;
        return colores.computeIfAbsent(id, this::calcularColor);
    }

    private Color calcularColor(String id) {
        String n = id.substring(id.indexOf(':') + 1);
        if (id.startsWith("coleccionista:") && texturas != null) {
            for (String f : new String[] {n + ".png", n + "_0.png", n + "_top.png", n + "_lado.png", n + "_frente.png"}) {
                File a = texturas.resolve(f).toFile();
                if (a.exists()) {
                    try {
                        return promedio(ImageIO.read(a));
                    } catch (IOException ignored) {
                        // sigue con el siguiente
                    }
                }
            }
        }
        // los de Minecraft, por su nombre
        String[][] t = {{"dark_oak", "4a3222"}, {"spruce", "6b4f30"}, {"oak", "a8874f"}, {"birch", "c8b778"}, {"stone_brick", "7a7a7a"},
                {"deepslate", "4a4a50"}, {"stone", "828282"}, {"cobble", "6e6e6e"}, {"brick", "8a4a3a"}, {"glass", "b8d8e8"}, {"iron", "c8c8c8"},
                {"gold", "e8c040"}, {"red", "a02828"}, {"black", "1c1c20"}, {"white", "e8e8e8"}, {"gray", "5a5a5a"}, {"blue", "3050a0"},
                {"green", "4a7a30"}, {"yellow", "e0c030"}, {"orange", "e07a20"}, {"purple", "7a3aa0"}, {"magenta", "b040a0"}, {"pink", "e08aa8"},
                {"lime", "70c030"}, {"cyan", "30a0a0"}, {"brown", "6a4a2a"}, {"water", "3a5ad0"}, {"lava", "e06010"}, {"dripstone", "8a6a50"},
                {"chain", "404048"}, {"bars", "5a5a60"}, {"wool", "d8d8d8"}, {"quartz", "e8e0d8"}, {"terracotta", "a0604a"}, {"dirt", "7a5a3a"},
                {"grass", "5a8a3a"}};
        for (String[] e : t) {
            if (n.contains(e[0])) {
                return new Color(Integer.parseInt(e[1], 16));
            }
        }
        return new Color(0x9a, 0x8a, 0x7a);
    }

    private static Color promedio(BufferedImage im) {
        long rr = 0, gg = 0, bb = 0, n = 0;
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                int a = im.getRGB(x, y);
                if ((a >>> 24) < 100) {
                    continue;
                }
                rr += (a >> 16) & 255;
                gg += (a >> 8) & 255;
                bb += a & 255;
                n++;
            }
        }
        return n == 0 ? Color.GRAY : new Color((int) (rr / n), (int) (gg / n), (int) (bb / n));
    }

    private static Color mezclar(Color a, Color b, double t) {
        return new Color((int) (a.getRed() * (1 - t) + b.getRed() * t), (int) (a.getGreen() * (1 - t) + b.getGreen() * t),
                (int) (a.getBlue() * (1 - t) + b.getBlue() * t));
    }

    private static Color oscuro(Color c, double f) {
        return new Color((int) (c.getRed() * f), (int) (c.getGreen() * f), (int) (c.getBlue() * f));
    }

    private static boolean aire(String s) {
        return s == null || s.equals("air") || s.startsWith("light") || s.startsWith("minecraft:light");
    }

    // ------------------------------------------------------------------ desde arriba

    /**
     * La planta de un piso: lo de abajo de {@code yTecho} visto desde arriba (hasta {@code yPiso}); verde/rojo para los pies entre
     * {@code yPiso + 1} y {@code yTecho}. {@code marcas}: prefijo de las marcas que se dibujan (o null).
     */
    public void planta(File salida, int yPiso, int yTecho, String marcas, String titulo) throws IOException {
        int esc = 8, mx = 34, my = 40;
        int w = (p.x1 - p.x0 + 1) * esc + mx * 2, h = (p.z1 - p.z0 + 1) * esc + my + 24;
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(0x15, 0x12, 0x14));
        g.fillRect(0, 0, w, h);
        for (int x = p.x0; x <= p.x1; x++) {
            for (int z = p.z0; z <= p.z1; z++) {
                int px = mx + (x - p.x0) * esc, pz = my + (z - p.z0) * esc;
                Color c = null;
                int yTope = 0;
                for (int y = yTecho; y >= yPiso; y--) {
                    String s = p.get(x, y, z);
                    if (!aire(s)) {
                        c = color(s);
                        yTope = y;
                        break;
                    }
                }
                if (c == null) {
                    continue;
                }
                // más oscuro mientras más abajo (así se notan los pisos desfasados)
                c = oscuro(c, 0.55 + 0.45 * (yTope - yPiso) / Math.max(1.0, yTecho - yPiso));
                String s = p.get(x, yTope, z);
                boolean pared = true;
                for (int y = yPiso + 1; y <= Math.min(yTecho, yPiso + 3); y++) {
                    pared &= !aire(p.get(x, y, z));
                }
                if (s != null && (s.contains("door") || s.contains("puerta")) && !s.contains("trapdoor")) {
                    c = new Color(0xF0C020);
                }
                // dónde se camina
                boolean llega = false, esta = false;
                for (int y = yPiso + 1; y <= yTecho; y++) {
                    long k = Plano.key(x, y, z);
                    llega |= r != null && r.alcanzado.contains(k);
                    esta |= r != null && r.parado.contains(k);
                }
                if (llega) {
                    c = mezclar(c, new Color(0x40D060), 0.35);
                } else if (esta) {
                    c = mezclar(c, new Color(0xF03030), 0.6);
                } else if (pared) {
                    c = oscuro(c, 0.6);
                }
                g.setColor(c);
                g.fillRect(px, pz, esc, esc);
            }
        }
        // la cuadrícula cada 5 y las coordenadas
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        for (int x = p.x0; x <= p.x1; x++) {
            if (x % 5 == 0) {
                int px = mx + (x - p.x0) * esc;
                g.setColor(new Color(255, 255, 255, 40));
                g.drawLine(px, my, px, my + (p.z1 - p.z0 + 1) * esc);
                g.setColor(Color.LIGHT_GRAY);
                g.drawString(String.valueOf(x), px - 4, my - 4);
            }
        }
        for (int z = p.z0; z <= p.z1; z++) {
            if (z % 5 == 0) {
                int pz = my + (z - p.z0) * esc;
                g.setColor(new Color(255, 255, 255, 40));
                g.drawLine(mx, pz, mx + (p.x1 - p.x0 + 1) * esc, pz);
                g.setColor(Color.LIGHT_GRAY);
                g.drawString(String.valueOf(z), 4, pz + 4);
            }
        }
        // las marcas y los problemas
        if (marcas != null) {
            for (Map.Entry<String, Plano.Marca> e : p.marcas.entrySet()) {
                Plano.Marca m = e.getValue();
                if (!e.getKey().startsWith(marcas) || m.y() <= yPiso || m.y() > yTecho) {
                    continue;
                }
                int px = mx + (m.x() - p.x0) * esc, pz = my + (m.z() - p.z0) * esc;
                g.setColor(Color.WHITE);
                g.fillOval(px + 2, pz + 2, esc - 4, esc - 4);
            }
        }
        if (r != null) {
            g.setStroke(new BasicStroke(2));
            for (Revisor.Problema q : r.problemas) {
                if (q.y() < yPiso || q.y() > yTecho) {
                    continue;
                }
                int px = mx + (q.x() - p.x0) * esc, pz = my + (q.z() - p.z0) * esc;
                g.setColor(new Color(0xFF40FF));
                g.drawRect(px - 2, pz - 2, esc + 4, esc + 4);
            }
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        g.drawString(titulo + "  (y " + yPiso + " a " + yTecho + ")   verde: se llega · rojo: no se llega · amarillo: puertas · morado: problema",
                mx, 18);
        g.dispose();
        ImageIO.write(im, "png", salida);
    }

    // ------------------------------------------------------------------ en 3D (casa de muñecas)

    /**
     * Vista isométrica desde la esquina {@code esquina} (0: suroeste, 1: sureste, 2: noreste, 3: noroeste), con todo lo de arriba de
     * {@code yCorte} quitado (para ver adentro de los cuartos). {@code x0..x1, z0..z1}: el pedazo que se dibuja (o todo, con null).
     */
    public void iso(File salida, int yCorte, int esquina, int[] caja, int esc, String titulo) throws IOException {
        int ax = caja == null ? p.x0 : caja[0], az = caja == null ? p.z0 : caja[1], bx = caja == null ? p.x1 : caja[2],
                bz = caja == null ? p.z1 : caja[3];
        int yMin = p.y0;
        // el giro: se recorre para que lo de atrás se pinte primero
        int nx = bx - ax + 1, nz = bz - az + 1;
        double tw = esc, th = esc / 2.0, tz = esc * 0.82;
        int w = (int) ((nx + nz) * tw) + 40, h = (int) ((nx + nz) * th + (yCorte - yMin + 2) * tz) + 60;
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(new Color(0x26, 0x24, 0x2a));
        g.fillRect(0, 0, w, h);
        double ox = 20 + nz * tw, oy = 40 + (yCorte - yMin + 1) * tz;
        List<int[]> orden = new ArrayList<>();
        for (int i = 0; i < nx; i++) {
            for (int k = 0; k < nz; k++) {
                orden.add(new int[] {i, k});
            }
        }
        // de atrás hacia adelante: mientras más chica i+k (girado), más atrás
        orden.sort((a, b) -> Integer.compare(a[0] + a[1], b[0] + b[1]));
        for (int[] ik : orden) {
            for (int y = yMin; y <= yCorte; y++) {
                // (i, k) de la vista -> (x, z) del plano según la esquina
                int x, z;
                switch (esquina) {
                    case 1 -> {
                        x = bx - ik[0];
                        z = az + ik[1];
                    }
                    case 2 -> {
                        x = bx - ik[0];
                        z = bz - ik[1];
                    }
                    case 3 -> {
                        x = ax + ik[0];
                        z = bz - ik[1];
                    }
                    default -> {
                        x = ax + ik[0];
                        z = az + ik[1];
                    }
                }
                String s = p.get(x, y, z);
                if (aire(s)) {
                    continue;
                }
                Color c = color(s);
                double cx = ox + (ik[0] - ik[1]) * tw, cy = oy + (ik[0] + ik[1]) * th - (y - yMin) * tz;
                // arriba, izquierda y derecha (solo si no hay bloque que las tape)
                boolean tapaArriba = y < yCorte && !aire(p.get(x, y + 1, z));
                if (!tapaArriba) {
                    Polygon top = new Polygon(new int[] {(int) cx, (int) (cx + tw), (int) cx, (int) (cx - tw)},
                            new int[] {(int) (cy - th), (int) cy, (int) (cy + th), (int) cy}, 4);
                    g.setColor(c);
                    g.fillPolygon(top);
                }
                Polygon izq = new Polygon(new int[] {(int) (cx - tw), (int) cx, (int) cx, (int) (cx - tw)},
                        new int[] {(int) cy, (int) (cy + th), (int) (cy + th + tz), (int) (cy + tz)}, 4);
                Polygon der = new Polygon(new int[] {(int) cx, (int) (cx + tw), (int) (cx + tw), (int) cx},
                        new int[] {(int) (cy + th), (int) cy, (int) (cy + tz), (int) (cy + th + tz)}, 4);
                g.setColor(oscuro(c, 0.72));
                g.fillPolygon(izq);
                g.setColor(oscuro(c, 0.55));
                g.fillPolygon(der);
            }
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        g.drawString(titulo + " (cortado en y " + yCorte + ")", 12, 20);
        g.dispose();
        ImageIO.write(im, "png", salida);
    }
}
