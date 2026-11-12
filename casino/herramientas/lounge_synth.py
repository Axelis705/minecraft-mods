#!/usr/bin/env python3
"""Pista de jazz de salón hecha con código (piano eléctrico, contrabajo caminante, escobillas y platillo ride con swing).
Original y sin derechos de nadie. Sale en casino/src/main/resources/assets/casino/sounds/music/<nombre>.ogg

    python3 casino/herramientas/lounge_synth.py
"""
import os
import random
import subprocess
import wave

import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "casino", "sounds", "music")


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def env(n, a, d, s, r, total):
    e = np.zeros(total)
    a, d, r = int(a * SR), int(d * SR), int(r * SR)
    hold = max(0, total - a - d - r)
    seg = [np.linspace(0, 1, a, False), np.linspace(1, s, d, False), np.full(hold, s), np.linspace(s, 0, r)]
    x = np.concatenate(seg)[:total]
    e[:len(x)] = x
    return e


def epiano(f, dur, vel):
    n = int(dur * SR)
    t = np.arange(n) / SR
    # tipo Rhodes: senoidal con un poco de modulación FM y campana al inicio
    mod = np.sin(2 * np.pi * f * 1.0 * t) * (1.2 * np.exp(-t * 6))
    s = np.sin(2 * np.pi * f * t + mod) + 0.25 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-t * 3)
    trem = 1 + 0.06 * np.sin(2 * np.pi * 4.5 * t)
    return s * env(n, 0.005, 0.4, 0.35, 0.25, n) * np.exp(-t * 0.9) * trem * vel


def bass(f, dur, vel):
    n = int(dur * SR)
    t = np.arange(n) / SR
    s = np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * 2 * f * t) + 0.12 * np.sin(2 * np.pi * 3 * f * t)
    pluck = np.exp(-t * 7) * 0.6 + 0.4
    return np.tanh(s * 1.3) * env(n, 0.004, 0.08, 0.7, 0.06, n) * pluck * vel


def noise_hit(dur, vel, color, decay, rnd):
    n = int(dur * SR)
    t = np.arange(n) / SR
    w = rnd.standard_normal(n)
    if color == "ride":
        # metal: ruido filtrado agudo + parciales inarmónicos
        w = np.diff(w, prepend=0)
        w += 0.6 * sum(np.sin(2 * np.pi * fr * t) for fr in (3150, 4870, 6230, 8130)) / 4
    elif color == "brush":
        w = np.convolve(w, np.ones(6) / 6, mode="same")
    elif color == "kick":
        w = np.sin(2 * np.pi * (55 + 60 * np.exp(-t * 30)) * t) * 3
    return w * np.exp(-t * decay) * vel


def place(buf, sig, start):
    i = int(start * SR)
    if i >= len(buf):
        return
    end = min(len(buf), i + len(sig))
    buf[i:end] += sig[:end - i]


# acordes (notas MIDI) y raíz del bajo
CHORDS = {
    "Dm9": ([62, 65, 69, 72, 76], 50), "G13": ([59, 64, 65, 69], 43), "Cmaj9": ([60, 64, 67, 71, 74], 48), "A7b9": ([61, 64, 67, 70], 45),
    "Fmaj7": ([60, 64, 65, 69], 41), "Fm6": ([60, 62, 65, 68], 41), "Em7": ([59, 62, 64, 67], 40), "Ebdim": ([60, 63, 66, 69], 39),
    "Bbmaj7": ([58, 62, 65, 69], 46), "Am9": ([60, 64, 67, 71], 45), "D9": ([60, 62, 66, 69], 50), "Gm9": ([58, 62, 65, 69], 43),
    "C9": ([58, 62, 64, 67], 48), "E7": ([56, 59, 62, 64], 40),
}

SONGS = {
    "after_hours": dict(bpm=104, seed=7, prog=["Dm9", "G13", "Cmaj9", "A7b9", "Dm9", "G13", "Cmaj9", "Cmaj9",
                                           "Fmaj7", "Fm6", "Em7", "A7b9", "Dm9", "G13", "Cmaj9", "A7b9"] * 2),
    "midnight": dict(bpm=92, seed=11, prog=["Am9", "D9", "Gm9", "C9", "Fmaj7", "Bbmaj7", "Em7", "E7",
                                             "Am9", "D9", "Gm9", "C9", "Fmaj7", "E7", "Am9", "E7"] * 2),
}

SCALE_TONES = [0, 2, 3, 5, 7, 9, 10]


def song(name, bpm, seed, prog):
    rnd = np.random.default_rng(seed)
    pr = random.Random(seed)
    beat = 60.0 / bpm
    swing = beat * 0.66  # corchea larga del swing
    total = len(prog) * 4 * beat
    buf = np.zeros(int((total + 2) * SR))
    for bar, ch in enumerate(prog):
        notes, root = CHORDS[ch]
        nxt = CHORDS[prog[(bar + 1) % len(prog)]][1]
        t0 = bar * 4 * beat
        # piano: comping en el 2 y el "y" del 3, con variaciones
        hits = [(1, 0.55), (2 + 0.66, 0.45)] if bar % 2 == 0 else [(0, 0.5), (1 + 0.66, 0.4), (3, 0.42)]
        for b, v in hits:
            for k, nn in enumerate(notes):
                place(buf, epiano(midi(nn), beat * 1.4, v * 0.16), t0 + b * beat + k * 0.008)
        # contrabajo caminante: raíz, notas del acorde y aproximación cromática al siguiente
        line = [root, root + pr.choice([3, 4, 7]), root + 7, nxt + pr.choice([-1, 1])]
        for b, n in enumerate(line):
            place(buf, bass(midi(n - 12), beat * 0.95, 0.42), t0 + b * beat)
        # batería: ride con swing, escobilla en 2 y 4, bombo suave en 1
        for b in range(4):
            place(buf, noise_hit(0.6, 0.05, "ride", 6, rnd), t0 + b * beat)
            if b in (1, 3):
                place(buf, noise_hit(0.25, 0.05, "ride", 14, rnd), t0 + b * beat + swing)
                place(buf, noise_hit(0.35, 0.10, "brush", 9, rnd), t0 + b * beat)
            if b == 0:
                place(buf, noise_hit(0.3, 0.18, "kick", 12, rnd), t0)
        # melodía suave en la segunda mitad de cada 4 compases
        if bar % 4 in (2, 3):
            base = notes[-1] + 12
            for i in range(pr.choice([2, 3, 4])):
                n = 60 + 12 + pr.choice(SCALE_TONES) + (root % 12) - 2
                n = max(72, min(88, n))
                place(buf, epiano(midi(n), beat * 0.9, 0.11), t0 + (i * 1.0 + pr.choice([0, 0.66])) * beat)
    # suavizar y normalizar; el final se mezcla con el principio para que el bucle no se note
    buf = buf[:int(total * SR)]
    fade = int(0.08 * SR)
    buf[:fade] *= np.linspace(0, 1, fade)
    buf[-fade:] *= np.linspace(1, 0, fade)
    buf = np.convolve(buf, np.ones(3) / 3, mode="same")
    buf /= np.max(np.abs(buf)) * 1.15
    # un poco de sala (eco corto)
    rev = np.zeros_like(buf)
    for d, g in ((0.031, 0.25), (0.047, 0.2), (0.071, 0.15), (0.113, 0.1)):
        k = int(d * SR)
        rev[k:] += buf[:-k] * g
    buf = (buf + rev) / 1.5
    st = np.stack([buf, np.roll(buf, int(0.012 * SR))], axis=1)
    pcm = (st * 32767).astype(np.int16)
    os.makedirs(OUT, exist_ok=True)
    wav = os.path.join(OUT, name + ".wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    ogg = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "4", ogg], check=True)
    os.remove(wav)
    print(name, round(total, 1), "s")


if __name__ == "__main__":
    for k, v in SONGS.items():
        song(k, **v)
