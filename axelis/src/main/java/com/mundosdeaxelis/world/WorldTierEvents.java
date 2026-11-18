package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aplica la dificultad del mundo ({@link WorldTier}) a los monstruos de Minecraft que nacen en las dimensiones normales:
 * +20 % de vida y +20 % de daño por nivel, y de vez en cuando junto a ellos aparece un monstruo más fuerte de lo normal.
 * No toca a los jefes ni a los mobs de las dimensiones del mod.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class WorldTierEvents {
    private WorldTierEvents() {
    }

    private static final UUID HP_ID = UUID.fromString("5d1c7a02-3b84-4e19-a6f0-9c2e8b7d4a55");
    private static final UUID DMG_ID = UUID.fromString("e92b4c11-70fa-4d3a-8b65-1a6d3f0c9e66");
    public static final double HP_PER_LEVEL = 0.20D;
    public static final double DMG_PER_LEVEL = 0.20D;
    /** Probabilidad por nivel de que un monstruo común natural venga con un refuerzo. */
    public static final double REINFORCE_PER_LEVEL = 0.04D;

    private record Extra(EntityType<? extends Mob> type, int minLevel, int weight) {
    }

    /** Monstruos «fuera de lugar» que se unen a las hordas, de menos a más peligrosos. */
    private static final List<Extra> EXTRAS = List.of(
            new Extra(EntityType.PILLAGER, 1, 10),
            new Extra(EntityType.VINDICATOR, 2, 8),
            new Extra(EntityType.WITCH, 2, 8),
            new Extra(EntityType.WITHER_SKELETON, 3, 6),
            new Extra(EntityType.PIGLIN_BRUTE, 4, 5),
            new Extra(EntityType.BLAZE, 4, 5),
            new Extra(EntityType.EVOKER, 5, 4),
            new Extra(EntityType.RAVAGER, 5, 3),
            new Extra(EntityType.GHAST, 7, 1));

    private static boolean spawning;

    private static boolean normalDimension(Level level) {
        return level.dimension().location().getNamespace().equals("minecraft");
    }

    /** Sube vida y daño del monstruo según el nivel del mundo y lo deja con la vida llena. */
    public static void scale(Mob mob, int level) {
        if (level <= 0 || !(mob instanceof Monster) || mob instanceof WitherBoss) {
            return;
        }
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && hp.getModifier(HP_ID) == null) {
            hp.addPermanentModifier(new AttributeModifier(HP_ID, "mdc_world_tier_hp", HP_PER_LEVEL * level, AttributeModifier.Operation.MULTIPLY_BASE));
            mob.setHealth(mob.getMaxHealth());
        }
        AttributeInstance dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && dmg.getModifier(DMG_ID) == null) {
            dmg.addPermanentModifier(new AttributeModifier(DMG_ID, "mdc_world_tier_dmg", DMG_PER_LEVEL * level, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn e) {
        ServerLevel sl = e.getLevel().getLevel();
        if (spawning || !normalDimension(sl)) {
            return;
        }
        Mob mob = e.getEntity();
        boolean brote = mob instanceof com.mundosdeaxelis.entity.Brote;
        if (!(mob instanceof Monster) || (!brote && !EntityTypeIsVanilla(mob))) {
            return;
        }
        int lv = WorldTier.get(sl.getServer()).level();
        if (brote) {
            // el Brote solo vaga por el mundo después de vencer al Rey Brote (nivel 1)
            if (lv < 1 && (e.getSpawnType() == MobSpawnType.NATURAL || e.getSpawnType() == MobSpawnType.CHUNK_GENERATION)) {
                e.setSpawnCancelled(true);
                return;
            }
        }
        if (lv <= 0) {
            return;
        }
        scale(mob, lv);
        if (brote) {
            return;
        }
        boolean horde = mob instanceof Zombie || mob instanceof Skeleton || mob instanceof Spider || mob instanceof Creeper;
        if (horde && e.getSpawnType() == MobSpawnType.NATURAL && sl.dimension() == Level.OVERWORLD
                && sl.random.nextDouble() < REINFORCE_PER_LEVEL * lv) {
            spawnExtra(sl, lv, e.getX(), e.getY(), e.getZ());
        }
    }

    private static boolean EntityTypeIsVanilla(Mob mob) {
        return net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).getNamespace().equals("minecraft");
    }

    private static void spawnExtra(ServerLevel sl, int lv, double x, double y, double z) {
        List<Extra> ok = new ArrayList<>();
        int total = 0;
        for (Extra x2 : EXTRAS) {
            if (x2.minLevel <= lv) {
                ok.add(x2);
                total += x2.weight;
            }
        }
        if (ok.isEmpty()) {
            return;
        }
        RandomSource r = sl.random;
        int roll = r.nextInt(total);
        EntityType<? extends Mob> type = ok.get(0).type;
        for (Extra x2 : ok) {
            roll -= x2.weight;
            if (roll < 0) {
                type = x2.type;
                break;
            }
        }
        Mob extra = type.create(sl);
        if (extra == null) {
            return;
        }
        extra.moveTo(x + r.nextInt(5) - 2, y, z + r.nextInt(5) - 2, r.nextFloat() * 360.0F, 0.0F);
        if (!sl.noCollision(extra)) {
            extra.discard();
            return;
        }
        spawning = true;
        try {
            extra.finalizeSpawn(sl, sl.getCurrentDifficultyAt(extra.blockPosition()), MobSpawnType.EVENT, null, null);
            scale(extra, lv);
            sl.addFreshEntity(extra);
        } finally {
            spawning = false;
        }
    }

    /** Aviso a todos los jugadores cuando el mundo se vuelve más peligroso. */
    public static void announce(MinecraftServer server, int level) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            // quien acaba de vencer al jefe ya lo vio en su pantalla de resultados
            if (!BossRewards.hadResult(p)) {
                p.sendSystemMessage(Component.translatable("message.mundos_de_axelis.world_tier", level, Math.round(HP_PER_LEVEL * level * 100)));
            }
            p.level().playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 0.5F, 1.4F);
        }
    }
}
