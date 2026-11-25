package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import com.coleccionista.bot.PiezaBot;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Apagon;
import com.coleccionista.evento.Bailarina;
import com.coleccionista.evento.Cabina;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Objetivo;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.mapa.Pasaje;
import com.coleccionista.mapa.SalaPiano;
import com.coleccionista.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Capítulo 6 · El piano gigante. Del salón de té pasan (por el pasillo de servicio) al salón de baile. Cada pieza tiene su piano de piso
 * (un tapete al centro y ocho teclas de colores alrededor), separado de los demás por cortinas. La bailarina de la caja de música toca una
 * melodía: en todos los pianos se iluminan las mismas teclas, en orden. Luego todos la repiten a la vez, pisando las teclas (y regresando
 * al tapete entre una y otra). Al final de cada ronda:
 * <ul>
 *     <li>nadie se equivocó: otra ronda con una nota más;</li>
 *     <li>se equivocó uno: se abre el piso de su piano y cae al foso;</li>
 *     <li>se equivocaron varios (no todos): los que acertaron se salvan y los otros juegan el desempate;</li>
 *     <li>se equivocaron todos: se repite la ronda.</li>
 * </ul>
 * Así nadie tiene ventaja por ir primero. Se acaba cuando cayeron los que tocaba (uno, si no dice otra cosa el organizador).
 */
public final class Piano {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":piano";
    public static final String PRUEBA = "El piano gigante";
    /**
     * Ajustes (los cambia el organizador con /evento piano ...): cuántos caen (0: los que hagan falta para que lleguen dos a la final), los
     * segundos para cada nota y con cuántas notas empieza.
     */
    public static int caer = 0, segundosNota = 4, notasInicio = 3;
    public static int esperaPasillo = 1200, esperaSalon = 900;
    /** Al terminar el salón de té, pasan solos al salón de baile; {@code rapido}: los bots tocan al instante y no se equivocan solos. */
    public static boolean encadenar = true, rapido;
    /** Para las pruebas: las piezas que se equivocan (a propósito) en su siguiente nota, y cuántas (al azar) en cada ronda que sigue. */
    private static final java.util.Set<UUID> fallan = new java.util.HashSet<>();
    private static final java.util.ArrayDeque<Integer> fallanPorRonda = new java.util.ArrayDeque<>();
    /** Ticks entre nota y nota cuando toca la bailarina, y cuántos tiene que estar en una tecla para que cuente. */
    public static final int T_NOTA = 13, T_PISA = 4;

    public enum Fase {
        NADA, PASILLO, LLEGANDO, HABLANDO, LUGARES, MUESTRA, TOCA, REVISA, CAE, FIN
    }

    private static Fase fase = Fase.NADA;
    private static long t, desde, apagonFin;
    private static final Random R = new Random();
    private static MinecraftServer srv;

    /** Quién está en qué piano. */
    private static final Map<UUID, Integer> piano = new LinkedHashMap<>();
    /** Los que siguen jugando (en el desempate, solo los que se equivocaron). */
    private static final List<UUID> juegan = new ArrayList<>();
    /** Los que se salvaron de esta caída (acertaron cuando otros fallaron) y los que no cupieron en los pianos. */
    private static final List<UUID> salvados = new ArrayList<>(), extras = new ArrayList<>();
    private static final List<Integer> melodia = new ArrayList<>();
    private static int ronda, largo, cayeron, cuota = 1;
    private static boolean desempate;
    /** Lo de cada pieza en la ronda: cuántas notas lleva, si ya se equivocó o terminó, y lo que va pisando. */
    private static final Map<UUID, Jugador> jug = new HashMap<>();
    private static Bailarina bailarina;
    private static final List<UUID> cae = new ArrayList<>(), caidos = new ArrayList<>();
    private static boolean lucesBajas;
    private static long aFinal;

    private static final class Jugador {
        int nota, ultima = -1, pisando = -2, ticks;
        boolean mal, listo, centro = true, botEnTecla;
        long desdeNota, botEn = -1;
        /** La última tecla que pisó (para el destello) y si estuvo bien. */
        int destello = -1;
        boolean destelloBien;
        long destelloEn;
    }

    private Piano() {
    }

    public static void init() {
        AxelisCore.onServerTick(Piano::tick);
        AxelisCore.onLogin(p -> PlayerStore.Sync.toPlayer(p, KEY, estado()));
        // aquí nadie se lastima: el foso no mata, lo que mata es el Coleccionista
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

    // ------------------------------------------------------------------ del salón de té al salón de baile

    public static Component llamar(MinecraftServer s) {
        if (!Complejo.construido(s, "piano")) {
            return Component.translatable(MOD + ".piano.sin_mapa");
        }
        if (!Complejo.construido(s, "te")) {
            return llegar(s);
        }
        Pruebas.cerrarOtras(s, "piano");
        limpiarTodo(s);
        puertas(s, "pas_te", true);
        fase = Fase.PASILLO;
        desde = t;
        Evento ev = Evento.get(s);
        ev.prueba(s, Component.translatable(MOD + ".caja.prueba_pasillo").getString());
        ev.reloj(s, 0);
        Anuncio.titulo(s, Component.empty(), Component.translatable(MOD + ".caja.al_pasillo"));
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_PIANO llamar");
        return null;
    }

    public static Component llegar(MinecraftServer s) {
        if (!Complejo.construido(s, "piano")) {
            return Component.translatable(MOD + ".piano.sin_mapa");
        }
        if (fase == Fase.NADA) {
            Pruebas.cerrarOtras(s, "piano");
            limpiarTodo(s);
        }
        Evento ev = Evento.get(s);
        List<Entity> enPasillo = new ArrayList<>(), fuera = new ArrayList<>();
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null) {
                continue;
            }
            if (Complejo.construido(s, "te") && Cabina.dentro(s, "pas_te", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                enPasillo.add(e);
            } else {
                fuera.add(e);
            }
        }
        if (!enPasillo.isEmpty()) {
            Cabina.trasladar(s, "pas_te", "pas_piano", enPasillo, List.of());
        }
        BlockPos c = Complejo.pos(s, "pas_piano.centro");
        int n = 0;
        for (Entity e : fuera) {
            if (c == null) {
                break;
            }
            ir(s, e, c.getX() + 0.5 - 1.5 + (n % 3) * 1.5, c.getY(), c.getZ() + 0.5 - 1 + (n / 3 % 4), 270F);
            n++;
        }
        puertas(s, "pas_te", false);
        puertas(s, "pas_piano", true);
        fase = Fase.LLEGANDO;
        desde = t;
        ev.prueba(s, Component.translatable(MOD + ".piano.prueba").getString());
        ev.reloj(s, 0);
        Pantallas.modo(s, "apagada", "");
        crearBailarina(s);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_PIANO llegan pasillo={} fuera={}", enPasillo.size(), fuera.size());
        return null;
    }

    private static void puertas(MinecraftServer s, String pasillo, boolean abrir) {
        for (String lado : new String[] {".izq", ".der"}) {
            for (String h : new String[] {".a", ".b"}) {
                BlockPos p = Complejo.pos(s, pasillo + lado + h);
                if (p == null) {
                    continue;
                }
                for (BlockPos q : new BlockPos[] {p, p.above()}) {
                    BlockState st = s.overworld().getBlockState(q);
                    if (st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
                            && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN) != abrir) {
                        s.overworld().setBlock(q, st.setValue(net.minecraft.world.level.block.DoorBlock.OPEN, abrir), 2 | 16);
                    }
                }
            }
        }
    }

    private static boolean enSalon(MinecraftServer s, Entity e) {
        BlockPos o = Complejo.origen(s, "piano");
        if (o == null) {
            return false;
        }
        double x = e.getX() - o.getX(), z = e.getZ() - o.getZ();
        return x >= SalaPiano.HX0 && x <= SalaPiano.HX1 + 1 && z >= SalaPiano.HZ0 && z <= SalaPiano.HZ1 + 1;
    }

    private static void crearBailarina(MinecraftServer s) {
        if (bailarina != null && bailarina.isAlive()) {
            return;
        }
        BlockPos b = Complejo.pos(s, "piano.bailarina");
        Bailarina e = b == null ? null : ModRegistry.BAILARINA.get().create(s.overworld());
        if (e != null) {
            e.moveTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, Complejo.yaw("piano.bailarina"), 0F);
            e.setYHeadRot(e.getYRot());
            s.overworld().addFreshEntity(e);
            bailarina = e;
        }
    }

    // ------------------------------------------------------------------ el discurso y a los pianos

    private static void hablar(MinecraftServer s) {
        fase = Fase.HABLANDO;
        desde = t;
        BlockPos c = Complejo.pos(s, "piano.entrada");
        for (Pieza p : Evento.get(s).vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null && c != null && !enSalon(s, e)) {
                ir(s, e, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 270F);
            }
        }
        puertas(s, "pas_piano", false);
        crearBailarina(s);
        if (bailarina != null) {
            bailarina.estado(Bailarina.BAILA);
        }
        Escena.presentacion(s, "VI", MOD + ".piano.titulo", MOD + ".piano.lema");
        Pantallas.soloGrande(s, true);
        Pantallas.hablar(s, "piano", () -> {
            if (fase == Fase.HABLANDO) {
                aLosPianos(s);
            }
        });
        Evento.get(s).prueba(s, PRUEBA);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_PIANO habla");
    }

    /** Un apagón y, en lo oscuro, cada pieza a su piano (si son más de cuatro, los que sobran se salvan esta vez). */
    private static void aLosPianos(MinecraftServer s) {
        if (fase != Fase.HABLANDO && fase != Fase.LLEGANDO) {
            return;
        }
        Pantallas.callar(s);
        fase = Fase.LUGARES;
        desde = t;
        apagonFin = t + 70;
        Apagon.hacer(s, s.getPlayerList().getPlayers(), 60, () -> acomodar(s));
        enviar(s);
    }

    private static void acomodar(MinecraftServer s) {
        Evento ev = Evento.get(s);
        List<Pieza> v = new ArrayList<>(ev.vivos());
        java.util.Collections.shuffle(v, R);
        piano.clear();
        juegan.clear();
        salvados.clear();
        extras.clear();
        caidos.clear();
        jug.clear();
        int k = 0;
        for (Pieza p : v) {
            if (k < SalaPiano.PIANOS) {
                piano.put(p.id, k++);
                juegan.add(p.id);
            } else {
                extras.add(p.id);
            }
        }
        for (UUID id : piano.keySet()) {
            Entity e = Evento.entidad(s, ev.pieza(id));
            if (e != null) {
                alTapete(s, e, piano.get(id));
            }
        }
        BlockPos sal = Complejo.pos(s, "piano.salida");
        for (UUID id : extras) {
            Entity e = Evento.entidad(s, ev.pieza(id));
            if (e != null && sal != null) {
                ir(s, e, sal.getX() + 0.5, sal.getY(), sal.getZ() + 0.5, 90F);
            }
        }
        ronda = 0;
        largo = Math.max(1, notasInicio) - 1;
        cayeron = 0;
        desempate = false;
        // cuántos caen: los que pidió el organizador o, si no, los que sobran para que lleguen dos a la final
        cuota = caer > 0 ? caer : Math.max(1, ev.vivos().size() - 2);
        cuota = Math.min(cuota, Math.max(1, piano.size() - 1));
        luces(s, true);
        Coleccionista.LOGGER.info("COLECCIONISTA_PIANO pianos={} extras={} caen={}", piano.size(), extras.size(), cuota);
    }

    /** El centro del tapete del piano k (donde se para la pieza): x, y, z. */
    private static double[] tapete(MinecraftServer s, int k) {
        BlockPos b = Complejo.pos(s, "piano.anillo." + k);
        return b == null ? null : new double[] {b.getX() + 1.0, b.getY() + 1.0, b.getZ() + 1.0};
    }

    private static void alTapete(MinecraftServer s, Entity e, int k) {
        double[] c = tapete(s, k);
        if (c != null) {
            ir(s, e, c[0], c[1], c[2], 180F);
        }
    }

    // ------------------------------------------------------------------ las rondas

    private static void nuevaRonda(MinecraftServer s, boolean otraNota) {
        ronda++;
        if (otraNota) {
            largo++;
        }
        melodia.clear();
        int antes = -1;
        for (int i = 0; i < largo; i++) {
            int n;
            do {
                n = R.nextInt(SalaPiano.TECLA.length);
            } while (n == antes);
            melodia.add(n);
            antes = n;
        }
        for (UUID id : juegan) {
            Jugador j = new Jugador();
            jug.put(id, j);
        }
        Integer n = fallanPorRonda.poll();
        if (n != null && n > 0) {
            List<UUID> v = new ArrayList<>(juegan);
            java.util.Collections.shuffle(v, R);
            fallan.addAll(v.subList(0, Math.min(n, v.size())));
        }
        fase = Fase.MUESTRA;
        desde = t;
        if (bailarina != null) {
            bailarina.estado(Bailarina.BAILA);
        }
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".piano.pantalla.escuchen", largo).getString());
        Evento.get(s).reloj(s, 0);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_PIANO ronda {} notas={} juegan={} desempate={}", ronda, largo, juegan.size(), desempate);
    }

    private static SoundEvent propio(String id) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD, id));
    }

    /** La nota de una tecla: suena en la bailarina (y en el piano de quien la pisa). */
    private static void nota(MinecraftServer s, int tecla, double[] donde, float vol) {
        ServerLevel l = s.overworld();
        if (donde != null) {
            l.playSound(null, donde[0], donde[1], donde[2], propio("piano_" + tecla), SoundSource.RECORDS, vol, 1F);
        } else if (bailarina != null) {
            l.playSound(null, bailarina.getX(), bailarina.getY() + 3, bailarina.getZ(), propio("piano_" + tecla), SoundSource.RECORDS, 3F, 1F);
        }
    }

    /** ¿Qué pisa? -1 el tapete, 0..7 una tecla, -2 nada (fuera del piano). */
    private static int region(MinecraftServer s, Entity e, int k) {
        BlockPos b = Complejo.pos(s, "piano.anillo." + k);
        if (b == null) {
            return -2;
        }
        int dx = (int) Math.floor(e.getX()) - b.getX(), dz = (int) Math.floor(e.getZ()) - b.getZ();
        if (dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1) {
            return -1;
        }
        for (int i = 0; i < SalaPiano.TECLA.length; i++) {
            int tx = SalaPiano.TECLA[i][0], tz = SalaPiano.TECLA[i][1];
            if (dx >= tx && dx <= tx + 1 && dz >= tz && dz <= tz + 1) {
                return i;
            }
        }
        return -2;
    }

    /** Pisó una tecla: si es la que sigue, suena bien; si no, se equivocó. */
    private static void pisa(MinecraftServer s, UUID id, int tecla) {
        Jugador j = jug.get(id);
        Integer k = piano.get(id);
        if (j == null || k == null || j.mal || j.listo) {
            return;
        }
        double[] c = tapete(s, k);
        j.destello = tecla;
        j.destelloEn = s.overworld().getGameTime();
        int quiere = melodia.get(j.nota);
        boolean forzado = fallan.contains(id);
        if (tecla == quiere && !forzado) {
            j.destelloBien = true;
            nota(s, tecla, c, 1.6F);
            j.nota++;
            j.desdeNota = t;
            if (j.nota >= melodia.size()) {
                j.listo = true;
                if (c != null) {
                    s.overworld().playSound(null, c[0], c[1], c[2], SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.2F);
                }
            }
        } else {
            equivoco(s, id, j, c);
        }
        enviar(s);
    }

    private static void equivoco(MinecraftServer s, UUID id, Jugador j, double[] c) {
        j.mal = true;
        j.destelloBien = false;
        j.destelloEn = s.overworld().getGameTime();
        if (c != null) {
            s.overworld().playSound(null, c[0], c[1], c[2], propio("piano_mal"), SoundSource.HOSTILE, 2F, 1F);
        }
        fallan.remove(id);
        Pieza p = Evento.get(s).pieza(id);
        Coleccionista.LOGGER.info("COLECCIONISTA_PIANO mal {} nota={}", p == null ? id : p.nombre, j.nota + 1);
    }

    private static void tocaTick(MinecraftServer s) {
        Evento ev = Evento.get(s);
        boolean todos = true;
        for (UUID id : juegan) {
            Jugador j = jug.get(id);
            Pieza p = ev.pieza(id);
            Entity e = p == null || !p.vivo() ? null : Evento.entidad(s, p);
            Integer k = piano.get(id);
            if (j == null || k == null) {
                continue;
            }
            if (e == null) {
                if (!j.mal && !j.listo) {
                    equivoco(s, id, j, tapete(s, k));
                }
                continue;
            }
            if (!j.mal && !j.listo) {
                todos = false;
                // se tardó mucho en la nota: cuenta como equivocarse
                if (t - j.desdeNota > segundosNota * 20L) {
                    equivoco(s, id, j, tapete(s, k));
                    enviar(s);
                    continue;
                }
                if (p.bot) {
                    botTick(s, id, j, e, k);
                } else {
                    jugadorTick(s, id, j, e, k);
                }
            }
        }
        if (todos) {
            revisar(s);
        }
    }

    /** Lo que pisa una persona: una tecla cuenta cuando lleva un ratito en ella y viene del tapete (o de otra tecla). */
    private static void jugadorTick(MinecraftServer s, UUID id, Jugador j, Entity e, int k) {
        int r = region(s, e, k);
        if (r == -2) {
            // se salió del piano: de regreso al tapete
            alTapete(s, e, k);
            j.pisando = -1;
            j.ticks = 0;
            j.centro = true;
            return;
        }
        if (r != j.pisando) {
            j.pisando = r;
            j.ticks = 0;
        }
        j.ticks++;
        if (r == -1) {
            j.centro = true;
            return;
        }
        // solo cuenta si viene del tapete (así no se pisa sin querer una tecla al pasar a otra)
        if (j.ticks == T_PISA && j.centro) {
            j.ultima = r;
            j.centro = false;
            pisa(s, id, r);
        }
    }

    /** Los bots: brincan a la tecla, la pisan y regresan; se equivocan más mientras más larga es la melodía. */
    private static void botTick(MinecraftServer s, UUID id, Jugador j, Entity e, int k) {
        if (j.botEn < 0) {
            j.botEn = t + (rapido ? 4 : 14 + R.nextInt(18));
            return;
        }
        if (t < j.botEn) {
            return;
        }
        BlockPos b = Complejo.pos(s, "piano.anillo." + k);
        if (j.botEnTecla) {
            // de regreso al tapete
            j.botEnTecla = false;
            j.botEn = t + (rapido ? 2 : 6 + R.nextInt(8));
            if (b != null) {
                ir(s, e, b.getX() + 1.0, b.getY() + 1.0, b.getZ() + 1.0, 180F);
            }
            return;
        }
        j.botEnTecla = true;
        j.botEn = t + (rapido ? 2 : 8 + R.nextInt(8));
        int tecla = melodia.get(j.nota);
        double p = rapido ? 0 : Math.min(0.45, 0.03 + 0.045 * Math.max(0, largo - 3));
        if (R.nextDouble() < p) {
            int otra = (tecla + 1 + R.nextInt(SalaPiano.TECLA.length - 1)) % SalaPiano.TECLA.length;
            tecla = otra;
        }
        if (b != null) {
            int[] tc = SalaPiano.TECLA[tecla];
            ir(s, e, b.getX() + tc[0] + 1.0, b.getY() + 1.0, b.getZ() + tc[1] + 1.0, 180F);
        }
        pisa(s, id, tecla);
    }

    /** Terminaron todos (bien o mal): quién se salva, quién juega el desempate y quién cae. */
    private static void revisar(MinecraftServer s) {
        fase = Fase.REVISA;
        desde = t;
        List<UUID> mal = new ArrayList<>();
        for (UUID id : juegan) {
            Jugador j = jug.get(id);
            if (j != null && j.mal) {
                mal.add(id);
            }
        }
        Evento ev = Evento.get(s);
        cae.clear();
        String que;
        if (mal.isEmpty()) {
            que = "nadie";
        } else if (mal.size() == juegan.size()) {
            que = "todos";
        } else if (mal.size() == 1) {
            que = "cae";
            cae.add(mal.get(0));
        } else {
            que = "desempate";
            for (UUID id : juegan) {
                if (!mal.contains(id)) {
                    salvados.add(id);
                }
            }
            juegan.retainAll(mal);
            desempate = true;
        }
        resultado = que;
        if (bailarina != null && !mal.isEmpty()) {
            double[] c = tapete(s, piano.getOrDefault(mal.get(0), 0));
            if (c != null) {
                bailarina.mirarA(c[0], c[2]);
            }
        } else if (bailarina != null) {
            bailarina.estado(Bailarina.QUIETA);
        }
        switch (que) {
            case "nadie" -> Anuncio.sonido(s, SoundEvents.AMETHYST_BLOCK_RESONATE, 1F, 1F);
            case "todos" -> Anuncio.sonido(s, SoundEvents.NOTE_BLOCK_BASS.get(), 1F, 0.5F);
            default -> {
                Anuncio.sonido(s, SoundEvents.WARDEN_HEARTBEAT, 1F, 1F);
                Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.5F);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (UUID id : mal) {
            Pieza p = ev.pieza(id);
            sb.append(p == null ? "?" : p.nombre).append(' ');
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_PIANO revisa ronda={} mal=[{}] {}", ronda, sb.toString().trim(), que);
        enviar(s);
    }

    private static String resultado = "";

    /** Se abre el piso del piano de quien se equivocó solo: cae al foso. */
    private static void caer(MinecraftServer s) {
        fase = Fase.CAE;
        desde = t;
        ServerLevel l = s.overworld();
        for (UUID id : cae) {
            Integer k = piano.get(id);
            BlockPos b = k == null ? null : Complejo.pos(s, "piano.anillo." + k);
            if (b == null) {
                continue;
            }
            for (int dx = -2; dx <= 3; dx++) {
                for (int dz = -2; dz <= 3; dz++) {
                    BlockPos q = b.offset(dx, 0, dz);
                    BlockState st = l.getBlockState(q);
                    if (!st.isAir()) {
                        l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), q.getX() + 0.5, q.getY() + 0.9, q.getZ() + 0.5, 4, 0.3,
                                0.1, 0.3, 0.1);
                        l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
            l.playSound(null, b.getX() + 1, b.getY() + 1, b.getZ() + 1, propio("piano_abre"), SoundSource.HOSTILE, 3F, 1F);
            Pieza p = Evento.get(s).pieza(id);
            Entity e = p == null ? null : Evento.entidad(s, p);
            if (e != null) {
                e.stopRiding();
                e.setDeltaMovement(0, -0.4, 0);
                e.hurtMarked = true;
            }
        }
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            Fx.shake(sp, 0.9F, 12);
        }
        enviar(s);
    }

    private static void eliminarCaidos(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (UUID id : cae) {
            Pieza p = ev.pieza(id);
            if (p == null) {
                continue;
            }
            String motivo = Component.translatable(MOD + ".piano.motivo").getString();
            if (ev.eliminarEnPrueba(s, p, motivo)) {
                cayeron++;
                Coleccionista.LOGGER.info("COLECCIONISTA_PIANO cae {} {} cayeron={}/{}", p.etiqueta(), p.nombre, cayeron, cuota);
            } else {
                // se salvó (no quedan suficientes para la final): sale del foso
                cayeron++;
                Entity e = Evento.entidad(s, p);
                BlockPos sal = Complejo.pos(s, "piano.salida");
                if (e != null && sal != null) {
                    ir(s, e, sal.getX() + 0.5, sal.getY(), sal.getZ() + 0.5, 90F);
                }
                Coleccionista.LOGGER.info("COLECCIONISTA_PIANO perdona {}", p.nombre);
            }
            juegan.remove(id);
            caidos.add(id);
        }
        cae.clear();
    }

    /** Después de revisar: otra ronda (con una nota más, la misma o el desempate) o se acabó. */
    private static void despues(MinecraftServer s) {
        if ("cae".equals(resultado)) {
            caer(s);
            return;
        }
        if (juegan.size() <= 1) {
            fin(s);
            return;
        }
        // si nadie se equivocó, una nota más; si se equivocaron todos o es desempate, igual de larga (pero otra melodía)
        nuevaRonda(s, "nadie".equals(resultado));
    }

    // ------------------------------------------------------------------ cada tick

    private static void tick(MinecraftServer s) {
        srv = s;
        if (fase == Fase.NADA) {
            return;
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        t++;
        long dt = t - desde;
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
                        boolean dentro = Cabina.dentro(s, "pas_te", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO);
                        if (p.bot && !dentro && dt >= 60) {
                            BlockPos c = Complejo.pos(s, "pas_te.centro");
                            if (c != null) {
                                ir(s, e, c.getX() + 0.5 + R.nextInt(3) - 1, c.getY(), c.getZ() + 0.5 + R.nextInt(3) - 1, 90F);
                                dentro = true;
                            }
                        }
                        todos &= dentro;
                    }
                    if (n > 0 && todos || dt >= esperaPasillo) {
                        BlockPos c = Complejo.pos(s, "pas_te.centro");
                        for (Pieza p : ev.vivos()) {
                            Entity e = Evento.entidad(s, p);
                            if (e != null && c != null && !Cabina.dentro(s, "pas_te", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                                ir(s, e, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 90F);
                            }
                        }
                        puertas(s, "pas_te", false);
                        fase = Fase.LLEGANDO;
                        desde = t + 1000000;
                        apagonFin = t + 50;
                    }
                }
            }
            case LLEGANDO -> {
                if (desde > t && t >= apagonFin) {
                    desde = t + 2000000;
                    apagonFin = Long.MAX_VALUE;
                    Apagon.hacer(s, s.getPlayerList().getPlayers(), 60, () -> llegar(s));
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
                        if (p.bot && !enSalon(s, e) && dt >= 60) {
                            BlockPos c = Complejo.pos(s, "piano.entrada");
                            if (c != null) {
                                ir(s, e, c.getX() + 0.5 + R.nextInt(3), c.getY(), c.getZ() + 0.5 - 2 + R.nextInt(5), 270F);
                            }
                        }
                        todos &= enSalon(s, e);
                    }
                    if (n > 0 && todos || dt >= esperaSalon) {
                        hablar(s);
                    }
                }
            }
            case LUGARES -> {
                if (t >= apagonFin) {
                    nuevaRonda(s, true);
                }
            }
            case MUESTRA -> {
                // la bailarina toca: cada T_NOTA ticks se ilumina la siguiente tecla (en todos los pianos) y suena
                long fin0 = 30;
                if (dt >= fin0 && (dt - fin0) % T_NOTA == 0) {
                    int i = (int) ((dt - fin0) / T_NOTA);
                    if (i < melodia.size()) {
                        nota(s, melodia.get(i), null, 3F);
                        for (UUID id : juegan) {
                            Integer k = piano.get(id);
                            if (k != null) {
                                nota(s, melodia.get(i), tapete(s, k), 0.8F);
                            }
                        }
                    }
                }
                if (dt >= fin0 + (long) melodia.size() * T_NOTA + 16) {
                    fase = Fase.TOCA;
                    desde = t;
                    for (Jugador j : jug.values()) {
                        j.desdeNota = t;
                    }
                    if (bailarina != null) {
                        bailarina.estado(Bailarina.QUIETA);
                    }
                    Anuncio.sonido(s, SoundEvents.NOTE_BLOCK_BELL.get(), 1F, 1.4F);
                    Pantallas.modo(s, "texto", Component.translatable(MOD + ".piano.pantalla.toquen").getString());
                    enviar(s);
                }
            }
            case TOCA -> tocaTick(s);
            case REVISA -> {
                if (dt >= 60) {
                    despues(s);
                }
            }
            case CAE -> {
                if (dt == 30) {
                    eliminarCaidos(s);
                    enviar(s);
                }
                if (dt >= 70) {
                    if (cayeron >= cuota || juegan.size() <= 1) {
                        fin(s);
                    } else {
                        // quedan por caer: vuelven a jugar todos los que siguen en su piano (los salvados solo se salvaron de esa caída)
                        juegan.clear();
                        salvados.clear();
                        desempate = false;
                        for (Map.Entry<UUID, Integer> en : piano.entrySet()) {
                            Pieza p = ev.pieza(en.getKey());
                            if (p != null && p.vivo() && !cae.contains(en.getKey()) && !caidos.contains(en.getKey())) {
                                juegan.add(en.getKey());
                            }
                        }
                        if (juegan.size() <= 1) {
                            fin(s);
                        } else {
                            if (bailarina != null) {
                                bailarina.estado(Bailarina.QUIETA);
                            }
                            nuevaRonda(s, true);
                        }
                    }
                }
            }
            case FIN -> {
                if (aFinal > 0 && t >= aFinal) {
                    aFinal = 0;
                    if (Veintiuno.encadenar && Complejo.construido(s, "veintiuno")) {
                        Coleccionista.LOGGER.info("COLECCIONISTA_PIANO a_la_final");
                        Veintiuno.llamar(s);
                        return;
                    }
                }
            }
            default -> {
            }
        }
        if (t % 20 == 0 && (fase == Fase.MUESTRA || fase == Fase.TOCA || fase == Fase.REVISA)) {
            vigilar(s);
        }
        if (t % 20 == 0) {
            enviar(s);
        }
    }

    /** Nadie se sale de su piano mientras toca la bailarina (si alguien se reconecta, vuelve a su tapete). */
    private static void vigilar(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Map.Entry<UUID, Integer> en : piano.entrySet()) {
            if (!juegan.contains(en.getKey()) || fase == Fase.TOCA) {
                continue;
            }
            Pieza p = ev.pieza(en.getKey());
            Entity e = p == null || !p.vivo() ? null : Evento.entidad(s, p);
            if (e != null && region(s, e, en.getValue()) == -2) {
                alTapete(s, e, en.getValue());
            }
        }
    }

    // ------------------------------------------------------------------ luces

    private static void luces(MinecraftServer s, boolean bajas) {
        if (lucesBajas == bajas) {
            return;
        }
        lucesBajas = bajas;
        ServerLevel l = s.overworld();
        for (int i = 0; i < 400; i++) {
            BlockPos p = Complejo.pos(s, "piano.luz." + i);
            if (p == null) {
                break;
            }
            int nivel = Math.round(Complejo.yaw("piano.luz." + i));
            int n = bajas && nivel < 13 ? 8 : nivel;
            BlockState st = l.getBlockState(p);
            if (st.isAir() || st.is(Blocks.LIGHT)) {
                l.setBlock(p, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, n), 2);
            }
        }
    }

    // ------------------------------------------------------------------ el final

    public static boolean terminar(MinecraftServer s) {
        if (fase == Fase.NADA || fase == Fase.FIN) {
            return false;
        }
        fin(s);
        return true;
    }

    private static void fin(MinecraftServer s) {
        Pantallas.callar(s);
        fase = Fase.FIN;
        desde = t;
        Evento ev = Evento.get(s);
        ev.reloj(s, 0);
        luces(s, false);
        if (bailarina != null) {
            bailarina.estado(Bailarina.QUIETA);
        }
        // los que quedan, fuera de los pianos (junto a la salida)
        BlockPos sal = Complejo.pos(s, "piano.salida");
        int n = 0;
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null && sal != null) {
                ir(s, e, sal.getX() + 0.5 + (n / 3) * 1.4, sal.getY(), sal.getZ() + 0.5 - 1.4 + (n % 3) * 1.4, 270F);
                n++;
            }
        }
        Pantallas.hablar(s, "piano_fin", () -> {
            Evento.get(s).prueba(s, Component.translatable(MOD + ".piano.fin_capitulo").getString());
            Coleccionista.LOGGER.info("COLECCIONISTA_CAPITULO6 fin vivos={}", Evento.get(s).vivos().size());
            if (fase == Fase.FIN) {
                aFinal = t + 60;
            }
        });
        Anuncio.titulo(s, Component.translatable(MOD + ".piano.fin.titulo"), Component.translatable(MOD + ".piano.fin.sub", ev.vivos().size()));
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_PIANO fin cayeron={} vivos={}", cayeron, ev.vivos().size());
    }

    // ------------------------------------------------------------------ el estado para los clientes

    public static CompoundTag estado() {
        CompoundTag e = new CompoundTag();
        e.putString("fase", fase.name());
        e.putInt("ronda", ronda);
        e.putInt("largo", largo);
        e.putBoolean("desempate", desempate);
        e.putString("resultado", resultado);
        if (srv != null) {
            e.putLong("desde", srv.overworld().getGameTime() - (t - desde));
            if (fase == Fase.MUESTRA) {
                e.put("melodia", new IntArrayTag(melodia.stream().mapToInt(Integer::intValue).toArray()));
                e.putInt("tNota", T_NOTA);
            }
            Evento ev = Evento.get(srv);
            ListTag l = new ListTag();
            for (Map.Entry<UUID, Integer> en : piano.entrySet()) {
                CompoundTag c = new CompoundTag();
                UUID id = en.getKey();
                c.putUUID("id", id);
                Pieza p = ev.pieza(id);
                c.putString("nombre", p == null ? "?" : p.nombre);
                c.putInt("piano", en.getValue());
                BlockPos b = Complejo.pos(srv, "piano.anillo." + en.getValue());
                if (b != null) {
                    c.putIntArray("pos", new int[] {b.getX(), b.getY(), b.getZ()});
                }
                c.putBoolean("juega", juegan.contains(id));
                c.putBoolean("salvado", salvados.contains(id));
                c.putBoolean("caido", caidos.contains(id) || cae.contains(id));
                c.putBoolean("vivo", p != null && p.vivo());
                Jugador j = jug.get(id);
                if (j != null) {
                    c.putInt("nota", j.nota);
                    c.putBoolean("mal", j.mal);
                    c.putBoolean("listo", j.listo);
                    c.putInt("destello", j.destello);
                    c.putBoolean("destelloBien", j.destelloBien);
                    c.putLong("destelloEn", j.destelloEn);
                    c.putLong("desdeNota", srv.overworld().getGameTime() - (t - j.desdeNota));
                }
                l.add(c);
            }
            e.put("pianos", l);
            e.putInt("segundosNota", segundosNota);
        }
        return e;
    }

    private static void enviar(MinecraftServer s) {
        srv = s;
        PlayerStore.Sync.toAll(s, KEY, estado());
    }

    public static String resumen() {
        return fase.name() + " ronda=" + ronda + " notas=" + largo + " juegan=" + juegan.size() + " cayeron=" + cayeron + "/" + cuota
                + " desempate=" + desempate;
    }

    public static Objetivo.Meta objetivo(MinecraftServer s, ServerPlayer sp) {
        if (fase == Fase.PASILLO && !Cabina.dentro(s, "pas_te", sp, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
            return Objetivo.Meta.en(Complejo.pos(s, "pas_te.centro"), 1.5, MOD + ".obj.pasillo");
        }
        if (fase == Fase.LLEGANDO && desde <= t && !enSalon(s, sp)) {
            return Objetivo.Meta.en(Complejo.pos(s, "piano.entrada"), 1.5, MOD + ".obj.salon_baile");
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

    /** Quita todo: la bailarina y vuelve a poner el piso de los pianos que se abrieron (y la luz). */
    private static void limpiarTodo(MinecraftServer s) {
        ServerLevel l = s.overworld();
        BlockPos o = Complejo.origen(s, "piano");
        if (o != null) {
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(o.getX(), o.getY() - 4, o.getZ() - 2, o.getX() + 66, o.getY() + 18,
                    o.getZ() + 46);
            for (Bailarina b : l.getEntitiesOfClass(Bailarina.class, box)) {
                b.discard();
            }
            // el piso de los pianos (por si alguno se abrió)
            for (int k = 0; k < SalaPiano.PIANOS; k++) {
                BlockPos b = Complejo.pos(s, "piano.anillo." + k);
                if (b == null) {
                    continue;
                }
                for (int dx = -2; dx <= 3; dx++) {
                    for (int dz = -2; dz <= 3; dz++) {
                        BlockPos q = b.offset(dx, 0, dz);
                        if (!l.getBlockState(q).isAir()) {
                            continue;
                        }
                        String bloque = MOD + ":marmol_negro";
                        for (int i = 0; i < SalaPiano.TECLA.length; i++) {
                            int tx = SalaPiano.TECLA[i][0], tz = SalaPiano.TECLA[i][1];
                            if (dx >= tx && dx <= tx + 1 && dz >= tz && dz <= tz + 1) {
                                bloque = "minecraft:" + SalaPiano.COLOR[i] + "_concrete";
                            }
                        }
                        BlockState st = Complejo.estado(bloque);
                        if (st != null) {
                            l.setBlock(q, st, 2);
                        }
                    }
                }
            }
            lucesBajas = true;
            luces(s, false);
        }
        bailarina = null;
        piano.clear();
        juegan.clear();
        salvados.clear();
        jug.clear();
        melodia.clear();
        cae.clear();
        caidos.clear();
        extras.clear();
        ronda = 0;
        largo = 0;
        cayeron = 0;
        desempate = false;
        resultado = "";
        aFinal = 0;
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase == Fase.HABLANDO || fase == Fase.FIN) {
            Pantallas.callar(s);
        }
        if (fase != Fase.NADA) {
            Pantallas.modo(s, "apagada", "");
        }
        limpiarTodo(s);
        // lo de las pruebas (quién falla a propósito) solo se borra al limpiar a mano
        fallan.clear();
        fallanPorRonda.clear();
        fase = Fase.NADA;
        enviar(s);
    }

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
                aLosPianos(s);
                return true;
            }
            case MUESTRA -> {
                desde = t - 10000;
                return true;
            }
            case TOCA -> {
                // a los que no han terminado se les acaba el tiempo
                for (UUID id : juegan) {
                    Jugador j = jug.get(id);
                    if (j != null && !j.mal && !j.listo) {
                        j.desdeNota = t - segundosNota * 20L - 1;
                    }
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** {@code /evento piano fallar <pieza>}: esa pieza se equivoca en su siguiente nota (para las pruebas). */
    public static void fallar(UUID id) {
        fallan.add(id);
    }

    /** {@code /evento piano fallan <n> [<n2> ...]}: en la siguiente ronda se equivocan n piezas al azar (y en la otra n2...). */
    public static void fallanEnRondas(List<Integer> n) {
        fallanPorRonda.clear();
        fallanPorRonda.addAll(n);
    }

    /** {@code /evento piano pisar <pieza> <1-8>}: como si esa pieza pisara la tecla (para las pruebas). */
    public static boolean pisar(MinecraftServer s, UUID id, int tecla) {
        if (fase != Fase.TOCA || !juegan.contains(id) || tecla < 0 || tecla >= SalaPiano.TECLA.length) {
            return false;
        }
        pisa(s, id, tecla);
        return true;
    }

    /** La tecla que sigue en la melodía para esa pieza (-1 si no está tocando): para las pruebas. */
    public static int siguiente(UUID id) {
        Jugador j = jug.get(id);
        return fase != Fase.TOCA || j == null || j.mal || j.listo || j.nota >= melodia.size() ? -1 : melodia.get(j.nota);
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        return fase == Fase.TOCA;
    }
}
