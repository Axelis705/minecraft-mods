package com.mundosdeaxelis.table;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Los mobs sueltan sus materiales cuando los mata un jugador (el Botín suma 5 % por nivel).
 * Los jefes (Dragón, Wither y Warden) dan su material a TODOS los jugadores cercanos, para que en cooperativo nadie se quede sin él.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class MobMaterialDrops {
    private MobMaterialDrops() {
    }

    private static final String DONE = "mdc_mats_done";

    private static Item item(Mats m) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(MundosDeAxelis.MODID, m.itemId()));
    }

    /** Material de jefe: uno para cada jugador a menos de 64 bloques. */
    private static void bossLoot(Entity victim, Mats m) {
        Item it = item(m);
        if (it == null || !(victim.level() instanceof net.minecraft.server.level.ServerLevel sl)) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(victim) < 64 * 64) {
                ItemStack st = new ItemStack(it);
                if (!p.getInventory().add(st)) {
                    p.drop(st, false);
                }
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mundos_de_axelis.boss_material",
                        new ItemStack(it).getHoverName()).withStyle(net.minecraft.ChatFormatting.GOLD), true);
            }
        }
    }

    private static boolean isBoss(Mats m) {
        return m == Mats.BOSS_HEART || m == Mats.ABYSS_CORE;
    }

    /** El Dragón no siempre pasa por los drops normales: se resuelve al morir. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        Entity victim = e.getEntity();
        if (victim.level().isClientSide || !(e.getSource().getEntity() instanceof ServerPlayer)) {
            return;
        }
        if (victim.getType() == EntityType.ENDER_DRAGON && !victim.getPersistentData().getBoolean(DONE)) {
            victim.getPersistentData().putBoolean(DONE, true);
            bossLoot(victim, Mats.BOSS_HEART);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        Entity victim = e.getEntity();
        if (victim.level().isClientSide || !(e.getSource().getEntity() instanceof ServerPlayer)) {
            return;
        }
        if (victim instanceof Slime s && s.getSize() < 2) {
            return;
        }
        if (victim instanceof com.mundosdeaxelis.entity.Brote && !victim.getTags().contains(com.mundosdeaxelis.entity.Brote.DUNGEON_TAG)) {
            // el Brote salvaje suelta Gelatina Brillante (la del slime)
            if (victim.level().random.nextInt(100) < 60 + 5 * e.getLootingLevel()) {
                Item jelly = item(Mats.SHINY_JELLY);
                if (jelly != null) {
                    e.getDrops().add(new ItemEntity(victim.level(), victim.getX(), victim.getY() + 0.3, victim.getZ(),
                            new ItemStack(jelly, 1 + victim.level().random.nextInt(2))));
                }
            }
        }
        EntityType<?> type = victim.getType();
        for (Mats m : Mats.from(type)) {
            if (isBoss(m)) {
                if (!victim.getPersistentData().getBoolean(DONE)) {
                    victim.getPersistentData().putBoolean(DONE, true);
                    bossLoot(victim, m);
                }
                continue;
            }
            int chance = m.chance + 5 * e.getLootingLevel();
            if (victim.level().random.nextInt(100) < chance) {
                Item it = item(m);
                if (it != null) {
                    e.getDrops().add(new ItemEntity(victim.level(), victim.getX(), victim.getY() + 0.3, victim.getZ(), new ItemStack(it)));
                }
            }
        }
    }
}
