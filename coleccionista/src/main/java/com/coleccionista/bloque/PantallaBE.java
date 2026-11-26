package com.coleccionista.bloque;

import com.coleccionista.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lo que muestra la pantalla es igual en todas y lo manda el evento; la entidad solo existe para poder dibujarla grande y para recordar su
 * tamaño: {@code tam} 1 = mediana (7 x 4), 2 = chica (4 x 3: la de los pasillos de servicio, entre sus dos puertas, y la de los fondos
 * angostos). {@code dx}: cuánto se corre la imagen a la derecha de quien la ve (en bloques; -0.5 la centra entre dos bloques).
 */
public class PantallaBE extends BlockEntity {
    private int tam;
    private float dx;

    public PantallaBE(BlockPos pos, BlockState state) {
        super(ModRegistry.PANTALLA_BE.get(), pos, state);
    }

    /** 0 = según el bloque (3 x 2 o 11 x 7); 1 = mediana (7 x 4); 2 = chica (4 x 3). */
    public int tam() {
        return tam;
    }

    public float dx() {
        return dx;
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        tam = t.getInt("tam");
        dx = t.getFloat("dx");
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        if (tam != 0) {
            t.putInt("tam", tam);
        }
        if (dx != 0) {
            t.putFloat("dx", dx);
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

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        // se dibuja más grande que el bloque (3x2, 7x4 o 11x7)
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(7);
    }
}
