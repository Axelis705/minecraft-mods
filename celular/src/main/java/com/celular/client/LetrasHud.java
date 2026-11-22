package com.celular.client;

import com.celular.music.Letra;
import com.celular.music.Letras;
import com.celular.music.MusicPlayer;
import com.celular.music.Track;
import com.celular.plat.client.G;
import com.celular.radio.SongInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Las letras de las canciones flotando alrededor de quien pone la música (y un subtítulo tipo karaoke abajo).
 *
 * <ul>
 * <li>Tus canciones: en primera persona las palabras van apareciendo en un arco delante de ti, de izquierda a derecha, y se quedan
 * en su sitio del mundo aunque gires la cabeza; en tercera persona (o si suena por la bocina o con el celular tirado) giran alrededor.</li>
 * <li>Las de los demás (altavoz, bocina): solo si se oyen desde donde estás, alrededor de quien la pone.</li>
 * </ul>
 * Se dibujan en el HUD: cada punto del mundo se pasa a la pantalla con la posición y el giro de la cámara (así funciona igual en todas
 * las versiones). Cada palabra entra con un salto, sube despacio y se desvanece al terminar su línea.
 */
public final class LetrasHud {
    private LetrasHud() {
    }

    /** Para las fotos de la prueba: si es 0 o más, la canción va en ese segundo (y cuenta como sonando). */
    public static volatile double debugSeg = -1;
    /** Para la prueba: alguien "de mentira" cantando alrededor (como si fuera el altavoz de otro). */
    public static Entity debugOtro;
    public static Letra debugLetraOtro;
    public static double debugSegOtro;
    /** Lo que se dibujó en el último cuadro (para la prueba). */
    public static int palabras;
    public static String linea = "";

    private static final double MAX_LINEA = 7.0;

    static {
        Letras.cache = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("celular").resolve("letras");
    }

    /** Una fuente de letra: quién canta, en qué segundo va y su letra. */
    private static final class Fuente {
        Letra letra;
        double seg;
        Entity entity;
        Vec3 fijo;
        boolean mia;
        String quien = "";
        double oir = 99;
        int hue;
        String clave;
    }

    // ------------------------------------------------------------------ cámara

    private static Vec3 camPos(Minecraft mc) {
        //#if MC >= 26.1
        //$$ return mc.gameRenderer.mainCamera().position();
        //#else
        return mc.gameRenderer.getMainCamera().getPosition();
        //#endif
    }

    private static Quaternionf camRot(Minecraft mc) {
        //#if MC >= 26.1
        //$$ return mc.gameRenderer.mainCamera().rotation();
        //#else
        return mc.gameRenderer.getMainCamera().rotation();
        //#endif
    }

    /** Hacia dónde mira la cámara en su propio giro (en 1.21 Minecraft le dio media vuelta al giro de la cámara). */
    //#if MC >= 1.21
    //$$ private static final float FWD = -1F;
    //#else
    private static final float FWD = 1F;
    //#endif

    private static Vec3 cam;
    private static Vector3f fwd, up, left;
    private static float focal;
    private static int cx, cy;

    private static void vista(Minecraft mc, int sw, int sh) {
        cam = camPos(mc);
        Quaternionf q = camRot(mc);
        fwd = new Vector3f(0, 0, FWD).rotate(q);
        up = new Vector3f(0, 1, 0).rotate(q);
        left = new Vector3f(FWD, 0, 0).rotate(q);
        double fov = Math.toRadians(mc.options.fov().get());
        focal = (float) ((sh / 2.0) / Math.tan(fov / 2.0));
        cx = sw / 2;
        cy = sh / 2;
    }

    /** Punto del mundo → {x, y, profundidad} en la pantalla (en unidades del HUD); null si queda detrás de la cámara. */
    public static float[] proyectar(Vec3 p) {
        float dx = (float) (p.x - cam.x), dy = (float) (p.y - cam.y), dz = (float) (p.z - cam.z);
        float z = dx * fwd.x + dy * fwd.y + dz * fwd.z;
        if (z < 0.25F) {
            return null;
        }
        float x = dx * left.x + dy * left.y + dz * left.z;
        float y = dx * up.x + dy * up.y + dz * up.z;
        return new float[]{cx - x / z * focal, cy - y / z * focal, z};
    }

    /** Para la prueba: dónde cae en la pantalla un punto que está justo delante de la cámara. */
    public static float[] probarCentro(Minecraft mc, int sw, int sh) {
        vista(mc, sw, sh);
        Vec3 ahead = cam.add(fwd.x * 6.0, fwd.y * 6.0, fwd.z * 6.0);
        Vec3 look = mc.player.getViewVector(1.0F);
        Vec3 viewAhead = cam.add(look.scale(6.0));
        float[] a = proyectar(ahead), b = proyectar(viewAhead);
        return a == null || b == null ? null : new float[]{a[0], a[1], b[0], b[1]};
    }

    // ------------------------------------------------------------------ tiempo entre ticks (para que no tiemble al moverse)

    private static long tickAt;
    private static long lastGameTime = Long.MIN_VALUE;

    private static float pt(Minecraft mc) {
        long gt = mc.level.getGameTime();
        long now = System.nanoTime();
        if (gt != lastGameTime) {
            lastGameTime = gt;
            tickAt = now;
        }
        return mc.isPaused() ? 1F : Mth.clamp((now - tickAt) / 50_000_000F, 0F, 1F);
    }

    private static Vec3 pos(Entity e, float pt) {
        return new Vec3(Mth.lerp(pt, e.xo, e.getX()), Mth.lerp(pt, e.yo, e.getY()), Mth.lerp(pt, e.zo, e.getZ()));
    }

    // ------------------------------------------------------------------ quién canta

    private static Letras.Pedido pedido(Track t) {
        String clave = !t.hash.isEmpty() ? "h" + t.hash : "p" + t.path.toAbsolutePath() + ":" + t.modified;
        return new Letras.Pedido(clave, t.path, t.ext, t.title, t.artist, t.seconds);
    }

    private static Letras.Pedido pedido(SongInfo i) {
        return new Letras.Pedido("h" + i.hash, Radio.cacheFile(i), i.ext, i.title, i.artist, i.seconds);
    }

    /** La letra de lo que suena en mi celular (null si no hay o aún se busca). También la usa la app de Música. */
    public static Letra mia() {
        Track t = MusicPlayer.current;
        if (t == null) {
            return null;
        }
        Letras.internet = Prefs.lyricsNet;
        return Letras.get(pedido(t));
    }

    /** "lista", "buscando", "ninguna" para lo que suena en mi celular. */
    public static String estadoMia() {
        Track t = MusicPlayer.current;
        return t == null ? "" : Letras.estado(pedido(t).clave);
    }

    private static List<Fuente> fuentes(Minecraft mc) {
        List<Fuente> out = new ArrayList<>();
        Track t = MusicPlayer.current;
        if (Prefs.lyricsMine && t != null && (MusicPlayer.playing || debugSeg >= 0)) {
            Letra l = mia();
            if (l != null) {
                Fuente f = new Fuente();
                f.letra = l;
                f.seg = debugSeg >= 0 ? debugSeg : MusicPlayer.position();
                f.mia = true;
                f.hue = t.hue();
                f.clave = "mia";
                Vec3 bocina = Radio.mySpeakerPos(mc);
                if (bocina != null) {
                    f.fijo = bocina;
                } else {
                    Entity src = mc.level.getEntity(Music.speakerSource(mc));
                    f.entity = src != null ? src : mc.player;
                }
                out.add(f);
            }
        }
        if (Prefs.lyricsOthers) {
            Letras.internet = Prefs.lyricsNet;
            for (Radio.Oido o : Radio.oidos(mc)) {
                Letra l = Letras.get(pedido(o.song));
                if (l == null) {
                    continue;
                }
                Fuente f = new Fuente();
                f.letra = l;
                f.seg = o.seg;
                f.entity = o.entity;
                f.fijo = o.block;
                f.quien = o.name;
                f.oir = o.hear;
                f.hue = Math.floorMod(o.song.title.hashCode(), 360);
                f.clave = "otro:" + o.name;
                out.add(f);
            }
            if (debugOtro != null && debugLetraOtro != null) {
                Fuente f = new Fuente();
                f.letra = debugLetraOtro;
                f.seg = debugSegOtro;
                f.entity = debugOtro;
                f.quien = debugOtro.getName().getString();
                f.oir = 24;
                f.hue = 300;
                f.clave = "prueba";
                out.add(f);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ dibujar

    /** Una palabra lista para dibujar (se ordenan de lejos a cerca). */
    private static final class Pal {
        String txt;
        float x, y, z, scale, alpha;
        int color, sombra;
    }

    /** Hacia dónde mira el arco de cada fuente {grados, último cuadro en ns}: sigue a la cámara con suavidad. */
    private static final Map<String, double[]> CENTRO = new HashMap<>();

    /** Mueve el centro del arco hacia {@code objetivo} (grados) sin saltos: tarda ~0.4 s en alcanzarlo. */
    private static double centro(String clave, double objetivo) {
        long now = System.nanoTime();
        double[] c = CENTRO.get(clave);
        if (c == null) {
            CENTRO.put(clave, new double[]{objetivo, now});
            return objetivo;
        }
        double dt = Math.min(0.25, (now - c[1]) / 1e9);
        c[1] = now;
        double k = 1.0 - Math.exp(-dt / 0.4);
        c[0] += Mth.wrapDegrees(objetivo - c[0]) * k;
        return c[0];
    }

    public static void render(G g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        palabras = 0;
        linea = "";
        if (mc.player == null || mc.level == null) {
            return;
        }
        List<Fuente> fs = fuentes(mc);
        if (fs.isEmpty()) {
            CENTRO.clear();
            return;
        }
        vista(mc, sw, sh);
        float pt = pt(mc);
        List<Pal> todo = new ArrayList<>();
        Fuente principal = null;
        double mejor = Double.MAX_VALUE;
        for (Fuente f : fs) {
            Vec3 ancla = f.fijo != null ? f.fijo : f.entity != null ? pos(f.entity, pt) : null;
            if (ancla == null) {
                continue;
            }
            double dist = ancla.distanceTo(cam);
            if (!f.mia && dist > f.oir) {
                continue;
            }
            palabrasDe(mc, f, ancla, dist, pt, todo);
            double prio = f.mia ? -1 : dist;
            if (prio < mejor) {
                mejor = prio;
                principal = f;
            }
        }
        todo.sort((a, b) -> Float.compare(b.z, a.z));
        Font font = mc.font;
        for (Pal p : todo) {
            dibujar(g, font, p);
        }
        palabras = todo.size();
        if (principal != null && Prefs.lyricsSubs) {
            subtitulo(g, font, sw, sh, principal);
        }
    }

    private static void palabrasDe(Minecraft mc, Fuente f, Vec3 ancla, double dist, float pt, List<Pal> out) {
        Letra l = f.letra;
        double t = f.seg;
        int i = l.linea(t);
        if (i < 0) {
            return;
        }
        Font font = mc.font;
        boolean primera = f.mia && f.fijo == null && f.entity == mc.player && mc.options.getCameraType().isFirstPerson();
        float lejos = f.mia ? 1F : (float) Mth.clamp((f.oir - dist) / 6.0, 0.0, 1.0);
        // el arco mira hacia la cámara (en primera persona, hacia donde miras) y la sigue despacio
        double objetivo;
        if (primera) {
            objetivo = mc.player.getViewYRot(pt);
        } else {
            objetivo = Math.toDegrees(Math.atan2(-(cam.x - ancla.x), cam.z - ancla.z));
        }
        double centro = centro(f.clave + (primera ? ":1p" : ":3p"), objetivo);
        double r = primera ? 2.5 : f.fijo != null ? 1.7 : 1.9;
        float alto3d = primera ? 0.23F : 0.27F;               // alto de la letra en bloques
        double h0 = primera ? mc.player.getEyeHeight() + 0.3 : f.fijo != null ? 0.75 : (f.entity != null ? f.entity.getBbHeight() + 0.3 : 2.1);
        for (int k = Math.max(0, i - 1); k <= i; k++) {
            double fin = l.fin(k, MAX_LINEA);
            float salida = (float) Mth.clamp(1.0 - (t - fin) / 0.9, 0.0, 1.0);
            if (salida <= 0F) {
                continue;
            }
            Letra.Linea ln = l.lineas.get(k);
            double[] ts = l.tiempos(k, MAX_LINEA);
            int n = ln.palabras.length;
            // ancho de cada palabra en el arco (en grados), para que no se pisen
            double[] ancho = new double[n];
            double total = 0, hueco = Math.toDegrees(alto3d * 0.45 / r);
            for (int j = 0; j < n; j++) {
                ancho[j] = Math.toDegrees(font.width(ln.palabras[j]) / 9.0 * alto3d / r);
                total += ancho[j] + (j > 0 ? hueco : 0);
            }
            double acc = -total / 2;
            for (int j = 0; j < n; j++) {
                double mitad = acc + ancho[j] / 2;
                acc += ancho[j] + hueco;
                double edad = t - ts[j];
                if (edad < -0.02) {
                    continue;
                }
                // sube despacio mientras dura su línea y se va hacia arriba rápido al terminar
                double sube = Math.min(0.35, Math.max(0, edad) * 0.07) + Math.max(0, t - fin) * 0.9;
                double ola = 0.07 * Math.sin(j * 1.9 + k);
                // en primera persona el ángulo crece hacia la derecha; visto desde fuera, hacia la izquierda
                double yaw = primera ? centro + mitad : centro - mitad;
                double rad = Math.toRadians(yaw);
                Vec3 p = ancla.add(-Math.sin(rad) * r, h0 + ola + sube, Math.cos(rad) * r);
                float[] s = proyectar(p);
                if (s == null) {
                    continue;
                }
                boolean cantando = edad >= 0 && (j + 1 >= n || t < ts[j + 1]) && t < fin;
                float pop = (float) Mth.clamp(edad / 0.2, 0, 1);
                float salto = pop < 1 ? 0.45F + 0.55F * backOut(pop) : 1F;
                float altoPx = alto3d * focal / s[2];
                Pal w = new Pal();
                w.txt = ln.palabras[j];
                w.x = s[0];
                w.y = s[1];
                w.z = s[2];
                w.scale = Mth.clamp(altoPx / 9F, 0.3F, 3.2F) * salto * (cantando ? 1.1F : 1F);
                w.alpha = Mth.clamp(pop * 1.6F, 0F, 1F) * salida * lejos;
                int hue = f.hue + j * 17 + k * 40;
                w.color = cantando ? 0xFFFFFFFF : D.hsv(hue, 0.5F, 1F);
                w.sombra = D.hsv(hue, 0.85F, 0.28F);
                if (w.alpha > 0.03F) {
                    out.add(w);
                }
            }
        }
    }

    private static float backOut(float x) {
        float c1 = 1.70158F, c3 = c1 + 1F;
        float u = x - 1F;
        return 1F + c3 * u * u * u + c1 * u * u;
    }

    private static int conAlfa(int c, float a) {
        int al = Math.round(((c >>> 24) & 255) * a);
        return Math.max(5, al) << 24 | (c & 0xFFFFFF);
    }

    private static void dibujar(G g, Font font, Pal p) {
        int w = font.width(p.txt);
        g.push();
        g.translate(p.x, p.y, 0);
        g.scale(p.scale, p.scale, 1F);
        int x = -w / 2, y = -4;
        int sombra = conAlfa(p.sombra, p.alpha * 0.85F);
        g.drawString(font, p.txt, x + 1, y, sombra, false);
        g.drawString(font, p.txt, x - 1, y, sombra, false);
        g.drawString(font, p.txt, x, y + 1, sombra, false);
        g.drawString(font, p.txt, x, y - 1, sombra, false);
        g.drawString(font, p.txt, x, y, conAlfa(p.color, p.alpha), false);
        g.pop();
    }

    // ------------------------------------------------------------------ subtítulo (karaoke)

    private static void subtitulo(G g, Font font, int sw, int sh, Fuente f) {
        Letra l = f.letra;
        double t = f.seg;
        int i = l.linea(t);
        if (i < 0 || t > l.fin(i, MAX_LINEA) + 0.6) {
            return;
        }
        Letra.Linea ln = l.lineas.get(i);
        linea = ln.texto;
        double[] ts = l.tiempos(i, MAX_LINEA);
        int y = sh - 78;
        String pre = f.mia ? "♪ " : "♪ " + f.quien + ": ";
        int total = font.width(pre) + font.width(ln.texto);
        float sc = Math.min(1F, (sw - 20F) / Math.max(1, total));
        g.push();
        g.translate(sw / 2.0, y, 0);
        g.scale(sc, sc, 1F);
        int x = -total / 2;
        g.fill(x - 4, -2, x + total + 4, 10, 0x70000000);
        x = g.drawString(font, pre, x, 0, D.hsv(f.hue, 0.5F, 1F), true);
        for (int j = 0; j < ln.palabras.length; j++) {
            boolean cantada = t >= ts[j];
            String s = ln.palabras[j] + (j + 1 < ln.palabras.length ? " " : "");
            x = g.drawString(font, s, x, 0, cantada ? 0xFFFFFFFF : 0xFF9A9AA2, true);
        }
        g.pop();
        // la siguiente, más chica y apagada
        if (i + 1 < l.lineas.size()) {
            String next = l.lineas.get(i + 1).texto;
            float s2 = Math.min(0.75F, (sw - 20F) / Math.max(1, font.width(next)));
            g.push();
            g.translate(sw / 2.0, y + 12, 0);
            g.scale(s2, s2, 1F);
            g.drawString(font, next, -font.width(next) / 2, 0, 0xB0C8C8D0, true);
            g.pop();
        }
    }
}
