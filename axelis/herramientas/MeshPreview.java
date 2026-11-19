package com.mundosdeaxelis.vfx;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Herramienta (no va en el mod): vistas de las mallas de {@link BossMesh} con el rasterizador de {@link PetBake}.
 * Uso: java -cp <clases del mod>:<out> com.mundosdeaxelis.vfx.MeshPreview <assets/mundos_de_axelis> <salida.png> <cosa> [t]
 * cosa = corazon | ancla | larva | escena
 */
public final class MeshPreview {
    interface Draw {
        void draw(Sink s, Cam cam, double t);
    }

    public static void main(String[] a) throws Exception {
        PetBake.assets = a[0];
        String what = a[2];
        double t = a.length > 3 ? Double.parseDouble(a[3]) : 30;
        Draw d = switch (what) {
            case "corazon" -> (s, cam, tt) -> BossMesh.heart(s, cam, new V3(0, 0, 0), 2.4, tt, BossMesh.beat(tt, 30), 14, 0.0, 0.3,
                    new V3[]{new V3(6, -3, 6), new V3(-6, -3, 6), new V3(6, -3, -6), new V3(-6, -3, -6)});
            case "corazon_roto" -> (s, cam, tt) -> BossMesh.heart(s, cam, new V3(0, 0, 0), 2.4, tt, BossMesh.beat(tt, 22), 9, 0.8, 0.8, null);
            case "ancla" -> (s, cam, tt) -> BossMesh.anchor(s, cam, new V3(0, 0, 0), tt, 0.0, 1.0);
            case "larva" -> (s, cam, tt) -> BossMesh.larva(s, cam, new V3(0, 0, 0), 1.0, tt, 0.5, 0.0, 0.0);
            case "salamandra" -> (s, cam, tt) -> VolcanMesh.salamander(s, cam, new V3(0, 0, 0), 1.0, tt, 0.6, 0.0, 0.0, 0.0, 0.3);
            case "salamandra_boca" -> (s, cam, tt) -> VolcanMesh.salamander(s, cam, new V3(0, 0, 0), 1.0, tt, 0.0, 0.0, 0.0, 1.0, 1.0);
            case "diablillo" -> (s, cam, tt) -> VolcanMesh.imp(s, cam, new V3(0, 0, 0), 1.0, tt, 0.0, 0.0, 0.0, 0.0);
            case "diablillo_bola" -> (s, cam, tt) -> VolcanMesh.imp(s, cam, new V3(0, 0, 0), 1.0, tt, 0.0, 0.0, 1.0, 0.0);
            case "diablillo_picada" -> (s, cam, tt) -> VolcanMesh.imp(s, cam, new V3(0, 0, 0), 1.0, tt, 0.0, 0.0, 0.0, 1.0);
            case "cultista" -> (s, cam, tt) -> VolcanMesh.cultist(s, cam, new V3(0, 0, 0), 1.0, tt, 0.6, 0.0, 0.0, 0.0);
            case "cultista_conjuro" -> (s, cam, tt) -> VolcanMesh.cultist(s, cam, new V3(0, 0, 0), 1.0, tt, 0.0, 0.0, 0.0, 1.0);
            default -> (s, cam, tt) -> {
                BossMesh.heart(s, cam, new V3(0, 6, 0), 2.4, tt, BossMesh.beat(tt, 30), 14, 0.0, 0.3,
                        new V3[]{new V3(9, -5, 9), new V3(-9, -5, 9)});
                BossMesh.chain(s, cam, new V3(0.9, 6.0, 0.9), new V3(9, 1.3, 9), tt, 1.0, 0.8);
                BossMesh.chain(s, cam, new V3(-0.9, 6.0, 0.9), new V3(-9, 1.3, 9), tt, 1.0, 0.8);
                BossMesh.anchor(s, cam, new V3(9, 0, 9), tt, 0.0, 1.0);
                BossMesh.anchor(s, cam, new V3(-9, 0, 9), tt, 0.0, 1.0);
                BossMesh.larva(s, cam, new V3(2, 0, 5), 1.0, tt, 0.6, 0.4, 0.0);
            };
        };
        double[][] vs = {{0, 8}, {35, 18}, {90, 5}, {200, 25}};
        if (a.length > 4) {
            String[] parts = a[4].split(",");
            for (int k = 0; k < Math.min(4, parts.length); k++) {
                String[] yp = parts[k].split(":");
                vs[k] = new double[]{Double.parseDouble(yp[0]), Double.parseDouble(yp[1])};
            }
        }
        BufferedImage sheet = new BufferedImage(512 * 4, 512, BufferedImage.TYPE_INT_ARGB);
        for (int k = 0; k < vs.length; k++) {
            List<PetBake.Q> probe = new ArrayList<>();
            Cam c0 = PetBake.camFrom(vs[k][0], vs[k][1], new V3(0, 0, 0));
            d.draw((tx, b, p, uv, col) -> probe.add(new PetBake.Q(tx, b, p.clone(), uv.clone(), col.clone())), c0, t);
            double[] bb = PetBake.bbox(probe);
            V3 center = new V3((bb[0] + bb[3]) / 2, (bb[1] + bb[4]) / 2, (bb[2] + bb[5]) / 2);
            Cam cam = PetBake.camFrom(vs[k][0], vs[k][1], center);
            List<PetBake.Q> qs = new ArrayList<>();
            d.draw((tx, b, p, uv, col) -> qs.add(new PetBake.Q(tx, b, p.clone(), uv.clone(), col.clone())), cam, t);
            PetBake.Canvas cv = new PetBake.Canvas(512, 512);
            Object[] ft = PetBake.fit(qs, cam, 512, 512, 0.8);
            PetBake.render(cv, qs, cam, (V3) ft[0], (double) ft[1]);
            sheet.getGraphics().drawImage(PetBake.toImage(cv, 0x241A1A), 512 * k, 0, null);
            if (k == 0) {
                System.out.println(what + ": " + qs.size() + " cuadros");
            }
        }
        ImageIO.write(sheet, "png", new File(a[1]));
    }
}
