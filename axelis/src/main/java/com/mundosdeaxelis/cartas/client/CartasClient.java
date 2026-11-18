package com.mundosdeaxelis.cartas.client;

import com.mundosdeaxelis.cartas.AlbumLogic;
import com.mundosdeaxelis.cartas.CartasNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

/** Lado cliente: guarda la ultima copia del album que mando el servidor y abre las pantallas. Solo se carga en el cliente. */
public final class CartasClient {
    /** copia del album tal como la mando el servidor */
    public static final AlbumLogic ALBUM = new AlbumLogic();

    private CartasClient() {
    }

    private static void setAlbum(java.util.Set<String> normal, java.util.Set<String> holo) {
        ALBUM.clear();
        ALBUM.normal.addAll(normal);
        ALBUM.holo.addAll(holo);
    }

    public static void onAlbumOpen(CartasNet.AlbumOpen m) {
        setAlbum(m.normal(), m.holo());
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AlbumScreen s) {
            s.refresh();
        } else {
            mc.setScreen(new AlbumScreen());
        }
    }

    public static void onInsertResult(CartasNet.InsertResult m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AlbumScreen s) {
            s.onInserted(m);
        }
        setAlbum(m.normal(), m.holoAll());
    }

    public static void onPackOpen(CartasNet.PackOpen m) {
        Minecraft.getInstance().setScreen(new PackOpenScreen(m));
    }

    public static void play(SoundEvent e, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(e, pitch, volume));
    }
}
