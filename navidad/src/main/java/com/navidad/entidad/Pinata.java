package com.navidad.entidad;

import com.navidad.objeto.Premios;
import com.navidad.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * La piñata de siete picos (como en las posadas): cuelga quieta; cada golpe la hace columpiarse y de vez en cuando suelta un dulce. Al
 * último golpe revienta: dulces, fruta (tejocotes, mandarinas, cañas) y, a veces, un regalo misterioso, con confeti.
 */
public class Pinata extends Entity {
    private static final EntityDataAccessor<Integer> GOLPE = SynchedEntityData.defineId(Pinata.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GOLPES = SynchedEntityData.defineId(Pinata.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LADO = SynchedEntityData.defineId(Pinata.class, EntityDataSerializers.FLOAT);
    public static int AGUANTE = 12;

    public Pinata(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(GOLPE, -100);
        this.entityData.define(GOLPES, 0);
        this.entityData.define(LADO, 0F);
    }

    /** Cuándo fue el último golpe (para columpiarse) y hacia qué lado (grados). */
    public int golpe() {
        return entityData.get(GOLPE);
    }

    public float lado() {
        return entityData.get(LADO);
    }

    public int golpes() {
        return entityData.get(GOLPES);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource src, float amt) {
        if (level().isClientSide || isRemoved()) {
            return false;
        }
        Entity quien = src.getEntity();
        if (!(quien instanceof Player p)) {
            return false;
        }
        boolean palo = p.getMainHandItem().is(ModRegistry.PALO_PINATA.get());
        int n = golpes() + (palo ? 1 : 0) + (palo ? 0 : (random.nextFloat() < 0.5F ? 1 : 0));
        entityData.set(GOLPES, n);
        entityData.set(GOLPE, tickCount);
        entityData.set(LADO, (float) Math.toDegrees(Math.atan2(getZ() - p.getZ(), getX() - p.getX())));
        ServerLevel l = (ServerLevel) level();
        l.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1F, 0.9F + random.nextFloat() * 0.3F);
        l.playSound(null, blockPosition(), SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 1F, 0.8F);
        l.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, getX(), getY() + 0.8, getZ(), 6, 0.3, 0.3, 0.3, 0.2);
        if (random.nextFloat() < 0.25F) {
            soltar(l, new ItemStack(ModRegistry.DULCE.get()), 0.25);
        }
        if (n >= AGUANTE) {
            romper(l, p);
        } else if (n == AGUANTE - 3) {
            p.displayClientMessage(Component.translatable("navidad.pinata.casi"), true);
        }
        return true;
    }

    private void soltar(ServerLevel l, ItemStack st, double fuerza) {
        ItemEntity it = new ItemEntity(l, getX(), getY() + 0.5, getZ(), st);
        it.setDeltaMovement((random.nextDouble() - 0.5) * fuerza, 0.2 + random.nextDouble() * fuerza, (random.nextDouble() - 0.5) * fuerza);
        it.setDefaultPickUpDelay();
        l.addFreshEntity(it);
    }

    private void romper(ServerLevel l, Player p) {
        int dulces = 14 + random.nextInt(12);
        for (int i = 0; i < dulces; i++) {
            soltar(l, new ItemStack(ModRegistry.DULCE.get()), 0.6);
        }
        for (int i = 0; i < 3 + random.nextInt(4); i++) {
            soltar(l, new ItemStack(ModRegistry.TEJOCOTE.get()), 0.6);
            soltar(l, new ItemStack(ModRegistry.MANDARINA.get()), 0.6);
        }
        for (int i = 0; i < 2 + random.nextInt(3); i++) {
            soltar(l, new ItemStack(ModRegistry.CANA.get()), 0.6);
        }
        if (random.nextFloat() < 0.35F) {
            soltar(l, new ItemStack(ModRegistry.REGALO_MISTERIOSO.get()), 0.4);
        }
        Premios.confeti(l, getX(), getY() + 0.6, getZ(), true);
        l.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.5F, 1F);
        l.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.2F, 1F);
        l.playSound(null, blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.5F, 0.7F);
        for (Player cerca : l.players()) {
            if (cerca.distanceTo(this) < 24) {
                cerca.displayClientMessage(Component.translatable("navidad.pinata.rota", p.getDisplayName()), true);
                if (cerca instanceof net.minecraft.server.level.ServerPlayer sp) {
                    com.axeliscore.npc.Misiones.evento(sp, com.navidad.Navidad.MODID + ":pinata", 1);
                }
            }
        }
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        entityData.set(GOLPES, t.getInt("Golpes"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putInt("Golpes", golpes());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
