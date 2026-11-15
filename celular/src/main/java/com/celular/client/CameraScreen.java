package com.celular.client;

import com.celular.CelularReg;
import com.celular.plat.client.BaseScreen;
import com.celular.plat.client.G;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

/**
 * La cámara: se ve el mundo con un visor cuadrado. Arrastrando con el mouse se mira alrededor; el botón grande (o Espacio) toma la foto
 * y la guarda en la galería. Con el botón de girar (o Tab) se cambia a selfie: la cámara se pone delante del jugador.
 */
public class CameraScreen extends BaseScreen {
    private final int color;
    private boolean selfie;
    private CameraType before;
    //#if MC < 26.1
    private boolean hideBefore;
    //#elif MC >= 26.3
    //$$ private boolean hudBefore;
    //#endif
    /**
     * Se pidió una foto. 1: se toma al empezar el siguiente cuadro (ya sin el HUD). 2 (en 26.x): primero un cuadro sin el visor, porque
     * ahí la imagen puede venir del cuadro anterior.
     */
    private int pending;
    private long flashAt;
    private Path last;
    private double dragged;
    private boolean pressingShutter;
    private long openedAt;

    public CameraScreen(int color) {
        super(Component.translatable("celular.app.camera"));
        this.color = color;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (before == null) {
            before = mc.options.getCameraType();
            //#if MC < 26.1
            hideBefore = mc.options.hideGui;
            //#elif MC >= 26.3
            //$$ hudBefore = mc.gui.hud.isHidden();
            //#endif
            openedAt = System.currentTimeMillis();
            List<Path> g = Photos.gallery();
            last = g.isEmpty() ? null : g.get(0);
        }
        //#if MC < 26.1
        mc.options.hideGui = true;
        //#elif MC >= 26.3
        //$$ hud(true);
        //#endif
        applyMode();
    }

    private void applyMode() {
        Minecraft.getInstance().options.setCameraType(selfie ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON);
    }

    @Override
    public void removed() {
        Minecraft mc = Minecraft.getInstance();
        if (before != null) {
            mc.options.setCameraType(before);
        }
        //#if MC < 26.1
        mc.options.hideGui = hideBefore;
        //#elif MC >= 26.3
        //$$ hud(hudBefore);
        //#endif
        super.removed();
    }

    //#if MC >= 26.3
    //$$ /** En 26.3 el HUD (barra de objetos, mira) se oculta en Hud.isHidden, que no tiene "set": se cambia por reflexión. */
    //$$ private static void hud(boolean hidden) {
    //$$     try {
    //$$         net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(net.minecraft.client.gui.Hud.class,
    //$$                 Minecraft.getInstance().gui.hud, hidden, "isHidden");
    //$$     } catch (Exception ex) {
    //$$         com.celular.Celular.LOGGER.debug("No se pudo ocultar el HUD", ex);
    //$$     }
    //$$ }
    //#endif

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ geometría

    private int side() {
        return (int) (Math.min(width, height) * 0.8);
    }

    private int vx() {
        return width / 2 - side() / 2;
    }

    private int vy() {
        return height / 2 - side() / 2 - 8;
    }

    private int shutterX() {
        return width / 2;
    }

    private int shutterY() {
        return Math.min(height - 22, vy() + side() + 20);
    }

    // ------------------------------------------------------------------ foto

    private void shoot() {
        if (pending > 0 || System.currentTimeMillis() - openedAt < 300) {
            return;
        }
        //#if MC >= 26.1
        //$$ pending = 2;
        //#else
        pending = 1;
        //#endif
    }

    private void capture() {
        int side = side(), vx = vx(), vy = vy(), w = width, h = height;
        Tex.screenshot(img -> {
            try (NativeImage n = img) {
                double k = n.getHeight() / (double) h;
                int ps = (int) (side * k), px = (int) (vx * (n.getWidth() / (double) w)), py = (int) (vy * k);
                BufferedImage bi = Tex.cropSquare(n, px, py, ps, Photos.SIZE);
                Path p = Photos.save(bi);
                if (p != null) {
                    last = p;
                }
            } catch (Exception ex) {
                com.celular.Celular.LOGGER.warn("No se pudo tomar la foto", ex);
            }
        });
        flashAt = System.currentTimeMillis();
        Prefs.load();
        if (Prefs.shutterSound) {
            ClientHooks.play(CelularReg.SHUTTER.get(), 1.0F, 0.8F);
        }
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        if (pending == 2) {
            pending = 1;
            return; // un cuadro limpio, sin el visor
        }
        if (pending == 1) {
            pending = 0;
            capture(); // antes de dibujar nada encima
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        long now = System.currentTimeMillis();
        int side = side(), vx = vx(), vy = vy();
        int shade = 0x99000000;
        // fuera del visor más oscuro
        g.fill(0, 0, width, vy, shade);
        g.fill(0, vy + side, width, height, shade);
        g.fill(0, vy, vx, vy + side, shade);
        g.fill(vx + side, vy, width, vy + side, shade);
        // cuadrícula de tercios
        Prefs.load();
        if (Prefs.grid) {
            for (int i = 1; i < 3; i++) {
                g.fill(vx + side * i / 3, vy, vx + side * i / 3 + 1, vy + side, 0x40FFFFFF);
                g.fill(vx, vy + side * i / 3, vx + side, vy + side * i / 3 + 1, 0x40FFFFFF);
            }
        }
        // esquinas
        int c = 0xFFFFFFFF, L = 14;
        g.fill(vx - 1, vy - 1, vx + L, vy + 1, c);
        g.fill(vx - 1, vy - 1, vx + 1, vy + L, c);
        g.fill(vx + side - L, vy - 1, vx + side + 1, vy + 1, c);
        g.fill(vx + side - 1, vy - 1, vx + side + 1, vy + L, c);
        g.fill(vx - 1, vy + side - 1, vx + L, vy + side + 1, c);
        g.fill(vx - 1, vy + side - L, vx + 1, vy + side + 1, c);
        g.fill(vx + side - L, vy + side - 1, vx + side + 1, vy + side + 1, c);
        g.fill(vx + side - 1, vy + side - L, vx + side + 1, vy + side + 1, c);
        // punto de grabación y modo arriba
        Component mode = Component.translatable(selfie ? "celular.camera.selfie" : "celular.camera.photo").withStyle(ChatFormatting.BOLD);
        int mw = font.width(mode) + 20;
        D.rr(g, width / 2 - mw / 2, vy - 22, width / 2 + mw / 2, vy - 8, 7, 0xB0000000);
        D.circle(g, width / 2 - mw / 2 + 8, vy - 15, 3, (now / 500) % 2 == 0 ? 0xFFFF3B30 : 0xFF7A1A16);
        g.drawString(font, mode, width / 2 - mw / 2 + 15, vy - 19, 0xFFFFD60A, false);
        // botón de disparo
        int sx = shutterX(), sy = shutterY();
        boolean hov = Math.hypot(mx - sx, my - sy) < 15;
        D.circle(g, sx, sy, 15, 0xFFFFFFFF);
        D.circle(g, sx, sy, 13, 0xFF202020);
        D.circle(g, sx, sy, pressingShutter ? 10 : hov ? 12 : 11, 0xFFFFFFFF);
        // girar cámara
        int rx = sx + 48;
        boolean hovR = Math.hypot(mx - rx, my - sy) < 12;
        D.circle(g, rx, sy, 12, hovR ? 0xC0505050 : 0xA0303030);
        D.icon(g, D.SWITCH, rx - 8, sy - 8, 16, 0xFFFFFFFF);
        // última foto (abre la galería)
        int gx = sx - 60;
        D.rr(g, gx - 1, sy - 13, gx + 25, sy + 13, 5, 0xFFFFFFFF);
        ResourceLocation t = last != null ? Photos.local(last) : null;
        if (t != null) {
            g.blit(t, gx, sy - 12, 24, 24, 0, 0, Photos.SIZE, Photos.SIZE, Photos.SIZE, Photos.SIZE);
        } else {
            D.rr(g, gx, sy - 12, gx + 24, sy + 12, 4, 0xFF303030);
            D.icon(g, D.GALLERY, gx + 4, sy - 8, 16, 0xFF909090);
        }
        // ayuda
        Component help = Component.translatable("celular.camera.help");
        g.drawString(font, help, width / 2 - font.width(help) / 2, Math.min(height - 10, sy + 20), 0xC0FFFFFF, false);
        // destello
        long fa = now - flashAt;
        if (fa < 350) {
            float a = 1 - fa / 350F;
            g.fill(0, 0, width, height, D.alpha(0xFFFFFFFF, a * 0.85F));
        }
    }

    // ------------------------------------------------------------------ entrada

    @Override
    protected boolean onClick(double x, double y, int b) {
        dragged = 0;
        int sx = shutterX(), sy = shutterY();
        if (b == 0 && Math.hypot(x - sx, y - sy) < 16) {
            pressingShutter = true;
            return true;
        }
        if (b == 0 && Math.hypot(x - (sx + 48), y - sy) < 13) {
            toggle();
            return true;
        }
        if (b == 0 && x >= sx - 61 && x <= sx - 35 && Math.abs(y - sy) <= 13) {
            ClientHooks.tap();
            ClientHooks.open(new PhoneScreen(color, "gallery"));
            return true;
        }
        if (b == 1) {
            shoot();
            return true;
        }
        return true;
    }

    @Override
    protected boolean onRelease(double x, double y, int b) {
        if (pressingShutter) {
            pressingShutter = false;
            if (Math.hypot(x - shutterX(), y - shutterY()) < 18) {
                shoot();
            }
        }
        return true;
    }

    @Override
    protected boolean onDrag(double x, double y, int b, double dx, double dy) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !pressingShutter) {
            dragged += Math.abs(dx) + Math.abs(dy);
            double k = selfie ? -1 : 1;
            mc.player.turn(dx * 4 * k, dy * 4);
        }
        return true;
    }

    private void toggle() {
        selfie = !selfie;
        applyMode();
        ClientHooks.tap();
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        if (key == 32 || key == 257 || key == 335) { // espacio, enter
            shoot();
            return true;
        }
        if (key == 258) { // tab
            toggle();
            return true;
        }
        if (key == 256) { // escape: de vuelta al celular
            ClientHooks.open(new PhoneScreen(color, null));
            return true;
        }
        return super.onKey(key, scan, mods);
    }
}
