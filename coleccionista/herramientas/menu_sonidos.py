#!/usr/bin/env python3
"""La música y los sonidos de la pantalla de inicio del Coleccionista, hechos a mano (sin descargar nada):

- musica.ogg    un vals de caja de música en la menor (melodía propia, 16 compases dos veces): se le da cuerda, toca, la segunda vuelta
                una octava arriba y al final se le acaba la cuerda (se va haciendo lenta). Debajo, un zumbido grave y el crujir de un
                disco viejo. ~1.5 min; el juego la repite.
- encima.ogg    al pasar el ratón por un botón: una nota suelta de caja de música, muy bajita
- clic.ogg      al apretar un botón: un golpe sordo de madera con un chispazo de estática
- estatica.ogg  al abrir la pantalla: la estática de una tele que se prende

Uso: python3 herramientas/menu_sonidos.py  (necesita ffmpeg con libvorbis)
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

SND = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista', 'sounds', 'menu')
R = np.random.RandomState(2611)


def guardar(nombre, x, calidad=6):
    os.makedirs(SND, exist_ok=True)
    x = np.clip(x, -1, 1)
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, 'a.wav')
        with wave.open(w, 'wb') as f:
            f.setnchannels(1)
            f.setsampwidth(2)
            f.setframerate(SR)
            f.writeframes((x * 32767).astype(np.int16).tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', w, '-c:a', 'libvorbis', '-q:a', str(calidad),
                        os.path.join(SND, nombre + '.ogg')], check=True)
    print(nombre, f'{len(x) / SR:.2f} s')


def hz(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def lengueta(f, seg=2.2, fuerza=1.0):
    """Una lengüeta de caja de música (como las del piano gigante, un poco más larga)."""
    t = t_(seg)
    f = f * (1 + R.uniform(-0.004, 0.004))
    x = np.sin(2 * np.pi * f * t) * np.exp(-t * 2.2)
    x += 0.32 * np.sin(2 * np.pi * f * 2.0 * t) * np.exp(-t * 4.5)
    x += 0.2 * np.sin(2 * np.pi * f * 5.4 * t) * np.exp(-t * 13)
    x += 0.1 * np.sin(2 * np.pi * f * 8.9 * t) * np.exp(-t * 24)
    n = int(0.005 * SR)
    golpe = R.randn(n) * np.exp(-np.arange(n) / (0.0012 * SR))
    x[:n] += 0.45 * hp(golpe, 3500)
    return x * np.minimum(1, t / 0.002) * fuerza


# la melodía (midi, tiempos) y la armonía de cada compás (raíz del bajo y las dos notas del acorde)
MELODIA = [
    [(76, 1), (69, 1), (72, 1)], [(71, 1.5), (69, .5), (68, 1)], [(69, 1), (72, 1), (76, 1)], [(74, 3)],
    [(77, 1), (76, 1), (74, 1)], [(72, 1.5), (71, .5), (69, 1)], [(68, 1), (71, 1), (76, 1)], [(69, 3)],
    [(72, 1), (76, 1), (81, 1)], [(80, 1.5), (77, .5), (76, 1)], [(74, 1), (77, 1), (81, 1)], [(76, 3)],
    [(77, 1), (74, 1), (71, 1)], [(72, 1), (69, 1), (64, 1)], [(68, 1), (71, 1), (74, 1)], [(69, 3)],
]
ACORDES = {'Am': (45, 60, 64), 'E': (40, 59, 64), 'Dm': (50, 62, 65)}
ARMONIA = ['Am', 'E', 'Am', 'Dm', 'Dm', 'Am', 'E', 'Am', 'Am', 'E', 'Dm', 'Am', 'Dm', 'Am', 'E', 'Am']


def poner(pista, ini, x):
    i = int(ini * SR)
    if i >= len(pista):
        return
    n = min(len(x), len(pista) - i)
    pista[i:i + n] += x[:n]


def cuerda(seg):
    """Se le da cuerda a la caja: el trinquete hace clic-clic-clic."""
    t = t_(seg)
    x = np.zeros_like(t)
    k = 0.0
    while k < seg - 0.1:
        n = int(0.012 * SR)
        c = R.randn(n) * np.exp(-np.arange(n) / (0.0018 * SR))
        poner(x, k, bp(c, 1500, 7000) * R.uniform(0.5, 0.9))
        k += R.uniform(0.07, 0.11)
    return x


def musica():
    beat0 = 60 / 66.0
    total = 2 * 16 * 3 * beat0 * 1.35 + 9
    largo = int(total * SR)
    mel = np.zeros(largo)
    acomp = np.zeros(largo)
    poner(mel, 0.2, 0.6 * cuerda(2.2))
    ahora = 3.0
    for vuelta in range(2):
        for c, compas in enumerate(MELODIA):
            # al final de la segunda vuelta se acaba la cuerda: cada compás un poco más lento
            lento = 1.0 if vuelta == 0 or c < 11 else 1.0 + 0.07 * (c - 10) ** 1.5
            beat = beat0 * lento
            raiz, a, b = ACORDES[ARMONIA[c]]
            fuerza = 0.75 if vuelta == 0 else 0.6
            poner(acomp, ahora, lengueta(hz(raiz), 2.6, 0.55))
            if not (vuelta == 1 and c >= 14):
                poner(acomp, ahora + beat, lengueta(hz(a), 1.6, 0.25))
                poner(acomp, ahora + 2 * beat, lengueta(hz(b), 1.6, 0.22))
            k = 0.0
            for (m, d) in compas:
                nota = m + (12 if vuelta == 1 else 0)
                # un pequeño "tropiezo" de la caja de vez en cuando (llega tarde la nota)
                tarde = R.uniform(0.0, 0.025) + (0.05 if R.rand() < 0.08 else 0.0)
                poner(mel, ahora + k * beat + tarde, lengueta(hz(nota), 2.4, fuerza * (1.0 if k == 0 else 0.85)))
                k += d
            ahora += 3 * beat
    # la última nota, sola y grave, y el resorte que se suelta
    poner(acomp, ahora + 0.4, lengueta(hz(45), 4.0, 0.7))
    poner(mel, ahora + 0.4, lengueta(hz(69), 4.0, 0.5))
    fin = ahora + 5.5
    largo = int(fin * SR)
    mel, acomp = mel[:largo], acomp[:largo]
    t = t_(fin)[:largo]
    # un vaivén de cinta vieja (la afinación sube y baja un poquito): se aplica remuestreando
    wow = 1 + 0.0035 * np.sin(2 * np.pi * 0.37 * t) + 0.0015 * np.sin(2 * np.pi * 2.9 * t)
    pos = np.cumsum(wow) - wow[0]
    pos = np.clip(pos, 0, largo - 1)
    caja = np.interp(pos, np.arange(largo), mel + acomp)
    caja = eco(caja, seg=2.4, mezcla=0.38, oscuro=3800)[:largo]
    # el zumbido grave (la y mi muy abajo, que respiran) y el disco viejo
    dron = (np.sin(2 * np.pi * 55 * t) + 0.6 * np.sin(2 * np.pi * 82.4 * t + 1) + 0.3 * np.sin(2 * np.pi * 110.3 * t))
    dron *= 0.5 + 0.5 * np.sin(2 * np.pi * t / 11.0) ** 2
    dron = lp(dron + 0.4 * lp(R.randn(largo), 180), 260)
    disco = np.zeros(largo)
    for _ in range(int(fin * 9)):
        i = R.randint(0, largo - 200)
        disco[i:i + 40] += R.randn(40) * np.exp(-np.arange(40) / 8) * R.uniform(0.2, 1)
    disco = hp(disco, 1800) * 0.5 + hp(lp(R.randn(largo), 6000), 2500) * 0.012
    x = normal(caja, 0.8) + 0.16 * normal(dron, 1) + 0.25 * disco
    # entra y sale suave (así el juego la puede repetir sin golpe)
    x *= np.minimum(1, t / 1.5) * np.minimum(1, (fin - t) / 3.0)
    guardar('musica', normal(x, 0.85), calidad=5)


def encima():
    x = lengueta(hz(88), 1.2, 1.0)
    guardar('encima', normal(eco(x, seg=0.9, mezcla=0.3, oscuro=5000), 0.5))


def clic():
    t = t_(0.7)
    golpe = np.sin(2 * np.pi * 95 * t) * np.exp(-t * 28) + 0.6 * np.sin(2 * np.pi * 210 * t) * np.exp(-t * 40)
    madera = bp(R.randn(len(t)) * np.exp(-t * 60), 300, 2500)
    est = bp(R.randn(len(t)), 1200, 9000) * np.exp(-t * 14) * (np.sin(2 * np.pi * 60 * t) > -0.3)
    x = 1.4 * golpe + 0.7 * madera + 0.35 * est
    guardar('clic', normal(eco(x, seg=0.8, mezcla=0.2, oscuro=3000), 0.8))


def estatica():
    seg = 1.6
    t = t_(seg)
    ruido = bp(R.randn(len(t)), 600, 11000)
    # sube de golpe, tiembla (como la señal que agarra) y se apaga con un zumbido
    env = np.minimum(1, t / 0.03) * np.exp(-np.maximum(0, t - 0.5) * 4)
    temblor = 0.6 + 0.4 * (np.sin(2 * np.pi * 13 * t) > 0)
    zum = np.sin(2 * np.pi * 60 * t) + 0.5 * np.sin(2 * np.pi * 180 * t)
    x = ruido * env * temblor + 0.25 * zum * np.exp(-t * 2.5) * np.minimum(1, t / 0.1)
    guardar('estatica', normal(x, 0.7))


def main():
    musica()
    encima()
    clic()
    estatica()


if __name__ == '__main__':
    main()
