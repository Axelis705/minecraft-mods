package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.client.CoreScreen;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Te;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

/**
 * (Cliente) Perdiste: elige uno de tus cálices. La cámara se acerca a la mesa (encima de tu cabeza, mirando tus cálices) y la mano es el
 * mouse, como en el candado de la celda: el cáliz al que apunta se levanta un poquito; clic (o las teclas 1 a 5) y lo agarras.
 */
public class CalizScreen extends CoreScreen {
    private static final String MOD = Coleccionista.MODID;
    private static CalizScreen abierta;
    private static final Matrix4f INV = new Matrix4f();
    private static Vec3 camPos;
    private static boolean registrado;

    private int lado = -1;
    /** Hacia el frente y hacia la derecha de quien está sentado, y la altura de la orilla de los cálices. */
    private Vec3 frente, derecha;
    private double alto;
    private Vec3 mano, manoVis;
    private long abiertaEn, tomoEn;
    private int tomo = -1;

    public CalizScreen() {
        super(Component.translatable(MOD + ".te.caliz.titulo"));
    }

    private static void registrar() {
        if (!registrado) {
            registrado = true;
            MinecraftForge.EVENT_BUS.addListener(CalizScreen::dibujarMundo);
        }
    }

    public static CalizScreen actual() {
        return abierta;
    }

    @Override
    protected void init() {
        super.init();
        if (lado >= 0) {
            return;
        }
        registrar();
        lado = TeCliente.miLado();
        double[] s = TeCliente.silla(lado);
        if (s == null) {
            Minecraft.getInstance().tell(this::onClose);
            return;
        }
        double r = Math.toRadians(s[3]);
        frente = new Vec3(-Math.sin(r), 0, Math.cos(r));
        derecha = new Vec3(-frente.z, 0, frente.x);
        Vec3 c = TeCliente.caliz(lado, (Te.CALICES - 1) / 2);
        alto = c.y + 0.42;
        abierta = this;
        abiertaEn = System.currentTimeMillis();
        // la cámara: arriba de la cabeza, un poco al frente, mirando los cálices
        Vec3 ojo = new Vec3(s[0], s[1] + 1.95, s[2]).add(frente.scale(0.05));
        Cine.acercarCandado(ojo, c.add(0, 0.15, 0));
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GLASS_HIT, 1.2F, 0.6F));
        GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
    }

    @Override
    public void removed() {
        super.removed();
        if (abierta == this) {
            abierta = null;
        }
        TeCliente.senalado = -1;
        Cine.soltarCandado();
        Minecraft mc = Minecraft.getInstance();
        GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (tomo >= 0 ? System.currentTimeMillis() - tomoEn > 450 : !TeCliente.meToca("caliz")) {
            onClose();
        }
    }

    // ------------------------------------------------------------------ la mano

    /** El mouse sobre el plano de la orilla de los cálices; null si no se puede saber. */
    private Vec3 apuntar(double mx, double my) {
        if (camPos == null || width <= 0 || height <= 0) {
            return null;
        }
        float x = (float) (mx / width * 2 - 1), y = (float) (1 - my / height * 2);
        Vector4f a = new Vector4f(x, y, -1, 1).mul(INV);
        Vector4f b = new Vector4f(x, y, 0, 1).mul(INV);
        if (a.w == 0 || b.w == 0) {
            return null;
        }
        a.div(a.w);
        b.div(b.w);
        Vec3 o = camPos.add(a.x, a.y, a.z);
        Vec3 d = new Vec3(b.x - a.x, b.y - a.y, b.z - a.z);
        if (Math.abs(d.y) < 1e-6) {
            return null;
        }
        double t = (alto - o.y) / d.y;
        return t < 0 ? null : o.add(d.scale(t));
    }

    /** El cáliz (que todavía está) más cerca de la mano, si está cerca; si no, -1. */
    private int cercano(Vec3 p) {
        if (p == null) {
            return -1;
        }
        int mejor = -1;
        double d = 0.28 * 0.28;
        int copas = TeCliente.copas(lado);
        for (int k = 0; k < Te.CALICES; k++) {
            Vec3 c = TeCliente.caliz(lado, k);
            if (c == null || (copas & (1 << k)) == 0) {
                continue;
            }
            double dx = c.x - p.x, dz = c.z - p.z;
            if (dx * dx + dz * dz < d) {
                d = dx * dx + dz * dz;
                mejor = k;
            }
        }
        return mejor;
    }

    @Override
    public void mouseMoved(double x, double y) {
        Vec3 p = apuntar(x, y);
        if (p != null) {
            mano = p;
            if (tomo < 0) {
                TeCliente.senalado = cercano(p);
            }
        }
    }

    @Override
    protected boolean onClick(double x, double y, int b) {
        mouseMoved(x, y);
        if (b == 0 && TeCliente.senalado >= 0) {
            tomar(TeCliente.senalado);
        }
        return true;
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_5) {
            int k = key - GLFW.GLFW_KEY_1;
            if ((TeCliente.copas(lado) & (1 << k)) != 0) {
                tomar(k);
            }
            return true;
        }
        return superKey(key, scan, mods);
    }

    private void tomar(int k) {
        if (tomo >= 0) {
            return;
        }
        tomo = k;
        tomoEn = System.currentTimeMillis();
        TeCliente.senalado = k;
        Vec3 c = TeCliente.caliz(lado, k);
        if (c != null) {
            mano = c.add(0, 0.42, 0);
        }
        CompoundTag t = new CompoundTag();
        t.putString("que", "caliz");
        t.putInt("k", k);
        ClientData.send(Te.ACCION, t);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GLASS_HIT, 1.0F, 1.2F));
    }

    /** Para la prueba de cliente. */
    public void tomarPrueba(int k) {
        tomar(k);
    }

    public void senalarPrueba(int k) {
        Vec3 c = TeCliente.caliz(lado, k);
        if (c != null) {
            mano = c.add(0, 0.42, 0);
            TeCliente.senalado = k;
        }
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        Font font = Ui.font();
        int seg = TeCliente.mio().getInt("segundos");
        long pasados = (System.currentTimeMillis() - abiertaEn) / 1000;
        int y = height - 34;
        g.fill(0, y - 8, width, height, 0x99000000);
        g.drawCenteredString(font, Component.translatable(MOD + ".te.caliz.ayuda", Math.max(0, seg - pasados)).getString(), width / 2, y,
                0xFFE8E2D8);
        g.drawCenteredString(font, Component.translatable(MOD + ".te.caliz.teclas").getString(), width / 2, y + 12, 0xFF8A807A);
    }

    private static void dibujarMundo(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || abierta == null) {
            return;
        }
        INV.set(new Matrix4f(e.getProjectionMatrix()).mul(e.getPoseStack().last().pose())).invert();
        camPos = e.getCamera().getPosition();
        abierta.dibujarMano(e.getPoseStack(), camPos);
    }

    /** El brazo derecho: la punta de la mano donde está el mouse (o bajando sobre el cáliz señalado); el brazo viene de atrás a la derecha. */
    private void dibujarMano(PoseStack ps, Vec3 cam) {
        Minecraft mc = Minecraft.getInstance();
        if (mano == null || mc.player == null) {
            return;
        }
        Vec3 meta = mano;
        int s = TeCliente.senalado;
        if (s >= 0) {
            Vec3 c = TeCliente.caliz(lado, s);
            if (c != null) {
                // sobre el cáliz: la mano baja hasta la orilla (y lo agarra si ya lo tomaste)
                double baja = tomo >= 0 ? Math.min(1, (System.currentTimeMillis() - tomoEn) / 200.0) : 0.4;
                meta = c.add(0, 0.62 - 0.22 * baja, 0).subtract(frente.scale(0.1));
            }
        } else {
            meta = mano.add(0, 0.18, 0);
        }
        manoVis = manoVis == null ? meta : manoVis.lerp(meta, 0.4);
        Vec3 arriba = new Vec3(0, 1, 0);
        // del hombro a la mano: el brazo viene de atrás, de la derecha y de arriba (donde está el hombro)
        Vec3 y = frente.scale(0.78).add(derecha.scale(-0.28)).add(arriba.scale(-0.52)).normalize();
        Vec3 up = arriba.subtract(y.scale(arriba.dot(y))).normalize();
        Vec3 z = up.scale(-1);
        Vec3 x = y.cross(z).normalize();
        Matrix3f rot = new Matrix3f((float) x.x, (float) x.y, (float) x.z, (float) y.x, (float) y.y, (float) y.z, (float) z.x, (float) z.y,
                (float) z.z);
        boolean delgado = "slim".equals(mc.player.getModelName());
        int packed = LevelRenderer.getLightColor(mc.level, BlockPos.containing(manoVis));
        int luz = LightTexture.pack(Math.max(LightTexture.block(packed), 10), LightTexture.sky(packed));
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        ps.pushPose();
        ps.translate(manoVis.x - cam.x, manoVis.y - cam.y, manoVis.z - cam.z);
        ps.mulPose(new Quaternionf().setFromNormalized(rot));
        ps.scale(0.55F, 0.55F, 0.55F);
        ps.translate((delgado ? 6.5 : 7.0) / 16, -(delgado ? 12.35 : 11.85) / 16, 0);
        if (mc.getEntityRenderDispatcher().getRenderer(mc.player) instanceof PlayerRenderer pr) {
            pr.renderRightHand(ps, buf, luz, mc.player);
        }
        ps.popPose();
        buf.endBatch();
    }
}
