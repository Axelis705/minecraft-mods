#!/usr/bin/env python3
"""Pisos y paredes del casino: alfombras (capa y bloque), mármol blanco y negro (bloque, escalera, losa), panel de caoba,
moldura dorada y pared de terciopelo capitoné.

    python3 casino/herramientas/floor_models.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import decor_models as dm  # noqa: E402
from decor_models import write, A, D  # noqa: E402

T = "casino:block/decor/"


def mdl(name, parent, textures):
    write(os.path.join(A, "models", "block", "decor", name + ".json"), {"parent": parent, "textures": textures})


def simple_item(name, model):
    write(os.path.join(A, "models", "item", name + ".json"), {"parent": "casino:block/decor/" + model})


def cube(name, tex, es, en, recipe):
    mdl(name, "minecraft:block/cube_all", {"all": T + tex})
    write(os.path.join(A, "blockstates", name + ".json"), {"variants": {"": {"model": "casino:block/decor/" + name}}})
    simple_item(name, name)
    dm.names(name, es, en)
    dm.loot(name)
    dm.recipe(name, *recipe)


def carpet(name, tex, es, en, block):
    mdl(name, "minecraft:block/carpet", {"wool": T + tex})
    write(os.path.join(A, "blockstates", name + ".json"), {"variants": {"": {"model": "casino:block/decor/" + name}}})
    simple_item(name, name)
    dm.names(name, es, en)
    dm.loot(name)
    write(os.path.join(D, "recipes", name + ".json"), {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["BB"],
                                                      "key": {"B": {"item": "casino:" + block}}, "result": {"item": "casino:" + name, "count": 3}})


def stairs(name, tex, es, en, base):
    t = {"bottom": T + tex, "top": T + tex, "side": T + tex}
    mdl(name, "minecraft:block/stairs", t)
    mdl(name + "_inner", "minecraft:block/inner_stairs", t)
    mdl(name + "_outer", "minecraft:block/outer_stairs", t)
    v = {}
    rot = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, y0 in rot.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                m = name + ("" if shape == "straight" else "_inner" if shape.startswith("inner") else "_outer")
                y = y0
                if shape in ("inner_left", "outer_left"):
                    y = (y + 270) % 360
                x = 0
                if half == "top":
                    x = 180
                    if shape in ("inner_left", "outer_left"):
                        y = (y + 90) % 360
                    elif shape in ("inner_right", "outer_right"):
                        y = (y + 90) % 360
                e = {"model": "casino:block/decor/" + m}
                if x:
                    e["x"] = x
                if y:
                    e["y"] = y
                if shape != "straight" or x:
                    e["uvlock"] = True
                v[f"facing={facing},half={half},shape={shape}"] = e
    write(os.path.join(A, "blockstates", name + ".json"), {"variants": v})
    simple_item(name, name)
    dm.names(name, es, en)
    dm.loot(name)
    dm.recipe(name, ["B  ", "BB ", "BBB"], {"B": "casino:" + base}, 4)


def slab(name, tex, es, en, base):
    t = {"bottom": T + tex, "top": T + tex, "side": T + tex}
    mdl(name, "minecraft:block/slab", t)
    mdl(name + "_top", "minecraft:block/slab_top", t)
    write(os.path.join(A, "blockstates", name + ".json"), {"variants": {
        "type=bottom": {"model": "casino:block/decor/" + name}, "type=top": {"model": "casino:block/decor/" + name + "_top"},
        "type=double": {"model": "casino:block/decor/" + base}}})
    simple_item(name, name)
    dm.names(name, es, en)
    write(os.path.join(D, "loot_tables", "blocks", name + ".json"), {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "casino:" + name, "functions": [
            {"function": "minecraft:set_count", "count": 2.0, "add": False,
             "conditions": [{"condition": "minecraft:block_state_property", "block": "casino:" + name, "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})
    dm.recipe(name, ["BBB"], {"B": "casino:" + base}, 6)


def gen():
    pats = {"royal": ("real", "Royal", "minecraft:red_wool", "minecraft:gold_nugget"),
            "galaxy": ("galaxia", "Galaxy", "minecraft:purple_wool", "minecraft:cyan_dye"),
            "deco": ("art déco", "Art Deco", "minecraft:black_wool", "minecraft:gold_nugget"),
            "lounge": ("lounge", "Lounge", "minecraft:blue_wool", "minecraft:red_dye")}
    for p, (es, en, wool, extra) in pats.items():
        cube(f"casino_floor_{p}", f"carpet_{p}", f"Piso de casino {es}", f"{en} Casino Floor",
             (["WW", "WE"], {"W": wool, "E": extra}, 4))
        carpet(f"casino_carpet_{p}", f"carpet_{p}", f"Alfombra de casino {es}", f"{en} Casino Carpet", f"casino_floor_{p}")
    cube("marble", "marble", "Mármol", "Marble", (["QQ", "QQ"], {"Q": "minecraft:smooth_quartz"}, 4))
    cube("marble_black", "marble_black", "Mármol negro", "Black Marble", (["BB", "BG"], {"B": "minecraft:polished_blackstone", "G": "minecraft:gold_nugget"}, 4))
    stairs("marble_stairs", "marble", "Escaleras de mármol", "Marble Stairs", "marble")
    stairs("marble_black_stairs", "marble_black", "Escaleras de mármol negro", "Black Marble Stairs", "marble_black")
    slab("marble_slab", "marble", "Losa de mármol", "Marble Slab", "marble")
    slab("marble_black_slab", "marble_black", "Losa de mármol negro", "Black Marble Slab", "marble_black")
    cube("mahogany_panel", "mahogany_panel", "Panel de caoba", "Mahogany Panel", (["PP", "PP"], {"P": "minecraft:dark_oak_planks"}, 4))
    cube("gold_trim", "gold_trim", "Moldura dorada", "Gold Trim", (["GGG"], {"G": "minecraft:gold_ingot"}, 6))
    for c in dm.COLORS:
        cube(f"velvet_wall_{c}", f"velvet_{c}_tufted", f"Pared de terciopelo {dm.COLOR_ES[c]}", f"{dm.COLOR_EN[c]} Velvet Wall",
             (["WW", "WW"], {"W": dm.WOOL[c]}, 4))
    for lang, extra in (("es_es", dm.LANG_ES), ("en_us", dm.LANG_EN)):
        p = os.path.join(A, "lang", lang + ".json")
        with open(p, encoding="utf-8") as fh:
            data = json.load(fh)
        data.update(extra)
        with open(p, "w", encoding="utf-8") as fh:
            json.dump(data, fh, indent=2, ensure_ascii=False)
            fh.write("\n")
    print("pisos y paredes listos")


if __name__ == "__main__":
    gen()
