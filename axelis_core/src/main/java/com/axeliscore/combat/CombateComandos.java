package com.axeliscore.combat;

import com.axeliscore.character.Coloso;
import com.axeliscore.character.CoreMob;
import com.axeliscore.plat.Plat;
import com.axeliscore.registry.CoreEntities;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * /axeliscore combate ... (operadores): para ver y probar el combate del Core.
 * <ul>
 *     <li>{@code estados}: cinco bestias con un estado cada una (y un golpe para ver la barra y el número).</li>
 *     <li>{@code estado <nombre> [segundos] [nivel]}: un estado en uno mismo (en supervivencia).</li>
 *     <li>{@code avisos}: una figura de cada forma (sin daño) para verlas llenarse.</li>
 *     <li>{@code proyectiles}: los proyectiles de muestra, despacio.</li>
 *     <li>{@code jefe}: el Coloso enfrente; {@code fase <n>} y {@code ataque <nombre>} para el jefe más cercano.</li>
 * </ul>
 */
public final class CombateComandos {
    private CombateComandos() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> nodo() {
        return Commands.literal("combate").requires(Plat::isOp)
                .then(Commands.literal("estados").executes(CombateComandos::estados))
                .then(Commands.literal("estado").then(Commands.argument("nombre", StringArgumentType.word())
                        .executes(c -> estado(c, 8, 1))
                        .then(Commands.argument("segundos", IntegerArgumentType.integer(1, 600))
                                .executes(c -> estado(c, IntegerArgumentType.getInteger(c, "segundos"), 1))
                                .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 5))
                                        .executes(c -> estado(c, IntegerArgumentType.getInteger(c, "segundos"), IntegerArgumentType.getInteger(c, "nivel")))))))
                .then(Commands.literal("limpiar").executes(CombateComandos::limpiar))
                .then(Commands.literal("avisos").executes(CombateComandos::avisos))
                .then(Commands.literal("proyectiles").executes(CombateComandos::proyectiles))
                .then(Commands.literal("jefe").executes(CombateComandos::jefe))
                .then(Commands.literal("fase").then(Commands.argument("n", IntegerArgumentType.integer(0, 9)).executes(CombateComandos::fase)))
                .then(Commands.literal("ataque").then(Commands.argument("nombre", StringArgumentType.word()).executes(CombateComandos::ataque)))
                .then(Commands.literal("prueba").executes(CombateComandos::prueba))
                .then(Commands.literal("revisar").executes(CombateComandos::revisar));
    }

    private static LivingEntity blancoPrueba;
    private static float vidaPrueba;

    /**
     * Prueba sin jugadores (para el servidor de pruebas): estados en una bestia, un aviso y una bola de fuego hacia ella, y un Coloso que
     * pasa a la fase 2. {@code revisar} (unos segundos después) mira que la bestia sí recibió los golpes.
     */
    private static int prueba(CommandContext<CommandSourceStack> c) {
        CommandSourceStack s = c.getSource();
        ServerLevel l = s.getLevel();
        Vec3 p = s.getPosition().add(3, 0, 3);
        CoreMob m = Plat.create(CoreEntities.BESTIA.get(), l);
        m.moveTo(p.x, Combate.suelo(l, p), p.z, 0, 0);
        m.setNoAi(true);
        l.addFreshEntity(m);
        boolean est = Estados.aplicar(m, Estados.MARCADO, 400, 2, null) && Estados.aplicar(m, Estados.CONGELADO, 400, 1, null)
                && Estados.tiene(m, Estados.MARCADO) && Estados.nivel(m, Estados.MARCADO) == 2;
        float mult = Estados.multiplicadorDano(m);
        blancoPrueba = m;
        vidaPrueba = m.getHealth();
        Aviso.circulo(l, m.position(), 2F, 10).dano(null, 3).sonido("", 0, 0).particulas("crit").lanzar();
        Proyectil.hacia(l, CoreProyectiles.FUEGO, null, m.position().add(5, 1.5, 0), m, 0.6F);
        Coloso col = Plat.create(CoreEntities.COLOSO.get(), l);
        Vec3 q = s.getPosition().add(-8, 0, -8);
        col.moveTo(q.x, Combate.suelo(l, q), q.z, 0, 0);
        l.addFreshEntity(col);
        col.forzarFase(1);
        String r = "AXCORE_COMBATE estados=" + est + " marcado=" + String.format(java.util.Locale.ROOT, "%.2f", mult) + " jefe=" + col.fase()
                + " ataques=" + Coloso.ataques().size();
        com.axeliscore.AxelisCore.LOGGER.info(r);
        s.sendSuccess(() -> Component.literal(r), false);
        return 1;
    }

    private static int revisar(CommandContext<CommandSourceStack> c) {
        boolean ok = blancoPrueba != null && blancoPrueba.getHealth() < vidaPrueba;
        String r = "AXCORE_COMBATE_GOLPES " + (ok ? "ok" : "MAL") + " vida=" + (blancoPrueba == null ? -1 : blancoPrueba.getHealth()) + "/" + vidaPrueba
                + " quemado=" + (blancoPrueba != null && Estados.tiene(blancoPrueba, Estados.QUEMADO));
        com.axeliscore.AxelisCore.LOGGER.info(r);
        c.getSource().sendSuccess(() -> Component.literal(r), false);
        return ok ? 1 : 0;
    }

    /** Un punto a {@code dist} bloques delante de quien da el comando (y su giro). */
    private static Vec3 delante(CommandSourceStack s, double dist) {
        float yaw = s.getRotation().y;
        Vec3 f = Combate.adelante(yaw);
        return s.getPosition().add(f.x * dist, 0, f.z * dist);
    }

    private static int estados(CommandContext<CommandSourceStack> c) {
        CommandSourceStack s = c.getSource();
        ServerLevel l = s.getLevel();
        float yaw = s.getRotation().y;
        Vec3 f = Combate.adelante(yaw), side = new Vec3(f.z, 0, -f.x);
        Vec3 c0 = delante(s, 5);
        Estado[] est = {Estados.ATURDIDO, Estados.QUEMADO, Estados.CONGELADO, Estados.MARCADO, Estados.SANGRADO};
        for (int i = 0; i < est.length; i++) {
            Vec3 p = c0.add(side.scale((i - 2) * 1.8));
            CoreMob m = Plat.create(CoreEntities.BESTIA.get(), l);
            if (m == null) {
                continue;
            }
            m.moveTo(p.x, Combate.suelo(l, p), p.z, yaw + 180F, 0);
            m.setNoAi(true);
            m.setCustomName(est[i].nombre());
            l.addFreshEntity(m);
            Estados.aplicar(m, est[i], 20 * 30, i == 3 ? 2 : 1, s.getEntity());
            m.invulnerableTime = 0;
            Plat.hurt(m, l.damageSources().magic(), 3F + i);
        }
        s.sendSuccess(() -> Component.literal("Cinco bestias con un estado cada una"), false);
        return 1;
    }

    private static int estado(CommandContext<CommandSourceStack> c, int segundos, int nivel) {
        String n = StringArgumentType.getString(c, "nombre");
        Estado e = Estados.porId(n.contains(":") ? n : "axelis_core:" + n);
        if (e == null) {
            c.getSource().sendFailure(Component.literal("No existe el estado " + n + " (aturdido, quemado, congelado, marcado, sangrado)"));
            return 0;
        }
        if (!(c.getSource().getEntity() instanceof LivingEntity le)) {
            return 0;
        }
        if (!Estados.aplicar(le, e, segundos * 20, nivel, null)) {
            c.getSource().sendFailure(Component.literal("No lo tomó (en creativo no hay estados)"));
            return 0;
        }
        return 1;
    }

    private static int limpiar(CommandContext<CommandSourceStack> c) {
        if (c.getSource().getEntity() instanceof LivingEntity le) {
            Estados.limpiar(le);
        }
        return 1;
    }

    /** Las cinco formas, sin daño y lentas (10 s), alrededor de un punto delante. */
    private static int avisos(CommandContext<CommandSourceStack> c) {
        CommandSourceStack s = c.getSource();
        ServerLevel l = s.getLevel();
        float yaw = s.getRotation().y;
        Vec3 f = Combate.adelante(yaw), side = new Vec3(f.z, 0, -f.x);
        Vec3 c0 = delante(s, 9);
        int t = 200;
        Aviso.circulo(l, c0.add(side.scale(-6)), 2.5F, t).color(Aviso.NARANJA).sonido("", 0, 0).particulas("").lanzar();
        Aviso.anillo(l, c0.add(side.scale(-1)), 3F, 1.4F, t).color(Aviso.AMARILLO).sonido("", 0, 0).particulas("").lanzar();
        Aviso.linea(l, c0.add(side.scale(3.5)).subtract(f.scale(3)), yaw, 7, 1.8F, t).color(Aviso.ROJO).sonido("", 0, 0).particulas("").lanzar();
        Aviso.cono(l, c0.add(side.scale(-3.5)).add(f.scale(4)), yaw + 180F, 4F, 100, t).color(Aviso.MORADO).sonido("", 0, 0).particulas("").lanzar();
        Aviso.cuadro(l, c0.add(side.scale(6.5)), yaw + 20, 3F, 3F, t).color(Aviso.AZUL).sonido("", 0, 0).particulas("").lanzar();
        s.sendSuccess(() -> Component.literal("Avisos: círculo, anillo, línea, cono y cuadro"), false);
        return 1;
    }

    /** Los cuatro proyectiles de muestra cruzando despacio delante. */
    private static int proyectiles(CommandContext<CommandSourceStack> c) {
        CommandSourceStack s = c.getSource();
        ServerLevel l = s.getLevel();
        float yaw = s.getRotation().y;
        Vec3 f = Combate.adelante(yaw), side = new Vec3(f.z, 0, -f.x);
        Vec3 c0 = delante(s, 6).add(0, 1.6, 0);
        ProyectilTipo[] tipos = {CoreProyectiles.FUEGO, CoreProyectiles.ESCARCHA, CoreProyectiles.FRAGMENTO, CoreProyectiles.CRISTAL};
        for (int i = 0; i < tipos.length; i++) {
            Vec3 from = c0.add(side.scale(-4)).add(0, i * 0.9 - 1.2, 0);
            Proyectil p = Proyectil.lanzar(l, tipos[i], s.getEntity(), from, side.scale(0.08));
            p.setDeltaMovement(side.scale(0.08));
        }
        s.sendSuccess(() -> Component.literal("Proyectiles: fuego, escarcha, fragmento y cristal"), false);
        return 1;
    }

    private static int jefe(CommandContext<CommandSourceStack> c) {
        CommandSourceStack s = c.getSource();
        ServerLevel l = s.getLevel();
        Vec3 p = delante(s, 10);
        Coloso col = Plat.create(CoreEntities.COLOSO.get(), l);
        if (col == null) {
            return 0;
        }
        col.moveTo(p.x, Combate.suelo(l, p), p.z, s.getRotation().y + 180F, 0);
        l.addFreshEntity(col);
        s.sendSuccess(() -> Component.literal("¡Ahí viene el Coloso!"), false);
        return 1;
    }

    private static Jefe cercano(CommandSourceStack s) {
        Vec3 p = s.getPosition();
        Jefe best = null;
        double bd = Double.MAX_VALUE;
        for (Jefe j : s.getLevel().getEntitiesOfClass(Jefe.class, new AABB(p.x - 48, p.y - 24, p.z - 48, p.x + 48, p.y + 24, p.z + 48))) {
            double d = j.distanceToSqr(p);
            if (d < bd) {
                bd = d;
                best = j;
            }
        }
        return best;
    }

    private static int fase(CommandContext<CommandSourceStack> c) {
        Jefe j = cercano(c.getSource());
        if (j == null) {
            c.getSource().sendFailure(Component.literal("No hay jefes cerca"));
            return 0;
        }
        j.forzarFase(IntegerArgumentType.getInteger(c, "n"));
        return 1;
    }

    private static int ataque(CommandContext<CommandSourceStack> c) {
        Jefe j = cercano(c.getSource());
        if (j == null) {
            c.getSource().sendFailure(Component.literal("No hay jefes cerca"));
            return 0;
        }
        Ataque a = j.buscarAtaque(StringArgumentType.getString(c, "nombre"));
        if (a == null) {
            c.getSource().sendFailure(Component.literal("Ese jefe no tiene ese ataque"));
            return 0;
        }
        LivingEntity t = c.getSource().getEntity() instanceof LivingEntity le ? le : j.getTarget();
        if (t == null && c.getSource().getEntity() == null) {
            t = s(c);
        }
        j.empezar(a, t);
        return 1;
    }

    private static LivingEntity s(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = c.getSource().getServer().getPlayerList().getPlayers().isEmpty() ? null
                : c.getSource().getServer().getPlayerList().getPlayers().get(0);
        return p;
    }
}
