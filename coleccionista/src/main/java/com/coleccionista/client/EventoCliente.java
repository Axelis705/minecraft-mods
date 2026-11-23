package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.coleccionista.evento.Evento;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** (Cliente) Lo que el servidor contó del evento, ya ordenado para el HUD, las escenas y los fantasmas. */
public final class EventoCliente {
    private static final Map<UUID, CompoundTag> PIEZAS = new LinkedHashMap<>();
    private static final Set<UUID> ORGANIZADORES = new HashSet<>();
    private static final Set<UUID> DISFRAZ = new HashSet<>();
    private static CompoundTag estado = new CompoundTag();
    private static long recibido;

    private EventoCliente() {
    }

    public static void init() {
        ClientData.listen(Evento.KEY, EventoCliente::recibir);
    }

    private static void recibir(CompoundTag t) {
        estado = t;
        recibido = System.currentTimeMillis();
        PIEZAS.clear();
        for (Tag x : t.getList("piezas", Tag.TAG_COMPOUND)) {
            CompoundTag p = (CompoundTag) x;
            PIEZAS.put(p.getUUID("id"), p);
        }
        ORGANIZADORES.clear();
        for (Tag x : t.getList("organizadores", Tag.TAG_COMPOUND)) {
            ORGANIZADORES.add(((CompoundTag) x).getUUID("id"));
        }
        DISFRAZ.clear();
        for (Tag x : t.getList("disfraz", Tag.TAG_COMPOUND)) {
            DISFRAZ.add(((CompoundTag) x).getUUID("id"));
        }
    }

    /** ¿Hay un evento armado (con al menos una pieza)? */
    public static boolean activo() {
        return !PIEZAS.isEmpty();
    }

    /** ¿Estamos en la sala de espera? */
    public static boolean sala() {
        return estado.getBoolean("sala");
    }

    public static String fase() {
        return Nbt.s(estado, "fase");
    }

    public static String prueba() {
        return Nbt.s(estado, "prueba");
    }

    public static int vivos() {
        return Nbt.i(estado, "vivos");
    }

    public static int total() {
        return Nbt.i(estado, "total");
    }

    /** Segundos que le quedan al reloj (-1: sin reloj). */
    public static int resta() {
        Minecraft mc = Minecraft.getInstance();
        if ("PAUSA".equals(fase())) {
            long r = Nbt.l(estado, "resta");
            return r > 0 ? (int) (r / 20) : -1;
        }
        long fin = Nbt.l(estado, "fin");
        if (fin <= 0 || mc.level == null) {
            return -1;
        }
        return (int) Math.max(0, (fin - mc.level.getGameTime()) / 20);
    }

    public static CompoundTag pieza(UUID id) {
        return id == null ? null : PIEZAS.get(id);
    }

    public static List<CompoundTag> piezas() {
        return new ArrayList<>(PIEZAS.values());
    }

    /** Mi pieza (o null si no participo). */
    public static CompoundTag yo() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : PIEZAS.get(mc.player.getUUID());
    }

    public static boolean vivo(CompoundTag p) {
        return p != null && "VIVO".equals(Nbt.s(p, "rol"));
    }

    /** ¿Está disfrazado del Coleccionista? */
    public static boolean disfrazado(UUID id) {
        return DISFRAZ.contains(id);
    }

    public static boolean esFantasma(UUID id) {
        CompoundTag p = PIEZAS.get(id);
        return p != null && !vivo(p);
    }

    public static boolean soyOrganizador() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && ORGANIZADORES.contains(mc.player.getUUID());
    }

    /** Mis compañeros de equipo o pareja (vivos, sin mí). */
    public static List<CompoundTag> companeros() {
        CompoundTag yo = yo();
        List<CompoundTag> r = new ArrayList<>();
        if (yo == null || !yo.contains("equipo") || yo.getInt("equipo") < 0) {
            return r;
        }
        int eq = yo.getInt("equipo");
        for (CompoundTag p : PIEZAS.values()) {
            if (p != yo && vivo(p) && p.getInt("equipo") == eq) {
                r.add(p);
            }
        }
        return r;
    }

    /** La piel de alguien (la de su cuenta si está conectado; si no, la de por defecto). */
    public static ResourceLocation piel(UUID id) {
        Minecraft mc = Minecraft.getInstance();
        PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(id);
        return info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id);
    }

    public static long recibido() {
        return recibido;
    }
}
