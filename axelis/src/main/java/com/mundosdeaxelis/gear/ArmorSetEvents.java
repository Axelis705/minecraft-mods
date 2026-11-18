package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Bonus de set: se activan al llevar las cuatro piezas del mismo set. Son suaves a propósito (la protección ya sube con la caja):
 * <ul>
 * <li>Naturaleza: +1 corazón y regeneración lenta sobre hierba, tierra u hojas.</li>
 * <li>Jardín de Pétalos (Naturaleza B): +1 corazón, se quita el veneno y regeneración lenta sobre hierba, tierra u hojas.</li>
 * <li>Forja: las piezas se reparan solas y empujan menos.</li>
 * <li>Hielo: sin congelación y quien te golpea cuerpo a cuerpo se ralentiza.</li>
 * <li>Fuego: inmune al fuego y a la lava; quien te golpea cuerpo a cuerpo arde.</li>
 * <li>Magia: +25 % de experiencia y -30 % de daño mágico.</li>
 * <li>Dragón: +2 corazones y +1,5 de daño.</li>
 * <li>Tormenta: +12 % de velocidad y sin daño de caída.</li>
 * <li>Sombra: visión nocturna y 8 % de esquivar un golpe.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class ArmorSetEvents {
    private ArmorSetEvents() {
    }

    private static final UUID HEALTH_ID = UUID.fromString("2f6b1a3e-91d4-4c07-8a52-0b7e3d5c9a11");
    private static final UUID KB_ID = UUID.fromString("7c0d9e24-5b18-4a6f-b3c1-4e8a2f6d1b22");
    private static final UUID DMG_ID = UUID.fromString("a41e7b95-0c3d-42f8-9e66-d15b8c2a7f33");
    private static final UUID SPEED_ID = UUID.fromString("c8352f10-e7a9-4b4d-85c0-39f1b6d4a244");

    private static final GearSet[] NONE = new GearSet[0];

    /** El set que lleva puesto el jugador completo (las cuatro piezas), o null. */
    public static GearSet worn(LivingEntity e) {
        GearSet set = null;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = e.getItemBySlot(slot);
            if (!(s.getItem() instanceof SetArmor a) || a.piece().slot != slot) {
                return null;
            }
            if (set == null) {
                set = a.set();
            } else if (set != a.set()) {
                return null;
            }
        }
        return set;
    }

    private static void mod(LivingEntity e, Attribute attr, UUID id, String name, double amount, AttributeModifier.Operation op, boolean on) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) {
            return;
        }
        AttributeModifier cur = inst.getModifier(id);
        if (!on) {
            if (cur != null) {
                inst.removeModifier(id);
            }
            return;
        }
        if (cur != null && Math.abs(cur.getAmount() - amount) < 0.0001D) {
            return;
        }
        if (cur != null) {
            inst.removeModifier(id);
        }
        boolean isHp = attr == Attributes.MAX_HEALTH && cur == null && amount > 0;
        float hpBefore = e.getHealth();
        float maxBefore = e.getMaxHealth();
        inst.addTransientModifier(new AttributeModifier(id, name, amount, op));
        if (isHp) {
            // Vida con el set puesto, sin curas gratis:
            //  - al volver a entrar al mundo se recupera la vida que tenías al salir (el juego la recorta a 10 corazones al cargar
            //    porque el modificador no se guarda);
            //  - si estabas con la vida llena, los corazones nuevos llegan llenos;
            //  - si estabas herido, sigues igual de herido (ya no te curas quitándote y poniéndote el set).
            float target = hpBefore;
            if (e instanceof ServerPlayer sp && sp.getPersistentData().getBoolean(HP_LOGIN)) {
                target = Math.max(hpBefore, sp.getPersistentData().getFloat(HP_SAVED));
            } else if (hpBefore >= maxBefore - 0.01F) {
                target = hpBefore + (float) amount;
            }
            e.setHealth(Math.min(target, e.getMaxHealth()));
        }
    }

    /** Vida guardada al salir del mundo / bandera de «acaba de entrar» (para devolver los corazones extra tal cual estaban). */
    private static final String HP_SAVED = "mdc_hp_saved";
    private static final String HP_LOGIN = "mdc_hp_login";

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && sp.getPersistentData().contains(HP_SAVED)) {
            sp.getPersistentData().putBoolean(HP_LOGIN, true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && sp.isAlive() && !sp.getPersistentData().getBoolean(HP_LOGIN)) {
            sp.getPersistentData().putFloat(HP_SAVED, sp.getHealth());
        }
    }

    /** Al morir no se arrastra la vida guardada: el nuevo cuerpo empieza con la vida llena. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (e.isWasDeath()) {
            e.getEntity().getPersistentData().remove(HP_SAVED);
            e.getEntity().getPersistentData().remove(HP_LOGIN);
        }
    }

    /** Texto de diagnóstico (comando /corazon vida). */
    public static java.util.List<String> debugHealth(ServerPlayer sp) {
        java.util.List<String> out = new java.util.ArrayList<>();
        GearSet set = worn(sp);
        out.add("Set completo puesto: " + (set == null ? "ninguno" : set.name()));
        AttributeInstance inst = sp.getAttribute(Attributes.MAX_HEALTH);
        out.add(String.format("Vida: %.1f / %.1f  (base %.1f)  Absorción: %.1f", sp.getHealth(), sp.getMaxHealth(), inst == null ? 0.0D : inst.getBaseValue(), sp.getAbsorptionAmount()));
        if (inst != null) {
            for (AttributeModifier m : inst.getModifiers()) {
                out.add("  modificador de vida máxima: " + m.getName() + " " + (m.getAmount() >= 0 ? "+" : "") + m.getAmount() + " (" + m.getOperation() + ")");
            }
        }
        for (MobEffectInstance ef : sp.getActiveEffects()) {
            String id = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKey(ef.getEffect()).toString();
            if (ef.getEffect() == MobEffects.HEALTH_BOOST || ef.getEffect() == MobEffects.ABSORPTION || ef.getEffect() == MobEffects.REGENERATION) {
                out.add("  efecto " + id + " nivel " + (ef.getAmplifier() + 1) + " quedan " + ef.getDuration() / 20 + " s");
            }
        }
        out.add("Vida guardada al salir: " + (sp.getPersistentData().contains(HP_SAVED) ? String.valueOf(sp.getPersistentData().getFloat(HP_SAVED)) : "-"));
        return out;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp) || !sp.isAlive()) {
            return;
        }
        long t = sp.level().getGameTime() + sp.getId();
        if (t % 10 != 0) {
            return;
        }
        GearSet set = worn(sp);
        mod(sp, Attributes.MAX_HEALTH, HEALTH_ID, "mdc_set_health", set == GearSet.FOREST ? 2.0D : set == GearSet.BLOOM ? 3.0D : set == GearSet.RUBY ? 4.0D : set == GearSet.GOLDEN ? 6.0D : 8.0D,
                AttributeModifier.Operation.ADDITION, set == GearSet.FOREST || set == GearSet.BLOOM || set == GearSet.RUBY || set == GearSet.GOLDEN || set == GearSet.COSMIC);
        mod(sp, Attributes.KNOCKBACK_RESISTANCE, KB_ID, "mdc_set_kb", set == GearSet.BRONZE ? 0.15D : 0.1D, AttributeModifier.Operation.ADDITION, set == GearSet.FORGE || set == GearSet.BRONZE);
        mod(sp, Attributes.ATTACK_DAMAGE, DMG_ID, "mdc_set_dmg", set == GearSet.GOLDEN ? 2.5D : 1.5D, AttributeModifier.Operation.ADDITION, set == GearSet.RUBY || set == GearSet.GOLDEN);
        mod(sp, Attributes.MOVEMENT_SPEED, SPEED_ID, "mdc_set_speed", set == GearSet.PEGASUS ? 0.15D : 0.12D, AttributeModifier.Operation.MULTIPLY_BASE, set == GearSet.STORM || set == GearSet.PEGASUS);
        if (sp.getHealth() > sp.getMaxHealth()) {
            sp.setHealth(sp.getMaxHealth());
        }
        sp.getPersistentData().remove(HP_LOGIN);
        sp.getPersistentData().putFloat(HP_SAVED, sp.getHealth());
        if (set == null) {
            return;
        }
        ServerLevel sl = sp.serverLevel();
        switch (set) {
            case BLOOM -> {
                if (t % 20 == 0 && sp.hasEffect(MobEffects.POISON)) {
                    sp.removeEffect(MobEffects.POISON);
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, sp.getX(), sp.getY() + 1.2D, sp.getZ(), 8, 0.4D, 0.5D, 0.4D, 0.02D);
                }
                if (t % 30 == 0 && sp.getHealth() < sp.getMaxHealth()) {
                    BlockPos below = sp.blockPosition().below();
                    BlockState st = sl.getBlockState(below);
                    if (st.is(BlockTags.DIRT) || st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS) || sl.getBlockState(sp.blockPosition()).is(BlockTags.LEAVES)) {
                        sp.heal(1.0F);
                        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, sp.getX(), sp.getY() + 1.0D, sp.getZ(), 4, 0.3D, 0.4D, 0.3D, 0.0D);
                    }
                }
            }
            case FOREST -> {
                if (t % 40 == 0 && sp.getHealth() < sp.getMaxHealth()) {
                    BlockPos below = sp.blockPosition().below();
                    BlockState st = sl.getBlockState(below);
                    if (st.is(BlockTags.DIRT) || st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS) || sl.getBlockState(sp.blockPosition()).is(BlockTags.LEAVES)) {
                        sp.heal(1.0F);
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, sp.getX(), sp.getY() + 1.0D, sp.getZ(), 3, 0.3D, 0.4D, 0.3D, 0.0D);
                    }
                }
            }
            case FORGE -> {
                if (t % 160 == 0) {
                    for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                        ItemStack s = sp.getItemBySlot(slot);
                        if (s.isDamaged()) {
                            s.setDamageValue(s.getDamageValue() - 1);
                        }
                    }
                }
            }
            case BRONZE -> {
                if (t % 80 == 0) {
                    for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND}) {
                        ItemStack s = sp.getItemBySlot(slot);
                        if (s.isDamaged()) {
                            s.setDamageValue(s.getDamageValue() - 1);
                        }
                    }
                }
            }
            case AURORA -> {
                if (t % 60 == 0 && sp.getHealth() < sp.getMaxHealth()) {
                    BlockPos here = sp.blockPosition();
                    BlockState below = sl.getBlockState(here.below());
                    BlockState at = sl.getBlockState(here);
                    if (below.is(BlockTags.SNOW) || below.is(BlockTags.ICE) || at.is(BlockTags.SNOW) || sl.getBiome(here).value().coldEnoughToSnow(here)) {
                        sp.heal(1.0F);
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, sp.getX(), sp.getY() + 1.0D, sp.getZ(), 4, 0.3D, 0.5D, 0.3D, 0.0D);
                    }
                }
            }
            case MAGMA, OBSIDIAN, RUBY, GOLDEN -> {
                if (t % 20 == 0) {
                    sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false, true));
                }
            }
            case MOON -> {
                long dayTime = sl.getDayTime() % 24000L;
                if (t % 40 == 0 && !sl.dimensionType().hasFixedTime() && dayTime >= 13000L && dayTime <= 23000L && sl.canSeeSky(sp.blockPosition())) {
                    sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, true, false, true));
                    sl.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1.2D, sp.getZ(), 3, 0.3D, 0.5D, 0.3D, 0.0D);
                }
            }
            case PEGASUS -> {
                // Aliento del pegaso: salto mejorado y, si caes desde alto, caída lenta.
                if (t % 20 == 0) {
                    sp.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 1, true, false, true));
                    if (!sp.onGround() && sp.fallDistance > 3.0F) {
                        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false, true));
                        sl.sendParticles(ParticleTypes.CLOUD, sp.getX(), sp.getY() + 0.2D, sp.getZ(), 6, 0.3D, 0.1D, 0.3D, 0.02D);
                    }
                }
            }
            case COSMIC -> {
                // Latido del cosmos: inmune al fuego, visión nocturna y Resistencia I mientras lo llevas puesto.
                if (t % 20 == 0) {
                    sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
                    sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false, true));
                }
                if (t % 60 == 0) {
                    sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
                }
            }
            case SHADOW -> {
                if (t % 60 == 0) {
                    sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
                }
            }
            default -> {
            }
        }
    }

    /** Hielo y Aurora: sin daño de congelación. Sombra (10 %) y Cosmos (12 %): esquivar un golpe de un enemigo. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.level().isClientSide) {
            return;
        }
        GearSet set = worn(sp);
        if ((set == GearSet.ICE || set == GearSet.AURORA) && e.getSource().is(DamageTypes.FREEZE)) {
            e.setCanceled(true);
        } else if ((set == GearSet.SHADOW || set == GearSet.COSMIC) && e.getSource().getEntity() instanceof LivingEntity
                && sp.getRandom().nextFloat() < (set == GearSet.COSMIC ? 0.12F : 0.10F)) {
            e.setCanceled(true);
            ServerLevel sl = sp.serverLevel();
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, sp.getX(), sp.getY() + 1.0D, sp.getZ(), 14, 0.3D, 0.6D, 0.3D, 0.02D);
            sl.playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.6F);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || sp.level().isClientSide) {
            return;
        }
        // reducción de daño extra por pieza (cuenta aunque no lleves el set completo); no afecta al vacío ni a /kill
        if (!e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            float red = 0.0F;
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                if (sp.getItemBySlot(slot).getItem() instanceof SetArmor a && a.piece().slot == slot) {
                    red += ArmorDefs.damageReduction(a.set()) / 4.0F;
                }
            }
            if (red > 0.0F) {
                e.setAmount(e.getAmount() * (1.0F - Math.min(red, 0.4F)));
            }
            // los puntos de armadura por encima de 30 (que el mod ya no recorta) dan 0,5 % más de reducción cada uno, hasta 15 %
            // (la fórmula de Minecraft solo cuenta 20 puntos efectivos y esos ya se alcanzan con 30 contra golpes normales)
            int armorPts = sp.getArmorValue();
            if (armorPts > 30) {
                e.setAmount(e.getAmount() * (1.0F - Math.min(0.15F, (armorPts - 30) * 0.005F)));
            }
        }
        GearSet set = worn(sp);
        if (set == null) {
            return;
        }
        DamageSource src = e.getSource();
        boolean melee = src.getEntity() instanceof LivingEntity && src.getDirectEntity() == src.getEntity();
        switch (set) {
            case ICE -> {
                if (melee) {
                    ((LivingEntity) src.getEntity()).addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
            }
            case AURORA -> {
                if (melee) {
                    ((LivingEntity) src.getEntity()).addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                }
            }
            case MAGMA -> {
                if (melee) {
                    src.getEntity().setSecondsOnFire(4);
                }
            }
            case OBSIDIAN -> {
                if (melee) {
                    src.getEntity().setSecondsOnFire(6);
                }
                if (src.is(DamageTypes.EXPLOSION) || src.is(DamageTypes.PLAYER_EXPLOSION)) {
                    e.setAmount(e.getAmount() * 0.65F);
                }
                if (melee && sp.getRandom().nextFloat() < 0.3F) {
                    ServerLevel sl = sp.serverLevel();
                    for (LivingEntity near : sl.getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(3.0D),
                            x -> x != sp && x.isAlive() && !(x instanceof Player))) {
                        near.setSecondsOnFire(3);
                    }
                    sl.sendParticles(ParticleTypes.FLAME, sp.getX(), sp.getY() + 1.0D, sp.getZ(), 18, 0.5D, 0.6D, 0.5D, 0.05D);
                    sl.sendParticles(ParticleTypes.ASH, sp.getX(), sp.getY() + 1.0D, sp.getZ(), 14, 0.6D, 0.6D, 0.6D, 0.02D);
                }
            }
            case MAGIC -> {
                if (src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC)) {
                    e.setAmount(e.getAmount() * 0.7F);
                }
            }
            case MOON -> {
                if (src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC)) {
                    e.setAmount(e.getAmount() * 0.6F);
                }
            }
            case GOLDEN -> {
                // Furia dorada: al quedar por debajo del 30 % de vida, fuerza y absorción durante 10 s (se recarga en 2 minutos).
                long now = sp.level().getGameTime();
                if (sp.getHealth() - e.getAmount() < sp.getMaxHealth() * 0.3F && now >= sp.getPersistentData().getLong("mdc_golden_fury")) {
                    sp.getPersistentData().putLong("mdc_golden_fury", now + 2400L);
                    sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, true, true));
                    sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1, false, true, true));
                    ServerLevel sl = sp.serverLevel();
                    sl.sendParticles(ParticleTypes.FLAME, sp.getX(), sp.getY() + 1.0D, sp.getZ(), 24, 0.5D, 0.8D, 0.5D, 0.06D);
                    sl.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1.0D, sp.getZ(), 10, 0.4D, 0.8D, 0.4D, 0.05D);
                    sl.playSound(null, sp.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.4F, 1.5F);
                }
            }
            default -> {
            }
        }
    }

    /** Magia: +25 % de experiencia (Luz de luna: +35 %). */
    @SubscribeEvent
    public static void onXp(PlayerXpEvent.XpChange e) {
        if (e.getEntity() instanceof ServerPlayer sp && e.getAmount() > 0 && (worn(sp) == GearSet.MAGIC || worn(sp) == GearSet.MOON)) {
            float k = worn(sp) == GearSet.MOON ? 1.35F : 1.25F;
            e.setAmount(Math.max(e.getAmount() + 1, Math.round(e.getAmount() * k)));
        }
    }

    /** Tormenta: sin daño de caída. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (e.getEntity() instanceof Player p && !p.level().isClientSide) {
            GearSet w = worn(p);
            if (w == GearSet.STORM || w == GearSet.PEGASUS) {
                e.setDamageMultiplier(0.0F);
            } else if (w == GearSet.MOON) {
                e.setDamageMultiplier(e.getDamageMultiplier() * 0.5F);
            }
        }
    }
}
