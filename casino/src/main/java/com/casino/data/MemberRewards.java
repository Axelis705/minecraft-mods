package com.casino.data;

import com.casino.chip.Chips;
import com.casino.registry.ModDecor;
import com.casino.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;

/**
 * Premios de socio: lo que trae el cofre al subir de nivel (trofeo del nivel, ficha de colección, fichas y sorpresas) y el título
 * sobre la cabeza (un equipo del marcador con prefijo "★★ Oro", que Minecraft ya muestra en el nombre, la lista de jugadores y el chat).
 */
public final class MemberRewards {
    private MemberRewards() {
    }

    /** Lo que trae el cofre de un nivel. */
    public static List<ItemStack> chest(int level, RandomSource r) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(ModDecor.TROPHIES.get(level).get()));
        out.add(new ItemStack(ModItems.MEMBER_CHIPS.get(level).get()));
        out.addAll(Chips.stacksFor(Membership.LEVEL_REWARD[level]));
        // sorpresas: más y mejores mientras más alto el nivel
        Item[][] pool = {
                {Items.EMERALD, Items.GOLD_INGOT, Items.CAKE, Items.FIREWORK_ROCKET},
                {Items.DIAMOND, Items.EMERALD, Items.GOLD_BLOCK, Items.JUKEBOX},
                {Items.DIAMOND, Items.EMERALD_BLOCK, Items.GOLDEN_APPLE, Items.NAME_TAG},
                {Items.DIAMOND_BLOCK, Items.ENCHANTED_GOLDEN_APPLE, Items.TOTEM_OF_UNDYING, Items.ELYTRA},
                {Items.NETHERITE_INGOT, Items.DIAMOND_BLOCK, Items.ENCHANTED_GOLDEN_APPLE, Items.NETHER_STAR}};
        Item[] p = pool[Math.max(0, Math.min(pool.length - 1, level))];
        int n = 1 + level;
        for (int i = 0; i < n; i++) {
            Item it = p[r.nextInt(p.length)];
            int count = it == Items.DIAMOND || it == Items.EMERALD || it == Items.GOLD_INGOT ? 2 + r.nextInt(2 + level * 2)
                    : it == Items.FIREWORK_ROCKET ? 8 : 1;
            out.add(new ItemStack(it, count));
        }
        // decoración del casino de regalo
        List<Item> decor = new ArrayList<>();
        decor.add(ModDecor.CHANDELIER.get().asItem());
        decor.add(ModDecor.CASINO_PAINTING.get().asItem());
        decor.add(ModDecor.COCKTAIL_TABLE.get().asItem());
        decor.add(ModDecor.PARASOL.get().asItem());
        if (level >= 2) {
            decor.add(ModDecor.GRAND_CHANDELIER.get().asItem());
        }
        out.add(new ItemStack(decor.get(r.nextInt(decor.size())), 1 + r.nextInt(2)));
        return out;
    }

    // ------------------------------------------------------------------ título sobre la cabeza

    private static final String TEAM = "casino_lv";

    /** Pone al jugador en el equipo de su nivel (si no está en otro equipo que no sea del casino). */
    public static void title(MinecraftServer server, ServerPlayer p, int level) {
        Scoreboard sb = server.getScoreboard();
        String who = p.getScoreboardName();
        PlayerTeam cur = sb.getPlayersTeam(who);
        if (cur != null && !cur.getName().startsWith(TEAM)) {
            return; // ya tiene su propio equipo: no lo tocamos
        }
        String name = TEAM + level;
        PlayerTeam t = sb.getPlayerTeam(name);
        if (t == null) {
            t = sb.addPlayerTeam(name);
        }
        String stars = "★".repeat(level + 1);
        Component prefix = Component.literal(stars + " ").append(Component.translatable("casino.member.level_" + Membership.LEVELS[level]))
                .append(Component.literal(" ")).withStyle(s -> s.withColor(TextColor.fromRgb(Membership.COLOR[level])));
        t.setPlayerPrefix(prefix);
        t.setColor(level >= 4 ? ChatFormatting.AQUA : level >= 2 ? ChatFormatting.YELLOW : ChatFormatting.WHITE);
        if (cur != t) {
            sb.addPlayerToTeam(who, t);
        }
    }

    /** Nivel del título de un jugador visto desde el cliente (por su equipo), o -1 si no tiene. */
    public static int titleLevel(net.minecraft.world.entity.player.Player p) {
        var t = p.getTeam();
        if (t != null && t.getName().startsWith(TEAM)) {
            try {
                return Integer.parseInt(t.getName().substring(TEAM.length()));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }
}
