package com.navidad.entidad;

import com.navidad.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * El trineo de madera para pasear: se pone en el suelo, se monta con clic derecho (caben dos) y se desliza cuesta abajo (sube escalones de medio bloque, no de uno entero). En la nieve y
 * en el hielo resbala mucho (en el hielo todavía más); en pasto o tierra casi no avanza. Cada vez que cae de un escalón de la ladera, la
 * caída se vuelve velocidad, así que bajar una montaña nevada es cada vez más rápido.
 * <p>Controles: A y D lo voltean, W empuja con los pies (solo despacio, como de verdad), S frena metiendo los pies, Shift para bajarse.
 * Se rompe a golpes y suelta su objeto. Igual que una lancha, lo mueve la computadora de quien lo maneja y el servidor solo lo sigue.
 */
public class TrineoMadera extends Entity {
    private static final EntityDataAccessor<Integer> GOLPE = SynchedEntityData.defineId(TrineoMadera.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DANO = SynchedEntityData.defineId(TrineoMadera.class, EntityDataSerializers.FLOAT);

    /** Lo pone el cliente: arranca el sonido de deslizarse (que sigue al trineo y sube con la velocidad). */
    public static Consumer<TrineoMadera> alAparecer = t -> {
    };

    /** Cómo resbala cada suelo: cuánto conserva la velocidad por tick, cuánto agarra de lado, y hasta dónde empujas con los pies. */
    public enum Suelo {
        HIELO_AZUL(0.993, 0.12, 0.30), HIELO(0.989, 0.2, 0.26), NIEVE(0.981, 0.5, 0.20), OTRO(0.80, 0.9, 0.06), AIRE(0.996, 0, 0);

        final double conserva, agarre, empujeMax;

        Suelo(double conserva, double agarre, double empujeMax) {
            this.conserva = conserva;
            this.agarre = agarre;
            this.empujeMax = empujeMax;
        }

        public boolean resbala() {
            return this == HIELO_AZUL || this == HIELO || this == NIEVE;
        }
    }

    /** Velocidad tope (bloques por tick): 1.1 son unos 80 km/h. */
    public static final double TOPE = 1.1;

    private float giro;
    private int lerpPasos;
    private double lerpX, lerpY, lerpZ, lerpYaw;
    private boolean enSueloAntes = true;
    /** Lo que se ve de cliente: la velocidad de este tick (para chispas y sonido) y el ladeo al dar vuelta. */
    private double rapidez;
    private float ladeo, ladeoAntes;
    private Suelo suelo = Suelo.OTRO;

    public TrineoMadera(EntityType<?> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        setMaxUpStep(0.6F);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(GOLPE, 0);
        entityData.define(DANO, 0F);
    }

    // ------------------------------------------------------------------ montarse

    @Override
    public InteractionResult interact(Player p, InteractionHand hand) {
        if (p.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            return p.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity e) {
        return getPassengers().size() < 2;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return -0.06;
    }

    /** El de adelante maneja; el segundo va atrás, abrazado. */
    @Override
    protected void positionRider(Entity e, Entity.MoveFunction mover) {
        if (!hasPassenger(e)) {
            return;
        }
        int i = getPassengers().indexOf(e);
        double atras = getPassengers().size() > 1 ? (i == 0 ? 0.28 : -0.5) : -0.08;
        Vec3 h = rumbo(getYRot());
        mover.accept(e, getX() + h.x * atras, getY() + getPassengersRidingOffset() + e.getMyRidingOffset(), getZ() + h.z * atras);
        e.setYRot(e.getYRot() + giro);
        e.setYHeadRot(e.getYHeadRot() + giro);
        mirar(e);
    }

    @Override
    public void onPassengerTurned(Entity e) {
        mirar(e);
    }

    /** Quien va montado puede voltear a los lados, pero no ver para atrás (como en la lancha). */
    private void mirar(Entity e) {
        e.setYBodyRot(getYRot());
        float f = Mth.wrapDegrees(e.getYRot() - getYRot());
        float g = Mth.clamp(f, -105F, 105F);
        e.yRotO += g - f;
        e.setYRot(e.getYRot() + g - f);
        e.setYHeadRot(e.getYRot());
    }

    // ------------------------------------------------------------------ golpes

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (isInvulnerableTo(src) || !(src.getEntity() instanceof Player p)) {
            return false;
        }
        if (level().isClientSide || isRemoved()) {
            return true;
        }
        entityData.set(GOLPE, 10);
        entityData.set(DANO, entityData.get(DANO) + amount * 10F);
        markHurt();
        if (p.getAbilities().instabuild || entityData.get(DANO) > 40F) {
            if (!p.getAbilities().instabuild) {
                spawnAtLocation(new ItemStack(ModRegistry.TRINEO_MADERA_ITEM.get()));
            }
            level().playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1F, 1F);
            discard();
        } else {
            level().playSound(null, blockPosition(), SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 1F, 1F);
        }
        return true;
    }

    public float golpe(float pt) {
        return Math.max(0, entityData.get(GOLPE) - pt);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModRegistry.TRINEO_MADERA_ITEM.get());
    }

    /** Caer de unos cuantos bloques no lastima (la nieve amortigua); de muy alto, sí, a los que van arriba. */
    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return dist > 7F && super.causeFallDamage(dist - 6F, mult, src);
    }

    // ------------------------------------------------------------------ el movimiento

    public static Vec3 rumbo(float yaw) {
        float r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    /** El suelo que importa: la capa de nieve en la que va hundido, o el bloque de abajo. */
    public Suelo sueloDebajo() {
        if (!onGround()) {
            return Suelo.AIRE;
        }
        BlockState aqui = level().getBlockState(blockPosition());
        if (aqui.is(Blocks.SNOW) || aqui.is(Blocks.POWDER_SNOW)) {
            return Suelo.NIEVE;
        }
        BlockState b = level().getBlockState(BlockPos.containing(getX(), getY() - 0.2, getZ()));
        if (b.is(Blocks.BLUE_ICE)) {
            return Suelo.HIELO_AZUL;
        }
        if (b.is(BlockTags.ICE)) {
            return Suelo.HIELO;
        }
        if (b.is(Blocks.SNOW_BLOCK) || b.is(Blocks.SNOW) || b.is(Blocks.POWDER_SNOW)) {
            return Suelo.NIEVE;
        }
        return Suelo.OTRO;
    }

    @Override
    public void tick() {
        int g = entityData.get(GOLPE);
        if (g > 0) {
            entityData.set(GOLPE, g - 1);
        }
        float d = entityData.get(DANO);
        if (d > 0) {
            entityData.set(DANO, Math.max(0, d - 1F));
        }
        super.tick();
        seguirLerp();
        ladeoAntes = ladeo;
        if (isControlledByLocalInstance()) {
            if (lerpPasos > 0) {
                lerpPasos = 0;
                absMoveTo(lerpX, lerpY, lerpZ, getYRot(), getXRot());
            }
            fisica();
        } else {
            setDeltaMovement(Vec3.ZERO);
            giro = 0;
        }
        checkInsideBlocks();
        double dx = getX() - xo, dz = getZ() - zo;
        rapidez = Math.sqrt(dx * dx + dz * dz);
        suelo = sueloDebajo();
        if (level().isClientSide) {
            efectos(dx, dz);
            ladeo = Mth.clamp(ladeo * 0.8F + giro * (float) Math.min(1, rapidez * 2) * 1.6F, -10F, 10F);
        }
    }

    private void fisica() {
        Suelo s = sueloDebajo();
        Player p = getControllingPassenger() instanceof Player pl ? pl : null;
        float adelante = p != null ? p.zza : 0, lado = p != null ? p.xxa : 0;
        Vec3 v = getDeltaMovement();
        double vel = Math.sqrt(v.x * v.x + v.z * v.z);
        // voltear: más fácil despacio; en el aire casi no
        giro = 0;
        if (lado != 0) {
            giro = -lado * (vel < 0.35 ? 4.5F : 3.0F) * (s == Suelo.AIRE ? 0.3F : 1F);
        }
        setYRot(getYRot() + giro);
        Vec3 h = rumbo(getYRot());
        double f = v.x * h.x + v.z * h.z;
        double lx = v.x - f * h.x, lz = v.z - f * h.z;
        if (s != Suelo.AIRE) {
            // el patín muerde: lo que iba de lado se vuelve (casi todo) para adelante
            double deLado = Math.sqrt(lx * lx + lz * lz) * s.agarre;
            lx *= 1 - s.agarre;
            lz *= 1 - s.agarre;
            f += (f >= 0 ? 1 : -1) * deLado * 0.8;
            if (adelante > 0 && f < s.empujeMax) {
                f = Math.min(s.empujeMax, f + (s.resbala() ? 0.014 : 0.006));
            } else if (adelante < 0) {
                f *= 0.86;
            }
        }
        f *= s.conserva;
        lx *= s.conserva;
        lz *= s.conserva;
        double vy = v.y - 0.06;
        if (isInWater()) {
            vy = Math.min(vy + 0.1, 0.06);
            f *= 0.85;
        }
        f = Mth.clamp(f, -0.25, TOPE);
        setDeltaMovement(h.x * f + lx, vy * 0.98, h.z * f + lz);
        double yAntes = getY(), vyAntes = getDeltaMovement().y;
        move(MoverType.SELF, getDeltaMovement());
        boolean ahora = onGround();
        Vec3 n = getDeltaMovement();
        if (ahora && !enSueloAntes && vyAntes < -0.1) {
            // aterrizó: lo que cayó se vuelve velocidad (solo donde resbala)
            Suelo s2 = sueloDebajo();
            if (s2.resbala()) {
                double extra = Math.min(0.3, -vyAntes * 0.5);
                n = n.add(h.x * extra, 0, h.z * extra);
            }
        } else if (ahora && enSueloAntes && getY() - yAntes > 0.05) {
            // se subió un escalón: cuesta arriba frena
            double k = Math.max(0.3, 1 - 0.6 * (getY() - yAntes));
            n = new Vec3(n.x * k, n.y, n.z * k);
        }
        if (horizontalCollision) {
            if (vel > 0.45 && level().isClientSide) {
                level().playLocalSound(getX(), getY(), getZ(), SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 1F, 0.8F, false);
            }
            n = new Vec3(n.x * 0.2, n.y, n.z * 0.2);
        }
        setDeltaMovement(n);
        enSueloAntes = ahora;
    }

    /** Nieve que salta de los patines, escarcha en el hielo y un cascabel cuando va muy rápido. */
    private void efectos(double dx, double dz) {
        if (rapidez < 0.06 || !onGround()) {
            return;
        }
        Vec3 h = rumbo(getYRot());
        double sx = -h.z, sz = h.x;
        int n = Math.min(4, (int) (rapidez * 6) + 1);
        BlockState nieve = Blocks.SNOW_BLOCK.defaultBlockState();
        for (int i = 0; i < n; i++) {
            for (int lado = -1; lado <= 1; lado += 2) {
                double px = getX() - h.x * 0.7 + sx * 0.4 * lado, pz = getZ() - h.z * 0.7 + sz * 0.4 * lado;
                if (suelo == Suelo.NIEVE) {
                    level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, nieve), px, getY() + 0.1, pz, -dx * 0.6 + sx * lado * 0.1,
                            0.12 + random.nextDouble() * 0.1, -dz * 0.6 + sz * lado * 0.1);
                } else if (suelo == Suelo.HIELO || suelo == Suelo.HIELO_AZUL) {
                    if (random.nextInt(3) == 0) {
                        level().addParticle(ParticleTypes.SNOWFLAKE, px, getY() + 0.05, pz, -dx * 0.2, 0.02, -dz * 0.2);
                    }
                }
            }
        }
        if (rapidez > 0.55 && tickCount % 14 == 0) {
            level().playLocalSound(getX(), getY(), getZ(), ModRegistry.CASCABEL.get(), SoundSource.NEUTRAL, 0.35F,
                    0.9F + random.nextFloat() * 0.3F, false);
        }
    }

    public double rapidez() {
        return rapidez;
    }

    public Suelo suelo() {
        return suelo;
    }

    public float ladeo(float pt) {
        return Mth.lerp(pt, ladeoAntes, ladeo);
    }

    // ------------------------------------------------------------------ lo que ven los demás (el servidor manda la posición)

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int pasos, boolean tp) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYaw = yaw;
        lerpPasos = 10;
    }

    private void seguirLerp() {
        if (isControlledByLocalInstance()) {
            lerpPasos = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpPasos > 0) {
            double x = getX() + (lerpX - getX()) / lerpPasos;
            double y = getY() + (lerpY - getY()) / lerpPasos;
            double z = getZ() + (lerpZ - getZ()) / lerpPasos;
            float yaw = (float) (getYRot() + Mth.wrapDegrees(lerpYaw - getYRot()) / lerpPasos);
            giro = yaw - getYRot();
            setYRot(yaw);
            lerpPasos--;
            setPos(x, y, z);
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (level().isClientSide) {
            alAparecer.accept(this);
        }
    }

    // ------------------------------------------------------------------ guardar

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
