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

    private Celdas() {
    }

    public static void init() {
        Actions.on(ACCION_CODIGO, Celdas::probarCodigo);
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
            sp.teleportTo(l, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0);
            if (acostar) {
                BlockPos cama = Complejo.pos(s, "celda." + p.celda + ".cama");
                if (cama != null && l.getBlockState(cama).is(net.minecraft.tags.BlockTags.BEDS)) {
                    sp.startSleeping(cama);
                }
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
     * Prepara el acertijo de todas las celdas: un código de 3 números por celda y sus pistas (una nota en el barril, dos rayones en las
     * paredes) más una página del diario de alguna "pieza anterior" para leer mientras esperan.
     */
    public static void prepararPistas(MinecraftServer s) {
        ServerLevel l = mundo(s);
        Random r = new Random(l.getGameTime() ^ 0x5EEDL);
        CompoundTag d = datos(s);
        for (int n = 1; n <= com.coleccionista.mapa.AlaCeldas.CELDAS; n++) {
            int a = 1 + r.nextInt(9), b = r.nextInt(10), c = r.nextInt(10);
            d.putInt("codigo" + n, a * 100 + b * 10 + c);
            BlockPos barril = Complejo.pos(s, "celda." + n + ".barril");
            // el baúl de la celda (o un barril en mapas viejos)
            if (barril != null && l.getBlockEntity(barril) instanceof net.minecraft.world.Container be && be.getContainerSize() >= 16) {
                be.clearContent();
                be.setItem(11, nota(Component.translatable(MOD + ".pista.nota_titulo"), Component.translatable(MOD + ".pista.nota1", a)));
                be.setItem(15, diario(n, r));
                be.setChanged();
            }
            rayon(l, Complejo.pos(s, "celda." + n + ".letrero"), "west", "·  " + b + "  ·", Component.translatable(MOD + ".pista.rayon_medio")
                    .getString());
            rayon(l, Complejo.pos(s, "celda." + n + ".letrero2"), "east", "·  ·  " + c, Component.translatable(MOD + ".pista.rayon_fin")
                    .getString());
        }
        d.putBoolean("candados", false);
        sucio(s);
        Coleccionista.LOGGER.info("COLECCIONISTA_PISTAS listas");
    }

    private static ItemStack nota(Component titulo, Component texto) {
        ItemStack st = new ItemStack(Items.PAPER);
        st.setHoverName(titulo.copy().withStyle(s -> s.withItalic(false).withColor(ChatFormatting.GOLD)));
        CompoundTag disp = st.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(texto.copy().withStyle(s -> s.withItalic(true).withColor(ChatFormatting.GRAY)))));
        disp.put("Lore", lore);
        return st;
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

    /** Rayones en la pared de la celda (una pista): se escriben encima del muro, como raspados con algo filoso. */
    private static void rayon(ServerLevel l, BlockPos p, String facing, String linea, String sub) {
        if (p == null) {
            return;
        }
        BlockState st = Complejo.estado("coleccionista:rayon[facing=" + facing + "]");
        if (st.isAir()) {
            return;
        }
        l.setBlock(p, st, 2);
        if (l.getBlockEntity(p) instanceof com.coleccionista.bloque.deco.TextoBE be) {
            be.escribir(0xC9C2B4, linea, sub);
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
        CompoundTag t = new CompoundTag();
        t.putInt("celda", celda);
        t.putLong("cuando", p.level().getGameTime());
        PlayerStore.Sync.toPlayer(p, KEY_CANDADO, t);
        CoreScreens.open(p, MOD + ":candado");
    }

    private static void probarCodigo(ServerPlayer p, CompoundTag d) {
        MinecraftServer s = Plat.server(p);
        int celda = Nbt.i(d, "celda");
        BlockPos c = Complejo.pos(s, "celda." + celda + ".candado");
        if (c == null || !candados(s) || p.distanceToSqr(c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5) > 36) {
            return;
        }
        int puesto = Nbt.i(d, "codigo");
        if (puesto == codigo(s, celda)) {
            Coleccionista.LOGGER.info("COLECCIONISTA_CELDA_ABIERTA {} por {}", celda, p.getGameProfile().getName());
            puertas(s, celda, true);
            p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.6F, 0.7F);
            Plat.msg(p, Component.translatable(MOD + ".candado.abierto").withStyle(ChatFormatting.GREEN), true);
        } else {
            p.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.MASTER, 1F, 0.5F);
            Plat.msg(p, Component.translatable(MOD + ".candado.mal").withStyle(ChatFormatting.RED), true);
            com.axeliscore.fx.Fx.shake(p, 0.4F, 6);
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
