package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lo que obtienes (objetos, materiales, Esencia) aparece abajo a la derecha como fichas dorado oscuro con el icono del objeto.
 * Los mensajes del tipo «+3 Baya del Santuario» llegan por la barra de acción y se recogen en {@link NoticeHud}; aquí se
 * suman: si consigues lo mismo otra vez mientras la ficha sigue en pantalla, el número sube y la ficha da un pequeño pulso.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LootHud {
    private LootHud() {
    }

    private static final Pattern GAIN = Pattern.compile("^\\+(\\d+)\\s+(.+)$");
    private static final int MAX = 6;
    private static final int LIFE = 80;
    private static final int IN = 7;
    private static final int OUT = 10;
    private static final int H = 20;
    private static final int GOLD = 0xFFC857;

    private static final class Entry {
        String key;
        String name;
        int count;
        Item item;
        boolean essence;
        long born, until, pulse = -100;
        float y = -1;
        int w;
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static Map<String, Item> byName;
    private static long ticks;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && !Minecraft.getInstance().isPaused()) {
            ticks++;
            ENTRIES.removeIf(en -> ticks > en.until + OUT);
        }
    }

    /** ¿Es un texto de «lo que obtienes»? («+1 Baya del Santuario», «+15 Esencia») */
    public static boolean isGain(String text) {
        return GAIN.matcher(text.trim()).matches();
    }

    private static Item find(String name) {
        if (byName == null) {
            byName = new HashMap<>();
            for (Item it : ForgeRegistries.ITEMS.getValues()) {
                try {
                    byName.putIfAbsent(new ItemStack(it).getHoverName().getString(), it);
                } catch (RuntimeException ignored) {
                    // algún objeto raro sin nombre: se ignora
                }
            }
        }
        return byName.get(name);
    }

    public static void push(String text) {
        Matcher m = GAIN.matcher(text.trim());
        if (!m.matches()) {
            return;
        }
        int n = Integer.parseInt(m.group(1));
        String name = m.group(2).replaceAll("§.", "").trim();
        boolean ess = name.toLowerCase().startsWith("esencia");
        add(ess ? "essence" : "n:" + name, name, ess ? null : find(name), ess, n);
    }

    /** Botín con el objeto exacto (id vacío = Esencia). */
    public static void push(String itemId, int count) {
        if (itemId == null || itemId.isEmpty()) {
            add("essence", "Esencia", null, true, count);
            return;
        }
        Item it = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(itemId));
        if (it == null || it == net.minecraft.world.item.Items.AIR) {
            return;
        }
        add("i:" + itemId, new ItemStack(it).getHoverName().getString(), it, false, count);
    }

    private static void add(String key, String name, Item item, boolean essence, int n) {
        for (Entry en : ENTRIES) {
            if (en.key.equals(key) && ticks <= en.until) {
                en.count += n;
                en.until = ticks + LIFE;
                en.pulse = ticks;
                return;
            }
        }
        Entry en = new Entry();
        en.key = key;
        en.name = name;
        en.count = n;
        en.essence = essence;
        en.item = item;
        en.born = ticks;
        en.until = ticks + LIFE;
        ENTRIES.add(en);
        while (ENTRIES.size() > MAX) {
            ENTRIES.remove(0);
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, essence ? 1.5F : 1.2F, 0.5F));
        }
    }

    private static int a(float alpha, int rgb) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24 | (rgb & 0xFFFFFF);
    }

    public static void render(GuiGraphics g, float pt, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (ENTRIES.isEmpty() || mc.player == null || mc.level == null || mc.options.hideGui || CinematicClient.isActive()) {
            return;
        }
        Font f = mc.font;
        float now = ticks + pt;
        int right = width - 8;
        float target = height - 8 - H;
        // la ficha más nueva abajo; las anteriores suben
        for (int i = ENTRIES.size() - 1; i >= 0; i--) {
            Entry en = ENTRIES.get(i);
            String label = "+" + en.count + "  " + en.name;
            en.w = 26 + f.width(label) + 10;
            float age = now - en.born;
            float in = Mth.clamp(age / IN, 0.0F, 1.0F);
            float slide = 1.0F - (1.0F - in) * (1.0F - in);
            float out = now > en.until ? Mth.clamp((now - en.until) / OUT, 0.0F, 1.0F) : 0.0F;
            float alpha = (1.0F - out) * Math.min(1.0F, 0.2F + in);
            if (en.y < 0) {
                en.y = target + 6;
            }
            en.y += (target - en.y) * 0.3F;
            int x = right - en.w + (int) ((1.0F - slide) * (en.w + 10)) + (int) (out * 10);
            int y = Math.round(en.y);
            float pulse = Mth.clamp(1.0F - (now - en.pulse) / 8.0F, 0.0F, 1.0F);
            // ficha con esquinas cortadas, ranura del icono y borde dorado fino
            g.fill(x + 1, y, x + en.w - 1, y + H, a(alpha * 0.85F, 0x1A1208));
            g.fill(x, y + 1, x + en.w, y + H - 1, a(alpha * 0.85F, 0x1A1208));
            g.fill(x + 1, y, x + en.w - 1, y + 1, a(alpha * (0.8F + 0.2F * pulse), GOLD));
            g.fill(x + 1, y + H - 1, x + en.w - 1, y + H, a(alpha * 0.45F, GOLD));
            g.fill(x, y + 1, x + 1, y + H - 1, a(alpha * 0.6F, GOLD));
            g.fill(x + en.w - 1, y + 1, x + en.w, y + H - 1, a(alpha * 0.25F, GOLD));
            if (pulse > 0) {
                g.fill(x + 1, y + 1, x + en.w - 1, y + H - 1, a(alpha * 0.18F * pulse, 0xFFFFFF));
            }
            g.fill(x + 3, y + 3, x + 3 + 14, y + 3 + 14, a(alpha * 0.9F, 0x0C0804));
            if (en.item != null) {
                if (alpha > 0.5F) {
                    g.renderItem(new ItemStack(en.item), x + 3 - 1, y + 3 - 1);
                }
            } else {
                String glyph = en.essence ? "◆" : "✦";
                g.drawString(f, glyph, x + 3 + 7 - f.width(glyph) / 2, y + 6, a(Math.max(alpha, 0.03F), GOLD), false);
            }
            g.drawString(f, label, x + 24, y + 6, a(Math.max(alpha, 0.03F), 0xFFF2D0), false);
            target -= H + 3;
        }
    }
}
