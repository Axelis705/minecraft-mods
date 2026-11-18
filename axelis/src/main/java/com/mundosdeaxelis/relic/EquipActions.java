package com.mundosdeaxelis.relic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

/** Acciones del equipo que el jugador pide con las teclas (se comprueban siempre en el servidor). */
public final class EquipActions {
    private EquipActions() {
    }

    /** Abre el menú de equipo. */
    public static void open(ServerPlayer sp) {
        Equipment.Data d = Equipment.get(sp);
        int np = Equipment.petSlots(sp), nr = Equipment.relicSlots(sp);
        NetworkHooks.openScreen(sp, new SimpleMenuProvider(
                (id, inv, p) -> new EquipMenu(id, inv, d.petContainer(), d.relicContainer(), d.capeContainer(), np, nr),
                Component.translatable("menu.mundos_de_axelis.equip")), buf -> {
            buf.writeVarInt(np);
            buf.writeVarInt(nr);
        });
    }

    /** Lanza la habilidad de la reliquia equipada en esa ranura. */
    public static void useRelic(ServerPlayer sp, int slot) {
        if (slot < 0 || slot >= Equipment.relicSlots(sp)) {
            return;
        }
        ItemStack stack = Equipment.get(sp).relics[slot];
        if (!(stack.getItem() instanceof RelicItem r) || r.isPet() || r.kind() != RelicItem.Kind.KEY) {
            return;
        }
        if (sp.getCooldowns().isOnCooldown(r)) {
            int left = Math.round(sp.getCooldowns().getCooldownPercent(r, 0.0F) * 100);
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.equip_cooldown", stack.getHoverName(), left)
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        r.useEquipped(sp, stack);
    }
}
