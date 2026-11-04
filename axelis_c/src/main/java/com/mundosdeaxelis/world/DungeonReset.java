package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Brote;
import com.mundosdeaxelis.entity.GranHerrero;
import com.mundosdeaxelis.entity.LoboArtico;
import com.mundosdeaxelis.entity.LoboEscarcha;
import com.mundosdeaxelis.entity.ReyBrote;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Cada vez que una dungeon se reinicia (alguien sale o muere y la dungeon queda vacía, o se entra a una nueva) sube su "época".
 * Todo enemigo que aparece se marca con la época en que nació. Si un enemigo guardado en el mapa vuelve a cargarse con una
 * época vieja (por ejemplo el jefe, que se quedó en una zona descargada cuando el jugador murió), se borra al instante:
 * así el jefe de la vez anterior nunca reaparece. Las tareas con retardo (intros) también comprueban la época para no
 * hacer aparecer a un jefe de una partida que ya se canceló.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class DungeonReset {
    private static final String KEY = "mdc_dungeon_epoch";
    /** Empieza en un valor distinto cada vez que arranca el servidor: lo guardado de una sesión anterior siempre cuenta como viejo. */
    private static final int START = (int) (System.currentTimeMillis() / 1000L);
    private static final Map<ResourceKey<Level>, Integer> EPOCH = new HashMap<>();

    private DungeonReset() {
    }

    public static int epoch(Level level) {
        return EPOCH.getOrDefault(level.dimension(), START);
    }

    /** La dungeon de este nivel empieza de cero: todo enemigo anterior (cargado o guardado) pasa a ser viejo. */
    public static void bump(Level level) {
        EPOCH.put(level.dimension(), epoch(level) + 1);
    }

    private static boolean enemy(Mob m) {
        return m instanceof ReyBrote || m instanceof GranHerrero || m instanceof LoboArtico || m instanceof LoboEscarcha
                || m instanceof Enemy || m.getTags().contains(Brote.DUNGEON_TAG) || m.getTags().contains(ReyBrote.MINION_TAG)
                || m.getTags().contains(GranHerrero.MINION_TAG) || m.getTags().contains(LoboArtico.MINION_TAG);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !BossTravel.inArena(e.getLevel()) || !(e.getEntity() instanceof Mob m) || !enemy(m)) {
            return;
        }
        int now = epoch(e.getLevel());
        var data = m.getPersistentData();
        if (e.loadedFromDisk()) {
            if (!data.contains(KEY) || data.getInt(KEY) != now) {
                e.setCanceled(true); // enemigo de una dungeon anterior que estaba guardado
            }
        } else {
            data.putInt(KEY, now);
        }
    }
}
