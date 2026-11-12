package com.casino.plat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;

/** Cosas pequeñas que cambiaron entre versiones. */
public final class Plat {
    private Plat() {
    }

    /** Éxito (en 1.21.2+ ya no existe sidedSuccess: SUCCESS sirve en los dos lados). */
    public static InteractionResult ok(boolean clientSide) {
        //#if MC >= 1.21.2
        //$$ return InteractionResult.SUCCESS;
        //#else
        return InteractionResult.sidedSuccess(clientSide);
        //#endif
    }

    public static <T extends Entity> T create(EntityType<T> type, Level level) {
        //#if MC >= 1.21.2
        //$$ return type.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        //#else
        return type.create(level);
        //#endif
    }

    /** Altura mínima del mundo (incluida). */
    public static int minY(LevelHeightAccessor l) {
        //#if MC >= 1.21.2
        //$$ return l.getMinY();
        //#else
        return l.getMinBuildHeight();
        //#endif
    }

    /** Altura máxima del mundo (excluida, como el viejo getMaxBuildHeight). */
    public static int maxY(LevelHeightAccessor l) {
        //#if MC >= 1.21.2
        //$$ return l.getMaxY() + 1;
        //#else
        return l.getMaxBuildHeight();
        //#endif
    }

    /** Mensaje al jugador: arriba de la barra (overlay) o en el chat. */
    public static void msg(Player p, Component c, boolean overlay) {
        //#if MC >= 26.1
        //$$ if (overlay) {
        //$$     p.sendOverlayMessage(c);
        //$$ } else {
        //$$     p.sendSystemMessage(c);
        //$$ }
        //#else
        p.displayClientMessage(c, overlay);
        //#endif
    }

    /** El servidor de una entidad (null en el cliente). */
    public static MinecraftServer server(Entity e) {
        //#if MC >= 26.1
        //$$ return e.level() instanceof net.minecraft.server.level.ServerLevel sl ? sl.getServer() : null;
        //#else
        return e.getServer();
        //#endif
    }

    /** ¿Es operador (nivel 2, como /give)? */
    public static boolean isOp(Player p) {
        //#if MC >= 26.1
        //$$ return p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        //#else
        return p.hasPermissions(2);
        //#endif
    }

    public static boolean isOp(CommandSourceStack s) {
        //#if MC >= 26.1
        //$$ return s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        //#else
        return s.hasPermission(2);
        //#endif
    }

    /** Inventario principal (sin armadura), en orden. Los cambios en las pilas se aplican al inventario. */
    public static List<ItemStack> mainItems(Inventory inv) {
        //#if MC >= 1.21.5
        //$$ return inv.getNonEquipmentItems();
        //#else
        return inv.items;
        //#endif
    }

    /** Inventario principal + mano izquierda. */
    public static List<ItemStack> mainAndOffhand(Inventory inv) {
        List<ItemStack> all = new ArrayList<>(mainItems(inv));
        //#if MC >= 1.21.5
        //$$ all.add(inv.getItem(Inventory.SLOT_OFFHAND));
        //#else
        all.addAll(inv.offhand);
        //#endif
        return all;
    }

    /** Mueve el brazo derecho (y lo ven los demás). */
    public static void swing(LivingEntity e) {
        //#if MC >= 26.3
        //$$ e.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        //#else
        e.swing(InteractionHand.MAIN_HAND, true);
        //#endif
    }

    /** No recibe daño nunca. */
    public static void invulnerable(Entity e) {
        //#if MC >= 26.3
        //$$ e.setPermanentlyInvulnerable(true);
        //#else
        e.setInvulnerable(true);
        //#endif
    }

    /** ¿Es el dueño de la partida de un jugador (mundo propio o LAN)? */
    public static boolean isHost(Player p) {
        MinecraftServer s = server(p);
        //#if MC >= 26.1
        //$$ return s != null && s.isSingleplayerOwner(p.nameAndId());
        //#else
        return s != null && s.isSingleplayerOwner(p.getGameProfile());
        //#endif
    }
}
