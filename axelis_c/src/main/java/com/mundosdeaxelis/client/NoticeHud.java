package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel de avisos arriba en el centro: tarjetas pequeñas que entran deslizándose, se apilan y se desvanecen solas.
 * Reemplaza al texto de la barra de acción (se intercepta aquí) y a los carteles gigantes de las salas.
 * Un aviso igual a otro que ya está en pantalla (ignorando los números) solo se refresca, así que contadores como
 * «Encendiendo la linterna... 40 %» se actualizan en la misma tarjeta en vez de apilarse.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NoticeHud {
    private NoticeHud() {
    }

    private static final int MAX = 5;
    private static final int IN = 8;
    private static final int OUT = 10;
    private static final int MAX_TEXT = 162;
    private static final int GAP = 4;

    private static final class Card {
        String key;
        List<FormattedCharSequence> title = new ArrayList<>();
        List<FormattedCharSequence> sub = new ArrayList<>();
        int rgb;
        int w, h;
        long born;
        long until;
        float y = -1;
        boolean fresh = true;
    }

    /** Borde de abajo de la barra de jefe (la anota BossBars) y última vez que se dibujó. */
    static int bossBottom;
    static long bossSeen;

    static void noteBossBar(int bottom) {
        long now = System.currentTimeMillis();
        if (now - bossSeen > 60) {
            bossBottom = 0;
        }
        bossSeen = now;
        bossBottom = Math.max(bossBottom, bottom);
    }

    private static final List<Card> CARDS = new ArrayList<>();
    private static long ticks;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && !Minecraft.getInstance().isPaused()) {
            ticks++;
            CARDS.removeIf(c -> ticks > c.until + OUT);
        }
    }

    /**
     * De la barra de acción solo se recoge lo que obtienes («+1 Baya del Santuario», «+15 Esencia»), que va a {@link LootHud}
     * (abajo a la derecha). Todo lo demás (contadores, avisos de error...) se queda en la barra de acción de siempre; este panel
     * de arriba es solo para los avisos que manda el servidor a propósito (nombres de sala, pistas, pruebas).
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSystem(ClientChatReceivedEvent.System e) {
        if (!e.isOverlay()) {
            return;
        }
        Component msg = e.getMessage();
        String s = msg.getString();
        if (LootHud.isGain(s.replaceAll("§.", ""))) {
            e.setCanceled(true);
            LootHud.push(s.replaceAll("§.", ""));
        }
    }

    public static void push(String title, String sub, int rgb, int lifeTicks) {
        boolean bold = (sub != null && !sub.isEmpty()) || title.length() <= 32;
        push(bold ? Component.literal(title).withStyle(ChatFormatting.BOLD) : Component.literal(title), sub == null || sub.isEmpty() ? null : Component.literal(sub), rgb, lifeTicks);
    }

    public static void push(Component title, Component sub, int rgb, int lifeTicks) {
        Minecraft mc = Minecraft.getInstance();
        Font f = mc.font;
        String key = (title.getString() + "|" + (sub == null ? "" : sub.getString())).replaceAll("[0-9]+", "#");
        for (Card c : CARDS) {
            if (c.key.equals(key)) {
                fill(c, f, title, sub, rgb);
                c.until = ticks + lifeTicks;
                c.fresh = false;
                return;
            }
        }
        Card c = new Card();
        c.key = key;
        c.born = ticks;
        c.until = ticks + lifeTicks;
        fill(c, f, title, sub, rgb);
        CARDS.add(c);
        while (CARDS.size() > MAX) {
            Card old = CARDS.get(0);
            if (old.until > ticks) {
                old.until = ticks;
            }
            if (CARDS.size() > MAX + 2) {
                CARDS.remove(0);
            } else {
                break;
            }
        }
        if (mc.level != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.get(), 1.6F, 0.35F));
        }
    }

    private static void fill(Card c, Font f, Component title, Component sub, int rgb) {
        c.title = f.split(title, MAX_TEXT);
        c.sub = sub == null ? new ArrayList<>() : f.split(sub.copy().withStyle(ChatFormatting.GRAY), MAX_TEXT);
        c.rgb = rgb & 0xFFFFFF;
        int widest = 0;
        for (FormattedCharSequence l : c.title) {
            widest = Math.max(widest, f.width(l));
        }
        for (FormattedCharSequence l : c.sub) {
            widest = Math.max(widest, f.width(l));
        }
        c.w = Math.max(88, widest + 18);
        c.h = 8 + c.title.size() * 10 + c.sub.size() * 9 + (c.sub.isEmpty() ? 0 : 1);
    }

    private static int a(float alpha, int rgb) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24 | (rgb & 0xFFFFFF);
    }

    public static void render(GuiGraphics g, float pt, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (CARDS.isEmpty() || mc.player == null || mc.level == null || mc.options.hideGui || CinematicClient.isActive()) {
            return;
        }
        Font f = mc.font;
        // arriba en el centro; si hay barra de jefe, justo debajo de ella
        int top = 6;
        if (System.currentTimeMillis() - bossSeen < 250) {
            top = Math.max(top, bossBottom + 4);
        }
        float now = ticks + pt;
        float target = top;
        for (Card c : CARDS) {
            float age = now - c.born;
            float in = Mth.clamp(age / IN, 0.0F, 1.0F);
            float slide = 1.0F - (1.0F - in) * (1.0F - in) * (1.0F - in);
            float out = now > c.until ? Mth.clamp((now - c.until) / OUT, 0.0F, 1.0F) : 0.0F;
            float alpha = (1.0F - out) * Math.min(1.0F, 0.25F + in);
            if (c.y < 0) {
                c.y = target;
            }
            c.y += (target - c.y) * 0.3F;
            int x = (width - c.w) / 2;
            // entra bajando desde arriba y se va subiendo un poco al desvanecerse
            int y = Math.round(c.y) - (int) ((1.0F - slide) * 14) - (int) (out * 8);
            int acc = c.rgb;
            // fondo, brillo del color a la izquierda, barra de color y borde fino
            g.fill(x - 1, y - 1, x + c.w + 1, y + c.h + 1, a(alpha * 0.55F, 0x000000));
            g.fill(x, y, x + c.w, y + c.h, a(alpha * 0.82F, 0x120C1E));
            g.fillGradient(x, y, x + c.w * 2 / 5, y + c.h, a(alpha * 0.20F, acc), a(0.0F, acc));
            g.fill(x, y, x + 2, y + c.h, a(alpha, acc));
            g.fill(x + 2, y, x + c.w, y + 1, a(alpha * 0.25F, acc));
            // destello que cruza la tarjeta al llegar
            if (age < 14 && c.fresh) {
                int sx = x + (int) (age / 14.0F * c.w);
                g.fill(sx, y, Math.min(sx + 3, x + c.w), y + c.h, a(alpha * 0.30F * (1.0F - age / 14.0F), 0xFFFFFF));
            }
            // tiempo restante: línea fina abajo
            float left = Mth.clamp((c.until - now) / Math.max(1.0F, c.until - c.born), 0.0F, 1.0F);
            g.fill(x + 2, y + c.h - 1, x + 2 + (int) ((c.w - 2) * left), y + c.h, a(alpha * 0.55F, acc));
            int ty = y + 4;
            int tcol = a(Math.max(alpha, 0.03F), 0xFFFFFF);
            for (FormattedCharSequence l : c.title) {
                g.drawString(f, l, x + 8, ty, tcol, false);
                ty += 10;
            }
            if (!c.sub.isEmpty()) {
                ty += 1;
                for (FormattedCharSequence l : c.sub) {
                    g.drawString(f, l, x + 8, ty, a(Math.max(alpha, 0.03F), 0xB8C0D0), false);
                    ty += 9;
                }
            }
            target += c.h + GAP;
        }
    }
}
