package com.axeliscore.anim;

/**
 * Una entidad que se dibuja con el motor de animación del Core. Solo tiene que guardar un {@link AnimHost} y decir qué animar:
 * <pre>
 *     private final AnimHost anim = new AnimHost();
 *     public AnimHost axHost() { return anim; }
 *     public void axAnimate(AnimHost h, float partial) {
 *         h.play("base", isMoving ? "walk" : "idle", 0.25F);
 *     }
 * </pre>
 */
public interface Animated {
    AnimHost axHost();

    /** Cada cuadro, ANTES de calcular la postura: elegir qué animación suena en cada capa (play no reinicia si ya está). */
    default void axAnimate(AnimHost h, float partial) {
    }

    /** Cada cuadro, DESPUÉS de calcular la postura: retoques a mano (mirar a alguien, parpadear, respirar...). */
    default void axPose(AnimHost h, Pose pose, float partial) {
    }

    /** Un evento de la animación (los sonidos ya los toca el renderizador; esto es para "timeline" y partículas). */
    default void axEvent(AnimHost h, String layer, Clip clip, Clip.Event e) {
    }
}
