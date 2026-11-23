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
@Mod.EventBusSubscriber(modid = AxelisCore.MODID)
public final class CoreCommands {
    /** Gesto que repite cada Persona de la galería y cada cuántos ticks. */
    private static final String[][] SHOW = {{"wave", "50"}, {"talk", "400"}, {"cheer", "40"}, {"fish_cast", "40"}, {"dance", "400"}};

    private CoreCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
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
        var probar = Commands.literal("probar").requires(Plat::isOp)
                .then(Commands.argument("animacion", StringArgumentType.word()).executes(CoreCommands::tryClip));
        var ir = Commands.literal("ir").requires(Plat::isOp)
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .executes(c -> go(c, 0.6))
                                        .then(Commands.argument("velocidad", DoubleArgumentType.doubleArg(0.1, 3))
                                                .executes(c -> go(c, DoubleArgumentType.getDouble(c, "velocidad")))))));
        d.register(Commands.literal("axeliscore").executes(CoreCommands::info).then(galeria).then(criaturas).then(probar).then(ir));
        var node = d.getRoot().getChild("axeliscore");
        StringBuilder sb = new StringBuilder();
        if (node != null) {
            for (var ch : node.getChildren()) {
                sb.append(ch.getName()).append(ch.getCommand() != null ? "*" : "").append(' ');
            }
        }
        AxelisCore.LOGGER.info("AXCORE_COMANDOS {}", sb.toString().trim());
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
