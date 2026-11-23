package com.axeliscore.combat.client;

import com.axeliscore.combat.Estado;
import com.axeliscore.combat.Estados;
import com.axeliscore.net.CoreNet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** (Cliente) Los estados de combate que mandó el servidor de cada entidad (para íconos, partículas y el HUD). */
public final class EstadosCliente {
    /** Un estado que se ve. */
    public static final class Visto {
        public final Estado estado;
        public final int nivel, max;
        final int ticks;
        final long llego = System.currentTimeMillis();

        Visto(Estado estado, int ticks, int max, int nivel) {
            this.estado = estado;
            this.ticks = ticks;
            this.max = Math.max(1, max);
            this.nivel = nivel;
        }

        /** Ticks que le quedan (contando desde que llegó el aviso). */
        public float resta() {
            return ticks - (System.currentTimeMillis() - llego) / 50F;
        }

        /** Cuánto le queda (1 = recién puesto, 0 = se acaba). */
        public float frac() {
            return Math.max(0, Math.min(1, resta() / max));
        }
    }

    private static final Map<Integer, List<Visto>> MAPA = new ConcurrentHashMap<>();

    private EstadosCliente() {
    }

    public static void recibir(CoreNet.EstadosMsg m) {
        List<Visto> l = new ArrayList<>();
        for (CoreNet.EstadosMsg.Item i : m.items()) {
            Estado s = Estados.porId(i.id());
            if (s != null && i.ticks() > 0) {
                l.add(new Visto(s, i.ticks(), i.max(), i.nivel()));
            }
        }
        if (l.isEmpty()) {
            MAPA.remove(m.entity());
        } else {
            MAPA.put(m.entity(), l);
        }
    }

    /** Los estados que tiene ahora (sin los que ya se acabaron). */
    public static List<Visto> de(int entity) {
        List<Visto> l = MAPA.get(entity);
        if (l == null) {
            return List.of();
        }
        List<Visto> r = new ArrayList<>(l.size());
        for (Visto v : l) {
            if (v.resta() > 0) {
                r.add(v);
            }
        }
        return r;
    }

    public static boolean tiene(int entity, Estado s) {
        for (Visto v : de(entity)) {
            if (v.estado == s) {
                return true;
            }
        }
        return false;
    }

    public static boolean aturdido(int entity) {
        for (Visto v : de(entity)) {
            if (v.estado.aturdeAlQueLoTiene()) {
                return true;
            }
        }
        return false;
    }

    /** Las entidades que tienen algún estado. */
    public static java.util.Set<Integer> conEstados() {
        return MAPA.keySet();
    }

    /** Al cambiar de mundo. */
    public static void limpiar() {
        MAPA.clear();
    }
}
