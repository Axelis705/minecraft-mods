package com.axeliscore.anim.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import com.axeliscore.anim.Clip;
import com.axeliscore.anim.Geo;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.client.Draw;
import com.axeliscore.plat.client.VB;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
//#if MC >= 26.1
//$$ import net.minecraft.client.renderer.SubmitNodeCollector;
//$$ import net.minecraft.client.renderer.entity.state.EntityRenderState;
//$$ import net.minecraft.client.renderer.state.level.CameraRenderState;
//#else
import net.minecraft.client.renderer.MultiBufferSource;
//#endif

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Renderizador para entidades animadas con el Core ({@link Animated}): carga el modelo y las animaciones, avanza la animación cada
 * cuadro, gira el cuerpo, se inclina al morir (si el modelo no tiene animación "death"), se pone rojo al recibir daño, dibuja las
 * partes que brillan aparte y los objetos que lleva en las manos (huesos "right_item" y "left_item").
 * <p>
 * En 1.20/1.21 dibuja directo; en 26.x copia lo necesario en el "estado" y lo entrega después (como pide Minecraft).
 */
//#if MC >= 26.1
//$$ public abstract class GeoEntityRenderer<T extends Entity & Animated> extends EntityRenderer<T, GeoEntityRenderer.State> {
//$$     public static class State extends EntityRenderState {
//$$         public final Snap snap = new Snap();
//$$     }
//#else
public abstract class GeoEntityRenderer<T extends Entity & Animated> extends EntityRenderer<T> {
    private final Snap snap = new Snap();
//#endif

    /** Lo que hace falta para dibujar una entidad en un cuadro (en 26.x se guarda en el estado). */
    public static final class Snap {
        public AnimHost host;
        public float bodyYaw, deathTilt, scale = 1;
        public int overlay = OverlayTexture.NO_OVERLAY, tint = 0xFFFFFFFF, light;
        public ResourceLocation texture, glowTexture;
        public boolean invisible;
        public ItemStack mainHand = ItemStack.EMPTY, offHand = ItemStack.EMPTY;
        public boolean leftHanded;
    }

    protected final ResourceLocation geoId;
    protected final ResourceLocation[] animIds;
    private final Map<Geo, GeoRenderer.Frame> frames = new IdentityHashMap<>();

    /**
     * @param geo   el modelo (assets/mod/geo/nombre.geo.json)
     * @param anims las animaciones (assets/mod/animations/nombre.animation.json; si son varias, se juntan)
     */
    protected GeoEntityRenderer(EntityRendererProvider.Context ctx, ResourceLocation geo, float shadow, ResourceLocation... anims) {
        super(ctx);
        this.geoId = geo;
        this.animIds = anims;
        this.shadowRadius = shadow;
    }

    /** La textura de esta entidad. */
    protected abstract ResourceLocation texture(T e);

    /** El modelo que usa (para el editor de animaciones). */
    public ResourceLocation geoId() {
        return geoId;
    }

    /** Sus archivos de animaciones (para el editor). */
    public ResourceLocation[] animIds() {
        return animIds.clone();
    }

    /** El tamaño del modelo para esa entidad (para el editor). */
    @SuppressWarnings("unchecked")
    public float escalaPara(Entity e) {
        try {
            return modelScale((T) e);
        } catch (ClassCastException ex) {
            return 1F;
        }
    }

    /** Textura de las partes que brillan (null = la misma). */
    protected ResourceLocation glowTexture(T e) {
        return null;
    }

    /** Tamaño del modelo (1 = como está en Blockbench). */
    protected float modelScale(T e) {
        return 1F;
    }

    /** Color que multiplica la textura (ARGB; 0xFFFFFFFF = normal). */
    protected int tint(T e) {
        return 0xFFFFFFFF;
    }

    /** Recién conectado el modelo: aquí se crean las capas (y sus máscaras) en el orden en que se aplican. */
    protected void setupLayers(AnimHost h) {
        h.animator().layer("base");
    }

    // ------------------------------------------------------------------ cada cuadro

    /** Prepara la animación y llena {@code s} (lo común a todas las versiones). */
    protected void fill(T e, float pt, Snap s) {
        AnimHost h = e.axHost();
        if (frames.size() > 16) {
            frames.clear(); // modelos viejos (recargados desde el estudio)
        }
        if (h.bind(GeoAssets.geo(geoId), GeoAssets.clips(animIds))) {
            setupLayers(h);
            h.animator().listen((layer, clip, ev) -> onEvent(e, h, layer, clip, ev));
        }
        consultas(e, h, pt);
        boolean editor = EstudioEditor.controla(e, h);
        if (!editor) {
            e.axAnimate(h, pt);
        }
        h.advance((e.tickCount + pt) / 20F);
        if (!editor || EstudioEditor.conCodigo()) {
            e.axPose(h, h.pose(), pt);
        }
        s.host = h;
        s.scale = modelScale(e);
        s.tint = tint(e);
        s.texture = EstudioArchivos.textura(texture(e));
        s.glowTexture = EstudioArchivos.textura(glowTexture(e));
        s.invisible = e.isInvisible();
        s.deathTilt = 0;
        s.overlay = OverlayTexture.NO_OVERLAY;
        s.mainHand = ItemStack.EMPTY;
        s.offHand = ItemStack.EMPTY;
        if (e instanceof LivingEntity le) {
            s.bodyYaw = Mth.rotLerp(pt, le.yBodyRotO, le.yBodyRot);
            boolean hurt = le.hurtTime > 0 || le.deathTime > 0;
            s.overlay = OverlayTexture.pack(OverlayTexture.u(0F), OverlayTexture.v(hurt));
            if (le.deathTime > 0 && h.clip("death") == null) {
                float f = (le.deathTime + pt - 1F) / 20F * 1.6F;
                s.deathTilt = Math.min(1F, Mth.sqrt(Math.max(0F, f)));
            }
            s.mainHand = le.getMainHandItem().copy();
            s.offHand = le.getOffhandItem().copy();
            s.leftHanded = le instanceof net.minecraft.world.entity.Mob m && m.isLeftHanded();
        } else {
            s.bodyYaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        }
        if (editor) {
            s.bodyYaw = EstudioEditor.giro(s.bodyYaw);
        }
    }

    /**
     * Lo que las animaciones pueden leer como {@code query.X} (o {@code q.X}) en Blockbench: ground_speed (bloques/s),
     * vertical_speed, is_on_ground, is_in_water, is_sneaking, is_sprinting, health, max_health, hurt_time, yaw_speed (grados/s),
     * head_x_rotation y head_y_rotation (grados, relativos al cuerpo).
     */
    protected void consultas(T e, AnimHost h, float pt) {
        com.axeliscore.anim.Animator a = h.animator();
        double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
        a.query("ground_speed", Math.sqrt(dx * dx + dz * dz) * 20);
        a.query("vertical_speed", (e.getY() - e.yo) * 20);
        a.query("is_on_ground", e.onGround() ? 1 : 0);
        a.query("is_in_water", e.isInWater() ? 1 : 0);
        a.query("is_sneaking", e.isShiftKeyDown() ? 1 : 0);
        a.query("is_sprinting", e.isSprinting() ? 1 : 0);
        if (e instanceof LivingEntity le) {
            a.query("health", le.getHealth());
            a.query("max_health", le.getMaxHealth());
            a.query("hurt_time", le.hurtTime);
            a.query("yaw_speed", Mth.wrapDegrees(le.yBodyRot - le.yBodyRotO) * 20);
            a.query("head_y_rotation", Mth.wrapDegrees(Mth.rotLerp(pt, le.yHeadRotO, le.yHeadRot) - Mth.rotLerp(pt, le.yBodyRotO, le.yBodyRot)));
            a.query("head_x_rotation", Mth.lerp(pt, le.xRotO, le.getXRot()));
        }
    }

    /** Sonidos de la animación: se tocan aquí; lo demás va a la entidad. */
    protected void onEvent(T e, AnimHost h, String layer, Clip clip, Clip.Event ev) {
        if ("sound".equals(ev.kind) && !ev.value.isEmpty()) {
            try {
                SoundEvent snd = SoundEvent.createVariableRangeEvent(Ids.parse(ev.value));
                e.level().playLocalSound(e.getX(), e.getY(), e.getZ(), snd, e.getSoundSource(), 1F, 0.95F + (float) Math.random() * 0.1F,
                        false);
            } catch (RuntimeException ignored) {
                // un nombre de sonido mal escrito no debe tirar el juego
            }
            return;
        }
        if ("particle".equals(ev.kind) && !ev.value.isEmpty()) {
            try {
                com.axeliscore.fx.client.EfectosCliente.desdeAnimacion(e, ev.value, ev.locator);
            } catch (RuntimeException ignored) {
                // un efecto mal escrito no debe tirar el juego
            }
        }
        e.axEvent(h, layer, clip, ev);
    }

    /** Dibuja con lo guardado en {@code s} (lo común a todas las versiones). */
    protected void drawSnap(Snap s, PoseStack ps, Draw d) {
        AnimHost h = s.host;
        if (h == null || !h.ready()) {
            return;
        }
        Geo geo = h.geo();
        ps.pushPose();
        ps.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180F - s.bodyYaw)));
        if (s.deathTilt > 0) {
            ps.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(s.deathTilt * 90F)));
        }
        ps.scale(-1F, -1F, 1F);
        if (s.scale != 1F) {
            ps.scale(s.scale, s.scale, s.scale);
        }
        ps.translate(0F, -1.501F, 0F);
        GeoRenderer.Frame f = frames.computeIfAbsent(geo, GeoRenderer.Frame::new);
        GeoRenderer.compute(geo, h.pose(), ps, f);
        if (!s.invisible) {
            VB vb = d.cutout(s.texture);
            GeoRenderer.draw(geo, h.pose(), f, vb, GeoRenderer.Pass.NORMAL, s.light, s.overlay, s.tint);
            if (hasGlow(geo)) {
                VB g = d.glow(s.glowTexture != null ? s.glowTexture : s.texture);
                GeoRenderer.draw(geo, h.pose(), f, g, GeoRenderer.Pass.GLOW, 0xF000F0, s.overlay, s.tint);
            }
        }
        heldItem(geo, f, s.leftHanded ? s.offHand : s.mainHand, "right_item", ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ps, d, s.light);
        heldItem(geo, f, s.leftHanded ? s.mainHand : s.offHand, "left_item", ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ps, d, s.light);
        drawExtra(s, geo, f, ps, d);
        ps.popPose();
    }

    /** Para dibujar algo más pegado a un hueso (usar {@link #atBone}). */
    protected void drawExtra(Snap s, Geo geo, GeoRenderer.Frame f, PoseStack ps, Draw d) {
    }

    /** Pone la pila en el espacio de un hueso (dentro de un push/pop). */
    protected static void atBone(GeoRenderer.Frame f, int bone, PoseStack ps) {
        ps.last().pose().set(f.world[bone]);
        ps.last().normal().set(f.normal[bone]);
    }

    private static boolean hasGlow(Geo geo) {
        for (Geo.Bone b : geo.bones) {
            if (b.glow && !b.cubes.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Un objeto en la mano: el hueso localizador está donde Minecraft pondría el objeto (1 px hacia fuera, 10 px brazo abajo y 2 px
     * al frente del hombro) y se gira igual que en la capa de objetos en mano de los humanos.
     */
    private static void heldItem(Geo geo, GeoRenderer.Frame f, ItemStack st, String locator, ItemDisplayContext ctx, PoseStack ps, Draw d,
            int light) {
        if (st.isEmpty()) {
            return;
        }
        int b = geo.bone(locator);
        if (b < 0) {
            return;
        }
        ps.pushPose();
        atBone(f, b, ps);
        ps.mulPose(new Quaternionf().rotationX((float) Math.toRadians(-90)));
        ps.mulPose(new Quaternionf().rotationY((float) Math.PI));
        d.item(st, ctx, ps, light, Minecraft.getInstance().level, 0);
        ps.popPose();
    }

    //#if MC >= 26.1
    //$$ @Override
    //$$ public State createRenderState() {
    //$$     return new State();
    //$$ }
    //$$
    //$$ @Override
    //$$ public void extractRenderState(T e, State s, float pt) {
    //$$     super.extractRenderState(e, s, pt);
    //$$     fill(e, pt, s.snap);
    //$$     s.snap.light = s.lightCoords;
    //$$ }
    //$$
    //$$ @Override
    //$$ public void submit(State s, PoseStack ps, SubmitNodeCollector out, CameraRenderState cam) {
    //$$     Draw d = new Draw(out);
    //$$     drawSnap(s.snap, ps, d);
    //$$     d.flush();
    //$$     super.submit(s, ps, out, cam);
    //$$ }
    //#else
    @Override
    public void render(T e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        fill(e, pt, snap);
        snap.light = light;
        Draw d = new Draw(buf);
        drawSnap(snap, ps, d);
        d.flush();
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T e) {
        return texture(e);
    }
    //#endif
}
