package com.casino.registry;

import com.casino.Casino;
import com.casino.worldgen.CasinoPiece;
import com.casino.worldgen.CasinoStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** El edificio del casino como estructura del mundo (tipo de estructura y tipo de pieza). */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Casino.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Casino.MODID);

    public static final RegistryObject<StructureType<CasinoStructure>> CASINO = TYPES.register("casino", () -> () -> CasinoStructure.CODEC);
    public static final RegistryObject<StructurePieceType> CASINO_PIECE = PIECES.register("casino",
            () -> (StructurePieceType) (ctx, tag) -> new CasinoPiece(tag));
    /** Casinos hechos con plantilla .nbt (THE DIAMOND y los que vengan). */
    public static final RegistryObject<StructureType<com.casino.worldgen.TemplateCasinoStructure>> TEMPLATE = TYPES.register("template",
            () -> () -> com.casino.worldgen.TemplateCasinoStructure.CODEC);
    public static final RegistryObject<StructurePieceType> TEMPLATE_PIECE = PIECES.register("template",
            () -> (StructurePieceType) (ctx, tag) -> new com.casino.worldgen.TemplateCasinoPiece(ctx.structureTemplateManager(), tag));

    private ModStructures() {
    }
}
