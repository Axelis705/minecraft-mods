package com.coleccionista.mapa;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

/**
 * Revisa los mapas del evento sin Minecraft ({@link Revisor}) y saca sus vistas ({@link Vista}) en una carpeta. Uso:
 * <pre>
 *   java -cp ... com.coleccionista.mapa.RevisarMapas &lt;carpeta de salida&gt; [mansion|...] [--estricto]
 * </pre>
 * Con {@code --estricto} termina con error si hay cuartos inaccesibles o escondites a los que no se llega (para la prueba de tests/run.sh).
 */
public final class RevisarMapas {
    private RevisarMapas() {
    }

    public static void main(String[] a) throws Exception {
        File out = new File(a.length > 0 ? a[0] : "mapas_revision");
        out.mkdirs();
        String cual = a.length > 1 && !a[1].startsWith("--") ? a[1] : "mansion";
        boolean estricto = String.join(" ", a).contains("--estricto");
        Path base = Path.of(System.getProperty("coleccionista.raiz", "."));
        Map<String, Revisor.Forma> deco = Revisor.leerDeco(base.resolve("src/main/resources/data/coleccionista/deco.json"));
        Path tex = base.resolve("src/main/resources/assets/coleccionista/textures/block");
        int fallas = 0;
        if (cual.equals("mansion")) {
            Plano p = Mansion.crear(1234L);
            Plano.Marca e = p.marcas.get("man.entrada");
            Revisor r = new Revisor(p, deco).caminar(e.x(), e.y(), e.z());
            // la jaula de Cascabel está cerrada a propósito (la reja se abre en el juego)
            r.cerrado(42, 14, 52, 29).islas(8, 0, 30).marcas("man.esc.").marcas("man.entrada").pisos(p.x0, p.z0, p.x1, p.z1, 0, 30).puertas();
            System.out.print(r.informe("La mansión", 12));
            Vista v = new Vista(p, r, tex);
            v.planta(new File(out, "mansion_planta_baja.png"), 0, 5, "man.esc.", "Mansión · planta baja");
            v.planta(new File(out, "mansion_planta_alta.png"), 7, 12, "man.esc.", "Mansión · planta alta");
            v.planta(new File(out, "mansion_atico.png"), 13, 19, "man.esc.", "Mansión · ático");
            for (int esq = 0; esq < 4; esq++) {
                v.iso(new File(out, "mansion_iso_baja_" + esq + ".png"), 5, esq, null, 7, "Mansión · planta baja · esquina " + esq);
                v.iso(new File(out, "mansion_iso_alta_" + esq + ".png"), 12, esq, null, 7, "Mansión · planta alta · esquina " + esq);
            }
            // de cerca: el vestíbulo y cada fila de cuartos
            v.iso(new File(out, "mansion_cerca_vestibulo.png"), 12, 0, new int[] {14, 8, 42, 35}, 16, "Vestíbulo");
            v.iso(new File(out, "mansion_cerca_norte.png"), 5, 0, new int[] {5, -18, 53, 9}, 12, "Planta baja · norte");
            v.iso(new File(out, "mansion_cerca_sur.png"), 5, 0, new int[] {5, 34, 53, 61}, 12, "Planta baja · sur");
            v.iso(new File(out, "mansion_cerca_norte_alta.png"), 12, 0, new int[] {5, -18, 53, 9}, 12, "Planta alta · norte");
            v.iso(new File(out, "mansion_cerca_sur_alta.png"), 12, 0, new int[] {5, 34, 53, 61}, 12, "Planta alta · sur");
            v.iso(new File(out, "mansion_cerca_atico.png"), 17, 0, new int[] {5, -18, 53, -1}, 12, "Ático norte");
            fallas += r.cuantos("cuarto inaccesible") + r.cuantos("no se llega") + r.cuantos("no cabe nadie");
        }
        if (estricto && fallas > 0) {
            System.out.println("REVISION_MAPAS_FALLO " + fallas);
            System.exit(1);
        }
        System.out.println("REVISION_MAPAS_OK");
    }
}
