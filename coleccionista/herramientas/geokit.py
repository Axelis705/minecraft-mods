#!/usr/bin/env python3
"""Herramienta común para hacer modelos "a mano" por código (los del hotel: el Botones, las ratas de cuerda, la escopeta, la puerta).

Igual que mascota.py, pero reutilizable: se declaran huesos y cubos (unidades de Blockbench: 16 = 1 bloque, y hacia arriba, el frente
mira a -z), un pintor por material, y la herramienta acomoda la textura (UV de caja, como Minecraft), escribe el .geo.json, el
.animation.json y la textura, y dibuja vistas previas sin Minecraft (cajas con el color promedio de cada cara).

Uso desde otro archivo:
    m = Modelo('botones', 256, 256)
    m.hueso('root', None, (0, 0, 0))
    m.cubo('root', (-4, 0, -4), (8, 8, 8), 'madera')
    m.guardar(pintor, anims, glow=['ojos'])
"""
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw

AQUI = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista')


# ---------------------------------------------------------------- colores y ruido
def hexc(c):
    c = c.lstrip('#')
    return np.array([int(c[i:i + 2], 16) for i in (0, 2, 4)], np.float32)


def ruido(w, h, celda, semilla):
    """Ruido suave (valor interpolado) de h x w, entre 0 y 1."""
    rs = np.random.RandomState(semilla)
    g = rs.rand(h // max(1, celda) + 3, w // max(1, celda) + 3)
    y, x = np.mgrid[0:h, 0:w] / max(1, celda)
    ix, iy = x.astype(int), y.astype(int)
    fx, fy = x - ix, y - iy
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a = g[iy, ix] * (1 - fx) + g[iy, ix + 1] * fx
    b = g[iy + 1, ix] * (1 - fx) + g[iy + 1, ix + 1] * fx
    return a * (1 - fy) + b * fy


def liso(w, h, color, s, var=0.18, celda=3):
    """Un color con variación suave (tela, pintura vieja)."""
    rs = np.random.RandomState(s)
    n = ruido(w, h, celda, s) * 0.6 + rs.rand(h, w) * 0.4
    return hexc(color) * (1 - var / 2 + var * n)[..., None]


def manchas(rgb, s, color, umbral=0.75, fuerza=0.35, celda=3):
    """Ensucia: donde el ruido pasa el umbral, mezcla con un color (mugre, óxido, sangre seca)."""
    h, w = rgb.shape[:2]
    m = ruido(w, h, celda, s) > umbral
    return np.where(m[..., None], rgb * (1 - fuerza) + hexc(color) * fuerza, rgb)


def caras_caja(u, v, w, h, d):
    """Las caras de un cubo con UV de caja (como Minecraft): cara -> (x, y, ancho, alto)."""
    return {
        'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d),
        'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
        'west': (u + d + w, v + d, d, h), 'south': (u + d + w + d, v + d, w, h),
    }


def rotm(rx, ry, rz):
    rx, ry, rz = (math.radians(v) for v in (rx, ry, rz))
    X = np.array([[1, 0, 0], [0, math.cos(rx), -math.sin(rx)], [0, math.sin(rx), math.cos(rx)]])
    Y = np.array([[math.cos(ry), 0, math.sin(ry)], [0, 1, 0], [-math.sin(ry), 0, math.cos(ry)]])
    Z = np.array([[math.cos(rz), -math.sin(rz), 0], [math.sin(rz), math.cos(rz), 0], [0, 0, 1]])
    return Z @ Y @ X


class Modelo:
    def __init__(self, nombre, tw=128, th=128):
        self.nombre = nombre
        self.tw, self.th = tw, th
        self.huesos = []   # (nombre, padre, pivote, giro)
        self.cubos = []    # dict

    def hueso(self, nombre, padre, pivote, giro=(0, 0, 0)):
        self.huesos.append((nombre, padre, list(pivote), list(giro)))

    def cubo(self, h, o, t, mat, inflar=0.0, giro=None, pivote=None):
        c = {'hueso': h, 'o': list(o), 't': list(t), 'mat': mat, 'inflar': inflar}
        if giro:
            c['giro'] = list(giro)
            c['pivote'] = list(pivote)
        self.cubos.append(c)

    # ------------------------------------------------------------ textura
    def empacar(self):
        """Acomoda las UV de caja de todos los cubos en la textura (por estantes, los más altos primero)."""
        tam = []
        for i, c in enumerate(self.cubos):
            w, h, d = (max(1, int(math.ceil(x))) for x in c['t'])
            tam.append((i, 2 * (w + d), d + h, w, h, d))
        tam.sort(key=lambda r: (-r[2], -r[1]))
        x = y = fila = 0
        for i, ww, hh, w, h, d in tam:
            if x + ww > self.tw:
                x, y, fila = 0, y + fila, 0
            if y + hh > self.th:
                raise SystemExit(f'{self.nombre}: no cabe en la textura de {self.tw}x{self.th}')
            self.cubos[i]['uv'] = [x, y]
            self.cubos[i]['uvt'] = (w, h, d)
            x += ww
            fila = max(fila, hh)

    def textura(self, pintor):
        """pintor(mat, cara, w, h, semilla, cubo) -> rgb (h x w x 3) o (rgb, alfa)."""
        img = Image.new('RGBA', (self.tw, self.th), (0, 0, 0, 0))
        for i, c in enumerate(self.cubos):
            u, v = c['uv']
            w, h, d = c['uvt']
            for cara, (x, y, cw, ch) in caras_caja(u, v, w, h, d).items():
                if cw <= 0 or ch <= 0:
                    continue
                r = pintor(c['mat'], cara, cw, ch, 1000 + i * 7 + len(cara), c)
                if isinstance(r, tuple):
                    rgb, a = r
                else:
                    rgb, a = r, np.full((ch, cw), 255, np.uint8)
                rgb = np.broadcast_to(rgb, (ch, cw, 3))
                out = np.dstack([np.clip(rgb, 0, 255).astype(np.uint8), a.astype(np.uint8)])
                img.paste(Image.fromarray(out, 'RGBA'), (x, y))
        return img

    # ------------------------------------------------------------ archivos
    def geo(self, glow=(), solo=None, bounds=(4, 4, 1.5)):
        """El .geo.json. {@code solo}: si se da, solo esos huesos (y se cuelgan de la raíz los que se queden sin padre)."""
        bones = []
        nombres = {h[0] for h in self.huesos} if solo is None else set(solo)
        for nombre, padre, piv, giro in self.huesos:
            if nombre not in nombres:
                continue
            b = {'name': nombre, 'pivot': piv}
            if padre and padre in nombres:
                b['parent'] = padre
            if any(giro):
                b['rotation'] = giro
            cs = []
            for c in self.cubos:
                if c['hueso'] != nombre:
                    continue
                x = {'origin': c['o'], 'size': c['t'], 'uv': c['uv']}
                if c['inflar']:
                    x['inflate'] = c['inflar']
                if c.get('giro'):
                    x['rotation'] = c['giro']
                    x['pivot'] = c['pivote']
                cs.append(x)
            if cs:
                b['cubes'] = cs
            bones.append(b)
        return {'format_version': '1.12.0', 'minecraft:geometry': [{
            'description': {'identifier': 'geometry.coleccionista.' + self.nombre, 'texture_width': self.tw,
                            'texture_height': self.th, 'visible_bounds_width': bounds[0], 'visible_bounds_height': bounds[1],
                            'visible_bounds_offset': [0, bounds[2], 0]},
            'axelis_glow': [g for g in glow if g in nombres],
            'bones': bones}]}

    def guardar(self, pintor, anims=None, glow=(), nombre=None, solo=None, tex=None, bounds=(4, 4, 1.5)):
        """Escribe geo/N.geo.json, animations/N.animation.json y textures/entity/N.png (devuelve la textura)."""
        nombre = nombre or self.nombre
        if tex is None:
            if not self.cubos or 'uv' not in self.cubos[0]:
                self.empacar()
            tex = self.textura(pintor)
        for d in ('geo', 'animations', os.path.join('textures', 'entity')):
            os.makedirs(os.path.join(ASSETS, d), exist_ok=True)
        json.dump(self.geo(glow, solo, bounds), open(os.path.join(ASSETS, 'geo', nombre + '.geo.json'), 'w'), indent=1)
        if anims is not None:
            json.dump({'format_version': '1.8.0', 'animations': anims}, open(os.path.join(ASSETS, 'animations', nombre + '.animation.json'), 'w'),
                      indent=1)
        tex.save(os.path.join(ASSETS, 'textures', 'entity', nombre + '.png'))
        return tex

    # ------------------------------------------------------------ vista previa (sin Minecraft)
    def vista(self, tex, salida, yaw=35, pitch=18, extra=None, escala=7, tam=(900, 560), centro=(450, 470), ocultar=()):
        """Dibuja el modelo con cada cara del color promedio de su textura. {@code extra}: giros de más por hueso (una pose)."""
        extra = extra or {}
        mats = {}
        porn = {h[0]: h for h in self.huesos}

        def mundo(nombre):
            if nombre in mats:
                return mats[nombre]
            n, padre, piv, giro = porn[nombre]
            g = np.array(giro, float) + np.array(extra.get(nombre, (0, 0, 0)), float)
            R_ = rotm(-g[0], g[1], -g[2])
            M = np.eye(4)
            M[:3, :3] = R_
            T1, T2 = np.eye(4), np.eye(4)
            T1[:3, 3] = piv
            T2[:3, 3] = [-v for v in piv]
            L = T1 @ M @ T2
            P = mundo(padre) if padre else np.eye(4)
            mats[nombre] = P @ L
            return mats[nombre]

        def oculto(nombre):
            while nombre:
                if nombre in ocultar:
                    return True
                nombre = porn[nombre][1]
            return False

        caras = []
        t = np.array(tex)
        defs = {
            'north': ([0, 0, 0], [1, 0, 0], [1, 1, 0], [0, 1, 0]), 'south': ([0, 0, 1], [1, 0, 1], [1, 1, 1], [0, 1, 1]),
            'west': ([0, 0, 0], [0, 0, 1], [0, 1, 1], [0, 1, 0]), 'east': ([1, 0, 0], [1, 0, 1], [1, 1, 1], [1, 1, 0]),
            'up': ([0, 1, 0], [1, 1, 0], [1, 1, 1], [0, 1, 1]), 'down': ([0, 0, 0], [1, 0, 0], [1, 0, 1], [0, 0, 1])}
        for c in self.cubos:
            if oculto(c['hueso']):
                continue
            M = mundo(c['hueso'])
            if c.get('giro'):
                g = c['giro']
                Rc = np.eye(4)
                Rc[:3, :3] = rotm(-g[0], g[1], -g[2])
                T1, T2 = np.eye(4), np.eye(4)
                T1[:3, 3] = c['pivote']
                T2[:3, 3] = [-v for v in c['pivote']]
                M = M @ T1 @ Rc @ T2
            o, s = np.array(c['o'], float), np.array(c['t'], float)
            o -= c['inflar']
            s += 2 * c['inflar']
            u, v = c['uv']
            w, h, d = c['uvt']
            reg = caras_caja(u, v, w, h, d)
            for cara, pts in defs.items():
                q = [M @ np.append(o + np.array(p) * s, 1) for p in pts]
                x, y, cw, ch = reg[cara]
                parche = t[y:y + max(1, ch), x:x + max(1, cw)]
                al = parche[..., 3] > 0
                col = parche[..., :3][al].mean(0) if al.any() else np.array([0, 0, 0])
                caras.append(([p[:3] for p in q], col, al.mean() if al.size else 0))
        ya, pi = math.radians(yaw), math.radians(pitch)
        Ry = np.array([[math.cos(ya), 0, math.sin(ya)], [0, 1, 0], [-math.sin(ya), 0, math.cos(ya)]])
        Rx = np.array([[1, 0, 0], [0, math.cos(pi), -math.sin(pi)], [0, math.sin(pi), math.cos(pi)]])
        Rv = Rx @ Ry
        luz = np.array([0.4, 0.8, -0.5])
        luz /= np.linalg.norm(luz)
        S = escala
        img = Image.new('RGB', tam, (40, 38, 44))
        dr = ImageDraw.Draw(img)
        proy = []
        for pts, col, al in caras:
            P = [Rv @ p for p in pts]
            n = np.cross(P[1] - P[0], P[3] - P[0])
            if np.linalg.norm(n) < 1e-9 or al < 0.2:
                continue
            n /= np.linalg.norm(n)
            if n[2] > 0:
                n = -n
            k = 0.55 + 0.45 * max(0, float(np.dot(n, Rv @ luz)))
            z = sum(p[2] for p in P) / 4
            proy.append((z, [(centro[0] + p[0] * S, centro[1] - p[1] * S) for p in P], tuple(int(v) for v in np.clip(col * k, 0, 255))))
        for z, poly, col in sorted(proy, key=lambda r: r[0], reverse=True):
            dr.polygon(poly, fill=col, outline=(20, 18, 16))
        img.save(salida)
        return img
