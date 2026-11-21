package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.block.VaultLoot;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.world.DungeonBalance;
import com.mundosdeaxelis.world.Sellos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Etapa 276: el <b>Saqueador del Tesoro</b> (la idea del duende ladrón de Diablo). Un bicho pequeño con un saco lleno de botín
 * que aparece muy de vez en cuando dentro de una estructura que ya puedes saquear. Cuando te ve, se ríe y huye soltando monedas;
 * tienes unos 22 segundos para atraparlo antes de que abra un portal y escape. Si lo matas, revienta el saco: una lluvia de
 * botín de su tier (runas de esencia, esencias, arrojadizos, fragmentos de llave, a veces una gema vacía o un fragmento de corazón).
 * No ataca. Los arrojadizos (sobre todo el Orbe de Escarcha) ayudan mucho a cazarlo.
 */
public class Saqueador extends PathfinderMob implements Enemy {
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(Saqueador.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SPOTTED = SynchedEntityData.defineId(Saqueador.class, EntityDataSerializers.BOOLEAN);
    /** Segundos que tarda en escapar desde que te ve. */
    public static final int ESCAPE_TICKS = 22 * 20;
    private static final float HITS = 8.0F;

    private int escapeIn = -1;
    private int lonely;
    private boolean scaled;

    public Saqueador(EntityType<? extends Saqueador> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, DungeonBalance.hp(1, HITS))
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TIER, 1);
        this.entityData.define(SPOTTED, false);
    }

    public int tier() {
        return this.entityData.get(TIER);
    }

    public boolean spotted() {
        return this.entityData.get(SPOTTED);
    }

    public int escapeIn() {
        return escapeIn;
    }

    public void setTier(int t) {
        t = Mth.clamp(t, 1, 8);
        this.entityData.set(TIER, t);
        float hp = DungeonBalance.hp(t, HITS);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        setHealth(hp);
        getAttribute(Attributes.ARMOR).setBaseValue(1.0D + t);
        this.xpReward = 15 + 10 * t;
        this.scaled = true;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 12.0F, 1.2D, 1.5D,
                p -> !((Player) p).isCreative() && !p.isSpectator()));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
    }

    // ------------------------------------------------------------------ te ve, huye y escapa

    private void spot() {
        if (spotted() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        this.entityData.set(SPOTTED, true);
        escapeIn = ESCAPE_TICKS;
        playSound(SoundEvents.WITCH_CELEBRATE, 1.0F, 1.7F);
        for (ServerPlayer sp : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(28.0D))) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.saqueador.visto").withStyle(ChatFormatting.GOLD), true);
        }
    }

    /** Abre un portal y se va con su botín. */
    public void escape() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.5, getZ(), 60, 0.3, 0.6, 0.3, 0.15);
        sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 0.5, getZ(), 40, 0.4, 0.6, 0.4, 0.5);
        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.4F);
        sl.playSound(null, blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 0.8F, 2.0F);
        for (ServerPlayer sp : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32.0D))) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.saqueador.escapa").withStyle(ChatFormatting.GRAY), true);
        }
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (spotted() && tickCount % 4 == 0) {
                level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLD_NUGGET)),
                        getX(), getY() + 0.6, getZ(), (random.nextDouble() - 0.5) * 0.1, 0.12, (random.nextDouble() - 0.5) * 0.1);
            }
            return;
        }
        if (!scaled) {
            setTier(tier());
        }
        Player near = level().getNearestPlayer(this, 48.0D);
        if (near == null) {
            if (++lonely > 20 * 60) {
                discard(); // nadie lo vio: se va sin hacer ruido
            }
            return;
        }
        lonely = 0;
        if (!spotted()) {
            Player p = level().getNearestPlayer(this, 13.0D);
            if (p != null && !p.isCreative() && !p.isSpectator() && hasLineOfSight(p)) {
                spot();
            }
            return;
        }
        if (escapeIn > 0) {
            escapeIn--;
            if (tickCount % 10 == 0) {
                playSound(SoundEvents.ARMOR_EQUIP_GOLD, 0.5F, 1.5F + random.nextFloat() * 0.3F);
            }
            if (escapeIn == 60 && level() instanceof ServerLevel sl) {
                sl.playSound(null, blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 0.5F, 1.8F);
            }
            if (escapeIn < 60 && level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 0.5, getZ(), 6, 0.4, 0.5, 0.4, 0.3);
            }
            if (escapeIn == 0) {
                escape();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean ok = super.hurt(source, amount);
        if (ok && !level().isClientSide) {
            spot();
            playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.8F);
            if (random.nextInt(100) < 35 && isAlive()) {
                spawnAtLocation(new ItemStack(Items.GOLD_NUGGET, 1 + random.nextInt(2)), 0.5F);
            }
        }
        return ok;
    }

    // ------------------------------------------------------------------ el saco

    /** El botín del saco de un Saqueador de ese tier. */
    public static List<ItemStack> loot(int tier, RandomSource r) {
        List<ItemStack> out = new ArrayList<>();
        int t = Math.max(1, Math.min(8, tier));
        out.add(new ItemStack(ModItems.ESSENCE_RUNES.get(t).get(), 3 + r.nextInt(3)));
        out.add(new ItemStack(ModItems.ESENCIAS.get(com.mundosdeaxelis.gema.Gemas.Elemento.ofTier(t)).get(), 1 + r.nextInt(2)));
        out.add(new ItemStack(ModItems.ARROJADIZOS.get(com.mundosdeaxelis.arrojadizo.Arrojadizos.Tipo.ofTier(t)).get(), 3 + r.nextInt(3)));
        out.add(new ItemStack(ModItems.KEY_FRAGMENTS.get(com.mundosdeaxelis.crate.CrateKind.byIndex(t)).get(), 1 + r.nextInt(2)));
        out.add(new ItemStack(Items.GOLD_INGOT, 2 + r.nextInt(4)));
        out.add(new ItemStack(Items.GOLD_NUGGET, 6 + r.nextInt(10)));
        if (r.nextInt(100) < 55) {
            out.add(VaultLoot.emptyGem(t));
        }
        if (r.nextInt(100) < 30) {
            out.add(new ItemStack(ModItems.HEART_FRAGMENT.get()));
        }
        if (r.nextInt(100) < 40) {
            out.add(new ItemStack(Items.EMERALD, 2 + r.nextInt(4)));
        }
        return out;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        ServerPlayer sp = source.getEntity() instanceof ServerPlayer p ? p : getLastHurtByMob() instanceof ServerPlayer p2 ? p2 : null;
        if (sp == null) {
            return;
        }
        if (!Sellos.canLoot(sp, tier())) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.sello.mimico", Sellos.bossToBeat(tier()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        for (ItemStack it : loot(tier(), random)) {
            ItemEntity ie = spawnAtLocation(it, 0.7F);
            if (ie != null) {
                ie.setDeltaMovement((random.nextDouble() - 0.5) * 0.45, 0.35 + random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.45);
            }
        }
        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLD_INGOT)), getX(), getY() + 0.7, getZ(), 40, 0.3, 0.3, 0.3, 0.25);
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 0.7, getZ(), 30, 0.3, 0.5, 0.3, 0.35);
        sl.playSound(null, blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 0.8F, 1.2F);
        sl.playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.HOSTILE, 1.0F, 0.8F);
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.saqueador.atrapado").withStyle(ChatFormatting.GOLD), true);
        sp.getPersistentData().putInt("mdc_saqueadores", sp.getPersistentData().getInt("mdc_saqueadores") + 1);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return spotted() ? SoundEvents.WITCH_AMBIENT : null;
    }

    @Override
    public float getVoicePitch() {
        return 1.8F + random.nextFloat() * 0.2F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Tier", tier());
        tag.putBoolean("Spotted", spotted());
        tag.putInt("EscapeIn", escapeIn);
        tag.putBoolean("Scaled", scaled);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Tier")) {
            this.entityData.set(TIER, Mth.clamp(tag.getInt("Tier"), 1, 8));
        }
        this.entityData.set(SPOTTED, tag.getBoolean("Spotted"));
        escapeIn = tag.contains("EscapeIn") ? tag.getInt("EscapeIn") : -1;
        scaled = tag.getBoolean("Scaled");
    }
}
