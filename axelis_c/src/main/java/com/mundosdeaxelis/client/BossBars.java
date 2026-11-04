package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Barras de jefe decoradas de los dungeons (bosque y forja). En otros mundos se deja la barra normal del juego.
 * Las texturas son de 256x256: marco en (0,0) de 202x26 y rellenos de 182x6 en v=32, 40 y 48.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BossBars {

    private static final ResourceLocation FOREST =
            new ResourceLocation(MundosDeAxelis.MODID, "textures/gui/boss_forest.png");

    private static final ResourceLocation FORGE =
            new ResourceLocation(MundosDeAxelis.MODID, "textures/gui/boss_forge.png");

    private BossBars() {
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        NoticeHud.noteBossBar(event.getY() + Math.max(event.getIncrement(), 18));
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        boolean forest = ModDimensions.isForest(mc.level.dimension());
        boolean forge = ModDimensions.isForge(mc.level.dimension());
        if (!forest && !forge) {
            return;
        }
        LerpingBossEvent boss = event.getBossEvent();
        if (forge) {
            drawForge(event, boss, mc);
            return;
        }
        if (forest) {
            drawForest(event, boss, mc);
            return;
        }
    }

    /**
     * Barra del bosque: marco de piedra con musgo, una gelatina a la izquierda y una semilla que brilla a la derecha (Rey Brote),
     * o de corteza dorada (Guardián de Musgo, marco a partir de v=96). El relleno cambia con la fase: verde, ámbar y rojo.
     */
    private static void drawForest(CustomizeGuiOverlayEvent.BossEventProgress event, LerpingBossEvent boss, Minecraft mc) {
        GuiGraphics g = event.getGuiGraphics();
        boolean guardian = boss.getName().getString().contains("Guardi");
        int x = event.getX();
        int y = event.getY();
        float progress = Math.max(0.0F, Math.min(1.0F, boss.getProgress()));
        int fv = guardian ? 96 : 0;
        g.blit(FOREST, x - 10, y - 8, 0, fv, 202, 26);
        BossEvent.BossBarColor color = boss.getColor();
        int v;
        if (guardian) {
            v = color == BossEvent.BossBarColor.RED ? 136 : 128;
        } else {
            v = color == BossEvent.BossBarColor.RED ? 48 : color == BossEvent.BossBarColor.YELLOW ? 40 : 32;
        }
        int w = (int) (182 * progress);
        if (w > 0) {
            g.blit(FOREST, x, y + 2, 0, v, w, 6);
            long t = mc.level.getGameTime();
            int sx = (int) ((t * 2) % (w + 50)) - 20;
            int x0 = Math.max(0, sx);
            int x1 = Math.min(w, sx + 10);
            if (x1 > x0) {
                g.fill(x + x0, y + 2, x + x1, y + 4, 0x38FFFFFF);
            }
        }
        if (!guardian) {
            // marcas de las fases del Rey Brote (65 % y 30 % de vida)
            g.fill(x + 118, y + 2, x + 119, y + 8, 0xFF0B1A0C);
            g.fill(x + 55, y + 2, x + 56, y + 8, 0xFF0B1A0C);
        }
        g.drawCenteredString(mc.font, boss.getName(), x + 91, y - 6, guardian ? 0xFFEFC0 : 0xE6FFD8);
        event.setIncrement(30);
        event.setCanceled(true);
    }

    /**
     * Barra de la forja: marco de piedra negra con brasas, martillo a la izquierda y yunque a la derecha (Gran Herrero), o de
     * placas de hierro (Capataz, marco a partir de v=96). El relleno cambia con la fase: naranja, blanco incandescente y rojo.
     */
    private static void drawForge(CustomizeGuiOverlayEvent.BossEventProgress event, LerpingBossEvent boss, Minecraft mc) {
        GuiGraphics g = event.getGuiGraphics();
        boolean foreman = boss.getName().getString().contains("Capataz");
        int x = event.getX();
        int y = event.getY();
        float progress = Math.max(0.0F, Math.min(1.0F, boss.getProgress()));
        g.blit(FORGE, x - 10, y - 8, 0, foreman ? 96 : 0, 202, 26);
        BossEvent.BossBarColor color = boss.getColor();
        int v;
        if (foreman) {
            v = color == BossEvent.BossBarColor.RED ? 136 : 128;
        } else {
            v = color == BossEvent.BossBarColor.RED ? 48 : color == BossEvent.BossBarColor.PURPLE ? 40 : 32;
        }
        int w = (int) (182 * progress);
        if (w > 0) {
            g.blit(FORGE, x, y + 2, 0, v, w, 6);
            long t = mc.level.getGameTime();
            int sx = (int) ((t * 2) % (w + 50)) - 20;
            int x0 = Math.max(0, sx);
            int x1 = Math.min(w, sx + 10);
            if (x1 > x0) {
                g.fill(x + x0, y + 2, x + x1, y + 4, 0x40FFFFFF);
            }
        }
        if (!foreman) {
            // marcas de las fases del Gran Herrero (65 % y 30 % de vida)
            g.fill(x + 118, y + 2, x + 119, y + 8, 0xFF140A08);
            g.fill(x + 55, y + 2, x + 56, y + 8, 0xFF140A08);
        }
        g.drawCenteredString(mc.font, boss.getName(), x + 91, y - 6, foreman ? 0xFFE6C0 : 0xFFD9A0);
        event.setIncrement(30);
        event.setCanceled(true);
    }
}
