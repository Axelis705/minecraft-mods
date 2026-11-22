package com.mundosdeaxelis.block;

import com.mundosdeaxelis.registry.ModBlockEntities;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.world.Elites;
import com.mundosdeaxelis.world.Sellos;
import com.mundosdeaxelis.world.StructBp;
import com.mundosdeaxelis.world.StructPlacer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * La lógica del {@link AltarMalditoBlock}: listo → 3 oleadas (con barra de progreso) → botín → descansa un día.
 * Si todos los jugadores se van lejos durante medio minuto, el desafío se cancela y el altar vuelve a estar listo.
 */
public class AltarMalditoBlockEntity extends BlockEntity {
    public static final int IDLE = 0, RUNNING = 1, RESTING = 2;
    public static final int WAVES = 3;
    public static final long REST = 24000L;
    private static final double RANGE = 32.0D;

    private int tier = 1;
    private int state = IDLE;
    private int wave;
    private int waveDelay;
    private int away;
    private int waveSize;
    private long rechargeAt;
    private final List<UUID> mobs = new ArrayList<>();
    private ServerBossEvent bar;

    public AltarMalditoBlockEntity(BlockPos pos, BlockState st) {
        super(ModBlockEntities.ALTAR_MALDITO.get(), pos, st);
    }

    public int tier() {
        return tier;
    }

    public int state() {
        return state;
    }

    public int wave() {
        return wave;
    }

    public List<UUID> mobs() {
        return mobs;
    }

    public void setTier(int t) {
        tier = Math.max(1, Math.min(8, t));
        setChanged();
    }

    // ------------------------------------------------------------------ empezar

    public void activate(ServerPlayer sp) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        if (state == RESTING) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.altar.descansa").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (state == RUNNING) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.altar.en_marcha", wave, WAVES).withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!Sellos.canLoot(sp, tier)) {
            Sellos.deny(sp, worldPosition, tier, "altar");
            return;
        }
        state = RUNNING;
        wave = 0;
        waveDelay = 50;
        away = 0;
        mobs.clear();
        setChanged();
        sl.playSound(null, worldPosition, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 0.5F, 1.6F);
        sl.playSound(null, worldPosition, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 40, 0.3, 0.6, 0.3, 0.08);
        for (ServerPlayer p : players(sl)) {
            p.displayClientMessage(Component.translatable("message.mundos_de_axelis.altar.despierta", WAVES).withStyle(ChatFormatting.DARK_RED), true);
        }
    }

    private List<ServerPlayer> players(ServerLevel sl) {
        return sl.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(RANGE),
                p -> !p.isSpectator() && p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5) < RANGE * RANGE);
    }

    // ------------------------------------------------------------------ cada tick

    public static void serverTick(Level level, BlockPos pos, BlockState st, AltarMalditoBlockEntity be) {
        if (level instanceof ServerLevel sl) {
            be.tick(sl);
        }
    }

    private void tick(ServerLevel sl) {
        tick(sl, state == RUNNING ? players(sl) : List.of());
    }

    /** Un tick con esos jugadores cerca (las pruebas lo llaman con un jugador falso). */
    public void tick(ServerLevel sl, List<ServerPlayer> near) {
        if (state == RESTING) {
            if (sl.getGameTime() >= rechargeAt) {
                state = IDLE;
                setCharged(true);
                setChanged();
            }
            return;
        }
        if (state != RUNNING) {
            return;
        }
        updateBar(sl, near);
        if (near.isEmpty()) {
            if (++away > 600) {
                cancel(sl);
            }
            return;
        }
        away = 0;
        mobs.removeIf(id -> {
            Entity e = sl.getEntity(id);
            return e == null || !e.isAlive();
        });
        if (!mobs.isEmpty()) {
            return;
        }
        if (wave >= WAVES) {
            complete(sl, near);
            return;
        }
        if (--waveDelay > 0) {
            if (waveDelay == 20) {
                sl.playSound(null, worldPosition, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0F, 0.8F);
            }
            return;
        }
        wave++;
        spawnWave(sl, near.size());
        waveDelay = 60;
        setChanged();
    }

    private void updateBar(ServerLevel sl, List<ServerPlayer> near) {
        if (bar == null) {
            bar = new ServerBossEvent(Component.translatable("bossbar.mundos_de_axelis.altar"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);
            com.mundosdeaxelis.world.BarrasLejanas.track(bar, this);
        }
        float done = Math.max(0, wave - 1) + (waveSize <= 0 ? 0.0F : (waveSize - mobs.size()) / (float) waveSize);
        if (wave == 0) {
            done = 0.0F;
        }
        bar.setProgress(Math.max(0.0F, Math.min(1.0F, 1.0F - done / WAVES)));
        bar.setName(Component.translatable("bossbar.mundos_de_axelis.altar.oleada", Math.max(1, wave), WAVES));
        for (ServerPlayer p : new ArrayList<>(bar.getPlayers())) {
            if (!near.contains(p)) {
                bar.removePlayer(p);
            }
        }
        for (ServerPlayer p : near) {
            bar.addPlayer(p);
        }
    }

    private void clearBar() {
        if (bar != null) {
            bar.removeAllPlayers();
            com.mundosdeaxelis.world.BarrasLejanas.untrack(bar);
            bar = null;
        }
    }

    // ------------------------------------------------------------------ oleadas

    /** Cuántos enemigos trae una oleada: 3, 4 y 5 (más uno por cada jugador extra, hasta 7). */
    public static int waveCount(int wave, int players) {
        return Math.min(7, 2 + wave + Math.max(0, players - 1));
    }

    private void spawnWave(ServerLevel sl, int players) {
        List<String> kinds = Elites.mobsOfTier(tier);
        if (kinds.isEmpty()) {
            return;
        }
        RandomSource r = sl.random;
        int n = waveCount(wave, players);
        List<BlockPos> spots = spots(sl, r, n);
        mobs.clear();
        for (int i = 0; i < spots.size(); i++) {
            BlockPos p = spots.get(i);
            String kind = kinds.get(r.nextInt(kinds.size()));
            UUID id = StructPlacer.spawn(sl, new StructBp.Mob(0, 0, 0, kind), p, i, null);
            if (id == null) {
                continue;
            }
            mobs.add(id);
            Entity e = sl.getEntity(id);
            if (e instanceof Mob m) {
                m.getPersistentData().putBoolean("mdc_altar", true);
                if (wave == WAVES && i == 0) {
                    Elites.make(m, tier, Elites.Afijo.values()[r.nextInt(Elites.Afijo.values().length)]);
                }
                Entity target = sl.getNearestPlayer(m, RANGE);
                if (target instanceof net.minecraft.world.entity.LivingEntity le && !(target instanceof net.minecraft.world.entity.player.Player pl && (pl.isCreative() || pl.isSpectator()))) {
                    m.setTarget(le);
                }
            }
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 20, 0.3, 0.8, 0.3, 0.03);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 0.3, p.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
        }
        waveSize = mobs.size();
        sl.playSound(null, worldPosition, SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 0.7F);
        for (ServerPlayer p : players(sl)) {
            p.displayClientMessage(Component.translatable("message.mundos_de_axelis.altar.oleada", wave, WAVES).withStyle(ChatFormatting.RED), true);
        }
    }

    /** Sitios pisables a 3–6 bloques del altar, a la misma altura (±2). */
    private List<BlockPos> spots(ServerLevel sl, RandomSource r, int n) {
        List<BlockPos> out = new ArrayList<>();
        for (int tries = 0; tries < 80 && out.size() < n; tries++) {
            double ang = r.nextDouble() * Math.PI * 2;
            int d = 3 + r.nextInt(4);
            int x = worldPosition.getX() + (int) Math.round(Math.cos(ang) * d);
            int z = worldPosition.getZ() + (int) Math.round(Math.sin(ang) * d);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos p = new BlockPos(x, worldPosition.getY() + dy, z);
                if (sl.getBlockState(p).getCollisionShape(sl, p).isEmpty() && sl.getBlockState(p.above()).getCollisionShape(sl, p.above()).isEmpty()
                        && sl.getBlockState(p.below()).isFaceSturdy(sl, p.below(), Direction.UP) && sl.getFluidState(p).isEmpty() && !out.contains(p)) {
                    out.add(p);
                    break;
                }
            }
        }
        if (out.isEmpty()) {
            out.add(worldPosition.above());
        }
        return out;
    }

    // ------------------------------------------------------------------ final

    /** El botín del altar de ese tier: un cofre con cerradura de nivel 3 y una pieza buena segura. */
    public static List<ItemStack> loot(int tier, RandomSource r, java.util.Set<String> archive) {
        List<ItemStack> out = new ArrayList<>(VaultLoot.roll(VaultLoot.Kind.LOCKED, tier, r, 3, archive));
        if (r.nextInt(100) < 40) {
            out.add(new ItemStack(ModItems.HEART_FRAGMENT.get()));
        } else {
            out.add(VaultLoot.emptyGem(tier));
        }
        return out;
    }

    private void complete(ServerLevel sl, List<ServerPlayer> near) {
        clearBar();
        state = RESTING;
        rechargeAt = sl.getGameTime() + REST;
        setCharged(false);
        setChanged();
        ServerPlayer first = near.stream().filter(p -> Sellos.canLoot(p, tier)).findFirst().orElse(null);
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.1, z = worldPosition.getZ() + 0.5;
        if (first != null) {
            for (ItemStack it : loot(tier, sl.random, com.mundosdeaxelis.table.Essence.archive(first))) {
                ItemEntity ie = new ItemEntity(sl, x, y, z, it);
                ie.setDeltaMovement((sl.random.nextDouble() - 0.5) * 0.3, 0.35 + sl.random.nextDouble() * 0.2, (sl.random.nextDouble() - 0.5) * 0.3);
                ie.setDefaultPickUpDelay();
                sl.addFreshEntity(ie);
            }
        }
        int ess = VaultLoot.essence(VaultLoot.Kind.TRIAL, tier);
        for (ServerPlayer p : near) {
            if (Sellos.canLoot(p, tier)) {
                com.mundosdeaxelis.table.Essence.add(p, ess);
                com.mundosdeaxelis.table.Essence.sync(p);
                p.getPersistentData().putInt("mdc_altares", p.getPersistentData().getInt("mdc_altares") + 1);
            }
            p.displayClientMessage(Component.translatable("message.mundos_de_axelis.altar.vencido").withStyle(ChatFormatting.GOLD), true);
        }
        sl.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.0F);
        sl.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 40, 0.4, 0.6, 0.4, 0.4);
    }

    private void cancel(ServerLevel sl) {
        for (UUID id : mobs) {
            Entity e = sl.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
        mobs.clear();
        clearBar();
        state = IDLE;
        wave = 0;
        setChanged();
    }

    private void setCharged(boolean c) {
        if (level != null) {
            BlockState st = getBlockState();
            if (st.hasProperty(AltarMalditoBlock.CARGADO) && st.getValue(AltarMalditoBlock.CARGADO) != c) {
                level.setBlock(worldPosition, st.setValue(AltarMalditoBlock.CARGADO, c), 3);
            }
        }
    }

    /** Para las pruebas: lo deja listo otra vez ya mismo. */
    public void recharge() {
        state = IDLE;
        rechargeAt = 0;
        setCharged(true);
        setChanged();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        clearBar();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        clearBar();
    }

    // ------------------------------------------------------------------ guardar

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Tier", tier);
        tag.putInt("State", state);
        tag.putInt("Wave", wave);
        tag.putInt("WaveSize", waveSize);
        tag.putLong("Recharge", rechargeAt);
        ListTag l = new ListTag();
        for (UUID id : mobs) {
            l.add(NbtUtils.createUUID(id));
        }
        tag.put("Mobs", l);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tier = tag.contains("Tier") ? Math.max(1, Math.min(8, tag.getInt("Tier"))) : 1;
        state = tag.getInt("State");
        wave = tag.getInt("Wave");
        waveSize = tag.getInt("WaveSize");
        rechargeAt = tag.getLong("Recharge");
        mobs.clear();
        for (Tag t : tag.getList("Mobs", Tag.TAG_INT_ARRAY)) {
            mobs.add(NbtUtils.loadUUID(t));
        }
    }
}
