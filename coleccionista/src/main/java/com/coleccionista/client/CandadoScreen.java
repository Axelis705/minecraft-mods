package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.CoreScreen;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.bloque.CandadoBlock;
import com.coleccionista.evento.Celdas;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * (Cliente) El candado de la celda, de cerca: la cámara se acerca al candado y la mano del jugador es el mouse. Se arrastra una rueda
 * hacia arriba para subir el número y hacia abajo para bajarlo (también la ruedita del mouse o las teclas), y el botón rojo prueba el
 * código. Cada toque mueve el brazo del jugador para los demás. Esc regresa.
 * <p>
 * Las medidas salen del modelo {@code candado_ruedas} (pixeles del bloque, mirando al norte, corrido 7 px hacia los barrotes).
 */
public class CandadoScreen extends CoreScreen {
    private static final String MOD = Coleccionista.MODID;
    /** Plano de enfrente de las ruedas (z del modelo ya corrido) y centro de la ventanita. */
    private static final double ZR = 19.15, XC = 7.975, YC = 8.0;
    /** Las tres ruedas (de izquierda a derecha vistas de frente): centro en u (px), medio ancho y medio alto. */
    private static final double SEP = 2.15, MEDIO_ANCHO = 0.98, MEDIO_ALTO = 2.45;
    /** El botón para probar (u, v en px). */
    private static final double BU0 = -1.3, BU1 = 1.25, BV0 = -5.1, BV1 = -3.1;
    /** Cuánto hay que arrastrar (px del candado) para pasar un número. */
    private static final double PASO = 1.5;
    /** Lo que quedó puesto en cada candado (como uno de verdad: las ruedas se quedan donde las dejaste). */
    private static final Map<BlockPos, int[]> PUESTO = new HashMap<>();
    /** La vista abierta (la usa el dibujo del mundo). */
    private static CandadoScreen abierta;
    /** Para saber a qué parte del candado apunta el mouse: la inversa de proyección·vista del último cuadro y dónde estaba la cámara. */
    private static final Matrix4f INV = new Matrix4f();
    private static Vec3 camPos;
    private static List<BlockPos> cerca = new ArrayList<>();
    private static long revisado;

    private int celda;
    private BlockPos pos;
    private Direction f = Direction.NORTH;
    /** Centro de la ventanita de las ruedas y sus ejes: derecha (vista de frente), arriba y hacia afuera. */
    private Vec3 origen, der, arriba = new Vec3(0, 1, 0), n;
    private final int[] dig = {0, 0, 0};
    /** Lo que se ve de cada rueda (se mueve suave hacia su número). */
    private final float[] vis = {0, 0, 0};
    private int arrastra = -1;
    private double v0;
    private float r0;
    /** Dónde está la mano sobre la cara del candado (px); null = el mouse no apunta al plano. */
    private double[] mano;
    /** La mano dibujada (se mueve suave hacia el mouse). */
    private Vec3 manoVis;
    private long apreto, resultadoEn, ultimoToque, teclado;
    private String res = "";
    private int cursor;
    private boolean empezo;

    public CandadoScreen() {
        super(Component.translatable(MOD + ".candado.titulo"));
    }

    /** (Cliente) Una vez al iniciar: dibujar los números de las ruedas y la mano, y escuchar cómo salió cada intento. */
    public static void registrar() {
        MinecraftForge.EVENT_BUS.addListener(CandadoScreen::dibujarMundo);
        ClientData.listen(Celdas.KEY_CANDADO, t -> {
            if (abierta != null && t != null && !t.getString("res").isEmpty()) {
                abierta.res = t.getString("res");
                abierta.resultadoEn = System.currentTimeMillis();
            }
        });
    }

    public static boolean abierta() {
        return abierta != null;
    }

    // ------------------------------------------------------------------ geometría

    /** Un punto del modelo (px, mirando al norte, ya corrido hacia los barrotes) en el mundo, según hacia dónde mira el candado. */
    private static Vec3 mundo(BlockPos p, Direction f, double x, double y, double z) {
        double wx, wz;
        switch (f) {
            case EAST -> {
                wx = 16 - z;
                wz = x;
            }
            case SOUTH -> {
                wx = 16 - x;
                wz = 16 - z;
            }
            case WEST -> {
                wx = z;
                wz = 16 - x;
            }
            default -> {
                wx = x;
                wz = z;
            }
        }
        return new Vec3(p.getX() + wx / 16, p.getY() + y / 16, p.getZ() + wz / 16);
    }

    /** Las medidas de un candado: {origen, derecha, afuera}. */
    private static Vec3[] marco(BlockPos p, Direction f) {
        Vec3 o = mundo(p, f, XC, YC, ZR);
        Vec3 d = mundo(p, f, XC - 1, YC, ZR).subtract(o).normalize();
        Vec3 a = mundo(p, f, XC, YC, ZR - 1).subtract(o).normalize();
        return new Vec3[] {o, d, a};
    }

    private Vec3 punto(double u, double v, double fuera) {
        return origen.add(der.scale(u / 16)).add(arriba.scale(v / 16)).add(n.scale(fuera / 16));
    }

    /** Qué hay bajo la mano: 0..2 una rueda, 3 el botón, -1 nada. */
    private static int parte(double u, double v) {
        for (int k = 0; k < 3; k++) {
            if (Math.abs(u - (k - 1) * SEP) <= MEDIO_ANCHO + 0.1 && Math.abs(v) <= MEDIO_ALTO + 0.3) {
                return k;
            }
        }
        return u >= BU0 && u <= BU1 && v >= BV0 && v <= BV1 ? 3 : -1;
    }

    /** El mouse (coordenadas de la pantalla) sobre la cara del candado, en px {u, v}; null si no se puede saber. */
    private double[] apuntar(double mx, double my) {
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
        double dn = d.dot(n);
        if (Math.abs(dn) < 1e-6) {
            return null;
        }
        double t = origen.subtract(o).dot(n) / dn;
        if (t < 0) {
            return null;
        }
        Vec3 h = o.add(d.scale(t)).subtract(origen);
        return new double[] {h.dot(der) * 16, h.dot(arriba) * 16};
    }

    // ------------------------------------------------------------------ abrir y cerrar

    @Override
    protected void init() {
        super.init();
        if (empezo) {
            return;
        }
        empezo = true;
        Minecraft mc = Minecraft.getInstance();
        CompoundTag t = ClientData.get(Celdas.KEY_CANDADO);
        BlockState st = t != null && t.contains("x") && mc.level != null ? mc.level.getBlockState(new BlockPos(t.getInt("x"), t.getInt("y"),
                t.getInt("z"))) : null;
        if (st == null || !(st.getBlock() instanceof CandadoBlock)) {
            mc.tell(this::onClose);
            return;
        }
        celda = Nbt.i(t, "celda");
        pos = new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z"));
        f = st.getValue(CandadoBlock.FACING);
        Vec3[] m = marco(pos, f);
        origen = m[0];
        der = m[1];
        n = m[2];
        int[] antes = PUESTO.get(pos);
        if (antes != null) {
            for (int k = 0; k < 3; k++) {
                dig[k] = antes[k];
                vis[k] = antes[k];
            }
        }
        abierta = this;
        // la cámara: frente al candado, un poco abajo del centro (se ven las ruedas y el botón)
        Cine.acercarCandado(punto(0, -0.8, 9.2), punto(0, -0.9, 0));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CHAIN_STEP, 1.2F, 0.5F));
        // la mano es el mouse: no se ve la flecha
        GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
    }

    @Override
    public void removed() {
        super.removed();
        if (abierta == this) {
            abierta = null;
        }
        if (pos != null) {
            PUESTO.put(pos, dig.clone());
        }
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
        Minecraft mc = Minecraft.getInstance();
        if (pos == null) {
            return;
        }
        if (mc.player == null || !mc.player.isAlive() || mc.player.distanceToSqr(Vec3.atCenterOf(pos)) > 25
                || !(mc.level.getBlockState(pos).getBlock() instanceof CandadoBlock)) {
            onClose();
            return;
        }
        if ("ok".equals(res) && System.currentTimeMillis() - resultadoEn > 1300) {
            onClose();
        }
    }

    // ------------------------------------------------------------------ la mano

    @Override
    protected boolean onClick(double x, double y, int b) {
        mano = apuntar(x, y);
        if (b != 0 || mano == null || "ok".equals(res)) {
            return true;
        }
        int p = parte(mano[0], mano[1]);
        if (p >= 0 && p < 3) {
            arrastra = p;
            v0 = mano[1];
            r0 = vis[p];
            tocar("rueda");
        } else if (p == 3) {
            probar();
        }
        return true;
    }

    @Override
    protected boolean onDrag(double x, double y, int b, double dx, double dy) {
        double[] m = apuntar(x, y);
        if (m != null) {
            mano = m;
        }
        if (arrastra >= 0 && mano != null) {
            // arrastrar hacia arriba sube el número; hacia abajo, lo baja
            float nuevo = (float) (r0 + (mano[1] - v0) / PASO);
            int antes = Math.round(vis[arrastra]);
            vis[arrastra] = nuevo;
            int ahora = Math.round(nuevo);
            if (ahora != antes) {
                dig[arrastra] = Math.floorMod(ahora, 10);
                clic();
            }
        }
        return true;
    }

    @Override
    protected boolean onRelease(double x, double y, int b) {
        arrastra = -1;
        return true;
    }

    @Override
    protected boolean onScroll(double x, double y, double delta) {
        double[] m = apuntar(x, y);
        int p = m == null ? -1 : parte(m[0], m[1]);
        if (p >= 0 && p < 3 && delta != 0) {
            girar(p, delta > 0 ? 1 : -1);
            tocar("rueda");
        }
        return true;
    }

    @Override
    public void mouseMoved(double x, double y) {
        double[] m = apuntar(x, y);
        if (m != null) {
            mano = m;
        }
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        teclado = System.currentTimeMillis();
        if (key >= 48 && key <= 57 || key >= 320 && key <= 329) {
            int d = key >= 320 ? key - 320 : key - 48;
            girar(cursor, Math.floorMod(d - dig[cursor] + 5, 10) - 5);
            cursor = (cursor + 1) % 3;
            tocar("rueda");
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> {
                girar(cursor, 1);
                tocar("rueda");
                return true;
            }
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> {
                girar(cursor, -1);
                tocar("rueda");
                return true;
            }
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_BACKSPACE -> {
                cursor = (cursor + 2) % 3;
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> {
                cursor = (cursor + 1) % 3;
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                probar();
                return true;
            }
            default -> {
                return superKey(key, scan, mods);
            }
        }
    }

    /** Gira una rueda {@code pasos} números (la animación la alcanza sola). */
    private void girar(int k, int pasos) {
        if (pasos == 0 || "ok".equals(res)) {
            return;
        }
        dig[k] = Math.floorMod(dig[k] + pasos, 10);
        vis[k] += pasos;
        clic();
    }

    private void clic() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_OFF, 1.9F, 0.35F));
        tocar("rueda");
    }

    /** Le avisa al servidor que la mano tocó el candado (los demás ven el brazo y oyen el clic). Como mucho cada 120 ms. */
    private void tocar(String tipo) {
        long now = System.currentTimeMillis();
        if (now - ultimoToque < 120) {
            return;
        }
        ultimoToque = now;
        CompoundTag t = new CompoundTag();
        t.putInt("celda", celda);
        t.putString("tipo", tipo);
        ClientData.send(Celdas.ACCION_TOQUE, t);
    }

    private void probar() {
        if ("ok".equals(res) || System.currentTimeMillis() - apreto < 600) {
            return;
        }
        apreto = System.currentTimeMillis();
        res = "";
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.STONE_BUTTON_CLICK_ON, 0.8F, 0.7F));
        ultimoToque = 0;
        tocar("boton");
        CompoundTag t = new CompoundTag();
        t.putInt("celda", celda);
        t.putInt("codigo", dig[0] * 100 + dig[1] * 10 + dig[2]);
        ClientData.send(Celdas.ACCION_CODIGO, t);
    }

    /** Para la prueba de cliente: arrastra la rueda {@code k} con la mano {@code px} pixeles (positivo = hacia arriba). */
    public void arrastrarPrueba(int k, double px, boolean soltar) {
        if (origen == null) {
            return;
        }
        if (arrastra != k) {
            arrastra = k;
            v0 = 0;
            r0 = vis[k];
        }
        mano = new double[] {(k - 1) * SEP, px};
        float nuevo = (float) (r0 + px / PASO);
        vis[k] = nuevo;
        dig[k] = Math.floorMod(Math.round(nuevo), 10);
        if (soltar) {
            arrastra = -1;
        }
    }

    /** Para la prueba de cliente. */
    public static CandadoScreen actual() {
        return abierta;
    }

    public void probarPrueba() {
        mano = new double[] {(BU0 + BU1) / 2, (BV0 + BV1) / 2};
        probar();
    }

    public int codigo() {
        return dig[0] * 100 + dig[1] * 10 + dig[2];
    }

    // ------------------------------------------------------------------ dibujo

    /** Abajo, la ayuda (unos segundos) y nada más: lo demás es el mundo (el candado, sus números y la mano). */
    @Override
    protected void draw(G g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        // las ruedas alcanzan su número
        for (int k = 0; k < 3; k++) {
            if (k != arrastra) {
                float meta = Math.round(vis[k]);
                vis[k] += (meta - vis[k]) * 0.35F;
                if (Math.abs(meta - vis[k]) < 0.01F) {
                    vis[k] = Math.floorMod((int) meta, 10);
                }
            }
        }
        Font font = Ui.font();
        String s = Component.translatable(MOD + ".candado.ayuda").getString();
        int y = height - 22;
        g.fill(0, y - 6, width, height, 0x88000000);
        g.drawCenteredString(font, s, width / 2, y, 0xFFB9B0A2);
    }

    /** Las medidas de cada candado cercano (para dibujar sus números aunque nadie lo esté usando). */
    private static void buscarCerca(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now - revisado < 1000 || mc.player == null) {
            return;
        }
        revisado = now;
        List<BlockPos> l = new ArrayList<>();
        BlockPos yo = mc.player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(yo.offset(-7, -3, -7), yo.offset(7, 4, 7))) {
            if (mc.level.getBlockState(p).getBlock() instanceof CandadoBlock) {
                l.add(p.immutable());
            }
        }
        cerca = l;
    }

    private static void dibujarMundo(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        Vec3 cam = e.getCamera().getPosition();
        CandadoScreen v = abierta;
        if (v != null) {
            INV.set(new Matrix4f(e.getProjectionMatrix()).mul(e.getPoseStack().last().pose())).invert();
            camPos = cam;
        }
        buscarCerca(mc);
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        PoseStack ps = e.getPoseStack();
        boolean alguno = false;
        for (BlockPos p : cerca) {
            if (v != null && p.equals(v.pos)) {
                continue;
            }
            BlockState st = mc.level.getBlockState(p);
            if (!(st.getBlock() instanceof CandadoBlock) || cam.distanceToSqr(Vec3.atCenterOf(p)) > 10 * 10) {
                continue;
            }
            int[] d = PUESTO.getOrDefault(p, new int[3]);
            Vec3[] m = marco(p, st.getValue(CandadoBlock.FACING));
            ruedas(mc, ps, buf, cam, p, m[0], m[2], new float[] {d[0], d[1], d[2]}, -1, 0);
            alguno = true;
        }
        if (v != null && v.origen != null) {
            long now = System.currentTimeMillis();
            float temblor = "mal".equals(v.res) && now - v.resultadoEn < 450 ? (float) Math.sin((now - v.resultadoEn) / 25.0) * 0.35F : 0;
            int marcada = now - v.teclado < 4000 ? v.cursor : -1;
            ruedas(mc, ps, buf, cam, v.pos, v.origen, v.n, v.vis, marcada, temblor);
            v.foquito(ps, buf, cam, now);
            v.dibujarMano(mc, ps, buf, cam, now);
            alguno = true;
        }
        if (alguno) {
            buf.endBatch();
        }
    }

    /** Los números de las tres ruedas, como en un cilindro: el de en medio derecho y los de arriba y abajo achatados y oscuros. */
    private static void ruedas(Minecraft mc, PoseStack ps, MultiBufferSource buf, Vec3 cam, BlockPos p, Vec3 o, Vec3 afuera, float[] vals,
                               int marcada, float temblor) {
        Font font = mc.font;
        int luz = luz(mc, p);
        float ang = (float) Math.toDegrees(Math.atan2(afuera.x, afuera.z));
        ps.pushPose();
        ps.translate(o.x - cam.x, o.y - cam.y, o.z - cam.z);
        ps.mulPose(Axis.YP.rotationDegrees(ang));
        ps.scale(1 / 16F, 1 / 16F, 1 / 16F); // de aquí en adelante, pixeles del candado
        ps.translate(temblor, 0, 0.03F);
        for (int k = 0; k < 3; k++) {
            float u = (k - 1) * (float) SEP;
            float r = vals[k];
            int base = Mth.floor(r);
            for (int j = -2; j <= 3; j++) {
                int dv = base + j;
                float th = (dv - r) * 36F;
                if (Math.abs(th) > 78) {
                    continue;
                }
                double rad = Math.toRadians(th);
                float y = (float) (-MEDIO_ALTO * 0.92 * Math.sin(rad));
                float c = (float) Math.cos(rad);
                int a = (int) (255 * Mth.clamp(c * c * 1.1F, 0, 1));
                String t = String.valueOf(Math.floorMod(dv, 10));
                ps.pushPose();
                ps.translate(u, y, 0);
                float esc = 0.27F;
                ps.scale(esc, -esc * c, esc);
                font.drawInBatch(t, -font.width(t) / 2F + 0.5F, -3.5F, (a << 24) | 0x1C1914, false, ps.last().pose(), buf,
                        Font.DisplayMode.POLYGON_OFFSET, 0, luz);
                ps.popPose();
            }
            if (k == marcada) {
                // la rueda que se mueve con las teclas: una rayita roja abajo
                quad(ps, buf, u - 0.9F, (float) -MEDIO_ALTO - 0.55F, u + 0.9F, (float) -MEDIO_ALTO - 0.3F, 0.02F, 0xFFC0392B);
            }
        }
        ps.popPose();
    }

    private static int luz(Minecraft mc, BlockPos p) {
        int packed = LevelRenderer.getLightColor(mc.level, p);
        return LightTexture.pack(Math.max(LightTexture.block(packed), 6), LightTexture.sky(packed));
    }

    /** Un rectángulo de color plano en el plano del candado (px). */
    private static void quad(PoseStack ps, MultiBufferSource buf, float x0, float y0, float x1, float y1, float z, int argb) {
        VertexConsumer vc = buf.getBuffer(RenderType.gui());
        Matrix4f m = ps.last().pose();
        int a = argb >>> 24 & 255, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        vc.vertex(m, x0, y0, z).color(r, g, b, a).endVertex();
        vc.vertex(m, x1, y0, z).color(r, g, b, a).endVertex();
        vc.vertex(m, x1, y1, z).color(r, g, b, a).endVertex();
        vc.vertex(m, x0, y1, z).color(r, g, b, a).endVertex();
    }

    /** El foquito de arriba: rojo apagado; verde cuando abre; rojo parpadeando cuando no es el código. */
    private void foquito(PoseStack ps, MultiBufferSource buf, Vec3 cam, long now) {
        int color;
        long dt = now - resultadoEn;
        if ("ok".equals(res)) {
            color = 0xFF4CE07A;
        } else if ("mal".equals(res) && dt < 1200) {
            color = (dt / 150) % 2 == 0 ? 0xFFFF3B2F : 0xFF5A1712;
        } else {
            return;
        }
        float ang = (float) Math.toDegrees(Math.atan2(n.x, n.z));
        Vec3 o = punto(0, 0, 0);
        ps.pushPose();
        ps.translate(o.x - cam.x, o.y - cam.y, o.z - cam.z);
        ps.mulPose(Axis.YP.rotationDegrees(ang));
        ps.scale(1 / 16F, 1 / 16F, 1 / 16F);
        // el foquito está en x 7.3..8.7, y 11.3..12.5, al frente en z 18.85 (0.3 px delante de las ruedas)
        quad(ps, buf, -0.7F, 3.3F, 0.7F, 4.5F, 0.32F, color);
        ps.popPose();
    }

    /** El brazo del jugador: la punta de la mano donde está el mouse; el brazo viene de abajo a la derecha, como en primera persona. */
    private void dibujarMano(Minecraft mc, PoseStack ps, MultiBufferSource buf, Vec3 cam, long now) {
        if (mano == null) {
            return;
        }
        // tocando (arrastrando o apretando el botón) la mano está pegada; si no, flota un poquito enfrente
        boolean toca = arrastra >= 0;
        double fuera = toca ? 0.15 : 0.8;
        long dp = now - apreto;
        if (dp < 260) {
            fuera = 0.8 - 1.3 * Math.sin(dp / 260.0 * Math.PI);
        }
        double u = Mth.clamp(mano[0], -14, 14), v = Mth.clamp(mano[1], -12, 14);
        Vec3 meta = punto(u, v, fuera);
        manoVis = manoVis == null ? meta : manoVis.lerp(meta, 0.45);
        Vec3 y = n.scale(-0.82).add(der.scale(-0.42)).add(arriba.scale(0.5)).normalize(); // del hombro a la mano
        Vec3 up = arriba.subtract(y.scale(arriba.dot(y))).normalize();
        Vec3 z = up.scale(-1);
        Vec3 x = y.cross(z).normalize();
        Matrix3f rot = new Matrix3f((float) x.x, (float) x.y, (float) x.z, (float) y.x, (float) y.y, (float) y.z, (float) z.x, (float) z.y,
                (float) z.z);
        boolean delgado = "slim".equals(mc.player.getModelName());
        int luz = luz(mc, pos);
        ps.pushPose();
        ps.translate(manoVis.x - cam.x, manoVis.y - cam.y, manoVis.z - cam.z);
        ps.mulPose(new Quaternionf().setFromNormalized(rot));
        ps.scale(0.8F, 0.8F, 0.8F);
        // la punta de la mano en el modelo del brazo derecho (pixeles /16): al centro del brazo, abajo (el brazo viene un poco ladeado)
        ps.translate((delgado ? 6.5 : 7.0) / 16, -(delgado ? 12.35 : 11.85) / 16, 0);
        if (mc.getEntityRenderDispatcher().getRenderer(mc.player) instanceof PlayerRenderer pr) {
            pr.renderRightHand(ps, buf, luz, mc.player);
        }
        ps.popPose();
    }
}
