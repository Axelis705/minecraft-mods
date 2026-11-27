package com.coleccionista.prueba;

import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Objetivo;
import com.coleccionista.mapa.Pasos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * El camino hacia adelante entre capítulos ({@link com.coleccionista.mapa.Salida}, {@link Pasos}): al terminar una sala se abre su
 * puerta de salida; todos cruzan caminando el pasillo con el tema de la sala que sigue y entran por atrás a su pasillo de servicio (ahí ya
 * nadie vuelve por donde vino ni hay apagón ni "traslado"). Cada capítulo lo usa en su llamada ({@code llamar}): si el pasillo está
 * construido se va por aquí; si no (un mapa viejo), como antes.
 */
public final class Adelante {
    private static final String MOD = Coleccionista.MODID;

    private Adelante() {
    }

    /** ¿Se puede llegar caminando a la sala {@code hacia}? (Están construidas la que se deja, su pasillo y la que sigue, con sus puertas.) */
    public static boolean hay(MinecraftServer s, String hacia) {
        String de = Pasos.anterior(hacia);
        return de != null && Complejo.construido(s, de) && Complejo.construido(s, hacia) && Complejo.construido(s, "paso_" + hacia)
                && Complejo.pos(s, "salida." + de + ".a") != null && Complejo.pos(s, "paso_" + hacia + ".inicio") != null;
    }

    /** Abre (o cierra) el camino: la puerta de salida de la sala que se deja y la de atrás del pasillo de servicio de la que sigue. */
    public static void abrir(MinecraftServer s, String hacia, String pasillo, boolean abrir) {
        puerta(s, "salida." + Pasos.anterior(hacia), abrir);
        puerta(s, pasillo + ".entrada", abrir);
        Coleccionista.LOGGER.info("COLECCIONISTA_ADELANTE {} {}", hacia, abrir ? "abre" : "cierra");
    }

    /** Las dos hojas de una puerta ({@code <marca>.a} y {@code .b}), con su sonido de acero. */
    public static void puerta(MinecraftServer s, String marca, boolean abrir) {
        ServerLevel l = s.overworld();
        boolean sono = false;
        for (String h : new String[] {".a", ".b"}) {
            BlockPos p = Complejo.pos(s, marca + h);
            if (p == null) {
                continue;
            }
            for (BlockPos q : new BlockPos[] {p, p.above()}) {
                BlockState st = l.getBlockState(q);
                if (st.getBlock() instanceof DoorBlock && st.getValue(DoorBlock.OPEN) != abrir) {
                    l.setBlock(q, st.setValue(DoorBlock.OPEN, abrir), 2 | 16);
                    if (!sono) {
                        sono = true;
                        l.playSound(null, q, abrir ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1F, 0.6F);
                    }
                }
            }
        }
    }

    /**
     * A dónde ir mientras se camina: a la puerta de salida (si todavía está en la sala que se deja) y luego por el pasillo, de codo en
     * codo, hasta la puerta de atrás del pasillo de servicio. Null si ya está más allá (adentro del pasillo de servicio o en la sala).
     */
    public static Objetivo.Meta meta(MinecraftServer s, ServerPlayer sp, String hacia, String pasillo) {
        BlockPos o = Complejo.origen(s, "paso_" + hacia);
        Complejo.Zona z = Complejo.zona("paso_" + hacia);
        if (o == null || z == null) {
            return null;
        }
        double x = sp.getX() - o.getX();
        if (x < 0) {
            String de = Pasos.anterior(hacia);
            BlockPos salida = Complejo.pos(s, "salida." + de + ".a");
            // si la sala tiene camino marcado (la casa: la salida está en el sótano), se va de punto en punto
            java.util.List<BlockPos> via = new java.util.ArrayList<>();
            for (int i = 0; i < 64; i++) {
                BlockPos v = Complejo.pos(s, "salida." + de + ".via." + i);
                if (v == null) {
                    break;
                }
                via.add(v);
            }
            if (!via.isEmpty() && salida != null) {
                via.add(salida);
                BlockPos mejor = salida;
                double dm = Double.MAX_VALUE;
                for (int i = 0; i + 1 < via.size(); i++) {
                    BlockPos a = via.get(i), b = via.get(i + 1);
                    double d = distTramo(sp.getX(), sp.getZ(), a.getX() + 0.5, a.getZ() + 0.5, b.getX() + 0.5, b.getZ() + 0.5);
                    // otro piso cuenta como lejos (la escalera del sótano une dos)
                    double dy = Math.max(0, Math.min(Math.abs(sp.getY() - a.getY()), Math.abs(sp.getY() - b.getY())) - 1.5);
                    d += dy * 4;
                    if (d <= dm + 0.01) {
                        dm = d;
                        mejor = b;
                    }
                }
                return Objetivo.Meta.en(mejor, 1, MOD + ".obj.salida");
            }
            return Objetivo.Meta.en(salida, 1, MOD + ".obj.salida");
        }
        if (x > z.plano().x1 + 1) {
            return null;
        }
        // el tramo más cercano (los tres van derechos); se va al final de ese tramo (en empate, al de más adelante)
        String[] pts = {"inicio", "codo1", "codo2", "fin"};
        BlockPos[] b = new BlockPos[pts.length + 1];
        for (int i = 0; i < pts.length; i++) {
            b[i] = Complejo.pos(s, "paso_" + hacia + "." + pts[i]);
        }
        b[pts.length] = Complejo.pos(s, pasillo + ".entrada.a");
        BlockPos mejor = null;
        double dm = Double.MAX_VALUE;
        for (int i = 0; i + 1 < b.length; i++) {
            if (b[i] == null || b[i + 1] == null) {
                continue;
            }
            double d = distTramo(sp.getX(), sp.getZ(), b[i].getX() + 0.5, b[i].getZ() + 0.5, b[i + 1].getX() + 0.5, b[i + 1].getZ() + 0.5);
            if (d <= dm + 0.01) {
                dm = d;
                mejor = b[i + 1];
            }
        }
        return Objetivo.Meta.en(mejor, 1, MOD + ".obj.adelante");
    }

    private static double distTramo(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az, l = dx * dx + dz * dz;
        double k = l == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / l));
        double qx = ax + k * dx - px, qz = az + k * dz - pz;
        return Math.sqrt(qx * qx + qz * qz);
    }
}
