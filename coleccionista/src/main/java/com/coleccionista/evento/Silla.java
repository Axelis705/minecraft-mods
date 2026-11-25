package com.coleccionista.evento;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityMountEvent;

import java.util.function.BiPredicate;

/**
 * Una silla de metal que se mueve sola (la salida de los circuitos): se sienta quien le dé clic derecho; cuando el evento la "cierra" ya no
 * se puede levantar (las correas). El evento la lleva a donde quiera con {@link #irA}: rueda despacio por el piso con un chirrido.
 * No se guarda en el mundo: el evento las pone y las quita.
 */
public class Silla extends Entity {
    private static final EntityDataAccessor<Boolean> CERRADA = SynchedEntityData.defineId(Silla.class, EntityDataSerializers.BOOLEAN);
    /** Quién se puede sentar en cuál (lo decide la prueba); si es null, cualquiera. */
    public static BiPredicate<ServerPlayer, Silla> permiso;
    /** Para que el propio evento pueda bajar a alguien de una silla cerrada. */
    private static boolean soltando;
    private Vec3 destino;
    private double rapidez;
    public String grupo = "";

    public Silla(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.blocksBuilding = false;
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(Silla::alBajar);
    }

    /** Nadie se levanta de una silla cerrada (ni en el cliente: así no "parpadea" al intentarlo). */
    private static void alBajar(EntityMountEvent e) {
        if (e.isDismounting() && e.getEntityBeingMounted() instanceof Silla s && s.cerrada() && !soltando && s.isAlive()) {
            e.setCanceled(true);
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(CERRADA, false);
    }

    public boolean cerrada() {
        return this.entityData.get(CERRADA);
    }

    public void cerrar(boolean si) {
        this.entityData.set(CERRADA, si);
        if (si && !level().isClientSide) {
            level().playSound(null, blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1F, 0.6F);
            level().playSound(null, blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.5F);
        }
    }

    /** Baja a quien esté sentado aunque la silla esté cerrada. */
    public void soltar() {
        soltando = true;
        try {
            ejectPassengers();
        } finally {
            soltando = false;
        }
    }

    /** Que ruede hasta ese punto, a {@code rapidez} bloques por tick. */
    public void irA(Vec3 p, double rapidez) {
        this.destino = p;
        this.rapidez = rapidez;
    }

    public boolean llego() {
        return destino == null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || destino == null) {
            return;
        }
        Vec3 d = destino.subtract(position());
        double l = d.length();
        if (l <= rapidez || l < 1e-3) {
            setPos(destino);
            destino = null;
            return;
        }
        Vec3 paso = d.scale(rapidez / l);
        setPos(position().add(paso));
        if (tickCount % 6 == 0) {
            level().playSound(null, blockPosition(), SoundEvents.MINECART_INSIDE, SoundSource.BLOCKS, 0.15F, 0.6F);
        }
        if (tickCount % 17 == 0) {
            level().playSound(null, blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 0.5F, 0.5F);
        }
    }

    @Override
    public InteractionResult interact(Player p, InteractionHand hand) {
        if (p.isSecondaryUseActive() || isVehicle() || cerrada() || p.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            if (p instanceof ServerPlayer sp && (permiso == null || permiso.test(sp, this))) {
                p.startRiding(this);
                level().playSound(null, blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 0.7F, 0.7F);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.42;
    }

    @Override
    protected boolean canAddPassenger(Entity e) {
        return getPassengers().isEmpty();
    }

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
