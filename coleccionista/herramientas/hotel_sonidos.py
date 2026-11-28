#!/usr/bin/env python3
"""Los sonidos del hotel (capítulo 1), hechos a mano (sin descargar nada):

- timbre.ogg     el timbre de recepción del Botones: ¡ding! metálico y brillante, con su eco por el pasillo
- carrito.ogg    las ruedas del carrito que rechinan (dos chillidos chuecos y el traqueteo)
- cuerda.ogg     la cuerda de la rata de juguete: el trinquete que hace clic-clic-clic
- chillido.ogg   el chillido de la rata (metálico, de juguete)
- rechinido.ogg  la puerta de servicio que se abre despacio: bisagra oxidada que rechina a tirones
- tobogan.ogg    la caída por el tubo de colores: el viento que zumba, el plástico que retumba y un "¡boing!" al final

Uso: python3 herramientas/hotel_sonidos.py  (necesita ffmpeg con libvorbis)
"""
import os
import sys

import numpy as np

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
from caja_sonidos import SR, t_, lp, hp, bp, eco, normal  # noqa: E402
import sorpresa_sonidos  # noqa: E402

sorpresa_sonidos.SND = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista', 'sounds', 'hotel')
guardar = sorpresa_sonidos.guardar
R = np.random.RandomState(9090)


def timbre():
    tt = t_(2.2)
    x = np.zeros_like(tt)
    # una campanita: parciales inarmónicos que se apagan a distinto ritmo
    for f, a, d in ((2093, 1.0, 2.2), (2093 * 2.76, 0.5, 4.5), (2093 * 5.4, 0.25, 8), (2093 * 0.5, 0.15, 3)):
        x += a * np.sin(2 * np.pi * f * tt + R.rand()) * np.exp(-tt * d)
    golpe = hp(R.randn(len(tt)), 3000) * np.exp(-tt * 120) * 0.6
    return normal(eco(x + golpe, 1.4, 0.4, 5000))


def carrito():
    tt = t_(1.2)
    x = np.zeros_like(tt)
    for t0, f0, dur in ((0.05, 1450, 0.28), (0.55, 1180, 0.35)):
        m = (tt >= t0) & (tt < t0 + dur)
        k = (tt[m] - t0) / dur
        f = f0 * (1 + 0.18 * np.sin(np.pi * k) + 0.03 * np.sin(2 * np.pi * 37 * tt[m]))
        fase = 2 * np.pi * np.cumsum(f) / SR
        x[m] += (np.sin(fase) + 0.4 * np.sign(np.sin(fase * 2))) * np.sin(np.pi * k) * 0.5
    # el traqueteo de las ruedas en el piso
    for t0 in np.arange(0.0, 1.2, 0.09):
        i = int(t0 * SR)
        n = int(0.02 * SR)
        x[i:i + n] += lp(R.randn(n), 900) * np.exp(-np.arange(n) / (0.004 * SR)) * 0.5
    return normal(bp(x, 300, 7000), 0.8)


def cuerda():
    tt = t_(1.0)
    x = np.zeros_like(tt)
    for k, t0 in enumerate(np.arange(0.02, 0.95, 0.075)):
        i = int(t0 * SR)
        n = int(0.012 * SR)
        clic = hp(R.randn(n), 2500) * np.exp(-np.arange(n) / (0.0015 * SR))
        x[i:i + n] += clic * (0.8 if k % 2 == 0 else 0.5)
        # el resorte que zumbita
        m = int(0.03 * SR)
        x[i:i + m] += np.sin(2 * np.pi * 3200 * np.arange(m) / SR) * np.exp(-np.arange(m) / (0.006 * SR)) * 0.25
    return normal(x, 0.85)


def chillido():
    tt = t_(0.45)
    f = 2600 + 900 * np.sin(np.pi * tt / 0.45) + 120 * np.sin(2 * np.pi * 28 * tt)
    fase = 2 * np.pi * np.cumsum(f) / SR
    x = (np.sin(fase) + 0.5 * np.sin(fase * 2.01) + 0.3 * np.sign(np.sin(fase))) * np.sin(np.pi * tt / 0.45) ** 0.6
    return normal(bp(x, 900, 9000), 0.8)


def rechinido():
    tt = t_(1.8)
    x = np.zeros_like(tt)
    # a tirones: tres chirridos de bisagra con la frecuencia que sube y baja (stick-slip)
    for t0, dur, f0 in ((0.0, 0.35, 380), (0.45, 0.55, 520), (1.05, 0.6, 430)):
        m = (tt >= t0) & (tt < t0 + dur)
        k = (tt[m] - t0) / dur
        f = f0 * (1 + 0.35 * np.sin(np.pi * k * 1.3)) * (1 + 0.04 * np.sign(np.sin(2 * np.pi * 23 * tt[m])))
        fase = 2 * np.pi * np.cumsum(f) / SR
        sierra = 2 * ((fase / (2 * np.pi)) % 1) - 1
        env = np.sin(np.pi * k) ** 0.5
        x[m] += (sierra * 0.6 + np.sin(fase * 3) * 0.25) * env
    x += lp(R.randn(len(tt)), 400) * 0.03
    return normal(eco(bp(x, 200, 6000), 0.9, 0.25, 3000))


def tobogan():
    tt = t_(3.2)
    viento = bp(R.randn(len(tt)), 300, 3000)
    env = np.clip(tt / 0.3, 0, 1) * np.clip((3.0 - tt) / 0.4, 0, 1)
    # el zumbido sube (cada vez más rápido) y retumba el plástico
    f = 90 + 60 * tt
    retumbo = np.sin(2 * np.pi * np.cumsum(f) / SR) * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * tt))
    golpes = np.zeros_like(tt)
    for t0 in (0.6, 1.3, 1.9, 2.4):
        i = int(t0 * SR)
        n = int(0.12 * SR)
        golpes[i:i + n] += lp(R.randn(n), 300) * np.exp(-np.arange(n) / (0.03 * SR))
    x = viento * 0.5 * env + retumbo * 0.35 * env + golpes * 0.8
    # ¡boing! al caer en el colchón
    t0 = 2.85
    m = tt >= t0
    k = tt[m] - t0
    fb = 180 + 260 * np.exp(-k * 9)
    x[m] += np.sin(2 * np.pi * np.cumsum(fb) / SR) * np.exp(-k * 6) * 0.9
    return normal(x)


def main():
    for nombre, f in (('timbre', timbre), ('carrito', carrito), ('cuerda', cuerda), ('chillido', chillido), ('rechinido', rechinido),
                      ('tobogan', tobogan)):
        guardar(nombre, f())


if __name__ == '__main__':
    main()
