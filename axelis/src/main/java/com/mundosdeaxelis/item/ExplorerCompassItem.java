package com.mundosdeaxelis.item;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Brújula del explorador: señala la construcción más cercana del tipo elegido.
 * Agáchate + clic derecho para cambiar de tipo; clic derecho para buscar.
 */
public class ExplorerCompassItem extends Item {
    private static final String[] IDS = {"village", "treasure", "stronghold", "mansion", "monument", "mineshaft", "portal"};
    @SuppressWarnings("unchecked")
    private static final TagKey<Structure>[] TAGS = new TagKey[]{
            StructureTags.VILLAGE, StructureTags.ON_TREASURE_MAPS, StructureTags.EYE_OF_ENDER_LOCATED,
            StructureTags.ON_WOODLAND_EXPLORER_MAPS, StructureTags.ON_OCEAN_EXPLORER_MAPS, StructureTags.MINESHAFT,
            StructureTags.RUINED_PORTAL
    };
    private static final int[] COLORS = {0x8CFF9C, 0xFFD36B, 0xC9A8FF, 0xFF9C8C, 0x7CE6FF, 0xD8C8A8, 0xFF8AE0};
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    public ExplorerCompassItem(Properties properties) {
        super(properties);
    }

    private static int mode(ItemStack stack) {
        return stack.hasTag() ? Mth.clamp(stack.getTag().getInt("Mode"), 0, IDS.length - 1) : 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel sl = sp.serverLevel();
        int mode = mode(stack);
        if (sp.isShiftKeyDown()) {
            mode = (mode + 1) % IDS.length;
            stack.getOrCreateTag().putInt("Mode", mode);
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.compass_mode",
                    Component.translatable("compass.mundos_de_axelis." + IDS[mode])), true);
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 0.8F, 1.4F);
            sl.sendParticles(Fx.spark(COLORS[mode], 1.0F), sp.getX(), sp.getY() + 1.2, sp.getZ(), 6, 0.4, 0.3, 0.4, 0.0);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        sp.getCooldowns().addCooldown(this, 60);
        BlockPos target = sl.findNearestMapStructure(TAGS[mode], sp.blockPosition(), 64, false);
        Component name = Component.translatable("compass.mundos_de_axelis." + IDS[mode]);
        if (target == null) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.compass_none", name), true);
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
            return InteractionResultHolder.fail(stack);
        }
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.5F);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.8F, 1.3F);
        Fx.screen(sp, ScreenFxPacket.STARS, COLORS[mode], 40);
        final int color = COLORS[mode];
        // Durante unos segundos: flecha en pantalla, y un rastro de luz que va hacia el destino.
        for (int i = 0; i <= 16; i++) {
            final int k = i;
            Scheduler.runLater(Math.max(1, i * 20), () -> {
                if (!sp.isAlive() || sp.hasDisconnected()) {
                    return;
                }
                Vec3 d = new Vec3(target.getX() + 0.5 - sp.getX(), 0, target.getZ() + 0.5 - sp.getZ());
                double dist = d.length();
                float want = (float) (Math.toDegrees(Math.atan2(-d.x, d.z)));
                float rel = Mth.wrapDegrees(want - sp.getYRot());
                int idx = Math.floorMod(Math.round(rel / 45.0F), 8);
                sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.compass_found",
                        name, (int) dist, ARROWS[idx]), true);
            });
        }
        for (int i = 1; i <= 200; i += 2) {
            final int k = i;
            Scheduler.runLater(i, () -> {
                if (!sp.isAlive() || sp.hasDisconnected()) {
                    return;
                }
                Vec3 d = new Vec3(target.getX() + 0.5 - sp.getX(), 0, target.getZ() + 0.5 - sp.getZ());
                double len = d.length();
                if (len < 3.0) {
                    return;
                }
                Vec3 dir = d.scale(1.0 / len);
                ServerLevel w = sp.serverLevel();
                for (int s = 2; s <= 9; s++) {
                    double wave = Math.sin(k * 0.25 - s * 0.7);
                    double px = sp.getX() + dir.x * s - dir.z * wave * 0.25;
                    double pz = sp.getZ() + dir.z * s + dir.x * wave * 0.25;
                    double py = sp.getY() + 0.3 + (s % 2) * 0.1;
                    if (((k / 2) + s) % 3 == 0) {
                        w.sendParticles(Fx.dust(color, 0.7F), px, py, pz, 1, 0, 0, 0, 0);
                    }
                }
                w.sendParticles(Fx.spark(color, 0.8F), sp.getX() + dir.x * 4.5, sp.getY() + 0.5, sp.getZ() + dir.z * 4.5, 1, 0.2, 0.1, 0.2, 0.0);
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "explorer_compass", 2);
        tooltip.add(Component.translatable("tooltip.mundos_de_axelis.explorer_compass.mode",
                Component.translatable("compass.mundos_de_axelis." + IDS[mode(stack)])).withStyle(net.minecraft.ChatFormatting.AQUA));
    }
}
