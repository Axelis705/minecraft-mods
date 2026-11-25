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
    /** Sin modelo: un asiento invisible (el mueble ya está en el mapa, como los de las góndolas de la caja de música). */
    private static final EntityDataAccessor<Boolean> OCULTA = SynchedEntityData.defineId(Silla.class, EntityDataSerializers.BOOLEAN);
    /** Quién se puede sentar en cuál (lo decide la prueba); si es null, cualquiera. */
    public static BiPredicate<ServerPlayer, Silla> permiso;
    /** Para que el propio evento pueda bajar a alguien de una silla cerrada. */
    private static boolean soltando;
    private Vec3 destino;
    private double rapidez;
    /** Hacia dónde gira (yaw) y qué tan rápido (grados por tick); null: no gira. */
    private Float giroObj;
    private float giroVel = 3F;
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

    /** Nadie se levanta de una silla cerrada (lo decide el servidor; el cliente solo obedece lo que le dicen). */
    private static void alBajar(EntityMountEvent e) {
        if (e.getLevel() == null || e.getLevel().isClientSide()) {
            return;
        }
        if (e.isDismounting() && e.getEntityBeingMounted() instanceof Silla s && s.cerrada() && !soltando && s.isAlive()) {
            e.setCanceled(true);
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(CERRADA, false);
        this.entityData.define(OCULTA, false);
    }

    public boolean oculta() {
        return this.entityData.get(OCULTA);
    }

    public void oculta(boolean si) {
        this.entityData.set(OCULTA, si);
    }

    public boolean cerrada() {
        return this.entityData.get(CERRADA);
    }

    public void cerrar(boolean si) {
        cerrar(si, true);
    }

    /** Cierra o abre las correas; sin {@code sonido}, en silencio (al pasar la silla de un cuarto a otro sin que se note). */
    public void cerrar(boolean si, boolean sonido) {
        this.entityData.set(CERRADA, si);
        if (si && sonido && !level().isClientSide) {
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

    /** Que gire despacio (sobre su lugar) hasta mirar a {@code yaw}; quien va sentado gira con ella (el cliente le gira la vista). */
    public void girarA(float yaw, float gradosPorTick) {
        this.giroObj = yaw;
        this.giroVel = gradosPorTick;
    }

    public boolean girando() {
        return giroObj != null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (giroObj != null) {
            float d = net.minecraft.util.Mth.wrapDegrees(giroObj - getYRot());
            if (Math.abs(d) <= giroVel) {
                setYRot(giroObj);
                giroObj = null;
            } else {
                setYRot(getYRot() + Math.signum(d) * giroVel);
                if (tickCount % 8 == 0) {
                    level().playSound(null, blockPosition(), SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 0.35F, 0.6F);
                }
            }
            setYHeadRot(getYRot());
        }
        if (destino == null) {
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
