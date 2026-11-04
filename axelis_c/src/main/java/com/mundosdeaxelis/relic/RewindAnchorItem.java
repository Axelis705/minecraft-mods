package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.vfx.VfxType;
import com.mundosdeaxelis.world.Scheduler;
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

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Ancla del Ayer: recuerda dónde y cómo estabas; al usarla vuelves a donde estabas hace unos segundos. */
public class RewindAnchorItem extends RelicItem {
    private static final int COLOR = 0x7CF5E0;
    private static final int KEEP = 16;

    private record Snap(ResourceKey<Level> dim, Vec3 pos, float yRot, float xRot, float health, int food, long time) {
    }

    private static final Map<UUID, Deque<Snap>> SNAPS = new HashMap<>();

    public static void clearAll() {
        SNAPS.clear();
    }

    public static void forget(UUID id) {
        SNAPS.remove(id);
    }

    public RewindAnchorItem() {
        super("rewind_anchor", COLOR, 20 * 45, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (!sp.isAlive() || sp.isSpectator()) {
            return;
        }
        Deque<Snap> q = SNAPS.computeIfAbsent(sp.getUUID(), k -> new ArrayDeque<>());
        if (!q.isEmpty() && time - q.peekLast().time > 40) {
            q.clear();
        }
        q.addLast(new Snap(sl.dimension(), sp.position(), sp.getYRot(), sp.getXRot(), sp.getHealth(), sp.getFoodData().getFoodLevel(), time));
        while (q.size() > KEEP) {
            q.removeFirst();
        }
        if (time % 60 == 0) {
            Vec3 h = RelicFx.hand(sp);
            sl.sendParticles(Fx.spark(COLOR, 0.45F), h.x, h.y, h.z, 1, 0.12, 0.12, 0.12, 0.0);
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Deque<Snap> q = SNAPS.get(sp.getUUID());
        long now = sl.getGameTime();
        Snap best = null;
        if (q != null) {
            long want = now - 120;
            for (Snap s : q) {
                if (now - s.time >= 60 && (best == null || Math.abs(s.time - want) < Math.abs(best.time - want))) {
                    best = s;
                }
            }
        }
        if (best == null) {
            RelicFx.sendActionBar(sp, "message.mundos_de_axelis.rewind_none");
            return false;
        }
        Vec3 from = sp.position();
        Vec3 to = best.pos;
        // despedida: el yo de ahora se desvanece
        Vfx.rune(sl, from, COLOR, 3.0, 36);
        Vfx.column(sl, from, COLOR, 0.55, 4.5, 26);
        Vfx.flash(sl, from.add(0, 1, 0), COLOR, 2.4, 12);
        RelicFx.sphere(sl, Fx.spark(COLOR, 0.6F), from.add(0, 1, 0), 1.0, 26, 0);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, COLOR, 26);
        Fx.screen(sp, ScreenFxPacket.FLASH, COLOR, 14);
        ServerLevel dest = sl.getServer().getLevel(best.dim);
        if (dest == null) {
            dest = sl;
        }
        if (dest == sl) {
            sp.connection.teleport(to.x, to.y, to.z, best.yRot, best.xRot);
        } else {
            sp.teleportTo(dest, to.x, to.y, to.z, best.yRot, best.xRot);
        }
        sp.setDeltaMovement(Vec3.ZERO);
        sp.fallDistance = 0;
        sp.clearFire();
        if (best.health > sp.getHealth()) {
            sp.setHealth(Math.min(sp.getMaxHealth(), best.health));
        }
        if (best.food > sp.getFoodData().getFoodLevel()) {
            sp.getFoodData().setFoodLevel(best.food);
        }
        if (q != null) {
            q.clear();
        }
        final ServerLevel dl = dest;
        Vec3 arrive = to;
        Vfx.beam(dest, from.add(0, 1, 0), to.add(0, 1, 0), COLOR, 0.06, 14, VfxType.F_CAPS | VfxType.F_FLICKER);
        RelicSounds.play(dest, arrive, relicId(), "tick", 0.9F, 1.0F);
        for (int i = 0; i < 6; i++) {
            final int k = i;
            Scheduler.runLater(2 + i * 3, () -> {
                RelicSounds.play(dl, arrive, "rewind_anchor", "tick", 0.5F, 1.0F + k * 0.06F);
                Vfx.ring(dl, arrive.add(0, 0.1, 0), COLOR, 3.2 - k * 0.45, 14);
                RelicFx.sphere(dl, Fx.spark(COLOR, 0.5F), arrive.add(0, 1, 0), 2.4 - k * 0.35, 18, k);
            });
        }
        Scheduler.runLater(2, () -> {
            Vfx.rune(dl, arrive, COLOR, 3.0, 36);
            Vfx.flash(dl, arrive.add(0, 1, 0), 0xFFFFFF, 2.2, 12);
        });
        return true;
    }
}
