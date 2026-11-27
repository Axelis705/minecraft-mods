#!/usr/bin/env python3
"""Sonidos del lanzanieves (sintetizados, versión 2: más cuerpo y más "neumáticos"):
- dispara (4 variantes): clic del mecanismo + golpe grave de aire ("tup") + soplido que baja + crujido de nieve apretada + un brillito de hielo.
- gira: el motor que toma vuelo (zumbido que sube), el trinquete de los cañones cada vez más rápido y el aire que cortan.
- carga: la bola gigante formándose: zumbido grave que crece, cristales de hielo que crujen y un acorde que sube hasta un "¡ding!".
- bola: el cañonazo de la bola gigante: golpe muy grave, ráfaga de aire con barrido y cola de nieve.
- impacto: la bola gigante revienta: golpe sordo, nieve que se esparce y copos que caen.
- aspira: aspirar nieve del piso (un "shhhup" que sube con granitos de nieve).
- vacio: sin tiros: doble clic seco y un soplidito.
Uso: python3 herramientas/sonidos_lanzanieves.py"""
import json
import os
import subprocess

import numpy as np
from scipy.signal import butter, lfilter, sosfilt

SR = 44100
AQUI = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad', 'sounds')


def t_(dur):
    return np.arange(int(dur * SR)) / SR


def banda(x, lo, hi, orden=2):
    sos = butter(orden, [lo / (SR / 2), min(hi, SR / 2 - 100) / (SR / 2)], btype='band', output='sos')
    return sosfilt(sos, x)


def bajos(x, corte, orden=2):
    sos = butter(orden, corte / (SR / 2), output='sos')
    return sosfilt(sos, x)


def altos(x, corte, orden=2):
    sos = butter(orden, corte / (SR / 2), btype='high', output='sos')
    return sosfilt(sos, x)


def barrido(x, f0, f1, q=0.7, pasos=48):
    """Filtro pasa-bandas que se mueve de f0 a f1 (por bloques, suave)."""
    n = len(x)
    out = np.zeros(n)
    bloque = n // pasos + 1
    for i in range(pasos):
        a, b = i * bloque, min(n, (i + 1) * bloque + 512)
        if a >= n:
            break
        f = f0 * (f1 / f0) ** (i / max(1, pasos - 1))
        lo, hi = f * (1 - q / 2), f * (1 + q / 2)
        y = banda(x[max(0, a - 512):b], max(30, lo), hi)
        y = y[a - max(0, a - 512):]
        w = np.ones(len(y))
        k = min(256, len(y))
        w[:k] = np.linspace(0, 1, k) if i > 0 else 1
        out[a:a + len(y)] += (y * w)[:n - a]
    return out


def env(t, ataque, caida, inicio=0.0):
    tt = t - inicio
    e = np.where(tt < 0, 0, np.where(tt < ataque, tt / max(ataque, 1e-4), np.exp(-(tt - ataque) / caida)))
    return e


def granos(rng, n, t0, t1, flo, fhi, nivel, dur=(0.001, 0.004), densidad_baja=True):
    """Muchos granitos de nieve (clics filtrados) entre t0 y t1."""
    x = np.zeros(int(t1 * SR) + int(0.01 * SR))
    for _ in range(n):
        u = rng.random()
        if densidad_baja:
            u = u ** 1.8           # más al principio
        p = t0 + (t1 - t0) * u
        d = rng.uniform(*dur)
        m = int(d * SR)
        g = rng.standard_normal(m) * np.hanning(m)
        a = int(p * SR)
        x[a:a + m] += g * rng.uniform(0.4, 1.0) * nivel * (1 - 0.6 * (p - t0) / (t1 - t0))
    return banda(x, flo, fhi)


def pad(x, n):
    """Del largo n (corta o rellena con silencio)."""
    return x[:n] if len(x) >= n else np.concatenate([x, np.zeros(n - len(x))])


def sat(x, k=1.6):
    return np.tanh(x * k) / np.tanh(k)


def norm(x, pico=0.9):
    return x / (np.max(np.abs(x)) + 1e-9) * pico


# ------------------------------------------------------------------------------------------------ sonidos

def dispara(rng, tono=1.0):
    t = t_(0.32)
    n = len(t)
    clic = altos(rng.standard_normal(n), 2500) * env(t, 0.0003, 0.0025) * 0.9
    f = (128 * tono) * np.exp(-t / 0.05) + 62 * tono
    golpe = np.sin(2 * np.pi * np.cumsum(f) / SR) * env(t, 0.002, 0.045) * 1.25
    golpe += np.sin(4 * np.pi * np.cumsum(f) / SR) * env(t, 0.002, 0.03) * 0.35
    aire = barrido(rng.standard_normal(n), 2600 * tono, 700 * tono, q=1.1) * env(t, 0.003, 0.055, 0.002) * 1.3
    cola = bajos(rng.standard_normal(n), 900) * env(t, 0.01, 0.07, 0.01) * 0.35
    nieve = pad(granos(rng, 70, 0.012, 0.16, 1800, 7000, 1.0), n) * 0.85
    brillo = np.zeros(n)
    for fb, ab in ((3520 * tono, 0.05), (5280 * tono, 0.03)):
        brillo += np.sin(2 * np.pi * fb * t) * env(t, 0.004, 0.06, 0.02) * ab
    x = sat(clic + golpe + aire + cola + nieve, 1.3) + brillo
    return norm(x, 0.78)


def gira(rng):
    d = 0.75
    t = t_(d)
    n = len(t)
    vel = np.clip(t / 0.55, 0, 1) ** 0.65                   # de parado a toda velocidad
    f = 38 + 190 * vel
    fase = 2 * np.pi * np.cumsum(f) / SR
    sierra = 2 * ((fase / (2 * np.pi)) % 1.0) - 1
    motor = np.zeros(n)
    corte = 300 + 2600 * vel
    for i in range(0, n, 1024):                             # pasa-bajos que se abre al acelerar
        seg = sierra[max(0, i - 512):i + 1024]
        y = bajos(seg, corte[min(n - 1, i)])
        motor[i:i + 1024] = y[i - max(0, i - 512):][:len(motor[i:i + 1024])]
    motor = motor * 0.55 + np.sin(2 * fase) * 0.18 + np.sin(0.5 * fase) * 0.22
    # trinquete: un clic por cada cuarto de vuelta (4 cañones)
    vueltas = np.cumsum(f / 8.0) / SR
    clics = np.zeros(n)
    marcas = np.where(np.diff(np.floor(vueltas * 4)) > 0)[0]
    for m in marcas:
        k = int(0.0025 * SR)
        g = rng.standard_normal(k) * np.hanning(k)
        clics[m:m + k] += g[:n - m] * (0.9 - 0.5 * vel[m])
    clics = banda(clics, 1800, 6500)
    # el aire que cortan los cañones (soplido que late con la vuelta)
    aire = banda(rng.standard_normal(n), 600, 3200) * (0.5 + 0.5 * np.sin(2 * np.pi * np.cumsum(f / 2) / SR)) * vel * 0.35
    silbido = np.sin(2 * np.pi * np.cumsum(f * 11) / SR) * vel ** 2 * 0.05
    e = np.clip(t / 0.03, 0, 1) * np.clip((d - t) / 0.12, 0, 1)
    x = (motor + clics * 0.8 + aire + silbido) * e
    return norm(sat(x, 1.2), 0.75)


def carga(rng):
    d = 1.15
    t = t_(d)
    n = len(t)
    sube = np.clip(t / 1.0, 0, 1)
    zumbido = np.sin(2 * np.pi * np.cumsum(55 + 50 * sube) / SR) * (0.2 + 0.5 * sube)
    zumbido += np.sin(2 * np.pi * np.cumsum(110 + 100 * sube) / SR) * 0.15 * sube
    acorde = np.zeros(n)
    for k, base in enumerate((523.25, 659.25, 783.99, 1046.5)):
        fk = base * (1 + 0.5 * sube)
        trem = 0.7 + 0.3 * np.sin(2 * np.pi * (6 + 10 * sube) * t + k)
        acorde += np.sin(2 * np.pi * np.cumsum(fk) / SR) * trem * (0.22 / (1 + 0.4 * k)) * np.clip((t - 0.08 * k) / 0.2, 0, 1)
    cristales = pad(granos(rng, 160, 0.05, 1.0, 2500, 9000, 0.9, dur=(0.0006, 0.002), densidad_baja=False), n) * (0.3 + 0.7 * sube)
    ding = np.zeros(n)
    t0 = 0.98
    for fb, ab in ((2093, 0.35), (3136, 0.2), (4186, 0.12)):
        ding += np.sin(2 * np.pi * fb * t) * env(t, 0.002, 0.12, t0) * ab
    e = np.clip(t / 0.06, 0, 1) * np.clip((d - t) / 0.08, 0, 1)
    x = (zumbido * 0.6 + acorde + cristales) * e + ding
    return norm(x, 0.7)


def bola(rng):
    d = 0.9
    t = t_(d)
    n = len(t)
    f = 70 * np.exp(-t / 0.12) + 34
    golpe = np.sin(2 * np.pi * np.cumsum(f) / SR) * env(t, 0.003, 0.14) * 1.5
    clic = altos(rng.standard_normal(n), 1800) * env(t, 0.0005, 0.006) * 0.7
    rafaga = barrido(rng.standard_normal(n), 1800, 180, q=1.3) * env(t, 0.006, 0.16, 0.004) * 1.4
    soplo = banda(rng.standard_normal(n), 300, 1400) * env(t, 0.05, 0.25, 0.08) * 0.45
    nieve = pad(granos(rng, 140, 0.02, 0.45, 1500, 6500, 1.0), n) * 0.6
    x = sat(golpe + clic + rafaga + soplo + nieve, 1.8)
    return norm(x, 0.8)


def impacto(rng):
    d = 1.1
    t = t_(d)
    n = len(t)
    f = 55 * np.exp(-t / 0.1) + 30
    sordo = np.sin(2 * np.pi * np.cumsum(f) / SR) * env(t, 0.004, 0.18) * 1.3
    puf = bajos(rng.standard_normal(n), 700) * env(t, 0.004, 0.2) * 1.1
    nieve = pad(granos(rng, 260, 0.01, 0.8, 1200, 7000, 1.0), n) * 0.75
    copos = pad(granos(rng, 90, 0.2, 1.05, 4000, 10000, 0.6, dur=(0.0005, 0.0015), densidad_baja=False), n) * 0.4
    x = sat(sordo + puf + nieve, 1.5) + copos
    e = np.clip((d - t) / 0.2, 0, 1)
    return norm(x * e, 0.82)


def aspira(rng):
    d = 0.55
    t = t_(d)
    n = len(t)
    x = barrido(rng.standard_normal(n), 400, 3800, q=0.9) * np.clip(t / 0.4, 0, 1) ** 1.5
    x = x * np.clip((d - t) / 0.05, 0, 1)
    nieve = pad(granos(rng, 80, 0.05, 0.5, 1500, 6000, 0.8, densidad_baja=False), n) * np.clip(t / 0.5, 0, 1)
    pop = np.sin(2 * np.pi * 220 * t) * env(t, 0.002, 0.03, 0.47) * 0.4
    return norm(x * 0.9 + nieve * 0.6 + pop, 0.7)


def vacio(rng):
    d = 0.3
    t = t_(d)
    n = len(t)
    x = np.zeros(n)
    for t0, a in ((0.0, 1.0), (0.085, 0.75)):
        x += banda(rng.standard_normal(n), 1500, 5000) * env(t, 0.0004, 0.004, t0) * a
        x += np.sin(2 * np.pi * 900 * t) * env(t, 0.0005, 0.008, t0) * 0.3 * a
    x += banda(rng.standard_normal(n), 900, 3000) * env(t, 0.01, 0.06, 0.12) * 0.18
    return norm(x, 0.6)


def guardar(x, nombre):
    k = int(0.003 * SR)
    x = x.copy()
    x[:k] *= np.linspace(0, 1, k)
    x[-k:] *= np.linspace(1, 0, k)
    ruta = os.path.join(OUT, nombre + '.ogg')
    raw = ruta + '.raw'
    (np.clip(x, -1, 1) * 32767).astype('<i2').tofile(raw)
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-f', 's16le', '-ar', str(SR), '-ac', '1', '-i', raw, '-c:a', 'libvorbis', '-q:a', '5',
                    ruta], check=True)
    os.remove(raw)


if __name__ == '__main__':
    rng = np.random.default_rng(2611)
    for i, tono in enumerate((1.0, 0.93, 1.07, 0.97), 1):
        guardar(dispara(rng, tono), f'lanzanieves_dispara{i}')
    guardar(gira(rng), 'lanzanieves_gira')
    guardar(carga(rng), 'lanzanieves_carga')
    guardar(bola(rng), 'lanzanieves_bola')
    guardar(impacto(rng), 'lanzanieves_impacto')
    guardar(aspira(rng), 'lanzanieves_aspira')
    guardar(vacio(rng), 'lanzanieves_vacio')
    p = os.path.join(OUT, '..', 'sounds.json')
    d = json.load(open(p, encoding='utf-8'))
    d['lanzanieves_dispara'] = {'sounds': [f'navidad:lanzanieves_dispara{i}' for i in (1, 2, 3, 4)], 'subtitle': 'navidad.sub.lanzanieves'}
    d['lanzanieves_gira'] = {'sounds': ['navidad:lanzanieves_gira'], 'subtitle': 'navidad.sub.lanzanieves_gira'}
    d['lanzanieves_carga'] = {'sounds': ['navidad:lanzanieves_carga'], 'subtitle': 'navidad.sub.lanzanieves_carga'}
    d['lanzanieves_bola'] = {'sounds': ['navidad:lanzanieves_bola'], 'subtitle': 'navidad.sub.lanzanieves_bola'}
    d['lanzanieves_impacto'] = {'sounds': ['navidad:lanzanieves_impacto'], 'subtitle': 'navidad.sub.lanzanieves_impacto'}
    d['lanzanieves_aspira'] = {'sounds': ['navidad:lanzanieves_aspira'], 'subtitle': 'navidad.sub.lanzanieves_aspira'}
    d['lanzanieves_vacio'] = {'sounds': ['navidad:lanzanieves_vacio'], 'subtitle': 'navidad.sub.lanzanieves_vacio'}
    json.dump(d, open(p, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)
    print('sonidos del lanzanieves listos')
