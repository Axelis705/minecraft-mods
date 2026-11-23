package com.pescacoleccion.nilo;

import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.pesca.Especie;
import com.pescacoleccion.pesca.Especies;
import com.pescacoleccion.pesca.PezItem;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** (Servidor) Prueba de Nilo sin jugadores (la corre el servidor de prueba): el muelle cabe en un estanque, amistad y precios. */
public final class Prueba {
    private Prueba() {
    }

    public static int correr(CommandSourceStack src) {
        ServerLevel l = src.getServer().overworld();
        List<String> fallos = new ArrayList<>();
        // un estanque: tierra al norte (z <= 0) y agua al sur (z de 1 a 12)
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 40, 40);
        int x0 = 40, z0 = 40;
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 12; z++) {
                for (int k = -4; k <= 4; k++) {
                    BlockPos b = new BlockPos(x0 + x, y + k, z0 + z);
                    boolean agua = z >= 1 && k >= -3 && k <= -1;
                    boolean tierra = k < -1 || z <= 0 && k == -1;
                    l.setBlock(b, agua ? Blocks.WATER.defaultBlockState() : tierra ? Blocks.GRASS_BLOCK.defaultBlockState()
                            : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        Muelle.Sitio s = Muelle.buscar(l, new BlockPos(x0, y - 1, z0 + 6), new BlockPos(x0, y, z0 - 2));
        if (s == null) {
            fallos.add("no encontró sitio para el muelle");
        } else {
            if (s.hacia() != Direction.SOUTH) {
                fallos.add("el muelle mira a " + s.hacia());
            }
            for (Muelle.Pieza pz : s.piezas()) {
                if (Muelle.cabe(l, pz)) {
                    l.setBlock(pz.pos(), pz.state(), 2);
                }
            }
            BlockPos punta = s.orilla().relative(s.hacia(), Muelle.LARGO);
            if (!l.getBlockState(punta).is(Blocks.SPRUCE_PLANKS)) {
                fallos.add("falta la punta del muelle");
            }
            if (!l.getBlockState(s.parado().below()).is(Blocks.SPRUCE_PLANKS)) {
                fallos.add("Nilo no tiene dónde pararse");
            }
            PescaColeccion.LOGGER.info("PESCA_NILO_MUELLE piezas={} orilla={} hacia={}", s.piezas().size(), s.orilla(), s.hacia());
        }
        // sin agua no hay muelle
        if (Muelle.buscar(l, new BlockPos(x0, y + 30, z0), new BlockPos(x0, y + 30, z0)) != null) {
            fallos.add("encontró muelle en el aire");
        }
        // amistad y precios
        if (Encargos.nivel(0) != 0 || Encargos.nivel(6) != 1 || Encargos.nivel(31) != 2 || Encargos.nivel(999) != 4) {
            fallos.add("niveles de amistad");
        }
        Especie e = Especies.get("charalito");
        ItemStack chico = PezItem.crear(ModRegistry.PECES.get(e.id()).get(), e, e.cmMin(), "prueba", false);
        ItemStack grande = PezItem.crear(ModRegistry.PECES.get(e.id()).get(), e, e.cmMax(), "prueba", false);
        if (Tienda.precio(chico) != Encargos.VALOR[0] || Tienda.precio(grande) != Encargos.VALOR_GRANDE[0]) {
            fallos.add("precios " + Tienda.precio(chico) + " " + Tienda.precio(grande));
        }
        String r = fallos.isEmpty() ? "ok" : "fallo " + String.join("; ", fallos);
        PescaColeccion.LOGGER.info("PESCA_NILO_PRUEBA {}", r);
        src.sendSuccess(() -> Component.literal("Nilo: " + r), false);
        return fallos.isEmpty() ? 1 : 0;
    }
}
