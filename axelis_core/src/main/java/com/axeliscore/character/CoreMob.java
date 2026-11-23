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
