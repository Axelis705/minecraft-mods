"""Sonidos propios del Archimago, sintetizados con numpy/scipy (sin muestras externas). Uso:
    python3 gen_sonidos_archimago.py <carpeta sounds>
Cada sonido sale en .ogg mono (los efectos con posición en Minecraft tienen que ser mono).
 - archimago_ambient: murmullo de conjuro con voz grave (2 variantes)
 - archimago_hurt: quejido corto con chasquido mágico (2 variantes)
 - archimago_death: coro que cae, cristales que se rompen y un eco largo
 - archimago_charge: zumbido que sube (prepara un hechizo)
 - archimago_cast: destello brillante que sube y estalla en campanitas
 - archimago_blink: teletransporte (aspiración al revés y "pop")
 - archimago_orb: lanza una esfera arcana (silbido que se aleja)
 - archimago_burst: estallido arcano (golpe grave + cristal)
"""
import os, subprocess, sys
import numpy as np
from scipy.signal import butter, sosfilt, lfilter

SR = 44100
rng = np.random.default_rng(42)


def t_of(n):
    return np.arange(n) / SR


def bp(x, lo, hi, order=2):
    sos = butter(order, [max(20, lo), min(hi, SR / 2.1)], btype="bandpass", fs=SR, output="sos")
    return sosfilt(sos, x)


def lp(x, cut, order=2):
    return sosfilt(butter(order, min(cut, SR / 2.1), btype="low", fs=SR, output="sos"), x)


def hp(x, cut, order=2):
    return sosfilt(butter(order, cut, btype="high", fs=SR, output="sos"), x)


def saw_glide(f0, f1, n, harm=30):
    """Diente de sierra con la frecuencia deslizándose de f0 a f1 (fase acumulada)."""
    f = np.geomspace(f0, f1, n)
    ph = 2 * np.pi * np.cumsum(f) / SR
    out = np.zeros(n)
    for k in range(1, harm + 1):
        out += np.sin(k * ph) / k * (f * k < SR / 2.2)
    return out


def voice(f0, f1, dur, vowel="a", vib=5.0, growl=0.0):
    """Voz grave sintética: sierra + vibrato pasada por formantes de una vocal."""
    n = int(dur * SR)
    t = t_of(n)
    f = np.geomspace(f0, f1, n) * (1 + 0.012 * np.sin(2 * np.pi * vib * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    src = np.zeros(n)
    for k in range(1, 40):
        src += np.sin(k * ph) / k * (f * k < SR / 2.2)
    if growl:
        src *= 1 + growl * np.sin(2 * np.pi * 31 * t)
    forms = {"a": (750, 1200, 2600), "o": (450, 800, 2800), "u": (330, 700, 2500), "e": (500, 1800, 2600)}[vowel]
    out = np.zeros(n)
    for i, fc in enumerate(forms):
        out += bp(src, fc * 0.85, fc * 1.15) * (1.0, 0.6, 0.25)[i]
    return out


def noise(n):
    return rng.standard_normal(n)


def sweep_noise(n, f0, f1, width=0.35):
    """Ruido con un filtro de banda que se mueve suave de f0 a f1 (soplo/silbido), filtrando en el espectro (STFT)."""
    from scipy.signal import stft, istft
    x = noise(n + 2048)
    f, tt, Z = stft(x, SR, nperseg=1024, noverlap=768)
    centers = np.geomspace(f0, f1, Z.shape[1])
    for i, c in enumerate(centers):
        Z[:, i] *= np.exp(-0.5 * (np.log(np.maximum(f, 1) / c) / width) ** 2)
    _, y = istft(Z, SR, nperseg=1024, noverlap=768)
    return y[:n]


def chimes(n, base, count=7, decay=0.6, spread=2.2):
    t = t_of(n)
    out = np.zeros(n)
    for i in range(count):
        f = base * (1 + spread * rng.random())
        at = rng.random() * 0.35
        k = t >= at
        tt = t[k] - at
        out[k] += np.sin(2 * np.pi * f * tt) * np.exp(-tt / decay) * (0.6 + 0.4 * rng.random())
        out[k] += 0.3 * np.sin(2 * np.pi * f * 2.76 * tt) * np.exp(-tt / (decay * 0.5))
    return out


def env(n, a, r, curve=1.0):
    t = t_of(n)
    dur = n / SR
    e = np.minimum(1, t / max(a, 1e-4)) * np.clip((dur - t) / max(r, 1e-4), 0, 1) ** curve
    return e


def echo(x, delay=0.18, fb=0.35, taps=4):
    d = int(delay * SR)
    out = np.concatenate([x, np.zeros(d * taps)])
    for k in range(1, taps + 1):
        out[d * k:d * k + len(x)] += x * fb ** k
    return out


def reverb(x, wet=0.3, decay=0.55):
    """Sala difusa: convolución con una respuesta de ruido que se apaga (sin el timbre metálico de los filtros peine)."""
    from scipy.signal import fftconvolve
    m = int(decay * 3 * SR)
    t = t_of(m)
    ir = lp(rng.standard_normal(m), 6000) * np.exp(-t / decay)
    ir[: int(0.012 * SR)] = 0  # pequeño retraso antes de las reflexiones
    ir /= np.sqrt(np.sum(ir ** 2))
    wetsig = fftconvolve(x, ir)
    dry = np.concatenate([x, np.zeros(len(wetsig) - len(x))])
    return dry * (1 - wet) + wetsig * wet * 0.9


def norm(x, peak=0.9):
    m = np.max(np.abs(x))
    return x / m * peak if m > 0 else x


def trim(x, thr=1.2e-2):
    """Corta el silencio del final y apaga suave los últimos 120 ms (sin chasquido)."""
    idx = np.where(np.abs(x) > thr * np.max(np.abs(x)))[0]
    x = x[: idx[-1] + 1].copy() if len(idx) else x.copy()
    k = min(len(x), int(0.12 * SR))
    x[-k:] *= np.linspace(1, 0, k) ** 2
    return x


# ---------------------------------------------------------------- sonidos

def ambient(seed):
    global rng
    rng = np.random.default_rng(seed)
    dur = 1.8
    n = int(dur * SR)
    v = voice(95 + 10 * seed, 88 + 6 * seed, dur, "o" if seed % 2 else "u", vib=4.0) * env(n, 0.35, 0.6)
    whisper = bp(noise(n), 2500, 6000) * env(n, 0.2, 0.8) * 0.25
    sh = chimes(n, 1400 + 200 * seed, 4, 0.5) * 0.08
    return norm(trim(reverb(v * 0.8 + whisper + sh, 0.35)), 0.7)


def hurt(seed):
    global rng
    rng = np.random.default_rng(100 + seed)
    dur = 0.45
    n = int(dur * SR)
    v = voice(150 + 25 * seed, 95, dur, "a" if seed == 0 else "e", vib=7.0, growl=0.5) * env(n, 0.01, 0.25, 1.5)
    crack = hp(noise(n), 3000) * np.exp(-t_of(n) / 0.03) * 0.6
    zap = np.sin(2 * np.pi * np.cumsum(np.geomspace(1800, 600, n)) / SR) * np.exp(-t_of(n) / 0.08) * 0.3
    return norm(trim(reverb(v + crack + zap, 0.25)), 0.9)


def death():
    global rng
    rng = np.random.default_rng(7)
    dur = 3.4
    n = int(dur * SR)
    t = t_of(n)
    choir = sum(voice(f, f * 0.45, dur, "a", vib=5.5) * w for f, w in ((110, 1.0), (165, 0.6), (220, 0.4)))
    choir *= env(n, 0.05, 1.8, 1.3)
    shatter = np.zeros(n)
    for k in range(5):
        at = int((0.4 + 0.35 * k) * SR)
        m = n - at
        shatter[at:] += hp(noise(m), 4000) * np.exp(-t_of(m) / 0.06) * (0.5 - 0.07 * k)
        shatter[at:] += chimes(m, 2000 + 300 * k, 3, 0.4) * 0.15
    boom = np.sin(2 * np.pi * np.cumsum(np.geomspace(90, 35, n)) / SR) * np.exp(-t / 0.7) * 0.7
    return norm(trim(reverb(echo(choir * 0.8 + shatter + boom, 0.22, 0.3), 0.4)), 0.95)


def charge():
    dur = 1.3
    n = int(dur * SR)
    t = t_of(n)
    hum = saw_glide(70, 260, n, 20)
    hum = lp(hum, 1800) * (0.3 + 0.7 * t / dur) * (1 + 0.4 * np.sin(2 * np.pi * np.geomspace(6, 22, n) * t))
    air = sweep_noise(n, 400, 5000, 0.25) * (t / dur) ** 2 * 0.6
    sh = chimes(n, 1800, 6, 0.3) * (t / dur) * 0.15
    x = (hum * 0.6 + air + sh) * env(n, 0.05, 0.08)
    return norm(trim(reverb(x, 0.25)), 0.85)


def cast():
    global rng
    rng = np.random.default_rng(11)
    dur = 1.1
    n = int(dur * SR)
    t = t_of(n)
    rise = sweep_noise(n, 600, 7000, 0.2) * np.clip(t / 0.35, 0, 1) * np.exp(-np.clip(t - 0.35, 0, None) / 0.15)
    tone = np.sin(2 * np.pi * np.cumsum(np.geomspace(300, 1500, n)) / SR) * np.clip(t / 0.35, 0, 1) * np.exp(-np.clip(t - 0.35, 0, None) / 0.1) * 0.4
    bells = np.zeros(n)
    at = int(0.33 * SR)
    bells[at:] = chimes(n - at, 1300, 9, 0.5, 2.5) * 0.35
    return norm(trim(reverb(rise + tone + bells, 0.35)), 0.9)


def blink():
    dur = 0.7
    n = int(dur * SR)
    t = t_of(n)
    suck = sweep_noise(n, 6000, 300, 0.3) * np.clip(t / 0.4, 0, 1) ** 2 * (t < 0.42)
    pop_at = int(0.42 * SR)
    pop = np.zeros(n)
    m = n - pop_at
    pop[pop_at:] = np.sin(2 * np.pi * np.cumsum(np.geomspace(500, 120, m)) / SR) * np.exp(-t_of(m) / 0.05)
    pop[pop_at:] += hp(noise(m), 2500) * np.exp(-t_of(m) / 0.02) * 0.4
    return norm(trim(reverb(suck * 0.8 + pop, 0.3)), 0.9)


def orb():
    dur = 0.8
    n = int(dur * SR)
    t = t_of(n)
    whistle = np.sin(2 * np.pi * np.cumsum(np.geomspace(1400, 500, n) * (1 + 0.03 * np.sin(2 * np.pi * 18 * t))) / SR)
    whistle *= env(n, 0.02, 0.5, 1.4) * 0.5
    air = sweep_noise(n, 3000, 800, 0.3) * env(n, 0.01, 0.6) * 0.5
    sparkle = chimes(n, 2500, 5, 0.2) * 0.12
    return norm(trim(reverb(whistle + air + sparkle, 0.2)), 0.85)


def burst():
    global rng
    rng = np.random.default_rng(23)
    dur = 1.6
    n = int(dur * SR)
    t = t_of(n)
    boom = np.sin(2 * np.pi * np.cumsum(np.geomspace(140, 40, n)) / SR) * np.exp(-t / 0.35)
    body = lp(noise(n), 900) * np.exp(-t / 0.18) * 0.8
    glass = hp(noise(n), 3500) * np.exp(-t / 0.09) * 0.5 + chimes(n, 1900, 8, 0.45) * 0.25
    return norm(trim(reverb(boom + body + glass, 0.3)), 0.95)


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
    sounds = {
        "archimago_ambient1": ambient(1), "archimago_ambient2": ambient(2),
        "archimago_hurt1": hurt(0), "archimago_hurt2": hurt(1),
        "archimago_death": death(), "archimago_charge": charge(), "archimago_cast": cast(),
        "archimago_blink": blink(), "archimago_orb": orb(), "archimago_burst": burst(),
    }
    for name, data in sounds.items():
        save(data, os.path.join(out, name + ".ogg"))
        print(name, round(len(data) / SR, 2), "s")
