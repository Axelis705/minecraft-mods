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
 * <li>Tercera persona (etapa 283): el golpe es de todo el cuerpo, ver {@link ArmaCuerpo}. El hacha de guerra, el martillo y la
 * lanza se llevan con las dos manos y las dagas, una en cada mano.</li>
 * </ul>
 */
public final class ArmaAnim implements IClientItemExtensions {
    public static final ArmaAnim INSTANCE = new ArmaAnim();
    /** Para las fotos de la prueba: si es 0 o más, todas las armas se dibujan en ese punto del golpe (0..1). */
    public static volatile float fijo = -1.0F;

    private ArmaAnim() {
    }

    // ------------------------------------------------------------------ primera persona

    /**
     * Fotogramas clave: {tiempo, x, y, z, guiño, giro, cabeceo} (x hacia el lado del brazo; grados). Igual que el golpe de
     * Minecraft, el cabeceo (bajar el arma hacia delante, negativo) se hace en un marco girado "guiño" grados: con 45 el golpe
     * sale en diagonal y con 0 sale de arriba abajo.
     */
    private static final float[][] ESPADA = {
            {0.00F, 0, 0, 0, 0, 0, 0},
            {0.12F, 0.01F, 0.17F, 0.00F, -54, 1, -4},
            {0.40F, -0.26F, 0.04F, 0.00F, 68, -46, -91},
            {1.00F, 0, 0, 0, 0, 0, 0}};
    // el hacha y el martillo suben (sin echarse hacia la cara) y bajan de golpe
    private static final float[][] HACHA = {
            {0.00F, 0, 0, 0, 0, 0, 0},
            {0.22F, 0.00F, 0.27F, 0.02F, -45, 9, 0},
            {0.50F, -0.07F, 0.03F, 0.01F, 68, -60, -91},
            {1.00F, 0, 0, 0, 0, 0, 0}};
    private static final float[][] MARTILLO = {
            {0.00F, 0, 0, 0, 0, 0, 0},
            {0.28F, -0.04F, 0.30F, 0.03F, 9, 4, -17},
            {0.58F, 0.03F, 0.01F, -0.02F, 67, -56, -96},
            {1.00F, 0, 0, 0, 0, 0, 0}};
    // la lanza se pone apuntando al frente, se echa atrás y embiste hacia la mira
    private static final float[][] LANZA = {
            {0.00F, 0, 0, 0, 0, 0, 0},
            {0.18F, 0.03F, 0.03F, 0.07F, 4, -16, -69},
            {0.42F, -0.02F, -0.01F, -0.65F, 26, -12, -80},
            {1.00F, 0, 0, 0, 0, 0, 0}};
    private static final float[][] DAGAS = {
            {0.00F, 0, 0, 0, 0, 0, 0},
            {0.10F, 0.05F, 0.01F, -0.03F, 25, -40, -33},
            {0.30F, -0.32F, 0.13F, -0.02F, 53, -40, -70},
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

    /** Posición del arma en el punto {@code t} (0..1) del golpe: {x, y, z, guiño, giro, cabeceo}. */
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
        // etapa 283: con las dos dagas se turnan; cuando apuñala la otra, esta se queda quieta
        if (a.tipo == TipoArma.DAGAS && ArmaCuerpo.dagasDobles(player) && !ArmaCuerpo.daLaPrincipal(player)) {
            t = 0.0F;
        }
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        // la misma posición de reposo que Minecraft (con la bajada al cambiar de objeto o mientras se recarga el golpe); durante el
        // golpe casi no baja, para que se vea entero aunque el arma sea lenta
        float baja = equipProcess * (1.0F - 0.85F * Mth.sin(t * (float) Math.PI));
        pose.translate(i * 0.56F, -0.52F + baja * -0.6F, -0.72F);
        float[] k = pose(a.tipo, t);
        pose.translate(i * k[0], k[1], k[2]);
        pose.mulPose(Axis.YP.rotationDegrees(i * k[3]));
        pose.mulPose(Axis.ZP.rotationDegrees(i * k[4]));
        pose.mulPose(Axis.XP.rotationDegrees(k[5]));
        pose.mulPose(Axis.YP.rotationDegrees(i * -k[3]));
        return true;
    }

    /** Para las fotos: con {@link #fijo}, ¿apuñala la mano del arma (true) o la otra? */
    public static volatile boolean fijoPrincipal = true;

    /**
     * Primera persona (etapa 283): coloca la segunda daga en la otra mano, con el mismo golpe de las dagas, cuando le toca a ella.
     */
    public static void manoDaga(PoseStack pose, HumanoidArm arm, ItemStack stack, float partialTick, float equipProcess) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        float t = fijo >= 0.0F ? fijo : mc.player.getAttackAnim(partialTick);
        if (ArmaCuerpo.daLaPrincipal(mc.player)) {
            t = 0.0F;
        }
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float baja = equipProcess * (1.0F - 0.85F * Mth.sin(t * (float) Math.PI));
        pose.translate(i * 0.56F, -0.52F + baja * -0.6F, -0.72F);
        float[] k = pose(TipoArma.DAGAS, t);
        pose.translate(i * k[0], k[1], k[2]);
        pose.mulPose(Axis.YP.rotationDegrees(i * k[3]));
        pose.mulPose(Axis.ZP.rotationDegrees(i * k[4]));
        pose.mulPose(Axis.XP.rotationDegrees(k[5]));
        pose.mulPose(Axis.YP.rotationDegrees(i * -k[3]));
    }

    // ------------------------------------------------------------------ tercera persona (etapa 283: todo el cuerpo, ver ArmaCuerpo)

    private static HumanoidModel.ArmPose espada, dosManos, dagas;

    /** Crea las poses nuevas al arrancar el cliente (las poses son un enum ampliable: no se deben crear a mitad de partida). */
    public static void init() {
        pose(TipoArma.ESPADA);
        pose(TipoArma.HACHA);
        pose(TipoArma.DAGAS);
    }

    /**
     * La pose de cada tipo: la espada se lleva con una mano (la otra queda para el escudo); el hacha de guerra, el martillo y la
     * lanza con las dos; las dagas, una en cada mano. El golpe de todo el cuerpo lo pone {@link ArmaCuerpo#aplicar}.
     */
    private static HumanoidModel.ArmPose pose(TipoArma t) {
        switch (t) {
            case ESPADA -> {
                if (espada == null) {
                    espada = HumanoidModel.ArmPose.create("MUNDOS_DE_AXELIS_ESPADA", false,
                            (model, entity, arm) -> ArmaCuerpo.aplicar(model, entity, arm, tipoDe(entity, TipoArma.ESPADA)));
                }
                return espada;
            }
            case DAGAS -> {
                if (dagas == null) {
                    dagas = HumanoidModel.ArmPose.create("MUNDOS_DE_AXELIS_DAGAS", true,
                            (model, entity, arm) -> ArmaCuerpo.aplicar(model, entity, arm, TipoArma.DAGAS));
                }
                return dagas;
            }
            default -> {
                if (dosManos == null) {
                    dosManos = HumanoidModel.ArmPose.create("MUNDOS_DE_AXELIS_DOS_MANOS", true,
                            (model, entity, arm) -> ArmaCuerpo.aplicar(model, entity, arm, tipoDe(entity, TipoArma.HACHA)));
                }
                return dosManos;
            }
        }
    }

    /** El tipo del arma que lleva (la pose de dos manos la comparten el hacha, el martillo y la lanza). */
    private static TipoArma tipoDe(LivingEntity e, TipoArma porDefecto) {
        return e.getMainHandItem().getItem() instanceof ArmaItem a ? a.tipo : porDefecto;
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (hand != InteractionHand.MAIN_HAND || !(stack.getItem() instanceof ArmaItem a) || entity.isUsingItem()) {
            return null;
        }
        return pose(a.tipo);
    }
}
