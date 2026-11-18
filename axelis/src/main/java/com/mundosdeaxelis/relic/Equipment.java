package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.EquipSyncPacket;
import com.mundosdeaxelis.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Equipo del jugador: las mascotas y reliquias que lleva puestas en el menú de equipo. Solo lo equipado funciona
 * (poderes pasivos, tecla R para las reliquias y tecla V para las mascotas). Se guarda con el jugador y no se pierde al morir.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Equipment {
    private Equipment() {
    }

    /** Ranuras que existen en total (las que aún no se han desbloqueado se ven bloqueadas en el menú). */
    public static final int MAX_PETS = 5;
    public static final int MAX_RELICS = 8;
    private static final String KEY = "mdc_equip";

    /** Ranuras de mascotas desbloqueadas (3 y +1 con la mesa en nivel 2 y +1 en nivel 3). */
    public static int petSlots(Player p) {
        return Math.min(MAX_PETS, com.mundosdeaxelis.table.TableLevels.petSlots(com.mundosdeaxelis.table.Essence.level(p)));
    }

    /** Ranuras de reliquias desbloqueadas. */
    public static int relicSlots(Player p) {
        return Math.min(MAX_RELICS, com.mundosdeaxelis.table.TableLevels.relicSlots(com.mundosdeaxelis.table.Essence.level(p)));
    }

    /** Datos de un jugador: sus mascotas y reliquias equipadas. */
    public static final class Data {
        public final ItemStack[] pets = empty(MAX_PETS);
        public final ItemStack[] relics = empty(MAX_RELICS);
        /** Ranura de la capa (una sola). */
        public final ItemStack[] capes = empty(1);
        boolean dirty;

        private static ItemStack[] empty(int n) {
            ItemStack[] a = new ItemStack[n];
            java.util.Arrays.fill(a, ItemStack.EMPTY);
            return a;
        }

        /** Vista como contenedor (comparte los objetos con los datos, sin copiarlos). */
        public Container petContainer() {
            return new View(pets, this);
        }

        public Container relicContainer() {
            return new View(relics, this);
        }

        public Container capeContainer() {
            return new View(capes, this);
        }
    }

    /** Contenedor sobre un arreglo de objetos: así las ranuras del menú leen y escriben directamente los datos del jugador. */
    private static final class View implements Container {
        private final ItemStack[] a;
        private final Data owner;

        View(ItemStack[] a, Data owner) {
            this.a = a;
            this.owner = owner;
        }

        @Override
        public int getContainerSize() {
            return a.length;
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack s : a) {
                if (!s.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int i) {
            return a[i];
        }

        @Override
        public ItemStack removeItem(int i, int n) {
            ItemStack r = a[i].isEmpty() ? ItemStack.EMPTY : a[i].split(n);
            if (!r.isEmpty()) {
                setChanged();
            }
            return r;
        }

        @Override
        public ItemStack removeItemNoUpdate(int i) {
            ItemStack r = a[i];
            a[i] = ItemStack.EMPTY;
            return r;
        }

        @Override
        public void setItem(int i, ItemStack s) {
            a[i] = s;
            setChanged();
        }

        @Override
        public void setChanged() {
            owner.dirty = true;
        }

        @Override
        public boolean stillValid(Player p) {
            return true;
        }

        @Override
        public void clearContent() {
            java.util.Arrays.fill(a, ItemStack.EMPTY);
            setChanged();
        }
    }

    private static final Map<UUID, Data> CACHE = new HashMap<>();

    /** Equipo del jugador (se carga de sus datos la primera vez). */
    public static Data get(ServerPlayer sp) {
        return CACHE.computeIfAbsent(sp.getUUID(), id -> load(sp.getPersistentData()));
    }

    /** Lo mismo sin exigir un jugador del servidor: vacío si no lo es (por ejemplo en el cliente). */
    public static Data peek(Player p) {
        return p instanceof ServerPlayer sp ? get(sp) : null;
    }

    private static Data load(CompoundTag root) {
        Data d = new Data();
        if (!root.contains(KEY, Tag.TAG_COMPOUND)) {
            return d;
        }
        CompoundTag t = root.getCompound(KEY);
        read(t.getList("pets", Tag.TAG_COMPOUND), d.pets);
        read(t.getList("relics", Tag.TAG_COMPOUND), d.relics);
        read(t.getList("capes", Tag.TAG_COMPOUND), d.capes);
        return d;
    }

    private static void read(ListTag list, ItemStack[] out) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            int slot = e.getInt("Slot");
            if (slot >= 0 && slot < out.length) {
                out[slot] = ItemStack.of(e.getCompound("Item"));
            }
        }
    }

    private static ListTag write(ItemStack[] a) {
        ListTag list = new ListTag();
        for (int i = 0; i < a.length; i++) {
            if (!a[i].isEmpty()) {
                CompoundTag e = new CompoundTag();
                e.putInt("Slot", i);
                e.put("Item", a[i].save(new CompoundTag()));
                list.add(e);
            }
        }
        return list;
    }

    /** Escribe el equipo en los datos persistentes del jugador. */
    public static void save(ServerPlayer sp) {
        Data d = CACHE.get(sp.getUUID());
        if (d == null) {
            return;
        }
        CompoundTag t = new CompoundTag();
        t.put("pets", write(d.pets));
        t.put("relics", write(d.relics));
        t.put("capes", write(d.capes));
        sp.getPersistentData().put(KEY, t);
        d.dirty = false;
    }

    /** Avisa al cliente de lo que lleva equipado (para el panel de la pantalla y la selección de reliquias). */
    public static void sync(ServerPlayer sp) {
        Data d = get(sp);
        ModNetwork.sendTo(sp, new EquipSyncPacket(d.pets, d.relics, d.capes[0], petSlots(sp), relicSlots(sp)));
        broadcastCape(sp);
    }

    // ------------------------------------------------------------------ capa

    private static final Map<UUID, String> CAPE_SENT = new HashMap<>();

    /** Id de la capa equipada ("" si no lleva). */
    public static String capeId(ServerPlayer sp) {
        ItemStack c = get(sp).capes[0];
        return c.getItem() instanceof com.mundosdeaxelis.item.CapeItem ci ? ci.def().id() : "";
    }

    /** Avisa a todos los que ven al jugador (y a él mismo) de qué capa lleva, para que se dibuje en su espalda. */
    public static void broadcastCape(ServerPlayer sp) {
        String id = capeId(sp);
        CAPE_SENT.put(sp.getUUID(), id);
        ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                new com.mundosdeaxelis.network.CapeSyncPacket(sp.getUUID(), id));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof ServerPlayer target && e.getEntity() instanceof ServerPlayer viewer) {
            ModNetwork.sendTo(viewer, new com.mundosdeaxelis.network.CapeSyncPacket(target.getUUID(), capeId(target)));
        }
    }

    // ------------------------------------------------------------------ consultas

    /** Si lo equipado incluye ese objeto, devuelve su objeto; si no, vacío. */
    public static ItemStack find(ServerPlayer sp, Supplier<? extends Item> item) {
        Item it = item.get();
        Data d = get(sp);
        for (int i = 0; i < relicSlots(sp); i++) {
            if (d.relics[i].is(it)) {
                return d.relics[i];
            }
        }
        for (int i = 0; i < petSlots(sp); i++) {
            if (d.pets[i].is(it)) {
                return d.pets[i];
            }
        }
        return ItemStack.EMPTY;
    }

    /** ¿Está ese objeto en alguna ranura de reliquia o mascota (excepto la indicada)? Para no equipar dos iguales. */
    public static boolean hasOther(ItemStack[] arr, Item it, int exceptSlot) {
        for (int i = 0; i < arr.length; i++) {
            if (i != exceptSlot && arr[i].is(it)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ eventos

    private static final UUID PET_ARMOR_ID = UUID.fromString("5d0e7a52-3c1b-4f6e-9a28-6b1c0d4e9f17");

    /** Armadura extra por las mascotas y la capa equipadas (siempre números enteros) (un único modificador temporal que se recalcula cada medio segundo). */
    private static void applyPetArmor(ServerPlayer sp, float armor) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance inst = sp.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        if (inst == null) {
            return;
        }
        net.minecraft.world.entity.ai.attributes.AttributeModifier old = inst.getModifier(PET_ARMOR_ID);
        if (old != null && Math.abs(old.getAmount() - armor) < 0.001D) {
            return;
        }
        if (old != null) {
            inst.removeModifier(PET_ARMOR_ID);
        }
        if (armor > 0.0F) {
            inst.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(PET_ARMOR_ID, "mdc_pet_armor", armor,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp) || !sp.isAlive()) {
            return;
        }
        Data d = get(sp);
        long t = sp.level().getGameTime();
        int np = petSlots(sp), nr = relicSlots(sp);
        for (int i = 0; i < nr; i++) {
            if (d.relics[i].getItem() instanceof RelicItem r) {
                r.tickEquipped(sp, d.relics[i], i, t);
            }
        }
        for (int i = 0; i < np; i++) {
            if (d.pets[i].getItem() instanceof RelicItem r) {
                r.tickEquipped(sp, d.pets[i], i, t);
            }
        }
        // las mascotas equipadas te acompañan siempre (sin acciones: solo su pasiva)
        if ((t + sp.getId()) % 10 == 0) {
            java.util.List<Companions.Spec> want = new java.util.ArrayList<>();
            int armor = 0;
            for (int i = 0; i < np; i++) {
                if (d.pets[i].getItem() instanceof CompanionItem ci && !want.contains(ci.spec())) {
                    want.add(ci.spec());
                    armor += com.mundosdeaxelis.crate.CratePools.petArmor(ci.spec().id());
                }
            }
            if (d.capes[0].getItem() instanceof com.mundosdeaxelis.item.CapeItem cape) {
                armor += cape.def().armor();
            }
            Companions.follow(sp, want);
            applyPetArmor(sp, (float) armor);
        }
        // si cambió la capa (por el menú, clic derecho o un comando), avisa a los demás
        if ((t + sp.getId()) % 20 == 0 && !capeId(sp).equals(CAPE_SENT.getOrDefault(sp.getUUID(), ""))) {
            broadcastCape(sp);
        }
        // guarda de vez en cuando (los usos y enfriamientos cambian los datos de los objetos)
        if ((t + sp.getId()) % 200 == 0) {
            save(sp);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            CACHE.remove(sp.getUUID());
            sync(sp);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            save(sp);
            CACHE.remove(sp.getUUID());
            CAPE_SENT.remove(sp.getUUID());
        }
    }

    @SubscribeEvent
    public static void onSaveToFile(PlayerEvent.SaveToFile e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            save(sp);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (e.getOriginal() instanceof ServerPlayer old && e.getEntity() instanceof ServerPlayer neu) {
            save(old);
            if (old.getPersistentData().contains(KEY)) {
                neu.getPersistentData().put(KEY, old.getPersistentData().getCompound(KEY).copy());
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        CACHE.clear();
    }
}
