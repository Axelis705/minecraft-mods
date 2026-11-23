package com.axeliscore.npc.client;

import com.axeliscore.AxelisCore;
import com.axeliscore.combat.client.Proyeccion;
import com.axeliscore.data.Config;
import com.axeliscore.data.client.ClientData;
import com.axeliscore.npc.Mision;
import com.axeliscore.npc.Misiones;
import com.axeliscore.npc.Npc;
import com.axeliscore.npc.NpcAcciones;
import com.axeliscore.npc.NpcDef;
import com.axeliscore.npc.Objetivo;
import com.axeliscore.npc.Tienda;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Plat;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.InfoCard;
import com.axeliscore.ui.ScreenRegistry;
import com.axeliscore.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * (Cliente) Lo que se ve de los NPC y las misiones sin abrir ninguna ventana: el "!" (misión nueva) o "?" (misión lista) sobre la
 * cabeza, su nombre de cerca, la tarjeta al mirarlo y el rastreador de la misión que se sigue (a la derecha). También lee los datos del
 * jugador para las pantallas.
 */
public final class NpcCliente {
    private static final ResourceLocation PIEL = Ids.core("textures/entity/persona/mara.png");
    private static boolean rastreador = true, marcas = true;
    private static int leer;

    private NpcCliente() {
    }

    public static void init() {
        ScreenRegistry.register(NpcAcciones.PANTALLA, NpcScreen::new);
        ScreenRegistry.register(NpcAcciones.DIARIO, DiarioScreen::new);
        Npc.INFO = NpcCliente::info;
        com.axeliscore.fx.client.Hud.add(NpcCliente::render);
    }

    public static ResourceLocation piel(Npc n) {
        NpcDef d = n.def();
        return d != null && d.piel() != null ? d.piel() : PIEL;
    }

    public static ResourceLocation piel(NpcDef d) {
        return d != null && d.piel() != null ? d.piel() : PIEL;
    }

    // ------------------------------------------------------------------ datos del jugador

    public static CompoundTag yo() {
        return ClientData.player(AxelisCore.MODID);
    }

    public static CompoundTag misiones() {
        return Nbt.c(yo(), "misiones");
    }

    public static CompoundTag amistad() {
        return Nbt.c(yo(), "amistad");
    }

    public static long dia() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : Plat.dayTime(mc.level) / 24000L;
    }

    public static Misiones.Estado estado(Mision m) {
        return Misiones.estadoDe(m, misiones(), amistad(), dia());
    }

    public static int[] progreso(String id) {
        CompoundTag a = Nbt.c(misiones(), "activas");
        return a.contains(id) ? Nbt.ia(Nbt.c(a, id), "p") : new int[0];
    }

    public static int hechas(String id) {
        return Nbt.i(Nbt.c(misiones(), "hechas"), id);
    }

    public static int puntos(String npc) {
        return Nbt.i(amistad(), npc);
    }

    public static String sigue() {
        return Nbt.s(misiones(), "sigue");
    }

    /** Cuántas veces más se puede usar hoy esa oferta (Integer.MAX_VALUE = sin límite). */
    public static int quedan(NpcDef d, int i) {
        Tienda.Oferta o = d.tienda().ofertas().get(i);
        if (!o.limitada()) {
            return Integer.MAX_VALUE;
        }
        CompoundTag t = Nbt.c(Nbt.c(yo(), "tiendas"), d.id);
        if (Nbt.l(t, "dia") != dia()) {
            return o.existencias;
        }
        return Math.max(0, o.existencias - Nbt.i(t, "o" + i));
    }

    /** Cuántos objetos de ese id tiene el jugador. */
    public static int contar(String item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0;
        }
        net.minecraft.world.item.Item it = Plat.item(item);
        int n = 0;
        for (net.minecraft.world.item.ItemStack st : Plat.mainAndOffhand(mc.player.getInventory())) {
            if (!st.isEmpty() && st.getItem() == it) {
                n += st.getCount();
            }
        }
        return n;
    }

    /** 2 = tiene una misión lista para entregar ("?"), 1 = una nueva ("!"), 0 = nada. */
    public static int marca(NpcDef d) {
        int r = 0;
        for (Mision m : d.misiones()) {
            Misiones.Estado e = estado(m);
            if (e == Misiones.Estado.LISTA) {
                return 2;
            }
            if (e == Misiones.Estado.DISPONIBLE) {
                r = 1;
            }
        }
        return r;
    }

    /** Lo que agrega la tarjeta al mirarlo. */
    static void info(InfoCard card, NpcDef d) {
        int pts = puntos(d.id);
        int lv = d.nivelDe(pts);
        card.pair(Component.translatable("axelis_core.npc.amistad"), d.nombreNivel(lv));
        if (lv + 1 < d.niveles()) {
            int desde = d.umbral(lv), hasta = d.umbral(lv + 1);
            card.bar(Component.translatable("axelis_core.npc.siguiente"), (pts - desde) / (float) Math.max(1, hasta - desde), 0xFFFF7AB8,
                    pts + "/" + hasta);
        }
        int m = marca(d);
        if (m == 2) {
            card.text(Component.translatable("axelis_core.npc.tiene_lista"), 0xFF7CFC9A);
        } else if (m == 1) {
            card.text(Component.translatable("axelis_core.npc.tiene_mision"), 0xFFFFD166);
        }
    }

    // ------------------------------------------------------------------ HUD

    static void render(G g, int w, int h) {
        com.axeliscore.plat.client.TeclasCliente.revisar();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (--leer <= 0) {
            leer = 200;
            Config c = Config.of(AxelisCore.MODID);
            rastreador = c.bool("rastreador_misiones", true, "La misión que sigues, a la derecha de la pantalla");
            marcas = c.bool("marcas_npc", true, "El ! y el ? sobre la cabeza de los NPC con misiones");
        }
        if (marcas) {
            marcas(g, w, h, mc);
        }
        if (rastreador && mc.screen == null) {
            rastreador(g, w, h);
        }
    }

    private static void marcas(G g, int w, int h, Minecraft mc) {
        float pt = Proyeccion.partial();
        long ms = System.currentTimeMillis();
        for (Npc n : mc.level.getEntitiesOfClass(Npc.class, mc.player.getBoundingBox().inflate(24))) {
            NpcDef d = n.def();
            if (d == null || n.isInvisible()) {
                continue;
            }
            double dist = Math.sqrt(n.distanceToSqr(mc.player));
            if (dist > 6 && !mc.player.hasLineOfSight(n)) {
                continue;
            }
            int m = marca(d);
            boolean nombre = dist < 9;
            if (m == 0 && !nombre) {
                continue;
            }
            double x = Mth.lerp(pt, n.xo, n.getX()), y = Mth.lerp(pt, n.yo, n.getY()) + n.getBbHeight() + 0.35, z = Mth.lerp(pt, n.zo, n.getZ());
            float[] p = Proyeccion.aPantalla(x, y, z, w, h);
            if (p == null || p[0] < -30 || p[0] > w + 30 || p[1] < -30 || p[1] > h + 30) {
                continue;
            }
            float k = Mth.clamp(1.6F - p[2] / 16F, 0.55F, 1.3F);
            var f = Ui.font();
            if (nombre) {
                String s = d.nombre().getString();
                int tw = f.width(s);
                g.push();
                g.translate(p[0], p[1], 0);
                g.scale(k * 0.8F, k * 0.8F, 1);
                g.fill(-tw / 2 - 2, -1, tw / 2 + 2, 9, 0x70000000);
                g.drawString(f, s, -tw / 2, 0, 0xFFFFFFFF, false);
                g.pop();
            }
            if (m > 0) {
                String s = m == 2 ? "?" : "!";
                int col = m == 2 ? 0xFF7CFC9A : 0xFFFFD166;
                float bob = (float) Math.sin((ms + n.getId() * 137L) / 260.0) * 2F;
                g.push();
                g.translate(p[0], p[1] - (nombre ? 8 * k : 0) - 4 + bob, 0);
                g.scale(k * 2.2F, k * 2.2F, 1);
                int tw = f.width(s);
                g.drawString(f, s, -tw / 2 + 1, -8 + 1, 0xFF3A2A00, false);
                g.drawString(f, s, -tw / 2, -8, col, false);
                g.pop();
            }
        }
    }

    /** La misión que se sigue, a la derecha (título, objetivos y progreso). */
    private static void rastreador(G g, int w, int h) {
        Mision m = Misiones.porId(sigue());
        if (m == null) {
            return;
        }
        Misiones.Estado e = estado(m);
        if (e != Misiones.Estado.ACTIVA && e != Misiones.Estado.LISTA) {
            return;
        }
        var f = Ui.font();
        int[] prog = progreso(m.id);
        List<Objetivo> objs = m.objetivos();
        int ancho = 150;
        String titulo = Ui.fit(m.titulo().getString(), ancho - 8);
        int lineas = objs.size() + (e == Misiones.Estado.LISTA && !m.npc().isEmpty() ? 1 : 0);
        int alto = 16 + lineas * 10 + 4;
        int x = w - ancho - 6, y = h / 2 - alto / 2 - 20;
        g.fill(x - 2, y - 2, x + ancho, y + alto, 0x80000000);
        g.fill(x - 2, y - 2, x, y + alto, m.rareza().light);
        g.drawString(f, titulo, x + 4, y + 2, 0xFFFFD166, true);
        int yy = y + 15;
        for (int i = 0; i < objs.size(); i++) {
            Objetivo o = objs.get(i);
            int v = i < prog.length ? prog[i] : 0;
            boolean hecho = v >= o.cantidad;
            String cuenta = o.cantidad > 1 ? " " + v + "/" + o.cantidad : "";
            String txt = (hecho ? "✔ " : "• ") + o.descripcion().getString();
            txt = Ui.fit(txt, ancho - 10 - f.width(cuenta)) + cuenta;
            g.drawString(f, txt, x + 4, yy, hecho ? 0xFF7CFC9A : 0xFFE8ECF6, true);
            yy += 10;
        }
        if (e == Misiones.Estado.LISTA && !m.npc().isEmpty()) {
            NpcDef d = NpcDef.porId(m.npc());
            Component c = d == null ? Component.translatable("axelis_core.mision.lista")
                    : Component.translatable("axelis_core.mision.vuelve", d.nombre());
            g.drawString(f, Ui.fit("» " + c.getString(), ancho - 8), x + 4, yy, 0xFF7CFC9A, true);
        }
    }
}
