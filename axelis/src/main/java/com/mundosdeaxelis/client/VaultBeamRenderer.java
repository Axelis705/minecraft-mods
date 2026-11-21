package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mundosdeaxelis.block.VaultChestBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Etapa 269: el haz de luz del botín (como en Diablo o Borderlands). Sobre cada cofre del santuario que el jugador todavía no
 * abrió sube un rayo fino del color de su rareza (Común gris … Celestial celeste), más alto cuanto más raro. Desaparece al
 * abrirlo; los cofres sellados para ese jugador (de un tier que aún no le toca) no lo tienen.
 */
public class VaultBeamRenderer implements BlockEntityRenderer<VaultChestBlockEntity> {
    public VaultBeamRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(VaultChestBlockEntity be, float pt, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || be.getLevel() == null || be.openedBy(mc.player.getUUID())) {
            return;
        }
        int st = be.stage();
        int ps = com.mundosdeaxelis.crate.CrateForge.stage(TableClient.archive);
        if (st > 1 && st > ps + 1 && !mc.player.isCreative()) {
            return; // sellado: sin luz (solo el humo de alma del bloque)
        }
        int c = VaultChestBlockEntity.rarity(st).color;
        float[] col = {((c >> 16) & 255) / 255.0F, ((c >> 8) & 255) / 255.0F, (c & 255) / 255.0F};
        BeaconRenderer.renderBeaconBeam(pose, buf, BeaconRenderer.BEAM_LOCATION, pt, 1.0F, be.getLevel().getGameTime(), 1, 4 + st, col, 0.07F, 0.13F);
    }

    @Override
    public boolean shouldRenderOffScreen(VaultChestBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
