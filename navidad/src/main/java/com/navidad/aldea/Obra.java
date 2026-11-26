package com.navidad.aldea;

import com.navidad.bloque.deco.Deco;
import com.navidad.bloque.deco.DecoBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.registries.ForgeRegistries;

/** Herramientas para construir con coordenadas relativas a un origen (x al este, z al sur, y 0 = el primer bloque sobre el piso). */
final class Obra {
    final ServerLevel l;
    final BlockPos o;
    int puestos;

    Obra(ServerLevel l, BlockPos o) {
        this.l = l;
        this.o = o;
    }

    BlockPos p(int x, int y, int z) {
        return o.offset(x, y, z);
    }

    static BlockState b(String id) {
        Block bl = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id.contains(":") ? id : "minecraft:" + id));
        return (bl == null ? Blocks.STONE : bl).defaultBlockState();
    }

    static BlockState nav(String id) {
        Block bl = Deco.block(id);
        if (bl == null) {
            bl = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("navidad", id));
        }
        return (bl == null ? Blocks.SNOW_BLOCK : bl).defaultBlockState();
    }

    void set(int x, int y, int z, BlockState s) {
        l.setBlock(p(x, y, z), s, 2);
        puestos++;
    }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    set(x, y, z, s);
                }
            }
        }
    }

    /** Paredes (sin piso ni techo). */
    void paredes(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        fill(x0, y0, z0, x1, y1, z0, s);
        fill(x0, y0, z1, x1, y1, z1, s);
        fill(x0, y0, z0, x0, y1, z1, s);
        fill(x1, y0, z0, x1, y1, z1, s);
    }

    /** Un adorno mirando hacia f (los altos ponen sus dos mitades). */
    void deco(int x, int y, int z, String id, Direction f) {
        BlockState s = nav(id);
        if (s.hasProperty(DecoBlock.FACING)) {
            s = s.setValue(DecoBlock.FACING, f);
        }
        if (s.hasProperty(DecoBlock.HALF)) {
            set(x, y, z, s.setValue(DecoBlock.HALF, DoubleBlockHalf.LOWER));
            set(x, y + 1, z, s.setValue(DecoBlock.HALF, DoubleBlockHalf.UPPER));
            return;
        }
        set(x, y, z, s);
    }

    void bloqueNav(int x, int y, int z, String id) {
        set(x, y, z, nav(id));
    }

    void puerta(int x, int y, int z, String id, Direction f) {
        BlockState s = b(id).setValue(DoorBlock.FACING, f).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        set(x, y, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, y + 1, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    void escalon(int x, int y, int z, String id, Direction f) {
        set(x, y, z, b(id).setValue(StairBlock.FACING, f));
    }

    /**
     * Techo de dos aguas sobre paredes de x0..x1 y z0..z1 (con alero de un bloque). La cumbrera corre a lo largo del eje x (aguas al norte y
     * al sur) o del eje z. Rellena los triángulos de los lados con {@code pared}.
     */
    void techo(int x0, int x1, int z0, int z1, int y, boolean cumbreraEnX, String escalera, BlockState remate, BlockState pared) {
        for (int k = 0; ; k++) {
            int a = (cumbreraEnX ? z0 : x0) - 1 + k, c = (cumbreraEnX ? z1 : x1) + 1 - k;
            if (a > c) {
                break;
            }
            int lo = cumbreraEnX ? x0 - 1 : z0 - 1, hi = cumbreraEnX ? x1 + 1 : z1 + 1;
            for (int t = lo; t <= hi; t++) {
                if (a == c) {
                    if (cumbreraEnX) {
                        set(t, y + k, a, remate);
                    } else {
                        set(a, y + k, t, remate);
                    }
                } else if (cumbreraEnX) {
                    escalon(t, y + k, a, escalera, Direction.SOUTH);
                    escalon(t, y + k, c, escalera, Direction.NORTH);
                } else {
                    escalon(a, y + k, t, escalera, Direction.EAST);
                    escalon(c, y + k, t, escalera, Direction.WEST);
                }
            }
            if (c - a >= 2) {
                if (cumbreraEnX) {
                    fill(x0, y + k, a + 1, x0, y + k, c - 1, pared);
                    fill(x1, y + k, a + 1, x1, y + k, c - 1, pared);
                } else {
                    fill(a + 1, y + k, z0, c - 1, y + k, z0, pared);
                    fill(a + 1, y + k, z1, c - 1, y + k, z1, pared);
                }
            }
        }
    }

    void farol(int x, int y, int z) {
        set(x, y, z, b("spruce_fence"));
        set(x, y + 1, z, b("spruce_fence"));
        deco(x, y + 2, z, "farolito", Direction.SOUTH);
    }

    void linterna(int x, int y, int z) {
        set(x, y, z, b("lantern").setValue(BlockStateProperties.HANGING, true));
    }
}
