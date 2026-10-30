package com.kensei.client;

import com.kensei.net.DuelStatePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderNameTagEvent;

/**
 * Cómo se distingue quién es quién en un duelo por equipos: los aliados llevan el nombre en verde con la etiqueta [ALIADO] y una flecha
 * verde sobre la cabeza que se ve a cualquier distancia (también tras las paredes); los rivales llevan el nombre en rojo.
 */
public final class TeamMarks {
    private static final ResourceLocation ARROW = new ResourceLocation("kensei", "textures/gui/ally_marker.png");

    private TeamMarks() {
    }

    /** Colorea el nombre que flota sobre cada peleador del duelo. */
    public static void onNameTag(RenderNameTagEvent e) {
        int rel = ClientDuel.relation(e.getEntity());
        if (rel == 0) {
            return;
        }
        Component orig = e.getContent();
        if (rel == 1) {
            e.setContent(Component.translatable("kensei.team.ally").append(" ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    .append(orig.copy().withStyle(ChatFormatting.GREEN)));
        } else {
            e.setContent(orig.copy().withStyle(ChatFormatting.RED));
        }
    }

    /** La flecha verde y el cartel "ALIADO" sobre la cabeza de cada compañero de equipo (se dibuja en la pantalla, siempre nítida). */
    public static void render(GuiGraphics g, int w, int h, float partial) {
        Minecraft mc = Minecraft.getInstance();
        DuelStatePacket.F me = ClientDuel.self();
        if (me == null || mc.level == null || mc.player == null || !ClientLock.haveMatrices) {
            return;
        }
        for (DuelStatePacket.F f : ClientDuel.state().fighters) {
            if (f.team != me.team || f.state != 0 || f.id.equals(mc.player.getUUID()) || f.entityId < 0) {
                continue;
            }
            Entity en = mc.level.getEntity(f.entityId);
            if (!(en instanceof LivingEntity t) || !t.isAlive()) {
                continue;
            }
            Vec3 base = t.getPosition(partial);
            float[] hp = ClientLock.project(new Vec3(base.x, base.y + t.getBbHeight() + 0.75, base.z), w, h);
            if (hp == null) {
                continue;
            }
            double dist = mc.gameRenderer.getMainCamera().getPosition().distanceTo(base);
            int size = Math.round((float) Mth.clamp(170.0 / Math.max(1.0, dist), 14.0, 26.0));
            float bob = 2.0F * (float) Math.sin(System.currentTimeMillis() / 260.0);
            float x = Mth.clamp(hp[0], size, w - size);
            float y = Mth.clamp(hp[1] + bob, size + 12, h - size);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(ARROW, Math.round(x) - size / 2, Math.round(y) - size / 2, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
            Component label = Component.translatable("kensei.team.ally").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
            g.pose().pushPose();
            g.pose().translate(x, y - size / 2.0F - 9.0F, 0.0F);
            g.pose().scale(0.8F, 0.8F, 1.0F);
            g.drawCenteredString(mc.font, label, 0, 0, 0xFF6BFF8A);
            g.pose().popPose();
            RenderSystem.disableBlend();
        }
    }
}
