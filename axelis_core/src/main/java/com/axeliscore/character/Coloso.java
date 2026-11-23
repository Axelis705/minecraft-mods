package com.axeliscore.character;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Pose;
import com.axeliscore.combat.Ataque;
import com.axeliscore.combat.Aviso;
import com.axeliscore.combat.Combate;
import com.axeliscore.combat.CoreProyectiles;
import com.axeliscore.combat.Estados;
import com.axeliscore.combat.Fase;
import com.axeliscore.combat.Jefe;
import com.axeliscore.combat.Proyectil;
import com.axeliscore.fx.Fx;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Coloso: jefe de muestra del Core (golem de piedra con cristales que brillan), hecho con {@link Jefe}. Tres fases:
 * <ol>
 *     <li><b>Piedra</b> (toda la vida): martillazo (círculo a su alrededor), barrido (cono al frente), pisotón (anillo: saltar lo
 *     esquiva) y rugido (empuja).</li>
 *     <li><b>Cristales despiertos</b> (60 %): barra roja, cristales rojos, pega 20 % más; agrega lluvia de cristales (círculos que
 *     persiguen a cada jugador y hacen sangrar) y fragmentos de amatista que marcan.</li>
 *     <li><b>Furia de cristal</b> (30 %): barra azul, cristales de hielo, más rápido; el martillazo aturde, ondas de hielo en anillos
 *     que se abren uno tras otro y orbes de escarcha que persiguen y congelan.</li>
 * </ol>
 * Cada fase tiene su música. Todos los números están aquí para ajustarlos fácil.
 */
public class Coloso extends Jefe {
    static final float SLAM_DMG = 14, SWEEP_DMG = 10, STOMP_DMG = 7, LLUVIA_DMG = 8, ONDA_DMG = 6;
    /** Tiempos de impacto de cada animación (ticks), los mismos que el evento de la animación. */
    static final int SLAM_HIT = 23, SWEEP_HIT = 15, STOMP_HIT = 12, ROAR_HIT = 8, PHASE_LEN = 52;

    private static final Ataque MARTILLAZO = new Ataque("martillazo").clip("slam").dura(40).golpe(SLAM_HIT).distancia(0, 4.5F).peso(3).espera(50)
            .despues(50).alEmpezar((j, t) -> {
                Aviso a = Aviso.circulo(j.level(), j.position(), 4.6F, SLAM_HIT).color(Aviso.NARANJA).dano(j, j.dano(SLAM_DMG)).empuje(0.6, 0.75)
                        .sonido("", 0, 0).particulas("explosion").temblor(0.7F);
                if (j.fase() >= 2) {
                    a.estado(Estados.ATURDIDO, 30, 1);
                }
                a.lanzar();
            });
    private static final Ataque BARRIDO = new Ataque("barrido").clip("sweep").dura(28).golpe(SWEEP_HIT).distancia(0, 4.8F).peso(2).espera(40)
            .despues(45).alEmpezar((j, t) -> Aviso.cono(j.level(), j.position(), j.getYRot(), 5.6F, 150, SWEEP_HIT).dano(j, j.dano(SWEEP_DMG))
                    .empuje(1.0, 0.3).sonido("minecraft:entity.player.attack.sweep", 1.3F, 0.6F).particulas("crit").lanzar());
    private static final Ataque PISOTON = new Ataque("pisoton").clip("stomp").dura(22).golpe(STOMP_HIT).distancia(3, 9).peso(2).espera(60)
            .despues(45).alEmpezar((j, t) -> Aviso.anillo(j.level(), j.position(), 7F, 1.8F, STOMP_HIT).alto(0.9).color(Aviso.AMARILLO)
                    .dano(j, j.dano(STOMP_DMG)).empuje(1.0, 0.4).sonido("", 0, 0).particulas("cloud").temblor(0.4F).lanzar());
    private static final Ataque RUGIDO = new Ataque("rugido").clip("roar").dura(44).golpe(ROAR_HIT).distancia(0, 14).peso(1).espera(240)
            .despues(60).alGolpe((j, t) -> {
                for (Player p : j.level().players()) {
                    if (p.distanceTo(j) < 8 && Combate.blanco(j, p)) {
                        Combate.empujarDesde(p, j.position(), 1.4, 0.35);
                    }
                }
                Fx.shakeNear(j, 16, 0.5F, 20);
            });
    /** Fase 2: un círculo morado persigue a cada jugador cercano y se fija antes de caer. */
    private static final Ataque LLUVIA = new Ataque("lluvia").clip("roar").dura(44).golpe(ROAR_HIT).distancia(2, 22).peso(2).espera(160)
            .despues(50).alEmpezar((j, t) -> {
                int n = 0;
                for (Player p : j.level().players()) {
                    if (n < 4 && p.distanceTo(j) < 24 && Combate.blanco(j, p) || p == t && n < 4) {
                        Aviso.circulo(j.level(), p.position(), 2.4F, 36).seguir(p, 12).color(Aviso.MORADO).dano(j, j.dano(LLUVIA_DMG))
                                .estado(Estados.SANGRADO, 80, 1).particulas("magic").sonido("minecraft:block.amethyst_cluster.break", 1.4F, 0.7F)
                                .lanzar();
                        n++;
                    }
                }
            });
    /** Fase 2: tres fragmentos de amatista en abanico. */
    private static final Ataque FRAGMENTOS = new Ataque("fragmentos").clip("sweep").dura(28).golpe(SWEEP_HIT).distancia(5, 20).peso(2)
            .espera(80).despues(40).alGolpe((j, t) -> {
                if (t == null) {
                    return;
                }
                Vec3 desde = j.position().add(0, 2.2, 0);
                for (int i = -1; i <= 1; i++) {
                    Proyectil p = Proyectil.hacia(j.level(), CoreProyectiles.FRAGMENTO, j, desde, t, 0.9F);
                    if (i != 0) {
                        Vec3 v = p.getDeltaMovement();
                        double a = Math.toRadians(12 * i), c = Math.cos(a), s = Math.sin(a);
                        p.setDeltaMovement(new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c));
                    }
                }
            });
    /** Fase 2 y 3: cristal grande que cae en arco sobre el objetivo. */
    private static final Ataque CRISTAL = new Ataque("cristal").clip("slam").dura(40).golpe(SLAM_HIT).distancia(6, 22).peso(1).espera(120)
            .despues(40).alGolpe((j, t) -> {
                if (t != null) {
                    Proyectil.hacia(j.level(), CoreProyectiles.CRISTAL, j, j.position().add(0, 3.2, 0), t, 0.75F);
                }
            });
    /** Fase 3: tres anillos de hielo que se abren uno tras otro (saltar los esquiva). */
    private static final Ataque ONDA = new Ataque("onda").clip("stomp").dura(46).golpe(STOMP_HIT).distancia(0, 12).peso(2).espera(120)
            .despues(40).alEmpezar((j, t) -> onda(j, j.position(), 0));
    /** Fase 3: orbes de escarcha que persiguen a cada jugador cercano. */
    private static final Ataque ESCARCHA = new Ataque("escarcha").clip("roar").dura(44).golpe(10).distancia(4, 24).peso(2).espera(140)
            .despues(40).alGolpe((j, t) -> {
                int n = 0;
                for (Player p : j.level().players()) {
                    if (n < 3 && p.distanceTo(j) < 26 && Combate.blanco(j, p) || p == t && n < 3) {
                        Proyectil.hacia(j.level(), CoreProyectiles.ESCARCHA, j, j.position().add(0, 3.4, 0), p, 0.45F).persiguiendo(p);
                        n++;
                    }
                }
            });

    private static final List<Fase> FASES = List.of(
            new Fase(1F).color(BossEvent.BossBarColor.PURPLE).musica("axelis_core:musica.coloso_1")
                    .ataque(MARTILLAZO).ataque(BARRIDO).ataque(PISOTON).ataque(RUGIDO),
            new Fase(0.6F).color(BossEvent.BossBarColor.RED).titulo(Component.translatable("axelis_core.coloso.fase2"), 0xFF5A4A)
                    .subtitulo(Component.translatable("axelis_core.coloso.fase2_sub")).entrada("phase", PHASE_LEN).dano(1.2F).rapidez(1.2F)
                    .aspecto(1).musica("axelis_core:musica.coloso_2")
                    .ataque(MARTILLAZO).ataque(BARRIDO).ataque(PISOTON).ataque(RUGIDO).ataque(LLUVIA).ataque(FRAGMENTOS).ataque(CRISTAL),
            new Fase(0.3F).color(BossEvent.BossBarColor.BLUE).titulo(Component.translatable("axelis_core.coloso.fase3"), 0x8FE3FF)
                    .subtitulo(Component.translatable("axelis_core.coloso.fase3_sub")).entrada("phase", PHASE_LEN).dano(1.35F).rapidez(1.45F)
                    .caminar(1.15F).aspecto(2).musica("axelis_core:musica.coloso_3")
                    .ataque(MARTILLAZO).ataque(BARRIDO).ataque(ONDA).ataque(LLUVIA).ataque(ESCARCHA).ataque(CRISTAL));

    public Coloso(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 300.0).add(Attributes.ARMOR, 10.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.ATTACK_DAMAGE, 12.0).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected List<Fase> fases() {
        return FASES;
    }

    /** Todos los ataques (para el comando de prueba). */
    public static List<Ataque> ataques() {
        List<Ataque> l = new ArrayList<>();
        for (Fase f : FASES) {
            for (Ataque a : f.ataques()) {
                if (!l.contains(a)) {
                    l.add(a);
                }
            }
        }
        return l;
    }

    /** Un anillo de hielo y, al terminar, el siguiente más grande (hasta tres). */
    private static void onda(Jefe j, Vec3 c, int n) {
        float in = 1.5F + n * 3F, out = in + 3F;
        Aviso a = Aviso.anillo(j.level(), c, out, in, n == 0 ? STOMP_HIT : 10).alto(0.9).color(Aviso.AZUL).dano(j, j.dano(ONDA_DMG))
                .empuje(0.6, 0.35).estado(Estados.CONGELADO, 70, 1).particulas("snow").sonido("minecraft:block.glass.break", 1F, 0.6F + n * 0.15F)
                .temblor(0.25F);
        if (n < 2) {
            a.alTerminar(x -> {
                if (j.isAlive()) {
                    onda(j, c, n + 1);
                }
            });
        }
        a.lanzar();
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        if (axDeath(h, "action")) {
            return;
        }
        axActions(h, n -> "action");
        axHurt(h, "action");
        axMove(h, 0.2F, 99F, 0.85F, 1F);
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        var act = h.animator().get("action");
        if (deathTime <= 0 && (act == null || !act.playing())) {
            axLook(h, pose, pt, "head", "torso", 0.6F);
        }
    }

    @Override
    public void axInfo(com.axeliscore.ui.InfoCard card, boolean detail) {
        super.axInfo(card, detail);
        if (detail) {
            card.text(Component.translatable("axelis_core.info.coloso"));
        } else {
            card.hint(Component.translatable("axelis_core.info.mas"));
        }
    }

    /** El objetivo actual o el jugador más cercano (para forzar ataques desde un comando). */
    public LivingEntity objetivoOCercano() {
        LivingEntity t = getTarget();
        if (t != null) {
            return t;
        }
        return level().getNearestPlayer(this, 32);
    }
}
