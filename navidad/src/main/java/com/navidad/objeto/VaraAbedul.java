package com.navidad.objeto;

import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La vara de abedul del Krampus (de su botín): un manojo de varitas con listón rojo, ligera y rápida. Cada tercer golpe seguido al mismo
 * (en menos de 2 segundos) es un <b>varazo</b>: chasquido, daño extra, lo empuja y lo deja lento un momento.
 */
public class VaraAbedul extends SwordItem {
    /** Quién, a quién le pegó la última vez, cuántas seguidas y cuándo. */
    private static final Map<UUID, long[]> RACHA = new HashMap<>();
    public static int varazos;

    public VaraAbedul(Properties p) {
        super(Tiers.WOOD, 3, -1.6F, p.durability(300));
    }

    @Override
    public boolean hurtEnemy(ItemStack st, LivingEntity t, LivingEntity quien) {
        boolean r = super.hurtEnemy(st, t, quien);
        if (quien.level() instanceof ServerLevel l && quien instanceof Player p) {
            long ahora = l.getGameTime();
            long[] e = RACHA.computeIfAbsent(p.getUUID(), u -> new long[] {-1, 0, 0});
            if (e[0] == t.getId() && ahora - e[2] < 40) {
                e[1]++;
            } else {
                e[0] = t.getId();
                e[1] = 1;
            }
            e[2] = ahora;
            if (e[1] >= 3) {
                e[1] = 0;
                varazos++;
                t.invulnerableTime = 0;
                t.hurt(l.damageSources().playerAttack(p), 4F);
                double dx = t.getX() - p.getX(), dz = t.getZ() - p.getZ(), d = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
                t.push(dx / d * 0.7, 0.25, dz / d * 0.7);
                t.hurtMarked = true;
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                l.playSound(null, t.blockPosition(), ModRegistry.KRAMPUS_LATIGO.get(), SoundSource.PLAYERS, 1.2F, 1.1F);
                l.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 14, 0.3, 0.3, 0.3, 0.2);
                p.displayClientMessage(Component.translatable("item.navidad.vara_abedul.varazo").withStyle(ChatFormatting.GOLD), true);
            }
        }
        return r;
    }

    @Override
    public void appendHoverText(ItemStack st, Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("item.navidad.vara_abedul.ayuda").withStyle(ChatFormatting.GRAY));
    }
}
