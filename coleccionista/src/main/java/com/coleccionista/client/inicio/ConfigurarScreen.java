package com.coleccionista.client.inicio;

import com.coleccionista.Coleccionista;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * (Cliente) Configurar, lo justo: a la izquierda las teclas para moverse (adelante, atrás, a los lados, saltar, correr, agacharse) y la
 * de hablar del chat de voz; a la derecha la sensibilidad del ratón y el volumen (general, música, la voz del Coleccionista y los
 * efectos). El tamaño de la interfaz no se toca: siempre es 3 para todos.
 */
public class ConfigurarScreen extends Screen {
    private static final String MOD = Coleccionista.MODID;
    /** Los sonidos que van juntos en "Efectos". */
    private static final SoundSource[] EFECTOS = {SoundSource.BLOCKS, SoundSource.HOSTILE, SoundSource.PLAYERS, SoundSource.AMBIENT,
            SoundSource.RECORDS, SoundSource.NEUTRAL, SoundSource.WEATHER};

    private final Screen antes;
    private final List<Fila> filas = new ArrayList<>();
    /** La tecla que se está cambiando (esperando a que aprieten otra). */
    private Fila esperando;

    private record Fila(KeyMapping tecla, Component nombre, Estilo.Boton boton) {
    }

    public ConfigurarScreen(Screen antes) {
        super(Component.translatable(MOD + ".inicio.configurar"));
        this.antes = antes;
    }

    /** La tecla de hablar del chat de voz (Simple Voice Chat), si está instalado. */
    private KeyMapping hablar() {
        for (KeyMapping k : minecraft.options.keyMappings) {
            if (k.getName().equals("key.push_to_talk")) {
                return k;
            }
        }
        return null;
    }

    @Override
    protected void init() {
        filas.clear();
        Options o = minecraft.options;
        int mitad = width / 2;
        int col = Math.min(170, mitad - 18);
        int xi = mitad - col - 8, xd = mitad + 8;
        int y0 = 40, alto = 16, paso = 19;
        // ---- las teclas
        List<Object[]> teclas = new ArrayList<>();
        teclas.add(new Object[] {o.keyUp, "adelante"});
        teclas.add(new Object[] {o.keyDown, "atras"});
        teclas.add(new Object[] {o.keyLeft, "izquierda"});
        teclas.add(new Object[] {o.keyRight, "derecha"});
        teclas.add(new Object[] {o.keyJump, "saltar"});
        teclas.add(new Object[] {o.keySprint, "correr"});
        teclas.add(new Object[] {o.keyShift, "agacharse"});
        KeyMapping voz = hablar();
        if (voz != null) {
            teclas.add(new Object[] {voz, "hablar"});
        }
        int y = y0;
        int bw = Math.min(70, col / 2 - 4);
        for (Object[] t : teclas) {
            KeyMapping km = (KeyMapping) t[0];
            Component nombre = Component.translatable(MOD + ".config." + t[1]);
            Fila[] f = new Fila[1];
            Estilo.Boton b = new Estilo.Boton(xi + col - bw, y, bw, alto, km.getTranslatedKeyMessage(), () -> esperar(f[0]));
            f[0] = new Fila(km, nombre, b);
            filas.add(f[0]);
            addRenderableWidget(b);
            y += paso;
        }
        // ---- el ratón y el sonido
        y = y0;
        addRenderableWidget(new Estilo.Deslizador(xd, y, col, alto, () -> o.sensitivity().get(), v -> o.sensitivity().set(v),
                v -> Component.translatable(MOD + ".config.raton", (int) (v * 200) + "%")));
        y += paso + 8;
        y = volumen(xd, y, col, alto, SoundSource.MASTER, "general") + paso;
        y = volumen(xd, y, col, alto, SoundSource.MUSIC, "musica") + paso;
        y = volumen(xd, y, col, alto, SoundSource.VOICE, "voz") + paso;
        Options op = o;
        addRenderableWidget(new Estilo.Deslizador(xd, y, col, alto, () -> op.getSoundSourceOptionInstance(SoundSource.BLOCKS).get(), v -> {
            for (SoundSource s : EFECTOS) {
                op.getSoundSourceOptionInstance(s).set(v);
            }
        }, v -> Component.translatable(MOD + ".config.efectos", (int) (v * 100) + "%")));
        // ---- listo
        int lw = 110;
        addRenderableWidget(new Estilo.Boton(mitad - lw / 2, height - 28, lw, 20, Component.translatable(MOD + ".config.listo"), this::onClose));
    }

    private int volumen(int x, int y, int w, int h, SoundSource s, String clave) {
        Options o = minecraft.options;
        addRenderableWidget(new Estilo.Deslizador(x, y, w, h, () -> o.getSoundSourceOptionInstance(s).get(),
                v -> o.getSoundSourceOptionInstance(s).set(v), v -> Component.translatable(MOD + ".config." + clave, (int) (v * 100) + "%")));
        return y;
    }

    private void esperar(Fila f) {
        esperando = f;
        f.boton().setMessage(Component.literal("> ? <"));
    }

    private void poner(InputConstants.Key k) {
        Fila f = esperando;
        esperando = null;
        if (k != null) {
            minecraft.options.setKey(f.tecla(), k);
            f.tecla().setKeyModifierAndCode(KeyModifier.NONE, k);
            KeyMapping.resetMapping();
            minecraft.options.save();
            Coleccionista.LOGGER.info("COLECCIONISTA_INICIO tecla {} = {}", f.tecla().getName(), k.getName());
        }
        f.boton().setMessage(f.tecla().getTranslatedKeyMessage());
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (esperando != null) {
            poner(key == GLFW.GLFW_KEY_ESCAPE ? null : InputConstants.getKey(key, scan));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int boton) {
        if (esperando != null && !esperando.boton().isMouseOver(mx, my)) {
            poner(InputConstants.Type.MOUSE.getOrCreate(boton));
            return true;
        }
        if (esperando != null) {
            // clic sobre el mismo botón: también vale como tecla del ratón (izquierdo no, que es para elegir)
            if (boton != 0) {
                poner(InputConstants.Type.MOUSE.getOrCreate(boton));
                return true;
            }
            return true;
        }
        return super.mouseClicked(mx, my, boton);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (minecraft.level == null) {
            g.fill(0, 0, width, height, 0xFF0B0607);
        } else {
            g.fill(0, 0, width, height, 0xB0000000);
        }
        int mitad = width / 2;
        int col = Math.min(170, mitad - 18);
        Estilo.panel(g, mitad - col - 16, 8, mitad + col + 16, height - 34);
        Estilo.grande(g, getTitle().getString(), mitad, 14, 2F, 0xFFE9D9B8, true);
        g.fill(mitad - col - 8, 33, mitad + col + 8, 34, Estilo.ORO_OSCURO);
        g.fill(mitad, 38, mitad + 1, height - 40, 0x40D9A93A);
        int xi = mitad - col - 8;
        for (Fila f : filas) {
            int y = f.boton().getY();
            boolean es = f == esperando;
            g.drawString(font, f.nombre(), xi + 2, y + 4, es ? Estilo.ORO : Estilo.TEXTO, true);
        }
        super.render(g, mx, my, pt);
        String ayuda = esperando != null ? Component.translatable(MOD + ".config.aprieta").getString()
                : Component.translatable(MOD + ".config.ayuda").getString();
        g.drawCenteredString(font, ayuda, mitad, height - 46, Estilo.TEXTO_APAGADO);
    }

    @Override
    public void onClose() {
        minecraft.options.save();
        minecraft.setScreen(antes);
    }

    @Override
    public Music getBackgroundMusic() {
        return minecraft.level == null ? Inicio.MUSICA : null;
    }
}
