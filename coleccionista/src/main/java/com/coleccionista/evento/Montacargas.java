package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.fx.Fx;
import com.coleccionista.Coleccionista;
import com.coleccionista.mapa.Antesala;
import com.coleccionista.prueba.Escena;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * El montacargas del Coleccionista: cómo se va de una prueba a otra sin aparecer de la nada en otro lugar. Un gas dulce duerme a las
 * piezas donde estén; despiertan dentro de la jaula del montacargas, con la reja cerrada; el montacargas baja (temblor, cadenas, el foco
 * que falla, el Coleccionista en la pantallita anunciando la parada); se detiene de golpe, suena la campana y la reja se abre a la antesala
 * de la siguiente prueba. Los fantasmas aparecen en la antesala; al organizador no se le mueve.
 * <pre>
 *     /evento montacargas archivo        (o el botón "Montacargas" de cada prueba en el panel)
 * </pre>
 */
public final class Montacargas {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":montacargas";
    /** Duración de cada parte (ticks): el gas, el negro (ahí se mueve a todos), el viaje y la llegada (se abre la reja). */
    public static final int GAS = 70, NEGRO = 50, VIAJE = 220, LLEGADA = 50;

    private enum Fase { NADA, GAS, NEGRO, VIAJE, LLEGADA }

    private static Fase fase = Fase.NADA;
    private static String zona = "";
    private static int t;
    private static MinecraftServer servidor;
    private static final Random R = new Random();
    /** Quiénes viajan (las piezas vivas: personas y bots). */
    private static final List<java.util.UUID> VIAJAN = new ArrayList<>();

    private Montacargas() {
    }

    public static void init() {
        AxelisCore.onServerTick(Montacargas::tick);
        AxelisCore.onLogin(p -> PlayerStore.Sync.toPlayer(p, KEY, estado()));
    }

    public static boolean activo() {
        return fase != Fase.NADA;
    }

    /** ¿Esa zona tiene antesala? */
    public static boolean tieneAntesala(String z) {
        for (String a : Antesala.ZONAS) {
            if (a.equals(z)) {
                return true;
            }
        }
        return false;
    }

    /** El nombre de la zona para el chat ("el Archivo"). */
    public static Component nombre(String z) {
        return Component.translatable(MOD + ".mon.zona." + z);
    }

    /**
     * Manda a las piezas en el montacargas a la antesala de {@code z}. Devuelve null si arrancó, o el motivo por el que no se puede.
     */
    public static Component viajar(MinecraftServer s, String z) {
        if (!tieneAntesala(z)) {
            return Component.literal(z + "?");
        }
        // ahora el viaje es el del recorrido: en elevador a la sala de llegada de esa prueba, y de ahí sigue solo
        if (true) {
            return Recorrido.ir(s, prueba(z)) ? null : Component.translatable(MOD + ".cmd.recorrido_no");
        }
        if (activo()) {
            return Component.translatable(MOD + ".cmd.montacargas_ya");
        }
        if (!Complejo.construido(s, "ante_" + z)) {
            return Component.translatable(MOD + ".cmd.montacargas_no", nombre(z));
        }
        // la prueba de antes se cierra (sin eliminar a nadie) para que sus avisos no viajen con ellas
        com.coleccionista.prueba.Pruebas.cerrarOtras(s, prueba(z));
        servidor = s;
        zona = z;
        fase = Fase.GAS;
        t = 0;
        VIAJAN.clear();
        for (Pieza p : Evento.get(s).vivos()) {
            VIAJAN.add(p.id);
        }
        Pantallas.callar(s);
        Pantallas.modo(s, "coleccionista", "");
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".mon.chat.gas")));
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_MONTACARGAS gas {} piezas={}", z, VIAJAN.size());
        return null;
    }

    /** La prueba que se juega en la zona de esa parada. */
    static String prueba(String z) {
        return switch (z) {
            case "cloacas" -> "encadenados";
            case "archivo" -> "traidor";
            case "salon" -> "compartir";
            case "laberintos" -> "ciegas";
            case "capilla" -> "sacrificio";
            case "final" -> "copas";
            case "galeria" -> "tres";
            default -> z;
        };
    }

    private static void tick(MinecraftServer s) {
        if (fase == Fase.NADA || s != servidor) {
            return;
        }
        t++;
        switch (fase) {
            case GAS -> {
                // el gas: verde en las orillas, mareo y el siseo; luego todo a negro
                if (t % 10 == 1) {
                    for (ServerPlayer sp : viajeros(s)) {
                        Fx.vignette(sp, 0xC05A8A2A, 14);
                        Fx.shake(sp, 0.08F + t / (float) GAS * 0.25F, 12);
                    }
                }
                if (t == 2 || t == 26) {
                    sonido(s, SoundEvents.FIRE_EXTINGUISH, 0.9F, 0.5F);
                }
                if (t == 34 || t == 50) {
                    sonido(s, SoundEvents.PLAYER_HURT_DROWN, 0.7F, 0.8F + R.nextFloat() * 0.3F);
                }
                if (t == GAS - 14) {
                    Escena.fundido(s, MOD + ".mon.fundido", NEGRO + 34);
                }
                if (t >= GAS) {
                    llevar(s);
                    siguiente(s, Fase.NEGRO);
                }
            }
            case NEGRO -> {
                if (t >= NEGRO) {
                    siguiente(s, Fase.VIAJE);
                    // arranca: golpe, motor y cadenas; el foco empieza a fallar; el Coleccionista anuncia la parada
                    sonido(s, SoundEvents.ANVIL_PLACE, 0.8F, 0.5F);
                    sonido(s, SoundEvents.BEACON_ACTIVATE, 0.8F, 0.5F);
                    focos(s, true);
                    for (ServerPlayer sp : viajeros(s)) {
                        Fx.shake(sp, 0.7F, 8);
                    }
                    Pantallas.parada = zona;
                    Pantallas.hablar(s, "montacargas", null);
                }
            }
            case VIAJE -> {
                if (t % 20 == 1) {
                    for (ServerPlayer sp : viajeros(s)) {
                        Fx.shake(sp, 0.16F, 24);
                    }
                }
                if (t % 40 == 5) {
                    sonido(s, SoundEvents.BEACON_AMBIENT, 0.6F, 0.5F);
                }
                if (t % 27 == 13) {
                    sonido(s, R.nextBoolean() ? SoundEvents.CHAIN_STEP : SoundEvents.CHAIN_HIT, 0.9F, 0.5F + R.nextFloat() * 0.3F);
                }
                if (t % 53 == 30) {
                    sonido(s, SoundEvents.WOODEN_DOOR_OPEN, 0.5F, 0.4F);
                }
                if (t == VIAJE / 2) {
                    // un jalón a medio camino
                    sonido(s, SoundEvents.ANVIL_LAND, 0.5F, 0.6F);
                    for (ServerPlayer sp : viajeros(s)) {
                        Fx.shake(sp, 1.1F, 10);
                    }
                }
                if (t >= VIAJE) {
                    // se detiene de golpe y suena la campana
                    sonido(s, SoundEvents.ANVIL_LAND, 0.8F, 0.5F);
                    sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.8F);
                    focos(s, false);
                    for (ServerPlayer sp : viajeros(s)) {
                        Fx.shake(sp, 0.9F, 8);
                    }
                    siguiente(s, Fase.LLEGADA);
                }
            }
            case LLEGADA -> {
                if (t == 25) {
                    reja(s, true);
                    sonido(s, SoundEvents.IRON_DOOR_OPEN, 1F, 0.6F);
                }
                if (t >= LLEGADA) {
                    String z = zona;
                    fase = Fase.NADA;
                    enviar(s);
                    Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".mon.chat.llegada", nombre(z))));
                    Coleccionista.LOGGER.info("COLECCIONISTA_MONTACARGAS llegada {}", z);
                }
            }
            default -> {
            }
        }
    }

    private static void siguiente(MinecraftServer s, Fase f) {
        fase = f;
        t = 0;
        enviar(s);
    }

    /** En el negro: cierra la reja, y pone a cada pieza en un lugar de la jaula (los fantasmas, en la antesala). */
    private static void llevar(MinecraftServer s) {
        reja(s, false);
        Evento ev = Evento.get(s);
        ServerLevel l = s.overworld();
        List<Integer> lugares = new ArrayList<>();
        for (int i = 0; i < Antesala.LUGARES; i++) {
            lugares.add(i);
        }
        Collections.shuffle(lugares, R);
        int i = 0;
        for (java.util.UUID id : VIAJAN) {
            Pieza p = ev.pieza(id);
            Entity e = p == null ? null : Evento.entidad(s, p);
            if (e == null || !p.vivo()) {
                continue;
            }
            String m = "ante." + zona + ".lugar." + lugares.get(i++ % lugares.size());
            BlockPos b = Complejo.pos(s, m);
            if (b == null) {
                continue;
            }
            mover(l, e, Vec3.atBottomCenterOf(b), Complejo.yaw(m));
        }
        // los fantasmas (y quien no esté vivo) miran desde el salón de la antesala
        BlockPos centro = Complejo.pos(s, "ante." + zona + ".centro");
        if (centro != null) {
            for (Pieza p : ev.piezas()) {
                if (p.vivo() || p.bot) {
                    continue;
                }
                ServerPlayer sp = s.getPlayerList().getPlayer(p.id);
                if (sp != null) {
                    sp.teleportTo(l, centro.getX() + 0.5, centro.getY() + 1.5, centro.getZ() + 0.5, 90F, 0F);
                }
            }
        }
    }

    private static void mover(ServerLevel l, Entity e, Vec3 v, float yaw) {
        if (e instanceof ServerPlayer sp) {
            if (sp.isSleeping()) {
                sp.stopSleeping();
            }
            sp.teleportTo(l, v.x, v.y, v.z, yaw, 0F);
            if (!sp.isCreative() && !sp.isSpectator()) {
                sp.setGameMode(GameType.ADVENTURE);
            }
        } else {
            e.moveTo(v.x, v.y, v.z, yaw, 0F);
            e.setYHeadRot(yaw);
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** Abre o cierra la reja corrediza de la jaula (las dos hojas, con su sonido de riel). */
    private static void reja(MinecraftServer s, boolean abrir) {
        ServerLevel l = s.overworld();
        for (String h : new String[] {"a", "b"}) {
            BlockPos p = Complejo.pos(s, "ante." + zona + ".reja." + h);
            if (p == null) {
                continue;
            }
            for (BlockPos q : new BlockPos[] {p, p.above()}) {
                BlockState st = l.getBlockState(q);
                if (st.getBlock() instanceof DoorBlock && st.getValue(DoorBlock.OPEN) != abrir) {
                    l.setBlock(q, st.setValue(DoorBlock.OPEN, abrir), 2 | 16);
                }
            }
        }
    }

    /** Los focos de la jaula fallan (parpadean) durante el viaje. */
    private static void focos(MinecraftServer s, boolean fallan) {
        ServerLevel l = s.overworld();
        for (String h : new String[] {"a", "b"}) {
            BlockPos p = Complejo.pos(s, "ante." + zona + ".foco." + h);
            if (p == null) {
                continue;
            }
            BlockState st = l.getBlockState(p);
            Property<?> pr = st.getBlock().getStateDefinition().getProperty("falla");
            if (pr instanceof BooleanProperty b && st.getValue(b) != fallan) {
                l.setBlock(p, st.setValue(b, fallan), 2);
            }
        }
    }

    /** Las personas que viajan (conectadas). */
    private static List<ServerPlayer> viajeros(MinecraftServer s) {
        List<ServerPlayer> r = new ArrayList<>();
        for (java.util.UUID id : VIAJAN) {
            ServerPlayer sp = s.getPlayerList().getPlayer(id);
            if (sp != null) {
                r.add(sp);
            }
        }
        return r;
    }

    /** Un sonido para los que viajan (y para los fantasmas, que también van mirando). */
    private static void sonido(MinecraftServer s, SoundEvent e, float vol, float tono) {
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            Pieza p = Evento.get(s).pieza(sp.getUUID());
            if (p != null) {
                sp.playNotifySound(e, SoundSource.AMBIENT, vol, tono);
            }
        }
    }

    public static CompoundTag estado() {
        CompoundTag t = new CompoundTag();
        t.putString("fase", fase.name());
        t.putString("zona", zona);
        t.putInt("dura", switch (fase) {
            case GAS -> GAS;
            case NEGRO -> NEGRO;
            case VIAJE -> VIAJE;
            case LLEGADA -> LLEGADA;
            default -> 0;
        });
        return t;
    }

    private static void enviar(MinecraftServer s) {
        PlayerStore.Sync.toAll(s, KEY, estado());
    }

    /** Al reiniciar el evento: se detiene y abre la reja. */
    public static void reiniciar(MinecraftServer s) {
        if (fase != Fase.NADA) {
            focos(s, false);
            reja(s, true);
            fase = Fase.NADA;
            enviar(s);
        }
    }
}
