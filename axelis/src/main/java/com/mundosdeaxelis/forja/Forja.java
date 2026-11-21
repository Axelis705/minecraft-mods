package com.mundosdeaxelis.forja;

import com.mundosdeaxelis.block.GranYunqueBlock;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.network.ForjaOpenPacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.table.Essence;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * La forja de llaves del Gran Yunque (etapa 270), en el servidor. Al hacer clic se abre la pantalla con los fragmentos que llevas;
 * el juego de habilidad se juega en el cliente ({@link ForjaGame}) y aquí se aplica el resultado, comprobando que la sesión es de
 * verdad (abierta por este jugador, en este yunque, hace al menos unos segundos) y que aún tiene los fragmentos:
 * <ul>
 *     <li><b>forjada</b>: se gastan 4 fragmentos y la Esencia de la forja (la quinta parte de la llave) y recibes la llave;</li>
 *     <li><b>perfecta</b>: lo mismo, y el yunque te devuelve 1 fragmento;</li>
 *     <li><b>fallida</b>: la llave se quiebra y se pierde 1 fragmento (decisión de Axelis).</li>
 * </ul>
 */
public final class Forja {
    private Forja() {
    }

    public static final int NEED = com.mundosdeaxelis.block.VaultLoot.FRAGMENTS_PER_KEY;
    /** Lo mínimo que dura una forja de verdad (las tres fases). */
    private static final long MIN_TICKS = 20 * 5;

    private record Session(BlockPos pos, long start, long seed) {
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static Item fragment(int tier) {
        return ModItems.KEY_FRAGMENTS.get(CrateKind.byIndex(tier)).get();
    }

    /** Esencia que cuesta forjar la llave de ese tier (la quinta parte de comprarla en la Mesa). */
    public static int cost(int tier) {
        return CrateKind.byIndex(tier).keyCost() / 5;
    }

    private static int count(ServerPlayer sp, Item it) {
        int n = 0;
        for (ItemStack s : sp.getInventory().items) {
            if (s.is(it)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void take(ServerPlayer sp, Item it, int n) {
        for (ItemStack s : sp.getInventory().items) {
            if (n <= 0) {
                break;
            }
            if (s.is(it)) {
                int k = Math.min(n, s.getCount());
                s.shrink(k);
                n -= k;
            }
        }
        sp.inventoryMenu.broadcastChanges();
    }

    private static void give(ServerPlayer sp, ItemStack st) {
        if (!sp.getInventory().add(st)) {
            sp.drop(st, false);
        }
    }

    public static void open(ServerPlayer sp, BlockPos anvil) {
        int[] counts = new int[9];
        for (int t = 1; t <= 8; t++) {
            counts[t] = count(sp, fragment(t));
        }
        long seed = sp.getRandom().nextLong();
        SESSIONS.put(sp.getUUID(), new Session(anvil.immutable(), sp.serverLevel().getGameTime(), seed));
        sp.serverLevel().playSound(null, anvil, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4F, 1.4F);
        ModNetwork.sendTo(sp, new ForjaOpenPacket(anvil, counts, seed));
    }

    public static void result(ServerPlayer sp, BlockPos pos, int tier, int code) {
        Session s = SESSIONS.remove(sp.getUUID());
        if (s == null || !s.pos().equals(pos) || code <= 0 || code > 3 || tier < 1 || tier > 8) {
            return;
        }
        ServerLevel sl = sp.serverLevel();
        if (sp.blockPosition().distSqr(pos) > 100.0 || !(sl.getBlockState(pos).getBlock() instanceof GranYunqueBlock)) {
            return;
        }
        if (sl.getGameTime() - s.start() < MIN_TICKS) {
            return; // demasiado rápido para haber jugado de verdad
        }
        Item frag = fragment(tier);
        if (count(sp, frag) < NEED) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.forja.faltan", NEED).withStyle(ChatFormatting.RED), true);
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        if (code == 3) {
            take(sp, frag, 1);
            sl.playSound(null, pos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 0.8F, 1.1F);
            sl.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.7F);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 14, 0.3, 0.3, 0.3, 0.02);
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.forja.quebrada").withStyle(ChatFormatting.RED), false);
            return;
        }
        int cost = cost(tier);
        if (!Essence.spend(sp, cost)) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.forja.esencia", cost).withStyle(ChatFormatting.RED), true);
            return;
        }
        Essence.sync(sp);
        take(sp, frag, NEED);
        CrateKind k = CrateKind.byIndex(tier);
        ItemStack key = new ItemStack(ModItems.KEYS.get(k).get());
        Component name = key.getHoverName();
        give(sp, key);
        sl.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        sl.sendParticles(ParticleTypes.LAVA, x, y, z, 10, 0.3, 0.2, 0.3, 0.0);
        sl.sendParticles(ParticleTypes.FLAME, x, y, z, 24, 0.3, 0.4, 0.3, 0.05);
        if (code == 2) {
            give(sp, new ItemStack(frag));
            sl.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 0.7F, 1.4F);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 0.3, z, 30, 0.3, 0.5, 0.3, 0.3);
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.forja.perfecta", name).withStyle(ChatFormatting.GOLD), false);
        } else {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.forja.forjada", name).withStyle(ChatFormatting.GREEN), false);
        }
    }

    public static void clearRuntime() {
        SESSIONS.clear();
    }
}
