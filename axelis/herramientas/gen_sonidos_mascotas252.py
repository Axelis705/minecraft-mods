"""Sonidos de las 8 mascotas nuevas de la etapa 252, sintetizados con numpy/scipy (sin muestras externas). Uso:
    python3 gen_sonidos_mascotas252.py <carpeta sounds>
Salen como relic_comp_<id>_<act|cast|end>.ogg, mono (los sonidos con posición en Minecraft tienen que ser mono).
 - act: lo que hace al atacar o al ayudar (corto)
 - cast: su habilidad grande
 - end: cuando se despide (se guarda la figurita)
"""
import os, subprocess, sys
import numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
rng = np.random.default_rng(252)


def t_of(n):
    return np.arange(n) / SR


def N(d):
    return int(d * SR)


def bp(x, lo, hi, order=2):
    sos = butter(order, [max(20, lo), min(hi, SR / 2.1)], btype="bandpass", fs=SR, output="sos")
    return sosfilt(sos, x)


def lp(x, cut, order=2):
    return sosfilt(butter(order, min(cut, SR / 2.1), btype="low", fs=SR, output="sos"), x)


def hp(x, cut, order=2):
    return sosfilt(butter(order, cut, btype="high", fs=SR, output="sos"), x)


def noise(n):
    return rng.standard_normal(n)


def env(n, a, r, curve=1.0):
    """Sube en a segundos y cae hasta 0 al final (curva exponencial suave)."""
    e = np.ones(n)
    ka = max(1, N(a))
    e[:ka] = np.linspace(0, 1, ka)
    rest = n - ka
    if rest > 0:
        e[ka:] = (1 - np.linspace(0, 1, rest)) ** (curve * 2.0)
    return e


def glide(f0, f1, n, shape="sin", harm=8):
    f = np.geomspace(f0, f1, n)
    ph = 2 * np.pi * np.cumsum(f) / SR
    if shape == "sin":
        return np.sin(ph)
    out = np.zeros(n)
    for k in range(1, harm + 1):
        amp = 1 / k if shape == "saw" else (1 / k if k % 2 else 0)
        out += np.sin(k * ph) * amp * (f * k < SR / 2.2)
    return out


def bell(f, dur, decay=3.0, partials=((1, 1), (2.76, 0.45), (5.4, 0.25), (8.9, 0.12))):
    n = N(dur)
    t = t_of(n)
    out = np.zeros(n)
    for m, a in partials:
        out += a * np.sin(2 * np.pi * f * m * t) * np.exp(-t * decay * (0.7 + m * 0.3))
    return out


def chimes(dur, base, count=6, spread=2.0, seed=0):
    r = np.random.default_rng(seed)
    n = N(dur)
    out = np.zeros(n)
    for i in range(count):
        st = int(r.uniform(0, 0.55) * n)
        f = base * spread ** r.uniform(0, 1)
        b = bell(f, (n - st) / SR, decay=5.0) * r.uniform(0.4, 1.0)
        k = min(len(b), n - st)
        out[st:st + k] += b[:k]
    return out


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[: len(p)] += p
    return out


def at(x, start, total):
    out = np.zeros(N(total))
    s = N(start)
    k = min(len(x), len(out) - s)
    out[s:s + k] += x[:k]
    return out


def reverb(x, wet=0.25, decay=0.5):
    out = x.copy()
    for i, d in enumerate((0.037, 0.053, 0.071, 0.097, 0.131)):
        k = N(d)
        y = np.zeros_like(x)
        y[k:] = x[:-k] * decay ** (i + 1)
        out += y * wet
    return out


def norm(x, peak=0.88):
    m = np.max(np.abs(x))
    return x * (peak / m) if m > 0 else x


def thump(dur, f0=120, f1=45):
    n = N(dur)
    return glide(f0, f1, n) * env(n, 0.003, 0, 1.6)


def click(dur=0.03, lo=1500, hi=6000):
    n = N(dur)
    return bp(noise(n), lo, hi) * env(n, 0.001, 0, 2.0)


def voice(f0, f1, dur, forms=(500, 1800, 2600), vib=6.0):
    n = N(dur)
    t = t_of(n)
    f = np.geomspace(f0, f1, n) * (1 + 0.015 * np.sin(2 * np.pi * vib * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    src = np.zeros(n)
    for k in range(1, 30):
        src += np.sin(k * ph) / k * (f * k < SR / 2.2)
    out = np.zeros(n)
    for i, fc in enumerate(forms):
        out += bp(src, fc * 0.85, fc * 1.15) * (1.0, 0.55, 0.25)[i]
    return out


# ------------------------------------------------------------------ tiburoncito de tierra
def land_shark_act():
    total = 0.35
    bite = mix(click(0.04, 800, 4000) * 1.0, at(click(0.05, 600, 3500), 0.07, 0.12) * 0.9)
    dirt = lp(noise(N(0.3)), 900) * env(N(0.3), 0.01, 0, 1.2) * 0.5
    return norm(mix(at(bite, 0.0, total), at(thump(0.25, 140, 55), 0.0, total) * 0.8, at(dirt, 0.02, total)))


def land_shark_cast():
    total = 0.9
    rumble = lp(noise(N(0.8)), 220) * env(N(0.8), 0.25, 0, 0.8) * 1.4
    jaws = mix(click(0.05, 700, 3000), at(click(0.06, 500, 2800), 0.08, 0.15))
    burst = lp(noise(N(0.4)), 1600) * env(N(0.4), 0.005, 0, 1.4)
    return norm(reverb(mix(rumble, at(thump(0.45, 110, 38), 0.42, total) * 1.2, at(jaws, 0.45, total), at(burst, 0.44, total) * 0.7)))


def land_shark_end():
    n = N(0.6)
    grit = bp(noise(n), 300, 1800) * env(n, 0.05, 0, 1.0) * (0.6 + 0.4 * np.sin(2 * np.pi * 14 * t_of(n)))
    return norm(mix(grit, thump(0.3, 90, 40) * 0.6))


# ------------------------------------------------------------------ nubecita
def cloud_act():
    total = 0.4
    zap = hp(noise(N(0.18)), 2500) * env(N(0.18), 0.002, 0, 1.5) * (np.sign(np.sin(2 * np.pi * 60 * t_of(N(0.18)))) * 0.5 + 0.5)
    return norm(mix(at(zap, 0, total), at(glide(1800, 600, N(0.2)) * env(N(0.2), 0.005, 0, 1.2) * 0.4, 0.0, total),
                    at(thump(0.2, 90, 50) * 0.4, 0.02, total)))


def cloud_cast():
    total = 1.4
    n = N(1.3)
    rain = hp(noise(n), 3000) * env(n, 0.2, 0, 0.6) * 0.35
    drops = np.zeros(n)
    for i in range(40):
        st = int(rng.uniform(0, 0.9) * n)
        d = click(0.02, 2500, 7000) * rng.uniform(0.2, 0.7)
        k = min(len(d), n - st)
        drops[st:st + k] += d[:k]
    return norm(reverb(mix(rain, drops, at(chimes(0.9, 880, 7, seed=4) * 0.6, 0.35, total))))


def cloud_end():
    n = N(0.5)
    puff = bp(noise(n), 400, 2500) * env(n, 0.04, 0, 1.3)
    return norm(mix(puff, at(bell(1320, 0.4, 6) * 0.25, 0.05, 0.5)))


# ------------------------------------------------------------------ gatito cometa
def comet_cat_act():
    total = 0.45
    mew = voice(700, 980, 0.22, forms=(900, 2200, 3200)) * env(N(0.22), 0.02, 0, 1.0)
    return norm(mix(at(mew, 0, total), at(chimes(0.35, 1760, 3, seed=1) * 0.35, 0.08, total)))


def comet_cat_cast():
    total = 1.4
    n = N(0.9)
    whoosh = bp(noise(n), 800, 6000) * env(n, 0.4, 0, 1.2) * 0.6
    whoosh *= np.linspace(0.3, 1, n)
    rise = glide(500, 2400, n) * env(n, 0.3, 0, 1.0) * 0.25
    return norm(reverb(mix(whoosh, rise, at(chimes(0.9, 1046, 9, seed=7), 0.5, total), at(bell(2093, 0.8, 3) * 0.5, 0.52, total)), 0.3))


def comet_cat_end():
    total = 0.8
    n = N(0.55)
    purr = lp(noise(n), 300) * (0.5 + 0.5 * np.sin(2 * np.pi * 24 * t_of(n))) * env(n, 0.05, 0, 0.8)
    return norm(mix(purr * 1.2, at(chimes(0.5, 2093, 4, seed=2) * 0.3, 0.2, total)))


# ------------------------------------------------------------------ pulpo astronauta
def bubbles(dur, count, lo=300, hi=900, seed=0):
    r = np.random.default_rng(seed)
    n = N(dur)
    out = np.zeros(n)
    for i in range(count):
        st = int(r.uniform(0, 0.8) * n)
        d = r.uniform(0.04, 0.09)
        f = r.uniform(lo, hi)
        b = glide(f, f * 2.2, N(d)) * env(N(d), 0.005, 0, 1.2)
        k = min(len(b), n - st)
        out[st:st + k] += b[:k] * r.uniform(0.4, 1.0)
    return out


def astro_octopus_act():
    total = 0.4
    squish = lp(noise(N(0.25)), 700) * env(N(0.25), 0.01, 0, 1.2) * (0.6 + 0.4 * np.sin(2 * np.pi * 35 * t_of(N(0.25))))
    return norm(mix(at(squish, 0, total), at(bubbles(0.3, 5, seed=3) * 0.5, 0.05, total)))


def astro_octopus_cast():
    total = 1.0
    notes = [523, 659, 784, 1046]
    arp = np.zeros(N(total))
    for i, f in enumerate(notes):
        b = (glide(f, f, N(0.18), "sq", 6) * 0.3) * env(N(0.18), 0.005, 0, 1.0)
        arp += at(b, i * 0.1, total)
    return norm(reverb(mix(arp, bubbles(total, 14, 400, 1200, seed=5) * 0.4)))


def astro_octopus_end():
    total = 0.5
    b1 = glide(1200, 1200, N(0.08), "sq", 5) * env(N(0.08), 0.003, 0, 0.5) * 0.4
    b2 = glide(800, 800, N(0.12), "sq", 5) * env(N(0.12), 0.003, 0, 0.6) * 0.4
    return norm(mix(at(b1, 0, total), at(b2, 0.12, total), at(lp(noise(N(0.2)), 2000) * 0.08, 0.0, total)))


# ------------------------------------------------------------------ robot tostadora
def toaster_act():
    total = 0.5
    n = N(0.32)
    t = t_of(n)
    boing = np.sin(2 * np.pi * np.cumsum(220 + 90 * np.sin(2 * np.pi * 18 * t) * np.exp(-t * 6)) / SR) * env(n, 0.003, 0, 1.0)
    pop = click(0.03, 1200, 5000)
    return norm(mix(at(pop, 0, total) * 1.2, at(boing * 0.7, 0.01, total)))


def toaster_cast():
    total = 1.2
    ding = bell(1568, 1.1, 2.2, ((1, 1), (2.0, 0.3), (3.0, 0.15), (4.2, 0.08)))
    clunk = mix(click(0.04, 400, 2500), thump(0.12, 160, 80) * 0.5)
    return norm(reverb(mix(at(clunk, 0, total), at(ding, 0.12, total))))


def toaster_end():
    total = 0.7
    n = N(0.6)
    down = glide(900, 160, n, "sq", 6) * env(n, 0.01, 0, 0.7) * 0.4
    return norm(mix(down, at(click(0.03, 600, 3000) * 0.6, 0.58, total)))


# ------------------------------------------------------------------ murcielaguito vampiro
def vampire_bat_act():
    total = 0.35
    sq = glide(3200, 4600, N(0.07)) * env(N(0.07), 0.004, 0, 1.0)
    sq2 = glide(3600, 5000, N(0.06)) * env(N(0.06), 0.004, 0, 1.0)
    bite = click(0.04, 900, 4000)
    return norm(mix(at(sq * 0.6, 0, total), at(sq2 * 0.5, 0.09, total), at(bite, 0.16, total)))


def vampire_bat_cast():
    total = 1.2
    out = np.zeros(N(total))
    for i in range(5):
        p = glide(2600, 2600, N(0.05)) * env(N(0.05), 0.003, 0, 1.0) * (0.9 ** i)
        out += at(p, i * 0.17, total)
    low = lp(noise(N(total)), 200) * env(N(total), 0.2, 0, 0.7) * 0.5
    return norm(reverb(mix(out, low), 0.45, 0.6))


def vampire_bat_end():
    total = 0.6
    n = N(total)
    t = t_of(n)
    flap = bp(noise(n), 300, 2000) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 12 * t))) * env(n, 0.02, 0, 1.0)
    return norm(flap)


# ------------------------------------------------------------------ capibara zen
def zen_capybara_act():
    n = N(0.4)
    hum = voice(150, 135, 0.4, forms=(330, 700, 2500)) * env(n, 0.06, 0, 0.8)
    return norm(hum)


def zen_capybara_cast():
    total = 2.0
    n = N(total)
    t = t_of(n)
    bowl = (np.sin(2 * np.pi * 392 * t) + 0.6 * np.sin(2 * np.pi * 394.5 * t) + 0.35 * np.sin(2 * np.pi * 1083 * t)
            + 0.15 * np.sin(2 * np.pi * 2093 * t)) * np.exp(-t * 1.6)
    strike = click(0.02, 800, 3000) * 0.3
    return norm(reverb(mix(bowl * env(n, 0.01, 0, 0.3), strike), 0.3, 0.55))


def zen_capybara_end():
    n = N(0.8)
    sigh = bp(noise(n), 300, 1600) * env(n, 0.15, 0, 0.8) * np.linspace(1, 0.4, n)
    return norm(mix(sigh, voice(170, 120, 0.8, forms=(500, 900, 2400)) * env(n, 0.2, 0, 0.8) * 0.15))


# ------------------------------------------------------------------ dragoncito de cristal
def crystal_dragon_act():
    total = 0.55
    tink = bell(2637, 0.4, 7.0) * 0.7
    roar = voice(320, 240, 0.3, forms=(700, 1200, 2600)) * env(N(0.3), 0.02, 0, 1.0) * 0.5
    return norm(mix(at(roar, 0, total), at(tink, 0.04, total)))


def crystal_dragon_cast():
    total = 1.6
    chord = mix(bell(1318, 1.4, 2.0), bell(1661, 1.4, 2.0) * 0.8, bell(1976, 1.4, 2.0) * 0.7)
    shim = hp(noise(N(1.2)), 5000) * env(N(1.2), 0.3, 0, 1.0) * 0.12
    return norm(reverb(mix(at(chord, 0.05, total), shim, at(chimes(1.0, 2637, 8, seed=9) * 0.4, 0.2, total)), 0.35))


def crystal_dragon_end():
    total = 0.9
    shatter = hp(noise(N(0.2)), 3500) * env(N(0.2), 0.002, 0, 1.6) * 0.6
    return norm(reverb(mix(shatter, at(chimes(0.8, 1976, 10, seed=11), 0.02, total))))


SOUNDS = {
    "land_shark": (land_shark_act, land_shark_cast, land_shark_end),
    "cloud": (cloud_act, cloud_cast, cloud_end),
    "comet_cat": (comet_cat_act, comet_cat_cast, comet_cat_end),
    "astro_octopus": (astro_octopus_act, astro_octopus_cast, astro_octopus_end),
    "toaster": (toaster_act, toaster_cast, toaster_end),
    "vampire_bat": (vampire_bat_act, vampire_bat_cast, vampire_bat_end),
    "zen_capybara": (zen_capybara_act, zen_capybara_cast, zen_capybara_end),
    "crystal_dragon": (crystal_dragon_act, crystal_dragon_cast, crystal_dragon_end),
}


def save(x, path):
    k = int(0.004 * SR)
    x = x.copy()
    x[:k] *= np.linspace(0, 1, k)
    x[-k:] *= np.linspace(1, 0, k)
    raw = path + ".raw"
    (np.clip(x, -1, 1) * 32767).astype("<i2").tofile(raw)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(SR), "-ac", "1", "-i", raw,
                    "-c:a", "libvorbis", "-q:a", "4", path], check=True)
    os.remove(raw)


if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for pet, fns in SOUNDS.items():
        for kind, fn in zip(("act", "cast", "end"), fns):
            p = os.path.join(out, f"relic_comp_{pet}_{kind}.ogg")
            save(fn(), p)
            print(p)
