#!/usr/bin/env python3
"""La risa de Santa ("¡Jo, jo, jo!") con voz sintetizada a la manera de un sintetizador de formantes (tipo Klatt):

* fuente de voz: pulsos glotales con su caída natural de agudos, un poquito de temblor (jitter y shimmer) y algo de aire;
* cada "jo" empieza con el soplido de la "j" (ruido que ya suena a "o" porque pasa por los mismos formantes) y entra la voz;
* los formantes de la "o" en cascada (F1 ~ 480, F2 ~ 820, F3 ~ 2500 Hz) que se cierran un poquito hacia "u" al final;
* un resonador grave de pecho (panza de Santa) y una sala chiquita.
Tres variantes (risa_santa1..3) para que no suene siempre igual. Uso: python3 herramientas/voz_santa.py"""
import os
import subprocess

import numpy as np
from scipy.signal import lfilter

SR = 44100
AQUI = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad', 'sounds')


def resonador(x, f, bw):
    """Resonador de dos polos (como en Klatt), con ganancia 1 en 0 Hz. f y bw pueden ser arreglos (varían en el tiempo)."""
    f = np.broadcast_to(np.asarray(f, float), x.shape)
    bw = np.broadcast_to(np.asarray(bw, float), x.shape)
    # por bloques de 64 muestras (los formantes cambian despacio)
    y = np.zeros_like(x)
    zi = np.zeros(2)
    for i in range(0, len(x), 64):
        fc, b = f[i], bw[i]
        r = np.exp(-np.pi * b / SR)
        c = -r * r
        bb = 2 * r * np.cos(2 * np.pi * fc / SR)
        a = 1 - bb - c
        seg, zi = lfilter([a], [1, -bb, -c], x[i:i + 64], zi=zi)
        y[i:i + 64] = seg
    return y


def glotal(f0, rng, jitter=0.012, shimmer=0.05):
    """Pulsos glotales (derivada del flujo, forma de Rosenberg) siguiendo el tono f0 (arreglo por muestra)."""
    n = len(f0)
    out = np.zeros(n)
    t = 0.0
    while t < n - 1:
        i = int(t)
        per = SR / max(40.0, f0[i]) * (1 + jitter * rng.standard_normal())
        abierto = 0.62 * per
        tp = 0.7 * abierto
        k = np.arange(int(abierto))
        flujo = np.where(k < tp, 0.5 * (1 - np.cos(np.pi * k / tp)), np.cos(np.pi * (k - tp) / (2 * (abierto - tp))))
        d = np.diff(np.concatenate([[0.0], flujo, [0.0]]))
        amp = 1 + shimmer * rng.standard_normal()
        fin = min(n, i + len(d))
        out[i:fin] += d[:fin - i] * amp
        t += per
    return out


def jo(f_ini, f_pico, f_fin, dur, rng, soplo=0.075, fuerza=1.0, hacia_u=0.35):
    """Un "jo": soplido de la j, la o con su tono que sube un poquito y cae, y se cierra un poco hacia u."""
    n = int((soplo + dur) * SR)
    t = np.arange(n) / SR
    tv = np.clip((t - soplo) / dur, 0, 1)
    # tono: sube al principio y cae al final
    f0 = np.where(tv < 0.2, f_ini + (f_pico - f_ini) * (tv / 0.2), f_pico + (f_fin - f_pico) * np.clip((tv - 0.2) / 0.8, 0, 1) ** 1.3)
    voz = glotal(f0, rng)
    # envolventes: la voz entra después del soplido y se apaga al final
    ev = np.clip((t - soplo * 0.75) / 0.03, 0, 1) * np.clip((dur + soplo - t) / (dur * 0.45), 0, 1) ** 1.4
    aire = rng.standard_normal(n)
    eh = np.clip(t / 0.02, 0, 1) * np.clip((soplo + 0.02 - t) / 0.04, 0, 1)
    fuente = voz * ev * fuerza + aire * (eh * 0.09 + ev * 0.012)
    # formantes de la "o" que se cierran hacia "u"
    f1 = 480 - 90 * hacia_u * tv
    f2 = 820 - 120 * hacia_u * tv
    x = resonador(fuente, f1, 80)
    x = resonador(x, f2, 95)
    x = resonador(x, 2500, 160)
    x = resonador(x, 3400, 250) * 0.6 + x * 0.4
    # el pecho (resonancia grave) y quitar el zumbido de abajo
    pecho = resonador(fuente * 0.5, 210, 90)
    x = x + pecho * 0.35
    x = x - lfilter([0.02], [1, -0.98], x)
    return x


def sala(x, wet=0.16):
    out = x.copy()
    for d, g in ((0.031, 0.32), (0.047, 0.26), (0.071, 0.2), (0.097, 0.15), (0.13, 0.1)):
        k = int(d * SR)
        out[k:] += x[:-k] * g * wet * 3
    return out


def risa(variante):
    rng = np.random.default_rng(100 + variante)
    planes = {
        1: [(112, 124, 104, 0.24), (106, 118, 98, 0.23), (100, 112, 84, 0.46)],
        2: [(120, 132, 110, 0.2), (114, 126, 104, 0.2), (108, 120, 100, 0.2), (98, 110, 80, 0.5)],
        3: [(104, 116, 98, 0.26), (100, 112, 94, 0.26), (94, 106, 78, 0.55)],
    }[variante]
    partes = []
    for i, (a, b, c, d) in enumerate(planes):
        partes.append(jo(a, b, c, d, rng, fuerza=1.0 if i < len(planes) - 1 else 1.1, hacia_u=0.25 + 0.15 * i))
        partes.append(np.zeros(int((0.07 + 0.02 * rng.random()) * SR)))
    x = np.concatenate(partes)
    x = sala(x)
    # compresión suave para que suene llena
    x = np.tanh(x / np.max(np.abs(x)) * 1.6)
    return x / np.max(np.abs(x)) * 0.9


def guardar(x, ruta):
    k = int(0.004 * SR)
    x = x.copy()
    x[:k] *= np.linspace(0, 1, k)
    x[-k:] *= np.linspace(1, 0, k)
    raw = ruta + '.raw'
    (np.clip(x, -1, 1) * 32767).astype('<i2').tofile(raw)
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-f', 's16le', '-ar', str(SR), '-ac', '1', '-i', raw, '-c:a', 'libvorbis',
                    '-q:a', '4', ruta], check=True)
    os.remove(raw)


if __name__ == '__main__':
    import sys
    destino = sys.argv[1] if len(sys.argv) > 1 else OUT
    for v in (1, 2, 3):
        guardar(risa(v), os.path.join(destino, f'risa_santa{v}.ogg'))
    print('risa de Santa lista (3 variantes)')
