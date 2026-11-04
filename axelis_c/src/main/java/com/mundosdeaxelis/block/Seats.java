package com.mundosdeaxelis.block;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Asientos: al sentarte en un banco se crea un soporte invisible (un soporte de armadura "marcador") sobre el que montas. Se
 * borra solo cuando te levantas, si falta el banco o si el servidor se reinicia con alguien sentado.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Seats {
    private Seats() {
    }

    public static final String TAG = "mdc_seat";
    /** Altura del soporte sobre la base del bloque (el jugador montado queda unos 0,35 más abajo). */
    private static final double SEAT_Y = 0.14D;
    private static final List<ArmorStand> ACTIVE = new ArrayList<>();

    public static boolean sit(Player player, ServerLevel level, BlockPos pos, Direction facing) {
        if (player.isPassenger() || player.isSpectator()) {
            return false;
        }
        for (ArmorStand s : ACTIVE) {
            if (!s.isRemoved() && s.level() == level && s.blockPosition().equals(pos)) {
                return false;
            }
        }
        ArmorStand st = new ArmorStand(level, pos.getX() + 0.5D, pos.getY() + SEAT_Y, pos.getZ() + 0.5D);
        CompoundTag t = st.saveWithoutId(new CompoundTag());
        t.putBoolean("Marker", true);
        t.putBoolean("Invisible", true);
        t.putBoolean("NoGravity", true);
        t.putBoolean("Invulnerable", true);
        st.load(t);
        st.addTag(TAG);
        st.setYRot(facing.toYRot());
        ACTIVE.add(st);
        level.addFreshEntity(st);
        player.setYRot(facing.toYRot());
        if (!player.startRiding(st, true)) {
            st.discard();
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }
        Iterator<ArmorStand> it = ACTIVE.iterator();
        while (it.hasNext()) {
            ArmorStand s = it.next();
            if (s.isRemoved()) {
                it.remove();
                continue;
            }
            boolean noBench = !(s.level().getBlockState(s.blockPosition()).getBlock() instanceof BenchBlock);
            if ((s.getPassengers().isEmpty() && s.tickCount > 10) || noBench) {
                s.ejectPassengers();
                s.discard();
                it.remove();
            }
        }
    }

    /** Un soporte de asiento que vuelve a cargarse desde el disco (servidor reiniciado) ya no sirve: se descarta. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide && e.getEntity() instanceof ArmorStand a && a.getTags().contains(TAG) && !ACTIVE.contains(a)) {
            e.setCanceled(true);
        }
    }
}
