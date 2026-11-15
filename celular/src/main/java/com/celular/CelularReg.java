package com.celular;

import com.celular.plat.Ids;
import com.celular.plat.Reg;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Objetos, sonidos y la pestaña del modo creativo. */
public final class CelularReg {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Celular.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Celular.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Celular.MODID);

    /** Colores de la carcasa (el primero es el celular "normal"). */
    public static final String[] COLORS = {"black", "white", "pink", "blue", "green", "purple", "gold", "red"};
    public static final List<RegistryObject<Item>> PHONES = new ArrayList<>();

    static {
        for (int i = 0; i < COLORS.length; i++) {
            String name = i == 0 ? "phone" : "phone_" + COLORS[i];
            int color = i;
            PHONES.add(ITEMS.register(name, () -> new PhoneItem(Reg.item(name).stacksTo(1), color)));
        }
    }

    public static final RegistryObject<SoundEvent> SHUTTER = sound("shutter");
    public static final RegistryObject<SoundEvent> RINGTONE = sound("ringtone");
    public static final RegistryObject<SoundEvent> RINGBACK = sound("ringback");
    public static final RegistryObject<SoundEvent> NOTIFY = sound("notify");
    public static final RegistryObject<SoundEvent> LIKE = sound("like");
    public static final RegistryObject<SoundEvent> HANGUP = sound("hangup");
    public static final RegistryObject<SoundEvent> TAP = sound("tap");
    public static final RegistryObject<SoundEvent> UNLOCK = sound("unlock");

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.celular"))
            .icon(() -> new ItemStack(PHONES.get(0).get()))
            .displayItems((params, out) -> {
                for (var p : PHONES) {
                    out.accept(p.get());
                }
            })
            .build());

    private CelularReg() {
    }

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Ids.mod(name)));
    }

    /** ¿Lleva un celular en el inventario (o en la mano)? Devuelve el color de la carcasa o -1. */
    public static int phoneColor(net.minecraft.world.entity.player.Player p) {
        for (ItemStack s : com.celular.plat.Plat.mainAndOffhand(p.getInventory())) {
            if (s.getItem() instanceof PhoneItem ph) {
                return ph.color;
            }
        }
        return -1;
    }
}
