package com.coleccionista.client.inicio;

import com.axeliscore.plat.client.G;
import com.coleccionista.Coleccionista;
import com.coleccionista.client.ColeccionistaTV;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * (Cliente) La pantalla de inicio del evento: un escenario con telones, el reflector que parpadea y, en medio de la luz, el Coleccionista
 * en grande (el mismo de las pantallas, que respira, ladea la cabeza y sigue al ratón con la mirada). A la izquierda el título y tres
 * botones: <b>Jugar</b> (entra directo al servidor del evento), <b>Configurar</b> ({@link ConfigurarScreen}) y <b>Salir</b>. Suena su
 * vals de caja de música; la primera vez se prende como una tele vieja y él saluda.
 */
public class InicioScreen extends Screen {
    private static final String MOD = Coleccionista.MODID;
    private static final ResourceLocation FONDO = new ResourceLocation(MOD, "textures/gui/inicio_fondo.png");
    private static final ResourceLocation LUZ = new ResourceLocation(MOD, "textures/gui/inicio_luz.png");
    private static final int TW = 384, TH = 216;
    /** Dónde está él (como en las imágenes del fondo: x del reflector y el piso). */
    private static final float CX = 0.68F;
    private static final String SALUDO = "voz.bienvenida.que_tenemos";

    private final long abierta = System.currentTimeMillis();
    private final boolean prende;
    private final Random r = new Random();
    private final float[][] polvo = new float[46][3];
    private SoundInstance voz;
    private long vozFin;
    private Estilo.Boton salir;
    private String aviso = "";
    private long avisoHasta;

    public InicioScreen() {
        super(Component.translatable(MOD + ".inicio.titulo"));
        prende = !Inicio.saludo;
        Inicio.saludo = true;
        for (float[] p : polvo) {
            p[0] = r.nextFloat();
            p[1] = r.nextFloat();
            p[2] = 0.3F + r.nextFloat();
        }
    }

    @Override
    protected void init() {
        int bw = Math.min(130, (int) (width * 0.32F)), bh = 20;
        int x = Math.max(14, (int) (width * 0.07F));
        int y = (int) (height * 0.47F);
        addRenderableWidget(new Estilo.Boton(x, y, bw, bh + 4, Component.translatable(MOD + ".inicio.jugar"), this::jugar));
        addRenderableWidget(new Estilo.Boton(x, y + bh + 10, bw, bh, Component.translatable(MOD + ".inicio.configurar"),
                () -> minecraft.setScreen(new ConfigurarScreen(this))));
        salir = addRenderableWidget(new Estilo.Boton(x, y + 2 * bh + 16, bw, bh, Component.translatable(MOD + ".inicio.salir"),
                () -> minecraft.stop()));
        if (prende && voz == null) {
            Inicio.sonar("menu.estatica", SoundSource.MASTER, 0.5F, 1F);
        }
    }

    private void jugar() {
        Inicio.leer();
        String dir = Inicio.servidor;
        if (dir.isEmpty() || !ServerAddress.isValidAddress(dir)) {
            // todavía no hay servidor: se dice y se abre la lista de servidores de siempre
            aviso = Component.translatable(MOD + ".inicio.sin_servidor").getString();
            avisoHasta = System.currentTimeMillis() + 4000;
            minecraft.setScreen(new JoinMultiplayerScreen(this));
            return;
        }
        ServerData sd = new ServerData(Inicio.nombre, dir, false);
        sd.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        Coleccionista.LOGGER.info("COLECCIONISTA_INICIO jugar {}", dir);
        ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(dir), sd, false);
    }

    @Override
    public void tick() {
        long t = System.currentTimeMillis() - abierta;
        // el saludo: al terminar de prenderse la tele (solo la primera vez)
        if (prende && voz == null && t > 1800) {
            voz = Inicio.sonar(SALUDO, SoundSource.VOICE, 1F, 1F);
            vozFin = System.currentTimeMillis() + 4500;
        }
    }

    private boolean habla() {
        return voz != null && (minecraft.getSoundManager().isActive(voz) || System.currentTimeMillis() < vozFin - 3000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long ms = System.currentTimeMillis();
        float t = (ms - abierta) / 1000F;
        // ---- el escenario y el reflector (que parpadea de vez en cuando)
        g.blit(FONDO, 0, 0, width, height, 0, 0, TW, TH, TW, TH);
        float parpadeo = (ms / 90) % 61 < 2 || (ms / 70) % 97 < 1 ? 0.45F : 1F;
        float luz = (0.82F + 0.1F * Mth.sin(t * 2.3F) + 0.05F * Mth.sin(t * 7.1F)) * parpadeo;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1F, 1F, 1F, Mth.clamp(luz, 0F, 1F));
        g.blit(LUZ, 0, 0, width, height, 0, 0, TW, TH, TW, TH);
        g.setColor(1F, 1F, 1F, 1F);
        // ---- el polvo que flota en el haz de luz
        int cx = (int) (width * CX);
        for (float[] p : polvo) {
            float yy = (p[1] - t * 0.012F * p[2]) % 1F;
            if (yy < 0) {
                yy += 1F;
            }
            float ancho = 0.03F + 0.17F * yy;
            float xx = cx + (p[0] - 0.5F) * 2F * ancho * width + Mth.sin(t * 0.7F + p[0] * 20F) * 3F;
            int a = (int) (110 * p[2] * (1F - Math.abs(p[0] - 0.5F) * 2F) * luz);
            g.fill((int) xx, (int) (yy * height * 0.92F), (int) xx + 1, (int) (yy * height * 0.92F) + 1, a << 24 | 0xFFE8C0);
        }
        // ---- él, en grande, en medio de la luz (sigue al ratón con la mirada)
        int rh = (int) (height * 0.9F), rw = (int) (width * 0.5F);
        int rx = cx - rw / 2, ry = (int) (height * 0.93F) - rh;
        ColeccionistaTV.mirarLado = Mth.clamp((mx - cx) / (float) width * 1.4F, -0.55F, 0.55F);
        ColeccionistaTV.mirarAlto = Mth.clamp((my - ry - rh * 0.25F) / (float) height * 0.9F, -0.3F, 0.35F);
        boolean risa = salir != null && salir.isHoveredOrFocused();
        boolean habla = habla();
        ColeccionistaTV.dibujarHud(new G(g), rx, ry, rw, rh, habla || risa, risa ? "inicio.risa" : SALUDO);
        // (lo que sigue va encima de él: el modelo se dibuja con profundidad en z 200)
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        // ---- el título y lo que dice
        izquierda(g, t);
        if (habla) {
            subtitulo(g, Component.translatable(MOD + ".voz.bienvenida.que_tenemos").getString(), cx);
        }
        // ---- la tele vieja: rayas, grano y, de vez en cuando, una franja de estática
        tele(g, ms, t);
        super.render(g, mx, my, pt);
        if (!aviso.isEmpty() && ms < avisoHasta) {
            g.drawCenteredString(font, aviso, width / 2, height - 14, Estilo.ORO);
        }
        // ---- al abrir: se prende de negro (con un destello blanco como tele)
        float fade = prende ? 2.2F : 0.6F;
        if (t < fade) {
            float k = 1F - t / fade;
            int a = (int) (Mth.clamp(k, 0F, 1F) * 255);
            g.fill(0, 0, width, height, a << 24);
            if (prende && t > 0.25F && t < 0.55F) {
                int h2 = (int) (height * (0.55F - t) * 3F);
                g.fill(0, height / 2 - h2 / 2, width, height / 2 + h2 / 2 + 1, 0xC0FFFFFF);
            }
        }
        g.pose().popPose();
    }

    /** A la izquierda: "EL" chiquito, COLECCIONISTA grande (a veces tiembla una letra) y quién lo presenta. */
    private void izquierda(GuiGraphics g, float t) {
        int x = Math.max(14, (int) (width * 0.07F));
        int y = (int) (height * 0.16F);
        float esc = width >= 520 ? 3F : 2F;
        Estilo.grande(g, "EL", x + 1, y, Math.max(1F, esc - 1F), Estilo.TEXTO_APAGADO, false);
        String nombre = "COLECCIONISTA";
        int yy = (int) (y + 10 * Math.max(1F, esc - 1F));
        // la letra que tiembla
        int cual = (int) (t * 0.6F) % nombre.length();
        boolean tiembla = (t * 0.6F) % 1F < 0.12F;
        float px = x;
        for (int i = 0; i < nombre.length(); i++) {
            String c = String.valueOf(nombre.charAt(i));
            float dy = tiembla && i == cual ? (r.nextFloat() - 0.5F) * 2F : 0F;
            int col = tiembla && i == cual ? Estilo.VINO_CLARO | 0xFF000000 : 0xFFE9D9B8;
            Estilo.grande(g, c, px, yy + dy, esc, col, false);
            px += font.width(c) * esc;
        }
        int linea = (int) (yy + 9 * esc + 3);
        g.fill(x, linea, (int) px, linea + 1, Estilo.ORO_OSCURO);
        g.drawString(font, Component.translatable(MOD + ".inicio.lema").getString(), x, linea + 4, Estilo.TEXTO_APAGADO, true);
    }

    private void subtitulo(GuiGraphics g, String s, int cx) {
        int w = font.width(s) + 10;
        int x0 = Mth.clamp(cx - w / 2, 4, width - w - 4), y0 = (int) (height * 0.94F) - 12;
        g.fill(x0, y0, x0 + w, y0 + 13, 0xB0000000);
        g.drawString(font, s, x0 + 5, y0 + 3, 0xFFFFFFFF, false);
    }

    private void tele(GuiGraphics g, long ms, float t) {
        for (int y = (int) (ms / 60 % 3); y < height; y += 3) {
            g.fill(0, y, width, y + 1, 0x16000000);
        }
        // una franja que baja despacio, un poco más clara
        int fy = (int) ((t * 22F) % (height + 40)) - 20;
        g.fillGradient(0, fy, width, fy + 20, 0x00FFFFFF, 0x0CFFFFFF);
        // cada tanto, un golpe de estática en una franja
        if (ms % 7300 < 140) {
            int y0 = (int) ((ms / 7300 * 37) % Math.max(1, height - 30));
            for (int i = 0; i < 160; i++) {
                int x = r.nextInt(width), y = y0 + r.nextInt(26);
                g.fill(x, y, x + 1 + r.nextInt(3), y + 1, (40 + r.nextInt(80)) << 24 | 0xFFFFFF);
            }
        }
    }

    @Override
    public Music getBackgroundMusic() {
        return Inicio.MUSICA;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void removed() {
        ColeccionistaTV.mirarLado = 0F;
        ColeccionistaTV.mirarAlto = 0F;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
