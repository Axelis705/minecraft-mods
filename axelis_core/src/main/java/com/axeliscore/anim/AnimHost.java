package com.axeliscore.anim;

/**
 * Lo que una entidad animada guarda para animarse (solo se usa en el cliente, pero no carga clases del cliente, así que puede vivir
 * como campo de la entidad en los dos lados). El renderizador la conecta a un modelo y sus animaciones la primera vez que la dibuja.
 */
public final class AnimHost {
    private Geo geo;
    private Clip.Library lib;
    private Animator animator;
    private Pose pose;
    private float last = -1;
    /** Datos libres para el controlador del personaje (lo que necesite recordar entre cuadros). */
    public final java.util.Map<String, Object> data = new java.util.HashMap<>();
    /** Último gesto que se vio (para notar cuando el servidor pide uno nuevo). */
    public String lastAction = "";

    public boolean ready() {
        return animator != null;
    }

    /** La primera vez (o si cambió el modelo): prepara el animador. Devuelve true si se acaba de preparar. */
    public boolean bind(Geo geo, Clip.Library lib) {
        if (this.geo == geo && this.lib == lib && animator != null) {
            return false;
        }
        this.geo = geo;
        this.lib = lib;
        this.animator = new Animator(geo);
        this.pose = new Pose(geo.bones.length);
        this.last = -1;
        return true;
    }

    public Geo geo() {
        return geo;
    }

    public Clip.Library clips() {
        return lib;
    }

    public Animator animator() {
        return animator;
    }

    public Pose pose() {
        return pose;
    }

    /** Una animación por nombre (corto o largo); null si no existe. */
    public Clip clip(String name) {
        return lib == null ? null : lib.get(name);
    }

    /** Atajo: pone una animación en una capa (no hace nada si no existe). */
    public void play(String layer, String clip, float fade) {
        Clip c = clip(clip);
        if (c != null) {
            animator.play(layer, c, fade);
        }
    }

    /** Avanza hasta el tiempo {@code seconds} (reloj del juego) y calcula la postura. */
    public void advance(float seconds) {
        float dt = last < 0 ? 0 : seconds - last;
        if (dt < 0 || dt > 0.5F) {
            dt = 0; // vuelta atrás del reloj o la entidad estuvo lejos: sin saltos raros
        }
        last = seconds;
        animator.tick(dt);
        animator.sample(pose);
    }
}
