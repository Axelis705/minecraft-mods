package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.vfx.VfxTex;
import com.mundosdeaxelis.vfx.VfxType;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Los diez compañeros en 3D (zorro, gatito, búho, conejo, medusa, fénix, tortuga, pingüino, unicornio y panda).
 * Igual que {@link Familiars}: no son entidades; el servidor simula posición y habilidades y manda un modelo 3D
 * al cliente (textura {@link VfxTex#CREATURE3D}, la especie va en el campo "b").
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Companions {
    private Companions() {
    }

    /** Ficha de un compañero: objeto, especie del modelo, tamaño, si vuela, color, duración y enfriamiento (segundos). */
    public record Spec(String id, int species, double size, boolean fly, int rgb, int secs, int cdSecs, Rarity rarity, int slot) {
    }

    public static final Spec FOX = new Spec("comp_fox", 0, 1.05, false, 0xFF9A3C, 60, 60, Rarity.RARE, 0);
    public static final Spec CAT = new Spec("comp_cat", 1, 0.95, false, 0xB8B0FF, 60, 60, Rarity.RARE, 1);
    public static final Spec BUNNY = new Spec("comp_bunny", 3, 0.95, false, 0x7CE38A, 70, 70, Rarity.RARE, 3);
    public static final Spec JELLY = new Spec("comp_jelly", 4, 1.15, true, 0xB8D8FF, 55, 70, Rarity.EPIC, 4);
    public static final Spec PHOENIX = new Spec("comp_phoenix", 5, 1.15, true, 0xFF6A2E, 50, 80, Rarity.EPIC, 5);
    public static final Spec TURTLE = new Spec("comp_turtle", 6, 0.95, false, 0x59E0D0, 70, 75, Rarity.RARE, 6);
    public static final Spec PENGUIN = new Spec("comp_penguin", 7, 1.05, false, 0x9FE8FF, 55, 65, Rarity.RARE, 7);
    public static final Spec PANDA = new Spec("comp_panda", 9, 1.05, false, 0x9AD86A, 55, 70, Rarity.EPIC, 9);
    public static final Spec CHICK = new Spec("comp_chick", 11, 0.80, false, 0xFFE55C, 40, 45, Rarity.COMMON, 10);
    public static final Spec FROG = new Spec("comp_frog", 12, 0.85, false, 0x6CE070, 40, 45, Rarity.COMMON, 11);
    public static final Spec HAMSTER = new Spec("comp_hamster", 13, 0.85, false, 0xFFBC66, 40, 45, Rarity.COMMON, 12);
    public static final Spec SNAIL = new Spec("comp_snail", 14, 0.95, false, 0xFFB0E8, 45, 50, Rarity.COMMON, 13);
    public static final Spec BEE = new Spec("comp_bee", 15, 0.80, true, 0xFFCC33, 45, 50, Rarity.UNCOMMON, 14);
    public static final Spec SHEEP = new Spec("comp_sheep", 16, 1.0, false, 0xE8F0FF, 50, 55, Rarity.UNCOMMON, 15);
    public static final Spec HEDGEHOG = new Spec("comp_hedgehog", 17, 0.85, false, 0xB07850, 50, 55, Rarity.UNCOMMON, 16);
    public static final Spec AXOLOTL = new Spec("comp_axolotl", 18, 0.95, false, 0xFF8FB0, 55, 60, Rarity.EPIC, 17);
    public static final Spec RACCOON = new Spec("comp_raccoon", 19, 1.0, false, 0x9AA0C0, 55, 60, Rarity.RARE, 18);
    public static final Spec WHALE = new Spec("comp_whale", 21, 1.6, true, 0x6A9CFF, 60, 90, Rarity.EPIC, 20);
    public static final Spec KITSUNE = new Spec("comp_kitsune", 22, 1.2, false, 0xB8C8FF, 60, 100, Rarity.EPIC, 21);
    public static final Spec CAPYBARA = new Spec("comp_capybara", 23, 1.1, false, 0xC89A62, 60, 55, Rarity.COMMON, 22);
    public static final Spec DUCK = new Spec("comp_duck", 24, 0.85, false, 0xFFE04A, 40, 45, Rarity.COMMON, 23);
    public static final Spec SLIME = new Spec("comp_slime", 25, 0.95, false, 0x7CE06A, 40, 45, Rarity.COMMON, 24);
    public static final Spec CORGI = new Spec("comp_corgi", 26, 1.0, false, 0xF5A64C, 50, 55, Rarity.UNCOMMON, 25);
    public static final Spec CREEPER = new Spec("comp_creeper", 27, 1.0, false, 0x6FD060, 50, 60, Rarity.EPIC, 26);
    public static final Spec SNOWMAN = new Spec("comp_snowman", 28, 1.05, false, 0xDDF0FF, 50, 55, Rarity.UNCOMMON, 27);
    public static final Spec GHOST = new Spec("comp_ghost", 29, 0.95, true, 0xB8D0FF, 55, 65, Rarity.RARE, 28);
    public static final Spec GOLEM = new Spec("comp_golem", 30, 1.3, false, 0xC8CEDA, 55, 65, Rarity.EPIC, 29);
    public static final Spec SPARKMOUSE = new Spec("comp_sparkmouse", 31, 1.0, false, 0xFFD930, 55, 65, Rarity.RARE, 30);
    public static final Spec DROID = new Spec("comp_droid", 32, 1.1, false, 0x5C9CFF, 60, 75, Rarity.EPIC, 31);
    public static final Spec FOREST_SPIRIT = new Spec("comp_forest_spirit", 33, 1.5, false, 0x9AB4C8, 60, 80, Rarity.EPIC, 32);
    public static final Spec MOON_WOLF = new Spec("comp_moon_wolf", 34, 1.25, false, 0xA8D8FF, 60, 80, Rarity.EPIC, 33);
    public static final Spec NIGHT_DRAGON = new Spec("comp_night_dragon", 35, 1.4, true, 0x7A6AFF, 55, 90, Rarity.EPIC, 34);
    public static final Spec OCTOPUS = new Spec("comp_octopus", 36, 0.95, false, 0xD08AE0, 50, 55, Rarity.UNCOMMON, 35);
    public static final Spec BUTTERFLY = new Spec("comp_butterfly", 37, 1.0, true, 0x5CB4FF, 60, 55, Rarity.EPIC, 36);
    public static final Spec CRAB = new Spec("comp_crab", 38, 0.85, false, 0xFF6A50, 40, 45, Rarity.COMMON, 37);
    public static final Spec FIREFLY = new Spec("comp_firefly", 39, 0.85, true, 0xD8FF50, 50, 55, Rarity.UNCOMMON, 38);
    public static final Spec ELEPHANT = new Spec("comp_elephant", 41, 1.3, false, 0xB8B8D8, 55, 65, Rarity.RARE, 40);
    public static final Spec LION = new Spec("comp_lion", 42, 1.0, false, 0xFFB84A, 55, 65, Rarity.RARE, 41);
    public static final Spec PEACOCK = new Spec("comp_peacock", 44, 1.2, false, 0x30C8D8, 60, 75, Rarity.EPIC, 43);
    public static final Spec DOLPHIN = new Spec("comp_dolphin", 45, 1.5, true, 0x6AC8FF, 60, 80, Rarity.EPIC, 44);
    public static final Spec PEGASUS = new Spec("comp_pegasus", 46, 1.4, true, 0xF0F4FF, 55, 90, Rarity.EPIC, 45);
    public static final Spec CELESTIAL_DRAGON = new Spec("comp_celestial_dragon", 47, 1.7, true, 0x40E0B8, 60, 100, Rarity.EPIC, 46);
    public static final Spec FERRET = new Spec("comp_ferret", 48, 0.9, false, 0xB4BAC6, 45, 50, Rarity.EPIC, 47);
    public static final Spec MIMIC = new Spec("comp_mimic", 49, 1.1, false, 0xC88A3A, 55, 70, Rarity.EPIC, 48);
    public static final Spec GRIFFON = new Spec("comp_griffon", 50, 1.5, true, 0xFFD060, 50, 80, Rarity.EPIC, 49);
    public static final Spec PIGGY = new Spec("comp_piggy", 51, 0.9, false, 0xFF9EB8, 40, 45, Rarity.COMMON, 50);
    public static final Spec SKUNK = new Spec("comp_skunk", 52, 0.95, false, 0x7A3CC8, 45, 50, Rarity.UNCOMMON, 51);
    public static final Spec SEAL = new Spec("comp_seal", 54, 1.0, false, 0x7C98C0, 60, 65, Rarity.RARE, 53);
    public static final Spec MOONIMP = new Spec("comp_moonimp", 55, 1.0, false, 0x8E6CD8, 55, 70, Rarity.EPIC, 54);
    public static final Spec PATCH = new Spec("comp_patch", 53, 1.12, false, 0xB59CF0, 40, 45, Rarity.COMMON, 58);
    public static final Spec TRIKE = new Spec("comp_trike", 61, 1.0, false, 0xFF7C98, 55, 60, Rarity.RARE, 61);
    public static final Spec YETI = new Spec("comp_yeti", 62, 1.1, false, 0x9AEAFF, 55, 75, Rarity.EPIC, 62);
    // mascotas exclusivas de los jefes de las mazmorras (solo caen de ellos)
    public static final Spec SPROUT_KING = new Spec("comp_sprout_king", 63, 1.0, false, 0x5CD66C, 50, 60, Rarity.EPIC, 63);
    public static final Spec EMBER_ANVIL = new Spec("comp_ember_anvil", 64, 1.05, false, 0xFF7A2A, 55, 70, Rarity.EPIC, 64);
    public static final Spec[] ALL = {FOX, CAT, BUNNY, JELLY, PHOENIX, TURTLE, PENGUIN, PANDA,
            CHICK, FROG, HAMSTER, SNAIL, BEE, SHEEP, HEDGEHOG, AXOLOTL, RACCOON, WHALE, KITSUNE,
            CAPYBARA, DUCK, SLIME, CORGI, CREEPER, SNOWMAN, GHOST, GOLEM, SPARKMOUSE, DROID, FOREST_SPIRIT, MOON_WOLF, NIGHT_DRAGON,
            OCTOPUS, BUTTERFLY, CRAB, FIREFLY, ELEPHANT, LION, PEACOCK, DOLPHIN, PEGASUS, CELESTIAL_DRAGON, FERRET, MIMIC, GRIFFON, PIGGY, SKUNK, PATCH, SEAL, MOONIMP, TRIKE, YETI, SPROUT_KING, EMBER_ANVIL};

    private static final class C {
        Spec s;
        UUID owner;
        ServerLevel level;
        Vec3 pos;
        Vec3 vel = Vec3.ZERO;
        Vec3 face = new Vec3(0, 0, 1);
        /** Sitio fijo en el mundo donde espera (no se mueve al girar la cámara). */
        Vec3 anchor;
        int age, life, sprite;
        /** Mascota equipada: siempre te acompaña, no ataca ni hace acciones (solo da la pasiva de su figurita). */
        boolean perm;
        int cd, cd2, cd3;
        float power;
        boolean used;
        String last = "";
    }

    private static final List<C> LIST = new ArrayList<>();
    /** Las mascotas fijas se redibujan cada {@code RESEND} ticks (así también las ven los que llegan después) y su dibujo dura un poco más. */
    private static final int PERM_SPRITE_LIFE = 600, RESEND = 400;

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        LIST.clear();
    }

    public static Spec byId(String id) {
        for (Spec s : ALL) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** Invoca (o renueva) a ese compañero para el jugador. */
    public static void summon(ServerPlayer sp, Spec spec, float power) {
        spawn(sp, spec, power, false);
    }

    /**
     * Mascotas equipadas: se asegura de que cada una te acompañe (las crea si faltan) y retira las que ya no llevas.
     * Se llama varias veces por segundo desde el equipo del jugador.
     */
    public static void follow(ServerPlayer sp, java.util.Collection<Spec> wanted) {
        Iterator<C> it = LIST.iterator();
        while (it.hasNext()) {
            C c = it.next();
            if (c.perm && c.owner.equals(sp.getUUID()) && !wanted.contains(c.s)) {
                finish(c, false);
                it.remove();
            }
        }
        for (Spec w : wanted) {
            boolean has = false;
            for (C c : LIST) {
                if (c.perm && c.owner.equals(sp.getUUID()) && c.s == w) {
                    has = true;
                    break;
                }
            }
            if (!has) {
                spawn(sp, w, equipPower(w), true);
            }
        }
    }

    /**
     * Factor de equilibrio por especie: iguala la fuerza real de cada mascota a su rareza (las de rareza baja, que
     * antes pegaban o curaban de más, bajan; las flojas de su rareza suben un poco). Se multiplica por la fuerza de la rareza.
     */
    private static float balance(int species) {
        return switch (species) {
            case 11 -> 0.63F; case 12 -> 1.1F; case 13 -> 0.85F; case 23 -> 0.7F; case 24 -> 0.5F; case 25 -> 0.7F;
            case 1 -> 0.55F; case 3 -> 1.3F; case 15 -> 0.85F; case 16 -> 1.05F; case 17 -> 0.5F; case 26 -> 0.5F;
            case 27 -> 1.15F; case 30 -> 1.15F; case 28 -> 0.5F;
            case 0 -> 0.75F; case 6 -> 1.1F; case 7 -> 1.3F; case 18 -> 1.15F; case 19 -> 0.93F;
            case 4 -> 1.2F; case 8 -> 1.2F; case 9 -> 1.4F; case 32 -> 1.2F; case 33 -> 1.4F; case 34 -> 1.2F;
            case 21 -> 1.8F; case 5 -> 1.1F; case 35 -> 1.2F; case 31 -> 0.7F; case 43 -> 0.8F;
            case 36 -> 0.5F; case 37 -> 1.1F; case 38 -> 0.5F; case 40 -> 0.8F; case 41 -> 0.7F; case 42 -> 0.5F; case 48 -> 1.3F;
            case 47 -> 1.1F;
            case 49 -> 1.0F; case 50 -> 1.0F; case 51 -> 1.0F; case 52 -> 1.0F; case 54 -> 1.0F; case 55 -> 1.0F; case 53 -> 0.6F; case 61 -> 1.0F; case 62 -> 1.0F;
            default -> 1.0F;
        };
    }

    /** Fuerza de una mascota equipada: rareza por equilibrio de especie. */
    private static float equipPower(Spec spec) {
        return com.mundosdeaxelis.crate.Tiers.of(spec.id()).power * balance(spec.species());
    }

    private static void spawn(ServerPlayer sp, Spec spec, float power, boolean perm) {
        remove(sp, spec, true);
        ServerLevel sl = sp.serverLevel();
        C c = new C();
        c.s = spec;
        c.owner = sp.getUUID();
        c.level = sl;
        c.perm = perm;
        c.life = perm ? Integer.MAX_VALUE / 2 : (int) (20 * spec.secs() * power);
        c.power = power;
        Vec3 look = sp.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        double gx = sp.getX() + flat.x * 1.8, gz = sp.getZ() + flat.z * 1.8;
        double gy = spec.fly() ? sp.getY() + 1.8 : RelicFx.groundY(sl, gx, sp.getY() + 1, gz);
        c.pos = new Vec3(gx, gy, gz);
        c.face = flat.scale(-1);
        c.cd = 30;
        c.cd2 = 50;
        c.cd3 = 60;
        c.sprite = Vfx.make(sl, VfxType.SPRITE).at(c.pos).a(VfxTex.CREATURE3D).b(spec.species()).size(spec.size()).rgb(0xFFFFFF)
                .dir(c.face).life(perm ? PERM_SPRITE_LIFE : c.life + 12).send();
        Vfx.flash(sl, c.pos.add(0, spec.size() * 0.5, 0), spec.rgb(), perm ? 1.6 : 2.4, 12);
        if (!perm) {
            Vfx.ring(sl, c.pos.add(0, 0.1, 0), spec.rgb(), 2.6, 16);
        }
        RelicFx.burst(sl, Fx.spark(spec.rgb(), 0.7F), c.pos.add(0, spec.size() * 0.5, 0), perm ? 10 : 18, 0.25);
        LIST.add(c);
    }

    private static void remove(ServerPlayer sp, Spec spec, boolean silent) {
        Iterator<C> it = LIST.iterator();
        while (it.hasNext()) {
            C c = it.next();
            if (c.owner.equals(sp.getUUID()) && c.s == spec) {
                Vfx.remove(c.level, c.sprite);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || LIST.isEmpty()) {
            return;
        }
        Iterator<C> it = LIST.iterator();
        while (it.hasNext()) {
            C c = it.next();
            ServerPlayer owner = c.level.getServer().getPlayerList().getPlayer(c.owner);
            boolean gone = owner == null || !owner.isAlive() || owner.serverLevel() != c.level;
            c.age++;
            if (gone || (!c.perm && c.age > c.life)) {
                finish(c, gone);
                it.remove();
                continue;
            }
            try {
                tick(c, owner);
            } catch (Exception e) {
                MundosDeAxelis.LOGGER.error("Error en un compañero", e);
                finish(c, true);
                it.remove();
            }
        }
    }

    private static void finish(C c, boolean silent) {
        if (silent) {
            Vfx.remove(c.level, c.sprite);
            return;
        }
        // se encoge unos ticks y se va con una chispa
        Vfx.make(c.level, VfxType.SPRITE).at(c.pos).a(VfxTex.CREATURE3D).b(c.s.species()).size(c.s.size()).rgb(0xFFFFFF).dir(c.face).life(8).update(c.sprite);
        int id = c.sprite;
        ServerLevel sl = c.level;
        Scheduler.runLater(9, () -> Vfx.remove(sl, id));
        RelicSounds.play(sl, c.pos, c.s.id(), "end", 0.9F, 1.0F);
        Vec3 mid = c.pos.add(0, c.s.size() * 0.5, 0);
        Vfx.flash(sl, mid, c.s.rgb(), 1.8, 10);
        RelicFx.burst(sl, Fx.spark(c.s.rgb(), 0.6F), mid, 14, 0.25);
    }

    // ------------------------------------------------------------------ movimiento

    /** Hacia dónde mira el dueño en el plano (no se rompe al mirar recto arriba o abajo). */
    private static Vec3 yawLook(ServerPlayer p) {
        double y = Math.toRadians(p.getYRot());
        return new Vec3(-Math.sin(y), 0, Math.cos(y));
    }

    private static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0, v.z);
        return f.lengthSqr() < 1.0E-6 ? Vec3.ZERO : f.normalize();
    }


    /** Sitio de espera: delante del dueño, alternando izquierda y derecha, siempre dentro de su campo de visión. */
    private static Vec3 waitSpot(Vec3 ownerPos, Vec3 look, int idx, int slot, boolean fly) {
        double sign = idx % 2 == 0 ? 1.0 : -1.0;
        int ring = idx / 2;
        double deg = Math.min(70.0, 30.0 + 17.0 * ring);
        double a = Math.toRadians(sign * deg);
        double r = 2.7 + 0.55 * ring + (slot % 3) * 0.12;
        if (fly) {
            r += 0.4;
        }
        double ca = Math.cos(a), sa = Math.sin(a);
        double lx = look.x * ca - look.z * sa;
        double lz = look.x * sa + look.z * ca;
        return ownerPos.add(lx * r, 0, lz * r);
    }

    private static void teleportNear(C c, ServerPlayer owner) {
        Vec3 look = waitSpot(owner.position(), yawLook(owner), 0, c.s.slot(), c.s.fly()).subtract(owner.position());
        double x = owner.getX() + look.x, z = owner.getZ() + look.z;
        double y = c.s.fly() ? owner.getY() + 1.8 : RelicFx.groundY(c.level, x, owner.getY() + 1, z);
        Vec3 from = c.pos;
        c.pos = new Vec3(x, y, z);
        c.vel = Vec3.ZERO;
        Vfx.flash(c.level, from.add(0, 0.5, 0), c.s.rgb(), 1.2, 8);
        Vfx.flash(c.level, c.pos.add(0, 0.5, 0), c.s.rgb(), 1.2, 8);
        // el sprite no debe deslizarse por todo el mapa: se marca un salto limpio
        Vfx.remove(c.level, c.sprite);
        c.sprite = Vfx.make(c.level, VfxType.SPRITE).at(c.pos).a(VfxTex.CREATURE3D).b(c.s.species()).size(c.s.size()).rgb(0xFFFFFF)
                .dir(c.face).life(c.life - c.age + 12).send();
    }

    /** Camina hacia un punto pegado al suelo (con pasos y caídas suaves). */
    private static void groundMove(C c, Vec3 to, double maxSpeed) {
        Vec3 d = new Vec3(to.x - c.pos.x, 0, to.z - c.pos.z);
        double len = d.length();
        Vec3 step = Vec3.ZERO;
        if (len > 0.12) {
            step = d.scale(Math.min(maxSpeed, len * 0.22) / len);
        }
        c.vel = c.vel.scale(0.55).add(step.scale(0.45));
        double nx = c.pos.x + c.vel.x, nz = c.pos.z + c.vel.z;
        double gy = RelicFx.groundY(c.level, nx, c.pos.y + 1.0, nz);
        if (gy > c.pos.y + 1.15) {
            // pared: no avanza en esa dirección
            nx = c.pos.x;
            nz = c.pos.z;
            gy = RelicFx.groundY(c.level, nx, c.pos.y + 1.0, nz);
            c.vel = Vec3.ZERO;
        }
        double ny = gy > c.pos.y ? c.pos.y + Math.min(gy - c.pos.y, 0.42) : c.pos.y - Math.min(c.pos.y - gy, 0.45);
        c.pos = new Vec3(nx, ny, nz);
    }

    private static void flyMove(C c, Vec3 to, double maxStep, double smooth) {
        Vec3 d = to.subtract(c.pos);
        Vec3 step = d.scale(smooth);
        if (step.length() > maxStep) {
            step = step.normalize().scale(maxStep);
        }
        c.vel = c.vel.scale(0.5).add(step.scale(0.5));
        c.pos = c.pos.add(c.vel);
    }

    private static LivingEntity pickFoe(C c, ServerPlayer owner, double range) {
        for (LivingEntity e : RelicFx.foes(c.level, owner.position(), range, owner)) {
            if (owner.hasLineOfSight(e) || c.pos.distanceTo(e.position()) < 7) {
                return e;
            }
        }
        return null;
    }

    private static void buff(ServerPlayer p, MobEffect e, int ticks, int amp) {
        p.addEffect(new MobEffectInstance(e, ticks, amp, true, false, true));
    }

    // ------------------------------------------------------------------ simulación

    private static void tick(C c, ServerPlayer owner) {
        ServerLevel sl = c.level;
        Spec s = c.s;
        int left = c.life - c.age;
        Vec3 look = yawLook(owner);
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        if (c.pos.distanceTo(owner.position()) > 24 || c.pos.y < owner.getY() - 14) {
            teleportNear(c, owner);
        }
        LivingEntity foe = !c.perm && c.age > 20 && left > 12 ? pickFoe(c, owner, 14) : null;
        double dist = foe == null ? 0 : c.pos.distanceTo(foe.position());
        Vec3 ownerPos = owner.position();
        // sitio de espera: en un arco delante del dueño (a los lados de la mira) para que se vean siempre al girar la cámara
        int n = 0, idx = 0;
        for (C o : LIST) {
            if (o.owner.equals(c.owner)) {
                if (o == c) {
                    idx = n;
                }
                n++;
            }
        }
        // El sitio se fija en el mundo: solo se vuelve a elegir (delante del dueño) si este se aleja mucho o se
        // mete encima de la mascota. Así, al quedarte quieto y girar la cámara, las mascotas no se escapan de la vista.
        if (c.anchor != null && c.anchor.y < ownerPos.y - 10) {
            c.anchor = null;
        }
        if (c.anchor == null || flat(c.anchor.subtract(ownerPos)).lengthSqr() < 0.1
                || Math.hypot(c.anchor.x - ownerPos.x, c.anchor.z - ownerPos.z) > 6.5
                || Math.hypot(c.anchor.x - ownerPos.x, c.anchor.z - ownerPos.z) < 1.3) {
            c.anchor = waitSpot(ownerPos, look, idx, s.slot(), s.fly());
        }
        Vec3 home = new Vec3(c.anchor.x, ownerPos.y, c.anchor.z);
        double speed = 0.20;
        Vec3 target = home;
        switch (s.species()) {
            case 0 -> speed = foe != null ? 0.36 : 0.30;
            case 1 -> speed = foe != null ? 0.32 : 0.27;
            case 3 -> speed = 0.24;
            case 6 -> speed = 0.15;
            case 7 -> speed = 0.20;
            case 8 -> speed = 0.27;
            case 9 -> speed = 0.20;
            case 11 -> speed = foe != null ? 0.30 : 0.25;
            case 12 -> speed = 0.22;
            case 13 -> speed = 0.20;
            case 14 -> speed = 0.09;
            case 16 -> speed = 0.20;
            case 17 -> speed = 0.20;
            case 18 -> speed = 0.20;
            case 19 -> speed = foe != null ? 0.28 : 0.24;
            case 20 -> speed = 0.30;
            case 22 -> speed = 0.30;
            case 23 -> speed = 0.12;
            case 24 -> speed = 0.21;
            case 25 -> speed = foe != null ? 0.26 : 0.20;
            case 26 -> speed = foe != null ? 0.34 : 0.28;
            case 27 -> speed = foe != null ? 0.30 : 0.22;
            case 28 -> speed = 0.14;
            case 30 -> speed = foe != null ? 0.24 : 0.18;
            case 31 -> speed = foe != null ? 0.30 : 0.26;
            case 32 -> speed = 0.22;
            case 33 -> speed = 0.13;
            case 34 -> speed = foe != null ? 0.38 : 0.30;
            case 36 -> speed = 0.16;
            case 38 -> speed = foe != null ? 0.30 : 0.24;
            case 40 -> speed = 0.12;
            case 41 -> speed = 0.18;
            case 42 -> speed = foe != null ? 0.34 : 0.27;
            case 43 -> speed = foe != null ? 0.28 : 0.22;
            case 44 -> speed = 0.18;
            case 48 -> speed = foe != null ? 0.38 : 0.32;
            default -> {
            }
        }
        boolean melee = s.species() == 0 || s.species() == 1 || s.species() == 3 || s.species() == 9 || s.species() == 11 || s.species() == 16
                || s.species() == 17 || s.species() == 18 || s.species() == 19
                || s.species() == 24 || s.species() == 25 || s.species() == 26 || s.species() == 27 || s.species() == 30 || s.species() == 34
                || s.species() == 38 || s.species() == 42 || s.species() == 48;
        if (foe != null && melee && left > 14) {
            Vec3 toFoe = flat(foe.position().subtract(c.pos));
            target = foe.position().subtract(toFoe.scale(s.species() == 9 ? 1.4 : 1.0));
        }
        if (!s.fly()) {
            if (c.pos.distanceTo(home) < 0.4 && foe == null) {
                target = c.pos;
            }
            groundMove(c, target, speed);
        } else {
            double hover = switch (s.species()) {
                case 2 -> 2.3;
                case 4 -> 2.7;
                case 15 -> 1.9;
                case 21 -> 3.0;
                case 29 -> 1.9;
                case 35 -> 2.6;
                case 37 -> 1.6;
                case 39 -> 1.5;
                case 45 -> 2.8;
                case 46 -> 2.5;
                case 47 -> 3.0;
                default -> 2.2;
            };
            Vec3 want = home.add(0, hover - 0.35 + Math.sin(c.age * 0.12 + s.slot()) * 0.25, 0);
            if (foe != null && s.species() == 5) {
                Vec3 away = flat(ownerPos.subtract(foe.position()));
                want = foe.position().add(away.scale(4.0)).add(0, 2.4, 0);
            }
            if (left < 14) {
                want = c.pos.add(0, 0.8, 0);
            }
            if (c.age <= 10) {
                want = c.pos.add(0, 0.05, 0);
            }
            double gy = RelicFx.groundY(sl, c.pos.x, c.pos.y + 1, c.pos.z);
            want = new Vec3(want.x, Math.max(want.y, gy + 1.0), want.z);
            flyMove(c, want, 0.55, 0.18);
        }
        // hacia dónde mira
        Vec3 fd = flat(c.vel.lengthSqr() > 0.0009 ? c.vel : Vec3.ZERO);
        if (foe != null) {
            Vec3 tf = flat(foe.position().subtract(c.pos));
            if (tf.lengthSqr() > 0.5 && (fd.lengthSqr() < 0.1 || dist < 6)) {
                fd = tf;
            }
        }
        if (fd.lengthSqr() < 0.1) {
            // quieto: mira hacia su dueño (así se le ve la cara)
            fd = flat(ownerPos.subtract(c.pos)).lengthSqr() > 0.1 ? flat(ownerPos.subtract(c.pos)) : c.face;
        }
        c.face = fd;
        c.cd--;
        c.cd2--;
        c.cd3--;
        if (!c.perm && left > 12) {
            behave(c, owner, foe, dist);
        }
        if (c.perm && c.age % RESEND == 0) {
            Vfx.make(sl, VfxType.SPRITE).at(c.pos).a(VfxTex.CREATURE3D).b(s.species()).size(s.size()).rgb(0xFFFFFF).dir(c.face)
                    .life(PERM_SPRITE_LIFE).resend(c.sprite);
        }
        // un poco de brillo de su color
        if (c.age % (c.perm ? 12 : 6) == 0) {
            sl.sendParticles(Fx.spark(s.rgb(), 0.28F), c.pos.x, c.pos.y + s.size() * 0.5, c.pos.z, 1, 0.25, 0.3, 0.25, 0.0);
        }
        Vfx.make(sl, VfxType.SPRITE).at(c.pos).a(VfxTex.CREATURE3D).b(s.species()).size(s.size()).rgb(0xFFFFFF).dir(c.face).life(0).update(c.sprite);
    }

    private static void act(C c, float vol, float pitch) {
        RelicSounds.play(c.level, c.pos, c.s.id(), "act", vol, pitch);
    }

    private static Vec3 mid(LivingEntity e) {
        return e.getBoundingBox().getCenter();
    }

    private static void behave(C c, ServerPlayer owner, LivingEntity foe, double dist) {
        ServerLevel sl = c.level;
        Vec3 body = c.pos.add(0, c.s.size() * 0.5, 0);
        float p = c.power;
        switch (c.s.species()) {
            // ---------------------------------------------------------- zorro estelar: mordida veloz + velocidad
            case 0 -> {
                if (c.age % 40 == 0) {
                    buff(owner, MobEffects.MOVEMENT_SPEED, 80, 0);
                }
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 2.0) {
                    c.cd = 28;
                    Vec3 to = mid(foe);
                    RelicFx.damage(sl, owner, foe, 3.8F * p);
                    Vfx.flash(sl, to, 0xFFD070, 1.4, 7);
                    Vfx.slash(sl, to, c.face.scale(1.5).add(0, -0.3, 0), 0xFFB040, 2.0, 0.6, 7);
                    RelicFx.burst(sl, Fx.spark(0xFFD070, 0.6F), to, 10, 0.25);
                    act(c, 0.85F, 0.95F + sl.random.nextFloat() * 0.15F);
                    Vec3 kb = flat(foe.position().subtract(c.pos)).scale(0.25);
                    foe.setDeltaMovement(foe.getDeltaMovement().add(kb.x, 0.18, kb.z));
                    foe.hurtMarked = true;
                }
                if (c.age % 3 == 0 && c.vel.lengthSqr() > 0.01) {
                    sl.sendParticles(Fx.spark(0xFFE08A, 0.4F), c.pos.x, c.pos.y + 0.3, c.pos.z, 1, 0.1, 0.1, 0.1, 0.0);
                }
            }
            // ---------------------------------------------------------- gatito lunar: arañazos y visión nocturna
            case 1 -> {
                if (c.age % 100 == 0 || c.age == 2) {
                    buff(owner, MobEffects.NIGHT_VISION, 20 * 16, 0);
                }
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 1.9) {
                    c.cd = 18;
                    Vec3 to = mid(foe);
                    RelicFx.damage(sl, owner, foe, 2.3F * p);
                    for (int k = 0; k < 2; k++) {
                        Vfx.slash(sl, to, new Vec3(0.6 * (k == 0 ? 1 : -1), -0.7, 0.4), 0xDCD2FF, 1.8, k * 1.2 - 0.6, 6);
                    }
                    RelicFx.burst(sl, Fx.spark(0xC8C0FF, 0.5F), to, 8, 0.2);
                    act(c, 0.8F, 0.95F + sl.random.nextFloat() * 0.2F);
                    if (RelicFx.isNight(sl) && sl.random.nextInt(4) == 0) {
                        foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true, true));
                    }
                }
            }
            // ---------------------------------------------------------- búho sabio: marca a los enemigos
            case 2 -> {
                if (c.age % 100 == 0 || c.age == 2) {
                    buff(owner, MobEffects.LUCK, 20 * 12, 0);
                }
                if (c.cd2 <= 0) {
                    List<LivingEntity> foes = RelicFx.foes(sl, owner.position(), 16, owner);
                    if (!foes.isEmpty()) {
                        c.cd2 = 70;
                        act(c, 0.9F, 1.0F);
                        Vfx.ring(sl, c.pos.add(0, 0.2, 0), 0xE0C080, 2.2, 12);
                        int n = 0;
                        for (LivingEntity e : foes) {
                            if (n++ >= 4) {
                                break;
                            }
                            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false, true));
                            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true, true));
                            Vfx.bolt(sl, body, mid(e), 0xFFD870, 0.03, 6);
                            Vfx.flash(sl, mid(e), 0xFFE9A0, 1.0, 8);
                            RelicFx.damage(sl, owner, e, 2.0F * p);
                        }
                    }
                }
            }
            // ---------------------------------------------------------- conejito de la suerte: experiencia y botín
            case 3 -> {
                if (c.age % 100 == 0 || c.age == 2) {
                    buff(owner, MobEffects.LUCK, 20 * 14, 0);
                }
                if (c.cd2 <= 0) {
                    c.cd2 = 90;
                    Vec3 at = owner.position().add(0, 0.5, 0).add(flat(owner.getLookAngle()).scale(1.0));
                    ExperienceOrb.award(sl, at, 3 + sl.random.nextInt(4));
                    if (sl.random.nextInt(5) == 0) {
                        ItemStack drop = sl.random.nextInt(4) == 0 ? new ItemStack(Items.EMERALD, 1) : new ItemStack(Items.GOLD_NUGGET, 1 + sl.random.nextInt(3));
                        ItemEntity ie = new ItemEntity(sl, c.pos.x, c.pos.y + 0.6, c.pos.z, drop);
                        ie.setDeltaMovement((sl.random.nextDouble() - 0.5) * 0.2, 0.25, (sl.random.nextDouble() - 0.5) * 0.2);
                        sl.addFreshEntity(ie);
                    }
                    act(c, 0.8F, 1.0F);
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.pos.x, c.pos.y + 0.7, c.pos.z, 6, 0.3, 0.3, 0.3, 0.0);
                    RelicFx.burst(sl, Fx.heart(0x7CE38A, 0.5F), body.add(0, 0.3, 0), 3, 0.1);
                }
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 1.8) {
                    c.cd = 36;
                    RelicFx.damage(sl, owner, foe, 1.8F * p);
                    Vec3 kb = flat(foe.position().subtract(c.pos)).scale(0.5);
                    foe.setDeltaMovement(foe.getDeltaMovement().add(kb.x, 0.3, kb.z));
                    foe.hurtMarked = true;
                    Vfx.flash(sl, mid(foe), 0x9FFFA8, 1.0, 7);
                }
            }
            // ---------------------------------------------------------- medusa de nube: cura y descargas suaves
            case 4 -> {
                if (c.age % 100 == 0 || c.age == 2) {
                    buff(owner, MobEffects.WATER_BREATHING, 20 * 12, 0);
                }
                if (c.age % 4 == 0) {
                    sl.sendParticles(Fx.bubble(0xB8D8FF, 0.4F), c.pos.x + (sl.random.nextDouble() - 0.5) * 0.5, c.pos.y + 0.3, c.pos.z + (sl.random.nextDouble() - 0.5) * 0.5, 1, 0, -0.05, 0, 0.0);
                }
                if (c.cd2 <= 0) {
                    c.cd2 = 44;
                    boolean any = false;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 7, owner, true)) {
                        if (a.getHealth() < a.getMaxHealth() - 0.4F) {
                            a.heal(2.0F * p);
                            sl.sendParticles(Fx.heart(0xFF9BD8, 0.6F), a.getX(), a.getY() + a.getBbHeight() * 0.8, a.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
                            any = true;
                        }
                    }
                    if (any) {
                        act(c, 0.7F, 1.1F);
                        Vfx.ring(sl, c.pos.add(0, -1.0, 0), 0xB8D8FF, 2.4, 12);
                    }
                }
                if (foe != null && c.cd <= 0 && dist < 9) {
                    c.cd = 52;
                    Vec3 to = mid(foe);
                    Vfx.bolt(sl, body.add(0, -0.2, 0), to, 0xCFE6FF, 0.04, 7);
                    Vfx.flash(sl, to, 0xCFE6FF, 1.2, 8);
                    RelicFx.damage(sl, owner, foe, 2.8F * p);
                    foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true, true));
                    act(c, 0.8F, 1.0F);
                }
            }
            // ---------------------------------------------------------- fénix bebé: llamas y renacimiento
            case 5 -> {
                if (c.age % 60 == 0 || c.age == 2) {
                    buff(owner, MobEffects.FIRE_RESISTANCE, 20 * 8, 0);
                }
                if (c.age % 3 == 0) {
                    sl.sendParticles(ParticleTypes.SMALL_FLAME, c.pos.x, c.pos.y + 0.5, c.pos.z, 1, 0.2, 0.15, 0.2, 0.0);
                }
                if (!c.used && owner.getHealth() < owner.getMaxHealth() * 0.4F) {
                    c.used = true;
                    owner.heal(8.0F * p);
                    buff(owner, MobEffects.REGENERATION, 100, 1);
                    Vec3 om = owner.position().add(0, 1, 0);
                    Vfx.flash(sl, om, 0xFFB040, 3.2, 14);
                    Vfx.column(sl, owner.position(), 0xFF8A2E, 0.8, 5.0, 24);
                    Vfx.ring(sl, owner.position().add(0, 0.1, 0), 0xFF8A2E, 5.0, 18);
                    RelicFx.burst(sl, ParticleTypes.FLAME, om, 24, 0.2);
                    RelicFx.burst(sl, Fx.ember(0xFFB040, 0.8F), om, 20, 0.3);
                    RelicSounds.play(sl, om, c.s.id(), "cast", 1.0F, 1.2F);
                    for (LivingEntity e : RelicFx.foes(sl, owner.position(), 4.5, owner)) {
                        e.setSecondsOnFire(4);
                        RelicFx.damage(sl, owner, e, 3.0F * p);
                    }
                }
                if (foe != null && c.cd <= 0 && dist < 14) {
                    c.cd = 30;
                    Vec3 to = mid(foe);
                    Vfx.make(sl, VfxType.BEAM).at(body).dir(to.subtract(body)).rgb(0xFF7A1A).size(0.18).life(8).flags(VfxType.F_FLICKER | VfxType.F_CAPS).send();
                    Vfx.make(sl, VfxType.BEAM).at(body).dir(to.subtract(body)).rgb(0xFFE080).size(0.08).life(8).flags(VfxType.F_FLICKER).send();
                    RelicFx.line(sl, ParticleTypes.FLAME, body, to, 0.6);
                    Vfx.flash(sl, to, 0xFF8A2E, 1.4, 8);
                    RelicFx.damage(sl, owner, foe, 4.0F * p);
                    foe.setSecondsOnFire(4);
                    act(c, 0.8F, 1.0F);
                }
            }
            // ---------------------------------------------------------- tortuguita de cristal: escudo y pulso
            case 6 -> {
                if (c.cd2 <= 0) {
                    c.cd2 = 160;
                    int n = 0;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 8, owner, true)) {
                        a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 12, 1, false, true, true));
                        n++;
                    }
                    buff(owner, MobEffects.DAMAGE_RESISTANCE, 20 * 6, 0);
                    act(c, 0.9F, 1.0F);
                    Vfx.dome(sl, owner.position(), 0x59E0D0, 2.6, 24, true);
                    Vfx.flash(sl, owner.position().add(0, 1, 0), 0xBFFFF4, 2.2, 10);
                    RelicFx.burst(sl, Fx.shard(0x9FFFEF, 0.6F), owner.position().add(0, 1, 0), 14, 0.25);
                }
                if (foe != null && c.cd <= 0 && dist < 3.2) {
                    c.cd = 50;
                    Vfx.ring(sl, c.pos.add(0, 0.15, 0), 0x59E0D0, 3.4, 12);
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 3.2, owner)) {
                        RelicFx.damage(sl, owner, e, 2.2F * p);
                        Vec3 kb = flat(e.position().subtract(c.pos)).scale(0.7);
                        e.setDeltaMovement(e.getDeltaMovement().add(kb.x, 0.3, kb.z));
                        e.hurtMarked = true;
                    }
                    act(c, 0.7F, 1.2F);
                }
            }
            // ---------------------------------------------------------- pingüino polar: carámbanos
            case 7 -> {
                if (c.age % 100 == 0 || c.age == 2) {
                    buff(owner, MobEffects.DOLPHINS_GRACE, 20 * 8, 0);
                }
                if (c.age % 5 == 0) {
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, c.pos.x, c.pos.y + 0.6, c.pos.z, 1, 0.2, 0.2, 0.2, 0.0);
                }
                if (foe != null && c.cd <= 0 && dist < 12) {
                    c.cd = 34;
                    Vec3 to = mid(foe);
                    Vec3 from = body.add(c.face.scale(0.3));
                    Vfx.make(sl, VfxType.BEAM).at(from).dir(to.subtract(from)).rgb(0x9FE8FF).size(0.12).life(7).flags(VfxType.F_CAPS).send();
                    Vfx.flash(sl, to, 0xCFF4FF, 1.2, 8);
                    RelicFx.burst(sl, Fx.shard(0xBFEFFF, 0.5F), to, 10, 0.2);
                    RelicFx.damage(sl, owner, foe, 2.6F * p);
                    foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true));
                    foe.setTicksFrozen(Math.min(foe.getTicksRequiredToFreeze() + 60, foe.getTicksFrozen() + 80));
                    act(c, 0.8F, 0.95F + sl.random.nextFloat() * 0.2F);
                }
            }
            // ---------------------------------------------------------- unicornio bebé: sana y limpia
            case 8 -> {
                if (c.age % 3 == 0 && c.vel.lengthSqr() > 0.004) {
                    int[] rb = {0xFF6FA0, 0xFFB366, 0xFFF080, 0x80F0A0, 0x80C8FF, 0xC090FF};
                    sl.sendParticles(Fx.spark(rb[(c.age / 3) % 6], 0.45F), c.pos.x, c.pos.y + 0.4, c.pos.z, 1, 0.1, 0.1, 0.1, 0.0);
                }
                if (c.cd2 <= 0) {
                    c.cd2 = 40;
                    boolean any = false;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 8, owner, true)) {
                        if (a.getHealth() < a.getMaxHealth() - 0.4F) {
                            a.heal(2.0F * p);
                            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true, true));
                            sl.sendParticles(Fx.heart(0xFF9CF0, 0.6F), a.getX(), a.getY() + a.getBbHeight() * 0.8, a.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
                            any = true;
                        }
                    }
                    if (any) {
                        act(c, 0.8F, 1.0F);
                        Vfx.ring(sl, c.pos.add(0, 0.1, 0), 0xFF9CF0, 3.0, 14);
                    }
                }
                if (c.cd3 <= 0) {
                    c.cd3 = 110;
                    List<MobEffect> bad = new ArrayList<>();
                    for (MobEffectInstance mi : owner.getActiveEffects()) {
                        if (mi.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                            bad.add(mi.getEffect());
                        }
                    }
                    if (!bad.isEmpty()) {
                        for (MobEffect e : bad) {
                            owner.removeEffect(e);
                        }
                        Vfx.flash(sl, owner.position().add(0, 1, 0), 0xFFFFFF, 2.0, 10);
                        RelicFx.burst(sl, Fx.spark(0xFFE0FF, 0.6F), owner.position().add(0, 1, 0), 14, 0.2);
                        act(c, 0.9F, 1.25F);
                    }
                }
            }
            // ---------------------------------------------------------- pollito: picotazos, huevos y plumas
            case 11 -> {
                if (c.age % 40 == 0 && owner.fallDistance > 2.5F) {
                    buff(owner, MobEffects.SLOW_FALLING, 60, 0);
                }
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 1.7) {
                    c.cd = 20;
                    Vec3 to = mid(foe);
                    RelicFx.damage(sl, owner, foe, 1.6F * p);
                    Vfx.flash(sl, to, 0xFFE55C, 1.0, 6);
                    RelicFx.burst(sl, Fx.spark(0xFFE55C, 0.5F), to, 6, 0.2);
                    act(c, 0.8F, 1.0F + sl.random.nextFloat() * 0.2F);
                }
                if (c.cd2 <= 0) {
                    c.cd2 = 20 * 22;
                    ItemEntity ie = new ItemEntity(sl, c.pos.x, c.pos.y + 0.4, c.pos.z, new ItemStack(Items.EGG, 1 + sl.random.nextInt(2)));
                    ie.setDeltaMovement((sl.random.nextDouble() - 0.5) * 0.15, 0.2, (sl.random.nextDouble() - 0.5) * 0.15);
                    sl.addFreshEntity(ie);
                    RelicFx.burst(sl, Fx.heart(0xFFE55C, 0.5F), body.add(0, 0.3, 0), 3, 0.1);
                    act(c, 0.7F, 1.2F);
                }
            }
            // ---------------------------------------------------------- rana: lengua pegajosa
            case 12 -> {
                if (c.age % 60 == 0 || c.age == 2) {
                    buff(owner, MobEffects.JUMP, 100, 1);
                }
                if (foe != null && c.cd <= 0 && dist < 7 && dist > 1.2) {
                    c.cd = 44;
                    Vec3 to = mid(foe);
                    Vec3 mouth = body.add(c.face.scale(0.35));
                    Vfx.make(sl, VfxType.BEAM).at(mouth).dir(to.subtract(mouth)).rgb(0xFF7A9A).size(0.07).life(6).flags(VfxType.F_CAPS).send();
                    RelicFx.damage(sl, owner, foe, 2.0F * p);
                    Vec3 pull = flat(c.pos.subtract(foe.position())).scale(0.55);
                    foe.setDeltaMovement(foe.getDeltaMovement().add(pull.x, 0.15, pull.z));
                    foe.hurtMarked = true;
                    Vfx.flash(sl, to, 0xFFB0C0, 1.0, 6);
                    act(c, 0.85F, 1.0F);
                }
            }
            // ---------------------------------------------------------- hámster: comida y semillas
            case 13 -> {
                if (c.cd2 <= 0 && owner.getFoodData().needsFood()) {
                    c.cd2 = 160;
                    owner.getFoodData().eat(2, 0.5F);
                    RelicFx.burst(sl, Fx.heart(0xFFBC66, 0.5F), body.add(0, 0.3, 0), 3, 0.1);
                    act(c, 0.7F, 1.1F);
                }
                if (foe != null && c.cd <= 0 && dist < 9) {
                    c.cd = 30;
                    Vec3 to = mid(foe);
                    Vec3 from = body.add(c.face.scale(0.3));
                    Vfx.make(sl, VfxType.BEAM).at(from).dir(to.subtract(from)).rgb(0xFFE070).size(0.05).life(5).flags(VfxType.F_CAPS).send();
                    RelicFx.damage(sl, owner, foe, 1.8F * p);
                    Vfx.flash(sl, to, 0xFFE070, 0.9, 6);
                    act(c, 0.75F, 1.1F + sl.random.nextFloat() * 0.2F);
                }
            }
            // ---------------------------------------------------------- caracol: rastro pegajoso
            case 14 -> {
                if (c.age % 4 == 0) {
                    sl.sendParticles(Fx.bubble(0xB8F0A0, 0.35F), c.pos.x, c.pos.y + 0.08, c.pos.z, 1, 0.1, 0.0, 0.1, 0.0);
                }
                if (c.age % 20 == 0) {
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 4.5, owner)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true, true));
                    }
                }
                if (c.cd2 <= 0 && owner.getHealth() < owner.getMaxHealth() && c.pos.distanceTo(owner.position()) < 6) {
                    c.cd2 = 70;
                    owner.heal(1.5F * p);
                    sl.sendParticles(Fx.heart(0xFFB0E8, 0.5F), owner.getX(), owner.getY() + 1.6, owner.getZ(), 3, 0.3, 0.2, 0.3, 0.02);
                    act(c, 0.6F, 1.0F);
                }
            }
            // ---------------------------------------------------------- abeja: aguijón y polinización
            case 15 -> {
                if (c.age % 100 == 0 || c.age == 2) {
                    buff(owner, MobEffects.DIG_SPEED, 120, 0);
                }
                if (foe != null && c.cd <= 0 && dist < 10) {
                    c.cd = 26;
                    Vec3 to = mid(foe);
                    Vfx.bolt(sl, body, to, 0xFFD020, 0.03, 5);
                    Vfx.flash(sl, to, 0xFFE070, 1.0, 6);
                    RelicFx.damage(sl, owner, foe, 2.2F * p);
                    foe.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true, true));
                    act(c, 0.75F, 1.0F + sl.random.nextFloat() * 0.2F);
                }
                if (c.cd2 <= 0) {
                    c.cd2 = 100;
                    for (int tries = 0; tries < 12; tries++) {
                        BlockPos bp = owner.blockPosition().offset(sl.random.nextInt(13) - 6, sl.random.nextInt(5) - 2, sl.random.nextInt(13) - 6);
                        BlockState st = sl.getBlockState(bp);
                        if ((st.getBlock() instanceof CropBlock || st.getBlock() instanceof StemBlock) && BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), sl, bp)) {
                            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, bp.getX() + 0.5, bp.getY() + 0.6, bp.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
                            act(c, 0.6F, 1.2F);
                            break;
                        }
                    }
                }
                if (c.age % 5 == 0) {
                    sl.sendParticles(Fx.spark(0xFFCC33, 0.3F), c.pos.x, c.pos.y + 0.3, c.pos.z, 1, 0.15, 0.1, 0.15, 0.0);
                }
            }
            // ---------------------------------------------------------- oveja de nube: lana protectora
            case 16 -> {
                if (c.cd2 <= 0) {
                    c.cd2 = 20 * 18;
                    owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 12, 0, true, false, true));
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 8, owner, false)) {
                        a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 10, 0, false, true, true));
                    }
                    Vfx.ring(sl, c.pos.add(0, 0.1, 0), 0xE8F0FF, 3.0, 14);
                    Vfx.flash(sl, owner.position().add(0, 1, 0), 0xFFFFFF, 1.8, 9);
                    RelicFx.burst(sl, ParticleTypes.CLOUD, owner.position().add(0, 1, 0), 10, 0.1);
                    act(c, 0.8F, 1.0F);
                }
                if (c.age % 60 == 0) {
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 5, owner)) {
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true, true));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, true, true));
                    }
                }
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 2.2) {
                    c.cd = 40;
                    RelicFx.damage(sl, owner, foe, 2.6F * p);
                    Vec3 kb = flat(foe.position().subtract(c.pos)).scale(0.8);
                    foe.setDeltaMovement(foe.getDeltaMovement().add(kb.x, 0.3, kb.z));
                    foe.hurtMarked = true;
                    Vfx.flash(sl, mid(foe), 0xFFFFFF, 1.2, 6);
                    act(c, 0.7F, 1.15F);
                }
            }
            // ---------------------------------------------------------- erizo: púas
            case 17 -> {
                LivingEntity at = owner.getLastHurtByMob();
                if (owner.hurtTime == 9 && at != null && at.isAlive() && c.cd3 <= 0 && at.distanceTo(owner) < 6) {
                    c.cd3 = 30;
                    RelicFx.damage(sl, owner, at, 3.0F * p);
                    Vec3 kb = flat(at.position().subtract(owner.position())).scale(0.6);
                    at.setDeltaMovement(at.getDeltaMovement().add(kb.x, 0.25, kb.z));
                    at.hurtMarked = true;
                    Vfx.bolt(sl, body, mid(at), 0xE0B890, 0.03, 5);
                    Vfx.flash(sl, mid(at), 0xE0B890, 1.2, 6);
                    act(c, 0.9F, 1.0F);
                }
                if (foe != null && c.cd <= 0 && dist < 3.5) {
                    c.cd = 46;
                    Vfx.ring(sl, c.pos.add(0, 0.15, 0), 0xE0B890, 3.4, 12);
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 3.2, owner)) {
                        RelicFx.damage(sl, owner, e, 2.4F * p);
                        Vec3 kb = flat(e.position().subtract(c.pos)).scale(0.5);
                        e.setDeltaMovement(e.getDeltaMovement().add(kb.x, 0.2, kb.z));
                        e.hurtMarked = true;
                    }
                    RelicFx.burst(sl, Fx.shard(0xE0B890, 0.5F), c.pos.add(0, 0.2, 0), 12, 0.3);
                    act(c, 0.85F, 1.0F);
                }
            }
            // ---------------------------------------------------------- ajolote: regeneración
            case 18 -> {
                if (c.age % 40 == 0 && owner.isInWaterOrBubble()) {
                    buff(owner, MobEffects.REGENERATION, 80, 0);
                    buff(owner, MobEffects.WATER_BREATHING, 120, 0);
                }
                if (c.cd2 <= 0 && owner.getHealth() < owner.getMaxHealth() - 0.5F) {
                    c.cd2 = 80;
                    owner.heal(1.5F * p);
                    sl.sendParticles(Fx.heart(0xFF8FB0, 0.5F), owner.getX(), owner.getY() + 1.6, owner.getZ(), 3, 0.3, 0.2, 0.3, 0.02);
                    act(c, 0.6F, 1.1F);
                }
                if (!c.used && owner.getHealth() < owner.getMaxHealth() * 0.5F) {
                    c.used = true;
                    buff(owner, MobEffects.REGENERATION, 20 * 10, 1);
                    Vfx.ring(sl, owner.position().add(0, 0.1, 0), 0xFF8FB0, 3.6, 16);
                    Vfx.flash(sl, owner.position().add(0, 1, 0), 0xFFC0D4, 2.0, 10);
                    RelicSounds.play(sl, owner.position(), c.s.id(), "cast", 0.9F, 1.1F);
                }
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 2.0) {
                    c.cd = 26;
                    RelicFx.damage(sl, owner, foe, 3.0F * p);
                    RelicFx.burst(sl, Fx.bubble(0xFFB0C8, 0.5F), mid(foe), 8, 0.2);
                    act(c, 0.8F, 1.0F);
                }
            }
            // ---------------------------------------------------------- mapache: recoge objetos y roba botín
            case 19 -> {
                if (c.age % 10 == 0) {
                    Vec3 tw = owner.position().add(0, 0.6, 0);
                    for (ItemEntity ie : sl.getEntitiesOfClass(ItemEntity.class, new AABB(c.pos.subtract(7, 3, 7), c.pos.add(7, 3, 7)))) {
                        if (!ie.hasPickUpDelay() && ie.position().distanceTo(tw) > 1.5) {
                            Vec3 dv = tw.subtract(ie.position()).normalize().scale(0.35);
                            ie.setDeltaMovement(dv.x, dv.y + 0.1, dv.z);
                            ie.hurtMarked = true;
                        }
                    }
                }
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 1.9) {
                    c.cd = 26;
                    Vec3 to = mid(foe);
                    RelicFx.damage(sl, owner, foe, 2.8F * p);
                    Vfx.slash(sl, to, c.face.scale(1.4).add(0, -0.3, 0), 0xC8CCE8, 1.8, 0.4, 6);
                    if (sl.random.nextInt(3) == 0) {
                        ItemStack loot = sl.random.nextInt(5) == 0 ? new ItemStack(Items.EMERALD) : new ItemStack(Items.GOLD_NUGGET, 1 + sl.random.nextInt(3));
                        ItemEntity ie = new ItemEntity(sl, to.x, to.y, to.z, loot);
                        ie.setDeltaMovement(0, 0.25, 0);
                        sl.addFreshEntity(ie);
                    }
                    act(c, 0.85F, 1.0F);
                }
                if (c.cd2 <= 0) {
                    c.cd2 = 20 * 14;
                    ExperienceOrb.award(sl, owner.position().add(0, 0.5, 0), 4 + sl.random.nextInt(4));
                    act(c, 0.6F, 1.3F);
                }
            }
            // ---------------------------------------------------------- ciervo lunar: luz de luna
            case 20 -> {
                if (c.age % 40 == 0 || c.age == 2) {
                    buff(owner, MobEffects.MOVEMENT_SPEED, 80, 1);
                    if (RelicFx.isNight(sl)) {
                        buff(owner, MobEffects.NIGHT_VISION, 260, 0);
                    }
                }
                if (c.cd2 <= 0) {
                    c.cd2 = 60;
                    boolean any = false;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 9, owner, true)) {
                        if (a.getHealth() < a.getMaxHealth() - 0.4F) {
                            a.heal(2.5F * p);
                            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true, true));
                            sl.sendParticles(Fx.heart(0xA0C8FF, 0.6F), a.getX(), a.getY() + a.getBbHeight() * 0.8, a.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
                            any = true;
                        }
                    }
                    if (any) {
                        Vfx.ring(sl, c.pos.add(0, 0.1, 0), 0xA0C8FF, 3.8, 16);
                        act(c, 0.85F, 1.0F);
                    }
                }
                if (foe != null && c.cd <= 0 && dist < 12) {
                    c.cd = 32;
                    Vec3 to = mid(foe);
                    Vec3 from = c.pos.add(0, c.s.size() * 1.1, 0);
                    Vfx.make(sl, VfxType.BEAM).at(from).dir(to.subtract(from)).rgb(0xA8C8FF).size(0.12).life(7).flags(VfxType.F_CAPS | VfxType.F_FLICKER).send();
                    Vfx.flash(sl, to, 0xE0ECFF, 1.4, 8);
                    RelicFx.damage(sl, owner, foe, 3.6F * p);
                    act(c, 0.85F, 1.1F);
                }
            }
            // ---------------------------------------------------------- ballena estelar: canto y géiser
            case 21 -> {
                if (c.age % 100 == 0 || c.age == 2) {
                    buff(owner, MobEffects.WATER_BREATHING, 200, 0);
                }
                if (c.cd2 <= 0) {
                    c.cd2 = 20 * 7;
                    int n = 0;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 12, owner, true)) {
                        a.heal(3.5F * p);
                        a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 0, false, true, true));
                        sl.sendParticles(Fx.heart(0x8FB8FF, 0.6F), a.getX(), a.getY() + a.getBbHeight() * 0.8, a.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
                        n++;
                    }
                    Vfx.ring(sl, c.pos.add(0, -1.0, 0), 0x6A9CFF, 6.0, 20);
                    Vfx.flash(sl, body, 0xBFD8FF, 2.6, 12);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 0.9F, 1.0F);
                }
                if (foe != null && c.cd <= 0 && dist < 10) {
                    c.cd = 60;
                    Vec3 at = foe.position();
                    Vfx.column(sl, at, 0x7FB0FF, 0.7, 4.0, 14);
                    Vfx.ring(sl, at.add(0, 0.1, 0), 0x7FB0FF, 4.6, 14);
                    RelicFx.shakeNear(sl, at, 10, 0.5F, 8);
                    for (LivingEntity e : RelicFx.foes(sl, at, 4.5, owner)) {
                        RelicFx.damage(sl, owner, e, 4.0F * p);
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.9, 0));
                        e.hurtMarked = true;
                    }
                    act(c, 1.0F, 1.0F);
                }
            }
            // ---------------------------------------------------------- zorro de nueve colas: llamas de chakra
            case 22 -> {
                if (c.age % 60 == 0 || c.age == 2) {
                    buff(owner, MobEffects.MOVEMENT_SPEED, 120, 1);
                    buff(owner, MobEffects.DAMAGE_BOOST, 120, 0);
                }
                if (foe != null && c.cd <= 0 && dist < 16) {
                    c.cd = 18;
                    Vec3 to = mid(foe);
                    double a = c.age * 0.3;
                    Vec3 from = body.add(Math.cos(a) * 0.5, 0.2, Math.sin(a) * 0.5);
                    Vfx.bolt(sl, from, to, 0xFF7A2E, 0.05, 6);
                    Vfx.flash(sl, to, 0xFFD0A0, 1.3, 8);
                    RelicFx.damage(sl, owner, foe, 4.4F * p);
                    foe.setSecondsOnFire(3);
                    act(c, 0.85F, 0.95F + sl.random.nextFloat() * 0.2F);
                }
                if (!c.used && owner.getHealth() < owner.getMaxHealth() * 0.35F) {
                    c.used = true;
                    owner.heal(10.0F * p);
                    buff(owner, MobEffects.REGENERATION, 100, 1);
                    buff(owner, MobEffects.DAMAGE_RESISTANCE, 160, 1);
                    Vec3 om = owner.position().add(0, 1, 0);
                    Vfx.flash(sl, om, 0xFFD0A0, 3.4, 14);
                    Vfx.column(sl, owner.position(), 0xFF7A2E, 0.8, 5.0, 24);
                    Vfx.ring(sl, owner.position().add(0, 0.1, 0), 0xFF7A2E, 5.0, 18);
                    RelicSounds.play(sl, om, c.s.id(), "cast", 1.0F, 1.1F);
                    for (LivingEntity e : RelicFx.foes(sl, owner.position(), 6.0, owner)) {
                        e.setSecondsOnFire(4);
                        RelicFx.damage(sl, owner, e, 4.0F * p);
                    }
                }
            }
            // ---------------------------------------------------------- panda guardián: porrazo
            case 9 -> {
                if (c.age % 100 == 0 || c.age == 2) {
                    buff(owner, MobEffects.DAMAGE_RESISTANCE, 20 * 8, 0);
                    owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 10, 0, true, false, true));
                }
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 2.4) {
                    c.cd = 54;
                    Vec3 at = foe.position();
                    Vfx.ring(sl, at.add(0, 0.1, 0), 0x9AD86A, 4.4, 14);
                    Vfx.flash(sl, at.add(0, 0.3, 0), 0xFFFFFF, 2.0, 8);
                    RelicFx.burst(sl, RelicFx.blockDust(RelicFx.groundState(sl, at.x, at.y - 0.5, at.z)), at.add(0, 0.2, 0), 18, 0.25);
                    RelicFx.shakeNear(sl, at, 10, 0.6F, 8);
                    for (LivingEntity e : RelicFx.foes(sl, at, 3.2, owner)) {
                        RelicFx.damage(sl, owner, e, 4.5F * p);
                        Vec3 kb = flat(e.position().subtract(at)).scale(0.6);
                        e.setDeltaMovement(e.getDeltaMovement().add(kb.x, 0.45, kb.z));
                        e.hurtMarked = true;
                    }
                    act(c, 1.0F, 0.95F);
                }
            }
            default -> behave5(c, owner, foe, dist);
        }
    }

    // ------------------------------------------------------------------ lote 5

    private static void lightning(ServerLevel sl, Vec3 a, Vec3 b, int rgb) {
        Vfx.bolt(sl, a, b, rgb, 0.05, 6);
        Vfx.flash(sl, b, rgb, 1.2, 7);
    }

    private static void behave5(C c, ServerPlayer owner, LivingEntity foe, double dist) {
        ServerLevel sl = c.level;
        Vec3 body = c.pos.add(0, c.s.size() * 0.5, 0);
        float p = c.power;
        switch (c.s.species()) {
            // ---------------------------------------------------------- capibara: calma para todos
            case 23 -> {
                if (c.age % 100 == 0 || c.age == 2) {
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 6, owner, true)) {
                        a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, false, true));
                    }
                }
                if (c.cd2 <= 0) {
                    c.cd2 = 20 * 9;
                    boolean any = false;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 8, owner, true)) {
                        if (a.getHealth() < a.getMaxHealth() - 0.4F) {
                            a.heal(3.0F * p);
                            sl.sendParticles(Fx.heart(0xC89A62, 0.6F), a.getX(), a.getY() + a.getBbHeight() * 0.8, a.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
                            any = true;
                        }
                    }
                    if (any) {
                        Vfx.ring(sl, c.pos.add(0, 0.1, 0), 0xE8C890, 3.4, 16);
                        act(c, 0.8F, 1.0F);
                    }
                }
                if (c.age % 8 == 0) {
                    sl.sendParticles(Fx.spark(0xFFB040, 0.3F), c.pos.x, c.pos.y + 0.7, c.pos.z, 1, 0.1, 0.05, 0.1, 0.0);
                }
            }
            // ---------------------------------------------------------- patito: picotazos y saltos
            case 24 -> {
                if (c.cd2 <= 0) {
                    c.cd2 = 20 * 8;
                    buff(owner, MobEffects.MOVEMENT_SPEED, 20 * 6, 0);
                    buff(owner, MobEffects.JUMP, 20 * 6, 1);
                    act(c, 0.7F, 1.2F);
                    RelicFx.burst(sl, Fx.spark(0xFFE04A, 0.5F), owner.position().add(0, 0.2, 0), 8, 0.2);
                }
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 1.8) {
                    c.cd = 18;
                    Vec3 to = mid(foe);
                    RelicFx.damage(sl, owner, foe, 2.4F * p);
                    Vfx.flash(sl, to, 0xFFE04A, 1.0, 6);
                    RelicFx.burst(sl, Fx.spark(0xFFE04A, 0.5F), to, 6, 0.2);
                    act(c, 0.8F, 0.95F + sl.random.nextFloat() * 0.2F);
                }
            }
            // ---------------------------------------------------------- babosa: salto aplastante y gelatina
            case 25 -> {
                if (c.cd2 <= 0) {
                    c.cd2 = 20 * 16;
                    owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 10, 0, true, false, true));
                    RelicFx.burst(sl, Fx.bubble(0x7CE06A, 0.5F), owner.position().add(0, 1, 0), 8, 0.15);
                    act(c, 0.6F, 1.2F);
                }
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 2.6) {
                    c.cd = 44;
                    Vec3 at = foe.position();
                    Vfx.ring(sl, at.add(0, 0.1, 0), 0x7CE06A, 3.4, 12);
                    RelicFx.burst(sl, Fx.bubble(0x9CF08A, 0.5F), at.add(0, 0.3, 0), 12, 0.25);
                    for (LivingEntity e : RelicFx.foes(sl, at, 2.4, owner)) {
                        RelicFx.damage(sl, owner, e, 3.4F * p);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1, false, true, true));
                    }
                    act(c, 0.9F, 1.0F);
                }
            }
            // ---------------------------------------------------------- corgi: ladrido valiente
            case 26 -> {
                if (owner.hurtTime == 9 && c.cd3 <= 0) {
                    c.cd3 = 20 * 6;
                    buff(owner, MobEffects.DAMAGE_BOOST, 20 * 6, 0);
                    Vfx.flash(sl, owner.position().add(0, 1, 0), 0xF5A64C, 1.6, 8);
                    act(c, 0.9F, 1.1F);
                }
                if (c.age % 10 == 0) {
                    Vec3 tw = owner.position().add(0, 0.6, 0);
                    for (ItemEntity ie : sl.getEntitiesOfClass(ItemEntity.class, new AABB(c.pos.subtract(6, 3, 6), c.pos.add(6, 3, 6)))) {
                        if (!ie.hasPickUpDelay() && ie.position().distanceTo(tw) > 1.5) {
                            Vec3 dv = tw.subtract(ie.position()).normalize().scale(0.3);
                            ie.setDeltaMovement(dv.x, dv.y + 0.1, dv.z);
                            ie.hurtMarked = true;
                        }
                    }
                }
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 1.9) {
                    c.cd = 22;
                    Vec3 to = mid(foe);
                    RelicFx.damage(sl, owner, foe, 3.2F * p);
                    Vfx.slash(sl, to, c.face.scale(1.4).add(0, -0.3, 0), 0xFFC880, 1.6, 0.3, 6);
                    Vec3 kb = flat(foe.position().subtract(c.pos)).scale(0.3);
                    foe.setDeltaMovement(foe.getDeltaMovement().add(kb.x, 0.15, kb.z));
                    foe.hurtMarked = true;
                    act(c, 0.9F, 0.95F + sl.random.nextFloat() * 0.15F);
                }
            }
            // ---------------------------------------------------------- creepercito: explosión sin romper nada
            case 27 -> {
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 3.2) {
                    c.cd = 20 * 3;
                    Vec3 at = c.pos.add(0, 0.5, 0);
                    sl.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                    Vfx.flash(sl, at, 0xFFF0A0, 3.0, 9);
                    Vfx.ring(sl, c.pos.add(0, 0.1, 0), 0x8CE070, 5.0, 12);
                    RelicFx.burst(sl, ParticleTypes.POOF, at, 18, 0.3);
                    RelicFx.shakeNear(sl, at, 10, 0.5F, 6);
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 3.6, owner)) {
                        RelicFx.damage(sl, owner, e, 8.0F * p);
                        Vec3 kb = flat(e.position().subtract(c.pos)).scale(0.9);
                        e.setDeltaMovement(e.getDeltaMovement().add(kb.x, 0.5, kb.z));
                        e.hurtMarked = true;
                    }
                    act(c, 1.0F, 1.0F);
                }
                if (c.age % 5 == 0) {
                    sl.sendParticles(Fx.spark(0x7CE06A, 0.3F), c.pos.x, c.pos.y + 0.5, c.pos.z, 1, 0.15, 0.2, 0.15, 0.0);
                }
            }
            // ---------------------------------------------------------- muñeco de nieve: bolas de nieve y escarcha
            case 28 -> {
                if (owner.isOnFire() && c.cd2 <= 0) {
                    c.cd2 = 20 * 8;
                    owner.clearFire();
                    RelicFx.burst(sl, ParticleTypes.SNOWFLAKE, owner.position().add(0, 1, 0), 14, 0.15);
                    act(c, 0.7F, 1.2F);
                }
                if (c.age % 5 == 0) {
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, c.pos.x, c.pos.y + 0.7, c.pos.z, 1, 0.25, 0.3, 0.25, 0.0);
                }
                if (c.age % 50 == 0) {
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 5, owner)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0, false, true, true));
                    }
                }
                if (foe != null && c.cd <= 0 && dist < 12) {
                    c.cd = 16;
                    Vec3 to = mid(foe);
                    Vec3 from = body.add(0, 0.2, 0);
                    Vfx.make(sl, VfxType.BEAM).at(from).dir(to.subtract(from)).rgb(0xE8F6FF).size(0.08).life(5).flags(VfxType.F_CAPS).send();
                    Vfx.flash(sl, to, 0xFFFFFF, 1.0, 6);
                    RelicFx.burst(sl, ParticleTypes.SNOWFLAKE, to, 8, 0.15);
                    RelicFx.damage(sl, owner, foe, 2.4F * p);
                    foe.setTicksFrozen(Math.min(foe.getTicksRequiredToFreeze() + 40, foe.getTicksFrozen() + 50));
                    act(c, 0.75F, 1.0F + sl.random.nextFloat() * 0.2F);
                }
            }
            // ---------------------------------------------------------- fantasmita: susto y velo
            case 29 -> {
                if (c.cd2 <= 0) {
                    c.cd2 = 20 * 14;
                    owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 10, 1, true, false, true));
                    buff(owner, MobEffects.DAMAGE_RESISTANCE, 20 * 5, 0);
                    Vfx.ring(sl, owner.position().add(0, 0.1, 0), 0xB8D0FF, 3.2, 14);
                    Vfx.flash(sl, owner.position().add(0, 1, 0), 0xDCE8FF, 1.8, 9);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 0.7F, 1.2F);
                }
                if (foe != null && c.cd <= 0 && dist < 9) {
                    c.cd = 54;
                    Vec3 at = foe.position();
                    Vfx.ring(sl, at.add(0, 0.1, 0), 0xB8D0FF, 4.4, 12);
                    Vfx.flash(sl, mid(foe), 0xDCE8FF, 1.8, 8);
                    for (LivingEntity e : RelicFx.foes(sl, at, 4.0, owner)) {
                        RelicFx.damage(sl, owner, e, 2.4F * p);
                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true, true));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true, true));
                    }
                    act(c, 0.9F, 1.0F);
                }
            }
            // ---------------------------------------------------------- golemcito: solo pasiva (resistencia)
            case 30 -> {
                if (c.age % 60 == 0 || c.age == 2) {
                    buff(owner, MobEffects.DAMAGE_RESISTANCE, 100, 0);
                }
                // puñetazo de hierro: lanza al enemigo por los aires
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 3.0) {
                    c.cd = 36;
                    RelicFx.damage(sl, owner, foe, 5.0F * p);
                    Vec3 kb = flat(foe.position().subtract(c.pos)).scale(0.7);
                    foe.setDeltaMovement(foe.getDeltaMovement().add(kb.x, 0.35, kb.z));
                    foe.hurtMarked = true;
                    Vfx.flash(sl, mid(foe), 0xDDE4F0, 1.6, 7);
                    RelicFx.shakeNear(sl, mid(foe), 8, 0.3F, 5);
                    act(c, 1.0F, 0.85F);
                }
            }
            // ---------------------------------------------------------- ratón relámpago: rayo en cadena
            case 31 -> {
                if (c.age % 40 == 0 || c.age == 2) {
                    buff(owner, MobEffects.DIG_SPEED, 80, 1);
                }
                if (foe != null && c.cd <= 0 && dist < 14) {
                    c.cd = 30;
                    Vec3 prev = body.add(0, 0.2, 0);
                    LivingEntity cur = foe;
                    List<LivingEntity> hit = new ArrayList<>();
                    for (int k = 0; k < 3 && cur != null; k++) {
                        hit.add(cur);
                        Vec3 to = mid(cur);
                        lightning(sl, prev, to, 0xFFE040);
                        RelicFx.damage(sl, owner, cur, (k == 0 ? 4.0F : 2.4F) * p);
                        prev = to;
                        LivingEntity next = null;
                        double best = 6.0;
                        for (LivingEntity e : RelicFx.foes(sl, to, 6.0, owner)) {
                            if (!hit.contains(e) && e.position().distanceTo(to) < best) {
                                best = e.position().distanceTo(to);
                                next = e;
                            }
                        }
                        cur = next;
                    }
                    act(c, 0.9F, 0.95F + sl.random.nextFloat() * 0.2F);
                }
            }
            // ---------------------------------------------------------- droide astromecánico: repara, escanea y dispara
            case 32 -> {
                if (c.cd2 <= 0) {
                    c.cd2 = 20 * 6;
                    boolean fixed = false;
                    List<ItemStack> gear = new ArrayList<>();
                    gear.add(owner.getMainHandItem());
                    gear.add(owner.getOffhandItem());
                    owner.getArmorSlots().forEach(gear::add);
                    for (ItemStack st : gear) {
                        if (!st.isEmpty() && st.isDamaged()) {
                            st.setDamageValue(Math.max(0, st.getDamageValue() - Math.round(3 * p)));
                            fixed = true;
                        }
                    }
                    if (fixed) {
                        Vfx.ring(sl, owner.position().add(0, 0.1, 0), 0x5C9CFF, 2.6, 12);
                        RelicFx.burst(sl, Fx.spark(0x9CD0FF, 0.5F), owner.position().add(0, 1, 0), 10, 0.2);
                        act(c, 0.6F, 1.4F);
                    }
                }
                if (c.cd3 <= 0) {
                    c.cd3 = 20 * 5;
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 14, owner)) {
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                    }
                }
                if (foe != null && c.cd <= 0 && dist < 11) {
                    c.cd = 24;
                    Vec3 to = mid(foe);
                    Vec3 from = body.add(0, 0.3, 0);
                    Vfx.make(sl, VfxType.BEAM).at(from).dir(to.subtract(from)).rgb(0x6CC0FF).size(0.07).life(6).flags(VfxType.F_CAPS | VfxType.F_FLICKER).send();
                    Vfx.flash(sl, to, 0xBFE4FF, 1.2, 7);
                    RelicFx.damage(sl, owner, foe, 3.6F * p);
                    act(c, 0.8F, 1.0F + sl.random.nextFloat() * 0.2F);
                }
            }
            // ---------------------------------------------------------- espíritu del bosque: hace crecer y protege
            case 33 -> {
                if (c.age % 40 == 0 || c.age == 2) {
                    buff(owner, MobEffects.REGENERATION, 60, 0);
                    buff(owner, MobEffects.DAMAGE_RESISTANCE, 60, 0);
                }
                if (c.age % 50 == 0) {
                    BlockPos origin = BlockPos.containing(c.pos);
                    for (int tries = 0; tries < 10; tries++) {
                        BlockPos bp = origin.offset(sl.random.nextInt(13) - 6, sl.random.nextInt(3) - 1, sl.random.nextInt(13) - 6);
                        BlockState st = sl.getBlockState(bp);
                        if (st.getBlock() instanceof CropBlock cb && !cb.isMaxAge(st)) {
                            cb.growCrops(sl, bp, st);
                            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.0);
                            break;
                        }
                    }
                }
                if (c.cd2 <= 0) {
                    c.cd2 = 20 * 20;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 8, owner, true)) {
                        a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 12, 1, false, true, true));
                    }
                    Vfx.ring(sl, c.pos.add(0, 0.1, 0), 0x7CE070, 5.0, 18);
                    Vfx.column(sl, c.pos, 0x7CE070, 0.8, 4.0, 16);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 0.8F, 1.0F);
                }
                if (foe != null && c.cd <= 0 && dist < 8) {
                    c.cd = 46;
                    Vec3 at = foe.position();
                    Vfx.ring(sl, at.add(0, 0.1, 0), 0x7CE070, 4.2, 12);
                    RelicFx.burst(sl, ParticleTypes.COMPOSTER, at.add(0, 0.3, 0), 14, 0.25);
                    for (LivingEntity e : RelicFx.foes(sl, at, 3.2, owner)) {
                        RelicFx.damage(sl, owner, e, 3.6F * p);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true, true));
                    }
                    act(c, 0.9F, 1.0F);
                }
            }
            // ---------------------------------------------------------- lobo de luna: mordida y aullido
            case 34 -> {
                if (c.age % 40 == 0 || c.age == 2) {
                    buff(owner, MobEffects.MOVEMENT_SPEED, 80, 0);
                    if (RelicFx.isNight(sl)) {
                        buff(owner, MobEffects.NIGHT_VISION, 260, 0);
                    }
                }
                if (c.cd2 <= 0 && foe != null) {
                    c.cd2 = 20 * 24;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 10, owner, true)) {
                        a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 8, 0, false, true, true));
                        a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 8, 0, false, true, true));
                    }
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 9, owner)) {
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 6, 0, false, false));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 5, 0, false, true, true));
                    }
                    Vfx.ring(sl, c.pos.add(0, 0.1, 0), 0xA8D8FF, 6.0, 18);
                    Vfx.column(sl, c.pos, 0xA8D8FF, 0.7, 5.0, 20);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 1.0F, 1.0F);
                }
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 2.1) {
                    c.cd = 24;
                    Vec3 to = mid(foe);
                    float dmg = 4.0F * p * (RelicFx.isNight(sl) ? 1.5F : 1.0F);
                    RelicFx.damage(sl, owner, foe, dmg);
                    Vfx.slash(sl, to, c.face.scale(1.5).add(0, -0.3, 0), 0xCFE8FF, 2.0, 0.5, 7);
                    Vfx.flash(sl, to, 0xCFE8FF, 1.3, 7);
                    foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true));
                    act(c, 0.9F, 0.95F + sl.random.nextFloat() * 0.15F);
                }
            }
            // ---------------------------------------------------------- dragón de la noche: plasma y grito
            case 35 -> {
                if (c.age % 40 == 0 || c.age == 2) {
                    buff(owner, MobEffects.SLOW_FALLING, 80, 0);
                    buff(owner, MobEffects.MOVEMENT_SPEED, 80, 0);
                }
                if (foe != null && c.cd <= 0 && dist < 18) {
                    c.cd = 36;
                    Vec3 to = mid(foe);
                    Vec3 from = body.add(c.face.scale(0.5));
                    Vfx.make(sl, VfxType.BEAM).at(from).dir(to.subtract(from)).rgb(0x8A7AFF).size(0.16).life(8).flags(VfxType.F_CAPS | VfxType.F_FLICKER).send();
                    sl.sendParticles(ParticleTypes.EXPLOSION, to.x, to.y, to.z, 1, 0, 0, 0, 0);
                    Vfx.flash(sl, to, 0xC8B8FF, 2.6, 9);
                    Vfx.ring(sl, foe.position().add(0, 0.1, 0), 0x8A7AFF, 4.0, 12);
                    RelicFx.shakeNear(sl, to, 10, 0.4F, 6);
                    for (LivingEntity e : RelicFx.foes(sl, to, 2.8, owner)) {
                        RelicFx.damage(sl, owner, e, (e == foe ? 7.0F : 4.0F) * p);
                    }
                    act(c, 1.0F, 1.0F);
                }
                if (c.cd2 <= 0 && foe != null && dist < 9) {
                    c.cd2 = 20 * 20;
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 8, owner)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3, false, true, true));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, true, true));
                        RelicFx.damage(sl, owner, e, 3.0F * p);
                    }
                    Vfx.ring(sl, c.pos.add(0, -0.5, 0), 0x8A7AFF, 7.0, 18);
                    Vfx.flash(sl, body, 0xC8B8FF, 3.2, 12);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 1.0F, 1.0F);
                }
                if (c.age % 4 == 0) {
                    sl.sendParticles(Fx.spark(0x8A7AFF, 0.35F), c.pos.x, c.pos.y + 0.6, c.pos.z, 1, 0.3, 0.2, 0.3, 0.0);
                }
            }
            default -> behave6(c, owner, foe, dist);
        }
    }

    // ------------------------------------------------------------------ lote 6

    private static void behave6(C c, ServerPlayer owner, LivingEntity foe, double dist) {
        ServerLevel sl = c.level;
        Vec3 body = c.pos.add(0, c.s.size() * 0.5, 0);
        float p = c.power;
        switch (c.s.species()) {
            // ---------------------------------------------------------- pulpo: tinta y tentáculo
            case 36 -> {
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 2.4) {
                    c.cd = 26;
                    RelicFx.damage(sl, owner, foe, 3.5F * p);
                    Vfx.slash(sl, mid(foe), c.face.scale(1.3).add(0, 0.2, 0), 0xD08AE0, 1.8, 0.5, 7);
                    act(c, 0.9F, 1.0F);
                }
                if (c.cd2 <= 0 && foe != null && dist < 14) {
                    c.cd2 = 20 * 14;
                    Vec3 at = foe.position().add(0, 0.8, 0);
                    for (LivingEntity e : RelicFx.foes(sl, at, 4.0, owner)) {
                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, true, true));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true, true));
                    }
                    RelicFx.burst(sl, Fx.spark(0x2A1840, 0.9F), at, 40, 0.18);
                    Vfx.ring(sl, foe.position().add(0, 0.1, 0), 0x4A2870, 4.0, 14);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 1.0F, 1.0F);
                }
            }
            // ---------------------------------------------------------- mariposa: polen que sana
            case 37 -> {
                // Fosu (mítica): bendición de luz. Cura y limpia males de los aliados y ciega un poco a los enemigos cercanos.
                if (c.age % 5 == 0) {
                    sl.sendParticles(Fx.spark(0x9ED8FF, 0.3F), c.pos.x, c.pos.y + 1.0, c.pos.z, 1, 0.3, 0.2, 0.3, 0.0);
                }
                if (c.cd2 <= 0) {
                    boolean any = false;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 8, owner, true)) {
                        boolean hurt = a.getHealth() < a.getMaxHealth() - 0.4F;
                        net.minecraft.world.effect.MobEffectInstance bad = null;
                        for (net.minecraft.world.effect.MobEffectInstance ei : a.getActiveEffects()) {
                            if (ei.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                                bad = ei;
                                break;
                            }
                        }
                        if (hurt || bad != null) {
                            if (hurt) {
                                a.heal(5.0F * p);
                                a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true, true));
                            }
                            if (bad != null) {
                                a.removeEffect(bad.getEffect());
                            }
                            sl.sendParticles(Fx.heart(0x5CB4FF, 0.6F), a.getX(), a.getY() + a.getBbHeight() * 0.8, a.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
                            any = true;
                        }
                    }
                    if (any) {
                        c.cd2 = 20 * 7;
                        Vfx.ring(sl, c.pos.add(0, -0.8, 0), 0x9ED8FF, 5.0, 18);
                        Vfx.flash(sl, c.pos.add(0, 0.2, 0), 0xCFEFFF, 2.0, 10);
                        RelicFx.burst(sl, Fx.spark(0xCFEFFF, 0.4F), c.pos.add(0, 0.6, 0), 30, 0.35);
                        RelicSounds.play(sl, c.pos, c.s.id(), "cast", 0.9F, 1.0F);
                    } else {
                        c.cd2 = 20;
                    }
                }
                if (foe != null && c.cd <= 0 && dist < 12) {
                    c.cd = 30;
                    Vec3 to = mid(foe);
                    Vfx.bolt(sl, body.add(0, 0.1, 0), to, 0x9ED8FF, 0.04, 6);
                    Vfx.flash(sl, to, 0xCFEFFF, 1.2, 7);
                    RelicFx.damage(sl, owner, foe, 3.0F * p);
                    act(c, 0.8F, 1.1F);
                }
                if (foe != null && c.cd3 <= 0 && c.pos.distanceTo(foe.position()) < 9) {
                    c.cd3 = 20 * 8;
                    for (LivingEntity e : RelicFx.foes(sl, foe.position(), 4, owner)) {
                        RelicFx.damage(sl, owner, e, 2.5F * p);
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, true, true));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0, false, true, true));
                        Vfx.flash(sl, mid(e), 0xCFEFFF, 1.2, 6);
                    }
                    Vfx.ring(sl, foe.position().add(0, 0.1, 0), 0x9ED8FF, 4.0, 14);
                    RelicSounds.play(sl, c.pos, c.s.id(), "act", 0.9F, 1.0F);
                }
            }
            // ---------------------------------------------------------- cangrejo: pinza y caparazón
            case 38 -> {
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 1.9) {
                    c.cd = 22;
                    RelicFx.damage(sl, owner, foe, 3.0F * p);
                    foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, true, true));
                    Vfx.flash(sl, mid(foe), 0xFF8A70, 1.1, 6);
                    act(c, 0.9F, 1.0F);
                }
                if (c.cd2 <= 0 && foe != null) {
                    c.cd2 = 20 * 18;
                    buff(owner, MobEffects.ABSORPTION, 20 * 10, 1);
                    Vfx.ring(sl, owner.position().add(0, 0.1, 0), 0xFF8A70, 2.4, 14);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 0.9F, 1.0F);
                }
            }
            // ---------------------------------------------------------- luciérnaga: luz que marca
            case 39 -> {
                if (c.age % 5 == 0) {
                    sl.sendParticles(Fx.spark(0xD8FF50, 0.3F), c.pos.x, c.pos.y + 0.7, c.pos.z, 1, 0.2, 0.2, 0.2, 0.0);
                }
                if (foe != null && c.cd <= 0 && dist < 12) {
                    c.cd = 40;
                    Vec3 to = mid(foe);
                    Vec3 from = body.add(0, 0.1, 0);
                    Vfx.make(sl, VfxType.BEAM).at(from).dir(to.subtract(from)).rgb(0xD8FF50).size(0.07).life(6).flags(VfxType.F_CAPS | VfxType.F_FLICKER).send();
                    RelicFx.damage(sl, owner, foe, 2.5F * p);
                    Vfx.flash(sl, to, 0xF0FFA0, 1.2, 6);
                    act(c, 0.7F, 1.0F);
                }
                if (c.cd2 <= 0 && foe != null && dist < 14) {
                    c.cd2 = 20 * 16;
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 10, owner)) {
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 8, 0, false, false));
                        RelicFx.damage(sl, owner, e, 1.5F * p);
                    }
                    Vfx.ring(sl, c.pos.add(0, -0.5, 0), 0xD8FF50, 8.0, 18);
                    Vfx.flash(sl, body, 0xF0FFA0, 3.0, 10);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 1.0F, 1.0F);
                }
            }
            // ---------------------------------------------------------- koala: abrazo curativo
            case 40 -> {
                if (c.cd2 <= 0) {
                    boolean any = false;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 8, owner, true)) {
                        if (a.getHealth() < a.getMaxHealth() - 0.4F) {
                            a.heal(4.0F * p);
                            sl.sendParticles(Fx.heart(0x9AD88A, 0.6F), a.getX(), a.getY() + a.getBbHeight() * 0.8, a.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
                            any = true;
                        }
                    }
                    if (any) {
                        c.cd2 = 20 * 12;
                        Vfx.ring(sl, c.pos.add(0, 0.1, 0), 0x9AD88A, 4.0, 16);
                        RelicSounds.play(sl, c.pos, c.s.id(), "cast", 0.9F, 1.0F);
                    } else {
                        c.cd2 = 20;
                    }
                }
                if (c.age % 10 == 0) {
                    sl.sendParticles(Fx.spark(0x9AD88A, 0.3F), c.pos.x, c.pos.y + 0.9, c.pos.z, 1, 0.2, 0.1, 0.2, 0.0);
                }
            }
            // ---------------------------------------------------------- elefante: chorro de agua
            case 41 -> {
                if (c.cd2 <= 0 && foe != null && dist < 14) {
                    c.cd2 = 20 * 12;
                    Vec3 to = mid(foe);
                    Vec3 from = body.add(c.face.scale(0.8)).add(0, 0.3, 0);
                    Vfx.make(sl, VfxType.BEAM).at(from).dir(to.subtract(from)).rgb(0x60C0FF).size(0.18).life(10).flags(VfxType.F_CAPS).send();
                    RelicFx.burst(sl, Fx.spark(0x80D0FF, 0.5F), to, 40, 0.25);
                    for (LivingEntity e : RelicFx.foes(sl, to, 4.0, owner)) {
                        RelicFx.damage(sl, owner, e, 3.0F * p);
                        Vec3 kb = flat(e.position().subtract(c.pos)).scale(0.7);
                        e.setDeltaMovement(e.getDeltaMovement().add(kb.x, 0.3, kb.z));
                        e.hurtMarked = true;
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true));
                    }
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 8, owner, true)) {
                        a.heal(2.0F * p);
                    }
                    Vfx.ring(sl, to.add(0, -0.7, 0), 0x60C0FF, 4.0, 14);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 1.0F, 1.0F);
                }
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 2.6) {
                    c.cd = 34;
                    RelicFx.damage(sl, owner, foe, 4.5F * p);
                    Vfx.flash(sl, mid(foe), 0xC8C8F0, 1.4, 7);
                    act(c, 1.0F, 0.9F);
                }
            }
            // ---------------------------------------------------------- cachorro de león: mordida y rugido
            case 42 -> {
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 2.2) {
                    c.cd = 26;
                    RelicFx.damage(sl, owner, foe, 5.0F * p);
                    Vfx.slash(sl, mid(foe), c.face.scale(1.5).add(0, -0.3, 0), 0xFFD070, 2.0, 0.5, 7);
                    Vfx.flash(sl, mid(foe), 0xFFD070, 1.3, 7);
                    act(c, 0.9F, 1.0F);
                }
                if (c.cd2 <= 0 && foe != null && dist < 10) {
                    c.cd2 = 20 * 22;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 10, owner, true)) {
                        a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 8, 0, false, true, true));
                    }
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 9, owner)) {
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 5, 0, false, true, true));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 3, 1, false, true, true));
                    }
                    Vfx.ring(sl, c.pos.add(0, 0.1, 0), 0xFFB84A, 7.0, 18);
                    Vfx.column(sl, c.pos, 0xFFD070, 0.6, 4.0, 18);
                    RelicFx.shakeNear(sl, c.pos, 10, 0.5F, 8);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 1.0F, 1.0F);
                }
            }
            // ---------------------------------------------------------- salamandra: aliento de fuego
            case 43 -> {
                if (foe != null && c.cd <= 0 && dist < 9) {
                    c.cd = 34;
                    Vec3 to = mid(foe);
                    Vec3 from = body.add(c.face.scale(0.5));
                    Vfx.make(sl, VfxType.BEAM).at(from).dir(to.subtract(from)).rgb(0xFF7A20).size(0.16).life(8).flags(VfxType.F_CAPS | VfxType.F_FLICKER).send();
                    RelicFx.burst(sl, ParticleTypes.FLAME, to, 16, 0.12);
                    for (LivingEntity e : RelicFx.foes(sl, to, 2.4, owner)) {
                        RelicFx.damage(sl, owner, e, (e == foe ? 4.5F : 3.0F) * p);
                        e.setSecondsOnFire(4);
                    }
                    Vfx.flash(sl, to, 0xFFA040, 1.6, 8);
                    act(c, 1.0F, 1.0F);
                }
                if (c.cd2 <= 0 && foe != null && dist < 8) {
                    c.cd2 = 20 * 20;
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 5, owner)) {
                        RelicFx.damage(sl, owner, e, 3.0F * p);
                        e.setSecondsOnFire(5);
                    }
                    Vfx.ring(sl, c.pos.add(0, 0.1, 0), 0xFF6A28, 5.0, 16);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 1.0F, 1.0F);
                }
                if (c.age % 6 == 0) {
                    sl.sendParticles(ParticleTypes.FLAME, c.pos.x, c.pos.y + 0.5, c.pos.z, 1, 0.2, 0.1, 0.2, 0.01);
                }
            }
            // ---------------------------------------------------------- pavo real: abanico deslumbrante
            case 44 -> {
                if (c.cd2 <= 0 && foe != null && dist < 12) {
                    c.cd2 = 20 * 20;
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 9, owner)) {
                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, true, true));
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 6, 0, false, false));
                    }
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 9, owner, true)) {
                        a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 12, 0, false, true, true));
                    }
                    Vfx.ring(sl, c.pos.add(0, 0.1, 0), 0x30C8D8, 7.0, 18);
                    Vfx.flash(sl, body.add(0, 0.5, 0), 0xC0FFF0, 3.4, 12);
                    RelicFx.burst(sl, Fx.spark(0x40E0D0, 0.5F), body.add(0, 0.5, 0), 30, 0.35);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 1.0F, 1.0F);
                }
                if (c.age % 6 == 0) {
                    sl.sendParticles(Fx.spark(0x40E0D0, 0.3F), c.pos.x, c.pos.y + 1.2, c.pos.z - c.face.z * 0.5, 1, 0.4, 0.3, 0.2, 0.0);
                }
            }
            // ---------------------------------------------------------- delfín: onda sonora
            case 45 -> {
                if (foe != null && c.cd <= 0 && dist < 14) {
                    c.cd = 40;
                    Vec3 to = mid(foe);
                    Vfx.ring(sl, body.add(c.face.scale(0.8)), 0x6AC8FF, 2.6, 10);
                    Vfx.ring(sl, to, 0x9AE0FF, 2.6, 10);
                    RelicFx.damage(sl, owner, foe, 4.0F * p);
                    Vec3 kb = flat(foe.position().subtract(c.pos)).scale(0.6);
                    foe.setDeltaMovement(foe.getDeltaMovement().add(kb.x, 0.25, kb.z));
                    foe.hurtMarked = true;
                    act(c, 0.9F, 1.0F);
                }
                if (c.cd2 <= 0) {
                    boolean any = false;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 8, owner, true)) {
                        if (a.getHealth() < a.getMaxHealth() - 0.4F) {
                            a.heal(3.0F * p);
                            a.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100, 0, false, true, true));
                            sl.sendParticles(Fx.heart(0x6AC8FF, 0.6F), a.getX(), a.getY() + a.getBbHeight() * 0.8, a.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
                            any = true;
                        }
                    }
                    if (any) {
                        c.cd2 = 20 * 16;
                        Vfx.ring(sl, c.pos.add(0, -1.0, 0), 0x6AC8FF, 5.0, 16);
                        RelicSounds.play(sl, c.pos, c.s.id(), "cast", 0.9F, 1.0F);
                    } else {
                        c.cd2 = 20;
                    }
                }
            }
            // ---------------------------------------------------------- pegaso: lanza de luz y viento
            case 46 -> {
                if (foe != null && c.cd <= 0 && dist < 16) {
                    c.cd = 34;
                    Vec3 to = mid(foe);
                    Vec3 from = body.add(c.face.scale(0.6)).add(0, 0.4, 0);
                    Vfx.make(sl, VfxType.BEAM).at(from).dir(to.subtract(from)).rgb(0xFFF4C0).size(0.12).life(7).flags(VfxType.F_CAPS | VfxType.F_FLICKER).send();
                    Vfx.flash(sl, to, 0xFFF4C0, 2.0, 8);
                    for (LivingEntity e : RelicFx.foes(sl, to, 2.2, owner)) {
                        RelicFx.damage(sl, owner, e, (e == foe ? 6.0F : 3.5F) * p);
                    }
                    act(c, 1.0F, 1.0F);
                }
                if (c.cd2 <= 0 && foe != null && dist < 14) {
                    c.cd2 = 20 * 20;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 10, owner, true)) {
                        a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 8, 1, false, true, true));
                        a.addEffect(new MobEffectInstance(MobEffects.JUMP, 20 * 8, 1, false, true, true));
                        a.heal(2.0F * p);
                    }
                    Vfx.ring(sl, c.pos.add(0, -1.0, 0), 0xFFF4C0, 7.0, 18);
                    Vfx.column(sl, c.pos, 0xFFF4C0, 0.7, 5.0, 18);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 1.0F, 1.0F);
                }
                if (c.age % 5 == 0) {
                    sl.sendParticles(Fx.spark(0xFFF4C0, 0.3F), c.pos.x, c.pos.y + 0.8, c.pos.z, 1, 0.4, 0.2, 0.4, 0.0);
                }
            }
            // ---------------------------------------------------------- dragón celestial: rayo y lluvia sanadora
            case 47 -> {
                if (foe != null && c.cd <= 0 && dist < 20) {
                    c.cd = 30;
                    Vec3 to = mid(foe);
                    lightning(sl, to.add(0, 6, 0), to, 0x70FFE0);
                    sl.sendParticles(ParticleTypes.EXPLOSION, to.x, to.y, to.z, 1, 0, 0, 0, 0);
                    RelicFx.shakeNear(sl, to, 10, 0.4F, 6);
                    for (LivingEntity e : RelicFx.foes(sl, to, 2.8, owner)) {
                        RelicFx.damage(sl, owner, e, (e == foe ? 7.0F : 4.0F) * p);
                    }
                    act(c, 1.0F, 1.0F);
                }
                if (c.cd2 <= 0 && foe != null && dist < 14) {
                    c.cd2 = 20 * 24;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 10, owner, true)) {
                        a.heal(4.0F * p);
                        a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 6, 1, false, true, true));
                        sl.sendParticles(Fx.heart(0x40E0B8, 0.6F), a.getX(), a.getY() + a.getBbHeight() * 0.8, a.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
                    }
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 8, owner)) {
                        RelicFx.damage(sl, owner, e, 3.0F * p);
                    }
                    Vfx.ring(sl, c.pos.add(0, -1.5, 0), 0x40E0B8, 8.0, 20);
                    Vfx.column(sl, c.pos, 0x70FFE0, 0.8, 6.0, 22);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 1.0F, 1.0F);
                }
                if (c.age % 4 == 0) {
                    sl.sendParticles(Fx.spark(0x70FFE0, 0.35F), c.pos.x, c.pos.y + 0.8, c.pos.z, 1, 0.5, 0.3, 0.5, 0.0);
                }
            }
            // ---------------------------------------------------------- Cookie (hurón): mordisquito y baile de guerra
            case 48 -> {
                if (foe != null && c.cd <= 0 && c.pos.distanceTo(foe.position()) < 1.8) {
                    c.cd = 14;
                    RelicFx.damage(sl, owner, foe, 3.0F * p);
                    Vfx.flash(sl, mid(foe), 0xE4E8F4, 1.1, 6);
                    act(c, 0.9F, 1.0F);
                }
                if (c.cd2 <= 0 && foe != null && dist < 10) {
                    c.cd2 = 20 * 14;
                    for (LivingEntity a : RelicFx.allies(sl, c.pos, 8, owner, true)) {
                        a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 8, 1, false, true, true));
                        a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 8, 1, false, true, true));
                    }
                    for (LivingEntity e : RelicFx.foes(sl, c.pos, 5, owner)) {
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 5, 1, false, true, true));
                    }
                    Vfx.ring(sl, c.pos.add(0, 0.1, 0), 0xB4BAC6, 5.0, 16);
                    RelicFx.burst(sl, Fx.spark(0xE4E8F4, 0.4F), c.pos.add(0, 0.5, 0), 24, 0.3);
                    RelicSounds.play(sl, c.pos, c.s.id(), "cast", 1.0F, 1.0F);
                }
            }
            default -> {
            }
        }
    }
}
