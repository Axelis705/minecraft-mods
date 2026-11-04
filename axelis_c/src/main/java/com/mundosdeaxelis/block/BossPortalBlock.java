package com.mundosdeaxelis.block;

import com.mundosdeaxelis.world.BossTravel;
import com.mundosdeaxelis.world.WorldTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Piedra de Portal: al usarla abre la ficha del jefe; con «Comenzar» (se activa a los 5 s) lleva a ti y a los jugadores que tengas a menos de 8 bloques a la dungeon del jefe de su etapa.
 * Para entrar a una etapa hay que haber vencido a los jefes de las anteriores (la etapa 1 está siempre abierta).
 */
public class BossPortalBlock extends Block {
    public final int stage;
    /** Jugador -> instante (tick) en que abrió la ficha del jefe; «Comenzar» solo vale pasados 4,5 s desde entonces. */
    private static final java.util.Map<java.util.UUID, Long> OPENED = new java.util.HashMap<>();

    public BossPortalBlock(Properties props, int stage) {
        super(props);
        this.stage = stage;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        if (BossTravel.inArena(level)) {
            return InteractionResult.CONSUME;
        }
        if (!unlocked(sp)) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.portal_locked").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        // abre la ficha del jefe; el botón «Comenzar» (activo a los 5 s) manda BossStartPacket
        OPENED.put(sp.getUUID(), level.getGameTime());
        com.mundosdeaxelis.network.ModNetwork.sendTo(sp, com.mundosdeaxelis.world.BossInfo.brief(stage, pos, gather(level, pos, sp), sp));
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.2F);
        return InteractionResult.CONSUME;
    }

    private boolean unlocked(ServerPlayer sp) {
        return WorldTier.get(sp.server).level() >= stage - 1;
    }

    /** Jugadores a menos de 8 bloques de la piedra (siempre incluye a quien la usa). */
    private static List<ServerPlayer> gather(Level level, BlockPos pos, ServerPlayer sp) {
        List<ServerPlayer> group = new ArrayList<>();
        for (ServerPlayer p : ((net.minecraft.server.level.ServerLevel) level).players()) {
            if (p.isAlive() && !p.isSpectator() && p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0) {
                group.add(p);
            }
        }
        if (!group.contains(sp)) {
            group.add(sp);
        }
        return group;
    }

    /** Lo llama el botón «Comenzar» de la ficha: el servidor lo revisa todo otra vez y lleva al grupo a la dungeon. */
    public static void tryStart(ServerPlayer sp, BlockPos pos) {
        Level level = sp.level();
        if (!(level.getBlockState(pos).getBlock() instanceof BossPortalBlock portal) || BossTravel.inArena(level)) {
            return;
        }
        if (sp.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 100.0 || !portal.unlocked(sp)) {
            return;
        }
        Long opened = OPENED.get(sp.getUUID());
        long now = level.getGameTime();
        if (opened == null || now < opened || now - opened < 90L || now - opened > 6000L) {
            return;
        }
        OPENED.remove(sp.getUUID());
        List<ServerPlayer> group = gather(level, pos, sp);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.PORTAL_TRIGGER, net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.6F);
        BossTravel.enter(sp.server, group, portal.stage);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.getX() + random.nextDouble(), pos.getY() + 1.0 + random.nextDouble() * 0.4,
                    pos.getZ() + random.nextDouble(), 0.0, 0.03, 0.0);
        }
    }
}
