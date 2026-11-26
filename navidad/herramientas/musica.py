"""Música y sonidos de Navidad de Axelis, sintetizados con numpy (sin muestras externas). Los villancicos son de dominio público
(melodías tradicionales del siglo XIX o antes), arreglados aquí como caja de música con campanas:
 - villancico_campanas: Jingle Bells (J. L. Pierpont, 1857)
 - villancico_navidad: We Wish You a Merry Christmas (tradicional inglés)
 - villancico_noche_paz: Noche de paz (F. X. Gruber, 1818)
 - villancico_adornen: Deck the Halls (tradicional galés)
 - cancion_pinata: una tonada original de posada para romper la piñata
 - krampus_musica: la pelea contra el Krampus (las campanas de Pierpont en menor, con timbales y cadenas)
 - cascabeles, risa_santa, krampus_ruge, calendario_abre, regalo_abre: efectos
Los discos son mono (así el tocadiscos se oye desde donde está). Uso: python3 musica.py <carpeta sounds>  (imprime la duración)"""
import os, sys, json
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sinte as T  # noqa: E402

SR = T.SR
note = T.note
rng = np.random.default_rng(1225)


# ---------------------------------------------------------------- instrumentos

def caja(f, dur=1.4, vol=0.12):
    """Caja de música: lengüeta de metal (parciales casi armónicos que se apagan rápido, ataque brillante)."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    out = np.sin(2 * np.pi * f * t) * np.exp(-t / 0.55)
    out += 0.45 * np.sin(2 * np.pi * f * 2.01 * t) * np.exp(-t / 0.28)
    out += 0.22 * np.sin(2 * np.pi * f * 4.03 * t) * np.exp(-t / 0.09)
    out += 0.10 * np.sin(2 * np.pi * f * 6.9 * t) * np.exp(-t / 0.04)
    out *= np.minimum(1, t / 0.0015)
    return out * vol


def celesta(f, dur=1.2, vol=0.1):
    n = int(dur * SR)
    t = np.arange(n) / SR
    out = np.sin(2 * np.pi * f * t) * np.exp(-t / 0.6) + 0.3 * np.sin(2 * np.pi * f * 3 * t) * np.exp(-t / 0.12)
    out *= np.minimum(1, t / 0.002)
    return out * vol


def cascabel(dur=0.25, vol=0.05):
    """Un cascabel: parciales agudos inarmónicos con mucho ruido metálico."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    base = rng.uniform(2600, 3400)
    for r, a in ((1, 1), (1.47, 0.7), (2.09, 0.5), (2.63, 0.4), (3.3, 0.25)):
        out += a * np.sin(2 * np.pi * base * r * t + rng.random() * 6)
    out *= np.exp(-t / 0.06)
    ruido = rng.standard_normal(n) * np.exp(-t / 0.02)
    ruido = ruido - T.lowpass(ruido, 5000)
    return (out * 0.6 + ruido * 0.5) * vol * np.minimum(1, t / 0.001)


def sonaja(dur, vol=0.05):
    """Varios cascabeles juntos (una sacudida)."""
    n = int(dur * SR)
    out = np.zeros(n)
    for _ in range(9):
        c = cascabel(0.25, vol)
        i = int(rng.uniform(0, 0.035) * SR)
        j = min(n, i + len(c))
        out[i:j] += c[:j - i]
    return out


def campana(f, dur=2.5, vol=0.1):
    return T.bell(f, dur, vol)


def bajo_suave(f, dur, vol=0.12):
    n = int((dur + 0.1) * SR)
    t = np.arange(n) / SR
    out = np.sin(2 * np.pi * f * t) + 0.3 * np.sin(4 * np.pi * f * t)
    return out * T.env(n, 0.01, 0.2, 0.55, 0.12, dur) * vol


def tambor(dur=0.18, vol=0.15, f0=180):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = f0 * (1 + 0.6 * np.exp(-t / 0.02))
    out = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.07)
    out += T.lowpass(rng.standard_normal(n), 3000) * np.exp(-t / 0.03) * 0.3
    return out * vol


def trompeta(f, dur, vol=0.08):
    n = int((dur + 0.08) * SR)
    t = np.arange(n) / SR
    vib = 1 + 0.003 * np.sin(2 * np.pi * 5.5 * t) * np.minimum(1, t / 0.25)
    ph = 2 * np.pi * f * np.cumsum(vib) / SR
    out = np.zeros(n)
    for k in range(1, 9):
        out += np.sin(k * ph) / k ** 0.8
    out = T.lowpass(out, 3200)
    return out * T.env(n, 0.025, 0.08, 0.75, 0.06, dur) * vol


def guitarra(f, dur=0.4, vol=0.07):
    """Guitarra rasgueada: cuerda pulsada (Karplus-Strong sencillo)."""
    n = int((dur + 0.3) * SR)
    p = max(2, int(SR / f))
    buf = rng.uniform(-1, 1, p)
    out = np.zeros(n)
    for i in range(n):
        out[i] = buf[i % p]
        buf[i % p] = 0.5 * (buf[i % p] + buf[(i + 1) % p]) * 0.996
    return out * vol


# ---------------------------------------------------------------- piezas

def tocar(tr, melodia, t0, beat, inst, oct=0, vol=0.12, pan=0.0):
    """melodia = [(midi o None, tiempos)]. Devuelve dónde termina."""
    t = t0
    for m, d in melodia:
        if m is not None:
            tr.add(inst(note(m + 12 * oct), max(0.6, d * beat * 2.2), vol), t, pan)
        t += d * beat
    return t


def acordes(tr, prog, t0, compas, beat, vol=1.0, estilo="vals"):
    """prog = [(bajo, [notas])], un acorde por compás."""
    for i, (b, ch) in enumerate(prog):
        t = t0 + i * compas
        tr.add(bajo_suave(note(b), compas * 0.9, 0.11 * vol), t)
        if estilo == "vals":  # bajo - acorde - acorde
            for k in (1, 2):
                for x in ch:
                    tr.add(celesta(note(x), 0.7, 0.03 * vol), t + k * beat, 0.3)
        else:  # 4/4: acorde en 2 y 4
            for k in (1, 3):
                for x in ch:
                    tr.add(celesta(note(x), 0.6, 0.028 * vol), t + k * beat, 0.3)
        tr.add(T.pad([note(x) for x in ch], compas * 1.02, 0.025 * vol, 1200), t, -0.2)


def mono(st):
    return st.mean(axis=0, keepdims=True)


# Jingle Bells (en Do)
E, F, G, A, B, C5, D5 = 64, 65, 67, 69, 71, 72, 74
CORO = [(E, 1), (E, 1), (E, 2), (E, 1), (E, 1), (E, 2), (E, 1), (G, 1), (60, 1.5), (62, .5), (E, 4),
        (F, 1), (F, 1), (F, 1.5), (F, .5), (F, 1), (E, 1), (E, 1), (E, .5), (E, .5), (E, 1), (62, 1), (62, 1), (E, 1), (62, 2), (G, 2),
        (E, 1), (E, 1), (E, 2), (E, 1), (E, 1), (E, 2), (E, 1), (G, 1), (60, 1.5), (62, .5), (E, 4),
        (F, 1), (F, 1), (F, 1.5), (F, .5), (F, 1), (E, 1), (E, 1), (E, .5), (E, .5), (G, 1), (G, 1), (F, 1), (62, 1), (60, 4)]
VERSO = [(55, 1), (E, 1), (62, 1), (60, 1), (55, 3), (55, .5), (55, .5), (55, 1), (E, 1), (62, 1), (60, 1), (57, 4),
         (57, 1), (F, 1), (E, 1), (62, 1), (59, 4), (G, 1), (G, 1), (F, 1), (62, 1), (E, 4),
         (55, 1), (E, 1), (62, 1), (60, 1), (55, 4), (55, 1), (E, 1), (62, 1), (60, 1), (57, 3), (57, 1),
         (57, 1), (F, 1), (E, 1), (62, 1), (G, 1), (G, 1), (G, 1), (G, 1), (A, 1), (G, 1), (F, 1), (62, 1), (60, 2), (G, 2)]
P_VERSO = [(48, [64, 67]), (48, [64, 67]), (48, [64, 67]), (53, [65, 69]), (50, [65, 69]), (55, [62, 67]), (55, [62, 65]), (48, [64, 67]),
           (48, [64, 67]), (48, [64, 67]), (48, [64, 67]), (53, [65, 69]), (50, [65, 69]), (55, [62, 67]), (55, [62, 65]), (48, [64, 67])]
P_CORO = [(48, [64, 67]), (48, [64, 67]), (48, [64, 67]), (48, [64, 67]), (53, [65, 69]), (48, [64, 67]), (50, [62, 66]), (55, [62, 67]),
          (48, [64, 67]), (48, [64, 67]), (48, [64, 67]), (48, [64, 67]), (53, [65, 69]), (48, [64, 67]), (55, [62, 65]), (48, [64, 67])]


def jingle(beat=60 / 150):
    compas = beat * 4
    tr = T.Track(compas * 16 * 4 + 2)
    t = 0
    for vuelta in range(2):
        acordes(tr, P_VERSO, t, compas, beat, 0.8, "4")
        tocar(tr, VERSO, t, beat, caja, 1 if vuelta else 0, 0.12)
        t += compas * 16
        acordes(tr, P_CORO, t, compas, beat, 1.0, "4")
        tocar(tr, CORO, t, beat, caja, 1, 0.12)
        if vuelta:
            tocar(tr, CORO, t, beat, campana, 0, 0.05, -0.3)
        # cascabeles en cada tiempo durante el coro
        for k in range(64):
            tr.add(sonaja(0.12, 0.03 if k % 2 else 0.045), t + k * beat, 0.4)
        t += compas * 16
    tr.add(campana(note(72), 3, 0.1), t, 0)
    return tr.render(0.25)


# We Wish You a Merry Christmas (en Sol, 3/4)
g, a, b, c, d, e, f2, gg = 67, 69, 71, 72, 74, 76, 78, 79
WISH = [(62, 1), (g, 1), (g, .5), (a, .5), (g, .5), (66, .5), (64, 1), (64, 1), (64, 1), (a, 1), (a, .5), (b, .5), (a, .5), (g, .5),
        (66, 1), (62, 1), (62, 1), (b, 1), (b, .5), (c, .5), (b, .5), (a, .5), (g, 1), (64, 1), (62, .5), (62, .5), (64, 1), (a, 1),
        (66, 1), (g, 2)]
TIDINGS = [(62, 1), (g, 1), (g, 1), (g, 1), (66, 2), (66, 1), (g, 1), (66, 1), (64, 1), (62, 2), (a, 1), (b, 1), (a, 1), (g, 1),
           (d, 1), (62, 1), (62, .5), (62, .5), (64, 1), (a, 1), (66, 1), (g, 2)]
P_WISH = [(43, [59, 62]), (48, [60, 64]), (45, [60, 64]), (50, [62, 66]), (47, [62, 67]), (40, [64, 67]), (48, [64, 67]),
          (50, [62, 66]), (43, [59, 62])]
P_TID = [(43, [59, 62]), (50, [62, 66]), (43, [59, 62]), (50, [62, 66]), (45, [60, 64]), (50, [62, 66]), (48, [64, 67]),
         (43, [59, 62])]


def wish(beat=60 / 160):
    compas = beat * 3
    tr = T.Track(compas * 18 * 3 + beat + 2)
    t = 0
    for vuelta in range(3):
        acordes(tr, P_WISH, t + beat, compas, beat, 0.9)
        tocar(tr, WISH * 1, t, beat, caja, 0 if vuelta == 0 else 1, 0.12)
        if vuelta == 2:
            tocar(tr, WISH, t, beat, campana, 0, 0.045, -0.3)
        t += beat + compas * 9 - beat
        acordes(tr, P_TID, t + beat, compas, beat, 0.9)
        tocar(tr, TIDINGS, t, beat, caja, 1, 0.12)
        t += compas * 8 + beat
    tr.add(campana(note(79), 3, 0.1), t, 0)
    return tr.render(0.25)


# Noche de paz (en Do, 6/8 contado en corcheas)
NOCHE = [(67, 3), (69, 1), (67, 2), (64, 6), (67, 3), (69, 1), (67, 2), (64, 6), (74, 4), (74, 2), (71, 6), (72, 4), (72, 2), (67, 6),
         (69, 4), (69, 2), (72, 3), (71, 1), (69, 2), (67, 3), (69, 1), (67, 2), (64, 6), (69, 4), (69, 2), (72, 3), (71, 1), (69, 2),
         (67, 3), (69, 1), (67, 2), (64, 6), (74, 4), (74, 2), (77, 3), (74, 1), (71, 2), (72, 6), (76, 6), (72, 2), (67, 2), (64, 2),
         (67, 3), (65, 1), (62, 2), (60, 12)]
P_NOCHE = [(48, [64, 67]), (48, [64, 67]), (48, [64, 67]), (48, [64, 67]), (43, [62, 65]), (43, [62, 67]), (48, [64, 67]), (48, [64, 67]),
           (53, [65, 69]), (53, [65, 69]), (48, [64, 67]), (48, [64, 67]), (53, [65, 69]), (53, [65, 69]), (48, [64, 67]), (48, [64, 67]),
           (43, [62, 67]), (43, [62, 65]), (48, [64, 67]), (48, [64, 67]), (48, [64, 67]), (43, [62, 65]), (48, [64, 67]), (48, [64, 67])]


def noche(beat=60 / 160):
    compas = beat * 6
    tr = T.Track(compas * 24 * 2 + 3)
    t = 0
    for vuelta in range(2):
        for i, (bj, ch) in enumerate(P_NOCHE):
            tt = t + i * compas
            tr.add(bajo_suave(note(bj), compas * 0.95, 0.1), tt)
            # arpegio suave de celesta en corcheas
            arp = [ch[0], ch[1], ch[0] + 12, ch[1], ch[0] + 12, ch[1] + 12]
            for k, x in enumerate(arp):
                tr.add(celesta(note(x), 0.9, 0.022), tt + k * beat, 0.35)
            tr.add(T.pad([note(x) for x in ch], compas * 1.02, 0.03, 1000), tt, -0.25)
        tocar(tr, NOCHE, t, beat, caja, 0 if vuelta == 0 else 1, 0.13)
        if vuelta:
            tocar(tr, NOCHE, t, beat, lambda f, d, v: T.choir([f], d, v), -1, 0.05, -0.2)
        t += compas * 24
    tr.add(campana(note(84), 4, 0.07), t, 0)
    return tr.render(0.35)


# Deck the Halls (en Fa)
Fm = dict(F=65, G=67, A=69, Bb=70, B=71, C=72, D=74, E=76, FF=77)
L1 = [("C", 1.5), ("Bb", .5), ("A", 1), ("G", 1), ("F", 1), ("G", 1), ("A", 1), ("F", 1)]
FALALA = [("G", .5), ("A", .5), ("Bb", .5), ("G", .5), ("A", 1.5), ("G", .5), ("F", 1), ("E0", 1), ("F", 2)]
L3 = [("G", 1.5), ("A", .5), ("Bb", 1), ("G", 1), ("A", 1.5), ("Bb", .5), ("C", 1), ("G", 1),
      ("A", .5), ("B", .5), ("C", 1), ("D", .5), ("E", .5), ("FF", 1), ("E", 1), ("D", 1), ("C", 2)]
L4 = [("D", .5), ("D", .5), ("D", .5), ("D", .5), ("C", 1.5), ("Bb", .5), ("A", 1), ("G", 1), ("F", 2)]


def _fa(seq):
    out = []
    for n, d in seq:
        out.append((64 if n == "E0" else Fm[n], d))
    return out


DECK = _fa(L1 + FALALA + L1 + FALALA + L3 + L1 + L4)
P_DECK = [(41, [65, 69]), (41, [65, 69]), (36, [64, 67]), (41, [65, 69]), (41, [65, 69]), (41, [65, 69]), (36, [64, 67]), (41, [65, 69]),
          (48, [64, 67]), (48, [64, 67]), (43, [62, 67]), (48, [64, 67]), (41, [65, 69]), (41, [65, 70]), (46, [62, 65]), (41, [65, 69])]


def deck(beat=60 / 140):
    compas = beat * 4
    tr = T.Track(compas * 16 * 3 + 2)
    t = 0
    for vuelta in range(3):
        acordes(tr, P_DECK, t, compas, beat, 0.9, "4")
        tocar(tr, DECK, t, beat, caja if vuelta != 1 else celesta, 1 if vuelta else 0, 0.12)
        if vuelta == 2:
            tocar(tr, DECK, t, beat, campana, 0, 0.045, -0.3)
            for k in range(32):
                tr.add(sonaja(0.12, 0.03), t + k * 2 * beat + beat, 0.4)
        t += compas * 16
    tr.add(campana(note(77), 3, 0.1), t, 0)
    return tr.render(0.25)


# Tonada original de posada (Re mayor, 2/4 rápido): trompetas, guitarras y tambor
POSADA = [(69, .5), (69, .5), (69, 1), (69, .5), (71, .5), (69, .5), (67, .5), (66, 1), (64, 1), (62, 2),
          (64, .5), (66, .5), (67, 1), (67, .5), (69, .5), (71, .5), (69, .5), (67, 1), (66, 1), (64, 2),
          (69, .5), (69, .5), (69, 1), (69, .5), (71, .5), (73, .5), (74, .5), (76, 1), (73, 1), (69, 2),
          (71, .5), (69, .5), (67, .5), (66, .5), (64, 1), (73, 1), (74, 2), (None, 2)]
P_POSADA = [(50, [62, 66, 69]), (50, [62, 66, 69]), (45, [61, 64, 69]), (50, [62, 66, 69]), (43, [62, 67, 71]), (45, [61, 64, 67]),
            (45, [61, 64, 67]), (50, [62, 66, 69]), (50, [62, 66, 69]), (50, [62, 66, 69]), (45, [61, 64, 69]), (45, [61, 64, 69]),
            (43, [62, 67, 71]), (45, [61, 64, 69]), (50, [62, 66, 69]), (50, [62, 66, 69])]


def posada(beat=60 / 168):
    compas = beat * 2
    vueltas = 4
    tr = T.Track(compas * 16 * vueltas)
    t = 0
    for v in range(vueltas):
        for i, (bj, ch) in enumerate(P_POSADA):
            tt = t + i * compas
            tr.add(bajo_suave(note(bj), beat * 0.8, 0.13), tt)
            tr.add(bajo_suave(note(bj + 7), beat * 0.8, 0.1), tt + beat)
            for k in (0.5, 1.5):
                for x in ch:
                    tr.add(guitarra(note(x), 0.25, 0.03), tt + k * beat + rng.uniform(0, 0.012), 0.3)
            tr.add(tambor(0.15, 0.1), tt, 0)
            tr.add(tambor(0.12, 0.06, 260), tt + beat, 0)
            tr.add(sonaja(0.1, 0.02), tt + beat * 0.5, -0.4)
        tocar(tr, POSADA, t, beat, lambda f, d, vv: trompeta(f, min(d / 2.2, beat * 0.95), vv), 0, 0.07, -0.15)
        if v % 2:
            tocar(tr, POSADA, t, beat, lambda f, d, vv: trompeta(f, min(d / 2.2, beat * 0.95), vv), -1, 0.05, 0.15)
        t += compas * 16
    return tr.render(0.15)


def krampus(beat=60 / 132):
    """Pelea: la melodía del coro de Jingle Bells en la menor, lenta y pesada, con timbales, cadenas y coro."""
    compas = beat * 4
    tr = T.Track(compas * 16)
    menor = {64: 64, 65: 65, 67: 67, 60: 60, 62: 62, 69: 69}
    # en la menor: la melodía de mi se queda, do->do, sol->sol, fa->fa, pero el bajo va en La menor/Fa/Mi
    prog = [33, 33, 29, 29, 26, 26, 28, 28, 33, 33, 29, 29, 26, 28, 33, 33]
    for i, r in enumerate(prog):
        t = i * compas
        tr.add(T.bass(note(r), compas * 0.95, 0.2), t)
        tr.add(T.timpani(1.0, note(r + 12), 0.35), t)
        tr.add(T.timpani(0.6, note(r + 12), 0.2), t + 2 * beat)
        tr.add(T.choir([note(r + 24), note(r + 27), note(r + 31)], compas * 1.02, 0.07), t, -0.2)
        for k in range(8):
            tr.add(T.hit(0.08, 0.05 if k % 2 else 0.08), t + k * beat / 2, 0.3)
        # cadenas: cascabeles graves y ásperos en el contratiempo
        tr.add(sonaja(0.2, 0.025), t + beat * 1.5, -0.5)
        tr.add(sonaja(0.2, 0.025), t + beat * 3.5, 0.5)
    mel = [(menor.get(m, m) - 12 if m else None, d) for m, d in CORO]
    tocar(tr, mel, 0, beat, lambda f, d, v: trompeta(f, d / 2.6, v), 0, 0.09)
    tocar(tr, mel, 0, beat, lambda f, d, v: T.harpsi(f, 0.25, v), 1, 0.05, 0.4)
    return tr.render(0.3)


# ---------------------------------------------------------------- efectos

def fx_cascabeles():
    n = int(2.2 * SR)
    out = np.zeros(n)
    for k in range(14):
        at = k * 0.14 + rng.uniform(0, 0.02)
        s = sonaja(0.2, 0.08 * (1 - k / 18))
        i = int(at * SR)
        j = min(n, i + len(s))
        out[i:j] += s[:j - i]
    return out[None, :] / np.max(np.abs(out)) * 0.8


def fx_risa():
    """'¡Jo, jo, jo!' grave: voz con formantes de la 'o' (F1 ~ 450, F2 ~ 800) y un soplido al empezar cada jo."""
    from scipy.signal import lfilter
    piezas = []
    for k, (f0, dur) in enumerate(((118, 0.32), (108, 0.3), (96, 0.55))):
        n = int(dur * SR)
        t = np.arange(n) / SR
        f = f0 * (1 + 0.06 * np.exp(-t / 0.08)) * (1 - 0.08 * t / dur)
        ph = 2 * np.pi * np.cumsum(f) / SR
        voz = np.zeros(n)
        for h in range(1, 30):
            voz += np.sin(h * ph) / h
        for fc, bw in ((470, 90), (820, 110), (2600, 200)):
            r = np.exp(-np.pi * bw / SR)
            th = 2 * np.pi * fc / SR
            voz = voz + 0.6 * lfilter([1 - r], [1, -2 * r * np.cos(th), r * r], voz)
        sop = T.lowpass(rng.standard_normal(n), 2500) * np.exp(-t / 0.05) * 0.5
        e = np.minimum(1, t / 0.04) * np.exp(-np.maximum(0, t - dur * 0.55) / 0.08)
        piezas.append((voz / np.max(np.abs(voz)) * 0.9 + sop) * e)
        piezas.append(np.zeros(int(0.09 * SR)))
    out = np.concatenate(piezas)
    out = T.reverb(out, 0.18)
    return out[None, :] / np.max(np.abs(out)) * 0.85


def fx_ruge():
    n = int(2.0 * SR)
    t = np.arange(n) / SR
    f = 70 + 25 * np.sin(np.pi * np.clip(t / 1.6, 0, 1))
    ph = 2 * np.pi * np.cumsum(f * (1 + 0.04 * rng.standard_normal(n).cumsum() / np.sqrt(np.arange(1, n + 1)))) / SR
    voz = np.sign(np.sin(ph)) * 0.5 + np.sin(2 * ph) * 0.4
    ruido = T.lowpass(rng.standard_normal(n), 1200)
    out = T.lowpass(voz * (0.6 + 0.4 * np.abs(np.sin(2 * np.pi * 23 * t))) + ruido * 0.8, 1800)
    e = np.minimum(1, t / 0.12) * np.exp(-np.maximum(0, t - 1.3) / 0.25)
    out = T.reverb(out * e, 0.25)
    return out[None, :] / np.max(np.abs(out)) * 0.9


def fx_calendario():
    tr = np.zeros(int(1.6 * SR))
    for k, m in enumerate((84, 88, 91, 96)):
        s = celesta(note(m), 1.2, 0.2)
        i = int(k * 0.08 * SR)
        tr[i:i + len(s)] += s[:len(tr) - i]
    return tr[None, :] / np.max(np.abs(tr)) * 0.8


def fx_regalo():
    n = int(1.2 * SR)
    t = np.arange(n) / SR
    papel = T.lowpass(rng.standard_normal(n), 6000) * (0.3 + 0.7 * (rng.random(n) < 0.02)) * np.exp(-t / 0.25)
    out = papel * 0.5
    for k, m in enumerate((79, 84, 88)):
        s = caja(note(m), 0.9, 0.3)
        i = int((0.25 + k * 0.07) * SR)
        out[i:i + len(s)] += s[:n - i]
    return out[None, :] / np.max(np.abs(out)) * 0.8


def guardar(data, path):
    T.save(data, path) if data.shape[0] == 2 else _mono(data[0], path)


def _mono(x, path):
    import subprocess
    k = int(0.005 * SR)
    x = x.copy()
    x[:k] *= np.linspace(0, 1, k)
    x[-k:] *= np.linspace(1, 0, k)
    raw = path + ".raw"
    (np.clip(x, -1, 1) * 32767).astype("<i2").tofile(raw)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(SR), "-ac", "1", "-i", raw,
                    "-c:a", "libvorbis", "-q:a", "3", path], check=True)
    os.remove(raw)


if __name__ == "__main__":
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    solo = sys.argv[2:] or None
    piezas = {"villancico_campanas": lambda: mono(jingle()), "villancico_navidad": lambda: mono(wish()),
              "villancico_noche_paz": lambda: mono(noche()), "villancico_adornen": lambda: mono(deck()),
              "cancion_pinata": lambda: mono(posada()), "krampus_musica": krampus,
              "cascabeles": fx_cascabeles, "risa_santa": fx_risa, "krampus_ruge": fx_ruge,
              "calendario_abre": fx_calendario, "regalo_abre": fx_regalo}
    for nombre, fn in piezas.items():
        if solo and nombre not in solo:
            continue
        d = fn()
        guardar(d, os.path.join(out, nombre + ".ogg"))
        print(nombre, round(d.shape[1] / SR, 2), "s")
