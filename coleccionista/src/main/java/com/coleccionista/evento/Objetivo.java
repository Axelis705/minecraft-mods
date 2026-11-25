package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.coleccionista.Coleccionista;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El indicador de "ir acá" (como en los videojuegos): a cada pieza viva se le dice a dónde tiene que ir ahora (el portón, la sala, un
 * ascensor, la siguiente puerta del circuito, una silla...). El cliente lo dibuja como un rombo en la pantalla con la distancia; si queda
 * atrás o fuera de la vista, una flecha en la orilla. Cada parte del evento dice su objetivo ({@link #calcular}).
 */
public final class Objetivo {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":objetivo";

    /** A dónde ir: el punto y el texto (clave de idioma) que se ve junto al rombo. */
    public record Meta(double x, double y, double z, String clave) {
        public static Meta en(BlockPos b, double dy, String clave) {
            return b == null ? null : new Meta(b.getX() + 0.5, b.getY() + dy, b.getZ() + 0.5, clave);
        }

        String firma() {
            return String.format(java.util.Locale.ROOT, "%.1f %.1f %.1f %s", x, y, z, clave);
        }

        CompoundTag tag() {
            CompoundTag t = new CompoundTag();
            t.putDouble("x", x);
            t.putDouble("y", y);
            t.putDouble("z", z);
            t.putString("texto", clave);
            return t;
        }
    }

    /** Lo último que se le mandó a cada jugador (para no mandar lo mismo cada medio segundo). */
    private static final Map<UUID, String> ENVIADO = new HashMap<>();

    private Objetivo() {
    }

    public static void init() {
        AxelisCore.onServerTick(Objetivo::tick);
        AxelisCore.onLogin(p -> ENVIADO.remove(p.getUUID()));
    }

    private static void tick(MinecraftServer s) {
        if (s.getTickCount() % 10 != 3) {
            return;
        }
        Evento ev = Evento.get(s);
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            Pieza p = ev.pieza(sp);
            Meta m = p == null || !p.vivo() || ev.fase() == Evento.Fase.PAUSA ? null : calcular(s, ev, p, sp);
            String firma = m == null ? "" : m.firma();
            if (!firma.equals(ENVIADO.get(sp.getUUID()))) {
                ENVIADO.put(sp.getUUID(), firma);
                PlayerStore.Sync.toPlayer(sp, KEY, m == null ? new CompoundTag() : m.tag());
            }
        }
    }

    /** A dónde tiene que ir esa pieza ahora (null: a ningún lado, o no se le dice). */
    private static Meta calcular(MinecraftServer s, Evento ev, Pieza p, ServerPlayer sp) {
        if (!Complejo.construido(s)) {
            return null;
        }
        // el patio: ya fuera de la celda, al portón (y por él a la sala grande)
        if (Despertar.saliendo() || Celdas.portonAbierto(s) && Capitulo1.fase() == Capitulo1.Fase.SALA) {
            if (Celdas.enCelda(s, sp.blockPosition()) || Capitulo1.enSala(s, sp)) {
                return null;
            }
            return Meta.en(Complejo.pos(s, "porton.2"), 1.5, MOD + ".obj.porton");
        }
        Meta m = Capitulo1.objetivo(s, sp);
        if (m != null) {
            return m;
        }
        if (com.coleccionista.prueba.Piano.abierta()) {
            return com.coleccionista.prueba.Piano.objetivo(s, sp);
        }
        if (com.coleccionista.prueba.Te.abierta()) {
            return com.coleccionista.prueba.Te.objetivo(s, sp);
        }
        if (com.coleccionista.prueba.Sorpresa.abierta()) {
            return com.coleccionista.prueba.Sorpresa.objetivo(s, sp);
        }
        if (com.coleccionista.prueba.Caja.abierta()) {
            return com.coleccionista.prueba.Caja.objetivo(s, sp);
        }
        if (com.coleccionista.prueba.Mascota.abierta()) {
            return com.coleccionista.prueba.Mascota.objetivo(s, sp);
        }
        return com.coleccionista.prueba.Sentidos.objetivo(s, sp);
    }
}
