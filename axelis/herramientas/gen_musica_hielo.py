"""Música original del Palacio de Hielo (Lobo Ártico), sintetizada con numpy (usa los instrumentos de gen_musica_torre.py):
 - music_ice_dungeon: palacio helado, lento y cristalino (mi menor, 64 bpm, celesta, vidrio, viento).
 - music_ice_boss: pelea contra el Lobo Ártico, 140 bpm, bajo galopante, timbales, coro y aullidos que se deslizan.
Cada pista es un bucle sin corte. Uso: python3 gen_musica_hielo.py <carpeta sounds>"""
import os, sys
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_musica_torre as T  # noqa: E402

SR = T.SR
note = T.note
rng = np.random.default_rng(31)


def wind(dur, vol=0.05):
    """Viento: ruido filtrado que sube y baja despacio."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = T.lowpass(rng.standard_normal(n), 900)
    x = x - T.lowpass(x, 200)
    swell = 0.55 + 0.45 * np.sin(2 * np.pi * t / dur * 2 + 1.3) * np.sin(2 * np.pi * t / 7.3)
    return x * swell * vol * 6


def glass(freqs, dur, vol=0.08):
    """Pad de vidrio: senos agudos con trémolo lento y un poco de desafinación."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for i, f in enumerate(freqs):
        for d in (-0.003, 0.003):
            out += np.sin(2 * np.pi * f * (1 + d) * t) + 0.25 * np.sin(2 * np.pi * 2 * f * (1 + d) * t)
    out *= 1 + 0.25 * np.sin(2 * np.pi * 0.7 * t)
    return out * T.env(n, dur * 0.3, 0.2, 0.9, 0.6, dur * 0.85) * vol / len(freqs)


def celesta(f, dur=1.6, vol=0.1):
    n = int(dur * SR)
    t = np.arange(n) / SR
    out = np.sin(2 * np.pi * f * t) * np.exp(-t / 0.7) + 0.35 * np.sin(2 * np.pi * f * 4 * t) * np.exp(-t / 0.15)
    out *= np.minimum(1, t / 0.002)
    return out * vol


def howl(f0, dur, vol=0.07):
    """Aullido lejano: voz 'uuu' que sube y cae con vibrato."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    shape = np.sin(np.pi * np.clip(t / dur, 0, 1)) ** 0.7
    f = f0 * (1 + 0.5 * shape) * (1 + 0.01 * np.sin(2 * np.pi * 5.5 * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    out = np.sin(ph) + 0.3 * np.sin(2 * ph) + 0.1 * np.sin(3 * ph)
    out = T.lowpass(out, 1400)
    return out * shape * vol


def dungeon():
    bpm = 64
    beat = 60 / bpm
    bar = beat * 4
    bars = 16
    tr = T.Track(bars * bar)
    # mi menor con color lidio/dórico: Em, Cmaj7, Am9, Bsus -> B
    prog = [(40, [64, 67, 71]), (36, [64, 67, 71, 72]), (45, [64, 69, 72]), (47, [64, 66, 71])] * 3 + \
           [(40, [64, 67, 71]), (36, [64, 67, 72]), (38, [62, 66, 69]), (47, [63, 66, 71])]
    tr.add(wind(bars * bar, 0.05), 0, 0.0)
    for b, (root, ch) in enumerate(prog):
        t0 = b * bar
        tr.add(glass([note(x + 12) for x in ch], bar * 1.05, 0.06), t0, -0.3)
        tr.add(T.pad([note(x) for x in ch], bar * 1.05, 0.06, 1100), t0, 0.3)
        tr.add(T.bass(note(root - 12), bar * 0.9, 0.09), t0)
        # celesta: notas sueltas, como gotas que caen
        seq = [ch[-1] + 12, ch[0] + 24, ch[1] + 12, ch[-1] + 24, ch[0] + 12]
        for k in range(6):
            if rng.random() < 0.3:
                continue
            m = seq[(b + k) % len(seq)]
            at = t0 + k * beat * 0.66 + rng.random() * 0.08
            tr.add(celesta(note(m), 1.8, 0.07), at, rng.uniform(-0.6, 0.6))
        if b % 4 == 3:
            tr.add(T.bell(note(ch[0] + 24), 3.5, 0.06), t0 + beat * 2, 0.0)
    return tr.render(0.5)


def boss():
    bpm = 140
    beat = 60 / bpm
    bar = beat * 4
    bars = 32
    tr = T.Track(bars * bar)
    prog = [(40, [64, 67, 71]), (36, [64, 67, 72]), (38, [62, 66, 69]), (35, [63, 66, 71])]
    for b in range(bars):
        root, ch = prog[b % 4]
        part = b // 8  # 0 acecho, 1 carrera, 2 coro fuerte, 3 vuelta
        t0 = b * bar
        # bajo galopante (corchea + dos semicorcheas)
        if part > 0:
            for k in range(4):
                at = t0 + k * beat
                tr.add(T.bass(note(root - 12), beat * 0.45, 0.17), at)
                tr.add(T.bass(note(root - 12 + (7 if k % 2 else 0)), beat * 0.22, 0.12), at + beat * 0.5)
                tr.add(T.bass(note(root - 12), beat * 0.22, 0.12), at + beat * 0.75)
        else:
            tr.add(T.bass(note(root - 12), bar * 0.95, 0.12), t0)
        tr.add(T.timpani(1.0, note(root), 0.45 if part else 0.3), t0)
        if part >= 1:
            tr.add(T.timpani(0.6, note(root + 7), 0.28), t0 + beat * 2)
            for k in range(8):
                tr.add(T.hit(0.08, 0.05 if k % 2 else 0.08), t0 + k * beat / 2, 0.35 if k % 2 else -0.35)
        vol = 0.06 if part == 0 else 0.09 if part != 2 else 0.12
        tr.add(T.choir([note(x - 12) for x in ch], bar * 1.02, vol), t0, -0.25)
        tr.add(glass([note(x + 12) for x in ch], bar * 1.02, 0.05), t0, 0.3)
        # arpegio de hielo (celesta en semicorcheas) desde la segunda parte
        if part >= 1:
            arp = [ch[0] + 12, ch[1] + 12, ch[2] + 12, ch[1] + 24]
            for k in range(16):
                m = arp[k % 4] + (12 if part == 2 and k >= 8 else 0)
                tr.add(celesta(note(m), 0.5, 0.045), t0 + k * beat / 4, 0.55 if k % 2 else -0.55)
        # aullidos lejanos al empezar cada frase (y en el acecho)
        if b % 8 == 0 or (part == 0 and b % 4 == 2):
            tr.add(howl(note(ch[0] - 12 + (5 if b % 16 else 0)), bar * 1.6, 0.06), t0 + beat, 0.6 if b % 16 else -0.6)
    return tr.render(0.3)


if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    T.save(dungeon(), os.path.join(out, "music_ice_dungeon.ogg"))
    T.save(boss(), os.path.join(out, "music_ice_boss.ogg"))
    print("ok")
