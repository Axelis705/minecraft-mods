package com.kensei.duel;

import com.kensei.Kensei;
import com.kensei.KenseiConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import com.kensei.entity.RivalEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Las reglas de un duelo: solo se pelea contra el equipo contrario y solo en la pelea; nadie muere de verdad (cae K.O.); y los avisos de conexión y apagado. */
@Mod.EventBusSubscriber(modid = Kensei.MODID)
public final class DuelEvents {
    private DuelEvents() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        DuelCommands.register(e.getDispatcher());
        com.kensei.entity.RivalCommands.register(e.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent e) {
        LivingEntity victim = e.getEntity();
        Duel d = DuelManager.fighterDuel(victim);
        if (d == null) {
            return;
        }
        Entity attacker = e.getSource().getEntity();
        Duel.Fighter vf = d.find(victim.getUUID());
        boolean enemy = false;
        if (attacker instanceof ServerPlayer || attacker instanceof RivalEntity) {
            Duel.Fighter af = d.find(attacker.getUUID());
            enemy = af != null && af.team != vf.team && !af.ko && !af.gone;
        }
        // fuera de la pelea, ya caído, de un aliado o de algo que no sea el rival: no pasa nada
        if (d.frozen(victim.getUUID()) || !enemy) {
            e.setCanceled(true);
        }
    }

    /** Si el golpe iba a matar, el peleador se queda con 1 de vida y cae K.O. (el último en aplicarse: ya con el daño final). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        Duel d = DuelManager.fighterDuel(victim);
        if (d == null || d.phase() != Duel.Phase.FIGHT) {
            return;
        }
        float hp = victim.getHealth();
        if (e.getAmount() >= hp) {
            e.setAmount(Math.max(0.0F, hp - 1.0F));
            d.onFatal(victim, e.getSource().getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        Duel d = DuelManager.fighterDuel(victim);
        if (d == null) {
            return;
        }
        e.setCanceled(true);
        victim.setHealth(1.0F);
        d.onFatal(victim, e.getSource().getEntity());
    }

    @SubscribeEvent
    public static void onTotem(LivingUseTotemEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && DuelManager.fighterDuel(p.getUUID()) != null) {
            e.setCanceled(true);
        }
    }

    /** Sin trampas: perlas, pociones, comida y frutas de chorus no se pueden usar durante el duelo. */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem e) {
        if (!KenseiConfig.DUEL_BLOCK_ITEMS.get() || !(e.getEntity() instanceof ServerPlayer p) || DuelManager.fighterDuel(p.getUUID()) == null) {
            return;
        }
        ItemStack st = e.getItemStack();
        if (st.isEdible() || st.getItem() instanceof PotionItem || st.is(Items.ENDER_PEARL) || st.is(Items.CHORUS_FRUIT)) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            DuelManager.tick(e.getServer());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            DuelManager.onLogout(p);
        }
    }

    /** Si alguien se desconectó en medio de un duelo, al volver a entrar se le devuelve a donde estaba. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && DuelBackup.has(p) && DuelManager.of(p.getUUID()) == null) {
            DuelBackup.restore(p, true);
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        DuelManager.abortAll();
    }
}
