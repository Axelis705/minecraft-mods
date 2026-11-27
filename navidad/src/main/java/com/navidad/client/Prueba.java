package com.navidad.client;

import com.axeliscore.test.ClientTest;
import com.navidad.Navidad;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * La prueba con fotos (cliente-core.yml con mod=navidad y escenario=navidad): la galería de decoración de día y de noche, todos los
 * objetos en marcos, la ropa puesta, la piñata (entera, columpiándose y rota) y lo demás que se vaya agregando.
 */
final class Prueba {
    private Prueba() {
    }

    static List<String> bloques() {
        List<String> out = new ArrayList<>();
        for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys()) {
            if (id.getNamespace().equals(Navidad.MODID)) {
                out.add(id.getPath());
            }
        }
        out.sort(String::compareTo);
        return out;
    }

    static List<String> objetos() {
        List<String> out = new ArrayList<>();
        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            Item it = ForgeRegistries.ITEMS.getValue(id);
            if (id.getNamespace().equals(Navidad.MODID) && (!(it instanceof BlockItem) || id.getPath().equals("caja_regalo")
                    || id.getPath().equals("calendario_adviento"))) {
                out.add(id.getPath());
            }
        }
        out.sort(String::compareTo);
        return out;
    }

    /** Un regalo de Santa que ya abrió su paracaídas (el más cercano), o null. */
    static com.navidad.entidad.RegaloCayendo regaloAbierto() {
        var mc = Minecraft.getInstance();
        com.navidad.entidad.RegaloCayendo mejor = null;
        for (var e : mc.level.entitiesForRendering()) {
            if (e instanceof com.navidad.entidad.RegaloCayendo r && r.abierto() && r.getY() > -57
                    && (mejor == null || r.distanceToSqr(mc.player) < mejor.distanceToSqr(mc.player))) {
                mejor = r;
            }
        }
        return mejor;
    }

    /** El pingüino más cercano (de alguien o sin dueño, adulto). */
    static com.navidad.entidad.Pinguino pinguino(boolean domado) {
        var mc = Minecraft.getInstance();
        com.navidad.entidad.Pinguino mejor = null;
        for (var e : mc.level.entitiesForRendering()) {
            if (e instanceof com.navidad.entidad.Pinguino p && p.isTame() == domado && !p.isBaby() && p.isAlive()
                    && (mejor == null || p.distanceToSqr(mc.player) < mejor.distanceToSqr(mc.player))) {
                mejor = p;
            }
        }
        return mejor;
    }

    /** El duende ladrón más cercano, o null. */
    static com.navidad.entidad.DuendeLadron ladron() {
        var mc = Minecraft.getInstance();
        for (var e : mc.level.entitiesForRendering()) {
            if (e instanceof com.navidad.entidad.DuendeLadron d && d.isAlive()) {
                return d;
            }
        }
        return null;
    }

    /** Don Abeto (el del puesto de árboles), si está cargado. */
    static com.axeliscore.npc.Npc abeto() {
        var mc = Minecraft.getInstance();
        for (var e : mc.level.entitiesForRendering()) {
            if (e instanceof com.axeliscore.npc.Npc n && n.def() == com.navidad.gente.Gente.PINERO) {
                return n;
            }
        }
        return null;
    }

    /** El duende viajero, si está cargado. */
    static com.axeliscore.npc.Npc viajero() {
        var mc = Minecraft.getInstance();
        for (var e : mc.level.entitiesForRendering()) {
            if (e instanceof com.axeliscore.npc.Npc n && n.def() == com.navidad.gente.Gente.VIAJERO) {
                return n;
            }
        }
        return null;
    }

    static void registrar() {
        ClientTest.scenario(Navidad.MODID, t -> {
            t.add("preparar", 2500, () -> {
                com.navidad.Fiestas.automatico = false;
                t.cmd("gamemode creative @a");
                t.cmd("clear @a");
                t.cmd("time set 6000");
                t.cmd("weather clear");
                t.cmd("gamerule doDaylightCycle false");
                t.cmd("gamerule doMobSpawning false");
                t.cmd("gamerule doWeatherCycle false");
                t.cmd("navidad temporada si");
                t.cmd("kill @e[type=minecraft:slime]");
                t.cmd("fill -14 -61 0 14 -61 26 minecraft:snow_block");
                t.cmd("fill -14 -60 27 14 -55 27 minecraft:spruce_planks");
            });
            t.add("comando", 1200, () -> t.cmd(Navidad.MODID));
            // ¿la letra del juego trae los símbolos y emojis que usan los textos del mod? (los que no, salen como cajita)
            t.add("emojis", 300, () -> {
                try {
                    var f = net.minecraft.client.Minecraft.getInstance().font;
                    net.minecraft.client.gui.font.FontSet fs = null;
                    for (var fld : net.minecraft.client.gui.Font.class.getDeclaredFields()) {
                        if (java.util.function.Function.class.isAssignableFrom(fld.getType())) {
                            fld.setAccessible(true);
                            fs = (net.minecraft.client.gui.font.FontSet) ((java.util.function.Function<Object, Object>) fld.get(f))
                                    .apply(net.minecraft.network.chat.Style.DEFAULT_FONT);
                        }
                    }
                    var rm = net.minecraft.client.Minecraft.getInstance().getResourceManager();
                    java.util.TreeSet<Integer> usados = new java.util.TreeSet<>();
                    for (String code : new String[]{"es_mx", "en_us"}) {
                        var r = rm.getResource(new net.minecraft.resources.ResourceLocation(Navidad.MODID, "lang/" + code + ".json"));
                        if (r.isPresent()) {
                            try (var in = r.get().open()) {
                                new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).codePoints()
                                        .filter(c -> c >= 0x2000 && c != 0x2026 && c != 0x2014 && c != 0x2013).forEach(usados::add);
                            }
                        }
                    }
                    StringBuilder faltan = new StringBuilder();
                    for (int cp : usados) {
                        if (fs != null && fs.getGlyphInfo(cp, false) == net.minecraft.client.gui.font.glyphs.SpecialGlyphs.MISSING) {
                            faltan.append(' ').append(Integer.toHexString(cp));
                        }
                    }
                    Navidad.LOGGER.info("NAVIDAD_EMOJI usados={} faltan:{}", usados.size(), faltan.length() == 0 ? " ninguno" : faltan);
                } catch (Exception e) {
                    Navidad.LOGGER.info("NAVIDAD_EMOJI no se pudo revisar: {}", e.toString());
                }
            });
            // galería: diez por fila, cada dos bloques; filas cada tres
            t.add("galeria", 2500, () -> {
                t.cmd("kill @e[type=minecraft:slime]");
                List<String> bs = bloques();
                t.log("bloques: " + bs.size());
                for (int i = 0; i < bs.size(); i++) {
                    int x = -9 + (i % 10) * 2, z = 3 + (i / 10) * 3;
                    t.cmd(String.format(Locale.ROOT, "setblock %d -60 %d navidad:%s", x, z, bs.get(i)));
                }
            });
            t.add("hud", 200, () -> t.hud(false));
            t.photo("galeria_1", 3000, () -> t.lookFrom(0.5, -54, -5, 0.5, -60, 7));
            t.photo("galeria_2", 2500, () -> t.lookFrom(0.5, -54, 4, 0.5, -60, 16));
            t.photo("galeria_cerca", 2500, () -> t.lookFrom(-3.5, -58.5, -1.5, -3.5, -59.5, 5));
            t.photo("galeria_noche", 3500, () -> {
                t.cmd("time set 18000");
                t.lookFrom(0.5, -55, -3, 0.5, -60, 9);
            });
            t.add("dia", 1000, () -> t.cmd("time set 6000"));
            // todos los objetos en marcos, en la pared de tablones
            t.add("marcos", 2500, () -> {
                t.cmd("kill @e[type=minecraft:slime]");
                List<String> os = objetos();
                t.log("objetos: " + os.size());
                for (int i = 0; i < os.size(); i++) {
                    int x = -6 + (i % 12), y = -56 - (i / 12);
                    t.cmd(String.format(Locale.ROOT, "summon item_frame %d %d 26 {Facing:2b,Fixed:1b,Invisible:0b,Item:{id:\"navidad:%s\",Count:1b}}",
                            x, y, os.get(i)));
                }
            });
            t.photo("objetos", 3000, () -> t.lookFrom(0.5, -59.6, 19.5, 0.5, -57.5, 26));
            // la ropa (cámara de frente en tercera persona)
            t.add("ropa 1", 1500, () -> {
                t.cmd("tp @a 0.5 -60 -20.5 0 0");
                t.cmd("execute as @p run navidad ropa sueter_reno");
                t.cmd("execute as @p run navidad ropa gorro_santa");
                Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            });
            t.photo("ropa_reno_gorro", 2000, () -> t.cmd("tp @a 0.5 -60 -20.5 0 5"));
            t.photo("ropa_santa", 2000, () -> {
                t.cmd("execute as @p run navidad ropa traje_santa");
                t.cmd("execute as @p run navidad ropa gorro_santa");
            });
            t.photo("ropa_pinos_duende", 2000, () -> {
                t.cmd("execute as @p run navidad ropa sueter_pinos");
                t.cmd("execute as @p run navidad ropa gorro_duende");
            });
            t.photo("ropa_copos_cuernos", 2000, () -> {
                t.cmd("execute as @p run navidad ropa sueter_copos");
                t.cmd("execute as @p run navidad ropa cuernos_reno");
            });
            t.photo("ropa_lado", 1500, () -> t.cmd("tp @a 0.5 -60 -20.5 60 5"));
            t.photo("ropa_bufanda_orejeras", 2000, () -> {
                t.cmd("execute as @p run navidad ropa sueter_pinos");
                t.cmd("execute as @p run navidad ropa orejeras");
                t.cmd("execute as @p run navidad ropa bufanda");
                t.cmd("tp @a 0.5 -60 -20.5 20 5");
            });
            t.add("camara normal", 500, () -> Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON));
            // la piñata
            t.add("pinata", 2500, () -> {
                t.cmd("fill -3 -55 -36 3 -55 -30 minecraft:spruce_planks");
                t.cmd("tp @a 0.5 -60 -36.5 0 0");
                t.cmd("execute as @p at @p rotated 0 0 run navidad pinata");
            });
            t.photo("pinata", 2000, () -> t.lookFrom(0.5, -60, -38.5, 0.5, -58.6, -34));
            t.add("golpes", 500, () -> {
                t.cmd("item replace entity @p weapon.mainhand with navidad:palo_pinata");
                for (int i = 0; i < 4; i++) {
                    t.cmd("damage @e[type=navidad:pinata,limit=1] 1 minecraft:player_attack by @p");
                }
            });
            t.photo("pinata_columpio", 250, () -> {
            });
            for (int k = 0; k < 9; k++) {
                t.add("golpe " + k, 120, () -> t.cmd("damage @e[type=navidad:pinata,limit=1] 1 minecraft:player_attack by @p"));
            }
            t.add("rota?", 200, () -> t.log("pinatas que quedan: " + Minecraft.getInstance().level.getEntitiesOfClass(
                    com.navidad.entidad.Pinata.class, Minecraft.getInstance().player.getBoundingBox().inflate(20)).size()));
            t.photo("pinata_rota", 500, () -> {
            });
            t.photo("pinata_dulces", 2500, () -> t.lookFrom(0.5, -58.5, -38, 0.5, -60.5, -34));
            // las criaturas (quietas, en fila)
            t.add("criaturas", 2500, () -> {
                t.cmd("fill -10 -61 -66 10 -61 -54 minecraft:snow_block");
                t.cmd("kill @e[type=minecraft:slime]");
                t.cmd("tp @a 0.5 -60 -50.5 180 0");
                t.cmd("summon navidad:muneco_malvado -5 -60 -60 {NoAI:1b,Rotation:[0f,0f]}");
                t.cmd("summon navidad:reno -1 -60 -60 {NoAI:1b,Rotation:[20f,0f]}");
                t.cmd("summon navidad:hombre_jengibre 2.5 -60 -59 {NoAI:1b,Rotation:[-10f,0f]}");
                t.cmd("summon navidad:krampus 7 -60 -62 {NoAI:1b,Rotation:[-20f,0f]}");
            });
            t.photo("criaturas", 3000, () -> t.lookFrom(0.5, -58, -51, 0.5, -59, -61));
            t.photo("krampus_cerca", 2000, () -> t.lookFrom(4, -58, -56, 7, -58, -62));
            t.add("quitar criaturas", 500, () -> t.cmd("kill @e[type=!player,x=0,y=-60,z=-60,distance=..20]"));
            // en temporada, los mobs con cabeza de persona traen gorro (bajo techo, para que no se quemen con el sol)
            t.add("gorros", 2500, () -> {
                t.cmd("fill -25 -61 -64 -9 -61 -54 minecraft:snow_block");
                t.cmd("fill -25 -56 -64 -9 -56 -57 minecraft:spruce_slab");
                String nbt = "{NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}";
                t.cmd("summon minecraft:zombie -22 -60 -60 " + nbt);
                t.cmd("summon minecraft:villager -19.5 -60 -60 " + nbt);
                t.cmd("summon minecraft:skeleton -17 -60 -60 " + nbt);
                t.cmd("summon minecraft:pillager -14.5 -60 -60 " + nbt);
                t.cmd("summon minecraft:zombie_villager -12 -60 -60 " + nbt);
            });
            t.photo("gorros_mobs", 2000, () -> t.lookFrom(-17, -59.2, -54.5, -17, -58.6, -60));
            t.add("quitar gorros", 500, () -> t.cmd("kill @e[type=!player,x=-17,y=-60,z=-60,distance=..9]"));
            // la aldea de Santa, lejos de lo demás
            t.add("ir a la aldea", 4000, () -> t.cmd("tp @a 300.5 -40 45.5 180 30"));
            t.add("aldea", 6000, () -> t.cmd("execute positioned 300 -60 0 run navidad aldea"));
            t.add("volar", 500, () -> t.cmd("gamemode spectator @a"));
            t.photo("aldea_arriba", 5000, () -> t.lookFrom(300.5, -28, 44, 300.5, -58, 2));
            t.photo("aldea_arriba_lado", 3000, () -> t.lookFrom(338, -32, -30, 300.5, -58, 0));
            t.add("caminar", 500, () -> t.cmd("gamemode creative @a"));
            t.photo("aldea_plaza", 3000, () -> t.lookFrom(300.5, -59, 13, 300.5, -54, 0));
            t.photo("aldea_santa", 3000, () -> t.lookFrom(300.5, -59, -12, 300.5, -59, -20));
            t.photo("aldea_casa_dentro", 3000, () -> t.lookFrom(300.5, -59, -18.5, 300.5, -59.5, -26));
            t.photo("aldea_taller", 3000, () -> t.lookFrom(312, -57, -6, 322, -59, 0));
            t.photo("aldea_taller_dentro", 3000, () -> t.lookFrom(318.5, -59, 0.5, 324, -59.5, -4));
            t.photo("aldea_cocina", 3000, () -> t.lookFrom(288, -57, 6, 278, -59, 0));
            t.photo("aldea_cocina_dentro", 3000, () -> t.lookFrom(282.5, -59, 0.5, 276, -59.5, 0));
            t.photo("aldea_establo", 3000, () -> t.lookFrom(300.5, -57, 12, 300.5, -59, 22));
            t.photo("aldea_nacimiento", 3000, () -> t.lookFrom(280.5, -56.2, -11, 280.5, -59.5, -21.5));
            t.photo("aldea_nacimiento_cerca", 2500, () -> t.lookFrom(283.5, -57.6, -17.5, 280.5, -59, -21.5));
            t.photo("aldea_noche", 4000, () -> {
                t.cmd("time set 18000");
                t.cmd("gamemode spectator @a");
                t.lookFrom(300.5, -40, 40, 300.5, -56, 0);
            });
            t.add("caminar 2", 500, () -> t.cmd("gamemode creative @a"));
            t.photo("aldea_pino_noche", 3000, () -> t.lookFrom(300.5, -58, 12, 300.5, -51, 0));
            t.photo("aldea_nacimiento_noche", 3000, () -> t.lookFrom(280.5, -56.2, -12, 280.5, -59, -21.5));
            t.add("trineo", 1500, () -> {
                t.cmd("gamemode spectator @a");
                t.lookFrom(352, -52, 2, 340, -46, -10);
                t.cmd("summon navidad:trineo_santa 340 -44 -22 {Motion:[0.0,0.0,0.25]}");
            });
            t.photo("trineo_noche", 200, () -> {
            });
            t.photo("trineo_noche_2", 1200, () -> {
            });
            t.add("caminar 3", 300, () -> t.cmd("gamemode creative @a"));
            t.add("dia otra vez", 1000, () -> t.cmd("time set 6000"));
            // ---------------------------------------------------------------- Santa en survival: pasa de día y suelta regalos con paracaídas
            t.add("santa de dia", 1500, () -> {
                t.cmd("time set 9000");
                t.cmd("gamemode survival @a");
                t.cmd("kill @e[type=navidad:regalo_cayendo]");
                com.navidad.entidad.RegaloCayendo.aterrizados = 0;
                t.cmd("tp @a 440.5 -60 80.5 90 -25");
                t.cmd("summon navidad:trineo_santa 400 -40 80.5 {Motion:[0.5,0.0,0.0]}");
            });
            t.photo("santa_viene", 2200, () -> {
            });
            t.waitFor("se abrió un paracaídas", 15000, () -> regaloAbierto() != null);
            t.add("que baje un poco", 1500, () -> {
            });
            t.photo("regalo_paracaidas", 150, () -> {
                var r = regaloAbierto();
                if (r != null) {
                    t.lookFrom(r.getX() + 3.4, r.getY() - 0.4, r.getZ() + 3.4, r.getX(), r.getY() + 0.7, r.getZ());
                }
            });
            t.add("volver al piso", 300, () -> t.cmd("tp @a 440.5 -60 80.5 -90 30"));
            t.waitFor("el regalo aterrizó como caja", 40000, () -> com.navidad.entidad.RegaloCayendo.aterrizados > 0);
            t.add("buscar la caja", 1200, () -> {
                var l = Minecraft.getInstance().level;
                for (BlockPos q : BlockPos.betweenClosed(425, -60, 65, 455, -58, 95)) {
                    if (l.getBlockState(q).is(com.navidad.registry.ModRegistry.CAJA_REGALO.get())) {
                        t.lookFrom(q.getX() + 2.5, -58.8, q.getZ() + 2.5, q.getX() + 0.5, -59.7, q.getZ() + 0.5);
                        break;
                    }
                }
            });
            t.photo("regalo_aterrizo", 600, () -> {
            });
            t.add("fin santa", 300, () -> t.cmd("gamemode creative @a"));
            // ---------------------------------------------------------------- el pingüino: en grupito con un bebé, domarlo con pescado, sentarlo,
            // verlo deslizarse de panza en el hielo y nadar
            t.add("pinguinos", 1500, () -> {
                t.cmd("time set 6000");
                t.cmd("gamemode creative @a");
                t.cmd("fill 450 -60 90 490 -54 130 minecraft:air");
                t.cmd("fill 450 -61 90 490 -61 130 minecraft:packed_ice");
                t.cmd("fill 476 -61 112 486 -61 124 minecraft:water");
                t.cmd("kill @e[type=navidad:pinguino]");
                t.cmd("summon navidad:pinguino 460 -60 100 {NoAI:1b,Rotation:[20f,0f]}");
                t.cmd("summon navidad:pinguino 461.5 -60 101 {NoAI:1b,Rotation:[40f,0f]}");
                t.cmd("summon navidad:pinguino 459 -60 101.5 {NoAI:1b,Age:-24000,Rotation:[10f,0f]}");
            });
            t.photo("pinguinos", 1500, () -> t.lookFrom(462.6, -59.0, 104.2, 460.2, -59.5, 100.6));
            t.add("despertar pinguinos", 300, () -> t.cmd("execute as @e[type=navidad:pinguino] run data merge entity @s {NoAI:0b}"));
            for (int k = 0; k < 12; k++) {
                t.add("darle pescado " + k, 700, () -> {
                    if (com.navidad.entidad.Pinguino.domados > 0) {
                        return;
                    }
                    var mc = Minecraft.getInstance();
                    var pg = pinguino(false);
                    if (pg != null) {
                        t.cmd("item replace entity @p weapon.mainhand with minecraft:cod 8");
                        t.lookFrom(pg.getX() + 1.6, pg.getY(), pg.getZ() + 1.6, pg.getX(), pg.getY() + 0.5, pg.getZ());
                    }
                });
                t.add("dar " + k, 300, () -> {
                    var mc = Minecraft.getInstance();
                    var pg = pinguino(false);
                    if (pg != null && com.navidad.entidad.Pinguino.domados == 0) {
                        mc.gameMode.interact(mc.player, pg, InteractionHand.MAIN_HAND);
                    }
                });
            }
            t.waitFor("el pingüino es mío", 3000, () -> com.navidad.entidad.Pinguino.domados > 0);
            t.add("mirar mi pinguino", 1500, () -> {
                t.cmd("item replace entity @p weapon.mainhand with minecraft:air");
                var pg = pinguino(true);
                if (pg != null) {
                    t.lookFrom(pg.getX() - 1.4, pg.getY() + 0.4, pg.getZ() - 1.8, pg.getX(), pg.getY() + 0.55, pg.getZ());
                }
            });
            t.photo("pinguino_mio", 600, () -> {
            });
            t.add("sentarlo", 1500, () -> {
                var mc = Minecraft.getInstance();
                var pg = pinguino(true);
                if (pg != null) {
                    mc.gameMode.interact(mc.player, pg, InteractionHand.MAIN_HAND);
                }
            });
            t.photo("pinguino_sentado", 600, () -> {
                var pg = pinguino(true);
                t.check(pg != null && pg.isInSittingPose(), "el pingüino se sentó");
            });
            t.add("pararlo", 800, () -> {
                var mc = Minecraft.getInstance();
                var pg = pinguino(true);
                if (pg != null) {
                    mc.gameMode.interact(mc.player, pg, InteractionHand.MAIN_HAND);
                }
                // me alejo por el hielo: me sigue con prisa (de panza)
                t.cmd("gamemode survival @a");
                if (pg != null) {
                    t.cmd(String.format(Locale.ROOT, "tp @a %.1f -60 %.1f 0 20", pg.getX() + 1.5, pg.getZ() + 11.0));
                }
            });
            t.waitFor("el pingüino va de panza", 5000, () -> {
                var pg = pinguino(true);
                return pg != null && pg.deslizando();
            });
            t.photo("pinguino_de_panza", 100, () -> {
                var pg = pinguino(true);
                if (pg != null) {
                    t.lookFrom(pg.getX() + 3.0, pg.getY() + 0.9, pg.getZ() + 1.2, pg.getX(), pg.getY() + 0.2, pg.getZ());
                }
            });
            // al agua: lo pongo en el estanque y nada
            t.add("al agua", 600, () -> {
                t.cmd("tp @a 481.5 -60 126.5 180 25");
                var pg = pinguino(true);
                if (pg != null) {
                    t.cmd("tp " + pg.getStringUUID() + " 481.5 -61 119.5");
                }
            });
            t.waitFor("el pingüino está nadando", 6000, () -> {
                var pg = pinguino(true);
                return pg != null && pg.isInWater();
            });
            t.photo("pinguino_nadando", 300, () -> {
                var pg = pinguino(true);
                if (pg != null) {
                    t.lookFrom(pg.getX() + 2.6, pg.getY() + 1.6, pg.getZ() + 2.6, pg.getX(), pg.getY() + 0.2, pg.getZ());
                }
            });
            t.add("fin pinguino", 300, () -> {
                t.cmd("gamemode creative @a");
                t.cmd("kill @e[type=navidad:pinguino]");
            });
            // ---------------------------------------------------------------- el renito de nariz roja: con una zanahoria dorada es tuyo
            t.add("renito", 1200, () -> {
                t.cmd("time set 6000");
                t.cmd("kill @e[type=navidad:renito]");
                t.cmd("fill 455 -61 135 470 -61 150 minecraft:snow_block");
                t.cmd("summon navidad:renito 462 -60 142 {Rotation:[200f,0f]}");
                t.cmd("tp @a 462.5 -60 145.5 180 15");
                t.cmd("item replace entity @p weapon.mainhand with minecraft:golden_carrot 4");
            });
            t.add("darle zanahoria", 800, () -> {
                Minecraft mc = Minecraft.getInstance();
                for (var e : mc.level.entitiesForRendering()) {
                    if (e instanceof com.navidad.entidad.Renito r) {
                        t.lookFrom(r.getX() + 1.5, r.getY(), r.getZ() + 1.5, r.getX(), r.getY() + 0.4, r.getZ());
                        mc.gameMode.interact(mc.player, r, InteractionHand.MAIN_HAND);
                        break;
                    }
                }
            });
            t.waitFor("el renito es mío", 3000, () -> com.navidad.entidad.Renito.domados > 0);
            t.add("sentar al renito", 800, () -> {
                Minecraft mc = Minecraft.getInstance();
                t.cmd("item replace entity @p weapon.mainhand with minecraft:air");
                for (var e : mc.level.entitiesForRendering()) {
                    if (e instanceof com.navidad.entidad.Renito r) {
                        mc.gameMode.interact(mc.player, r, InteractionHand.MAIN_HAND);
                        break;
                    }
                }
            });
            t.photo("renito", 1500, () -> {
                for (var e : Minecraft.getInstance().level.entitiesForRendering()) {
                    if (e instanceof com.navidad.entidad.Renito r) {
                        double a = Math.toRadians(r.yBodyRot);
                        t.lookFrom(r.getX() - Math.sin(a) * 2.0 + 0.6, r.getY() + 0.7, r.getZ() + Math.cos(a) * 2.0 + 0.6, r.getX(), r.getY() + 0.45,
                                r.getZ());
                        break;
                    }
                }
            });
            t.photo("renito_noche", 1500, () -> t.cmd("time set 18000"));
            t.add("renito dia", 300, () -> {
                t.cmd("time set 6000");
                t.cmd("kill @e[type=navidad:renito]");
            });
            // ---------------------------------------------------------------- tu árbol: lo pongo, lo adorno y en la mañana hay regalos;
            // de noche llega el duende ladrón, se roba uno y lo atrapo
            t.add("arbol", 1500, () -> {
                t.cmd("time set 6000");
                t.cmd("fill 494 -60 134 512 -54 152 minecraft:air");
                t.cmd("fill 494 -61 134 512 -61 152 minecraft:snow_block");
                t.cmd("tp @a 503.5 -60 146.5 180 30");
                t.cmd("item replace entity @p weapon.mainhand with navidad:arbol_navidad");
            });
            t.add("poner el arbol", 800, () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(new Vec3(503.5, -61, 143.5), Direction.UP, new BlockPos(503, -61, 143), false));
            });
            t.add("adornarlo", 1200, () -> {
                t.cmd("item replace entity @p weapon.mainhand with minecraft:air");
                t.cmd("setblock 501 -60 141 navidad:pino_maceta[facing=south]");
                t.cmd("setblock 505 -60 141 navidad:muneco_nieve[facing=south]");
                t.cmd("setblock 500 -60 144 navidad:farolito[facing=east]");
                t.cmd("setblock 506 -60 144 navidad:farolito[facing=west]");
                t.cmd("setblock 503 -60 140 navidad:chimenea[facing=south]");
                t.cmd("setblock 502 -60 146 navidad:nochebuena");
                t.cmd("setblock 504 -60 146 navidad:nochebuena");
            });
            t.add("santa pasa por el arbol", 1500, () -> t.cmd("navidad arbol"));
            t.waitFor("Santa dejó regalos bajo mi árbol", 4000, () -> com.navidad.ArbolSanta.entregas > 0);
            t.photo("arbol_regalos", 1500, () -> t.lookFrom(506.5, -58.2, 148.5, 503.5, -59.4, 143.5));
            t.add("de noche", 1500, () -> {
                t.cmd("time set 15000");
                t.cmd("tp @a 503.5 -60 149.5 180 25");
            });
            t.add("llega el ladron", 1500, () -> t.cmd("execute as @p run navidad ladron"));
            t.waitFor("el duende ladrón se robó un regalo", 25000, () -> com.navidad.entidad.DuendeLadron.robos > 0);
            t.photo("ladron_con_regalo", 200, () -> {
                var d = ladron();
                if (d != null) {
                    t.lookFrom(d.getX() + 2.2, d.getY() + 1.0, d.getZ() + 2.2, d.getX(), d.getY() + 0.6, d.getZ());
                }
            });
            t.add("atraparlo", 600, () -> {
                Minecraft mc = Minecraft.getInstance();
                var d = ladron();
                if (d != null) {
                    t.lookFrom(d.getX() + 1.5, d.getY(), d.getZ() + 1.5, d.getX(), d.getY() + 0.5, d.getZ());
                    mc.gameMode.attack(mc.player, d);
                }
            });
            t.waitFor("atrapé al duende ladrón", 3000, () -> com.navidad.entidad.DuendeLadron.atrapados > 0);
            t.photo("ladron_atrapado", 800, () -> {
            });
            t.add("fin arbol", 500, () -> t.cmd("time set 6000"));
            // ---------------------------------------------------------------- el puesto de árboles de Navidad (sale solo en el mundo) y Don Abeto
            t.add("puesto", 2500, () -> {
                t.cmd("time set 6000");
                t.cmd("fill 530 -61 170 560 -61 200 minecraft:grass_block");
                t.cmd("fill 530 -60 170 560 -50 200 minecraft:air");
                t.cmd("place feature navidad:puesto_pinos 545 -60 185");
            });
            t.waitFor("llegó Don Abeto", 15000, () -> abeto() != null);
            t.add("buscar el puesto", 800, () -> {
                var n = abeto();
                if (n != null) {
                    // de frente al puesto: el letrero queda del lado de la entrada; me paro afuera viendo hacia Don Abeto
                    var l = Minecraft.getInstance().level;
                    BlockPos letrero = null;
                    for (BlockPos q : BlockPos.betweenClosed(530, -60, 170, 560, -60, 200)) {
                        if (l.getBlockState(q).is(com.navidad.registry.ModRegistry.LETRERO_PINOS.get())) {
                            letrero = q.immutable();
                        }
                    }
                    double dx = letrero == null ? 0 : letrero.getX() + 0.5 - n.getX(), dz = letrero == null ? 10 : letrero.getZ() + 0.5 - n.getZ();
                    double d = Math.sqrt(dx * dx + dz * dz);
                    t.lookFrom(n.getX() + dx / d * 13 + 2, n.getY() + 5.5, n.getZ() + dz / d * 13 + 2, n.getX(), n.getY() + 0.5, n.getZ());
                }
            });
            t.photo("puesto_pinos", 2500, () -> {
            });
            t.add("acercarme a Don Abeto", 800, () -> {
                var n = abeto();
                if (n != null) {
                    double a2 = Math.toRadians(n.yBodyRot);
                    t.lookFrom(n.getX() - Math.sin(a2) * 3.4, n.getY() + 0.4, n.getZ() + Math.cos(a2) * 3.4, n.getX(), n.getY() + 1.2, n.getZ());
                }
            });
            t.photo("don_abeto", 1500, () -> {
            });
            t.add("su tienda", 1500, () -> {
                Minecraft mc = Minecraft.getInstance();
                var n = abeto();
                if (n != null) {
                    mc.gameMode.interact(mc.player, n, InteractionHand.MAIN_HAND);
                }
            });
            t.photo("don_abeto_tienda", 1500, () -> {
            });
            t.add("cerrar tienda", 600, () -> Minecraft.getInstance().setScreen(null));
            t.photo("puesto_noche", 2500, () -> {
                t.cmd("time set 18000");
                var n = abeto();
                if (n != null) {
                    t.lookFrom(n.getX() + 7, n.getY() + 3.5, n.getZ() + 9, n.getX(), n.getY() + 0.5, n.getZ() + 1);
                }
            });
            t.add("fin puesto", 600, () -> t.cmd("time set 6000"));
            // ---------------------------------------------------------------- el duende viajero llega con su reno
            t.add("viajero", 1500, () -> {
                t.cmd("fill 580 -61 200 610 -61 230 minecraft:grass_block");
                t.cmd("fill 580 -60 200 610 -50 230 minecraft:air");
                t.cmd("tp @a 595.5 -60 215.5 0 10");
                t.cmd("execute as @p run navidad viajero");
            });
            t.waitFor("llegó el duende viajero", 5000, () -> com.navidad.Viajero.llegadas > 0 && viajero() != null);
            t.photo("duende_viajero", 2000, () -> {
                var n = viajero();
                if (n != null) {
                    var mc = Minecraft.getInstance();
                    double dx = mc.player.getX() - n.getX(), dz = mc.player.getZ() - n.getZ(), d = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
                    t.lookFrom(n.getX() + dx / d * 6.5 + 1.5, n.getY() + 1.4, n.getZ() + dz / d * 6.5 + 1.5, n.getX() + 0.6, n.getY() + 0.9, n.getZ());
                }
            });
            // ---------------------------------------------------------------- el buzón y la carta a Santa
            t.add("buzon", 1000, () -> {
                t.cmd("time set 1000");
                t.cmd("gamemode creative @a");
            });
            t.photo("buzon", 2500, () -> t.lookFrom(299.6, -59.2, -11.6, 302.5, -59.3, -13.6));
            t.add("carta", 1500, () -> {
                t.cmd("tp @a 300.5 -60 -13.5 -90 20");
                t.cmd("item replace entity @p weapon.mainhand with minecraft:writable_book{pages:[\"Querido Santa: este año me porté muy bien. "
                        + "Quiero un trineo, diamantes y galletas. ¡Gracias!\"]}");
            });
            t.add("meter la carta", 1500, () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(new Vec3(302.05, -59.4, -13.5), Direction.WEST, new BlockPos(302, -60, -14), false));
            });
            t.waitFor("la carta salió volando", 4000, () -> Minecraft.getInstance().player.getMainHandItem().isEmpty());
            t.add("respuesta de Santa", 1500, () -> t.cmd("execute as @p run navidad carta entregar"));
            t.waitFor("llegó la respuesta", 5000, () -> Minecraft.getInstance().player.getInventory().items.stream()
                    .anyMatch(st -> st.is(Items.WRITTEN_BOOK)));
            t.add("hud carta", 300, () -> t.hud(true));
            t.photo("carta_respuesta", 1500, () -> t.lookFrom(300.5, -60, -13.5, 302.5, -59.5, -13.5));
            t.add("hud carta fin", 300, () -> t.hud(false));
            // ---------------------------------------------------------------- el calcetín que Santa llena cada noche
            t.add("calcetin", 1200, () -> {
                t.cmd("setblock 260 -59 0 minecraft:spruce_planks");
                t.cmd("setblock 260 -59 1 navidad:calcetin_rojo[facing=south]");
                t.cmd("tp @a 260.5 -60 3.5 180 15");
            });
            t.add("sacar del calcetin", 1200, () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(new Vec3(260.5, -58.6, 1.2), Direction.SOUTH, new BlockPos(260, -59, 1), false));
            });
            t.waitFor("Santa llenó el calcetín", 4000, () -> !com.navidad.Calcetines.ultimo.isEmpty());
            t.add("hud calcetin", 300, () -> t.hud(true));
            t.photo("calcetin", 1200, () -> t.lookFrom(260.5, -58.4, 3.5, 260.5, -58.6, 1));
            t.add("hud calcetin fin", 300, () -> t.hud(false));
            // ---------------------------------------------------------------- galletas y leche para Santa
            t.add("galletas", 800, () -> t.cmd("item replace entity @p weapon.mainhand with navidad:galletas_leche"));
            t.add("poner galletas", 1000, () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(new Vec3(262.5, -61, 2.5), Direction.UP, new BlockPos(262, -61, 2), false));
            });
            t.photo("galletas_puestas", 1200, () -> t.lookFrom(264.5, -58.8, 5.5, 262.5, -60, 2.5));
            t.add("santa pasa", 1500, () -> t.cmd("navidad galletas"));
            t.waitFor("Santa se comió las galletas", 4000, () -> Minecraft.getInstance().level.getBlockState(new BlockPos(262, -60, 2)).getBlock()
                    == com.navidad.bloque.deco.Deco.block("plato_migajas"));
            t.photo("galletas_comidas", 1500, () -> t.lookFrom(264.5, -58.8, 5.5, 262.5, -60, 2.5));
            // ---------------------------------------------------------------- un ángel de nieve: agachado mirando al cielo 3 segundos
            t.add("angel", 1500, () -> {
                t.cmd("fill 264 -61 6 266 -61 8 minecraft:snow_block");
                t.cmd("gamemode survival @a");
                t.cmd("tp @a 265.5 -60 7.5 0 -80");
            });
            t.add("acostarme", 4500, () -> Minecraft.getInstance().options.keyShift.setDown(true));
            t.add("levantarme del angel", 500, () -> {
                Minecraft.getInstance().options.keyShift.setDown(false);
                t.check(com.navidad.Invierno.angeles > 0, "hice un ángel de nieve");
                t.cmd("gamemode creative @a");
            });
            t.photo("angel_nieve", 1500, () -> t.lookFrom(265.5, -57.2, 9.6, 265.5, -60.6, 7.4));
            // ---------------------------------------------------------------- adornos que responden: la esfera de nieve se sacude
            t.add("esfera", 1000, () -> {
                t.cmd("setblock 268 -60 4 navidad:esfera_nieve[facing=south]");
                t.cmd("tp @a 268.5 -60 6.5 180 35");
            });
            t.add("sacudir esfera", 300, () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(new Vec3(268.5, -59.7, 4.8), Direction.SOUTH, new BlockPos(268, -60, 4), false));
            });
            t.photo("esfera_sacudida", 400, () -> t.lookFrom(268.5, -59, 5.9, 268.5, -59.7, 4.5));
            t.add("la esfera respondio", 300, () -> t.check(com.navidad.DecoViva.usos > 0, "la esfera de nieve se sacudió"));
            // ---------------------------------------------------------------- la colina de trineos
            t.add("colina", 1000, () -> {
                t.cmd("time set 6000");
                t.cmd("gamemode spectator @a");
            });
            t.photo("colina", 2500, () -> t.lookFrom(309, -50, -9, 318, -55, -21));
            t.add("trineo solo", 300, () -> t.cmd("summon navidad:trineo_madera 318.5 -53.4 -26.5 {Motion:[0.0,0.0,0.35]}"));
            t.photo("colina_bajando", 1000, () -> {
            });
            t.photo("colina_bajando_2", 1000, () -> {
            });
            t.add("subir al trineo", 1500, () -> {
                t.cmd("gamemode creative @a");
                t.cmd("kill @e[type=navidad:trineo_madera]");
                t.cmd("summon navidad:trineo_madera 316.5 -53.4 -26.5 {Rotation:[0f,0f]}");
                t.cmd("tp @a 316.5 -53 -26.5 0 15");
            });
            t.add("montar", 1200, () -> com.navidad.Invierno.diagnostico = true);
            t.add("montar 2", 1200, () -> t.cmd("execute as @p at @s run ride @s mount @e[type=navidad:trineo_madera,limit=1,sort=nearest]"));
            t.add("empujar", 1500, () -> {
                t.check(Minecraft.getInstance().player.getVehicle() instanceof com.navidad.entidad.TrineoMadera, "arriba del trineo");
                Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
                Minecraft.getInstance().options.keyUp.setDown(true);
            });
            t.photo("trineo_montado", 300, () -> {
            });
            t.add("bajando", 3500, () -> {
            });
            t.add("soltar", 300, () -> {
                Minecraft.getInstance().options.keyUp.setDown(false);
                double z = Minecraft.getInstance().player.getZ();
                t.log("trineo: z=" + z);
                t.check(z > -20, "el trineo bajó la colina (z=" + String.format(Locale.ROOT, "%.1f", z) + ")");
            });
            t.photo("trineo_abajo", 1500, () -> {
            });
            t.add("carrera", 300, () -> t.check(com.navidad.Invierno.ultimaCarrera > 0, "carrera cronometrada (" + String.format(Locale.ROOT,
                    "%.2f", com.navidad.Invierno.ultimaCarrera) + " s)"));
            t.add("bajarse", 800, () -> {
                t.cmd("execute as @p run ride @s dismount");
                Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            });
            // ---------------------------------------------------------------- la pista de hielo con patines
            t.add("pista", 1500, () -> {
                t.cmd("tp @a 317.5 -60 19.5 -90 10");
                t.cmd("execute as @p run navidad ropa patines");
                Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            });
            t.photo("patines_pista", 2000, () -> t.cmd("tp @a 317.5 -60 19.5 -90 5"));
            t.add("camara normal 2", 500, () -> {
                Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
                t.cmd("execute as @p run navidad ropa patines");
                t.cmd("gamemode spectator @a");
            });
            t.photo("pista", 2500, () -> t.lookFrom(321, -46, 33, 321, -60, 18));
            // ---------------------------------------------------------------- el pino adornado de los biomas nevados
            t.add("pinos", 3000, () -> {
                t.cmd("fill 368 -61 -8 394 -61 12 minecraft:snow_block");
                t.cmd("place feature navidad:pino_adornado 380 -60 0");
                t.cmd("place feature navidad:pino_adornado 389 -60 5");
                t.cmd("place feature navidad:pino_adornado 372 -60 6");
            });
            t.photo("pino_adornado", 3000, () -> t.lookFrom(380.5, -57, 17, 380.5, -54, 0));
            t.add("casita", 2500, () -> {
                t.cmd("fill 395 -61 -24 410 -61 -8 minecraft:snow_block");
                t.cmd("place feature navidad:casita_duende 402 -60 -16");
            });
            t.photo("casita_duende", 2500, () -> t.lookFrom(410, -57, -5, 402, -58, -16));
            t.photo("casita_duende_dentro", 2500, () -> t.lookFrom(402.5, -59.5, -15.5, 402.5, -59.6, -19));
            t.photo("pino_adornado_noche", 3500, () -> t.cmd("time set 18000"));
            // ---------------------------------------------------------------- Año Nuevo: cuenta, 12 campanadas con uvas y fuegos
            t.add("anio nuevo", 1200, () -> {
                t.cmd("gamemode creative @a");
                t.cmd("tp @a 300.5 -60 8.5 180 -20");
                t.cmd("item replace entity @p weapon.mainhand with navidad:uvas 12");
                t.hud(true);
                t.cmd("navidad anio_nuevo 3");
            });
            t.photo("anio_cuenta", 0, () -> {
            });
            t.add("comer uvas", 2500, () -> Minecraft.getInstance().options.keyUse.setDown(true));
            t.photo("anio_campanada", 0, () -> {
            });
            t.add("seguir comiendo", 16000, () -> {
            });
            t.add("ya", 1600, () -> Minecraft.getInstance().options.keyUse.setDown(false));
            t.photo("anio_feliz", 0, () -> {
            });
            t.add("uvas?", 300, () -> {
                int n = com.navidad.AnioNuevo.ultimo.getOrDefault(Minecraft.getInstance().player.getUUID(), 0);
                t.check(n >= 12, "doce uvas en las campanadas (" + n + ")");
            });
            t.photo("anio_fuegos", 3000, () -> t.lookFrom(300.5, -59, 8.5, 300.5, -38, -12));
            t.photo("anio_fuegos_2", 2500, () -> {
            });
            t.add("hud otra vez", 300, () -> t.hud(false));
            t.add("dia al final", 1000, () -> t.cmd("time set 6000"));
            t.add("hud fin", 200, () -> t.hud(true));
        });
    }
}
