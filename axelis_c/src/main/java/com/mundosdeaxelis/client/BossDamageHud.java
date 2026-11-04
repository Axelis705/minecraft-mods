package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.network.BossDamagePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel de daño al jefe: una barra por jugador con su porcentaje del daño total. Se actualiza en vivo durante la pelea y,
 * al vencer al jefe, queda unos segundos con el reparto final y la probabilidad de extras de cada jugador.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BossDamageHud {
    private BossDamageHud() {
    }

    private static String boss = "";
    private static boolean end;
    private static List<BossDamagePacket.Entry> entries = new ArrayList<>();
    private static long ticks;
    /** Hasta qué tick se muestra (en vivo: se renueva con cada paquete; final: 20 s). */
    private static long until;

    public static void set(BossDamagePacket p) {
        boss = p.boss;
        end = p.end;
        entries = new ArrayList<>(p.entries);
        until = ticks + (p.end ? 20 * 20 : 20 * 8);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && !Minecraft.getInstance().isPaused()) {
            ticks++;
        }
    }

    private static final int[] COLORS = {0xFFFFD27A, 0xFF7FE8FF, 0xFFFF9AC8, 0xFFA8FF9A};

    public static void render(GuiGraphics g, float pt, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || entries.isEmpty() || ticks > until) {
            return;
        }
        Font f = mc.font;
        String me = mc.player.getName().getString();
        int rows = Math.min(entries.size(), 6);
        int w = 132, h = 16 + rows * 12 + (end ? 11 : 0);
        int x = 6, y = 38;
        long left = until - ticks;
        float fade = left < 20 ? Math.max(0.0F, left / 20.0F) : 1.0F;
        int a = (int) (0xB0 * fade);
        if (a < 8) {
            return;
        }
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, (int) (0xCC * fade) << 24 | 0x6A4A1E);
        g.fill(x, y, x + w, y + h, a << 24 | 0x100A1C);
        Component title = Component.translatable(end ? "hud.mundos_de_axelis.boss_damage.end" : "hud.mundos_de_axelis.boss_damage", boss);
        g.drawString(f, title, x + 4, y + 3, (int) (0xFF * fade) << 24 | 0xFFD27A, true);
        for (int i = 0; i < rows; i++) {
            BossDamagePacket.Entry en = entries.get(i);
            int ry = y + 15 + i * 12;
            int bx = x + 4, bw = w - 8;
            g.fill(bx, ry, bx + bw, ry + 10, (int) (0x90 * fade) << 24 | 0x000000);
            int col = COLORS[i % COLORS.length];
            int fw = Math.max(1, bw * Math.min(100, en.pct()) / 100);
            g.fill(bx, ry, bx + fw, ry + 10, (int) (0x70 * fade) << 24 | (col & 0xFFFFFF));
            boolean mine = en.name().equals(me);
            String nm = en.name().length() > 11 ? en.name().substring(0, 10) + "." : en.name();
            int tc = (int) (0xFF * fade) << 24 | (mine ? 0xFFFFFF : 0xE0D8F0);
            g.drawString(f, nm, bx + 3, ry + 1, tc, true);
            String pc = en.pct() + " %";
            g.drawString(f, pc, bx + bw - 3 - f.width(pc), ry + 1, tc, true);
        }
        if (end) {
            for (BossDamagePacket.Entry en : entries) {
                if (en.name().equals(me)) {
                    Component c = Component.translatable("hud.mundos_de_axelis.boss_damage.chance", Math.max(0, en.chance()));
                    g.drawString(f, c, x + 4, y + h - 11, (int) (0xFF * fade) << 24 | 0x8CFFA8, true);
                    break;
                }
            }
        }
    }
}
