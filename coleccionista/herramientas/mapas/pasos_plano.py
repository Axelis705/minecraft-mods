#!/usr/bin/env python3
"""Arma los planos de texto de los pasillos que unen una sala con la siguiente (hacia adelante): uno por capítulo, cada uno con su tema.

Cada pasillo sale de la puerta de salida de una sala (en su muro del este) y llega a la puerta de atrás del pasillo de servicio de la sala
siguiente. Va en su propia zona (paso_<sala a la que llega>), de x 0 a ANCHO-1; nunca pisa las zonas de al lado (las de las salas ponen
su puerta y su pedacito de muro abierto, ver Salida.java).

Escribe src/main/resources/data/coleccionista/planos/paso_<sala>.txt (los lee Casa.java). Uso: python3 herramientas/mapas/pasos_plano.py
"""
import os

AQUI = os.path.dirname(os.path.abspath(__file__))
DIR = os.path.join(AQUI, '..', '..', 'src', 'main', 'resources', 'data', 'coleccionista', 'planos')

# (zona a la que llega, x del mundo donde empieza (relativo), ancho, z de la puerta de salida de la anterior, tema)
PASOS = [
    ('caja', 775, 15, 8, 'cajas'),
    ('sorpresa', 852, 18, 8, 'payasos'),
    ('te', 934, 16, 39, 'merienda'),
    ('piano', 1014, 16, 4, 'espejos'),
    ('veintiuno', 1094, 16, 37, 'cartas'),
]
Z_LLEGA = 21

TEMAS = {
    # estilo del pasillo y cómo se decora la parte larga (las dos orillas, alternando)
    'cajas': ('estilo a nombre=Taller_de_cajas_de_música piso=coleccionista:parquet alfombra=coleccionista:alfombra_roja '
              'pared=coleccionista:acolchado luz=11', 'VCKC'),
    'payasos': ('estilo a nombre=Pasillo_de_los_payasos piso=coleccionista:madera_pulida|coleccionista:tablon pared=coleccionista:telon '
                'techo=coleccionista:marco_luz_amarilla luz=10', 'zOxz'),
    'merienda': ('estilo a nombre=La_merienda piso=coleccionista:marmol_crema|coleccionista:marmol_negro pared=coleccionista:azulejo '
                 'luz=11', 'MYMO'),
    'espejos': ('estilo a nombre=Galería_de_espejos piso=coleccionista:parquet pared=coleccionista:marmol_crema '
                'zoclo=coleccionista:marmol_negro luz=12', 'ECEK'),
    'cartas': ('estilo a nombre=El_pasillo_de_las_cartas piso=coleccionista:marmol_negro|coleccionista:tapete_rojo '
               'pared=coleccionista:panel_madera luz=10', 'THSH'),
}

LEYENDA = """\
# Un pasillo hacia adelante (lo arma herramientas/mapas/pasos_plano.py; lo lee Casa.java). Cada letra, una columna de bloques.
mueble V coleccionista:vitrina doble
mueble C coleccionista:candelabro doble
mueble K coleccionista:cajon encima=coleccionista:muneca[facing={mira}]
mueble O coleccionista:barril
mueble x coleccionista:craneo
mueble z coleccionista:zapatos
mueble M coleccionista:mesa encima=coleccionista:copa
mueble Y minecraft:cauldron[-]
mueble E coleccionista:espejo y=2
mueble T coleccionista:mesa_tapete
mueble H coleccionista:sillon doble
mueble S coleccionista:estandarte_rojo doble
"""


def plano(zona, ancho, zin, tema):
    g = {}

    def put(x, z, c):
        if 0 <= x < ancho:
            g[(x, z)] = c

    def cuarto(x0, z0, x1, z1):
        for x in range(x0 - 1, x1 + 2):
            for z in range(z0 - 1, z1 + 2):
                if x0 <= x <= x1 and z0 <= z <= z1:
                    put(x, z, 'a')
                elif g.get((x, z), ' ') == ' ':
                    put(x, z, '#')

    xm = ancho // 2 - 1
    z0, z1 = min(zin, Z_LLEGA), max(zin, Z_LLEGA) + 1
    # tramo 1: de la puerta hacia el este; tramo 2: el largo (3 de ancho, hacia el norte o el sur); tramo 3: al este, hasta la llegada
    cuarto(0, zin, xm + 2, zin + 1)
    cuarto(xm, z0, xm + 2, z1)
    cuarto(xm, Z_LLEGA, ancho - 1, Z_LLEGA + 1)
    # a lo largo del tramo 2: los adornos del tema en las orillas (el centro queda libre para caminar)
    deco = TEMAS[tema][1]
    i = 0
    for z in range(z0 + 1, z1):
        if zin <= z <= zin + 1 or Z_LLEGA <= z <= Z_LLEGA + 1 or (z - z0) % 2 == 1:
            continue
        lado = xm if i % 2 == 0 else xm + 2
        put(lado, z, deco[i % len(deco)])
        i += 1
    filas = []
    zs = [z for (_, z) in g]
    for z in range(min(zs), max(zs) + 1):
        filas.append(''.join(g.get((x, z), ' ') for x in range(ancho)).rstrip())
    return min(zs), filas


def main():
    os.makedirs(DIR, exist_ok=True)
    for zona, dx, ancho, zin, tema in PASOS:
        zmin, filas = plano(zona, ancho, zin, tema)
        out = [LEYENDA, TEMAS[tema][0], '', 'punto 1 paso_%s.inicio 270' % zona, 'punto 2 paso_%s.codo1 0' % zona,
               'punto 3 paso_%s.codo2 0' % zona, 'punto 4 paso_%s.fin 270' % zona, '',
               '# el pasillo (x del mundo %d..%d; sale en z %d..%d, llega en z %d..%d)' % (dx, dx + ancho - 1, zin, zin + 1, Z_LLEGA,
                                                                                          Z_LLEGA + 1),
               'piso 0 alto 4 origen 0 %d' % zmin]
        # los puntos: el inicio (la primera casilla), los dos codos (al centro del tramo largo) y el final (frente a la llegada); con
        # ellos se marca el camino (Adelante.meta)
        xm = ancho // 2 - 1
        for (x, z, c) in ((0, zin, '1'), (xm + 1, zin, '2'), (xm + 1, Z_LLEGA, '3'), (ancho - 1, Z_LLEGA, '4')):
            f = list(filas[z - zmin].ljust(ancho))
            f[x] = c
            filas[z - zmin] = ''.join(f).rstrip()
        out.extend(filas)
        out.append('fin')
        ruta = os.path.join(DIR, 'paso_%s.txt' % zona)
        with open(ruta, 'w', encoding='utf-8') as fh:
            fh.write('\n'.join(out) + '\n')
        print('plano:', os.path.relpath(ruta))


if __name__ == '__main__':
    main()
