package com.kensei.combat;

import com.kensei.KenseiConfig;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Lo que el servidor sabe del combate de cada jugador (no se guarda: al volver a entrar se empieza de cero). Solo se usa en el hilo del servidor. */
public final class CombatState {
    private static final Map<UUID, CombatState> STATES = new HashMap<>();

    /** Energía: cada 100 es una barra de esquive. */
    public float energy;
    /** Hasta qué tick del mundo dura la invulnerabilidad del esquive. */
    public long dodgeUntil = -1;
    public long dodgeReadyAt = 0;
    /** Hasta qué tick se puede hacer el contraataque (0 = no hay). */
    public long counterUntil = 0;
    public long lastHitTick = -1000;
    /** Último tick en que hizo algo (atacar, esquivar): mientras pasa poco tiempo desde entonces, la energía no se recarga. */
    public long lastActionTick = -1000;
    public long lastPerfectTick = -1000;
    /** Golpe del combo en el que va (0 = ninguno). */
    public int comboStep = 0;
    /** Golpe normal (1 a 4) en el que va la cadena de tajos y cuándo fue el último. */
    public int swingStep = 0;
    public long lastSwingTick = -1000;
    /** Hasta qué tick no se puede empezar otro ataque. */
    public long swingReadyAt = 0;
    /** Golpe normal que conectará en pendingHitAt (0 = ninguno). */
    public long pendingHitAt = 0;
    public int pendingStep = 0;
    /** Crítico en preparación: cuándo cae el golpe (0 = ninguno) y cuándo se puede volver a usar. */
    public long pendingHeavyAt = 0;
    public long heavyReadyAt = 0;
    /** Mientras se reparte un golpe: cuál es (1 a 4 normales, 5 crítico, 0 = ninguno). */
    public int activeStep = 0;
    public boolean dirty = true;
    public long lastSyncTick = -1000;
    private float regenCarry = 0;
    /** Estamina: se gasta al atacar y bloquear, se recarga sola cuando dejas de hacerlo. */
    public float stamina;
    /** Desde qué tick se puede empezar a recargar la estamina. */
    public long staminaRegenAt = 0;
    /** Hasta qué tick estás agotado (sin estamina: no se ataca ni se recarga). */
    public long exhaustedUntil = 0;
    /** Último tick en que se avisó "sin estamina" (para no repetir el aviso en cada clic). */
    public long lastDenyTick = -1000;

    private CombatState() {
        this.energy = (float) (double) KenseiConfig.START_ENERGY.get();
        this.stamina = KenseiConfig.staminaMax();
    }

    public static CombatState of(ServerPlayer p) {
        return STATES.computeIfAbsent(p.getUUID(), id -> new CombatState());
    }

    public static void remove(UUID id) {
        STATES.remove(id);
    }

    public static void clearAll() {
        STATES.clear();
    }

    public void reset() {
        energy = (float) (double) KenseiConfig.START_ENERGY.get();
        stamina = KenseiConfig.staminaMax();
        staminaRegenAt = 0;
        exhaustedUntil = 0;
        dodgeUntil = -1;
        dodgeReadyAt = 0;
        lastActionTick = -1000;
        counterUntil = 0;
        comboStep = 0;
        swingStep = 0;
        swingReadyAt = 0;
        pendingHitAt = 0;
        pendingHeavyAt = 0;
        heavyReadyAt = 0;
        activeStep = 0;
        dirty = true;
    }

    /** Cancela lo que está a medias (golpe o crítico en preparación), por ejemplo al esquivar. */
    public void cancelPending() {
        pendingHitAt = 0;
        pendingHeavyAt = 0;
    }

    public void add(float amount) {
        float max = KenseiConfig.maxEnergy();
        float v = Math.max(0, Math.min(max, energy + amount));
        if (v != energy) {
            energy = v;
            dirty = true;
        }
    }

    public boolean spend(float amount) {
        if (energy + 0.001F < amount) {
            return false;
        }
        energy = Math.max(0, energy - amount);
        dirty = true;
        return true;
    }

    public boolean exhausted(long now) {
        return KenseiConfig.STAMINA_ENABLED.get() && now < exhaustedUntil;
    }

    /** ¿Alcanza la estamina para un movimiento que cuesta esto (y no se está agotado)? */
    public boolean canSpendStamina(float cost, long now) {
        if (!KenseiConfig.STAMINA_ENABLED.get()) {
            return true;
        }
        return now >= exhaustedUntil && stamina + 0.001F >= cost;
    }

    /**
     * Gasta estamina (sin bajar de 0) y reinicia la espera de recarga. Si se acaba, queda agotado un rato.
     * @return true si se quedó en 0 (agotado)
     */
    public boolean useStamina(float cost, long now) {
        if (!KenseiConfig.STAMINA_ENABLED.get() || cost <= 0) {
            return false;
        }
        stamina = Math.max(0.0F, stamina - cost);
        staminaRegenAt = now + KenseiConfig.STAMINA_DELAY.get();
        dirty = true;
        if (stamina <= 0.01F) {
            exhaustedUntil = now + KenseiConfig.EXHAUST_TICKS.get();
            return true;
        }
        return false;
    }

    public void addStamina(float amount) {
        float v = Math.max(0.0F, Math.min(KenseiConfig.staminaMax(), stamina + amount));
        if (v != stamina) {
            stamina = v;
            dirty = true;
        }
    }

    public void markAction(long now) {
        lastActionTick = now;
    }

    /** Recarga pasiva (por tick). */
    public void regen(float perTick) {
        regenCarry += perTick;
        if (regenCarry >= 1.0F) {
            int whole = (int) regenCarry;
            regenCarry -= whole;
            add(whole);
        }
    }
}
