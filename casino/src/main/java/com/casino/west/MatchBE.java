package com.casino.west;

import com.casino.chip.Chips;
import com.casino.data.CasinoStats;
import com.casino.entity.DealerEntity;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Base de los retos del saloon (duelo al mediodía, vencidas): uno contra un NPC o uno contra otro jugador.
 * <ul>
 * <li>Contra NPC: eliges rival y apuesta; el NPC aparece en su lugar y empieza.</li>
 * <li>Contra jugador: el retador paga y espera (1 minuto) a que alguien acepte pagando lo mismo; el ganador se lleva las dos apuestas
 * menos el 5 % de la casa. Si nadie acepta, se le devuelve.</li>
 * </ul>
 * Los jugadores se quedan en su lugar (a cada uno se le abre una pantalla que atrapa el teclado); si alguno se va, pierde.
 */
public abstract class MatchBE extends CasinoBE {
    public static final int IDLE = 0, WAITING = 1, RUNNING = 2, RESULT = 3;
    public static final int NPC = 0, PVP = 1;
    /** Lado 0 = quien retó; lado 1 = el otro jugador o el NPC. Ganador 2 = empate o cancelado (se devuelve todo). */
    public static final int TIE = 2;
    public static final int R_NONE = 0, R_BEAT = 1, R_EARLY = 2, R_LEFT = 3, R_TIMEOUT = 4, R_GAVE_UP = 5;
    protected static final int SHOW_RESULT = 80;
    public static final int WAIT_TICKS = 60 * 20;

    protected int phase;
    protected int mode;
    protected int opp;
    protected int bet;
    protected final UUID[] who = new UUID[2];
    protected final String[] names = {"", ""};
    protected UUID npc;
    protected long start;
    protected long until;
    protected int winner = -1;
    protected int reason;
    protected int paid;

    protected MatchBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------------ lo que define cada juego

    /** Dónde se para cada lado (lado 1 también es el lugar del NPC). */
    public abstract BlockPos spot(int side);

    /** Hacia dónde mira cada lado (grados, como el yaw de Minecraft). */
    public abstract float yaw(int side);

    /** Variante (piel) del NPC para ese rival. */
    protected abstract int npcVariant(int opp);

    public abstract int opponents();

    protected abstract boolean validBet(int bet);

    /** Premio contra el NPC (fichas) si ganas. */
    protected abstract long npcPayout(int bet, int opp);

    protected abstract long pvpPayout(int bet);

    protected abstract int statGame();

    /** Prefijo de los textos ("casino.duel." o "casino.arm."). */
    protected abstract String lang();

    /** Qué tan lejos de su lugar se puede mover un jugador (bloques). */
    /** Lo que dice el rival del pueblo al aparecer (DealerEntity.SAY + n: "say_n" en los textos). */
    protected int greeting() {
        return DealerEntity.SAY;
    }

    protected double leash() {
        return 1.8;
    }

    /** Empezó (todos en su lugar y pagados). */
    protected abstract void onStart(long now);

    /** Cada tick mientras se juega. */
    protected abstract void onRunning(long now);

    /** Terminó: efectos (sonidos, poses del NPC...). */
    protected abstract void onFinish(int winner, int reason, ServerPlayer w, ServerPlayer l);

    protected abstract void writeMatch(CompoundTag tag);

    protected abstract void readMatch(CompoundTag tag);

    // ------------------------------------------------------------------ consultas

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(WestBlocks.FACING) ? s.getValue(WestBlocks.FACING) : Direction.EAST;
    }

    public int phase() {
        return phase;
    }

    public int mode() {
        return mode;
    }

    public int opponent() {
        return opp;
    }

    public int bet() {
        return bet;
    }

    public int winner() {
        return winner;
    }

    public int reason() {
        return reason;
    }

    public int paid() {
        return paid;
    }

    public String name(int side) {
        return names[side];
    }

    /** ¿En qué lado está este jugador? (-1 = no juega) */
    public int side(UUID id) {
        if (id == null || phase == IDLE) {
            return -1;
        }
        for (int i = 0; i < 2; i++) {
            if (id.equals(who[i])) {
                return i;
            }
        }
        return -1;
    }

    public boolean isPlayer(UUID id) {
        return side(id) >= 0;
    }

    /** Tiempo desde que empezó (ticks). */
    public double time(float pt) {
        return level == null ? 0 : level.getGameTime() + pt - start;
    }

    /** Ticks que le quedan al reto abierto. */
    public int waitLeft() {
        return level == null ? 0 : (int) Math.max(0, until - level.getGameTime());
    }

    public Component npcName() {
        return Component.translatable("casino.npc.duelist_" + npcVariant(opp));
    }

    /** Nombre de quien está del lado 1 (el NPC o el otro jugador). */
    public Component rivalName() {
        return mode == NPC ? npcName() : Component.literal(names[1]);
    }

    protected ServerPlayer player(int side) {
        if (level == null || level.getServer() == null || who[side] == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(who[side]);
    }

    protected DealerEntity npcEntity() {
        if (npc == null || !(level instanceof ServerLevel sl)) {
            return null;
        }
        Entity e = sl.getEntity(npc);
        return e instanceof DealerEntity d ? d : null;
    }

    // ------------------------------------------------------------------ retar, aceptar, rendirse

    private boolean pay(ServerPlayer p, int b) {
        if (!Chips.take(p, b)) {
            Plat.msg(p, Component.translatable("casino.plinko.no_chips", b).withStyle(ChatFormatting.RED), true);
            level.playSound(null, worldPosition, ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            return false;
        }
        return true;
    }

    public void challengeNpc(ServerPlayer p, int oppIndex, int b) {
        if (level == null || phase != IDLE || oppIndex < 0 || oppIndex >= opponents() || !validBet(b) || !pay(p, b)) {
            return;
        }
        clearStrays();
        mode = NPC;
        opp = oppIndex;
        bet = b;
        who[0] = p.getUUID();
        names[0] = p.getGameProfile().getName();
        who[1] = null;
        names[1] = "";
        DealerEntity d = Plat.create(ModEntities.DEALER.get(), level);
        if (d != null) {
            BlockPos m = spot(1);
            d.setGame(DealerEntity.GAME_DUELIST);
            d.setVariant(npcVariant(opp));
            d.moveTo(m.getX() + 0.5, m.getY(), m.getZ() + 0.5, yaw(1), 0.0F);
            d.setYHeadRot(yaw(1));
            d.setYBodyRot(yaw(1));
            level.addFreshEntity(d);
            npc = d.getUUID();
            d.say(greeting());
            ((ServerLevel) level).sendParticles(ParticleTypes.POOF, m.getX() + 0.5, m.getY() + 1, m.getZ() + 0.5, 12, 0.3, 0.6, 0.3, 0.02);
        }
        begin();
    }

    public void challengePvp(ServerPlayer p, int b) {
        if (level == null || phase != IDLE || !validBet(b) || !pay(p, b)) {
            return;
        }
        clearStrays();
        mode = PVP;
        bet = b;
        who[0] = p.getUUID();
        names[0] = p.getGameProfile().getName();
        who[1] = null;
        names[1] = "";
        phase = WAITING;
        until = level.getGameTime() + WAIT_TICKS;
        Component msg = Component.translatable(lang() + "challenge", names[0], b).withStyle(ChatFormatting.GOLD);
        for (Player q : level.players()) {
            if (q.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5) < 48 * 48) {
                Plat.msg(q, msg, false);
            }
        }
        sync();
    }

    public void accept(ServerPlayer q) {
        if (level == null || phase != WAITING || q.getUUID().equals(who[0])) {
            return;
        }
        ServerPlayer p = player(0);
        if (p == null || p.level() != level || p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5) > 64 * 64) {
            // quien retó ya no está por aquí: se le devuelve su apuesta y el reto se cierra
            if (p != null) {
                Chips.give(p, bet);
                Plat.msg(p, Component.translatable(lang() + "nobody", bet).withStyle(ChatFormatting.YELLOW), false);
            }
            Plat.msg(q, Component.translatable("casino.match.gone").withStyle(ChatFormatting.YELLOW), true);
            phase = IDLE;
            who[0] = null;
            sync();
            return;
        }
        if (!pay(q, bet)) {
            return;
        }
        who[1] = q.getUUID();
        names[1] = q.getGameProfile().getName();
        Plat.msg(p, Component.translatable(lang() + "accepted", names[1]).withStyle(ChatFormatting.GOLD), false);
        begin();
    }

    /** El retador cancela su reto (antes de que alguien acepte): se le devuelve. */
    public void cancel(ServerPlayer p) {
        if (phase != WAITING || !p.getUUID().equals(who[0])) {
            return;
        }
        Chips.give(p, bet);
        Plat.msg(p, Component.translatable(lang() + "cancelled", bet).withStyle(ChatFormatting.YELLOW), false);
        phase = IDLE;
        who[0] = null;
        sync();
    }

    /** Un jugador se rinde (cerró la pantalla con Esc). */
    public void giveUp(ServerPlayer p) {
        int s = side(p.getUUID());
        if (phase == RUNNING && s >= 0) {
            finish(1 - s, R_GAVE_UP);
        }
    }

    private void begin() {
        phase = RUNNING;
        winner = -1;
        reason = R_NONE;
        paid = 0;
        start = level.getGameTime();
        for (int s = 0; s < 2; s++) {
            ServerPlayer p = player(s);
            if (p != null) {
                BlockPos m = spot(s);
                if (p.isPassenger()) {
                    p.stopRiding();
                }
                p.teleportTo(m.getX() + 0.5, m.getY(), m.getZ() + 0.5);
            }
        }
        onStart(start);
        sync();
    }

    /** Quita NPCs que hayan quedado de un reto anterior (por ejemplo, si se cerró el mundo a medio reto). */
    protected void clearStrays() {
        if (level == null) {
            return;
        }
        for (DealerEntity d : level.getEntitiesOfClass(DealerEntity.class, new AABB(spot(1)).inflate(2.5))) {
            if (d.getGame() == DealerEntity.GAME_DUELIST && (npc == null || !npc.equals(d.getUUID()) || phase == IDLE)) {
                d.discard();
            }
        }
    }

    // ------------------------------------------------------------------ cada tick

    public void serverTick() {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        switch (phase) {
            case IDLE -> {
                if (now % 100 == 0) {
                    clearStrays();
                }
            }
            case WAITING -> {
                ServerPlayer w0 = player(0);
                if (now >= until || w0 == null || w0.level() != level) {
                    ServerPlayer p = player(0);
                    if (p != null) {
                        Chips.give(p, bet);
                        Plat.msg(p, Component.translatable(lang() + "nobody", bet).withStyle(ChatFormatting.YELLOW), false);
                    }
                    phase = IDLE;
                    sync();
                }
            }
            case RUNNING -> {
                for (int s = 0; s < 2; s++) {
                    if (mode == NPC && s == 1) {
                        continue;
                    }
                    ServerPlayer p = player(s);
                    BlockPos m = spot(s);
                    if (p == null || p.level() != level) {
                        finish(1 - s, R_LEFT);
                        return;
                    }
                    double dx = p.getX() - (m.getX() + 0.5), dz = p.getZ() - (m.getZ() + 0.5);
                    if (dx * dx + dz * dz > leash() * leash()) {
                        finish(1 - s, R_LEFT);
                        return;
                    }
                }
                onRunning(now);
            }
            case RESULT -> {
                if (now >= until) {
                    DealerEntity d = npcEntity();
                    if (d != null) {
                        ((ServerLevel) level).sendParticles(ParticleTypes.POOF, d.getX(), d.getY() + 1, d.getZ(), 14, 0.3, 0.6, 0.3, 0.02);
                        d.discard();
                    }
                    npc = null;
                    phase = IDLE;
                    who[0] = who[1] = null;
                    sync();
                }
            }
            default -> {
            }
        }
    }

    /** Termina el reto: ganador 0/1 (o TIE) y por qué. Paga, cuenta estadísticas y avisa. */
    protected void finish(int w, int why) {
        if (level == null || phase != RUNNING) {
            return;
        }
        phase = RESULT;
        winner = w;
        reason = why;
        until = level.getGameTime() + SHOW_RESULT;
        ServerPlayer p0 = player(0), p1 = mode == PVP ? player(1) : null;
        paid = 0;
        if (w == TIE) {
            if (p0 != null) {
                Chips.give(p0, bet);
            }
            if (p1 != null) {
                Chips.give(p1, bet);
            }
            for (ServerPlayer p : new ServerPlayer[]{p0, p1}) {
                if (p != null) {
                    Plat.msg(p, Component.translatable(lang() + "tie", bet).withStyle(ChatFormatting.YELLOW), false);
                }
            }
        } else if (mode == NPC) {
            if (w == 0) {
                paid = (int) npcPayout(bet, opp);
                if (p0 != null) {
                    Chips.give(p0, paid);
                }
            }
            if (p0 != null) {
                CasinoStats.play(p0, statGame(), bet, paid);
                message(p0, w == 0, w == 0 ? null : npcName());
            }
        } else {
            paid = (int) pvpPayout(bet);
            ServerPlayer win = w == 0 ? p0 : p1, lose = w == 0 ? p1 : p0;
            if (win != null) {
                Chips.give(win, paid);
                CasinoStats.play(win, statGame(), bet, paid);
                message(win, true, null);
            }
            if (lose != null) {
                CasinoStats.play(lose, statGame(), bet, 0);
                message(lose, false, Component.literal(names[w]));
            }
        }
        onFinish(w, why, w == 0 ? p0 : w == 1 ? p1 : null, w == 0 ? p1 : w == 1 ? p0 : null);
        sync();
    }

    private void message(ServerPlayer p, boolean won, Component other) {
        String why = switch (reason) {
            case R_EARLY -> "early";
            case R_LEFT -> "left";
            case R_GAVE_UP -> "gave_up";
            default -> "beat";
        };
        Component head = won ? Component.translatable(lang() + "won", paid).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                : Component.translatable(lang() + "lost_" + why, other == null ? Component.literal("") : other, bet).withStyle(ChatFormatting.RED);
        Plat.msg(p, head, false);
        Plat.msg(p, head, true);
    }

    protected void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ------------------------------------------------------------------ guardar

    @Override
    protected final void writeData(CompoundTag tag) {
        tag.putInt("Phase", phase);
        tag.putInt("Mode", mode);
        tag.putInt("Opp", opp);
        tag.putInt("Bet", bet);
        for (int s = 0; s < 2; s++) {
            if (who[s] != null) {
                Nbt.putUuid(tag, "Who" + s, who[s]);
            }
            tag.putString("Name" + s, names[s]);
        }
        if (npc != null) {
            Nbt.putUuid(tag, "Npc", npc);
        }
        tag.putLong("Start", start);
        tag.putLong("Until", until);
        tag.putInt("Winner", winner);
        tag.putInt("Reason", reason);
        tag.putInt("Paid", paid);
        writeMatch(tag);
    }

    @Override
    protected final void readData(CompoundTag tag) {
        phase = Nbt.i(tag, "Phase");
        mode = Nbt.i(tag, "Mode");
        opp = Math.max(0, Math.min(opponents() - 1, Nbt.i(tag, "Opp")));
        bet = Nbt.i(tag, "Bet");
        for (int s = 0; s < 2; s++) {
            who[s] = Nbt.hasUuid(tag, "Who" + s) ? Nbt.uuid(tag, "Who" + s) : null;
            names[s] = Nbt.s(tag, "Name" + s);
        }
        npc = Nbt.hasUuid(tag, "Npc") ? Nbt.uuid(tag, "Npc") : null;
        start = Nbt.l(tag, "Start");
        until = Nbt.l(tag, "Until");
        winner = Nbt.has(tag, "Winner") ? Nbt.i(tag, "Winner") : -1;
        reason = Nbt.i(tag, "Reason");
        paid = Nbt.i(tag, "Paid");
        readMatch(tag);
    }
}
