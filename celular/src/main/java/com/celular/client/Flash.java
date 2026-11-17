package com.celular.client;

import com.celular.PhoneItem;
import com.celular.PhoneUse;
import com.celular.net.CelularNet;
import com.celular.plat.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * La linterna y "qué se ve en mi celular": cada cuadro decide qué pantalla mostrar (la app abierta, la linterna o nada) y se la
 * dice al servidor solo cuando cambia. La linterna necesita el celular en la mano; si lo sueltas, se apaga.
 */
public final class Flash {
    /** Linterna encendida. */
    public static boolean on;
    private static int sent = -1;

    private Flash() {
    }

    /** ¿Tiene un celular en alguna mano? */
    public static boolean inHand(Minecraft mc) {
        return mc.player != null && (mc.player.getMainHandItem().getItem() instanceof PhoneItem
                || mc.player.getOffhandItem().getItem() instanceof PhoneItem);
    }

    public static void toggle() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!on && !inHand(mc)) {
            com.celular.plat.Plat.msg(mc.player, Component.translatable("celular.flash.need_hand"), true);
            return;
        }
        on = !on;
        ClientHooks.tap();
    }

    /** La pantalla que debe verse ahora en mi celular. */
    private static int screen(Minecraft mc) {
        Object sc = ClientHooks.screen();
        if (sc instanceof PhoneScreen ps) {
            return ps.useScreen();
        }
        if (sc instanceof CameraScreen) {
            return PhoneUse.CAMERA;
        }
        int st = ClientHooks.call.state;
        if (st == CelularNet.CallState.ACTIVE || st == CelularNet.CallState.DIALING) {
            return PhoneUse.EAR;                           // hablando por teléfono: el celular a la oreja
        }
        return on ? PhoneUse.FLASH : PhoneUse.OFF;
    }

    static void frame(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        ClientHooks.putAwayIfClosed();
        if (on && !inHand(mc) && !(ClientHooks.screen() instanceof PhoneScreen)) {
            on = false;                                    // soltó el celular (o lo cambió de mano a la barra)
            com.celular.plat.Plat.msg(mc.player, Component.translatable("celular.flash.off_msg"), true);
        }
        int want = screen(mc);
        if (sent == -1 && !Prefs.shareLocation) {
            // recién entró al mundo: el servidor no sabe que no quiere compartir su ubicación
            Net.toServer(new com.celular.net.AppNet.Req(com.celular.net.AppNet.Req.SHARE, null, 0, ""));
        }
        if (want != sent) {
            sent = want;
            Net.toServer(new CelularNet.UseScreen(want));
        }
    }

    static void reset() {
        on = false;
        sent = -1;
    }
}
