package com.casino.decor;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Formas de choque de los muebles, escritas mirando al norte y giradas para las otras direcciones. */
public final class Shapes {
    private Shapes() {
    }

    /** Cajas {x1,y1,z1,x2,y2,z2} en píxeles (0-16), con el frente al norte. */
    public static Map<Direction, VoxelShape> rotated(double[]... boxes) {
        Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
        for (Direction d : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            VoxelShape s = net.minecraft.world.phys.shapes.Shapes.empty();
            for (double[] b : boxes) {
                double[] r = rotate(b, d);
                s = net.minecraft.world.phys.shapes.Shapes.or(s, Block.box(r[0], r[1], r[2], r[3], r[4], r[5]));
            }
            out.put(d, s.optimize());
        }
        return out;
    }

    public static VoxelShape of(double[]... boxes) {
        VoxelShape s = net.minecraft.world.phys.shapes.Shapes.empty();
        for (double[] b : boxes) {
            s = net.minecraft.world.phys.shapes.Shapes.or(s, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
        return s.optimize();
    }

    /** Igual que el "y" de un blockstate: norte 0, este 90, sur 180, oeste 270 (en el sentido del reloj visto desde arriba). */
    static double[] rotate(double[] b, Direction d) {
        double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
        int k = switch (d) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        for (int i = 0; i < k; i++) {
            double nx1 = 16 - z2, nx2 = 16 - z1, nz1 = x1, nz2 = x2;
            x1 = nx1;
            x2 = nx2;
            z1 = nz1;
            z2 = nz2;
        }
        return new double[]{x1, b[1], z1, x2, b[4], z2};
    }
}
