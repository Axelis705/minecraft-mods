package com.casino.west;

import com.casino.diamond.DiamondReg;
import com.casino.entity.DealerEntity;
import com.casino.plat.Reg;
import com.casino.registry.ModBlockEntities;
import com.casino.registry.ModBlocks;
import com.casino.registry.ModDecor;
import com.casino.registry.ModItems;
import com.casino.worldgen.TemplateCasinoPiece;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/** Registro del casino del Viejo Oeste: bloques, entidades de bloque y marcadores de la plantilla del saloon. */
public final class WestReg {
    private static RegistryObject<Block> block(String name, Supplier<Block> make) {
        RegistryObject<Block> b = ModBlocks.BLOCKS.register(name, make);
        ModDecor.TAB_ITEMS.add(ModItems.ITEMS.register(name, () -> new BlockItem(b.get(), Reg.blockItem(name))));
        return b;
    }

    public static final RegistryObject<Block> SALOON_DOOR = block("saloon_door", () -> new WestBlocks.SaloonDoor(Reg.block("saloon_door")
            .mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion().noCollission()));
    public static final RegistryObject<Block> VIP_GATE = block("vip_gate", () -> new WestBlocks.VipGate(Reg.block("vip_gate")
            .mapColor(MapColor.COLOR_RED).strength(-1.0F, 3_600_000.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> PIANO = block("player_piano", () -> new WestBlocks.Piano(Reg.block("player_piano")
            .mapColor(MapColor.TERRACOTTA_BROWN).strength(2.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> PAINTED_LETTER = block("painted_letter", () -> new WestBlocks.PaintedLetter(Reg.block("painted_letter")
            .mapColor(MapColor.WOOD).strength(0.8F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> WANTED = block("wanted_poster", () -> new WestBlocks.Wanted(Reg.block("wanted_poster")
            .mapColor(MapColor.SAND).strength(0.3F).sound(SoundType.WOOL).noOcclusion().noCollission()));

    public static final RegistryObject<BlockEntityType<SaloonDoorBE>> DOOR_BE = ModBlockEntities.TYPES.register("saloon_door",
            () -> Reg.blockEntity(SaloonDoorBE::new, SALOON_DOOR.get()));
    public static final RegistryObject<BlockEntityType<SaloonDoorBE>> GATE_BE = ModBlockEntities.TYPES.register("vip_gate",
            () -> Reg.blockEntity((p, s) -> new SaloonDoorBE(WestReg.GATE_BE.get(), p, s), VIP_GATE.get()));
    public static final RegistryObject<BlockEntityType<PianoBE>> PIANO_BE = ModBlockEntities.TYPES.register("player_piano",
            () -> Reg.blockEntity(PianoBE::new, PIANO.get()));

    private WestReg() {
    }

    /** Llamar en el constructor del mod: carga la clase a tiempo y conecta los marcadores. */
    public static void init() {
        // vaqueros y vaqueras que pasean por el saloon: "wpatron dir [piel]"
        TemplateCasinoPiece.Extra.register("wpatron", (a, pos, level, rot, box, dir) -> {
            DealerEntity p = DiamondReg.npc(DealerEntity.GAME_PATRON, new String[]{a[0], a[1], "w"}, pos, level, rot, box, dir);
            if (p != null) {
                int v = a.length > 2 ? DiamondReg.parse(a[2]) : level.getRandom().nextInt(DealerEntity.WEST_PATRON_SKINS);
                p.setVariant(DealerEntity.WEST + Math.floorMod(v, DealerEntity.WEST_PATRON_SKINS));
                p.setHome(pos);
            }
        });
    }
}
