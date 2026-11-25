package com.axeliscore.forma.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.data.client.ClientData;
import com.axeliscore.forma.Formas;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * (Cliente) Dibuja a los jugadores que traen una forma ({@link Formas}): en lugar del jugador, el dibujo que registró el mod para esa
 * forma (por ejemplo un {@link FormaGeo}, un modelo de Blockbench con sus animaciones). En primera persona no se ven las manos del
 * jugador. Los gestos que pide el servidor ({@link Formas#accion}) llegan aquí y el dibujo los toca.
 */
public final class FormasCliente {
    /** Cómo se dibuja una forma. */
    public interface Dibujo {
        //#if MC < 1.20.5
        void dibujar(net.minecraft.client.player.AbstractClientPlayer p, Estado e, float pt, com.mojang.blaze3d.vertex.PoseStack ps,
                net.minecraft.client.renderer.MultiBufferSource buf, int luz);
        //#endif

        /** Si en primera persona se esconden las manos (por defecto sí). */
        default boolean sinManos() {
            return true;
        }
    }

    /** Lo que cada jugador transformado guarda en el cliente: su animación y el último gesto que pidió el servidor. */
    public static final class Estado {
        public final AnimHost host = new AnimHost();
        public String accion = "";
        /** El tick del mundo en que empezó el gesto (y si ya se tocó). */
        public long accionT = -1;
        public boolean accionNueva;
    }

    private static final Map<ResourceLocation, Dibujo> DIBUJOS = new HashMap<>();
    private static final Map<UUID, Estado> ESTADOS = new HashMap<>();
    private static boolean dibujando;

    private FormasCliente() {
    }

    public static void registrar(ResourceLocation forma, Dibujo d) {
        DIBUJOS.put(forma, d);
    }

    public static Estado estado(UUID id) {
        return ESTADOS.computeIfAbsent(id, k -> new Estado());
    }

    /** ¿Ese jugador se ve con una forma? */
    public static boolean transformado(Player p) {
        return p != null && Formas.de(p.getUUID(), true) != null;
    }

    public static void init() {
        ClientData.listen(Formas.KEY, t -> {
            java.util.Set<UUID> cambian = Formas.recibir(t);
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                for (UUID id : cambian) {
                    Player p = mc.level.getPlayerByUUID(id);
                    if (p != null) {
                        p.refreshDimensions();
                    }
                    ESTADOS.remove(id);
                }
            }
        });
        ClientData.listen(Formas.KEY_ACCION, t -> {
            if (!com.axeliscore.plat.Nbt.hasUuid(t, "id")) {
                return;
            }
            Estado e = estado(com.axeliscore.plat.Nbt.uuid(t, "id"));
            e.accion = com.axeliscore.plat.Nbt.s(t, "accion");
            e.accionT = com.axeliscore.plat.Nbt.l(t, "t");
            e.accionNueva = true;
        });
        //#if MC < 1.20.5
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(FormasCliente::antes);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(FormasCliente::manos);
        //#endif
    }

    //#if MC < 1.20.5
    /** El jugador transformado se dibuja con su forma (y no como jugador). */
    private static void antes(net.minecraftforge.client.event.RenderPlayerEvent.Pre e) {
        if (dibujando || !(e.getEntity() instanceof net.minecraft.client.player.AbstractClientPlayer p)) {
            return;
        }
        ResourceLocation f = Formas.de(p.getUUID(), true);
        Dibujo d = f == null ? null : DIBUJOS.get(f);
        if (d == null) {
            return;
        }
        e.setCanceled(true);
        dibujando = true;
        try {
            d.dibujar(p, estado(p.getUUID()), e.getPartialTick(), e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight());
        } finally {
            dibujando = false;
        }
    }

    /** En primera persona, transformado no se ven las manos de jugador. */
    private static void manos(net.minecraftforge.client.event.RenderHandEvent e) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation f = mc.player == null ? null : Formas.de(mc.player.getUUID(), true);
        Dibujo d = f == null ? null : DIBUJOS.get(f);
        if (d != null && d.sinManos()) {
            e.setCanceled(true);
        }
    }
    //#endif

    /** Para las pruebas: el estado que llegó de otro lado (como si lo hubiera mandado el servidor). */
    public static void recibirPrueba(CompoundTag t) {
        ClientData.receive(Formas.KEY, t);
    }
}
