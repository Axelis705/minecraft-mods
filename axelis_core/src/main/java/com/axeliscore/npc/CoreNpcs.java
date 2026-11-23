package com.axeliscore.npc;

import com.axeliscore.AxelisCore;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Plat;
import com.axeliscore.ui.Rareza;
import com.axeliscore.ui.Theme;
import net.minecraft.world.item.ItemStack;

/**
 * NPC de muestra del Core (para probar y como ejemplo): Mara, la comerciante, e Iker, el guardia. Se ponen con
 * {@code /axeliscore npc poner axelis_core:mara}.
 */
public final class CoreNpcs {
    public static NpcDef MARA, IKER;
    /** Misión suelta (sin NPC) para la prueba automática: se completa sola con un evento. */
    public static Mision PRUEBA;
    private static boolean iniciado;

    private CoreNpcs() {
    }

    private static Mision m(String nombre) {
        return new Mision(AxelisCore.MODID, nombre);
    }

    private static void dar(net.minecraft.server.level.ServerPlayer p, String id, int n) {
        Plat.give(p, new ItemStack(Plat.item(id), n));
    }

    public static synchronized void init() {
        if (iniciado) {
            return;
        }
        iniciado = true;
        MARA = NpcDef.registrar(new NpcDef(AxelisCore.MODID, "mara")
                .piel(Ids.core("textures/entity/persona/mara.png")).tema(Theme.MADERA)
                .saludos("npc.axelis_core.mara.hola1", "npc.axelis_core.mara.hola2", "npc.axelis_core.mara.hola3", "npc.axelis_core.mara.hola4")
                .tienda(new Tienda()
                        .vende("minecraft:bread", 4, 1)
                        .vende("minecraft:cookie", 8, 1)
                        .vende("minecraft:cooked_beef", 3, 2)
                        .vende("minecraft:golden_carrot", 4, 3, 0, 1)
                        .vende("minecraft:cake", 1, 4, 0, 2)
                        .vende("minecraft:golden_apple", 1, 8, 2, 3)
                        .compra("minecraft:wheat", 20, 1)
                        .compra("minecraft:carrot", 22, 1)
                        .compra("minecraft:paper", 24, 1)
                        .compra("minecraft:rotten_flesh", 32, 1))
                .mision(m("pan").objetivo(Objetivo.entregar("minecraft:bread", 6))
                        .premio(Mision.Premio.esmeraldas(4)).amistad(10))
                .mision(m("trigo").diaria().objetivo(Objetivo.entregar("minecraft:wheat", 16))
                        .premio(Mision.Premio.esmeraldas(2)).premio(Mision.Premio.experiencia(20)).amistad(4))
                .mision(m("recado").requiere(AxelisCore.MODID + ":pan").objetivo(Objetivo.hablar(AxelisCore.MODID + ":iker"))
                        .premio(Mision.Premio.esmeraldas(2)).premio(Mision.Premio.amistad(AxelisCore.MODID + ":iker", 5)).amistad(6))
                .mision(m("pastel").nivel(2).rareza(Rareza.RARO)
                        .objetivo(Objetivo.entregar("minecraft:sugar", 4)).objetivo(Objetivo.entregar("minecraft:egg", 2))
                        .objetivo(Objetivo.entregar("minecraft:milk_bucket", 1))
                        .premio(Mision.Premio.objeto("minecraft:cake", 1)).premio(Mision.Premio.esmeraldas(6)).amistad(15))
                .regalo(1, p -> dar(p, "minecraft:bread", 8))
                .regalo(2, p -> dar(p, "minecraft:golden_carrot", 6))
                .regalo(4, p -> dar(p, "minecraft:golden_apple", 2)));

        IKER = NpcDef.registrar(new NpcDef(AxelisCore.MODID, "iker")
                .piel(Ids.core("textures/entity/persona/iker.png")).tema(Theme.PIEDRA).rutina(NpcDef.Rutina.QUIETO)
                .saludos("npc.axelis_core.iker.hola1", "npc.axelis_core.iker.hola2", "npc.axelis_core.iker.hola3", "npc.axelis_core.iker.hola4")
                .tienda(new Tienda()
                        .vende("minecraft:arrow", 16, 1)
                        .vende("minecraft:bow", 1, 3)
                        .vende("minecraft:shield", 1, 4)
                        .vende("minecraft:iron_sword", 1, 5, 0, 1)
                        .vende("minecraft:iron_chestplate", 1, 9, 1, 2)
                        .vende("minecraft:diamond", 1, 12, 1, 3)
                        .compra("minecraft:bone", 16, 1)
                        .compra("minecraft:string", 16, 1)
                        .compra("minecraft:spider_eye", 8, 1)
                        .compra("minecraft:gunpowder", 6, 1))
                .mision(m("zombis").objetivo(Objetivo.cazar("minecraft:zombie", 5))
                        .premio(Mision.Premio.esmeraldas(3)).premio(Mision.Premio.experiencia(30)).amistad(8))
                .mision(m("huesos").requiere(AxelisCore.MODID + ":zombis").objetivo(Objetivo.cazar("minecraft:skeleton", 4))
                        .objetivo(Objetivo.entregar("minecraft:bone", 6))
                        .premio(Mision.Premio.objeto("minecraft:arrow", 32)).premio(Mision.Premio.esmeraldas(3)).amistad(8))
                .mision(m("ronda").diaria().objetivo(Objetivo.cazar("*", 8))
                        .premio(Mision.Premio.esmeraldas(2)).premio(Mision.Premio.experiencia(25)).amistad(4))
                .mision(m("aranas").nivel(1).rareza(Rareza.POCO_COMUN).objetivo(Objetivo.cazar("minecraft:spider", 4))
                        .objetivo(Objetivo.entregar("minecraft:string", 8))
                        .premio(Mision.Premio.objeto("minecraft:shield", 1)).premio(Mision.Premio.esmeraldas(4)).amistad(10))
                .mision(m("coloso").nivel(3).requiere(AxelisCore.MODID + ":huesos").rareza(Rareza.EPICO)
                        .objetivo(Objetivo.cazar(AxelisCore.MODID + ":coloso", 1))
                        .premio(Mision.Premio.objeto("minecraft:diamond", 3)).premio(Mision.Premio.esmeraldas(10)).amistad(25))
                .regalo(1, p -> dar(p, "minecraft:arrow", 16))
                .regalo(2, p -> dar(p, "minecraft:iron_sword", 1))
                .regalo(4, p -> dar(p, "minecraft:diamond_sword", 1)));

        PRUEBA = Misiones.registrar(m("prueba").objetivo(Objetivo.evento(AxelisCore.MODID + ":prueba", 3))
                .premio(Mision.Premio.experiencia(5)));
    }
}
