package com.mundosdeaxelis.cinematic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Definición de una cinemática: trayectoria de cámara, subtítulos y "cues"
 * (cosas que pasan en el mundo en un tick concreto: sonidos, partículas, aparecer un jefe...).
 *
 * Las posiciones se escriben relativas a un "origen" que se indica al reproducirla,
 * así la misma cinemática se puede usar en distintos lugares.
 */
public final class Cinematic {

    /** Algo que ocurre en el servidor en un momento de la cinemática. */
    @FunctionalInterface
    public interface CueAction {
        void run(ServerLevel level, Vec3 origin);
    }

    public record Cue(int tick, CueAction action) {
    }

    private final String id;
    private final int duration;
    private final int fadeOut;
    private final boolean bars;
    private final List<Keyframe> frames;
    private final List<Subtitle> subtitles;
    private final List<Cue> cues;

    private Cinematic(Builder b) {
        this.id = b.id;
        this.duration = b.duration;
        this.fadeOut = b.fadeOut;
        this.bars = b.bars;
        this.frames = List.copyOf(b.frames);
        this.subtitles = List.copyOf(b.subtitles);
        List<Cue> sorted = new ArrayList<>(b.cues);
        sorted.sort(Comparator.comparingInt(Cue::tick));
        this.cues = List.copyOf(sorted);
    }

    public String id() {
        return id;
    }

    /** Duración total en ticks (20 ticks = 1 segundo). Incluye el fundido a negro final. */
    public int duration() {
        return duration;
    }

    public int fadeOut() {
        return fadeOut;
    }

    public boolean bars() {
        return bars;
    }

    public List<Keyframe> frames() {
        return frames;
    }

    public List<Subtitle> subtitles() {
        return subtitles;
    }

    public List<Cue> cues() {
        return cues;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        /** Todas las cinemáticas van un poco más lentas para dar tiempo a leer los subtítulos. */
        private static final double TIME_SCALE = 1.4D;

        private static int scaled(int ticks) {
            return (int) Math.round(ticks * TIME_SCALE);
        }

        private final String id;
        private int duration = 200;
        private int fadeOut = 30;
        private boolean bars = true;
        private final List<Keyframe> frames = new ArrayList<>();
        private final List<Subtitle> subtitles = new ArrayList<>();
        private final List<Cue> cues = new ArrayList<>();

        private Builder(String id) {
            this.id = id;
        }

        public Builder duration(int ticks) {
            this.duration = scaled(ticks);
            return this;
        }

        public Builder fadeOut(int ticks) {
            this.fadeOut = ticks;
            return this;
        }

        public Builder bars(boolean bars) {
            this.bars = bars;
            return this;
        }

        /** Cámara en (px,py,pz) mirando a (tx,ty,tz) en el tick indicado. Añadir en orden de tick. */
        public Builder frame(int tick, double px, double py, double pz, double tx, double ty, double tz) {
            frames.add(new Keyframe(scaled(tick), new Vec3(px, py, pz), new Vec3(tx, ty, tz)));
            return this;
        }

        public Builder subtitle(int start, int end, String text) {
            subtitles.add(new Subtitle(scaled(start), scaled(end), Component.literal(text)));
            return this;
        }

        public Builder cue(int tick, CueAction action) {
            cues.add(new Cue(scaled(tick), action));
            return this;
        }

        public Cinematic build() {
            if (frames.size() < 2) {
                throw new IllegalStateException("La cinematica " + id + " necesita al menos 2 puntos de camara");
            }
            return new Cinematic(this);
        }
    }
}
