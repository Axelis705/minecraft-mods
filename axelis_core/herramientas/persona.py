#!/usr/bin/env python3
"""Persona: el humano "Minecraft mejorado" del Core (modelo y animaciones).

Mismas medidas y UV que el jugador (sirve cualquier piel de 64x64), más:
- codos, rodillas y cintura que se doblan sin huecos (axelis_bends);
- cara con expresiones: dos planos delante de la cara que usan casillas que la piel normal no usa:
  (0,0) ojos cerrados, (24,0) boca abierta, (32,0) ojos felices, (56,0) boca sonriente/sorpresa.
  Si la piel no las pinta, no se ve nada (son transparentes).
- huesos "right_item" y "left_item" donde Minecraft pondría el objeto en la mano.

    python3 axelis_core/herramientas/persona.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo import Anim, Model, save_anims  # noqa: E402

A = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "axelis_core")


def model():
    m = Model("geometry.axelis_core.persona")
    m.bone("root", [0, 0, 0])
    m.bone("body", [0, 12, 0], "root")
    m.cube("body", [-4, 12, -2], [8, 12, 4], [16, 16])
    m.cube("body", [-4, 12, -2], [8, 12, 4], [16, 32], inflate=0.25)
    m.bone("chest", [0, 18, 0], "body")
    m.bend("body", "chest", 18)
    m.bone("head", [0, 24, 0], "chest")
    m.cube("head", [-4, 24, -4], [8, 8, 8], [0, 0])
    m.cube("head", [-4, 24, -4], [8, 8, 8], [32, 0], inflate=0.5)
    # expresiones (planos delante de la cara, entre la cara y el sombrero)
    for name, cell, z in (("face_eyes", (0, 0), -4.03), ("face_mouth", (24, 0), -4.03), ("face_happy", (32, 0), -4.05),
                          ("face_smile", (56, 0), -4.05)):
        m.bone(name, [0, 24, 0], "head")
        m.cube(name, [-4, 24, z], [8, 8, 0], faces={"north": {"uv": list(cell), "uv_size": [8, 8]}})
    for side, s in (("right", -1), ("left", 1)):
        arm = f"{side}_arm"
        fore = f"{side}_forearm"
        m.bone(arm, [5 * s, 22, 0], "chest")
        x0 = -8 if s < 0 else 4
        m.cube(arm, [x0, 12, -2], [4, 12, 4], [40, 16] if s < 0 else [32, 48])
        m.cube(arm, [x0, 12, -2], [4, 12, 4], [40, 32] if s < 0 else [48, 48], inflate=0.25)
        m.bone(fore, [6 * s, 18, 0], arm)
        m.bend(arm, fore, 18)
        m.bone(f"{side}_item", [6 * s, 12, -2], fore)
        leg = f"{side}_leg"
        shin = f"{side}_shin"
        m.bone(leg, [1.9 * s, 12, 0], "root")
        lx = -3.9 if s < 0 else -0.1
        m.cube(leg, [lx, 0, -2], [4, 12, 4], [0, 16] if s < 0 else [16, 48])
        m.cube(leg, [lx, 0, -2], [4, 12, 4], [0, 32] if s < 0 else [0, 48], inflate=0.25)
        m.bone(shin, [1.9 * s, 6, 0], leg)
        m.bend(leg, shin, 6)
    return m


S = "easeInOutSine"
C = "catmullrom"


def mirror(keys, ax=(1, -1, -1)):
    """Lo mismo para el otro lado: misma x, y y z al revés."""
    out = {}
    for t, v in keys.items():
        if isinstance(v, tuple):
            out[t] = ([v[0][0] * ax[0], v[0][1] * ax[1], v[0][2] * ax[2]], v[1])
        else:
            out[t] = [v[0] * ax[0], v[1] * ax[1], v[2] * ax[2]]
    return out


def shift(keys, length, d):
    """Desfasa unas claves de un ciclo (para la pierna/brazo del otro lado)."""
    out = {}
    for t, v in keys.items():
        nt = round((t + d) % length, 4)
        out[nt] = v
    out[length] = out[0.0] if 0.0 in out else out[min(out)]
    if 0.0 not in out:
        out[0.0] = out[length]
    return out


def anims():
    out = []

    # ---------------------------------------------------------------- quieto: respira, hombros y cabeza muy suave
    a = Anim("idle", 4.0, True)
    a.rot("chest", {0: ([0, 0, 0], C), 2.0: ([1.6, 0, 0], C), 4.0: ([0, 0, 0], C)})
    a.rot("head", {0: ([0, 0, 0], C), 2.0: ([-1.8, 0, 0], C), 4.0: ([0, 0, 0], C)})
    a.rot("right_arm", {0: ([0, 0, 3], C), 2.0: ([1, 0, 5], C), 4.0: ([0, 0, 3], C)})
    a.rot("left_arm", {0: ([0, 0, -3], C), 2.0: ([1, 0, -5], C), 4.0: ([0, 0, -3], C)})
    a.rot("right_forearm", [-6, 0, 0])
    a.rot("left_forearm", [-6, 0, 0])
    a.pos("chest", {0: ([0, 0, 0], C), 2.0: ([0, 0.25, 0], C), 4.0: ([0, 0, 0], C)})
    out.append(a)

    # mirar alrededor (de vez en cuando, en la capa de gestos)
    a = Anim("look_around", 5.0, False)
    a.rot("head", {0: [0, 0, 0], 0.6: ([0, 38, 0], S), 1.8: ([4, 34, 0], S), 2.6: ([0, -36, 0], S), 3.8: ([-3, -32, 0], S), 5.0: ([0, 0, 0], S)})
    a.rot("chest", {0: [0, 0, 0], 0.6: ([0, 8, 0], S), 1.8: ([0, 7, 0], S), 2.6: ([0, -8, 0], S), 3.8: ([0, -7, 0], S), 5.0: ([0, 0, 0], S)})
    out.append(a)

    # ---------------------------------------------------------------- caminar (1 s por ciclo de dos pasos)
    L = 1.0
    a = Anim("walk", L, True)
    leg = {0: ([-26, 0, 0], C), 0.25: ([0, 0, 0], C), 0.5: ([26, 0, 0], C), 0.75: ([0, 0, 0], C), 1.0: ([-26, 0, 0], C)}
    shin = {0: ([6, 0, 0], C), 0.25: ([4, 0, 0], C), 0.5: ([12, 0, 0], C), 0.62: ([42, 0, 0], C), 0.8: ([24, 0, 0], C), 1.0: ([6, 0, 0], C)}
    arm = {0: ([22, 0, 3], C), 0.5: ([-22, 0, 3], C), 1.0: ([22, 0, 3], C)}
    fore = {0: ([-8, 0, 0], C), 0.5: ([-24, 0, 0], C), 1.0: ([-8, 0, 0], C)}
    a.rot("right_leg", leg).rot("right_shin", shin)
    a.rot("left_leg", shift(leg, L, 0.5)).rot("left_shin", shift(shin, L, 0.5))
    a.rot("right_arm", arm).rot("right_forearm", fore)
    a.rot("left_arm", mirror(shift(arm, L, 0.5))).rot("left_forearm", shift(fore, L, 0.5))
    a.pos("root", {0: ([0, -0.3, 0], C), 0.25: ([0, 0.5, 0], C), 0.5: ([0, -0.3, 0], C), 0.75: ([0, 0.5, 0], C), 1.0: ([0, -0.3, 0], C)})
    a.rot("body", {0: ([2, 4, 0], C), 0.5: ([2, -4, 0], C), 1.0: ([2, 4, 0], C)})
    a.rot("chest", {0: ([0, -7, 0], C), 0.5: ([0, 7, 0], C), 1.0: ([0, -7, 0], C)})
    a.rot("head", {0: ([0, 3, 0], C), 0.5: ([0, -3, 0], C), 1.0: ([0, 3, 0], C)})
    out.append(a)

    # ---------------------------------------------------------------- correr
    L = 0.62
    a = Anim("run", L, True)
    h = L / 2
    leg = {0: ([-52, 0, 0], C), h / 2: ([-6, 0, 0], C), h: ([46, 0, 0], C), h * 1.5: ([0, 0, 0], C), L: ([-52, 0, 0], C)}
    shin = {0: ([14, 0, 0], C), h / 2: ([8, 0, 0], C), h: ([24, 0, 0], C), h * 1.3: ([95, 0, 0], C), h * 1.7: ([60, 0, 0], C), L: ([14, 0, 0], C)}
    arm = {0: ([48, 0, 5], C), h: ([-55, 0, 5], C), L: ([48, 0, 5], C)}
    fore = {0: ([-55, 0, 0], C), h: ([-80, 0, 0], C), L: ([-55, 0, 0], C)}
    a.rot("right_leg", leg).rot("right_shin", shin)
    a.rot("left_leg", shift(leg, L, h)).rot("left_shin", shift(shin, L, h))
    a.rot("right_arm", arm).rot("right_forearm", fore)
    a.rot("left_arm", mirror(shift(arm, L, h))).rot("left_forearm", shift(fore, L, h))
    a.pos("root", {0: ([0, -1, 0], C), h / 2: ([0, 1.2, 0], C), h: ([0, -1, 0], C), h * 1.5: ([0, 1.2, 0], C), L: ([0, -1, 0], C)})
    a.rot("body", {0: ([10, 6, 0], C), h: ([10, -6, 0], C), L: ([10, 6, 0], C)})
    a.rot("chest", {0: ([6, -10, 0], C), h: ([6, 10, 0], C), L: ([6, -10, 0], C)})
    a.rot("head", {0: ([-12, 6, 0], C), h: ([-12, -6, 0], C), L: ([-12, 6, 0], C)})
    out.append(a)

    # ---------------------------------------------------------------- hablar (gestos con la mano)
    a = Anim("talk", 2.4, True)
    a.rot("right_arm", {0: ([-12, 0, 8], C), 0.6: ([-38, 0, 14], C), 1.2: ([-18, 0, 6], C), 1.8: ([-32, 0, 16], C), 2.4: ([-12, 0, 8], C)})
    a.rot("right_forearm", {0: ([-25, 0, 0], C), 0.6: ([-62, 0, -10], C), 1.2: ([-35, 0, 0], C), 1.8: ([-58, 0, 8], C), 2.4: ([-25, 0, 0], C)})
    a.rot("left_arm", {0: ([-4, 0, -4], C), 1.2: ([-14, 0, -8], C), 2.4: ([-4, 0, -4], C)})
    a.rot("left_forearm", {0: ([-12, 0, 0], C), 1.2: ([-30, 0, 0], C), 2.4: ([-12, 0, 0], C)})
    a.rot("head", {0: ([0, 0, 0], C), 0.3: ([4, 0, 2], C), 0.9: ([-2, 0, -1], C), 1.5: ([3, 0, 2], C), 2.1: ([0, 0, -1], C), 2.4: ([0, 0, 0], C)})
    a.rot("chest", {0: ([0, 0, 0], C), 0.6: ([1, -5, 0], C), 1.8: ([1, 4, 0], C), 2.4: ([0, 0, 0], C)})
    out.append(a)

    # ---------------------------------------------------------------- saludar con la mano
    a = Anim("wave", 1.9, False)
    a.rot("right_arm", {0: [0, 0, 0], 0.32: ([-12, 0, 148], "easeOutBack"), 1.5: ([-12, 0, 146], S), 1.9: ([0, 0, 0], S)})
    a.rot("right_forearm", {0: [0, 0, 0], 0.32: ([-18, 0, 0], S), 0.5: ([-18, 0, 26], S), 0.7: ([-18, 0, -14], S), 0.9: ([-18, 0, 26], S),
                            1.1: ([-18, 0, -14], S), 1.3: ([-18, 0, 20], S), 1.5: ([-18, 0, 0], S), 1.9: ([0, 0, 0], S)})
    a.rot("head", {0: [0, 0, 0], 0.4: ([-3, 0, 6], S), 1.5: ([-3, 0, 6], S), 1.9: ([0, 0, 0], S)})
    a.rot("chest", {0: [0, 0, 0], 0.4: ([0, 0, 3], S), 1.5: ([0, 0, 3], S), 1.9: ([0, 0, 0], S)})
    out.append(a)

    # ---------------------------------------------------------------- señalar (se queda señalando)
    a = Anim("point", 0.6, "hold")
    a.rot("right_arm", {0: [0, 0, 0], 0.35: ([-86, -6, 0], "easeOutCubic"), 0.6: ([-84, -6, 0], S)})
    a.rot("right_forearm", {0: [0, 0, 0], 0.35: ([-4, 0, 0], S)})
    a.rot("chest", {0: [0, 0, 0], 0.35: ([0, -10, 0], S)})
    a.rot("head", {0: [0, 0, 0], 0.35: ([0, -6, 0], S)})
    out.append(a)

    # ---------------------------------------------------------------- sí y no con la cabeza
    a = Anim("nod", 0.9, False)
    a.rot("head", {0: [0, 0, 0], 0.2: ([15, 0, 0], S), 0.4: ([-2, 0, 0], S), 0.6: ([12, 0, 0], S), 0.9: ([0, 0, 0], S)})
    out.append(a)
    a = Anim("shake_head", 1.0, False)
    a.rot("head", {0: [0, 0, 0], 0.15: ([0, 24, 0], S), 0.35: ([0, -24, 0], S), 0.55: ([0, 18, 0], S), 0.75: ([0, -12, 0], S),
                   1.0: ([0, 0, 0], S)})
    out.append(a)

    # ---------------------------------------------------------------- reírse
    a = Anim("laugh", 2.0, False)
    bounce = {0: [0, 0, 0], 0.3: ([-12, 0, 0], S)}
    t = 0.3
    for i in range(6):
        t += 0.2
        bounce[round(t, 3)] = ([-6 if i % 2 == 0 else -13, 0, 0], S)
    bounce[2.0] = ([0, 0, 0], S)
    a.rot("chest", bounce)
    a.rot("head", {0: [0, 0, 0], 0.3: ([-16, 0, 0], S), 1.6: ([-12, 0, 4], S), 2.0: ([0, 0, 0], S)})
    a.rot("right_arm", {0: [0, 0, 0], 0.35: ([-32, 0, -12], S), 1.6: ([-30, 0, -12], S), 2.0: ([0, 0, 0], S)})
    a.rot("left_arm", {0: [0, 0, 0], 0.35: ([-32, 0, 12], S), 1.6: ([-30, 0, 12], S), 2.0: ([0, 0, 0], S)})
    a.rot("right_forearm", {0: [0, 0, 0], 0.35: ([-72, 0, 0], S), 1.6: ([-70, 0, 0], S), 2.0: ([0, 0, 0], S)})
    a.rot("left_forearm", {0: [0, 0, 0], 0.35: ([-72, 0, 0], S), 1.6: ([-70, 0, 0], S), 2.0: ([0, 0, 0], S)})
    out.append(a)

    # ---------------------------------------------------------------- festejar (brazos arriba y brinco)
    a = Anim("cheer", 1.6, False)
    a.rot("right_arm", {0: [0, 0, 0], 0.22: ([-8, 0, 30], S), 0.45: ([-10, 0, 158], "easeOutBack"), 1.25: ([-10, 0, 150], S), 1.6: ([0, 0, 0], S)})
    a.rot("left_arm", {0: [0, 0, 0], 0.22: ([-8, 0, -30], S), 0.45: ([-10, 0, -158], "easeOutBack"), 1.25: ([-10, 0, -150], S), 1.6: ([0, 0, 0], S)})
    a.rot("right_forearm", {0: [0, 0, 0], 0.45: ([-10, 0, 0], S), 0.65: ([-30, 0, 0], S), 0.85: ([-8, 0, 0], S), 1.05: ([-28, 0, 0], S), 1.6: ([0, 0, 0], S)})
    a.rot("left_forearm", {0: [0, 0, 0], 0.45: ([-10, 0, 0], S), 0.65: ([-30, 0, 0], S), 0.85: ([-8, 0, 0], S), 1.05: ([-28, 0, 0], S), 1.6: ([0, 0, 0], S)})
    a.pos("root", {0: [0, 0, 0], 0.22: ([0, -1.6, 0], S), 0.45: ([0, 5, 0], "easeOutQuad"), 0.68: ([0, 0, 0], "easeInQuad"), 0.8: ([0, -1.2, 0], S), 1.0: ([0, 0, 0], S)})
    for side in ("right", "left"):
        a.rot(f"{side}_leg", {0: [0, 0, 0], 0.22: ([-22, 0, 0], S), 0.45: ([4, 0, 0], S), 0.68: ([0, 0, 0], S), 0.8: ([-18, 0, 0], S), 1.0: ([0, 0, 0], S)})
        a.rot(f"{side}_shin", {0: [0, 0, 0], 0.22: ([42, 0, 0], S), 0.45: ([6, 0, 0], S), 0.68: ([0, 0, 0], S), 0.8: ([34, 0, 0], S), 1.0: ([0, 0, 0], S)})
    a.rot("head", {0: [0, 0, 0], 0.45: ([-14, 0, 0], S), 1.25: ([-10, 0, 0], S), 1.6: ([0, 0, 0], S)})
    out.append(a)

    # ---------------------------------------------------------------- triste (se queda así)
    a = Anim("sad", 1.0, "hold")
    a.rot("head", {0: [0, 0, 0], 1.0: ([26, 0, 0], S)})
    a.rot("chest", {0: [0, 0, 0], 1.0: ([12, 0, 0], S)})
    a.rot("right_arm", {0: [0, 0, 0], 1.0: ([6, 0, 1], S)})
    a.rot("left_arm", {0: [0, 0, 0], 1.0: ([6, 0, -1], S)})
    out.append(a)

    # ---------------------------------------------------------------- pensar (mano a la barbilla)
    a = Anim("think", 0.8, "hold")
    a.rot("right_arm", {0: [0, 0, 0], 0.8: ([-56, 0, -18], S)})
    a.rot("right_forearm", {0: [0, 0, 0], 0.8: ([-108, 0, 0], S)})
    a.rot("left_arm", {0: [0, 0, 0], 0.8: ([-30, 0, 22], S)})
    a.rot("left_forearm", {0: [0, 0, 0], 0.8: ([-78, 0, 0], S)})
    a.rot("head", {0: [0, 0, 0], 0.8: ([8, 0, 7], S)})
    out.append(a)

    # ---------------------------------------------------------------- sentarse (se queda sentado)
    a = Anim("sit", 0.6, "hold")
    a.pos("root", {0: [0, 0, 0], 0.6: ([0, -6, 0], S)})
    for side, s in (("right", -1), ("left", 1)):
        a.rot(f"{side}_leg", {0: [0, 0, 0], 0.6: ([-88, 8 * -s, 0], S)})
        a.rot(f"{side}_shin", {0: [0, 0, 0], 0.6: ([82, 0, 0], S)})
    a.rot("right_arm", {0: [0, 0, 0], 0.6: ([-20, 0, 4], S)})
    a.rot("left_arm", {0: [0, 0, 0], 0.6: ([-20, 0, -4], S)})
    a.rot("right_forearm", {0: [0, 0, 0], 0.6: ([-30, 0, 0], S)})
    a.rot("left_forearm", {0: [0, 0, 0], 0.6: ([-30, 0, 0], S)})
    out.append(a)

    # ---------------------------------------------------------------- atacar (golpe de arriba abajo)
    a = Anim("attack", 0.6, False)
    a.rot("right_arm", {0: [0, 0, 0], 0.16: ([-168, 0, 12], "easeOutQuad"), 0.3: ([-22, 0, -6], "easeInQuart"), 0.6: ([0, 0, 0], S)})
    a.rot("right_forearm", {0: [0, 0, 0], 0.16: ([-45, 0, 0], S), 0.3: ([-6, 0, 0], S), 0.6: ([0, 0, 0], S)})
    a.rot("chest", {0: [0, 0, 0], 0.16: ([-6, 16, 0], S), 0.3: ([10, -22, 0], "easeInQuart"), 0.6: ([0, 0, 0], S)})
    a.rot("left_arm", {0: [0, 0, 0], 0.16: ([-30, 0, -10], S), 0.3: ([20, 0, -6], S), 0.6: ([0, 0, 0], S)})
    a.event(0.3, "golpe")
    out.append(a)

    # ---------------------------------------------------------------- recibir daño
    a = Anim("hurt", 0.4, False)
    a.rot("chest", {0: [0, 0, 0], 0.08: ([-12, 0, 0], "easeOutQuad"), 0.4: ([0, 0, 0], S)})
    a.rot("head", {0: [0, 0, 0], 0.08: ([-16, 0, 0], "easeOutQuad"), 0.4: ([0, 0, 0], S)})
    a.rot("right_arm", {0: [0, 0, 0], 0.08: ([0, 0, 14], "easeOutQuad"), 0.4: ([0, 0, 0], S)})
    a.rot("left_arm", {0: [0, 0, 0], 0.08: ([0, 0, -14], "easeOutQuad"), 0.4: ([0, 0, 0], S)})
    out.append(a)

    # ---------------------------------------------------------------- morir (cae de espaldas)
    a = Anim("death", 1.3, "hold")
    a.rot("root", {0: [0, 0, 0], 0.25: ([8, 0, 0], S), 1.0: ([-90, 0, 0], "easeInQuad"), 1.15: ([-84, 0, 0], S), 1.3: ([-90, 0, 0], S)})
    a.pos("root", {0: [0, 0, 0], 1.0: ([0, 0, 0], S), 1.3: ([0, 1, 0], S)})
    a.rot("right_arm", {0: [0, 0, 0], 0.5: ([-40, 0, 30], S), 1.0: ([-10, 0, 70], S), 1.3: ([-10, 0, 62], S)})
    a.rot("left_arm", {0: [0, 0, 0], 0.5: ([-40, 0, -30], S), 1.0: ([-10, 0, -70], S), 1.3: ([-10, 0, -62], S)})
    a.rot("right_leg", {0: [0, 0, 0], 0.25: ([-20, 0, 0], S), 1.0: ([-6, 0, 6], S)})
    a.rot("right_shin", {0: [0, 0, 0], 0.25: ([40, 0, 0], S), 1.0: ([10, 0, 0], S)})
    a.rot("left_leg", {0: [0, 0, 0], 0.5: ([-10, 0, 0], S), 1.0: ([0, 0, -8], S)})
    a.rot("head", {0: [0, 0, 0], 0.6: ([20, 0, 0], S), 1.0: ([-10, 10, 0], S), 1.3: ([-12, 18, 0], S)})
    out.append(a)

    # ---------------------------------------------------------------- pesca: lanzar, esperar, recoger
    a = Anim("fish_cast", 1.0, False)
    a.rot("right_arm", {0: ([-38, 0, 0], S), 0.36: ([-172, 0, 6], "easeOutQuad"), 0.55: ([-48, 0, 0], "easeInBack"), 1.0: ([-40, 0, 0], S)})
    a.rot("right_forearm", {0: ([-34, 0, 0], S), 0.36: ([-34, 0, 0], S), 0.55: ([-8, 0, 0], S), 1.0: ([-32, 0, 0], S)})
    a.rot("chest", {0: [0, 0, 0], 0.36: ([-6, 16, 0], S), 0.55: ([8, -10, 0], S), 1.0: ([0, 0, 0], S)})
    a.rot("left_arm", {0: ([-24, 0, 0], S), 0.36: ([-10, 0, -10], S), 0.55: ([-30, 0, 0], S), 1.0: ([-26, 0, 0], S)})
    a.rot("left_forearm", {0: ([-44, 0, 0], S), 1.0: ([-44, 0, 0], S)})
    a.event(0.55, "lanzar")
    out.append(a)
    a = Anim("fish_hold", 3.0, True)
    a.rot("right_arm", {0: ([-40, 0, 2], C), 1.5: ([-44, 0, 3], C), 3.0: ([-40, 0, 2], C)})
    a.rot("right_forearm", [-32, 0, 0])
    a.rot("left_arm", {0: ([-24, 0, -6], C), 1.5: ([-26, 0, -6], C), 3.0: ([-24, 0, -6], C)})
    a.rot("left_forearm", [-46, 0, 0])
    a.rot("head", {0: ([6, 0, 0], C), 1.5: ([8, 0, 0], C), 3.0: ([6, 0, 0], C)})
    out.append(a)
    a = Anim("fish_reel", 0.5, True)
    a.rot("right_arm", {0: ([-52, 0, 2], C), 0.25: ([-56, 0, 2], C), 0.5: ([-52, 0, 2], C)})
    a.rot("right_forearm", [-30, 0, 0])
    a.rot("left_arm", {0: ([-24, 0, -6], C), 0.25: ([-34, 0, -10], C), 0.5: ([-24, 0, -6], C)})
    a.rot("left_forearm", {0: ([-50, 0, 0], C), 0.125: ([-66, 0, 8], C), 0.25: ([-50, 0, 14], C), 0.375: ([-34, 0, 8], C), 0.5: ([-50, 0, 0], C)})
    a.rot("chest", {0: ([6, 0, 0], C), 0.25: ([8, 0, 0], C), 0.5: ([6, 0, 0], C)})
    out.append(a)

    # ---------------------------------------------------------------- bailar
    L = 1.2
    a = Anim("dance", L, True)
    a.rot("right_arm", {0: ([-20, 0, 40], C), 0.3: ([-160, 0, 20], C), 0.6: ([-20, 0, 40], C), 0.9: ([-40, 0, 70], C), 1.2: ([-20, 0, 40], C)})
    a.rot("left_arm", {0: ([-40, 0, -70], C), 0.3: ([-20, 0, -40], C), 0.6: ([-160, 0, -20], C), 0.9: ([-20, 0, -40], C), 1.2: ([-40, 0, -70], C)})
    a.rot("right_forearm", {0: ([-60, 0, 0], C), 0.3: ([-10, 0, 0], C), 0.6: ([-60, 0, 0], C), 0.9: ([-40, 0, 0], C), 1.2: ([-60, 0, 0], C)})
    a.rot("left_forearm", {0: ([-40, 0, 0], C), 0.3: ([-60, 0, 0], C), 0.6: ([-10, 0, 0], C), 0.9: ([-60, 0, 0], C), 1.2: ([-40, 0, 0], C)})
    a.rot("body", {0: ([0, 0, 6], C), 0.3: ([0, 0, -6], C), 0.6: ([0, 0, 6], C), 0.9: ([0, 0, -6], C), 1.2: ([0, 0, 6], C)})
    a.rot("chest", {0: ([0, 10, -4], C), 0.6: ([0, -10, 4], C), 1.2: ([0, 10, -4], C)})
    a.rot("head", {0: ([4, -8, 5], C), 0.3: ([-4, 0, -5], C), 0.6: ([4, 8, 5], C), 0.9: ([-4, 0, -5], C), 1.2: ([4, -8, 5], C)})
    a.pos("root", {0: ([0, 0, 0], C), 0.15: ([0, 1.2, 0], C), 0.3: ([0, 0, 0], C), 0.45: ([0, 1.2, 0], C), 0.6: ([0, 0, 0], C), 0.75: ([0, 1.2, 0], C),
                   0.9: ([0, 0, 0], C), 1.05: ([0, 1.2, 0], C), 1.2: ([0, 0, 0], C)})
    for side, d in (("right", 0), ("left", 0.6)):
        leg = {0: ([-14, 0, 0], C), 0.3: ([4, 0, 0], C), 0.6: ([0, 0, 0], C), 0.9: ([4, 0, 0], C), 1.2: ([-14, 0, 0], C)}
        shin = {0: ([30, 0, 0], C), 0.3: ([4, 0, 0], C), 0.6: ([6, 0, 0], C), 0.9: ([4, 0, 0], C), 1.2: ([30, 0, 0], C)}
        a.rot(f"{side}_leg", shift(leg, L, d) if d else leg).rot(f"{side}_shin", shift(shin, L, d) if d else shin)
    out.append(a)
    return out


if __name__ == "__main__":
    model().save(os.path.join(A, "geo", "persona.geo.json"))
    save_anims("persona", anims(), os.path.join(A, "animations", "persona.animation.json"))
    print("persona: modelo y", len(anims()), "animaciones")
