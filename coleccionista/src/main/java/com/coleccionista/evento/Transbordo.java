package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.coleccionista.Coleccionista;
import com.coleccionista.bot.PiezaBot;
import com.coleccionista.mapa.Vehiculos;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Subirse a un vehículo para ir de un lugar al siguiente (sin apagón): se abre su puerta, la flecha marca por dónde subir y cuando ya están
 * todos los vivos adentro (o pasó un rato: los que se quedaron afuera suben solos) se cierra y viaja ({@link Cabina}). Al llegar se abre
 * la puerta del otro lado y sigue lo que toque.
 * <pre>
 *     El autobús del Coleccionista: de la cochera del teatro (fin del capítulo 1) a la parada de la casa (capítulo 2).
 * </pre>
 */
public final class Transbordo {
    private static final String MOD = Coleccionista.MODID;

    /** Un vehículo: sus dos cabinas idénticas, cómo viaja, su interior, cuánto dura el viaje y lo que dice el Coleccionista en él. */
    public record Vehiculo(String id, String sale, String llega, Cabina.Tipo tipo, int ancho, int alto, int largo, int viaje, String guion,
                           String luces) {
    }

    public static final Vehiculo AUTOBUS = new Vehiculo("autobus", "bus_tea", "bus_man", Cabina.Tipo.AUTOBUS, Vehiculos.BUS_ANCHO,
            Vehiculos.BUS_ALTO, Vehiculos.BUS_LARGO, 470, "autobus", "coch.luz.");
    /** Cuánto espera a que suban todos antes de irse de todos modos (los de afuera suben solos). */
    public static int esperaAbordar = 1200;

    private enum Fase { NADA, ABORDAR, VIAJE }

    private static Fase fase = Fase.NADA;
    private static Vehiculo activo;
    private static long desde;
    private static Runnable alLlegar;

    private Transbordo() {
    }

    public static void init() {
        AxelisCore.onServerTick(Transbordo::tick);
    }

    /** ¿Están construidas las dos cabinas de ese vehículo? */
    public static boolean hay(MinecraftServer s, Vehiculo v) {
        return Complejo.pos(s, "cab." + v.sale() + ".origen") != null && Complejo.pos(s, "cab." + v.llega() + ".origen") != null;
    }

    public static boolean activo() {
        return fase != Fase.NADA;
    }

    /** ¿Ese vehículo está esperando gente o viajando? */
    public static boolean activo(Vehiculo v) {
        return fase != Fase.NADA && activo == v;
    }

    /**
     * Abre la puerta del vehículo (con la luz prendida) y espera a que suban todos los vivos; los que no estén ni cerca (el organizador lo
     * pidió desde otro lado) suben de una vez. Al llegar corre {@code fin}.
     */
    public static void abordar(MinecraftServer s, Vehiculo v, Runnable fin) {
        activo = v;
        alLlegar = fin;
        fase = Fase.ABORDAR;
        desde = s.overworld().getGameTime();
        luces(s, v, true);
        Cabina.puerta(s, v.llega(), false);
        Cabina.puerta(s, v.sale(), true);
        BlockPos c = Complejo.pos(s, v.id().equals("autobus") ? "vit.antesala" : v.sale() + ".subir");
        int n = 0;
        for (Entity e : vivos(s)) {
            boolean cerca = c != null && e.distanceToSqr(Vec3.atCenterOf(c)) < 40 * 40;
            if (!cerca || e instanceof PiezaBot) {
                subir(s, v, e, n++);
            }
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_TRANSBORDO abordar {}", v.id());
    }

    private static void tick(MinecraftServer s) {
        if (fase != Fase.ABORDAR || activo == null) {
            return;
        }
        long now = s.overworld().getGameTime();
        if (now % 10 != 0) {
            return;
        }
        Vehiculo v = activo;
        List<Entity> todos = vivos(s);
        boolean listos = !todos.isEmpty();
        for (Entity e : todos) {
            listos &= dentro(s, v, e);
        }
        if (listos && now - desde >= 40 || now - desde >= esperaAbordar) {
            partir(s, v, todos);
        }
    }

    /** Se cierra la puerta (los que se quedaron afuera suben), se apaga la luz de donde sale y viaja; en el camino habla el Coleccionista. */
    private static void partir(MinecraftServer s, Vehiculo v, List<Entity> todos) {
        int n = 0;
        for (Entity e : todos) {
            if (!dentro(s, v, e)) {
                subir(s, v, e, n++);
            }
        }
        fase = Fase.VIAJE;
        luces(s, v, false);
        Runnable fin = alLlegar;
        boolean ok = Cabina.viajar(s, v.sale(), v.llega(), v.tipo(), v.viaje(), v.ancho(), v.alto(), v.largo(), todos, () -> {
            fase = Fase.NADA;
            activo = null;
            luces(s, v, true);
            Coleccionista.LOGGER.info("COLECCIONISTA_TRANSBORDO llega {}", v.id());
            if (fin != null) {
                fin.run();
            }
        });
        if (!ok) {
            fase = Fase.NADA;
            activo = null;
            if (fin != null) {
                fin.run();
            }
            return;
        }
        if (v.guion() != null) {
            Pantallas.soloGrande(s, true);
            Pantallas.hablar(s, v.guion(), null);
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_TRANSBORDO sale {} van={}", v.id(), todos.size());
    }

    /** Sube a alguien al vehículo: a un lugar del pasillo (en fila, de atrás hacia adelante), mirando al frente. */
    private static void subir(MinecraftServer s, Vehiculo v, Entity e, int n) {
        BlockPos o = Complejo.pos(s, "cab." + v.sale() + ".origen");
        if (o == null) {
            return;
        }
        float yaw = Complejo.yaw("cab." + v.sale() + ".origen");
        double u = 1.5 + n % 3, w = 1.5 + (n / 3) % Math.max(1, v.largo() - 3);
        double r = Math.toRadians(yaw), c = Math.round(Math.cos(r)), sn = Math.round(Math.sin(r));
        double x = o.getX() + u * c - w * sn, z = o.getZ() + u * sn + w * c;
        e.stopRiding();
        e.fallDistance = 0;
        if (e instanceof ServerPlayer sp) {
            sp.teleportTo(s.overworld(), x, o.getY(), z, yaw, 0F);
        } else {
            e.moveTo(x, o.getY(), z, yaw, 0F);
            e.setYHeadRot(yaw);
        }
    }

    private static boolean dentro(MinecraftServer s, Vehiculo v, Entity e) {
        AABB c = Cabina.caja(s, v.sale(), v.ancho(), v.alto(), v.largo());
        return c != null && c.contains(e.position());
    }

    private static List<Entity> vivos(MinecraftServer s) {
        List<Entity> l = new ArrayList<>();
        for (Pieza p : Evento.get(s).vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null) {
                l.add(e);
            }
        }
        return l;
    }

    /** Prende o apaga las luces de donde sale el vehículo (los focos de la cochera): de noche, por las ventanas no se ve nada. */
    private static void luces(MinecraftServer s, Vehiculo v, boolean on) {
        if (v.luces() == null) {
            return;
        }
        ServerLevel l = s.overworld();
        for (int i = 0; i < 64; i++) {
            BlockPos p = Complejo.pos(s, v.luces() + i);
            if (p == null) {
                break;
            }
            BlockState st = l.getBlockState(p);
            if (st.hasProperty(com.coleccionista.bloque.deco.DecoBlock.ENCENDIDA) && st.getValue(com.coleccionista.bloque.deco.DecoBlock.ENCENDIDA) != on) {
                l.setBlock(p, st.setValue(com.coleccionista.bloque.deco.DecoBlock.ENCENDIDA, on), 3);
            }
        }
    }

    /** A dónde ir mientras se espera para subir: la puerta del vehículo (null si ya está adentro o no hay nada que abordar). */
    public static Objetivo.Meta objetivo(MinecraftServer s, ServerPlayer sp) {
        if (fase != Fase.ABORDAR || activo == null || dentro(s, activo, sp)) {
            return null;
        }
        return Objetivo.Meta.en(Complejo.pos(s, activo.sale() + ".subir"), 1, MOD + ".obj." + activo.id());
    }

    /** Lo que se lee en la línea de la prueba mientras se espera (o null). */
    public static Component estado() {
        return fase == Fase.ABORDAR && activo != null ? Component.translatable(MOD + ".trans." + activo.id() + ".subir") : null;
    }

    public static void reiniciar(MinecraftServer s) {
        if (activo != null) {
            luces(s, activo, true);
        }
        fase = Fase.NADA;
        activo = null;
        alLlegar = null;
    }
}
