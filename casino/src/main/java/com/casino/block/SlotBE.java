package com.casino.block;

import com.casino.chip.Chips;
import com.casino.data.CasinoData;
import com.casino.game.Prize;
import com.casino.game.SlotLayout;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * El corazón de la tragamonedas (mitad de abajo). El servidor cobra la apuesta y sortea en qué posición se detiene cada carrete en el
 * momento de tirar de la palanca; paga al terminar la animación. El cliente solo anima con el reloj del juego.
 */
public class SlotBE extends BlockEntity {
    /** Premio gordo (tres coronas en una línea), cambiable agachado + clic con un objeto en la mano. */
    public ItemStack jackpot = new ItemStack(Items.DIAMOND_BLOCK, 2);

    public boolean spinning = false;
    public long spinStart;
    public final int[] stops = {0, 0, 0};
    public int bet;
    public int units;
    public int lineMask;
    public boolean jackpotHit;
    @Nullable
    public UUID spinner;
    public String spinnerName = "";
    /** Hasta cuándo (reloj del juego) se resaltan las líneas ganadoras. */
    public long flashEnd = 0;
    public int flashMask = 0;
    public int flashPayout = 0;
    public int spinSeq = 0;
    public int endSeq = 0;

    // ---- solo cliente
    private boolean clientInit = false;
    private int clientSpinSeq = 0;
    private int clientEndSeq = 0;
    private final boolean[] stopped = new boolean[3];
    private int lastTickSound = 0;

    public SlotBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLOT.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(SlotBlock.FACING);
    }

    /** Centro de la pantalla de los carretes en el mundo. */
    public Vec3 screenCenter() {
        Direction f = facing();
        return new Vec3(worldPosition.getX() + 0.5 + f.getStepX() * 0.36, worldPosition.getY() + 1.45, worldPosition.getZ() + 0.5 + f.getStepZ() * 0.36);
    }

    // ------------------------------------------------------------------ servidor

    private boolean canEditJackpot(ServerPlayer p) {
        return p.hasPermissions(2) || p.isCreative() || (p.getServer() != null && p.getServer().isSingleplayerOwner(p.getGameProfile()));
    }

    /** Agachado: con un objeto en la mano lo pone de premio gordo; con la mano vacía muestra el premio actual. */
    public void interactJackpot(ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        if (!held.isEmpty()) {
            if (spinning) {
                p.displayClientMessage(Component.translatable("casino.slot.busy").withStyle(ChatFormatting.RED), true);
            } else if (canEditJackpot(p)) {
                ItemStack s = held.copy();
                s.setCount(Math.min(held.getCount(), s.getMaxStackSize()));
                jackpot = s;
                sync();
                level.playSound(null, worldPosition, ModSounds.PRIZE_SET.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                p.displayClientMessage(Component.translatable("casino.slot.prize_set", s.getCount() + " x ", s.getHoverName()).withStyle(ChatFormatting.GOLD), true);
            } else {
                p.displayClientMessage(Component.translatable("casino.wheel.no_permission").withStyle(ChatFormatting.RED), true);
            }
        } else {
            p.displayClientMessage(Component.translatable("casino.slot.prize_info", jackpot.getCount() + " x ", jackpot.getHoverName()).withStyle(ChatFormatting.GOLD), true);
        }
    }

    /** Un jugador tira de la palanca. Todas las comprobaciones se hacen aquí (el cliente solo pide). */
    public void spin(ServerPlayer p, int betChips) {
        if (level == null || !SlotLayout.validBet(betChips)) {
            return;
        }
        if (spinning) {
            p.displayClientMessage(Component.translatable("casino.slot.busy").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (!Chips.take(p, betChips)) {
            p.displayClientMessage(Component.translatable("casino.plinko.no_chips", betChips).withStyle(ChatFormatting.RED), true);
            level.playSound(null, worldPosition, ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            return;
        }
        for (int i = 0; i < SlotLayout.REELS; i++) {
            stops[i] = level.random.nextInt(SlotLayout.STRIP);
        }
        SlotLayout.Result r = SlotLayout.evaluate(stops);
        bet = betChips;
        units = r.units;
        lineMask = r.lineMask;
        jackpotHit = r.jackpot;
        spinner = p.getUUID();
        spinnerName = p.getGameProfile().getName();
        spinStart = level.getGameTime();
        spinning = true;
        flashEnd = 0;
        spinSeq++;
        level.playSound(null, worldPosition, ModSounds.SLOT_LEVER.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
        sync();
    }

    public void serverTick() {
        if (level == null || !spinning) {
            return;
        }
        long now = level.getGameTime();
        if (now >= spinStart + SlotLayout.END_TICK) {
            finish(now);
        }
    }

    private void finish(long now) {
        spinning = false;
        int payout = SlotLayout.payout(bet, units);
        flashMask = lineMask;
        flashPayout = payout;
        flashEnd = now + 60;
        endSeq++;
        if (level instanceof ServerLevel sl && spinner != null) {
            boolean won = payout > 0 || jackpotHit;
            if (won) {
                List<ItemStack> items = new ArrayList<>(Chips.stacksFor(payout));
                if (jackpotHit && !jackpot.isEmpty()) {
                    items.add(jackpot.copy());
                }
                boolean big = jackpotHit || units >= 80;
                Component title = jackpotHit ? Component.translatable("casino.slot.jackpot") : Component.translatable("casino.slot.banner", payout);
                Prize prize = new Prize(title, items, 0, -1, big);
                ServerPlayer p = sl.getServer().getPlayerList().getPlayer(spinner);
                if (p != null) {
                    prize.deliver(p);
                    p.displayClientMessage(Component.translatable("casino.slot.win", payout).withStyle(ChatFormatting.GREEN), true);
                    if (units >= 32 || jackpotHit) {
                        CasinoNet.sendTo(p, new WheelWinPacket(prize));
                    }
                } else {
                    CasinoData.get(sl.getServer()).addPending(spinner, prize);
                }
                if (units >= 200 || jackpotHit) {
                    Component what = jackpotHit
                            ? Component.translatable("casino.slot.won_jackpot", jackpot.getCount() + " x ", jackpot.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE)
                            : Component.literal(payout + "").withStyle(ChatFormatting.GOLD);
                    Component msg = Component.translatable(jackpotHit ? "casino.slot.won_big" : "casino.slot.won", Component.literal(spinnerName).withStyle(ChatFormatting.YELLOW), what)
                            .withStyle(ChatFormatting.GRAY);
                    Vec3 h = screenCenter();
                    for (ServerPlayer o : sl.players()) {
                        if (o.distanceToSqr(h.x, h.y, h.z) < 32 * 32) {
                            o.sendSystemMessage(msg);
                        }
                    }
                }
            }
        }
        sync();
    }

    // ------------------------------------------------------------------ cliente

    public void clientTick() {
        if (level == null) {
            return;
        }
        RandomSource r = level.random;
        long now = level.getGameTime();
        if (!clientInit) {
            clientInit = true;
            clientSpinSeq = spinSeq;
            clientEndSeq = endSeq;
            return;
        }
        if (spinSeq != clientSpinSeq) {
            clientSpinSeq = spinSeq;
            java.util.Arrays.fill(stopped, false);
            lastTickSound = 0;
        }
        Vec3 c = screenCenter();
        if (spinning) {
            long t = now - spinStart;
            if (t / 3 != lastTickSound && t < SlotLayout.STOP_TICK[2]) {
                lastTickSound = (int) (t / 3);
                level.playLocalSound(c.x, c.y, c.z, ModSounds.SLOT_TICK.get(), SoundSource.BLOCKS, 0.35F, 0.9F + (lastTickSound % 4) * 0.05F, false);
            }
            for (int i = 0; i < SlotLayout.REELS; i++) {
                if (!stopped[i] && t >= SlotLayout.STOP_TICK[i]) {
                    stopped[i] = true;
                    level.playLocalSound(c.x, c.y, c.z, ModSounds.SLOT_STOP.get(), SoundSource.BLOCKS, 0.7F, 0.9F + i * 0.12F, false);
                }
            }
        }
        if (endSeq != clientEndSeq) {
            clientEndSeq = endSeq;
            int pay = flashPayout;
            if (pay > 0 || jackpotHit) {
                boolean big = jackpotHit;
                level.playLocalSound(c.x, c.y, c.z, big ? ModSounds.WHEEL_JACKPOT.get() : units >= 32 ? ModSounds.WHEEL_WIN.get() : ModSounds.COINS.get(),
                        SoundSource.BLOCKS, big ? 1.2F : 0.9F, 1.0F, false);
                int n = big ? 60 : units >= 80 ? 30 : units >= 32 ? 14 : 5;
                Direction f = facing();
                for (int k = 0; k < n; k++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double sp = 0.05 + r.nextDouble() * 0.15;
                    level.addParticle(big && k % 3 == 0 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.FIREWORK, c.x + f.getStepX() * 0.2, c.y, c.z + f.getStepZ() * 0.2,
                            Math.cos(a) * sp, Math.abs(Math.sin(a)) * sp + 0.04, Math.sin(a) * sp);
                }
            }
        }
    }

    // ------------------------------------------------------------------ red y guardado

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 3, 2));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag t = pkt.getTag();
        if (t != null) {
            load(t);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("jackpot", jackpot.save(new CompoundTag()));
        tag.putBoolean("spinning", spinning);
        tag.putLong("spinStart", spinStart);
        tag.putIntArray("stops", stops);
        tag.putInt("bet", bet);
        tag.putInt("units", units);
        tag.putInt("lineMask", lineMask);
        tag.putBoolean("jackpotHit", jackpotHit);
        if (spinner != null) {
            tag.putUUID("spinner", spinner);
        }
        tag.putString("spinnerName", spinnerName);
        tag.putLong("flashEnd", flashEnd);
        tag.putInt("flashMask", flashMask);
        tag.putInt("flashPayout", flashPayout);
        tag.putInt("spinSeq", spinSeq);
        tag.putInt("endSeq", endSeq);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ItemStack j = tag.contains("jackpot") ? ItemStack.of(tag.getCompound("jackpot")) : ItemStack.EMPTY;
        jackpot = j.isEmpty() ? new ItemStack(Items.DIAMOND_BLOCK, 2) : j;
        spinning = tag.getBoolean("spinning");
        spinStart = tag.getLong("spinStart");
        int[] s = tag.getIntArray("stops");
        for (int i = 0; i < 3; i++) {
            stops[i] = i < s.length ? SlotLayout.mod(s[i]) : 0;
        }
        bet = tag.getInt("bet");
        units = tag.getInt("units");
        lineMask = tag.getInt("lineMask");
        jackpotHit = tag.getBoolean("jackpotHit");
        spinner = tag.hasUUID("spinner") ? tag.getUUID("spinner") : null;
        spinnerName = tag.getString("spinnerName");
        flashEnd = tag.getLong("flashEnd");
        flashMask = tag.getInt("flashMask");
        flashPayout = tag.getInt("flashPayout");
        spinSeq = tag.getInt("spinSeq");
        endSeq = tag.getInt("endSeq");
    }
}
