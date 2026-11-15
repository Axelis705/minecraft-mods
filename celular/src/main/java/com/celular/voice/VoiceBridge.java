package com.celular.voice;

/** Lo único del chat de voz que ve el resto del mod (así nada más depende de Simple Voice Chat). */
public final class VoiceBridge {
    /** ¿Simple Voice Chat cargó nuestro complemento? (en el servidor) */
    public static volatile boolean available;

    private VoiceBridge() {
    }
}
