package com.axeliscore.combat;

import com.axeliscore.AxelisCore;
import com.axeliscore.net.CoreNet;
import com.axeliscore.plat.Plat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * (Servidor) Estados de combate en cualquier criatura o jugador: aturdido, quemado, congelado, marcado y sangrado (y los que agreguen los
 * mods). Se ven en todos los clientes: ícono sobre la cabeza, partículas y, en el propio jugador, íconos con el tiempo arriba de la barra.
 * <pre>
 *     Estados.aplicar(objetivo, Estados.QUEMADO, 100);          // 5 segundos
 *     Estados.aplicar(objetivo, Estados.CONGELADO, 60, 2, jefe); // nivel 2, lo puso el jefe
 *     if (Estados.aturdido(this)) return;                         // en la IA de una criatura propia
 * </pre>
 * Si se vuelve a poner un estado que ya tiene, se queda con el tiempo y el nivel más altos. Los jefes ({@link Resiste}) pueden durar
 * menos o no tomarlos.
 */
public final class Estados {
    /** Una criatura que resiste estados: cuánto le dura cada uno (1 = normal, 0.3 = 30 %, 0 = no lo toma). */
    public interface Resiste {
        float axDuracionEstado(Estado s);
    }

    /** Un estado puesto en alguien. */
    public static final class Activo {
        public final Estado estado;
        public int ticks, max, nivel, edad;
        public Entity fuente;

        Activo(Estado estado, int ticks, int nivel, Entity fuente) {
            this.estado = estado;
            this.ticks = ticks;
            this.max = ticks;
            this.nivel = nivel;
            this.fuente = fuente;
        }
    }

    private static final Map<String, Estado> REGISTRO = new LinkedHashMap<>();
    private static final Map<LivingEntity, LinkedHashMap<Estado, Activo>> ACTIVOS = new WeakHashMap<>();
    private static boolean iniciado;

    /** Sin moverse ni atacar (los jefes del Core lo resisten). */
    public static final Estado ATURDIDO = registrar(new Estado(AxelisCore.MODID, "aturdido", 0xFFE14A).icono(0).aturde().particula("stars"));
    /** Pierde vida cada segundo (más por nivel); el agua lo apaga. */
    public static final Estado QUEMADO = registrar(new Estado(AxelisCore.MODID, "quemado", 0xFF7A2E).icono(1).seApagaConAgua().particula("flame")
            .cada(20, (e, a) -> {
                com.axeliscore.plat.Plat.sinInvulnerable(e);
                Plat.hurt(e, e.damageSources().onFire(), 1F * a.nivel);
            }));
    /** Más lento (20 % por nivel) y con escarcha. */
    public static final Estado CONGELADO = registrar(new Estado(AxelisCore.MODID, "congelado", 0x8FE3FF).icono(2).lento(0.2F).particula("snowflake")
            .cada(1, (e, a) -> e.setTicksFrozen(Math.max(e.getTicksFrozen(), Math.min(139, 60 + 40 * a.nivel))))
            .alQuitar((e, a) -> e.setTicksFrozen(Math.min(e.getTicksFrozen(), 40))));
    /** Recibe 25 % más de daño por nivel. */
    public static final Estado MARCADO = registrar(new Estado(AxelisCore.MODID, "marcado", 0xFF5AA0).icono(3).recibeMas(0.25F).particula("spark"));
    /** Pierde vida poco a poco (y más si se mueve). */
    public static final Estado SANGRADO = registrar(new Estado(AxelisCore.MODID, "sangrado", 0xD8263A).icono(4).particula("drip")
            .cada(30, (e, a) -> {
                var v = e.getDeltaMovement();
                boolean seMueve = v.x * v.x + v.z * v.z > 0.004;
                com.axeliscore.plat.Plat.sinInvulnerable(e);
                Plat.hurt(e, e.damageSources().magic(), a.nivel * (seMueve ? 1.5F : 1F));
            }));

    private Estados() {
    }

    /** Registra un estado (los del Core ya están). Llamarlo al arrancar, en el servidor y en el cliente. */
    public static synchronized Estado registrar(Estado s) {
        REGISTRO.put(s.id, s);
        return s;
    }

    public static Estado porId(String id) {
        return REGISTRO.get(id);
    }

    public static Collection<Estado> todos() {
        return REGISTRO.values();
    }

    /** Lo llama el Core al arrancar. */
    public static synchronized void init() {
        if (!iniciado) {
            iniciado = true;
            AxelisCore.onServerTick(Estados::tick);
        }
    }

    // ------------------------------------------------------------------ poner, quitar, preguntar

    public static boolean aplicar(LivingEntity e, Estado s, int ticks) {
        return aplicar(e, s, ticks, 1, null);
    }

    /** Pone un estado ({@code ticks}: cuánto dura; {@code nivel}: 1, 2, 3...). Devuelve false si no lo tomó (inmune o muerto). */
    public static boolean aplicar(LivingEntity e, Estado s, int ticks, int nivel, Entity fuente) {
        if (e == null || e.level().isClientSide() || !e.isAlive() || ticks <= 0) {
            return false;
        }
        if (e instanceof Resiste r) {
            ticks = Math.round(ticks * Math.max(0, r.axDuracionEstado(s)));
            if (ticks <= 0) {
                return false;
            }
        }
        if (e instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) {
            return false;
        }
        nivel = Math.max(1, nivel);
        var m = ACTIVOS.computeIfAbsent(e, k -> new LinkedHashMap<>());
        Activo a = m.get(s);
        if (a == null) {
            a = new Activo(s, ticks, nivel, fuente);
            m.put(s, a);
            if (s.alPoner != null) {
                s.alPoner.run(e, a);
            }
        } else {
            a.ticks = Math.max(a.ticks, ticks);
            a.max = Math.max(a.max, a.ticks);
            a.nivel = Math.max(a.nivel, nivel);
            if (fuente != null) {
                a.fuente = fuente;
            }
        }
        efectos(e, a, true);
        enviar(e);
        return true;
    }

    public static boolean tiene(LivingEntity e, Estado s) {
        var m = ACTIVOS.get(e);
        return m != null && m.containsKey(s);
    }

    /** Nivel del estado (0 si no lo tiene). */
    public static int nivel(LivingEntity e, Estado s) {
        var m = ACTIVOS.get(e);
        Activo a = m == null ? null : m.get(s);
        return a == null ? 0 : a.nivel;
    }

    /** ¿Está aturdido (por cualquier estado que aturda)? En el cliente responde con lo que llegó del servidor. */
    public static boolean aturdido(LivingEntity e) {
        if (e.level().isClientSide()) {
            return ClientBridge.aturdido(e);
        }
        var m = ACTIVOS.get(e);
        if (m == null) {
            return false;
        }
        for (Activo a : m.values()) {
            if (a.estado.aturde) {
                return true;
            }
        }
        return false;
    }

    /** Los estados que tiene (copia). */
    public static List<Activo> de(LivingEntity e) {
        var m = ACTIVOS.get(e);
        return m == null ? List.of() : new ArrayList<>(m.values());
    }

    public static void quitar(LivingEntity e, Estado s) {
        var m = ACTIVOS.get(e);
        if (m != null) {
            Activo a = m.remove(s);
            if (a != null) {
                terminar(e, a);
                enviar(e);
            }
        }
    }

    /** Le quita todos los estados. */
    public static void limpiar(LivingEntity e) {
        var m = ACTIVOS.remove(e);
        if (m != null) {
            for (Activo a : m.values()) {
                terminar(e, a);
            }
            enviar(e);
        }
    }

    /** Cuánto se multiplica el daño que recibe por sus estados (marcado...). */
    public static float multiplicadorDano(LivingEntity e) {
        var m = ACTIVOS.get(e);
        if (m == null) {
            return 1F;
        }
        float k = 1F;
        for (Activo a : m.values()) {
            if (a.estado.danoExtra != 0) {
                k *= 1F + a.estado.danoExtra * a.nivel;
            }
        }
        return Math.max(0F, k);
    }

    /** (Lo llama el Core cuando algo recibe daño) el daño ya con sus estados. */
    public static float alRecibirDano(LivingEntity e, float amount) {
        return amount * multiplicadorDano(e);
    }

    // ------------------------------------------------------------------ cada tick

    private static void tick(MinecraftServer s) {
        if (ACTIVOS.isEmpty()) {
            return;
        }
        List<LivingEntity> lista = new ArrayList<>(ACTIVOS.keySet());
        for (LivingEntity e : lista) {
            var m = ACTIVOS.get(e);
            if (m == null) {
                continue;
            }
            if (!e.isAlive() || e.isRemoved()) {
                ACTIVOS.remove(e);
                enviarVacio(e);
                continue;
            }
            boolean cambio = false;
            for (Activo a : new ArrayList<>(m.values())) {
                a.edad++;
                a.ticks--;
                Estado st = a.estado;
                if (st.seApagaConAgua && e.isInWater()) {
                    a.ticks = 0;
                }
                if (st.aturde) {
                    quieto(e);
                }
                if (a.edad % 20 == 0) {
                    efectos(e, a, false);
                }
                if (st.cada != null && a.edad % st.cadaTicks == 0 && a.ticks >= 0) {
                    try {
                        st.cada.run(e, a);
                    } catch (RuntimeException ex) {
                        AxelisCore.LOGGER.error("Axelis Core: falló el estado {}", st.id, ex);
                    }
                }
                if (a.ticks <= 0 || !e.isAlive()) {
                    m.remove(st);
                    terminar(e, a);
                    cambio = true;
                }
            }
            if (m.isEmpty()) {
                ACTIVOS.remove(e);
                cambio = true;
            }
            // cada 2 s se repite (para quien llegó después) y al cambiar, de inmediato
            if (cambio || e.tickCount % 40 == 0) {
                enviar(e);
            }
        }
    }

    /** Aturdido: la criatura no camina ni se lanza (los jugadores quedan casi quietos por la lentitud). */
    private static void quieto(LivingEntity e) {
        if (e instanceof Mob mob) {
            mob.getNavigation().stop();
            var v = mob.getDeltaMovement();
            mob.setDeltaMovement(0, Math.min(v.y, 0.0), 0);
        }
    }

    /** Efectos de Minecraft que acompañan al estado (ocultos: sin partículas ni ícono). */
    private static void efectos(LivingEntity e, Activo a, boolean alPoner) {
        Estado s = a.estado;
        int dur = Math.min(a.ticks + 2, 40);
        if (s.aturde) {
            e.addEffect(lento(dur, 9));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 9, false, false, false));
        } else if (s.lentitud > 0) {
            float k = Math.min(0.9F, s.lentitud * a.nivel);
            int amp = Math.max(0, Math.min(5, Math.round(k / 0.15F) - 1));
            e.addEffect(lento(dur, amp));
        }
    }

    private static void terminar(LivingEntity e, Activo a) {
        Estado s = a.estado;
        if (s.alQuitar != null) {
            try {
                s.alQuitar.run(e, a);
            } catch (RuntimeException ex) {
                AxelisCore.LOGGER.error("Axelis Core: falló al quitar el estado {}", s.id, ex);
            }
        }
        if (s.aturde) {
            var w = e.getEffect(MobEffects.WEAKNESS);
            if (w != null && w.getAmplifier() >= 9) {
                e.removeEffect(MobEffects.WEAKNESS);
            }
        }
        if (s.aturde || s.lentitud > 0) {
            var l = e.getEffect(lentoTipo());
            if (l != null && l.getDuration() <= 42) {
                e.removeEffect(lentoTipo());
            }
        }
    }

    //#if MC >= 26.1
    //$$ private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> lentoTipo() {
    //$$     return MobEffects.SLOWNESS;
    //$$ }
    //#elif MC >= 1.20.5
    //$$ private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> lentoTipo() {
    //$$     return MobEffects.MOVEMENT_SLOWDOWN;
    //$$ }
    //#else
    private static net.minecraft.world.effect.MobEffect lentoTipo() {
        return MobEffects.MOVEMENT_SLOWDOWN;
    }
    //#endif

    private static MobEffectInstance lento(int ticks, int amp) {
        return new MobEffectInstance(lentoTipo(), ticks, amp, false, false, false);
    }

    // ------------------------------------------------------------------ a los clientes

    private static void enviar(LivingEntity e) {
        var m = ACTIVOS.get(e);
        List<CoreNet.EstadosMsg.Item> items = new ArrayList<>();
        if (m != null) {
            for (Activo a : m.values()) {
                items.add(new CoreNet.EstadosMsg.Item(a.estado.id, Math.max(0, a.ticks), a.max, a.nivel));
            }
        }
        CoreNet.NET.toNear(e, 64, new CoreNet.EstadosMsg(e.getId(), items));
    }

    private static void enviarVacio(LivingEntity e) {
        CoreNet.NET.toNear(e, 64, new CoreNet.EstadosMsg(e.getId(), List.of()));
    }

    /** Al entrar un jugador: los estados de lo que tiene cerca (para que los vea aunque ya estuvieran puestos). */
    public static void alEntrar(ServerPlayer p) {
        for (LivingEntity e : new ArrayList<>(ACTIVOS.keySet())) {
            if (e.level() == p.level() && e.distanceToSqr(p) < 64 * 64) {
                enviar(e);
            }
        }
    }

    /** Puente al cliente (las clases del cliente solo se cargan allá). */
    static final class ClientBridge {
        static boolean aturdido(LivingEntity e) {
            boolean[] r = {false};
            com.axeliscore.plat.Net.onClient(() -> r[0] = com.axeliscore.combat.client.EstadosCliente.aturdido(e.getId()));
            return r[0];
        }
    }
}
