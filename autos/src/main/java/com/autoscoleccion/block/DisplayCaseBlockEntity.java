package com.autoscoleccion.block;

import com.autoscoleccion.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Guarda el auto exhibido y si la base gira; se manda a los clientes para que lo dibujen. */
public class DisplayCaseBlockEntity extends BlockEntity {
    private ItemStack car = ItemStack.EMPTY;
    private boolean spin = true;
    /** Ángulo donde se quedó al parar (para que no salte). */
    public float frozenAngle;
    /** Cuándo se puso el auto (para la animación de entrada, en el cliente). */
    public long placedTime = -1000;

    public DisplayCaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISPLAY_CASE.get(), pos, state);
    }

    public ItemStack getCar() {
        return car;
    }

    public void setCar(ItemStack stack) {
        car = stack;
        sync();
    }

    public boolean isSpinning() {
        return spin;
    }

    public void toggleSpin() {
        if (level != null) {
            frozenAngle = (level.getGameTime() * 1.5F) % 360.0F;
        }
        spin = !spin;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!car.isEmpty()) {
            tag.put("car", car.save(new CompoundTag()));
        }
        tag.putBoolean("spin", spin);
        tag.putFloat("angle", frozenAngle);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ItemStack before = car;
        car = tag.contains("car") ? ItemStack.of(tag.getCompound("car")) : ItemStack.EMPTY;
        spin = !tag.contains("spin") || tag.getBoolean("spin");
        frozenAngle = tag.getFloat("angle");
        if (level != null && level.isClientSide && !car.isEmpty() && (before.isEmpty() || before.getItem() != car.getItem())) {
            placedTime = level.getGameTime();
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
