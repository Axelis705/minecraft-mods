package com.navidad;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.vestir.Vestuario;
import com.navidad.aldea.Aldea;
import com.navidad.entidad.TrineoMadera;
import com.navidad.objeto.Ropa;
import com.navidad.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Cosas del invierno que pasan solas cada tick: los patines de hielo (en el hielo vas mucho más rápido y suena el filo; fuera del hielo
 * caminas torpe, como con patines de verdad) y la carrera de trineos de la colina de la aldea (se cronometra sola de la cima al pie y se
 * guarda el récord del mundo), los ángeles de nieve y el muérdago.
 */
public final class Invierno {
    private static final UUID PATINES = UUID.fromString("6f1c1d8e-25a1-4c7e-9a0b-7d21c2b5e1a4");
    private static final ResourceLocation PRENDA = Ropa.id("patines");
    /** En el hielo: +55% de velocidad; en la tierra: -35%. */
    public static final double HIELO = 0.55, TIERRA = -0.35;

    private Invierno() {
    }

    public static void init() {
        AxelisCore.onServerTick(Invierno::tick);
    }

    static void tick(MinecraftServer s) {
        if (s.getTickCount() % 2 != 0) {
            return;
        }
        boolean temporada = Temporada.activa(s);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            patines(s, p);
            carrera(s, p);
            if (temporada) {
                angel(p);
            }
        }
        if (s.getTickCount() % 20 == 0) {
            muerdago(s);
        }
    }

    // ------------------------------------------------------------------ ángeles de nieve

    /** Cuánto lleva cada quien "acostado" en la nieve (ticks). */
    private static final Map<UUID, Integer> ANGEL = new HashMap<>();
    /** Dónde se "acostó" (si se mueve, empieza de nuevo). */
    private static final Map<UUID, double[]> DONDE = new HashMap<>();
    /** Cuántos ángeles de nieve se han hecho (para la prueba con fotos). */
    public static int angeles;

    /**
     * Agachado en la nieve mirando al cielo, sin moverte, 3 segundos: queda la huella de un ángel de nieve donde estabas (en temporada).
     */
    private static void angel(ServerPlayer p) {
        net.minecraft.world.level.block.Block a = com.navidad.bloque.deco.Deco.block("angel_nieve");
        BlockPos pies = p.blockPosition();
        BlockState aqui = p.level().getBlockState(pies), abajo = p.level().getBlockState(pies.below());
        boolean nieve = aqui.is(net.minecraft.world.level.block.Blocks.SNOW) || aqui.isAir() && abajo.is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK);
        double[] donde = DONDE.get(p.getUUID());
        boolean quieto = donde == null || Math.abs(p.getX() - donde[0]) < 0.3 && Math.abs(p.getZ() - donde[1]) < 0.3;
        if (a == null || p.isSpectator() || !p.isShiftKeyDown() || !p.onGround() || p.getXRot() > -50F || !quieto || !nieve) {
            ANGEL.remove(p.getUUID());
            DONDE.remove(p.getUUID());
            return;
        }
        if (donde == null) {
            DONDE.put(p.getUUID(), new double[]{p.getX(), p.getZ()});
        }
        int n = ANGEL.merge(p.getUUID(), 2, Integer::sum);
        var l = p.serverLevel();
        if (n % 10 == 0) {
            l.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 0.2, p.getZ(), 6, 0.6, 0.05, 0.6, 0.02);
        }
        if (n == 20) {
            p.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("navidad.angel.haciendo").withStyle(ChatFormatting.AQUA)));
        }
        if (n < 60) {
            return;
        }
        ANGEL.remove(p.getUUID());
        DONDE.remove(p.getUUID());
        l.setBlock(pies, a.defaultBlockState(), 3);
        l.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 0.3, p.getZ(), 30, 0.8, 0.2, 0.8, 0.04);
        l.playSound(null, pies, net.minecraft.sounds.SoundEvents.SNOW_PLACE, SoundSource.PLAYERS, 1F, 0.8F);
        p.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("navidad.angel.listo").withStyle(ChatFormatting.WHITE)));
        angeles++;
        Navidad.LOGGER.info("NAVIDAD_ANGEL {} {}", p.getGameProfile().getName(), pies.toShortString());
    }

    // ------------------------------------------------------------------ el muérdago

    /** Cuándo fue el último abrazo de cada pareja (para no repetirlo a cada rato). */
    private static final Map<String, Integer> ABRAZOS = new HashMap<>();

    /** ¿Hay muérdago colgando arriba de su cabeza (a 1, 2 o 3 bloques)? Devuelve dónde, o null. */
    static BlockPos muerdagoArriba(ServerPlayer p) {
        net.minecraft.world.level.block.Block m = com.navidad.bloque.deco.Deco.block("muerdago");
        if (m == null) {
            return null;
        }
        BlockPos cabeza = BlockPos.containing(p.getX(), p.getEyeY(), p.getZ());
        for (int k = 0; k <= 2; k++) {
            if (p.level().getBlockState(cabeza.above(k)).is(m)) {
                return cabeza.above(k);
            }
        }
        return null;
    }

    /** Dos personas bajo el mismo muérdago: corazones, un abrazo navideño y un poquito de regeneración. */
    private static void muerdago(MinecraftServer s) {
        var ps = s.getPlayerList().getPlayers();
        for (int i = 0; i < ps.size(); i++) {
            ServerPlayer a = ps.get(i);
            BlockPos ma = a.isSpectator() ? null : muerdagoArriba(a);
            if (ma == null) {
                continue;
            }
            for (int j = i + 1; j < ps.size(); j++) {
                ServerPlayer b = ps.get(j);
                if (b.isSpectator() || b.level() != a.level() || b.distanceTo(a) > 2.2F) {
                    continue;
                }
                BlockPos mb = muerdagoArriba(b);
                if (mb == null || mb.distManhattan(ma) > 2) {
                    continue;
                }
                String k = a.getUUID().compareTo(b.getUUID()) < 0 ? a.getUUID() + "|" + b.getUUID() : b.getUUID() + "|" + a.getUUID();
                Integer ya = ABRAZOS.get(k);
                if (ya != null && s.getTickCount() - ya < 20 * 60) {
                    continue;
                }
                ABRAZOS.put(k, s.getTickCount());
                for (ServerPlayer x : new ServerPlayer[]{a, b}) {
                    x.serverLevel().sendParticles(ParticleTypes.HEART, x.getX(), x.getY() + 2.1, x.getZ(), 5, 0.4, 0.2, 0.4, 0.02);
                    x.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100, 0));
                    x.sendSystemMessage(Component.translatable("navidad.muerdago", a.getDisplayName(), b.getDisplayName())
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                a.level().playSound(null, a.blockPosition(), ModRegistry.CASCABEL.get(), SoundSource.PLAYERS, 0.8F, 1.3F);
                Navidad.LOGGER.info("NAVIDAD_MUERDAGO {} {}", a.getGameProfile().getName(), b.getGameProfile().getName());
            }
        }
    }

    // ------------------------------------------------------------------ la carrera de trineos

    /** Cuándo arrancó cada quien (tick del servidor). */
    private static final Map<UUID, Integer> SALIDA = new HashMap<>();
    /** Dónde iba cada quien la vez pasada (z de la aldea), para saber si ya arrancó. */
    private static final Map<UUID, Double> ANTES = new HashMap<>();
    /** El último tiempo de alguien (segundos), para la prueba con fotos. */
    public static double ultimaCarrera;
    /** La prueba con fotos lo prende para dejar en el registro por dónde va el trineo. */
    public static boolean diagnostico;

    private static void carrera(MinecraftServer s, ServerPlayer p) {
        if (!(p.getVehicle() instanceof TrineoMadera t) || t.getControllingPassenger() != p) {
            SALIDA.remove(p.getUUID());
            ANTES.remove(p.getUUID());
            return;
        }
        CompoundTag g = WorldStore.get(s, Navidad.MODID).global();
        if (!g.contains("aldeaX") || p.level() != s.overworld()) {
            if (diagnostico && s.getTickCount() % 20 == 0) {
                Navidad.LOGGER.info("NAVIDAD_CARRERA sin aldea guardada ({})", g.getAllKeys());
            }
            return;
        }
        double lx = t.getX() - g.getInt("aldeaX"), lz = t.getZ() - g.getInt("aldeaZ"), ly = t.getY() - g.getInt("aldeaY");
        boolean enCarril = lx >= Aldea.CARRIL_X0 - 1 && lx <= Aldea.CARRIL_X1 + 2 && ly > -3 && ly < 10;
        if (diagnostico && s.getTickCount() % 10 == 0) {
            Navidad.LOGGER.info("NAVIDAD_CARRERA trineo ({}, {}, {}) carril={} antes={}", String.format(Locale.ROOT, "%.2f", lx),
                    String.format(Locale.ROOT, "%.2f", ly), String.format(Locale.ROOT, "%.2f", lz), enCarril, ANTES.get(p.getUUID()));
        }
        Integer ini = SALIDA.get(p.getUUID());
        Double antes = ANTES.put(p.getUUID(), lz);
        if (ini == null) {
            // arranca quien empieza a bajar estando en la cima (la plataforma de arriba y el primer tramo antes de la bajada); así cuenta
            // aunque te subas al trineo ya un poquito adelante de la orilla
            boolean enCima = antes != null && antes <= Aldea.COLINA_CIMA + 2.5 && antes >= Aldea.COLINA_CIMA - 4;
            if (enCarril && enCima && lz - antes > 0.01) {
                SALIDA.put(p.getUUID(), s.getTickCount());
                p.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("navidad.carrera.sale").withStyle(ChatFormatting.AQUA)));
                Navidad.LOGGER.info("NAVIDAD_CARRERA sale {}", p.getGameProfile().getName());
            }
            return;
        }
        double seg = (s.getTickCount() - ini) / 20.0;
        if (!enCarril || seg > 40 || lz < Aldea.COLINA_CIMA - 2) {
            SALIDA.remove(p.getUUID());
            Navidad.LOGGER.info("NAVIDAD_CARRERA se salió {} ({}, {}, {}) a los {} s", p.getGameProfile().getName(), String.format(Locale.ROOT,
                    "%.1f", lx), String.format(Locale.ROOT, "%.1f", ly), String.format(Locale.ROOT, "%.1f", lz), seg);
            return;
        }
        if (lz >= Aldea.COLINA_PIE + 1) {
            SALIDA.remove(p.getUUID());
            ultimaCarrera = seg;
            String tiempo = String.format(Locale.ROOT, "%.2f", seg);
            p.sendSystemMessage(Component.translatable("navidad.carrera.llega", tiempo).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            WorldStore w = WorldStore.get(s, Navidad.MODID);
            double rec = g.contains("colinaRecord") ? g.getDouble("colinaRecord") : 999;
            if (seg < rec) {
                g.putDouble("colinaRecord", seg);
                g.putString("colinaQuien", p.getGameProfile().getName());
                w.dirty();
                for (ServerPlayer o : s.getPlayerList().getPlayers()) {
                    o.sendSystemMessage(Component.translatable("navidad.carrera.record", p.getDisplayName(), tiempo)
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                }
                p.playNotifySound(ModRegistry.CASCABELES.get(), SoundSource.PLAYERS, 1F, 1.1F);
            } else {
                p.sendSystemMessage(Component.translatable("navidad.carrera.record_es", g.getString("colinaQuien"),
                        String.format(Locale.ROOT, "%.2f", rec)).withStyle(ChatFormatting.GRAY));
            }
            Navidad.LOGGER.info("NAVIDAD_CARRERA {} {} s", p.getGameProfile().getName(), tiempo);
        } else if (s.getTickCount() % 4 == 0) {
            p.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(String.format(Locale.ROOT, "❄ %.1f s", seg))
                    .withStyle(ChatFormatting.AQUA)));
        }
    }

    private static void patines(MinecraftServer s, ServerPlayer p) {
        AttributeInstance vel = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (vel == null) {
            return;
        }
        boolean puestos = !p.isSpectator() && !p.isPassenger() && Vestuario.tiene(s, p.getUUID(), PRENDA);
        double quiero = 0;
        boolean enHielo = false;
        if (puestos && p.onGround()) {
            BlockState b = p.level().getBlockState(BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ()));
            enHielo = b.is(BlockTags.ICE);
            quiero = enHielo ? HIELO : TIERRA;
        } else if (puestos) {
            AttributeModifier m = vel.getModifier(PATINES);
            quiero = m == null ? 0 : m.getAmount();
        }
        AttributeModifier ya = vel.getModifier(PATINES);
        if (ya == null || ya.getAmount() != quiero) {
            if (ya != null) {
                vel.removeModifier(PATINES);
            }
            if (quiero != 0) {
                vel.addTransientModifier(new AttributeModifier(PATINES, "navidad_patines", quiero, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        // el filo raspa el hielo y salta escarcha cuando vas rápido
        if (enHielo) {
            double dx = p.getX() - p.xOld, dz = p.getZ() - p.zOld;
            double rap = Math.sqrt(dx * dx + dz * dz);
            if (rap > 0.12) {
                if (p.tickCount % 12 < 2) {
                    p.level().playSound(null, p.getX(), p.getY(), p.getZ(), ModRegistry.PATIN.get(), SoundSource.PLAYERS,
                            (float) Math.min(0.8, 0.3 + rap), 0.9F + p.getRandom().nextFloat() * 0.25F);
                }
                p.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 0.05, p.getZ(), 2, 0.15, 0.02, 0.15, 0.01);
            }
        }
    }
}
