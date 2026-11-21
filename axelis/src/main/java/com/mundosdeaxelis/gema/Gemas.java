package com.mundosdeaxelis.gema;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Etapa 271: las gemas (como en Diablo 4, con la base de Apotheosis). En los cofres salen, con poca probabilidad, <b>gemas
 * vacías</b> (su pureza depende del tier: Agrietada, Tallada, Pulida o Perfecta) y <b>esencias de infusión</b> de cada tier. En la
 * Mesa de Gemas, una gema vacía + una esencia = una gema de ese elemento, que se <b>engarza</b> en un arma (espadas, hachas y
 * tridentes; un engarce por arma). Al golpear, la gema hace su efecto, más fuerte cuanto más pura:
 * <ul>
 *     <li>Veneno (bosque): envenena; Filo (forja): más daño; Escarcha (hielo): ralentiza; Fuego (volcán): prende fuego;</li>
 *     <li>Arcano (magia): daño arcano extra; Vampiro (dragón): te cura; Rayo (cielo): a veces cae un rayo; Vacío (abismo): marchita.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Gemas {
    private Gemas() {
    }

    public enum Elemento {
        VENENO("veneno", 1, 0x6BD84A), FILO("filo", 2, 0xD8E0E8), ESCARCHA("escarcha", 3, 0x8FDFFF), FUEGO("fuego", 4, 0xFF7A2A),
        ARCANO("arcano", 5, 0xC070FF), VAMPIRO("vampiro", 6, 0xFF3A5E), RAYO("rayo", 7, 0xFFE45A), VACIO("vacio", 8, 0x9A5CFF);

        public final String id;
        public final int tier;
        public final int color;

        Elemento(String id, int tier, int color) {
            this.id = id;
            this.tier = tier;
            this.color = color;
        }

        public Style style() {
            return Style.EMPTY.withColor(TextColor.fromRgb(color));
        }

        public static Elemento of(String id) {
            for (Elemento e : values()) {
                if (e.id.equals(id)) {
                    return e;
                }
            }
            return null;
        }

        public static Elemento ofTier(int tier) {
            return values()[Math.max(0, Math.min(7, tier - 1))];
        }
    }

    /** Las cuatro purezas (1 a 4). */
    public static final String[] PUREZA = {"", "agrietada", "tallada", "pulida", "perfecta"};
    private static final String TAG = "MdcGema";

    /** Pureza de las gemas de un tier: 1-2 Agrietada, 3-4 Tallada, 5-6 Pulida, 7-8 Perfecta. */
    public static int purezaDeTier(int tier) {
        return Math.max(1, Math.min(4, (tier + 1) / 2));
    }

    public static int pureza(ItemStack st) {
        return st.hasTag() ? Math.max(1, Math.min(4, st.getTag().getInt("Pureza"))) : 1;
    }

    public static ItemStack conPureza(ItemStack st, int p) {
        st.getOrCreateTag().putInt("Pureza", Math.max(1, Math.min(4, p)));
        return st;
    }

    public static Component purezaName(int p) {
        return Component.translatable("gema.mundos_de_axelis.pureza." + PUREZA[Math.max(1, Math.min(4, p))]);
    }

    /** ¿Admite gema? Armas cuerpo a cuerpo: espadas, hachas (las del mod también) y tridentes. */
    public static boolean admite(ItemStack st) {
        return st.getItem() instanceof SwordItem || st.getItem() instanceof AxeItem || st.getItem() instanceof TridentItem;
    }

    public static Elemento gemaDe(ItemStack weapon) {
        if (!weapon.hasTag() || !weapon.getTag().contains(TAG)) {
            return null;
        }
        return Elemento.of(weapon.getTag().getCompound(TAG).getString("E"));
    }

    public static int purezaDe(ItemStack weapon) {
        return weapon.hasTag() ? Math.max(1, Math.min(4, weapon.getTag().getCompound(TAG).getInt("P"))) : 1;
    }

    public static void engarzar(ItemStack weapon, Elemento e, int p) {
        CompoundTag t = new CompoundTag();
        t.putString("E", e.id);
        t.putInt("P", p);
        weapon.getOrCreateTag().put(TAG, t);
    }

    public static void quitar(ItemStack weapon) {
        if (weapon.hasTag()) {
            weapon.getTag().remove(TAG);
        }
    }

    // ------------------------------------------------------------------ efectos al golpear

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getSource().getDirectEntity() instanceof Player p) || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        ItemStack w = p.getMainHandItem();
        Elemento el = gemaDe(w);
        if (el == null) {
            return;
        }
        int pu = purezaDe(w);
        LivingEntity t = e.getEntity();
        double x = t.getX(), y = t.getY() + t.getBbHeight() * 0.6, z = t.getZ();
        switch (el) {
            case VENENO -> {
                t.addEffect(new MobEffectInstance(MobEffects.POISON, 40 + 30 * pu, pu >= 3 ? 1 : 0), p);
                sl.sendParticles(ParticleTypes.ITEM_SLIME, x, y, z, 6, 0.3, 0.3, 0.3, 0.05);
            }
            case FILO -> {
                e.setAmount(e.getAmount() * (1.0F + 0.07F * pu));
                sl.sendParticles(ParticleTypes.CRIT, x, y, z, 8, 0.3, 0.3, 0.3, 0.2);
            }
            case ESCARCHA -> {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 + 20 * pu, pu >= 3 ? 2 : 1), p);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 10, 0.3, 0.4, 0.3, 0.02);
            }
            case FUEGO -> {
                t.setSecondsOnFire(1 + pu);
                sl.sendParticles(ParticleTypes.FLAME, x, y, z, 10, 0.3, 0.4, 0.3, 0.02);
            }
            case ARCANO -> {
                e.setAmount(e.getAmount() + 0.8F * pu);
                sl.sendParticles(ParticleTypes.WITCH, x, y, z, 10, 0.3, 0.4, 0.3, 0.05);
            }
            case VAMPIRO -> {
                p.heal(Math.min(0.5F * pu, e.getAmount() * 0.2F));
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 4, 0.2, 0.2, 0.2, 0.05);
            }
            case RAYO -> {
                if (p.getRandom().nextInt(100) < 6 * pu) {
                    net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
                    if (bolt != null) {
                        bolt.moveTo(t.getX(), t.getY(), t.getZ());
                        bolt.setVisualOnly(true);
                        sl.addFreshEntity(bolt);
                    }
                    e.setAmount(e.getAmount() + 2.0F * pu);
                }
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 8, 0.3, 0.4, 0.3, 0.1);
            }
            case VACIO -> {
                t.addEffect(new MobEffectInstance(MobEffects.WITHER, 40 + 20 * pu, 0), p);
                sl.sendParticles(ParticleTypes.SQUID_INK, x, y, z, 6, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    // ------------------------------------------------------------------ la gema se ve en la descripción del arma

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent e) {
        Elemento el = gemaDe(e.getItemStack());
        if (el == null) {
            return;
        }
        int pu = purezaDe(e.getItemStack());
        e.getToolTip().add(Component.literal("◆ ").append(Component.translatable("item.mundos_de_axelis.gema_" + el.id))
                .append(" (").append(purezaName(pu)).append(")").withStyle(el.style()));
        e.getToolTip().add(Component.translatable("gema.mundos_de_axelis.efecto." + el.id).withStyle(ChatFormatting.GRAY));
    }
}
