package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Etapa 269: los corazones extra (como las piezas de corazón de Zelda o los cristales de vida de Terraria). Cuatro Fragmentos de
 * corazón = un corazón más de vida máxima, para siempre. El tope crece con el progreso: tantos corazones extra como jefes vencidos
 * + 1 (8 como mucho). Se guarda en el jugador y se vuelve a poner al morir, al cambiar de mundo y al entrar.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Corazones {
    private Corazones() {
    }

    public static final int PER_HEART = 4, MAX = 8;
    private static final String KEY = "mdc_corazones";
    private static final UUID MOD_ID = UUID.fromString("6d1c3a52-9a4e-4f0b-b2c7-268c0a2a7e11");

    private static CompoundTag tag(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(KEY)) {
            root.put(KEY, new CompoundTag());
        }
        return root.getCompound(KEY);
    }

    public static int hearts(Player p) {
        return tag(p).getInt("Hearts");
    }

    public static int fragments(Player p) {
        return tag(p).getInt("Frag");
    }

    /** Cuántos corazones extra puede tener ahora (jefes vencidos + 1, como mucho 8). */
    public static int cap(ServerPlayer sp) {
        return Math.min(MAX, Sellos.playerStage(sp) + 1);
    }

    /** Pone (o quita) el modificador de vida máxima según los corazones guardados. */
    public static void apply(Player p) {
        AttributeInstance a = p.getAttribute(Attributes.MAX_HEALTH);
        if (a == null) {
            return;
        }
        a.removeModifier(MOD_ID);
        int h = hearts(p);
        if (h > 0) {
            a.addPermanentModifier(new AttributeModifier(MOD_ID, "mundos_de_axelis corazones", 2.0 * h, AttributeModifier.Operation.ADDITION));
        }
    }

    /**
     * Absorbe un fragmento. Devuelve false (y no se gasta) si ya está en el tope. Al juntar cuatro, sube un corazón con un buen
     * efecto: sonido de nivel, partículas de corazón y te cura del todo.
     */
    public static boolean absorb(ServerPlayer sp) {
        CompoundTag t = tag(sp);
        int h = t.getInt("Hearts");
        if (h >= cap(sp)) {
            Component boss = Sellos.bossToBeat(Sellos.playerStage(sp) + 2);
            sp.displayClientMessage(h >= MAX ? Component.translatable("message.mundos_de_axelis.corazon.max").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("message.mundos_de_axelis.corazon.tope", boss).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return false;
        }
        int f = t.getInt("Frag") + 1;
        ServerLevel sl = sp.serverLevel();
        if (f >= PER_HEART) {
            f = 0;
            h++;
            t.putInt("Hearts", h);
            apply(sp);
            sp.setHealth(sp.getMaxHealth());
            sl.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
            sl.playSound(null, sp.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.5F, 1.4F);
            sl.sendParticles(ParticleTypes.HEART, sp.getX(), sp.getY() + 1.2, sp.getZ(), 16, 0.6, 0.6, 0.6, 0.1);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, sp.getX(), sp.getY() + 1.0, sp.getZ(), 30, 0.4, 0.8, 0.4, 0.3);
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.corazon.nuevo", h).withStyle(ChatFormatting.RED), false);
        } else {
            sl.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.9F + 0.15F * f);
            sl.sendParticles(ParticleTypes.HEART, sp.getX(), sp.getY() + 1.4, sp.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.corazon.fragmento", f, PER_HEART).withStyle(ChatFormatting.RED), true);
        }
        t.putInt("Frag", f);
        return true;
    }

    // ------------------------------------------------------------------ que no se pierdan

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        CompoundTag old = e.getOriginal().getPersistentData();
        if (old.contains(KEY)) {
            e.getEntity().getPersistentData().put(KEY, old.getCompound(KEY).copy());
        }
        apply(e.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        apply(e.getEntity());
        if (hearts(e.getEntity()) > 0) {
            e.getEntity().setHealth(e.getEntity().getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        apply(e.getEntity());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        apply(e.getEntity());
    }
}
