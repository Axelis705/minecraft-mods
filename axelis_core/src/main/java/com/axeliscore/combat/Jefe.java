package com.axeliscore.combat;

import com.axeliscore.character.CoreMob;
import com.axeliscore.fx.Fx;
import com.axeliscore.plat.Plat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base de los jefes del Core: fases por vida (con sus ataques, color de barra, aspecto, música y una entrada con título, animación y
 * temblor), elección de ataques por distancia y peso, barra de jefe propia, resistencia a estados y música para los que estén cerca.
 * Un jefe nuevo solo dice sus fases:
 * <pre>
 * public class MiJefe extends Jefe {
 *     private static final List&lt;Fase&gt; FASES = List.of(
 *             new Fase(1F).ataque(golpe).ataque(salto).musica("mimod:musica.jefe_1"),
 *             new Fase(0.5F).color(BossEvent.BossBarColor.RED).titulo(Component.translatable("mimod.jefe.furia"), 0xFF5030)
 *                     .entrada("roar", 40).dano(1.3F).rapidez(1.3F).aspecto(1).ataque(golpe).ataque(salto).ataque(lluvia));
 *     protected List&lt;Fase&gt; fases() { return FASES; }
 * }
 * </pre>
 * La IA de moverse y buscar objetivo es la de Minecraft (se puede cambiar en {@code registerGoals}).
 */
public abstract class Jefe extends CoreMob implements Estados.Resiste {
    private static final EntityDataAccessor<Integer> FASE = SynchedEntityData.defineId(Jefe.class, EntityDataSerializers.INT);
    /** Ticks que le quedan a la entrada de una fase (sin recibir daño). */
    private static final EntityDataAccessor<Integer> ENTRADA = SynchedEntityData.defineId(Jefe.class, EntityDataSerializers.INT);

    protected final ServerBossEvent barra;
    private int descanso = 40, ocupado, golpeEn = -1, desdeAtaque;
    private Ataque actual;
    private LivingEntity objetivoAtaque;
    private final Map<String, Integer> esperas = new HashMap<>();

    protected Jefe(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        //#if MC >= 26.1
        //$$ this.barra = new ServerBossEvent(java.util.UUID.randomUUID(), type.getDescription(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        //#else
        this.barra = new ServerBossEvent(type.getDescription(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        //#endif
    }

    /** Las fases en orden (la primera con {@code desde = 1}). Mejor una lista {@code static final}: se pide seguido. */
    protected abstract List<Fase> fases();

    // ------------------------------------------------------------------ lo que se puede preguntar

    /** Número de fase (0, 1, 2...), igual en el servidor y en el cliente. */
    public int fase() {
        return entityData.get(FASE);
    }

    public Fase faseActual() {
        List<Fase> f = fases();
        return f.get(Math.max(0, Math.min(f.size() - 1, fase())));
    }

    /** Aspecto (textura) de la fase actual, para el renderizador. */
    public int aspecto() {
        return faseActual().aspecto;
    }

    /** Música de la fase actual ("" = ninguna). */
    public String musica() {
        return faseActual().musica;
    }

    /** ¿Está entrando a una fase? (no recibe daño). */
    public boolean entrando() {
        return entityData.get(ENTRADA) > 0;
    }

    /** Daño de un ataque con el multiplicador de la fase. */
    public float dano(float base) {
        return base * faseActual().dano;
    }

    /** Busca un ataque por su nombre en todas las fases (null si no existe). */
    public Ataque buscarAtaque(String id) {
        for (Fase f : fases()) {
            for (Ataque a : f.ataques) {
                if (a.id.equals(id)) {
                    return a;
                }
            }
        }
        return null;
    }

    /** El ataque que está haciendo (null si ninguno). */
    public Ataque ataqueActual() {
        return actual;
    }

    // ------------------------------------------------------------------ IA

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            return;
        }
        barra.setProgress(Math.max(0, getHealth() / getMaxHealth()));
        if (isNoAi() || deathTime > 0 || showcasing()) {
            return;
        }
        int entrada = entityData.get(ENTRADA);
        if (entrada > 0) {
            entityData.set(ENTRADA, entrada - 1);
            getNavigation().stop();
            return;
        }
        for (Map.Entry<String, Integer> e : esperas.entrySet()) {
            if (e.getValue() > 0) {
                e.setValue(e.getValue() - 1);
            }
        }
        if (actual != null) {
            desdeAtaque++;
            if (golpeEn >= 0 && desdeAtaque >= golpeEn) {
                golpeEn = -1;
                if (actual.alGolpe != null) {
                    correr(() -> actual.alGolpe.run(this, objetivoAtaque));
                }
            }
            if (actual != null && actual.mientras != null) {
                Ataque a = actual;
                correr(() -> a.mientras.run(this, objetivoAtaque, desdeAtaque));
            }
            if (actual != null && actual.quieto) {
                getNavigation().stop();
            }
            if (--ocupado > 0) {
                return;
            }
            actual = null;
        }
        // ¿pasó a otra fase?
        int nueva = faseSegunVida();
        if (nueva > fase()) {
            cambiarFase(nueva);
            return;
        }
        if (Estados.aturdido(this)) {
            return;
        }
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) {
            return;
        }
        getLookControl().setLookAt(t, 30F, 30F);
        double d = distanceTo(t);
        if (--descanso > 0) {
            if (d > alcanceCaminar()) {
                Plat.walkTo(this, t, faseActual().caminar);
            }
            return;
        }
        Ataque a = elegir(d);
        if (a == null) {
            Plat.walkTo(this, t, faseActual().caminar);
            descanso = 10;
            return;
        }
        empezar(a, t);
    }

    /** Distancia a la que deja de caminar hacia el objetivo (mientras descansa). */
    protected double alcanceCaminar() {
        return 3.2;
    }

    private void correr(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException ex) {
            com.axeliscore.AxelisCore.LOGGER.error("Axelis Core: falló un ataque de {}", getType().getDescription().getString(), ex);
        }
    }

    private int faseSegunVida() {
        float k = getHealth() / Math.max(1, getMaxHealth());
        List<Fase> f = fases();
        int r = 0;
        for (int i = 0; i < f.size(); i++) {
            if (k <= f.get(i).desde + 1.0E-4F) {
                r = i;
            }
        }
        return r;
    }

    /** Elige un ataque de la fase que sirva a esa distancia (por peso, sin repetir los que esperan). */
    protected Ataque elegir(double d) {
        List<Ataque> ok = new ArrayList<>();
        int total = 0;
        for (Ataque a : faseActual().ataques) {
            if (d >= a.min && d <= a.max && esperas.getOrDefault(a.id, 0) <= 0) {
                ok.add(a);
                total += a.peso;
            }
        }
        if (ok.isEmpty()) {
            return null;
        }
        int r = getRandom().nextInt(total);
        for (Ataque a : ok) {
            r -= a.peso;
            if (r < 0) {
                return a;
            }
        }
        return ok.get(0);
    }

    /** Empieza un ataque ya elegido (también sirve para forzar uno desde el código o un comando). */
    public void empezar(Ataque a, LivingEntity t) {
        actual = a;
        objetivoAtaque = t;
        desdeAtaque = 0;
        ocupado = a.dura;
        golpeEn = a.golpe;
        esperas.put(a.id, a.espera);
        descanso = Math.max(5, Math.round(a.despues / faseActual().rapidez));
        if (a.quieto) {
            getNavigation().stop();
        }
        if (t != null) {
            getLookControl().setLookAt(t, 90F, 90F);
            setYRot(Combate.giro(position(), t.position()));
            this.yBodyRot = getYRot();
            this.yHeadRot = getYRot();
        }
        if (!a.clip.isEmpty()) {
            playAction(a.clip);
        }
        if (a.alEmpezar != null) {
            correr(() -> a.alEmpezar.run(this, t));
        }
    }

    /** Entra a la fase {@code n}: animación, título, temblor, barra, empujón a los que estén muy cerca y lo extra de la fase. */
    protected void cambiarFase(int n) {
        entityData.set(FASE, n);
        Fase f = faseActual();
        actual = null;
        golpeEn = -1;
        Estados.limpiar(this);
        barra.setColor(f.color);
        entityData.set(ENTRADA, Math.max(1, f.duracionEntrada));
        getNavigation().stop();
        if (!f.clipEntrada.isEmpty()) {
            playAction(f.clipEntrada);
        }
        Combate.sonido(level(), position(), f.sonidoEntrada, 2F, 0.8F);
        for (Player p : level().players()) {
            if (p instanceof ServerPlayer sp && sp.distanceTo(this) < 40) {
                if (f.titulo != null) {
                    Fx.title(sp, f.titulo, f.subtitulo == null ? Component.empty() : f.subtitulo, f.colorTitulo);
                }
                Fx.shake(sp, 0.9F, 30);
                Fx.flash(sp, 0x50000000 | (f.colorTitulo & 0xFFFFFF), 12);
                if (sp.distanceTo(this) < 6) {
                    Combate.empujarDesde(sp, position(), 1.4, 0.5);
                }
            }
        }
        descanso = 30;
        if (f.alEntrar != null) {
            correr(() -> f.alEntrar.accept(this));
        }
        com.axeliscore.AxelisCore.LOGGER.info("AXCORE_JEFE_FASE {} fase={}", getType().getDescription().getString(), n);
    }

    /** Pone la fase sin la entrada (para pruebas y comandos). */
    public void forzarFase(int n) {
        int max = fases().size() - 1;
        n = Math.max(0, Math.min(max, n));
        if (n > fase()) {
            cambiarFase(n);
        } else {
            entityData.set(FASE, n);
            barra.setColor(faseActual().color);
        }
    }

    /** Los estados le duran menos: aturdido 25 %, congelado 50 %, lo demás normal. */
    @Override
    public float axDuracionEstado(Estado s) {
        if (s.aturdeAlQueLoTiene()) {
            return 0.25F;
        }
        return s == Estados.CONGELADO ? 0.5F : 1F;
    }

    /** (Lo llama el Core al recibir daño) mientras entra a una fase no recibe daño. */
    public float alRecibirDano(float amount) {
        return entrando() ? 0F : amount;
    }

    // ------------------------------------------------------------------ barra

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        barra.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        barra.removePlayer(p);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        if (name != null) {
            barra.setName(name);
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    // ------------------------------------------------------------------ datos

    //#if MC >= 1.20.5
    //$$ @Override
    //$$ protected void defineSynchedData(SynchedEntityData.Builder b) {
    //$$     super.defineSynchedData(b);
    //$$     b.define(FASE, 0);
    //$$     b.define(ENTRADA, 0);
    //$$ }
    //#else
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FASE, 0);
        this.entityData.define(ENTRADA, 0);
    }
    //#endif

    //#if MC >= 1.21.6
    //$$ @Override
    //$$ protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
    //$$     super.addAdditionalSaveData(out);
    //$$     out.putInt("AxFase", fase());
    //$$ }
    //$$
    //$$ @Override
    //$$ protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
    //$$     super.readAdditionalSaveData(in);
    //$$     entityData.set(FASE, in.getIntOr("AxFase", 0));
    //$$     barra.setColor(faseActual().color);
    //$$ }
    //#else
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AxFase", fase());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FASE, com.axeliscore.plat.Nbt.i(tag, "AxFase"));
        barra.setColor(faseActual().color);
        if (hasCustomName()) {
            barra.setName(getDisplayName());
        }
    }
    //#endif

    // ------------------------------------------------------------------ tarjeta

    @Override
    public void axInfo(com.axeliscore.ui.InfoCard card, boolean detail) {
        card.rareza(com.axeliscore.ui.Rareza.LEGENDARIO).tag(Component.translatable("axelis_core.info.jefe"));
        super.axInfo(card, detail);
        if (fases().size() > 1) {
            card.pair(Component.translatable("axelis_core.info.fase"), Component.literal((fase() + 1) + "/" + fases().size()));
        }
    }
}
