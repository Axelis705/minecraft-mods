package com.navidad;

import com.axeliscore.AxelisCore;
import com.navidad.objeto.Premios;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Guerra de bolas de nieve ({@code /navidad guerra_nieve [segundos]}): todos contra todos. Cada bolazo que le das a otra persona es un
 * punto; a todos les llegan bolas de nieve al empezar y cada rato. Al final se anuncia el podio y quien ganó se lleva un regalo raro.
 */
public final class GuerraNieve {
    private static int quedan = -1, duracion;
    private static final Map<UUID, Integer> PUNTOS = new HashMap<>();
    private static final Map<UUID, String> NOMBRES = new HashMap<>();

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
        PUNTOS.clear();
        NOMBRES.clear();
        duracion = segundos * 20;
        quedan = duracion;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.translatable("navidad.guerra.empieza", segundos).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            p.playNotifySound(ModRegistry.CASCABELES.get(), SoundSource.MASTER, 1F, 1.2F);
            dar(p, 16);
        }
        Navidad.LOGGER.info("NAVIDAD_GUERRA empieza {} s", segundos);
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
        int n = PUNTOS.merge(a.getUUID(), 1, Integer::sum);
        NOMBRES.put(a.getUUID(), a.getGameProfile().getName());
        NOMBRES.putIfAbsent(b.getUUID(), b.getGameProfile().getName());
        PUNTOS.putIfAbsent(b.getUUID(), 0);
        b.serverLevel().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()), b.getX(),
                b.getEyeY(), b.getZ(), 18, 0.25, 0.25, 0.25, 0.1);
        b.setTicksFrozen(Math.min(b.getTicksRequiredToFreeze(), b.getTicksFrozen() + 40));
        a.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("navidad.guerra.punto", b.getDisplayName(), n)
                .withStyle(ChatFormatting.AQUA)));
        b.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("navidad.guerra.te_dieron", a.getDisplayName())
                .withStyle(ChatFormatting.WHITE)));
        a.playNotifySound(SoundEvents.SNOW_HIT, SoundSource.PLAYERS, 1F, 1.4F);
    }

    static void tick(MinecraftServer s) {
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
        for (ServerPlayer p : todos) {
            p.sendSystemMessage(Component.translatable("navidad.guerra.fin").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
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
        if (!orden.isEmpty() && orden.get(0).getValue() > 0) {
            ServerPlayer g = s.getPlayerList().getPlayer(orden.get(0).getKey());
            if (g != null) {
                Fiestas.darCaja(g, Premios.sacar(Premios.Rareza.RARO, g.getRandom()), "Santa");
            }
        }
        Navidad.LOGGER.info("NAVIDAD_GUERRA fin puntos={}", PUNTOS.values());
        quedan = -1;
    }
}
