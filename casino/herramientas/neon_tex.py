#!/usr/bin/env python3
"""Texturas de neón: letras (A-Z, 0-9, símbolos) en 6 colores, letreros animados de dibujos y pantallas de pared animadas.

    python3 casino/herramientas/neon_tex.py
"""
import json
import math
import os
import random

from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "casino", "textures", "block", "decor")
N = 32

# letra 5x7 (1 = tubo)
FONT = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"], "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01111", "10000", "10000", "10000", "10000", "10000", "01111"], "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"], "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01111", "10000", "10000", "10011", "10001", "10001", "01111"], "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"], "J": ["00111", "00010", "00010", "00010", "00010", "10010", "01100"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"], "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"], "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"], "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "Q": ["01110", "10001", "10001", "10001", "10101", "10010", "01101"], "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"], "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"], "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
    "W": ["10001", "10001", "10001", "10101", "10101", "10101", "01010"], "X": ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
    "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"], "Z": ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
    "0": ["01110", "10001", "10011", "10101", "11001", "10001", "01110"], "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00001", "00010", "00100", "01000", "11111"], "3": ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
    "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"], "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
    "6": ["00110", "01000", "10000", "11110", "10001", "10001", "01110"], "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
    "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"], "9": ["01110", "10001", "10001", "01111", "00001", "00010", "01100"],
    "!": ["00100", "00100", "00100", "00100", "00100", "00000", "00100"], "?": ["01110", "10001", "00001", "00010", "00100", "00000", "00100"],
    "$": ["00100", "01111", "10100", "01110", "00101", "11110", "00100"], "&": ["01100", "10010", "10100", "01000", "10101", "10010", "01101"],
    "♥": ["00000", "01010", "11111", "11111", "01110", "00100", "00000"], "♠": ["00100", "01110", "11111", "11111", "00100", "01110", "00000"],
    "♦": ["00100", "01110", "11111", "11111", "01110", "00100", "00000"], "♣": ["01110", "01110", "11111", "11111", "00100", "01110", "00000"],
    "★": ["00100", "00100", "11111", "01110", "01110", "11011", "00000"], "-": ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
}
CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?$&♥♠♦♣★- "
CHAR_NAMES = {"!": "excl", "?": "quest", "$": "dollar", "&": "and", "♥": "heart", "♠": "spade", "♦": "diamond", "♣": "club", "★": "star", "-": "dash", " ": "space"}
NEON = {"red": (255, 40, 70), "pink": (255, 70, 200), "gold": (255, 196, 40), "green": (60, 255, 120), "cyan": (40, 220, 255), "white": (240, 240, 255)}
BACK = (16, 12, 20)
SOLID = "♥♠♦♣★"


def clamp(v):
    return max(0, min(255, int(v)))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def glow(points, col, size=N, back=BACK, lit=1.0, frame_bg=True):
    """Tubos de neón (lista de píxeles de tubo) con halo suave sobre un panel oscuro."""
    im = Image.new("RGBA", (size, size), (*back, 255))
    px = im.load()
    pts = set(points)
    for y in range(size):
        for x in range(size):
            if (x, y) in pts:
                continue
            d = min((abs(x - a) + abs(y - b) for a, b in points), default=99)
            if d <= 3:
                px[x, y] = (*mix(back, col, (0.42, 0.24, 0.1)[d - 1] * lit), 255)
    core = mix(col, (255, 255, 255), 0.55)
    for (x, y) in pts:
        px[x, y] = (*mix(back, core if lit > 0.5 else col, lit), 255)
    if frame_bg:
        # marco de metal oscuro
        for i in range(size):
            for (x, y) in ((i, 0), (i, size - 1), (0, i), (size - 1, i)):
                px[x, y] = (46, 40, 52, 255)
    return im


def glyph_points(ch):
    if ch == " ":
        return []
    rows = FONT[ch]
    pts = []
    # 5x7 -> celdas de 4 px, tubo de 2 px, centrado en 32x32
    ox, oy = 6, 3
    for r, row in enumerate(rows):
        for c, v in enumerate(row):
            if v == "1":
                for dy in range(4):
                    for dx in range(4):
                        x, y = ox + c * 4 + dx, oy + r * 4 + dy
                        if ch in SOLID or (1 <= dx <= 2 and 1 <= dy <= 2):
                            pts.append((x, y))
                # unir con vecinos para que parezca un tubo continuo
                for (dc, dr) in ((1, 0), (0, 1)):
                    cc, rr = c + dc, r + dr
                    if rr < 7 and cc < 5 and rows[rr][cc] == "1":
                        for k in range(1, 5):
                            pts.append((ox + c * 4 + 1 + dc * k, oy + r * 4 + 1 + dr * k))
                            pts.append((ox + c * 4 + 2 + dc * k, oy + r * 4 + 2 + dr * k))
                            pts.append((ox + c * 4 + 1 + dc * k + (1 if dr else 0), oy + r * 4 + 1 + dr * k + (1 if dc else 0)))
    return [(x, y) for (x, y) in pts if 0 <= x < N and 0 <= y < N]


def save(im, name, frames=None, frametime=None):
    p = os.path.join(OUT, name + ".png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    im.save(p)
    if frames:
        meta = {"animation": {"frametime": frametime or 4}}
        if isinstance(frames, list):
            meta["animation"]["frames"] = frames
        with open(p + ".mcmeta", "w") as fh:
            json.dump(meta, fh)


def strip(frames):
    im = Image.new("RGBA", (N, N * len(frames)))
    for i, f in enumerate(frames):
        im.paste(f, (0, i * N))
    return im


# ------------------------------------------------------------------------------------------------ dibujos de letreros
def circle(cx, cy, r, w=2):
    pts = []
    for y in range(N):
        for x in range(N):
            d = math.hypot(x - cx, y - cy)
            if r - w / 2 <= d <= r + w / 2:
                pts.append((x, y))
    return pts


def line(x1, y1, x2, y2, w=2):
    pts = []
    n = int(max(abs(x2 - x1), abs(y2 - y1)) * 2) + 1
    for i in range(n + 1):
        t = i / n
        x, y = x1 + (x2 - x1) * t, y1 + (y2 - y1) * t
        for dx in range(w):
            for dy in range(w):
                pts.append((int(x) + dx, int(y) + dy))
    return pts


def small_text(text, x0, y0, scale=1):
    pts = []
    x = x0
    for ch in text:
        rows = FONT[ch]
        for r, row in enumerate(rows):
            for c, v in enumerate(row):
                if v == "1":
                    for dy in range(scale):
                        for dx in range(scale):
                            pts.append((x + c * scale + dx, y0 + r * scale + dy))
        x += 6 * scale
    return pts


def sign_frames(name):
    """Cada letrero: lista de capas (puntos, color) y cómo se animan."""
    if name == "cherry":
        a = circle(10, 22, 5) + circle(21, 21, 5)
        b = line(10, 17, 16, 6) + line(21, 16, 16, 6) + line(16, 6, 23, 4)
        return [[(a, NEON["red"]), (b, NEON["green"])], [(a, NEON["red"]), (b, NEON["green"])], [(a, NEON["pink"]), (b, NEON["green"])]]
    if name == "sevens":
        t = small_text("777", 3, 9, 2) if False else small_text("777", 7, 12, 1)
        big = []
        for i, x0 in enumerate((2, 12, 22)):
            big.append(line(x0, 8, x0 + 8, 8) + line(x0 + 8, 8, x0 + 3, 24))
        cols = [NEON["red"], NEON["gold"], NEON["red"]]
        return [[(big[j], cols[(j + k) % 3]) for j in range(3)] for k in range(3)]
    if name == "cocktail":
        g = line(6, 8, 26, 8) + line(6, 8, 16, 18) + line(26, 8, 16, 18) + line(16, 18, 16, 26) + line(10, 26, 22, 26)
        o = circle(20, 6, 2, 2)
        return [[(g, NEON["cyan"]), (o, NEON["green"])], [(g, NEON["cyan"]), (o, NEON["green"])], [(g, NEON["pink"]), (o, NEON["green"])]]
    if name == "dice":
        sq = line(5, 5, 26, 5) + line(26, 5, 26, 26) + line(26, 26, 5, 26) + line(5, 26, 5, 5)
        dots = [[(15, 15)], [(10, 10), (20, 20)], [(9, 9), (15, 15), (21, 21)], [(10, 10), (20, 10), (10, 20), (20, 20)],
                [(9, 9), (21, 9), (15, 15), (9, 21), (21, 21)], [(10, 8), (20, 8), (10, 15), (20, 15), (10, 22), (20, 22)]]
        fr = []
        for d in dots:
            pts = []
            for (x, y) in d:
                pts += [(x, y), (x + 1, y), (x, y + 1), (x + 1, y + 1)]
            fr.append([(sq, NEON["white"]), (pts, NEON["red"])])
        return fr
    if name == "crown":
        c = line(5, 24, 27, 24) + line(5, 24, 5, 10) + line(27, 24, 27, 10) + line(5, 10, 11, 17) + line(11, 17, 16, 7) + \
            line(16, 7, 21, 17) + line(21, 17, 27, 10)
        gem = circle(16, 20, 1.5, 2)
        return [[(c, NEON["gold"]), (gem, NEON["red"])], [(c, NEON["gold"]), (gem, NEON["cyan"])], [(c, NEON["gold"]), (gem, NEON["green"])]]
    if name == "star":
        pts = []
        for i in range(5):
            a1 = -math.pi / 2 + i * 4 * math.pi / 5
            a2 = -math.pi / 2 + (i + 1) * 4 * math.pi / 5
            pts += line(16 + 11 * math.cos(a1), 17 + 11 * math.sin(a1), 16 + 11 * math.cos(a2), 17 + 11 * math.sin(a2))
        return [[(pts, NEON["gold"])], [(pts, NEON["white"])], [(pts, NEON["gold"])], [(pts, NEON["gold"])]]
    if name == "dollar":
        return [[(small_text("$", 6, 3, 4) if False else glyph_points("$"), NEON["green"])], [(glyph_points("$"), NEON["green"])],
                [(glyph_points("$"), NEON["gold"])]]
    if name == "suits":
        s = small_text("♠♥", 4, 4, 2) + []
        h = small_text("♦♣", 4, 18, 2)
        return [[(small_text("♠", 4, 3, 2), NEON["white"]), (small_text("♥", 18, 3, 2), NEON["red"]),
                 (small_text("♦", 4, 17, 2), NEON["red"]), (small_text("♣", 18, 17, 2), NEON["white"])],
                [(small_text("♠", 4, 3, 2), NEON["red"]), (small_text("♥", 18, 3, 2), NEON["white"]),
                 (small_text("♦", 4, 17, 2), NEON["white"]), (small_text("♣", 18, 17, 2), NEON["red"])]]
    if name == "bar":
        t = small_text("BAR", 7, 12)
        return [[(t + line(3, 6, 28, 6, 1) + line(3, 25, 28, 25, 1), NEON["pink"])], [(t, NEON["pink"])], [(t + line(3, 6, 28, 6, 1) + line(3, 25, 28, 25, 1), NEON["pink"])]]
    if name == "vip":
        t = small_text("VIP", 7, 12)
        st = circle(16, 15, 13, 1)
        return [[(t, NEON["gold"]), (st, NEON["pink"])], [(t, NEON["gold"]), (st, NEON["cyan"])]]
    if name == "arrow":
        base = line(4, 16, 22, 16) + line(22, 16, 15, 9) + line(22, 16, 15, 23)
        fr = []
        for k in range(4):
            dots = []
            for i in range(4):
                if i <= k:
                    x = 24 + i * 1
                    dots += [(x, 15), (x, 16)]
            fr.append([(base, NEON["gold"]), (dots, NEON["white"])])
        return fr
    if name == "chip":
        c = circle(16, 16, 11) + circle(16, 16, 6)
        sp = []
        for i in range(8):
            a = i * math.pi / 4
            sp += line(16 + 7 * math.cos(a), 16 + 7 * math.sin(a), 16 + 11 * math.cos(a), 16 + 11 * math.sin(a), 2)
        return [[(c, NEON["red"]), (sp, NEON["white"])], [(c, NEON["red"]), (sp, NEON["gold"])]]
    raise KeyError(name)


SIGNS = ["cherry", "sevens", "cocktail", "dice", "crown", "star", "dollar", "suits", "bar", "vip", "arrow", "chip"]


def render_layers(layers, lit=1.0):
    pts_all = []
    im = None
    for pts, col in layers:
        pts = [(x, y) for (x, y) in pts if 0 <= x < N and 0 <= y < N]
        layer = glow(pts, col, lit=lit, frame_bg=False)
        if im is None:
            im = layer
        else:
            # suma de luces sobre el mismo fondo
            a, b = im.load(), layer.load()
            for y in range(N):
                for x in range(N):
                    pa, pb = a[x, y], b[x, y]
                    a[x, y] = tuple(max(pa[i], pb[i]) for i in range(3)) + (255,)
        pts_all += pts
    px = im.load()
    for i in range(N):
        for (x, y) in ((i, 0), (i, N - 1), (0, i), (N - 1, i)):
            px[x, y] = (46, 40, 52, 255)
    return im


# ------------------------------------------------------------------------------------------------ pantallas
def screen_frames(kind):
    rnd = random.Random(kind)
    frames = []
    if kind == "jackpot":
        for k in range(8):
            im = Image.new("RGBA", (N, N), (6, 4, 20, 255))
            px = im.load()
            for (x, y) in small_text("JACK", 4, 4):
                px[x, y] = (255, 210, 60, 255) if (k % 2 == 0) else (255, 120, 40, 255)
            for (x, y) in small_text("POT", 7, 12):
                px[x, y] = (255, 210, 60, 255) if (k % 2 == 0) else (255, 120, 40, 255)
            num = str(10000 + k * 1373)[:5]
            for (x, y) in small_text(num[:5], 1, 22):
                if 0 <= x < N and 0 <= y < N:
                    px[x, y] = (80, 255, 140, 255)
            for i in range(N):
                if (i + k) % 4 == 0:
                    px[i, 0] = px[i, N - 1] = (255, 60, 90, 255)
                    px[0, i] = px[N - 1, i] = (255, 60, 90, 255)
            frames.append(im)
    elif kind == "reels":
        sym = ["7", "$", "♥", "★", "♦"]
        cols = [(255, 60, 60), (80, 255, 120), (255, 80, 160), (255, 210, 60), (90, 200, 255)]
        for k in range(10):
            im = Image.new("RGBA", (N, N), (10, 8, 16, 255))
            px = im.load()
            for r in range(3):
                x0 = 1 + r * 10
                for y in range(2, 30):
                    for x in range(x0, x0 + 9):
                        px[x, y] = (232, 228, 220, 255)
                off = (k * (3 + r)) % 5
                for row in range(2):
                    s = sym[(off + row + r) % 5]
                    for (x, y) in small_text(s, x0 + 2, 4 + row * 13):
                        if 0 <= x < N and 0 <= y < N:
                            px[x, y] = (*cols[(off + row + r) % 5], 255)
            for x in range(N):
                px[x, 15] = (255, 40, 60, 255)
            frames.append(im)
    elif kind == "roulette":
        for k in range(12):
            im = Image.new("RGBA", (N, N), (8, 30, 16, 255))
            px = im.load()
            n = rnd.randint(0, 36)
            red = n in (1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)
            c = (60, 200, 90) if n == 0 else (230, 40, 50) if red else (30, 30, 34)
            for y in range(6, 26):
                for x in range(6, 26):
                    if math.hypot(x - 15.5, y - 15.5) < 10:
                        px[x, y] = (*c, 255)
            s = str(n)
            for (x, y) in small_text(s, 16 - 3 * len(s), 12):
                px[x, y] = (255, 255, 255, 255)
            for (x, y) in small_text("WIN", 7, 1):
                if y < N:
                    px[x, y] = (255, 210, 60, 255) if k % 2 else (255, 255, 255, 255)
            frames.append(im)
    elif kind == "cards":
        faces = ["A♠", "K♥", "Q♦", "J♣", "10"]
        for k in range(5):
            im = Image.new("RGBA", (N, N), (12, 60, 30, 255))
            px = im.load()
            for i, x0 in enumerate((3, 17)):
                for y in range(5, 28):
                    for x in range(x0, x0 + 12):
                        px[x, y] = (246, 242, 232, 255)
                f = faces[(k + i) % 5]
                col = (210, 30, 40) if ("♥" in f or "♦" in f) else (20, 20, 24)
                for (x, y) in small_text(f[0], x0 + 1, 7):
                    px[x, y] = (*col, 255)
                if len(f) > 1:
                    for (x, y) in small_text(f[1], x0 + 5, 18):
                        if x < N:
                            px[x, y] = (*col, 255)
            frames.append(im)
    return frames


SCREENS = ["jackpot", "reels", "roulette", "cards"]


def main():
    for cname, col in NEON.items():
        for ch in CHARS:
            nm = CHAR_NAMES.get(ch, ch.lower())
            save(glow(glyph_points(ch), col), f"neon/{cname}_{nm}")
    for s in SIGNS:
        fr = sign_frames(s)
        imgs = [render_layers(layers) for layers in fr]
        # parpadeo ocasional (un cuadro apagado a medias)
        imgs.append(render_layers(fr[0], lit=0.55))
        order = list(range(len(fr))) * 3 + [len(fr), 0, len(fr), 0]
        save(strip(imgs), f"sign_{s}", frames=[{"index": i, "time": 6} for i in order])
    for s in SCREENS:
        fr = screen_frames(s)
        save(strip(fr), f"screen_{s}", frames=True, frametime=8 if s != "reels" else 2)
    # cristal del candelabro (destellos que se mueven) y foco cálido
    rnd = random.Random("crystal")
    fr = []
    spark = [(rnd.randrange(N), rnd.randrange(N)) for _ in range(10)]
    for k in range(8):
        im = Image.new("RGBA", (N, N), (0, 0, 0, 255))
        px = im.load()
        for y in range(N):
            for x in range(N):
                f = 0.5 * math.sin((x + y) / 3.0 + k * 0.8) + 0.5
                px[x, y] = (*mix((150, 186, 214), (236, 246, 255), f), 255)
        for i, (x, y) in enumerate(spark):
            if (i + k) % 4 == 0:
                for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                    if 0 <= x + dx < N and 0 <= y + dy < N:
                        px[x + dx, y + dy] = (255, 255, 255, 255)
        fr.append(im)
    save(strip(fr), "crystal", frames=True, frametime=3)
    im = Image.new("RGBA", (N, N), (0, 0, 0, 255))
    px = im.load()
    for y in range(N):
        for x in range(N):
            d = math.hypot(x - 15.5, y - 12) / 20
            px[x, y] = (*mix((255, 250, 220), (255, 176, 60), min(1, d)), 255)
    save(im, "bulb")
    print("neón listo")


if __name__ == "__main__":
    main()
