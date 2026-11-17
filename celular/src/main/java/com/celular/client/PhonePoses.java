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
 * Cómo se ve en tercera persona quien usa el celular. Lo ven todos porque sale del número de pantalla que lleva el celular en la mano:
 * <ul>
 *     <li>cualquier app: las dos manos juntas delante del pecho, el celular mirando a los ojos (y un vaivén suave, como escribiendo);</li>
 *     <li>cámara: las dos manos arriba, delante de la cara, como tomando una foto;</li>
 *     <li>llamada: el celular en la oreja;</li>
 *     <li>linterna: el brazo estirado hacia donde mira.</li>
 * </ul>
 * Cada pose se define por hacia dónde apunta el brazo y hacia dónde mira la palma (y por lo tanto la pantalla), y de ahí se sacan los
 * ángulos del brazo. La forma de tener el celular en la mano está en el modelo (models/item/phone3d/base.json); las dos cosas se
 * calcularon juntas con una simulación (la pantalla queda mirando a los ojos al escribir y al tomar foto).
 */
public final class PhonePoses {
    private PhonePoses() {
    }

    /** Dónde están los ojos en el modelo del jugador (píxeles; y hacia abajo, el frente es -z). */
    private static final float EYE_Y = -4.5F, EYE_Z = -4F;

    private static HumanoidModel.ArmPose text, flash, ear, photo;

    //#if MC >= 26.1
    //$$ private static HumanoidModel.ArmPose make(String name, boolean both, java.util.function.BiConsumer<HumanoidModel<?>, HumanoidArm> fn) {
    //$$     return HumanoidModel.ArmPose.create(name, both, both, (model, who, arm) -> fn.accept(model, arm));
    //$$ }
    //#else
    private static HumanoidModel.ArmPose make(String name, boolean both, java.util.function.BiConsumer<HumanoidModel<?>, HumanoidArm> fn) {
        return HumanoidModel.ArmPose.create(name, both, (model, who, arm) -> fn.accept(model, arm));
    }
    //#endif

    private static HumanoidModel.ArmPose text() {
        if (text == null) {
            text = make("CELULAR_TEXT", true, PhonePoses::texting);
        }
        return text;
    }

    private static HumanoidModel.ArmPose flash() {
        if (flash == null) {
            flash = make("CELULAR_FLASH", false, PhonePoses::pointing);
        }
        return flash;
    }

    private static HumanoidModel.ArmPose ear() {
        if (ear == null) {
            ear = make("CELULAR_EAR", false, PhonePoses::atEar);
        }
        return ear;
    }

    private static HumanoidModel.ArmPose photo() {
        if (photo == null) {
            photo = make("CELULAR_PHOTO", true, PhonePoses::takingPhoto);
        }
        return photo;
    }

    // ------------------------------------------------------------------ matemática del brazo

    /**
     * Pone el brazo apuntando a (dx, dy, dz) con la palma hacia (px, py, pz) (en el modelo: y hacia abajo, el frente es -z). El brazo
     * derecho tiene la palma hacia +x con el brazo quieto y el izquierdo hacia -x. Los ángulos son los de ModelPart (orden Z·Y·X).
     */
    static void aim(ModelPart arm, boolean right, float dx, float dy, float dz, float px, float py, float pz) {
        float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dl < 1e-5F) {
            return;
        }
        dx /= dl;
        dy /= dl;
        dz /= dl;
        // la palma, perpendicular al brazo
        float dot = px * dx + py * dy + pz * dz;
        px -= dot * dx;
        py -= dot * dy;
        pz -= dot * dz;
        float pl = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (pl < 1e-4F) {
            px = right ? 1 : -1;
            py = 0;
            pz = 0;
            dot = px * dx;
            px -= dot * dx;
            py -= dot * dy;
            pz -= dot * dz;
            pl = (float) Math.sqrt(px * px + py * py + pz * pz);
            if (pl < 1e-4F) {
                return;
            }
        }
        float s = right ? 1 : -1;
        // columnas de la rotación: x local = palma (con signo), y local = brazo, z = x × y
        float c0x = s * px / pl, c0y = s * py / pl, c0z = s * pz / pl;
        float c2z = c0x * dy - c0y * dx;
        arm.yRot = (float) Math.asin(Math.max(-1F, Math.min(1F, -c0z)));
        arm.xRot = (float) Math.atan2(dz, c2z);
        arm.zRot = (float) Math.atan2(c0y, c0x);
        if (Float.isNaN(arm.xRot) || Float.isNaN(arm.yRot) || Float.isNaN(arm.zRot)) {
            arm.xRot = arm.yRot = arm.zRot = 0;
        }
    }

    /** Hacia dónde mira la cabeza (en el modelo). */
    private static float[] look(HumanoidModel<?> m) {
        float p = m.head.xRot, y = m.head.yRot;
        return new float[]{(float) (-Math.sin(y) * Math.cos(p)), (float) Math.sin(p), (float) (-Math.cos(y) * Math.cos(p))};
    }

    private static float time() {
        return (System.currentTimeMillis() % 1_000_000L) / 1000F;
    }

    // ------------------------------------------------------------------ las poses

    /** Las dos manos juntas delante del pecho; el celular mira a los ojos. Un vaivén suave, como escribiendo. */
    private static void texting(HumanoidModel<?> m, HumanoidArm arm) {
        float t = time();
        float pitch = Math.max(-0.7F, Math.min(0.7F, m.head.xRot));
        for (int k = 0; k < 2; k++) {
            boolean right = (arm == HumanoidArm.RIGHT) == (k == 0);
            ModelPart a = right ? m.rightArm : m.leftArm;
            float tap = (float) Math.sin(t * (k == 0 ? 7.0 : 6.1) + k) * 0.25F;           // los pulgares
            float bx = right ? -2.2F : 2.2F;
            float by = 5.5F + 3F * (float) Math.sin(pitch) + (float) Math.sin(t * 1.3) * 0.2F + tap;
            float bz = -7.5F;
            aim(a, right, bx - a.x, by - a.y, bz - a.z, -bx, EYE_Y - by, EYE_Z - bz);
        }
    }

    /** El brazo estirado hacia donde mira, la palma hacia arriba (la luz sale por detrás del celular). */
    private static void pointing(HumanoidModel<?> m, HumanoidArm arm) {
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart a = right ? m.rightArm : m.leftArm;
        float[] l = look(m);
        aim(a, right, l[0], l[1], l[2], right ? 0.4F : -0.4F, -1F, 0F);
    }

    /** El celular pegado a la oreja: el brazo sube por el lado de la cabeza, la palma hacia ella. */
    private static void atEar(HumanoidModel<?> m, HumanoidArm arm) {
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart a = right ? m.rightArm : m.leftArm;
        float in = right ? 1F : -1F;
        float sway = (float) Math.sin(time() * 1.1) * 0.03F;
        // el brazo sube por el lado de la cabeza con el codo un poco afuera; el modelo de la llamada (base_ear) pone el celular
        // entre la cabeza y el brazo, a la altura de la oreja
        aim(a, right, -0.35F * in, -1F, -0.2F + sway, in, 0F, 0F);
    }

    /** Las dos manos arriba delante de la cara, como tomando una foto con el celular. */
    private static void takingPhoto(HumanoidModel<?> m, HumanoidArm arm) {
        float[] l = look(m);
        for (int k = 0; k < 2; k++) {
            boolean right = (arm == HumanoidArm.RIGHT) == (k == 0);
            ModelPart a = right ? m.rightArm : m.leftArm;
            float in = right ? 1F : -1F;
            float dx = l[0] + 0.25F * in, dy = l[1] - 0.35F, dz = l[2];
            float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float hx = a.x + dx / dl * 10F, hy = a.y + dy / dl * 10F, hz = a.z + dz / dl * 10F;
            aim(a, right, dx, dy, dz, -hx, EYE_Y - hy, EYE_Z - hz);
        }
    }

    /** Para PhoneItem: la pose según la pantalla que lleva el celular. */
    public static final IClientItemExtensions EXT = new IClientItemExtensions() {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity who, InteractionHand hand, ItemStack stack) {
            return switch (PhoneUse.get(stack)) {
                case PhoneUse.OFF -> null;
                case PhoneUse.FLASH -> flash();
                case PhoneUse.EAR -> ear();
                case PhoneUse.CAMERA -> photo();
                default -> text();
            };
        }
    };
}
