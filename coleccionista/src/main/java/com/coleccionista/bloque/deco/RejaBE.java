package com.coleccionista.bloque.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Lo que lleva la reja de la celda para moverse: qué tan abierta va (solo en el cliente, no se guarda). El estado del bloque (abierta o
 * cerrada) lo pone el evento; aquí la reja lo alcanza poco a poco (en poco más de un segundo) y suena: el pestillo, el riel y el golpe al
 * final.
 */
public class RejaBE extends BlockEntity {
    /** Lo que avanza por tick (1 = toda la carrera). */
    public static final float VEL = 1F / 24F;
    /** 0 cerrada, 1 abierta. */
    float prog, antes;
    private boolean listo;
    private int pasos;

    public RejaBE(BlockPos p, BlockState s) {
        super(Deco.REJA_BE, p, s);
    }

    public float avance(float pt) {
        return antes + (prog - antes) * pt;
    }

    void tickCliente(Level l, BlockPos p, BlockState s) {
        float meta = s.getValue(DoorBlock.OPEN) ? 1F : 0F;
        antes = prog;
        if (!listo) {
            // recién cargada: ya está donde debe (sin animación)
            prog = antes = meta;
            listo = true;
            return;
        }
        if (prog == meta) {
            return;
        }
        boolean abre = meta > prog;
        boolean asc = s.getBlock() instanceof DecoReja r && r.ascensor;
        if (prog == 0F || prog == 1F) {
            if (asc) {
                // la campanita del ascensor y el motor de la puerta
                if (abre) {
                    sonar(l, p, SoundEvents.NOTE_BLOCK_BELL.get(), 0.7F, 1.2F);
                }
                sonar(l, p, abre ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, 0.35F, 0.55F);
            } else {
                // arranca: el pestillo y el chirrido del riel
                sonar(l, p, abre ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, 0.9F, 0.55F);
                sonar(l, p, SoundEvents.GRINDSTONE_USE, 0.35F, 0.5F);
            }
            pasos = 0;
        }
        prog = abre ? Math.min(meta, prog + VEL) : Math.max(meta, prog - VEL);
        // las ruedas en el riel
        if (!asc && ++pasos % 4 == 0 && prog > 0.12F && prog < 0.95F) {
            sonar(l, p, SoundEvents.CHAIN_STEP, 0.45F, 0.55F + l.random.nextFloat() * 0.15F);
        }
        if (prog == meta) {
            // el golpe al llegar al tope
            sonar(l, p, SoundEvents.IRON_TRAPDOOR_CLOSE, asc ? 0.5F : 1F, abre ? 0.5F : 0.42F);
            if (!asc) {
                sonar(l, p, SoundEvents.ANVIL_LAND, 0.12F, abre ? 1.5F : 1.3F);
            }
        }
    }

    private static void sonar(Level l, BlockPos p, SoundEvent e, float vol, float tono) {
        l.playLocalSound(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, e, SoundSource.BLOCKS, vol, tono, false);
    }

    /** La reja ocupa dos bloques de alto y abierta se mete en el bloque de al lado. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 2, 2));
    }
}
