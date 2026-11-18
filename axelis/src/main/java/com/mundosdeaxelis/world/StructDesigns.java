package com.mundosdeaxelis.world;

/**
 * Los diseños de las cuatro estructuras grandes (bosque, forja, hielo y torre). Solo texto y números: ver {@link StructBp}.
 * Todas miran hacia quien las pide: la entrada está en z = 0 y se extienden hacia z positivo.
 */
public final class StructDesigns {
    private StructDesigns() {
    }

    public static StructBp make(String kind) {
        StructBp bp = switch (kind) {
            case "bosque" -> DesignBosque.make();
            case "forja" -> DesignForja.make();
            case "hielo" -> DesignHielo.make();
            default -> DesignTorre.make();
        };
        // lo que el juego rompería solo (antorcha sin muro, farola en el aire...) se quita del plano: así lo revisado es lo construido
        Soporte.fix(bp);
        bp.unsupported = Soporte.prune(bp);
        return bp;
    }

    // ================================================================== utilidades comunes

    /** Cristal inclinado: una columna de grosor w que se desplaza 1 bloque cada {@code every} de altura hacia (dx,dz). */
    static void crystal(StructBp b, int x, int z, int y0, int h, int w, int dx, int dz, int every, String... pal) {
        for (int i = 0; i < h; i++) {
            int off = i / every;
            double taper = i > h * 0.7 ? Math.max(1, w - (int) ((i - h * 0.7) / (h * 0.3 / w + 0.01))) : w;
            int ww = (int) Math.max(1, Math.round(taper));
            for (int a = 0; a < ww; a++) {
                for (int c = 0; c < ww; c++) {
                    b.set(x + dx * off + a, y0 + i, z + dz * off + c, b.pick(pal));
                }
            }
        }
    }

    /** Vacía un arco apuntado: ancho medio hw, alto h, en el plano z (de z0 a z1), centrado en cx, desde la altura y0. */
    static void ogiveZ(StructBp b, int cx, int y0, int z0, int z1, int hw, int h) {
        int base = h - hw - 2;
        for (int y = 0; y < h; y++) {
            double w = y <= base ? hw : hw * Math.sqrt(Math.max(0.0, 1.0 - Math.pow((y - base) / (double) (h - base), 2)));
            int wi = (int) Math.round(w);
            for (int x = -wi; x <= wi; x++) {
                for (int z = z0; z <= z1; z++) {
                    b.set(cx + x, y0 + y, z, "air");
                }
            }
        }
    }

    // ================================================================== HIELO: Ciudadela de Cristal

    private static final String[] ICE = {"packed_ice", "packed_ice", "packed_ice", "blue_ice", "snow_block", "packed_ice"};
    private static final String[] ICE_DARK = {"blue_ice", "blue_ice", "packed_ice", "blue_ice"};
    private static final String Q = "quartz_bricks";
    private static final String GLASS = "light_blue_stained_glass";

    private static StructBp hielo() {
        StructBp b = new StructBp(3003);
        // ---- suelo: gran explanada de nieve con aros de hielo
        b.foundation(-36, -12, 36, 78, "packed_ice");
        for (int x = -36; x <= 36; x++) {
            for (int z = -12; z <= 78; z++) {
                String f = ((x + z) & 7) == 0 ? "packed_ice" : (b.chance(0.12) ? "packed_ice" : "snow_block");
                b.set(x, -1, z, f);
            }
        }
        // foso helado delante (z 0..7) con puente
        b.box(-34, -1, 0, 34, -1, 7, "blue_ice");
        for (int x = -34; x <= 34; x += 2) {
            b.set(x, -1, 3, "packed_ice");
        }
        b.box(-4, -1, 0, 4, -1, 7, "polished_diorite");
        b.box(-3, -1, 0, 3, -1, 7, "quartz_bricks");
        for (int z = 0; z <= 7; z += 3) {
            for (int sx : new int[]{-4, 4}) {
                b.box(sx, 0, z, sx, 3, z, "quartz_pillar");
                b.set(sx, 4, z, "sea_lantern");
            }
        }
        // ---- muralla
        wallsIce(b);
        // ---- torres de esquina y de la puerta
        for (int sx : new int[]{-1, 1}) {
            tower(b, sx * 30, 9, 6, 32, 24);
            tower(b, sx * 30, 69, 6, 34, 24);
            tower(b, sx * 10, 9, 4, 24, 15);
        }
        // ---- puerta grande (arco apuntado) y rastrillo
        ogiveZ(b, 0, 0, 7, 11, 5, 15);
        for (int y = 0; y < 15; y++) {
            for (int x = -5; x <= 5; x++) {
                // marco de cuarzo alrededor del arco
            }
        }
        for (int x = -6; x <= 6; x++) {
            b.set(x, 15, 8, Q);
        }
        // ---- el torreón central
        keep(b);
        // ---- cristales gigantes
        int[][] cr = {{-22, -6, 18, 3, -1, 1}, {-17, -9, 26, 3, -1, 0}, {-27, -4, 14, 2, -1, 1}, {22, -6, 20, 3, 1, 1}, {17, -9, 28, 3, 1, 0}, {27, -4, 12, 2, 1, 1},
                {-12, -7, 10, 2, 0, 1}, {12, -7, 11, 2, 0, 1}};
        for (int[] c : cr) {
            crystal(b, c[0], c[1], 0, c[2], c[3], c[4], c[5], 5, ICE_DARK);
        }
        // ---- patio: avenida con braseros y pequeños cristales
        for (int z = 14; z <= 24; z += 5) {
            for (int sx : new int[]{-7, 7}) {
                b.box(sx, 0, z, sx, 3, z, "quartz_pillar");
                b.set(sx, 4, z, "sea_lantern");
                b.set(sx, 5, z, "snow");
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = 11; z <= 25; z++) {
                b.set(x, -1, z, ((x + z) & 1) == 0 ? "quartz_bricks" : "polished_diorite");
            }
        }
        // gran fuente helada en el patio
        b.disc(0, 18, 3.5, 0, 0, "blue_ice");
        b.disc(0, 18, 2.2, 1, 1, "packed_ice");
        b.disc(0, 18, 1.0, 2, 5, "blue_ice");
        b.set(0, 6, 18, "sea_lantern");
        // ---- nieve encima y carámbanos debajo
        snowAndIcicles(b);
        return b;
    }

    private static void wallsIce(StructBp b) {
        // lados: x=±30 (2 de grosor), frente z=8..10 y fondo z=68..70, altura 16
        for (int y = 0; y < 16; y++) {
            for (int x = -33; x <= 33; x++) {
                for (int z : new int[]{8, 9, 10, 68, 69, 70}) {
                    b.set(x, y, z, b.pick(ICE));
                }
            }
            for (int z = 8; z <= 70; z++) {
                for (int x : new int[]{-31, -30, -29, 29, 30, 31}) {
                    b.set(x, y, z, b.pick(ICE));
                }
            }
        }
        // zócalo de cuarzo y banda de farolas
        for (int x = -33; x <= 33; x++) {
            for (int z : new int[]{8, 70}) {
                b.set(x, 0, z, Q);
                b.set(x, 1, z, Q);
                if (x % 3 == 0) {
                    b.set(x, 9, z, "sea_lantern");
                }
            }
        }
        // almenas
        for (int x = -33; x <= 33; x++) {
            if ((x & 1) == 0) {
                for (int z : new int[]{8, 70}) {
                    b.set(x, 16, z, b.pick(ICE));
                    b.set(x, 17, z, "blue_ice");
                }
            }
        }
        for (int z = 8; z <= 70; z++) {
            if ((z & 1) == 0) {
                for (int x : new int[]{-31, 31}) {
                    b.set(x, 16, z, b.pick(ICE));
                    b.set(x, 17, z, "blue_ice");
                }
            }
        }
        // contrafuertes de hielo cada 8 bloques con cristal
        for (int z = 16; z <= 62; z += 9) {
            for (int sx : new int[]{-1, 1}) {
                crystal(b, sx * 32, z, 0, 22, 2, sx, 0, 6, ICE_DARK);
            }
        }
        // ventanas (cristal azul) en los lados
        for (int z = 14; z <= 64; z += 6) {
            for (int sx : new int[]{-30, 30}) {
                for (int y = 6; y <= 10; y++) {
                    b.set(sx, y, z, GLASS);
                }
            }
        }
        // camino de ronda arriba (interior)
        for (int x = -28; x <= 28; x++) {
            b.set(x, 13, 11, "snow_block");
        }
    }

    private static void tower(StructBp b, int cx, int cz, int r, int h, int spireH) {
        b.tube(cx, cz, r, 2.4, 0, h, ICE);
        b.disc(cx, cz, r - 1.5, -1, -1, "packed_ice");
        // ventanas
        for (int y = 6; y < h - 4; y += 8) {
            for (int a = 0; a < 8; a++) {
                double ang = Math.PI * a / 4;
                int wx = cx + (int) Math.round(Math.cos(ang) * r);
                int wz = cz + (int) Math.round(Math.sin(ang) * r);
                for (int dy = 0; dy < 3; dy++) {
                    b.set(wx, y + dy, wz, GLASS);
                }
            }
        }
        // balcón con almenas
        b.tube(cx, cz, r + 1.5, 1.5, h, h, "quartz_bricks");
        b.tube(cx, cz, r + 1.5, 1.0, h + 1, h + 1, "blue_ice");
        for (int a = 0; a < 16; a++) {
            double ang = Math.PI * a / 8;
            if ((a & 1) == 0) {
                b.set(cx + (int) Math.round(Math.cos(ang) * (r + 1)), h + 2, cz + (int) Math.round(Math.sin(ang) * (r + 1)), "packed_ice");
            }
        }
        // aguja de hielo con bandas
        for (int i = 0; i < spireH; i++) {
            double rr = (r + 0.5) * (1.0 - (double) i / spireH);
            String s = i % 7 == 6 ? "quartz_bricks" : "blue_ice";
            if (rr < 0.8) {
                b.set(cx, h + 2 + i, cz, s);
            } else {
                b.disc(cx, cz, rr, h + 2 + i, h + 2 + i, s);
            }
        }
        b.set(cx, h + 2 + spireH, cz, "sea_lantern");
        b.set(cx, h + 3 + spireH, cz, "end_rod");
        b.set(cx, h + 4 + spireH, cz, "end_rod");
        // hueco de entrada en la base mirando al interior (nada), anillo de farolas en la base
        b.tube(cx, cz, r + 0.5, 1.0, 0, 0, "sea_lantern");
        b.tube(cx, cz, r + 0.5, 1.0, 0, 1, Q);
    }

    private static void keep(StructBp b) {
        // cuerpo: 30x30 hasta y=24, 20x20 hasta y=40
        int z0 = 26, z1 = 58;
        for (int y = 0; y < 26; y++) {
            for (int x = -16; x <= 16; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean edge = x == -16 || x == 16 || z == z0 || z == z1;
                    if (edge) {
                        b.set(x, y, z, y < 2 ? Q : b.pick(ICE));
                    }
                }
            }
        }
        b.box(-16, 26, z0, 16, 26, z1, "quartz_bricks");
        b.box(-15, 26, z0 + 1, 15, 26, z1 - 1, "packed_ice");
        for (int y = 27; y < 44; y++) {
            for (int x = -10; x <= 10; x++) {
                for (int z = 34; z <= 52; z++) {
                    if (x == -10 || x == 10 || z == 34 || z == 52) {
                        b.set(x, y, z, b.pick(ICE));
                    }
                }
            }
        }
        b.box(-10, 44, 34, 10, 44, 52, "quartz_bricks");
        b.box(-9, 44, 35, 9, 44, 51, "packed_ice");
        // pilastras de cuarzo en las esquinas y banda de farolas
        for (int y = 0; y <= 43; y++) {
            for (int[] c : new int[][]{{-16, z0}, {16, z0}, {-16, z1}, {16, z1}}) {
                if (y <= 25) {
                    b.set(c[0], y, c[1], "quartz_pillar");
                }
            }
            for (int[] c : new int[][]{{-10, 34}, {10, 34}, {-10, 52}, {10, 52}}) {
                if (y >= 27) {
                    b.set(c[0], y, c[1], "quartz_pillar");
                }
            }
        }
        for (int x = -16; x <= 16; x++) {
            if (x % 4 == 0) {
                b.set(x, 12, z0, "sea_lantern");
                b.set(x, 12, z1, "sea_lantern");
            }
        }
        // ventanales altos
        for (int x = -12; x <= 12; x += 6) {
            for (int y = 5; y <= 14; y++) {
                if (Math.abs(x) > 2) {
                    b.set(x, y, z0, GLASS);
                    b.set(x + 1, y, z0, GLASS);
                }
            }
            for (int y = 31; y <= 38; y++) {
                b.set(x / 2 * 1, y, 34, GLASS);
                b.set(x / 2 * 1 + 1, y, 34, GLASS);
            }
        }
        // entrada: arco apuntado + escalinata
        ogiveZ(b, 0, 0, z0, z0 + 1, 4, 13);
        for (int i = 0; i < 6; i++) {
            for (int x = -7 + i / 2; x <= 7 - i / 2; x++) {
                b.set(x, -1 + (5 - i) / 2, z0 - 1 - (5 - i), Q);
                for (int h = 0; h < (5 - i) / 2; h++) {
                    b.set(x, h, z0 - 1 - (5 - i), Q);
                }
            }
        }
        // interior: gran salón
        b.air(-15, 0, z0 + 1, 15, 25, z1 - 1);
        b.box(-15, -1, z0 + 1, 15, -1, z1 - 1, "polished_diorite");
        for (int x = -14; x <= 14; x++) {
            for (int z = z0 + 1; z <= z1 - 1; z++) {
                if (Math.abs(x) <= 2) {
                    b.set(x, -1, z, "light_blue_wool");
                } else if (((x + z) & 1) == 0) {
                    b.set(x, -1, z, "quartz_bricks");
                }
            }
        }
        for (int z = z0 + 4; z <= z1 - 4; z += 5) {
            for (int sx : new int[]{-8, 8}) {
                b.box(sx, 0, z, sx, 22, z, "quartz_pillar");
                b.set(sx, 23, z, "sea_lantern");
            }
        }
        // trono de hielo al fondo
        b.box(-4, 0, z1 - 5, 4, 1, z1 - 2, "quartz_bricks");
        b.box(-3, 2, z1 - 4, 3, 2, z1 - 2, "blue_ice");
        b.box(-1, 3, z1 - 2, 1, 9, z1 - 2, "blue_ice");
        b.box(-2, 3, z1 - 3, -2, 5, z1 - 3, "blue_ice");
        b.box(2, 3, z1 - 3, 2, 5, z1 - 3, "blue_ice");
        crystal(b, -6, z1 - 3, 0, 14, 2, 0, 0, 9, ICE_DARK);
        crystal(b, 5, z1 - 3, 0, 12, 2, 0, 0, 9, ICE_DARK);
        // torretas y aguja central
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{0, 1}) {
                tower(b, sx * 11, sz == 0 ? 34 : 52, 3, 50, 16);
            }
        }
        // aguja grande en la cima
        for (int i = 0; i < 58; i++) {
            double rr = 8.5 * Math.pow(1.0 - (double) i / 58, 1.2);
            String s = i % 8 == 7 ? "quartz_bricks" : (i % 3 == 0 ? "blue_ice" : "packed_ice");
            if (rr < 0.8) {
                b.set(0, 45 + i, 43, s);
            } else {
                b.disc(0, 43, rr, 45 + i, 45 + i, s);
            }
        }
        for (int i = 0; i < 8; i++) {
            b.set(0, 103 + i, 43, "end_rod");
        }
        b.set(0, 102, 43, "sea_lantern");
    }

    private static void snowAndIcicles(StructBp b) {
        java.util.List<long[]> addSnow = new java.util.ArrayList<>();
        java.util.List<long[]> addIce = new java.util.ArrayList<>();
        for (var e : b.m.entrySet()) {
            long k = e.getKey();
            int x = StructBp.kx(k), y = StructBp.ky(k), z = StructBp.kz(k);
            String v = e.getValue();
            if (v.equals("air") || y < 0) {
                continue;
            }
            boolean glass = v.contains("glass") || v.contains("lantern") || v.contains("end_rod") || v.contains("snow") || v.contains("carpet") || v.contains("wool");
            if (!glass && !b.m.containsKey(StructBp.key(x, y + 1, z)) && b.r.nextDouble() < 0.55) {
                addSnow.add(new long[]{x, y + 1, z});
            }
            // carámbanos bajo los bordes salientes (solo desde arriba del suelo)
            if (!glass && y > 6 && !b.m.containsKey(StructBp.key(x, y - 1, z)) && b.r.nextDouble() < 0.10) {
                addIce.add(new long[]{x, y - 1, z});
            }
        }
        for (long[] a : addSnow) {
            b.setIfAbsent((int) a[0], (int) a[1], (int) a[2], "snow");
        }
        for (long[] a : addIce) {
            int len = 1 + b.r.nextInt(3);
            for (int i = 0; i < len; i++) {
                b.setIfAbsent((int) a[0], (int) a[1] - i, (int) a[2], i == len - 1 ? "light_blue_stained_glass_pane" : "light_blue_stained_glass_pane");
            }
        }
    }

    // ================================================================== BOSQUE: Gran Templo Cubierto

    private static final String[] MOSSY = {"stone_bricks", "mossy_stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "stone_bricks", "mossy_stone_bricks"};
    private static final String[] LEAF = {"oak_leaves[persistent=true]", "azalea_leaves[persistent=true]", "flowering_azalea_leaves[persistent=true]", "oak_leaves[persistent=true]"};

    private static StructBp bosque() {
        StructBp b = new StructBp(1001);
        b.foundation(-28, -12, 28, 68, "stone_bricks");
        // suelo: musgo, adoquín y hierba
        for (int x = -28; x <= 28; x++) {
            for (int z = -12; z <= 68; z++) {
                String f = b.pick("moss_block", "mossy_cobblestone", "moss_block", "grass_block", "coarse_dirt");
                b.set(x, -1, z, f);
            }
        }
        // avenida de piedra hasta la gran escalera
        for (int x = -4; x <= 4; x++) {
            for (int z = -12; z <= 3; z++) {
                b.set(x, -1, z, b.pick(MOSSY));
            }
        }
        // los tres cuerpos (rampa en cuña con la escalera central a 45 grados)
        tier(b, -26, 14, 26, 64, 12);
        tier(b, -19, 26, 19, 60, 24);
        tier(b, -12, 36, 12, 54, 34);
        // la escalera
        for (int i = 0; i <= 34; i++) {
            int z = 2 + i;
            for (int x = -4; x <= 4; x++) {
                for (int y = 0; y < i; y++) {
                    b.setIfAbsent(x, y, z, b.pick(MOSSY));
                }
                b.set(x, i, z, StructBp.stair("mossy_stone_brick_stairs", "+z", false));
            }
            for (int sx : new int[]{-5, 5}) {
                for (int y = 0; y <= i + 1; y++) {
                    b.set(sx, y, z, b.pick(MOSSY));
                }
                if (i % 6 == 3) {
                    b.set(sx, i + 2, z, "mossy_stone_brick_wall");
                    b.set(sx, i + 3, z, "lantern");
                }
            }
        }
        // puertas laterales al gran salón (a los lados de la escalera)
        for (int sx : new int[]{-1, 1}) {
            ogiveZ(b, sx * 15, 0, 14, 17, 3, 9);
            for (int x = -3; x <= 3; x++) {
                b.set(sx * 15 + x, 10, 14, "chiseled_stone_bricks");
            }
        }
        // gran salón interior
        b.air(-24, 0, 17, 24, 10, 62);
        b.box(-24, -1, 17, 24, -1, 62, "mossy_cobblestone");
        for (int z = 20; z <= 60; z += 6) {
            for (int sx : new int[]{-16, -8, 8, 16}) {
                b.box(sx, 0, z, sx, 10, z, "mossy_stone_bricks");
                b.set(sx, 9, z, "chiseled_stone_bricks");
                b.set(sx, 8, z, "lantern[hanging=false]");
            }
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = 18; z <= 60; z++) {
                b.set(x, -1, z, "green_carpet");
            }
        }
        // santuario en la cima
        sanctum(b);
        // árboles gigantes a los lados y atrás
        int[][] trees = {{-34, 4}, {34, 6}, {-36, 26}, {36, 28}, {-34, 50}, {34, 52}, {-18, 72}, {0, 76}, {18, 72}, {-30, -6}, {30, -6}};
        for (int[] t : trees) {
            bigTree(b, t[0], t[1], 20 + b.r.nextInt(8));
        }
        // columnas rotas y pilares de la avenida
        for (int z = 0; z <= 10; z += 5) {
            for (int sx : new int[]{-8, 8}) {
                b.box(sx, 0, z, sx, 9, z, "mossy_stone_bricks");
                b.set(sx, 10, z, "chiseled_stone_bricks");
                b.set(sx, 11, z, "lantern");
            }
        }
        // musgo, enredaderas y hojas colgantes
        overgrow(b);
        return b;
    }

    private static void tier(StructBp b, int x0, int z0, int x1, int z1, int h) {
        for (int y = 0; y <= h; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                    if (edge || y == h) {
                        b.set(x, y, z, b.pick(MOSSY));
                    }
                }
            }
        }
        // cornisa (resalto) y parapeto con almenas
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z : new int[]{z0 - 1, z1 + 1}) {
                b.set(x, h - 1, z, StructBp.slab("mossy_stone_brick_slab", true));
            }
        }
        for (int z = z0 - 1; z <= z1 + 1; z++) {
            for (int x : new int[]{x0 - 1, x1 + 1}) {
                b.set(x, h - 1, z, StructBp.slab("mossy_stone_brick_slab", true));
            }
        }
        for (int x = x0; x <= x1; x++) {
            for (int z : new int[]{z0, z1}) {
                if ((x & 1) == 0) {
                    b.set(x, h + 1, z, "mossy_stone_brick_wall");
                }
            }
        }
        for (int z = z0; z <= z1; z++) {
            for (int x : new int[]{x0, x1}) {
                if ((z & 1) == 0) {
                    b.set(x, h + 1, z, "mossy_stone_brick_wall");
                }
            }
        }
        // pilastras en las esquinas y cada 6 bloques con detalle cincelado
        for (int y = 0; y <= h + 2; y++) {
            for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
                b.set(c[0], y, c[1], y % 4 == 3 ? "chiseled_stone_bricks" : "mossy_stone_bricks");
            }
        }
        for (int z = z0 + 6; z < z1; z += 6) {
            for (int x : new int[]{x0, x1}) {
                for (int y = 0; y <= h - 2; y++) {
                    b.set(x + (x == x0 ? -1 : 1), y, z, y % 3 == 2 ? "chiseled_stone_bricks" : "mossy_stone_bricks");
                }
            }
        }
        // ventanitas con luz verde
        for (int z = z0 + 3; z < z1; z += 6) {
            for (int x : new int[]{x0, x1}) {
                b.set(x, h - 4, z, "shroomlight");
            }
        }
    }

    private static void sanctum(StructBp b) {
        int y0 = 35;
        // plataforma y cuatro grandes pilares
        for (int x = -9; x <= 9; x++) {
            for (int z = 38; z <= 52; z++) {
                b.set(x, 34, z, b.pick(MOSSY));
            }
        }
        for (int sx : new int[]{-8, 8}) {
            for (int z : new int[]{39, 51}) {
                b.box(sx, y0, z, sx, y0 + 12, z, "mossy_stone_bricks");
                b.set(sx, y0 + 13, z, "chiseled_stone_bricks");
            }
        }
        // techo: pirámide de losas y escaleras
        for (int i = 0; i < 9; i++) {
            int x0 = -10 + i, x1 = 10 - i, z0 = 37 + i, z1 = 53 - i;
            if (x0 > x1 || z0 > z1) {
                break;
            }
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    if (x == x0 || x == x1 || z == z0 || z == z1) {
                        b.set(x, y0 + 14 + i, z, b.pick(MOSSY));
                    }
                }
            }
        }
        b.box(-1, y0 + 14 + 8, 45, 1, y0 + 14 + 8, 45, "mossy_stone_bricks");
        // corona: anillo de pinchos de roble oscuro con shroomlight
        for (int a = 0; a < 8; a++) {
            double ang = Math.PI * a / 4;
            int cx = (int) Math.round(Math.cos(ang) * 3), cz = 45 + (int) Math.round(Math.sin(ang) * 3);
            b.box(cx, y0 + 23, cz, cx, y0 + 26, cz, "dark_oak_log");
            b.set(cx, y0 + 27, cz, "shroomlight");
        }
        b.box(0, y0 + 23, 45, 0, y0 + 30, 45, "dark_oak_log");
        b.set(0, y0 + 31, 45, "shroomlight");
        b.set(0, y0 + 32, 45, "end_rod");
        // altar en el centro bajo el techo
        b.box(-3, y0, 43, 3, y0, 47, "chiseled_stone_bricks");
        b.box(-1, y0 + 1, 44, 1, y0 + 1, 46, "mossy_stone_bricks");
        b.set(0, y0 + 2, 45, "shroomlight");
        // farolas colgando
        for (int sx : new int[]{-6, 6}) {
            for (int z : new int[]{43, 47}) {
                b.set(sx, y0 + 11, z, "chain");
                b.set(sx, y0 + 10, z, "lantern[hanging=true]");
            }
        }
    }

    private static void bigTree(StructBp b, int x, int z, int h) {
        // tronco de roble oscuro 2x2 con raíces
        for (int y = -1; y < h; y++) {
            for (int a = 0; a < 2; a++) {
                for (int c = 0; c < 2; c++) {
                    b.set(x + a, y, z + c, "dark_oak_log[axis=y]");
                }
            }
        }
        for (int[] d : new int[][]{{-1, 0}, {2, 1}, {0, -1}, {1, 2}, {-1, 1}, {2, 0}}) {
            for (int y = -1; y < 3; y++) {
                if (b.chance(0.8)) {
                    b.set(x + d[0], y, z + d[1], "dark_oak_log[axis=y]");
                }
            }
        }
        // copa: bolas de hojas
        int[][] blobs = {{0, 0, 0, 8}, {-5, -1, 3, 6}, {5, -2, 2, 6}, {2, -3, -5, 6}, {-2, -2, 6, 5}, {0, 4, 0, 5}};
        for (int[] c : blobs) {
            int bx = x + 1 + c[0], by = h - 2 + c[1], bz = z + 1 + c[2];
            int rr = c[3];
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dy = -rr / 2 - 1; dy <= rr / 2; dy++) {
                    for (int dz = -rr; dz <= rr; dz++) {
                        double d = dx * dx + dy * dy * 2.2 + dz * dz;
                        if (d <= rr * rr && b.chance(0.92)) {
                            b.setIfAbsent(bx + dx, by + dy, bz + dz, b.pick(LEAF));
                        }
                    }
                }
            }
        }
    }

    private static void overgrow(StructBp b) {
        java.util.List<int[]> hang = new java.util.ArrayList<>();
        for (var e : b.m.entrySet()) {
            long k = e.getKey();
            int x = StructBp.kx(k), y = StructBp.ky(k), z = StructBp.kz(k);
            String v = e.getValue();
            if (y < 4 || !(v.contains("stone_brick") && !v.contains("wall"))) {
                continue;
            }
            if (!b.m.containsKey(StructBp.key(x, y - 1, z)) && b.r.nextDouble() < 0.22) {
                hang.add(new int[]{x, y - 1, z});
            }
        }
        for (int[] h : hang) {
            int len = 1 + b.r.nextInt(5);
            for (int i = 0; i < len; i++) {
                b.setIfAbsent(h[0], h[1] - i, h[2], i == len - 1 ? "vine[up=true]" : "oak_leaves[persistent=true]");
            }
        }
        // hojas y flores encima de los bordes
        for (var e : new java.util.ArrayList<>(b.m.entrySet())) {
            long k = e.getKey();
            int x = StructBp.kx(k), y = StructBp.ky(k), z = StructBp.kz(k);
            if (y >= 0 && e.getValue().contains("stone_brick") && !b.m.containsKey(StructBp.key(x, y + 1, z)) && b.r.nextDouble() < 0.12) {
                b.setIfAbsent(x, y + 1, z, b.pick("moss_carpet", "moss_carpet", "azalea_leaves[persistent=true]", "flowering_azalea[]", "fern"));
            }
        }
    }

    // ================================================================== FORJA: Gran Fundición

    private static final String[] BLK = {"blackstone", "polished_blackstone_bricks", "polished_blackstone_bricks", "cracked_polished_blackstone_bricks", "blackstone", "polished_blackstone_bricks"};
    private static final String[] NETH = {"nether_bricks", "red_nether_bricks", "nether_bricks", "cracked_nether_bricks"};

    private static StructBp forja() {
        StructBp b = new StructBp(2002);
        b.foundation(-38, -14, 38, 76, "blackstone");
        for (int x = -38; x <= 38; x++) {
            for (int z = -14; z <= 76; z++) {
                String f = b.pick("blackstone", "basalt[axis=y]", "polished_blackstone", "blackstone", "magma_block", "smooth_basalt", "polished_blackstone_bricks");
                b.set(x, -1, z, f);
            }
        }
        // rios de lava con puente central
        for (int z = -14; z <= 12; z++) {
            for (int sx : new int[]{-1, 1}) {
                for (int w = 0; w <= 3; w++) {
                    int x = sx * (10 + w);
                    b.set(x, -1, z, z == -14 || z == 12 ? "nether_bricks" : (w == 0 || w == 3 ? "nether_bricks" : "lava"));
                }
            }
        }
        for (int x = -9; x <= 9; x++) {
            for (int z = -14; z <= 12; z++) {
                if (Math.abs(x) <= 3) {
                    b.set(x, -1, z, ((x + z) & 1) == 0 ? "polished_blackstone_bricks" : "smooth_basalt");
                }
            }
        }
        // foso de lava delante de la fachada
        for (int x = -36; x <= 36; x++) {
            for (int z = 8; z <= 12; z++) {
                if (Math.abs(x) <= 6) {
                    b.set(x, -1, z, "polished_blackstone_bricks");
                } else if (z == 8 || z == 12) {
                    b.set(x, -1, z, "nether_bricks");
                } else {
                    b.set(x, -1, z, "lava");
                }
            }
        }
        // farolas con fuego a lo largo del camino
        for (int z = -12; z <= 6; z += 6) {
            for (int sx : new int[]{-4, 4}) {
                b.box(sx, 0, z, sx, 5, z, "polished_blackstone_brick_wall");
                b.set(sx, 6, z, "polished_blackstone_bricks");
                b.set(sx, 7, z, "campfire[lit=true]");
            }
        }
        // fachada y cuerpo principal
        facade(b);
        // chimeneas
        chimney(b, -24, 60, 5, 62);
        chimney(b, 0, 66, 6, 76);
        chimney(b, 24, 60, 5, 62);
        // gran salón elevado con tejado a dos aguas
        for (int y = 26; y <= 37; y++) {
            for (int x = -14; x <= 14; x++) {
                for (int z = 26; z <= 62; z++) {
                    if (x == -14 || x == 14 || z == 26 || z == 62) {
                        b.set(x, y, z, b.pick(BLK));
                    }
                }
            }
        }
        for (int y = 26; y <= 38; y++) {
            for (int[] c : new int[][]{{-14, 26}, {14, 26}, {-14, 62}, {14, 62}}) {
                b.set(c[0], y, c[1], y % 4 == 3 ? "chiseled_polished_blackstone" : "red_nether_bricks");
            }
        }
        for (int z = 32; z <= 56; z += 8) {
            for (int sx : new int[]{-14, 14}) {
                for (int y = 29; y <= 35; y++) {
                    b.set(sx, y, z, y % 2 == 0 ? "magma_block" : "orange_stained_glass");
                    b.set(sx, y, z + 1, y % 2 == 0 ? "magma_block" : "orange_stained_glass");
                }
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int y = 28; y <= 35; y++) {
                b.set(x, y, 26, Math.abs(x) <= 3 ? (y % 2 == 0 ? "magma_block" : "orange_stained_glass") : "polished_blackstone_bricks");
            }
        }
        gable(b, -15, 15, 26, 62, 38, "polished_blackstone_brick_stairs", "polished_blackstone_bricks");
        for (int z = 26; z <= 62; z += 4) {
            b.set(0, 53, z, "red_nether_bricks");
        }
        // gran yunque y martillo en el patio
        anvil(b, -24, 0);
        hammer(b, 24, 2);
        // interior: pozo de fundición
        interior(b);
        // cadenas con faroles en la entrada
        for (int x = -6; x <= 6; x += 3) {
            for (int y = 18; y > 12 + Math.abs(x) / 2; y--) {
                b.setIfAbsent(x, y, 13, "chain[axis=y]");
            }
            b.set(x, 12 + Math.abs(x) / 2, 13, "soul_lantern[hanging=true]");
        }
        return b;
    }

    private static void facade(StructBp b) {
        // cuerpo: z 14..74, x ±32, altura 24
        for (int y = 0; y <= 24; y++) {
            for (int x = -32; x <= 32; x++) {
                for (int z = 14; z <= 74; z++) {
                    boolean edge = x == -32 || x == 32 || z == 14 || z == 74 || z == 15 && Math.abs(x) > 6 || z == 73;
                    if (edge) {
                        b.set(x, y, z, y < 3 ? "polished_blackstone_bricks" : b.pick(BLK));
                    }
                }
            }
        }
        b.box(-32, 25, 14, 32, 25, 74, "polished_blackstone_bricks");
        b.box(-31, 25, 16, 31, 25, 72, "blackstone");
        // pilastras de ladrillo rojo cada 8 bloques con bandas
        for (int x = -32; x <= 32; x += 8) {
            for (int y = 0; y <= 27; y++) {
                for (int d = 0; d < 2; d++) {
                    b.set(x, y, 13 - d, y % 6 == 5 ? "chiseled_polished_blackstone" : NETH[(Math.abs(x / 8) + y) & 1]);
                }
            }
        }
        for (int z = 22; z <= 70; z += 8) {
            for (int sx : new int[]{-33, 33}) {
                for (int y = 0; y <= 27; y++) {
                    b.set(sx, y, z, y % 6 == 5 ? "chiseled_polished_blackstone" : "red_nether_bricks");
                }
            }
        }
        // ventanas de lava (rendijas con barrotes) en la fachada
        for (int x = -28; x <= 28; x += 8) {
            if (Math.abs(x) < 8) {
                continue;
            }
            for (int y = 8; y <= 16; y++) {
                b.set(x + 1, y, 14, y % 2 == 0 ? "magma_block" : "orange_stained_glass");
                b.set(x + 2, y, 14, y % 2 == 0 ? "magma_block" : "orange_stained_glass");
                b.set(x + 1, y, 13, "iron_bars");
                b.set(x + 2, y, 13, "iron_bars");
            }
        }
        // puerta gigante: arco apuntado con rastrillo de barrotes y marco de ladrillo rojo
        ogiveZ(b, 0, 0, 14, 17, 6, 18);
        for (int y = 5; y < 18; y++) {
            int hw = (int) Math.round(6 * Math.sqrt(Math.max(0.0, 1.0 - Math.pow(Math.max(0, y - 10) / 8.0, 2))));
            for (int x = -hw; x <= hw; x++) {
                if (y >= 10) {
                    b.set(x, y, 15, "iron_bars");
                }
            }
        }
        for (int y = 0; y < 21; y++) {
            for (int x : new int[]{-8, 8}) {
                b.set(x, y, 13, "red_nether_bricks");
                b.set(x, y, 12, "chiseled_polished_blackstone");
            }
        }
        for (int x = -8; x <= 8; x++) {
            b.set(x, 21, 13, "red_nether_bricks");
            b.set(x, 22, 13, "gilded_blackstone");
        }
        // almenas con pinchos
        for (int x = -32; x <= 32; x++) {
            for (int z : new int[]{14, 74}) {
                if ((x & 1) == 0) {
                    b.set(x, 26, z, "polished_blackstone_bricks");
                    b.set(x, 27, z, "polished_blackstone_brick_wall");
                }
            }
        }
        for (int z = 14; z <= 74; z++) {
            for (int x : new int[]{-32, 32}) {
                if ((z & 1) == 0) {
                    b.set(x, 26, z, "polished_blackstone_bricks");
                    b.set(x, 27, z, "polished_blackstone_brick_wall");
                }
            }
        }
        // la trasera también lleva pilastras y rendijas de lava
        for (int x = -32; x <= 32; x += 8) {
            for (int y = 0; y <= 27; y++) {
                b.set(x, y, 75, y % 6 == 5 ? "chiseled_polished_blackstone" : "red_nether_bricks");
                b.set(x, y, 76, y % 6 == 5 ? "chiseled_polished_blackstone" : "red_nether_bricks");
            }
        }
        for (int x = -28; x <= 28; x += 8) {
            for (int y = 8; y <= 18; y++) {
                b.set(x + 3, y, 74, y % 2 == 0 ? "magma_block" : "orange_stained_glass");
                b.set(x + 4, y, 74, y % 2 == 0 ? "magma_block" : "orange_stained_glass");
            }
        }
        // interior vacío
        b.air(-31, 0, 16, 31, 24, 72);
        b.box(-31, -1, 16, 31, -1, 72, "polished_blackstone_bricks");
        // torres cuadradas de la fachada
        for (int sx : new int[]{-1, 1}) {
            for (int y = 0; y <= 34; y++) {
                for (int x = 14; x <= 24; x++) {
                    for (int z = 8; z <= 18; z++) {
                        boolean edge = x == 14 || x == 24 || z == 8 || z == 18;
                        if (edge) {
                            b.set(sx * x, y, z, y < 3 ? "polished_blackstone_bricks" : b.pick(BLK));
                        }
                    }
                }
            }
            for (int x = 13; x <= 25; x++) {
                for (int z = 7; z <= 19; z++) {
                    b.set(sx * x, 35, z, "polished_blackstone_bricks");
                }
            }
            for (int[] c : new int[][]{{14, 8}, {24, 8}, {14, 18}, {24, 18}}) {
                for (int y = 0; y <= 36; y++) {
                    b.set(sx * c[0], y, c[1], y % 5 == 4 ? "chiseled_polished_blackstone" : "red_nether_bricks");
                }
            }
            pyramid(b, sx * 19, 13, 6, 36, "polished_blackstone_bricks", "blackstone", "polished_blackstone_bricks", "red_nether_bricks");
            for (int y = 10; y < 28; y += 6) {
                for (int z = 11; z <= 15; z += 4) {
                    b.set(sx * 14, y, z, "magma_block");
                    b.set(sx * 24, y, z, "magma_block");
                }
            }
        }
    }

    private static void chimney(StructBp b, int cx, int cz, int r, int h) {
        // fuste cuadrado hueco que se ensancha en tres collares hacia arriba
        for (int y = 10; y < h; y++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) == r || Math.abs(z) == r) {
                        b.set(cx + x, y, cz + z, y % 8 == 7 ? "red_nether_bricks" : b.pick(BLK));
                    }
                }
            }
        }
        for (int c = 0; c < 3; c++) {
            int rr = r + 1 + c;
            for (int y = h - 3 + c; y <= h - 3 + c; y++) {
                for (int x = -rr; x <= rr; x++) {
                    for (int z = -rr; z <= rr; z++) {
                        if (Math.abs(x) == rr || Math.abs(z) == rr) {
                            b.set(cx + x, y + 0, cz + z, c == 1 ? "red_nether_bricks" : "polished_blackstone_bricks");
                        }
                    }
                }
            }
        }
        int top = h + 1;
        for (int x = -r - 3; x <= r + 3; x++) {
            for (int z = -r - 3; z <= r + 3; z++) {
                if (Math.abs(x) == r + 3 || Math.abs(z) == r + 3) {
                    b.set(cx + x, top, cz + z, "polished_blackstone_bricks");
                    if (((x + z) & 1) == 0) {
                        b.set(cx + x, top + 1, cz + z, "polished_blackstone_brick_wall");
                    }
                }
            }
        }
        // brasa en la boca
        b.box(cx - r + 1, h - 3, cz - r + 1, cx + r - 1, h - 3, cz + r - 1, "magma_block");
        b.box(cx - r + 1, h - 2, cz - r + 1, cx + r - 1, h - 2, cz + r - 1, "campfire[lit=true]");
        // cuernos de hierro en las esquinas del collar
        for (int[] c : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
            for (int y = 0; y < 4; y++) {
                b.set(cx + c[0] * (r + 3), top + 1 + y, cz + c[1] * (r + 3), y == 3 ? "campfire[lit=true]" : "polished_blackstone_brick_wall");
            }
        }
        // rendijas de lava a lo largo del fuste
        for (int y = 14; y < h - 6; y += 9) {
            for (int a = -1; a <= 1; a += 2) {
                b.set(cx + a, y, cz - r, "magma_block");
                b.set(cx + a, y + 1, cz - r, "magma_block");
                b.set(cx + a, y + 2, cz - r, "magma_block");
            }
        }
    }

    /** Tejado a dos aguas con la cumbrera a lo largo de z. */
    private static void gable(StructBp b, int x0, int x1, int z0, int z1, int y0, String stairBase, String fill) {
        for (int i = 0; x0 + i <= x1 - i; i++) {
            int xl = x0 + i, xr = x1 - i, y = y0 + i;
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                if (xl == xr) {
                    b.set(xl, y, z, fill);
                } else {
                    b.set(xl, y, z, StructBp.stair(stairBase, "+x", false));
                    b.set(xr, y, z, StructBp.stair(stairBase, "-x", false));
                    b.set(xl + 1, y, z, fill);
                    b.set(xr - 1, y, z, fill);
                }
            }
        }
        for (int x = x0; x <= x1; x++) {
            int hh = Math.min(x - x0, x1 - x);
            for (int y = 0; y < hh; y++) {
                b.set(x, y0 + y, z0, fill);
                b.set(x, y0 + y, z1, fill);
            }
        }
    }

    /** Aguja de pirámide escalonada cuadrada con un fuego arriba. */
    private static void pyramid(StructBp b, int cx, int cz, int r, int y0, String... pal) {
        for (int i = 0; i <= r; i++) {
            b.box(cx - r + i, y0 + i * 2, cz - r + i, cx + r - i, y0 + i * 2 + 1, cz + r - i, b.pick(pal));
        }
        b.box(cx, y0 + r * 2 + 2, cz, cx, y0 + r * 2 + 4, cz, "polished_blackstone_brick_wall");
        b.set(cx, y0 + r * 2 + 5, cz, "campfire[lit=true]");
    }

    private static void anvil(StructBp b, int cx, int cz) {
        b.box(cx - 5, 0, cz - 4, cx + 5, 2, cz + 4, "polished_blackstone_bricks");
        b.box(cx - 4, 3, cz - 3, cx + 4, 5, cz + 3, "blackstone");
        b.box(cx - 8, 6, cz - 4, cx + 8, 8, cz + 4, "deepslate_tiles");
        b.box(cx - 8, 9, cz - 4, cx + 8, 9, cz + 4, "iron_block");
        b.box(cx + 9, 7, cz - 3, cx + 12, 8, cz + 3, "deepslate_tiles");
        b.box(cx + 13, 7, cz - 2, cx + 14, 8, cz + 2, "deepslate_tiles");
        b.set(cx, 10, cz, "campfire[lit=true]");
        b.set(cx - 3, 10, cz, "lava");
    }

    private static void hammer(StructBp b, int cx, int cz) {
        b.box(cx - 1, 0, cz - 1, cx + 1, 16, cz + 1, "dark_oak_log[axis=y]");
        b.box(cx - 5, 17, cz - 2, cx + 5, 21, cz + 2, "deepslate_tiles");
        b.box(cx - 5, 22, cz - 2, cx + 5, 22, cz + 2, "iron_block");
        b.box(cx - 4, 18, cz - 3, cx + 4, 20, cz + 3, "polished_blackstone_bricks");
        b.set(cx, 23, cz, "campfire[lit=true]");
    }

    private static void interior(StructBp b) {
        // pozo de lava central con pasarela
        for (int x = -10; x <= 10; x++) {
            for (int z = 34; z <= 54; z++) {
                double d = Math.hypot(x, (z - 44) * 0.9);
                if (d < 7) {
                    b.set(x, -1, z, "lava");
                } else if (d < 9) {
                    b.set(x, -1, z, "nether_bricks");
                }
            }
        }
        for (int z = 22; z <= 66; z++) {
            for (int x = -2; x <= 2; x++) {
                if (z >= 36 && z <= 52) {
                    b.set(x, -1, z, "polished_blackstone_bricks");
                    b.set(x, 0, z, "air");
                }
            }
        }
        for (int z = 22; z <= 30; z += 4) {
            for (int sx : new int[]{-12, 12}) {
                b.box(sx, 0, z, sx, 22, z, "red_nether_bricks");
                b.set(sx, 5, z + 1, "lantern");
            }
        }
        b.box(-6, 0, 66, 6, 3, 70, "polished_blackstone_bricks");
        b.box(-4, 4, 67, 4, 4, 69, "gilded_blackstone");
        b.box(-3, 5, 68, 3, 8, 68, "red_nether_bricks");
    }

    // ================================================================== TORRE: Fortaleza en ruinas

    private static final String[] RUIN = {"stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "stone_bricks", "andesite", "mossy_stone_bricks", "stone_bricks"};

    private static StructBp torre() {
        StructBp b = new StructBp(4004);
        b.foundation(-34, -10, 34, 66, "stone_bricks");
        for (int x = -34; x <= 34; x++) {
            for (int z = -10; z <= 66; z++) {
                b.set(x, -1, z, b.pick("stone_bricks", "cobblestone", "mossy_cobblestone", "gravel", "coarse_dirt", "mossy_stone_bricks", "stone_bricks"));
            }
        }
        // camino al portón
        for (int x = -3; x <= 3; x++) {
            for (int z = -10; z <= 12; z++) {
                b.set(x, -1, z, b.pick("stone_bricks", "mossy_stone_bricks", "stone_bricks", "cracked_stone_bricks"));
            }
        }
        // muralla (altura variable por ruina) con portón
        for (int y = 0; y < 18; y++) {
            for (int x = -33; x <= 33; x++) {
                for (int z : new int[]{10, 11, 12, 62, 63, 64}) {
                    b.set(x, y, z, b.pick(RUIN));
                }
            }
            for (int z = 10; z <= 64; z++) {
                for (int x : new int[]{-33, -32, -31, 31, 32, 33}) {
                    b.set(x, y, z, b.pick(RUIN));
                }
            }
        }
        ogiveZ(b, 0, 0, 10, 12, 4, 14);
        // torres de la puerta (cuadradas)
        for (int sx : new int[]{-1, 1}) {
            squareTower(b, sx * 11, 6, 6, 28);
        }
        // torres redondas
        for (int sx : new int[]{-1, 1}) {
            roundTower(b, sx * 31, 11, 6, 30);
            roundTower(b, sx * 31, 63, 6, 36);
        }
        // torreón central
        roundTower(b, 0, 42, 10, 56);
        // ruinas: alturas irregulares
        ruin(b, -34, 8, 34, 14, 6, 16, 0.55);
        ruin(b, -34, 60, 34, 66, 6, 16, 0.55);
        ruin(b, -34, 10, -29, 66, 4, 16, 0.5);
        ruin(b, 29, 10, 34, 66, 4, 16, 0.5);
        // el torreón se derrumba por un costado
        for (var e : new java.util.ArrayList<>(b.m.entrySet())) {
            long k = e.getKey();
            int x = StructBp.kx(k), y = StructBp.ky(k), z = StructBp.kz(k);
            if (Math.abs(x) <= 11 && z >= 31 && z <= 53 && y > 30) {
                double cut = 30 + 14 * (0.5 + 0.5 * Math.sin(x * 0.7 + z * 0.4)) + (x + 10) * 0.9;
                if (y > cut) {
                    b.m.remove(k);
                }
            }
        }
        // escombros
        for (int i = 0; i < 160; i++) {
            int x = b.r.nextInt(55) - 27, z = 14 + b.r.nextInt(46);
            if (b.solid(x, 0, z) || Math.abs(x) < 4 && z < 40) {
                continue;
            }
            int hh = 1 + b.r.nextInt(3);
            for (int y = 0; y < hh; y++) {
                b.setIfAbsent(x, y, z, b.pick("cobblestone", "mossy_cobblestone", "stone_bricks", "cracked_stone_bricks", "stone_brick_slab"));
            }
        }
        // columnas caídas
        for (int i = 0; i < 6; i++) {
            int x = -22 + b.r.nextInt(44), z = 16 + b.r.nextInt(30);
            for (int d = 0; d < 6; d++) {
                b.setIfAbsent(x + d, 0, z, "stone_bricks");
            }
        }
        // braseros a lo largo del camino y estandartes rojos en el portón
        for (int z = 16; z <= 40; z += 8) {
            for (int sx : new int[]{-1, 1}) {
                b.box(sx * 6, 0, z, sx * 6, 2, z, "cobblestone_wall");
                b.set(sx * 6, 3, z, "campfire[lit=true]");
            }
        }
        for (int x = -3; x <= 3; x += 3) {
            if (x == 0) {
                continue;
            }
            for (int y = 8; y <= 13; y++) {
                b.set(x, y, 9, "red_wool");
            }
        }
        moss(b);
        return b;
    }

    private static void squareTower(StructBp b, int cx, int cz, int r, int h) {
        for (int y = 0; y <= h; y++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) == r || Math.abs(z) == r) {
                        b.set(cx + x, y, cz + z, b.pick(RUIN));
                    }
                }
            }
        }
        for (int y = 8; y < h - 3; y += 7) {
            for (int s = -1; s <= 1; s += 2) {
                b.box(cx + s * r, y, cz - 1, cx + s * r, y + 2, cz + 1, "air");
                b.box(cx - 1, y, cz + s * r, cx + 1, y + 2, cz + s * r, "air");
            }
        }
        for (int a = -r; a <= r; a++) {
            if ((a & 1) == 0) {
                for (int[] c : new int[][]{{a, -r}, {a, r}, {-r, a}, {r, a}}) {
                    b.set(cx + c[0], h + 1, cz + c[1], "stone_bricks");
                }
            }
        }
        for (int a = -r - 1; a <= r + 1; a++) {
            for (int c : new int[]{-r - 1, r + 1}) {
                b.set(cx + a, h, cz + c, "stone_brick_slab");
                b.set(cx + c, h, cz + a, "stone_brick_slab");
            }
        }
    }

    private static void roundTower(StructBp b, int cx, int cz, int r, int h) {
        b.tube(cx, cz, r, 2.5, 0, h, RUIN);
        for (int y = 12; y < h - 2; y += 12) {
            b.disc(cx, cz, r - 2.4, y, y, "dark_oak_planks");
        }
        for (int y = 6; y < h - 4; y += 9) {
            for (int a = 0; a < 6; a++) {
                double ang = Math.PI * a / 3 + 0.3;
                int wx = cx + (int) Math.round(Math.cos(ang) * r), wz = cz + (int) Math.round(Math.sin(ang) * r);
                b.box(wx, y, wz, wx, y + 2, wz, "air");
            }
        }
        b.tube(cx, cz, r + 1.5, 1.5, h, h, "stone_brick_slab");
        for (int a = 0; a < 20; a++) {
            double ang = Math.PI * a / 10;
            if ((a & 1) == 0) {
                b.set(cx + (int) Math.round(Math.cos(ang) * (r + 1)), h + 1, cz + (int) Math.round(Math.sin(ang) * (r + 1)), "stone_bricks");
            }
        }
    }

    /** Recorta los muros a alturas irregulares: en cada columna de la zona se borra todo lo que supere un tope al azar. */
    private static void ruin(StructBp b, int x0, int z0, int x1, int z1, int minH, int maxH, double wobble) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                double n = 0.5 + 0.5 * Math.sin(x * 0.45 + 2.1) * Math.cos(z * 0.33 + 0.7) + (b.r.nextDouble() - 0.5) * wobble;
                int top = (int) Math.round(minH + (maxH - minH) * n);
                for (int y = top; y <= 60; y++) {
                    b.m.remove(StructBp.key(x, y, z));
                }
            }
        }
    }

    private static void moss(StructBp b) {
        for (var e : new java.util.ArrayList<>(b.m.entrySet())) {
            long k = e.getKey();
            int x = StructBp.kx(k), y = StructBp.ky(k), z = StructBp.kz(k);
            if (y >= 0 && e.getValue().contains("stone") && !b.m.containsKey(StructBp.key(x, y + 1, z)) && b.r.nextDouble() < 0.10) {
                b.setIfAbsent(x, y + 1, z, b.pick("moss_carpet", "vine[up=true]", "fern", "moss_carpet"));
            }
        }
    }
}
