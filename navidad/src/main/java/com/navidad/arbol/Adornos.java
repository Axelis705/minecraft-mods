package com.navidad.arbol;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Los adornos para el Pino de Navidad: qué tipo son (se cuelgan de una rama, son luces, guirnalda o van en la punta), cuántos puntos
 * le dan al árbol, qué tan raros son y dónde está su gancho en el modelo (para colgarlos derechitos). Los modelos los hace
 * {@code herramientas/adornos.py}.
 */
public final class Adornos {
    public enum Tipo {
        COLGANTE, LUCES, GUIRNALDA, PUNTA
    }

    public enum Rareza {
        COMUN(ChatFormatting.WHITE), BONITO(ChatFormatting.GREEN), RARO(ChatFormatting.AQUA), EPICO(ChatFormatting.LIGHT_PURPLE);

        public final ChatFormatting color;

        Rareza(ChatFormatting c) {
            this.color = c;
        }
    }

    /** Un adorno: su id, tipo, puntos, rareza y la altura de su gancho en el modelo (0 a 16). */
    public record Def(String id, Tipo tipo, int puntos, Rareza rareza, float ancla) {
    }

    public static final Map<String, Def> TODOS = new LinkedHashMap<>();

    private static void def(String id, Tipo t, int puntos, Rareza r, float ancla) {
        TODOS.put(id, new Def(id, t, puntos, r, ancla));
    }

    static {
        def("adorno_esfera_roja", Tipo.COLGANTE, 1, Rareza.COMUN, 14);
        def("adorno_esfera_azul", Tipo.COLGANTE, 1, Rareza.COMUN, 14);
        def("adorno_esfera_dorada", Tipo.COLGANTE, 1, Rareza.COMUN, 14);
        def("adorno_baston", Tipo.COLGANTE, 1, Rareza.COMUN, 14);
        def("adorno_mono", Tipo.COLGANTE, 1, Rareza.COMUN, 12);
        def("adorno_campanita", Tipo.COLGANTE, 2, Rareza.BONITO, 14);
        def("adorno_copo", Tipo.COLGANTE, 2, Rareza.BONITO, 14);
        def("adorno_galleta", Tipo.COLGANTE, 2, Rareza.BONITO, 14);
        def("adorno_pina", Tipo.COLGANTE, 2, Rareza.BONITO, 14);
        def("adorno_cristal", Tipo.COLGANTE, 4, Rareza.RARO, 14);
        def("adorno_pajarito", Tipo.COLGANTE, 4, Rareza.RARO, 4);
        def("adorno_pinguino", Tipo.COLGANTE, 4, Rareza.RARO, 14);
        def("adorno_renito", Tipo.COLGANTE, 4, Rareza.RARO, 14);
        def("adorno_luces", Tipo.LUCES, 3, Rareza.BONITO, 14);
        def("adorno_guirnalda", Tipo.GUIRNALDA, 2, Rareza.COMUN, 14);
        def("punta_estrella", Tipo.PUNTA, 6, Rareza.RARO, 0);
        def("punta_angel", Tipo.PUNTA, 8, Rareza.RARO, 0);
        def("punta_polar", Tipo.PUNTA, 15, Rareza.EPICO, 0);
    }

    private Adornos() {
    }

    public static Def de(String id) {
        return id == null ? null : TODOS.get(id);
    }

    /** El adorno de un objeto (o null si no es adorno). */
    public static Def de(Item it) {
        ResourceLocation k = ForgeRegistries.ITEMS.getKey(it);
        return k != null && com.navidad.Navidad.MODID.equals(k.getNamespace()) ? TODOS.get(k.getPath()) : null;
    }

    public static Item item(String id) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(com.navidad.Navidad.MODID, id));
    }
}
