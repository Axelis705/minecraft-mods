package com.mundosdeaxelis.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Estado de movimiento de cada criatura, solo en el cliente, para que las animaciones se sientan vivas y fluidas.
 * <p>Se simula a 20 pasos por segundo (uno por tick de la criatura) y se dibuja interpolando con el tiempo parcial, así no
 * depende de los FPS. Da:
 * <ul>
 *   <li><b>inclinación</b> hacia delante al arrancar y hacia atrás al frenar ({@link #lean});</li>
 *   <li><b>ladeo</b> hacia dentro de las curvas ({@link #bank});</li>
 *   <li><b>estirar y aplastar</b>: se estira al saltar o caer y se aplasta al aterrizar, con rebote ({@link #squash});</li>
 *   <li><b>inercia</b> para colas, capas, orejas y hojas: se quedan atrás en las curvas ({@link #sway}) y se levantan al correr
 *       ({@link #drag});</li>
 *   <li><b>temblor</b> con rebote al recibir un golpe o al aterrizar ({@link #jiggle});</li>
 *   <li><b>aparición</b> con un pequeño salto al entrar en escena ({@link #pop});</li>
 *   <li>valores <b>suavizados</b> a gusto de cada modelo ({@link #smooth}), para que los cambios de pose no salten de golpe.</li>
 * </ul>
 */
public final class Motion {
    private static final Map<LivingEntity, Motion> ALL = new WeakHashMap<>();
    private static final int SLOTS = 10;

    private int lastTick = Integer.MIN_VALUE;
    private int bornTick = -1;
    private boolean pops;

    private float speed, speedO;
    private float lean, leanO, leanV;
    private float bank, bankO, bankV;
    private float squash, squashO, squashV;
    private float sway, swayO, swayV;
    private float drag, dragO, dragV;
    private float jiggle, jiggleO, jiggleV;
    private float yawPrev;
    private float vyPrev;
    private boolean wasGround = true;
    private int hurtPrev;
    /** Cuánto pesó el último aterrizaje (0..1) y en qué tick fue; {@link MotionFx} lo usa para el polvo. */
    float landed;
    int landedTick = -100;

    private final float[] slot = new float[SLOTS];
    private final float[] slotO = new float[SLOTS];
    private final float[] slotT = new float[SLOTS];
    private final float[] slotR = new float[SLOTS];
    private final boolean[] slotOn = new boolean[SLOTS];
    /** Muelles con rebote (para piezas que se abren o se cierran "con resorte"). */
    private final float[] wob = new float[SLOTS];
    private final float[] wobO = new float[SLOTS];
    private final float[] wobV = new float[SLOTS];
    private final float[] wobT = new float[SLOTS];
    private final float[] wobK = new float[SLOTS];
    private final float[] wobD = new float[SLOTS];
    private final boolean[] wobOn = new boolean[SLOTS];
    /** Relojes de sucesos: último tick en que se vio encendido y tick en que se encendió. */
    private final int[] evLastOn = new int[SLOTS];
    private final int[] evStart = new int[SLOTS];
    private final boolean[] evWas = new boolean[SLOTS];
    {
        java.util.Arrays.fill(evLastOn, Integer.MIN_VALUE / 2);
        java.util.Arrays.fill(evStart, Integer.MIN_VALUE / 2);
    }

    private Motion() {
    }

    /** El estado de esta criatura, puesto al día hasta su tick actual. */
    public static Motion of(LivingEntity e) {
        Motion m = ALL.get(e);
        if (m == null) {
            m = new Motion();
            ALL.put(e, m);
        }
        m.sync(e);
        return m;
    }

    static void clear() {
        ALL.clear();
    }

    private void sync(LivingEntity e) {
        int t = e.tickCount;
        if (lastTick == Integer.MIN_VALUE) {
            lastTick = t;
            bornTick = t;
            // solo "aparece" si es nueva de verdad (recién creada en el cliente), no si ya existía
            pops = t < 3;
            yawPrev = e.yBodyRot;
            wasGround = e.onGround();
            hurtPrev = e.hurtTime;
            return;
        }
        int n = t - lastTick;
        if (n <= 0) {
            return;
        }
        if (n > 10) {
            n = 1;
        }
        for (int i = 0; i < n; i++) {
            step(e);
        }
        lastTick = t;
    }

    private static float spring(float x, float target, float k, float damp, float[] v) {
        v[0] += (target - x) * k;
        v[0] *= 1.0F - damp;
        return x + v[0];
    }

    private final float[] tmp = new float[1];

    private void step(LivingEntity e) {
        float dx = (float) (e.getX() - e.xo), dz = (float) (e.getZ() - e.zo), dy = (float) (e.getY() - e.yo);
        float sp = Mth.sqrt(dx * dx + dz * dz);
        if (sp > 1.2F) {
            sp = 0.0F; // teletransporte: no cuenta como movimiento
        }
        speedO = speed;
        speed += (sp - speed) * 0.35F;
        float accel = speed - speedO;

        // inclinación: hacia delante al acelerar (y un poco al ir rápido), atrás al frenar
        float leanT = Mth.clamp(accel * 11.0F + Math.min(speed, 0.4F) * 0.45F, -0.32F, 0.38F);
        leanO = lean;
        tmp[0] = leanV;
        lean = spring(lean, leanT, 0.22F, 0.42F, tmp);
        leanV = tmp[0];

        // ladeo hacia dentro de la curva (más cuanto más rápido va)
        float dyaw = Mth.wrapDegrees(e.yBodyRot - yawPrev);
        yawPrev = e.yBodyRot;
        if (Math.abs(dyaw) > 60.0F) {
            dyaw = 0.0F;
        }
        float bankT = Mth.clamp(dyaw * 0.018F * Math.min(1.0F, speed * 5.0F + 0.25F), -0.32F, 0.32F);
        bankO = bank;
        tmp[0] = bankV;
        bank = spring(bank, bankT, 0.2F, 0.4F, tmp);
        bankV = tmp[0];

        // inercia de colas y capas: se quedan atrás en las curvas (rebotan un poco) y se levantan al correr
        swayO = sway;
        tmp[0] = swayV;
        sway = spring(sway, Mth.clamp(-dyaw * 0.035F, -0.7F, 0.7F), 0.14F, 0.22F, tmp);
        swayV = tmp[0];
        dragO = drag;
        tmp[0] = dragV;
        drag = spring(drag, Mth.clamp(speed * 3.2F, 0.0F, 1.0F), 0.12F, 0.3F, tmp);
        dragV = tmp[0];

        // estirar al subir o caer rápido; aplastarse al aterrizar (con rebote)
        boolean ground = e.onGround();
        float sqT = ground ? 0.0F : Mth.clamp(Math.abs(dy) * 1.8F, 0.0F, 0.28F);
        if (ground && !wasGround && vyPrev < -0.1F) {
            float hit = Math.min(1.0F, -vyPrev * 1.7F);
            squashV -= hit * 0.55F;
            jiggleV += hit * 0.35F;
            landed = hit;
            landedTick = e.tickCount;
        }
        wasGround = ground;
        vyPrev = dy;
        squashO = squash;
        tmp[0] = squashV;
        squash = spring(squash, sqT, 0.28F, 0.28F, tmp);
        squashV = tmp[0];

        // temblor con rebote al recibir un golpe
        if (e.hurtTime > hurtPrev) {
            jiggleV += 0.85F;
        }
        hurtPrev = e.hurtTime;
        jiggleO = jiggle;
        tmp[0] = jiggleV;
        jiggle = spring(jiggle, 0.0F, 0.32F, 0.2F, tmp);
        jiggleV = tmp[0];

        for (int i = 0; i < SLOTS; i++) {
            if (slotOn[i]) {
                slotO[i] = slot[i];
                slot[i] += (slotT[i] - slot[i]) * slotR[i];
            }
            if (wobOn[i]) {
                wobO[i] = wob[i];
                wobV[i] += (wobT[i] - wob[i]) * wobK[i];
                wobV[i] *= 1.0F - wobD[i];
                wob[i] += wobV[i];
            }
        }
    }

    // ------------------------------------------------------------------ lectura (con tiempo parcial)

    /** Velocidad horizontal suavizada, en bloques por tick. */
    public float speed(float pt) {
        return Mth.lerp(pt, speedO, speed);
    }

    /** Inclinación hacia delante en radianes (negativa = hacia atrás). */
    public float lean(float pt) {
        return Mth.lerp(pt, leanO, lean);
    }

    /** Ladeo en radianes: positivo = hacia su derecha. */
    public float bank(float pt) {
        return Mth.lerp(pt, bankO, bank);
    }

    /** Positivo = estirado; negativo = aplastado. */
    public float squash(float pt) {
        return Mth.lerp(pt, squashO, squash);
    }

    /** Inercia lateral para colas y capas (radianes aproximados). */
    public float sway(float pt) {
        return Mth.lerp(pt, swayO, sway);
    }

    /** 0..1: cuánto se levanta lo que cuelga al correr. */
    public float drag(float pt) {
        return Mth.lerp(pt, dragO, drag);
    }

    /** Temblor que rebota tras un golpe o un aterrizaje (-1..1 aprox.). */
    public float jiggle(float pt) {
        return Mth.lerp(pt, jiggleO, jiggle);
    }

    /** 0..1 (con un pequeño rebote por encima de 1) al aparecer; 1 el resto del tiempo. */
    public float pop(LivingEntity e, float pt) {
        if (!pops) {
            return 1.0F;
        }
        float a = (e.tickCount - bornTick + pt) / 9.0F;
        if (a >= 1.0F) {
            pops = false;
            return 1.0F;
        }
        return Anim.backOut(Math.max(0.0F, a));
    }

    /**
     * Un valor que va hacia {@code target} poco a poco ({@code rate} = fracción por tick, 0..1). Cada modelo usa sus casillas
     * (0..9) para lo que quiera: subir los brazos al enfadarse, abrir la boca, etc.
     */
    public float smooth(int i, float target, float rate, float pt) {
        if (!slotOn[i]) {
            slotOn[i] = true;
            slot[i] = slotO[i] = target;
        }
        slotT[i] = target;
        slotR[i] = rate;
        return Mth.lerp(pt, slotO[i], slot[i]);
    }

    /**
     * Como {@link #smooth} pero con resorte: llega pasándose un poco y rebota ({@code k} rigidez 0.1–0.4, {@code damp}
     * amortiguación 0.15–0.5). Casillas aparte de las de {@link #smooth}.
     */
    public float spring(int i, float target, float k, float damp, float pt) {
        if (!wobOn[i]) {
            wobOn[i] = true;
            wob[i] = wobO[i] = target;
        }
        wobT[i] = target;
        wobK[i] = k;
        wobD[i] = damp;
        return Mth.lerp(pt, wobO[i], wob[i]);
    }

    /** Da un empujón a un resorte (por ejemplo, el retroceso al disparar). */
    public void kick(int i, float v) {
        wobV[i] += v;
    }

    /**
     * Reloj de un suceso que dura un rato (cargar, rugir, disparar...): devuelve los ticks (con decimales) desde que
     * {@code on} dejó de cumplirse, o un número muy grande si nunca se cumplió o si se cumple ahora. Sirve para el
     * "después" de una acción (el retroceso al soltar el disparo, el golpe al terminar de cargar).
     */
    public float sinceEnd(int i, boolean on, LivingEntity e, float pt) {
        track(i, on, e);
        return on ? 9999.0F : e.tickCount + pt - evLastOn[i];
    }

    /** Ticks (con decimales) desde que {@code on} empezó a cumplirse; -1 si ahora no se cumple. */
    public float sinceStart(int i, boolean on, LivingEntity e, float pt) {
        track(i, on, e);
        return on ? e.tickCount + pt - evStart[i] : -1.0F;
    }

    /** Ticks (con decimales) desde la última vez que {@code on} empezó a cumplirse (se cumpla o no ahora); 9999 si nunca. */
    public float sinceLastStart(int i, boolean on, LivingEntity e, float pt) {
        track(i, on, e);
        return evStart[i] < -1000000 ? 9999.0F : e.tickCount + pt - evStart[i];
    }

    private void track(int i, boolean on, LivingEntity e) {
        if (on) {
            if (!evWas[i]) {
                evStart[i] = e.tickCount;
            }
            evLastOn[i] = e.tickCount;
        }
        evWas[i] = on;
    }

    /** Una curva de "golpe" de {@code len} ticks a partir de un reloj de suceso: 0 → 1 rápido y luego vuelve a 0. */
    public static float pulse(float since, float len) {
        if (since < 0.0F || since > len) {
            return 0.0F;
        }
        float u = since / len;
        return u < 0.2F ? Anim.easeOut(u / 0.2F) : 1.0F - Anim.smooth((u - 0.2F) / 0.8F);
    }
}
