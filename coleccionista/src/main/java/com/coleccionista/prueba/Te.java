package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import com.coleccionista.bot.PiezaBot;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Apagon;
import com.coleccionista.evento.Cabina;
import com.coleccionista.evento.Cadaver;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Cuerpos;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.ManoGigante;
import com.coleccionista.evento.Objetivo;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.evento.Silla;
import com.coleccionista.mapa.Pasaje;
import com.coleccionista.mapa.SalaTe;
import com.coleccionista.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Capítulo 5 · La hora del té. Del teatrito de Don Resorte pasan (por el pasillo de servicio) al salón de té. El Coleccionista les pide
 * que se sienten (en los sillones de dos) con la persona en la que más confíen. Ya hechas las parejas, un apagón: todos aparecen en jaulas
 * de pájaro colgadas del techo alrededor de la mesa, menos la primera pareja, sentada frente a frente en las sillas de la mesa. La tele
 * explica: piedra, papel o tijera; el que pierde bebe de uno de sus cinco cálices, y uno de los cinco está envenenado (al azar). Ah, y solo
 * uno de cada pareja sale.
 * <p>
 * Los duelos van uno por uno: cada ronda los dos eligen en secreto (si no, al azar); dos manos gigantes de porcelana salen de la mesa, se
 * sacuden tres veces y muestran las jugadas. El que pierde agarra uno de sus cálices (la mano es el mouse) y se le ve bebiendo: a salvo, se
 * marea y sigue; envenenado, tose, se pone verde y se va de espaldas con la silla. Luego, en un parpadeo de oscuridad, el muerto queda en
 * una jaula (verde, tieso) y el ganador en otra, y la siguiente pareja aparece en la mesa. Si sobra alguien sin pareja, juega contra la
 * muñeca del Coleccionista. Al final, los ganadores salen; los muertos se quedan colgados.
 */
public final class Te {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":te";
    public static final String KEY_MIO = MOD + ":te_mio";
    public static final String ACCION = MOD + ":te_accion";
    public static final String PRUEBA = "La hora del té";
    /** Ajustes (los cambia el organizador con /evento te ...). */
    public static int segundosParejas = 60, segundosJugada = 12, segundosCaliz = 20;
    public static int esperaPasillo = 1200, esperaSalon = 900;
    /** Al terminar la caja sorpresa, pasan solos al salón de té. */
    public static boolean encadenar = true;
    /** Para las pruebas: cuál cáliz tiene el veneno (-1: al azar) y que los bots jueguen al instante. */
    public static int venenoFijo = -1;
    public static boolean rapido;
    /** Cuántos cálices tiene cada quien. */
    public static final int CALICES = 5;
    /** Cuánto dura beber (ticks): a salvo, se marea y sigue; envenenado, a los {@link #T_MUERE} se lo lleva el Coleccionista. */
    public static final int T_TRAGA = 60, T_SALVO = 120, T_MUERE = 130;

    public enum Fase {
        NADA, PASILLO, LLEGANDO, HABLANDO, PAREJAS, JAULAS, REGLAS, JUGADA, MANOS, CALIZ, BEBE, MUERE, CAMBIO, FIN
    }

    private static Fase fase = Fase.NADA;
    private static long t, desde, apagonFin;
    private static final Random R = new Random();

    /** Los asientos de los sillones (para elegir pareja). */
    private static final List<Silla> sofa = new ArrayList<>();
    private static final Map<UUID, Long> botDesde = new HashMap<>();
    private static final Map<UUID, Integer> botVa = new HashMap<>();
    /** Las parejas, en el orden de los duelos ({a, b}; b null = contra la muñeca). */
    private static final List<UUID[]> parejas = new ArrayList<>();
    private static int duelo = -1;
    /** En qué jaula está cada quien (vivo o muerto). */
    private static final Map<UUID, Integer> jaulaDe = new HashMap<>();
    private static final Map<Integer, Cadaver> muertosEnJaula = new HashMap<>();
    /** El duelo de ahora: las sillas, las manos, los cálices que quedan (bits), cuál tiene veneno, las jugadas y quién perdió. */
    private static final Silla[] silla = new Silla[2];
    private static final ManoGigante[] mano = new ManoGigante[2];
    private static final int[] copas = new int[2], veneno = new int[2], jugada = new int[2];
    private static final long[] botJuega = new long[2];
    private static int perdedor = -1, ganador = -1, bebida = -1, ronda;
    private static boolean muere, muneca, munecaRota;
    private static long bebeDesde, munecaJuegaEn, munecaBebeEn;
    private static String resultado = "";
    private static boolean lucesBajas;
    private static final List<UUID> ganadores = new ArrayList<>();
    private static MinecraftServer srv;

    private Te() {
    }

    public static void init() {
        AxelisCore.onServerTick(Te::tick);
        AxelisCore.onLogin(p -> {
            PlayerStore.Sync.toPlayer(p, KEY, estado());
            mio(Plat.server(p), p);
        });
        Actions.on(ACCION, Te::accion);
        // aquí nadie se lastima: solo se muere con el veneno
        MinecraftForge.EVENT_BUS.addListener((LivingHurtEvent e) -> {
            if (fase != Fase.NADA && fase != Fase.PASILLO && e.getEntity() instanceof ServerPlayer) {
                e.setCanceled(true);
            }
        });
        // para sentarse en un sillón basta con darle clic derecho (el asiento de verdad es invisible)
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (!eligiendo() || e.getLevel().isClientSide() || !(e.getEntity() instanceof ServerPlayer sp)) {
                return;
            }
            Silla si = asientoEn(e.getPos());
            if (si != null) {
                e.setCanceled(true);
                intentarSentar(sp, si);
            }
        });
    }

    public static Fase fase() {
        return fase;
    }

    public static boolean abierta() {
        return fase != Fase.NADA;
    }

    private static boolean eligiendo() {
        return fase == Fase.HABLANDO || fase == Fase.PAREJAS;
    }

    /** ¿Puede sentarse en esa silla? Solo en los sillones, mientras eligen pareja. */
    public static boolean puedeSentarse(ServerPlayer sp, Silla si) {
        Pieza p = Evento.get(sp.server).pieza(sp);
        return eligiendo() && sofa.contains(si) && p != null && p.vivo();
    }

    // ------------------------------------------------------------------ del teatrito al salón de té

    /** Al terminar la caja sorpresa: se abren las puertas de su pasillo y el Coleccionista los llama. */
    public static Component llamar(MinecraftServer s) {
        if (!Complejo.construido(s, "te")) {
            return Component.translatable(MOD + ".te.sin_mapa");
        }
        if (!Complejo.construido(s, "sorpresa")) {
            return llegar(s);
        }
        Pruebas.cerrarOtras(s, "te");
        limpiarTodo(s);
        puertas(s, "pas_sor", true);
        fase = Fase.PASILLO;
        desde = t;
        Evento ev = Evento.get(s);
        ev.prueba(s, Component.translatable(MOD + ".caja.prueba_pasillo").getString());
        ev.reloj(s, 0);
        Anuncio.titulo(s, Component.empty(), Component.translatable(MOD + ".caja.al_pasillo"));
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_TE llamar");
        return null;
    }

    /** Los vivos pasan (sin que se note) del pasillo del teatrito al del salón de té y se abren las puertas al salón. */
    public static Component llegar(MinecraftServer s) {
        if (!Complejo.construido(s, "te")) {
            return Component.translatable(MOD + ".te.sin_mapa");
        }
        if (fase == Fase.NADA) {
            Pruebas.cerrarOtras(s, "te");
            limpiarTodo(s);
        }
        Evento ev = Evento.get(s);
        List<Entity> enPasillo = new ArrayList<>(), fuera = new ArrayList<>();
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null) {
                continue;
            }
            if (Complejo.construido(s, "sorpresa") && Cabina.dentro(s, "pas_sor", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                enPasillo.add(e);
            } else {
                fuera.add(e);
            }
        }
        if (!enPasillo.isEmpty()) {
            Cabina.trasladar(s, "pas_sor", "pas_te", enPasillo, List.of());
        }
        BlockPos c = Complejo.pos(s, "pas_te.centro");
        int n = 0;
        for (Entity e : fuera) {
            if (c == null) {
                break;
            }
            ir(s, e, c.getX() + 0.5 - 1.5 + (n % 3) * 1.5, c.getY(), c.getZ() + 0.5 - 1 + (n / 3 % 4), 270F);
            n++;
        }
        puertas(s, "pas_sor", false);
        puertas(s, "pas_te", true);
        fase = Fase.LLEGANDO;
        desde = t;
        ev.prueba(s, Component.translatable(MOD + ".te.prueba").getString());
        ev.reloj(s, 0);
        Pantallas.modo(s, "apagada", "");
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_TE llegan pasillo={} fuera={}", enPasillo.size(), fuera.size());
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
        BlockPos o = Complejo.origen(s, "te");
        if (o == null) {
            return false;
        }
        double x = e.getX() - o.getX(), z = e.getZ() - o.getZ();
        return x >= SalaTe.HX0 && x <= SalaTe.HX1 + 1 && z >= SalaTe.HZ0 && z <= SalaTe.HZ1 + 1;
    }

    // ------------------------------------------------------------------ el discurso y las parejas

    private static void hablar(MinecraftServer s) {
        fase = Fase.HABLANDO;
        desde = t;
        BlockPos c = Complejo.pos(s, "te.entrada");
        for (Pieza p : Evento.get(s).vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null && c != null && !enSalon(s, e)) {
                ir(s, e, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 270F);
            }
        }
        puertas(s, "pas_te", false);
        crearSofa(s);
        Escena.presentacion(s, "V", MOD + ".te.titulo", MOD + ".te.lema");
        Pantallas.soloGrande(s, true);
        Pantallas.hablar(s, "te", () -> empezarParejas(s));
        Evento.get(s).prueba(s, PRUEBA);
        // los bots se van sentando mientras habla (unos antes, otros después)
        botDesde.clear();
        for (Pieza p : Evento.get(s).vivos()) {
            if (p.bot) {
                botDesde.put(p.id, t + (rapido ? 10 : 100 + R.nextInt(500)));
            }
        }
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_TE habla");
    }

    private static void crearSofa(MinecraftServer s) {
        ServerLevel l = s.overworld();
        for (Silla si : sofa) {
            si.soltar();
            si.discard();
        }
        sofa.clear();
        for (int k = 0; k < 40; k++) {
            boolean alguno = false;
            for (String lado : new String[] {".a", ".b"}) {
                String m = "te.sofa." + k + lado;
                BlockPos b = Complejo.pos(s, m);
                if (b == null) {
                    continue;
                }
                alguno = true;
                Silla si = ModRegistry.SILLA.get().create(l);
                if (si == null) {
                    continue;
                }
                float yaw = Complejo.yaw(m);
                si.moveTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, yaw, 0F);
                si.setYHeadRot(yaw);
                si.grupo = "te";
                si.oculta(true);
                l.addFreshEntity(si);
                sofa.add(si);
            }
            if (!alguno) {
                break;
            }
        }
    }

    private static void empezarParejas(MinecraftServer s) {
        if (fase != Fase.HABLANDO) {
            return;
        }
        fase = Fase.PAREJAS;
        desde = t;
        Evento.get(s).reloj(s, segundosParejas);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".te.pantalla.parejas").getString());
        enviar(s);
    }

    private static Silla asientoEn(BlockPos p) {
        for (Silla si : sofa) {
            if (si.isAlive() && (si.blockPosition().equals(p) || si.blockPosition().equals(p.below()))) {
                return si;
            }
        }
        return null;
    }

    private static void intentarSentar(ServerPlayer sp, Silla si) {
        if (si == null || !si.isAlive() || sp.getVehicle() == si || !puedeSentarse(sp, si)) {
            return;
        }
        if (si.isVehicle()) {
            Plat.msg(sp, Component.translatable(MOD + ".caja.ocupado").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (sp.getVehicle() instanceof Silla vieja) {
            vieja.soltar();
        }
        sp.startRiding(si, true);
        sp.level().playSound(null, si.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 0.8F);
        Coleccionista.LOGGER.info("COLECCIONISTA_TE sienta {} sofa={}", sp.getGameProfile().getName(), sofa.indexOf(si) / 2);
        enviar(sp.server);
    }

    /** El sillón (k) en el que va sentado, o -1. */
    private static int sofaDe(Entity e) {
        if (!(e.getVehicle() instanceof Silla v)) {
            return -1;
        }
        int i = sofa.indexOf(v);
        return i < 0 ? -1 : i / 2;
    }

    /** Un asiento libre: de preferencia junto a alguien que va solo (para que nadie se quede sin pareja); si no, en un sillón vacío. */
    private static int elegirAsiento() {
        List<Integer> conUno = new ArrayList<>(), vacios = new ArrayList<>();
        for (int k = 0; k + 1 < sofa.size(); k += 2) {
            Silla a = sofa.get(k), b = sofa.get(k + 1);
            boolean va = a.isVehicle() || botVa.containsValue(k), vb = b.isVehicle() || botVa.containsValue(k + 1);
            if (va && !vb) {
                conUno.add(k + 1);
            } else if (!va && vb) {
                conUno.add(k);
            } else if (!va) {
                vacios.add(k);
            }
        }
        if (!conUno.isEmpty()) {
            return conUno.get(R.nextInt(conUno.size()));
        }
        return vacios.isEmpty() ? -1 : vacios.get(R.nextInt(vacios.size()));
    }

    private static void parejasTick(MinecraftServer s) {
        Evento ev = Evento.get(s);
        int n = 0, sentados = 0;
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null) {
                continue;
            }
            n++;
            if (sofaDe(e) >= 0) {
                sentados++;
                botVa.remove(p.id);
                continue;
            }
            Long d = botDesde.get(p.id);
            if (!p.bot || d == null || t < d) {
                continue;
            }
            Integer va = botVa.get(p.id);
            if (va == null || va >= sofa.size() || sofa.get(va).isVehicle()) {
                botVa.remove(p.id);
                int a = elegirAsiento();
                if (a < 0) {
                    continue;
                }
                va = a;
                botVa.put(p.id, va);
            }
            Silla si = sofa.get(va);
            if (e.distanceToSqr(si) < 2.2 * 2.2 || t - d > (rapido ? 5 : 220)) {
                sentar(s, e, si);
                botVa.remove(p.id);
                sentados++;
            } else if (e instanceof PiezaBot b && (t - d) % 20 == 0) {
                b.getNavigation().moveTo(si.getX() + 1.2, si.getY(), si.getZ(), 1.0);
            }
        }
        if (fase == Fase.PAREJAS && (n > 0 && sentados >= n || t - desde >= segundosParejas * 20L)) {
            formarParejas(s);
        }
    }

    /** Ya eligieron (o se acabó el minuto): las parejas son los que comparten sillón; a los demás los empareja el Coleccionista. */
    private static void formarParejas(MinecraftServer s) {
        Evento ev = Evento.get(s);
        parejas.clear();
        List<UUID> solos = new ArrayList<>();
        Map<Integer, List<UUID>> porSofa = new HashMap<>();
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null) {
                continue;
            }
            int k = sofaDe(e);
            if (k < 0) {
                solos.add(p.id);
            } else {
                porSofa.computeIfAbsent(k, x -> new ArrayList<>()).add(p.id);
            }
        }
        for (List<UUID> l : porSofa.values()) {
            if (l.size() >= 2) {
                parejas.add(new UUID[] {l.get(0), l.get(1)});
                for (int i = 2; i < l.size(); i++) {
                    solos.add(l.get(i));
                }
            } else {
                solos.addAll(l);
            }
        }
        Collections.shuffle(solos, R);
        for (int i = 0; i + 1 < solos.size(); i += 2) {
            parejas.add(new UUID[] {solos.get(i), solos.get(i + 1)});
        }
        Collections.shuffle(parejas, R);
        if (solos.size() % 2 == 1) {
            // el que se quedó sin pareja juega al final, contra la muñeca del Coleccionista
            parejas.add(new UUID[] {solos.get(solos.size() - 1), null});
        }
        // en cada pareja, quién va a la silla del oeste y quién a la del este: al azar
        for (UUID[] pr : parejas) {
            if (pr[1] != null && R.nextBoolean()) {
                UUID x = pr[0];
                pr[0] = pr[1];
                pr[1] = x;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (UUID[] pr : parejas) {
            sb.append(nombre(ev, pr[0])).append('+').append(pr[1] == null ? "muneca" : nombre(ev, pr[1])).append(' ');
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_TE parejas={} {}", parejas.size(), sb.toString().trim());
        Pantallas.callar(s);
        fase = Fase.JAULAS;
        desde = t;
        apagonFin = t + 70;
        ev.reloj(s, 0);
        Apagon.hacer(s, s.getPlayerList().getPlayers(), 60, () -> aLasJaulas(s));
        enviar(s);
    }

    private static String nombre(Evento ev, UUID id) {
        Pieza p = id == null ? null : ev.pieza(id);
        return p == null ? "?" : p.nombre;
    }

    /** En lo oscuro: todos a sus jaulas, menos la primera pareja, que queda en las sillas de la mesa. */
    private static void aLasJaulas(MinecraftServer s) {
        for (Silla si : sofa) {
            si.soltar();
            si.discard();
        }
        sofa.clear();
        botVa.clear();
        crearManos(s);
        duelo = 0;
        Evento ev = Evento.get(s);
        UUID[] pr = parejas.isEmpty() ? null : parejas.get(0);
        jaulaDe.clear();
        int k = 0;
        List<Integer> orden = ordenJaulas();
        for (Pieza p : ev.vivos()) {
            if (pr != null && (p.id.equals(pr[0]) || p.id.equals(pr[1]))) {
                continue;
            }
            Entity e = Evento.entidad(s, p);
            if (e == null || k >= orden.size()) {
                continue;
            }
            int j = orden.get(k++);
            jaulaDe.put(p.id, j);
            aJaula(s, e, j);
        }
        aLaMesa(s, pr);
        luces(s, true);
        Coleccionista.LOGGER.info("COLECCIONISTA_TE jaulas={}", jaulaDe.size());
    }

    /** Las jaulas en un orden que reparte a la gente alrededor de la mesa (no todas juntas de un lado). */
    private static List<Integer> ordenJaulas() {
        List<Integer> l = new ArrayList<>();
        for (int k = 0; k < SalaTe.JAULAS; k += 2) {
            l.add(k);
        }
        for (int k = 1; k < SalaTe.JAULAS; k += 2) {
            l.add(k);
        }
        return l;
    }

    private static Vec3 jaula(MinecraftServer s, int j) {
        BlockPos b = Complejo.pos(s, "te.jaula." + j);
        return b == null ? null : new Vec3(b.getX() + 0.5, b.getY() + 0.07, b.getZ() + 0.5);
    }

    private static void aJaula(MinecraftServer s, Entity e, int j) {
        Vec3 p = jaula(s, j);
        if (p != null) {
            ir(s, e, p.x, p.y, p.z, Complejo.yaw("te.jaula." + j));
        }
    }

    private static int jaulaLibre() {
        for (int j : ordenJaulas()) {
            if (!jaulaDe.containsValue(j) && !muertosEnJaula.containsKey(j)) {
                return j;
            }
        }
        return -1;
    }

    private static void crearManos(MinecraftServer s) {
        BlockPos m = Complejo.pos(s, "te.mesa");
        if (m == null) {
            return;
        }
        ServerLevel l = s.overworld();
        double y = m.getY() - 1 + 0.975;
        for (int lado = 0; lado < 2; lado++) {
            if (mano[lado] != null && mano[lado].isAlive()) {
                continue;
            }
            ManoGigante g = ModRegistry.MANO_GIGANTE.get().create(l);
            if (g == null) {
                continue;
            }
            double x = SalaTeOrigen.x(s) + SalaTe.CX + (lado == 0 ? -2.2 : 2.2);
            double z = SalaTeOrigen.z(s) + SalaTe.CZ;
            float yaw = lado == 0 ? 270F : 90F;
            g.moveTo(x, y, z, yaw, 0F);
            g.setYHeadRot(yaw);
            l.addFreshEntity(g);
            mano[lado] = g;
        }
    }

    /** Las sillas de la mesa (nuevas) con la pareja del duelo sentada (y quieta). */
    private static void aLaMesa(MinecraftServer s, UUID[] pr) {
        ServerLevel l = s.overworld();
        Evento ev = Evento.get(s);
        for (int lado = 0; lado < 2; lado++) {
            if (silla[lado] != null) {
                silla[lado].soltar();
                silla[lado].discard();
                silla[lado] = null;
            }
            String m = "te.silla." + lado;
            BlockPos b = Complejo.pos(s, m);
            Silla si = b == null ? null : ModRegistry.SILLA.get().create(l);
            if (si == null) {
                continue;
            }
            float yaw = Complejo.yaw(m);
            si.moveTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, yaw, 0F);
            si.setYHeadRot(yaw);
            si.grupo = "te";
            l.addFreshEntity(si);
            silla[lado] = si;
            UUID id = pr == null ? null : pr[lado];
            Pieza p = id == null ? null : ev.pieza(id);
            Entity e = p == null ? null : Evento.entidad(s, p);
            if (e != null) {
                sentar(s, e, si);
                si.cerrar(true, false);
            }
        }
        muneca = pr != null && pr[1] == null;
        munecaRota = false;
        // los cálices ya están servidos cuando se sientan (el veneno se decide al empezar el duelo)
        copas[0] = copas[1] = (1 << CALICES) - 1;
    }

    // ------------------------------------------------------------------ los duelos

    private static void reglas(MinecraftServer s) {
        fase = Fase.REGLAS;
        desde = t;
        Pantallas.soloGrande(s, true);
        Pantallas.hablar(s, "te_reglas", () -> {
            if (fase == Fase.REGLAS) {
                empezarDuelo(s);
            }
        });
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_TE reglas");
    }

    private static UUID lado(int l) {
        return duelo < 0 || duelo >= parejas.size() ? null : parejas.get(duelo)[l];
    }

    private static void empezarDuelo(MinecraftServer s) {
        if (duelo < 0 || duelo >= parejas.size()) {
            fin(s);
            return;
        }
        Evento ev = Evento.get(s);
        for (int l = 0; l < 2; l++) {
            copas[l] = (1 << CALICES) - 1;
            veneno[l] = venenoFijo >= 0 && venenoFijo < CALICES ? venenoFijo : R.nextInt(CALICES);
        }
        ronda = 0;
        perdedor = -1;
        ganador = -1;
        String a = nombre(ev, lado(0)), b = muneca ? Component.translatable(MOD + ".te.muneca").getString() : nombre(ev, lado(1));
        Anuncio.titulo(s, Component.translatable(MOD + ".te.duelo.titulo", duelo + 1, parejas.size()).withStyle(ChatFormatting.GOLD),
                Component.translatable(MOD + ".te.duelo.sub", a, b));
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".te.pantalla.duelo", a, b).getString());
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.6F);
        Coleccionista.LOGGER.info("COLECCIONISTA_TE duelo {} {} vs {} veneno={},{}", duelo + 1, a, b, veneno[0], veneno[1]);
        nuevaRonda(s);
    }

    private static void nuevaRonda(MinecraftServer s) {
        fase = Fase.JUGADA;
        desde = t;
        ronda++;
        jugada[0] = jugada[1] = ManoGigante.NADA;
        perdedor = -1;
        bebida = -1;
        resultado = "";
        for (ManoGigante m : mano) {
            if (m != null) {
                m.empunar();
            }
        }
        Evento ev = Evento.get(s);
        ev.reloj(s, segundosJugada);
        for (int l = 0; l < 2; l++) {
            Pieza p = lado(l) == null ? null : ev.pieza(lado(l));
            botJuega[l] = p != null && p.bot ? t + (rapido ? 6 : 25 + R.nextInt(70)) : -1;
        }
        munecaJuegaEn = muneca ? t + 20 + R.nextInt(40) : -1;
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mio(s, sp);
        }
        enviar(s);
    }

    /** Alguien eligió su jugada (1 piedra, 2 papel, 3 tijera). Cuando ya están las dos, las manos se sacuden. */
    public static boolean jugar(MinecraftServer s, int l, int g) {
        if (fase != Fase.JUGADA || l < 0 || l > 1 || g < ManoGigante.PIEDRA || g > ManoGigante.TIJERA || jugada[l] != ManoGigante.NADA) {
            return false;
        }
        jugada[l] = g;
        Evento ev = Evento.get(s);
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            if (sp.getUUID().equals(lado(l))) {
                mio(s, sp);
            }
        }
        // a todos: un clic de que ya eligió (sin decir qué)
        Anuncio.sonido(s, SoundEvents.STONE_BUTTON_CLICK_ON, 0.6F, 1.4F);
        Coleccionista.LOGGER.info("COLECCIONISTA_TE elige {} ronda={}", nombre(ev, lado(l)), ronda);
        if (jugada[0] != ManoGigante.NADA && jugada[1] != ManoGigante.NADA) {
            revelar(s);
        } else {
            enviar(s);
        }
        return true;
    }

    private static void revelar(MinecraftServer s) {
        fase = Fase.MANOS;
        desde = t;
        Evento.get(s).reloj(s, 0);
        for (int l = 0; l < 2; l++) {
            if (mano[l] != null) {
                mano[l].jugar(jugada[l]);
            }
        }
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mio(s, sp);
        }
        enviar(s);
    }

    /** Piedra le gana a tijera, tijera a papel y papel a piedra. Devuelve el lado que pierde, o -1 si empatan. */
    public static int quienPierde(int a, int b) {
        if (a == b) {
            return -1;
        }
        boolean ganaA = a == ManoGigante.PIEDRA && b == ManoGigante.TIJERA || a == ManoGigante.TIJERA && b == ManoGigante.PAPEL
                || a == ManoGigante.PAPEL && b == ManoGigante.PIEDRA;
        return ganaA ? 1 : 0;
    }

    private static String jugadaNombre(int g) {
        return switch (g) {
            case ManoGigante.PIEDRA -> "piedra";
            case ManoGigante.PAPEL -> "papel";
            case ManoGigante.TIJERA -> "tijera";
            default -> "nada";
        };
    }

    private static void resultadoManos(MinecraftServer s) {
        Evento ev = Evento.get(s);
        int pierde = quienPierde(jugada[0], jugada[1]);
        Coleccionista.LOGGER.info("COLECCIONISTA_TE jugada {}={} {}={} pierde={}", nombre(ev, lado(0)), jugadaNombre(jugada[0]),
                muneca ? "muneca" : nombre(ev, lado(1)), jugadaNombre(jugada[1]), pierde < 0 ? "empate" : pierde == 0 ? nombre(ev, lado(0))
                        : muneca ? "muneca" : nombre(ev, lado(1)));
        if (pierde < 0) {
            resultado = "empate";
            Anuncio.sonido(s, SoundEvents.NOTE_BLOCK_BASS.get(), 1F, 0.7F);
        } else {
            perdedor = pierde;
            resultado = "pierde";
            Anuncio.sonido(s, SoundEvents.NOTE_BLOCK_BASS.get(), 1F, 0.5F);
            Anuncio.sonido(s, SoundEvents.WARDEN_HEARTBEAT, 1F, 1F);
            String quien = pierde == 1 && muneca ? Component.translatable(MOD + ".te.muneca").getString() : nombre(ev, lado(pierde));
            Pantallas.modo(s, "texto", Component.translatable(MOD + ".te.pantalla.bebe", quien).getString());
        }
        enviar(s);
    }

    private static void elegirCaliz(MinecraftServer s) {
        fase = Fase.CALIZ;
        desde = t;
        for (ManoGigante m : mano) {
            if (m != null && m.estado() != ManoGigante.GUARDADA) {
                m.estado(ManoGigante.BAJA);
            }
        }
        Evento ev = Evento.get(s);
        ev.reloj(s, segundosCaliz);
        UUID id = lado(perdedor);
        Pieza p = id == null ? null : ev.pieza(id);
        botJuega[0] = botJuega[1] = -1;
        munecaBebeEn = -1;
        if (perdedor == 1 && muneca) {
            munecaBebeEn = t + 40 + R.nextInt(40);
        } else if (p != null && p.bot) {
            botJuega[perdedor] = t + (rapido ? 8 : 40 + R.nextInt(60));
        }
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mio(s, sp);
        }
        enviar(s);
    }

    private static int calizAlAzar(int l) {
        List<Integer> q = new ArrayList<>();
        for (int k = 0; k < CALICES; k++) {
            if ((copas[l] & (1 << k)) != 0) {
                q.add(k);
            }
        }
        return q.isEmpty() ? -1 : q.get(R.nextInt(q.size()));
    }

    /** El que perdió agarra el cáliz {@code k} y se lo bebe. */
    public static boolean beber(MinecraftServer s, int k) {
        if (fase != Fase.CALIZ || perdedor < 0 || k < 0 || k >= CALICES || (copas[perdedor] & (1 << k)) == 0) {
            return false;
        }
        copas[perdedor] &= ~(1 << k);
        bebida = k;
        muere = k == veneno[perdedor];
        fase = Fase.BEBE;
        desde = t;
        bebeDesde = s.overworld().getGameTime();
        Evento ev = Evento.get(s);
        ev.reloj(s, 0);
        Silla si = silla[perdedor];
        if (si != null) {
            s.overworld().playSound(null, si.getX(), si.getY() + 1, si.getZ(), SoundEvents.GLASS_HIT, SoundSource.PLAYERS, 1F, 1.4F);
        }
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mio(s, sp);
        }
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_TE bebe {} caliz={} quedan={}", perdedor == 1 && muneca ? "muneca" : nombre(ev, lado(perdedor)),
                k + 1, Integer.bitCount(copas[perdedor]));
        return true;
    }

    private static void sonidoMesa(MinecraftServer s, int l, SoundEvent e, float vol, float tono) {
        Silla si = l >= 0 && l < 2 ? silla[l] : null;
        if (si != null) {
            s.overworld().playSound(null, si.getX(), si.getY() + 1.2, si.getZ(), e, SoundSource.PLAYERS, vol, tono);
        }
    }

    /** Lo que pasa mientras bebe: los tragos, el suspenso y, a los 3 segundos, si se salvó o no. */
    private static void bebeTick(MinecraftServer s, long dt) {
        int l = perdedor;
        if (dt >= 24 && dt < T_TRAGA && dt % 9 == 6) {
            sonidoMesa(s, l, SoundEvents.GENERIC_DRINK, 1.2F, 0.85F);
        }
        if (dt < T_TRAGA && dt % 14 == 0) {
            Anuncio.sonido(s, SoundEvents.WARDEN_HEARTBEAT, 0.8F, 1F);
        }
        Evento ev = Evento.get(s);
        UUID id = lado(l);
        Pieza p = id == null ? null : ev.pieza(id);
        Entity e = p == null ? null : Evento.entidad(s, p);
        if (dt == T_TRAGA) {
            if (!muere) {
                resultado = "salvo";
                sonidoMesa(s, l, SoundEvents.PLAYER_BURP, 1F, 0.8F);
                Anuncio.sonido(s, SoundEvents.AMETHYST_BLOCK_CHIME, 1F, 0.9F);
                if (e instanceof ServerPlayer sp) {
                    sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 150, 0, false, false));
                }
                Coleccionista.LOGGER.info("COLECCIONISTA_TE a_salvo {}", l == 1 && muneca ? "muneca" : nombre(ev, id));
            } else {
                resultado = "veneno";
                sonidoMesa(s, l, SoundEvents.WITCH_DRINK, 1.2F, 0.5F);
                sonidoMesa(s, l, SoundEvents.PLAYER_HURT_DROWN, 1.2F, 0.8F);
                Anuncio.sonido(s, SoundEvents.ELDER_GUARDIAN_CURSE, 0.5F, 1.4F);
                if (e instanceof ServerPlayer sp) {
                    sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 1, false, false));
                    sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false));
                    Fx.shake(sp, 1.2F, 60);
                }
                Coleccionista.LOGGER.info("COLECCIONISTA_TE veneno {}", l == 1 && muneca ? "muneca" : nombre(ev, id));
            }
            enviar(s);
        }
        if (muere && dt > T_TRAGA && dt < T_MUERE) {
            // se ahoga: tos, arcadas y burbujas verdes
            if (dt % 11 == 3) {
                sonidoMesa(s, l, SoundEvents.PLAYER_HURT_DROWN, 1F, 0.7F + R.nextFloat() * 0.3F);
            }
            if (dt % 7 == 0 && silla[l] != null) {
                Silla si = silla[l];
                s.overworld().sendParticles(ParticleTypes.ENTITY_EFFECT, si.getX(), si.getY() + 1.5, si.getZ(), 6, 0.25, 0.3, 0.25, 0.4);
                s.overworld().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.defaultBlockState()), si.getX(),
                        si.getY() + 1.4, si.getZ(), 4, 0.15, 0.1, 0.15, 0.1);
            }
        }
        if (!muere && dt >= T_SALVO) {
            nuevaRonda(s);
            return;
        }
        if (muere && dt >= T_MUERE) {
            morir(s);
        }
    }

    /** El veneno hizo efecto: el que perdió queda en la colección (o la muñeca se rompe). */
    private static void morir(MinecraftServer s) {
        Evento ev = Evento.get(s);
        int l = perdedor;
        ganador = 1 - l;
        fase = Fase.MUERE;
        desde = t;
        sonidoMesa(s, l, SoundEvents.WOOD_BREAK, 1.2F, 0.6F);
        sonidoMesa(s, l, SoundEvents.PLAYER_DEATH, 1F, 0.7F);
        if (l == 1 && muneca) {
            munecaRota = true;
            sonidoMesa(s, l, SoundEvents.GLASS_BREAK, 1.5F, 0.6F);
            Silla si = silla[1];
            if (si != null) {
                s.overworld().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_CONCRETE.defaultBlockState()), si.getX(),
                        si.getY() + 1.2, si.getZ(), 40, 0.3, 0.4, 0.3, 0.2);
            }
            Coleccionista.LOGGER.info("COLECCIONISTA_TE muneca_rota");
        } else {
            UUID id = lado(l);
            Pieza p = id == null ? null : ev.pieza(id);
            if (p != null) {
                Cuerpos.VENENO.add(p.id);
                String motivo = Component.translatable(MOD + ".te.motivo").getString();
                if (!ev.eliminarEnPrueba(s, p, motivo)) {
                    Cuerpos.VENENO.remove(p.id);
                    Coleccionista.LOGGER.info("COLECCIONISTA_TE perdona {}", p.nombre);
                } else {
                    Coleccionista.LOGGER.info("COLECCIONISTA_TE muere {} {}", p.etiqueta(), p.nombre);
                }
            }
        }
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            Fx.shake(sp, 0.6F, 10);
            mio(s, sp);
        }
        enviar(s);
    }

    /** Un parpadeo de oscuridad: el muerto a una jaula, el ganador a otra y la siguiente pareja a la mesa. */
    private static void cambio(MinecraftServer s) {
        fase = Fase.CAMBIO;
        desde = t;
        apagonFin = t + 70;
        Apagon.hacer(s, s.getPlayerList().getPlayers(), 60, () -> cambiar(s));
        enviar(s);
    }

    private static void cambiar(MinecraftServer s) {
        Evento ev = Evento.get(s);
        UUID[] actual = duelo >= 0 && duelo < parejas.size() ? parejas.get(duelo) : null;
        UUID[] sig = duelo + 1 < parejas.size() ? parejas.get(duelo + 1) : null;
        // los de la siguiente pareja dejan sus jaulas (ahí van el muerto y el ganador)
        if (sig != null) {
            for (UUID id : sig) {
                if (id != null) {
                    jaulaDe.remove(id);
                }
            }
        }
        if (actual != null) {
            for (int l = 0; l < 2; l++) {
                UUID id = actual[l];
                if (id == null) {
                    continue;
                }
                Pieza p = ev.pieza(id);
                int j = jaulaLibre();
                if (p == null || j < 0) {
                    continue;
                }
                if (p.vivo()) {
                    Entity e = Evento.entidad(s, p);
                    if (e != null) {
                        e.stopRiding();
                        jaulaDe.put(id, j);
                        aJaula(s, e, j);
                        if (!ganadores.contains(id)) {
                            ganadores.add(id);
                        }
                    }
                } else {
                    muertoAJaula(s, p, j);
                }
            }
        }
        duelo++;
        if (sig != null) {
            aLaMesa(s, sig);
        } else {
            for (int l = 0; l < 2; l++) {
                if (silla[l] != null) {
                    silla[l].soltar();
                    silla[l].discard();
                    silla[l] = null;
                }
            }
            muneca = false;
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_TE cambio duelo={} jaulas={} muertos={}", duelo + 1, jaulaDe.size(), muertosEnJaula.size());
    }

    /** El cuerpo (verde) del que se envenenó: de la mesa a su jaula, sentado en el piso de la jaula. */
    private static void muertoAJaula(MinecraftServer s, Pieza p, int j) {
        ServerLevel l = s.overworld();
        BlockPos o = Complejo.origen(s, "te");
        if (o != null) {
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(o.getX() + SalaTe.MX0 - 4, o.getY(), o.getZ() + SalaTe.MZ0 - 4,
                    o.getX() + SalaTe.MX1 + 5, o.getY() + 5, o.getZ() + SalaTe.MZ1 + 5);
            for (Cadaver c : l.getEntitiesOfClass(Cadaver.class, box, x -> p.id.equals(x.dueno()))) {
                c.discard();
            }
        }
        Vec3 v = jaula(s, j);
        Cadaver c = ModRegistry.CADAVER.get().create(l);
        if (v == null || c == null) {
            return;
        }
        float yaw = Complejo.yaw("te.jaula." + j);
        // un poco hacia atrás (las piernas quedan al frente, hacia la mesa)
        Vec3 atras = Vec3.directionFromRotation(0, yaw).scale(-0.35);
        c.moveTo(v.x + atras.x, v.y - 0.05, v.z + atras.z, yaw, 0);
        c.setYRot(yaw);
        c.de(p.id, p.nombre, Cadaver.JAULA + Math.abs(p.id.hashCode()) % 3);
        c.setNoGravity(true);
        l.addFreshEntity(c);
        muertosEnJaula.put(j, c);
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
                        boolean dentro = Cabina.dentro(s, "pas_sor", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO);
                        if (p.bot && !dentro && dt >= 60) {
                            BlockPos c = Complejo.pos(s, "pas_sor.centro");
                            if (c != null) {
                                ir(s, e, c.getX() + 0.5 + R.nextInt(3) - 1, c.getY(), c.getZ() + 0.5 + R.nextInt(3) - 1, 90F);
                                dentro = true;
                            }
                        }
                        todos &= dentro;
                    }
                    if (n > 0 && todos || dt >= esperaPasillo) {
                        BlockPos c = Complejo.pos(s, "pas_sor.centro");
                        for (Pieza p : ev.vivos()) {
                            Entity e = Evento.entidad(s, p);
                            if (e != null && c != null && !Cabina.dentro(s, "pas_sor", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                                ir(s, e, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 90F);
                            }
                        }
                        puertas(s, "pas_sor", false);
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
                            BlockPos c = Complejo.pos(s, "te.entrada");
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
            case HABLANDO, PAREJAS -> {
                if (t % 5 == 0) {
                    parejasTick(s);
                }
                if (t % 20 == 0) {
                    enviar(s);
                }
            }
            case JAULAS -> {
                if (t >= apagonFin) {
                    reglas(s);
                }
            }
            case JUGADA -> {
                for (int l = 0; l < 2; l++) {
                    if (botJuega[l] > 0 && t >= botJuega[l]) {
                        botJuega[l] = -1;
                        jugar(s, l, 1 + R.nextInt(3));
                    }
                }
                if (munecaJuegaEn > 0 && t >= munecaJuegaEn && fase == Fase.JUGADA) {
                    munecaJuegaEn = -1;
                    jugar(s, 1, 1 + R.nextInt(3));
                }
                if (fase == Fase.JUGADA && dt >= segundosJugada * 20L) {
                    // se acabó el tiempo: el Coleccionista elige por quien no eligió
                    for (int l = 0; l < 2 && fase == Fase.JUGADA; l++) {
                        if (jugada[l] == ManoGigante.NADA) {
                            jugar(s, l, 1 + R.nextInt(3));
                        }
                    }
                }
            }
            case MANOS -> {
                long m = dt;
                if (m < ManoGigante.T_MUESTRA && m % ManoGigante.T_SACUDIDA == 7) {
                    // ¡pum! cada sacudida pega en la mesa
                    for (int l = 0; l < 2; l++) {
                        if (mano[l] != null) {
                            s.overworld().playSound(null, mano[l].getX(), mano[l].getY(), mano[l].getZ(), SoundEvents.NOTE_BLOCK_BASEDRUM.get(),
                                    SoundSource.PLAYERS, 1.2F, 0.6F);
                        }
                    }
                }
                if (m == ManoGigante.T_MUESTRA + 2) {
                    for (int l = 0; l < 2; l++) {
                        if (mano[l] != null) {
                            s.overworld().playSound(null, mano[l].getX(), mano[l].getY(), mano[l].getZ(), SoundEvents.DECORATED_POT_HIT,
                                    SoundSource.PLAYERS, 1.4F, 0.8F);
                        }
                    }
                }
                if (m == ManoGigante.T_MUESTRA + 10) {
                    resultadoManos(s);
                }
                if (m > ManoGigante.T_MUESTRA + 10) {
                    if (perdedor < 0 && m >= ManoGigante.T_MUESTRA + 44) {
                        nuevaRonda(s);
                    } else if (perdedor >= 0 && m >= ManoGigante.T_MUESTRA + 50) {
                        elegirCaliz(s);
                    }
                }
            }
            case CALIZ -> {
                for (int l = 0; l < 2; l++) {
                    if (botJuega[l] > 0 && t >= botJuega[l]) {
                        botJuega[l] = -1;
                        // en modo rápido (las pruebas), el bot agarra el envenenado: cada duelo se acaba a la primera
                        beber(s, rapido && (copas[l] & (1 << veneno[l])) != 0 ? veneno[l] : calizAlAzar(l));
                    }
                }
                if (fase == Fase.CALIZ && munecaBebeEn > 0 && t >= munecaBebeEn) {
                    munecaBebeEn = -1;
                    beber(s, calizAlAzar(1));
                }
                if (fase == Fase.CALIZ && dt >= segundosCaliz * 20L) {
                    Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".te.chat.elijo")));
                    beber(s, calizAlAzar(perdedor));
                }
            }
            case BEBE -> bebeTick(s, dt);
            case MUERE -> {
                if (dt >= 90) {
                    cambio(s);
                }
            }
            case CAMBIO -> {
                if (t >= apagonFin) {
                    if (duelo < parejas.size()) {
                        empezarDuelo(s);
                    } else {
                        fin(s);
                    }
                }
            }
            default -> {
            }
        }
        if (t % 20 == 0 && fase.ordinal() >= Fase.REGLAS.ordinal() && fase != Fase.FIN && fase != Fase.CAMBIO) {
            vigilar(s);
        }
        if (t % 40 == 0) {
            enviar(s);
        }
    }

    /** Los de la mesa no se levantan; los de las jaulas no se salen (si alguien se sale o se reconecta, vuelve a su lugar). */
    private static void vigilar(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (int l = 0; l < 2; l++) {
            UUID id = lado(l);
            Pieza p = id == null ? null : ev.pieza(id);
            Entity e = p == null || !p.vivo() ? null : Evento.entidad(s, p);
            if (e != null && silla[l] != null && silla[l].isAlive() && e.getVehicle() != silla[l] && fase != Fase.MUERE) {
                sentar(s, e, silla[l]);
                silla[l].cerrar(true, false);
            }
        }
        for (Map.Entry<UUID, Integer> en : jaulaDe.entrySet()) {
            Pieza p = ev.pieza(en.getKey());
            Entity e = p == null || !p.vivo() ? null : Evento.entidad(s, p);
            Vec3 v = jaula(s, en.getValue());
            if (e != null && v != null && (e.distanceToSqr(v) > 1.6 * 1.6 || e.getVehicle() != null)) {
                aJaula(s, e, en.getValue());
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
            BlockPos p = Complejo.pos(s, "te.luz." + i);
            if (p == null) {
                break;
            }
            int nivel = Math.round(Complejo.yaw("te.luz." + i));
            // la luz de la mesa (13 y 14) y la de las jaulas (8) no se bajan
            int n = bajas && nivel >= 10 && nivel < 13 ? 4 : nivel;
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
        for (ManoGigante m : mano) {
            if (m != null && m.estado() != ManoGigante.GUARDADA) {
                m.estado(ManoGigante.BAJA);
            }
        }
        luces(s, false);
        Pantallas.hablar(s, "te_fin", () -> {
            if (fase == Fase.FIN) {
                salir(s);
            }
        });
        int vivos = ev.vivos().size();
        Anuncio.titulo(s, Component.translatable(MOD + ".te.fin.titulo"), Component.translatable(MOD + ".te.fin.sub", vivos));
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mio(s, sp);
        }
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_TE fin vivos={} muertos={}", vivos, muertosEnJaula.size());
    }

    /** Se abren las jaulas de los que siguen vivos: en un parpadeo, quedan juntos frente a la salida. Los muertos se quedan colgados. */
    private static void salir(MinecraftServer s) {
        Apagon.hacer(s, s.getPlayerList().getPlayers(), 50, () -> {
            Evento ev = Evento.get(s);
            BlockPos c = Complejo.pos(s, "te.salida");
            int n = 0;
            for (Pieza p : ev.vivos()) {
                Entity e = Evento.entidad(s, p);
                if (e == null || c == null) {
                    continue;
                }
                e.stopRiding();
                ir(s, e, c.getX() + 0.5 - (n / 3) * 1.4, c.getY(), c.getZ() + 0.5 - 1.4 + (n % 3) * 1.4, 270F);
                n++;
            }
            jaulaDe.clear();
            for (int l = 0; l < 2; l++) {
                if (silla[l] != null) {
                    silla[l].soltar();
                    silla[l].discard();
                    silla[l] = null;
                }
            }
            Evento.get(s).prueba(s, Component.translatable(MOD + ".te.fin_capitulo").getString());
            Coleccionista.LOGGER.info("COLECCIONISTA_CAPITULO5 fin vivos={}", ev.vivos().size());
        });
    }

    // ------------------------------------------------------------------ lo que mandan los clientes

    private static void accion(ServerPlayer p, CompoundTag d) {
        MinecraftServer s = p.server;
        String que = d.getString("que");
        int l = p.getUUID().equals(lado(0)) ? 0 : p.getUUID().equals(lado(1)) ? 1 : -1;
        if (l < 0) {
            return;
        }
        if ("jugada".equals(que)) {
            jugar(s, l, d.getInt("g"));
        } else if ("caliz".equals(que) && l == perdedor) {
            beber(s, d.getInt("k"));
        }
    }

    // ------------------------------------------------------------------ el estado para los clientes

    private static void silla(CompoundTag e, String k, Silla si) {
        if (si == null) {
            return;
        }
        ListTag l = new ListTag();
        l.add(net.minecraft.nbt.DoubleTag.valueOf(si.getX()));
        l.add(net.minecraft.nbt.DoubleTag.valueOf(si.getY()));
        l.add(net.minecraft.nbt.DoubleTag.valueOf(si.getZ()));
        l.add(net.minecraft.nbt.DoubleTag.valueOf(si.getYRot()));
        e.put(k, l);
    }

    public static CompoundTag estado() {
        CompoundTag e = new CompoundTag();
        e.putString("fase", fase.name());
        e.putInt("duelo", duelo + 1);
        e.putInt("duelos", parejas.size());
        e.putInt("ronda", ronda);
        e.putBoolean("muneca", muneca);
        e.putBoolean("munecaRota", munecaRota);
        if (fase == Fase.PAREJAS || fase == Fase.HABLANDO) {
            int n = 0;
            for (Silla si : sofa) {
                if (si.isVehicle()) {
                    n++;
                }
            }
            e.putInt("sentados", n);
        }
        if (fase.ordinal() >= Fase.REGLAS.ordinal() && fase != Fase.FIN) {
            Evento ev = srv == null ? null : Evento.get(srv);
            for (int l = 0; l < 2; l++) {
                UUID id = lado(l);
                if (id != null) {
                    e.putUUID("id" + l, id);
                    e.putString("nombre" + l, ev == null ? "" : nombre(ev, id));
                }
                silla(e, "silla" + l, silla[l]);
                e.putInt("copas" + l, copas[l]);
                e.putBoolean("eligio" + l, jugada[l] != ManoGigante.NADA);
            }
            e.putInt("perdedor", perdedor);
            e.putInt("ganador", ganador);
            e.putString("resultado", resultado);
            e.putLong("desde", srv == null ? 0 : srv.overworld().getGameTime() - (t - desde));
            if (fase == Fase.MANOS && t - desde >= ManoGigante.T_MUESTRA) {
                e.putInt("j0", jugada[0]);
                e.putInt("j1", jugada[1]);
            }
            if (fase == Fase.BEBE || fase == Fase.MUERE) {
                e.putInt("bebida", bebida);
                e.putLong("bebeDesde", bebeDesde);
                // si tiene veneno, se sabe hasta que hace efecto
                e.putBoolean("veneno", muere && t - desde >= T_TRAGA || fase == Fase.MUERE);
            }
        }
        return e;
    }

    private static void enviar(MinecraftServer s) {
        srv = s;
        PlayerStore.Sync.toAll(s, KEY, estado());
    }

    /** Lo de cada pieza: de qué lado de la mesa está, si tiene que elegir jugada o cáliz, y la cámara cuando bebe. */
    private static void mio(MinecraftServer s, ServerPlayer sp) {
        CompoundTag d = new CompoundTag();
        int l = sp.getUUID().equals(lado(0)) ? 0 : sp.getUUID().equals(lado(1)) ? 1 : -1;
        if (fase != Fase.NADA && l >= 0) {
            d.putInt("lado", l);
            d.putBoolean("jugar", fase == Fase.JUGADA && jugada[l] == ManoGigante.NADA);
            d.putBoolean("caliz", fase == Fase.CALIZ && l == perdedor);
            d.putInt("segundos", fase == Fase.CALIZ ? segundosCaliz : segundosJugada);
            if (fase == Fase.BEBE && l == perdedor) {
                d.putBoolean("cine", true);
                d.putLong("bebeDesde", bebeDesde);
            }
        }
        if (fase != Fase.NADA && jaulaDe.containsKey(sp.getUUID())) {
            d.putBoolean("enJaula", true);
        }
        PlayerStore.Sync.toPlayer(sp, KEY_MIO, d);
    }

    public static String resumen() {
        return fase.name() + " duelo=" + (duelo + 1) + "/" + parejas.size() + " ronda=" + ronda + " copas=" + Integer.toBinaryString(copas[0]) + ","
                + Integer.toBinaryString(copas[1]) + " veneno=" + veneno[0] + "," + veneno[1];
    }

    /** A dónde ir: al pasillo, al salón o a un sillón (si todavía no eliges pareja). */
    public static Objetivo.Meta objetivo(MinecraftServer s, ServerPlayer sp) {
        if (fase == Fase.PASILLO && !Cabina.dentro(s, "pas_sor", sp, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
            return Objetivo.Meta.en(Complejo.pos(s, "pas_sor.centro"), 1.5, MOD + ".obj.pasillo");
        }
        if (fase == Fase.LLEGANDO && desde <= t && !enSalon(s, sp)) {
            return Objetivo.Meta.en(Complejo.pos(s, "te.entrada"), 1.5, MOD + ".obj.salon_te");
        }
        if (eligiendo() && sofaDe(sp) < 0) {
            Silla mejor = null;
            double d = Double.MAX_VALUE;
            for (Silla si : sofa) {
                if (!si.isVehicle() && sp.distanceToSqr(si) < d) {
                    d = sp.distanceToSqr(si);
                    mejor = si;
                }
            }
            if (mejor != null) {
                return Objetivo.Meta.en(mejor.blockPosition(), 1.0, MOD + ".obj.sillon");
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ utilidades

    /** Dónde va el cáliz {@code k} (0..4) de quien está sentado en esa silla (posición y yaw de la silla): en arco, frente a él. */
    public static Vec3 caliz(double sx, double sy, double sz, float yaw, int k) {
        double r = Math.toRadians(yaw);
        double fx = -Math.sin(r), fz = Math.cos(r);
        double rx = -fz, rz = fx;
        double c = k - (CALICES - 1) / 2.0;
        double adelante = 0.98 - 0.07 * c * c, lado = c * 0.36;
        return new Vec3(sx + fx * adelante + rx * lado, sy + 0.975, sz + fz * adelante + rz * lado);
    }

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

    private static void sentar(MinecraftServer s, Entity e, Silla si) {
        if (si == null || !si.isAlive() || e.getVehicle() == si) {
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

    /** Quita todo: sillones, sillas, manos y cuerpos de las jaulas; vuelve la luz. */
    private static void limpiarTodo(MinecraftServer s) {
        ServerLevel l = s.overworld();
        for (Silla si : sofa) {
            si.soltar();
            si.discard();
        }
        sofa.clear();
        for (int k = 0; k < 2; k++) {
            if (silla[k] != null) {
                silla[k].soltar();
                silla[k].discard();
                silla[k] = null;
            }
            if (mano[k] != null) {
                mano[k].discard();
                mano[k] = null;
            }
        }
        BlockPos o = Complejo.origen(s, "te");
        if (o != null) {
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(o.getX(), o.getY() - 2, o.getZ() - 2, o.getX() + 66, o.getY() + 20,
                    o.getZ() + 46);
            for (Silla si : l.getEntitiesOfClass(Silla.class, box, x -> "te".equals(x.grupo))) {
                si.soltar();
                si.discard();
            }
            for (ManoGigante m : l.getEntitiesOfClass(ManoGigante.class, box)) {
                m.discard();
            }
            lucesBajas = true;
            luces(s, false);
        }
        for (Cadaver c : muertosEnJaula.values()) {
            c.discard();
        }
        muertosEnJaula.clear();
        jaulaDe.clear();
        parejas.clear();
        ganadores.clear();
        botDesde.clear();
        botVa.clear();
        duelo = -1;
        perdedor = -1;
        ganador = -1;
        bebida = -1;
        ronda = 0;
        muneca = false;
        munecaRota = false;
        copas[0] = copas[1] = 0;
        resultado = "";
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase == Fase.HABLANDO || fase == Fase.REGLAS || fase == Fase.FIN) {
            Pantallas.callar(s);
        }
        if (fase != Fase.NADA) {
            Pantallas.modo(s, "apagada", "");
        }
        limpiarTodo(s);
        fase = Fase.NADA;
        enviar(s);
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            PlayerStore.Sync.toPlayer(sp, KEY_MIO, new CompoundTag());
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
                Pantallas.callar(s);
                empezarParejas(s);
                return true;
            }
            case PAREJAS -> {
                formarParejas(s);
                return true;
            }
            case REGLAS -> {
                Pantallas.callar(s);
                empezarDuelo(s);
                return true;
            }
            case JUGADA -> {
                for (int l = 0; l < 2 && fase == Fase.JUGADA; l++) {
                    if (jugada[l] == ManoGigante.NADA) {
                        jugar(s, l, 1 + R.nextInt(3));
                    }
                }
                return true;
            }
            case CALIZ -> {
                return beber(s, calizAlAzar(perdedor));
            }
            default -> {
                return false;
            }
        }
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        return fase == Fase.PAREJAS || fase == Fase.JUGADA || fase == Fase.CALIZ;
    }

    /** El origen de la zona del salón de té (para poner las manos sin depender de una marca por mano). */
    private static final class SalaTeOrigen {
        static double x(MinecraftServer s) {
            BlockPos o = Complejo.origen(s, "te");
            return o == null ? 0 : o.getX();
        }

        static double z(MinecraftServer s) {
            BlockPos o = Complejo.origen(s, "te");
            return o == null ? 0 : o.getZ();
        }
    }
}
