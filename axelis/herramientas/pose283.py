"""Etapa 283: vista esquemática (de lado y de frente) de las poses del golpe con todo el cuerpo.

Usa las mismas cuentas que ArmaCuerpo.java: brazos dados por su dirección y la del arma (derecha, arriba, delante), giro del tronco,
piernas e inclinación desde los pies. Sirve para ajustar los fotogramas sin abrir el juego.
Uso: python3 pose283.py salida.png  (lee POSES de abajo)
"""
import math, sys
import numpy as np
from PIL import Image, ImageDraw

def norm(v):
    v = np.array(v, float); n = np.linalg.norm(v); return v / n if n > 1e-9 else v

def ry(a):  # rotación alrededor del eje vertical (en coordenadas derecha, arriba, delante: girar hacia la derecha con a>0)
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, -s], [0, 1, 0], [s, 0, c]])  # el frente (0,0,1) va hacia la derecha (s,0,c)... ver abajo

def rx_lean(deg):  # inclinar hacia delante (la cabeza va al frente) alrededor de los pies
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])  # (0,1,0) -> (0,c,s): arriba se va hacia delante

def ortho(a, b):
    a = norm(a); b = np.array(b, float); b = norm(b - a * np.dot(a, b)); return a, b

LEN = {"espada": 14, "hacha": 15, "martillo": 17, "lanza": 26, "dagas": 8}

def pose_points(p):
    """Puntos en (derecha, arriba, delante) en píxeles del modelo, con los pies en el suelo (y=0)."""
    giro = p.get("giro", 0.0)  # >0: hombro derecho atrás
    R = ry(-giro)  # hombro derecho atrás = el pecho mira a la izquierda
    pts = {}
    hip = np.array([0, 12, 0.0]); neck = np.array([0, 24, 0.0])
    pts["cadera"] = hip; pts["cuello"] = neck; pts["cabeza"] = neck + [0, 8, 0]
    for lado, s in (("der", 1), ("izq", -1)):
        sh = neck + R @ np.array([5.0 * s, -2, 0])
        pts["hombro_" + lado] = sh
        a, b = ortho(p["brazo_" + lado], p["arma_" + lado] if ("arma_" + lado) in p else [0, 0, 1])
        a = R @ a; b = R @ b
        mano = sh + a * 10
        pts["mano_" + lado] = mano
        pts["dir_arma_" + lado] = b
    for lado, s, key in (("der", 1, "pierna_der"), ("izq", -1, "pierna_izq")):
        ang = p.get(key, 0.0)  # xRot de Minecraft: negativo = hacia delante
        top = hip + [1.9 * s, 0, 0]
        pts["cadera_" + lado] = top
        pts["pie_" + lado] = top + [0, -12 * math.cos(ang), -12 * math.sin(ang)]
    L = rx_lean(p.get("incl", 0.0))
    off = np.array([0, -p.get("baja", 0.0) * 16, p.get("avance", 0.0) * 16])
    out = {}
    for k, v in pts.items():
        if k.startswith("dir_"):
            out[k] = L @ v
        else:
            out[k] = L @ v + off
    return out

def draw(img, ox, oy, pts, tipo, armas, side=True, sc=5):
    d = ImageDraw.Draw(img)
    def P(v):
        x = v[2] if side else v[0]
        return (ox + x * sc, oy - v[1] * sc)
    d.line([P(pts["pie_der"]), P(pts["cadera_der"])], fill=(70, 60, 140), width=int(4 * sc / 2))
    d.line([P(pts["pie_izq"]), P(pts["cadera_izq"])], fill=(40, 30, 90), width=int(4 * sc / 2))
    d.line([P(pts["cadera"]), P(pts["cuello"])], fill=(120, 190, 200), width=int(8 * sc / 2))
    d.ellipse([P(pts["cabeza"])[0] - 4 * sc, P(pts["cabeza"])[1] - 4 * sc, P(pts["cabeza"])[0] + 4 * sc, P(pts["cabeza"])[1] + 4 * sc], fill=(150, 110, 80))
    for lado, col in (("izq", (150, 120, 90)), ("der", (200, 160, 120))):
        d.line([P(pts["hombro_" + lado]), P(pts["mano_" + lado])], fill=col, width=int(4 * sc / 2))
        if lado in armas:
            m = pts["mano_" + lado]; e = m + pts["dir_arma_" + lado] * LEN[tipo]
            d.line([P(m), P(e)], fill=(230, 230, 240), width=3)
            d.ellipse([P(e)[0] - 4, P(e)[1] - 4, P(e)[0] + 4, P(e)[1] + 4], fill=(255, 80, 80))
    d.line([(ox - 60, oy), (ox + 60, oy)], fill=(90, 140, 60), width=2)

TILT = math.radians(18)  # el arma en la mano no sale a 90 grados del brazo: se inclina 18 hacia la mano (como en el juego)

def blade_actual(a, b):
    a, bp = ortho(a, b)
    return norm(math.cos(TILT) * bp + math.sin(TILT) * a)

def resolve(p, tipo):
    """Completa el brazo izquierdo en las armas a dos manos: su mano va al mango (o al asta)."""
    q = dict(p)
    if q.get("brazo_izq") == "agarre":
        giro = q.get("giro", 0.0); R = ry(-giro)
        neck = np.array([0, 24, 0.0])
        sh_d = neck + R @ np.array([5.0, -2, 0]); sh_i = neck + R @ np.array([-5.0, -2, 0])
        a = R @ norm(q["brazo_der"]); b = R @ blade_actual(q["brazo_der"], q["arma_der"])
        mano = sh_d + a * 10
        objetivo = mano + b * (5 if tipo == "lanza" else -3)
        ai = norm(objetivo - sh_i)
        q["brazo_izq"] = list(np.linalg.inv(R) @ ai)
        q["arma_izq"] = q["arma_der"]
    return q

def sheet(poses, out):
    cols = max(len(v) for v in poses.values())
    W, H = 300, 260
    img = Image.new("RGB", (W * cols * 2, H * len(poses)), (24, 28, 40))
    d = ImageDraw.Draw(img)
    for r, (tipo, frames) in enumerate(poses.items()):
        for c, (name, p) in enumerate(frames):
            q = resolve(p, tipo)
            pts = pose_points(q)
            for lado in ("der", "izq"):
                a = q["brazo_" + lado]; b = q.get("arma_" + lado, [0, 0, 1])
                pts["dir_arma_" + lado] = rx_lean(q.get("incl", 0)) @ (ry(-q.get("giro", 0)) @ blade_actual(a, b))
            armas = ["der"] + (["izq"] if tipo == "dagas" else [])
            if tipo in ("hacha", "martillo", "lanza"):
                armas = ["der"]
            draw(img, c * 2 * W + W // 2, r * H + 220, pts, tipo, armas, side=True, sc=5)
            draw(img, c * 2 * W + W + W // 2, r * H + 220, pts, tipo, armas, side=False, sc=5)
            d.text((c * 2 * W + 6, r * H + 4), tipo + " " + name + " (lado | frente)", fill=(255, 255, 0))
    img.save(out)

# lado: derecha = delante del personaje; de frente: derecha de la imagen = su derecha
POSES = {}
