package com.navidad.bloque.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** El barandal de las pasarelas: se ve como barrotes, pero choca como una cerca (bloque y medio): nadie se sube para caer al piso de abajo. */
public class DecoBarandal extends IronBarsBlock {
    private final Map<BlockState, VoxelShape> altas = new ConcurrentHashMap<>();

    public DecoBarandal(Properties p) {
        super(p);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        VoxelShape v = altas.get(s);
        if (v == null) {
            VoxelShape base = super.getCollisionShape(s, g, p, c);
            v = Shapes.empty();
            for (AABB b : base.toAabbs()) {
                v = Shapes.or(v, Shapes.box(b.minX, b.minY, b.minZ, b.maxX, 1.5, b.maxZ));
            }
            altas.put(s, v);
        }
        return v;
    }
}
