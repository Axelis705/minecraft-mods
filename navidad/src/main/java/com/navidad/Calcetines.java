package com.navidad;

import com.axeliscore.data.WorldStore;
import com.navidad.bloque.deco.Deco;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Los calcetines navideños (rojo, verde y de rayas) que cuelgas en la chimenea: en temporada, Santa los llena cada noche. Con clic derecho
 * sacas lo de hoy (dulces, bastones, galletas, fruta y a veces un regalo misterioso), una vez por día del juego (o cada 20 horas de verdad
 * si el tiempo está detenido). Si desde la última vez lastimaste aldeanos, gólems, gatos o renos, solo hay carbón. Agachado se coloca
 * un bloque normal junto al calcetín.
 */
public final class Calcetines {
    private static final String[] IDS = {"calcetin_rojo", "calcetin_verde", "calcetin_rayas"};
    private static final long ESPERA_REAL = 20L * 60L * 60L * 1000L;
    /** Lo último que sacó alguien (para la prueba con fotos). */
    public static ItemStack ultimo = ItemStack.EMPTY;

    private Calcetines() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(Calcetines::alUsar);
    }

    public static boolean esCalcetin(Block b) {
        for (String id : IDS) {
            if (Deco.block(id) == b) {
                return true;
            }
        }
        return false;
    }

    static void alUsar(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || e.getEntity().isSecondaryUseActive()
                || !esCalcetin(e.getLevel().getBlockState(e.getPos()).getBlock())) {
            return;
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (e.getEntity() instanceof ServerPlayer p) {
            sacar(p, e.getPos());
        }
    }

    /** Saca lo del calcetín (si Santa ya lo llenó). Devuelve true si había algo. */
    public static boolean sacar(ServerPlayer p, BlockPos pos) {
        MinecraftServer s = p.server;
        if (!Temporada.activa(s)) {
            p.displayClientMessage(Component.translatable("navidad.calcetin.fuera").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        WorldStore w = WorldStore.get(s, Navidad.MODID);
        CompoundTag yo = w.player(p.getUUID());
        long dia = s.overworld().getDayTime() / 24000L;
        long ahora = System.currentTimeMillis();
        boolean nuevo = !yo.contains("calcetinDia") || dia > yo.getLong("calcetinDia") || ahora - yo.getLong("calcetinCuando") > ESPERA_REAL;
        if (!nuevo) {
            p.displayClientMessage(Component.translatable("navidad.calcetin.vacio").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        int malos = Cartas.malos(p);
        boolean carbon = yo.contains("calcetinMalos") && malos > yo.getInt("calcetinMalos");
        yo.putLong("calcetinDia", dia);
        yo.putLong("calcetinCuando", ahora);
        yo.putInt("calcetinMalos", malos);
        w.dirty();
        RandomSource r = p.getRandom();
        List<ItemStack> cosas = new ArrayList<>();
        if (carbon) {
            cosas.add(new ItemStack(Items.COAL, 1 + r.nextInt(3)));
        } else {
            cosas.add(new ItemStack(ModRegistry.DULCE.get(), 2 + r.nextInt(4)));
            cosas.add(new ItemStack(ModRegistry.BASTON_CARAMELO.get(), 1 + r.nextInt(2)));
            Item[] uno = {ModRegistry.GALLETA_JENGIBRE.get(), ModRegistry.MANDARINA.get(), ModRegistry.TEJOCOTE.get(), ModRegistry.CANA.get()};
            cosas.add(new ItemStack(uno[r.nextInt(uno.length)], 1 + r.nextInt(3)));
            if (r.nextInt(6) == 0) {
                String[] adornos = {"adorno_esfera_roja", "adorno_esfera_azul", "adorno_esfera_dorada", "adorno_baston", "adorno_campanita"};
                cosas.add(new ItemStack(ModRegistry.ADORNOS.get(adornos[r.nextInt(adornos.length)]).get()));
            }
        }
        for (ItemStack c : cosas) {
            ultimo = c.copy();
            if (!p.getInventory().add(c)) {
                p.drop(c, false);
            }
        }
        ServerLevel l = p.serverLevel();
        double x = pos.getX() + 0.5, y = pos.getY() + 0.6, z = pos.getZ() + 0.5;
        if (carbon) {
            l.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 12, 0.2, 0.2, 0.2, 0.02);
            l.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.8F, 0.7F);
            p.displayClientMessage(Component.translatable("navidad.calcetin.carbon").withStyle(ChatFormatting.DARK_GRAY), true);
        } else {
            l.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 14, 0.3, 0.3, 0.3, 0.05);
            l.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 0.3, z, 10, 0.3, 0.3, 0.3, 0.02);
            l.playSound(null, pos, ModRegistry.CASCABEL.get(), SoundSource.PLAYERS, 0.8F, 1.3F);
            p.displayClientMessage(Component.translatable("navidad.calcetin.lleno").withStyle(ChatFormatting.GOLD), true);
        }
        Navidad.LOGGER.info("NAVIDAD_CALCETIN {} carbon={} cosas={}", p.getGameProfile().getName(), carbon, cosas.size());
        if (!carbon) {
            Logros.dar(p, "calcetin");
        }
        return true;
    }
}
