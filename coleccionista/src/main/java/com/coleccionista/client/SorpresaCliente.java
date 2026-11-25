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
 * (Cliente) La caja sorpresa: a quién le toca, cuántos se ha comido Don Resorte (y cuántos faltan), si el siguiente está obligado a dar 3,
 * qué hacer cuando te toca, y la pantalla para elegir las vueltas. También: cuando la silla en la que vas sentado gira, tu vista gira con
 * ella (sirve para cualquier silla).
 */
public final class SorpresaCliente {
    private static final String MOD = Coleccionista.MODID;
    private static CompoundTag estado = new CompoundTag(), mio = new CompoundTag();
    private static boolean pantallaVista;
    private static Float yawSilla;
    private static Silla sillaAntes;

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

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !activa() || !EventoHud.visible()) {
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
