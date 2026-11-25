#!/usr/bin/env python3
"""La máquina de la final (veintiuno): un cepo de latón viejo, con remaches, donde cada finalista mete la mano izquierda. Por dentro, dos
prensas (aprietan a la primera ronda perdida); arriba, una guillotina entre dos postes (baja a la tercera); al frente, tres focos rojos
(uno por ronda perdida); a un lado, un engrane y un cuadrante con su aguja. Cuando corta, aparece la sangre.

Escribe geo/maquina.geo.json, animations/maquina.animation.json y textures/entity/maquina.png.
La entidad está sobre la mesa (y = 0 del modelo); mira a -z (hacia el rival): el brazo entra por atrás (+z).
Mide 12 px de ancho, 16 de largo y 22 de alto (con la guillotina arriba).

Huesos: raiz, base, prensa_izq, prensa_der, cuchilla, engrane, aguja, luz0..2 (los focos encendidos), sangre. Todo lo mueve el código
(Maquina.axPose).

Uso: python3 herramientas/maquina.py
"""
import json
import math
import os

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista')
TW, TH = 128, 128
HUESOS = []
CUBOS = []


def hueso(nombre, padre, pivote, giro=(0, 0, 0)):
    HUESOS.append((nombre, padre, list(pivote), list(giro)))


def cubo(h, o, t, mat, inflar=0.0):
    CUBOS.append({'hueso': h, 'o': list(o), 't': list(t), 'mat': mat, 'inflar': inflar})


def modelo():
    hueso('raiz', None, (0, 0, 0))
    hueso('base', 'raiz', (0, 0, 0))
    # la plancha de abajo y el túnel (por donde entra el brazo, de +z a -z)
    cubo('base', (-6, 0, -8), (12, 1, 16), 'laton_oscuro')
    cubo('base', (-5, 1, -7), (1.5, 6, 14), 'laton')                 # pared izquierda (x-)
    cubo('base', (3.5, 1, -7), (1.5, 6, 14), 'laton')                # pared derecha (x+)
    cubo('base', (-5, 7, -7), (10, 1.5, 9.5), 'laton')               # el techo, adelante de la ranura
    cubo('base', (-5, 7, 3.7), (10, 1.5, 3.3), 'laton')              # el techo, atrás de la ranura
    cubo('base', (-5, 1, -8), (10, 6.5, 1), 'laton_oscuro')          # la tapa del frente (ahí topan los dedos)
    # el collar de cobre de la entrada
    cubo('base', (-5.6, 7, 7), (11.2, 1.8, 1.2), 'cobre')
    cubo('base', (-5.6, 1, 7), (1.8, 6, 1.2), 'cobre')
    cubo('base', (3.8, 1, 7), (1.8, 6, 1.2), 'cobre')
    # remaches en el techo
    for x in (-4.2, 3.2):
        for z in (-6, -2, 5):
            cubo('base', (x, 8.5, z), (1, 0.5, 1), 'remache')
    # la caja de los focos, al frente arriba
    cubo('base', (-4.5, 8.5, -7), (9, 1.5, 3.4), 'laton_oscuro')
    for k, x in enumerate((-3.6, -0.8, 2.0)):
        cubo('base', (x, 10, -6.2), (1.6, 1.6, 1.6), 'foco_apagado')
    # los postes y la viga de la guillotina (sobre la ranura del techo)
    cubo('base', (-5.2, 8.5, 2.4), (1.3, 13, 1.4), 'hierro')
    cubo('base', (3.9, 8.5, 2.4), (1.3, 13, 1.4), 'hierro')
    cubo('base', (-5.6, 21.5, 2.2), (11.2, 1.6, 1.8), 'hierro')
    # el cuadrante a la derecha
    cubo('base', (5, 2.5, -3.5), (0.6, 4.5, 4.5), 'cuadrante')
    # los focos encendidos (se ven solo cuando hay rondas perdidas)
    for k, x in enumerate((-3.6, -0.8, 2.0)):
        hueso('luz%d' % k, 'base', (x + 0.8, 10.8, -5.4))
        cubo('luz%d' % k, (x, 10, -6.2), (1.6, 1.6, 1.6), 'foco_encendido', 0.18)
    # las prensas de adentro (se cierran sobre el brazo)
    hueso('prensa_izq', 'base', (-3.5, 4, -1))
    cubo('prensa_izq', (-3.5, 2, -5.5), (1, 4.5, 9), 'acero_oscuro')
    hueso('prensa_der', 'base', (3.5, 4, -1))
    cubo('prensa_der', (2.5, 2, -5.5), (1, 4.5, 9), 'acero_oscuro')
    # la guillotina (arriba, lista para bajar)
    hueso('cuchilla', 'base', (0, 19, 3.1))
    cubo('cuchilla', (-3.9, 14.5, 2.8), (7.8, 4.5, 0.6), 'cuchilla')
    cubo('cuchilla', (-3.2, 19, 2.5), (6.4, 2, 1.2), 'hierro')
    # el engrane del lado izquierdo y la aguja del cuadrante
    hueso('engrane', 'base', (-5.6, 4.5, -1.2))
    cubo('engrane', (-6.1, 2, -3.7), (0.8, 5, 5), 'engrane')
    hueso('aguja', 'base', (5.7, 4.75, -1.25))
    cubo('aguja', (5.6, 4.6, -1.35), (0.3, 1.9, 0.2), 'aguja')
    # la sangre (solo después del corte)
    hueso('sangre', 'base', (0, 1, 6))
    cubo('sangre', (-4.8, 1.02, 3), (9.6, 0.1, 7.5), 'sangre')
    cubo('sangre', (-5.7, 3, 8.25), (11.4, 4, 0.1), 'sangre_gotas')
    cubo('sangre', (-3.9, 14.4, 2.75), (7.8, 1.6, 0.7), 'sangre')


# ---------------------------------------------------------------- textura

def hexc(c):
    c = c.lstrip('#')
    return [int(c[i:i + 2], 16) for i in (0, 2, 4)]


def mat(nombre, w, h, cara, s):
    r = np.random.RandomState(s)
    yy, xx = np.mgrid[0:h, 0:w]
    ruido = r.rand(h, w)[..., None]
    a = np.full((h, w), 255, np.uint8)
    if nombre in ('laton', 'laton_oscuro', 'cobre'):
        base = {'laton': '#b08a3a', 'laton_oscuro': '#6c5222', 'cobre': '#a35a2c'}[nombre]
        rgb = np.array(hexc(base), float) * (0.78 + 0.3 * ruido)
        # pátina verde en manchas y rayones
        pat = r.rand(h, w) < 0.07
        rgb[pat] = rgb[pat] * 0.5 + np.array(hexc('#4a7a5c')) * 0.5
        if w > 3 and h > 3:
            rgb[0, :] *= 1.25
            rgb[-1, :] *= 0.7
            rgb[(xx == w // 2) & (yy % 3 == 0)] *= 0.85
    elif nombre == 'remache':
        rgb = np.array(hexc('#d8b860'), float) * (0.85 + 0.2 * ruido)
    elif nombre == 'hierro':
        rgb = np.array(hexc('#3a3a40'), float) * (0.75 + 0.4 * ruido)
        oxido = r.rand(h, w) < 0.12
        rgb[oxido] = np.array(hexc('#6a3a1c')) * (0.8 + 0.3 * r.rand())
    elif nombre == 'acero_oscuro':
        rgb = np.array(hexc('#55585e'), float) * (0.8 + 0.3 * ruido)
        rgb[(xx + yy) % 2 == 0] *= 0.9
    elif nombre == 'cuchilla':
        rgb = np.array(hexc('#c4c9d0'), float) * (0.88 + 0.16 * ruido)
        if cara in ('north', 'south') and h >= 3:
            rgb[-1, :] = np.array(hexc('#f4f8ff'))      # el filo
            rgb[-2, :] *= 1.08
            rgb[0, :] *= 0.75
    elif nombre == 'foco_apagado':
        rgb = np.array(hexc('#3a0c0e'), float) * (0.8 + 0.4 * ruido)
    elif nombre == 'foco_encendido':
        rgb = np.array(hexc('#ff3a24'), float) * (0.9 + 0.15 * ruido)
        if w >= 3 and h >= 3:
            rgb[h // 2, w // 2] = np.array(hexc('#ffd8b0'))
    elif nombre == 'cuadrante':
        rgb = np.array(hexc('#6c5222'), float) * (0.8 + 0.3 * ruido)
        if cara in ('east', 'west') and w >= 4 and h >= 4:
            d = np.hypot(xx - (w - 1) / 2, yy - (h - 1) / 2)
            rgb[d < min(w, h) / 2 - 0.4] = np.array(hexc('#e8dcc0'))
            rgb[(d < min(w, h) / 2 - 0.4) & (yy < h / 2) & (xx > w / 2)] = np.array(hexc('#c83a2a'))   # la zona roja
    elif nombre == 'aguja':
        rgb = np.full((h, w, 3), 20.0)
    elif nombre == 'engrane':
        rgb = np.array(hexc('#8a6a2a'), float) * (0.8 + 0.3 * ruido)
        if cara in ('east', 'west') and w >= 4 and h >= 4:
            d = np.hypot(xx - (w - 1) / 2, yy - (h - 1) / 2)
            ang = np.arctan2(yy - (h - 1) / 2, xx - (w - 1) / 2)
            dientes = (np.cos(ang * 8) > 0.2) & (d > min(w, h) / 2 - 1.2)
            rgb[dientes] *= 0.55
            rgb[d < 0.9] = np.array(hexc('#2a2018'))
    elif nombre == 'sangre':
        rgb = np.array(hexc('#6e0a0e'), float) * (0.7 + 0.5 * ruido)
        a = np.where(r.rand(h, w) < 0.82, 255, 0).astype(np.uint8)
    elif nombre == 'sangre_gotas':
        rgb = np.array(hexc('#7a0c10'), float) * (0.7 + 0.5 * ruido)
        gota = np.zeros((h, w), bool)
        for x in range(w):
            largo = r.randint(0, h + 1)
            gota[:largo, x] = r.rand() < 0.6
        a = np.where(gota, 255, 0).astype(np.uint8)
    else:
        rgb = np.full((h, w, 3), 200.0)
    return np.dstack([np.clip(rgb, 0, 255).astype(np.uint8), a])


def caras_caja(u, v, w, h, d):
    return {
        'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d),
        'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
        'west': (u + d + w, v + d, d, h), 'south': (u + d + w + d, v + d, w, h),
    }


def empacar():
    tam = []
    for i, c in enumerate(CUBOS):
        w, h, d = (max(1, int(math.ceil(x))) for x in c['t'])
        tam.append((i, 2 * (w + d), d + h, w, h, d))
    tam.sort(key=lambda r: -r[2])
    x = y = fila = 0
    for i, ww, hh, w, h, d in tam:
        if x + ww > TW:
            x, y, fila = 0, y + fila, 0
        if y + hh > TH:
            raise SystemExit('no cabe en la textura')
        CUBOS[i]['uv'] = [x, y]
        CUBOS[i]['uvt'] = (w, h, d)
        x += ww
        fila = max(fila, hh)


def textura():
    img = Image.new('RGBA', (TW, TH), (0, 0, 0, 0))
    for i, c in enumerate(CUBOS):
        u, v = c['uv']
        w, h, d = c['uvt']
        for cara, (x, y, cw, ch) in caras_caja(u, v, w, h, d).items():
            if cw <= 0 or ch <= 0:
                continue
            img.paste(Image.fromarray(mat(c['mat'], cw, ch, cara, 900 + i * 7 + len(cara)), 'RGBA'), (x, y))
    return img


def geo():
    bones = []
    for nombre, padre, piv, giro in HUESOS:
        b = {'name': nombre, 'pivot': piv}
        if padre:
            b['parent'] = padre
        if any(giro):
            b['rotation'] = giro
        cs = []
        for c in CUBOS:
            if c['hueso'] != nombre:
                continue
            x = {'origin': c['o'], 'size': c['t'], 'uv': c['uv']}
            if c['inflar']:
                x['inflate'] = c['inflar']
            cs.append(x)
        if cs:
            b['cubes'] = cs
        bones.append(b)
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.coleccionista.maquina', 'texture_width': TW, 'texture_height': TH,
                        'visible_bounds_width': 3, 'visible_bounds_height': 3, 'visible_bounds_offset': [0, 1, 0]},
        'bones': bones}]}


def anims():
    return {'format_version': '1.8.0', 'animations': {
        'animation.maquina.idle': {'loop': True, 'animation_length': 4.0, 'bones': {}},
    }}


def main():
    modelo()
    empacar()
    tex = textura()
    for sub in ('geo', 'animations', os.path.join('textures', 'entity')):
        os.makedirs(os.path.join(A, sub), exist_ok=True)
    json.dump(geo(), open(os.path.join(A, 'geo', 'maquina.geo.json'), 'w'), indent=1)
    json.dump(anims(), open(os.path.join(A, 'animations', 'maquina.animation.json'), 'w'), indent=1)
    tex.save(os.path.join(A, 'textures', 'entity', 'maquina.png'))
    print(len(HUESOS), 'huesos', len(CUBOS), 'cubos')


if __name__ == '__main__':
    main()
