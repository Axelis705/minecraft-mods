package com.coleccionista.client;

import com.axeliscore.data.client.ClientData;
import com.coleccionista.evento.Pistas;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

/**
 * (Cliente) Dibuja los números escondidos de las celdas encima de las cosas: rayados en el metal, con gis en la pared, con plumón en
 * el colchón o el espejo. Vienen del servidor ({@link Pistas}); solo se dibujan de cerca (hay que buscarlos).
 */
public final class InscripcionesCliente {
    /** Hasta dónde se ven (bloques). */
    private static final double LEJOS = 14;
    private static final List<Ins> LISTA = new ArrayList<>();

    record Ins(Vec3 pos, int plano, float ang, float esc, int estilo, float giro, String texto, int celda, String lugar) {
        /** Un poco enfrente de la superficie: de ahí se toma la luz. */
        BlockPos luz() {
            Vec3 n = switch (plano) {
                case Pistas.PISO -> new Vec3(0, 0.3, 0);
                case Pistas.TECHO -> new Vec3(0, -0.3, 0);
                default -> {
                    double r = Math.toRadians(ang);
                    yield new Vec3(Math.sin(r) * 0.3, 0, Math.cos(r) * 0.3);
                }
            };
            return BlockPos.containing(pos.add(n));
        }
    }

    private InscripcionesCliente() {
    }

    public static void init() {
        ClientData.listen(Pistas.KEY, InscripcionesCliente::leer);
        MinecraftForge.EVENT_BUS.addListener(InscripcionesCliente::dibujar);
    }

    private static void leer(CompoundTag t) {
        LISTA.clear();
        if (t == null || !t.getBoolean("reveladas")) {
            return;
        }
        ListTag l = t.getList("l", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompound(i);
            LISTA.add(new Ins(new Vec3(c.getDouble("x"), c.getDouble("y"), c.getDouble("z")), c.getInt("p"), c.getFloat("a"), c.getFloat("s"),
                    c.getInt("e"), c.getFloat("g"), c.getString("t"), c.getInt("celda"), c.getString("lugar")));
        }
    }

    /** Para la prueba de cliente: cuántas hay y cuántas de la celda {@code n}. */
    public static int cuantas(int celda) {
        int k = 0;
        for (Ins i : LISTA) {
            if (celda <= 0 || i.celda() == celda) {
                k++;
            }
        }
        return k;
    }

    public static List<Ins> de(int celda) {
        List<Ins> out = new ArrayList<>();
        for (Ins i : LISTA) {
            if (i.celda() == celda) {
                out.add(i);
            }
        }
        return out;
    }

    private static void dibujar(RenderLevelStageEvent e) {
        // (si cambió de mundo, lo de antes ya no cuenta)
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || LISTA.isEmpty() || !ClientData.has(Pistas.KEY)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Vec3 cam = e.getCamera().getPosition();
        Font font = mc.font;
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        PoseStack ps = e.getPoseStack();
        boolean alguno = false;
        for (Ins i : LISTA) {
            if (i.pos().distanceToSqr(cam) > LEJOS * LEJOS) {
                continue;
            }
            alguno = true;
            int packed = LevelRenderer.getLightColor(mc.level, i.luz());
            // un mínimo para que se distinga con la luz baja del ala (pero a oscuras total no se ve)
            int bl = Math.max(LightTexture.block(packed), 3), sl = LightTexture.sky(packed);
            int luz = LightTexture.pack(bl, sl);
            ps.pushPose();
            ps.translate(i.pos().x - cam.x, i.pos().y - cam.y, i.pos().z - cam.z);
            switch (i.plano()) {
                case Pistas.PISO -> {
                    ps.mulPose(Axis.YP.rotationDegrees(i.ang()));
                    ps.mulPose(Axis.XP.rotationDegrees(-90));
                }
                case Pistas.TECHO -> {
                    ps.mulPose(Axis.YP.rotationDegrees(i.ang()));
                    ps.mulPose(Axis.XP.rotationDegrees(90));
                }
                default -> ps.mulPose(Axis.YP.rotationDegrees(i.ang()));
            }
            // chueco, como escrito a mano
            ps.mulPose(Axis.ZP.rotationDegrees(i.giro()));
            ps.scale(i.esc(), -i.esc(), i.esc());
            String t = i.texto();
            float x = -font.width(t) / 2F, y = -4;
            switch (i.estilo()) {
                case Pistas.RAYADO -> {
                    // raspado: el surco oscuro y el metal claro encima, corrido medio pixel
                    font.drawInBatch(t, x + 0.6F, y + 0.6F, 0xFF16140F, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, luz);
                    ps.translate(0, 0, -0.35F);
                    font.drawInBatch(t, x, y, 0xFFCFC8B8, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, luz);
                }
                case Pistas.TIZA -> {
                    font.drawInBatch(t, x + 0.4F, y + 0.3F, 0x88D8D2C2, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, luz);
                    ps.translate(0, 0, -0.3F);
                    font.drawInBatch(t, x, y, 0xFFE9E4D6, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, luz);
                }
                case Pistas.ROJO -> font.drawInBatch(t, x, y, 0xFFA3161B, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, luz);
                default -> font.drawInBatch(t, x, y, 0xFF1A1918, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, luz);
            }
            ps.popPose();
        }
        if (alguno) {
            buf.endBatch();
        }
    }
}
