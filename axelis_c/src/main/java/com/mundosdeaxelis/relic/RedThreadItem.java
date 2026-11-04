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

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Hilo Rojo: une a dos personas con un hilo de luz; mientras estén cerca se dan velocidad, se curan y se reparten los golpes. */
public class RedThreadItem extends RelicItem {
    private static final int COLOR = 0xFF3B4F;
    private static final double NEAR = 16.0;
    private static final double SNAP = 46.0;

    private static final class Link {
        UUID a;
        UUID b;
        ServerLevel level;
        int vfx;
        int age;
        int life;
        boolean dead;
    }

    private static final List<Link> LINKS = new ArrayList<>();

    public RedThreadItem() {
        super("red_thread", COLOR, 20 * 60, Rarity.EPIC);
    }

    public static void clearAll() {
        for (Link l : LINKS) {
            l.dead = true;
        }
        LINKS.clear();
    }

    @Override
    protected int interval() {
        return 20;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        ServerPlayer other = nearestPlayer(sl, sp, 12.0);
        if (other == null) {
            return;
        }
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
        other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
        if (!isLinked(sp)) {
            RelicFx.line(sl, Fx.dust(COLOR, 0.5F), sp.position().add(0, 1.1, 0), other.position().add(0, 1.1, 0), 0.7);
        }
    }

    private static ServerPlayer nearestPlayer(ServerLevel sl, ServerPlayer sp, double r) {
        ServerPlayer best = null;
        double bd = r * r;
        for (ServerPlayer o : sl.players()) {
            if (o == sp || !o.isAlive() || o.isSpectator()) {
                continue;
            }
            double d = o.distanceToSqr(sp);
            if (d < bd) {
                bd = d;
                best = o;
            }
        }
        return best;
    }

    private static boolean isLinked(Player p) {
        for (Link l : LINKS) {
            if (l.a.equals(p.getUUID()) || l.b.equals(p.getUUID())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        if (isLinked(sp)) {
            RelicFx.sendActionBar(sp, "message.mundos_de_axelis.red_thread_already");
            return false;
        }
        ServerPlayer other = nearestPlayer(sl, sp, 24.0);
        if (other == null) {
            RelicFx.sendActionBar(sp, "message.mundos_de_axelis.red_thread_none");
            return false;
        }
        if (isLinked(other)) {
            RelicFx.sendActionBar(sp, "message.mundos_de_axelis.red_thread_busy");
            return false;
        }
        Link l = new Link();
        l.a = sp.getUUID();
        l.b = other.getUUID();
        l.level = sl;
        l.life = (int) (20 * 90 * power(stack));
        Vec3 pa = chest(sp);
        Vec3 pb = chest(other);
        l.vfx = Vfx.make(sl, VfxType.BEAM).at(pa).dir(pb.subtract(pa)).rgb(COLOR).size(0.05).life(l.life + 10)
                .flags(VfxType.F_CURVE | VfxType.F_CAPS).a(0.04).b(4).send();
        LINKS.add(l);
        RelicSounds.play(sl, pa, relicId(), "link", 1.0F, 1.0F);
        RelicSounds.play(sl, pb, relicId(), "link", 0.8F, 1.15F);
        for (ServerPlayer p : new ServerPlayer[]{sp, other}) {
            Vfx.rune(sl, p.position(), COLOR, 3.0, 40);
            Vfx.flash(sl, chest(p), COLOR, 2.0, 12);
            Vfx.column(sl, p.position(), COLOR, 0.5, 3.0, 24);
            RelicFx.burst(sl, Fx.heart(COLOR, 0.8F), chest(p), 8, 0.12);
            Fx.screen(p, ScreenFxPacket.HEARTS, COLOR, 30);
            RelicFx.sendActionBar(p, "message.mundos_de_axelis.red_thread_linked");
        }
        tick(l);
        return true;
    }

    private static Vec3 chest(Player p) {
        return p.position().add(0, p.getBbHeight() * 0.62, 0);
    }

    private static ServerPlayer get(Link l, UUID id) {
        return l.level.getServer().getPlayerList().getPlayer(id);
    }

    private static void tick(Link l) {
        if (l.dead) {
            return;
        }
        ServerPlayer a = get(l, l.a);
        ServerPlayer b = get(l, l.b);
        l.age += 3;
        if (a == null || b == null || !a.isAlive() || !b.isAlive() || a.serverLevel() != l.level || b.serverLevel() != l.level
                || l.age > l.life || a.distanceTo(b) > SNAP) {
            end(l, a, b);
            return;
        }
        Vec3 pa = chest(a);
        Vec3 pb = chest(b);
        double d = pa.distanceTo(pb);
        boolean close = d <= NEAR;
        // el hilo se tensa y palidece al estirarse
        int rgb = close ? COLOR : 0xFF9AA6;
        double sag = close ? 0.04 : Math.max(0.0, 0.04 - (d - NEAR) * 0.0015);
        Vfx.make(l.level, VfxType.BEAM).at(pa).dir(pb.subtract(pa)).rgb(rgb).size(close ? 0.05 : 0.03)
                .flags(VfxType.F_CURVE | VfxType.F_CAPS).a(sag).b(4).life(0).update(l.vfx);
        if (close && l.age % 15 == 0) {
            a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false, true));
            b.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false, true));
        }
        if (close && l.age % 60 == 0) {
            pulse(l, a, b, pa, pb);
        }
        Scheduler.runLater(3, () -> tick(l));
    }

    private static void pulse(Link l, ServerPlayer a, ServerPlayer b, Vec3 pa, Vec3 pb) {
        ServerLevel sl = l.level;
        RelicSounds.play(sl, pa.add(pb).scale(0.5), "red_thread", "pulse", 0.7F, 1.0F);
        for (int k = 0; k <= 8; k++) {
            final int kk = k;
            Scheduler.runLater(k, () -> {
                Vec3 x = pa.lerp(pb, kk / 8.0);
                Vec3 y = pb.lerp(pa, kk / 8.0);
                sl.sendParticles(Fx.heart(COLOR, 0.55F), x.x, x.y + 0.1, x.z, 1, 0.02, 0.02, 0.02, 0.0);
                sl.sendParticles(Fx.heart(COLOR, 0.55F), y.x, y.y + 0.1, y.z, 1, 0.02, 0.02, 0.02, 0.0);
            });
        }
        Scheduler.runLater(8, () -> {
            for (ServerPlayer p : new ServerPlayer[]{a, b}) {
                if (p.isAlive()) {
                    p.heal(1.0F);
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, false, true));
                    Vfx.flash(sl, chest(p), COLOR, 1.3, 7);
                    RelicFx.burst(sl, Fx.heart(COLOR, 0.6F), chest(p), 3, 0.08);
                }
            }
        });
    }

    private static void end(Link l, ServerPlayer a, ServerPlayer b) {
        l.dead = true;
        LINKS.remove(l);
        Vfx.remove(l.level, l.vfx);
        for (ServerPlayer p : new ServerPlayer[]{a, b}) {
            if (p != null && p.isAlive() && p.serverLevel() == l.level) {
                Vec3 c = chest(p);
                RelicSounds.play(l.level, c, "red_thread", "snap", 0.8F, 1.0F);
                Vfx.flash(l.level, c, COLOR, 1.8, 10);
                Vfx.ring(l.level, p.position().add(0, 0.1, 0), COLOR, 2.2, 14);
                RelicFx.burst(l.level, Fx.heart(COLOR, 0.7F), c, 6, 0.15);
                RelicFx.burst(l.level, Fx.dust(COLOR, 0.6F), c, 12, 0.15);
                RelicFx.sendActionBar(p, "message.mundos_de_axelis.red_thread_end");
            }
        }
    }

    /** Mientras estén cerca, el hilo reparte el daño: cada uno recibe menos y quien está unido se refuerza. */
    public static void onHurt(LivingHurtEvent event, ServerPlayer victim) {
        for (Link l : LINKS) {
            boolean isA = l.a.equals(victim.getUUID());
            boolean isB = l.b.equals(victim.getUUID());
            if (!isA && !isB) {
                continue;
            }
            ServerPlayer other = get(l, isA ? l.b : l.a);
            if (other == null || !other.isAlive() || other.serverLevel() != l.level || other.distanceTo(victim) > NEAR) {
                return;
            }
            float dmg = event.getAmount();
            event.setAmount(dmg * 0.8F);
            other.heal(Math.min(2.0F, dmg * 0.2F));
            Vec3 pa = chest(victim);
            Vec3 pb = chest(other);
            RelicSounds.play(l.level, pa, "red_thread", "pulse", 0.5F, 1.3F);
            for (int k = 0; k <= 5; k++) {
                final int kk = k;
                Scheduler.runLater(k, () -> {
                    Vec3 x = pa.lerp(pb, kk / 5.0);
                    l.level.sendParticles(Fx.heart(COLOR, 0.5F), x.x, x.y, x.z, 1, 0.02, 0.02, 0.02, 0.0);
                });
            }
            return;
        }
    }
}
