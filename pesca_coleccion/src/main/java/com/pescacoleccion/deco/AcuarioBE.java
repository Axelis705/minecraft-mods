package com.pescacoleccion.deco;

import com.axeliscore.block.CoreBlockEntity;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Ser;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** El acuario guarda hasta {@link #MAX} peces que nadan adentro. */
public class AcuarioBE extends CoreBlockEntity {
    public static final int MAX = 3;
    public final List<ItemStack> peces = new ArrayList<>();

    public AcuarioBE(BlockPos pos, BlockState state) {
        super(ModRegistry.ACUARIO_BE.get(), pos, state);
    }

    @Override
    protected void writeData(CompoundTag tag) {
        ListTag l = new ListTag();
        for (ItemStack st : peces) {
            CompoundTag t = new CompoundTag();
            t.put("pez", Ser.item(st));
            l.add(t);
        }
        tag.put("peces", l);
    }

    @Override
    protected void readData(CompoundTag tag) {
        peces.clear();
        ListTag l = Nbt.list(tag, "peces");
        for (int i = 0; i < l.size() && i < MAX; i++) {
            ItemStack st = Ser.item(Nbt.at(l, i).get("pez"));
            if (!st.isEmpty()) {
                peces.add(st);
            }
        }
    }
}
