package com.navidad.entidad;

import com.navidad.Logros;
import com.navidad.Navidad;
import com.navidad.bloque.CajaRegalo;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Un regalo que Santa suelta desde su trineo: cae rápido un momento, se abre su paracaídas de gajos rojos y blancos y baja despacito
 * meciéndose, hacia la persona para la que es. Al tocar el suelo queda como una caja de regalo de verdad ("De: Santa, Para: ...");
 * si cae al agua, en lava o no hay dónde ponerlo, queda como objeto. Se puede atrapar en el aire con clic (derecho o izquierdo).
 */
public class RegaloCayendo extends Entity {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(RegaloCayendo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ABIERTO = SynchedEntityData.defineId(RegaloCayendo.class, EntityDataSerializers.BOOLEAN);
    /** Los adornos de regalo con los que se dibuja (según el color). */
    public static final String[] COLORES = {"regalo_rojo", "regalo_verde", "regalo_azul", "regalo_dorado"};

    public ItemStack contenido = ItemStack.EMPTY;
    public String para = "";
    private double haciaX, haciaZ;
    private boolean conDestino;
    private int vida;
    /** (Cliente) desde cuándo está abierto el paracaídas, para su animación de abrirse. */
    public int abiertoDesde = -1;
    /** Cuántos regalos han aterrizado como caja (para la prueba con fotos). */
    public static int aterrizados;

    public RegaloCayendo(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** (Servidor) Un regalo para {@code p}, soltado en (x, y, z). */
    public static RegaloCayendo soltar(ServerLevel l, double x, double y, double z, ItemStack contenido, ServerPlayer p) {
        RegaloCayendo r = ModRegistry.REGALO_CAYENDO.get().create(l);
        if (r == null) {
            return null;
        }
        r.moveTo(x, y, z, l.random.nextFloat() * 360F, 0);
        r.contenido = contenido.copy();
        r.entityData.set(COLOR, l.random.nextInt(COLORES.length));
        if (p != null) {
            r.para = p.getGameProfile().getName();
            r.haciaX = p.getX() + (l.random.nextDouble() - 0.5) * 8;
            r.haciaZ = p.getZ() + (l.random.nextDouble() - 0.5) * 8;
            r.conDestino = true;
        }
        r.setDeltaMovement((l.random.nextDouble() - 0.5) * 0.2, -0.1, (l.random.nextDouble() - 0.5) * 0.2);
        l.addFreshEntity(r);
        return r;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR, 0);
        this.entityData.define(ABIERTO, false);
    }

    public int color() {
        return Mth.clamp(this.entityData.get(COLOR), 0, COLORES.length - 1);
    }

    public boolean abierto() {
        return this.entityData.get(ABIERTO);
    }

    @Override
    public void tick() {
        super.tick();
        vida++;
        boolean abre = abierto();
        if (!level().isClientSide && !abre && vida > 14) {
            this.entityData.set(ABIERTO, true);
            level().playSound(null, blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.AMBIENT, 1.5F, 0.6F);
            abre = true;
        }
        if (level().isClientSide && abre && abiertoDesde < 0) {
            abiertoDesde = tickCount;
        }
        Vec3 v = getDeltaMovement();
        double vy = abre ? v.y + (-0.075 - v.y) * 0.18 : Math.max(v.y - 0.04, -0.6);
        double vx = v.x * 0.92, vz = v.z * 0.92;
        if (abre) {
            // se mece y el aire lo lleva hacia quien es el regalo
            vx += Mth.sin(tickCount * 0.06F) * 0.006;
            vz += Mth.cos(tickCount * 0.05F) * 0.006;
            if (conDestino) {
                vx += Mth.clamp((haciaX - getX()) * 0.004, -0.012, 0.012);
                vz += Mth.clamp((haciaZ - getZ()) * 0.004, -0.012, 0.012);
            }
        }
        setDeltaMovement(vx, vy, vz);
        move(MoverType.SELF, getDeltaMovement());
        if (level().isClientSide) {
            if (abre && tickCount % 6 == 0) {
                level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 0.6, getY() - 0.1,
                        getZ() + (random.nextDouble() - 0.5) * 0.6, 0, -0.03, 0);
            }
            return;
        }
        if (onGround() || isInWater() || isInLava() || vida > 2400) {
            aterrizar();
        }
    }

    private void aterrizar() {
        ServerLevel l = (ServerLevel) level();
        BlockPos base = blockPosition();
        Direction mira = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        boolean caja = false;
        if (!isInWater() && !isInLava()) {
            for (BlockPos p : new BlockPos[]{base, base.above(), base.north(), base.south(), base.east(), base.west()}) {
                if (CajaRegalo.poner(l, p, mira, contenido, "Santa", para)) {
                    caja = true;
                    base = p;
                    break;
                }
            }
        }
        if (!caja) {
            ItemEntity it = new ItemEntity(l, getX(), getY() + 0.3, getZ(), CajaRegalo.envuelta(contenido, "Santa", para));
            it.setDeltaMovement(0, 0.15, 0);
            l.addFreshEntity(it);
        } else {
            aterrizados++;
        }
        double x = base.getX() + 0.5, y = base.getY() + 0.3, z = base.getZ() + 0.5;
        l.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 0.4, z, 24, 0.5, 0.3, 0.5, 0.04);
        l.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.3, z, 8, 0.4, 0.3, 0.4, 0.02);
        l.playSound(null, base, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1F, 0.8F);
        l.playSound(null, base, ModRegistry.CASCABEL.get(), SoundSource.BLOCKS, 0.7F, 1.4F);
        ServerPlayer p = l.getServer().getPlayerList().getPlayerByName(para);
        if (p != null && p.level() == l && p.distanceToSqr(this) < 48 * 48) {
            p.displayClientMessage(Component.translatable("navidad.regalo_cielo.aterrizo").withStyle(ChatFormatting.GOLD), true);
            Logros.dar(p, "regalo_cielo");
        }
        Navidad.LOGGER.info("NAVIDAD_REGALO_CAYO {} caja={} para={}", base.toShortString(), caja, para);
        discard();
    }

    /** Lo atrapa alguien en el aire: se lo lleva envuelto. */
    private void atrapar(Player p) {
        if (level().isClientSide || isRemoved()) {
            return;
        }
        ItemStack st = CajaRegalo.envuelta(contenido, "Santa", para);
        if (!p.getInventory().add(st)) {
            p.drop(st, false);
        }
        level().playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1F, 0.8F);
        level().playSound(null, blockPosition(), ModRegistry.CASCABEL.get(), SoundSource.PLAYERS, 0.8F, 1.5F);
        ((ServerLevel) level()).sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.5, getZ(), 20, 0.4, 0.4, 0.4, 0.05);
        p.displayClientMessage(Component.translatable("navidad.regalo_cielo.atrapado").withStyle(ChatFormatting.AQUA), true);
        if (p instanceof ServerPlayer sp) {
            Logros.dar(sp, "regalo_cielo");
        }
        discard();
    }

    @Override
    public InteractionResult interact(Player p, InteractionHand hand) {
        atrapar(p);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.getEntity() instanceof Player p) {
            atrapar(p);
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 160 * 160;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        contenido = t.contains("Contenido") ? ItemStack.of(t.getCompound("Contenido")) : ItemStack.EMPTY;
        para = t.getString("Para");
        this.entityData.set(COLOR, t.getInt("Color"));
        this.entityData.set(ABIERTO, t.getBoolean("Abierto"));
        vida = t.getInt("Vida");
        conDestino = t.contains("HaciaX");
        haciaX = t.getDouble("HaciaX");
        haciaZ = t.getDouble("HaciaZ");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        if (!contenido.isEmpty()) {
            t.put("Contenido", contenido.save(new CompoundTag()));
        }
        t.putString("Para", para);
        t.putInt("Color", color());
        t.putBoolean("Abierto", abierto());
        t.putInt("Vida", vida);
        if (conDestino) {
            t.putDouble("HaciaX", haciaX);
            t.putDouble("HaciaZ", haciaZ);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
