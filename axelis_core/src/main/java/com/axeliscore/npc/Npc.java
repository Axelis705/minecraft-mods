package com.axeliscore.npc;

import com.axeliscore.character.Persona;
import com.axeliscore.plat.Plat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Un NPC con misiones, tienda y amistad: una {@link Persona} que usa un {@link NpcDef}. No recibe daño (salvo en creativo), no
 * desaparece y sigue su rutina: de día cerca de su trabajo, en la tarde va a su casa y de noche se sienta a descansar. Al hacerle clic
 * abre su ventana (hablar, misiones, tienda y amistad).
 * <pre>
 *     Npc n = Npc.poner(level, HERRERA, pos);   // o /axeliscore npc poner mimod:herrera
 *     n.casa(posCasa).trabajo(posFragua);
 * </pre>
 * También: {@code /summon axelis_core:npc ~ ~ ~ {AxNpc:"mimod:herrera"}}.
 */
public class Npc extends Persona {
    private static final EntityDataAccessor<String> DEF = SynchedEntityData.defineId(Npc.class, EntityDataSerializers.STRING);

    /** (Cliente) Lo que agrega la tarjeta al mirarlo (amistad, misiones): lo pone el cliente al arrancar. */
    public static java.util.function.BiConsumer<com.axeliscore.ui.InfoCard, NpcDef> INFO = (c, d) -> {
    };

    private BlockPos casa, trabajo;
    private ServerPlayer hablando;
    private int hablandoHasta;

    public Npc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        Plat.invulnerable(this);
    }

    /** (Servidor) Crea y pone un NPC en el mundo, con su casa y su trabajo en ese lugar. */
    public static Npc poner(net.minecraft.server.level.ServerLevel level, NpcDef def, BlockPos pos) {
        Npc n = Plat.create(com.axeliscore.registry.CoreEntities.NPC.get(), level);
        n.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
        n.setDef(def.id);
        n.casa = pos;
        n.trabajo = pos;
        level.addFreshEntity(n);
        return n;
    }

    //#if MC >= 1.20.5
    //$$ @Override
    //$$ protected void defineSynchedData(SynchedEntityData.Builder b) {
    //$$     super.defineSynchedData(b);
    //$$     b.define(DEF, "");
    //$$ }
    //#else
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DEF, "");
    }
    //#endif

    public String defId() {
        return this.entityData.get(DEF);
    }

    public NpcDef def() {
        return NpcDef.porId(defId());
    }

    public void setDef(String id) {
        this.entityData.set(DEF, id == null ? "" : id);
        NpcDef d = NpcDef.porId(id);
        if (d != null) {
            setCustomName(d.nombre());
        }
    }

    /** Dónde vive (va en la tarde y duerme ahí). */
    public Npc casa(BlockPos p) {
        this.casa = p;
        return this;
    }

    /** Dónde trabaja (pasa el día ahí). */
    public Npc trabajo(BlockPos p) {
        this.trabajo = p;
        return this;
    }

    public BlockPos casa() {
        return casa;
    }

    public BlockPos trabajo() {
        return trabajo;
    }

    /** Se queda quieto mirando a ese jugador un rato (mientras tiene su ventana abierta). */
    void hablandoCon(ServerPlayer p) {
        hablando = p;
        hablandoHasta = tickCount + 200;
        getNavigation().stop();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || showcasing()) {
            return;
        }
        if (hablando != null) {
            if (tickCount > hablandoHasta || !hablando.isAlive() || hablando.distanceToSqr(this) > 12 * 12) {
                hablando = null;
            } else {
                getLookControl().setLookAt(hablando, 30F, 30F);
                return;
            }
        }
        if (tickCount % 20 == 0) {
            rutina();
        }
    }

    /** Qué hace ahora según la hora y su rutina. */
    private void rutina() {
        if (casa == null && trabajo == null) {
            casa = blockPosition();
            trabajo = casa;
        }
        NpcDef d = def();
        NpcDef.Rutina r = d == null ? NpcDef.Rutina.LIBRE : d.rutina;
        long t = Math.floorMod(Plat.dayTime(level()), 24000L);
        boolean noche = t >= 13000 && t < 23000, tarde = t >= 11000 && t < 13000;
        BlockPos ancla;
        int radio;
        switch (r) {
            case QUIETO -> {
                ancla = trabajo != null ? trabajo : casa;
                radio = 3;
            }
            case LIBRE -> {
                ancla = trabajo != null ? trabajo : casa;
                radio = 14;
            }
            default -> {
                boolean enCasa = noche || tarde;
                ancla = enCasa ? (casa != null ? casa : trabajo) : (trabajo != null ? trabajo : casa);
                radio = enCasa ? 3 : 7;
            }
        }
        double dx = getX() - (ancla.getX() + 0.5), dz = getZ() - (ancla.getZ() + 0.5);
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > radio + 1.5) {
            Plat.walkTo(this, ancla.getX() + 0.5, ancla.getY(), ancla.getZ() + 0.5, tarde ? 0.75 : 0.6);
            return;
        }
        if (!getNavigation().isDone()) {
            return;
        }
        if (noche && r == NpcDef.Rutina.NORMAL) {
            // a descansar: sentado cerca de su casa
            if (tickCount - lastActionTick > 220) {
                playAction("sit", 240);
            }
            return;
        }
        if (getRandom().nextInt(r == NpcDef.Rutina.QUIETO ? 10 : 5) == 0) {
            double ang = getRandom().nextDouble() * Math.PI * 2, rr = getRandom().nextDouble() * radio;
            Plat.walkTo(this, ancla.getX() + 0.5 + Math.cos(ang) * rr, ancla.getY(), ancla.getZ() + 0.5 + Math.sin(ang) * rr, 0.5);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (def() == null) {
            return super.mobInteract(player, hand);
        }
        if (hand != InteractionHand.MAIN_HAND) {
            return Plat.ok(level().isClientSide());
        }
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            hablandoCon(sp);
            playAction("talk", 50);
            NpcAcciones.abrir(sp, this);
        }
        return Plat.ok(level().isClientSide());
    }

    @Override
    public void axInfo(com.axeliscore.ui.InfoCard card, boolean detail) {
        NpcDef d = def();
        if (d == null) {
            super.axInfo(card, detail);
            return;
        }
        INFO.accept(card, d);
        card.hint(Component.translatable("axelis_core.npc.pista"));
    }

    @Override
    protected Component axInfoHint() {
        return def() == null ? super.axInfoHint() : Component.translatable("axelis_core.npc.pista");
    }

    // ------------------------------------------------------------------ guardar

    //#if MC >= 1.21.6
    //$$ @Override
    //$$ protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
    //$$     super.addAdditionalSaveData(out);
    //$$     out.putString("AxNpc", defId());
    //$$     if (casa != null) {
    //$$         out.putLong("AxCasa", casa.asLong());
    //$$     }
    //$$     if (trabajo != null) {
    //$$         out.putLong("AxTrabajo", trabajo.asLong());
    //$$     }
    //$$ }
    //$$
    //$$ @Override
    //$$ protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
    //$$     super.readAdditionalSaveData(in);
    //$$     setDef(in.getStringOr("AxNpc", ""));
    //$$     long c = in.getLongOr("AxCasa", Long.MIN_VALUE), t = in.getLongOr("AxTrabajo", Long.MIN_VALUE);
    //$$     casa = c == Long.MIN_VALUE ? null : BlockPos.of(c);
    //$$     trabajo = t == Long.MIN_VALUE ? null : BlockPos.of(t);
    //$$ }
    //#else
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AxNpc", defId());
        if (casa != null) {
            tag.putLong("AxCasa", casa.asLong());
        }
        if (trabajo != null) {
            tag.putLong("AxTrabajo", trabajo.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setDef(com.axeliscore.plat.Nbt.s(tag, "AxNpc"));
        casa = tag.contains("AxCasa") ? BlockPos.of(com.axeliscore.plat.Nbt.l(tag, "AxCasa")) : null;
        trabajo = tag.contains("AxTrabajo") ? BlockPos.of(com.axeliscore.plat.Nbt.l(tag, "AxTrabajo")) : null;
    }
    //#endif
}
