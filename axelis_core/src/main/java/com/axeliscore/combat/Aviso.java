package com.axeliscore.combat;

import com.axeliscore.fx.Fx;
import com.axeliscore.registry.CoreEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Ataque con aviso: una figura en el suelo (círculo, anillo, línea, cono o cuadro) que se va llenando y, al terminar, pega a lo que
 * esté dentro (daño, empujón, estado, sonido, partículas y temblor). Todos lo ven igual. Desde el servidor:
 * <pre>
 *     Aviso.circulo(level, objetivo.position(), 3.5F, 30).dano(jefe, 12).empuje(0.8, 0.5).estado(Estados.QUEMADO, 60, 1)
 *             .sonido("minecraft:entity.generic.explode", 1.2F, 0.8F).particulas("explosion").temblor(0.6F).lanzar();
 *     Aviso.linea(level, jefe.position(), jefe.getYRot(), 12, 2.5F, 25).dano(jefe, 9).lanzar();
 *     Aviso.circulo(level, p.position(), 2.5F, 40).seguir(p, 12).dano(jefe, 8).lanzar();   // lo persigue y se fija 12 ticks antes
 * </pre>
 * Saltar no salva (el daño llega hasta 2.5 bloques de alto), salvo que se pida {@link #alto}.
 */
public class Aviso extends Entity {
    public static final int CIRCULO = 0, ANILLO = 1, LINEA = 2, CONO = 3, CUADRO = 4;
    public static final int ROJO = 0xFFFF3A2E, NARANJA = 0xFFFF8A2A, MORADO = 0xFFB25CFF, AZUL = 0xFF5CC8FF, AMARILLO = 0xFFFFD23A;

    private static final EntityDataAccessor<Integer> FORMA = SynchedEntityData.defineId(Aviso.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> A = SynchedEntityData.defineId(Aviso.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> B = SynchedEntityData.defineId(Aviso.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIR = SynchedEntityData.defineId(Aviso.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(Aviso.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURA = SynchedEntityData.defineId(Aviso.class, EntityDataSerializers.INT);

    // ---- solo en el servidor (no se guarda: si se cierra el mundo, el aviso desaparece)
    private Entity dueno, sigue;
    private int fijarAntes;
    private float dano, temblor;
    private double empuje = 0.5, arriba = 0.35, alto = 2.5;
    private Estado estado;
    private int estadoTicks, estadoNivel = 1;
    private String sonido = "minecraft:entity.generic.explode", particulas = "explosion";
    private float sonidoVol = 1F, sonidoPitch = 1F;
    private Predicate<LivingEntity> filtro;
    private BiConsumer<Aviso, LivingEntity> alPegar;
    private Consumer<Aviso> alTerminar;
    private int edad;

    public Aviso(EntityType<? extends Aviso> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // ------------------------------------------------------------------ crear (servidor)

    private static Aviso nuevo(Level l, Vec3 p, int forma, float a, float b, float dir, int ticks) {
        Aviso av = new Aviso(CoreEntities.AVISO.get(), l);
        Vec3 s = Combate.alSuelo(l, p);
        av.setPos(s.x, s.y, s.z);
        av.entityData.set(FORMA, forma);
        av.entityData.set(A, a);
        av.entityData.set(B, b);
        av.entityData.set(DIR, dir);
        av.entityData.set(DURA, Math.max(1, ticks));
        av.entityData.set(COLOR, ROJO);
        return av;
    }

    /** Círculo de radio {@code radio} que pega al terminar {@code ticks}. */
    public static Aviso circulo(Level l, Vec3 centro, float radio, int ticks) {
        return nuevo(l, centro, CIRCULO, radio, 0, 0, ticks);
    }

    /** Anillo (dona): pega entre el radio de adentro y el de afuera (pararse en el centro salva). */
    public static Aviso anillo(Level l, Vec3 centro, float afuera, float adentro, int ticks) {
        return nuevo(l, centro, ANILLO, afuera, Math.min(adentro, afuera - 0.3F), 0, ticks);
    }

    /** Línea recta desde {@code origen} hacia {@code giro} (0 = sur), de {@code largo} por {@code ancho}. */
    public static Aviso linea(Level l, Vec3 origen, float giro, float largo, float ancho, int ticks) {
        return nuevo(l, origen, LINEA, largo, ancho, giro, ticks);
    }

    /** Cono (abanico) desde {@code origen} hacia {@code giro}, de {@code radio} y {@code angulo} grados de abierto en total. */
    public static Aviso cono(Level l, Vec3 origen, float giro, float radio, float angulo, int ticks) {
        return nuevo(l, origen, CONO, radio, Math.max(5, angulo) / 2F, giro, ticks);
    }

    /** Cuadro de {@code ancho} × {@code largo} con centro en {@code centro}, girado {@code giro}. */
    public static Aviso cuadro(Level l, Vec3 centro, float giro, float ancho, float largo, int ticks) {
        return nuevo(l, centro, CUADRO, ancho / 2F, largo / 2F, giro, ticks);
    }

    public Aviso color(int argb) {
        entityData.set(COLOR, argb);
        return this;
    }

    /** Daño al terminar (y quién lo hace: no se pega a sí mismo ni a sus aliados). */
    public Aviso dano(Entity quien, float cantidad) {
        this.dueno = quien;
        this.dano = cantidad;
        return this;
    }

    /** Empujón al pegar (hacia afuera del centro; en las líneas, hacia adelante). */
    public Aviso empuje(double fuerza, double haciaArriba) {
        this.empuje = fuerza;
        this.arriba = haciaArriba;
        return this;
    }

    public Aviso estado(Estado s, int ticks, int nivel) {
        this.estado = s;
        this.estadoTicks = ticks;
        this.estadoNivel = nivel;
        return this;
    }

    /** Sonido del golpe (nombre del sonido; "" = ninguno). */
    public Aviso sonido(String id, float vol, float pitch) {
        this.sonido = id;
        this.sonidoVol = vol;
        this.sonidoPitch = pitch;
        return this;
    }

    /** Partículas del golpe (ver {@link Combate#particula}). */
    public Aviso particulas(String nombre) {
        this.particulas = nombre;
        return this;
    }

    /** Temblor de pantalla para los que estén cerca al pegar. */
    public Aviso temblor(float fuerza) {
        this.temblor = fuerza;
        return this;
    }

    /** Hasta qué altura sobre el suelo pega (por defecto 2.5; con 0.9, saltar lo esquiva). */
    public Aviso alto(double bloques) {
        this.alto = bloques;
        return this;
    }

    /** Persigue a alguien (por el suelo) y se queda fijo {@code fijarAntes} ticks antes de pegar. */
    public Aviso seguir(Entity quien, int fijarAntes) {
        this.sigue = quien;
        this.fijarAntes = fijarAntes;
        return this;
    }

    /** Solo pega a quien cumpla esto (además de no ser aliado). */
    public Aviso soloA(Predicate<LivingEntity> f) {
        this.filtro = f;
        return this;
    }

    /** Algo extra por cada uno que le pegue (después del daño). */
    public Aviso alPegar(BiConsumer<Aviso, LivingEntity> c) {
        this.alPegar = c;
        return this;
    }

    /** Algo extra al terminar (siempre, le pegue a alguien o no): por ejemplo, otro aviso más grande. */
    public Aviso alTerminar(Consumer<Aviso> c) {
        this.alTerminar = c;
        return this;
    }

    /** Lo pone en el mundo (desde ese momento empieza a llenarse). */
    public Aviso lanzar() {
        level().addFreshEntity(this);
        return this;
    }

    // ------------------------------------------------------------------ lectura (las dos partes)

    public int forma() {
        return entityData.get(FORMA);
    }

    public float a() {
        return entityData.get(A);
    }

    public float b() {
        return entityData.get(B);
    }

    public float dir() {
        return entityData.get(DIR);
    }

    public int colorArgb() {
        return entityData.get(COLOR);
    }

    public int duracion() {
        return entityData.get(DURA);
    }

    public Entity dueno() {
        return dueno;
    }

    /** ¿Ese punto (del suelo) está dentro de la figura? {@code margen}: medio ancho de quien está ahí. */
    public boolean dentro(double x, double z, double margen) {
        double dx = x - getX(), dz = z - getZ();
        double r = Math.sqrt(dx * dx + dz * dz);
        float a = a(), b = b();
        Vec3 f = Combate.adelante(dir());
        double fw = dx * f.x + dz * f.z, sd = dx * f.z - dz * f.x;
        return switch (forma()) {
            case ANILLO -> r <= a + margen && r >= b - margen;
            case LINEA -> fw >= -margen && fw <= a + margen && Math.abs(sd) <= b / 2 + margen;
            case CONO -> {
                if (r > a + margen) {
                    yield false;
                }
                if (r < 0.6) {
                    yield true;
                }
                double ang = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, fw / r))));
                yield ang <= b + Math.toDegrees(Math.atan2(margen, r));
            }
            case CUADRO -> Math.abs(fw) <= b + margen && Math.abs(sd) <= a + margen;
            default -> r <= a + margen;
        };
    }

    // ------------------------------------------------------------------ cada tick

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        edad++;
        int dura = duracion();
        if (sigue != null && sigue.isAlive() && edad < dura - fijarAntes) {
            Vec3 s = Combate.alSuelo(level(), sigue.position());
            setPos(s.x, s.y, s.z);
        }
        if (edad >= dura) {
            pegar();
            discard();
        }
    }

    private void pegar() {
        Level l = level();
        Vec3 c = position();
        List<LivingEntity> golpeados = new ArrayList<>();
        double reach = switch (forma()) {
            case LINEA -> a() + 1;
            case CUADRO -> Math.hypot(a(), b()) + 1;
            default -> a() + 1;
        };
        AABB box = new AABB(c.x - reach, c.y - 1.0, c.z - reach, c.x + reach, c.y + alto, c.z + reach);
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!Combate.blanco(dueno, e) || (filtro != null && !filtro.test(e))) {
                continue;
            }
            if (e.getY() > c.y + alto || e.getY() + e.getBbHeight() < c.y - 0.5) {
                continue;
            }
            if (!dentro(e.getX(), e.getZ(), e.getBbWidth() / 2)) {
                continue;
            }
            golpeados.add(e);
        }
        for (LivingEntity e : golpeados) {
            if (dano > 0) {
                com.axeliscore.plat.Plat.sinInvulnerable(e);
                Combate.danar(e, Combate.fuente(l, dueno), dano);
            }
            if (empuje != 0 || arriba != 0) {
                if (forma() == LINEA) {
                    Vec3 f = Combate.adelante(dir());
                    com.axeliscore.plat.Plat.push(e, f.x * empuje, arriba, f.z * empuje);
                } else {
                    Combate.empujarDesde(e, c, empuje, arriba);
                }
            }
            if (estado != null && estadoTicks > 0) {
                Estados.aplicar(e, estado, estadoTicks, estadoNivel, dueno);
            }
            if (alPegar != null) {
                alPegar.accept(this, e);
            }
        }
        Combate.sonido(l, c, sonido, sonidoVol, sonidoPitch);
        if (particulas != null && !particulas.isEmpty()) {
            switch (forma()) {
                case CIRCULO, ANILLO -> {
                    Combate.anillo(l, particulas.equals("explosion") ? "cloud" : particulas, c, a() * 0.9, Math.min(48, 10 + (int) (a() * 6)));
                    if (particulas.equals("explosion")) {
                        Combate.particulas(l, "explosion", c.add(0, 0.5, 0), Math.max(1, (int) (a() / 1.5)), a() * 0.4, 0.2, a() * 0.4, 0);
                    }
                }
                case LINEA -> {
                    Vec3 f = Combate.adelante(dir());
                    for (float d = 0.5F; d <= a(); d += 1F) {
                        Combate.particulas(l, particulas, c.add(f.x * d, 0.4, f.z * d), 2, b() * 0.25, 0.1, b() * 0.25, 0.01);
                    }
                }
                default -> Combate.particulas(l, particulas, c.add(0, 0.4, 0), 12, a() * 0.4, 0.2, a() * 0.4, 0.02);
            }
        }
        if (temblor > 0) {
            Fx.shakeNear(this, Math.max(8, a() * 3), temblor, 10);
        }
        if (alTerminar != null) {
            alTerminar.accept(this);
        }
    }

    // ------------------------------------------------------------------ lo de siempre de una entidad

    //#if MC >= 1.20.5
    //$$ @Override
    //$$ protected void defineSynchedData(SynchedEntityData.Builder b) {
    //$$     b.define(FORMA, CIRCULO);
    //$$     b.define(A, 2F);
    //$$     b.define(B, 0F);
    //$$     b.define(DIR, 0F);
    //$$     b.define(COLOR, ROJO);
    //$$     b.define(DURA, 20);
    //$$ }
    //#else
    @Override
    protected void defineSynchedData() {
        this.entityData.define(FORMA, CIRCULO);
        this.entityData.define(A, 2F);
        this.entityData.define(B, 0F);
        this.entityData.define(DIR, 0F);
        this.entityData.define(COLOR, ROJO);
        this.entityData.define(DURA, 20);
    }
    //#endif

    //#if MC >= 1.21.6
    //$$ @Override
    //$$ protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
    //$$ }
    //$$
    //$$ @Override
    //$$ protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
    //$$ }
    //#else
    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
    }
    //#endif

    //#if MC >= 1.21.2
    //$$ @Override
    //$$ public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
    //$$     return false;
    //$$ }
    //#endif

    //#if MC < 1.20.5
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
    }
    //#endif

    /** No se guarda con el mundo. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
