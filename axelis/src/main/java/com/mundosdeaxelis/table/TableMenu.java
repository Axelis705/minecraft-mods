package com.mundosdeaxelis.table;

import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.crate.Tiers;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.registry.ModBlocks;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.registry.ModMenus;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.relic.Equipment;
import com.mundosdeaxelis.relic.RelicItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Menú de la Mesa del Artífice. Tiene una ranura para dejar una reliquia o mascota (pestaña Taller) y el inventario del jugador;
 * el resto (Tienda, Archivo, Mesa) se dibuja en la pantalla y sus acciones llegan como botones del menú, que se comprueban aquí.
 */
public class TableMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 24, SLOT_Y = 58;
    public static final int INV_X = 47, INV_Y = 172;

    // botones del menú
    public static final int B_LEVEL = 0, B_DISMANTLE = 1, B_REFINE = 2, B_UPGRADE = 3, B_BIND = 4;
    public static final int B_KEY = 100, B_CRATE = 200, B_CRAFT = 300;
    /** Cuántas veces más que la llave cuesta el bloque del crate. */
    public static final int CRATE_FACTOR = 4;

    private final Container slotBox = new SimpleContainer(1);
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Player player;
    /** Solo en el cliente: si la pestaña Taller está abierta (la ranura solo se ve y se usa ahí). */
    public boolean tallerOpen = true;

    public TableMenu(int id, Inventory inv, ContainerLevelAccess access, ContainerData data) {
        super(ModMenus.TABLE.get(), id);
        this.access = access;
        this.data = data;
        this.player = inv.player;
        addSlot(new Slot(slotBox, 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return s.getItem() instanceof RelicItem || s.getItem() instanceof com.mundosdeaxelis.gear.GearTool;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean isActive() {
                return tallerOpen;
            }
        });
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inv, 9 + r * 9 + c, INV_X + c * 18, INV_Y + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inv, c, INV_X + c * 18, INV_Y + 58));
        }
        addDataSlots(data);
    }

    public static TableMenu client(int id, Inventory inv) {
        return new TableMenu(id, inv, ContainerLevelAccess.NULL, new SimpleContainerData(4));
    }

    /** Nivel con el que puede trabajar aquí (el suyo o el de un jugador más avanzado que esté cerca). */
    public int effectiveLevel() {
        return Math.max(1, data.get(0));
    }

    /** ¿Está usando el nivel de otro jugador? */
    public boolean helped() {
        return data.get(1) != 0;
    }

    /** Objetos que ya tienes vinculados a tu alma. */
    public int boundCount() {
        return data.get(2);
    }

    /** Cuántos puedes tener vinculados con el nivel con el que trabajas. */
    public int boundLimit() {
        return data.get(3);
    }

    public ItemStack boxItem() {
        return slotBox.getItem(0);
    }

    @Override
    public void broadcastChanges() {
        if (player instanceof ServerPlayer sp) {
            int own = Essence.level(sp);
            int eff = own;
            Vec3 c = access.evaluate((lv, pos) -> Vec3.atCenterOf(pos)).orElse(null);
            if (c != null) {
                for (ServerPlayer o : sp.serverLevel().players()) {
                    if (o != sp && o.position().distanceToSqr(c) < 12 * 12) {
                        eff = Math.max(eff, Essence.level(o));
                    }
                }
            }
            data.set(0, eff);
            data.set(1, eff > own ? 1 : 0);
            data.set(2, Bound.count(sp));
            data.set(3, TableLevels.boundLimit(eff));
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(access, p, ModBlocks.ARTIFICER_TABLE.get());
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        access.execute((lv, pos) -> clearContainer(p, slotBox));
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack s = slot.getItem();
            result = s.copy();
            if (index == 0) {
                if (!moveItemStackTo(s, 1, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slots.get(0).mayPlace(s) && !slots.get(0).hasItem()) {
                slots.get(0).set(s.split(1));
            } else {
                return ItemStack.EMPTY;
            }
            if (s.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ acciones

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer sp)) {
            return false;
        }
        CrateKind[] kinds = CrateKind.values();
        if (id == B_LEVEL) {
            levelUp(sp);
        } else if (id == B_DISMANTLE) {
            dismantle(sp);
        } else if (id == B_REFINE) {
            refine(sp);
        } else if (id == B_UPGRADE) {
            upgradeTable(sp);
        } else if (id == B_BIND) {
            bind(sp);
        } else if (id >= B_KEY && id < B_KEY + kinds.length) {
            buy(sp, kinds[id - B_KEY], false);
        } else if (id >= B_CRATE && id < B_CRATE + kinds.length) {
            buy(sp, kinds[id - B_CRATE], true);
        } else if (id >= B_CRAFT && id < B_CRAFT + TableRecipes.all().size()) {
            craft(sp, TableRecipes.all().get(id - B_CRAFT));
        } else {
            return false;
        }
        return true;
    }

    private static void toast(ServerPlayer sp, boolean good, String key, Object... args) {
        Essence.sync(sp, Component.translatable("message.mundos_de_axelis.table." + key, args), good);
    }

    private void levelUp(ServerPlayer sp) {
        ItemStack s = slotBox.getItem(0);
        if (!(s.getItem() instanceof RelicItem)) {
            toast(sp, false, "need_item");
            return;
        }
        int lv = RelicItem.level(s);
        if (lv >= TableLevels.maxItemLevel(effectiveLevel())) {
            toast(sp, false, lv >= 5 ? "item_max" : "item_cap");
            return;
        }
        int cost = TableLevels.levelCost(Tiers.of(s), lv, effectiveLevel());
        if (!Essence.spend(sp, cost)) {
            toast(sp, false, "no_essence", cost);
            return;
        }
        RelicItem.setLevel(s, lv + 1);
        slotBox.setChanged();
        ServerLevel sl = sp.serverLevel();
        Vec3 p = sp.position();
        Fx.sound(sl, p.x, p.y, p.z, ModSounds.RELIC_LEVELUP.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        Vfx.column(sl, p, Tiers.of(s).color, 0.7, 5.0, 28);
        toast(sp, true, "leveled", s.getHoverName(), lv + 1);
    }

    /** Vincula el objeto de la ranura a tu alma (cuesta Esencia según su rareza) o lo desvincula (gratis). */
    private void bind(ServerPlayer sp) {
        ItemStack s = slotBox.getItem(0);
        if (s.isEmpty()) {
            toast(sp, false, "need_item");
            return;
        }
        if (Bound.isBoundTo(s, sp)) {
            Bound.unbind(s);
            slotBox.setChanged();
            sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8F, 0.8F);
            toast(sp, true, "unbound", s.getHoverName());
            return;
        }
        if (Bound.isBound(s)) {
            toast(sp, false, "bound_other", Bound.ownerName(s));
            return;
        }
        if (effectiveLevel() < 2) {
            toast(sp, false, "bind_level");
            return;
        }
        if (Bound.count(sp) >= TableLevels.boundLimit(effectiveLevel())) {
            toast(sp, false, "bind_limit", TableLevels.boundLimit(effectiveLevel()));
            return;
        }
        int cost = Bound.cost(s);
        if (!Essence.spend(sp, cost)) {
            toast(sp, false, "no_essence", cost);
            return;
        }
        Bound.bind(s, sp);
        slotBox.setChanged();
        ServerLevel sl = sp.serverLevel();
        Vec3 p = sp.position();
        Fx.sound(sl, p.x, p.y, p.z, ModSounds.RELIC_LEVELUP.get(), SoundSource.PLAYERS, 0.8F, 1.4F);
        Vfx.column(sl, p, 0x9FE8FF, 0.6, 4.0, 24);
        toast(sp, true, "bound", s.getHoverName(), cost);
    }

    private void dismantle(ServerPlayer sp) {
        ItemStack s = slotBox.getItem(0);
        if (!(s.getItem() instanceof RelicItem)) {
            toast(sp, false, "need_item");
            return;
        }
        if (s.hasTag() && s.getTag().getBoolean("mdc_unique")) {
            toast(sp, false, "unique");
            return;
        }
        int gain = TableLevels.dismantleValue(Tiers.of(s), RelicItem.level(s), effectiveLevel());
        Component name = s.getHoverName();
        slotBox.setItem(0, ItemStack.EMPTY);
        Essence.add(sp, gain);
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
        toast(sp, true, "dismantled", name, gain);
    }

    /** Cuántos de ese objeto lleva el jugador en el inventario. */
    public static int count(Player p, Item it) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(it)) {
                n += s.getCount();
            }
        }
        for (ItemStack s : p.getInventory().offhand) {
            if (s.is(it)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void take(Player p, Item it, int n) {
        for (ItemStack s : p.getInventory().items) {
            if (n > 0 && s.is(it)) {
                int t = Math.min(n, s.getCount());
                s.shrink(t);
                n -= t;
            }
        }
        for (ItemStack s : p.getInventory().offhand) {
            if (n > 0 && s.is(it)) {
                int t = Math.min(n, s.getCount());
                s.shrink(t);
                n -= t;
            }
        }
    }

    /** Materiales que se guardan para subir la mesa al siguiente nivel (no se refinan). */
    public static int reserved(Player p, Mats m) {
        int lv = Essence.level(p);
        int up = lv >= TableLevels.MAX ? 0 : TableLevels.upgradeMats(lv + 1).getOrDefault(m, 0);
        return Math.max(up, com.mundosdeaxelis.crate.CrateForge.reservedFor(m));
    }

    private void refine(ServerPlayer sp) {
        int gain = 0, pieces = 0;
        for (Mats m : Mats.values()) {
            Item it = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("mundos_de_axelis", m.itemId()));
            if (it == null || m.isBoss()) {
                continue; // los materiales de jefe no se refinan: son para subir la Mesa
            }
            int n = count(sp, it) - reserved(sp, m);
            if (n > 0) {
                take(sp, it, n);
                gain += n * m.essence;
                pieces += n;
            }
        }
        if (pieces == 0) {
            toast(sp, false, "nothing_to_refine");
            return;
        }
        Essence.add(sp, gain);
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9F, 1.1F);
        toast(sp, true, "refined", pieces, gain);
        sp.inventoryMenu.broadcastChanges();
    }

    private void upgradeTable(ServerPlayer sp) {
        int lv = Essence.level(sp);
        if (lv >= TableLevels.MAX) {
            toast(sp, false, "table_max");
            return;
        }
        int to = lv + 1;
        int cost = TableLevels.upgradeEssence(to);
        Map<Mats, Integer> need = TableLevels.upgradeMats(to);
        if (Essence.get(sp) < cost) {
            toast(sp, false, "no_essence", cost);
            return;
        }
        for (Map.Entry<Mats, Integer> e : need.entrySet()) {
            Item it = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("mundos_de_axelis", e.getKey().itemId()));
            if (it == null || count(sp, it) < e.getValue()) {
                toast(sp, false, "no_materials");
                return;
            }
        }
        Essence.spend(sp, cost);
        for (Map.Entry<Mats, Integer> e : need.entrySet()) {
            take(sp, net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("mundos_de_axelis", e.getKey().itemId())), e.getValue());
        }
        Essence.setLevel(sp, to);
        ServerLevel sl = sp.serverLevel();
        Vec3 p = sp.position();
        Fx.sound(sl, p.x, p.y, p.z, ModSounds.RELIC_LEVELUP.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        Vfx.column(sl, p, 0xC8A0FF, 0.9, 7.0, 44);
        Vfx.rune(sl, p, 0xFFE9A8, 3.2, 44);
        Equipment.sync(sp);
        sp.inventoryMenu.broadcastChanges();
        toast(sp, true, "table_up", Component.translatable(TableLevels.nameKey(to)));
    }

    /** Forja el bloque de una caja: Esencia + materiales de mobs + el sello del jefe anterior. */
    private void forge(ServerPlayer sp, CrateKind k) {
        if (k.keyCost() <= 0) {
            return;
        }
        if (k.rank() > com.mundosdeaxelis.crate.CrateForge.stage(Essence.archive(sp)) + 1) {
            toast(sp, false, "crate_hidden");
            return;
        }
        int cost = com.mundosdeaxelis.crate.CrateForge.essence(k);
        if (Essence.get(sp) < cost) {
            toast(sp, false, "no_essence", cost);
            return;
        }
        for (com.mundosdeaxelis.crate.CrateForge.Need n : com.mundosdeaxelis.crate.CrateForge.mats(k)) {
            Item mi = ModItems.MATS.get(n.mat()).get();
            if (count(sp, mi) < n.n()) {
                toast(sp, false, "no_materials");
                return;
            }
        }
        CrateKind sealKind = com.mundosdeaxelis.crate.CrateForge.sealOf(k);
        if (sealKind != null && count(sp, com.mundosdeaxelis.crate.CrateForge.sealItem(sealKind)) < 1) {
            toast(sp, false, "no_seal", Component.translatable(sealKind.titleKey()));
            return;
        }
        if (!Essence.spend(sp, cost)) {
            toast(sp, false, "no_essence", cost);
            return;
        }
        for (com.mundosdeaxelis.crate.CrateForge.Need n : com.mundosdeaxelis.crate.CrateForge.mats(k)) {
            take(sp, ModItems.MATS.get(n.mat()).get(), n.n());
        }
        if (sealKind != null) {
            take(sp, com.mundosdeaxelis.crate.CrateForge.sealItem(sealKind), 1);
        }
        ItemStack st = new ItemStack(ModItems.CRATE_ITEMS.get(k).get());
        if (!sp.getInventory().add(st)) {
            sp.drop(st, false);
        }
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.3F);
        toast(sp, true, "forged", Component.translatable(k.titleKey()), cost);
        sp.inventoryMenu.broadcastChanges();
    }

    /** Crea un objeto de la pestaña «Crear»: pide nivel de mesa, Esencia y los ingredientes del inventario. */
    private void craft(ServerPlayer sp, TableRecipes.Recipe r) {
        if (effectiveLevel() < r.minLevel()) {
            toast(sp, false, "craft_level", Component.translatable(TableLevels.nameKey(r.minLevel())));
            return;
        }
        if (Essence.get(sp) < r.essence()) {
            toast(sp, false, "no_essence", r.essence());
            return;
        }
        for (TableRecipes.Ing in : r.ings()) {
            if (count(sp, in.item().get()) < in.n()) {
                toast(sp, false, "no_materials");
                return;
            }
        }
        if (!Essence.spend(sp, r.essence())) {
            toast(sp, false, "no_essence", r.essence());
            return;
        }
        for (TableRecipes.Ing in : r.ings()) {
            take(sp, in.item().get(), in.n());
        }
        ItemStack st = new ItemStack(r.result().get(), r.count());
        Component name = st.getHoverName();
        if (!sp.getInventory().add(st)) {
            sp.drop(st, false);
        }
        ServerLevel sl = sp.serverLevel();
        sl.playSound(null, sp.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.7F, 1.3F);
        sl.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.2F);
        Vfx.column(sl, sp.position(), 0xFFB347, 0.6, 4.0, 24);
        toast(sp, true, "crafted", name, r.count());
        sp.inventoryMenu.broadcastChanges();
    }

    private void buy(ServerPlayer sp, CrateKind k, boolean block) {
        if (block) {
            forge(sp, k);
            return;
        }
        if (k.keyCost() <= 0) {
            return;
        }
        if (k.rank() > com.mundosdeaxelis.crate.CrateForge.stage(Essence.archive(sp)) + 1) {
            toast(sp, false, "crate_hidden");
            return;
        }
        int cost = block ? k.keyCost() * CRATE_FACTOR : TableLevels.keyPrice(k, effectiveLevel());
        if (!Essence.spend(sp, cost)) {
            toast(sp, false, "no_essence", cost);
            return;
        }
        Item it = (block ? ModItems.CRATE_ITEMS : ModItems.KEYS).get(k).get();
        ItemStack st = new ItemStack(it);
        if (!sp.getInventory().add(st)) {
            sp.drop(st, false);
        }
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.3F);
        toast(sp, true, block ? "bought_crate" : "bought_key", Component.translatable(k.titleKey()), cost);
    }
}
