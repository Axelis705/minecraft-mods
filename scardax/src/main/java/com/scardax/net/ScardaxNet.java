package com.scardax.net;

import com.scardax.Scardax;
import com.scardax.rules.SpawnRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;

/**
 * Canal de red de Scardax. El mod funciona aunque solo esté en el servidor (los jugadores sin el mod entran sin problema); quien quiera
 * abrir la pantalla necesita tenerlo también en su juego. Servidor -> cliente: abrir la pantalla y actualizar reglas. Cliente -> servidor: cambiar reglas y limpiar.
 */
public final class ScardaxNet {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Scardax.MODID, "main"),
            () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL),
            NetworkRegistry.acceptMissingOr(PROTOCOL));

    private static boolean registered = false;

    private ScardaxNet() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(id++, RulesPacket.class, RulesPacket::encode, RulesPacket::decode, RulesPacket::handle);
        CHANNEL.registerMessage(id++, SetRulePacket.class, SetRulePacket::encode, SetRulePacket::decode, SetRulePacket::handle);
        CHANNEL.registerMessage(id++, PurgePacket.class, PurgePacket::encode, PurgePacket::decode, PurgePacket::handle);
        CHANNEL.registerMessage(id++, StrictPacket.class, StrictPacket::encode, StrictPacket::decode, StrictPacket::handle);
    }

    public static void sendTo(ServerPlayer p, RulesPacket pkt) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
    }

    /** Manda las reglas actuales a todos los que pueden usar el comando (los que tienen la pantalla abierta las verán al instante). */
    public static void broadcast(MinecraftServer server, SpawnRules rules) {
        Map<ResourceLocation, Integer> all = rules.all();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.hasPermissions(2)) {
                sendTo(p, new RulesPacket(false, rules.strict(), all));
            }
        }
    }

    static void writeRules(FriendlyByteBuf buf, Map<ResourceLocation, Integer> rules) {
        buf.writeVarInt(rules.size());
        for (Map.Entry<ResourceLocation, Integer> e : rules.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeVarInt(e.getValue());
        }
    }

    static Map<ResourceLocation, Integer> readRules(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Map<ResourceLocation, Integer> out = new HashMap<>();
        for (int i = 0; i < n; i++) {
            ResourceLocation id = buf.readResourceLocation();
            out.put(id, buf.readVarInt());
        }
        return out;
    }
}
