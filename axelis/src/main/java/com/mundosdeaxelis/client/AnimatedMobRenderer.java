package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.Mob;

/**
 * Base de los dibujos de criaturas con "movimiento con vida" ({@link Juice}): inclinación, ladeo, rebote al andar, retroceso
 * al recibir golpes, estirar y aplastar, aparición y muerte con estilo. Cada criatura dice cómo con su {@link CreatureStyle}.
 * Las subclases ponen su tamaño en {@link #baseScale} (no en {@code scale}).
 */
public abstract class AnimatedMobRenderer<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
    protected AnimatedMobRenderer(EntityRendererProvider.Context context, M model, float shadow) {
        super(context, model, shadow);
    }

    protected CreatureStyle style(T e) {
        return CreatureStyles.of(e);
    }

    @Override
    protected float getFlipDegrees(T e) {
        return 0.0F; // la muerte la hace Juice
    }

    @Override
    protected void setupRotations(T e, PoseStack pose, float age, float yaw, float pt) {
        super.setupRotations(e, pose, age, yaw, pt);
        Juice.body(pose, e, style(e), pt);
    }

    @Override
    protected final void scale(T e, PoseStack pose, float pt) {
        baseScale(e, pose, pt);
        Juice.scale(pose, e, style(e), pt);
    }

    /** El tamaño propio de la criatura (antes del estirar/aplastar). */
    protected void baseScale(T e, PoseStack pose, float pt) {
    }
}
