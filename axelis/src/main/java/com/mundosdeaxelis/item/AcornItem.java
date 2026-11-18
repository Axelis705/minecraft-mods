package com.mundosdeaxelis.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Bellota del Santuario (dungeon del bosque) y Remache Ardiente (dungeon de la Forja): se usan desde la mano secundaria y reparan
 * un 25 % (60 % los dorados) de la durabilidad de cada pieza de la caja de su dungeon: la armadura puesta y las espadas y
 * herramientas del inventario. No se gastan si no hay nada dañado.
 */
public class AcornItem extends Item {
    /** Parte de la durabilidad máxima que recupera cada pieza (normal 25 %, dorada 60 %). */
    public static final double REPAIR = 0.25, REPAIR_GOLDEN = 0.60;

    private final double fraction;
    private final String key;
    /** Prefijo de los textos: «acorn» (bosque) o «rivet» (Forja). */
    private final String prefix;
    private final com.mundosdeaxelis.crate.CrateKind crate;

    private java.util.Set<String> ids;

    /** ¿Es una pieza de armadura, espada o herramienta de la caja de este objeto (Naturaleza: Bosque y Brote; Forja: Forja y Bronce)? */
    private synchronized boolean isOurs(ItemStack piece) {
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(piece.getItem());
        if (key == null) {
            return false;
        }
        if (ids == null) {
            java.util.Set<String> found = new java.util.HashSet<>();
            for (com.mundosdeaxelis.gear.GearSet set : crate.sets) {
                for (com.mundosdeaxelis.gear.ArmorDefs.Piece p : com.mundosdeaxelis.gear.ArmorDefs.Piece.values()) {
                    found.add(com.mundosdeaxelis.gear.ArmorDefs.id(set, p));
                }
                for (com.mundosdeaxelis.gear.GearDef d : com.mundosdeaxelis.gear.GearDefs.of(set)) {
                    found.add(d.id());
                }
            }
            ids = found;
        }
        return ids.contains(key.getPath());
    }

    private boolean repair(ItemStack piece) {
        if (piece.isEmpty() || !piece.isDamaged() || !isOurs(piece)) {
            return false;
        }
        int amount = Math.max(1, (int) Math.round(piece.getMaxDamage() * fraction));
        piece.setDamageValue(Math.max(0, piece.getDamageValue() - amount));
        return true;
    }

    public AcornItem() {
        this(REPAIR, "acorn", Rarity.UNCOMMON);
    }

    /** Bellota (Naturaleza). */
    public AcornItem(double fraction, String key, Rarity rarity) {
        this(fraction, key, rarity, "acorn", com.mundosdeaxelis.crate.CrateKind.FOREST);
    }

    public AcornItem(double fraction, String key, Rarity rarity, String prefix, com.mundosdeaxelis.crate.CrateKind crate) {
        super(new Item.Properties().stacksTo(16).rarity(rarity));
        this.fraction = fraction;
        this.key = key;
        this.prefix = prefix;
        this.crate = crate;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (hand != InteractionHand.OFF_HAND) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.mundos_de_axelis." + prefix + ".offhand").withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.fail(held);
        }
        boolean any = false;
        for (ItemStack piece : player.getInventory().armor) {
            any |= repair(piece);
        }
        for (ItemStack piece : player.getInventory().items) {
            any |= repair(piece);
        }
        if (!any) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.mundos_de_axelis." + prefix + ".none").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(held);
        }
        if (!level.isClientSide) {
            boolean forge = crate == com.mundosdeaxelis.crate.CrateKind.FORGE;
            if (forge) {
                level.playSound(null, player.blockPosition(), com.mundosdeaxelis.registry.ModSounds.RIVET_USE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            } else {
                level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5F, 1.8F);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
            if (level instanceof ServerLevel sl) {
                if (forge) {
                    sl.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.4, 0.6, 0.4, 0.0);
                    sl.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.4, 0.7, 0.4, 0.15);
                } else {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.4, 0.7, 0.4, 0.0);
                }
            }
            player.displayClientMessage(Component.translatable("message.mundos_de_axelis." + prefix + ".used").withStyle(ChatFormatting.GREEN), true);
        }
        player.getCooldowns().addCooldown(this, 30);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis." + key).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis." + prefix + ".use", Math.round(fraction * 100)).withStyle(ChatFormatting.GREEN));
    }
}
