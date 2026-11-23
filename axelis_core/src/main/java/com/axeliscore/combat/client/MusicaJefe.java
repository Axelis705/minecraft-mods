package com.axeliscore.combat.client;

import com.axeliscore.combat.Jefe;
import com.axeliscore.plat.Ids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * (Cliente) La música de los jefes: suena la de la fase del jefe más cercano (hasta 48 bloques), entra y sale suave, cambia al cambiar
 * de fase y calla la música normal de Minecraft mientras tanto. Se oye con el volumen de "Música" del jugador.
 */
public final class MusicaJefe {
    private static final class Bucle extends AbstractTickableSoundInstance {
        private float objetivo = 0.9F;
        private boolean apagando;

        Bucle(String id) {
            super(SoundEvent.createVariableRangeEvent(Ids.parse(id)), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.02F;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            volume += (objetivo - volume) * (apagando ? 0.06F : 0.035F);
            if (apagando && volume < 0.03F) {
                stop();
            }
        }

        void apagar() {
            apagando = true;
            objetivo = 0F;
        }
    }

    private static Bucle sonando;
    private static String id = "";
    private static long ultimoTick = Long.MIN_VALUE;

    private MusicaJefe() {
    }

    public static void init() {
        com.axeliscore.fx.client.Hud.add((g, w, h) -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            cambiar("");
            return;
        }
        long t = mc.level.getGameTime();
        if (t == ultimoTick) {
            return;
        }
        ultimoTick = t;
        String quiero = "";
        double mejor = 48 * 48;
        for (Jefe j : mc.level.getEntitiesOfClass(Jefe.class, mc.player.getBoundingBox().inflate(48))) {
            if (!j.isAlive() || j.showcasing()) {
                continue;
            }
            double d = j.distanceToSqr(mc.player);
            String m = j.musica();
            if (d < mejor && !m.isEmpty()) {
                mejor = d;
                quiero = m;
            }
        }
        cambiar(quiero);
        if (sonando != null && t % 60 == 0) {
            mc.getMusicManager().stopPlaying();
        }
    }

    private static void cambiar(String quiero) {
        if (quiero.equals(id)) {
            return;
        }
        if (sonando != null) {
            sonando.apagar();
            sonando = null;
        }
        id = quiero;
        if (!quiero.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            sonando = new Bucle(quiero);
            mc.getSoundManager().play(sonando);
            mc.getMusicManager().stopPlaying();
        }
    }

    /** Para las pruebas: qué música está sonando ("" = ninguna). */
    public static String actual() {
        return id;
    }
}
