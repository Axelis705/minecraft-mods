package com.casino.west;

import com.casino.data.CasinoStats;
import com.casino.entity.DealerAnim;
import com.casino.entity.DealerEntity;
import com.casino.game.ArmRules;
import com.casino.plat.Nbt;
import com.casino.plat.Plat;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Mesa de vencidas. Lado 0 (quien retó) del lado de FACING; lado 1 (forzudo o el otro jugador) del lado contrario. Tras la cuenta
 * regresiva cada jugador aprieta A y D alternando; su pantalla manda cuántos golpes dio y aquí se mueven las manos
 * ({@link ArmRules}). Gana quien tumbe la mano del otro.
 */
public class ArmBE extends MatchBE {
    /** Dónde están las manos: +1 = ganó el lado 0, -1 = ganó el lado 1. */
    private double x;
    private final double[] rate = new double[2];
    private final int[] taps = new int[2];
    private double phaseOff;
    private int bestTicks;
    private String bestName = "";

    public ArmBE(BlockPos pos, BlockState state) {
        super(WestReg.ARM_BE.get(), pos, state);
    }

    @Override
    public BlockPos spot(int side) {
        return side == 0 ? worldPosition.relative(facing()) : worldPosition.relative(facing().getOpposite());
    }

    @Override
    public float yaw(int side) {
        return side == 0 ? facing().getOpposite().toYRot() : facing().toYRot();
    }

    @Override
    protected int greeting() {
        return DealerEntity.SAY + 5;
    }

    @Override
    protected double leash() {
        return 1.6;
    }

    @Override
    protected int npcVariant(int o) {
        return DealerEntity.WEST + ArmRules.SKIN[o];
    }

    @Override
    public int opponents() {
        return ArmRules.OPPONENTS;
    }

    @Override
    protected boolean validBet(int b) {
        return ArmRules.validBet(b);
    }

    @Override
    protected long npcPayout(int b, int o) {
        return ArmRules.payout(b, o);
    }

    @Override
    protected long pvpPayout(int b) {
        return ArmRules.pvpPayout(b);
    }

    @Override
    protected int statGame() {
        return CasinoStats.ARM;
    }

    @Override
    protected String lang() {
        return "casino.arm.";
    }

    public double x() {
        return x;
    }

    /** Ritmo (golpes/s) de cada lado, para la pantalla. */
    public double rate(int side) {
        return rate[side];
    }

    public int bestTicks() {
        return bestTicks;
    }

    public String bestName() {
        return bestName;
    }

    /** Ticks de lucha (negativo durante la cuenta regresiva). */
    public double fightTime(float pt) {
        return time(pt) - ArmRules.COUNTDOWN;
    }

    private void npcPose() {
        DealerEntity d = npcEntity();
        if (d != null) {
            // el forzudo es el lado 1: para él "ganar" es x negativo
            d.setCombat(100 + (int) Math.round((-x + 1) * 50));
        }
    }

    @Override
    protected void onStart(long now) {
        x = 0;
        rate[0] = rate[1] = 0;
        taps[0] = taps[1] = 0;
        phaseOff = level.getRandom().nextDouble() * 6.28;
        npcPose();
        level.playSound(null, worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    /** Golpes de un jugador (su pantalla los manda cada tick). */
    public void taps(ServerPlayer p, int n) {
        int s = side(p.getUUID());
        if (phase != RUNNING || s < 0 || (mode == NPC && s == 1) || fightTime(0) < 0) {
            return;
        }
        taps[s] = Math.min(ArmRules.MAX_TAPS_PER_TICK, taps[s] + Math.max(0, n));
    }

    @Override
    protected void onRunning(long now) {
        int t = (int) (now - start) - ArmRules.COUNTDOWN;
        if (t < 0) {
            if (t % 20 == 0) {
                level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.9F, 1.2F);
            }
            taps[0] = taps[1] = 0;
            return;
        }
        if (t == 0) {
            level.playSound(null, worldPosition, ModSounds.BULL_BELL.get(), SoundSource.BLOCKS, 1.0F, 1.2F);
        }
        rate[0] = ArmRules.rate(rate[0], taps[0]);
        rate[1] = mode == NPC ? ArmRules.npcRate(opp, t, phaseOff) : ArmRules.rate(rate[1], taps[1]);
        taps[0] = taps[1] = 0;
        x = ArmRules.step(x, rate[0], rate[1], level.getRandom().nextGaussian());
        npcPose();
        if (t % 25 == 10) {
            level.playSound(null, worldPosition, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.4F, 0.6F + level.getRandom().nextFloat() * 0.3F);
        }
        int w = ArmRules.winner(x);
        if (w >= 0) {
            finish(w, R_BEAT);
            return;
        }
        if (t >= ArmRules.MAX_TICKS) {
            finish(Math.abs(x) < 0.02 ? TIE : x > 0 ? 0 : 1, R_TIMEOUT);
            return;
        }
        if (t % 2 == 0) {
            sync();
        }
    }

    @Override
    protected void onFinish(int w, int why, ServerPlayer winP, ServerPlayer loseP) {
        if (w == 0 || w == 1) {
            x = w == 0 ? 1 : -1;
        }
        npcPose();
        DealerEntity d = npcEntity();
        if (why == R_BEAT || why == R_TIMEOUT) {
            level.playSound(null, worldPosition, ModSounds.ARM_SLAM.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
            ((ServerLevel) level).sendParticles(ParticleTypes.CRIT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                    10, 0.2, 0.1, 0.2, 0.1);
        }
        if (d != null && w != TIE) {
            d.anim(w == 1 ? DealerAnim.CHEER : DealerAnim.SHAKE);
            d.say(DealerEntity.SAY + (w == 1 ? 6 : 7));
        }
        // los que miran se emocionan
        for (DealerEntity e : level.getEntitiesOfClass(DealerEntity.class, new AABB(worldPosition).inflate(8))) {
            if (e.isPatron() && level.getRandom().nextBoolean()) {
                e.anim(DealerAnim.CLAP);
            }
        }
        int t = (int) (level.getGameTime() - start) - ArmRules.COUNTDOWN;
        if (winP != null && why == R_BEAT && (bestTicks <= 0 || t < bestTicks)) {
            bestTicks = t;
            bestName = winP.getGameProfile().getName();
            Plat.msg(winP, Component.translatable("casino.arm.record", String.format(java.util.Locale.ROOT, "%.1f", t / 20.0))
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        }
    }

    public void clientTick() {
        if (phase != IDLE) {
            com.casino.plat.Net.onClient(() -> com.casino.client.WestClient.armSeen(this));
        }
    }

    @Override
    protected AABB renderBox() {
        return new AABB(worldPosition).inflate(1.5, 2.5, 1.5);
    }

    @Override
    protected void writeMatch(CompoundTag tag) {
        tag.putDouble("X", x);
        tag.putDouble("Rate0", rate[0]);
        tag.putDouble("Rate1", rate[1]);
        tag.putDouble("PhaseOff", phaseOff);
        tag.putInt("BestTicks", bestTicks);
        tag.putString("BestName", bestName);
    }

    @Override
    protected void readMatch(CompoundTag tag) {
        x = Nbt.d(tag, "X");
        rate[0] = Nbt.d(tag, "Rate0");
        rate[1] = Nbt.d(tag, "Rate1");
        phaseOff = Nbt.d(tag, "PhaseOff");
        bestTicks = Nbt.i(tag, "BestTicks");
        bestName = Nbt.s(tag, "BestName");
    }
}
