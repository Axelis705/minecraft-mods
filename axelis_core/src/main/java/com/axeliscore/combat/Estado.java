package com.axeliscore.combat;

import com.axeliscore.plat.Ids;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Un estado de combate (aturdido, quemado, congelado...): qué hace y cómo se ve. Los del Core están en {@link Estados}; un mod crea
 * los suyos así (y los registra al arrancar, igual en el servidor y en el cliente):
 * <pre>
 *     public static final Estado ELECTRIZADO = Estados.registrar(new Estado("mimod", "electrizado", 0x9FE8FF)
 *             .icono(Ids.rl("mimod", "textures/gui/electrizado.png"))      // 16x16 (o un ícono del Core con .icono(n))
 *             .cada(20, (e, a) -> e.hurt(e.damageSources().lightningBolt(), a.nivel))
 *             .lento(0.15F));
 * </pre>
 * El nombre que se ve es la traducción {@code estado.<mod>.<nombre>}.
 */
public class Estado {
    /** Lo que hace un estado en el servidor mientras dura (cada tantos ticks, al ponerse o al quitarse). */
    @FunctionalInterface
    public interface Accion {
        void run(LivingEntity e, Estados.Activo a);
    }

    public final String id;
    public final int color;
    /** Ícono de la hoja del Core ({@code textures/gui/estados.png}, de 16 px) o -1 si trae uno propio. */
    int iconoCore = -1;
    ResourceLocation iconoPropio;
    /** Cuánto daño de más recibe por nivel (0.25 = +25 % por nivel). */
    float danoExtra;
    /** Cuánto más lento se mueve por nivel (0.15 = 15 % por nivel; hasta 90 %). */
    float lentitud;
    /** No se puede mover ni atacar (las criaturas del Core tampoco usan sus ataques). */
    boolean aturde;
    /** Se quita al meterse al agua. */
    boolean seApagaConAgua;
    int cadaTicks;
    Accion cada, alPoner, alQuitar;
    /** Partículas que salen del que lo tiene (en el cliente): "flame", "snowflake", "drip" (gotas), "stars", "spark" o "" para ninguna. */
    String particula = "";

    public Estado(String modid, String nombre, int color) {
        this.id = modid + ":" + nombre;
        this.color = color & 0xFFFFFF;
    }

    public Estado icono(int indiceDelCore) {
        this.iconoCore = indiceDelCore;
        return this;
    }

    public Estado icono(ResourceLocation tex16) {
        this.iconoPropio = tex16;
        return this;
    }

    public Estado recibeMas(float porNivel) {
        this.danoExtra = porNivel;
        return this;
    }

    public Estado lento(float porNivel) {
        this.lentitud = porNivel;
        return this;
    }

    public Estado aturde() {
        this.aturde = true;
        return this;
    }

    public Estado seApagaConAgua() {
        this.seApagaConAgua = true;
        return this;
    }

    public Estado cada(int ticks, Accion a) {
        this.cadaTicks = Math.max(1, ticks);
        this.cada = a;
        return this;
    }

    public Estado alPoner(Accion a) {
        this.alPoner = a;
        return this;
    }

    public Estado alQuitar(Accion a) {
        this.alQuitar = a;
        return this;
    }

    public Estado particula(String p) {
        this.particula = p == null ? "" : p;
        return this;
    }

    /** Su nombre en el idioma del jugador. */
    public Component nombre() {
        return Component.translatable("estado." + id.replace(':', '.'));
    }

    /** Textura del ícono y su posición (u) en ella: {textura, u}. */
    public Object[] icono() {
        if (iconoPropio != null) {
            return new Object[] {iconoPropio, 0};
        }
        return new Object[] {ICONOS, Math.max(0, iconoCore) * 16};
    }

    public boolean aturdeAlQueLoTiene() {
        return aturde;
    }

    public String particula() {
        return particula;
    }

    /** La hoja de íconos del Core (16 px cada uno, en fila). */
    public static final ResourceLocation ICONOS = Ids.core("textures/gui/estados.png");
    /** Ancho de la hoja de íconos del Core. */
    public static final int ICONOS_W = 128;

    @Override
    public String toString() {
        return id;
    }
}
