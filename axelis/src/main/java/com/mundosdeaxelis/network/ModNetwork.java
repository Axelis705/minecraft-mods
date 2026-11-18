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

    /** Cada mensaje solo vale en un sentido: lo que manda el cliente al servidor, o al revés (lo demás se rechaza antes de leerlo). */
    private static final java.util.Optional<net.minecraftforge.network.NetworkDirection> TO_CLIENT =
            java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
    private static final java.util.Optional<net.minecraftforge.network.NetworkDirection> TO_SERVER =
            java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER);

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(id++, FadePacket.class,
                FadePacket::encode, FadePacket::decode, FadePacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, StartCinematicPacket.class,
                StartCinematicPacket::encode, StartCinematicPacket::decode, StartCinematicPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, StopCinematicPacket.class,
                StopCinematicPacket::encode, StopCinematicPacket::decode, StopCinematicPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, SkipCinematicPacket.class,
                SkipCinematicPacket::encode, SkipCinematicPacket::decode, SkipCinematicPacket::handle, TO_SERVER);
        CHANNEL.registerMessage(id++, ScreenFxPacket.class,
                ScreenFxPacket::encode, ScreenFxPacket::decode, ScreenFxPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, VfxPacket.class,
                VfxPacket::encode, VfxPacket::decode, VfxPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, CrateOpenPacket.class,
                CrateOpenPacket::encode, CrateOpenPacket::decode, CrateOpenPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, OreRadarPacket.class,
                OreRadarPacket::encode, OreRadarPacket::decode, OreRadarPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, SoulsPacket.class,
                SoulsPacket::encode, SoulsPacket::decode, SoulsPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, EquipSyncPacket.class,
                EquipSyncPacket::encode, EquipSyncPacket::decode, EquipSyncPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, EssenceSyncPacket.class,
                EssenceSyncPacket::encode, EssenceSyncPacket::decode, EssenceSyncPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, EquipActionPacket.class,
                EquipActionPacket::encode, EquipActionPacket::decode, EquipActionPacket::handle, TO_SERVER);
        CHANNEL.registerMessage(id++, GravePacket.class,
                GravePacket::encode, GravePacket::decode, GravePacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, CapeSyncPacket.class,
                CapeSyncPacket::encode, CapeSyncPacket::decode, CapeSyncPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, BossBriefPacket.class,
                BossBriefPacket::encode, BossBriefPacket::decode, BossBriefPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, BossStartPacket.class,
                BossStartPacket::encode, BossStartPacket::decode, BossStartPacket::handle, TO_SERVER);
        CHANNEL.registerMessage(id++, LockpickOpenPacket.class,
                LockpickOpenPacket::encode, LockpickOpenPacket::decode, LockpickOpenPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, LockpickResultPacket.class,
                LockpickResultPacket::encode, LockpickResultPacket::decode, LockpickResultPacket::handle, TO_SERVER);
        CHANNEL.registerMessage(id++, DungeonMapPacket.class,
                DungeonMapPacket::encode, DungeonMapPacket::decode, DungeonMapPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, BossDamagePacket.class,
                BossDamagePacket::encode, BossDamagePacket::decode, BossDamagePacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, BossResultPacket.class,
                BossResultPacket::encode, BossResultPacket::decode, BossResultPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, BonusKeyPacket.class,
                BonusKeyPacket::encode, BonusKeyPacket::decode, BonusKeyPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, BossTitlePacket.class,
                BossTitlePacket::encode, BossTitlePacket::decode, BossTitlePacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, NoticePacket.class,
                NoticePacket::encode, NoticePacket::decode, NoticePacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, LootPacket.class,
                LootPacket::encode, LootPacket::decode, LootPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, FrozenStatePacket.class,
                FrozenStatePacket::encode, FrozenStatePacket::decode, FrozenStatePacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, ColdPacket.class,
                ColdPacket::encode, ColdPacket::decode, ColdPacket::handle, TO_CLIENT);
        CHANNEL.registerMessage(id++, FreezeHitPacket.class,
                FreezeHitPacket::encode, FreezeHitPacket::decode, FreezeHitPacket::handle, TO_SERVER);
    }

    public static void sendTo(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    private ModNetwork() {
    }
}
