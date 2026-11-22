package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.arma.ArmaItem;
import com.mundosdeaxelis.arma.TipoArma;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Etapa 283: el golpe con todo el cuerpo, visto desde fuera (tercera persona y otros jugadores).
 *
 * <ul>
 * <li>Cada tipo de arma tiene su golpe: el tronco gira, los dos brazos se mueven, las piernas dan un paso (la de delante adelante,
 * la de atrás atrás) y todo el cuerpo se inclina, baja o avanza un poco (eso último se hace girando el dibujo entero desde los
 * pies).</li>
 * <li>El golpe tiene su propio reloj (el de Minecraft dura siempre 0,3 s): el martillo tarda más que las dagas, pero el impacto
 * llega pronto, para que coincida con el daño.</li>
 * <li>Las dagas se ven las dos (una en cada mano, también en primera persona) y apuñalan alternando.</li>
 * <li>En primera persona la cámara acompaña un poco (se puede quitar en Opciones).</li>
 * </ul>
 * El golpe vanilla del brazo se apaga poniendo {@code attackTime} a 0 dentro de la pose (Minecraft lo aplica después).
 */
public final class ArmaCuerpo {
    private ArmaCuerpo() {
    }

    // ------------------------------------------------------------------ reloj del golpe

    private static final class Reloj {
        boolean lastSwinging;
        int lastSwingTime;
        float start = -1000.0F;
        int swings;
    }

    private static final Map<LivingEntity, Reloj> RELOJ = new WeakHashMap<>();

    /** Lo que dura el golpe entero en el cuerpo, en segundos (siempre menos que la recarga del arma). */
    public static float duracion(TipoArma t) {
        return switch (t) {
            case ESPADA -> 0.45F;
            case HACHA -> 0.62F;
            case MARTILLO -> 0.72F;
            case LANZA -> 0.50F;
            case DAGAS -> 0.32F;
        };
    }

    private static Reloj reloj(LivingEntity e) {
        Reloj r = RELOJ.computeIfAbsent(e, k -> new Reloj());
        // (swingingArm es null hasta el primer golpe)
        if (e.swinging && e.swingingArm != InteractionHand.OFF_HAND && (!r.lastSwinging || e.swingTime < r.lastSwingTime)) {
            r.start = e.tickCount - Math.max(0, e.swingTime);
            r.swings++;
        }
        r.lastSwinging = e.swinging;
        r.lastSwingTime = e.swingTime;
        return r;
    }

    /** Punto del golpe (0..1), o -1 si no está golpeando con el arma. */
    public static float progreso(LivingEntity e, TipoArma tipo, float pt) {
        if (ArmaAnim.fijo >= 0.0F) {
            return ArmaAnim.fijo;
        }
        Reloj r = reloj(e);
        float el = (e.tickCount + pt - r.start) / 20.0F / duracion(tipo);
        return el >= 0.0F && el < 1.0F ? el : -1.0F;
    }

    /** Con dagas: ¿este golpe lo da la mano del arma (true) o la otra (false)? Se van turnando. */
    public static boolean daLaPrincipal(LivingEntity e) {
        if (ArmaAnim.fijo >= 0.0F) {
            return ArmaAnim.fijoPrincipal;
        }
        return reloj(e).swings % 2 == 1;
    }

    // ------------------------------------------------------------------ poses del cuerpo

    /*
     * Fotogramas (etapa 283, ajustados con herramientas/pose283.py): {t, peso, giro, brazo del arma: dirección (3), hacia dónde
     * apunta el arma (3), el otro brazo: dirección (3), su arma (3), pierna delante, pierna detrás, inclinación (grados), bajada
     * (bloques), avance (bloques)}. Las direcciones van en (derecha, arriba, delante) para quien golpea con la derecha (con la
     * izquierda se refleja). El otro brazo en NaN = agarra el mango del arma a dos manos. El giro positivo echa atrás el hombro
     * del arma. Peso 0 = la pose normal.
     */
    /** Espada: tajo en diagonal, de arriba a la derecha hacia abajo a la izquierda; el otro brazo equilibra. */
    private static final float[][] ESPADA = {
            {0.00F, 0, 0.55F, 0.35F, 0.85F, 0.10F, 0.15F, 0.20F, -1.00F, -0.30F, -0.40F, 0.86F, 0.00F, 0.00F, 1.00F, -0.25F, 0.20F, -4, 0.00F, 0.00F},
            {0.20F, 1, 0.55F, 0.35F, 0.85F, 0.10F, 0.15F, 0.20F, -1.00F, -0.30F, -0.40F, 0.86F, 0.00F, 0.00F, 1.00F, -0.25F, 0.20F, -4, 0.00F, 0.00F},
            {0.38F, 1, -0.50F, -0.35F, -0.15F, 0.92F, -0.90F, -0.30F, -0.30F, -0.20F, -0.90F, -0.30F, 0.00F, 0.00F, 1.00F, -0.45F, 0.35F, 10, 0.02F, 0.12F},
            {0.62F, 1, -0.62F, -0.60F, -0.35F, 0.72F, -0.55F, -0.40F, -0.73F, -0.20F, -0.90F, -0.30F, 0.00F, 0.00F, 1.00F, -0.40F, 0.30F, 8, 0.02F, 0.10F},
            {1.00F, 0, -0.62F, -0.60F, -0.35F, 0.72F, -0.55F, -0.40F, -0.73F, -0.20F, -0.90F, -0.30F, 0.00F, 0.00F, 1.00F, -0.40F, 0.30F, 8, 0.02F, 0.10F}};
    /** Hacha de guerra: a dos manos por encima del hombro y abajo con todo el cuerpo. */
    private static final float[][] HACHA = {
            {0.00F, 0, 0.40F, 0.30F, 0.90F, -0.30F, 0.00F, -0.30F, -0.95F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.30F, 0.25F, -8, 0.00F, 0.00F},
            {0.22F, 1, 0.40F, 0.30F, 0.90F, -0.30F, 0.00F, -0.30F, -0.95F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.30F, 0.25F, -8, 0.00F, 0.00F},
            {0.40F, 1, -0.35F, -0.10F, 0.10F, 0.99F, 0.00F, -0.99F, 0.10F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.55F, 0.45F, 20, 0.08F, 0.15F},
            {0.66F, 1, -0.42F, -0.20F, -0.25F, 0.95F, 0.00F, -0.95F, -0.25F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.50F, 0.40F, 16, 0.06F, 0.12F},
            {1.00F, 0, -0.42F, -0.20F, -0.25F, 0.95F, 0.00F, -0.95F, -0.25F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.50F, 0.40F, 16, 0.06F, 0.12F}};
    /** Martillo: los brazos arriba del todo y un mazazo al suelo, agachándose. */
    private static final float[][] MARTILLO = {
            {0.00F, 0, 0.18F, 0.05F, 0.98F, 0.15F, 0.00F, -0.15F, -0.99F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.35F, 0.30F, -12, 0.00F, -0.02F},
            {0.26F, 1, 0.18F, 0.05F, 0.98F, 0.15F, 0.00F, -0.15F, -0.99F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.35F, 0.30F, -12, 0.00F, -0.02F},
            {0.44F, 1, -0.08F, 0.00F, 0.05F, 1.00F, 0.00F, -1.00F, 0.05F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.70F, 0.55F, 26, 0.16F, 0.10F},
            {0.70F, 1, -0.10F, 0.00F, -0.15F, 0.99F, 0.00F, -0.99F, -0.15F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.62F, 0.50F, 22, 0.12F, 0.08F},
            {1.00F, 0, -0.10F, 0.00F, -0.15F, 0.99F, 0.00F, -0.99F, -0.15F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.62F, 0.50F, 22, 0.12F, 0.08F}};
    /** Lanza: el asta a la altura de la cadera, el cuerpo se carga atrás y embiste con un paso largo. */
    private static final float[][] LANZA = {
            {0.00F, 0, 0.45F, 0.15F, -0.95F, 0.20F, 0.00F, 0.20F, 0.95F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.20F, 0.25F, -5, 0.00F, -0.05F},
            {0.18F, 1, 0.45F, 0.15F, -0.95F, 0.20F, 0.00F, 0.20F, 0.95F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.20F, 0.25F, -5, 0.00F, -0.05F},
            {0.34F, 1, -0.30F, 0.00F, -0.75F, 0.66F, 0.00F, 0.66F, 0.75F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.75F, 0.55F, 16, 0.05F, 0.28F},
            {0.60F, 1, -0.25F, 0.00F, -0.80F, 0.60F, 0.00F, 0.60F, 0.80F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.65F, 0.50F, 13, 0.04F, 0.22F},
            {1.00F, 0, -0.25F, 0.00F, -0.80F, 0.60F, 0.00F, 0.60F, 0.80F, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, -0.65F, 0.50F, 13, 0.04F, 0.22F}};
    /** Dagas: el «brazo» es el que apuñala en este golpe (se turnan) y el «otro» se queda en guardia. */
    private static final float[][] DAGAS = {
            {0.00F, 0, 0.30F, 0.10F, 0.60F, 0.79F, 0.00F, -0.79F, 0.60F, -0.20F, -0.50F, 0.85F, 0.00F, 0.85F, 0.50F, -0.20F, 0.20F, 2, 0.00F, 0.00F},
            {0.15F, 1, 0.30F, 0.10F, 0.60F, 0.79F, 0.00F, -0.79F, 0.60F, -0.20F, -0.50F, 0.85F, 0.00F, 0.85F, 0.50F, -0.20F, 0.20F, 2, 0.00F, 0.00F},
            {0.32F, 1, -0.40F, 0.00F, -0.50F, 0.87F, 0.00F, -0.87F, -0.50F, -0.20F, -0.50F, 0.85F, 0.00F, 0.85F, 0.50F, -0.40F, 0.30F, 9, 0.02F, 0.12F},
            {0.58F, 1, -0.30F, 0.00F, -0.60F, 0.80F, 0.00F, -0.80F, -0.60F, -0.20F, -0.50F, 0.85F, 0.00F, 0.85F, 0.50F, -0.35F, 0.25F, 6, 0.02F, 0.08F},
            {1.00F, 0, -0.30F, 0.00F, -0.60F, 0.80F, 0.00F, -0.80F, -0.60F, -0.20F, -0.50F, 0.85F, 0.00F, 0.85F, 0.50F, -0.35F, 0.25F, 6, 0.02F, 0.08F}};

    static final int T = 0, W = 1, GIRO = 2, AM = 3, BM = 6, AO = 9, BO = 12, DELANTE = 15, DETRAS = 16, INCL = 17, BAJA = 18, AVANCE = 19;

    private static float[][] claves(TipoArma t) {
        return switch (t) {
            case ESPADA -> ESPADA;
            case HACHA -> HACHA;
            case MARTILLO -> MARTILLO;
            case LANZA -> LANZA;
            case DAGAS -> DAGAS;
        };
    }

    /** Tiempo (0..1) del fotograma {@code i} del golpe (1 = preparando, 2 = impacto). */
    public static float tiempoClave(TipoArma tipo, int i) {
        return claves(tipo)[i][T];
    }

    /** La pose del cuerpo en el punto {@code t} del golpe (20 valores, ver arriba; los NaN se quedan NaN). */
    public static float[] cuadro(TipoArma tipo, float t) {
        float[][] k = claves(tipo);
        t = Mth.clamp(t, 0.0F, 1.0F);
        int s = 0;
        while (s < k.length - 2 && t > k[s + 1][T]) {
            s++;
        }
        float[] a = k[s], b = k[s + 1];
        float u = Mth.clamp((t - a[T]) / Math.max(1.0E-4F, b[T] - a[T]), 0.0F, 1.0F);
        u = u * u * (3.0F - 2.0F * u);
        float[] out = new float[a.length];
        for (int j = 0; j < a.length; j++) {
            out[j] = Mth.lerp(u, a[j], b[j]);
        }
        return out;
    }

    /** ¿Se puede mover todo el cuerpo ahora? (no montado, nadando, planeando ni durmiendo) */
    static boolean libre(LivingEntity e) {
        return !e.isPassenger() && !e.isVisuallySwimming() && !e.isFallFlying() && !e.isSleeping() && e.getPose() != Pose.SPIN_ATTACK;
    }

    /** El arma que lleva en la mano principal (null si no es de las nuevas o está usando algo). */
    static ArmaItem arma(LivingEntity e) {
        return !e.isUsingItem() && e.getMainHandItem().getItem() instanceof ArmaItem a ? a : null;
    }

    /** Dagas con la otra mano libre: se ven las dos. */
    public static boolean dagasDobles(LivingEntity e) {
        ArmaItem a = arma(e);
        return a != null && a.tipo == TipoArma.DAGAS && e.getOffhandItem().isEmpty();
    }

    /** Cómo sale el arma de la mano (en el brazo, con +y hacia la mano): casi en ángulo recto, 18 grados hacia la mano. */
    private static final Vector3f HOJA = new Vector3f(0.0F, 0.309F, -0.951F);

    /** De (derecha, arriba, delante) a las coordenadas del modelo, reflejando si golpea con la izquierda ({@code s} = -1). */
    private static Vector3f modelo(float[] f, int i, float s) {
        return new Vector3f(-f[i] * s, -f[i + 1], -f[i + 2]).normalize();
    }

    /**
     * La pose de las armas en el modelo (la llama la ArmPose de cada tipo, antes de que Minecraft aplique el golpe). Primero el
     * agarre de siempre y, si está golpeando, el golpe de todo el cuerpo.
     */
    static void aplicar(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm, TipoArma tipo) {
        ModelPart main = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        ModelPart off = arm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
        float s = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        boolean manoLibre = entity.getOffhandItem().isEmpty();
        // 1) agarre
        switch (tipo) {
            case HACHA, MARTILLO, LANZA -> {
                float mira = Mth.clamp(model.head.xRot, -0.6F, 0.6F) * 0.3F;
                main.xRot = -0.70F + mira;
                main.yRot = s * -0.15F;
                main.zRot = 0.0F;
                if (manoLibre) {
                    off.xRot = -0.95F + mira;
                    off.yRot = s * 0.62F;
                    off.zRot = 0.0F;
                }
            }
            case DAGAS -> {
                main.xRot = main.xRot * 0.5F - (float) Math.PI / 10.0F;
                main.yRot = 0.0F;
                if (manoLibre) {
                    off.xRot = off.xRot * 0.5F - (float) Math.PI / 10.0F;
                    off.yRot = 0.0F;
                }
            }
            default -> {
                main.xRot = main.xRot * 0.5F - (float) Math.PI / 10.0F;
                main.yRot = 0.0F;
            }
        }
        if (!(entity instanceof Player) || entity.swingingArm == InteractionHand.OFF_HAND) {
            return;
        }
        // 2) el golpe: el de Minecraft (solo el brazo) se apaga y se hace el nuestro
        model.attackTime = 0.0F;
        float t = progreso(entity, tipo, Minecraft.getInstance().getFrameTime());
        if (t < 0.0F || !libre(entity)) {
            return;
        }
        float[] f = cuadro(tipo, t);
        float w = f[W];
        if (w <= 0.0F) {
            return;
        }
        ModelPart act = main, otro = off;
        float sa = s;
        if (tipo == TipoArma.DAGAS && manoLibre && !daLaPrincipal(entity)) {
            act = off;
            otro = main;
            sa = -s;
        }
        boolean otroLibre = manoLibre || otro == main;
        // tronco: gira y los hombros van con él (como en el golpe de Minecraft)
        float giro = sa * f[GIRO] * w;
        model.body.yRot = giro;
        model.rightArm.z = Mth.sin(giro) * 5.0F;
        model.rightArm.x = -Mth.cos(giro) * 5.0F;
        model.leftArm.z = -Mth.sin(giro) * 5.0F;
        model.leftArm.x = Mth.cos(giro) * 5.0F;
        // brazo que golpea: dirección del brazo y del arma, girados con el tronco
        Vector3f aM = modelo(f, AM, sa).rotateY(giro), bM = modelo(f, BM, sa).rotateY(giro);
        apuntar(act, aM, bM, w);
        if (otroLibre) {
            Vector3f aO, bO;
            if (Float.isNaN(f[AO])) {
                // a dos manos: la otra mano va al mango (más abajo en el hacha y el martillo, más adelante en la lanza)
                Vector3f mano = new Vector3f(act.x, act.y, act.z).add(new Vector3f(aM).mul(10.0F));
                Vector3f hoja = arma(aM, bM);
                Vector3f obj = mano.add(hoja.mul(tipo == TipoArma.LANZA ? 5.0F : -3.0F));
                aO = obj.sub(otro.x, otro.y, otro.z).normalize();
                bO = new Vector3f(bM);
            } else {
                aO = modelo(f, AO, sa).rotateY(giro);
                bO = modelo(f, BO, sa).rotateY(giro);
            }
            apuntar(otro, aO, bO, w);
        } else {
            otro.yRot += giro;
        }
        // piernas: un paso con la de delante (la del lado contrario al brazo que golpea) y la otra atrás
        ModelPart delante = sa > 0 ? model.leftLeg : model.rightLeg;
        ModelPart detras = sa > 0 ? model.rightLeg : model.leftLeg;
        if (entity.onGround() || entity.isInWater()) {
            delante.xRot = Mth.lerp(w, delante.xRot, f[DELANTE]);
            detras.xRot = Mth.lerp(w, detras.xRot, f[DETRAS]);
            model.rightLeg.zRot = Mth.lerp(w, model.rightLeg.zRot, 0.07F);
            model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.07F);
        }
    }

    /** Hacia dónde sale el arma de un brazo que apunta a {@code a} con el arma «de lado» hacia {@code b}. */
    private static Vector3f arma(Vector3f a, Vector3f b) {
        Vector3f bp = perp(a, b);
        return bp.mul(-HOJA.z).add(new Vector3f(a).mul(HOJA.y)).normalize();
    }

    /** {@code b} sin la parte que va en la dirección de {@code a} (perpendicular, normalizado). */
    private static Vector3f perp(Vector3f a, Vector3f b) {
        Vector3f bp = new Vector3f(b).sub(new Vector3f(a).mul(a.dot(b)));
        if (bp.lengthSquared() < 1.0E-6F) {
            bp.set(0.0F, 0.0F, -1.0F).sub(new Vector3f(a).mul(-a.z));
        }
        return bp.normalize();
    }

    /**
     * Pone el brazo para que apunte a {@code a} con el arma hacia {@code b}, mezclado con su pose normal según {@code w}. Las
     * cuentas se hacen con direcciones y se pasan a los ángulos de Minecraft (Z, Y, X) al final: así no hay saltos raros.
     */
    private static void apuntar(ModelPart p, Vector3f a, Vector3f b, float w) {
        Matrix3f r0 = new Matrix3f().rotationZYX(p.zRot, p.yRot, p.xRot);
        Vector3f a0 = r0.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f b0 = r0.transform(new Vector3f(0.0F, 0.0F, -1.0F));
        Vector3f ay = new Vector3f(a0).lerp(a, w).normalize();
        Vector3f bp = perp(ay, new Vector3f(b0).lerp(perp(a, b), w));
        // columnas: x = y × z, y = el brazo, z = el contrario de hacia donde sale el arma
        Vector3f cz = new Vector3f(bp).negate();
        Vector3f cx = new Vector3f(ay).cross(cz);
        Vector3f e = new Matrix3f(cx, ay, cz).getEulerAnglesZYX(new Vector3f());
        p.xRot = e.x;
        p.yRot = e.y;
        p.zRot = e.z;
    }

    // ------------------------------------------------------------------ inclinar todo el cuerpo (desde los pies)

    private static final Deque<Boolean> EMPUJADO = new ArrayDeque<>();

    @Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, value = Dist.CLIENT)
    public static final class Eventos {
        private Eventos() {
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onPre(RenderPlayerEvent.Pre e) {
            Player p = e.getEntity();
            if (!Float.isNaN(AutoTest.ladoFoto) && p == Minecraft.getInstance().player) {
                // solo para las fotos de la prueba: el cuerpo y la cabeza de lado respecto a la cámara
                float r = p.getYRot() + AutoTest.ladoFoto;
                p.yBodyRot = p.yBodyRotO = p.yHeadRot = p.yHeadRotO = r;
            }
            ArmaItem a = arma(p);
            float t = a == null || p.swingingArm == InteractionHand.OFF_HAND || !libre(p) ? -1.0F : progreso(p, a.tipo, e.getPartialTick());
            if (t < 0.0F) {
                EMPUJADO.push(false);
                return;
            }
            float[] f = cuadro(a.tipo, t);
            float w = f[W];
            float yaw = Mth.rotLerp(e.getPartialTick(), p.yBodyRotO, p.yBodyRot);
            PoseStack ps = e.getPoseStack();
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(-yaw));
            float incl = f[INCL] * w * (p.isCrouching() ? 0.5F : 1.0F);
            ps.translate(0.0F, -f[BAJA] * w, f[AVANCE] * w);
            ps.mulPose(Axis.XP.rotationDegrees(incl));
            ps.mulPose(Axis.YP.rotationDegrees(yaw));
            EMPUJADO.push(true);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onPost(RenderPlayerEvent.Post e) {
            Boolean b = EMPUJADO.poll();
            if (b != null && b) {
                e.getPoseStack().popPose();
            }
        }

        /** Primera persona: la cámara acompaña el golpe (un poco). */
        @SubscribeEvent
        public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !mc.options.getCameraType().isFirstPerson() || !com.mundosdeaxelis.ClientConfig.ATTACK_CAMERA.get()) {
                return;
            }
            ArmaItem a = arma(mc.player);
            if (a == null || mc.player.swingingArm == InteractionHand.OFF_HAND) {
                return;
            }
            float t = progreso(mc.player, a.tipo, (float) e.getPartialTick());
            if (t < 0.0F) {
                return;
            }
            float[] f = cuadro(a.tipo, t);
            float w = f[W];
            float s = mc.player.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            if (a.tipo == TipoArma.DAGAS && !daLaPrincipal(mc.player)) {
                s = -s;
            }
            e.setPitch(e.getPitch() + f[INCL] * 0.14F * w);
            e.setRoll(e.getRoll() - s * f[GIRO] * 3.0F * w);
        }

        /** Primera persona: la segunda daga en la mano izquierda (si está libre), y apuñala cuando le toca. */
        @SubscribeEvent
        public static void onHand(RenderHandEvent e) {
            if (e.getHand() != InteractionHand.OFF_HAND) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !dagasDobles(mc.player)) {
                return;
            }
            ItemStack st = mc.player.getMainHandItem();
            HumanoidArm brazo = mc.player.getMainArm().getOpposite();
            PoseStack ps = e.getPoseStack();
            ps.pushPose();
            ArmaAnim.manoDaga(ps, brazo, st, e.getPartialTick(), e.getEquipProgress());
            mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(mc.player, st,
                    brazo == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    brazo == HumanoidArm.LEFT, ps, e.getMultiBufferSource(), e.getPackedLight());
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ la segunda daga en tercera persona

    @Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Capas {
        private Capas() {
        }

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                if (event.getPlayerSkin(skin) instanceof PlayerRenderer pr) {
                    pr.addLayer(new DagaLayer(pr));
                }
            }
        }
    }

    /** Dibuja la daga de la otra mano (la misma del arma) cuando la mano izquierda está libre. */
    static final class DagaLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        DagaLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                           float pt, float age, float yaw, float pitch) {
            if (!dagasDobles(p)) {
                return;
            }
            ItemStack st = p.getMainHandItem();
            HumanoidArm brazo = p.getMainArm().getOpposite();
            boolean izq = brazo == HumanoidArm.LEFT;
            ps.pushPose();
            getParentModel().translateToHand(brazo, ps);
            ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
            ps.mulPose(Axis.YP.rotationDegrees(180.0F));
            ps.translate((izq ? -1 : 1) / 16.0F, 0.125F, -0.625F);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(p, st,
                    izq ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, izq, ps, buf, light);
            ps.popPose();
        }
    }
}
