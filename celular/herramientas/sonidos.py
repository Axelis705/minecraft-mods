#!/usr/bin/env python3
"""Genera los sonidos del celular (sintetizados) como .ogg y el sounds.json. Uso: python3 celular/herramientas/sonidos.py"""
import json
import os
import subprocess
import tempfile
import wave

import numpy as np

R = 44100
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "celular")
OUT = os.path.join(ROOT, "sounds")


def t(sec):
    return np.arange(int(R * sec)) / R


def env(n, a=0.005, d=None, curve=4.0):
    """Envolvente: sube en 'a' segundos y cae exponencialmente."""
    x = np.arange(n) / R
    e = np.minimum(1, x / max(a, 1e-4))
    if d is None:
        d = n / R
    return e * np.exp(-curve * x / d)


def note(freq, dur, kind="bell", vol=1.0):
    x = t(dur)
    if kind == "bell":
        s = np.sin(2 * np.pi * freq * x) + 0.35 * np.sin(2 * np.pi * freq * 2.01 * x) + 0.12 * np.sin(2 * np.pi * freq * 3.98 * x)
        s *= env(len(x), 0.004, dur, 5)
    elif kind == "marimba":
        s = np.sin(2 * np.pi * freq * x) + 0.25 * np.sin(2 * np.pi * freq * 4 * x) * np.exp(-30 * x)
        s *= env(len(x), 0.003, dur, 6)
    else:
        s = np.sin(2 * np.pi * freq * x) * env(len(x), 0.01, dur, 2)
    return s * vol


def place(buf, s, at):
    i = int(at * R)
    end = min(len(buf), i + len(s))
    buf[i:end] += s[:end - i]


def bandpass_noise(n, lo, hi, seed=1):
    rng = np.random.default_rng(seed)
    w = rng.standard_normal(n)
    f = np.fft.rfft(w)
    fr = np.fft.rfftfreq(n, 1 / R)
    f[(fr < lo) | (fr > hi)] = 0
    return np.fft.irfft(f, n)


def write(name, s, gain=0.8):
    s = s / (np.max(np.abs(s)) + 1e-9) * gain
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as d:
        wav = os.path.join(d, name + ".wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(R)
            w.writeframes((np.clip(s, -1, 1) * 32767).astype(np.int16).tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "5",
                        os.path.join(OUT, name + ".ogg")], check=True)


def shutter():
    buf = np.zeros(int(R * 0.32))
    for at, lo, hi, g in [(0.0, 1500, 9000, 1.0), (0.09, 900, 6000, 0.8)]:
        n = int(R * 0.06)
        click = bandpass_noise(n, lo, hi, int(at * 100)) * env(n, 0.0008, 0.05, 6) * g
        place(buf, click, at)
    # pequeño "zumbido" mecánico
    x = t(0.09)
    place(buf, 0.15 * np.sign(np.sin(2 * np.pi * 180 * x)) * env(len(x), 0.002, 0.09, 3), 0.005)
    return buf


def ringtone():
    # melodía alegre de marimba (dura menos de 2.6 s porque se repite)
    seq = [(76, 0.0), (79, 0.16), (83, 0.32), (81, 0.48), (79, 0.64), (76, 0.8),
           (76, 1.12), (79, 1.28), (83, 1.44), (86, 1.6), (83, 1.76), (88, 1.92)]
    buf = np.zeros(int(R * 2.45))
    for m, at in seq:
        f = 440 * 2 ** ((m - 69) / 12)
        place(buf, note(f, 0.45, "marimba"), at)
        place(buf, note(f / 2, 0.3, "marimba", 0.25), at)
    return buf


def ringback():
    x = t(1.4)
    s = np.sin(2 * np.pi * 440 * x) + np.sin(2 * np.pi * 480 * x)
    e = np.minimum(1, x / 0.03) * np.minimum(1, (1.4 - x) / 0.05)
    return s * e * 0.5


def notify():
    buf = np.zeros(int(R * 0.7))
    place(buf, note(1046.5, 0.5, "bell"), 0)
    place(buf, note(1568, 0.6, "bell", 0.9), 0.11)
    return buf


def like():
    x = t(0.16)
    f = 500 + 900 * (x / 0.16)
    ph = 2 * np.pi * np.cumsum(f) / R
    return np.sin(ph) * env(len(x), 0.002, 0.16, 4)


def hangup():
    buf = np.zeros(int(R * 0.75))
    for i, f in enumerate([620, 520, 420]):
        x = t(0.16)
        s = np.sin(2 * np.pi * f * x) * np.minimum(1, x / 0.01) * np.minimum(1, (0.16 - x) / 0.02)
        place(buf, s * 0.6, i * 0.22)
    return buf


def tap():
    n = int(R * 0.03)
    return bandpass_noise(n, 2000, 7000, 9) * env(n, 0.0005, 0.02, 6) + note(1800, 0.03, "sine", 0.3)


def unlock():
    x = t(0.28)
    f = 300 + 700 * (x / 0.28) ** 0.7
    ph = 2 * np.pi * np.cumsum(f) / R
    s = np.sin(ph) * np.sin(np.pi * x / 0.28) * 0.5
    s += bandpass_noise(len(x), 3000, 9000, 4) * np.sin(np.pi * x / 0.28) * 0.15
    return s


SOUNDS = {"shutter": shutter, "ringtone": ringtone, "ringback": ringback, "notify": notify, "like": like, "hangup": hangup,
          "tap": tap, "unlock": unlock}


def main():
    for name, fn in SOUNDS.items():
        write(name, fn())
    js = {n: {"sounds": ["celular:" + n], "subtitle": "subtitles.celular." + n} for n in SOUNDS}
    with open(os.path.join(ROOT, "sounds.json"), "w") as fh:
        json.dump(js, fh, indent=2)
    print("sonidos listos:", ", ".join(SOUNDS))


if __name__ == "__main__":
    main()
