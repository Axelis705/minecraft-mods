package com.mundosdeaxelis.relic;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ficha de cada mascota cuando es una criatura de verdad (etapa 254): si pelea y cómo, qué hace en su menú, su comida favorita,
 * sus sonidos (del juego, con tono propio), su caja de choque (medida con el modelo 3D) y su velocidad.
 * La especie es el número del modelo de {@link com.mundosdeaxelis.vfx.Creature3D} (la misma que {@link Companions.Spec#species()}).
 */
public final class PetKinds {
    private PetKinds() {
    }

    /** Cómo pelea: no pelea, a mordiscos (cuerpo a cuerpo) o disparando desde lejos. */
    public enum Attack { NONE, BITE, BOLT }

    /** Sabor de su ataque (lo que le pasa al enemigo además del daño). */
    public enum Element { NONE, FIRE, ICE, SHOCK, STAR, INK, TOAST, CRYSTAL, MAGIC, LEECH, SEED, EARTH }

    /**
     * Acción especial de su menú: mochila, luz que te sigue, hacer crecer plantas, recoger lo que hay en el suelo, regalo del
     * día, curarte, hechizo sorpresa, rugido que espanta y señalar el camino a casa.
     */
    public enum Special { NONE, BAG, LIGHT, GROW, FETCH, GIFT, HEAL, SPELL, ROAR, GUIDE }

    public static final class Kind {
        public final int species;
        public final Attack attack;
        public final Element element;
        public final int rgb;
        public final Special special;
        /** Ranuras de la mochila (solo {@link Special#BAG}). */
        public final int bag;
        private final Supplier<Item> food;
        private final SoundEvent amb, hurt, death;
        public final float pitch;
        /** Caja de choque (ancho y alto) y cuánto se baja el modelo para que su parte de abajo quede en el suelo de la caja. */
        public final float w, h, yOff;
        public final double speed;
        public final boolean fly, fireImmune;

        Kind(int species, Attack attack, Element element, int rgb, Special special, int bag, Supplier<Item> food,
             SoundEvent amb, SoundEvent hurt, SoundEvent death, float pitch, float w, float h, float yOff, double speed,
             boolean fly, boolean fireImmune) {
            this.species = species;
            this.attack = attack;
            this.element = element;
            this.rgb = rgb;
            this.special = special;
            this.bag = bag;
            this.food = food;
            this.amb = amb;
            this.hurt = hurt;
            this.death = death;
            this.pitch = pitch;
            this.w = w;
            this.h = h;
            this.yOff = yOff;
            this.speed = speed;
            this.fly = fly;
            this.fireImmune = fireImmune;
        }

        public Item food() {
            return food.get();
        }

        public SoundEvent ambient() {
            return amb;
        }

        public SoundEvent hurtSound() {
            return hurt;
        }

        public SoundEvent deathSound() {
            return death;
        }

        public boolean combat() {
            return attack != Attack.NONE;
        }

        /** Clave de texto de su acción especial. */
        public String specialKey() {
            return special.name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final Map<Integer, Kind> BY = new HashMap<>();

    public static Kind of(int species) {
        if (BY.isEmpty()) {
            init();
        }
        return BY.get(species);
    }

    // Atajos para que la tabla se lea bien
    private static final Attack N = Attack.NONE, B = Attack.BITE, R = Attack.BOLT;
    private static final Element E0 = Element.NONE;

    private static void k(int sp, Attack at, Element el, int rgb, Special spc, int bag, Supplier<Item> food, SoundEvent amb, SoundEvent hurt,
                          SoundEvent death, float pitch, float w, float h, float yOff, double speed, boolean fly, boolean fire) {
        BY.put(sp, new Kind(sp, at, el, rgb, spc, bag, food, amb, hurt, death, pitch, w, h, yOff, speed, fly, fire));
    }

    private static synchronized void init() {
        if (!BY.isEmpty()) {
            return;
        }
        SoundEvent bit = SoundEvents.NOTE_BLOCK_BIT.value();
        Special S0 = Special.NONE;
        // ---------------------------------------------------------------- de suelo
        k(0, B, Element.EARTH, 0x8EC4F0, Special.GIFT, 0, () -> Items.COD, SoundEvents.DOLPHIN_AMBIENT, SoundEvents.DOLPHIN_HURT, SoundEvents.DOLPHIN_DEATH, 1.35F, 0.62F, 0.56F, 0, 0.32, false, false);
        k(1, N, E0, 0xB8B0FF, Special.GIFT, 0, () -> Items.COD, SoundEvents.CAT_AMBIENT, SoundEvents.CAT_HURT, SoundEvents.CAT_DEATH, 1.15F, 0.42F, 0.61F, 0, 0.31, false, false);
        k(3, N, E0, 0x7CE38A, Special.GIFT, 0, () -> Items.CARROT, SoundEvents.RABBIT_AMBIENT, SoundEvents.RABBIT_HURT, SoundEvents.RABBIT_DEATH, 1.2F, 0.42F, 0.57F, 0, 0.31, false, false);
        k(6, N, E0, 0x59E0D0, Special.BAG, 9, () -> Items.SEAGRASS, SoundEvents.TURTLE_AMBIENT_LAND, SoundEvents.TURTLE_HURT, SoundEvents.TURTLE_DEATH, 1.3F, 0.52F, 0.53F, 0, 0.21, false, false);
        k(7, N, E0, 0x9FE8FF, Special.GUIDE, 0, () -> Items.COD, SoundEvents.PARROT_AMBIENT, SoundEvents.PARROT_HURT, SoundEvents.PARROT_DEATH, 0.8F, 0.42F, 0.77F, 0, 0.26, false, false);
        k(9, R, Element.INK, 0x5A3A8A, Special.GIFT, 0, () -> Items.TROPICAL_FISH, SoundEvents.SQUID_AMBIENT, SoundEvents.SQUID_HURT, SoundEvents.SQUID_DEATH, 1.45F, 0.52F, 0.67F, 0, 0.25, false, false);
        k(11, N, E0, 0xFFE55C, Special.GROW, 0, () -> Items.BONE_MEAL, SoundEvents.SLIME_SQUISH_SMALL, SoundEvents.SLIME_HURT_SMALL, SoundEvents.SLIME_DEATH_SMALL, 1.4F, 0.42F, 0.55F, 0, 0.26, false, false);
        k(12, N, E0, 0x6CE070, Special.GROW, 0, () -> Items.SLIME_BALL, SoundEvents.FROG_AMBIENT, SoundEvents.FROG_HURT, SoundEvents.FROG_DEATH, 1.25F, 0.42F, 0.5F, 0, 0.28, false, false);
        k(13, N, E0, 0xFFBC66, Special.GIFT, 0, () -> Items.PUMPKIN_SEEDS, SoundEvents.RABBIT_AMBIENT, SoundEvents.RABBIT_HURT, SoundEvents.RABBIT_DEATH, 1.7F, 0.42F, 0.45F, 0, 0.28, false, false);
        k(14, N, E0, 0xFFB0E8, Special.BAG, 9, () -> Items.BEETROOT, SoundEvents.TURTLE_AMBIENT_LAND, SoundEvents.TURTLE_HURT, SoundEvents.TURTLE_DEATH, 1.7F, 0.42F, 0.47F, 0, 0.18, false, false);
        k(16, N, E0, 0xE8F0FF, Special.GIFT, 0, () -> Items.WHEAT, SoundEvents.GOAT_AMBIENT, SoundEvents.GOAT_HURT, SoundEvents.GOAT_DEATH, 1.35F, 0.42F, 0.67F, 0, 0.31, false, false);
        k(17, R, Element.TOAST, 0xD49A52, Special.GIFT, 0, () -> Items.BREAD, bit, SoundEvents.IRON_GOLEM_HURT, SoundEvents.ITEM_BREAK, 1.6F, 0.48F, 0.62F, 0, 0.27, false, false);
        k(18, N, E0, 0xFF8FB0, Special.HEAL, 0, () -> Items.TROPICAL_FISH, SoundEvents.AXOLOTL_IDLE_AIR, SoundEvents.AXOLOTL_HURT, SoundEvents.AXOLOTL_DEATH, 1.1F, 0.54F, 0.42F, 0, 0.25, false, false);
        k(22, R, Element.FIRE, 0x7FB8FF, S0, 0, () -> Items.SWEET_BERRIES, SoundEvents.FOX_AMBIENT, SoundEvents.FOX_HURT, SoundEvents.FOX_DEATH, 1.0F, 0.85F, 1.07F, 0, 0.33, false, true);
        k(23, N, E0, 0xC89A62, Special.HEAL, 0, () -> Items.MELON_SLICE, SoundEvents.PANDA_AMBIENT, SoundEvents.PANDA_HURT, SoundEvents.PANDA_DEATH, 1.45F, 0.45F, 0.68F, 0, 0.24, false, false);
        k(24, N, E0, 0xFFE04A, Special.BAG, 9, () -> Items.WHEAT_SEEDS, SoundEvents.CHICKEN_AMBIENT, SoundEvents.CHICKEN_HURT, SoundEvents.CHICKEN_DEATH, 1.25F, 0.42F, 0.48F, 0, 0.27, false, false);
        k(25, N, E0, 0x7CE06A, Special.GIFT, 0, () -> Items.SWEET_BERRIES, SoundEvents.SLIME_SQUISH_SMALL, SoundEvents.SLIME_HURT_SMALL, SoundEvents.SLIME_DEATH_SMALL, 1.2F, 0.42F, 0.4F, 0, 0.26, false, false);
        k(26, B, E0, 0xF5A64C, Special.FETCH, 0, () -> Items.BONE, SoundEvents.WOLF_AMBIENT, SoundEvents.WOLF_HURT, SoundEvents.WOLF_DEATH, 1.3F, 0.42F, 0.66F, 0, 0.33, false, false);
        k(27, N, E0, 0x6FD060, Special.GIFT, 0, () -> Items.GUNPOWDER, SoundEvents.CREEPER_PRIMED, SoundEvents.CREEPER_HURT, SoundEvents.CREEPER_DEATH, 1.7F, 0.42F, 0.68F, 0, 0.28, false, false);
        k(28, N, E0, 0xDDF0FF, Special.GIFT, 0, () -> Items.CARROT, SoundEvents.SNOW_GOLEM_AMBIENT, SoundEvents.SNOW_GOLEM_HURT, SoundEvents.SNOW_GOLEM_DEATH, 1.35F, 0.62F, 0.79F, 0, 0.25, false, false);
        k(30, B, E0, 0xC8CEDA, Special.GIFT, 0, () -> Items.IRON_INGOT, SoundEvents.IRON_GOLEM_STEP, SoundEvents.IRON_GOLEM_HURT, SoundEvents.IRON_GOLEM_DEATH, 1.45F, 0.62F, 0.94F, 0, 0.26, false, false);
        k(31, R, Element.SHOCK, 0xFFE04A, Special.LIGHT, 0, () -> Items.GLOW_BERRIES, SoundEvents.RABBIT_AMBIENT, SoundEvents.RABBIT_HURT, SoundEvents.RABBIT_DEATH, 1.9F, 0.42F, 0.61F, 0, 0.31, false, false);
        k(32, N, E0, 0x5C9CFF, Special.LIGHT, 0, () -> Items.REDSTONE, bit, SoundEvents.IRON_GOLEM_HURT, SoundEvents.ITEM_BREAK, 1.2F, 0.42F, 0.57F, 0, 0.27, false, false);
        k(33, N, E0, 0x9AB4C8, Special.GROW, 0, () -> Items.APPLE, SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.ALLAY_HURT, SoundEvents.ALLAY_DEATH, 1.2F, 0.45F, 0.96F, 0, 0.29, false, false);
        k(34, B, Element.FIRE, 0x7FC8FF, Special.FETCH, 0, () -> Items.COOKED_BEEF, SoundEvents.WOLF_AMBIENT, SoundEvents.WOLF_HURT, SoundEvents.WOLF_DEATH, 0.95F, 0.75F, 0.74F, 0, 0.33, false, true);
        k(36, N, E0, 0xD08AE0, Special.GIFT, 0, () -> Items.COD, SoundEvents.SQUID_AMBIENT, SoundEvents.SQUID_HURT, SoundEvents.SQUID_DEATH, 1.3F, 0.45F, 0.56F, 0, 0.24, false, false);
        k(38, N, E0, 0xFF6A50, Special.GIFT, 0, () -> Items.KELP, SoundEvents.TURTLE_AMBIENT_LAND, SoundEvents.TURTLE_HURT, SoundEvents.TURTLE_DEATH, 1.6F, 0.45F, 0.43F, 0, 0.26, false, false);
        k(41, N, E0, 0xB8B8D8, Special.BAG, 18, () -> Items.HAY_BLOCK, SoundEvents.SNIFFER_IDLE, SoundEvents.SNIFFER_HURT, SoundEvents.SNIFFER_DEATH, 1.25F, 0.8F, 0.81F, 0, 0.26, false, false);
        k(42, B, E0, 0xFFB84A, Special.ROAR, 0, () -> Items.BEEF, SoundEvents.OCELOT_AMBIENT, SoundEvents.OCELOT_HURT, SoundEvents.OCELOT_DEATH, 0.8F, 0.42F, 0.67F, 0, 0.32, false, false);
        k(44, N, E0, 0x34D8A0, Special.GIFT, 0, () -> Items.MELON_SEEDS, SoundEvents.PARROT_AMBIENT, SoundEvents.PARROT_HURT, SoundEvents.PARROT_DEATH, 1.0F, 0.7F, 0.64F, 0, 0.29, false, false);
        k(48, N, E0, 0xE0892F, Special.FETCH, 0, () -> Items.COOKIE, SoundEvents.FOX_SNIFF, SoundEvents.FOX_HURT, SoundEvents.FOX_DEATH, 1.6F, 0.5F, 0.35F, 0, 0.34, false, false);
        k(49, B, E0, 0xC88A3A, Special.BAG, 18, () -> Items.GOLD_NUGGET, SoundEvents.CHEST_CLOSE, SoundEvents.SHULKER_HURT_CLOSED, SoundEvents.CHEST_LOCKED, 1.3F, 0.42F, 0.59F, 0, 0.27, false, false);
        k(51, N, E0, 0xFF9EB8, Special.BAG, 9, () -> Items.CARROT, SoundEvents.PIG_AMBIENT, SoundEvents.PIG_HURT, SoundEvents.PIG_DEATH, 1.35F, 0.42F, 0.44F, 0, 0.27, false, false);
        k(52, N, E0, 0x7A3CC8, Special.SPELL, 0, () -> Items.SWEET_BERRIES, SoundEvents.FOX_AMBIENT, SoundEvents.FOX_HURT, SoundEvents.FOX_DEATH, 1.35F, 0.42F, 0.56F, 0, 0.28, false, false);
        k(53, N, E0, 0xB59CF0, Special.GIFT, 0, () -> Items.COOKIE, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundEvents.ALLAY_HURT, SoundEvents.ALLAY_DEATH, 1.0F, 0.42F, 0.62F, 0, 0.27, false, false);
        k(54, N, E0, 0x7C98C0, Special.GIFT, 0, () -> Items.SALMON, SoundEvents.DOLPHIN_PLAY, SoundEvents.DOLPHIN_HURT, SoundEvents.DOLPHIN_DEATH, 1.2F, 0.42F, 0.55F, 0, 0.24, false, false);
        k(55, N, E0, 0x8E6CD8, Special.SPELL, 0, () -> Items.PUMPKIN_PIE, SoundEvents.VEX_AMBIENT, SoundEvents.VEX_HURT, SoundEvents.VEX_DEATH, 1.5F, 0.42F, 0.71F, 0, 0.3, false, false);
        k(61, B, E0, 0xFF7C98, S0, 0, () -> Items.BEETROOT, SoundEvents.SNIFFER_IDLE, SoundEvents.SNIFFER_HURT, SoundEvents.SNIFFER_DEATH, 1.65F, 0.42F, 0.48F, 0, 0.28, false, false);
        k(62, B, Element.ICE, 0x9AEAFF, Special.GIFT, 0, () -> Items.SNOWBALL, SoundEvents.POLAR_BEAR_AMBIENT_BABY, SoundEvents.POLAR_BEAR_HURT, SoundEvents.POLAR_BEAR_DEATH, 1.2F, 0.48F, 0.55F, 0, 0.28, false, false);
        k(63, R, Element.SEED, 0x5CD66C, Special.GROW, 0, () -> Items.BONE_MEAL, SoundEvents.SLIME_SQUISH_SMALL, SoundEvents.SLIME_HURT_SMALL, SoundEvents.SLIME_DEATH_SMALL, 1.5F, 0.42F, 0.47F, 0, 0.26, false, false);
        k(64, B, Element.FIRE, 0xFF8A2A, Special.GIFT, 0, () -> Items.COAL, SoundEvents.ANVIL_LAND, SoundEvents.IRON_GOLEM_HURT, SoundEvents.ANVIL_DESTROY, 2.0F, 0.46F, 0.52F, 0, 0.26, false, true);
        k(65, B, Element.ICE, 0x9AEAFF, Special.FETCH, 0, () -> Items.MUTTON, SoundEvents.WOLF_AMBIENT, SoundEvents.WOLF_HURT, SoundEvents.WOLF_DEATH, 1.45F, 0.42F, 0.51F, 0, 0.33, false, false);
        k(66, R, Element.MAGIC, 0x8A7AFF, Special.SPELL, 0, () -> Items.LAPIS_LAZULI, SoundEvents.EVOKER_AMBIENT, SoundEvents.EVOKER_HURT, SoundEvents.EVOKER_DEATH, 1.8F, 0.47F, 0.73F, 0, 0.28, false, false);
        k(67, R, Element.FIRE, 0xFF6A1A, Special.LIGHT, 0, () -> Items.MAGMA_CREAM, SoundEvents.MAGMA_CUBE_SQUISH_SMALL, SoundEvents.MAGMA_CUBE_HURT_SMALL, SoundEvents.MAGMA_CUBE_DEATH_SMALL, 1.2F, 0.42F, 0.42F, 0, 0.27, false, true);
        // ---------------------------------------------------------------- voladoras (flotan cerca de ti; al sentarse se posan)
        k(4, R, Element.SHOCK, 0xBFE6FF, Special.GROW, 0, () -> Items.SUGAR, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundEvents.ALLAY_HURT, SoundEvents.ALLAY_DEATH, 0.9F, 0.75F, 0.66F, 0.15F, 0.28, true, false);
        k(5, R, Element.STAR, 0x9FD8FF, Special.LIGHT, 0, () -> Items.SALMON, SoundEvents.CAT_AMBIENT, SoundEvents.CAT_HURT, SoundEvents.CAT_DEATH, 1.4F, 0.42F, 0.59F, 0.2F, 0.3, true, false);
        k(15, N, E0, 0xFFCC33, Special.GROW, 0, () -> Items.DANDELION, SoundEvents.BEE_POLLINATE, SoundEvents.BEE_HURT, SoundEvents.BEE_DEATH, 1.2F, 0.42F, 0.4F, 0.19F, 0.3, true, false);
        k(19, B, Element.LEECH, 0xC02A4A, S0, 0, () -> Items.SWEET_BERRIES, SoundEvents.BAT_AMBIENT, SoundEvents.BAT_HURT, SoundEvents.BAT_DEATH, 1.3F, 0.7F, 0.45F, 0.22F, 0.3, true, false);
        k(21, N, E0, 0x6A9CFF, Special.BAG, 18, () -> Items.DRIED_KELP, SoundEvents.DOLPHIN_AMBIENT_WATER, SoundEvents.DOLPHIN_HURT, SoundEvents.DOLPHIN_DEATH, 0.55F, 0.9F, 1.04F, 0.36F, 0.26, true, false);
        k(29, N, E0, 0xB8D0FF, Special.LIGHT, 0, () -> Items.COOKIE, SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundEvents.ALLAY_HURT, SoundEvents.ALLAY_DEATH, 1.2F, 0.42F, 0.48F, 0.17F, 0.28, true, false);
        k(35, R, Element.FIRE, 0x8A6AFF, S0, 0, () -> Items.COOKED_SALMON, SoundEvents.ENDER_DRAGON_AMBIENT, SoundEvents.ENDER_DRAGON_HURT, SoundEvents.PHANTOM_DEATH, 1.9F, 0.9F, 1.08F, 0.36F, 0.3, true, true);
        k(37, R, Element.STAR, 0x8FD8FF, Special.LIGHT, 0, () -> Items.HONEY_BOTTLE, SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.ALLAY_HURT, SoundEvents.ALLAY_DEATH, 1.6F, 0.42F, 0.4F, 0.36F, 0.3, true, false);
        k(39, N, E0, 0xD8FF50, Special.LIGHT, 0, () -> Items.GLOW_BERRIES, SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.ALLAY_HURT, SoundEvents.ALLAY_DEATH, 1.7F, 0.42F, 0.45F, 0.13F, 0.3, true, false);
        k(45, N, E0, 0x6AC8FF, Special.GUIDE, 0, () -> Items.COD, SoundEvents.DOLPHIN_AMBIENT, SoundEvents.DOLPHIN_HURT, SoundEvents.DOLPHIN_DEATH, 1.0F, 0.9F, 0.51F, 0.39F, 0.3, true, false);
        k(46, N, E0, 0xF0F4FF, Special.GIFT, 0, () -> Items.GOLDEN_CARROT, SoundEvents.HORSE_AMBIENT, SoundEvents.HORSE_HURT, SoundEvents.HORSE_DEATH, 1.35F, 0.69F, 0.79F, 0.34F, 0.3, true, false);
        k(47, R, Element.STAR, 0x40E0B8, Special.GIFT, 0, () -> Items.CHORUS_FRUIT, SoundEvents.ENDER_DRAGON_AMBIENT, SoundEvents.ENDER_DRAGON_HURT, SoundEvents.PHANTOM_DEATH, 2.0F, 0.9F, 0.96F, 0.43F, 0.3, true, true);
        k(50, B, E0, 0xFFD060, Special.FETCH, 0, () -> Items.RABBIT, SoundEvents.PARROT_AMBIENT, SoundEvents.PARROT_HURT, SoundEvents.PARROT_DEATH, 0.6F, 0.6F, 0.77F, 0.29F, 0.3, true, false);
        k(68, R, Element.CRYSTAL, 0xC08AFF, Special.GIFT, 0, () -> Items.AMETHYST_SHARD, SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.AMETHYST_BLOCK_HIT, SoundEvents.AMETHYST_BLOCK_BREAK, 1.0F, 0.88F, 0.68F, 0.28F, 0.3, true, false);
    }

    /** Velocidad de vuelo de las que vuelan (más rápidas las de alas grandes). */
    public static double flySpeed(int species) {
        return switch (species) {
            case 21 -> 0.42;
            case 29, 4 -> 0.48;
            case 37, 39, 45 -> 0.52;
            case 35, 47, 50, 19 -> 0.68;
            default -> 0.6;
        };
    }

    /** Lo que trae de regalo una vez al día (cosas pequeñas y con su historia). */
    public static List<ItemStack> gift(int species, RandomSource r) {
        List<ItemStack> out = new ArrayList<>();
        switch (species) {
            case 0 -> { // tiburoncito: lo que encontró nadando bajo la tierra
                Item[] t = {Items.IRON_NUGGET, Items.GOLD_NUGGET, Items.FLINT, Items.BONE, Items.CLAY_BALL, Items.PRISMARINE_SHARD};
                out.add(new ItemStack(t[r.nextInt(t.length)], 1 + r.nextInt(3)));
                if (r.nextInt(12) == 0) {
                    out.add(new ItemStack(Items.NAUTILUS_SHELL));
                }
            }
            case 1 -> { // gato de la fortuna: monedita de la suerte
                out.add(new ItemStack(Items.GOLD_NUGGET, 1 + r.nextInt(3)));
                if (r.nextInt(10) == 0) {
                    out.add(new ItemStack(Items.EMERALD));
                }
            }
            case 3 -> out.add(new ItemStack(Items.COOKIE, 2)); // mochis de luna
            case 9 -> out.add(new ItemStack(Items.GLOW_INK_SAC, 1 + r.nextInt(2)));
            case 13 -> { // pipas y un caramelito
                out.add(new ItemStack(Items.PUMPKIN_SEEDS, 3));
                out.add(new ItemStack(Items.COOKIE));
            }
            case 16 -> out.add(new ItemStack(Items.LILY_OF_THE_VALLEY)); // la flor de la montaña
            case 17 -> out.add(new ItemStack(Items.BREAD, 2)); // tostadas
            case 25 -> out.add(new ItemStack(Items.SLIME_BALL, 2));
            case 27 -> out.add(new ItemStack(Items.GUNPOWDER, 2));
            case 28, 62 -> out.add(new ItemStack(Items.SNOWBALL, 4));
            case 30 -> { // protege la flor
                out.add(new ItemStack(Items.IRON_NUGGET, 2));
                out.add(new ItemStack(Items.POPPY));
            }
            case 36 -> { // pinturas del calamar pintor
                Item[] d = {Items.RED_DYE, Items.BLUE_DYE, Items.YELLOW_DYE, Items.LIME_DYE, Items.PINK_DYE, Items.CYAN_DYE, Items.ORANGE_DYE,
                        Items.MAGENTA_DYE, Items.LIGHT_BLUE_DYE, Items.PURPLE_DYE};
                out.add(new ItemStack(d[r.nextInt(d.length)]));
                out.add(new ItemStack(d[r.nextInt(d.length)]));
            }
            case 38 -> out.add(new ItemStack(Items.HONEY_BOTTLE)); // un té con miel
            case 44 -> {
                out.add(new ItemStack(Items.FEATHER, 2));
                if (r.nextInt(4) == 0) {
                    out.add(new ItemStack(Items.GOLD_NUGGET));
                }
            }
            case 46 -> out.add(new ItemStack(Items.FEATHER, 3));
            case 47 -> { // polvo de estrellas
                Item[] s = {Items.GLOWSTONE_DUST, Items.LAPIS_LAZULI, Items.AMETHYST_SHARD};
                out.add(new ItemStack(s[r.nextInt(s.length)], 2 + r.nextInt(2)));
            }
            case 53 -> { // retazos
                out.add(new ItemStack(Items.STRING, 2));
                Item[] w = {Items.PINK_WOOL, Items.LIGHT_BLUE_WOOL, Items.LIME_WOOL, Items.PURPLE_WOOL, Items.WHITE_WOOL};
                out.add(new ItemStack(w[r.nextInt(w.length)]));
            }
            case 54 -> out.add(new ItemStack(r.nextBoolean() ? Items.COD : Items.SALMON));
            case 64 -> out.add(new ItemStack(Items.IRON_NUGGET, 3)); // de la forja
            case 68 -> out.add(new ItemStack(Items.AMETHYST_SHARD, 2));
            default -> out.add(new ItemStack(Items.COOKIE));
        }
        return out;
    }
}
