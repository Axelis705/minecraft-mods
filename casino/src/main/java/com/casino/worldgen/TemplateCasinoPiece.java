package com.casino.worldgen;

import com.casino.game.CasinoPlan.Mach;
import com.casino.plat.Ids;
import com.casino.plat.Nbt;
import com.casino.plat.Plat;
import com.casino.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Locale;

/**
 * Un casino hecho como plantilla (archivo .nbt diseñado con las herramientas de casino/herramientas/estructuras). Los bloques normales
 * vienen en la plantilla ya girados; las máquinas y los bloques con datos van como "marcadores" (bloques de estructura en modo datos)
 * y aquí se cambian por la máquina de verdad. Debajo se rellena hasta tocar suelo para que no quede flotando.
 */
public class TemplateCasinoPiece extends TemplateStructurePiece {
    public static final int FOUNDATION = 16;

    public TemplateCasinoPiece(StructureTemplateManager mgr, ResourceLocation id, BlockPos pos, Rotation rot) {
        super(ModStructures.TEMPLATE_PIECE.get(), 0, mgr, id, id.toString(), settings(rot), pos);
    }

    public TemplateCasinoPiece(StructureTemplateManager mgr, CompoundTag tag) {
        super(ModStructures.TEMPLATE_PIECE.get(), tag, mgr, id -> settings(Rotation.valueOf(Nbt.s(tag, "Rot"))));
    }

    public static StructurePlaceSettings settings(Rotation rot) {
        return new StructurePlaceSettings().setRotation(rot).setMirror(Mirror.NONE).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
        tag.putString("Rot", placeSettings.getRotation().name());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator gen, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        foundation(level, chunkBox);
        super.postProcess(level, structures, gen, random, chunkBox, chunkPos, pivot);
    }

    /** Rellena debajo del piso de la plantilla hasta tocar suelo firme (en laderas o huecos). */
    private void foundation(WorldGenLevel level, BoundingBox chunkBox) {
        BoundingBox b = boundingBox;
        BlockState fill = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = Math.max(b.minX(), chunkBox.minX()); x <= Math.min(b.maxX(), chunkBox.maxX()); x++) {
            for (int z = Math.max(b.minZ(), chunkBox.minZ()); z <= Math.min(b.maxZ(), chunkBox.maxZ()); z++) {
                boolean edge = x == b.minX() || x == b.maxX() || z == b.minZ() || z == b.maxZ();
                for (int y = b.minY() - 1; y >= b.minY() - FOUNDATION && y > Plat.minY(level); y--) {
                    p.set(x, y, z);
                    BlockState cur = level.getBlockState(p);
                    if (cur.canOcclude() && cur.getFluidState().isEmpty() && !cur.is(net.minecraft.tags.BlockTags.LEAVES)
                            && !cur.is(net.minecraft.tags.BlockTags.LOGS)) {
                        break;
                    }
                    level.setBlock(p, edge ? dirt : fill, 2);
                }
            }
        }
    }

    @Override
    protected void handleDataMarker(String meta, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        if (box.isInside(pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        Markers.apply(meta, pos, level, placeSettings.getRotation(), box);
    }

    /** Construye la plantilla entera de una vez (comando): carga los chunks, coloca todo y hace aparecer a los crupieres. */
    public static BoundingBox buildNow(ServerLevel level, String name, BlockPos corner, Rotation rot) {
        //#if MC >= 26.3
        //$$ StructureTemplateManager mgr = level.getStructureTemplateManager();
        //#else
        StructureTemplateManager mgr = level.getStructureManager();
        //#endif
        ResourceLocation id = Ids.casino(name.toLowerCase(Locale.ROOT));
        StructureTemplate tpl = mgr.getOrCreate(id);
        StructurePlaceSettings ps = settings(rot);
        // la esquina mínima de la caja ya girada queda en corner
        BoundingBox box0 = tpl.getBoundingBox(ps, BlockPos.ZERO);
        BlockPos pos = corner.offset(-box0.minX(), 0, -box0.minZ());
        TemplateCasinoPiece piece = new TemplateCasinoPiece(mgr, id, pos, rot);
        BoundingBox box = piece.getBoundingBox();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                level.getChunk(cx, cz);
                BoundingBox cb = new BoundingBox(cx << 4, Plat.minY(level), cz << 4, (cx << 4) + 15, Plat.maxY(level), (cz << 4) + 15);
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), cb, new ChunkPos(cx, cz), BlockPos.ZERO);
            }
        }
        for (StructureTemplate.StructureBlockInfo info : tpl.filterBlocks(pos, ps, Blocks.STRUCTURE_BLOCK)) {
            String meta = info.nbt() == null ? "" : Nbt.s(info.nbt(), "metadata");
            Markers.after(meta, info.pos(), level);
        }
        return box;
    }

    /** Lo que significa cada marcador. */
    public static final class Markers {
        private Markers() {
        }

        static void apply(String meta, BlockPos pos, ServerLevelAccessor level, Rotation rot, BoundingBox box) {
            String[] a = meta.trim().split("\\s+");
            if (a.length < 2) {
                return;
            }
            int dir = parse(a[a.length > 2 ? 2 : 1]);
            switch (a[0]) {
                case "m" -> {
                    Mach k = Mach.valueOf(a[1].toUpperCase(Locale.ROOT));
                    Machines.place(level, box, k, parse(a[2]), rot, pos);
                }
                default -> Extra.apply(a, pos, level, rot, box, dir);
            }
        }

        /** Después de construir con el comando (todo cargado): crupieres. */
        static void after(String meta, BlockPos pos, ServerLevel level) {
            String[] a = meta.trim().split("\\s+");
            if (a.length >= 3 && a[0].equals("m")) {
                Machines.spawnDealer(level, pos);
            }
        }

        static int parse(String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    /** Marcadores de otros bloques especiales (elevador, tienda, guardia...). Se llenan en sus propias clases. */
    public static final class Extra {
        public interface Handler {
            void apply(String[] args, BlockPos pos, ServerLevelAccessor level, Rotation rot, BoundingBox box, int dir);
        }

        private static final java.util.Map<String, Handler> HANDLERS = new java.util.HashMap<>();

        private Extra() {
        }

        public static void register(String key, Handler h) {
            HANDLERS.put(key, h);
        }

        static void apply(String[] a, BlockPos pos, ServerLevelAccessor level, Rotation rot, BoundingBox box, int dir) {
            Handler h = HANDLERS.get(a[0]);
            if (h != null) {
                h.apply(a, pos, level, rot, box, dir);
            }
        }
    }
}
