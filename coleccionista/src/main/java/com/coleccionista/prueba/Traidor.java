package com.coleccionista.prueba;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Plat;
import com.axeliscore.ui.CoreScreens;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Anuncio;
import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Evento;
import com.coleccionista.evento.Pantallas;
import com.coleccionista.evento.Pieza;
import com.coleccionista.mapa.Archivo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Prueba 2: El traidor. Equipos al azar, cada uno en una sala del Archivo con una bóveda cerrada con un código (tantos números como
 * piezas tiene el equipo). Cada pieza recibe una nota con dos números del código; cada número aparece en dos notas. Pero en cada equipo
 * hay un traidor en secreto: los dos números de su nota son falsos. Con tres intentos en el teclado:
 * <ul>
 *     <li>Si abren la bóveda, el traidor queda al descubierto y se va a la colección.</li>
 *     <li>Si no la abren (tres intentos o se acaba el tiempo), el traidor se salva y elige a quién de su equipo se lleva el Coleccionista.</li>
 * </ul>
 * Al final se revela quiénes eran los traidores. Cada equipo pierde exactamente una pieza.
 */
public final class Traidor {
    private static final String MOD = Coleccionista.MODID;
    public static final String KEY = MOD + ":traidor";
    public static final String KEY_MIA = MOD + ":traidor_mia";
    public static final String ACCION_CODIGO = MOD + ":boveda";
    public static final String ACCION_VICTIMA = MOD + ":victima";
    public static final String PANTALLA_TECLADO = MOD + ":teclado";
    public static final String PANTALLA_VICTIMA = MOD + ":victima";
    public static final String PRUEBA = "El traidor";
    public static int segundosBuscar = 240, segundosCastigo = 20, tamano = 4, intentosMax = 3;

    public enum Fase {
        NADA, LISTA, HABLANDO, ROLES, BUSCAR, FIN
    }

    enum Estado {
        BUSCANDO, ABIERTA, CASTIGO, RESUELTO
    }

    static final class Sala {
        final int k;
        final List<UUID> ids = new ArrayList<>();
        UUID traidor, victima;
        int[] codigo;
        int intentos;
        Estado estado = Estado.BUSCANDO;
        long castigoHasta;

        Sala(int k) {
            this.k = k;
        }
    }

    private static Fase fase = Fase.NADA;
    private static final List<Sala> salas = new ArrayList<>();
    private static final Map<UUID, Long> botEn = new HashMap<>();
    private static long t, tpEn = -1, presentarEn = -1, rolesHasta;
    private static int eliminados;
    private static boolean cambio;

    private Traidor() {
    }

    public static void init() {
        AxelisCore.onServerTick(Traidor::tick);
        AxelisCore.onLogin(p -> {
            if (fase != Fase.NADA) {
                PlayerStore.Sync.toPlayer(p, KEY, estado(Plat.server(p)));
                mia(Plat.server(p), p);
            }
        });
        Actions.on(ACCION_CODIGO, Traidor::codigo);
        Actions.on(ACCION_VICTIMA, Traidor::victima);
    }

    public static Fase fase() {
        return fase;
    }

    // ------------------------------------------------------------------ preparar

    /** ¿La prueba está abierta (preparada, en juego o terminada sin limpiar)? */
    public static boolean abierta() {
        return fase != Fase.NADA;
    }

    public static Component preparar(MinecraftServer s) {
        if (!Complejo.construido(s, "archivo")) {
            return Component.translatable(MOD + ".tra.sin_mapa");
        }
        if (fase != Fase.NADA && fase != Fase.LISTA && fase != Fase.FIN) {
            return Component.translatable(MOD + ".tra.ya_empezo");
        }
        Evento ev = Evento.get(s);
        List<Pieza> v = ev.vivos();
        if (v.size() < 3) {
            return Component.translatable(MOD + ".tra.pocos");
        }
        Pruebas.cerrarOtras(s, "traidor");
        limpiar(s);
        Random r = new Random(s.overworld().getRandom().nextLong());
        int n = Math.max(1, Math.min(Archivo.SALAS, v.size() / Math.max(3, tamano)));
        List<List<Pieza>> dados = Pruebas.dados(ev);
        if (dados != null) {
            // los equipos que ellos mismos formaron en los cubículos
            n = Math.min(Archivo.SALAS, dados.size());
            for (int i = 0; i < n; i++) {
                Sala sa = new Sala(i + 1);
                salas.add(sa);
                for (Pieza p : dados.get(i)) {
                    if (sa.ids.size() < Archivo.LUGARES) {
                        sa.ids.add(p.id);
                        p.equipo = sa.k - 1;
                    }
                }
            }
        } else {
            Collections.shuffle(v, r);
            for (int i = 0; i < n; i++) {
                salas.add(new Sala(i + 1));
            }
            for (int i = 0; i < v.size(); i++) {
                Sala sa = salas.get(i % n);
                if (sa.ids.size() < Archivo.LUGARES) {
                    sa.ids.add(v.get(i).id);
                    v.get(i).equipo = sa.k - 1;
                }
            }
        }
        for (Sala sa : salas) {
            int L = Math.max(3, Math.min(6, sa.ids.size()));
            sa.codigo = new int[L];
            for (int i = 0; i < L; i++) {
                sa.codigo[i] = r.nextInt(10);
            }
            sa.traidor = sa.ids.size() >= 2 ? sa.ids.get(r.nextInt(sa.ids.size())) : null;
            sa.intentos = intentosMax;
        }
        ev.cambio(s);
        ev.prueba(s, PRUEBA);
        ev.reloj(s, 0);
        for (int k = 1; k <= Archivo.SALAS; k++) {
            reja(s, "arc.sala." + k + ".reja", false);
            reja(s, "arc.sala." + k + ".boveda", false);
        }
        Pantallas.modo(s, "estatica", "");
        fase = Fase.LISTA;
        long now = s.overworld().getGameTime();
        Escena.fundido(s, MOD + ".tra.fundido", 80);
        tpEn = now + 22;
        presentarEn = now + 90;
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_TRAIDOR preparada salas={}", n);
        return null;
    }

    /** La nota de una pieza: dos números (de las posiciones j y j+1); si es el traidor, los dos son falsos. */
    private static ItemStack nota(Sala sa, int j, Pieza p, Random r) {
        int L = sa.codigo.length;
        int a = j % L, b = (j + 1) % L;
        int va = sa.codigo[a], vb = sa.codigo[b];
        if (p.id.equals(sa.traidor)) {
            va = (va + 1 + r.nextInt(9)) % 10;
            vb = (vb + 1 + r.nextInt(9)) % 10;
        }
        ItemStack st = new ItemStack(Items.PAPER);
        st.setHoverName(Component.translatable(MOD + ".tra.nota", p.etiqueta()).withStyle(ChatFormatting.GOLD));
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(MOD + ".tra.nota_linea", a + 1, va)
                .withStyle(ChatFormatting.WHITE))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(MOD + ".tra.nota_linea", b + 1, vb)
                .withStyle(ChatFormatting.WHITE))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(MOD + ".tra.nota_digitos", L)
                .withStyle(ChatFormatting.DARK_GRAY))));
        st.getOrCreateTagElement("display").put("Lore", lore);
        st.getOrCreateTag().putBoolean("coleccionista_nota", true);
        return st;
    }

    private static void aLasSalas(MinecraftServer s) {
        Evento ev = Evento.get(s);
        Random r = new Random(s.overworld().getRandom().nextLong());
        for (Sala sa : salas) {
            for (int i = 0; i < sa.ids.size(); i++) {
                Pieza p = ev.pieza(sa.ids.get(i));
                mover(s, p, "arc.sala." + sa.k + ".lugar." + i);
                Entity e = p == null ? null : Evento.entidad(s, p);
                if (e instanceof ServerPlayer sp) {
                    quitarNotas(sp);
                    sp.getInventory().add(nota(sa, i, p, r));
                }
            }
        }
    }

    private static void quitarNotas(ServerPlayer sp) {
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.hasTag() && st.getTag().getBoolean("coleccionista_nota")) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private static void mover(MinecraftServer s, Pieza p, String marca) {
        BlockPos b = Complejo.pos(s, marca);
        Entity e = p == null ? null : Evento.entidad(s, p);
        if (b == null || e == null) {
            return;
        }
        float yaw = Complejo.yaw(marca);
        e.fallDistance = 0;
        if (e instanceof ServerPlayer sp) {
            if (sp.isSleeping()) {
                sp.stopSleeping();
            }
            sp.teleportTo(s.overworld(), b.getX() + 0.5, b.getY(), b.getZ() + 0.5, yaw, 0F);
            sp.setGameMode(GameType.ADVENTURE);
        } else {
            e.moveTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, yaw, 0F);
            e.setYHeadRot(yaw);
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void reja(MinecraftServer s, String marca, boolean abrir) {
        BlockPos b = Complejo.pos(s, marca);
        if (b == null) {
            return;
        }
        ServerLevel l = s.overworld();
        List<BlockPos> ps = new ArrayList<>();
        for (int dx = 0; dx < Archivo.REJA_ANCHO; dx++) {
            for (int y = 0; y < Archivo.REJA_ALTO; y++) {
                ps.add(b.offset(dx, y, 0));
            }
        }
        for (BlockPos p : ps) {
            l.setBlock(p, abrir ? Blocks.AIR.defaultBlockState() : com.coleccionista.bloque.deco.Deco.barrotes(), 3);
        }
        if (!abrir) {
            for (BlockPos p : ps) {
                l.setBlock(p, Block.updateFromNeighbourShapes(com.coleccionista.bloque.deco.Deco.barrotes(), l, p), 3);
            }
        }
    }

    // ------------------------------------------------------------------ fases

    public static boolean empezar(MinecraftServer s, boolean conDiscurso) {
        if (fase != Fase.LISTA) {
            return false;
        }
        if (conDiscurso) {
            fase = Fase.HABLANDO;
            cambio = true;
            Pantallas.hablar(s, "traidor", () -> {
                if (fase == Fase.HABLANDO) {
                    roles(s);
                }
            });
        } else {
            Pantallas.callar(s);
            roles(s);
        }
        return true;
    }

    /** Cada uno ve en secreto su papel (todos al mismo tiempo, para que nadie se delate). */
    private static void roles(MinecraftServer s) {
        fase = Fase.ROLES;
        java.util.Map<UUID, Integer> voz = new java.util.HashMap<>();
        for (var g : salas) {
            for (UUID id : g.ids) {
                voz.put(id, g.k);
            }
        }
        com.coleccionista.voz.Voz.separar(voz);
        rolesHasta = t + 150;
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mia(s, sp);
        }
        Anuncio.sonido(s, SoundEvents.WARDEN_HEARTBEAT, 1F, 1F);
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_TRAIDOR roles");
    }

    private static void buscar(MinecraftServer s) {
        fase = Fase.BUSCAR;
        Evento ev = Evento.get(s);
        ev.reloj(s, segundosBuscar);
        Pantallas.modo(s, "texto", Component.translatable(MOD + ".tra.pantalla.buscar").getString());
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".tra.chat.buscar", intentosMax, segundosBuscar / 60)));
        Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.6F);
        // los bots: el que no es traidor a veces acierta (para las pruebas), en un momento al azar
        Random r = new Random(s.overworld().getRandom().nextLong());
        long now = s.overworld().getGameTime();
        for (Sala sa : salas) {
            for (UUID id : sa.ids) {
                Pieza p = ev.pieza(id);
                if (p != null && p.bot) {
                    botEn.put(id, now + 100 + r.nextInt(Math.max(1, segundosBuscar * 20 / 2)));
                }
            }
        }
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mia(s, sp);
        }
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_TRAIDOR buscar");
    }

    private static Sala salaDe(UUID id) {
        for (Sala sa : salas) {
            if (sa.ids.contains(id)) {
                return sa;
            }
        }
        return null;
    }

    /** Clic derecho en el teclado de la bóveda. */
    public static void usarTeclado(ServerPlayer sp, BlockPos pos) {
        Sala sa = salaDe(sp.getUUID());
        Pieza p = Evento.get(Plat.server(sp)).pieza(sp.getUUID());
        if (fase != Fase.BUSCAR || sa == null || p == null || !p.vivo() || sa.estado != Estado.BUSCANDO) {
            Plat.msg(sp, Component.translatable(MOD + ".tra.teclado_muerto").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (!pos.equals(Complejo.pos(Plat.server(sp), "arc.sala." + sa.k + ".teclado"))) {
            Plat.msg(sp, Component.translatable(MOD + ".tra.otra_sala").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        CoreScreens.open(sp, PANTALLA_TECLADO);
    }

    private static void codigo(ServerPlayer sp, CompoundTag d) {
        MinecraftServer s = Plat.server(sp);
        Sala sa = salaDe(sp.getUUID());
        if (fase != Fase.BUSCAR || sa == null || sa.estado != Estado.BUSCANDO) {
            return;
        }
        BlockPos tec = Complejo.pos(s, "arc.sala." + sa.k + ".teclado");
        if (tec == null || sp.distanceToSqr(Vec3.atCenterOf(tec)) > 36) {
            return;
        }
        probar(s, sa, d.getString("codigo"), sp.getUUID());
    }

    private static void probar(MinecraftServer s, Sala sa, String cod, UUID quien) {
        StringBuilder bueno = new StringBuilder();
        for (int c : sa.codigo) {
            bueno.append(c);
        }
        BlockPos tec = Complejo.pos(s, "arc.sala." + sa.k + ".teclado");
        Pieza p = Evento.get(s).pieza(quien);
        if (bueno.toString().equals(cod)) {
            abrir(s, sa);
            return;
        }
        sa.intentos--;
        if (tec != null) {
            s.overworld().playSound(null, tec, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.2F, 0.6F);
            s.overworld().playSound(null, tec, SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.BLOCKS, 1.2F, 0.5F);
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_TRAIDOR sala={} fallo quedan={}", sa.k, sa.intentos);
        avisarSala(s, sa, Component.translatable(MOD + ".tra.chat.fallo", p == null ? "?" : p.etiqueta(), sa.intentos));
        if (sa.intentos <= 0) {
            fallar(s, sa);
        }
        cambio = true;
        for (UUID id : sa.ids) {
            ServerPlayer o = s.getPlayerList().getPlayer(id);
            if (o != null) {
                mia(s, o);
            }
        }
    }

    private static void avisarSala(MinecraftServer s, Sala sa, Component c) {
        for (UUID id : sa.ids) {
            ServerPlayer o = s.getPlayerList().getPlayer(id);
            if (o != null) {
                Plat.msg(o, Anuncio.coleccionista(c), false);
            }
        }
    }

    /** ¡Abrieron la bóveda! El traidor queda al descubierto. */
    private static void abrir(MinecraftServer s, Sala sa) {
        Evento ev = Evento.get(s);
        sa.estado = Estado.ABIERTA;
        reja(s, "arc.sala." + sa.k + ".boveda", true);
        BlockPos b = Complejo.pos(s, "arc.sala." + sa.k + ".boveda");
        if (b != null) {
            s.overworld().playSound(null, b, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.5F, 0.6F);
            s.overworld().playSound(null, b, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1F, 0.8F);
        }
        Pieza tr = ev.pieza(sa.traidor);
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".tra.chat.abrieron", sa.k)));
        if (tr != null && tr.vivo()) {
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".tra.chat.descubierto", sa.k, Anuncio.nombre(tr))));
            if (ev.eliminarEnPrueba(s, tr, PRUEBA)) {
                eliminados++;
            }
        }
        sa.estado = Estado.RESUELTO;
        Coleccionista.LOGGER.info("COLECCIONISTA_TRAIDOR sala={} abierta", sa.k);
        cambio = true;
    }

    /** No la abrieron: el traidor elige a quién se lleva el Coleccionista. */
    private static void fallar(MinecraftServer s, Sala sa) {
        if (sa.estado != Estado.BUSCANDO) {
            return;
        }
        Evento ev = Evento.get(s);
        sa.estado = Estado.CASTIGO;
        sa.castigoHasta = t + segundosCastigo * 20L;
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".tra.chat.fallaron", sa.k)));
        Pieza tr = ev.pieza(sa.traidor);
        if (tr != null && tr.vivo()) {
            if (tr.bot) {
                botEn.put(tr.id, s.overworld().getGameTime() + 40 + s.overworld().getRandom().nextInt(80));
            } else {
                ServerPlayer sp = s.getPlayerList().getPlayer(tr.id);
                if (sp != null) {
                    mia(s, sp);
                    CoreScreens.open(sp, PANTALLA_VICTIMA);
                }
            }
        } else {
            sa.castigoHasta = t;
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_TRAIDOR sala={} fallida", sa.k);
        cambio = true;
    }

    private static void victima(ServerPlayer sp, CompoundTag d) {
        Sala sa = salaDe(sp.getUUID());
        if (sa == null || sa.estado != Estado.CASTIGO || !sp.getUUID().equals(sa.traidor) || !d.hasUUID("id")) {
            return;
        }
        UUID v = d.getUUID("id");
        if (v.equals(sa.traidor) || !sa.ids.contains(v)) {
            return;
        }
        castigar(Plat.server(sp), sa, v);
    }

    private static void castigar(MinecraftServer s, Sala sa, UUID v) {
        Evento ev = Evento.get(s);
        Random r = new Random(s.overworld().getRandom().nextLong());
        if (v == null) {
            List<UUID> leales = new ArrayList<>();
            for (UUID id : sa.ids) {
                Pieza p = ev.pieza(id);
                if (p != null && p.vivo() && !id.equals(sa.traidor)) {
                    leales.add(id);
                }
            }
            v = leales.isEmpty() ? null : leales.get(r.nextInt(leales.size()));
        }
        sa.victima = v;
        Pieza p = v == null ? null : ev.pieza(v);
        if (p != null && p.vivo()) {
            Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".tra.chat.victima", sa.k, Anuncio.nombre(p))));
            if (ev.eliminarEnPrueba(s, p, PRUEBA)) {
                eliminados++;
            }
        }
        sa.estado = Estado.RESUELTO;
        Coleccionista.LOGGER.info("COLECCIONISTA_TRAIDOR sala={} castigo", sa.k);
        cambio = true;
        ServerPlayer tsp = sa.traidor == null ? null : s.getPlayerList().getPlayer(sa.traidor);
        if (tsp != null) {
            mia(s, tsp);
        }
    }

    public static boolean alAcabarReloj(MinecraftServer s) {
        if (fase != Fase.BUSCAR) {
            return false;
        }
        for (Sala sa : salas) {
            if (sa.estado == Estado.BUSCANDO) {
                fallar(s, sa);
            }
        }
        return true;
    }

    /** El organizador corta el tiempo: las salas que no abrieron, fallan. */
    public static boolean cortar(MinecraftServer s) {
        if (fase != Fase.BUSCAR) {
            return false;
        }
        Evento.get(s).reloj(s, 0);
        return alAcabarReloj(s);
    }

    private static void tick(MinecraftServer s) {
        if (fase == Fase.NADA) {
            return;
        }
        long now = s.overworld().getGameTime();
        if (tpEn > 0 && now >= tpEn) {
            tpEn = -1;
            aLasSalas(s);
        }
        if (presentarEn > 0 && now >= presentarEn) {
            presentarEn = -1;
            Escena.presentacion(s, "II", MOD + ".tra.titulo", MOD + ".tra.lema");
            Anuncio.sonido(s, SoundEvents.BELL_BLOCK, 1F, 0.45F);
        }
        Evento ev = Evento.get(s);
        if (ev.fase() == Evento.Fase.PAUSA) {
            return;
        }
        t++;
        if (fase == Fase.ROLES && t >= rolesHasta) {
            buscar(s);
        }
        Random r = new Random(now);
        for (Map.Entry<UUID, Long> b : new ArrayList<>(botEn.entrySet())) {
            if (now < b.getValue()) {
                continue;
            }
            botEn.remove(b.getKey());
            Sala sa = salaDe(b.getKey());
            if (sa == null) {
                continue;
            }
            if (fase == Fase.BUSCAR && sa.estado == Estado.BUSCANDO && !b.getKey().equals(sa.traidor)) {
                // un bot leal intenta: a veces acierta, a veces se equivoca en un número
                StringBuilder c = new StringBuilder();
                for (int d : sa.codigo) {
                    c.append(d);
                }
                if (r.nextFloat() < 0.5F) {
                    c.setCharAt(0, (char) ('0' + (sa.codigo[0] + 1) % 10));
                }
                probar(s, sa, c.toString(), b.getKey());
                if (sa.estado == Estado.BUSCANDO) {
                    botEn.put(b.getKey(), now + 80 + r.nextInt(200));
                }
            } else if (sa.estado == Estado.CASTIGO && b.getKey().equals(sa.traidor)) {
                castigar(s, sa, null);
            }
        }
        boolean pendientes = false;
        for (Sala sa : salas) {
            if (sa.estado == Estado.CASTIGO && t >= sa.castigoHasta) {
                castigar(s, sa, null);
            }
            if (sa.estado != Estado.RESUELTO) {
                pendientes = true;
            }
        }
        if (!pendientes && fase == Fase.BUSCAR) {
            fin(s);
        }
        if (cambio || now % 20 == 0) {
            enviar(s);
        }
    }

    private static void fin(MinecraftServer s) {
        fase = Fase.FIN;
        com.coleccionista.voz.Voz.juntar();
        Evento ev = Evento.get(s);
        ev.reloj(s, 0);
        Pantallas.modo(s, "coleccionista", "");
        Anuncio.todos(s, Anuncio.coleccionista(Component.translatable(MOD + ".tra.chat.fin", eliminados)));
        for (Sala sa : salas) {
            Pieza tr = ev.pieza(sa.traidor);
            if (tr != null) {
                Anuncio.todos(s, Component.literal("  ").append(Component.translatable(MOD + ".tra.chat.era", sa.k, Anuncio.nombre(tr))
                        .withStyle(ChatFormatting.GRAY)));
            }
            reja(s, "arc.sala." + sa.k + ".reja", true);
        }
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            quitarNotas(sp);
            mia(s, sp);
        }
        cambio = true;
        Coleccionista.LOGGER.info("COLECCIONISTA_TRAIDOR fin eliminados={}", eliminados);
    }

    public static void terminar(MinecraftServer s) {
        if (fase == Fase.HABLANDO) {
            Pantallas.callar(s);
        }
        if (Complejo.construido(s, "archivo")) {
            for (int k = 1; k <= Archivo.SALAS; k++) {
                reja(s, "arc.sala." + k + ".reja", true);
                reja(s, "arc.sala." + k + ".boveda", false);
            }
        }
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            quitarNotas(sp);
        }
        limpiar(s);
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            mia(s, sp);
        }
        enviar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_TRAIDOR terminada");
    }

    public static void reiniciar(MinecraftServer s) {
        if (fase != Fase.NADA) {
            terminar(s);
        }
    }

    private static void limpiar(MinecraftServer s) {
        com.coleccionista.voz.Voz.juntar();
        fase = Fase.NADA;
        salas.clear();
        botEn.clear();
        eliminados = 0;
        tpEn = -1;
        presentarEn = -1;
        t = 0;
    }

    // ------------------------------------------------------------------ a los clientes

    public static CompoundTag estado(MinecraftServer s) {
        CompoundTag c0 = new CompoundTag();
        c0.putString("fase", fase.name());
        ListTag l = new ListTag();
        for (Sala sa : salas) {
            CompoundTag x = new CompoundTag();
            x.putInt("k", sa.k);
            x.putString("estado", sa.estado.name());
            x.putInt("intentos", sa.intentos);
            ListTag ms = new ListTag();
            for (UUID id : sa.ids) {
                CompoundTag m = new CompoundTag();
                m.putUUID("id", id);
                ms.add(m);
            }
            x.put("miembros", ms);
            l.add(x);
        }
        c0.put("salas", l);
        return c0;
    }

    /** Lo secreto de cada jugador: su papel, su sala, cuántos números tiene el código y si le toca elegir víctima. */
    private static void mia(MinecraftServer s, ServerPlayer sp) {
        CompoundTag c = new CompoundTag();
        Sala sa = fase == Fase.NADA ? null : salaDe(sp.getUUID());
        Evento ev = Evento.get(s);
        if (sa != null && fase != Fase.LISTA && fase != Fase.HABLANDO) {
            c.putString("rol", sp.getUUID().equals(sa.traidor) ? "traidor" : "leal");
            c.putInt("sala", sa.k);
            c.putInt("digitos", sa.codigo.length);
            c.putInt("intentos", sa.intentos);
            if (sa.estado == Estado.CASTIGO && sp.getUUID().equals(sa.traidor)) {
                c.putBoolean("elige", true);
                ListTag cand = new ListTag();
                for (UUID id : sa.ids) {
                    Pieza p = ev.pieza(id);
                    if (p != null && p.vivo() && !id.equals(sa.traidor)) {
                        CompoundTag m = new CompoundTag();
                        m.putUUID("id", id);
                        m.putInt("numero", p.numero);
                        m.putString("nombre", p.nombre);
                        cand.add(m);
                    }
                }
                c.put("candidatos", cand);
            }
        }
        PlayerStore.Sync.toPlayer(sp, KEY_MIA, c);
    }

    private static void enviar(MinecraftServer s) {
        cambio = false;
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }

    /** Para la prueba automática: escribe el código bueno (o uno malo) por la sala de una pieza. */
    public static boolean probarComo(MinecraftServer s, Pieza p, boolean bueno) {
        Sala sa = p == null ? null : salaDe(p.id);
        if (fase != Fase.BUSCAR || sa == null || sa.estado != Estado.BUSCANDO) {
            return false;
        }
        StringBuilder c = new StringBuilder();
        for (int d : sa.codigo) {
            c.append(d);
        }
        if (!bueno) {
            c.setCharAt(0, (char) ('0' + (sa.codigo[0] + 1) % 10));
        }
        probar(s, sa, c.toString(), p.id);
        return true;
    }
}
