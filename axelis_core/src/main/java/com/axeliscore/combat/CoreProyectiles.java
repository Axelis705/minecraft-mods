package com.axeliscore.combat;

import com.axeliscore.AxelisCore;
import com.axeliscore.plat.Ids;

/** Proyectiles de muestra del Core (los usa el Coloso y el comando /axeliscore combate proyectiles). */
public final class CoreProyectiles {
    /** Fragmento de amatista que gira (objeto) y marca al que toca. */
    public static final ProyectilTipo FRAGMENTO = ProyectilTipo.registrar(new ProyectilTipo(AxelisCore.MODID, "fragmento")
            .objeto("minecraft:amethyst_shard").tam(0.35F).gira(30).estela("magic").dano(5).empuje(0.25F)
            .estado(Estados.MARCADO, 100, 1).impacto("minecraft:block.amethyst_block.break", "magic").alLanzar("minecraft:entity.snowball.throw"));
    /** Cristal grande (modelo de Blockbench) que cae en arco y estalla en área. */
    public static final ProyectilTipo CRISTAL = ProyectilTipo.registrar(new ProyectilTipo(AxelisCore.MODID, "cristal")
            .modelo(Ids.core("geo/cristal.geo.json"), Ids.core("textures/entity/cristal.png"), 1F).gira(14).estela("end_rod").gravedad(0.035F)
            .dano(8).area(2.4F).empuje(0.5F).temblor(0.3F).estado(Estados.SANGRADO, 80, 1)
            .impacto("minecraft:block.amethyst_cluster.break", "magic"));
    /** Orbe de escarcha que persigue y congela. */
    public static final ProyectilTipo ESCARCHA = ProyectilTipo.registrar(new ProyectilTipo(AxelisCore.MODID, "escarcha")
            .orbe(0xFF8FE3FF, 0.55F).estela("snow").persigue(0.07F).vida(140).dano(5).estado(Estados.CONGELADO, 90, 2)
            .impacto("minecraft:block.glass.break", "snow").alLanzar("minecraft:entity.evoker.cast_spell"));
    /** Bola de fuego con área que quema. */
    public static final ProyectilTipo FUEGO = ProyectilTipo.registrar(new ProyectilTipo(AxelisCore.MODID, "fuego")
            .orbe(0xFFFF7A2E, 0.6F).estela("flame").dano(6).area(2.2F).temblor(0.2F).estado(Estados.QUEMADO, 80, 1)
            .impacto("minecraft:entity.generic.explode", "explosion").alLanzar("minecraft:entity.blaze.shoot"));

    private CoreProyectiles() {
    }

    /** Fuerza la carga (los registra) al arrancar. */
    public static void init() {
    }
}
