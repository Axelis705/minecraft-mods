package com.navidad.client;

import com.axeliscore.fx.client.Hud;
import com.axeliscore.plat.client.G;
import com.navidad.Navidad;
import com.navidad.entidad.Krampus;
import com.navidad.krampus.KrampusMsg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/**
 * (Cliente) Lo que se siente cuando anda el Krampus:
 * <ul>
 *     <li><b>Niebla</b>: ligera cuando está cerca (fases 1 y 2), negra y cerrada en el apagón, y la que manda el servidor en la Noche del
 *     Krampus.</li>
 *     <li><b>Dentro del saco</b>: la pantalla se ve desde adentro del costal (con la luz que se cuela por el tejido), se sacude, y dice
 *     cuánto falta; machacar la tecla de saltar manda los forcejeos al servidor. Al que va adentro no se le dibuja (va escondido).</li>
 * </ul>
 */
public final class KrampusCliente {
    private static final ResourceLocation COSTAL = new ResourceLocation(Navidad.MODID, "textures/gui/saco_dentro.png");
    private static float nieblaNoche, niebla;
    private static boolean enSaco;
    private static int forcejeos, tiempoSaco, pendientes;

    private KrampusCliente() {
    }

    public static void init() {
        KrampusMsg.alNiebla = f -> nieblaNoche = f;
        MinecraftForge.EVENT_BUS.addListener(KrampusCliente::tick);
        MinecraftForge.EVENT_BUS.addListener(KrampusCliente::alNiebla);
        MinecraftForge.EVENT_BUS.addListener(KrampusCliente::alColorNiebla);
        MinecraftForge.EVENT_BUS.addListener(KrampusCliente::alDibujarJugador);
        Hud.add(KrampusCliente::hud);
    }

    /** Qué tan espesa está la niebla ahora (para la prueba con fotos). */
    public static float niebla() {
        return niebla;
    }

    public static boolean enSaco() {
        return enSaco;
    }

    static void tick(TickEvent.ClientTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.phase != TickEvent.Phase.END || mc.player == null || mc.level == null) {
            return;
        }
        float quiere = nieblaNoche;
        for (Krampus k : mc.level.getEntitiesOfClass(Krampus.class, mc.player.getBoundingBox().inflate(48))) {
            if (k.isAlive()) {
                quiere = Math.max(quiere, k.fase() >= 2 ? 0.92F : 0.32F);
            }
        }
        niebla += (quiere - niebla) * 0.04F;
        if (Math.abs(quiere - niebla) < 0.002F) {
            niebla = quiere;
        }
        boolean ahora = mc.player.getVehicle() instanceof Krampus;
        if (ahora && !enSaco) {
            forcejeos = 0;
            pendientes = 0;
            tiempoSaco = 0;
            while (mc.options.keyJump.consumeClick()) {
                // lo que se apretó antes no cuenta
            }
        }
        enSaco = ahora;
        if (enSaco) {
            tiempoSaco++;
            while (mc.options.keyJump.consumeClick()) {
                pendientes++;
            }
            // el servidor cuenta hasta 4 por mensaje: lo que sobre se manda en los siguientes ticks
            int n = Math.min(3, pendientes);
            if (n > 0) {
                pendientes -= n;
                forcejeos += n;
                Navidad.NET.toServer(new KrampusMsg.Forcejeo(n));
            }
        }
    }

    static void alNiebla(ViewportEvent.RenderFog e) {
        if (niebla < 0.01F) {
            return;
        }
        float k = niebla;
        if (e.getMode() == FogRenderer.FogMode.FOG_SKY) {
            e.setFarPlaneDistance(Mth.lerp(k, e.getFarPlaneDistance(), 4F));
            e.setNearPlaneDistance(0F);
        } else {
            float lejos = Mth.lerp(k, e.getFarPlaneDistance(), 9F);
            e.setFarPlaneDistance(lejos);
            e.setNearPlaneDistance(Mth.lerp(k, e.getNearPlaneDistance(), -4F));
        }
        e.setCanceled(true);
    }

    static void alColorNiebla(ViewportEvent.ComputeFogColor e) {
        if (niebla < 0.01F) {
            return;
        }
        float k = niebla * 0.9F;
        e.setRed(Mth.lerp(k, e.getRed(), 0.035F));
        e.setGreen(Mth.lerp(k, e.getGreen(), 0.03F));
        e.setBlue(Mth.lerp(k, e.getBlue(), 0.06F));
    }

    static void alDibujarJugador(RenderPlayerEvent.Pre e) {
        if (e.getEntity().getVehicle() instanceof Krampus) {
            e.setCanceled(true);
        }
    }

    /** La vista desde adentro del saco. */
    static void hud(G gg, int w, int h) {
        if (!enSaco) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = gg.raw;
        float t = tiempoSaco + mc.getFrameTime();
        int dx = Math.round(Mth.sin(t * 0.9F) * 3 + Mth.sin(t * 2.3F) * 2), dy = Math.round(Mth.cos(t * 1.1F) * 2);
        // el costal por dentro: estirado a toda la pantalla (un poco más grande para que el temblor no deje orillas)
        g.pose().pushPose();
        g.pose().translate(dx, dy, 0);
        g.blit(COSTAL, -8, -8, w + 16, h + 16, 0, 0, 256, 256, 256, 256);
        g.pose().popPose();
        // orillas más oscuras
        g.fillGradient(0, 0, w, h / 4, 0xC0000000, 0x00000000);
        g.fillGradient(0, h - h / 4, w, h, 0x00000000, 0xC0000000);
        // lo que dice
        int cx = w / 2, cy = h / 2;
        g.drawCenteredString(mc.font, Component.translatable("navidad.krampus.saco.hud"), cx, cy - 26, 0xFFFFD9A0);
        g.drawCenteredString(mc.font, Component.translatable("navidad.krampus.saco.hud2", mc.options.keyJump.getTranslatedKeyMessage()),
                cx, cy - 12, 0xFFFFFFFF);
        int ancho = 120, x0 = cx - ancho / 2, y0 = cy + 6;
        float k = Mth.clamp(forcejeos / 12F, 0F, 1F);
        g.fill(x0 - 1, y0 - 1, x0 + ancho + 1, y0 + 7, 0xFF1A0E06);
        g.fill(x0, y0, x0 + Math.round(ancho * k), y0 + 6, 0xFFE0A040);
        int seg = Math.max(0, (180 - tiempoSaco) / 20);
        g.drawCenteredString(mc.font, Component.translatable("navidad.krampus.saco.hud3", seg), cx, y0 + 12, 0xFFC0A080);
    }
}
