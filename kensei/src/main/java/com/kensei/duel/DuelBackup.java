package com.kensei.duel;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

/**
 * Lo que había antes de entrar al duelo (dónde estaba el jugador, su modo de juego, vida y hambre). Se guarda en los datos del propio
 * jugador, así que si el servidor se cae a mitad del duelo, al volver a entrar se le devuelve a su sitio.
 */
public final class DuelBackup {
    private static final String KEY = "KenseiDuelBackup";

    private DuelBackup() {
    }

    public static boolean has(ServerPlayer p) {
        return p.getPersistentData().contains(KEY);
    }

    /** Guarda el estado actual (si ya había uno guardado de antes no se pisa: es el original). */
    public static void save(ServerPlayer p) {
        if (has(p)) {
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putString("dim", p.level().dimension().location().toString());
        t.putDouble("x", p.getX());
        t.putDouble("y", p.getY());
        t.putDouble("z", p.getZ());
        t.putFloat("yaw", p.getYRot());
        t.putFloat("pitch", p.getXRot());
        t.putInt("gm", p.gameMode.getGameModeForPlayer().getId());
        t.putFloat("hp", p.getHealth());
        t.putInt("food", p.getFoodData().getFoodLevel());
        t.putFloat("sat", p.getFoodData().getSaturationLevel());
        p.getPersistentData().put(KEY, t);
    }

    /** Devuelve al jugador a donde estaba. {@code vitals}: también su vida y hambre de antes. */
    public static void restore(ServerPlayer p, boolean vitals) {
        if (!has(p)) {
            return;
        }
        CompoundTag t = p.getPersistentData().getCompound(KEY);
        p.getPersistentData().remove(KEY);
        ServerLevel level = p.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(t.getString("dim"))));
        if (level == null) {
            level = p.getServer().getLevel(Level.OVERWORLD);
        }
        p.setDeltaMovement(0, 0, 0);
        p.fallDistance = 0;
        p.teleportTo(level, t.getDouble("x"), t.getDouble("y"), t.getDouble("z"), t.getFloat("yaw"), t.getFloat("pitch"));
        p.setGameMode(GameType.byId(t.getInt("gm")));
        if (vitals) {
            p.setHealth(Math.max(1.0F, Math.min(t.getFloat("hp"), p.getMaxHealth())));
            p.getFoodData().setFoodLevel(t.getInt("food"));
            p.getFoodData().setSaturation(t.getFloat("sat"));
        }
    }
}
