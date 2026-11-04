package com.mundosdeaxelis.block;

import com.mundosdeaxelis.network.LockpickOpenPacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.registry.ModBlockEntities;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.table.Essence;
import com.mundosdeaxelis.world.DungeonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Datos de un cofre del santuario: el botín de CADA jugador se genera la primera vez que lo abre y se guarda aparte (así nadie
 * se pelea por el contenido), y en los cofres sellados se apunta quién ya usó su llave.
 */
public class VaultChestBlockEntity extends BlockEntity {
    private VaultLoot.Kind kind = VaultLoot.Kind.TRIAL;
    private int stage = 1;
    private final Map<UUID, SimpleContainer> loots = new HashMap<>();
    private final Set<UUID> unlocked = new HashSet<>();
    private int lockLevel = 1;
    /** Cuando alguien abre la cerradura, el cofre queda abierto para todos (cada uno con su botín). */
    private boolean picked = false;
    private static final int SESSION_TICKS = 20 * 90;
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private record Session(BlockPos pos, long start) {
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final int FAIL_COOLDOWN = 400;
    private static final int MIN_TICKS = 30;

    public VaultChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VAULT.get(), pos, state);
        if (state.getBlock() instanceof VaultChestBlock b && b.locked) {
            kind = VaultLoot.Kind.LOCKED;
        }
    }

    public void configureLock(int level) {
        this.lockLevel = Math.max(1, Math.min(5, level));
        setChanged();
    }

    public boolean locked() {
        return getBlockState().getBlock() instanceof VaultChestBlock b && b.locked;
    }

    /** El minijuego terminó en el cliente: 0 = salió, 1 = abierta, 2 = fallada. El servidor comprueba que de verdad había una cerradura en marcha. */
    public static void onLockResult(ServerPlayer sp, BlockPos pos, int code) {
        Session ss = SESSIONS.remove(sp.getUUID());
        if (ss == null || !ss.pos().equals(pos) || !(sp.level() instanceof ServerLevel sl) || sp.blockPosition().distSqr(pos) > 100.0
                || !(sl.getBlockEntity(pos) instanceof VaultChestBlockEntity be) || !be.locked()) {
            return;
        }
        UUID id = sp.getUUID();
        if (code == 1 && sl.getGameTime() - ss.start() >= MIN_TICKS) {
            be.picked = true;
            be.unlocked.add(id);
            be.setChanged();
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.2);
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.vault.lock_open"), true);
            be.open(sp);
        } else if (code == 2) {
            be.cooldown.put(id, sl.getGameTime() + FAIL_COOLDOWN);
            sl.playSound(null, pos, ModSounds.LOCK_MISS.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.vault.lock_fail"), true);
        }
    }

    public void configure(VaultLoot.Kind kind, int stage) {
        this.kind = kind;
        this.stage = stage;
        setChanged();
    }

    public boolean sealed() {
        return getBlockState().getBlock() instanceof VaultChestBlock b && b.sealed;
    }

    /** Llave de la dungeon de la etapa: Llave de Hierro en la Forja (etapa 2), Llave del Santuario en el bosque. */
    private static net.minecraft.world.item.Item keyOf(int stage) {
        return stage == 2 ? ModItems.FORGE_KEY.get() : ModItems.DUNGEON_KEY.get();
    }

    private static ItemStack findKey(ServerPlayer sp, int stage) {
        net.minecraft.world.item.Item want = keyOf(stage);
        for (ItemStack s : sp.getInventory().items) {
            if (s.is(want)) {
                return s;
            }
        }
        ItemStack off = sp.getOffhandItem();
        return off.is(want) ? off : ItemStack.EMPTY;
    }

    private String themed(String key) {
        return stage == 2 ? key + "_forge" : key;
    }

    public void open(ServerPlayer sp) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        UUID id = sp.getUUID();
        if (locked() && !picked && !unlocked.contains(id)) {
            long now = sl.getGameTime();
            for (Map.Entry<UUID, Session> en : SESSIONS.entrySet()) {
                Session o = en.getValue();
                if (!en.getKey().equals(id) && o.pos().equals(worldPosition) && now - o.start() < SESSION_TICKS) {
                    ServerPlayer other = sl.getServer().getPlayerList().getPlayer(en.getKey());
                    if (other != null && !other.hasDisconnected()) {
                        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.vault.lock_other", other.getDisplayName()), true);
                        return;
                    }
                }
            }
            if (DungeonManager.of(sl).roomBusy(sl, worldPosition) || com.mundosdeaxelis.world.ForgeManager.of(sl).roomBusy(sl, worldPosition)) {
                sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.vault.lock_busy"), true);
                return;
            }
            long left = cooldown.getOrDefault(id, 0L) - sl.getGameTime();
            if (left > 0) {
                sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.vault.lock_wait", (int) Math.ceil(left / 20.0)), true);
                return;
            }
            SESSIONS.put(id, new Session(worldPosition, sl.getGameTime()));
            ModNetwork.sendTo(sp, new LockpickOpenPacket(worldPosition, lockLevel, sl.random.nextLong()));
            return;
        }
        if (sealed() && !unlocked.contains(id)) {
            ItemStack key = findKey(sp, stage);
            if (key.isEmpty()) {
                sp.displayClientMessage(Component.translatable(themed("message.mundos_de_axelis.vault.need_key")), true);
                sl.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.8F, 1.0F);
                return;
            }
            key.shrink(1);
            unlocked.add(id);
            setChanged();
            sl.playSound(null, worldPosition, ModSounds.KEY_USE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            sl.sendParticles(ParticleTypes.WAX_OFF, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5, 14, 0.3, 0.3, 0.3, 0.1);
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.vault.unlocked"), true);
        }
        SimpleContainer c = loots.get(id);
        if (c == null) {
            c = new SimpleContainer(27);
            List<ItemStack> items = VaultLoot.roll(kind, stage, sp.getRandom(), lockLevel);
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < 27; i++) {
                slots.add(i);
            }
            Collections.shuffle(slots, new java.util.Random(sp.getRandom().nextLong()));
            for (int i = 0; i < items.size() && i < 27; i++) {
                c.setItem(slots.get(i), items.get(i));
            }
            c.addListener(x -> setChanged());
            loots.put(id, c);
            int ess = VaultLoot.essence(kind, stage);
            Essence.add(sp, ess);
            Essence.sync(sp);
            com.mundosdeaxelis.world.Titles.lootEssence(sp, ess);
            sl.playSound(null, worldPosition, ModSounds.VAULT_OPEN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 12, 0.4, 0.3, 0.4, 0.0);
            setChanged();
        } else {
            sl.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        final SimpleContainer container = c;
        sp.openMenu(new SimpleMenuProvider((wid, inv, p) -> ChestMenu.threeRows(wid, inv, container),
                Component.translatable(themed("container.mundos_de_axelis.vault_" + kind.name().toLowerCase(java.util.Locale.ROOT)))));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Kind", kind.name());
        tag.putInt("Stage", stage);
        tag.putInt("LockLevel", lockLevel);
        tag.putBoolean("Picked", picked);
        ListTag u = new ListTag();
        for (UUID id : unlocked) {
            u.add(StringTag.valueOf(id.toString()));
        }
        tag.put("Unlocked", u);
        ListTag l = new ListTag();
        for (Map.Entry<UUID, SimpleContainer> e : loots.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", e.getKey());
            t.put("Items", e.getValue().createTag());
            l.add(t);
        }
        tag.put("Loots", l);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        try {
            kind = VaultLoot.Kind.valueOf(tag.getString("Kind"));
        } catch (IllegalArgumentException ex) {
            kind = VaultLoot.Kind.TRIAL;
        }
        stage = Math.max(1, tag.getInt("Stage"));
        lockLevel = Math.max(1, Math.min(5, tag.contains("LockLevel") ? tag.getInt("LockLevel") : 1));
        picked = tag.getBoolean("Picked");
        unlocked.clear();
        for (Tag t : tag.getList("Unlocked", Tag.TAG_STRING)) {
            try {
                unlocked.add(UUID.fromString(t.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        loots.clear();
        for (Tag t : tag.getList("Loots", Tag.TAG_COMPOUND)) {
            CompoundTag ct = (CompoundTag) t;
            SimpleContainer c = new SimpleContainer(27);
            c.fromTag(ct.getList("Items", Tag.TAG_COMPOUND));
            c.addListener(x -> setChanged());
            loots.put(ct.getUUID("Id"), c);
        }
    }
}
