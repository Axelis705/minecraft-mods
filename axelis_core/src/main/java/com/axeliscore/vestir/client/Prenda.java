package com.axeliscore.vestir.client;

import com.axeliscore.anim.Geo;
import com.axeliscore.anim.GeoLoader;
import com.axeliscore.anim.client.GeoAssets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Cómo se ve una prenda del {@link com.axeliscore.vestir.Vestuario}. Dos clases:
 * <ul>
 *     <li><b>Piel</b> ({@link #piel}): una textura de 64x64 con el mismo acomodo que una skin de Minecraft (camisa, mangas, pantalón,
 *     zapatos, gorro). Se dibuja un poquito más grande que el cuerpo, encima de la skin (y de su segunda capa), y se mueve igual. Lo
 *     transparente deja ver la skin (por ejemplo las manos o la cara).</li>
 *     <li><b>Modelo</b> ({@link #modelo}): un modelo de Blockbench (geo.json o .bbmodel en el estudio) cuyos grupos de arriba se llaman como
 *     las partes del cuerpo ({@code cabeza/head, cuerpo/body, brazo_der/right_arm, brazo_izq/left_arm, pierna_der/right_leg,
 *     pierna_izq/left_leg}); cada uno sigue a esa parte. Hecho sobre la plantilla de jugador de Blockbench, queda en su lugar.</li>
 * </ul>
 * Opciones: otra textura para brazos delgados, cuánto se infla, qué partes lleva, textura o color según quién la lleva, y un
 * {@link Decorador} para dibujar encima (un número en el pecho, un nombre...).
 */
public final class Prenda {
    /** Las partes del cuerpo que puede seguir una prenda. */
    public enum Parte {
        CABEZA, CUERPO, BRAZO_DER, BRAZO_IZQ, PIERNA_DER, PIERNA_IZQ
    }

    /** Quién la lleva y con qué datos (los que puso el servidor). {@code entidad} puede ser null. */
    public record Contexto(UUID uuid, Entity entidad, CompoundTag datos, boolean delgado, boolean primeraPersona) {
        /** Un número sacado del uuid (para variantes fijas por persona: manchas, desgaste...). */
        public int semilla() {
            return (int) (uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
        }
    }

    /** Dibuja algo más encima de la prenda (en el espacio de una parte del cuerpo). */
    public interface Decorador {
        void dibujar(Contexto c, Lienzo l);
    }

    final ResourceLocation geo, geoDelgado;
    final ResourceLocation textura;
    ResourceLocation texturaDelgada;
    Function<Contexto, ResourceLocation> elegir;
    Function<Contexto, Integer> tinte;
    Decorador decorador;
    float inflar = 0.3F;
    boolean segundaCapa, primeraPersona = true, translucida;
    final EnumSet<Parte> partes = EnumSet.allOf(Parte.class);

    private Prenda(ResourceLocation geo, ResourceLocation geoDelgado, ResourceLocation textura) {
        this.geo = geo;
        this.geoDelgado = geoDelgado;
        this.textura = textura;
    }

    /** Una prenda pintada como una skin (64x64, mismo acomodo). */
    public static Prenda piel(ResourceLocation textura) {
        return new Prenda(null, null, textura);
    }

    /** Una prenda con modelo de Blockbench (assets/mod/geo/nombre.geo.json) y su textura. */
    public static Prenda modelo(ResourceLocation geo, ResourceLocation textura) {
        return new Prenda(geo, null, textura);
    }

    /** Lo mismo, con otro modelo para quien tiene brazos delgados (skin "Alex"). */
    public static Prenda modelo(ResourceLocation geo, ResourceLocation geoDelgado, ResourceLocation textura) {
        return new Prenda(geo, geoDelgado, textura);
    }

    /** Textura para brazos delgados (en las de tipo piel las mangas miden 3 px de ancho, como en una skin "Alex"). */
    public Prenda delgada(ResourceLocation textura) {
        this.texturaDelgada = textura;
        return this;
    }

    /** Cuánto más grande que el cuerpo (píxeles; 0.3 tapa la segunda capa de la skin). La cabeza siempre va un poco más. */
    public Prenda inflar(float px) {
        this.inflar = px;
        return this;
    }

    /** También dibuja la segunda capa de la textura (como la chamarra de una skin), un poco más afuera. */
    public Prenda segundaCapa() {
        this.segundaCapa = true;
        return this;
    }

    /** Partes que NO lleva (por ejemplo, sin cabeza para que se vea la cara). */
    public Prenda sin(Parte... ps) {
        for (Parte p : ps) {
            partes.remove(p);
        }
        return this;
    }

    /** La textura según quién la lleva (por ejemplo, una variante de manchas por persona). null = la normal. */
    public Prenda textura(Function<Contexto, ResourceLocation> f) {
        this.elegir = f;
        return this;
    }

    /** Color que multiplica la textura según quién la lleva (ARGB; con alfa menor que 255 se ve transparente). */
    public Prenda tinte(Function<Contexto, Integer> f) {
        this.tinte = f;
        return this;
    }

    public Prenda decorar(Decorador d) {
        this.decorador = d;
        return this;
    }

    /** No se dibuja en la mano de primera persona. */
    public Prenda sinPrimeraPersona() {
        this.primeraPersona = false;
        return this;
    }

    /** Se dibuja semitransparente (vidrio, tela fina...). */
    public Prenda translucida() {
        this.translucida = true;
        return this;
    }

    // ------------------------------------------------------------------ uso interno

    ResourceLocation texturaPara(Contexto c) {
        ResourceLocation t = elegir != null ? elegir.apply(c) : null;
        if (t == null) {
            t = c.delgado() && texturaDelgada != null ? texturaDelgada : textura;
        }
        return t;
    }

    int tintePara(Contexto c) {
        Integer t = tinte != null ? tinte.apply(c) : null;
        return t == null ? 0xFFFFFFFF : t;
    }

    Geo geoPara(boolean delgado) {
        if (geo != null) {
            return GeoAssets.geo(delgado && geoDelgado != null ? geoDelgado : geo);
        }
        int mascara = 0;
        for (Parte p : partes) {
            mascara |= 1 << p.ordinal();
        }
        String k = inflar + "|" + segundaCapa + "|" + delgado + "|" + mascara;
        return PIELES.computeIfAbsent(k, x -> GeoLoader.parse(pielJson(inflar, segundaCapa, delgado, partes)));
    }

    private static final Map<String, Geo> PIELES = new HashMap<>();

    /** La parte del cuerpo que sigue un hueso, por su nombre (null si no es ninguna). */
    public static Parte parteDe(String hueso) {
        String n = hueso.toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
        return switch (n) {
            case "cabeza", "head", "casco", "helmet" -> Parte.CABEZA;
            case "cuerpo", "body", "torso", "pecho", "chest" -> Parte.CUERPO;
            case "brazo_der", "brazo_derecho", "right_arm", "rightarm" -> Parte.BRAZO_DER;
            case "brazo_izq", "brazo_izquierdo", "left_arm", "leftarm" -> Parte.BRAZO_IZQ;
            case "pierna_der", "pierna_derecha", "right_leg", "rightleg" -> Parte.PIERNA_DER;
            case "pierna_izq", "pierna_izquierda", "left_leg", "leftleg" -> Parte.PIERNA_IZQ;
            default -> null;
        };
    }

    /**
     * El modelo de una prenda tipo piel: las seis partes del jugador (como en Blockbench, con la plantilla de jugador), cada una un poco
     * inflada, con las coordenadas de textura de una skin de 64x64.
     */
    static String pielJson(float inflar, boolean segunda, boolean delgado, EnumSet<Parte> partes) {
        StringBuilder b = new StringBuilder();
        b.append("{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"description\":{\"identifier\":\"geometry.axelis_core.prenda\",")
                .append("\"texture_width\":64,\"texture_height\":64},\"bones\":[");
        float ab = delgado ? 3 : 4; // ancho del brazo
        float ay = delgado ? 11.5F : 12F; // dónde empieza el brazo (abajo)
        float py = delgado ? 21.5F : 22F;
        Object[][] defs = {
                // nombre, parte, pivote, origen, tamaño, uv, uv de la segunda capa, inflado extra
                {"cabeza", Parte.CABEZA, new float[] {0, 24, 0}, new float[] {-4, 24, -4}, new float[] {8, 8, 8}, new int[] {0, 0}, new int[] {32, 0}, 0.3F},
                {"cuerpo", Parte.CUERPO, new float[] {0, 24, 0}, new float[] {-4, 12, -2}, new float[] {8, 12, 4}, new int[] {16, 16}, new int[] {16, 32}, 0F},
                {"brazo_der", Parte.BRAZO_DER, new float[] {-5, py, 0}, new float[] {-4 - ab, ay, -2}, new float[] {ab, 12, 4}, new int[] {40, 16},
                        new int[] {40, 32}, 0.02F},
                {"brazo_izq", Parte.BRAZO_IZQ, new float[] {5, py, 0}, new float[] {4, ay, -2}, new float[] {ab, 12, 4}, new int[] {32, 48},
                        new int[] {48, 48}, 0.02F},
                {"pierna_der", Parte.PIERNA_DER, new float[] {-1.9F, 12, 0}, new float[] {-3.9F, 0, -2}, new float[] {4, 12, 4}, new int[] {0, 16},
                        new int[] {0, 32}, 0.01F},
                {"pierna_izq", Parte.PIERNA_IZQ, new float[] {1.9F, 12, 0}, new float[] {-0.1F, 0, -2}, new float[] {4, 12, 4}, new int[] {16, 48},
                        new int[] {0, 48}, 0.01F}};
        boolean primero = true;
        for (Object[] d : defs) {
            if (!partes.contains((Parte) d[1])) {
                continue;
            }
            float[] piv = (float[]) d[2], o = (float[]) d[3], s = (float[]) d[4];
            int[] uv = (int[]) d[5], uv2 = (int[]) d[6];
            float inf = inflar + (Float) d[7];
            if (!primero) {
                b.append(',');
            }
            primero = false;
            b.append("{\"name\":\"").append(d[0]).append("\",\"pivot\":").append(v(piv)).append(",\"cubes\":[");
            b.append(cubo(o, s, uv, inf));
            if (segunda) {
                b.append(',').append(cubo(o, s, uv2, inf + 0.25F));
            }
            b.append("]}");
        }
        b.append("]}]}");
        return b.toString();
    }

    private static String cubo(float[] o, float[] s, int[] uv, float inf) {
        return "{\"origin\":" + v(o) + ",\"size\":" + v(s) + ",\"uv\":[" + uv[0] + "," + uv[1] + "],\"inflate\":" + inf + "}";
    }

    private static String v(float[] a) {
        return "[" + a[0] + "," + a[1] + "," + a[2] + "]";
    }
}
