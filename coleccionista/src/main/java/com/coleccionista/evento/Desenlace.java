package com.coleccionista.evento;

import com.axeliscore.data.PlayerStore;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Adelante;
import com.coleccionista.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * El desenlace del evento (después de la final): "Puedes irte. La puerta está abierta… esta vez."
 * <ol>
 *     <li><b>Camina</b>: se abre la puerta del este de la mesa de juego y el ganador se va solo por el pasillo largo ({@link
 *     com.coleccionista.mapa.SalidaFinal}): la casa, el sótano, el yeso cada vez más claro y al fondo la luz. Los fantasmas lo ven irse
 *     con una cámara de cine que lo sigue por detrás (y su pantalla se va poniendo blanca mientras él se acerca).</li>
 *     <li><b>Luz</b>: cuando cruza el portón, todo se pone blanco.</li>
 *     <li><b>Créditos</b>: la colección (cada pieza, en el orden en que cayó, con su cara, su número y dónde cayó), el ganador en grande
 *     ("la única pieza que salió") y "Hecho por Axelis". Mientras, todos regresan (sin que se vea) a la mesa de juego.</li>
 *     <li><b>Después de los créditos</b>: la cámara frente a la vitrina vacía junto al trono… que ya no está vacía: ahí está el ganador,
 *     hecho piedra. La pantalla grande se prende: "Una pieza nueva para mi colección. Hasta la próxima colección…" y se va a negro: FIN.</li>
 * </ol>
 * Empieza solo al terminar la final (cuando se sueltan las sillas) si el pasillo está construido; o con {@code /evento desenlace}.
 */
public final class Desenlace {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":desenlace";

    public enum Fase { NADA, CAMINA, LUZ, CREDITOS, POST, FIN }

    private static Fase fase = Fase.NADA;
    private static long t, desde;
    private static UUID ganador;
    private static String nombre = "";
    private static UUID estatua;
    /** Cuánto duran los créditos (ticks): depende de cuántas piezas hay. */
    private static int creditos;
    private static boolean hablo;
    /** ¿Ya cruzó la puerta del este? (para la flecha: primero la puerta, luego la luz). */
    private static boolean cruzo;

    /** Cuánto dura todo lo blanco (ticks) antes de los créditos y cuánto la vitrina antes de que hable. */
    public static final int T_LUZ = 70, T_POST_HABLA = 70, T_FIN = 110;

    private Desenlace() {
    }

    public static void init() {
        com.axeliscore.AxelisCore.onServerTick(Desenlace::tick);
        com.axeliscore.AxelisCore.onLogin(Desenlace::alEntrar);
    }

    public static Fase fase() {
        return fase;
    }

    /** ¿Está pasando el desenlace? (la flecha, los fantasmas y la mesa lo consultan). */
    public static boolean activo() {
        return fase != Fase.NADA && fase != Fase.FIN;
    }

    public static boolean hay(MinecraftServer s) {
        return Complejo.construido(s, "salida_final") && Complejo.pos(s, "sf.luz") != null && Complejo.pos(s, "salida.veintiuno.a") != null;
    }

    // ------------------------------------------------------------------ empezar y terminar

    /** Empieza con el ganador (la única pieza viva). Devuelve por qué no se pudo (null si empezó). */
    public static Component empezar(MinecraftServer s) {
        if (!hay(s)) {
            return Component.translatable(MOD + ".des.sin_mapa");
        }
        Evento ev = Evento.get(s);
        List<Pieza> vivos = ev.vivos();
        if (vivos.size() != 1) {
            return Component.translatable(MOD + ".des.sin_ganador", vivos.size());
        }
        Pieza g = vivos.get(0);
        limpiarEstatua(s);
        ganador = g.id;
        nombre = g.nombre;
        fase = Fase.CAMINA;
        desde = t;
        cruzo = false;
        hablo = false;
        Adelante.puerta(s, "salida.veintiuno", true);
        Entity e = Evento.entidad(s, g);
        if (e instanceof ServerPlayer sp && sp.isSpectator()) {
            sp.setGameMode(GameType.ADVENTURE);
        }
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_DESENLACE camina ganador={} {}", g.etiqueta(), g.nombre);
        return null;
    }

    /** Salta a la siguiente parte (para probar o si algo se atora). */
    public static boolean saltar(MinecraftServer s) {
        switch (fase) {
            case CAMINA -> luz(s);
            case LUZ -> creditos(s);
            case CREDITOS -> post(s);
            case POST -> fin(s);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Lo deja todo como antes: la puerta cerrada, sin estatua ni luz en la vitrina, las cámaras sueltas. */
    public static void limpiar(MinecraftServer s) {
        if (fase == Fase.POST) {
            Pantallas.callar(s);
        }
        fase = Fase.NADA;
        ganador = null;
        nombre = "";
        cruzo = false;
        if (Complejo.construido(s)) {
            Adelante.puerta(s, "salida.veintiuno", false);
            luzVitrina(s, false);
        }
        limpiarEstatua(s);
        enviar(s);
    }

    private static void limpiarEstatua(MinecraftServer s) {
        if (estatua == null) {
            return;
        }
        for (ServerLevel l : s.getAllLevels()) {
            Entity e = l.getEntity(estatua);
            if (e != null) {
                e.discard();
            }
        }
        estatua = null;
    }

    // ------------------------------------------------------------------ las partes

    private static void luz(MinecraftServer s) {
        fase = Fase.LUZ;
        desde = t;
        Anuncio.sonido(s, SoundEvents.BEACON_ACTIVATE, 1.0F, 0.6F);
        Anuncio.sonido(s, SoundEvents.BELL_RESONATE, 0.8F, 0.7F);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_DESENLACE luz");
    }

    private static void creditos(MinecraftServer s) {
        fase = Fase.CREDITOS;
        desde = t;
        Evento ev = Evento.get(s);
        int caidos = 0;
        for (Pieza p : ev.piezas()) {
            if (!p.vivo()) {
                caidos++;
            }
        }
        // título (6 s) + "la colección" (2 s) + cada pieza (1.6 s) + el ganador (7 s) + Axelis (7 s) + gracias (5 s)
        creditos = (int) ((6 + 2 + caidos * 1.6 + 7 + 7 + 5) * 20);
        // todos de regreso a la mesa de juego (con la pantalla tapada no se ve): ahí es lo de después de los créditos
        BlockPos v = Complejo.pos(s, "v21.vitrina");
        if (v != null) {
            ServerLevel l = s.overworld();
            int i = 0;
            for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
                double x = v.getX() + 0.5 + (i % 5) - 2, z = v.getZ() + 12.5 + (i / 5);
                sp.teleportTo(l, x, v.getY() - 1, z, 180F, 0F);
                i++;
            }
            Entity e = ganador == null ? null : Evento.entidad(s, ev.pieza(ganador));
            if (e != null && !(e instanceof ServerPlayer)) {
                e.teleportTo(v.getX() + 0.5, v.getY() - 1, v.getZ() + 14.5);
            }
        }
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_DESENLACE creditos piezas={} dura={}s", ev.piezas().size(), creditos / 20);
    }

    private static void post(MinecraftServer s) {
        fase = Fase.POST;
        desde = t;
        hablo = false;
        BlockPos v = Complejo.pos(s, "v21.vitrina");
        ServerLevel l = s.overworld();
        if (v != null && ganador != null) {
            limpiarEstatua(s);
            Cadaver c = ModRegistry.CADAVER.get().create(l);
            if (c != null) {
                c.moveTo(v.getX() + 0.5, v.getY(), v.getZ() + 0.5, 0F, 0F);
                c.de(ganador, nombre, Cadaver.ESTATUA + Math.abs(ganador.hashCode()) % 3);
                c.setYRot(0F);
                c.setNoGravity(true);
                l.addFreshEntity(c);
                estatua = c.getUUID();
            }
            luzVitrina(s, true);
        }
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_DESENLACE post estatua={}", estatua != null);
    }

    private static void fin(MinecraftServer s) {
        fase = Fase.FIN;
        desde = t;
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_DESENLACE fin");
    }

    private static void luzVitrina(MinecraftServer s, boolean prender) {
        BlockPos p = Complejo.pos(s, "v21.vitrina.luz");
        if (p == null) {
            return;
        }
        ServerLevel l = s.overworld();
        if (prender) {
            l.setBlock(p, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 13), 2);
        } else if (l.getBlockState(p).is(Blocks.LIGHT)) {
            l.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    // ------------------------------------------------------------------ cada tick

    private static void tick(MinecraftServer s) {
        t++;
        if (fase == Fase.NADA) {
            return;
        }
        long dt = t - desde;
        switch (fase) {
            case CAMINA -> camina(s, dt);
            case LUZ -> {
                if (dt >= T_LUZ) {
                    creditos(s);
                }
            }
            case CREDITOS -> {
                if (dt >= creditos) {
                    post(s);
                }
            }
            case POST -> {
                if (!hablo && dt >= T_POST_HABLA) {
                    hablo = true;
                    if (!Pantallas.hablar(s, "desenlace", () -> {
                        if (fase == Fase.POST) {
                            fin(s);
                        }
                    })) {
                        fin(s);
                    }
                }
            }
            case FIN -> {
                if (dt >= T_FIN) {
                    fase = Fase.NADA;
                    enviar(s);
                }
            }
            default -> {
            }
        }
    }

    private static void camina(MinecraftServer s, long dt) {
        Evento ev = Evento.get(s);
        Pieza g = ganador == null ? null : ev.pieza(ganador);
        Entity e = g == null ? null : Evento.entidad(s, g);
        BlockPos luz = Complejo.pos(s, "sf.luz"), puerta = Complejo.pos(s, "salida.veintiuno.a");
        if (e == null || luz == null || puerta == null) {
            return;
        }
        if (!cruzo && e.getX() > puerta.getX() + 1) {
            cruzo = true;
            Coleccionista.LOGGER.info("COLECCIONISTA_DESENLACE cruzo");
        }
        if (e.getX() >= luz.getX() - 1) {
            luz(s);
            return;
        }
        // un bot que ganó camina solo: a la puerta y luego derecho por el pasillo
        if (e instanceof PathfinderMob m && dt % 20 == 0) {
            Vec3 a = cruzo ? new Vec3(luz.getX() + 4, luz.getY(), luz.getZ() + 1.0) : new Vec3(puerta.getX() + 2.5, puerta.getY(), puerta.getZ() + 1.0);
            m.getNavigation().moveTo(a.x, a.y, a.z, 0.8);
        }
        // los fantasmas que se quedaron muy lejos lo alcanzan (si no, no les llega el pasillo y la cámara no lo ve)
        if (dt % 20 == 0) {
            for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
                if (sp.isSpectator() && sp != e && sp.distanceTo(e) > 40) {
                    sp.teleportTo((ServerLevel) e.level(), e.getX() - 6, e.getY() + 1, e.getZ(), 270F, 10F);
                }
            }
        }
        if (dt % 40 == 0) {
            enviar(s);
        }
    }

    // ------------------------------------------------------------------ la flecha

    /** A dónde va el ganador: a la puerta del este y luego a la luz. Los demás, a ningún lado. */
    public static Objetivo.Meta objetivo(MinecraftServer s, ServerPlayer sp) {
        if (fase != Fase.CAMINA || !sp.getUUID().equals(ganador)) {
            return null;
        }
        if (!cruzo) {
            return Objetivo.Meta.en(Complejo.pos(s, "salida.veintiuno.a"), 1.5, MOD + ".obj.salida");
        }
        return Objetivo.Meta.en(Complejo.pos(s, "sf.luz"), 1.5, MOD + ".obj.luz");
    }

    // ------------------------------------------------------------------ a los clientes

    private static ListTag v(double x, double y, double z) {
        ListTag l = new ListTag();
        l.add(DoubleTag.valueOf(x));
        l.add(DoubleTag.valueOf(y));
        l.add(DoubleTag.valueOf(z));
        return l;
    }

    private static CompoundTag estado(MinecraftServer s) {
        CompoundTag d = new CompoundTag();
        d.putString("fase", fase.name());
        if (fase == Fase.NADA) {
            return d;
        }
        d.putLong("desde", desde);
        d.putInt("dt", (int) (t - desde));
        if (ganador != null) {
            d.putUUID("ganador", ganador);
        }
        d.putString("nombre", nombre);
        BlockPos luz = Complejo.pos(s, "sf.luz"), inicio = Complejo.pos(s, "sf.inicio"), puerta = Complejo.pos(s, "salida.veintiuno.a");
        if (luz != null && inicio != null && puerta != null) {
            d.put("luz", v(luz.getX() + 0.5, luz.getY(), luz.getZ() + 1.0));
            d.put("inicio", v(inicio.getX() + 0.5, inicio.getY(), inicio.getZ() + 1.0));
            d.put("puerta", v(puerta.getX() + 0.5, puerta.getY(), puerta.getZ() + 1.0));
        }
        BlockPos vi = Complejo.pos(s, "v21.vitrina");
        if (vi != null) {
            // la cámara de después de los créditos: de lejos, frente a la vitrina, se acerca despacio
            double cx = vi.getX() + 0.5, cz = vi.getZ() + 0.5;
            d.put("cam_a", v(cx + 1.2, vi.getY() + 1.6, cz + 7.5));
            d.put("cam_b", v(cx + 0.25, vi.getY() + 1.35, cz + 3.1));
            d.put("cam_mira", v(cx, vi.getY() + 1.35, cz));
        }
        if (fase == Fase.CREDITOS) {
            d.putInt("dura", creditos);
            d.put("piezas", piezas(s));
        }
        return d;
    }

    /** Las piezas para los créditos: primero las que cayeron (en el orden en que cayeron) y al final el ganador. */
    private static ListTag piezas(MinecraftServer s) {
        Evento ev = Evento.get(s);
        List<Pieza> l = new ArrayList<>(ev.piezas());
        l.sort((a, b) -> {
            int oa = a.vivo() ? Integer.MAX_VALUE : a.orden, ob = b.vivo() ? Integer.MAX_VALUE : b.orden;
            return Integer.compare(oa, ob);
        });
        ListTag out = new ListTag();
        for (Pieza p : l) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", p.id);
            c.putString("nombre", p.nombre);
            c.putString("etiqueta", p.etiqueta());
            c.putString("motivo", p.motivo == null ? "" : p.motivo);
            c.putInt("orden", p.orden);
            c.putBoolean("gano", p.vivo() && p.id.equals(ganador));
            c.putBoolean("bot", p.bot);
            out.add(c);
        }
        return out;
    }

    private static void enviar(MinecraftServer s) {
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }

    /** Al entrar alguien a la mitad, que le llegue en qué va. */
    public static void alEntrar(ServerPlayer sp) {
        if (fase != Fase.NADA) {
            PlayerStore.Sync.toPlayer(sp, KEY, estado(sp.server));
        }
    }

    public static String resumen() {
        return "desenlace fase=" + fase + " ganador=" + nombre + " cruzo=" + cruzo + " estatua=" + (estatua != null);
    }
}
