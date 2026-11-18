package com.mundosdeaxelis.registry;

/** Lista de los sonidos propios de cada reliquia, con el formato "reliquia.tipo" (archivos relic_reliquia_tipo.ogg). */
public final class RelicSoundKeys {
    private RelicSoundKeys() {
    }

    public static final String[] KEYS = {
        "abyss_scepter.cast", "abyss_scepter.grab", "abyss_scepter.slam", "arcane_tome.cast", "astral_spyglass.cast",
        "astral_spyglass.mark", "astral_spyglass.scan", "aurora_horn.cast", "celestial_aegis.absorb",
        "celestial_aegis.cast", "cloud_bottle.cast", "comet_lance.cast", "comet_lance.charge", "comet_lance.dash",
        "comet_lance.hit", "crystal_heart.cast", "dragon_scale.cast", "dragon_whip.cast", "dragon_whip.crack",
        "dragon_whip.hit", "eclipse_dagger.cast", "eclipse_dagger.slash", "ender_orb.cast", "eternal_rose.cast",
        "fairy_jar.bolt", "fairy_jar.cast", "fairy_jar.end", "fairy_jar.heal", "fang_staff.bite", "fang_staff.cast",
        "fate_coin.cast", "fate_coin.flip", "fate_coin.heads", "fate_coin.tails", "feather_talisman.cast",
        "fox_charm.cast", "frost_bracelet.cast", "genie_lamp.cast", "genie_lamp.wish", "hatchling_egg.breath",
        "hatchling_egg.cast", "hatchling_egg.end", "hatchling_egg.hatch", "hatchling_egg.roar", "healing_lyre.cast",
        "healing_lyre.note", "heart_fireworks.burst", "heart_fireworks.cast", "heart_fireworks.charge",
        "heart_fireworks.crackle", "heart_fireworks.launch", "home_mirror.arrive", "home_mirror.cast",
        "home_mirror.charge", "hourglass.cast", "light_wings.cast", "light_wings.flap", "magma_boots.cast",
        "magma_boots.land", "medusa_eye.cast", "medusa_eye.crack", "meteor_scepter.cast", "meteor_scepter.fall",
        "meteor_scepter.impact", "miner_medallion.cast", "mirror_shield.cast", "mirror_shield.raise",
        "mirror_shield.reflect", "moon_blade.cast", "moon_blade.hit", "moon_blade.wave", "moon_crystal.cast",
        "music_box.cast", "music_box.song", "music_box.wake", "oak_heart.cast", "phoenix_ring.cast",
        "prism_rainbow.cast", "prism_rainbow.hum", "quake_seal.cast", "quake_seal.wave", "rage_gauntlet.cast",
        "rage_gauntlet.hit", "red_thread.cast", "red_thread.link", "red_thread.pulse", "red_thread.snap",
        "rewind_anchor.cast", "rewind_anchor.tick", "rocket_pack.cast", "rocket_pack.empty", "rocket_pack.ready",
        "rocket_pack.thrust", "solar_chakram.cast", "solar_chakram.catch", "solar_chakram.spin", "soul_lantern.cast",
        "soul_lantern.collect", "soul_lantern.release", "specter_mask.cast", "star_bow.cast", "star_bow.charge",
        "star_bow.hit", "star_bow.shoot", "star_charm.cast", "star_heart.beam", "star_heart.cast",
        "star_heart.charge", "star_heart.hit", "storm_eye.cast", "sun_crown.cast", "tide_shell.cast",
        "time_pendulum.cast", "time_pendulum.end", "titan_belt.cast", "umbral_glaive.blade", "umbral_glaive.cast",
        "vampire_chalice.cast", "victory_banner.cast", "victory_banner.end", "victory_banner.plant",
        "void_singularity.cast", "void_singularity.implode", "voltaic_scepter.cast", "voltaic_scepter.charge",
        "voltaic_scepter.zap", "war_horn.cast", "war_horn.charge", "warrior_medal.cast", "whirlwind_staff.cast",
        "whirlwind_staff.fling", "whirlwind_staff.loop", "wind_ring.cast", "wolf_fang.cast", "world_tree_seed.cast",
        "world_tree_seed.end", "world_tree_seed.grow", "world_tree_seed.plant",
        "comp_bunny.act", "comp_bunny.cast", "comp_bunny.end", "comp_cat.act", "comp_cat.cast", "comp_cat.end",
        "comp_fox.act", "comp_fox.cast", "comp_fox.end", "comp_jelly.act", "comp_jelly.cast", "comp_jelly.end",
        "comp_owl.act", "comp_owl.cast", "comp_owl.end", "comp_panda.act", "comp_panda.cast", "comp_panda.end",
        "comp_penguin.act", "comp_penguin.cast", "comp_penguin.end", "comp_phoenix.act", "comp_phoenix.cast", "comp_phoenix.end",
        "comp_turtle.act", "comp_turtle.cast", "comp_turtle.end", "comp_unicorn.act", "comp_unicorn.cast", "comp_unicorn.end",
        "comp_chick.act", "comp_chick.cast", "comp_chick.end", "comp_frog.act", "comp_frog.cast", "comp_frog.end", "comp_hamster.act", "comp_hamster.cast", "comp_hamster.end", "comp_snail.act", "comp_snail.cast", "comp_snail.end", "comp_bee.act", "comp_bee.cast", "comp_bee.end", "comp_sheep.act", "comp_sheep.cast", "comp_sheep.end", "comp_hedgehog.act", "comp_hedgehog.cast", "comp_hedgehog.end", "comp_axolotl.act", "comp_axolotl.cast", "comp_axolotl.end", "comp_raccoon.act", "comp_raccoon.cast", "comp_raccoon.end", "comp_deer.act", "comp_deer.cast", "comp_deer.end", "comp_whale.act", "comp_whale.cast", "comp_whale.end", "comp_kitsune.act", "comp_kitsune.cast", "comp_kitsune.end", "comp_capybara.cast", "comp_capybara.act", "comp_capybara.end", "comp_duck.cast", "comp_duck.act", "comp_duck.end", "comp_slime.cast", "comp_slime.act", "comp_slime.end", "comp_corgi.cast", "comp_corgi.act", "comp_corgi.end", "comp_creeper.cast", "comp_creeper.act", "comp_creeper.end", "comp_snowman.cast", "comp_snowman.act", "comp_snowman.end", "comp_ghost.cast", "comp_ghost.act", "comp_ghost.end", "comp_golem.cast", "comp_golem.act", "comp_golem.end", "comp_sparkmouse.cast", "comp_sparkmouse.act", "comp_sparkmouse.end", "comp_droid.cast", "comp_droid.act", "comp_droid.end", "comp_forest_spirit.cast", "comp_forest_spirit.act", "comp_forest_spirit.end", "comp_moon_wolf.cast", "comp_moon_wolf.act", "comp_moon_wolf.end", "comp_night_dragon.cast", "comp_night_dragon.act", "comp_night_dragon.end", "comp_octopus.act", "comp_octopus.cast", "comp_octopus.end", "comp_butterfly.act", "comp_butterfly.cast", "comp_butterfly.end", "comp_crab.act", "comp_crab.cast", "comp_crab.end", "comp_firefly.act", "comp_firefly.cast", "comp_firefly.end", "comp_koala.act", "comp_koala.cast", "comp_koala.end", "comp_elephant.act", "comp_elephant.cast", "comp_elephant.end", "comp_lion.act", "comp_lion.cast", "comp_lion.end", "comp_salamander.act", "comp_salamander.cast", "comp_salamander.end", "comp_peacock.act", "comp_peacock.cast", "comp_peacock.end", "comp_dolphin.act", "comp_dolphin.cast", "comp_dolphin.end", "comp_pegasus.act", "comp_pegasus.cast", "comp_pegasus.end", "comp_celestial_dragon.act", "comp_celestial_dragon.cast", "comp_celestial_dragon.end", "comp_ferret.act", "comp_ferret.cast", "comp_ferret.end"
    };
}
