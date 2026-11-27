package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.Cuerda;
import com.coleccionista.prueba.TresSentidos;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * (Cliente) Encadenados de tres: la cadena de cada trío (pasa por los postes donde se enredó), el freno de la cadena para el propio jugador,
 * la venda (la misma de A ciegas) y el HUD con el papel de cada uno.
 */
public final class SentidosCliente {
    private static final String MOD = Coleccionista.MODID;
    private static final ResourceLocation CADENA = new ResourceLocation("minecraft", "textures/block/chain.png");
    private static CompoundTag estado = new CompoundTag();
    /** Los tríos tal como llegan: miembros en orden y las cuerdas (con sus dobleces) entre cada par. */
    private static final List<Trio> trios = new ArrayList<>();
    private static final Map<UUID, Entity> ents = new HashMap<>();

    record Trio(int k, List<UUID> ids, List<String> roles, List<Cuerda> cuerdas, boolean salio) {
    }

    private SentidosCliente() {
    }

    public static void init() {
        ClientData.listen(TresSentidos.KEY, t -> {
            tres = t;
            recibir(activo());
        });
        ClientData.listen(com.coleccionista.prueba.Sentidos.KEY, t -> {
            sen = t;
            recibir(activo());
        });
        ClientData.listen(com.coleccionista.prueba.Sentidos.KEY + "_susto", t -> {
            int n = Nbt.i(t, "n");
            var lvl = Minecraft.getInstance().level;
            // solo si es de ahorita (al volver a entrar no se repite el susto)
            if (n != sustoN && lvl != null && Math.abs(lvl.getGameTime() - t.getLong("en")) < 80) {
                sustoDesde = System.currentTimeMillis();
                sustoCara = Nbt.b(t, "cara");
            }
            sustoN = n;
        });
        MinecraftForge.EVENT_BUS.addListener(SentidosCliente::tickJugador);
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) {
                buscarEntidades();
            }
        });
        MinecraftForge.EVENT_BUS.addListener(SentidosCliente::dibujar);
        com.axeliscore.fx.client.Hud.add(SentidosCliente::hud);
    }

    /** Lo último de cada prueba (Encadenados de tres y Los sentidos del capítulo 1): se dibuja la que esté en marcha. */
    private static CompoundTag tres = new CompoundTag(), sen = new CompoundTag();

    private static CompoundTag activo() {
        String f = Nbt.s(sen, "fase");
        return !f.isEmpty() && !"NADA".equals(f) ? sen : tres;
    }

    /** En qué cuarto del circuito va mi equipo (capítulo 1), o vacío. */
    public static String miEtapa() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return "";
        }
        for (Tag x : estado.getList("trios", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            for (Tag y : c.getList("miembros", Tag.TAG_COMPOUND)) {
                if (((CompoundTag) y).getUUID("id").equals(mc.player.getUUID())) {
                    return Nbt.s(c, "etapa");
                }
            }
        }
        return "";
    }

    private static void recibir(CompoundTag t) {
        estado = t;
        trios.clear();
        for (Tag x : t.getList("trios", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            List<UUID> ids = new ArrayList<>();
            List<String> roles = new ArrayList<>();
            for (Tag y : c.getList("miembros", Tag.TAG_COMPOUND)) {
                CompoundTag m = (CompoundTag) y;
                ids.add(m.getUUID("id"));
                roles.add(Nbt.s(m, "rol"));
            }
            List<Cuerda> cs = new ArrayList<>();
            for (Tag y : c.getList("cuerdas", Tag.TAG_COMPOUND)) {
                Cuerda cu = new Cuerda();
                for (Tag z : ((CompoundTag) y).getList("p", Tag.TAG_COMPOUND)) {
                    CompoundTag q = (CompoundTag) z;
                    cu.pivotes.add(new Cuerda.Pivote(q.getDouble("x"), q.getDouble("z"), q.getInt("g")));
                }
                cs.add(cu);
            }
            trios.add(new Trio(Nbt.i(c, "k"), ids, roles, cs, c.getBoolean("salio")));
        }
    }

    public static String fase() {
        String f = Nbt.s(estado, "fase");
        return f.isEmpty() ? "NADA" : f;
    }

    public static boolean activa() {
        return !"NADA".equals(fase());
    }

    private static double largo() {
        return estado.contains("largo") ? estado.getDouble("largo") : TresSentidos.largo;
    }

    private static Trio miTrio() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        for (Trio t : trios) {
            if (t.ids().contains(mc.player.getUUID())) {
                return t;
            }
        }
        return null;
    }

    /** El papel de cualquier jugador (para dibujarle la venda, la cinta o las esposas), o vacío. */
    public static String rolDe(UUID id) {
        String pr = rolPrueba(id);
        if (!pr.isEmpty()) {
            return pr;
        }
        String f = fase();
        if (!"ELEGIR".equals(f) && !"JUEGO".equals(f)) {
            return "";
        }
        for (Trio t : trios) {
            int i = t.ids().indexOf(id);
            if (i >= 0) {
                return t.salio() ? "" : t.roles().get(i);
            }
        }
        return "";
    }

    /** El papel puesto para probar ({@code /evento probar ...}), o vacío. */
    public static String rolPrueba(UUID id) {
        net.minecraft.nbt.CompoundTag t = com.axeliscore.data.client.ClientData.get(com.coleccionista.prueba.RolesPrueba.KEY);
        return t == null || id == null ? "" : t.getString(id.toString());
    }

    public static String miRol() {
        Minecraft mc = Minecraft.getInstance();
        String pr = mc.player == null ? "" : rolPrueba(mc.player.getUUID());
        if (!pr.isEmpty()) {
            return pr;
        }
        Trio t = miTrio();
        return t == null || mc.player == null ? "NADA" : t.roles().get(t.ids().indexOf(mc.player.getUUID()));
    }

    /** ¿Traigo la venda puesta? (la dibuja la misma venda de A ciegas) */
    public static boolean vendado() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && "VENDA".equals(rolPrueba(mc.player.getUUID()))) {
            return true;
        }
        return "JUEGO".equals(fase()) && "VENDA".equals(miRol()) && EventoCliente.vivo(EventoCliente.yo());
    }

    private static void buscarEntidades() {
        ents.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !"JUEGO".equals(fase())) {
            return;
        }
        for (Entity e : mc.level.entitiesForRendering()) {
            for (Trio t : trios) {
                if (t.ids().contains(e.getUUID())) {
                    ents.put(e.getUUID(), e);
                }
            }
        }
    }

    // ------------------------------------------------------------------ el freno de la cadena

    private static void tickJugador(TickEvent.PlayerTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.side != LogicalSide.CLIENT || e.player != mc.player || !"JUEGO".equals(fase()) || mc.player.isSpectator()) {
            return;
        }
        LocalPlayer me = mc.player;
        Trio t = miTrio();
        if (t == null || t.salio()) {
            return;
        }
        int i = t.ids().indexOf(me.getUUID());
        double max = largo();
        for (int j : new int[] {i - 1, i}) {
            if (j < 0 || j >= t.cuerdas().size()) {
                continue;
            }
            Cuerda c = t.cuerdas().get(j);
            Entity a = j == i ? me : ents.get(t.ids().get(j)), b = j == i ? ents.get(t.ids().get(j + 1)) : me;
            if (a == null || b == null) {
                continue;
            }
            double[] an = j == i ? c.anclaDeA(a.getX(), a.getZ(), b.getX(), b.getZ(), max) : c.anclaDeB(a.getX(), a.getZ(), b.getX(), b.getZ(), max);
            Entity otro = j == i ? b : a;
            boolean directo = c.pivotes.isEmpty();
            Vec3 ancla = new Vec3(an[0], directo ? otro.getY() : me.getY(), an[1]);
            if (e.phase == TickEvent.Phase.START) {
                me.setDeltaMovement(EncadenadosCliente.restringir(me.position(), me.getDeltaMovement(), ancla, an[2]));
            } else {
                EncadenadosCliente.regresar(me, ancla, an[2] + 0.5, directo && otro instanceof net.minecraft.world.entity.player.Player ? 0.5 : 1.0);
            }
        }
    }

    // ------------------------------------------------------------------ la cadena en el mundo

    private static void dibujar(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || !"JUEGO".equals(fase())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float pt = e.getPartialTick();
        Vec3 cam = e.getCamera().getPosition();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        RenderType rt = RenderType.entityCutoutNoCull(CADENA);
        VertexConsumer vc = buf.getBuffer(rt);
        for (Trio t : trios) {
            for (int j = 0; j < t.cuerdas().size(); j++) {
                Entity a = ents.get(t.ids().get(j)), b = ents.get(t.ids().get(j + 1));
                if (a == null || b == null || a.distanceToSqr(cam) > 80 * 80) {
                    continue;
                }
                Vec3 pa = a.getPosition(pt).add(0, a.getBbHeight() * 0.48, 0), pb = b.getPosition(pt).add(0, b.getBbHeight() * 0.48, 0);
                List<Vec3> puntos = new ArrayList<>();
                puntos.add(pa);
                Cuerda c = t.cuerdas().get(j);
                for (int k = 0; k < c.pivotes.size(); k++) {
                    double f = (k + 1.0) / (c.pivotes.size() + 1);
                    Cuerda.Pivote p = c.pivotes.get(k);
                    puntos.add(new Vec3(p.x(), pa.y + (pb.y - pa.y) * f, p.z()));
                }
                puntos.add(pb);
                double piso = Math.min(a.getPosition(pt).y, b.getPosition(pt).y) + 0.06;
                double total = c.largo(pa.x, pa.z, pb.x, pb.z);
                double floja = Math.max(0, largo() - total);
                for (int k = 0; k + 1 < puntos.size(); k++) {
                    Vec3 p0 = puntos.get(k), p1 = puntos.get(k + 1);
                    CadenaRender.cadena(vc, ps, p0, p1, p0.distanceTo(p1) + floja / (puntos.size() - 1), piso);
                }
            }
        }
        buf.endBatch(rt);
        ps.popPose();
    }

    // ------------------------------------------------------------------ HUD

    /** El susto del cuarto a oscuras: la cara de la muñeca de golpe (al que chocó) o las orillas rojas (a los demás del equipo). */
    private static final ResourceLocation SUSTO = com.axeliscore.plat.Ids.rl(MOD, "textures/gui/susto_muneca.png");
    private static int sustoN = -1;
    private static long sustoDesde;
    private static boolean sustoCara;

    private static void susto(G g, int w, int h) {
        float ts = (System.currentTimeMillis() - sustoDesde) / 1000F;
        if (sustoDesde <= 0 || ts > 1.4F) {
            return;
        }
        if (sustoCara && ts < 1.1F) {
            if (ts < 0.6F) {
                float esc = 1.3F - 0.3F * Math.min(1F, ts / 0.1F);
                int iw = (int) (w * esc), ih = (int) (iw * 400F / 512F);
                if (ih < h * esc) {
                    ih = (int) (h * esc);
                    iw = (int) (ih * 512F / 400F);
                }
                float sx = (float) Math.sin(ts * 80) * 5;
                g.fill(0, 0, w, h, 0xFF000000);
                g.blit(SUSTO, (int) (w / 2 - iw / 2 + sx), h / 2 - ih / 2, iw, ih, 0, 0, 512, 400, 512, 400);
            }
            float a = ts < 0.6F ? 0.15F : 0.6F * (1F - (ts - 0.6F) / 0.5F);
            g.fill(0, 0, w, h, Ui.alpha(0xFF000000, Math.max(0F, a)));
        } else if (!sustoCara) {
            float a = ts < 0.08F ? ts / 0.08F : 1F - (ts - 0.08F) / 1.32F;
            int capas = 12, grueso = Math.max(2, Math.min(w, h) / 40);
            for (int i = 0; i < capas; i++) {
                float k = 1F - i / (float) capas;
                int c = Ui.alpha(0xFF9A000C, Math.max(0F, Math.min(1F, a)) * 0.5F * k * k);
                int o = i * grueso;
                g.fill(o, o, w - o, o + grueso, c);
                g.fill(o, h - o - grueso, w - o, h - o, c);
                g.fill(o, o + grueso, o + grueso, h - o - grueso, c);
                g.fill(w - o - grueso, o + grueso, w - o, h - o - grueso, c);
            }
        }
    }

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        susto(g, w, h);
        // mientras está la presentación (título y tarjeta de reglas) no se encima nada
        if (!activa() || mc.player == null || !EventoHud.visible() || Presentacion.activa() || Presentacion.reglasVisibles()) {
            return;
        }
        Trio t = miTrio();
        if (t == null) {
            return;
        }
        String rol = miRol();
        String linea;
        if ("ELEGIR".equals(fase())) {
            linea = "NADA".equals(rol) ? Component.translatable(MOD + ".tres.hud.elige").getString()
                    : Component.translatable(MOD + ".tres.hud.elegiste", Component.translatable(MOD + ".tres.rol." + rol.toLowerCase())).getString();
        } else if ("JUEGO".equals(fase())) {
            linea = t.salio() ? Component.translatable(MOD + ".tres.hud.salieron").getString()
                    : Component.translatable(MOD + ".tres.hud.rol." + rol.toLowerCase()).getString();
        } else if ("LISTA".equals(fase()) || "HABLANDO".equals(fase())) {
            linea = Component.translatable(MOD + ".tres.hud.trio", t.k()).getString();
        } else {
            return;
        }
        EventoHud.lado(g, linea, 0xFFD8D0C8, true);
        // el equipo: quién trae qué
        for (int i = 0; i < t.ids().size(); i++) {
            CompoundTag p = EventoCliente.pieza(t.ids().get(i));
            String nom = p == null ? "?" : Nbt.s(p, "nombre");
            String r = t.roles().get(i);
            String s = nom + " · " + ("NADA".equals(r) ? "…" : Component.translatable(MOD + ".tres.rol." + r.toLowerCase()).getString());
            EventoHud.lado(g, s, 0xFFA89E94, false);
        }
        // capítulo 1: qué hay que hacer en el cuarto donde va el equipo
        String et = miEtapa();
        if ("JUEGO".equals(fase()) && !et.isEmpty() && !t.salio()) {
            EventoHud.lado(g, Component.translatable(MOD + ".sen.etapa." + et.toLowerCase()).getString(), 0xFFE0B050, true);
        }
    }
}
