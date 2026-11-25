#!/usr/bin/env python3
"""Los sonidos de la caja sorpresa (capítulo 4), hechos a mano (sin descargar nada):

- sor_susto.ogg    el golpe del susto cuando Don Resorte muerde: un estallido grave, un chillido de cuerdas desafinadas y el eco
- sor_mordida.ogg  la mordida: crujido de huesos, algo húmedo que se aplasta y un gruñido
- sor_salta.ogg    el resorte: ¡boing! que se estira y tiembla, con una risa ahogada de juguete

Uso: python3 herramientas/sorpresa_sonidos.py  (necesita ffmpeg con libvorbis)
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
from caja_sonidos import SR, t_, lp, hp, bp, eco, normal  # noqa: E402

SND = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista', 'sounds', 'sorpresa')
R = np.random.RandomState(4321)


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


def susto():
    tt = t_(1.6)
    ruido = R.randn(len(tt))
    golpe = lp(ruido, 6000 * np.exp(-tt * 10) + 200) * np.exp(-tt * 7) * 1.5
    f = 32 + 60 * np.exp(-tt * 9)
    boom = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 3) * 1.4
    # cuerdas desafinadas que chillan (un racimo de tonos agudos que tiemblan)
    chillido = np.zeros_like(tt)
    for k, f0 in enumerate((1320, 1398, 1480, 1568, 1661)):
        vib = 1 + 0.012 * np.sin(2 * np.pi * (6 + k) * tt)
        fase = 2 * np.pi * np.cumsum(f0 * vib) / SR
        chillido += np.sign(np.sin(fase)) * 0.15 + np.sin(fase * 2) * 0.1
    chillido *= np.exp(-tt * 2.2) * np.clip(tt * 40, 0, 1)
    x = np.tanh((golpe + boom + bp(chillido, 900, 7000) * 1.3) * 2.2)
    return normal(eco(x, 1.6, 0.5, 3500))


def mordida():
    tt = t_(1.3)
    x = np.zeros_like(tt)
    # el chasquido de la quijada y los crujidos (muchos clics secos y graves)
    for k in range(9):
        i = int((0.02 + k * 0.08 + R.rand() * 0.04) * SR)
        n = int(0.05 * SR)
        tl = np.arange(n) / SR
        c = hp(R.randn(n), 900 + R.rand() * 1500) * np.exp(-tl * (90 + R.rand() * 60)) * (1.4 if k == 0 else 0.6 + R.rand() * 0.5)
        x[i:i + n] += c[: len(x) - i]
    # lo húmedo: ruido filtrado que borbotea
    hum = lp(R.randn(len(tt)), 900) * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * tt)) * np.exp(-tt * 2.5) * 0.9
    # el gruñido (grave, ronco)
    f = 70 + 15 * np.sin(2 * np.pi * 3 * tt)
    gru = np.tanh(np.sin(2 * np.pi * np.cumsum(f) / SR) * 3) * np.exp(-((tt - 0.5) ** 2) / 0.08) * 0.5
    x = x + hum + gru
    return normal(eco(x, 0.8, 0.3, 3000))


def salta():
    tt = t_(1.4)
    # ¡boing!: un tono que sube rápido y tiembla (el resorte)
    f = 140 + 520 * (1 - np.exp(-tt * 9)) + 60 * np.sin(2 * np.pi * 18 * tt) * np.exp(-tt * 2)
    boing = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 2.2)
    golpe = lp(R.randn(len(tt)), 3000) * np.exp(-tt * 30) * 1.2
    # la risa ahogada de juguete: tres "ja" con tono de caja de música desafinada
    risa = np.zeros_like(tt)
    for k in range(3):
        i = int((0.45 + k * 0.17) * SR)
        n = int(0.12 * SR)
        tl = np.arange(n) / SR
        r = np.sin(2 * np.pi * (520 - k * 60) * tl) * np.sin(2 * np.pi * 9 * tl) * np.exp(-tl * 18)
        risa[i:i + n] += r[: len(risa) - i] * 0.6
    x = np.tanh((boing * 0.9 + golpe + risa) * 1.6)
    return normal(eco(x, 1.0, 0.35, 4000), 0.9)


def main():
    guardar('sor_susto', susto())
    guardar('sor_mordida', mordida())
    guardar('sor_salta', salta())


if __name__ == '__main__':
    main()
