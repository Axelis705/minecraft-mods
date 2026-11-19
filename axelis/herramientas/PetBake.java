package com.mundosdeaxelis.vfx;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Herramienta (no va en el mod): dibuja una mascota 3D de {@link Creature3D} sin Minecraft, con un rasterizador propio.
 * Saca: vistas grandes para revisar, el icono animado de 32x32 (12 cuadros, como los demás) y la figurita 3D del objeto
 * (modelo de bloques 2,5D con paleta, como las de los otros compañeros).
 *
 * Uso: java -cp <clases del mod>:<esta carpeta> com.mundosdeaxelis.vfx.PetBake <especie> <tamaño> <nombre> <carpeta assets/mundos_de_axelis> <carpeta vistas> <rgb hex>
 */
public final class PetBake {
    record Q(int tex, int blend, double[] p, float[] uv, float[] col) {
    }

    static final Map<Integer, BufferedImage> TEX = new LinkedHashMap<>();
    static String assets;

    static BufferedImage tex(int i) {
        return TEX.computeIfAbsent(i, k -> {
            try {
                BufferedImage im = ImageIO.read(new File(assets + "/textures/vfx/" + VfxTex.FILE[k] + ".png"));
                int fr = VfxTex.FRAMES[k];
                if (fr > 1 && k != VfxTex.ZZZ) {
                    im = im.getSubimage(0, 0, im.getWidth(), im.getHeight() / fr);
                }
                return im;
            } catch (Exception e) {
                return null;
            }
        });
    }

    static List<Q> build(int species, double size, double age, double yawDeg, Cam cam, int rgb, boolean fly) {
        List<Q> out = new ArrayList<>();
        VfxData d = new VfxData();
        d.b = species;
        d.size = (float) size;
        d.id = 3;
        d.rgb = rgb;
        d.life = 0;
        d.yawInit = true;
        d.yaw = d.pyaw = Math.toRadians(yawDeg);
        Creature3D.emit(d, age, 0.0, new V3(0, 0, 0), cam, (t, b, p, uv, col) -> out.add(new Q(t, b, p.clone(), uv.clone(), col.clone())));
        return out;
    }

    /** Lienzo con profundidad: color premultiplicado + z. */
    static final class Canvas {
        final int w, h;
        final double[] r, g, b, a, z;

        Canvas(int w, int h) {
            this.w = w;
            this.h = h;
            r = new double[w * h];
            g = new double[w * h];
            b = new double[w * h];
            a = new double[w * h];
            z = new double[w * h];
            java.util.Arrays.fill(z, Double.MAX_VALUE);
        }
    }

    /** Vista ortográfica: cámara con ejes, centro y escala (píxeles por unidad). */
    static void render(Canvas cv, List<Q> qs, Cam cam, V3 center, double ppu) {
        List<double[]> trans = new ArrayList<>();
        // 1) opacos (SOLID, y NORMAL casi opaco) con z; 2) translúcidos de lejos a cerca; 3) aditivos
        for (int pass = 0; pass < 3; pass++) {
            List<Q> list = new ArrayList<>();
            for (Q q : qs) {
                boolean add = q.blend == Sink.ADD;
                double am = (q.col[3] + q.col[7] + q.col[11] + q.col[15]) / 4;
                boolean opaque = !add && am > 0.98 && q.tex == VfxTex.CREATURE3D && !decalCell(q.uv);
                if ((pass == 0 && opaque) || (pass == 1 && !add && !opaque) || (pass == 2 && add)) {
                    list.add(q);
                }
            }
            if (pass == 1) {
                list.sort((x, y) -> Double.compare(depth(y, cam), depth(x, cam)));
            }
            for (Q q : list) {
                tri(cv, q, 0, 1, 2, cam, center, ppu, pass);
                tri(cv, q, 0, 2, 3, cam, center, ppu, pass);
            }
        }
    }

    /** Las decalcomanías (ojos, boca, mejillas) usan celdas con transparencia. */
    static boolean decalCell(float[] uv) {
        // la geometría sólida usa un solo punto de la textura (las 4 esquinas con la misma UV); las pegatinas, una celda
        for (int i = 2; i < 8; i++) {
            if (Math.abs(uv[i] - uv[i % 2]) > 1.0E-4) {
                return true;
            }
        }
        return false;
    }

    static double depth(Q q, Cam cam) {
        double s = 0;
        for (int i = 0; i < 4; i++) {
            s += (q.p[i * 3] - cam.pos.x) * cam.fwd.x + (q.p[i * 3 + 1] - cam.pos.y) * cam.fwd.y + (q.p[i * 3 + 2] - cam.pos.z) * cam.fwd.z;
        }
        return s / 4;
    }

    static void tri(Canvas cv, Q q, int i0, int i1, int i2, Cam cam, V3 center, double ppu, int pass) {
        int[] ids = {i0, i1, i2};
        double[] sx = new double[3], sy = new double[3], sz = new double[3];
        for (int k = 0; k < 3; k++) {
            int i = ids[k];
            double px = q.p[i * 3] - center.x, py = q.p[i * 3 + 1] - center.y, pz = q.p[i * 3 + 2] - center.z;
            sx[k] = cv.w / 2.0 + (px * cam.right.x + py * cam.right.y + pz * cam.right.z) * ppu;
            sy[k] = cv.h / 2.0 - (px * cam.up.x + py * cam.up.y + pz * cam.up.z) * ppu;
            sz[k] = (q.p[i * 3] - cam.pos.x) * cam.fwd.x + (q.p[i * 3 + 1] - cam.pos.y) * cam.fwd.y + (q.p[i * 3 + 2] - cam.pos.z) * cam.fwd.z;
        }
        double area = (sx[1] - sx[0]) * (sy[2] - sy[0]) - (sx[2] - sx[0]) * (sy[1] - sy[0]);
        if (Math.abs(area) < 1e-9) {
            return;
        }
        int x0 = (int) Math.max(0, Math.floor(Math.min(sx[0], Math.min(sx[1], sx[2]))));
        int x1 = (int) Math.min(cv.w - 1, Math.ceil(Math.max(sx[0], Math.max(sx[1], sx[2]))));
        int y0 = (int) Math.max(0, Math.floor(Math.min(sy[0], Math.min(sy[1], sy[2]))));
        int y1 = (int) Math.min(cv.h - 1, Math.ceil(Math.max(sy[0], Math.max(sy[1], sy[2]))));
        BufferedImage tx = tex(q.tex);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                double px = x + 0.5, py = y + 0.5;
                double w0 = ((sx[1] - px) * (sy[2] - py) - (sx[2] - px) * (sy[1] - py)) / area;
                double w1 = ((sx[2] - px) * (sy[0] - py) - (sx[0] - px) * (sy[2] - py)) / area;
                double w2 = 1 - w0 - w1;
                if (w0 < 0 || w1 < 0 || w2 < 0) {
                    continue;
                }
                double z = w0 * sz[0] + w1 * sz[1] + w2 * sz[2];
                int idx = y * cv.w + x;
                if (z > cv.z[idx] + (pass == 2 ? 0.02 : 1e-4)) {
                    continue;
                }
                double u = 0, v = 0;
                double[] col = new double[4];
                double[] ww = {w0, w1, w2};
                for (int k = 0; k < 3; k++) {
                    int i = ids[k];
                    u += ww[k] * q.uv[i * 2];
                    v += ww[k] * q.uv[i * 2 + 1];
                    for (int c = 0; c < 4; c++) {
                        col[c] += ww[k] * q.col[i * 4 + c];
                    }
                }
                double tr = 1, tg = 1, tb = 1, ta = 1;
                if (tx != null) {
                    int ix = (int) Math.min(tx.getWidth() - 1, Math.max(0, u * tx.getWidth()));
                    int iy = (int) Math.min(tx.getHeight() - 1, Math.max(0, v * tx.getHeight()));
                    int argb = tx.getRGB(ix, iy);
                    ta = (argb >>> 24) / 255.0;
                    tr = (argb >> 16 & 255) / 255.0;
                    tg = (argb >> 8 & 255) / 255.0;
                    tb = (argb & 255) / 255.0;
                }
                double cr = col[0] * tr, cg = col[1] * tg, cb = col[2] * tb, ca = col[3] * ta;
                if (pass == 2) {
                    // aditivo: suma luz (pesada por alfa)
                    cv.r[idx] += cr * ca;
                    cv.g[idx] += cg * ca;
                    cv.b[idx] += cb * ca;
                    cv.a[idx] = Math.min(1, cv.a[idx] + ca * 0.5 * Math.max(cr, Math.max(cg, cb)));
                    continue;
                }
                if (ca < 0.02) {
                    continue;
                }
                if (pass == 0 && ca > 0.5) {
                    cv.r[idx] = cr;
                    cv.g[idx] = cg;
                    cv.b[idx] = cb;
                    cv.a[idx] = 1;
                    cv.z[idx] = z;
                } else {
                    cv.r[idx] = cr * ca + cv.r[idx] * (1 - ca);
                    cv.g[idx] = cg * ca + cv.g[idx] * (1 - ca);
                    cv.b[idx] = cb * ca + cv.b[idx] * (1 - ca);
                    cv.a[idx] = ca + cv.a[idx] * (1 - ca);
                    if (ca > 0.6) {
                        cv.z[idx] = z;
                    }
                }
            }
        }
    }

    static BufferedImage toImage(Canvas cv, int bg) {
        BufferedImage im = new BufferedImage(cv.w, cv.h, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < cv.w * cv.h; i++) {
            double a = Math.min(1, cv.a[i]);
            int r, g, b;
            if (bg != 0) {
                r = (int) Math.min(255, (cv.r[i] + (bg >> 16 & 255) / 255.0 * (1 - a)) * 255);
                g = (int) Math.min(255, (cv.g[i] + (bg >> 8 & 255) / 255.0 * (1 - a)) * 255);
                b = (int) Math.min(255, (cv.b[i] + (bg & 255) / 255.0 * (1 - a)) * 255);
                im.setRGB(i % cv.w, i / cv.w, 0xFF000000 | r << 16 | g << 8 | b);
            } else {
                double k = a > 1e-4 ? 1 / a : 0;
                r = (int) Math.min(255, cv.r[i] * k * 255);
                g = (int) Math.min(255, cv.g[i] * k * 255);
                b = (int) Math.min(255, cv.b[i] * k * 255);
                im.setRGB(i % cv.w, i / cv.w, (int) (a * 255) << 24 | r << 16 | g << 8 | b);
            }
        }
        return im;
    }

    /** Caja de lo sólido (sin brillos aditivos): min x,y,z y max x,y,z. */
    static double[] bbox(List<Q> qs) {
        double[] b = {1e9, 1e9, 1e9, -1e9, -1e9, -1e9};
        for (Q q : qs) {
            if (q.blend == Sink.ADD || q.tex == VfxTex.SOFT || q.tex == VfxTex.ZZZ) {
                continue;
            }
            for (int i = 0; i < 4; i++) {
                for (int k = 0; k < 3; k++) {
                    b[k] = Math.min(b[k], q.p[i * 3 + k]);
                    b[k + 3] = Math.max(b[k + 3], q.p[i * 3 + k]);
                }
            }
        }
        return b;
    }

    /** Encuadre: centro y píxeles por unidad para que lo sólido ocupe {@code fill} del lienzo. */
    static Object[] fit(List<Q> qs, Cam cam, int w, int h, double fill) {
        double[] b = bbox(qs);
        V3 c = new V3((b[0] + b[3]) / 2, (b[1] + b[4]) / 2, (b[2] + b[5]) / 2);
        double ex = 0, ey = 0;
        for (int i = 0; i < 8; i++) {
            V3 p = new V3(b[(i & 1) == 0 ? 0 : 3], b[(i & 2) == 0 ? 1 : 4], b[(i & 4) == 0 ? 2 : 5]).sub(c);
            ex = Math.max(ex, Math.abs(p.dot(cam.right)));
            ey = Math.max(ey, Math.abs(p.dot(cam.up)));
        }
        double ppu = Math.min(w * fill / (2 * ex), h * fill / (2 * ey));
        return new Object[]{c, ppu};
    }

    static Cam camFrom(double yawDeg, double pitchDeg, V3 target) {
        double y = Math.toRadians(yawDeg), p = Math.toRadians(pitchDeg);
        V3 dir = new V3(Math.sin(y) * Math.cos(p), Math.sin(p), Math.cos(y) * Math.cos(p));
        return Cam.lookAt(target.add(dir.mul(6)), target);
    }

    public static void main(String[] a) throws Exception {
        Creature3D.SHADOWS = false;
        int species = Integer.parseInt(a[0]);
        double size = Double.parseDouble(a[1]);
        String name = a[2];
        assets = a[3];
        String views = a[4];
        int rgb = Integer.parseInt(a[5], 16);
        boolean fly = a.length > 6 && a[6].equals("vuela");
        new File(views).mkdirs();
        V3 center = new V3(0, 0.55 * size, 0);

        // 1) vistas grandes para revisar (frente, 3/4, lado, espalda)
        double[][] vs = {{0, 8}, {35, 15}, {90, 5}, {180, 10}};
        BufferedImage sheet = new BufferedImage(512 * 4, 512, BufferedImage.TYPE_INT_ARGB);
        for (int k = 0; k < vs.length; k++) {
            Cam cam = camFrom(vs[k][0], vs[k][1], center);
            Canvas cv = new Canvas(512, 512);
            List<Q> qv = build(species, size, 40, 0, cam, rgb, fly);
            Object[] ft = fit(qv, cam, 512, 512, 0.82);
            render(cv, qv, cam, (V3) ft[0], (double) ft[1]);
            sheet.getGraphics().drawImage(toImage(cv, 0x2A2A34), 512 * k, 0, null);
        }
        ImageIO.write(sheet, "png", new File(views, name + "_vistas.png"));

        // 2) icono animado 32x32, 12 cuadros (vista 3/4 como los demás iconos)
        int N = 12, S = 32, SS = 8;
        BufferedImage strip = new BufferedImage(S, S * N, BufferedImage.TYPE_INT_ARGB);
        BufferedImage big = new BufferedImage(S * 4 * N, S * 4, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < N; f++) {
            Cam cam = camFrom(28, 14, center);
            Canvas cv = new Canvas(S * SS, S * SS);
            double age = 40 + f * (96.0 / N) * 0.6;
            List<Q> qi = build(species, size, age, 0, cam, rgb, fly);
            Object[] ft = fit(build(species, size, 40, 0, cam, rgb, fly), cam, S * SS, S * SS, 0.86);
            V3 fc = ((V3) ft[0]).add(cam.up.mul(-0.03 * S * SS / (double) ft[1]));
            render(cv, qi, cam, fc, (double) ft[1]);
            BufferedImage frame = downsample(cv, SS, rgb, f, N);
            strip.getGraphics().drawImage(frame, 0, S * f, null);
            big.getGraphics().drawImage(frame.getScaledInstance(S * 4, S * 4, java.awt.Image.SCALE_FAST), S * 4 * f, 0, null);
        }
        new File(assets + "/textures/item").mkdirs();
        ImageIO.write(strip, "png", new File(assets + "/textures/item/" + name + ".png"));
        try (FileWriter fw = new FileWriter(assets + "/textures/item/" + name + ".png.mcmeta")) {
            fw.write("{\"animation\": {\"frametime\": 3}}");
        }
        ImageIO.write(big, "png", new File(views, name + "_icono.png"));

        // 3) figurita 2,5D: vista de frente ortográfica a 0,5 px de modelo; cada píxel es una caja con la profundidad del cuerpo
        figurine(species, size, name, rgb, fly, views);
        System.out.println("ok " + name);
    }

    /** Reduce el lienzo grande a 32x32 (media de cada bloque) y añade el halo y la sombra suaves de los otros iconos. */
    static BufferedImage downsample(Canvas cv, int ss, int rgb, int f, int n) {
        int w = cv.w / ss, h = cv.h / ss;
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        double gr = (rgb >> 16 & 255) / 255.0, gg = (rgb >> 8 & 255) / 255.0, gb = (rgb & 255) / 255.0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double r = 0, g = 0, b = 0, a = 0;
                for (int j = 0; j < ss; j++) {
                    for (int i = 0; i < ss; i++) {
                        int idx = (y * ss + j) * cv.w + x * ss + i;
                        double aa = Math.min(1, cv.a[idx]);
                        r += cv.r[idx];
                        g += cv.g[idx];
                        b += cv.b[idx];
                        a += aa;
                    }
                }
                int m = ss * ss;
                r /= m;
                g /= m;
                b /= m;
                a /= m;
                // sombra de color bajo los pies (elipse suave)
                double ex = (x + 0.5 - w / 2.0) / (w * 0.30), ey = (y + 0.5 - h * 0.93) / (h * 0.06);
                double sh = Math.max(0, 1 - (ex * ex + ey * ey)) * 0.55 * (1 - a);
                r += gr * sh * 0.6;
                g += gg * sh * 0.6;
                b += gb * sh * 0.6;
                a += sh;
                if (a < 0.08) {
                    im.setRGB(x, y, 0);
                    continue;
                }
                double k = 1 / Math.min(1, a);
                a = Math.min(1, a < 0.5 ? a : Math.min(1, a * 1.25));
                im.setRGB(x, y, (int) (a * 255) << 24 | (int) Math.min(255, r * k * 255) << 16 | (int) Math.min(255, g * k * 255) << 8
                        | (int) Math.min(255, b * k * 255));
            }
        }
        // chispitas que suben (cambian con el cuadro)
        java.util.Random rnd = new java.util.Random(7);
        for (int s = 0; s < 3; s++) {
            int sx = 3 + rnd.nextInt(w - 6);
            double ph = rnd.nextDouble();
            double u = ((double) f / n + ph) % 1.0;
            int sy = (int) (h * 0.8 - u * h * 0.7);
            if (sy > 1 && sy < h - 1 && ((im.getRGB(sx, sy) >>> 24) < 40)) {
                int al = (int) (200 * Math.sin(u * Math.PI));
                int c = (int) Math.min(255, gr * 140 + 115) << 16 | (int) Math.min(255, gg * 140 + 115) << 8 | (int) Math.min(255, gb * 140 + 115);
                im.setRGB(sx, sy, al << 24 | c);
            }
        }
        return im;
    }

    static void figurine(int species, double size, String name, int rgb, boolean fly, String views) throws Exception {
        V3 center = new V3(0, 0.55 * size, 0);
        double unit = 0.5;                  // tamaño de cada caja (unidades del modelo de objeto)
        int ss = 4;
        Cam front = camFrom(0, 0, center);
        Cam back = camFrom(180, 0, center);
        List<Q> qs = build(species, size, 40, 0, front, rgb, fly);
        double[] bb = bbox(qs);
        double wid = bb[3] - bb[0], hei = bb[4] - bb[1];
        double k = Math.min(17.0 / hei, 15.0 / wid);   // unidades del objeto por bloque del mundo
        int px = (int) Math.ceil(wid * k / unit) + 2, py = (int) Math.ceil(hei * k / unit) + 1;
        double ppu = k / unit * ss;
        Canvas cf = new Canvas(px * ss, py * ss), cb = new Canvas(px * ss, py * ss);
        V3 cen = new V3((bb[0] + bb[3]) / 2, bb[1] + (py * unit / 2.0) / k - 0.5 * unit / k, 0);
        double xOff = 8 - px * unit / 2.0;
        // solo lo sólido (sin brillos aditivos)
        List<Q> solid = new ArrayList<>();
        for (Q q : qs) {
            if (q.blend != Sink.ADD) {
                solid.add(q);
            }
        }
        render(cf, solid, front, cen, ppu);
        List<Q> qb = build(species, size, 40, 0, back, rgb, fly);
        List<Q> sb = new ArrayList<>();
        for (Q q : qb) {
            if (q.blend != Sink.ADD) {
                sb.add(q);
            }
        }
        render(cb, sb, back, cen, ppu);
        // paleta (máx. 256 colores) y cajas
        Map<Integer, Integer> pal = new LinkedHashMap<>();
        List<double[]> boxes = new ArrayList<>();
        int[][] colAt = new int[py][px];
        double[][] z0 = new double[py][px], z1 = new double[py][px];
        for (int y = 0; y < py; y++) {
            for (int x = 0; x < px; x++) {
                double r = 0, g = 0, b = 0, a = 0, zf = 0, zb = 0;
                int nf = 0, nb = 0;
                for (int j = 0; j < ss; j++) {
                    for (int i = 0; i < ss; i++) {
                        int idx = (y * ss + j) * cf.w + x * ss + i;
                        a += Math.min(1, cf.a[idx]);
                        r += cf.r[idx];
                        g += cf.g[idx];
                        b += cf.b[idx];
                        if (cf.z[idx] < Double.MAX_VALUE) {
                            zf += cf.z[idx];
                            nf++;
                        }
                        // la vista de atrás está espejada en x
                        int bidx = (y * ss + j) * cb.w + (cb.w - 1 - (x * ss + i));
                        if (cb.z[bidx] < Double.MAX_VALUE) {
                            zb += cb.z[bidx];
                            nb++;
                        }
                    }
                }
                a /= ss * ss;
                colAt[y][x] = -1;
                if (a < 0.45 || nf == 0) {
                    continue;
                }
                double kk = 1 / a;
                int R = (int) Math.min(255, r / (ss * ss) * kk * 255), G = (int) Math.min(255, g / (ss * ss) * kk * 255), B = (int) Math.min(255, b / (ss * ss) * kk * 255);
                // cuantizar a 5 bits por canal para que la paleta quepa
                int key = (R >> 3) << 10 | (G >> 3) << 5 | (B >> 3);
                pal.putIfAbsent(key, pal.size());
                colAt[y][x] = key;
                // profundidad: delante = 6 - z del frente; detrás = z de atrás - 6 (la cámara está a 6 bloques)
                double front_ = 6 - zf / nf, back_ = nb > 0 ? (zb / nb) - 6 : -front_;
                double thick = Math.max(0.15 / size, front_ - back_);
                double d = Math.max(3, Math.min(10, Math.round(thick * k * 0.62 / 0.84))) * 0.84;
                z0[y][x] = 8 - d / 2;
                z1[y][x] = 8 + d / 2;
            }
        }
        // si no cabe la paleta (mucho degradado), se juntan los colores parecidos: 4 bits por canal en vez de 5
        if (pal.size() > 256) {
            pal.clear();
            for (int y = 0; y < py; y++) {
                for (int x = 0; x < px; x++) {
                    int c = colAt[y][x];
                    if (c >= 0) {
                        int r5 = c >> 10 & 31, g5 = c >> 5 & 31, b5 = c & 31;
                        int k2 = (r5 & ~1) << 10 | (g5 & ~1) << 5 | (b5 & ~1);
                        colAt[y][x] = k2;
                        pal.putIfAbsent(k2, pal.size());
                    }
                }
            }
        }
        if (pal.size() > 256) {
            throw new IllegalStateException("demasiados colores: " + pal.size());
        }
        // unir píxeles vecinos en fila con el mismo color y profundidad
        StringBuilder el = new StringBuilder();
        int count = 0;
        for (int y = 0; y < py; y++) {
            int x = 0;
            while (x < px) {
                int c = colAt[y][x];
                if (c < 0) {
                    x++;
                    continue;
                }
                int x2 = x + 1;
                while (x2 < px && colAt[y][x2] == c && z0[y][x2] == z0[y][x] && z1[y][x2] == z1[y][x]) {
                    x2++;
                }
                int pi = pal.get(c);
                double u = (pi % 16) + 0.35, v = (pi / 16) + 0.35;
                String uv = String.format(Locale.ROOT, "[%.2f, %.2f, %.2f, %.2f]", u, v, u + 0.3, v + 0.3);
                double fx = xOff + x * unit, tx = xOff + x2 * unit;
                double fy = (py - 1 - y) * unit, ty = fy + unit;
                if (count > 0) {
                    el.append(", ");
                }
                el.append(String.format(Locale.ROOT, "{\"from\": [%.2f, %.2f, %.2f], \"to\": [%.2f, %.2f, %.2f], \"faces\": {", fx, fy, z0[y][x], tx, ty, z1[y][x]));
                String[] faces = {"north", "east", "south", "west", "up", "down"};
                for (int i = 0; i < 6; i++) {
                    el.append(i > 0 ? ", " : "").append("\"").append(faces[i]).append("\": {\"uv\": ").append(uv).append(", \"texture\": \"#0\"}");
                }
                el.append("}}");
                count++;
                x = x2;
            }
        }
        BufferedImage palImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (Map.Entry<Integer, Integer> e : pal.entrySet()) {
            int key = e.getKey(), i = e.getValue();
            int R = (key >> 10 & 31) << 3 | 4, G = (key >> 5 & 31) << 3 | 4, B = (key & 31) << 3 | 4;
            palImg.setRGB(i % 16, i / 16, 0xFF000000 | R << 16 | G << 8 | B);
        }
        new File(assets + "/textures/item/relic3d").mkdirs();
        new File(assets + "/models/item/relic3d").mkdirs();
        ImageIO.write(palImg, "png", new File(assets + "/textures/item/relic3d/" + name + ".png"));
        String t = "mundos_de_axelis:item/relic3d/" + name;
        String display = "{\"thirdperson_righthand\": {\"rotation\": [0, 0, 0], \"translation\": [0, 3, 1], \"scale\": [0.55, 0.55, 0.55]}, "
                + "\"thirdperson_lefthand\": {\"rotation\": [0, 0, 0], \"translation\": [0, 3, 1], \"scale\": [0.55, 0.55, 0.55]}, "
                + "\"firstperson_righthand\": {\"rotation\": [0, -90, 25], \"translation\": [1.13, 3.2, 1.13], \"scale\": [0.68, 0.68, 0.68]}, "
                + "\"firstperson_lefthand\": {\"rotation\": [0, 90, -25], \"translation\": [1.13, 3.2, 1.13], \"scale\": [0.68, 0.68, 0.68]}, "
                + "\"ground\": {\"rotation\": [0, 0, 0], \"translation\": [0, 3, 0], \"scale\": [0.7, 0.7, 0.7]}, "
                + "\"fixed\": {\"rotation\": [0, 180, 0], \"translation\": [0, 0, 0], \"scale\": [0.95, 0.95, 0.95]}, "
                + "\"head\": {\"rotation\": [0, 180, 0], \"translation\": [0, 13, 7], \"scale\": [1, 1, 1]}, "
                + "\"gui\": {\"rotation\": [25, -35, 0], \"translation\": [0, 0, 0], \"scale\": [0.9, 0.9, 0.9]}}";
        try (FileWriter fw = new FileWriter(assets + "/models/item/relic3d/" + name + "_3d.json")) {
            fw.write("{\"textures\": {\"0\": \"" + t + "\", \"particle\": \"" + t + "\"}, \"elements\": [" + el + "], \"display\": " + display + "}");
        }
        try (FileWriter fw = new FileWriter(assets + "/models/item/" + name + ".json")) {
            fw.write("{\n \"loader\": \"forge:separate_transforms\",\n \"gui_light\": \"front\",\n \"base\": {\n  \"parent\": \"mundos_de_axelis:item/relic3d/" + name
                    + "_3d\"\n },\n \"perspectives\": {\n  \"gui\": {\n   \"parent\": \"minecraft:item/generated\",\n   \"textures\": {\n    \"layer0\": \"mundos_de_axelis:item/"
                    + name + "\"\n   }\n  }\n }\n}\n");
        }
        // vista previa de la figurita (frente y costado de las cajas, para revisar)
        BufferedImage prev = new BufferedImage(px * 12, py * 12, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < py; y++) {
            for (int x = 0; x < px; x++) {
                if (colAt[y][x] >= 0) {
                    int key = colAt[y][x];
                    int R = (key >> 10 & 31) << 3, G = (key >> 5 & 31) << 3, B = (key & 31) << 3;
                    for (int j = 0; j < 12; j++) {
                        for (int i = 0; i < 12; i++) {
                            prev.setRGB(x * 12 + i, y * 12 + j, 0xFF000000 | R << 16 | G << 8 | B);
                        }
                    }
                }
            }
        }
        ImageIO.write(prev, "png", new File(views, name + "_figurita.png"));
        System.out.println("figurita: " + count + " cajas, " + pal.size() + " colores");
    }
}
