#!/usr/bin/env python3
"""Silla de tragamonedas (casino:slot_chair): taburete alto de pie cromado con asiento redondo de terciopelo rojo y respaldo bajo.
Es distinta de la silla de casino (esa queda solo para las carreras de caballos).

    python3 casino/herramientas/slot_chair.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import decor_models as dm  # noqa: E402
from decor_models import box, octagon, model, write, A  # noqa: E402


def slot_chair():
    e = []
    # pie: disco dorado, base cromada, columna y anillo
    e += octagon([3, 0, 3], [13, 0.8, 13], "gold")
    e += octagon([4.5, 0.8, 4.5], [11.5, 1.5, 11.5], "chrome")
    e += [box([7.1, 1.5, 7.1], [8.9, 9.6, 8.9], "chrome"),
          box([6.6, 5.0, 6.6], [9.4, 5.6, 9.4], "gold")]
    # reposapiés al frente (lado norte), unido a la columna
    e += [box([4.2, 4.6, 3.2], [11.8, 5.2, 4.0], "gold"),
          box([4.2, 4.6, 4.0], [5.0, 5.2, 7.4], "gold"),
          box([11.0, 4.6, 4.0], [11.8, 5.2, 7.4], "gold"),
          box([5.0, 4.7, 7.4], [11.0, 5.1, 8.2], "chrome")]
    # asiento redondo: aro dorado, cojín y botones
    e += [box([6.4, 9.6, 6.4], [9.6, 10.0, 9.6], "gold")]
    e += octagon([2.8, 10.0, 2.8], [13.2, 10.8, 13.2], "gold")
    e += octagon([3.0, 10.8, 3.0], [13.0, 12.8, 13.0], "#v")
    e += octagon([3.8, 12.8, 3.8], [12.2, 13.3, 12.2], "#vt", faces=["up", "north", "south", "west", "east"])
    # respaldo bajo, acolchado, con marco dorado
    e += [box([3.6, 12.6, 12.4], [12.4, 20.2, 13.4], "#vt", faces=["north", "up", "west", "east"]),
          box([3.0, 12.2, 13.4], [13.0, 20.8, 14.4], "lac"),
          box([2.8, 20.8, 13.2], [13.2, 21.5, 14.6], "gold"),
          box([5.5, 21.5, 13.5], [10.5, 22.2, 14.3], "gold"),
          box([2.6, 12.2, 13.2], [3.2, 20.8, 14.6], "gold"),
          box([12.8, 12.2, 13.2], [13.4, 20.8, 14.6], "gold"),
          box([7.2, 10.8, 13.4], [8.8, 12.2, 14.2], "chrome")]
    model("slot_chair", e, textures={"v": "casino:block/decor/velvet_red", "vt": "casino:block/decor/velvet_red_tufted"},
          particle="casino:block/decor/velvet_red")


NAMES = {"es": "Silla de tragamonedas", "en_us": "Slot Chair", "de_de": "Spielautomaten-Stuhl", "fr_fr": "Chaise de machine à sous",
         "it_it": "Sedia da slot", "pt_br": "Cadeira de caça-níquel", "ru_ru": "Стул для игровых автоматов", "ja_jp": "スロット用チェア",
         "zh_cn": "老虎机座椅"}


def gen():
    slot_chair()
    dm.facing_states("slot_chair", "slot_chair")
    dm.item("slot_chair", "slot_chair", dm.DISPLAY_TALL)
    dm.loot("slot_chair")
    dm.recipe("slot_chair", ["W  ", "WGW", " I "], {"W": "minecraft:red_wool", "G": "minecraft:gold_ingot", "I": "minecraft:iron_ingot"}, 2)
    ldir = os.path.join(A, "lang")
    for fn in sorted(os.listdir(ldir)):
        p = os.path.join(ldir, fn)
        with open(p, encoding="utf-8") as fh:
            lang = json.load(fh)
        code = fn[:-5]
        lang["block.casino.slot_chair"] = NAMES["es"] if code.startswith("es") else NAMES.get(code, NAMES["en_us"])
        with open(p, "w", encoding="utf-8") as fh:
            json.dump(lang, fh, ensure_ascii=False, indent=2)
            fh.write("\n")


if __name__ == "__main__":
    gen()
    import zfix
    zfix.run(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/resources/assets/casino/models"))
