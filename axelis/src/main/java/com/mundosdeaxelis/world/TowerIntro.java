package com.mundosdeaxelis.world;

import com.mundosdeaxelis.cinematic.Cinematic;
import com.mundosdeaxelis.cinematic.CinematicManager;
import com.mundosdeaxelis.entity.Archimago;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.BossTitlePacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Intro del Archimago (~12 s): vista alta de la cima de la torre entre las estrellas; los libros se levantan del suelo y giran
 * alrededor del círculo rúnico; tres cristales llegan volando, encienden columnas de luz y chocan en el centro; el Archimago baja
 * del cielo dentro de un remolino, aterriza con un destello y aparece su título. La cámara se mueve respecto al sitio del jefe
 * ({@link TowerArena#bossSpot()}); los jugadores llegan por el sur, así que la cámara empieza detrás de ellos mirando al norte.
 */
public final class TowerIntro {
    private static final AABB ARENA = new AABB(-40, 60, -40, 40, 160, 40);
    private static final int VIOLET = 0xB46EFF;
    private static final int ARCANE = 0x78F0FF;
    private static final int PALE = 0xE8DCFF;
    private static final int GOLD = 0xFFD86A;
    /** Altura desde la que baja el Archimago. */
    private static final double DROP = 9.0;

    private TowerIntro() {
    }

    private static Cinematic build() {
        return Cinematic.builder("archimago_intro")
                .bars(true)
                .duration(245)
                .fadeOut(25)
                // 1) vista alta de la cima de la torre, bajando despacio sobre el puente
                .frame(0, 0, 22, 40, 0, 1, 0)
                .frame(60, 0, 9, 27, 0, 3, 0)
                // 2) giro alrededor del círculo mientras suben los libros
                .frame(82, -13, 4, 9, 0, 3, 0)
                .frame(122, 12, 5, 7, 0, 3.5, 0)
                // 3) a ras de suelo: los cristales llegan y se encienden las columnas
                .frame(136, 0, 1.6, 13, 0, 4, 0)
                .frame(170, 0, 2.6, 10, 0, 5.5, 0)
                // 4) mirando hacia arriba mientras baja el Archimago; al aterrizar la cámara retrocede un poco
                .frame(180, 0, 1.2, 12, 0, 7.5, 0)
                .frame(212, 0, 1.6, 10.5, 0, 3.6, 0)
                .frame(245, 0, 2.4, 12, 0, 3.2, 0)
                .subtitle(18, 72, "La torre guarda silencio entre las estrellas...")
                .subtitle(84, 132, "Las páginas despiertan.")
                .subtitle(200, 240, "Archimago, señor de la Torre.")
                .cue(0, (l, o) -> {
                    screen(l, ScreenFxPacket.DARK, 0x0A0418, 40);
                    sound(l, o, SoundEvents.AMETHYST_BLOCK_RESONATE, 4.0F, 0.6F);
                    sparkles(l, o, 0);
                })
                .cue(30, (l, o) -> sound(l, o, SoundEvents.BELL_RESONATE, 3.0F, 0.7F))
                // los libros se levantan y giran
                .cue(58, (l, o) -> {
                    sound(l, o, SoundEvents.ENCHANTMENT_TABLE_USE, 5.0F, 0.6F);
                    sound(l, o, SoundEvents.BOOK_PAGE_TURN, 4.0F, 0.8F);
                    books(l, o);
                })
                .cue(90, (l, o) -> {
                    Vfx.rune(l, o.add(0, 0.12, 0), VIOLET, 7.5, 150);
                    Vfx.ring(l, o.add(0, 0.12, 0), PALE, 4.0, 60);
                    sound(l, o, ModSounds.BOSS_RUMBLE.get(), 4.0F, 1.1F);
                    shake(l, 5, 60);
                })
                .cue(105, (l, o) -> sound(l, o, SoundEvents.BOOK_PAGE_TURN, 4.0F, 1.2F))
                // tres cristales llegan volando
                .cue(118, (l, o) -> {
                    sound(l, o, ModSounds.BOSS_RISER.get(), 5.0F, 1.0F);
                    crystals(l, o);
                })
                .cue(150, (l, o) -> {
                    for (int i = 0; i < 3; i++) {
                        double a = Math.PI * 2.0 * i / 3 + Math.PI / 2.0;
                        Vfx.column(l, o.add(Math.cos(a) * 5.0, 0.1, Math.sin(a) * 5.0), i == 1 ? ARCANE : VIOLET, 0.9, 22.0, 70);
                    }
                    sound(l, o, SoundEvents.BEACON_POWER_SELECT, 4.0F, 0.7F);
                    shake(l, 10, 30);
                })
                // los cristales chocan: un remolino y el Archimago baja del cielo
                .cue(166, (l, o) -> {
                    screen(l, ScreenFxPacket.FLASH, 0xE8DCFF, 6);
                    sound(l, o, SoundEvents.AMETHYST_BLOCK_BREAK, 5.0F, 0.5F);
                    sound(l, o, SoundEvents.EVOKER_PREPARE_SUMMON, 5.0F, 0.6F);
                    Vfx.funnel(l, o.add(0, 0.1, 0), VIOLET, 3.0, 12.0, 60);
                    Vfx.flash(l, o.add(0, 3.0, 0), PALE, 6.0, 10);
                    flingBooks(l, o);
                    descend(l, o);
                })
                // aterriza
                .cue(206, (l, o) -> land(l, o))
                .cue(214, (l, o) -> {
                    title(l, "ARCHIMAGO", "SEÑOR DE LA TORRE", VIOLET);
                    sound(l, o, ModSounds.BOSS_IMPACT.get(), 4.0F, 1.1F);
                    sound(l, o, SoundEvents.ILLUSIONER_CAST_SPELL, 4.0F, 0.7F);
                })
                .cue(232, (l, o) -> sound(l, o, SoundEvents.EVOKER_CELEBRATE, 4.0F, 0.6F))
                .build();
    }

    private static final Cinematic INTRO = build();

    // ------------------------------------------------------------------ efectos

    /** Chispas de magia que suben por toda la arena mientras dura la escena. */
    private static void sparkles(ServerLevel l, Vec3 o, int t) {
        if (t > INTRO.duration()) {
            return;
        }
        Vec3 c = TowerArena.center();
        for (int i = 0; i < 4; i++) {
            double a = l.random.nextDouble() * Math.PI * 2.0;
            double d = 4.0 + l.random.nextDouble() * 15.0;
            l.sendParticles(ParticleTypes.ENCHANT, c.x + Math.cos(a) * d, c.y + 0.5 + l.random.nextDouble() * 4.0, c.z + Math.sin(a) * d, 8, 0.6, 0.6, 0.6, 0.4);
        }
        l.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 6.0, c.z, 2, 12.0, 4.0, 12.0, 0.01);
        Scheduler.runLater(3, () -> sparkles(l, o, t + 3));
    }

    /** Libros voladores (de todas las copias a la vez: cada intro toma solo los de su nivel). */
    private static final List<Props.Piece> BOOKS = new ArrayList<>();

    static void clearRuntime() {
        BOOKS.clear();
    }

    /** Diez libros (estanterías pequeñas) que salen del suelo y giran cada vez más alto alrededor del sitio del jefe. */
    private static void books(ServerLevel l, Vec3 o) {
        Props.take(BOOKS, l);
        final int n = 10;
        for (int i = 0; i < n; i++) {
            Props.Piece pc = Props.spawn(l, i % 3 == 0 ? Blocks.CHISELED_BOOKSHELF : Blocks.BOOKSHELF, o.add(0, 0.2, 0), 0.55F, 0.7F, 0.25F,
                    true, 9, new Quaternionf());
            if (pc != null) {
                BOOKS.add(pc);
            }
        }
        final List<Props.Piece> mine = new ArrayList<>();
        for (Props.Piece pc : BOOKS) {
            if (pc.entity().level() == l) {
                mine.add(pc);
            }
        }
        final int ticks = 110;
        for (int k = 1; k <= ticks; k += 2) {
            final int t = k;
            Scheduler.runLater(t, () -> {
                double f = (double) t / ticks;
                double radius = 1.0 + 3.6 * Math.min(1.0, f * 1.6);
                double spin = t * 0.07;
                for (int i = 0; i < mine.size(); i++) {
                    Props.Piece pc = mine.get(i);
                    if (pc.entity().isRemoved()) {
                        continue;
                    }
                    double a = spin + Math.PI * 2.0 * i / mine.size();
                    double y = 0.3 + 3.2 * Math.min(1.0, f * 1.3) + Math.sin(t * 0.12 + i) * 0.35;
                    Vec3 p = o.add(Math.cos(a) * radius, y, Math.sin(a) * radius);
                    pc.put(p, new Quaternionf().rotateY((float) (-a)).rotateZ(0.25F * (float) Math.sin(t * 0.1 + i)));
                    if (t % 8 == 1) {
                        l.sendParticles(ParticleTypes.ENCHANT, p.x, p.y + 0.3, p.z, 4, 0.2, 0.2, 0.2, 0.3);
                    }
                }
            });
        }
    }

    /** Al chocar los cristales los libros salen disparados hacia arriba y desaparecen en chispas. */
    private static void flingBooks(ServerLevel l, Vec3 o) {
        final List<Props.Piece> mine = Props.take(BOOKS, l);
        final int n = mine.size();
        final Vec3[] pos = new Vec3[n];
        final double[][] v = new double[n][3];
        for (int i = 0; i < n; i++) {
            pos[i] = mine.get(i).entity().position();
            Vec3 out = new Vec3(pos[i].x - o.x, 0, pos[i].z - o.z).normalize();
            v[i][0] = out.x * 0.35;
            v[i][1] = 0.5 + l.random.nextDouble() * 0.3;
            v[i][2] = out.z * 0.35;
        }
        for (int t = 1; t <= 24; t++) {
            final int tick = t;
            Scheduler.runLater(t, () -> {
                for (int i = 0; i < n; i++) {
                    Props.Piece pc = mine.get(i);
                    if (pc.entity().isRemoved()) {
                        continue;
                    }
                    if (tick == 24) {
                        l.sendParticles(ParticleTypes.ENCHANT, pos[i].x, pos[i].y, pos[i].z, 20, 0.3, 0.3, 0.3, 0.8);
                        l.sendParticles(ParticleTypes.END_ROD, pos[i].x, pos[i].y, pos[i].z, 4, 0.2, 0.2, 0.2, 0.05);
                        pc.remove();
                        continue;
                    }
                    v[i][1] -= 0.02;
                    pos[i] = pos[i].add(v[i][0], v[i][1], v[i][2]);
                    pc.put(pos[i], new Quaternionf().rotateXYZ(tick * 0.2F, tick * 0.15F * (i % 3 + 1), 0.0F));
                }
            });
        }
    }

    /** Tres cristales de amatista que llegan desde lejos, se quedan girando sobre sus columnas y al final chocan en el centro. */
    private static void crystals(ServerLevel l, Vec3 o) {
        final List<Props.Piece> cr = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Props.Piece pc = Props.spawn(l, i == 1 ? Blocks.LIGHT_BLUE_STAINED_GLASS : Blocks.AMETHYST_BLOCK, o.add(0, 6, 0), 0.9F, 0.9F, 0.9F,
                    true, 15, new Quaternionf());
            if (pc != null) {
                cr.add(pc);
            }
        }
        final int fly = 30, hold = 18, crash = 10;
        for (int t = 1; t <= fly + hold + crash; t++) {
            final int tick = t;
            Scheduler.runLater(t, () -> {
                for (int i = 0; i < cr.size(); i++) {
                    Props.Piece pc = cr.get(i);
                    if (pc.entity().isRemoved()) {
                        continue;
                    }
                    double a = Math.PI * 2.0 * i / 3 + Math.PI / 2.0;
                    Vec3 rest = o.add(Math.cos(a) * 5.0, 3.5, Math.sin(a) * 5.0);
                    Vec3 p;
                    if (tick <= fly) {
                        double f = (double) tick / fly;
                        double e = 1.0 - (1.0 - f) * (1.0 - f);
                        Vec3 from = o.add(Math.cos(a) * 18.0, 9.0, Math.sin(a) * 18.0);
                        p = from.add(rest.subtract(from).scale(e));
                    } else if (tick <= fly + hold) {
                        p = rest.add(0, Math.sin(tick * 0.3) * 0.2, 0);
                    } else {
                        double f = (double) (tick - fly - hold) / crash;
                        p = rest.add(o.add(0, 3.0, 0).subtract(rest).scale(f * f));
                    }
                    float spin = tick * 0.18F;
                    pc.put(p, new Quaternionf().rotateY(spin).rotateX(0.6F).rotateZ(0.6F));
                    if (tick % 3 == 0) {
                        l.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.01);
                    }
                    if (tick == fly + hold + crash) {
                        pc.remove();
                    }
                }
            });
        }
    }

    /** El Archimago aparece arriba del sitio del jefe y baja despacio dentro del remolino. */
    private static void descend(ServerLevel l, Vec3 o) {
        Archimago boss = spawnBoss(l);
        if (boss == null) {
            return;
        }
        final int ticks = 40;
        for (int t = 0; t <= ticks; t++) {
            final int tick = t;
            Scheduler.runLater(t, () -> {
                if (boss.isRemoved() || !boss.isNoAi()) {
                    return; // ya despertó (se saltó la intro): no se le mueve más
                }
                double f = (double) tick / ticks;
                double e = 1.0 - (1.0 - f) * (1.0 - f) * (1.0 - f);
                double y = o.y + DROP * (1.0 - e);
                boss.setPos(o.x, y, o.z);
                if (tick % 2 == 0) {
                    l.sendParticles(ParticleTypes.PORTAL, o.x, y + 2.0, o.z, 20, 0.6, 1.5, 0.6, 0.5);
                    l.sendParticles(ParticleTypes.REVERSE_PORTAL, o.x, y + 0.5, o.z, 6, 0.4, 0.2, 0.4, 0.05);
                }
            });
        }
    }

    private static void land(ServerLevel l, Vec3 o) {
        Archimago boss = find(l);
        if (boss != null) {
            boss.setPos(o.x, o.y, o.z);
        }
        screen(l, ScreenFxPacket.FLASH, 0xFFFFFF, 8);
        shake(l, 26, 40);
        sound(l, o, ModSounds.BOSS_SHATTER.get(), 4.0F, 1.2F);
        sound(l, o, SoundEvents.BEACON_ACTIVATE, 5.0F, 0.6F);
        Vfx.flash(l, o.add(0, 2.5, 0), PALE, 12.0, 14);
        Vfx.dome(l, o.add(0, 0.2, 0), VIOLET, 10.0, 22, true);
        Vfx.ring(l, o.add(0, 0.2, 0), GOLD, 7.0, 24);
        Vfx.ring(l, o.add(0, 0.25, 0), ARCANE, 13.0, 30);
        for (int ring = 0; ring < 6; ring++) {
            final int r = ring;
            Scheduler.runLater(ring * 2, () -> {
                double rad = 2.0 + r * 2.6;
                for (int i = 0; i < 36; i++) {
                    double a = Math.PI * 2.0 * i / 36;
                    l.sendParticles(ParticleTypes.ENCHANT, o.x + Math.cos(a) * rad, o.y + 0.4, o.z + Math.sin(a) * rad, 2, 0.1, 0.3, 0.1, 0.4);
                    l.sendParticles(ParticleTypes.WITCH, o.x + Math.cos(a) * rad, o.y + 0.3, o.z + Math.sin(a) * rad, 1, 0.1, 0.1, 0.1, 0.02);
                }
            });
        }
    }

    // ------------------------------------------------------------------ jefe

    public static Archimago find(ServerLevel l) {
        for (Archimago b : l.getEntitiesOfClass(Archimago.class, ARENA)) {
            if (b.isAlive()) {
                return b;
            }
        }
        return null;
    }

    /** El Archimago de la escena: quieto e invulnerable hasta que termine la intro. Si ya existe uno lo reutiliza. */
    private static Archimago spawnBoss(ServerLevel l) {
        Archimago boss = find(l);
        Vec3 spot = TowerArena.bossSpot();
        if (boss == null) {
            boss = ModEntities.ARCHIMAGO.get().create(l);
            if (boss == null) {
                return null;
            }
            boss.moveTo(spot.x, spot.y + DROP, spot.z, 0.0F, 0.0F);
            boss.setHome(TowerArena.center());
            l.addFreshEntity(boss);
        }
        boss.setInvulnerable(true);
        boss.setNoAi(true);
        boss.setNoGravity(true);
        return boss;
    }

    // ------------------------------------------------------------------ pequeños

    private static void sound(ServerLevel l, Vec3 at, SoundEvent s, float vol, float pitch) {
        l.playSound(null, at.x, at.y + 2.0, at.z, s, SoundSource.HOSTILE, vol, pitch);
    }

    private static void screen(ServerLevel l, int kind, int rgb, int ticks) {
        for (ServerPlayer p : l.players()) {
            Fx.screen(p, kind, rgb, ticks);
        }
    }

    private static void shake(ServerLevel l, int tenthsOfDegree, int ticks) {
        screen(l, ScreenFxPacket.SHAKE, tenthsOfDegree, ticks);
    }

    private static void title(ServerLevel l, String title, String subtitle, int rgb) {
        for (ServerPlayer p : l.players()) {
            ModNetwork.sendTo(p, new BossTitlePacket(title, subtitle, rgb, 100));
        }
    }

    // ------------------------------------------------------------------ uso

    /** ¿Se puede poner la intro a estos jugadores? (si alguno ya ve otra cinemática, no). */
    public static boolean canPlay(List<ServerPlayer> players) {
        if (players.isEmpty()) {
            return false;
        }
        for (ServerPlayer p : players) {
            if (CinematicManager.isWatching(p.getUUID())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pone la intro. Al terminar (o al saltarla) el Archimago queda en su sitio, quieto e invulnerable, y se llama a {@code onDone};
     * si la torre se reinició durante la escena solo se limpia y {@code onDone} no se llama.
     */
    public static void play(ServerLevel level, List<ServerPlayer> watchers, Runnable onDone) {
        Props.clear(level);
        final int epoch = DungeonReset.epoch(level);
        for (ServerPlayer p : watchers) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, INTRO.duration() + 30, 6, false, false, false));
        }
        CinematicManager.play(INTRO, level, TowerArena.bossSpot(), watchers, () -> {
            for (ServerPlayer p : level.players()) {
                p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
            Props.clear(level);
            Props.take(BOOKS, level);
            if (DungeonReset.epoch(level) != epoch) {
                return;
            }
            Archimago boss = spawnBoss(level);
            if (boss != null) {
                Vec3 s = TowerArena.bossSpot();
                boss.setPos(s.x, s.y, s.z);
                boss.setNoGravity(false);
            }
            if (onDone != null) {
                onDone.run();
            }
        });
    }
}
