package com.casino.west;

import com.casino.data.CasinoStats;
import com.casino.entity.DealerEntity;
import com.casino.game.DuelRules;
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
import net.minecraft.world.phys.Vec3;

/**
 * La campana del duelo al mediodía. Lado 0 (quien retó) en la X junto a la campana; lado 1 (pistolero o el otro jugador) en la X a
 * 16 bloques sobre la calle. Tres campanadas, un silencio al azar y "¡DESENFUNDA!" con una tecla al azar: gana quien la apriete
 * primero (cada cliente mide su propio tiempo desde que ve la tecla, así la red no le quita a nadie). FACING = hacia el lado 1.
 */
public class DuelBE extends MatchBE {
    private int drawTick;
    /** La tecla del grito (0 hasta que se grita: así nadie la sabe antes). */
    private char key;
    /** Tiempo de cada lado en ms (-1 = no ha disparado). */
    private final int[] ms = {-1, -1};
    private long firstAt = -1;
    private int npcMs;
    private boolean noon;
    private int bestMs;
    private String bestName = "";

    public DuelBE(BlockPos pos, BlockState state) {
        super(WestReg.DUEL_BE.get(), pos, state);
    }

    // ------------------------------------------------------------------ lo propio del duelo

    @Override
    public BlockPos spot(int side) {
        BlockPos a = worldPosition.relative(facing().getCounterClockWise(), 2);
        return side == 0 ? a : a.relative(facing(), DuelRules.DISTANCE);
    }

    @Override
    public float yaw(int side) {
        return side == 0 ? facing().toYRot() : facing().getOpposite().toYRot();
    }

    @Override
    protected int npcVariant(int o) {
        return DealerEntity.WEST + DuelRules.SKIN[o];
    }

    @Override
    public int opponents() {
        return DuelRules.OPPONENTS;
    }

    @Override
    protected boolean validBet(int b) {
        return DuelRules.validBet(b);
    }

    @Override
    protected long npcPayout(int b, int o) {
        return DuelRules.payout(b, o, noon);
    }

    @Override
    protected long pvpPayout(int b) {
        return DuelRules.pvpPayout(b);
    }

    @Override
    protected int statGame() {
        return CasinoStats.DUEL;
    }

    @Override
    protected String lang() {
        return "casino.duel.";
    }

    public char key() {
        return key;
    }

    public int drawTick() {
        return drawTick;
    }

    public int ms(int side) {
        return ms[side];
    }

    public boolean noon() {
        return noon;
    }

    public int bestMs() {
        return bestMs;
    }

    public String bestName() {
        return bestName;
    }

    /** Tick del mundo en que se grita. */
    public long drawAt() {
        return start + drawTick;
    }

    // ------------------------------------------------------------------ jugar

    @Override
    protected void onStart(long now) {
        drawTick = DuelRules.drawTick(level.getRandom().nextDouble());
        key = 0;
        ms[0] = ms[1] = -1;
        firstAt = -1;
        npcMs = DuelRules.npcMs(opp, level.getRandom().nextGaussian());
        noon = mode == NPC && DuelRules.noon(Plat.dayTime(level));
        level.playSound(null, worldPosition, ModSounds.DUEL_THEME.get(), SoundSource.RECORDS, 1.2F, 1.0F);
        ServerPlayer p = player(0);
        if (p != null && noon) {
            Plat.msg(p, Component.translatable("casino.duel.noon").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), false);
        }
    }

    @Override
    protected void onRunning(long now) {
        long t = now - start;
        if (t == DuelRules.TOLL_1 || t == DuelRules.TOLL_2 || t == DuelRules.TOLL_3) {
            level.playSound(null, worldPosition, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, t == DuelRules.TOLL_3 ? 0.7F : 0.8F);
        }
        DealerEntity d = npcEntity();
        if (t == drawTick) {
            key = DuelRules.key(level.getRandom().nextDouble());
            level.playSound(null, worldPosition, ModSounds.DUEL_DRAW.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
            level.playSound(null, spot(1), ModSounds.DUEL_DRAW.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
            if (d != null) {
                d.setCombat(1);
            }
            sync();
        }
        if (key == 0) {
            return;
        }
        long elapsed = (t - drawTick) * 50;
        // el pistolero dispara cuando le toca
        if (mode == NPC && ms[1] < 0 && elapsed >= npcMs) {
            ms[1] = npcMs;
            if (firstAt < 0) {
                firstAt = t;
            }
            if (d != null) {
                d.setCombat(2);
            }
        }
        if (firstAt >= 0 && ((ms[0] >= 0 && ms[1] >= 0) || t >= firstAt + DuelRules.GRACE)) {
            int w = ms[1] < 0 || (ms[0] >= 0 && ms[0] < ms[1]) ? 0 : 1;
            finish(w, R_BEAT);
            return;
        }
        if (t > drawTick + DuelRules.TIMEOUT) {
            finish(TIE, R_TIMEOUT);
        }
    }

    /** Un jugador apretó una tecla (c = letra o número en mayúscula; clientMs = su tiempo medido desde que vio la tecla). */
    public void press(ServerPlayer p, char c, int clientMs) {
        int s = side(p.getUUID());
        if (phase != RUNNING || s < 0 || (mode == NPC && s == 1) || ms[s] >= 0) {
            return;
        }
        long t = level.getGameTime() - start;
        if (key == 0) {
            finish(1 - s, R_EARLY);
            return;
        }
        if (c != key) {
            return;   // tecla equivocada: el revólver se traba un momento (lo controla su pantalla)
        }
        long elapsed = Math.max(0, (t - drawTick) * 50);
        int m = clientMs;
        // el tiempo del cliente no puede ser mayor que lo que pasó en el servidor (más un margen) ni mucho menor (más de 1.5 s de red)
        if (!DuelRules.validMs(m) || m > elapsed + 150 || m < elapsed - 1500) {
            m = (int) Math.max(80, elapsed);
        }
        ms[s] = m;
        if (firstAt < 0) {
            firstAt = t;
        }
        level.playSound(null, p.getX(), p.getY() + 1, p.getZ(), ModSounds.GALLERY_SHOT.get(), SoundSource.PLAYERS, 1.2F, 0.9F);
        Vec3 look = Vec3.directionFromRotation(0, yaw(s));
        ((ServerLevel) level).sendParticles(ParticleTypes.SMOKE, p.getX() + look.x * 0.6, p.getY() + 1.3, p.getZ() + look.z * 0.6, 5, 0.03, 0.03, 0.03, 0.01);
        sync();
    }

    @Override
    protected void onFinish(int w, int why, ServerPlayer winP, ServerPlayer loseP) {
        DealerEntity d = npcEntity();
        if (w == TIE) {
            if (d != null) {
                d.setCombat(0);
            }
            return;
        }
        if (mode == NPC && w == 1) {
            // ganó el pistolero: dispara y el jugador recibe el golpe (sin daño)
            if (d != null) {
                d.setCombat(2);
                d.say(DealerEntity.SAY + (why == R_EARLY ? 3 : why == R_LEFT || why == R_GAVE_UP ? 4 : 1));
                Plat.swing(d);
                level.playSound(null, d.getX(), d.getY() + 1.4, d.getZ(), ModSounds.GALLERY_SHOT.get(), SoundSource.HOSTILE, 1.2F, 0.8F);
                Vec3 muzzle = d.position().add(Vec3.directionFromRotation(0, d.getYRot()).scale(0.7)).add(0, 1.35, 0);
                ((ServerLevel) level).sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 6, 0.04, 0.04, 0.04, 0.01);
            }
        } else if (mode == NPC) {
            if (d != null) {
                d.setCombat(3);
                d.say(DealerEntity.SAY + 2);
                ((ServerLevel) level).sendParticles(ParticleTypes.CRIT, d.getX(), d.getY() + 1.3, d.getZ(), 14, 0.2, 0.3, 0.2, 0.15);
            }
        }
        if (why == R_BEAT) {
            level.playSound(null, worldPosition, ModSounds.GALLERY_DING.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        if (loseP != null && why == R_BEAT) {
            Vec3 back = Vec3.directionFromRotation(0, yaw(1 - w)).scale(-0.5).add(0, 0.25, 0);
            loseP.setDeltaMovement(back);
            loseP.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(loseP));
            level.playSound(null, loseP.getX(), loseP.getY() + 1, loseP.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        // récord del pueblo: el disparo ganador más rápido de un jugador
        if (winP != null && why == R_BEAT && ms[w] > 0 && (bestMs <= 0 || ms[w] < bestMs)) {
            bestMs = ms[w];
            bestName = winP.getGameProfile().getName();
            Plat.msg(winP, Component.translatable("casino.duel.record", String.format(java.util.Locale.ROOT, "%.3f", ms[w] / 1000.0))
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        }
        if (winP != null && why == R_BEAT && ms[w] > 0) {
            Plat.msg(winP, Component.translatable("casino.duel.your_time", String.format(java.util.Locale.ROOT, "%.3f", ms[w] / 1000.0))
                    .withStyle(ChatFormatting.GOLD), false);
        }
    }

    public void clientTick() {
        if (phase != IDLE) {
            com.casino.plat.Net.onClient(() -> com.casino.client.WestClient.duelSeen(this));
        }
    }

    @Override
    protected AABB renderBox() {
        BlockPos a = spot(0), b = spot(1), c = worldPosition;
        return new AABB(Math.min(Math.min(a.getX(), b.getX()), c.getX()) - 1, c.getY() - 1, Math.min(Math.min(a.getZ(), b.getZ()), c.getZ()) - 1,
                Math.max(Math.max(a.getX(), b.getX()), c.getX()) + 2, c.getY() + 3, Math.max(Math.max(a.getZ(), b.getZ()), c.getZ()) + 2);
    }

    @Override
    protected void writeMatch(CompoundTag tag) {
        tag.putInt("DrawTick", drawTick);
        tag.putInt("Key", key);
        tag.putIntArray("Ms", ms);
        tag.putLong("FirstAt", firstAt);
        tag.putInt("NpcMs", npcMs);
        tag.putBoolean("Noon", noon);
        tag.putInt("BestMs", bestMs);
        tag.putString("BestName", bestName);
    }

    @Override
    protected void readMatch(CompoundTag tag) {
        drawTick = Nbt.i(tag, "DrawTick");
        key = (char) Nbt.i(tag, "Key");
        int[] m = Nbt.ia(tag, "Ms");
        ms[0] = m.length > 0 ? m[0] : -1;
        ms[1] = m.length > 1 ? m[1] : -1;
        firstAt = Nbt.has(tag, "FirstAt") ? Nbt.l(tag, "FirstAt") : -1;
        npcMs = Nbt.i(tag, "NpcMs");
        noon = Nbt.b(tag, "Noon");
        bestMs = Nbt.i(tag, "BestMs");
        bestName = Nbt.s(tag, "BestName");
    }

    /** A los clientes no se les manda cuándo dispara el pistolero. */
    @Override
    protected CompoundTag syncTag(CompoundTag full) {
        full.putInt("NpcMs", 0);
        return full;
    }
}
