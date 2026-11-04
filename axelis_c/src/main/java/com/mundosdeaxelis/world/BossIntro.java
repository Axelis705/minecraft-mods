package com.mundosdeaxelis.world;

import com.mundosdeaxelis.cinematic.Cinematic;
import com.mundosdeaxelis.cinematic.CinematicManager;
import com.mundosdeaxelis.entity.GranHerrero;
import com.mundosdeaxelis.entity.ReyBrote;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.BossTitlePacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Introducción épica de los jefes de las dungeons: cámara que recorre la arena, la sala se oscurece y tiembla, un círculo
 * brillante se enciende, el jefe sale del suelo entre explosiones y aparece su título. Mientras dura, los jugadores están
 * quietos y protegidos, y el jefe no puede atacar. Si se salta la escena, el jefe aparece y la pelea empieza de inmediato.
 */
public final class BossIntro {
    public enum Kind {
        FORGE, FOREST
    }

    private static final AABB ARENA = new AABB(-60, 0, -60, 60, 200, 60);

    private BossIntro() {
    }

    // ------------------------------------------------------------------ las dos escenas (cámara relativa al lugar del jefe)

    /** Cámara de la Forja: vista alta del círculo, baja junto a la lava cuando el coloso sube, y plano final de él. */
    private static Cinematic.Builder cameraForge(Cinematic.Builder b) {
        // Cámara tranquila: casi siempre por el carril despejado del sur, lejos del lago, los pinchos y los pilares, con un
        // acercamiento lento. Nada de pasar rozando objetos.
        return b.bars(true).duration(190).fadeOut(18)
                .frame(0, 0.0, 10.0, 26.0, 0.0, 1.0, 0.0)
                .frame(110, -2.5, 6.5, 21.5, 0.0, 3.0, 0.0)
                .frame(190, 0.0, 4.5, 18.0, 0.0, 5.5, 0.0);
    }

    /** Cámara del Bosque: mira al cielo, sigue la caída del jefe hasta el suelo y se acerca a él. */
    private static Cinematic.Builder cameraForest(Cinematic.Builder b) {
        // la vaina ya está en lo alto desde el primer cuadro (y baja despacio); la cámara la sigue hasta el suelo
        return b.bars(true).duration(165).fadeOut(18)
                .frame(0, 0.0, 2.0, 24.0, 0.0, 85.0, 0.0)
                .frame(30, -8.0, 2.5, 20.0, 0.0, 78.0, 0.0)
                .frame(55, -13.0, 3.5, 14.0, 0.0, 52.0, 0.0)
                .frame(61, -13.5, 3.5, 13.0, 0.0, 42.0, 0.0)
                .frame(68, -14.0, 3.3, 12.0, 0.0, 28.0, 0.0)
                .frame(75, -14.0, 3.2, 11.0, 0.0, 12.0, 0.0)
                .frame(82, -14.0, 3.2, 10.5, 0.0, 3.0, 0.0)
                .frame(96, -7.0, 2.0, 9.0, 0.0, 3.0, 0.0)
                .frame(125, 6.0, 2.2, 10.0, 0.0, 4.0, 0.0)
                .frame(165, 0.0, 2.0, 14.0, 0.0, 6.0, 0.0);
    }

    // ---- lago de lava de la Forja

    private static final Map<BlockPos, BlockState> LAKE = new HashMap<>();

    private static boolean inLake(int x, int z) {
        long h = (x * 73856093L) ^ (z * 19349663L);
        return Math.hypot(x, z) <= 4.6 + ((h >>> 3) & 7) * 0.12;
    }

    /**
     * Convierte el círculo central (entre los radios from y to) en una costra de magma o en lava de 3 bloques de hondo. Guarda lo
     * que había para devolverlo después.
     */
    private static void lakeStage(ServerLevel l, double from, double to, boolean lava) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                double d = Math.hypot(x, z);
                if (d <= from || d > to || !inLake(x, z)) {
                    continue;
                }
                for (int y = ForgeArena.Y - 2; y <= ForgeArena.Y; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    LAKE.putIfAbsent(pos, l.getBlockState(pos));
                    if (lava) {
                        l.setBlock(pos, Blocks.LAVA.defaultBlockState(), 2);
                    } else if (y == ForgeArena.Y) {
                        l.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
                    }
                }
                double top = ForgeArena.Y + 1.0;
                l.sendParticles(lava ? ParticleTypes.LAVA : ParticleTypes.SMOKE, x + 0.5, top + 0.1, z + 0.5, lava ? 2 : 3, 0.3, 0.1, 0.3, 0.02);
                if (lava && l.random.nextInt(3) == 0) {
                    l.sendParticles(ParticleTypes.FLAME, x + 0.5, top + 0.2, z + 0.5, 2, 0.2, 0.3, 0.2, 0.04);
                }
            }
        }
    }

    /** Devuelve el suelo tal como estaba (se llama al terminar la escena o al saltarla). */
    private static void lakeRestore(ServerLevel l) {
        if (LAKE.isEmpty()) {
            return;
        }
        for (Map.Entry<BlockPos, BlockState> e : LAKE.entrySet()) {
            l.setBlock(e.getKey(), e.getValue(), 3);
        }
        LAKE.clear();
        Vec3 c = ForgeArena.bossSpot();
        l.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.3, c.z, 40, 2.6, 0.2, 2.6, 0.03);
        l.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.3, c.z, 30, 2.6, 0.2, 2.6, 0.05);
    }

    /** Chorros de lava y chispas que suben desde el lago. */
    private static void lavaGeysers(ServerLevel l, Vec3 o, int n, double power) {
        for (int i = 0; i < n; i++) {
            double a = l.random.nextDouble() * Math.PI * 2.0;
            double d = l.random.nextDouble() * 4.6;
            double x = o.x + Math.cos(a) * d;
            double z = o.z + Math.sin(a) * d;
            double vy = power * (0.6 + l.random.nextDouble() * 0.7);
            l.sendParticles(ParticleTypes.LAVA, x, o.y, z, 0, (l.random.nextDouble() - 0.5) * 0.3, 1.0, (l.random.nextDouble() - 0.5) * 0.3, vy);
            if (i % 2 == 0) {
                l.sendParticles(ParticleTypes.FLAME, x, o.y + 0.2, z, 0, (l.random.nextDouble() - 0.5) * 0.2, 1.0, (l.random.nextDouble() - 0.5) * 0.2, vy * 0.9);
            }
        }
    }

    // ------------------------------------------------------------------ modelos 3D de las escenas (bloques display)

    private record PodPart(Props.Piece piece, Vector3f off, Quaternionf base) {
    }

    private static final List<PodPart> POD = new ArrayList<>();

    private static Quaternionf rotXYZ(double x, double y, double z) {
        return new Quaternionf().rotationXYZ((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
    }

    private static void podAdd(ServerLevel l, Vec3 c, Block b, float size, int light, Vector3f off, Quaternionf base) {
        Props.Piece p = Props.spawn(l, b, c.add(off.x, off.y, off.z), size, size, size, true, light, base);
        if (p != null) {
            POD.add(new PodPart(p, off, base));
        }
    }

    /** Vaina-meteoro del Rey Brote: una semilla gigante de hojas y musgo con el núcleo de gelatina brillando y una cola de hojas. */
    private static void podSpawn(ServerLevel l, Vec3 c) {
        podClear();
        podAdd(l, c, Blocks.SLIME_BLOCK, 2.5F, 15, new Vector3f(), new Quaternionf());
        podAdd(l, c, Blocks.MOSS_BLOCK, 2.9F, 10, new Vector3f(), rotXYZ(0, 45, 0));
        podAdd(l, c, Blocks.OAK_LEAVES, 3.7F, 12, new Vector3f(), new Quaternionf());
        podAdd(l, c, Blocks.OAK_LEAVES, 3.7F, 12, new Vector3f(), rotXYZ(0, 45, 0));
        podAdd(l, c, Blocks.FLOWERING_AZALEA_LEAVES, 3.4F, 12, new Vector3f(), rotXYZ(45, 0, 45));
        podAdd(l, c, Blocks.OAK_LEAVES, 3.4F, 12, new Vector3f(), rotXYZ(45, 30, -45));
        // cola
        podAdd(l, c, Blocks.OAK_LEAVES, 2.2F, 12, new Vector3f(0, 3.3F, 0), rotXYZ(0, 45, 0));
        podAdd(l, c, Blocks.SLIME_BLOCK, 1.1F, 15, new Vector3f(0, 4.5F, 0), rotXYZ(30, 20, 30));
        podAdd(l, c, Blocks.OAK_LEAVES, 1.5F, 12, new Vector3f(0, 5.4F, 0), rotXYZ(0, 20, 0));
        podAdd(l, c, Blocks.MOSS_BLOCK, 0.8F, 10, new Vector3f(0, 6.3F, 0), rotXYZ(45, 45, 0));
    }

    private static void podMove(Vec3 c, double spin) {
        for (PodPart pp : POD) {
            if (pp.piece().entity().isRemoved()) {
                continue;
            }
            Quaternionf q = new Quaternionf().rotationY((float) spin);
            q.mul(pp.base());
            pp.piece().put(c.add(pp.off().x, pp.off().y, pp.off().z), q);
        }
    }

    private static void podClear() {
        for (PodPart pp : POD) {
            pp.piece().remove();
        }
        POD.clear();
    }

    /** Lanza piezas por el aire (con gravedad) y las quita al acabar. */
    private static void fling(ServerLevel l, List<Props.Piece> pieces, Vec3 from, double speed, double up, int ticks) {
        final int n = pieces.size();
        final double[][] v = new double[n][3];
        final double[][] spin = new double[n][3];
        final Vec3[] pos = new Vec3[n];
        for (int i = 0; i < n; i++) {
            double a = l.random.nextDouble() * Math.PI * 2.0;
            double sp = speed * (0.4 + l.random.nextDouble() * 0.9);
            v[i][0] = Math.cos(a) * sp;
            v[i][1] = up * (0.5 + l.random.nextDouble());
            v[i][2] = Math.sin(a) * sp;
            for (int k = 0; k < 3; k++) {
                spin[i][k] = (l.random.nextDouble() - 0.5) * 0.5;
            }
            pos[i] = pieces.get(i).entity().position();
        }
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
                    v[i][1] -= 0.07;
                    v[i][0] *= 0.985;
                    v[i][2] *= 0.985;
                    pos[i] = pos[i].add(v[i][0], v[i][1], v[i][2]);
                    if (pos[i].y < from.y - 0.2 && v[i][1] < 0) {
                        pos[i] = new Vec3(pos[i].x, from.y - 0.2, pos[i].z);
                        v[i][1] = 0;
                        v[i][0] *= 0.6;
                        v[i][2] *= 0.6;
                    }
                    pc.put(pos[i], rotXYZ(tick * spin[i][0] * 57.3, tick * spin[i][1] * 57.3, tick * spin[i][2] * 57.3));
                }
            });
        }
    }

    /** La vaina se rompe: trozos de hojas, musgo, tierra y gelatina salen volando. */
    private static void podShatter(ServerLevel l, Vec3 ground, Vec3 center) {
        podClear();
        Block[] kinds = {Blocks.MOSS_BLOCK, Blocks.OAK_LEAVES, Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.SLIME_BLOCK, Blocks.FLOWERING_AZALEA_LEAVES};
        List<Props.Piece> bits = new ArrayList<>();
        for (int i = 0; i < 22; i++) {
            float sz = 0.45F + l.random.nextFloat() * 0.9F;
            Block b = kinds[l.random.nextInt(kinds.length)];
            Props.Piece p = Props.spawn(l, b, center.add((l.random.nextDouble() - 0.5) * 2.0, (l.random.nextDouble() - 0.5) * 2.0, (l.random.nextDouble() - 0.5) * 2.0),
                    sz, sz, sz, true, b == Blocks.SLIME_BLOCK ? 15 : -1, new Quaternionf());
            if (p != null) {
                bits.add(p);
            }
        }
        fling(l, bits, ground, 0.55, 0.55, 30);
    }

    // ---- cadenas y pinchos de la Forja

    private static final List<List<Props.Piece>> CHAINS = new ArrayList<>();
    private static final List<Vec3> CHAIN_BASE = new ArrayList<>();
    private static final double CHAIN_TOP = 30.0;

    private record Spike(Props.Piece shaft, Props.Piece tip, Vec3 base, Quaternionf rot, double len) {
    }

    private static final List<Spike> SPIKES = new ArrayList<>();

    /** Seis cadenas de hierro bajan del techo hasta clavarse en el lago de lava, alrededor del lugar donde saldrá el jefe. */
    private static void chainsDrop(ServerLevel l, Vec3 o) {
        chainsClear();
        final int segs = 10;
        for (int c = 0; c < 6; c++) {
            double a = Math.PI * 2.0 * c / 6 + 0.26;
            Vec3 base = new Vec3(o.x + Math.cos(a) * 2.9, o.y - 2.0, o.z + Math.sin(a) * 2.9);
            List<Props.Piece> chain = new ArrayList<>();
            for (int j = 0; j < segs; j++) {
                Props.Piece p = Props.spawn(l, Blocks.CHAIN, base.add(0, 70.0 + j * 3.0, 0), 1.7F, 3.0F, 1.7F, false, 9, new Quaternionf());
                if (p != null) {
                    chain.add(p);
                }
            }
            CHAINS.add(chain);
            CHAIN_BASE.add(base);
        }
        final int steps = 14;
        for (int k = 1; k <= steps; k++) {
            final int step = k;
            Scheduler.runLater(k + 2, () -> {
                double f = (double) step / steps;
                double dy = 70.0 * (1.0 - f * f);
                for (int c = 0; c < CHAINS.size(); c++) {
                    List<Props.Piece> chain = CHAINS.get(c);
                    for (int j = 0; j < chain.size(); j++) {
                        if (!chain.get(j).entity().isRemoved()) {
                            chain.get(j).put(CHAIN_BASE.get(c).add(0, dy + j * 3.0, 0));
                        }
                    }
                }
                if (step == steps) {
                    sound(l, o, SoundEvents.ANVIL_LAND, 4.0F, 0.5F);
                    sound(l, o, SoundEvents.CHAIN_PLACE, 5.0F, 0.5F);
                    shake(l, 18, 24);
                    for (Vec3 b : CHAIN_BASE) {
                        l.sendParticles(ParticleTypes.LAVA, b.x, o.y + 0.5, b.z, 10, 0.4, 0.2, 0.4, 0.2);
                        l.sendParticles(ParticleTypes.CRIT, b.x, o.y + 0.5, b.z, 12, 0.3, 0.3, 0.3, 0.5);
                    }
                }
            });
        }
    }

    /** Las cadenas se revientan cuando el coloso sale: eslabones y chispas salen disparados. */
    private static void chainsSnap(ServerLevel l, Vec3 o) {
        List<Props.Piece> all = new ArrayList<>();
        for (List<Props.Piece> chain : CHAINS) {
            all.addAll(chain);
        }
        CHAINS.clear();
        CHAIN_BASE.clear();
        if (all.isEmpty()) {
            return;
        }
        sound(l, o, SoundEvents.CHAIN_BREAK, 6.0F, 0.6F);
        sound(l, o, SoundEvents.ANVIL_DESTROY, 3.0F, 0.6F);
        l.sendParticles(ParticleTypes.CRIT, o.x, o.y + 4.0, o.z, 80, 3.0, 3.0, 3.0, 0.6);
        fling(l, all, o.add(0, -2.0, 0), 0.5, 0.9, 40);
    }

    private static void chainsClear() {
        for (List<Props.Piece> chain : CHAINS) {
            for (Props.Piece p : chain) {
                p.remove();
            }
        }
        CHAINS.clear();
        CHAIN_BASE.clear();
    }

    private static void spikePlace(Spike sp, double dy) {
        if (sp.shaft().entity().isRemoved()) {
            return;
        }
        Vec3 b = sp.base().add(0, dy, 0);
        sp.shaft().put(b, sp.rot());
        Vector3f t = new Vector3f(0, (float) sp.len(), 0);
        sp.rot().transform(t);
        sp.tip().put(b.add(t.x, t.y, t.z), sp.rot());
    }

    /** Ocho pinchos de roca negra con la punta al rojo vivo brotan del suelo alrededor del lago. */
    private static void spikesRise(ServerLevel l, Vec3 o) {
        spikesClear();
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(22.5 + 45.0 * i);
            double r = 6.6 + (i % 2) * 0.8;
            double len = 5.0 + (i % 3) * 1.3;
            Vec3 base = new Vec3(o.x + Math.cos(a) * r, o.y - 0.4, o.z + Math.sin(a) * r);
            // inclinados hacia afuera
            double dx = Math.cos(a);
            double dz = Math.sin(a);
            Quaternionf rot = new Quaternionf().rotationAxis((float) Math.toRadians(17), (float) dz, 0.0F, (float) -dx);
            Props.Piece shaft = Props.spawn(l, Blocks.BLACKSTONE, base.add(0, -9.0, 0), 1.35F, (float) len, 1.35F, false, -1, rot);
            Props.Piece tip = Props.spawn(l, Blocks.MAGMA_BLOCK, base.add(0, -9.0 + len, 0), 1.0F, 1.0F, 1.0F, false, 15, rot);
            if (shaft != null && tip != null) {
                SPIKES.add(new Spike(shaft, tip, base, rot, len));
            }
        }
        sound(l, o, SoundEvents.GENERIC_EXPLODE, 3.0F, 0.5F);
        final int steps = 14;
        for (int k = 1; k <= steps; k++) {
            final int step = k;
            Scheduler.runLater(k, () -> {
                double f = (double) step / steps;
                double e = 1.0 - (1.0 - f) * (1.0 - f) * (1.0 - f);
                for (Spike sp : SPIKES) {
                    spikePlace(sp, -9.0 * (1.0 - e));
                }
                if (step % 4 == 0) {
                    for (Spike sp : SPIKES) {
                        l.sendParticles(ParticleTypes.LARGE_SMOKE, sp.base().x, sp.base().y + 0.3, sp.base().z, 3, 0.5, 0.1, 0.5, 0.02);
                    }
                }
            });
        }
    }

    private static void spikesSink(ServerLevel l) {
        final int steps = 12;
        for (int k = 1; k <= steps; k++) {
            final int step = k;
            Scheduler.runLater(k, () -> {
                double f = (double) step / steps;
                for (Spike sp : SPIKES) {
                    spikePlace(sp, -9.0 * f * f);
                }
                if (step == steps) {
                    spikesClear();
                }
            });
        }
    }

    private static void spikesClear() {
        for (Spike sp : SPIKES) {
            sp.shaft().remove();
            sp.tip().remove();
        }
        SPIKES.clear();
    }

    /** Quita todos los modelos de escena. */
    private static void propsClear(ServerLevel l) {
        POD.clear();
        CHAINS.clear();
        CHAIN_BASE.clear();
        SPIKES.clear();
        Props.clear(l);
    }

    private static final Cinematic FORGE = cameraForge(Cinematic.builder("boss_forge_intro"))
            .subtitle(8, 80, "Bajo el yunque, algo muy antiguo despierta...")
            .subtitle(85, 135, "La forja se funde.")
            .cue(0, (l, o) -> {
                sound(l, o, ModSounds.BOSS_RUMBLE.get(), 5.0F, 1.0F);
                screen(l, ScreenFxPacket.DARK, 0x401000, 150);
            })
            .cue(15, (l, o) -> {
                Vfx.rune(l, o.add(0, 0.1, 0), 0xFF7A2A, 10.0, 170);
            })
            // el círculo del centro se agrieta: primero costra de magma, de dentro hacia afuera
            .cue(24, (l, o) -> {
                lakeStage(l, -1.0, 2.2, false);
                sound(l, o, SoundEvents.LAVA_POP, 4.0F, 0.6F);
            })
            .cue(32, (l, o) -> {
                lakeStage(l, 2.2, 5.6, false);
                embers(l, o);
                sound(l, o, SoundEvents.LAVA_AMBIENT, 4.0F, 0.7F);
            })
            .cue(45, (l, o) -> sound(l, o, ModSounds.BOSS_RISER.get(), 5.0F, 1.0F))
            // ...y luego se funde y se vuelve un lago de lava
            .cue(50, (l, o) -> {
                lakeStage(l, -1.0, 2.0, true);
                lavaGeysers(l, o, 8, 0.5);
                sound(l, o, SoundEvents.LAVA_POP, 5.0F, 0.5F);
            })
            .cue(58, (l, o) -> {
                lakeStage(l, 2.0, 3.6, true);
                lavaGeysers(l, o, 8, 0.55);
                sound(l, o, SoundEvents.LAVA_POP, 5.0F, 0.6F);
            })
            .cue(64, (l, o) -> {
                lakeStage(l, 3.6, 5.6, true);
                lavaGeysers(l, o, 10, 0.6);
            })
            .cue(66, (l, o) -> chainsDrop(l, o))
            .cue(70, (l, o) -> pillars(l, o, 0xFF6A1A, 9.0))
            .cue(85, (l, o) -> {
                sound(l, o, ModSounds.BOSS_SHATTER.get(), 5.0F, 0.8F);
                lavaGeysers(l, o, 22, 0.9);
                debris(l, o, Blocks.MAGMA_BLOCK.defaultBlockState(), 60);
                shake(l, 14, 26);
                spikesRise(l, o);
            })
            .cue(95, (l, o) -> spawnRising(l, o, Kind.FORGE))
            .cue(120, (l, o) -> {
                sound(l, o, ModSounds.BOSS_IMPACT.get(), 6.0F, 1.0F);
                shake(l, 24, 30);
                Vfx.flash(l, o.add(0, 3.0, 0), 0xFFE0A0, 9.0, 16);
                Vfx.ring(l, o.add(0, 0.3, 0), 0xFF5A20, 26.0, 34);
                lavaGeysers(l, o, 30, 1.0);
                debris(l, o, Blocks.MAGMA_BLOCK.defaultBlockState(), 50);
                chainsSnap(l, o);
            })
            // la lava se enfría en una costra y el suelo vuelve a quedar firme
            .cue(128, (l, o) -> {
                for (BlockPos pos : new ArrayList<>(LAKE.keySet())) {
                    if (pos.getY() == ForgeArena.Y) {
                        l.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
                    }
                }
                sound(l, o, SoundEvents.LAVA_EXTINGUISH, 5.0F, 0.7F);
                l.sendParticles(ParticleTypes.CLOUD, o.x, o.y + 0.3, o.z, 40, 2.6, 0.3, 2.6, 0.06);
            })
            .cue(135, (l, o) -> {
                sound(l, o, ModSounds.HERRERO_ROAR.get(), 6.0F, 0.85F);
                embers(l, o);
                lakeRestore(l);
                spikesSink(l);
            })
            .cue(150, (l, o) -> {
                title(l, "GRAN HERRERO DE BRASAS", "EL COLOSO DE LA FORJA", 0xFFB040);
                sound(l, o, ModSounds.BOSS_TITLE_FORGE.get(), 4.0F, 1.0F);
                screen(l, ScreenFxPacket.FLASH, 0xFFFFFF, 7);
            })
            .build();

    private static final Cinematic FOREST = cameraForest(Cinematic.builder("boss_forest_intro"))
            .subtitle(8, 52, "Una sombra crece sobre el bosque...")
            .subtitle(56, 95, "¡Algo cae del cielo!")
            .cue(0, (l, o) -> {
                spawnFalling(l, o);
                sound(l, o, ModSounds.BOSS_RUMBLE.get(), 5.0F, 1.1F);
                shake(l, 6, 130);
                screen(l, ScreenFxPacket.DARK, 0x103010, 140);
            })
            // marca en el suelo: ahí va a caer
            .cue(12, (l, o) -> {
                Vfx.rune(l, o.add(0, 0.1, 0), 0x5CD66C, 10.0, 160);
                Vfx.ring(l, o.add(0, 0.2, 0), 0xA6F2A4, 14.0, 40);
            })
            .cue(30, (l, o) -> {
                spores(l, o);
                Vfx.ring(l, o.add(0, 0.2, 0), 0x7BE070, 18.0, 44);
            })
            .cue(38, (l, o) -> sound(l, o, ModSounds.BOSS_RISER.get(), 5.0F, 1.0F))
            // un rayo de luz baja del cielo y por él cae el jefe
            .cue(14, (l, o) -> Vfx.column(l, o.add(0, 0.1, 0), 0x9CFFA0, 2.2, 125.0, 135))
            .cue(66, (l, o) -> Vfx.ring(l, o.add(0, 0.3, 0), 0x7BE070, 12.0, 20))
            .cue(74, (l, o) -> Vfx.ring(l, o.add(0, 0.3, 0), 0xB8FFB0, 12.0, 14))
            .cue(100, (l, o) -> {
                sound(l, o, ModSounds.BOSS_ROAR_FOREST.get(), 6.0F, 1.0F);
                shake(l, 30, 60);
                Vfx.ring(l, o.add(0, 0.2, 0), 0xFFD24A, 20.0, 30);
                spores(l, o);
            })
            .cue(120, (l, o) -> {
                title(l, "REY BROTE", "EL SLIME ANCESTRAL DEL BOSQUE", 0x8CF08C);
                sound(l, o, ModSounds.BOSS_TITLE_FOREST.get(), 4.0F, 1.0F);
                screen(l, ScreenFxPacket.FLASH, 0xFFFFFF, 7);
            })
            .build();

    // ------------------------------------------------------------------ entrada

    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> introIn;
    private static long introSince;

    /** Empieza la intro; al acabar (o al saltarla) el jefe queda listo para pelear. */
    public static void play(ServerLevel level, List<ServerPlayer> players, Kind kind) {
        // las escenas comparten piezas temporales (lago, vaina, cadenas): si otra copia de dungeon está en plena intro, esta espera su turno
        long now = level.getServer().getTickCount();
        if (introIn != null && !introIn.equals(level.dimension()) && now - introSince < 1800L) {
            Scheduler.runLater(20, () -> play(level, players, kind));
            return;
        }
        introIn = level.dimension();
        introSince = now;
        List<ServerPlayer> watchers = new ArrayList<>();
        for (ServerPlayer p : players) {
            if (p.isAlive() && !CinematicManager.isWatching(p.getUUID())) {
                watchers.add(p);
            }
        }
        if (watchers.isEmpty()) {
            finish(level, kind, true);
            return;
        }
        propsClear(level);
        Cinematic c = kind == Kind.FORGE ? FORGE : FOREST;
        for (ServerPlayer p : watchers) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, c.duration() + 30, 6, false, false, false));
        }
        int epoch = DungeonReset.epoch(level);
        CinematicManager.play(c, level, ForgeArena.bossSpot(), watchers, () -> finish(level, kind, DungeonReset.epoch(level) == epoch));
    }

    /** @param valid false si la dungeon se reinició durante la intro: solo se limpia, no sale ningún jefe. */
    private static void finish(ServerLevel level, Kind kind, boolean valid) {
        if (level.dimension().equals(introIn)) {
            introIn = null;
        }
        for (ServerPlayer p : level.players()) {
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        lakeRestore(level);
        propsClear(level);
        if (!valid) {
            return;
        }
        Mob boss = find(level, kind);
        if (boss == null) {
            spawn(level, kind);
            boss = find(level, kind);
        }
        if (boss != null) {
            Vec3 c = ForgeArena.bossSpot();
            boss.noPhysics = false;
            boss.setNoGravity(false);
            boss.setInvulnerable(false);
            boss.moveTo(c.x, c.y, c.z, 0.0F, 0.0F);
            boss.fallDistance = 0.0F;
            boss.setInvisible(false);
            boss.setNoAi(false);
        }
    }

    private static void spawn(ServerLevel level, Kind kind) {
        if (kind == Kind.FORGE) {
            ForgeTravel.spawnBoss(level);
        } else {
            BossTravel.spawnBoss(level);
        }
    }

    private static Mob find(ServerLevel level, Kind kind) {
        if (kind == Kind.FORGE) {
            for (GranHerrero b : level.getEntitiesOfClass(GranHerrero.class, ARENA)) {
                if (b.isAlive()) {
                    return b;
                }
            }
        } else {
            for (ReyBrote b : level.getEntitiesOfClass(ReyBrote.class, ARENA)) {
                if (b.isAlive()) {
                    return b;
                }
            }
        }
        return null;
    }

    /** El jefe aparece dentro del suelo, quieto e invulnerable, y sube hasta quedar de pie. */
    private static void spawnRising(ServerLevel level, Vec3 origin, Kind kind) {
        if (find(level, kind) == null) {
            spawn(level, kind);
        }
        Mob boss = find(level, kind);
        if (boss == null) {
            return;
        }
        double depth = boss.getBbHeight() + 0.6;
        boss.setNoAi(true);
        boss.setInvulnerable(true);
        boss.setNoGravity(true);
        boss.noPhysics = true;
        boss.setPos(origin.x, origin.y - depth, origin.z);
        final int steps = 20;
        for (int k = 1; k <= steps; k++) {
            final int step = k;
            Scheduler.runLater(k * 2, () -> {
                if (!boss.isAlive() || !boss.isNoAi()) {
                    return;
                }
                double f = (double) step / steps;
                double e = 1.0 - (1.0 - f) * (1.0 - f) * (1.0 - f);
                boss.setPos(origin.x, origin.y - depth * (1.0 - e), origin.z);
                if (step % 3 == 0) {
                    level.sendParticles(ParticleTypes.POOF, origin.x, origin.y + 0.2, origin.z, 6, 1.6, 0.1, 1.6, 0.03);
                }
            });
        }
    }

    /**
     * El jefe del bosque (dentro de la vaina) ya está en lo alto desde el primer momento y baja despacio al principio y cada vez más
     * rápido, como un meteoro; al tocar el suelo rebota una vez, como un slime.
     */
    private static void spawnFalling(ServerLevel level, Vec3 origin) {
        if (find(level, Kind.FOREST) == null) {
            spawn(level, Kind.FOREST);
        }
        Mob boss = find(level, Kind.FOREST);
        if (boss == null) {
            return;
        }
        final double height = 115.0;
        final int steps = 113;
        boss.setNoAi(true);
        boss.setInvulnerable(true);
        boss.setNoGravity(true);
        boss.noPhysics = true;
        boss.setPos(origin.x, origin.y + height, origin.z);
        boss.setInvisible(true);
        podSpawn(level, origin.add(0, height + 1.5, 0));
        Vfx.domeFollow(level, boss, 0x8CF08C, 3.2, steps + 10);
        for (int k = 1; k <= steps; k++) {
            final int step = k;
            Scheduler.runLater(k, () -> {
                if (!boss.isAlive() || !boss.isNoAi()) {
                    return;
                }
                double f = (double) step / steps;
                double y = origin.y + height * (1.0 - Math.pow(f, 2.2));
                boss.setPos(origin.x, y, origin.z);
                boss.setYRot(step * 8.0F);
                podMove(new Vec3(origin.x, y + 1.5, origin.z), step * 0.12);
                // estela de babas y esporas detrás de él (solo se ve cuando ya está cerca)
                level.sendParticles(ParticleTypes.ITEM_SLIME, origin.x, y + 1.0, origin.z, 8, 0.8, 0.8, 0.8, 0.05);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, origin.x, y + 2.2, origin.z, 4, 0.7, 1.2, 0.7, 0.0);
                level.sendParticles(ParticleTypes.CLOUD, origin.x, y + 2.5, origin.z, 3, 0.4, 1.5, 0.4, 0.01);
                if (step == steps) {
                    podShatter(level, origin, new Vec3(origin.x, origin.y + 1.5, origin.z));
                    boss.setInvisible(false);
                    landing(level, origin, 1.0F);
                    bounce(level, origin, boss);
                }
            });
        }
    }

    /** Un rebote pequeño tras el golpe: sube, cae otra vez y suelta un segundo impacto más suave. */
    private static void bounce(ServerLevel level, Vec3 origin, Mob boss) {
        final int steps = 16;
        for (int k = 1; k <= steps; k++) {
            final int step = k;
            Scheduler.runLater(k + 2, () -> {
                if (!boss.isAlive() || !boss.isNoAi()) {
                    return;
                }
                double u = (double) step / steps;
                boss.setPos(origin.x, origin.y + 3.2 * 4.0 * u * (1.0 - u), origin.z);
                if (step == steps) {
                    boss.setPos(origin.x, origin.y, origin.z);
                    landing(level, origin, 0.55F);
                }
            });
        }
    }

    /** Golpe contra el suelo: onda, sacudida, destello, tierra y babas. */
    private static void landing(ServerLevel level, Vec3 o, float power) {
        sound(level, o, ModSounds.BOSS_IMPACT.get(), 6.0F * power, 1.2F + (1.0F - power) * 0.4F);
        sound(level, o, SoundEvents.SLIME_SQUISH, 4.0F, 0.6F);
        shake(level, (int) (45 * power), 50);
        Vfx.flash(level, o.add(0, 2.0, 0), 0xE8FFC0, 9.0 * power, 14);
        Vfx.dome(level, o.add(0, 0.2, 0), 0x7BE070, 7.0 * power, 20, true);
        Vfx.ring(level, o.add(0, 0.2, 0), 0x5CD66C, 28.0 * power, 34);
        Vfx.ring(level, o.add(0, 0.25, 0), 0xFFD24A, 16.0 * power, 24);
        if (power > 0.9F) {
            screen(level, ScreenFxPacket.FLASH, 0xB8FFB0, 9);
        }
        debris(level, o, Blocks.MOSS_BLOCK.defaultBlockState(), (int) (120 * power));
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRASS_BLOCK.defaultBlockState()), o.x, o.y + 0.3, o.z, (int) (90 * power), 3.0, 0.3, 3.0, 0.4);
        level.sendParticles(ParticleTypes.ITEM_SLIME, o.x, o.y + 1.0, o.z, (int) (160 * power), 3.5, 1.2, 3.5, 0.2);
        level.sendParticles(ParticleTypes.CLOUD, o.x, o.y + 0.3, o.z, (int) (40 * power), 3.0, 0.2, 3.0, 0.2);
    }

    // ------------------------------------------------------------------ pequeños efectos

    private static void sound(ServerLevel l, Vec3 at, SoundEvent s, float vol, float pitch) {
        l.playSound(null, at.x, at.y + 2.0, at.z, s, SoundSource.HOSTILE, vol, pitch);
    }

    private static void shake(ServerLevel l, int tenthsOfDegree, int ticks) {
        screen(l, ScreenFxPacket.SHAKE, tenthsOfDegree, ticks);
    }

    private static void screen(ServerLevel l, int kind, int rgb, int ticks) {
        for (ServerPlayer p : l.players()) {
            Fx.screen(p, kind, rgb, ticks);
        }
    }

    private static void title(ServerLevel l, String title, String subtitle, int rgb) {
        for (ServerPlayer p : l.players()) {
            ModNetwork.sendTo(p, new BossTitlePacket(title, subtitle, rgb, 100));
        }
    }

    /** Columnas de luz alrededor del círculo. */
    private static void pillars(ServerLevel l, Vec3 o, int rgb, double r) {
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2.0 * i / 6 + 0.3;
            Vfx.column(l, o.add(Math.cos(a) * r, 0.1, Math.sin(a) * r), rgb, 0.9, 16.0, 110);
        }
    }

    private static void embers(ServerLevel l, Vec3 o) {
        for (int i = 0; i < 40; i++) {
            double a = l.random.nextDouble() * Math.PI * 2.0;
            double d = 2.0 + l.random.nextDouble() * 10.0;
            l.sendParticles(ParticleTypes.LAVA, o.x + Math.cos(a) * d, o.y + 0.2, o.z + Math.sin(a) * d, 1, 0.0, 0.0, 0.0, 0.0);
            l.sendParticles(ParticleTypes.FLAME, o.x + Math.cos(a) * d, o.y + 0.3, o.z + Math.sin(a) * d, 2, 0.1, 0.4, 0.1, 0.03);
        }
    }

    private static void spores(ServerLevel l, Vec3 o) {
        for (int i = 0; i < 50; i++) {
            double a = l.random.nextDouble() * Math.PI * 2.0;
            double d = 2.0 + l.random.nextDouble() * 11.0;
            l.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, o.x + Math.cos(a) * d, o.y + 0.5 + l.random.nextDouble() * 3.0, o.z + Math.sin(a) * d, 2, 0.3, 0.5, 0.3, 0.01);
            l.sendParticles(ParticleTypes.HAPPY_VILLAGER, o.x + Math.cos(a) * d, o.y + 0.4, o.z + Math.sin(a) * d, 1, 0.2, 0.4, 0.2, 0.0);
        }
    }

    private static void slime(ServerLevel l, Vec3 o) {
        l.sendParticles(ParticleTypes.ITEM_SLIME, o.x, o.y + 1.5, o.z, 120, 2.5, 1.5, 2.5, 0.15);
    }

    private static void debris(ServerLevel l, Vec3 o, net.minecraft.world.level.block.state.BlockState st, int n) {
        l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), o.x, o.y + 0.3, o.z, n, 2.0, 0.4, 2.0, 0.35);
        l.sendParticles(ParticleTypes.EXPLOSION, o.x, o.y + 0.5, o.z, 6, 2.0, 0.3, 2.0, 0.0);
    }
}
