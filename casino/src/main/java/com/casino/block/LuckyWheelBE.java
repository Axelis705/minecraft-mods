package com.casino.block;

import com.casino.plat.Plat;
import com.casino.plat.Ser;
import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import com.casino.data.CasinoData;
import com.casino.game.Prize;
import com.casino.game.WheelLayout;
import com.casino.game.WheelRewards;
import com.casino.net.CasinoNet;
import com.casino.net.WheelWinPacket;
import com.casino.registry.ModBlockEntities;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;

/**
 * El corazón de la ruleta (en la parte de abajo del centro): el premio gordo del podio, el giro en curso y el premio que se entregará
 * al terminar. El servidor decide todo; el cliente solo anima el giro con el reloj del juego (así lo ven igual todos los jugadores).
 */
public class LuckyWheelBE extends CasinoBE {
    public static final int SPIN_TICKS = 180;
    /** Altura del centro de la ruleta sobre la base (en bloques). */
    public static final double HUB_Y = 2.0;

    public ItemStack podium = new ItemStack(Items.DIAMOND_BLOCK, 3);
    @Nullable
    public UUID owner;
    public boolean spinning = false;
    public long spinStart;
    public float startAngle;
    public float finalAngle;
    public float restAngle = 351;
    public int result = -1;
    @Nullable
    public UUID spinner;
    public String spinnerName = "";
    public long flashEnd = 0;
    @Nullable
    private Prize pending;

    // ---- solo cliente
    public boolean clientInit = false;
    public boolean wasSpinning = false;
    public int lastSlice = -1;
    public float kickTime = -100;

    public LuckyWheelBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LUCKY_WHEEL.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(LuckyWheelBlock.FACING);
    }

    public void setOwner(@Nullable LivingEntity e) {
        if (e != null) {
            owner = e.getUUID();
            setChanged();
        }
    }

    /** Ángulo actual de la ruleta (grados, sentido de las agujas del reloj). */
    public float currentAngle(float partial) {
        if (!spinning || level == null) {
            return restAngle;
        }
        float t = (level.getGameTime() + partial - spinStart) / SPIN_TICKS;
        t = Math.max(0, Math.min(1, t));
        float inv = 1 - t;
        float ease = 1 - inv * inv * inv * inv; // sale rápido y frena poco a poco
        return startAngle + (finalAngle - startAngle) * ease;
    }

    /** Centro de la ruleta en el mundo. */
    public Vec3 hub() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + HUB_Y, worldPosition.getZ() + 0.5);
    }

    // ------------------------------------------------------------------ interacción (servidor)

    private boolean canEditPrize(ServerPlayer p) {
        return Plat.isOp(p) || p.isCreative() || Plat.isHost(p);
    }

    public void interact(ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        if (p.isShiftKeyDown()) {
            if (!held.isEmpty()) {
                if (spinning) {
                    Plat.msg(p, Component.translatable("casino.wheel.busy").withStyle(ChatFormatting.RED), true);
                } else if (canEditPrize(p)) {
                    ItemStack s = held.copy();
                    s.setCount(Math.min(held.getCount(), s.getMaxStackSize()));
                    podium = s;
                    sync();
                    level.playSound(null, worldPosition, ModSounds.PRIZE_SET.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    Plat.msg(p, Component.translatable("casino.wheel.prize_set", s.getCount() + " x ", s.getHoverName()).withStyle(ChatFormatting.GOLD), true);
                } else {
                    Plat.msg(p, Component.translatable("casino.wheel.no_permission").withStyle(ChatFormatting.RED), true);
                }
            } else {
                Plat.msg(p, Component.translatable("casino.wheel.prize_info", podium.getCount() + " x ", podium.getHoverName()).withStyle(ChatFormatting.GOLD), true);
            }
            return;
        }
        if (spinning) {
            Plat.msg(p, Component.translatable("casino.wheel.busy").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        CasinoData data = CasinoData.get(Plat.server(p));
        if (!data.canSpin(p.getUUID())) {
            Duration d = CasinoData.untilReset();
            String time = String.format("%dh %02dm", d.toHours(), d.toMinutesPart());
            Plat.msg(p, Component.translatable("casino.wheel.already", time).withStyle(ChatFormatting.RED), true);
            level.playSound(null, worldPosition, ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
            return;
        }
        data.markSpin(p.getUUID());
        startSpin(p);
    }

    private void startSpin(ServerPlayer p) {
        RandomSource r = level.getRandom();
        int slice = WheelLayout.roll(new Random(r.nextLong()));
        double off = (r.nextDouble() * 2 - 1) * 0.35;
        float target = (float) WheelLayout.restAngleFor(slice, off);
        float start = restAngle;
        float delta = ((target - start) % 360 + 360) % 360 + 360 * (5 + r.nextInt(3));
        startAngle = start;
        finalAngle = start + delta;
        spinStart = level.getGameTime();
        result = slice;
        spinner = p.getUUID();
        spinnerName = p.getGameProfile().getName();
        pending = WheelRewards.build(slice, podium, r);
        spinning = true;
        sync();
    }

    // ------------------------------------------------------------------ ticks

    public void serverTick() {
        if (spinning && level.getGameTime() >= spinStart + SPIN_TICKS) {
            finish();
        }
    }

    private void finish() {
        spinning = false;
        restAngle = ((finalAngle % 360) + 360) % 360;
        flashEnd = level.getGameTime() + 140;
        Prize prize = pending;
        pending = null;
        if (prize != null && level instanceof ServerLevel sl && spinner != null) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(spinner);
            if (p != null) {
                prize.deliver(p);
                CasinoNet.sendTo(p, new WheelWinPacket(prize));
            } else {
                CasinoData.get(sl.getServer()).addPending(spinner, prize);
            }
            Component msg = Component.translatable("casino.wheel.won", Component.literal(spinnerName).withStyle(ChatFormatting.YELLOW),
                    prize.title.copy().withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY);
            Vec3 h = hub();
            for (ServerPlayer o : sl.players()) {
                if (o.distanceToSqr(h.x, h.y, h.z) < 32 * 32) {
                    o.sendSystemMessage(msg);
                }
            }
        }
        sync();
    }

    public void clientTick() {
        if (level == null) {
            return;
        }
        RandomSource r = level.getRandom();
        Vec3 h = hub();
        if (!clientInit) {
            clientInit = true;
            wasSpinning = spinning;
            lastSlice = WheelLayout.sliceAt(currentAngle(0));
            return;
        }
        if (spinning && !wasSpinning) {
            level.playLocalSound(h.x, h.y, h.z, ModSounds.WHEEL_START.get(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        if (!spinning && wasSpinning) {
            boolean big = result >= 0 && WheelLayout.isBig(result);
            level.playLocalSound(h.x, h.y, h.z, big ? ModSounds.WHEEL_JACKPOT.get() : ModSounds.WHEEL_WIN.get(), SoundSource.BLOCKS, 1.4F, 1.0F, false);
            celebrate(big);
        }
        wasSpinning = spinning;
        // chispas alrededor del premio gordo
        if (r.nextInt(3) == 0) {
            Vec3 pp = prizePos();
            level.addParticle(ParticleTypes.END_ROD, pp.x + (r.nextDouble() - 0.5) * 1.1, pp.y + (r.nextDouble() - 0.5) * 0.9, pp.z + (r.nextDouble() - 0.5) * 1.1,
                    0, 0.01 + r.nextDouble() * 0.015, 0);
        }
        if (level.getGameTime() < flashEnd && r.nextInt(2) == 0) {
            Vec3 d = rightVec();
            double a = r.nextDouble() * Math.PI * 2;
            double rad = 1.45;
            level.addParticle(ParticleTypes.FIREWORK, h.x + d.x * Math.cos(a) * rad, h.y + Math.sin(a) * rad, h.z + d.z * Math.cos(a) * rad,
                    (r.nextDouble() - 0.5) * 0.05, 0.05, (r.nextDouble() - 0.5) * 0.05);
        }
    }

    /** Dónde flota el premio gordo. */
    public Vec3 prizePos() {
        Vec3 h = hub();
        return new Vec3(h.x, h.y + 2.55, h.z);
    }

    private Vec3 rightVec() {
        Direction r = facing().getCounterClockWise();
        return new Vec3(r.getStepX(), 0, r.getStepZ());
    }

    private void celebrate(boolean big) {
        RandomSource r = level.getRandom();
        Vec3 h = hub();
        Vec3 d = rightVec();
        int n = big ? 90 : 40;
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double sp = 0.12 + r.nextDouble() * 0.25;
            level.addParticle(big && i % 3 == 0 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.FIREWORK, h.x, h.y, h.z,
                    d.x * Math.cos(a) * sp, Math.sin(a) * sp + 0.05, d.z * Math.cos(a) * sp);
        }
    }

    // ------------------------------------------------------------------ red y guardado

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected AABB renderBox() {
        return box(worldPosition.offset(-3, -1, -3), worldPosition.offset(4, 7, 4));
    }




    @Override
    protected void writeData(CompoundTag tag) {
        tag.put("podium", Ser.item(podium));
        if (owner != null) {
            Nbt.putUuid(tag, "owner", owner);
        }
        tag.putBoolean("spinning", spinning);
        tag.putLong("spinStart", spinStart);
        tag.putFloat("startAngle", startAngle);
        tag.putFloat("finalAngle", finalAngle);
        tag.putFloat("restAngle", restAngle);
        tag.putInt("result", result);
        if (spinner != null) {
            Nbt.putUuid(tag, "spinner", spinner);
        }
        tag.putString("spinnerName", spinnerName);
        tag.putLong("flashEnd", flashEnd);
        if (pending != null) {
            tag.put("pending", pending.save());
        }
    }

    @Override
    protected void readData(CompoundTag tag) {
        ItemStack p = Ser.item(tag.get("podium"));
        podium = p.isEmpty() ? new ItemStack(Items.DIAMOND_BLOCK, 3) : p;
        owner = Nbt.hasUuid(tag, "owner") ? Nbt.uuid(tag, "owner") : null;
        spinning = Nbt.b(tag, "spinning");
        spinStart = Nbt.l(tag, "spinStart");
        startAngle = Nbt.f(tag, "startAngle");
        finalAngle = Nbt.f(tag, "finalAngle");
        restAngle = Nbt.f(tag, "restAngle");
        result = tag.contains("result") ? Nbt.i(tag, "result") : -1;
        spinner = Nbt.hasUuid(tag, "spinner") ? Nbt.uuid(tag, "spinner") : null;
        spinnerName = Nbt.s(tag, "spinnerName");
        flashEnd = Nbt.l(tag, "flashEnd");
        pending = tag.contains("pending") ? Prize.load(Nbt.c(tag, "pending")) : null;
    }
}
