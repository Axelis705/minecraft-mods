package com.axeliscore.character;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * Base de los personajes del Core: una criatura con IA de Minecraft que se dibuja con el motor de animación. Agrega:
 * <ul>
 *     <li>{@link #playAction}: el servidor pide un gesto ("wave", "attack"...) y todos los que la ven lo reproducen.</li>
 *     <li>Variante (piel/forma) guardada y sincronizada.</li>
 * </ul>
 */
public abstract class CoreMob extends PathfinderMob implements Animated {
    private static final EntityDataAccessor<String> ACTION = SynchedEntityData.defineId(CoreMob.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(CoreMob.class, EntityDataSerializers.INT);
    private final AnimHost anim = new AnimHost();
    private int actionSeq;
    /** Para galerías y pruebas: repite este gesto cada {@link #showcaseTicks} y no se mueve. */
    private String showcase;
    private int showcaseTicks;
    /** Desde cuándo (tickCount del servidor) no se pide un gesto. */
    protected int lastActionTick;

    protected CoreMob(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    //#if MC >= 1.20.5
    //$$ @Override
    //$$ protected void defineSynchedData(SynchedEntityData.Builder b) {
    //$$     super.defineSynchedData(b);
    //$$     b.define(ACTION, "");
    //$$     b.define(VARIANT, 0);
    //$$ }
    //#else
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACTION, "");
        this.entityData.define(VARIANT, 0);
    }
    //#endif

    @Override
    public AnimHost axHost() {
        return anim;
    }

    /**
     * (Servidor) Reproduce una animación en todos los clientes. {@code ticks}: cuánto dura si es de las que se repiten (hablar,
     * bailar); 0 = lo que dure la animación.
     */
    public void playAction(String clip, int ticks) {
        actionSeq++;
        lastActionTick = tickCount;
        this.entityData.set(ACTION, clip + "#" + actionSeq + "#" + ticks);
    }

    public void playAction(String clip) {
        playAction(clip, 0);
    }

    /** (Servidor) Para galerías y pruebas: se queda quieta repitiendo un gesto cada {@code everyTicks}. */
    public void showcase(String clip, int everyTicks) {
        this.showcase = clip;
        this.showcaseTicks = everyTicks;
        setNoAi(true);
        playAction(clip, everyTicks);
    }

    public boolean showcasing() {
        return showcase != null;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && showcase != null && tickCount - lastActionTick >= showcaseTicks) {
            playAction(showcase, showcaseTicks);
        }
    }

    public int variant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, v);
    }

    /** (Cliente) Si llegó un gesto nuevo: {nombre, ticks}; si no, null. */
    protected String[] takeAction(AnimHost h) {
        String a = this.entityData.get(ACTION);
        if (a.equals(h.lastAction)) {
            return null;
        }
        h.lastAction = a;
        String[] p = a.split("#");
        if (p.length < 3 || p[0].isEmpty()) {
            return null;
        }
        return new String[] {p[0], p[2]};
    }

    //#if MC >= 1.21.6
    //$$ @Override
    //$$ protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
    //$$     super.addAdditionalSaveData(out);
    //$$     out.putInt("AxVariant", variant());
    //$$ }
    //$$
    //$$ @Override
    //$$ protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
    //$$     super.readAdditionalSaveData(in);
    //$$     setVariant(in.getIntOr("AxVariant", 0));
    //$$ }
    //#else
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AxVariant", variant());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(com.axeliscore.plat.Nbt.i(tag, "AxVariant"));
    }
    //#endif

    // ------------------------------------------------------------------ ayudas para los controladores (cliente)

    /**
     * Gestos pedidos por el servidor: los pone en la capa que diga {@code layerFor} (por ejemplo "action" para todo el cuerpo o
     * "upper" para de la cintura arriba) y los quita al pasar los ticks pedidos.
     */
    protected void axActions(AnimHost h, java.util.function.Function<String, String> layerFor) {
        com.axeliscore.anim.Animator an = h.animator();
        String[] act = takeAction(h);
        if (act != null) {
            com.axeliscore.anim.Clip c = h.clip(act[0]);
            if (c != null) {
                String layer = layerFor.apply(act[0]);
                Object old = h.data.get("actLayer");
                if (old instanceof String o && !o.equals(layer)) {
                    an.stop(o, 0.2F);
                }
                an.play(layer, c, 0.2F, 1F, true);
                int ticks = Integer.parseInt(act[1]);
                h.data.put("actEnd", ticks > 0 ? tickCount + ticks : -1);
                h.data.put("actLayer", layer);
                h.data.put("actName", act[0]);
                h.data.put("actAt", tickCount);
            }
        }
        Object end = h.data.get("actEnd");
        if (end instanceof Integer e && e > 0 && tickCount >= e) {
            an.stop((String) h.data.get("actLayer"), 0.35F);
            h.data.put("actEnd", -1);
        }
    }

    /** ¿Está sonando un gesto con este nombre? */
    protected boolean axDoing(AnimHost h, String clip) {
        Object l = h.data.get("actLayer");
        if (!(l instanceof String layer) || !clip.equals(h.data.get("actName"))) {
            return false;
        }
        var ly = h.animator().get(layer);
        return ly != null && ly.playing();
    }

    /** Al morir: la animación "death" (si existe) en la capa dada. Devuelve true si está muerta (no hace falta animar más). */
    protected boolean axDeath(AnimHost h, String layer) {
        if (deathTime <= 0) {
            return false;
        }
        if (!Boolean.TRUE.equals(h.data.get("dead"))) {
            h.data.put("dead", true);
            for (String l : new String[] {"upper", "action", "full"}) {
                if (!l.equals(layer)) {
                    h.animator().stop(l, 0.1F);
                }
            }
            h.play(layer, "death", 0.1F);
        }
        return true;
    }

    /** Un respingo al recibir daño (si la capa está libre). */
    protected void axHurt(AnimHost h, String layer) {
        if (hurtTime > 0 && hurtTime == hurtDuration && !Integer.valueOf(tickCount).equals(h.data.get("hurtAt"))) {
            h.data.put("hurtAt", tickCount);
            com.axeliscore.anim.Clip hc = h.clip("hurt");
            if (hc != null && h.animator().finished(layer)) {
                h.animator().play(layer, hc, 0.05F, 1F, true);
            }
        }
    }

    /** Cuando ataca (el brazo/mordida que manda Minecraft al pegar): reproduce {@code clip}. */
    protected void axSwing(AnimHost h, String clip, String layer) {
        // el golpe empieza cuando el "ataque" de Minecraft pasa de 0 a más (sirve en todas las versiones)
        //#if MC >= 26.3
        //$$ float now = getSwingAnimation(0F);
        //#else
        float now = getAttackAnim(0F);
        //#endif
        Object prev = h.data.get("atk");
        h.data.put("atk", now);
        if (now > 0F && prev instanceof Float p && p == 0F && !Integer.valueOf(tickCount).equals(h.data.get("swingAt"))) {
            h.data.put("swingAt", tickCount);
            com.axeliscore.anim.Clip c = h.clip(clip);
            if (c != null) {
                h.animator().play(layer, c, 0.08F, 1F, true);
            }
        }
    }

    /**
     * Quieto / caminar / correr en la capa "base", a la velocidad real (bloques por segundo): {@code walkRef} y {@code runRef} son
     * cuántos bloques avanza un ciclo de la animación por segundo, así los pies no resbalan.
     */
    protected void axMove(AnimHost h, float walkMin, float runMin, float walkRef, float runRef) {
        float spd = smoothSpeed(h);
        com.axeliscore.anim.Animator an = h.animator();
        if (spd > runMin && h.clip("run") != null) {
            an.play("base", h.clip("run"), 0.25F, net.minecraft.util.Mth.clamp(spd / runRef, 0.6F, 1.8F), false);
        } else if (spd > walkMin && h.clip("walk") != null) {
            an.play("base", h.clip("walk"), 0.25F, net.minecraft.util.Mth.clamp(spd / walkRef, 0.5F, 2.0F), false);
        } else {
            an.play("base", h.clip("idle"), 0.35F, 1F, false);
        }
    }

    /** Mirar con la cabeza (y un poco con {@code chest}, si se da) hacia donde mira la entidad. */
    protected void axLook(AnimHost h, com.axeliscore.anim.Pose pose, float pt, String head, String chest, float headShare) {
        com.axeliscore.anim.Geo geo = h.geo();
        float yaw = net.minecraft.util.Mth.wrapDegrees(net.minecraft.util.Mth.rotLerp(pt, yHeadRotO, yHeadRot)
                - net.minecraft.util.Mth.rotLerp(pt, yBodyRotO, yBodyRot));
        yaw = net.minecraft.util.Mth.clamp(yaw, -75F, 75F);
        float pitch = net.minecraft.util.Mth.lerp(pt, xRotO, getXRot());
        float r = (float) Math.PI / 180F;
        pose.addRot(geo.bone(head), pitch * 0.85F * r, yaw * headShare * r, 0);
        if (chest != null) {
            pose.addRot(geo.bone(chest), 0, yaw * (1 - headShare) * r, 0);
        }
    }

    /** Velocidad horizontal en bloques por segundo (de la posición, así sirve en el cliente). */
    protected float groundSpeed() {
        double dx = getX() - xo, dz = getZ() - zo;
        return (float) (Math.sqrt(dx * dx + dz * dz) * 20);
    }

    /** Velocidad suavizada (para no saltar entre caminar y quieto en cada cuadro). */
    protected float smoothSpeed(AnimHost h) {
        float raw = groundSpeed();
        Object o = h.data.get("spd");
        float prev = o instanceof Float f ? f : raw;
        float s = prev + (raw - prev) * 0.25F;
        h.data.put("spd", s);
        return s;
    }
}
