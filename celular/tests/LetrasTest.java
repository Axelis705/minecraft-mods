import com.celular.music.Letra;
import com.celular.music.Letras;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Letras de las canciones: leer LRC (varias marcas, offset, palabras con tiempo), letra sin tiempos, buscar un .lrc junto a la
 * canción, la letra guardada dentro de un MP3 (USLT y SYLT) y la respuesta de LRCLIB (sin internet: se prueba el JSON). Las letras
 * de prueba son inventadas.
 */
public class LetrasTest {
    static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
        System.out.println("  ok: " + what);
    }

    static boolean near(double a, double b) {
        return Math.abs(a - b) < 0.011;
    }

    public static void main(String[] a) throws Exception {
        System.out.println("LetrasTest");
        String lrc = "[ti:Prueba]\n[ar:Celular]\n[offset:+200]\n"
                + "[00:01.50]Hola desde el celular\n"
                + "[00:04.00][00:20.00]Esta línea se repite\n"
                + "[00:08.25]<00:08.25>Cada <00:08.75>palabra <00:09.10>con <00:09.60>tiempo\n"
                + "\n[00:12.00]Última línea\n";
        Letra l = Letra.lrc(lrc);
        check(l != null && l.sincronizada, "lee el LRC");
        check(l.lineas.size() == 5, "5 líneas (una repetida dos veces): " + l.lineas.size());
        check(near(l.lineas.get(0).t, 1.3), "offset +200 ms adelanta la línea (" + l.lineas.get(0).t + ")");
        check(l.lineas.get(0).texto.equals("Hola desde el celular"), "texto de la línea");
        check(near(l.lineas.get(4).t, 19.8) && l.lineas.get(4).texto.equals("Esta línea se repite"), "la línea repetida va al final");
        Letra.Linea w = l.lineas.get(2);
        check(w.texto.equals("Cada palabra con tiempo") && w.tPalabras != null && near(w.tPalabras[1], 8.55), "palabras con tiempo");
        check(l.linea(0.5) == -1 && l.linea(1.3) == 0 && l.linea(5) == 1 && l.linea(100) == 4, "en qué línea vamos");
        double[] ts = l.tiempos(0, 6);
        check(ts.length == 4 && near(ts[0], 1.3) && ts[1] > ts[0] && ts[3] < l.fin(0, 6), "palabras repartidas en la línea");
        check(near(l.fin(3, 6), 17.8), "una línea dura como mucho 6 s (" + l.fin(3, 6) + ")");
        Letra back = Letra.lrc(l.aLrc());
        check(back != null && back.lineas.size() == 5 && near(back.lineas.get(1).t, 3.8), "se guarda y se vuelve a leer");

        Letra p = Letra.plana("Primera\n\nSegunda\nTercera\n", 100);
        check(p != null && !p.sincronizada && p.lineas.size() == 3 && near(p.lineas.get(0).t, 7) && p.lineas.get(2).t < 93, "letra sin tiempos repartida");
        check(Letra.de("[00:01.00]Con tiempo", 10).sincronizada && !Letra.de("Sin tiempo", 10).sincronizada, "elige LRC o texto");
        check(Letra.lrc("solo texto sin marcas") == null, "un texto sin marcas no es LRC");

        // .lrc junto a la canción y la caché
        Path dir = Files.createTempDirectory("letras");
        Path song = dir.resolve("Artista - Cancion.mp3");
        Files.write(song, new byte[64]);
        Files.writeString(dir.resolve("Artista - Cancion.lrc"), "﻿[00:02.00]Desde el archivo\n");
        Letras.internet = false;
        Letras.cache = dir.resolve("cache");
        Letra f = Letras.buscar(new Letras.Pedido("x1", song, "mp3", "Cancion", "Artista", 60));
        check(f != null && f.origen.equals("archivo") && f.lineas.get(0).texto.equals("Desde el archivo"), "lee el .lrc de al lado (con BOM)");
        Files.createDirectories(Letras.cache);
        Files.writeString(Letras.cache.resolve("x2.lrc"), "[00:03.00]De la caché\n");
        Letra c = Letras.buscar(new Letras.Pedido("x2", dir.resolve("otra.mp3"), "mp3", "Otra", "", 60));
        check(c != null && c.origen.equals("cache"), "lee la caché");
        check(Letras.buscar(new Letras.Pedido("x3", null, "", "Nada", "", 60)) == null, "sin internet y sin archivo: no hay letra");

        // letra dentro del MP3: USLT (texto con tiempos) y SYLT (tiempos en ms)
        Path m1 = dir.resolve("uslt.mp3");
        Files.write(m1, mp3(frame("USLT", uslt("[00:05.00]Letra guardada en el MP3"))));
        Letra u = Letras.buscar(new Letras.Pedido("u", m1, "mp3", "", "", 60));
        check(u != null && u.origen.equals("mp3") && near(u.lineas.get(0).t, 5) && u.lineas.get(0).texto.equals("Letra guardada en el MP3"), "USLT del MP3");
        Path m2 = dir.resolve("sylt.mp3");
        Files.write(m2, mp3(frame("SYLT", sylt(new String[]{"Primera ", "línea", "\nSegunda ", "línea"}, new int[]{1000, 1500, 4000, 4600}))));
        Letra s = Letras.buscar(new Letras.Pedido("s", m2, "mp3", "", "", 60));
        check(s != null && s.lineas.size() == 2 && near(s.lineas.get(1).t, 4) && s.lineas.get(1).texto.equals("Segunda línea"), "SYLT del MP3");

        // respuestas de LRCLIB
        String json = "[{\"id\":1,\"trackName\":\"Cancion\",\"duration\":215.0,\"instrumental\":false,\"plainLyrics\":\"A\\nB\",\"syncedLyrics\":null},"
                + "{\"id\":2,\"trackName\":\"Cancion\",\"duration\":181.0,\"instrumental\":false,\"plainLyrics\":\"x\",\"syncedLyrics\":\"[00:01.00]Con \\u00e1cento\\n[00:02.00]y \\\"comillas\\\"\"},"
                + "{\"id\":3,\"trackName\":\"Cancion\",\"duration\":180.0,\"instrumental\":true,\"plainLyrics\":null,\"syncedLyrics\":null}]";
        String[] best = mejorDe(json, 180);
        check(best != null && best[0] != null && best[0].contains("Con ácento") && best[0].contains("\"comillas\""), "elige la versión con tiempos y la duración que cuadra");
        check(mejorDe(json, 0) != null, "sin duración también elige");
        check(mejorDe("[]", 100) == null, "sin resultados");
        // internet (opcional: si LRCLIB no responde no es un fallo). Una canción de dominio público.
        if (a.length > 0 && a[0].equals("--internet")) {
            Letras.internet = true;
            Letras.cache = null;
            try {
                Letra net = Letras.buscar(new Letras.Pedido("net", null, "", "Amazing Grace", "", 0));
                System.out.println("  internet: " + (net == null ? "sin letra" : net.lineas.size() + " líneas, con tiempos: " + net.sincronizada));
            } catch (Exception e) {
                System.out.println("  internet: no respondió (" + e + ")");
            }
        }
        System.out.println("LetrasTest OK");
    }

    static String[] mejorDe(String json, int segundos) throws Exception {
        java.lang.reflect.Method parse = Class.forName("com.celular.music.Letras$Json").getDeclaredMethod("parse", String.class);
        parse.setAccessible(true);
        java.lang.reflect.Method mejor = Letras.class.getDeclaredMethod("mejor", Object.class, int.class);
        mejor.setAccessible(true);
        return (String[]) mejor.invoke(null, parse.invoke(null, json), segundos);
    }

    // ---- MP3 de mentira: una etiqueta ID3v2.3 con un marco y unos bytes después

    static byte[] frame(String id, byte[] body) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.writeBytes(id.getBytes(StandardCharsets.ISO_8859_1));
        int n = body.length;
        o.write(n >>> 24);
        o.write(n >>> 16);
        o.write(n >>> 8);
        o.write(n);
        o.write(0);
        o.write(0);
        o.writeBytes(body);
        return o.toByteArray();
    }

    static byte[] mp3(byte[] frames) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.writeBytes(new byte[]{'I', 'D', '3', 3, 0, 0});
        int n = frames.length;
        o.write((n >> 21) & 0x7F);
        o.write((n >> 14) & 0x7F);
        o.write((n >> 7) & 0x7F);
        o.write(n & 0x7F);
        o.writeBytes(frames);
        o.writeBytes(new byte[32]);
        return o.toByteArray();
    }

    static byte[] uslt(String text) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(3);                                   // UTF-8
        o.writeBytes("spa".getBytes(StandardCharsets.ISO_8859_1));
        o.write(0);                                   // descripción vacía
        o.writeBytes(text.getBytes(StandardCharsets.UTF_8));
        return o.toByteArray();
    }

    static byte[] sylt(String[] parts, int[] ms) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(3);
        o.writeBytes("spa".getBytes(StandardCharsets.ISO_8859_1));
        o.write(2);                                   // milisegundos
        o.write(1);                                   // letra
        o.write(0);                                   // descripción vacía
        for (int i = 0; i < parts.length; i++) {
            o.writeBytes(parts[i].getBytes(StandardCharsets.UTF_8));
            o.write(0);
            o.write(ms[i] >>> 24);
            o.write(ms[i] >>> 16);
            o.write(ms[i] >>> 8);
            o.write(ms[i]);
        }
        return o.toByteArray();
    }
}
