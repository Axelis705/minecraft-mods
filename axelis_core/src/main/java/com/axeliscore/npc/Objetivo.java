package com.axeliscore.npc;

import com.axeliscore.plat.Plat;
import net.minecraft.network.chat.Component;

/**
 * Un objetivo de una {@link Mision}:
 * <ul>
 *     <li>{@link #cazar}: derrotar criaturas ("minecraft:zombie", o "*" para cualquiera hostil).</li>
 *     <li>{@link #entregar}: llevar objetos (se los quedan al entregar la misión).</li>
 *     <li>{@link #tener}: tener objetos en el inventario (no se los quedan).</li>
 *     <li>{@link #hablar}: hablar con otro NPC.</li>
 *     <li>{@link #llegar}: llegar a un lugar (x, z, radio).</li>
 *     <li>{@link #evento}: algo que avisa el mod con {@code Misiones.evento(jugador, "mimod:pescar", 1)}.</li>
 * </ul>
 */
public final class Objetivo {
    public enum Tipo {
        CAZAR, ENTREGAR, TENER, HABLAR, LLEGAR, EVENTO
    }

    public final Tipo tipo;
    /** Lo que cuenta: criatura, objeto, NPC o evento. */
    public final String que;
    public final int cantidad;
    public final int x, z, radio;
    String texto;

    private Objetivo(Tipo tipo, String que, int cantidad, int x, int z, int radio) {
        this.tipo = tipo;
        this.que = que;
        this.cantidad = Math.max(1, cantidad);
        this.x = x;
        this.z = z;
        this.radio = radio;
    }

    public static Objetivo cazar(String criatura, int n) {
        return new Objetivo(Tipo.CAZAR, criatura, n, 0, 0, 0);
    }

    public static Objetivo entregar(String objeto, int n) {
        return new Objetivo(Tipo.ENTREGAR, objeto, n, 0, 0, 0);
    }

    public static Objetivo tener(String objeto, int n) {
        return new Objetivo(Tipo.TENER, objeto, n, 0, 0, 0);
    }

    public static Objetivo hablar(String npc) {
        return new Objetivo(Tipo.HABLAR, npc, 1, 0, 0, 0);
    }

    public static Objetivo llegar(int x, int z, int radio) {
        return new Objetivo(Tipo.LLEGAR, "", 1, x, z, radio);
    }

    public static Objetivo evento(String clave, int n) {
        return new Objetivo(Tipo.EVENTO, clave, n, 0, 0, 0);
    }

    /** Texto propio (clave de traducción) en vez del automático. */
    public Objetivo texto(String clave) {
        this.texto = clave;
        return this;
    }

    /** Lo que se lee en el diario: "Derrota zombis", "Entrega pan"... */
    public Component descripcion() {
        if (texto != null) {
            return Component.translatable(texto);
        }
        return switch (tipo) {
            case CAZAR -> Component.translatable("axelis_core.obj.cazar", nombreCriatura());
            case ENTREGAR -> Component.translatable("axelis_core.obj.entregar", nombreObjeto(que));
            case TENER -> Component.translatable("axelis_core.obj.tener", nombreObjeto(que));
            case HABLAR -> {
                NpcDef d = NpcDef.porId(que);
                yield Component.translatable("axelis_core.obj.hablar", d == null ? Component.literal(que) : d.nombre());
            }
            case LLEGAR -> Component.translatable("axelis_core.obj.llegar", x, z);
            case EVENTO -> Component.translatable("axelis_core.obj.evento." + que.replace(':', '.'));
        };
    }

    /** Nombre de un objeto por su id (igual en todas las versiones). */
    public static Component nombreObjeto(String id) {
        return new net.minecraft.world.item.ItemStack(Plat.item(id)).getHoverName();
    }

    private Component nombreCriatura() {
        if ("*".equals(que)) {
            return Component.translatable("axelis_core.obj.monstruos");
        }
        return Component.translatable("entity." + que.replace(':', '.'));
    }

    /** ¿Esta criatura cuenta? */
    boolean cuenta(String tipoId, boolean hostil) {
        return "*".equals(que) ? hostil : que.equals(tipoId);
    }
}
