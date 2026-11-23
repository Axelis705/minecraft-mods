package com.axeliscore.vestir;

import com.axeliscore.AxelisCore;
import com.axeliscore.data.PlayerStore;
import com.axeliscore.data.WorldStore;
import com.axeliscore.plat.Plat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * Vestuario: ropa, uniformes, disfraces y accesorios que se dibujan <b>encima</b> de un jugador (o de un personaje del Core), sin tocar su
 * piel y sin ocupar los espacios de armadura. El servidor decide quién lleva qué; cada cliente lo dibuja con la prenda registrada con ese
 * nombre ({@link com.axeliscore.vestir.client.VestuarioCliente#registrar}).
 * <pre>
 *     // servidor: el uniforme del evento con el número de la pieza
 *     CompoundTag d = new CompoundTag();
 *     d.putString("numero", "07");
 *     Vestuario.poner(server, jugador.getUUID(), Ids.rl("mimod", "uniforme"), d);
 *
 *     // cliente (al iniciar): cómo se ve esa prenda
 *     VestuarioCliente.registrar(Ids.rl("mimod", "uniforme"), Prenda.piel(Ids.rl("mimod", "textures/prenda/uniforme.png")));
 * </pre>
 * Se guarda con el mundo (sigue puesta al reiniciar) y les llega a todos al entrar. Para probar: {@code /axeliscore vestir}.
 */
public final class Vestuario {
    /** Lo que se le manda a los clientes: {uuid: {prenda: datos}}. */
    public static final String KEY = AxelisCore.MODID + ":vestuario";

    private Vestuario() {
    }

    public static void init() {
        AxelisCore.onLogin(p -> PlayerStore.Sync.toPlayer(p, KEY, todo(Plat.server(p))));
    }

    private static CompoundTag todo(MinecraftServer s) {
        if (s == null) {
            return new CompoundTag();
        }
        CompoundTag g = WorldStore.get(s, AxelisCore.MODID).global();
        if (!g.contains("vestuario")) {
            g.put("vestuario", new CompoundTag());
        }
        return g.getCompound("vestuario");
    }

    private static void sync(MinecraftServer s) {
        WorldStore.get(s, AxelisCore.MODID).dirty();
        PlayerStore.Sync.toAll(s, KEY, todo(s));
    }

    /** Le pone una prenda a alguien (con sus datos: número, color, variante... lo que la prenda sepa leer). Si ya la tenía, la cambia. */
    public static void poner(MinecraftServer s, UUID quien, ResourceLocation prenda, CompoundTag datos) {
        CompoundTag t = todo(s);
        String k = quien.toString();
        CompoundTag suyas = t.getCompound(k);
        suyas.put(prenda.toString(), datos == null ? new CompoundTag() : datos.copy());
        t.put(k, suyas);
        sync(s);
    }

    /** Le quita una prenda. */
    public static void quitar(MinecraftServer s, UUID quien, ResourceLocation prenda) {
        CompoundTag t = todo(s);
        String k = quien.toString();
        if (!t.contains(k)) {
            return;
        }
        CompoundTag suyas = t.getCompound(k);
        suyas.remove(prenda.toString());
        if (suyas.isEmpty()) {
            t.remove(k);
        }
        sync(s);
    }

    /** Le quita todo. */
    public static void quitarTodo(MinecraftServer s, UUID quien) {
        CompoundTag t = todo(s);
        if (t.contains(quien.toString())) {
            t.remove(quien.toString());
            sync(s);
        }
    }

    /** Le quita esa prenda a todos (por ejemplo, al terminar un evento). */
    public static void quitarDeTodos(MinecraftServer s, ResourceLocation prenda) {
        CompoundTag t = todo(s);
        boolean cambio = false;
        for (String k : t.getAllKeys().toArray(new String[0])) {
            CompoundTag suyas = t.getCompound(k);
            if (suyas.contains(prenda.toString())) {
                suyas.remove(prenda.toString());
                cambio = true;
                if (suyas.isEmpty()) {
                    t.remove(k);
                }
            }
        }
        if (cambio) {
            sync(s);
        }
    }

    /** ¿La lleva puesta? */
    public static boolean tiene(MinecraftServer s, UUID quien, ResourceLocation prenda) {
        return todo(s).getCompound(quien.toString()).contains(prenda.toString());
    }

    /** Los datos con que la lleva (vacío si no la lleva). */
    public static CompoundTag datos(MinecraftServer s, UUID quien, ResourceLocation prenda) {
        return todo(s).getCompound(quien.toString()).getCompound(prenda.toString()).copy();
    }
}
