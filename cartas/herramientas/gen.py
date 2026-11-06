#!/usr/bin/env python3
"""Genera, a partir de cartas.json: CardData.java, texturas (icono 16x16 y arte 64x96), modelos de item y lang.
Uso (desde la carpeta cartas/):  python3 herramientas/gen.py
El arte es pixel art PROVISIONAL hecho por codigo; se puede reemplazar cualquier PNG a mano sin tocar nada mas."""
import json, math, os, random
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "herramientas", "cartas.json")
JAVA = os.path.join(ROOT, "src/main/java/com/mundosdeaxelis/cartas/CardData.java")
RES = os.path.join(ROOT, "src/main/resources/assets/mundos_de_axelis")
T_ITEM = os.path.join(RES, "textures/item")
T_GUI = os.path.join(RES, "textures/gui/cartas")
M_ITEM = os.path.join(RES, "models/item")
LANG = os.path.join(RES, "lang")
for d in (T_ITEM, T_GUI, M_ITEM, LANG, os.path.dirname(JAVA)):
    os.makedirs(d, exist_ok=True)

data = json.load(open(SRC, encoding="utf-8"))

RARITY_COLOR = {"COMUN": "#9aa0a6", "RARA": "#3b8cff", "EPICA": "#b04cff", "LEGENDARIA": "#ffb21e", "MITICA": "#ff3d6e"}


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def dark(c, f):
    return tuple(int(x * f) for x in c)


def light(c, f):
    return tuple(int(x + (255 - x) * f) for x in c)


# ------------------------------------------------------------------ sprites 12x12 (A=color1, B=color2, D=oscuro, O=contorno, W=brillo)
S = {
    "crown": ["............", ".B...BB...B.", ".BA..BB..AB.", ".BAA.BB.AAB.", ".BAAABBAAAB.", ".BAAAAAAAAB.", ".BAWAAAAWAB.", ".BAAAAAAAAB.", ".DDDDDDDDDD.", "............", "............", "............"],
    "skull": ["............", "...OOOOOO...", "..OAAAAAAO..", ".OAAWWAAAAO.", ".OAAAAAAAAO.", ".OADDAADDAO.", ".OADDAADDAO.", "..OAAADAAO..", "...OAAAAO...", "...OADADO...", "....OOOO....", "............"],
    "sword": [".....OO.....", "....OWAO....", "....OWAO....", "....OWAO....", "....OWAO....", "....OWAO....", ".OOOOWAOOOO.", ".OBBBBBBBBO.", ".OOOOBBOOOO.", ".....OBO....", ".....OBO....", ".....OOO...."],
    "paw": ["............", "............", ".OO..OO..OO.", "OAAOOAAOOAAO", "OAAOOAAOOAAO", ".OO..OO..OO.", "...OOOOOO...", "..OAAAAAAO..", ".OAAWAAAAAO.", ".OAAAOOAAAO.", "..OAO..OAO..", "...OO..OO..."],
    "tree": ["....OOOO....", "..OOAAAAOO..", ".OAAAWAAAAO.", ".OAAAAAAAAO.", "OAAWAAAAAAAO", "OAAAAAAAWAAO", ".OAAAAAAAAO.", "..OOAAAAOO..", "....OBBO....", "....OBBO....", "....OBBO....", "...OBBBBO..."],
    "hammer": ["............", ".OOOOOOOO...", ".OAWAAAAAO..", ".OAAAAAAAAO.", ".OAAAAAAAO..", ".OOOOBOOO...", ".....OBO....", ".....OBO....", ".....OBO....", ".....OBO....", ".....OBO....", ".....OOO...."],
    "wolf": ["..O......O..", "..OO....OO..", "..OAO..OAO..", "..OAAOOAAO..", ".OAAAAAAAAO.", ".OADAAAADAO.", ".OAAAAAAAAO.", "..OAAAAAAO..", "...OAADAAO..", "....OAAAO...", ".....OOO....", "............"],
    "star": [".....OO.....", ".....OO.....", "....OAAO....", "OOOOAWAAOOOO", ".OAAAAAAAAO.", "..OAAAAAAO..", "...OAAAAO...", "..OAAAAAAO..", "..OAAOOAAO..", ".OAAO..OAAO.", ".OOO....OOO.", "............"],
    "blob": ["............", "............", "...OOOOOO...", "..OAAAAAAO..", ".OAAWWAAAAO.", ".OAAWAAAAAO.", "OAAAAAAAAAAO", "OAADAAAADAAO", "OAADAAAADAAO", "OAAAAAAAAAAO", ".OAAAAAAAAO.", "..OOOOOOOO.."],
    "flame": [".....O......", "....OAO.....", "....OAAO....", "...OAAAAO...", "..OAABAAAO..", "..OAABBAAO..", ".OAAABBBAAO.", ".OAABBBBBAO.", ".OAABWWBBAO.", "..OABWWBAO..", "...OABBAO...", "....OOOO...."],
    "helmet": ["............", "...OOOOOO...", "..OAAAAAAO..", ".OAAWWAAAAO.", ".OAAAAAAAAO.", ".OAAAAAAAAO.", ".OADDDDDDAO.", ".OADDBBDDAO.", ".OAADDDDAAO.", ".OAAA..AAAO.", ".OOOO..OOOO.", "............"],
    "book": ["............", ".OOOOOOOOOO.", ".OAAAAAAAAO.", ".OABBBBBBAO.", ".OAAAAAAAAO.", ".OAAWAAAAAO.", ".OAAAAAAAAO.", ".OAAAAAAAAO.", ".OAAAAAAAAO.", ".OOOOOOOOOO.", ".OWWWWWWWWO.", ".OOOOOOOOOO."],
    "dragon": ["O..........O", "OO...OO...OO", "OAO.OAAO.OAO", "OAAOAAAAOAAO", ".OAAAAAAAAO.", ".OAABAABAAO.", "..OAAAAAAO..", "..OAAAAAAO..", "...OAOOAO...", "....OAAO....", ".....OO.....", "............"],
    "creeper": ["............", ".OOOOOOOOOO.", ".OAAAAAAAAO.", ".OADDAADDAO.", ".OADDAADDAO.", ".OAAAAAAAAO.", ".OAAADDAAAO.", ".OAADDDDAAO.", ".OAADDDDAAO.", ".OAADAADAAO.", ".OOOOOOOOOO.", "............"],
    "flower": ["............", ".OO......OO.", "OAAO....OAAO", "OAAAOOOOAAAO", ".OAAADDAAAO.", "..OAADDAAO..", "...OODDOO...", "....OBBO....", "..OO.OBO.OO.", ".OBBOOBOOBBO", "..OOOOBOOOO.", "............"],
    "golem": ["............", "...OOOOOO...", "..OAAAAAAO..", "..OADAADAO..", "..OAAAAAAO..", ".OOOAAAAOOO.", "OAAOBBBBOAAO", "OAAOBBBBOAAO", ".OO.OBBO.OO.", "...OAAAAO...", "...OAOOAO...", "..OOO..OOO.."],
    "bug": ["............", "..OOO..OOO..", ".OBBBOOBBBO.", ".OBWBBBBWBO.", "..OOOAAOOO..", "....OAAO....", "...OADDAO...", "...OAAAAO...", "....OAAO....", ".....OAO....", "......O.....", "............"],
    "ember": [".....OO.....", "....OAAO....", "...OAWWAO...", "..OAAWBAAO..", ".OAAWBBBAAO.", "OAAABBBBAAAO", ".OAAABBBAAO.", "..OAAABBAO..", "...OAAAAO...", "....OAAO....", ".....OO.....", "............"],
    "butterfly": [".....O.O....", ".OOO....OOO.", "OAAAO..OAAAO", "OAWAAOOAAWAO", "OAAAABBAAAAO", ".OAAAODOAAAO", "..OOOODOOOO.", ".OAAAODOAAAO", "OAAWAODOAWAO", "OAAAOODOOAAO", ".OOO.ODO.OOO", "............"],
    "enderman": ["............", "...OOOOOO...", "..OAAAAAAO..", "..OAAAAAAO..", "..OBBAABBO..", "..OAAAAAAO..", "...OOOOOO...", "..OAAAAAAO..", ".OOAAAAAAOO.", ".OAOAAAAOAO.", ".OAOAAAAOAO.", ".OOOOOOOOOO."],
    "witchhat": [".......OO...", "......OAAO..", ".....OAAAO..", ".....OAAO...", "....OAAAAO..", "....OBBBBO..", "...OAAAAAAO.", "..OAAAAAAAAO", ".OAAAAAAAAAO", "OOOOOOOOOOOO", "............", "............"],
    "ferret": ["............", "..OO....OO..", ".OAAO..OAAO.", ".OAAOOOOAAO.", ".OAAAAAAAAO.", "OAADAAAADAAO", "OAAAAAAAAAAO", "OAAAABBAAAAO", ".OAAAADAAAO.", "..OAAAAAAO..", "...OOOOOO...", "............"],
    "villager": ["............", "...OOOOOO...", "..OAAAAAAO..", "..OBBBBBBO..", "..OWDAADWO..", "..OAAOOAAO..", "..OAAOOAAO..", "...OAOOAO...", "..OBBBBBBO..", ".OBBBAABBBO.", ".OBBAAAABBO.", ".OOOOOOOOOO."],
    "heart": ["............", "..OOO..OOO..", ".OAAAOOAAAO.", "OAAWAAAAAAAO", "OAAAAAAAAAAO", "OAAAAAAAAAAO", ".OAAAAAAAAO.", "..OAAAAAAO..", "...OAAAAO...", "....OAAO....", ".....OO.....", "............"],
}
for k, v in S.items():
    assert len(v) == 12 and all(len(r) == 12 for r in v), k


# ------------------------------------------------------------------ texto en pixeles (fuente 3x5)
F35 = {
    "A": ["010", "101", "111", "101", "101"], "B": ["110", "101", "110", "101", "110"], "C": ["011", "100", "100", "100", "011"],
    "D": ["110", "101", "101", "101", "110"], "E": ["111", "100", "110", "100", "111"], "F": ["111", "100", "110", "100", "100"],
    "G": ["011", "100", "101", "101", "011"], "H": ["101", "101", "111", "101", "101"], "I": ["111", "010", "010", "010", "111"],
    "J": ["001", "001", "001", "101", "010"], "K": ["101", "101", "110", "101", "101"], "L": ["100", "100", "100", "100", "111"],
    "M": ["101", "111", "111", "101", "101"], "N": ["110", "101", "101", "101", "101"], "O": ["010", "101", "101", "101", "010"],
    "P": ["110", "101", "110", "100", "100"], "Q": ["010", "101", "101", "110", "011"], "R": ["110", "101", "110", "101", "101"],
    "S": ["011", "100", "010", "001", "110"], "T": ["111", "010", "010", "010", "010"], "U": ["101", "101", "101", "101", "111"],
    "V": ["101", "101", "101", "101", "010"], "W": ["101", "101", "111", "111", "101"], "X": ["101", "101", "010", "101", "101"],
    "Y": ["101", "101", "010", "010", "010"], "Z": ["111", "001", "010", "100", "111"],
    "0": ["111", "101", "101", "101", "111"], "1": ["010", "110", "010", "010", "111"], "2": ["110", "001", "010", "100", "111"],
    "3": ["110", "001", "010", "001", "110"], "4": ["101", "101", "111", "001", "001"], "5": ["111", "100", "110", "001", "110"],
    "6": ["011", "100", "111", "101", "111"], "7": ["111", "001", "010", "010", "010"], "8": ["111", "101", "111", "101", "111"],
    "9": ["111", "101", "111", "001", "110"], "-": ["000", "000", "111", "000", "000"], ".": ["000", "000", "000", "000", "010"],
    "'": ["010", "010", "000", "000", "000"], " ": ["000", "000", "000", "000", "000"],
}
ACC = str.maketrans("ÁÉÍÓÚÜÑáéíóúüñ", "AEIOUUNaeiouun")


def text_w(t):
    return 4 * len(t) - 1


def draw_text(im, x, y, t, col):
    px = im.load()
    for ch in t.upper().translate(ACC):
        g = F35.get(ch, F35[" "])
        for yy, row in enumerate(g):
            for xx, bit in enumerate(row):
                if bit == "1" and 0 <= x + xx < im.size[0] and 0 <= y + yy < im.size[1]:
                    px[x + xx, y + yy] = col + (255,)
        x += 4


def name_lines(name, maxw=56):
    words = name.split()
    lines, cur = [], ""
    for w in words:
        t = (cur + " " + w).strip()
        if text_w(t) <= maxw:
            cur = t
        else:
            if cur:
                lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    return lines[:2]


# ------------------------------------------------------------------ skins de jugadores
def load_skin(path):
    sk = Image.open(os.path.join(ROOT, "herramientas", path)).convert("RGBA")
    if sk.size[1] == 32:  # skin antigua 64x32: el brazo izquierdo es el derecho volteado
        full = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        full.paste(sk, (0, 0))
        full.paste(sk.crop((44, 16, 48, 32)).transpose(Image.FLIP_LEFT_RIGHT), (36, 48))
        sk = full
    return sk


def skin_bust(path, scale):
    """Busto del jugador (cabeza, torso y brazos) sacado de su skin de Minecraft, 16 x 14 pixeles de skin."""
    sk = load_skin(path)

    def part(box, over):
        a = sk.crop(box)
        b = sk.crop(over)
        a.alpha_composite(b)
        return a
    bust = Image.new("RGBA", (16, 14), (0, 0, 0, 0))
    bust.alpha_composite(part((8, 8, 16, 16), (40, 8, 48, 16)), (4, 0))
    bust.alpha_composite(part((20, 20, 28, 26), (20, 36, 28, 42)), (4, 8))
    bust.alpha_composite(part((44, 20, 48, 26), (44, 36, 48, 42)), (0, 8))
    bust.alpha_composite(part((36, 52, 40, 58), (52, 52, 56, 58)), (12, 8))
    return bust.resize((16 * scale, 14 * scale), Image.NEAREST)


def palette(c1, c2):
    c1, c2 = hexrgb(c1), hexrgb(c2)
    return {"A": c1, "B": c2, "D": (14, 14, 24), "O": dark(c1, 0.45), "W": light(c1, 0.7)}


def sprite_img(glyph, c1, c2, scale):
    pal = palette(c1, c2)
    rows = S[glyph]
    im = Image.new("RGBA", (12 * scale, 12 * scale), (0, 0, 0, 0))
    px = im.load()
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch == ".":
                continue
            col = pal[ch] + (255,)
            for dy in range(scale):
                for dx in range(scale):
                    px[x * scale + dx, y * scale + dy] = col
    return im


# ------------------------------------------------------------------ iconos 16x16
def make_icon(card):
    rc = hexrgb(RARITY_COLOR[card["rar"]])
    c1 = hexrgb(card["c1"])
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([3, 0, 12, 15], fill=dark(rc, 0.55) + (255,))
    d.rectangle([4, 1, 11, 14], fill=rc + (255,))
    d.rectangle([5, 2, 10, 13], fill=dark(c1, 0.35) + (255,))
    d.rectangle([5, 12, 10, 13], fill=dark(rc, 0.7) + (255,))
    if card.get("skin"):
        head = skin_bust(card["skin"], 1).crop((4, 0, 12, 8))
        for y in range(6):
            for x in range(6):
                p = head.getpixel((min(7, round(x * 7 / 5)), min(7, round(y * 7 / 5))))
                if p[3]:
                    im.putpixel((5 + x, 3 + y), p)
        im.putpixel((4, 1), light(rc, 0.7) + (255,))
        return im
    big = sprite_img(card["glyph"], card["c1"], card["c2"], 1)
    px = big.load()
    for y in range(6):
        for x in range(6):
            p = px[2 * x, 2 * y]
            if p[3] == 0:
                p = px[2 * x + 1, 2 * y + 1]
            if p[3] == 0:
                p = px[2 * x + 1, 2 * y]
            if p[3]:
                im.putpixel((5 + x, 3 + y), p)
    im.putpixel((4, 1), light(rc, 0.7) + (255,))
    return im


# ------------------------------------------------------------------ escenas (arte grande 120x136 de dentro de la carta)
IW, IH = 120, 136          # zona del dibujo
GROUND = 104               # altura del suelo dentro de la zona

THEMES = {
    #           cielo arriba      cielo abajo       colinas lejanas   suelo (cesped)    tierra            brillo del cielo
    "forest": ((28, 66, 52), (150, 205, 120), (30, 84, 56), (74, 150, 62), (92, 62, 40), (255, 240, 170)),
    "forge": ((18, 8, 12), (112, 40, 12), (40, 22, 22), (66, 54, 58), (40, 30, 34), (255, 120, 30)),
    "ice": ((8, 22, 58), (130, 195, 232), (160, 205, 232), (232, 242, 252), (170, 200, 226), (150, 255, 220)),
    "end": ((6, 3, 18), (64, 24, 96), (30, 14, 50), (222, 222, 150), (110, 110, 80), (190, 110, 255)),
    "nether": ((52, 6, 8), (176, 54, 12), (90, 20, 20), (120, 34, 34), (80, 22, 22), (255, 150, 40)),
    "plains": ((70, 84, 170), (255, 186, 112), (60, 120, 80), (92, 168, 70), (110, 76, 48), (255, 230, 150)),
    "cave": ((8, 10, 18), (34, 38, 56), (40, 42, 56), (74, 74, 86), (52, 52, 62), (110, 220, 255)),
    "night": ((6, 10, 36), (44, 66, 118), (20, 44, 52), (44, 100, 60), (60, 44, 34), (220, 232, 255)),
    "swamp": ((26, 44, 38), (112, 142, 92), (34, 66, 48), (60, 104, 54), (70, 56, 40), (190, 130, 255)),
    "sky": ((52, 112, 224), (196, 232, 255), (140, 190, 240), (110, 190, 90), (120, 84, 54), (255, 250, 200)),
    "cherry": ((255, 176, 204), (255, 232, 238), (230, 150, 182), (116, 188, 98), (120, 84, 60), (255, 255, 255)),
}


def tint(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def scene_image(theme, seed):
    top, bot, far, grass, dirt, glow = THEMES[theme]
    rnd = random.Random(seed)
    im = Image.new("RGBA", (IW, IH), (0, 0, 0, 255))
    px = im.load()
    # cielo
    for y in range(IH):
        c = mix(top, bot, min(1.0, y / (GROUND + 6)) ** 1.1)
        for x in range(IW):
            px[x, y] = c + (255,)
    # resplandor / astro
    gx, gy = rnd.choice((34, 86)), 30 if theme != "forge" and theme != "nether" else 82
    big = 11 if theme in ("plains", "forest", "sky", "cherry") else 8
    for y in range(IH):
        for x in range(IW):
            d2 = ((x - gx) ** 2 + (y - gy) ** 2) ** 0.5
            if d2 < 46:
                k = (1 - d2 / 46) ** 2.2 * 0.55
                px[x, y] = mix(px[x, y][:3], glow, k) + (255,)
            if d2 < big and theme not in ("cave", "forge", "nether", "end"):
                px[x, y] = mix(glow, (255, 255, 255), 0.5) + (255,)
    if theme == "end":
        for _ in range(70):
            px[rnd.randrange(IW), rnd.randrange(0, 90)] = (230, 210, 255, 255)
    if theme in ("night", "ice", "swamp"):
        for _ in range(40):
            px[rnd.randrange(IW), rnd.randrange(0, 60)] = (235, 240, 255, 255)
    if theme == "ice":  # aurora
        for b, col in enumerate(((80, 255, 190), (90, 200, 255), (170, 120, 255))):
            for x in range(IW):
                yy = 18 + b * 7 + int(math.sin(x / 11.0 + b * 1.7) * 6)
                for k in range(-5, 7):
                    a = (1 - abs(k) / 7.0) * 0.5
                    if 0 <= yy + k < IH:
                        px[x, yy + k] = mix(px[x, yy + k][:3], col, a) + (255,)
    if theme in ("plains", "sky", "cherry"):  # nubes de bloques
        for _ in range(4):
            cx, cy = rnd.randrange(0, IW - 24), rnd.randrange(8, 52)
            for bx in range(rnd.randrange(3, 6)):
                for by in range(2):
                    x0, y0 = cx + bx * 6, cy + by * 4 - (bx % 2) * 2
                    for yy in range(y0, y0 + 5):
                        for xx in range(x0, x0 + 6):
                            if 0 <= xx < IW and 0 <= yy < IH:
                                px[xx, yy] = mix(px[xx, yy][:3], (255, 255, 255), 0.7 - by * 0.2) + (255,)
    # colinas / siluetas lejanas (columnas de 4 px)
    ph = [rnd.uniform(0, 6.28) for _ in range(3)]
    for x in range(IW):
        xq = x // 4 * 4
        h = GROUND - 14 - int(9 * math.sin(xq / 17.0 + ph[0]) + 6 * math.sin(xq / 7.0 + ph[1]) + 3 * math.sin(xq / 3.1 + ph[2]))
        if theme == "cave":  # estalactitas desde arriba
            hh = 10 + int(10 * abs(math.sin(xq / 6.0 + ph[0])))
            for y in range(0, hh):
                px[x, y] = tint(far, 0.9 - y * 0.01) + (255,)
            continue
        for y in range(max(0, h), GROUND):
            px[x, y] = tint(far, 1.0 + (y - h) * 0.004) + (255,)
    # suelo con bloques
    for y in range(GROUND, IH):
        for x in range(IW):
            depth = y - GROUND
            if depth < 4:
                c = grass
                if (x // 4 + y // 4) % 2:
                    c = tint(grass, 0.93)
            else:
                c = tint(dirt, 1.0 - min(0.5, depth * 0.01))
                if (x // 6 + y // 6 + (x // 12)) % 3 == 0:
                    c = tint(c, 0.9)
            if rnd.random() < 0.05:
                c = tint(c, 1.15)
            px[x, y] = c + (255,)
    for x in range(0, IW, 12):  # juntas de los bloques
        for y in range(GROUND + 4, IH):
            px[x, y] = tint(px[x, y][:3], 0.82) + (255,)
    for y in range(GROUND + 4, IH, 12):
        for x in range(IW):
            px[x, y] = tint(px[x, y][:3], 0.82) + (255,)
    d = ImageDraw.Draw(im)
    # decoracion del tema (por detras del personaje: a los lados)
    if theme in ("forest", "swamp", "cherry", "plains", "night"):
        for tx in (rnd.randrange(2, 14), rnd.randrange(100, 112), rnd.randrange(86, 96)):
            if theme == "plains" and tx not in (rnd.randrange(2, 14),):
                continue
            leaf = {"forest": (36, 112, 50), "swamp": (44, 84, 48), "cherry": (240, 150, 190), "plains": (56, 134, 56), "night": (24, 76, 48)}[theme]
            d.rectangle([tx + 5, GROUND - 22, tx + 8, GROUND], fill=(86, 58, 36, 255))
            for (bx, by, bw, bh) in ((tx - 4, GROUND - 40, 20, 10), (tx - 8, GROUND - 32, 28, 12), (tx - 2, GROUND - 48, 16, 8)):
                d.rectangle([bx, by, bx + bw, by + bh], fill=leaf + (255,))
                for _ in range(6):
                    qx, qy = bx + rnd.randrange(0, bw - 3), by + rnd.randrange(0, bh - 3)
                    d.rectangle([qx, qy, qx + 3, qy + 3], fill=tint(leaf, 1.25) + (255,))
    if theme == "ice":
        for tx in (4, 100, 84):
            for k in range(4):
                w = 6 + k * 5
                d.polygon([(tx + 7 - w // 2, GROUND - 26 + k * 9), (tx + 7 + w // 2, GROUND - 26 + k * 9), (tx + 7, GROUND - 36 + k * 9)], fill=(44, 92, 96 + k * 8, 255))
                d.line([(tx + 7 - w // 2, GROUND - 26 + k * 9), (tx + 7, GROUND - 36 + k * 9)], fill=(230, 245, 255, 255))
    if theme in ("forge", "nether"):  # rio de lava
        for x in range(IW):
            yy = GROUND + 10 + int(math.sin(x / 9.0) * 3)
            for y in range(yy, yy + 8):
                c = mix((255, 90, 10), (255, 220, 80), ((x * 7 + y * 3) % 17) / 17.0)
                px[x, y] = c + (255,)
        for y in range(GROUND - 6, GROUND + 10):  # calor sobre la lava
            for x in range(IW):
                px[x, y] = mix(px[x, y][:3], (255, 110, 20), 0.07) + (255,)
    if theme == "end":  # islas flotantes
        for (ix, iy, iw) in ((6, 52, 26), (92, 70, 22), (70, 36, 14)):
            d.rectangle([ix, iy, ix + iw, iy + 4], fill=(222, 222, 150, 255))
            d.polygon([(ix, iy + 5), (ix + iw, iy + 5), (ix + iw // 2, iy + 16)], fill=(96, 96, 70, 255))
    if theme == "cave":  # vetas brillantes
        for _ in range(16):
            ox, oy = rnd.randrange(2, IW - 4), rnd.randrange(GROUND + 6, IH - 4)
            col = rnd.choice(((110, 230, 255), (255, 210, 90)))
            d.rectangle([ox, oy, ox + 2, oy + 2], fill=col + (255,))
    # particulas del tema
    part = {"forest": (255, 245, 150), "forge": (255, 170, 50), "ice": (255, 255, 255), "end": (210, 140, 255),
            "nether": (255, 190, 60), "plains": (255, 255, 200), "cave": (140, 230, 255), "night": (200, 255, 150),
            "swamp": (200, 150, 255), "sky": (255, 255, 255), "cherry": (255, 210, 225)}[theme]
    for _ in range(30):
        x, y = rnd.randrange(0, IW), rnd.randrange(8, GROUND + 6)
        px[x, y] = part + (255,)
        if rnd.random() < 0.3 and x + 1 < IW:
            px[x + 1, y] = mix(px[x + 1, y][:3], part, 0.5) + (255,)
    # viñeta
    for y in range(IH):
        for x in range(IW):
            e = max(abs(x - IW / 2) / (IW / 2), abs(y - IH / 2) / (IH / 2))
            if e > 0.72:
                k = (e - 0.72) / 0.28
                px[x, y] = tint(px[x, y][:3], 1 - 0.38 * k) + (255,)
    return im


def hero_sprite(card, scale_glyph=8):
    """El dibujo principal: sprite de 12x12 ampliado, o el busto de la skin."""
    if card.get("skin"):
        return skin_bust(card["skin"], 6)
    return sprite_img(card["glyph"], card["c1"], card["c2"], scale_glyph)


def make_illustration(card):
    if card.get("art"):
        src = Image.open(os.path.join(ROOT, "herramientas", card["art"])).convert("RGBA")
        # recorte "cover": llena la zona sin deformar (el sujeto va centrado y arriba)
        r = max(IW / src.size[0], IH / src.size[1])
        nw, nh = max(IW, round(src.size[0] * r)), max(IH, round(src.size[1] * r))
        src = src.resize((nw, nh), Image.LANCZOS)
        left, topy = (nw - IW) // 2, 0
        out = src.crop((left, topy, left + IW, topy + IH))
        return out.filter(ImageFilter.UnsharpMask(radius=1.2, percent=70, threshold=2))
    theme = card.get("scene", "plains")
    im = scene_image(theme, card["id"])
    c1 = hexrgb(card["c1"])
    sp = hero_sprite(card)
    # sombra en el suelo
    sh = Image.new("RGBA", (IW, IH), (0, 0, 0, 0))
    d = ImageDraw.Draw(sh)
    sx = (IW - sp.size[0]) // 2
    sy = GROUND + 8 - sp.size[1] + 0
    d.ellipse([sx + 4, GROUND + 2, sx + sp.size[0] - 4, GROUND + 14], fill=(0, 0, 0, 120))
    im.alpha_composite(sh)
    # halo detras del personaje
    halo = Image.new("RGBA", (IW, IH), (0, 0, 0, 0))
    hp = halo.load()
    cx, cy = IW // 2, sy + sp.size[1] // 2
    for y in range(IH):
        for x in range(IW):
            dd = ((x - cx) / 44.0) ** 2 + ((y - cy) / 44.0) ** 2
            if dd < 1:
                hp[x, y] = light(c1, 0.4) + (int((1 - dd) ** 2 * 120),)
    im.alpha_composite(halo)
    # contorno oscuro del sprite
    mask = sp.split()[3].filter(ImageFilter.MaxFilter(5))
    ol = Image.new("RGBA", sp.size, (8, 8, 16, 255))
    ol.putalpha(mask)
    im.alpha_composite(ol, (sx, sy))
    im.alpha_composite(sp, (sx, sy))
    return im


# ------------------------------------------------------------------ marco y nombre (se dibuja a 64x96 y se amplia x2)
def make_chrome(card):
    rr = card["rar"]
    rc = hexrgb(RARITY_COLOR[rr])
    W, H = 64, 96
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = im.load()
    d = ImageDraw.Draw(im)
    d.rectangle([3, 70, W - 4, H - 4], fill=(10, 10, 18, 235))
    d.rectangle([3, 70, W - 4, 71], fill=rc + (255,))
    lines = name_lines(card["name"])
    y0 = 73 + (18 - (len(lines) * 5 + (len(lines) - 1) * 2)) // 2
    for i, ln in enumerate(lines):
        draw_text(im, (W - text_w(ln)) // 2, y0 + i * 7, ln, light(rc, 0.75))
    d.rectangle([0, 0, W - 1, H - 1], outline=dark(rc, 0.5) + (255,))
    d.rectangle([1, 1, W - 2, H - 2], outline=rc + (255,))
    d.rectangle([2, 2, W - 3, H - 3], outline=light(rc, 0.45) + (255,))
    ncorner = {"COMUN": 0, "RARA": 1, "EPICA": 2, "LEGENDARIA": 3, "MITICA": 4}[rr]
    for i in range(ncorner):
        for (x, y) in ((3 + i, 3), (3, 3 + i), (W - 4 - i, 3), (W - 4, 3 + i)):
            px[x, y] = light(rc, 0.8) + (255,)
        for (x, y) in ((3 + i, H - 4), (3, H - 4 - i), (W - 4 - i, H - 4), (W - 4, H - 4 - i)):
            px[x, y] = light(rc, 0.8) + (255,)
    d.rectangle([6, 6, 10, 10], fill=rc + (255,), outline=(10, 10, 18, 255))
    px[7, 7] = (255, 255, 255, 255)
    return im


CARD_W, CARD_H = 128, 192   # tamaño real de la textura de cada carta


def make_art(card):
    base = Image.new("RGBA", (CARD_W, CARD_H), (8, 8, 16, 255))
    base.paste(make_illustration(card), (4, 4))
    base.alpha_composite(make_chrome(card).resize((CARD_W, CARD_H), Image.NEAREST))
    return base


def make_back():
    W, H = 64, 96
    im = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    px = im.load()
    for y in range(H):
        for x in range(W):
            k = ((x + y) // 6 + (x - y) // 6) % 2
            px[x, y] = (30, 34, 72, 255) if k else (22, 26, 58, 255)
    d = ImageDraw.Draw(im)
    for i, col in enumerate(((90, 100, 190), (140, 150, 230), (60, 66, 140))):
        d.rectangle([i, i, W - 1 - i, H - 1 - i], outline=col + (255,))
    st = sprite_img("star", "#c8d0ff", "#ffffff", 3)
    im.alpha_composite(st, (14, 30))
    return im


def make_pack_art(p):
    W, H = 64, 96
    col = hexrgb(p["color"])
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = im.load()
    for y in range(4, H - 4):
        t = (y - 4) / (H - 8)
        for x in range(6, W - 6):
            sh = 1.0 - abs((x - 32) / 26.0) * 0.35
            c = mix(light(col, 0.35), dark(col, 0.6), t)
            px[x, y] = tuple(int(v * sh) for v in c) + (255,)
    d = ImageDraw.Draw(im)
    for x in range(6, W - 6, 4):  # bordes sellados en zigzag
        d.polygon([(x, 4), (x + 2, 8), (x + 4, 4)], fill=dark(col, 0.45) + (255,))
        d.polygon([(x, H - 5), (x + 2, H - 9), (x + 4, H - 5)], fill=dark(col, 0.45) + (255,))
    d.rectangle([6, 4, W - 7, H - 5], outline=dark(col, 0.4) + (255,))
    d.rectangle([14, 36, W - 15, 70], fill=(10, 10, 18, 120), outline=light(col, 0.5) + (255,))
    st = sprite_img("star", "#ffffff", "#ffe27a", 3)
    im.alpha_composite(st, (14, 40))
    return im


def make_pack_icon(p):
    col = hexrgb(p["color"])
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([3, 1, 12, 14], fill=dark(col, 0.5) + (255,))
    d.rectangle([4, 2, 11, 13], fill=col + (255,))
    d.rectangle([4, 2, 5, 13], fill=light(col, 0.35) + (255,))
    for x in range(3, 13, 2):
        im.putpixel((x, 0), dark(col, 0.5) + (255,))
        im.putpixel((x, 15), dark(col, 0.5) + (255,))
    d.rectangle([6, 6, 10, 9], fill=(255, 255, 255, 220))
    im.putpixel((8, 7), light(col, 0.2) + (255,))
    return im


def make_album_icon():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([2, 1, 13, 14], fill=(70, 34, 20, 255))
    d.rectangle([3, 2, 12, 13], fill=(122, 60, 34, 255))
    d.rectangle([3, 2, 4, 13], fill=(90, 44, 26, 255))
    d.rectangle([13, 3, 13, 12], fill=(235, 225, 200, 255))
    for (x, y) in ((8, 5), (7, 6), (8, 6), (9, 6), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (7, 8), (8, 8), (9, 8), (7, 9), (9, 9)):
        im.putpixel((x, y), (255, 214, 90, 255))
    return im


# ------------------------------------------------------------------ escritura
def jstr(s):
    out = ""
    for ch in s:
        if ch == '"':
            out += '\\"'
        elif ch == "\\":
            out += "\\\\"
        elif ord(ch) > 126:
            out += "\\u%04x" % ord(ch)
        else:
            out += ch
    return '"' + out + '"'


def model(layer):
    return json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": "mundos_de_axelis:item/" + layer}}, indent=2)


def main():
    cats = data["categorias"]
    cards = data["cartas"]
    packs = data["sobres"]
    ids = [c["id"] for c in cards]
    assert len(ids) == len(set(ids)), "ids repetidos"
    catids = {c["id"] for c in cats}
    for c in cards:
        assert c["cat"] in catids and c["rar"] in RARITY_COLOR and c["glyph"] in S, c["id"]

    # texturas y modelos
    for c in cards:
        make_icon(c).save(os.path.join(T_ITEM, "carta_%s.png" % c["id"]))
        make_art(c).save(os.path.join(T_GUI, "%s.png" % c["id"]))
        open(os.path.join(M_ITEM, "carta_%s.json" % c["id"]), "w").write(model("carta_" + c["id"]))
    make_back().resize((CARD_W, CARD_H), Image.NEAREST).save(os.path.join(T_GUI, "reverso.png"))
    for p in packs:
        make_pack_icon(p).save(os.path.join(T_ITEM, "sobre_%s.png" % p["id"]))
        make_pack_art(p).save(os.path.join(T_GUI, "sobre_%s.png" % p["id"]))
        open(os.path.join(M_ITEM, "sobre_%s.json" % p["id"]), "w").write(model("sobre_" + p["id"]))
    make_album_icon().save(os.path.join(T_ITEM, "album_cartas.png"))
    open(os.path.join(M_ITEM, "album_cartas.json"), "w").write(model("album_cartas"))

    # CardData.java
    L = []
    L.append("package com.mundosdeaxelis.cartas;\n")
    L.append("/** GENERADO por herramientas/gen.py a partir de cartas.json. No editar a mano. */")
    L.append("final class CardData {")
    L.append("    private CardData() {\n    }\n")
    L.append("    /** id, nombre, color */")
    L.append("    static final String[][] CATEGORIES = {")
    for c in cats:
        L.append("        {%s, %s, %s}," % (jstr(c["id"]), jstr(c["name"]), jstr(c["color"])))
    L.append("    };\n")
    L.append("    /** id, nombre, categoria, rareza, descripcion, color1, color2 */")
    L.append("    static final String[][] CARDS = {")
    for c in cards:
        L.append("        {%s, %s, %s, %s, %s, %s, %s}," % tuple(jstr(x) for x in (c["id"], c["name"], c["cat"], c["rar"], c["desc"], c["c1"], c["c2"])))
    L.append("    };\n")
    L.append("    /** id, nombre, color, cartas, rareza minima garantizada, prob. holografica */")
    L.append("    static final String[][] PACKS = {")
    for p in packs:
        L.append("        {%s, %s, %s, %s, %s, %s}," % tuple(jstr(str(x)) for x in (p["id"], p["name"], p["color"], p["cartas"], p["min"], p["holo"])))
    L.append("    };\n")
    L.append("    /** probabilidades (en %) por sobre, en el orden COMUN, RARA, EPICA, LEGENDARIA, MITICA */")
    L.append("    static final double[][] ODDS = {")
    for p in packs:
        o = p["odds"]
        L.append("        {%s}," % ", ".join(repr(float(o[k])) for k in ("COMUN", "RARA", "EPICA", "LEGENDARIA", "MITICA")))
    L.append("    };")
    L.append("}")
    open(JAVA, "w", encoding="utf-8").write("\n".join(L) + "\n")

    # lang
    lang = {
        "itemGroup.mundos_de_axelis.cartas": "Mundos de Axelis: Cartas",
        "key.categories.mundos_de_axelis_cartas": "Mundos de Axelis: Cartas",
        "key.mundos_de_axelis.cartas.abrir": "Abrir el álbum",
        "item.mundos_de_axelis.album_cartas": "Álbum de Cartas",
    }
    for p in packs:
        lang["item.mundos_de_axelis.sobre_" + p["id"]] = p["name"]
    for c in cards:
        lang["item.mundos_de_axelis.carta_" + c["id"]] = "Carta: " + c["name"]
    for n in ("es_mx", "es_es", "es_ar", "es_cl", "es_uy", "es_ve", "en_us"):
        json.dump(lang, open(os.path.join(LANG, n + ".json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("listo:", len(cards), "cartas,", len(packs), "sobres")


main()
