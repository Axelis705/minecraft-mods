package com.axeliscore;

import com.axeliscore.character.CoreMob;
import com.axeliscore.character.Persona;
import com.axeliscore.plat.Plat;
import com.axeliscore.registry.CoreEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * /axeliscore: versión y quién lo usa. Para probar los personajes (operadores):
 * <ul>
 *     <li>/axeliscore galeria [x y z giro]: las cinco Personas de muestra en fila, cada una con un gesto.</li>
 *     <li>/axeliscore probar &lt;animación&gt;: todos los personajes del Core cercanos la hacen.</li>
 *     <li>/axeliscore ir &lt;x y z&gt; [velocidad]: la Persona más cercana camina (o corre) hasta ahí.</li>
 * </ul>
 */
//#if MC >= 26.1
//$$ @Mod.EventBusSubscriber(modid = AxelisCore.MODID)
//#endif
public final class CoreCommands {
    /** Comandos de otros mods (ver {@link AxelisCore#commands}). */
    static final java.util.List<java.util.function.Consumer<CommandDispatcher<CommandSourceStack>>> EXTRA =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Gesto que repite cada Persona de la galería y cada cuántos ticks. */
    private static final String[][] SHOW = {{"wave", "50"}, {"talk", "400"}, {"cheer", "40"}, {"fish_cast", "40"}, {"dance", "400"}};

    private CoreCommands() {
    }

    /**
     * (En 1.20 y 1.21 se registra a mano desde {@link AxelisCore}: en la 1.21.1 el registro automático no llegaba al servidor del
     * mundo de un jugador y los comandos no existían ahí.)
     */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        AxelisCore.LOGGER.info("DIAG_AXCORE_REGISTRO {}", e.getCommandSelection());
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        var galeria = Commands.literal("galeria").requires(Plat::isOp)
                .executes(c -> gallery(c, c.getSource().getPosition(), c.getSource().getRotation().y + 180F))
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("giro", FloatArgumentType.floatArg())
                                                .executes(c -> gallery(c, new Vec3(DoubleArgumentType.getDouble(c, "x"),
                                                        DoubleArgumentType.getDouble(c, "y"), DoubleArgumentType.getDouble(c, "z")),
                                                        FloatArgumentType.getFloat(c, "giro")))))));
        var criaturas = Commands.literal("criaturas").requires(Plat::isOp)
                .executes(c -> creatures(c, c.getSource().getPosition(), c.getSource().getRotation().y + 180F))
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("giro", FloatArgumentType.floatArg())
                                                .executes(c -> creatures(c, new Vec3(DoubleArgumentType.getDouble(c, "x"),
                                                        DoubleArgumentType.getDouble(c, "y"), DoubleArgumentType.getDouble(c, "z")),
                                                        FloatArgumentType.getFloat(c, "giro")))))));
        var efecto = Commands.literal("efecto").requires(Plat::isOp)
                .then(Commands.argument("tipo", StringArgumentType.word())
                        .executes(c -> effect(c, c.getSource().getServer().getPlayerList().getPlayers()))
                        .then(Commands.argument("jugadores", net.minecraft.commands.arguments.EntityArgument.players())
                                .executes(c -> effect(c, net.minecraft.commands.arguments.EntityArgument.getPlayers(c, "jugadores")))));
        var probar = Commands.literal("probar").requires(Plat::isOp)
                .then(Commands.argument("animacion", StringArgumentType.word()).executes(CoreCommands::tryClip));
        var ir = Commands.literal("ir").requires(Plat::isOp)
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .executes(c -> go(c, 0.6))
                                        .then(Commands.argument("velocidad", DoubleArgumentType.doubleArg(0.1, 3))
                                                .executes(c -> go(c, DoubleArgumentType.getDouble(c, "velocidad")))))));
        var datos = Commands.literal("datos").requires(Plat::isOp)
                .executes(c -> dataTest(c, c.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer sp ? List.of(sp) : List.of()))
                .then(Commands.argument("jugadores", net.minecraft.commands.arguments.EntityArgument.players())
                        .executes(c -> dataTest(c, net.minecraft.commands.arguments.EntityArgument.getPlayers(c, "jugadores"))));
        d.register(Commands.literal("axeliscore").executes(CoreCommands::info).then(galeria).then(criaturas).then(probar).then(ir).then(efecto)
                .then(datos).then(com.axeliscore.combat.CombateComandos.nodo()).then(com.axeliscore.npc.NpcComandos.nodo())
                .then(com.axeliscore.anim.EstudioServidor.nodo()));
        var node = d.getRoot().getChild("axeliscore");
        StringBuilder sb = new StringBuilder();
        if (node != null) {
            for (var ch : node.getChildren()) {
                sb.append(ch.getName()).append(ch.getCommand() != null ? "*" : "").append(' ');
            }
        }
        AxelisCore.LOGGER.info("AXCORE_COMANDOS {}", sb.toString().trim());
        for (var extra : EXTRA) {
            try {
                extra.accept(d);
            } catch (RuntimeException ex) {
                AxelisCore.LOGGER.error("Axelis Core: falló el registro de comandos de un mod", ex);
            }
        }
    }

    /** Prueba de los datos guardados: el mundo (guardar y volver a leer) y las opciones (archivo de config). */
    private static int dataTest(CommandContext<CommandSourceStack> c, java.util.Collection<net.minecraft.server.level.ServerPlayer> players) {
        net.minecraft.server.MinecraftServer server = c.getSource().getServer();
        com.axeliscore.data.WorldStore s = com.axeliscore.data.WorldStore.get(server, "axelis_core_prueba");
        java.util.UUID id = java.util.UUID.nameUUIDFromBytes("prueba".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        net.minecraft.nbt.CompoundTag pl = s.player(id);
        pl.putInt("peces", com.axeliscore.plat.Nbt.i(pl, "peces") + 1);
        s.global().putString("hola", "mundo");
        s.dirty();
        com.axeliscore.data.WorldStore r = com.axeliscore.data.WorldStore.load(s.save(new net.minecraft.nbt.CompoundTag()));
        boolean world = com.axeliscore.plat.Nbt.i(r.player(id), "peces") == com.axeliscore.plat.Nbt.i(pl, "peces")
                && "mundo".equals(com.axeliscore.plat.Nbt.s(r.global(), "hola")) && r.players().size() == s.players().size();
        com.axeliscore.data.Config cfg = com.axeliscore.data.Config.of("axelis_core_prueba");
        int first = cfg.num("numero", 7, 1, 10, "Número de prueba");
        cfg.set("numero", 9);
        cfg.reload();
        boolean config = cfg.num("numero", 7, 1, 10, "Número de prueba") == 9 && cfg.bool("activo", true, "Prueba") && first >= 1;
        cfg.set("numero", 7);
        for (net.minecraft.server.level.ServerPlayer sp : players) {
            // los datos del jugador: se guardan y se manda la copia a su cliente (la prueba con cliente la revisa)
            net.minecraft.nbt.CompoundTag mine = com.axeliscore.data.PlayerStore.of(sp, "axelis_core_prueba");
            mine.putInt("visitas", com.axeliscore.plat.Nbt.i(mine, "visitas") + 1);
            com.axeliscore.data.PlayerStore.save(sp, "axelis_core_prueba");
        }
        c.getSource().sendSuccess(() -> Component.literal("datos: mundo " + (world ? "bien" : "MAL") + ", opciones " + (config ? "bien" : "MAL")), false);
        AxelisCore.LOGGER.info("AXCORE_DATOS mundo={} config={} peces={} jugadores={}", world, config, com.axeliscore.plat.Nbt.i(pl, "peces"), players.size());
        return world && config ? 1 : 0;
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        String users = String.join(", ", CoreUsers.list());
        c.getSource().sendSuccess(() -> Component.literal(AxelisCore.version() + (users.isEmpty() ? "" : " · usado por: " + users)), false);
        AxelisCore.LOGGER.info("AXCORE_INFO {} usuarios=[{}]", AxelisCore.version(), users);
        return 1;
    }

    private static int tryClip(CommandContext<CommandSourceStack> c) {
        String clip = StringArgumentType.getString(c, "animacion");
        int n = 0;
        for (CoreMob m : near(c, 40)) {
            m.showcase(clip, clip.equals("talk") || clip.equals("dance") || clip.equals("sleep") ? 400 : 60);
            n++;
        }
        AxelisCore.LOGGER.info("AXCORE_PROBAR {} personajes={}", clip, n);
        return n;
    }

    private static List<CoreMob> near(CommandContext<CommandSourceStack> c, double r) {
        Vec3 p = c.getSource().getPosition();
        return c.getSource().getLevel().getEntitiesOfClass(CoreMob.class, new AABB(p.x - r, p.y - r, p.z - r, p.x + r, p.y + r, p.z + r));
    }

    private static int gallery(CommandContext<CommandSourceStack> c, Vec3 at, float yaw) {
        if (!com.axeliscore.Ejemplos.activos()) {
            return com.axeliscore.Ejemplos.noEstan(c.getSource());
        }
        ServerLevel level = c.getSource().getLevel();
        // en fila, perpendicular a hacia donde miran
        double rad = Math.toRadians(yaw);
        double rx = Math.cos(rad), rz = Math.sin(rad);
        for (int i = 0; i < Persona.SKINS.length; i++) {
            Persona p = Plat.create(CoreEntities.PERSONA.get(), level);
            double off = (i - (Persona.SKINS.length - 1) / 2.0) * 2.0;
            p.moveTo(at.x + rx * off, at.y, at.z + rz * off, yaw, 0F);
            p.setYHeadRot(yaw);
            p.setYBodyRot(yaw);
            p.setVariant(i);
            p.setCustomName(Component.translatable("axelis_core.persona." + Persona.SKINS[i]));
            if (Persona.SKINS[i].equals("nilo")) {
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FISHING_ROD));
            } else if (Persona.SKINS[i].equals("iker")) {
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
            }
            p.setPersistenceRequired();
            level.addFreshEntity(p);
            p.showcase(SHOW[i][0], Integer.parseInt(SHOW[i][1]));
        }
        AxelisCore.LOGGER.info("AXCORE_GALERIA {}", Persona.SKINS.length);
        c.getSource().sendSuccess(() -> Component.literal("Galería de Axelis Core"), false);
        return 1;
    }

    /** Las criaturas de muestra en fila: bestia, ave (en el aire), gelatina, mascota y el coloso al final. */
    private static int creatures(CommandContext<CommandSourceStack> c, Vec3 at, float yaw) {
        if (!com.axeliscore.Ejemplos.activos()) {
            return com.axeliscore.Ejemplos.noEstan(c.getSource());
        }
        ServerLevel level = c.getSource().getLevel();
        double rad = Math.toRadians(yaw);
        double rx = Math.cos(rad), rz = Math.sin(rad);
        Object[][] row = {
                {CoreEntities.BESTIA.get(), "howl", 70, -6.0, 0.0},
                {CoreEntities.ALADO.get(), "attack", 50, -2.5, 2.2},
                {CoreEntities.GELATINA.get(), "hop", 16, 0.0, 0.0},
                {CoreEntities.CHISPA.get(), "beg", 40, 2.0, 0.0},
                {CoreEntities.COLOSO.get(), "roar", 60, 6.0, 0.0},
        };
        for (Object[] r : row) {
            @SuppressWarnings("unchecked")
            net.minecraft.world.entity.EntityType<? extends CoreMob> type = (net.minecraft.world.entity.EntityType<? extends CoreMob>) r[0];
            CoreMob m = Plat.create(type, level);
            double off = (double) r[3];
            m.moveTo(at.x + rx * off, at.y + (double) r[4], at.z + rz * off, yaw, 0F);
            m.setYHeadRot(yaw);
            m.setYBodyRot(yaw);
            if ((double) r[4] > 0) {
                m.setNoGravity(true);
            }
            m.setPersistenceRequired();
            level.addFreshEntity(m);
            m.showcase((String) r[1], (int) r[2]);
        }
        AxelisCore.LOGGER.info("AXCORE_CRIATURAS {}", row.length);
        return 1;
    }

    /** Prueba de los efectos y la interfaz: temblor, destello, vineta, titulo, aviso, dialogo, demo. */
    private static int effect(CommandContext<CommandSourceStack> c, java.util.Collection<net.minecraft.server.level.ServerPlayer> ps) {
        String k = StringArgumentType.getString(c, "tipo");
        for (var p : ps) {
            switch (k) {
                case "temblor" -> com.axeliscore.fx.Fx.shake(p, 1F, 30);
                case "destello" -> com.axeliscore.fx.Fx.flash(p, 0xC0FFFFFF, 12);
                case "vineta" -> com.axeliscore.fx.Fx.vignette(p, 0xB0600000, 40);
                case "titulo" -> com.axeliscore.fx.Fx.title(p, Component.literal("¡Récord!"), Component.literal("Salmón de 54 cm"), 0xFFD166);
                case "aviso" -> com.axeliscore.fx.Fx.toast(p, new ItemStack(Items.TROPICAL_FISH), Component.literal("¡Pez nuevo!"),
                        Component.literal("Pez tropical · 31 cm"), 0x7FF0E0);
                case "demo" -> com.axeliscore.ui.CoreScreens.open(p, AxelisCore.MODID + ":demo");
                case "dialogo" -> com.axeliscore.ui.Dialogs.show(p, Component.translatable("axelis_core.persona.nilo"),
                        com.axeliscore.plat.Ids.core("textures/entity/persona/nilo.png"),
                        Component.translatable("axelis_core.dialog.muestra"),
                        List.of(Component.translatable("axelis_core.dialog.si"), Component.translatable("axelis_core.dialog.no")),
                        choice -> {
                            AxelisCore.LOGGER.info("AXCORE_DIALOGO respuesta={}", choice);
                            if (choice == 0) {
                                com.axeliscore.fx.Fx.toast(p, new ItemStack(Items.FISHING_ROD), Component.translatable("axelis_core.persona.nilo"),
                                        Component.translatable("axelis_core.dialog.genial"), 0xFFD166);
                            }
                        });
                default -> {
                    c.getSource().sendFailure(Component.literal("temblor, destello, vineta, titulo, aviso, dialogo o demo"));
                    return 0;
                }
            }
        }
        AxelisCore.LOGGER.info("AXCORE_EFECTO {} jugadores={}", k, ps.size());
        return ps.size();
    }

    private static int go(CommandContext<CommandSourceStack> c, double speed) {
        double x = DoubleArgumentType.getDouble(c, "x"), y = DoubleArgumentType.getDouble(c, "y"), z = DoubleArgumentType.getDouble(c, "z");
        Persona best = null;
        double bd = Double.MAX_VALUE;
        for (CoreMob m : near(c, 64)) {
            if (m instanceof Persona p && p.distanceToSqr(x, y, z) < bd) {
                bd = p.distanceToSqr(x, y, z);
                best = p;
            }
        }
        if (best == null) {
            return 0;
        }
        best.setNoAi(false);
        boolean ok = Plat.walkTo(best, x, y, z, speed);
        AxelisCore.LOGGER.info("AXCORE_IR {} {}", speed, ok);
        return ok ? 1 : 0;
    }
}
