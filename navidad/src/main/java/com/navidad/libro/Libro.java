package com.navidad.libro;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.fx.Fx;
import com.navidad.ArbolSanta;
import com.navidad.Navidad;
import com.navidad.objeto.Premios;
import com.navidad.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.util.List;

/**
 * (Servidor) El Libro de Navidad: cuenta lo que vas haciendo para cada tarea, avisa cuando una está lista, da los premios cuando los
 * reclamas desde el libro y le manda al cliente todo lo que necesita para dibujarlo (progreso, premios ya reclamados y cómo va tu árbol).
 * Se guarda por jugador en el mundo.
 */
public final class Libro {
    /** Lo que recibe el cliente para dibujar el libro. */
    public static final String KEY = Navidad.MODID + ":libro";
    /** Cuántas tareas ha completado alguien (para la prueba con fotos). */
    public static int completadas;

    private Libro() {
    }

    public static void init() {
        AxelisCore.onLogin(Libro::sync);
        Actions.on(Navidad.MODID + ":libro_abrir", (p, d) -> {
            ArbolSanta.revisarDe(p);
            sync(p);
        });
        Actions.on(Navidad.MODID + ":libro_reclamar", (p, d) -> reclamar(p, d.getString("tarea")));
        Actions.on(Navidad.MODID + ":libro_capitulo", (p, d) -> reclamarCapitulo(p, d.getInt("cap")));
        MinecraftForge.EVENT_BUS.addListener(Libro::alRomper);
        MinecraftForge.EVENT_BUS.addListener(Libro::alPoner);
        MinecraftForge.EVENT_BUS.addListener(Libro::alPescar);
        MinecraftForge.EVENT_BUS.addListener(Libro::alMorir);
        MinecraftForge.EVENT_BUS.addListener(Libro::alAparecer);
    }

    static CompoundTag datos(ServerPlayer p) {
        WorldStore w = WorldStore.get(p.server, Navidad.MODID);
        CompoundTag yo = w.player(p.getUUID());
        if (!yo.contains("libro")) {
            yo.put("libro", new CompoundTag());
        }
        CompoundTag l = yo.getCompound("libro");
        for (String k : new String[]{"p", "r", "cr"}) {
            if (!l.contains(k)) {
                l.put(k, new CompoundTag());
            }
        }
        return l;
    }

    // ------------------------------------------------------------------ avanzar

    /** Pasó algo que puede contar para una tarea (por ejemplo "adorno", "pescar" o "logro:pinguino"). */
    public static void evento(ServerPlayer p, String evento, int n) {
        avanzar(p, evento, n, Tareas.Modo.SUMA);
    }

    /** Un valor que solo cuenta si es más alto que antes (el nivel o la variedad del árbol). */
    public static void maximo(ServerPlayer p, String evento, int valor) {
        avanzar(p, evento, valor, Tareas.Modo.MAXIMO);
    }

    public static void nivelArbol(ServerPlayer p, int nivel) {
        maximo(p, "nivel_arbol", nivel);
    }

    private static void avanzar(ServerPlayer p, String evento, int n, Tareas.Modo modo) {
        if (p == null || p.server == null) {
            return;
        }
        CompoundTag l = datos(p), prog = l.getCompound("p");
        boolean cambio = false;
        for (Tareas.Tarea t : Tareas.TODAS) {
            if (!t.evento().equals(evento) || t.modo() != modo) {
                continue;
            }
            int antes = prog.getInt(t.id());
            if (antes >= t.meta()) {
                continue;
            }
            int ahora = Math.min(t.meta(), modo == Tareas.Modo.SUMA ? antes + n : Math.max(antes, n));
            if (ahora == antes) {
                continue;
            }
            prog.putInt(t.id(), ahora);
            cambio = true;
            if (ahora >= t.meta()) {
                lista(p, t);
            }
        }
        if (cambio) {
            WorldStore.get(p.server, Navidad.MODID).dirty();
            sync(p);
        }
    }

    /** Una tarea quedó lista: aviso con su ícono y sonido de páginas. */
    private static void lista(ServerPlayer p, Tareas.Tarea t) {
        completadas++;
        ItemStack icono = pila(t.icono(), 1);
        Fx.toast(p, icono, Component.translatable("navidad.libro.lista"), Component.translatable("navidad.libro.tarea." + t.id()), 0xFFD84A);
        p.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1F, 1F);
        p.playNotifySound(ModRegistry.CASCABEL.get(), SoundSource.PLAYERS, 0.8F, 1.5F);
        Navidad.LOGGER.info("NAVIDAD_LIBRO {} completó la tarea {}", p.getGameProfile().getName(), t.id());
    }

    // ------------------------------------------------------------------ reclamar

    public static boolean completa(CompoundTag l, Tareas.Tarea t) {
        return l.getCompound("p").getInt(t.id()) >= t.meta();
    }

    static void reclamar(ServerPlayer p, String id) {
        Tareas.Tarea t = Tareas.tarea(id);
        CompoundTag l = datos(p);
        if (t == null || !completa(l, t) || l.getCompound("r").getBoolean(id)) {
            return;
        }
        l.getCompound("r").putBoolean(id, true);
        WorldStore.get(p.server, Navidad.MODID).dirty();
        dar(p, t.premios());
        p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
        p.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, 1.2F);
        Navidad.LOGGER.info("NAVIDAD_LIBRO {} reclamó {}", p.getGameProfile().getName(), id);
        sync(p);
    }

    static void reclamarCapitulo(ServerPlayer p, int cap) {
        if (cap < 0 || cap >= Tareas.CAPITULOS.size()) {
            return;
        }
        CompoundTag l = datos(p);
        String k = String.valueOf(cap);
        if (l.getCompound("cr").getBoolean(k)) {
            return;
        }
        for (Tareas.Tarea t : Tareas.delCapitulo(cap)) {
            if (!l.getCompound("r").getBoolean(t.id())) {
                return;
            }
        }
        l.getCompound("cr").putBoolean(k, true);
        WorldStore.get(p.server, Navidad.MODID).dirty();
        dar(p, Tareas.CAPITULOS.get(cap).premios());
        Fx.title(p, Component.translatable("navidad.libro.capitulo_titulo"), Component.translatable("navidad.libro.cap." + cap), 0xFFD84A);
        Premios.confeti(p.serverLevel(), p.getX(), p.getY() + 1.6, p.getZ(), true);
        p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1F);
        Navidad.LOGGER.info("NAVIDAD_LIBRO {} terminó el capítulo {}", p.getGameProfile().getName(), cap);
        sync(p);
    }

    private static void dar(ServerPlayer p, List<Tareas.Premio> premios) {
        for (Tareas.Premio pr : premios) {
            ItemStack st = pila(pr.id(), pr.n());
            if (st.isEmpty()) {
                continue;
            }
            if (!p.getInventory().add(st)) {
                p.drop(st, false);
            }
        }
    }

    public static ItemStack pila(String id, int n) {
        var it = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return it == null || it == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(it, n);
    }

    // ------------------------------------------------------------------ mandar al cliente

    public static void sync(ServerPlayer p) {
        CompoundTag l = datos(p).copy();
        CompoundTag arbol = ArbolSanta.estadoGuardado(p);
        if (arbol != null) {
            l.put("arbol", arbol);
        }
        l.putString("nombre", p.getGameProfile().getName());
        PlayerStore.Sync.toPlayer(p, KEY, l);
    }

    // ------------------------------------------------------------------ lo que cuenta del mundo

    static void alRomper(BlockEvent.BreakEvent e) {
        if (e.getPlayer() instanceof ServerPlayer p && !p.isCreative()
                && (e.getState().is(Blocks.SPRUCE_LOG) || e.getState().is(Blocks.STRIPPED_SPRUCE_LOG) || e.getState().is(Blocks.SPRUCE_WOOD))) {
            evento(p, "tronco_abeto", 1);
        }
    }

    static void alPoner(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(e.getPlacedBlock().getBlock());
        if (id != null && Navidad.MODID.equals(id.getNamespace()) && !ArbolSanta.esArbol(e.getPlacedBlock().getBlock())) {
            evento(p, "deco_puesta", 1);
        }
    }

    static void alPescar(ItemFishedEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            evento(p, "pescar", 1);
        }
    }

    static void alMorir(LivingDeathEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer p && e.getEntity() instanceof com.navidad.entidad.MunecoMalvado) {
            evento(p, "muneco", 1);
        }
    }

    static void alAparecer(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || e.loadedFromDisk()) {
            return;
        }
        if (e.getEntity() instanceof Snowball s && s.getOwner() instanceof ServerPlayer p) {
            evento(p, "bola_nieve", 1);
        } else if (e.getEntity() instanceof SnowGolem g) {
            // un gólem recién armado: cuenta para quien esté más cerca (el que le puso la calabaza)
            Player p = g.level().getNearestPlayer(g, 8);
            if (p instanceof ServerPlayer sp) {
                evento(sp, "golem_nieve", 1);
            }
        }
    }
}
