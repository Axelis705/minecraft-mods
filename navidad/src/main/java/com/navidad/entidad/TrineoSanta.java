package com.navidad.entidad;

import com.navidad.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * El trineo de Santa cruzando el cielo de noche: pasa en línea recta, suenan los cascabeles, Santa se ríe y va soltando regalos
 * misteriosos cerca de los jugadores que están abajo. Se va solo al terminar su recorrido.
 */
public class TrineoSanta extends Entity {
    private int vida = 520, regalos;

    public TrineoSanta(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        setPos(getX() + v.x, getY() + v.y + Math.sin(tickCount * 0.08) * 0.03, getZ() + v.z);
        if (level().isClientSide) {
            if (tickCount % 2 == 0) {
                level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, getX() - v.x * 4, getY() + 0.5, getZ() - v.z * 4, 0, -0.02, 0);
                level().addParticle(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, getX() - v.x * 6, getY() + 0.3, getZ() - v.z * 6, 0, -0.05, 0);
            }
            return;
        }
        ServerLevel l = (ServerLevel) level();
        if (tickCount % 40 == 0) {
            l.playSound(null, blockPosition(), ModRegistry.CASCABELES.get(), SoundSource.AMBIENT, 6F, 1F);
        }
        if (tickCount % 160 == 60) {
            l.playSound(null, blockPosition(), ModRegistry.RISA_SANTA.get(), SoundSource.AMBIENT, 6F, 1F);
        }
        // suelta regalos cuando pasa encima de alguien (como mucho 6 por viaje)
        if (tickCount % 25 == 0 && regalos < 6) {
            for (ServerPlayer p : l.players()) {
                double dx = p.getX() - getX(), dz = p.getZ() - getZ();
                if (dx * dx + dz * dz < 18 * 18 && Math.abs(p.getY() - getY()) < 60) {
                    ItemEntity it = new ItemEntity(l, getX(), getY() - 1, getZ(), new ItemStack(ModRegistry.REGALO_MISTERIOSO.get()));
                    it.setDeltaMovement((p.getX() - getX()) * 0.02, -0.1, (p.getZ() - getZ()) * 0.02);
                    it.setNoPickUpDelay();
                    it.setGlowingTag(true);
                    l.addFreshEntity(it);
                    regalos++;
                    break;
                }
            }
        }
        if (--vida <= 0) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 300 * 300;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        vida = t.getInt("Vida");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putInt("Vida", vida);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
