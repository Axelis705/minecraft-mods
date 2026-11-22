package com.mundosdeaxelis.gema;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Etapa 277: <b>gemas en la armadura</b> (como en Apotheosis, la misma gema hace otra cosa en la armadura). Cada pieza tiene un
 * engarce; los efectos de todas las piezas se suman (por pureza: Agrietada 1 … Perfecta 4, hasta 16 con las 4 piezas):
 * <ul>
 *     <li><b>Veneno</b> — espinas venenosas: quien te golpea cuerpo a cuerpo queda envenenado.</li>
 *     <li><b>Filo</b> — espinas: le devuelves un 4 % por pureza del golpe.</li>
 *     <li><b>Escarcha</b> — coraza helada: quien te golpea queda lento.</li>
 *     <li><b>Fuego</b> — piel ígnea: −8 % de daño de fuego y lava por pureza (máximo −80 %).</li>
 *     <li><b>Arcano</b> — velo arcano: −8 % de daño mágico por pureza (máximo −80 %).</li>
 *     <li><b>Vampiro</b> — sangre viva: te curas 0,25 de vida por pureza cada 5 segundos.</li>
 *     <li><b>Rayo</b> — pies ligeros: +2,5 % de velocidad por pureza.</li>
 *     <li><b>Vacío</b> — vitalidad: +1 de vida máxima (medio corazón) por pureza.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class GemasArmadura {
    private GemasArmadura() {
    }

    private static final Map<EquipmentSlot, UUID> SPEED = new EnumMap<>(EquipmentSlot.class);
    private static final Map<EquipmentSlot, UUID> HEALTH = new EnumMap<>(EquipmentSlot.class);

    static {
        int i = 0;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            SPEED.put(s, UUID.fromString("6e6a2770-277a-4c0e-8d00-00000000001" + i));
            HEALTH.put(s, UUID.fromString("6e6a2770-277a-4c0e-8d00-00000000002" + i));
            i++;
        }
    }

    /** Suma de purezas de ese elemento en la armadura puesta. */
    public static int total(LivingEntity e, Gemas.Elemento el) {
        int n = 0;
        for (ItemStack st : e.getArmorSlots()) {
            if (Gemas.esArmadura(st) && Gemas.gemaDe(st) == el) {
                n += Gemas.purezaDe(st);
            }
        }
        return n;
    }

    /** Rayo y Vacío dan atributos (se ven en la descripción de la pieza, como cualquier armadura). */
    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent e) {
        ItemStack st = e.getItemStack();
        if (!(st.getItem() instanceof ArmorItem armor) || e.getSlotType() != armor.getEquipmentSlot()) {
            return;
        }
        Gemas.Elemento el = Gemas.gemaDe(st);
        if (el == null) {
            return;
        }
        int pu = Gemas.purezaDe(st);
        if (el == Gemas.Elemento.RAYO) {
            e.addModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(SPEED.get(e.getSlotType()), "Gema de Rayo", 0.025D * pu,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (el == Gemas.Elemento.VACIO) {
            e.addModifier(Attributes.MAX_HEALTH, new AttributeModifier(HEALTH.get(e.getSlotType()), "Gema del Vacío", 1.0D * pu,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    /** Te golpean: defensas y espinas. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof Player p) || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        int fire = total(p, Gemas.Elemento.FUEGO);
        if (fire > 0 && e.getSource().is(DamageTypeTags.IS_FIRE)) {
            e.setAmount(e.getAmount() * (1.0F - Math.min(0.8F, 0.08F * fire)));
        }
        int arc = total(p, Gemas.Elemento.ARCANO);
        if (arc > 0 && e.getSource().is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            e.setAmount(e.getAmount() * (1.0F - Math.min(0.8F, 0.08F * arc)));
        }
        // cuerpo a cuerpo: el que pega es el mismo que el golpe
        if (!(e.getSource().getEntity() instanceof LivingEntity att) || e.getSource().getDirectEntity() != att || att == p) {
            return;
        }
        int ven = total(p, Gemas.Elemento.VENENO);
        if (ven > 0) {
            att.addEffect(new MobEffectInstance(MobEffects.POISON, 40 + 10 * ven, ven >= 6 ? 1 : 0), p);
            sl.sendParticles(ParticleTypes.ITEM_SLIME, att.getX(), att.getY() + att.getBbHeight() * 0.6, att.getZ(), 5, 0.3, 0.3, 0.3, 0.05);
        }
        int filo = total(p, Gemas.Elemento.FILO);
        if (filo > 0 && !e.getSource().is(DamageTypeTags.AVOIDS_GUARDIAN_THORNS)) {
            att.hurt(sl.damageSources().thorns(p), e.getAmount() * 0.04F * filo);
            sl.sendParticles(ParticleTypes.CRIT, att.getX(), att.getY() + att.getBbHeight() * 0.6, att.getZ(), 6, 0.3, 0.3, 0.3, 0.2);
        }
        int esc = total(p, Gemas.Elemento.ESCARCHA);
        if (esc > 0) {
            att.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 + 10 * esc, esc >= 6 ? 1 : 0), p);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, att.getX(), att.getY() + att.getBbHeight() * 0.6, att.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        }
    }

    /** Vampiro: se cura poco a poco. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.level().isClientSide || e.player.tickCount % 100 != 0) {
            return;
        }
        Player p = e.player;
        if (p.isAlive() && p.getHealth() < p.getMaxHealth()) {
            int v = total(p, Gemas.Elemento.VAMPIRO);
            if (v > 0) {
                p.heal(0.25F * v);
            }
        }
    }
}
