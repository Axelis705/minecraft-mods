#!/usr/bin/env python3
"""Pieles (64x64, formato de jugador, con las casillas de expresiones de la Persona del Core) de la gente de Navidad de Axelis:
Santa, la Señora Claus, tres duendes y el hombre de jengibre. Usa persona() de axelis_core/herramientas/pieles.py.
Uso: python3 herramientas/pieles.py"""
import os, sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas'))
import pieles as P  # noqa: E402

E = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad', 'textures', 'entity')
ROJO, BLANCO, NEGRO, ORO, VERDE = (196, 30, 42), (244, 244, 240), (28, 26, 28), (232, 192, 64), (44, 150, 64)

GENTE = {
    'santa': dict(piel=0, pelo=(238, 238, 236), ojos=(70, 110, 170), ropa=ROJO, ropa2=BLANCO, pantalon=ROJO, zapatos=NEGRO, barba=True,
                  botones=True, cinturon=NEGRO, puno=BLANCO, manga=11),
    'senora_claus': dict(piel=0, pelo=(224, 224, 228), ojos=(90, 130, 90), ropa=(176, 28, 40), ropa2=BLANCO, pantalon=(176, 28, 40),
                         zapatos=(60, 30, 26), largo=False, delantal=True, falda=(176, 28, 40), cuello=True, manga=11, puno=BLANCO,
                         flequillo=True),
    'duende_juguetero': dict(piel=0, pelo=(150, 84, 36), ojos=(60, 120, 60), ropa=VERDE, ropa2=ROJO, pantalon=ROJO, zapatos=(30, 110, 50),
                             cinturon=ORO, puno=ROJO, manga=11, flequillo=True, botones=True),
    'duende_sastre': dict(piel=1, pelo=(200, 96, 40), ojos=(90, 70, 40), ropa=(196, 40, 52), ropa2=VERDE, pantalon=VERDE,
                          zapatos=(150, 30, 40), tirantes=True, manga=11, puno=VERDE, flequillo=True),
    'duende_cocinero': dict(piel=2, pelo=(60, 40, 30), ojos=(70, 50, 30), ropa=VERDE, ropa2=BLANCO, pantalon=(150, 30, 40), zapatos=(30, 110, 50),
                            delantal=True, manga=4, flequillo=True),
    # Don Abeto, el del puesto de árboles: camisa de leñador (roja con negro), pantalón de mezclilla, botas y barba café
    'pinero': dict(piel=0, pelo=(110, 72, 40), ojos=(70, 100, 60), ropa=(176, 32, 36), ropa2=(30, 26, 28), pantalon=(52, 76, 120),
                   zapatos=(84, 52, 30), barba=True, cinturon=(70, 46, 26), puno=(30, 26, 28), manga=11, botones=True),
    # Ramiro, el duende viajero: abrigo café de viaje con bufanda roja, mochila (tirantes) y botas
    'duende_viajero': dict(piel=2, pelo=(170, 110, 50), ojos=(60, 110, 150), ropa=(122, 84, 46), ropa2=(196, 30, 42), pantalon=(46, 90, 52),
                           zapatos=(70, 44, 26), tirantes=True, cinturon=(60, 40, 22), puno=(196, 30, 42), manga=11, flequillo=True),
    # el duende ladrón: ropa oscura (para esconderse de noche), bufanda morada y botas puntiagudas moradas
    'duende_ladron': dict(piel=1, pelo=(40, 30, 28), ojos=(150, 60, 170), ropa=(34, 62, 44), ropa2=(112, 44, 132), pantalon=(26, 26, 32),
                          zapatos=(112, 44, 132), cinturon=(70, 50, 30), puno=(112, 44, 132), manga=11, flequillo=True, tirantes=True),
}


def jengibre():
    s = P.Skin(77)
    galleta, borde, glase = (184, 116, 58), (142, 84, 38), (246, 246, 242)

    def cab(f, x, y, w, h):
        if f == 'front':
            if y == 3 and x in (2, 5):
                return (26, 20, 18)  # ojos de chocolate
            if y == 5 and 2 <= x <= 5:
                return glase if x in (2, 5) or y == 5 else galleta
            if y == 6 and 3 <= x <= 4:
                return glase  # sonrisa
        if f == 'top' and (x in (0, 7) or y in (0, 7)):
            return borde
        if f in ('right', 'left', 'back') and (y == 0):
            return borde
        return galleta

    def cuerpo(f, x, y, w, h):
        if f == 'front':
            if x in (3, 4) and y in (2, 5, 8):
                return (210, 40, 40) if y != 5 else (40, 160, 60)  # botones de gomita
            if y in (11,) and 1 <= x <= 6:
                return glase
        return galleta

    def extremidad(f, x, y, w, h):
        if f == 'bottom':
            return borde
        if f != 'top' and y in (9, 10) and (x + y) % 2 == 0:
            return glase  # zigzag de glaseado
        return galleta

    s.box(0, 0, 8, 8, 8, cab)
    s.box(16, 16, 8, 12, 4, cuerpo)
    for (u, v) in ((40, 16), (32, 48), (0, 16), (16, 48)):
        s.box(u, v, 4, 12, 4, extremidad)
    # expresiones: (24,0) boca abierta, (32,0) ojos felices, (56,0) sonrisa grande
    for x in range(2, 6):
        s.px(24 + x, 6, (70, 30, 20), 0)
    for x0 in (2, 5):
        s.px(32 + x0, 3, galleta, 0)
        s.px(32 + x0, 2, (26, 20, 18), 0)
    for x in range(2, 6):
        s.px(56 + x, 6, glase, 0)
    s.save(os.path.join(E, 'hombre_jengibre.png'))


if __name__ == '__main__':
    for i, (n, p) in enumerate(GENTE.items()):
        s = P.persona(900 + i, p)
        if n == 'santa':
            # saco: cinturón negro con hebilla dorada a la mitad y orilla de peluche blanco abajo
            for x in range(16, 40):
                if s.get(x, 26)[3]:
                    s.px(x, 26, NEGRO)
                for y in (29, 30):
                    if s.get(x, y)[3]:
                        s.px(x, y, BLANCO)
            for x in (23, 24):
                s.px(x, 26, ORO)
            # barbota y bigote: la mitad de abajo de la cara blanca (menos la boca), y a los lados
            for y in range(4, 8):
                for x in range(8):
                    if y == 4 and x not in (0, 3, 4, 7):
                        continue
                    if y == 6 and x in (3, 4):
                        continue
                    s.px(8 + x, 8 + y, BLANCO if (x + y) % 3 else (226, 226, 222))
            for y in range(3, 8):
                for x in range(4):
                    s.px(0 + 4 + x, 8 + y, BLANCO)   # lado derecho, mitad de enfrente
                    s.px(16 + x, 8 + y, BLANCO)      # lado izquierdo, mitad de enfrente
            # expresiones con barba: boca abierta y sonrisa solo en la boca
        if n == 'pinero':
            # camisa de cuadros de leñador: líneas negras cada 4 pixeles sobre lo rojo
            for y in range(16, 64):
                for x in range(16, 64):
                    c_ = s.get(x, y)
                    if c_[3] and abs(c_[0] - 176) < 30 and c_[1] < 70 and c_[2] < 70 and (x % 4 == 0 or y % 4 == 0):
                        s.px(x, y, (44, 20, 22) if (x % 4 == 0) != (y % 4 == 0) else (24, 18, 20))
        s.save(os.path.join(E, n + '.png'))
    jengibre()
    print('ok')
