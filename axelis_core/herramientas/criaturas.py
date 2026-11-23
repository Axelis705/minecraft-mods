#!/usr/bin/env python3
"""Las criaturas listas del Core: Bestia (cuatro patas), Alado (vuela), Gelatina (salta), Coloso (jefe grande) y Chispa (mascota).
Cada una: modelo (.geo.json), textura (pintada aquí mismo) y animaciones (.animation.json).

    python3 axelis_core/herramientas/criaturas.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo import Anim, Creature, save_anims  # noqa: E402

A = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "axelis_core")
S = "easeInOutSine"
C = "catmullrom"


def out(name):
    return os.path.join(A, "geo", name + ".geo.json"), os.path.join(A, "textures", "entity", name + ".png")


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def loop(keys, length):
    """Cierra un ciclo: la última clave igual a la primera."""
    keys = dict(keys)
    keys[length] = keys[0]
    return keys


def cyc(bone_vals, length, phase=0.0, ease=C):
    """Claves de un ciclo a partir de valores repartidos parejo (con desfase 0..1)."""
    n = len(bone_vals)
    out_ = {}
    for i, v in enumerate(bone_vals):
        t = ((i / n + phase) % 1.0) * length
        out_[round(t, 4)] = (v, ease)
    first = min(out_)
    if first > 0:
        # valor en 0 = interpolar entre la última y la primera (con catmull se aproxima repitiendo la más cercana)
        last = max(out_)
        out_[0.0] = out_[last] if (length - last) < first else out_[first]
    out_[length] = out_[0.0]
    return out_


# ================================================================================================================ BESTIA
def bestia():
    fur, dark, belly, mane, ember = (128, 124, 120), (74, 70, 70), (176, 170, 160), (52, 48, 52), (255, 136, 48)
    c = Creature("geometry.axelis_core.bestia", 128, 64, seed=11)

    def body(face, x, y, w, h):
        if face == "top":
            return dark if (w // 2 - 1 <= x <= w // 2) else fur  # raya oscura en el lomo
        if face == "bottom":
            return belly
        if y >= h - 2:
            return belly
        if face in ("right", "left") and (x + y * 3) % 7 == 0:
            return mix(fur, dark, 0.5)
        return fur

    def mane_p(face, x, y, w, h):
        if face == "bottom":
            return mane
        tip = (x * 5 + y * 3) % 9 == 0
        return ember if tip and face != "top" else mix(mane, dark, (y % 3) / 4)

    def head(face, x, y, w, h):
        if face == "top":
            return mix(fur, dark, 0.3)
        if face == "bottom":
            return belly
        if face == "front" and y >= h - 2:
            return belly
        return fur

    def snout(face, x, y, w, h):
        if face == "front" and y == 0 and w // 2 - 1 <= x <= w // 2:
            return (30, 26, 28)  # nariz
        if face == "top":
            return fur
        return belly if y >= 1 else fur

    def jaw(face, x, y, w, h):
        if face == "top":
            return (150, 52, 60) if 0 < x < w - 1 else (236, 232, 220)
        return belly

    def leg(face, x, y, w, h):
        if face == "bottom" or (y >= h - 2 and face != "top"):
            return dark
        return mix(fur, dark, 0.25 * (y / max(1, h)))

    def ear(face, x, y, w, h):
        return (40, 36, 38) if face in ("front",) and y > 0 else dark

    def tail(t):
        def f(face, x, y, w, h):
            base = mix(fur, (226, 220, 210), t)
            return mix(base, dark, 0.3) if face == "top" else base
        return f

    def eyes(face, x, y, w, h):
        return ember + (255,) if (x in (0, 1) or x in (w - 2, w - 1)) else None

    c.bone("root", [0, 0, 0])
    c.bone("body", [0, 13, 2], "root")
    c.part("body", [-4.5, 9, -6], [9, 8, 15], body)
    c.part("body", [-5, 9.5, -7.5], [10, 9, 6], mane_p)
    c.bone("neck", [0, 15, -6], "body")
    c.part("neck", [-2.5, 12, -9.5], [5, 5, 4], head)
    c.bone("head", [0, 16, -8.5], "neck")
    c.part("head", [-3.5, 13, -14.5], [7, 7, 6], head)
    c.part("head", [-2, 13, -18.5], [4, 3.5, 4], snout)
    c.bone("jaw", [0, 13.2, -14], "head")
    c.part("jaw", [-2, 12, -18], [4, 1.2, 4], jaw)
    c.bone("ear_r", [-2.2, 20, -11], "head")
    c.part("ear_r", [-3.4, 20, -11.5], [2, 3, 1.2], ear)
    c.bone("ear_l", [2.2, 20, -11], "head")
    c.part("ear_l", [1.4, 20, -11.5], [2, 3, 1.2], ear)
    for name, x, z in (("front_r", -2.6, -4), ("front_l", 2.6, -4), ("back_r", -2.6, 6), ("back_l", 2.6, 6)):
        c.bone(name, [x, 10, z], "body")
        c.part(name, [x - 1.5, 0, z - 1.5], [3, 10, 3], leg)
        c.bone(name + "_low", [x, 5, z], name)
        c.bend(name, name + "_low", 5)
    c.bone("tail1", [0, 15, 9], "body", [32, 0, 0])
    c.part("tail1", [-1.5, 13.5, 9], [3, 3, 4], tail(0.0))
    c.bone("tail2", [0, 15, 13], "tail1", [-12, 0, 0])
    c.part("tail2", [-1.4, 13.6, 13], [2.8, 2.8, 4], tail(0.2))
    c.bone("tail3", [0, 15, 17], "tail2", [-12, 0, 0])
    c.part("tail3", [-1.2, 13.8, 17], [2.4, 2.4, 3.5], tail(0.75))
    # los ojos van en su propio hueso para que brillen sin que brille la cabeza
    c.bone("eyes", [0, 17, -14.5], "head")
    c.plane("eyes", [-3, 17, -14.53], 6, 1, eyes)
    c.glow("eyes")
    c.build(*out("bestia"))

    an = []
    a = Anim("idle", 3.0, True)
    a.pos("body", {0: ([0, 0, 0], C), 1.5: ([0, 0.3, 0], C), 3.0: ([0, 0, 0], C)})
    a.rot("tail1", {0: ([0, -10, 0], C), 1.5: ([4, 10, 0], C), 3.0: ([0, -10, 0], C)})
    a.rot("tail2", {0: ([0, -6, 0], C), 1.5: ([0, 8, 0], C), 3.0: ([0, -6, 0], C)})
    a.rot("ear_r", {0: [0, 0, 0], 2.2: [0, 0, 0], 2.3: ([0, 0, -20], S), 2.45: ([0, 0, 0], S), 3.0: [0, 0, 0]})
    a.rot("head", {0: ([0, 0, 0], C), 1.0: ([4, 6, 0], C), 2.0: ([2, -5, 0], C), 3.0: ([0, 0, 0], C)})
    an.append(a)
    L = 1.0
    a = Anim("walk", L, True)
    up = [[-24, 0, 0], [0, 0, 0], [22, 0, 0], [0, 0, 0]]
    low = [[8, 0, 0], [4, 0, 0], [-8, 0, 0], [-34, 0, 0]]
    lowb = [[-8, 0, 0], [-4, 0, 0], [10, 0, 0], [34, 0, 0]]
    a.rot("front_r", cyc(up, L, 0.0)).rot("front_r_low", cyc(low, L, 0.0))
    a.rot("back_l", cyc(up, L, 0.0)).rot("back_l_low", cyc(lowb, L, 0.0))
    a.rot("front_l", cyc(up, L, 0.5)).rot("front_l_low", cyc(low, L, 0.5))
    a.rot("back_r", cyc(up, L, 0.5)).rot("back_r_low", cyc(lowb, L, 0.5))
    a.pos("body", cyc([[0, -0.2, 0], [0, 0.4, 0]], L / 2))
    a.rot("head", cyc([[2, -3, 0], [2, 3, 0]], L))
    a.rot("tail1", cyc([[0, -8, 0], [0, 8, 0]], L))
    an.append(a)
    L = 0.5
    a = Anim("run", L, True)
    a.rot("front_r", cyc([[-55, 0, 0], [-10, 0, 0], [40, 0, 0], [0, 0, 0]], L, 0.0))
    a.rot("front_l", cyc([[-55, 0, 0], [-10, 0, 0], [40, 0, 0], [0, 0, 0]], L, 0.08))
    a.rot("back_r", cyc([[45, 0, 0], [0, 0, 0], [-45, 0, 0], [-10, 0, 0]], L, 0.0))
    a.rot("back_l", cyc([[45, 0, 0], [0, 0, 0], [-45, 0, 0], [-10, 0, 0]], L, 0.08))
    a.rot("front_r_low", cyc([[10, 0, 0], [-50, 0, 0]], L)).rot("front_l_low", cyc([[10, 0, 0], [-50, 0, 0]], L, 0.08))
    a.rot("back_r_low", cyc([[0, 0, 0], [50, 0, 0]], L)).rot("back_l_low", cyc([[0, 0, 0], [50, 0, 0]], L, 0.08))
    a.rot("body", cyc([[-7, 0, 0], [6, 0, 0]], L))
    a.pos("body", cyc([[0, 1.2, 0], [0, -0.6, 0]], L))
    a.rot("head", cyc([[10, 0, 0], [-4, 0, 0]], L))
    a.rot("tail1", cyc([[-20, 0, 0], [-8, 0, 0]], L))
    a.rot("ear_r", [0, 0, 25]).rot("ear_l", [0, 0, -25])
    an.append(a)
    a = Anim("sit", 0.6, "hold")
    a.rot("body", {0: [0, 0, 0], 0.6: ([-28, 0, 0], S)})
    a.pos("body", {0: [0, 0, 0], 0.6: ([0, -3.5, 2], S)})
    a.rot("back_r", {0: [0, 0, 0], 0.6: ([-60, 0, 0], S)}).rot("back_l", {0: [0, 0, 0], 0.6: ([-60, 0, 0], S)})
    a.rot("back_r_low", {0: [0, 0, 0], 0.6: ([95, 0, 0], S)}).rot("back_l_low", {0: [0, 0, 0], 0.6: ([95, 0, 0], S)})
    a.rot("front_r", {0: [0, 0, 0], 0.6: ([28, 0, 0], S)}).rot("front_l", {0: [0, 0, 0], 0.6: ([28, 0, 0], S)})
    a.rot("head", {0: [0, 0, 0], 0.6: ([22, 0, 0], S)})
    a.rot("tail1", {0: [0, 0, 0], 0.6: ([-30, 0, 0], S)})
    an.append(a)
    a = Anim("attack", 0.6, False)
    a.pos("body", {0: [0, 0, 0], 0.18: ([0, -0.5, 2], S), 0.3: ([0, 0.5, -3.5], "easeOutQuad"), 0.6: ([0, 0, 0], S)})
    a.rot("head", {0: [0, 0, 0], 0.18: ([-18, 0, 0], S), 0.3: ([16, 0, 0], "easeOutQuad"), 0.6: ([0, 0, 0], S)})
    a.rot("jaw", {0: [0, 0, 0], 0.18: ([38, 0, 0], S), 0.3: ([0, 0, 0], "easeInQuad"), 0.42: ([10, 0, 0], S), 0.6: ([0, 0, 0], S)})
    a.rot("front_r", {0: [0, 0, 0], 0.18: ([10, 0, 0], S), 0.3: ([-30, 0, 0], S), 0.6: ([0, 0, 0], S)})
    a.rot("front_l", {0: [0, 0, 0], 0.18: ([10, 0, 0], S), 0.3: ([-30, 0, 0], S), 0.6: ([0, 0, 0], S)})
    a.event(0.3, "mordida")
    an.append(a)
    a = Anim("howl", 2.6, False)
    a.rot("body", {0: [0, 0, 0], 0.4: ([-16, 0, 0], S), 2.2: ([-16, 0, 0], S), 2.6: ([0, 0, 0], S)})
    a.rot("neck", {0: [0, 0, 0], 0.5: ([-30, 0, 0], S), 2.2: ([-34, 0, 0], S), 2.6: ([0, 0, 0], S)})
    a.rot("head", {0: [0, 0, 0], 0.5: ([-24, 0, 0], S), 2.2: ([-28, 0, 0], S), 2.6: ([0, 0, 0], S)})
    a.rot("jaw", {0: [0, 0, 0], 0.6: ([30, 0, 0], S), 1.4: ([22, 0, 0], S), 2.0: ([32, 0, 0], S), 2.3: ([0, 0, 0], S)})
    a.rot("ear_r", {0: [0, 0, 0], 0.5: ([30, 0, 0], S), 2.2: ([30, 0, 0], S), 2.6: [0, 0, 0]})
    a.rot("ear_l", {0: [0, 0, 0], 0.5: ([30, 0, 0], S), 2.2: ([30, 0, 0], S), 2.6: [0, 0, 0]})
    a.sound(0.6, "minecraft:entity.wolf.howl")
    an.append(a)
    a = Anim("sniff", 2.0, False)
    a.rot("neck", {0: [0, 0, 0], 0.4: ([34, 0, 0], S), 1.6: ([34, 0, 0], S), 2.0: ([0, 0, 0], S)})
    a.rot("head", {0: [0, 0, 0], 0.4: ([16, 0, 0], S), 0.6: ([12, 8, 0], S), 0.8: ([18, -6, 0], S), 1.0: ([12, 4, 0], S), 1.3: ([18, -8, 0], S),
                   1.6: ([16, 0, 0], S), 2.0: ([0, 0, 0], S)})
    an.append(a)
    a = Anim("happy", 1.2, False)
    a.rot("tail1", {0: ([10, -30, 0], S), 0.1: ([10, 30, 0], S), 0.2: ([10, -30, 0], S), 0.3: ([10, 30, 0], S), 0.4: ([10, -30, 0], S),
                    0.5: ([10, 30, 0], S), 0.6: ([10, -30, 0], S), 0.7: ([10, 30, 0], S), 0.8: ([10, -30, 0], S), 0.9: ([10, 30, 0], S),
                    1.2: ([0, 0, 0], S)})
    a.pos("body", {0: [0, 0, 0], 0.3: ([0, 1, 0], S), 0.6: ([0, 0, 0], S), 0.9: ([0, 1, 0], S), 1.2: ([0, 0, 0], S)})
    an.append(a)
    a = Anim("hurt", 0.4, False)
    a.rot("body", {0: [0, 0, 0], 0.08: ([6, 0, 4], "easeOutQuad"), 0.4: ([0, 0, 0], S)})
    a.rot("head", {0: [0, 0, 0], 0.08: ([-14, 0, 0], "easeOutQuad"), 0.4: ([0, 0, 0], S)})
    an.append(a)
    a = Anim("death", 1.0, "hold")
    a.rot("root", {0: [0, 0, 0], 0.7: ([0, 0, 88], "easeInQuad"), 0.85: ([0, 0, 80], S), 1.0: ([0, 0, 88], S)})
    a.pos("root", {0: [0, 0, 0], 1.0: ([3, 2, 0], S)})
    for leg_ in ("front_r", "front_l", "back_r", "back_l"):
        a.rot(leg_, {0: [0, 0, 0], 0.7: ([-14 if leg_.startswith("front") else 14, 0, 0], S)})
    a.rot("head", {0: [0, 0, 0], 0.9: ([10, 0, -10], S)})
    a.rot("tail1", {0: [0, 0, 0], 1.0: ([-30, 0, 0], S)})
    an.append(a)
    save_anims("bestia", an, os.path.join(A, "animations", "bestia.animation.json"))


# ================================================================================================================ ALADO
def alado():
    blue, dark, light, gold, storm = (58, 92, 158), (32, 46, 86), (196, 212, 236), (236, 186, 64), (120, 220, 255)
    c = Creature("geometry.axelis_core.alado", 128, 64, seed=21)

    def body(face, x, y, w, h):
        if face == "bottom":
            return light
        if face == "front":
            return light if y >= 2 else blue
        if face == "top":
            return dark if x == w // 2 else blue
        return mix(blue, light, max(0, (y - h * 0.5) / h))

    def head(face, x, y, w, h):
        if face == "front" and y == 1 and x in (0, w - 1):
            return (20, 20, 30)
        return blue if face != "bottom" else light

    def beak(face, x, y, w, h):
        return gold if face != "bottom" else mix(gold, (120, 80, 30), 0.5)

    def wing(tip):
        def f(face, x, y, w, h):
            if face in ("top", "bottom"):
                # plumas: rayas a lo largo
                band = (x // 2) % 2 == 0
                col = dark if band else blue
                if tip and x < 2:
                    col = storm
                if face == "bottom":
                    col = mix(col, light, 0.45)
                return col
            return dark
        return f

    def tail(face, x, y, w, h):
        if face in ("top", "bottom"):
            return storm if y >= h - 1 else (dark if x % 2 == 0 else blue)
        return dark

    def leg(face, x, y, w, h):
        return gold

    def crest(face, x, y, w, h):
        return storm

    c.bone("root", [0, 0, 0])
    c.bone("body", [0, 8, 0], "root")
    c.part("body", [-3, 5, -5], [6, 6, 10], body)
    c.bone("head", [0, 10, -4.5], "body")
    c.part("head", [-2.5, 9, -9.5], [5, 5, 5], head)
    c.part("head", [-1, 9.5, -12], [2, 2, 2.5], beak)
    c.part("head", [-0.5, 13.5, -8.5], [1, 2, 4], crest, rotation=[-25, 0, 0], pivot=[0, 13.5, -6])
    for side, s in (("r", -1), ("l", 1)):
        c.bone(f"wing_{side}", [3 * s, 10, -2], "body")
        x0 = -11 if s < 0 else 3
        c.part(f"wing_{side}", [x0, 9.5, -4.5], [8, 1, 7], wing(False))
        c.bone(f"wing_{side}_tip", [11 * s, 10, -2], f"wing_{side}")
        x1 = -19.5 if s < 0 else 10.5
        c.part(f"wing_{side}_tip", [x1, 9.7, -4], [9, 0.6, 6], wing(True))
        c.bone(f"leg_{side}", [1.5 * s, 5, 0], "body")
        c.part(f"leg_{side}", [1.5 * s - 0.5, 1, -0.5], [1, 4, 1], leg)
        c.part(f"leg_{side}", [1.5 * s - 1, 0, -2], [2, 1, 3], leg)
    c.bone("tail", [0, 8, 5], "body")
    c.part("tail", [-2.5, 7.5, 5], [5, 1, 6], tail)
    c.build(*out("alado"))

    an = []
    L = 0.5
    a = Anim("fly", L, True)
    a.rot("wing_r", cyc([[0, 0, 40], [0, 0, 5], [0, 0, -42], [0, 0, -8]], L)).rot("wing_l", cyc([[0, 0, -40], [0, 0, -5], [0, 0, 42], [0, 0, 8]], L))
    a.rot("wing_r_tip", cyc([[0, 0, -10], [0, 0, 20], [0, 0, 18], [0, 0, -16]], L)).rot("wing_l_tip", cyc([[0, 0, 10], [0, 0, -20], [0, 0, -18], [0, 0, 16]], L))
    a.pos("body", cyc([[0, -0.8, 0], [0, 0.8, 0]], L, 0.25))
    a.rot("tail", cyc([[-6, 0, 0], [4, 0, 0]], L))
    a.rot("leg_r", [60, 0, 0]).rot("leg_l", [60, 0, 0])
    a.rot("head", cyc([[4, 0, 0], [-2, 0, 0]], L, 0.25))
    an.append(a)
    a = Anim("glide", 2.0, True)
    a.rot("wing_r", cyc([[0, 0, 4], [0, 0, -4]], 2.0)).rot("wing_l", cyc([[0, 0, -4], [0, 0, 4]], 2.0))
    a.rot("wing_r_tip", cyc([[0, 0, 6], [0, 0, -2]], 2.0)).rot("wing_l_tip", cyc([[0, 0, -6], [0, 0, 2]], 2.0))
    a.rot("leg_r", [70, 0, 0]).rot("leg_l", [70, 0, 0])
    a.rot("body", cyc([[0, 0, -5], [0, 0, 5]], 2.0))
    an.append(a)
    a = Anim("idle", 3.0, True)  # posado: alas plegadas
    a.rot("body", [-18, 0, 0])
    a.rot("head", {0: ([18, 0, 0], C), 1.0: ([18, 25, 0], C), 2.0: ([14, -20, 0], C), 3.0: ([18, 0, 0], C)})
    a.rot("wing_r", [0, -78, 6]).rot("wing_l", [0, 78, -6])
    a.rot("wing_r_tip", [0, -95, 0]).rot("wing_l_tip", [0, 95, 0])
    a.rot("leg_r", [18, 0, 0]).rot("leg_l", [18, 0, 0])
    a.rot("tail", {0: ([22, 0, 0], C), 1.5: ([26, 0, 0], C), 3.0: ([22, 0, 0], C)})
    an.append(a)
    a = Anim("attack", 0.7, False)
    a.rot("body", {0: [0, 0, 0], 0.2: ([30, 0, 0], S), 0.4: ([-20, 0, 0], "easeOutQuad"), 0.7: ([0, 0, 0], S)})
    a.rot("wing_r", {0: [0, 0, 0], 0.2: ([0, -40, 30], S), 0.4: ([0, 0, -50], "easeOutQuad"), 0.7: [0, 0, 0]})
    a.rot("wing_l", {0: [0, 0, 0], 0.2: ([0, 40, -30], S), 0.4: ([0, 0, 50], "easeOutQuad"), 0.7: [0, 0, 0]})
    a.rot("leg_r", {0: [0, 0, 0], 0.3: ([-70, 0, 0], S), 0.45: ([-30, 0, 0], S), 0.7: [0, 0, 0]})
    a.rot("leg_l", {0: [0, 0, 0], 0.3: ([-70, 0, 0], S), 0.45: ([-30, 0, 0], S), 0.7: [0, 0, 0]})
    a.event(0.4, "garras")
    an.append(a)
    a = Anim("hurt", 0.35, False)
    a.rot("body", {0: [0, 0, 0], 0.08: ([-10, 0, 14], "easeOutQuad"), 0.35: [0, 0, 0]})
    an.append(a)
    a = Anim("death", 1.0, "hold")
    a.rot("root", {0: [0, 0, 0], 0.8: ([0, 0, 170], "easeInQuad")})
    a.rot("wing_r", {0: [0, 0, 0], 0.8: ([0, 0, 50], S)}).rot("wing_l", {0: [0, 0, 0], 0.8: ([0, 0, -50], S)})
    a.pos("root", {0: [0, 0, 0], 0.8: ([0, 8, 0], S)})
    an.append(a)
    save_anims("alado", an, os.path.join(A, "animations", "alado.animation.json"))


# ================================================================================================================ GELATINA
def gelatina():
    g1, g2, g3, leaf = (102, 214, 140), (64, 168, 104), (180, 250, 200), (70, 150, 60)
    c = Creature("geometry.axelis_core.gelatina", 64, 64, seed=31)

    def body(face, x, y, w, h):
        if face == "top":
            return g3 if (x + y) % 5 == 0 else g1
        if face == "bottom":
            return g2
        if face == "front":
            if y == 4 and x in (2, 3, 8, 9):
                return (24, 40, 30)  # ojos
            if y == 3 and x in (2, 9):
                return (250, 255, 250)  # brillo
            if y == 7 and 4 <= x <= 7:
                return (40, 80, 50)  # boca
        if x == 1 and y < 4:
            return g3  # reflejo
        return mix(g1, g2, y / max(1, h - 1))

    def sprout(face, x, y, w, h):
        return leaf

    c.bone("root", [0, 0, 0])
    c.bone("body", [0, 0, 0], "root")
    c.part("body", [-6, 0, -6], [12, 11, 12], body)
    c.bone("sprout", [0, 11, 0], "body")
    c.part("sprout", [-0.5, 11, -0.5], [1, 3, 1], sprout)
    c.part("sprout", [0.5, 13, -1], [3, 0.5, 2], sprout, rotation=[0, 0, -20], pivot=[0.5, 13, 0])
    c.part("sprout", [-3.5, 13, -1], [3, 0.5, 2], sprout, rotation=[0, 0, 20], pivot=[-0.5, 13, 0])
    c.build(*out("gelatina"))
    an = []
    a = Anim("idle", 2.0, True)
    a.scale("body", cyc([[1.0, 1.0, 1.0], [1.04, 0.95, 1.04], [1.0, 1.0, 1.0], [0.97, 1.03, 0.97]], 2.0))
    a.rot("sprout", cyc([[0, 0, -6], [0, 0, 6]], 2.0))
    an.append(a)
    a = Anim("hop", 0.8, True)
    a.scale("body", {0: ([1.2, 0.75, 1.2], S), 0.15: ([0.85, 1.25, 0.85], "easeOutQuad"), 0.45: ([0.95, 1.05, 0.95], S),
                     0.62: ([1.25, 0.72, 1.25], "easeOutQuad"), 0.8: ([1.2, 0.75, 1.2], S)})
    a.pos("root", {0: [0, 0, 0], 0.15: ([0, 2, 0], S), 0.32: ([0, 7, 0], "easeOutQuad"), 0.55: ([0, 0, 0], "easeInQuad"), 0.8: [0, 0, 0]})
    a.rot("sprout", {0: ([0, 0, 0], S), 0.2: ([-25, 0, 0], S), 0.55: ([20, 0, 0], S), 0.8: ([0, 0, 0], S)})
    an.append(a)
    a = Anim("attack", 0.6, False)
    a.scale("body", {0: [1, 1, 1], 0.2: ([1.25, 0.75, 1.25], S), 0.35: ([0.8, 1.15, 1.3], "easeOutQuad"), 0.6: [1, 1, 1]})
    a.pos("root", {0: [0, 0, 0], 0.35: ([0, 3, -5], "easeOutQuad"), 0.6: [0, 0, 0]})
    a.event(0.35, "golpe")
    an.append(a)
    a = Anim("hurt", 0.35, False)
    a.scale("body", {0: [1, 1, 1], 0.08: ([1.2, 0.8, 1.2], "easeOutQuad"), 0.35: [1, 1, 1]})
    an.append(a)
    a = Anim("death", 0.9, "hold")
    a.scale("body", {0: [1, 1, 1], 0.6: ([1.6, 0.18, 1.6], "easeInQuad"), 0.9: ([1.5, 0.12, 1.5], S)})
    a.pos("sprout", {0: [0, 0, 0], 0.6: ([0, -8, 0], S)})
    an.append(a)
    save_anims("gelatina", an, os.path.join(A, "animations", "gelatina.animation.json"))


# ================================================================================================================ COLOSO (jefe)
def coloso():
    st, st2, moss, cry, cry2 = (104, 102, 112), (72, 70, 82), (78, 112, 62), (120, 230, 255), (190, 120, 255)
    c = Creature("geometry.axelis_core.coloso", 256, 128, seed=41)

    def stone(face, x, y, w, h):
        if face == "top" and (x * 7 + y * 3) % 11 < 3:
            return moss
        crack = (x * 13 + y * 7) % 23 == 0 or (x + 2 * y) % 17 == 0
        if crack:
            return st2
        return mix(st, st2, ((x // 3 + y // 4) % 3) / 5)

    def plate(face, x, y, w, h):
        edge = x in (0, w - 1) or y in (0, h - 1)
        return st2 if edge else mix(st, (140, 138, 150), 0.3)

    def crystal(face, x, y, w, h):
        return (mix(cry, cry2, (x + y) / max(1, w + h)) + (255,))

    def eyes(face, x, y, w, h):
        return (cry + (255,)) if x in (0, 1, w - 2, w - 1) else None

    def core(face, x, y, w, h):
        d = abs(x - (w - 1) / 2) + abs(y - (h - 1) / 2)
        return (mix(cry2, cry, d / 3) + (255,)) if d <= 3 else None

    c.bone("root", [0, 0, 0])
    c.bone("hips", [0, 20, 0], "root")
    c.part("hips", [-7, 16, -4], [14, 6, 8], stone)
    c.bone("torso", [0, 22, 0], "hips")
    c.part("torso", [-10, 22, -6], [20, 14, 12], stone)
    c.part("torso", [-8, 25, -7], [16, 9, 1], plate)
    c.bone("core", [0, 29, -7.1], "torso")
    c.plane("core", [-3.5, 26, -7.13], 7, 7, core)
    for side, s in (("r", -1), ("l", 1)):
        c.bone(f"crystal_{side}", [7 * s, 36, 2], "torso")
        c.part(f"crystal_{side}", [7 * s - 1.5, 36, 0.5], [3, 6, 3], crystal, rotation=[-12, 0, 15 * -s], pivot=[7 * s, 36, 2])
        c.part(f"crystal_{side}", [3 * s - 1, 36, 2.5], [2, 4, 2], crystal, rotation=[-20, 0, 8 * -s], pivot=[3 * s, 36, 3])
    c.bone("head", [0, 36, -2], "torso")
    c.part("head", [-4, 35, -9], [8, 8, 8], stone)
    c.bone("eyes", [0, 39, -9.05], "head")
    c.plane("eyes", [-3, 38.5, -9.08], 6, 1, eyes)
    for side, s in (("r", -1), ("l", 1)):
        arm = f"arm_{side}"
        x0 = -17 if s < 0 else 10
        c.bone(arm, [13.5 * s, 34, 0], "torso")
        c.part(arm, [x0, 22, -4], [7, 14, 8], stone)
        c.bone(arm + "_low", [13.5 * s, 29, 0], arm)
        c.bend(arm, arm + "_low", 29)
        c.part(arm + "_low", [x0 - 1, 13, -5], [9, 9, 10], plate)
        leg = f"leg_{side}"
        lx = -8.5 if s < 0 else 0.5
        c.bone(leg, [4 * s, 17, 0], "hips")
        c.part(leg, [lx, 0, -4.5], [8, 17, 9], stone)
        c.bone(leg + "_low", [4 * s, 8.5, 0], leg)
        c.bend(leg, leg + "_low", 8.5)
    c.glow("core", "eyes", "crystal_r", "crystal_l")
    c.build(*out("coloso"))

    an = []
    a = Anim("idle", 4.0, True)
    a.rot("torso", cyc([[0, 0, 0], [3, 0, 0]], 4.0))
    a.pos("torso", cyc([[0, 0, 0], [0, 0.5, 0]], 4.0))
    a.rot("arm_r", cyc([[0, 0, 4], [-3, 0, 6]], 4.0)).rot("arm_l", cyc([[0, 0, -4], [-3, 0, -6]], 4.0))
    a.rot("arm_r_low", [-10, 0, 0]).rot("arm_l_low", [-10, 0, 0])
    a.rot("head", cyc([[0, 0, 0], [-3, 6, 0], [0, 0, 0], [-2, -6, 0]], 4.0))
    a.scale("core", cyc([[1, 1, 1], [1.15, 1.15, 1]], 4.0))
    an.append(a)
    L = 1.6
    a = Anim("walk", L, True)
    a.rot("leg_r", cyc([[-24, 0, 0], [0, 0, 0], [20, 0, 0], [0, 0, 0]], L)).rot("leg_l", cyc([[-24, 0, 0], [0, 0, 0], [20, 0, 0], [0, 0, 0]], L, 0.5))
    a.rot("leg_r_low", cyc([[6, 0, 0], [6, 0, 0], [14, 0, 0], [40, 0, 0]], L)).rot("leg_l_low", cyc([[6, 0, 0], [6, 0, 0], [14, 0, 0], [40, 0, 0]], L, 0.5))
    a.rot("arm_r", cyc([[18, 0, 6], [-18, 0, 6]], L)).rot("arm_l", cyc([[-18, 0, -6], [18, 0, -6]], L))
    a.rot("arm_r_low", cyc([[-10, 0, 0], [-26, 0, 0]], L)).rot("arm_l_low", cyc([[-26, 0, 0], [-10, 0, 0]], L))
    a.rot("hips", cyc([[0, 5, 3], [0, -5, -3]], L))
    a.rot("torso", cyc([[4, -7, -2], [4, 7, 2]], L))
    a.pos("hips", cyc([[0, -1, 0], [0, 0.6, 0], [0, -1, 0], [0, 0.6, 0]], L))
    a.sound(0.02, "minecraft:entity.iron_golem.step").sound(L / 2, "minecraft:entity.iron_golem.step")
    an.append(a)
    a = Anim("slam", 2.0, False)
    a.rot("arm_r", {0: [0, 0, 0], 0.7: ([-170, 0, -12], "easeOutQuad"), 0.95: ([-175, 0, -14], S), 1.15: ([-35, 0, -6], "easeInQuart"),
                    1.6: ([-35, 0, -6], S), 2.0: [0, 0, 0]})
    a.rot("arm_l", {0: [0, 0, 0], 0.7: ([-170, 0, 12], "easeOutQuad"), 0.95: ([-175, 0, 14], S), 1.15: ([-35, 0, 6], "easeInQuart"),
                    1.6: ([-35, 0, 6], S), 2.0: [0, 0, 0]})
    a.rot("arm_r_low", {0: [0, 0, 0], 0.7: ([-40, 0, 0], S), 1.15: ([-5, 0, 0], S), 2.0: [0, 0, 0]})
    a.rot("arm_l_low", {0: [0, 0, 0], 0.7: ([-40, 0, 0], S), 1.15: ([-5, 0, 0], S), 2.0: [0, 0, 0]})
    a.rot("torso", {0: [0, 0, 0], 0.7: ([-16, 0, 0], "easeOutQuad"), 0.95: ([-18, 0, 0], S), 1.15: ([30, 0, 0], "easeInQuart"),
                    1.6: ([28, 0, 0], S), 2.0: [0, 0, 0]})
    a.rot("head", {0: [0, 0, 0], 0.7: ([-14, 0, 0], S), 1.15: ([10, 0, 0], S), 2.0: [0, 0, 0]})
    for leg_ in ("leg_r", "leg_l"):
        a.rot(leg_, {0: [0, 0, 0], 1.15: ([-14, 0, 0], S), 1.6: ([-14, 0, 0], S), 2.0: [0, 0, 0]})
        a.rot(leg_ + "_low", {0: [0, 0, 0], 1.15: ([26, 0, 0], S), 1.6: ([26, 0, 0], S), 2.0: [0, 0, 0]})
    a.pos("hips", {0: [0, 0, 0], 1.15: ([0, -3, 0], S), 1.6: ([0, -3, 0], S), 2.0: [0, 0, 0]})
    a.event(1.15, "impacto").sound(1.15, "minecraft:entity.generic.explode")
    an.append(a)
    a = Anim("sweep", 1.4, False)
    a.rot("arm_r", {0: [0, 0, 0], 0.45: ([-80, 70, 0], "easeOutQuad"), 0.75: ([-80, -60, 0], "easeInQuart"), 1.0: ([-70, -70, 0], S), 1.4: [0, 0, 0]})
    a.rot("torso", {0: [0, 0, 0], 0.45: ([0, 30, 0], S), 0.75: ([4, -34, 0], "easeInQuart"), 1.0: ([4, -38, 0], S), 1.4: [0, 0, 0]})
    a.rot("arm_r_low", {0: [0, 0, 0], 0.45: ([-30, 0, 0], S), 0.75: ([-4, 0, 0], S), 1.4: [0, 0, 0]})
    a.event(0.75, "barrido")
    an.append(a)
    a = Anim("roar", 2.2, False)
    a.rot("torso", {0: [0, 0, 0], 0.4: ([-16, 0, 0], S), 1.8: ([-18, 0, 0], S), 2.2: [0, 0, 0]})
    a.rot("head", {0: [0, 0, 0], 0.4: ([-26, 0, 0], S), 0.6: ([-22, 0, 3], S), 0.8: ([-26, 0, -3], S), 1.0: ([-22, 0, 3], S),
                   1.2: ([-26, 0, -3], S), 1.8: ([-24, 0, 0], S), 2.2: [0, 0, 0]})
    a.rot("arm_r", {0: [0, 0, 0], 0.4: ([-20, 0, 72], "easeOutBack"), 1.8: ([-18, 0, 70], S), 2.2: [0, 0, 0]})
    a.rot("arm_l", {0: [0, 0, 0], 0.4: ([-20, 0, -72], "easeOutBack"), 1.8: ([-18, 0, -70], S), 2.2: [0, 0, 0]})
    a.rot("arm_r_low", {0: [0, 0, 0], 0.4: ([-50, 0, 0], S), 1.8: ([-50, 0, 0], S), 2.2: [0, 0, 0]})
    a.rot("arm_l_low", {0: [0, 0, 0], 0.4: ([-50, 0, 0], S), 1.8: ([-50, 0, 0], S), 2.2: [0, 0, 0]})
    a.scale("core", {0: [1, 1, 1], 0.4: ([1.5, 1.5, 1], S), 1.8: ([1.5, 1.5, 1], S), 2.2: [1, 1, 1]})
    a.event(0.4, "rugido").sound(0.4, "minecraft:entity.ravager.roar")
    an.append(a)
    a = Anim("stomp", 1.1, False)
    a.rot("leg_r", {0: [0, 0, 0], 0.4: ([-60, 0, 0], "easeOutQuad"), 0.6: ([-8, 0, 0], "easeInQuart"), 1.1: [0, 0, 0]})
    a.rot("leg_r_low", {0: [0, 0, 0], 0.4: ([60, 0, 0], S), 0.6: ([6, 0, 0], S), 1.1: [0, 0, 0]})
    a.rot("torso", {0: [0, 0, 0], 0.4: ([-6, 0, 6], S), 0.6: ([10, 0, -3], S), 1.1: [0, 0, 0]})
    a.event(0.6, "pisoton").sound(0.6, "minecraft:entity.generic.explode")
    an.append(a)
    a = Anim("hurt", 0.45, False)
    a.rot("torso", {0: [0, 0, 0], 0.1: ([-8, 0, 4], "easeOutQuad"), 0.45: [0, 0, 0]})
    a.rot("head", {0: [0, 0, 0], 0.1: ([-10, 0, 0], "easeOutQuad"), 0.45: [0, 0, 0]})
    an.append(a)
    a = Anim("death", 3.0, "hold")
    for leg_ in ("leg_r", "leg_l"):
        a.rot(leg_, {0: [0, 0, 0], 1.0: ([-70, 0, 0], S), 3.0: ([-80, 0, 0], S)})
        a.rot(leg_ + "_low", {0: [0, 0, 0], 1.0: ([110, 0, 0], S), 3.0: ([100, 0, 0], S)})
    a.pos("hips", {0: [0, 0, 0], 1.0: ([0, -9, -2], S), 2.4: ([0, -9, -2], S), 3.0: ([0, -12, -6], "easeInQuad")})
    a.rot("torso", {0: [0, 0, 0], 1.0: ([16, 0, 0], S), 2.0: ([24, 0, 0], S), 3.0: ([80, 0, 0], "easeInQuad")})
    a.rot("head", {0: [0, 0, 0], 1.0: ([20, 0, 0], S), 3.0: ([10, 0, 8], S)})
    a.rot("arm_r", {0: [0, 0, 0], 1.0: ([-10, 0, 10], S), 3.0: ([-60, 0, 30], S)})
    a.rot("arm_l", {0: [0, 0, 0], 1.0: ([-10, 0, -10], S), 3.0: ([-60, 0, -30], S)})
    a.scale("core", {0: [1, 1, 1], 1.0: ([1.6, 1.6, 1], S), 3.0: ([0.1, 0.1, 1], S)})
    an.append(a)
    a = Anim("phase", 2.6, False)
    for leg_ in ("leg_r", "leg_l"):
        a.rot(leg_, {0: [0, 0, 0], 0.6: ([-55, 0, 0], S), 1.6: ([-55, 0, 0], S), 2.6: [0, 0, 0]})
        a.rot(leg_ + "_low", {0: [0, 0, 0], 0.6: ([90, 0, 0], S), 1.6: ([90, 0, 0], S), 2.6: [0, 0, 0]})
    a.pos("hips", {0: [0, 0, 0], 0.6: ([0, -7, 0], S), 1.6: ([0, -7, 0], S), 2.6: [0, 0, 0]})
    a.rot("torso", {0: [0, 0, 0], 0.6: ([30, 0, 0], S), 1.6: ([-25, 0, 0], "easeOutBack"), 2.6: [0, 0, 0]})
    a.rot("arm_r", {0: [0, 0, 0], 0.6: ([20, 0, 10], S), 1.6: ([-160, 0, 20], "easeOutBack"), 2.2: ([-150, 0, 20], S), 2.6: [0, 0, 0]})
    a.rot("arm_l", {0: [0, 0, 0], 0.6: ([20, 0, -10], S), 1.6: ([-160, 0, -20], "easeOutBack"), 2.2: ([-150, 0, -20], S), 2.6: [0, 0, 0]})
    a.scale("crystal_r", {0: [1, 1, 1], 1.6: ([1.4, 1.6, 1.4], "easeOutBack"), 2.6: ([1.2, 1.3, 1.2], S)})
    a.scale("crystal_l", {0: [1, 1, 1], 1.6: ([1.4, 1.6, 1.4], "easeOutBack"), 2.6: ([1.2, 1.3, 1.2], S)})
    a.event(1.6, "fase")
    an.append(a)
    save_anims("coloso", an, os.path.join(A, "animations", "coloso.animation.json"))


# ================================================================================================================ CHISPA (mascota)
def chispa():
    orange, cream, dark, pink = (236, 132, 52), (252, 236, 210), (90, 50, 30), (246, 150, 160)
    c = Creature("geometry.axelis_core.chispa", 64, 64, seed=51)

    def body(face, x, y, w, h):
        if face == "bottom":
            return cream
        if face == "top":
            return mix(orange, dark, 0.15) if x % 3 == 0 else orange
        return cream if y >= h - 1 else orange

    def head(face, x, y, w, h):
        if face == "front":
            if y == 1 and x in (1, w - 2):
                return (30, 24, 30)  # ojos grandes
            if y == 0 and x in (1, w - 2):
                return (250, 250, 250)  # brillo en los ojos
            if y >= 3 and 1 <= x <= w - 2:
                return cream
        if face == "bottom":
            return cream
        return orange

    def snout(face, x, y, w, h):
        if face == "front" and y == 0 and x == w // 2:
            return (40, 26, 30)
        return cream

    def ear(face, x, y, w, h):
        if face == "front" and 0 < x < w - 1 and y > 0:
            return pink
        return dark if y == 0 else orange

    def leg(face, x, y, w, h):
        return dark if y >= h - 1 else orange

    def tail(tip):
        def f(face, x, y, w, h):
            if tip and (face == "back" or y >= h - 2 or face == "top" and y >= h // 2):
                return cream
            return orange
        return f

    c.bone("root", [0, 0, 0])
    c.bone("body", [0, 5, 0], "root")
    c.part("body", [-2.5, 3, -4], [5, 4, 8], body)
    c.bone("head", [0, 7, -3.5], "body")
    c.part("head", [-3, 5.5, -8.5], [6, 5, 5], head)
    c.part("head", [-1.5, 5.5, -10], [3, 2, 1.5], snout)
    for side, s in (("r", -1), ("l", 1)):
        c.bone(f"ear_{side}", [1.8 * s, 10.5, -6.5], "head")
        c.part(f"ear_{side}", [1.8 * s - 1.25, 10.5, -7], [2.5, 4, 1], ear)
    for name, x, z in (("front_r", -1.5, -2.5), ("front_l", 1.5, -2.5), ("back_r", -1.5, 2.5), ("back_l", 1.5, 2.5)):
        c.bone(name, [x, 3, z], "body")
        c.part(name, [x - 1, 0, z - 1], [2, 3, 2], leg)
    c.bone("tail1", [0, 6, 4], "body", [35, 0, 0])
    c.part("tail1", [-1.5, 4.5, 4], [3, 3, 4], tail(False))
    c.bone("tail2", [0, 6, 8], "tail1", [-25, 0, 0])
    c.part("tail2", [-2, 4, 8], [4, 4, 5], tail(True))
    c.build(*out("chispa"))

    an = []
    a = Anim("idle", 3.0, True)
    a.rot("tail1", cyc([[0, -12, 0], [4, 12, 0]], 3.0)).rot("tail2", cyc([[0, -10, 0], [0, 10, 0]], 3.0, 0.15))
    a.rot("ear_r", {0: [0, 0, 0], 1.9: [0, 0, 0], 2.0: ([0, 0, -25], S), 2.15: ([0, 0, 0], S), 3.0: [0, 0, 0]})
    a.rot("ear_l", {0: [0, 0, 0], 0.7: [0, 0, 0], 0.8: ([0, 0, 25], S), 0.95: ([0, 0, 0], S), 3.0: [0, 0, 0]})
    a.scale("body", cyc([[1, 1, 1], [1.04, 1.04, 1]], 3.0))
    a.rot("head", cyc([[0, 0, 0], [3, 8, 4], [0, 0, 0], [3, -8, -4]], 3.0))
    an.append(a)
    L = 0.6
    a = Anim("walk", L, True)
    a.rot("front_r", cyc([[-30, 0, 0], [30, 0, 0]], L)).rot("back_l", cyc([[-30, 0, 0], [30, 0, 0]], L))
    a.rot("front_l", cyc([[30, 0, 0], [-30, 0, 0]], L)).rot("back_r", cyc([[30, 0, 0], [-30, 0, 0]], L))
    a.pos("body", cyc([[0, 0, 0], [0, 0.4, 0]], L / 2))
    a.rot("tail1", cyc([[0, -10, 0], [0, 10, 0]], L))
    an.append(a)
    L = 0.4
    a = Anim("run", L, True)
    a.rot("front_r", cyc([[-60, 0, 0], [40, 0, 0]], L)).rot("front_l", cyc([[-60, 0, 0], [40, 0, 0]], L, 0.1))
    a.rot("back_r", cyc([[50, 0, 0], [-50, 0, 0]], L)).rot("back_l", cyc([[50, 0, 0], [-50, 0, 0]], L, 0.1))
    a.rot("body", cyc([[-8, 0, 0], [8, 0, 0]], L))
    a.pos("body", cyc([[0, 1.5, 0], [0, -0.3, 0]], L))
    a.rot("tail1", [-20, 0, 0])
    a.rot("ear_r", [-30, 0, 0]).rot("ear_l", [-30, 0, 0])
    an.append(a)
    a = Anim("sit", 0.5, "hold")
    a.rot("body", {0: [0, 0, 0], 0.5: ([-32, 0, 0], S)})
    a.pos("body", {0: [0, 0, 0], 0.5: ([0, -1, 1.5], S)})
    a.rot("back_r", {0: [0, 0, 0], 0.5: ([-58, 0, 0], S)}).rot("back_l", {0: [0, 0, 0], 0.5: ([-58, 0, 0], S)})
    a.rot("front_r", {0: [0, 0, 0], 0.5: ([32, 0, 0], S)}).rot("front_l", {0: [0, 0, 0], 0.5: ([32, 0, 0], S)})
    a.rot("head", {0: [0, 0, 0], 0.5: ([30, 0, 0], S)})
    a.rot("tail1", {0: [0, 0, 0], 0.5: ([-40, 0, 0], S)})
    an.append(a)
    a = Anim("sleep", 1.0, "hold")
    a.pos("body", {0: [0, 0, 0], 1.0: ([0, -2.6, 0], S)})
    for leg_ in ("front_r", "front_l"):
        a.rot(leg_, {0: [0, 0, 0], 1.0: ([-85, 0, 0], S)})
    for leg_ in ("back_r", "back_l"):
        a.rot(leg_, {0: [0, 0, 0], 1.0: ([85, 0, 0], S)})
    a.rot("head", {0: [0, 0, 0], 1.0: ([18, 0, 10], S)})
    a.rot("tail1", {0: [0, 0, 0], 1.0: ([-30, 70, 0], S)}).rot("tail2", {0: [0, 0, 0], 1.0: ([0, 60, 0], S)})
    a.rot("ear_r", {0: [0, 0, 0], 1.0: ([40, 0, -20], S)}).rot("ear_l", {0: [0, 0, 0], 1.0: ([40, 0, 20], S)})
    an.append(a)
    a = Anim("happy", 1.0, False)
    a.pos("root", {0: [0, 0, 0], 0.15: ([0, -0.8, 0], S), 0.45: ([0, 6, 0], "easeOutQuad"), 0.75: ([0, 0, 0], "easeInQuad"), 1.0: [0, 0, 0]})
    a.rot("root", {0: [0, 0, 0], 0.15: [0, 0, 0], 0.75: ([0, 360, 0], S), 1.0: ([0, 360, 0], S)})
    a.rot("ear_r", {0: [0, 0, 0], 0.45: ([-20, 0, -20], S), 1.0: [0, 0, 0]}).rot("ear_l", {0: [0, 0, 0], 0.45: ([-20, 0, 20], S), 1.0: [0, 0, 0]})
    a.rot("tail1", {0: [0, 0, 0], 0.45: ([30, 0, 0], S), 1.0: [0, 0, 0]})
    an.append(a)
    a = Anim("beg", 1.4, False)
    a.rot("body", {0: [0, 0, 0], 0.4: ([-62, 0, 0], "easeOutBack"), 1.1: ([-60, 0, 0], S), 1.4: [0, 0, 0]})
    a.pos("body", {0: [0, 0, 0], 0.4: ([0, 1, 2], S), 1.1: ([0, 1, 2], S), 1.4: [0, 0, 0]})
    a.rot("back_r", {0: [0, 0, 0], 0.4: ([-60, 0, 0], S), 1.1: ([-60, 0, 0], S), 1.4: [0, 0, 0]})
    a.rot("back_l", {0: [0, 0, 0], 0.4: ([-60, 0, 0], S), 1.1: ([-60, 0, 0], S), 1.4: [0, 0, 0]})
    a.rot("front_r", {0: [0, 0, 0], 0.4: ([-10, 0, 0], S), 0.6: ([-40, 0, 0], S), 0.8: ([-10, 0, 0], S), 1.0: ([-40, 0, 0], S), 1.4: [0, 0, 0]})
    a.rot("front_l", {0: [0, 0, 0], 0.4: ([-10, 0, 0], S), 0.7: ([-40, 0, 0], S), 0.9: ([-10, 0, 0], S), 1.1: ([-40, 0, 0], S), 1.4: [0, 0, 0]})
    a.rot("head", {0: [0, 0, 0], 0.4: ([50, 0, 12], S), 1.1: ([50, 0, -12], S), 1.4: [0, 0, 0]})
    an.append(a)
    a = Anim("play", 1.2, False)
    a.rot("body", {0: [0, 0, 0], 0.35: ([10, 0, 0], S), 0.6: ([10, 0, 0], S), 0.8: ([-20, 0, 0], "easeOutQuad"), 1.2: [0, 0, 0]})
    a.pos("body", {0: [0, 0, 0], 0.35: ([0, -1.5, 1], S), 0.6: ([0, -1.5, 1], S), 0.85: ([0, 4, -5], "easeOutQuad"), 1.05: ([0, 0, -6], "easeInQuad"),
                   1.2: [0, 0, 0]})
    a.rot("tail1", {0: [0, 0, 0], 0.35: ([20, -20, 0], S), 0.45: ([20, 20, 0], S), 0.55: ([20, -20, 0], S), 0.65: ([20, 20, 0], S), 1.2: [0, 0, 0]})
    a.rot("front_r", {0: [0, 0, 0], 0.85: ([-70, 0, 0], S), 1.05: ([-20, 0, 0], S), 1.2: [0, 0, 0]})
    a.rot("front_l", {0: [0, 0, 0], 0.85: ([-70, 0, 0], S), 1.05: ([-20, 0, 0], S), 1.2: [0, 0, 0]})
    an.append(a)
    a = Anim("hurt", 0.35, False)
    a.rot("body", {0: [0, 0, 0], 0.08: ([6, 0, 8], "easeOutQuad"), 0.35: [0, 0, 0]})
    a.rot("ear_r", {0: [0, 0, 0], 0.08: ([40, 0, 0], S), 0.35: [0, 0, 0]}).rot("ear_l", {0: [0, 0, 0], 0.08: ([40, 0, 0], S), 0.35: [0, 0, 0]})
    an.append(a)
    a = Anim("death", 0.8, "hold")
    a.rot("root", {0: [0, 0, 0], 0.6: ([0, 0, 90], "easeInQuad")})
    a.pos("root", {0: [0, 0, 0], 0.6: ([2, 1.5, 0], S)})
    an.append(a)
    save_anims("chispa", an, os.path.join(A, "animations", "chispa.animation.json"))


if __name__ == "__main__":
    bestia()
    alado()
    gelatina()
    coloso()
    chispa()
    print("criaturas listas: bestia, alado, gelatina, coloso, chispa")
