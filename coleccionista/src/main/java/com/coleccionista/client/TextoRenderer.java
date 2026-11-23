package com.coleccionista.client;

import com.coleccionista.bloque.deco.DecoBlock;
import com.coleccionista.bloque.deco.TextoBE;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * (Cliente) Escribe el texto de las placas, los letreros y los rayones de las paredes encima de su bloque. Los rayones se ven raspados
 * (una sombra oscura y el trazo claro encima); el resto, como pintura.
 */
public class TextoRenderer implements BlockEntityRenderer<TextoBE> {
    private final Font font;

    public TextoRenderer(BlockEntityRendererProvider.Context c) {
        this.font = c.getFont();
    }

    @Override
    public void render(TextoBE be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BlockState s = be.getBlockState();
        String[] ls = be.lineas();
        if (!(s.getBlock() instanceof DecoBlock db) || ls.length == 0) {
            return;
        }
        String estilo = db.def().str("texto");
        Direction f = s.hasProperty(DecoBlock.FACING) ? s.getValue(DecoBlock.FACING) : Direction.NORTH;
        float rot = switch (f) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
        // dónde queda el frente (en píxeles del modelo, que mira al norte) y qué tan grande va la letra
        float z, esc;
        switch (estilo) {
            case "placa" -> {
                z = 15.3F;
                esc = 0.0072F;
            }
            case "letrero" -> {
                z = 14.9F;
                esc = 0.0105F;
            }
            default -> {
                z = 15.9F;
                esc = 0.011F;
            }
        }
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-rot));
        ps.translate(0, 0, z / 16F - 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(180));
        ps.scale(esc, -esc, esc);
        float lh = 10;
        float y0 = -ls.length * lh / 2F + 1;
        int color = 0xFF000000 | be.color();
        for (int i = 0; i < ls.length; i++) {
            String t = ls[i];
            float x = -font.width(t) / 2F, y = y0 + i * lh;
            if (estilo.equals("rayon")) {
                font.drawInBatch(t, x + 0.7F, y + 0.7F, 0xFF1C1914, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, light);
                ps.translate(0, 0, -0.4F);
                font.drawInBatch(t, x, y, color, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, light);
                ps.translate(0, 0, 0.4F);
            } else {
                font.drawInBatch(t, x, y, color, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0, light);
            }
        }
        ps.popPose();
    }
}
