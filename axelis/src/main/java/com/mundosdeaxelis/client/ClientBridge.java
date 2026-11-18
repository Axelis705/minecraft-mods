package com.mundosdeaxelis.client;

import com.mundosdeaxelis.network.StartCinematicPacket;
import net.minecraft.client.Minecraft;

/** Puente servidor->cliente. Solo se carga en el cliente (se llama desde DistExecutor). */
public final class ClientBridge {
    private ClientBridge() {
    }

    public static void openBossBrief(com.mundosdeaxelis.network.BossBriefPacket p) {
        Minecraft.getInstance().setScreen(new BossBriefScreen(p));
    }

    public static void openBossResult(com.mundosdeaxelis.network.BossResultPacket p) {
        Minecraft.getInstance().setScreen(new BossResultScreen(p));
    }

    public static void dungeonMap(com.mundosdeaxelis.network.DungeonMapPacket p) {
        DungeonMapHud.set(p);
    }

    public static void openLockpick(com.mundosdeaxelis.network.LockpickOpenPacket p) {
        Minecraft.getInstance().setScreen(new LockpickScreen(p.pos, p.level, p.seed));
    }

    public static void startFade(int rgb, int fadeIn, int hold, int fadeOut) {
        FadeOverlay.start(rgb, fadeIn, hold, fadeOut);
    }

    public static void bossTitle(String title, String subtitle, int rgb, int ticks) {
        BossTitleOverlay.start(title, subtitle, rgb, ticks);
    }

    public static void frozen(boolean frozen, int hits, int max) {
        FrozenClient.set(frozen, hits, max);
    }

    public static void cold(int meter) {
        ColdHud.set(meter);
    }

    public static void loot(String itemId, int count) {
        LootHud.push(itemId, count);
    }

    public static void notice(String title, String subtitle, int rgb, int ticks) {
        NoticeHud.push(title, subtitle, rgb, ticks);
    }

    public static void screenFx(int kind, int rgb, int ticks) {
        ScreenFx.start(kind, rgb, ticks);
    }

    public static void vfx(com.mundosdeaxelis.network.VfxPacket packet) {
        VfxClient.handle(packet);
    }

    public static void openCrate(com.mundosdeaxelis.network.CrateOpenPacket p) {
        Minecraft.getInstance().setScreen(new CrateRollScreen(p));
    }

    public static void openPrizes(com.mundosdeaxelis.crate.CrateKind kind) {
        Minecraft.getInstance().setScreen(new CratePrizesScreen(kind));
    }

    public static void souls(com.mundosdeaxelis.network.SoulsPacket p) {
        SoulsHud.set(p.souls, p.max);
    }

    public static void bonusKey(com.mundosdeaxelis.network.BonusKeyPacket p) {
        BonusKeyHud.show(p.kind);
    }

    public static void bossDamage(com.mundosdeaxelis.network.BossDamagePacket p) {
        BossDamageHud.set(p);
    }

    public static void graves(com.mundosdeaxelis.network.GravePacket p) {
        GraveHud.set(p);
    }

    public static void equip(com.mundosdeaxelis.network.EquipSyncPacket p) {
        EquipClient.set(p);
    }

    public static void cape(com.mundosdeaxelis.network.CapeSyncPacket p) {
        ClientCapes.set(p.player, p.cape);
    }

    public static void essence(com.mundosdeaxelis.network.EssenceSyncPacket p) {
        TableClient.set(p);
    }

    /** Nivel de mesa del jugador local (para el brillo de la mesa). */
    public static int tableLevel() {
        return TableClient.level;
    }

    public static void oreRadar(com.mundosdeaxelis.network.OreRadarPacket p) {
        OreRadar.show(p);
    }

    public static void startCinematic(StartCinematicPacket packet) {
        CinematicClient.start(packet);
    }

    public static void stopCinematic() {
        CinematicClient.stop();
    }
}
