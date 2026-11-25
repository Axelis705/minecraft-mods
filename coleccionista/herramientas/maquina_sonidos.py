#!/usr/bin/env python3
"""Los sonidos de la final (veintiuno), hechos a mano (sin descargar nada):

- maquina_cierra.ogg   la máquina atrapa el brazo: trinquete (clic, clic, clic) y el golpe del cepo de latón
- maquina_aprieta.ogg  primera ronda perdida: el tornillo gira, el metal cruje y algo truena
- maquina_chispas.ogg  segunda ronda perdida: chisporroteo eléctrico con zumbido
- maquina_pum.ogg      tercera ronda perdida: la cuchilla silba al bajar y ¡PUM! (golpe sordo, metal que vibra y algo húmedo)
- maquina_abre.ogg     la máquina suelta al ganador: vapor y el cepo que se abre
- carta.ogg            una carta que se desliza sobre el tapete

Uso: python3 herramientas/maquina_sonidos.py  (necesita ffmpeg con libvorbis)
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
from caja_sonidos import SR, t_, lp, hp, bp, eco, normal, metal  # noqa: E402

SND = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista', 'sounds', 'veintiuno')
R = np.random.RandomState(2121)


def guardar(nombre, x):
    os.makedirs(SND, exist_ok=True)
    x = np.clip(x, -1, 1)
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, 'a.wav')
        with wave.open(w, 'wb') as f:
            f.setnchannels(1)
            f.setsampwidth(2)
            f.setframerate(SR)
            f.writeframes((x * 32767).astype(np.int16).tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', w, '-c:a', 'libvorbis', '-q:a', '6', os.path.join(SND, nombre + '.ogg')],
                       check=True)
    print(nombre, f'{len(x) / SR:.2f} s')


def poner(x, y, seg, g=1.0):
    i = int(seg * SR)
    n = min(len(y), len(x) - i)
    if n > 0:
        x[i:i + n] += y[:n] * g


def clic(f=3200):
    return metal(0.06, [f, f * 1.47, f * 2.1], 95)


def sordo(seg, f0=55, decae=7):
    """Un golpe grave (como un mazo sobre madera gruesa)."""
    tt = t_(seg)
    x = np.sin(2 * np.pi * f0 * tt * (1 + 0.6 * np.exp(-tt * 25))) * np.exp(-tt * decae)
    x += 0.6 * lp(R.randn(len(tt)), 400) * np.exp(-tt * 30)
    return x


def maquina_cierra():
    x = np.zeros(int(1.3 * SR))
    for k in range(4):
        poner(x, clic(2600 + k * 180), 0.05 + k * 0.09, 0.55)
    poner(x, metal(0.9, [310, 520, 890, 1430, 2210], 9) * 1.3, 0.42)
    poner(x, sordo(0.5, 70, 10), 0.42, 0.8)
    return normal(eco(x, 1.0, 0.35, 3000), 0.92)


def maquina_aprieta():
    x = np.zeros(int(2.2 * SR))
    # el tornillo: trinquete que se va haciendo lento
    tt = 0.0
    for k in range(9):
        poner(x, clic(2300 - k * 60), 0.02 + tt, 0.45)
        tt += 0.07 + k * 0.022
    # el metal que cruje (ruido grave modulado)
    n = t_(1.4)
    cruje = bp(R.randn(len(n)), 120, 900) * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * n) ** 2) * np.clip(n * 3, 0, 1) * np.exp(-n * 1.2)
    poner(x, cruje, 0.2, 0.6)
    # ¡crac! (algo truena)
    c = t_(0.18)
    crac = hp(R.randn(len(c)), 1200) * np.exp(-c * 45) + 0.5 * sordo(0.18, 120, 25)
    poner(x, crac, 1.25, 1.1)
    return normal(eco(x, 1.0, 0.3, 3500), 0.92)


def maquina_chispas():
    tt = t_(1.8)
    # el zumbido (120 Hz con armónicos) que sube y baja
    zum = sum(np.sin(2 * np.pi * 120 * k * tt) / k for k in (1, 2, 3, 5, 7)) * 0.25
    zum *= np.clip(tt * 8, 0, 1) * np.exp(-np.clip(tt - 1.2, 0, None) * 6)
    x = zum
    # los chispazos: golpecitos de ruido brillante al azar (más al principio)
    for _ in range(46):
        s = R.rand() ** 1.6 * 1.5
        d = t_(0.01 + R.rand() * 0.025)
        ch = hp(R.randn(len(d)), 3000) * np.exp(-d * (120 + R.rand() * 200))
        poner(x, ch, s, 0.5 + R.rand() * 0.9)
    return normal(eco(x, 0.7, 0.25, 6000), 0.9)


def maquina_pum():
    x = np.zeros(int(2.6 * SR))
    # la cuchilla silba al bajar (ruido que sube de tono)
    s = t_(0.22)
    silbido = bp(R.randn(len(s)), 1500, 3000 + 9000 * s / 0.22) * (s / 0.22) ** 2
    poner(x, silbido, 0.0, 0.7)
    # ¡PUM!
    poner(x, sordo(1.2, 48, 4.5) * 1.4, 0.22)
    poner(x, metal(1.6, [190, 330, 610, 1020, 1730, 2650], 3.2) * 1.1, 0.22)
    # algo húmedo
    h = t_(0.35)
    humedo = lp(R.randn(len(h)), 900) * np.exp(-h * 14) * (1 + 0.6 * np.sin(2 * np.pi * 23 * h))
    poner(x, humedo, 0.26, 0.9)
    return normal(eco(x, 1.8, 0.5, 2200), 0.97)


def maquina_abre():
    x = np.zeros(int(1.4 * SR))
    v = t_(1.0)
    vapor = bp(R.randn(len(v)), 2500, 9000) * np.clip(v * 10, 0, 1) * np.exp(-v * 2.8) * 0.6
    poner(x, vapor, 0.0)
    poner(x, metal(0.6, [420, 700, 1180, 1900], 11), 0.18, 0.9)
    for k in range(3):
        poner(x, clic(2000 + k * 300), 0.5 + k * 0.08, 0.4)
    return normal(eco(x, 0.9, 0.3, 4000), 0.88)


def carta():
    tt = t_(0.16)
    x = bp(R.randn(len(tt)), 2500, 8000) * np.sin(np.pi * np.clip(tt / 0.13, 0, 1)) ** 2
    x += 0.4 * hp(R.randn(len(tt)), 4000) * np.exp(-np.abs(tt - 0.12) * 300)
    return normal(x, 0.6)


def main():
    guardar('maquina_cierra', maquina_cierra())
    guardar('maquina_aprieta', maquina_aprieta())
    guardar('maquina_chispas', maquina_chispas())
    guardar('maquina_pum', maquina_pum())
    guardar('maquina_abre', maquina_abre())
    guardar('carta', carta())


if __name__ == '__main__':
    main()
