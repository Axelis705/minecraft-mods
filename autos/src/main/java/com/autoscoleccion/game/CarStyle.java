package com.autoscoleccion.game;

/**
 * Forma y colores de un cochecito, en "píxeles" (1/16 de bloque). El auto mira hacia +X; Z es el ancho e Y la altura. El cliente lo
 * dibuja con cajas y prismas (ver CarModel): carrocería baja, cabina con parabrisas inclinado, ruedas, luces y extras.
 * Se crea a partir de un {@link Shape} y se ajusta con los métodos encadenados.
 */
public final class CarStyle {
    /** Siluetas base. */
    public enum Shape {
        HATCH, SEDAN, PICKUP, VAN, CLASSIC, MINI, LOWRIDER, COUPE, MUSCLE, WEDGE, GT, HYPER, PROTO, FORMULA, RALLY
    }

    // extras (se combinan)
    public static final int SPOILER = 1;
    public static final int BIG_WING = 1 << 1;
    public static final int STRIPES = 1 << 2;
    public static final int SIDE_STRIPE = 1 << 3;
    public static final int NEON = 1 << 4;
    public static final int SIREN = 1 << 5;
    public static final int TAXI_SIGN = 1 << 6;
    public static final int ICE_CREAM = 1 << 7;
    public static final int HOOD_SCOOP = 1 << 8;
    public static final int EXHAUST = 1 << 9;
    public static final int ROOF_LIGHTS = 1 << 10;
    public static final int MUDFLAPS = 1 << 11;
    public static final int FIN = 1 << 12;
    public static final int CHROME = 1 << 13;
    public static final int BULLBAR = 1 << 14;
    public static final int ROOF_RACK = 1 << 15;
    public static final int GOLD_SPARKLE = 1 << 16;
    public static final int NUMBER_DISC = 1 << 17;
    public static final int TWO_TONE = 1 << 18;
    public static final int HELMET = 1 << 19;
    public static final int CANOPY = 1 << 20;
    public static final int SURFBOARD = 1 << 21;

    public final Shape shape;
    // medidas
    public float len = 14;
    public float wid = 6.4F;
    public float ride = 1.2F;
    public float bodyH = 2.6F;
    /** Cuánto se mete hacia atrás la parte de arriba del morro / hacia delante la de la cola (cuñas). */
    public float noseSlope = 1.5F;
    public float tailSlope = 0.8F;
    /** Posición X de la cabina (inicio delantero y final trasero, medidos desde el centro). */
    public float cabFront = 2.0F;
    public float cabRear = -4.0F;
    public float roofH = 2.2F;
    /** Inclinación del parabrisas y del vidrio trasero, y cuánto se estrecha la cabina arriba. */
    public float wsInset = 1.6F;
    public float rearInset = 1.0F;
    public float sideInset = 0.5F;
    public float wheelR = 1.35F;
    public float wheelW = 1.1F;
    public float wheelFront = 4.4F;
    public float wheelRear = -4.4F;
    /** Ancho de vía: cuánto salen las ruedas del costado (0 = al ras). */
    public float track = 0.0F;
    public int extras;
    // colores
    public int body = 0xD0D0D0;
    public int accent = 0x202020;
    public int glass = 0x2A3B52;
    public int rim = 0xC8CCD2;
    public int neon = 0x30F0FF;
    public int interior = 0x1C1C22;

    public CarStyle(Shape shape) {
        this.shape = shape;
        switch (shape) {
            case HATCH -> {
                len = 12.5F; cabFront = 1.8F; cabRear = -5.4F; roofH = 2.4F; tailSlope = 0.2F; rearInset = 0.4F; wheelFront = 3.9F; wheelRear = -3.9F;
            }
            case SEDAN -> {
                len = 14.5F; cabFront = 2.2F; cabRear = -3.6F; roofH = 2.2F; tailSlope = 0.5F; wheelFront = 4.6F; wheelRear = -4.4F;
            }
            case PICKUP -> {
                len = 15; wid = 6.8F; ride = 1.9F; bodyH = 2.8F; cabFront = 2.4F; cabRear = -1.6F; roofH = 2.4F; wsInset = 1.0F; rearInset = 0.2F;
                noseSlope = 0.6F; tailSlope = 0.0F; wheelR = 1.7F; wheelW = 1.3F; wheelFront = 4.9F; wheelRear = -4.6F;
            }
            case VAN -> {
                len = 13.5F; wid = 6.6F; bodyH = 2.8F; cabFront = 4.6F; cabRear = -6.6F; roofH = 3.4F; wsInset = 1.2F; rearInset = 0.1F; sideInset = 0.3F;
                noseSlope = 0.4F; tailSlope = 0.0F; wheelFront = 4.3F; wheelRear = -4.3F;
            }
            case CLASSIC -> {
                len = 12.5F; wid = 6.2F; bodyH = 2.6F; ride = 1.4F; cabFront = 1.2F; cabRear = -3.8F; roofH = 2.6F; wsInset = 1.0F; rearInset = 2.0F;
                sideInset = 0.8F; noseSlope = 2.2F; tailSlope = 2.0F; wheelFront = 3.9F; wheelRear = -3.8F;
            }
            case MINI -> {
                len = 9.5F; wid = 5.6F; bodyH = 2.4F; cabFront = 1.6F; cabRear = -4.4F; roofH = 2.6F; wsInset = 1.1F; rearInset = 0.2F;
                noseSlope = 0.8F; tailSlope = 0.0F; wheelR = 1.15F; wheelFront = 3.0F; wheelRear = -3.0F;
            }
            case LOWRIDER -> {
                len = 16.5F; wid = 6.6F; ride = 0.5F; bodyH = 2.3F; cabFront = 1.6F; cabRear = -3.8F; roofH = 2.0F; wsInset = 1.4F; rearInset = 1.8F;
                noseSlope = 0.8F; tailSlope = 0.6F; wheelR = 1.25F; wheelW = 0.9F; wheelFront = 5.2F; wheelRear = -5.0F;
            }
            case COUPE -> {
                len = 13.5F; ride = 0.9F; bodyH = 2.3F; cabFront = 1.4F; cabRear = -3.6F; roofH = 2.0F; wsInset = 2.0F; rearInset = 2.2F;
                noseSlope = 1.8F; tailSlope = 0.6F; wheelFront = 4.2F; wheelRear = -4.2F; track = 0.2F;
            }
            case MUSCLE -> {
                len = 15; wid = 6.8F; ride = 1.0F; bodyH = 2.7F; cabFront = 0.6F; cabRear = -4.2F; roofH = 2.0F; wsInset = 1.8F; rearInset = 2.0F;
                noseSlope = 0.6F; tailSlope = 0.3F; wheelR = 1.5F; wheelW = 1.4F; wheelFront = 4.8F; wheelRear = -4.6F; track = 0.2F;
            }
            case WEDGE -> {
                len = 14.5F; wid = 7.0F; ride = 0.7F; bodyH = 2.1F; cabFront = 1.2F; cabRear = -3.4F; roofH = 2.0F; wsInset = 2.8F; rearInset = 2.0F;
                sideInset = 0.9F; noseSlope = 3.6F; tailSlope = 0.8F; wheelR = 1.45F; wheelW = 1.4F; wheelFront = 4.6F; wheelRear = -4.4F; track = 0.25F;
            }
            case GT -> {
                len = 15.5F; wid = 6.8F; ride = 0.8F; bodyH = 2.3F; cabFront = -0.4F; cabRear = -4.6F; roofH = 2.1F; wsInset = 2.2F; rearInset = 2.4F;
                noseSlope = 2.4F; tailSlope = 1.0F; wheelR = 1.45F; wheelFront = 5.0F; wheelRear = -4.6F; track = 0.2F;
            }
            case HYPER -> {
                len = 15.5F; wid = 7.2F; ride = 0.6F; bodyH = 2.0F; cabFront = 1.4F; cabRear = -3.0F; roofH = 1.7F; wsInset = 2.6F; rearInset = 2.6F;
                sideInset = 1.1F; noseSlope = 3.8F; tailSlope = 0.4F; wheelR = 1.5F; wheelW = 1.5F; wheelFront = 4.9F; wheelRear = -4.7F; track = 0.3F;
            }
            case PROTO -> {
                len = 16.5F; wid = 7.2F; ride = 0.5F; bodyH = 1.9F; cabFront = 2.4F; cabRear = -1.8F; roofH = 1.8F; wsInset = 2.0F; rearInset = 1.6F;
                sideInset = 1.6F; noseSlope = 4.2F; tailSlope = 0.2F; wheelR = 1.5F; wheelW = 1.5F; wheelFront = 5.4F; wheelRear = -5.0F; track = 0.2F;
            }
            case FORMULA -> {
                len = 16; wid = 3.2F; ride = 0.7F; bodyH = 1.6F; cabFront = 1.0F; cabRear = -2.0F; roofH = 0.0F; noseSlope = 4.0F; tailSlope = 0.6F;
                wheelR = 1.6F; wheelW = 1.7F; wheelFront = 5.2F; wheelRear = -4.8F; track = 2.4F;
            }
            case RALLY -> {
                len = 13; ride = 1.6F; bodyH = 2.5F; cabFront = 1.6F; cabRear = -4.4F; roofH = 2.3F; wsInset = 1.6F; rearInset = 0.8F; noseSlope = 1.4F;
                tailSlope = 0.4F; wheelR = 1.5F; wheelW = 1.3F; wheelFront = 4.1F; wheelRear = -4.1F; track = 0.2F;
            }
        }
    }

    public static CarStyle of(Shape s) {
        return new CarStyle(s);
    }

    public CarStyle colors(int body, int accent) {
        this.body = body;
        this.accent = accent;
        return this;
    }

    public CarStyle glass(int c) {
        this.glass = c;
        return this;
    }

    public CarStyle rim(int c) {
        this.rim = c;
        return this;
    }

    public CarStyle neon(int c) {
        this.neon = c;
        this.extras |= NEON;
        return this;
    }

    public CarStyle with(int flags) {
        this.extras |= flags;
        return this;
    }

    public boolean has(int flag) {
        return (extras & flag) != 0;
    }

    public CarStyle size(float len, float wid) {
        this.len = len;
        this.wid = wid;
        return this;
    }
}
