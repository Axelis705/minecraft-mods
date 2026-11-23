"""La piel de Nilo para la pesca (64x64, formato de jugador): la del pescador de muestra del Core con más detalle: chaleco con
bolsillos, pañuelo rojo, botas de hule y una mosca de pesca en la gorra.

Uso (desde la raíz del repo): python3 pesca_coleccion/herramientas/nilo.py
"""
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, "..", "..", "axelis_core", "herramientas"))
import pieles  # noqa: E402

OUT = os.path.join(AQUI, "..", "src", "main", "resources", "assets", "pesca_coleccion", "textures", "entity", "nilo.png")


def main():
    p = dict(pieles.PERSONAS["nilo"])
    s = pieles.persona(907, p)
    px = s.px
    # pañuelo rojo al cuello (arriba del pecho, en V)
    rojo = (196, 52, 44)
    for x in range(20, 28):
        px(x, 20, rojo, noise=8)
    for x in (21, 22, 25, 26):
        px(x, 21, rojo, noise=8)
    px(23, 21, (150, 36, 30))
    px(24, 21, (150, 36, 30))
    px(23, 22, rojo)
    px(24, 22, rojo)
    # bolsillos del impermeable (con su tapa)
    tapa, bolsa = (176, 140, 30), (214, 176, 40)
    for (bx, by) in ((20, 26), (25, 26)):
        for x in range(bx, bx + 3):
            px(x, by, tapa, noise=4)
            px(x, by + 1, bolsa, noise=4)
            px(x, by + 2, bolsa, noise=4)
        px(bx + 1, by, (120, 96, 20))
    # botones al centro
    for y in (23, 26, 29):
        px(23, y, (60, 44, 24), noise=0)
        px(24, y, (60, 44, 24), noise=0)
    # botas de hule verde oscuro (las 5 filas de abajo de cada pierna, todos los lados)
    bota, suela = (58, 86, 58), (30, 40, 30)
    for (u, v) in ((0, 16), (16, 48)):
        for y in range(v + 4 + 7, v + 4 + 12):
            for x in range(u, u + 16):
                px(x, y, suela if y == v + 4 + 11 else bota, noise=6)
        for x in range(u + 8, u + 12):
            for y in range(v, v + 4):
                px(x, y, suela, noise=4)
        # borde de la bota
        for x in range(u, u + 16):
            px(x, v + 4 + 7, (78, 112, 74), noise=4)
    # mosca de pesca en la gorra (capa del sombrero, lado derecho del frente)
    px(46, 10, (240, 80, 60), noise=0)
    px(47, 10, (250, 210, 70), noise=0)
    px(46, 11, (200, 200, 210), noise=0)
    s.save(OUT)
    print("piel de Nilo:", os.path.relpath(OUT))


if __name__ == "__main__":
    main()
