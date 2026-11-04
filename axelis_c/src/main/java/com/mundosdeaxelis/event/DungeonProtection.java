package com.mundosdeaxelis.event;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.world.BossTravel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Protege la dimensión del Rey Brote y su dungeon: no aparecen monstruos normales (solo los de las oleadas), las explosiones no
 * rompen la obra, no se pueden vaciar cubos y no se puede saltar las salas con perlas de ender. (Romper y colocar bloques
 * lo bloquea {@link HubProtection}.) En creativo se puede hacer todo, para poder probar.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class DungeonProtection {
    private DungeonProtection() {
    }

    private static boolean denied(MobSpawnType t) {
        return switch (t) {
            case NATURAL, CHUNK_GENERATION, SPAWNER, STRUCTURE, JOCKEY, REINFORCEMENT, PATROL -> true;
            default -> false;
        };
    }

    /** Nada de zombis, esqueletos ni creepers por la oscuridad, ni animales sobre el pasto de los techos. */
    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn e) {
        ServerLevel sl = e.getLevel().getLevel();
        if (BossTravel.inArena(sl) && denied(e.getSpawnType())) {
            e.setSpawnCancelled(true);
        }
    }

    /** Red de seguridad: cualquier monstruo que no sea del mod y entre en la dimensión sin ser invocado a propósito, se va. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !BossTravel.inArena(e.getLevel()) || !(e.getEntity() instanceof Mob m) || !(m instanceof Enemy)) {
            return;
        }
        EntityType<?> type = m.getType();
        var key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (key != null && key.getNamespace().equals("minecraft") && !m.getPersistentData().getBoolean("mdc_allowed")) {
            e.setCanceled(true);
        }
    }

    /** Los enemigos no se hieren entre sí (golpes, proyectiles, raíces...): solo hacen daño a los jugadores. */
    @SubscribeEvent
    public static void onFriendlyFire(LivingAttackEvent e) {
        if (e.getEntity().level().isClientSide() || !BossTravel.inArena(e.getEntity().level())) {
            return;
        }
        if (e.getEntity() instanceof Mob && e.getSource().getEntity() instanceof Mob) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate e) {
        if (BossTravel.inArena(e.getLevel())) {
            e.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent
    public static void onBucket(FillBucketEvent e) {
        Level l = e.getLevel();
        if (BossTravel.inArena(l) && !e.getEntity().isCreative()) {
            e.setCanceled(true);
        }
    }

    private static boolean banned(net.minecraft.world.item.ItemStack st) {
        return st.is(net.minecraft.world.item.Items.ENDER_PEARL) || st.is(net.minecraft.world.item.Items.CHORUS_FRUIT);
    }

    private static void warn(net.minecraft.world.entity.player.Player p) {
        if (!p.level().isClientSide()) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("No se puede usar esto aquí.")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
        }
    }

    /** Las perlas y la fruta de chorus no se pueden ni lanzar ni comer: se impide antes de gastarlas, con un aviso abajo. */
    @SubscribeEvent
    public static void onUsePearl(PlayerInteractEvent.RightClickItem e) {
        if (BossTravel.inArena(e.getLevel()) && !e.getEntity().isCreative() && banned(e.getItemStack())) {
            e.setCanceled(true);
            e.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            warn(e.getEntity());
        }
    }

    @SubscribeEvent
    public static void onUseChorus(LivingEntityUseItemEvent.Start e) {
        if (e.getEntity() instanceof net.minecraft.world.entity.player.Player p && BossTravel.inArena(p.level()) && !p.isCreative() && banned(e.getItem())) {
            e.setCanceled(true);
            warn(p);
        }
    }

    @SubscribeEvent
    public static void onPearl(EntityTeleportEvent.EnderPearl e) {
        if (BossTravel.inArena(e.getPlayer().level()) && !e.getPlayer().isCreative()) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChorus(EntityTeleportEvent.ChorusFruit e) {
        if (BossTravel.inArena(e.getEntity().level()) && !(e.getEntity() instanceof net.minecraft.world.entity.player.Player p && p.isCreative())) {
            e.setCanceled(true);
        }
    }
}
