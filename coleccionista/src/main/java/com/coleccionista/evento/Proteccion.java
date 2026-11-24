package com.coleccionista.evento;

import com.axeliscore.plat.Plat;
import com.coleccionista.Coleccionista;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lo que cuida el mapa del evento una vez construido (solo dentro del complejo; el resto del mundo sigue normal):
 * <ul>
 *   <li>No aparece ningún bicho que no pusiera el evento (murciélagos, zombis, patrullas, fantasmas de la noche...). Los bots y lo que se
 *   invoque con comando sí.</li>
 *   <li>Nadie rompe ni pone bloques si no está en creativo (en supervivencia o aventura el mapa no se toca).</li>
 *   <li>Nadie se lastima por caer (brincar de una escalera o una pasarela no le quita vida a nadie; las pruebas eliminan por sus reglas,
 *   no por un mal brinco).</li>
 * </ul>
 */
public final class Proteccion {
    /** Las formas de aparecer que se bloquean (las de comando y huevo se dejan: los bots y las pruebas). */
    private static final Set<MobSpawnType> PROHIBIDAS = EnumSet.of(MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION, MobSpawnType.STRUCTURE,
            MobSpawnType.PATROL, MobSpawnType.SPAWNER, MobSpawnType.REINFORCEMENT, MobSpawnType.JOCKEY, MobSpawnType.MOB_SUMMONED,
            MobSpawnType.BREEDING, MobSpawnType.CONVERSION, MobSpawnType.EVENT, MobSpawnType.TRIGGERED);

    private Proteccion() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(Proteccion::aparecer);
        MinecraftForge.EVENT_BUS.addListener(Proteccion::romper);
        MinecraftForge.EVENT_BUS.addListener(Proteccion::poner);
        MinecraftForge.EVENT_BUS.addListener(Proteccion::caer);
    }

    private static void caer(net.minecraftforge.event.entity.living.LivingFallEvent e) {
        Entity en = e.getEntity();
        MinecraftServer s = en.getServer();
        if (s != null && (en instanceof ServerPlayer || en instanceof com.coleccionista.bot.PiezaBot) && en.level() == s.overworld()
                && dentro(s, en.blockPosition())) {
            e.setCanceled(true);
        }
    }

    /** ¿Está construido el complejo y este punto cae en él (todas las zonas, con orilla)? Fuera del complejo el mundo sigue normal. */
    static boolean dentro(MinecraftServer s, net.minecraft.core.BlockPos p) {
        if (s == null || p == null || !Complejo.construido(s)) {
            return false;
        }
        net.minecraft.core.BlockPos o = Complejo.origen(s);
        int dx = p.getX() - o.getX(), dy = p.getY() - o.getY(), dz = p.getZ() - o.getZ();
        return dx >= -170 && dx <= 400 && dy >= -30 && dy <= 70 && dz >= -130 && dz <= 200;
    }

    private static void aparecer(MobSpawnEvent.FinalizeSpawn e) {
        MinecraftServer s = e.getLevel().getServer();
        if (s != null && PROHIBIDAS.contains(e.getSpawnType()) && e.getLevel().getLevel() == s.overworld()
                && dentro(s, net.minecraft.core.BlockPos.containing(e.getX(), e.getY(), e.getZ()))) {
            e.setSpawnCancelled(true);
        }
    }

    private static boolean puedeTocar(ServerPlayer p) {
        return p.isCreative() || p.isSpectator();
    }

    private static void romper(BlockEvent.BreakEvent e) {
        if (e.getPlayer() instanceof ServerPlayer p && !puedeTocar(p) && p.level() == Plat.server(p).overworld() && dentro(Plat.server(p), e.getPos())) {
            e.setCanceled(true);
        }
    }

    private static void poner(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !puedeTocar(p) && p.level() == Plat.server(p).overworld() && dentro(Plat.server(p), e.getPos())) {
            e.setCanceled(true);
        }
    }

    /** Al terminar de construir: reglas para que nada aparezca solo y se van los bichos que ya estaban. */
    public static void alConstruir(ServerLevel l) {
        MinecraftServer s = l.getServer();
        GameRules g = s.getGameRules();
        g.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, s);
        g.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, s);
        g.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, s);
        g.getRule(GameRules.RULE_DOINSOMNIA).set(false, s);
        g.getRule(GameRules.RULE_DO_WARDEN_SPAWNING).set(false, s);
        int n = 0;
        for (Entity e : l.getAllEntities()) {
            if (e instanceof AmbientCreature || e instanceof Enemy && !(e instanceof com.coleccionista.bot.PiezaBot)) {
                e.discard();
                n++;
            }
        }
        Coleccionista.LOGGER.info("COLECCIONISTA_PROTECCION bichos_quitados={}", n);
    }
}
