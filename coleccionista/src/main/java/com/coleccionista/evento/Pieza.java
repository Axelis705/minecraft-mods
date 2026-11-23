package com.coleccionista.evento;

import com.axeliscore.plat.Nbt;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Un participante del evento. El Coleccionista los llama "piezas" (de su colección): cada uno tiene su número (Pieza 07), su estado
 * (vivo o fantasma), sus fichas de vida y su equipo. Los bots de prueba también son piezas.
 */
public final class Pieza {
    public enum Rol {
        VIVO, FANTASMA
    }

    public final UUID id;
    public String nombre;
    public int numero;
    public Rol rol = Rol.VIVO;
    /** Fichas de vida: cada una salva de una eliminación. */
    public int fichas = 1;
    /** Equipo o pareja actual (-1: sin equipo). */
    public int equipo = -1;
    /** Por qué quedó fuera (nombre de la prueba o lo que escribió el organizador). */
    public String motivo = "";
    /** Lugar en la colección (1 = el primero eliminado; 0 = sigue vivo). */
    public int orden;
    /** Jugador de prueba (no es una persona). */
    public boolean bot;

    public Pieza(UUID id, String nombre, int numero, boolean bot) {
        this.id = id;
        this.nombre = nombre;
        this.numero = numero;
        this.bot = bot;
    }

    public boolean vivo() {
        return rol == Rol.VIVO;
    }

    /** "Pieza 07". */
    public String etiqueta() {
        return String.format("%02d", numero);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putString("nombre", nombre);
        t.putInt("numero", numero);
        t.putString("rol", rol.name());
        t.putInt("fichas", fichas);
        t.putInt("equipo", equipo);
        t.putString("motivo", motivo);
        t.putInt("orden", orden);
        t.putBoolean("bot", bot);
        return t;
    }

    public static Pieza load(CompoundTag t) {
        Pieza p = new Pieza(t.getUUID("id"), Nbt.s(t, "nombre"), Nbt.i(t, "numero"), t.getBoolean("bot"));
        try {
            p.rol = Rol.valueOf(Nbt.s(t, "rol"));
        } catch (IllegalArgumentException e) {
            p.rol = Rol.VIVO;
        }
        p.fichas = Nbt.i(t, "fichas");
        p.equipo = t.contains("equipo") ? t.getInt("equipo") : -1;
        p.motivo = Nbt.s(t, "motivo");
        p.orden = Nbt.i(t, "orden");
        return p;
    }
}
