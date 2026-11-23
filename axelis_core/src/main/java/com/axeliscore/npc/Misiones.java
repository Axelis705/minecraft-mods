package com.axeliscore.npc;

import com.axeliscore.AxelisCore;
import com.axeliscore.combat.Combate;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * (Servidor) Las misiones de cada jugador: aceptar, avanzar, entregar y abandonar. Todo se guarda con el jugador en
 * {@code PlayerStore.of(p, "axelis_core")} → "misiones" y el cliente lo ve para el diario y el rastreador:
 * <pre>
 *     misiones: { activas: { "mimod:hierro": { p: [3, 0], d: 12 } },   // progreso de cada objetivo, día en que se aceptó
 *                 hechas:  { "mimod:hierro": 2 },                        // veces que se entregó
 *                 ult:     { "mimod:hierro": 15 },                       // último día en que se entregó (para las diarias)
 *                 sigue:   "mimod:hierro" }                              // la que se ve en pantalla
 * </pre>
 * Los mods avisan de lo suyo con {@link #evento}; cazar, tener, entregar, llegar y hablar los cuenta el Core.
 */
public final class Misiones {
    /** Cómo está una misión para un jugador. */
    public enum Estado {
        /** Falta otra misión antes o más amistad. */
        BLOQUEADA,
        /** Se puede aceptar. */
        DISPONIBLE,
        /** Aceptada, falta algo. */
        ACTIVA,
        /** Todo listo: falta entregarla. */
        LISTA,
        /** Ya hecha (y no se repite, o es diaria y ya se hizo hoy). */
        HECHA
    }

    private static final Map<String, Mision> REGISTRO = new LinkedHashMap<>();
    private static final int MAX_ACTIVAS = 12;
    private static boolean iniciado;
    private static int reloj;

    private Misiones() {
    }

    /** (Core) Al arrancar. */
    public static synchronized void init() {
        if (iniciado) {
            return;
        }
        iniciado = true;
        PlayerStore.syncOnLogin(AxelisCore.MODID);
        AxelisCore.onServerTick(Misiones::tick);
    }

    /** Registra una misión suelta (las de un NPC se registran solas con él). */
    public static synchronized Mision registrar(Mision m) {
        REGISTRO.put(m.id, m);
        return m;
    }

    public static Mision porId(String id) {
        return id == null ? null : REGISTRO.get(id);
    }

    public static Collection<Mision> todas() {
        return REGISTRO.values();
    }

    /** Las misiones que da un NPC. */
    public static List<Mision> deNpc(String npc) {
        List<Mision> l = new ArrayList<>();
        for (Mision m : REGISTRO.values()) {
            if (m.npc.equals(npc)) {
                l.add(m);
            }
        }
        return l;
    }

    // ------------------------------------------------------------------ datos

    static CompoundTag datos(ServerPlayer p) {
        CompoundTag d = PlayerStore.of(p, AxelisCore.MODID);
        if (!d.contains("misiones")) {
            d.put("misiones", new CompoundTag());
        }
        CompoundTag m = Nbt.c(d, "misiones");
        for (String k : new String[] {"activas", "hechas", "ult"}) {
            if (!m.contains(k)) {
                m.put(k, new CompoundTag());
            }
        }
        return m;
    }

    private static CompoundTag activas(ServerPlayer p) {
        return Nbt.c(datos(p), "activas");
    }

    private static void guardar(ServerPlayer p) {
        PlayerStore.save(p, AxelisCore.MODID);
    }

    /** Día del juego (para las misiones diarias). */
    public static long dia(ServerPlayer p) {
        return Plat.dayTime(p.level()) / 24000L;
    }

    public static boolean activa(ServerPlayer p, String id) {
        return activas(p).contains(id);
    }

    public static int vecesHecha(ServerPlayer p, String id) {
        return Nbt.i(Nbt.c(datos(p), "hechas"), id);
    }

    /** Progreso de cada objetivo (vacío si no está activa). */
    public static int[] progreso(ServerPlayer p, String id) {
        CompoundTag a = activas(p);
        return a.contains(id) ? Nbt.ia(Nbt.c(a, id), "p") : new int[0];
    }

    public static Estado estado(ServerPlayer p, Mision m) {
        return estadoDe(m, datos(p), Amistad.datos(p), dia(p));
    }

    /**
     * Cómo está una misión según los datos del jugador (sirve igual en el servidor y en el cliente, con
     * {@code ClientData.player("axelis_core")}).
     */
    public static Estado estadoDe(Mision m, CompoundTag misiones, CompoundTag amistad, long dia) {
        CompoundTag act = Nbt.c(misiones, "activas");
        if (act.contains(m.id)) {
            return completa(m, Nbt.ia(Nbt.c(act, m.id), "p")) ? Estado.LISTA : Estado.ACTIVA;
        }
        int veces = Nbt.i(Nbt.c(misiones, "hechas"), m.id);
        if (veces > 0) {
            if (m.diaria) {
                if (Nbt.l(Nbt.c(misiones, "ult"), m.id) >= dia) {
                    return Estado.HECHA;
                }
            } else if (!m.repetible) {
                return Estado.HECHA;
            }
        }
        if (!m.requiere.isEmpty() && Nbt.i(Nbt.c(misiones, "hechas"), m.requiere) == 0) {
            return Estado.BLOQUEADA;
        }
        if (m.nivel > 0 && !m.npc.isEmpty()) {
            NpcDef d = NpcDef.porId(m.npc);
            if (d == null || d.nivelDe(Nbt.i(amistad, m.npc)) < m.nivel) {
                return Estado.BLOQUEADA;
            }
        }
        return Estado.DISPONIBLE;
    }

    public static boolean completa(Mision m, int[] prog) {
        for (int i = 0; i < m.objetivos.size(); i++) {
            if (i >= prog.length || prog[i] < m.objetivos.get(i).cantidad) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ acciones

    /** Acepta una misión (si se puede). Devuelve true si quedó activa. */
    public static boolean aceptar(ServerPlayer p, String id) {
        Mision m = porId(id);
        if (m == null || estado(p, m) != Estado.DISPONIBLE) {
            return false;
        }
        CompoundTag a = activas(p);
        if (a.getAllKeys().size() >= MAX_ACTIVAS) {
            Plat.msg(p, Component.translatable("axelis_core.mision.llenas").withStyle(ChatFormatting.RED), true);
            return false;
        }
        CompoundTag e = new CompoundTag();
        e.putIntArray("p", new int[m.objetivos.size()]);
        e.putLong("d", dia(p));
        a.put(id, e);
        CompoundTag d = datos(p);
        if (Nbt.s(d, "sigue").isEmpty() || !a.contains(Nbt.s(d, "sigue"))) {
            d.putString("sigue", id);
        }
        revisar(p, m, false);
        guardar(p);
        Fx.toast(p, iconoDe(m), Component.translatable("axelis_core.mision.nueva"), m.titulo(), m.rareza.light & 0xFFFFFF);
        Combate.sonido(p.level(), p.position(), "minecraft:item.book.page_turn", 0.9F, 1.1F);
        AxelisCore.LOGGER.info("AXCORE_MISION aceptar {}", id);
        return true;
    }

    /** Entrega una misión lista: se quitan los objetos pedidos, se dan los premios y sube la amistad. */
    public static boolean entregar(ServerPlayer p, String id) {
        Mision m = porId(id);
        if (m == null || !activa(p, id)) {
            return false;
        }
        revisar(p, m, false);
        if (!completa(m, progreso(p, id))) {
            return false;
        }
        // primero comprobar que de verdad tiene todo lo que hay que entregar
        for (Objetivo o : m.objetivos) {
            if (o.tipo == Objetivo.Tipo.ENTREGAR && contar(p, o.que) < o.cantidad) {
                return false;
            }
        }
        for (Objetivo o : m.objetivos) {
            if (o.tipo == Objetivo.Tipo.ENTREGAR) {
                quitar(p, o.que, o.cantidad);
            }
        }
        activas(p).remove(id);
        CompoundTag d = datos(p);
        CompoundTag hechas = Nbt.c(d, "hechas");
        hechas.putInt(id, Nbt.i(hechas, id) + 1);
        Nbt.c(d, "ult").putLong(id, dia(p));
        if (id.equals(Nbt.s(d, "sigue"))) {
            d.putString("sigue", siguienteActiva(p));
        }
        for (Mision.Premio pr : m.premios) {
            pr.dar.accept(p);
        }
        guardar(p);
        if (!m.npc.isEmpty() && m.amistad != 0) {
            Amistad.sumar(p, m.npc, m.amistad);
        }
        Fx.toast(p, iconoDe(m), Component.translatable("axelis_core.mision.completada"), m.titulo(), 0xFFD166);
        Combate.sonido(p.level(), p.position(), "minecraft:entity.player.levelup", 0.7F, 1.2F);
        AxelisCore.LOGGER.info("AXCORE_MISION entregar {}", id);
        return true;
    }

    /** Deja una misión (se pierde el progreso). */
    public static boolean abandonar(ServerPlayer p, String id) {
        CompoundTag a = activas(p);
        if (!a.contains(id)) {
            return false;
        }
        a.remove(id);
        CompoundTag d = datos(p);
        if (id.equals(Nbt.s(d, "sigue"))) {
            d.putString("sigue", siguienteActiva(p));
        }
        guardar(p);
        return true;
    }

    /** Elige la misión que se ve en pantalla ("" para ninguna). */
    public static void seguir(ServerPlayer p, String id) {
        if (!id.isEmpty() && !activa(p, id)) {
            return;
        }
        datos(p).putString("sigue", id);
        guardar(p);
    }

    /** Borra todo (comando de pruebas). */
    public static void reiniciar(ServerPlayer p) {
        PlayerStore.of(p, AxelisCore.MODID).remove("misiones");
        guardar(p);
    }

    private static String siguienteActiva(ServerPlayer p) {
        for (String k : activas(p).getAllKeys()) {
            return k;
        }
        return "";
    }

    // ------------------------------------------------------------------ avances

    /** Algo que avisa un mod (pescar, abrir un cofre, ganar un juego...): suma a los objetivos {@code evento(clave, n)}. */
    public static void evento(ServerPlayer p, String clave, int n) {
        sumar(p, o -> o.tipo == Objetivo.Tipo.EVENTO && o.que.equals(clave), n);
    }

    /** (Core) Un jugador derrotó a una criatura. */
    public static void alDerrotar(ServerPlayer p, LivingEntity e) {
        String tipo = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
        contarDerrota(p, tipo, e.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER);
    }

    /** Cuenta una criatura derrotada por su id ("minecraft:zombie"); {@code hostil} para los objetivos de "cualquier monstruo". */
    public static void contarDerrota(ServerPlayer p, String tipo, boolean hostil) {
        sumar(p, o -> o.tipo == Objetivo.Tipo.CAZAR && o.cuenta(tipo, hostil), 1);
    }

    /** (Core) Un jugador habló con un NPC: cuenta para "hablar con" y entrega lo que esté listo con ese NPC. */
    public static void alHablar(ServerPlayer p, String npc) {
        sumar(p, o -> o.tipo == Objetivo.Tipo.HABLAR && o.que.equals(npc), 1);
    }

    private static void sumar(ServerPlayer p, java.util.function.Predicate<Objetivo> cuenta, int n) {
        CompoundTag a = activas(p);
        boolean cambio = false;
        for (String id : new ArrayList<>(a.getAllKeys())) {
            Mision m = porId(id);
            if (m == null) {
                continue;
            }
            CompoundTag e = Nbt.c(a, id);
            int[] prog = ajustar(Nbt.ia(e, "p"), m.objetivos.size());
            boolean antes = completa(m, prog);
            boolean esta = false;
            for (int i = 0; i < m.objetivos.size(); i++) {
                Objetivo o = m.objetivos.get(i);
                if (prog[i] < o.cantidad && cuenta.test(o)) {
                    prog[i] = Math.min(o.cantidad, prog[i] + n);
                    esta = true;
                    Plat.msg(p, o.descripcion().copy().append(" " + prog[i] + "/" + o.cantidad).withStyle(ChatFormatting.YELLOW), true);
                }
            }
            if (esta) {
                e.putIntArray("p", prog);
                cambio = true;
                alCambiar(p, m, antes, completa(m, prog));
            }
        }
        if (cambio) {
            guardar(p);
        }
    }

    private static int[] ajustar(int[] prog, int n) {
        if (prog.length == n) {
            return prog;
        }
        int[] r = new int[n];
        System.arraycopy(prog, 0, r, 0, Math.min(n, prog.length));
        return r;
    }

    /** Recalcula lo que depende del momento (tener objetos, llegar a un lugar). Devuelve true si cambió algo. */
    static boolean revisar(ServerPlayer p, Mision m, boolean avisar) {
        CompoundTag a = activas(p);
        if (!a.contains(m.id)) {
            return false;
        }
        CompoundTag e = Nbt.c(a, m.id);
        int[] prog = ajustar(Nbt.ia(e, "p"), m.objetivos.size());
        int[] antes = prog.clone();
        for (int i = 0; i < m.objetivos.size(); i++) {
            Objetivo o = m.objetivos.get(i);
            switch (o.tipo) {
                case TENER, ENTREGAR -> prog[i] = Math.min(o.cantidad, contar(p, o.que));
                case LLEGAR -> {
                    if (prog[i] < 1) {
                        double dx = p.getX() - o.x, dz = p.getZ() - o.z;
                        if (dx * dx + dz * dz <= (double) o.radio * o.radio) {
                            prog[i] = 1;
                        }
                    }
                }
                default -> {
                }
            }
        }
        if (java.util.Arrays.equals(antes, prog)) {
            return false;
        }
        e.putIntArray("p", prog);
        if (avisar) {
            alCambiar(p, m, completa(m, antes), completa(m, prog));
        }
        return true;
    }

    private static void alCambiar(ServerPlayer p, Mision m, boolean antes, boolean ahora) {
        if (antes || !ahora) {
            return;
        }
        if (m.npc.isEmpty()) {
            // sin NPC: se entrega sola
            AxelisCore.LOGGER.info("AXCORE_MISION lista {}", m.id);
            entregarLuego.add(new Object[] {p, m.id});
            return;
        }
        NpcDef d = NpcDef.porId(m.npc);
        Fx.toast(p, iconoDe(m), Component.translatable("axelis_core.mision.lista"),
                d == null ? m.titulo() : Component.translatable("axelis_core.mision.vuelve", d.nombre()), 0x7CFC9A);
        Combate.sonido(p.level(), p.position(), "minecraft:entity.experience_orb.pickup", 0.8F, 1.4F);
        AxelisCore.LOGGER.info("AXCORE_MISION lista {}", m.id);
    }

    /** Misiones sin NPC que se completaron (se entregan al final del tick, fuera del recorrido de las activas). */
    private static final List<Object[]> entregarLuego = new ArrayList<>();

    private static void tick(MinecraftServer s) {
        if (!entregarLuego.isEmpty()) {
            List<Object[]> l = new ArrayList<>(entregarLuego);
            entregarLuego.clear();
            for (Object[] o : l) {
                entregar((ServerPlayer) o[0], (String) o[1]);
            }
        }
        if (++reloj % 20 != 0) {
            return;
        }
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            CompoundTag d = PlayerStore.of(p, AxelisCore.MODID);
            if (!d.contains("misiones")) {
                continue;
            }
            CompoundTag a = activas(p);
            if (a.isEmpty()) {
                continue;
            }
            boolean cambio = false;
            for (String id : new ArrayList<>(a.getAllKeys())) {
                Mision m = porId(id);
                if (m != null && revisar(p, m, true)) {
                    cambio = true;
                }
            }
            if (cambio) {
                guardar(p);
            }
        }
    }

    // ------------------------------------------------------------------ objetos

    /** Cuántos objetos de ese id tiene (inventario y mano izquierda). */
    public static int contar(ServerPlayer p, String item) {
        net.minecraft.world.item.Item it = Plat.item(item);
        int n = 0;
        for (ItemStack st : Plat.mainAndOffhand(p.getInventory())) {
            if (!st.isEmpty() && st.getItem() == it) {
                n += st.getCount();
            }
        }
        return n;
    }

    /** Quita objetos del inventario (primero de la mano izquierda y de atrás hacia adelante). Devuelve cuántos quitó. */
    public static int quitar(ServerPlayer p, String item, int n) {
        net.minecraft.world.item.Item it = Plat.item(item);
        List<ItemStack> l = Plat.mainAndOffhand(p.getInventory());
        int falta = n;
        for (int i = l.size() - 1; i >= 0 && falta > 0; i--) {
            ItemStack st = l.get(i);
            if (!st.isEmpty() && st.getItem() == it) {
                int q = Math.min(falta, st.getCount());
                st.shrink(q);
                falta -= q;
            }
        }
        p.getInventory().setChanged();
        return n - falta;
    }

    static ItemStack iconoDe(Mision m) {
        if (!m.premios.isEmpty()) {
            return m.premios.get(0).icono();
        }
        return new ItemStack(Plat.item("minecraft:writable_book"));
    }
}
