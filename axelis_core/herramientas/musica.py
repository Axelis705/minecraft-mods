"""Música de jefe del Coloso (tres fases), hecha con síntesis (numpy) y guardada en .ogg con ffmpeg. Bucles exactos (8 compases).

python3 herramientas/musica.py
  fase 1 "Piedra": lenta y pesada (tambores graves, cuerdas bajas, zumbido).
  fase 2 "Cristales": más rápida, bajo en corcheas, campanas de cristal en arpegio, caja.
  fase 3 "Furia": rápida, doble bombo, coro, arpegio veloz y acordes más tensos.
"""
import json
import os
import subprocess
import tempfile
import wave

import numpy as np
from scipy import signal

A = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "axelis_core")
SR = 32000
RNG = np.random.default_rng(7)


def nota(n):
    """Frecuencia de una nota MIDI."""
    return 440.0 * 2 ** ((n - 69) / 12)


def env(n, a, d, s, r, total):
    """Envolvente ADSR de n muestras (a, d, r en segundos; s nivel)."""
    e = np.zeros(n)
    ia, idd, ir = int(a * SR), int(d * SR), int(r * SR)
    sus = max(0, int(total * SR) - ia - idd)
    i = 0
    if ia:
        e[:min(n, ia)] = np.linspace(0, 1, ia)[:min(n, ia)]
    i = ia
    if idd and i < n:
        k = min(n - i, idd)
        e[i:i + k] = np.linspace(1, s, idd)[:k]
    i += idd
    if i < n:
        k = min(n - i, sus)
        e[i:i + k] = s
        i += k
    if i < n:
        k = min(n - i, ir)
        e[i:i + k] = np.linspace(s, 0, ir)[:k]
    return e


def osc(kind, f, n, det=0.0):
    t = np.arange(n) / SR
    ph = (f * (1 + det)) * t
    if kind == "sin":
        return np.sin(2 * np.pi * ph)
    if kind == "saw":
        return 2 * (ph % 1) - 1
    if kind == "sq":
        return np.sign(np.sin(2 * np.pi * ph))
    return np.zeros(n)


def lp(x, fc, order=2):
    b, a = signal.butter(order, min(0.99, fc / (SR / 2)), "low")
    return signal.lfilter(b, a, x)


def hp(x, fc, order=2):
    b, a = signal.butter(order, min(0.99, fc / (SR / 2)), "high")
    return signal.lfilter(b, a, x)


def bp(x, lo, hi):
    b, a = signal.butter(2, [lo / (SR / 2), min(0.99, hi / (SR / 2))], "band")
    return signal.lfilter(b, a, x)


class Pista:
    def __init__(self, bpm, compases):
        self.beat = 60.0 / bpm
        self.n = int(round(compases * 4 * self.beat * SR))
        self.buf = np.zeros(self.n + SR * 3)

    def add(self, x, t, vol=1.0):
        i = int(t * SR)
        self.buf[i:i + len(x)] += x * vol

    def final(self):
        # lo que se pasa del final vuelve al principio (bucle sin cortes)
        out = self.buf[:self.n].copy()
        cola = self.buf[self.n:]
        out[:len(cola)] += cola[:min(len(cola), self.n)]
        return out


# ------------------------------------------------------------------ instrumentos

def bombo(f0=110, f1=38, dur=0.45):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = f1 + (f0 - f1) * np.exp(-t * 28)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 7)
    x += 0.3 * lp(RNG.standard_normal(n), 900) * np.exp(-t * 60)
    return np.tanh(x * 1.6)


def tambor(f=72, dur=0.9):
    n = int(dur * SR)
    t = np.arange(n) / SR
    fr = f * (1 + 0.6 * np.exp(-t * 18))
    x = np.sin(2 * np.pi * np.cumsum(fr) / SR) * np.exp(-t * 4.2)
    x += 0.25 * bp(RNG.standard_normal(n), 80, 600) * np.exp(-t * 9)
    return np.tanh(x * 1.3)


def caja(dur=0.3):
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = 0.7 * bp(RNG.standard_normal(n), 1200, 7000) * np.exp(-t * 18)
    x += 0.5 * np.sin(2 * np.pi * 190 * t) * np.exp(-t * 25)
    return x


def platillo(dur=0.06, vol=0.35):
    n = int(dur * SR)
    t = np.arange(n) / SR
    return vol * hp(RNG.standard_normal(n), 7000) * np.exp(-t * 70)


def metal(f=220, dur=1.6):
    """Golpe metálico grave (como un yunque lejano)."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = sum(np.sin(2 * np.pi * f * r * t) * np.exp(-t * (2.5 + r)) / (1 + i) for i, r in enumerate((1, 2.76, 5.4, 8.9)))
    return 0.5 * x


def campana(f, dur=1.2, idx=2.2):
    """Campana de cristal (FM)."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    mod = np.sin(2 * np.pi * f * 3.5 * t) * idx * np.exp(-t * 6)
    x = np.sin(2 * np.pi * f * t + mod) * np.exp(-t * 3.2)
    return 0.45 * x


def cuerda(f, dur, vol=0.5, brillo=900):
    n = int((dur + 0.6) * SR)
    x = osc("saw", f, n) + osc("saw", f, n, 0.004) + 0.6 * osc("saw", f / 2, n, -0.003)
    x = lp(x, brillo) * env(n, 0.25, 0.3, 0.8, 0.6, dur)
    return vol * x


def bajo(f, dur, vol=0.6, brillo=500):
    n = int((dur + 0.08) * SR)
    x = osc("saw", f, n) + 0.8 * osc("sq", f / 2, n)
    x = lp(x, brillo, 3) * env(n, 0.005, 0.12, 0.6, 0.06, dur)
    return vol * np.tanh(x * 1.4)


def coro(fs, dur, vol=0.35):
    n = int((dur + 1.0) * SR)
    x = np.zeros(n)
    for f in fs:
        for d in (-0.006, 0, 0.005):
            x += osc("saw", f, n, d)
    # dos "vocales" (formantes) para que suene a voces
    x = 0.6 * bp(x, 550, 900) + 0.4 * bp(x, 1000, 1500) + 0.2 * lp(x, 400)
    return vol * x * env(n, 0.6, 0.4, 0.85, 1.0, dur) / len(fs)


def zumbido(f, dur, vol=0.35):
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = osc("saw", f, n) + osc("saw", f, n, 0.003) + 0.5 * osc("sin", f / 2, n)
    lfo = 300 + 200 * (1 + np.sin(2 * np.pi * t / dur * 2)) / 2
    y = np.zeros(n)
    # filtro que se abre y cierra despacio (por pedazos)
    blk = SR // 10
    for i in range(0, n, blk):
        y[i:i + blk] = lp(x[i:i + blk + 64], lfo[i])[:len(y[i:i + blk])]
    return vol * y


def eco(x, s=1.4, mezcla=0.28):
    """Reverberación sencilla (ruido que se apaga)."""
    n = int(s * SR)
    t = np.arange(n) / SR
    ir = RNG.standard_normal(n) * np.exp(-t * 4.0)
    ir = lp(ir, 3500)
    ir /= np.sqrt(np.sum(ir ** 2))
    w = signal.fftconvolve(x, ir)[:len(x)]
    # la cola que se pasa vuelve al principio (bucle)
    full = signal.fftconvolve(x, ir)
    if len(full) > len(x):
        w[:len(full) - len(x)] += full[len(x):][:len(x)]
    return x * (1 - mezcla) + w * mezcla * 1.8


# ------------------------------------------------------------------ las tres fases

# re menor: Dm - Bb - Gm - A (dos compases cada uno)
ACORDES = [(50, (62, 65, 69)), (46, (58, 62, 65)), (43, (55, 58, 62)), (45, (57, 61, 64))]
TENSOS = [(50, (62, 65, 68)), (46, (58, 62, 65)), (43, (55, 58, 61)), (45, (57, 61, 64))]


def fase1():
    p = Pista(88, 8)
    b = p.beat
    p.add(zumbido(nota(38), p.n / SR + 0.5), 0, 0.6)
    for c in range(8):
        raiz, ac = ACORDES[c // 2]
        t0 = c * 4 * b
        p.add(tambor(70), t0, 0.9)
        p.add(tambor(70), t0 + 2 * b, 0.7)
        p.add(tambor(58), t0 + 3.5 * b, 0.55)
        if c % 2 == 0:
            p.add(cuerda(nota(raiz - 12), 8 * b - 0.2, 0.5, 700), t0)
            p.add(cuerda(nota(ac[0] - 12), 8 * b - 0.2, 0.28, 900), t0)
        if c % 4 == 3:
            p.add(metal(nota(raiz - 2)), t0 + 3 * b, 0.4)
    # melodía grave y lenta
    mel = [62, 65, 64, 62, 58, 57, 55, 57]
    for i, m in enumerate(mel):
        p.add(cuerda(nota(m - 12), 3.6 * b, 0.3, 1400), i * 4 * b + 0.5 * b)
    return eco(p.final(), 1.6, 0.3)


def fase2():
    p = Pista(112, 8)
    b = p.beat
    for c in range(8):
        raiz, ac = ACORDES[c // 2]
        t0 = c * 4 * b
        for k in range(4):
            p.add(bombo(), t0 + k * b, 0.9 if k % 2 == 0 else 0.6)
        p.add(caja(), t0 + b, 0.55)
        p.add(caja(), t0 + 3 * b, 0.55)
        for k in range(8):
            p.add(platillo(), t0 + k * b / 2, 0.8 if k % 2 else 0.4)
            p.add(bajo(nota(raiz - 12 + (12 if k % 4 == 3 else 0)), b / 2 - 0.03, 0.5), t0 + k * b / 2)
        # campanas de cristal en arpegio (semicorcheas)
        arp = [ac[0], ac[1], ac[2], ac[1] + 12, ac[2], ac[1], ac[0] + 12, ac[2]]
        for k in range(16):
            f = nota(arp[k % 8] + 12)
            p.add(campana(f, 0.9, 1.8), t0 + k * b / 4, 0.32 if k % 4 == 0 else 0.2)
        if c % 2 == 0:
            p.add(cuerda(nota(ac[0]), 8 * b - 0.3, 0.22, 1800), t0)
    return eco(p.final(), 1.3, 0.25)


def fase3():
    p = Pista(132, 8)
    b = p.beat
    for c in range(8):
        raiz, ac = TENSOS[c // 2]
        t0 = c * 4 * b
        for k in range(8):
            p.add(bombo(120, 40, 0.3), t0 + k * b / 2, 0.85 if k % 2 == 0 else 0.55)
            p.add(bajo(nota(raiz - 12 + (7 if k in (3, 7) else 0)), b / 2 - 0.02, 0.55, 700), t0 + k * b / 2)
        p.add(caja(), t0 + b, 0.7)
        p.add(caja(), t0 + 3 * b, 0.7)
        p.add(caja(0.2), t0 + 3.75 * b, 0.35)
        for k in range(16):
            p.add(platillo(0.05, 0.3), t0 + k * b / 4)
        arp = [ac[0], ac[1], ac[2], ac[0] + 12, ac[2] + 12, ac[0] + 12, ac[2], ac[1]]
        for k in range(16):
            p.add(campana(nota(arp[k % 8] + 12), 0.6, 2.6), t0 + k * b / 4, 0.25)
        if c % 2 == 0:
            p.add(coro([nota(x) for x in ac], 8 * b - 0.2, 0.55), t0)
            p.add(metal(nota(raiz)), t0, 0.35)
    return eco(p.final(), 1.1, 0.22)


def guardar(x, nombre):
    x = x - np.mean(x)
    x = np.tanh(x / (np.max(np.abs(x)) + 1e-9) * 1.3)
    x = x / np.max(np.abs(x)) * 0.89
    d = os.path.join(A, "sounds", "musica")
    os.makedirs(d, exist_ok=True)
    tmp = os.path.join(tempfile.gettempdir(), nombre + ".wav")
    with wave.open(tmp, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((x * 32767).astype("<i2").tobytes())
    out = os.path.join(d, nombre + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis", "-q:a", "3", out], check=True)
    print(nombre, round(len(x) / SR, 1), "s", os.path.getsize(out) // 1024, "KB")


def sonidos_json():
    p = os.path.join(A, "sounds.json")
    d = json.load(open(p, encoding="utf-8")) if os.path.exists(p) else {}
    for i in (1, 2, 3):
        d[f"musica.coloso_{i}"] = {"sounds": [{"name": f"axelis_core:musica/coloso_{i}", "stream": True}]}
    with open(p, "w", encoding="utf-8") as fh:
        json.dump(d, fh, indent=2, ensure_ascii=False)


if __name__ == "__main__":
    guardar(fase1(), "coloso_1")
    guardar(fase2(), "coloso_2")
    guardar(fase3(), "coloso_3")
    sonidos_json()
