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
