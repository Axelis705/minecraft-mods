package com.casino.worldgen;

import com.casino.plat.Ids;
import com.casino.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

/**
 * Un casino que sale de una plantilla .nbt (el campo "template" del JSON de la estructura). Busca terreno seco y no muy empinado,
 * gira la plantilla al azar y la centra en el chunk. El piso queda a la altura del terreno en la entrada (el frente de la plantilla es
 * el lado sur, z máxima).
 */
public class TemplateCasinoStructure extends Structure {
    //#if MC >= 1.20.5
    //$$ public static final com.mojang.serialization.MapCodec<TemplateCasinoStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
    //$$         settingsCodec(i), Codec.STRING.fieldOf("template").forGetter(s -> s.template),
    //$$         Codec.INT.optionalFieldOf("max_slope", 10).forGetter(s -> s.maxSlope)).apply(i, TemplateCasinoStructure::new));
    //#else
    public static final Codec<TemplateCasinoStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i), Codec.STRING.fieldOf("template").forGetter(s -> s.template),
            Codec.INT.optionalFieldOf("max_slope", 10).forGetter(s -> s.maxSlope)).apply(i, TemplateCasinoStructure::new));
    //#endif

    public final String template;
    public final int maxSlope;

    public TemplateCasinoStructure(Structure.StructureSettings settings, String template, int maxSlope) {
        super(settings);
        this.template = template;
        this.maxSlope = maxSlope;
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        Rotation rot = Rotation.values()[ctx.random().nextInt(4)];
        ResourceLocation id = Ids.casino(template);
        StructureTemplate tpl = ctx.structureTemplateManager().getOrCreate(id);
        net.minecraft.core.Vec3i size = tpl.getSize();
        if (size.getX() == 0) {
            return Optional.empty();
        }
        var ps = TemplateCasinoPiece.settings(rot);
        BoundingBox box0 = tpl.getBoundingBox(ps, BlockPos.ZERO);
        int sx = box0.getXSpan();
        int sz = box0.getZSpan();
        int x0 = cp.getMiddleBlockX() - sx / 2;
        int z0 = cp.getMiddleBlockZ() - sz / 2;
        // la entrada (centro del lado sur de la plantilla) manda la altura del piso
        BlockPos front = StructureTemplate.calculateRelativePosition(ps, new BlockPos(size.getX() / 2, 0, size.getZ() - 1));
        int fx = x0 + front.getX() - box0.minX();
        int fz = z0 + front.getZ() - box0.minZ();
        int hFront = height(ctx, fx, fz, Heightmap.Types.WORLD_SURFACE_WG);
        // 5x5 puntos por toda la huella: nada de agua y poca diferencia de altura
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                int x = x0 + (sx - 1) * i / 4;
                int z = z0 + (sz - 1) * j / 4;
                int top = height(ctx, x, z, Heightmap.Types.WORLD_SURFACE_WG);
                int floor = height(ctx, x, z, Heightmap.Types.OCEAN_FLOOR_WG);
                if (top != floor || Math.abs(top - hFront) > maxSlope) {
                    return Optional.empty();
                }
            }
        }
        int y0 = hFront - 1;
        BlockPos pos = new BlockPos(x0 - box0.minX(), y0, z0 - box0.minZ());
        return Optional.of(new Structure.GenerationStub(new BlockPos(cp.getMiddleBlockX(), y0, cp.getMiddleBlockZ()),
                builder -> builder.addPiece(new TemplateCasinoPiece(ctx.structureTemplateManager(), id, pos, rot))));
    }

    private static int height(Structure.GenerationContext ctx, int x, int z, Heightmap.Types t) {
        return ctx.chunkGenerator().getBaseHeight(x, z, t, ctx.heightAccessor(), ctx.randomState());
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.TEMPLATE.get();
    }
}
