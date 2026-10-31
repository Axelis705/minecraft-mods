package com.casino.entity;

import com.casino.net.CasinoNet;
import com.casino.net.OpenBlackjackPacket;
import com.casino.net.OpenPokerPacket;
import com.casino.net.OpenRoulettePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
    /** Qué mesa atiende: 0 = blackjack, 1 = ruleta (cambia la ropa y qué panel se abre). */
    public static final int GAME_BLACKJACK = 0;
    public static final int GAME_ROULETTE = 1;
    public static final int GAME_POKER = 2;
    private static final EntityDataAccessor<Integer> GAME = SynchedEntityData.defineId(DealerEntity.class, EntityDataSerializers.INT);

    /** Consejo que está diciendo ahora (-1 = ninguno): el crupier levanta la mano y sobre su cabeza sale un globo con el texto. */
    private static final EntityDataAccessor<Integer> TIP = SynchedEntityData.defineId(DealerEntity.class, EntityDataSerializers.INT);
    public static final int TIPS_ROULETTE = 12;
    public static final int TIPS_BLACKJACK = 10;
    public static final int TIPS_POKER = 10;
    private static final int TIP_TICKS = 180;

    @Nullable
    private BlockPos table;
    private int tipLeft;
    private int nextTip = 400 + (int) (Math.random() * 400);

    public DealerEntity(EntityType<? extends DealerEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GAME, GAME_BLACKJACK);
        this.entityData.define(TIP, -1);
    }

    public int getTip() {
        return this.entityData.get(TIP);
    }

    /** Cada tanto, si hay alguien cerca, dice un consejo al azar de su juego. */
    private void tickTips() {
        if (tipLeft > 0) {
            if (--tipLeft == 0) {
                this.entityData.set(TIP, -1);
            }
            return;
        }
        if (--nextTip > 0) {
            return;
        }
        nextTip = 900 + random.nextInt(900); // entre 45 y 90 segundos
        if (level().getNearestPlayer(this, 8.0) == null) {
            nextTip = 200;
            return;
        }
        int n = getGame() == GAME_ROULETTE ? TIPS_ROULETTE : getGame() == GAME_POKER ? TIPS_POKER : TIPS_BLACKJACK;
        this.entityData.set(TIP, random.nextInt(n));
        tipLeft = TIP_TICKS;
        level().playSound(null, getX(), getY() + 1.6, getZ(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.NEUTRAL, 0.6F, 1.3F);
    }

    public int getGame() {
        return this.entityData.get(GAME);
    }

    public void setGame(int game) {
        this.entityData.set(GAME, game);
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
            tickTips();
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
            if (getGame() == GAME_POKER) {
                CasinoNet.sendTo(sp, new OpenPokerPacket(table));
            } else if (getGame() == GAME_ROULETTE) {
                CasinoNet.sendTo(sp, new OpenRoulettePacket(table));
            } else {
                CasinoNet.sendTo(sp, new OpenBlackjackPacket(table));
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Game", getGame());
        if (table != null) {
            tag.putIntArray("Table", new int[]{table.getX(), table.getY(), table.getZ()});
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setGame(tag.getInt("Game"));
        int[] t = tag.getIntArray("Table");
        table = t.length == 3 ? new BlockPos(t[0], t[1], t[2]) : null;
    }
}
