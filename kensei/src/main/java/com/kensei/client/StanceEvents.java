package com.kensei.client;

import com.kensei.Kensei;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Baja el cuerpo entero un poco cuando el espadachín está en postura de combate (las piernas abiertas quedan apoyadas en el suelo). */
@Mod.EventBusSubscriber(modid = Kensei.MODID, value = Dist.CLIENT)
public final class StanceEvents {
    private static float applied = 0.0F;

    private StanceEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPre(RenderLivingEvent.Pre<?, ?> e) {
        applied = 0.0F;
        LivingEntity le = e.getEntity();
        float d = KatanaPoses.dropFor(le);
        if (d <= 0.01F) {
            return;
        }
        applied = d / 16.0F;
        PoseStack ps = e.getPoseStack();
        ps.translate(0.0F, -applied, 0.0F);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPost(RenderLivingEvent.Post<?, ?> e) {
        if (applied == 0.0F) {
            return;
        }
        e.getPoseStack().translate(0.0F, applied, 0.0F);
        applied = 0.0F;
    }
}
