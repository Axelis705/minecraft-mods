package com.axeliscore.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Una fase de un {@link Jefe}: desde qué vida empieza, sus ataques, el color de la barra, el aspecto (textura), la música y cómo entra
 * (título grande, animación, temblor). Ejemplo:
 * <pre>
 *     new Fase(0.5F).color(BossEvent.BossBarColor.RED).titulo(Component.translatable("mimod.jefe.furia"), 0xFF5A3A)
 *             .entrada("phase", 52).dano(1.3F).rapidez(1.25F).aspecto(1).musica("mimod:musica.jefe_2")
 *             .ataque(martillazo).ataque(lluvia);
 * </pre>
 */
public final class Fase {
    /** Fracción de la vida en que empieza (1 = la primera; 0.5 = a la mitad). */
    public final float desde;
    final List<Ataque> ataques = new ArrayList<>();
    BossEvent.BossBarColor color = BossEvent.BossBarColor.PURPLE;
    Component titulo, subtitulo;
    int colorTitulo = 0xFFD24A;
    String clipEntrada = "", sonidoEntrada = "";
    int duracionEntrada = 40;
    float dano = 1F, rapidez = 1F, caminar = 1F;
    String musica = "";
    int aspecto;
    Consumer<Jefe> alEntrar;

    public Fase(float desde) {
        this.desde = desde;
    }

    public Fase ataque(Ataque a) {
        ataques.add(a);
        return this;
    }

    public Fase color(BossEvent.BossBarColor c) {
        this.color = c;
        return this;
    }

    /** Título grande (y subtítulo opcional) para los que estén cerca al entrar en esta fase. */
    public Fase titulo(Component t, int rgb) {
        this.titulo = t;
        this.colorTitulo = rgb;
        return this;
    }

    public Fase subtitulo(Component s) {
        this.subtitulo = s;
        return this;
    }

    /** Animación al entrar y cuántos ticks dura (en ese tiempo no recibe daño). */
    public Fase entrada(String clip, int ticks) {
        this.clipEntrada = clip;
        this.duracionEntrada = ticks;
        return this;
    }

    public Fase sonidoEntrada(String id) {
        this.sonidoEntrada = id;
        return this;
    }

    /** Multiplica el daño de los ataques (usar {@link Jefe#dano}). */
    public Fase dano(float k) {
        this.dano = k;
        return this;
    }

    /** Más de 1 = descansa menos entre ataques. */
    public Fase rapidez(float k) {
        this.rapidez = Math.max(0.2F, k);
        return this;
    }

    /** Velocidad al caminar (1 = normal). */
    public Fase caminar(float k) {
        this.caminar = k;
        return this;
    }

    /** Música en bucle mientras dura (nombre del sonido, en sounds.json con "stream": true); "" = sin música. */
    public Fase musica(String id) {
        this.musica = id == null ? "" : id;
        return this;
    }

    /** Aspecto (número de textura) que el renderizador del jefe usa en esta fase. */
    public Fase aspecto(int n) {
        this.aspecto = n;
        return this;
    }

    /** Algo extra al entrar (invocar ayudantes, cambiar la arena...). */
    public Fase alEntrar(Consumer<Jefe> c) {
        this.alEntrar = c;
        return this;
    }

    /** Sus ataques (en orden). */
    public List<Ataque> ataques() {
        return ataques;
    }

    public String musica() {
        return musica;
    }

    public int aspecto() {
        return aspecto;
    }
}
