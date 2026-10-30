package com.kensei.client;

import com.kensei.Kensei;
import com.kensei.KenseiConfig;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * La postura y los ataques de la katana vistos en tercera persona. Se hace con una postura de brazo propia (sin librerías externas): el
 * juego la aplica mientras llevas la katana y aquí se dibuja la guardia, cada tajo del combo, el crítico y el esquive. Qué movimiento es
 * lo dice {@link ClientAnims} (el servidor se lo avisa a todos los que ven al jugador).
 */
public final class KatanaPoses {
    private static HumanoidModel.ArmPose pose;
    private static boolean failed = false;
    private static boolean verified = false;

    /** Para entidades sin aviso del servidor (por ejemplo mobs con katana): el golpe se deduce contando los swings seguidos. */
    private static final class Anim {
        int step = 0;
        int startTick = Integer.MIN_VALUE;
    }

    private static final Map<LivingEntity, Anim> ANIMS = new WeakHashMap<>();

    private KatanaPoses() {
    }

    /**
     * Crea la postura propia. Tiene que llamarse al arrancar el mod, ANTES de que el juego dibuje cualquier modelo: el juego prepara una
     * tabla con las posturas que existen la primera vez que dibuja un brazo, y una postura creada después hace que falle (crasheo).
     */
    public static void init() {
        if (pose != null || failed) {
            return;
        }
        try {
            pose = HumanoidModel.ArmPose.create("KENSEI_KATANA", false, KatanaPoses::apply);
            Kensei.LOGGER.info("Kensei: postura de katana creada al arrancar.");
        } catch (Throwable t) {
            failed = true;
            Kensei.LOGGER.warn("Kensei: no se pudo crear la postura de katana, se usa la normal.", t);
        }
    }

    /**
     * La postura propia, o null si no se puede usar (entonces el juego usa la postura normal y no pasa nada). Nunca la crea aquí.
     * Antes de usarla la primera vez se comprueba que la tabla interna del juego ya la conozca: si no, usarla provocaría un crasheo.
     */
    public static HumanoidModel.ArmPose get() {
        if (pose == null || failed) {
            return null;
        }
        if (!verified) {
            verified = true;
            if (!tableKnowsPose()) {
                failed = true;
                Kensei.LOGGER.warn("Kensei: el juego ya había preparado su tabla de posturas sin la de la katana; se usa la postura normal.");
                return null;
            }
            Kensei.LOGGER.info("Kensei: postura de katana lista (numero {}).", pose.ordinal());
        }
        return pose;
    }

    /** Mira la tabla que el juego usa para elegir la postura de cada brazo y confirma que tiene sitio para la postura nueva. */
    private static boolean tableKnowsPose() {
        try {
            Class<?> switchMap = Class.forName("net.minecraft.client.model.HumanoidModel$1");
            int needed = HumanoidModel.ArmPose.values().length;
            for (java.lang.reflect.Field f : switchMap.getDeclaredFields()) {
                if (f.getType() == int[].class && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    int[] table = (int[]) f.get(null);
                    if (table.length < needed) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Throwable t) {
            return true; // no se pudo comprobar: se confía en que está bien
        }
    }

    private static float ease(float u) {
        u = Mth.clamp(u, 0.0F, 1.0F);
        return u * u * (3.0F - 2.0F * u);
    }

    /** Valores suavizados por entidad: cuánto está en guardia y cuánto corre (para que las posturas se mezclen y no den saltos). */
    private static final class Smooth {
        float guard;
        float sprint;
        float drop;
        long last;
    }

    private static final Map<LivingEntity, Smooth> SMOOTH = new WeakHashMap<>();
    private static final KatanaStance.Out OUT = new KatanaStance.Out();

    private static Smooth smooth(LivingEntity e, boolean guard, boolean sprint) {
        Smooth s = SMOOTH.computeIfAbsent(e, k -> new Smooth());
        long now = System.nanoTime();
        float dt = s.last == 0L ? 0.05F : Mth.clamp((now - s.last) / 1.0e9F, 0.0F, 0.1F);
        s.last = now;
        float tg = guard ? 1.0F : 0.0F;
        float ts = sprint && !guard ? 1.0F : 0.0F;
        s.guard += Mth.clamp(tg - s.guard, -dt * 9.0F, dt * 9.0F);
        s.sprint += Mth.clamp(ts - s.sprint, -dt * 5.0F, dt * 5.0F);
        return s;
    }

    /** Coloca el torso girado y los hombros donde toca (los brazos giran con el cuerpo, como hace el juego al golpear). */
    private static void placeBody(HumanoidModel<?> model, float yaw) {
        model.body.yRot = yaw;
        model.rightArm.z = Mth.sin(yaw) * 5.0F;
        model.rightArm.x = -Mth.cos(yaw) * 5.0F;
        model.leftArm.z = -Mth.sin(yaw) * 5.0F;
        model.leftArm.x = Mth.cos(yaw) * 5.0F;
    }

    /**
     * Postura de piernas de combate: pies separados, uno adelantado (el del lado del arma), y el cuerpo algo más bajo. Solo cuando está
     * quieto o casi (al caminar las piernas siguen su paso normal). Guarda cuánto baja el cuerpo para que {@link StanceEvents} lo aplique.
     */
    public static void stance(HumanoidModel<?> model, LivingEntity e, float m, float weight, float partial, float amount) {
        Smooth sm = SMOOTH.computeIfAbsent(e, k -> new Smooth());
        sm.drop = 0.0F;
        if (e.isPassenger() || e.isInWater() || e.isFallFlying() || e.isCrouching()) {
            return;
        }
        float spd = e.walkAnimation.speed(partial);
        float s = weight * amount * (1.0F - Mth.clamp(spd * 1.8F, 0.0F, 1.0F));
        if (s <= 0.001F) {
            return;
        }
        ModelPart lead = m > 0 ? model.rightLeg : model.leftLeg;
        ModelPart trail = m > 0 ? model.leftLeg : model.rightLeg;
        lead.xRot += -0.45F * s;
        trail.xRot += 0.35F * s;
        model.rightLeg.zRot += 0.2F * s;
        model.leftLeg.zRot -= 0.2F * s;
        lead.yRot += -m * 0.12F * s;
        trail.yRot += m * 0.1F * s;
        sm.drop = 1.2F * s;
    }

    /** Cuánto debe bajar el cuerpo entero (en píxeles del modelo) por la postura de combate; 0 si no aplica. */
    public static float dropFor(LivingEntity e) {
        Smooth sm = SMOOTH.get(e);
        return sm == null ? 0.0F : sm.drop;
    }

    /**
     * Pone una postura (calculada para el lado derecho) en los brazos del modelo: el brazo del arma con los ángulos resueltos y el otro
     * brazo apuntando a la empuñadura, o relajado hacia afuera si la empuñadura queda lejos.
     */
    public static void put(HumanoidModel<?> model, ModelPart main, ModelPart off, float m, double[] pose, float walkMain, float walkOff) {
        KatanaStance.solve(pose, OUT);
        placeBody(model, m * OUT.twist);
        main.xRot = OUT.mx + walkMain;
        main.yRot = m * OUT.my;
        main.zRot = m * OUT.mz;
        float w = OUT.follow;
        off.xRot = Mth.lerp(w, -0.35F + walkOff, OUT.ox + walkOff * 0.4F);
        off.yRot = Mth.lerp(w, -m * 0.4F, m * OUT.oy);
        off.zRot = Mth.lerp(w, -m * 0.25F, 0.0F);
    }

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        boolean right = arm == HumanoidArm.RIGHT;
        float m = right ? 1.0F : -1.0F;
        ModelPart main = right ? model.rightArm : model.leftArm;
        ModelPart off = right ? model.leftArm : model.rightArm;
        ModelPart legFwd = right ? model.rightLeg : model.leftLeg;
        ModelPart legBack = right ? model.leftLeg : model.rightLeg;
        InteractionHand hand = entity.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        float partial = Minecraft.getInstance().getFrameTime();
        float walkMain = main.xRot; // el balanceo de caminar que ya puso el juego
        float walkOff = off.xRot;

        boolean blocking = entity.isUsingItem() && entity.getUseItem().getItem() instanceof com.kensei.item.KatanaItem;
        Smooth sm = smooth(entity, blocking, entity.isSprinting());
        // postura base: espera, carrera o guardia según lo suavizado
        double[] base = KatanaStance.lerp(KatanaStance.IDLE, KatanaStance.SPRINT, sm.sprint);
        base = KatanaStance.lerp(base, KatanaStance.GUARD, sm.guard);
        float calm = (1.0F - sm.guard) * (1.0F - sm.sprint);
        float breathe = Mth.sin((entity.tickCount + partial) * 0.09F) * 0.025F * calm;
        base[1] += breathe;
        base[4] -= breathe * 0.8;
        float walkW = 0.22F * (1.0F - sm.guard * 0.7F);
        put(model, main, off, m, base, walkMain * walkW, walkOff * walkW);
        // piernas: postura de combate (más abierta y baja al guardar)
        stance(model, entity, m, 1.0F - sm.sprint, partial, 1.0F + 0.45F * sm.guard);

        ClientAnims.Anim anim = ClientAnims.get(entity, 40);

        // ---- esquive: los brazos se echan hacia atrás
        if (anim != null && anim.move == ClientAnims.DODGE) {
            float t = ClientAnims.age(anim, partial);
            if (t < 12.0F) {
                float k = Mth.sin(Mth.clamp(t / 12.0F, 0.0F, 1.0F) * (float) Math.PI);
                main.xRot = Mth.lerp(k, main.xRot, 0.5F);
                main.yRot = Mth.lerp(k, main.yRot, m * -0.2F);
                off.xRot = Mth.lerp(k, off.xRot, 0.6F);
                legFwd.xRot = Mth.lerp(k, legFwd.xRot, -0.7F);
                legBack.xRot = Mth.lerp(k, legBack.xRot, 0.6F);
                return;
            }
        }

        // ---- crítico: la hoja sube sobre la cabeza con las dos manos y cae
        if (anim != null && anim.move == ClientAnims.HEAVY) {
            float t = ClientAnims.age(anim, partial);
            float w = KenseiConfig.HEAVY_WINDUP.get();
            float strike = 5.0F;
            double[] p;
            float legSpread;
            if (t < w) {
                float u = ease(t / w);
                p = KatanaStance.lerp(base, KatanaStance.sample(KatanaStance.HEAVY, 0.5), u);
                legSpread = u * 0.45F;
            } else if (t < w + strike) {
                float u = Mth.clamp((t - w) / strike, 0.0F, 1.0F);
                float e = u * u;
                p = KatanaStance.sample(KatanaStance.HEAVY, 0.5 + 0.5 * e);
                legSpread = 0.45F + 0.25F * e;
            } else if (t < w + strike + 12.0F) {
                float u = ease((t - w - strike) / 12.0F);
                p = KatanaStance.lerp(KatanaStance.sample(KatanaStance.HEAVY, 1.0), base, u);
                legSpread = Mth.lerp(u, 0.7F, 0.0F);
            } else {
                return;
            }
            put(model, main, off, m, p, 0.0F, 0.0F);
            legFwd.xRot = -0.45F - legSpread;
            legBack.xRot = 0.35F + legSpread * 0.8F;
            model.rightLeg.zRot = 0.2F + legSpread * 0.25F;
            model.leftLeg.zRot = -0.2F - legSpread * 0.25F;
            sm.drop = 1.2F + legSpread * 2.0F;
            model.attackTime = 0.0F;
            return;
        }

        // ---- tajos del combo
        float t = model.attackTime;
        boolean swingingHere = entity.swinging && entity.swingingArm == hand && t > 0.0F;
        if (!swingingHere) {
            return;
        }
        int step = stepFor(entity);
        float u = Mth.clamp(t, 0.0F, 1.0F);
        float a = Mth.sin(u * (float) Math.PI);
        double[][] keys;
        switch (step) {
            case 1:
                keys = KatanaStance.SLASH1;
                break;
            case 2:
                keys = KatanaStance.SLASH2;
                break;
            case 3:
                keys = KatanaStance.SLASH3;
                break;
            case 7:
                keys = KatanaStance.THRUST;
                break;
            default:
                keys = KatanaStance.SLASH4;
                break;
        }
        // el golpe arranca rápido y frena al final; al empezar y al terminar se mezcla con la postura de espera para que no haya saltos
        float uu = 1.0F - (float) Math.pow(1.0F - u, 1.7F);
        double[] move = KatanaStance.sample(keys, uu);
        float win = ease(u / 0.16F);
        float wout = 1.0F - ease((u - 0.8F) / 0.2F);
        double[] p = KatanaStance.lerp(base, move, win * wout);
        put(model, main, off, m, p, 0.0F, 0.0F);
        // paso adelante: una pierna se adelanta y la otra se queda atrás (cambia de pierna en cada golpe); el torso arrastra las caderas
        float lunge = a * (step == 7 ? 0.95F : (step >= 4 ? 0.75F : 0.5F));
        float side = (step % 2 == 0 && step != 7) ? -1.0F : 1.0F;
        ModelPart lead = side > 0 ? legFwd : legBack;
        ModelPart trail = side > 0 ? legBack : legFwd;
        float k = Mth.clamp(a * 1.6F, 0.0F, 1.0F);
        lead.xRot = Mth.lerp(k, lead.xRot, -0.45F - lunge);
        trail.xRot = Mth.lerp(k, trail.xRot, 0.35F + lunge * 0.7F);
        float hip = m * OUT.twist * 0.45F;
        legFwd.yRot += hip;
        legBack.yRot += hip;
        sm.drop += a * (step == 7 ? 1.8F : 1.1F);
        // se quita el tajo normal del juego, que ya se dibujó aquí
        model.attackTime = 0.0F;
    }

    /** En qué golpe del combo va esta entidad (1 a 4): el que avisó el servidor, o si no hay aviso, contando los swings seguidos. */
    private static int stepFor(LivingEntity entity) {
        ClientAnims.Anim told = ClientAnims.get(entity, 14);
        if (told != null && ((told.move >= 1 && told.move <= 4) || told.move == ClientAnims.THRUST)) {
            return told.move;
        }
        Anim an = ANIMS.computeIfAbsent(entity, k -> new Anim());
        int start = entity.tickCount - entity.swingTime;
        if (start != an.startTick) {
            int gap = start - an.startTick;
            int window = KenseiConfig.COMBO_WINDOW.get() + 6;
            an.step = (an.step > 0 && gap <= window && gap >= 0) ? an.step % 4 + 1 : 1;
            an.startTick = start;
        }
        return an.step == 0 ? 1 : an.step;
    }
}
