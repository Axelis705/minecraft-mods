package com.navidad.nieve;

import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * El lanzanieves: mantén el clic derecho y, después de que los cañones toman vuelo, dispara bolas de nieve en ráfaga (cada una empuja,
 * escarcha y alenta a los mobs; a los blazes les duele de verdad). Si apuntas a nieve cercana con el tanque puesto, la aspira y llena
 * el tanque. Agachado, carga una bola gigante (8 tiros) que revienta en una nevada.
 */
public class Lanzanieves extends Item {
    /** Ticks que tardan los cañones en tomar vuelo, y cada cuánto dispara. */
    public static final int ARRANQUE = 6, CADA = 3, CARGA = 20, COSTO_GIGANTE = 8;

    public Lanzanieves(Properties p) {
        super(p);
    }

    @Override
    public int getUseDuration(ItemStack st) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack st) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        p.startUsingItem(h);
        return InteractionResultHolder.consume(p.getItemInHand(h));
    }

    /** La nieve que tienes enfrente y cerca (para aspirarla), o null. */
    public static BlockPos nieveEnfrente(Player p) {
        Vec3 ojo = p.getEyePosition();
        Vec3 fin = ojo.add(p.getLookAngle().scale(4.5));
        BlockHitResult hit = p.level().clip(new ClipContext(ojo, fin, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockState s = p.level().getBlockState(hit.getBlockPos());
        return s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW) ? hit.getBlockPos() : null;
    }

    /** ¿Está aspirando? (con el tanque puesto, que no esté lleno, y nieve enfrente) Lo usan los dos lados para el dibujo. */
    public static boolean aspirando(Player p) {
        ItemStack t = Municion.tanque(p);
        return !t.isEmpty() && !p.isShiftKeyDown() && TanqueNieve.suelta(t) < TanqueNieve.MAX_SUELTA && nieveEnfrente(p) != null;
    }

    /** ¿En este tick de uso sale un tiro? (igual en los dos lados: el cliente lo usa para el culatazo) */
    public static boolean tocaTiro(int t) {
        return t >= ARRANQUE && (t - ARRANQUE) % CADA == 0;
    }

    @Override
    public void onUseTick(Level l, LivingEntity e, ItemStack st, int queda) {
        if (!(e instanceof Player p) || l.isClientSide || !(l instanceof ServerLevel sl)) {
            return;
        }
        int t = getUseDuration(st) - queda;
        Vec3 boca = boca(p);
        if (p.isShiftKeyDown()) {
            // cargando la bola gigante
            if (t == 1) {
                l.playSound(null, p.blockPosition(), ModRegistry.LANZANIEVES_CARGA.get(), SoundSource.PLAYERS, 0.8F, 1F);
            }
            if (t == CARGA) {
                l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, 1.8F);
            }
            if (t % 3 == 0) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, boca.x, boca.y, boca.z, 2 + Math.min(t, CARGA) / 5, 0.12, 0.12, 0.12, 0.01);
            }
            return;
        }
        if (aspirando(p)) {
            if (t % 3 == 0) {
                aspirar(sl, p, boca);
            }
            return;
        }
        if (t == 1) {
            l.playSound(null, p.blockPosition(), ModRegistry.LANZANIEVES_GIRA.get(), SoundSource.PLAYERS, 0.7F, 1F);
        }
        if (!tocaTiro(t)) {
            return;
        }
        if (!Municion.gastar(p, 1)) {
            if ((t - ARRANQUE) % (CADA * 5) == 0) {
                l.playSound(null, p.blockPosition(), ModRegistry.LANZANIEVES_VACIO.get(), SoundSource.PLAYERS, 0.8F, 1F);
                p.displayClientMessage(Component.translatable(Municion.tanque(p).isEmpty() ? "navidad.lanzanieves.sin_nieve"
                        : "navidad.lanzanieves.tanque_vacio").withStyle(ChatFormatting.AQUA), true);
            }
            return;
        }
        BolaNieve b = new BolaNieve(l, p);
        b.setPos(boca.x, boca.y - 0.1, boca.z);
        b.shootFromRotation(p, p.getXRot(), p.getYRot(), 0F, 2.3F, 3.0F);
        l.addFreshEntity(b);
        l.playSound(null, p.blockPosition(), ModRegistry.LANZANIEVES_DISPARA.get(), SoundSource.PLAYERS, 0.55F, 0.9F + l.random.nextFloat() * 0.25F);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, boca.x, boca.y, boca.z, 3, 0.05, 0.05, 0.05, 0.04);
        if (p instanceof ServerPlayer sp) {
            disparos++;
        }
    }

    /** Para la prueba con fotos. */
    public static int disparos, aspiradas, gigantes;

    /** Dónde está la boca de los cañones (adelante, un poco a la derecha y abajo de los ojos). */
    public static Vec3 boca(Player p) {
        Vec3 mira = p.getLookAngle();
        Vec3 der = new Vec3(-mira.z, 0, mira.x).normalize();
        return p.getEyePosition().add(mira.scale(0.9)).add(der.scale(0.28)).add(0, -0.22, 0);
    }

    private static void aspirar(ServerLevel l, Player p, Vec3 boca) {
        BlockPos q = nieveEnfrente(p);
        if (q == null) {
            return;
        }
        BlockState s = l.getBlockState(q);
        int gana;
        if (s.is(Blocks.SNOW)) {
            int capas = s.getValue(SnowLayerBlock.LAYERS);
            if (capas > 1) {
                l.setBlock(q, s.setValue(SnowLayerBlock.LAYERS, capas - 1), 3);
            } else {
                l.removeBlock(q, false);
            }
            gana = 1;
        } else if (s.is(Blocks.SNOW_BLOCK)) {
            l.setBlock(q, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 7), 3);
            gana = 1;
        } else {
            l.removeBlock(q, false);
            gana = 4;
        }
        TanqueNieve.meter(Municion.tanque(p), gana);
        aspiradas++;
        // los copos vuelan de la nieve a la boca del lanzanieves
        Vec3 de = Vec3.atCenterOf(q).add(0, 0.3, 0);
        Vec3 d = boca.subtract(de);
        for (int i = 0; i < 4; i++) {
            Vec3 o = de.add((l.random.nextDouble() - 0.5) * 0.6, (l.random.nextDouble() - 0.5) * 0.3, (l.random.nextDouble() - 0.5) * 0.6);
            l.sendParticles(ParticleTypes.SNOWFLAKE, o.x, o.y, o.z, 0, d.x, d.y, d.z, 0.16);
        }
        l.playSound(null, q, SoundEvents.SNOW_BREAK, SoundSource.PLAYERS, 0.5F, 1.6F);
        if (l.random.nextInt(3) == 0) {
            l.playSound(null, p.blockPosition(), ModRegistry.LANZANIEVES_ASPIRA.get(), SoundSource.PLAYERS, 0.6F, 0.9F + l.random.nextFloat() * 0.2F);
        }
    }

    @Override
    public void releaseUsing(ItemStack st, Level l, LivingEntity e, int queda) {
        if (!(e instanceof Player p) || l.isClientSide) {
            return;
        }
        int t = getUseDuration(st) - queda;
        if (!p.isShiftKeyDown() || t < CARGA) {
            return;
        }
        if (!Municion.gastar(p, COSTO_GIGANTE)) {
            l.playSound(null, p.blockPosition(), ModRegistry.LANZANIEVES_VACIO.get(), SoundSource.PLAYERS, 0.9F, 0.85F);
            p.displayClientMessage(Component.translatable("navidad.lanzanieves.falta_gigante", COSTO_GIGANTE).withStyle(ChatFormatting.AQUA),
                    true);
            return;
        }
        Vec3 boca = boca(p);
        BolaGigante b = new BolaGigante(l, p);
        b.setPos(boca.x, boca.y - 0.2, boca.z);
        b.shootFromRotation(p, p.getXRot(), p.getYRot(), 0F, 1.5F, 0.5F);
        l.addFreshEntity(b);
        l.playSound(null, p.blockPosition(), ModRegistry.LANZANIEVES_BOLA.get(), SoundSource.PLAYERS, 1F, 1F);
        p.getCooldowns().addCooldown(this, 14);
        gigantes++;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack a, ItemStack b, boolean slotChanged) {
        return slotChanged || a.getItem() != b.getItem();
    }

    @Override
    public void appendHoverText(ItemStack st, Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.translatable("navidad.lanzanieves.ayuda1").withStyle(ChatFormatting.GRAY));
        t.add(Component.translatable("navidad.lanzanieves.ayuda2").withStyle(ChatFormatting.GRAY));
        t.add(Component.translatable("navidad.lanzanieves.ayuda3").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c) {
        c.accept(com.navidad.client.LanzanievesObjeto.extension());
    }
}
