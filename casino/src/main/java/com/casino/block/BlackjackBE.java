package com.casino.block;

import com.casino.chip.Chips;
import com.casino.data.CasinoData;
import com.casino.entity.DealerEntity;
import com.casino.game.BlackjackGame;
import com.casino.game.BlackjackGame.Phase;
import com.casino.game.BlackjackGame.Result;
import com.casino.game.Prize;
import com.casino.net.BlackjackPacket;
import com.casino.net.CasinoNet;
import com.casino.net.WheelWinPacket;
import com.casino.registry.ModEntities;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * El corazón de la mesa de blackjack (bloque del centro). La partida vive aquí y se guarda con el mundo, así que si se cierra el
 * servidor a mitad de una mano no se pierde nada. El servidor decide todo; el cliente solo pide jugadas y anima lo que ve.
 */
public class BlackjackBE extends BlockEntity {
    /** Si el jugador no hace nada en este tiempo (3 minutos), el crupier lo planta solo. */
    private static final int IDLE_TICKS = 3600;

    public final BlackjackGame game = new BlackjackGame();
    @Nullable
    public UUID player;
    public String playerName = "";
    public int dealSeq = 0;
    private long nextStep = 0;
    private long lastAction = 0;
    private long doneAt = 0;
    private final Random rng = new Random();

    // ---- solo cliente: animación de las cartas
    public static final int MAX_CARDS = 14;
    public final long[] pBorn = new long[MAX_CARDS];
    public final long[] dBorn = new long[MAX_CARDS];
    public long revealAt = -1000;
    public long doneSeenAt = -1000;
    private boolean clientInit = false;
    private int seenP;
    private int seenD;
    private boolean seenHole;
    private int seenSeq;
    private Phase seenPhase = Phase.IDLE;

    public BlackjackBE(BlockPos pos, BlockState state) {
        super(com.casino.registry.ModBlockEntities.BLACKJACK.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(BlackjackBlock.FACING);
    }

    // ------------------------------------------------------------------ crupier

    private AABB spotBox() {
        return new AABB(BlackjackBlock.dealerSpot(worldPosition, facing())).inflate(1.2, 1.0, 1.2);
    }

    private List<DealerEntity> dealers() {
        return level == null ? List.of() : level.getEntitiesOfClass(DealerEntity.class, spotBox(), d -> worldPosition.equals(d.getTable()));
    }

    public void spawnDealer(ServerLevel sl) {
        if (!dealers().isEmpty()) {
            return;
        }
        Direction f = facing();
        BlockPos spot = BlackjackBlock.dealerSpot(worldPosition, f);
        DealerEntity d = ModEntities.DEALER.get().create(sl);
        if (d == null) {
            return;
        }
        d.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, f.toYRot(), 0.0F);
        d.setYHeadRot(f.toYRot());
        d.setYBodyRot(f.toYRot());
        d.setTable(worldPosition);
        sl.addFreshEntity(d);
    }

    private void dealerSwing() {
        for (DealerEntity d : dealers()) {
            d.swing(InteractionHand.MAIN_HAND);
        }
    }

    private Vec3 center() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5);
    }

    private void sound(SoundEvent s, float vol, float pitch) {
        if (level != null) {
            level.playSound(null, worldPosition, s, SoundSource.BLOCKS, vol, pitch);
        }
    }

    // ------------------------------------------------------------------ jugadas (servidor)

    private void msg(ServerPlayer p, String key, ChatFormatting color, Object... args) {
        p.displayClientMessage(Component.translatable(key, args).withStyle(color), true);
    }

    public void act(ServerPlayer p, int action, int betChips) {
        if (level == null || level.isClientSide) {
            return;
        }
        long now = level.getGameTime();
        boolean mine = p.getUUID().equals(player);
        boolean inHand = game.phase == Phase.PLAYER || game.phase == Phase.DEALER;
        switch (action) {
            case BlackjackPacket.DEAL -> {
                if (inHand || (game.phase == Phase.DONE && !mine && now < doneAt + 40)) {
                    if (!mine) {
                        msg(p, "casino.blackjack.busy", ChatFormatting.YELLOW, playerName);
                    }
                    return;
                }
                if (!BlackjackGame.validBet(betChips)) {
                    return;
                }
                if (!Chips.take(p, betChips)) {
                    msg(p, "casino.plinko.no_chips", ChatFormatting.RED, betChips);
                    sound(ModSounds.DENIED.get(), 0.6F, 1.0F);
                    return;
                }
                player = p.getUUID();
                playerName = p.getGameProfile().getName();
                game.start(rng, betChips);
                dealSeq++;
                nextStep = 0;
                lastAction = now;
                sound(ModSounds.CHIP.get(), 0.8F, 1.0F);
                sound(ModSounds.CARD_DEAL.get(), 0.8F, 1.0F);
                dealerSwing();
                afterAction(now);
            }
            case BlackjackPacket.HIT -> {
                if (!mine || game.phase != Phase.PLAYER) {
                    return;
                }
                game.hit(rng);
                lastAction = now;
                sound(ModSounds.CARD_DEAL.get(), 0.8F, 1.1F);
                dealerSwing();
                afterAction(now);
            }
            case BlackjackPacket.STAND -> {
                if (!mine || game.phase != Phase.PLAYER) {
                    return;
                }
                game.stand();
                lastAction = now;
                sound(ModSounds.CHIP.get(), 0.4F, 1.4F);
                afterAction(now);
            }
            case BlackjackPacket.DOUBLE -> {
                if (!mine || !game.canDouble()) {
                    return;
                }
                if (!Chips.take(p, game.bet)) {
                    msg(p, "casino.plinko.no_chips", ChatFormatting.RED, game.bet);
                    sound(ModSounds.DENIED.get(), 0.6F, 1.0F);
                    return;
                }
                game.doubleDown(rng);
                lastAction = now;
                sound(ModSounds.CHIP.get(), 0.8F, 0.9F);
                sound(ModSounds.CARD_DEAL.get(), 0.8F, 1.0F);
                dealerSwing();
                afterAction(now);
            }
            default -> {
            }
        }
    }

    /** Después de cada jugada: si ahora le toca al crupier se programa su turno; si la mano terminó se paga. */
    private void afterAction(long now) {
        if (game.phase == Phase.DEALER && nextStep == 0) {
            nextStep = now + 12;
        }
        if (game.phase == Phase.DONE) {
            settle(now);
        }
        sync();
    }

    public void serverTick() {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (now % 100 == 7 && level instanceof ServerLevel sl) {
            spawnDealer(sl); // si al crupier le pasó algo, vuelve a aparecer
        }
        if (game.phase == Phase.PLAYER) {
            ServerPlayer sp = onlinePlayer();
            Vec3 c = center();
            if (sp == null || sp.distanceToSqr(c.x, c.y, c.z) > 24 * 24 || now - lastAction > IDLE_TICKS) {
                game.stand(); // el jugador se fue: el crupier cierra la mano y le guarda lo que gane
                afterAction(now);
            }
        }
        if (game.phase == Phase.DEALER && nextStep != 0 && now >= nextStep) {
            int before = game.dealer.size();
            boolean wasHole = game.holeRevealed;
            game.dealerStep(rng);
            nextStep = now + 16;
            if (game.dealer.size() > before) {
                sound(ModSounds.CARD_DEAL.get(), 0.8F, 0.95F);
                dealerSwing();
            } else if (!wasHole) {
                sound(ModSounds.CARD_FLIP.get(), 0.9F, 1.0F);
            }
            if (game.phase == Phase.DONE) {
                settle(now);
            }
            sync();
        }
    }

    @Nullable
    private ServerPlayer onlinePlayer() {
        return level instanceof ServerLevel sl && player != null ? sl.getServer().getPlayerList().getPlayer(player) : null;
    }

    /** La mano terminó: se pagan las fichas (al instante si el jugador está, o quedan guardadas hasta que vuelva). */
    private void settle(long now) {
        doneAt = now;
        nextStep = 0;
        if (!(level instanceof ServerLevel sl) || player == null) {
            return;
        }
        ServerPlayer p = onlinePlayer();
        Result r = game.result;
        int pay = game.payout;
        int net = pay - game.bet;
        if (pay > 0) {
            List<ItemStack> items = new ArrayList<>(Chips.stacksFor(pay));
            boolean big = r == Result.BLACKJACK || net >= 250;
            Component title = r == Result.BLACKJACK ? Component.translatable("casino.blackjack.banner_bj")
                    : Component.translatable("casino.blackjack.banner", pay);
            Prize prize = new Prize(title, items, 0, -1, big);
            if (p != null) {
                prize.deliver(p);
                if (r == Result.BLACKJACK || net >= 100) {
                    CasinoNet.sendTo(p, new WheelWinPacket(prize));
                }
            } else {
                CasinoData.get(sl.getServer()).addPending(player, prize);
            }
        }
        if (p != null) {
            switch (r) {
                case BLACKJACK -> msg(p, "casino.blackjack.res_bj", ChatFormatting.GOLD, pay);
                case WIN -> msg(p, "casino.blackjack.res_win", ChatFormatting.GREEN, pay);
                case PUSH -> msg(p, "casino.blackjack.res_push", ChatFormatting.YELLOW, pay);
                case BUST -> msg(p, "casino.blackjack.res_bust", ChatFormatting.RED, game.bet);
                default -> msg(p, "casino.blackjack.res_lose", ChatFormatting.RED, game.bet);
            }
        }
        if (pay > 0 && r != Result.PUSH) {
            sound(r == Result.BLACKJACK ? ModSounds.WHEEL_WIN.get() : ModSounds.COINS.get(), 0.9F, 1.0F);
        }
        if (net >= 500) {
            Component msg = Component.translatable("casino.blackjack.won_big", Component.literal(playerName).withStyle(ChatFormatting.YELLOW),
                    Component.literal(String.valueOf(pay)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY);
            Vec3 c = center();
            for (ServerPlayer o : sl.players()) {
                if (o.distanceToSqr(c.x, c.y, c.z) < 32 * 32) {
                    o.sendSystemMessage(msg);
                }
            }
        }
    }

    // ------------------------------------------------------------------ cliente

    public void clientTick() {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (!clientInit) {
            clientInit = true;
            seenP = game.player.size();
            seenD = game.dealer.size();
            seenHole = game.holeRevealed;
            seenSeq = dealSeq;
            seenPhase = game.phase;
            Arrays.fill(pBorn, -1000);
            Arrays.fill(dBorn, -1000);
            return;
        }
        Vec3 c = center();
        boolean fresh = false;
        if (dealSeq != seenSeq) {
            seenSeq = dealSeq;
            seenP = 0;
            seenD = 0;
            seenHole = false;
            revealAt = -1000;
            fresh = true;
        }
        int np = game.player.size();
        int nd = game.dealer.size();
        for (int i = seenP; i < np && i < MAX_CARDS; i++) {
            pBorn[i] = now + (fresh ? i * 10L : 0);
        }
        for (int i = seenD; i < nd && i < MAX_CARDS; i++) {
            dBorn[i] = now + (fresh ? i * 10L + 5 : 0);
        }
        seenP = np;
        seenD = nd;
        if (game.holeRevealed && !seenHole) {
            revealAt = now + (fresh ? 20 : 0);
        }
        seenHole = game.holeRevealed;
        if (game.phase == Phase.DONE && seenPhase != Phase.DONE) {
            doneSeenAt = now + (revealAt > now ? 24 : 0);
        }
        // pequeño efecto al terminar una mano ganada
        if (doneSeenAt == now && (game.result == Result.WIN || game.result == Result.BLACKJACK)) {
            int n = game.result == Result.BLACKJACK ? 30 : 8;
            for (int k = 0; k < n; k++) {
                double a = level.random.nextDouble() * Math.PI * 2;
                double sp = 0.04 + level.random.nextDouble() * 0.1;
                level.addParticle(game.result == Result.BLACKJACK && k % 3 == 0 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.HAPPY_VILLAGER,
                        c.x, c.y + 0.1, c.z, Math.cos(a) * sp, Math.abs(Math.sin(a)) * sp + 0.03, Math.sin(a) * sp);
            }
        }
        seenPhase = game.phase;
        // sonidos de cartas cuando "llegan" (las del reparto inicial salen escalonadas)
        for (int i = 0; i < MAX_CARDS; i++) {
            if (pBorn[i] == now || dBorn[i] == now) {
                level.playLocalSound(c.x, c.y, c.z, ModSounds.CARD_DEAL.get(), SoundSource.BLOCKS, 0.5F, 1.0F + (i % 3) * 0.05F, false);
            }
        }
        if (revealAt == now) {
            level.playLocalSound(c.x, c.y, c.z, ModSounds.CARD_FLIP.get(), SoundSource.BLOCKS, 0.7F, 1.0F, false);
        }
    }

    // ------------------------------------------------------------------ red y guardado

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-2, 0, -2), worldPosition.offset(3, 2, 3));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag t = pkt.getTag();
        if (t != null) {
            load(t);
        }
    }

    private static int[] toArray(List<Integer> l) {
        return l.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void fill(List<Integer> l, int[] a) {
        l.clear();
        for (int v : a) {
            if (v >= 0 && v < 52 && l.size() < MAX_CARDS) {
                l.add(v);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putIntArray("pc", toArray(game.player));
        tag.putIntArray("dc", toArray(game.dealer));
        tag.putInt("phase", game.phase.ordinal());
        tag.putInt("result", game.result.ordinal());
        tag.putInt("bet", game.bet);
        tag.putBoolean("doubled", game.doubled);
        tag.putBoolean("hole", game.holeRevealed);
        tag.putInt("payout", game.payout);
        if (player != null) {
            tag.putUUID("player", player);
        }
        tag.putString("playerName", playerName);
        tag.putInt("dealSeq", dealSeq);
        tag.putLong("nextStep", nextStep);
        tag.putLong("lastAction", lastAction);
        tag.putLong("doneAt", doneAt);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fill(game.player, tag.getIntArray("pc"));
        fill(game.dealer, tag.getIntArray("dc"));
        Phase[] ph = Phase.values();
        Result[] rs = Result.values();
        game.phase = ph[Math.max(0, Math.min(ph.length - 1, tag.getInt("phase")))];
        game.result = rs[Math.max(0, Math.min(rs.length - 1, tag.getInt("result")))];
        game.bet = tag.getInt("bet");
        game.doubled = tag.getBoolean("doubled");
        game.holeRevealed = tag.getBoolean("hole");
        game.payout = tag.getInt("payout");
        player = tag.hasUUID("player") ? tag.getUUID("player") : null;
        playerName = tag.getString("playerName");
        dealSeq = tag.getInt("dealSeq");
        nextStep = tag.getLong("nextStep");
        lastAction = tag.getLong("lastAction");
        doneAt = tag.getLong("doneAt");
    }
}
