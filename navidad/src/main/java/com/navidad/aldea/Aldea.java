package com.navidad.aldea;

import com.navidad.Navidad;
import com.navidad.entidad.Reno;
import com.navidad.gente.Gente;
import com.navidad.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/**
 * La aldea de Santa: una plaza nevada con un pino gigante lleno de luces y regalos, la casa de Santa, el taller de juguetes de los
 * duendes, la casita de jengibre de la Señora Claus y el establo de los renos, con caminos de hielo, faroles y bastones de caramelo.
 * Se construye con {@code /navidad aldea} donde estés parado (ocupa 61 x 61 bloques) y pone a su gente.
 * <p>En las esquinas: la colina de trineos (noreste, con trineos de madera arriba) y la pista de hielo (sureste, con patines para
 * prestar). Frente a la casa de Santa, su buzón para las cartas.
 */
public final class Aldea {
    public static final int R = 30;

    private Aldea() {
    }

    public static int construir(ServerLevel l, BlockPos centro, boolean conGente) {
        Obra o = new Obra(l, centro);
        RandomSource rnd = RandomSource.create(1225L);
        // dónde quedó (para la carrera de trineos de la colina)
        com.axeliscore.data.WorldStore w = com.axeliscore.data.WorldStore.get(l.getServer(), Navidad.MODID);
        w.global().putInt("aldeaX", centro.getX());
        w.global().putInt("aldeaY", centro.getY());
        w.global().putInt("aldeaZ", centro.getZ());
        w.dirty();
        // quitar a los que estorban (menos jugadores) y limpiar
        for (Entity e : l.getEntities((Entity) null, new AABB(centro).inflate(R + 2, 24, R + 2), e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
            e.discard();
        }
        o.fill(-R, 0, -R, R, 22, R, Blocks.AIR.defaultBlockState());
        o.fill(-R, -4, -R, R, -2, R, Blocks.DIRT.defaultBlockState());
        o.fill(-R, -1, -R, R, -1, R, Blocks.SNOW_BLOCK.defaultBlockState());
        plaza(o, rnd);
        casaSanta(o);
        taller(o);
        cocina(o);
        establo(o);
        alrededores(o, rnd);
        colina(o);
        pista(o);
        if (conGente) {
            gente(l, o);
        }
        return o.puestos;
    }

    // ------------------------------------------------------------------ plaza y pino gigante

    static void plaza(Obra o, RandomSource rnd) {
        for (int x = -11; x <= 11; x++) {
            for (int z = -11; z <= 11; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d <= 9.5) {
                    o.bloqueNav(x, -1, z, (x + z) % 2 == 0 ? "nieve_brillante" : "glaseado");
                } else if (d <= 10.6) {
                    o.bloqueNav(x, -1, z, (Math.abs(x) + Math.abs(z)) % 2 == 0 ? "bloque_caramelo" : "bloque_caramelo_verde");
                }
            }
        }
        // caminos de hielo hacia las cuatro casas
        for (int t = 10; t <= 16; t++) {
            for (int w = -1; w <= 1; w++) {
                o.set(w, -1, -t, Blocks.PACKED_ICE.defaultBlockState());
                o.set(w, -1, t, Blocks.PACKED_ICE.defaultBlockState());
                o.set(t, -1, w, Blocks.PACKED_ICE.defaultBlockState());
                o.set(-t, -1, w, Blocks.PACKED_ICE.defaultBlockState());
            }
        }
        // el pino: tronco y capas de hojas cada vez más chicas, con focos y esferas
        BlockState hojas = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        o.fill(0, -1, 0, 0, 14, 0, Blocks.SPRUCE_LOG.defaultBlockState());
        String[] focos = {"bloque_luces", "bloque_regalo_rojo", "bloque_luces", "bloque_regalo_azul", "bloque_luces"};
        for (int y = 3; y <= 15; y++) {
            double r = 5.6 * (1 - (y - 3) / 13.0) + 0.4;
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d > r || (x == 0 && z == 0 && y < 15)) {
                        continue;
                    }
                    boolean borde = d > r - 1.2;
                    if (borde && rnd.nextFloat() < 0.16F) {
                        o.bloqueNav(x, y, z, focos[rnd.nextInt(focos.length)]);
                    } else {
                        o.set(x, y, z, hojas);
                    }
                }
            }
        }
        // estrella dorada arriba
        o.set(0, 16, 0, Blocks.GLOWSTONE.defaultBlockState());
        o.set(0, 17, 0, Blocks.GOLD_BLOCK.defaultBlockState());
        o.set(-1, 16, 0, Blocks.GOLD_BLOCK.defaultBlockState());
        o.set(1, 16, 0, Blocks.GOLD_BLOCK.defaultBlockState());
        o.set(0, 15, 0, Blocks.GOLD_BLOCK.defaultBlockState());
        // esferas colgando de la orilla de las ramas bajas
        String[] esferas = {"esfera_roja", "esfera_dorada", "esfera_azul", "esfera_plata", "esfera_verde"};
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2 / 12;
            int x = (int) Math.round(Math.cos(a) * 5), z = (int) Math.round(Math.sin(a) * 5);
            o.deco(x, 2, z, esferas[i % esferas.length], Direction.SOUTH);
        }
        // regalos alrededor del pino
        String[] regalos = {"regalo_rojo", "pila_regalos", "regalo_verde", "regalo_dorado", "regalo_azul", "pila_regalos", "regalo_rojo",
                "regalo_dorado"};
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI * 2 / 8 + 0.2;
            int x = (int) Math.round(Math.cos(a) * 3), z = (int) Math.round(Math.sin(a) * 3);
            o.deco(x, 0, z, regalos[i], Direction.fromYRot(Math.toDegrees(a) + 90));
        }
        // faroles y bastones gigantes en la orilla de la plaza
        for (int[] c : new int[][]{{7, 7}, {-7, 7}, {7, -7}, {-7, -7}}) {
            o.farol(c[0], 0, c[1]);
        }
        for (int[] c : new int[][]{{2, -11}, {-2, -11}, {2, 11}, {-2, 11}, {11, 2}, {11, -2}, {-11, 2}, {-11, -2}}) {
            o.deco(c[0], 0, c[1], "baston_gigante", Direction.SOUTH);
        }
        // muñecos de nieve (de adorno) y un trineo en la plaza
        o.deco(8, 0, 2, "muneco_nieve", Direction.WEST);
        o.deco(-8, 0, -3, "muneco_nieve", Direction.EAST);
        o.deco(-7, 0, 4, "trineo", Direction.EAST);
        o.deco(-6, 0, 4, "reno_adorno", Direction.EAST);
    }

    // ------------------------------------------------------------------ casa de Santa (al norte)

    static void casaSanta(Obra o) {
        BlockState pared = Obra.nav("madera_cabana"), piso = Blocks.SPRUCE_PLANKS.defaultBlockState();
        o.fill(-6, -1, -27, 6, -1, -17, piso);
        o.paredes(-6, 0, -27, 6, 4, -17, pared);
        for (int[] c : new int[][]{{-6, -27}, {6, -27}, {-6, -17}, {6, -17}}) {
            o.fill(c[0], 0, c[1], c[0], 4, c[1], Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        }
        o.techo(-6, 6, -27, -17, 5, true, "red_nether_brick_stairs", Blocks.RED_NETHER_BRICKS.defaultBlockState(), pared);
        o.puerta(0, 0, -17, "spruce_door", Direction.SOUTH);
        for (int x : new int[]{-3, 3}) {
            o.fill(x, 1, -17, x, 2, -17, Blocks.GLASS.defaultBlockState());
        }
        for (int z : new int[]{-24, -20}) {
            o.fill(-6, 1, z, -6, 2, z, Blocks.GLASS.defaultBlockState());
            o.fill(6, 1, z, 6, 2, z, Blocks.GLASS.defaultBlockState());
        }
        // adentro: chimenea con calcetines, árbol, alfombra, mesa con galletas y leche, sillón rojo
        o.deco(0, 0, -26, "chimenea", Direction.SOUTH);
        o.fill(0, 1, -26, 0, 4, -26, Blocks.BRICKS.defaultBlockState());
        o.deco(-1, 2, -26, "calcetin_rojo", Direction.SOUTH);
        o.deco(1, 2, -26, "calcetin_verde", Direction.SOUTH);
        o.deco(2, 2, -26, "calcetin_rayas", Direction.SOUTH);
        o.deco(-2, 3, -26, "corona_navidad", Direction.SOUTH);
        o.deco(-4, 0, -25, "arbol_navidad", Direction.SOUTH);
        o.deco(-3, 0, -25, "pila_regalos", Direction.SOUTH);
        o.deco(-5, 0, -24, "regalo_dorado", Direction.EAST);
        for (int x = -2; x <= 2; x++) {
            for (int z = -23; z <= -20; z++) {
                o.deco(x, 0, z, "alfombra_navidad", Direction.SOUTH);
            }
        }
        o.set(4, 0, -23, Blocks.SPRUCE_FENCE.defaultBlockState());
        o.set(4, 1, -23, Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState());
        o.deco(4, 2, -23, "galletas_leche", Direction.WEST);
        o.set(4, 0, -25, Blocks.RED_WOOL.defaultBlockState());
        o.set(5, 0, -25, Blocks.RED_WOOL.defaultBlockState());
        o.deco(5, 0, -21, "velas_navidad", Direction.WEST);
        o.deco(-5, 0, -19, "nochebuena", Direction.EAST);
        o.deco(5, 0, -19, "nochebuena", Direction.WEST);
        o.linterna(-3, 4, -22);
        o.linterna(3, 4, -22);
        o.linterna(0, 4, -19);
        // afuera: corona en la puerta, luces y pinitos
        o.deco(0, 3, -16, "corona_navidad", Direction.SOUTH);
        o.deco(-4, 0, -15, "pino_maceta", Direction.SOUTH);
        o.deco(4, 0, -15, "pino_maceta", Direction.SOUTH);
        for (int x = -5; x <= 5; x += 2) {
            o.deco(x, 4, -16, "guirnalda_luces", Direction.SOUTH);
        }
        // el buzón para las cartas a Santa, junto al camino
        o.set(2, 0, -14, ModRegistry.BUZON.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                Direction.WEST));
    }

    // ------------------------------------------------------------------ taller de juguetes (al este)

    static void taller(Obra o) {
        BlockState pared = Blocks.SPRUCE_PLANKS.defaultBlockState(), piso = Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState();
        o.fill(17, -1, -7, 27, -1, 7, piso);
        o.paredes(17, 0, -7, 27, 4, 7, pared);
        for (int[] c : new int[][]{{17, -7}, {27, -7}, {17, 7}, {27, 7}, {17, 0}, {27, 0}}) {
            o.fill(c[0], 0, c[1], c[0], 4, c[1], Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState());
        }
        o.techo(17, 27, -7, 7, 5, false, "dark_oak_stairs", Blocks.DARK_OAK_PLANKS.defaultBlockState(), pared);
        o.set(17, 0, 0, Blocks.AIR.defaultBlockState());
        o.puerta(17, 0, 0, "spruce_door", Direction.WEST);
        for (int z : new int[]{-4, 4}) {
            o.fill(17, 1, z, 17, 2, z, Blocks.GLASS.defaultBlockState());
            o.fill(27, 1, z, 27, 2, z, Blocks.GLASS.defaultBlockState());
        }
        // mesas de trabajo, juguetes y regalos
        for (int z = -5; z <= -2; z++) {
            o.set(25, 0, z, Blocks.CRAFTING_TABLE.defaultBlockState());
        }
        o.set(25, 0, 2, Blocks.LOOM.defaultBlockState());
        o.set(25, 0, 3, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        o.set(25, 0, 4, Blocks.SMITHING_TABLE.defaultBlockState());
        o.deco(22, 0, -5, "pila_regalos", Direction.SOUTH);
        o.deco(21, 0, -5, "regalo_rojo", Direction.SOUTH);
        o.deco(20, 0, -6, "regalo_azul", Direction.SOUTH);
        o.deco(23, 0, -6, "cascanueces", Direction.SOUTH);
        o.deco(19, 0, 5, "trineo", Direction.EAST);
        o.deco(22, 0, 6, "reno_adorno", Direction.NORTH);
        o.deco(26, 0, -6, "cascanueces", Direction.SOUTH);
        o.deco(26, 0, 6, "esfera_nieve", Direction.NORTH);
        o.deco(20, 0, 2, "pila_regalos", Direction.NORTH);
        for (int x = 18; x <= 24; x++) {
            o.deco(x, 0, 0, "alfombra_nieve", Direction.EAST);
        }
        o.linterna(20, 4, -3);
        o.linterna(24, 4, 3);
        o.linterna(20, 4, 3);
        o.linterna(24, 4, -3);
        o.deco(16, 3, 0, "corona_navidad", Direction.WEST);
        o.deco(16, 0, -2, "pino_maceta", Direction.WEST);
        o.deco(16, 0, 2, "pino_maceta", Direction.WEST);
    }

    // ------------------------------------------------------------------ casita de jengibre de la Señora Claus (al oeste)

    static void cocina(Obra o) {
        BlockState pared = Obra.nav("ladrillo_jengibre"), esquina = Obra.nav("bloque_caramelo"), glaseado = Obra.nav("glaseado");
        o.fill(-27, -1, -6, -17, -1, 6, Obra.nav("tela_cuadros"));
        o.paredes(-27, 0, -6, -17, 4, 6, pared);
        for (int[] c : new int[][]{{-27, -6}, {-17, -6}, {-27, 6}, {-17, 6}}) {
            o.fill(c[0], 0, c[1], c[0], 4, c[1], esquina);
        }
        o.fill(-27, 4, -6, -17, 4, -6, glaseado);
        o.fill(-27, 4, 6, -17, 4, 6, glaseado);
        o.techo(-27, -17, -6, 6, 5, false, "quartz_stairs", Obra.nav("glaseado_chispas"), pared);
        o.puerta(-17, 0, 0, "jungle_door", Direction.EAST);
        o.fill(-17, 2, -1, -17, 2, 1, Obra.nav("bloque_caramelo_verde"));
        for (int z : new int[]{-3, 3}) {
            o.fill(-17, 1, z, -17, 2, z, Blocks.GLASS.defaultBlockState());
            o.fill(-27, 1, z, -27, 2, z, Blocks.GLASS.defaultBlockState());
        }
        // la cocina: hornos, mesa con postres, ponche y chocolate
        o.set(-26, 0, -4, Blocks.SMOKER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        o.set(-26, 0, -3, Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        o.set(-26, 0, -2, Blocks.SMOKER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
        o.set(-26, 0, 2, Blocks.BARREL.defaultBlockState());
        o.set(-26, 0, 3, Blocks.BARREL.defaultBlockState());
        o.fill(-23, 0, -2, -21, 0, 2, Blocks.STRIPPED_BIRCH_WOOD.defaultBlockState());
        o.deco(-23, 1, -2, "galletas_leche", Direction.EAST);
        o.deco(-22, 1, 0, "olla_ponche", Direction.EAST);
        o.deco(-21, 1, 2, "taza_chocolate", Direction.EAST);
        o.deco(-23, 1, 1, "taza_chocolate", Direction.EAST);
        o.deco(-21, 1, -1, "velas_navidad", Direction.EAST);
        o.deco(-26, 0, 5, "arbol_nevado", Direction.EAST);
        o.deco(-19, 0, -5, "nochebuena", Direction.SOUTH);
        o.linterna(-22, 4, -3);
        o.linterna(-22, 4, 3);
        o.deco(-16, 0, -2, "baston_gigante", Direction.EAST);
        o.deco(-16, 0, 2, "baston_gigante", Direction.EAST);
    }

    // ------------------------------------------------------------------ establo de los renos (al sur)

    static void establo(Obra o) {
        BlockState cerca = Blocks.SPRUCE_FENCE.defaultBlockState();
        for (int x = -9; x <= 9; x++) {
            o.set(x, 0, 17, cerca);
            o.set(x, 0, 28, cerca);
        }
        for (int z = 17; z <= 28; z++) {
            o.set(-9, 0, z, cerca);
            o.set(9, 0, z, cerca);
        }
        o.set(0, 0, 17, Blocks.SPRUCE_FENCE_GATE.defaultBlockState());
        o.set(-1, 0, 17, Blocks.AIR.defaultBlockState());
        o.set(1, 0, 17, Blocks.AIR.defaultBlockState());
        o.set(-1, 0, 17, Blocks.SPRUCE_FENCE_GATE.defaultBlockState());
        o.set(1, 0, 17, Blocks.SPRUCE_FENCE_GATE.defaultBlockState());
        // cobertizo al fondo
        for (int x : new int[]{-8, -3, 3, 8}) {
            o.fill(x, 0, 24, x, 3, 24, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
        }
        o.fill(-8, 0, 27, 8, 3, 27, Blocks.SPRUCE_PLANKS.defaultBlockState());
        o.fill(-9, 4, 23, 9, 4, 28, Blocks.SPRUCE_SLAB.defaultBlockState());
        o.fill(-9, 5, 23, 9, 5, 28, Blocks.SNOW.defaultBlockState());
        for (int x = -7; x <= 7; x += 2) {
            o.set(x, 0, 26, Blocks.HAY_BLOCK.defaultBlockState());
        }
        o.set(-6, 0, 20, Blocks.WATER_CAULDRON.defaultBlockState().setValue(BlockStateProperties.LEVEL_CAULDRON, 3));
        o.set(-5, 0, 20, Blocks.HAY_BLOCK.defaultBlockState());
        o.deco(5, 0, 21, "trineo", Direction.WEST);
        o.deco(0, 3, 24, "campanas", Direction.SOUTH);
        o.linterna(-5, 3, 25);
        o.linterna(5, 3, 25);
        o.farol(-3, 0, 15);
        o.farol(3, 0, 15);
    }

    // ------------------------------------------------------------------ orilla: pinos nevados y cerca de bastones

    static void alrededores(Obra o, RandomSource rnd) {
        for (int i = 0; i < 26; i++) {
            int x, z;
            do {
                x = rnd.nextInt(2 * R - 3) - R + 2;
                z = rnd.nextInt(2 * R - 3) - R + 2;
            } while (Math.abs(x) < 28 && Math.abs(z) < 28 || o.l.getBlockState(o.p(x, 0, z)).isSolid());
            o.deco(x, 0, z, rnd.nextInt(3) == 0 ? "arbol_navidad" : "arbol_nevado", Direction.SOUTH);
        }
        for (int[] c : new int[][]{{13, 13}, {-13, 13}, {13, -13}, {-13, -13}}) {
            o.deco(c[0], 0, c[1], "arbol_nevado", Direction.SOUTH);
            o.farol(c[0] + (c[0] > 0 ? -2 : 2), 0, c[1]);
        }
        // letrero de bienvenida con bastones a la entrada sur (y luces colgando entre ellos)
        o.deco(-2, 0, 30, "baston_gigante", Direction.SOUTH);
        o.deco(2, 0, 30, "baston_gigante", Direction.SOUTH);
    }

    // ------------------------------------------------------------------ colina de trineos (noreste)

    /** Fondo del carril (z) donde empieza la subida, la cima (z) y el carril en x (con paredes de nieve a los lados). */
    public static final int COLINA_PIE = -14, COLINA_CIMA = -27, CARRIL_X0 = 15, CARRIL_X1 = 21;

    /** Altura de la colina en medios bloques en cada fila (0 al pie, 14 = 7 bloques en la cima). */
    static int mediosColina(int z) {
        return Math.max(0, Math.min(14, COLINA_PIE - z));
    }

    static void colina(Obra o) {
        BlockState nieve = Blocks.SNOW_BLOCK.defaultBlockState();
        BlockState media = Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 4);
        for (int z = COLINA_CIMA - 1; z <= COLINA_PIE; z++) {
            int h2 = z < COLINA_CIMA ? 14 : mediosColina(z);
            int lleno = h2 / 2;
            for (int x = CARRIL_X0 - 1; x <= CARRIL_X1 + 1; x++) {
                boolean pared = x == CARRIL_X0 - 1 || x == CARRIL_X1 + 1;
                if (lleno > 0) {
                    o.fill(x, 0, z, x, lleno - 1, z, nieve);
                }
                if (pared) {
                    // las paredes del carril: un bloque más altas que la pista
                    o.fill(x, lleno, z, x, lleno + (h2 % 2 == 1 ? 1 : 0), z, nieve);
                } else if (h2 % 2 == 1) {
                    o.set(x, lleno, z, media);
                }
            }
        }
        // la cima: plataforma con barandal, faroles, bandera y dos trineos esperando
        int cima = 7;
        o.fill(CARRIL_X0 - 1, 0, COLINA_CIMA - 2, CARRIL_X1 + 1, cima - 1, COLINA_CIMA - 1, nieve);
        o.fill(CARRIL_X0 - 1, cima - 1, COLINA_CIMA - 2, CARRIL_X1 + 1, cima - 1, COLINA_CIMA - 1, Blocks.SPRUCE_PLANKS.defaultBlockState());
        for (int x = CARRIL_X0 - 1; x <= CARRIL_X1 + 1; x++) {
            o.set(x, cima, COLINA_CIMA - 2, Blocks.SPRUCE_FENCE.defaultBlockState());
        }
        o.farol(CARRIL_X0 - 1, cima, COLINA_CIMA - 1);
        o.farol(CARRIL_X1 + 1, cima, COLINA_CIMA - 1);
        o.set(CARRIL_X0 + 3, cima, COLINA_CIMA - 2, Blocks.SPRUCE_FENCE.defaultBlockState());
        o.set(CARRIL_X0 + 3, cima + 1, COLINA_CIMA - 2, Blocks.SPRUCE_FENCE.defaultBlockState());
        o.set(CARRIL_X0 + 3, cima + 2, COLINA_CIMA - 2, Blocks.RED_BANNER.defaultBlockState());
        // escalones de madera por fuera de la pared del este para volver a subir
        for (int z = COLINA_PIE; z >= COLINA_CIMA; z--) {
            int h = (mediosColina(z) + 1) / 2;
            if (h > 0) {
                o.fill(CARRIL_X1 + 2, 0, z, CARRIL_X1 + 2, h - 1, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
            o.set(CARRIL_X1 + 2, h, z, Blocks.SNOW.defaultBlockState());
            o.set(CARRIL_X1 + 3, h, z, Blocks.SPRUCE_FENCE.defaultBlockState());
        }
        // al pie: un tramo plano, el piso de tablas que frena y una barda de pacas de paja por si acaso
        for (int x = CARRIL_X0 - 1; x <= CARRIL_X1 + 1; x++) {
            o.set(x, -1, -11, Blocks.SPRUCE_PLANKS.defaultBlockState());
            o.set(x, -1, -10, Blocks.SPRUCE_PLANKS.defaultBlockState());
            o.set(x, 0, -9, Blocks.HAY_BLOCK.defaultBlockState());
            o.set(x, 1, -9, Blocks.HAY_BLOCK.defaultBlockState());
        }
        o.deco(CARRIL_X0 - 2, 0, -12, "baston_gigante", Direction.SOUTH);
        o.deco(CARRIL_X1 + 2, 0, -12, "baston_gigante", Direction.SOUTH);
        o.farol(CARRIL_X1 + 4, 0, -12);
    }

    // ------------------------------------------------------------------ pista de hielo (sureste)

    static final int PISTA_X = 21, PISTA_Z = 19, PISTA_RX = 6, PISTA_RZ = 7;

    static boolean enPista(int x, int z, double extra) {
        double dx = (x - PISTA_X) / (PISTA_RX + extra), dz = (z - PISTA_Z) / (PISTA_RZ + extra);
        return dx * dx + dz * dz <= 1.0;
    }

    static void pista(Obra o) {
        for (int x = PISTA_X - PISTA_RX - 2; x <= PISTA_X + PISTA_RX + 2; x++) {
            for (int z = PISTA_Z - PISTA_RZ - 2; z <= PISTA_Z + PISTA_RZ + 2; z++) {
                if (enPista(x, z, 0)) {
                    o.set(x, -1, z, Blocks.PACKED_ICE.defaultBlockState());
                    o.set(x, 0, z, Blocks.AIR.defaultBlockState());
                } else if (enPista(x, z, 1.1)) {
                    // la barda: cerca blanca con linternas cada tanto
                    o.set(x, -1, z, Blocks.SPRUCE_PLANKS.defaultBlockState());
                    boolean entrada = z == PISTA_Z && x < PISTA_X;
                    if (!entrada) {
                        o.set(x, 0, z, Blocks.BIRCH_FENCE.defaultBlockState());
                        if (Math.floorMod(x + z, 3) == 0) {
                            o.set(x, 1, z, Blocks.LANTERN.defaultBlockState());
                        }
                    }
                }
            }
        }
        // en medio, un pinito con regalos; afuera, bancas y el barril de los patines para prestar
        o.deco(PISTA_X, 0, PISTA_Z, "arbol_navidad", Direction.SOUTH);
        o.deco(PISTA_X - PISTA_RX - 3, 0, PISTA_Z + 2, "farolito", Direction.EAST);
        o.escalon(PISTA_X - PISTA_RX - 3, 0, PISTA_Z - 3, "spruce_stairs", Direction.WEST);
        o.escalon(PISTA_X - PISTA_RX - 3, 0, PISTA_Z - 4, "spruce_stairs", Direction.WEST);
        BlockPos barril = o.p(PISTA_X - PISTA_RX - 3, 0, PISTA_Z - 1);
        o.set(PISTA_X - PISTA_RX - 3, 0, PISTA_Z - 1, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP));
        if (o.l.getBlockEntity(barril) instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity b) {
            for (int i = 0; i < 4; i++) {
                b.setItem(i, new net.minecraft.world.item.ItemStack(ModRegistry.PATINES.get()));
            }
            b.setItem(4, new net.minecraft.world.item.ItemStack(ModRegistry.CHOCOLATE_CALIENTE.get(), 4));
        }
        o.farol(PISTA_X + PISTA_RX + 2, 0, PISTA_Z - 4);
        o.farol(PISTA_X + PISTA_RX + 2, 0, PISTA_Z + 4);
    }

    /** Los trineos de madera que esperan en la cima de la colina. */
    static void trineos(ServerLevel l, Obra o) {
        for (int i = 0; i < 2; i++) {
            com.navidad.entidad.TrineoMadera t = ModRegistry.TRINEO_MADERA.get().create(l);
            if (t != null) {
                BlockPos p = o.p(CARRIL_X0 + 1 + i * 4, 7, COLINA_CIMA - 1);
                t.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0F, 0F);
                l.addFreshEntity(t);
            }
        }
    }

    // ------------------------------------------------------------------ la gente

    static void gente(ServerLevel l, Obra o) {
        Gente.poner(l, Gente.SANTA, o.p(0, 0, -20));
        Gente.poner(l, Gente.SENORA, o.p(-20, 0, -3));
        Gente.poner(l, Gente.JUGUETERO, o.p(23, 0, -3));
        Gente.poner(l, Gente.SASTRE, o.p(23, 0, 3));
        Gente.poner(l, Gente.RENOS, o.p(0, 0, 19));
        for (int i = 0; i < 3; i++) {
            Reno r = ModRegistry.RENO.get().create(l);
            if (r != null) {
                BlockPos p = o.p(-5 + i * 5, 0, 22);
                r.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 180, 0);
                r.finalizeSpawn(l, l.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null, null);
                r.setPersistenceRequired();
                l.addFreshEntity(r);
            }
        }
        trineos(l, o);
        Navidad.LOGGER.info("aldea de Santa lista en {}", o.o);
    }
}
