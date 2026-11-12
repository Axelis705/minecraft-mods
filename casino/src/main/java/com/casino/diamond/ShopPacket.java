package com.casino.diamond;

import com.casino.plat.Net;
import com.casino.plat.Ser;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Mensajes de la tienda de premios. */
public final class ShopPacket {
    private ShopPacket() {
    }

    /** Servidor -> jugador: abrir (o refrescar) la tienda. */
    public static final class Open {
        public final BlockPos pos;
        public final List<PrizeShopBE.Offer> offers;
        public final boolean edit;

        public Open(BlockPos pos, List<PrizeShopBE.Offer> offers, boolean edit) {
            this.pos = pos;
            this.offers = new ArrayList<>(offers);
            this.edit = edit;
        }

        public static void encode(Open m, FriendlyByteBuf b) {
            b.writeBlockPos(m.pos);
            b.writeVarInt(m.offers.size());
            for (PrizeShopBE.Offer o : m.offers) {
                Ser.writeItem(b, o.item());
                b.writeVarInt(o.price());
            }
            b.writeBoolean(m.edit);
        }

        public static Open decode(FriendlyByteBuf b) {
            BlockPos pos = b.readBlockPos();
            int n = Math.min(PrizeShopBE.MAX, b.readVarInt());
            List<PrizeShopBE.Offer> l = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                ItemStack s = Ser.readItem(b);
                l.add(new PrizeShopBE.Offer(s, b.readVarInt()));
            }
            return new Open(pos, l, b.readBoolean());
        }

        public static void handle(Open m, ServerPlayer p) {
            Net.onClient(() -> com.casino.client.ClientHooks.showShop(m));
        }
    }

    /** Cliente -> servidor: comprar el premio idx. */
    public static final class Buy {
        public final BlockPos pos;
        public final int idx;

        public Buy(BlockPos pos, int idx) {
            this.pos = pos;
            this.idx = idx;
        }

        public static void encode(Buy m, FriendlyByteBuf b) {
            b.writeBlockPos(m.pos);
            b.writeVarInt(m.idx);
        }

        public static Buy decode(FriendlyByteBuf b) {
            return new Buy(b.readBlockPos(), b.readVarInt());
        }

        public static void handle(Buy m, ServerPlayer p) {
            PrizeShopBlock.buy(p, m.pos, m.idx);
        }
    }

    /** Cliente -> servidor: editar la lista (solo operador). */
    public static final class Edit {
        public final BlockPos pos;
        public final int op;
        public final int idx;
        public final int price;

        public Edit(BlockPos pos, int op, int idx, int price) {
            this.pos = pos;
            this.op = op;
            this.idx = idx;
            this.price = price;
        }

        public static void encode(Edit m, FriendlyByteBuf b) {
            b.writeBlockPos(m.pos);
            b.writeVarInt(m.op);
            b.writeVarInt(m.idx);
            b.writeVarInt(m.price);
        }

        public static Edit decode(FriendlyByteBuf b) {
            return new Edit(b.readBlockPos(), b.readVarInt(), b.readVarInt(), b.readVarInt());
        }

        public static void handle(Edit m, ServerPlayer p) {
            PrizeShopBlock.edit(p, m.pos, m.op, m.idx, m.price);
        }
    }
}
