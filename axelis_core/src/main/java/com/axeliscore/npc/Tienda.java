package com.axeliscore.npc;

import com.axeliscore.AxelisCore;
import com.axeliscore.combat.Combate;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * La tienda de un {@link NpcDef}: lo que vende (el jugador paga) y lo que compra (el jugador recibe). Se paga con esmeraldas o con la
 * moneda que se elija. Las existencias son por jugador y se renuevan cada día del juego.
 * <pre>
 *     new Tienda().vende("minecraft:bread", 3, 1)                // 3 panes por 1 esmeralda
 *                 .vende("minecraft:diamond", 1, 8, 2, 2)        // 2 al día, con amistad nivel 2
 *                 .compra("minecraft:wheat", 16, 1)              // 16 de trigo → 1 esmeralda
 *                 .moneda("mimod:moneda");
 * </pre>
 */
public final class Tienda {
    /** Una línea de la tienda. */
    public static final class Oferta {
        public final boolean vende;
        public final String objeto;
        public final int cuantos, precio, existencias, nivel;

        Oferta(boolean vende, String objeto, int cuantos, int precio, int existencias, int nivel) {
            this.vende = vende;
            this.objeto = objeto;
            this.cuantos = Math.max(1, cuantos);
            this.precio = Math.max(0, precio);
            this.existencias = existencias;
            this.nivel = nivel;
        }

        public ItemStack icono() {
            return new ItemStack(Plat.item(objeto), cuantos);
        }

        /** ¿Tiene límite por día? */
        public boolean limitada() {
            return existencias > 0;
        }
    }

    final List<Oferta> ofertas = new ArrayList<>();
    String moneda = "minecraft:emerald";

    /** Vende {@code n} objetos por {@code precio} monedas, sin límite. */
    public Tienda vende(String objeto, int n, int precio) {
        return vende(objeto, n, precio, 0, 0);
    }

    /** Vende con existencias por día (0 = sin límite) y nivel de amistad pedido. */
    public Tienda vende(String objeto, int n, int precio, int existencias, int nivel) {
        ofertas.add(new Oferta(true, objeto, n, precio, existencias, nivel));
        return this;
    }

    /** Compra {@code n} objetos y paga {@code precio} monedas. */
    public Tienda compra(String objeto, int n, int precio) {
        return compra(objeto, n, precio, 0, 0);
    }

    public Tienda compra(String objeto, int n, int precio, int existencias, int nivel) {
        ofertas.add(new Oferta(false, objeto, n, precio, existencias, nivel));
        return this;
    }

    /** Con qué se paga (por defecto esmeraldas). */
    public Tienda moneda(String objeto) {
        this.moneda = objeto;
        return this;
    }

    public List<Oferta> ofertas() {
        return ofertas;
    }

    public String moneda() {
        return moneda;
    }

    public ItemStack iconoMoneda(int n) {
        return new ItemStack(Plat.item(moneda), Math.max(1, Math.min(64, n)));
    }

    // ------------------------------------------------------------------ servidor

    private static CompoundTag datos(ServerPlayer p, String npc) {
        CompoundTag d = PlayerStore.of(p, AxelisCore.MODID);
        if (!d.contains("tiendas")) {
            d.put("tiendas", new CompoundTag());
        }
        CompoundTag t = Nbt.c(d, "tiendas");
        if (!t.contains(npc)) {
            t.put(npc, new CompoundTag());
        }
        CompoundTag n = Nbt.c(t, npc);
        long hoy = Misiones.dia(p);
        if (Nbt.l(n, "dia") != hoy) {
            // día nuevo: se renuevan las existencias
            for (String k : new ArrayList<>(n.getAllKeys())) {
                n.remove(k);
            }
            n.putLong("dia", hoy);
        }
        return n;
    }

    /** Cuántas veces más se puede usar esa oferta hoy (Integer.MAX_VALUE si no tiene límite). */
    public int quedan(ServerPlayer p, String npc, int i) {
        Oferta o = ofertas.get(i);
        if (!o.limitada()) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, o.existencias - Nbt.i(datos(p, npc), "o" + i));
    }

    /**
     * Usa una oferta {@code veces} veces (comprar si el NPC vende, vender si compra). Devuelve cuántas veces se hizo (0 si no se pudo:
     * falta dinero, objetos, existencias o amistad).
     */
    public int usar(ServerPlayer p, String npc, int i, int veces) {
        if (i < 0 || i >= ofertas.size() || veces <= 0) {
            return 0;
        }
        Oferta o = ofertas.get(i);
        if (o.nivel > 0 && Amistad.nivel(p, npc) < o.nivel) {
            Plat.msg(p, Component.translatable("axelis_core.tienda.amistad").withStyle(ChatFormatting.RED), true);
            return 0;
        }
        int n = Math.min(veces, quedan(p, npc, i));
        if (n <= 0) {
            Plat.msg(p, Component.translatable("axelis_core.tienda.agotado").withStyle(ChatFormatting.RED), true);
            return 0;
        }
        if (o.vende) {
            if (o.precio > 0) {
                n = Math.min(n, Misiones.contar(p, moneda) / o.precio);
            }
            if (n <= 0) {
                Plat.msg(p, Component.translatable("axelis_core.tienda.sin_dinero").withStyle(ChatFormatting.RED), true);
                return 0;
            }
            Misiones.quitar(p, moneda, o.precio * n);
            darVarios(p, o.objeto, o.cuantos * n);
        } else {
            n = Math.min(n, Misiones.contar(p, o.objeto) / o.cuantos);
            if (n <= 0) {
                Plat.msg(p, Component.translatable("axelis_core.tienda.sin_objetos").withStyle(ChatFormatting.RED), true);
                return 0;
            }
            Misiones.quitar(p, o.objeto, o.cuantos * n);
            darVarios(p, moneda, o.precio * n);
        }
        if (o.limitada()) {
            CompoundTag d = datos(p, npc);
            d.putInt("o" + i, Nbt.i(d, "o" + i) + n);
        }
        PlayerStore.save(p, AxelisCore.MODID);
        Combate.sonido(p.level(), p.position(), "minecraft:entity.villager.yes", 0.6F, 1.1F);
        AxelisCore.LOGGER.info("AXCORE_TIENDA {} oferta={} veces={}", npc, i, n);
        return n;
    }

    /** Da muchos objetos en pilas del tamaño que permite el objeto. */
    static void darVarios(ServerPlayer p, String id, int total) {
        net.minecraft.world.item.Item it = Plat.item(id);
        int max = Math.max(1, new ItemStack(it).getMaxStackSize());
        while (total > 0) {
            int q = Math.min(max, total);
            Plat.give(p, new ItemStack(it, q));
            total -= q;
        }
    }
}
