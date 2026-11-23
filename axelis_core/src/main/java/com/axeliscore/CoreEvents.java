package com.axeliscore;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Eventos del juego que el Core escucha para todos los mods (por ahora: mandar los datos de cada jugador al entrar). */
//#if MC >= 26.1
//$$ @Mod.EventBusSubscriber(modid = AxelisCore.MODID)
//#endif
public final class CoreEvents {
    private CoreEvents() {
    }

    /** Lo que los mods quieren hacer cuando entra un jugador ({@link com.axeliscore.AxelisCore#onLogin}). */
    static final java.util.List<java.util.function.Consumer<ServerPlayer>> LOGIN = new java.util.concurrent.CopyOnWriteArrayList<>();

    private static boolean avisoRomper;

    /** Lo que los mods quieren hacer cuando un jugador rompe un bloque ({@link com.axeliscore.AxelisCore#onBreak}). */
    static final java.util.List<AxelisCore.BreakHandler> BREAK = new java.util.concurrent.CopyOnWriteArrayList<>();

    //#if MC >= 26.1
    //$$ @SubscribeEvent(priority = net.minecraftforge.eventbus.api.listener.Priority.LOWEST)
    //$$ public static void onBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent e) {
    //$$     // en 26.x Forge decide con el resultado: DENY = no se rompe
    //$$     if (e.getResult() == net.minecraftforge.common.util.Result.DENY) {
    //$$         return;
    //$$     }
    //#else
    public static void onBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent e) {
    //#endif
        if (!avisoRomper) {
            avisoRomper = true;
            AxelisCore.LOGGER.info("AXCORE_ROMPER primer bloque roto (mods que escuchan: {})", BREAK.size());
        }
        if (BREAK.isEmpty() || !(e.getPlayer() instanceof ServerPlayer p) || !(e.getLevel() instanceof net.minecraft.server.level.ServerLevel l)) {
            return;
        }
        for (var h : BREAK) {
            try {
                h.broke(p, l, e.getPos(), e.getState());
            } catch (RuntimeException ex) {
                AxelisCore.LOGGER.error("Axelis Core: falló algo al romper un bloque", ex);
            }
        }
    }

    /** Lo que los mods quieren hacer en cada tick del servidor ({@link com.axeliscore.AxelisCore#onServerTick}). */
    static final java.util.List<java.util.function.Consumer<net.minecraft.server.MinecraftServer>> TICK = new java.util.concurrent.CopyOnWriteArrayList<>();

    //#if MC >= 26.1
    //$$ @SubscribeEvent
    //$$ public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent.Post e) {
    //$$     serverTick(e.server());
    //$$ }
    //#elif MC >= 1.21
    //$$ public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent.Post e) {
    //$$     serverTick(e.getServer());
    //$$ }
    //#else
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent e) {
        if (e.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            serverTick(e.getServer());
        }
    }
    //#endif

    private static void serverTick(net.minecraft.server.MinecraftServer s) {
        for (var c : TICK) {
            try {
                c.accept(s);
            } catch (RuntimeException ex) {
                AxelisCore.LOGGER.error("Axelis Core: falló algo en el tick del servidor", ex);
            }
        }
    }

    /** El daño que recibe algo: los estados (marcado) y la entrada de fase de los jefes lo cambian. */
    //#if MC >= 26.1
    //$$ @SubscribeEvent
    //#endif
    public static void onHurt(net.minecraftforge.event.entity.living.LivingHurtEvent e) {
        net.minecraft.world.entity.LivingEntity ent = e.getEntity();
        if (ent == null || ent.level().isClientSide()) {
            return;
        }
        float a = e.getAmount(), b = a;
        if (ent instanceof com.axeliscore.combat.Jefe j) {
            b = j.alRecibirDano(b);
        }
        b = com.axeliscore.combat.Estados.alRecibirDano(ent, b);
        if (b != a) {
            e.setAmount(b);
        }
    }

    /** Algo murió: si lo derrotó un jugador, cuenta para sus misiones de cazar. */
    //#if MC >= 26.1
    //$$ @SubscribeEvent
    //#endif
    public static void onDeath(net.minecraftforge.event.entity.living.LivingDeathEvent e) {
        net.minecraft.world.entity.LivingEntity ent = e.getEntity();
        if (ent == null || ent.level().isClientSide() || ent instanceof ServerPlayer) {
            return;
        }
        if (e.getSource().getEntity() instanceof ServerPlayer p) {
            try {
                com.axeliscore.npc.Misiones.alDerrotar(p, ent);
            } catch (RuntimeException ex) {
                AxelisCore.LOGGER.error("Axelis Core: falló contar una criatura derrotada", ex);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            com.axeliscore.data.PlayerStore.onLogin(p);
            com.axeliscore.combat.Estados.alEntrar(p);
            for (var l : LOGIN) {
                try {
                    l.accept(p);
                } catch (RuntimeException ex) {
                    AxelisCore.LOGGER.error("Axelis Core: falló algo al entrar {}", p.getGameProfile().getName(), ex);
                }
            }
        }
    }
}
