#!/usr/bin/env python3
"""Los sonidos de los vehículos del evento, hechos a mano (sin descargar nada):

- bus_motor.ogg    el motor diésel del autobús andando (3 s; el evento lo repite): los golpes del motor, el retumbo y el traqueteo
- bus_arranca.ogg  la marcha (rrr-rr-rr), el motor que prende y el acelerón
- bus_frena.ogg    el rechinido de los frenos, el motor que baja y el aire de los frenos (psssh)
- bus_puerta.ogg   la puerta neumática: aire y el golpe de la hoja al doblarse
- montacargas.ogg  el montacargas de jaula subiendo: el motor de engranes, los dientes, la cadena y la jaula que cruje

Uso: python3 herramientas/transporte_sonidos.py  (necesita ffmpeg con libvorbis)
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

SND = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista', 'sounds', 'transporte')
R = np.random.RandomState(1313)


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


def poner(x, y, seg, g=1.0):
    i = int(seg * SR)
    n = min(len(y), len(x) - i)
    if n > 0:
        x[i:i + n] += y[:n] * g


def bordes(x, ent=0.08, sal=0.08):
    n1, n2 = int(ent * SR), int(sal * SR)
    x[:n1] *= np.linspace(0, 1, n1)
    x[-n2:] *= np.linspace(1, 0, n2)
    return x


def motor(f0):
    """Un motor diésel: un golpe por cada explosión (f0 puede cambiar en el tiempo), grave, con su retumbo y el traqueteo de metal."""
    f0 = np.asarray(f0, float)
    fase = np.cumsum(f0 / SR)
    pulso = np.exp(-((fase % 1.0) * 9.0)) * (1 + 0.25 * R.randn(len(fase)) * 0.3)
    golpe = lp(pulso - pulso.mean(), 380) * 3.0
    armon = np.sin(2 * np.pi * fase) * 0.5 + np.sin(4 * np.pi * fase + 0.7) * 0.25
    retumbo = lp(R.randn(len(fase)), 110) * 4.0
    traqueteo = bp(R.randn(len(fase)), 900, 2600) * (0.3 + 0.7 * pulso) * 0.25
    return golpe + armon * 0.6 + retumbo * 0.5 + traqueteo


def bus_motor():
    tt = t_(3.0)
    f0 = 27 + 1.5 * np.sin(2 * np.pi * tt / 3.0)
    x = motor(f0)
    x += np.sin(2 * np.pi * 430 * tt) * 0.02 * (1 + np.sin(2 * np.pi * 0.7 * tt))   # el silbido de la caja de cambios
    return bordes(normal(np.tanh(x * 0.9)) * 0.85, 0.06, 0.06)


def bus_arranca():
    tt = t_(3.0)
    x = np.zeros_like(tt)
    # la marcha: un zumbido áspero que gira a tirones (rrr-rr-rr)
    n = int(0.9 * SR)
    m = tt[:n]
    tir = 0.55 + 0.45 * np.sign(np.sin(2 * np.pi * 5.5 * m))
    marcha = (2 * ((m * 150) % 1.0) - 1) * tir
    marcha = bp(marcha, 120, 1800) * 0.7 + bp(R.randn(n), 1500, 4000) * 0.08 * tir
    x[:n] += marcha
    # prende: el motor sube de vueltas y se asienta
    k = tt - 0.85
    f0 = np.where(k < 0, 0.0, 14 + 34 * np.exp(-((k - 0.55) ** 2) / 0.09) + 13 * (1 - np.exp(-k * 3)))
    viva = np.clip(k * 6, 0, 1)
    x += motor(np.maximum(f0, 1.0)) * viva * 0.9
    # el escape al prender: un soplido
    poner(x, lp(R.randn(int(0.4 * SR)), 600) * np.exp(-t_(0.4) * 9) * 1.2, 0.85)
    return bordes(normal(np.tanh(x * 1.1)), 0.01, 0.25)


def bus_frena():
    tt = t_(2.6)
    x = np.zeros_like(tt)
    # el motor que baja de vueltas
    f0 = 22 + 12 * np.exp(-tt * 1.4)
    x += motor(f0) * np.exp(-tt * 0.6) * 0.6
    # el rechinido: un tono agudo que tiembla y baja un poco
    n = int(1.3 * SR)
    m = tt[:n]
    f = 2350 - 220 * m + 40 * np.sin(2 * np.pi * 7 * m)
    ph = 2 * np.pi * np.cumsum(f) / SR
    rech = (np.sin(ph) + 0.4 * np.sin(2.01 * ph) + 0.2 * np.sin(3.02 * ph)) * np.clip(m * 8, 0, 1) * np.exp(-m * 1.2) * 0.22
    x[:n] += rech
    # el aire de los frenos: psssh
    a = int(1.2 * SR)
    aire = hp(R.randn(a), 2200) * np.exp(-t_(1.2) * 2.6) * 0.55
    poner(x, aire, 1.3)
    return bordes(normal(eco(x, 0.6, 0.15, 3000)), 0.01, 0.15)


def bus_puerta():
    tt = t_(1.2)
    x = np.zeros_like(tt)
    aire = hp(R.randn(int(0.55 * SR)), 2800) * np.exp(-t_(0.55) * 4) * 0.6
    poner(x, aire, 0.0)
    # la hoja que se dobla y pega: un golpe sordo con un traqueteo de metal
    g = t_(0.4)
    golpe = np.sin(2 * np.pi * (70 + 50 * np.exp(-g * 30)) * g) * np.exp(-g * 14) * 0.9
    golpe += bp(R.randn(len(g)), 1200, 4500) * np.exp(-g * 30) * 0.5
    poner(x, golpe, 0.5)
    poner(x, bp(R.randn(int(0.15 * SR)), 2000, 6000) * np.exp(-t_(0.15) * 40) * 0.3, 0.62)
    return bordes(normal(x), 0.005, 0.1)


def montacargas():
    tt = t_(2.6)
    # el motor eléctrico de engranes: zumbido de 60 y 120 Hz que sube y baja un poquito
    x = (np.sin(2 * np.pi * 60 * tt) * 0.35 + np.sin(2 * np.pi * 120 * tt) * 0.25) * (0.85 + 0.15 * np.sin(2 * np.pi * 1.3 * tt))
    # los dientes de los engranes: clics metálicos parejos
    clic = bp(R.randn(int(0.02 * SR)), 2200, 6000) * np.exp(-t_(0.02) * 220)
    k = 0.0
    while k < 2.58:
        poner(x, clic, k, 0.35 + 0.1 * R.rand())
        k += 1 / 17.0
    # la cadena (golpecitos al azar) y la jaula que cruje (tonos que se doblan)
    for _ in range(9):
        c = t_(0.06)
        poner(x, bp(R.randn(len(c)), 900, 3500) * np.exp(-c * 60) * 0.5, R.rand() * 2.5)
    for _ in range(2):
        c = t_(0.5)
        f = 280 + 180 * c + 60 * R.rand()
        cr = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-c * 5) * 0.18
        cr += bp(R.randn(len(c)), 300, 1200) * np.exp(-c * 6) * 0.12
        poner(x, cr, 0.3 + R.rand() * 1.8)
    x += lp(R.randn(len(tt)), 90) * 1.2
    return bordes(normal(np.tanh(x * 1.2)) * 0.85, 0.06, 0.06)


def main():
    guardar('bus_motor', bus_motor())
    guardar('bus_arranca', bus_arranca())
    guardar('bus_frena', bus_frena())
    guardar('bus_puerta', bus_puerta())
    guardar('montacargas', montacargas())


if __name__ == '__main__':
    main()
