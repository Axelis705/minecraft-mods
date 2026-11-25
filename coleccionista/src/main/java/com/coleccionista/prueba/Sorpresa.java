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
import com.coleccionista.evento.CajaSorpresa;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Cuerpos;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Objetivo;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.evento.Silla;
import com.coleccionista.mapa.Pasaje;
import com.coleccionista.mapa.SalaSorpresa;
import com.coleccionista.registry.ModRegistry;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Capítulo 4 · La caja sorpresa. Del salón de la caja de música pasan (por el pasillo de servicio) al teatrito de Don Resorte. El
 * Coleccionista explica y, en un apagón, aparecen sentados en las gradas, mirando al centro. Por turnos (en el sentido del reloj, desde
 * uno al azar) a cada quien se le gira la silla hacia el escenario y se le sueltan las correas: baja, sube los escalones y le da a la
 * manivela de la caja 1, 2 o 3 vueltas (las que quiera; pero si alguien da 1, el siguiente está obligado a dar 3). La caja salta en una
 * vuelta secreta: Don Resorte sale (con su cabeza gigante), agarra a quien le dio cuerda, se lo come (sangre y todo) y escupe sus zapatos,
 * que quedan en fila al frente del escenario. Luego se vuelve a meter, se le da cuerda con un número secreto nuevo y sigue el que sigue.
 * Se acaba cuando Don Resorte ya se comió a los que puso el organizador ({@code /evento sorpresa comer <n>}; si no, el 40% de los vivos).
 */
public final class Sorpresa {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":sorpresa";
    public static final String KEY_MIO = MOD + ":sorpresa_mio";
    public static final String ACCION = MOD + ":sorpresa_accion";
    public static final String PRUEBA = "La caja sorpresa";
    /** Ajustes (los cambia el organizador con /evento sorpresa ...). */
    public static int comer = 0, segundosIr = 45, segundosElegir = 20, segundosVolver = 40;
    public static int esperaPasillo = 1200, esperaSalon = 900;
    /** Al terminar la caja de música, pasan solos a la caja sorpresa. */
    public static boolean encadenar = true;
    /** Para las pruebas: el número secreto de la siguiente caja (0: al azar) y quién va primero. */
    public static int secretoFijo = 0;
    public static UUID primero;
    /** Para las pruebas: la caja siempre salta en la vuelta 2 y los bots van y vuelven al instante. */
    public static boolean rapido;

    public enum Fase {
        NADA, PASILLO, LLEGANDO, HABLANDO, SENTANDO, IR, ELIGE, GIRANDO, VOLVER, SALTO, AGARRA, MUERDE, ESCUPE, GUARDA, FIN
    }

    private static Fase fase = Fase.NADA;
    private static long t, desde, apagonFin;
    private static final Random R = new Random();

    /** Las sillas de cada pieza y el orden de los turnos (en el sentido del reloj). */
    private static final Map<UUID, Silla> sillas = new LinkedHashMap<>();
    private static final List<UUID> orden = new ArrayList<>();
    private static final Map<UUID, Float> yawCentro = new HashMap<>();
    private static int idx = -1;
    private static UUID turno;
    private static String turnoNombre = "";
    /** La caja: su número secreto, cuántas vueltas lleva y si el que sigue está obligado a dar 3. */
    private static int secreto, cuenta, quiere, dadas, comidos, cuota, zapatosK, nota;
    private static boolean obligado, obligadoAhora;
    private static CajaSorpresa caja;
    private static Silla agarre;
    private static BlockPos foco;
    private static final List<BlockPos> puestos = new ArrayList<>();
    private static boolean lucesBajas;

    private Sorpresa() {
    }

    public static void init() {
        AxelisCore.onServerTick(Sorpresa::tick);
        AxelisCore.onLogin(p -> {
            PlayerStore.Sync.toPlayer(p, KEY, estado());
            mio(Plat.server(p), p);
        });
        Actions.on(ACCION, Sorpresa::accion);
        // aquí nadie se lastima: solo se muere en la boca de Don Resorte
        MinecraftForge.EVENT_BUS.addListener((LivingHurtEvent e) -> {
            if (fase != Fase.NADA && fase != Fase.PASILLO && e.getEntity() instanceof ServerPlayer) {
                e.setCanceled(true);
            }
        });
        // al volver, basta con darle clic derecho a su silla
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> {
            if (fase == Fase.VOLVER && !e.getLevel().isClientSide() && e.getEntity() instanceof ServerPlayer sp && sp.getUUID().equals(turno)
                    && e.getTarget() == sillas.get(turno)) {
                e.setCanceled(true);
                sentar(sp.server, sp, sillas.get(turno));
            }
        });
    }

    public static Fase fase() {
        return fase;
    }

    public static boolean abierta() {
        return fase != Fase.NADA;
    }

    /** ¿Puede sentarse en esa silla? Solo el que tiene turno, en la suya, al volver. */
    public static boolean puedeSentarse(ServerPlayer sp, Silla si) {
        return fase == Fase.VOLVER && sp.getUUID().equals(turno) && sillas.get(turno) == si;
    }

    // ------------------------------------------------------------------ del salón de la caja de música al teatrito

    /** Al terminar la caja de música: se abren las puertas de su pasillo y el Coleccionista los llama. */
    public static Component llamar(MinecraftServer s) {
        if (!Complejo.construido(s, "sorpresa")) {
            return Component.translatable(MOD + ".sor.sin_mapa");
        }
        if (!Complejo.construido(s, "caja")) {
            return llegar(s);
        }
        Pruebas.cerrarOtras(s, "sorpresa");
        limpiarTodo(s);
        puertas(s, "pas_caj", true);
        fase = Fase.PASILLO;
        desde = t;
        Evento ev = Evento.get(s);
        ev.prueba(s, Component.translatable(MOD + ".caja.prueba_pasillo").getString());
        ev.reloj(s, 0);
        Anuncio.titulo(s, Component.empty(), Component.translatable(MOD + ".caja.al_pasillo"));
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_SORPRESA llamar");
        return null;
    }

    /** Los vivos pasan (sin que se note) del pasillo de la caja de música al del teatrito y se abren las puertas al salón. */
    public static Component llegar(MinecraftServer s) {
        if (!Complejo.construido(s, "sorpresa")) {
            return Component.translatable(MOD + ".sor.sin_mapa");
        }
        if (fase == Fase.NADA) {
            Pruebas.cerrarOtras(s, "sorpresa");
            limpiarTodo(s);
        }
        Evento ev = Evento.get(s);
        List<Entity> enPasillo = new ArrayList<>(), fuera = new ArrayList<>();
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null) {
                continue;
            }
            if (Complejo.construido(s, "caja") && Cabina.dentro(s, "pas_caj", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                enPasillo.add(e);
            } else {
                fuera.add(e);
            }
        }
        if (!enPasillo.isEmpty()) {
            Cabina.trasladar(s, "pas_caj", "pas_sor", enPasillo, List.of());
        }
        BlockPos c = Complejo.pos(s, "pas_sor.centro");
        int n = 0;
        for (Entity e : fuera) {
            if (c == null) {
                break;
            }
            ir(s, e, c.getX() + 0.5 - 1.5 + (n % 3) * 1.5, c.getY(), c.getZ() + 0.5 - 1 + (n / 3 % 4), 270F);
            n++;
        }
        puertas(s, "pas_caj", false);
        puertas(s, "pas_sor", true);
        fase = Fase.LLEGANDO;
        desde = t;
        ev.prueba(s, Component.translatable(MOD + ".sor.prueba").getString());
        ev.reloj(s, 0);
        Pantallas.modo(s, "apagada", "");
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_SORPRESA llegan pasillo={} fuera={}", enPasillo.size(), fuera.size());
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
        BlockPos o = Complejo.origen(s, "sorpresa");
        if (o == null) {
            return false;
        }
        double x = e.getX() - o.getX(), z = e.getZ() - o.getZ();
        return x >= SalaSorpresa.HX0 && x <= SalaSorpresa.HX1 + 1 && z >= SalaSorpresa.HZ0 && z <= SalaSorpresa.HZ1 + 1;
    }

    // ------------------------------------------------------------------ el discurso y a las gradas

    private static void hablar(MinecraftServer s) {
        fase = Fase.HABLANDO;
        desde = t;
        BlockPos c = Complejo.pos(s, "sor.entrada");
        for (Pieza p : Evento.get(s).vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null && c != null && !enSalon(s, e)) {
                ir(s, e, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 270F);
            }
        }
        puertas(s, "pas_sor", false);
        crearCaja(s);
        Escena.presentacion(s, "IV", MOD + ".sor.titulo", MOD + ".sor.lema");
        Pantallas.soloGrande(s, true);
        Pantallas.hablar(s, "sorpresa", () -> sentar(s));
        Evento.get(s).prueba(s, PRUEBA);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_SORPRESA habla");
    }

    private static void crearCaja(MinecraftServer s) {
        if (caja != null && caja.isAlive()) {
            return;
        }
        BlockPos b = Complejo.pos(s, "sor.caja");
        CajaSorpresa c = b == null ? null : ModRegistry.CAJA_SORPRESA.get().create(s.overworld());
        if (c != null) {
            c.moveTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, Complejo.yaw("sor.caja"), 0F);
            c.setYHeadRot(c.getYRot());
            s.overworld().addFreshEntity(c);
            caja = c;
        }
    }

    /** Se acabó el discurso: un apagón, y en lo oscuro todos aparecen sentados (y atados) en las gradas. */
    public static void sentar(MinecraftServer s) {
        if (fase != Fase.HABLANDO && fase != Fase.LLEGANDO) {
            return;
        }
        if (fase == Fase.HABLANDO) {
            Pantallas.callar(s);
        }
        fase = Fase.SENTANDO;
        desde = t;
        apagonFin = t + 70;
        crearCaja(s);
        Apagon.hacer(s, s.getPlayerList().getPlayers(), 60, () -> acomodar(s));
        enviar(s);
    }

    private static void acomodar(MinecraftServer s) {
        Evento ev = Evento.get(s);
        ServerLevel l = s.overworld();
        List<Pieza> v = new ArrayList<>(ev.vivos());
        Collections.shuffle(v, R);
        // las sillas, en orden alrededor de la herradura
        List<BlockPos> lugares = new ArrayList<>();
        List<Float> yaws = new ArrayList<>();
        for (int k = 0; k < 200; k++) {
            BlockPos b = Complejo.pos(s, "sor.silla." + k);
            if (b == null) {
                break;
            }
            lugares.add(b);
            yaws.add(Complejo.yaw("sor.silla." + k));
        }
        // de preferencia las de la grada de abajo, repartidas (no todas juntas)
        List<Integer> usar = new ArrayList<>();
        for (int k = 0; k < lugares.size(); k++) {
            usar.add(k);
        }
        int n = Math.min(v.size(), lugares.size());
        if (n < lugares.size()) {
            List<Integer> elegidas = new ArrayList<>();
            double paso = lugares.size() / (double) n;
            for (int i = 0; i < n; i++) {
                elegidas.add((int) Math.floor(i * paso));
            }
            usar = elegidas;
        }
        sillas.clear();
        orden.clear();
        yawCentro.clear();
        for (int i = 0; i < n; i++) {
            int k = usar.get(i);
            BlockPos b = lugares.get(k);
            Silla si = ModRegistry.SILLA.get().create(l);
            if (si == null) {
                continue;
            }
            float yaw = yaws.get(k);
            si.moveTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, yaw, 0F);
            si.setYHeadRot(yaw);
            si.grupo = "sorpresa";
            l.addFreshEntity(si);
            Pieza p = v.get(i);
            Entity e = Evento.entidad(s, p);
            if (e != null) {
                sentar(s, e, si);
            }
            si.cerrar(true, false);
            sillas.put(p.id, si);
            orden.add(p.id);
            yawCentro.put(p.id, yaw);
        }
        comidos = 0;
        cuota = comer > 0 ? comer : Math.max(1, (int) Math.round(orden.size() * 0.4));
        cuota = Math.max(1, Math.min(cuota, orden.size() - 1));
        nuevaCaja();
        // empieza uno al azar (o el que pidieron las pruebas)
        idx = R.nextInt(Math.max(1, orden.size())) - 1;
        if (primero != null && orden.contains(primero)) {
            idx = orden.indexOf(primero) - 1;
        }
        primero = null;
        luces(s, true);
        Coleccionista.LOGGER.info("COLECCIONISTA_SORPRESA sentados={} cuota={} secreto={}", orden.size(), cuota, secreto);
    }

    /** {@code /evento sorpresa secreto <n>}: la caja salta en la vuelta n (la de ahora, si todavía no pasa de ahí, y la siguiente). */
    public static void secreto(int n) {
        secretoFijo = n;
        if (fase != Fase.NADA && n > cuenta) {
            secreto = n;
        }
    }

    private static void nuevaCaja() {
        secreto = rapido ? 2 : secretoFijo > 0 ? secretoFijo : 3 + R.nextInt(10);
        secretoFijo = 0;
        cuenta = 0;
        obligado = false;
    }

    // ------------------------------------------------------------------ los turnos

    /** Al siguiente vivo de la herradura: su silla gira hacia el escenario y se le sueltan las correas. */
    private static void siguiente(MinecraftServer s) {
        Evento ev = Evento.get(s);
        if (comidos >= cuota || ev.vivos().size() <= 1) {
            terminar(s);
            return;
        }
        UUID id = null;
        for (int k = 0; k < orden.size(); k++) {
            idx = (idx + 1) % orden.size();
            Pieza p = ev.pieza(orden.get(idx));
            if (p != null && p.vivo() && Evento.entidad(s, p) != null) {
                id = p.id;
                break;
            }
        }
        if (id == null) {
            terminar(s);
            return;
        }
        turno = id;
        Pieza pt = ev.pieza(id);
        turnoNombre = pt == null ? "" : pt.nombre;
        obligadoAhora = obligado;
        quiere = 0;
        dadas = 0;
        fase = Fase.IR;
        desde = t;
        Silla si = sillas.get(id);
        BlockPos sub = Complejo.pos(s, "sor.subida");
        if (si != null && sub != null) {
            float yaw = (float) Math.toDegrees(Math.atan2(-(sub.getX() + 0.5 - si.getX()), sub.getZ() + 0.5 - si.getZ()));
            si.girarA(yaw, 4F);
            si.cerrar(false, true);
        }
        Entity e = Evento.entidad(s, ev.pieza(id));
        foco(s, si);
        Pieza p = ev.pieza(id);
        ev.reloj(s, segundosIr);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".sor.pantalla.turno", p == null ? "?" : p.nombre).getString());
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            sp.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.MASTER, 0.7F, sp.getUUID().equals(id) ? 0.5F : 0.8F);
            mio(s, sp);
        }
        if (e instanceof ServerPlayer sp) {
            sp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.MASTER, 1F, 1F);
        }
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_SORPRESA turno {} obligado={}", p == null ? id : p.nombre, obligadoAhora);
    }

    /** Llegó a la manivela: se queda parado ahí, mirando la caja, y elige (los bots solos). */
    private static void llegoACaja(MinecraftServer s, Entity e) {
        BlockPos m = Complejo.pos(s, "sor.manivela");
        if (m != null) {
            ir(s, e, m.getX() + 0.5, m.getY(), m.getZ() + 0.5, Complejo.yaw("sor.manivela"));
        }
        fase = Fase.ELIGE;
        desde = t;
        Evento.get(s).reloj(s, segundosElegir);
        if (e instanceof ServerPlayer sp) {
            mio(s, sp);
        }
        enviar(s);
    }

    /** Cuántas vueltas le da (1, 2 o 3; si está obligado, 3). */
    public static boolean elegir(MinecraftServer s, int n) {
        if (fase != Fase.ELIGE || n < 1 || n > 3 || obligadoAhora && n != 3) {
            return false;
        }
        quiere = n;
        dadas = 0;
        fase = Fase.GIRANDO;
        desde = t;
        Evento.get(s).reloj(s, 0);
        Pieza p = Evento.get(s).pieza(turno);
        Anuncio.todos(s, Component.translatable(MOD + ".sor.chat.vueltas", p == null ? "?" : p.nombre, n).withStyle(ChatFormatting.GOLD));
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mio(s, sp);
        }
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_SORPRESA vueltas {} {} (cuenta={} secreto={})", p == null ? turno : p.nombre, n, cuenta, secreto);
        return true;
    }

    /** Una vuelta de la manivela (cada {@link CajaSorpresa#TICKS_VUELTA} ticks), con la música; si llega al número secreto, ¡salta! */
    private static void vuelta(MinecraftServer s) {
        if (caja != null) {
            caja.vuelta();
        }
        cuenta++;
        dadas++;
        sonidoCaja(s, SoundEvents.LEVER_CLICK, 1.2F, 0.6F);
        if (cuenta >= secreto) {
            salta(s);
            return;
        }
        if (dadas >= quiere) {
            // listo: a su silla (y si dio 1, el siguiente da 3)
            obligado = quiere == 1;
            fase = Fase.VOLVER;
            desde = t;
            Evento.get(s).reloj(s, segundosVolver);
            for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
                mio(s, sp);
            }
            enviar(s);
            Coleccionista.LOGGER.info("COLECCIONISTA_SORPRESA se_salva cuenta={} obligado_siguiente={}", cuenta, obligado);
        }
    }

    /** La música de la caja: una melodía de feria, lenta y desafinada, nota por nota mientras gira la manivela. */
    private static final int[] MELODIA = {12, 15, 19, 15, 12, 15, 19, 22, 20, 19, 15, 12, 11, 14, 17, 14, 11, 14, 17, 20, 19, 17, 14, 11};

    private static void musica(MinecraftServer s) {
        int n = MELODIA[nota++ % MELODIA.length];
        float tono = (float) Math.pow(2, (n - 12) / 12.0) * (0.97F + R.nextFloat() * 0.04F);
        sonidoCaja(s, SoundEvents.NOTE_BLOCK_CHIME.get(), 1.4F, tono);
        sonidoCaja(s, SoundEvents.NOTE_BLOCK_BELL.get(), 0.5F, tono * 0.5F);
    }

    private static void sonidoCaja(MinecraftServer s, SoundEvent e, float vol, float tono) {
        if (caja != null) {
            s.overworld().playSound(null, caja.getX(), caja.getY() + 1.5, caja.getZ(), e, SoundSource.HOSTILE, vol, tono);
        }
    }

    private static SoundEvent propio(String id) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD, id));
    }

    // ------------------------------------------------------------------ ¡sorpresa!

    private static void salta(MinecraftServer s) {
        fase = Fase.SALTO;
        desde = t;
        if (caja != null) {
            caja.estado(CajaSorpresa.SALTO);
        }
        Evento ev = Evento.get(s);
        Pieza p = ev.pieza(turno);
        Entity e = p == null ? null : Evento.entidad(s, p);
        sonidoCaja(s, SoundEvents.PISTON_EXTEND, 3F, 0.5F);
        sonidoCaja(s, SoundEvents.WITCH_CELEBRATE, 3F, 0.6F);
        sonidoCaja(s, SoundEvents.EVOKER_PREPARE_ATTACK, 3F, 0.7F);
        sonidoCaja(s, propio("caja_compuerta"), 2F, 1.6F);
        if (caja != null) {
            s.overworld().sendParticles(ParticleTypes.POOF, caja.getX(), caja.getY() + 2.6, caja.getZ(), 30, 0.8, 0.4, 0.8, 0.05);
        }
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            Fx.shake(sp, 0.8F, 10);
            sp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.MASTER, 1F, 1.4F);
        }
        // quien le dio cuerda se queda tieso (ya no se puede ir)
        if (e != null) {
            Silla si = ModRegistry.SILLA.get().create(s.overworld());
            if (si != null) {
                si.moveTo(e.getX(), e.getY() - 0.1, e.getZ(), Complejo.yaw("sor.manivela"), 0F);
                si.oculta(true);
                si.grupo = "sorpresa";
                s.overworld().addFreshEntity(si);
                sentar(s, e, si);
                si.cerrar(true, false);
                agarre = si;
            }
        }
        Anuncio.titulo(s, Component.translatable(MOD + ".sor.sorpresa").withStyle(ChatFormatting.DARK_RED), Component.empty());
        Pantallas.modo(s, "estatica", "");
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_SORPRESA salta {} en_vuelta={}", p == null ? turno : p.nombre, cuenta);
    }

    private static void sangre(MinecraftServer s, Vec3 c, int cuanta, double abre) {
        ServerLevel l = s.overworld();
        DustParticleOptions rojo = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.03F), 1.8F);
        DustParticleOptions oscuro = new DustParticleOptions(new Vector3f(0.3F, 0.0F, 0.02F), 2.4F);
        l.sendParticles(rojo, c.x, c.y, c.z, cuanta, abre, abre, abre, 0.08);
        l.sendParticles(oscuro, c.x, c.y - 0.3, c.z, cuanta / 2, abre * 1.3, abre, abre * 1.3, 0.03);
        l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), c.x, c.y, c.z, cuanta, abre * 0.8,
                abre * 0.5, abre * 0.8, 0.35);
    }

    /** Manchas de sangre en el escenario, enfrente de la caja. */
    private static void mancharEscenario(MinecraftServer s, int cuantas) {
        ServerLevel l = s.overworld();
        BlockPos m = Complejo.pos(s, "sor.manivela");
        if (m == null) {
            return;
        }
        for (int i = 0; i < cuantas; i++) {
            BlockPos q = m.offset(R.nextInt(5) - 1, 0, R.nextInt(5) - 2);
            BlockState st = Complejo.estado("coleccionista:sangre[facing=" + new String[] {"north", "east", "south", "west"}[R.nextInt(4)] + "]");
            if (st != null && l.getBlockState(q).isAir() && l.getBlockState(q.below()).isSolidRender(l, q.below())) {
                l.setBlock(q, st, 2);
                puestos.add(q);
            }
        }
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
                        boolean dentro = Cabina.dentro(s, "pas_caj", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO);
                        if (p.bot && !dentro && dt >= 60) {
                            BlockPos c = Complejo.pos(s, "pas_caj.centro");
                            if (c != null) {
                                ir(s, e, c.getX() + 0.5 + R.nextInt(3) - 1, c.getY(), c.getZ() + 0.5 + R.nextInt(3) - 1, 90F);
                                dentro = true;
                            }
                        }
                        todos &= dentro;
                    }
                    if (n > 0 && todos || dt >= esperaPasillo) {
                        BlockPos c = Complejo.pos(s, "pas_caj.centro");
                        for (Pieza p : ev.vivos()) {
                            Entity e = Evento.entidad(s, p);
                            if (e != null && c != null && !Cabina.dentro(s, "pas_caj", e, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
                                ir(s, e, c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 90F);
                            }
                        }
                        puertas(s, "pas_caj", false);
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
                            BlockPos c = Complejo.pos(s, "sor.entrada");
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
            case SENTANDO -> {
                if (t >= apagonFin) {
                    siguiente(s);
                }
            }
            case IR -> irTick(s, dt);
            case ELIGE -> {
                quieto(s);
                Pieza p = ev.pieza(turno);
                if (p != null && p.bot && dt == 40) {
                    elegir(s, obligadoAhora ? 3 : eleccionBot());
                } else if (dt >= segundosElegir * 20L) {
                    elegir(s, obligadoAhora ? 3 : 1);
                }
            }
            case GIRANDO -> {
                quieto(s);
                if (dt % CajaSorpresa.TICKS_VUELTA == 0) {
                    vuelta(s);
                }
                if (fase == Fase.GIRANDO && dt % 8 == 2) {
                    musica(s);
                }
            }
            case VOLVER -> volverTick(s, dt);
            case SALTO -> {
                if (dt == 8 && caja != null) {
                    sonidoCaja(s, SoundEvents.GOAT_SCREAMING_PREPARE_RAM, 3F, 0.5F);
                }
                if (dt >= CajaSorpresa.T_SALTO) {
                    fase = Fase.AGARRA;
                    desde = t;
                    if (caja != null) {
                        caja.estado(CajaSorpresa.AGARRA);
                        sonidoCaja(s, SoundEvents.ZOMBIE_INFECT, 3F, 0.5F);
                    }
                }
            }
            case AGARRA -> {
                // lo levanta hasta su boca
                if (dt == 8 && agarre != null && caja != null) {
                    Vec3 b = caja.boca();
                    agarre.irA(new Vec3(b.x, b.y - 1.5, b.z), 0.16);
                    for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
                        if (sp.getUUID().equals(turno)) {
                            sp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.MASTER, 1F, 1.8F);
                            Fx.shake(sp, 1.4F, 20);
                        }
                    }
                    sonidoCaja(s, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, 2F, 0.5F);
                }
                if (dt >= CajaSorpresa.T_AGARRA) {
                    morder(s);
                }
            }
            case MUERDE -> {
                // mastica: crujidos y sangre que escurre de la boca
                if (dt > 4 && dt % 9 == 2 && caja != null) {
                    sonidoCaja(s, SoundEvents.GENERIC_EAT, 3F, 0.5F + R.nextFloat() * 0.2F);
                    sonidoCaja(s, SoundEvents.SKELETON_HURT, 1.6F, 0.4F);
                    sonidoCaja(s, SoundEvents.SLIME_SQUISH, 2F, 0.5F);
                    Vec3 b = caja.boca();
                    sangre(s, b.add(0, -0.4, 0), 18, 0.35);
                }
                if (dt == 30 || dt == 55) {
                    mancharEscenario(s, 2);
                }
                if (dt >= CajaSorpresa.T_MUERDE) {
                    fase = Fase.ESCUPE;
                    desde = t;
                    BlockPos z = Complejo.pos(s, "sor.zapatos." + zapatosK);
                    if (caja != null) {
                        if (z != null) {
                            caja.zapatosA(new Vec3(z.getX() + 0.5, z.getY(), z.getZ() + 0.5));
                        }
                        caja.estado(CajaSorpresa.ESCUPE);
                    }
                }
            }
            case ESCUPE -> {
                if (dt == 8) {
                    sonidoCaja(s, SoundEvents.LLAMA_SPIT, 3F, 0.5F);
                    sonidoCaja(s, SoundEvents.PLAYER_BURP, 2.5F, 0.5F);
                    if (caja != null) {
                        sangre(s, caja.boca(), 25, 0.3);
                    }
                }
                if (dt == 22) {
                    // los zapatos caen en su lugar, al frente del escenario
                    BlockPos z = Complejo.pos(s, "sor.zapatos." + zapatosK);
                    BlockState st = Complejo.estado("coleccionista:zapatos[facing=west]");
                    if (z != null && st != null && s.overworld().getBlockState(z).isAir()) {
                        s.overworld().setBlock(z, st, 2);
                        puestos.add(z);
                        s.overworld().playSound(null, z, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.HOSTILE, 2F, 0.7F);
                        s.overworld().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                                z.getX() + 0.5, z.getY() + 0.2, z.getZ() + 0.5, 12, 0.2, 0.1, 0.2, 0.1);
                    }
                    zapatosK++;
                }
                if (dt >= CajaSorpresa.T_ESCUPE) {
                    fase = Fase.GUARDA;
                    desde = t;
                    if (caja != null) {
                        caja.estado(CajaSorpresa.GUARDA);
                    }
                }
            }
            case GUARDA -> {
                if (dt == 22) {
                    sonidoCaja(s, SoundEvents.CHEST_CLOSE, 3F, 0.5F);
                    sonidoCaja(s, SoundEvents.ANVIL_LAND, 1F, 0.6F);
                }
                if (dt >= CajaSorpresa.T_GUARDA) {
                    if (caja != null) {
                        caja.estado(CajaSorpresa.CERRADA);
                    }
                    nuevaCaja();
                    Pantallas.modo(s, "apagada", "");
                    siguiente(s);
                }
            }
            default -> {
            }
        }
        if (t % 20 == 0 && sillas.size() > 0) {
            vigilarSillas(s);
        }
        if (t % 40 == 0) {
            enviar(s);
        }
    }

    /** ¡Ñam! Se lo come: la mordida, la sangre, y la pieza queda eliminada (no deja cuerpo: solo sus zapatos). */
    private static void morder(MinecraftServer s) {
        fase = Fase.MUERDE;
        desde = t;
        if (caja != null) {
            caja.estado(CajaSorpresa.MUERDE);
        }
        Evento ev = Evento.get(s);
        Pieza p = ev.pieza(turno);
        Entity e = p == null ? null : Evento.entidad(s, p);
        sonidoCaja(s, SoundEvents.EVOKER_FANGS_ATTACK, 3F, 0.5F);
        sonidoCaja(s, SoundEvents.PLAYER_ATTACK_CRIT, 3F, 0.5F);
        sonidoCaja(s, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.5F, 0.6F);
        if (caja != null) {
            sangre(s, caja.boca(), 90, 0.6);
        }
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            Fx.shake(sp, 1.3F, 14);
            if (e != null && sp.distanceToSqr(e) < 40 * 40) {
                Fx.flash(sp, 0x60A00010, 4);
            }
        }
        mancharEscenario(s, 4);
        if (p != null) {
            Cuerpos.SIN_CUERPO.add(p.id);
            String motivo = Component.translatable(MOD + ".sor.motivo").getString();
            if (ev.eliminarEnPrueba(s, p, motivo)) {
                comidos++;
                Coleccionista.LOGGER.info("COLECCIONISTA_SORPRESA come {} {} comidos={}/{}", p.etiqueta(), p.nombre, comidos, cuota);
            } else {
                Cuerpos.SIN_CUERPO.remove(p.id);
                Coleccionista.LOGGER.info("COLECCIONISTA_SORPRESA perdona {}", p.nombre);
            }
        }
        if (agarre != null) {
            agarre.soltar();
            agarre.discard();
            agarre = null;
        }
        // si no se lo comió (la regla de los pocos), regresa a su silla
        if (p != null && p.vivo() && e != null) {
            sentar(s, e, sillas.get(p.id));
        }
        Silla si = p == null ? null : sillas.get(p.id);
        if (si != null && (p == null || !p.vivo())) {
            si.soltar();
            si.cerrar(false, false);
        }
        foco(s, null);
        enviar(s);
    }

    private static int eleccionBot() {
        float x = R.nextFloat();
        return x < 0.4F ? 1 : x < 0.75F ? 2 : 3;
    }

    /** Camino a la caja: los bots bajan y suben solos; a los que no llegan, al acabarse el tiempo la caja los lleva. */
    private static void irTick(MinecraftServer s, long dt) {
        Evento ev = Evento.get(s);
        Pieza p = ev.pieza(turno);
        Entity e = p == null || !p.vivo() ? null : Evento.entidad(s, p);
        if (e == null) {
            siguiente(s);
            return;
        }
        BlockPos m = Complejo.pos(s, "sor.manivela"), sub = Complejo.pos(s, "sor.subida");
        if (m == null) {
            return;
        }
        double dx = e.getX() - (m.getX() + 0.5), dz = e.getZ() - (m.getZ() + 0.5);
        boolean ahi = dx * dx + dz * dz < 1.6 * 1.6 && e.getY() > m.getY() - 0.6;
        if (ahi) {
            llegoACaja(s, e);
            return;
        }
        // cuando la silla ya giró hacia el escenario, lo levanta (a todos: personas y bots); queda parado frente a ella
        if (dt == 30) {
            Silla si = sillas.get(p.id);
            if (si != null && e.getVehicle() == si) {
                si.soltar();
                Vec3 f = Vec3.directionFromRotation(0, si.getYRot()).scale(0.9);
                BlockPos q = BlockPos.containing(si.getX() + f.x, si.getY() + 0.1, si.getZ() + f.z);
                if (s.overworld().getBlockState(q).getCollisionShape(s.overworld(), q).isEmpty()
                        && s.overworld().getBlockState(q.above()).getCollisionShape(s.overworld(), q.above()).isEmpty()) {
                    ir(s, e, si.getX() + f.x, si.getY(), si.getZ() + f.z, si.getYRot());
                }
                s.overworld().playSound(null, si.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F);
            }
        }
        if (p.bot) {
            if (dt > 30 && e instanceof PiezaBot b && dt % 20 == 10) {
                BlockPos a = dt < 120 && sub != null ? sub : m;
                b.getNavigation().moveTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 1.0);
            }
            if (dt >= (rapido ? 20 : 260)) {
                llegoACaja(s, e);
            }
            return;
        }
        if (dt >= segundosIr * 20L) {
            llegoACaja(s, e);
        }
    }

    /** De regreso a su silla: se sienta solo (clic derecho, o al llegar junto a ella); si no, al acabarse el tiempo la caja lo sienta. */
    private static void volverTick(MinecraftServer s, long dt) {
        Evento ev = Evento.get(s);
        Pieza p = ev.pieza(turno);
        Entity e = p == null || !p.vivo() ? null : Evento.entidad(s, p);
        Silla si = p == null ? null : sillas.get(p.id);
        if (e == null || si == null) {
            siguiente(s);
            return;
        }
        if (e.getVehicle() == si) {
            sentado(s, si);
            return;
        }
        double d = e.distanceToSqr(si);
        if (p.bot && e instanceof PiezaBot b && dt % 20 == 10) {
            BlockPos sub = Complejo.pos(s, "sor.subida");
            if (dt < 60 && sub != null) {
                b.getNavigation().moveTo(sub.getX() + 0.5, sub.getY(), sub.getZ() + 0.5, 1.0);
            } else {
                b.getNavigation().moveTo(si.getX(), si.getY(), si.getZ(), 1.0);
            }
        }
        if (d < 1.3 * 1.3 || dt >= (p.bot ? (rapido ? 20 : 240) : segundosVolver * 20L)) {
            sentar(s, e, si);
            sentado(s, si);
        }
    }

    /** Ya volvió: se cierran las correas, la silla vuelve a mirar al centro y sigue el siguiente. */
    private static void sentado(MinecraftServer s, Silla si) {
        si.cerrar(true, true);
        Float y = yawCentro.get(turno);
        if (y != null) {
            si.girarA(y, 4F);
        }
        foco(s, null);
        fase = Fase.SENTANDO;
        desde = t;
        apagonFin = t + 40;
        Evento.get(s).reloj(s, 0);
        enviar(s);
    }

    /** Mientras le da cuerda, no se mueve de la manivela. */
    private static void quieto(MinecraftServer s) {
        Pieza p = Evento.get(s).pieza(turno);
        Entity e = p == null ? null : Evento.entidad(s, p);
        BlockPos m = Complejo.pos(s, "sor.manivela");
        if (e == null || m == null || e.getVehicle() != null) {
            return;
        }
        double dx = e.getX() - (m.getX() + 0.5), dz = e.getZ() - (m.getZ() + 0.5);
        if (dx * dx + dz * dz > 0.8 * 0.8) {
            ir(s, e, m.getX() + 0.5, m.getY(), m.getZ() + 0.5, Complejo.yaw("sor.manivela"));
        }
    }

    /** Nadie se levanta de su silla (menos el que tiene turno), y quien se reconecta vuelve a su lugar. */
    private static void vigilarSillas(MinecraftServer s) {
        Evento ev = Evento.get(s);
        for (Map.Entry<UUID, Silla> e : sillas.entrySet()) {
            if (e.getKey().equals(turno) && fase != Fase.SENTANDO) {
                continue;
            }
            Pieza p = ev.pieza(e.getKey());
            Entity ent = p == null || !p.vivo() ? null : Evento.entidad(s, p);
            Silla si = e.getValue();
            if (ent != null && si.isAlive() && ent.getVehicle() != si && fase != Fase.NADA && fase != Fase.FIN && fase != Fase.HABLANDO
                    && fase != Fase.LLEGANDO && fase != Fase.PASILLO) {
                sentar(s, ent, si);
                si.cerrar(true, false);
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
            BlockPos p = Complejo.pos(s, "sor.luz." + i);
            if (p == null) {
                break;
            }
            int nivel = Math.round(Complejo.yaw("sor.luz." + i));
            // la luz fuerte de arriba de la caja (13) no se baja: la caja siempre se ve
            int n = bajas && nivel < 13 ? Math.max(2, nivel / 3) : nivel;
            BlockState st = l.getBlockState(p);
            if (st.isAir() || st.is(Blocks.LIGHT)) {
                l.setBlock(p, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, n), 2);
            }
        }
    }

    private static void foco(MinecraftServer s, Entity silla) {
        ServerLevel l = s.overworld();
        if (foco != null && l.getBlockState(foco).is(Blocks.LIGHT)) {
            l.setBlock(foco, Blocks.AIR.defaultBlockState(), 2);
        }
        foco = null;
        if (silla == null) {
            return;
        }
        BlockPos p = BlockPos.containing(silla.getX(), silla.getY() + 3, silla.getZ());
        if (l.getBlockState(p).isAir()) {
            l.setBlock(p, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15), 2);
            foco = p;
        }
    }

    // ------------------------------------------------------------------ el final

    public static boolean terminar(MinecraftServer s) {
        if (fase == Fase.NADA || fase == Fase.FIN) {
            return false;
        }
        Pantallas.callar(s);
        fase = Fase.FIN;
        desde = t;
        turno = null;
        Evento ev = Evento.get(s);
        ev.reloj(s, 0);
        for (Silla si : sillas.values()) {
            si.cerrar(false);
        }
        foco(s, null);
        luces(s, false);
        if (caja != null && caja.estado() != CajaSorpresa.CERRADA) {
            caja.estado(CajaSorpresa.CERRADA);
        }
        Pantallas.hablar(s, "sorpresa_fin", () -> {
            Evento.get(s).prueba(s, Component.translatable(MOD + ".sor.fin_capitulo").getString());
            Coleccionista.LOGGER.info("COLECCIONISTA_CAPITULO4 fin vivos={}", Evento.get(s).vivos().size());
        });
        Anuncio.titulo(s, Component.translatable(MOD + ".sor.fin.titulo"), Component.translatable(MOD + ".sor.fin.sub", comidos));
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mio(s, sp);
        }
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_SORPRESA fin comidos={} vivos={}", comidos, ev.vivos().size());
        return true;
    }

    // ------------------------------------------------------------------ lo que mandan los clientes

    private static void accion(ServerPlayer p, CompoundTag d) {
        MinecraftServer s = p.server;
        String que = d.getString("que");
        if ("vueltas".equals(que) && p.getUUID().equals(turno)) {
            elegir(s, d.getInt("n"));
        }
    }

    // ------------------------------------------------------------------ el estado para los clientes

    public static CompoundTag estado() {
        CompoundTag e = new CompoundTag();
        e.putString("fase", fase.name());
        e.putInt("comidos", comidos);
        e.putInt("cuota", cuota);
        e.putBoolean("obligado", fase == Fase.IR || fase == Fase.ELIGE ? obligadoAhora : obligado);
        if (turno != null) {
            e.putUUID("turno", turno);
            e.putString("nombre", turnoNombre);
        }
        if (fase == Fase.GIRANDO) {
            e.putInt("quiere", quiere);
            e.putInt("dadas", dadas);
        }
        return e;
    }

    private static void enviar(MinecraftServer s) {
        PlayerStore.Sync.toAll(s, KEY, estado());
    }

    /** Lo de cada pieza: si le toca, a dónde va y si tiene que elegir (y si está obligado a dar 3). */
    private static void mio(MinecraftServer s, ServerPlayer sp) {
        CompoundTag d = new CompoundTag();
        if (fase != Fase.NADA && sp.getUUID().equals(turno)) {
            d.putBoolean("turno", true);
            d.putBoolean("elige", fase == Fase.ELIGE);
            d.putBoolean("obligado", obligadoAhora);
            d.putInt("segundos", segundosElegir);
        }
        PlayerStore.Sync.toPlayer(sp, KEY_MIO, d);
    }

    public static String resumen() {
        return fase.name() + " comidos=" + comidos + "/" + cuota + " cuenta=" + cuenta + " secreto=" + secreto + " obligado=" + obligado;
    }

    /** A dónde ir: al pasillo, al salón, a la caja (si te toca) o de regreso a tu silla. */
    public static Objetivo.Meta objetivo(MinecraftServer s, ServerPlayer sp) {
        if (fase == Fase.PASILLO && !Cabina.dentro(s, "pas_caj", sp, Pasaje.ANCHO, Pasaje.ALTO, Pasaje.LARGO)) {
            return Objetivo.Meta.en(Complejo.pos(s, "pas_caj.centro"), 1.5, MOD + ".obj.pasillo");
        }
        if (fase == Fase.LLEGANDO && desde <= t && !enSalon(s, sp)) {
            return Objetivo.Meta.en(Complejo.pos(s, "sor.entrada"), 1.5, MOD + ".obj.teatro");
        }
        if (sp.getUUID().equals(turno)) {
            if (fase == Fase.IR) {
                return Objetivo.Meta.en(Complejo.pos(s, "sor.manivela"), 1.2, MOD + ".obj.manivela");
            }
            Silla si = sillas.get(turno);
            if (fase == Fase.VOLVER && si != null) {
                return Objetivo.Meta.en(si.blockPosition(), 1.0, MOD + ".obj.tu_silla");
            }
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

    /** Quita todo: sillas, la caja, los zapatos, la sangre y el foco; vuelve la luz. */
    private static void limpiarTodo(MinecraftServer s) {
        ServerLevel l = s.overworld();
        for (Silla si : sillas.values()) {
            si.soltar();
            si.discard();
        }
        sillas.clear();
        if (agarre != null) {
            agarre.soltar();
            agarre.discard();
            agarre = null;
        }
        BlockPos o = Complejo.origen(s, "sorpresa");
        if (o != null) {
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(o.getX(), o.getY() - 2, o.getZ() - 2, o.getX() + 66, o.getY() + 18,
                    o.getZ() + 46);
            for (Silla si : l.getEntitiesOfClass(Silla.class, box, x -> "sorpresa".equals(x.grupo))) {
                si.soltar();
                si.discard();
            }
            for (CajaSorpresa c : l.getEntitiesOfClass(CajaSorpresa.class, box)) {
                c.discard();
            }
        }
        if (caja != null) {
            caja.discard();
            caja = null;
        }
        for (BlockPos q : puestos) {
            if (!l.getBlockState(q).isAir()) {
                l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        puestos.clear();
        foco(s, null);
        if (o != null) {
            lucesBajas = true;
            luces(s, false);
        }
        orden.clear();
        yawCentro.clear();
        turno = null;
        idx = -1;
        comidos = 0;
        cuota = 0;
        zapatosK = 0;
        cuenta = 0;
        obligado = false;
        obligadoAhora = false;
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase == Fase.HABLANDO || fase == Fase.FIN) {
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
                sentar(s);
                return true;
            }
            case IR -> {
                Pieza p = Evento.get(s).pieza(turno);
                Entity e = p == null ? null : Evento.entidad(s, p);
                if (e != null) {
                    llegoACaja(s, e);
                }
                return true;
            }
            case ELIGE -> {
                return elegir(s, obligadoAhora ? 3 : 1);
            }
            case VOLVER -> {
                Pieza p = Evento.get(s).pieza(turno);
                Entity e = p == null ? null : Evento.entidad(s, p);
                Silla si = sillas.get(turno);
                if (e != null && si != null) {
                    sentar(s, e, si);
                    sentado(s, si);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        return fase == Fase.IR || fase == Fase.ELIGE || fase == Fase.VOLVER;
    }
}
