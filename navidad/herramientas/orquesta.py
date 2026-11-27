#!/usr/bin/env python3
"""Villancicos de Navidad de Axelis con orquesta navideña (versión 2, pedida por Axelis: "que no suene solo a cajita").
Los cuatro villancicos son de dominio público (melodías y armonías de musica.py) y aquí se arreglan con instrumentos sintetizados:
piano, cuerdas, flauta, clarinete, cornos, contrabajo en pizzicato, arpa, glockenspiel, coro, cascabeles, escobillas y timbales.

Salidas (todas mono, para que el tocadiscos y la caja de música se oigan desde donde están):
 - villancico_campanas / _navidad / _noche_paz / _adornen: los discos (la versión con orquesta).
 - caja_campanas / _navidad / _noche_paz / _adornen: la versión de caja de música (la de antes), para la Caja de música navideña.
Escribe además src/main/java/com/navidad/registry/Musica.java con cuánto dura cada pieza (en ticks).
Uso: python3 herramientas/orquesta.py   (tarda unos minutos)"""
import os
import subprocess
import sys

import numpy as np
from scipy.signal import butter, sosfilt

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
import musica as M  # noqa: E402
import sinte as T  # noqa: E402

SR = T.SR
note = T.note
rng = np.random.default_rng(2512)
SONIDOS = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad', 'sounds')
JAVA = os.path.join(AQUI, '..', 'src', 'main', 'java', 'com', 'navidad', 'registry', 'Musica.java')


def lp(x, corte, orden=2):
    return sosfilt(butter(orden, min(corte, SR / 2 - 200) / (SR / 2), output='sos'), x)


def bp(x, lo, hi, orden=2):
    return sosfilt(butter(orden, [lo / (SR / 2), min(hi, SR / 2 - 200) / (SR / 2)], btype='band', output='sos'), x)


def tt(seg):
    return np.arange(int(seg * SR)) / SR


def soltar(t, dur, rel):
    """1 mientras suena; después se apaga con la constante rel."""
    return np.where(t < dur, 1.0, np.exp(-(t - dur) / rel))


# -------------------------------------------------------------------------------------------- instrumentos

def piano(f, dur, vol=0.1):
    t = tt(dur + 1.0)
    out = np.zeros(len(t))
    base = np.clip(2.6 * (261.6 / f) ** 0.55, 0.5, 4.5)
    B = 0.00035
    for k in range(1, 16):
        fk = f * k * np.sqrt(1 + B * k * k)
        if fk > 9000:
            break
        a = 1 / k ** 1.15 * (1.0 if k < 4 else 0.7)
        dec = base / (1 + 0.45 * (k - 1))
        for det in (-0.0007, 0.0007):
            out += a * 0.5 * np.sin(2 * np.pi * fk * (1 + det) * t + rng.random() * 6) * np.exp(-t / dec)
    martillo = lp(rng.standard_normal(len(t)), min(6000, f * 8)) * np.exp(-t / 0.004) * 0.25
    out = (out + martillo) * np.minimum(1, t / 0.002) * soltar(t, dur, 0.16)
    return out * vol


def cuerdas(freqs, dur, vol=0.08, ataque=0.22, brillo=2400):
    t = tt(dur + 0.6)
    out = np.zeros(len(t))
    for f in freqs:
        vib = 1 + 0.0028 * np.sin(2 * np.pi * 5.3 * t + rng.random() * 6) * np.clip((t - 0.25) / 0.4, 0, 1)
        for cents in (-9, -3, 3, 9):
            fr = f * 2 ** (cents / 1200)
            fase = 2 * np.pi * fr * np.cumsum(vib) / SR + rng.random() * 6
            for k in range(1, 14):
                if fr * k > 7000:
                    break
                out += np.sin(k * fase) / k * 0.25
    out = lp(out, brillo)
    e = np.clip(t / ataque, 0, 1) * soltar(t, dur, 0.35)
    return out * e * vol / max(1, len(freqs)) ** 0.6


def flauta(f, dur, vol=0.08):
    t = tt(dur + 0.25)
    vib = 1 + 0.004 * np.sin(2 * np.pi * 5.0 * t) * np.clip((t - 0.18) / 0.3, 0, 1)
    caida = 1 - 0.01 * np.exp(-t / 0.03)            # entra un poquito abajo y sube
    fase = 2 * np.pi * f * np.cumsum(vib * caida) / SR
    out = np.sin(fase) + 0.22 * np.sin(2 * fase) + 0.07 * np.sin(3 * fase)
    aire = bp(rng.standard_normal(len(t)), f * 0.9, f * 3.5) * 0.06 * (0.4 + 0.6 * np.exp(-t / 0.08))
    e = np.clip(t / 0.05, 0, 1) * soltar(t, dur, 0.08)
    return (out + aire) * e * vol


def clarinete(f, dur, vol=0.07):
    t = tt(dur + 0.2)
    vib = 1 + 0.002 * np.sin(2 * np.pi * 4.6 * t) * np.clip((t - 0.3) / 0.4, 0, 1)
    fase = 2 * np.pi * f * np.cumsum(vib) / SR
    out = np.zeros(len(t))
    for k, a in ((1, 1), (3, 0.5), (5, 0.28), (7, 0.15), (9, 0.08), (2, 0.06), (4, 0.04)):
        if f * k < 8000:
            out += a * np.sin(k * fase)
    out = lp(out, 3500)
    e = np.clip(t / 0.035, 0, 1) * soltar(t, dur, 0.07)
    return out * e * vol


def corno(f, dur, vol=0.07):
    """Corno francés / metales suaves: se abre (más brillo) en el ataque."""
    t = tt(dur + 0.25)
    vib = 1 + 0.0025 * np.sin(2 * np.pi * 5.2 * t) * np.clip((t - 0.3) / 0.4, 0, 1)
    fase = 2 * np.pi * f * np.cumsum(vib) / SR
    out = np.zeros(len(t))
    for k in range(1, 12):
        if f * k > 6000:
            break
        out += np.sin(k * fase) / k ** 0.9
    brillo = 900 + 1600 * np.exp(-t / 0.12) + 500
    # pasa-bajos que cambia (por bloques)
    y = np.zeros(len(t))
    for i in range(0, len(t), 2048):
        seg = out[max(0, i - 1024):i + 2048]
        z = lp(seg, brillo[min(len(t) - 1, i)])
        y[i:i + 2048] = z[i - max(0, i - 1024):][:len(y[i:i + 2048])]
    e = np.clip(t / 0.06, 0, 1) * soltar(t, dur, 0.1)
    return y * e * vol


def pizz(f, dur=0.6, vol=0.12):
    """Contrabajo en pizzicato."""
    t = tt(0.9)
    out = np.zeros(len(t))
    for k, a in ((1, 1), (2, 0.5), (3, 0.25), (4, 0.12)):
        out += a * np.sin(2 * np.pi * f * k * t) * np.exp(-t / (0.45 / k ** 0.6))
    golpe = lp(rng.standard_normal(len(t)), 400) * np.exp(-t / 0.01) * 0.3
    return (out + golpe) * np.minimum(1, t / 0.003) * vol


def arpa(f, dur=1.5, vol=0.06):
    t = tt(dur + 0.3)
    out = np.zeros(len(t))
    for k, a in ((1, 1), (2, 0.35), (3, 0.18), (4, 0.08)):
        out += a * np.sin(2 * np.pi * f * k * t + rng.random() * 6) * np.exp(-t / (1.3 / k))
    return out * np.minimum(1, t / 0.002) * vol


def glock(f, dur=1.2, vol=0.05):
    t = tt(dur)
    out = np.zeros(len(t))
    for r, a, d in ((1, 1, 0.9), (2.76, 0.32, 0.25), (5.4, 0.12, 0.08)):
        out += a * np.sin(2 * np.pi * f * r * t) * np.exp(-t / d)
    return out * np.minimum(1, t / 0.0008) * vol


def escobilla(vol=0.05):
    t = tt(0.25)
    return bp(rng.standard_normal(len(t)), 1200, 7000) * np.exp(-t / 0.07) * vol


def bombo(vol=0.2):
    t = tt(0.5)
    f = 55 + 60 * np.exp(-t / 0.03)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.18) * vol


# -------------------------------------------------------------------------------------------- mezcla

class Mezcla:
    def __init__(self, seg):
        self.n = int((seg + 6) * SR)
        self.seco = np.zeros(self.n)
        self.envio = np.zeros(self.n)

    def add(self, sig, en, rev=0.3):
        i = int(en * SR)
        j = min(i + len(sig), self.n)
        if j > i:
            self.seco[i:j] += sig[:j - i]
            self.envio[i:j] += sig[:j - i] * rev

    def render(self, largo):
        sala = T.reverb(self.envio, 1.0)
        out = self.seco + sala * 0.55
        fin = int((largo + 2.5) * SR)
        out = out[:fin]
        k = int(1.5 * SR)
        out[-k:] *= np.linspace(1, 0, k) ** 2
        return out / (np.max(np.abs(out)) + 1e-9) * 0.88


def linea(mz, seq, t0, beat, inst, oct=0, vol=0.08, legato=0.95, rev=0.3):
    t = t0
    for m, d in seq:
        if m is not None:
            mz.add(inst(note(m + 12 * oct), max(0.08, d * beat * legato), vol), t, rev)
        t += d * beat
    return t


def tercera_abajo(seq):
    """Una segunda voz sencilla: la melodía una tercera abajo (dentro de la escala mayor de Do/Sol/Fa da casi siempre bien)."""
    escala = [0, 2, 4, 5, 7, 9, 11]
    out = []
    for m, d in seq:
        if m is None:
            out.append((None, d))
            continue
        pc = m % 12
        if pc in escala:
            i = escala.index(pc)
            j = (i - 2) % 7
            baja = (pc - escala[j]) % 12
            out.append((m - baja, d))
        else:
            out.append((m - 3, d))
    return out


def triada(bajo, ch):
    """Del (bajo, [3a, 5a]) de musica.py: la tríada en registro medio."""
    return sorted(set([x for x in ch] + [bajo + 24 if bajo + 24 < ch[0] else bajo + 12]))


def acompanar(mz, prog, t0, compas, beat, estilo, vol=1.0, piano_on=True, pizz_on=True, cuerdas_on=False, campanitas=False):
    for i, (b, ch) in enumerate(prog):
        t = t0 + i * compas
        tri = triada(b, ch)
        if pizz_on:
            mz.add(pizz(note(b), vol=0.13 * vol), t, 0.15)
            if estilo == '4':
                mz.add(pizz(note(b + 7), vol=0.1 * vol), t + 2 * beat, 0.15)
        if piano_on:
            golpes = (1, 3) if estilo == '4' else (1, 2) if estilo == 'vals' else ()
            for k in golpes:
                for x in tri:
                    mz.add(piano(note(x), beat * 0.6, 0.035 * vol), t + k * beat, 0.25)
        if cuerdas_on:
            mz.add(cuerdas([note(x) for x in tri], compas * 0.98, 0.06 * vol), t, 0.45)
        if campanitas:
            mz.add(glock(note(tri[-1] + 12), 1.0, 0.025 * vol), t, 0.4)


# -------------------------------------------------------------------------------------------- piezas

def jingle():
    beat = 60 / 150
    compas = beat * 4
    seg = compas * 64 + compas * 2
    mz = Mezcla(seg)
    t = 0.0
    # introducción: cascabeles solos un compás
    for k in range(4):
        mz.add(M.sonaja(0.12, 0.05), t + k * beat, 0.2)
    t += compas
    for vuelta in range(2):
        # verso
        acompanar(mz, M.P_VERSO, t, compas, beat, '4', 0.9, cuerdas_on=vuelta == 1)
        if vuelta == 0:
            linea(mz, M.VERSO, t, beat, flauta, 0, 0.085)
        else:
            linea(mz, M.VERSO, t, beat, clarinete, 0, 0.08)
            linea(mz, M.VERSO, t, beat, flauta, 1, 0.04)
        for k in range(16):
            mz.add(M.sonaja(0.1, 0.022), t + (k * 4 + 1) * beat, 0.2)
            mz.add(M.sonaja(0.1, 0.022), t + (k * 4 + 3) * beat, 0.2)
        t += compas * 16
        # coro
        acompanar(mz, M.P_CORO, t, compas, beat, '4', 1.0, cuerdas_on=True, campanitas=True)
        linea(mz, M.CORO, t, beat, glock, 1, 0.05, rev=0.35)
        if vuelta == 0:
            linea(mz, M.CORO, t, beat, flauta, 0, 0.08)
        else:
            linea(mz, M.CORO, t, beat, corno, 0, 0.085)
            linea(mz, tercera_abajo(M.CORO), t, beat, clarinete, 0, 0.05)
            for i in range(16):
                mz.add(T.choir([note(x) for x in triada(*M.P_CORO[i])], compas * 1.02, 0.045), t + i * compas, 0.5)
                if i % 4 == 0:
                    mz.add(T.timpani(1.0, note(M.P_CORO[i][0]), 0.14), t + i * compas, 0.2)
        for k in range(64):
            mz.add(M.sonaja(0.12, 0.035 if k % 2 else 0.05), t + k * beat, 0.2)
            if k % 2:
                mz.add(escobilla(0.05), t + k * beat, 0.1)
        t += compas * 16
    # final: acorde grande
    for x in (48, 60, 64, 67, 72):
        mz.add(cuerdas([note(x)], compas * 1.6, 0.06), t, 0.5)
        mz.add(piano(note(x), compas, 0.05), t, 0.3)
    mz.add(corno(note(64), compas * 1.4, 0.06), t, 0.4)
    mz.add(M.campana(note(72), 3, 0.1), t, 0.4)
    mz.add(M.sonaja(0.4, 0.06), t, 0.3)
    return mz.render(t + compas * 1.6)


def wish():
    beat = 60 / 160
    compas = beat * 3
    mz = Mezcla(compas * 18 * 3 + 4)
    t = 0.0
    for vuelta in range(3):
        # "We wish you..." (con anacrusa de un tiempo)
        acompanar(mz, M.P_WISH, t + beat, compas, beat, 'vals', 0.9, cuerdas_on=vuelta > 0, campanitas=vuelta == 2)
        inst = (clarinete, flauta, corno)[vuelta]
        linea(mz, M.WISH, t, beat, inst, 0 if vuelta < 2 else 0, 0.08)
        if vuelta == 1:
            linea(mz, M.WISH, t, beat, glock, 1, 0.035)
        if vuelta == 2:
            linea(mz, M.WISH, t, beat, glock, 1, 0.045)
            linea(mz, tercera_abajo(M.WISH), t, beat, clarinete, 0, 0.045)
        for i in range(9):
            for k in (1, 2):
                mz.add(M.sonaja(0.1, 0.02 + 0.01 * vuelta), t + beat + i * compas + k * beat, 0.2)
        t += compas * 9
        # "Good tidings..."
        acompanar(mz, M.P_TID, t + beat, compas, beat, 'vals', 0.9, cuerdas_on=True, campanitas=vuelta == 2)
        linea(mz, M.TIDINGS, t, beat, corno if vuelta == 2 else flauta, 0 if vuelta == 2 else 1, 0.075)
        if vuelta == 2:
            for i in range(8):
                mz.add(T.choir([note(x) for x in triada(*M.P_TID[i])], compas * 1.02, 0.04), t + beat + i * compas, 0.5)
        t += compas * 8 + beat
    for x in (43, 55, 59, 62, 67):
        mz.add(cuerdas([note(x)], compas * 2, 0.06), t, 0.5)
        mz.add(piano(note(x), compas, 0.05), t, 0.3)
    mz.add(M.campana(note(79), 3, 0.09), t, 0.4)
    return mz.render(t + compas * 2)


def noche():
    beat = 60 / 160
    compas = beat * 6
    mz = Mezcla(compas * 24 * 2 + 5)
    t = 0.0
    for vuelta in range(2):
        for i, (bj, ch) in enumerate(M.P_NOCHE):
            ti = t + i * compas
            tri = triada(bj, ch)
            mz.add(pizz(note(bj), vol=0.1), ti, 0.2)
            arp = [tri[0], tri[1], tri[-1], tri[0] + 12, tri[-1], tri[1]]
            for k, x in enumerate(arp):
                mz.add(arpa(note(x), 1.6, 0.04), ti + k * beat, 0.4)
            mz.add(cuerdas([note(x) for x in tri], compas * 1.0, 0.045 if vuelta == 0 else 0.06, ataque=0.5, brillo=1800), ti, 0.5)
        if vuelta == 0:
            linea(mz, M.NOCHE, t, beat, flauta, 0, 0.08, rev=0.4)
        else:
            linea(mz, M.NOCHE, t, beat, lambda f, d, v: cuerdas([f], d, v, ataque=0.12, brillo=3200), 0, 0.1, rev=0.45)
            linea(mz, M.NOCHE, t, beat, lambda f, d, v: T.choir([f], d + 0.2, v), -1, 0.05, rev=0.5)
            linea(mz, M.NOCHE, t, beat, glock, 1, 0.02, rev=0.5)
        t += compas * 24
    for x in (36, 48, 55, 64, 67, 72):
        mz.add(cuerdas([note(x)], compas * 1.5, 0.05, ataque=0.6), t, 0.5)
    mz.add(arpa(note(60), 3, 0.05), t, 0.5)
    mz.add(M.campana(note(84), 4, 0.06), t + beat, 0.5)
    return mz.render(t + compas * 1.4)


def deck():
    beat = 60 / 140
    compas = beat * 4
    mz = Mezcla(compas * 16 * 3 + 4)
    t = 0.0
    for vuelta in range(3):
        acompanar(mz, M.P_DECK, t, compas, beat, '4', 0.95, cuerdas_on=vuelta > 0, campanitas=vuelta > 0)
        if vuelta == 0:
            linea(mz, M.DECK, t, beat, clarinete, 0, 0.08)
        elif vuelta == 1:
            linea(mz, M.DECK, t, beat, flauta, 1, 0.07)
            linea(mz, M.DECK, t, beat, glock, 1, 0.03)
        else:
            linea(mz, M.DECK, t, beat, corno, 0, 0.085)
            linea(mz, tercera_abajo(M.DECK), t, beat, clarinete, 0, 0.05)
            linea(mz, M.DECK, t, beat, glock, 1, 0.04)
            for i in range(16):
                mz.add(T.choir([note(x) for x in triada(*M.P_DECK[i])], compas * 1.02, 0.04), t + i * compas, 0.5)
                if i % 2 == 0:
                    mz.add(T.timpani(0.9, note(M.P_DECK[i][0]), 0.12), t + i * compas, 0.2)
            for k in range(32):
                mz.add(M.sonaja(0.12, 0.03), t + k * 2 * beat + beat, 0.2)
        for k in range(16):
            mz.add(escobilla(0.035), t + k * compas + beat, 0.1)
            mz.add(escobilla(0.035), t + k * compas + 3 * beat, 0.1)
        t += compas * 16
    for x in (41, 53, 57, 60, 65):
        mz.add(cuerdas([note(x)], compas * 1.6, 0.06), t, 0.5)
        mz.add(piano(note(x), compas, 0.05), t, 0.3)
    mz.add(corno(note(65), compas * 1.4, 0.06), t, 0.4)
    mz.add(M.campana(note(77), 3, 0.09), t, 0.4)
    return mz.render(t + compas * 1.6)


def guardar(x, nombre):
    ruta = os.path.join(SONIDOS, nombre + '.ogg')
    raw = ruta + '.raw'
    k = int(0.005 * SR)
    x = x.copy()
    x[:k] *= np.linspace(0, 1, k)
    (np.clip(x, -1, 1) * 32767).astype('<i2').tofile(raw)
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-f', 's16le', '-ar', str(SR), '-ac', '1', '-i', raw, '-c:a', 'libvorbis',
                    '-q:a', '4', ruta], check=True)
    os.remove(raw)
    return len(x) / SR


if __name__ == '__main__':
    solo = sys.argv[1:] or None
    largos = {}
    piezas = [('campanas', jingle, M.jingle), ('navidad', wish, M.wish), ('noche_paz', noche, M.noche), ('adornen', deck, M.deck)]
    for nombre, orquesta, cajita in piezas:
        if solo and nombre not in solo:
            continue
        s = guardar(orquesta(), 'villancico_' + nombre)
        c = guardar(M.mono(cajita())[0], 'caja_' + nombre)
        largos[nombre] = (int(s * 20), int(c * 20))
        print(nombre, 'orquesta', round(s, 1), 's | caja', round(c, 1), 's', flush=True)
    if not solo:
        con = {'campanas': 'CAMPANAS', 'navidad': 'NAVIDAD', 'noche_paz': 'NOCHE_PAZ', 'adornen': 'ADORNEN'}
        lineas = []
        for k, v in largos.items():
            lineas.append(f'    public static final int LARGO_{con[k]} = {v[0]};')
        for k, v in largos.items():
            lineas.append(f'    public static final int CAJA_{con[k]} = {v[1]};')
        java = ('package com.navidad.registry;\n\n'
                '/** Cuánto dura cada villancico (en ticks): los discos (con orquesta) y la versión de caja de música. Lo escribe\n'
                ' *  {@code herramientas/orquesta.py}. */\n'
                'public final class Musica {\n' + '\n'.join(lineas) + '\n\n    private Musica() {\n    }\n}\n')
        open(JAVA, 'w').write(java)
        print('Musica.java listo')
