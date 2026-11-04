package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> jugador: lo que lleva equipado (mascotas y reliquias) y cuántas ranuras tiene desbloqueadas. */
public class EquipSyncPacket {
    public final ItemStack[] pets;
    public final ItemStack[] relics;
    public final ItemStack cape;
    public final int petSlots;
    public final int relicSlots;

    public EquipSyncPacket(ItemStack[] pets, ItemStack[] relics, ItemStack cape, int petSlots, int relicSlots) {
        this.pets = pets.clone();
        this.relics = relics.clone();
        this.cape = cape;
        this.petSlots = petSlots;
        this.relicSlots = relicSlots;
    }

    public static void encode(EquipSyncPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.petSlots);
        buf.writeVarInt(m.relicSlots);
        buf.writeVarInt(m.pets.length);
        for (ItemStack s : m.pets) {
            buf.writeItem(s);
        }
        buf.writeVarInt(m.relics.length);
        for (ItemStack s : m.relics) {
            buf.writeItem(s);
        }
        buf.writeItem(m.cape);
    }

    public static EquipSyncPacket decode(FriendlyByteBuf buf) {
        int ps = buf.readVarInt(), rs = buf.readVarInt();
        ItemStack[] pets = new ItemStack[buf.readVarInt()];
        for (int i = 0; i < pets.length; i++) {
            pets[i] = buf.readItem();
        }
        ItemStack[] relics = new ItemStack[buf.readVarInt()];
        for (int i = 0; i < relics.length; i++) {
            relics[i] = buf.readItem();
        }
        ItemStack cape = buf.readItem();
        return new EquipSyncPacket(pets, relics, cape, ps, rs);
    }

    public static void handle(EquipSyncPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.equip(m)));
        c.setPacketHandled(true);
    }
}
