package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Ejecuta una acción dentro de N ticks (20 ticks = 1 segundo). */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Scheduler {
    private static final class Task {
        int left;
        final Runnable action;

        Task(int left, Runnable action) {
            this.left = left;
            this.action = action;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private Scheduler() {
    }

    public static void runLater(int ticks, Runnable action) {
        TASKS.add(new Task(Math.max(1, ticks), action));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TASKS.isEmpty()) {
            return;
        }
        List<Runnable> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            t.left--;
            if (t.left <= 0) {
                due.add(t.action);
                it.remove();
            }
        }
        for (Runnable r : due) {
            try {
                r.run();
            } catch (Exception e) {
                MundosDeAxelis.LOGGER.error("Error en una tarea programada", e);
            }
        }
    }
}
