package com.kensei;

import net.minecraftforge.common.ForgeConfigSpec;

/** Todos los números del combate en un solo lugar (config/kensei-common.toml). Se pueden cambiar sin tocar el código. */
public final class KenseiConfig {
    public static final ForgeConfigSpec SPEC;

    // energía (cada barra vale 100)
    public static final ForgeConfigSpec.IntValue BARS;
    public static final ForgeConfigSpec.DoubleValue START_ENERGY;
    public static final ForgeConfigSpec.DoubleValue GAIN_ON_PARRY;
    public static final ForgeConfigSpec.DoubleValue GAIN_ON_PERFECT_DODGE;
    public static final ForgeConfigSpec.DoubleValue REST_REGEN_PER_SECOND;
    public static final ForgeConfigSpec.IntValue REST_DELAY;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_KATANA;

    // esquive
    public static final ForgeConfigSpec.DoubleValue DODGE_COST;
    public static final ForgeConfigSpec.DoubleValue DODGE_POWER;
    public static final ForgeConfigSpec.IntValue DODGE_IFRAMES;
    public static final ForgeConfigSpec.IntValue DODGE_COOLDOWN;

    // parry
    public static final ForgeConfigSpec.IntValue PARRY_WINDOW;
    public static final ForgeConfigSpec.DoubleValue BLOCK_REDUCTION;
    public static final ForgeConfigSpec.IntValue STUN_TICKS;
    public static final ForgeConfigSpec.IntValue COUNTER_WINDOW;
    public static final ForgeConfigSpec.DoubleValue COUNTER_MULTIPLIER;

    // combo y aire
    public static final ForgeConfigSpec.IntValue COMBO_WINDOW;
    public static final ForgeConfigSpec.DoubleValue FINISHER_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue LAUNCH_POWER;
    public static final ForgeConfigSpec.IntValue AIR_HITS_BEFORE_SLAM;
    public static final ForgeConfigSpec.DoubleValue SLAM_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SLAM_AOE_DAMAGE;

    // golpes normales (clic izquierdo)
    public static final ForgeConfigSpec.IntValue M1_INTERVAL;
    public static final ForgeConfigSpec.IntValue M1_RECOVERY;
    public static final ForgeConfigSpec.IntValue M1_HIT_DELAY;
    public static final ForgeConfigSpec.DoubleValue M1_RANGE;
    public static final ForgeConfigSpec.DoubleValue M1_ANGLE;
    public static final ForgeConfigSpec.IntValue M1_MAX_TARGETS;
    public static final ForgeConfigSpec.DoubleValue M1_DAMAGE_SCALE;
    public static final ForgeConfigSpec.DoubleValue M1_LUNGE;
    public static final ForgeConfigSpec.BooleanValue M1_HOLD_TO_CHAIN;

    // estocada corriendo
    public static final ForgeConfigSpec.DoubleValue THRUST_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue THRUST_RANGE;
    public static final ForgeConfigSpec.DoubleValue THRUST_ANGLE;
    public static final ForgeConfigSpec.IntValue THRUST_COOLDOWN;

    // ataque crítico (tecla R)
    public static final ForgeConfigSpec.IntValue HEAVY_WINDUP;
    public static final ForgeConfigSpec.IntValue HEAVY_RECOVERY;
    public static final ForgeConfigSpec.IntValue HEAVY_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue HEAVY_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue HEAVY_RANGE;
    public static final ForgeConfigSpec.DoubleValue HEAVY_ANGLE;
    public static final ForgeConfigSpec.IntValue HEAVY_MAX_TARGETS;
    public static final ForgeConfigSpec.DoubleValue HEAVY_KNOCKBACK;
    public static final ForgeConfigSpec.BooleanValue HEAVY_BREAKS_GUARD;

    // efectos
    public static final ForgeConfigSpec.BooleanValue CAMERA_SHAKE;
    public static final ForgeConfigSpec.BooleanValue BODY_LEAN;
    public static final ForgeConfigSpec.BooleanValue HIDE_VANILLA_HEALTH;
    public static final ForgeConfigSpec.BooleanValue VFX_ENABLED;
    public static final ForgeConfigSpec.BooleanValue DAMAGE_NUMBERS;
    public static final ForgeConfigSpec.BooleanValue SCREEN_FX;
    public static final ForgeConfigSpec.BooleanValue GHOSTS;

    // estamina
    public static final ForgeConfigSpec.BooleanValue STAMINA_ENABLED;
    public static final ForgeConfigSpec.DoubleValue STAMINA_MAX;
    public static final ForgeConfigSpec.DoubleValue STAMINA_REGEN;
    public static final ForgeConfigSpec.IntValue STAMINA_DELAY;
    public static final ForgeConfigSpec.DoubleValue STAMINA_GUARD_REGEN;
    public static final ForgeConfigSpec.DoubleValue COST_LIGHT;
    public static final ForgeConfigSpec.DoubleValue COST_FINISHER;
    public static final ForgeConfigSpec.DoubleValue COST_THRUST;
    public static final ForgeConfigSpec.DoubleValue COST_HEAVY;
    public static final ForgeConfigSpec.DoubleValue COST_WHIFF;
    public static final ForgeConfigSpec.DoubleValue COST_BLOCK_BASE;
    public static final ForgeConfigSpec.DoubleValue COST_BLOCK_PER_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue PARRY_REFUND;
    public static final ForgeConfigSpec.IntValue EXHAUST_TICKS;
    public static final ForgeConfigSpec.BooleanValue ATTACK_SLOWS;

    // rival de práctica
    public static final ForgeConfigSpec.DoubleValue RIVAL_LIGHT_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue RIVAL_HEAVY_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue RIVAL_AGGRESSION;
    public static final ForgeConfigSpec.BooleanValue RIVAL_DEFENDS;
    // duelos
    public static final ForgeConfigSpec.BooleanValue DUEL_ENABLED;
    public static final ForgeConfigSpec.IntValue DUEL_ROUNDS;
    public static final ForgeConfigSpec.IntValue DUEL_ROUND_SECONDS;
    public static final ForgeConfigSpec.DoubleValue DUEL_HEALTH;
    public static final ForgeConfigSpec.BooleanValue DUEL_REQUIRE_KATANA;
    public static final ForgeConfigSpec.BooleanValue DUEL_BLOCK_ITEMS;
    public static final ForgeConfigSpec.IntValue DUEL_CHALLENGE_SECONDS;
    public static final ForgeConfigSpec.BooleanValue DUEL_ANNOUNCE;
    public static final ForgeConfigSpec.BooleanValue DUEL_RESTORE_HEALTH;

    // cámara de combate (F5)
    public static final ForgeConfigSpec.BooleanValue CAMERA_ENABLED;
    public static final ForgeConfigSpec.DoubleValue CAMERA_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue CAMERA_SHOULDER;
    public static final ForgeConfigSpec.DoubleValue CAMERA_HEIGHT;
    public static final ForgeConfigSpec.BooleanValue CAMERA_DYNAMIC;

    // PvP
    public static final ForgeConfigSpec.DoubleValue PVP_BONUS_SCALE;
    public static final ForgeConfigSpec.BooleanValue PVP_LAUNCH;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("energia");
        BARS = b.comment("Cuántas barras de esquive hay (cada una vale 100 de energía).").defineInRange("barras", 3, 1, 6);
        START_ENERGY = b.comment("Energía con la que se empieza (y al reaparecer).").defineInRange("energiaInicial", 100.0, 0.0, 600.0);
        GAIN_ON_PARRY = b.comment("Energía que se gana con un parry perfecto.").defineInRange("ganaConParry", 35.0, 0.0, 100.0);
        GAIN_ON_PERFECT_DODGE = b.comment("Energía que se devuelve si un esquive evitó un golpe de verdad.").defineInRange("ganaConEsquivePerfecto", 25.0, 0.0, 100.0);
        REST_REGEN_PER_SECOND = b.comment("Energía que se recarga por segundo cuando no estás haciendo nada (cada dash vale 100). Se pausa mientras atacas, esquivas o te golpean.")
                .defineInRange("recargaEnReposoPorSegundo", 45.0, 0.0, 400.0);
        REST_DELAY = b.comment("Ticks sin atacar ni esquivar antes de que empiece la recarga (20 ticks = 1 segundo).").defineInRange("esperaParaRecargar", 20, 0, 200);
        REQUIRE_KATANA = b.comment("Si es true, el esquive y el parry solo funcionan con una katana en la mano.").define("requiereKatana", true);
        b.pop();

        b.push("esquive");
        DODGE_COST = b.comment("Energía que cuesta cada esquive.").defineInRange("costo", 100.0, 0.0, 600.0);
        DODGE_POWER = b.comment("Fuerza del impulso (bloques por tick aprox.).").defineInRange("fuerza", 1.05, 0.2, 3.0);
        DODGE_IFRAMES = b.comment("Ticks de invulnerabilidad durante el esquive (20 ticks = 1 segundo).").defineInRange("ticksInvulnerable", 8, 1, 40);
        DODGE_COOLDOWN = b.comment("Ticks de espera entre un esquive y otro.").defineInRange("espera", 10, 1, 100);
        b.pop();

        b.push("parry");
        PARRY_WINDOW = b.comment("Ticks desde que empiezas a bloquear en los que un golpe cuenta como parry perfecto.").defineInRange("ventana", 7, 1, 30);
        BLOCK_REDUCTION = b.comment("Cuánto daño se reduce al bloquear tarde (0.6 = 60 %).").defineInRange("reduccionBloqueo", 0.6, 0.0, 1.0);
        STUN_TICKS = b.comment("Ticks que queda aturdido quien fue parreado.").defineInRange("aturdimiento", 40, 0, 200);
        COUNTER_WINDOW = b.comment("Ticks que dura el contraataque disponible tras un parry o esquive perfecto.").defineInRange("ventanaContraataque", 40, 1, 200);
        COUNTER_MULTIPLIER = b.comment("Multiplicador de daño del contraataque.").defineInRange("multiplicadorContraataque", 1.75, 1.0, 10.0);
        b.pop();

        b.push("combo");
        COMBO_WINDOW = b.comment("Ticks máximos entre golpes para que el combo siga.").defineInRange("ventana", 22, 5, 100);
        FINISHER_MULTIPLIER = b.comment("Multiplicador de daño del golpe final (el cuarto).").defineInRange("multiplicadorFinal", 1.5, 1.0, 10.0);
        LAUNCH_POWER = b.comment("Fuerza con la que el golpe final levanta al enemigo.").defineInRange("fuerzaLanzamiento", 0.85, 0.1, 2.0);
        AIR_HITS_BEFORE_SLAM = b.comment("Golpes en el aire antes del golpe de remate hacia el suelo.").defineInRange("golpesAereos", 3, 1, 10);
        SLAM_MULTIPLIER = b.comment("Multiplicador de daño del remate.").defineInRange("multiplicadorRemate", 1.6, 1.0, 10.0);
        SLAM_AOE_DAMAGE = b.comment("Daño en área al aterrizar el remate (0 = sin daño en área).").defineInRange("danoAreaRemate", 4.0, 0.0, 40.0);
        b.pop();

        b.push("golpes");
        M1_INTERVAL = b.comment("Ticks entre un golpe normal y el siguiente (20 ticks = 1 segundo). Menos = más rápido.").defineInRange("espera", 8, 3, 40);
        M1_RECOVERY = b.comment("Ticks de espera tras el cuarto golpe (el final del combo).").defineInRange("esperaTrasFinal", 16, 3, 60);
        M1_HIT_DELAY = b.comment("Ticks desde que empieza el tajo hasta que el golpe conecta (para que coincida con la animación).").defineInRange("retrasoGolpe", 2, 0, 10);
        M1_RANGE = b.comment("Alcance del golpe en bloques.").defineInRange("alcance", 3.2, 1.5, 6.0);
        M1_ANGLE = b.comment("Mitad del ángulo del golpe en grados (55 = abanico de 110 grados delante de ti).").defineInRange("angulo", 55.0, 10.0, 180.0);
        M1_MAX_TARGETS = b.comment("Cuántos enemigos puede alcanzar un mismo golpe.").defineInRange("maximoObjetivos", 3, 1, 10);
        M1_DAMAGE_SCALE = b.comment("Multiplicador del daño de cada golpe normal (1 = el daño completo del arma).").defineInRange("multiplicadorDano", 0.8, 0.1, 5.0);
        M1_LUNGE = b.comment("Fuerza del paso adelante al golpear (0 = sin avance).").defineInRange("avance", 0.32, 0.0, 1.5);
        M1_HOLD_TO_CHAIN = b.comment("Si es true, mantener apretado el clic izquierdo repite los golpes del combo.").define("mantenerParaEncadenar", true);
        b.pop();

        b.push("estocada");
        THRUST_DAMAGE = b.comment("Multiplicador de daño de la estocada (clic izquierdo mientras corres).").defineInRange("multiplicadorDano", 1.3, 0.5, 10.0);
        THRUST_RANGE = b.comment("Alcance de la estocada en bloques.").defineInRange("alcance", 3.8, 1.5, 7.0);
        THRUST_ANGLE = b.comment("Mitad del ángulo de la estocada en grados (es más estrecha que un tajo).").defineInRange("angulo", 35.0, 5.0, 180.0);
        THRUST_COOLDOWN = b.comment("Ticks de espera tras una estocada.").defineInRange("espera", 14, 3, 80);
        b.pop();

        b.push("critico");
        HEAVY_WINDUP = b.comment("Ticks de preparación (el arma sube) antes de que caiga el golpe crítico.").defineInRange("carga", 10, 2, 40);
        HEAVY_RECOVERY = b.comment("Ticks que quedas quieto después del golpe crítico.").defineInRange("recuperacion", 10, 0, 60);
        HEAVY_COOLDOWN = b.comment("Ticks de espera para volver a usar el crítico.").defineInRange("espera", 50, 5, 400);
        HEAVY_DAMAGE = b.comment("Multiplicador de daño del crítico.").defineInRange("multiplicadorDano", 2.2, 0.5, 10.0);
        HEAVY_RANGE = b.comment("Alcance del crítico en bloques.").defineInRange("alcance", 3.8, 1.5, 7.0);
        HEAVY_ANGLE = b.comment("Mitad del ángulo del crítico en grados.").defineInRange("angulo", 65.0, 10.0, 180.0);
        HEAVY_MAX_TARGETS = b.comment("Cuántos enemigos puede alcanzar el crítico.").defineInRange("maximoObjetivos", 5, 1, 10);
        HEAVY_KNOCKBACK = b.comment("Fuerza con la que el crítico empuja al enemigo.").defineInRange("empuje", 1.0, 0.0, 4.0);
        HEAVY_BREAKS_GUARD = b.comment("Si es true, el crítico rompe la guardia (katana o escudo) y no se puede parrear: solo esquivar.").define("rompeGuardia", true);
        b.pop();

        b.push("efectos");
        CAMERA_SHAKE = b.comment("Sacudida de cámara al golpear y al recibir golpes (cada jugador la puede apagar en su config).").define("sacudidaCamara", true);
        BODY_LEAN = b.comment("Inclinación del cuerpo al esquivar y al hacer el crítico (se ve en tercera persona).").define("inclinacionCuerpo", true);
        HIDE_VANILLA_HEALTH = b.comment("Si es true, con la katana en la mano se oculta la fila de corazones del juego (la vida se ve en el panel de arriba a la izquierda).").define("ocultarCorazones", true);
        VFX_ENABLED = b.comment("Efectos visuales del combate: círculos mágicos, ondas, tajos de luz y estrellas al golpear.").define("efectosVisuales", true);
        DAMAGE_NUMBERS = b.comment("Números de daño flotantes y el nombre del movimiento en pantalla.").define("numerosDeDanio", true);
        SCREEN_FX = b.comment("Líneas de velocidad y destellos de pantalla (esquive, estocada, crítico y golpes fuertes).").define("efectosDePantalla", true);
        GHOSTS = b.comment("Imágenes residuales (siluetas brillantes) que deja el jugador al esquivar y al hacer la estocada.").define("imagenesResiduales", true);
        b.pop();

        b.push("camara");
        CAMERA_ENABLED = b.comment("Cámara de combate en tercera persona (F5, vista de espaldas) mientras llevas la katana.").define("activada", true);
        CAMERA_DISTANCE = b.comment("Distancia de la cámara al jugador (el juego usa 4). Menos = más cerca.").defineInRange("distancia", 3.0, 1.5, 4.0);
        CAMERA_SHOULDER = b.comment("Cuánto se corre la cámara hacia el hombro derecho (el jugador queda a la izquierda de la pantalla).").defineInRange("hombro", 0.55, 0.0, 1.5);
        CAMERA_HEIGHT = b.comment("Cuánto sube la cámara.").defineInRange("altura", 0.15, -0.5, 1.0);
        CAMERA_DYNAMIC = b.comment("Zoom y campo de visión dinámicos: se abre al esquivar, al correr y al hacer el crítico.").define("dinamica", true);
        b.pop();

        b.push("estamina");
        STAMINA_ENABLED = b.comment("Si es true, atacar y bloquear gasta estamina (no se pueden dar golpes sin parar).").define("activada", true);
        STAMINA_MAX = b.comment("Estamina máxima.").defineInRange("maxima", 100.0, 20.0, 1000.0);
        STAMINA_REGEN = b.comment("Estamina que se recupera por segundo cuando no atacas.").defineInRange("recargaPorSegundo", 32.0, 1.0, 500.0);
        STAMINA_DELAY = b.comment("Ticks sin gastar estamina antes de que empiece a recargarse (20 ticks = 1 segundo).").defineInRange("esperaParaRecargar", 18, 0, 200);
        STAMINA_GUARD_REGEN = b.comment("Qué parte de la recarga se mantiene mientras bloqueas (0.35 = 35 %).").defineInRange("recargaBloqueando", 0.35, 0.0, 1.0);
        COST_LIGHT = b.comment("Estamina que cuesta cada tajo normal.").defineInRange("costoTajo", 10.0, 0.0, 200.0);
        COST_FINISHER = b.comment("Estamina que cuesta el golpe final del combo.").defineInRange("costoFinal", 16.0, 0.0, 200.0);
        COST_THRUST = b.comment("Estamina que cuesta la estocada corriendo.").defineInRange("costoEstocada", 14.0, 0.0, 200.0);
        COST_HEAVY = b.comment("Estamina que cuesta el crítico.").defineInRange("costoCritico", 30.0, 0.0, 200.0);
        COST_WHIFF = b.comment("Estamina extra que se pierde si el golpe no toca a nadie (no golpees al aire).").defineInRange("costoFallar", 6.0, 0.0, 100.0);
        COST_BLOCK_BASE = b.comment("Estamina que cuesta bloquear un golpe (más una parte según el daño).").defineInRange("costoBloqueo", 8.0, 0.0, 200.0);
        COST_BLOCK_PER_DAMAGE = b.comment("Estamina extra por cada punto de daño bloqueado.").defineInRange("costoBloqueoPorDano", 1.6, 0.0, 20.0);
        PARRY_REFUND = b.comment("Estamina que se devuelve con un parry perfecto.").defineInRange("devuelveParry", 25.0, 0.0, 200.0);
        EXHAUST_TICKS = b.comment("Ticks de agotamiento (sin recargar ni atacar) al quedarte sin estamina.").defineInRange("agotamiento", 28, 0, 200);
        ATTACK_SLOWS = b.comment("Si es true, atacar te frena un poco (hay que decidir cuándo pegar).").define("atacarFrena", true);
        b.pop();

        b.push("rival");
        RIVAL_LIGHT_DAMAGE = b.comment("Daño de cada tajo normal del rival de práctica.").defineInRange("danoTajo", 4.0, 0.5, 40.0);
        RIVAL_HEAVY_DAMAGE = b.comment("Daño del golpe crítico del rival (rompe la guardia: solo se puede esquivar).").defineInRange("danoCritico", 9.0, 0.5, 60.0);
        RIVAL_AGGRESSION = b.comment("Qué tan seguido ataca el rival. 1 = normal, 2 = el doble de seguido, 0.5 = la mitad.").defineInRange("agresividad", 1.0, 0.2, 4.0);
        RIVAL_DEFENDS = b.comment("Si es true, el rival también se cubre y esquiva cuando le vas a pegar.").define("seDefiende", true);
        b.pop();

        b.push("duelo");
        DUEL_ENABLED = b.comment("Si es false, nadie puede iniciar duelos.").define("activado", true);
        DUEL_ROUNDS = b.comment("Cuántas rondas como máximo tiene un duelo (al mejor de: 3 = gana quien llegue a 2).").defineInRange("rondas", 3, 1, 9);
        DUEL_ROUND_SECONDS = b.comment("Segundos que dura cada ronda. Si se acaba el tiempo gana quien tenga más vida.").defineInRange("segundosPorRonda", 99, 10, 600);
        DUEL_HEALTH = b.comment("Vida máxima de cada peleador durante el duelo (el jugador normal tiene 20). Más vida = rondas más largas.").defineInRange("vida", 40.0, 10.0, 200.0);
        DUEL_REQUIRE_KATANA = b.comment("Si es true, todos los peleadores deben tener una katana en el inventario.").define("requiereKatana", true);
        DUEL_BLOCK_ITEMS = b.comment("Si es true, durante el duelo no se pueden usar perlas, pociones, comida, manzanas doradas ni frutas de chorus.").define("bloquearObjetos", true);
        DUEL_CHALLENGE_SECONDS = b.comment("Segundos que tiene el desafiado para aceptar.").defineInRange("segundosParaAceptar", 30, 5, 120);
        DUEL_ANNOUNCE = b.comment("Si es true, al empezar un duelo se avisa a todos en el chat con un botón para verlo.").define("anunciar", true);
        DUEL_RESTORE_HEALTH = b.comment("Si es true, al terminar el duelo cada peleador vuelve con la vida y el hambre que tenía antes.").define("restaurarVida", true);
        b.pop();

        b.push("pvp");
        PVP_BONUS_SCALE = b.comment("Qué parte de los bonos de daño (combo, contraataque, remate) se aplica contra jugadores. 1 = completa, 0 = ninguna.")
                .defineInRange("escalaBonos", 0.6, 0.0, 1.0);
        PVP_LAUNCH = b.comment("Si es false, el golpe final no levanta a otros jugadores.").define("lanzarJugadores", true);
        b.pop();

        SPEC = b.build();
    }

    private KenseiConfig() {
    }

    public static float staminaMax() {
        return (float) (double) STAMINA_MAX.get();
    }

    public static float maxEnergy() {
        return BARS.get() * 100.0F;
    }
}
