package com.axeliscore.anim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reproduce animaciones sobre un modelo, por capas. Cada personaje tiene el suyo (en el cliente).
 * <ul>
 *     <li>Las capas se aplican en el orden en que se crearon: p. ej. "base" (quieto/caminar/correr), luego "cuerpo" (saludar, atacar:
 *     solo brazos y cabeza con una máscara) y una "aditiva" (respirar, temblar) que suma en vez de reemplazar.</li>
 *     <li>Cambiar de animación en una capa mezcla suave entre la vieja y la nueva durante {@code fade} segundos.</li>
 *     <li>Las animaciones de una vez se desvanecen solas al terminar (o se quedan en el último cuadro si son "hold").</li>
 *     <li>Los eventos (sonidos, partículas, avisos) se mandan a quien escuche al pasar por su tiempo.</li>
 * </ul>
 */
public final class Animator {
    public interface Listener {
        void onEvent(String layer, Clip clip, Clip.Event e);
    }

    public final class Layer {
        public final String name;
        final boolean additive;
        boolean[] mask;
        Clip clip;
        float time, speed = 1;
        Clip prev;
        float prevTime, prevSpeed = 1;
        /** Mezcla de la vieja a la nueva (0..1) y cuánto dura. */
        float fade = 1, fadeLen;
        /** Peso de la capa y hacia dónde va (para que se desvanezca sola). */
        float weight = 1, weightTarget = 1, weightSpeed;
        boolean ended;
        /** Peso por hueso (máscara suave), o null. */
        float[] maskW;
        Clip[] blendClips;
        float[] blendPts;
        float blendVal, phase;

        Layer(String name, boolean additive) {
            this.name = name;
            this.additive = additive;
        }

        public Clip clip() {
            return clip;
        }

        public float time() {
            return time;
        }

        public boolean playing() {
            return clip != null && weightTarget > 0 && !ended;
        }

        /** Peso de la capa de una vez (0 = no se ve, 1 = completa), sin desvanecer. */
        public Layer peso(float w) {
            this.weight = this.weightTarget = Math.max(0, Math.min(1, w));
            return this;
        }

        public float peso() {
            return weight;
        }

        /** Pone la animación en ese segundo (para el editor o para sincronizar). */
        public Layer tiempo(float t) {
            this.time = clip == null ? 0 : Math.max(0, Math.min(clip.length, t));
            this.ended = false;
            return this;
        }

        public Layer velocidad(float s) {
            this.speed = s;
            return this;
        }

        public float velocidad() {
            return speed;
        }

        /** Máscara suave: cuánto afecta la capa a cada hueso (0..1); los que no se nombran, nada. Los hijos heredan el peso. */
        public Layer mascara(Geo geo, java.util.Map<String, Float> pesos) {
            float[] m = new float[geo.bones.length];
            for (java.util.Map.Entry<String, Float> e : pesos.entrySet()) {
                int rb = geo.bone(e.getKey());
                if (rb < 0) {
                    continue;
                }
                for (int b = 0; b < m.length; b++) {
                    if (geo.isUnder(b, rb)) {
                        // el hueso más cercano manda (si "brazo" pesa 1 y "mano" 0.3, la mano queda en 0.3)
                        int d = depth(geo, b, rb);
                        if (d < maskDepth(b)) {
                            m[b] = e.getValue();
                            setMaskDepth(b, d, m.length);
                        }
                    }
                }
            }
            this.maskW = m;
            this.mask = null;
            maskDepths = null;
            return this;
        }

        private int[] maskDepths;

        private int maskDepth(int b) {
            return maskDepths == null ? Integer.MAX_VALUE : maskDepths[b];
        }

        private void setMaskDepth(int b, int d, int n) {
            if (maskDepths == null) {
                maskDepths = new int[n];
                java.util.Arrays.fill(maskDepths, Integer.MAX_VALUE);
            }
            maskDepths[b] = d;
        }

        /**
         * Mezcla por un valor (por ejemplo la velocidad): las animaciones van en orden con su punto ({@code walk} en 1.3, {@code run} en
         * 3.7) y se mezclan las dos más cercanas, con el paso sincronizado (los pies no resbalan al cambiar). Se cambia el valor con
         * {@link #valor(float)}.
         */
        public Layer mezcla(Clip[] clips, float[] puntos) {
            this.blendClips = clips;
            this.blendPts = puntos;
            this.weightTarget = 1;
            this.ended = false;
            return this;
        }

        public Layer valor(float v) {
            this.blendVal = v;
            return this;
        }

        /** Solo afecta a estos huesos (y a los que cuelgan de ellos). */
        public Layer mask(Geo geo, String... rootBones) {
            boolean[] m = new boolean[geo.bones.length];
            for (String r : rootBones) {
                int rb = geo.bone(r);
                if (rb < 0) {
                    continue;
                }
                for (int b = 0; b < m.length; b++) {
                    if (geo.isUnder(b, rb)) {
                        m[b] = true;
                    }
                }
            }
            this.mask = m;
            return this;
        }
    }

    static int depth(Geo geo, int b, int ancestor) {
        int d = 0;
        while (b >= 0 && b != ancestor) {
            b = geo.bones[b].parent;
            d++;
        }
        return d;
    }

    private final Geo geo;
    private final List<Layer> layers = new ArrayList<>();
    /** Para el editor: todo quieto o más lento. */
    private boolean pausado;
    private float escala = 1;
    private final Map<String, Layer> byName = new HashMap<>();
    private final Map<Clip, int[]> binds = new IdentityHashMap<>();
    private final Pose tmpA, tmpB;
    private Listener listener;
    /** Variables que las expresiones leen como variable.X (las pone el personaje cada cuadro). */
    private final Map<String, Double> vars = new HashMap<>();
    private float lifeTime;

    public Animator(Geo geo) {
        this.geo = geo;
        this.tmpA = new Pose(geo.bones.length);
        this.tmpB = new Pose(geo.bones.length);
    }

    public Geo geo() {
        return geo;
    }

    public void listen(Listener l) {
        this.listener = l;
    }

    /** Crea (o devuelve) una capa. Las capas se aplican en el orden en que se crean. */
    public Layer layer(String name, boolean additive) {
        Layer l = byName.get(name);
        if (l == null) {
            l = new Layer(name, additive);
            layers.add(l);
            byName.put(name, l);
        }
        return l;
    }

    public Layer layer(String name) {
        return layer(name, false);
    }

    public void var(String name, double v) {
        vars.put("variable." + name, v);
    }

    /** Valor que las expresiones leen como query.X (lo llenan los renderizadores: ground_speed, health...). */
    public void query(String name, double v) {
        vars.put("query." + name, v);
    }

    /** Detiene todas las capas (el editor de animaciones). */
    public void pausar(boolean p) {
        this.pausado = p;
    }

    public boolean pausado() {
        return pausado;
    }

    /** Cámara lenta (1 = normal). */
    public void escala(float e) {
        this.escala = e;
    }

    public java.util.List<Layer> capas() {
        return java.util.Collections.unmodifiableList(layers);
    }

    /** Pone una animación en una capa con mezcla suave. Si ya está sonando la misma, no la reinicia (salvo restart). */
    public void play(String layer, Clip clip, float fade, float speed, boolean restart) {
        Layer l = layer(layer);
        if (clip == null) {
            stop(layer, fade);
            return;
        }
        l.weightTarget = 1;
        l.weightSpeed = fade > 0 ? 1 / fade : 1000;
        if (l.clip == clip && !restart && !l.ended) {
            l.speed = speed;
            return;
        }
        if (l.clip != null && fade > 0 && l.weight > 0) {
            l.prev = l.clip;
            l.prevTime = l.time;
            l.prevSpeed = l.speed;
            l.fade = 0;
            l.fadeLen = fade;
        } else {
            l.prev = null;
            l.fade = 1;
            if (l.weight <= 0) {
                // la capa estaba apagada: se enciende suave en vez de mezclar con nada
                l.weight = fade > 0 ? 0 : 1;
            }
        }
        l.clip = clip;
        l.time = 0;
        l.speed = speed;
        l.ended = false;
    }

    public void play(String layer, Clip clip, float fade) {
        play(layer, clip, fade, 1, false);
    }

    /** Desvanece una capa en {@code fade} segundos. */
    public void stop(String layer, float fade) {
        Layer l = byName.get(layer);
        if (l == null) {
            return;
        }
        l.weightTarget = 0;
        l.weightSpeed = fade > 0 ? 1 / fade : 1000;
    }

    public Layer get(String layer) {
        return byName.get(layer);
    }

    /** Avanza el tiempo (segundos) y manda los eventos por los que se pasó. */
    public void tick(float dt) {
        dt = pausado ? 0 : dt * escala;
        lifeTime += dt;
        for (Layer l : layers) {
            if (l.blendClips != null) {
                if (l.weight != l.weightTarget) {
                    float step = l.weightSpeed * dt;
                    l.weight = l.weight < l.weightTarget ? Math.min(l.weightTarget, l.weight + step) : Math.max(l.weightTarget, l.weight - step);
                }
                float len = blendLen(l);
                if (len > 0) {
                    l.phase = (l.phase + dt * l.speed / len) % 1F;
                }
                continue;
            }
            if (l.weight != l.weightTarget) {
                float step = l.weightSpeed * dt;
                l.weight = l.weight < l.weightTarget ? Math.min(l.weightTarget, l.weight + step) : Math.max(l.weightTarget, l.weight - step);
            }
            if (l.prev != null) {
                l.prevTime = advance(l.prev, l.prevTime, dt * l.prevSpeed);
                l.fade += l.fadeLen > 0 ? dt / l.fadeLen : 1;
                if (l.fade >= 1) {
                    l.fade = 1;
                    l.prev = null;
                }
            }
            if (l.clip == null || l.ended) {
                continue;
            }
            float before = l.time;
            float after = before + dt * l.speed;
            fire(l, before, after);
            Clip c = l.clip;
            if (c.loop == Clip.Loop.LOOP && c.length > 0) {
                while (after >= c.length) {
                    after -= c.length;
                    fire(l, -1e-4F, after);
                }
                l.time = after;
            } else if (after >= c.length) {
                l.time = c.length;
                if (c.loop == Clip.Loop.ONCE) {
                    l.ended = true;
                    l.weightTarget = 0;
                    l.weightSpeed = 1 / 0.2F;
                }
            } else {
                l.time = after;
            }
        }
    }

    private static float advance(Clip c, float t, float dt) {
        t += dt;
        if (c.loop == Clip.Loop.LOOP && c.length > 0) {
            t %= c.length;
        } else if (t > c.length) {
            t = c.length;
        }
        return t;
    }

    private void fire(Layer l, float from, float to) {
        if (listener == null || l.clip.events.length == 0) {
            return;
        }
        for (Clip.Event e : l.clip.events) {
            if (e.t > from && e.t <= to) {
                listener.onEvent(l.name, l.clip, e);
            }
        }
    }

    /** ¿Terminó la animación de una vez de esa capa (o no hay nada)? */
    public boolean finished(String layer) {
        Layer l = byName.get(layer);
        return l == null || l.clip == null || l.ended;
    }

    /** Las dos animaciones de la mezcla que tocan y cuánto de la segunda. */
    private static int blendIndex(Layer l, float[] f) {
        float[] p = l.blendPts;
        int n = p.length;
        if (n == 1 || l.blendVal <= p[0]) {
            f[0] = 0;
            return 0;
        }
        if (l.blendVal >= p[n - 1]) {
            f[0] = 0;
            return n - 1;
        }
        int i = 0;
        while (i < n - 2 && l.blendVal > p[i + 1]) {
            i++;
        }
        f[0] = (l.blendVal - p[i]) / Math.max(1e-4F, p[i + 1] - p[i]);
        return i;
    }

    private static float blendLen(Layer l) {
        float[] f = new float[1];
        int i = blendIndex(l, f);
        float a = l.blendClips[i].length;
        float b = i + 1 < l.blendClips.length ? l.blendClips[i + 1].length : a;
        return a + (b - a) * f[0];
    }

    /** Calcula la postura: empieza en reposo y aplica las capas en orden. */
    public void sample(Pose out) {
        out.reset();
        for (Layer l : layers) {
            if (l.blendClips != null) {
                if (l.weight <= 0.001F) {
                    continue;
                }
                float[] f = new float[1];
                int i = blendIndex(l, f);
                tmpB.copyFrom(out);
                Clip a = l.blendClips[i];
                apply(a, l.phase * a.length, tmpB);
                if (f[0] > 0.001F && i + 1 < l.blendClips.length) {
                    Clip b = l.blendClips[i + 1];
                    tmpA.copyFrom(out);
                    apply(b, l.phase * b.length, tmpA);
                    tmpB.blendTo(tmpA, f[0], (boolean[]) null);
                }
                blend(out, tmpB, Ease.smooth(l.weight), l);
                continue;
            }
            if (l.clip == null || l.weight <= 0.001F) {
                continue;
            }
            if (l.additive) {
                tmpA.reset();
                apply(l.clip, l.time, tmpA);
                float w = l.weight;
                for (int b = 0; b < out.n; b++) {
                    if (l.mask != null && !l.mask[b]) {
                        continue;
                    }
                    float wb = l.maskW != null ? w * l.maskW[b] : w;
                    for (int k = b * 3; k < b * 3 + 3; k++) {
                        out.rot[k] += tmpA.rot[k] * wb;
                        out.pos[k] += tmpA.pos[k] * wb;
                        out.scl[k] *= 1 + (tmpA.scl[k] - 1) * wb;
                    }
                }
                continue;
            }
            tmpB.copyFrom(out);
            apply(l.clip, l.time, tmpB);
            if (l.prev != null && l.fade < 1) {
                tmpA.copyFrom(out);
                apply(l.prev, l.prevTime, tmpA);
                tmpA.blendTo(tmpB, Ease.smooth(l.fade), (boolean[]) null);
                blend(out, tmpA, Ease.smooth(l.weight), l);
            } else {
                blend(out, tmpB, Ease.smooth(l.weight), l);
            }
        }
    }

    private static void blend(Pose out, Pose o, float w, Layer l) {
        if (l.maskW != null) {
            out.blendTo(o, w, l.maskW);
        } else {
            out.blendTo(o, w, l.mask);
        }
    }

    private int[] bind(Clip c) {
        return binds.computeIfAbsent(c, k -> {
            int[] b = new int[k.tracks.length];
            for (int i = 0; i < b.length; i++) {
                b[i] = geo.bone(k.tracks[i].bone);
            }
            return b;
        });
    }

    private final float[] v3 = new float[3];

    /** Escribe en {@code p} los huesos que esta animación mueve (en el tiempo t); los demás se quedan como estaban. */
    private void apply(Clip c, float t, Pose p) {
        int[] b = bind(c);
        Expr.Ctx ctx = name -> {
            switch (name) {
                case "query.anim_time":
                    return t;
                case "query.life_time":
                    return lifeTime;
                case "query.anim_length":
                    return c.length;
                default:
                    Double v = vars.get(name);
                    return v == null ? 0 : v;
            }
        };
        final float deg = (float) (Math.PI / 180);
        for (int i = 0; i < b.length; i++) {
            int bone = b[i];
            if (bone < 0) {
                continue;
            }
            Clip.Track tr = c.tracks[i];
            int o = bone * 3;
            if (tr.rot != null) {
                tr.rot.sample(t, ctx, v3);
                p.rot[o] = v3[0] * deg;
                p.rot[o + 1] = v3[1] * deg;
                p.rot[o + 2] = v3[2] * deg;
            }
            if (tr.pos != null) {
                tr.pos.sample(t, ctx, v3);
                p.pos[o] = v3[0];
                p.pos[o + 1] = -v3[1];
                p.pos[o + 2] = v3[2];
            }
            if (tr.scl != null) {
                tr.scl.sample(t, ctx, v3);
                p.scl[o] = v3[0];
                p.scl[o + 1] = v3[1];
                p.scl[o + 2] = v3[2];
            }
        }
    }
}
