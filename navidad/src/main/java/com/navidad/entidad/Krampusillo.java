package com.navidad.entidad;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.character.CoreMob;
import com.navidad.Logros;
import com.navidad.Navidad;
import com.navidad.bloque.CajaRegalo;
import com.navidad.bloque.CajaRegaloBE;
import com.navidad.krampus.SacoDelKrampus;
import com.navidad.objeto.MascaraKrampus;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/**
 * El krampusillo: diablito de cabra que trabaja para el Krampus. Llega con él (su rugido los llama), en la Noche del Krampus o en su
 * guarida. Si hay una caja de regalo cerca (la que le dijeron), va de puntitas, se la roba y sale corriendo riéndose; si lo matas, la
 * suelta; si se escapa (un minuto, o se aleja), el regalo se va al saco del Krampus (lo recuperas al vencerlo). Sin regalo que robar,
 * araña a la gente. No ataca a quien trae la máscara del Krampus, y la campanita de Santa lo espanta.
 */
public class Krampusillo extends CoreMob {
    private BlockPos caja;
    private ItemStack botin = ItemStack.EMPTY;
    private int vida, espantado;
    private UUID duenio;
    /** Para la prueba con fotos y el registro. */
    public static int robos, atrapados, escapados;

    public Krampusillo(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 28.0);
    }

    /** (Servidor) Aparece uno en una nubecita. {@code caja}: el regalo que viene a robar (o null para pelear). */
    public static Krampusillo llamar(ServerLevel l, Vec3 donde, LivingEntity objetivo, BlockPos caja) {
        Krampusillo k = ModRegistry.KRAMPUSILLO.get().create(l);
        if (k == null) {
            return null;
        }
        Vec3 p = com.axeliscore.combat.Combate.alSuelo(l, donde.add(0, 1, 0));
        k.moveTo(p.x, p.y, p.z, l.random.nextFloat() * 360F, 0);
        k.finalizeSpawn(l, l.getCurrentDifficultyAt(k.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        k.caja = caja == null ? null : caja.immutable();
        if (objetivo instanceof Player pl && !MascaraKrampus.lleva(pl)) {
            k.setTarget(objetivo);
            k.duenio = pl.getUUID();
        }
        l.addFreshEntity(k);
        l.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.5, p.z, 10, 0.3, 0.4, 0.3, 0.01);
        l.playSound(null, k.blockPosition(), ModRegistry.KRAMPUSILLO_RISA.get(), SoundSource.HOSTILE, 1F, 1F);
        return k;
    }

    public void robarA(BlockPos caja, ServerPlayer de) {
        this.caja = caja.immutable();
        this.duenio = de == null ? null : de.getUUID();
    }

    public boolean conRegalo() {
        return !botin.isEmpty();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new Huir());
        this.goalSelector.addGoal(2, new Robar());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> e instanceof Player p && !MascaraKrampus.lleva(p)));
    }

    @Override
    public boolean isAlliedTo(Entity e) {
        return e instanceof Krampus || e instanceof Krampusillo || e instanceof MunecoMalvado || super.isAlliedTo(e);
    }

    @Override
    public void setTarget(LivingEntity t) {
        if (t instanceof Player p && MascaraKrampus.lleva(p)) {
            t = null;
        }
        super.setTarget(t);
    }

    /** Va de puntitas hacia la caja de regalo; al llegar, se la roba. */
    private final class Robar extends Goal {
        Robar() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return caja != null && !conRegalo() && espantado <= 0;
        }

        @Override
        public void tick() {
            if (caja == null) {
                return;
            }
            getLookControl().setLookAt(caja.getX() + 0.5, caja.getY() + 0.3, caja.getZ() + 0.5);
            if (distanceToSqr(caja.getX() + 0.5, caja.getY(), caja.getZ() + 0.5) < 2.4) {
                robar();
            } else if (tickCount % 10 == 0 || getNavigation().isDone()) {
                getNavigation().moveTo(caja.getX() + 0.5, caja.getY(), caja.getZ() + 0.5, 1.1);
            }
        }
    }

    /** Con el regalo (o espantado): corre lejos de la gente. */
    private final class Huir extends Goal {
        Huir() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return conRegalo() || espantado > 0;
        }

        @Override
        public void tick() {
            if (getNavigation().isDone() || tickCount % 25 == 0) {
                Player p = level().getNearestPlayer(Krampusillo.this, 24);
                Vec3 lejos = p == null ? DefaultRandomPos.getPos(Krampusillo.this, 14, 5)
                        : DefaultRandomPos.getPosAway(Krampusillo.this, 16, 6, p.position());
                if (lejos != null) {
                    getNavigation().moveTo(lejos.x, lejos.y, lejos.z, 1.45);
                }
            }
        }
    }

    private void robar() {
        ServerLevel l = (ServerLevel) level();
        if (!(l.getBlockState(caja).getBlock() instanceof CajaRegalo) || !(l.getBlockEntity(caja) instanceof CajaRegaloBE be)
                || be.contenido.isEmpty()) {
            caja = null;
            return;
        }
        botin = CajaRegalo.envuelta(be.contenido, be.de, be.para);
        be.contenido = ItemStack.EMPTY;
        l.removeBlock(caja, false);
        setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.CAJA_REGALO_ITEM.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0F);
        playAction("risa", 0);
        l.sendParticles(ParticleTypes.SMOKE, caja.getX() + 0.5, caja.getY() + 0.5, caja.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        robos++;
        ServerPlayer p = duenio == null ? null : l.getServer().getPlayerList().getPlayer(duenio);
        if (p != null) {
            p.sendSystemMessage(Component.translatable("navidad.krampusillo.robo").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }
        Navidad.LOGGER.info("NAVIDAD_KRAMPUSILLO se robó un regalo en {}", caja.toShortString());
        caja = null;
        vida = Math.min(vida, 400);
    }

    /** La campanita de Santa: suelta lo que trae y huye. */
    public void espantar() {
        if (!(level() instanceof ServerLevel l)) {
            return;
        }
        soltarBotin(l);
        espantado = 60;
        setTarget(null);
        playAction("hurt", 0);
        l.playSound(null, blockPosition(), ModRegistry.KRAMPUSILLO_DOLOR.get(), SoundSource.HOSTILE, 1F, 1.2F);
    }

    private void soltarBotin(ServerLevel l) {
        if (!botin.isEmpty()) {
            ItemEntity it = new ItemEntity(l, getX(), getY() + 0.6, getZ(), botin);
            it.setDeltaMovement(0, 0.25, 0);
            l.addFreshEntity(it);
            botin = ItemStack.EMPTY;
            setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (level() instanceof ServerLevel l) {
            boolean traia = conRegalo();
            soltarBotin(l);
            atrapados++;
            if (src.getEntity() instanceof ServerPlayer p) {
                Logros.dar(p, "krampusillo");
                if (traia) {
                    p.displayClientMessage(Component.translatable("navidad.krampusillo.atrapado").withStyle(ChatFormatting.GOLD), true);
                }
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (tickCount % 6 == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 0, 0.01, 0);
            }
            return;
        }
        vida++;
        if (espantado > 0 && --espantado == 0) {
            escapar();
            return;
        }
        boolean lejos = level().getNearestPlayer(this, 40) == null;
        if (vida > 1200 || conRegalo() && (vida > 500 || lejos && vida > 100)) {
            escapar();
            return;
        }
        if (conRegalo() && tickCount % 50 == 0) {
            playAction("risa", 0);
        }
    }

    /** Se escapa en una nubecita (y si trae regalo, se lo lleva al saco del Krampus). */
    private void escapar() {
        ServerLevel l = (ServerLevel) level();
        l.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 16, 0.3, 0.4, 0.3, 0.02);
        if (conRegalo()) {
            SacoDelKrampus.meter(l.getServer(), botin);
            escapados++;
            ServerPlayer p = duenio == null ? null : l.getServer().getPlayerList().getPlayer(duenio);
            if (p != null) {
                p.sendSystemMessage(Component.translatable("navidad.krampusillo.escapo").withStyle(ChatFormatting.DARK_PURPLE));
            }
            botin = ItemStack.EMPTY;
        }
        discard();
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!botin.isEmpty()) {
            tag.put("Botin", botin.save(new CompoundTag()));
        }
        tag.putInt("Vida", vida);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        botin = tag.contains("Botin") ? ItemStack.of(tag.getCompound("Botin")) : ItemStack.EMPTY;
        vida = tag.getInt("Vida");
    }

    @Override
    public boolean doHurtTarget(Entity e) {
        playAction("attack", 0);
        return super.doHurtTarget(e);
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        if (axDeath(h, "action")) {
            return;
        }
        axActions(h, n -> "action");
        axHurt(h, "action");
        axMove(h, 0.15F, 3.2F, 1.4F, 4.0F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModRegistry.KRAMPUSILLO_RISA.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return ModRegistry.KRAMPUSILLO_DOLOR.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModRegistry.KRAMPUSILLO_DOLOR.get();
    }

}
