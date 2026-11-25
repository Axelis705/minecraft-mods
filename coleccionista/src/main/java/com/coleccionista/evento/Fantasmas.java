package com.coleccionista.evento;

import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;

import java.util.Locale;
import java.util.Set;

/**
 * Los eliminados quedan como fantasmas: flotan y ven todo (modo espectador), pero no tocan nada, los vivos no los ven y no les pueden
 * decir nada (ni por el chat ni por mensajes privados; en la voz van a su propio canal). Así nadie ayuda a nadie desde afuera.
 */
public final class Fantasmas {
    private static final String MOD = Coleccionista.MODID;
    /** Comandos con los que un fantasma podría pasarle información a un vivo. */
    private static final Set<String> BLOQUEADOS = Set.of("msg", "tell", "w", "me", "teammsg", "tm", "say", "tellraw", "title");

    private Fantasmas() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(Fantasmas::chat);
        MinecraftForge.EVENT_BUS.addListener(Fantasmas::comando);
        com.axeliscore.AxelisCore.onServerTick(Fantasmas::rondar);
    }

    // ------------------------------------------------------------------ los fantasmas no se salen del mapa

    /** Qué tan lejos del vivo más cercano puede andar un fantasma (bloques). */
    public static double radio = 14;
    /** El último lugar bueno de cada fantasma (fuera de las paredes). */
    private static final java.util.Map<java.util.UUID, net.minecraft.world.phys.Vec3> BUENO = new java.util.HashMap<>();

    /**
     * Cada tick: un fantasma no atraviesa paredes (si se mete en un bloque o lo cruza, regresa a donde estaba) y no se aleja del grupo (si
     * no hay ningún vivo cerca, aparece junto al vivo más cercano). Así sigue al grupo solo cuando los pasan a otra sala y no ve el mundo
     * vacío de afuera.
     */
    private static void rondar(MinecraftServer s) {
        Evento ev = Evento.get(s);
        if (ev.piezas.isEmpty()) {
            BUENO.clear();
            return;
        }
        long now = s.overworld().getGameTime();
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            Pieza p = ev.pieza(sp);
            if (p == null || p.vivo() || !sp.isSpectator() || sp.getCamera() != sp) {
                BUENO.remove(sp.getUUID());
                continue;
            }
            ServerLevel l = sp.serverLevel();
            net.minecraft.world.phys.Vec3 aqui = sp.position(), antes = BUENO.get(sp.getUUID());
            boolean dentro = metido(l, sp, aqui);
            if (antes != null) {
                boolean salto = aqui.distanceTo(antes) > 6;
                boolean cruzo = !salto && cruza(l, sp, antes, aqui);
                if (dentro || cruzo) {
                    sp.teleportTo(l, antes.x, antes.y, antes.z, sp.getYRot(), sp.getXRot());
                    sp.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    continue;
                }
            }
            if (!dentro) {
                BUENO.put(sp.getUUID(), aqui);
            }
            // lejos de todos los vivos: aparece detrás del más cercano
            if (now % 10 == 0) {
                Entity cerca = null;
                double dm = Double.MAX_VALUE;
                for (Pieza v : ev.vivos()) {
                    Entity e = Evento.entidad(s, v);
                    if (e != null && e.level() == l) {
                        double d = e.distanceToSqr(sp);
                        if (d < dm) {
                            dm = d;
                            cerca = e;
                        }
                    }
                }
                if (cerca != null && dm > radio * radio) {
                    double r = Math.toRadians(cerca.getYRot());
                    net.minecraft.world.phys.Vec3 a = cerca.position().add(Math.sin(r) * 2, 0.4, -Math.cos(r) * 2);
                    if (metido(l, sp, a)) {
                        a = cerca.position().add(0, 0.1, 0);
                    }
                    sp.teleportTo(l, a.x, a.y, a.z, cerca.getYRot(), 10F);
                    BUENO.put(sp.getUUID(), a);
                }
            }
        }
    }

    /** ¿Ese lugar (los pies o los ojos del fantasma) está dentro de un bloque sólido? */
    private static boolean metido(ServerLevel l, ServerPlayer sp, net.minecraft.world.phys.Vec3 pies) {
        for (double dy : new double[] {0.1, sp.getEyeHeight()}) {
            net.minecraft.core.BlockPos b = net.minecraft.core.BlockPos.containing(pies.x, pies.y + dy, pies.z);
            if (!l.getBlockState(b).getCollisionShape(l, b).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** ¿Entre esos dos lugares hay una pared (a la altura de los ojos o de los pies)? */
    private static boolean cruza(ServerLevel l, ServerPlayer sp, net.minecraft.world.phys.Vec3 a, net.minecraft.world.phys.Vec3 b) {
        for (double dy : new double[] {0.2, sp.getEyeHeight()}) {
            net.minecraft.world.phys.Vec3 x = a.add(0, dy, 0), y = b.add(0, dy, 0);
            if (x.distanceToSqr(y) < 1.0E-6) {
                continue;
            }
            net.minecraft.world.phys.BlockHitResult h = l.clip(new net.minecraft.world.level.ClipContext(x, y,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, sp));
            if (h.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    /** La pieza ya es fantasma: el jugador pasa a espectador; el bot se desvanece. */
    static void convertir(MinecraftServer s, Evento ev, Pieza p) {
        Entity e = Evento.entidad(s, p);
        if (e != null) {
            Cuerpos.dejar(e, p);
        }
        if (e instanceof ServerPlayer sp) {
            sp.setGameMode(GameType.SPECTATOR);
            Plat.msg(sp, Component.translatable(MOD + ".fantasma.eres").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        } else if (e instanceof com.coleccionista.bot.PiezaBot b) {
            if (e.level() instanceof ServerLevel l) {
                l.sendParticles(ParticleTypes.SOUL, e.getX(), e.getY() + 1.0, e.getZ(), 24, 0.3, 0.6, 0.3, 0.02);
                l.sendParticles(ParticleTypes.LARGE_SMOKE, e.getX(), e.getY() + 0.8, e.getZ(), 10, 0.25, 0.4, 0.25, 0.01);
            }
            b.fantasma(true);
        }
        com.coleccionista.voz.Voz.actualizar(s);
        if (e instanceof ServerPlayer sp && com.coleccionista.voz.VozPuente.disponible) {
            Plat.msg(sp, Component.translatable(MOD + ".fantasma.voz").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC), false);
        }
    }

    /** Vuelve a ser jugador normal (al revivir o al terminar el evento). */
    static void restaurar(MinecraftServer s, Pieza p) {
        ServerPlayer sp = s.getPlayerList().getPlayer(p.id);
        if (sp != null && sp.isSpectator()) {
            sp.setGameMode(GameType.ADVENTURE);
        }
        if (p.bot && Evento.entidad(s, p) instanceof com.coleccionista.bot.PiezaBot b) {
            b.fantasma(false);
        }
        com.coleccionista.voz.Voz.actualizar(s);
    }

    /** Al conectarse: si ya era fantasma, sigue siéndolo. */
    static void alEntrar(Evento ev, ServerPlayer p) {
        Pieza x = ev.pieza(p);
        if (x != null && !x.vivo() && !p.isSpectator()) {
            p.setGameMode(GameType.SPECTATOR);
        }
    }

    /** Lo que escribe un fantasma solo lo leen los otros fantasmas y los organizadores. */
    private static void chat(ServerChatEvent e) {
        ServerPlayer p = e.getPlayer();
        Evento ev = Evento.get(p.server);
        if (!ev.esFantasma(p.getUUID())) {
            return;
        }
        e.setCanceled(true);
        Component m = Component.literal("[Fantasma] ").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(p.getGameProfile().getName() + ": ").withStyle(ChatFormatting.AQUA))
                .append(e.getMessage().copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
            if (ev.esFantasma(o.getUUID()) || ev.esOrganizador(o.getUUID()) || (ev.pieza(o) == null && Plat.isOp(o))) {
                Plat.msg(o, m, false);
            }
        }
    }

    private static void comando(CommandEvent e) {
        CommandSourceStack src = e.getParseResults().getContext().getSource();
        if (!(src.getEntity() instanceof ServerPlayer p) || !Evento.get(p.server).esFantasma(p.getUUID())) {
            return;
        }
        String cmd = e.getParseResults().getReader().getString().trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        String raiz = cmd.split(" ", 2)[0].toLowerCase(Locale.ROOT);
        if (raiz.startsWith("minecraft:")) {
            raiz = raiz.substring("minecraft:".length());
        }
        if (BLOQUEADOS.contains(raiz)) {
            e.setCanceled(true);
            Plat.msg(p, Component.translatable(MOD + ".fantasma.no_hablar").withStyle(ChatFormatting.RED), false);
        }
    }
}
