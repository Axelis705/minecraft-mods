package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Maquina;
import com.coleccionista.prueba.Veintiuno;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * (Cliente) La final: veintiuno. Lo que se ve en la mesa (el mazo, las cartas de cada uno: la tapada boca abajo salvo para su dueño, o para
 * quien la espió, y volteándose al final de la ronda; cómo se reparten y cómo llega la carta que se pide) y los dos finalistas sentados,
 * dibujados aparte: inclinados sobre la mesa, la mano izquierda metida en su máquina y la derecha junto a sus cartas; cuando la máquina
 * aprieta se retuercen, y al que le cortan se le queda el muñón. En primera persona solo ves tus brazos (como en una mesa de verdad).
 * Y el HUD: la ronda, tus cartas, lo que se ve del otro, las rondas perdidas, lo último que pasó y el resultado.
 */
public final class VeintiunoCliente {
    private static final String MOD = Coleccionista.MODID;
    public static final ResourceLocation CARTAS = com.axeliscore.plat.Ids.rl(MOD, "textures/entity/cartas21.png");
    private static CompoundTag estado = new CompoundTag(), mio = new CompoundTag();
    private static boolean vista;
    private static PlayerModel<AbstractClientPlayer> ancho, delgado;
    /** El tamaño de una carta sobre la mesa (a lo largo, hacia el otro, y a lo ancho) y cuánto se separan. */
    private static final float LARGO = 0.36F, ANCHO = 0.24F, PASO = 0.27F;

    private VeintiunoCliente() {
    }

    public static void init() {
        ClientData.listen(Veintiuno.KEY, t -> estado = t);
        ClientData.listen(Veintiuno.KEY_MIO, t -> {
            mio = t;
            if (!Nbt.b(t, "turno")) {
                vista = false;
            }
        });
        com.axeliscore.fx.client.Hud.add(VeintiunoCliente::hud);
        MinecraftForge.EVENT_BUS.addListener(VeintiunoCliente::tick);
        MinecraftForge.EVENT_BUS.addListener(VeintiunoCliente::dibujarMundo);
        MinecraftForge.EVENT_BUS.addListener((RenderPlayerEvent.Pre e) -> {
            if (oculto(e.getEntity())) {
                e.setCanceled(true);
            }
        });
        // sentado a la mesa: la mano de primera persona no (se dibujan tus brazos de verdad, en la mesa)
        MinecraftForge.EVENT_BUS.addListener((RenderHandEvent e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && oculto(mc.player)) {
                e.setCanceled(true);
            }
        });
    }

    public static String fase() {
        String f = Nbt.s(estado, "fase");
        return f.isEmpty() ? "NADA" : f;
    }

    public static boolean activa() {
        return !"NADA".equals(fase());
    }

    public static CompoundTag estado() {
        return estado;
    }

    public static CompoundTag mio() {
        return mio;
    }

    public static int miLado() {
        return mio.contains("lado") ? mio.getInt("lado") : -1;
    }

    public static boolean meToca() {
        return Nbt.b(mio, "turno") && "TURNO".equals(fase());
    }

    public static double[] silla(int l) {
        ListTag t = estado.getList("silla" + l, Tag.TAG_DOUBLE);
        return t.size() < 4 ? null : new double[] {t.getDouble(0), t.getDouble(1), t.getDouble(2), t.getDouble(3)};
    }

    private static UUID id(int l) {
        return estado.hasUUID("id" + l) ? estado.getUUID("id" + l) : null;
    }

    private static String nombre(int l) {
        return Nbt.s(estado, "nombre" + l);
    }

    /** ¿Es uno de los finalistas, sentado a la mesa? (su cuerpo de verdad no se dibuja: se dibuja el de aquí) */
    public static boolean oculto(Entity e) {
        if (!activa() || e == null || !e.isPassenger()) {
            return false;
        }
        UUID u = e.getUUID();
        for (int l = 0; l < 2; l++) {
            if (u.equals(id(l)) && Nbt.b(estado, "vivo" + l) && silla(l) != null) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ cada tick: la pantalla del turno

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !activa()) {
            return;
        }
        if (meToca() && !vista && mc.screen == null) {
            vista = true;
            mc.setScreen(new VeintiunoScreen());
        }
    }

    // ------------------------------------------------------------------ dónde va cada cosa en la mesa

    private static Vec3 frente(double[] s) {
        double r = Math.toRadians(s[3]);
        return new Vec3(-Math.sin(r), 0, Math.cos(r));
    }

    private static Vec3 derecha(double[] s) {
        Vec3 f = frente(s);
        return new Vec3(-f.z, 0, f.x);
    }

    /** El centro de la carta k de ese lado (0 la tapada) sobre el tapete. */
    private static Vec3 carta(int l, int k, int n) {
        double[] s = silla(l);
        float paso = n > 6 ? PASO * 6F / n : PASO;
        Vec3 p = new Vec3(s[0], s[1] + 0.952 + k * 0.002, s[2]);
        return p.add(frente(s).scale(0.47 + paso * k)).add(derecha(s).scale(0.4));
    }

    private static Vec3 mazo() {
        double[] a = silla(0), b = silla(1);
        return new Vec3((a[0] + b[0]) / 2, a[1] + 0.952, (a[2] + b[2]) / 2);
    }

    /** ¿Ve la cara de la tapada de ese lado? (su dueño, quien la espió o todos cuando se voltean) y cuál es (0 si no la ve). */
    private static int tapada(int l) {
        if (estado.contains("oculta" + l)) {
            return estado.getInt("oculta" + l);
        }
        int yo = miLado();
        if (yo == l) {
            return Nbt.i(mio, "oculta");
        }
        if (yo == 1 - l && mio.contains("espia")) {
            return mio.getInt("espia");
        }
        return 0;
    }

    // ------------------------------------------------------------------ dibujar

    private static int luz(Minecraft mc, Vec3 p, int min) {
        int packed = LevelRenderer.getLightColor(mc.level, BlockPos.containing(p));
        return LightTexture.pack(Math.max(LightTexture.block(packed), min), LightTexture.sky(packed));
    }

    private static boolean enMesa() {
        String f = fase();
        return "SENTAR".equals(f) || "REGLAS".equals(f) || "REPARTE".equals(f) || "TURNO".equals(f) || "REVELA".equals(f) || "CASTIGO".equals(f)
                || "FIN".equals(f);
    }

    private static void dibujarMundo(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !activa() || !enMesa()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || silla(0) == null || silla(1) == null) {
            return;
        }
        Vec3 cam = e.getCamera().getPosition();
        if (cam.distanceToSqr(mazo()) > 64 * 64) {
            return;
        }
        PoseStack ps = e.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        float pt = e.getPartialTick();
        long gt = mc.level.getGameTime();
        float dt = gt + pt - estado.getLong("desde");
        String f = fase();
        // el mazo: un montoncito de cartas boca abajo en el centro
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(CARTAS));
        Vec3 m = mazo();
        int enMazo = "REPARTE".equals(f) ? Veintiuno.CARTAS - Math.min(4, Math.max(0, (int) ((dt + 2) / 8F))) : Nbt.i(estado, "mazo");
        int luzMesa = luz(mc, m.add(0, 0.5, 0), 12);
        double[] s0 = silla(0);
        for (int i = 0; i < enMazo; i++) {
            cartaPlana(vc, ps, cam, m.add(0, i * 0.006, 0), frente(s0), 0, 1F, 0F, luzMesa, i * 3F);
        }
        // las cartas de cada lado
        for (int l = 0; l < 2; l++) {
            if (id(l) == null || !Nbt.b(estado, "tieneOculta" + l)) {
                continue;
            }
            double[] s = silla(l);
            int[] vis = estado.getIntArray("visibles" + l);
            int n = vis.length + 1;
            for (int k = 0; k < n; k++) {
                Vec3 p = carta(l, k, n);
                float llega = 1F;
                if ("REPARTE".equals(f)) {
                    // el orden del reparto: la tapada del 0, la del 1, la visible del 0, la del 1
                    int orden = k == 0 ? l : 2 + l;
                    if (k > 1) {
                        continue;
                    }
                    llega = Mth.clamp((dt - (8 * (orden + 1) - 6)) / 6F, 0F, 1F);
                    if (llega <= 0F) {
                        continue;
                    }
                } else if (k == n - 1 && k >= 2 && "pedir".equals(Nbt.s(estado, "accion")) && Nbt.i(estado, "accionDe") == l) {
                    llega = Mth.clamp((gt + pt - estado.getLong("accionEn")) / 7F, 0F, 1F);
                }
                if (llega < 1F) {
                    float a = suave(llega);
                    p = m.lerp(p, a).add(0, Math.sin(a * Math.PI) * 0.12, 0);
                }
                int cara = k == 0 ? tapada(l) : vis[k - 1];
                float giro = 0F;
                if (k == 0 && ("REVELA".equals(f) && estado.contains("oculta" + l))) {
                    // se voltea al empezar
                    giro = Mth.clamp(dt / 10F, 0F, 1F);
                    if (giro < 0.5F) {
                        cara = l == miLado() ? cara : (mio.contains("espia") && miLado() == 1 - l ? cara : 0);
                    }
                }
                cartaPlana(vc, ps, cam, p, frente(s), cara, 1F, giro, luzMesa, 0F);
            }
        }
        buf.endBatch(RenderType.entityCutoutNoCull(CARTAS));
        // los dos finalistas
        for (int l = 0; l < 2; l++) {
            UUID id = id(l);
            if (id == null || !Nbt.b(estado, "vivo" + l)) {
                continue;
            }
            Entity ent = entidad(mc, id);
            if (ent == null || !ent.isPassenger()) {
                continue;
            }
            boolean yo = ent == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON;
            dibujarFinalista(mc, ps, buf, cam, id, ent, l, dt, gt + pt, yo);
        }
        buf.endBatch();
    }

    private static Entity entidad(Minecraft mc, UUID id) {
        if (mc.player != null && mc.player.getUUID().equals(id)) {
            return mc.player;
        }
        for (Entity x : mc.level.entitiesForRendering()) {
            if (x.getUUID().equals(id)) {
                return x;
            }
        }
        return null;
    }

    private static float suave(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /**
     * Una carta acostada en (p), con su "arriba" hacia {@code hacia}. {@code cara}: 0 el reverso, 1..11 el número. {@code giro} (0..1): se
     * está volteando (de lado, a lo largo).
     */
    private static void cartaPlana(VertexConsumer vc, PoseStack ps, Vec3 cam, Vec3 p, Vec3 hacia, int cara, float escala, float giro, int luz,
            float torcida) {
        ps.pushPose();
        ps.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
        float yaw = (float) Math.toDegrees(Math.atan2(hacia.x, hacia.z));
        ps.mulPose(Axis.YP.rotationDegrees(yaw + torcida));
        if (giro > 0F && giro < 1F) {
            ps.translate(0, Math.sin(giro * Math.PI) * 0.14, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(180F * giro));
            if (giro > 0.5F) {
                // ya del otro lado: la cara hacia arriba
                ps.mulPose(Axis.ZP.rotationDegrees(180F));
            }
        }
        ps.scale(escala, escala, escala);
        Matrix4f mt = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float u0 = (cara % 8) * 64F / 512F, v0 = (cara / 8) * 96F / 256F, u1 = u0 + 64F / 512F, v1 = v0 + 96F / 256F;
        float w = ANCHO / 2, h = LARGO / 2;
        // +z local es "hacia": la orilla de arriba de la carta (v0) va hacia allá
        vc.vertex(mt, -w, 0, h).color(255, 255, 255, 255).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
        vc.vertex(mt, w, 0, h).color(255, 255, 255, 255).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
        vc.vertex(mt, w, 0, -h).color(255, 255, 255, 255).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
        vc.vertex(mt, -w, 0, -h).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
        ps.popPose();
    }

    private static PlayerInfo info(UUID id) {
        var c = Minecraft.getInstance().getConnection();
        return id == null || c == null ? null : c.getPlayerInfo(id);
    }

    /** Uno de los finalistas, sentado: inclinado sobre la mesa, la mano izquierda en la máquina y la derecha junto a sus cartas. */
    private static void dibujarFinalista(Minecraft mc, PoseStack ps, MultiBufferSource buf, Vec3 cam, UUID id, Entity ent, int l, float dt,
            float ahora, boolean soloBrazos) {
        if (ancho == null) {
            ancho = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            delgado = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }
        double[] s = silla(l);
        PlayerInfo i = info(id);
        boolean slim = i != null ? "slim".equals(i.getModelName()) : "slim".equals(DefaultPlayerSkin.getSkinModelName(id));
        ResourceLocation piel = i != null ? i.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id);
        PlayerModel<AbstractClientPlayer> m = slim ? delgado : ancho;
        m.setAllVisible(true);
        m.young = false;
        m.crouching = false;
        m.riding = false;
        String f = fase();
        boolean miTurno = "TURNO".equals(f) && Nbt.i(estado, "turno") == l;
        // la pose de siempre
        float inclina = Corte.INCLINA, cuerpo = 0F, hx = 0.18F, hy = 0F, hz = 0F;
        float ax = Corte.AX, ay = Corte.AY, bx = -1.05F, by = 0.22F, bz = 0.05F;
        if (miTurno) {
            // pensando: tamborilea con los dedos (el brazo derecho sube y baja un poquito) y mira sus cartas
            bx += 0.06F * (float) Math.sin(ahora * 0.9F);
            hx = 0.42F;
            hy = -0.25F;
        } else if (Nbt.i(estado, "turno") == 1 - l && "TURNO".equals(f)) {
            hx = 0.1F;
        }
        // el castigo: se retuerce
        Maquina maq = maquina(mc, s);
        boolean corto = false;
        if ("CASTIGO".equals(f) && Nbt.i(estado, "perdedor") == l && maq != null) {
            float t = maq.desde(0F);
            int st = maq.estado();
            float fuerza = st == Maquina.APRIETA ? Math.max(0F, 1F - t / 50F) : st == Maquina.CHISPAS ? Math.max(0F, 1F - t / 60F) : 0F;
            if (st == Maquina.CORTA) {
                fuerza = t < Maquina.T_CAE ? 0.3F : Math.max(0F, 1.2F - (t - Maquina.T_CAE) / 30F);
                corto = maq.cortado(0F);
            }
            float sac = (float) (Math.sin(ahora * 2.3F) * Math.cos(ahora * 1.7F));
            hx = 0.18F - 0.65F * Math.min(1F, fuerza) + 0.15F * fuerza * sac;
            hz = 0.2F * fuerza * sac;
            cuerpo = 7F * fuerza * sac;
            inclina = Corte.INCLINA + 12F * Math.min(1F, fuerza);
            bx = -1.05F - 0.9F * Math.min(1F, fuerza);
            by = 0.22F + 0.5F * fuerza;
            ax = Corte.AX + 0.04F * fuerza * sac;
            if (corto) {
                // se desangra: deja de moverse y se va quedando echado hacia atrás, la cabeza colgando y los brazos caídos (como su cuerpo)
                float muere = t < Maquina.T_CAE + 35 ? 0F : Math.min(1F, (t - Maquina.T_CAE - 35) / 30F);
                muere = muere * muere * (3 - 2 * muere);
                float[] cab = Corte.cabezaMuerto(Math.abs(id.hashCode()) % 3);
                inclina += (Corte.MUERTO_INCLINA - inclina) * muere;
                cuerpo *= 1F - muere;
                hx += (cab[0] - hx) * muere;
                hy += (cab[1] - hy) * muere;
                hz += (cab[2] - hz) * muere;
                bx += (Corte.MUERTO_BRAZO - bx) * muere;
                by += (0F - by) * muere;
                bz += (0.15F - bz) * muere;
                ax += (Corte.MUERTO_BRAZO - ax) * muere;
            }
        }
        m.head.setRotation(hx, hy, hz);
        m.body.setRotation(0, 0, 0);
        m.leftArm.setRotation(ax, ay, Corte.AZ);
        m.rightArm.setRotation(bx, by, bz);
        m.rightLeg.setRotation(-1.4137167F, 0.31415927F, 0.07853982F);
        m.leftLeg.setRotation(-1.4137167F, -0.31415927F, -0.07853982F);
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
        Vec3 pos = new Vec3(s[0], s[1], s[2]);
        int luz = luz(mc, pos.add(0, 1.2, 0), 11);
        ps.pushPose();
        ps.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        ps.mulPose(Axis.YP.rotationDegrees(180F - (float) s[3]));
        ps.mulPose(Axis.ZP.rotationDegrees(cuerpo));
        ps.mulPose(Axis.XP.rotationDegrees(inclina));
        ps.translate(0, 0.07, 0);
        ps.scale(-1, -1, 1);
        ps.scale(0.9375F, 0.9375F, 0.9375F);
        ps.translate(0, -1.501, 0);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(piel));
        if (soloBrazos) {
            // en primera persona: solo los brazos (la cabeza y el cuerpo los tapa la cámara)
            m.head.visible = m.hat.visible = m.body.visible = m.jacket.visible = false;
            m.rightLeg.visible = m.leftLeg.visible = m.rightPants.visible = m.leftPants.visible = false;
        }
        if (corto) {
            m.leftArm.visible = m.leftSleeve.visible = false;
        }
        m.renderToBuffer(ps, vc, luz, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        if (corto) {
            // el muñón (la mitad de arriba del brazo, con el corte); la otra mitad se quedó en la máquina
            Corte.munon(m, ps, vc, luz);
        }
        if (!soloBrazos) {
            com.axeliscore.plat.client.Draw d = new com.axeliscore.plat.client.Draw(buf);
            com.axeliscore.vestir.client.VestuarioCliente.dibujarJugador(id, ent, slim, new ModelPart[] {m.head, m.body, m.rightArm, m.leftArm,
                    m.rightLeg, m.leftLeg}, ps, d, luz, OverlayTexture.NO_OVERLAY, 1F, false);
            d.flush();
        }
        m.setAllVisible(true);
        ps.popPose();
    }

    /** La máquina de ese lado (la más cercana a donde debe estar). */
    private static Maquina maquina(Minecraft mc, double[] s) {
        Vec3 p = Veintiuno.posMaquina(s);
        Maquina mejor = null;
        double d = 1.0;
        for (Entity x : mc.level.entitiesForRendering()) {
            if (x instanceof Maquina mq) {
                double dd = mq.position().distanceToSqr(p);
                if (dd < d) {
                    d = dd;
                    mejor = mq;
                }
            }
        }
        return mejor;
    }

    // ------------------------------------------------------------------ las cartas en la pantalla (para la pantalla del turno)

    /** Las cartas de un lado en fila (desde x hacia la derecha si {@code izquierda}, o hacia la izquierda), con su total. */
    static void dibujarMano(GuiGraphics g0, int l, int x, int y, boolean izquierda) {
        if (l < 0 || l > 1 || !estado.hasUUID("id" + l)) {
            return;
        }
        Font font = Ui.font();
        G g = new G(g0);
        int[] vis = estado.getIntArray("visibles" + l);
        int n = vis.length + 1, w = 30, h = 45, paso = 32;
        int ancho = (n - 1) * paso + w;
        int x0 = izquierda ? x : x - ancho;
        int tap = tapada(l);
        int suma = 0;
        for (int v : vis) {
            suma += v;
        }
        String quien = l == miLado() ? Component.translatable(MOD + ".v21.hud.tu").getString() : nombre(l);
        String tot = tap > 0 ? String.valueOf(suma + tap) : "? + " + suma;
        String titulo = quien + ": " + tot;
        int tc = tap > 0 && suma + tap > Veintiuno.META ? EventoHud.ROJO_CL : EventoHud.PORCELANA;
        g.drawString(font, titulo, izquierda ? x0 : x - font.width(titulo), y - 12, tc);
        for (int k = 0; k < n; k++) {
            int cara = k == 0 ? tap : vis[k - 1];
            int cx = x0 + k * paso;
            int u = (cara % 8) * 64, v = (cara / 8) * 96;
            g0.blit(CARTAS, cx, y, w, h, u, v, 64, 96, 512, 256);
        }
    }

    // ------------------------------------------------------------------ el HUD

    private static String t(String k, Object... a) {
        return Component.translatable(MOD + ".v21.hud." + k, a).getString();
    }

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !activa() || !EventoHud.visible() || !enMesa() || "SENTAR".equals(fase())) {
            return;
        }
        String f = fase();
        Font font = Ui.font();
        long gt = mc.level == null ? 0 : mc.level.getGameTime();
        float dt = gt - estado.getLong("desde");
        int yo = miLado();
        if (estado.hasUUID("id0") && estado.hasUUID("id1")) {
            EventoHud.lado(g, t("ronda", Nbt.i(estado, "ronda"), nombre(0), nombre(1)), EventoHud.ROJO_CL, true);
            int max = Math.max(1, Nbt.i(estado, "golpesMax"));
            for (int l = 0; l < 2; l++) {
                StringBuilder sb = new StringBuilder();
                int gl = Nbt.i(estado, "golpes" + l);
                for (int k = 0; k < max; k++) {
                    sb.append(k < gl ? "●" : "○");
                }
                EventoHud.lado(g, t("golpes", nombre(l), sb.toString()), gl > 0 ? 0xFFE08070 : 0xFFA89E96, false);
            }
        }
        if (yo >= 0 && !"FIN".equals(f)) {
            int tap = Nbt.i(mio, "oculta");
            int[] vis = estado.getIntArray("visibles" + yo);
            int suma = tap;
            for (int v : vis) {
                suma += v;
            }
            EventoHud.lado(g, t("tus_cartas", suma), suma > Veintiuno.META ? EventoHud.ROJO_CL : EventoHud.PORCELANA, false);
            int otro = 1 - yo;
            int[] vo = estado.getIntArray("visibles" + otro);
            int so = 0;
            for (int v : vo) {
                so += v;
            }
            if (mio.contains("espia")) {
                EventoHud.lado(g, t("rival_espiado", nombre(otro), so + mio.getInt("espia")), 0xFFD8D0C8, false);
            } else {
                EventoHud.lado(g, t("rival", nombre(otro), so), 0xFFD8D0C8, false);
            }
            if (Nbt.b(estado, "comodines")) {
                String c = (Nbt.b(estado, "espiar" + yo) && !Nbt.b(estado, "espio" + yo) ? "✔ " : "✘ ")
                        + Component.translatable(MOD + ".v21.boton.espiar").getString() + "  ·  "
                        + (Nbt.b(estado, "regresar" + yo) ? "✔ " : "✘ ") + Component.translatable(MOD + ".v21.boton.regresar").getString();
                EventoHud.lado(g, t("comodines", c), 0xFFA89E96, false);
            }
        }
        ListTag cola = estado.getList("cola", Tag.TAG_STRING);
        if (!cola.isEmpty()) {
            EventoHud.lado(g, t("esperan", cola.size()), 0xFF8A807A, false);
        }
        // lo último que pasó (2 segundos)
        String ac = Nbt.s(estado, "accion");
        int de = Nbt.i(estado, "accionDe");
        if (!ac.isEmpty() && de >= 0 && gt - estado.getLong("accionEn") < 50 && ("TURNO".equals(f) || "REVELA".equals(f))) {
            float a = Math.min(1F, (50 - (gt - estado.getLong("accionEn"))) / 10F);
            EventoHud.texto(g, font, t("a." + ac, nombre(de), nombre(1 - de)), w / 2, h / 2 + 30, 1.3F, Ui.alpha(0xFFE8DCC8, a));
        }
        switch (f) {
            case "TURNO" -> {
                int tu = Nbt.i(estado, "turno");
                if (tu != yo) {
                    EventoHud.texto(g, font, t("turno_de", nombre(tu)), w / 2, h / 2 - 70, 1.6F, 0xFFD8D0C8);
                }
            }
            case "REPARTE" -> EventoHud.texto(g, font, t("reparte", Nbt.i(estado, "ronda")), w / 2, h / 2 - 70, 1.8F, EventoHud.ORO);
            case "REVELA" -> {
                int a0 = Nbt.i(estado, "total0"), a1 = Nbt.i(estado, "total1");
                float a = Math.min(1F, Math.max(0F, (dt - 10F) / 8F));
                EventoHud.texto(g, font, t("totales", nombre(0), a0, nombre(1), a1), w / 2, h / 2 - 76, 2.0F, Ui.alpha(0xFFF4ECE4, a));
                String r;
                int col;
                if ("empate".equals(Nbt.s(estado, "resultado"))) {
                    r = t("empate");
                    col = 0xFFD8D0C8;
                } else {
                    int p = Nbt.i(estado, "perdedor");
                    int tp = p == 0 ? a0 : a1;
                    r = tp > Veintiuno.META ? t("se_paso", nombre(p)) : t("pierde", nombre(p));
                    col = EventoHud.ROJO_CL;
                }
                float b = Math.min(1F, Math.max(0F, (dt - 30F) / 8F));
                EventoHud.texto(g, font, r, w / 2, h / 2 - 50, 1.5F, Ui.alpha(col, b));
            }
            case "CASTIGO" -> {
                int p = Nbt.i(estado, "perdedor");
                int gl = Nbt.i(estado, "golpes" + p), max = Nbt.i(estado, "golpesMax");
                String k = Nbt.b(estado, "cortado") ? "corta" : gl >= max - 1 && max >= 3 ? "chispas" : "aprieta";
                float a = Math.min(1F, dt / 6F);
                EventoHud.texto(g, font, t(k, nombre(p)), w / 2, h / 2 - 70, k.equals("corta") ? 3.0F : 1.8F, Ui.alpha(EventoHud.ROJO_CL, a));
            }
            default -> {
            }
        }
    }
}
