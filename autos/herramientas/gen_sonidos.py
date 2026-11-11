#!/usr/bin/env python3
"""Sintetiza los sonidos de la maquina expendedora y las cajas de autos (sin usar archivos de nadie) y los guarda como .ogg.
Uso: python3 gen_sonidos.py   (necesita numpy y ffmpeg)"""
import os, subprocess, wave
import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/autos_coleccion/sounds")
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(7)


def t(d):
    return np.arange(int(SR * d)) / SR


def env(n, a=0.01, d=0.3, s=0.0, r=0.1, hold=None):
    """Envolvente simple: ataque a (s), caida d hasta nivel s, relajacion r al final."""
    e = np.zeros(n)
    na, nd, nr = int(a * SR), int(d * SR), int(r * SR)
    e[:na] = np.linspace(0, 1, max(na, 1))
    e[na:na + nd] = np.linspace(1, s, max(len(e[na:na + nd]), 1))
    e[na + nd:] = s
    if nr:
        e[-nr:] *= np.linspace(1, 0, nr)
    return e


def sine(f, d, ph=0.0):
    return np.sin(2 * np.pi * f * t(d) + ph)


def saw(f, d):
    x = (t(d) * f) % 1.0
    return 2 * x - 1


def square(f, d):
    return np.sign(np.sin(2 * np.pi * f * t(d)))


def glide(f0, f1, d, wave_fn="sine"):
    n = int(SR * d)
    f = np.linspace(f0, f1, n)
    ph = 2 * np.pi * np.cumsum(f) / SR
    if wave_fn == "sine":
        return np.sin(ph)
    if wave_fn == "saw":
        return 2 * ((ph / (2 * np.pi)) % 1.0) - 1
    return np.sign(np.sin(ph))


def noise(d):
    return rng.standard_normal(int(SR * d))


def bandpass(x, lo, hi):
    X = np.fft.rfft(x)
    fr = np.fft.rfftfreq(len(x), 1 / SR)
    X[(fr < lo) | (fr > hi)] = 0
    return np.fft.irfft(X, len(x))


def mix(parts, d):
    out = np.zeros(int(SR * d))
    for at, x, g in parts:
        i = int(at * SR)
        j = min(len(out), i + len(x))
        if j > i:
            out[i:j] += x[:j - i] * g
    return out


def norm(x, peak=0.85):
    m = np.max(np.abs(x)) or 1.0
    return x / m * peak


def save(name, x):
    x = norm(x)
    # fundido corto para evitar clics
    f = int(0.004 * SR)
    x[:f] *= np.linspace(0, 1, f)
    x[-f:] *= np.linspace(1, 0, f)
    wav = os.path.join(OUT, name + ".wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((x * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "3", os.path.join(OUT, name + ".ogg")], check=True)
    os.remove(wav)



def tone(f, d, a=0.005, dec=0.2, kind="sine"):
    x = {"sine": sine, "saw": saw, "square": square}[kind](f, d)
    return x * env(len(x), a, dec, 0.0, 0.02)


def chord(fs, d, kind="sine", dec=None):
    return sum(tone(f, d, 0.005, dec or d * 0.9, kind) for f in fs) / len(fs)


def engine(f0, f1, d, grit=0.25):
    n = int(SR * d)
    f = np.linspace(f0, f1, n) ** 1.0
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = np.sign(np.sin(ph)) * 0.5 + np.sin(2 * ph) * 0.3 + (2 * ((ph / (2 * np.pi)) % 1) - 1) * 0.4
    x += bandpass(rng.standard_normal(n), 80, 900) * grit
    return x


def arpeggio(fs, step, kind="square", dec=0.12):
    d = step * len(fs) + dec + 0.05
    return mix([(i * step, tone(f, dec + 0.05, 0.003, dec, kind), 1.0) for i, f in enumerate(fs)], d)


S = {}
S["coin"] = mix([(0, tone(1568, 0.12, 0.002, 0.1, "square"), 0.6), (0.07, tone(2093, 0.35, 0.002, 0.33, "square"), 0.6),
                 (0, bandpass(noise(0.05), 3000, 9000) * np.exp(-np.linspace(0, 8, int(SR * 0.05))), 0.6)], 0.45)
S["machine_start"] = mix([(0, glide(180, 720, 0.5, "saw") * env(int(SR * 0.5), 0.02, 0.45, 0.3, 0.05), 0.5),
                          (0.42, chord([523, 659, 784, 1047], 0.5, "square"), 0.5)], 1.0)
S["count"] = tone(880, 0.22, 0.003, 0.2, "square")
S["go"] = chord([1047, 1319, 1568], 0.55, "square")
S["perfect"] = arpeggio([1319, 1568, 2093], 0.045, "square", 0.14)
S["good"] = arpeggio([988, 1319], 0.05, "square", 0.12)
S["miss"] = glide(320, 110, 0.32, "saw") * env(int(SR * 0.32), 0.005, 0.3, 0.0, 0.02)
S["rev"] = engine(60, 260, 1.1) * env(int(SR * 1.1), 0.05, 1.0, 0.2, 0.1)
S["shift"] = mix([(0, bandpass(noise(0.06), 600, 4000) * np.exp(-np.linspace(0, 7, int(SR * 0.06))), 0.8),
                  (0.04, engine(220, 140, 0.25) * env(int(SR * 0.25), 0.005, 0.24, 0.0, 0.01), 0.5)], 0.32)
S["nitro"] = mix([(0, bandpass(noise(0.9), 300, 6000) * env(int(SR * 0.9), 0.02, 0.85, 0.0, 0.05), 0.8),
                  (0, glide(300, 1400, 0.9, "saw") * env(int(SR * 0.9), 0.05, 0.8, 0.0, 0.05), 0.3)], 0.95)
S["note"] = tone(784, 0.18, 0.002, 0.16, "sine") + 0.3 * tone(1568, 0.18, 0.002, 0.1, "sine")
S["key"] = bandpass(noise(0.035), 1500, 7000) * np.exp(-np.linspace(0, 10, int(SR * 0.035)))
S["error"] = mix([(0, tone(196, 0.14, 0.002, 0.13, "square"), 1), (0.12, tone(147, 0.22, 0.002, 0.2, "square"), 1)], 0.36)
S["tally"] = tone(1760, 0.05, 0.001, 0.045, "square")
S["result"] = arpeggio([523, 659, 784], 0.07, "square", 0.18)
S["box_drop"] = mix([(0, sine(70, 0.3) * np.exp(-np.linspace(0, 9, int(SR * 0.3))), 1.0),
                     (0, bandpass(noise(0.12), 200, 2500) * np.exp(-np.linspace(0, 10, int(SR * 0.12))), 0.6)], 0.32)
S["box_shake"] = mix([(i * 0.06, bandpass(noise(0.05), 400, 3500) * np.exp(-np.linspace(0, 8, int(SR * 0.05))), 0.8) for i in range(4)], 0.3)
S["tape"] = bandpass(noise(0.4), 1500, 9000) * env(int(SR * 0.4), 0.01, 0.35, 0.0, 0.03) * (0.6 + 0.4 * np.sign(np.sin(2 * np.pi * 38 * t(0.4))))
S["flaps"] = mix([(0, bandpass(noise(0.15), 200, 2500) * env(int(SR * 0.15), 0.01, 0.13, 0, 0.01), 1),
                  (0.15, bandpass(noise(0.15), 250, 2800) * env(int(SR * 0.15), 0.01, 0.13, 0, 0.01), 1),
                  (0.05, glide(400, 900, 0.5, "sine") * env(int(SR * 0.5), 0.05, 0.45, 0, 0.02), 0.3)], 0.6)
S["reveal_common"] = arpeggio([523, 659, 784, 1047], 0.06, "square", 0.25)
S["reveal_rare"] = mix([(0, arpeggio([587, 740, 880, 1175], 0.06, "square", 0.25), 1), (0.3, chord([587, 740, 880, 1175], 0.8, "sine"), 0.6)], 1.2)
S["reveal_epic"] = mix([(0, arpeggio([523, 659, 784, 1047, 1319], 0.06, "saw", 0.3), 0.8),
                        (0.35, chord([523, 659, 784, 988, 1319], 1.2, "saw"), 0.5),
                        (0.35, bandpass(noise(1.2), 4000, 12000) * env(int(SR * 1.2), 0.05, 1.1, 0, 0.05), 0.15)], 1.7)
S["reveal_legend"] = mix([(0, glide(200, 1600, 0.6, "saw") * env(int(SR * 0.6), 0.05, 0.5, 0.3, 0.05), 0.5),
                          (0.5, arpeggio([784, 988, 1175, 1568, 1976, 2349], 0.05, "square", 0.3), 0.7),
                          (0.75, chord([392, 494, 587, 784, 988, 1175], 1.6, "saw"), 0.55),
                          (0.75, bandpass(noise(1.5), 5000, 14000) * env(int(SR * 1.5), 0.05, 1.4, 0, 0.05), 0.2)], 2.4)
S["sparkle"] = mix([(i * 0.04, tone(2637 + 300 * i, 0.15, 0.001, 0.14, "sine"), 0.7) for i in range(4)], 0.32)
S["place"] = mix([(0, tone(330, 0.12, 0.002, 0.1, "sine"), 1), (0, bandpass(noise(0.05), 800, 4000) * np.exp(-np.linspace(0, 9, int(SR * 0.05))), 0.6),
                  (0.08, tone(660, 0.25, 0.002, 0.22, "sine"), 0.5)], 0.4)
S["garage"] = mix([(0, engine(50, 90, 0.8, 0.4) * env(int(SR * 0.8), 0.05, 0.7, 0.0, 0.05), 0.6),
                   (0.1, arpeggio([392, 523, 659], 0.08, "sine", 0.3), 0.5)], 0.9)
S["whoosh"] = bandpass(noise(0.45), 300, 3000) * np.sin(np.linspace(0, np.pi, int(SR * 0.45))) ** 2
S["new_car"] = mix([(0, arpeggio([1047, 1319, 1568, 2093, 2637], 0.05, "square", 0.2), 0.8), (0.3, chord([1047, 1319, 1568, 2093], 0.6, "sine"), 0.5)], 1.0)

if __name__ == "__main__":
    for k, v in S.items():
        save(k, v)
    print("sonidos:", len(S))
