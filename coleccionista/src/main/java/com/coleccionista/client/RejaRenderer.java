package com.coleccionista.client;

import com.coleccionista.bloque.deco.DecoReja;
import com.coleccionista.bloque.deco.RejaBE;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.client.model.data.ModelData;

/**
 * (Cliente) Dibuja la reja de la celda con su animación: primero sale un poco hacia el pasillo (deja los barrotes) y luego se corre de lado
 * por su riel hasta quedar frente a los barrotes de junto. Dibuja los modelos de la reja cerrada (las dos mitades) movidos.
 */
public class RejaRenderer implements BlockEntityRenderer<RejaBE> {
    private static final float SALE = 2.5F / 16F, CORRE = 15F / 16F;
    private final BlockRenderDispatcher bloques;
    private final RandomSource azar = RandomSource.create();

    public RejaRenderer(BlockEntityRendererProvider.Context c) {
        this.bloques = c.getBlockRenderDispatcher();
    }

    private static float suave(float t) {
        t = Mth.clamp(t, 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    @Override
    public void render(RejaBE be, float pt, PoseStack ps, MultiBufferSource buf, int luz, int overlay) {
        BlockState s = be.getBlockState();
        Level l = be.getLevel();
        if (!(s.getBlock() instanceof DecoReja) || l == null) {
            return;
        }
        float t = be.avance(pt);
        float fuera = suave(t / 0.25F) * SALE, lado = suave((t - 0.15F) / 0.85F) * CORRE;
        Direction f = s.getValue(DoorBlock.FACING);
        Direction corre = s.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT ? f.getClockWise() : f.getCounterClockWise();
        Direction sale = f.getOpposite();
        BlockState abajo = s.setValue(DoorBlock.OPEN, false).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState arriba = abajo.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        BlockPos p = be.getBlockPos();
        RenderType rt = RenderType.cutout();
        VertexConsumer vc = buf.getBuffer(rt);
        ps.pushPose();
        ps.translate(corre.getStepX() * lado + sale.getStepX() * fuera, 0, corre.getStepZ() * lado + sale.getStepZ() * fuera);
        bloques.getModelRenderer().tesselateBlock(l, bloques.getBlockModel(abajo), abajo, p, ps, vc, false, azar, abajo.getSeed(p), overlay,
                ModelData.EMPTY, rt);
        ps.translate(0, 1, 0);
        bloques.getModelRenderer().tesselateBlock(l, bloques.getBlockModel(arriba), arriba, p.above(), ps, vc, false, azar, arriba.getSeed(p.above()),
                overlay, ModelData.EMPTY, rt);
        ps.popPose();
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
