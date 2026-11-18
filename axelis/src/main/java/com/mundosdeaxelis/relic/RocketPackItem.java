package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Propulsor Cohete: mantén el clic derecho para volar. Minecraft repite el uso cada pocos ticks mientras se mantiene,
 * y cada repetición da un empujón (con levitación para que el vuelo sea suave en el cliente) y gasta combustible.
 */
public class RocketPackItem extends RelicItem {
    private static final int COLOR = 0xFF9A3C;
    public static final int FUEL_MAX = 180;
    private static final String FUEL = "mdc_fuel";

    public RocketPackItem() {
        super("rocket_pack", COLOR, 0, Rarity.RARE);
    }

    @Override
    protected boolean hasActive() {
        return true;
    }

    /** Se usa sosteniéndola y manteniendo el clic derecho. */
    @Override
    public Kind kind() {
        return Kind.HAND;
    }

    @Override
    protected int interval() {
        return 4;
    }

    public static int fuel(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(FUEL) ? stack.getTag().getInt(FUEL) : FUEL_MAX;
    }

    private static void setFuel(ItemStack stack, int v) {
        int f = Math.max(0, Math.min(FUEL_MAX, v));
        if (f >= FUEL_MAX) {
            if (stack.hasTag()) {
                stack.getTag().remove(FUEL);
                if (stack.getTag().isEmpty()) {
                    stack.setTag(null);
                }
            }
        } else {
            stack.getOrCreateTag().putInt(FUEL, f);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return fuel(stack) < FUEL_MAX;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(1, Math.round(13.0F * fuel(stack) / FUEL_MAX));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float r = fuel(stack) / (float) FUEL_MAX;
        return Mth.hsvToRgb(0.02F + 0.10F * r, 0.9F, 1.0F);
    }

    private ItemStack held(ServerPlayer sp) {
        if (sp.getOffhandItem().getItem() == this) {
            return sp.getOffhandItem();
        }
        for (int i = 0; i < 9; i++) {
            ItemStack s = sp.getInventory().items.get(i);
            if (s.getItem() == this) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Se recarga mientras no esté soltando fuego y el jugador esté en el suelo o en el agua. */
    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        ItemStack stack = held(sp);
        if (stack.isEmpty()) {
            return;
        }
        long last = sp.getPersistentData().getLong("mdc_rp_last");
        int fuel = fuel(stack);
        if (fuel < FUEL_MAX && time - last > 8 && (sp.onGround() || sp.isInWater() || sp.isInLava())) {
            int add = sp.isInWater() ? 6 : 12;
            setFuel(stack, fuel + add);
            if (fuel(stack) >= FUEL_MAX) {
                RelicSounds.play(sl, sp, relicId(), "ready", 0.7F, 1.0F);
                Vfx.ring(sl, sp.position().add(0, 0.1, 0), COLOR, 1.8, 12);
                RelicFx.burst(sl, Fx.spark(COLOR, 0.6F), sp.position().add(0, 1.0, 0), 10, 0.15);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.pass(stack);
        }
        ServerLevel sl = sp.serverLevel();
        long now = sl.getGameTime();
        var data = sp.getPersistentData();
        long last = data.getLong("mdc_rp_last");
        int fuel = fuel(stack);
        if (fuel <= 0) {
            if (now - data.getLong("mdc_rp_empty") > 30) {
                data.putLong("mdc_rp_empty", now);
                RelicSounds.play(sl, sp, relicId(), "empty", 0.8F, 1.0F);
                RelicFx.burst(sl, ParticleTypes.SMOKE, sp.position().add(0, 0.9, 0), 10, 0.06);
                RelicFx.sendActionBar(sp, "message.mundos_de_axelis.rocket_empty");
            }
            return InteractionResultHolder.pass(stack);
        }
        float power = power(stack);
        boolean ignition = now - last > 10;
        int cost = Math.max(2, Math.round(4.0F / power));
        setFuel(stack, fuel - cost);
        data.putLong("mdc_rp_last", now);
        int count = ignition ? 0 : data.getInt("mdc_rp_n") + 1;
        data.putInt("mdc_rp_n", count);
        Vec3 v = sp.getDeltaMovement();
        Vec3 look = sp.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
        double nx = v.x + flat.x * 0.2 * power;
        double nz = v.z + flat.z * 0.2 * power;
        double hs = Math.sqrt(nx * nx + nz * nz);
        double max = 0.78;
        if (hs > max) {
            nx *= max / hs;
            nz *= max / hs;
        }
        double ny = ignition ? Math.max(v.y, 0.45) : v.y;
        sp.setDeltaMovement(nx, ny, nz);
        sp.hurtMarked = true;
        sp.fallDistance = 0;
        sp.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 7, 4, true, false, false));
        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 90, 0, true, false, false));
        if (ignition) {
            bump(sp, stack);
            RelicSounds.play(sl, sp, relicId(), "cast", 0.9F, 1.0F);
            Vfx.flash(sl, sp.position().add(0, 0.2, 0), COLOR, 3.0, 9);
            Vfx.ring(sl, sp.position().add(0, 0.1, 0), COLOR, 3.6, 12);
            RelicFx.burst(sl, ParticleTypes.CLOUD, sp.position().add(0, 0.1, 0), 18, 0.25);
        }
        if (count % 3 == 0) {
            RelicSounds.play(sl, sp, relicId(), "thrust", 0.65F, 1.0F);
        }
        // llamas hacia abajo y atrás
        Vec3 back = flat.scale(-0.35);
        double fx = sp.getX() + back.x, fy = sp.getY() + 0.7, fz = sp.getZ() + back.z;
        sl.sendParticles(ParticleTypes.FLAME, fx, fy, fz, 0, (sl.random.nextDouble() - 0.5) * 0.1, -0.4, (sl.random.nextDouble() - 0.5) * 0.1, 1.0);
        sl.sendParticles(ParticleTypes.FLAME, fx, fy, fz, 0, (sl.random.nextDouble() - 0.5) * 0.1, -0.5, (sl.random.nextDouble() - 0.5) * 0.1, 1.0);
        sl.sendParticles(Fx.ember(COLOR, 0.7F), fx, fy - 0.1, fz, 4, 0.15, 0.1, 0.15, 0.05);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, fx, fy - 0.3, fz, 2, 0.12, 0.05, 0.12, 0.02);
        sl.sendParticles(Fx.spark(0xFFE0A0, 0.5F), fx, fy - 0.2, fz, 2, 0.1, 0.1, 0.1, 0.0);
        if (count % 2 == 0) {
            Vfx.flash(sl, new Vec3(fx, fy - 0.2, fz), COLOR, 1.4, 5);
        }
        // si el suelo está cerca, el chorro levanta polvo
        double gy = RelicFx.groundY(sl, sp.getX(), sp.getY(), sp.getZ());
        if (sp.getY() - gy < 4.5 && sp.getY() - gy > 0.0 && count % 2 == 0) {
            Vec3 g = new Vec3(sp.getX(), gy, sp.getZ());
            sl.sendParticles(RelicFx.blockDust(RelicFx.groundState(sl, g.x, g.y - 0.5, g.z)), g.x, g.y + 0.1, g.z, 8, 0.7, 0.05, 0.7, 0.15);
            if (count % 4 == 0) {
                Vfx.ring(sl, g.add(0, 0.05, 0), 0xFFD7A0, 2.4, 9);
            }
        }
        if (fuel - cost <= 0) {
            RelicFx.burst(sl, ParticleTypes.SMOKE, new Vec3(fx, fy, fz), 14, 0.1);
        }
        return InteractionResultHolder.pass(stack);
    }
}
