package com.casino.entity;

import com.casino.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

/**
 * Las animaciones de los crupieres (las decide el servidor y se ven igual para todos). Cada una dura unos ticks; el cliente las
 * dibuja en {@code client/DealerAnimator}. Sin librerías extra: se mueven los brazos y la cabeza del modelo de jugador.
 */
public final class DealerAnim {
    public static final int NONE = 0;
    /** Reparte una carta (brazo derecho de lado a lado). */
    public static final int DEAL = 1;
    /** Baraja con las dos manos. */
    public static final int SHUFFLE = 2;
    /** Voltea una carta. */
    public static final int FLIP = 3;
    /** Acomoda fichas con la mano izquierda. */
    public static final int CHIP = 4;
    /** Lanza la bola de la ruleta (brazo en arco). */
    public static final int SPIN = 5;
    /** Recoge las fichas perdedoras (dos brazos hacia el centro). */
    public static final int COLLECT = 6;
    /** Paga: empuja las fichas hacia el jugador. */
    public static final int PAY = 7;
    /** Aplaude. */
    public static final int CLAP = 8;
    /** Saluda con la mano. */
    public static final int WAVE = 9;
    /** Niega con la cabeza. */
    public static final int SHAKE = 10;
    /** Asiente con la cabeza. */
    public static final int NOD = 11;
    /** Sirve un trago (botella en alto, vaso en la otra mano). */
    public static final int POUR = 12;
    /** "No va más": mano al frente. */
    public static final int STOP = 13;
    /** Jala la ruleta de la suerte (brazo arriba y hacia abajo con fuerza). */
    public static final int PULL = 14;
    /** Presenta con los brazos abiertos ("¡adelante!"). */
    public static final int PRESENT = 15;
    /** Festeja con los brazos arriba. */
    public static final int CHEER = 16;
    /** Bartender: saca la botella (de abajo de la barra o de la repisa de atrás), toma un vaso, sirve y te lo da. */
    public static final int SERVE = 17;
    /** Bartender: agita la coctelera arriba del hombro. */
    public static final int SHAKER = 18;
    /** Guardia: se toca el auricular y habla por él. */
    public static final int EARPIECE = 19;
    /** Cliente: toma un trago de su copa. */
    public static final int SIP = 20;
    /** Pelea de cantina: golpe con la derecha y con la izquierda. */
    public static final int PUNCH = 21;
    public static final int PUNCH_L = 22;
    /** Momento del servicio en que el vaso llega a la mano del jugador (ahí se da el efecto). */
    public static final int SERVE_HANDOVER = 56;

    private static final int[] TICKS = {0, 18, 36, 14, 16, 26, 24, 20, 26, 30, 20, 16, 40, 22, 34, 30, 32, 66, 36, 40, 30, 8, 8};

    private DealerAnim() {
    }

    public static int duration(int anim) {
        return anim > 0 && anim < TICKS.length ? TICKS[anim] : 0;
    }

    /** Que todos los crupieres de la lista hagan esa animación. */
    public static void play(List<DealerEntity> dealers, int anim) {
        for (DealerEntity d : dealers) {
            d.anim(anim);
        }
    }

    /** La animación que va con un sonido de la mesa (cartas, fichas, monedas...), o NONE. */
    public static int forSound(SoundEvent s) {
        if (s == ModSounds.CARD_DEAL.get()) {
            return DEAL;
        }
        if (s == ModSounds.CARD_FLIP.get()) {
            return FLIP;
        }
        if (s == ModSounds.CHIP.get()) {
            return CHIP;
        }
        if (s == ModSounds.COINS.get()) {
            return PAY;
        }
        if (s == ModSounds.WHEEL_WIN.get()) {
            return CLAP;
        }
        if (s == ModSounds.ROULETTE_BELL.get()) {
            return NONE; // el lanzamiento de la bola lo sigue el cliente con la hora de la mesa (DealerMotion)
        }
        if (s == ModSounds.DENIED.get()) {
            return SHAKE;
        }
        return NONE;
    }

    /** Con el sonido de la mesa, el crupier hace la animación que le toca (si no está haciendo una más importante). */
    public static void onSound(List<DealerEntity> dealers, SoundEvent s) {
        int a = forSound(s);
        if (a == NONE) {
            return;
        }
        for (DealerEntity d : dealers) {
            // las fichas son muy seguidas: no cortan un reparto ni un pago a la mitad
            if (a == CHIP && d.animBusy()) {
                continue;
            }
            d.anim(a);
        }
    }
}
