"""Música original de la Torre del Archimago, sintetizada con numpy (sin muestras externas):
 - music_tower_dungeon: biblioteca, tranquila y misteriosa (re menor, 70 bpm, campanitas y coro suave).
 - music_tower_boss: pelea, 132 bpm, bajo insistente, timbales, coro y arpegios de clavecín.
Cada pista es un bucle sin corte (la cola de la reverb se suma al principio). Uso: python3 gen_musica_torre.py <carpeta sounds>"""
import os, subprocess, sys
import numpy as np

SR = 44100
rng = np.random.default_rng(7)


def note(n):
    """Número MIDI -> frecuencia."""
    return 440.0 * 2 ** ((n - 69) / 12.0)


def env(n, a, d, s, r, total):
    """Envolvente ADSR de n muestras (tiempos en segundos; total = duración de la nota antes de soltar)."""
    t = np.arange(n) / SR
    e = np.ones(n) * s
    e[t < a] = t[t < a] / max(a, 1e-4)
    m = (t >= a) & (t < a + d)
    e[m] = 1 - (1 - s) * (t[m] - a) / max(d, 1e-4)
    rel = t >= total
    e[rel] = s * np.exp(-(t[rel] - total) / max(r, 1e-4))
    return e


def lowpass(x, cut):
    a = np.exp(-2 * np.pi * cut / SR)
    y = np.empty_like(x)
    acc = 0.0
    # filtro de un polo (bucle en bloques con numpy sería más rápido, pero las pistas son cortas)
    from scipy.signal import lfilter  # noqa
    return lfilter([1 - a], [1, -a], x)


def saw(f, n, harm=14, detune=0.0):
    t = np.arange(n) / SR
    out = np.zeros(n)
    for k in range(1, harm + 1):
        if f * k > SR / 2.2:
            break
        out += np.sin(2 * np.pi * f * k * t * (1 + detune)) / k
    return out


def pad(freqs, dur, vol=0.12, cut=1600):
    n = int(dur * SR)
    out = np.zeros(n)
    for f in freqs:
        for dt in (-0.004, 0.0, 0.0045):
            out += saw(f, n, 10, dt)
    out = lowpass(out, cut)
    return out * env(n, dur * 0.35, 0.2, 0.85, 0.6, dur * 0.8) * vol / len(freqs)


def choir(freqs, dur, vol=0.1):
    """Coro: senos con vibrato y formantes suaves (vocal 'aah')."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for f in freqs:
        vib = 1 + 0.004 * np.sin(2 * np.pi * 5.2 * t + rng.random() * 6)
        for k, a in ((1, 1.0), (2, 0.5), (3, 0.35), (4, 0.18), (5, 0.12)):
            out += a * np.sin(2 * np.pi * f * k * np.cumsum(vib) / SR)
    out = lowpass(out, 2200)
    return out * env(n, dur * 0.4, 0.2, 0.9, 0.8, dur * 0.85) * vol / len(freqs)


def bell(f, dur=2.5, vol=0.15):
    n = int(dur * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for ratio, a, dec in ((1, 1.0, 1.4), (2.76, 0.45, 0.7), (5.4, 0.25, 0.35), (8.93, 0.12, 0.2)):
        out += a * np.sin(2 * np.pi * f * ratio * t) * np.exp(-t / dec)
    out *= np.minimum(1, t / 0.003)
    return out * vol


def harpsi(f, dur=0.3, vol=0.08):
    n = int((dur + 0.4) * SR)
    t = np.arange(n) / SR
    out = saw(f, n, 18) * np.exp(-t / 0.25)
    return lowpass(out, 5000) * vol


def bass(f, dur, vol=0.22):
    n = int((dur + 0.05) * SR)
    out = saw(f, n, 12) + 0.5 * np.sin(2 * np.pi * f / 2 * np.arange(n) / SR)
    out = lowpass(out, 500)
    return out * env(n, 0.005, 0.08, 0.6, 0.05, dur) * vol


def timpani(dur=1.2, f0=98.0, vol=0.5):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = f0 * (1 + 0.15 * np.exp(-t / 0.05))
    out = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.45)
    out += lowpass(rng.standard_normal(n), 900) * np.exp(-t / 0.04) * 0.4
    return out * vol


def hit(dur=0.2, vol=0.18):
    n = int(dur * SR)
    t = np.arange(n) / SR
    out = rng.standard_normal(n) * np.exp(-t / 0.05)
    out = out - lowpass(out, 1500)
    return out * vol


def reverb(x, wet=0.35):
    from scipy.signal import lfilter
    out = np.zeros_like(x)
    for d, g in ((1557, 0.84), (1617, 0.83), (1491, 0.85), (1422, 0.86), (1277, 0.87), (1356, 0.86)):
        b = np.zeros(d + 1)
        b[0] = 1
        a = np.zeros(d + 1)
        a[0] = 1
        a[d] = -g
        out += lfilter([1], a, x)
    out /= 6
    for d, g in ((225, 0.5), (556, 0.5), (441, 0.5)):
        b = np.zeros(d + 1)
        b[0] = -g
        b[d] = 1
        a = np.zeros(d + 1)
        a[0] = 1
        a[d] = -g
        out = lfilter(b, a, out)
    return x * (1 - wet) + out * wet


class Track:
    def __init__(self, seconds):
        self.n = int(seconds * SR)
        self.tail = int(6 * SR)
        self.buf = np.zeros((2, self.n + self.tail))

    def add(self, sig, at, pan=0.0):
        i = int(at * SR)
        j = min(i + len(sig), self.buf.shape[1])
        if j <= i:
            return
        l = np.cos((pan + 1) * np.pi / 4) * 1.41
        r = np.sin((pan + 1) * np.pi / 4) * 1.41
        self.buf[0, i:j] += sig[:j - i] * min(1, l)
        self.buf[1, i:j] += sig[:j - i] * min(1, r)

    def render(self, wet):
        out = np.stack([reverb(self.buf[0], wet), reverb(self.buf[1], wet)])
        loop = out[:, :self.n].copy()
        loop[:, :self.tail] += out[:, self.n:self.n + self.tail]
        peak = np.max(np.abs(loop))
        return loop / peak * 0.85


def dungeon():
    bpm = 70
    beat = 60 / bpm
    bar = beat * 4
    bars = 16
    tr = Track(bars * bar)
    # Re menor misterioso: Dm, Bb, Gm, A (y una vez C en lugar de A)
    prog = [(50, [62, 65, 69]), (46, [62, 65, 70]), (43, [62, 67, 70]), (45, [61, 64, 69])] * 3 + \
           [(50, [62, 65, 69]), (46, [62, 65, 70]), (48, [60, 64, 67]), (45, [61, 64, 69])]
    for b, (root, ch) in enumerate(prog):
        t0 = b * bar
        tr.add(pad([note(x) for x in ch], bar * 1.05, 0.10, 1300), t0, -0.2)
        tr.add(choir([note(x + 12) for x in ch[:2]], bar * 1.05, 0.05), t0, 0.25)
        tr.add(bass(note(root - 12), bar * 0.9, 0.12), t0)
        # campanitas: un arpegio lento distinto en cada compás
        seq = [ch[0] + 12, ch[2] + 12, ch[1] + 24, ch[2] + 12] if b % 2 == 0 else [ch[2] + 12, ch[1] + 12, ch[0] + 24, ch[1] + 24]
        for k, m in enumerate(seq):
            if (b + k) % 5 == 4:
                continue
            tr.add(bell(note(m), 3.0, 0.07), t0 + k * beat + (0.5 * beat if k == 3 else 0), 0.5 if k % 2 else -0.5)
    return tr.render(0.45)


def boss():
    bpm = 132
    beat = 60 / bpm
    bar = beat * 4
    bars = 32
    tr = Track(bars * bar)
    prog = [(38, [62, 65, 69]), (34, [62, 65, 70]), (36, [60, 64, 67]), (33, [61, 64, 69])]
    for b in range(bars):
        root, ch = prog[b % 4]
        part = b // 8  # 0 intro tensa, 1 entra todo, 2 coro fuerte, 3 vuelta
        t0 = b * bar
        # bajo en corcheas (re-re-la-re...)
        pat = [0, 0, 12, 0, 0, 7, 0, 12]
        if part > 0:
            for k, iv in enumerate(pat):
                tr.add(bass(note(root + iv), beat * 0.45, 0.16), t0 + k * beat / 2)
        else:
            tr.add(bass(note(root), bar * 0.95, 0.12), t0)
        # timbales
        tr.add(timpani(1.0, note(root + 12 if root < 36 else root), 0.45 if part else 0.3), t0)
        if part >= 1:
            tr.add(timpani(0.8, note(root + 19), 0.25), t0 + beat * 2.5)
        if part >= 1 and b % 2 == 1:
            tr.add(timpani(0.5, note(root + 12), 0.3), t0 + beat * 3.5)
        # golpecitos en contratiempo
        if part >= 1:
            for k in range(4):
                tr.add(hit(0.15, 0.08), t0 + k * beat + beat / 2, 0.3 if k % 2 else -0.3)
        # coro y cuerdas
        vol = 0.06 if part == 0 else 0.09 if part != 2 else 0.12
        tr.add(choir([note(x) for x in ch], bar * 1.02, vol), t0, -0.3)
        tr.add(pad([note(x - 12) for x in ch], bar * 1.02, 0.07, 2200), t0, 0.3)
        # arpegios de clavecín (semicorcheas) desde la segunda parte
        if part >= 1:
            arp = [ch[0] + 12, ch[1] + 12, ch[2] + 12, ch[1] + 24]
            for k in range(16):
                m = arp[k % 4] + (12 if part == 2 and k >= 8 else 0)
                tr.add(harpsi(note(m), beat / 4, 0.05), t0 + k * beat / 4, 0.55 if k % 2 else -0.55)
        # campana en el primer tiempo de cada frase
        if b % 4 == 0:
            tr.add(bell(note(ch[2] + 24), 2.5, 0.08), t0, 0.0)
    return tr.render(0.25)


def save(data, path):
    # 5 ms de fundido en los bordes para que el bucle no chasquee
    k = int(0.005 * SR)
    ramp = np.linspace(0, 1, k)
    data = data.copy()
    data[:, :k] *= ramp
    data[:, -k:] *= ramp[::-1]
    raw = path + ".raw"
    (np.clip(data.T, -1, 1) * 32767).astype("<i2").tofile(raw)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(SR), "-ac", "2", "-i", raw,
                    "-c:a", "libvorbis", "-q:a", "3", path], check=True)
    os.remove(raw)


if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    save(dungeon(), os.path.join(out, "music_tower_dungeon.ogg"))
    save(boss(), os.path.join(out, "music_tower_boss.ogg"))
    print("ok")
