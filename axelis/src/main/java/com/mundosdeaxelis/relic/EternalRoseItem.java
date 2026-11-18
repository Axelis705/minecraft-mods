package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Rosa Eterna: cura suavemente a quienes estén cerca; activo: floración que sana a todos. */
public class EternalRoseItem extends RelicItem {
    public EternalRoseItem() {
        super("eternal_rose", 0xFF6F9C, 20 * 45, Rarity.EPIC);
    }

    @Override
    protected int interval() {
        return 40;
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        AABB box = new AABB(sp.blockPosition()).inflate(10.0);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false, true));
        }
        for (TamableAnimal t : sl.getEntitiesOfClass(TamableAnimal.class, box, t -> t.isTame() && t.isOwnedBy(sp))) {
            t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false, true));
        }
        sl.sendParticles(Fx.petal(0, 1.0F), sp.getX(), sp.getY() + 2.0, sp.getZ(), 1, 0.8, 0.1, 0.8, 0.0);
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.1F);
        Fx.sound(sl, x, y, z, SoundEvents.FLOWERING_AZALEA_PLACE, SoundSource.PLAYERS, 1.0F, 0.9F);
        for (int i = 1; i <= 10; i++) {
            final int r = i;
            Scheduler.runLater(i, () -> {
                Fx.ring(sl, Fx.petal(0, 1.3F), x, y + 0.3, z, r, 10 + r * 4, r * 0.3);
                Fx.ring(sl, Fx.heart(0xFF9CB8, 0.9F), x, y + 1.0, z, r * 0.7, 4 + r, -r * 0.4);
            });
        }
        int n = 0;
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(x - 12, y - 6, z - 12, x + 12, y + 6, z + 12))) {
            p.heal(16.0F);
            p.getFoodData().eat(6, 0.6F);
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 30, 1));
            Fx.screen(p, ScreenFxPacket.PETALS, 0xFFA8CC, 80);
            Fx.screen(p, ScreenFxPacket.HEARTS, 0xFF9CB8, 60);
            n++;
        }
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.rose_bloom", n), true);
        return true;
    }
}
