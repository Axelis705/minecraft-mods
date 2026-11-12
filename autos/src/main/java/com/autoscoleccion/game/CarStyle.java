package com.autoscoleccion.game;

/**
 * Forma y colores de un cochecito, en "píxeles" (1/16 de bloque). El auto mira hacia +X; Z es el ancho e Y la altura. El cliente lo
 * dibuja con cajas y prismas (ver CarModel): carrocería baja, cabina con parabrisas inclinado, ruedas, luces y extras.
 * Se crea a partir de un {@link Shape} y se ajusta con los métodos encadenados.
 */
public final class CarStyle {
    /** Siluetas base. */
    public enum Shape {
        HATCH, SEDAN, PICKUP, VAN, CLASSIC, MINI, LOWRIDER, COUPE, MUSCLE, WEDGE, GT, HYPER, PROTO, FORMULA, RALLY, SUV, WAGON, ROADSTER, BOXVAN,
        // exclusivos
        HOTROD, MONSTER, DRAGSTER, BUGGY, KART, ROCKET, ROVER, HOVER, SHARK
    }

    // estilos de faros delanteros
    public static final int HEAD_ROUND = 0, HEAD_QUAD = 1, HEAD_SLIM = 2, HEAD_RECT = 3, HEAD_POPUP = 4, HEAD_BAR = 5, HEAD_ANGULAR = 6;
    // estilos de calaveras
    public static final int TAIL_BAR = 0, TAIL_ROUND2 = 1, TAIL_RECT = 2, TAIL_SLIM = 3, TAIL_ROUND1 = 4, TAIL_VERT = 5, TAIL_C = 6;
    // estilos de rin
    public static final int RIM_TWIN5 = 0, RIM_MULTI = 1, RIM_MESH = 2, RIM_DISH = 3, RIM_HUB = 4, RIM_WIRE = 5, RIM_Y = 6, RIM_TURBINE = 7,
            RIM_STEEL = 8, RIM_SIX = 9, RIM_SEVEN = 10;

    // detalles (se combinan, en {@link #detail})
    public static final int D_SIDE_INTAKE = 1;
    public static final int D_HOOD_VENTS = 1 << 1;
    public static final int D_LOUVERS = 1 << 2;
    public static final int D_DIFFUSER = 1 << 3;
    public static final int D_SPLITTER = 1 << 4;
    public static final int D_SKIRTS = 1 << 5;
    public static final int D_ROOF_SCOOP = 1 << 6;
    public static final int D_BIG_GRILLE = 1 << 7;
    public static final int D_OVAL_GRILLE = 1 << 8;
    public static final int D_SNORKEL = 1 << 9;
    public static final int D_SPARE = 1 << 10;
    public static final int D_ENGINE_GLASS = 1 << 11;
    public static final int D_C_LINE = 1 << 12;
    public static final int D_FOG = 1 << 13;
    public static final int D_WIPERS = 1 << 14;
    public static final int D_RAILS = 1 << 15;
    public static final int D_QUAD_EXH = 1 << 16;
    public static final int D_CENTER_EXH = 1 << 17;
    public static final int D_SIDE_EXH = 1 << 18;
    public static final int D_HOOD_LINES = 1 << 19;
    public static final int D_LIGHT_BAR = 1 << 20;
    public static final int D_GUARDS = 1 << 21;
    public static final int D_RACE_DECALS = 1 << 22;
    public static final int D_CHROME_TRIM = 1 << 23;
    public static final int D_ENGINE_OUT = 1 << 24;
    public static final int D_FLAMES = 1 << 25;
    public static final int D_CAGE = 1 << 26;
    public static final int D_HOVER = 1 << 27;
    public static final int D_JET = 1 << 28;
    public static final int D_SHARK = 1 << 29;
    public static final int D_ARMOR = 1 << 30;
    public static final int D_DISH = 1 << 31;

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
    public int detail;
    /** Redondez general (1 = normal, menos = más cuadrado, más = más curvo). */
    public float round = 1.0F;
    /** Cintura de avispa: cuánto se angosta en las puertas. */
    public float waist = 0.0F;
    /** La línea de cintura sube hacia atrás (cuña) o baja (negativo). */
    public float beltRise = 0.0F;
    /** Colita levantada al final de la cajuela. */
    public float ducktail = 0.0F;
    /** Guardabarros delantero y trasero (-1 = el de la silueta). */
    public float flareF = -1.0F;
    public float flareR = -1.0F;
    /** Curva del techo (-1 = la normal). */
    public float crown = -1.0F;
    /** Cuánto se mete la cabina respecto al costado. */
    public float cabInset = 0.42F;
    /** Parte de abajo de las ventanas que es carrocería (ventanas más bajitas). */
    public float glassLow = 0.0F;
    /** Jorobas de los guardabarros sobre las ruedas (-1 = lo justo para cubrir la rueda). */
    public float humpF = -1.0F;
    public float humpR = -1.0F;
    /** Cuánto sube el centro del cofre respecto a su orilla. */
    public float dome = 0.08F;
    /** Ancho de la punta del morro y de la cola respecto al ancho del auto (-1 = el de la silueta) y largo de ese afilado. */
    public float noseW = -1.0F;
    public float tailW = -1.0F;
    public float noseLen = 3.2F;
    public float tailLen = 2.6F;
    /** La cabina se angosta hacia atrás (forma de gota). */
    public float cabTaper = 0.0F;
    /** Tamaño del rin respecto a la llanta (-1 = el de la silueta). */
    public float rimFrac = -1.0F;
    /** Radio y ancho de las ruedas delanteras si son distintas (-1 = iguales a las traseras). */
    public float wheelRF = -1.0F;
    public float wheelWF = -1.0F;
    /** Un tercer eje en medio (NaN = no hay). */
    public float axleMid = Float.NaN;
    /** Ruedas al aire, por fuera de la carrocería (sin pasos de rueda). */
    public boolean openWheels;
    /** El piso sube atrás (difusor). */
    public float tailLift = 0.0F;
    /** Pintura que cambia de color con el tiempo. */
    public boolean chroma;
    /** Color de la parte baja de la carrocería (-1 = igual). */
    public int belly = -1;
    /** Perfil hecho a mano: línea de arriba de la carrocería (cofre, cintura, cajuela) en pares x, y (de atrás hacia adelante). */
    public float[] sideTop;
    /** Línea de abajo (piso) en pares x, y. */
    public float[] sideBot;
    /** Planta hecha a mano: pares x, medio ancho. */
    public float[] plan;
    /** Techo hecho a mano: pares x, y desde la base del vidrio de atrás hasta la base del parabrisas. */
    public float[] roofLine;
    /** Índices (de punto) donde empieza y termina el techo en {@link #roofLine}. */
    public int roofA = 1;
    public int roofB = 2;
    public int head = -1;
    public int tail = -1;
    public int rimStyle = -1;
    /** Color de los calipers (-1 = el automático). */
    public int caliper = -1;
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
            case SUV -> {
                len = 14; wid = 7.0F; ride = 1.9F; bodyH = 3.0F; cabFront = 2.6F; cabRear = -6.2F; roofH = 3.0F; wsInset = 0.9F; rearInset = 0.2F;
                sideInset = 0.15F; noseSlope = 0.4F; tailSlope = 0.0F; wheelR = 1.7F; wheelW = 1.4F; wheelFront = 4.4F; wheelRear = -4.3F; track = 0.25F;
                round = 0.35F;
            }
            case WAGON -> {
                len = 15; wid = 6.6F; ride = 1.1F; bodyH = 2.5F; cabFront = 2.4F; cabRear = -6.6F; roofH = 2.2F; wsInset = 1.6F; rearInset = 0.5F;
                noseSlope = 1.4F; tailSlope = 0.2F; wheelFront = 4.8F; wheelRear = -4.6F;
            }
            case ROADSTER -> {
                len = 12.5F; wid = 6.4F; ride = 0.9F; bodyH = 2.4F; cabFront = 0.8F; cabRear = -2.6F; roofH = 1.6F; wsInset = 1.4F; rearInset = 0.0F;
                noseSlope = 2.0F; tailSlope = 0.8F; wheelFront = 4.0F; wheelRear = -3.9F; track = 0.15F;
            }
            case BOXVAN -> {
                len = 14.5F; wid = 7.0F; ride = 1.5F; bodyH = 2.6F; cabFront = 4.4F; cabRear = -7.1F; roofH = 4.2F; wsInset = 0.8F; rearInset = 0.0F;
                sideInset = 0.0F; noseSlope = 0.8F; tailSlope = 0.0F; wheelR = 1.5F; wheelFront = 4.6F; wheelRear = -4.4F; round = 0.3F;
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

    /** Largo, ancho, altura del piso y alto de la carrocería. */
    public CarStyle dims(float len, float wid, float ride, float bodyH) {
        this.len = len;
        this.wid = wid;
        this.ride = ride;
        this.bodyH = bodyH;
        return this;
    }

    /** Cabina: dónde empieza el parabrisas, dónde termina atrás y altura del techo. */
    public CarStyle cab(float front, float rear, float roofH) {
        this.cabFront = front;
        this.cabRear = rear;
        this.roofH = roofH;
        return this;
    }

    /** Inclinación del parabrisas, del vidrio trasero y estrechez de la cabina arriba. */
    public CarStyle glassSlope(float ws, float rear, float side) {
        this.wsInset = ws;
        this.rearInset = rear;
        this.sideInset = side;
        return this;
    }

    /** Caída del cofre (morro) y de la cajuela. */
    public CarStyle slopes(float nose, float tail) {
        this.noseSlope = nose;
        this.tailSlope = tail;
        return this;
    }

    public CarStyle wheels(float r, float w, float front, float rear, float track) {
        this.wheelR = r;
        this.wheelW = w;
        this.wheelFront = front;
        this.wheelRear = rear;
        this.track = track;
        return this;
    }

    public CarStyle form(float round, float waist, float beltRise, float ducktail) {
        this.round = round;
        this.waist = waist;
        this.beltRise = beltRise;
        this.ducktail = ducktail;
        return this;
    }

    /** Línea de arriba de la carrocería, pares x, y de atrás hacia adelante. */
    public CarStyle top(float... xy) {
        this.sideTop = xy;
        return this;
    }

    public CarStyle bottom(float... xy) {
        this.sideBot = xy;
        return this;
    }

    /** Medio ancho a lo largo del auto, pares x, w de atrás hacia adelante. */
    public CarStyle plan(float... xw) {
        this.plan = xw;
        return this;
    }

    /** Techo: pares x, y desde la base trasera hasta la base del parabrisas; a y b marcan dónde empieza y termina el techo. */
    public CarStyle roofLine(int a, int b, float... xy) {
        this.roofLine = xy;
        this.roofA = a;
        this.roofB = b;
        this.cabRear = xy[0];
        this.cabFront = xy[xy.length - 2];
        float hi = 0;
        for (int i = 1; i < xy.length; i += 2) {
            hi = Math.max(hi, xy[i]);
        }
        this.roofH = Math.max(0.5F, hi - ride - bodyH);
        return this;
    }

    public CarStyle humps(float front, float rear) {
        this.humpF = front;
        this.humpR = rear;
        return this;
    }

    /** Punta del morro y de la cola: ancho (fracción) y largo del afilado. */
    public CarStyle taper(float noseW, float noseLen, float tailW, float tailLen) {
        this.noseW = noseW;
        this.noseLen = noseLen;
        this.tailW = tailW;
        this.tailLen = tailLen;
        return this;
    }

    public CarStyle teardrop(float cabTaper) {
        this.cabTaper = cabTaper;
        return this;
    }

    public CarStyle dome(float d) {
        this.dome = d;
        return this;
    }

    public CarStyle frontWheels(float r, float w) {
        this.wheelRF = r;
        this.wheelWF = w;
        return this;
    }

    public CarStyle open() {
        this.openWheels = true;
        return this;
    }

    public CarStyle tailLift(float v) {
        this.tailLift = v;
        return this;
    }

    public CarStyle rimFrac(float f) {
        this.rimFrac = f;
        return this;
    }

    public CarStyle belly(int c) {
        this.belly = c;
        return this;
    }

    public CarStyle chroma() {
        this.chroma = true;
        return this;
    }

    public CarStyle flares(float front, float rear) {
        this.flareF = front;
        this.flareR = rear;
        return this;
    }

    public CarStyle roof(float crown, float cabInset, float glassLow) {
        this.crown = crown;
        this.cabInset = cabInset;
        this.glassLow = glassLow;
        return this;
    }

    public CarStyle lights(int head, int tail) {
        this.head = head;
        this.tail = tail;
        return this;
    }

    public CarStyle rims(int style, int color) {
        this.rimStyle = style;
        this.rim = color;
        return this;
    }

    public CarStyle details(int flags) {
        this.detail |= flags;
        return this;
    }

    public CarStyle caliper(int c) {
        this.caliper = c;
        return this;
    }

    public boolean hasD(int flag) {
        return (detail & flag) != 0;
    }

    public CarStyle size(float len, float wid) {
        this.len = len;
        this.wid = wid;
        return this;
    }
}
