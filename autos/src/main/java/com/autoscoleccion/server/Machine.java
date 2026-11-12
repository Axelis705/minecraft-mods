package com.autoscoleccion.server;

import com.autoscoleccion.game.Boxes;
import com.autoscoleccion.game.SkillGame;
import com.autoscoleccion.net.AutosNet;
import com.autoscoleccion.net.GameOutcomePacket;
import com.autoscoleccion.net.StartGamePacket;
import com.autoscoleccion.registry.ModItems;
import com.autoscoleccion.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La máquina expendedora en el servidor: guarda la partida de cada jugador (qué prueba, semilla y cuándo empezó), recibe sus teclas,
 * calcula el puntaje repitiendo la partida (así no se puede hacer trampa mandando un puntaje inventado) y entrega la caja.
 */
public final class Machine {
    /** Una partida en curso. */
    private record Session(int id, int type, long seed, BlockPos pos, Direction facing, long startMs) {
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static int nextId = 1;

    private Machine() {
    }

    public static boolean busy(ServerPlayer sp) {
        Session s = SESSIONS.get(sp.getUUID());
        // una partida olvidada (cerró el juego o se trabó) caduca a los 2 minutos
        if (s != null && System.currentTimeMillis() - s.startMs > 120_000L) {
            SESSIONS.remove(sp.getUUID());
            return false;
        }
        return s != null;
    }

    /** Empieza una prueba: elige una distinta de la última que jugó, para que no se acostumbre. */
    public static void start(ServerPlayer sp, BlockPos machine, Direction facing) {
        int last = AutosData.lastGame(sp);
        int type;
        do {
            type = sp.getRandom().nextInt(SkillGame.TYPES);
        } while (type == last && SkillGame.TYPES > 1);
        AutosData.setLastGame(sp, type);
        long seed = sp.getRandom().nextLong();
        Session s = new Session(nextId++, type, seed, machine.immutable(), facing, System.currentTimeMillis());
        SESSIONS.put(sp.getUUID(), s);
        sp.serverLevel().playSound(null, machine, ModSounds.get("coin"), SoundSource.BLOCKS, 1.0F, 1.0F);
        sp.serverLevel().playSound(null, machine, ModSounds.get("machine_start"), SoundSource.BLOCKS, 0.9F, 1.0F);
        AutosNet.toPlayer(sp, new StartGamePacket(s.id, type, seed, machine));
    }

    /** El cliente terminó (o cerró la pantalla): calcula el puntaje y entrega la caja. */
    public static void finish(ServerPlayer sp, int sessionId, List<SkillGame.Input> inputs, int endT) {
        Session s = SESSIONS.get(sp.getUUID());
        if (s == null || s.id != sessionId) {
            return;
        }
        SESSIONS.remove(sp.getUUID());
        SkillGame proto = SkillGame.create(s.type, s.seed);
        // el tiempo que dice el cliente no puede ser mayor que el que pasó de verdad (con margen por el retraso de la red)
        long real = System.currentTimeMillis() - s.startMs + 1500L;
        int end = (int) Math.min(Math.min(endT, real), proto.maxDuration());
        double score = SkillGame.replay(s.type, s.seed, inputs, end);
        int box = Boxes.rollBox(score, new java.util.Random(sp.getRandom().nextLong()));
        double[] odds = Boxes.odds(score);
        AutosNet.toPlayer(sp, new GameOutcomePacket(sessionId, (float) score, box, (float) odds[0], (float) odds[1], (float) odds[2], (float) odds[3]));
        dispense(sp, s, box);
    }

    public static void cancel(ServerPlayer sp) {
        SESSIONS.remove(sp.getUUID());
    }

    /** La máquina suelta la caja por la bandeja de abajo, con chispas y sonido. */
    private static void dispense(ServerPlayer sp, Session s, int box) {
        ServerLevel level = sp.serverLevel();
        ItemStack stack = new ItemStack(ModItems.BOXES.get(box).get());
        Vec3 front = Vec3.atCenterOf(s.pos).add(s.facing.getStepX() * 0.7, -0.15, s.facing.getStepZ() * 0.7);
        boolean near = sp.distanceToSqr(front) < 64.0;
        if (near) {
            ItemEntity e = new ItemEntity(level, front.x, front.y, front.z, stack);
            e.setDeltaMovement(s.facing.getStepX() * 0.12, 0.18, s.facing.getStepZ() * 0.12);
            e.setPickUpDelay(10);
            level.addFreshEntity(e);
        } else if (!sp.getInventory().add(stack)) {
            sp.drop(stack, false);
        }
        level.playSound(null, s.pos, ModSounds.get("box_drop"), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(box == 2 ? ParticleTypes.TOTEM_OF_UNDYING : box == 1 ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER,
                front.x, front.y + 0.3, front.z, box == 2 ? 40 : 14, 0.3, 0.3, 0.3, box == 2 ? 0.25 : 0.05);
        level.blockEvent(s.pos, level.getBlockState(s.pos).getBlock(), 1, box);
        if (box == 2) {
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§6✦ " + sp.getGameProfile().getName()
                    + " sacó una caja §lLeyendas del Circuito§r§6 en la máquina expendedora."), false);
        }
    }
}
