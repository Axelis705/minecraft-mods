package com.mundosdeaxelis.vfx;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.List;

/**
 * Herramienta (no va en el mod): cuadros de una mascota 3D en movimiento para revisar su animación: quieta (respira, parpadea,
 * da su saltito) y andando en círculo (la cámara gira despacio a su alrededor). Uso:
 * java -cp <clases>:<esta carpeta> com.mundosdeaxelis.vfx.PetClip <assets> <carpeta salida> <especie> <tamaño> <nombre>
 */
public final class PetClip {
    public static void main(String[] a) throws Exception {
        PetBake.assets = a[0];
        File out = new File(a[1]);
        out.mkdirs();
        int species = Integer.parseInt(a[2]);
        double size = Double.parseDouble(a[3]);
        String name = a[4];
        int W = 260;
        V3 center = new V3(0, 0.5 * size, 0);
        Cam c0 = PetBake.camFrom(35, 12, center);
        Object[] ft0 = PetBake.fit(PetSheet.pose(species, size, 40, c0, 0), c0, W, W, 0.62);
        double ppu = (double) ft0[1];
        int n = 0;
        // 40 cuadros quieta (2 ticks cada uno), 40 andando y 30 dormida (quieta mucho rato: se duerme)
        for (int k = 0; k < 110; k++) {
            boolean walk = k >= 40 && k < 80;
            boolean sleep = k >= 80;
            double age = 300 + k * 2.0;
            double yaw = sleep ? 20 + (k - 80) * 0.6 : 35 + k * 2.2;
            Cam cam = PetBake.camFrom(yaw, 12, center);
            List<PetBake.Q> q = PetSheet.pose(species, size, age, cam, walk ? 0.2 : 0, sleep ? 900 + 25 + (k - 80) * 2.0 : 0);
            PetBake.Canvas cv = new PetBake.Canvas(W, W);
            PetBake.render(cv, q, cam, center, ppu);
            ImageIO.write(PetBake.toImage(cv, 0xEEF0F6), "png", new File(out, String.format("%s_%03d.png", name, n++)));
        }
        System.out.println("ok " + name);
    }
}
