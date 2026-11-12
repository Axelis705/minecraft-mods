package com.casino.diamond;

import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import com.casino.plat.Ser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Mostrador de la tienda de premios: lista de objetos con su precio en fichas. La configura el dueño (operador) desde la pantalla;
 * trae una lista inicial que se puede cambiar o borrar.
 */
public class PrizeShopBE extends CasinoBE {
    public static final int MAX = 24;

    public record Offer(ItemStack item, int price) {
    }

    public final List<Offer> offers = new ArrayList<>();
    /** false hasta que se le pone la lista inicial (así un mostrador vaciado a propósito no se vuelve a llenar). */
    public boolean stocked = false;

    public PrizeShopBE(BlockPos pos, BlockState state) {
        super(DiamondReg.SHOP_BE.get(), pos, state);
    }

    /** Lista inicial (precios pensados para que no convenga comprar aquí y vender en el cajero: un diamante cuesta 80 fichas allá). */
    public void stockDefaults() {
        offers.clear();
        offers.add(new Offer(new ItemStack(Items.DIAMOND), 100));
        offers.add(new Offer(new ItemStack(Items.GOLDEN_APPLE, 2), 120));
        offers.add(new Offer(new ItemStack(Items.EXPERIENCE_BOTTLE, 16), 250));
        offers.add(new Offer(new ItemStack(Items.NETHERITE_INGOT), 3000));
        offers.add(new Offer(new ItemStack(Items.TOTEM_OF_UNDYING), 4000));
        offers.add(new Offer(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 6000));
        offers.add(new Offer(new ItemStack(Items.TRIDENT), 8000));
        offers.add(new Offer(new ItemStack(Items.ELYTRA), 20000));
        stocked = true;
    }

    @Override
    protected AABB renderBox() {
        return new AABB(worldPosition).inflate(0.5, 1.5, 0.5);
    }

    @Override
    protected void writeData(CompoundTag tag) {
        ListTag l = new ListTag();
        for (Offer o : offers) {
            CompoundTag t = new CompoundTag();
            t.put("Item", Ser.item(o.item()));
            t.putInt("Price", o.price());
            l.add(t);
        }
        tag.put("Offers", l);
        tag.putBoolean("Stocked", stocked);
    }

    @Override
    protected void readData(CompoundTag tag) {
        offers.clear();
        ListTag l = Nbt.list(tag, "Offers");
        for (int i = 0; i < l.size() && offers.size() < MAX; i++) {
            CompoundTag t = Nbt.at(l, i);
            ItemStack s = Ser.item(t.get("Item"));
            if (!s.isEmpty()) {
                offers.add(new Offer(s, Math.max(1, Nbt.i(t, "Price"))));
            }
        }
        stocked = Nbt.b(tag, "Stocked");
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState s = getBlockState();
            level.sendBlockUpdated(worldPosition, s, s, 3);
        }
    }
}
