package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.forma.Formas;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import com.coleccionista.bot.PiezaBot;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Cabina;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Cuerpos;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Objetivo;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.mapa.Mansion;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Capítulo 2 · La mascota: las escondidas en la casa del Coleccionista ({@link Mansion}). Llegan por el pasillo de servicio (pasan sin que se
 * note desde el del teatro) y entran al vestíbulo; cuando están todos, el Coleccionista cuenta que se le escapó su mascota, Cascabel (se oye
 * rugir detrás de la reja), y les da un minuto para esconderse. Luego el organizador se convierte en Cascabel (una fiera disecada y cosida,
 * con máscara de porcelana y un cascabel en el collar) y sale de su jaula: a quien atrapa (un golpe), se lo come. Si se acaba el tiempo, los
 * que quedan se salvan y Cascabel vuelve a su jaula.
 * <p>
 * Cascabel ruge (tecla G: a los que están cerca les tiembla la pantalla) y olfatea (tecla H: oye el corazón de los que están escondidos
 * cerca, de donde vienen). Su cascabel suena mientras camina: así lo oyen venir.
 */
public final class Mascota {
    private static final String MOD = Coleccionista.MODID;
    public static final ResourceLocation FORMA = new ResourceLocation(MOD, "cascabel");
    public static final String KEY = MOD + ":mascota";
    /** A quien se están comiendo: desde cuándo y quién (para su cinemática). */
    public static final String KEY_COMIDO = MOD + ":comido";
    public static final String ACCION = MOD + ":mascota_accion";
    public static final String PRUEBA = "La mascota";
    public static int segundosEsconder = 60, segundosCazar = 300;
    /** Al terminar el capítulo 1 (la antesala del teatro), pasan solos a la casa. */
    public static boolean auto = true;
    /** Cuánto espera a que entren todos al vestíbulo antes de hablar de todos modos (ticks). */
    public static int esperaVestibulo = 900;

    public enum Fase {
        NADA, LLEGANDO, HABLANDO, ESCONDER, CAZA, FIN
    }

    private static Fase fase = Fase.NADA;
    private static long desde, ultimoRugido, ultimoOlfato, ultimoCascabel, gruneEn, finEn;
    private static boolean sacudio;
    /** Quién será Cascabel (si el organizador lo eligió) y quién lo es ahora. */
    private static UUID elegido, cazador;
    /** Dónde estaba y cómo estaba el organizador antes de transformarse (para regresarlo igual). */
    private static Vec3 antes;
    private static GameType modoAntes;
    private static Vec3 ultimaPos;
    private static final Map<UUID, Long> comiendo = new HashMap<>();
    private static final Map<UUID, Vec3> quieto = new HashMap<>();
    private static int comidos;
    private static final Random R = new Random();

    private Mascota() {
    }

    public static void init() {
        // la caja cabe por las puertas de la casa (2 de alto, 1 de ancho) aunque el modelo se vea más grande
        Formas.registrar(new Formas.Def(FORMA, 0.9F, 1.95F, 1.75F));
        AxelisCore.onServerTick(Mascota::tick);
        AxelisCore.onLogin(p -> PlayerStore.Sync.toPlayer(p, KEY, estado()));
        Actions.on(ACCION, Mascota::accion);
        MinecraftForge.EVENT_BUS.addListener(Mascota::golpe);
        // en la casa nadie se lastima: aquí solo se muere en el hocico de Cascabel
        MinecraftForge.EVENT_BUS.addListener((LivingHurtEvent e) -> {
            if (fase != Fase.NADA && e.getEntity() instanceof ServerPlayer) {
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

    /** Quién es Cascabel ahora (null: nadie). */
    public static UUID cazador() {
        return cazador;
    }

    // ------------------------------------------------------------------ llegar: del pasillo del teatro al de la casa

    /**
     * Los vivos que están en el pasillo de servicio del teatro pasan (sin que se note) al de la casa; se abren las dos puertas del fondo y
     * entran al vestíbulo. Devuelve null si se pudo.
     */
    public static Component llegar(MinecraftServer s) {
        if (!Complejo.construido(s, "mansion")) {
            return Component.translatable(MOD + ".man.sin_mapa");
        }
        Evento ev = Evento.get(s);
        Pruebas.cerrarOtras(s, "mascota");
        reiniciarCasa(s);
        List<Entity> van = new ArrayList<>();
        BlockPos o = Complejo.pos(s, "cab.pas_tea.origen");
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null) {
                van.add(e);
            }
        }
        // los que están en el pasillo pasan tal cual; los demás (si el organizador lo pidió desde otro lado) llegan al centro del pasillo
        List<Entity> enPasillo = new ArrayList<>(), fuera = new ArrayList<>();
        for (Entity e : van) {
            if (o != null && Cabina.dentro(s, "pas_tea", e, com.coleccionista.mapa.Pasaje.ANCHO, com.coleccionista.mapa.Pasaje.ALTO,
                    com.coleccionista.mapa.Pasaje.LARGO)) {
                enPasillo.add(e);
            } else {
                fuera.add(e);
            }
        }
        Cabina.trasladar(s, "pas_tea", "pas_man", enPasillo, List.of());
        BlockPos c = Complejo.pos(s, "pas_man.centro");
        int n = 0;
        for (Entity e : fuera) {
            if (c == null) {
                break;
            }
            double x = c.getX() + 0.5 - 1.5 + (n % 3) * 1.5, z = c.getZ() + 0.5 + (n / 3 % 4);
            if (e instanceof ServerPlayer sp) {
                sp.stopRiding();
                sp.teleportTo(s.overworld(), x, c.getY(), z, 270F, 0F);
            } else {
                e.stopRiding();
                e.moveTo(x, c.getY(), z, 270F, 0F);
            }
            n++;
        }
        puertasPasillo(s, true);
        fase = Fase.LLEGANDO;
        desde = s.overworld().getGameTime();
        ev.prueba(s, Component.translatable(MOD + ".man.prueba_llegar").getString());
        ev.reloj(s, 0);
        Pantallas.modo(s, "apagada", "");
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_MASCOTA llegan pasillo={} fuera={}", enPasillo.size(), fuera.size());
        return null;
    }

    private static void puertasPasillo(MinecraftServer s, boolean abrir) {
        puerta(s, "pas_man.izq", abrir);
        puerta(s, "pas_man.der", abrir);
    }

    /** Las dos hojas de una puerta ({@code marca.a} y {@code marca.b}). */
    private static void puerta(MinecraftServer s, String marca, boolean abrir) {
        ServerLevel l = s.overworld();
        for (String h : new String[] {".a", ".b"}) {
            BlockPos p = Complejo.pos(s, marca + h);
            if (p == null) {
                continue;
            }
            for (BlockPos q : new BlockPos[] {p, p.above()}) {
                BlockState st = l.getBlockState(q);
                if (st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN) != abrir) {
                    l.setBlock(q, st.setValue(net.minecraft.world.level.block.DoorBlock.OPEN, abrir), 2 | 16);
                }
            }
        }
    }

    /** ¿Ya está dentro del vestíbulo (pasó las puertas del pasillo)? */
    private static boolean enVestibulo(MinecraftServer s, Entity e) {
        BlockPos o = Complejo.origen(s, "mansion");
        if (o == null) {
            return false;
        }
        double x = e.getX() - o.getX(), z = e.getZ() - o.getZ();
        return x >= Mansion.HX0 && x <= Mansion.HX1 + 1 && z >= Mansion.HZ0 && z <= Mansion.HZ1 + 1;
    }

    // ------------------------------------------------------------------ el discurso y a esconderse

    private static void hablar(MinecraftServer s) {
        fase = Fase.HABLANDO;
        desde = s.overworld().getGameTime();
        sacudio = false;
        // nadie se queda en el pasillo (ahí estaría a salvo): los que no entraron pasan al vestíbulo antes de que se cierren las puertas
        BlockPos c = Complejo.pos(s, "man.entrada");
        for (Pieza p : Evento.get(s).vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null && c != null && !enVestibulo(s, e)) {
                if (e instanceof ServerPlayer sp) {
                    sp.teleportTo(s.overworld(), c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 270F, 0F);
                } else {
                    e.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 270F, 0F);
                }
            }
        }
        puertasPasillo(s, false);
        Escena.presentacion(s, "II", MOD + ".man.titulo", MOD + ".man.lema");
        Pantallas.soloGrande(s, true);
        Pantallas.hablar(s, "mansion", () -> esconder(s));
        Evento.get(s).prueba(s, PRUEBA);
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_MASCOTA habla");
    }

    private static void esconder(MinecraftServer s) {
        if (fase != Fase.HABLANDO && fase != Fase.LLEGANDO) {
            return;
        }
        fase = Fase.ESCONDER;
        desde = s.overworld().getGameTime();
        Evento ev = Evento.get(s);
        ev.reloj(s, segundosEsconder);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".man.pantalla.esconder").getString());
        Anuncio.titulo(s, Component.translatable(MOD + ".man.esconder.titulo"), Component.translatable(MOD + ".man.esconder.sub", segundosEsconder));
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.6F);
        // los bots (solo en las pruebas) se van solos a un escondite
        List<BlockPos> esc = escondites(s);
        java.util.Collections.shuffle(esc, R);
        int i = 0;
        for (Pieza p : ev.vivos()) {
            if (p.bot && Evento.entidad(s, p) instanceof PiezaBot b && i < esc.size()) {
                BlockPos q = esc.get(i++);
                b.getNavigation().stop();
                b.moveTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5, R.nextFloat() * 360, 0F);
            }
        }
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_MASCOTA esconder segundos={} escondites={}", segundosEsconder, esc.size());
    }

    private static List<BlockPos> escondites(MinecraftServer s) {
        List<BlockPos> l = new ArrayList<>();
        for (int n = 0; n < 64; n++) {
            BlockPos q = Complejo.pos(s, "man.esc." + n);
            if (q == null) {
                break;
            }
            l.add(q);
        }
        return l;
    }

    // ------------------------------------------------------------------ sale Cascabel

    /** El organizador que será Cascabel ({@code null}: quien lo pida o el primer organizador conectado). */
    public static void elegir(UUID id) {
        elegido = id;
    }

    /** ¿Quién se convierte? El elegido; si no, el primer operador u organizador conectado que no es pieza viva. */
    private static ServerPlayer quien(MinecraftServer s) {
        Evento ev = Evento.get(s);
        if (elegido != null) {
            ServerPlayer p = s.getPlayerList().getPlayer(elegido);
            if (p != null) {
                return p;
            }
        }
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            Pieza pz = ev.pieza(p);
            if ((pz == null || !pz.vivo()) && (Plat.isOp(p) || Plat.isHost(p) || ev.esOrganizador(p.getUUID()))) {
                return p;
            }
        }
        return null;
    }

    /** Se acabó el minuto (o el organizador lo pide): el organizador se convierte en Cascabel y se abre la reja. */
    public static boolean soltar(MinecraftServer s) {
        if (fase != Fase.ESCONDER && fase != Fase.HABLANDO && fase != Fase.LLEGANDO) {
            return false;
        }
        if (fase != Fase.ESCONDER) {
            Pantallas.callar(s);
        }
        fase = Fase.CAZA;
        desde = s.overworld().getGameTime();
        Evento ev = Evento.get(s);
        ServerPlayer p = quien(s);
        BlockPos j = Complejo.pos(s, "man.jaula");
        if (p != null && j != null) {
            cazador = p.getUUID();
            antes = p.position();
            modoAntes = p.gameMode.getGameModeForPlayer();
            p.setGameMode(GameType.ADVENTURE);
            p.stopRiding();
            p.teleportTo(s.overworld(), j.getX() + 0.5, j.getY(), j.getZ() + 0.5, 90F, 0F);
            Formas.poner(p, FORMA);
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * (segundosCazar + 30), 0, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * (segundosCazar + 30), 0, false, false));
            p.setInvulnerable(true);
            Formas.accion(p, "rugir");
            Plat.msg(p, Component.translatable(MOD + ".man.eres").withStyle(ChatFormatting.GOLD), false);
            Coleccionista.LOGGER.info("COLECCIONISTA_MASCOTA cazador {}", p.getGameProfile().getName());
        } else {
            Coleccionista.LOGGER.info("COLECCIONISTA_MASCOTA sin_cazador");
            Anuncio.organizadores(s, Component.translatable(MOD + ".man.sin_cazador"));
        }
        reja(s, false);
        ev.reloj(s, segundosCazar);
        Pantallas.hablar(s, "mansion_suelta", null);
        BlockPos g = Complejo.pos(s, "man.reja.3");
        if (g != null) {
            sonido(s, g, SoundEvents.IRON_DOOR_OPEN, 2F, 0.5F);
            sonido(s, g, SoundEvents.ANVIL_LAND, 1.5F, 0.5F);
            sonido(s, g, son("rugido"), 3F, 1F);
        }
        Anuncio.titulo(s, Component.translatable(MOD + ".man.suelta.titulo"), Component.translatable(MOD + ".man.suelta.sub"));
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            Fx.shake(sp, 0.7F, 14);
        }
        enviar(s);
        return true;
    }

    /** Detrás de la reja (en lo oscuro) algo grande ruge y la sacude. */
    private static void sacudirReja(MinecraftServer s, boolean fuerte) {
        BlockPos g = Complejo.pos(s, "man.reja." + R.nextInt(Mansion.REJA_Z1 - Mansion.REJA_Z0 + 1));
        if (g == null) {
            return;
        }
        sonido(s, g, fuerte ? son("rugido") : son("rugido_corto"), fuerte ? 3F : 2F, 1F);
        sonido(s, g, SoundEvents.CHAIN_HIT, 1.5F, 0.6F);
        sonido(s, g, SoundEvents.IRON_DOOR_CLOSE, 1.4F, 0.5F);
        s.overworld().sendParticles(ParticleTypes.CRIT, g.getX() + 0.5, g.getY() + 1.5, g.getZ() + 0.5, 10, 0.1, 1.2, 0.4, 0.1);
        if (fuerte) {
            for (Pieza v : Evento.get(s).vivos()) {
                if (Evento.entidad(s, v) instanceof ServerPlayer vp && vp.distanceToSqr(g.getX(), g.getY(), g.getZ()) < 20 * 20) {
                    Fx.shake(vp, 0.5F, 10);
                }
            }
        }
    }

    /** La reja de la jaula: cerrada (barrotes) o abierta (de golpe). */
    private static void reja(MinecraftServer s, boolean cerrada) {
        ServerLevel l = s.overworld();
        BlockState bar = Complejo.estado(MOD + ":barrotes");
        for (int k = 0; k <= Mansion.REJA_Z1 - Mansion.REJA_Z0; k++) {
            BlockPos b = Complejo.pos(s, "man.reja." + k);
            if (b == null) {
                continue;
            }
            for (int y = 0; y < Mansion.REJA_ALTO; y++) {
                l.setBlock(b.above(y), cerrada ? bar : Blocks.AIR.defaultBlockState(), 2 | 16);
                if (!cerrada && y % 2 == 0) {
                    l.sendParticles(ParticleTypes.CRIT, b.getX() + 0.5, b.getY() + y + 0.5, b.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.2);
                }
            }
        }
        if (cerrada) {
            for (int k = 0; k <= Mansion.REJA_Z1 - Mansion.REJA_Z0; k++) {
                BlockPos b = Complejo.pos(s, "man.reja." + k);
                for (int y = 0; b != null && y < Mansion.REJA_ALTO; y++) {
                    BlockPos q = b.above(y);
                    l.setBlock(q, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(l.getBlockState(q), l, q), 2 | 16);
                }
            }
        }
    }

    // ------------------------------------------------------------------ la caza

    /** Cascabel golpea a alguien: lo atrapa y se lo come (si es una pieza viva y no se está comiendo a otro). */
    private static void golpe(AttackEntityEvent e) {
        if (fase != Fase.CAZA || !(e.getEntity() instanceof ServerPlayer p) || !p.getUUID().equals(cazador)) {
            return;
        }
        e.setCanceled(true);
        Entity t = e.getTarget();
        Pieza v = Evento.get(p.server).pieza(t);
        if (v == null || !v.vivo() || !comiendo.isEmpty()) {
            return;
        }
        comer(p.server, p, v, t);
    }

    /** Se lo come: la presa se queda quieta, Cascabel la muerde y la sacude, la jala al hocico y se la traga. */
    private static void comer(MinecraftServer s, ServerPlayer p, Pieza v, Entity t) {
        long now = s.overworld().getGameTime();
        comiendo.put(v.id, now);
        quieto.put(v.id, t.position());
        if (p != null) {
            Formas.accion(p, "comer");
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 5, false, false));
            sonido(s, p.blockPosition(), son("rugido_corto"), 1.6F, 1F);
        }
        if (t instanceof ServerPlayer vp) {
            CompoundTag c = new CompoundTag();
            c.putLong("desde", now);
            if (p != null) {
                c.putUUID("cazador", p.getUUID());
            }
            PlayerStore.Sync.toPlayer(vp, KEY_COMIDO, c);
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_MASCOTA atrapa {} {}", v.etiqueta(), v.nombre);
    }

    /** Para las pruebas (y por si el organizador lo necesita): Cascabel atrapa a esa pieza aunque no esté cerca. */
    public static boolean comerA(MinecraftServer s, Pieza v) {
        if (fase != Fase.CAZA || v == null || !v.vivo() || !comiendo.isEmpty()) {
            return false;
        }
        Entity t = Evento.entidad(s, v);
        if (t == null) {
            return false;
        }
        ServerPlayer p = cazador == null ? null : s.getPlayerList().getPlayer(cazador);
        comer(s, p, v, t);
        return true;
    }

    private static void comiendoTick(MinecraftServer s, long now) {
        if (comiendo.isEmpty()) {
            return;
        }
        Evento ev = Evento.get(s);
        ServerPlayer p = cazador == null ? null : s.getPlayerList().getPlayer(cazador);
        for (Map.Entry<UUID, Long> e : new ArrayList<>(comiendo.entrySet())) {
            Pieza v = ev.pieza(e.getKey());
            Entity t = v == null ? null : Evento.entidad(s, v);
            long dt = now - e.getValue();
            if (t == null || v == null || !v.vivo()) {
                comiendo.remove(e.getKey());
                continue;
            }
            // la presa no se puede ir: primero quieta, luego la jala hacia el hocico
            Vec3 q = quieto.getOrDefault(e.getKey(), t.position());
            Vec3 boca = p == null ? q : boca(p);
            Vec3 aqui = dt < 8 ? q : dt < 20 ? q.lerp(boca, (dt - 8) / 12.0) : boca;
            if (t instanceof ServerPlayer vp) {
                // mirando a Cascabel (para que vea el hocico que se le viene encima)
                float yaw = vp.getYRot(), pitch = vp.getXRot();
                if (p != null) {
                    double dx = p.getX() - aqui.x, dz = p.getZ() - aqui.z, dy = p.getEyeY() - (aqui.y + vp.getEyeHeight());
                    yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
                }
                vp.teleportTo(s.overworld(), aqui.x, aqui.y, aqui.z, yaw, pitch);
            } else {
                t.moveTo(aqui.x, aqui.y, aqui.z, t.getYRot(), t.getXRot());
            }
            t.setDeltaMovement(Vec3.ZERO);
            if (dt == 8 || dt == 14) {
                sonido(s, BlockPos.containing(aqui), son("mordida"), 1.4F, 1F);
                s.overworld().sendParticles(ParticleTypes.DAMAGE_INDICATOR, aqui.x, aqui.y + 1, aqui.z, 6, 0.3, 0.3, 0.3, 0.1);
            }
            if (dt == 20) {
                // adentro: ya no se ve
                t.setInvisible(true);
                s.overworld().sendParticles(ParticleTypes.POOF, aqui.x, aqui.y + 0.8, aqui.z, 16, 0.3, 0.4, 0.3, 0.02);
            }
            if (dt >= 46) {
                comiendo.remove(e.getKey());
                quieto.remove(e.getKey());
                sonido(s, BlockPos.containing(aqui), son("trago"), 1.5F, 1F);
                t.setInvisible(false);
                Cuerpos.SIN_CUERPO.add(v.id);
                String motivo = Component.translatable(MOD + ".man.motivo").getString();
                if (ev.eliminarEnPrueba(s, v, motivo)) {
                    comidos++;
                    Coleccionista.LOGGER.info("COLECCIONISTA_MASCOTA comio {} {}", v.etiqueta(), v.nombre);
                } else {
                    // se salvó (quedan muy pocos para lo que sigue): Cascabel lo escupe
                    Cuerpos.SIN_CUERPO.remove(v.id);
                    escupir(s, p, t);
                    Coleccionista.LOGGER.info("COLECCIONISTA_MASCOTA escupe {} {}", v.etiqueta(), v.nombre);
                }
                if (t instanceof ServerPlayer vp) {
                    PlayerStore.Sync.toPlayer(vp, KEY_COMIDO, new CompoundTag());
                }
                enviar(s);
            }
        }
    }

    /** No se lo pudo comer: lo avienta lejos (vivo, babeado y asustado). */
    private static void escupir(MinecraftServer s, ServerPlayer p, Entity t) {
        double r = Math.toRadians(p == null ? t.getYRot() : p.getYRot());
        Vec3 d = new Vec3(-Math.sin(r), 0, Math.cos(r)).scale(1.1).add(0, 0.45, 0);
        t.setDeltaMovement(d);
        t.hurtMarked = true;
        if (t instanceof ServerPlayer vp) {
            Plat.msg(vp, Component.translatable(MOD + ".man.escupido").withStyle(ChatFormatting.GREEN), false);
            vp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
        }
        sonido(s, t.blockPosition(), SoundEvents.SLIME_SQUISH, 1.5F, 0.6F);
        s.overworld().sendParticles(ParticleTypes.SPIT, t.getX(), t.getY() + 1, t.getZ(), 20, 0.3, 0.3, 0.3, 0.2);
    }

    /** Dónde queda el hocico de Cascabel (un poco al frente de su pecho, a la altura de la cabeza). */
    private static Vec3 boca(ServerPlayer p) {
        double r = Math.toRadians(p.getYRot());
        return new Vec3(p.getX() - Math.sin(r) * 2.1, p.getY() + 0.9, p.getZ() + Math.cos(r) * 2.1);
    }

    /** Las teclas de Cascabel (las manda el cliente del organizador transformado). */
    private static void accion(ServerPlayer p, CompoundTag d) {
        if (fase != Fase.CAZA || !p.getUUID().equals(cazador)) {
            return;
        }
        MinecraftServer s = p.server;
        long now = s.overworld().getGameTime();
        String que = d.getString("que");
        if ("rugir".equals(que) && now - ultimoRugido >= 120) {
            ultimoRugido = now;
            Formas.accion(p, "rugir");
            sonido(s, p.blockPosition(), son("rugido"), 3F, 1F);
            // a los que están cerca les tiembla todo
            for (Pieza v : Evento.get(s).vivos()) {
                if (Evento.entidad(s, v) instanceof ServerPlayer vp && vp.distanceToSqr(p) < 14 * 14) {
                    Fx.shake(vp, 0.9F, 16);
                    vp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1F, 1.2F);
                }
            }
            Coleccionista.LOGGER.info("COLECCIONISTA_MASCOTA ruge");
        } else if ("olfatear".equals(que) && now - ultimoOlfato >= 300) {
            ultimoOlfato = now;
            Formas.accion(p, "olfatear");
            sonido(s, p.blockPosition(), son("olfatea"), 1.2F, 1F);
            // oye el corazón de los que están cerca, de donde vienen (solo él)
            int n = 0;
            for (Pieza v : Evento.get(s).vivos()) {
                Entity t = Evento.entidad(s, v);
                if (t != null && t.distanceToSqr(p) < 18 * 18) {
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                            net.minecraft.core.Holder.direct(SoundEvents.WARDEN_HEARTBEAT), SoundSource.PLAYERS, t.getX(), t.getY() + 1, t.getZ(), 2.5F,
                            0.9F, R.nextLong()));
                    s.overworld().sendParticles(p, ParticleTypes.SCULK_SOUL, true, t.getX(), t.getY() + 1.2, t.getZ(), 5, 0.2, 0.3, 0.2, 0.01);
                    n++;
                }
            }
            Plat.msg(p, Component.translatable(MOD + ".man.olfato", n).withStyle(ChatFormatting.GOLD), true);
            Coleccionista.LOGGER.info("COLECCIONISTA_MASCOTA olfatea cerca={}", n);
        }
    }

    // ------------------------------------------------------------------ cada tick

    private static void tick(MinecraftServer s) {
        if (fase == Fase.NADA) {
            return;
        }
        long now = s.overworld().getGameTime();
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        switch (fase) {
            case LLEGANDO -> {
                if (now % 10 == 0) {
                    boolean todos = true;
                    int vivos = 0;
                    for (Pieza p : ev.vivos()) {
                        Entity e = Evento.entidad(s, p);
                        if (e == null) {
                            continue;
                        }
                        vivos++;
                        // los bots (solo en las pruebas) entran solos
                        if (p.bot && !enVestibulo(s, e) && now - desde >= 60) {
                            BlockPos c = Complejo.pos(s, "man.entrada");
                            if (c != null) {
                                e.moveTo(c.getX() + 0.5 + R.nextInt(3), c.getY(), c.getZ() + 0.5 - 2 + R.nextInt(5), 270F, 0F);
                            }
                        }
                        todos &= enVestibulo(s, e);
                    }
                    if (vivos > 0 && todos || now - desde >= esperaVestibulo) {
                        hablar(s);
                    }
                }
            }
            case HABLANDO -> {
                // "¿Cascabel?… uy, creo que se escapó" (silencio)… "ah, no, ahí está": rugido y los barrotes se sacuden; luego sigue gruñendo
                String c = Pantallas.clave();
                if (c.endsWith(".mansion.ahi_esta") && !sacudio) {
                    sacudio = true;
                    sacudirReja(s, true);
                    gruneEn = now + 90;
                } else if (sacudio && now >= gruneEn) {
                    gruneEn = now + 70 + R.nextInt(90);
                    sacudirReja(s, R.nextInt(3) == 0);
                }
            }
            case ESCONDER -> {
                if (now >= gruneEn) {
                    gruneEn = now + 120 + R.nextInt(120);
                    BlockPos g = Complejo.pos(s, "man.jaula");
                    if (g != null) {
                        sonido(s, g, R.nextInt(2) == 0 ? son("rugido_corto") : son("cascabel"), 2.5F, 1F);
                    }
                }
                int resta = ev.resta(s);
                if (resta >= 0 && resta <= 10 && now % 20 == 0) {
                    for (Pieza p : ev.vivos()) {
                        if (Evento.entidad(s, p) instanceof ServerPlayer sp) {
                            sp.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.get(), SoundSource.MASTER, 0.8F, 0.6F);
                        }
                    }
                }
            }
            case CAZA -> {
                comiendoTick(s, now);
                ServerPlayer p = cazador == null ? null : s.getPlayerList().getPlayer(cazador);
                if (p != null) {
                    // el cascabel suena mientras camina (más seguido si corre): así lo oyen venir
                    Vec3 v = p.position();
                    double dist = ultimaPos == null ? 0 : v.distanceTo(ultimaPos);
                    ultimaPos = v;
                    int cada = dist > 0.2 ? 7 : dist > 0.04 ? 12 : 0;
                    if (cada > 0 && now - ultimoCascabel >= cada) {
                        ultimoCascabel = now;
                        s.overworld().playSound(null, p.getX(), p.getY() + 1, p.getZ(), son("cascabel"), SoundSource.HOSTILE, 1.1F,
                                0.9F + R.nextFloat() * 0.2F);
                    }
                    if (dist > 0.04 && now % 9 == 0) {
                        s.overworld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 0.5F, 0.7F);
                    }
                }
                if (ev.vivos().isEmpty()) {
                    terminar(s);
                }
            }
            case FIN -> {
                if (finEn > 0 && now >= finEn) {
                    finEn = 0;
                    volverCazador(s);
                }
            }
            default -> {
            }
        }
        if (now % 40 == 0) {
            enviar(s);
        }
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        if (fase == Fase.ESCONDER) {
            soltar(s);
            return true;
        }
        if (fase == Fase.CAZA) {
            terminar(s);
            return true;
        }
        return false;
    }

    /** Se acabó la caza: Cascabel vuelve a su jaula (el organizador a su forma y a donde estaba) y los que quedan se salvan. */
    public static boolean terminar(MinecraftServer s) {
        if (fase != Fase.CAZA) {
            return false;
        }
        fase = Fase.FIN;
        Evento ev = Evento.get(s);
        ev.reloj(s, 0);
        // los que se estaban comiendo se salvan (los suelta)
        for (UUID id : comiendo.keySet()) {
            Pieza v = ev.pieza(id);
            Entity t = v == null ? null : Evento.entidad(s, v);
            if (t != null) {
                t.setInvisible(false);
                if (t instanceof ServerPlayer vp) {
                    PlayerStore.Sync.toPlayer(vp, KEY_COMIDO, new CompoundTag());
                }
            }
        }
        comiendo.clear();
        quieto.clear();
        // Cascabel vuelve a su jaula (todavía como Cascabel), se cierra la reja y luego el organizador vuelve a ser él
        ServerPlayer p = cazador == null ? null : s.getPlayerList().getPlayer(cazador);
        BlockPos j = Complejo.pos(s, "man.jaula");
        if (p != null && j != null) {
            p.teleportTo(s.overworld(), j.getX() + 0.5, j.getY(), j.getZ() + 0.5, 90F, 0F);
            Formas.accion(p, "rugir");
            sonido(s, j, son("rugido"), 2.5F, 0.9F);
        }
        reja(s, true);
        BlockPos g = Complejo.pos(s, "man.reja.3");
        if (g != null) {
            sonido(s, g, SoundEvents.IRON_DOOR_CLOSE, 2F, 0.5F);
            sonido(s, g, SoundEvents.ANVIL_LAND, 1.2F, 0.6F);
        }
        finEn = s.overworld().getGameTime() + 70;
        Pantallas.hablar(s, "mansion_fin", () -> {
            Evento.get(s).prueba(s, Component.translatable(MOD + ".man.fin_capitulo").getString());
            Coleccionista.LOGGER.info("COLECCIONISTA_CAPITULO2 fin vivos={}", Evento.get(s).vivos().size());
        });
        Anuncio.titulo(s, Component.translatable(MOD + ".man.fin.titulo"), Component.translatable(MOD + ".man.fin.sub", ev.vivos().size()));
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_MASCOTA fin comidos={} vivos={}", comidos, ev.vivos().size());
        return true;
    }

    private static void volverCazador(MinecraftServer s) {
        ServerPlayer p = cazador == null ? null : s.getPlayerList().getPlayer(cazador);
        if (p != null) {
            Formas.quitar(p);
            p.removeEffect(MobEffects.NIGHT_VISION);
            p.removeEffect(MobEffects.MOVEMENT_SPEED);
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            p.setInvulnerable(false);
            if (antes != null) {
                p.teleportTo(s.overworld(), antes.x, antes.y, antes.z, p.getYRot(), p.getXRot());
            }
            if (modoAntes != null) {
                p.setGameMode(modoAntes);
            }
        }
        cazador = null;
        antes = null;
        modoAntes = null;
        ultimaPos = null;
        enviar(s);
    }

    // ------------------------------------------------------------------ el indicador y el estado

    /** A dónde ir: al llegar, al vestíbulo. (Para esconderse no se dice nada.) */
    public static Objetivo.Meta objetivo(MinecraftServer s, ServerPlayer sp) {
        if (fase == Fase.LLEGANDO && !enVestibulo(s, sp)) {
            return Objetivo.Meta.en(Complejo.pos(s, "man.centro"), 1.5, MOD + ".obj.vestibulo");
        }
        return null;
    }

    private static SoundEvent son(String id) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD, id));
    }

    private static void sonido(MinecraftServer s, BlockPos p, SoundEvent e, float vol, float tono) {
        s.overworld().playSound(null, p, e, SoundSource.HOSTILE, vol, tono);
    }

    public static CompoundTag estado() {
        CompoundTag t = new CompoundTag();
        t.putString("fase", fase.name());
        if (cazador != null) {
            t.putUUID("cazador", cazador);
        }
        t.putInt("comidos", comidos);
        return t;
    }

    private static void enviar(MinecraftServer s) {
        PlayerStore.Sync.toAll(s, KEY, estado());
    }

    public static String resumen() {
        return fase.name() + " comidos=" + comidos + (cazador != null ? " cazador" : "");
    }

    /** Deja la casa como nueva: la reja cerrada, las puertas del pasillo cerradas. */
    private static void reiniciarCasa(MinecraftServer s) {
        if (!Complejo.construido(s, "mansion")) {
            return;
        }
        reja(s, true);
        puertasPasillo(s, false);
    }

    public static void reiniciar(MinecraftServer s) {
        volverCazador(s);
        if (fase == Fase.HABLANDO || fase == Fase.CAZA) {
            Pantallas.callar(s);
        }
        fase = Fase.NADA;
        comiendo.clear();
        quieto.clear();
        comidos = 0;
        finEn = 0;
        sacudio = false;
        reiniciarCasa(s);
        enviar(s);
    }

    /** Para el organizador: salta al siguiente paso (habla ya / suelta ya). */
    public static boolean saltar(MinecraftServer s) {
        switch (fase) {
            case LLEGANDO -> {
                hablar(s);
                return true;
            }
            case HABLANDO -> {
                Pantallas.callar(s);
                esconder(s);
                return true;
            }
            case ESCONDER -> {
                return soltar(s);
            }
            default -> {
                return false;
            }
        }
    }
}
