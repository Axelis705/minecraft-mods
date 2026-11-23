package com.axeliscore.npc;

import com.axeliscore.plat.Plat;
import com.axeliscore.ui.Rareza;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Una misión: objetivos, premios y reglas (diaria, repetible, qué misión pide antes, qué amistad hace falta). Se agrega a un
 * {@link NpcDef} con {@code .mision(...)} (entonces se acepta y se entrega con ese NPC) o se registra sola con
 * {@link Misiones#registrar} (se acepta desde el código y se completa sola). Textos: {@code mision.<mod>.<nombre>} (título) y
 * {@code mision.<mod>.<nombre>.desc} (qué cuenta el NPC).
 */
public final class Mision {
    /**
     * Un premio: lo que se ve en la lista y lo que se da. Los objetos se crean al usarlos (no al arrancar: en 1.20.5+ un objeto creado
     * antes de tiempo tira el juego).
     */
    public static final class Premio {
        private final String iconoId;
        private final int cuantos;
        private final java.util.function.Supplier<Component> texto;
        final Consumer<ServerPlayer> dar;

        public Premio(String iconoId, int cuantos, java.util.function.Supplier<Component> texto, Consumer<ServerPlayer> dar) {
            this.iconoId = iconoId;
            this.cuantos = cuantos;
            this.texto = texto;
            this.dar = dar;
        }

        public ItemStack icono() {
            return new ItemStack(Plat.item(iconoId), Math.max(1, cuantos));
        }

        public Component texto() {
            return texto.get();
        }

        public static Premio objeto(String id, int n) {
            return new Premio(id, n, () -> Component.literal(n + "× ").append(Objetivo.nombreObjeto(id)),
                    p -> Plat.give(p, new ItemStack(Plat.item(id), n)));
        }

        public static Premio esmeraldas(int n) {
            return objeto("minecraft:emerald", n);
        }

        public static Premio experiencia(int puntos) {
            return new Premio("minecraft:experience_bottle", 1, () -> Component.translatable("axelis_core.premio.xp", puntos),
                    p -> p.giveExperiencePoints(puntos));
        }

        /** Amistad con un NPC (además de la que se gana siempre con quien da la misión). */
        public static Premio amistad(String npc, int puntos) {
            return new Premio("minecraft:poppy", 1, () -> Component.translatable("axelis_core.premio.amistad", puntos),
                    p -> Amistad.sumar(p, npc, puntos));
        }
    }

    public final String id;
    String npc = "";
    final List<Objetivo> objetivos = new ArrayList<>();
    final List<Premio> premios = new ArrayList<>();
    boolean diaria, repetible;
    String requiere = "";
    int nivel, amistad = 5;
    Rareza rareza = Rareza.COMUN;

    public Mision(String modid, String nombre) {
        this.id = modid + ":" + nombre;
    }

    Mision deNpc(String npcId) {
        if (npc.isEmpty()) {
            npc = npcId;
        }
        return this;
    }

    public Mision objetivo(Objetivo o) {
        objetivos.add(o);
        return this;
    }

    public Mision premio(Premio p) {
        premios.add(p);
        return this;
    }

    /** Se puede repetir una vez al día. */
    public Mision diaria() {
        this.diaria = true;
        return this;
    }

    /** Se puede repetir cuando se quiera. */
    public Mision repetible() {
        this.repetible = true;
        return this;
    }

    /** Hay que haber hecho antes esa misión (para cadenas de misiones). */
    public Mision requiere(String misionId) {
        this.requiere = misionId;
        return this;
    }

    /** Nivel de amistad con el NPC que hace falta. */
    public Mision nivel(int n) {
        this.nivel = n;
        return this;
    }

    /** Amistad que da al entregarla (por defecto 5). */
    public Mision amistad(int puntos) {
        this.amistad = puntos;
        return this;
    }

    /** Color en las listas (común, raro, épico...). */
    public Mision rareza(Rareza r) {
        this.rareza = r;
        return this;
    }

    public Component titulo() {
        return Component.translatable("mision." + id.replace(':', '.'));
    }

    public Component descripcion() {
        return Component.translatable("mision." + id.replace(':', '.') + ".desc");
    }

    public String npc() {
        return npc;
    }

    public List<Objetivo> objetivos() {
        return objetivos;
    }

    public List<Premio> premios() {
        return premios;
    }

    public boolean esDiaria() {
        return diaria;
    }

    public Rareza rareza() {
        return rareza;
    }

    public int nivelPedido() {
        return nivel;
    }

    public String requerida() {
        return requiere;
    }
}
