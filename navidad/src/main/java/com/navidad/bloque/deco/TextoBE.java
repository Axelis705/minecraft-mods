package com.navidad.bloque.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * El texto de una placa, un letrero o unos rayones en la pared (hasta 4 líneas). El mapa lo escribe con
 * {@code {lineas:["PIEZA","07"],color:16711680}} y el evento con {@link #escribir}.
 */
public class TextoBE extends BlockEntity {
    private String[] lineas = new String[0];
    private int color = 0xD8D0C0;

    public TextoBE(BlockPos p, BlockState s) {
        super(Deco.TEXTO_BE, p, s);
    }

    public String[] lineas() {
        return lineas;
    }

    public int color() {
        return color;
    }

    /** Escribe y avisa a los jugadores. */
    public void escribir(int color, String... lineas) {
        this.lineas = lineas.clone();
        this.color = color;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        if (t.contains("lineas")) {
            ListTag l = t.getList("lineas", Tag.TAG_STRING);
            lineas = new String[l.size()];
            for (int i = 0; i < l.size(); i++) {
                lineas[i] = l.getString(i);
            }
        }
        if (t.contains("color")) {
            color = t.getInt("color");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        ListTag l = new ListTag();
        for (String s : lineas) {
            l.add(StringTag.valueOf(s));
        }
        t.put("lineas", l);
        t.putInt("color", color);
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
