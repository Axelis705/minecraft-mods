package com.navidad.entidad;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Pose;
import com.axeliscore.combat.Ataque;
import com.axeliscore.combat.Aviso;
import com.axeliscore.combat.Combate;
import com.axeliscore.combat.Estados;
import com.axeliscore.combat.Fase;
import com.axeliscore.combat.Jefe;
import com.axeliscore.combat.Proyectil;
import com.axeliscore.fx.Fx;
import com.navidad.Navidad;
import com.navidad.objeto.Premios;
import com.navidad.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * El Krampus: el jefe de Navidad. Peludo, con cuernos de cabra, cadenas y un saco para los que se portaron mal. Dos fases:
 * <ol>
 *     <li>Cadenazo (cono al frente), pisotón (anillo: saltar lo esquiva), carbones encendidos en abanico y un rugido que empuja y llama a
 *     dos muñecos de nieve malvados.</li>
 *     <li>A la mitad de la vida se enfurece (ojos rojos, más rápido): agrega el saco (un círculo persigue a cada jugador y lo aturde) y
 *     cascabeles malditos que persiguen.</li>
 * </ol>
 * Al vencerlo: "¡Salvaste la Navidad!", regalos misteriosos para todos los que pelearon y su cuerno de trofeo.
 */
public class Krampus extends Jefe {
    static final float CADENA_DMG = 9, PISOTON_DMG = 6, SACO_DMG = 8;
    static final int SLAM_HIT = 23, SWEEP_HIT = 15, STOMP_HIT = 12, ROAR_HIT = 8;

    private static final Ataque CADENAZO = new Ataque("cadenazo").clip("sweep").dura(28).golpe(SWEEP_HIT).distancia(0, 5F).peso(3).espera(40)
            .despues(40).alEmpezar((j, t) -> Aviso.cono(j.level(), j.position(), j.getYRot(), 6F, 140, SWEEP_HIT).color(Aviso.ROJO)
                    .dano(j, j.dano(CADENA_DMG)).empuje(1.0, 0.3).estado(Estados.MARCADO, 80, 1)
                    .sonido("minecraft:block.chain.break", 1.5F, 0.6F).particulas("crit").lanzar());
    private static final Ataque PISOTON = new Ataque("pisoton").clip("stomp").dura(22).golpe(STOMP_HIT).distancia(2, 9).peso(2).espera(60)
            .despues(40).alEmpezar((j, t) -> Aviso.anillo(j.level(), j.position(), 7F, 1.8F, STOMP_HIT).alto(0.9).color(Aviso.AMARILLO)
                    .dano(j, j.dano(PISOTON_DMG)).empuje(1.0, 0.45).sonido("", 0, 0).particulas("snow").temblor(0.4F).lanzar());
    private static final Ataque CARBONES = new Ataque("carbones").clip("sweep").dura(28).golpe(SWEEP_HIT).distancia(5, 22).peso(2).espera(70)
            .despues(35).alGolpe((j, t) -> {
                if (t == null) {
                    return;
                }
                Vec3 desde = j.position().add(0, 2.4, 0);
                for (int i = -1; i <= 1; i++) {
                    Proyectil p = Proyectil.hacia(j.level(), Proyectiles.CARBON, j, desde, t, 0.85F);
                    if (i != 0) {
                        Vec3 v = p.getDeltaMovement();
                        double a = Math.toRadians(14 * i), c = Math.cos(a), s = Math.sin(a);
                        p.setDeltaMovement(new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c));
                    }
                }
            });
    private static final Ataque RUGIDO = new Ataque("rugido").clip("roar").dura(44).golpe(ROAR_HIT).distancia(0, 16).peso(1).espera(300)
            .despues(60).alGolpe((j, t) -> {
                for (Player p : j.level().players()) {
                    if (p.distanceTo(j) < 9 && Combate.blanco(j, p)) {
                        Combate.empujarDesde(p, j.position(), 1.3, 0.35);
                    }
                }
                Fx.shakeNear(j, 18, 0.5F, 20);
                if (j.level() instanceof ServerLevel sl) {
                    for (int i = 0; i < 2; i++) {
                        MunecoMalvado m = ModRegistry.MUNECO_MALVADO.get().create(sl);
                        if (m != null) {
                            double a = Math.toRadians(j.getYRot() + (i == 0 ? 70 : -70));
                            Vec3 p = j.position().add(-Math.sin(a) * 4, 0.2, Math.cos(a) * 4);
                            m.moveTo(p.x, p.y, p.z, j.getYRot(), 0);
                            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                            sl.addFreshEntity(m);
                            com.axeliscore.fx.Efectos.lanzar(sl, Navidad.MODID + ":nieve", p.add(0, 1, 0));
                        }
                    }
                }
            });
    /** Fase 2: el saco. Un círculo persigue a cada jugador cercano y cae: pega y aturde. */
    private static final Ataque SACO = new Ataque("saco").clip("slam").dura(40).golpe(SLAM_HIT).distancia(0, 20).peso(2).espera(140)
            .despues(45).alEmpezar((j, t) -> {
                int n = 0;
                for (Player p : j.level().players()) {
                    if (n < 4 && p.distanceTo(j) < 22 && Combate.blanco(j, p) || p == t && n < 4) {
                        Aviso.circulo(j.level(), p.position(), 2.2F, 34).seguir(p, 14).color(Aviso.MORADO).dano(j, j.dano(SACO_DMG))
                                .estado(Estados.ATURDIDO, 40, 1).particulas("smoke").sonido("minecraft:block.wool.break", 1.5F, 0.5F).lanzar();
                        n++;
                    }
                }
            });
    /** Fase 2: cascabeles malditos que persiguen a cada jugador. */
    private static final Ataque CASCABELES = new Ataque("cascabeles").clip("roar").dura(44).golpe(10).distancia(4, 24).peso(2).espera(130)
            .despues(40).alGolpe((j, t) -> {
                int n = 0;
                for (Player p : j.level().players()) {
                    if (n < 3 && p.distanceTo(j) < 26 && Combate.blanco(j, p) || p == t && n < 3) {
                        Proyectil.hacia(j.level(), Proyectiles.CASCABEL, j, j.position().add(0, 3.0, 0), p, 0.45F).persiguiendo(p);
                        n++;
                    }
                }
            });

    private static final List<Fase> FASES = List.of(
            new Fase(1F).color(BossEvent.BossBarColor.PURPLE).musica(Navidad.MODID + ":krampus_musica")
                    .ataque(CADENAZO).ataque(PISOTON).ataque(CARBONES).ataque(RUGIDO),
            new Fase(0.5F).color(BossEvent.BossBarColor.RED).titulo(Component.translatable("navidad.krampus.fase2"), 0xFF3A2A)
                    .subtitulo(Component.translatable("navidad.krampus.fase2_sub")).entrada("phase", 52).dano(1.25F).rapidez(1.3F)
                    .caminar(1.15F).aspecto(1).musica(Navidad.MODID + ":krampus_musica")
                    .ataque(CADENAZO).ataque(PISOTON).ataque(CARBONES).ataque(SACO).ataque(CASCABELES).ataque(RUGIDO));

    public Krampus(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 150;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 360.0).add(Attributes.ARMOR, 8.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected List<Fase> fases() {
        return FASES;
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (level() instanceof ServerLevel sl) {
            Premios.confeti(sl, getX(), getY() + 2, getZ(), true);
            for (ServerPlayer p : sl.players()) {
                if (p.distanceTo(this) < 48) {
                    p.sendSystemMessage(Component.translatable("navidad.krampus.vencido").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    for (int k = 0; k < 3; k++) {
                        Premios.dar(p, Premios.sacar(Premios.Rareza.EPICO, p.getRandom()), Premios.Rareza.EPICO, "navidad.regalo.te_toco");
                    }
                    com.axeliscore.npc.Misiones.evento(p, Navidad.MODID + ":krampus", 1);
                    com.navidad.Logros.dar(p, "krampus");
                }
            }
        }
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
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return net.minecraft.sounds.SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) {
        return net.minecraft.sounds.SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return ModRegistry.KRAMPUS_RUGE.get();
    }

    /** (Servidor) Llega el Krampus: humo, cadenas y aviso para los que estén cerca. */
    public static Krampus llamar(ServerLevel l, Vec3 donde, LivingEntity quien) {
        Krampus k = ModRegistry.KRAMPUS.get().create(l);
        if (k == null) {
            return null;
        }
        k.moveTo(donde.x, donde.y, donde.z, quien == null ? 0 : quien.getYRot() + 180, 0);
        k.finalizeSpawn(l, l.getCurrentDifficultyAt(k.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        l.addFreshEntity(k);
        com.axeliscore.fx.Efectos.lanzar(l, Navidad.MODID + ":humo_krampus", donde.add(0, 1, 0));
        l.playSound(null, k.blockPosition(), ModRegistry.KRAMPUS_RUGE.get(), net.minecraft.sounds.SoundSource.HOSTILE, 3F, 0.8F);
        for (ServerPlayer p : l.players()) {
            if (p.distanceToSqr(donde) < 80 * 80) {
                p.sendSystemMessage(Component.translatable("navidad.krampus.llega").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }
        }
        return k;
    }
}
