package com.navidad.client;

import com.navidad.musica.CajaMusicaBE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * (Cliente) Lo que hace cada cuadro la caja de música en tu pantalla: abre o cierra la tapa poco a poco, gira el pinito y la manivela
 * (toman vuelo y frenan), suelta notitas de colores y toca el villancico desde la caja (se oye de dónde viene, con el volumen de
 * "Música de tocadiscos"); si cambian la canción o la paran, la corta.
 */
public final class CajaMusicaCliente {
    private CajaMusicaCliente() {
    }

    public static void tick(CajaMusicaBE be) {
        be.abiertaAntes = be.abierta;
        float meta = be.sonando ? 1F : 0F;
        be.abierta += Math.signum(meta - be.abierta) * Math.min(Math.abs(meta - be.abierta), be.sonando ? 0.09F : 0.06F);
        be.giroAntes = be.giro;
        float velMeta = be.sonando && be.abierta > 0.6F ? 5.5F : 0F;
        be.vel += (velMeta - be.vel) * 0.06F;
        be.giro += be.vel;
        var mc = Minecraft.getInstance();
        var lv = be.getLevel();
        if (lv == null) {
            return;
        }
        // el sonido
        Sonido s = be.sonido instanceof Sonido x ? x : null;
        if (be.sonando) {
            if (s == null || s.desde != be.desde || s.cancion != be.cancion) {
                if (s != null) {
                    mc.getSoundManager().stop(s);
                }
                Sonido nuevo = new Sonido(be);
                be.sonido = nuevo;
                mc.getSoundManager().play(nuevo);
            }
        } else if (s != null) {
            mc.getSoundManager().stop(s);
            be.sonido = null;
        }
        // notitas
        if (be.sonando && be.abierta > 0.8F && lv.getGameTime() % 7 == 0) {
            var p = be.getBlockPos();
            RandomSource r = lv.random;
            lv.addParticle(ParticleTypes.NOTE, p.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.5, p.getY() + 1.0 + r.nextDouble() * 0.2,
                    p.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.5, r.nextInt(25) / 24.0, 0, 0);
        }
    }

    /** El villancico saliendo de la caja: se calla solo si quitan la caja, la paran o cambian de canción. */
    static final class Sonido extends AbstractTickableSoundInstance {
        private final CajaMusicaBE be;
        final long desde;
        final int cancion;

        Sonido(CajaMusicaBE be) {
            super(CajaMusicaBE.sonidoDe(be.cancion), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.be = be;
            this.desde = be.desde;
            this.cancion = be.cancion;
            this.x = be.getBlockPos().getX() + 0.5;
            this.y = be.getBlockPos().getY() + 0.5;
            this.z = be.getBlockPos().getZ() + 0.5;
            this.volume = 1.4F;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.looping = false;
        }

        @Override
        public void tick() {
            if (be.isRemoved() || !be.sonando || be.desde != desde || be.cancion != cancion) {
                stop();
            }
        }
    }
}
