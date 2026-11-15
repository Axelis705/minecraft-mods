package com.celular.client;

import com.celular.plat.client.G;

/** La app de la cámara: al abrirla se pasa a la pantalla de la cámara ({@link CameraScreen}). */
final class CameraApp extends App {
    CameraApp(PhoneScreen s) {
        super(s, D.APP_CAMERA, "camera");
    }

    @Override
    int bg() {
        return 0xFF000000;
    }

    @Override
    void draw(G g, int mx, int my, float pt) {
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, 0xFF000000);
    }
}
