package com.mundosdeaxelis.world;

import com.mundosdeaxelis.cinematic.Cinematic;
import com.mundosdeaxelis.cinematic.CinematicManager;
import com.mundosdeaxelis.entity.LoboArtico;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.BossTitlePacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Intro épica del Lobo Ártico (solo el jefe, sin dungeon): ventisca sobre la arena, pares de ojos azules que se encienden en la
 * nieve, un monolito de hielo que sube del suelo con un círculo de runas, grietas, y por fin el monolito estalla y el lobo salta
 * dentro de una ola de nieve, aúlla y aparece su título. La cámara se mueve relativa al punto del jefe ({@link IceArena#bossSpot()}).
 * Los jugadores van hacia el sur del jefe (z positivo): la cámara empieza detrás de ellos, mira al norte.
 */
public final class IceIntro {
    private static final AABB ARENA = new AABB(-40, 40, -40, 40, 120, 40);
    private static final int ICE = 0x9FE3FF;
    private static final int WHITE = 0xE8F8FF;

    private IceIntro() {
    }

    private static Cinematic build() {
        return Cinematic.builder("lobo_artico_intro")
                .bars(true)
                .duration(270)
                .fadeOut(25)
                // 1) vista alta de la arena en plena ventisca, bajando despacio
                .frame(0, 0, 12, 36, 0, 4, 0)
                .frame(50, 0, 7, 28, 0, 3, -2)
                // 2) giro hacia los ojos del borde de la arena
                .frame(85, -9, 3, 20, -14, 1.5, 4)
                .frame(115, 8, 2.5, 16, 15, 1.5, 2)
                // 3) a ras de suelo frente al monolito que sube
                .frame(135, 4, 1.4, 13, 0, 4, 0)
                .frame(175, 2, 2.2, 9, 0, 4.5, 0)
                // 4) acercamiento nervioso antes de la explosión
                .frame(198, 1.2, 3.0, 6, 0, 4.5, 0)
                // 5) el golpe: cámara baja y atrás, mirando hacia arriba al lobo
                .frame(200, 0, 1.0, 14, 0, 4.5, 0)
                .frame(235, 0, 1.6, 12, 0, 5.0, 0)
                .frame(270, 0, 2.2, 10, 0, 5.2, 0)
                .subtitle(30, 78, "El viento se vuelve un aullido...")
                .subtitle(90, 130, "Algo observa desde la nieve.")
                .subtitle(210, 255, "Lobo Ártico, guardián del palacio de hielo.")
                .cue(0, (l, o) -> {
                    screen(l, ScreenFxPacket.WHITEOUT, 0xDDEEFF, 50);
                    sound(l, o, ModSounds.LOBO_WIND.get(), 7.0F, 0.7F);
                    blizzard(l, o, 0);
                })
                .cue(30, (l, o) -> sound(l, o, ModSounds.LOBO_HOWL.get(), 3.0F, 1.5F))
                // ojos azules que se encienden uno a uno en el borde
                .cue(58, (l, o) -> eyes(l, o, 0, 3))
                .cue(72, (l, o) -> eyes(l, o, 3, 6))
                .cue(88, (l, o) -> {
                    eyes(l, o, 6, 9);
                    sound(l, o, ModSounds.LOBO_AMBIENT.get(), 3.0F, 0.6F);
                })
                .cue(104, (l, o) -> {
                    eyes(l, o, 9, 12);
                    sound(l, o, ModSounds.LOBO_HOWL.get(), 4.0F, 1.2F);
                })
                // el monolito sube del suelo
                .cue(118, (l, o) -> {
                    sound(l, o, ModSounds.BOSS_RUMBLE.get(), 5.0F, 0.8F);
                    shake(l, 6, 90);
                    screen(l, ScreenFxPacket.DARK, 0x000814, 60);
                    Vfx.rune(l, o.add(0, 0.12, 0), ICE, 7.5, 160);
                    Vfx.ring(l, o.add(0, 0.12, 0), WHITE, 4.0, 60);
                    monolith(l, o);
                })
                .cue(150, (l, o) -> {
                    sound(l, o, ModSounds.BOSS_RISER.get(), 5.0F, 1.0F);
                    for (int i = 0; i < 6; i++) {
                        double a = Math.PI * 2.0 * i / 6 + 0.3;
                        Vfx.column(l, o.add(Math.cos(a) * 7.5, 0.1, Math.sin(a) * 7.5), ICE, 0.8, 18.0, 90);
                    }
                })
                .cue(172, (l, o) -> {
                    shake(l, 14, 40);
                    sound(l, o, SoundEvents.GLASS_BREAK, 3.0F, 0.5F);
                    l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()), o.x, o.y + 3.5, o.z, 60, 1.8, 3.0, 1.8, 0.15);
                })
                .cue(190, (l, o) -> {
                    shake(l, 22, 30);
                    sound(l, o, SoundEvents.GLASS_BREAK, 4.0F, 0.4F);
                    l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()), o.x, o.y + 3.5, o.z, 90, 2.0, 3.2, 2.0, 0.2);
                })
                // el estallido
                .cue(200, (l, o) -> burst(l, o))
                .cue(222, (l, o) -> {
                    title(l, "LOBO ÁRTICO", "GUARDIÁN DEL PALACIO DE HIELO", ICE);
                    sound(l, o, ModSounds.BOSS_IMPACT.get(), 4.0F, 0.9F);
                    screen(l, ScreenFxPacket.FLASH, 0xFFFFFF, 6);
                    Vfx.ring(l, o.add(0, 0.2, 0), WHITE, 14.0, 24);
                })
                .cue(245, (l, o) -> sound(l, o, ModSounds.LOBO_ROAR.get(), 5.0F, 0.9F))
                .build();
    }

    private static final Cinematic INTRO = build();

    // ------------------------------------------------------------------ efectos

    /** Ventisca continua sobre la arena mientras dura la escena. */
    private static void blizzard(ServerLevel l, Vec3 o, int t) {
        if (t > INTRO.duration()) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            double a = l.random.nextDouble() * Math.PI * 2.0;
            double d = l.random.nextDouble() * 22.0;
            l.sendParticles(ParticleTypes.SNOWFLAKE, o.x + Math.cos(a) * d, o.y + 1.0 + l.random.nextDouble() * 6.0, o.z + 13.5 + Math.sin(a) * d, 14, 3.0, 2.0, 3.0, 0.15);
        }
        l.sendParticles(ParticleTypes.CLOUD, o.x, o.y + 0.5, o.z + 6.0, 4, 10.0, 0.5, 10.0, 0.05);
        Scheduler.runLater(3, () -> blizzard(l, o, t + 3));
    }

    /** Posiciones de ojos en el borde de la arena (relativas al jefe). */
    private static Vec3 eyeSpot(Vec3 o, int i) {
        double a = Math.PI * (0.15 + 0.7 * ((i * 5) % 12) / 12.0) + (i % 2 == 0 ? Math.PI : 0.0);
        double r = 17.0 + (i % 3) * 2.5;
        return new Vec3(o.x + Math.cos(a) * r, o.y + 0.9 + (i % 4) * 0.4, o.z + 13.5 + Math.sin(a) * r);
    }

    private static void eyes(ServerLevel l, Vec3 o, int from, int to) {
        for (int i = from; i < to; i++) {
            Vec3 p = eyeSpot(o, i);
            Vec3 look = new Vec3(o.x - p.x, 0, o.z - p.z).normalize();
            Vec3 side = new Vec3(-look.z, 0, look.x).scale(0.28);
            for (int s = -1; s <= 1; s += 2) {
                Vec3 e = p.add(side.scale(s));
                Block b = Blocks.SEA_LANTERN;
                Props.Piece pc = Props.spawn(l, b, e, 0.2F, 0.1F, 0.1F, true, 15, new Quaternionf().rotateY((float) Math.atan2(-look.x, -look.z)));
                if (pc != null) {
                    // parpadeo: se apagan solos tras un rato
                    Scheduler.runLater(100, pc::remove);
                }
                l.sendParticles(ParticleTypes.SNOWFLAKE, e.x, e.y, e.z, 3, 0.1, 0.1, 0.1, 0.01);
            }
            l.playSound(null, p.x, p.y, p.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.4F, 1.6F);
        }
    }

    private static final List<Props.Piece> MONO = new ArrayList<>();

    /** El monolito de hielo: una columna con afilados cristales alrededor, sube del suelo. */
    private static void monolith(ServerLevel l, Vec3 o) {
        MONO.clear();
        final double depth = 7.5;
        Props.Piece core = Props.spawn(l, Blocks.BLUE_ICE, o.add(0, -depth, 0), 4.0F, 7.5F, 4.0F, false, 12, new Quaternionf());
        if (core != null) {
            MONO.add(core);
        }
        List<Props.Piece> crystals = new ArrayList<>();
        List<double[]> offs = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0 * i / 8;
            double d = 2.6;
            double h = 3.5 + (i % 3) * 1.2;
            Quaternionf q = new Quaternionf().rotateY((float) a).rotateZ(0.45F);
            Props.Piece pc = Props.spawn(l, i % 2 == 0 ? Blocks.PACKED_ICE : Blocks.BLUE_ICE,
                    o.add(Math.cos(a) * d, -depth, Math.sin(a) * d), 0.9F, (float) h, 0.9F, false, 10, q);
            if (pc != null) {
                crystals.add(pc);
                MONO.add(pc);
                offs.add(new double[]{Math.cos(a) * d, Math.sin(a) * d, a});
            }
        }
        final int steps = 30;
        for (int k = 1; k <= steps; k++) {
            final int step = k;
            Scheduler.runLater(k * 2, () -> {
                double f = (double) step / steps;
                double e = 1.0 - (1.0 - f) * (1.0 - f) * (1.0 - f);
                double y = o.y - depth * (1.0 - e);
                if (core != null && !core.entity().isRemoved()) {
                    core.put(new Vec3(o.x, y, o.z), new Quaternionf());
                }
                for (int i = 0; i < crystals.size(); i++) {
                    double[] of = offs.get(i);
                    Props.Piece pc = crystals.get(i);
                    if (!pc.entity().isRemoved()) {
                        pc.put(new Vec3(o.x + of[0], y, o.z + of[1]), new Quaternionf().rotateY((float) of[2]).rotateZ(0.45F));
                    }
                }
                if (step % 4 == 0) {
                    l.sendParticles(ParticleTypes.SNOWFLAKE, o.x, o.y + 0.3, o.z, 20, 2.5, 0.2, 2.5, 0.08);
                    l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), o.x, o.y + 0.3, o.z, 8, 2.5, 0.2, 2.5, 0.1);
                }
            });
        }
    }

    /** El monolito estalla: salen despedidos los trozos, la onda de nieve y el lobo. */
    private static void burst(ServerLevel l, Vec3 o) {
        screen(l, ScreenFxPacket.FLASH, 0xFFFFFF, 10);
        shake(l, 35, 50);
        sound(l, o, ModSounds.BOSS_SHATTER.get(), 6.0F, 0.8F);
        sound(l, o, ModSounds.LOBO_CRASH.get(), 5.0F, 0.9F);
        sound(l, o, ModSounds.LOBO_HOWL.get(), 6.0F, 0.8F);
        l.playSound(null, o.x, o.y, o.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 5.0F, 0.35F);
        Vfx.flash(l, o.add(0, 4.0, 0), WHITE, 14.0, 14);
        Vfx.dome(l, o.add(0, 0.2, 0), ICE, 12.0, 20, true);
        Vfx.column(l, o.add(0, 0.1, 0), WHITE, 2.2, 28.0, 40);
        Vfx.ring(l, o.add(0, 0.2, 0), ICE, 6.0, 20);
        // ola de nieve que se expande
        for (int ring = 0; ring < 6; ring++) {
            final int r = ring;
            Scheduler.runLater(ring * 2, () -> {
                double rad = 2.0 + r * 3.0;
                for (int i = 0; i < 40; i++) {
                    double a = Math.PI * 2.0 * i / 40;
                    l.sendParticles(ParticleTypes.CLOUD, o.x + Math.cos(a) * rad, o.y + 0.4, o.z + Math.sin(a) * rad, 1, 0.1, 0.2, 0.1, 0.05);
                    l.sendParticles(ParticleTypes.SNOWFLAKE, o.x + Math.cos(a) * rad, o.y + 0.8, o.z + Math.sin(a) * rad, 2, 0.2, 0.5, 0.2, 0.06);
                }
            });
        }
        l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()), o.x, o.y + 3.0, o.z, 220, 2.5, 3.0, 2.5, 0.5);
        l.sendParticles(ParticleTypes.EXPLOSION, o.x, o.y + 2.0, o.z, 8, 2.0, 2.0, 2.0, 0.0);
        // trozos que salen volando y caen
        fling(l, new ArrayList<>(MONO), o);
        MONO.clear();
        // el lobo, de pie en el centro del estallido
        LoboArtico boss = spawnBoss(l);
        if (boss != null) {
            boss.setPos(o.x, o.y, o.z);
        }
    }

    private static void fling(ServerLevel l, List<Props.Piece> pieces, Vec3 o) {
        final int n = pieces.size();
        final double[][] v = new double[n][3];
        final Vec3[] pos = new Vec3[n];
        for (int i = 0; i < n; i++) {
            double a = l.random.nextDouble() * Math.PI * 2.0;
            double sp = 0.25 + l.random.nextDouble() * 0.5;
            v[i][0] = Math.cos(a) * sp;
            v[i][1] = 0.35 + l.random.nextDouble() * 0.4;
            v[i][2] = Math.sin(a) * sp;
            pos[i] = pieces.get(i).entity().position();
        }
        final int ticks = 40;
        for (int t = 1; t <= ticks; t++) {
            final int tick = t;
            Scheduler.runLater(t, () -> {
                for (int i = 0; i < n; i++) {
                    Props.Piece pc = pieces.get(i);
                    if (pc.entity().isRemoved()) {
                        continue;
                    }
                    if (tick == ticks) {
                        pc.remove();
                        continue;
                    }
                    v[i][1] -= 0.045;
                    pos[i] = pos[i].add(v[i][0], v[i][1], v[i][2]);
                    if (pos[i].y < o.y) {
                        pos[i] = new Vec3(pos[i].x, o.y, pos[i].z);
                        v[i][0] *= 0.5;
                        v[i][2] *= 0.5;
                        v[i][1] = 0.0;
                    }
                    pc.put(pos[i], new Quaternionf().rotateXYZ(tick * 0.15F * (i % 3 + 1), tick * 0.1F, tick * 0.12F));
                }
            });
        }
    }

    // ------------------------------------------------------------------ jefe

    public static LoboArtico find(ServerLevel l) {
        for (LoboArtico b : l.getEntitiesOfClass(LoboArtico.class, ARENA)) {
            if (b.isAlive()) {
                return b;
            }
        }
        return null;
    }

    /** El lobo de la escena: quieto e invulnerable hasta que termine la intro. Si ya existe uno lo reutiliza. */
    private static LoboArtico spawnBoss(ServerLevel l) {
        LoboArtico boss = find(l);
        Vec3 spot = IceArena.bossSpot();
        if (boss == null) {
            boss = ModEntities.LOBO_ARTICO.get().create(l);
            if (boss == null) {
                return null;
            }
            boss.moveTo(spot.x, spot.y, spot.z, 0.0F, 0.0F);
            boss.setHome(IceArena.center());
            l.addFreshEntity(boss);
        }
        boss.setInvulnerable(true);
        boss.setNoAi(true);
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
     * Pone la intro. Al terminar (o al saltarla) el lobo queda en su sitio, quieto e invulnerable, y se llama a {@code onDone};
     * si la sala se reinició durante la escena solo se limpia y {@code onDone} no se llama.
     */
    public static void play(ServerLevel level, List<ServerPlayer> watchers, Runnable onDone) {
        Props.clear(level);
        final int epoch = DungeonReset.epoch(level);
        for (ServerPlayer p : watchers) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, INTRO.duration() + 30, 6, false, false, false));
        }
        CinematicManager.play(INTRO, level, IceArena.bossSpot(), watchers, () -> {
            for (ServerPlayer p : level.players()) {
                p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
            Props.clear(level);
            MONO.clear();
            if (DungeonReset.epoch(level) != epoch) {
                return;
            }
            LoboArtico boss = spawnBoss(level);
            if (boss != null) {
                Vec3 s = IceArena.bossSpot();
                boss.setPos(s.x, s.y, s.z);
            }
            if (onDone != null) {
                onDone.run();
            }
        });
    }
}
