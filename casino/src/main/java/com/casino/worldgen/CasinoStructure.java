package com.casino.worldgen;

import com.casino.game.CasinoPlan;
import com.casino.registry.ModStructures;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * El casino se genera solo en terreno bastante plano y seco (no en el agua ni en laderas), girado al azar hacia uno de los cuatro lados.
 * Es raro, como un pueblo: hay uno cada varios cientos de bloques en biomas de planicie, bosque, desierto y sabana.
 */
public class CasinoStructure extends Structure {
    public static final Codec<CasinoStructure> CODEC = simpleCodec(CasinoStructure::new);

    public CasinoStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int rot = ctx.random().nextInt(4);
        boolean odd = (rot & 1) == 1;
        int sx = odd ? CasinoPlan.D : CasinoPlan.W;
        int sz = odd ? CasinoPlan.W : CasinoPlan.D;
        int x0 = cp.getMiddleBlockX() - sx / 2;
        int z0 = cp.getMiddleBlockZ() - sz / 2;

        // la puerta (el frente) manda: el piso queda a la altura del terreno de ahí
        int[] front = CasinoPiece.toWorld(rot, x0, z0, CasinoPlan.W / 2, CasinoPlan.D - 1);
        int hFront = ctx.chunkGenerator().getBaseHeight(front[0], front[1], Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
        // 3x3 puntos repartidos por toda la huella: nada de agua y poca diferencia de altura
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int x = x0 + (sx - 1) * i / 2;
                int z = z0 + (sz - 1) * j / 2;
                int top = ctx.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
                int floor = ctx.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
                if (top != floor || Math.abs(top - hFront) > 4) {
                    return Optional.empty();
                }
            }
        }
        int y0 = hFront - 1;
        BoundingBox box = new BoundingBox(x0, y0 - CasinoPiece.FOUNDATION, z0, x0 + sx - 1, y0 + CasinoPlan.TOP_Y, z0 + sz - 1);
        BlockPos start = new BlockPos(cp.getMiddleBlockX(), y0, cp.getMiddleBlockZ());
        return Optional.of(new Structure.GenerationStub(start, builder -> builder.addPiece(new CasinoPiece(rot, y0, box))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CASINO.get();
    }
}
