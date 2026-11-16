package com.casino.west;

import com.casino.chip.Chips;
import com.casino.data.CasinoStats;
import com.casino.entity.DealerEntity;
import com.casino.game.DuelRules;
import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import com.casino.plat.Plat;
import com.casino.registry.ModEntities;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * La campana del duelo al mediodía: quién está retando, contra cuál pistolero, la semilla (cuándo se grita "¡desenfunda!" y cuándo
 * dispara el pistolero) y el resultado. El jugador se para en su X y el pistolero aparece en la otra, a 16 bloques sobre la calle.
 * FACING de la campana = hacia donde está el pistolero; las X quedan 2 bloques hacia la calle.
 */
public class DuelBE extends CasinoBE implements GunGame {
    public static final int IDLE = 0, RUNNING = 1, RESULT = 2;
    public static final int R_NONE = 0, R_WIN = 1, R_LOSE = 2, R_EARLY = 3, R_LOOKED = 4, R_LEFT = 5;
    private static final int SHOW_RESULT = 70;

    private int phase;
    private UUID player;
    private String playerName = "";
    private int opp;
    private int bet;
    private long seed;
    private long start;
    private boolean noon;
    private UUID npc;
    private int shots;
    private int npcFired;
    private int result;
    private int reaction = -1;
    private long resultUntil;
    private int pay;
    private int bestTicks;
    private String bestName = "";

    public DuelBE(BlockPos pos, BlockState state) {
        super(WestReg.DUEL_BE.get(), pos, state);
    }

    // ------------------------------------------------------------------ consultas

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(WestBlocks.FACING) ? s.getValue(WestBlocks.FACING) : Direction.EAST;
    }

    /** La X del jugador (2 bloques hacia la calle) y la del pistolero (16 bloques más allá). */
    public BlockPos playerMark() {
        return worldPosition.relative(facing().getCounterClockWise(), 2);
    }

    public BlockPos npcMark() {
        return playerMark().relative(facing(), DuelRules.DISTANCE);
    }

    public int phase() {
        return phase;
    }

    @Override
    public boolean isShooter(UUID id) {
        return phase != IDLE && player != null && player.equals(id);
    }

    public int opponent() {
        return opp;
    }

    public int result() {
        return result;
    }

    public int reaction() {
        return reaction;
    }

    public int pay() {
        return pay;
    }

    public int bet() {
        return bet;
    }

    public boolean noon() {
        return noon;
    }

    public int shots() {
        return shots;
    }

    public int bestTicks() {
        return bestTicks;
    }

    public String bestName() {
        return bestName;
    }

    /** Ticks desde que empezó. */
    public double time(float pt) {
        return level == null ? 0 : level.getGameTime() + pt - start;
    }

    /** Tick del mundo en que se grita "¡desenfunda!". */
    public long drawAt() {
        return start + DuelRules.drawTick(seed);
    }

    public boolean drawn(float pt) {
        return phase == RUNNING && level != null && level.getGameTime() + pt >= drawAt();
    }

    // ------------------------------------------------------------------ jugar

    public void begin(ServerPlayer p, int oppIndex, int betChips) {
        if (level == null || phase != IDLE || oppIndex < 0 || oppIndex >= DuelRules.OPPONENTS || !DuelRules.validBet(betChips)) {
            return;
        }
        if (!Chips.take(p, betChips)) {
            Plat.msg(p, Component.translatable("casino.plinko.no_chips", betChips).withStyle(ChatFormatting.RED), true);
            level.playSound(null, worldPosition, ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            return;
        }
        clearStrays();
        phase = RUNNING;
        player = p.getUUID();
        playerName = p.getGameProfile().getName();
        opp = oppIndex;
        bet = betChips;
        seed = level.getRandom().nextLong();
        start = level.getGameTime();
        noon = DuelRules.noon(Plat.dayTime(level));
        shots = DuelRules.SHOTS;
        npcFired = 0;
        result = R_NONE;
        reaction = -1;
        pay = 0;
        // el pistolero llega a su X, mirando al jugador
        DealerEntity d = Plat.create(ModEntities.DEALER.get(), level);
        if (d != null) {
            BlockPos m = npcMark();
            float yaw = facing().getOpposite().toYRot();
            d.setGame(DealerEntity.GAME_DUELIST);
            d.setVariant(DealerEntity.WEST + DuelRules.SKIN[opp]);
            d.moveTo(m.getX() + 0.5, m.getY(), m.getZ() + 0.5, yaw, 0.0F);
            d.setYHeadRot(yaw);
            d.setYBodyRot(yaw);
            level.addFreshEntity(d);
            npc = d.getUUID();
            d.say(DealerEntity.SAY);
            ((ServerLevel) level).sendParticles(ParticleTypes.POOF, m.getX() + 0.5, m.getY() + 1, m.getZ() + 0.5, 12, 0.3, 0.6, 0.3, 0.02);
        }
        BlockPos mk = playerMark();
        p.teleportTo(mk.getX() + 0.5, mk.getY(), mk.getZ() + 0.5);
        GunGame.lend(p, worldPosition);
        Plat.msg(p, Component.translatable("casino.duel.start", Component.translatable("casino.npc.duelist_" + (DealerEntity.WEST + DuelRules.SKIN[opp])))
                .withStyle(ChatFormatting.GOLD), false);
        if (noon) {
            Plat.msg(p, Component.translatable("casino.duel.noon").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), false);
        }
        level.playSound(null, worldPosition, ModSounds.DUEL_THEME.get(), SoundSource.RECORDS, 1.2F, 1.0F);
        sync();
    }

    private DealerEntity duelist() {
        if (npc == null || !(level instanceof ServerLevel sl)) {
            return null;
        }
        Entity e = sl.getEntity(npc);
        return e instanceof DealerEntity d ? d : null;
    }

    /** Quita pistoleros que hayan quedado de un duelo anterior (por ejemplo, si se cerró el mundo a medio duelo). */
    private void clearStrays() {
        if (level == null) {
            return;
        }
        for (DealerEntity d : level.getEntitiesOfClass(DealerEntity.class, new AABB(npcMark()).inflate(3))) {
            if (d.getGame() == DealerEntity.GAME_DUELIST && (npc == null || !npc.equals(d.getUUID()) || phase == IDLE)) {
                d.discard();
            }
        }
    }

    @Override
    public void shoot(ServerPlayer p) {
        if (level == null || phase != RUNNING || !isShooter(p.getUUID()) || shots <= 0) {
            return;
        }
        long t = level.getGameTime() - start;
        level.playSound(null, p.getX(), p.getY() + 1, p.getZ(), ModSounds.GALLERY_SHOT.get(), SoundSource.PLAYERS, 1.0F, 0.9F);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1.0F);
        ((ServerLevel) level).sendParticles(ParticleTypes.SMOKE, eye.x + look.x * 0.9, eye.y - 0.25 + look.y * 0.9, eye.z + look.z * 0.9, 4, 0.03, 0.03, 0.03, 0.01);
        int draw = DuelRules.drawTick(seed);
        if (t < draw) {
            finish(p, R_EARLY);
            return;
        }
        shots--;
        DealerEntity d = duelist();
        if (d != null && d.getBoundingBox().inflate(0.12).clip(eye, eye.add(look.scale(DuelRules.DISTANCE + 10))).isPresent()) {
            reaction = (int) (t - draw);
            finish(p, R_WIN);
        } else {
            sync();
        }
    }

    public void serverTick() {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (phase == IDLE) {
            if (now % 100 == 0) {
                clearStrays();
            }
            return;
        }
        if (phase == RESULT) {
            if (now >= resultUntil) {
                DealerEntity d = duelist();
                if (d != null) {
                    ((ServerLevel) level).sendParticles(ParticleTypes.POOF, d.getX(), d.getY() + 1, d.getZ(), 14, 0.3, 0.6, 0.3, 0.02);
                    d.discard();
                }
                npc = null;
                phase = IDLE;
                player = null;
                sync();
            }
            return;
        }
        long t = now - start;
        ServerPlayer p = level.getServer() == null || player == null ? null : level.getServer().getPlayerList().getPlayer(player);
        BlockPos mk = playerMark();
        if (p == null || p.level() != level) {
            finish(null, R_LEFT);
            return;
        }
        double dx = p.getX() - (mk.getX() + 0.5), dz = p.getZ() - (mk.getZ() + 0.5);
        if (dx * dx + dz * dz > 1.8 * 1.8) {
            finish(p, R_LEFT);
            return;
        }
        if (t == DuelRules.TOLL_1 || t == DuelRules.TOLL_2 || t == DuelRules.TOLL_3) {
            level.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, t == DuelRules.TOLL_3 ? 0.7F : 0.8F);
        }
        int draw = DuelRules.drawTick(seed);
        DealerEntity d = duelist();
        if (t == draw) {
            if (p.getXRot() < DuelRules.HOLSTER_PITCH) {
                finish(p, R_LOOKED);
                return;
            }
            level.playSound(null, worldPosition, ModSounds.DUEL_DRAW.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
            if (d != null) {
                d.setCombat(1);
            }
            sync();
        }
        if (d != null) {
            int next = DuelRules.npcShot(seed, opp, npcFired);
            if (t == next - 3) {
                d.setCombat(2);
            }
            if (t >= next) {
                boolean hits = DuelRules.npcHits(seed, opp, npcFired);
                npcFired++;
                level.playSound(null, d.getX(), d.getY() + 1.4, d.getZ(), ModSounds.GALLERY_SHOT.get(), SoundSource.HOSTILE, 1.2F, 0.8F);
                Vec3 muzzle = d.position().add(Vec3.directionFromRotation(0, d.getYRot()).scale(0.7)).add(0, 1.35, 0);
                ((ServerLevel) level).sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 6, 0.04, 0.04, 0.04, 0.01);
                Plat.swing(d);
                if (hits) {
                    finish(p, R_LOSE);
                    return;
                }
                // falló: la bala levanta polvo junto al jugador
                ((ServerLevel) level).sendParticles(ParticleTypes.CLOUD, p.getX() + (level.getRandom().nextDouble() - 0.5) * 1.5, p.getY() + 0.1,
                        p.getZ() + (level.getRandom().nextDouble() - 0.5) * 1.5, 5, 0.1, 0.05, 0.1, 0.01);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARROW_HIT, SoundSource.HOSTILE, 0.8F, 1.6F);
            }
        }
        // sin balas y sin acertar: el pistolero termina el duelo con su siguiente disparo (que ya no falla después del aviso)
    }

    private void finish(ServerPlayer p, int how) {
        if (level == null || phase != RUNNING) {
            return;
        }
        phase = RESULT;
        result = how;
        resultUntil = level.getGameTime() + SHOW_RESULT;
        DealerEntity d = duelist();
        String npcName = "casino.npc.duelist_" + (DealerEntity.WEST + DuelRules.SKIN[opp]);
        if (how == R_WIN) {
            pay = (int) DuelRules.payout(bet, opp, noon);
            if (d != null) {
                d.setCombat(3);
                d.say(DealerEntity.SAY + 2);
                ((ServerLevel) level).sendParticles(ParticleTypes.CRIT, d.getX(), d.getY() + 1.3, d.getZ(), 14, 0.2, 0.3, 0.2, 0.15);
            }
            level.playSound(null, worldPosition, ModSounds.GALLERY_DING.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
            level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.0F);
        } else {
            pay = 0;
            if (d != null) {
                d.setCombat(how == R_LOSE ? 2 : 0);
                d.say(DealerEntity.SAY + (how == R_LOSE ? 1 : how == R_LEFT ? 4 : 3));
            }
            if (p != null && how == R_LOSE) {
                // el golpe de la bala (sin daño): te echa un poquito para atrás
                Vec3 back = new Vec3(-facing().getStepX() * 0.5, 0.25, -facing().getStepZ() * 0.5);
                p.setDeltaMovement(back);
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
                level.playSound(null, p.getX(), p.getY() + 1, p.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8F, 1.0F);
            }
        }
        if (p != null) {
            if (pay > 0) {
                Chips.give(p, pay);
            }
            CasinoStats.play(p, CasinoStats.DUEL, bet, pay);
            GunGame.takeBack(p);
            String key = switch (how) {
                case R_WIN -> "casino.duel.win";
                case R_LOSE -> "casino.duel.lose";
                case R_EARLY -> "casino.duel.early";
                case R_LOOKED -> "casino.duel.looked";
                default -> "casino.duel.left";
            };
            Component head = how == R_WIN ? Component.translatable(key, String.format(java.util.Locale.ROOT, "%.2f", reaction / 20.0))
                    : Component.translatable(key, Component.translatable(npcName));
            Component tail = pay > 0 ? Component.translatable("casino.duel.won", pay).withStyle(ChatFormatting.GREEN)
                    : Component.translatable("casino.duel.lost", bet).withStyle(ChatFormatting.RED);
            Plat.msg(p, head.copy().withStyle(how == R_WIN ? ChatFormatting.GOLD : ChatFormatting.YELLOW).append(" ").append(tail), false);
            if (how == R_WIN && (bestTicks <= 0 || reaction < bestTicks)) {
                bestTicks = reaction;
                bestName = playerName;
                Plat.msg(p, Component.translatable("casino.duel.record", String.format(java.util.Locale.ROOT, "%.2f", reaction / 20.0))
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
            }
        }
        sync();
    }

    public void clientTick() {
        if (phase != IDLE && player != null) {
            com.casino.plat.Net.onClient(() -> com.casino.client.WestClient.duelSeen(this));
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
        BlockPos a = playerMark(), b = npcMark(), c = worldPosition;
        return new AABB(Math.min(Math.min(a.getX(), b.getX()), c.getX()) - 1, c.getY() - 1, Math.min(Math.min(a.getZ(), b.getZ()), c.getZ()) - 1,
                Math.max(Math.max(a.getX(), b.getX()), c.getX()) + 2, c.getY() + 3, Math.max(Math.max(a.getZ(), b.getZ()), c.getZ()) + 2);
    }

    @Override
    protected void writeData(CompoundTag tag) {
        tag.putInt("Phase", phase);
        if (player != null) {
            Nbt.putUuid(tag, "Player", player);
        }
        tag.putString("PlayerName", playerName);
        tag.putInt("Opp", opp);
        tag.putInt("Bet", bet);
        tag.putLong("Seed", seed);
        tag.putLong("Start", start);
        tag.putBoolean("Noon", noon);
        if (npc != null) {
            Nbt.putUuid(tag, "Npc", npc);
        }
        tag.putInt("Shots", shots);
        tag.putInt("NpcFired", npcFired);
        tag.putInt("Result", result);
        tag.putInt("Reaction", reaction);
        tag.putLong("ResultUntil", resultUntil);
        tag.putInt("Pay", pay);
        tag.putInt("BestTicks", bestTicks);
        tag.putString("BestName", bestName);
    }

    @Override
    protected void readData(CompoundTag tag) {
        phase = Nbt.i(tag, "Phase");
        player = Nbt.hasUuid(tag, "Player") ? Nbt.uuid(tag, "Player") : null;
        playerName = Nbt.s(tag, "PlayerName");
        opp = Math.max(0, Math.min(DuelRules.OPPONENTS - 1, Nbt.i(tag, "Opp")));
        bet = Nbt.i(tag, "Bet");
        seed = Nbt.l(tag, "Seed");
        start = Nbt.l(tag, "Start");
        noon = Nbt.b(tag, "Noon");
        npc = Nbt.hasUuid(tag, "Npc") ? Nbt.uuid(tag, "Npc") : null;
        shots = Nbt.i(tag, "Shots");
        npcFired = Nbt.i(tag, "NpcFired");
        result = Nbt.i(tag, "Result");
        reaction = Nbt.has(tag, "Reaction") ? Nbt.i(tag, "Reaction") : -1;
        resultUntil = Nbt.l(tag, "ResultUntil");
        pay = Nbt.i(tag, "Pay");
        bestTicks = Nbt.i(tag, "BestTicks");
        bestName = Nbt.s(tag, "BestName");
    }
}
