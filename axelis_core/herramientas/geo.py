"""Ayudas para escribir modelos (.geo.json) y animaciones (.animation.json) de Blockbench desde Python.

Coordenadas de Blockbench/Bedrock: píxeles, y hacia ARRIBA, los pies en y=0, el frente del modelo mira a -z (norte).
Los ángulos van en grados (el Core los pasa a Java con el mismo signo: x negativo = hacia delante/arriba para brazos y piernas).
"""
import json
import os


class Model:
    def __init__(self, ident, tw=64, th=64):
        self.ident = ident
        self.tw, self.th = tw, th
        self.bones = []
        self.bends = []
        self.glow = []

    def bone(self, name, pivot, parent=None, rotation=None):
        b = {"name": name, "pivot": [round(v, 4) for v in pivot]}
        if parent:
            b["parent"] = parent
        if rotation and any(rotation):
            b["rotation"] = list(rotation)
        b["cubes"] = []
        self.bones.append(b)
        return b

    def cube(self, bone, origin, size, uv=None, inflate=0, mirror=False, faces=None, rotation=None, pivot=None):
        b = self.get(bone)
        c = {"origin": [round(v, 4) for v in origin], "size": [round(v, 4) for v in size]}
        if faces is not None:
            c["uv"] = faces
        else:
            c["uv"] = list(uv)
        if inflate:
            c["inflate"] = inflate
        if mirror:
            c["mirror"] = True
        if rotation:
            c["rotation"] = list(rotation)
            c["pivot"] = list(pivot)
        b["cubes"].append(c)
        return c

    def get(self, name):
        for b in self.bones:
            if b["name"] == name:
                return b
        raise KeyError(name)

    def bend(self, bone, child, at):
        self.bends.append({"bone": bone, "child": child, "at": at})

    def json(self):
        bones = []
        for b in self.bones:
            d = dict(b)
            if not d["cubes"]:
                del d["cubes"]
            bones.append(d)
        g = {"description": {"identifier": self.ident, "texture_width": self.tw, "texture_height": self.th},
             "bones": bones}
        if self.bends:
            g["axelis_bends"] = self.bends
        if self.glow:
            g["axelis_glow"] = self.glow
        return {"format_version": "1.12.0", "minecraft:geometry": [g]}

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(self.json(), fh, indent=1)


def r3(v):
    return [round(x, 3) if isinstance(x, (int, float)) else x for x in v]


class Anim:
    """Una animación: anim.rot("brazo", {0: [0,0,0], 0.5: ([-30,0,0], "easeInOutSine")})"""

    def __init__(self, name, length, loop=False):
        self.name = name
        self.length = length
        self.loop = loop
        self.bones = {}
        self.sounds = {}
        self.timeline = {}

    def _chan(self, bone, kind, keys):
        if isinstance(keys, (list, tuple)) and (len(keys) == 0 or not isinstance(keys[0], (list, tuple, dict))) and not isinstance(keys, dict):
            self.bones.setdefault(bone, {})[kind] = r3(keys)
            return
        out = {}
        for t, v in sorted(keys.items()):
            k = f"{float(t):.4f}".rstrip("0").rstrip(".") if t else "0.0"
            if "." not in k:
                k += ".0"
            if isinstance(v, tuple):
                vec, ease = v
                if ease == "catmullrom":
                    out[k] = {"post": r3(vec), "lerp_mode": "catmullrom"}
                elif ease == "step":
                    out[k] = {"post": r3(vec), "lerp_mode": "step"}
                else:
                    out[k] = {"vector": r3(vec), "easing": ease}
            else:
                out[k] = r3(v)
        self.bones.setdefault(bone, {})[kind] = out

    def rot(self, bone, keys):
        self._chan(bone, "rotation", keys)
        return self

    def pos(self, bone, keys):
        self._chan(bone, "position", keys)
        return self

    def scale(self, bone, keys):
        self._chan(bone, "scale", keys)
        return self

    def sound(self, t, name):
        self.sounds[f"{t:.4f}".rstrip("0").rstrip(".")] = {"effect": name}
        return self

    def event(self, t, text):
        self.timeline[f"{t:.4f}".rstrip("0").rstrip(".")] = text
        return self

    def json(self):
        d = {"loop": "hold_on_last_frame" if self.loop == "hold" else bool(self.loop), "animation_length": self.length,
             "bones": self.bones}
        if self.sounds:
            d["sound_effects"] = self.sounds
        if self.timeline:
            d["timeline"] = self.timeline
        return d


def save_anims(prefix, anims, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out = {"format_version": "1.8.0", "animations": {f"animation.{prefix}.{a.name}": a.json() for a in anims}}
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(out, fh, indent=1)


# ---------------------------------------------------------------------------------------------------------------- criaturas
# Modelo + textura juntos: cada cubo recibe su propio pedazo de textura (UV de caja) y una función que lo pinta.

import math
import random as _random


def _ceil(v):
    return int(math.ceil(v - 1e-6))


class Creature:
    """c = Creature("geometry.axelis_core.bestia", 128, 64); c.bone(...); c.part("body", origen, tamaño, pintar); c.save(geo, png)"""

    def __init__(self, ident, tw=128, th=128, seed=1):
        self.m = Model(ident, tw, th)
        self.tw, self.th = tw, th
        self.parts = []  # (cubo, tamaño, pintar)
        self.rnd = _random.Random(seed)

    def bone(self, name, pivot, parent=None, rotation=None):
        self.m.bone(name, pivot, parent, rotation)

    def part(self, bone, origin, size, paint, inflate=0, rotation=None, pivot=None):
        c = self.m.cube(bone, origin, size, [0, 0], inflate=inflate, rotation=rotation, pivot=pivot)
        self.parts.append((c, size, paint))
        return c

    def plane(self, bone, origin, w, h, paint, face="north"):
        """Un plano (para ojos, marcas): solo la cara de frente con su propio pedazo de textura."""
        c = self.m.cube(bone, origin, [w, h, 0], faces={})
        self.parts.append((c, [w, h, 0], paint, face))
        return c

    def glow(self, *bones):
        self.m.glow.extend(bones)

    def bend(self, bone, child, at):
        self.m.bend(bone, child, at)

    def _pack(self):
        x = y = rowh = 0
        out = []
        for p in self.parts:
            c, size = p[0], p[1]
            w, h, d = _ceil(size[0]), _ceil(size[1]), _ceil(size[2])
            if len(p) == 4:  # plano
                bw, bh = w, h
            else:
                bw, bh = 2 * (d + w), d + h
            if x + bw > self.tw:
                x = 0
                y += rowh
                rowh = 0
            if y + bh > self.th:
                raise ValueError(f"no cabe la textura ({self.tw}x{self.th}) en {self.m.ident}")
            out.append((x, y))
            x += bw
            rowh = max(rowh, bh)
        return out

    def build(self, geo_path, png_path):
        from PIL import Image
        img = Image.new("RGBA", (self.tw, self.th), (0, 0, 0, 0))
        for p, (u, v) in zip(self.parts, self._pack()):
            c, size, paint = p[0], p[1], p[2]
            w, h, d = _ceil(size[0]), _ceil(size[1]), _ceil(size[2])
            if len(p) == 4:
                c["uv"] = {p[3]: {"uv": [u, v], "uv_size": [size[0], size[1]]}}
                for yy in range(h):
                    for xx in range(w):
                        col = paint("front", xx, yy, w, h)
                        if col is not None:
                            img.putpixel((u + xx, v + yy), _rgba(col, self.rnd, 1.0))
                continue
            c["uv"] = [u, v]
            reg = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
                   "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}
            for face, (x0, y0, ww, hh) in reg.items():
                k = {"top": 1.12, "bottom": 0.7, "right": 0.88, "front": 1.0, "left": 0.88, "back": 0.82}[face]
                for yy in range(hh):
                    for xx in range(ww):
                        col = paint(face, xx, yy, ww, hh)
                        if col is not None:
                            img.putpixel((x0 + xx, y0 + yy), _rgba(col, self.rnd, k))
        self.m.save(geo_path)
        os.makedirs(os.path.dirname(png_path), exist_ok=True)
        img.save(png_path)


def _rgba(col, rnd, k):
    noise = 0
    a = 255
    if len(col) == 4:
        a = col[3]
    if len(col) >= 3 and not (len(col) == 4 and col[3] == 254):
        noise = rnd.randint(-5, 5)
    return tuple(max(0, min(255, int(col[i] * k) + noise)) for i in range(3)) + (a,)


def solid(c, top=None, bottom=None, front=None, back=None, side=None):
    """Un color por cara (con algo de variación)."""
    def f(face, x, y, w, h):
        return {"top": top, "bottom": bottom, "front": front, "back": back, "right": side, "left": side}.get(face) or c
    return f


def shade_v(c1, c2):
    """Degradado de arriba (c1) a abajo (c2) en los lados; arriba c1, abajo c2."""
    def f(face, x, y, w, h):
        if face == "top":
            return c1
        if face == "bottom":
            return c2
        t = y / max(1, h - 1)
        return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))
    return f
