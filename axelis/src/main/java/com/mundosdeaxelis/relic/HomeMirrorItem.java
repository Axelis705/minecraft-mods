package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Espejo del Hogar: mirándote en él durante tres segundos, un portal te lleva a tu cama (o al inicio del mundo). */
public class HomeMirrorItem extends RelicItem {
    private static final int COLOR = 0xB690FF;

    public HomeMirrorItem() {
        super("home_mirror", COLOR, 20 * 60, Rarity.EPIC);
    }

    @Override
    protected int channelTicks() {
        return 60;
    }

    @Override
    protected UseAnim anim() {
        return UseAnim.SPYGLASS;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sl.getMaxLocalRawBrightness(sp.blockPosition()) < 5) {
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
            if (time % 40 == 0) {
                sl.sendParticles(Fx.spark(COLOR, 0.6F), sp.getX(), sp.getEyeY(), sp.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }

    @Override
    protected boolean channelStart(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        sp.getPersistentData().putDouble("mdc_mx", sp.getX());
        sp.getPersistentData().putDouble("mdc_mz", sp.getZ());
        RelicSounds.play(sl, sp, relicId(), "charge", 0.85F, 1.0F);
        return true;
    }

    @Override
    protected void channelTick(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used) {
        double dx = sp.getX() - sp.getPersistentData().getDouble("mdc_mx");
        double dz = sp.getZ() - sp.getPersistentData().getDouble("mdc_mz");
        if (sp.hurtTime > 0 || dx * dx + dz * dz > 6.0) {
            sp.stopUsingItem();
            sp.displayClientMessage(Component.translatable("msg.mundos_de_axelis.mirror_break"), true);
            RelicSounds.stop(sp, relicId(), "charge");
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
            RelicFx.burst(sl, Fx.shard(COLOR, 0.8F), sp.position().add(0, 1, 0), 14, 0.25);
            return;
        }
        float k = used / 60.0F;
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        for (int i = 0; i < 3; i++) {
            double a = used * 0.38 + i * 2.094;
            double r = 1.5 - k * 0.9;
            double h = ((used * 0.05 + i * 0.7) % 2.2);
            sl.sendParticles(Fx.dust(COLOR, 0.8F), x + Math.cos(a) * r, y + h, z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            sl.sendParticles(Fx.wisp(0xD8C0FF, 0.7F), x + Math.cos(a + 1) * r * 0.8, y + 2.2 - h, z + Math.sin(a + 1) * r * 0.8, 1, 0, 0, 0, 0);
        }
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 1.0, z, 3, 0.5, 0.8, 0.5, 0.05);
        if (used % 4 == 0) {
            RelicFx.sigil(sl, new Vec3(x, y, z), 1.6, COLOR, used * 0.15);
        }
        if (used % 10 == 0) {
            Fx.sound(sl, x, y, z, ModSounds.RELIC_MAGIC.get(), SoundSource.PLAYERS, 0.4F, 0.7F + k * 0.9F);
        }
        if (used == 45) {
            Fx.screen(sp, ScreenFxPacket.STARS, COLOR, 30);
            RelicFx.shake(sp, 0.8F, 20);
        }
    }

    @Override
    protected void channelEnd(ServerPlayer sp, ServerLevel sl, ItemStack stack, int used, boolean completed) {
        RelicSounds.stop(sp, relicId(), "charge");
        if (!completed) {
            return;
        }
        MinecraftServer server = sl.getServer();
        ServerLevel dest = server.overworld();
        BlockPos rp = sp.getRespawnPosition();
        double tx;
        double ty;
        double tz;
        boolean ok = false;
        tx = 0;
        ty = 0;
        tz = 0;
        if (rp != null) {
            ServerLevel d = server.getLevel(sp.getRespawnDimension());
            if (d != null) {
                BlockState st = d.getBlockState(rp);
                if (st.getBlock() instanceof BedBlock) {
                    dest = d;
                    tx = rp.getX() + 0.5;
                    ty = rp.getY() + 0.65;
                    tz = rp.getZ() + 0.5;
                    ok = true;
                } else if (st.is(Blocks.RESPAWN_ANCHOR)) {
                    dest = d;
                    tx = rp.getX() + 0.5;
                    ty = rp.getY() + 1.0;
                    tz = rp.getZ() + 0.5;
                    ok = true;
                }
            }
        }
        if (!ok) {
            BlockPos sp0 = dest.getSharedSpawnPos();
            BlockPos top = dest.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sp0);
            tx = sp0.getX() + 0.5;
            ty = top.getY();
            tz = sp0.getZ() + 0.5;
        }
        final ServerLevel fd = dest;
        final double fx = tx;
        final double fy = ty;
        final double fz = tz;
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 0.8F);
        RelicSounds.play(sl, sp, relicId(), "cast", 1.0F, 1.0F);
        RelicFx.burst(sl, Fx.spark(COLOR, 1.0F), sp.position().add(0, 1, 0), 30, 0.4);
        Fx.helix(sl, Fx.dust(COLOR, 1.0F), sp.getX(), sp.getY(), sp.getZ(), 0.8, 2.4, 24, 0);
        sp.teleportTo(fd, fx, fy, fz, sp.getYRot(), sp.getXRot());
        sp.fallDistance = 0.0F;
        spend(sp, stack, cooldownTicks(), false);
        Scheduler.runLater(2, () -> {
            ServerLevel nl = sp.serverLevel();
            Fx.sound(nl, fx, fy, fz, ModSounds.RELIC_ACTIVATE.get(), SoundSource.PLAYERS, 1.0F, 1.1F);
            Fx.sound(nl, fx, fy, fz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.3F);
            RelicSounds.play(nl, new Vec3(fx, fy, fz), "home_mirror", "arrive", 1.0F, 1.0F);
            RelicFx.burst(nl, Fx.spark(COLOR, 1.0F), new Vec3(fx, fy + 1, fz), 30, 0.4);
            for (int i = 0; i < 4; i++) {
                final int r = i;
                Scheduler.runLater(i * 2, () -> Fx.ring(nl, Fx.dust(COLOR, 1.0F), fx, fy + 0.2 + r * 0.5, fz, 0.8 + r * 0.5, 16, r));
            }
            Fx.screen(sp, ScreenFxPacket.STARS, COLOR, 30);
        });
    }
}
