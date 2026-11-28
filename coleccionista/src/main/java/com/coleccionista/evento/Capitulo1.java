package com.coleccionista.evento;

import com.axeliscore.AxelisCore;
import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import com.coleccionista.bot.PiezaBot;
import com.coleccionista.mapa.Cabinas;
import com.coleccionista.mapa.SalaGrande;
import com.coleccionista.prueba.Sentidos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Capítulo 1, en la sala grande (después del portón del patio). Cuando ya están todos dentro (o pasa un rato), el portón baja detrás de
 * ellos, se prende la pantalla y el Coleccionista pide equipos (el organizador lo puede adelantar con
 * {@code /evento capitulo1 equipos}). Se abren los diez ascensores; cada equipo de 2 o de 3 se mete en uno (el foco de arriba se prende cuando está
 * bien). Cuando todos están en un ascensor bien formado, cuenta 5 segundos y bajan; si se acaba el tiempo (o el organizador lo pide), los
 * que se quedaron afuera ya no quedan eliminados: se juntan entre ellos (o completan una pareja) y el Coleccionista los mete a un ascensor
 * ({@link #armarEquipos}). El viaje es una caída violenta; cada ascensor llega al ascensor falso de su
 * circuito, donde empieza {@link Sentidos}.
 */
public final class Capitulo1 {
    private static final String MOD = Coleccionista.MODID;
    public static final String PRUEBA = "La sala grande";
    public static int formarSeg = 150, viajeTicks = 240, cuentaTicks = 100;
    /** Segundos que espera el Coleccionista a que entren todos a la sala antes de hablar de todos modos. */
    public static int salaEspera = 90;

    public enum Fase {
        NADA, SALA, HOTEL, HABLANDO, FORMAR, CUENTA, BAJANDO
    }

    private static Fase fase = Fase.NADA;
    private static long cuentaFin = -1;
    /** Cuándo subió el portón, cuándo se cierra (todos dentro) y cuándo empieza a hablar (-1: todavía no). */
    private static long salaDesde = -1, cierraEn = -1, hablaEn = -1;
    private static int viajes, llegados;
    /** Quién está en qué ascensor (lo último que se revisó). */
    private static final Map<Integer, List<Entity>> dentro = new HashMap<>();

    private Capitulo1() {
    }

    public static void init() {
        AxelisCore.onServerTick(Capitulo1::tick);
    }

    public static Fase fase() {
        return fase;
    }

    /** El portón del patio subió: todos a la sala grande; cuando estén todos dentro (o pase un rato), habla el Coleccionista. */
    public static void alAbrirPorton(MinecraftServer s) {
        if (fase == Fase.NADA) {
            fase = Fase.SALA;
            salaDesde = s.overworld().getGameTime();
            cierraEn = -1;
            hablaEn = -1;
            Evento.get(s).prueba(s, Component.translatable(MOD + ".cap1.prueba_sala").getString());
            Pantallas.modo(s, "apagada", "");
            Coleccionista.LOGGER.info("COLECCIONISTA_CAP1 sala");
        }
    }

    /** ¿Ya está dentro de la sala grande (pasó el portón)? */
    public static boolean enSala(MinecraftServer s, Entity e) {
        BlockPos o = Complejo.origen(s, "sala");
        if (o == null) {
            return false;
        }
        double x = e.getX() - o.getX(), z = e.getZ() - o.getZ(), y = e.getY() - o.getY();
        return x >= 2 && x <= 57 && z >= -0.2 && z <= 35.2 && y >= -1 && y <= 13;
    }

    /** Empieza el hotel (antes de los equipos): el capítulo se queda esperando a que regresen. */
    public static void alEmpezarHotel() {
        fase = Fase.HOTEL;
        cierraEn = -1;
        hablaEn = -1;
    }

    /** El organizador pide equipos (o ya están todos en la sala): baja el portón y "formen equipos". */
    public static Component equipos(MinecraftServer s) {
        return equipos(s, "equipos");
    }

    /** Lo mismo, con el guion que toque ("hotel_regreso" si vienen del hotel). */
    public static Component equipos(MinecraftServer s, String guion) {
        if (!Complejo.construido(s, "sala")) {
            return Component.translatable(MOD + ".cap1.sin_mapa");
        }
        if (fase == Fase.HABLANDO || fase == Fase.FORMAR || fase == Fase.CUENTA || fase == Fase.BAJANDO) {
            return Component.translatable(MOD + ".cap1.ya");
        }
        fase = Fase.HABLANDO;
        cierraEn = -1;
        hablaEn = -1;
        Evento.get(s).prueba(s, PRUEBA);
        // los bots que siguieran en el patio entran (solo hay bots en las pruebas)
        botsALaSala(s);
        if (Celdas.portonAbierto(s)) {
            Celdas.porton(s, false);
        }
        Pantallas.soloGrande(s, true);
        Pantallas.hablar(s, guion, () -> formar(s));
        Coleccionista.LOGGER.info("COLECCIONISTA_CAP1 equipos");
        return null;
    }

    /** Los bots pasan del patio a la sala (en el juego real no hay bots; en las pruebas automáticas no caminan solos). */
    public static void botsALaSala(MinecraftServer s) {
        BlockPos c = Complejo.pos(s, "sala.entrada");
        if (c == null) {
            return;
        }
        Evento ev = Evento.get(s);
        int n = 0;
        for (Pieza p : ev.vivos()) {
            if (p.bot && Evento.entidad(s, p) instanceof PiezaBot b && !enSala(s, b)) {
                b.getNavigation().stop();
                b.moveTo(c.getX() + 3.5 + (n % 4), c.getY(), c.getZ() - 1.5 + (n / 4) % 4, 270F, 0F);
                n++;
            }
        }
    }

    /** En la sala: cuando están todos dentro (o pasó el rato de espera), el portón baja detrás de ellos y luego habla. */
    private static void tickSala(MinecraftServer s, long now) {
        if (now % 10 != 0) {
            return;
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        // los bots entran solos un momento después de que sube el portón
        if (now - salaDesde >= 80 && (now - salaDesde) % 40 == 0) {
            botsALaSala(s);
        }
        int vivos = 0, dentro = 0;
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null) {
                continue;
            }
            vivos++;
            if (enSala(s, e)) {
                dentro++;
            }
        }
        boolean todos = vivos > 0 && dentro >= vivos;
        if (cierraEn < 0 && (todos || now - salaDesde >= salaEspera * 20L)) {
            // una pausa para que miren la sala antes de que baje el portón
            cierraEn = now + 60;
            Coleccionista.LOGGER.info("COLECCIONISTA_CAP1 todos_en_sala={} dentro={} vivos={}", todos, dentro, vivos);
        }
        if (cierraEn > 0 && now >= cierraEn) {
            cierraEn = -1;
            // primero el hotel (cada quien solo); al regresar, los equipos
            if (com.coleccionista.hotel.JuegoHotel.toca(s)) {
                com.coleccionista.hotel.JuegoHotel.empezar(s);
            } else {
                equipos(s);
            }
        }
    }

    /** Se abren los ascensores y corre el reloj para formar equipos. */
    public static void formar(MinecraftServer s) {
        if (fase != Fase.HABLANDO && fase != Fase.SALA && fase != Fase.NADA) {
            return;
        }
        Pantallas.callar(s);
        fase = Fase.FORMAR;
        for (int n = 1; n <= SalaGrande.ASCENSORES; n++) {
            Cabina.puerta(s, "sala_" + n, true);
        }
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.7F);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".cap1.pantalla").getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".cap1.chat.formen", formarSeg)));
        Evento.get(s).reloj(s, formarSeg);
        Coleccionista.LOGGER.info("COLECCIONISTA_CAP1 formar");
    }

    private static boolean pieza(Evento ev, Entity e) {
        Pieza p = ev.pieza(e);
        return p != null && p.vivo();
    }

    private static void tick(MinecraftServer s) {
        long now = s.overworld().getGameTime();
        if (fase == Fase.SALA) {
            tickSala(s, now);
            return;
        }
        if (fase != Fase.FORMAR && fase != Fase.CUENTA) {
            return;
        }
        if (fase == Fase.CUENTA && now >= cuentaFin) {
            bajar(s, false);
            return;
        }
        if (now % 10 != 0) {
            return;
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        ServerLevel l = s.overworld();
        dentro.clear();
        int adentro = 0;
        boolean bien = true;
        for (int n = 1; n <= SalaGrande.ASCENSORES; n++) {
            AABB c = Cabina.caja(s, "sala_" + n, Cabinas.EQ_ANCHO, Cabinas.ALTO, Cabinas.EQ_FONDO);
            List<Entity> l2 = c == null ? new ArrayList<>()
                    : new ArrayList<>(l.getEntitiesOfClass(Entity.class, c, e -> (e instanceof ServerPlayer || e instanceof PiezaBot) && pieza(ev, e)));
            dentro.put(n, l2);
            adentro += l2.size();
            boolean ok = l2.size() >= 2 && l2.size() <= 3;
            if (!l2.isEmpty() && !ok) {
                bien = false;
            }
            foco(s, n, ok);
            // a quien está adentro: cómo va su ascensor
            for (Entity e : l2) {
                if (e instanceof ServerPlayer sp) {
                    String k = l2.size() == 1 ? ".cap1.falta" : l2.size() > 3 ? ".cap1.sobra" : ".cap1.listo";
                    Plat.msg(sp, Component.translatable(MOD + k).withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.GOLD), true);
                }
            }
        }
        int vivos = 0;
        for (Pieza p : ev.vivos()) {
            if (Evento.entidad(s, p) != null) {
                vivos++;
            }
        }
        // los bots se suben solos (uno cada medio segundo) a un ascensor donde quepan
        if (now % 10 == 0) {
            botsSuben(s, ev);
        }
        boolean todos = bien && adentro >= vivos && vivos > 0;
        if (todos && fase == Fase.FORMAR) {
            fase = Fase.CUENTA;
            cuentaFin = now + cuentaTicks;
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".cap1.chat.listos")));
            Anuncio.sonido(s, SoundEvents.NOTE_BLOCK_BELL.get(), 1F, 0.8F);
            Coleccionista.LOGGER.info("COLECCIONISTA_CAP1 cuenta");
        } else if (!todos && fase == Fase.CUENTA) {
            fase = Fase.FORMAR;
            cuentaFin = -1;
            Anuncio.sonido(s, SoundEvents.NOTE_BLOCK_BASS.get(), 1F, 0.5F);
        }
        if (fase == Fase.CUENTA) {
            int seg = (int) Math.max(0, (cuentaFin - now + 19) / 20);
            for (List<Entity> l2 : dentro.values()) {
                for (Entity e : l2) {
                    if (e instanceof ServerPlayer sp) {
                        Plat.msg(sp, Component.translatable(MOD + ".cap1.bajan", seg).withStyle(ChatFormatting.RED), true);
                        if (now % 20 == 0) {
                            sp.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.get(), SoundSource.MASTER, 1F, 0.7F);
                        }
                    }
                }
            }
        }
    }

    private static void foco(MinecraftServer s, int n, boolean on) {
        BlockPos p = Complejo.pos(s, "sala.asc." + n + ".luz");
        if (p == null) {
            return;
        }
        BlockState st = s.overworld().getBlockState(p);
        if (st.hasProperty(com.coleccionista.bloque.deco.DecoBlock.ENCENDIDA) && st.getValue(com.coleccionista.bloque.deco.DecoBlock.ENCENDIDA) != on) {
            s.overworld().setBlock(p, st.setValue(com.coleccionista.bloque.deco.DecoBlock.ENCENDIDA, on), 3);
        }
    }

    /**
     * Un bot que no está en ningún ascensor se mete en uno: primero completa una pareja (un ascensor con uno), si no empieza otro; si es
     * el último que falta y no hay pareja que completar, hace un trío (para no quedarse solo).
     */
    private static void botsSuben(MinecraftServer s, Evento ev) {
        List<PiezaBot> fuera = new ArrayList<>();
        for (Pieza p : ev.vivos()) {
            if (p.bot && Evento.entidad(s, p) instanceof PiezaBot b) {
                boolean ya = false;
                for (List<Entity> l2 : dentro.values()) {
                    ya |= l2.contains(b);
                }
                if (!ya) {
                    fuera.add(b);
                }
            }
        }
        if (fuera.isEmpty()) {
            return;
        }
        PiezaBot b = fuera.get(0);
        int[] orden = fuera.size() == 1 ? new int[] {1, 2, 0} : new int[] {1, 0};
        int mejor = -1;
        for (int quiero : orden) {
            for (int n = 1; n <= SalaGrande.ASCENSORES && mejor < 0; n++) {
                if (dentro.getOrDefault(n, List.of()).size() == quiero) {
                    mejor = n;
                }
            }
            if (mejor > 0) {
                break;
            }
        }
        AABB c = mejor < 0 ? null : Cabina.caja(s, "sala_" + mejor, Cabinas.EQ_ANCHO, Cabinas.ALTO, Cabinas.EQ_FONDO);
        if (c == null) {
            return;
        }
        int i = dentro.get(mejor).size();
        double x = c.minX + 0.6 + (c.maxX - c.minX - 1.2) * (i % 3) / 2.0, z = c.minZ + 0.6 + (c.maxZ - c.minZ - 1.2) * ((i + 1) % 2);
        b.getNavigation().stop();
        b.moveTo(x, c.minY + 0.05, z, b.getYRot(), 0);
        dentro.get(mejor).add(b);
    }

    /** Se acabó el reloj de formar: bajan los que están dentro. */
    static boolean alAcabarReloj(MinecraftServer s) {
        if (fase != Fase.FORMAR && fase != Fase.CUENTA) {
            return false;
        }
        bajar(s, true);
        return true;
    }

    /**
     * Bajan los ascensores. Los equipos de 1 se juntan con otro (o entre sí) y los de 4 o más se parten (cada quien viaja desde su
     * ascensor al circuito de su equipo); con {@code forzado}, quien se quedó afuera de los ascensores queda eliminado.
     */
    public static boolean bajar(MinecraftServer s, boolean forzado) {
        if (fase != Fase.FORMAR && fase != Fase.CUENTA) {
            return false;
        }
        Evento ev = Evento.get(s);
        // quién está en cuál (revisado ahora)
        ServerLevel l = s.overworld();
        List<int[]> origen = new ArrayList<>();
        List<List<Entity>> grupos = new ArrayList<>();
        java.util.Set<Entity> subidos = new java.util.HashSet<>();
        for (int n = 1; n <= SalaGrande.ASCENSORES; n++) {
            AABB c = Cabina.caja(s, "sala_" + n, Cabinas.EQ_ANCHO, Cabinas.ALTO, Cabinas.EQ_FONDO);
            if (c == null) {
                continue;
            }
            List<Entity> l2 = new ArrayList<>(l.getEntitiesOfClass(Entity.class, c, e -> (e instanceof ServerPlayer || e instanceof PiezaBot) && pieza(ev, e)));
            if (!l2.isEmpty()) {
                grupos.add(l2);
                origen.add(new int[] {n});
                subidos.addAll(l2);
            }
        }
        // los que se quedaron afuera: ya nadie queda eliminado por no tener equipo (el Coleccionista se los arma)
        List<Entity> fuera = new ArrayList<>();
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e != null && !subidos.contains(e)) {
                fuera.add(e);
            }
        }
        if (grupos.isEmpty() && fuera.isEmpty()) {
            Anuncio.organizadores(s, Component.translatable(MOD + ".cap1.nadie"));
            return false;
        }
        // equipos finales: cada miembro con el ascensor del que sale
        Map<Entity, Integer> sale = new HashMap<>();
        for (int i = 0; i < grupos.size(); i++) {
            for (Entity e : grupos.get(i)) {
                sale.put(e, origen.get(i)[0]);
            }
        }
        List<List<Entity>> equipos = armarEquipos(grupos, fuera, com.coleccionista.mapa.Circuito.CIRCUITOS);
        meterSueltos(s, equipos, sale, fuera);
        // a cada equipo, su circuito
        List<List<Pieza>> piezas = new ArrayList<>();
        for (List<Entity> eq : equipos) {
            List<Pieza> lp = new ArrayList<>();
            for (Entity e : eq) {
                Pieza p = ev.pieza(e);
                if (p != null) {
                    lp.add(p);
                }
            }
            piezas.add(lp);
        }
        Component no = Sentidos.preparar(s, piezas);
        if (no != null) {
            Anuncio.organizadores(s, no);
            return false;
        }
        fase = Fase.BAJANDO;
        ev.reloj(s, 0);
        Pantallas.modo(s, "estatica", "");
        viajes = 0;
        llegados = 0;
        // cada miembro viaja desde su ascensor al falso de su circuito (agrupados por par ascensor-circuito)
        Map<String, List<Entity>> pares = new HashMap<>();
        for (int k = 0; k < equipos.size() && k < com.coleccionista.mapa.Circuito.CIRCUITOS; k++) {
            for (Entity e : equipos.get(k)) {
                pares.computeIfAbsent(sale.get(e) + ">" + (k + 1), x -> new ArrayList<>()).add(e);
            }
        }
        for (Map.Entry<String, List<Entity>> par : pares.entrySet()) {
            String[] ab = par.getKey().split(">");
            boolean ok = Cabina.viajar(s, "sala_" + ab[0], "circ_" + ab[1], Cabina.Tipo.CAIDA, viajeTicks, Cabinas.EQ_ANCHO, Cabinas.ALTO,
                    Cabinas.EQ_FONDO, par.getValue(), () -> llego(s));
            if (ok) {
                viajes++;
            }
        }
        for (int n = 1; n <= SalaGrande.ASCENSORES; n++) {
            Cabina.puerta(s, "sala_" + n, false);
            foco(s, n, false);
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_CAP1 bajan equipos={} viajes={} forzado={}", equipos.size(), viajes, forzado);
        if (viajes == 0) {
            fase = Fase.NADA;
        }
        return true;
    }

    /**
     * Los equipos finales (de 2 o 3, como mucho {@code max}) a partir de lo que formaron en los ascensores y de los que se quedaron
     * sueltos: los grupos de 4 o más se parten; los de 1 y los de afuera primero completan parejas (quedan tríos), luego se juntan entre
     * ellos en tríos (o dos parejas); si sobra uno solo, se lleva a uno de un trío para hacer pareja. Si salen más equipos que circuitos,
     * el más chico se reparte entre los que tienen lugar. Nadie se queda fuera.
     */
    public static <T> List<List<T>> armarEquipos(List<List<T>> grupos, List<T> fuera, int max) {
        List<List<T>> equipos = new ArrayList<>();
        List<T> sueltos = new ArrayList<>();
        for (List<T> g0 : grupos) {
            List<T> g = new ArrayList<>(g0);
            while (g.size() > 3) {
                int corte = g.size() == 4 ? 2 : 3;
                equipos.add(new ArrayList<>(g.subList(0, corte)));
                g = new ArrayList<>(g.subList(corte, g.size()));
            }
            if (g.size() == 1) {
                sueltos.add(g.get(0));
            } else if (!g.isEmpty()) {
                equipos.add(g);
            }
        }
        sueltos.addAll(fuera);
        // primero completan las parejas
        for (List<T> eq : equipos) {
            if (sueltos.isEmpty()) {
                break;
            }
            if (eq.size() == 2) {
                eq.add(sueltos.remove(0));
            }
        }
        // los demás, en tríos (con 2 o 4, en parejas)
        while (!sueltos.isEmpty()) {
            int n = sueltos.size();
            if (n == 1) {
                List<T> trio = null;
                for (List<T> eq : equipos) {
                    if (eq.size() == 3) {
                        trio = eq;
                        break;
                    }
                }
                List<T> par = new ArrayList<>();
                if (trio != null) {
                    par.add(trio.remove(2));
                } else if (!equipos.isEmpty()) {
                    equipos.get(0).add(sueltos.remove(0));
                    continue;
                }
                par.add(sueltos.remove(0));
                equipos.add(par);
                continue;
            }
            int t = n == 2 || n == 4 ? 2 : 3;
            equipos.add(new ArrayList<>(sueltos.subList(0, t)));
            sueltos.subList(0, t).clear();
        }
        // más equipos que circuitos: el más chico se reparte entre los que tienen lugar
        while (equipos.size() > max && max > 0) {
            equipos.sort(java.util.Comparator.comparingInt(List::size));
            List<T> chico = equipos.remove(0);
            for (T e : chico) {
                List<T> d = equipos.get(0);
                for (List<T> eq : equipos) {
                    if (eq.size() < 3) {
                        d = eq;
                        break;
                    }
                }
                d.add(e);
            }
        }
        return equipos;
    }

    /**
     * Los que se quedaron afuera entran al ascensor de su equipo nuevo (si es un equipo nuevo, a uno vacío): el Coleccionista los acomoda.
     * Cada uno queda en {@code sale} con el ascensor del que va a bajar.
     */
    private static void meterSueltos(MinecraftServer s, List<List<Entity>> equipos, Map<Entity, Integer> sale, List<Entity> fuera) {
        if (fuera.isEmpty()) {
            return;
        }
        java.util.Set<Integer> usados = new java.util.HashSet<>(sale.values());
        ServerLevel l = s.overworld();
        for (List<Entity> eq : equipos) {
            Integer n = null;
            for (Entity e : eq) {
                if (sale.containsKey(e)) {
                    n = sale.get(e);
                    break;
                }
            }
            if (n == null) {
                for (int k = 1; k <= SalaGrande.ASCENSORES && n == null; k++) {
                    if (!usados.contains(k)) {
                        n = k;
                    }
                }
                if (n == null) {
                    n = 1;
                }
                usados.add(n);
            }
            AABB c = Cabina.caja(s, "sala_" + n, Cabinas.EQ_ANCHO, Cabinas.ALTO, Cabinas.EQ_FONDO);
            int i = 0;
            for (Entity e : eq) {
                if (sale.containsKey(e) || c == null) {
                    continue;
                }
                sale.put(e, n);
                double x = c.minX + 0.6 + (c.maxX - c.minX - 1.2) * ((i * 2 + 1) % 4) / 3.0;
                double z = c.minZ + 0.6 + (c.maxZ - c.minZ - 1.2) * (i % 2);
                i++;
                if (e instanceof ServerPlayer sp) {
                    sp.teleportTo(l, x, c.minY + 0.05, z, sp.getYRot(), 0F);
                    Plat.msg(sp, Component.translatable(MOD + ".cap1.te_acomodo").withStyle(ChatFormatting.GOLD), true);
                    sp.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.MASTER, 0.6F, 0.6F);
                } else {
                    if (e instanceof PiezaBot b) {
                        b.getNavigation().stop();
                    }
                    e.moveTo(x, c.minY + 0.05, z, e.getYRot(), 0F);
                }
            }
        }
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".cap1.chat.sueltos", fuera.size())));
        Coleccionista.LOGGER.info("COLECCIONISTA_CAP1 sueltos={} acomodados", fuera.size());
    }

    private static void llego(MinecraftServer s) {
        llegados++;
        if (llegados >= viajes && fase == Fase.BAJANDO) {
            fase = Fase.NADA;
            Coleccionista.LOGGER.info("COLECCIONISTA_CAP1 llegaron");
            Sentidos.alLlegar(s, true);
        }
    }

    /** Para el indicador de "ir acá": mientras se forman los equipos, a quien no está en un ascensor, el más cercano donde cabe. */
    public static Objetivo.Meta objetivo(MinecraftServer s, ServerPlayer sp) {
        if (fase != Fase.FORMAR && fase != Fase.CUENTA) {
            return null;
        }
        Objetivo.Meta mejor = null;
        double dm = Double.MAX_VALUE;
        for (int n = 1; n <= SalaGrande.ASCENSORES; n++) {
            AABB c = Cabina.caja(s, "sala_" + n, Cabinas.EQ_ANCHO, Cabinas.ALTO, Cabinas.EQ_FONDO);
            if (c == null) {
                continue;
            }
            if (c.contains(sp.position())) {
                return null;
            }
            if (dentro.getOrDefault(n, List.of()).size() >= 3) {
                continue;
            }
            net.minecraft.world.phys.Vec3 m = c.getCenter();
            double d = m.distanceToSqr(sp.position());
            if (d < dm) {
                dm = d;
                mejor = new Objetivo.Meta(m.x, c.minY + 1.4, m.z, MOD + ".obj.ascensor");
            }
        }
        return mejor;
    }

    public static void reiniciar(MinecraftServer s) {
        fase = Fase.NADA;
        cuentaFin = -1;
        salaDesde = -1;
        cierraEn = -1;
        hablaEn = -1;
        viajes = 0;
        llegados = 0;
        dentro.clear();
    }

    /** Para el panel: en qué va. */
    public static String resumen() {
        return fase.name();
    }

    /** Para probar: lleva a todas las piezas vivas a la sala grande (como si hubieran cruzado el portón). */
    public static int aLaSala(MinecraftServer s) {
        BlockPos c = Complejo.pos(s, "sala.centro");
        if (c == null) {
            return 0;
        }
        Evento ev = Evento.get(s);
        int n = 0;
        for (Pieza p : ev.vivos()) {
            Entity e = Evento.entidad(s, p);
            if (e == null) {
                continue;
            }
            double x = c.getX() + 0.5 - (n % 6) * 1.5, z = c.getZ() + 0.5 + (n / 6 - 1) * 1.5;
            if (e instanceof ServerPlayer sp) {
                sp.teleportTo(s.overworld(), x, c.getY(), z, 90F, 0F);
            } else {
                e.moveTo(x, c.getY(), z, 90F, 0F);
            }
            n++;
        }
        if (fase == Fase.NADA) {
            fase = Fase.SALA;
            salaDesde = s.overworld().getGameTime();
        }
        return n;
    }
}
