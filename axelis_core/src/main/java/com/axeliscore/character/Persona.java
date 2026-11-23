package com.axeliscore.character;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animator;
import com.axeliscore.anim.Clip;
import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.plat.Plat;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * Persona: el humano "Minecraft mejorado" del Core. Camina y corre con pasos que no resbalan (la animación va a la velocidad real),
 * mira a quien tiene cerca con cabeza y pecho, parpadea, mueve la boca al hablar y hace gestos (saludar, señalar, reír, festejar,
 * pensar, sentarse, pescar, bailar...). Cualquier mod puede usarla tal cual (con su propia piel) o heredar de ella.
 * <p>
 * Comportamiento de muestra: pasea, saluda con la mano a quien se acerca, al hacerle clic se pone a platicar y a veces hace un gesto
 * por su cuenta.
 */
public class Persona extends CoreMob {
    /** Pieles de muestra (textures/entity/persona/&lt;nombre&gt;.png). */
    public static final String[] SKINS = {"mara", "iker", "lu", "nilo", "sol"};
    /** Animaciones que mueven todo el cuerpo (las demás solo de la cintura para arriba, así se puede saludar caminando). */
    public static final Set<String> FULL_BODY = Set.of("cheer", "sit", "death", "dance", "fish_cast");

    private int greetCooldown;
    private int nextIdleGesture = 300;
    /** Para la galería: repite este gesto (y no se mueve). */
    private String showcase;
    private int showcaseTicks;

    public Persona(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Para la galería: se queda quieta repitiendo un gesto. */
    public void showcase(String clip, int everyTicks) {
        this.showcase = clip;
        this.showcaseTicks = everyTicks;
        setNoAi(true);
        playAction(clip, everyTicks);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            return;
        }
        if (showcase != null) {
            if (tickCount - lastActionTick >= showcaseTicks) {
                playAction(showcase, showcaseTicks);
            }
            return;
        }
        if (greetCooldown > 0) {
            greetCooldown--;
        }
        if (tickCount % 10 == 0 && greetCooldown == 0) {
            Player p = level().getNearestPlayer(this, 6.0);
            if (p != null && !p.isSpectator() && hasLineOfSight(p)) {
                getLookControl().setLookAt(p, 40F, 40F);
                playAction("wave");
                greetCooldown = 1200;
            }
        }
        if (--nextIdleGesture <= 0) {
            nextIdleGesture = 400 + getRandom().nextInt(700);
            if (getNavigation().isDone() && tickCount - lastActionTick > 200) {
                String[] g = {"look_around", "look_around", "think", "nod", "laugh"};
                playAction(g[getRandom().nextInt(g.length)], 60);
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide()) {
            getNavigation().stop();
            getLookControl().setLookAt(player, 60F, 60F);
            String[] g = {"talk", "talk", "laugh", "nod", "point", "shake_head"};
            String pick = g[getRandom().nextInt(g.length)];
            playAction(pick, pick.equals("talk") ? 80 : 0);
            if (!hasCustomName()) {
                Plat.msg(player, Component.translatable("axelis_core.persona.hola", Component.translatable(
                        "axelis_core.persona." + SKINS[Math.floorMod(variant(), SKINS.length)])), true);
            }
        }
        return Plat.ok(level().isClientSide());
    }

    // ------------------------------------------------------------------ animación (cliente)

    @Override
    public void axAnimate(AnimHost h, float pt) {
        Animator an = h.animator();
        // gestos pedidos por el servidor
        String[] act = takeAction(h);
        if (act != null) {
            String layer = FULL_BODY.contains(act[0]) ? "full" : "upper";
            Clip c = h.clip(act[0]);
            if (c != null) {
                an.stop(layer.equals("full") ? "upper" : "full", 0.2F);
                an.play(layer, c, 0.22F, 1F, true);
                int ticks = Integer.parseInt(act[1]);
                h.data.put("actEnd", ticks > 0 ? tickCount + ticks : -1);
                h.data.put("actLayer", layer);
                h.data.put("talkUntil", act[0].equals("talk") ? tickCount + (ticks > 0 ? ticks : 60) : 0);
            }
        }
        Object end = h.data.get("actEnd");
        if (end instanceof Integer e && e > 0 && tickCount >= e) {
            an.stop((String) h.data.get("actLayer"), 0.35F);
            h.data.put("actEnd", -1);
        }
        if (deathTime > 0) {
            if (!"death".equals(h.data.get("dead"))) {
                h.data.put("dead", "death");
                an.stop("upper", 0.1F);
                h.play("full", "death", 0.1F);
            }
            return;
        }
        if (hurtTime > 0 && hurtTime == hurtDuration && !Integer.valueOf(tickCount).equals(h.data.get("hurtAt"))) {
            h.data.put("hurtAt", tickCount);
            Clip hc = h.clip("hurt");
            if (hc != null && an.finished("upper")) {
                an.play("upper", hc, 0.05F, 1F, true);
            }
        }
        // caminar / correr / quieto, a la velocidad real (los pies no resbalan)
        float spd = smoothSpeed(h);
        if (isPassenger()) {
            h.play("base", "sit", 0.3F);
        } else if (spd > 3.0F) {
            an.play("base", h.clip("run"), 0.25F, Mth.clamp(spd / 3.7F, 0.7F, 1.6F), false);
        } else if (spd > 0.25F) {
            an.play("base", h.clip("walk"), 0.25F, Mth.clamp(spd / 1.31F, 0.5F, 1.9F), false);
        } else {
            an.play("base", h.clip("idle"), 0.35F, 1F, false);
        }
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        Geo geo = h.geo();
        // mirar: 70 % la cabeza, 30 % el pecho (si no está haciendo algo con todo el cuerpo)
        float headYaw = Mth.wrapDegrees(Mth.rotLerp(pt, yHeadRotO, yHeadRot) - Mth.rotLerp(pt, yBodyRotO, yBodyRot));
        float pitch = Mth.lerp(pt, xRotO, getXRot());
        headYaw = Mth.clamp(headYaw, -75F, 75F);
        boolean busy = deathTime > 0 || h.animator().get("full") != null && h.animator().get("full").playing();
        if (!busy) {
            float r = (float) Math.PI / 180F;
            pose.addRot(geo.bone("head"), pitch * 0.85F * r, headYaw * 0.7F * r, 0);
            pose.addRot(geo.bone("chest"), 0, headYaw * 0.3F * r, 0);
        }
        // cara: parpadeo y boca
        for (String f : new String[] {"face_eyes", "face_mouth", "face_happy", "face_smile"}) {
            pose.hide(geo.bone(f), true);
        }
        float now = tickCount + pt;
        Object nb = h.data.get("blinkAt");
        float blinkAt = nb instanceof Float b ? b : now + 40 + getRandom().nextInt(60);
        if (now > blinkAt + 3) {
            blinkAt = now + 50 + getRandom().nextInt(90);
        }
        h.data.put("blinkAt", blinkAt);
        boolean dead = deathTime > 0;
        if (dead || now >= blinkAt && now <= blinkAt + 3) {
            pose.hide(geo.bone("face_eyes"), false);
        }
        Object tu = h.data.get("talkUntil");
        Clip upper = h.animator().get("upper") == null ? null : h.animator().get("upper").clip();
        boolean laughing = upper != null && upper.name.endsWith("laugh") && h.animator().get("upper").playing();
        boolean cheering = h.animator().get("full") != null && h.animator().get("full").playing() && h.animator().get("full").clip() != null
                && h.animator().get("full").clip().name.endsWith("cheer");
        if (laughing || cheering) {
            pose.hide(geo.bone("face_happy"), false);
            pose.hide(geo.bone("face_eyes"), true);
            pose.hide(geo.bone("face_smile"), false);
        } else if (tu instanceof Integer until && tickCount < until) {
            // sílabas: boca abierta/cerrada con un ritmo irregular
            int syl = (int) (now / 2.6F);
            boolean open = ((syl * 7919) % 5) < 3;
            pose.hide(geo.bone("face_mouth"), !open);
        }
    }
}
