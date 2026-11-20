package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.npc.NpcKinds;
import com.mundosdeaxelis.npc.NpcService;
import com.mundosdeaxelis.npc.Vecinos;
import com.mundosdeaxelis.registry.ModEntities;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Habitante (etapa 257): los personajes que se rescatan de las celdas de las estructuras. No se le puede hacer daño, no
 * desaparece y nadie lo ataca. Tiene tres estados:
 * <ul>
 * <li>{@link #PRESO}: en la celda, da vueltas por ella; al hablarle te cuenta quién tiene la llave.</li>
 * <li>{@link #LIBRE}: rescatado pero sin casa; se queda cerca de donde lo sacaste esperando que plantes su semilla.</li>
 * <li>{@link #VECINO}: vive en su casa; pasea por ella y el jardín (abre y cierra la puerta) y tiene la tienda abierta.</li>
 * </ul>
 */
public class Habitante extends PathfinderMob {
    public static final int PRESO = 0, LIBRE = 1, VECINO = 2;

    private static final EntityDataAccessor<String> KIND = SynchedEntityData.defineId(Habitante.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(Habitante.class, EntityDataSerializers.INT);

    private BlockPos home;
    /** Con quién está hablando y por cuánto (se queda quieto mirándolo). */
    private UUID talkTo;
    private int talkTicks;

    public Habitante(EntityType<? extends Habitante> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
        if (getNavigation() instanceof GroundPathNavigation g) {
            g.setCanOpenDoors(true);
            g.setCanPassDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    /** Crea un habitante ya colocado en el mundo (al rescatarlo, al plantar su casa o al reaparecer). */
    public static Habitante create(ServerLevel level, String kind, int state, BlockPos spot) {
        Habitante h = ModEntities.HABITANTE.get().create(level);
        if (h == null) {
            return null;
        }
        h.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        h.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null, null);
        h.setup(kind, state, spot);
        return level.addFreshEntity(h) ? h : null;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(KIND, "");
        entityData.define(STATE, PRESO);
    }

    // ================================================================== datos

    public String kindId() {
        return entityData.get(KIND);
    }

    public NpcKinds.Kind kind() {
        return NpcKinds.get(kindId());
    }

    public int state() {
        return entityData.get(STATE);
    }

    public BlockPos home() {
        return home;
    }

    /** ¿Puede ponerse este preso en una celda nueva? (no si ya lo rescataron en este mundo). */
    public boolean prisonerAllowed(ServerLevel level, String kind) {
        return NpcKinds.get(kind) != null && Vecinos.state(level.getServer(), kind) == PRESO;
    }

    public void setup(String kind, int state, BlockPos spot) {
        entityData.set(KIND, kind);
        NpcKinds.Kind k = NpcKinds.get(kind);
        if (k != null) {
            setCustomName(Component.translatable(k.nameKey()));
            setCustomNameVisible(true);
        }
        setState(state, spot);
    }

    public void setState(int state, BlockPos spot) {
        entityData.set(STATE, state);
        home = spot == null ? blockPosition() : spot.immutable();
        restrictTo(home, state == PRESO ? 2 : state == LIBRE ? 4 : 7);
        getNavigation().stop();
    }

    /** Se queda quieto mirando a quien le habla unos segundos (se renueva con cada acción del menú). */
    public void talkWith(Player p) {
        talkTo = p.getUUID();
        talkTicks = 100;
        getNavigation().stop();
    }

    public boolean talking() {
        return talkTicks > 0;
    }

    // ================================================================== comportamiento

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(3, new GoHome());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.42D, 0.001F) {
            @Override
            public boolean canUse() {
                return !talking() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !talking() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        goalSelector.addGoal(2, new FaceTalker());
    }

    /**
     * Vuelve a su sitio por el camino más corto cuando está lejos (al salir de la celda, si lo empujan, si se aleja paseando).
     * Va directo al punto: así cruza la puerta abierta de la celda en vez de buscar al azar.
     */
    private final class GoHome extends Goal {
        GoHome() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !talking() && home != null && !isWithinRestriction();
        }

        @Override
        public void start() {
            getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.6D);
        }

        @Override
        public boolean canContinueToUse() {
            return !talking() && !getNavigation().isDone();
        }
    }

    /** Mira a quien le habla y no se mueve. */
    private final class FaceTalker extends Goal {
        FaceTalker() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return talking();
        }

        @Override
        public void tick() {
            Player p = talkTo == null ? null : level().getPlayerByUUID(talkTo);
            if (p != null) {
                getLookControl().setLookAt(p, 30.0F, 30.0F);
            }
            getNavigation().stop();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (talkTicks > 0) {
            talkTicks--;
        }
        if (!level().isClientSide && tickCount % 40 == 7 && level() instanceof ServerLevel sl) {
            String kind = kindId();
            Vecinos.Entry e = Vecinos.entry(sl.getServer(), kind);
            if (state() == PRESO) {
                if (e != null && e.state != PRESO) {
                    discard(); // ya lo rescataron en otro santuario
                }
            } else if (e == null || e.state == PRESO || (e.uuid != null && !e.uuid.equals(getUUID()))) {
                discard(); // hay otro que vale (se mudó, reapareció o se reinició)
            } else if (e.uuid == null) {
                Vecinos.set(sl.getServer(), kind, e.state, getUUID(), sl.dimension(), e.spot, e.house, e.rot);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            NpcService.interact(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // ================================================================== no se le hace daño ni se va

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && level() instanceof ServerLevel sl && state() != PRESO) {
            Vecinos.lost(sl.getServer(), kindId(), getUUID());
        }
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity other) {
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    // ================================================================== guardar

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Npc", kindId());
        tag.putInt("NpcState", state());
        if (home != null) {
            tag.putLong("NpcHome", home.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(KIND, tag.getString("Npc"));
        int st = Math.max(PRESO, Math.min(VECINO, tag.getInt("NpcState")));
        setState(st, tag.contains("NpcHome") ? BlockPos.of(tag.getLong("NpcHome")) : blockPosition());
        setInvulnerable(true);
    }
}
