package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Figurita de un compañero: al usarla invoca a su criatura en 3D. Equipada solo da armadura. */
public class CompanionItem extends RelicItem {
    private final Companions.Spec spec;

    public CompanionItem(Companions.Spec spec) {
        super(spec.id(), spec.rgb(), 20 * spec.cdSecs(), spec.rarity());
        this.spec = spec;
    }

    public Companions.Spec spec() {
        return spec;
    }

    @Override
    protected int interval() {
        return 40;
    }

    @Override
    public boolean isPet() {
        return true;
    }

    /** Las mascotas equipadas te acompañan solas: no tienen tecla ni acción (solo su pasiva). */
    @Override
    public Kind kind() {
        return Kind.KEY;
    }

    /**
     * Las mascotas ya no dan efectos: solo armadura (1 punto cada una; 2 la estrella de su caja). Aquí solo queda el destello
     * de adorno.
     */
    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (time % 120 == 0) {
            Vec3 h = RelicFx.hand(sp);
            sl.sendParticles(Fx.spark(spec.rgb(), 0.4F), h.x, h.y + 0.1, h.z, 2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    /** Sin habilidad activa: la mascota equipada solo te acompaña y da armadura. */
    @Override
    protected boolean hasActive() {
        return false;
    }

    /**
     * Etapa 254: lo que la figurita cuenta de su mascota-criatura: su nombre, su cariño, si pelea y cómo, y qué hace en su menú.
     */
    static void petLines(Companions.Spec spec, ItemStack stack, java.util.List<net.minecraft.network.chat.Component> tip) {
        PetKinds.Kind k = PetKinds.of(spec.species());
        if (k == null) {
            return;
        }
        String g = "tooltip.mundos_de_axelis.pet.";
        String name = PetData.name(stack);
        if (!name.isEmpty()) {
            tip.add(net.minecraft.network.chat.Component.translatable(g + "name", name).withStyle(net.minecraft.ChatFormatting.WHITE));
        }
        int love = PetData.love(stack);
        StringBuilder h = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            h.append(love >= (i + 1) * 20 ? "\u2665" : "\u2661");
        }
        tip.add(net.minecraft.network.chat.Component.translatable(g + "love", h.toString()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        if (k.combat()) {
            tip.add(net.minecraft.network.chat.Component.translatable("gui.mundos_de_axelis.pet.fight." + k.attack.name().toLowerCase(java.util.Locale.ROOT),
                    net.minecraft.network.chat.Component.translatable("gui.mundos_de_axelis.pet.element." + k.element.name().toLowerCase(java.util.Locale.ROOT)))
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
        } else {
            tip.add(net.minecraft.network.chat.Component.translatable("gui.mundos_de_axelis.pet.fight.none").withStyle(net.minecraft.ChatFormatting.GREEN));
        }
        if (k.special != PetKinds.Special.NONE) {
            tip.add(net.minecraft.network.chat.Component.translatable(g + "special",
                    net.minecraft.network.chat.Component.translatable("gui.mundos_de_axelis.pet.special." + k.specialKey())).withStyle(net.minecraft.ChatFormatting.AQUA));
        }
        if (k.special == PetKinds.Special.BAG && PetData.bagCount(stack) > 0) {
            tip.add(net.minecraft.network.chat.Component.translatable(g + "bag_count", PetData.bagCount(stack), k.bag).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        tip.add(net.minecraft.network.chat.Component.translatable(g + "how").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Companions.summon(sp, spec, power(stack));
        return true;
    }
}
