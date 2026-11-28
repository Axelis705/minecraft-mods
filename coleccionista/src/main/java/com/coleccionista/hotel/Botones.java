package com.coleccionista.hotel;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.character.CoreMob;
import com.coleccionista.Coleccionista;
import com.coleccionista.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * El Botones: un muñeco de porcelana gigante con uniforme de botones que empuja un carrito de maletas por los pasillos del hotel. No se le
 * puede hacer daño. Cuando ve a alguien se detiene, voltea de golpe, azota el timbre del carrito (¡ding!), suelta el carrito y lo persigue
 * con sus brazos larguísimos. Si lo pierde, busca un rato (la cabeza le da vueltas de muñeco), regresa por su carrito y sigue su ronda.
 * Es muy alto para pasar por las puertas y las escaleras: solo anda por los pasillos (los cuartos son refugio). Cambia de piso por el
 * elevador de servicio (lo decide {@link JuegoHotel}: aparece con un "ding").
 * <p>
 * Hace daño pero no mata de un golpe (6 de vida, y después se queda un momento quieto: da tiempo de huir).
 */
public class Botones extends CoreMob {
    private static final EntityDataAccessor<Boolean> CARRITO = SynchedEntityData.defineId(Botones.class, EntityDataSerializers.BOOLEAN);
    public static float danio = 6F;
    /** Qué tan lejos ve (bloques) y cuánto aguanta sin ver a quien persigue antes de rendirse (ticks). */
    public static double vista = 18;
    public static int paciencia = 120;

    public enum Estado {
        PATRULLA, VIO, PERSIGUE, BUSCA, REGRESA
    }

    private Estado estado = Estado.PATRULLA;
    private int estadoDesde, ultimoGolpe = -100, sinVer, quietoDesde;
    private UUID presa;
    private int carritoId = -1;
    /** La esquina del hotel (en el mundo) y el piso en que anda (1..10). */
    public BlockPos base;
    public int piso = 1;
    private int esquina;
    private Vec3 ultimoLugar = Vec3.ZERO;

    public Botones(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 200).add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 32).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ATTACK_DAMAGE, 6);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CARRITO, true);
    }

    public boolean conCarrito() {
        return entityData.get(CARRITO);
    }

    public Estado estado() {
        return estado;
    }

    private void estado(Estado e) {
        estado = e;
        estadoDesde = tickCount;
    }

    private int hace() {
        return tickCount - estadoDesde;
    }

    // ------------------------------------------------------------------ la cabeza (servidor)

    /** Las cuatro esquinas del pasillo (alrededor del patio) del piso en que anda. */
    private Vec3 esquina(int i) {
        int[][] e = {{17, 17}, {33, 17}, {33, 33}, {17, 33}};
        int[] q = e[Math.floorMod(i, 4)];
        return new Vec3(base.getX() + q[0] + 0.5, base.getY() + (piso - 1) * com.coleccionista.mapa.Hotel.ALTO_PISO + 1, base.getZ() + q[1] + 0.5);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || base == null || showcasing()) {
            return;
        }
        switch (estado) {
            case PATRULLA -> patrulla();
            case VIO -> vio();
            case PERSIGUE -> persigue();
            case BUSCA -> busca();
            case REGRESA -> regresa();
        }
        // los pasos de porcelana (pesados) y las ruedas del carrito que rechinan
        if (walkDist - walkDistO > 0.01F && tickCount % 9 == 0) {
            level().playSound(null, blockPosition(), SoundEvents.DEEPSLATE_TILES_STEP, SoundSource.HOSTILE, 1.1F, 0.55F);
            if (conCarrito() && tickCount % 18 == 0) {
                level().playSound(null, blockPosition(), ModRegistry.HOTEL_CARRITO.get(), SoundSource.HOSTILE, 0.8F, 0.9F + random.nextFloat() * 0.2F);
            }
        }
    }

    private void patrulla() {
        Vec3 a = esquina(esquina);
        if (position().distanceToSqr(a) < 2.5) {
            esquina += random.nextInt(5) == 0 ? -1 : 1;
            if (random.nextInt(3) == 0) {
                playAction("buscar", 60);
                quietoDesde = tickCount;
            }
        }
        if (tickCount - quietoDesde > 60 && (tickCount % 20 == 0 || getNavigation().isDone())) {
            Vec3 b = esquina(esquina);
            getNavigation().moveTo(b.x, b.y, b.z, 0.62);
        }
        atorado();
        ServerPlayer v = buscarPresa();
        if (v != null) {
            presa = v.getUUID();
            getNavigation().stop();
            estado(Estado.VIO);
            playAction("campana", 32);
            getLookControl().setLookAt(v, 180F, 180F);
            level().playSound(null, blockPosition(), ModRegistry.HOTEL_TIMBRE.get(), SoundSource.HOSTILE, 2.2F, 1F);
            Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL botones_vio {}", v.getGameProfile().getName());
        }
    }

    /** Si lleva un rato sin avanzar (un mueble, una esquina rara), cambia de esquina. */
    private void atorado() {
        if (tickCount % 100 == 0) {
            if (position().distanceToSqr(ultimoLugar) < 0.5 && tickCount - quietoDesde > 100) {
                esquina++;
            }
            ultimoLugar = position();
        }
    }

    private void vio() {
        ServerPlayer p = presa();
        if (p != null) {
            getLookControl().setLookAt(p, 180F, 180F);
            lookAt(p, 30F, 30F);
        }
        if (hace() == 14) {
            level().playSound(null, blockPosition(), ModRegistry.HOTEL_TIMBRE.get(), SoundSource.HOSTILE, 2.2F, 0.94F);
        }
        if (hace() >= 30) {
            soltarCarrito();
            sinVer = 0;
            estado(Estado.PERSIGUE);
        }
    }

    private void persigue() {
        ServerPlayer p = presa();
        if (p == null || !JuegoHotel.presaValida(p) || Math.abs(p.getY() - getY()) > 3.5) {
            perder();
            return;
        }
        boolean ve = hasLineOfSight(p) && distanceTo(p) < vista + 6;
        sinVer = ve ? 0 : sinVer + 1;
        if (sinVer > paciencia) {
            perder();
            return;
        }
        getLookControl().setLookAt(p, 30F, 30F);
        if (tickCount % 8 == 0) {
            getNavigation().moveTo(p, 1.0);
        }
        // lo agarra: se lanza, junta las manos de golpe (el golpe pega a la mitad del gesto) y se queda un momento quieto
        double d = distanceToSqr(p);
        if (d < 2.6 * 2.6 && tickCount - ultimoGolpe > 50) {
            ultimoGolpe = tickCount;
            playAction("atacar", 18);
        }
        if (tickCount - ultimoGolpe == 8 && d < 3.0 * 3.0) {
            p.hurt(damageSources().mobAttack(this), danio);
            Vec3 k = p.position().subtract(position()).normalize().scale(0.6);
            p.push(k.x, 0.25, k.z);
            p.hurtMarked = true;
            level().playSound(null, p.blockPosition(), SoundEvents.GLASS_HIT, SoundSource.HOSTILE, 1.2F, 0.6F);
            com.axeliscore.fx.Fx.shake(p, 1.4F, 14);
            Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL botones_golpe {} vida={}", p.getGameProfile().getName(), p.getHealth());
        }
        if (tickCount - ultimoGolpe < 40) {
            getNavigation().stop();
        }
    }

    private void perder() {
        presa = null;
        getNavigation().stop();
        playAction("buscar", 60);
        estado(Estado.BUSCA);
    }

    private void busca() {
        ServerPlayer v = buscarPresa();
        if (v != null && hace() > 20) {
            presa = v.getUUID();
            sinVer = 0;
            level().playSound(null, blockPosition(), ModRegistry.HOTEL_TIMBRE.get(), SoundSource.HOSTILE, 2.2F, 1.05F);
            estado(Estado.PERSIGUE);
            return;
        }
        if (hace() >= 60) {
            estado(Estado.REGRESA);
        }
    }

    private void regresa() {
        Carrito c = carrito();
        if (c == null || c.distanceToSqr(this) > 40 * 40) {
            recogerCarrito(c);
            return;
        }
        if (distanceToSqr(c) < 2.2 * 2.2) {
            recogerCarrito(c);
            return;
        }
        if (tickCount % 20 == 0) {
            getNavigation().moveTo(c.getX(), c.getY(), c.getZ(), 0.7);
        }
        // si no llega en 20 s, el carrito "aparece" junto a él (nadie lo está viendo... o sí)
        if (hace() > 400) {
            recogerCarrito(c);
            return;
        }
        ServerPlayer v = buscarPresa();
        if (v != null) {
            presa = v.getUUID();
            sinVer = 0;
            level().playSound(null, blockPosition(), ModRegistry.HOTEL_TIMBRE.get(), SoundSource.HOSTILE, 2.2F, 1.05F);
            estado(Estado.PERSIGUE);
        }
    }

    private void soltarCarrito() {
        if (!conCarrito() || !(level() instanceof ServerLevel l)) {
            return;
        }
        Carrito c = ModRegistry.CARRITO.get().create(l);
        if (c != null) {
            c.moveTo(getX(), getY(), getZ(), yBodyRot, 0F);
            c.setYRot(yBodyRot);
            l.addFreshEntity(c);
            carritoId = c.getId();
        }
        entityData.set(CARRITO, false);
        level().playSound(null, blockPosition(), SoundEvents.CHAIN_FALL, SoundSource.HOSTILE, 1F, 0.6F);
    }

    private void recogerCarrito(Carrito c) {
        if (c != null) {
            c.discard();
        }
        carritoId = -1;
        entityData.set(CARRITO, true);
        estado(Estado.PATRULLA);
    }

    private Carrito carrito() {
        return carritoId >= 0 && level().getEntity(carritoId) instanceof Carrito c && c.isAlive() ? c : null;
    }

    private ServerPlayer presa() {
        return presa == null || !(level() instanceof ServerLevel l) ? null : (ServerPlayer) l.getPlayerByUUID(presa);
    }

    /** El más cercano que se ve, en este piso. */
    private ServerPlayer buscarPresa() {
        if (tickCount % 5 != 0 || !(level() instanceof ServerLevel l)) {
            return null;
        }
        ServerPlayer mejor = null;
        double md = vista * vista;
        for (ServerPlayer p : l.players()) {
            if (!JuegoHotel.presaValida(p) || Math.abs(p.getY() - getY()) > 2.5) {
                continue;
            }
            double d = distanceToSqr(p);
            if (d < md && hasLineOfSight(p) && (d < 25 || enFrente(p))) {
                md = d;
                mejor = p;
            }
        }
        return mejor;
    }

    /** ¿Está más o menos enfrente (no ve para atrás... casi)? */
    private boolean enFrente(Player p) {
        Vec3 a = getLookAngle(), b = p.position().subtract(position()).normalize();
        return a.x * b.x + a.z * b.z > -0.2;
    }

    /** (Servidor) Se va por el elevador de servicio a otro piso: aparece en una esquina del pasillo con un "ding". */
    public void irAPiso(int k) {
        if (!(level() instanceof ServerLevel l)) {
            return;
        }
        l.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 20, 0.4, 1, 0.4, 0.01);
        Carrito c = carrito();
        if (c != null) {
            c.discard();
        }
        carritoId = -1;
        entityData.set(CARRITO, true);
        presa = null;
        piso = Mth.clamp(k, 1, com.coleccionista.mapa.Hotel.PISOS);
        esquina = random.nextInt(4);
        Vec3 e = esquina(esquina);
        getNavigation().stop();
        moveTo(e.x, e.y, e.z, random.nextFloat() * 360F, 0F);
        estado(Estado.PATRULLA);
        quietoDesde = tickCount;
        playAction("campana", 32);
        l.playSound(null, blockPosition(), ModRegistry.HOTEL_TIMBRE.get(), SoundSource.HOSTILE, 3F, 1.2F);
        Coleccionista.LOGGER.info("COLECCIONISTA_HOTEL botones_piso {}", piso);
    }

    // ------------------------------------------------------------------ no se le puede hacer nada

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(src, amount);
        }
        if (src.getEntity() instanceof Player p && level() instanceof ServerLevel l) {
            // se oye a porcelana: no le pasa nada
            l.playSound(null, blockPosition(), SoundEvents.GLASS_HIT, SoundSource.HOSTILE, 1F, 1.6F);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void registerGoals() {
        // la cabeza va en aiStep (no hay metas de Minecraft)
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
    }

    // ------------------------------------------------------------------ cómo se dibuja (cliente)

    @Override
    public void axAnimate(AnimHost h, float pt) {
        axActions(h, n -> "action");
        float spd = smoothSpeed(h);
        boolean carro = conCarrito();
        String c = carro ? (spd > 0.25F ? "walk" : "idle") : (spd > 3.2F ? "run" : spd > 0.25F ? "caminar" : "idle_solo");
        float vel = c.equals("run") ? Mth.clamp(spd / 5.0F, 0.7F, 1.6F) : c.equals("idle") || c.equals("idle_solo") ? 1F
                : Mth.clamp(spd / 2.0F, 0.6F, 1.6F);
        if (h.clip(c) != null) {
            h.animator().play("base", h.clip(c), 0.3F, vel, false);
        }
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        Geo geo = h.geo();
        pose.hide(geo.bone("carrito"), !conCarrito());
        // las ruedas del carrito giran al avanzar
        if (conCarrito()) {
            float giro = (walkDist + (walkDist - walkDistO) * pt) * 160F * (float) Math.PI / 180F;
            for (int i = 1; i <= 4; i++) {
                pose.addRot(geo.bone("rueda_" + i), giro, 0, 0);
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2.2, 0.8, 2.2);
    }
}
