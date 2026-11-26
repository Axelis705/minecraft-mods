package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Silla;
import com.coleccionista.prueba.Sorpresa;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/**
 * (Cliente) La caja sorpresa: a quién le toca, cuántos se ha comido Resortes (y cuántos faltan), si el siguiente está obligado a dar 3,
 * qué hacer cuando te toca, y la pantalla para elegir las vueltas. También: cuando la silla en la que vas sentado gira, tu vista gira con
 * ella (sirve para cualquier silla).
 */
public final class SorpresaCliente {
    private static final String MOD = Coleccionista.MODID;
    private static CompoundTag estado = new CompoundTag(), mio = new CompoundTag();
    private static boolean pantallaVista;
    private static Float yawSilla;
    private static Silla sillaAntes;
    private static final net.minecraft.resources.ResourceLocation SUSTO = com.axeliscore.plat.Ids.rl(MOD, "textures/gui/susto_resorte.png");
    private static int sustoN = -1;
    private static long sustoDesde;
    /** true: la cara de Resortes en toda la pantalla (solo al que se come); false: el susto de los demás (orillas rojas). */
    private static boolean sustoCara;

    private SorpresaCliente() {
    }

    public static void init() {
        ClientData.listen(Sorpresa.KEY, t -> estado = t);
        ClientData.listen(Sorpresa.KEY_MIO, t -> {
            mio = t;
            if (!Nbt.b(t, "elige")) {
                pantallaVista = false;
            }
        });
        ClientData.listen(Sorpresa.KEY + "_susto", t -> {
            int n = Nbt.i(t, "n");
            var lvl = Minecraft.getInstance().level;
            // solo si es de ahorita (al volver a entrar no se repite el susto)
            if (n != sustoN && lvl != null && Math.abs(lvl.getGameTime() - t.getLong("en")) < 80) {
                sustoDesde = System.currentTimeMillis();
                sustoCara = Nbt.b(t, "cara");
            }
            sustoN = n;
        });
        com.axeliscore.fx.client.Hud.add(SorpresaCliente::hud);
        MinecraftForge.EVENT_BUS.addListener(SorpresaCliente::tick);
    }

    public static String fase() {
        String f = Nbt.s(estado, "fase");
        return f.isEmpty() ? "NADA" : f;
    }

    public static boolean activa() {
        return !"NADA".equals(fase());
    }

    public static boolean elige() {
        return Nbt.b(mio, "elige");
    }

    public static boolean obligado() {
        return Nbt.b(mio, "obligado");
    }

    public static boolean meToca() {
        return Nbt.b(mio, "turno");
    }

    public static CompoundTag estado() {
        return estado;
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        // la silla gira: la vista también
        if (mc.player.getVehicle() instanceof Silla si) {
            float y = si.getYRot();
            if (si == sillaAntes && yawSilla != null) {
                float d = Mth.wrapDegrees(y - yawSilla);
                if (Math.abs(d) > 0.01F && Math.abs(d) < 45F) {
                    mc.player.setYRot(mc.player.getYRot() + d);
                    mc.player.yRotO += d;
                    mc.player.setYHeadRot(mc.player.getYHeadRot() + d);
                }
            }
            yawSilla = y;
            sillaAntes = si;
        } else {
            yawSilla = null;
            sillaAntes = null;
        }
        if (elige() && !pantallaVista && mc.screen == null) {
            pantallaVista = true;
            mc.setScreen(new VueltasScreen());
        }
    }

    /** Las orillas de la pantalla en rojo (más fuerte en el borde, nada en el centro): el susto sin taparle la vista a nadie. */
    private static void orillas(G g, int w, int h, float a) {
        int capas = 12, grueso = Math.max(2, Math.min(w, h) / 40);
        for (int i = 0; i < capas; i++) {
            float k = 1F - i / (float) capas;
            int c = Ui.alpha(0xFF9A000C, a * 0.5F * k * k);
            int o = i * grueso;
            g.fill(o, o, w - o, o + grueso, c);
            g.fill(o, h - o - grueso, w - o, h - o, c);
            g.fill(o, o + grueso, o + grueso, h - o - grueso, c);
            g.fill(w - o - grueso, o + grueso, w - o, h - o - grueso, c);
        }
    }

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !activa()) {
            return;
        }
        float ts = (System.currentTimeMillis() - sustoDesde) / 1000F;
        if (sustoDesde > 0 && sustoCara && ts < 1.1F) {
            // ¡pam! al que se come: la cara de Resortes de golpe, toda la pantalla, medio segundo (y un destello rojo que se va)
            if (ts < 0.55F) {
                float esc = 1.25F - 0.25F * Math.min(1F, ts / 0.12F);
                int iw = (int) (w * esc), ih = (int) (iw * 400F / 512F);
                if (ih < h * esc) {
                    ih = (int) (h * esc);
                    iw = (int) (ih * 512F / 400F);
                }
                float sx = (float) Math.sin(ts * 90) * 6;
                g.blit(SUSTO, (int) (w / 2 - iw / 2 + sx), h / 2 - ih / 2, iw, ih, 0, 0, 512, 400, 512, 400);
            }
            float a = ts < 0.55F ? 0.25F : 0.55F * (1F - (ts - 0.55F) / 0.55F);
            g.fill(0, 0, w, h, Ui.alpha(0xFF8A0008, Math.max(0F, a)));
        } else if (sustoDesde > 0 && !sustoCara && ts < 1.4F) {
            // a los demás: siguen viendo cómo se lo come; solo las orillas de la pantalla se ponen rojas de golpe (latido) y se van
            float a = ts < 0.08F ? ts / 0.08F : 1F - (ts - 0.08F) / 1.32F;
            a *= 1F + 0.25F * (float) Math.sin(ts * 18F) * (1F - Math.min(1F, ts));
            orillas(g, w, h, Math.max(0F, Math.min(1F, a)));
        }
        if (!EventoHud.visible()) {
            return;
        }
        String f = fase();
        if ("PASILLO".equals(f) || "LLEGANDO".equals(f) || "HABLANDO".equals(f) || "NADA".equals(f)) {
            return;
        }
        Font font = Ui.font();
        int comidos = Nbt.i(estado, "comidos"), cuota = Nbt.i(estado, "cuota");
        if (cuota > 0) {
            EventoHud.lado(g, Component.translatable(MOD + ".sor.hud.comidos", comidos, cuota).getString(), EventoHud.ROJO_CL, true);
        }
        if (meToca()) {
            String que = switch (f) {
                case "IR" -> ".sor.hud.ve";
                case "ELIGE" -> ".sor.hud.elige";
                case "GIRANDO" -> ".sor.hud.girando";
                case "VOLVER" -> ".sor.hud.vuelve";
                default -> "";
            };
            if (!que.isEmpty()) {
                EventoHud.lado(g, Component.translatable(MOD + que).getString(), EventoHud.ORO, false);
            }
            if (obligado() && ("IR".equals(f) || "ELIGE".equals(f))) {
                EventoHud.lado(g, Component.translatable(MOD + ".sor.hud.obligado").getString(), EventoHud.ROJO_CL, false);
            }
        } else if (!Nbt.s(estado, "nombre").isEmpty() && !"FIN".equals(f)) {
            EventoHud.lado(g, Component.translatable(MOD + ".sor.hud.le_toca", Nbt.s(estado, "nombre")).getString(), 0xFFD8D0C8, false);
            if ("GIRANDO".equals(f)) {
                EventoHud.lado(g, Component.translatable(MOD + ".sor.hud.vueltas", Nbt.i(estado, "dadas"), Nbt.i(estado, "quiere")).getString(),
                        0xFFA89E96, false);
            }
            if (Nbt.b(estado, "obligado") && ("IR".equals(f) || "ELIGE".equals(f))) {
                EventoHud.lado(g, Component.translatable(MOD + ".sor.hud.obligado_otro").getString(), EventoHud.ROJO_CL, false);
            }
        }
    }
}
