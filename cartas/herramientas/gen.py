#!/usr/bin/env python3
"""Genera, a partir de cartas.json: CardData.java, texturas (icono 16x16 y arte 64x96), modelos de item y lang.
Uso (desde la carpeta cartas/):  python3 herramientas/gen.py
El arte es pixel art PROVISIONAL hecho por codigo; se puede reemplazar cualquier PNG a mano sin tocar nada mas."""
import json, os, random
from PIL import Image, ImageDraw

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
    "heart": ["............", "..OOO..OOO..", ".OAAAOOAAAO.", "OAAWAAAAAAAO", "OAAAAAAAAAAO", "OAAAAAAAAAAO", ".OAAAAAAAAO.", "..OAAAAAAO..", "...OAAAAO...", "....OAAO....", ".....OO.....", "............"],
}
for k, v in S.items():
    assert len(v) == 12 and all(len(r) == 12 for r in v), k


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


# ------------------------------------------------------------------ arte 64x96
def make_art(card):
    rr = card["rar"]
    rc = hexrgb(RARITY_COLOR[rr])
    c1, c2 = hexrgb(card["c1"]), hexrgb(card["c2"])
    W, H = 64, 96
    im = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    px = im.load()
    rnd = random.Random(card["id"])
    top, bot = dark(c1, 0.32), dark(c2, 0.22)
    for y in range(H):
        t = y / (H - 1)
        base = mix(top, bot, t)
        for x in range(W):
            px[x, y] = base + (255,)
    # resplandor detras del dibujo
    cx, cy = 32, 38
    for y in range(H):
        for x in range(W):
            dd = ((x - cx) ** 2 / 26.0 ** 2 + (y - cy) ** 2 / 26.0 ** 2)
            if dd < 1:
                k = (1 - dd) ** 2 * 0.55
                px[x, y] = mix(px[x, y][:3], light(c1, 0.3), k) + (255,)
    # estrellitas
    for _ in range(26):
        x, y = rnd.randrange(4, W - 4), rnd.randrange(4, 66)
        px[x, y] = light(c1, 0.8) + (255,)
    d = ImageDraw.Draw(im)
    # sprite con sombra
    sp = sprite_img(card["glyph"], card["c1"], card["c2"], 4)
    sh = Image.new("RGBA", sp.size, (0, 0, 0, 0))
    spx, shx = sp.load(), sh.load()
    for y in range(sp.size[1]):
        for x in range(sp.size[0]):
            if spx[x, y][3]:
                shx[x, y] = (0, 0, 0, 110)
    im.alpha_composite(sh, (10, 18))
    im.alpha_composite(sp, (8, 14))
    # banda del nombre
    d.rectangle([3, 70, W - 4, H - 4], fill=(10, 10, 18, 235))
    d.rectangle([3, 70, W - 4, 71], fill=rc + (255,))
    # marco
    d.rectangle([0, 0, W - 1, H - 1], outline=dark(rc, 0.5) + (255,))
    d.rectangle([1, 1, W - 2, H - 2], outline=rc + (255,))
    d.rectangle([2, 2, W - 3, H - 3], outline=light(rc, 0.45) + (255,))
    # adornos de esquina segun rareza
    ncorner = {"COMUN": 0, "RARA": 1, "EPICA": 2, "LEGENDARIA": 3, "MITICA": 4}[rr]
    for i in range(ncorner):
        for (x, y) in ((3 + i, 3), (3, 3 + i), (W - 4 - i, 3), (W - 4, 3 + i)):
            px[x, y] = light(rc, 0.8) + (255,)
        for (x, y) in ((3 + i, H - 4), (3, H - 4 - i), (W - 4 - i, H - 4), (W - 4, H - 4 - i)):
            px[x, y] = light(rc, 0.8) + (255,)
    # gema de rareza arriba a la izquierda
    d.rectangle([6, 6, 10, 10], fill=rc + (255,), outline=(10, 10, 18, 255))
    px[7, 7] = (255, 255, 255, 255)
    return im


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
    make_back().save(os.path.join(T_GUI, "reverso.png"))
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
        "key.mundos_de_axelis.cartas.insertar": "Guardar cartas en el álbum",
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
