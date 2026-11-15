package com.casino.net;

import com.casino.data.CasinoData;
import com.casino.data.CasinoStats;
import com.casino.data.Membership;
import com.casino.plat.Net;
import com.casino.plat.Plat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** La tarjeta de socio: servidor -> jugador (abrir con tus datos) y jugador -> servidor (cobrar el bono diario). */
public final class MemberPacket {
    private MemberPacket() {
    }

    /** Manda al jugador su tarjeta (y se abre la pantalla). */
    public static void open(ServerPlayer p) {
        CasinoStats s = CasinoStats.get(Plat.server(p));
        CasinoStats.Entry e = s.entryOrNew(p.getUUID(), p.getGameProfile().getName());
        CasinoNet.sendTo(p, new Open(p.getGameProfile().getName(), e.wagered, e.won, e.bets, e.biggest, e.achievements, e.gamesMask, e.wheelSpins,
                e.drinks, e.lastBonus < CasinoData.today()));
    }

    public static final class Open {
        public final String name;
        public final long wagered, won, bets, biggest, achievements;
        public final int gamesMask, wheelSpins, drinks;
        public final boolean bonus;

        public Open(String name, long wagered, long won, long bets, long biggest, long achievements, int gamesMask, int wheelSpins, int drinks,
                boolean bonus) {
            this.name = name;
            this.wagered = wagered;
            this.won = won;
            this.bets = bets;
            this.biggest = biggest;
            this.achievements = achievements;
            this.gamesMask = gamesMask;
            this.wheelSpins = wheelSpins;
            this.drinks = drinks;
            this.bonus = bonus;
        }

        public int level() {
            return Membership.level(wagered);
        }

        public static void encode(Open m, FriendlyByteBuf b) {
            b.writeUtf(m.name, 32);
            b.writeVarLong(m.wagered);
            b.writeVarLong(m.won);
            b.writeVarLong(m.bets);
            b.writeVarLong(m.biggest);
            b.writeLong(m.achievements);
            b.writeVarInt(m.gamesMask);
            b.writeVarInt(m.wheelSpins);
            b.writeVarInt(m.drinks);
            b.writeBoolean(m.bonus);
        }

        public static Open decode(FriendlyByteBuf b) {
            return new Open(b.readUtf(32), b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readVarLong(), b.readLong(), b.readVarInt(), b.readVarInt(),
                    b.readVarInt(), b.readBoolean());
        }

        public static void handle(Open m, ServerPlayer p) {
            Net.onClient(() -> com.casino.client.ClientHooks.openMember(m));
        }
    }

    /** Cobrar el bono diario. */
    public static final class Claim {
        public static void encode(Claim m, FriendlyByteBuf b) {
        }

        public static Claim decode(FriendlyByteBuf b) {
            return new Claim();
        }

        public static void handle(Claim m, ServerPlayer p) {
            if (p == null) {
                return;
            }
            int got = CasinoStats.claimDaily(p);
            if (got > 0) {
                Plat.msg(p, Component.translatable("casino.member.bonus_got", got).withStyle(ChatFormatting.GOLD), true);
                p.level().playSound(null, p.blockPosition(), com.casino.registry.ModSounds.COINS.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.1F);
            } else {
                Plat.msg(p, Component.translatable("casino.member.bonus_done").withStyle(ChatFormatting.GRAY), true);
            }
            open(p);
        }
    }
}
