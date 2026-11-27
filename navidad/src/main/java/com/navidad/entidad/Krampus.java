package com.navidad.entidad;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.combat.Ataque;
import com.axeliscore.combat.Aviso;
import com.axeliscore.combat.Combate;
import com.axeliscore.combat.Estados;
import com.axeliscore.combat.Fase;
import com.axeliscore.combat.Jefe;
import com.axeliscore.combat.Proyectil;
import com.axeliscore.fx.Efectos;
import com.axeliscore.fx.Fx;
import com.navidad.Logros;
import com.navidad.Navidad;
import com.navidad.bloque.FarolNochebuena;
import com.navidad.krampus.SacoDelKrampus;
import com.navidad.objeto.MascaraKrampus;
import com.navidad.objeto.Premios;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * El Krampus: el jefe de Navidad (modelo hecho a mano, ver herramientas/krampus.py). Da miedo de verdad: se oyen sus cadenas y sus
 * pezuñas antes de verlo, trae niebla y en la última fase apaga la luz. Tres fases:
 * <ol>
 *     <li><b>La visita</b> (100 a 65 %): varazos con la vara de abedul (tres golpes), la cadena con gancho que te jala hacia él,
 *     pisotón (salta para esquivarlo), carbones encendidos y un rugido que llama krampusillos.</li>
 *     <li><b>¡Al saco!</b> (65 a 30 %): se enfurece; además brinca y te mete en su saco (dos a la vez), da vueltas con la cadena y
 *     avienta cascabeles malditos que persiguen.</li>
 *     <li><b>Apagón</b> (30 a 0 %): todo se oscurece y pone cuatro faroles; lejos de la luz se esconde (solo se le ven los ojos y casi no
 *     le hace daño nada), aparece detrás de alguien, y va a soplarle a los faroles. Si alguien enciende un farol junto a él, o toca la
 *     campanita de Santa, se deslumbra.</li>
 * </ol>
 * <b>El saco</b>: el que está adentro va en su espalda; cada segundo lo aprieta. Se sale machacando la tecla de saltar (o agacharse) o
 * cuando sus amigos le pegan al saco por la espalda (se rompe: lo aturde y suelta regalos). Si nadie hace nada, a los 9 segundos lo
 * avienta. Pegarle por la espalda sin nadie adentro también le rompe el saco.
 * <p>Al vencerlo revienta el saco con todos los regalos robados por los krampusillos, y cada quien recibe regalos épicos y una pieza
 * de su botín. Si viene de visita (o lo llamaron con la campana) y amanece al aire libre, o si se queda solo, se va riéndose.
 */
public class Krampus extends Jefe {
    static final float VARA_DMG = 5, CADENA_DMG = 4, PISOTON_DMG = 6, GIRO_DMG = 8, EMBOSCADA_DMG = 10, APRETON_DMG = 1.5F;
    /** Cuánto daño por la espalda aguanta el saco. */
    static final float SACO_MAX = 30F;
    static final int SACO_GENTE = 2, SACO_TIEMPO = 180, FORCEJEOS = 12, SACO_REGRESA = 500;
    /** Hasta dónde lo alumbra un farol (bloques). */
    public static final double LUZ = 4.5;

    private static final EntityDataAccessor<Boolean> OCULTO = SynchedEntityData.defineId(Krampus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SIN_SACO = SynchedEntityData.defineId(Krampus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GANCHO_FUERA = SynchedEntityData.defineId(Krampus.class, EntityDataSerializers.BOOLEAN);

    /** Mientras el Krampus aprieta a alguien en su saco (ese daño sí pasa; los demás no le llegan al de adentro). */
    public static boolean apretando;
    /** Para la prueba con fotos y el registro. */
    public static int ensacadosTotal, sacosRotos, escapados, emboscadas, farolesApagados;

    /** Los que van en el saco: {ticks adentro, veces que forcejeó, último intento (tick)}. */
    private final Map<UUID, int[]> ensacados = new LinkedHashMap<>();
    private final Set<UUID> peleadores = new HashSet<>();
    private final List<BlockPos> farolesPuestos = new ArrayList<>();
    private final List<BlockPos> farolesCerca = new ArrayList<>();
    private float sacoVida = SACO_MAX;
    private int revelado, sinNadie, soploEn = -1, quemadoAviso, sinSacoTicks;
    private boolean enLaLuz, agarroAlguien;
    private BlockPos farolObjetivo;
    private Vec3 apuntaGancho;
    /** Si vino de visita o con la campana: al amanecer al aire libre se va. */
    public boolean seVaDeDia;

    // ------------------------------------------------------------------ ataques

    private static Krampus k(Jefe j) {
        return (Krampus) j;
    }

    private static final Ataque NADA = new Ataque("nada").dura(2).golpe(99).distancia(999, 999).espera(0).despues(12);
    /** Tres golpes con la vara de abedul, en cono al frente (0.5, 0.9 y 1.4 s). */
    private static final Ataque VARAZOS = new Ataque("varazos").clip("varazos").dura(32).golpe(99).distancia(0, 5.5F).peso(4).espera(30)
            .despues(26).mientras((j, t, tick) -> {
                if (tick == 2 || tick == 10 || tick == 20) {
                    k(j).varazo(t, tick == 20);
                }
            });
    /** La cadena con gancho: avisa con una línea y la avienta por ahí (hazte a un lado); si te agarra, te jala hacia él. */
    private static final Ataque CADENA = new Ataque("cadena").clip("cadena").dura(28).golpe(14).distancia(5, 20).peso(3).espera(90).despues(26)
            .alEmpezar((j, t) -> k(j).avisarGancho(t)).alGolpe((j, t) -> k(j).lanzarGancho());
    private static final Ataque PISOTON = new Ataque("pisoton").clip("pisoton").dura(22).golpe(12).distancia(2, 9).peso(2).espera(70)
            .despues(36).alEmpezar((j, t) -> Aviso.anillo(j.level(), j.position(), 7F, 1.8F, 12).alto(0.9).color(Aviso.AMARILLO)
                    .dano(j, j.dano(PISOTON_DMG)).empuje(1.0, 0.45).particulas("snow").temblor(0.4F).lanzar());
    private static final Ataque CARBONES = new Ataque("carbones").clip("lanza").dura(28).golpe(15).distancia(6, 22).peso(2).espera(80)
            .despues(32).alGolpe((j, t) -> {
                if (t == null) {
                    return;
                }
                Vec3 desde = j.position().add(0, 2.8, 0);
                for (int i = -1; i <= 1; i++) {
                    Proyectil p = Proyectil.hacia(j.level(), Proyectiles.CARBON, j, desde, t, 0.85F);
                    if (i != 0) {
                        Vec3 v = p.getDeltaMovement();
                        double a = Math.toRadians(14 * i), c = Math.cos(a), s = Math.sin(a);
                        p.setDeltaMovement(new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c));
                    }
                }
            });
    private static final Ataque RUGIDO = new Ataque("rugido").clip("rugido").dura(44).golpe(8).distancia(0, 16).peso(1).espera(320)
            .despues(50).alGolpe((j, t) -> k(j).rugir());
    /** Fase 2: brinca y mete a alguien en el saco. */
    private static final Ataque AGARRE = new Ataque("agarre").clip("agarre").dura(36).golpe(18).distancia(2.5F, 12).peso(3).espera(170)
            .despues(36).alEmpezar((j, t) -> k(j).avisarAgarre(t)).mientras((j, t, tick) -> {
                if (tick == 11) {
                    k(j).brincarHacia(t);
                }
            });
    /** Fase 2: da vueltas con la cadena. */
    private static final Ataque GIRO = new Ataque("giro").clip("giro").dura(32).golpe(16).distancia(0, 6).peso(2).espera(140).despues(32)
            .alEmpezar((j, t) -> Aviso.circulo(j.level(), j.position(), 6.5F, 16).color(Aviso.ROJO).dano(j, j.dano(GIRO_DMG)).empuje(1.3, 0.4)
                    .particulas("crit").temblor(0.3F).lanzar());
    /** Fase 2: cascabeles malditos que persiguen a cada quien. */
    private static final Ataque CASCABELES = new Ataque("cascabeles").clip("risa").dura(32).golpe(8).distancia(4, 24).peso(2).espera(150)
            .despues(36).alGolpe((j, t) -> {
                int n = 0;
                for (Player p : j.level().players()) {
                    if (n < 3 && (p.distanceTo(j) < 26 && Combate.blanco(j, p) || p == t)) {
                        Proyectil.hacia(j.level(), Proyectiles.CASCABEL, j, j.position().add(0, 3.0, 0), p, 0.45F).persiguiendo(p);
                        n++;
                    }
                }
            });
    /** Fase 3: va a un farol encendido y lo apaga de un soplido (mientras camina hacia él, la luz lo deja a la vista). */
    private static final Ataque SOPLIDO = new Ataque("soplido").dura(120).golpe(999).distancia(0, 64).peso(3).espera(150).despues(16)
            .caminando().mientras((j, t, tick) -> k(j).irASoplar(t, tick));
    /** Fase 3: aparece detrás de alguien desde la oscuridad. */
    private static final Ataque EMBOSCADA = new Ataque("emboscada").clip("emboscada").dura(24).golpe(99).distancia(0, 30).peso(3).espera(110)
            .despues(30).alEmpezar((j, t) -> k(j).emboscar(t));

    private static final List<Fase> FASES = List.of(
            new Fase(1F).color(BossEvent.BossBarColor.PURPLE).musica(Navidad.MODID + ":krampus_musica")
                    .ataque(VARAZOS).ataque(CADENA).ataque(PISOTON).ataque(CARBONES).ataque(RUGIDO),
            new Fase(0.65F).color(BossEvent.BossBarColor.RED).titulo(Component.translatable("navidad.krampus.fase2"), 0xFF3A2A)
                    .subtitulo(Component.translatable("navidad.krampus.fase2_sub")).entrada("fase", 52).dano(1.2F).rapidez(1.3F)
                    .caminar(1.15F).aspecto(1).musica(Navidad.MODID + ":krampus_musica")
                    .ataque(VARAZOS).ataque(CADENA).ataque(AGARRE).ataque(GIRO).ataque(PISOTON).ataque(CARBONES).ataque(CASCABELES)
                    .ataque(RUGIDO),
            new Fase(0.3F).color(BossEvent.BossBarColor.WHITE).titulo(Component.translatable("navidad.krampus.fase3"), 0xB8B0FF)
                    .subtitulo(Component.translatable("navidad.krampus.fase3_sub")).entrada("apagon", 52).dano(1.3F).rapidez(1.4F)
                    .caminar(1.3F).aspecto(2).musica(Navidad.MODID + ":krampus_apagon_musica").alEntrar(j -> k(j).apagon())
                    .ataque(VARAZOS).ataque(CADENA).ataque(AGARRE).ataque(SOPLIDO).ataque(EMBOSCADA).ataque(GIRO));

    public Krampus(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 400.0).add(Attributes.ARMOR, 8.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected List<Fase> fases() {
        return FASES;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OCULTO, false);
        this.entityData.define(SIN_SACO, false);
        this.entityData.define(GANCHO_FUERA, false);
    }

    // ------------------------------------------------------------------ lo que se puede preguntar

    /** (Las dos partes) En el apagón y lejos de la luz: solo se le ven los ojos. */
    public boolean oculto() {
        return entityData.get(OCULTO);
    }

    /** La textura: escondido en lo oscuro usa la de la sombra (y el dibujo lo pinta todo negro menos los ojos). */
    @Override
    public int aspecto() {
        return oculto() ? 2 : super.aspecto();
    }

    /** Ticks que le faltan para tener saco otra vez (0 = tiene saco). */
    public int sinSaco() {
        return level().isClientSide ? (entityData.get(SIN_SACO) ? 1 : 0) : sinSacoTicks;
    }

    public boolean ganchoFuera() {
        return entityData.get(GANCHO_FUERA);
    }

    public boolean ensacado(UUID u) {
        return ensacados.containsKey(u);
    }

    public int cuantosEnSaco() {
        return ensacados.size();
    }

    public float sacoVida() {
        return sacoVida;
    }

    @Override
    public boolean isAlliedTo(Entity e) {
        return e instanceof Krampusillo || e instanceof MunecoMalvado || super.isAlliedTo(e);
    }

    @Override
    protected Ataque elegir(double d) {
        for (int intento = 0; intento < 4; intento++) {
            Ataque a = super.elegir(d);
            if (a == null) {
                return null;
            }
            if (a == SOPLIDO && farolEncendidoMasCerca(24) == null || a == EMBOSCADA && !oculto()
                    || a == AGARRE && (ensacados.size() >= SACO_GENTE || sinSaco() > 0)) {
                continue;
            }
            return a;
        }
        return null;
    }

    // ------------------------------------------------------------------ varazos, cadena, rugido

    private void mirarA(LivingEntity t) {
        if (t != null && t.isAlive()) {
            setYRot(Combate.giro(position(), t.position()));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    void varazo(LivingEntity t, boolean ultimo) {
        mirarA(t);
        Aviso.cono(level(), position(), getYRot(), 5.4F, 110, 8).color(Aviso.NARANJA).dano(this, dano(VARA_DMG))
                .empuje(ultimo ? 1.2 : 0.35, ultimo ? 0.4 : 0.1).particulas("crit").lanzar();
    }

    /** Dónde sale el gancho: su mano izquierda, un poco al frente. */
    private Vec3 manoGancho() {
        Vec3 lado = Vec3.directionFromRotation(0, yBodyRot - 90F).scale(0.8);
        return position().add(lado).add(0, 1.8, 0).add(Vec3.directionFromRotation(0, yBodyRot).scale(0.6));
    }

    /** Avisa con una línea desde su mano hasta donde estás AHORA: ahí va a llegar el gancho (hazte a un lado). */
    void avisarGancho(LivingEntity t) {
        if (t == null) {
            apuntaGancho = null;
            return;
        }
        mirarA(t);
        apuntaGancho = t.position().add(0, t.getBbHeight() * 0.5, 0);
        Vec3 mano = manoGancho();
        float largo = (float) Math.min(24, Math.sqrt(mano.distanceToSqr(apuntaGancho.x, mano.y, apuntaGancho.z)) + 3);
        Aviso.linea(level(), new Vec3(mano.x, getY(), mano.z), Combate.giro(mano, apuntaGancho), largo, 1.6F, 14).color(Aviso.MORADO).lanzar();
    }

    void lanzarGancho() {
        Vec3 desde = manoGancho();
        Vec3 dir = apuntaGancho == null ? Vec3.directionFromRotation(0, yBodyRot) : apuntaGancho.subtract(desde);
        if (dir.lengthSqr() < 1.0E-4) {
            dir = Vec3.directionFromRotation(0, yBodyRot);
        }
        GanchoCadena g = GanchoCadena.lanzar(level(), this, desde, dir.normalize(), 1.5F, 24);
        if (g != null) {
            entityData.set(GANCHO_FUERA, true);
        }
    }

    /** (Lo llama el gancho) Ya regresó el gancho; si trajo a alguien, le toca una tanda de varazos. */
    void ganchoRegreso(LivingEntity jalado) {
        entityData.set(GANCHO_FUERA, false);
        if (jalado != null && jalado.isAlive() && ataqueActual() == null && distanceTo(jalado) < 5) {
            empezar(VARAZOS, jalado);
        }
    }

    void rugir() {
        for (Player p : level().players()) {
            if (p.distanceTo(this) < 9 && Combate.blanco(this, p)) {
                Combate.empujarDesde(p, position(), 1.3, 0.35);
            }
        }
        Fx.shakeNear(this, 18, 0.5F, 20);
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        int ya = sl.getEntitiesOfClass(Krampusillo.class, getBoundingBox().inflate(24)).size();
        for (int i = 0; i < 2 && ya + i < 4; i++) {
            double a = Math.toRadians(getYRot() + (i == 0 ? 70 : -70));
            Vec3 p = position().add(-Math.sin(a) * 4, 0, Math.cos(a) * 4);
            Krampusillo.llamar(sl, p, getTarget(), null);
        }
    }

    // ------------------------------------------------------------------ el saco

    void avisarAgarre(LivingEntity t) {
        agarroAlguien = false;
        if (t == null) {
            return;
        }
        Aviso.circulo(level(), t.position(), 2.3F, 18).seguir(t, 7).color(Aviso.MORADO).alto(2.5)
                .alPegar((av, e) -> agarrar(e)).particulas("smoke").lanzar();
    }

    void brincarHacia(LivingEntity t) {
        if (t == null || !t.isAlive()) {
            return;
        }
        Vec3 d = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ());
        double dist = d.length();
        if (dist < 0.5) {
            return;
        }
        d = d.scale(1 / dist);
        double f = Math.min(1.4, dist * 0.13);
        setDeltaMovement(d.x * f, 0.45, d.z * f);
        hasImpulse = true;
    }

    private void agarrar(LivingEntity e) {
        if (e instanceof ServerPlayer p && !agarroAlguien && meterAlSaco(p)) {
            agarroAlguien = true;
            return;
        }
        Combate.danar(e, Combate.fuente(level(), this), dano(6));
    }

    /** (Servidor) Mete a alguien en el saco. Devuelve false si no cupo. */
    public boolean meterAlSaco(ServerPlayer p) {
        if (ensacados.size() >= SACO_GENTE || sinSaco() > 0 || p.isPassenger() || p.isCreative() || p.isSpectator() || !isAlive()) {
            return false;
        }
        ensacados.put(p.getUUID(), new int[] {0, 0, -10});
        if (!p.startRiding(this, true)) {
            ensacados.remove(p.getUUID());
            return false;
        }
        ensacadosTotal++;
        playAction("risa", 0);
        ServerLevel sl = (ServerLevel) level();
        sl.playSound(null, blockPosition(), ModRegistry.KRAMPUS_SACO.get(), SoundSource.HOSTILE, 2F, 0.8F);
        Fx.title(p, Component.translatable("navidad.krampus.saco.titulo"), Component.empty(), 0xC89060);
        Fx.shake(p, 0.6F, 20);
        for (ServerPlayer o : sl.players()) {
            if (o != p && o.distanceTo(this) < 48) {
                o.sendSystemMessage(Component.translatable("navidad.krampus.saco.otros", p.getDisplayName())
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
        }
        Navidad.LOGGER.info("NAVIDAD_KRAMPUS_SACO metió a {}", p.getGameProfile().getName());
        return true;
    }

    /** (Servidor) El de adentro forcejea (machacó saltar, o intentó bajarse). */
    public void forcejear(ServerPlayer p, int veces) {
        int[] e = ensacados.get(p.getUUID());
        if (e == null || veces <= 0) {
            return;
        }
        e[1] += veces;
        if (e[1] >= FORCEJEOS) {
            escapados++;
            liberar(p, true);
            playAction("hurt", 0);
            return;
        }
        if (tickCount % 3 == 0) {
            level().playSound(null, blockPosition(), ModRegistry.KRAMPUS_SACO.get(), SoundSource.HOSTILE, 0.6F, 1.3F + random.nextFloat() * 0.3F);
        }
    }

    /** (Lo llama el evento de bajarse) Agacharse adentro del saco también cuenta como forcejear (una vez por apretón de tecla). */
    public void intentoBajarse(ServerPlayer p) {
        int[] e = ensacados.get(p.getUUID());
        if (e != null && tickCount - e[2] > 3) {
            forcejear(p, 1);
        }
        if (e != null) {
            e[2] = tickCount;
        }
    }

    private void liberar(ServerPlayer p, boolean seEscapo) {
        ensacados.remove(p.getUUID());
        Vec3 atras = Vec3.directionFromRotation(0, yBodyRot).scale(-1.6);
        bajarEn = new Vec3(getX() + atras.x, getY() + 0.2, getZ() + atras.z);
        if (p.getVehicle() == this) {
            p.stopRiding();
        }
        bajarEn = null;
        Plat.push(p, atras.x * 0.25, 0.45, atras.z * 0.25);
        if (seEscapo) {
            Fx.title(p, Component.translatable("navidad.krampus.saco.escapaste"), Component.empty(), 0x9CE07A);
            Logros.dar(p, "saco");
            Navidad.LOGGER.info("NAVIDAD_KRAMPUS_SACO {} se escapó", p.getGameProfile().getName());
        }
        level().playSound(null, blockPosition(), ModRegistry.KRAMPUS_SACO.get(), SoundSource.HOSTILE, 1.2F, 1.1F);
    }

    /** Lo avienta fuera del saco (si nadie lo sacó a tiempo). */
    private void aventar(ServerPlayer p) {
        ensacados.remove(p.getUUID());
        Vec3 f = Vec3.directionFromRotation(0, yBodyRot);
        bajarEn = new Vec3(getX() + f.x * 1.6, getY() + 0.4, getZ() + f.z * 1.6);
        p.stopRiding();
        bajarEn = null;
        Plat.push(p, f.x * 1.5, 0.7, f.z * 1.5);
        Combate.danar(p, Combate.fuente(level(), this), dano(4));
        playAction("risa", 0);
    }

    private void liberarTodos() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        for (UUID u : new ArrayList<>(ensacados.keySet())) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(u);
            if (p != null) {
                liberar(p, false);
            }
        }
        ensacados.clear();
    }

    private void tickSaco(ServerLevel sl) {
        if (sinSacoTicks > 0 && --sinSacoTicks == 0) {
            entityData.set(SIN_SACO, false);
        }
        if (ensacados.isEmpty()) {
            return;
        }
        for (UUID u : new ArrayList<>(ensacados.keySet())) {
            int[] e = ensacados.get(u);
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(u);
            if (p == null || !p.isAlive() || p.getVehicle() != this) {
                ensacados.remove(u);
                continue;
            }
            e[0]++;
            if (e[0] == 3) {
                // tapa el "agáchate para bajarte" de Minecraft: aquí agacharse no sirve, hay que forcejear
                p.displayClientMessage(Component.translatable("navidad.krampus.saco.sub").withStyle(ChatFormatting.GOLD), true);
            }
            if (e[0] % 20 == 10) {
                apretando = true;
                try {
                    Combate.danar(p, Combate.fuente(sl, this), dano(APRETON_DMG));
                } finally {
                    apretando = false;
                }
            }
            if (e[0] >= SACO_TIEMPO) {
                aventar(p);
            }
        }
    }

    private boolean porLaEspalda(Vec3 desde) {
        Vec3 f = Vec3.directionFromRotation(0, yBodyRot);
        Vec3 d = new Vec3(desde.x - getX(), 0, desde.z - getZ());
        if (d.lengthSqr() < 1.0E-4) {
            return false;
        }
        return f.dot(d.normalize()) < -0.35;
    }

    private void golpeSaco(float cuanto, Entity quien) {
        sacoVida -= cuanto;
        ServerLevel sl = (ServerLevel) level();
        Vec3 atras = Vec3.directionFromRotation(0, yBodyRot).scale(-0.9);
        sl.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,
                new ItemStack(Items.BROWN_WOOL)), getX() + atras.x, getY() + 2.1, getZ() + atras.z, 10, 0.4, 0.4, 0.4, 0.08);
        sl.playSound(null, blockPosition(), ModRegistry.KRAMPUS_SACO.get(), SoundSource.HOSTILE, 1.2F, 0.9F + random.nextFloat() * 0.3F);
        if (quien instanceof Player p) {
            int pct = Math.max(0, Math.round(sacoVida / SACO_MAX * 100));
            p.displayClientMessage(Component.translatable("navidad.krampus.saco.golpe", pct).withStyle(ChatFormatting.GOLD), true);
        }
        if (sacoVida <= 0) {
            romperSaco(quien);
        }
    }

    /** ¡Le rompieron el saco! Sale el que estaba adentro, se aturde y se le caen regalos. */
    public void romperSaco(Entity quien) {
        ServerLevel sl = (ServerLevel) level();
        liberarTodos();
        sacoVida = SACO_MAX;
        sinSacoTicks = SACO_REGRESA;
        entityData.set(SIN_SACO, true);
        sacosRotos++;
        Estados.aplicar(this, Estados.ATURDIDO, 220);
        playAction("aturdido", 48);
        Efectos.lanzar(sl, Navidad.MODID + ":saco_roto", position().add(Vec3.directionFromRotation(0, yBodyRot).scale(-0.9)).add(0, 2, 0));
        List<ItemStack> caen = SacoDelKrampus.sacar(sl.getServer(), 3);
        if (caen.isEmpty()) {
            caen.add(Premios.caja(random, Premios.Rareza.RARO, "Krampus"));
        }
        caen.add(new ItemStack(Items.COAL, 2 + random.nextInt(4)));
        soltar(sl, caen);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceTo(this) < 48) {
                p.sendSystemMessage(Component.translatable("navidad.krampus.saco.roto").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
        }
        if (quien instanceof ServerPlayer sp) {
            Logros.dar(sp, "saco_roto");
        }
        Navidad.LOGGER.info("NAVIDAD_KRAMPUS_SACO roto (por {})", quien == null ? "?" : quien.getName().getString());
    }

    private void soltar(ServerLevel sl, List<ItemStack> cosas) {
        Vec3 atras = Vec3.directionFromRotation(0, yBodyRot).scale(-1.0);
        for (ItemStack st : cosas) {
            ItemEntity it = new ItemEntity(sl, getX() + atras.x, getY() + 2.2, getZ() + atras.z, st);
            it.setDeltaMovement(atras.x * 0.2 + (random.nextDouble() - 0.5) * 0.35, 0.35, atras.z * 0.2 + (random.nextDouble() - 0.5) * 0.35);
            it.setDefaultPickUpDelay();
            sl.addFreshEntity(it);
        }
    }

    /** Dónde se baja el que sale del saco (lo pone quien lo saca). */
    private Vec3 bajarEn;

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity p) {
        if (bajarEn != null) {
            return bajarEn;
        }
        Vec3 atras = Vec3.directionFromRotation(0, yBodyRot).scale(-1.6);
        return new Vec3(getX() + atras.x, getY() + 0.2, getZ() + atras.z);
    }

    @Override
    protected boolean canAddPassenger(Entity e) {
        return e instanceof Player && getPassengers().size() < SACO_GENTE;
    }

    @Override
    protected void positionRider(Entity p, Entity.MoveFunction f) {
        if (!hasPassenger(p)) {
            return;
        }
        int i = getPassengers().indexOf(p);
        Vec3 atras = Vec3.directionFromRotation(0, yBodyRot).scale(-0.85);
        Vec3 lado = Vec3.directionFromRotation(0, yBodyRot + 90F).scale(i == 0 ? 0 : 0.25);
        f.accept(p, getX() + atras.x + lado.x, getY() + 1.05, getZ() + atras.z + lado.z);
    }

    // ------------------------------------------------------------------ el apagón

    /** Entra a la tercera fase: se oscurece todo y salen los faroles (si no hay ya, como en su guarida). */
    void apagon() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        buscarFaroles();
        Efectos.lanzar(sl, Navidad.MODID + ":apagon", position().add(0, 1.5, 0));
        if (farolesCerca.size() < 3) {
            for (int i = 0; i < 4; i++) {
                double a = Math.toRadians(45 + 90 * i);
                int x = Mth.floor(getX() + Math.cos(a) * 9), z = Mth.floor(getZ() + Math.sin(a) * 9);
                int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - getY()) > 6) {
                    continue;
                }
                BlockPos p = new BlockPos(x, y, z);
                if (sl.getBlockState(p).canBeReplaced() && sl.getBlockState(p.below()).isFaceSturdy(sl, p.below(), net.minecraft.core.Direction.UP)) {
                    sl.setBlock(p, ModRegistry.FAROL_NOCHEBUENA.get().defaultBlockState().setValue(FarolNochebuena.LIT, true)
                            .setValue(FarolNochebuena.TEMPORAL, true), 3);
                    farolesPuestos.add(p);
                    Efectos.lanzar(sl, Navidad.MODID + ":farol_enciende", Vec3.atCenterOf(p));
                }
            }
        }
        oscurecer(sl);
        buscarFaroles();
        Navidad.LOGGER.info("NAVIDAD_KRAMPUS_APAGON faroles puestos={} cerca={}", farolesPuestos.size(), farolesCerca.size());
    }

    /** Un pulso de oscuridad (al apagar todo y cuando sale de la oscuridad a emboscar): no para siempre, para que se vean los faroles. */
    private void oscurecer(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (p.distanceTo(this) < 40 && !MascaraKrampus.lleva(p)) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, false, false));
            }
        }
    }

    private void buscarFaroles() {
        farolesCerca.clear();
        BlockPos c = blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-16, -5, -16), c.offset(16, 5, 16))) {
            if (level().getBlockState(q).getBlock() instanceof FarolNochebuena) {
                farolesCerca.add(q.immutable());
            }
        }
    }

    private boolean encendido(BlockPos p) {
        BlockState s = level().getBlockState(p);
        return s.getBlock() instanceof FarolNochebuena && s.getValue(FarolNochebuena.LIT);
    }

    private BlockPos farolEncendidoMasCerca(double max) {
        BlockPos mejor = null;
        double d = max * max;
        for (BlockPos p : farolesCerca) {
            double q = distanceToSqr(Vec3.atCenterOf(p));
            if (q < d && encendido(p)) {
                d = q;
                mejor = p;
            }
        }
        return mejor;
    }

    /** ¿Lo alumbra algún farol? */
    public boolean enLaLuz() {
        return farolEncendidoMasCerca(LUZ) != null;
    }

    private void tickApagon(ServerLevel sl) {
        if (tickCount % 20 == 0) {
            buscarFaroles();
        }
        enLaLuz = enLaLuz();
        if (revelado > 0) {
            revelado--;
        }
        boolean oculto = !enLaLuz && revelado <= 0 && deathTime <= 0 && (isNoAi() || !entrando());
        if (oculto != oculto()) {
            entityData.set(OCULTO, oculto);
            if (!oculto && enLaLuz && quemadoAviso <= 0) {
                quemadoAviso = 100;
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getX(), getY() + 2, getZ(), 20, 0.5, 0.8, 0.5, 0.02);
                for (ServerPlayer p : sl.players()) {
                    if (p.distanceTo(this) < 32) {
                        p.displayClientMessage(Component.translatable("navidad.krampus.luz").withStyle(ChatFormatting.YELLOW), true);
                    }
                }
            }
        }
        if (quemadoAviso > 0) {
            quemadoAviso--;
        }
        setGlowingTag(revelado > 0);
        if (oculto && tickCount % 55 == 0) {
            sl.playSound(null, blockPosition(), ModRegistry.KRAMPUS_CASCABELES.get(), SoundSource.HOSTILE, 1.3F, 0.8F + random.nextFloat() * 0.2F);
        }
        if (enLaLuz && tickCount % 4 == 0) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, getX(), getY() + 1.5 + random.nextDouble(), getZ(), 2, 0.5, 0.6, 0.5, 0.01);
        }
    }

    void irASoplar(LivingEntity t, int tick) {
        if (tick == 1) {
            farolObjetivo = farolEncendidoMasCerca(24);
            soploEn = -1;
        }
        if (farolObjetivo == null || !encendido(farolObjetivo) && soploEn < 0) {
            if (tick > 1) {
                empezar(NADA, t);
            }
            return;
        }
        Vec3 f = Vec3.atBottomCenterOf(farolObjetivo);
        if (soploEn < 0) {
            if (distanceToSqr(f) < 2.8 * 2.8) {
                soploEn = tick;
                getNavigation().stop();
                setYRot(Combate.giro(position(), f));
                yBodyRot = getYRot();
                playAction("soplido", 0);
            } else if (tick % 10 == 1 || getNavigation().isDone()) {
                getNavigation().moveTo(f.x, f.y, f.z, 1.3);
            }
            return;
        }
        getNavigation().stop();
        if (tick == soploEn + 18) {
            apagarFarol(farolObjetivo);
        }
        if (tick >= soploEn + 32) {
            empezar(NADA, t);
        }
    }

    private void apagarFarol(BlockPos p) {
        if (!(level() instanceof ServerLevel sl) || !encendido(p)) {
            return;
        }
        sl.setBlock(p, sl.getBlockState(p).setValue(FarolNochebuena.LIT, false), 3);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 0.7, p.getZ() + 0.5, 16, 0.2, 0.3, 0.2, 0.03);
        sl.playSound(null, p, ModRegistry.KRAMPUS_SOPLIDO.get(), SoundSource.HOSTILE, 1.6F, 0.9F);
        farolesApagados++;
        for (ServerPlayer o : sl.players()) {
            if (o.distanceTo(this) < 40) {
                o.displayClientMessage(Component.translatable("navidad.krampus.apago_farol").withStyle(ChatFormatting.DARK_PURPLE), true);
            }
        }
        Navidad.LOGGER.info("NAVIDAD_KRAMPUS_SOPLIDO apagó el farol de {}", p.toShortString());
    }

    void emboscar(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        List<ServerPlayer> opciones = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.distanceTo(this) < 30 && Combate.blanco(this, p) && !p.isPassenger()) {
                opciones.add(p);
            }
        }
        LivingEntity v = opciones.isEmpty() ? t : opciones.get(random.nextInt(opciones.size()));
        if (v == null) {
            return;
        }
        Vec3 donde = null;
        for (int i = 0; i < 6 && donde == null; i++) {
            float giro = v.getYRot() + 180F + (i == 0 ? 0 : (i % 2 == 0 ? 1 : -1) * 35F * ((i + 1) / 2));
            Vec3 d = Vec3.directionFromRotation(0, giro).scale(4.5);
            Vec3 q = Combate.alSuelo(sl, v.position().add(d.x, 1.5, d.z));
            BlockPos b = BlockPos.containing(q);
            if (sl.getBlockState(b).isAir() && sl.getBlockState(b.above()).isAir() && sl.getBlockState(b.above(2)).isAir()) {
                donde = q;
            }
        }
        if (donde == null) {
            return;
        }
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 20, 0.5, 1, 0.5, 0.02);
        teleportTo(donde.x, donde.y, donde.z);
        setYRot(Combate.giro(position(), v.position()));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        sl.playSound(null, BlockPos.containing(donde), ModRegistry.KRAMPUS_CASCABELES.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
        Aviso.circulo(sl, v.position(), 2.4F, 12).color(Aviso.ROJO).dano(this, dano(EMBOSCADA_DMG)).empuje(1.2, 0.45).lanzar();
        oscurecer(sl);
        emboscadas++;
        Navidad.LOGGER.info("NAVIDAD_KRAMPUS_EMBOSCADA detrás de {}", v.getName().getString());
    }

    /** (Lo llama el farol) Alguien encendió un farol: si está junto a él, lo deslumbra. */
    public void alEncenderFarol(BlockPos p, Player quien) {
        if (fase() < 2 || distanceToSqr(Vec3.atCenterOf(p)) > (LUZ + 1.5) * (LUZ + 1.5)) {
            return;
        }
        deslumbrar(60);
        if (quien != null) {
            quien.displayClientMessage(Component.translatable("navidad.krampus.deslumbrado").withStyle(ChatFormatting.YELLOW), true);
        }
    }

    /** (La campanita de Santa) Lo deja a la vista un rato y lo aturde. */
    public void revelar(int ticks) {
        revelado = Math.max(revelado, ticks);
        if (oculto()) {
            entityData.set(OCULTO, false);
        }
        deslumbrar(40);
    }

    private void deslumbrar(int ticks) {
        Estados.aplicar(this, Estados.ATURDIDO, ticks * 4);
        playAction("aturdido", ticks);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, getX(), getY() + 2.2, getZ(), 1, 0, 0, 0, 0);
        }
    }

    /** (Pruebas) Hace ya un ataque por su nombre (varazos, cadena, agarre, giro, soplido, emboscada...). */
    public void probar(String ataque, LivingEntity t) {
        Ataque a = buscarAtaque(ataque);
        if (a != null) {
            empezar(a, t);
        }
    }

    // ------------------------------------------------------------------ cada tick, daño, morir, irse

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        tickSaco(sl);
        if (fase() >= 2 && deathTime <= 0) {
            tickApagon(sl);
        } else {
            if (oculto()) {
                entityData.set(OCULTO, false);
            }
            if (revelado > 0) {
                revelado--;
            }
            setGlowingTag(revelado > 0);
        }
        if (ganchoFuera() && tickCount % 20 == 0 && sl.getEntitiesOfClass(GanchoCadena.class, getBoundingBox().inflate(32),
                g -> g.getOwner() == this).isEmpty()) {
            entityData.set(GANCHO_FUERA, false);
        }
        if (tickCount % 20 == 0 && deathTime <= 0) {
            revisarSiSeVa(sl);
        }
    }

    private void revisarSiSeVa(ServerLevel sl) {
        boolean alguien = false;
        for (ServerPlayer p : sl.players()) {
            if (p.distanceTo(this) < 64 && !p.isSpectator()) {
                alguien = true;
                break;
            }
        }
        sinNadie = alguien ? 0 : sinNadie + 20;
        long hora = sl.getDayTime() % 24000L;
        boolean dia = hora > 300 && hora < 12500;
        if (sinNadie >= 600 || seVaDeDia && dia && ensacados.isEmpty() && sl.canSeeSky(blockPosition().above(3))) {
            irse(sl);
        }
    }

    /** Se va riéndose en una nube de humo (con los regalos que se robaron). */
    public void irse(ServerLevel sl) {
        liberarTodos();
        quitarFaroles(sl);
        Efectos.lanzar(sl, Navidad.MODID + ":humo_krampus", position().add(0, 1, 0));
        sl.playSound(null, blockPosition(), ModRegistry.KRAMPUS_RISA.get(), SoundSource.HOSTILE, 3F, 0.9F);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceTo(this) < 64) {
                p.sendSystemMessage(Component.translatable("navidad.krampus.se_fue").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            }
        }
        Navidad.LOGGER.info("NAVIDAD_KRAMPUS se fue (sin nadie={}, de día={})", sinNadie >= 600, seVaDeDia);
        discard();
    }

    private void quitarFaroles(ServerLevel sl) {
        for (BlockPos p : farolesPuestos) {
            BlockState s = sl.getBlockState(p);
            if (s.getBlock() instanceof FarolNochebuena && s.getValue(FarolNochebuena.TEMPORAL)) {
                sl.removeBlock(p, false);
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.01);
            }
        }
        farolesPuestos.clear();
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (!level().isClientSide && amount > 0 && !entrando() && !isInvulnerableTo(src)) {
            Entity quien = src.getEntity();
            if (quien instanceof ServerPlayer sp) {
                peleadores.add(sp.getUUID());
            }
            Vec3 desde = src.getSourcePosition();
            if (desde != null && quien != null && sinSaco() == 0 && porLaEspalda(desde)) {
                golpeSaco(amount, quien);
            }
            if (fase() >= 2) {
                if (oculto()) {
                    amount *= 0.4F;
                } else if (enLaLuz) {
                    amount *= 1.25F;
                }
            }
        }
        return super.hurt(src, amount);
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        liberarTodos();
        quitarFaroles(sl);
        entityData.set(OCULTO, false);
        Premios.confeti(sl, getX(), getY() + 2, getZ(), true);
        // revienta el saco: todos los regalos robados
        List<ItemStack> robados = SacoDelKrampus.vaciar(sl.getServer());
        if (!robados.isEmpty()) {
            soltar(sl, robados);
            Efectos.lanzar(sl, Navidad.MODID + ":saco_roto", position().add(0, 2, 0));
        }
        for (ServerPlayer p : sl.players()) {
            if (p.distanceTo(this) < 48 || peleadores.contains(p.getUUID())) {
                p.sendSystemMessage(Component.translatable("navidad.krampus.vencido").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                if (!robados.isEmpty()) {
                    p.sendSystemMessage(Component.translatable("navidad.krampus.robados", robados.size()).withStyle(ChatFormatting.YELLOW));
                }
                for (int i = 0; i < 2; i++) {
                    Premios.dar(p, Premios.sacar(Premios.Rareza.EPICO, p.getRandom()), Premios.Rareza.EPICO, "navidad.regalo.te_toco");
                }
                Premios.dar(p, botin(p), Premios.Rareza.EPICO, "navidad.krampus.botin");
                com.axeliscore.npc.Misiones.evento(p, Navidad.MODID + ":krampus", 1);
                Logros.dar(p, "krampus");
            }
        }
        Navidad.LOGGER.info("NAVIDAD_KRAMPUS vencido (robados={}, peleadores={})", robados.size(), peleadores.size());
    }

    /** Una pieza de su botín: primero las que no te han tocado. */
    private static ItemStack botin(ServerPlayer p) {
        CompoundTag yo = com.axeliscore.data.WorldStore.get(p.server, Navidad.MODID).player(p.getUUID());
        int ya = yo.getInt("krampusBotin");
        List<Integer> faltan = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if ((ya & (1 << i)) == 0) {
                faltan.add(i);
            }
        }
        int k = faltan.isEmpty() ? p.getRandom().nextInt(4) : faltan.get(p.getRandom().nextInt(faltan.size()));
        yo.putInt("krampusBotin", ya | (1 << k));
        com.axeliscore.data.WorldStore.get(p.server, Navidad.MODID).dirty();
        return new ItemStack(switch (k) {
            case 0 -> ModRegistry.CADENA_KRAMPUS.get();
            case 1 -> ModRegistry.VARA_ABEDUL.get();
            case 2 -> ModRegistry.MASCARA_KRAMPUS.get();
            default -> ModRegistry.SACO_KRAMPUS.get();
        });
    }

    @Override
    public void remove(RemovalReason r) {
        if (!level().isClientSide && r != RemovalReason.UNLOADED_TO_CHUNK && r != RemovalReason.UNLOADED_WITH_PLAYER) {
            liberarTodos();
            if (level() instanceof ServerLevel sl && r != RemovalReason.CHANGED_DIMENSION) {
                quitarFaroles(sl);
            }
        }
        super.remove(r);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("SeVaDeDia", seVaDeDia);
        tag.putFloat("SacoVida", sacoVida);
        ListTag l = new ListTag();
        for (BlockPos p : farolesPuestos) {
            l.add(LongTag.valueOf(p.asLong()));
        }
        tag.put("Faroles", l);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        seVaDeDia = tag.getBoolean("SeVaDeDia");
        if (tag.contains("SacoVida")) {
            sacoVida = tag.getFloat("SacoVida");
        }
        farolesPuestos.clear();
        for (Tag t : tag.getList("Faroles", Tag.TAG_LONG)) {
            farolesPuestos.add(BlockPos.of(((LongTag) t).getAsLong()));
        }
    }

    // ------------------------------------------------------------------ cómo se ve (cliente)

    @Override
    public void axAnimate(AnimHost h, float pt) {
        if (axDeath(h, "action")) {
            return;
        }
        axActions(h, n -> "action");
        axHurt(h, "action");
        axMove(h, 0.2F, 3.6F, 1.25F, 4.2F);
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        var act = h.animator().get("action");
        if (deathTime <= 0 && (act == null || !act.playing())) {
            axLook(h, pose, pt, "head", "neck", 0.7F);
        }
        Geo g = h.geo();
        if (oculto()) {
            // escondido en lo oscuro: una sombra negra; solo brillan los ojos
            for (int b = 0; b < g.bones.length; b++) {
                if (!"ojos".equals(g.bones[b].name)) {
                    pose.tinte(b, 0xFF000000);
                }
            }
            int ojos = g.bone("ojos");
            if (ojos >= 0) {
                pose.mulScale(ojos, 1.35F, 1.6F, 1F);
            }
        }
        int saco = g.bone("saco");
        if (saco >= 0) {
            if (sinSaco() > 0) {
                pose.hide(saco, true);
            } else if (isVehicle()) {
                // alguien adentro: el saco se infla y se mueve
                float t = tickCount + pt;
                float k = 1.12F + 0.05F * Mth.sin(t * 0.9F);
                pose.mulScale(saco, k, 1.08F, k + 0.06F);
                pose.addRot(saco, 0.08F * Mth.sin(t * 1.3F), 0, 0.12F * Mth.sin(t * 1.7F));
            }
        }
        if (ganchoFuera()) {
            int b = g.bone("gancho");
            if (b >= 0) {
                pose.hide(b, true);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(4) == 0 ? ModRegistry.KRAMPUS_RISA.get() : ModRegistry.KRAMPUS_CADENAS.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return net.minecraft.sounds.SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModRegistry.KRAMPUS_RUGE.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    // ------------------------------------------------------------------ llamarlo

    /** (Servidor) Llega el Krampus: humo, cadenas y aviso para los que estén cerca. */
    public static Krampus llamar(ServerLevel l, Vec3 donde, LivingEntity quien) {
        return llamar(l, donde, quien, false);
    }

    /** {@code seVaDeDia}: si vino de visita o con la campana (al amanecer al aire libre se va). */
    public static Krampus llamar(ServerLevel l, Vec3 donde, LivingEntity quien, boolean seVaDeDia) {
        Krampus k = ModRegistry.KRAMPUS.get().create(l);
        if (k == null) {
            return null;
        }
        k.moveTo(donde.x, donde.y, donde.z, quien == null ? 0 : quien.getYRot() + 180, 0);
        k.finalizeSpawn(l, l.getCurrentDifficultyAt(k.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        k.seVaDeDia = seVaDeDia;
        if (quien != null && quien != k) {
            k.setTarget(quien);
        }
        l.addFreshEntity(k);
        k.playAction("rugido", 0);
        Efectos.lanzar(l, Navidad.MODID + ":humo_krampus", donde.add(0, 1, 0));
        l.playSound(null, k.blockPosition(), ModRegistry.KRAMPUS_RUGE.get(), SoundSource.HOSTILE, 3F, 0.8F);
        l.playSound(null, k.blockPosition(), ModRegistry.KRAMPUS_CADENAS.get(), SoundSource.HOSTILE, 3F, 0.8F);
        for (ServerPlayer p : l.players()) {
            if (p.distanceToSqr(donde) < 80 * 80) {
                p.sendSystemMessage(Component.translatable("navidad.krampus.llega").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }
        }
        return k;
    }

    /** Para cosas que necesitan a "Plat" sin importarlo en todos lados. */
    private static final class Plat {
        static void push(Entity e, double x, double y, double z) {
            com.axeliscore.plat.Plat.push(e, x, y, z);
        }
    }
}
