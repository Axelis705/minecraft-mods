package com.celular.client;

import com.celular.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * Clima: el tiempo de Minecraft donde estás (sol, luna, nubes, lluvia o tormenta), la hora del mundo con un reloj que gira como el
 * de Minecraft, el día, la fase de la luna, el bioma y su "temperatura", y cuánto falta para que anochezca (salen los monstruos) o
 * amanezca. En el Nether y el End lo dice.
 */
final class WeatherApp extends App {
    WeatherApp(PhoneScreen s) {
        super(s, D.APP_WEATHER, "weather");
    }

    /** Lo que se muestra del clima (también en el widget del inicio). */
    static final class Info {
        int icon;
        int iconColor;
        String time;
        Component line;
        Component state;
        int temp;
        long day;
        int moon;
        boolean night;
        boolean overworld;
        Component biome;
        int minutesToChange;
        int sky0;
        int sky1;
    }

    static Info info() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel lv = mc.level;
        if (lv == null || mc.player == null) {
            return null;
        }
        Info i = new Info();
        long dt = dayTime(lv);
        long t = Math.floorMod(dt, 24000L);
        i.day = dt / 24000L + 1;
        i.moon = (int) Math.floorMod(dt / 24000L, 8L);
        int h = (int) ((t / 1000 + 6) % 24), m = (int) (t % 1000 * 60 / 1000);
        i.time = String.format(java.util.Locale.ROOT, "%d:%02d", h, m);
        i.night = t >= 12542 && t < 23460;
        i.overworld = lv.dimension() == Level.OVERWORLD;
        BlockPos pos = mc.player.blockPosition();
        var holder = lv.getBiome(pos);
        float baseTemp = holder.value().getBaseTemperature();
        i.temp = Math.round(baseTemp * 25F);
        i.biome = biomeName(holder.unwrapKey().map(Object::toString).orElse(""));
        boolean cold = baseTemp < 0.15F;
        boolean dry = baseTemp > 1.5F;
        if (!i.overworld) {
            i.icon = lv.dimension() == Level.NETHER ? D.SUN : D.STAR;
            i.iconColor = lv.dimension() == Level.NETHER ? 0xFFFF6A3D : 0xFFE6D7FF;
            i.state = Component.translatable(lv.dimension() == Level.NETHER ? "celular.weather.nether" : "celular.weather.end");
            i.sky0 = lv.dimension() == Level.NETHER ? 0xFF5A1209 : 0xFF140B26;
            i.sky1 = lv.dimension() == Level.NETHER ? 0xFF1E0603 : 0xFF05030C;
        } else if (lv.isThundering() && !dry) {
            i.icon = D.STORM;
            i.iconColor = 0xFFFFE066;
            i.state = Component.translatable("celular.weather.storm");
            i.sky0 = 0xFF2B3240;
            i.sky1 = 0xFF0F1218;
        } else if (lv.isRaining() && !dry) {
            i.icon = cold ? D.CLOUD : D.RAIN;
            i.iconColor = 0xFFB9D7FF;
            i.state = Component.translatable(cold ? "celular.weather.snow" : "celular.weather.rain");
            i.sky0 = 0xFF4A5A70;
            i.sky1 = 0xFF1F2733;
        } else if (i.night) {
            i.icon = D.MOON;
            i.iconColor = 0xFFF2F0D8;
            i.state = Component.translatable("celular.weather.clear_night");
            i.sky0 = 0xFF0B1A3A;
            i.sky1 = 0xFF03060F;
        } else {
            i.icon = D.SUN;
            i.iconColor = 0xFFFFD23F;
            i.state = Component.translatable("celular.weather.sunny");
            i.sky0 = 0xFF4FB3F6;
            i.sky1 = 0xFF1D6FD6;
        }
        // minutos (reales) hasta que cambie: anochecer a 13000 (salen los monstruos) o amanecer a 23460
        long target = i.night ? (t < 23460 ? 23460 : 23460 + 24000) : (t < 12542 ? 12542 : 12542 + 24000);
        i.minutesToChange = (int) Math.ceil((target - t) / 1200.0);
        i.line = i.overworld ? Component.translatable("celular.weather.widget", i.state, i.day) : i.state;
        return i;
    }

    /** Hora del mundo en ticks (en 26.x el día lo lleva el "reloj" de la dimensión). */
    static long dayTime(ClientLevel lv) {
        //#if MC >= 26.1
        //$$ return lv.getDefaultClockTime();
        //#else
        return lv.getDayTime();
        //#endif
    }

    /** "ResourceKey[minecraft:worldgen/biome / minecraft:dark_forest]" → nombre traducido del bioma. */
    private static Component biomeName(String key) {
        int a = key.lastIndexOf(" / "), b = key.lastIndexOf(']');
        if (a < 0 || b < a) {
            return Component.literal("?");
        }
        String id = key.substring(a + 3, b);
        int c = id.indexOf(':');
        return Component.translatable("biome." + (c < 0 ? "minecraft." + id : id.substring(0, c) + "." + id.substring(c + 1)));
    }

    @Override
    int bg() {
        Info i = info();
        return i == null ? 0xFF1D6FD6 : i.sky0;
    }

    @Override
    boolean lightStatus() {
        return false;
    }

    @Override
    void draw(G g, int mx, int my, float pt) {
        Font f = font();
        Info i = info();
        if (i == null) {
            g.fill(s.dx0, s.dy0, s.dx1, s.dy1, 0xFF1D6FD6);
            return;
        }
        g.fillGradient(s.dx0, s.dy0, s.dx1, s.dy1, i.sky0, i.sky1);
        long now = System.currentTimeMillis();
        int cx = (s.dx0 + s.dx1) / 2;
        // lluvia que cae
        if (i.icon == D.RAIN || i.icon == D.STORM) {
            for (int k = 0; k < 40; k++) {
                double ph = (now / 600.0 + k * 0.173) % 1.0;
                int x = s.dx0 + (int) ((Math.sin(k * 91.7) * 0.5 + 0.5) * s.dw());
                int y = s.dy0 + (int) (ph * (s.dy1 - s.dy0));
                g.fill(x, y, x + 1, y + 4, 0x60C8DCFF);
            }
            if (i.icon == D.STORM && now % 4000 < 90) {
                g.fill(s.dx0, s.dy0, s.dx1, s.dy1, 0x50FFFFFF);       // relámpago
            }
        } else if (i.icon == D.CLOUD) {
            for (int k = 0; k < 30; k++) {
                double ph = (now / 3000.0 + k * 0.173) % 1.0;
                int x = s.dx0 + (int) (((Math.sin(k * 91.7) * 0.5 + 0.5) * s.dw() + Math.sin(now / 700.0 + k) * 4));
                int y = s.dy0 + (int) (ph * (s.dy1 - s.dy0));
                g.fill(x, y, x + 2, y + 2, 0xB0FFFFFF);
            }
        } else if (i.night) {
            for (int k = 0; k < 30; k++) {
                int x = s.dx0 + (int) ((Math.sin(k * 12.9898) * 0.5 + 0.5) * s.dw());
                int y = s.dy0 + 14 + (int) ((Math.sin(k * 78.233) * 0.5 + 0.5) * (s.dy1 - s.dy0) * 0.6);
                float tw = (float) (0.4 + 0.6 * Math.abs(Math.sin(now / 900.0 + k)));
                g.fill(x, y, x + 1, y + 1, D.alpha(0xFFFFFFFF, tw));
            }
        }
        int y = s.cy0 + 8;
        String place = D.cut(f, i.biome.getString(), s.dw() - 12);
        g.drawString(f, place, cx - f.width(place) / 2, y, 0xFFFFFFFF, true);
        y += 12;
        // temperatura grande
        String temp = i.overworld ? i.temp + "°" : "—";
        g.push();
        g.translate(cx, y, 0);
        g.scale(3.2F, 3.2F, 1);
        g.drawString(f, temp, -f.width(temp) / 2, 0, 0xFFFFFFFF, true);
        g.pop();
        y += 32;
        Component st = i.state.copy().withStyle(ChatFormatting.BOLD);
        g.drawString(f, st, cx - f.width(st) / 2, y, 0xFFFFFFFF, true);
        y += 16;
        // reloj de Minecraft: un disco mitad día / mitad noche que gira
        int r = Math.min(26, s.dw() / 5);
        int ccx = s.dx0 + 10 + r, ccy = y + r;
        drawDial(g, ccx, ccy, r, i);
        int tx = ccx + r + 10;
        g.drawString(f, Component.translatable("celular.weather.time"), tx, y + 4, 0xC0FFFFFF, false);
        g.push();
        g.translate(tx, y + 15, 0);
        g.scale(2F, 2F, 1);
        g.drawString(f, i.time, 0, 0, 0xFFFFFFFF, true);
        g.pop();
        g.drawString(f, Component.translatable("celular.weather.day", i.day), tx, y + 36, 0xC0FFFFFF, false);
        y += 2 * r + 12;
        // tarjetas: cuánto falta y la luna
        int x0 = s.dx0 + 6, x1 = s.dx1 - 6;
        if (i.overworld) {
            D.rr(g, x0, y, x1, y + 22, 7, 0x40000000);
            D.icon(g, i.night ? D.SUN : D.SKULL, x0 + 5, y + 5, 12, 0xFFFFFFFF);
            Component when = Component.translatable(i.night ? "celular.weather.dawn_in" : "celular.weather.mobs_in", i.minutesToChange);
            small(g, D.cut(f, when.getString(), (int) ((x1 - x0 - 24) / 0.75F)), x0 + 21, y + 8, 0xFFFFFFFF, 0.75F);
            y += 26;
            D.rr(g, x0, y, x1, y + 22, 7, 0x40000000);
            drawMoon(g, x0 + 11, y + 11, 6, i.moon);
            small(g, Component.translatable("celular.weather.moon." + i.moon).getString(), x0 + 21, y + 8, 0xFFFFFFFF, 0.75F);
            y += 26;
        }
        // hora real
        java.time.LocalTime real = java.time.LocalTime.now();
        Component rl = Component.translatable("celular.weather.real", String.format(java.util.Locale.ROOT, "%d:%02d", real.getHour(), real.getMinute()));
        if (y + 10 < s.cy1) {
            small(g, rl.getString(), cx - (int) (f.width(rl) * 0.7F / 2), s.cy1 - 10, 0xA0FFFFFF, 0.7F);
        }
    }

    private void drawDial(G g, int cx, int cy, int r, Info i) {
        D.circle(g, cx, cy, r + 2, 0x60000000);
        D.circle(g, cx, cy, r, 0xFF1A1F3D);
        // mitad de día: un semicírculo que gira según la hora (arriba al mediodía)
        long t = Math.floorMod(dayTime(Minecraft.getInstance().level), 24000L);
        double ang = (t - 6000) / 24000.0 * 2 * Math.PI;   // 0 = mediodía arriba
        for (int yy = -r; yy <= r; yy++) {
            for (int xx = -r; xx <= r; xx++) {
                if (xx * xx + yy * yy > r * r) {
                    continue;
                }
                // ¿del lado del día? (perpendicular a la dirección del sol)
                double sx = Math.sin(ang), sy = -Math.cos(ang);
                if (xx * sx + yy * sy > 0) {
                    g.fill(cx + xx, cy + yy, cx + xx + 1, cy + yy + 1, 0xFF3FA7F0);
                }
            }
        }
        int sx = cx + (int) Math.round(Math.sin(ang) * r * 0.6), sy = cy - (int) Math.round(Math.cos(ang) * r * 0.6);
        D.circle(g, sx, sy, Math.max(3, r / 5), 0xFFFFD23F);
        int mx = cx - (int) Math.round(Math.sin(ang) * r * 0.6), my = cy + (int) Math.round(Math.cos(ang) * r * 0.6);
        D.circle(g, mx, my, Math.max(2, r / 6), 0xFFE8E8E8);
        // marca de "ahora" arriba
        g.fill(cx - 1, cy - r - 3, cx + 1, cy - r + 3, 0xFFFFFFFF);
    }

    /** Fase de la luna (0 llena ... 4 nueva ... 7). */
    private void drawMoon(G g, int cx, int cy, int r, int phase) {
        D.circle(g, cx, cy, r, 0xFF2A2F45);
        double lit = Math.cos(phase * Math.PI / 4);     // 1 llena, -1 nueva
        boolean waxing = phase > 4;
        for (int yy = -r; yy <= r; yy++) {
            int w = (int) Math.round(Math.sqrt(Math.max(0, r * r - yy * yy)));
            for (int xx = -w; xx <= w; xx++) {
                double edge = w * lit;
                boolean on = waxing ? xx >= -edge : xx <= edge;
                if (phase == 0 || on && phase != 4) {
                    g.fill(cx + xx, cy + yy, cx + xx + 1, cy + yy + 1, 0xFFF2F0D8);
                }
            }
        }
    }

    private void small(G g, String text, int x, int y, int color, float sc) {
        g.push();
        g.translate(x, y, 0);
        g.scale(sc, sc, 1);
        g.drawString(font(), text, 0, 0, color, false);
        g.pop();
    }
}
