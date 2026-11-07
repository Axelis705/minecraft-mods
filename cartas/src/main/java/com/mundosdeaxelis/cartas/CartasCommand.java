package com.mundosdeaxelis.cartas;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** /cartas: herramientas para dar cartas y sobres, ver el estado y probar el sistema (nivel de permiso 2). */
@Mod.EventBusSubscriber(modid = Cartas.MOD_ID)
public final class CartasCommand {
    private static final SuggestionProvider<CommandSourceStack> CARD_IDS = (c, b) -> SharedSuggestionProvider.suggest(Cards.ALL.stream().map(d -> d.id), b);
    private static final SuggestionProvider<CommandSourceStack> PACK_IDS = (c, b) -> SharedSuggestionProvider.suggest(Cards.PACKS.stream().map(p -> p.id), b);

    private CartasCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> d) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cartas").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("dar").then(Commands.argument("jugador", EntityArgument.player())
                .then(Commands.argument("carta", StringArgumentType.word()).suggests(CARD_IDS)
                        .executes(c -> give(c.getSource(), EntityArgument.getPlayer(c, "jugador"), StringArgumentType.getString(c, "carta"), false, 1))
                        .then(Commands.argument("holo", BoolArgumentType.bool())
                                .executes(c -> give(c.getSource(), EntityArgument.getPlayer(c, "jugador"), StringArgumentType.getString(c, "carta"), BoolArgumentType.getBool(c, "holo"), 1))
                                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 64))
                                        .executes(c -> give(c.getSource(), EntityArgument.getPlayer(c, "jugador"), StringArgumentType.getString(c, "carta"),
                                                BoolArgumentType.getBool(c, "holo"), IntegerArgumentType.getInteger(c, "cantidad"))))))));
        root.then(Commands.literal("sobre").then(Commands.argument("jugador", EntityArgument.player())
                .then(Commands.argument("sobre", StringArgumentType.word()).suggests(PACK_IDS)
                        .executes(c -> pack(c.getSource(), EntityArgument.getPlayer(c, "jugador"), StringArgumentType.getString(c, "sobre"), 1))
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 16))
                                .executes(c -> pack(c.getSource(), EntityArgument.getPlayer(c, "jugador"), StringArgumentType.getString(c, "sobre"),
                                        IntegerArgumentType.getInteger(c, "cantidad")))))));
        root.then(Commands.literal("estado").then(player(c -> state(c.getSource(), EntityArgument.getPlayer(c, "jugador")))));
        root.then(Commands.literal("llenar").then(player(c -> fill(c.getSource(), EntityArgument.getPlayer(c, "jugador")))));
        root.then(Commands.literal("reiniciar").then(player(c -> reset(c.getSource(), EntityArgument.getPlayer(c, "jugador")))));
        root.then(Commands.literal("simular").then(Commands.argument("sobre", StringArgumentType.word()).suggests(PACK_IDS)
                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 200000))
                        .executes(c -> simulate(c.getSource(), StringArgumentType.getString(c, "sobre"), IntegerArgumentType.getInteger(c, "cantidad"))))));
        root.then(Commands.literal("cabana").then(Commands.literal("probar").executes(c -> {
            ServerLevel lv = c.getSource().getServer().overworld();
            net.minecraft.core.BlockPos at = new net.minecraft.core.BlockPos(300, 90, 300);
            Cabana.build(lv, at, net.minecraft.core.Direction.SOUTH, null);
            String bad = Cabana.check(lv, at, net.minecraft.core.Direction.SOUTH);
            c.getSource().sendSuccess(() -> Component.literal(bad.isEmpty() ? "PRUEBA_CABANA_OK" : "PRUEBA_CABANA_FALLA: " + bad), false);
            return bad.isEmpty() ? 1 : 0;
        })).executes(c -> {
            ServerPlayer sp = c.getSource().getPlayerOrException();
            String msg = Cabana.build(sp);
            c.getSource().sendSuccess(() -> Component.literal(msg), true);
            return 1;
        }));
        root.then(Commands.literal("probar").executes(c -> selfTest(c.getSource())));
        d.register(root);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> player(com.mojang.brigadier.Command<CommandSourceStack> run) {
        return Commands.argument("jugador", EntityArgument.player()).executes(run);
    }

    private static int give(CommandSourceStack s, ServerPlayer p, String id, boolean holo, int n) {
        CardDef d = Cards.get(id);
        if (d == null) {
            s.sendFailure(Component.literal("No existe la carta '" + id + "'."));
            return 0;
        }
        ItemStack st = CartasItems.stack(d, holo, n);
        if (!p.getInventory().add(st)) {
            p.drop(st, false);
        }
        s.sendSuccess(() -> Component.literal("Carta " + d.name + (holo ? " (holográfica)" : "") + " x" + n + " para " + p.getName().getString()), true);
        return n;
    }

    private static int pack(CommandSourceStack s, ServerPlayer p, String id, int n) {
        PackDef d = Cards.pack(id);
        if (d == null) {
            s.sendFailure(Component.literal("No existe el sobre '" + id + "'."));
            return 0;
        }
        ItemStack st = CartasItems.pack(d, n);
        if (!p.getInventory().add(st)) {
            p.drop(st, false);
        }
        s.sendSuccess(() -> Component.literal(d.name + " x" + n + " para " + p.getName().getString()), true);
        return n;
    }

    private static int state(CommandSourceStack s, ServerPlayer p) {
        AlbumCap a = AlbumCaps.of(p);
        s.sendSuccess(() -> Component.literal(String.format("Álbum de %s: %d/%d huecos (%.1f %%), categorías completas %d/%d, vida extra +%.1f corazones",
                p.getName().getString(), a.filled(), AlbumLogic.totalSlots(), a.percent(), a.completeCategories(), Cards.CATEGORIES.size(), a.bonusHealth() / 2.0)), false);
        return a.filled();
    }

    private static int fill(CommandSourceStack s, ServerPlayer p) {
        AlbumCap a = AlbumCaps.of(p);
        for (CardDef d : Cards.ALL) {
            a.add(d.id, false);
            a.add(d.id, true);
        }
        AlbumBonuses.apply(p);
        s.sendSuccess(() -> Component.literal("Álbum completado (para probar)."), true);
        return 1;
    }

    private static int reset(CommandSourceStack s, ServerPlayer p) {
        AlbumCaps.of(p).clear();
        AlbumBonuses.apply(p);
        s.sendSuccess(() -> Component.literal("Álbum vaciado."), true);
        return 1;
    }

    private static int simulate(CommandSourceStack s, String id, int n) {
        PackDef d = Cards.pack(id);
        if (d == null) {
            s.sendFailure(Component.literal("No existe el sobre '" + id + "'."));
            return 0;
        }
        Random r = new Random(1234);
        int[] byRar = new int[CardRarity.values().length];
        int holo = 0;
        int total = 0;
        for (int i = 0; i < n; i++) {
            for (PackRoller.Pull p : PackRoller.roll(d, r)) {
                byRar[p.card().rarity.ordinal()]++;
                if (p.holo()) {
                    holo++;
                }
                total++;
            }
        }
        StringBuilder sb = new StringBuilder("Simulación de " + n + " " + d.name + ": ");
        for (CardRarity q : CardRarity.values()) {
            sb.append(String.format("%s %.2f %%  ", q.label, 100.0 * byRar[q.ordinal()] / total));
        }
        sb.append(String.format("holográficas %.2f %%", 100.0 * holo / total));
        s.sendSuccess(() -> Component.literal(sb.toString()), false);
        return n;
    }

    /** Prueba sin jugador (sirve en la consola del servidor): registro de objetos, sorteo y guardado del album. */
    private static int selfTest(CommandSourceStack s) {
        List<String> errors = new ArrayList<>();
        for (CardDef d : Cards.ALL) {
            Item it = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(Cartas.MOD_ID, "carta_" + d.id));
            if (it == null || it == Items.AIR) {
                errors.add("falta el objeto carta_" + d.id);
            }
        }
        for (PackDef p : Cards.PACKS) {
            Item it = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(Cartas.MOD_ID, "sobre_" + p.id));
            if (it == null || it == Items.AIR) {
                errors.add("falta el objeto sobre_" + p.id);
            }
            Random r = new Random(7);
            for (int i = 0; i < 2000; i++) {
                List<PackRoller.Pull> l = PackRoller.roll(p, r);
                if (l.size() != p.count || !l.get(l.size() - 1).card().rarity.atLeast(p.min)) {
                    errors.add("sobre " + p.id + " mal sorteado");
                    break;
                }
            }
        }
        // el album se guarda y se lee igual
        AlbumCap a = new AlbumCap();
        a.add(Cards.ALL.get(0).id, false);
        a.add(Cards.ALL.get(1).id, true);
        CompoundTag tag = a.serializeNBT();
        AlbumCap b = new AlbumCap();
        b.deserializeNBT(tag);
        if (!b.has(Cards.ALL.get(0).id, false) || !b.has(Cards.ALL.get(1).id, true) || b.filled() != 2) {
            errors.add("el album no se guarda bien");
        }
        // el stack holografico conserva su marca
        ItemStack hs = CartasItems.stack(Cards.ALL.get(0), true, 1);
        if (!CardItem.isHolo(hs) || CardItem.isHolo(CartasItems.stack(Cards.ALL.get(0), false, 1))) {
            errors.add("la marca holografica falla");
        }
        if (errors.isEmpty()) {
            s.sendSuccess(() -> Component.literal("PRUEBA_CARTAS_OK " + Cards.ALL.size() + " cartas, " + Cards.PACKS.size() + " sobres"), false);
            return 1;
        }
        s.sendFailure(Component.literal("PRUEBA_CARTAS_FALLO " + String.join("; ", errors)));
        return 0;
    }
}
