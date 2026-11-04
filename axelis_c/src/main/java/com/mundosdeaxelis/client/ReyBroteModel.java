package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.ReyBrote;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Modelo del Rey Brote. Se dibuja en tres capas, como el slime de Minecraft: el cuerpo de dentro (cara, arbolito, flores y
 * musgo), la gelatina de fuera (translúcida) y la semilla que brilla en el centro. Medidas en "unidades de slime": el
 * renderizador lo agranda cuatro veces.
 */
public class ReyBroteModel extends EntityModel<ReyBrote> {
    public static final ModelLayerLocation BODY =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "rey_brote"), "body");
    public static final ModelLayerLocation SHELL =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "rey_brote"), "shell");
    public static final ModelLayerLocation SEED =
            new ModelLayerLocation(new ResourceLocation(MundosDeAxelis.MODID, "rey_brote"), "seed");

    private final ModelPart root;
    private final ModelPart leaves;

    public ReyBroteModel(ModelPart root) {
        this.root = root;
        this.leaves = root.hasChild("leaves") ? root.getChild("leaves") : null;
    }

    public static LayerDefinition bodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        // cuerpo interior con la cara
        r.addOrReplaceChild("inner", CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, 17.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);
        // tronco del arbolito (sale por arriba de la gelatina)
        r.addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(0, 36).addBox(-0.5F, 9.0F, -0.5F, 1.0F, 8.5F, 1.0F), PartPose.ZERO);
        r.addOrReplaceChild("branch", CubeListBuilder.create().texOffs(0, 36).addBox(0.0F, 11.5F, -0.5F, 2.5F, 0.8F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5F));
        // copa de hojas: dos bloques que se mecen un poco
        PartDefinition leaves = r.addOrReplaceChild("leaves", CubeListBuilder.create().texOffs(20, 36)
                .addBox(-2.5F, 5.5F, -2.5F, 5.0F, 3.5F, 5.0F).texOffs(20, 36)
                .addBox(-1.5F, 3.8F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.ZERO);
        leaves.addOrReplaceChild("leaf_side", CubeListBuilder.create().texOffs(20, 36)
                .addBox(1.5F, 7.5F, -1.5F, 2.5F, 2.0F, 3.0F), PartPose.ZERO);
        // flores sobre la gelatina
        r.addOrReplaceChild("flower_a", CubeListBuilder.create().texOffs(0, 48).addBox(2.7F, 14.9F, 1.6F, 1.0F, 1.0F, 1.0F), PartPose.ZERO);
        r.addOrReplaceChild("flower_b", CubeListBuilder.create().texOffs(8, 48).addBox(-3.5F, 14.9F, -2.6F, 1.0F, 1.0F, 1.0F), PartPose.ZERO);
        r.addOrReplaceChild("flower_c", CubeListBuilder.create().texOffs(16, 48).addBox(-0.5F, 15.0F, 3.0F, 1.0F, 1.0F, 1.0F), PartPose.ZERO);
        // manchas de musgo pegadas por fuera
        r.addOrReplaceChild("moss_r", CubeListBuilder.create().texOffs(30, 48).addBox(3.9F, 17.0F, -2.0F, 0.3F, 3.0F, 4.0F), PartPose.ZERO);
        r.addOrReplaceChild("moss_t", CubeListBuilder.create().texOffs(30, 56).addBox(-3.0F, 15.85F, 0.5F, 4.0F, 0.3F, 3.0F), PartPose.ZERO);
        r.addOrReplaceChild("moss_l", CubeListBuilder.create().texOffs(44, 48).addBox(-4.2F, 19.0F, -3.0F, 0.3F, 3.0F, 3.0F), PartPose.ZERO);
        // corona dorada alrededor del tronco: aro, puntas y una esmeralda al frente
        CubeListBuilder crown = CubeListBuilder.create().texOffs(48, 0)
                .addBox(-2.2F, 14.6F, -2.2F, 4.4F, 1.4F, 0.6F)
                .addBox(-2.2F, 14.6F, 1.6F, 4.4F, 1.4F, 0.6F)
                .addBox(-2.2F, 14.6F, -1.6F, 0.6F, 1.4F, 3.2F)
                .addBox(1.6F, 14.6F, -1.6F, 0.6F, 1.4F, 3.2F);
        for (float[] sp : new float[][]{{-2.2F, -2.2F}, {1.4F, -2.2F}, {-2.2F, 1.4F}, {1.4F, 1.4F}, {-0.4F, -2.2F}, {-0.4F, 1.6F}, {-2.2F, -0.4F}, {1.6F, -0.4F}}) {
            crown.addBox(sp[0], 13.0F, sp[1], 0.8F, 1.7F, 0.8F);
        }
        r.addOrReplaceChild("crown", crown, PartPose.ZERO);
        r.addOrReplaceChild("crown_gem", CubeListBuilder.create().texOffs(48, 16).addBox(-0.6F, 14.8F, -2.5F, 1.2F, 1.0F, 0.4F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition shellLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 16.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition seedLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("seed", CubeListBuilder.create().texOffs(0, 28).addBox(-1.0F, 19.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ReyBrote entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (this.leaves != null) {
            this.leaves.zRot = Mth.sin(ageInTicks * 0.08F) * 0.04F;
            this.leaves.xRot = Mth.cos(ageInTicks * 0.06F) * 0.03F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
