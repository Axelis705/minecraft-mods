import com.axeliscore.anim.Animator;
import com.axeliscore.anim.Clip;
import com.axeliscore.anim.ClipLoader;
import com.axeliscore.anim.Geo;
import com.axeliscore.anim.GeoLoader;
import com.axeliscore.anim.Pose;
import com.axeliscore.anim.client.GeoRenderer;
import com.axeliscore.plat.client.VB;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Vista previa de un modelo del Core fuera del juego, con el MISMO código que lo dibuja en Minecraft (Geo, Animator, GeoRenderer):
 * graba los vértices y los pinta con un z-buffer propio. Hace una hoja con varias poses/ángulos.
 *
 * java Visor modelo.geo.json animaciones.json textura.png salida.png "clip@tiempo@giro[@capas...]" ...
 *   clip vacío = sin animación; giro = grados alrededor del personaje (0 = de frente); se pueden añadir "+face_eyes" para mostrar huesos
 *   que el controlador oculta. Huesos que empiezan con "face_" se ocultan si no se piden.
 */
public class Visor {
    static final int W = Integer.getInteger("w", 300), H = Integer.getInteger("h", 380);
    static final float SCALE = Float.parseFloat(System.getProperty("escala", "92"));

    public static void main(String[] a) throws Exception {
        Geo geo = GeoLoader.parse(Files.readString(new File(a[0]).toPath()));
        Clip.Library lib = ClipLoader.library(ClipLoader.parse(Files.readString(new File(a[1]).toPath())));
        BufferedImage tex = ImageIO.read(new File(a[2]));
        List<BufferedImage> cells = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int k = 4; k < a.length; k++) {
            String[] p = a[k].split("@");
            String clip = p[0];
            float t = p.length > 1 ? Float.parseFloat(p[1]) : 0;
            float yaw = p.length > 2 ? Float.parseFloat(p[2]) : 0;
            float pitch = p.length > 3 ? Float.parseFloat(p[3]) : 12;
            List<String> show = new ArrayList<>();
            for (int j = 4; j < p.length; j++) {
                show.add(p[j]);
            }
            Pose pose = new Pose(geo.bones.length);
            Animator an = new Animator(geo);
            if (!clip.isEmpty()) {
                for (String c : clip.split("\\+")) {
                    Clip cl = lib.get(c);
                    if (cl == null) {
                        throw new IllegalArgumentException("no hay animación " + c);
                    }
                    an.play(c, cl, 0);
                }
                // avanzar en pasos pequeños (como el juego) hasta t
                float done = 0;
                while (done < t - 1e-6) {
                    float dt = Math.min(0.05F, t - done);
                    an.tick(dt);
                    done += dt;
                }
            }
            an.sample(pose);
            for (int b = 0; b < geo.bones.length; b++) {
                if (geo.bones[b].name.startsWith("face_") && !show.contains(geo.bones[b].name)) {
                    pose.hidden[b] = true;
                }
            }
            cells.add(render(geo, pose, tex, yaw, pitch));
            labels.add((clip.isEmpty() ? "reposo" : clip) + " " + t + "s " + (int) yaw + "°");
        }
        int cols = Math.min(cells.size(), 6);
        int rows = (cells.size() + cols - 1) / cols;
        BufferedImage out = new BufferedImage(cols * W, rows * (H + 18), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(new Color(58, 64, 76));
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        for (int i = 0; i < cells.size(); i++) {
            int x = (i % cols) * W, y = (i / cols) * (H + 18);
            g.drawImage(cells.get(i), x, y, null);
            g.setColor(Color.WHITE);
            g.drawString(labels.get(i), x + 6, y + H + 14);
        }
        ImageIO.write(out, "png", new File(a[3]));
    }

    static BufferedImage render(Geo geo, Pose pose, BufferedImage tex, float yaw, float pitch) {
        PoseStack ps = new PoseStack();
        // igual que el renderizador del Core con la entidad mirando al norte (cuerpo a 180°): queda de frente a la cámara
        ps.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180F - 180F)));
        ps.scale(-1F, -1F, 1F);
        ps.translate(0F, -1.501F, 0F);
        GeoRenderer.Frame f = new GeoRenderer.Frame(geo);
        GeoRenderer.compute(geo, pose, ps, f);
        VB vb = VB.recorder();
        GeoRenderer.draw(geo, pose, f, vb, GeoRenderer.Pass.ALL, 0xF000F0, 0, 0xFFFFFFFF);
        // cámara: gira alrededor del personaje (yaw) y lo mira un poco desde arriba (pitch); el frente del personaje es -z
        Matrix4f view = new Matrix4f().rotateX((float) Math.toRadians(pitch)).rotateY((float) Math.toRadians(yaw + 180));
        float scale = SCALE; // píxeles por bloque
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        float[] zb = new float[W * H];
        java.util.Arrays.fill(zb, Float.NEGATIVE_INFINITY);
        // fondo
        for (int y = 0; y < H; y++) {
            int c = new Color(70 + y / 12, 80 + y / 10, 96 + y / 9).getRGB();
            for (int x = 0; x < W; x++) {
                img.setRGB(x, y, c);
            }
        }
        // piso
        int floorY = (int) (H * 0.86);
        float[] v = new float[8];
        int[] e = new int[3];
        Vector3f light0 = new Vector3f(0.2F, 1F, -0.7F).normalize();
        Vector3f light1 = new Vector3f(-0.2F, 1F, 0.7F).normalize();
        int n = vb.size();
        for (int q = 0; q + 3 < n; q += 4) {
            float[][] sx = new float[4][];
            Vector3f nor = null;
            for (int k = 0; k < 4; k++) {
                vb.read(q + k, v, e);
                Vector4f p = new Vector4f(v[0], v[1], v[2], 1).mul(view);
                sx[k] = new float[] {W / 2F + p.x * scale, floorY - p.y * scale, p.z, v[3], v[4]};
                if (k == 0) {
                    nor = new Vector3f(v[5], v[6], v[7]);
                    view.transformDirection(nor);
                }
            }
            // luz como la de las entidades de Minecraft (dos luces + ambiente)
            Vector3f nw = new Vector3f(v[5], v[6], v[7]);
            float l = 0.4F + 0.6F * Math.min(1F, Math.max(0F, nw.dot(light0)) * 0.6F + Math.max(0F, nw.dot(light1)) * 0.6F);
            tri(img, zb, tex, sx[0], sx[1], sx[2], l);
            tri(img, zb, tex, sx[0], sx[2], sx[3], l);
        }
        return img;
    }

    static void tri(BufferedImage img, float[] zb, BufferedImage tex, float[] a, float[] b, float[] c, float light) {
        int minX = (int) Math.max(0, Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
        int maxX = (int) Math.min(W - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
        int minY = (int) Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
        int maxY = (int) Math.min(H - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
        float area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
        if (Math.abs(area) < 1e-6) {
            return;
        }
        int tw = tex.getWidth(), th = tex.getHeight();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5F, py = y + 0.5F;
                float w0 = ((b[0] - px) * (c[1] - py) - (b[1] - py) * (c[0] - px)) / area;
                float w1 = ((c[0] - px) * (a[1] - py) - (c[1] - py) * (a[0] - px)) / area;
                float w2 = 1 - w0 - w1;
                if (w0 < -1e-4 || w1 < -1e-4 || w2 < -1e-4) {
                    continue;
                }
                float z = a[2] * w0 + b[2] * w1 + c[2] * w2;
                int idx = y * W + x;
                // la cámara mira hacia -z: lo más cercano tiene z mayor
                if (z <= zb[idx]) {
                    continue;
                }
                float u = a[3] * w0 + b[3] * w1 + c[3] * w2, vv = a[4] * w0 + b[4] * w1 + c[4] * w2;
                int tx = Math.min(tw - 1, Math.max(0, (int) Math.floor(u * tw)));
                int ty = Math.min(th - 1, Math.max(0, (int) Math.floor(vv * th)));
                int col = tex.getRGB(tx, ty);
                if (((col >>> 24) & 255) < 26) {
                    continue; // recorte (como entityCutout)
                }
                zb[idx] = z;
                int r = (int) (((col >> 16) & 255) * light), g = (int) (((col >> 8) & 255) * light), bb = (int) ((col & 255) * light);
                img.setRGB(x, y, 0xFF000000 | r << 16 | g << 8 | bb);
            }
        }
    }
}
