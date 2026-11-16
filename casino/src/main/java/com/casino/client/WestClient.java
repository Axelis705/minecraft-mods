package com.casino.client;

import com.casino.net.CasinoNet;
import com.casino.west.BullRideEntity;
import com.casino.west.GalleryBE;
import com.casino.west.WestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/** Lo del cliente para los juegos del saloon: abrir la pantalla de apuesta, mandar las teclas del jinete y saber si estás tirando. */
public final class WestClient {
    public static final int BULL = 0;
    public static final int GALLERY = 1;
    public static final int DUEL = 2;

    private static int lastInput;
    private static int sendAgain;
    private static GalleryBE gallery;
    private static long gallerySeenAt;
    private static long lastRecoil;

    private WestClient() {
    }

    public static void openBet(BlockPos pos, int kind) {
        ClientHooks.open(new WestBetScreen(pos, kind));
    }

    /** Cada tick de la montada (en el cliente): si el jinete soy yo, mando A/D cuando cambian (y de vez en cuando por si se pierde). */
    public static void bullTick(BullRideEntity e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getVehicle() != e) {
            return;
        }
        boolean l = mc.options.keyLeft.isDown(), r = mc.options.keyRight.isDown();
        int in = l == r ? 0 : l ? -1 : 1;
        if (!ClientScreens.none()) {
            in = 0;
        }
        if (in != lastInput || --sendAgain <= 0) {
            lastInput = in;
            sendAgain = 10;
            CasinoNet.sendToServer(new WestPacket(WestPacket.BULL_INPUT, BlockPos.ZERO, in));
        }
    }

    /** La tecla que está apretando el jinete ahora (para el aviso). */
    public static int input() {
        return lastInput;
    }

    /** Lo llama la galería en cada tick si yo soy el tirador. */
    public static void gallerySeen(GalleryBE be) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && be.isShooter(mc.player.getUUID())) {
            gallery = be;
            gallerySeenAt = mc.level == null ? 0 : mc.level.getGameTime();
        }
    }

    /** La galería donde estoy tirando (o null). */
    public static GalleryBE gallery() {
        Minecraft mc = Minecraft.getInstance();
        if (gallery == null || mc.level == null || mc.player == null || gallery.isRemoved() || gallery.getLevel() != mc.level
                || mc.level.getGameTime() - gallerySeenAt > 5 || !gallery.isShooter(mc.player.getUUID())) {
            gallery = null;
        }
        return gallery;
    }

    /** Patada del revólver: la mira sube un poquito (al ritmo al que el servidor deja disparar). */
    public static void recoil(Player p) {
        Minecraft mc = Minecraft.getInstance();
        if (p != mc.player || mc.level == null) {
            return;
        }
        GalleryBE g = gallery();
        com.casino.west.DuelBE d = duel();
        if (g == null && d == null) {
            return;
        }
        if (g != null && (g.time(0) < 0 || g.shots() <= 0)) {
            return;
        }
        if (d != null && !d.drawn(0)) {
            return;
        }
        long now = mc.level.getGameTime();
        if (now - lastRecoil < com.casino.game.GalleryPlan.COOLDOWN) {
            return;
        }
        lastRecoil = now;
        p.setXRot(p.getXRot() - 2.2F);
    }

    // ------------------------------------------------------------------ duelo

    private static com.casino.west.DuelBE duel;
    private static long duelSeenAt;

    public static void duelSeen(com.casino.west.DuelBE be) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && be.isShooter(mc.player.getUUID())) {
            duel = be;
            duelSeenAt = mc.level == null ? 0 : mc.level.getGameTime();
        }
    }

    /** El duelo en el que estoy (o null). */
    public static com.casino.west.DuelBE duel() {
        Minecraft mc = Minecraft.getInstance();
        if (duel == null || mc.level == null || mc.player == null || duel.isRemoved() || duel.getLevel() != mc.level
                || mc.level.getGameTime() - duelSeenAt > 5 || !duel.isShooter(mc.player.getUUID())) {
            duel = null;
        }
        return duel;
    }
}
