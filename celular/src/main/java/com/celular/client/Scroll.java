package com.celular.client;

/** Desplazamiento suave de una lista (con la ruedita o arrastrando). */
final class Scroll {
    double target, pos;
    int max;

    void wheel(double d) {
        target -= d * 28;
        clamp();
    }

    void drag(double dy) {
        target -= dy;
        pos = target;
        clamp();
    }

    void update(int contentHeight, int viewHeight) {
        max = Math.max(0, contentHeight - viewHeight);
        clamp();
        pos += (target - pos) * 0.35;
        if (Math.abs(target - pos) < 0.5) {
            pos = target;
        }
    }

    void clamp() {
        target = Math.max(0, Math.min(max, target));
    }

    void reset() {
        target = pos = 0;
    }

    int off() {
        return (int) Math.round(pos);
    }
}
