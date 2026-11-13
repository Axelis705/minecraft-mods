package com.casino;

import com.casino.data.CasinoData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Los casinos construidos (por el mundo o con /casino build) quedan protegidos: en supervivencia y aventura nadie puede romper sus
 * bloques (ni llevarse el diamante de la vitrina) y las explosiones no los dañan. En creativo sí se puede editar.
 */
@Mod.EventBusSubscriber(modid = Casino.MODID)
public final class Protection {
    private Protection() {
    }

    public static String dim(Level level) {
        return level.dimension().toString();
    }

    /** Registra la caja de un casino recién construido. */
    public static void protect(MinecraftServer server, Level level, BoundingBox b) {
        if (server != null) {
            CasinoData.get(server).addZone(dim(level), new int[]{b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()});
        }
    }

    public static boolean protectedAt(Level level, BlockPos pos) {
        MinecraftServer s = level.getServer();
        return s != null && !level.isClientSide() && CasinoData.get(s).inZone(dim(level), pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean blocks(Level level, BlockPos pos, Player p) {
        if (p != null && (p.isCreative() || p.isSpectator())) {
            return false;
        }
        return protectedAt(level, pos);
    }

    //#if MC >= 26.1
    //$$ @SubscribeEvent
    //$$ public static boolean onBreak(BlockEvent.BreakEvent e) {
    //$$     return e.getLevel() instanceof Level l && blocks(l, e.getPos(), e.getPlayer());
    //$$ }
    //#else
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.getLevel() instanceof Level l && blocks(l, e.getPos(), e.getPlayer())) {
            e.setCanceled(true);
        }
    }
    //#endif

    @SubscribeEvent
    public static void onExplode(ExplosionEvent.Detonate e) {
        Level l = e.getLevel();
        if (!l.isClientSide()) {
            e.getAffectedBlocks().removeIf(p -> protectedAt(l, p));
        }
    }
}
