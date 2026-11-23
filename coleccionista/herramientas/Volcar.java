import com.coleccionista.mapa.AlaCeldas;
import com.coleccionista.mapa.Plano;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Vuelca un plano del mapa del evento a texto ("x y z estado" por línea) para verlo fuera del juego con el visor de
 * estructuras/herramientas/visor (bake.py + shoot.py). Uso:
 * <pre>
 *   javac -d out coleccionista/src/main/java/com/coleccionista/mapa/*.java coleccionista/herramientas/Volcar.java
 *   java -cp out Volcar celdas salida.txt
 * </pre>
 */
public final class Volcar {
    public static void main(String[] a) throws Exception {
        Plano p = switch (a[0]) {
            case "celdas" -> AlaCeldas.crear(1234L);
            default -> throw new IllegalArgumentException("plano desconocido: " + a[0]);
        };
        int n = 0;
        try (PrintWriter w = new PrintWriter(a[1], StandardCharsets.UTF_8)) {
            for (Map.Entry<Long, String> e : p.m.entrySet()) {
                if (e.getValue().equals("air")) {
                    continue;
                }
                w.println(Plano.kx(e.getKey()) + " " + Plano.ky(e.getKey()) + " " + Plano.kz(e.getKey()) + " " + e.getValue());
                n++;
            }
        }
        System.out.println("bloques=" + n + " marcas=" + p.marcas.size() + " caja=" + p.x0 + ".." + p.x1 + " " + p.y0 + ".." + p.y1 + " "
                + p.z0 + ".." + p.z1);
    }
}
