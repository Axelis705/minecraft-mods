package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mundosdeaxelis.arma.ArmaItem;
import com.mundosdeaxelis.arma.TipoArma;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Animaciones de ataque de las armas nuevas (etapa 281).
 *
 * <ul>
 * <li>Primera persona: cada tipo golpea a su manera (la espada hace un tajo en diagonal, el hacha y el martillo levantan el
 * arma y la bajan con fuerza, la lanza se echa atrás y embiste, las dagas apuñalan rápido).</li>
 * <li>Tercera persona: el hacha de guerra, el martillo y la lanza se llevan con las dos manos y el otro brazo acompaña el golpe.</li>
 * </ul>
 */
public final class ArmaAnim implements IClientItemExtensions {
    public static final ArmaAnim INSTANCE = new ArmaAnim();
    /** Para las fotos de la prueba: si es 0 o más, todas las armas se dibujan en ese punto del golpe (0..1). */
    public static volatile float fijo = -1.0F;

    private ArmaAnim() {
    }

    // ------------------------------------------------------------------ primera persona

    /** Fotogramas clave: {tiempo, x, y, z, giro X, giro Y, giro Z} (x hacia el lado del brazo, grados). */
    private static final float[][] ESPADA = {
            {0.00F, 0, 0, 0, 0, 0, 0},
            {0.12F, 0.06F, 0.10F, 0.06F, 20, -20, -30},
            {0.42F, -0.42F, -0.18F, -0.22F, -50, 50, 65},
            {1.00F, 0, 0, 0, 0, 0, 0}};
    private static final float[][] HACHA = {
            {0.00F, 0, 0, 0, 0, 0, 0},
            {0.20F, 0.02F, 0.22F, 0.12F, 55, -5, -10},
            {0.50F, -0.12F, -0.32F, -0.25F, -85, 10, 12},
            {1.00F, 0, 0, 0, 0, 0, 0}};
    private static final float[][] MARTILLO = {
            {0.00F, 0, 0, 0, 0, 0, 0},
            {0.25F, 0.00F, 0.32F, 0.20F, 70, -8, -15},
            {0.55F, -0.15F, -0.45F, -0.30F, -100, 12, 18},
            {1.00F, 0, 0, 0, 0, 0, 0}};
    private static final float[][] LANZA = {
            {0.00F, 0, 0, 0, 0, 0, 0},
            {0.15F, 0.04F, -0.04F, 0.22F, -55, -8, 0},
            {0.40F, -0.12F, 0.02F, -0.65F, -62, 8, 0},
            {1.00F, 0, 0, 0, 0, 0, 0}};
    private static final float[][] DAGAS = {
            {0.00F, 0, 0, 0, 0, 0, 0},
            {0.10F, 0.03F, 0.02F, 0.12F, -25, -10, 0},
            {0.30F, -0.18F, 0.04F, -0.45F, -70, 18, -10},
            {1.00F, 0, 0, 0, 0, 0, 0}};

    private static float[][] claves(TipoArma t) {
        return switch (t) {
            case ESPADA -> ESPADA;
            case HACHA -> HACHA;
            case MARTILLO -> MARTILLO;
            case LANZA -> LANZA;
            case DAGAS -> DAGAS;
        };
    }

    /** Posición del arma en el punto {@code t} (0..1) del golpe: {x, y, z, giro X, giro Y, giro Z}. */
    public static float[] pose(TipoArma tipo, float t) {
        float[][] k = claves(tipo);
        t = Mth.clamp(t, 0.0F, 1.0F);
        int s = 0;
        while (s < k.length - 2 && t > k[s + 1][0]) {
            s++;
        }
        float[] a = k[s], b = k[s + 1];
        float u = (t - a[0]) / Math.max(1.0E-4F, b[0] - a[0]);
        u = Mth.clamp(u, 0.0F, 1.0F);
        u = u * u * (3.0F - 2.0F * u);
        float[] out = new float[6];
        for (int j = 0; j < 6; j++) {
            out[j] = Mth.lerp(u, a[j + 1], b[j + 1]);
        }
        return out;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick,
                                           float equipProcess, float swingProcess) {
        if (!(stack.getItem() instanceof ArmaItem a) || player.isUsingItem()) {
            return false;
        }
        float t = fijo >= 0.0F ? fijo : swingProcess;
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        // la misma posición de reposo que Minecraft (con la bajada al cambiar de objeto o mientras se recarga el golpe)
        pose.translate(i * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
        float[] k = pose(a.tipo, t);
        pose.translate(i * k[0], k[1], k[2]);
        pose.mulPose(Axis.YP.rotationDegrees(i * k[4]));
        pose.mulPose(Axis.XP.rotationDegrees(k[3]));
        pose.mulPose(Axis.ZP.rotationDegrees(i * k[5]));
        return true;
    }

    // ------------------------------------------------------------------ tercera persona

    private static HumanoidModel.ArmPose dosManos;

    /** Crea la pose nueva al arrancar el cliente (las poses son un enum ampliable: no se deben crear a mitad de partida). */
    public static void init() {
        dosManos();
    }

    /** Agarre a dos manos (hacha de guerra, martillo y lanza): el arma cruzada delante del pecho; el otro brazo sigue el golpe. */
    private static HumanoidModel.ArmPose dosManos() {
        if (dosManos == null) {
            dosManos = HumanoidModel.ArmPose.create("MUNDOS_DE_AXELIS_DOS_MANOS", true, (model, entity, arm) -> {
                ModelPart main = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                ModelPart off = arm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
                float s = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
                float mira = Mth.clamp(model.head.xRot, -0.6F, 0.6F) * 0.3F;
                main.xRot = -0.70F + mira;
                main.yRot = s * -0.15F;
                main.zRot = 0.0F;
                off.xRot = -0.95F + mira;
                off.yRot = s * 0.62F;
                off.zRot = 0.0F;
                // el golpe de Minecraft solo mueve el brazo del arma: aquí se le suma lo mismo al otro para que no suelte el mango
                float t = model.attackTime;
                if (t > 0.0F) {
                    float f = 1.0F - t;
                    f *= f;
                    f *= f;
                    f = 1.0F - f;
                    float f1 = Mth.sin(f * (float) Math.PI);
                    float f2 = Mth.sin(t * (float) Math.PI) * -(model.head.xRot - 0.7F) * 0.75F;
                    off.xRot -= f1 * 1.2F + f2;
                    off.zRot += s * Mth.sin(t * (float) Math.PI) * 0.3F;
                }
            });
        }
        return dosManos;
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (hand != InteractionHand.MAIN_HAND || !(stack.getItem() instanceof ArmaItem a) || entity.isUsingItem()) {
            return null;
        }
        return switch (a.tipo) {
            case HACHA, MARTILLO, LANZA -> dosManos();
            default -> null;
        };
    }
}
