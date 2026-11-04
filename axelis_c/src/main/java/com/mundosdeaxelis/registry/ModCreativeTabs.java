package com.mundosdeaxelis.registry;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MundosDeAxelis.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MundosDeAxelis.MODID))
                    .icon(() -> new ItemStack(ModItems.PORTAL_STONE_FOREST.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ARTIFICER_TABLE.get());
                        output.accept(ModItems.PORTAL_STONE_FOREST.get());
                        output.accept(ModItems.PORTAL_STONE_FORGE.get());
                        output.accept(ModItems.PORTAL_STONE_ICE.get());
                        output.accept(ModItems.ETERNAL_BRAZIER.get());
                        output.accept(ModItems.REY_BROTE_SPAWN_EGG.get());
                        output.accept(ModItems.LOBO_ARTICO_SPAWN_EGG.get());
                        output.accept(ModItems.LOBO_ESCARCHA_SPAWN_EGG.get());
                        output.accept(ModItems.BROTE_SPAWN_EGG.get());
                        output.accept(ModItems.RAIZAL_SPAWN_EGG.get());
                        output.accept(ModItems.VAULT_CHEST.get());
                        output.accept(ModItems.VAULT_CHEST_SEALED.get());
                        output.accept(ModItems.VAULT_CHEST_LOCKED.get());
                        output.accept(ModItems.DUNGEON_KEY.get());
                        output.accept(ModItems.SANCTUARY_BERRY.get());
                        output.accept(ModItems.ACORN.get());
                        output.accept(ModItems.GOLDEN_ACORN.get());
                        output.accept(ModItems.EMBER_STEEL.get());
                        output.accept(ModItems.EMBER_RIVET.get());
                        output.accept(ModItems.FORGE_ROAST.get());
                        output.accept(ModItems.FORGE_KEY.get());
                        output.accept(ModItems.VAULT_CHEST_FORGE.get());
                        output.accept(ModItems.VAULT_CHEST_SEALED_FORGE.get());
                        output.accept(ModItems.VAULT_CHEST_LOCKED_FORGE.get());
                        output.accept(ModItems.GOLDEN_RIVET.get());
                        output.accept(ModItems.POLINIZADOR_SPAWN_EGG.get());
                        output.accept(ModItems.ESPINOSO_SPAWN_EGG.get());
                        output.accept(ModItems.GUARDIAN_MUSGO_SPAWN_EGG.get());
                        output.accept(ModItems.ESCORIA_SPAWN_EGG.get());
                        output.accept(ModItems.CHISPERO_SPAWN_EGG.get());
                        output.accept(ModItems.ASCUA_SPAWN_EGG.get());
                        output.accept(ModItems.FUNDIDOR_SPAWN_EGG.get());
                        output.accept(ModItems.CAPATAZ_SPAWN_EGG.get());
                        output.accept(ModItems.GRAN_HERRERO_SPAWN_EGG.get());
                        for (com.mundosdeaxelis.table.Mats m : com.mundosdeaxelis.table.Mats.values()) {
                            output.accept(ModItems.MATS.get(m).get());
                        }
                        for (var seal : ModItems.SEALS.values()) {
                            output.accept(seal.get());
                        }
                        output.accept(ModItems.COFFEE.get());
                        output.accept(ModItems.DOLLAR.get());
                        output.accept(ModItems.FORTUNE_COOKIE.get());
                        output.accept(ModItems.STAR_FEATHER.get());
                        output.accept(ModItems.STAR_MAGNET.get());
                        output.accept(ModItems.CONFETTI_CANNON.get());
                        output.accept(ModItems.RAINBOW_GRAPPLE.get());
                        output.accept(ModItems.BUBBLE_PEARL.get());
                        output.accept(ModItems.POCKET_BACKPACK.get());
                        output.accept(ModItems.HEART_LOCKET.get());
                        output.accept(ModItems.FIREFLY_LANTERN.get());
                        output.accept(ModItems.STAR_CLOCK.get());
                        output.accept(ModItems.FLOWER_WAND.get());
                        output.accept(ModItems.UMBRELLA.get());
                        output.accept(ModItems.EXPLORER_COMPASS.get());
                        output.accept(ModItems.LUCKY_DIE.get());
                        output.accept(ModItems.WATCHER_EYE.get());
                        output.accept(ModItems.COCOA_FLASK.get());
                        output.accept(ModItems.CALM_BELL.get());
                        output.accept(ModItems.RAINBOW_BRUSH.get());
                        output.accept(ModItems.BLINK_STONE.get());
                        output.accept(ModItems.RAINBOW_LOLLIPOP.get());
                        output.accept(ModItems.PHOENIX_RING.get());
                        output.accept(ModItems.FEATHER_TALISMAN.get());
                        output.accept(ModItems.OAK_HEART.get());
                        output.accept(ModItems.MINER_MEDALLION.get());
                        output.accept(ModItems.TIDE_SHELL.get());
                        output.accept(ModItems.WOLF_FANG.get());
                        output.accept(ModItems.STAR_CHARM.get());
                        output.accept(ModItems.FROST_BRACELET.get());
                        output.accept(ModItems.SUN_CROWN.get());
                        output.accept(ModItems.STORM_EYE.get());
                        output.accept(ModItems.WIND_RING.get());
                        output.accept(ModItems.SMITH_RING.get());
                        output.accept(ModItems.DRAGON_SCALE.get());
                        output.accept(ModItems.MOON_CRYSTAL.get());
                        output.accept(ModItems.ETERNAL_ROSE.get());
                        output.accept(ModItems.HOURGLASS.get());
                        output.accept(ModItems.FOX_CHARM.get());
                        output.accept(ModItems.CRYSTAL_HEART.get());
                        output.accept(ModItems.SPECTER_MASK.get());
                        output.accept(ModItems.AURORA_HORN.get());
                        output.accept(ModItems.ARCANE_TOME.get());
                        output.accept(ModItems.WARRIOR_MEDAL.get());
                        output.accept(ModItems.VAMPIRE_CHALICE.get());
                        output.accept(ModItems.ENDER_ORB.get());
                        output.accept(ModItems.TITAN_BELT.get());
                        output.accept(ModItems.VOLTAIC_SCEPTER.get());
                        output.accept(ModItems.WAR_HORN.get());
                        output.accept(ModItems.HOME_MIRROR.get());
                        output.accept(ModItems.MEDUSA_EYE.get());
                        output.accept(ModItems.RAGE_GAUNTLET.get());
                        output.accept(ModItems.UMBRAL_GLAIVE.get());
                        output.accept(ModItems.MAGMA_BOOTS.get());
                        output.accept(ModItems.CLOUD_BOTTLE.get());
                        output.accept(ModItems.VOID_SINGULARITY.get());
                        output.accept(ModItems.CELESTIAL_AEGIS.get());
                        output.accept(ModItems.HEALING_LYRE.get());
                        output.accept(ModItems.GENIE_LAMP.get());
                        output.accept(ModItems.METEOR_SCEPTER.get());
                        output.accept(ModItems.ECLIPSE_DAGGER.get());
                        output.accept(ModItems.QUAKE_SEAL.get());
                        output.accept(ModItems.LIGHT_WINGS.get());
                        output.accept(ModItems.SOUL_LANTERN.get());
                        output.accept(ModItems.PRISM_RAINBOW.get());
                        output.accept(ModItems.SOLAR_CHAKRAM.get());
                        output.accept(ModItems.TIME_PENDULUM.get());
                        output.accept(ModItems.STAR_BOW.get());
                        output.accept(ModItems.COMET_LANCE.get());
                        output.accept(ModItems.MIRROR_SHIELD.get());
                        output.accept(ModItems.ASTRAL_SPYGLASS.get());
                        output.accept(ModItems.FANG_STAFF.get());
                        output.accept(ModItems.REWIND_ANCHOR.get());
                        output.accept(ModItems.FATE_COIN.get());
                        output.accept(ModItems.HEART_FIREWORKS.get());
                        output.accept(ModItems.RED_THREAD.get());
                        output.accept(ModItems.VICTORY_BANNER.get());
                        output.accept(ModItems.WHIRLWIND_STAFF.get());
                        output.accept(ModItems.WORLD_TREE_SEED.get());
                        output.accept(ModItems.HATCHLING_EGG.get());
                        output.accept(ModItems.FAIRY_JAR.get());
                        output.accept(ModItems.MOON_BLADE.get());
                        output.accept(ModItems.ROCKET_PACK.get());
                        output.accept(ModItems.MUSIC_BOX.get());
                        output.accept(ModItems.STAR_HEART.get());
                        output.accept(ModItems.DRAGON_WHIP.get());
                        output.accept(ModItems.ABYSS_SCEPTER.get());
                        output.accept(ModItems.COMP_FOX.get());
                        output.accept(ModItems.COMP_CAT.get());
                        output.accept(ModItems.COMP_BUNNY.get());
                        output.accept(ModItems.COMP_JELLY.get());
                        output.accept(ModItems.COMP_PHOENIX.get());
                        output.accept(ModItems.COMP_TURTLE.get());
                        output.accept(ModItems.COMP_PENGUIN.get());
                        output.accept(ModItems.COMP_PANDA.get());
                        output.accept(ModItems.COMP_CHICK.get());
                        output.accept(ModItems.COMP_FROG.get());
                        output.accept(ModItems.COMP_HAMSTER.get());
                        output.accept(ModItems.COMP_SNAIL.get());
                        output.accept(ModItems.COMP_BEE.get());
                        output.accept(ModItems.COMP_SHEEP.get());
                        output.accept(ModItems.COMP_HEDGEHOG.get());
                        output.accept(ModItems.COMP_AXOLOTL.get());
                        output.accept(ModItems.COMP_RACCOON.get());
                        output.accept(ModItems.COMP_WHALE.get());
                        output.accept(ModItems.COMP_KITSUNE.get());
                        output.accept(ModItems.COMP_CAPYBARA.get());
                        output.accept(ModItems.COMP_DUCK.get());
                        output.accept(ModItems.COMP_SLIME.get());
                        output.accept(ModItems.COMP_CORGI.get());
                        output.accept(ModItems.COMP_CREEPER.get());
                        output.accept(ModItems.COMP_SNOWMAN.get());
                        output.accept(ModItems.COMP_GHOST.get());
                        output.accept(ModItems.COMP_GOLEM.get());
                        output.accept(ModItems.COMP_SPARKMOUSE.get());
                        output.accept(ModItems.COMP_DROID.get());
                        output.accept(ModItems.COMP_FOREST_SPIRIT.get());
                        output.accept(ModItems.COMP_MOON_WOLF.get());
                        output.accept(ModItems.COMP_NIGHT_DRAGON.get());
                        output.accept(ModItems.COMP_OCTOPUS.get());
                        output.accept(ModItems.COMP_BUTTERFLY.get());
                        output.accept(ModItems.COMP_CRAB.get());
                        output.accept(ModItems.COMP_FIREFLY.get());
                        output.accept(ModItems.COMP_ELEPHANT.get());
                        output.accept(ModItems.COMP_LION.get());
                        output.accept(ModItems.COMP_PEACOCK.get());
                        output.accept(ModItems.COMP_DOLPHIN.get());
                        output.accept(ModItems.COMP_PEGASUS.get());
                        output.accept(ModItems.COMP_CELESTIAL_DRAGON.get());
                        output.accept(ModItems.COMP_FERRET.get());
                        output.accept(ModItems.COMP_MIMIC.get());
                        output.accept(ModItems.COMP_GRIFFON.get());
                        output.accept(ModItems.COMP_PIGGY.get());
                        output.accept(ModItems.COMP_SKUNK.get());
                        output.accept(ModItems.COMP_SEAL.get());
                        output.accept(ModItems.COMP_MOONIMP.get());
                        output.accept(ModItems.COMP_PATCH.get());
                        output.accept(ModItems.COMP_TRIKE.get());
                        output.accept(ModItems.COMP_YETI.get());
                        output.accept(ModItems.COMP_SPROUT_KING.get());
                        output.accept(ModItems.COMP_EMBER_ANVIL.get());
                        output.accept(ModItems.GEAR_DAWN_BLADE.get());
                        output.accept(ModItems.GEAR_FROST_BLADE.get());
                        output.accept(ModItems.GEAR_STORM_BLADE.get());
                        output.accept(ModItems.GEAR_VAMPIRE_SABER.get());
                        output.accept(ModItems.GEAR_DRAGON_SWORD.get());
                        output.accept(ModItems.GEAR_CHEER_PICK.get());
                        output.accept(ModItems.GEAR_PROSPECTOR_PICK.get());
                        output.accept(ModItems.GEAR_VEIN_PICK.get());
                        output.accept(ModItems.GEAR_CORE_PICK.get());
                        output.accept(ModItems.GEAR_EXCAVATOR.get());
                        output.accept(ModItems.GEAR_LUMBER_AXE.get());
                        output.accept(ModItems.GEAR_THUNDER_AXE.get());
                        for (com.mundosdeaxelis.gear.GearDef d : com.mundosdeaxelis.gear.GearDefs.ALL) {
                            output.accept(ModItems.SET_GEAR.get(d.id()).get());
                        }
                        for (java.util.Map.Entry<String, net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item>> a : ModItems.SET_ARMOR.entrySet()) {
                            output.accept(a.getValue().get());
                        }
                        for (net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> a : ModItems.DECOR.values()) {
                            output.accept(a.get());
                        }
                        for (net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> a : ModItems.CAPES.values()) {
                            output.accept(a.get());
                        }
                        for (com.mundosdeaxelis.crate.CrateKind k : com.mundosdeaxelis.crate.CrateKind.values()) {
                            output.accept(ModItems.CRATE_ITEMS.get(k).get());
                            output.accept(ModItems.KEYS.get(k).get());
                        }
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
