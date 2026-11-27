package com.navidad;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.WorldStore;
import com.navidad.aldea.Aldea;
import com.navidad.objeto.Premios;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Guerra de bolas de nieve ({@code /navidad guerra_nieve [segundos] [equipos]}). Sin equipos es todos contra todos: cada bolazo que le das a
 * otra persona es un punto. Con equipos, se reparten en Rojo y Verde (los nombres salen de su color), los bolazos al propio equipo no
 * cuentan y gana el equipo con más puntos; si la aldea de Santa está cerca, cada equipo aparece en su fuerte de nieve. A todos les llegan
 * bolas de nieve al empezar y cada rato. Al final se anuncia el podio y quien ganó (o todo el equipo ganador) se lleva un regalo raro.
 */
public final class GuerraNieve {
    private static int quedan = -1, duracion;
    private static boolean equipos;
    private static final Map<UUID, Integer> PUNTOS = new HashMap<>();
    private static final Map<UUID, String> NOMBRES = new HashMap<>();
    /** Con equipos: 0 = Rojo, 1 = Verde. */
    private static final Map<UUID, Integer> EQUIPO = new HashMap<>();
    /** El equipo de scoreboard que tenía cada quien antes (para devolvérselo al terminar). */
    private static final Map<String, String> ANTES = new HashMap<>();
    private static final int[] TOTAL = new int[2];
    private static final String[] EQUIPOS = {"navidad_rojo", "navidad_verde"};
    private static final ChatFormatting[] COLOR = {ChatFormatting.RED, ChatFormatting.GREEN};

    private GuerraNieve() {
    }

    public static void init() {
        AxelisCore.onServerTick(GuerraNieve::tick);
        MinecraftForge.EVENT_BUS.addListener(GuerraNieve::alPegar);
    }

    public static boolean activa() {
        return quedan > 0;
    }

    public static void empezar(MinecraftServer s, int segundos) {
        empezar(s, segundos, false);
    }

    public static void empezar(MinecraftServer s, int segundos, boolean conEquipos) {
        if (activa()) {
            quitarEquipos(s);
        }
        PUNTOS.clear();
        NOMBRES.clear();
        EQUIPO.clear();
        TOTAL[0] = TOTAL[1] = 0;
        duracion = segundos * 20;
        quedan = duracion;
        List<ServerPlayer> todos = new ArrayList<>(s.getPlayerList().getPlayers());
        equipos = conEquipos && !todos.isEmpty();
        if (equipos) {
            Collections.shuffle(todos);
            Scoreboard sb = s.getScoreboard();
            for (int i = 0; i < todos.size(); i++) {
                ServerPlayer p = todos.get(i);
                int eq = i % 2;
                EQUIPO.put(p.getUUID(), eq);
                NOMBRES.put(p.getUUID(), p.getGameProfile().getName());
                PUNTOS.put(p.getUUID(), 0);
                String nombre = p.getScoreboardName();
                PlayerTeam antes = sb.getPlayersTeam(nombre);
                if (antes != null && !antes.getName().startsWith("navidad_")) {
                    ANTES.put(nombre, antes.getName());
                }
                sb.addPlayerToTeam(nombre, equipo(sb, eq));
                alFuerte(s, p, eq);
                p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(eq == 0 ? "navidad.guerra.rojo" : "navidad.guerra.verde")
                        .withStyle(COLOR[eq], ChatFormatting.BOLD)));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("navidad.guerra.equipo_sub")
                        .withStyle(ChatFormatting.WHITE)));
            }
        }
        for (ServerPlayer p : todos) {
            p.sendSystemMessage(Component.translatable(equipos ? "navidad.guerra.empieza_equipos" : "navidad.guerra.empieza", segundos)
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            p.playNotifySound(ModRegistry.CASCABELES.get(), SoundSource.MASTER, 1F, 1.2F);
            dar(p, 16);
        }
        Navidad.LOGGER.info("NAVIDAD_GUERRA empieza {} s equipos={}", segundos, equipos);
    }

    private static PlayerTeam equipo(Scoreboard sb, int eq) {
        PlayerTeam t = sb.getPlayerTeam(EQUIPOS[eq]);
        if (t == null) {
            t = sb.addPlayerTeam(EQUIPOS[eq]);
        }
        t.setColor(COLOR[eq]);
        t.setDisplayName(Component.translatable(eq == 0 ? "navidad.guerra.rojo" : "navidad.guerra.verde"));
        return t;
    }

    /** Si la aldea de Santa está cerca (a menos de 120 bloques), cada equipo aparece detrás de su fuerte mirando al otro. */
    private static void alFuerte(MinecraftServer s, ServerPlayer p, int eq) {
        CompoundTag g = WorldStore.get(s, Navidad.MODID).global();
        ServerLevel l = s.overworld();
        if (!g.contains("aldeaX") || p.level() != l) {
            return;
        }
        int ax = g.getInt("aldeaX"), ay = g.getInt("aldeaY"), az = g.getInt("aldeaZ");
        if (p.distanceToSqr(ax, ay, az) > 120 * 120) {
            return;
        }
        int[] f = eq == 0 ? Aldea.FUERTE_ROJO : Aldea.FUERTE_VERDE;
        double x = ax + f[0] + 0.5 + (p.getRandom().nextInt(5) - 2), z = az + f[1] + 0.5;
        p.teleportTo(l, x, ay, z, eq == 0 ? 0F : 180F, 0F);
    }

    private static void dar(ServerPlayer p, int n) {
        ItemStack b = new ItemStack(Items.SNOWBALL, n);
        if (!p.getInventory().add(b)) {
            p.drop(b, false);
        }
    }

    static void alPegar(ProjectileImpactEvent e) {
        if (!activa() || !(e.getProjectile() instanceof Snowball bola) || !(bola.getOwner() instanceof ServerPlayer a)
                || !(e.getRayTraceResult() instanceof EntityHitResult eh) || !(eh.getEntity() instanceof ServerPlayer b) || a == b) {
            return;
        }
        Integer ea = EQUIPO.get(a.getUUID()), ebq = EQUIPO.get(b.getUUID());
        if (equipos && ea != null && ea.equals(ebq)) {
            a.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("navidad.guerra.mismo", b.getDisplayName())
                    .withStyle(ChatFormatting.GRAY)));
            return;
        }
        int n = PUNTOS.merge(a.getUUID(), 1, Integer::sum);
        if (equipos && ea != null) {
            TOTAL[ea]++;
        }
        NOMBRES.put(a.getUUID(), a.getGameProfile().getName());
        NOMBRES.putIfAbsent(b.getUUID(), b.getGameProfile().getName());
        PUNTOS.putIfAbsent(b.getUUID(), 0);
        b.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()), b.getX(),
                b.getEyeY(), b.getZ(), 18, 0.25, 0.25, 0.25, 0.1);
        b.setTicksFrozen(Math.min(b.getTicksRequiredToFreeze(), b.getTicksFrozen() + 40));
        Component texto = equipos && ea != null
                ? Component.translatable("navidad.guerra.punto_equipo", b.getDisplayName(), n, TOTAL[0], TOTAL[1])
                : Component.translatable("navidad.guerra.punto", b.getDisplayName(), n);
        a.connection.send(new ClientboundSetActionBarTextPacket(texto.copy().withStyle(ChatFormatting.AQUA)));
        b.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("navidad.guerra.te_dieron", a.getDisplayName())
                .withStyle(ChatFormatting.WHITE)));
        a.playNotifySound(SoundEvents.SNOW_HIT, SoundSource.PLAYERS, 1F, 1.4F);
    }

    /** El servidor que ya revisamos (si se cerró a media guerra, quedan los equipos de colores guardados en el mundo: se quitan). */
    private static MinecraftServer visto;

    static void tick(MinecraftServer s) {
        if (visto != s) {
            visto = s;
            if (!activa()) {
                quitarEquipos(s);
            }
        }
        if (quedan <= 0) {
            return;
        }
        quedan--;
        int t = duracion - quedan;
        if (t % 300 == 0) {
            for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                dar(p, 8);
            }
        }
        if (quedan == 200 || quedan == 600) {
            for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                p.sendSystemMessage(Component.translatable("navidad.guerra.faltan", quedan / 20).withStyle(ChatFormatting.AQUA));
            }
        }
        if (quedan == 0) {
            terminar(s);
        }
    }

    private static void terminar(MinecraftServer s) {
        List<Map.Entry<UUID, Integer>> orden = new ArrayList<>(PUNTOS.entrySet());
        orden.sort((x, y) -> y.getValue() - x.getValue());
        List<ServerPlayer> todos = s.getPlayerList().getPlayers();
        int gana = !equipos ? -1 : TOTAL[0] > TOTAL[1] ? 0 : TOTAL[1] > TOTAL[0] ? 1 : 2;
        for (ServerPlayer p : todos) {
            p.sendSystemMessage(Component.translatable("navidad.guerra.fin").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            if (equipos) {
                p.sendSystemMessage(Component.translatable("navidad.guerra.marcador", TOTAL[0], TOTAL[1]).withStyle(ChatFormatting.WHITE));
                p.sendSystemMessage((gana == 2 ? Component.translatable("navidad.guerra.empate")
                        : Component.translatable("navidad.guerra.gana_equipo", Component.translatable(gana == 0 ? "navidad.guerra.rojo"
                        : "navidad.guerra.verde"))).withStyle(gana == 2 ? ChatFormatting.YELLOW : COLOR[gana], ChatFormatting.BOLD));
            }
            String[] medallas = {"★ 1º", "2º", "3º"};
            for (int i = 0; i < Math.min(3, orden.size()); i++) {
                var en = orden.get(i);
                p.sendSystemMessage(Component.literal(medallas[i] + " " + NOMBRES.getOrDefault(en.getKey(), "?") + ": " + en.getValue())
                        .withStyle(i == 0 ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
            }
            if (orden.isEmpty() || orden.get(0).getValue() == 0) {
                p.sendSystemMessage(Component.translatable("navidad.guerra.nadie").withStyle(ChatFormatting.GRAY));
            }
        }
        if (equipos && gana < 2) {
            for (Map.Entry<UUID, Integer> en : EQUIPO.entrySet()) {
                ServerPlayer g = s.getPlayerList().getPlayer(en.getKey());
                if (g != null && en.getValue() == gana) {
                    Logros.dar(g, "guerra");
                    Fiestas.darCaja(g, Premios.sacar(Premios.Rareza.RARO, g.getRandom()), "Santa");
                }
            }
        } else if (!equipos && !orden.isEmpty() && orden.get(0).getValue() > 0) {
            ServerPlayer g = s.getPlayerList().getPlayer(orden.get(0).getKey());
            if (g != null) {
                Logros.dar(g, "guerra");
                Fiestas.darCaja(g, Premios.sacar(Premios.Rareza.RARO, g.getRandom()), "Santa");
            }
        }
        Navidad.LOGGER.info("NAVIDAD_GUERRA fin puntos={} equipos={} rojo={} verde={}", PUNTOS.values(), equipos, TOTAL[0], TOTAL[1]);
        quitarEquipos(s);
        quedan = -1;
    }

    /** Devuelve a cada quien a su equipo de antes y borra los equipos de la guerra. */
    private static void quitarEquipos(MinecraftServer s) {
        Scoreboard sb = s.getScoreboard();
        for (String n : EQUIPOS) {
            PlayerTeam t = sb.getPlayerTeam(n);
            if (t != null) {
                sb.removePlayerTeam(t);
            }
        }
        for (Map.Entry<String, String> en : ANTES.entrySet()) {
            PlayerTeam t = sb.getPlayerTeam(en.getValue());
            if (t != null) {
                sb.addPlayerToTeam(en.getKey(), t);
            }
        }
        ANTES.clear();
    }
}
