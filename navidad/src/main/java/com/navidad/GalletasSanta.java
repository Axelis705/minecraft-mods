package com.navidad;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.WorldStore;
import com.navidad.bloque.CajaRegalo;
import com.navidad.bloque.CajaRegaloBE;
import com.navidad.bloque.deco.Deco;
import com.navidad.bloque.deco.DecoBlock;
import com.navidad.objeto.Premios;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import java.util.UUID;

/**
 * Galletas y leche para Santa: en temporada, si pones el adorno de galletas y leche, esa noche (a medianoche, o a la media hora de verdad si
 * el tiempo está detenido) Santa pasa, se las come (queda el plato con migajas y una notita) y te deja junto una caja de regalo a tu
 * nombre. Una vez por noche por persona.
 */
public final class GalletasSanta {
    private static final long ESPERA_REAL = 30L * 60L * 1000L;

    private GalletasSanta() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(GalletasSanta::alPoner);
        AxelisCore.onServerTick(s -> {
            if (s.getTickCount() % 100 == 61) {
                revisar(s, false);
            }
        });
    }

    static void alPoner(BlockEvent.EntityPlaceEvent e) {
        Block g = Deco.block("galletas_leche");
        if (g == null || e.getPlacedBlock().getBlock() != g || !(e.getEntity() instanceof ServerPlayer p) || !(e.getLevel() instanceof Level l)) {
            return;
        }
        MinecraftServer s = p.server;
        if (!Temporada.activa(s)) {
            return;
        }
        WorldStore w = WorldStore.get(s, Navidad.MODID);
        ListTag lista = w.global().getList("galletas", Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            if (lista.getCompound(i).getUUID("quien").equals(p.getUUID())) {
                p.displayClientMessage(Component.translatable("navidad.galletas.ya").withStyle(ChatFormatting.GRAY), true);
                return;
            }
        }
        CompoundTag c = new CompoundTag();
        c.putUUID("quien", p.getUUID());
        c.putString("nombre", p.getGameProfile().getName());
        c.putString("mundo", l.dimension().location().toString());
        c.putInt("x", e.getPos().getX());
        c.putInt("y", e.getPos().getY());
        c.putInt("z", e.getPos().getZ());
        c.putLong("dia", l.getDayTime() / 24000L);
        c.putLong("puesto", l.getDayTime());
        c.putLong("cuando", System.currentTimeMillis());
        lista.add(c);
        w.global().put("galletas", lista);
        w.dirty();
        p.displayClientMessage(Component.translatable("navidad.galletas.puestas").withStyle(ChatFormatting.GOLD), true);
        Navidad.LOGGER.info("NAVIDAD_GALLETAS puestas {} en {}", p.getGameProfile().getName(), e.getPos().toShortString());
    }

    /** Santa pasa por las galletas que ya toca (o por todas, con {@code ya}). Devuelve cuántas se comió. */
    public static int revisar(MinecraftServer s, boolean ya) {
        WorldStore w = WorldStore.get(s, Navidad.MODID);
        ListTag lista = w.global().getList("galletas", Tag.TAG_COMPOUND);
        if (lista.isEmpty()) {
            return 0;
        }
        int comidas = 0;
        Block g = Deco.block("galletas_leche");
        for (int i = lista.size() - 1; i >= 0; i--) {
            CompoundTag c = lista.getCompound(i);
            ServerLevel l = s.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(c.getString("mundo"))));
            BlockPos pos = new BlockPos(c.getInt("x"), c.getInt("y"), c.getInt("z"));
            if (l == null) {
                lista.remove(i);
                continue;
            }
            if (!l.isLoaded(pos)) {
                continue;
            }
            BlockState st = l.getBlockState(pos);
            if (g == null || st.getBlock() != g) {
                lista.remove(i);    // las quitaron (o se las comió alguien más)
                continue;
            }
            long hora = l.getDayTime() % 24000L;
            // pasa ya de noche (de medianoche al amanecer, o si ya amaneció otro día), y no enfrente de ti recién puestas
            boolean noche = l.getDayTime() - c.getLong("puesto") >= 1000L && (l.getDayTime() / 24000L > c.getLong("dia") || hora >= 18000L);
            boolean tarde = System.currentTimeMillis() - c.getLong("cuando") > ESPERA_REAL;
            if (!ya && !noche && !tarde) {
                continue;
            }
            comer(l, pos, st, c);
            lista.remove(i);
            comidas++;
        }
        w.global().put("galletas", lista);
        w.dirty();
        return comidas;
    }

    private static void comer(ServerLevel l, BlockPos pos, BlockState st, CompoundTag c) {
        RandomSource r = l.getRandom();
        Block plato = Deco.block("plato_migajas");
        Direction f = st.hasProperty(DecoBlock.FACING) ? st.getValue(DecoBlock.FACING) : Direction.NORTH;
        if (plato != null) {
            BlockState ps = plato.defaultBlockState();
            if (ps.hasProperty(DecoBlock.FACING)) {
                ps = ps.setValue(DecoBlock.FACING, f);
            }
            l.setBlock(pos, ps, 3);
        }
        // la caja de regalo, en un lugar libre junto al plato
        String nombre = c.getString("nombre");
        BlockPos caja = null;
        for (Direction d : new Direction[]{f.getClockWise(), f.getCounterClockWise(), f, f.getOpposite()}) {
            BlockPos q = pos.relative(d);
            if (l.getBlockState(q).canBeReplaced() && l.getFluidState(q).isEmpty() && l.getBlockState(q.below()).isFaceSturdy(l, q.below(), Direction.UP)) {
                caja = q;
                break;
            }
        }
        var premio = Premios.sacar(r.nextInt(8) == 0 ? Premios.Rareza.EPICO : Premios.Rareza.RARO, r);
        ServerPlayer p = l.getServer().getPlayerList().getPlayer(c.getUUID("quien"));
        if (caja != null) {
            l.setBlock(caja, ModRegistry.CAJA_REGALO.get().defaultBlockState().setValue(CajaRegalo.LLENA, true), 3);
            BlockEntity be = l.getBlockEntity(caja);
            if (be instanceof CajaRegaloBE cb) {
                cb.contenido = premio;
                cb.de = "Santa";
                cb.para = nombre;
                cb.setChanged();
            }
        } else if (p != null) {
            Fiestas.darCaja(p, premio, "Santa");
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        l.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 0.5, z, 30, 0.6, 0.6, 0.6, 0.03);
        l.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 12, 0.5, 0.3, 0.5, 0.05);
        l.playSound(null, pos, ModRegistry.CASCABELES.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
        l.playSound(null, pos, ModRegistry.RISA_SANTA.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
        if (p != null) {
            Logros.dar(p, "galletas");
            p.sendSystemMessage(Component.translatable("navidad.galletas.comidas").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        Navidad.LOGGER.info("NAVIDAD_GALLETAS comidas {} caja={}", nombre, caja != null);
    }

    /** ¿Tiene alguien galletas esperando a Santa? (para la prueba) */
    public static boolean pendientes(MinecraftServer s, UUID quien) {
        ListTag lista = WorldStore.get(s, Navidad.MODID).global().getList("galletas", Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            if (lista.getCompound(i).getUUID("quien").equals(quien)) {
                return true;
            }
        }
        return false;
    }
}
