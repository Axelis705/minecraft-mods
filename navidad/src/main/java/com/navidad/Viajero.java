package com.navidad;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.npc.Npc;
import com.navidad.entidad.Reno;
import com.navidad.gente.Gente;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

import java.util.List;
import java.util.UUID;

/**
 * El duende viajero: como el comerciante errante, pero navideño. Cada dos o tres días del juego, de día, puede llegar cerca de alguien que
 * esté afuera, con su reno Cometa amarrado. Vende cosas que no encuentras en otro lado (trineo, patines, árbol dorado, discos, traje de
 * Santa, regalos misteriosos...) por esmeraldas, y al día siguiente se va en una nube de nieve. Si su lugar quedó lejos y sin cargar,
 * al volver a cargarse ya no está.
 */
public final class Viajero {
    /** Para la prueba con fotos: cuántas veces ha llegado. */
    public static int llegadas;

    private Viajero() {
    }

    public static void init() {
        AxelisCore.onServerTick(s -> {
            if (s.getTickCount() % 600 == 311) {
                revisar(s);
            }
        });
        MinecraftForge.EVENT_BUS.addListener(Viajero::alCargar);
    }

    private static CompoundTag datos(MinecraftServer s) {
        return WorldStore.get(s, Navidad.MODID).global();
    }

    static void revisar(MinecraftServer s) {
        ServerLevel l = s.overworld();
        CompoundTag g = datos(s);
        long ahora = l.getGameTime();
        if (g.hasUUID("viajero")) {
            if (ahora > g.getLong("viajeroHasta")) {
                irse(s, l);
                return;
            }
            Entity v = l.getEntity(g.getUUID("viajero"));
            if (v != null) {
                for (ServerPlayer p : l.players()) {
                    if (p.distanceToSqr(v) < 10 * 10) {
                        Logros.dar(p, "viajero");
                    }
                }
            }
            return;
        }
        if (!Fiestas.automatico || !Temporada.activa(s) || ahora < g.getLong("viajeroProx")) {
            return;
        }
        long hora = l.getDayTime() % 24000L;
        if (hora < 1000 || hora > 10000 || l.random.nextInt(4) != 0) {
            return;
        }
        List<ServerPlayer> ps = l.players().stream().filter(p -> !p.isSpectator() && l.canSeeSky(p.blockPosition().above())).toList();
        if (ps.isEmpty()) {
            return;
        }
        llegar(l, ps.get(l.random.nextInt(ps.size())));
    }

    /** Llega cerca de {@code p}. Devuelve false si no encontró dónde pararse. */
    public static boolean llegar(ServerLevel l, ServerPlayer p) {
        MinecraftServer s = l.getServer();
        CompoundTag g = datos(s);
        if (g.hasUUID("viajero")) {
            irse(s, l);
        }
        for (int intento = 0; intento < 12; intento++) {
            double a = l.random.nextDouble() * Math.PI * 2, d = 9 + l.random.nextInt(6);
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (Math.abs(y - p.getY()) > 6 || !l.getFluidState(pos.below()).isEmpty() || !l.getBlockState(pos).canBeReplaced()) {
                continue;
            }
            Npc n = Gente.poner(l, Gente.VIAJERO, pos);
            if (n == null) {
                return false;
            }
            n.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
            Reno r = ModRegistry.RENO.get().create(l);
            if (r != null) {
                r.moveTo(x + 1.5, y, z + 0.5, l.random.nextFloat() * 360F, 0);
                r.setCustomName(Component.literal("Cometa"));
                r.addTag("navidad_viajero");
                r.setPersistenceRequired();
                l.addFreshEntity(r);
                r.setLeashedTo(n, true);
                g.putUUID("viajeroReno", r.getUUID());
            }
            g.putUUID("viajero", n.getUUID());
            g.putLong("viajeroHasta", l.getGameTime() + 24000L);
            WorldStore.get(s, Navidad.MODID).dirty();
            l.sendParticles(ParticleTypes.SNOWFLAKE, x + 0.5, y + 1, z + 0.5, 40, 0.8, 1.0, 0.8, 0.05);
            l.playSound(null, pos, ModRegistry.CASCABELES.get(), SoundSource.NEUTRAL, 1.5F, 1F);
            for (ServerPlayer q : l.players()) {
                if (q.distanceToSqr(n) < 64 * 64) {
                    q.sendSystemMessage(Component.translatable("navidad.viajero.llega").withStyle(ChatFormatting.GREEN));
                }
            }
            llegadas++;
            Navidad.LOGGER.info("NAVIDAD_VIAJERO llega cerca de {} en {}", p.getGameProfile().getName(), pos.toShortString());
            return true;
        }
        return false;
    }

    /** Se va (con su reno) en una nube de nieve. */
    static void irse(MinecraftServer s, ServerLevel l) {
        CompoundTag g = datos(s);
        for (String k : new String[]{"viajero", "viajeroReno"}) {
            if (g.hasUUID(k)) {
                Entity e = l.getEntity(g.getUUID(k));
                if (e != null) {
                    l.sendParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + 1, e.getZ(), 30, 0.6, 0.8, 0.6, 0.05);
                    l.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 0.5, e.getZ(), 10, 0.4, 0.4, 0.4, 0.02);
                    if (k.equals("viajero")) {
                        for (ServerPlayer q : l.players()) {
                            if (q.distanceToSqr(e) < 48 * 48) {
                                q.sendSystemMessage(Component.translatable("navidad.viajero.se_va").withStyle(ChatFormatting.GRAY));
                            }
                        }
                    }
                    e.discard();
                }
                g.remove(k);
            }
        }
        g.putLong("viajeroProx", l.getGameTime() + 48000L + l.random.nextInt(24000));
        WorldStore.get(s, Navidad.MODID).dirty();
        Navidad.LOGGER.info("NAVIDAD_VIAJERO se va");
    }

    /** Si se carga un duende viajero (o su reno) que ya no es el de ahora, se va. */
    static void alCargar(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || e.loadedFromDisk() == false) {
            return;
        }
        Entity en = e.getEntity();
        boolean esViajero = en instanceof Npc n && n.def() == Gente.VIAJERO;
        boolean esReno = en.getTags().contains("navidad_viajero");
        if (!esViajero && !esReno) {
            return;
        }
        MinecraftServer s = e.getLevel().getServer();
        if (s == null) {
            return;
        }
        CompoundTag g = datos(s);
        String k = esViajero ? "viajero" : "viajeroReno";
        if (!g.hasUUID(k) || !g.getUUID(k).equals(en.getUUID())) {
            e.setCanceled(true);
        }
    }

    /** ¿Este UUID es el duende viajero de ahora? */
    public static boolean esElDeAhora(MinecraftServer s, UUID u) {
        CompoundTag g = datos(s);
        return g.hasUUID("viajero") && g.getUUID("viajero").equals(u);
    }
}
