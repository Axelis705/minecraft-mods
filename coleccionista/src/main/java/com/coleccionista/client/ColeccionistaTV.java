package com.coleccionista.client;

import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.anim.client.GeoAssets;
import com.axeliscore.anim.client.GeoRenderer;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.client.Draw;
import com.coleccionista.Coleccionista;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;

/**
 * (Cliente) El Coleccionista en vivo en las pantallas: medio cuerpo en 3D sentado en su sillón (modelo de
 * {@code herramientas/personajes/coleccionista_tv.py}). Respira, ladea la cabeza de golpe como muñeco, junta las manos mientras espera,
 * hace un ademán distinto en cada frase, se ríe en las risas y a veces la señal "brinca". Va aplanado sobre el vidrio (como una imagen de
 * verdad) y con la luz de frente, así se ve igual en cualquier pared.
 */
public final class ColeccionistaTV {
    private static final ResourceLocation GEO = Ids.rl(Coleccionista.MODID, "geo/coleccionista_tv.geo.json");
    private static final ResourceLocation TEX = Ids.rl(Coleccionista.MODID, "textures/entity/coleccionista_tv.png");
    private static final int LUZ = 0xF000F0;
    /** Alto del modelo en unidades (de la base del sillón a arriba del sombrero, con margen para que ladee la cabeza). */
    private static final float ALTO = 80F;
    /** Grosor en bloques de cada unidad del modelo una vez aplanado (lo de adelante tapa a lo de atrás sin parpadear). */
    private static final float GROSOR = 0.001F;
    /** Hasta dónde llega lo más de adelante (en bloques, desde el vidrio): lo que va encima (rayas, subtítulos) va más allá. */
    public static final float FRENTE = 0.046F;

    // canales que se mueven suave: cuerpo (x), cabeza (x, y, z), brazo y antebrazo derechos (x, z), izquierdos (x, z)
    private static final int CX = 0, KX = 1, KY = 2, KZ = 3, BDX = 4, BDZ = 5, ADX = 6, ADZ = 7, BIX = 8, BIZ = 9, AIX = 10, AIZ = 11, N = 12;
    private static final float[] ACT = new float[N], OBJ = new float[N];
    private static Geo geo;
    private static Pose pose;
    private static GeoRenderer.Frame frame;
    private static int cuerpo, cabeza, bd, ad, bi, ai;
    private static long ultimo;
    private static boolean empezado;

    private ColeccionistaTV() {
    }

    /**
     * Dibuja al Coleccionista en una pantalla de {@code w} x {@code h} (la pila ya está en el centro del vidrio, mirando hacia +z) con el
     * fondo en {@code z}. {@code giroTv}: los grados que se giró la pila para la pantalla (para poner la luz de frente). Devuelve false si el modelo
     * no cargó (entonces se usa el retrato).
     */
    static boolean dibujar(PoseStack ps, MultiBufferSource buf, float w, float h, float z, boolean habla, String linea, long ms, float giroTv) {
        Geo g = GeoAssets.geo(GEO);
        if (g == null || g.bones.length == 0) {
            return false;
        }
        if (g != geo) {
            geo = g;
            pose = new Pose(g.bones.length);
            frame = new GeoRenderer.Frame(g);
            cuerpo = g.bone("cuerpo");
            cabeza = g.bone("cabeza");
            bd = g.bone("brazo_der");
            ad = g.bone("antebrazo_der");
            bi = g.bone("brazo_izq");
            ai = g.bone("antebrazo_izq");
            empezado = false;
        }
        // la pose se calcula una vez por cuadro (todas las pantallas lo muestran igual, como una transmisión)
        if (ms != ultimo) {
            animar(ms, habla, linea);
        }
        float sc = h * 0.97F / (ALTO / 16F);
        // a veces la señal brinca de lado un instante
        float dx = ms % 5300L < 90L ? ((ms / 30L) % 2L == 0L ? 1F : -1F) * 0.012F * w : 0F;
        Matrix3f luz = new Matrix3f(ps.last().normal());
        ps.pushPose();
        // la base del modelo (y = 24 del modelo, 1.5 bloques) al pie de la pantalla; su plano z = 0 queda un poco delante del vidrio
        ps.translate(dx, -h / 2F + h * 0.012F + 1.5F * sc, z + 0.004F + 18F * GROSOR);
        ps.scale(1F, 1F, GROSOR * 16F / sc);
        ps.mulPose(Axis.YP.rotationDegrees(180F));
        ps.scale(-sc, -sc, sc);
        // la luz: lo que mira de frente queda "hacia arriba" para Minecraft (luz completa); arriba/abajo un poco más oscuro y los lados
        // más (como un foco de frente), sin importar a dónde mire la pared. (cara de frente del modelo = -z; tras voltear queda +z)
        luz.rotateY((float) Math.toRadians(-giroTv)).rotateX((float) Math.toRadians(-90)).scale(1F, -1F, -1F);
        ps.last().normal().set(luz);
        GeoRenderer.compute(geo, pose, ps, frame);
        Draw d = new Draw(buf);
        GeoRenderer.draw(geo, pose, frame, d.cutout(TEX), GeoRenderer.Pass.NORMAL, LUZ, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        // los puntos rojos de los ojos: brillan más al hablar
        float brillo = habla ? 0.6F + 0.4F * Math.abs(Mth.sin(ms / 140F)) : 0.35F + 0.12F * Mth.sin(ms / 900F);
        int b = Mth.clamp((int) (brillo * 255), 0, 255);
        GeoRenderer.draw(geo, pose, frame, d.glow(TEX), GeoRenderer.Pass.GLOW, LUZ, OverlayTexture.NO_OVERLAY, 0xFF000000 | b << 16 | b << 8 | b);
        ps.popPose();
        return true;
    }

    /**
     * El Coleccionista en la mini tele del HUD (arriba a la derecha), en el rectángulo {@code x0, y0, w, h} de la interfaz. Misma
     * animación que las pantallas. La luz se pone de frente solo para este dibujo y luego se regresa la de la interfaz.
     */
    static boolean dibujarHud(com.axeliscore.plat.client.G g, int x0, int y0, int w, int h, boolean habla, String linea) {
        Geo gg = GeoAssets.geo(GEO);
        if (gg == null || gg.bones.length == 0) {
            return false;
        }
        if (gg != geo) {
            // la primera vez se arma como en las pantallas
            long ms = System.currentTimeMillis();
            geo = gg;
            pose = new Pose(gg.bones.length);
            frame = new GeoRenderer.Frame(gg);
            cuerpo = gg.bone("cuerpo");
            cabeza = gg.bone("cabeza");
            bd = gg.bone("brazo_der");
            ad = gg.bone("antebrazo_der");
            bi = gg.bone("brazo_izq");
            ai = gg.bone("antebrazo_izq");
            empezado = false;
            animar(ms, habla, linea);
        }
        long ms = System.currentTimeMillis();
        if (ms != ultimo) {
            animar(ms, habla, linea);
        }
        net.minecraft.client.gui.GuiGraphics gr = g.raw;
        gr.flush();
        gr.enableScissor(x0, y0, x0 + w, y0 + h);
        PoseStack ps = gr.pose();
        ps.pushPose();
        float sc = h * 0.97F / (ALTO / 16F);
        // la base del modelo (y 1.5 del modelo) al pie del cuadro; la cara del modelo (-z) mira hacia quien juega (+z en la interfaz)
        ps.translate(x0 + w / 2F, y0 + h - 1.5F * sc, 200);
        ps.scale(sc, sc, -sc);
        ps.last().normal().set(new Matrix3f().scaling(1F, 1F, -1F));
        GeoRenderer.compute(geo, pose, ps, frame);
        com.mojang.blaze3d.systems.RenderSystem.setShaderLights(new org.joml.Vector3f(0.35F, -0.55F, 1F).normalize(),
                new org.joml.Vector3f(-0.4F, -0.25F, 1F).normalize());
        Draw d = new Draw(gr.bufferSource());
        GeoRenderer.draw(geo, pose, frame, d.cutout(TEX), GeoRenderer.Pass.NORMAL, LUZ, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        float brillo = habla ? 0.6F + 0.4F * Math.abs(Mth.sin(ms / 140F)) : 0.35F;
        int b = Mth.clamp((int) (brillo * 255), 0, 255);
        GeoRenderer.draw(geo, pose, frame, d.glow(TEX), GeoRenderer.Pass.GLOW, LUZ, OverlayTexture.NO_OVERLAY, 0xFF000000 | b << 16 | b << 8 | b);
        gr.flush();
        com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        ps.popPose();
        gr.disableScissor();
        return true;
    }

    private static void animar(long ms, boolean habla, String linea) {
        float dt = ultimo == 0 ? 0F : Mth.clamp((ms - ultimo) / 1000F, 0F, 0.25F);
        ultimo = ms;
        float t = (ms % 3_600_000L) / 1000F;
        boolean risa = habla && linea.endsWith(".risa");
        // ---- a dónde va cada parte
        set(CX, -0.04F);
        if (risa) {
            OBJ[CX] = 0.06F;
            set3(KX, -0.38F, 0F, 0.1F);
            brazos(-0.45F, 0.25F, -0.3F, -1.75F, -0.45F, -0.25F, -0.3F, 1.75F);
        } else if (habla) {
            // un ademán por frase (siempre el mismo para la misma frase)
            int g = Math.floorMod(linea.hashCode(), 3);
            if (g == 0) {
                set3(KX, 0.02F, 0.22F, -0.08F);
                brazos(-0.75F, 0.35F, -0.2F, -2.75F, -0.55F, -0.12F, -0.35F, 2.0F);
            } else if (g == 1) {
                set3(KX, 0F, -0.1F, 0.05F);
                brazos(-0.3F, 0.5F, -0.6F, 1.0F, -0.3F, -0.5F, -0.6F, -1.0F);
            } else {
                set3(KX, 0.02F, -0.22F, 0.08F);
                brazos(-0.55F, 0.12F, -0.35F, -2.0F, -0.75F, -0.35F, -0.2F, 2.75F);
            }
        } else {
            // esperando: manos juntas y la cabeza ladeada; cada tanto la ladea al otro lado de golpe, como muñeco
            float lado = ((int) (t / 6.5F)) % 2 == 0 ? 0.2F : -0.12F;
            set3(KX, 0.08F, 0F, lado);
            brazos(-0.55F, 0.12F, -0.35F, -2.0F, -0.55F, -0.12F, -0.35F, 2.0F);
        }
        // ---- se acerca suave (la cabeza ladeada va rápido, como muñeco)
        if (!empezado) {
            System.arraycopy(OBJ, 0, ACT, 0, N);
            empezado = true;
        }
        float k = 1F - (float) Math.exp(-dt * 5F), kr = 1F - (float) Math.exp(-dt * 11F);
        for (int i = 0; i < N; i++) {
            ACT[i] += (OBJ[i] - ACT[i]) * (i == KZ ? kr : k);
        }
        // ---- encima, lo que no se suaviza: respirar, asentir al hablar, mover la mano, reír
        pose.reset();
        float resp = Mth.sin(t * 1.7F);
        pose.setRot(cuerpo, ACT[CX] + resp * 0.012F, 0F, 0F);
        pose.addPos(cuerpo, 0F, -resp * 0.35F, 0F);
        float kx = ACT[KX], ky = ACT[KY], kz = ACT[KZ];
        if (risa) {
            pose.addPos(cuerpo, 0F, Mth.sin(t * 40F) * 0.6F, 0F);
            kz += Mth.sin(t * 23F) * 0.04F;
        } else if (habla) {
            kx += Mth.sin(t * 7F) * 0.05F + Mth.sin(t * 11.3F) * 0.02F;
            ky += Mth.sin(t * 1.9F) * 0.05F;
        } else {
            kx += Mth.sin(t * 0.53F) * 0.04F;
            ky += Mth.sin(t * 0.37F) * 0.14F;
        }
        pose.setRot(cabeza, kx, ky, kz);
        float mano = habla && !risa ? Mth.sin(t * 3.1F) * 0.12F : 0F;
        // esperando: tamborilea los dedos (las manos se mueven apenitas)
        float dedos = !habla ? Mth.sin(t * 9F) * 0.025F * Math.max(0F, Mth.sin(t * 0.8F)) : 0F;
        pose.setRot(bd, ACT[BDX], 0F, ACT[BDZ]);
        pose.setRot(ad, ACT[ADX], 0F, ACT[ADZ] + mano + dedos);
        pose.setRot(bi, ACT[BIX], 0F, ACT[BIZ]);
        pose.setRot(ai, ACT[AIX], 0F, ACT[AIZ] - mano * 0.4F - dedos);
    }

    private static void set(int i, float v) {
        OBJ[i] = v;
    }

    private static void set3(int i, float x, float y, float z) {
        OBJ[i] = x;
        OBJ[i + 1] = y;
        OBJ[i + 2] = z;
    }

    private static void brazos(float bdx, float bdz, float adx, float adz, float bix, float biz, float aix, float aiz) {
        OBJ[BDX] = bdx;
        OBJ[BDZ] = bdz;
        OBJ[ADX] = adx;
        OBJ[ADZ] = adz;
        OBJ[BIX] = bix;
        OBJ[BIZ] = biz;
        OBJ[AIX] = aix;
        OBJ[AIZ] = aiz;
    }
}
