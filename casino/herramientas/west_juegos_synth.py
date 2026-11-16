#!/usr/bin/env python3
"""Sonidos del toro mecánico y de la galería de tiro del saloon, hechos con código (originales, sin derechos de nadie).

    python3 casino/herramientas/west_juegos_synth.py

Salen en casino/src/main/resources/assets/casino/sounds/west/:
  gallery_shot  disparo de revólver de feria (estallido seco con cola corta)
  gallery_ding  campanita de blanco derribado
  gallery_quack chillido de patito de hule
  gallery_buzz  zumbido de error (le diste a la señorita)
  bull_bell     cencerro (cada premio que alcanzas en el toro)
  bull_motor    resoplido mecánico del toro (pistón y motor)
  duel_theme    silbido del enfrentamiento con guitarra (duelo al mediodía)
  duel_draw     golpe metálico de "¡desenfunda!"
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from ragtime_synth import SR, write  # noqa: E402

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "casino", "sounds", "west")


def env(t, a, d):
    return np.minimum(1, t / max(a, 1e-4)) * np.exp(-t / d)


def norm(s, level=0.85):
    return s / (np.max(np.abs(s)) + 1e-9) * level


def lowpass(s, k):
    return np.convolve(s, np.ones(k) / k, mode="same")


def shot():
    rnd = np.random.default_rng(11)
    dur = 0.7
    t = np.arange(int(dur * SR)) / SR
    crack = rnd.standard_normal(len(t)) * np.exp(-t / 0.012)
    body = lowpass(rnd.standard_normal(len(t)), 6) * np.exp(-t / 0.06) * 0.8
    thump = np.sin(2 * np.pi * (95 - 40 * t) * t) * np.exp(-t / 0.05) * 0.9
    tail = lowpass(rnd.standard_normal(len(t)), 30) * np.exp(-t / 0.22) * 0.35
    s = crack + body + thump + tail
    # eco del cuarto de madera
    d = int(0.045 * SR)
    s[d:] += s[:-d] * 0.25
    return norm(s, 0.9)


def ding():
    dur = 1.1
    t = np.arange(int(dur * SR)) / SR
    f0 = 1320
    s = np.zeros_like(t)
    for k, (r, a, dd) in enumerate(((1.0, 1.0, 0.45), (2.76, 0.45, 0.22), (5.40, 0.25, 0.12), (8.93, 0.12, 0.06))):
        s += a * np.sin(2 * np.pi * f0 * r * t) * env(t, 0.001, dd)
    click = np.random.default_rng(2).standard_normal(len(t)) * np.exp(-t / 0.003) * 0.3
    return norm(s + click, 0.75)


def quack():
    dur = 0.32
    t = np.arange(int(dur * SR)) / SR
    f = 900 + 500 * np.sin(np.pi * t / dur) ** 2 - 350 * t / dur
    ph = 2 * np.pi * np.cumsum(f) / SR
    s = np.sign(np.sin(ph)) * 0.5 + np.sin(ph * 2) * 0.3 + np.sin(ph * 3) * 0.2
    s = lowpass(s, 3)
    s *= np.sin(np.pi * t / dur) ** 0.6
    return norm(s, 0.6)


def buzz():
    dur = 0.6
    t = np.arange(int(dur * SR)) / SR
    ph = 2 * np.pi * 140 * t
    s = ((ph / (2 * np.pi)) % 1.0 * 2 - 1) * 0.6 + np.sign(np.sin(ph * 1.5)) * 0.3
    s = lowpass(s, 4) * np.minimum(1, t / 0.01) * np.minimum(1, (dur - t) / 0.05)
    return norm(s, 0.6)


def cowbell():
    dur = 0.9
    t = np.arange(int(dur * SR)) / SR
    s = np.zeros_like(t)
    for f, a in ((562, 1.0), (845, 0.8), (1130, 0.3), (1680, 0.15)):
        sq = np.sign(np.sin(2 * np.pi * f * t)) * 0.4 + np.sin(2 * np.pi * f * t) * 0.6
        s += a * sq
    s = lowpass(s, 3) * env(t, 0.001, 0.18)
    s += np.random.default_rng(4).standard_normal(len(t)) * np.exp(-t / 0.004) * 0.4
    return norm(s, 0.8)


def motor():
    rnd = np.random.default_rng(9)
    dur = 0.9
    t = np.arange(int(dur * SR)) / SR
    hiss = lowpass(rnd.standard_normal(len(t)), 5) * (np.exp(-((t - 0.12) / 0.09) ** 2) * 0.9 + 0.15)
    f = 55 + 25 * np.sin(np.pi * t / dur)
    ph = 2 * np.pi * np.cumsum(f) / SR
    hum = (np.sin(ph) * 0.6 + np.sin(ph * 2) * 0.3 + np.sign(np.sin(ph * 4)) * 0.1)
    clank = np.zeros_like(t)
    for at in (0.02, 0.48):
        i = int(at * SR)
        n = int(0.08 * SR)
        k = np.arange(n) / SR
        clank[i:i + n] += np.sin(2 * np.pi * 410 * k) * np.exp(-k / 0.02) * 0.7 + rnd.standard_normal(n) * np.exp(-k / 0.006) * 0.4
    s = hiss * 0.6 + hum * 0.5 * np.sin(np.pi * t / dur) + clank
    return norm(s, 0.7)


def whistle_note(f0, dur, glide=0.0, vib=5.5):
    t = np.arange(int(dur * SR)) / SR
    f = f0 * (1 + glide * np.minimum(1, t / 0.12)) * (1 + 0.012 * np.sin(2 * np.pi * vib * t) * np.minimum(1, t / 0.25))
    ph = 2 * np.pi * np.cumsum(f) / SR
    s = np.sin(ph) + 0.08 * np.sin(2 * ph)
    breath = lowpass(np.random.default_rng(int(f0)).standard_normal(len(t)), 3) * 0.05
    return (s + breath) * np.minimum(1, t / 0.04) * np.minimum(1, (dur - t) / 0.08).clip(0, 1)


def pluck(f0, dur, rnd):
    n = int(dur * SR)
    p = int(SR / f0)
    buf = rnd.uniform(-1, 1, p)
    out = np.zeros(n)
    for i in range(n):
        out[i] = buf[i % p]
        buf[i % p] = 0.5 * (buf[i % p] + buf[(i + 1) % p]) * 0.996
    return out


def duel_theme():
    """Silbido del enfrentamiento (motivo original): dos llamadas que suben, una respuesta que baja y una guitarra grave."""
    rnd = np.random.default_rng(21)
    total = 3.4
    buf = np.zeros(int(total * SR))

    def put(sig, at, gain=1.0):
        i = int(at * SR)
        j = min(len(buf), i + len(sig))
        buf[i:j] += sig[:j - i] * gain

    # E5 -> B5 (sube), E5 -> B5, y G5 - F#5 - E5 largo
    put(whistle_note(659, 0.42, glide=0.5), 0.05, 0.5)
    put(whistle_note(659, 0.42, glide=0.5), 0.70, 0.5)
    put(whistle_note(784, 0.30), 1.35, 0.5)
    put(whistle_note(740, 0.30), 1.68, 0.5)
    put(whistle_note(659, 1.10), 2.00, 0.55)
    # guitarra: E2 y B2 punteados
    for at, f in ((0.05, 82.4), (0.70, 82.4), (1.35, 123.5), (2.00, 82.4)):
        put(pluck(f, 1.3, rnd), at, 0.35)
    # eco del cañón
    d = int(0.28 * SR)
    buf[d:] += buf[:-d] * 0.3
    return norm(buf, 0.8)


def duel_draw():
    """Golpe metálico seco con zumbido: "¡desenfunda!"."""
    dur = 1.0
    t = np.arange(int(dur * SR)) / SR
    s = np.zeros_like(t)
    for r, a, dd in ((1.0, 1.0, 0.35), (2.41, 0.6, 0.2), (3.98, 0.35, 0.1), (6.2, 0.2, 0.05)):
        s += a * np.sin(2 * np.pi * 980 * r * t) * env(t, 0.0005, dd)
    whoosh = lowpass(np.random.default_rng(5).standard_normal(len(t)), 8) * np.exp(-((t - 0.05) / 0.06) ** 2) * 0.6
    return norm(s + whoosh, 0.85)


def dice_shake():
    """Dados sonando dentro de un cubilete de cuero y el golpe al ponerlo en la mesa."""
    rnd = np.random.default_rng(31)
    dur = 0.9
    n = int(dur * SR)
    s = np.zeros(n)
    for k in range(26):
        at = int((0.02 + 0.6 * rnd.random()) * SR)
        m = int(0.012 * SR)
        t = np.arange(m) / SR
        f = rnd.uniform(2500, 4800)
        click = np.sin(2 * np.pi * f * t) * np.exp(-t / 0.003) + rnd.standard_normal(m) * np.exp(-t / 0.002) * 0.5
        s[at:at + m] += click[:max(0, min(m, n - at))] * rnd.uniform(0.3, 1.0)
    s = lowpass(s, 3) * 0.7
    thud_at = int(0.7 * SR)
    t = np.arange(n - thud_at) / SR
    s[thud_at:] += (np.sin(2 * np.pi * 140 * t) * np.exp(-t / 0.04) + lowpass(rnd.standard_normal(len(t)), 10) * np.exp(-t / 0.02)) * 0.9
    return norm(s, 0.8)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, fn in (("gallery_shot", shot), ("gallery_ding", ding), ("gallery_quack", quack), ("gallery_buzz", buzz),
                     ("bull_bell", cowbell), ("bull_motor", motor), ("duel_theme", duel_theme), ("duel_draw", duel_draw),
                     ("dice_shake", dice_shake)):
        write(os.path.join(OUT, name + ".ogg"), fn(), q=5)
        print("listo", name)
