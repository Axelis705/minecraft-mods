package com.mundosdeaxelis.table;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.crate.Tiers;
import com.mundosdeaxelis.item.MaterialItem;
import com.mundosdeaxelis.network.EssenceSyncPacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.relic.Equipment;
import com.mundosdeaxelis.relic.RelicItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Datos personales de la Mesa del Artífice: Esencia, nivel de mesa y Archivo (lo descubierto). Se guardan con el jugador
 * (por eso son de cada quien, también en multijugador) y viajan con él a cualquier mundo.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Essence {
    private Essence() {
    }

    private static final String KEY = "mdc_table";

    // ------------------------------------------------------------------ Archivo: lista de entradas

    /** Una entrada del Archivo. */
    public record Entry(String cat, String id, Tier tier) {
        public Item item() {
            return ForgeRegistries.ITEMS.getValue(new ResourceLocation(MundosDeAxelis.MODID, id));
        }
    }

    public static final String[] CATS = {"relics", "pets", "gear", "capes", "mats"};
    private static List<Entry> entries;

    /** Todas las entradas del Archivo, agrupadas por categoría y ordenadas por rareza (se calcula una sola vez). */
    public static List<Entry> entries() {
        if (entries == null) {
            List<Entry> l = new ArrayList<>();
            Tiers.all().forEach((id, t) -> {
                String cat = id.startsWith("comp_") ? "pets"
                        : id.startsWith("gear_") || com.mundosdeaxelis.gear.GearDefs.get(id) != null || com.mundosdeaxelis.gear.ArmorDefs.isArmorId(id) ? "gear" : "relics";
                l.add(new Entry(cat, id, t));
            });
            Comparator<Entry> cmp = Comparator.comparingInt((Entry e) -> e.tier.ordinal()).thenComparing(Entry::id);
            l.sort(cmp);
            for (com.mundosdeaxelis.cape.CapeDefs.Def d : com.mundosdeaxelis.cape.CapeDefs.ALL) {
                l.add(new Entry("capes", d.id(), d.tier()));
            }
            for (Mats m : Mats.values()) {
                l.add(new Entry("mats", m.itemId(), m.rarity == net.minecraft.world.item.Rarity.EPIC ? Tier.EPIC
                        : m.rarity == net.minecraft.world.item.Rarity.RARE ? Tier.RARE : m.rarity == net.minecraft.world.item.Rarity.UNCOMMON ? Tier.UNCOMMON : Tier.COMMON));
            }
            entries = l;
        }
        return entries;
    }

    public static List<Entry> entries(String cat) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries()) {
            if (e.cat.equals(cat)) {
                out.add(e);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ datos del jugador

    private static CompoundTag tag(Player p, boolean create) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(KEY, Tag.TAG_COMPOUND)) {
            if (!create) {
                return new CompoundTag();
            }
            root.put(KEY, new CompoundTag());
        }
        return root.getCompound(KEY);
    }

    public static int get(Player p) {
        return tag(p, false).getInt("essence");
    }

    /** Nivel personal de mesa (1 a 4). */
    public static int level(Player p) {
        int l = tag(p, false).getInt("level");
        return Math.max(1, Math.min(TableLevels.MAX, l));
    }

    public static void setLevel(Player p, int level) {
        tag(p, true).putInt("level", Math.max(1, Math.min(TableLevels.MAX, level)));
    }

    public static void add(ServerPlayer sp, int n) {
        if (n > 0) {
            CompoundTag t = tag(sp, true);
            t.putInt("essence", (int) Math.min(Integer.MAX_VALUE / 2, (long) t.getInt("essence") + n));
        }
    }

    public static boolean spend(ServerPlayer sp, int n) {
        CompoundTag t = tag(sp, true);
        if (t.getInt("essence") < n) {
            return false;
        }
        t.putInt("essence", t.getInt("essence") - n);
        return true;
    }

    /** Intentos seguidos sin conseguir un extra de jefe (protección contra la mala suerte). */
    public static int bossPity(Player p, String key) {
        return tag(p, false).getCompound("bosspity").getInt(key);
    }

    public static void setBossPity(Player p, String key, int v) {
        CompoundTag t = tag(p, true);
        CompoundTag c = t.getCompound("bosspity");
        c.putInt(key, Math.max(0, v));
        t.put("bosspity", c);
    }

    /**
     * ¿Este jugador ya venció al jefe de esa etapa? Se apunta al repartir las recompensas; además cuenta el sello de la etapa
     * en su Archivo (así los jugadores que vencieron a los primeros jefes antes de existir esta marca no lo ven oculto).
     */
    public static boolean bossBeaten(Player p, int stage) {
        if (tag(p, false).getCompound("bossbeaten").getBoolean("s" + stage)) {
            return true;
        }
        if (stage < 1 || stage >= com.mundosdeaxelis.crate.CrateKind.values().length) {
            return false;
        }
        return archive(p).contains("seal_" + com.mundosdeaxelis.crate.CrateKind.byIndex(stage).id);
    }

    public static void setBossBeaten(Player p, int stage) {
        CompoundTag t = tag(p, true);
        CompoundTag c = t.getCompound("bossbeaten");
        c.putBoolean("s" + stage, true);
        t.put("bossbeaten", c);
    }

    /** Contadores de garantía de una caja: aperturas seguidas sin Épica, sin Legendaria y sin Mítica. */
    public static int[] pity(Player p, String crateId) {
        int[] v = tag(p, false).getCompound("pity").getIntArray(crateId);
        return v.length == 3 ? v : new int[3];
    }

    public static void setPity(Player p, String crateId, int[] v) {
        CompoundTag t = tag(p, true);
        CompoundTag pt = t.getCompound("pity");
        pt.putIntArray(crateId, v);
        t.put("pity", pt);
    }

    /** Todos los contadores (id de caja -> contadores) para enviarlos al cliente. */
    public static java.util.Map<String, int[]> allPity(Player p) {
        java.util.Map<String, int[]> out = new java.util.LinkedHashMap<>();
        CompoundTag pt = tag(p, false).getCompound("pity");
        for (String k : pt.getAllKeys()) {
            int[] v = pt.getIntArray(k);
            if (v.length == 3) {
                out.put(k, v);
            }
        }
        return out;
    }

    public static Set<String> archive(Player p) {
        Set<String> s = new LinkedHashSet<>();
        ListTag l = tag(p, false).getList("archive", Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) {
            s.add(l.getString(i));
        }
        return s;
    }

    /** Apunta un objeto en el Archivo. Devuelve true si es nuevo. */
    public static boolean discover(ServerPlayer sp, String id) {
        CompoundTag t = tag(sp, true);
        ListTag l = t.getList("archive", Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) {
            if (l.getString(i).equals(id)) {
                return false;
            }
        }
        l.add(StringTag.valueOf(id));
        t.put("archive", l);
        return true;
    }

    /**
     * Registra el progreso del jugador (jefes vencidos) en su Archivo: apunta los sellos de las etapas 1..{@code stage}, aunque
     * no los lleve encima. Eso es lo que abre las cajas siguientes (ver {@code CrateForge.stage}); la Mesa ya no interviene.
     */
    public static boolean registerProgress(ServerPlayer sp, int stage) {
        boolean fresh = false;
        for (com.mundosdeaxelis.crate.CrateKind k : com.mundosdeaxelis.crate.CrateKind.values()) {
            if (k != com.mundosdeaxelis.crate.CrateKind.PETS && k.rank() <= stage) {
                fresh |= discover(sp, "seal_" + k.id);
            }
        }
        if (fresh) {
            sync(sp);
            if (!com.mundosdeaxelis.world.BossRewards.hadResult(sp)) { // si no, ya salió en la pantalla de resultados
                sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.progress_new", Math.max(1, stage))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
        }
        return fresh;
    }

    // ------------------------------------------------------------------ sincronización

    public static void sync(ServerPlayer sp) {
        sync(sp, null, false);
    }

    /** Avisa al cliente de sus datos; el aviso (si lo hay) sale en la pantalla de la mesa. */
    public static void sync(ServerPlayer sp, Component toast, boolean good) {
        ModNetwork.sendTo(sp, new EssenceSyncPacket(get(sp), level(sp), archive(sp), allPity(sp), toast, good));
    }

    // ------------------------------------------------------------------ eventos

    private static void scan(ServerPlayer sp) {
        boolean fresh = false;
        List<ItemStack> all = new ArrayList<>(sp.getInventory().items);
        all.addAll(sp.getInventory().offhand);
        all.addAll(sp.getInventory().armor); // la armadura puesta también se apunta
        Equipment.Data eq = Equipment.get(sp);
        for (ItemStack s : eq.pets) {
            all.add(s);
        }
        for (ItemStack s : eq.relics) {
            all.add(s);
        }
        for (ItemStack s : eq.capes) {
            all.add(s);
        }
        for (ItemStack s : all) {
            if (s.isEmpty() || !(s.getItem() instanceof RelicItem || s.getItem() instanceof MaterialItem || s.getItem() instanceof com.mundosdeaxelis.item.CapeItem || s.getItem() instanceof com.mundosdeaxelis.item.SealItem || s.getItem() instanceof com.mundosdeaxelis.gear.GearTool || s.getItem() instanceof com.mundosdeaxelis.gear.SetArmor)) {
                continue;
            }
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(s.getItem());
            if (rl != null && MundosDeAxelis.MODID.equals(rl.getNamespace()) && discover(sp, rl.getPath())) {
                fresh = true;
                sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.archive_new", s.getHoverName())
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
        if (fresh) {
            sync(sp);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer sp && sp.isAlive() && (sp.tickCount + sp.getId()) % 40 == 0) {
            scan(sp);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            // el progreso del mundo (jefes vencidos) también cuenta para quien no estaba conectado cuando se venció
            int lv = com.mundosdeaxelis.world.WorldTier.get(sp.server).level();
            if (lv > 0) {
                registerProgress(sp, lv);
            }
            sync(sp);
            Equipment.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        CompoundTag old = e.getOriginal().getPersistentData();
        if (old.contains(KEY, Tag.TAG_COMPOUND)) {
            e.getEntity().getPersistentData().put(KEY, old.getCompound(KEY).copy());
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
}
