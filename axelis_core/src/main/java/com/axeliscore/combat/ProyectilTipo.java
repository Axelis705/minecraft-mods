package com.axeliscore.combat;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un tipo de proyectil: cómo se ve, cómo vuela y qué hace al pegar. Se registra al arrancar (igual en el servidor y en el cliente) y se
 * lanza con {@link Proyectil}:
 * <pre>
 *     public static final ProyectilTipo BOLA_FUEGO = ProyectilTipo.registrar(new ProyectilTipo("mimod", "bola_fuego")
 *             .orbe(0xFFFF7A2E, 0.6F).estela("flame")
 *             .dano(7).area(2.5F).estado(Estados.QUEMADO, 80, 1).impacto("minecraft:entity.generic.explode", "explosion"));
 *     public static final ProyectilTipo LANZA = ProyectilTipo.registrar(new ProyectilTipo("mimod", "lanza")
 *             .objeto("minecraft:trident").gravedad(0.03F).dano(9));
 *     public static final ProyectilTipo CRISTAL = ProyectilTipo.registrar(new ProyectilTipo("mimod", "cristal")
 *             .modelo(Ids.rl("mimod", "geo/cristal.geo.json"), Ids.rl("mimod", "textures/entity/cristal.png"), 1F).gira(12));
 * </pre>
 */
public final class ProyectilTipo {
    private static final Map<String, ProyectilTipo> REGISTRO = new LinkedHashMap<>();

    public final String id;
    // ---- cómo se ve (cliente)
    public String aspecto = "orbe";
    public int color = 0xFFFFFFFF;
    public float tam = 0.5F;
    public String objeto = "";
    public ResourceLocation geo, textura;
    /** Grados por tick que gira sobre sí mismo (0 = apunta hacia donde vuela). */
    public float gira;
    public String estela = "";
    // ---- cómo vuela
    public float gravedad, arrastre = 1F, persigue;
    public int vida = 100;
    public boolean atraviesaAgua = true;
    // ---- qué hace al pegar (servidor)
    public float dano = 4, area, empuje = 0.35F, temblor;
    public Estado estado;
    public int estadoTicks, estadoNivel = 1;
    public String sonidoImpacto = "", particulasImpacto = "crit", sonidoLanzar = "";

    public ProyectilTipo(String modid, String nombre) {
        this.id = modid + ":" + nombre;
    }

    public static synchronized ProyectilTipo registrar(ProyectilTipo t) {
        REGISTRO.put(t.id, t);
        return t;
    }

    public static ProyectilTipo porId(String id) {
        return REGISTRO.get(id);
    }

    /** Esfera de luz de un color (con halo). */
    public ProyectilTipo orbe(int argb, float tamano) {
        this.aspecto = "orbe";
        this.color = argb;
        this.tam = tamano;
        return this;
    }

    /** Un objeto de Minecraft o de un mod ("minecraft:snowball"). */
    public ProyectilTipo objeto(String itemId) {
        this.aspecto = "objeto";
        this.objeto = itemId;
        return this;
    }

    /** Un modelo de Blockbench (geo.json) con su textura. */
    public ProyectilTipo modelo(ResourceLocation geo, ResourceLocation tex, float tamano) {
        this.aspecto = "modelo";
        this.geo = geo;
        this.textura = tex;
        this.tam = tamano;
        return this;
    }

    /** Tamaño (orbe: diámetro en bloques; objeto y modelo: escala). */
    public ProyectilTipo tam(float t) {
        this.tam = t;
        return this;
    }

    public ProyectilTipo gira(float gradosPorTick) {
        this.gira = gradosPorTick;
        return this;
    }

    /** Partículas que deja al volar (ver {@link Combate#particula}). */
    public ProyectilTipo estela(String particula) {
        this.estela = particula;
        return this;
    }

    public ProyectilTipo gravedad(float g) {
        this.gravedad = g;
        return this;
    }

    /** Freno por tick (1 = no frena; 0.98 = como una flecha). */
    public ProyectilTipo arrastre(float a) {
        this.arrastre = a;
        return this;
    }

    /** Cuánto gira hacia su objetivo por tick (0 = va derecho; 0.08 = persigue suave; 0.3 = casi no falla). */
    public ProyectilTipo persigue(float k) {
        this.persigue = k;
        return this;
    }

    /** Ticks que vuela antes de deshacerse. */
    public ProyectilTipo vida(int ticks) {
        this.vida = ticks;
        return this;
    }

    public ProyectilTipo dano(float d) {
        this.dano = d;
        return this;
    }

    /** Radio del golpe en área (0 = solo al que toca). */
    public ProyectilTipo area(float radio) {
        this.area = radio;
        return this;
    }

    public ProyectilTipo empuje(float e) {
        this.empuje = e;
        return this;
    }

    public ProyectilTipo temblor(float t) {
        this.temblor = t;
        return this;
    }

    public ProyectilTipo estado(Estado s, int ticks, int nivel) {
        this.estado = s;
        this.estadoTicks = ticks;
        this.estadoNivel = nivel;
        return this;
    }

    /** Sonido y partículas al pegar. */
    public ProyectilTipo impacto(String sonido, String particulas) {
        this.sonidoImpacto = sonido;
        this.particulasImpacto = particulas;
        return this;
    }

    public ProyectilTipo alLanzar(String sonido) {
        this.sonidoLanzar = sonido;
        return this;
    }
}
