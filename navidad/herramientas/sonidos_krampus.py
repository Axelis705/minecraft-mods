#!/usr/bin/env python3
"""Sonidos del Krampus renovado (sintetizados con numpy, salvo las risas, que salen de la grabación CC0 de Santa):
- krampus_cadenas1..3: cadenas que se sacuden (eslabones de metal que chocan, cada uno con sus parciales raros).
- krampus_cascabeles1..2: cascabeles malditos: campanitas viejas y desafinadas (tritono) con eco largo; avisan dónde anda en lo oscuro.
- krampus_latigo1..2: el varazo: zumbido del aire y el chasquido.
- krampus_saco: costal que se azota (golpe sordo y tela).
- krampus_pezuna1..3: pezuña en la nieve: crujido, golpe grave y un "toc".
- krampus_soplido: el soplido que apaga los faroles.
- krampus_apagon: todo se apaga: aire que se jala, golpe muy grave y un zumbido que se queda.
- krampus_risa1..2: "¡jo, jo, jo!" de Santa pero muy grave, distorsionado y con eco (el Krampus se burla).
- krampusillo_risa1..2: la misma risa muy aguda y rápida (risita de diablito); krampusillo_dolor: chillido corto.
- campanita: la campanita dorada de Santa (dos toques claros).
- gancho: la cadena que vuela (silbido metálico).
- krampus_apagon_musica: música de la tercera fase (48 s, en bucle): zumbido grave, latidos, viento y cascabeles a lo lejos.
- noche_krampus: el aviso de la Noche del Krampus (cadenas a lo lejos con viento y un cascabel).
Uso: python3 herramientas/sonidos_krampus.py  (necesita ffmpeg con rubberband)"""
import os
import subprocess
import sys

import numpy as np

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
import sonidos_lanzanieves as L  # noqa: E402
import sinte as T  # noqa: E402

SR = L.SR
OUT = L.OUT
t_, banda, bajos, altos, pad, sat, norm, guardar = L.t_, L.banda, L.bajos, L.altos, L.pad, L.sat, L.norm, L.guardar
RISA = os.path.join(AQUI, 'fuentes', 'santa_oh_oh_oh_4_bigsoundbank_cc0.wav')


def eslabon(rng, dur=0.18):
    """Un choque de eslabones: parciales inarmónicos agudos que se apagan rápido + un chasquido."""
    t = t_(dur)
    f0 = rng.uniform(1700, 3200)
    out = np.zeros(len(t))
    for r, a, d in ((1.0, 1.0, 0.05), (1.51, 0.7, 0.035), (2.33, 0.5, 0.025), (3.7, 0.35, 0.015), (5.1, 0.2, 0.01)):
        out += a * np.sin(2 * np.pi * f0 * r * t + rng.uniform(0, 6)) * np.exp(-t / d)
    out += altos(rng.standard_normal(len(t)), 3000) * np.exp(-t / 0.004) * 0.6
    return out


def cadenas(rng, dur=0.9, n=26):
    out = np.zeros(int(dur * SR))
    for _ in range(n):
        at = rng.beta(1.4, 2.2) * (dur - 0.2)
        e = eslabon(rng) * rng.uniform(0.25, 1.0)
        i = int(at * SR)
        out[i:i + len(e)] += e[:len(out) - i]
    # el roce del metal (ruido agudo suave)
    out += banda(rng.standard_normal(len(out)), 2500, 7000) * 0.05 * np.exp(-np.linspace(0, 3, len(out)))
    return norm(sat(out, 1.3), 0.66)


def cascabel_maldito(f, dur=2.4):
    t = t_(dur)
    out = np.zeros(len(t))
    for r, a, d in ((1, 1.0, 0.9), (2.41, 0.5, 0.5), (4.13, 0.3, 0.25), (5.97, 0.2, 0.12)):
        out += a * np.sin(2 * np.pi * f * r * t) * np.exp(-t / d)
    out *= np.minimum(1, t / 0.002)
    return out


def cascabeles(rng, base=740):
    out = np.zeros(int(2.8 * SR))
    for k in range(7):
        f = base * (1 if k % 2 == 0 else 2 ** (6 / 12)) * rng.uniform(0.985, 1.015)   # tritono, desafinado
        e = cascabel_maldito(f) * rng.uniform(0.4, 1.0)
        i = int((k * 0.16 + rng.uniform(0, 0.03)) * SR)
        out[i:i + len(e)] += e[:len(out) - i]
    out = T.reverb(out, 0.5) if hasattr(T, 'reverb') else out
    return norm(out, 0.75)


def latigo(rng):
    t = t_(0.5)
    ruido = rng.standard_normal(len(t))
    swish = L.barrido(ruido, 400, 3500, 0.8) * np.exp(-((t - 0.16) / 0.07) ** 2) * 0.8
    crack = altos(rng.standard_normal(len(t)), 1800) * np.exp(-np.maximum(0, t - 0.2) / 0.012) * (t >= 0.2) * 1.6
    golpe = np.sin(2 * np.pi * 120 * t) * np.exp(-np.maximum(0, t - 0.2) / 0.03) * (t >= 0.2) * 0.4
    return norm(sat(swish + crack + golpe, 1.8), 0.7)


def saco(rng):
    t = t_(0.6)
    golpe = np.sin(2 * np.pi * 70 * t * (1 + 0.4 * np.exp(-t / 0.03))) * np.exp(-t / 0.09)
    tela = banda(rng.standard_normal(len(t)), 500, 3500) * np.exp(-t / 0.12) * 0.5
    roce = banda(rng.standard_normal(len(t)), 1500, 6000) * np.exp(-((t - 0.25) / 0.08) ** 2) * 0.25
    return norm(golpe + tela + roce, 0.85)


def pezuna(rng, tono=1.0):
    t = t_(0.35)
    crujido = np.zeros(len(t))
    for _ in range(40):
        i = int(rng.uniform(0, 0.09) * SR)
        g = altos(rng.standard_normal(200), 1500) * np.exp(-np.arange(200) / 30)
        crujido[i:i + 200] += g * rng.uniform(0.2, 0.7)
    golpe = np.sin(2 * np.pi * 85 * tono * t) * np.exp(-t / 0.05) * 0.9
    toc = banda(rng.standard_normal(len(t)), 700 * tono, 1600 * tono) * np.exp(-t / 0.015) * 0.6
    return norm(crujido + golpe + toc, 0.8)


def soplido(rng):
    t = t_(1.2)
    aire = bajos(rng.standard_normal(len(t)), 1400) * np.minimum(1, t / 0.15) * np.exp(-np.maximum(0, t - 0.4) / 0.25)
    silbo = L.barrido(rng.standard_normal(len(t)), 900, 300, 0.5) * 0.4 * np.exp(-t / 0.5)
    return norm(aire + silbo, 0.75)


def apagon(rng):
    t = t_(3.2)
    jalar = L.barrido(rng.standard_normal(len(t)), 200, 2500, 0.9) * np.clip(t / 0.5, 0, 1) * (t < 0.55) * 0.6
    boom = np.sin(2 * np.pi * np.cumsum(45 * (1 + 1.5 * np.exp(-np.maximum(0, t - 0.55) / 0.05))) / SR) * \
        np.exp(-np.maximum(0, t - 0.55) / 0.6) * (t >= 0.55)
    zumbido = (np.sin(2 * np.pi * 55 * t) + 0.5 * np.sin(2 * np.pi * 82.4 * t)) * 0.25 * np.exp(-np.maximum(0, t - 0.6) / 1.4) * (t >= 0.55)
    return norm(sat(jalar + boom + zumbido, 1.4), 0.9)


def campanita(rng):
    out = np.zeros(int(2.4 * SR))
    for k, f in enumerate((1568, 2093)):
        e = T.bell(f, 2.0, 0.5)
        i = int(k * 0.22 * SR)
        out[i:i + len(e)] += e[:len(out) - i]
    return norm(out, 0.8)


def gancho(rng):
    t = t_(0.6)
    silbo = L.barrido(rng.standard_normal(len(t)), 600, 2400, 0.4) * np.sin(np.pi * np.clip(t / 0.6, 0, 1)) * 0.5
    out = silbo.copy()
    for k in range(6):
        e = eslabon(rng, 0.12) * 0.3
        i = int((0.05 + k * 0.08) * SR)
        out[i:i + len(e)] += e[:len(out) - i]
    return norm(out, 0.8)


def dolor(rng):
    t = t_(0.35)
    f = 1400 * np.exp(-t / 0.25) + 500
    out = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.15) + 0.2 * banda(rng.standard_normal(len(t)), 1500, 5000) * np.exp(-t / 0.05)
    return norm(sat(out, 2.0), 0.7)


def musica_apagon(rng, dur=48.0):
    """Tercera fase: zumbido grave que respira, latidos, viento y cascabeles a lo lejos (en bucle sin corte)."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for f, a in ((41.2, 0.5), (55.0, 0.35), (58.3, 0.25), (82.4, 0.15)):            # mi y fa: roce oscuro
        out += a * T.saw(f, n, 8, 0.0) if hasattr(T, 'saw') else a * np.sin(2 * np.pi * f * t)
    out = bajos(out, 220) * (0.75 + 0.25 * np.sin(2 * np.pi * t / 8))
    latido = np.zeros(n)
    k = 0.0
    while k < dur:
        for d, a in ((0.0, 1.0), (0.28, 0.7)):
            i = int((k + d) * SR)
            m = min(n - i, int(0.3 * SR))
            if m > 0:
                tt = np.arange(m) / SR
                latido[i:i + m] += a * np.sin(2 * np.pi * 50 * tt) * np.exp(-tt / 0.07)
        k += 1.2
    viento = L.barrido(rng.standard_normal(n), 300, 900, 0.6, 96) * 0.25 * (0.6 + 0.4 * np.sin(2 * np.pi * t / 12 + 1))
    lejos = np.zeros(n)
    for at in (5.0, 17.5, 29.0, 41.0):
        e = cascabeles(rng, 520) * 0.35
        i = int(at * SR)
        lejos[i:i + len(e)] += e[:n - i]
    lejos = bajos(lejos, 2500)
    mezcla = out * 0.55 + latido * 0.6 + viento + lejos
    # bucle sin corte: el final se mezcla con el principio
    f = int(1.5 * SR)
    mezcla[:f] = mezcla[:f] * np.linspace(0, 1, f) + mezcla[-f:] * np.linspace(1, 0, f)
    return norm(mezcla[:n - f], 0.8)


def noche(rng):
    a = cadenas(rng, 1.6, 34)
    v = L.barrido(rng.standard_normal(len(a)), 250, 700, 0.6) * 0.35
    out = bajos(a, 3000) * 0.7 + v
    e = cascabeles(rng, 600)[:len(out)] * 0.3
    out[:len(e)] += e
    return norm(T.reverb(out, 0.45) if hasattr(T, 'reverb') else out, 0.8)


def risa(nombre, tono, tempo, extra):
    filtros = [f'rubberband=pitch={tono}:tempo={tempo}'] + extra + [
        'loudnorm=I=-13:TP=-1.2:LRA=9', 'afade=t=in:d=0.02', 'areverse,afade=t=in:d=0.15,areverse']
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', RISA, '-af', ','.join(filtros), '-ac', '1', '-ar', '44100',
                    '-c:a', 'libvorbis', '-q:a', '5', os.path.join(OUT, nombre + '.ogg')], check=True)


if __name__ == '__main__':
    rng = np.random.default_rng(512)
    for i in range(1, 4):
        guardar(cadenas(rng), f'krampus_cadenas{i}')
        guardar(pezuna(rng, (1.0, 0.92, 1.08)[i - 1]), f'krampus_pezuna{i}')
    for i in range(1, 3):
        guardar(cascabeles(rng, (740, 660)[i - 1]), f'krampus_cascabeles{i}')
        guardar(latigo(rng), f'krampus_latigo{i}')
    guardar(saco(rng), 'krampus_saco')
    guardar(soplido(rng), 'krampus_soplido')
    guardar(apagon(rng), 'krampus_apagon')
    guardar(campanita(rng), 'campanita')
    guardar(gancho(rng), 'gancho')
    guardar(dolor(rng), 'krampusillo_dolor')
    guardar(noche(rng), 'noche_krampus')
    guardar(musica_apagon(rng), 'krampus_apagon_musica')
    demonio = ['highpass=f=40', 'equalizer=f=90:t=q:w=1:g=6', 'acrusher=bits=10:mode=log:aa=1:mix=0.25',
               'aecho=0.8:0.7:90|170|260:0.4|0.3|0.2', 'lowpass=f=5000']
    risa('krampus_risa1', 0.58, 0.85, demonio)
    risa('krampus_risa2', 0.52, 0.8, demonio)
    chiquito = ['highpass=f=200', 'aecho=0.8:0.4:30:0.2']
    risa('krampusillo_risa1', 1.9, 1.5, chiquito)
    risa('krampusillo_risa2', 2.1, 1.7, chiquito)
    print('sonidos del Krampus listos')
