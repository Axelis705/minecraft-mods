package com.navidad.krampus;

import com.navidad.Navidad;
import com.navidad.entidad.Krampus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Los mensajes del Krampus entre el cliente y el servidor. */
public final class KrampusMsg {
    private KrampusMsg() {
    }

    /** (Cliente → servidor) Dentro del saco: cuántas veces machacó la tecla de saltar desde el último mensaje. */
    public record Forcejeo(int veces) {
        public void write(FriendlyByteBuf b) {
            b.writeVarInt(veces);
        }

        public static Forcejeo read(FriendlyByteBuf b) {
            return new Forcejeo(b.readVarInt());
        }

        static void handle(Forcejeo m, ServerPlayer p) {
            if (p != null && p.getVehicle() instanceof Krampus k) {
                k.forcejear(p, Math.max(0, Math.min(4, m.veces())));
            }
        }
    }

    /** (Servidor → cliente) Qué tan espesa es la niebla de la Noche del Krampus (0 = nada, 1 = no ves nada). */
    public record Niebla(float cuanta) {
        public void write(FriendlyByteBuf b) {
            b.writeFloat(cuanta);
        }

        public static Niebla read(FriendlyByteBuf b) {
            return new Niebla(b.readFloat());
        }
    }

    /** Lo que hace el cliente con la niebla (lo pone el cliente al arrancar; en el servidor no hace nada). */
    public static java.util.function.Consumer<Float> alNiebla = f -> {
    };

    /** Se registran en el arranque común, en este orden. */
    public static void registrar() {
        Navidad.NET.register(Forcejeo.class, Forcejeo::write, Forcejeo::read, Forcejeo::handle);
        Navidad.NET.register(Niebla.class, Niebla::write, Niebla::read, (m, p) -> alNiebla.accept(m.cuanta()));
    }

    public static void niebla(ServerPlayer p, float cuanta) {
        Navidad.NET.toPlayer(p, new Niebla(cuanta));
    }
}
