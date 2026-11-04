package com.mundosdeaxelis.block;

import com.mundosdeaxelis.grave.GraveEvents;
import com.mundosdeaxelis.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Lo que guarda una lápida: los objetos del jugador (con su ranura original) y cuándo caduca. */
public class GraveBlockEntity extends BlockEntity {
    /** 15 minutos. */
    public static final long LIFETIME = 15L * 60L * 20L;

    public record Stored(int slot, ItemStack stack) {
    }

    private UUID owner;
    private String ownerName = "";
    private long expire;
    private final List<Stored> items = new ArrayList<>();
    /** Se está llevando a otro sitio (al reconstruir la dungeon): al quitar el bloque no suelta nada. */
    private boolean moving;

    public void markMoving() {
        this.moving = true;
    }

    public boolean isMoving() {
        return moving;
    }

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVE.get(), pos, state);
    }

    public void setup(ServerPlayer owner, List<Stored> stored, long expire) {
        this.owner = owner.getUUID();
        this.ownerName = owner.getGameProfile().getName();
        this.expire = expire;
        this.items.clear();
        this.items.addAll(stored);
        setChanged();
    }

    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public long expire() {
        return expire;
    }

    public int count() {
        return items.size();
    }

    public boolean mine(ServerPlayer sp) {
        return owner == null || owner.equals(sp.getUUID()) || sp.hasPermissions(2);
    }

    /** Cada segundo mira si ya caducó. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, GraveBlockEntity be) {
        if (level instanceof ServerLevel sl && sl.getGameTime() % 20 == 0 && be.expire > 0 && sl.getGameTime() >= be.expire) {
            be.release(sl);
        }
    }

    /** El dueño recoge todo: cada objeto vuelve a su ranura si está libre, y si no al inventario. */
    public void give(ServerPlayer sp) {
        Inventory inv = sp.getInventory();
        for (Stored s : items) {
            ItemStack st = s.stack().copy();
            if (s.slot() >= 0 && s.slot() < inv.getContainerSize() && inv.getItem(s.slot()).isEmpty()) {
                inv.setItem(s.slot(), st);
            } else if (!inv.add(st)) {
                sp.drop(st, false);
            }
        }
        items.clear();
        sp.inventoryMenu.broadcastChanges();
        GraveEvents.remove((ServerLevel) level, worldPosition, owner);
    }

    /** Caducó: lo que guardaba cae al suelo y la lápida desaparece. */
    public void release(ServerLevel sl) {
        dropAll(sl);
        GraveEvents.remove(sl, worldPosition, owner);
    }

    /** Suelta los objetos en el suelo (si quedan) sin tocar el bloque. */
    public void dropAll(Level lv) {
        for (Stored s : items) {
            if (!s.stack().isEmpty()) {
                ItemEntity ie = new ItemEntity(lv, worldPosition.getX() + 0.5, worldPosition.getY() + 0.4, worldPosition.getZ() + 0.5, s.stack().copy());
                ie.setDefaultPickUpDelay();
                lv.addFreshEntity(ie);
            }
        }
        items.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putString("ownerName", ownerName);
        tag.putLong("expire", expire);
        ListTag l = new ListTag();
        for (Stored s : items) {
            CompoundTag c = new CompoundTag();
            c.putInt("Slot", s.slot());
            c.put("Item", s.stack().save(new CompoundTag()));
            l.add(c);
        }
        tag.put("items", l);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        ownerName = tag.getString("ownerName");
        expire = tag.getLong("expire");
        items.clear();
        for (Tag t : tag.getList("items", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            items.add(new Stored(c.getInt("Slot"), ItemStack.of(c.getCompound("Item"))));
        }
    }
}
