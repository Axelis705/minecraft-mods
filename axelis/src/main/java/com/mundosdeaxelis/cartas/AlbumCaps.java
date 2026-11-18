package com.mundosdeaxelis.cartas;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Registro y enlace del album a cada jugador. */
@Mod.EventBusSubscriber(modid = Cartas.MOD_ID)
public final class AlbumCaps {
    public static final Capability<AlbumCap> ALBUM = CapabilityManager.get(new CapabilityToken<>() {
    });
    private static final ResourceLocation KEY = new ResourceLocation(Cartas.MOD_ID, "album");

    private AlbumCaps() {
    }

    /** Se llama desde el bus del mod (Cartas.init). */
    public static void register(RegisterCapabilitiesEvent e) {
        e.register(AlbumCap.class);
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> e) {
        if (e.getObject() instanceof Player) {
            e.addCapability(KEY, new Provider());
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        e.getOriginal().reviveCaps();
        e.getOriginal().getCapability(ALBUM).ifPresent(old -> e.getEntity().getCapability(ALBUM).ifPresent(n -> n.copyFrom(old)));
        e.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            AlbumBonuses.apply(sp);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            AlbumBonuses.apply(sp);
        }
    }

    public static AlbumCap of(Player p) {
        return p.getCapability(ALBUM).orElseGet(AlbumCap::new);
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final AlbumCap cap = new AlbumCap();
        private final LazyOptional<AlbumCap> opt = LazyOptional.of(() -> cap);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> c, @Nullable Direction side) {
            return ALBUM.orEmpty(c, opt);
        }

        @Override
        public CompoundTag serializeNBT() {
            return cap.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            cap.deserializeNBT(nbt);
        }
    }
}
