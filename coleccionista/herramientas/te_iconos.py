#!/usr/bin/env python3
"""Los dibujos de piedra, papel o tijera para la pantalla del salón de té (capítulo 5): una mano de porcelana (como las manos gigantes de
la mesa), en pixel art de 32x32 aumentado a 64x64. Escribe textures/gui/te_piedra.png, te_papel.png y te_tijera.png.

Uso: python3 herramientas/te_iconos.py
"""
import os

from PIL import Image, ImageDraw

AQUI = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'coleccionista', 'textures', 'gui')
PORC = (236, 232, 226, 255)
SOMBRA = (176, 168, 160, 255)
LINEA = (40, 28, 30, 255)
UNA = (150, 26, 38, 255)
ENCAJE = (244, 239, 228, 255)
ORO = (199, 154, 50, 255)


def lienzo():
    return Image.new('RGBA', (32, 32), (0, 0, 0, 0))


def rect(d, x0, y0, x1, y1, relleno=PORC, borde=LINEA):
    d.rectangle([x0, y0, x1, y1], fill=borde)
    d.rectangle([x0 + 1, y0 + 1, x1 - 1, y1 - 1], fill=relleno)


def puno(d):
    """El puño (visto de lado, los nudillos a la derecha): la base de la mano, el puño de encaje y el aro dorado."""
    rect(d, 2, 13, 8, 25, ENCAJE)               # el puño de encaje
    d.line([3, 14, 3, 24], fill=ORO)
    rect(d, 8, 11, 20, 27)                      # la palma


def dedos_cerrados(d, cuales):
    for k in cuales:
        y = 12 + k * 4
        rect(d, 19, y, 25, y + 4)               # el dedo doblado (los nudillos)
        d.point([23, y + 2], fill=SOMBRA)


def dedo_estirado(d, y, largo=11, dy=0):
    """Un dedo estirado desde los nudillos (19, y..y+4); con dy sube (negativo) o baja (positivo) hacia la punta."""
    x0, x1 = 19, 19 + largo
    ya, yb = y + 2, y + 2 + dy
    if dy == 0:
        d.rectangle([x0, y, x1, y + 4], fill=LINEA)
        d.rectangle([x0, y + 1, x1 - 1, y + 3], fill=PORC)
        d.rectangle([x1 - 3, y + 1, x1 - 2, y + 2], fill=UNA)
        return
    # en diagonal, columna por columna (pixel art): borde arriba y abajo, porcelana en medio
    for i in range(largo + 1):
        x = x0 + i
        yc = round(ya + dy * i / largo)
        d.line([x, yc - 2, x, yc + 2], fill=LINEA)
        if i < largo:
            d.line([x, yc - 1, x, yc + 1], fill=PORC)
    d.rectangle([x1 - 2, yb - 1, x1 - 1, yb], fill=UNA)


def pulgar(d, arriba=False):
    if arriba:
        rect(d, 12, 5, 16, 12)                  # el pulgar parado
        d.rectangle([13, 6, 14, 7], fill=UNA)
    else:
        rect(d, 12, 9, 22, 13)                  # el pulgar cruzado sobre los dedos
        d.rectangle([20, 10, 21, 11], fill=UNA)


def guardar(img, nombre):
    img = img.resize((64, 64), Image.NEAREST)
    os.makedirs(A, exist_ok=True)
    img.save(os.path.join(A, nombre + '.png'))


def piedra():
    img = lienzo()
    d = ImageDraw.Draw(img)
    puno(d)
    dedos_cerrados(d, range(4))
    pulgar(d)
    guardar(img, 'te_piedra')


def papel():
    img = lienzo()
    d = ImageDraw.Draw(img)
    puno(d)
    for k in range(4):
        dedo_estirado(d, 11 + k * 4, (10, 11, 10, 8)[k])
    pulgar(d, arriba=True)
    guardar(img, 'te_papel')


def tijera():
    img = lienzo()
    d = ImageDraw.Draw(img)
    puno(d)
    dedos_cerrados(d, (2, 3))
    dedo_estirado(d, 12, 11, -5)
    dedo_estirado(d, 15, 11, 4)
    pulgar(d)
    guardar(img, 'te_tijera')


if __name__ == '__main__':
    piedra()
    papel()
    tijera()
    print('listo')
