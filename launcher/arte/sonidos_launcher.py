#!/usr/bin/env python3
"""Los sonidos de terror del launcher, hechos a mano (sin descargar nada). Van a launcher/web/sonidos:

- ambiente.ogg  un zumbido grave que respira, con viento y una cuerda que tiembla (se repite sin corte)
- crujido.ogg   madera vieja que cruje (cuando él ladea la cabeza de golpe)
- golpe.ogg     se va la luz: un golpe grave con un chasquido de foco
- latido.ogg    un corazón (mientras se instala)
- susurro.ogg   un susurro sin palabras que pasa de un oído al otro
- risa.ogg      una risita de caja de música rota (notas que bajan, desafinadas)

Uso: python3 launcher/arte/sonidos_launcher.py (necesita numpy y ffmpeg con libvorbis)
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'coleccionista', 'herramientas'))
from caja_sonidos import SR, t_, lp, hp, bp, eco, normal  # noqa: E402

SND = os.path.join(AQUI, '..', 'web', 'sonidos')
R = np.random.RandomState(1307)


def guardar(nombre, x, estereo=None, calidad=4):
    os.makedirs(SND, exist_ok=True)
    if estereo is None:
        datos = np.clip(x, -1, 1)
        canales = 1
    else:
        datos = np.clip(np.stack([x, estereo], axis=1).reshape(-1), -1, 1)
        canales = 2
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, 'a.wav')
        with wave.open(w, 'wb') as f:
            f.setnchannels(canales)
            f.setsampwidth(2)
            f.setframerate(SR)
            f.writeframes((datos * 32767).astype(np.int16).tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', w, '-c:a', 'libvorbis', '-q:a', str(calidad),
                        os.path.join(SND, nombre + '.ogg')], check=True)
    print(nombre, f'{len(x) / SR:.2f} s')


def ruido_lento(n, corte):
    return lp(R.randn(n), corte)


def ambiente():
    seg = 24.0
    t = t_(seg)
    n = len(t)
    # todo con periodos que caben exacto en 24 s, para que se repita sin que se note el corte
    f = 2 * np.pi / seg
    dron = (np.sin(2 * np.pi * 41.25 * t) + 0.7 * np.sin(2 * np.pi * 55 * t + 0.4) + 0.35 * np.sin(2 * np.pi * 82.5 * t))
    dron *= 0.55 + 0.45 * np.sin(f * 2 * t) ** 2
    # una cuerda que tiembla muy bajito (tritono, siempre incómodo)
    cuerda = (np.sin(2 * np.pi * 311.25 * t + 3 * np.sin(2 * np.pi * 5.5 * t)) * (0.5 + 0.5 * np.sin(f * 3 * t)) ** 3
              + 0.6 * np.sin(2 * np.pi * 440 * t + 2 * np.sin(2 * np.pi * 4.75 * t)) * (0.5 + 0.5 * np.sin(f * 3 * t + np.pi)) ** 3)
    viento = ruido_lento(n, 400) * (0.6 + 0.4 * np.sin(f * 4 * t + 1))
    x = normal(lp(dron, 300), 1) + 0.08 * cuerda + 0.5 * normal(viento, 1)
    # se cruza el final con el principio (por si el ruido no empata)
    k = int(1.5 * SR)
    rampa = np.linspace(0, 1, k)
    x[:k] = x[:k] * rampa + x[-k:] * (1 - rampa)
    x = x[:-k]
    guardar('ambiente', normal(x, 0.7))


def crujido():
    seg = 1.4
    t = t_(seg)
    x = np.zeros_like(t)
    # el crujido: muchos chasquidos que se aceleran y se frenan, con un tono de madera
    k = 0.05
    while k < seg - 0.25:
        n = int(0.006 * SR)
        i = int(k * SR)
        x[i:i + n] += R.randn(n) * np.exp(-np.arange(n) / (0.0012 * SR)) * R.uniform(0.4, 1)
        k += 0.004 + 0.03 * abs(np.sin(k * 5)) * R.uniform(0.5, 1.5)
    tono = np.sin(2 * np.pi * (180 + 60 * np.sin(t * 7)) * t) * np.exp(-t * 2)
    x = bp(x, 300, 3000) + 0.08 * tono
    guardar('crujido', normal(eco(x, seg=1.0, mezcla=0.3, oscuro=2000), 0.8))


def golpe():
    t = t_(2.6)
    gol = np.sin(2 * np.pi * (48 + 30 * np.exp(-t * 8)) * t) * np.exp(-t * 3.5)
    chas = bp(R.randn(len(t)), 2000, 9000) * np.exp(-t * 60)
    zumb = np.sin(2 * np.pi * 120 * t) * np.exp(-t * 6) * 0.2
    x = 1.4 * gol + 0.5 * chas + zumb
    guardar('golpe', normal(eco(x, seg=2.0, mezcla=0.5, oscuro=1200), 0.95))


def latido():
    seg = 1.6
    t = t_(seg)
    x = np.zeros_like(t)
    for ini, fuerza in ((0.0, 1.0), (0.28, 0.7)):
        tt = t - ini
        m = tt >= 0
        x[m] += fuerza * np.sin(2 * np.pi * (55 + 20 * np.exp(-tt[m] * 30)) * tt[m]) * np.exp(-tt[m] * 14)
    guardar('latido', normal(lp(x, 400), 0.9))


def susurro():
    seg = 3.2
    t = t_(seg)
    n = len(t)
    # ruido con "vocales" (dos resonancias que se mueven) y sílabas que entran y salen
    base = R.randn(n)
    f1 = 700 + 300 * np.sin(2 * np.pi * 2.3 * t)
    f2 = 1800 + 600 * np.sin(2 * np.pi * 3.1 * t + 1)
    a = lp(hp(base, 500), f1)
    b = lp(hp(base, 1400), f2)
    voz = a + 0.6 * b
    silabas = np.clip(np.sin(2 * np.pi * 3.7 * t) * 1.6, 0, 1) ** 2 * np.minimum(1, t / 0.4) * np.minimum(1, (seg - t) / 0.6)
    voz *= silabas
    # pasa de la izquierda a la derecha
    pan = t / seg
    izq = voz * np.cos(pan * np.pi / 2)
    der = voz * np.sin(pan * np.pi / 2)
    m = max(np.max(np.abs(izq)), np.max(np.abs(der)))
    guardar('susurro', izq / m * 0.6, der / m * 0.6)


def risa():
    # una caja de música que se ríe: notas que bajan, cada vez más lentas y desafinadas
    notas = [88, 85, 81, 78, 76, 73, 69]
    x = np.zeros(int(3.2 * SR))
    k = 0.0
    for i, m in enumerate(notas):
        f = 440 * 2 ** ((m - 69) / 12) * (1 - 0.012 * i)
        tt = t_(1.2)
        nota = (np.sin(2 * np.pi * f * tt) * np.exp(-tt * 3) + 0.3 * np.sin(2 * np.pi * f * 5.4 * tt) * np.exp(-tt * 14))
        i0 = int(k * SR)
        x[i0:i0 + len(nota)] += nota[:len(x) - i0] * (1 - 0.06 * i)
        k += 0.12 + 0.035 * i
    guardar('risa', normal(eco(x, seg=1.6, mezcla=0.45, oscuro=3000), 0.6))


def main():
    ambiente()
    crujido()
    golpe()
    latido()
    susurro()
    risa()


if __name__ == '__main__':
    main()
