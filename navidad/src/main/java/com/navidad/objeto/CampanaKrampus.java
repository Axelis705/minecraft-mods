package com.navidad.objeto;

import com.axeliscore.item.CoreItem;
import com.navidad.entidad.Krampus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** La campana negra del Krampus: tocarla de noche lo llama (se gasta). De día solo suena. */
public class CampanaKrampus extends CoreItem {
    public CampanaKrampus(Properties p) {
        super(p);
    }

    @Override
    protected void tooltip(ItemStack stack, Consumer<Component> add) {
        add.accept(Component.translatable("navidad.campana.ayuda").withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    protected boolean onUse(Level level, Player p, InteractionHand hand) {
        if (!(level instanceof ServerLevel sl)) {
            return true;
        }
        long t = sl.getDayTime() % 24000L;
        boolean noche = t > 13000 && t < 23000;
        sl.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.BELL_BLOCK, net.minecraft.sounds.SoundSource.PLAYERS, 2F, 0.5F);
        if (!noche) {
            p.displayClientMessage(Component.translatable("navidad.campana.dia"), true);
            p.getCooldowns().addCooldown(this, 40);
            return true;
        }
        Vec3 frente = Vec3.directionFromRotation(0, p.getYRot()).scale(9);
        Vec3 donde = p.position().add(frente);
        int y = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(donde.x),
                (int) Math.floor(donde.z));
        Krampus.llamar(sl, new Vec3(donde.x, Math.max(y, p.getY()), donde.z), p, true);
        if (!p.getAbilities().instabuild) {
            p.getItemInHand(hand).shrink(1);
        }
        p.getCooldowns().addCooldown(this, 200);
        return true;
    }
}
