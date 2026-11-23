package com.pescacoleccion.deco;

import com.axeliscore.block.CoreBlock;
import com.axeliscore.plat.Plat;
import com.axeliscore.ui.InfoBlock;
import com.axeliscore.ui.InfoCard;
import com.axeliscore.ui.Rareza;
import com.pescacoleccion.PescaColeccion;
import com.pescacoleccion.pesca.PezItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Locale;

/**
 * Acuario: un tanque de vidrio con agua y arena. Clic derecho con un pez lo suelta adentro (hasta {@link AcuarioBE#MAX}) y nada; agachado
 * con la mano vacía se saca el último. Al mirarlo dice quién vive ahí.
 */
public class AcuarioBlock extends CoreBlock implements EntityBlock, InfoBlock {
    public AcuarioBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AcuarioBE(pos, state);
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AcuarioBE be)) {
            return InteractionResult.PASS;
        }
        ItemStack mano = player.getItemInHand(hand);
        if (mano.getItem() instanceof PezItem) {
            if (be.peces.size() >= AcuarioBE.MAX) {
                if (!level.isClientSide()) {
                    Plat.msg(player, Component.translatable(PescaColeccion.MODID + ".acuario.lleno", AcuarioBE.MAX), true);
                }
                return Plat.ok(level.isClientSide());
            }
            if (!level.isClientSide()) {
                soltar(be, player, mano);
            }
            return Plat.ok(level.isClientSide());
        }
        if (mano.isEmpty() && player.isShiftKeyDown() && !be.peces.isEmpty()) {
            if (!level.isClientSide()) {
                Plat.give(player, be.peces.remove(be.peces.size() - 1));
                be.changed();
                level.playSound(null, pos, SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.BLOCKS, 1F, 1F);
            }
            return Plat.ok(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    /** (Servidor) Suelta en el acuario el pez que trae en la mano (si cabe). */
    public static boolean soltar(AcuarioBE be, Player player, ItemStack mano) {
        if (be.peces.size() >= AcuarioBE.MAX || !(mano.getItem() instanceof PezItem)) {
            return false;
        }
        ItemStack uno = mano.copy();
        uno.setCount(1);
        be.peces.add(uno);
        if (!player.getAbilities().instabuild) {
            mano.shrink(1);
        }
        be.changed();
        be.getLevel().playSound(null, be.getBlockPos(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.BLOCKS, 0.8F, 1.2F);
        return true;
    }

    @Override
    protected void onPlayerBreak(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AcuarioBE be) {
            for (ItemStack st : be.peces) {
                Block.popResource(level, pos, st);
            }
            be.peces.clear();
        }
    }

    @Override
    public void axInfo(Level level, BlockPos pos, BlockState state, InfoCard card, boolean detail) {
        if (!(level.getBlockEntity(pos) instanceof AcuarioBE be) || be.peces.isEmpty()) {
            card.text(Component.translatable(PescaColeccion.MODID + ".acuario.vacio"));
            return;
        }
        card.subtitle(Component.translatable(PescaColeccion.MODID + ".acuario.viven", be.peces.size(), AcuarioBE.MAX));
        int mejor = 0;
        for (ItemStack st : be.peces) {
            if (st.getItem() instanceof PezItem pz && pz.especie() != null) {
                mejor = Math.max(mejor, pz.especie().rareza());
                float cm = PezItem.cm(st);
                card.pair(st.getHoverName(), Component.literal(cm > 0 ? String.format(Locale.ROOT, cm >= 100 ? "%.0f cm" : "%.1f cm", cm) : ""));
            }
        }
        card.rareza(Rareza.of(mejor));
        card.hint(Component.translatable(PescaColeccion.MODID + ".acuario.sacar"));
    }
}
