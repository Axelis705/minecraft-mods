package com.coleccionista.bloque.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * El catre de la celda: es una cama de verdad (el evento acuesta a las piezas en la sala de espera), pero con su propio modelo de metal y
 * colchón viejo en vez de la cama de lana. La cama antigua de la mansión usa la misma forma, pero es solo de adorno ({@code adorno}).
 */
public class DecoCama extends BedBlock {
    /** false: solo de adorno (la cama antigua de la mansión): no se duerme ni guarda el punto de aparición. */
    private final boolean dormir;

    public DecoCama(BlockBehaviour.Properties p, boolean dormir) {
        super(DyeColor.GRAY, p);
        this.dormir = dormir;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
        return dormir ? super.use(s, l, p, pl, h, hit) : InteractionResult.PASS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return null;
    }
}
