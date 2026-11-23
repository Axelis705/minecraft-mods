package com.axeliscore.vestir.client;

import com.axeliscore.AxelisCore;
import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.anim.client.EstudioArchivos;
import com.axeliscore.anim.client.GeoRenderer;
import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.client.Draw;
import com.axeliscore.plat.client.VB;
import com.axeliscore.vestir.Vestuario;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * (Cliente) El lado del cliente del {@link Vestuario}: las prendas registradas por cada mod, quién lleva qué (lo manda el servidor) y el
 * dibujo encima de los jugadores (con la capa {@code CapaVestuario}), de la mano en primera persona y de los personajes del Core.
 */
public final class VestuarioCliente {
    private static final Map<ResourceLocation, Prenda> PRENDAS = new HashMap<>();
    private static final Map<UUID, List<Puesta>> PUESTAS = new HashMap<>();
    private static final Map<Geo, Pose> POSES = new IdentityHashMap<>();
    private static final Map<Geo, GeoRenderer.Frame> FRAMES = new IdentityHashMap<>();
    /** Puntos de giro de cada parte en el modelo de jugador (píxeles; y hacia abajo): normal y brazos delgados. */
    private static final float[][] PIVOTE = {{0, 0, 0}, {0, 0, 0}, {-5, 2, 0}, {5, 2, 0}, {-1.9F, 12, 0}, {1.9F, 12, 0}};
    private static final float[][] PIVOTE_DELGADO = {{0, 0, 0}, {0, 0, 0}, {-5, 2.5F, 0}, {5, 2.5F, 0}, {-1.9F, 12, 0}, {1.9F, 12, 0}};
    /** Huesos del modelo de persona del Core para cada parte. */
    private static final String[] HUESO_PERSONA = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"};

    /** Una prenda puesta: cuál y con qué datos. */
    public record Puesta(ResourceLocation id, CompoundTag datos) {
    }

    private VestuarioCliente() {
    }

    /** (Cliente, al iniciar) Cómo se ve la prenda con ese nombre. */
    public static void registrar(ResourceLocation id, Prenda p) {
        PRENDAS.put(id, p);
    }

    public static Prenda prenda(ResourceLocation id) {
        return PRENDAS.get(id);
    }

    public static void init() {
        ClientData.listen(Vestuario.KEY, VestuarioCliente::leer);
        //#if MC >= 26.1
        //$$ net.minecraftforge.client.event.RenderArmEvent.BUS.addListener(VestuarioCliente::brazo);
        //#else
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(VestuarioCliente::brazo);
        //#endif
    }

    private static void leer(CompoundTag t) {
        PUESTAS.clear();
        if (t == null) {
            return;
        }
        for (String k : t.getAllKeys()) {
            UUID id;
            try {
                id = UUID.fromString(k);
            } catch (IllegalArgumentException e) {
                continue;
            }
            CompoundTag suyas = com.axeliscore.plat.Nbt.c(t, k);
            List<Puesta> l = new ArrayList<>();
            for (String p : suyas.getAllKeys()) {
                l.add(new Puesta(Ids.parse(p), com.axeliscore.plat.Nbt.c(suyas, p)));
            }
            if (!l.isEmpty()) {
                PUESTAS.put(id, l);
            }
        }
    }

    /** Lo que lleva puesto alguien (vacío si nada, o si cambió de mundo). */
    public static List<Puesta> de(UUID id) {
        if (id == null || !ClientData.has(Vestuario.KEY)) {
            return List.of();
        }
        return PUESTAS.getOrDefault(id, List.of());
    }

    // ------------------------------------------------------------------ de dónde sale cada parte

    /** Matriz de una parte del modelo de jugador (como ModelPart.translateAndRotate), llevada al origen del modelo. */
    private static void desdeParte(PoseStack ps, ModelPart mp, float[] pivote, Matrix4f m, Matrix3f n) {
        Quaternionf q = new Quaternionf().rotationZYX(mp.zRot, mp.yRot, mp.xRot);
        m.set(ps.last().pose()).translate(mp.x / 16F, mp.y / 16F, mp.z / 16F).rotate(q).scale(mp.xScale, mp.yScale, mp.zScale)
                .translate(-pivote[0] / 16F, -pivote[1] / 16F, -pivote[2] / 16F);
        n.set(ps.last().normal()).rotate(q);
    }

    /**
     * Dibuja lo que lleva un jugador. {@code partes}: cabeza, cuerpo, brazo derecho, brazo izquierdo, pierna derecha, pierna izquierda
     * del modelo que lo está dibujando (null = esa parte no). {@code ps} en el espacio del modelo (como dentro de una capa).
     */
    public static void dibujarJugador(UUID id, Entity e, boolean delgado, ModelPart[] partes, PoseStack ps, Draw d, int luz, int overlay,
            float alfa, boolean primeraPersona) {
        List<Puesta> l = de(id);
        if (l.isEmpty()) {
            return;
        }
        Matrix4f[] o = new Matrix4f[6];
        Matrix3f[] on = new Matrix3f[6];
        float[][] piv = delgado ? PIVOTE_DELGADO : PIVOTE;
        for (int i = 0; i < 6; i++) {
            if (partes[i] != null && partes[i].visible) {
                o[i] = new Matrix4f();
                on[i] = new Matrix3f();
                desdeParte(ps, partes[i], piv[i], o[i], on[i]);
            }
        }
        dibujar(l, id, e, delgado, o, on, ps, d, luz, overlay, alfa, primeraPersona);
    }

    /** Dibuja lo que lleva un personaje del Core hecho con el modelo de persona (huesos head, body, right_arm...). */
    public static void dibujarEnGeo(UUID id, Entity e, Geo geo, GeoRenderer.Frame f, PoseStack ps, Draw d, int luz, int overlay) {
        List<Puesta> l = de(id);
        if (l.isEmpty() || geo == null) {
            return;
        }
        Matrix4f[] o = new Matrix4f[6];
        Matrix3f[] on = new Matrix3f[6];
        for (int i = 0; i < 6; i++) {
            int b = geo.bone(HUESO_PERSONA[i]);
            if (b < 0) {
                continue;
            }
            Geo.Bone bone = geo.bones[b];
            o[i] = new Matrix4f(f.mundo(b)).translate(-bone.px / 16F, -bone.py / 16F, -bone.pz / 16F);
            on[i] = new Matrix3f(f.normal(b));
        }
        dibujar(l, id, e, false, o, on, ps, d, luz, overlay, 1F, false);
    }

    private static void dibujar(List<Puesta> l, UUID id, Entity e, boolean delgado, Matrix4f[] o, Matrix3f[] on, PoseStack ps, Draw d, int luz,
            int overlay, float alfa, boolean primeraPersona) {
        for (Puesta pu : l) {
            Prenda p = PRENDAS.get(pu.id());
            if (p == null || primeraPersona && !p.primeraPersona) {
                continue;
            }
            Prenda.Contexto c = new Prenda.Contexto(id, e, pu.datos(), delgado, primeraPersona);
            try {
                una(p, c, o, on, ps, d, luz, overlay, alfa);
            } catch (RuntimeException ex) {
                // una prenda mal hecha (modelo o textura) no debe tirar el juego
                if (System.currentTimeMillis() - ultimoError > 10000) {
                    ultimoError = System.currentTimeMillis();
                    AxelisCore.LOGGER.warn("AXCORE_VESTUARIO no se pudo dibujar {}: {}", pu.id(), ex.toString());
                }
            }
        }
    }

    private static long ultimoError;

    private static void una(Prenda p, Prenda.Contexto c, Matrix4f[] o, Matrix3f[] on, PoseStack ps, Draw d, int luz, int overlay, float alfa) {
        Geo geo = p.geoPara(c.delgado());
        if (geo == null || geo.bones.length == 0) {
            return;
        }
        Pose pose = POSES.computeIfAbsent(geo, g -> new Pose(g.bones.length));
        GeoRenderer.Frame f = FRAMES.computeIfAbsent(geo, GeoRenderer.Frame::new);
        if (POSES.size() > 64) {
            POSES.clear();
            FRAMES.clear();
        }
        pose.reset();
        Matrix4f[] base = new Matrix4f[geo.roots.length];
        Matrix3f[] baseN = new Matrix3f[geo.roots.length];
        boolean alguna = false;
        for (int i = 0; i < geo.roots.length; i++) {
            Prenda.Parte parte = Prenda.parteDe(geo.bones[geo.roots[i]].name);
            // un grupo que no es parte del cuerpo va con el cuerpo
            int k = (parte == null ? Prenda.Parte.CUERPO : parte).ordinal();
            if (o[k] != null && (parte == null || p.partes.contains(parte))) {
                base[i] = o[k];
                baseN[i] = on[k];
                alguna = true;
            }
        }
        if (!alguna) {
            return;
        }
        GeoRenderer.computeRaices(geo, pose, base, baseN, f);
        int tinte = p.tintePara(c);
        if (alfa < 1F) {
            int a = (int) (((tinte >>> 24) & 255) * alfa);
            tinte = (a << 24) | (tinte & 0xFFFFFF);
        }
        ResourceLocation tex = EstudioArchivos.textura(p.texturaPara(c));
        boolean trans = p.translucida || ((tinte >>> 24) & 255) < 255;
        VB vb = trans ? d.translucent(tex) : d.cutout(tex);
        GeoRenderer.draw(geo, pose, f, vb, GeoRenderer.Pass.NORMAL, luz, overlay, tinte);
        for (Geo.Bone b : geo.bones) {
            if (b.glow && !b.cubes.isEmpty()) {
                GeoRenderer.draw(geo, pose, f, d.glow(tex), GeoRenderer.Pass.GLOW, 0xF000F0, overlay, tinte);
                break;
            }
        }
        if (p.decorador != null) {
            p.decorador.dibujar(c, new Lienzo(o, on, ps, d, luz, p.inflar));
        }
    }

    /** ¿Tiene brazos delgados (skin tipo "Alex")? */
    //#if MC >= 26.1
    //$$ public static boolean esDelgado(net.minecraft.world.entity.player.PlayerSkin skin) {
    //$$     return skin != null && skin.model() == net.minecraft.world.entity.player.PlayerModelType.SLIM;
    //$$ }
    //#elif MC >= 1.20.2
    //$$ public static boolean esDelgado(net.minecraft.client.player.AbstractClientPlayer p) {
    //$$     return p.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM;
    //$$ }
    //#else
    public static boolean esDelgado(net.minecraft.client.player.AbstractClientPlayer p) {
        return "slim".equals(p.getModelName());
    }
    //#endif

    // ------------------------------------------------------------------ la mano en primera persona

    //#if MC >= 26.1
    //$$ /** La manga sobre la mano de primera persona (Minecraft avisa justo antes de dibujar el brazo, ya acomodado en la pantalla). */
    //$$ private static void brazo(net.minecraftforge.client.event.RenderArmEvent e) {
    //$$     Minecraft mc = Minecraft.getInstance();
    //$$     net.minecraft.client.player.LocalPlayer pl = mc.player;
    //$$     if (pl == null || de(pl.getUUID()).isEmpty()) {
    //$$         return;
    //$$     }
    //$$     if (!(mc.getEntityRenderDispatcher().getRenderer(pl) instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> ar)) {
    //$$         return;
    //$$     }
    //$$     net.minecraft.client.model.player.PlayerModel m = (net.minecraft.client.model.player.PlayerModel) ar.getModel();
    //$$     boolean der = e.getArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
    //$$     ModelPart parte = der ? m.rightArm : m.leftArm;
    //$$     // lo mismo que hace Minecraft antes de dibujar la mano
    //$$     parte.resetPose();
    //$$     parte.zRot = der ? 0.1F : -0.1F;
    //$$     ModelPart[] partes = new ModelPart[6];
    //$$     partes[der ? 2 : 3] = parte;
    //$$     Draw d = new Draw(e.getNodeCollector());
    //$$     dibujarJugador(pl.getUUID(), pl, esDelgado(pl.getSkin()), partes, e.getPoseStack(), d, e.getPackedLight(),
    //$$             net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1F, true);
    //$$     d.flush();
    //$$ }
    //#else
    /** La manga sobre la mano de primera persona (Minecraft avisa justo antes de dibujar el brazo, ya acomodado en la pantalla). */
    private static void brazo(net.minecraftforge.client.event.RenderArmEvent e) {
        net.minecraft.client.player.AbstractClientPlayer pl = e.getPlayer();
        if (de(pl.getUUID()).isEmpty()) {
            return;
        }
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(pl) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr)) {
            return;
        }
        net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer> m = pr.getModel();
        // lo mismo que hace Minecraft antes de dibujar la mano (lo vuelve a hacer después, da igual)
        m.attackTime = 0F;
        m.crouching = false;
        m.swimAmount = 0F;
        m.setupAnim(pl, 0F, 0F, 0F, 0F, 0F);
        boolean der = e.getArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        ModelPart parte = der ? m.rightArm : m.leftArm;
        parte.xRot = 0F;
        ModelPart[] partes = new ModelPart[6];
        partes[der ? 2 : 3] = parte;
        Draw d = new Draw(e.getMultiBufferSource());
        dibujarJugador(pl.getUUID(), pl, esDelgado(pl), partes, e.getPoseStack(), d, e.getPackedLight(),
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1F, true);
        d.flush();
    }
    //#endif
}
