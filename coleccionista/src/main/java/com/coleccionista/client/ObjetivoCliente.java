package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.evento.Objetivo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * (Cliente) El indicador de "ir acá": un rombo dorado sobre el lugar a donde hay que ir, con su nombre y la distancia. Si el lugar queda
 * atrás o fuera de la pantalla, el rombo se pega a la orilla con una flecha que apunta hacia allá. Al llegar (a menos de 2.5 bloques)
 * se desvanece. No se ve con la venda puesta, en las cinemáticas ni con el HUD escondido.
 */
public final class ObjetivoCliente {
    private static final int ORO = 0xFFE8B44A;
    private static CompoundTag meta = new CompoundTag();
    /** Dónde cae en la pantalla (de -1 a 1) y si quedó detrás de la cámara (se calcula al dibujar el mundo). */
    private static float nx, ny;
    private static boolean atras, visto;
    private static float alfa;
    private static long cambio;

    private ObjetivoCliente() {
    }

    public static void init() {
        ClientData.listen(Objetivo.KEY, t -> {
            if (!t.getString("texto").equals(meta.getString("texto"))) {
                cambio = System.currentTimeMillis();
            }
            meta = t;
        });
        MinecraftForge.EVENT_BUS.addListener(ObjetivoCliente::proyectar);
        com.axeliscore.fx.client.Hud.add(ObjetivoCliente::dibujar);
    }

    public static boolean hay() {
        return meta.contains("x");
    }

    private static Vec3 punto() {
        return new Vec3(meta.getDouble("x"), meta.getDouble("y"), meta.getDouble("z"));
    }

    /** Proyecta el punto a la pantalla con las mismas matrices con las que se dibuja el mundo. */
    private static void proyectar(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        visto = false;
        if (!hay()) {
            return;
        }
        Vec3 cam = e.getCamera().getPosition();
        Vec3 r = punto().subtract(cam);
        Matrix4f vista = e.getPoseStack().last().pose();
        Vector4f v = new Vector4f((float) r.x, (float) r.y, (float) r.z, 1F);
        vista.transform(v);
        e.getProjectionMatrix().transform(v);
        atras = v.w() <= 0.05F;
        float w = Math.max(0.05F, Math.abs(v.w()));
        nx = v.x() / w;
        ny = v.y() / w;
        if (atras) {
            nx = -nx;
            ny = -ny;
        }
        visto = true;
    }

    private static void dibujar(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        boolean mostrar = hay() && visto && mc.player != null && EventoHud.visible() && !CiegasCliente.vendado();
        double d = mostrar ? mc.player.getEyePosition().distanceTo(punto()) : 0;
        if (d < 2.5) {
            mostrar = false;
        }
        alfa = Math.max(0F, Math.min(1F, alfa + (mostrar ? 0.08F : -0.08F)));
        if (alfa <= 0.01F || !hay()) {
            return;
        }
        float sx = (nx * 0.5F + 0.5F) * w, sy = (1F - (ny * 0.5F + 0.5F)) * h;
        int m = 22;
        boolean fuera = atras || sx < m || sx > w - m || sy < m + 30 || sy > h - m - 30;
        float ang = 0;
        if (fuera) {
            // a la orilla, en la dirección del punto
            float cx = w / 2F, cy = h / 2F, dx = sx - cx, dy = sy - cy;
            if (Math.abs(dx) < 0.001F && Math.abs(dy) < 0.001F) {
                dy = 1;
            }
            float kx = (w / 2F - m) / Math.max(0.001F, Math.abs(dx)), ky = (h / 2F - m - 30) / Math.max(0.001F, Math.abs(dy));
            float k = Math.min(kx, ky);
            sx = cx + dx * k;
            sy = cy + dy * k;
            ang = (float) Math.atan2(dy, dx);
        }
        // late un poco cuando cambia de objetivo
        float pulso = 1F + 0.35F * Math.max(0F, 1F - (System.currentTimeMillis() - cambio) / 600F);
        Font f = Ui.font();
        int x = Math.round(sx), y = Math.round(sy);
        int col = Ui.alpha(ORO, alfa), sombra = Ui.alpha(0xFF140C0E, alfa * 0.8F);
        g.push();
        g.translate(x, y, 0);
        g.scale(pulso, pulso, 1);
        rombo(g, 6, sombra);
        rombo(g, 5, col);
        rombo(g, 2, Ui.alpha(0xFFFFF4D6, alfa));
        g.pop();
        if (fuera) {
            // la flecha que apunta hacia el lugar
            g.push();
            g.translate(x, y, 0);
            g.rotate(ang);
            for (int i = 0; i < 5; i++) {
                g.fill(9 + i, -4 + i, 10 + i, 5 - i, col);
            }
            g.pop();
        }
        String nombre = Component.translatable(meta.getString("texto")).getString();
        String dist = Math.round(d) + " m";
        int ty = y + 10;
        if (ty > h - 26) {
            ty = y - 30;
        }
        int nw = f.width(nombre);
        g.fill(x - nw / 2 - 3, ty - 1, x + nw / 2 + 3, ty + 19, Ui.alpha(0xFF140C0E, alfa * 0.55F));
        g.drawCenteredString(f, nombre, x, ty, col);
        g.drawCenteredString(f, dist, x, ty + 10, Ui.alpha(0xFFD8D0C8, alfa));
    }

    private static void rombo(G g, int r, int col) {
        for (int i = -r; i <= r; i++) {
            int a = r - Math.abs(i);
            g.fill(-a, i, a + 1, i + 1, col);
        }
    }
}
