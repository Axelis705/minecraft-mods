package com.navidad.bloque;

import com.axeliscore.npc.Npc;
import com.navidad.Navidad;
import com.navidad.gente.Gente;
import com.navidad.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Lo que guarda el letrero del puesto: dónde va el vendedor (si es el del puesto del mundo). Cada 10 s revisa que esté ahí. */
public class LetreroPinosBE extends BlockEntity {
    /** Dónde se para Don Abeto (relativo al letrero); null = letrero puesto a mano, no trae a nadie. */
    public BlockPos vendedor;
    private int espera = 20;

    public LetreroPinosBE(BlockPos p, BlockState s) {
        super(ModRegistry.LETRERO_PINOS_BE.get(), p, s);
    }

    void tick(Level l) {
        if (vendedor == null || !(l instanceof ServerLevel sl) || --espera > 0) {
            return;
        }
        espera = 200;
        BlockPos donde = worldPosition.offset(vendedor);
        if (!sl.isLoaded(donde)) {
            return;
        }
        for (net.minecraft.server.level.ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(donde.getX(), donde.getY(), donde.getZ()) < 10 * 10) {
                com.navidad.Logros.dar(p, "puesto");
            }
        }
        AABB caja = new AABB(donde).inflate(18);
        boolean esta = !sl.getEntitiesOfClass(Npc.class, caja, n -> n.def() == Gente.PINERO).isEmpty();
        if (!esta) {
            Npc n = Gente.poner(sl, Gente.PINERO, donde);
            if (n != null) {
                n.setPersistenceRequired();
                Navidad.LOGGER.info("NAVIDAD_PUESTO llega Don Abeto a {}", donde.toShortString());
            }
        }
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        vendedor = t.contains("VX") ? new BlockPos(t.getInt("VX"), t.getInt("VY"), t.getInt("VZ")) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        if (vendedor != null) {
            t.putInt("VX", vendedor.getX());
            t.putInt("VY", vendedor.getY());
            t.putInt("VZ", vendedor.getZ());
        }
    }
}
