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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * El trineo de Santa cruzando el cielo: pasa en línea recta, suenan los cascabeles, Santa se ríe, saluda ("¡Jo, jo, jo! ¡Feliz
 * Navidad!") a quien está abajo y le suelta dos regalos con paracaídas. Se va solo al terminar su recorrido.
 */
public class TrineoSanta extends Entity {
    private int vida = 560, regalos;
    /** Cuántos regalos le soltó a cada quien en este viaje. */
    private final java.util.Map<java.util.UUID, Integer> DADOS = new java.util.HashMap<>();

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
        // al pasar encima de alguien: lo saluda y suelta regalos con paracaídas (2 por persona, como mucho 8 por viaje)
        if (tickCount % 12 == 0 && regalos < 8) {
            for (ServerPlayer p : l.players()) {
                double dx = p.getX() - getX(), dz = p.getZ() - getZ();
                if (dx * dx + dz * dz > 26 * 26 || Math.abs(p.getY() - getY()) > 80) {
                    continue;
                }
                int ya = DADOS.getOrDefault(p.getUUID(), 0);
                if (ya == 0) {
                    saludar(p);
                }
                if (ya < 2) {
                    DADOS.put(p.getUUID(), ya + 1);
                    RegaloCayendo.soltar(l, getX(), getY() - 1.2, getZ(), premio(l.random), p);
                    regalos++;
                    break;
                }
            }
        }
        if (--vida <= 0) {
            discard();
        }
    }

    /** Lo que trae cada regalo: casi siempre algo sencillo, a veces algo bueno y muy de vez en cuando algo increíble. */
    static ItemStack premio(net.minecraft.util.RandomSource r) {
        float x = r.nextFloat();
        var rz = x < 0.03F ? com.navidad.objeto.Premios.Rareza.EPICO : x < 0.25F ? com.navidad.objeto.Premios.Rareza.RARO
                : com.navidad.objeto.Premios.Rareza.COMUN;
        return com.navidad.objeto.Premios.sacar(rz, r);
    }

    private void saludar(ServerPlayer p) {
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(6, 50, 20));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                net.minecraft.network.chat.Component.translatable("navidad.santa.jojojo").withStyle(net.minecraft.ChatFormatting.RED,
                        net.minecraft.ChatFormatting.BOLD)));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                net.minecraft.network.chat.Component.translatable("navidad.santa.feliz", p.getDisplayName())
                        .withStyle(net.minecraft.ChatFormatting.GOLD)));
        com.navidad.Navidad.LOGGER.info("NAVIDAD_SANTA saluda a {}", p.getGameProfile().getName());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 300 * 300;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        if (t.contains("Vida")) {
            vida = t.getInt("Vida");
        }
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
