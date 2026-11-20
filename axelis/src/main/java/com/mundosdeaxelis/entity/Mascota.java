package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.relic.Companions;
import com.mundosdeaxelis.relic.PetActions;
import com.mundosdeaxelis.relic.PetData;
import com.mundosdeaxelis.relic.PetKeeper;
import com.mundosdeaxelis.relic.PetKinds;
import com.mundosdeaxelis.relic.PetMenu;
import com.mundosdeaxelis.relic.RelicFx;
import com.mundosdeaxelis.relic.RelicSounds;
import com.mundosdeaxelis.vfx.Creature3D;
import com.mundosdeaxelis.vfx.VfxData;
import com.mundosdeaxelis.world.BossDamage;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Etapa 254: una mascota equipada hecha criatura de verdad, como los lobos y los gatos de Minecraft. Anda y salta con su
 * propio camino, nada, vuela (las voladoras), se sienta con clic derecho, come, se deja acariciar, tiene vida, recibe golpes
 * (parpadeo rojo) y, si es de combate, te defiende. Agachado + clic derecho abre su menú ({@link PetMenu}).
 * <p>
 * Se ve con el mismo modelo 3D de siempre ({@link Creature3D}), dibujado por {@code client.PetRenderer}. Nunca se guarda con el
 * mundo: la crea y la quita {@link PetKeeper} según las figuritas equipadas, y sus recuerdos van en la figurita ({@link PetData}).
 */
public class Mascota extends TamableAnimal {
    private static final EntityDataAccessor<Integer> SPECIES = SynchedEntityData.defineId(Mascota.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(Mascota.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> LOVE = SynchedEntityData.defineId(Mascota.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> MODE = SynchedEntityData.defineId(Mascota.class, EntityDataSerializers.BYTE);

    /** Modos de las que pelean: defenderte (como el lobo), atacar a todo monstruo cercano, o quedarse tranquila. */
    public static final int MODE_DEFEND = 0, MODE_ATTACK = 1, MODE_CALM = 2;
    public static final int F_LEAVING = 1, F_LIGHT = 2, F_FETCH = 4;
    /** Avisos al cliente (animaciones): contenta, truco, dispara, muerde, come, ruge. */
    public static final byte EV_HAPPY = 90, EV_TRICK = 91, EV_CAST = 92, EV_BITE = 93, EV_EAT = 94, EV_ROAR = 95;

    private PetKinds.Kind kind;
    private Companions.Spec spec;
    private final PathNavigation groundNav;
    private final MoveControl groundMove;
    private FlyingPathNavigation flyingNav;
    private FlyingMoveControl flyingMove;
    private int leaveTicks = -1;
    private BlockPos lightAt;
    public int boltCd;
    private int petCd, loveCd;

    // ---- solo en el cliente: estado de animación del modelo 3D
    public final VfxData anim = new VfxData();
    public int trickT, happyT, castT, biteT, eatT;
    private boolean ownerWasGround = true;
    private int lastHurt;
    public double leaveAge = -1;

    public Mascota(EntityType<? extends Mascota> type, Level level) {
        super(type, level);
        this.groundNav = this.navigation;
        this.groundMove = this.moveControl;
        this.setTame(true);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SPECIES, -1);
        this.entityData.define(FLAGS, (byte) 0);
        this.entityData.define(LOVE, 0);
        this.entityData.define(MODE, (byte) MODE_DEFEND);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new MascotaGoals.Leap(this));
        this.goalSelector.addGoal(4, new MascotaGoals.Bite(this));
        this.goalSelector.addGoal(4, new MascotaGoals.Bolt(this));
        this.goalSelector.addGoal(4, new MascotaGoals.Panic(this));
        this.goalSelector.addGoal(5, new MascotaGoals.Fetch(this));
        this.goalSelector.addGoal(6, new MascotaGoals.Follow(this));
        this.goalSelector.addGoal(7, new MascotaGoals.Wander(this));
        this.goalSelector.addGoal(7, new MascotaGoals.Hover(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new MascotaGoals.OwnerHurtBy(this));
        this.targetSelector.addGoal(2, new MascotaGoals.OwnerHurt(this));
        this.targetSelector.addGoal(3, new MascotaGoals.Retaliate(this));
        this.targetSelector.addGoal(4, new MascotaGoals.Hunt(this));
    }

    // ------------------------------------------------------------------ especie y datos

    /** Prepara la mascota recién creada: especie, dueño y lo que recuerda su figurita. */
    public void setup(ServerPlayer owner, Companions.Spec spec, ItemStack figurine) {
        this.setSpecies(spec.species());
        this.setTame(true);
        this.setOwnerUUID(owner.getUUID());
        String name = PetData.name(figurine);
        if (!name.isEmpty()) {
            super.setCustomName(Component.literal(name));
        }
        this.entityData.set(LOVE, PetData.love(figurine));
        this.entityData.set(MODE, (byte) Mth.clamp(PetData.mode(figurine), 0, 2));
        setFlag(F_LIGHT, kind != null && kind.special == PetKinds.Special.LIGHT && PetData.flag(figurine, "Light"));
        setFlag(F_FETCH, kind != null && kind.special == PetKinds.Special.FETCH && PetData.flag(figurine, "Fetch"));
        this.setHealth(this.getMaxHealth());
    }

    public void setSpecies(int sp) {
        this.entityData.set(SPECIES, sp);
        applyKind();
    }

    public int species() {
        return this.entityData.get(SPECIES);
    }

    public PetKinds.Kind kind() {
        return kind;
    }

    public Companions.Spec spec() {
        return spec;
    }

    /** Nivel de su caja (1 a 8): sale de la armadura que da su figurita. */
    public int tier() {
        if (spec == null) {
            return 1;
        }
        int a = com.mundosdeaxelis.crate.CratePools.petArmor(spec.id());
        return Mth.clamp((a + 1) / 2, 1, 8);
    }

    private void applyKind() {
        int sp = species();
        this.kind = PetKinds.of(sp);
        this.spec = Companions.bySpecies(sp);
        this.refreshDimensions();
        if (kind == null || level().isClientSide) {
            return;
        }
        int t = tier();
        boolean combat = kind.combat();
        set(Attributes.MAX_HEALTH, combat ? 16 + 4 * t : 18 + 2 * t);
        set(Attributes.MOVEMENT_SPEED, kind.speed);
        set(Attributes.FLYING_SPEED, PetKinds.flySpeed(sp));
        set(Attributes.ATTACK_DAMAGE, 2.5 + 0.9 * t);
        set(Attributes.ARMOR, combat ? t : t * 0.5);
        if (kind.fly) {
            if (flyingNav == null) {
                flyingNav = new FlyingPathNavigation(this, level());
                flyingNav.setCanOpenDoors(false);
                flyingNav.setCanFloat(true);
                flyingNav.setCanPassDoors(true);
                flyingMove = new FlyingMoveControl(this, 20, true);
            }
            this.navigation = flyingNav;
            this.moveControl = flyingMove;
            this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
            this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
            this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 16.0F);
        } else {
            this.navigation = groundNav;
            this.moveControl = groundMove;
            this.setPathfindingMalus(BlockPathTypes.POWDER_SNOW, -1.0F);
            this.setPathfindingMalus(BlockPathTypes.DANGER_POWDER_SNOW, -1.0F);
        }
        if (kind.fireImmune) {
            this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
            this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
        }
    }

    private void set(net.minecraft.world.entity.ai.attributes.Attribute a, double v) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance i = this.getAttribute(a);
        if (i != null) {
            i.setBaseValue(v);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SPECIES.equals(key)) {
            applyKind();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return kind == null ? super.getDimensions(pose) : EntityDimensions.scalable(kind.w, kind.h);
    }

    public boolean flier() {
        return kind != null && kind.fly;
    }

    public int love() {
        return this.entityData.get(LOVE);
    }

    public int mode() {
        return this.entityData.get(MODE);
    }

    public boolean hasFlag(int f) {
        return (this.entityData.get(FLAGS) & f) != 0;
    }

    public void setFlag(int f, boolean on) {
        byte b = this.entityData.get(FLAGS);
        this.entityData.set(FLAGS, (byte) (on ? (b | f) : (b & ~f)));
    }

    public boolean leaving() {
        return hasFlag(F_LEAVING);
    }

    public boolean canFight() {
        return kind != null && kind.combat() && mode() != MODE_CALM && !isOrderedToSit() && !leaving();
    }

    /** Dueño conectado (en el servidor). */
    public ServerPlayer ownerPlayer() {
        return getOwner() instanceof ServerPlayer sp ? sp : null;
    }

    /** Su figurita equipada (para guardar recuerdos), o vacío. */
    public ItemStack figurine() {
        ServerPlayer o = ownerPlayer();
        return o == null || spec == null ? ItemStack.EMPTY : PetKeeper.figurine(o, spec);
    }

    /** Sube el cariño (0 a 100) y lo guarda en la figurita. */
    public void addLove(int n) {
        int before = love();
        int v = Mth.clamp(before + n, 0, PetData.MAX_LOVE);
        if (v == before) {
            return;
        }
        this.entityData.set(LOVE, v);
        ItemStack f = figurine();
        if (!f.isEmpty()) {
            PetData.edit(f).putInt("Love", v);
            PetKeeper.saved(ownerPlayer(), false);
        }
        if (v == PetData.MAX_LOVE && before < PetData.MAX_LOVE && ownerPlayer() != null) {
            ownerPlayer().displayClientMessage(Component.translatable("msg.mundos_de_axelis.pet.best_friend", getDisplayName()), true);
            burst(ParticleTypes.HEART, 10, 0.4);
            playSound(SoundEvents.PLAYER_LEVELUP, 0.5F, 1.6F);
        }
    }

    public void cycleMode() {
        if (kind == null || !kind.combat()) {
            return;
        }
        int m = (mode() + 1) % 3;
        this.entityData.set(MODE, (byte) m);
        if (m == MODE_CALM) {
            setTarget(null);
        }
        ItemStack f = figurine();
        if (!f.isEmpty()) {
            PetData.edit(f).putByte("Mode", (byte) m);
            PetKeeper.saved(ownerPlayer(), false);
        }
    }

    // ------------------------------------------------------------------ nombre

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        if (!level().isClientSide) {
            ItemStack f = figurine();
            if (!f.isEmpty()) {
                if (name == null || name.getString().isBlank()) {
                    PetData.edit(f).remove("Name");
                } else {
                    String s = name.getString();
                    PetData.edit(f).putString("Name", s.length() > PetData.MAX_NAME ? s.substring(0, PetData.MAX_NAME) : s);
                }
                PetKeeper.saved(ownerPlayer(), true);
            }
        }
    }

    /** Sin nombre propio se llama como su especie ("Gato de la Fortuna"). */
    @Override
    protected Component getTypeName() {
        return spec == null ? super.getTypeName() : Component.translatable("pet.mundos_de_axelis." + spec.id());
    }

    // ------------------------------------------------------------------ reglas de criatura domesticada

    @Override
    public boolean shouldBeSaved() {
        return false; // la vuelve a crear PetKeeper; así nunca hay copias guardadas en el mundo
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean isFood(ItemStack st) {
        return false;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean canBeLeashed(Player p) {
        return !this.isLeashed() && this.isOwnedBy(p);
    }

    @Override
    public boolean fireImmune() {
        return kind != null && kind.fireImmune;
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        if (!flier()) {
            super.checkFallDamage(y, onGround, state, pos);
        }
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Creeper || target instanceof Ghast || target instanceof ArmorStand || target == owner) {
            return false;
        }
        if (target instanceof TamableAnimal t && t.isTame()) {
            return false;
        }
        if (target instanceof AbstractHorse h && h.isTamed()) {
            return false;
        }
        if (target instanceof Player p && owner instanceof Player o && !o.canHarmPlayer(p)) {
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ sonidos

    @Override
    protected SoundEvent getAmbientSound() {
        if (kind == null || leaving()) {
            return null;
        }
        if (isInSittingPose() && kind.species == 1 && random.nextInt(3) == 0) {
            return SoundEvents.CAT_PURR;
        }
        return kind.ambient();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return kind == null ? null : kind.hurtSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return kind == null ? null : kind.deathSound();
    }

    @Override
    public float getVoicePitch() {
        return (kind == null ? 1F : kind.pitch) * (0.94F + random.nextFloat() * 0.12F);
    }

    @Override
    protected float getSoundVolume() {
        return kind != null && (kind.species == 35 || kind.species == 47) ? 0.25F : 0.6F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    /** Su voz con un tono a gusto (contenta, al comer, al saludar). */
    public void voice(float vol, float pitchMul) {
        if (kind != null) {
            playSound(kind.ambient(), vol, kind.pitch * pitchMul);
        }
    }

    // ------------------------------------------------------------------ clic derecho

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (kind == null || leaving()) {
            return InteractionResult.PASS;
        }
        boolean client = this.level().isClientSide;
        boolean main = hand == InteractionHand.MAIN_HAND;
        // la misma decisión en el cliente y en el servidor (si no, el cliente se queda esperando o come lo que tienes en la mano)
        if (!this.isOwnedBy(player)) {
            // los demás también pueden acariciarla (pero el cariño solo sube con su dueño)
            if (st.isEmpty() && main) {
                if (!client) {
                    happy(player, false);
                }
                return InteractionResult.sidedSuccess(client);
            }
            return InteractionResult.PASS;
        }
        if (main && player.isSecondaryUseActive()) {
            if (!client && player instanceof ServerPlayer sp) {
                openMenu(sp);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (wantsFood(st)) {
            if (!client) {
                feed(player, st);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (main && (st.isEmpty() || !st.isEdible())) {
            if (!client) {
                toggleSit(player);
            }
            return InteractionResult.sidedSuccess(client);
        }
        return InteractionResult.PASS;
    }

    /** ¿Le apetece? (su comida favorita o cualquier comida, si le falta vida o cariño). */
    public boolean wantsFood(ItemStack st) {
        if (kind == null || st.isEmpty()) {
            return false;
        }
        boolean fav = st.is(kind.food());
        if (!fav && !st.isEdible()) {
            return false;
        }
        return this.getHealth() < this.getMaxHealth() || love() < PetData.MAX_LOVE;
    }

    private void feed(Player player, ItemStack st) {
        boolean fav = st.is(kind.food());
        ItemStack bite = st.copyWithCount(1);
        if (!player.getAbilities().instabuild) {
            st.shrink(1);
        }
        float heal = fav ? 8.0F : (st.isEdible() && bite.getFoodProperties(this) != null ? Math.max(2, bite.getFoodProperties(this).getNutrition()) : 2.0F);
        this.heal(heal);
        addLove(fav ? 8 : 3);
        this.playSound(SoundEvents.GENERIC_EAT, 0.7F, 1.0F + random.nextFloat() * 0.3F);
        Scheduler.runLater(8, () -> {
            if (isAlive()) {
                voice(0.6F, 1.15F);
            }
        });
        this.level().broadcastEntityEvent(this, EV_EAT);
        if (level() instanceof ServerLevel sl) {
            Vec3 m = mouth();
            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, bite), m.x, m.y, m.z, 8, 0.1, 0.05, 0.1, 0.05);
            burst(ParticleTypes.HEART, fav ? 5 : 2, 0.3);
        }
    }

    /** Clic derecho: se sienta o se levanta (como el lobo). */
    public void toggleSit(Player player) {
        boolean sit = !this.isOrderedToSit();
        this.setOrderedToSit(sit);
        this.setInSittingPose(sit);
        this.jumping = false;
        this.navigation.stop();
        this.setTarget(null);
        PetKeeper.sitChanged(this, ownerPlayer(), sit);
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable(sit ? "msg.mundos_de_axelis.pet.sit" : "msg.mundos_de_axelis.pet.follow", getDisplayName()), true);
        }
        burst(ParticleTypes.HEART, 1, 0.1);
        voice(0.5F, sit ? 0.95F : 1.12F);
    }

    /** Acariciarla: corazones, su sonido contento y (con su dueño) un poco más de cariño. */
    public void happy(Player player, boolean fromMenu) {
        if (petCd > 0) {
            return;
        }
        petCd = 12;
        this.level().broadcastEntityEvent(this, EV_HAPPY);
        burst(ParticleTypes.HEART, fromMenu ? 4 : 2, 0.35);
        if (kind != null && kind.species == 1) {
            playSound(SoundEvents.CAT_PURR, 0.9F, 1.0F);
        } else {
            voice(0.7F, 1.2F);
        }
        if (isOwnedBy(player) && loveCd <= 0) {
            loveCd = 400; // como mucho, un poco de cariño cada 20 s por caricias
            addLove(3);
        }
    }

    /** Hace un truco: da una vuelta y un saltito. */
    public void trick() {
        if (petCd > 0) {
            return;
        }
        petCd = 26;
        this.level().broadcastEntityEvent(this, EV_TRICK);
        voice(0.6F, 1.3F);
        if (level() instanceof ServerLevel sl && kind != null) {
            Vec3 c = this.getBoundingBox().getCenter();
            sl.sendParticles(Fx.spark(kind.rgb, 0.7F), c.x, c.y, c.z, 10, 0.3, 0.3, 0.3, 0.05);
        }
    }

    private void openMenu(ServerPlayer sp) {
        ItemStack fig = figurine();
        if (fig.isEmpty()) {
            return;
        }
        this.navigation.stop();
        int bag = kind.special == PetKinds.Special.BAG ? kind.bag : 0;
        NetworkHooks.openScreen(sp, new SimpleMenuProvider((id, inv, p) -> new PetMenu(id, inv, this, fig), getDisplayName()), buf -> {
            buf.writeVarInt(this.getId());
            buf.writeVarInt(bag);
        });
    }

    // ------------------------------------------------------------------ daño y desmayo

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source) || kind == null) {
            return false;
        }
        Entity src = source.getEntity();
        if (src instanceof Player p) {
            if (this.isOwnedBy(p) || !(p.getServer() != null && p.getServer().isPvpAllowed())) {
                return false;
            }
        }
        if (src instanceof Mascota m && Objects.equals(m.getOwnerUUID(), this.getOwnerUUID())) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        if (source.is(DamageTypes.IN_WALL)) {
            if (!level().isClientSide && ownerPlayer() != null) {
                teleportNear(ownerPlayer());
            }
            return false;
        }
        if (!level().isClientSide && isOrderedToSit() && src instanceof LivingEntity) {
            // como el lobo: si la atacan, se levanta
            this.setOrderedToSit(false);
            PetKeeper.sitChanged(this, ownerPlayer(), false);
        }
        if (love() >= PetData.MAX_LOVE) {
            amount *= 0.8F; // su mejor amigo la cuida: aguanta un poco más
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (this.level().isClientSide) {
            return;
        }
        // no muere: se cansa y vuelve a su figurita (regresa sola al minuto)
        this.dead = true;
        ServerPlayer o = ownerPlayer();
        if (level() instanceof ServerLevel sl) {
            Vec3 c = this.getBoundingBox().getCenter();
            sl.sendParticles(ParticleTypes.POOF, c.x, c.y, c.z, 14, 0.25, 0.25, 0.25, 0.03);
            if (kind != null) {
                sl.sendParticles(Fx.spark(kind.rgb, 0.7F), c.x, c.y, c.z, 12, 0.3, 0.3, 0.3, 0.06);
            }
        }
        PetKeeper.fainted(this, o);
        clearLight();
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        clearLight();
        super.remove(reason);
    }

    /** Se despide: con chispas (al quitarla del menú) o en silencio (se queda en casa, sales del juego...). */
    public void leave(boolean fx) {
        clearLight();
        if (!fx) {
            this.discard();
            return;
        }
        if (leaving()) {
            return;
        }
        setFlag(F_LEAVING, true);
        leaveTicks = 9;
        this.navigation.stop();
        if (level() instanceof ServerLevel sl && spec != null) {
            Vec3 c = this.getBoundingBox().getCenter();
            RelicSounds.play(sl, c, spec.id(), "end", 0.7F, 1.0F);
            Vfx.flash(sl, c, spec.rgb(), 1.4, 9);
            RelicFx.burst(sl, Fx.spark(spec.rgb(), 0.6F), c, 12, 0.22);
        }
    }

    /** Al aparecer junto a ti: un destello de su color. */
    public void arriveFx() {
        if (level() instanceof ServerLevel sl && spec != null) {
            Vec3 c = this.position().add(0, kind == null ? 0.4 : kind.h * 0.5, 0);
            Vfx.flash(sl, c, spec.rgb(), 1.4, 10);
            RelicFx.burst(sl, Fx.spark(spec.rgb(), 0.7F), c, 10, 0.22);
            voice(0.5F, 1.1F);
        }
    }

    // ------------------------------------------------------------------ dónde ponerse junto al dueño

    /** Un sitio libre cerca del dueño (detrás o a los lados), con suelo firme para las que andan. */
    public Vec3 spotNear(ServerPlayer owner) {
        BlockPos ob = owner.blockPosition();
        for (int i = 0; i < 16; i++) {
            int dx = random.nextInt(7) - 3, dz = random.nextInt(7) - 3;
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue;
            }
            // no justo delante de tus ojos (las primeras veces; si no hay sitio, vale cualquiera)
            Vec3 f = Vec3.directionFromRotation(0, owner.getYRot());
            if (i < 10 && (f.x * dx + f.z * dz) > 0.5 * Math.sqrt(dx * dx + dz * dz)) {
                continue;
            }
            int dy = random.nextInt(3) - 1;
            BlockPos p = ob.offset(dx, dy, dz);
            if (flier()) {
                Vec3 v = new Vec3(p.getX() + 0.5, owner.getY() + 1.2 + random.nextDouble() * 0.6, p.getZ() + 0.5);
                if (level().noCollision(this, this.getDimensions(Pose.STANDING).makeBoundingBox(v))) {
                    return v;
                }
            } else if (WalkNodeEvaluator.getBlockPathTypeStatic(level(), p.mutable()) == BlockPathTypes.WALKABLE) {
                Vec3 v = new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                if (level().noCollision(this, this.getDimensions(Pose.STANDING).makeBoundingBox(v))) {
                    return v;
                }
            }
        }
        return owner.position();
    }

    /** Se teletransporta junto a su dueño (como el lobo cuando te alejas mucho). */
    public void teleportNear(ServerPlayer owner) {
        Vec3 from = this.getBoundingBox().getCenter();
        Vec3 to = spotNear(owner);
        this.moveTo(to.x, to.y, to.z, this.getYRot(), this.getXRot());
        this.navigation.stop();
        if (level() instanceof ServerLevel sl && spec != null) {
            sl.sendParticles(Fx.spark(spec.rgb(), 0.5F), from.x, from.y, from.z, 6, 0.2, 0.2, 0.2, 0.02);
            sl.sendParticles(Fx.spark(spec.rgb(), 0.5F), to.x, to.y + 0.3, to.z, 6, 0.2, 0.2, 0.2, 0.02);
        }
    }

    // ------------------------------------------------------------------ combate

    /** Daño de un golpe suyo (a los jefes les hace menos, para no estropear sus peleas). */
    public float hitDamage(LivingEntity t, float mul) {
        float d = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * mul;
        if (BossDamage.stageOf(t) > 0) {
            d *= 0.4F;
        }
        return d;
    }

    /** Mordisco (o zarpazo, o martillazo): lo usa el ataque cuerpo a cuerpo. */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity le) || kind == null) {
            return false;
        }
        boolean ok = le.hurt(this.damageSources().mobAttack(this), hitDamage(le, 1.0F));
        if (ok) {
            this.level().broadcastEntityEvent(this, EV_BITE);
            playSound(SoundEvents.FOX_BITE, 0.6F, 1.1F + random.nextFloat() * 0.3F);
            if (spec != null && level() instanceof ServerLevel sl && random.nextInt(3) == 0) {
                RelicSounds.play(sl, this, spec.id(), "act", 0.5F, 1.0F + random.nextFloat() * 0.15F);
            }
            element(le, true);
            if (kind.species == 30 || kind.species == 61) {
                RelicFx.knock(le, this.position(), 0.6, 0.35); // el golemcito y el tricerito empujan
            }
            this.setLastHurtMob(le);
        }
        return ok;
    }

    /** Lo que le pasa al enemigo según el sabor de su ataque. */
    public void element(LivingEntity t, boolean melee) {
        if (!(level() instanceof ServerLevel sl) || kind == null) {
            return;
        }
        Vec3 c = t.getBoundingBox().getCenter();
        switch (kind.element) {
            case FIRE -> {
                t.setSecondsOnFire(3);
                sl.sendParticles(Fx.ember(kind.rgb, 0.8F), c.x, c.y, c.z, 8, 0.25, 0.3, 0.25, 0.03);
            }
            case ICE -> {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y, c.z, 10, 0.25, 0.3, 0.25, 0.02);
            }
            case SHOCK -> {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0), this);
                sl.sendParticles(Fx.bolt(kind.rgb, 0.7F), c.x, c.y, c.z, 6, 0.25, 0.3, 0.25, 0.02);
            }
            case STAR -> {
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), this);
                sl.sendParticles(Fx.spark(kind.rgb, 0.8F), c.x, c.y, c.z, 10, 0.25, 0.3, 0.25, 0.04);
            }
            case INK -> {
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), this);
                sl.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 10, 0.25, 0.25, 0.25, 0.03);
            }
            case TOAST -> {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(1.6), e -> e != t && isFoeOf(e))) {
                    e.hurt(this.damageSources().mobAttack(this), hitDamage(e, 0.4F));
                    e.setSecondsOnFire(2);
                }
                t.setSecondsOnFire(2);
                sl.sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 6, 0.2, 0.2, 0.2, 0.02);
            }
            case CRYSTAL -> {
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(2.0), e -> e != t && isFoeOf(e))) {
                    e.hurt(this.damageSources().mobAttack(this), hitDamage(e, 0.4F));
                }
                sl.sendParticles(Fx.shard(kind.rgb, 0.8F), c.x, c.y, c.z, 12, 0.35, 0.35, 0.35, 0.08);
            }
            case MAGIC -> {
                RelicFx.knock(t, this.position(), 0.5, 0.2);
                sl.sendParticles(Fx.rune(kind.rgb, 0.8F), c.x, c.y, c.z, 8, 0.25, 0.3, 0.25, 0.03);
            }
            case LEECH -> {
                this.heal(1.0F);
                if (ownerPlayer() != null && ownerPlayer().distanceToSqr(this) < 256) {
                    ownerPlayer().heal(1.0F);
                }
                sl.sendParticles(Fx.heart(kind.rgb, 0.6F), c.x, c.y, c.z, 3, 0.2, 0.2, 0.2, 0.02);
            }
            case SEED -> {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2), this);
                sl.sendParticles(Fx.petal(kind.rgb, 0.8F), c.x, c.y, c.z, 8, 0.25, 0.3, 0.25, 0.03);
            }
            case EARTH -> {
                t.setDeltaMovement(t.getDeltaMovement().add(0, 0.32, 0));
                t.hurtMarked = true;
                BlockState g = RelicFx.groundState(sl, t.getX(), t.getY() - 0.4, t.getZ());
                RelicFx.burst(sl, RelicFx.blockDust(g), t.position().add(0, 0.2, 0), 10, 0.22);
            }
            default -> {
            }
        }
    }

    /** ¿Es un enemigo de su dueño? (para los golpes en área: nunca toca a los tuyos). */
    public boolean isFoeOf(LivingEntity e) {
        if (!e.isAlive() || e == this || e == getOwner() || e instanceof Player) {
            return false;
        }
        if (e instanceof TamableAnimal t && t.isTame()) {
            return false;
        }
        return e instanceof net.minecraft.world.entity.monster.Enemy || e == getTarget();
    }

    /** Disparo a distancia: viaja hasta el enemigo con la estela de su elemento (la tostada va en parábola). */
    public void fireBolt(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl) || kind == null) {
            return;
        }
        this.level().broadcastEntityEvent(this, EV_CAST);
        Vec3 from = mouth();
        Vec3 to = t.getBoundingBox().getCenter();
        double dist = from.distanceTo(to);
        if (spec != null && RelicSounds.has(spec.id(), "act")) {
            RelicSounds.play(sl, from, spec.id(), "act", 0.55F, 1.0F + random.nextFloat() * 0.15F);
        } else {
            playSound(SoundEvents.FIRECHARGE_USE, 0.35F, 1.6F);
        }
        float dmg = hitDamage(t, 0.85F);
        if (kind.element == PetKinds.Element.SHOCK) {
            // el rayo llega al instante
            Vfx.bolt(sl, from, to, kind.rgb, 0.05, 6);
            Vfx.flash(sl, to, kind.rgb, 1.0, 6);
            if (t.isAlive() && t.hurt(this.damageSources().mobAttack(this), dmg)) {
                element(t, false);
            }
            return;
        }
        ParticleOptions trail = switch (kind.element) {
            case FIRE -> Fx.ember(kind.rgb, 0.9F);
            case STAR -> Fx.spark(kind.rgb, 0.8F);
            case INK -> ParticleTypes.SQUID_INK;
            case CRYSTAL -> Fx.shard(kind.rgb, 0.8F);
            case MAGIC -> Fx.rune(kind.rgb, 0.8F);
            case SEED -> Fx.petal(kind.rgb, 0.8F);
            case TOAST -> Fx.dust(0xD49A52, 0.9F);
            default -> Fx.spark(kind.rgb, 0.7F);
        };
        boolean lob = kind.element == PetKinds.Element.TOAST;
        int ticks = Mth.clamp((int) Math.ceil(dist / (lob ? 0.8 : 1.2)), 2, 16);
        double arc = lob ? 1.0 + dist * 0.18 : 0.0;
        final UUID tid = t.getUUID();
        for (int i = 1; i <= ticks; i++) {
            final double a = (i - 1) / (double) ticks, b = i / (double) ticks;
            final boolean last = i == ticks;
            Scheduler.runLater(i, () -> {
                Entity live = sl.getEntity(tid);
                Vec3 end = live instanceof LivingEntity le && le.isAlive() ? le.getBoundingBox().getCenter() : to;
                Vec3 p0 = from.lerp(end, a).add(0, 4 * arc * a * (1 - a), 0);
                Vec3 p1 = from.lerp(end, b).add(0, 4 * arc * b * (1 - b), 0);
                for (int k = 0; k < 3; k++) {
                    Vec3 q = p0.lerp(p1, k / 3.0);
                    sl.sendParticles(trail, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0.0);
                }
                if (last) {
                    Vfx.flash(sl, end, kind.rgb, 1.0, 6);
                    if (live instanceof LivingEntity le && le.isAlive() && le.hurt(this.damageSources().mobAttack(this), dmg)) {
                        element(le, false);
                    }
                }
            });
        }
    }

    /** Punto de la boca (de donde salen los disparos y las migas). */
    public Vec3 mouth() {
        Vec3 look = Vec3.directionFromRotation(0, this.yBodyRot);
        return this.position().add(look.scale(this.getBbWidth() * 0.45)).add(0, this.getBbHeight() * 0.7, 0);
    }

    // ------------------------------------------------------------------ luz que te sigue

    private void updateLight() {
        if (!hasFlag(F_LIGHT) || isRemoved()) {
            clearLight();
            return;
        }
        BlockPos p = BlockPos.containing(getX(), getY() + Math.min(0.9, getBbHeight() * 0.6), getZ());
        if (p.equals(lightAt)) {
            return;
        }
        BlockState here = level().getBlockState(p);
        if (!here.isAir()) {
            return;
        }
        clearLight();
        level().setBlock(p, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 13), 3);
        lightAt = p;
    }

    public void clearLight() {
        if (lightAt != null && !level().isClientSide) {
            if (level().getBlockState(lightAt).is(Blocks.LIGHT)) {
                level().setBlock(lightAt, Blocks.AIR.defaultBlockState(), 3);
            }
            lightAt = null;
        }
    }

    // ------------------------------------------------------------------ cada tick

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            clientTick();
            return;
        }
        if (kind == null) {
            this.discard();
            return;
        }
        if (leaveTicks >= 0) {
            if (--leaveTicks <= 0) {
                this.discard();
            }
            return;
        }
        if (this.tickCount % 20 == 7 && !PetKeeper.isCurrent(this)) {
            this.discard(); // sobra (no es la mascota vigente de su dueño)
            return;
        }
        if (petCd > 0) {
            petCd--;
        }
        if (loveCd > 0) {
            loveCd--;
        }
        if (boltCd > 0) {
            boltCd--;
        }
        // las voladoras se posan cuando se sientan; si no, flotan donde se quedan (no caen al suelo al pararse)
        if (flier()) {
            boolean sit = isOrderedToSit();
            if (sit == this.isNoGravity()) {
                this.setNoGravity(!sit);
            }
        }
        // se cura poco a poco cuando no la están golpeando (más rápido con mucho cariño)
        int every = love() >= 50 ? 25 : 40;
        if (this.tickCount % every == 0 && this.getHealth() < this.getMaxHealth() && this.tickCount - this.getLastHurtByMobTimestamp() > 100) {
            this.heal(1.0F);
        }
        if (this.tickCount % 4 == 0) {
            updateLight();
        }
        // su mejor amigo: de vez en cuando le salen corazoncitos cuando estás cerca
        if (love() >= PetData.MAX_LOVE && this.tickCount % 200 == 0 && random.nextInt(3) == 0 && getOwner() != null && distanceToSqr(getOwner()) < 64) {
            burst(ParticleTypes.HEART, 1, 0.1);
        }
        PetActions.tick(this);
    }

    /** Partículas alrededor de ella (en el servidor). */
    public void burst(ParticleOptions p, int n, double spread) {
        if (level() instanceof ServerLevel sl && n > 0) {
            sl.sendParticles(p, getX(), getY() + getBbHeight() * 0.9 + 0.2, getZ(), n, spread, 0.15, spread, 0.02);
        }
    }

    // ------------------------------------------------------------------ cliente: animación del modelo 3D

    private void clientTick() {
        if (kind == null || spec == null) {
            return;
        }
        VfxData d = anim;
        d.entity = true;
        d.b = spec.species();
        d.size = (float) spec.size();
        d.id = this.getId();
        d.rgb = 0xFFFFFF;
        d.light = 1F;
        if (!d.mInit) {
            d.x = d.px = getX();
            d.y = d.py = getY();
            d.z = d.pz = getZ();
        }
        d.px = d.x;
        d.py = d.y;
        d.pz = d.z;
        d.x = getX();
        d.y = getY();
        d.z = getZ();
        double target = -Math.toRadians(this.yBodyRot);
        if (!d.yawInit) {
            d.yaw = d.pyaw = target;
            d.yawInit = true;
        } else {
            d.pyaw = d.yaw;
            d.yaw += Mth.wrapDegrees(Math.toDegrees(target - d.yaw)) * Math.PI / 180.0;
        }
        double g0 = d.mGait;
        Creature3D.tickMotion(d);
        // reacciones: se sienta de verdad, salta contigo, se duerme si tú duermes, respingo al recibir un golpe
        double sitT = isInSittingPose() ? 1 : 0;
        LivingEntity o = getOwner();
        if (o != null && o.distanceToSqr(this) < 100) {
            if (o.isCrouching()) {
                sitT = Math.max(sitT, 0.7);
            }
            boolean g = o.onGround();
            if (ownerWasGround && !g && o.getDeltaMovement().y > 0.2 && d.mHop <= 0) {
                d.mHop = 12 + (getId() % 3) * 2;
            }
            ownerWasGround = g;
            if (o instanceof Player p && p.isSleeping()) {
                d.mIdle = Math.max(d.mIdle, Creature3D.SLEEP_T + 60);
            }
        }
        if (this.hurtTime > 0 && this.hurtTime != lastHurt && this.hurtTime >= 9) {
            if (d.mIdle > Creature3D.SLEEP_T) {
                d.mWake = 16;
            }
            d.mIdle = 0;
        }
        lastHurt = this.hurtTime;
        if (trickT > 0 || happyT > 0 || castT > 0 || biteT > 0 || eatT > 0) {
            d.mIdle = 0;
        }
        d.pmSit = d.mSit;
        d.mSit += (sitT - d.mSit) * 0.18;
        if (d.mHop > 0) {
            d.mHop--;
        }
        if (trickT > 0) {
            trickT--;
        }
        if (happyT > 0) {
            happyT--;
        }
        if (castT > 0) {
            castT--;
        }
        if (biteT > 0) {
            biteT--;
        }
        if (eatT > 0) {
            eatT--;
        }
        if (leaving() && leaveAge < 0) {
            leaveAge = this.tickCount;
        }
        // polvito al apoyar cada pata (las que andan por el suelo)
        if (!flier() && d.mSpd > 0.06 && Math.floor(g0 / Math.PI) != Math.floor(d.mGait / Math.PI) && this.onGround()) {
            BlockPos bp = BlockPos.containing(getX(), getY() - 0.2, getZ());
            BlockState bs = level().getBlockState(bp);
            if (!bs.isAir() && bs.getRenderShape() != RenderShape.INVISIBLE) {
                double side = (Math.floor(d.mGait / Math.PI) % 2 == 0 ? 1 : -1) * 0.12 * d.size;
                double c = Math.cos(d.yaw), sn = Math.sin(d.yaw);
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, bs), getX() + c * side, getY() + 0.03, getZ() - sn * side,
                        (random.nextDouble() - 0.5) * 0.06, 0.04, (random.nextDouble() - 0.5) * 0.06);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EV_HAPPY -> {
                happyT = 20;
                anim.mHop = 14;
                for (int i = 0; i < 3; i++) {
                    level().addParticle(ParticleTypes.HEART, getRandomX(0.6), getY() + getBbHeight() + 0.2, getRandomZ(0.6), 0, 0.05, 0);
                }
            }
            case EV_TRICK -> {
                trickT = 22;
                anim.mHop = 16;
            }
            case EV_CAST, EV_ROAR -> {
                castT = id == EV_ROAR ? 24 : 12;
                anim.mHop = Math.max(anim.mHop, 10);
            }
            case EV_BITE -> {
                biteT = 8;
                anim.mHop = Math.max(anim.mHop, 8);
            }
            case EV_EAT -> {
                eatT = 16;
                anim.mHop = 12;
            }
            default -> super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------ sin guardado (por si acaso)

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Species", species());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Species")) {
            setSpecies(tag.getInt("Species"));
        }
    }

    /** Enemigos alrededor de un punto, para las acciones de su menú. */
    public List<Mob> monstersNear(double r) {
        AABB box = this.getBoundingBox().inflate(r);
        return level().getEntitiesOfClass(Mob.class, box, m -> m instanceof net.minecraft.world.entity.monster.Enemy && m.isAlive());
    }
}
