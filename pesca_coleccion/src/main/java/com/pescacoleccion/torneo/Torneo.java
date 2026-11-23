package com.pescacoleccion.torneo;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.nilo.Encargos;
import com.pescacoleccion.nilo.Tienda;
import com.pescacoleccion.pesca.Especie;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * (Servidor) Torneos de pesca con amigos: alguien lo empieza en el muelle de Nilo (modo y minutos) y todos los que pesquen en ese
 * tiempo entran. Modos: más peces, el pez más grande para su especie, o puntos por rareza (y bonus sin errores). Al terminar, copas de
 * oro, plata y bronce (con el nombre del ganador), esmeraldas si jugaron dos o más, y amistad con Nilo para todos.
 */
public final class Torneo {
    private static final String MOD = PescaColeccion.MODID;
    public static final String KEY = MOD + ":torneo";
    public static final String[] MODOS = {"peces", "grande", "puntos"};
    public static final int[] MINUTOS = {3, 5, 10, 15};
    /** Puntos por rareza (común ... legendario). */
    public static final int[] PUNTOS = {1, 3, 6, 12, 25};
    public static final int[] ESMERALDAS = {12, 6, 3};

    private static boolean activo;
    /** El servidor del torneo (en un mundo de un jugador, al cambiar de mundo el torneo se olvida). */
    private static MinecraftServer servidor;
    private static String modo = "peces";
    private static long finTick;
    private static String quien = "";
    private static UUID quienId;
    private static final Map<UUID, Fila> TABLA = new LinkedHashMap<>();

    /** Un participante. */
    static final class Fila {
        final UUID id;
        final String nombre;
        float puntos;
        int peces;
        String detalle = "";

        Fila(UUID id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }

    private Torneo() {
    }

    public static void init() {
        AxelisCore.onServerTick(Torneo::tick);
        AxelisCore.onLogin(p -> PlayerStore.Sync.toPlayer(p, KEY, estado(Plat.server(p))));
        Actions.on(MOD + ":torneo_empezar", (p, d) -> {
            if (Tienda.cerca(p)) {
                empezar(p, Nbt.i(d, "min"), Nbt.s(d, "modo"));
            }
        });
        Actions.on(MOD + ":torneo_terminar", (p, d) -> {
            if (activo && (p.getUUID().equals(quienId) || Plat.isOp(p))) {
                terminar(Plat.server(p));
            }
        });
    }

    public static boolean activo() {
        return activo;
    }

    /** ¿Hay torneo en este servidor? (para el estado que se manda al entrar) */
    private static boolean activoEn(MinecraftServer s) {
        return activo && s == servidor;
    }

    /** Empieza un torneo (si no hay otro). */
    public static boolean empezar(ServerPlayer p, int min, String m) {
        MinecraftServer s = Plat.server(p);
        if (activo) {
            Plat.msg(p, Component.translatable(MOD + ".torneo.ya_hay"), true);
            return false;
        }
        int minutos = MINUTOS[0];
        for (int x : MINUTOS) {
            if (x == min) {
                minutos = x;
            }
        }
        modo = List.of(MODOS).contains(m) ? m : "peces";
        activo = true;
        servidor = s;
        finTick = s.getTickCount() + minutos * 60L * 20L;
        quien = p.getGameProfile().getName();
        quienId = p.getUUID();
        TABLA.clear();
        Component titulo = Component.translatable(MOD + ".torneo.empieza");
        Component sub = Component.translatable(MOD + ".torneo.empieza_sub", Component.translatable(MOD + ".torneo.modo." + modo), minutos, quien);
        for (ServerPlayer o : s.getPlayerList().getPlayers()) {
            Fx.title(o, titulo, sub, 0x6FB7FF);
            o.level().playSound(null, o.getX(), o.getY(), o.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 0.8F);
        }
        PescaColeccion.LOGGER.info("PESCA_TORNEO_EMPIEZA modo={} minutos={} por={}", modo, minutos, quien);
        enviar(s);
        return true;
    }

    /** Se sacó un pez: si hay torneo, cuenta. */
    public static void alPescar(ServerPlayer p, Especie e, float cm, boolean perfecto) {
        if (!activo || Plat.server(p) != servidor) {
            return;
        }
        Fila f = TABLA.computeIfAbsent(p.getUUID(), k -> new Fila(k, p.getGameProfile().getName()));
        f.peces++;
        String nombre = Component.translatable("item." + MOD + "." + e.itemId()).getString();
        switch (modo) {
            case "grande" -> {
                float rel = Math.max(0, Math.min(100, (cm - e.cmMin()) / Math.max(1, e.cmMax() - e.cmMin()) * 100));
                if (rel >= f.puntos) {
                    f.puntos = rel;
                    f.detalle = nombre + String.format(Locale.ROOT, cm >= 100 ? " %.0f cm" : " %.1f cm", cm);
                }
            }
            case "puntos" -> {
                float pts = PUNTOS[Math.max(0, Math.min(4, e.rareza()))] * (perfecto ? 1.5F : 1F);
                f.puntos += pts;
                f.detalle = nombre;
            }
            default -> {
                f.puntos = f.peces;
                f.detalle = nombre;
            }
        }
        enviar(Plat.server(p));
    }

    private static void tick(MinecraftServer s) {
        if (!activo) {
            return;
        }
        if (s != servidor) {
            // otro mundo: el torneo de antes ya no cuenta
            activo = false;
            TABLA.clear();
            return;
        }
        long left = finTick - s.getTickCount();
        if (left <= 0) {
            terminar(s);
            return;
        }
        if (left % 20 == 0) {
            long seg = left / 20;
            if (seg == 60 || seg == 30 || seg <= 5) {
                for (ServerPlayer o : s.getPlayerList().getPlayers()) {
                    Plat.msg(o, Component.translatable(MOD + ".torneo.quedan", seg), true);
                    if (seg <= 5) {
                        o.level().playSound(null, o.getX(), o.getY(), o.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
                    }
                }
            }
        }
    }

    /** La tabla ordenada (de mejor a peor), solo quienes pescaron algo. */
    static List<Fila> orden() {
        List<Fila> l = new ArrayList<>();
        for (Fila f : TABLA.values()) {
            if (f.peces > 0) {
                l.add(f);
            }
        }
        l.sort((a, b) -> Float.compare(b.puntos, a.puntos));
        return l;
    }

    static String marca(Fila f) {
        return switch (modo) {
            case "grande" -> String.format(Locale.ROOT, "%.0f%%", f.puntos);
            case "puntos" -> f.puntos == Math.floor(f.puntos) ? String.valueOf((int) f.puntos) : String.format(Locale.ROOT, "%.1f", f.puntos);
            default -> String.valueOf(f.peces);
        };
    }

    /** Termina el torneo: copas, esmeraldas, amistad con Nilo y el aviso para todos. */
    public static void terminar(MinecraftServer s) {
        if (!activo) {
            return;
        }
        activo = false;
        List<Fila> l = orden();
        long dia = Plat.dayTime(s.overworld()) / 24000L;
        int jugadores = l.size();
        for (int i = 0; i < l.size(); i++) {
            Fila f = l.get(i);
            ServerPlayer p = s.getPlayerList().getPlayer(f.id);
            if (p == null) {
                continue;
            }
            if (i < 3) {
                Plat.give(p, Copas.crear(i, f.nombre, modo, marca(f) + (f.detalle.isEmpty() || !modo.equals("grande") ? "" : " · " + f.detalle), dia,
                        jugadores));
                if (jugadores >= 2) {
                    Plat.give(p, new ItemStack(Items.EMERALD, ESMERALDAS[i]));
                }
            }
            CompoundTag nd = Encargos.datos(p);
            Encargos.sumarAmistad(p, nd, 2);
            PlayerStore.save(p, MOD);
        }
        Component titulo = Component.translatable(MOD + ".torneo.fin");
        Component sub = l.isEmpty() ? Component.translatable(MOD + ".torneo.nadie")
                : Component.translatable(MOD + ".torneo.gana", l.get(0).nombre, marca(l.get(0)));
        for (ServerPlayer o : s.getPlayerList().getPlayers()) {
            Fx.title(o, titulo, sub, 0xFFD24A);
            o.level().playSound(null, o.getX(), o.getY(), o.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7F, 1F);
            for (int i = 0; i < Math.min(3, l.size()); i++) {
                o.sendSystemMessage(Component.translatable(MOD + ".torneo.puesto", i + 1, l.get(i).nombre, marca(l.get(i))));
            }
        }
        // el último torneo queda anotado en el mundo
        CompoundTag g = WorldStore.get(s, MOD).global();
        CompoundTag u = new CompoundTag();
        u.putString("modo", modo);
        u.putInt("jugadores", jugadores);
        u.putLong("dia", dia);
        if (!l.isEmpty()) {
            u.putString("ganador", l.get(0).nombre);
            u.putString("marca", marca(l.get(0)));
        }
        g.put("torneo", u);
        WorldStore.get(s, MOD).dirty();
        PescaColeccion.LOGGER.info("PESCA_TORNEO_FIN modo={} jugadores={} ganador={}", modo, jugadores, l.isEmpty() ? "-" : l.get(0).nombre);
        enviar(s);
    }

    /** Lo que ven los clientes: si hay torneo, modo, segundos que quedan, quién lo empezó, la tabla y el último. */
    static CompoundTag estado(MinecraftServer s) {
        CompoundTag t = new CompoundTag();
        boolean on = activoEn(s);
        t.putBoolean("activo", on);
        t.putString("modo", modo);
        t.putLong("resta", on ? Math.max(0, (finTick - s.getTickCount()) / 20) : 0);
        t.putString("quien", quien);
        ListTag tabla = new ListTag();
        for (Fila f : on ? orden() : List.<Fila>of()) {
            CompoundTag r = new CompoundTag();
            r.putString("n", f.nombre);
            r.putString("p", marca(f));
            r.putString("d", f.detalle);
            Nbt.putUuid(r, "id", f.id);
            tabla.add(r);
        }
        t.put("tabla", tabla);
        t.put("ultimo", Nbt.c(WorldStore.get(s, MOD).global(), "torneo").copy());
        return t;
    }

    static void enviar(MinecraftServer s) {
        PlayerStore.Sync.toAll(s, KEY, estado(s));
    }
}
