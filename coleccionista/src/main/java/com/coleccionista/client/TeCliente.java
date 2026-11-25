package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.ManoGigante;
import com.coleccionista.prueba.Te;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * (Cliente) La hora del té. Lo que se ve en la mesa (los cálices de cada quien, la muñeca del Coleccionista si alguien juega contra ella,
 * y el que bebe: su cuerpo se dibuja aparte, sentado, alzando el cáliz, bebiendo, y luego mareado o ahogándose y poniéndose verde), lo que
 * dice el HUD en cada momento ("¡Piedra… papel… o tijera!", quién bebe, a salvo o veneno) y qué pantallas abrir (elegir jugada, elegir
 * cáliz con la mano) y la cámara de cine de quien bebe.
 */
public final class TeCliente {
    private static final String MOD = Coleccionista.MODID;
    private static CompoundTag estado = new CompoundTag(), mio = new CompoundTag();
    private static boolean jugadaVista, calizVista;
    private static long cineDe = -1;
    /** El cáliz que señala la mano en la pantalla de elegir (-1 ninguno). */
    public static int senalado = -1;
    private static PlayerModel<AbstractClientPlayer> ancho, delgado;
    private static BlockState copa, muneca, silla;

    private TeCliente() {
    }

    public static void init() {
        ClientData.listen(Te.KEY, t -> estado = t);
        ClientData.listen(Te.KEY_MIO, t -> {
            mio = t;
            if (!Nbt.b(t, "jugar")) {
                jugadaVista = false;
            }
            if (!Nbt.b(t, "caliz")) {
                calizVista = false;
            }
        });
        com.axeliscore.fx.client.Hud.add(TeCliente::hud);
        MinecraftForge.EVENT_BUS.addListener(TeCliente::tick);
        MinecraftForge.EVENT_BUS.addListener(TeCliente::dibujarMundo);
        // el que bebe se dibuja aparte (sentado, con el cáliz): su cuerpo de verdad no
        MinecraftForge.EVENT_BUS.addListener((RenderPlayerEvent.Pre e) -> {
            if (oculto(e.getEntity().getUUID())) {
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

    public static boolean meToca(String que) {
        return Nbt.b(mio, que);
    }

    // ------------------------------------------------------------------ la mesa

    /** La silla de un lado de la mesa: {x, y, z, yaw}, o null. */
    public static double[] silla(int lado) {
        ListTag l = estado.getList("silla" + lado, Tag.TAG_DOUBLE);
        return l.size() < 4 ? null : new double[] {l.getDouble(0), l.getDouble(1), l.getDouble(2), l.getDouble(3)};
    }

    public static Vec3 caliz(int lado, int k) {
        double[] s = silla(lado);
        return s == null ? null : Te.caliz(s[0], s[1], s[2], (float) s[3], k);
    }

    public static int copas(int lado) {
        return Nbt.i(estado, "copas" + lado);
    }

    private static boolean bebiendo() {
        String f = fase();
        return ("BEBE".equals(f) || "MUERE".equals(f)) && estado.contains("bebeDesde");
    }

    private static int lado(UUID id) {
        for (int l = 0; l < 2; l++) {
            if (estado.hasUUID("id" + l) && estado.getUUID("id" + l).equals(id)) {
                return l;
            }
        }
        return -1;
    }

    /** Ticks desde que empezó a beber (con la fracción del cuadro). */
    private static float tb(float pt) {
        var lvl = Minecraft.getInstance().level;
        return lvl == null ? 0 : lvl.getGameTime() + pt - estado.getLong("bebeDesde");
    }

    /** ¿Ese jugador (o bot) está bebiendo y se dibuja aparte? */
    public static boolean oculto(UUID id) {
        if (!activa()) {
            return false;
        }
        if (CalizScreen.actual() != null) {
            int yo = miLado();
            if (yo >= 0 && estado.hasUUID("id" + yo) && estado.getUUID("id" + yo).equals(id)) {
                return true;
            }
        }
        if (!bebiendo()) {
            return false;
        }
        int l = lado(id);
        if (l < 0 || l != Nbt.i(estado, "perdedor")) {
            return false;
        }
        float t = tb(0);
        return t < (Nbt.b(estado, "veneno") ? Te.T_MUERE : Te.T_SALVO);
    }

    // ------------------------------------------------------------------ cada tick: abrir las pantallas y la cámara

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !activa()) {
            return;
        }
        if (meToca("jugar") && !jugadaVista && mc.screen == null) {
            jugadaVista = true;
            mc.setScreen(new JugadaScreen());
        }
        if (meToca("caliz") && !calizVista && mc.screen == null) {
            calizVista = true;
            mc.setScreen(new CalizScreen());
        }
        if (meToca("cine") && mio.getLong("bebeDesde") != cineDe) {
            cineDe = mio.getLong("bebeDesde");
            double[] s = silla(miLado());
            if (s != null) {
                Cine.beber(new Vec3(s[0], s[1] + 1.35, s[2]), (float) s[3], 6.2F);
            }
        }
    }

    // ------------------------------------------------------------------ el HUD

    private static String nombre(int l) {
        if (l == 1 && Nbt.b(estado, "muneca")) {
            return Component.translatable(MOD + ".te.muneca").getString();
        }
        return Nbt.s(estado, "nombre" + l);
    }

    private static String jugada(int g) {
        return switch (g) {
            case ManoGigante.PIEDRA -> Component.translatable(MOD + ".te.piedra").getString();
            case ManoGigante.PAPEL -> Component.translatable(MOD + ".te.papel").getString();
            case ManoGigante.TIJERA -> Component.translatable(MOD + ".te.tijera").getString();
            default -> "?";
        };
    }

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !activa()) {
            return;
        }
        String f = fase();
        // en la cámara de cine (bebiendo) solo se dice si se salvó o no
        boolean cine = !EventoHud.visible();
        if (cine && !(Cine.activo() && ("BEBE".equals(f) || "MUERE".equals(f)))) {
            return;
        }
        Font font = Ui.font();
        long ahora = mc.level == null ? 0 : mc.level.getGameTime();
        float dt = ahora - estado.getLong("desde");
        if ("PAREJAS".equals(f) || "HABLANDO".equals(f)) {
            EventoHud.lado(g, Component.translatable(MOD + ".te.hud.pareja").getString(), EventoHud.ORO, true);
            return;
        }
        if (Nbt.i(estado, "duelos") <= 0 || "FIN".equals(f) || "PASILLO".equals(f) || "LLEGANDO".equals(f) || "JAULAS".equals(f)) {
            return;
        }
        int duelo = Nbt.i(estado, "duelo"), duelos = Nbt.i(estado, "duelos");
        if (duelo >= 1 && duelo <= duelos && !cine) {
            EventoHud.lado(g, Component.translatable(MOD + ".te.hud.duelo", duelo, duelos, nombre(0), nombre(1)).getString(), EventoHud.ROJO_CL,
                    true);
            EventoHud.lado(g, Component.translatable(MOD + ".te.hud.calices", nombre(0), Integer.bitCount(copas(0)), nombre(1),
                    Integer.bitCount(copas(1))).getString(), 0xFFA89E96, false);
        }
        int yo = miLado();
        if (Nbt.b(mio, "enJaula") && !cine) {
            EventoHud.lado(g, Component.translatable(MOD + ".te.hud.jaula").getString(), 0xFFA89E96, false);
        }
        switch (f) {
            case "JUGADA" -> {
                for (int l = 0; l < 2; l++) {
                    boolean eligio = Nbt.b(estado, "eligio" + l);
                    String s = Component.translatable(MOD + (eligio ? ".te.hud.eligio" : ".te.hud.pensando"), nombre(l)).getString();
                    EventoHud.lado(g, s, eligio ? EventoHud.PORCELANA : 0xFF8A807A, false);
                }
            }
            case "MANOS" -> {
                // "¡Piedra… papel… o tijera!" al ritmo de las manos, y luego las jugadas
                String s;
                int c = 0xFFF4ECE4;
                if (dt < ManoGigante.T_SACUDIDA) {
                    s = Component.translatable(MOD + ".te.canto.1").getString();
                } else if (dt < 2 * ManoGigante.T_SACUDIDA) {
                    s = Component.translatable(MOD + ".te.canto.2").getString();
                } else if (dt < ManoGigante.T_MUESTRA) {
                    s = Component.translatable(MOD + ".te.canto.3").getString();
                } else {
                    s = nombre(0) + ": " + jugada(Nbt.i(estado, "j0")) + "   ·   " + nombre(1) + ": " + jugada(Nbt.i(estado, "j1"));
                    c = EventoHud.ORO;
                }
                EventoHud.texto(g, font, s, w / 2, h / 2 - 70, 2.0F, c);
                String r = Nbt.s(estado, "resultado");
                if ("empate".equals(r)) {
                    EventoHud.texto(g, font, Component.translatable(MOD + ".te.hud.empate").getString(), w / 2, h / 2 - 46, 1.4F, 0xFFD8D0C8);
                } else if ("pierde".equals(r)) {
                    int p = Nbt.i(estado, "perdedor");
                    String q = p == yo ? Component.translatable(MOD + ".te.hud.pierdes").getString()
                            : Component.translatable(MOD + ".te.hud.bebe", nombre(p)).getString();
                    EventoHud.texto(g, font, q, w / 2, h / 2 - 46, 1.5F, EventoHud.ROJO_CL);
                }
            }
            case "CALIZ" -> {
                int p = Nbt.i(estado, "perdedor");
                if (p == yo) {
                    EventoHud.lado(g, Component.translatable(MOD + ".te.hud.elige_caliz").getString(), EventoHud.ORO, false);
                } else {
                    EventoHud.lado(g, Component.translatable(MOD + ".te.hud.elige_otro", nombre(p)).getString(), 0xFFD8D0C8, false);
                }
            }
            case "BEBE", "MUERE" -> {
                float t = tb(0);
                int p = Nbt.i(estado, "perdedor");
                if (t < Te.T_TRAGA) {
                    EventoHud.texto(g, font, Component.translatable(MOD + ".te.hud.bebiendo", nombre(p)).getString(), w / 2, h / 2 - 70, 1.6F,
                            0xFFD8D0C8);
                } else if (Nbt.b(estado, "veneno")) {
                    float a = Math.min(1F, (t - Te.T_TRAGA) / 10F);
                    EventoHud.texto(g, font, Component.translatable(MOD + ".te.hud.veneno").getString(), w / 2, h / 2 - 76, 3.0F,
                            Ui.alpha(0xFF5CE05A, a));
                    EventoHud.texto(g, font, nombre(p), w / 2, h / 2 - 44, 1.6F, Ui.alpha(0xFFD8D0C8, a));
                } else if (t < Te.T_SALVO) {
                    float a = Math.min(1F, (t - Te.T_TRAGA) / 8F);
                    EventoHud.texto(g, font, Component.translatable(MOD + ".te.hud.salvo").getString(), w / 2, h / 2 - 76, 2.6F,
                            Ui.alpha(EventoHud.PORCELANA, a));
                    EventoHud.texto(g, font, nombre(p), w / 2, h / 2 - 48, 1.4F, Ui.alpha(0xFFA89E96, a));
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ dibujar la mesa

    private static int luz(Minecraft mc, Vec3 p) {
        int packed = LevelRenderer.getLightColor(mc.level, BlockPos.containing(p));
        return LightTexture.pack(Math.max(LightTexture.block(packed), 12), LightTexture.sky(packed));
    }

    private static void dibujarMundo(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !activa()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || silla(0) == null) {
            return;
        }
        if (copa == null) {
            copa = Complejo.estado("coleccionista:copa");
            muneca = Complejo.estado("coleccionista:muneca[facing=north]");
            silla = Complejo.estado("coleccionista:silla[facing=north]");
            if (copa == null) {
                return;
            }
        }
        Vec3 cam = e.getCamera().getPosition();
        if (cam.distanceToSqr(caliz(0, 2)) > 60 * 60) {
            return;
        }
        PoseStack ps = e.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        float pt = e.getPartialTick();
        boolean bebe = bebiendo();
        int perdedor = Nbt.i(estado, "perdedor"), bebida = Nbt.i(estado, "bebida");
        float t = bebe ? tb(pt) : 0;
        boolean veneno = Nbt.b(estado, "veneno");
        // los cálices: los que quedan, parados; los que ya se bebieron, tirados de lado
        for (int l = 0; l < 2; l++) {
            int c = copas(l);
            for (int k = 0; k < Te.CALICES; k++) {
                Vec3 p = caliz(l, k);
                if (p == null) {
                    continue;
                }
                boolean queda = (c & (1 << k)) != 0;
                boolean enMano = bebe && l == perdedor && k == bebida && t < (veneno ? Te.T_TRAGA + 4 : 75);
                if (enMano) {
                    continue;
                }
                double[] s = silla(l);
                float yaw = (float) s[3];
                ps.pushPose();
                ps.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
                ps.mulPose(Axis.YP.rotationDegrees(180F - yaw + k * 23F));
                if (queda) {
                    float sube = l == miLado() && senalado == k && "CALIZ".equals(fase()) ? 0.06F : 0F;
                    ps.translate(0, sube, 0);
                    ps.scale(0.62F, 0.62F, 0.62F);
                    ps.translate(-0.5, 0, -0.5);
                } else {
                    // tirado: acostado sobre el mantel
                    ps.translate(0, 0.12, 0);
                    ps.mulPose(Axis.ZP.rotationDegrees(90));
                    ps.scale(0.62F, 0.62F, 0.62F);
                    ps.translate(-0.5, -0.3, -0.5);
                }
                mc.getBlockRenderer().renderSingleBlock(copa, ps, buf, luz(mc, p), OverlayTexture.NO_OVERLAY);
                ps.popPose();
            }
        }
        // la muñeca del Coleccionista, sentada en la silla del este (si alguien juega contra ella)
        if (Nbt.b(estado, "muneca") && muneca != null) {
            dibujarMuneca(mc, ps, buf, cam, bebe && perdedor == 1 ? t : -1, veneno, Nbt.b(estado, "munecaRota"));
        }
        // el que bebe
        if (bebe && perdedor >= 0 && !(perdedor == 1 && Nbt.b(estado, "muneca"))) {
            UUID id = estado.hasUUID("id" + perdedor) ? estado.getUUID("id" + perdedor) : null;
            if (id != null && t < (veneno ? Te.T_MUERE : Te.T_SALVO)) {
                dibujarBebedor(mc, ps, buf, cam, id, perdedor, bebida, t, veneno);
            }
        }
        buf.endBatch();
    }

    private static float suave(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    private static PlayerInfo info(UUID id) {
        var c = Minecraft.getInstance().getConnection();
        return id == null || c == null ? null : c.getPlayerInfo(id);
    }

    /** El cuerpo de quien bebe: sentado en su silla, con el brazo derecho alzando el cáliz y luego mareado o ahogándose. */
    private static void dibujarBebedor(Minecraft mc, PoseStack ps, MultiBufferSource buf, Vec3 cam, UUID id, int l, int k, float t,
            boolean veneno) {
        if (ancho == null) {
            ancho = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            delgado = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }
        double[] s = silla(l);
        if (s == null) {
            return;
        }
        PlayerInfo i = info(id);
        boolean slim = i != null ? "slim".equals(i.getModelName()) : "slim".equals(DefaultPlayerSkin.getSkinModelName(id));
        ResourceLocation piel = i != null ? i.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id);
        PlayerModel<AbstractClientPlayer> m = slim ? delgado : ancho;
        m.setAllVisible(true);
        m.young = false;
        m.crouching = false;
        m.riding = false;
        // la pose, por partes del tiempo
        float lado = (k - (Te.CALICES - 1) / 2F) * 0.22F;
        float hx = 0F, hy = 0F, hz = 0F, ax, ay = 0F, bx = 0F, by = 0F, cuerpo = 0F, inclina = 0F, verde = 0F, tilt = 0F;
        boolean conCaliz = true;
        if (t < 10) {
            float a = suave(t / 10F);
            ax = -1.15F * a;
            ay = lado * a;
            hx = 0.25F * a;
            inclina = 6F * a;
        } else if (t < 22) {
            float a = suave((t - 10) / 12F);
            ax = -1.15F + (-1.2F) * a;
            ay = lado + (-0.45F - lado) * a;
            hx = 0.25F - 0.8F * a;
            inclina = 6F - 6F * a;
            tilt = 25F * a;
        } else if (t < Te.T_TRAGA) {
            // bebiendo: la cabeza para atrás, el cáliz se inclina, tragos
            float trago = (float) Math.sin((t - 22) * 0.7F) * 0.06F;
            hx = -0.55F + trago;
            ax = -2.35F + trago;
            ay = -0.45F;
            tilt = 25F + 75F * suave((t - 22) / 20F);
        } else if (!veneno) {
            float a = suave((t - Te.T_TRAGA) / 18F);
            ax = -2.35F * (1F - a);
            ay = -0.45F * (1F - a);
            hx = -0.55F * (1F - a) + 0.15F * a;
            tilt = 100F * (1F - a);
            conCaliz = t < 75;
            // mareado: la cabeza y el cuerpo se mecen
            float mareo = Math.min(1F, (t - Te.T_TRAGA) / 20F);
            hy = 0.25F * mareo * (float) Math.sin(t * 0.22F);
            hz = 0.3F * mareo * (float) Math.sin(t * 0.17F + 1F);
            cuerpo = 5F * mareo * (float) Math.sin(t * 0.15F);
        } else {
            // el veneno: suelta el cáliz, las manos al cuello, convulsiones, verde
            float a = suave((t - Te.T_TRAGA) / 8F);
            conCaliz = t < Te.T_TRAGA + 4;
            ax = -2.35F + (0.3F) * a;
            ay = -0.45F - 0.1F * a;
            bx = -1.95F * a;
            by = 0.6F * a;
            float conv = Math.min(1F, (t - Te.T_TRAGA) / 30F);
            float sac = (float) (Math.sin(t * 2.7F) * Math.cos(t * 1.9F));
            hx = -0.55F - 0.15F * a + 0.18F * conv * sac;
            hy = 0.25F * conv * (float) Math.sin(t * 1.3F);
            cuerpo = 6F * conv * sac;
            inclina = -4F * conv + 3F * sac * conv;
            verde = suave((t - Te.T_TRAGA) / 45F);
        }
        m.head.setRotation(hx, hy, hz);
        m.body.setRotation(0, 0, 0);
        m.rightArm.setRotation(ax, ay, 0.05F);
        m.leftArm.setRotation(bx == 0F ? -0.25F : bx, by, -0.08F);
        m.rightLeg.setRotation(-1.4137167F, 0.31415927F, 0.07853982F);
        m.leftLeg.setRotation(-1.4137167F, -0.31415927F, -0.07853982F);
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
        Vec3 pos = new Vec3(s[0], s[1], s[2]);
        int luz = luz(mc, pos.add(0, 1, 0));
        ps.pushPose();
        ps.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        ps.mulPose(Axis.YP.rotationDegrees(180F - (float) s[3]));
        ps.mulPose(Axis.ZP.rotationDegrees(cuerpo));
        ps.mulPose(Axis.XP.rotationDegrees(inclina));
        ps.translate(0, 0.07, 0);
        ps.scale(-1, -1, 1);
        ps.scale(0.9375F, 0.9375F, 0.9375F);
        ps.translate(0, -1.501, 0);
        float r = 1F + (CadaverRenderer.VERDE_R - 1F) * verde, g = 1F + (CadaverRenderer.VERDE_G - 1F) * verde,
                b = 1F + (CadaverRenderer.VERDE_B - 1F) * verde;
        m.renderToBuffer(ps, buf.getBuffer(RenderType.entityCutoutNoCull(piel)), luz, OverlayTexture.NO_OVERLAY, r, g, b, 1F);
        Entity ent = null;
        for (Entity x : mc.level.entitiesForRendering()) {
            if (x.getUUID().equals(id)) {
                ent = x;
                break;
            }
        }
        com.axeliscore.plat.client.Draw d = new com.axeliscore.plat.client.Draw(buf);
        com.axeliscore.vestir.client.VestuarioCliente.dibujarJugador(id, ent != null ? ent : mc.player, slim, new ModelPart[] {m.head, m.body,
                m.rightArm, m.leftArm, m.rightLeg, m.leftLeg}, ps, d, luz, OverlayTexture.NO_OVERLAY, 1F, false);
        d.flush();
        // el cáliz en la mano derecha: en la punta del brazo, parado (y se va inclinando hacia la boca al beber)
        if (conCaliz && copa != null) {
            ModelPart brazo = m.rightArm;
            Vector3f mano = new Quaternionf().rotationZYX(brazo.zRot, brazo.yRot, brazo.xRot).transform(new Vector3f(-1F, 10.5F, 0F))
                    .add(brazo.x, brazo.y, brazo.z);
            ps.pushPose();
            ps.translate(mano.x / 16F, mano.y / 16F, mano.z / 16F);
            ps.scale(-1, -1, 1);
            float caida = veneno && t > Te.T_TRAGA ? (t - Te.T_TRAGA) / 4F : 0F;
            ps.mulPose(Axis.XP.rotationDegrees(tilt + caida * 70F));
            ps.translate(0, -caida * 0.4F, 0);
            ps.scale(0.62F, 0.62F, 0.62F);
            ps.translate(-0.5, -0.25, -0.5);
            mc.getBlockRenderer().renderSingleBlock(copa, ps, buf, luz, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ps.popPose();
    }

    /**
     * La muñeca del Coleccionista (la de las repisas, pero grande) sentada en la silla del este. Cuando bebe se echa para atrás; si le tocó
     * el veneno, tiembla y se rompe: queda tirada de lado.
     */
    private static void dibujarMuneca(Minecraft mc, PoseStack ps, MultiBufferSource buf, Vec3 cam, float t, boolean veneno, boolean rota) {
        double[] s = silla(1);
        if (s == null) {
            return;
        }
        Vec3 pos = new Vec3(s[0], s[1] + 0.45, s[2]);
        int luz = luz(mc, pos.add(0, 1, 0));
        float atras = 0F, tiembla = 0F, cae = 0F;
        if (t >= 0) {
            if (t < Te.T_TRAGA) {
                atras = 25F * suave((t - 10) / 15F);
            } else if (!veneno) {
                atras = 25F * (1F - suave((t - Te.T_TRAGA) / 15F));
            } else {
                atras = 25F;
                tiembla = (float) Math.sin(t * 2.5F) * 8F * Math.min(1F, (t - Te.T_TRAGA) / 20F);
                cae = 85F * suave((t - Te.T_MUERE + 10) / 12F);
            }
        }
        if (rota) {
            cae = 85F;
            tiembla = 0F;
        }
        ps.pushPose();
        ps.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        ps.mulPose(Axis.YP.rotationDegrees(180F - (float) s[3]));
        ps.mulPose(Axis.ZP.rotationDegrees(tiembla + cae));
        ps.mulPose(Axis.XP.rotationDegrees(atras));
        ps.scale(2.6F, 2.6F, 2.6F);
        ps.translate(-0.5, 0, -0.5);
        mc.getBlockRenderer().renderSingleBlock(muneca, ps, buf, luz, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }
}
