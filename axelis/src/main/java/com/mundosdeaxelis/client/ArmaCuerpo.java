package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.arma.ArmaItem;
import com.mundosdeaxelis.arma.TipoArma;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Etapa 283: el golpe con todo el cuerpo, visto desde fuera (tercera persona y otros jugadores).
 *
 * <ul>
 * <li>Cada tipo de arma tiene su golpe: el tronco gira, los dos brazos se mueven, las piernas dan un paso (la de delante adelante,
 * la de atrás atrás) y todo el cuerpo se inclina, baja o avanza un poco (eso último se hace girando el dibujo entero desde los
 * pies).</li>
 * <li>El golpe tiene su propio reloj (el de Minecraft dura siempre 0,3 s): el martillo tarda más que las dagas, pero el impacto
 * llega pronto, para que coincida con el daño.</li>
 * <li>Las dagas se ven las dos (una en cada mano, también en primera persona) y apuñalan alternando.</li>
 * <li>En primera persona la cámara acompaña un poco (se puede quitar en Opciones).</li>
 * </ul>
 * El golpe vanilla del brazo se apaga poniendo {@code attackTime} a 0 dentro de la pose (Minecraft lo aplica después).
 */
public final class ArmaCuerpo {
    private ArmaCuerpo() {
    }

    // ------------------------------------------------------------------ reloj del golpe

    private static final class Reloj {
        boolean lastSwinging;
        int lastSwingTime;
        float start = -1000.0F;
        int swings;
    }

    private static final Map<LivingEntity, Reloj> RELOJ = new WeakHashMap<>();

    /** Lo que dura el golpe entero en el cuerpo, en segundos (siempre menos que la recarga del arma). */
    public static float duracion(TipoArma t) {
        return switch (t) {
            case ESPADA -> 0.45F;
            case HACHA -> 0.62F;
            case MARTILLO -> 0.72F;
            case LANZA -> 0.50F;
            case DAGAS -> 0.32F;
        };
    }

    private static Reloj reloj(LivingEntity e) {
        Reloj r = RELOJ.computeIfAbsent(e, k -> new Reloj());
        if (e.swinging && e.swingingArm == InteractionHand.MAIN_HAND && (!r.lastSwinging || e.swingTime < r.lastSwingTime)) {
            r.start = e.tickCount - Math.max(0, e.swingTime);
            r.swings++;
        }
        r.lastSwinging = e.swinging;
        r.lastSwingTime = e.swingTime;
        return r;
    }

    /** Punto del golpe (0..1), o -1 si no está golpeando con el arma. */
    public static float progreso(LivingEntity e, TipoArma tipo, float pt) {
        if (ArmaAnim.fijo >= 0.0F) {
            return ArmaAnim.fijo;
        }
        Reloj r = reloj(e);
        float el = (e.tickCount + pt - r.start) / 20.0F / duracion(tipo);
        return el >= 0.0F && el < 1.0F ? el : -1.0F;
    }

    /** Con dagas: ¿este golpe lo da la mano del arma (true) o la otra (false)? Se van turnando. */
    public static boolean daLaPrincipal(LivingEntity e) {
        if (ArmaAnim.fijo >= 0.0F) {
            return ArmaAnim.fijoPrincipal;
        }
        return reloj(e).swings % 2 == 1;
    }

    // ------------------------------------------------------------------ poses del cuerpo

    /*
     * Fotogramas: {t, peso, giro, brazo: x, adentro, afuera, z, otro brazo: x, adentro, afuera, z, pierna delante, pierna detrás,
     * inclinación (grados), bajada (bloques), avance (bloques)}. «Adentro» lleva el brazo hacia el otro lado del cuerpo y «afuera»
     * lo abre; z negativo = el hombro hacia delante; el giro positivo echa atrás el hombro del arma. Peso 0 = la pose normal.
     */
    private static final float[][] ESPADA = {
            {0.00F, 0, 0.55F, -2.55F, -0.35F, 0.35F, 0.0F, -0.55F, 0.40F, -0.15F, 0.0F, -0.25F, 0.20F, -4, 0.00F, 0.00F},
            {0.20F, 1, 0.55F, -2.55F, -0.35F, 0.35F, 0.0F, -0.55F, 0.40F, -0.15F, 0.0F, -0.25F, 0.20F, -4, 0.00F, 0.00F},
            {0.38F, 1, -0.50F, -0.95F, 0.75F, -0.25F, -1.5F, -0.15F, 0.00F, 0.25F, 0.0F, -0.45F, 0.35F, 10, 0.02F, 0.12F},
            {0.62F, 1, -0.62F, -0.45F, 0.90F, -0.10F, -1.0F, 0.15F, 0.00F, 0.20F, 0.0F, -0.40F, 0.30F, 8, 0.02F, 0.10F},
            {1.00F, 0, -0.62F, -0.45F, 0.90F, -0.10F, -1.0F, 0.15F, 0.00F, 0.20F, 0.0F, -0.40F, 0.30F, 8, 0.02F, 0.10F}};
    private static final float[][] HACHA = {
            {0.00F, 0, 0.40F, -2.85F, -0.15F, 0.12F, 1.0F, -2.70F, 0.55F, -0.05F, 1.0F, -0.30F, 0.25F, -8, 0.00F, 0.00F},
            {0.22F, 1, 0.40F, -2.85F, -0.15F, 0.12F, 1.0F, -2.70F, 0.55F, -0.05F, 1.0F, -0.30F, 0.25F, -8, 0.00F, 0.00F},
            {0.40F, 1, -0.35F, -0.60F, 0.35F, -0.05F, -2.0F, -0.62F, 0.55F, 0.00F, -2.0F, -0.55F, 0.45F, 20, 0.08F, 0.15F},
            {0.66F, 1, -0.42F, -0.35F, 0.40F, 0.00F, -1.5F, -0.40F, 0.60F, 0.00F, -1.5F, -0.50F, 0.40F, 16, 0.06F, 0.12F},
            {1.00F, 0, -0.42F, -0.35F, 0.40F, 0.00F, -1.5F, -0.40F, 0.60F, 0.00F, -1.5F, -0.50F, 0.40F, 16, 0.06F, 0.12F}};
    private static final float[][] MARTILLO = {
            {0.00F, 0, 0.18F, -3.05F, 0.05F, 0.05F, 1.5F, -3.00F, 0.38F, 0.00F, 1.5F, -0.35F, 0.30F, -12, 0.00F, -0.02F},
            {0.26F, 1, 0.18F, -3.05F, 0.05F, 0.05F, 1.5F, -3.00F, 0.38F, 0.00F, 1.5F, -0.35F, 0.30F, -12, 0.00F, -0.02F},
            {0.44F, 1, -0.08F, -0.30F, 0.30F, 0.00F, -2.5F, -0.38F, 0.52F, 0.00F, -2.5F, -0.70F, 0.55F, 26, 0.16F, 0.10F},
            {0.70F, 1, -0.10F, -0.25F, 0.30F, 0.00F, -2.0F, -0.32F, 0.50F, 0.00F, -2.0F, -0.62F, 0.50F, 22, 0.12F, 0.08F},
            {1.00F, 0, -0.10F, -0.25F, 0.30F, 0.00F, -2.0F, -0.32F, 0.50F, 0.00F, -2.0F, -0.62F, 0.50F, 22, 0.12F, 0.08F}};
    private static final float[][] LANZA = {
            {0.00F, 0, 0.45F, -1.25F, 0.00F, 0.00F, 2.5F, -1.10F, 0.55F, 0.00F, 1.5F, -0.20F, 0.25F, -5, 0.00F, -0.05F},
            {0.18F, 1, 0.45F, -1.25F, 0.00F, 0.00F, 2.5F, -1.10F, 0.55F, 0.00F, 1.5F, -0.20F, 0.25F, -5, 0.00F, -0.05F},
            {0.34F, 1, -0.30F, -1.62F, 0.25F, 0.00F, -3.5F, -1.55F, 0.65F, 0.00F, -2.5F, -0.75F, 0.55F, 16, 0.05F, 0.28F},
            {0.60F, 1, -0.25F, -1.58F, 0.25F, 0.00F, -3.0F, -1.50F, 0.62F, 0.00F, -2.0F, -0.65F, 0.50F, 13, 0.04F, 0.22F},
            {1.00F, 0, -0.25F, -1.58F, 0.25F, 0.00F, -3.0F, -1.50F, 0.62F, 0.00F, -2.0F, -0.65F, 0.50F, 13, 0.04F, 0.22F}};
    /** Dagas: el «brazo» es el que apuñala en este golpe (se turnan) y el «otro» se queda en guardia. */
    private static final float[][] DAGAS = {
            {0.00F, 0, 0.30F, -0.75F, -0.10F, 0.10F, 1.5F, -0.95F, 0.35F, 0.00F, 0.0F, -0.20F, 0.20F, 2, 0.00F, 0.00F},
            {0.15F, 1, 0.30F, -0.75F, -0.10F, 0.10F, 1.5F, -0.95F, 0.35F, 0.00F, 0.0F, -0.20F, 0.20F, 2, 0.00F, 0.00F},
            {0.32F, 1, -0.40F, -1.60F, 0.30F, 0.00F, -3.0F, -0.70F, 0.20F, 0.00F, 0.5F, -0.40F, 0.30F, 9, 0.02F, 0.12F},
            {0.58F, 1, -0.30F, -1.45F, 0.30F, 0.00F, -2.0F, -0.75F, 0.25F, 0.00F, 0.3F, -0.35F, 0.25F, 6, 0.02F, 0.08F},
            {1.00F, 0, -0.30F, -1.45F, 0.30F, 0.00F, -2.0F, -0.75F, 0.25F, 0.00F, 0.3F, -0.35F, 0.25F, 6, 0.02F, 0.08F}};

    static final int T = 0, W = 1, GIRO = 2, MX = 3, MIN = 4, MOUT = 5, MZ = 6, OX = 7, OIN = 8, OOUT = 9, OZ = 10, DELANTE = 11,
            DETRAS = 12, INCL = 13, BAJA = 14, AVANCE = 15;

    private static float[][] claves(TipoArma t) {
        return switch (t) {
            case ESPADA -> ESPADA;
            case HACHA -> HACHA;
            case MARTILLO -> MARTILLO;
            case LANZA -> LANZA;
            case DAGAS -> DAGAS;
        };
    }

    /** Tiempo (0..1) del fotograma {@code i} del golpe (1 = preparando, 2 = impacto). */
    public static float tiempoClave(TipoArma tipo, int i) {
        return claves(tipo)[i][T];
    }

    /** La pose del cuerpo en el punto {@code t} del golpe (16 valores, ver arriba). */
    public static float[] cuadro(TipoArma tipo, float t) {
        float[][] k = claves(tipo);
        t = Mth.clamp(t, 0.0F, 1.0F);
        int s = 0;
        while (s < k.length - 2 && t > k[s + 1][T]) {
            s++;
        }
        float[] a = k[s], b = k[s + 1];
        float u = Mth.clamp((t - a[T]) / Math.max(1.0E-4F, b[T] - a[T]), 0.0F, 1.0F);
        u = u * u * (3.0F - 2.0F * u);
        float[] out = new float[a.length];
        for (int j = 0; j < a.length; j++) {
            out[j] = Mth.lerp(u, a[j], b[j]);
        }
        return out;
    }

    /** ¿Se puede mover todo el cuerpo ahora? (no montado, nadando, planeando ni durmiendo) */
    static boolean libre(LivingEntity e) {
        return !e.isPassenger() && !e.isVisuallySwimming() && !e.isFallFlying() && !e.isSleeping() && e.getPose() != Pose.SPIN_ATTACK;
    }

    /** El arma que lleva en la mano principal (null si no es de las nuevas o está usando algo). */
    static ArmaItem arma(LivingEntity e) {
        return !e.isUsingItem() && e.getMainHandItem().getItem() instanceof ArmaItem a ? a : null;
    }

    /** Dagas con la otra mano libre: se ven las dos. */
    public static boolean dagasDobles(LivingEntity e) {
        ArmaItem a = arma(e);
        return a != null && a.tipo == TipoArma.DAGAS && e.getOffhandItem().isEmpty();
    }

    /**
     * La pose de las armas en el modelo (la llama la ArmPose de cada tipo, antes de que Minecraft aplique el golpe). Primero el
     * agarre de siempre y, si está golpeando, el golpe de todo el cuerpo.
     */
    static void aplicar(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm, TipoArma tipo) {
        ModelPart main = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        ModelPart off = arm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
        float s = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        boolean manoLibre = entity.getOffhandItem().isEmpty();
        // 1) agarre
        switch (tipo) {
            case HACHA, MARTILLO, LANZA -> {
                float mira = Mth.clamp(model.head.xRot, -0.6F, 0.6F) * 0.3F;
                main.xRot = -0.70F + mira;
                main.yRot = s * -0.15F;
                main.zRot = 0.0F;
                if (manoLibre) {
                    off.xRot = -0.95F + mira;
                    off.yRot = s * 0.62F;
                    off.zRot = 0.0F;
                }
            }
            case DAGAS -> {
                main.xRot = main.xRot * 0.5F - (float) Math.PI / 10.0F;
                main.yRot = 0.0F;
                if (manoLibre) {
                    off.xRot = off.xRot * 0.5F - (float) Math.PI / 10.0F;
                    off.yRot = 0.0F;
                }
            }
            default -> {
                main.xRot = main.xRot * 0.5F - (float) Math.PI / 10.0F;
                main.yRot = 0.0F;
            }
        }
        if (!(entity instanceof Player) || entity.swingingArm != InteractionHand.MAIN_HAND) {
            return;
        }
        // 2) el golpe: el de Minecraft (solo el brazo) se apaga y se hace el nuestro
        model.attackTime = 0.0F;
        float t = progreso(entity, tipo, Minecraft.getInstance().getFrameTime());
        if (t < 0.0F || !libre(entity)) {
            return;
        }
        float[] f = cuadro(tipo, t);
        float w = f[W];
        if (w <= 0.0F) {
            return;
        }
        ModelPart act = main, otro = off;
        float sa = s;
        if (tipo == TipoArma.DAGAS && manoLibre && !daLaPrincipal(entity)) {
            act = off;
            otro = main;
            sa = -s;
        }
        boolean otroLibre = manoLibre || otro == main;
        // tronco: gira y los hombros van con él (como en el golpe de Minecraft)
        float giro = sa * f[GIRO] * w;
        model.body.yRot = giro;
        model.rightArm.z = Mth.sin(giro) * 5.0F;
        model.rightArm.x = -Mth.cos(giro) * 5.0F;
        model.leftArm.z = -Mth.sin(giro) * 5.0F;
        model.leftArm.x = Mth.cos(giro) * 5.0F;
        // brazos («adentro» va hacia el otro lado: en el derecho es yRot negativo y en el izquierdo positivo)
        brazo(act, act == model.rightArm, f[MX], f[MIN], f[MOUT], f[MZ], w, giro);
        if (otroLibre) {
            brazo(otro, otro == model.rightArm, f[OX], f[OIN], f[OOUT], f[OZ], w, giro);
        } else {
            otro.yRot += giro;
        }
        // piernas: un paso con la de delante (la del lado contrario al brazo que golpea) y la otra atrás
        ModelPart delante = sa > 0 ? model.leftLeg : model.rightLeg;
        ModelPart detras = sa > 0 ? model.rightLeg : model.leftLeg;
        if (entity.onGround() || entity.isInWater()) {
            delante.xRot = Mth.lerp(w, delante.xRot, f[DELANTE]);
            detras.xRot = Mth.lerp(w, detras.xRot, f[DETRAS]);
            model.rightLeg.zRot = Mth.lerp(w, model.rightLeg.zRot, 0.07F);
            model.leftLeg.zRot = Mth.lerp(w, model.leftLeg.zRot, -0.07F);
        }
    }

    private static void brazo(ModelPart p, boolean derecho, float x, float adentro, float afuera, float z, float w, float giro) {
        float side = derecho ? 1.0F : -1.0F;
        p.xRot = Mth.lerp(w, p.xRot, x);
        p.yRot = Mth.lerp(w, p.yRot, -side * adentro) + giro;
        p.zRot = Mth.lerp(w, p.zRot, side * afuera);
        p.z += z * w;
    }

    // ------------------------------------------------------------------ inclinar todo el cuerpo (desde los pies)

    private static final Deque<Boolean> EMPUJADO = new ArrayDeque<>();

    @Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, value = Dist.CLIENT)
    public static final class Eventos {
        private Eventos() {
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onPre(RenderPlayerEvent.Pre e) {
            Player p = e.getEntity();
            if (!Float.isNaN(AutoTest.ladoFoto) && p == Minecraft.getInstance().player) {
                // solo para las fotos de la prueba: el cuerpo y la cabeza de lado respecto a la cámara
                float r = p.getYRot() + AutoTest.ladoFoto;
                p.yBodyRot = p.yBodyRotO = p.yHeadRot = p.yHeadRotO = r;
            }
            ArmaItem a = arma(p);
            float t = a == null || p.swingingArm != InteractionHand.MAIN_HAND || !libre(p) ? -1.0F : progreso(p, a.tipo, e.getPartialTick());
            if (t < 0.0F) {
                EMPUJADO.push(false);
                return;
            }
            float[] f = cuadro(a.tipo, t);
            float w = f[W];
            float yaw = Mth.rotLerp(e.getPartialTick(), p.yBodyRotO, p.yBodyRot);
            PoseStack ps = e.getPoseStack();
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(-yaw));
            float incl = f[INCL] * w * (p.isCrouching() ? 0.5F : 1.0F);
            ps.translate(0.0F, -f[BAJA] * w, f[AVANCE] * w);
            ps.mulPose(Axis.XP.rotationDegrees(incl));
            ps.mulPose(Axis.YP.rotationDegrees(yaw));
            EMPUJADO.push(true);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onPost(RenderPlayerEvent.Post e) {
            Boolean b = EMPUJADO.poll();
            if (b != null && b) {
                e.getPoseStack().popPose();
            }
        }

        /** Primera persona: la cámara acompaña el golpe (un poco). */
        @SubscribeEvent
        public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !mc.options.getCameraType().isFirstPerson() || !com.mundosdeaxelis.ClientConfig.ATTACK_CAMERA.get()) {
                return;
            }
            ArmaItem a = arma(mc.player);
            if (a == null || mc.player.swingingArm != InteractionHand.MAIN_HAND) {
                return;
            }
            float t = progreso(mc.player, a.tipo, (float) e.getPartialTick());
            if (t < 0.0F) {
                return;
            }
            float[] f = cuadro(a.tipo, t);
            float w = f[W];
            float s = mc.player.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            if (a.tipo == TipoArma.DAGAS && !daLaPrincipal(mc.player)) {
                s = -s;
            }
            e.setPitch(e.getPitch() + f[INCL] * 0.14F * w);
            e.setRoll(e.getRoll() - s * f[GIRO] * 3.0F * w);
        }

        /** Primera persona: la segunda daga en la mano izquierda (si está libre), y apuñala cuando le toca. */
        @SubscribeEvent
        public static void onHand(RenderHandEvent e) {
            if (e.getHand() != InteractionHand.OFF_HAND) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !dagasDobles(mc.player)) {
                return;
            }
            ItemStack st = mc.player.getMainHandItem();
            HumanoidArm brazo = mc.player.getMainArm().getOpposite();
            PoseStack ps = e.getPoseStack();
            ps.pushPose();
            ArmaAnim.manoDaga(ps, brazo, st, e.getPartialTick(), e.getEquipProgress());
            mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(mc.player, st,
                    brazo == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    brazo == HumanoidArm.LEFT, ps, e.getMultiBufferSource(), e.getPackedLight());
            ps.popPose();
        }
    }

    // ------------------------------------------------------------------ la segunda daga en tercera persona

    @Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Capas {
        private Capas() {
        }

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                if (event.getPlayerSkin(skin) instanceof PlayerRenderer pr) {
                    pr.addLayer(new DagaLayer(pr));
                }
            }
        }
    }

    /** Dibuja la daga de la otra mano (la misma del arma) cuando la mano izquierda está libre. */
    static final class DagaLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        DagaLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                           float pt, float age, float yaw, float pitch) {
            if (!dagasDobles(p)) {
                return;
            }
            ItemStack st = p.getMainHandItem();
            HumanoidArm brazo = p.getMainArm().getOpposite();
            boolean izq = brazo == HumanoidArm.LEFT;
            ps.pushPose();
            getParentModel().translateToHand(brazo, ps);
            ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
            ps.mulPose(Axis.YP.rotationDegrees(180.0F));
            ps.translate((izq ? -1 : 1) / 16.0F, 0.125F, -0.625F);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(p, st,
                    izq ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, izq, ps, buf, light);
            ps.popPose();
        }
    }
}
