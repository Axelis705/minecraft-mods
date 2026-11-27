package com.navidad.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.fx.client.Hud;
import com.axeliscore.plat.client.G;
import com.mojang.math.Axis;
import com.navidad.Navidad;
import com.navidad.entidad.TrineoSanta;
import com.navidad.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * (Cliente) El aviso de Santa en la pantalla. Cuando el trineo sale: baja una cinta roja con "¡Santa Claus se acerca!", el trineo
 * dibujado, qué tan lejos viene y una flecha dorada que señala hacia dónde mirar (gira contigo). Cuando pasa encima: un "¡JO, JO, JO!"
 * grande con las letras saltando una por una, "¡Feliz Navidad, (tu nombre)!" y confeti cayendo por toda la pantalla.
 */
public final class SantaAviso {
    private static final ResourceLocation TEX = new ResourceLocation(Navidad.MODID, "textures/gui/aviso_santa.png");
    private static final int[] COLORES = {0xFFE0303C, 0xFF3BD16F, 0xFFFFD23A, 0xFF3B8BFF, 0xFFFFFFFF, 0xFFFF6AD5};
    private static String fase = "";
    private static long desde, jojoDesde = -1;
    private static int id;
    private static double sx, sz;
    private static String nombre = "";
    private static final List<float[]> confeti = new ArrayList<>();
    private static final Random R = new Random();
    private static long ultimo;
    /** Para la prueba con fotos. */
    public static int vistos;

    private SantaAviso() {
    }

    public static void init() {
        ClientData.listen(TrineoSanta.AVISO, SantaAviso::recibir);
        Hud.add(SantaAviso::render);
    }

    static void recibir(CompoundTag t) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            String f = t.getString("fase");
            id = t.getInt("id");
            sx = t.getDouble("x");
            sz = t.getDouble("z");
            nombre = t.getString("nombre");
            vistos++;
            if (f.equals("viene")) {
                fase = "viene";
                desde = System.currentTimeMillis();
                jojoDesde = -1;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModRegistry.CASCABELES.get(), 1.0F, 0.7F));
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 0.9F));
            } else if (f.equals("jojo")) {
                jojoDesde = System.currentTimeMillis();
                if (!fase.equals("viene")) {
                    fase = "viene";
                    desde = jojoDesde - 1000;
                }
                confeti.clear();
                int w = mc.getWindow().getGuiScaledWidth();
                for (int i = 0; i < 90; i++) {
                    confeti.add(new float[]{R.nextFloat() * w, -10 - R.nextFloat() * 160, 20 + R.nextFloat() * 40, R.nextFloat() * 6.28F,
                            COLORES[R.nextInt(COLORES.length)], 2 + R.nextInt(2), R.nextFloat() * 6.28F});
                }
            }
        });
    }

    private static float easeOutBack(float k) {
        float c1 = 1.70158F, c3 = c1 + 1;
        float x = k - 1;
        return 1 + c3 * x * x * x + c1 * x * x;
    }

    static void render(G gg, int w, int h) {
        if (fase.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        GuiGraphics g = gg.raw;
        long ahora = System.currentTimeMillis();
        float dt = Math.min(0.1F, (ahora - ultimo) / 1000F);
        ultimo = ahora;
        long t = ahora - desde;
        if (jojoDesde < 0 && t > 16000 || jojoDesde >= 0 && ahora - jojoDesde > 5200) {
            fase = "";
            confeti.clear();
            return;
        }
        // dónde va el trineo (si ya lo ve el cliente, su posición de verdad)
        Entity tr = mc.level == null ? null : mc.level.getEntity(id);
        if (tr != null) {
            sx = tr.getX();
            sz = tr.getZ();
        }
        float salida = jojoDesde >= 0 ? Mth.clamp(1 - (ahora - jojoDesde - 800) / 500F, 0, 1) : 1;
        if (salida > 0) {
            cinta(g, w, t, salida, mc);
        }
        if (jojoDesde >= 0) {
            jojo(g, w, h, ahora - jojoDesde, dt);
        }
    }

    /** La cinta de arriba con el trineo, el texto, la distancia y la flecha. */
    private static void cinta(GuiGraphics g, int w, long t, float salida, Minecraft mc) {
        float k = Mth.clamp(t / 450F, 0, 1);
        float y = -40 + 48 * easeOutBack(k) - (1 - salida) * 50;
        int x = w / 2 - 110;
        g.pose().pushPose();
        g.pose().translate(0, y, 400);
        float late = 1 + 0.025F * Mth.sin(t / 160F);
        g.pose().translate(w / 2F, 16, 0);
        g.pose().scale(late, late, 1);
        g.pose().translate(-w / 2F, -16, 0);
        g.blit(TEX, x, 0, 220, 32, 0, 0, 440, 64, 512, 128);
        // el trineo de adorno, meciéndose
        ItemStack trineo = new ItemStack(com.navidad.bloque.deco.Deco.block("trineo") != null
                ? com.navidad.bloque.deco.Deco.block("trineo").asItem() : ModRegistry.CAJA_REGALO_ITEM.get());
        g.pose().pushPose();
        g.pose().translate(x + 30, 8 + Mth.sin(t / 220F) * 1.5F, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(Mth.sin(t / 300F) * 6));
        g.renderItem(trineo, -8, -8 + 8);
        g.pose().popPose();
        Component titulo = Component.translatable("navidad.aviso.viene");
        var f = mc.font;
        g.drawString(f, titulo, w / 2 - f.width(titulo) / 2, 7, 0xFFFFF4D0, true);
        double dx = sx - mc.player.getX(), dz = sz - mc.player.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        Component sub = dist < 30 ? Component.translatable("navidad.aviso.mira") : Component.translatable("navidad.aviso.lejos", dist);
        g.pose().pushPose();
        g.pose().translate(w / 2F - f.width(sub) * 0.75F / 2F, 18, 0);
        g.pose().scale(0.75F, 0.75F, 1);
        g.drawString(f, sub, 0, 0, 0xFFFFD84A, true);
        g.pose().popPose();
        // la flecha: hacia dónde está el trineo respecto a hacia dónde miras
        float hacia = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float rel = Mth.wrapDegrees(hacia - mc.player.getYRot());
        g.pose().pushPose();
        g.pose().translate(x + 192, 16, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(rel));
        float p = 1 + 0.12F * Mth.sin(t / 120F);
        g.pose().scale(p, p, 1);
        g.blit(TEX, -8, -8, 16, 16, 448, 0, 32, 32, 512, 128);
        g.pose().popPose();
        g.pose().popPose();
    }

    /** "¡JO, JO, JO!" con las letras saltando y el saludo. */
    private static void jojo(GuiGraphics g, int w, int h, long t, float dt) {
        Minecraft mc = Minecraft.getInstance();
        var f = mc.font;
        float fuera = Mth.clamp(1 - (t - 4400) / 700F, 0, 1);
        if (fuera > 0) {
            String texto = Component.translatable("navidad.santa.jojojo").getString();
            float esc = 3.2F;
            float ancho = f.width(texto) * esc;
            float x = w / 2F - ancho / 2F, y = h * 0.26F;
            int a = (int) (255 * fuera);
            for (int i = 0; i < texto.length(); i++) {
                String ch = texto.substring(i, i + 1);
                float cw = f.width(ch) * esc;
                long ti = t - i * 70L;
                if (ti > 0 && !ch.isBlank()) {
                    float s = easeOutBack(Mth.clamp(ti / 220F, 0, 1));
                    float brinco = -Math.abs(Mth.sin(ti / 1000F * 6F)) * 6F * Mth.clamp(ti / 300F, 0, 1);
                    g.pose().pushPose();
                    g.pose().translate(x + cw / 2, y + 4 * esc + brinco, 450);
                    g.pose().scale(esc * s, esc * s, 1);
                    g.pose().mulPose(Axis.ZP.rotationDegrees(Mth.sin(ti / 1000F * 5F + i) * 6));
                    int cx = -f.width(ch) / 2;
                    // orilla blanca, sombra roja oscura y la letra roja
                    g.drawString(f, ch, cx + 1, -3, (a << 24) | 0x5A0A10, false);
                    for (int[] o : new int[][]{{-1, -4}, {1, -4}, {0, -5}, {0, -3}}) {
                        g.drawString(f, ch, cx + o[0], o[1], (a << 24) | 0xFFFFFF, false);
                    }
                    g.drawString(f, ch, cx, -4, (a << 24) | 0xE0202C, false);
                    g.pose().popPose();
                }
                x += cw;
            }
            float sa = Mth.clamp((t - 700) / 400F, 0, 1) * fuera;
            if (sa > 0.02F) {
                Component c = Component.translatable("navidad.santa.feliz", nombre);
                g.pose().pushPose();
                g.pose().translate(w / 2F, y + 44, 450);
                g.pose().scale(1.5F, 1.5F, 1);
                g.drawString(f, c, -f.width(c) / 2, 0, ((int) (255 * sa) << 24) | 0xFFD84A, true);
                g.pose().popPose();
            }
        }
        // confeti
        for (float[] c : confeti) {
            c[1] += c[2] * dt;
            c[3] += dt * 2.5F;
            c[0] += Mth.sin(c[3]) * 22 * dt;
            c[6] += dt * 4;
            if (c[1] > h + 10) {
                continue;
            }
            g.pose().pushPose();
            g.pose().translate(c[0], c[1], 420);
            g.pose().mulPose(Axis.ZP.rotation(c[6]));
            int s = (int) c[5];
            float ancho = Math.abs(Mth.cos(c[6] * 1.3F)) * s + 0.5F;
            g.pose().scale(ancho, 1, 1);
            g.fill(-1, -s, 1, s, (int) c[4]);
            g.pose().popPose();
        }
    }
}
