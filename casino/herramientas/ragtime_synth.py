#!/usr/bin/env python3
"""Música de pianola del saloon (ragtime) hecha con código: bajo "stride" (bajo-acorde) en la mano izquierda y melodía sincopada en la
derecha, con piano "honky-tonk" (dos cuerdas un poco desafinadas) y el golpecito mecánico de la pianola. Original y sin derechos de
nadie. También el rechinido de la puerta batiente. Sale en casino/src/main/resources/assets/casino/sounds/.

    python3 casino/herramientas/ragtime_synth.py
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 32000
BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "casino", "sounds")


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def piano(n, dur, vel, rnd):
    f = midi(n)
    m = int(dur * SR)
    t = np.arange(m) / SR
    s = np.zeros(m)
    bright = 1.0 + max(0, (n - 60) / 40)
    for detune in (1.0, 1.0045 + rnd.uniform(-0.001, 0.001)):          # dos cuerdas: suena a piano de cantina
        for k, a in enumerate((1.0, 0.6, 0.38, 0.24, 0.15, 0.09, 0.05), 1):
            fk = f * k * np.sqrt(1 + 0.00035 * k * k) * detune
            if fk > SR / 2.2:
                break
            dec = (1.1 + 0.55 * k) * bright
            s += a * np.sin(2 * np.pi * fk * t + rnd.uniform(0, 6.28)) * np.exp(-dec * t)
    att = np.minimum(1, t / 0.002)
    rel = np.minimum(1, (dur - t) / 0.03).clip(0, 1)
    s *= att * rel
    # golpe del martillo (ruidito agudo muy corto)
    k = min(m, int(0.004 * SR))
    s[:k] += rnd.standard_normal(k) * 0.25 * np.exp(-np.arange(k) / (0.001 * SR))
    return s * vel * 0.5


def place(buf, sig, start):
    i = int(start * SR)
    j = min(len(buf), i + len(sig))
    if i < len(buf):
        buf[i:j] += sig[:j - i]


QUAL = {"": (0, 4, 7), "7": (0, 4, 7, 10), "m": (0, 3, 7), "dim": (0, 3, 6, 9), "6": (0, 4, 7, 9)}

# ritmos de 2 compases (8 corcheas): 1 = nota, 0 = se alarga la anterior. Síncopas típicas del ragtime.
RHYTHMS = [
    [1, 1, 0, 1, 1, 0, 1, 1],
    [1, 0, 1, 1, 0, 1, 1, 0],
    [1, 1, 1, 0, 1, 1, 0, 1],
    [0, 1, 1, 0, 1, 1, 1, 1],
    [1, 1, 0, 1, 0, 1, 1, 1],
]
CONTOURS = [
    [0, 1, 2, 1, 3, 2, 1, 0],
    [2, 3, 4, 3, 2, 1, 2, 0],
    [4, 3, 2, 3, 1, 2, 0, 1],
    [0, 2, 1, 3, 2, 4, 3, 2],
]


def chord_tones(root, q, lo, hi):
    out = []
    for o in range(-3, 4):
        for iv in QUAL[q]:
            n = root + iv + 12 * o
            if lo <= n <= hi:
                out.append(n)
    return sorted(out)


def song(name, key, tempo, prog_a, prog_b, seed):
    rnd = np.random.default_rng(seed)
    e8 = 60.0 / tempo / 2          # una corchea
    bar = 4 * e8                   # compás de 2/4
    form = [prog_a, prog_a, prog_b, prog_b, prog_a]
    bars = sum(len(p) for p in form)
    buf = np.zeros(int((bars * bar + 2.5) * SR))
    motif_cache = {}
    b = 0
    for si, prog in enumerate(form):
        for bi in range(0, len(prog), 2):
            seg = prog[bi:bi + 2]
            # mano izquierda: bajo en el 1, acorde en el 2 (con su corchea suelta de vez en cuando)
            for k, ch in enumerate(seg):
                root, q = ch
                r = key + root
                t0 = (b + k) * bar
                bass_n = 36 + (r % 12)
                if bass_n > 45:
                    bass_n -= 12
                alt = bass_n + 7 if (b + k) % 2 else bass_n      # alterna fundamental y quinta
                if alt > 47:
                    alt -= 12
                place(buf, piano(alt, e8 * 1.8, 0.85, rnd) + piano(alt - 12, e8 * 1.8, 0.45, rnd), t0)
                chord = [n for n in chord_tones(r, q, 52, 64)][:3]
                for n in chord:
                    place(buf, piano(n, e8 * 1.6, 0.42, rnd), t0 + 2 * e8)
                if rnd.random() < 0.35:
                    place(buf, piano(bass_n + 12, e8 * 0.9, 0.35, rnd), t0 + 3 * e8)
            # mano derecha: motivo de 2 compases (se repite la idea con variaciones)
            mkey = (si % 2, (bi // 2) % 4)
            if mkey not in motif_cache or rnd.random() < 0.25:
                motif_cache[mkey] = (int(rnd.integers(len(RHYTHMS))), int(rnd.integers(len(CONTOURS))))
            rh, ct = motif_cache[mkey]
            onsets = [i for i, v in enumerate(RHYTHMS[rh]) if v] or [0]
            for j, i in enumerate(onsets):
                nxt = onsets[j + 1] if j + 1 < len(onsets) else 8
                ch = seg[min(len(seg) - 1, i // 4)]
                tones = chord_tones(key + ch[0], ch[1], 67, 86)
                deg = CONTOURS[ct][i] + (2 if si >= 2 else 0)
                n = tones[min(len(tones) - 1, deg)]
                dur = (nxt - i) * e8 * 0.95
                vel = 0.75 if i % 4 == 0 else 0.62
                place(buf, piano(n, dur + 0.1, vel, rnd), b * bar + i * e8)
                # tercera o sexta abajo para engordar la melodía (muy de ragtime)
                if rnd.random() < 0.55:
                    below = [x for x in tones if x < n]
                    if below:
                        place(buf, piano(below[-1], dur + 0.1, vel * 0.55, rnd), b * bar + i * e8)
            b += len(seg)
    # final: acorde de tónica
    for n in (key + 36, key + 48, key + 52, key + 55, key + 60, key + 64):
        place(buf, piano(n, 2.2, 0.6, rnd), b * bar)
    # algo de sala (un eco corto) y nivel
    echo = np.zeros_like(buf)
    d = int(0.055 * SR)
    echo[d:] = buf[:-d] * 0.18
    buf = buf + echo
    buf /= np.max(np.abs(buf)) + 1e-9
    write(os.path.join(BASE, "music", name + ".ogg"), buf * 0.85, q=4)
    print(name, f"{len(buf) / SR:.0f} s")


def creak():
    rnd = np.random.default_rng(3)
    dur = 0.55
    t = np.arange(int(dur * SR)) / SR
    # roce de madera: tono que sube y baja con tirones (pega-y-suelta)
    f = 420 + 380 * np.sin(np.pi * t / dur) ** 1.5 + 40 * np.sin(2 * np.pi * 9 * t)
    ph = 2 * np.pi * np.cumsum(f) / SR
    saw = (ph / (2 * np.pi)) % 1.0 * 2 - 1
    stick = (np.sin(2 * np.pi * 34 * t + rnd.uniform(0, 3)) > -0.2).astype(float) * 0.7 + 0.3
    s = (0.6 * saw + 0.4 * np.sin(ph * 2)) * stick
    s = np.convolve(s, np.ones(4) / 4, mode="same")
    s *= np.sin(np.pi * t / dur) ** 0.8
    s += rnd.standard_normal(len(t)) * 0.04 * np.sin(np.pi * t / dur)
    write(os.path.join(BASE, "saloon_door.ogg"), s / np.max(np.abs(s)) * 0.7, q=5)


def write(path, s, q=4):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with tempfile.TemporaryDirectory() as d:
        wav = os.path.join(d, "x.wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes((np.clip(s, -1, 1) * 32767).astype(np.int16).tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", str(q), path], check=True)


# progresiones de 16 compases (raíz en semitonos desde la tónica, calidad)
I, IV, V, VI, II = 0, 5, 7, 9, 2
A1 = [(I, ""), (I, ""), (V, "7"), (V, "7"), (I, ""), (I, ""), (II, "7"), (V, "7"),
      (I, ""), (I, "7"), (IV, ""), (IV, "m"), (I, ""), (VI, "7"), (II, "7"), (V, "7")]
B1 = [(IV, ""), (IV, ""), (I, ""), (I, ""), (II, "7"), (II, "7"), (V, "7"), (V, "7"),
      (IV, ""), (IV, "6"), (I, ""), (VI, "7"), (II, "7"), (V, "7"), (I, ""), (I, "")]
A2 = [(I, ""), (VI, "7"), (II, "7"), (V, "7"), (I, ""), (VI, "7"), (II, "7"), (V, "7"),
      (III := 4, "7"), (III, "7"), (VI, "7"), (VI, "7"), (II, "7"), (II, "7"), (V, "7"), (V, "7")]
B2 = [(IV, ""), (IV, "m"), (I, ""), (VI, "7"), (II, "7"), (V, "7"), (I, ""), (I, ""),
      (IV, ""), (1, "dim"), (I, ""), (VI, "7"), (II, "7"), (V, "7"), (I, ""), (V, "7")]


if __name__ == "__main__":
    song("saloon_rag", 60, 196, A1, B1, 11)
    song("dusty_trail", 65, 178, A2, B2, 23)
    song("gold_rush", 67, 210, B1, A2, 37)
    # el rechinido de la puerta ahora sale de grabaciones CC0: west_puerta_sonido.py
