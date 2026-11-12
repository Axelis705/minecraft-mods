#!/usr/bin/env python3
"""Sonidos de las carreras de caballos, hechos con código (mono, .ogg):
  race_bugle  fanfarria de clarín que llama a los caballos a la salida
  race_gate   campana y golpe metálico de los cajones al abrirse
  race_crowd  ovación del público al cruzar la meta
  race_gallop un paso de galope (se repite en el juego)

    python3 casino/herramientas/race_synth.py
"""
import math, os, random, subprocess, tempfile, wave
import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'casino', 'sounds')
R = np.random.default_rng(7)


def save(name, x):
    x = x / max(1e-9, np.max(np.abs(x))) * 0.85
    pcm = (x * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as f:
        tmp = f.name
    with wave.open(tmp, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', tmp, '-c:a', 'libvorbis', '-q:a', '5', os.path.join(OUT, name + '.ogg')], check=True)
    os.remove(tmp)


def env(n, a, d, s, r):
    e = np.ones(n) * s
    na, nd, nr = int(a * SR), int(d * SR), int(r * SR)
    e[:na] = np.linspace(0, 1, na)
    e[na:na + nd] = np.linspace(1, s, nd)
    e[-nr:] *= np.linspace(1, 0, nr)
    return e


def brass(freq, dur):
    n = int(dur * SR)
    t = np.arange(n) / SR
    vib = 1 + 0.004 * np.sin(2 * math.pi * 5.5 * t)
    x = np.zeros(n)
    for k in range(1, 9):
        x += np.sin(2 * math.pi * freq * k * vib * t) * (1 / k ** 1.1)
    return x * env(n, 0.03, 0.08, 0.8, 0.05)


def bugle():
    # fanfarria original en arpegio de clarín (Do mayor: sol, do, mi, sol)
    G4, C5, E5, G5 = 392.0, 523.25, 659.25, 783.99
    seq = [(G4, .18), (C5, .18), (E5, .18), (G5, .36), (E5, .18), (G5, .36), (0, .12), (G4, .14), (G4, .14), (C5, .18), (E5, .18),
           (G5, .18), (E5, .18), (C5, .5)]
    out = []
    for f, d in seq:
        out.append(brass(f, d) if f else np.zeros(int(d * SR)))
    x = np.concatenate(out)
    # un poco de eco de estadio
    y = x.copy()
    for delay, g in ((0.09, 0.35), (0.21, 0.2), (0.37, 0.1)):
        k = int(delay * SR)
        y[k:] += x[:-k] * g
    return y


def gate():
    n = int(1.2 * SR)
    t = np.arange(n) / SR
    bell = sum(np.sin(2 * math.pi * f * t) * a for f, a in ((880, 1), (1760 * 1.01, .5), (2640 * 1.02, .3))) * np.exp(-t * 3.5)
    clang = R.normal(0, 1, n) * np.exp(-t * 18) * 0.8
    clang2 = np.zeros(n)
    k = int(0.06 * SR)
    clang2[k:] = R.normal(0, 1, n - k) * np.exp(-t[:n - k] * 22) * 0.6
    return bell * 0.6 + clang + clang2


def crowd():
    dur = 3.5
    n = int(dur * SR)
    t = np.arange(n) / SR
    noise = R.normal(0, 1, n)
    # ruido rosado aproximado + "voces" (formantes que suben y bajan)
    spec = np.fft.rfft(noise)
    f = np.fft.rfftfreq(n, 1 / SR)
    spec *= 1 / np.maximum(1, np.sqrt(f / 200)) * ((f > 120) & (f < 6000))
    base = np.fft.irfft(spec, n)
    voices = np.zeros(n)
    for _ in range(40):
        f0 = R.uniform(180, 520)
        st = R.uniform(0, 0.6)
        ln = R.uniform(0.8, 2.6)
        m = (t > st) & (t < st + ln)
        tt = t[m] - st
        sweep = f0 * (1 + 0.25 * np.sin(math.pi * tt / ln))
        ph = 2 * math.pi * np.cumsum(sweep) / SR
        v = (np.sin(ph) + 0.4 * np.sin(2 * ph) + 0.2 * np.sin(3 * ph)) * np.sin(math.pi * tt / ln) ** 2
        voices[m] += v * R.uniform(0.2, 0.6)
    e = np.minimum(1, t / 0.25) * np.exp(-np.maximum(0, t - 1.6) * 1.3)
    claps = np.zeros(n)
    for _ in range(140):
        c = int(R.uniform(0.2, 3.2) * SR)
        ln = int(0.02 * SR)
        if c + ln < n:
            claps[c:c + ln] += R.normal(0, 1, ln) * np.exp(-np.arange(ln) / (0.004 * SR)) * 0.5
    return (base * 0.7 / np.max(np.abs(base)) + voices * 0.45 / max(1e-9, np.max(np.abs(voices))) + claps * 0.3) * e


def gallop():
    # cuatro golpes de casco (patrón de galope) sobre arena
    n = int(0.42 * SR)
    x = np.zeros(n)
    for start, g in ((0.0, 0.8), (0.07, 0.6), (0.16, 1.0), (0.23, 0.7)):
        k = int(start * SR)
        ln = int(0.07 * SR)
        tt = np.arange(ln) / SR
        thump = np.sin(2 * math.pi * 95 * tt) * np.exp(-tt * 55) + R.normal(0, 1, ln) * np.exp(-tt * 90) * 0.35
        x[k:k + ln] += thump * g
    return x


os.makedirs(OUT, exist_ok=True)
save('race_bugle', bugle())
save('race_gate', gate())
save('race_crowd', crowd())
save('race_gallop', gallop())
print('ok')
