package com.casino.plat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
//#if MC >= 1.20.5
//$$ import net.minecraft.core.HolderLookup;
//$$ import net.minecraft.nbt.NbtOps;
//$$ import net.minecraft.network.chat.ComponentSerialization;
//$$ import net.minecraft.resources.RegistryOps;
//$$ import net.minecraftforge.server.ServerLifecycleHooks;
//#endif

/** Guardar objetos y textos en NBT o en mensajes de red. Desde 1.20.5 hace falta el registro del mundo para hacerlo. */
public final class Ser {
    private Ser() {
    }

    //#if MC >= 1.20.5
    //$$ /** El registro del servidor (o, en el cliente, el del mundo que se está jugando). */
    //$$ public static HolderLookup.Provider reg() {
    //$$     var server = ServerLifecycleHooks.getCurrentServer();
    //$$     if (server != null) {
    //$$         return server.registryAccess();
    //$$     }
    //$$     return ClientReg.get();
    //$$ }
    //$$
    //$$ private static final class ClientReg {
    //$$     static HolderLookup.Provider get() {
    //$$         return net.minecraft.client.Minecraft.getInstance().level.registryAccess();
    //$$     }
    //$$ }
    //#endif

    public static Tag item(ItemStack s) {
        //#if MC >= 1.20.5
        //$$ return ItemStack.OPTIONAL_CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, reg()), s).result().orElse(new CompoundTag());
        //#else
        return s.save(new CompoundTag());
        //#endif
    }

    public static ItemStack item(Tag t) {
        if (t == null) {
            return ItemStack.EMPTY;
        }
        //#if MC >= 1.20.5
        //$$ return ItemStack.OPTIONAL_CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, reg()), t).result().orElse(ItemStack.EMPTY);
        //#else
        return t instanceof CompoundTag c ? ItemStack.of(c) : ItemStack.EMPTY;
        //#endif
    }

    public static Tag text(Component c) {
        //#if MC >= 1.20.5
        //$$ return ComponentSerialization.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, reg()), c).result().orElse(StringTag.valueOf(""));
        //#else
        return StringTag.valueOf(Component.Serializer.toJson(c));
        //#endif
    }

    public static Component text(Tag t) {
        if (t == null) {
            return Component.empty();
        }
        //#if MC >= 1.20.5
        //$$ return ComponentSerialization.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, reg()), t).result().orElse(Component.empty());
        //#else
        Component c = Component.Serializer.fromJson(t.getAsString());
        return c == null ? Component.empty() : c;
        //#endif
    }

    // ---- en mensajes de red (envueltos en un CompoundTag para que funcione igual en todas)

    public static void writeItem(FriendlyByteBuf b, ItemStack s) {
        CompoundTag w = new CompoundTag();
        w.put("v", item(s));
        b.writeNbt(w);
    }

    public static ItemStack readItem(FriendlyByteBuf b) {
        CompoundTag w = b.readNbt();
        return w == null ? ItemStack.EMPTY : item(w.get("v"));
    }

    public static void writeText(FriendlyByteBuf b, Component c) {
        CompoundTag w = new CompoundTag();
        w.put("v", text(c));
        b.writeNbt(w);
    }

    public static Component readText(FriendlyByteBuf b) {
        CompoundTag w = b.readNbt();
        return w == null ? Component.empty() : text(w.get("v"));
    }
}
