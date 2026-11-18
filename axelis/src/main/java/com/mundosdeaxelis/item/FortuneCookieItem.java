package com.mundosdeaxelis.item;

import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.Names;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Galleta de la suerte: al comerla trae un mensaje escondido y un pequeño regalo de suerte. */
public class FortuneCookieItem extends Item {
    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(3).saturationMod(0.5F).alwaysEat().fast().build();

    private static final String[] MESSAGES = {
            "Hoy alguien piensa en ti y sonríe sin saber por qué.",
            "Lo mejor de esta aventura siempre ha sido hacerla juntos.",
            Names.ODETTE + " y " + Names.AXELIS + ": el mejor equipo de todos los mundos.",
            "Un abrazo a tiempo vale más que cualquier diamante.",
            "Vas a encontrar algo bonito donde menos lo esperas.",
            "Si te caes, levántate: el mundo todavía tiene muchas sorpresas guardadas.",
            "Hoy es un buen día para hacer reír a alguien.",
            "La suerte te sigue... aunque a veces se tropieza.",
            "Esta galleta dice que mereces un descanso y algo dulce.",
            "Alguien cerca de ti tiene un secreto muy lindo para ti.",
            "Cuidado con los creepers, pero más con los que te hacen reír demasiado.",
            "El tesoro más grande no está en la mina, está en quien te acompaña.",
            "Pide un deseo. Ya lo estaba escuchando el universo.",
            "Tu próxima idea loca va a funcionar. O va a ser muy divertida.",
            "Un café, una canción y buena compañía: ya tienes la receta de un gran día.",
            "Hoy el destino huele a pan recién horneado.",
            "No hay cueva tan oscura que no se arregle con una antorcha y un amigo.",
            "Las estrellas se pusieron de acuerdo y hoy brillan solo para ti.",
            "Dicen que quien come esta galleta tiene el doble de suerte. Lo comprobé: es verdad.",
            "Siempre hay un camino de vuelta a casa. Y a veces es un abrazo.",
            "El mejor plan de hoy: nada de prisa y muchas risas.",
            "Esta galleta está tan orgullosa de ti como yo.",
            "Algo pequeño y brillante viene en camino. Atenta al suelo.",
            "Eres más valiente de lo que crees, y más linda también.",
            "Guarda este momento: dentro de poco será un recuerdo favorito."
    };

    private static final MobEffect[] LUCKY = {
            MobEffects.LUCK, MobEffects.REGENERATION, MobEffects.MOVEMENT_SPEED, MobEffects.NIGHT_VISION,
            MobEffects.DAMAGE_RESISTANCE, MobEffects.JUMP, MobEffects.DIG_SPEED
    };

    public FortuneCookieItem(Properties properties) {
        super(properties.food(FOOD));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (level instanceof ServerLevel sl && entity instanceof net.minecraft.server.level.ServerPlayer player) {
            String msg = MESSAGES[level.random.nextInt(MESSAGES.length)];
            player.sendSystemMessage(Component.literal("Dentro de la galleta decía:")
                    .withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.literal("“" + msg + "”")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            MobEffect effect = LUCKY[level.random.nextInt(LUCKY.length)];
            int seconds = effect == MobEffects.LUCK ? 180 : 45;
            player.addEffect(new MobEffectInstance(effect, 20 * seconds, 0, false, true, true));
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();
            sl.sendParticles(Fx.heart(0xFF8FB8, 0.9F), x, y + 0.4, z, 6, 0.4, 0.5, 0.4, 0.02);
            sl.sendParticles(Fx.spark(0xFFE9A0, 1.0F), x, y + 0.5, z, 10, 0.4, 0.5, 0.4, 0.03);
            sl.sendParticles(Fx.dust(0xFFD36B, 1.2F), x, y + 0.6, z, 10, 0.3, 0.5, 0.3, 0.02);
            Fx.screen(player, ScreenFxPacket.STARS, 0xFFE08A, 34);
            Fx.confetti(sl, x, y, z, 14, 1.2);
            Fx.sound(sl, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.5F);
            Fx.sound(sl, x, y, z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "fortune_cookie", 2);
    }
}
