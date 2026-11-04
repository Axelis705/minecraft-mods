package com.mundosdeaxelis.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.math.Axis;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.GravePacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Contador de la lápida: una flecha que señala hacia ella, la distancia y el tiempo que le queda. Se puede ocultar con una tecla
 * (H por defecto) y vuelve a salir solo si te aparece otra lápida.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GraveHud {
    private GraveHud() {
    }

    public static final KeyMapping HIDE = new KeyMapping("key." + MundosDeAxelis.MODID + ".hide_grave",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories." + MundosDeAxelis.MODID);

    private record G(ResourceLocation dim, BlockPos pos, long endTick) {
    }

    private static final List<G> graves = new ArrayList<>();
    private static long ticks;
    private static boolean hidden;

    public static void set(GravePacket p) {
        boolean fresh = false;
        List<G> old = new ArrayList<>(graves);
        graves.clear();
        for (GravePacket.Item i : p.graves) {
            G g = new G(i.dim(), i.pos(), ticks + i.ticksLeft());
            graves.add(g);
            boolean known = false;
            for (G o : old) {
                if (o.dim.equals(g.dim) && o.pos.equals(g.pos)) {
                    known = true;
                    break;
                }
            }
            fresh |= !known;
        }
        if (fresh) {
            hidden = false;
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isPaused()) {
            ticks++;
        }
        while (HIDE.consumeClick()) {
            if (!graves.isEmpty()) {
                hidden = !hidden;
            }
        }
        graves.removeIf(g -> g.endTick <= ticks);
    }

    public static void render(GuiGraphics g, float pt, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.level == null || mc.options.hideGui || hidden || graves.isEmpty()) {
            return;
        }
        G best = graves.get(0);
        for (G o : graves) {
            if (o.endTick > best.endTick) {
                best = o;
            }
        }
        Font f = mc.font;
        int left = (int) Math.max(0, best.endTick - ticks);
        int secs = left / 20;
        boolean low = secs < 120;
        boolean blink = secs < 30 && (ticks / 6) % 2 == 0;
        boolean here = mc.level.dimension().location().equals(best.dim);

        int w = 118, h = 36;
        int x = width / 2 + 100, y = 6;
        if (x + w > width - 4) {
            x = width - w - 4;
        }
        int edge = low ? 0xFFFF6B6B : 0xFF7FE8FF;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xCC000000 | (edge & 0xFFFFFF) >> 1 & 0x7F7F7F);
        g.fill(x, y, x + w, y + h, 0xB0100A1C);
        g.fill(x, y, x + w, y + 1, edge);
        g.fill(x, y + h - 1, x + w, y + h, 0x60000000 | (edge & 0xFFFFFF));

        // flecha (o aviso si está en otra dimensión)
        int cx = x + 20, cy = y + 18;
        if (here) {
            double dx = best.pos.getX() + 0.5 - p.getX();
            double dz = best.pos.getZ() + 0.5 - p.getZ();
            float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float rel = Mth.wrapDegrees(target - Mth.lerp(pt, p.yRotO, p.getYRot()));
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(rel));
            int col = blink ? 0xFFFFFFFF : 0xFF000000 | (low ? 0xFF9A9A : 0x9FF0FF);
            for (int i = 0; i < 8; i++) {
                g.fill(-i, -10 + i, i + 1, -9 + i, col);
            }
            g.fill(-2, -2, 3, 9, col);
            g.pose().popPose();
        } else {
            g.drawCenteredString(f, "?", cx, cy - 4, 0xFFFFD27A);
        }

        // textos
        int tx = x + 40;
        g.drawString(f, Component.translatable("hud.mundos_de_axelis.grave"), tx, y + 4, 0xFFFFD27A, true);
        String time = (secs / 60) + ":" + (secs % 60 < 10 ? "0" : "") + (secs % 60);
        g.drawString(f, time, tx, y + 14, blink ? 0xFFFFFFFF : low ? 0xFFFF8C8C : 0xFFFFFFFF, true);
        if (here) {
            int d = (int) Math.sqrt(best.pos.distToCenterSqr(p.getX(), p.getY(), p.getZ()));
            g.drawString(f, Component.translatable("hud.mundos_de_axelis.grave.distance", d), tx, y + 24, 0xFFB8E8FF, true);
        } else {
            g.drawString(f, Component.translatable("hud.mundos_de_axelis.grave.other_dim"), tx, y + 24, 0xFFFFD27A, true);
        }
        if (graves.size() > 1) {
            String more = "+" + (graves.size() - 1);
            g.drawString(f, more, x + w - 4 - f.width(more), y + 4, 0xFFB8AAD8, true);
        }
        Component hint = Component.translatable("hud.mundos_de_axelis.grave.hide", HIDE.getTranslatedKeyMessage());
        g.drawString(f, hint, x + w - 3 - f.width(hint), y + h + 3, 0x99B8AAD8, true);
    }
}
