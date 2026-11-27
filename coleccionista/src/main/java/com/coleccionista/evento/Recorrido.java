package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.coleccionista.Coleccionista;
import com.coleccionista.mapa.Antesala;
import com.coleccionista.mapa.Cabinas;
import com.coleccionista.prueba.ACiegas;
import com.coleccionista.prueba.Compartir;
import com.coleccionista.prueba.Encadenados;
import com.coleccionista.prueba.Escena;
import com.coleccionista.prueba.LasCopas;
import com.coleccionista.prueba.LasPuertas;
import com.coleccionista.prueba.Pruebas;
import com.coleccionista.prueba.Sacrificio;
import com.coleccionista.prueba.Traidor;
import com.coleccionista.prueba.TresSentidos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * El recorrido: el evento corre solo y siempre hacia adelante. Del Ala de celdas sale el primer elevador; cada prueba tiene su sala de
 * llegada (el Coleccionista explica en la pantalla y dice de cuántos son los equipos), sus cubículos (cada equipo se mete en uno; la puerta
 * se cierra sola al llenarse), la prueba (cada equipo en su lugar, lejos de los demás) y la sala de espera, de donde sale el elevador a la
 * siguiente prueba. Quien no entra a un cubículo a tiempo, o no sube al elevador, se queda atrás (eliminado).
 * <p>
 * El organizador solo mira: puede pausar, saltar el paso, llevar a todos a una prueba ({@code /evento recorrido ...}) o usar los comandos de
 * cada prueba como antes.
 */
public final class Recorrido {
    private static final String MOD = Coleccionista.MODID;

    /** Una prueba del recorrido: su zona, su id (para /evento), su guion, el tamaño de los equipos (1 = de uno por uno). */
    record Etapa(String zona, String prueba, IntSupplier tam, Function<MinecraftServer, Component> preparar,
            Function<MinecraftServer, Boolean> empezar, Supplier<String> fase, java.util.function.Consumer<MinecraftServer> terminar) {
    }

    static final List<Etapa> TODAS = List.of(
            new Etapa("cloacas", "encadenados", () -> 2, Encadenados::preparar, s -> Encadenados.empezar(s, false),
                    () -> Encadenados.fase().name(), Encadenados::terminar),
            new Etapa("archivo", "traidor", () -> Traidor.tamano, Traidor::preparar, s -> Traidor.empezar(s, false),
                    () -> Traidor.fase().name(), Traidor::terminar),
            new Etapa("salon", "compartir", () -> 2, Compartir::preparar, s -> Compartir.empezar(s, false), () -> Compartir.fase().name(),
                    Compartir::terminar),
            new Etapa("laberintos", "ciegas", () -> ACiegas.tamano, ACiegas::preparar, s -> ACiegas.empezar(s, false),
                    () -> ACiegas.fase().name(), ACiegas::terminar),
            new Etapa("capilla", "sacrificio", () -> Sacrificio.tamano, Sacrificio::preparar, s -> Sacrificio.empezar(s, false),
                    () -> Sacrificio.fase().name(), Sacrificio::terminar),
            new Etapa("puertas", "puertas", () -> 1, LasPuertas::preparar, LasPuertas::ronda, () -> LasPuertas.fase().name(),
                    LasPuertas::terminar),
            new Etapa("galeria", "tres", () -> TresSentidos.tamano, TresSentidos::preparar, s -> TresSentidos.empezar(s, false),
                    () -> TresSentidos.fase().name(), TresSentidos::terminar),
            new Etapa("final", "copas", () -> 1, LasCopas::preparar, s -> LasCopas.empezar(s, false), () -> LasCopas.fase().name(),
                    LasCopas::terminar));

    enum Fase {
        NADA, ELEVADOR, VIAJE, DISCURSO, FORMAR, ENTRAR, JUEGO, CIERRE, FIN
    }

    /** Ajustes: segundos para formar equipos y para subir al elevador; si empieza solo al salir de las celdas; si va la prueba extra. */
    public static int formarSeg = 90, elevadorSeg = 120;
    public static boolean auto = true, extra = false;

    private static Fase fase = Fase.NADA;
    private static int etapa = -1;
    private static String abordar = "";
    private static long desde, listosEn = -1, ultimoBot;
    private static boolean pausa;
    private static final Random R = new Random();

    private Recorrido() {
    }

    public static void init() {
        AxelisCore.onServerTick(Recorrido::tick);
    }

    /** Las etapas de esta noche (la extra solo si está activada, antes de la final). */
    static List<Etapa> etapas() {
        List<Etapa> l = new ArrayList<>();
        for (Etapa e : TODAS) {
            if (extra || !e.prueba().equals("tres")) {
                l.add(e);
            }
        }
        return l;
    }

    public static boolean activo() {
        return fase != Fase.NADA;
    }

    public static String estado() {
        Etapa e = actual();
        return fase.name() + (e == null ? "" : " · " + e.prueba()) + (pausa ? " · en pausa" : "");
    }

    private static Etapa actual() {
        List<Etapa> l = etapas();
        return etapa >= 0 && etapa < l.size() ? l.get(etapa) : null;
    }

    private static long ahora(MinecraftServer s) {
        return s.overworld().getGameTime();
    }

    // ------------------------------------------------------------------ inicio y control

    /** Empieza el recorrido: se abre el elevador del Ala de celdas. */
    public static boolean iniciar(MinecraftServer s) {
        if (!Complejo.construido(s) || Complejo.pos(s, "cab.inicio.origen") == null) {
            return false;
        }
        etapa = 0;
        pausa = false;
        subir(s, "inicio");
        Coleccionista.LOGGER.info("COLECCIONISTA_RECORRIDO inicia");
        return true;
    }

    /** Lo llama el despertar cuando el Coleccionista termina de hablar a las que salieron de sus celdas. */
    public static void alSalirDeLasCeldas(MinecraftServer s) {
        if (auto && fase == Fase.NADA) {
            iniciar(s);
        }
    }

    public static boolean pausar(MinecraftServer s, boolean p) {
        if (fase == Fase.NADA) {
            return false;
        }
        pausa = p;
        Coleccionista.LOGGER.info("COLECCIONISTA_RECORRIDO pausa={}", p);
        return true;
    }

    /** El organizador salta lo que se está esperando (que suban, que formen equipos, el discurso, el cierre). */
    public static boolean saltar(MinecraftServer s) {
        switch (fase) {
            case ELEVADOR -> partir(s);
            case DISCURSO -> {
                Pantallas.callar(s);
                formar(s);
            }
            case FORMAR -> entrar(s);
            case CIERRE -> reunir(s);
            default -> {
                return false;
            }
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_RECORRIDO salta");
        return true;
    }

    /** Lleva a todas las piezas vivas al elevador de llegada de esa prueba y sigue desde ahí. */
    public static boolean ir(MinecraftServer s, String prueba) {
        List<Etapa> l = etapas();
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).prueba().equals(prueba) || l.get(i).zona().equals(prueba)) {
                String cab = "ante_" + l.get(i).zona() + ".llega";
                AABB c = Cabina.caja(s, cab, Cabinas.ANCHO, Cabinas.ALTO, Cabinas.FONDO);
                if (c == null) {
                    return false;
                }
                Pruebas.cerrarOtras(s, "");
                etapa = i;
                pausa = false;
                Evento ev = Evento.get(s);
                for (Pieza p : ev.vivos()) {
                    Entity e = Evento.entidad(s, p);
                    if (e != null) {
                        mover(e, new Vec3(c.minX + 0.6 + R.nextDouble() * (c.getXsize() - 1.2), c.minY + 0.05,
                                c.minZ + 0.6 + R.nextDouble() * (c.getZsize() - 1.2)), Complejo.yaw("cab." + cab + ".origen") + 180F);
                    }
                }
                Cabina.puerta(s, cab, true);
                llegar(s);
                Coleccionista.LOGGER.info("COLECCIONISTA_RECORRIDO ir {}", prueba);
                return true;
            }
        }
        return false;
    }

    public static void parar(MinecraftServer s) {
        if (fase != Fase.NADA) {
            Evento.get(s).reloj(s, 0);
        }
        fase = Fase.NADA;
        etapa = -1;
        pausa = false;
        listosEn = -1;
    }

    // ------------------------------------------------------------------ elevador

    /** Se abre la puerta de un elevador y esperan a que suban todas las piezas vivas. */
    private static void subir(MinecraftServer s, String cab) {
        abordar = cab;
        fase = Fase.ELEVADOR;
        desde = ahora(s);
        listosEn = -1;
        Cabina.puerta(s, cab, true);
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".rec.suban")));
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.7F);
        Evento.get(s).reloj(s, elevadorSeg);
        Coleccionista.LOGGER.info("COLECCIONISTA_RECORRIDO elevador {}", cab);
    }

    private static void partir(MinecraftServer s) {
        Etapa e = actual();
        if (e == null) {
            return;
        }
        Evento ev = Evento.get(s);
        ev.reloj(s, 0);
        // quien no subió se queda atrás
        for (Pieza p : new ArrayList<>(ev.vivos())) {
            Entity en = Evento.entidad(s, p);
            if (en == null || !Cabina.dentro(s, abordar, en, Cabinas.ANCHO, Cabinas.ALTO, Cabinas.FONDO)) {
                if (ev.vivos().size() > 1) {
                    Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".rec.atras", Anuncio.nombre(p))));
                    ev.eliminar(s, p, Component.translatable(MOD + ".rec.motivo.atras").getString());
                }
            }
        }
        fase = Fase.VIAJE;
        Pantallas.parada = e.zona();
        Pantallas.hablar(s, "montacargas", null);
        Cabina.viajar(s, abordar, "ante_" + e.zona() + ".llega", Cabina.Tipo.ELEVADOR, 200, Cabinas.ANCHO, Cabinas.ALTO, Cabinas.FONDO,
                () -> llegar(s));
    }

    // ------------------------------------------------------------------ la prueba

    /** Llegaron a la sala de la prueba: el Coleccionista la explica en la pantalla y luego pide los equipos. */
    private static void llegar(MinecraftServer s) {
        Etapa e = actual();
        if (e == null) {
            return;
        }
        fase = Fase.DISCURSO;
        desde = ahora(s);
        Evento.get(s).reloj(s, 0);
        Evento.get(s).prueba(s, nombre(e.prueba()));
        if (!Pantallas.hablar(s, e.prueba(), () -> {
            if (fase == Fase.DISCURSO) {
                formar(s);
            }
        })) {
            formar(s);
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_RECORRIDO llega {}", e.zona());
    }

    /** Se abren los cubículos (o el portón, si es de uno por uno) y corre el tiempo para formar equipos. */
    private static void formar(MinecraftServer s) {
        Etapa e = actual();
        if (e == null) {
            return;
        }
        fase = Fase.FORMAR;
        desde = ahora(s);
        listosEn = -1;
        Evento ev = Evento.get(s);
        int tam = Math.max(1, e.tam().getAsInt());
        if (tam > 1) {
            int n = Math.min(Antesala.CUBICULOS, (ev.vivos().size() + tam - 1) / tam);
            for (int k = 1; k <= Antesala.CUBICULOS; k++) {
                puertaCub(s, e.zona(), k, k <= n);
                foco(s, e.zona(), k, false);
            }
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".rec.equipos", tam)));
            Pantallas.modo(s, "texto", Component.translatable(MOD + ".rec.pantalla.equipos", tam).getString());
        } else {
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".rec.porton")));
            Pantallas.modo(s, "texto", Component.translatable(MOD + ".rec.pantalla.porton").getString());
        }
        Anuncio.sonido(s, SoundEvents.IRON_DOOR_OPEN, 1F, 0.6F);
        ev.reloj(s, formarSeg);
        Coleccionista.LOGGER.info("COLECCIONISTA_RECORRIDO formar {} tam={}", e.prueba(), tam);
    }

    /** Se acabó la espera: quien no tiene lugar se queda atrás; se arman los equipos y empieza la prueba. */
    private static void entrar(MinecraftServer s) {
        Etapa e = actual();
        if (e == null) {
            return;
        }
        Evento ev = Evento.get(s);
        ev.reloj(s, 0);
        int tam = Math.max(1, e.tam().getAsInt());
        List<List<UUID>> equipos = new ArrayList<>();
        List<UUID> sueltos = new ArrayList<>();
        List<UUID> adentro = new ArrayList<>();
        if (tam > 1) {
            for (int k = 1; k <= Antesala.CUBICULOS; k++) {
                List<UUID> c = enCubiculo(s, e.zona(), k);
                puertaCub(s, e.zona(), k, false);
                if (c.isEmpty()) {
                    continue;
                }
                adentro.addAll(c);
                if (c.size() >= tam) {
                    equipos.add(c);
                } else {
                    sueltos.addAll(c);
                }
            }
            // los equipos incompletos se juntan para que queden parejos (si sobra uno, va con el último)
            List<UUID> grupo = new ArrayList<>();
            for (UUID id : sueltos) {
                grupo.add(id);
                if (grupo.size() == tam) {
                    equipos.add(grupo);
                    grupo = new ArrayList<>();
                }
            }
            if (!grupo.isEmpty()) {
                if (grupo.size() == 1 && !equipos.isEmpty()) {
                    equipos.get(equipos.size() - 1).add(grupo.get(0));
                } else {
                    equipos.add(grupo);
                }
            }
        } else {
            AABB f = frente(s, e.zona());
            for (Pieza p : ev.vivos()) {
                Entity en = Evento.entidad(s, p);
                if (en != null && f != null && f.contains(en.position())) {
                    adentro.add(p.id);
                }
            }
        }
        // quien no entró a tiempo, se queda atrás (siempre quedan al menos dos)
        for (Pieza p : new ArrayList<>(ev.vivos())) {
            if (!adentro.contains(p.id) && ev.vivos().size() > 2) {
                Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".rec.sin_equipo", Anuncio.nombre(p))));
                ev.eliminar(s, p, Component.translatable(MOD + ".rec.motivo.sin_equipo").getString());
            }
        }
        if (tam > 1) {
            for (int i = 0; i < equipos.size(); i++) {
                for (UUID id : equipos.get(i)) {
                    Pieza p = ev.pieza(id);
                    if (p != null) {
                        p.equipo = i;
                    }
                }
            }
            Pruebas.darEquipos(equipos);
        }
        Component err = e.preparar().apply(s);
        if (err != null) {
            pausa = true;
            Anuncio.todos(s, Component.translatable(MOD + ".rec.error", err).withStyle(ChatFormatting.RED));
            Coleccionista.LOGGER.warn("COLECCIONISTA_RECORRIDO error {}: {}", e.prueba(), err.getString());
            return;
        }
        fase = Fase.ENTRAR;
        desde = ahora(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_RECORRIDO entran {} equipos={}", e.prueba(), equipos.size());
    }

    // ------------------------------------------------------------------ cierre

    /** Terminó la prueba: todos (los que siguen) a la sala de espera, y de ahí al siguiente elevador. */
    private static void reunir(MinecraftServer s) {
        Etapa e = actual();
        if (e == null) {
            return;
        }
        Escena.fundido(s, MOD + ".rec.fundido", 70);
        fase = Fase.FIN;
        desde = ahora(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_RECORRIDO reunir {}", e.zona());
    }

    private static void aLaEspera(MinecraftServer s) {
        Etapa e = actual();
        if (e == null) {
            return;
        }
        Evento ev = Evento.get(s);
        int i = 0;
        for (Pieza p : ev.vivos()) {
            Entity en = Evento.entidad(s, p);
            BlockPos b = Complejo.pos(s, "ante." + e.zona() + ".espera." + (i++ % Antesala.LUGARES));
            if (en != null && b != null) {
                mover(en, Vec3.atBottomCenterOf(b), 270F);
            }
        }
        BlockPos c = Complejo.pos(s, "ante." + e.zona() + ".centro2");
        for (Pieza p : ev.piezas()) {
            if (!p.vivo() && !p.bot && c != null) {
                ServerPlayer sp = s.getPlayerList().getPlayer(p.id);
                if (sp != null) {
                    sp.teleportTo(s.overworld(), c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 270F, 0F);
                }
            }
        }
        e.terminar().accept(s);
        Pantallas.modo(s, "coleccionista", "");
        List<Etapa> l = etapas();
        if (etapa >= l.size() - 1) {
            fase = Fase.NADA;
            Coleccionista.LOGGER.info("COLECCIONISTA_RECORRIDO fin");
            return;
        }
        String cab = "ante_" + e.zona() + ".sale";
        etapa++;
        subir(s, cab);
    }

    // ------------------------------------------------------------------ cada tick

    private static void tick(MinecraftServer s) {
        if (fase == Fase.NADA || pausa) {
            return;
        }
        long now = ahora(s);
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        Etapa e = actual();
        switch (fase) {
            case ELEVADOR -> {
                botsA(s, Cabina.caja(s, abordar, Cabinas.ANCHO, Cabinas.ALTO, Cabinas.FONDO), now);
                if (now % 10 != 0) {
                    return;
                }
                boolean todos = !ev.vivos().isEmpty();
                for (Pieza p : ev.vivos()) {
                    Entity en = Evento.entidad(s, p);
                    if (en == null || !Cabina.dentro(s, abordar, en, Cabinas.ANCHO, Cabinas.ALTO, Cabinas.FONDO)) {
                        todos = false;
                        break;
                    }
                }
                if (todos && listosEn < 0) {
                    listosEn = now + 50;
                    Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".rec.todos_arriba")));
                } else if (!todos) {
                    listosEn = -1;
                }
                if (listosEn > 0 && now >= listosEn || now - desde >= elevadorSeg * 20L) {
                    partir(s);
                }
            }
            case FORMAR -> {
                if (e == null) {
                    return;
                }
                int tam = Math.max(1, e.tam().getAsInt());
                botsFormar(s, e, tam, now);
                if (now % 10 != 0) {
                    return;
                }
                int dentro = 0;
                if (tam > 1) {
                    for (int k = 1; k <= Antesala.CUBICULOS; k++) {
                        List<UUID> c = enCubiculo(s, e.zona(), k);
                        dentro += c.size();
                        if (c.size() >= tam && cubAbierto(s, e.zona(), k)) {
                            // el equipo está completo: se cierra su puerta y se prende su foco
                            puertaCub(s, e.zona(), k, false);
                            foco(s, e.zona(), k, true);
                            Anuncio.sonido(s, SoundEvents.IRON_DOOR_CLOSE, 0.8F, 0.7F);
                            Coleccionista.LOGGER.info("COLECCIONISTA_RECORRIDO cubiculo {} lleno", k);
                        }
                    }
                } else {
                    AABB f = frente(s, e.zona());
                    for (Pieza p : ev.vivos()) {
                        Entity en = Evento.entidad(s, p);
                        if (en != null && f != null && f.contains(en.position())) {
                            dentro++;
                        }
                    }
                }
                boolean todos = dentro >= ev.vivos().size();
                if (todos && listosEn < 0) {
                    listosEn = now + 60;
                    Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".rec.listos")));
                } else if (!todos) {
                    listosEn = -1;
                }
                if (listosEn > 0 && now >= listosEn || now - desde >= formarSeg * 20L) {
                    entrar(s);
                }
            }
            case ENTRAR -> {
                // después del fundido y la presentación, empieza (sin discurso: ya lo dio en la sala de llegada)
                if (e != null && now - desde >= 110) {
                    fase = Fase.JUEGO;
                    desde = now;
                    e.empezar().apply(s);
                }
            }
            case JUEGO -> {
                if (e == null) {
                    return;
                }
                String f = e.fase().get();
                if ("FIN".equals(f)) {
                    fase = Fase.CIERRE;
                    desde = now;
                } else if ("ENTRE".equals(f) && now - desde >= 100 && e.prueba().equals("puertas")) {
                    // Las puertas: la siguiente ronda sola
                    desde = now;
                    LasPuertas.ronda(s);
                } else if (!"ENTRE".equals(f)) {
                    if ("NADA".equals(f)) {
                        // alguien la terminó a mano: seguimos
                        fase = Fase.CIERRE;
                    }
                    desde = now;
                }
            }
            case CIERRE -> {
                if (now - desde >= 200) {
                    reunir(s);
                }
            }
            case FIN -> {
                if (now - desde >= 22) {
                    aLaEspera(s);
                }
            }
            default -> {
            }
        }
    }

    /** Lo llama el evento cuando se acaba el reloj; el recorrido lo atiende si está esperando algo. */
    public static boolean alAcabarReloj(MinecraftServer s) {
        if (fase == Fase.ELEVADOR) {
            partir(s);
            return true;
        }
        if (fase == Fase.FORMAR) {
            entrar(s);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ los bots (pruebas automáticas)

    /** Los bots no caminan: suben solos al elevador, uno cada medio segundo. */
    private static void botsA(MinecraftServer s, AABB c, long now) {
        if (c == null || now - ultimoBot < 10) {
            return;
        }
        Evento ev = Evento.get(s);
        for (Pieza p : ev.vivos()) {
            Entity en = p.bot ? Evento.entidad(s, p) : null;
            if (en != null && !c.contains(en.position())) {
                ultimoBot = now;
                mover(en, new Vec3(c.minX + 0.6 + R.nextDouble() * (c.getXsize() - 1.2), c.minY + 0.05,
                        c.minZ + 0.6 + R.nextDouble() * (c.getZsize() - 1.2)), 0F);
                return;
            }
        }
    }

    /** Los bots se meten a un cubículo con lugar (o al frente del portón). */
    private static void botsFormar(MinecraftServer s, Etapa e, int tam, long now) {
        if (now - ultimoBot < 10) {
            return;
        }
        Evento ev = Evento.get(s);
        for (Pieza p : ev.vivos()) {
            Entity en = p.bot ? Evento.entidad(s, p) : null;
            if (en == null) {
                continue;
            }
            if (tam <= 1) {
                AABB f = frente(s, e.zona());
                if (f != null && !f.contains(en.position())) {
                    ultimoBot = now;
                    mover(en, f.getCenter().with(net.minecraft.core.Direction.Axis.Y, f.minY + 0.05)
                            .add(R.nextDouble() * 3 - 1.5, 0, R.nextDouble() * 4 - 2), 90F);
                    return;
                }
                continue;
            }
            boolean ya = false;
            for (int k = 1; k <= Antesala.CUBICULOS && !ya; k++) {
                ya = enCubiculo(s, e.zona(), k).contains(p.id);
            }
            if (ya) {
                continue;
            }
            for (int k = 1; k <= Antesala.CUBICULOS; k++) {
                if (cubAbierto(s, e.zona(), k) && enCubiculo(s, e.zona(), k).size() < tam) {
                    AABB c = cubiculo(s, e.zona(), k);
                    if (c != null) {
                        ultimoBot = now;
                        mover(en, new Vec3(c.minX + 0.5 + R.nextInt(3), c.minY + 0.05, c.minZ + 0.5 + R.nextInt(3)), 0F);
                        return;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ los cubículos

    private static AABB cubiculo(MinecraftServer s, String zona, int k) {
        BlockPos b = Complejo.pos(s, "ante." + zona + ".cub." + k);
        return b == null ? null : new AABB(b.getX(), b.getY(), b.getZ(), b.getX() + 3, b.getY() + 3, b.getZ() + 3);
    }

    private static AABB frente(MinecraftServer s, String zona) {
        BlockPos a = Complejo.pos(s, "ante." + zona + ".frente.a"), b = Complejo.pos(s, "ante." + zona + ".frente.b");
        return a == null || b == null ? null : new AABB(a.getX(), a.getY(), a.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1);
    }

    /** Las piezas vivas dentro del cubículo k. */
    private static List<UUID> enCubiculo(MinecraftServer s, String zona, int k) {
        List<UUID> l = new ArrayList<>();
        AABB c = cubiculo(s, zona, k);
        if (c == null) {
            return l;
        }
        for (Pieza p : Evento.get(s).vivos()) {
            Entity en = Evento.entidad(s, p);
            if (en != null && c.contains(en.position())) {
                l.add(p.id);
            }
        }
        return l;
    }

    private static boolean cubAbierto(MinecraftServer s, String zona, int k) {
        BlockPos b = Complejo.pos(s, "ante." + zona + ".cub." + k + ".puerta");
        if (b == null) {
            return false;
        }
        BlockState st = s.overworld().getBlockState(b);
        return st.getBlock() instanceof DoorBlock && st.getValue(DoorBlock.OPEN);
    }

    private static void puertaCub(MinecraftServer s, String zona, int k, boolean abrir) {
        ServerLevel l = s.overworld();
        BlockPos p = Complejo.pos(s, "ante." + zona + ".cub." + k + ".puerta");
        if (p == null) {
            return;
        }
        for (BlockPos q : new BlockPos[] {p, p.above()}) {
            BlockState st = l.getBlockState(q);
            if (st.getBlock() instanceof DoorBlock && st.getValue(DoorBlock.OPEN) != abrir) {
                l.setBlock(q, st.setValue(DoorBlock.OPEN, abrir), 2 | 16);
            }
        }
    }

    private static void foco(MinecraftServer s, String zona, int k, boolean prendido) {
        BlockPos p = Complejo.pos(s, "ante." + zona + ".cub." + k + ".foco");
        if (p == null) {
            return;
        }
        ServerLevel l = s.overworld();
        BlockState st = l.getBlockState(p);
        Property<?> pr = st.getBlock().getStateDefinition().getProperty("encendida");
        if (pr instanceof BooleanProperty bp && st.getValue(bp) != prendido) {
            l.setBlock(p, st.setValue(bp, prendido), 2);
        }
    }

    private static void mover(Entity e, Vec3 v, float yaw) {
        e.fallDistance = 0;
        if (e instanceof ServerPlayer sp) {
            if (sp.isSleeping()) {
                sp.stopSleeping();
            }
            sp.teleportTo(sp.serverLevel(), v.x, v.y, v.z, yaw, 0F);
            if (!sp.isCreative() && !sp.isSpectator()) {
                sp.setGameMode(GameType.ADVENTURE);
            }
        } else {
            e.moveTo(v.x, v.y, v.z, yaw, 0F);
            e.setYHeadRot(yaw);
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    public static void reiniciar(MinecraftServer s) {
        parar(s);
        Cabina.reiniciar(s);
        Transbordo.reiniciar(s);
    }

    /** El nombre de la prueba (el que sale arriba en el HUD). */
    private static String nombre(String prueba) {
        return switch (prueba) {
            case "encadenados" -> Encadenados.PRUEBA;
            case "traidor" -> Traidor.PRUEBA;
            case "compartir" -> Compartir.PRUEBA;
            case "ciegas" -> ACiegas.PRUEBA;
            case "sacrificio" -> Sacrificio.PRUEBA;
            case "puertas" -> LasPuertas.PRUEBA;
            case "tres" -> TresSentidos.PRUEBA;
            default -> LasCopas.PRUEBA;
        };
    }

    static List<String> pruebas() {
        List<String> l = new ArrayList<>();
        for (Etapa e : TODAS) {
            l.add(e.prueba());
        }
        return Collections.unmodifiableList(l);
    }
}
