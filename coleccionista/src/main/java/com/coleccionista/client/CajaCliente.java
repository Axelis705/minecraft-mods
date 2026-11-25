package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.VB;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.axeliscore.vestir.client.Prenda;
import com.axeliscore.vestir.client.VestuarioCliente;
import com.coleccionista.Coleccionista;
import com.coleccionista.bot.PiezaBot;
import com.coleccionista.prueba.Caja;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;

/**
 * (Cliente) La caja de música: la carta del papel (y con quién, si eres traidor), los apagones, dónde vas sentado y qué te toca, los
 * susurros de los bots, la tarjeta con el símbolo en la espalda de cada quien (brilla un poco: se lee en la góndola oscura), la máscara
 * de muñeco, sin nombres ni números mientras dura la parte a ciegas, la pantalla para marcar tu símbolo y la del organizador.
 */
public final class CajaCliente {
    private static final String MOD = Coleccionista.MODID;
    public static final String[] SIMBOLOS = Caja.SIMBOLOS;
    private static final ResourceLocation[] ICONO = new ResourceLocation[4], TARJETA = new ResourceLocation[4];
    private static CompoundTag estado = new CompoundTag(), mio = new CompoundTag(), org = new CompoundTag();
    private static String rolVisto = "";
    private static long cartaDesde, apagonDesde, susurroDesde;
    private static int apagonN = -1, apagonDura = 70, susurroN = -1;
    private static String susurro = "";
    private static String eligeVisto = "", orgVisto = "";

    static {
        for (int i = 0; i < 4; i++) {
            ICONO[i] = Ids.rl(MOD, "textures/gui/caja_" + SIMBOLOS[i] + ".png");
            TARJETA[i] = Ids.rl(MOD, "textures/prenda/caja_etiqueta_" + SIMBOLOS[i] + ".png");
        }
    }

    private CajaCliente() {
    }

    public static void init() {
        ClientData.listen(Caja.KEY, t -> estado = t);
        ClientData.listen(Caja.KEY_ORG, t -> org = t);
        ClientData.listen(Caja.KEY_MIO, t -> {
            mio = t;
            String rol = Nbt.s(t, "rol");
            if (!rol.isEmpty() && !rol.equals(rolVisto)) {
                rolVisto = rol;
                cartaDesde = System.currentTimeMillis();
            }
            if (rol.isEmpty()) {
                rolVisto = "";
            }
        });
        ClientData.listen(Caja.KEY + "_apagon", t -> {
            int n = Nbt.i(t, "n");
            if (n != apagonN) {
                apagonN = n;
                apagonDura = Math.max(20, Nbt.i(t, "dura"));
                apagonDesde = System.currentTimeMillis();
            }
        });
        ClientData.listen(Caja.KEY + "_susurro", t -> {
            int n = Nbt.i(t, "n");
            if (n != susurroN) {
                susurroN = n;
                susurro = Nbt.s(t, "simbolo");
                susurroDesde = System.currentTimeMillis();
            }
        });
        com.axeliscore.fx.client.Hud.add(CajaCliente::hud);
        MinecraftForge.EVENT_BUS.addListener(CajaCliente::tick);
        // mientras es a ciegas: ni nombres arriba de nadie
        MinecraftForge.EVENT_BUS.addListener((RenderNameTagEvent e) -> {
            if (anonimo() && (e.getEntity() instanceof Player || e.getEntity() instanceof PiezaBot)) {
                e.setResult(Event.Result.DENY);
            }
        });
    }

    /** Las prendas: la tarjeta de la espalda y la máscara (se registran con las demás prendas). */
    public static void prendas() {
        VestuarioCliente.registrar(Caja.ETIQUETA, Prenda.piel(Ids.rl(MOD, "textures/prenda/vacia.png")).sinPrimeraPersona()
                .decorar((c, l) -> {
                    if (c.primeraPersona()) {
                        return;
                    }
                    int s = Mth.clamp(c.datos().getInt("s"), 0, 3);
                    PoseStack st = l.en(Prenda.Parte.CUERPO);
                    if (st == null) {
                        return;
                    }
                    // en la espalda (z = 2), arriba (y 1.5 a 7), un poco despegada; se ve desde atrás sin espejo
                    float z = (2F + l.inflado() + 0.35F) / 16F, x0 = -2.75F / 16F, x1 = 2.75F / 16F, y0 = 1.3F / 16F, y1 = 6.8F / 16F;
                    // con su luz propia (pintura que brilla un poco): se lee en la góndola oscura y el negro de la pica se ve negro
                    VB vb = l.draw().cutout(TARJETA[s]);
                    PoseStack.Pose p = st.last();
                    int luz = 0xF000F0;
                    vb.v(p, x1, y0, z, 255, 255, 255, 255, 0, 0, OverlayTexture.NO_OVERLAY, luz, 0, 0, 1);
                    vb.v(p, x1, y1, z, 255, 255, 255, 255, 0, 1, OverlayTexture.NO_OVERLAY, luz, 0, 0, 1);
                    vb.v(p, x0, y1, z, 255, 255, 255, 255, 1, 1, OverlayTexture.NO_OVERLAY, luz, 0, 0, 1);
                    vb.v(p, x0, y0, z, 255, 255, 255, 255, 1, 0, OverlayTexture.NO_OVERLAY, luz, 0, 0, 1);
                    st.popPose();
                }));
        VestuarioCliente.registrar(Caja.MASCARA, Prenda.piel(Ids.rl(MOD, "textures/prenda/caja_mascara.png"))
                .sin(Prenda.Parte.CUERPO, Prenda.Parte.BRAZO_DER, Prenda.Parte.BRAZO_IZQ, Prenda.Parte.PIERNA_DER, Prenda.Parte.PIERNA_IZQ)
                .inflar(0.45F));
    }

    // ------------------------------------------------------------------ consultas

    public static String fase() {
        String f = Nbt.s(estado, "fase");
        return f.isEmpty() ? "NADA" : f;
    }

    public static boolean activa() {
        return !"NADA".equals(fase());
    }

    /** ¿Es la parte a ciegas (máscaras, sin nombres ni números)? */
    public static boolean anonimo() {
        return Nbt.b(estado, "anonimo");
    }

    /** ¿Me toca marcar mi símbolo (y todavía no lo marco)? */
    public static boolean debeElegir() {
        return Nbt.b(mio, "elige") && !mio.contains("eligio");
    }

    public static boolean elige() {
        return Nbt.b(mio, "elige");
    }

    public static int eligio() {
        return mio.contains("eligio") ? Nbt.i(mio, "eligio") : -1;
    }

    public static String rol() {
        return Nbt.s(mio, "rol");
    }

    public static CompoundTag mio() {
        return mio;
    }

    public static CompoundTag estado() {
        return estado;
    }

    public static CompoundTag org() {
        return org;
    }

    public static ResourceLocation icono(int s) {
        return ICONO[Mth.clamp(s, 0, 3)];
    }

    public static int indice(String s) {
        for (int i = 0; i < SIMBOLOS.length; i++) {
            if (SIMBOLOS[i].equals(s)) {
                return i;
            }
        }
        return -1;
    }

    public static String nombreSimbolo(int s) {
        return s < 0 ? "—" : Component.translatable(MOD + ".caja.simbolo." + SIMBOLOS[Mth.clamp(s, 0, 3)]).getString();
    }

    /** ¿Soy un organizador que decide (no juego)? */
    public static boolean decido() {
        return org.contains("nombre") && ("REVISION".equals(Nbt.s(org, "fase")) || "TRAIDORES".equals(Nbt.s(org, "fase")));
    }

    // ------------------------------------------------------------------ abrir solas las pantallas

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        // me apunta el arma: la pantalla de los símbolos (una vez por turno)
        String clave = Nbt.s(estado, "paso") + ":" + (estado.hasUUID("apuntado") ? estado.getUUID("apuntado") : "");
        if (debeElegir() && !clave.equals(eligeVisto) && mc.screen == null) {
            eligeVisto = clave;
            mc.setScreen(new EligeSimboloScreen());
        }
        // el organizador: la de Bien o Bala, cuando ya le toca decidir
        String ok = Nbt.s(org, "nombre") + ":" + Nbt.i(org, "i") + ":" + Nbt.s(org, "fase");
        if (decido() && "ESPERA".equals(Nbt.s(org, "paso")) && Nbt.b(org, "juez") && !ok.equals(orgVisto) && mc.screen == null) {
            orgVisto = ok;
            mc.setScreen(new RevisionScreen());
        }
    }

    // ------------------------------------------------------------------ el HUD

    private static float suave(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !activa() && apagonN < 0) {
            return;
        }
        Font f = Ui.font();
        long ahora = System.currentTimeMillis();
        // el apagón: negro total (con un fundido corto al entrar y al salir)
        float ta = (ahora - apagonDesde) / 1000F, dura = apagonDura / 20F;
        if (apagonN >= 0 && ta < dura + 0.4F && activa()) {
            float a = suave(ta / 0.35F) * (1 - suave((ta - dura + 0.5F) / 0.6F));
            g.fill(0, 0, w, h, Ui.alpha(0xFF000000, a));
            if (a > 0.9F) {
                // la llave de cuerda, girando chiquita en medio (el carrusel se mueve)
                String llave = Component.translatable(MOD + ".caja.apagon").getString();
                g.drawCenteredString(f, llave, w / 2, h / 2 + 30, Ui.alpha(0xFF3A2E2A, a));
            }
            return;
        }
        if (!activa()) {
            return;
        }
        // la carta del papel (8 s)
        float t = (ahora - cartaDesde) / 1000F;
        if (cartaDesde > 0 && t < 8F && !rolVisto.isEmpty()) {
            float a = suave(t / 0.6F) * (1 - suave((t - 7.2F) / 0.8F));
            boolean traidor = "traidor".equals(rolVisto);
            g.fill(0, 0, w, h, Ui.alpha(0xFF000000, 0.86F * a));
            EventoHud.texto(g, f, Component.translatable(MOD + ".caja.eres").getString(), w / 2, h / 2 - 54, 1.4F, Ui.alpha(0xFFA89E96, a));
            EventoHud.texto(g, f, Component.translatable(MOD + (traidor ? ".caja.rol_traidor" : ".caja.rol_inocente")).getString(), w / 2,
                    h / 2 - 34, 4F, Ui.alpha(traidor ? EventoHud.ROJO_CL : EventoHud.PORCELANA, a));
            Ui.text(g, Component.translatable(MOD + (traidor ? ".caja.explica_traidor" : ".caja.explica_inocente")).getString(), w / 2 - 150,
                    h / 2 + 14, 300, Ui.alpha(0xFFD8D0C8, a), 11);
            if (traidor && !Nbt.s(mio, "otro").isEmpty()) {
                EventoHud.texto(g, f, Component.translatable(MOD + ".caja.otro_traidor", Nbt.s(mio, "otro")).getString(), w / 2, h / 2 + 56, 1.3F,
                        Ui.alpha(EventoHud.ROJO_CL, a));
            }
            return;
        }
        if (!EventoHud.visible()) {
            return;
        }
        String fase = fase();
        // dónde voy sentado y qué me toca
        if ("TURNO".equals(fase) && mio.contains("gondola")) {
            String lado = Nbt.s(mio, "lado");
            EventoHud.lado(g, Component.translatable(MOD + ".caja.hud.lugar", Nbt.i(mio, "gondola"),
                    Component.translatable(MOD + ".caja.lado." + lado)).getString(), EventoHud.ORO, true);
            boolean traidor = "traidor".equals(Nbt.s(mio, "rol"));
            String que = "atras".equals(lado) ? (traidor ? ".caja.hud.atras_traidor" : ".caja.hud.atras") : "solo".equals(lado) ? ".caja.hud.solo"
                    : ".caja.hud.frente";
            EventoHud.lado(g, Component.translatable(MOD + que).getString(), 0xFFD8D0C8, false);
            EventoHud.lado(g, Component.translatable(MOD + ".caja.hud.turno", Nbt.i(estado, "turno"), Nbt.i(estado, "turnos")).getString(),
                    0xFFA89E96, false);
        }
        if ("traidor".equals(Nbt.s(mio, "rol")) && ("TURNO".equals(fase) || "ROLES".equals(fase))) {
            g.drawString(f, Component.translatable(MOD + ".caja.hud.eres_traidor", Nbt.s(mio, "otro")).getString(), 6, h - 12, 0x90E0404A, false);
        }
        // el susurro de la voz de atrás (8 s)
        float ts = (ahora - susurroDesde) / 1000F;
        if (!susurro.isEmpty() && ts < 8F && "TURNO".equals(fase)) {
            float a = suave(ts / 0.4F) * (1 - suave((ts - 7F) / 1F));
            int s = indice(susurro);
            String txt = Component.translatable(MOD + ".caja.susurro", nombreSimbolo(s)).getString();
            int y = h - 92;
            int tw = f.width(txt);
            g.fill(w / 2 - tw / 2 - 30, y - 6, w / 2 + tw / 2 + 10, y + 22, Ui.alpha(0x90000000, a));
            g.blitTint(icono(s), w / 2 - tw / 2 - 26, y - 3, 16, 16, 0, 0, 64, 64, 64, 64, Ui.alpha(0xFFFFFFFF, a));
            g.drawString(f, txt, w / 2 - tw / 2, y + 2, Ui.alpha(0xFFC8C0D8, a), true);
        }
        // en la revisión: a quién apunta el arma (y si soy yo, que marque)
        if (("REVISION".equals(fase) || "TRAIDORES".equals(fase)) && estado.hasUUID("apuntado")) {
            boolean yo = estado.getUUID("apuntado").equals(mc.player.getUUID());
            if (yo) {
                String txt = Component.translatable(MOD + (elige() ? ".caja.hud.te_apunta_marca" : ".caja.hud.te_apunta")).getString();
                EventoHud.texto(g, f, txt, w / 2, h / 2 + 34, 1.4F, EventoHud.ROJO_CL);
                int e = eligio();
                if (e >= 0) {
                    g.blit(icono(e), w / 2 - 16, h / 2 + 52, 32, 32, 0, 0, 64, 64, 64, 64);
                }
            }
        }
        // el organizador: a quién apunta, su símbolo real y lo que marcó
        if (decido() && !(mc.screen instanceof RevisionScreen)) {
            organizador(g, f, w, h);
        }
    }

    private static void organizador(G g, Font f, int w, int h) {
        int pw = 170, x = w - pw - 6, y = 40;
        boolean traidor = Nbt.b(org, "traidor");
        g.fillGradient(x, y, x + pw, y + 92, 0xD0140C0E, 0xC0080506);
        g.fill(x, y, x + 2, y + 92, EventoHud.ROJO);
        g.drawString(f, Component.translatable(MOD + ".caja.org.titulo", Nbt.i(org, "i"), Nbt.i(org, "de")).getString(), x + 8, y + 5, 0xFFA89E96,
                false);
        g.drawString(f, Ui.fit(Nbt.s(org, "nombre"), pw - 14), x + 8, y + 17, EventoHud.PORCELANA, true);
        int real = indice(Nbt.s(org, "real")), marco = indice(Nbt.s(org, "marco"));
        g.drawString(f, Component.translatable(MOD + ".caja.org.real").getString(), x + 8, y + 32, 0xFFA89E96, false);
        if (real >= 0) {
            g.blit(icono(real), x + 8, y + 42, 24, 24, 0, 0, 64, 64, 64, 64);
        }
        g.drawString(f, Component.translatable(MOD + ".caja.org.marco").getString(), x + 88, y + 32, 0xFFA89E96, false);
        if (marco >= 0) {
            g.blit(icono(marco), x + 88, y + 42, 24, 24, 0, 0, 64, 64, 64, 64);
            boolean bien = marco == real;
            g.drawString(f, bien ? "✔" : "✘", x + 116, y + 50, bien ? 0xFF60C060 : EventoHud.ROJO_CL, true);
        } else {
            g.drawString(f, "…", x + 96, y + 50, 0xFFA89E96, false);
        }
        String pie = traidor ? Component.translatable(MOD + (Nbt.b(org, "fase_traidores") ? ".caja.org.traidor_final" : ".caja.org.traidor"))
                .getString() : Component.translatable(MOD + ".caja.org.decide").getString();
        Ui.text(g, pie, x + 8, y + 70, pw - 14, traidor ? EventoHud.ROJO_CL : 0xFFD8D0C8, 9);
    }
}
