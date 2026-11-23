package com.coleccionista.client;

import com.axeliscore.client.PersonaRenderer;
import com.axeliscore.plat.Nbt;
import com.coleccionista.bot.PiezaBot;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * (Cliente) El jugador de prueba se ve como un jugador con piel por defecto; si está eliminado, como el fantasmita (y solo para quien
 * puede ver fantasmas, igual que con los jugadores de verdad).
 */
public class BotRenderer extends PersonaRenderer<PiezaBot> {
    public BotRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, b -> DefaultPlayerSkin.getDefaultSkin(b.getUUID()));
    }

    @Override
    public void render(PiezaBot b, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        if (b.fantasma()) {
            if (FantasmaRender.veoFantasmas()) {
                CompoundTag pz = EventoCliente.pieza(b.getUUID());
                String et = pz == null ? null : String.format("%02d", Nbt.i(pz, "numero")) + " · " + Nbt.s(pz, "nombre");
                FantasmaRender.dibujar(b, pt, Mth.rotLerp(pt, b.yHeadRotO, b.yHeadRot), ps, buf, et, 1.05F);
            }
            return;
        }
        super.render(b, yaw, pt, ps, buf, light);
    }
}
