package com.mundosdeaxelis.vfx;

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Herramienta (no va en el mod): retrato de cada mascota 3D en vista 3/4, para el catálogo. Usa el rasterizador de {@link PetBake}.
 * Uso: java -cp <clases del mod>:<esta carpeta> com.mundosdeaxelis.vfx.PetCatalog <assets/mundos_de_axelis> <lista.txt> <carpeta salida>
 * Cada línea de la lista: id especie tamaño vuela(0/1) rgbHex. Especie -1 = el dragoncito (Dragon3D).
 */
public final class PetCatalog {
    public static void main(String[] a) throws Exception {
        PetBake.assets = a[0];
        File out = new File(a[2]);
        out.mkdirs();
        int W = 300, SS = 3;
        for (String line : Files.readAllLines(new File(a[1]).toPath())) {
            String[] p = line.trim().split("\\s+");
            if (p.length < 5) {
                continue;
            }
            String id = p[0];
            int species = Integer.parseInt(p[1]);
            double size = Double.parseDouble(p[2]);
            boolean fly = p[3].equals("1");
            int rgb = Integer.parseInt(p[4], 16);
            V3 center = new V3(0, 0.55 * size, 0);
            Cam cam = PetBake.camFrom(32, 14, center);
            List<PetBake.Q> qs = species < 0 ? dragon(cam) : PetBake.build(species, size, 40, 0, cam, rgb, fly);
            PetBake.Canvas cv = new PetBake.Canvas(W * SS, W * SS);
            Object[] ft = PetBake.fit(qs, cam, W * SS, W * SS, 0.80);
            PetBake.render(cv, qs, cam, (V3) ft[0], (double) ft[1]);
            BufferedImage big = PetBake.toImage(cv, 0);
            BufferedImage small = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = small.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(big.getScaledInstance(W, W, java.awt.Image.SCALE_AREA_AVERAGING), 0, 0, null);
            g.dispose();
            ImageIO.write(small, "png", new File(out, id + ".png"));
            System.out.println("ok " + id);
        }
    }

    static List<PetBake.Q> dragon(Cam cam) {
        List<PetBake.Q> list = new ArrayList<>();
        VfxData d = new VfxData();
        d.size = 1.3F;
        d.id = 3;
        d.rgb = 0xFF7A3A;
        d.yawInit = true;
        d.yaw = d.pyaw = 0;
        Dragon3D.emit(d, 40, 0.0, new V3(0, 0, 0), cam, (t, b, p, uv, col) -> list.add(new PetBake.Q(t, b, p.clone(), uv.clone(), col.clone())));
        return list;
    }
}
