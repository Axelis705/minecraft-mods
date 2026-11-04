package com.mundosdeaxelis.grave;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.block.GraveBlockEntity;
import com.mundosdeaxelis.network.GravePacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.registry.ModBlocks;
import com.mundosdeaxelis.table.Bound;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Muerte del jugador: los objetos vinculados vuelven con él al reaparecer y todo lo demás se guarda 15 minutos en una lápida
 * (la experiencia no se guarda). Si no hay sitio para la lápida, se pierde como siempre (cae al suelo).
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class GraveEvents {
    private GraveEvents() {
    }

    private static final String KEEP = "mdc_bound_keep";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent e) {
        if (e.isCanceled() || !(e.getEntity() instanceof ServerPlayer sp) || sp.isSpectator()) {
            return;
        }
        ServerLevel sl = sp.serverLevel();
        if (sl.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        Inventory inv = sp.getInventory();
        // 1) lo vinculado se aparta para devolverlo al reaparecer
        ListTag keep = new ListTag();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (Bound.isBoundTo(st, sp)) {
                CompoundTag c = new CompoundTag();
                c.putInt("Slot", i);
                c.put("Item", st.save(new CompoundTag()));
                keep.add(c);
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (!keep.isEmpty()) {
            sp.getPersistentData().put(KEEP, keep);
        }
        // 2) el resto va a la lápida
        List<GraveBlockEntity.Stored> stored = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty()) {
                stored.add(new GraveBlockEntity.Stored(i, st.copy()));
            }
        }
        if (stored.isEmpty()) {
            return;
        }
        // Si muere dentro de una dungeon/arena de jefe, la lápida se pone FUERA: donde entró (o el punto de reaparición).
        ServerLevel gl = sl;
        BlockPos spot = null;
        boolean outside = false;
        if (com.mundosdeaxelis.world.BossTravel.inArena(sl)) {
            net.minecraft.core.GlobalPos ret = com.mundosdeaxelis.world.BossTravel.returnSpot(sp);
            if (ret != null) {
                ServerLevel rl = sl.getServer().getLevel(ret.dimension());
                if (rl != null && !com.mundosdeaxelis.world.BossTravel.inArena(rl)) {
                    BlockPos s = findSpot(rl, ret.pos());
                    if (s != null) {
                        gl = rl;
                        spot = s;
                        outside = true;
                    }
                }
            }
            if (!outside) {
                ServerLevel ov = sl.getServer().overworld();
                BlockPos rp = sp.getRespawnPosition();
                BlockPos base = rp != null && sp.getRespawnDimension().equals(ov.dimension()) ? rp : ov.getSharedSpawnPos();
                BlockPos s = findSpot(ov, base);
                if (s != null) {
                    gl = ov;
                    spot = s;
                    outside = true;
                }
            }
            com.mundosdeaxelis.world.BossTravel.clearReturn(sp);
        }
        if (spot == null) {
            spot = findSpot(sl, sp.blockPosition());
        }
        if (spot == null) {
            return;
        }
        sl = gl;
        BlockState gs = ModBlocks.GRAVE.get().defaultBlockState().setValue(com.mundosdeaxelis.block.GraveBlock.FACING, sp.getDirection().getOpposite());
        sl.setBlock(spot, gs, 3);
        if (!(sl.getBlockEntity(spot) instanceof GraveBlockEntity be)) {
            sl.removeBlock(spot, false);
            return;
        }
        long expire = sl.getGameTime() + GraveBlockEntity.LIFETIME;
        be.setup(sp, stored, expire);
        for (int i = 0; i < inv.getContainerSize(); i++) {
            inv.setItem(i, ItemStack.EMPTY);
        }
        GraveData.get(sl.getServer()).add(new GraveData.Entry(sp.getUUID(), sl.dimension().location(), spot, expire));
        sl.sendParticles(ParticleTypes.SOUL, spot.getX() + 0.5, spot.getY() + 0.8, spot.getZ() + 0.5, 30, 0.4, 0.6, 0.4, 0.05);
        sl.playSound(null, spot, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0F, 0.7F);
        sp.sendSystemMessage(Component.translatable(outside ? "message.mundos_de_axelis.grave.created_outside" : "message.mundos_de_axelis.grave.created",
                        spot.getX(), spot.getY(), spot.getZ())
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        sync(sp);
    }

    /** Un sitio libre cerca de donde murió (no vale el vacío). */
    private static BlockPos findSpot(ServerLevel sl, BlockPos from) {
        int minY = sl.getMinBuildHeight() + 1, maxY = sl.getMaxBuildHeight() - 2;
        if (from.getY() < minY) {
            return null;
        }
        int y0 = Math.min(from.getY(), maxY);
        int[] dys = {0, 1, -1, 2, -2, 3, 4, -3};
        for (int r = 0; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int dy : dys) {
                        int y = y0 + dy;
                        if (y < minY || y > maxY) {
                            continue;
                        }
                        BlockPos p = new BlockPos(from.getX() + dx, y, from.getZ() + dz);
                        BlockState st = sl.getBlockState(p);
                        if (st.isAir() || st.canBeReplaced()) {
                            return p;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Una lápida apartada mientras se reconstruye la dungeon (su bloque, sus datos y a quién pertenece). */
    public record Pending(BlockState state, CompoundTag data, UUID owner, long expire) {
    }

    /**
     * Aparta (sin soltar nada) todas las lápidas de ese mundo: se usa antes de borrar y volver a construir la dungeon, para que lo
     * que guardan no caiga al suelo ni se pierda. Se devuelven con {@link #attach}.
     */
    public static List<Pending> detach(ServerLevel sl) {
        List<Pending> out = new ArrayList<>();
        GraveData gd = GraveData.get(sl.getServer());
        for (GraveData.Entry e : gd.inDimension(sl.dimension().location())) {
            BlockState st = sl.getBlockState(e.pos());
            if (st.is(ModBlocks.GRAVE.get()) && sl.getBlockEntity(e.pos()) instanceof GraveBlockEntity be) {
                be.markMoving();
                out.add(new Pending(st, be.saveWithFullMetadata(), e.owner(), e.expire()));
                sl.removeBlock(e.pos(), false);
            }
            gd.remove(e.dim(), e.pos());
        }
        return out;
    }

    /** Vuelve a poner las lápidas apartadas en sitios libres cerca de {@code center} (con lo que guardaban) y avisa a sus dueños. */
    public static void attach(ServerLevel sl, List<Pending> list, BlockPos center) {
        GraveData gd = GraveData.get(sl.getServer());
        for (Pending p : list) {
            BlockPos spot = freeNear(sl, center);
            if (spot == null) {
                spot = center;
            }
            sl.setBlock(spot, p.state(), 3);
            if (sl.getBlockEntity(spot) instanceof GraveBlockEntity be) {
                be.load(p.data());
                be.setChanged();
            }
            gd.add(new GraveData.Entry(p.owner(), sl.dimension().location(), spot, p.expire()));
            ServerPlayer owner = p.owner() == null ? null : sl.getServer().getPlayerList().getPlayer(p.owner());
            if (owner != null) {
                sync(owner);
            }
        }
    }

    /** Sitio libre con suelo firme, a 3-8 bloques del centro (para no tapar donde aparecen los jugadores). */
    private static BlockPos freeNear(ServerLevel sl, BlockPos center) {
        for (int r = 3; r <= 8; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos p = center.offset(dx, 0, dz);
                    if ((sl.getBlockState(p).isAir() || sl.getBlockState(p).canBeReplaced()) && sl.getBlockState(p.above()).isAir()
                            && sl.getBlockState(p.below()).isSolid()) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    /** Quita la lápida (recogida o caducada), la borra de la lista y avisa a su dueño. */
    public static void remove(ServerLevel sl, BlockPos pos, UUID owner) {
        GraveData.get(sl.getServer()).remove(sl.dimension().location(), pos);
        if (sl.getBlockState(pos).is(ModBlocks.GRAVE.get())) {
            sl.removeBlock(pos, false);
        }
        if (owner != null) {
            ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(owner);
            if (sp != null) {
                sync(sp);
            }
        }
    }

    /** Manda al jugador sus lápidas activas. */
    public static void sync(ServerPlayer sp) {
        long now = sp.serverLevel().getGameTime();
        List<GravePacket.Item> l = new ArrayList<>();
        for (GraveData.Entry e : GraveData.get(sp.server).of(sp.getUUID(), now)) {
            l.add(new GravePacket.Item(e.dim(), e.pos(), (int) Math.max(0, e.expire() - now)));
        }
        ModNetwork.sendTo(sp, new GravePacket(l));
    }

    /** Al reaparecer, los objetos vinculados vuelven a su ranura. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (!e.isWasDeath() || !(e.getEntity() instanceof ServerPlayer np)) {
            return;
        }
        CompoundTag od = e.getOriginal().getPersistentData();
        if (!od.contains(KEEP)) {
            return;
        }
        ListTag l = od.getList(KEEP, Tag.TAG_COMPOUND);
        od.remove(KEEP);
        Inventory inv = np.getInventory();
        for (Tag t : l) {
            CompoundTag c = (CompoundTag) t;
            ItemStack st = ItemStack.of(c.getCompound("Item"));
            int slot = c.getInt("Slot");
            if (slot >= 0 && slot < inv.getContainerSize() && inv.getItem(slot).isEmpty()) {
                inv.setItem(slot, st);
            } else if (!inv.add(st)) {
                np.drop(st, false);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
        }
    }
}
