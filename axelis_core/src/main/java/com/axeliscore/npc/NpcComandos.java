package com.axeliscore.npc;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Plat;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * /axeliscore npc ... (operadores):
 * <ul>
 *     <li>{@code poner <npc>}: pone un NPC donde estás (su casa y su trabajo quedan ahí).</li>
 *     <li>{@code casa} / {@code trabajo}: cambia la casa o el trabajo del NPC más cercano a donde estás.</li>
 *     <li>{@code lista}: los NPC y misiones registrados.</li>
 *     <li>{@code mision <id>}: acepta una misión; {@code evento <clave> [n]}: avisa un evento.</li>
 *     <li>{@code amistad <npc> <puntos>}: suma amistad; {@code reiniciar}: borra tus misiones, amistad y compras.</li>
 *     <li>{@code prueba} y {@code revisar}: prueba automática (con un jugador falso) para el servidor de pruebas.</li>
 * </ul>
 */
public final class NpcComandos {
    private NpcComandos() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> nodo() {
        return Commands.literal("npc").requires(Plat::isOp)
                .then(Commands.literal("poner").then(Commands.argument("npc", StringArgumentType.greedyString())
                        .suggests((c, b) -> {
                            for (NpcDef d : NpcDef.todos()) {
                                b.suggest(d.id);
                            }
                            return b.buildFuture();
                        }).executes(NpcComandos::poner)))
                .then(Commands.literal("casa").executes(c -> lugar(c, true)))
                .then(Commands.literal("trabajo").executes(c -> lugar(c, false)))
                .then(Commands.literal("lista").executes(NpcComandos::lista))
                .then(Commands.literal("mision").then(Commands.argument("id", StringArgumentType.greedyString()).executes(NpcComandos::mision)))
                .then(Commands.literal("evento").then(Commands.argument("clave", StringArgumentType.string())
                        .executes(c -> evento(c, 1))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 1000)).executes(c -> evento(c, IntegerArgumentType.getInteger(c, "n"))))))
                .then(Commands.literal("amistad").then(Commands.argument("npc", StringArgumentType.string())
                        .then(Commands.argument("puntos", IntegerArgumentType.integer(-1000, 1000)).executes(NpcComandos::amistad))))
                .then(Commands.literal("reiniciar").executes(NpcComandos::reiniciar))
                .then(Commands.literal("prueba").executes(NpcComandos::prueba))
                .then(Commands.literal("revisar").executes(NpcComandos::revisar));
    }

    private static void decir(CommandSourceStack s, String t) {
        s.sendSuccess(() -> Component.literal(t), false);
    }

    private static int poner(CommandContext<CommandSourceStack> c) {
        NpcDef d = NpcDef.porId(StringArgumentType.getString(c, "npc").trim());
        if (d == null) {
            decir(c.getSource(), "No existe ese NPC (mira /axeliscore npc lista)");
            return 0;
        }
        BlockPos pos = BlockPos.containing(c.getSource().getPosition());
        Npc.poner(c.getSource().getLevel(), d, pos);
        decir(c.getSource(), "Listo: " + d.nombre().getString() + " vive y trabaja aquí (cambia con /axeliscore npc casa|trabajo)");
        return 1;
    }

    private static Npc masCercano(CommandSourceStack s) {
        var v = s.getPosition();
        List<Npc> l = s.getLevel().getEntitiesOfClass(Npc.class, new net.minecraft.world.phys.AABB(v.x - 32, v.y - 32, v.z - 32, v.x + 32, v.y + 32,
                v.z + 32));
        Npc mejor = null;
        double md = Double.MAX_VALUE;
        for (Npc n : l) {
            double d = n.position().distanceToSqr(s.getPosition());
            if (d < md) {
                md = d;
                mejor = n;
            }
        }
        return mejor;
    }

    private static int lugar(CommandContext<CommandSourceStack> c, boolean casa) {
        Npc n = masCercano(c.getSource());
        if (n == null) {
            decir(c.getSource(), "No hay ningún NPC cerca");
            return 0;
        }
        BlockPos pos = BlockPos.containing(c.getSource().getPosition());
        if (casa) {
            n.casa(pos);
        } else {
            n.trabajo(pos);
        }
        decir(c.getSource(), (casa ? "Casa" : "Trabajo") + " de " + n.getName().getString() + ": " + pos.toShortString());
        return 1;
    }

    private static int lista(CommandContext<CommandSourceStack> c) {
        for (NpcDef d : NpcDef.todos()) {
            decir(c.getSource(), d.id + " (" + d.nombre().getString() + "): " + d.misiones().size() + " misiones"
                    + (d.tienda() != null ? ", tienda con " + d.tienda().ofertas().size() + " cosas" : ""));
        }
        decir(c.getSource(), "Misiones registradas: " + Misiones.todas().size());
        return 1;
    }

    private static int mision(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(c, "id").trim();
        boolean ok = Misiones.aceptar(p, id);
        decir(c.getSource(), ok ? "Misión aceptada" : "No se puede aceptar (" + (Misiones.porId(id) == null ? "no existe" : Misiones.estado(p,
                Misiones.porId(id))) + ")");
        return ok ? 1 : 0;
    }

    private static int evento(CommandContext<CommandSourceStack> c, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Misiones.evento(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "clave"), n);
        return 1;
    }

    private static int amistad(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String npc = StringArgumentType.getString(c, "npc");
        Amistad.sumar(p, npc, IntegerArgumentType.getInteger(c, "puntos"));
        decir(c.getSource(), "Amistad con " + npc + ": " + Amistad.puntos(p, npc) + " (nivel " + Amistad.nivel(p, npc) + ")");
        return 1;
    }

    private static void borrar(ServerPlayer p) {
        var d = PlayerStore.of(p, AxelisCore.MODID);
        d.remove("misiones");
        d.remove("amistad");
        d.remove("tiendas");
        PlayerStore.save(p, AxelisCore.MODID);
    }

    private static int reiniciar(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        borrar(c.getSource().getPlayerOrException());
        decir(c.getSource(), "Se borraron tus misiones, amistades y compras");
        return 1;
    }

    // ------------------------------------------------------------------ prueba automática

    private static Npc npcPrueba;
    private static ServerPlayer prueba;

    /** Un jugador que no está conectado (para probar sin nadie en el servidor). Lo que se le manda por la red no sale a ningún lado. */
    private static ServerPlayer jugadorPrueba(ServerLevel l) {
        if (prueba != null && prueba.level() == l) {
            return prueba;
        }
        var perfil = new com.mojang.authlib.GameProfile(java.util.UUID.fromString("a7e1c0de-0000-4000-8000-00000000c0de"), "[prueba_npc]");
        //#if MC >= 1.21
        //$$ prueba = new ServerPlayer(l.getServer(), l, perfil, net.minecraft.server.level.ClientInformation.createDefault());
        //#else
        prueba = net.minecraftforge.common.util.FakePlayerFactory.get(l, perfil);
        //#endif
        return prueba;
    }

    private static int contar(Player p, String id) {
        return p instanceof ServerPlayer sp ? Misiones.contar(sp, id) : 0;
    }

    /**
     * Con un jugador falso: aceptar y entregar (pan de Mara), cadena y "hablar con" (recado), amistad y regalo, tienda (comprar,
     * vender, nivel pedido), cazar (zombis de Iker), diaria (trigo) y una misión suelta que se entrega sola (revisar).
     */
    private static int prueba(CommandContext<CommandSourceStack> c) {
        ServerLevel l = c.getSource().getLevel();
        ServerPlayer p = jugadorPrueba(l);
        borrar(p);
        p.getInventory().clearContent();
        BlockPos pos = BlockPos.containing(c.getSource().getPosition()).offset(2, 0, 2);
        npcPrueba = Npc.poner(l, CoreNpcs.MARA, pos);
        StringBuilder r = new StringBuilder("AXCORE_NPC");
        boolean ok = true;
        String mara = CoreNpcs.MARA.id, iker = CoreNpcs.IKER.id;
        // 1. pan: aceptar, juntar, entregar
        Mision pan = Misiones.porId(AxelisCore.MODID + ":pan");
        ok &= ver(r, "disponible", Misiones.estado(p, pan) == Misiones.Estado.DISPONIBLE);
        ok &= ver(r, "aceptar", Misiones.aceptar(p, pan.id) && Misiones.estado(p, pan) == Misiones.Estado.ACTIVA);
        ok &= ver(r, "recado_bloqueado", Misiones.estado(p, Misiones.porId(AxelisCore.MODID + ":recado")) == Misiones.Estado.BLOQUEADA);
        Plat.give(p, new ItemStack(Plat.item("minecraft:bread"), 6));
        Misiones.revisar(p, pan, true);
        ok &= ver(r, "lista", Misiones.estado(p, pan) == Misiones.Estado.LISTA);
        ok &= ver(r, "entregar", Misiones.entregar(p, pan.id) && Misiones.estado(p, pan) == Misiones.Estado.HECHA);
        // regalo del nivel 1 de Mara: 8 panes (los 6 se los quedó ella)
        ok &= ver(r, "premio", contar(p, "minecraft:emerald") == 4 && contar(p, "minecraft:bread") == 8);
        ok &= ver(r, "amistad", Amistad.puntos(p, mara) == 10 && Amistad.nivel(p, mara) == 1);
        // 2. recado: cadena + hablar con Iker
        Mision recado = Misiones.porId(AxelisCore.MODID + ":recado");
        ok &= ver(r, "cadena", Misiones.aceptar(p, recado.id));
        Misiones.alHablar(p, iker);
        ok &= ver(r, "hablar", Misiones.entregar(p, recado.id) && Amistad.puntos(p, iker) == 5 && Amistad.puntos(p, mara) == 16);
        // 3. tienda: comprar 2 veces pan (4 por 1), vender trigo (20 por 1) y lo que pide más amistad
        Tienda t = CoreNpcs.MARA.tienda();
        int antes = contar(p, "minecraft:emerald");
        ok &= ver(r, "comprar", t.usar(p, mara, 0, 2) == 2 && contar(p, "minecraft:emerald") == antes - 2
                && contar(p, "minecraft:bread") == 16);
        Plat.give(p, new ItemStack(Plat.item("minecraft:wheat"), 45));
        ok &= ver(r, "vender", t.usar(p, mara, 6, 5) == 2 && contar(p, "minecraft:wheat") == 5 && contar(p, "minecraft:emerald") == antes);
        ok &= ver(r, "nivel_tienda", t.usar(p, mara, 5, 1) == 0);
        // 4. cazar: zombis de Iker
        Mision zombis = Misiones.porId(AxelisCore.MODID + ":zombis");
        Misiones.aceptar(p, zombis.id);
        for (int i = 0; i < 5; i++) {
            Misiones.contarDerrota(p, "minecraft:zombie", true);
        }
        ok &= ver(r, "cazar", Misiones.estado(p, zombis) == Misiones.Estado.LISTA);
        // 5. diaria: trigo (16) → hecha hoy
        Mision trigo = Misiones.porId(AxelisCore.MODID + ":trigo");
        Plat.give(p, new ItemStack(Plat.item("minecraft:wheat"), 16));
        ok &= ver(r, "diaria", Misiones.aceptar(p, trigo.id) && Misiones.entregar(p, trigo.id)
                && Misiones.estado(p, trigo) == Misiones.Estado.HECHA && !Misiones.aceptar(p, trigo.id));
        // 6. misión suelta con evento: se entrega sola en el siguiente tick
        ok &= ver(r, "evento", Misiones.aceptar(p, CoreNpcs.PRUEBA.id));
        Misiones.evento(p, AxelisCore.MODID + ":prueba", 3);
        ok &= ver(r, "npc", npcPrueba.def() == CoreNpcs.MARA);
        r.append(" ok=").append(ok);
        AxelisCore.LOGGER.info(r.toString());
        decir(c.getSource(), r.toString());
        return ok ? 1 : 0;
    }

    private static boolean ver(StringBuilder r, String que, boolean bien) {
        r.append(' ').append(que).append('=').append(bien ? "si" : "NO");
        return bien;
    }

    private static int revisar(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = jugadorPrueba(c.getSource().getLevel());
        boolean sola = Misiones.vecesHecha(p, CoreNpcs.PRUEBA.id) == 1 && !Misiones.activa(p, CoreNpcs.PRUEBA.id);
        boolean vivo = npcPrueba != null && npcPrueba.isAlive() && npcPrueba.def() == CoreNpcs.MARA;
        String r = "AXCORE_NPC_REVISAR " + (sola && vivo ? "ok" : "MAL") + " sola=" + sola + " npc=" + vivo;
        AxelisCore.LOGGER.info(r);
        decir(c.getSource(), r);
        if (npcPrueba != null) {
            npcPrueba.discard();
        }
        return sola && vivo ? 1 : 0;
    }
}
