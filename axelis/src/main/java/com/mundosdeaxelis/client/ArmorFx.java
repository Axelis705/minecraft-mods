package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.gear.SetArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.Map;

/** Partículas propias de cada armadura de set, más fuertes cuanto más piezas lleve puestas el jugador. */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ArmorFx {
    private ArmorFx() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.multiplayer.ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            return;
        }
        for (AbstractClientPlayer p : level.players()) {
            if (p.isSpectator() || p.distanceToSqr(mc.player) > 48 * 48) {
                continue;
            }
            Map<com.mundosdeaxelis.gear.GearSet, Integer> count = new EnumMap<>(com.mundosdeaxelis.gear.GearSet.class);
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack st = p.getItemBySlot(slot);
                if (st.getItem() instanceof SetArmor a && a.getEquipmentSlot() == slot) {
                    count.merge(a.set(), 1, Integer::sum);
                }
            }
            boolean firstPersonSelf = p == mc.player && mc.options.getCameraType().isFirstPerson();
            for (Map.Entry<com.mundosdeaxelis.gear.GearSet, Integer> e : count.entrySet()) {
                spawn(level, p, e.getKey().id, e.getValue(), firstPersonSelf);
            }
        }
    }

    private static void spawn(Level level, AbstractClientPlayer p, String set, int pieces, boolean firstPerson) {
        RandomSource r = level.getRandom();
        float chance = 0.05F * pieces * (firstPerson ? 0.4F : 1.0F);
        if (r.nextFloat() >= chance) {
            return;
        }
        double x = p.getX() + (r.nextDouble() - 0.5) * 0.8;
        double y = p.getY() + 0.2 + r.nextDouble() * 1.7;
        double z = p.getZ() + (r.nextDouble() - 0.5) * 0.8;
        ParticleOptions type;
        double vx = 0, vy = 0.01, vz = 0;
        switch (set) {
            case "forest" -> {
                type = r.nextInt(3) == 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SPORE_BLOSSOM_AIR;
                vy = -0.01;
            }
            case "bloom" -> {
                type = r.nextInt(3) == 0 ? ParticleTypes.CHERRY_LEAVES : ParticleTypes.SPORE_BLOSSOM_AIR;
                vy = -0.02;
                vx = (r.nextDouble() - 0.5) * 0.03;
            }
            case "forge" -> {
                type = r.nextInt(4) == 0 ? ParticleTypes.SMOKE : ParticleTypes.SMALL_FLAME;
                vy = 0.02;
            }
            case "bronze" -> {
                type = r.nextInt(3) == 0 ? ParticleTypes.CLOUD : (r.nextInt(3) == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE);
                vy = 0.03;
                vx = (r.nextDouble() - 0.5) * 0.02;
            }
            case "ice" -> {
                type = ParticleTypes.SNOWFLAKE;
                vy = -0.02;
                vx = (r.nextDouble() - 0.5) * 0.02;
            }
            case "aurora" -> {
                type = r.nextInt(3) == 0 ? ParticleTypes.WITCH : (r.nextInt(2) == 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.END_ROD);
                vy = 0.025;
                vx = (r.nextDouble() - 0.5) * 0.02;
            }
            case "magma" -> {
                if (r.nextInt(3) == 0) {
                    type = ParticleTypes.DRIPPING_LAVA;
                    y = p.getY() + 0.3 + r.nextDouble() * 0.6;
                } else {
                    type = ParticleTypes.SMALL_FLAME;
                    vy = 0.03;
                }
            }
            case "obsidian" -> {
                if (r.nextInt(4) == 0) {
                    type = ParticleTypes.DRIPPING_OBSIDIAN_TEAR;
                    y = p.getY() + 0.3 + r.nextDouble() * 0.6;
                } else if (r.nextInt(2) == 0) {
                    type = ParticleTypes.ASH;
                    vy = 0.02;
                } else {
                    type = ParticleTypes.SMALL_FLAME;
                    vy = 0.03;
                }
            }
            case "moon" -> {
                type = r.nextInt(3) == 0 ? ParticleTypes.END_ROD : ParticleTypes.GLOW;
                vy = -0.01;
                vx = (r.nextDouble() - 0.5) * 0.02;
            }
            case "magic" -> {
                type = ParticleTypes.ENCHANT;
                y = p.getY() + 1.2 + r.nextDouble() * 0.9;
                vx = (r.nextDouble() - 0.5) * 0.4;
                vy = -0.5 - r.nextDouble() * 0.4;
                vz = (r.nextDouble() - 0.5) * 0.4;
            }
            case "ruby" -> {
                type = r.nextInt(3) == 0 ? ParticleTypes.CRIMSON_SPORE : ParticleTypes.SMALL_FLAME;
                vy = 0.025;
            }
            case "golden" -> {
                type = r.nextInt(4) == 0 ? ParticleTypes.END_ROD : ParticleTypes.WAX_ON;
                vy = 0.02;
            }
            case "pegasus" -> {
                type = r.nextInt(3) == 0 ? ParticleTypes.END_ROD : ParticleTypes.CLOUD;
                vy = -0.01;
                vx = (r.nextDouble() - 0.5) * 0.03;
            }
            case "cosmic" -> {
                if (r.nextInt(3) == 0) {
                    type = ParticleTypes.END_ROD;
                    vy = 0.01;
                } else {
                    type = ParticleTypes.PORTAL;
                    vx = (r.nextDouble() - 0.5) * 0.6;
                    vy = (r.nextDouble() - 0.5) * 0.4;
                    vz = (r.nextDouble() - 0.5) * 0.6;
                }
            }
            case "storm" -> {
                type = r.nextInt(5) == 0 ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK;
                vx = (r.nextDouble() - 0.5) * 0.05;
                vz = (r.nextDouble() - 0.5) * 0.05;
            }
            case "shadow" -> {
                if (r.nextInt(4) == 0) {
                    type = ParticleTypes.SOUL;
                    vy = 0.02;
                } else {
                    type = ParticleTypes.PORTAL;
                    vx = (r.nextDouble() - 0.5) * 0.6;
                    vy = (r.nextDouble() - 0.5) * 0.4;
                    vz = (r.nextDouble() - 0.5) * 0.6;
                }
            }
            default -> {
                return;
            }
        }
        level.addParticle(type, x, y, z, vx, vy, vz);
    }
}
