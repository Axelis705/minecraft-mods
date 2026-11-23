package com.axeliscore.plat;

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
        if (p instanceof net.minecraft.server.level.ServerPlayer sp && sp.connection == null) {
            return; // jugador de prueba (no conectado)
        }
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

    /** Da un objeto al jugador; si no cabe, lo deja caer a sus pies (listo para recoger). */
    public static void give(Player p, ItemStack st) {
        if (st == null || st.isEmpty()) {
            return;
        }
        if (!p.getInventory().add(st) && !st.isEmpty()) {
            net.minecraft.world.entity.item.ItemEntity e = new net.minecraft.world.entity.item.ItemEntity(p.level(), p.getX(), p.getY() + 0.5,
                    p.getZ(), st);
            e.setNoPickUpDelay();
            p.level().addFreshEntity(e);
        }
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
    /**
     * Hora del día (ticks desde el amanecer, contando días). En 26.x Minecraft cambió los relojes del mundo; ahí se usa el reloj del
     * juego (el ciclo dura lo mismo, pero no se adelanta al dormir).
     */
    public static long dayTime(net.minecraft.world.level.Level level) {
        //#if MC >= 26.1
        //$$ return level.getGameTime();
        //#else
        return level.getDayTime();
        //#endif
    }

    /**
     * Que una criatura camine hasta una entidad o un punto. (Los espacios después del punto son a propósito: tools/mcver.py cambia el
     * moveTo de las entidades por snapTo en 26.x, pero el de la navegación no cambió.)
     */
    public static boolean walkTo(net.minecraft.world.entity.Mob m, Entity target, double speed) {
        return m.getNavigation(). moveTo(target, speed);
    }

    public static boolean walkTo(net.minecraft.world.entity.Mob m, double x, double y, double z, double speed) {
        return m.getNavigation(). moveTo(x, y, z, speed);
    }

    /** Empuja una entidad (se suma a su velocidad) y, si es un jugador, se lo avisa a su cliente para que se mueva de verdad. */
    public static void push(Entity e, double x, double y, double z) {
        e.setDeltaMovement(e.getDeltaMovement().add(x, y, z));
        if (e instanceof net.minecraft.server.level.ServerPlayer sp && sp.connection != null) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
        }
    }

    /** Un objeto por su id ("minecraft:fishing_rod"); aire si no existe. */
    public static net.minecraft.world.item.Item item(String id) {
        try {
            //#if MC >= 1.21.2
            //$$ return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Ids.parse(id));
            //#else
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Ids.parse(id));
            //#endif
        } catch (RuntimeException e) {
            return net.minecraft.world.item.Items.AIR;
        }
    }

    /** El id de un objeto ("minecraft:fishing_rod"). */
    public static String itemId(net.minecraft.world.item.Item it) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it).toString();
    }

    /** Daño a una entidad (en 1.21.2+ el daño se hace con el mundo del servidor). Devuelve true si le hizo daño. */
    public static boolean hurt(Entity target, net.minecraft.world.damagesource.DamageSource src, float amount) {
        //#if MC >= 1.21.2
        //$$ return target.level() instanceof net.minecraft.server.level.ServerLevel sl && target.hurtServer(sl, src, amount);
        //#else
        return target.hurt(src, amount);
        //#endif
    }

    /**
     * Le quita los "ticks de invulnerabilidad" que deja cada golpe (para que un daño periódico o un aviso sí peguen aunque otro golpe
     * acabe de llegar).
     */
    public static void sinInvulnerable(Entity e) {
        //#if MC >= 26.1
        //$$ e.setInvulnerableTime(0);
        //#else
        e.invulnerableTime = 0;
        //#endif
    }

    public static void invulnerable(Entity e, boolean v) {
        //#if MC >= 26.3
        //$$ e.setPermanentlyInvulnerable(v);
        //#else
        e.setInvulnerable(v);
        //#endif
    }

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
