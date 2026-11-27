package com.navidad.gente;

import com.axeliscore.npc.Mision;
import com.axeliscore.npc.Npc;
import com.axeliscore.npc.NpcDef;
import com.axeliscore.npc.Objetivo;
import com.axeliscore.npc.Tienda;
import com.axeliscore.plat.Plat;
import com.axeliscore.ui.Rareza;
import com.axeliscore.ui.Theme;
import com.axeliscore.vestir.Vestuario;
import com.navidad.Navidad;
import com.navidad.objeto.Ropa;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * La gente del Polo Norte (NPC del Core con tienda, misiones y amistad): Santa, la Señora Claus y tres duendes. Se ponen solos al
 * construir la aldea de Santa ({@code /navidad aldea}) o con {@code /navidad gente <quien>}.
 */
public final class Gente {
    /** Rojo navideño con detalles dorados y el marco de madera del Core. */
    public static final Theme TEMA = new Theme(0xF23A1216, 0xF2200A0C, 0xFF120405, 0xFF8A2A30, 0xFFF0C040, 0xFF7AE08A, 0xFFF8EEE6,
            0xFFC8AAA4, 0xFFFFE6A0, 0x80000000, 0xFF6A1E24, 0xFF8A2A30, 0xFFFFFFFF, 0xFF7AE08A, 0xFFFF7A6A, Theme.MADERA.frame);
    public static NpcDef SANTA, SENORA, JUGUETERO, SASTRE, RENOS, PINERO, VIAJERO;
    private static boolean iniciado;

    private Gente() {
    }

    private static ResourceLocation piel(String n) {
        return new ResourceLocation(Navidad.MODID, "textures/entity/" + n + ".png");
    }

    private static Mision m(String n) {
        return new Mision(Navidad.MODID, n);
    }

    private static String nv(String id) {
        return Navidad.MODID + ":" + id;
    }

    private static void dar(ServerPlayer p, String id, int n) {
        Plat.give(p, new ItemStack(Plat.item(id), n));
    }

    private static String[] saludos(String quien) {
        return new String[]{"npc.navidad." + quien + ".hola1", "npc.navidad." + quien + ".hola2", "npc.navidad." + quien + ".hola3"};
    }

    public static synchronized void init() {
        if (iniciado) {
            return;
        }
        iniciado = true;
        SANTA = NpcDef.registrar(new NpcDef(Navidad.MODID, "santa").piel(piel("santa")).tema(TEMA).rutina(NpcDef.Rutina.QUIETO)
                .saludos(saludos("santa"))
                .mision(m("galletas_leche").objetivo(Objetivo.entregar(nv("galleta_jengibre"), 6)).objetivo(Objetivo.entregar("minecraft:milk_bucket", 1))
                        .premio(Mision.Premio.objeto(nv("regalo_misterioso"), 2)).amistad(12))
                .mision(m("munecos").objetivo(Objetivo.cazar(nv("muneco_malvado"), 5))
                        .premio(Mision.Premio.objeto(nv("disco_campanas"), 1)).premio(Mision.Premio.experiencia(40)).amistad(10))
                .mision(m("jengibre").objetivo(Objetivo.cazar(nv("hombre_jengibre"), 3))
                        .premio(Mision.Premio.objeto(nv("galleta_jengibre"), 12)).premio(Mision.Premio.objeto(nv("regalo_misterioso"), 1)).amistad(8))
                .mision(m("pinata").objetivo(Objetivo.evento(nv("pinata"), 1))
                        .premio(Mision.Premio.objeto(nv("dulce"), 24)).amistad(8))
                .mision(m("regalos").requiere(nv("galletas_leche")).rareza(Rareza.RARO).objetivo(Objetivo.evento(nv("regalo_entregado"), 3))
                        .premio(Mision.Premio.objeto(nv("traje_santa"), 1)).amistad(20))
                .mision(m("krampus").nivel(2).rareza(Rareza.EPICO).objetivo(Objetivo.evento(nv("krampus"), 1))
                        .premio(Mision.Premio.objeto(nv("disco_noche_paz"), 1)).premio(Mision.Premio.objeto("minecraft:diamond", 8)).amistad(30))
                .mision(m("carta").diaria().objetivo(Objetivo.entregar("minecraft:paper", 3)).objetivo(Objetivo.entregar("minecraft:feather", 1))
                        .premio(Mision.Premio.objeto(nv("regalo_misterioso"), 1)).amistad(4))
                .regalo(1, p -> dar(p, nv("regalo_misterioso"), 2))
                .regalo(2, p -> dar(p, nv("disco_navidad"), 1))
                .regalo(3, p -> dar(p, nv("arbol_dorado"), 1))
                .regalo(4, p -> dar(p, nv("disco_adornen"), 1)));
        SENORA = NpcDef.registrar(new NpcDef(Navidad.MODID, "senora_claus").piel(piel("senora_claus")).tema(TEMA)
                .saludos(saludos("senora_claus"))
                .tienda(new Tienda()
                        .vende(nv("galleta_jengibre"), 6, 1)
                        .vende(nv("chocolate_caliente"), 2, 1)
                        .vende(nv("ponche"), 2, 1)
                        .vende(nv("champurrado"), 2, 1)
                        .vende(nv("tamal"), 4, 2)
                        .vende(nv("bunuelo"), 4, 1)
                        .vende(nv("pavo_navideno"), 1, 4, 0, 1)
                        .vende(nv("uvas"), 12, 2)
                        .vende(nv("disco_navidad"), 1, 16, 1, 3)
                        .compra("minecraft:sugar", 16, 1)
                        .compra("minecraft:cocoa_beans", 10, 1)
                        .compra("minecraft:wheat", 20, 1)
                        .compra("minecraft:egg", 12, 1))
                .mision(m("ingredientes").objetivo(Objetivo.entregar("minecraft:sugar", 16)).objetivo(Objetivo.entregar("minecraft:cocoa_beans", 8))
                        .premio(Mision.Premio.objeto(nv("chocolate_caliente"), 6)).amistad(10))
                .mision(m("pavo").objetivo(Objetivo.entregar("minecraft:cooked_chicken", 4))
                        .premio(Mision.Premio.objeto(nv("pavo_navideno"), 2)).premio(Mision.Premio.esmeraldas(3)).amistad(10))
                .mision(m("horno").diaria().objetivo(Objetivo.entregar("minecraft:wheat", 16))
                        .premio(Mision.Premio.objeto(nv("galleta_jengibre"), 8)).amistad(4))
                .regalo(1, p -> dar(p, nv("galleta_jengibre"), 12))
                .regalo(2, p -> dar(p, nv("pavo_navideno"), 2))
                .regalo(3, p -> dar(p, nv("olla_ponche"), 1)));
        JUGUETERO = NpcDef.registrar(new NpcDef(Navidad.MODID, "duende_juguetero").piel(piel("duende_juguetero")).tema(TEMA)
                .saludos(saludos("duende_juguetero"))
                .tienda(new Tienda().moneda(nv("dulce"))
                        .vende(nv("caja_adornos"), 2, 3)
                        .vende(nv("pinata"), 1, 12)
                        .vende(nv("palo_pinata"), 1, 4)
                        .vende(nv("venda"), 1, 4)
                        .vende(nv("caja_regalo"), 1, 6)
                        .vende(nv("calendario_adviento"), 1, 10)
                        .vende(nv("trineo_madera"), 1, 20)
                        .vende(nv("regalo_misterioso"), 1, 30, 2, 1)
                        .vende(nv("disco_campanas"), 1, 50, 1, 2)
                        .vende(nv("disco_adornen"), 1, 50, 1, 3)
                        .compra("minecraft:snowball", 16, 1)
                        .compra("minecraft:paper", 8, 1)
                        .compra(nv("tejocote"), 4, 1)
                        .compra(nv("mandarina"), 4, 1))
                .mision(m("nieve").objetivo(Objetivo.entregar("minecraft:snowball", 32))
                        .premio(Mision.Premio.objeto(nv("dulce"), 12)).amistad(8))
                .mision(m("juguetes").objetivo(Objetivo.entregar("minecraft:oak_planks", 16)).objetivo(Objetivo.entregar("minecraft:red_dye", 4))
                        .premio(Mision.Premio.objeto(nv("pinata"), 1)).premio(Mision.Premio.objeto(nv("dulce"), 8)).amistad(10))
                .regalo(1, p -> dar(p, nv("caja_adornos"), 8))
                .regalo(2, p -> dar(p, nv("pinata"), 2))
                .regalo(3, p -> dar(p, nv("trineo"), 1)));
        SASTRE = NpcDef.registrar(new NpcDef(Navidad.MODID, "duende_sastre").piel(piel("duende_sastre")).tema(TEMA)
                .saludos(saludos("duende_sastre"))
                .tienda(new Tienda()
                        .vende(nv("gorro_santa"), 1, 2)
                        .vende(nv("gorro_duende"), 1, 2)
                        .vende(nv("cuernos_reno"), 1, 3)
                        .vende(nv("sueter_reno"), 1, 4)
                        .vende(nv("sueter_pinos"), 1, 4)
                        .vende(nv("sueter_copos"), 1, 4)
                        .vende(nv("traje_santa"), 1, 12, 1, 3)
                        .vende(nv("patines"), 1, 5)
                        .compra("minecraft:white_wool", 12, 1)
                        .compra("minecraft:red_wool", 12, 1))
                .mision(m("lana").objetivo(Objetivo.entregar("minecraft:red_wool", 8)).objetivo(Objetivo.entregar("minecraft:white_wool", 4))
                        .premio(Mision.Premio.objeto(nv("gorro_santa"), 1)).premio(Mision.Premio.esmeraldas(2)).amistad(10))
                .regalo(1, p -> dar(p, nv("gorro_duende"), 1))
                .regalo(2, p -> dar(p, nv("sueter_copos"), 1)));
        RENOS = NpcDef.registrar(new NpcDef(Navidad.MODID, "duende_renos").piel(piel("duende_cocinero")).tema(TEMA)
                .saludos(saludos("duende_renos"))
                .tienda(new Tienda()
                        .vende("minecraft:carrot", 8, 1)
                        .vende("minecraft:golden_carrot", 4, 2)
                        .vende("minecraft:apple", 6, 1)
                        .vende("minecraft:hay_block", 2, 1)
                        .vende(nv("cuernos_reno"), 1, 3)
                        .compra("minecraft:wheat", 20, 1))
                .mision(m("zanahorias").objetivo(Objetivo.entregar("minecraft:carrot", 12))
                        .premio(Mision.Premio.objeto("minecraft:golden_carrot", 6)).amistad(10))
                .mision(m("amigo_reno").objetivo(Objetivo.evento(nv("reno_amigo"), 1))
                        .premio(Mision.Premio.objeto(nv("cuernos_reno"), 1)).premio(Mision.Premio.esmeraldas(3)).amistad(12))
                .regalo(1, p -> dar(p, "minecraft:golden_carrot", 8)));
        pinero();
        viajero();
    }

    /**
     * Don Abeto, el del puesto de árboles de Navidad (sale solo en el mundo, con el puesto): vende arbolitos, pinos recién cortados,
     * adornos y luces por esmeraldas, y compra troncos y retoños de abeto.
     */
    private static void pinero() {
        PINERO = NpcDef.registrar(new NpcDef(Navidad.MODID, "pinero").piel(piel("pinero")).tema(TEMA).rutina(NpcDef.Rutina.QUIETO)
                .saludos(saludos("pinero"))
                .tienda(new Tienda()
                        .vende(nv("pino_navidad"), 1, 3)
                        .vende(nv("adorno_esfera_roja"), 4, 1)
                        .vende(nv("adorno_esfera_azul"), 4, 1)
                        .vende(nv("adorno_campanita"), 2, 1)
                        .vende(nv("adorno_luces"), 1, 3)
                        .vende(nv("adorno_guirnalda"), 1, 2)
                        .vende(nv("punta_estrella"), 1, 8, 1, 1)
                        .vende(nv("arbol_navidad"), 1, 6)
                        .vende(nv("arbol_nevado"), 1, 7)
                        .vende(nv("pino_atado"), 1, 4)
                        .vende(nv("pino_maceta"), 1, 2)
                        .vende(nv("arbol_dorado"), 1, 24, 1, 2)
                        .vende(nv("caja_adornos"), 4, 1)
                        .vende(nv("luces_colgantes"), 4, 1)
                        .vende(nv("guirnalda_luces"), 3, 1)
                        .vende(nv("esfera_roja"), 4, 1)
                        .vende(nv("esfera_dorada"), 4, 1)
                        .vende(nv("corona_navidad"), 1, 2)
                        .vende(nv("calcetin_rojo"), 1, 2)
                        .vende(nv("galletas_leche"), 1, 3)
                        .vende(nv("chocolate_caliente"), 2, 1)
                        .compra("minecraft:spruce_log", 12, 1)
                        .compra("minecraft:spruce_sapling", 10, 1)
                        .compra("minecraft:sweet_berries", 16, 1))
                .mision(m("troncos").objetivo(Objetivo.entregar("minecraft:spruce_log", 32))
                        .premio(Mision.Premio.objeto(nv("arbol_navidad"), 1)).premio(Mision.Premio.esmeraldas(2)).amistad(10))
                .mision(m("retonos").objetivo(Objetivo.entregar("minecraft:spruce_sapling", 8))
                        .premio(Mision.Premio.objeto(nv("pino_maceta"), 2)).amistad(8))
                .mision(m("adornar").requiere(nv("troncos")).objetivo(Objetivo.evento(nv("regalos_arbol"), 1))
                        .premio(Mision.Premio.objeto(nv("caja_adornos"), 12)).premio(Mision.Premio.esmeraldas(3)).amistad(12))
                .mision(m("chocolate").diaria().objetivo(Objetivo.entregar("minecraft:cocoa_beans", 6))
                        .premio(Mision.Premio.objeto(nv("chocolate_caliente"), 3)).amistad(4))
                .regalo(1, p -> dar(p, nv("luces_colgantes"), 8))
                .regalo(2, p -> dar(p, nv("arbol_nevado"), 1))
                .regalo(3, p -> dar(p, nv("arbol_dorado"), 1)));
    }

    /** Ramiro, el duende viajero (llega solo, como el comerciante errante, con su reno): cosas que no hay en otro lado. */
    private static void viajero() {
        VIAJERO = NpcDef.registrar(new NpcDef(Navidad.MODID, "duende_viajero").piel(piel("duende_viajero")).tema(TEMA)
                .rutina(NpcDef.Rutina.QUIETO).saludos(saludos("duende_viajero"))
                .tienda(new Tienda()
                        .vende(nv("lanzanieves"), 1, 10, 1, 1)
                        .vende(nv("tanque_nieve"), 1, 6, 1, 1)
                        .vende(nv("adorno_cristal"), 1, 4, 1, 2)
                        .vende(nv("adorno_pajarito"), 1, 4, 1, 2)
                        .vende(nv("adorno_pinguino"), 1, 4, 1, 2)
                        .vende(nv("adorno_renito"), 1, 4, 1, 2)
                        .vende(nv("trineo_madera"), 1, 8, 1, 1)
                        .vende(nv("patines"), 1, 4, 1, 2)
                        .vende(nv("arbol_dorado"), 1, 16, 1, 1)
                        .vende(nv("regalo_misterioso"), 1, 3, 1, 3)
                        .vende(nv("traje_santa"), 1, 14, 1, 1)
                        .vende(nv("disco_navidad"), 1, 6, 1, 1)
                        .vende(nv("disco_campanas"), 1, 6, 1, 1)
                        .vende(nv("calendario_adviento"), 1, 5, 1, 1)
                        .vende(nv("cuernos_reno"), 1, 3, 1, 2)
                        .vende(nv("bufanda"), 1, 2, 1, 2)
                        .vende(nv("orejeras"), 1, 2, 1, 2)
                        .vende(nv("uvas"), 12, 2, 1, 2)
                        .vende(nv("esfera_nieve"), 1, 3, 1, 2)
                        .compra("minecraft:cocoa_beans", 12, 1)
                        .compra("minecraft:sweet_berries", 16, 1))
                .regalo(1, p -> dar(p, nv("dulce"), 16)));
    }

    /** El gorro (o lo que lleve en la cabeza) de cada quien. */
    public static String gorroDe(NpcDef d) {
        if (d == SANTA) {
            return "gorro_santa";
        }
        if (d == SENORA) {
            return null;
        }
        if (d == RENOS) {
            return "cuernos_reno";
        }
        if (d == PINERO) {
            return "orejeras";
        }
        return "gorro_duende";
    }

    /** (Servidor) Pone a alguien del Polo Norte con su gorro. */
    public static Npc poner(ServerLevel l, NpcDef d, BlockPos pos) {
        Npc n = Npc.poner(l, d, pos);
        String g = gorroDe(d);
        if (n != null && g != null) {
            Vestuario.poner(l.getServer(), n.getUUID(), Ropa.id(g), null);
        }
        if (n != null && d == PINERO) {
            Vestuario.poner(l.getServer(), n.getUUID(), Ropa.id("bufanda"), null);
        }
        return n;
    }

    public static NpcDef porNombre(String n) {
        return NpcDef.porId(Navidad.MODID + ":" + n);
    }
}
