package com.celular.client;

import com.celular.PhoneUse;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Cómo se ve en tercera persona quien usa el celular: con las dos manos al frente, mirando la pantalla (como escribiendo), o con la
 * linterna, el brazo estirado hacia donde mira. Lo ven todos porque sale del número de pantalla que lleva el celular en la mano.
 */
public final class PhonePoses {
    private PhonePoses() {
    }

    private static HumanoidModel.ArmPose text, flash;

    private static HumanoidModel.ArmPose text() {
        if (text == null) {
            //#if MC >= 26.1
            //$$ text = HumanoidModel.ArmPose.create("CELULAR_TEXT", true, true, (model, who, arm) -> texting(model, arm));
            //#else
            text = HumanoidModel.ArmPose.create("CELULAR_TEXT", true, (model, who, arm) -> texting(model, arm));
            //#endif
        }
        return text;
    }

    private static HumanoidModel.ArmPose flash() {
        if (flash == null) {
            //#if MC >= 26.1
            //$$ flash = HumanoidModel.ArmPose.create("CELULAR_FLASH", false, false, (model, who, arm) -> pointing(model, arm));
            //#else
            flash = HumanoidModel.ArmPose.create("CELULAR_FLASH", false, (model, who, arm) -> pointing(model, arm));
            //#endif
        }
        return flash;
    }

    /** Las dos manos juntas delante del pecho, siguiendo un poco la cabeza. */
    private static void texting(HumanoidModel<?> m, HumanoidArm arm) {
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart main = right ? m.rightArm : m.leftArm;
        ModelPart other = right ? m.leftArm : m.rightArm;
        float look = Math.max(-0.6F, Math.min(0.6F, m.head.xRot));
        main.xRot = -1.05F + look * 0.45F;
        main.yRot = (right ? -0.32F : 0.32F) + m.head.yRot * 0.25F;
        main.zRot = 0;
        other.xRot = -0.95F + look * 0.45F;
        other.yRot = (right ? 0.55F : -0.55F) + m.head.yRot * 0.25F;
        other.zRot = 0;
    }

    /** El brazo estirado hacia donde mira (como quien alumbra con la linterna del celular). */
    private static void pointing(HumanoidModel<?> m, HumanoidArm arm) {
        ModelPart a = arm == HumanoidArm.RIGHT ? m.rightArm : m.leftArm;
        a.xRot = m.head.xRot - (float) (Math.PI / 2) + 0.05F;
        a.yRot = m.head.yRot + (arm == HumanoidArm.RIGHT ? -0.06F : 0.06F);
        a.zRot = 0;
    }

    /** Para PhoneItem: la pose según la pantalla que lleva el celular. */
    public static final IClientItemExtensions EXT = new IClientItemExtensions() {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity who, InteractionHand hand, ItemStack stack) {
            int sc = PhoneUse.get(stack);
            if (sc == PhoneUse.OFF) {
                return null;
            }
            return sc == PhoneUse.FLASH ? flash() : text();
        }
    };
}
