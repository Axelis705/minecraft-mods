package com.coleccionista.evento;

import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import com.axeliscore.ui.CoreScreens;
import com.coleccionista.Coleccionista;
import com.coleccionista.bloque.FocoBlock;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Las celdas del Ala de celdas: a quién le toca cada una, llevar a cada pieza a la suya (acostada en la cama mientras espera), las luces,
 * las puertas y el acertijo para salir (3 pistas en la celda y un candado con el código).
 */
public final class Celdas {
    private static final String MOD = Coleccionista.MODID;
    /** Al jugador: qué candado está usando (para la ventana del código). */
    public static final String KEY_CANDADO = MOD + ":candado";
    public static final String ACCION_CODIGO = MOD + ":codigo";
    /** La mano toca el candado (gira una rueda o aprieta el botón): los demás ven el brazo moverse y oyen el clic. */
    public static final String ACCION_TOQUE = MOD + ":toque";

    private Celdas() {
    }

    public static void init() {
        Actions.on(ACCION_CODIGO, Celdas::probarCodigo);
        Actions.on(ACCION_TOQUE, Celdas::toque);
        com.axeliscore.AxelisCore.onServerTick(Celdas::vigilarSalida);
    }

    // ------------------------------------------------------------------ cuando todas salen de su celda

    /** Ticks que espera el Coleccionista desde que la última pieza salió de su celda hasta que habla en la pantalla grande. */
    public static int esperaSalida = 100;
    private static long salidaEn = -1;

    /**
     * Ya funcionan los candados: cuando todas las piezas vivas (las personas conectadas) están fuera de las celdas, a los pocos segundos el
     * Coleccionista habla en la pantalla grande del escenario (una sola vez por despertar).
     */
    private static void vigilarSalida(MinecraftServer s) {
        if (s.getTickCount() % 10 != 0 || !candados(s) || datos(s).getBoolean("salida") || !Complejo.construido(s)) {
            salidaEn = -1;
            return;
        }
        Evento ev = Evento.get(s);
        int personas = 0;
        boolean todas = true;
        for (Pieza p : ev.vivos()) {
            net.minecraft.server.level.ServerPlayer sp = p.bot ? null : s.getPlayerList().getPlayer(p.id);
            if (sp == null) {
                continue;
            }
            personas++;
            if (enCelda(s, sp.blockPosition())) {
                todas = false;
                break;
            }
        }
        long now = s.overworld().getGameTime();
        if (personas == 0 || !todas) {
            salidaEn = -1;
            return;
        }
        if (salidaEn < 0) {
            salidaEn = now + esperaSalida;
            Coleccionista.LOGGER.info("COLECCIONISTA_SALIDA todas fuera piezas={}", personas);
        } else if (now >= salidaEn && !Pantallas.ocupado()) {
            hablarSalida(s);
        }
    }

    /** El discurso de cuando todas salieron (también a mano: {@code /evento salida}). */
    public static boolean hablarSalida(MinecraftServer s) {
        if (Pantallas.ocupado()) {
            return false;
        }
        datos(s).putBoolean("salida", true);
        sucio(s);
        salidaEn = -1;
        Pantallas.hablar(s, "salida", null);
        Coleccionista.LOGGER.info("COLECCIONISTA_SALIDA discurso");
        return true;
    }

    /** ¿Ese punto cae dentro de alguna celda del Ala (las dos filas, los dos pisos)? */
    public static boolean enCelda(MinecraftServer s, BlockPos b) {
        BlockPos o = Complejo.origen(s, "celdas");
        if (o == null) {
            return false;
        }
        int x = b.getX() - o.getX(), y = b.getY() - o.getY(), z = b.getZ() - o.getZ();
        return x >= 1 && x <= 35 && Math.floorMod(x, 6) != 0 && y >= 0 && y <= 10 && (z >= 1 && z <= 5 || z >= 17 && z <= 21);
    }

    private static CompoundTag datos(MinecraftServer s) {
        CompoundTag g = WorldStore.get(s, MOD).global();
        if (!g.contains("celdas")) {
            g.put("celdas", new CompoundTag());
        }
        return g.getCompound("celdas");
    }

    private static void sucio(MinecraftServer s) {
        WorldStore.get(s, MOD).dirty();
    }

    public static ServerLevel mundo(MinecraftServer s) {
        return s.overworld();
    }

    // ------------------------------------------------------------------ a quién le toca cada celda

    /** Le da una celda libre (la más baja) a la pieza, si no tenía. */
    public static int asignar(MinecraftServer s, Evento ev, Pieza p) {
        if (p.celda > 0) {
            return p.celda;
        }
        Set<Integer> usadas = new HashSet<>();
        for (Pieza o : ev.piezas()) {
            usadas.add(o.celda);
        }
        for (int n = 1; n <= com.coleccionista.mapa.AlaCeldas.CELDAS; n++) {
            if (!usadas.contains(n)) {
                p.celda = n;
                Coleccionista.LOGGER.info("COLECCIONISTA_CELDA {} {} -> {}", p.etiqueta(), p.nombre, n);
                return n;
            }
        }
        return 0;
    }

    /** Lleva a la pieza a su celda, mirando a su pantalla. Con {@code acostar}, la deja dormida en la cama. */
    public static void llevar(MinecraftServer s, Pieza p, boolean acostar) {
        if (p.celda <= 0 || !Complejo.construido(s)) {
            return;
        }
        Entity e = Evento.entidad(s, p);
        BlockPos at = Complejo.pos(s, "celda." + p.celda);
        if (e == null || at == null) {
            return;
        }
        float yaw = Complejo.yaw("celda." + p.celda);
        ServerLevel l = mundo(s);
        if (e instanceof ServerPlayer sp) {
            if (sp.isSleeping()) {
                sp.stopSleeping();
            }
            BlockPos cama = acostar ? Complejo.pos(s, "celda." + p.celda + ".cama") : null;
            if (cama != null && l.getBlockState(cama).is(net.minecraft.tags.BlockTags.BEDS)) {
                // directo a la cama: si se le manda primero a medio cuarto, al confirmar ese salto el servidor lo regresa ahí y queda
                // "acostado" en el piso, lejos del catre (así no se veía en la cinemática)
                sp.teleportTo(l, cama.getX() + 0.5, cama.getY() + 0.6875, cama.getZ() + 0.5, yaw, 0);
                sp.startSleeping(cama);
            } else {
                sp.teleportTo(l, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0);
            }
        } else {
            if (e.level() != l) {
                return;
            }
            e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0);
            if (e instanceof Mob m) {
                m.getNavigation().stop();
                m.setYHeadRot(yaw);
                m.setYBodyRot(yaw);
            }
        }
    }

    // ------------------------------------------------------------------ luces y puertas

    /** Prende o apaga el foco de todas las celdas. */
    public static void luces(MinecraftServer s, boolean on) {
        ServerLevel l = mundo(s);
        for (int n = 1; n <= com.coleccionista.mapa.AlaCeldas.CELDAS; n++) {
            foco(l, Complejo.pos(s, "celda." + n + ".foco"), on);
        }
        // y las lámparas del pasillo
        for (int k = 0; k < 200; k++) {
            BlockPos q = Complejo.pos(s, "ala.luz." + k);
            if (q == null) {
                break;
            }
            BlockState st = l.getBlockState(q);
            if (st.hasProperty(com.coleccionista.bloque.deco.DecoBlock.ENCENDIDA) && st.getValue(com.coleccionista.bloque.deco.DecoBlock.ENCENDIDA) != on) {
                l.setBlock(q, st.setValue(com.coleccionista.bloque.deco.DecoBlock.ENCENDIDA, on), 3);
            }
        }
    }

    static void foco(ServerLevel l, BlockPos p, boolean on) {
        if (p == null) {
            return;
        }
        BlockState st = l.getBlockState(p);
        if (st.getBlock() instanceof FocoBlock && st.getValue(FocoBlock.ENCENDIDO) != on) {
            l.setBlock(p, st.setValue(FocoBlock.ENCENDIDO, on), 3);
        }
    }

    /** Abre o cierra la puerta de una celda (0 = todas). */
    public static void puertas(MinecraftServer s, int celda, boolean abrir) {
        ServerLevel l = mundo(s);
        for (int n = 1; n <= com.coleccionista.mapa.AlaCeldas.CELDAS; n++) {
            if (celda != 0 && n != celda) {
                continue;
            }
            BlockPos p = Complejo.pos(s, "celda." + n + ".puerta");
            if (p == null) {
                continue;
            }
            for (BlockPos q : new BlockPos[] {p, p.above()}) {
                BlockState st = l.getBlockState(q);
                if (st.getBlock() instanceof DoorBlock && st.getValue(DoorBlock.OPEN) != abrir) {
                    l.setBlock(q, st.setValue(DoorBlock.OPEN, abrir), 2 | 16);
                }
            }
            // el sonido de cada reja (pestillo, riel y golpe) lo pone la reja misma al moverse (RejaBE)
        }
        if (celda == 0) {
            Anuncio.sonido(s, abrir ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, 1F, 0.6F);
        }
    }

    // ------------------------------------------------------------------ el acertijo de la celda

    /** ¿Ya se pueden usar los candados? (después de que habla el Coleccionista) */
    public static boolean candados(MinecraftServer s) {
        return datos(s).getBoolean("candados");
    }

    public static void candados(MinecraftServer s, boolean on) {
        datos(s).putBoolean("candados", on);
        sucio(s);
    }

    public static int codigo(MinecraftServer s, int celda) {
        return Nbt.i(datos(s), "codigo" + celda);
    }

    /**
     * Prepara el acertijo de todas las celdas (sala de espera): un código de 3 números distinto en cada celda. Todavía no hay nada que
     * encontrar: el baúl queda vacío y los números no están escritos; salen en el apagón del final de la espera ({@link Pistas}).
     */
    public static void prepararPistas(MinecraftServer s) {
        ServerLevel l = mundo(s);
        Random r = new Random(l.getGameTime() ^ 0x5EEDL);
        CompoundTag d = datos(s);
        d.putBoolean("salida", false);
        Set<Integer> usados = new HashSet<>();
        for (int n = 1; n <= com.coleccionista.mapa.AlaCeldas.CELDAS; n++) {
            int cod;
            do {
                cod = 100 + r.nextInt(900);
            } while (!usados.add(cod));
            d.putInt("codigo" + n, cod);
            if (baul(l, Complejo.pos(s, "celda." + n + ".barril")) instanceof net.minecraft.world.Container be) {
                be.clearContent();
                be.setChanged();
            }
            // los rayones con pistas de antes (ya no se usan) se quitan si quedaron en el mundo
            quitarRayon(l, Complejo.pos(s, "celda." + n + ".letrero"));
            quitarRayon(l, Complejo.pos(s, "celda." + n + ".letrero2"));
        }
        d.putBoolean("candados", false);
        sucio(s);
        Pistas.ocultar(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_PISTAS listas");
    }

    /** Al volver la luz: en el baúl de cada celda, una hoja del diario de alguna "pieza anterior". */
    public static void llenarBaules(MinecraftServer s) {
        ServerLevel l = mundo(s);
        Random r = new Random(l.getGameTime() ^ 0xD1A10L);
        for (int n = 1; n <= com.coleccionista.mapa.AlaCeldas.CELDAS; n++) {
            if (baul(l, Complejo.pos(s, "celda." + n + ".barril")) instanceof net.minecraft.world.Container be
                    && be.getContainerSize() > 13) {
                be.clearContent();
                be.setItem(13, diario(n, r));
                be.setChanged();
            }
        }
    }

    /** Una página del diario de una "pieza anterior" (hay varias; cada celda tiene una al azar). */
    private static ItemStack diario(int celda, Random r) {
        int i = 1 + r.nextInt(8);
        ItemStack st = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag t = st.getOrCreateTag();
        t.putString("title", Component.translatable(MOD + ".diario.titulo").getString());
        t.putString("author", "Pieza 0" + (r.nextInt(9) + 1));
        ListTag pages = new ListTag();
        pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(MOD + ".diario." + i))));
        t.put("pages", pages);
        t.putBoolean("resolved", true);
        return st;
    }

    private static BlockEntity baul(ServerLevel l, BlockPos p) {
        return p == null ? null : l.getBlockEntity(p);
    }

    private static void quitarRayon(ServerLevel l, BlockPos p) {
        if (p != null && net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(l.getBlockState(p).getBlock()).getPath().equals("rayon")) {
            l.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
    }

    /** Alguien usó un candado: si ya se puede, se le abre la ventana del código. */
    public static void usarCandado(ServerPlayer p, BlockPos pos) {
        MinecraftServer s = Plat.server(p);
        int celda = 0;
        for (int n = 1; n <= com.coleccionista.mapa.AlaCeldas.CELDAS; n++) {
            if (pos.equals(Complejo.pos(s, "celda." + n + ".candado"))) {
                celda = n;
            }
        }
        if (celda == 0) {
            return;
        }
        if (!candados(s)) {
            Plat.msg(p, Component.translatable(MOD + ".candado.muerto").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
            p.playNotifySound(SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.6F, 0.6F);
            return;
        }
        PlayerStore.Sync.toPlayer(p, KEY_CANDADO, candado(p, celda, pos, ""));
        CoreScreens.open(p, MOD + ":candado");
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    /** Lo que sabe el cliente del candado que está usando: la celda, dónde está, hacia dónde mira y cómo salió el último intento. */
    private static CompoundTag candado(ServerPlayer p, int celda, BlockPos pos, String res) {
        CompoundTag t = new CompoundTag();
        t.putInt("celda", celda);
        t.putLong("cuando", p.level().getGameTime());
        t.putInt("x", pos.getX());
        t.putInt("y", pos.getY());
        t.putInt("z", pos.getZ());
        BlockState st = p.level().getBlockState(pos);
        t.putString("f", st.hasProperty(com.coleccionista.bloque.CandadoBlock.FACING) ? st.getValue(com.coleccionista.bloque.CandadoBlock.FACING)
                .getName() : "north");
        t.putString("res", res);
        return t;
    }

    /** La mano movió una rueda o apretó el botón: el brazo se mueve para los demás y se oye el clic ahí. */
    private static void toque(ServerPlayer p, CompoundTag d) {
        MinecraftServer s = Plat.server(p);
        int celda = Nbt.i(d, "celda");
        BlockPos c = Complejo.pos(s, "celda." + celda + ".candado");
        long now = p.level().getGameTime();
        if (c == null || p.distanceToSqr(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5) > 36
                || now - p.getPersistentData().getLong(MOD + ":toque") < 2) {
            return;
        }
        p.getPersistentData().putLong(MOD + ":toque", now);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        boolean boton = "boton".equals(d.getString("tipo"));
        p.level().playSound(p, c, boton ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS,
                boton ? 0.7F : 0.35F, boton ? 0.8F : 1.9F);
    }

    private static void probarCodigo(ServerPlayer p, CompoundTag d) {
        MinecraftServer s = Plat.server(p);
        int celda = Nbt.i(d, "celda");
        BlockPos c = Complejo.pos(s, "celda." + celda + ".candado");
        if (c == null || !candados(s) || p.distanceToSqr(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5) > 36) {
            return;
        }
        int puesto = Nbt.i(d, "codigo");
        boolean bueno = puesto == codigo(s, celda);
        PlayerStore.Sync.toPlayer(p, KEY_CANDADO, candado(p, celda, c, bueno ? "ok" : "mal"));
        if (bueno) {
            Coleccionista.LOGGER.info("COLECCIONISTA_CELDA_ABIERTA {} por {}", celda, p.getGameProfile().getName());
            puertas(s, celda, true);
            p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.6F, 0.7F);
            p.level().playSound(p, c, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.3F);
            Plat.msg(p, Component.translatable(MOD + ".candado.abierto").withStyle(ChatFormatting.GREEN), true);
        } else {
            Coleccionista.LOGGER.info("COLECCIONISTA_CANDADO_MAL celda={} puso={}", celda, puesto);
            p.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.MASTER, 1F, 0.5F);
            p.level().playSound(p, c, SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.BLOCKS, 0.7F, 0.5F);
            Plat.msg(p, Component.translatable(MOD + ".candado.mal").withStyle(ChatFormatting.RED), true);
        }
    }

    /** Para pruebas: abre con el código correcto. */
    public static boolean abrirConCodigo(MinecraftServer s, int celda, int codigo) {
        if (codigo != codigo(s, celda)) {
            return false;
        }
        puertas(s, celda, true);
        return true;
    }

    /** ¿Se sostiene esta entidad de bloque? (para avisar en el registro si el plano no puso algo) */
    static boolean hay(ServerLevel l, BlockPos p, Class<? extends BlockEntity> c) {
        return p != null && c.isInstance(l.getBlockEntity(p));
    }
}
