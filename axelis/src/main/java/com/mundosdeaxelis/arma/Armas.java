package com.mundosdeaxelis.arma;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.gear.Gear;
import com.mundosdeaxelis.gear.Souls;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.relic.RelicFx;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cómo pelea cada arma nueva (etapa 281): el estilo de su tipo (pasivas), el elemento de su tier y la especial de clic derecho
 * que gasta almas. Todo se ejecuta en el servidor.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Armas {
    private Armas() {
    }

    /** Lo que se recuerda de cada jugador entre el clic de ataque y el golpe (y unos segundos después). */
    private static final class Estado {
        long golpeTick = -100;
        LivingEntity objetivo;
        boolean cargado;
        boolean corriendo;
        long contraHasta = -1;
        long sombraHasta = -1;
        /** Etapa 282: hasta cuándo no puede volver a estallar un golpe cargado. */
        long estallidoHasta = -1;
    }

    private static final Map<UUID, Estado> ESTADO = new HashMap<>();

    private static Estado estado(Player p) {
        return ESTADO.computeIfAbsent(p.getUUID(), k -> new Estado());
    }

    public static void clearRuntime() {
        ESTADO.clear();
    }

    public static boolean esArma(String id) {
        return id.startsWith("arma_");
    }

    /** Enfriamiento de la especial de un arma (ticks): el del tipo, un 4 % menos por cada tier. */
    public static int enfriamiento(ArmaItem a) {
        return Math.round(a.tipo.enfriamiento * (1.0F - 0.04F * (a.tierArma.nivel() - 1)));
    }

    // ------------------------------------------------------------------ eventos

    /** Al empezar el ataque: si el golpe va cargado, si corre y, con dagas, que el enemigo no esté "protegido" del golpe anterior. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || !(sp.getMainHandItem().getItem() instanceof ArmaItem a)) {
            return;
        }
        Estado st = estado(sp);
        st.golpeTick = sp.level().getGameTime();
        st.objetivo = e.getTarget() instanceof LivingEntity le0 ? le0 : null;
        st.cargado = sp.getAttackStrengthScale(0.5F) > 0.9F;
        st.corriendo = sp.isSprinting();
        if (a.tipo == TipoArma.DAGAS && e.getTarget() instanceof LivingEntity le && st.cargado) {
            // las dagas son tan rápidas que el golpe anterior aún "protege" al enemigo: se quita esa protección
            le.invulnerableTime = 0;
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (Gear.busy) {
            return;
        }
        // contraataque de la espada: recibir un golpe de alguien carga el siguiente
        if (e.getEntity() instanceof ServerPlayer victim && e.getSource().getEntity() instanceof LivingEntity && e.getSource().getEntity() != victim) {
            if (victim.getMainHandItem().getItem() instanceof ArmaItem a && a.tipo == TipoArma.ESPADA) {
                estado(victim).contraHasta = victim.level().getGameTime() + 60;
            }
        }
        // solo los golpes cuerpo a cuerpo (el estallido y otros efectos hacen daño de otro tipo y no repiten el estilo)
        if (e.getSource().getEntity() instanceof ServerPlayer sp && e.getSource().getDirectEntity() == sp && sp.level() instanceof ServerLevel sl
                && e.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) && sp.getMainHandItem().getItem() instanceof ArmaItem a) {
            Estado st = estado(sp);
            boolean reciente = sl.getGameTime() - st.golpeTick <= 1;
            if (reciente && st.objetivo != null && st.objetivo != e.getEntity()) {
                // el barrido de Minecraft (espada) a los de al lado: no repite el estilo del arma
                return;
            }
            e.setAmount(alGolpear(a, sp, sl, e.getEntity(), e.getAmount(), reciente && st.cargado, reciente && st.corriendo));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof Player p) {
            ESTADO.remove(p.getUUID());
        }
    }

    // ------------------------------------------------------------------ golpes

    /** Daño final de un golpe normal con el arma: estilo del tipo + elemento del tier. */
    public static float alGolpear(ArmaItem a, ServerPlayer sp, ServerLevel sl, LivingEntity t, float amount, boolean cargado, boolean corriendo) {
        Estado st = estado(sp);
        long now = sl.getGameTime();
        Vec3 c = t.getBoundingBox().getCenter();
        int col = a.tierArma.color;
        estela(a, sp, sl, c, cargado);
        switch (a.tipo) {
            case ESPADA -> {
                if (st.contraHasta >= now) {
                    amount *= 1.35F;
                    st.contraHasta = -1;
                    Vfx.flash(sl, c, 0xFFFFFF, 1.6, 6);
                    sonido(sl, c, SoundEvents.PLAYER_ATTACK_CRIT, 0.8F, 1.3F);
                }
                if (cargado) {
                    // barrido: los de al lado del objetivo también reciben
                    for (LivingEntity o : RelicFx.foes(sl, c, 2.6, sp)) {
                        if (o != t) {
                            golpe(sl, sp, o, amount * 0.40F, null);
                            elemento(a, sp, sl, o, amount * 0.40F, false);
                        }
                    }
                }
            }
            case HACHA -> {
                // quiebra armaduras: +2,5 % por punto de armadura del enemigo (hasta +50 %)
                float extra = Math.min(0.50F, t.getArmorValue() * 0.025F);
                if (extra > 0.0F) {
                    amount *= 1.0F + extra;
                    sl.sendParticles(Fx.shard(0xB0B8C0, 0.6F), c.x, c.y, c.z, 4 + Math.round(extra * 12), 0.25, 0.25, 0.25, 0.05);
                }
            }
            case MARTILLO -> {
                if (cargado) {
                    aturdir(t, 24);
                    RelicFx.knock(t, sp.position(), 0.55, 0.22);
                    Vfx.ring(sl, new Vec3(c.x, t.getY() + 0.1, c.z), col, 2.8, 10);
                    sonido(sl, c, SoundEvents.ANVIL_LAND, 0.35F, 0.6F);
                    for (LivingEntity o : RelicFx.foes(sl, c, 2.8, sp)) {
                        if (o != t) {
                            golpe(sl, sp, o, amount * 0.35F, null);
                            RelicFx.knock(o, c, 0.45, 0.2);
                        }
                    }
                }
            }
            case LANZA -> {
                if (corriendo) {
                    amount *= 1.30F;
                    Vfx.flash(sl, c, col, 1.4, 6);
                }
                if (cargado) {
                    // atraviesa: el primer enemigo detrás del objetivo (en la línea del golpe) recibe la mitad
                    Vec3 dir = c.subtract(sp.getEyePosition()).normalize();
                    LivingEntity detras = null;
                    double best = 99;
                    for (LivingEntity o : RelicFx.foes(sl, c.add(dir.scale(1.8)), 2.2, sp)) {
                        if (o == t) {
                            continue;
                        }
                        Vec3 to = o.getBoundingBox().getCenter().subtract(c);
                        double along = to.dot(dir);
                        if (along > 0.2 && along < 3.5 && to.subtract(dir.scale(along)).length() < 1.2 && along < best) {
                            best = along;
                            detras = o;
                        }
                    }
                    if (detras != null) {
                        golpe(sl, sp, detras, amount * 0.5F, null);
                        Vfx.beam(sl, c, detras.getBoundingBox().getCenter(), col, 0.15, 6, 0);
                    }
                }
            }
            case DAGAS -> {
                if (st.sombraHasta >= now || porLaEspalda(sp, t)) {
                    amount *= 1.75F;
                    sl.sendParticles(Fx.slash(col, 0.8F), c.x, c.y, c.z, 3, 0.2, 0.2, 0.2, 0.0);
                    sonido(sl, c, SoundEvents.PLAYER_ATTACK_CRIT, 0.7F, 1.6F);
                }
            }
        }
        if (cargado) {
            estallido(a, sp, sl, t, st, now);
        }
        return elemento(a, sp, sl, t, amount, true);
    }

    /** Probabilidad de que un golpe cargado estalle y espera mínima entre estallidos (ticks). */
    public static final float ESTALLIDO_PROB = 0.20F;
    public static final int ESTALLIDO_ESPERA = 100;

    /** El arrojadizo de cada tier de arma (los tiers 6 a 8 usan el pulso arcano). */
    public static com.mundosdeaxelis.arrojadizo.Arrojadizos.Tipo estallidoDe(ArmaItem a) {
        return com.mundosdeaxelis.arrojadizo.Arrojadizos.Tipo.ofTier(Math.min(5, a.tierArma.nivel()));
    }

    /**
     * Etapa 282: los arrojadizos pasaron a ser un efecto de las armas. Un golpe cargado a veces «estalla» con el arrojadizo de su
     * tier (esporas, metralla, escarcha, erupción o pulso arcano) alrededor del enemigo.
     */
    private static void estallido(ArmaItem a, ServerPlayer sp, ServerLevel sl, LivingEntity t, Estado st, long now) {
        if (now < st.estallidoHasta || sl.random.nextFloat() >= ESTALLIDO_PROB) {
            return;
        }
        forzarEstallido(a, sp, sl, t);
        st.estallidoHasta = now + ESTALLIDO_ESPERA;
    }

    /** Hace estallar el arrojadizo del arma en el enemigo (también lo usa la prueba). */
    public static void forzarEstallido(ArmaItem a, ServerPlayer sp, ServerLevel sl, LivingEntity t) {
        Vec3 at = new Vec3(t.getX(), t.getY() + 0.2, t.getZ());
        com.mundosdeaxelis.arrojadizo.Arrojadizos.burst(sl, at, estallidoDe(a), sp, null);
        com.mundosdeaxelis.encargo.Encargos.progress(sp, com.mundosdeaxelis.encargo.Encargos.Tipo.ARROJADIZOS, 1);
    }

    /**
     * Estela del golpe, con el color del tier: la espada deja un tajo en diagonal, el hacha y el martillo uno vertical, la lanza
     * una línea recta y las dagas cortes pequeños. Solo en los golpes cargados (las dagas, siempre).
     */
    private static void estela(ArmaItem a, ServerPlayer sp, ServerLevel sl, Vec3 c, boolean cargado) {
        if (!cargado && a.tipo != TipoArma.DAGAS) {
            return;
        }
        int col = a.tierArma.color;
        Vec3 eye = sp.getEyePosition();
        Vec3 to = c.subtract(eye);
        double d = Math.max(0.6, to.length() - 0.6);
        Vec3 dir = to.normalize();
        Vec3 p = eye.add(dir.scale(d));
        switch (a.tipo) {
            case ESPADA -> Vfx.slash(sl, p, dir.scale(0.5), col, 1.7, 0.7, 6);
            case HACHA -> Vfx.slash(sl, p, dir.scale(0.4), col, 1.9, Math.PI / 2, 6);
            case MARTILLO -> Vfx.slash(sl, p, dir.scale(0.3), col, 2.2, Math.PI / 2, 7);
            case LANZA -> Vfx.beam(sl, eye.add(dir.scale(0.8)).subtract(0, 0.25, 0), c, col, 0.12, 5, 0);
            case DAGAS -> Vfx.slash(sl, p, dir.scale(0.4), col, 1.0, (sl.random.nextBoolean() ? 1 : -1) * 0.9, 4);
        }
    }

    /** ¿El jugador está detrás del enemigo (el enemigo le da la espalda)? */
    public static boolean porLaEspalda(LivingEntity atacante, LivingEntity t) {
        Vec3 mira = Vec3.directionFromRotation(0.0F, t.yBodyRot);
        Vec3 hacia = new Vec3(t.getX() - atacante.getX(), 0, t.getZ() - atacante.getZ());
        if (hacia.lengthSqr() < 1.0E-4) {
            return false;
        }
        return mira.dot(hacia.normalize()) > 0.35;
    }

    /** Los jefes del mod (y los de Minecraft) se aturden mucho menos. */
    public static boolean esJefe(LivingEntity e) {
        return e.getType().is(net.minecraftforge.common.Tags.EntityTypes.BOSSES)
                || e instanceof com.mundosdeaxelis.entity.ReyBrote || e instanceof com.mundosdeaxelis.entity.GranHerrero
                || e instanceof com.mundosdeaxelis.entity.LoboArtico || e instanceof com.mundosdeaxelis.entity.GranSalamandra
                || e instanceof com.mundosdeaxelis.entity.CorazonMagma || e instanceof com.mundosdeaxelis.entity.Archimago
                || e instanceof com.mundosdeaxelis.entity.ColosoHierro || e instanceof com.mundosdeaxelis.entity.GuardianMusgo
                || e instanceof com.mundosdeaxelis.entity.Capataz;
    }

    private static void aturdir(LivingEntity t, int ticks) {
        boolean jefe = esJefe(t);
        int d = jefe ? ticks / 3 : ticks;
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, d, jefe ? 2 : 5, false, true));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, d, 0, false, true));
        if (t instanceof Mob m && !jefe) {
            m.getNavigation().stop();
        }
    }

    /**
     * El elemento del tier sobre un enemigo. Devuelve el daño final (Arcano y Vacío suman algo). {@code principal}: es el golpe
     * normal (en los golpes extra no se encadenan rayos para no hacer bucles).
     */
    public static float elemento(ArmaItem a, ServerPlayer sp, ServerLevel sl, LivingEntity t, float amount, boolean principal) {
        Vec3 c = t.getBoundingBox().getCenter();
        int col = a.tierArma.color;
        switch (a.tierArma.elemento) {
            case VENENO -> {
                MobEffectInstance veneno = new MobEffectInstance(MobEffects.POISON, 70, 0);
                if (t.canBeAffected(veneno)) {
                    t.addEffect(veneno);
                } else {
                    // a los no-muertos (y a las arañas) no les hace nada el veneno: sus espinas pegan un 10 % más
                    amount *= 1.10F;
                }
                sl.sendParticles(Fx.spark(0x7BD66B, 0.5F), c.x, c.y, c.z, 4, 0.25, 0.3, 0.25, 0.02);
            }
            case SANGRADO -> {
                // sangrado: tres pinchazos de un 10 % del golpe, uno por segundo
                float d = amount * 0.10F;
                for (int i = 1; i <= 3; i++) {
                    Scheduler.runLater(20 * i, () -> {
                        if (t.isAlive() && sp.isAlive() && sp.level() == t.level()) {
                            golpe(sl, sp, t, d, sl.damageSources().indirectMagic(sp, sp));
                            Vec3 p = t.getBoundingBox().getCenter();
                            sl.sendParticles(Fx.dust(0xB01818, 0.9F), p.x, p.y, p.z, 5, 0.2, 0.3, 0.2, 0.0);
                        }
                    });
                }
            }
            case ESCARCHA -> {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 40, t.getTicksFrozen() + 60));
                sl.sendParticles(Fx.shard(col, 0.6F), c.x, c.y, c.z, 5, 0.25, 0.3, 0.25, 0.03);
            }
            case FUEGO -> {
                t.setSecondsOnFire(4);
                sl.sendParticles(Fx.ember(col, 0.7F), c.x, c.y, c.z, 5, 0.25, 0.3, 0.25, 0.03);
            }
            case ARCANO -> {
                // un 15 % más como daño mágico (no lo para la armadura)
                float d = amount * 0.15F;
                Scheduler.runLater(1, () -> {
                    if (t.isAlive()) {
                        golpe(sl, sp, t, d, sl.damageSources().indirectMagic(sp, sp));
                    }
                });
                sl.sendParticles(Fx.rune(col, 0.6F), c.x, c.y, c.z, 3, 0.3, 0.3, 0.3, 0.02);
            }
            case VAMPIRO -> {
                float hp = Math.min(4.0F, amount * 0.07F);
                if (hp > 0.05F && sp.getHealth() < sp.getMaxHealth()) {
                    sp.heal(hp);
                    Vec3 p = sp.getBoundingBox().getCenter();
                    sl.sendParticles(Fx.heart(0xFF4F6B, 0.6F), p.x, p.y + 0.4, p.z, 1, 0.2, 0.2, 0.2, 0.0);
                }
            }
            case RAYO -> {
                if (principal && sl.random.nextFloat() < 0.25F) {
                    cadena(a, sp, sl, t, amount * 0.35F);
                }
            }
            case VACIO -> {
                t.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                if (sl.random.nextFloat() < 0.12F) {
                    amount *= 1.25F;
                    Vfx.flash(sl, c, 0x2A1848, 1.6, 6);
                }
                sl.sendParticles(Fx.spark(col, 0.5F), c.x, c.y, c.z, 3, 0.25, 0.3, 0.25, 0.02);
            }
        }
        return amount;
    }

    private static void cadena(ArmaItem a, ServerPlayer sp, ServerLevel sl, LivingEntity first, float dmg) {
        Set<LivingEntity> done = new HashSet<>();
        done.add(first);
        LivingEntity cur = first;
        Vec3 top = first.getBoundingBox().getCenter();
        Vfx.bolt(sl, top.add(0, 5, 0), top, 0xFFF6A0, 0.6, 6);
        sonido(sl, top, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.45F, 1.7F);
        for (int i = 0; i < 3; i++) {
            LivingEntity next = null;
            for (LivingEntity e : RelicFx.foes(sl, cur.getBoundingBox().getCenter(), 6.0, sp)) {
                if (!done.contains(e)) {
                    next = e;
                    break;
                }
            }
            if (next == null) {
                break;
            }
            done.add(next);
            Vfx.bolt(sl, cur.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(), 0xFFF6A0, 0.4, 6);
            golpe(sl, sp, next, dmg, null);
            cur = next;
        }
    }

    /** Daño extra del arma (barridos, sangrado, especiales): cuenta como del jugador y no deja al enemigo "protegido". */
    public static void golpe(ServerLevel sl, ServerPlayer sp, LivingEntity e, float amount, DamageSource src) {
        if (!e.isAlive() || amount <= 0.0F) {
            return;
        }
        boolean was = Gear.busy;
        Gear.busy = true;
        try {
            e.invulnerableTime = 0;
            e.hurt(src != null ? src : sl.damageSources().playerAttack(sp), amount);
            e.invulnerableTime = 0;
        } finally {
            Gear.busy = was;
        }
    }

    private static void sonido(ServerLevel sl, Vec3 p, SoundEvent s, float vol, float pitch) {
        sl.playSound(null, p.x, p.y, p.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    // ------------------------------------------------------------------ especiales

    public static boolean tryEspecial(ArmaItem a, ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        if (sp.getCooldowns().isOnCooldown(a)) {
            return false;
        }
        int cost = a.tipo.almas;
        if (Souls.get(sp) < cost) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.souls_missing", cost, Souls.get(sp))
                    .withStyle(ChatFormatting.RED), true);
            sonido(sl, sp.position(), SoundEvents.SOUL_ESCAPE, 0.6F, 0.7F);
            return false;
        }
        if (!especial(a, sp, sl)) {
            return false;
        }
        Souls.spend(sp, cost);
        sp.getCooldowns().addCooldown(a, enfriamiento(a));
        return true;
    }

    /** Lanza la especial del arma (sin mirar almas ni enfriamiento; lo usan también las pruebas). */
    public static boolean especial(ArmaItem a, ServerPlayer sp, ServerLevel sl) {
        float d = a.dano();
        return switch (a.tipo) {
            case ESPADA -> tajo(a, sp, sl, d * 1.5F);
            case HACHA -> torbellino(a, sp, sl, d * 0.6F);
            case MARTILLO -> terremoto(a, sp, sl, d * 1.2F);
            case LANZA -> embestida(a, sp, sl, d * 1.6F);
            case DAGAS -> pasoSombrio(a, sp, sl, d * 2.5F);
        };
    }

    /** Espada: Tajo creciente. Una onda cortante que avanza 10 bloques y hiere a todos los que cruza. */
    private static boolean tajo(ArmaItem a, ServerPlayer sp, ServerLevel sl, float dmg) {
        int col = a.tierArma.color;
        Vec3 eye = sp.getEyePosition().subtract(0, 0.35, 0);
        Vec3 look = sp.getLookAngle();
        double len = 10.0;
        Vfx.slash(sl, eye.add(look.scale(0.8)), look.scale(len), col, 3.6, 0.0, 10);
        Vfx.slash(sl, eye.add(look.scale(0.8)), look.scale(len), 0xFFFFFF, 2.4, 0.0, 8);
        sonido(sl, sp.position(), ModSounds.RELIC_WHOOSH.get(), 1.0F, 1.2F);
        sonido(sl, sp.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
        Set<LivingEntity> hit = new HashSet<>();
        for (int step = 1; step <= 5; step++) {
            final int s = step;
            Scheduler.runLater(step, () -> {
                Vec3 p = eye.add(look.scale(len * s / 5.0));
                for (LivingEntity e : RelicFx.foes(sl, p, 2.4, sp)) {
                    if (hit.add(e)) {
                        golpe(sl, sp, e, dmg, null);
                        elemento(a, sp, sl, e, dmg, false);
                        RelicFx.knock(e, sp.position(), 0.6, 0.2);
                        Vfx.flash(sl, e.getBoundingBox().getCenter(), col, 1.6, 6);
                    }
                }
            });
        }
        return true;
    }

    /** Hacha de guerra: Torbellino. Giras 2 segundos golpeando cinco veces a todo lo que tengas alrededor. */
    private static boolean torbellino(ArmaItem a, ServerPlayer sp, ServerLevel sl, float dmg) {
        int col = a.tierArma.color;
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 44, 0, false, false));
        for (int i = 0; i < 5; i++) {
            final int k = i;
            Scheduler.runLater(1 + i * 8, () -> {
                if (!sp.isAlive() || sp.level() != sl) {
                    return;
                }
                Vec3 at = sp.position();
                double ang = k * 1.9;
                Vec3 dir = new Vec3(Math.cos(ang), 0, Math.sin(ang));
                Vfx.slash(sl, at.add(0, 1.0, 0).subtract(dir.scale(1.6)), dir.scale(3.2), col, 3.0, Math.PI / 2, 8);
                Vfx.ring(sl, at.add(0, 0.15, 0), col, 3.6, 8);
                sonido(sl, at, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 0.7F + k * 0.08F);
                for (LivingEntity e : RelicFx.foes(sl, at.add(0, 0.9, 0), 3.6, sp)) {
                    golpe(sl, sp, e, dmg, null);
                    if (k == 0 || k == 4) {
                        elemento(a, sp, sl, e, dmg, false);
                    }
                    // los atrae un poco hacia el centro del giro
                    Vec3 to = at.subtract(e.position());
                    if (to.lengthSqr() > 2.0) {
                        e.setDeltaMovement(e.getDeltaMovement().add(to.normalize().scale(0.18)));
                        e.hurtMarked = true;
                    }
                }
            });
        }
        return true;
    }

    /** Martillo: Terremoto. Saltas y al caer golpeas el suelo: daño en área, los lanza por el aire y los aturde. */
    private static boolean terremoto(ArmaItem a, ServerPlayer sp, ServerLevel sl, float dmg) {
        int col = a.tierArma.color;
        Vec3 look = sp.getLookAngle();
        Vec3 h = new Vec3(look.x, 0, look.z);
        h = h.lengthSqr() < 1.0E-4 ? Vec3.ZERO : h.normalize();
        sp.setDeltaMovement(h.x * 0.55, 0.95, h.z * 0.55);
        sp.hurtMarked = true;
        sp.fallDistance = 0;
        Vfx.ring(sl, sp.position().add(0, 0.1, 0), col, 1.8, 8);
        sonido(sl, sp.position(), ModSounds.RELIC_WHOOSH.get(), 0.8F, 0.7F);
        boolean[] done = {false};
        for (int t = 4; t <= 44; t++) {
            final int tt = t;
            Scheduler.runLater(t, () -> {
                if (done[0] || !sp.isAlive() || sp.level() != sl) {
                    return;
                }
                sp.fallDistance = 0;
                if (tt == 6) {
                    // empuja hacia abajo para que caiga con fuerza
                    sp.setDeltaMovement(sp.getDeltaMovement().add(0, -0.6, 0));
                    sp.hurtMarked = true;
                }
                if (sp.onGround() || tt == 44) {
                    done[0] = true;
                    golpeSuelo(a, sp, sl, dmg, col);
                }
            });
        }
        return true;
    }

    /** El golpe al suelo del Terremoto (público para las pruebas). */
    public static void golpeSuelo(ArmaItem a, ServerPlayer sp, ServerLevel sl, float dmg, int col) {
        Vec3 at = sp.position();
        double r = 5.0;
        Vfx.ring(sl, at.add(0, 0.1, 0), col, r + 0.6, 14);
        Vfx.ring(sl, at.add(0, 0.1, 0), 0xFFFFFF, r * 0.6, 10);
        Vfx.flash(sl, at.add(0, 0.4, 0), col, 3.4, 8);
        RelicFx.burst(sl, Fx.dust(0x8A7A6A, 1.4F), at.add(0, 0.3, 0), 40, 0.45);
        RelicFx.shakeNear(sl, at, 12, 0.9F, 10);
        sonido(sl, at, ModSounds.RELIC_BOOM.get(), 1.0F, 0.8F);
        sonido(sl, at, SoundEvents.GENERIC_EXPLODE, 0.6F, 1.3F);
        for (LivingEntity e : RelicFx.foes(sl, at, r, sp)) {
            double k = 1.0 - 0.4 * Math.min(1.0, e.position().distanceTo(at) / r);
            golpe(sl, sp, e, (float) (dmg * k), null);
            elemento(a, sp, sl, e, dmg, false);
            aturdir(e, 40);
            RelicFx.knock(e, at, 0.35, 0.55);
        }
    }

    /** Lanza: Embestida. Te lanzas hacia delante unos 9 bloques atravesando y apartando a todos. */
    private static boolean embestida(ArmaItem a, ServerPlayer sp, ServerLevel sl, float dmg) {
        int col = a.tierArma.color;
        Vec3 look = sp.getLookAngle();
        Vec3 h = new Vec3(look.x, 0, look.z);
        h = h.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : h.normalize();
        sp.setDeltaMovement(h.x * 2.2, 0.15 + Math.max(0, look.y) * 0.5, h.z * 2.2);
        sp.hurtMarked = true;
        sp.fallDistance = 0;
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 16, 1, false, false));
        Vfx.beam(sl, sp.getEyePosition().subtract(0, 0.4, 0), sp.getEyePosition().subtract(0, 0.4, 0).add(h.scale(9)), col, 0.35, 8, 0);
        Vfx.flash(sl, sp.position().add(0, 1, 0), col, 2.4, 6);
        sonido(sl, sp.position(), ModSounds.RELIC_WHOOSH.get(), 1.0F, 1.0F);
        sonido(sl, sp.position(), SoundEvents.TRIDENT_RIPTIDE_1, 0.9F, 1.1F);
        Set<LivingEntity> hit = new HashSet<>();
        Vec3 dir = h;
        for (int t = 1; t <= 9; t++) {
            Scheduler.runLater(t, () -> {
                if (!sp.isAlive() || sp.level() != sl) {
                    return;
                }
                sp.fallDistance = 0;
                Vec3 p = sp.position().add(0, 1.0, 0);
                sl.sendParticles(Fx.spark(col, 0.7F), p.x, p.y, p.z, 4, 0.2, 0.3, 0.2, 0.02);
                for (LivingEntity e : RelicFx.foes(sl, p.add(dir.scale(0.8)), 2.2, sp)) {
                    if (hit.add(e)) {
                        golpe(sl, sp, e, dmg, null);
                        elemento(a, sp, sl, e, dmg, false);
                        // se apartan a un lado
                        Vec3 side = new Vec3(-dir.z, 0, dir.x);
                        if (side.dot(e.position().subtract(sp.position())) < 0) {
                            side = side.scale(-1);
                        }
                        e.setDeltaMovement(e.getDeltaMovement().add(side.scale(0.7)).add(0, 0.25, 0));
                        e.hurtMarked = true;
                        Vfx.flash(sl, e.getBoundingBox().getCenter(), col, 1.6, 6);
                    }
                }
            });
        }
        return true;
    }

    /**
     * Dagas: Paso sombrío. Apareces a la espalda del enemigo al que miras (hasta 12 bloques), le das una puñalada y durante 3
     * segundos todos tus golpes cuentan como por la espalda. Sin nadie a la vista, avanzas unos bloques y te vuelves invisible un momento.
     */
    private static boolean pasoSombrio(ArmaItem a, ServerPlayer sp, ServerLevel sl, float dmg) {
        int col = a.tierArma.color;
        Estado st = estado(sp);
        Vec3 from = sp.position();
        LivingEntity t = RelicFx.aimedFoe(sl, sp, 12.0, 0.88);
        humo(sl, from, col);
        if (t == null) {
            Vec3 look = sp.getLookAngle();
            Vec3 h = new Vec3(look.x, 0, look.z);
            h = h.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : h.normalize();
            Vec3 to = from;
            for (double dd = 6.0; dd >= 1.0; dd -= 0.5) {
                Vec3 c = from.add(h.scale(dd));
                if (sl.noCollision(sp, sp.getBoundingBox().move(c.subtract(from)))) {
                    to = c;
                    break;
                }
            }
            sp.teleportTo(sl, to.x, to.y, to.z, sp.getYRot(), sp.getXRot());
            sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
            humo(sl, to, col);
            st.sombraHasta = sl.getGameTime() + 60;
            return true;
        }
        Vec3 back = Vec3.directionFromRotation(0.0F, t.yBodyRot);
        Vec3 dest = null;
        double dist = t.getBbWidth() * 0.5 + 0.9;
        for (Vec3 cand : new Vec3[]{t.position().subtract(back.scale(dist)), t.position().add(new Vec3(-back.z, 0, back.x).scale(dist)),
                t.position().add(new Vec3(back.z, 0, -back.x).scale(dist)), from}) {
            if (sl.noCollision(sp, sp.getBoundingBox().move(cand.subtract(from)))) {
                dest = cand;
                break;
            }
        }
        if (dest == null) {
            dest = from;
        }
        Vec3 to = t.position().subtract(dest);
        float yaw = (float) (Mth.atan2(to.z, to.x) * (180.0 / Math.PI)) - 90.0F;
        sp.teleportTo(sl, dest.x, dest.y, dest.z, yaw, 10.0F);
        sp.fallDistance = 0;
        humo(sl, dest, col);
        st.sombraHasta = sl.getGameTime() + 60;
        golpe(sl, sp, t, dmg, null);
        elemento(a, sp, sl, t, dmg, false);
        Vec3 c = t.getBoundingBox().getCenter();
        Vfx.slash(sl, c.add(0, 0.3, 0), to.normalize().scale(-1.2), col, 1.8, 0.6, 8);
        sonido(sl, c, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.5F);
        sonido(sl, dest, SoundEvents.ENDERMAN_TELEPORT, 0.6F, 1.6F);
        return true;
    }

    private static void humo(ServerLevel sl, Vec3 p, int col) {
        sl.sendParticles(Fx.dust(0x1A1024, 1.6F), p.x, p.y + 0.9, p.z, 18, 0.3, 0.6, 0.3, 0.02);
        sl.sendParticles(Fx.spark(col, 0.6F), p.x, p.y + 0.9, p.z, 8, 0.3, 0.6, 0.3, 0.04);
    }

    /** Para las pruebas: hace que el siguiente golpe de espada lleve el contraataque. */
    public static void cargarContra(ServerPlayer sp) {
        estado(sp).contraHasta = sp.level().getGameTime() + 60;
    }

    public static boolean sombra(ServerPlayer sp) {
        return estado(sp).sombraHasta >= sp.level().getGameTime();
    }
}
