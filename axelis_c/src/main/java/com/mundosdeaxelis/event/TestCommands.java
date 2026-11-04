package com.mundosdeaxelis.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.crate.CratePools;
import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.gear.ArmorDefs;
import com.mundosdeaxelis.gear.GearDef;
import com.mundosdeaxelis.gear.GearDefs;
import com.mundosdeaxelis.gear.GearSet;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.table.Essence;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comandos de prueba para no tener que jugar horas: {@code /corazon prueba ...} (hace falta permiso de operador).
 * <pre>
 *   /corazon prueba capas                    -> te da las 8 capas
 *   /corazon prueba equipo &lt;set|todos&gt;      -> te da la armadura (4 piezas) y todas las herramientas de ese set
 *   /corazon prueba llaves [cantidad]       -> te da esa cantidad de llaves de cada caja (por defecto 5)
 *   /corazon prueba cajas                   -> te da las ocho cajas de colección
 *   /corazon prueba esencia &lt;cantidad&gt;      -> te da Esencia
 *   /corazon prueba niveles                 -> muestra qué niveles de calidad da cada caja
 *   /corazon prueba simular &lt;caja&gt; [n]      -> simula n aperturas (1000 por defecto) y cuenta qué sale
 * </pre>
 */
public final class TestCommands {
    private TestCommands() {
    }

    private static List<String> setIds() {
        List<String> l = new ArrayList<>();
        for (GearSet s : GearSet.values()) {
            l.add(s.id);
        }
        l.add("todos");
        return l;
    }

    private static List<String> crateIds() {
        List<String> l = new ArrayList<>();
        for (CrateKind k : CrateKind.values()) {
            if (k != CrateKind.PETS) {
                l.add(k.id);
            }
        }
        return l;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("prueba")
                .then(Commands.literal("equipo")
                        .then(Commands.argument("set", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(setIds(), b))
                                .executes(ctx -> equipo(ctx.getSource(), StringArgumentType.getString(ctx, "set")))))
                .then(Commands.literal("capas").executes(ctx -> capas(ctx.getSource())))
                .then(Commands.literal("llaves")
                        .executes(ctx -> llaves(ctx.getSource(), 5))
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> llaves(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "cantidad")))))
                .then(Commands.literal("cajas").executes(ctx -> cajas(ctx.getSource())))
                .then(Commands.literal("esencia")
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 100000))
                                .executes(ctx -> esencia(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "cantidad")))))
                .then(Commands.literal("mesa")
                        .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 8))
                                .executes(ctx -> mesa(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "nivel")))))
                .then(Commands.literal("materiales_jefe").executes(ctx -> materialesJefe(ctx.getSource())))
                .then(Commands.literal("niveles").executes(ctx -> niveles(ctx.getSource())))
                .then(Commands.literal("ficha")
                        .then(Commands.argument("etapa", IntegerArgumentType.integer(1, 2))
                                .then(Commands.argument("modo", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("entrada_oculto", "entrada_visible", "antesala"), b))
                                        .executes(ctx -> ficha(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "etapa"),
                                                StringArgumentType.getString(ctx, "modo"))))))
                .then(Commands.literal("resultado")
                        .then(Commands.argument("etapa", IntegerArgumentType.integer(1, 2))
                                .executes(ctx -> resultado(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "etapa"), true))
                                .then(Commands.literal("poco")
                                        .executes(ctx -> resultado(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "etapa"), false)))))
                .then(Commands.literal("congelar")
                        .executes(ctx -> congelar(ctx.getSource(), com.mundosdeaxelis.world.Frozen.DEFAULT_HITS))
                        .then(Commands.argument("saltos", IntegerArgumentType.integer(1, 60))
                                .executes(ctx -> congelar(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "saltos")))))
                .then(Commands.literal("frio")
                        .then(Commands.argument("valor", IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> {
                                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                    com.mundosdeaxelis.world.Cold.set(sp, IntegerArgumentType.getInteger(ctx, "valor"));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Medidor de frío puesto."), false);
                                    return 1;
                                })))
                .then(Commands.literal("zona_fria")
                        .then(Commands.argument("ritmo", IntegerArgumentType.integer(1, 60))
                                .then(Commands.argument("segundos", IntegerArgumentType.integer(1, 600))
                                        .executes(ctx -> {
                                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                                            int secs = IntegerArgumentType.getInteger(ctx, "segundos");
                                            com.mundosdeaxelis.world.Cold.zone(sp, IntegerArgumentType.getInteger(ctx, "ritmo"), secs * 20);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Estás en una zona helada durante " + secs
                                                    + " s (ponte junto a una fogata para calentarte)."), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("aviso")
                        .executes(ctx -> {
                            ServerPlayer sp = ctx.getSource().getPlayerOrException();
                            com.mundosdeaxelis.world.Titles.hint(sp, "Puente de losas", "En cada fila hay una losa falsa: elige bien");
                            com.mundosdeaxelis.world.Titles.hint(sp, "Sala despejada", "Sala del Capataz");
                            com.mundosdeaxelis.world.Titles.hint(sp, "Prueba de runas", "Mira la secuencia y repítela");
                            com.mundosdeaxelis.world.Titles.actionBar(sp, "Encendiendo la linterna... 40%  (no se separen)");
                            com.mundosdeaxelis.world.Titles.lootEssence(sp, 15);
                            com.mundosdeaxelis.world.Titles.loot(sp, ModItems.SANCTUARY_BERRY.get(), 1);
                            com.mundosdeaxelis.world.Titles.loot(sp, ModItems.EMBER_STEEL.get(), 2);
                            com.mundosdeaxelis.world.Titles.actionBarLong(sp, "Caíste. Vuelves al inicio del tramo: inténtalo otra vez.", 4);
                            ctx.getSource().sendSuccess(() -> Component.literal("Avisos de prueba enviados (arriba a la derecha)."), false);
                            return 1;
                        }))
                .then(Commands.literal("llave_bonus")
                        .executes(ctx -> llaveBonus(ctx.getSource(), CrateKind.FORGE))
                        .then(Commands.argument("caja", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(crateIds(), b))
                                .executes(ctx -> llaveBonus(ctx.getSource(), CrateKind.byId(StringArgumentType.getString(ctx, "caja"))))))
                .then(Commands.literal("sello")
                        .then(Commands.argument("caja", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(crateIds(), b))
                                .executes(ctx -> sello(ctx.getSource(), StringArgumentType.getString(ctx, "caja"), 1))
                                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> sello(ctx.getSource(), StringArgumentType.getString(ctx, "caja"),
                                                IntegerArgumentType.getInteger(ctx, "cantidad"))))))
                .then(Commands.literal("progreso")
                        .then(Commands.argument("etapa", IntegerArgumentType.integer(1, 8))
                                .executes(ctx -> progreso(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "etapa")))))
                .then(Commands.literal("jefe").executes(ctx -> jefe(ctx.getSource())))
                .then(Commands.literal("arena").executes(ctx -> arena(ctx.getSource())))
                .then(Commands.literal("saltardungeon").executes(ctx -> saltarDungeon(ctx.getSource())))
                .then(Commands.literal("jefe2").executes(ctx -> jefe2(ctx.getSource())))
                .then(Commands.literal("intro").executes(ctx -> intro(ctx.getSource(), com.mundosdeaxelis.world.BossIntro.Kind.FOREST)))
                .then(Commands.literal("intro3").executes(ctx -> introHielo(ctx.getSource())))
                .then(Commands.literal("intro2").executes(ctx -> intro(ctx.getSource(), com.mundosdeaxelis.world.BossIntro.Kind.FORGE)))
                .then(Commands.literal("arena2").executes(ctx -> arena2(ctx.getSource())))
                .then(Commands.literal("jefe3").executes(ctx -> jefe3(ctx.getSource())))
                .then(Commands.literal("entrar3").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    com.mundosdeaxelis.world.BossTravel.enter(ctx.getSource().getServer(), java.util.List.of(p), 3);
                    ctx.getSource().sendSuccess(() -> Component.literal("Entrando a la sala del Lobo Ártico (dimensión de la etapa 3)."), false);
                    return 1;
                }))
                .then(Commands.literal("arena3").executes(ctx -> arena3(ctx.getSource())))
                .then(Commands.literal("limpiar3").executes(ctx -> limpiar3(ctx.getSource())))
                .then(Commands.literal("saltardungeon2").executes(ctx -> saltarDungeon2(ctx.getSource())))
                .then(Commands.literal("plano")
                        .executes(ctx -> plano(ctx.getSource(), new java.util.Random().nextLong(), 1))
                        .then(Commands.argument("jugadores", IntegerArgumentType.integer(1, 6))
                                .executes(ctx -> plano(ctx.getSource(), new java.util.Random().nextLong(), IntegerArgumentType.getInteger(ctx, "jugadores")))
                                .then(Commands.argument("semilla", com.mojang.brigadier.arguments.LongArgumentType.longArg())
                                        .executes(ctx -> plano(ctx.getSource(), com.mojang.brigadier.arguments.LongArgumentType.getLong(ctx, "semilla"),
                                                IntegerArgumentType.getInteger(ctx, "jugadores"))))))
                .then(Commands.literal("salir").executes(ctx -> salir(ctx.getSource())))
                .then(Commands.literal("balance").executes(ctx -> balance(ctx.getSource())))
                .then(Commands.literal("dificultad")
                        .executes(ctx -> dificultad(ctx.getSource(), -1))
                        .then(Commands.argument("nivel", IntegerArgumentType.integer(0, 8))
                                .executes(ctx -> dificultad(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "nivel")))))
                .then(Commands.literal("simular")
                        .then(Commands.argument("caja", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(crateIds(), b))
                                .executes(ctx -> simular(ctx.getSource(), StringArgumentType.getString(ctx, "caja"), 1000))
                                .then(Commands.argument("n", IntegerArgumentType.integer(10, 100000))
                                        .executes(ctx -> simular(ctx.getSource(), StringArgumentType.getString(ctx, "caja"),
                                                IntegerArgumentType.getInteger(ctx, "n"))))));
    }

    /** Hace aparecer al Rey Brote donde estás (para probar el combate sin ir a la arena). */
    private static int jefe(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.entity.ReyBrote b = com.mundosdeaxelis.registry.ModEntities.REY_BROTE.get().create(sp.serverLevel());
        if (b == null) {
            return 0;
        }
        b.moveTo(sp.getX() + 6, sp.getY(), sp.getZ(), 0.0F, 0.0F);
        sp.serverLevel().addFreshEntity(b);
        src.sendSuccess(() -> Component.literal("Rey Brote aparecido (vida " + (int) b.getMaxHealth() + ")."), false);
        return 1;
    }

    /**
     * Prueba la intro épica del jefe (hay que estar en su arena, cerca del centro; quita al jefe que haya para que salga de nuevo).
     */
    private static int intro(CommandSourceStack src, com.mundosdeaxelis.world.BossIntro.Kind kind) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = sp.serverLevel();
        net.minecraft.world.phys.AABB all = new net.minecraft.world.phys.AABB(-60, 0, -60, 60, 200, 60);
        for (com.mundosdeaxelis.entity.ReyBrote b : level.getEntitiesOfClass(com.mundosdeaxelis.entity.ReyBrote.class, all)) {
            b.discard();
        }
        for (com.mundosdeaxelis.entity.GranHerrero b : level.getEntitiesOfClass(com.mundosdeaxelis.entity.GranHerrero.class, all)) {
            b.discard();
        }
        com.mundosdeaxelis.world.BossIntro.play(level, new java.util.ArrayList<>(level.players()), kind);
        src.sendSuccess(() -> Component.literal("Intro del jefe en marcha (usa la tecla de saltar cinemática para cortarla)."), false);
        return 1;
    }

    /** Prueba la intro del Lobo Ártico (hay que estar en su arena de hielo; quita al lobo que haya). */
    private static int introHielo(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = sp.serverLevel();
        com.mundosdeaxelis.entity.LoboArtico old = com.mundosdeaxelis.world.IceIntro.find(level);
        if (old != null) {
            old.discard();
        }
        java.util.List<ServerPlayer> ps = new java.util.ArrayList<>(level.players());
        if (!com.mundosdeaxelis.world.IceIntro.canPlay(ps)) {
            src.sendFailure(Component.literal("Ya hay una cinemática en marcha."));
            return 0;
        }
        com.mundosdeaxelis.world.IceIntro.play(level, ps, () -> {
            com.mundosdeaxelis.entity.LoboArtico b = com.mundosdeaxelis.world.IceIntro.find(level);
            if (b != null) {
                b.setNoAi(false);
                b.setInvulnerable(false);
            }
        });
        src.sendSuccess(() -> Component.literal("Intro del Lobo Ártico en marcha (tecla de saltar para cortarla)."), false);
        return 1;
    }

    /** Hace aparecer al Gran Herrero de Brasas (etapa 2) donde estás. */
    private static int jefe2(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.entity.GranHerrero b = com.mundosdeaxelis.registry.ModEntities.GRAN_HERRERO.get().create(sp.serverLevel());
        if (b == null) {
            return 0;
        }
        b.moveTo(sp.getX() + 8, sp.getY(), sp.getZ(), 0.0F, 0.0F);
        sp.serverLevel().addFreshEntity(b);
        src.sendSuccess(() -> Component.literal("Gran Herrero aparecido (vida " + (int) b.getMaxHealth() + ")."), false);
        return 1;
    }

    // ------------------------------------------------------------------ Lobo Ártico (etapa 3)

    private static net.minecraft.world.phys.Vec3 arena3Center;

    /** Hace aparecer al Lobo Ártico (etapa 3) frente a ti. Mejor dentro de la arena de prueba (/prueba arena3). */
    private static int jefe3(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.entity.LoboArtico b = com.mundosdeaxelis.registry.ModEntities.LOBO_ARTICO.get().create(sp.serverLevel());
        if (b == null) {
            return 0;
        }
        net.minecraft.world.phys.Vec3 home = arena3Center != null && sp.serverLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                && Math.hypot(sp.getX() - arena3Center.x, sp.getZ() - arena3Center.z) < 26 ? arena3Center : sp.position();
        b.moveTo(sp.getX() + 10, sp.getY(), sp.getZ(), 90.0F, 0.0F);
        b.setHome(home);
        sp.serverLevel().addFreshEntity(b);
        src.sendSuccess(() -> Component.literal("Lobo Ártico aparecido (vida " + (int) b.getMaxHealth() + "). Fases al 65 % y 30 %."), false);
        return 1;
    }

    /**
     * Arena de prueba para el Lobo Ártico: un disco de nieve de 24 bloques de radio flotando en el cielo (sobre donde estás), con un
     * muro de hielo alrededor y sin braseros (la ventisca crea sus propias zonas de calor). Te lleva al centro.
     * Se quita con /prueba limpiar3.
     */
    private static int arena3(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = sp.serverLevel();
        if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            src.sendFailure(Component.literal("La arena de prueba se construye en el Overworld."));
            return 0;
        }
        int cx = net.minecraft.util.Mth.floor(sp.getX());
        int cz = net.minecraft.util.Mth.floor(sp.getZ());
        int fy = 236;
        int radius = 24;
        net.minecraft.world.level.block.state.BlockState snow = net.minecraft.world.level.block.Blocks.SNOW_BLOCK.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState packed = net.minecraft.world.level.block.Blocks.PACKED_ICE.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState lantern = net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState();
        for (int dx = -radius - 2; dx <= radius + 2; dx++) {
            for (int dz = -radius - 2; dz <= radius + 2; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > radius + 1.5) {
                    continue;
                }
                net.minecraft.core.BlockPos fp = new net.minecraft.core.BlockPos(cx + dx, fy, cz + dz);
                boolean ring = d > radius - 1.5 || ((int) d % 6 == 5);
                level.setBlock(fp, ring ? packed : snow, 2);
                level.setBlock(fp.below(), packed, 2);
                level.setBlock(fp.below(2), packed, 2);
                if (d > radius - 0.5) {
                    for (int h = 1; h <= 12; h++) {
                        level.setBlock(fp.above(h), h % 4 == 0 && ((dx + dz) & 3) == 0 ? lantern : packed, 2);
                    }
                }
            }
        }
        arena3Center = new net.minecraft.world.phys.Vec3(cx + 0.5, fy + 1.0, cz + 0.5);
        sp.teleportTo(level, cx + 0.5, fy + 1.0, cz - 12.5, 0.0F, 0.0F);
        src.sendSuccess(() -> Component.literal("Arena de hielo lista (disco de 24 bloques en el cielo, sin braseros: la ventisca crea zonas de calor). Ahora usa /prueba jefe3. Para quitarla: /prueba limpiar3."), false);
        return 1;
    }

    /** Quita la arena de prueba del Lobo Ártico y a sus criaturas. */
    private static int limpiar3(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = sp.serverLevel();
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(sp.getX() - 120, level.getMinBuildHeight(), sp.getZ() - 120,
                sp.getX() + 120, level.getMaxBuildHeight(), sp.getZ() + 120);
        for (com.mundosdeaxelis.entity.LoboArtico b : level.getEntitiesOfClass(com.mundosdeaxelis.entity.LoboArtico.class, box)) {
            b.clearFight(level);
            b.discard();
        }
        for (com.mundosdeaxelis.entity.LoboEscarcha b : level.getEntitiesOfClass(com.mundosdeaxelis.entity.LoboEscarcha.class, box)) {
            b.discard();
        }
        if (arena3Center != null && level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            int cx = net.minecraft.util.Mth.floor(arena3Center.x);
            int cz = net.minecraft.util.Mth.floor(arena3Center.z);
            int fy = net.minecraft.util.Mth.floor(arena3Center.y) - 1;
            net.minecraft.world.level.block.state.BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            for (int dx = -27; dx <= 27; dx++) {
                for (int dz = -27; dz <= 27; dz++) {
                    for (int dy = -3; dy <= 14; dy++) {
                        level.setBlock(new net.minecraft.core.BlockPos(cx + dx, fy + dy, cz + dz), air, 2);
                    }
                }
            }
            arena3Center = null;
        }
        src.sendSuccess(() -> Component.literal("Arena del Lobo Ártico quitada."), false);
        return 1;
    }

    private static int arena2(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.world.BossTravel.enter(src.getServer(), java.util.List.of(sp), 2);
        return 1;
    }

    /** Entra a la Forja y salta las salas (puertas abiertas hasta el jefe). */
    private static int saltarDungeon2(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.world.BossTravel.enter(src.getServer(), java.util.List.of(sp), 2);
        waitAndSkip2(src.getServer(), 0);
        return 1;
    }

    /** La copia de la dungeon donde hay jugadores ahora (o la primera, si no hay nadie). */
    private static net.minecraft.server.level.ServerLevel copyInUse(net.minecraft.server.MinecraftServer server,
                                                                   java.util.List<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> keys) {
        for (var k : keys) {
            net.minecraft.server.level.ServerLevel l = server.getLevel(k);
            if (l != null && !l.players().isEmpty()) {
                return l;
            }
        }
        return server.getLevel(keys.get(0));
    }

    private static void waitAndSkip2(net.minecraft.server.MinecraftServer server, int tries) {
        com.mundosdeaxelis.world.Scheduler.runLater(40, () -> {
            net.minecraft.server.level.ServerLevel l = copyInUse(server, com.mundosdeaxelis.world.ModDimensions.FORGE_COPIES);
            if (l == null) {
                return;
            }
            if (!com.mundosdeaxelis.world.ForgeManager.of(l).hasLayout() || com.mundosdeaxelis.world.ForgeDungeon.isBuilding(l) || l.players().isEmpty()) {
                if (tries < 60) {
                    waitAndSkip2(server, tries + 1);
                }
                return;
            }
            com.mundosdeaxelis.world.ForgeManager.of(l).skipAll(l);
            int k = 0;
            for (ServerPlayer q : l.players()) {
                net.minecraft.world.phys.Vec3 v = com.mundosdeaxelis.world.ForgeManager.of(l).entryPoint(l, k++);
                q.teleportTo(l, v.x, v.y, v.z, 180.0F, 0.0F);
            }
        });
    }

    private static int arena(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.world.BossTravel.enter(src.getServer(), java.util.List.of(sp), 1);
        return 1;
    }

    /** Entra y salta las salas: puertas abiertas hasta el jefe (para probar solo la pelea). Espera a que se construya la dungeon. */
    private static int saltarDungeon(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        com.mundosdeaxelis.world.BossTravel.enter(src.getServer(), java.util.List.of(sp), 1);
        waitAndSkip(src.getServer(), 0);
        return 1;
    }

    private static void waitAndSkip(net.minecraft.server.MinecraftServer server, int tries) {
        com.mundosdeaxelis.world.Scheduler.runLater(40, () -> {
            net.minecraft.server.level.ServerLevel l = copyInUse(server, com.mundosdeaxelis.world.ModDimensions.FOREST_COPIES);
            if (l == null) {
                return;
            }
            if (!com.mundosdeaxelis.world.DungeonManager.of(l).hasLayout() || com.mundosdeaxelis.world.ForestDungeon.isBuilding(l) || l.players().isEmpty()) {
                if (tries < 60) {
                    waitAndSkip(server, tries + 1);
                }
                return;
            }
            com.mundosdeaxelis.world.DungeonManager.of(l).skipAll(l);
            int k = 0;
            for (ServerPlayer q : l.players()) {
                net.minecraft.world.phys.Vec3 v = com.mundosdeaxelis.world.DungeonManager.of(l).entryPoint(l, k++);
                q.teleportTo(l, v.x, v.y, v.z, 180.0F, 0.0F);
            }
        });
    }

    /** Muestra en el chat el plano de una dungeon (sin construirla): /… plano [jugadores] [semilla]. */
    private static int plano(CommandSourceStack src, long seed, int players) {
        com.mundosdeaxelis.world.DungeonLayout lay = com.mundosdeaxelis.world.DungeonLayout.generate(seed, players);
        src.sendSuccess(() -> Component.literal(lay.summary()), false);
        src.sendSuccess(() -> Component.literal(lay.ascii()), false);
        src.sendSuccess(() -> Component.literal("E=entrada M=mini-jefe A=antesala L=llave C=cripta P=saltos R=runas 1-5=salas de combate"), false);
        return 1;
    }

    private static int salir(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        com.mundosdeaxelis.world.BossTravel.leave(src.getPlayerOrException());
        return 1;
    }

    private static int balance(CommandSourceStack src) {
        for (int i = 1; i <= 8; i++) {
            final int st = i;
            src.sendSuccess(() -> Component.literal(com.mundosdeaxelis.world.BossBalance.describe(st)), false);
        }
        return 1;
    }

    private static int sello(CommandSourceStack src, String caja, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        for (CrateKind k : CrateKind.values()) {
            if (k != CrateKind.PETS && k.id.equals(caja)) {
                give(sp, ModItems.SEALS.get(k).get(), n);
                src.sendSuccess(() -> Component.literal("Sello de " + k.id + " x" + n + " entregado."), false);
                return 1;
            }
        }
        src.sendFailure(Component.literal("Caja desconocida: " + caja));
        return 0;
    }

    private static int progreso(CommandSourceStack src, int etapa) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        boolean nuevo = com.mundosdeaxelis.table.Essence.registerProgress(sp, etapa);
        src.sendSuccess(() -> Component.literal(nuevo ? "Progreso registrado hasta la etapa " + etapa + "." : "Ese progreso ya estaba registrado."), false);
        return 1;
    }

    private static int dificultad(CommandSourceStack src, int nivel) {
        com.mundosdeaxelis.world.WorldTier t = com.mundosdeaxelis.world.WorldTier.get(src.getServer());
        if (nivel >= 0) {
            t.set(nivel);
        }
        int lv = t.level();
        src.sendSuccess(() -> Component.literal("Dificultad del mundo: nivel " + lv + " (monstruos +" + Math.round(com.mundosdeaxelis.world.WorldTierEvents.HP_PER_LEVEL * lv * 100)
                + "% de vida, +" + Math.round(com.mundosdeaxelis.world.WorldTierEvents.DMG_PER_LEVEL * lv * 100) + "% de daño). Los monstruos que ya existen no cambian; los nuevos sí."), false);
        return 1;
    }

    private static void give(ServerPlayer sp, Item it, int n) {
        if (it == null || it == Items.AIR) {
            return;
        }
        ItemStack st = new ItemStack(it, n);
        if (!sp.getInventory().add(st)) {
            sp.drop(st, false);
        }
    }

    private static Item byName(String id) {
        ResourceLocation rl = new ResourceLocation(MundosDeAxelis.MODID, id);
        return ForgeRegistries.ITEMS.containsKey(rl) ? ForgeRegistries.ITEMS.getValue(rl) : null;
    }

    private static int equipo(CommandSourceStack src, String which) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        int n = 0;
        for (GearSet s : GearSet.values()) {
            if (!which.equals("todos") && !s.id.equals(which)) {
                continue;
            }
            for (ArmorDefs.Piece p : ArmorDefs.Piece.values()) {
                give(sp, byName(ArmorDefs.id(s, p)), 1);
                n++;
            }
            for (GearDef d : GearDefs.of(s)) {
                give(sp, byName(d.id()), 1);
                n++;
            }
        }
        if (n == 0) {
            src.sendFailure(Component.literal("Set desconocido. Usa: " + String.join(", ", setIds())));
            return 0;
        }
        final int total = n;
        src.sendSuccess(() -> Component.literal("Te di " + total + " piezas de equipo (" + which + ")."), false);
        return n;
    }

    private static int capas(CommandSourceStack src) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        int n = 0;
        for (com.mundosdeaxelis.cape.CapeDefs.Def d : com.mundosdeaxelis.cape.CapeDefs.ALL) {
            give(sp, byName(d.id()), 1);
            n++;
        }
        final int total = n;
        src.sendSuccess(() -> Component.literal("Te di las " + total + " capas. Póntelas en la ranura Capa del menú de equipo (G) o con clic derecho."), false);
        return n;
    }

    private static int llaves(CommandSourceStack src, int n) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        for (CrateKind k : CrateKind.values()) {
            if (k != CrateKind.PETS) {
                give(sp, ModItems.KEYS.get(k).get(), n);
            }
        }
        src.sendSuccess(() -> Component.literal("Te di " + n + " llaves de cada caja."), false);
        return 1;
    }

    private static int cajas(CommandSourceStack src) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        for (CrateKind k : CrateKind.values()) {
            if (k != CrateKind.PETS) {
                give(sp, ModItems.CRATE_ITEMS.get(k).get(), 1);
            }
        }
        src.sendSuccess(() -> Component.literal("Te di las ocho cajas de colección."), false);
        return 1;
    }

    /** (Prueba) Pone tu nivel de Mesa del Artífice (1 a 8). */
    private static int mesa(CommandSourceStack src, int lv) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        Essence.setLevel(sp, lv);
        com.mundosdeaxelis.relic.Equipment.sync(sp);
        Essence.sync(sp, Component.literal("Nivel de mesa: " + lv), true);
        src.sendSuccess(() -> Component.literal("Tu Mesa del Artífice ahora es de nivel " + lv + "."), false);
        return 1;
    }

    /** (Prueba) Abre la ficha del jefe sin jugar la dungeon: entrada con el jefe oculto o visible, o la ficha de la antesala. */
    private static int ficha(CommandSourceStack src, int stage, String modo) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        java.util.List<ServerPlayer> group = java.util.List.of(sp);
        com.mundosdeaxelis.network.BossBriefPacket pk;
        switch (modo) {
            case "entrada_oculto" -> pk = com.mundosdeaxelis.world.BossInfo.brief(stage, sp.blockPosition(), group, sp).withKnown(false);
            case "entrada_visible" -> pk = com.mundosdeaxelis.world.BossInfo.brief(stage, sp.blockPosition(), group, sp).withKnown(true);
            case "antesala" -> pk = com.mundosdeaxelis.world.BossInfo.dossier(stage, group, sp);
            default -> {
                src.sendFailure(Component.literal("Modos: entrada_oculto, entrada_visible, antesala."));
                return 0;
            }
        }
        com.mundosdeaxelis.network.ModNetwork.sendTo(sp, pk);
        return 1;
    }

    /** (Prueba) Muestra la pantalla de resultados con premios de ejemplo (sin dar nada). */
    private static int resultado(CommandSourceStack src, int stage, boolean full) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        java.util.List<com.mundosdeaxelis.network.BossResultPacket.Got> got = new ArrayList<>();
        java.util.List<com.mundosdeaxelis.network.BossResultPacket.Miss> missed = new ArrayList<>();
        if (full) {
            int k = 0;
            for (com.mundosdeaxelis.network.BossBriefPacket.Reward r : com.mundosdeaxelis.world.BossRewards.preview(stage)) {
                // de ejemplo: lo seguro «sale», y de los extras sale uno sí y otro no
                if (r.tag() == 0 || k++ % 2 == 0) {
                    got.add(new com.mundosdeaxelis.network.BossResultPacket.Got(r.item(), r.item().isEmpty() ? 150 : 1, r.tag() == 0));
                } else {
                    missed.add(new com.mundosdeaxelis.network.BossResultPacket.Miss(r.item(), r.item().isEmpty() ? 1 : 0, 10 + 7 * k));
                }
            }
        }
        String name = stage == 2 ? "Gran Herrero de Brasas" : "Rey Brote";
        com.mundosdeaxelis.network.ModNetwork.sendTo(sp, new com.mundosdeaxelis.network.BossResultPacket(stage, name, full,
                full ? 62 : 3, full ? 84 : 0, got, stage, stage * 20, true, missed));
        return 1;
    }

    /** (Prueba) Muestra la revelación de la llave bonus (cartel, sonido y efectos) frente a ti; no entrega la llave. */
    private static int llaveBonus(CommandSourceStack src, CrateKind kind) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        if (kind == null || kind == CrateKind.PETS) {
            src.sendFailure(Component.literal("Caja no válida."));
            return 0;
        }
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(sp.position().add(sp.getLookAngle().multiply(3, 0, 3)));
        com.mundosdeaxelis.block.CrateBlockEntity.bonusKeyFx(sp.serverLevel(), sp, pos, kind);
        return 1;
    }

    /** (Prueba) Te da 3 de cada material de jefe. */
    private static int materialesJefe(CommandSourceStack src) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        for (com.mundosdeaxelis.table.Mats m : com.mundosdeaxelis.table.Mats.values()) {
            if (m.isBoss()) {
                ItemStack st = new ItemStack(com.mundosdeaxelis.registry.ModItems.MATS.get(m).get(), 3);
                if (!sp.getInventory().add(st)) {
                    sp.drop(st, false);
                }
            }
        }
        src.sendSuccess(() -> Component.literal("Te di 3 de cada material de jefe."), false);
        return 1;
    }

    private static int esencia(CommandSourceStack src, int n) {
        ServerPlayer sp;
        try {
            sp = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(Component.literal("Solo un jugador puede usar este comando."));
            return 0;
        }
        Essence.add(sp, n);
        src.sendSuccess(() -> Component.literal("Te di " + n + " de Esencia."), false);
        return 1;
    }

    private static int niveles(CommandSourceStack src) {
        for (CrateKind k : CrateKind.values()) {
            if (k == CrateKind.PETS) {
                continue;
            }
            StringBuilder sb = new StringBuilder(k.id).append(": ");
            int[] lv = k.levels();
            int[] pc = k.levelPct();
            for (int i = 0; i < lv.length; i++) {
                sb.append("nivel ").append(lv[i]).append(" ").append(pc[i]).append("%").append(i < lv.length - 1 ? " · " : "");
            }
            final String line = sb.toString();
            src.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static String kindOf(Item it) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(it);
        if (rl == null) {
            return "otro";
        }
        String p = rl.getPath();
        if (!rl.getNamespace().equals(MundosDeAxelis.MODID)) {
            return "Minecraft";
        }
        if (p.contains("_armor_")) {
            return "armadura";
        }
        if (p.startsWith("comp_")) {
            return "mascota";
        }
        if (p.startsWith("key_")) {
            return "llave";
        }
        if (p.startsWith("decor_")) {
            return "decoración";
        }
        if (p.startsWith("cape_")) {
            return "capa";
        }
        if (p.startsWith("mat_")) {
            return "material";
        }
        if (GearDefs.get(p) != null || p.startsWith("gear_")) {
            return "herramienta/arma";
        }
        return "reliquia";
    }

    private static int simular(CommandSourceStack src, String crate, int n) {
        CrateKind k = CrateKind.byId(crate);
        if (k == null || k == CrateKind.PETS) {
            src.sendFailure(Component.literal("Caja desconocida. Usa: " + String.join(", ", crateIds())));
            return 0;
        }
        RandomSource rs = RandomSource.create();
        Map<Tier, Integer> byTier = new EnumMap<>(Tier.class);
        Map<String, Integer> byKind = new HashMap<>();
        Map<Item, Integer> byItem = new HashMap<>();
        for (int i = 0; i < n; i++) {
            CratePools.Result r = CratePools.draw(k, rs, null);
            byTier.merge(r.tier(), 1, Integer::sum);
            byKind.merge(kindOf(r.item()), 1, Integer::sum);
            byItem.merge(r.item(), 1, Integer::sum);
        }
        src.sendSuccess(() -> Component.literal("=== " + n + " aperturas de la caja " + k.id + " ==="), false);
        List<String> bad = CratePools.verify(k);
        src.sendSuccess(() -> Component.literal(bad.isEmpty() ? "Orden justo (lo mejor sale menos): OK"
                : "Orden justo: FALLA -> " + bad.get(0) + " (+" + (bad.size() - 1) + ")"), false);
        StringBuilder t = new StringBuilder("Rareza: ");
        for (Tier tier : Tier.values()) {
            int c = byTier.getOrDefault(tier, 0);
            if (c > 0) {
                t.append(tier.id).append(" ").append(String.format("%.1f", 100.0 * c / n)).append("% · ");
            }
        }
        final String tl = t.toString();
        src.sendSuccess(() -> Component.literal(tl), false);
        List<Map.Entry<String, Integer>> ks = new ArrayList<>(byKind.entrySet());
        ks.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        StringBuilder kb = new StringBuilder("Tipo: ");
        for (Map.Entry<String, Integer> e : ks) {
            kb.append(e.getKey()).append(" ").append(String.format("%.1f", 100.0 * e.getValue() / n)).append("% · ");
        }
        final String kl = kb.toString();
        src.sendSuccess(() -> Component.literal(kl), false);
        List<Map.Entry<Item, Integer>> items = new ArrayList<>(byItem.entrySet());
        items.sort(Comparator.comparingInt((Map.Entry<Item, Integer> e) -> e.getValue()).reversed());
        StringBuilder ib = new StringBuilder("Más frecuentes: ");
        for (int i = 0; i < Math.min(8, items.size()); i++) {
            ib.append(items.get(i).getKey().getDescription().getString()).append(" ")
                    .append(String.format("%.1f", 100.0 * items.get(i).getValue() / n)).append("% · ");
        }
        final String il = ib.toString();
        src.sendSuccess(() -> Component.literal(il), false);
        return n;
    }

    private static int congelar(CommandSourceStack src, int saltos) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        boolean ok = com.mundosdeaxelis.world.Frozen.freeze(sp, saltos);
        src.sendSuccess(() -> Component.literal(ok ? "Congelado: pulsa ESPACIO para romper el hielo."
                : "No se pudo congelar (ya estás congelado o acabas de romper el hielo)."), false);
        return ok ? 1 : 0;
    }
}
