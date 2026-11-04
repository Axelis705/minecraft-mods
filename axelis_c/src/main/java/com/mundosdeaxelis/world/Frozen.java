package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.FrozenStatePacket;
import com.mundosdeaxelis.network.ModNetwork;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Congelación: un ataque helado deja al jugador atrapado en un bloque de hielo. No puede moverse, atacar ni usar objetos y tiene que
 * spamear la tecla de salto (cada pulsación quita un golpe de hielo). Si deja de pulsar, el hielo se vuelve a cerrar poco a poco.
 * Un compañero puede ayudar con clic derecho sobre él (quita 2 golpes). A los 20 s se deshace solo. Tras romperlo hay 3 s de
 * inmunidad para que no te encadenen.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Frozen {
    private Frozen() {
    }

    /** Golpes de hielo por defecto (pulsaciones de salto). */
    public static final int DEFAULT_HITS = 14;
    private static final int TIMEOUT = 400;
    private static final int IMMUNE = 60;

    private static final class State {
        int hits;
        int max;
        Vec3 pos;
        long start;
        long lastHit;
        long lastHitTick = -1;
        Props.Piece ice;
    }

    private static final Map<UUID, State> FROZEN = new HashMap<>();
    private static final Map<UUID, Long> IMMUNE_UNTIL = new HashMap<>();

    public static boolean isFrozen(net.minecraft.world.entity.player.Player p) {
        return FROZEN.containsKey(p.getUUID());
    }

    /** Congela a un jugador. Devuelve false si ya estaba congelado o está en su segundo de inmunidad. */
    public static boolean freeze(ServerPlayer p, int hits) {
        UUID id = p.getUUID();
        ServerLevel l = p.serverLevel();
        long now = l.getGameTime();
        if (p.isSpectator() || FROZEN.containsKey(id) || IMMUNE_UNTIL.getOrDefault(id, 0L) > now) {
            return false;
        }
        State s = new State();
        s.hits = Math.max(1, hits);
        s.max = s.hits;
        s.pos = p.position();
        s.start = now;
        s.lastHit = now;
        s.ice = Props.spawn(l, Blocks.ICE, s.pos.add(0, -0.05, 0), 1.3F, 2.3F, 1.3F, false, 13, new Quaternionf());
        FROZEN.put(id, s);
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.3F, 0.7F);
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.2F, 0.6F);
        l.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.6, 0.9, 0.6, 0.05);
        ModNetwork.sendTo(p, new FrozenStatePacket(true, s.hits, s.max));
        Titles.hint(p, "¡Congelado!", "Pulsa ESPACIO sin parar para romper el hielo");
        return true;
    }

    /** Quita {@code n} golpes de hielo (una pulsación de salto, o ayuda de un compañero). */
    public static void hit(ServerPlayer p, int n) {
        State s = FROZEN.get(p.getUUID());
        if (s == null) {
            return;
        }
        ServerLevel l = p.serverLevel();
        long now = l.getGameTime();
        if (s.lastHitTick == now && n == 1) {
            return; // máximo una pulsación por tick
        }
        s.lastHitTick = now;
        s.lastHit = now;
        s.hits -= n;
        float pitch = 0.8F + 0.9F * (1.0F - Math.max(0, s.hits) / (float) s.max);
        l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GLASS_HIT, SoundSource.PLAYERS, 1.0F, pitch);
        l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.5, 0.8, 0.5, 0.05);
        if (s.hits <= 0) {
            thaw(p, true);
        } else {
            ModNetwork.sendTo(p, new FrozenStatePacket(true, s.hits, s.max));
        }
    }

    /** Deshace el hielo. {@code broken}: se rompió con estruendo (si no, solo se limpia). */
    public static void thaw(ServerPlayer p, boolean broken) {
        State s = FROZEN.remove(p.getUUID());
        if (s == null) {
            return;
        }
        ServerLevel l = p.serverLevel();
        if (s.ice != null) {
            s.ice.remove();
        }
        if (broken) {
            l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.6F, 0.9F);
            l.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.2F, 1.2F);
            l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                    p.getX(), p.getY() + 1.0, p.getZ(), 60, 0.7, 0.9, 0.7, 0.2);
            l.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.7, 0.9, 0.7, 0.15);
        }
        IMMUNE_UNTIL.put(p.getUUID(), l.getGameTime() + IMMUNE);
        ModNetwork.sendTo(p, new FrozenStatePacket(false, 0, 0));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || FROZEN.isEmpty()) {
            return;
        }
        State s = FROZEN.get(p.getUUID());
        if (s == null) {
            return;
        }
        long now = p.serverLevel().getGameTime();
        if (!p.isAlive() || now - s.start > TIMEOUT) {
            thaw(p, p.isAlive());
            return;
        }
        // atrapado: no se mueve (el cliente ya ignora las teclas; esto es el seguro del servidor)
        Vec3 d = p.position().subtract(s.pos);
        if (d.horizontalDistanceSqr() > 0.04 || Math.abs(d.y) > 0.6) {
            p.connection.teleport(s.pos.x, s.pos.y, s.pos.z, p.getYRot(), p.getXRot());
        }
        p.setDeltaMovement(0, Math.min(0.0, p.getDeltaMovement().y), 0);
        p.fallDistance = 0.0F;
        // si dejas de pulsar, el hielo se vuelve a cerrar
        long idle = now - s.lastHit;
        if (idle >= 20 && idle % 15 == 0 && s.hits < s.max) {
            s.hits++;
            ModNetwork.sendTo(p, new FrozenStatePacket(true, s.hits, s.max));
        }
        // junto a un Brasero Eterno el hielo se derrite solo (1 golpe cada 0,25 s)
        if (now % 5 == 0 && Cold.heat(p) >= 2) {
            s.lastHitTick = -1;
            hit(p, 1);
            if (!FROZEN.containsKey(p.getUUID())) {
                return;
            }
        }
        if (now % 10 == 0) {
            p.serverLevel().sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.0, p.getZ(), 2, 0.4, 0.8, 0.4, 0.01);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        if (isFrozen(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (isFrozen(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (isFrozen(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (isFrozen(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    /** Un compañero con clic derecho sobre alguien congelado le quita 2 golpes de hielo. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (isFrozen(e.getEntity())) {
            e.setCanceled(true);
            return;
        }
        if (e.getHand() == InteractionHand.MAIN_HAND && !e.getLevel().isClientSide
                && e.getTarget() instanceof ServerPlayer target && isFrozen(target)) {
            hit(target, 2);
            e.setCanceled(true);
        }
    }

    /** Al cerrar el servidor se retira el hielo para que no quede un bloque flotando guardado en el mundo. */
    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent e) {
        for (State s : FROZEN.values()) {
            if (s.ice != null) {
                s.ice.remove();
            }
        }
        FROZEN.clear();
        IMMUNE_UNTIL.clear();
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            thaw(p, false);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            thaw(p, false);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            thaw(p, false);
        }
    }
}
