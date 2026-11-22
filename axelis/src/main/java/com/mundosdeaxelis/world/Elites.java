package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * Etapa 275: <b>enemigos de élite</b> (como los campeones de Diablo 4). Algunos enemigos de las estructuras (uno de cada nueve,
 * más o menos) salen como élite: llevan su nombre a la vista con un apodo de color, un aura, el doble y medio de vida, pegan más
 * y tienen un poder. Al morir sueltan un pequeño botín de su tier (si el jugador ya puede saquearlo) y el triple de experiencia.
 * <ul>
 *     <li><b>Ardiente</b>: sus golpes queman.</li>
 *     <li><b>Gélido</b>: sus golpes congelan y ralentizan.</li>
 *     <li><b>Vampírico</b>: se cura con lo que te quita.</li>
 *     <li><b>Blindado</b>: con más armadura, casi no retrocede y las flechas y arrojadizos le hacen la mitad.</li>
 *     <li><b>Veloz</b>: corre mucho más.</li>
 *     <li><b>Explosivo</b>: al morir, avisa y estalla un segundo después: ¡apártate!</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Elites {
    private Elites() {
    }

    public enum Afijo {
        ARDIENTE("ardiente", 0xFF7A2A, ParticleTypes.FLAME),
        GELIDO("gelido", 0x9FE6FF, ParticleTypes.SNOWFLAKE),
        VAMPIRICO("vampirico", 0xE0304E, ParticleTypes.CRIMSON_SPORE),
        BLINDADO("blindado", 0xC9D1D9, ParticleTypes.WAX_OFF),
        VELOZ("veloz", 0x8CF6A0, ParticleTypes.CLOUD),
        EXPLOSIVO("explosivo", 0xFFC93C, ParticleTypes.SMOKE);

        public final String id;
        public final int color;
        public final ParticleOptions aura;

        Afijo(String id, int color, ParticleOptions aura) {
            this.id = id;
            this.color = color;
            this.aura = aura;
        }

        @Nullable
        public static Afijo of(String id) {
            for (Afijo a : values()) {
                if (a.id.equals(id)) {
                    return a;
                }
            }
            return null;
        }
    }

    public static final String KEY = "mdc_elite", KEY_TIER = "mdc_elite_tier";
    /** Probabilidad (en %) de que un enemigo de estructura salga como élite. */
    public static final int CHANCE = 11;
    private static final UUID HP = UUID.fromString("e1a7e000-2750-4d1e-9f00-000000000001");
    private static final UUID DMG = UUID.fromString("e1a7e000-2750-4d1e-9f00-000000000002");
    private static final UUID SPEED = UUID.fromString("e1a7e000-2750-4d1e-9f00-000000000003");
    private static final UUID ARMOR = UUID.fromString("e1a7e000-2750-4d1e-9f00-000000000004");
    private static final UUID KNOCK = UUID.fromString("e1a7e000-2750-4d1e-9f00-000000000005");

    /** Tier de cada enemigo común que puede salir como élite (los jefes, mini-jefes y el mímico no). */
    private static final Map<String, Integer> TIER = Map.ofEntries(
            Map.entry("brote", 1), Map.entry("espinoso", 1), Map.entry("raizal", 1), Map.entry("polinizador", 1),
            Map.entry("escoria", 2), Map.entry("chispero", 2), Map.entry("ascua", 2),
            Map.entry("lobo_escarcha", 3), Map.entry("centinela_escarcha", 3),
            Map.entry("salamandra_lava", 4), Map.entry("diablillo", 4), Map.entry("cultista_volcan", 4), Map.entry("larva_magma", 4),
            Map.entry("esbirro", 5), Map.entry("libro_embrujado", 5), Map.entry("armadura_encantada", 5));

    /** Los enemigos comunes de un tier (los nombres de sus entidades), para quien los necesite (el Altar Maldito). */
    public static java.util.List<String> mobsOfTier(int tier) {
        java.util.List<String> out = new java.util.ArrayList<>();
        int t = Math.max(1, Math.min(5, tier));
        for (Map.Entry<String, Integer> e : TIER.entrySet()) {
            if (e.getValue() == t) {
                out.add(e.getKey());
            }
        }
        java.util.Collections.sort(out);
        return out;
    }

    /** Tier del enemigo si puede ser élite; 0 si no. */
    public static int eligibleTier(Entity e) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        if (id == null || !id.getNamespace().equals(MundosDeAxelis.MODID)) {
            return 0;
        }
        return TIER.getOrDefault(id.getPath(), 0);
    }

    @Nullable
    public static Afijo afijo(Entity e) {
        CompoundTag d = e.getPersistentData();
        return d.contains(KEY) ? Afijo.of(d.getString(KEY)) : null;
    }

    public static boolean isElite(Entity e) {
        return e.getPersistentData().contains(KEY);
    }

    /** Al poner los enemigos de una estructura: a veces sale uno de élite. */
    public static void maybe(Mob m, RandomSource r) {
        int t = eligibleTier(m);
        if (t > 0 && r.nextInt(100) < CHANCE) {
            make(m, t, Afijo.values()[r.nextInt(Afijo.values().length)]);
        }
    }

    /** Lo hace élite con ese poder (si ya lo era, no hace nada). */
    public static void make(Mob m, int tier, Afijo a) {
        if (isElite(m)) {
            return;
        }
        m.getPersistentData().putString(KEY, a.id);
        m.getPersistentData().putInt(KEY_TIER, tier);
        apply(m, a);
    }

    /**
     * Le pone la vida, el daño, el poder y el nombre (una vez). También sirve para los que se crean a mano con
     * {@code /summon … {ForgeData:{mdc_elite:"ardiente",mdc_elite_tier:2}}}: se aplica en su primer tick.
     */
    private static void apply(Mob m, Afijo a) {
        if (m.getPersistentData().getBoolean(KEY + "_ok")) {
            return;
        }
        m.getPersistentData().putBoolean(KEY + "_ok", true);
        if (!m.getPersistentData().contains(KEY_TIER)) {
            m.getPersistentData().putInt(KEY_TIER, Math.max(1, eligibleTier(m)));
        }
        add(m.getAttribute(Attributes.MAX_HEALTH), HP, 1.5D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        add(m.getAttribute(Attributes.ATTACK_DAMAGE), DMG, 0.3D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        if (a == Afijo.VELOZ) {
            add(m.getAttribute(Attributes.MOVEMENT_SPEED), SPEED, 0.45D, AttributeModifier.Operation.MULTIPLY_BASE);
        }
        if (a == Afijo.BLINDADO) {
            add(m.getAttribute(Attributes.ARMOR), ARMOR, 8.0D, AttributeModifier.Operation.ADDITION);
            add(m.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCK, 0.7D, AttributeModifier.Operation.ADDITION);
        }
        m.setHealth(m.getMaxHealth());
        m.setCustomName(name(m, a));
        m.setCustomNameVisible(true);
        m.setPersistenceRequired();
    }

    private static void add(@Nullable AttributeInstance at, UUID id, double v, AttributeModifier.Operation op) {
        if (at != null && at.getModifier(id) == null) {
            at.addPermanentModifier(new AttributeModifier(id, "Élite", v, op));
        }
    }

    public static MutableComponent name(Entity m, Afijo a) {
        return Component.translatable("elite.mundos_de_axelis.nombre", m.getType().getDescription(),
                Component.translatable("elite.mundos_de_axelis." + a.id)).withStyle(s -> s.withColor(a.color).withBold(true));
    }

    // ------------------------------------------------------------------ poderes

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        // el élite golpea
        if (e.getSource().getEntity() instanceof Mob att && e.getSource().getDirectEntity() == att) {
            Afijo a = afijo(att);
            if (a != null) {
                switch (a) {
                    case ARDIENTE -> victim.setSecondsOnFire(3);
                    case GELIDO -> {
                        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), att);
                        victim.setTicksFrozen(Math.max(victim.getTicksFrozen(), 160));
                    }
                    case VAMPIRICO -> {
                        att.heal(e.getAmount() * 0.4F);
                        if (att.level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.HEART, att.getX(), att.getY() + att.getBbHeight() + 0.2, att.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        // al élite le pegan
        Afijo va = afijo(victim);
        if (va == Afijo.BLINDADO && e.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            e.setAmount(e.getAmount() * 0.5F);
        }
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent e) {
        LivingEntity le = e.getEntity();
        if (le.tickCount % 12 != 0 || !(le.level() instanceof ServerLevel sl)) {
            return;
        }
        Afijo a = afijo(le);
        if (a == null || !le.isAlive()) {
            return;
        }
        if (le instanceof Mob m) {
            apply(m, a);
        }
        sl.sendParticles(a.aura, le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ(), 3, le.getBbWidth() * 0.5, le.getBbHeight() * 0.4,
                le.getBbWidth() * 0.5, 0.01);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity le = e.getEntity();
        if (!(le.level() instanceof ServerLevel sl) || afijo(le) != Afijo.EXPLOSIVO) {
            return;
        }
        double x = le.getX(), y = le.getY() + 0.5, z = le.getZ();
        sl.playSound(null, x, y, z, SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.2F);
        sl.sendParticles(ParticleTypes.SMOKE, x, y, z, 20, 0.3, 0.3, 0.3, 0.02);
        float dmg = le.getAttribute(Attributes.ATTACK_DAMAGE) != null ? (float) le.getAttributeValue(Attributes.ATTACK_DAMAGE) : 6.0F;
        Scheduler.runLater(25, () -> {
            sl.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.1F);
            sl.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 3, 0.5, 0.3, 0.5, 0.0);
            sl.sendParticles(ParticleTypes.FLAME, x, y, z, 20, 0.8, 0.4, 0.8, 0.05);
            for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(x - 3, y - 2, z - 3, x + 3, y + 3, z + 3))) {
                if (!p.isCreative() && !p.isSpectator() && p.distanceToSqr(x, y, z) < 9.0D) {
                    p.hurt(sl.damageSources().explosion(null, null), dmg * 0.9F);
                }
            }
        });
    }

    // ------------------------------------------------------------------ botín

    /** El botín de un élite de ese tier: siempre una Runa de Esencia; a veces arrojadizos, esencia, fragmento de llave, gema vacía o corazón. */
    public static java.util.List<ItemStack> loot(int tier, RandomSource r) {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        out.add(new ItemStack(ModItems.ESSENCE_RUNES.get(Math.max(1, Math.min(8, tier))).get()));
        if (r.nextInt(100) < 40) {
            out.add(new ItemStack(ModItems.ARROJADIZOS.get(com.mundosdeaxelis.arrojadizo.Arrojadizos.Tipo.ofTier(tier)).get(), 1 + r.nextInt(2)));
        }
        if (r.nextInt(100) < 35) {
            out.add(new ItemStack(ModItems.ESENCIAS.get(com.mundosdeaxelis.gema.Gemas.Elemento.ofTier(tier)).get()));
        }
        if (r.nextInt(100) < 30) {
            out.add(new ItemStack(ModItems.KEY_FRAGMENTS.get(com.mundosdeaxelis.crate.CrateKind.byIndex(tier)).get()));
        }
        int s = r.nextInt(100);
        if (s < 7) {
            out.add(new ItemStack(ModItems.HEART_FRAGMENT.get()));
        } else if (s < 25) {
            out.add(com.mundosdeaxelis.block.VaultLoot.emptyGem(tier));
        }
        return out;
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        LivingEntity le = e.getEntity();
        if (!(le.level() instanceof ServerLevel sl) || !isElite(le)) {
            return;
        }
        ServerPlayer sp = e.getSource().getEntity() instanceof ServerPlayer p ? p : null;
        if (sp == null) {
            return;
        }
        int tier = le.getPersistentData().getInt(KEY_TIER);
        if (!Sellos.canLoot(sp, tier)) {
            return;
        }
        for (ItemStack it : loot(tier, le.getRandom())) {
            e.getDrops().add(new ItemEntity(sl, le.getX(), le.getY() + 0.5, le.getZ(), it));
        }
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.elite.vencido", le.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
        sl.playSound(null, le.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 0.6F, 1.4F);
    }

    @SubscribeEvent
    public static void onXp(LivingExperienceDropEvent e) {
        if (isElite(e.getEntity())) {
            e.setDroppedExperience(e.getDroppedExperience() * 3);
        }
    }
}
