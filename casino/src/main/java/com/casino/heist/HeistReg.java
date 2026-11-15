package com.casino.heist;

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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Registro del robo a la bóveda: bloques, entidades de bloque, la tarjeta del gerente y los marcadores de la plantilla. */
public final class HeistReg {
    /** Los bloques del robo no se rompen en supervivencia (los quita el modo creativo). */
    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties hard(String name, MapColor c, SoundType s) {
        return Reg.block(name).mapColor(c).strength(-1.0F, 3_600_000.0F).sound(s).noOcclusion().noLootTable();
    }

    public static final List<RegistryObject<Item>> ITEMS = new ArrayList<>();

    private static RegistryObject<Block> block(String name, Supplier<Block> make, boolean item) {
        RegistryObject<Block> b = ModBlocks.BLOCKS.register(name, make);
        if (item) {
            ITEMS.add(ModItems.ITEMS.register(name, () -> new BlockItem(b.get(), Reg.blockItem(name))));
        }
        return b;
    }

    public static final RegistryObject<Block> CAMERA = block("security_camera",
            () -> new HeistBlocks.Camera(hard("security_camera", MapColor.COLOR_GRAY, SoundType.METAL)), true);
    public static final RegistryObject<Block> LASER = block("laser_emitter",
            () -> new HeistBlocks.Laser(hard("laser_emitter", MapColor.METAL, SoundType.METAL).lightLevel(s -> 4)), true);
    public static final RegistryObject<Block> READER = block("card_reader",
            () -> new HeistBlocks.Reader(hard("card_reader", MapColor.COLOR_BLACK, SoundType.METAL).lightLevel(s -> s.getValue(HeistBlocks.LIT) ? 7 : 3)), true);
    public static final RegistryObject<Block> SAFE = block("wall_safe",
            () -> new HeistBlocks.Safe(hard("wall_safe", MapColor.METAL, SoundType.METAL)), true);
    public static final RegistryObject<Block> NOTE = block("clue_note",
            () -> new HeistBlocks.Note(hard("clue_note", MapColor.SNOW, SoundType.WOOL)), true);
    public static final RegistryObject<Block> CONSOLE = block("camera_console",
            () -> new HeistBlocks.Console(hard("camera_console", MapColor.COLOR_BLACK, SoundType.METAL).lightLevel(s -> 6)), true);
    public static final RegistryObject<Block> MONITOR = block("cctv_monitor",
            () -> new HeistBlocks.Decor(Reg.block("cctv_monitor").mapColor(MapColor.COLOR_BLACK).strength(1.5F, 1200F).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(s -> 9), new double[]{0, 0, 10, 16, 16, 16}), true);
    public static final RegistryObject<Block> DEPOSIT = block("deposit_boxes",
            () -> new HeistBlocks.Decor(Reg.block("deposit_boxes").mapColor(MapColor.METAL).strength(5F, 1200F).sound(SoundType.METAL),
                    new double[]{0, 0, 0, 16, 16, 16}), true);
    public static final RegistryObject<Block> VAULT_DOOR = block("vault_door",
            () -> new HeistBlocks.VaultDoor(hard("vault_door", MapColor.METAL, SoundType.METAL)), true);
    public static final RegistryObject<Block> VAULT_SEAL = block("vault_seal",
            () -> new HeistBlocks.Seal(hard("vault_seal", MapColor.METAL, SoundType.METAL)), false);
    public static final RegistryObject<Block> CART = block("loot_cart",
            () -> new HeistBlocks.Cart(hard("loot_cart", MapColor.GOLD, SoundType.METAL)), true);
    public static final RegistryObject<Block> VENT = block("gas_vent",
            () -> new HeistBlocks.Vent(hard("gas_vent", MapColor.METAL, SoundType.METAL)), true);

    public static final RegistryObject<BlockEntityType<CameraBE>> CAMERA_BE = ModBlockEntities.TYPES.register("security_camera",
            () -> Reg.blockEntity(CameraBE::new, CAMERA.get()));
    public static final RegistryObject<BlockEntityType<LaserBE>> LASER_BE = ModBlockEntities.TYPES.register("laser_emitter",
            () -> Reg.blockEntity(LaserBE::new, LASER.get()));
    public static final RegistryObject<BlockEntityType<VaultDoorBE>> VAULT_BE = ModBlockEntities.TYPES.register("vault_door",
            () -> Reg.blockEntity(VaultDoorBE::new, VAULT_DOOR.get()));

    /** Tarjeta del gerente: abre la escalera de la bóveda y los dos lectores. Desaparece al terminar el robo. */
    public static final RegistryObject<Item> KEYCARD = ModItems.ITEMS.register("manager_keycard",
            () -> new Item(Reg.item("manager_keycard").stacksTo(4).rarity(Rarity.EPIC)));

    private HeistReg() {
    }

    static int parse(String[] a, int i) {
        try {
            return a.length > i ? Integer.parseInt(a[i]) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Llamar en el constructor del mod: carga la clase a tiempo y conecta los marcadores. */
    public static void init() {
        // guardias: "heistguard dir tipo dx dz" (patrulla entre su lugar y su lugar + (dx, dz))
        TemplateCasinoPiece.Extra.register("heistguard", (a, pos, level, rot, box, dir) -> {
            if (!box.isInside(pos)) {
                return;
            }
            DealerEntity g = Plat.create(ModEntities.DEALER.get(), level.getLevel());
            if (g == null) {
                return;
            }
            Direction f = rot.rotate(Machines.DIRS[dir & 3]);
            BlockPos off = new BlockPos(parse(a, 3), 0, parse(a, 4)).rotate(rot);
            int kind = parse(a, 2);
            g.setGame(DealerEntity.GAME_SECURITY);
            g.setVariant(kind);
            g.setHome(pos);
            g.setPatrol(off.getX(), off.getZ());
            g.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, f.toYRot(), 0.0F);
            g.setYHeadRot(f.toYRot());
            g.setYBodyRot(f.toYRot());
            if (kind == GuardAI.ARMED) {
                var hp = g.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) {
                    hp.setBaseValue(30.0);
                }
                g.setHealth(g.getMaxHealth());
            }
            level.addFreshEntityWithPassengers(g);
        });
        // el contacto: "heistboss dir"
        TemplateCasinoPiece.Extra.register("heistboss", (a, pos, level, rot, box, dir) -> {
            if (!box.isInside(pos)) {
                return;
            }
            DealerEntity g = Plat.create(ModEntities.DEALER.get(), level.getLevel());
            if (g == null) {
                return;
            }
            Direction f = rot.rotate(Machines.DIRS[dir & 3]);
            g.setGame(DealerEntity.GAME_FIXER);
            g.setVariant(7);
            g.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, f.toYRot(), 0.0F);
            g.setYHeadRot(f.toYRot());
            g.setYBodyRot(f.toYRot());
            level.addFreshEntityWithPassengers(g);
        });
    }
}
