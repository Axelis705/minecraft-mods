package com.axeliscore.client;

import com.axeliscore.fx.client.FxClient;
import net.minecraftforge.client.event.ViewportEvent;

/** Eventos del juego en el cliente (se registran a mano en {@link CoreClient}, igual en todas las versiones). */
public final class CoreClientEvents {
    private CoreClientEvents() {
    }

    static void register() {
        com.axeliscore.ui.client.Tooltips.init();
        //#if MC >= 26.1
        //$$ ViewportEvent.ComputeCameraAngles.BUS.addListener(CoreClientEvents::onCamera);
        //#else
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(CoreClientEvents::onCamera);
        //#endif
    }

    /** El temblor de cámara de {@link com.axeliscore.fx.Fx#shake}. */
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        float[] s = FxClient.cameraShake();
        if (s != null) {
            e.setYaw(e.getYaw() + s[0]);
            e.setPitch(e.getPitch() + s[1]);
            e.setRoll(e.getRoll() + s[2]);
        }
    }
}
