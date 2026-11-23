package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.G;
import com.axeliscore.ui.Ui;
import com.coleccionista.Coleccionista;
import com.coleccionista.prueba.ACiegas;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * (Cliente) A ciegas: la venda de los que caminan (todo negro; cada pocos segundos un pulso blanco, como un eco, deja ver un instante lo
 * que hay cerca), las trampas en rojo, las baldosas intermitentes (naranja apagadas, rojo encendidas) y la salida en verde para el guía
 * (y el organizador), y el HUD de cada equipo. La misma venda la usa Encadenados de tres.
 */
public final class CiegasCliente {
    private static final String MOD = Coleccionista.MODID;
    private static CompoundTag estado = new CompoundTag();
    private static float venda;
    /** El último pulso que sonó (para que suene una vez por pulso). */
    private static long ultimoPulso = -1;

    private CiegasCliente() {
    }

    public static void init() {
        ClientData.listen(ACiegas.KEY, t -> estado = t);
        MinecraftForge.EVENT_BUS.addListener(CiegasCliente::niebla);
        MinecraftForge.EVENT_BUS.addListener(CiegasCliente::colorNiebla);
        MinecraftForge.EVENT_BUS.addListener(CiegasCliente::dibujar);
        MinecraftForge.EVENT_BUS.addListener(CiegasCliente::vendaNegra);
        com.axeliscore.fx.client.Hud.add(CiegasCliente::hud);
    }

    public static String fase() {
        String f = Nbt.s(estado, "fase");
        return f.isEmpty() ? "NADA" : f;
    }

    public static boolean activa() {
        return !"NADA".equals(fase());
    }

    /** Mi equipo (null si no tengo). */
    private static CompoundTag miEquipo() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        for (Tag x : estado.getList("equipos", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) x;
            for (Tag y : e.getList("miembros", Tag.TAG_COMPOUND)) {
                if (((CompoundTag) y).getUUID("id").equals(mc.player.getUUID())) {
                    return e;
                }
            }
        }
        return null;
    }

    private static boolean soyGuia(CompoundTag e) {
        Minecraft mc = Minecraft.getInstance();
        return e != null && mc.player != null && e.hasUUID("guia") && e.getUUID("guia").equals(mc.player.getUUID());
    }

    private static boolean sali(CompoundTag e) {
        Minecraft mc = Minecraft.getInstance();
        for (Tag y : e.getList("miembros", Tag.TAG_COMPOUND)) {
            CompoundTag m = (CompoundTag) y;
            if (m.getUUID("id").equals(mc.player.getUUID())) {
                return Nbt.b(m, "salio");
            }
        }
        return false;
    }

    /** ¿Soy el guía de mi equipo? */
    public static boolean soyElGuia() {
        return soyGuia(miEquipo());
    }

    /** Para la prueba automática: el nombre de alguien de mi equipo que no soy yo (null si no hay). */
    public static String companero() {
        CompoundTag e = miEquipo();
        Minecraft mc = Minecraft.getInstance();
        if (e == null || mc.player == null) {
            return null;
        }
        for (Tag y : e.getList("miembros", Tag.TAG_COMPOUND)) {
            UUID id = ((CompoundTag) y).getUUID("id");
            CompoundTag p = id.equals(mc.player.getUUID()) ? null : EventoCliente.pieza(id);
            if (p != null) {
                return Nbt.s(p, "nombre");
            }
        }
        return null;
    }

    /** ¿Tengo la venda puesta? (camino en el laberinto, en juego, sin haber salido) */
    public static boolean vendado() {
        CompoundTag e = miEquipo();
        Minecraft mc = Minecraft.getInstance();
        return e != null && "JUEGO".equals(fase()) && !soyGuia(e) && !sali(e) && mc.player != null && EventoCliente.vivo(EventoCliente.yo())
                || SentidosCliente.vendado();
    }

    /** Cada cuántos ticks llega el pulso. */
    private static int periodo() {
        int seg = Nbt.i(estado, "pulso");
        return Math.max(20, (seg <= 0 ? 4 : seg) * 20);
    }

    /** Segundos desde que empezó el último pulso (todos los vendados lo ven al mismo tiempo: sale del reloj del mundo). */
    private static float desdePulso(float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 99F;
        }
        return (Math.floorMod(mc.level.getGameTime(), periodo()) + pt) / 20F;
    }

    /** Qué tan fuerte está el pulso (0..1): sube rápido y se apaga suave. */
    private static float pulso(float pt) {
        float f = desdePulso(pt);
        return f < 0.12F ? f / 0.12F : (float) Math.exp(-(f - 0.12F) * 3.2F);
    }

    private static void niebla(ViewportEvent.RenderFog e) {
        venda = Mth.clamp(venda + (vendado() ? 0.05F : -0.05F), 0, 1);
        if (venda <= 0.01F) {
            return;
        }
        float k = pulso((float) e.getPartialTick());
        e.setNearPlaneDistance(Mth.lerp(venda, e.getNearPlaneDistance(), 0F));
        e.setFarPlaneDistance(Mth.lerp(venda, e.getFarPlaneDistance(), Mth.lerp(k, 1.3F, 7F)));
        e.setCanceled(true);
    }

    private static void colorNiebla(ViewportEvent.ComputeFogColor e) {
        if (venda <= 0.01F) {
            return;
        }
        // en el pulso la niebla se vuelve blanca: lo cercano se ve y lo lejano brilla
        float k = pulso((float) e.getPartialTick()) * 0.72F;
        e.setRed(Mth.lerp(venda, e.getRed(), k));
        e.setGreen(Mth.lerp(venda, e.getGreen(), k));
        e.setBlue(Mth.lerp(venda, e.getBlue(), k + 0.02F));
    }

    /**
     * La venda: todo negro (debajo del resto del HUD, para que se sigan viendo el reloj y los avisos) y, en cada pulso, el negro se abre un
     * instante y un anillo blanco sale del centro de la vista.
     */
    private static void vendaNegra(net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre e) {
        if (venda <= 0.01F || !e.getOverlay().id().equals(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.VIGNETTE.id())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.gui.GuiGraphics g = e.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        float pt = mc.getFrameTime();
        float k = pulso(pt), f = desdePulso(pt);
        if (mc.level != null && vendado()) {
            long n = mc.level.getGameTime() / periodo();
            if (n != ultimoPulso && f < 0.5F) {
                ultimoPulso = n;
                mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.55F, 0.5F));
            }
        }
        g.fill(0, 0, w, h, Ui.alpha(0xFF000000, venda * (0.985F - 0.8F * k)));
        // un destello blanco suave al principio del pulso
        if (f < 0.35F) {
            g.fill(0, 0, w, h, Ui.alpha(0xFFE8ECF4, venda * 0.16F * (1 - f / 0.35F)));
        }
        // el anillo que se abre desde el centro
        if (f < 0.9F) {
            float r = f / 0.9F * (float) Math.hypot(w, h) * 0.55F;
            int col = Ui.alpha(0xFFF4F6FF, venda * 0.55F * (1 - f / 0.9F));
            int puntos = Math.max(24, (int) (r * 0.9F));
            for (int i = 0; i < puntos; i++) {
                double a = i * Math.PI * 2 / puntos;
                int x = w / 2 + (int) (Math.cos(a) * r), y = h / 2 + (int) (Math.sin(a) * r);
                g.fill(x - 1, y - 1, x + 1, y + 1, col);
            }
        }
    }

    /** Las trampas (rojo) y la salida (verde), solo para el guía de ese laberinto o para quien no juega (organizador, fantasmas). */
    private static void dibujar(RenderLevelStageEvent ev) {
        if (ev.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !"JUEGO".equals(fase())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        CompoundTag mio = miEquipo();
        boolean veTodo = mio == null || !EventoCliente.vivo(EventoCliente.yo());
        Vec3 cam = ev.getCamera().getPosition();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        PoseStack ps = ev.getPoseStack();
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        RenderType rt = RenderType.entityTranslucent(com.axeliscore.plat.client.Draw.BLANCO);
        VertexConsumer vc = buf.getBuffer(rt);
        float brillo = (float) (0.45 + 0.2 * Math.sin(System.currentTimeMillis() / 250.0));
        for (Tag x : estado.getList("equipos", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) x;
            if (!veTodo && !(soyGuia(mio) && Nbt.i(e, "k") == Nbt.i(mio, "k"))) {
                continue;
            }
            int ox = Nbt.i(e, "ox"), oy = Nbt.i(e, "oy"), oz = Nbt.i(e, "oz");
            for (Tag y : estado.getList("trampas", Tag.TAG_COMPOUND)) {
                CompoundTag tr = (CompoundTag) y;
                cuadro(vc, ps, ox + Nbt.i(tr, "dx"), oy + 1.03F, oz + Nbt.i(tr, "dz"), 0xE03030, brillo);
            }
            // las baldosas intermitentes: naranja tenue apagadas (parpadean justo antes de prender), rojo fuerte encendidas
            int ciclo = Math.max(1, Nbt.i(estado, "ciclo")), enc = Nbt.i(estado, "encendida");
            long gt = mc.level == null ? 0 : mc.level.getGameTime();
            for (Tag y : estado.getList("inter", Tag.TAG_COMPOUND)) {
                CompoundTag b = (CompoundTag) y;
                int fase = Math.floorMod(gt + Nbt.i(b, "des"), ciclo);
                boolean on = fase < enc;
                boolean aviso = !on && fase > ciclo - 20 && (gt / 3) % 2 == 0;
                cuadro(vc, ps, ox + Nbt.i(b, "dx"), oy + 1.04F, oz + Nbt.i(b, "dz"), on ? 0xFF2020 : 0xF08A20, on ? 0.8F : aviso ? 0.6F : 0.28F);
            }
            cuadro(vc, ps, ox + com.coleccionista.mapa.Laberintos.LADO - 2, oy + 1.03F, oz, 0x40E060, 0.5F);
        }
        ps.popPose();
        buf.endBatch(rt);
    }

    private static void cuadro(VertexConsumer vc, PoseStack ps, float x, float y, float z, int rgb, float a) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255, al = (int) (a * 255);
        int luz = 0xF000F0;
        vc.vertex(m, x + 0.05F, y, z + 0.05F).color(r, g, b, al).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x + 0.05F, y, z + 0.95F).color(r, g, b, al).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x + 0.95F, y, z + 0.95F).color(r, g, b, al).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
        vc.vertex(m, x + 0.95F, y, z + 0.05F).color(r, g, b, al).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(n, 0, 1, 0).endVertex();
    }

    private static void hud(G g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!activa() || !EventoHud.visible()) {
            return;
        }
        Font f = Ui.font();
        CompoundTag e = miEquipo();
        String linea = null, linea2 = null;
        if (e != null) {
            int salieron = 0, total = 0;
            for (Tag y : e.getList("miembros", Tag.TAG_COMPOUND)) {
                CompoundTag m = (CompoundTag) y;
                if (e.hasUUID("guia") && m.getUUID("id").equals(e.getUUID("guia"))) {
                    continue;
                }
                total++;
                if (Nbt.b(m, "salio")) {
                    salieron++;
                }
            }
            String guia = "?";
            if (e.hasUUID("guia")) {
                UUID gid = e.getUUID("guia");
                CompoundTag p = EventoCliente.pieza(gid);
                guia = p == null ? "?" : "Pieza " + String.format("%02d", Nbt.i(p, "numero")) + " · " + Nbt.s(p, "nombre");
            }
            if ("JUEGO".equals(fase())) {
                if (soyGuia(e)) {
                    int cada = Nbt.i(estado, "cambio");
                    linea2 = Component.translatable(MOD + ".cie.hud.colores").getString() + (cada > 0 ? Component.translatable(MOD
                            + ".cie.hud.mueven", cada - (int) ((estado.getLong("t") / 20) % cada)).getString() : "");
                }
                linea = soyGuia(e) ? Component.translatable(MOD + ".cie.hud.guia", Nbt.i(e, "k"), salieron, total).getString()
                        : sali(e) ? Component.translatable(MOD + ".cie.hud.saliste", salieron, total).getString()
                        : Component.translatable(MOD + ".cie.hud.vendado", guia).getString();
            } else if ("LISTA".equals(fase()) || "HABLANDO".equals(fase())) {
                linea = soyGuia(e) ? Component.translatable(MOD + ".cie.hud.eres_guia").getString()
                        : Component.translatable(MOD + ".cie.hud.tu_guia", guia).getString();
            }
        }
        if (linea != null) {
            int lw = f.width(linea) + 16, y = EventoHud.lineaY(w, lw);
            g.fillGradient(w / 2 - lw / 2, y, w / 2 + lw / 2, y + 13, 0xB0140C0E, 0x90080506);
            g.drawCenteredString(f, linea, w / 2, y + 3, 0xFFD8D0C8);
            if (linea2 != null) {
                int y2 = EventoHud.debajo(w, f.width(linea2) + 8, y + 16);
                g.drawCenteredString(f, linea2, w / 2, y2 + 2, 0xFFB8A898);
            }
        }
    }
}
