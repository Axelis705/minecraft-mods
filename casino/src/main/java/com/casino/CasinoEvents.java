package com.casino;

import com.casino.data.CasinoData;
import com.casino.game.Prize;
import com.casino.net.CasinoNet;
import com.casino.net.WheelWinPacket;
import net.minecraft.server.level.ServerPlayer;
import com.casino.block.CashierBlock;
import com.casino.block.LuckyWheelBlock;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Entrega los premios que quedaron pendientes si el jugador no estaba conectado cuando terminó el giro. */
@Mod.EventBusSubscriber(modid = Casino.MODID)
public final class CasinoEvents {
    private CasinoEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.getServer() != null) {
            List<Prize> list = CasinoData.get(p.getServer()).takePending(p.getUUID());
            for (Prize pz : list) {
                pz.deliver(p);
                CasinoNet.sendTo(p, new WheelWinPacket(pz));
            }
        }
    }

    /**
     * Agachado y con un objeto en la mano, Minecraft no usa el bloque (usa el objeto). Para cambiar el premio gordo de la ruleta
     * (agacharse + clic con el objeto) hay que permitir que el bloque sí se use.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        var block = e.getLevel().getBlockState(e.getPos()).getBlock();
        if (block instanceof LuckyWheelBlock || block instanceof CashierBlock) {
            e.setUseBlock(Event.Result.ALLOW);
        }
    }
}
