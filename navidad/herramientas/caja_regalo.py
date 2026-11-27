#!/usr/bin/env python3
"""La caja de regalo nueva: papel de envoltura con dibujito, listón que la cruza, tapa aparte (sobresale un poquito) y un moño de dos
lazos con sus tiras. Cuatro envolturas (rojo con dorado, verde con rojo, azul con plata y dorado con rojo).
Por cada color escribe tres modelos con la misma textura: la caja entera (la que se ve quieta), el cuerpo sin tapa y la tapa sola
(para que al abrirla la tapa salga volando). También el modelo vacío para cuando se está abriendo (solo partícula) y el estado de bloque.
Uso: python3 herramientas/caja_regalo.py"""
import json
import os
import sys

import numpy as np

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, 'deco'))
import kit  # noqa: E402
from kit import Prop, Ramp, hexc, metal, with_alpha, rnd  # noqa: E402

kit.RES = 4
A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')

COLORES = {
    0: ('#d0202e', '#9a1220', 'dorado', '#ffffff'),
    1: ('#2a8a46', '#1a6030', 'rojo', '#f4f4f0'),
    2: ('#2f6ad8', '#1c44a0', 'plata', '#e8f4ff'),
    3: ('#e0aa30', '#a87418', 'rojo', '#fff4d0'),
}
LISTON = {'dorado': ('#a8701a', '#d39a2a', '#f0c040', '#ffe58a'), 'rojo': ('#8a0c18', '#b8141e', '#e02434', '#ff7a7a'),
          'plata': ('#8a929c', '#b4bcc6', '#dce2ea', '#ffffff')}


def papel(base, osc, dibujo):
    """Papel de envoltura: rayitas en diagonal un poco más oscuras y copitos (o puntitos) del color del dibujo."""
    b, o, d = (np.array(hexc(x), np.float32) for x in (base, osc, dibujo))

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        diag = (np.floor((X + Y + Z) * 0.9) % 4) == 0
        rgb = np.where(diag[..., None], b * 0.88 + o * 0.12, b)
        # un copito cada tanto (en una rejilla corrida)
        cara = P['face']
        u, v = (X, Z) if cara in ('up', 'down') else (X, Y) if cara in ('north', 'south') else (Z, Y)
        gx, gy = (u + 0.5) % 3.0, (v + 0.5) % 3.0
        copo = ((np.abs(gx - 1.5) < 0.26) & (np.abs(gy - 1.5) < 0.7)) | ((np.abs(gy - 1.5) < 0.26) & (np.abs(gx - 1.5) < 0.7))
        rgb = np.where(copo[..., None], d * 0.9 + b * 0.1, rgb)
        rgb = rgb * (1 + (rnd(X, Y, Z, 3) - 0.5) * 0.06)[..., None]
        return with_alpha(np.clip(rgb, 0, 255))
    return f


def liston(nombre, seed=3):
    return metal(Ramp(*LISTON[nombre]), streak=0.35, seed=seed, sparkle=0.06)


def caja(c):
    base, osc, lis, dib = COLORES[c]
    p = Prop(f'caja_regalo_{c}', '', '')
    pap = papel(base, osc, dib)
    L = liston(lis)
    p.at('cuerpo')
    p.box(3.2, 0, 3.2, 12.8, 7.6, 12.8, pap)
    p.box(7.1, 0, 3.0, 8.9, 7.6, 13.0, L)            # listón de adelante hacia atrás
    p.box(3.0, 0, 7.1, 13.0, 7.6, 8.9, L)            # y de lado a lado
    p.at('tapa')
    p.box(2.7, 7.6, 2.7, 13.3, 9.8, 13.3, pap)
    p.box(7.0, 7.6, 2.5, 9.0, 10.0, 13.5, L)
    p.box(2.5, 7.6, 7.0, 13.5, 10.0, 9.0, L)
    # el moño: nudo, dos lazos inclinados y dos tiras
    p.box(7.0, 9.8, 7.0, 9.0, 11.4, 9.0, liston(lis, 5))
    p.box(3.6, 10.0, 7.3, 7.2, 12.6, 8.7, L, rot=('z', 22.5, (7.2, 10.6, 8)))
    p.box(8.8, 10.0, 7.3, 12.4, 12.6, 8.7, L, rot=('z', -22.5, (8.8, 10.6, 8)))
    p.box(7.3, 10.0, 3.6, 8.7, 12.6, 7.2, liston(lis, 7), rot=('x', -22.5, (8, 10.6, 7.2)))
    p.box(7.3, 10.0, 8.8, 8.7, 12.6, 12.4, liston(lis, 7), rot=('x', 22.5, (8, 10.6, 8.8)))
    return p


def escribir():
    os.makedirs(os.path.join(A, 'models', 'block'), exist_ok=True)
    for c in COLORES:
        p = caja(c)
        models, img, alfa = kit.build(p)
        nombre = f'caja_regalo_{c}'
        img.save(os.path.join(A, 'textures', 'block', nombre + '.png'))
        tex = {'0': f'navidad:block/{nombre}', 'particle': f'navidad:block/{nombre}'}
        for sufijo, els in (('', models['cuerpo'] + models['tapa']), ('_cuerpo', models['cuerpo']), ('_tapa', models['tapa'])):
            j = {'credit': 'Navidad de Axelis - caja de regalo', 'textures': tex, 'elements': els, 'display': kit.DISPLAY}
            with open(os.path.join(A, 'models', 'block', nombre + sufijo + '.json'), 'w') as fh:
                json.dump(j, fh, indent=1)
    with open(os.path.join(A, 'models', 'block', 'caja_regalo_abriendo.json'), 'w') as fh:
        json.dump({'textures': {'particle': 'navidad:block/caja_regalo_0'}}, fh, indent=1)
    # el estado de bloque: dirección x llena x color x abriendo
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    v = {}
    for f, y in rot.items():
        for llena in ('true', 'false'):
            for c in COLORES:
                for ab in ('true', 'false'):
                    if ab == 'true':
                        m = {'model': 'navidad:block/caja_regalo_abriendo'}
                    elif llena == 'true':
                        m = {'model': f'navidad:block/caja_regalo_{c}'}
                    else:
                        m = {'model': 'navidad:block/caja_regalo_vacia'}
                    if y and ab == 'false':
                        m['y'] = y
                    v[f'abriendo={ab},color={c},facing={f},llena={llena}'] = m
    with open(os.path.join(A, 'blockstates', 'caja_regalo.json'), 'w') as fh:
        json.dump({'variants': v}, fh, indent=1)
    with open(os.path.join(A, 'models', 'item', 'caja_regalo.json'), 'w') as fh:
        json.dump({'parent': 'navidad:block/caja_regalo_0'}, fh, indent=1)


if __name__ == '__main__':
    escribir()
    print('cajas de regalo listas')
