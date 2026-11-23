package com.axeliscore.combat;

import net.minecraft.world.entity.LivingEntity;

/**
 * Un ataque de un {@link Jefe}: qué animación usa, cuánto dura, en qué tick pega, a qué distancia sirve y qué hace. Lo normal es poner
 * los avisos al empezar (que terminan justo en el tick del golpe) y lo demás en el golpe:
 * <pre>
 *     new Ataque("martillazo").clip("slam").dura(40).golpe(23).distancia(0, 4.5F).peso(3).espera(60)
 *             .alEmpezar((j, t) -&gt; Aviso.circulo(j.level(), j.position(), 4.6F, 23).dano(j, j.dano(14)).lanzar());
 * </pre>
 */
public final class Ataque {
    /** Lo que hace el ataque en un momento (al empezar, al golpear). */
    @FunctionalInterface
    public interface Accion {
        void run(Jefe j, LivingEntity objetivo);
    }

    /** Lo que hace cada tick mientras dura ({@code t}: ticks desde que empezó). */
    @FunctionalInterface
    public interface Mientras {
        void run(Jefe j, LivingEntity objetivo, int t);
    }

    public final String id;
    String clip = "";
    int dura = 30, golpe = 15, peso = 1, espera = 40, despues = 30;
    float min = 0, max = 4;
    boolean quieto = true;
    Accion alEmpezar, alGolpe;
    Mientras mientras;

    public Ataque(String id) {
        this.id = id;
    }

    /** Animación del jefe (el nombre del clip, por ejemplo "slam"). */
    public Ataque clip(String c) {
        this.clip = c;
        return this;
    }

    /** Cuánto dura (ticks) sin poder hacer otra cosa. */
    public Ataque dura(int ticks) {
        this.dura = ticks;
        return this;
    }

    /** En qué tick (desde que empieza) llega el golpe. */
    public Ataque golpe(int tick) {
        this.golpe = tick;
        return this;
    }

    /** Distancia al objetivo en que sirve. */
    public Ataque distancia(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }

    /** Qué tan seguido lo elige (contra los otros que sirvan). */
    public Ataque peso(int p) {
        this.peso = Math.max(1, p);
        return this;
    }

    /** Ticks antes de poder repetir este mismo ataque. */
    public Ataque espera(int ticks) {
        this.espera = ticks;
        return this;
    }

    /** Ticks de descanso después de este ataque (antes del siguiente, el que sea). */
    public Ataque despues(int ticks) {
        this.despues = ticks;
        return this;
    }

    /** Si puede caminar mientras ataca (por defecto se queda quieto). */
    public Ataque caminando() {
        this.quieto = false;
        return this;
    }

    public Ataque alEmpezar(Accion a) {
        this.alEmpezar = a;
        return this;
    }

    public Ataque alGolpe(Accion a) {
        this.alGolpe = a;
        return this;
    }

    public Ataque mientras(Mientras m) {
        this.mientras = m;
        return this;
    }
}
