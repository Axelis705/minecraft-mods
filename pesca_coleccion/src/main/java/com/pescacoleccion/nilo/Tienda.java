package com.pescacoleccion.nilo;

import com.axeliscore.data.Actions;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.pesca.Cana;
import com.pescacoleccion.pesca.Cebo;
import com.pescacoleccion.pesca.PezItem;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

/** La tienda de Nilo: vende carnadas (y lo básico para pescar) y compra peces, todo en esmeraldas. El servidor siempre revisa. */
public final class Tienda {
    private static final String MOD = PescaColeccion.MODID;

    /** Lo que vende: qué, cuántos, precio en esmeraldas y la amistad que pide. */
    public record Oferta(Supplier<Item> item, int cuantos, int precio, int nivel) {
    }

    public static final List<Oferta> OFERTAS = List.of(
            new Oferta(() -> ModRegistry.CEBOS.get(Cebo.LOMBRIZ).get(), 8, 1, 0),
            new Oferta(() -> ModRegistry.CEBOS.get(Cebo.DULCE).get(), 4, 2, 0),
            new Oferta(() -> ModRegistry.CEBOS.get(Cebo.BRILLANTE).get(), 3, 3, 2),
            new Oferta(() -> ModRegistry.CEBOS.get(Cebo.LUNA).get(), 1, 5, 3),
            new Oferta(() -> ModRegistry.CANAS.get(Cana.BAMBU).get(), 1, 1, 0),
            new Oferta(() -> ModRegistry.LIBRO.get(), 1, 1, 0),
            new Oferta(() -> ModRegistry.TROFEO_ITEM.get(), 1, 2, 0),
            new Oferta(() -> ModRegistry.ACUARIO_ITEM.get(), 1, 4, 1));

    private Tienda() {
    }

    public static void init() {
        Actions.on(MOD + ":nilo_comprar", (p, d) -> {
            if (cerca(p)) {
                comprar(p, Nbt.i(d, "oferta"));
            }
        });
        Actions.on(MOD + ":nilo_vender", (p, d) -> {
            if (cerca(p)) {
                vender(p, Nbt.i(d, "slot"));
            }
        });
    }

    /** ¿Está junto a Nilo? (los botones de su ventana solo sirven a su lado) */
    public static boolean cerca(ServerPlayer p) {
        return !p.level().getEntitiesOfClass(Nilo.class, p.getBoundingBox().inflate(8)).isEmpty();
    }

    /** Esmeraldas que lleva. */
    public static int esmeraldas(Player p) {
        int n = 0;
        for (ItemStack st : Plat.mainAndOffhand(p.getInventory())) {
            if (st.is(Items.EMERALD)) {
                n += st.getCount();
            }
        }
        return n;
    }

    private static boolean pagar(ServerPlayer p, int n) {
        if (esmeraldas(p) < n) {
            return false;
        }
        for (ItemStack st : Plat.mainAndOffhand(p.getInventory())) {
            if (n <= 0) {
                break;
            }
            if (st.is(Items.EMERALD)) {
                int k = Math.min(n, st.getCount());
                st.shrink(k);
                n -= k;
            }
        }
        return true;
    }

    /** Lo que paga Nilo por esa pila de peces (0 si no es un pez). */
    public static int precio(ItemStack st) {
        if (!(st.getItem() instanceof PezItem pz) || pz.especie() == null) {
            return 0;
        }
        int rz = Math.min(4, pz.especie().rareza());
        return (PezItem.grande(st, pz.especie()) ? Encargos.VALOR_GRANDE[rz] : Encargos.VALOR[rz]) * st.getCount();
    }

    static void comprar(ServerPlayer p, int i) {
        if (i < 0 || i >= OFERTAS.size()) {
            return;
        }
        Oferta o = OFERTAS.get(i);
        int nivel = Encargos.nivel(Nbt.i(Encargos.datos(p), "amistad"));
        if (nivel < o.nivel()) {
            return;
        }
        if (!pagar(p, o.precio())) {
            Plat.msg(p, Component.translatable(MOD + ".nilo.no_alcanza"), true);
            return;
        }
        Plat.give(p, new ItemStack(o.item().get(), o.cuantos()));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.6F, 0.9F);
        PescaColeccion.LOGGER.info("PESCA_NILO_COMPRA oferta={} precio={}", i, o.precio());
    }

    /** Vende la pila de peces del lugar {@code slot} del inventario principal. */
    static void vender(ServerPlayer p, int slot) {
        List<ItemStack> inv = Plat.mainItems(p.getInventory());
        if (slot < 0 || slot >= inv.size()) {
            return;
        }
        ItemStack st = inv.get(slot);
        int pago = precio(st);
        if (pago <= 0) {
            return;
        }
        st.setCount(0);
        Plat.give(p, new ItemStack(Items.EMERALD, pago));
        Encargos.datos(p).putInt("vendidos", Nbt.i(Encargos.datos(p), "vendidos") + 1);
        PlayerStore.save(p, MOD);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.5F, 1.2F);
        PescaColeccion.LOGGER.info("PESCA_NILO_VENTA esmeraldas={}", pago);
    }
}
