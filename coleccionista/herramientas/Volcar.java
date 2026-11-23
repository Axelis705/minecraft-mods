import com.coleccionista.mapa.AlaCeldas;
import com.coleccionista.mapa.Cloacas;
import com.coleccionista.mapa.Archivo;
import com.coleccionista.mapa.Capilla;
import com.coleccionista.mapa.Final;
import com.coleccionista.mapa.Laberintos;
import com.coleccionista.mapa.Puertas;
import com.coleccionista.mapa.Salon;
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
 *   java -cp out Volcar cloacas_corte salida.txt   (sin techo: para verlo desde arriba)
 * </pre>
 */
public final class Volcar {
    public static void main(String[] a) throws Exception {
        Plano p = switch (a[0]) {
            case "celdas" -> AlaCeldas.crear(1234L);
            case "cloacas", "cloacas_corte" -> Cloacas.crear(1234L);
            case "salon", "salon_corte" -> Salon.crear(1234L);
            case "puertas", "puertas_corte" -> Puertas.crear(1234L);
            case "final", "final_corte" -> Final.crear(1234L);
            case "capilla", "capilla_corte" -> Capilla.crear(1234L);
            case "archivo", "archivo_corte" -> Archivo.crear(1234L);
            case "laberintos", "laberintos_corte" -> Laberintos.crear(1234L);
            case "galeria", "galeria_corte" -> com.coleccionista.mapa.Galeria.crear(1234L);
            default -> throw new IllegalArgumentException("plano desconocido: " + a[0]);
        };
        int n = 0;
        try (PrintWriter w = new PrintWriter(a[1], StandardCharsets.UTF_8)) {
            for (Map.Entry<Long, String> e : p.m.entrySet()) {
                if (e.getValue().equals("air") || a[0].endsWith("_corte") && Plano.ky(e.getKey()) >= (a[0].startsWith("salon") ? 7 : a[0].startsWith("puertas") ? 13 : a[0].startsWith("final") ? 6 : a[0].startsWith("capilla") || a[0].startsWith("archivo") ? 6 : a[0].startsWith("laberintos") ? 8 : a[0].startsWith("galeria") ? 4 : 5)) {
                    continue;
                }
                w.println(Plano.kx(e.getKey()) + " " + Plano.ky(e.getKey()) + " " + Plano.kz(e.getKey()) + " " + e.getValue());
                n++;
            }
        }
        System.out.println("bloques=" + n + " marcas=" + p.marcas.size() + " caja=" + p.x0 + ".." + p.x1 + " " + p.y0 + ".." + p.y1 + " "
                + p.z0 + ".." + p.z1);
        var falta = com.coleccionista.mapa.Estilo.pendientes(p);
        System.out.println(falta.isEmpty() ? "todo con bloques nuevos" : "bloques de Minecraft que quedan: " + falta);
    }
}
