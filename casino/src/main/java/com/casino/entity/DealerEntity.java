package com.casino.entity;

import com.casino.net.CasinoNet;
import com.casino.net.OpenBlackjackPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * El crupier: un personaje fijo y invulnerable que se queda detrás de su mesa de blackjack, mira al jugador más cercano y reparte
 * (mueve el brazo). Al hacerle clic derecho se abre la mesa. Lo coloca la mesa sola y lo quita cuando se rompe la mesa.
 */
public class DealerEntity extends Mob {
    @Nullable
    private BlockPos table;

    public DealerEntity(EntityType<? extends DealerEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public void setTable(BlockPos pos) {
        this.table = pos;
    }

    @Nullable
    public BlockPos getTable() {
        return table;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        // El cuerpo siempre mira hacia donde mira la mesa; solo la cabeza sigue al jugador más cercano.
        float base = getYRot();
        yBodyRot = base;
        yBodyRotO = base;
        if (!level().isClientSide) {
            Player p = level().getNearestPlayer(this, 7.0);
            if (p != null) {
                double dx = p.getX() - getX();
                double dz = p.getZ() - getZ();
                double dy = p.getEyeY() - getEyeY();
                float want = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                float rel = Mth.clamp(Mth.wrapDegrees(want - base), -70.0F, 70.0F);
                yHeadRot = base + rel;
                setXRot(Mth.clamp((float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI)), -30.0F, 30.0F));
            } else {
                yHeadRot = base;
                setXRot(0.0F);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide && table != null && player instanceof ServerPlayer sp) {
            CasinoNet.sendTo(sp, new OpenBlackjackPacket(table));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (table != null) {
            tag.putIntArray("Table", new int[]{table.getX(), table.getY(), table.getZ()});
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int[] t = tag.getIntArray("Table");
        table = t.length == 3 ? new BlockPos(t[0], t[1], t[2]) : null;
    }
}
