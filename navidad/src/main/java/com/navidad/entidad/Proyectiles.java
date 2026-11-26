package com.navidad.entidad;

import com.axeliscore.combat.Estados;
import com.axeliscore.combat.ProyectilTipo;
import com.navidad.Navidad;

/** Lo que avientan los malos de Navidad (proyectiles del Core: se dibujan como el objeto). */
public final class Proyectiles {
    /** Bola de nieve del muñeco malvado: pega y congela un poquito. */
    public static final ProyectilTipo BOLA_NIEVE = ProyectilTipo.registrar(new ProyectilTipo(Navidad.MODID, "bola_nieve")
            .objeto("minecraft:snowball").tam(0.4F).gira(25).estela("snow").gravedad(0.02F).dano(3).empuje(0.3F)
            .estado(Estados.CONGELADO, 30, 1).impacto("minecraft:block.snow.break", "snow").alLanzar("minecraft:entity.snowball.throw"));
    /** Carbón encendido del Krampus: quema. */
    public static final ProyectilTipo CARBON = ProyectilTipo.registrar(new ProyectilTipo(Navidad.MODID, "carbon")
            .objeto("minecraft:coal").tam(0.45F).gira(20).estela("flame").gravedad(0.025F).dano(6).area(1.8F).empuje(0.4F)
            .estado(Estados.QUEMADO, 80, 1).impacto("minecraft:entity.generic.explode", "explosion").alLanzar("minecraft:entity.blaze.shoot"));
    /** Cascabel maldito que persigue (segunda fase del Krampus). */
    public static final ProyectilTipo CASCABEL = ProyectilTipo.registrar(new ProyectilTipo(Navidad.MODID, "cascabel")
            .objeto("minecraft:bell").tam(0.4F).gira(35).estela("soul").persigue(0.06F).vida(140).dano(5)
            .estado(Estados.ATURDIDO, 25, 1).impacto("minecraft:block.bell.use", "soul").alLanzar("navidad:cascabeles"));

    private Proyectiles() {
    }

    public static void init() {
    }
}
