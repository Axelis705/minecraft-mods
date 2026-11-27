package com.navidad.client;

import com.navidad.Navidad;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** (Cliente) Eventos del juego del lado del cliente: quién se está dibujando (para animar el lanzanieves en la mano de cada quien). */
@Mod.EventBusSubscriber(modid = Navidad.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class EventosCliente {
    private EventosCliente() {
    }

    @SubscribeEvent
    public static void antes(RenderLivingEvent.Pre<?, ?> e) {
        LanzanievesObjeto.portador = e.getEntity();
    }

    @SubscribeEvent
    public static void despues(RenderLivingEvent.Post<?, ?> e) {
        LanzanievesObjeto.portador = null;
    }
}
