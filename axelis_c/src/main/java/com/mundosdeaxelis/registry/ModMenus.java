package com.mundosdeaxelis.registry;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.relic.EquipMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleContainer;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Tipos de menú del mod. */
public final class ModMenus {
    private ModMenus() {
    }

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MundosDeAxelis.MODID);

    /** Menú de equipo (mascotas y reliquias). El cliente solo recibe cuántas ranuras hay; el contenido lo manda el servidor. */
    public static final RegistryObject<MenuType<EquipMenu>> EQUIP = MENUS.register("equip",
            () -> IForgeMenuType.create((id, inv, buf) -> {
                int np = buf.readVarInt(), nr = buf.readVarInt();
                return new EquipMenu(id, inv, new SimpleContainer(com.mundosdeaxelis.relic.Equipment.MAX_PETS),
                        new SimpleContainer(com.mundosdeaxelis.relic.Equipment.MAX_RELICS), new SimpleContainer(1), np, nr);
            }));

    /** Menú de la Mesa del Artífice. */
    public static final RegistryObject<MenuType<com.mundosdeaxelis.table.TableMenu>> TABLE = MENUS.register("table",
            () -> IForgeMenuType.create((id, inv, buf) -> com.mundosdeaxelis.table.TableMenu.client(id, inv)));
}
