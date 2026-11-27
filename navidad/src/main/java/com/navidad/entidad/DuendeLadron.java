package com.navidad.entidad;

import com.axeliscore.character.Persona;
import com.axeliscore.vestir.Vestuario;
import com.navidad.Logros;
import com.navidad.Navidad;
import com.navidad.bloque.CajaRegalo;
import com.navidad.bloque.CajaRegaloBE;
import com.navidad.objeto.Ropa;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/**
 * El duende ladrón: algunas noches llega a escondidas a tu árbol, se roba uno de los regalos sin abrir y sale corriendo con él en las
 * manos, riéndose. Si lo alcanzas y le pegas (aunque sea una vez), se rinde: suelta el regalo, te deja unos dulces de disculpa y desaparece
 * en una nube. Si no lo atrapas en poco más de un minuto (o amanece), se escapa con el regalo. Nunca muere ni lastima a nadie.
 */
public class DuendeLadron extends Persona {
    private BlockPos caja;
    private ItemStack botin = ItemStack.EMPTY;
    private UUID duenio;
    private int vida;
    /** Para la prueba con fotos: cuántos robos y cuántas veces lo atraparon. */
    public static int robos, atrapados;

    public DuendeLadron(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** (Servidor) Llega por el regalo de {@code caja}: aparece a unos 14 bloques del árbol, del lado contrario a donde estás. */
    public static DuendeLadron llegar(ServerLevel l, BlockPos arbol, BlockPos caja, ServerPlayer p) {
        DuendeLadron d = ModRegistry.DUENDE_LADRON.get().create(l);
        if (d == null) {
            return null;
        }
        Vec3 lejos = new Vec3(arbol.getX() - p.getX(), 0, arbol.getZ() - p.getZ());
        lejos = lejos.lengthSqr() < 1 ? new Vec3(1, 0, 0) : lejos.normalize();
        int x = (int) Math.floor(arbol.getX() + lejos.x * 13), z = (int) Math.floor(arbol.getZ() + lejos.z * 13);
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (Math.abs(y - arbol.getY()) > 8) {
            x = arbol.getX() + 6;
            z = arbol.getZ() + 6;
            y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        }
        d.moveTo(x + 0.5, y, z + 0.5, l.random.nextFloat() * 360F, 0);
        d.caja = caja.immutable();
        d.duenio = p.getUUID();
        l.addFreshEntity(d);
        Vestuario.poner(l.getServer(), d.getUUID(), Ropa.id("gorro_duende"), null);
        l.sendParticles(ParticleTypes.LARGE_SMOKE, d.getX(), d.getY() + 0.5, d.getZ(), 10, 0.3, 0.4, 0.3, 0.01);
        l.playSound(null, d.blockPosition(), SoundEvents.FOX_SNIFF, SoundSource.HOSTILE, 1F, 1.6F);
        p.displayClientMessage(Component.translatable("navidad.ladron.llega").withStyle(ChatFormatting.DARK_PURPLE), true);
        Navidad.LOGGER.info("NAVIDAD_LADRON llega por la caja de {} en {}", p.getGameProfile().getName(), caja.toShortString());
        return d;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new Huir());
        this.goalSelector.addGoal(2, new Robar());
    }

    public boolean conRegalo() {
        return !botin.isEmpty();
    }

    /** Va de puntitas hacia el regalo; al llegar, se lo roba. */
    private final class Robar extends Goal {
        Robar() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return caja != null && !conRegalo();
        }

        @Override
        public void tick() {
            if (caja == null) {
                return;
            }
            getLookControl().setLookAt(caja.getX() + 0.5, caja.getY() + 0.3, caja.getZ() + 0.5);
            if (distanceToSqr(caja.getX() + 0.5, caja.getY(), caja.getZ() + 0.5) < 2.6) {
                robar();
            } else if (tickCount % 10 == 0 || getNavigation().isDone()) {
                getNavigation().moveTo(caja.getX() + 0.5, caja.getY(), caja.getZ() + 0.5, 1.0);
            }
        }
    }

    /** Ya con el regalo (o si se lo ganaron): corre lejos de la gente. */
    private final class Huir extends Goal {
        Huir() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return conRegalo() || caja == null;
        }

        @Override
        public void tick() {
            if (getNavigation().isDone() || tickCount % 30 == 0) {
                Player p = level().getNearestPlayer(DuendeLadron.this, 24);
                Vec3 lejos = p == null ? DefaultRandomPos.getPos(DuendeLadron.this, 14, 5)
                        : DefaultRandomPos.getPosAway(DuendeLadron.this, 16, 6, p.position());
                if (lejos != null) {
                    getNavigation().moveTo(lejos.x, lejos.y, lejos.z, p != null && p.distanceTo(DuendeLadron.this) < 8 ? 1.5 : 1.25);
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
        setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0F);
        playAction("laugh", 40);
        l.playSound(null, blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.HOSTILE, 1F, 1.9F);
        l.sendParticles(ParticleTypes.WITCH, caja.getX() + 0.5, caja.getY() + 0.5, caja.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        robos++;
        ServerPlayer p = duenio == null ? null : l.getServer().getPlayerList().getPlayer(duenio);
        if (p != null) {
            p.sendSystemMessage(Component.translatable("navidad.ladron.robo").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        }
        Navidad.LOGGER.info("NAVIDAD_LADRON se robó un regalo en {}", caja.toShortString());
        caja = null;
        vida = Math.min(vida, 200);
    }

    /** Lo atraparon: suelta el regalo, deja dulces de disculpa y desaparece. */
    private void atrapado(Player p) {
        ServerLevel l = (ServerLevel) level();
        if (!botin.isEmpty()) {
            ItemEntity it = new ItemEntity(l, getX(), getY() + 0.6, getZ(), botin);
            it.setDeltaMovement(0, 0.25, 0);
            l.addFreshEntity(it);
        }
        l.addFreshEntity(new ItemEntity(l, getX(), getY() + 0.6, getZ(), new ItemStack(ModRegistry.DULCE.get(), 3 + random.nextInt(4))));
        l.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.6, getZ(), 24, 0.35, 0.5, 0.35, 0.03);
        l.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.8, getZ(), 16, 0.4, 0.4, 0.4, 0.04);
        l.playSound(null, blockPosition(), SoundEvents.FOX_SCREECH, SoundSource.HOSTILE, 0.8F, 1.8F);
        l.playSound(null, blockPosition(), ModRegistry.CASCABEL.get(), SoundSource.HOSTILE, 0.8F, 1.6F);
        p.displayClientMessage(Component.translatable(botin.isEmpty() ? "navidad.ladron.atrapado_nada" : "navidad.ladron.atrapado")
                .withStyle(ChatFormatting.GOLD), false);
        if (p instanceof ServerPlayer sp) {
            Logros.dar(sp, "ladron");
        }
        atrapados++;
        Navidad.LOGGER.info("NAVIDAD_LADRON atrapado por {} (regalo={})", p.getGameProfile().getName(), !botin.isEmpty());
        botin = ItemStack.EMPTY;
        Vestuario.quitarTodo(l.getServer(), getUUID());
        discard();
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (!level().isClientSide && src.getEntity() instanceof Player p && !isRemoved()) {
            atrapado(p);
            return true;
        }
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            p.displayClientMessage(Component.translatable("navidad.ladron.burla").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            playAction("laugh", 30);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        vida++;
        long hora = level().getDayTime() % 24000L;
        boolean dia = hora < 12500 || hora > 23200;
        if (vida > 1500 || dia && vida > 100) {
            escapar();
            return;
        }
        if (conRegalo() && tickCount % 50 == 0) {
            playAction("laugh", 20);
            playSound(SoundEvents.VILLAGER_CELEBRATE, 0.7F, 2.0F);
        }
        if (tickCount % 4 == 0) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.2, getZ(), 1, 0.15, 0.05, 0.15, 0);
        }
    }

    /** Se escapa (con el regalo, si lo tiene). */
    private void escapar() {
        ServerLevel l = (ServerLevel) level();
        l.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 20, 0.3, 0.5, 0.3, 0.02);
        ServerPlayer p = duenio == null ? null : l.getServer().getPlayerList().getPlayer(duenio);
        if (p != null && conRegalo()) {
            p.sendSystemMessage(Component.translatable("navidad.ladron.escapo").withStyle(ChatFormatting.DARK_PURPLE));
        }
        Navidad.LOGGER.info("NAVIDAD_LADRON se escapó (regalo={})", conRegalo());
        Vestuario.quitarTodo(l.getServer(), getUUID());
        discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 1.7F;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }
}
