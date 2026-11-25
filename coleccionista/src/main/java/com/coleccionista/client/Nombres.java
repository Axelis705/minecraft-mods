package com.coleccionista.client;

import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.coleccionista.Coleccionista;
import com.coleccionista.bot.PiezaBot;
import com.coleccionista.voz.VozPuente;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * (Cliente) Los nombres: nunca se ven arriba de la cabeza de nadie (jugadores ni bots; el nombre va en el pecho del uniforme), las piezas
 * no pueden abrir la lista de jugadores (Tab) y los fantasmas ven, debajo de su tarjeta, quiénes son los otros fantasmas, con una notita
 * (♪) al lado de quien está hablando.
 */
public final class Nombres {
    private static final String MOD = Coleccionista.MODID;
    /** Cuándo se oyó por última vez a cada quien (por el canal de los fantasmas). */
    private static final Map<UUID, Long> HABLO = new ConcurrentHashMap<>();
    private static volatile long yoHable;

    private Nombres() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((RenderNameTagEvent e) -> {
            if (e.getEntity() instanceof Player || e.getEntity() instanceof PiezaBot) {
                e.setResult(Event.Result.DENY);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RenderGuiOverlayEvent.Pre e) -> {
            if (e.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_LIST.id()) && EventoCliente.yo() != null) {
                e.setCanceled(true);
            }
        });
        com.axeliscore.fx.client.Hud.add(Nombres::fantasmas);
        // Simple Voice Chat (si está): quién habla por el canal de los fantasmas (cada canal es "de>a")
        VozPuente.alOirCanal = Nombres::oido;
        VozPuente.alHablarYo = () -> yoHable = System.currentTimeMillis();
    }

    private static void oido(UUID canal) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        String yo = mc.player.getUUID().toString();
        for (CompoundTag p : EventoCliente.piezas()) {
            if (!p.hasUUID("id")) {
                continue;
            }
            UUID id = p.getUUID("id");
            if (UUID.nameUUIDFromBytes((id + ">" + yo).getBytes(StandardCharsets.UTF_8)).equals(canal)) {
                HABLO.put(id, System.currentTimeMillis());
                return;
            }
        }
    }

    /** ¿Está hablando (se oyó hace menos de medio segundo)? */
    public static boolean hablando(UUID id) {
        Minecraft mc = Minecraft.getInstance();
        long ahora = System.currentTimeMillis();
        if (mc.player != null && id.equals(mc.player.getUUID())) {
            return ahora - yoHable < 450;
        }
        Long t = HABLO.get(id);
        return t != null && ahora - t < 450;
    }

    /** Debajo de mi tarjeta (si soy fantasma): los otros fantasmas, y quién habla. */
    private static void fantasmas(G g, int w, int h) {
        CompoundTag yo = EventoCliente.yo();
        if (yo == null || EventoCliente.vivo(yo) || !EventoCliente.activo() || !EventoHud.visible()) {
            return;
        }
        int n = 0;
        StringBuilder sb = new StringBuilder();
        for (CompoundTag p : EventoCliente.piezas()) {
            if (EventoCliente.vivo(p) || !p.hasUUID("id")) {
                continue;
            }
            n++;
        }
        if (n == 0) {
            return;
        }
        EventoHud.lado(g, Component.translatable(MOD + ".fantasmas.titulo", n).getString(), 0xFFB8C4D8, true);
        for (CompoundTag p : EventoCliente.piezas()) {
            if (EventoCliente.vivo(p) || !p.hasUUID("id")) {
                continue;
            }
            UUID id = p.getUUID("id");
            boolean habla = hablando(id);
            String nom = Nbt.s(p, "nombre");
            if (nom.length() > 16) {
                nom = nom.substring(0, 15) + "…";
            }
            EventoHud.lado(g, (habla ? "♪ " : "   ") + nom, habla ? 0xFFF0F6FF : 0xFF8A94A8, false);
        }
        sb.setLength(0);
    }
}
