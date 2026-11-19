package com.mundosdeaxelis.vfx;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Herramienta (no va en el mod): hoja de revisión de una mascota 3D: quieta de 3/4, de lado, de espaldas, caminando y en el
 * momento de alegría. Uso: java -cp <clases>:<esta carpeta> com.mundosdeaxelis.vfx.PetSheet <assets> <salida.png> <especie> <tamaño> [edad]
 */
public final class PetSheet {
    static List<PetBake.Q> pose(int species, double size, double age, Cam cam, double speed) {
        List<PetBake.Q> out = new ArrayList<>();
        VfxData d = new VfxData();
        d.b = species;
        d.size = (float) size;
        d.id = 3;
        d.rgb = 0xFFFFFF;
        d.yawInit = true;
        d.yaw = d.pyaw = 0;
        d.z = speed;
        d.pz = 0;
        d.dz = 1;
        Creature3D.emit(d, age, 0.0, new V3(0, 0, 0), cam, (t, b, p, uv, col) -> out.add(new PetBake.Q(t, b, p.clone(), uv.clone(), col.clone())));
        return out;
    }

    public static void main(String[] a) throws Exception {
        PetBake.assets = a[0];
        int species = Integer.parseInt(a[2]);
        double size = Double.parseDouble(a[3]);
        double age = a.length > 4 ? Double.parseDouble(a[4]) : 40;
        V3 center = new V3(0, 0.5 * size, 0);
        // {yaw, pitch, edad, velocidad}
        double[][] vs = {{30, 12, age, 0}, {90, 4, age, 0}, {200, 15, age, 0}, {30, 12, age + 7, 0.2}, {30, 12, age + 11, 0.2}, {0, 8, age, 0}};
        int W = Integer.getInteger("w", 360);
        BufferedImage sheet = new BufferedImage(W * vs.length, W, BufferedImage.TYPE_INT_ARGB);
        // el mismo encuadre para todas las vistas (el de la vista quieta de 3/4) para comparar tamaños
        Cam c0 = PetBake.camFrom(vs[0][0], vs[0][1], center);
        Object[] ft0 = PetBake.fit(pose(species, size, age, c0, 0), c0, W, W, 0.7);
        double ppu = (double) ft0[1];
        for (int k = 0; k < vs.length; k++) {
            Cam cam = PetBake.camFrom(vs[k][0], vs[k][1], center);
            List<PetBake.Q> q = pose(species, size, vs[k][2], cam, vs[k][3]);
            PetBake.Canvas cv = new PetBake.Canvas(W, W);
            PetBake.render(cv, q, cam, center, ppu);
            sheet.getGraphics().drawImage(PetBake.toImage(cv, 0x30303C), W * k, 0, null);
        }
        ImageIO.write(sheet, "png", new File(a[1]));
        System.out.println("ok " + a[1]);
    }
}
