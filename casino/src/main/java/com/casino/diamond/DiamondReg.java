package com.casino.diamond;

import com.casino.entity.DealerEntity;
import com.casino.plat.Plat;
import com.casino.plat.Reg;
import com.casino.registry.ModBlockEntities;
import com.casino.registry.ModBlocks;
import com.casino.registry.ModEntities;
import com.casino.registry.ModItems;
import com.casino.worldgen.Machines;
import com.casino.worldgen.TemplateCasinoPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

/** Lo nuevo de THE DIAMOND: elevador, tienda de premios y los marcadores de la plantilla. */
public final class DiamondReg {
    public static final RegistryObject<Block> ELEVATOR = ModBlocks.BLOCKS.register("elevator",
            () -> new ElevatorBlock(Reg.block("elevator").mapColor(MapColor.GOLD).strength(3.0F, 1200.0F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(s -> 6)));
    public static final RegistryObject<Item> ELEVATOR_ITEM = ModItems.ITEMS.register("elevator",
            () -> new BlockItem(ELEVATOR.get(), Reg.blockItem("elevator")));
    public static final RegistryObject<BlockEntityType<ElevatorBE>> ELEVATOR_BE = ModBlockEntities.TYPES.register("elevator",
            () -> Reg.blockEntity(ElevatorBE::new, ELEVATOR.get()));

    public static final RegistryObject<Block> PRIZE_SHOP = ModBlocks.BLOCKS.register("prize_shop",
            () -> new PrizeShopBlock(Reg.block("prize_shop").mapColor(MapColor.TERRACOTTA_BROWN).strength(2.5F, 1200.0F).sound(SoundType.GLASS)
                    .noOcclusion().lightLevel(s -> 8)));
    public static final RegistryObject<Item> PRIZE_SHOP_ITEM = ModItems.ITEMS.register("prize_shop",
            () -> new BlockItem(PRIZE_SHOP.get(), Reg.blockItem("prize_shop")));
    public static final RegistryObject<BlockEntityType<PrizeShopBE>> SHOP_BE = ModBlockEntities.TYPES.register("prize_shop",
            () -> Reg.blockEntity(PrizeShopBE::new, PRIZE_SHOP.get()));

    private DiamondReg() {
    }

    /** Llamar en el constructor del mod (carga la clase a tiempo para registrar) y conecta los marcadores de plantilla. */
    public static void init() {
        TemplateCasinoPiece.Extra.register("elev", (a, pos, level, rot, box, dir) -> {
            if (!box.isInside(pos)) {
                return;
            }
            Direction f = rot.rotate(Machines.DIRS[dir & 3]);
            level.setBlock(pos, ELEVATOR.get().defaultBlockState().setValue(ElevatorBlock.FACING, f), 2);
            if (level.getBlockEntity(pos) instanceof ElevatorBE be) {
                be.access = a.length > 2 ? parse(a[2]) : 0;
                be.floor = a.length > 3 ? a[3] : "";
                be.setChanged();
            }
        });
        TemplateCasinoPiece.Extra.register("shop", (a, pos, level, rot, box, dir) -> {
            if (!box.isInside(pos)) {
                return;
            }
            Direction f = rot.rotate(Machines.DIRS[dir & 3]);
            level.setBlock(pos, PRIZE_SHOP.get().defaultBlockState().setValue(PrizeShopBlock.FACING, f), 2);
            if (level.getBlockEntity(pos) instanceof PrizeShopBE be) {
                be.stockDefaults();
                be.setChanged();
            }
        });
        TemplateCasinoPiece.Extra.register("guard", (a, pos, level, rot, box, dir) -> {
            if (!box.isInside(pos)) {
                return;
            }
            DealerEntity g = Plat.create(ModEntities.DEALER.get(), level.getLevel());
            if (g == null) {
                return;
            }
            Direction f = rot.rotate(Machines.DIRS[dir & 3]);
            g.setGame(DealerEntity.GAME_GUARD);
            g.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, f.toYRot(), 0.0F);
            g.setYHeadRot(f.toYRot());
            g.setYBodyRot(f.toYRot());
            level.addFreshEntityWithPassengers(g);
        });
        // pantalla gigante (por ahora, pantallas animadas; luego las carreras de caballos)
        TemplateCasinoPiece.Extra.register("tv", (a, pos, level, rot, box, dir) -> {
            Direction f = rot.rotate(Machines.DIRS[dir & 3]);
            Direction right = f.getCounterClockWise();
            int w = a.length > 2 ? parse(a[2]) : 4;
            int h = a.length > 3 ? parse(a[3]) : 3;
            Block screen = com.casino.registry.ModDecor.WALL_SCREEN.get();
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    BlockPos p = pos.relative(right, i - w / 2 + 1).above(j);
                    if (box.isInside(p)) {
                        level.setBlock(p, screen.defaultBlockState().setValue(com.casino.decor.WallDecorBlock.Screen.DESIGN, (i + j) & 3)
                                .setValue(com.casino.decor.WallDecorBlock.FACING, f), 2);
                    }
                }
            }
        });
    }

    static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
