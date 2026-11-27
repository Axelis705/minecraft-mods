#!/usr/bin/env python3
"""Sonidos del invierno, hechos a mano con numpy (sin descargar nada):

- trineo_desliza.ogg  el roce de los patines del trineo en la nieve (2 s, se repite sin cortes; el juego le sube el volumen con la velocidad)
- cascabel.ogg        un cascabel chiquito que se sacude (el del trineo de madera)
- patin.ogg           una zancada de patín de hielo: el filo que raspa (shhk)
- campanada.ogg       una campanada grande de iglesia (para las doce uvas de Año Nuevo)
- carta_vuela.ogg     la carta a Santa que sale volando: brillitos que suben

Uso: python3 herramientas/sonidos_invierno.py  (necesita ffmpeg con libvorbis)
"""
import os
import subprocess
import tempfile
import wave

import numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
AQUI = os.path.dirname(os.path.abspath(__file__))
SND = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad', 'sounds')
R = np.random.RandomState(2512)


def t_(seg):
    return np.arange(int(seg * SR)) / SR


def lp(x, f):
    return sosfilt(butter(4, f, 'low', fs=SR, output='sos'), x)


def hp(x, f):
    return sosfilt(butter(4, f, 'high', fs=SR, output='sos'), x)


def bp(x, a, b):
    return sosfilt(butter(3, [a, b], 'band', fs=SR, output='sos'), x)


def normal(x, pico=0.95):
    m = np.max(np.abs(x))
    return x / m * pico if m > 0 else x


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
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', w, '-c:a', 'libvorbis', '-q:a', '5', os.path.join(SND, nombre + '.ogg')],
                       check=True)
    print(nombre, f'{len(x) / SR:.2f} s')


def ciclico(x, cruce=0.25):
    """Para que se repita sin brinco: el final se funde con el principio."""
    n = int(cruce * SR)
    a = np.linspace(0, 1, n)
    y = x[:-n].copy()
    y[:n] = y[:n] * a + x[-n:] * (1 - a)
    return y


def trineo_desliza():
    tt = t_(2.25)
    # el siseo de la nieve: ruido entre 900 y 5000 Hz que respira un poco
    sis = bp(R.randn(len(tt)), 900, 5000) * (0.8 + 0.2 * np.sin(2 * np.pi * 1.7 * tt) + 0.1 * np.sin(2 * np.pi * 4.3 * tt + 1))
    # el crujido: granitos de nieve aplastada, graves y cortos
    cruj = np.zeros_like(tt)
    for _ in range(140):
        i = R.randint(0, len(tt) - 900)
        g = t_(0.02)
        cruj[i:i + len(g)] += lp(R.randn(len(g)), 1400) * np.exp(-g * 160) * (0.4 + R.rand())
    # el retumbo de la tabla
    ret = lp(R.randn(len(tt)), 160) * 2.2
    x = sis * 0.55 + cruj * 0.6 + ret * 0.25
    return normal(ciclico(x), 0.8)


def cascabel():
    tt = t_(0.9)
    x = np.zeros_like(tt)
    # tres sacudidas, cada una con un golpe de bolita y su campanita (parciales inarmónicos agudos)
    for k, (cuando, g) in enumerate(((0.0, 1.0), (0.11, 0.7), (0.2, 0.45))):
        i = int(cuando * SR)
        n = len(tt) - i
        s = np.arange(n) / SR
        f0 = 2650 + 90 * k
        tono = sum(a * np.sin(2 * np.pi * f0 * m * s + R.rand() * 6) for m, a in ((1, 1.0), (1.51, 0.5), (2.43, 0.35), (3.1, 0.2)))
        tono *= np.exp(-s * 9)
        clic = hp(R.randn(n), 5000) * np.exp(-s * 300) * 0.6
        x[i:] += (tono * 0.5 + clic) * g
    return normal(x, 0.8)


def patin():
    tt = t_(0.6)
    env = np.minimum(tt / 0.06, 1) * np.exp(-np.maximum(tt - 0.06, 0) * 7)
    # el filo raspa: una banda de ruido que sube y un silbido que tiembla
    ruido = R.randn(len(tt))
    x = np.zeros_like(tt)
    for a, b, g in ((2000, 4500, 0.6), (4500, 9000, 0.4)):
        x += bp(ruido, a, b) * g
    f = 3100 + 900 * tt + 60 * np.sin(2 * np.pi * 23 * tt)
    x += np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.06
    return normal(x * env, 0.75)


def campanada():
    tt = t_(5.0)
    f0 = 196.0
    # una campana de iglesia: el "hum" grave, la fundamental, la tercera menor, la quinta, la octava y los agudos que se apagan rápido
    parciales = ((0.5, 0.55, 0.45), (1.0, 1.0, 0.7), (1.19, 0.6, 0.9), (1.5, 0.45, 1.1), (2.0, 0.5, 1.3), (2.51, 0.3, 2.0),
                 (2.66, 0.25, 2.2), (3.01, 0.2, 2.6), (4.07, 0.12, 3.4), (5.4, 0.08, 4.5))
    x = np.zeros_like(tt)
    for m, a, d in parciales:
        bat = 1 + 0.15 * np.sin(2 * np.pi * (0.6 + m * 0.3) * tt)   # el batido de la campana
        x += a * np.sin(2 * np.pi * f0 * m * tt + R.rand() * 6) * np.exp(-tt * d) * bat
    golpe = lp(R.randn(len(tt)), 2500) * np.exp(-tt * 60) * 0.8
    x = x + golpe
    # un poco de eco de plaza
    eco = np.zeros_like(x)
    for dt, g in ((0.11, 0.35), (0.23, 0.22), (0.41, 0.12)):
        i = int(dt * SR)
        eco[i:] += x[:-i] * g
    x = x + lp(eco, 3000)
    x[-int(0.3 * SR):] *= np.linspace(1, 0, int(0.3 * SR))
    return normal(x, 0.9)


def carta_vuela():
    tt = t_(1.6)
    x = np.zeros_like(tt)
    # el papel que se dobla y sale: un soplido
    x += bp(R.randn(len(tt)), 600, 3000) * np.exp(-tt * 6) * 0.5
    # brillitos que suben (una escala de campanitas, cada vez más agudas)
    notas = [1046.5, 1318.5, 1568.0, 2093.0, 2637.0, 3136.0, 4186.0]
    for k, f in enumerate(notas):
        i = int((0.08 + k * 0.12) * SR)
        s = np.arange(len(tt) - i) / SR
        tono = (np.sin(2 * np.pi * f * s) + 0.3 * np.sin(2 * np.pi * f * 2.01 * s)) * np.exp(-s * 6)
        x[i:] += tono * (0.35 - k * 0.025)
    # un destello de agudos al final
    x += hp(R.randn(len(tt)), 7000) * np.exp(-((tt - 0.95) ** 2) / 0.02) * 0.15
    x[-int(0.2 * SR):] *= np.linspace(1, 0, int(0.2 * SR))
    return normal(x, 0.8)


def main():
    guardar('trineo_desliza', trineo_desliza())
    guardar('cascabel', cascabel())
    guardar('patin', patin())
    guardar('campanada', campanada())
    guardar('carta_vuela', carta_vuela())


if __name__ == '__main__':
    main()
