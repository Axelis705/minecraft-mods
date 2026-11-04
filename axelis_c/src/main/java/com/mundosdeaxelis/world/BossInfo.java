package com.mundosdeaxelis.world;

import com.mundosdeaxelis.gear.ArmorDefs;
import com.mundosdeaxelis.gear.GearSet;
import com.mundosdeaxelis.gear.GearStats;
import com.mundosdeaxelis.network.BossBriefPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Datos de la ficha del jefe que se muestra al usar su Piedra de Portal (la pantalla {@code BossBriefScreen}): vida, daño del
 * golpe fuerte con el set B y el A de la caja de la etapa, equipo recomendado y jugadores que viajarían. Los números salen
 * de {@link BossBalance}, así que siempre coinciden con la pelea real.
 */
public final class BossInfo {
    private BossInfo() {
    }

    private static GearSet setOfRank(int rank) {
        for (GearSet s : GearSet.values()) {
            if (s.rank == rank) {
                return s;
            }
        }
        return GearSet.FOREST;
    }

    /** Ficha de entrada (al usar el portal) para quien la abre y el grupo que viajaría ahora mismo. */
    public static BossBriefPacket brief(int stage, BlockPos pos, List<ServerPlayer> group, ServerPlayer viewer) {
        return brief(BossBriefPacket.ENTRY, stage, pos, group, viewer);
    }

    /** Ficha del jefe para la antesala (ya dentro de la dungeon): el grupo es todo el que está en la dimensión. */
    public static BossBriefPacket dossier(int stage, List<ServerPlayer> group, ServerPlayer viewer) {
        return brief(BossBriefPacket.DOSSIER, stage, BlockPos.ZERO, group, viewer);
    }

    private static BossBriefPacket brief(int mode, int stage, BlockPos pos, List<ServerPlayer> group, ServerPlayer viewer) {
        int bRank = BossBalance.setBRank(stage);
        GearSet b = setOfRank(bRank);
        GearSet a = setOfRank(bRank - 1);
        int n = Math.max(1, group.size());
        List<String> names = new ArrayList<>();
        for (ServerPlayer p : group) {
            names.add(p.getName().getString());
        }
        int hpBase = (int) BossBalance.hp(stage);
        int hpNow = Math.round(hpBase * BossBalance.hpPlayers(n));
        int dmgBonus = Math.round((BossBalance.dmgPlayers(n) - 1.0F) * 100.0F);
        boolean known = com.mundosdeaxelis.table.Essence.bossBeaten(viewer, stage);
        return new BossBriefPacket(mode, stage, pos, known, hpNow, hpBase, dmgBonus,
                Math.round((BossBalance.hpPlayers(2) - 1.0F) * 100.0F), Math.round((BossBalance.dmgPlayers(2) - 1.0F) * 100.0F),
                BossBalance.strongHitTakenB(stage) / 2.0F, BossBalance.strongHitTakenA(stage) / 2.0F,
                Component.translatable("set.mundos_de_axelis." + b.id), Component.translatable("set.mundos_de_axelis." + a.id),
                ArmorDefs.setTotal(bRank), GearStats.sword(bRank), names, BossRewards.preview(stage));
    }
}
