package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.voz.Voz;
import com.coleccionista.voz.VozPuente;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * (Cliente) La cinta en la boca: el servidor avisa quién la lleva; a ese jugador su micrófono le manda silencio ({@link VozPuente#yoMudo},
 * ver {@code VozPlugin}) y abajo le sale el aviso de que nadie lo oye (así no se confunde con el foquito de "hablando" del chat de voz).
 */
public final class MudoCliente {
    private MudoCliente() {
    }

    public static void init() {
        ClientData.listen(Voz.KEY_MUDO, t -> VozPuente.yoMudo = Nbt.b(t, "mudo"));
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> VozPuente.yoMudo = false);
        com.axeliscore.fx.client.Hud.add(MudoCliente::hud);
    }

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!VozPuente.yoMudo || mc.player == null || mc.options.hideGui) {
            return;
        }
        String s = Component.translatable(Coleccionista.MODID + ".mudo.aviso").getString();
        int tw = Ui.font().width(s), x = w / 2 - tw / 2, y = h - 62;
        float puls = 0.75F + 0.25F * (float) Math.sin(System.currentTimeMillis() / 300.0);
        g.fill(x - 6, y - 4, x + tw + 6, y + 12, Ui.alpha(0xC0100406, 1F));
        g.fill(x - 6, y - 4, x - 4, y + 12, EventoHud.ROJO);
        g.drawString(Ui.font(), s, x, y, Ui.alpha(EventoHud.ROJO_CL, puls), true);
    }
}
