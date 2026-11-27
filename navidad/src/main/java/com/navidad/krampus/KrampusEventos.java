package com.navidad.krampus;

import com.axeliscore.vestir.Vestuario;
import com.navidad.objeto.Ropa;
import com.navidad.entidad.Krampus;
import com.navidad.objeto.SacoKrampusItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Las reglas del saco del Krampus y del saco-mochila:
 * <ul>
 *     <li>Del saco del Krampus no te bajas agachándote (eso cuenta como forcejear).</li>
 *     <li>Adentro del saco no te llega más daño que el de sus apretones, y no puedes pegar ni usar cosas.</li>
 *     <li>Si sales del juego estando adentro, primero te saca (así el Krampus no se va contigo).</li>
 *     <li>Si traes el saco-mochila en el inventario, se ve en tu espalda.</li>
 * </ul>
 */
public final class KrampusEventos {
    public static final ResourceLocation PRENDA_SACO = Ropa.id("saco_krampus");

    private KrampusEventos() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(KrampusEventos::alMontar);
        MinecraftForge.EVENT_BUS.addListener(KrampusEventos::alAtacar);
        MinecraftForge.EVENT_BUS.addListener(KrampusEventos::alPegar);
        MinecraftForge.EVENT_BUS.addListener(KrampusEventos::alUsar);
        MinecraftForge.EVENT_BUS.addListener(KrampusEventos::alUsarBloque);
        MinecraftForge.EVENT_BUS.addListener(KrampusEventos::alSalir);
        MinecraftForge.EVENT_BUS.addListener(KrampusEventos::alTick);
    }

    private static Krampus sacoDe(Player p) {
        return p != null && p.getVehicle() instanceof Krampus k && k.ensacado(p.getUUID()) ? k : null;
    }

    static void alMontar(EntityMountEvent e) {
        if (e.isDismounting() && e.getEntityMounting() instanceof ServerPlayer p && e.getEntityBeingMounted() instanceof Krampus k
                && k.isAlive() && p.isAlive() && k.ensacado(p.getUUID())) {
            e.setCanceled(true);
            k.intentoBajarse(p);
        }
    }

    static void alAtacar(LivingAttackEvent e) {
        if (e.getEntity() instanceof Player p && !p.level().isClientSide && sacoDe(p) != null && !Krampus.apretando) {
            e.setCanceled(true);
        }
    }

    static void alPegar(AttackEntityEvent e) {
        if (sacoDe(e.getEntity()) != null || e.getEntity().getVehicle() instanceof Krampus) {
            e.setCanceled(true);
        }
    }

    static void alUsar(PlayerInteractEvent.RightClickItem e) {
        if (e.getEntity().getVehicle() instanceof Krampus) {
            e.setCanceled(true);
        }
    }

    static void alUsarBloque(PlayerInteractEvent.RightClickBlock e) {
        if (e.getEntity().getVehicle() instanceof Krampus) {
            e.setCanceled(true);
        }
    }

    static void alSalir(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.getVehicle() instanceof Krampus k) {
            k.forcejear(p, 99);
            if (p.getVehicle() == k) {
                p.stopRiding();
            }
        }
    }

    static void alTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 20 != 7) {
            return;
        }
        boolean trae = false;
        for (ItemStack st : p.getInventory().items) {
            if (st.getItem() instanceof SacoKrampusItem) {
                trae = true;
                break;
            }
        }
        trae |= p.getOffhandItem().getItem() instanceof SacoKrampusItem;
        boolean lo = Vestuario.tiene(p.server, p.getUUID(), PRENDA_SACO);
        if (trae && !lo) {
            Vestuario.poner(p.server, p.getUUID(), PRENDA_SACO, null);
        } else if (!trae && lo) {
            Vestuario.quitar(p.server, p.getUUID(), PRENDA_SACO);
        }
    }
}
