package com.pescacoleccion.pelea;

import com.axeliscore.plat.Net;
import com.pescacoleccion.PescaColeccion;
import net.minecraft.network.FriendlyByteBuf;

/** Mensajes de la pelea: el servidor la abre en el cliente y el cliente devuelve sus teclas al terminar. */
public final class PeleaNet {
    private PeleaNet() {
    }

    /** Servidor → cliente: empieza una pelea (id, nombre del pez para el final, rareza y parámetros). */
    public record Abrir(int id, String pez, int rareza, byte[] params) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(id);
            b.writeUtf(pez);
            b.writeVarInt(rareza);
            b.writeByteArray(params);
        }

        static Abrir read(FriendlyByteBuf b) {
            return new Abrir(b.readVarInt(), b.readUtf(), b.readVarInt(), b.readByteArray());
        }
    }

    /** Cliente → servidor: terminó (o se rindió) con estas teclas; {@code endMs} es cuándo terminó en el cliente. */
    public record Fin(int id, boolean rendido, int endMs, byte[] teclas) {
        void write(FriendlyByteBuf b) {
            b.writeVarInt(id);
            b.writeBoolean(rendido);
            b.writeVarInt(endMs);
            b.writeByteArray(teclas);
        }

        static Fin read(FriendlyByteBuf b) {
            return new Fin(b.readVarInt(), b.readBoolean(), b.readVarInt(), b.readByteArray());
        }
    }

    public static void register(Net net) {
        net.register(Abrir.class, Abrir::write, Abrir::read, (m, sender) -> Net.onClient(() -> ClientSide.open(m)));
        net.register(Fin.class, Fin::write, Fin::read, (m, sender) -> {
            if (sender != null) {
                Peleas.finish(sender, m.id(), m.rendido(), m.endMs(), Pelea.events(m.teclas()));
            }
        });
    }

    /** Puente al cliente (sus clases solo existen allá). */
    static final class ClientSide {
        static void open(Abrir m) {
            com.pescacoleccion.client.PeleaClient.open(m.id(), m.pez(), m.rareza(), Pelea.params(m.params()));
        }
    }

    static void send(net.minecraft.server.level.ServerPlayer p, Object msg) {
        PescaColeccion.NET.toPlayer(p, msg);
    }
}
