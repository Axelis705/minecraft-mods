package com.axeliscore.forma;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Formas: un jugador se transforma en otra cosa (un monstruo, un animal, un personaje) con su propio modelo de Blockbench y sus
 * animaciones; el servidor decide quién trae qué forma y todos los clientes lo ven igual. La forma puede cambiar el tamaño del cuerpo
 * (choques y altura de los ojos) y lanzar gestos (rugir, olfatear, comer...) que se ven en todos los clientes.
 * <p>
 * Uso (servidor): {@code Formas.registrar(new Formas.Def(id, 0.9F, 2.4F, 2.15F))} en los dos lados al arrancar, luego
 * {@code Formas.poner(jugador, id)}, {@code Formas.accion(jugador, "rugir")} y {@code Formas.quitar(jugador)}. En el cliente, cada mod
 * dice cómo se dibuja su forma ({@code FormasCliente.registrar(id, new FormaGeo(...))}).
 */
public final class Formas {
    public static final String KEY = AxelisCore.MODID + ":formas";
    public static final String KEY_ACCION = AxelisCore.MODID + ":forma_accion";

    /** Lo que se sabe de una forma en los dos lados: el tamaño del cuerpo y la altura de los ojos (en bloques). */
    public record Def(ResourceLocation id, float ancho, float alto, float ojos) {
    }

    private static final Map<ResourceLocation, Def> DEFS = new HashMap<>();
    /** Quién trae qué forma: en el servidor, lo de verdad. */
    private static final Map<UUID, ResourceLocation> SERVIDOR = new HashMap<>();
    /** Lo que llegó al cliente. */
    private static final Map<UUID, ResourceLocation> CLIENTE = new HashMap<>();
    private static MinecraftServer servidor;

    private Formas() {
    }

    public static void init() {
        AxelisCore.onLogin(p -> {
            servidor = com.axeliscore.plat.Plat.server(p);
            PlayerStore.Sync.toPlayer(p, KEY, estado());
        });
        //#if MC < 1.20.5
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(Formas::tamano);
        //#endif
    }

    public static void registrar(Def d) {
        DEFS.put(d.id(), d);
    }

    public static Def def(ResourceLocation id) {
        return id == null ? null : DEFS.get(id);
    }

    /** La forma que trae ese jugador (null: la suya). Sirve en los dos lados. */
    public static ResourceLocation de(Player p) {
        return p == null ? null : (p.level().isClientSide() ? CLIENTE : SERVIDOR).get(p.getUUID());
    }

    public static ResourceLocation de(UUID id, boolean cliente) {
        return (cliente ? CLIENTE : SERVIDOR).get(id);
    }

    /** Lo transforma (o le cambia la forma). */
    public static void poner(ServerPlayer p, ResourceLocation forma) {
        servidor = com.axeliscore.plat.Plat.server(p);
        SERVIDOR.put(p.getUUID(), forma);
        p.refreshDimensions();
        enviar(com.axeliscore.plat.Plat.server(p));
        AxelisCore.LOGGER.info("AXCORE_FORMA {} -> {}", p.getGameProfile().getName(), forma);
    }

    /** Vuelve a su forma de siempre. */
    public static void quitar(ServerPlayer p) {
        if (SERVIDOR.remove(p.getUUID()) != null) {
            p.refreshDimensions();
            enviar(com.axeliscore.plat.Plat.server(p));
            AxelisCore.LOGGER.info("AXCORE_FORMA {} -> normal", p.getGameProfile().getName());
        }
    }

    /** Quita todas las formas (al reiniciar algo). */
    public static void quitarTodas(MinecraftServer s) {
        for (UUID id : new java.util.ArrayList<>(SERVIDOR.keySet())) {
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            SERVIDOR.remove(id);
            if (p != null) {
                p.refreshDimensions();
            }
        }
        enviar(s);
    }

    /** Un gesto de la forma (el nombre de una animación: "rugir", "comer"...): lo ven todos los clientes. */
    public static void accion(ServerPlayer p, String accion) {
        CompoundTag t = new CompoundTag();
        com.axeliscore.plat.Nbt.putUuid(t, "id", p.getUUID());
        t.putString("accion", accion);
        t.putLong("t", p.level().getGameTime());
        PlayerStore.Sync.toAll(com.axeliscore.plat.Plat.server(p), KEY_ACCION, t);
    }

    private static CompoundTag estado() {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        for (Map.Entry<UUID, ResourceLocation> e : SERVIDOR.entrySet()) {
            CompoundTag x = new CompoundTag();
            com.axeliscore.plat.Nbt.putUuid(x, "id", e.getKey());
            x.putString("forma", e.getValue().toString());
            l.add(x);
        }
        t.put("formas", l);
        return t;
    }

    private static void enviar(MinecraftServer s) {
        if (s != null) {
            PlayerStore.Sync.toAll(s, KEY, estado());
        }
    }

    /** (Cliente) Llegó quién trae qué forma. Devuelve los que cambiaron. */
    public static java.util.Set<UUID> recibir(CompoundTag t) {
        Map<UUID, ResourceLocation> nuevo = new HashMap<>();
        ListTag lista = com.axeliscore.plat.Nbt.list(t, "formas");
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag x = com.axeliscore.plat.Nbt.at(lista, i);
            if (com.axeliscore.plat.Nbt.hasUuid(x, "id")) {
                ResourceLocation r = ResourceLocation.tryParse(com.axeliscore.plat.Nbt.s(x, "forma"));
                if (r != null) {
                    nuevo.put(com.axeliscore.plat.Nbt.uuid(x, "id"), r);
                }
            }
        }
        java.util.Set<UUID> cambian = new java.util.HashSet<>(CLIENTE.keySet());
        cambian.addAll(nuevo.keySet());
        cambian.removeIf(id -> java.util.Objects.equals(CLIENTE.get(id), nuevo.get(id)));
        CLIENTE.clear();
        CLIENTE.putAll(nuevo);
        return cambian;
    }

    //#if MC < 1.20.5
    /** El tamaño del cuerpo de quien trae una forma (con su altura de ojos), en cualquier postura. */
    private static void tamano(net.minecraftforge.event.entity.EntityEvent.Size e) {
        if (!(e.getEntity() instanceof Player p)) {
            return;
        }
        Def d = def(de(p));
        if (d == null) {
            return;
        }
        e.setNewSize(net.minecraft.world.entity.EntityDimensions.scalable(d.ancho(), d.alto()), false);
        e.setNewEyeHeight(d.ojos());
    }
    //#endif
}
