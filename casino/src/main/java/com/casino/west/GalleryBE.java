package com.casino.west;

import com.casino.chip.Chips;
import com.casino.data.CasinoStats;
import com.casino.game.GalleryPlan;
import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import com.casino.plat.Plat;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.UUID;

/**
 * La galería de tiro: quién está tirando, la semilla de los blancos (el cliente dibuja lo mismo), balas, puntos y qué blancos ya
 * cayeron. El servidor revisa cada disparo con el rayo de la mirada del jugador. También guarda el récord para el marcador.
 */
public class GalleryBE extends CasinoBE implements GunGame {
    /** Cuánto se puede alejar el tirador del mostrador antes de que se acabe su partida. */
    private static final double LEASH = 5.0;

    private UUID shooter;
    private String shooterName = "";
    private long seed;
    /** Tick del mundo en que salen los blancos (antes hay cuenta regresiva). */
    private long start;
    private int shots;
    private int score;
    private int bet;
    private int[] hits = new int[0];
    private int[] hitAt = new int[0];
    private long lastShot = -100;
    private long resultUntil;
    private int resultScore;
    private int resultPay;
    private int bestScore;
    private String bestName = "";
    private GalleryPlan plan;

    public GalleryBE(BlockPos pos, BlockState state) {
        super(WestReg.GALLERY_BE.get(), pos, state);
    }

    // ------------------------------------------------------------------ consultas

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(WestBlocks.FACING) ? s.getValue(WestBlocks.FACING) : Direction.SOUTH;
    }

    public boolean playing() {
        return shooter != null;
    }

    @Override
    public boolean isShooter(UUID id) {
        return shooter != null && shooter.equals(id);
    }

    public String shooterName() {
        return shooterName;
    }

    public GalleryPlan plan() {
        if (plan == null || plan.seed != seed) {
            plan = new GalleryPlan(seed);
        }
        return plan;
    }

    /** Ticks de partida (negativo durante la cuenta regresiva). */
    public double time(float pt) {
        return level == null ? 0 : level.getGameTime() + pt - start;
    }

    public int shots() {
        return shots;
    }

    public int score() {
        return score;
    }

    public int bet() {
        return bet;
    }

    public int bestScore() {
        return bestScore;
    }

    public String bestName() {
        return bestName;
    }

    /** Tick (de partida) en que cayó ese blanco, o -1. */
    public int hitAt(int id) {
        for (int i = 0; i < hits.length; i++) {
            if (hits[i] == id) {
                return hitAt[i];
            }
        }
        return -1;
    }

    public boolean down(int id) {
        return hitAt(id) >= 0;
    }

    /** Resultado de la última partida, mientras se muestra en el marcador (o -1). */
    public int resultScore() {
        return level != null && level.getGameTime() < resultUntil ? resultScore : -1;
    }

    public int resultPay() {
        return resultPay;
    }

    /** Del sistema del mostrador al mundo: u a la derecha del tirador, v arriba desde la base del bloque, w hacia el tirador. */
    public Vec3 world(double u, double v, double w) {
        Direction f = facing(), r = f.getCounterClockWise();
        return new Vec3(worldPosition.getX() + 0.5 + r.getStepX() * u + f.getStepX() * w, worldPosition.getY() + v,
                worldPosition.getZ() + 0.5 + r.getStepZ() * u + f.getStepZ() * w);
    }

    private double[] local(Vec3 p, boolean dir) {
        Direction f = facing(), r = f.getCounterClockWise();
        double x = dir ? p.x : p.x - worldPosition.getX() - 0.5, y = dir ? p.y : p.y - worldPosition.getY(), z = dir ? p.z : p.z - worldPosition.getZ() - 0.5;
        return new double[]{x * r.getStepX() + z * r.getStepZ(), y, x * f.getStepX() + z * f.getStepZ()};
    }

    private Vec3 center() {
        return Vec3.atCenterOf(worldPosition);
    }

    // ------------------------------------------------------------------ jugar

    /** Empieza una partida (servidor): cobra la apuesta y presta el revólver. */
    public void begin(ServerPlayer p, int betChips) {
        if (level == null || playing() || !GalleryPlan.validBet(betChips)) {
            return;
        }
        if (!Chips.take(p, betChips)) {
            Plat.msg(p, Component.translatable("casino.plinko.no_chips", betChips).withStyle(ChatFormatting.RED), true);
            level.playSound(null, worldPosition, ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            return;
        }
        shooter = p.getUUID();
        shooterName = p.getGameProfile().getName();
        seed = level.getRandom().nextLong();
        start = level.getGameTime() + GalleryPlan.COUNTDOWN;
        shots = GalleryPlan.SHOTS;
        score = 0;
        bet = betChips;
        hits = new int[0];
        hitAt = new int[0];
        lastShot = -100;
        resultUntil = 0;
        GunGame.lend(p, worldPosition);
        Plat.msg(p, Component.translatable("casino.gallery.start", GalleryPlan.SHOTS, GalleryPlan.DURATION / 20).withStyle(ChatFormatting.GOLD), false);
        level.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 1.0F);
        sync();
    }

    /** Un disparo del tirador (servidor). */
    @Override
    public void shoot(ServerPlayer p) {
        if (level == null || !isShooter(p.getUUID()) || shots <= 0) {
            return;
        }
        long now = level.getGameTime();
        long t = now - start;
        if (t < 0) {
            Plat.msg(p, Component.translatable("casino.gallery.wait").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (now - lastShot < GalleryPlan.COOLDOWN || t >= GalleryPlan.DURATION) {
            return;
        }
        lastShot = now;
        shots--;
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0F);
        double[] o = local(eye, false), d = local(look, true);
        GalleryPlan pl = plan();
        int id = -1;
        // el tirador ve los blancos un poquito atrasados (lo que tarda en llegar la información): se revisa también un par de ticks antes
        for (int back = 0; back <= 3 && id < 0; back++) {
            id = pl.hit(t - back, o[0], o[1], o[2], d[0], d[1], d[2], this::down);
        }
        ServerLevel sl = (ServerLevel) level;
        Vec3 muzzle = eye.add(look.scale(0.9)).add(0, -0.25, 0);
        sl.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 4, 0.03, 0.03, 0.03, 0.01);
        level.playSound(null, p.getX(), p.getY() + 1, p.getZ(), ModSounds.GALLERY_SHOT.get(), SoundSource.PLAYERS, 0.9F,
                0.95F + level.getRandom().nextFloat() * 0.1F);
        if (id >= 0) {
            GalleryPlan.Target g = pl.get(id);
            hits = Arrays.copyOf(hits, hits.length + 1);
            hitAt = Arrays.copyOf(hitAt, hitAt.length + 1);
            hits[hits.length - 1] = id;
            hitAt[hitAt.length - 1] = (int) t;
            score += GalleryPlan.POINTS[g.type];
            Vec3 at = world(g.u(t), g.v(t), g.w());
            switch (g.type) {
                case GalleryPlan.DUCK -> {
                    level.playSound(null, at.x, at.y, at.z, ModSounds.GALLERY_QUACK.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
                    sl.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.02);
                }
                case GalleryPlan.BOTTLE -> {
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.9F, 1.2F);
                    sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                            net.minecraft.world.level.block.Blocks.GREEN_STAINED_GLASS.defaultBlockState()), at.x, at.y, at.z, 14, 0.08, 0.12, 0.08, 0.1);
                }
                case GalleryPlan.LADY -> {
                    level.playSound(null, at.x, at.y, at.z, ModSounds.GALLERY_BUZZ.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
                    Plat.msg(p, Component.translatable("casino.gallery.lady").withStyle(ChatFormatting.RED), true);
                }
                case GalleryPlan.STAR -> {
                    level.playSound(null, at.x, at.y, at.z, ModSounds.GALLERY_DING.get(), SoundSource.BLOCKS, 1.0F, 1.5F);
                    sl.sendParticles(ParticleTypes.FIREWORK, at.x, at.y, at.z, 12, 0.1, 0.1, 0.1, 0.06);
                }
                default -> sl.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.15, 0.2, 0.05, 0.1);
            }
            if (g.type != GalleryPlan.LADY) {
                level.playSound(null, at.x, at.y, at.z, ModSounds.GALLERY_DING.get(), SoundSource.BLOCKS, 0.8F, 1.0F + 0.08F * g.type);
                Plat.msg(p, Component.translatable("casino.gallery.hit", GalleryPlan.POINTS[g.type], score).withStyle(ChatFormatting.GREEN), true);
            }
        } else if (d[2] < -1e-3) {
            // agujero en la pared del fondo
            double s = (GalleryPlan.W_STAR - 0.9 - o[2]) / d[2];
            Vec3 at = world(o[0] + d[0] * s, o[1] + d[1] * s, o[2] + d[2] * s);
            sl.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 3, 0.02, 0.02, 0.02, 0.0);
        }
        sync();
    }

    public void serverTick() {
        if (level == null || shooter == null) {
            return;
        }
        long now = level.getGameTime();
        long t = now - start;
        ServerPlayer p = level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(shooter);
        if (p == null || p.level() != level || p.distanceToSqr(center()) > LEASH * LEASH) {
            end(p, true);
            return;
        }
        if (t < 0 && (-t) % 20 == 0) {
            level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.8F, 1.3F);
        } else if (t == 0) {
            level.playSound(null, worldPosition, ModSounds.BULL_BELL.get(), SoundSource.BLOCKS, 0.9F, 1.3F);
        }
        if (t >= GalleryPlan.DURATION || (shots <= 0 && now - lastShot > 12)) {
            end(p, false);
        }
    }

    private void end(ServerPlayer p, boolean left) {
        if (level == null) {
            return;
        }
        int pts = Math.max(0, score);
        long pay = GalleryPlan.payout(bet, pts);
        if (p != null) {
            if (pay > 0) {
                Chips.give(p, (int) pay);
            }
            CasinoStats.play(p, CasinoStats.GALLERY, bet, pay);
            GunGame.takeBack(p);
            Component tail = pay > 0 ? Component.translatable("casino.gallery.won", pay).withStyle(ChatFormatting.GREEN)
                    : Component.translatable("casino.gallery.lost", bet).withStyle(ChatFormatting.RED);
            Plat.msg(p, Component.translatable(left ? "casino.gallery.left" : "casino.gallery.end", pts).withStyle(ChatFormatting.GOLD)
                    .append(" ").append(tail), false);
            Plat.msg(p, tail, true);
            if (pts > bestScore) {
                bestScore = pts;
                bestName = p.getGameProfile().getName();
                Plat.msg(p, Component.translatable("casino.gallery.record", pts).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
            }
        }
        level.playSound(null, worldPosition, pay > 0 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.7F, 1.0F);
        resultScore = pts;
        resultPay = (int) pay;
        resultUntil = level.getGameTime() + 120;
        shooter = null;
        shooterName = "";
        setChanged();
        sync();
    }

    public void clientTick() {
        if (shooter != null) {
            com.casino.plat.Net.onClient(() -> com.casino.client.WestClient.gallerySeen(this));
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected AABB renderBox() {
        return new AABB(world(-6, -1, 1.5), world(6, 4, -12.5));
    }

    @Override
    protected void writeData(CompoundTag tag) {
        if (shooter != null) {
            Nbt.putUuid(tag, "Shooter", shooter);
        }
        tag.putString("ShooterName", shooterName);
        tag.putLong("Seed", seed);
        tag.putLong("Start", start);
        tag.putInt("Shots", shots);
        tag.putInt("Score", score);
        tag.putInt("Bet", bet);
        tag.putIntArray("Hits", hits);
        tag.putIntArray("HitAt", hitAt);
        tag.putLong("ResultUntil", resultUntil);
        tag.putInt("ResultScore", resultScore);
        tag.putInt("ResultPay", resultPay);
        tag.putInt("BestScore", bestScore);
        tag.putString("BestName", bestName);
    }

    @Override
    protected void readData(CompoundTag tag) {
        shooter = Nbt.hasUuid(tag, "Shooter") ? Nbt.uuid(tag, "Shooter") : null;
        shooterName = Nbt.s(tag, "ShooterName");
        seed = Nbt.l(tag, "Seed");
        start = Nbt.l(tag, "Start");
        shots = Nbt.i(tag, "Shots");
        score = Nbt.i(tag, "Score");
        bet = Nbt.i(tag, "Bet");
        hits = Nbt.ia(tag, "Hits");
        hitAt = Nbt.ia(tag, "HitAt");
        if (hitAt.length != hits.length) {
            hitAt = Arrays.copyOf(hitAt, hits.length);
        }
        resultUntil = Nbt.l(tag, "ResultUntil");
        resultScore = Nbt.i(tag, "ResultScore");
        resultPay = Nbt.i(tag, "ResultPay");
        bestScore = Nbt.i(tag, "BestScore");
        bestName = Nbt.s(tag, "BestName");
    }
}
