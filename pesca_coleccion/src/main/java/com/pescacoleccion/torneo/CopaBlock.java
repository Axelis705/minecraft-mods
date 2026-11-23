package com.pescacoleccion.torneo;

import com.axeliscore.block.CoreBlock;
import com.axeliscore.plat.ItemTag;
import com.axeliscore.plat.Nbt;
import com.axeliscore.ui.InfoBlock;
import com.axeliscore.ui.InfoCard;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Copa de torneo (oro, plata o bronce): se pone donde se quiera y al mirarla dice quién la ganó, cuándo y con cuánto. */
public class CopaBlock extends CoreBlock implements EntityBlock, InfoBlock {
    private static final VoxelShape FORMA = Block.box(3, 0, 3, 13, 14, 13);
    /** 0 oro, 1 plata, 2 bronce. */
    public final int lugar;

    public CopaBlock(Properties props, int lugar) {
        super(props);
        this.lugar = lugar;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return FORMA;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopaBE(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CopaBE be) {
            be.info = ItemTag.get(stack).copy();
            be.changed();
        }
    }

    @Override
    protected void onPlayerBreak(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CopaBE be && !player.getAbilities().instabuild) {
            ItemStack st = new ItemStack(asItem());
            CompoundTag info = be.info;
            ItemTag.edit(st, t -> {
                for (String k : info.getAllKeys()) {
                    t.put(k, info.get(k).copy());
                }
            });
            Block.popResource(level, pos, st);
        }
    }

    @Override
    public void axInfo(Level level, BlockPos pos, BlockState state, InfoCard card, boolean detail) {
        CompoundTag info = level.getBlockEntity(pos) instanceof CopaBE be ? be.info : new CompoundTag();
        Copas.llenar(card, info, lugar);
    }

    /** El texto de la copa (también para el cuadro del objeto). */
    public static String ganador(CompoundTag info) {
        return Nbt.s(info, "g");
    }
}
