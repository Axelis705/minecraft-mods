package com.casino.heist;

import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * La puerta redonda de la bóveda (el bloque del centro; el resto del hueco son sellos invisibles). Es el "corazón" de cada robo: en
 * cada tick mueve a {@link HeistManager}, guarda la lista de bloques del robo de su casino (puertas, caja fuerte, lectores, carritos,
 * notas, ductos) para dejarlos como estaban al terminar, y anima el giro y la apertura (en el cliente lo dibuja
 * {@code VaultDoorRenderer}).
 */
public class VaultDoorBE extends CasinoBE {
    /** Cuándo empezó a abrirse / cerrarse (sincronizado para la animación). */
    public long openStart = -1;
    public long closeStart = -1;
    /** Día de Minecraft desde el que se puede volver a robar. */
    private long readyDay;
    /** Hay algo movido (un robo empezó y no se ha arreglado todo). */
    private boolean dirty;
    private final List<BlockPos> points = new ArrayList<>();
    private boolean scanned;
    private BlockPos unlightPos;
    private long unlightAt;

    public VaultDoorBE(BlockPos pos, BlockState state) {
        super(HeistReg.VAULT_BE.get(), pos, state);
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(HeistBlocks.FACING) ? s.getValue(HeistBlocks.FACING) : Direction.NORTH;
    }

    @Override
    protected AABB renderBox() {
        return new AABB(worldPosition).inflate(4);
    }

    // ------------------------------------------------------------------ búsqueda

    public static VaultDoorBE at(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof VaultDoorBE v ? v : null;
    }

    /** La bóveda del casino donde está esta posición (busca en los bloques cargados del casino). */
    public static VaultDoorBE find(Level level, BlockPos near) {
        int[] z = HeistManager.zoneOf(level, near);
        VaultDoorBE best = null;
        double bd = Double.MAX_VALUE;
        for (int cx = z[0] >> 4; cx <= z[3] >> 4; cx++) {
            for (int cz = z[2] >> 4; cz <= z[5] >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof VaultDoorBE v) {
                        double d = v.worldPosition.distSqr(near);
                        if (d < bd) {
                            bd = d;
                            best = v;
                        }
                    }
                }
            }
        }
        return best;
    }

    public boolean ready(Level level) {
        return com.casino.plat.Plat.dayTime(level) / 24000L >= readyDay;
    }

    void setCooldown(Level level) {
        readyDay = com.casino.plat.Plat.dayTime(level) / 24000L + 1;
        setChanged();
    }

    void markDirty() {
        dirty = true;
        setChanged();
    }

    /** Las notas en orden (de oeste a este y de norte a sur): la primera da el primer número de la combinación. */
    int noteIndex(BlockPos pos) {
        scan();
        List<BlockPos> notes = new ArrayList<>();
        for (BlockPos p : points) {
            if (level.getBlockState(p).getBlock() instanceof HeistBlocks.Note) {
                notes.add(p);
            }
        }
        notes.sort(Comparator.<BlockPos>comparingInt(p -> p.getX()).thenComparingInt(p -> p.getZ()));
        int i = notes.indexOf(pos);
        return i < 0 ? 0 : Math.min(2, i * 3 / Math.max(1, notes.size()));
    }

    /** Busca una vez los bloques del robo dentro del casino. */
    private void scan() {
        if (scanned || level == null) {
            return;
        }
        scanned = true;
        if (!points.isEmpty()) {
            return;
        }
        int[] z = HeistManager.zoneOf(level, worldPosition);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = z[0]; x <= z[3]; x++) {
            for (int zz = z[2]; zz <= z[5]; zz++) {
                for (int y = z[1]; y <= Math.min(z[4], worldPosition.getY() + 36); y++) {
                    m.set(x, y, zz);
                    BlockState s = level.getBlockState(m);
                    if (s.getBlock() instanceof HeistBlocks.Facing || s.getBlock() instanceof HeistBlocks.Vent
                            || s.getBlock() == Blocks.IRON_DOOR && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                        if (!(s.getBlock() instanceof HeistBlocks.VaultDoor)) {
                            points.add(m.immutable());
                        }
                    }
                }
            }
        }
        setChanged();
    }

    // ------------------------------------------------------------------ la bóveda por dentro

    private Direction back() {
        return facing().getOpposite();
    }

    /** El cuarto de la bóveda (detrás de la puerta). */
    public AABB interior() {
        Direction b = back(), side = facing().getClockWise();
        BlockPos a = worldPosition.relative(b, 2).relative(side, 11).below(3);
        BlockPos c = worldPosition.relative(b, 14).relative(side, -11).above(3);
        return new AABB(Vec3.atLowerCornerOf(a), Vec3.atLowerCornerOf(c)).inflate(0.5);
    }

    public boolean inside(Vec3 p) {
        return interior().contains(p);
    }

    /** Las posiciones del hueco de la puerta (5×5, dos de grueso). */
    private List<BlockPos> seals() {
        List<BlockPos> out = new ArrayList<>();
        Direction side = facing().getClockWise();
        for (int d = 0; d <= 1; d++) {
            for (int s = -2; s <= 2; s++) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos p = worldPosition.relative(back(), d).relative(side, s).above(y);
                    if (!p.equals(worldPosition)) {
                        out.add(p);
                    }
                }
            }
        }
        return out;
    }

    void startOpen(long now) {
        openStart = now;
        closeStart = -1;
        sync();
        level.playSound(null, worldPosition, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 2F, 0.5F);
    }

    void startClose(long now) {
        closeStart = now;
        sync();
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 2F, 0.5F);
    }

    void setGas(boolean on) {
        scan();
        for (BlockPos p : points) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof HeistBlocks.Vent && s.getValue(HeistBlocks.Vent.GAS) != on) {
                level.setBlock(p, s.setValue(HeistBlocks.Vent.GAS, on), 3);
            }
        }
        if (on) {
            level.playSound(null, worldPosition.relative(back(), 6), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 2F, 0.4F);
        }
    }

    void unlightLater(BlockPos pos, long at) {
        unlightPos = pos.immutable();
        unlightAt = at;
    }

    private void setSeals(boolean solid) {
        BlockState seal = HeistReg.VAULT_SEAL.get().defaultBlockState();
        for (BlockPos p : seals()) {
            BlockState s = level.getBlockState(p);
            if (solid && s.isAir()) {
                level.setBlock(p, seal, 3);
            } else if (!solid && s.getBlock() == seal.getBlock()) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        BlockState me = getBlockState();
        if (me.getValue(HeistBlocks.OPEN) == solid) {
            level.setBlock(worldPosition, me.setValue(HeistBlocks.OPEN, !solid), 3);
        }
    }

    /** Deja todo como estaba: puertas cerradas, caja fuerte cerrada, lectores apagados, carritos llenos, guardias de vuelta. */
    void reset(Level level, HeistManager.Heist h) {
        scan();
        for (BlockPos p : points) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() == Blocks.IRON_DOOR) {
                HeistManager.setDoor(level, p, false);
            } else if (s.getBlock() instanceof HeistBlocks.Safe && s.getValue(HeistBlocks.OPEN)) {
                level.setBlock(p, s.setValue(HeistBlocks.OPEN, false), 3);
            } else if (s.getBlock() instanceof HeistBlocks.Reader && s.getValue(HeistBlocks.LIT)) {
                level.setBlock(p, s.setValue(HeistBlocks.LIT, false), 3);
            } else if (s.getBlock() instanceof HeistBlocks.Cart && s.getValue(HeistBlocks.Cart.LEFT) < 3) {
                level.setBlock(p, s.setValue(HeistBlocks.Cart.LEFT, 3), 3);
            } else if (s.getBlock() instanceof HeistBlocks.Vent && s.getValue(HeistBlocks.Vent.GAS)) {
                level.setBlock(p, s.setValue(HeistBlocks.Vent.GAS, false), 3);
            }
        }
        setSeals(true);
        openStart = -1;
        closeStart = -1;
        if (h != null) {
            GuardAI.restore(level, h);
        }
        GuardAI.calm(level, worldPosition);
        dirty = false;
        sync();
    }

    // ------------------------------------------------------------------ ticks

    void serverTick() {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        HeistManager.Heist h = HeistManager.byVault(level, worldPosition);
        if (h != null) {
            HeistManager.tick(level, this, h);
        } else if (dirty && now % 20 == 0) {
            // un robo que se quedó a medias (se apagó el servidor): arreglar
            reset(level, null);
        }
        if (openStart >= 0 && now - openStart == HeistManager.OPEN_TICKS - 40) {
            setSeals(false);
            level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 2F, 0.4F);
        }
        if (openStart >= 0 && (now - openStart) % 10 == 0 && now - openStart < HeistManager.OPEN_TICKS - 40) {
            level.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.5F, 0.5F);
        }
        if (closeStart >= 0 && now - closeStart == HeistManager.CLOSE_TICKS) {
            setSeals(true);
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.5F, 0.5F);
        }
        if (unlightPos != null && now >= unlightAt) {
            BlockState s = level.getBlockState(unlightPos);
            if (s.getBlock() instanceof HeistBlocks.Reader && s.getValue(HeistBlocks.LIT) && (h == null || h.phase == HeistManager.Phase.STEALTH)) {
                level.setBlock(unlightPos, s.setValue(HeistBlocks.LIT, false), 3);
            }
            unlightPos = null;
        }
    }

    void clientTick() {
    }

    /** Qué tan abierta está (0 cerrada .. 1 abierta) y el giro de la rueda (radianes), para el dibujo. */
    public float[] anim(float time) {
        float open = 0, wheel = 0;
        if (openStart >= 0) {
            float t = time - openStart;
            float spin = Math.min(t, HeistManager.OPEN_TICKS - 40);
            wheel = spin * 0.12F;
            open = Math.max(0, Math.min(1, (t - (HeistManager.OPEN_TICKS - 40)) / 40F));
            open = open * open * (3 - 2 * open);
            if (closeStart >= 0) {
                float c = Math.max(0, Math.min(1, (time - closeStart) / HeistManager.CLOSE_TICKS));
                open = Math.min(open, 1 - c * c);
                wheel -= Math.max(0, time - closeStart) * 0.2F;
            }
        }
        return new float[]{open, wheel};
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void writeData(CompoundTag tag) {
        tag.putLong("open", openStart);
        tag.putLong("close", closeStart);
        tag.putLong("ready", readyDay);
        tag.putBoolean("dirty", dirty);
        ListTag l = new ListTag();
        for (BlockPos p : points) {
            l.add(LongTag.valueOf(p.asLong()));
        }
        tag.put("points", l);
    }

    @Override
    protected CompoundTag syncTag(CompoundTag full) {
        CompoundTag t = new CompoundTag();
        t.putLong("open", openStart);
        t.putLong("close", closeStart);
        return t;
    }

    @Override
    protected void readData(CompoundTag tag) {
        openStart = Nbt.has(tag, "open") ? Nbt.l(tag, "open") : -1;
        closeStart = Nbt.has(tag, "close") ? Nbt.l(tag, "close") : -1;
        if (Nbt.has(tag, "ready")) {
            readyDay = Nbt.l(tag, "ready");
            dirty = Nbt.b(tag, "dirty");
            points.clear();
            ListTag l = Nbt.list(tag, "points");
            for (int i = 0; i < l.size(); i++) {
                points.add(BlockPos.of(Nbt.atLong(l, i)));
            }
        }
    }
}
