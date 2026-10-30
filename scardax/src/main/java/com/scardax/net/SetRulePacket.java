package com.scardax.net;

import com.scardax.rules.SpawnRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Cliente -> servidor: poner este porcentaje (0 a 300) a uno o varios mobs. Solo lo aceptan los operadores. */
public class SetRulePacket {
    public final List<ResourceLocation> ids;
    public final int percent;

    public SetRulePacket(List<ResourceLocation> ids, int percent) {
        this.ids = ids;
        this.percent = percent;
    }

    public static void encode(SetRulePacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.percent);
        buf.writeVarInt(m.ids.size());
        for (ResourceLocation id : m.ids) {
            buf.writeResourceLocation(id);
        }
    }

    public static SetRulePacket decode(FriendlyByteBuf buf) {
        int pct = buf.readVarInt();
        int n = Math.min(buf.readVarInt(), 4096);
        List<ResourceLocation> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ids.add(buf.readResourceLocation());
        }
        return new SetRulePacket(ids, pct);
    }

    public static void handle(SetRulePacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        c.enqueueWork(() -> {
            if (sp == null) {
                return;
            }
            if (!sp.hasPermissions(2)) {
                sp.sendSystemMessage(Component.translatable("scardax.msg.no_permission"));
                return;
            }
            SpawnRules rules = SpawnRules.of(sp.server);
            for (ResourceLocation id : m.ids) {
                if (ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
                    rules.set(id, m.percent);
                }
            }
            ScardaxNet.broadcast(sp.server, rules);
        });
        c.setPacketHandled(true);
    }
}
