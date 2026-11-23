package com.axeliscore.data.client;

import com.axeliscore.net.CoreNet;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * (Cliente) Los datos que mandó el servidor, por clave, para mostrarlos en pantallas. Se olvidan solos al cambiar de mundo o de
 * servidor.
 */
public final class ClientData {
    private record Entry(Object conn, CompoundTag tag) {
    }

    private static final Map<String, Entry> DATA = new ConcurrentHashMap<>();
    private static final Map<String, List<Consumer<CompoundTag>>> LISTENERS = new ConcurrentHashMap<>();

    private ClientData() {
    }

    /** Los datos con esta clave (vacío si no llegaron todavía). No cambiarlos: son solo para leer. */
    public static CompoundTag get(String key) {
        Entry e = DATA.get(key);
        if (e == null || e.conn != Minecraft.getInstance().getConnection()) {
            return new CompoundTag();
        }
        return e.tag;
    }

    /** Los datos del jugador para un mod ({@link com.axeliscore.data.PlayerStore}). */
    public static CompoundTag player(String modid) {
        return get(com.axeliscore.data.PlayerStore.key(modid));
    }

    /** ¿Ya llegaron datos con esta clave en este mundo? */
    public static boolean has(String key) {
        Entry e = DATA.get(key);
        return e != null && e.conn == Minecraft.getInstance().getConnection();
    }

    /** Avisar cuando lleguen datos nuevos con esta clave (por ejemplo, para refrescar una pantalla abierta). */
    public static void listen(String key, Consumer<CompoundTag> l) {
        LISTENERS.computeIfAbsent(key, k -> new ArrayList<>()).add(l);
    }

    /** Pedirle algo al servidor ({@link com.axeliscore.data.Actions}). */
    public static void send(String key, CompoundTag data) {
        CoreNet.NET.toServer(new CoreNet.ActionMsg(key, data));
    }

    public static void send(String key) {
        send(key, new CompoundTag());
    }

    /** (Core) Llegaron datos. */
    public static void receive(String key, CompoundTag tag) {
        DATA.put(key, new Entry(Minecraft.getInstance().getConnection(), tag));
        List<Consumer<CompoundTag>> ls = LISTENERS.get(key);
        if (ls != null) {
            for (Consumer<CompoundTag> l : ls) {
                l.accept(tag);
            }
        }
    }
}
