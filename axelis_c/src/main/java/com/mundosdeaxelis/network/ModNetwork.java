package com.mundosdeaxelis.network;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Canal de red entre servidor y clientes (diálogos, destellos, cinemáticas...). */
public final class ModNetwork {
    private static final String PROTOCOL = "19";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MundosDeAxelis.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static boolean registered = false;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(id++, FadePacket.class,
                FadePacket::encode, FadePacket::decode, FadePacket::handle);
        CHANNEL.registerMessage(id++, StartCinematicPacket.class,
                StartCinematicPacket::encode, StartCinematicPacket::decode, StartCinematicPacket::handle);
        CHANNEL.registerMessage(id++, StopCinematicPacket.class,
                StopCinematicPacket::encode, StopCinematicPacket::decode, StopCinematicPacket::handle);
        CHANNEL.registerMessage(id++, SkipCinematicPacket.class,
                SkipCinematicPacket::encode, SkipCinematicPacket::decode, SkipCinematicPacket::handle);
        CHANNEL.registerMessage(id++, ScreenFxPacket.class,
                ScreenFxPacket::encode, ScreenFxPacket::decode, ScreenFxPacket::handle);
        CHANNEL.registerMessage(id++, VfxPacket.class,
                VfxPacket::encode, VfxPacket::decode, VfxPacket::handle);
        CHANNEL.registerMessage(id++, CrateOpenPacket.class,
                CrateOpenPacket::encode, CrateOpenPacket::decode, CrateOpenPacket::handle);
        CHANNEL.registerMessage(id++, OreRadarPacket.class,
                OreRadarPacket::encode, OreRadarPacket::decode, OreRadarPacket::handle);
        CHANNEL.registerMessage(id++, SoulsPacket.class,
                SoulsPacket::encode, SoulsPacket::decode, SoulsPacket::handle);
        CHANNEL.registerMessage(id++, EquipSyncPacket.class,
                EquipSyncPacket::encode, EquipSyncPacket::decode, EquipSyncPacket::handle);
        CHANNEL.registerMessage(id++, EssenceSyncPacket.class,
                EssenceSyncPacket::encode, EssenceSyncPacket::decode, EssenceSyncPacket::handle);
        CHANNEL.registerMessage(id++, EquipActionPacket.class,
                EquipActionPacket::encode, EquipActionPacket::decode, EquipActionPacket::handle);
        CHANNEL.registerMessage(id++, GravePacket.class,
                GravePacket::encode, GravePacket::decode, GravePacket::handle);
        CHANNEL.registerMessage(id++, CapeSyncPacket.class,
                CapeSyncPacket::encode, CapeSyncPacket::decode, CapeSyncPacket::handle);
        CHANNEL.registerMessage(id++, BossBriefPacket.class,
                BossBriefPacket::encode, BossBriefPacket::decode, BossBriefPacket::handle);
        CHANNEL.registerMessage(id++, BossStartPacket.class,
                BossStartPacket::encode, BossStartPacket::decode, BossStartPacket::handle);
        CHANNEL.registerMessage(id++, LockpickOpenPacket.class,
                LockpickOpenPacket::encode, LockpickOpenPacket::decode, LockpickOpenPacket::handle);
        CHANNEL.registerMessage(id++, LockpickResultPacket.class,
                LockpickResultPacket::encode, LockpickResultPacket::decode, LockpickResultPacket::handle);
        CHANNEL.registerMessage(id++, DungeonMapPacket.class,
                DungeonMapPacket::encode, DungeonMapPacket::decode, DungeonMapPacket::handle);
        CHANNEL.registerMessage(id++, BossDamagePacket.class,
                BossDamagePacket::encode, BossDamagePacket::decode, BossDamagePacket::handle);
        CHANNEL.registerMessage(id++, BossResultPacket.class,
                BossResultPacket::encode, BossResultPacket::decode, BossResultPacket::handle);
        CHANNEL.registerMessage(id++, BonusKeyPacket.class,
                BonusKeyPacket::encode, BonusKeyPacket::decode, BonusKeyPacket::handle);
        CHANNEL.registerMessage(id++, BossTitlePacket.class,
                BossTitlePacket::encode, BossTitlePacket::decode, BossTitlePacket::handle);
        CHANNEL.registerMessage(id++, NoticePacket.class,
                NoticePacket::encode, NoticePacket::decode, NoticePacket::handle);
        CHANNEL.registerMessage(id++, LootPacket.class,
                LootPacket::encode, LootPacket::decode, LootPacket::handle);
        CHANNEL.registerMessage(id++, FrozenStatePacket.class,
                FrozenStatePacket::encode, FrozenStatePacket::decode, FrozenStatePacket::handle);
        CHANNEL.registerMessage(id++, ColdPacket.class,
                ColdPacket::encode, ColdPacket::decode, ColdPacket::handle);
        CHANNEL.registerMessage(id++, FreezeHitPacket.class,
                FreezeHitPacket::encode, FreezeHitPacket::decode, FreezeHitPacket::handle);
    }

    public static void sendTo(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    private ModNetwork() {
    }
}
