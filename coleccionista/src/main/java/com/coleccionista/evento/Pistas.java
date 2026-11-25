package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Los tres números del código de cada celda, escondidos en la celda misma: escritos o rayados sobre las cosas (el costado del colchón,
 * el piso debajo del catre, el espejo, el tanque del retrete, el techo...). Ninguno se regala: cada número sale en un lugar distinto y
 * dice en qué posición va ("7 _ _", "_ 3 _", "_ _ 9").
 * <p>
 * No aparecen hasta que se acaba la espera del despertar (en el apagón). El servidor decide dónde va cada uno y se lo manda a todos;
 * cada cliente los dibuja encima de las superficies ({@code InscripcionesCliente}).
 */
public final class Pistas {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":inscripciones";
    /** Estilos de letra (los dibuja el cliente). */
    public static final int RAYADO = 0, TIZA = 1, ROJO = 2, NEGRO = 3;
    /** Planos: pared (mira hacia {@code ang}), piso / cara de arriba, techo / cara de abajo. */
    public static final int PARED = 0, PISO = 1, TECHO = 2;
    /** Escondites, en tres grupos (uno de cada grupo por celda: así quedan repartidos por la celda). */
    static final String[][] GRUPOS = {
            {"colchon_lado", "bajo_catre", "colchon", "techo"},
            {"espejo", "bajo_lavabo", "tanque"},
            {"oeste_bajo", "fondo_bajo", "dintel", "tapa_cajon"}};

    private Pistas() {
    }

    public static void init() {
        AxelisCore.onLogin(p -> PlayerStore.Sync.toPlayer(p, KEY, estado(Plat.server(p))));
    }

    private static CompoundTag datos(MinecraftServer s) {
        CompoundTag g = WorldStore.get(s, MOD).global();
        if (!g.contains("pistas")) {
            g.put("pistas", new CompoundTag());
        }
        return g.getCompound("pistas");
    }

    public static boolean reveladas(MinecraftServer s) {
        return datos(s).getBoolean("reveladas");
    }

    static CompoundTag estado(MinecraftServer s) {
        CompoundTag t = new CompoundTag();
        if (s == null) {
            return t;
        }
        CompoundTag d = datos(s);
        t.putBoolean("reveladas", d.getBoolean("reveladas"));
        t.put("l", d.getList("l", Tag.TAG_COMPOUND).copy());
        return t;
    }

    private static void sync(MinecraftServer s) {
        WorldStore.get(s, MOD).dirty();
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }

    /** Las borra de todas las celdas (sala de espera / reinicio). */
    public static void ocultar(MinecraftServer s) {
        CompoundTag d = datos(s);
        d.putBoolean("reveladas", false);
        d.put("l", new ListTag());
        sync(s);
    }

    /** Escribe los tres números de cada celda en sus escondites (con el código que ya tiene cada celda). */
    public static void revelar(MinecraftServer s) {
        Random r = new Random(s.overworld().getGameTime() * 31L ^ 0xC0DEL);
        ListTag l = new ListTag();
        StringBuilder log = new StringBuilder();
        for (int n = 1; n <= com.coleccionista.mapa.AlaCeldas.CELDAS; n++) {
            int codigo = Celdas.codigo(s, n);
            Celda c = Celda.de(s, n);
            if (c == null || codigo <= 0) {
                continue;
            }
            int[] dig = {codigo / 100, codigo / 10 % 10, codigo % 10};
            List<Integer> orden = new ArrayList<>(List.of(0, 1, 2));
            Collections.shuffle(orden, r);
            for (int g = 0; g < 3; g++) {
                String lugar = GRUPOS[g][r.nextInt(GRUPOS[g].length)];
                int k = orden.get(g);
                CompoundTag t = c.inscripcion(lugar, texto(k, dig[k]), r);
                t.putInt("celda", n);
                t.putString("lugar", lugar);
                l.add(t);
                if (n == 1) {
                    log.append(lugar).append('=').append(t.getString("t")).append(' ');
                }
            }
        }
        CompoundTag d = datos(s);
        d.putBoolean("reveladas", true);
        d.put("l", l);
        sync(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_PISTAS reveladas inscripciones={} celda1: {}", l.size(), log.toString().trim());
    }

    /** "7 _ _", "_ 3 _" o "_ _ 9". */
    static String texto(int pos, int d) {
        String[] p = {"_", "_", "_"};
        p[pos] = String.valueOf(d);
        return String.join(" ", p);
    }

    /**
     * Dónde está todo en una celda (sale de la marca de su esquina: el punto de atrás a la izquierda del interior y el giro de la celda). Las
     * inscripciones se describen como en una celda que mira al sur ({@code u} hacia la derecha, {@code w} desde la pared del fondo hacia los
     * barrotes) y se giran al mundo.
     */
    record Celda(double cx, double y1, double cz, float giro) {
        static Celda de(MinecraftServer s, int n) {
            BlockPos e = Complejo.pos(s, "celda." + n + ".esquina");
            if (e == null) {
                return null;
            }
            return new Celda(e.getX(), e.getY(), e.getZ(), Complejo.yaw("celda." + n + ".esquina"));
        }

        CompoundTag inscripcion(String lugar, String texto, Random r) {
            final double e = 0.006; // despegado de la superficie
            double u, y, w;
            int plano = PARED, estilo;
            float ang, esc;
            y = y1;
            switch (lugar) {
                case "colchon_lado" -> { // el costado del colchón, del lado de la celda
                    u = 14.5 / 16 + e; y = y1 + 7.75 / 16; w = 1.0; ang = 90; esc = 0.0085F; estilo = NEGRO;
                }
                case "bajo_catre" -> { // en el piso, debajo del catre (hay que agacharse)
                    u = 0.5; y = y1 + e; w = 1.1; plano = PISO; ang = 0; esc = 0.011F; estilo = TIZA;
                }
                case "colchon" -> { // encima del colchón, a los pies
                    u = 0.5; y = y1 + 9.0 / 16 + e; w = 1.6; plano = PISO; ang = 0; esc = 0.009F; estilo = NEGRO;
                }
                case "techo" -> { // en el techo, encima del catre
                    u = 0.5; y = y1 + 4 - e; w = 1.6; plano = TECHO; ang = 0; esc = 0.013F; estilo = RAYADO;
                }
                case "espejo" -> { // en el vidrio del espejo, con plumón rojo
                    u = 4 + 14.8 / 16 - e; y = y1 + 1 + 11.0 / 16; w = 3.5; ang = 270; esc = 0.0095F; estilo = ROJO;
                }
                case "bajo_lavabo" -> { // en la pared, debajo del lavabo (a un lado del tubo)
                    u = 5 - e; y = y1 + 0.32; w = 3.77; ang = 270; esc = 0.0085F; estilo = RAYADO;
                }
                case "tanque" -> { // el frente del tanque del retrete
                    u = 4 + 9.0 / 16 - e; y = y1 + 10.0 / 16; w = 2.5; ang = 270; esc = 0.0085F; estilo = RAYADO;
                }
                case "oeste_bajo" -> { // pared de la izquierda, abajo, junto a los barrotes
                    u = e; y = y1 + 0.3; w = 4.4; ang = 90; esc = 0.011F; estilo = TIZA;
                }
                case "fondo_bajo" -> { // pared del fondo, abajo, entre el catre y el baúl
                    u = 2.6; y = y1 + 0.28; w = e; ang = 0; esc = 0.011F; estilo = TIZA;
                }
                case "dintel" -> { // la viga de arriba de los barrotes, por dentro
                    u = 0.9; y = y1 + 3.5; w = 5 - e; ang = 180; esc = 0.012F; estilo = RAYADO;
                }
                default -> { // "tapa_cajon": rayado en la tapa del baúl
                    u = 4.5; y = y1 + 9.0 / 16 + e; w = 0.5; plano = PISO; ang = 0; esc = 0.011F; estilo = RAYADO;
                }
            }
            // de la celda "que mira al sur" al mundo: la posición se gira con la celda y el ángulo (0 = cara hacia +z, 90 = hacia +x) también
            double rad = Math.toRadians(giro);
            int c = (int) Math.round(Math.cos(rad)), sn = (int) Math.round(Math.sin(rad));
            double x = cx + u * c - w * sn, z = cz + u * sn + w * c;
            ang = ((ang - giro) % 360 + 360) % 360;
            CompoundTag t = new CompoundTag();
            t.putDouble("x", x);
            t.putDouble("y", y);
            t.putDouble("z", z);
            t.putInt("p", plano);
            t.putFloat("a", ang);
            t.putFloat("s", esc);
            t.putInt("e", estilo);
            // un poco chueco, como escrito a mano
            t.putFloat("g", (r.nextFloat() - 0.5F) * 14F);
            t.putString("t", texto);
            return t;
        }
    }
}
