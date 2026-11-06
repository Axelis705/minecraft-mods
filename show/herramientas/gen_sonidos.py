#!/usr/bin/env python3
"""Sintetiza los sonidos de programa de television del mod (sin usar archivos de nadie) y los guarda como .ogg.
Uso: python3 gen_sonidos.py   (necesita numpy y ffmpeg)"""
import os, subprocess, wave
import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/show_de_axelis/sounds")
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


def clap_burst(dur=0.03):
    n = int(SR * dur)
    return bandpass(rng.standard_normal(n), 1200, 7000) * np.exp(-np.linspace(0, 9, n))


def applause(d, density, swell):
    out = np.zeros(int(SR * d))
    n = int(d * density)
    for _ in range(n):
        at = rng.random() * (d - 0.05)
        i = int(at * SR)
        b = clap_burst(0.02 + rng.random() * 0.03) * (0.3 + rng.random() * 0.7)
        out[i:i + len(b)] += b[:len(out) - i]
    e = np.minimum(1, np.linspace(0, 1, len(out)) * swell) * np.minimum(1, np.linspace(1, 0, len(out)) * 4)
    return out * e


def voice(f0, d, formants, vib=5.0, vibamt=0.02):
    n = int(SR * d)
    f = f0 * (1 + vibamt * np.sin(2 * np.pi * vib * np.arange(n) / SR))
    ph = 2 * np.pi * np.cumsum(f) / SR
    src = 2 * ((ph / (2 * np.pi)) % 1.0) - 1
    x = np.zeros(n)
    for fc, bw, g in formants:
        x += bandpass(src, fc - bw, fc + bw) * g
    return x


S = {}

# --- publico
S["applause"] = applause(3.2, 700, 6)
S["applause_big"] = applause(6.0, 1400, 3)
cheer = voice(260, 2.0, [(500, 200, 1.0), (1500, 400, 0.6)], 6, 0.03) * env(int(SR * 2.0), 0.15, 0.4, 0.8, 0.7)
cheer += 0.6 * voice(330, 2.0, [(700, 200, 1.0), (1800, 400, 0.5)], 5, 0.03) * env(int(SR * 2.0), 0.2, 0.4, 0.8, 0.7)
S["cheer"] = cheer * 1.0 + 0.5 * applause(2.0, 500, 8).repeat(1)[:len(cheer)] if False else cheer + 0.5 * np.pad(applause(2.0, 500, 8), (0, len(cheer) - int(SR * 2.0)))
n = int(SR * 2.0)
boo = np.zeros(n)
for f in (150, 170, 190, 210):
    boo += glide(f * 1.15, f * 0.85, 2.0, "saw") * 0.25
boo = bandpass(boo, 100, 900) + 0.3 * bandpass(boo, 400, 900)
S["boo"] = boo * env(n, 0.3, 0.3, 0.8, 0.8)
laugh = np.zeros(int(SR * 2.2))
for i in range(9):
    at = 0.1 + i * 0.2
    f0 = 240 + rng.random() * 60 - i * 6
    x = voice(f0, 0.14, [(800, 250, 1.0), (1500, 300, 0.4)], 0, 0) * env(int(SR * 0.14), 0.01, 0.08, 0.2, 0.04)
    laugh[int(at * SR):int(at * SR) + len(x)] += x * (1 - i * 0.07)
S["laugh"] = laugh
S["crickets"] = mix([(i * 0.35 + j * 0.05, sine(4300 + 80 * (i % 2), 0.03) * env(int(SR * 0.03), 0.003, 0.02, 0, 0.005), 0.5)
                     for i in range(8) for j in range(3)], 3.0)
S["airhorn"] = (glide(430, 435, 1.3, "saw") + glide(540, 545, 1.3, "saw") + 0.6 * glide(650, 655, 1.3, "square")) * env(int(SR * 1.3), 0.02, 0.1, 0.9, 0.2)

# --- campanas y respuestas
def bell(f, d, g=1.0):
    x = sine(f, d) + 0.5 * sine(f * 2.76, d) * np.exp(-t(d) * 6) + 0.3 * sine(f * 5.4, d) * np.exp(-t(d) * 9)
    return x * np.exp(-t(d) * 4) * g

S["ding"] = bell(1318, 1.2)
S["correct"] = mix([(0, bell(988, 0.9), 0.8), (0.14, bell(1318, 1.1), 1.0)], 1.3)
S["reveal"] = mix([(0, sine(880, 0.25) * env(int(SR * 0.25), 0.003, 0.2, 0, 0.02), 0.7),
                   (0.09, sine(1175, 0.35) * env(int(SR * 0.35), 0.003, 0.3, 0, 0.02), 0.8),
                   (0.0, bell(1760, 0.6), 0.4)], 0.7)
S["strike"] = (square(110, 0.9) + square(116, 0.9) + 0.5 * saw(55, 0.9)) * env(int(SR * 0.9), 0.005, 0.15, 0.7, 0.3)
S["buzzer"] = (square(330, 0.4) + 0.6 * square(497, 0.4)) * env(int(SR * 0.4), 0.003, 0.05, 0.8, 0.08)
S["tick"] = sine(1700, 0.06) * np.exp(-t(0.06) * 60)
S["blip"] = square(520, 0.05) * env(int(SR * 0.05), 0.002, 0.04, 0.0, 0.01)
S["pop"] = glide(900, 220, 0.12) * np.exp(-t(0.12) * 25)
S["vote"] = (sine(300, 0.1) + 0.5 * sine(620, 0.1)) * np.exp(-t(0.1) * 40)
S["whistle_up"] = glide(400, 1700, 0.55) * env(int(SR * 0.55), 0.03, 0.1, 0.9, 0.1)
S["whistle_down"] = glide(1700, 350, 0.65) * env(int(SR * 0.65), 0.03, 0.1, 0.9, 0.2)
S["whoosh"] = bandpass(noise(0.7), 300, 5000) * np.sin(np.pi * np.linspace(0, 1, int(SR * 0.7))) ** 2
tu = []
for i, f in enumerate((880, 740, 620)):
    tu.append((i * 0.25, sine(f, 0.2) * env(int(SR * 0.2), 0.004, 0.15, 0.2, 0.03), 0.8))
tu.append((0.8, (square(150, 0.5) + square(155, 0.5)) * env(int(SR * 0.5), 0.005, 0.1, 0.8, 0.2), 0.8))
S["time_up"] = mix(tu, 1.4)

# --- percusion y musica
def snare(d=0.18):
    n = int(SR * d)
    return (bandpass(rng.standard_normal(n), 1500, 9000) + 0.5 * sine(190, d)) * np.exp(-np.linspace(0, 8, n))

def tom(f, d=0.3):
    return glide(f * 1.6, f, d) * np.exp(-t(d) * 9)

dr = []
tt = 0.0
gap = 0.11
while tt < 2.8:
    dr.append((tt, snare(0.12), 0.3 + 0.7 * tt / 2.8))
    tt += gap * (1 - 0.55 * tt / 2.8)
S["drumroll"] = mix(dr, 3.2) + mix([(2.85, snare(0.4), 1.0), (2.85, tom(110, 0.5), 1.2)], 3.2)
S["rimshot"] = mix([(0, snare(0.15), 0.8), (0.14, tom(180, 0.25), 1.0), (0.45, snare(0.25), 0.9), (0.45, bandpass(noise(0.6), 5000, 12000) * np.exp(-t(0.6) * 6), 0.7)], 1.2)

def brass(f, d, g=1.0):
    x = saw(f, d) + 0.5 * saw(f * 1.005, d) + 0.4 * square(f, d)
    x = bandpass(x, 100, 3500)
    return x * env(int(SR * d), 0.05, 0.1, 0.8, 0.1) * g

C, E, G = 523.25, 659.25, 783.99
fan = []
for i, f in enumerate((C, E, G)):
    fan.append((i * 0.18, brass(f, 0.35), 0.8))
for f in (C, E, G, C * 2):
    fan.append((0.7, brass(f, 1.8), 0.6))
S["fanfare"] = mix(fan, 2.7)
intro = [(0, bandpass(noise(1.2), 2000, 12000) * np.linspace(0, 1, int(SR * 1.2)) ** 2, 0.5)]
for i, f in enumerate((C, G, E, G, C * 2, G, E * 2)):
    intro.append((0.3 + i * 0.22, brass(f * 0.5, 0.3), 0.6))
for f in (C, E, G, C * 2):
    intro.append((2.0, brass(f, 2.0), 0.7))
intro.append((2.0, snare(0.6), 0.9))
intro.append((2.0, bandpass(noise(2.0), 4000, 14000) * np.exp(-t(2.0) * 2.5), 0.6))
S["intro"] = mix(intro, 4.2)
S["round_start"] = mix([(i * 0.13, bell(f, 0.7), 0.7) for i, f in enumerate((784, 988, 1175, 1568))], 1.4)
tb = []
for i, (f0, f1, d) in enumerate(((330, 300, 0.45), (311, 280, 0.45), (294, 262, 0.45), (277, 150, 1.4))):
    x = glide(f0, f1, d, "saw")
    x = bandpass(x, 80, 1800) * (1 + 0.15 * np.sin(2 * np.pi * 6 * t(d))) * env(int(SR * d), 0.04, 0.1, 0.8, 0.12)
    tb.append((i * 0.55, x, 1.0))
S["trombone"] = mix(tb, 3.2)
gl = np.zeros(int(SR * 1.5))
for i in range(14):
    at = i * 0.1
    seg = (square(200 + rng.integers(0, 2000), 0.07) * (rng.random() > 0.4) + bandpass(noise(0.07), 500, 9000) * 0.6)
    gl[int(at * SR):int(at * SR) + len(seg)] += np.round(seg * 4) / 4
S["glitch"] = gl
sus = (sine(55, 4.0) + 0.7 * sine(58, 4.0) + 0.4 * saw(110, 4.0) * 0.3) * (0.6 + 0.4 * np.sin(2 * np.pi * np.linspace(2, 9, int(SR * 4.0)) * t(4.0) / 4)) * np.linspace(0.3, 1, int(SR * 4.0))
sus += glide(220, 440, 4.0) * 0.12 * np.linspace(0, 1, int(SR * 4.0))
S["suspense"] = sus

for name, x in S.items():
    save(name, x.astype(np.float64))
    print("ok", name)
