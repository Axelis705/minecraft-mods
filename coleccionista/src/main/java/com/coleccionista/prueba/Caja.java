package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Plat;
import com.axeliscore.vestir.Vestuario;
import com.coleccionista.Coleccionista;
import com.coleccionista.bot.PiezaBot;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.ArmaTecho;
import com.coleccionista.evento.Cabina;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Objetivo;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.evento.Silla;
import com.coleccionista.mapa.CajaMusica;
import com.coleccionista.mapa.Pasaje;
import com.coleccionista.registry.ModRegistry;
import com.coleccionista.voz.Voz;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Capítulo 3 · La caja de música (El traidor, rehecho). Llegan por el pasillo de servicio al salón del carrusel; el Coleccionista explica
 * y cada pieza recibe un símbolo en la espalda (pica, corazón, diamante o trébol: el suyo no lo puede ver) y su papel: hay dos traidores
 * que se conocen. Luego, en seis turnos con apagones entre ellos, se sientan de dos en dos en las góndolas del carrusel: el de atrás le ve
 * la espalda al de adelante y le dice (en voz alta: cada góndola es un grupo privado de voz, con la voz cambiada) qué símbolo lleva; los
 * traidores pueden mentir. Dos turnos por pareja (cambian de asiento) y tres parejas distintas.
 * <p>
 * Al final, otro apagón: todos aparecen sentados y atados en círculo en el escenario del centro, sin máscaras. Baja el arma del techo,
 * gira y apunta a cada pieza: dice su símbolo (y lo marca); el organizador (que ve el símbolo real) decide: <b>Bien</b> o <b>Bala</b>
 * ({@code /bala <pieza>}). Si no hay organizador (o con {@code auto}), decide solo según lo que marcó. Después, el traidor que no hizo caer
 * a ninguno de los que tuvo enfrente se lleva la última bala. Una sola vez: los que sobreviven, sobreviven.
 */
public final class Caja {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":caja";
    public static final String KEY_MIO = MOD + ":caja_mio";
    public static final String KEY_ORG = MOD + ":caja_org";
    public static final String ACCION = MOD + ":caja_accion";
    public static final ResourceLocation ETIQUETA = new ResourceLocation(MOD, "caja_etiqueta");
    public static final ResourceLocation MASCARA = new ResourceLocation(MOD, "caja_mascara");
    public static final String PRUEBA = "La caja de música";
    public static final String[] SIMBOLOS = {"pica", "corazon", "diamante", "trebol"};
    public static final int TURNOS = 6;
    /** Ajustes (los cambia el organizador con /evento caja ...). */
    public static int segundosTurno = 45, segundosElegir = 25, traidores = 2;
    /** Decide solo (según lo que marcó cada pieza) aunque haya organizador. */
    public static boolean auto;
    /** Para las pruebas: nunca decide sola (espera a /evento caja bien o bala). */
    public static boolean esperar;
    /** Para las pruebas: quiénes van primero en la revisión (en ese orden). */
    public static final List<UUID> primero = new ArrayList<>();
    /** Al terminar La mascota, pasan solos a la caja. */
    public static boolean encadenar = true;
    public static int esperaPasillo = 1200, esperaSalon = 900;
    /** Los tonos de voz para disfrazar (más grave o más aguda). */
    private static final float[] TONOS = {0.72F, 0.8F, 1.3F, 1.42F};

    public enum Fase {
        NADA, PASILLO, LLEGANDO, HABLANDO, ROLES, APAGON, TURNO, CIRCULO, REVISION, TRAIDORES, FIN
    }

    /** Lo que pasa con la pieza apuntada: el arma gira, espera la respuesta, o ejecuta lo que se decidió. */
    enum Paso {
        GIRAR, ESPERA, BIEN, BALA
    }

    private static Fase fase = Fase.NADA;
    /** Reloj propio (ticks, se detiene con la pausa del evento). */
    private static long t, desde;
    private static int turno = -1, apagones;
    private static long apagonMedio, apagonFin, turnoFin, susurroEn;
    private static Fase trasApagon = Fase.TURNO;
    private static final Random R = new Random();

    private static final Map<UUID, Integer> simbolo = new LinkedHashMap<>();
    private static final List<UUID> traidoresIds = new ArrayList<>();
    /** Las tres rondas: cada una, sus parejas ({a, b}; b null si va solo) y la góndola de cada pareja. */
    private static final List<List<UUID[]>> rondas = new ArrayList<>();
    private static final List<int[]> gondolasRonda = new ArrayList<>();
    /** Los asientos de las góndolas: [góndola][0 atrás, 1 adelante]. */
    private static final Silla[][] asientos = new Silla[CajaMusica.GONDOLAS][2];
    /** Dónde está cada pieza en este turno: {góndola, lado} (lado 0 atrás, 1 adelante, 2 solo). */
    private static final Map<UUID, int[]> lugar = new HashMap<>();
    /** Lo que le dijeron a cada pieza (para que los bots "decidan"; de los humanos se supone). */
    private static final Map<UUID, List<Integer>> dichos = new HashMap<>();
    /** A quién le dijo cada traidor (estando atrás). */
    private static final Map<UUID, Set<UUID>> oyentes = new HashMap<>();

    // la revisión
    private static final Map<UUID, Silla> circulo = new LinkedHashMap<>();
    private static final List<UUID> orden = new ArrayList<>();
    private static final List<UUID> pendientesTraidores = new ArrayList<>();
    private static int idx;
    private static Paso paso = Paso.GIRAR;
    private static long pasoDesde;
    private static final Map<UUID, Integer> eligio = new HashMap<>();
    private static final Set<UUID> fusilados = new HashSet<>();
    private static ArmaTecho arma;
    private static final List<BlockPos> sangre = new ArrayList<>();
    private static int balas, salvados;

    private Caja() {
    }

    public static void init() {
        AxelisCore.onServerTick(Caja::tick);
        AxelisCore.onLogin(p -> {
            PlayerStore.Sync.toPlayer(p, KEY, estado());
            if (fase != Fase.NADA) {
                mio(Plat.server(p), p);
                org(Plat.server(p));
            }
        });
        Actions.on(ACCION, Caja::accion);
        // en la caja nadie se lastima: aquí solo se muere de un balazo
        MinecraftForge.EVENT_BUS.addListener((LivingHurtEvent e) -> {
            if (fase != Fase.NADA && fase != Fase.PASILLO && e.getEntity() instanceof ServerPlayer) {
                e.setCanceled(true);
            }
        });
    }

    public static Fase fase() {
        return fase;
    }

    public static boolean abierta() {
        return fase != Fase.NADA;
    }

    /** ¿Las piezas están en la parte "a ciegas" (máscaras y sin nombres)? */
    public static boolean anonimo() {
        return fase == Fase.ROLES || fase == Fase.APAGON && trasApagon != Fase.CIRCULO || fase == Fase.TURNO;
    }

    // ------------------------------------------------------------------ del pasillo de la casa a la caja

    /** Al terminar La mascota: se abren las puertas del pasillo de la casa y el Coleccionista los llama. */
    public static Component llamar(MinecraftServer s) {
        if (!Complejo.construido(s, "caja")) {
            return Component.translatable(MOD + ".caja.sin_mapa");
        }
        if (!Complejo.construido(s, "mansion")) {
            return llegar(s);
        }
        Pruebas.cerrarOtras(s, "caja");
        limpiarTodo(s);
        puertas(s, "pas_man", true);
        fase = Fase.PASILLO;
        desde = t;
        Evento ev = Evento.get(s);
        ev.prueba(s, Component.translatable(MOD + ".caja.prueba_pasillo").getString());
        ev.reloj(s, 0);
        Anuncio.titulo(s, Component.empty(), Component.translatable(MOD + ".caja.al_pasillo"));
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA llamar");
        return null;
    }

    /** Los vivos pasan (sin que se note) del pasillo de la casa al de la caja y se abren las puertas al salón. */
    public static Component llegar(MinecraftServer s) {
        if (!Complejo.construido(s, "caja")) {
            return Component.translatable(MOD + ".caja.sin_mapa");
        }
        if (fase == Fase.NADA) {
            Pruebas.cerrarOtras(s, "caja");
            limpiarTodo(s);
        }
        Evento ev = Evento.get(s);
        List<Entity> enPasillo = new ArrayList<>(), fuera = new ArrayList<>();
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null) {
                continue;
            }
            if (Complejo.construido(s, "mansion") && Cabina.dentro(s, "pas_man", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                enPasillo.add(e);
            } else {
                fuera.add(e);
            }
        }
        if (!enPasillo.isEmpty()) {
            Cabina.trasladar(s, "pas_man", "pas_caj", enPasillo, List.of());
        }
        BlockPos c = Complejo.pos(s, "pas_caj.centro");
        int n = 0;
        for (Entity e : fuera) {
            if (c == null) {
                break;
            }
            ir(s, e, c.getX() + 0.5 - 1.5 + (n % 3) * 1.5, c.getY(), c.getZ() + 0.5 - 1 + (n / 3 % 4), 270F);
            n++;
        }
        puertas(s, "pas_man", false);
        puertas(s, "pas_caj", true);
        fase = Fase.LLEGANDO;
        desde = t;
        ev.prueba(s, Component.translatable(MOD + ".caja.prueba_llegar").getString());
        ev.reloj(s, 0);
        Pantallas.modo(s, "apagada", "");
        musica(s, 1.2F);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA llegan pasillo={} fuera={}", enPasillo.size(), fuera.size());
        return null;
    }

    private static void puertas(MinecraftServer s, String pasillo, boolean abrir) {
        puerta(s, pasillo + ".izq", abrir);
        puerta(s, pasillo + ".der", abrir);
    }

    private static void puerta(MinecraftServer s, String marca, boolean abrir) {
        ServerLevel l = s.overworld();
        for (String h : new String[] {".a", ".b"}) {
            BlockPos p = Complejo.pos(s, marca + h);
            if (p == null) {
                continue;
            }
            for (BlockPos q : new BlockPos[] {p, p.above()}) {
                BlockState st = l.getBlockState(q);
                if (st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
                        && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN) != abrir) {
                    l.setBlock(q, st.setValue(net.minecraft.world.level.block.DoorBlock.OPEN, abrir), 2 | 16);
                }
            }
        }
    }

    /** ¿Ya entró al salón (pasó las puertas del pasillo)? */
    private static boolean enSalon(MinecraftServer s, Entity e) {
        BlockPos o = Complejo.origen(s, "caja");
        if (o == null) {
            return false;
        }
        double x = e.getX() - o.getX(), z = e.getZ() - o.getZ();
        return x >= CajaMusica.HX0 && x <= CajaMusica.HX1 + 1 && z >= CajaMusica.HZ0 && z <= CajaMusica.HZ1 + 1;
    }

    // ------------------------------------------------------------------ el discurso y los papeles

    private static void hablar(MinecraftServer s) {
        fase = Fase.HABLANDO;
        desde = t;
        BlockPos c = Complejo.pos(s, "caja.entrada");
        for (Pieza p : Evento.get(s).vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null && c != null && !enSalon(s, e)) {
                ir(s, e, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 270F);
            }
        }
        puertas(s, "pas_caj", false);
        Escena.presentacion(s, "III", MOD + ".caja.titulo", MOD + ".caja.lema");
        Pantallas.soloGrande(s, true);
        Pantallas.hablar(s, "caja", () -> roles(s));
        Evento.get(s).prueba(s, PRUEBA);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA habla");
    }

    /** Cada pieza recibe su símbolo (en la espalda), su máscara y su papel; dos traidores (que se conocen). */
    public static void roles(MinecraftServer s) {
        if (fase != Fase.HABLANDO && fase != Fase.LLEGANDO) {
            return;
        }
        if (fase == Fase.HABLANDO) {
            Pantallas.callar(s);
        }
        Evento ev = Evento.get(s);
        List<Pieza> v = new ArrayList<>(ev.vivos());
        simbolo.clear();
        traidoresIds.clear();
        Collections.shuffle(v, R);
        for (Pieza p : v) {
            int k = R.nextInt(SIMBOLOS.length);
            simbolo.put(p.id, k);
            CompoundTag d = new CompoundTag();
            d.putInt("s", k);
            Vestuario.poner(s, p.id, ETIQUETA, d);
            Vestuario.poner(s, p.id, MASCARA, new CompoundTag());
        }
        int nt = v.size() >= 6 ? traidores : v.size() >= 3 ? 1 : 0;
        for (int i = 0; i < nt && i < v.size(); i++) {
            traidoresIds.add(v.get(i).id);
        }
        fase = Fase.ROLES;
        desde = t;
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".caja.pantalla.roles").getString());
        for (Pieza p : v) {
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                mio(s, sp);
                sp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.MASTER, 1F, 1F);
            }
        }
        org(s);
        enviar(s);
        StringBuilder sb = new StringBuilder();
        for (UUID id : traidoresIds) {
            Pieza p = ev.pieza(id);
            sb.append(p == null ? id : p.nombre).append(' ');
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA roles piezas={} traidores={} [{}]", v.size(), traidoresIds.size(), sb.toString().trim());
    }

    // ------------------------------------------------------------------ las parejas y los turnos

    /** Tres rondas de parejas sin repetir (si se puede); el impar va solo (cada vez uno distinto). */
    private static void parejas(List<UUID> ids) {
        rondas.clear();
        gondolasRonda.clear();
        Set<String> usadas = new HashSet<>();
        Set<UUID> yaSolos = new HashSet<>();
        for (int r = 0; r < 3; r++) {
            List<UUID[]> mejor = null;
            int peor = Integer.MAX_VALUE;
            for (int intento = 0; intento < 300 && peor > 0; intento++) {
                List<UUID> l = new ArrayList<>(ids);
                Collections.shuffle(l, R);
                // el que va solo: alguien que no haya ido solo
                if (l.size() % 2 == 1) {
                    for (int i = 0; i < l.size(); i++) {
                        if (!yaSolos.contains(l.get(i))) {
                            l.add(l.remove(i));
                            break;
                        }
                    }
                }
                List<UUID[]> ps = new ArrayList<>();
                int rep = 0;
                for (int i = 0; i + 1 < l.size(); i += 2) {
                    ps.add(new UUID[] {l.get(i), l.get(i + 1)});
                    if (usadas.contains(par(l.get(i), l.get(i + 1)))) {
                        rep++;
                    }
                }
                if (l.size() % 2 == 1) {
                    ps.add(new UUID[] {l.get(l.size() - 1), null});
                }
                if (rep < peor) {
                    peor = rep;
                    mejor = ps;
                }
            }
            if (mejor == null) {
                mejor = new ArrayList<>();
            }
            for (UUID[] p : mejor) {
                if (p[1] == null) {
                    yaSolos.add(p[0]);
                } else {
                    usadas.add(par(p[0], p[1]));
                }
            }
            rondas.add(mejor);
            // el carrusel "gira": cada ronda, otras góndolas
            List<Integer> gs = new ArrayList<>();
            for (int g = 0; g < CajaMusica.GONDOLAS; g++) {
                gs.add(g);
            }
            Collections.shuffle(gs, R);
            int[] asig = new int[mejor.size()];
            for (int i = 0; i < asig.length; i++) {
                asig[i] = i < gs.size() ? gs.get(i) : -1;
            }
            gondolasRonda.add(asig);
        }
    }

    private static String par(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    /** Se acabó la carta del papel: aparecen los asientos y, con el primer apagón, cada quien a su góndola. */
    private static void empezarTurnos(MinecraftServer s) {
        Evento ev = Evento.get(s);
        List<UUID> ids = new ArrayList<>();
        for (Pieza p : ev.vivos()) {
            if (simbolo.containsKey(p.id)) {
                ids.add(p.id);
            }
        }
        parejas(ids);
        dichos.clear();
        oyentes.clear();
        ServerLevel l = s.overworld();
        for (int g = 0; g < CajaMusica.GONDOLAS; g++) {
            for (int lado = 0; lado < 2; lado++) {
                BlockPos b = Complejo.pos(s, "caja.gon." + g + (lado == 0 ? ".atras" : ".frente"));
                Silla si = ModRegistry.SILLA.get().create(l);
                if (b == null || si == null) {
                    continue;
                }
                float yaw = Complejo.yaw("caja.gon." + g + (lado == 0 ? ".atras" : ".frente"));
                si.moveTo(b.getX() + 0.5, b.getY() + (lado == 0 ? 0.0 : 0.0), b.getZ() + 0.5, yaw, 0F);
                si.setYHeadRot(yaw);
                si.grupo = "caja";
                si.oculta(true);
                l.addFreshEntity(si);
                asientos[g][lado] = si;
            }
        }
        // las voces cambiadas (cada pieza la suya, toda la prueba)
        Map<UUID, Float> tonos = new HashMap<>();
        for (UUID id : ids) {
            tonos.put(id, TONOS[R.nextInt(TONOS.length)]);
        }
        Voz.tonos(tonos);
        turno = -1;
        apagar(s, Fase.TURNO);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA turnos piezas={} parejas={}", ids.size(), rondas.isEmpty() ? 0 : rondas.get(0).size());
    }

    /** Se apaga la luz: a la mitad del apagón se mueven las piezas (nadie ve nada) y al final empieza lo que sigue. */
    private static void apagar(MinecraftServer s, Fase despues) {
        fase = Fase.APAGON;
        trasApagon = despues;
        desde = t;
        apagones++;
        int dura = despues == Fase.CIRCULO ? 100 : 70;
        apagonMedio = t + dura / 2;
        apagonFin = t + dura;
        Evento.get(s).reloj(s, 0);
        // el mecanismo de la caja: se le da cuerda (clic, clic, clic) y la musiquita se tuerce
        for (Pieza p : Evento.get(s).vivos()) {
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                sp.playNotifySound(SoundEvents.LEVER_CLICK, SoundSource.MASTER, 0.9F, 0.5F);
                sp.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.MASTER, 0.6F, 0.5F);
            }
        }
        CompoundTag d = new CompoundTag();
        d.putInt("n", apagones);
        d.putInt("dura", dura);
        PlayerStore.Sync.toAll(s, KEY + "_apagon", d);
        enviar(s);
    }

    /** A la mitad del apagón: el siguiente turno (o el círculo). */
    private static void enLaOscuridad(MinecraftServer s) {
        if (trasApagon == Fase.CIRCULO) {
            alCirculo(s);
            return;
        }
        turno++;
        acomodar(s);
    }

    /** Sienta a cada quien en su lugar de este turno. */
    private static void acomodar(MinecraftServer s) {
        lugar.clear();
        int r = Math.min(2, turno / 2), h = turno % 2;
        List<UUID[]> ps = rondas.size() > r ? rondas.get(r) : List.of();
        int[] gs = gondolasRonda.size() > r ? gondolasRonda.get(r) : new int[0];
        Map<UUID, Integer> grupos = new HashMap<>();
        for (int i = 0; i < ps.size(); i++) {
            UUID[] par = ps.get(i);
            int g = i < gs.length ? gs[i] : -1;
            if (g < 0) {
                continue;
            }
            if (par[1] == null) {
                lugar.put(par[0], new int[] {g, 2});
            } else {
                UUID frente = h == 0 ? par[0] : par[1], atras = h == 0 ? par[1] : par[0];
                lugar.put(frente, new int[] {g, 1});
                lugar.put(atras, new int[] {g, 0});
            }
            grupos.put(par[0], g + 1);
            if (par[1] != null) {
                grupos.put(par[1], g + 1);
            }
        }
        Voz.separar(grupos);
        for (Silla[] gg : asientos) {
            for (Silla si : gg) {
                if (si != null) {
                    si.soltar();
                    si.cerrar(false, false);
                }
            }
        }
        Evento ev = Evento.get(s);
        for (Map.Entry<UUID, int[]> e : lugar.entrySet()) {
            Pieza p = ev.pieza(e.getKey());
            Entity ent = p == null ? null : Evento.entidad(s, p);
            if (ent != null) {
                sentar(s, ent, asiento(e.getValue()));
            }
        }
        for (Silla[] gg : asientos) {
            for (Silla si : gg) {
                if (si != null && si.isVehicle()) {
                    si.cerrar(true, false);
                }
            }
        }
        for (Pieza p : ev.vivos()) {
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                mio(s, sp);
            }
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA turno {} ronda={} sentados={}", turno + 1, r + 1, lugar.size());
    }

    private static Silla asiento(int[] lu) {
        int g = lu[0], lado = lu[1] == 0 ? 0 : 1;
        return g >= 0 && g < asientos.length ? asientos[g][lado] : null;
    }

    private static void sentar(MinecraftServer s, Entity e, Silla si) {
        if (si == null || !si.isAlive()) {
            return;
        }
        if (e.getVehicle() == si) {
            return;
        }
        if (e.getVehicle() instanceof Silla vieja) {
            vieja.soltar();
        }
        e.stopRiding();
        e.fallDistance = 0;
        if (e instanceof ServerPlayer sp) {
            sp.teleportTo(s.overworld(), si.getX(), si.getY() + 0.3, si.getZ(), si.getYRot(), 0F);
        } else {
            if (e instanceof PiezaBot b) {
                b.getNavigation().stop();
            }
            e.moveTo(si.getX(), si.getY() + 0.3, si.getZ(), si.getYRot(), 0F);
        }
        e.startRiding(si, true);
    }

    /** Se prende la luz: empieza el turno. */
    private static void empezarTurno(MinecraftServer s) {
        fase = Fase.TURNO;
        desde = t;
        turnoFin = t + segundosTurno * 20L;
        susurroEn = t + 50 + R.nextInt(80);
        Evento ev = Evento.get(s);
        ev.reloj(s, segundosTurno);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".caja.pantalla.turno", turno + 1, TURNOS).getString());
        // quién le dice a quién (el de atrás al de adelante)
        for (Map.Entry<UUID, int[]> e : lugar.entrySet()) {
            if (e.getValue()[1] != 0) {
                continue;
            }
            UUID atras = e.getKey(), frente = quienEnfrente(e.getValue()[0]);
            if (frente == null) {
                continue;
            }
            if (traidoresIds.contains(atras)) {
                oyentes.computeIfAbsent(atras, k -> new HashSet<>()).add(frente);
            }
            // lo que "le dijo" (de los bots es exacto; de las personas se supone: un traidor miente)
            Integer real = simbolo.get(frente);
            if (real != null) {
                int dicho = traidoresIds.contains(atras) ? otroSimbolo(real) : real;
                dichos.computeIfAbsent(frente, k -> new ArrayList<>()).add(dicho);
            }
        }
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA luz turno={}", turno + 1);
    }

    private static int otroSimbolo(int real) {
        int k = R.nextInt(SIMBOLOS.length - 1);
        return k >= real ? k + 1 : k;
    }

    private static UUID quienEnfrente(int g) {
        for (Map.Entry<UUID, int[]> e : lugar.entrySet()) {
            if (e.getValue()[0] == g && e.getValue()[1] == 1) {
                return e.getKey();
            }
        }
        return null;
    }

    private static UUID quienAtras(int g) {
        for (Map.Entry<UUID, int[]> e : lugar.entrySet()) {
            if (e.getValue()[0] == g && e.getValue()[1] == 0) {
                return e.getKey();
            }
        }
        return null;
    }

    /** Los bots que van atrás le "susurran" al de adelante (si es persona) el símbolo que le ven (o uno falso, si son traidores). */
    private static void susurrar(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Map.Entry<UUID, int[]> e : lugar.entrySet()) {
            if (e.getValue()[1] != 0) {
                continue;
            }
            Pieza atras = ev.pieza(e.getKey());
            UUID fid = quienEnfrente(e.getValue()[0]);
            Pieza frente = fid == null ? null : ev.pieza(fid);
            if (atras == null || !atras.bot || frente == null || !(Evento.entidad(s, frente) instanceof ServerPlayer sp)) {
                continue;
            }
            List<Integer> l = dichos.get(fid);
            int dicho = l == null || l.isEmpty() ? simbolo.getOrDefault(fid, 0) : l.get(l.size() - 1);
            Component sim = Component.translatable(MOD + ".caja.simbolo." + SIMBOLOS[dicho]);
            Plat.msg(sp, Component.translatable(MOD + ".caja.susurro", sim).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            sp.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1F, 1.4F);
            CompoundTag d = new CompoundTag();
            d.putString("simbolo", SIMBOLOS[dicho]);
            d.putInt("n", (int) t);
            PlayerStore.Sync.toPlayer(sp, KEY + "_susurro", d);
        }
    }

    /** Se acabó el tiempo del turno: apagón y al siguiente (o al círculo). */
    private static void finTurno(MinecraftServer s) {
        if (turno + 1 < TURNOS) {
            apagar(s, Fase.TURNO);
        } else {
            apagar(s, Fase.CIRCULO);
        }
    }

    // ------------------------------------------------------------------ el círculo y el arma

    private static void alCirculo(MinecraftServer s) {
        Evento ev = Evento.get(s);
        ServerLevel l = s.overworld();
        // fuera las góndolas, las máscaras y las voces raras
        for (Silla[] gg : asientos) {
            for (int i = 0; i < gg.length; i++) {
                if (gg[i] != null) {
                    gg[i].soltar();
                    gg[i].discard();
                    gg[i] = null;
                }
            }
        }
        lugar.clear();
        Voz.juntar();
        Voz.tonos(Map.of());
        Vestuario.quitarDeTodos(s, MASCARA);
        BlockPos c = Complejo.pos(s, "caja.escenario");
        List<Pieza> v = new ArrayList<>();
        for (Pieza p : ev.vivos()) {
            if (simbolo.containsKey(p.id)) {
                v.add(p);
            }
        }
        Collections.shuffle(v, R);
        circulo.clear();
        orden.clear();
        int n = v.size();
        for (int i = 0; i < n && c != null; i++) {
            double a = Math.PI * 2 * i / Math.max(1, n);
            double x = c.getX() + 0.5 + Math.sin(a) * CajaMusica.RADIO_CIRCULO, z = c.getZ() + 0.5 + Math.cos(a) * CajaMusica.RADIO_CIRCULO;
            // mirando al centro
            float yaw = (float) Math.toDegrees(Math.atan2(-(c.getX() + 0.5 - x), c.getZ() + 0.5 - z));
            Silla si = ModRegistry.SILLA.get().create(l);
            if (si == null) {
                continue;
            }
            si.moveTo(x, c.getY(), z, yaw, 0F);
            si.setYHeadRot(yaw);
            si.grupo = "caja";
            l.addFreshEntity(si);
            Pieza p = v.get(i);
            circulo.put(p.id, si);
            orden.add(p.id);
            Entity e = Evento.entidad(s, p);
            if (e != null) {
                sentar(s, e, si);
            }
            si.cerrar(true, false);
        }
        // el arma: aparece arriba, metida en el techo
        BlockPos ap = Complejo.pos(s, "caja.arma");
        if (ap != null) {
            ArmaTecho a = ModRegistry.ARMA.get().create(l);
            if (a != null) {
                a.moveTo(ap.getX() + 0.5, ap.getY() + 1.0, ap.getZ() + 0.5, 0F, 0F);
                l.addFreshEntity(a);
                arma = a;
            }
        }
        for (Pieza p : ev.vivos()) {
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                mio(s, sp);
            }
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA circulo piezas={}", circulo.size());
    }

    /** Se prende la luz en el escenario: el Coleccionista habla y baja el arma. */
    private static void enCirculo(MinecraftServer s) {
        fase = Fase.CIRCULO;
        desde = t;
        Pantallas.modo(s, "apagada", "");
        Pantallas.hablar(s, "caja_revision", () -> {
            if (fase == Fase.CIRCULO) {
                revisar(s);
            }
        });
        if (arma != null) {
            arma.bajar();
        }
        enviar(s);
    }

    private static void revisar(MinecraftServer s) {
        fase = Fase.REVISION;
        desde = t;
        idx = -1;
        for (int i = primero.size() - 1; i >= 0; i--) {
            if (orden.remove(primero.get(i))) {
                orden.add(0, primero.get(i));
            }
        }
        primero.clear();
        Evento.get(s).prueba(s, Component.translatable(MOD + ".caja.prueba_revision").getString());
        siguiente(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA revision piezas={}", orden.size());
    }

    /** La siguiente pieza del círculo (o los traidores, o el final). */
    private static void siguiente(MinecraftServer s) {
        Evento ev = Evento.get(s);
        while (true) {
            idx++;
            List<UUID> l = fase == Fase.TRAIDORES ? pendientesTraidores : orden;
            if (idx >= l.size()) {
                if (fase == Fase.REVISION) {
                    aTraidores(s);
                } else {
                    terminar(s);
                }
                return;
            }
            Pieza p = ev.pieza(l.get(idx));
            if (p != null && p.vivo()) {
                break;
            }
        }
        paso = Paso.GIRAR;
        pasoDesde = t;
        UUID id = apuntado();
        Entity e = id == null ? null : Evento.entidad(s, ev.pieza(id));
        if (arma != null && e != null) {
            arma.apuntarA(e.getEyePosition());
        }
        enviar(s);
        org(s);
    }

    /** La pieza a la que apunta el arma ahora (null: a nadie). */
    public static UUID apuntado() {
        List<UUID> l = fase == Fase.TRAIDORES ? pendientesTraidores : fase == Fase.REVISION ? orden : null;
        return l == null || idx < 0 || idx >= l.size() ? null : l.get(idx);
    }

    /** ¿Hay alguien que decida (un organizador conectado que no está jugando)? */
    private static boolean hayJuez(MinecraftServer s) {
        if (auto) {
            return false;
        }
        Evento ev = Evento.get(s);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            Pieza pz = ev.pieza(p);
            if ((pz == null || !pz.vivo()) && (ev.esOrganizador(p.getUUID()) || Plat.isOp(p))) {
                return true;
            }
        }
        return false;
    }

    /** Lo que decidió el organizador (o la prueba sola) para la pieza apuntada. */
    public static boolean decidir(MinecraftServer s, boolean bala) {
        if (fase != Fase.REVISION && fase != Fase.TRAIDORES || paso != Paso.ESPERA && paso != Paso.GIRAR) {
            return false;
        }
        UUID id = apuntado();
        if (id == null) {
            return false;
        }
        paso = bala ? Paso.BALA : Paso.BIEN;
        pasoDesde = t;
        Evento ev = Evento.get(s);
        Pieza p = ev.pieza(id);
        if (bala) {
            // la tensión: el martillo se echa atrás y se oye el corazón
            sonidoArma(s, SoundEvents.CROSSBOW_LOADING_END, 1.2F, 0.6F);
            for (Pieza v : ev.vivos()) {
                if (Evento.entidad(s, v) instanceof ServerPlayer sp) {
                    sp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.MASTER, 1F, 1F);
                }
            }
        } else {
            sonidoArma(s, SoundEvents.DISPENSER_FAIL, 1.4F, 1.6F);
            if (p != null && Evento.entidad(s, p) instanceof ServerPlayer sp) {
                Plat.msg(sp, Component.translatable(MOD + ".caja.bien").withStyle(ChatFormatting.GREEN), true);
            }
        }
        sinElegir(s, id);
        enviar(s);
        org(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA decide {} {}", p == null ? id : p.nombre, bala ? "bala" : "bien");
        return true;
    }

    /** {@code /bala <pieza>}: si es a la que apunta el arma, bala; si no, avisa a quién apunta. */
    public static Component bala(MinecraftServer s, Pieza p) {
        UUID id = apuntado();
        if (id == null) {
            return Component.translatable(MOD + ".caja.cmd.nadie");
        }
        if (p != null && !p.id.equals(id)) {
            Pieza a = Evento.get(s).pieza(id);
            return Component.translatable(MOD + ".caja.cmd.apunta_a", a == null ? "?" : a.nombre);
        }
        return decidir(s, true) ? null : Component.translatable(MOD + ".caja.cmd.espera");
    }

    private static void sinElegir(MinecraftServer s, UUID id) {
        Pieza p = Evento.get(s).pieza(id);
        if (p != null && Evento.entidad(s, p) instanceof ServerPlayer sp) {
            mio(s, sp);
        }
    }

    /** ¡Bang! Fogonazo, sangre, la pieza cae (si no se trabó el arma por ser de las últimas). */
    private static void disparar(MinecraftServer s, UUID id) {
        Evento ev = Evento.get(s);
        Pieza p = ev.pieza(id);
        Entity e = p == null ? null : Evento.entidad(s, p);
        ServerLevel l = s.overworld();
        boolean traidor = traidoresIds.contains(id);
        String motivo = Component.translatable(MOD + (traidor ? ".caja.motivo_traidor" : ".caja.motivo")).getString();
        if (p != null && ev.eliminarEnPrueba(s, p, motivo)) {
            balas++;
            fusilados.add(id);
            if (arma != null) {
                arma.disparar();
                Vec3 m = arma.punta();
                l.sendParticles(ParticleTypes.FLASH, m.x, m.y, m.z, 1, 0, 0, 0, 0);
                l.sendParticles(ParticleTypes.LARGE_SMOKE, m.x, m.y, m.z, 8, 0.1, 0.1, 0.1, 0.02);
            }
            sonidoArma(s, SoundEvents.GENERIC_EXPLODE, 2.2F, 1.9F);
            sonidoArma(s, SoundEvents.FIREWORK_ROCKET_BLAST, 2.5F, 0.5F);
            sonidoArma(s, SoundEvents.TRIDENT_HIT, 1.5F, 0.6F);
            if (e != null) {
                Vec3 c = e.getEyePosition();
                DustParticleOptions rojo = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.03F), 1.6F);
                DustParticleOptions oscuro = new DustParticleOptions(new Vector3f(0.32F, 0.0F, 0.02F), 2.2F);
                l.sendParticles(rojo, c.x, c.y - 0.1, c.z, 70, 0.35, 0.35, 0.35, 0.05);
                l.sendParticles(oscuro, c.x, c.y - 0.6, c.z, 40, 0.5, 0.4, 0.5, 0.02);
                l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), c.x, c.y - 0.3, c.z, 50,
                        0.3, 0.3, 0.3, 0.25);
                mancharPiso(s, e.blockPosition());
            }
            for (Pieza v : ev.vivos()) {
                if (Evento.entidad(s, v) instanceof ServerPlayer sp) {
                    Fx.shake(sp, 1.2F, 12);
                }
            }
            for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
                if (e != null && sp.distanceToSqr(e) < 40 * 40) {
                    Fx.flash(sp, 0x30FFE8B0, 3);
                }
            }
            Coleccionista.LOGGER.info("COLECCIONISTA_CAJA bang {} {}{}", p.etiqueta(), p.nombre, traidor ? " (traidor)" : "");
        } else {
            // se trabó: quedan muy pocos para lo que sigue (o ya no estaba)
            salvados++;
            sonidoArma(s, SoundEvents.DISPENSER_FAIL, 1.6F, 0.7F);
            sonidoArma(s, SoundEvents.CHAIN_BREAK, 1.2F, 0.6F);
            if (e instanceof ServerPlayer sp) {
                Plat.msg(sp, Component.translatable(MOD + ".caja.trabo").withStyle(ChatFormatting.GOLD), false);
            }
            Coleccionista.LOGGER.info("COLECCIONISTA_CAJA trabo {}", p == null ? id : p.nombre);
        }
    }

    /** Unas manchas rojas en el piso detrás de la silla (se quitan al limpiar). */
    private static void mancharPiso(MinecraftServer s, BlockPos b) {
        ServerLevel l = s.overworld();
        BlockState st = Complejo.estado("coleccionista:sangre[facing=" + new String[] {"north", "east", "south", "west"}[R.nextInt(4)] + "]");
        if (st == null) {
            return;
        }
        BlockPos c = Complejo.pos(s, "caja.escenario");
        for (int i = 0; i < 3; i++) {
            BlockPos q = i == 0 ? b : b.offset(R.nextInt(3) - 1, 0, R.nextInt(3) - 1);
            if (c != null && i > 0) {
                // un poco hacia afuera del círculo (detrás de la silla)
                double dx = q.getX() - c.getX(), dz = q.getZ() - c.getZ();
                q = q.offset((int) Math.signum(dx), 0, (int) Math.signum(dz));
            }
            if (l.getBlockState(q).isAir() && l.getBlockState(q.below()).isSolidRender(l, q.below())) {
                l.setBlock(q, st, 2);
                sangre.add(q);
            }
        }
    }

    private static void sonidoArma(MinecraftServer s, net.minecraft.sounds.SoundEvent e, float vol, float tono) {
        BlockPos p = arma != null ? BlockPos.containing(arma.canon()) : Complejo.pos(s, "caja.escenario");
        if (p != null) {
            s.overworld().playSound(null, p, e, SoundSource.HOSTILE, vol, tono);
        }
    }

    /** Se revisó a todos: ahora los traidores que no engañaron a nadie. */
    private static void aTraidores(MinecraftServer s) {
        Evento ev = Evento.get(s);
        pendientesTraidores.clear();
        for (UUID tr : traidoresIds) {
            Pieza p = ev.pieza(tr);
            if (p == null || !p.vivo()) {
                continue;
            }
            boolean engano = false;
            for (UUID o : oyentes.getOrDefault(tr, Set.of())) {
                if (fusilados.contains(o)) {
                    engano = true;
                    break;
                }
            }
            if (!engano) {
                pendientesTraidores.add(tr);
            }
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA traidores sin_enganar={}", pendientesTraidores.size());
        if (pendientesTraidores.isEmpty()) {
            terminar(s);
            return;
        }
        fase = Fase.TRAIDORES;
        idx = -1;
        desde = t;
        paso = Paso.GIRAR;
        pasoDesde = t + 100000;
        if (arma != null) {
            BlockPos c = Complejo.pos(s, "caja.escenario");
            if (c != null) {
                arma.apuntar(arma.getYRot() + 180F, 20F);
            }
        }
        enviar(s);
        org(s);
        Pantallas.hablar(s, "caja_traidores", () -> {
            if (fase == Fase.TRAIDORES) {
                siguiente(s);
            }
        });
    }

    /** Se acabó: sube el arma, se dicen los traidores, se sueltan las correas. */
    public static boolean terminar(MinecraftServer s) {
        if (fase == Fase.NADA || fase == Fase.FIN) {
            return false;
        }
        Pantallas.callar(s);
        fase = Fase.FIN;
        desde = t;
        Evento ev = Evento.get(s);
        ev.reloj(s, 0);
        if (arma != null) {
            arma.subir();
        }
        for (Silla si : circulo.values()) {
            si.cerrar(false);
        }
        Voz.juntar();
        Voz.tonos(Map.of());
        Vestuario.quitarDeTodos(s, MASCARA);
        StringBuilder nombres = new StringBuilder();
        for (UUID id : traidoresIds) {
            Pieza p = ev.pieza(id);
            if (p != null) {
                nombres.append(nombres.length() > 0 ? " · " : "").append(p.nombre);
            }
        }
        // a cada quien, cuál era su símbolo (ya no importa: la música se acabó)
        for (Map.Entry<UUID, Integer> e : simbolo.entrySet()) {
            Pieza p = ev.pieza(e.getKey());
            if (p != null && Evento.entidad(s, p) instanceof ServerPlayer sp) {
                Plat.msg(sp, Component.translatable(MOD + ".caja.era", Component.translatable(MOD + ".caja.simbolo." + SIMBOLOS[e.getValue()]))
                        .withStyle(ChatFormatting.GRAY), false);
            }
        }
        String tr = nombres.toString();
        Pantallas.hablar(s, "caja_fin", () -> {
            Evento.get(s).prueba(s, Component.translatable(MOD + ".caja.fin_capitulo").getString());
            Coleccionista.LOGGER.info("COLECCIONISTA_CAPITULO3 fin vivos={}", Evento.get(s).vivos().size());
        });
        Anuncio.titulo(s, Component.translatable(MOD + ".caja.fin.titulo"), Component.translatable(MOD + ".caja.fin.traidores", tr));
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".caja.fin.traidores", tr)));
        for (Pieza p : ev.piezas()) {
            if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                mio(s, sp);
            }
        }
        enviar(s);
        org(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAJA fin balas={} salvados={} vivos={} traidores={}", balas, salvados, ev.vivos().size(), tr);
        return true;
    }

    // ------------------------------------------------------------------ cada tick

    private static void tick(MinecraftServer s) {
        if (fase == Fase.NADA) {
            return;
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        t++;
        switch (fase) {
            case PASILLO -> {
                if (t % 10 == 0) {
                    boolean todos = true;
                    int n = 0;
                    for (Pieza p : ev.vivos()) {
                        Entity e = Evento.entidad(s, p);
                        if (e == null) {
                            continue;
                        }
                        n++;
                        boolean dentro = Cabina.dentro(s, "pas_man", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO);
                        // los bots (solo en las pruebas) caminan solos
                        if (p.bot && !dentro && t - desde >= 60) {
                            BlockPos c = Complejo.pos(s, "pas_man.centro");
                            if (c != null) {
                                ir(s, e, c.getX() + 0.5 + R.nextInt(3) - 1, c.getY(), c.getZ() + 0.5 + R.nextInt(3) - 1, 90F);
                                dentro = true;
                            }
                        }
                        todos &= dentro;
                    }
                    if (n > 0 && todos || t - desde >= esperaPasillo) {
                        // los que se quedaron afuera llegan con los demás; se cierra la puerta y pasan
                        BlockPos c = Complejo.pos(s, "pas_man.centro");
                        for (Pieza p : ev.vivos()) {
                            Entity e = Evento.entidad(s, p);
                            if (e != null && c != null && !Cabina.dentro(s, "pas_man", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                                ir(s, e, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 90F);
                            }
                        }
                        puertas(s, "pas_man", false);
                        fase = Fase.LLEGANDO;
                        desde = t + 1000000; // espera a que se cierren las puertas
                        apagonFin = t + 50;
                    }
                }
                break;
            }
            case LLEGANDO -> {
                if (desde > t && t >= apagonFin) {
                    llegar(s);
                    break;
                }
                if (desde <= t && t % 10 == 0) {
                    boolean todos = true;
                    int n = 0;
                    for (Pieza p : ev.vivos()) {
                        Entity e = Evento.entidad(s, p);
                        if (e == null) {
                            continue;
                        }
                        n++;
                        if (p.bot && !enSalon(s, e) && t - desde >= 60) {
                            BlockPos c = Complejo.pos(s, "caja.entrada");
                            if (c != null) {
                                ir(s, e, c.getX() + 0.5 + R.nextInt(3), c.getY(), c.getZ() + 0.5 - 2 + R.nextInt(5), 270F);
                            }
                        }
                        todos &= enSalon(s, e);
                    }
                    if (n > 0 && todos || t - desde >= esperaSalon) {
                        hablar(s);
                    }
                }
            }
            case ROLES -> {
                if (t - desde >= 170) {
                    empezarTurnos(s);
                }
            }
            case APAGON -> {
                if (t == apagonMedio) {
                    enLaOscuridad(s);
                }
                if (t >= apagonFin) {
                    if (trasApagon == Fase.CIRCULO) {
                        enCirculo(s);
                    } else {
                        empezarTurno(s);
                    }
                }
            }
            case TURNO -> {
                if (t == susurroEn) {
                    susurrar(s);
                }
                if (t % 20 == 0) {
                    vigilarAsientos(s);
                }
                int resta = (int) ((turnoFin - t) / 20);
                if (resta <= 5 && resta >= 0 && (turnoFin - t) % 20 == 0) {
                    for (Pieza p : ev.vivos()) {
                        if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                            sp.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.MASTER, 0.5F, 0.5F + 0.1F * resta);
                        }
                    }
                }
                if (t >= turnoFin) {
                    finTurno(s);
                }
            }
            case CIRCULO -> {
                if (t % 20 == 0) {
                    vigilarCirculo(s);
                }
            }
            case REVISION, TRAIDORES -> {
                if (t % 20 == 0) {
                    vigilarCirculo(s);
                }
                revisionTick(s);
            }
            case FIN -> {
                if (arma != null && arma.estado() == 0 && t - desde > ArmaTecho.TICKS_SUBIR + 10) {
                    arma.discard();
                    arma = null;
                }
            }
            default -> {
            }
        }
        if (t % 40 == 0) {
            enviar(s);
        }
    }

    private static void revisionTick(MinecraftServer s) {
        UUID id = apuntado();
        if (id == null) {
            return;
        }
        long dt = t - pasoDesde;
        switch (paso) {
            case GIRAR -> {
                boolean listo = arma == null || arma.abajo() && arma.apuntando();
                if (listo && dt >= 20) {
                    paso = Paso.ESPERA;
                    pasoDesde = t;
                    eligio.remove(id);
                    Pieza p = Evento.get(s).pieza(id);
                    if (p != null && Evento.entidad(s, p) instanceof ServerPlayer sp) {
                        mio(s, sp);
                        sp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.MASTER, 1F, 1.2F);
                    }
                    if (p != null && p.bot && fase == Fase.REVISION) {
                        eligio.put(id, respuestaBot(id));
                    }
                    enviar(s);
                    org(s);
                }
            }
            case ESPERA -> {
                // sin juez: decide solo cuando ya marcó (o se le acabó el tiempo: callarse cuenta como error)
                if (!hayJuez(s) && !esperar) {
                    boolean traidor = traidoresIds.contains(id);
                    if (fase == Fase.TRAIDORES && dt >= 60) {
                        decidir(s, true);
                    } else if (fase == Fase.REVISION) {
                        Integer e = eligio.get(id);
                        if (e != null && dt >= 50 || dt >= segundosElegir * 20L) {
                            boolean bien = traidor || e != null && e.equals(simbolo.get(id));
                            decidir(s, !bien);
                        }
                    }
                } else if (t % 20 == 0) {
                    org(s);
                }
            }
            case BIEN -> {
                if (dt >= 45) {
                    siguiente(s);
                }
            }
            case BALA -> {
                if (dt == 45) {
                    disparar(s, id);
                }
                if (dt >= 100) {
                    siguiente(s);
                }
            }
            default -> {
            }
        }
    }

    /** Un bot "decide" su símbolo: el que más le dijeron (si no le dijeron nada, al azar). */
    private static int respuestaBot(UUID id) {
        List<Integer> l = dichos.getOrDefault(id, List.of());
        int[] c = new int[SIMBOLOS.length];
        for (int k : l) {
            c[k]++;
        }
        int mejor = R.nextInt(SIMBOLOS.length), n = -1;
        for (int k = 0; k < c.length; k++) {
            if (c[k] > n) {
                n = c[k];
                mejor = k;
            }
        }
        return n <= 0 ? R.nextInt(SIMBOLOS.length) : mejor;
    }

    /** Nadie se baja de su góndola (y quien se reconecta vuelve a su asiento). */
    private static void vigilarAsientos(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Map.Entry<UUID, int[]> e : lugar.entrySet()) {
            Pieza p = ev.pieza(e.getKey());
            Entity ent = p == null || !p.vivo() ? null : Evento.entidad(s, p);
            Silla si = asiento(e.getValue());
            if (ent != null && si != null && ent.getVehicle() != si) {
                sentar(s, ent, si);
                si.cerrar(true, false);
            }
        }
    }

    private static void vigilarCirculo(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Map.Entry<UUID, Silla> e : circulo.entrySet()) {
            Pieza p = ev.pieza(e.getKey());
            Entity ent = p == null || !p.vivo() ? null : Evento.entidad(s, p);
            if (ent != null && ent.getVehicle() != e.getValue() && e.getValue().isAlive()) {
                sentar(s, ent, e.getValue());
                e.getValue().cerrar(true, false);
            }
        }
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        if (fase == Fase.TURNO) {
            finTurno(s);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ lo que mandan los clientes

    private static void accion(ServerPlayer p, CompoundTag d) {
        MinecraftServer s = p.server;
        Evento ev = Evento.get(s);
        String que = d.getString("que");
        if ("simbolo".equals(que)) {
            UUID id = apuntado();
            if (id == null || !id.equals(p.getUUID()) || paso != Paso.ESPERA) {
                return;
            }
            int k = d.getInt("s");
            if (k < 0 || k >= SIMBOLOS.length) {
                return;
            }
            eligio.put(id, k);
            mio(s, p);
            org(s);
            Coleccionista.LOGGER.info("COLECCIONISTA_CAJA marca {} {}", p.getGameProfile().getName(), SIMBOLOS[k]);
            return;
        }
        // lo demás, solo el organizador (que no esté jugando)
        Pieza pz = ev.pieza(p);
        if (pz != null && pz.vivo() || !(ev.esOrganizador(p.getUUID()) || Plat.isOp(p))) {
            return;
        }
        if ("bien".equals(que)) {
            decidir(s, false);
        } else if ("bala".equals(que)) {
            decidir(s, true);
        } else if ("auto".equals(que)) {
            auto = d.getBoolean("si");
            org(s);
        }
    }

    // ------------------------------------------------------------------ el estado para los clientes

    public static CompoundTag estado() {
        CompoundTag t0 = new CompoundTag();
        t0.putString("fase", fase.name());
        t0.putInt("turno", turno + 1);
        t0.putInt("turnos", TURNOS);
        t0.putBoolean("anonimo", anonimo());
        UUID a = apuntado();
        if (a != null) {
            t0.putUUID("apuntado", a);
            t0.putString("paso", paso.name());
        }
        return t0;
    }

    private static void enviar(MinecraftServer s) {
        PlayerStore.Sync.toAll(s, KEY, estado());
    }

    /** Lo de cada pieza: su papel, con quién (si es traidor), dónde va sentada y si le toca decir su símbolo. */
    private static void mio(MinecraftServer s, ServerPlayer sp) {
        CompoundTag d = new CompoundTag();
        if (fase != Fase.NADA && simbolo.containsKey(sp.getUUID())) {
            boolean tr = traidoresIds.contains(sp.getUUID());
            d.putString("rol", tr ? "traidor" : "inocente");
            if (tr) {
                StringBuilder otros = new StringBuilder();
                for (UUID o : traidoresIds) {
                    if (!o.equals(sp.getUUID())) {
                        Pieza p = Evento.get(s).pieza(o);
                        otros.append(otros.length() > 0 ? ", " : "").append(p == null ? "?" : p.nombre);
                    }
                }
                d.putString("otro", otros.toString());
            }
            int[] lu = lugar.get(sp.getUUID());
            if (lu != null) {
                d.putInt("gondola", lu[0] + 1);
                d.putString("lado", lu[1] == 0 ? "atras" : lu[1] == 1 ? "frente" : "solo");
            }
            UUID a = apuntado();
            if (a != null && a.equals(sp.getUUID()) && paso == Paso.ESPERA && fase == Fase.REVISION) {
                d.putBoolean("elige", true);
                d.putInt("segundos", segundosElegir);
            }
            Integer e = eligio.get(sp.getUUID());
            if (e != null) {
                d.putInt("eligio", e);
            }
        }
        PlayerStore.Sync.toPlayer(sp, KEY_MIO, d);
    }

    /** Lo que ve el organizador (que no juega): a quién apunta el arma, su símbolo real y lo que marcó. */
    private static void org(MinecraftServer s) {
        Evento ev = Evento.get(s);
        CompoundTag d = new CompoundTag();
        UUID a = apuntado();
        if (a != null) {
            Pieza p = ev.pieza(a);
            d.putString("nombre", p == null ? "?" : p.nombre);
            d.putString("etiqueta", p == null ? "" : p.etiqueta());
            Integer k = simbolo.get(a);
            d.putString("real", k == null ? "" : SIMBOLOS[k]);
            Integer e = eligio.get(a);
            d.putString("marco", e == null ? "" : SIMBOLOS[e]);
            d.putBoolean("traidor", traidoresIds.contains(a));
            d.putBoolean("fase_traidores", fase == Fase.TRAIDORES);
            d.putString("paso", paso.name());
            d.putInt("i", idx + 1);
            d.putInt("de", fase == Fase.TRAIDORES ? pendientesTraidores.size() : orden.size());
        }
        d.putString("fase", fase.name());
        d.putBoolean("auto", auto);
        d.putBoolean("juez", hayJuez(s));
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            Pieza pz = ev.pieza(p);
            if ((pz == null || !pz.vivo()) && (ev.esOrganizador(p.getUUID()) || Plat.isOp(p))) {
                PlayerStore.Sync.toPlayer(p, KEY_ORG, d);
            }
        }
    }

    public static String resumen() {
        return fase.name() + " turno=" + (turno + 1) + " traidores=" + traidoresIds.size() + " balas=" + balas;
    }

    /** A dónde ir: al pasillo (desde la casa) y luego al salón. */
    public static Objetivo.Meta objetivo(MinecraftServer s, ServerPlayer sp) {
        if (fase == Fase.PASILLO && !Cabina.dentro(s, "pas_man", sp, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
            return Objetivo.Meta.en(Complejo.pos(s, "pas_man.centro"), 1.5, MOD + ".obj.pasillo");
        }
        if (fase == Fase.LLEGANDO && desde <= t && !enSalon(s, sp)) {
            return Objetivo.Meta.en(Complejo.pos(s, "caja.centro"), 1.5, MOD + ".obj.carrusel");
        }
        return null;
    }

    // ------------------------------------------------------------------ utilidades

    private static void ir(MinecraftServer s, Entity e, double x, double y, double z, float yaw) {
        e.stopRiding();
        e.fallDistance = 0;
        if (e instanceof ServerPlayer sp) {
            sp.teleportTo(s.overworld(), x, y, z, yaw, 0F);
        } else {
            if (e instanceof PiezaBot b) {
                b.getNavigation().stop();
            }
            e.moveTo(x, y, z, yaw, 0F);
        }
    }

    /** La musiquita de la caja (unas notas de campanita, despacio). */
    private static void musica(MinecraftServer s, float vol) {
        BlockPos c = Complejo.pos(s, "caja.centro");
        if (c == null) {
            return;
        }
        float[] notas = {1.0F, 1.19F, 1.5F, 1.33F, 1.19F, 1.0F};
        for (int i = 0; i < notas.length; i++) {
            s.overworld().playSound(null, c, SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.RECORDS, vol, notas[i] * 0.5F);
        }
    }

    /** Quita todo lo de la caja: asientos, sillas, arma, máscaras, etiquetas, sangre, voces. */
    private static void limpiarTodo(MinecraftServer s) {
        for (Silla[] gg : asientos) {
            for (int i = 0; i < gg.length; i++) {
                if (gg[i] != null) {
                    gg[i].soltar();
                    gg[i].discard();
                    gg[i] = null;
                }
            }
        }
        for (Silla si : circulo.values()) {
            si.soltar();
            si.discard();
        }
        circulo.clear();
        // también las que hayan quedado de antes (si el servidor se reinició a la mitad)
        ServerLevel l = s.overworld();
        BlockPos o = Complejo.origen(s, "caja");
        if (o != null) {
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(o.getX(), o.getY() - 2, o.getZ() - 2, o.getX() + 66, o.getY() + 16,
                    o.getZ() + 46);
            for (Silla si : l.getEntitiesOfClass(Silla.class, box, x -> "caja".equals(x.grupo) || x.oculta())) {
                si.soltar();
                si.discard();
            }
            for (ArmaTecho a : l.getEntitiesOfClass(ArmaTecho.class, box)) {
                a.discard();
            }
        }
        if (arma != null) {
            arma.discard();
            arma = null;
        }
        for (BlockPos q : sangre) {
            if (l.getBlockState(q).getBlock() != Blocks.AIR) {
                l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        sangre.clear();
        Vestuario.quitarDeTodos(s, ETIQUETA);
        Vestuario.quitarDeTodos(s, MASCARA);
        Voz.juntar();
        Voz.tonos(Map.of());
        simbolo.clear();
        traidoresIds.clear();
        rondas.clear();
        gondolasRonda.clear();
        lugar.clear();
        dichos.clear();
        oyentes.clear();
        orden.clear();
        pendientesTraidores.clear();
        eligio.clear();
        fusilados.clear();
        turno = -1;
        idx = -1;
        balas = 0;
        salvados = 0;
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase == Fase.HABLANDO || fase == Fase.CIRCULO || fase == Fase.TRAIDORES || fase == Fase.FIN) {
            Pantallas.callar(s);
        }
        limpiarTodo(s);
        fase = Fase.NADA;
        enviar(s);
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            PlayerStore.Sync.toPlayer(sp, KEY_MIO, new CompoundTag());
            PlayerStore.Sync.toPlayer(sp, KEY_ORG, new CompoundTag());
        }
    }

    /** Para el organizador: salta al siguiente paso. */
    public static boolean saltar(MinecraftServer s) {
        switch (fase) {
            case PASILLO -> {
                llegar(s);
                return true;
            }
            case LLEGANDO -> {
                hablar(s);
                return true;
            }
            case HABLANDO -> {
                roles(s);
                return true;
            }
            case ROLES -> {
                empezarTurnos(s);
                return true;
            }
            case TURNO -> {
                finTurno(s);
                return true;
            }
            case CIRCULO -> {
                Pantallas.callar(s);
                if (arma != null && !arma.abajo()) {
                    arma.bajar();
                }
                revisar(s);
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
