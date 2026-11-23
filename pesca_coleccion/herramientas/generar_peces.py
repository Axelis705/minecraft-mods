"""Genera todo lo de los 25 peces a partir de especies.py: dibujos (objeto 16 px, libro 32 px y su sombra), modelos de objeto, datos
para el juego (resources/pesca_coleccion/peces.json) y textos (es_es, es_mx, en_us).

Uso (desde la carpeta del mod): python3 herramientas/generar_peces.py [--hoja hoja.png]
"""
import collections
import json
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from especies import ESPECIES, RAREZAS, LUGAR, HORA, CLIMA, LUNA, lista  # noqa: E402
from peces import draw_fish  # noqa: E402

MOD = "pesca_coleccion"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", MOD)


def sombra(img):
    """Silueta oscura (para los peces que todavía no se pescan en el libro)."""
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    p, q = img.load(), out.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            if p[x, y][3]:
                edge = any(not (0 <= x + dx < w and 0 <= y + dy < h) or not p[x + dx, y + dy][3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                q[x, y] = (58, 66, 84, 255) if edge else (24, 29, 40, 255)
    return out


def main():
    os.makedirs(os.path.join(ASSETS, "textures", "item"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "textures", "gui", "peces"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "models", "item"), exist_ok=True)
    datos = []
    sheet_items = []
    for e in ESPECIES:
        art = dict(e["arte"])
        small = draw_fish(art, 16)
        big = draw_fish(art, 32)
        small.save(os.path.join(ASSETS, "textures", "item", f"pez_{e['id']}.png"))
        big.save(os.path.join(ASSETS, "textures", "gui", "peces", f"{e['id']}.png"))
        sombra(big).save(os.path.join(ASSETS, "textures", "gui", "peces", f"{e['id']}_sombra.png"))
        with open(os.path.join(ASSETS, "models", "item", f"pez_{e['id']}.json"), "w", encoding="utf-8") as fh:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/pez_{e['id']}"}}, fh, indent=2)
        datos.append({"id": e["id"], "rareza": e["rareza"], "lugar": lista(e["lugar"]), "hora": lista(e["hora"]),
                      "clima": e.get("clima", "cualquiera"), "luna": e.get("luna", "cualquiera"), "cm": list(e["cm"]), "kg": e["kg"],
                      "pelea": e.get("pelea", {})})
        sheet_items.append((e, small, big))
    os.makedirs(os.path.join(ROOT, MOD), exist_ok=True)
    with open(os.path.join(ROOT, MOD, "peces.json"), "w", encoding="utf-8") as fh:
        json.dump(datos, fh, indent=1, ensure_ascii=False)
    # textos
    for lang, k in (("es_es", 0), ("es_mx", 0), ("en_us", 1)):
        path = os.path.join(ASSETS, "lang", lang + ".json")
        with open(path, encoding="utf-8") as fh:
            j = json.load(fh, object_pairs_hook=collections.OrderedDict)
        for e in ESPECIES:
            j[f"item.{MOD}.pez_{e['id']}"] = e["nombre"] if k == 0 else e["name"]
            j[f"{MOD}.pez.{e['id']}.historia"] = e["historia"][k]
        for table, prefix in ((LUGAR, "lugar"), (HORA, "hora"), (CLIMA, "clima"), (LUNA, "luna")):
            for code, txt in table.items():
                if txt:
                    j[f"{MOD}.{prefix}.{code}"] = txt[k]
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(j, fh, indent=2, ensure_ascii=False)
    print(f"{len(ESPECIES)} peces: dibujos, modelos, datos y textos listos")
    if "--hoja" in sys.argv:
        out = sys.argv[sys.argv.index("--hoja") + 1]
        cols, cell = 5, 140
        rows = (len(sheet_items) + cols - 1) // cols
        sh = Image.new("RGBA", (cols * cell, rows * (cell + 10)), (40, 44, 54, 255))
        colors = [(214, 217, 224), (124, 227, 139), (111, 183, 255), (197, 140, 255), (255, 193, 77)]
        from PIL import ImageDraw
        d = ImageDraw.Draw(sh)
        for i, (e, small, big) in enumerate(sheet_items):
            x, y = (i % cols) * cell, (i // cols) * (cell + 10)
            bg = Image.new("RGBA", (128, 128), (64, 56, 44, 255))
            bg.alpha_composite(big.resize((128, 128), Image.NEAREST))
            sh.alpha_composite(bg, (x + 6, y + 4))
            sm = Image.new("RGBA", (32, 32), (139, 139, 139, 255))
            sm.alpha_composite(small.resize((32, 32), Image.NEAREST))
            sh.alpha_composite(sm, (x + 6 + 128 - 34, y + 4 + 2))
            d.text((x + 8, y + 134), e["nombre"][:22], fill=colors[e["rareza"]])
        sh.save(out)
        print("hoja:", out)


if __name__ == "__main__":
    main()
