package com.navidad.nieve;

import com.axeliscore.data.Actions;
import com.axeliscore.ui.Teclas;
import com.navidad.Navidad;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** El lanzanieves y su tanque: la tecla para abrir el tanque que traes puesto (K, se puede cambiar en Controles). */
public final class Nieve {
    public static final Teclas.Tecla TECLA = Teclas.nueva(Navidad.MODID, "tanque", 'K',
            () -> com.axeliscore.data.client.ClientData.send(Navidad.MODID + ":tanque_abrir"));

    private Nieve() {
    }

    public static void init() {
        Actions.on(Navidad.MODID + ":tanque_abrir", (p, d) -> {
            if (Municion.tanque(p).isEmpty()) {
                p.displayClientMessage(Component.translatable("navidad.tanque.no_traes").withStyle(ChatFormatting.AQUA), true);
                return;
            }
            TanqueMenu.abrir(p, TanqueMenu.ESPALDA);
        });
    }
}
