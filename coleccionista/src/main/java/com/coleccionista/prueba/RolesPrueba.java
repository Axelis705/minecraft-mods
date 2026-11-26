package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import com.coleccionista.voz.VozPuente;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Para probar los papeles del taller de los sentidos sin jugar el capítulo: {@code /evento probar venda|cinta|esposas|nada [jugadores]}.
 * La venda tapa la vista (como en el juego), la cinta corta el micrófono y el chat, y las esposas no dejan tocar nada. Se ven puestas
 * (la venda en los ojos, la cinta en la boca, las esposas en las muñecas) para todos.
 */
public final class RolesPrueba {
    public static final String KEY = Coleccionista.MODID + ":roles_prueba";
    private static final String MOD = Coleccionista.MODID;
    private static final Map<UUID, String> ROLES = new LinkedHashMap<>();

    private RolesPrueba() {
    }

    public static void init() {
        AxelisCore.onLogin(p -> PlayerStore.Sync.toPlayer(p, KEY, estado()));
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> manos(e));
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock e) -> manos(e));
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> manos(e));
        MinecraftForge.EVENT_BUS.addListener((AttackEntityEvent e) -> {
            if ("ESPOSAS".equals(ROLES.get(e.getEntity().getUUID()))) {
                e.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ServerChatEvent e) -> {
            if ("CINTA".equals(ROLES.get(e.getPlayer().getUUID()))) {
                e.setCanceled(true);
                Plat.msg(e.getPlayer(), Component.translatable(MOD + ".tres.no_hablar").withStyle(ChatFormatting.DARK_GRAY), true);
            }
        });
    }

    private static void manos(PlayerInteractEvent e) {
        if (!e.getLevel().isClientSide() && "ESPOSAS".equals(ROLES.get(e.getEntity().getUUID())) && e.isCancelable()) {
            e.setCanceled(true);
        }
    }

    /** Le pone (o quita, con "NADA") un papel a ese jugador. */
    public static void poner(MinecraftServer s, ServerPlayer p, String rol) {
        if (rol == null || "NADA".equals(rol)) {
            ROLES.remove(p.getUUID());
        } else {
            ROLES.put(p.getUUID(), rol);
        }
        Set<UUID> mudos = new HashSet<>();
        for (Map.Entry<UUID, String> e : ROLES.entrySet()) {
            if ("CINTA".equals(e.getValue())) {
                mudos.add(e.getKey());
            }
        }
        VozPuente.mudosPrueba = Set.copyOf(mudos);
        com.coleccionista.voz.Voz.avisarMudos();
        PlayerStore.Sync.toAll(s, KEY, estado());
        Coleccionista.LOGGER.info("COLECCIONISTA_ROL_PRUEBA {} {}", p.getGameProfile().getName(), rol);
    }

    public static CompoundTag estado() {
        CompoundTag t = new CompoundTag();
        for (Map.Entry<UUID, String> e : ROLES.entrySet()) {
            t.putString(e.getKey().toString(), e.getValue());
        }
        return t;
    }
}
