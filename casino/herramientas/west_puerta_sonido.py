#!/usr/bin/env python3
"""Sonidos de las puertas batientes del saloon, hechos con grabaciones CC0 (dominio público, sin derechos de nadie).

    python3 casino/herramientas/west_puerta_sonido.py <carpeta_con_las_grabaciones>

Fuentes (todas CC0; se bajan con el flujo "Bajar recursos libres" a la rama descargas):
  - creaky_door_hinge.wav   OpenGameArt, "Creaky door hinge (spooky)"  https://opengameart.org/content/creaky-door-hinge-spooky
  - 0224.mp3                BigSoundBank, "Light door" (puerta liviana de madera)  https://bigsoundbank.com/light-door-s0224.html
Salen en assets/casino/sounds/west/:
  saloon_door_1..3       rechinido corto de la bisagra al empujar la puerta (tres versiones, con tono distinto)
  saloon_door_flap_1..3  golpecito suave de la hoja de madera al pasar por el centro (va más fuerte cuanto más rápido se mueve)
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "casino", "sounds", "west")


def load(path):
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-ac", "1", "-ar", str(SR), "-f", "f32le", "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype=np.float32).astype(np.float64)


def write(name, s):
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as d:
        wav = os.path.join(d, "x.wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes((np.clip(s, -1, 1) * 32767).astype(np.int16).tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "5",
                        os.path.join(OUT, name + ".ogg")], check=True)


def cut(x, t0, t1):
    return x[int(t0 * SR):int(t1 * SR)].copy()


def fade(s, fin=0.01, fout=0.12):
    n = len(s)
    a, b = int(fin * SR), int(fout * SR)
    env = np.ones(n)
    if a:
        env[:a] = np.linspace(0, 1, a)
    if b:
        env[-b:] *= np.linspace(1, 0, b) ** 1.5
    return s * env


def pitch(s, factor):
    """Sube el tono (y acorta) por un factor, remuestreando."""
    n = int(len(s) / factor)
    return np.interp(np.arange(n) * factor, np.arange(len(s)), s)


def filt(s, kind, f):
    sos = butter(4, f, btype=kind, fs=SR, output="sos")
    return sosfilt(sos, s)


def norm(s, peak=0.8):
    return s / (np.max(np.abs(s)) + 1e-9) * peak


def mix(a, b, at, gain):
    """Pone b encima de a desde el segundo 'at'."""
    i = int(at * SR)
    out = np.zeros(max(len(a), i + len(b)))
    out[:len(a)] += a
    out[i:i + len(b)] += b * gain
    return out


def main():
    src = sys.argv[1] if len(sys.argv) > 1 else "."
    hinge = load(os.path.join(src, "creaky_door_hinge.wav"))
    door = load(os.path.join(src, "0224.mp3"))
    # golpecitos de la hoja: los golpes más limpios de la puerta liviana, sin graves (madera delgada) y sin la cola del cuarto
    flaps = []
    for t in (2.00, 9.58, 13.05):
        k = filt(filt(cut(door, t, t + 0.16), "highpass", 160), "lowpass", 4200)
        flaps.append(norm(fade(k, 0.002, 0.08), 0.75))
    for i, k in enumerate(flaps):
        write(f"saloon_door_flap_{i + 1}", k)
    # rechinido: tres pedazos distintos de la bisagra, un poco más agudos (bisagra de resorte chica) y cortos, con un golpecito al final
    for i, (t0, t1, f) in enumerate(((0.28, 0.95, 1.12), (0.85, 1.50, 1.06), (1.45, 2.10, 1.18))):
        c = filt(cut(hinge, t0, t1), "highpass", 220)
        c = norm(fade(pitch(c, f), 0.015, 0.18), 0.78)
        s = mix(c, flaps[i], len(c) / SR - 0.08, 0.55)
        write(f"saloon_door_{i + 1}", norm(s, 0.8))
    print("sonidos de la puerta listos")


if __name__ == "__main__":
    main()
