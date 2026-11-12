#!/usr/bin/env python3
"""Modelo de la bocina de jazz."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import decor_models as dm  # noqa: E402
from decor_models import box, write, A  # noqa: E402

dm.TEX["grille"] = "casino:block/decor/speaker_grille"
e = [box([2, 1, 4], [14, 15.6, 13], "mah"),
     box([3, 2, 3.9], [13, 14, 4], "grille", faces=["north"], uv="full"),
     box([2.6, 14, 3.7], [13.4, 14.5, 4], "gold"), box([2.6, 1.5, 3.7], [13.4, 2, 4], "gold"),
     box([2.6, 2, 3.7], [3.1, 14, 4], "gold"), box([12.9, 2, 3.7], [13.4, 14, 4], "gold"),
     box([1.6, 15.6, 3.6], [14.4, 16.2, 13.4], "gold"),
     box([3, 16.2, 5], [13, 16.6, 12], "mah"),
     box([1.8, 1, 3.8], [2.2, 15.6, 4.6], "gold", faces=["north", "west", "east"]),
     box([13.8, 1, 3.8], [14.2, 15.6, 4.6], "gold", faces=["north", "west", "east"])]
for x in (2.5, 11.5):
    for z in (4.5, 10.5):
        e.append(box([x, 0, z], [x + 2, 1, z + 2], "gold"))
dm.model("speaker", e, particle="casino:block/decor/mahogany")
v = {}
for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
    for lit in ("true", "false"):
        v[f"facing={f},lit={lit}"] = {"model": "casino:block/decor/speaker", "y": y}
write(os.path.join(A, "blockstates", "speaker.json"), {"variants": v})
dm.item("speaker", "speaker", dm.DISPLAY)
dm.names("speaker", "Bocina de jazz", "Jazz Speaker")
dm.loot("speaker")
dm.recipe("speaker", ["PGP", "PNP", "PRP"], {"P": "minecraft:dark_oak_planks", "G": "minecraft:gold_ingot", "N": "minecraft:note_block", "R": "minecraft:redstone"})
dm.LANG_ES["subtitles.casino.music_lounge"] = "Suena jazz de casino"
dm.LANG_EN["subtitles.casino.music_lounge"] = "Casino jazz plays"
for lang, extra in (("es_es", dm.LANG_ES), ("en_us", dm.LANG_EN)):
    p = os.path.join(A, "lang", lang + ".json")
    with open(p, encoding="utf-8") as fh:
        data = json.load(fh)
    data.update(extra)
    with open(p, "w", encoding="utf-8") as fh:
        json.dump(data, fh, indent=2, ensure_ascii=False)
        fh.write("\n")
print("bocina lista")
