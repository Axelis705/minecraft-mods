package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Sacrificio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.UUID;

/**
 * (Cliente) El sacrificio: qué pasa en mi cuarto (alguien en el altar, la votación, "¡salgan!") y la pantalla para votar (se vuelve a
 * abrir sola si la cierro sin votar).
 */
public final class SacrificioCliente {
    private static final String MOD = Coleccionista.MODID;
    static CompoundTag estado = new CompoundTag();
    static UUID miVoto;

    private SacrificioCliente() {
    }

    public static void init() {
        ClientData.listen(Sacrificio.KEY, t -> {
            estado = t;
            if (!"VOTAR".equals(fase())) {
                miVoto = null;
            }
        });
        MinecraftForge.EVENT_BUS.addListener(SacrificioCliente::tick);
        com.axeliscore.fx.client.Hud.add(SacrificioCliente::hud);
        com.axeliscore.ui.ScreenRegistry.register(Sacrificio.PANTALLA, VotoScreen::new);
    }

    public static String fase() {
        String f = Nbt.s(estado, "fase");
        return f.isEmpty() ? "NADA" : f;
    }

    public static boolean activa() {
        return !"NADA".equals(fase());
    }

    /** Mi cuarto (null si no tengo). */
    static CompoundTag miCuarto() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        for (Tag x : estado.getList("cuartos", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            for (Tag y : c.getList("miembros", Tag.TAG_COMPOUND)) {
                if (((CompoundTag) y).getUUID("id").equals(mc.player.getUUID())) {
                    return c;
                }
            }
        }
        return null;
    }

    /** ¿Me toca votar ahora? (votación, mi cuarto sin decidir, vivo y sin haber votado) */
    static boolean meToca() {
        Minecraft mc = Minecraft.getInstance();
        CompoundTag c = miCuarto();
        if (c == null || mc.player == null || !"VOTAR".equals(fase()) || !"ESPERA".equals(Nbt.s(c, "estado"))) {
            return false;
        }
        for (Tag y : c.getList("miembros", Tag.TAG_COMPOUND)) {
            CompoundTag m = (CompoundTag) y;
            if (m.getUUID("id").equals(mc.player.getUUID())) {
                return Nbt.b(m, "vivo") && !Nbt.b(m, "voto");
            }
        }
        return false;
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null && miVoto == null && meToca() && mc.player.tickCount % 20 == 0) {
            mc.setScreen(new VotoScreen());
        }
    }

    private static String nombre(CompoundTag c, UUID id) {
        for (Tag y : c.getList("miembros", Tag.TAG_COMPOUND)) {
            CompoundTag m = (CompoundTag) y;
            if (m.getUUID("id").equals(id)) {
                return "Pieza " + String.format("%02d", Nbt.i(m, "numero")) + " · " + Nbt.s(m, "nombre");
            }
        }
        return "?";
    }

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!activa() || mc.player == null || !EventoHud.visible()) {
            return;
        }
        Font f = Ui.font();
        CompoundTag c = miCuarto();
        String linea = null;
        int color = 0xFFD8D0C8;
        String fase = fase();
        if (c != null) {
            String est = Nbt.s(c, "estado");
            boolean yoQuedo = c.hasUUID("queda") && c.getUUID("queda").equals(mc.player.getUUID());
            if ("SALIENDO".equals(est)) {
                linea = yoQuedo ? Component.translatable(MOD + ".sac.hud.te_quedas").getString()
                        : Component.translatable(MOD + ".sac.hud.salgan", Nbt.i(c, "sale")).getString();
                color = EventoHud.ROJO_CL;
            } else if ("RESUELTO".equals(est)) {
                linea = Component.translatable(MOD + ".sac.hud.resuelto").getString();
            } else if ("OFRECER".equals(fase)) {
                if (c.hasUUID("altar")) {
                    int seg = Math.max(0, 5 - Nbt.i(c, "altar_t") / 20);
                    linea = Component.translatable(MOD + ".sac.hud.en_altar", nombre(c, c.getUUID("altar")), seg).getString();
                    color = EventoHud.ORO;
                } else {
                    linea = Component.translatable(MOD + ".sac.hud.ofrecer").getString();
                }
            } else if ("VOTAR".equals(fase)) {
                linea = Component.translatable(MOD + ".sac.hud.votar").getString();
            } else if ("LISTA".equals(fase) || "HABLANDO".equals(fase)) {
                linea = Component.translatable(MOD + ".sac.hud.cuarto", Nbt.i(c, "k")).getString();
            }
        }
        if (linea != null) {
            int lw = f.width(linea) + 16, y = EventoHud.lineaY(w, lw);
            g.fillGradient(w / 2 - lw / 2, y, w / 2 + lw / 2, y + 13, 0xB0140C0E, 0x90080506);
            g.drawCenteredString(f, linea, w / 2, y + 3, color);
        }
    }
}
