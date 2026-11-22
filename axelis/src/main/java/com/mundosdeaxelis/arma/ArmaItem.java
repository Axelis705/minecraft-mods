package com.mundosdeaxelis.arma;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.crate.Tiers;
import com.mundosdeaxelis.gear.Ability;
import com.mundosdeaxelis.gear.GearTool;
import com.mundosdeaxelis.gear.Perk;
import com.mundosdeaxelis.gear.SoulUser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Un arma nueva (etapa 281): un tipo (espada, hacha de guerra, martillo, lanza o dagas) de un tier (una caja). Es una
 * {@link SwordItem}, así le valen los encantamientos de espada y las gemas. Las almas se ganan al matar con ella y se gastan
 * en su especial (clic derecho).
 */
public class ArmaItem extends SwordItem implements GearTool, SoulUser {
    private static final UUID REACH_UUID = UUID.fromString("6b1d2c8e-4f7a-4c39-9a51-28c0e3a7b281");
    public final TipoArma tipo;
    public final TierArma tierArma;
    private Multimap<Attribute, AttributeModifier> mods;
    private final Map<Perk, Integer> perks = new EnumMap<>(Perk.class);

    public ArmaItem(TipoArma tipo, TierArma tier) {
        super(new ArmaMaterial(tipo, tier), 0, tipo.velocidad - 4.0F, props(tier));
        this.tipo = tipo;
        this.tierArma = tier;
        if (tier.nivelAlmas() > 0) {
            perks.put(Perk.SOULS, tier.nivelAlmas());
        }
        if (tier.nivelBotin() > 0) {
            perks.put(Perk.LOOT, tier.nivelBotin());
        }
    }

    private static Item.Properties props(TierArma t) {
        Item.Properties p = new Item.Properties().rarity(t.rareza().rarity);
        if (t.ignifuga()) {
            p.fireResistant();
        }
        return p;
    }

    public static String id(TipoArma tipo, TierArma tier) {
        return "arma_" + tipo.id + "_" + tier.id;
    }

    /** Daño total de un golpe normal (sin encantamientos). */
    public float dano() {
        return ArmaMaterial.total(tipo, tierArma);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }
        if (mods == null) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
            b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", dano() - 1.0F, AttributeModifier.Operation.ADDITION));
            b.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", tipo.velocidad - 4.0F, AttributeModifier.Operation.ADDITION));
            if (tipo.alcance != 0.0F) {
                b.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH_UUID, "Weapon reach", tipo.alcance, AttributeModifier.Operation.ADDITION));
            }
            mods = b.build();
        }
        return mods;
    }

    /** Solo la espada hace el barrido de Minecraft; el resto pega a uno solo (y tiene su propio estilo). */
    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        if (action == ToolActions.SWORD_SWEEP) {
            return tipo == TipoArma.ESPADA;
        }
        return super.canPerformAction(stack, action);
    }

    /** El hacha de guerra deja sin escudo al que se cubre (como el hacha de Minecraft). */
    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return tipo == TipoArma.HACHA;
    }

    /** Animaciones de ataque (solo en el cliente; Forge lo llama dentro del constructor, así que no se lee nada del arma aquí). */
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(com.mundosdeaxelis.client.ArmaAnim.INSTANCE);
    }

    // ------------------------------------------------------------------ GearTool

    @Override
    public Ability ability() {
        return null;
    }

    @Override
    public Tier tier() {
        return tierArma.rareza();
    }

    @Override
    public Map<Perk, Integer> perks() {
        return perks;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Tiers.shown(this).paint(super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        String k = "tooltip.mundos_de_axelis.arma.";
        Tier seen = Tiers.shown(this);
        tip.add(Component.translatable("tooltip.mundos_de_axelis.tier", seen.label()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable(k + "tipo." + tipo.id).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable("crate.mundos_de_axelis." + tierArma.caja.id).withStyle(Style.EMPTY.withColor(tierArma.color))));
        tip.add(Component.translatable(k + "pasiva." + tipo.id + ".1").withStyle(ChatFormatting.GREEN));
        tip.add(Component.translatable(k + "pasiva." + tipo.id + ".2").withStyle(ChatFormatting.GREEN));
        tip.add(Component.translatable(k + "elemento." + tierArma.elemento.id).withStyle(Style.EMPTY.withColor(tierArma.color)));
        // etapa 282: el arrojadizo del tier, ahora como estallido de los golpes cargados
        tip.add(Component.translatable(k + "estallido." + Armas.estallidoDe(this).id).withStyle(Style.EMPTY.withColor(Armas.estallidoDe(this).color)));
        for (Map.Entry<Perk, Integer> e : perks.entrySet()) {
            tip.add(Component.translatable("perk.mundos_de_axelis." + e.getKey().id + e.getValue()).withStyle(ChatFormatting.DARK_GREEN));
        }
        tip.add(Component.translatable(k + "especial." + tipo.id).withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable(k + "especial." + tipo.id + ".desc").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.translatable("tooltip.mundos_de_axelis.souls_cost", tipo.almas).withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable("tooltip.mundos_de_axelis.gear.cooldown", Math.round(Armas.enfriamiento(this) / 20.0F))
                        .withStyle(ChatFormatting.DARK_GRAY)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            return Armas.tryEspecial(this, sp, sl, stack) ? InteractionResultHolder.sidedSuccess(stack, false) : InteractionResultHolder.fail(stack);
        }
        return player.getCooldowns().isOnCooldown(this) ? InteractionResultHolder.fail(stack) : InteractionResultHolder.success(stack);
    }
}
