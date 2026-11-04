package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.crate.Tiered;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Una pieza de la armadura de un set. Con las cuatro puestas se activa el bonus del set (ver {@link ArmorSetEvents}). */
public class SetArmor extends ArmorItem implements Tiered {
    private final GearSet set;
    private final ArmorDefs.Piece piece;

    private static final java.util.Map<GearSet, SetArmorMaterial> MATERIALS = new java.util.EnumMap<>(GearSet.class);

    private static SetArmorMaterial material(GearSet s) {
        return MATERIALS.computeIfAbsent(s, SetArmorMaterial::new);
    }

    public SetArmor(GearSet set, ArmorDefs.Piece piece) {
        super(material(set), piece.type, props(set));
        this.set = set;
        this.piece = piece;
    }

    private static Properties props(GearSet set) {
        Properties p = new Properties().rarity(ArmorDefs.tier(set).rarity);
        if (set.fireproof) {
            p.fireResistant();
        }
        return p;
    }

    // Modelo propio de cada set (cuerpo humanoide + cubos extra, ver SetArmorModels) con su textura de 128x128 {set}_3d.png.
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return MundosDeAxelis.MODID + ":textures/models/armor/" + set.id + "_3d.png";
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                // Forge llama a esto dentro del constructor del Item: leer "set" tarde, nunca capturarlo aquí.
                GearSet current = SetArmor.this.set;
                if (current == null) {
                    return original;
                }
                return com.mundosdeaxelis.client.SetArmorModels.get(current, slot, original);
            }
        });
    }

    public GearSet set() {
        return set;
    }

    public ArmorDefs.Piece piece() {
        return piece;
    }

    @Override
    public Tier tier() {
        return ArmorDefs.tier(set);
    }

    @Override
    public Component getName(ItemStack stack) {
        return com.mundosdeaxelis.crate.Tiers.shown(this).paint(super.getName(stack));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tier().ordinal() >= Tier.EPIC.ordinal() || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.tier", tier().label()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.set",
                Component.translatable("set.mundos_de_axelis." + set.id).withStyle(Style.EMPTY.withColor(set.color)))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.armor_set.title",
                Component.translatable("armorset.mundos_de_axelis." + set.id + ".name")).withStyle(ChatFormatting.AQUA));
        float red = ArmorDefs.damageReduction(set);
        if (red > 0.0F) {
            tooltip.add(Component.translatable("tooltip.mundos_de_axelis.armor_reduction", String.format("%.1f", red * 25.0F),
                    String.format("%.0f", red * 100.0F)).withStyle(ChatFormatting.BLUE));
        }
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.armor_souls", 2 + set.rank).withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("armorset.mundos_de_axelis." + set.id + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("armorset.mundos_de_axelis." + set.id + ".lore")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
