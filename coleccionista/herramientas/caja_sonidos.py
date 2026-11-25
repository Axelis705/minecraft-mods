#!/usr/bin/env python3
"""Los sonidos de la caja de música, hechos a mano (sin descargar nada):

- caja_disparo.ogg   un escopetazo fuerte: chasquido, estallido, golpe grave y el eco largo del salón
- caja_tension.ogg   ~5 s de tensión: un zumbido grave que sube, un latido que se acelera y un silencio seco al final
- caja_cerrojo.ogg   el arma se arma: dos golpes metálicos (clic-clac) de escopeta de corredera
- caja_tictac.ogg    el mecanismo de cuerda girando: tic... tac... (2 s)
- caja_compuerta.ogg la compuerta del piso: golpe de metal, cadenas y un siseo de vapor

Uso: python3 herramientas/caja_sonidos.py  (necesita ffmpeg con libvorbis)
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

AQUI = os.path.dirname(os.path.abspath(__file__))
SND = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista', 'sounds', 'caja')
SR = 44100
R = np.random.RandomState(1234)


def t_(seg):
    return np.arange(int(seg * SR)) / SR


def lp(x, corte):
    """Filtro pasa-bajos de un polo (corte en Hz; puede ser un arreglo que cambia en el tiempo)."""
    corte = np.broadcast_to(np.asarray(corte, float), x.shape)
    a = np.exp(-2 * np.pi * corte / SR)
    y = np.zeros_like(x)
    prev = 0.0
    for i in range(len(x)):
        prev = (1 - a[i]) * x[i] + a[i] * prev
        y[i] = prev
    return y


def hp(x, corte):
    return x - lp(x, corte)


def bp(x, lo, hi):
    return lp(hp(x, lo), hi)


def eco(x, seg=1.8, mezcla=0.35, oscuro=2500):
    """Reverberación de salón grande: respuesta de ruido que decae (y se oscurece)."""
    n = int(seg * SR)
    tt = np.arange(n) / SR
    ir = R.randn(n) * np.exp(-tt * 3.2)
    ir = lp(ir, oscuro * np.exp(-tt * 1.2) + 300)
    ir[: int(0.012 * SR)] = 0  # el primer rebote llega un poco después
    ir /= np.max(np.abs(ir)) + 1e-9
    y = np.convolve(x, ir) * mezcla * 0.06
    out = np.zeros(len(y))
    out[: len(x)] += x
    return out + y


def normal(x, pico=0.97):
    return x / (np.max(np.abs(x)) + 1e-9) * pico


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
        out = os.path.join(SND, nombre + '.ogg')
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', w, '-c:a', 'libvorbis', '-q:a', '6', out], check=True)
    print(nombre, f'{len(x) / SR:.2f} s')


def disparo():
    tt = t_(0.6)
    # el chasquido (muy corto y brillante) y el estallido (ruido que se oscurece rápido)
    ruido = R.randn(len(tt))
    chas = hp(ruido, 3000) * np.exp(-tt * 900) * 1.6
    estallido = lp(ruido, 9000 * np.exp(-tt * 18) + 250) * np.exp(-tt * 14) * 1.3
    # el golpe grave (la boca de la escopeta): una onda que cae de 110 a 38 Hz
    f = 38 + 72 * np.exp(-tt * 22)
    fase = 2 * np.pi * np.cumsum(f) / SR
    golpe = np.sin(fase) * np.exp(-tt * 9) * 1.4
    x = chas + estallido + golpe
    x = np.tanh(x * 2.6)  # saturado: suena "fuerte" aunque se baje el volumen
    x = eco(x, 2.2, 0.55, 3000)
    return normal(x)


def tension():
    tt = t_(5.2)
    dur = tt[-1]
    # el zumbido grave que sube (dos tonos un poco desafinados, con armónicos)
    f = 46 * (1 + 0.55 * (tt / dur) ** 2)
    fase = 2 * np.pi * np.cumsum(f) / SR
    dron = (np.sin(fase) + 0.5 * np.sin(fase * 2.01) + 0.25 * np.sin(fase * 3.03)) * (0.25 + 0.75 * (tt / dur) ** 1.5)
    # un siseo que crece
    sis = bp(R.randn(len(tt)), 800, 3500) * (tt / dur) ** 3 * 0.35
    # el latido: cada vez más rápido
    lat = np.zeros_like(tt)
    k, intervalo = 0.25, 0.95
    while k < dur - 0.4:
        for dk, amp in ((0.0, 1.0), (0.17, 0.6)):
            i = int((k + dk) * SR)
            n = int(0.16 * SR)
            if i + n < len(lat):
                tl = np.arange(n) / SR
                lat[i:i + n] += np.sin(2 * np.pi * (55 - 20 * tl / 0.16) * tl) * np.exp(-tl * 28) * amp
        k += intervalo
        intervalo = max(0.42, intervalo * 0.88)
    x = dron * 0.55 + sis + lat * 0.9
    # silencio seco al final (corta de golpe)
    x[int((dur - 0.25) * SR):] *= np.linspace(1, 0, len(x) - int((dur - 0.25) * SR)) ** 4
    return normal(np.tanh(x * 1.5), 0.9)


def metal(n_seg, f0s, decae=40):
    tt = t_(n_seg)
    s = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * decae * (1 + i * 0.4)) for i, f in enumerate(f0s))
    golpe = bp(R.randn(len(tt)), 1500, 7000) * np.exp(-tt * 120)
    return s * 0.5 + golpe


def cerrojo():
    a = metal(0.35, [1850, 3120, 4710, 6100], 30)
    b = metal(0.4, [1420, 2650, 3980, 5600], 26)
    x = np.zeros(int(0.85 * SR))
    x[:len(a)] += a
    i = int(0.28 * SR)
    x[i:i + len(b)] += b[: len(x) - i] * 1.1
    # un poco de "arrastre" entre los dos golpes
    tt = t_(0.2)
    x[int(0.06 * SR):int(0.06 * SR) + len(tt)] += bp(R.randn(len(tt)), 600, 2500) * np.exp(-tt * 12) * 0.15
    return normal(eco(x, 0.9, 0.3, 4000), 0.9)


def tictac():
    x = np.zeros(int(2.0 * SR))
    for k in range(4):
        tic = metal(0.12, [2800, 4100] if k % 2 == 0 else [2100, 3300], 70)
        i = int(k * 0.5 * SR)
        x[i:i + len(tic)] += tic
    return normal(eco(x, 0.6, 0.25, 5000), 0.8)


def compuerta():
    tt = t_(2.4)
    golpe = metal(0.9, [180, 260, 410, 690], 6) * 1.2
    x = np.zeros(len(tt))
    x[:len(golpe)] += golpe
    # cadenas: muchos golpecitos metálicos
    for k in range(14):
        c = metal(0.08, [3000 + R.rand() * 1500, 5200], 90) * (0.4 + R.rand() * 0.3)
        i = int((0.25 + k * 0.07 + R.rand() * 0.03) * SR)
        x[i:i + len(c)] += c
    # vapor
    vap = bp(R.randn(len(tt)), 2000, 9000) * np.clip((tt - 0.3) * 3, 0, 1) * np.exp(-np.clip(tt - 0.6, 0, None) * 1.6) * 0.5
    return normal(eco(x + vap, 1.4, 0.45, 2500), 0.9)


def main():
    guardar('caja_disparo', disparo())
    guardar('caja_tension', tension())
    guardar('caja_cerrojo', cerrojo())
    guardar('caja_tictac', tictac())
    guardar('caja_compuerta', compuerta())


if __name__ == '__main__':
    main()
