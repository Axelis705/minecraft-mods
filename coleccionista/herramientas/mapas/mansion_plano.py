#!/usr/bin/env python3
"""Arma el plano de texto de la mansión (capítulo 2) para {@code Casa}: tres pisos dibujados letra por letra.

El plano que usa el juego es el .txt que escribe esto (src/main/resources/data/coleccionista/planos/mansion.txt). Se puede editar a mano
(cada letra es una columna de bloques; ver Casa.java); si se edita a mano, ya no se vuelve a correr esto (o se pasan los cambios aquí).

Después de cambiarlo: bash tests/run.sh (revisa que todo se pueda caminar) y las vistas con herramientas/mapas/vistas.sh.

Uso: python3 herramientas/mapas/mansion_plano.py
"""
import os

AQUI = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(AQUI, '..', '..', 'src', 'main', 'resources', 'data', 'coleccionista', 'planos', 'mansion.txt')
X0, X1, Z0, Z1 = 0, 54, -18, 61


class Piso:
    def __init__(self, y, alto):
        self.y, self.alto = y, alto
        self.g = {}

    def put(self, x, z, c):
        self.g[(x, z)] = c

    def get(self, x, z):
        return self.g.get((x, z), ' ')

    def cuarto(self, x0, z0, x1, z1, letra):
        """El cuarto (interior con su letra) y sus muros alrededor (sin pisar el interior de otros cuartos)."""
        for x in range(x0 - 1, x1 + 2):
            for z in range(z0 - 1, z1 + 2):
                dentro = x0 <= x <= x1 and z0 <= z <= z1
                if dentro:
                    self.put(x, z, letra)
                elif self.get(x, z) == ' ':
                    self.put(x, z, '#')

    def linea(self, x0, z0, x1, z1, c):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                self.put(x, z, c)

    def texto(self):
        filas = []
        for z in range(Z0, Z1 + 1):
            filas.append(''.join(self.get(x, z) for x in range(X0, X1 + 1)).rstrip())
        return filas


LEYENDA = """\
# El plano de la mansión del Coleccionista (capítulo 2, "La mascota"). Lo lee Casa.java; lo arma herramientas/mapas/mansion_plano.py.
# Cada piso es un dibujo: cada letra, una columna de bloques (x a la derecha, z hacia abajo). Ver Casa.java para las letras.

# ---- los cuartos (minúsculas)
estilo f nombre=Vestíbulo piso=coleccionista:marmol_negro|coleccionista:marmol_crema pared=coleccionista:papel_tapiz luz=10
estilo g nombre=Galería piso=coleccionista:parquet pared=coleccionista:papel_tapiz luz=9
estilo c nombre=Comedor piso=coleccionista:parquet pared=coleccionista:papel_tapiz luz=9
estilo k nombre=Cocina piso=coleccionista:linoleo|coleccionista:azulejo pared=coleccionista:azulejo luz=9
estilo d nombre=Despensa piso=coleccionista:tablon pared=coleccionista:panel_madera luz=5
estilo s nombre=Servicio piso=coleccionista:tablon pared=coleccionista:yeso luz=7
estilo m nombre=Música piso=coleccionista:parquet pared=coleccionista:acolchado luz=9
estilo b nombre=Biblioteca piso=coleccionista:parquet pared=coleccionista:panel_madera luz=8
estilo e nombre=Estudio piso=coleccionista:parquet alfombra=coleccionista:alfombra_roja pared=coleccionista:panel_madera luz=7
estilo h nombre=Pasillo piso=coleccionista:parquet pared=coleccionista:papel_tapiz luz=7
estilo i nombre=Invernadero piso=coleccionista:losa_piedra pared=coleccionista:yeso luz=10
estilo a nombre=Lavandería piso=coleccionista:azulejo_sucio pared=coleccionista:azulejo luz=8
estilo q nombre=Calderas piso=coleccionista:concreto_piso pared=coleccionista:ladrillo_oscuro luz=5
estilo l nombre=Billar piso=coleccionista:parquet pared=coleccionista:panel_madera luz=8
estilo p nombre=Retratos piso=coleccionista:parquet pared=coleccionista:papel_tapiz luz=8
estilo y nombre=Trofeos piso=coleccionista:marmol_crema pared=coleccionista:panel_madera luz=8
estilo r nombre=Recámara piso=coleccionista:parquet pared=coleccionista:papel_tapiz luz=8
estilo z nombre=Baño piso=coleccionista:azulejo pared=coleccionista:azulejo luz=9
estilo u nombre=Muñecas piso=coleccionista:parquet pared=coleccionista:acolchado luz=8
estilo n nombre=Bebé piso=coleccionista:parquet alfombra=coleccionista:alfombra_cafe pared=coleccionista:papel_tapiz luz=7
estilo o nombre=Costura piso=coleccionista:parquet pared=coleccionista:papel_tapiz luz=8
estilo t nombre=Ático piso=coleccionista:tablon pared=coleccionista:tablon techo=coleccionista:viga_madera luz=5

# ---- los muebles (mayúsculas y signos)
mueble L minecraft:bookshelf alto=3
mueble M coleccionista:mesa
mueble S coleccionista:silla mira=mesa
mueble B minecraft:red_bed cama
mueble A minecraft:white_bed cama
mueble T coleccionista:mesa_tapete
mueble C coleccionista:candelabro doble
mueble H coleccionista:sillon doble
mueble R coleccionista:reloj
mueble V coleccionista:vitrina doble
mueble E coleccionista:espejo
mueble K coleccionista:cajon
mueble O coleccionista:barril
mueble U coleccionista:muneca
mueble Q coleccionista:retrete
mueble N coleccionista:lavabo
mueble Y minecraft:cauldron[-]
mueble G minecraft:smoker[facing=south,lit=false]
mueble I coleccionista:atril
mueble P minecraft:note_block[-]
mueble J minecraft:jukebox[-]
mueble F minecraft:potted_fern[-]
mueble Z minecraft:azalea_leaves[persistent=true] alto=2
mueble x coleccionista:craneo
mueble * coleccionista:telarana
mueble = coleccionista:tubo
mueble $ coleccionista:metal_negro alto=3
mueble ! coleccionista:farol
"""


def planta_baja():
    f = Piso(0, 6)
    # el vestíbulo (doble altura) con sus dos escaleras junto a la jaula
    f.cuarto(17, 10, 40, 33, 'f')
    f.linea(36, 14, 38, 19, '^')
    f.linea(36, 24, 38, 29, 'v')
    # el muro del oeste es del pasillo de servicio (con sus puertas) y el del este, en medio, es la reja de la jaula
    f.linea(16, 17, 16, 26, ' ')
    f.linea(41, 18, 41, 25, ' ')
    for z in range(14, 30):
        for x in range(42, 53):
            f.put(x, z, ' ')
    # muebles del vestíbulo: sillones junto a las puertas, vitrinas bajo las galerías, candelabros, el reloj
    for x in (20, 24, 28, 32):
        f.put(x, 10, 'V')
        f.put(x, 33, 'V')
    f.put(17, 12, 'H')
    f.put(17, 31, 'H')
    f.put(17, 15, 'C')
    f.put(17, 28, 'C')
    f.put(34, 10, 'R')
    f.put(33, 21, 'C')
    f.put(33, 22, 'C')
    f.put(40, 14, 'C')
    f.put(40, 29, 'C')
    f.put(20, 21, '1')      # la entrada (donde aparecen)
    f.put(27, 21, '2')      # el centro

    # ---- la fila del norte: comedor, cocina, despensa y cuarto de servicio
    f.cuarto(17, 0, 30, 8, 'c')
    f.cuarto(32, 0, 43, 8, 'k')
    f.cuarto(45, 0, 49, 4, 'd')
    f.cuarto(45, 6, 52, 11, 's')
    f.put(23, 9, 'D')
    f.put(37, 9, 'D')
    f.put(31, 4, 'D')
    f.put(44, 2, 'D')
    f.put(44, 8, 'D')
    f.put(47, 5, 'D')
    # comedor: la mesa larga con sillas, candelabros, un armario-escondite en cada esquina del norte
    f.linea(20, 4, 27, 4, 'M')
    f.linea(20, 3, 27, 3, 'S')
    f.linea(20, 5, 27, 5, 'S')
    f.put(19, 4, 'S')
    f.put(28, 4, 'S')
    f.put(17, 8, 'C')
    f.put(30, 8, 'C')
    closet(f, 17, 0, 'v')
    closet(f, 30, 0, 'v')
    f.put(24, 0, 'R')
    # cocina: estufas y alacenas al norte, mesa al centro, barriles
    f.linea(33, 0, 35, 0, 'G')
    f.linea(36, 0, 40, 0, 'K')
    f.linea(36, 4, 39, 4, 'M')
    f.put(36, 3, 'S')
    f.put(39, 5, 'S')
    f.put(43, 6, 'O')
    f.put(43, 7, 'O')
    f.put(32, 8, 'O')
    # despensa: barriles y un escondite en el fondo
    f.linea(45, 0, 49, 0, 'O')
    f.put(49, 2, 'O')
    f.put(45, 4, 'O')
    f.put(49, 4, '@')
    # servicio: tubos, barriles, un catre y escondite detrás de la cortina
    f.put(52, 6, '=')
    f.put(52, 7, '=')
    f.linea(48, 11, 50, 11, 'O')
    f.put(45, 11, 'A')
    f.put(46, 11, 'A')
    f.put(52, 10, '@')
    f.put(51, 10, '%')

    # ---- la fila del sur: música, biblioteca y estudio
    f.cuarto(17, 35, 30, 43, 'm')
    f.cuarto(32, 35, 44, 43, 'b')
    f.cuarto(46, 35, 52, 43, 'e')
    f.put(23, 34, 'D')
    f.put(37, 34, 'D')
    f.put(31, 39, 'D')
    f.put(45, 39, 'D')
    # música: el "piano" (bloques de notas), atriles, sillas, tocadiscos, cortinas-escondite en el muro del oeste
    f.linea(21, 38, 23, 38, 'P')
    f.put(22, 39, 'S')
    f.linea(26, 37, 28, 37, 'I')
    f.linea(26, 39, 28, 39, 'S')
    f.put(30, 43, 'J')
    f.put(17, 35, 'C')
    f.put(30, 35, 'C')
    nicho(f, 17, 41, 'x-')
    # biblioteca: hileras de libreros (con huecos para pasar y esconderse), mesa de lectura y sillón
    for z in (37, 40):
        for x in range(33, 44):
            if x not in (37, 38):
                f.put(x, z, 'L')
    f.put(36, 42, 'M')
    f.put(35, 42, 'S')
    f.put(41, 42, 'H')
    f.put(44, 35, '@')
    f.put(32, 43, '@')
    f.put(44, 43, 'I')
    # estudio: escritorio, cráneos, vitrinas, el sillón del Coleccionista
    f.put(49, 38, 'M')
    f.put(49, 37, 'H')
    f.put(48, 38, 'x')
    f.linea(46, 43, 52, 43, 'V')
    f.put(52, 35, '@')

    # ---- el ala del norte: pasillo largo, invernadero, lavandería y calderas (con la escalera de servicio al este)
    f.cuarto(6, -3, 52, -2, 'h')
    f.cuarto(6, -17, 18, -5, 'i')
    f.cuarto(20, -17, 31, -5, 'a')
    f.cuarto(33, -17, 52, -5, 'q')
    f.linea(45, -3, 50, -3, '>')
    for x, z in ((12, -4), (25, -4), (40, -4), (20, -1), (40, -1)):
        f.put(x, z, 'D')
    # invernadero: macetas y matas (para esconderse entre ellas)
    for x in range(8, 18, 3):
        for z in range(-15, -6, 3):
            f.put(x, z, 'Z')
            f.put(x + 1, z, 'F')
    f.put(6, -17, '@')
    f.put(18, -17, '@')
    # lavandería: tinas, sábanas colgadas (cortinas: detrás se esconde uno), canastas
    f.linea(21, -17, 25, -17, 'Y')
    for x in (23, 27):
        f.put(x, -11, '%')
        f.put(x, -12, '@')
    f.put(31, -17, 'O')
    f.put(31, -16, 'O')
    # calderas: calderas de metal con tubos, oscuro; escondites entre ellas
    for x in (36, 42, 48):
        f.linea(x, -15, x + 1, -13, '$')
        f.put(x, -12, '=')
    f.put(39, -17, '@')
    f.put(45, -17, '@')
    f.put(51, -17, '@')
    f.put(33, -5, 'O')

    # ---- el ala del sur: pasillo, billar, retratos y trofeos (con su escalera de servicio)
    f.cuarto(6, 45, 52, 46, 'h')
    f.cuarto(6, 48, 18, 60, 'l')
    f.cuarto(20, 48, 31, 60, 'p')
    f.cuarto(33, 48, 52, 60, 'y')
    f.linea(45, 45, 50, 45, '>')
    for x, z in ((12, 47), (25, 47), (40, 47), (20, 44), (40, 44)):
        f.put(x, z, 'D')
    # billar: dos mesas, sillones, cortina-escondite
    f.linea(10, 52, 12, 53, 'T')
    f.linea(10, 56, 12, 57, 'T')
    f.put(6, 60, 'H')
    f.put(18, 60, 'R')
    f.put(18, 50, '@')
    f.put(17, 50, '%')
    # retratos: vitrinas con muñecas en hilera y escondites detrás
    for x in (22, 25, 28):
        f.put(x, 52, 'V')
        f.put(x, 56, 'V')
    f.put(20, 60, '@')
    f.put(31, 60, '@')
    f.put(31, 48, 'U')
    # trofeos: vitrinas, cráneos, candelabros; escondites en las esquinas
    for x in range(36, 51, 4):
        f.put(x, 52, 'V')
        f.put(x, 57, 'V')
    f.put(33, 60, '@')
    f.put(52, 60, '@')
    f.put(52, 48, 'C')
    f.put(33, 48, 'x')
    return f


def closet(f, x, z, abre):
    """Un clóset de 1x1 en la esquina (x, z) del cuarto: el escondite, su puerta del lado {abre} ('v' abajo, '^' arriba, '>' '<') y muros."""
    f.put(x, z, '@')
    dx, dz = {'v': (0, 1), '^': (0, -1), '>': (1, 0), '<': (-1, 0)}[abre]
    f.put(x + dx, z + dz, 'D')
    # los lados de la puerta y del clóset
    for s in (-1, 1):
        if dz != 0:
            for zz in (z, z + dz):
                if f.get(x + s, zz) not in ('#', 'X'):
                    f.put(x + s, zz, '#')
        else:
            for xx in (x, x + dx):
                if f.get(xx, z + s) not in ('#', 'X'):
                    f.put(xx, z + s, '#')


def nicho(f, x, z, lado):
    """Un nicho con cortina en el muro: la cortina en (x, z) y el hueco en el muro de al lado ('x-': hacia el oeste...)."""
    d = {'x-': (-1, 0), 'x+': (1, 0), 'z-': (0, -1), 'z+': (0, 1)}[lado]
    f.put(x, z, '%')
    f.put(x + d[0], z + d[1], '@')


def planta_alta():
    f = Piso(7, 5)
    # el vestíbulo: hueco en medio (se ve abajo) y las galerías al norte y al sur con barandal (sin barandal donde llegan las escaleras)
    f.cuarto(17, 10, 40, 33, 'g')
    for x in range(17, 41):
        for z in range(14, 30):
            f.put(x, z, '~')
    for x in range(17, 41):
        if not 36 <= x <= 38:
            f.put(x, 13, ':')
            f.put(x, 30, ':')
    f.linea(16, 17, 16, 26, '#')
    f.linea(41, 14, 41, 29, '#')
    for z in range(14, 30):
        for x in range(42, 53):
            f.put(x, z, ' ')

    # ---- la fila del norte: recámara principal, baño, cuarto de las muñecas
    f.cuarto(17, 0, 30, 8, 'r')
    f.cuarto(32, 0, 37, 8, 'z')
    f.cuarto(39, 0, 52, 8, 'u')
    f.put(23, 9, 'D')
    f.put(34, 9, 'D')
    f.put(39, 9, 'D')
    f.put(31, 5, 'D')
    # recámara: la cama grande, buró, tocador, dos clósets
    f.put(23, 0, 'B')
    f.put(23, 1, 'B')
    f.put(24, 0, 'B')
    f.put(24, 1, 'B')
    f.put(22, 0, 'K')
    f.put(25, 0, 'K')
    f.put(29, 8, 'E')
    f.put(30, 8, 'S')
    closet(f, 17, 0, 'v')
    closet(f, 30, 0, 'v')
    f.put(17, 8, 'H')
    # baño: la tina (con cortina: ahí se esconde uno), retrete, lavabo, espejo
    f.put(32, 0, 'Y')
    f.put(33, 0, 'Y')
    f.put(32, 2, '%')
    f.put(33, 2, '%')
    f.put(32, 1, '@')
    f.put(37, 0, 'Q')
    f.put(37, 3, 'N')
    f.put(37, 4, 'E')
    # muñecas: repisas (vitrinas) con muñecas, una casita, escondites
    for x in range(40, 52, 3):
        f.put(x, 0, 'V')
    for x in range(41, 52, 4):
        f.put(x, 4, 'U')
    f.put(52, 8, '@')
    f.put(39, 0, '@')

    # ---- la fila del sur: cuarto del bebé, costura, estudio secreto
    f.cuarto(17, 35, 30, 43, 'n')
    f.cuarto(32, 35, 44, 43, 'o')
    f.cuarto(46, 35, 52, 43, 'e')
    f.put(23, 34, 'D')
    f.put(37, 34, 'D')
    f.put(45, 39, 'D')
    # bebé: cuna (cama blanca), muñecas, un baúl, clóset
    f.put(23, 43, 'A')
    f.put(23, 42, 'A')
    f.linea(26, 43, 29, 43, 'U')
    f.put(17, 43, 'K')
    closet(f, 30, 35, 'v')
    f.put(17, 35, 'R')
    # costura: mesas de trabajo, telas (cortinas) y maniquíes (muñecas)
    f.linea(34, 38, 36, 38, 'M')
    f.put(35, 39, 'S')
    f.linea(40, 38, 42, 38, 'M')
    f.put(41, 39, 'S')
    for x in (34, 38, 42):
        f.put(x, 43, '%')
        f.put(x, 42, ' ')
    f.put(34, 42, '@')
    f.put(38, 42, '@')
    f.put(42, 42, '@')
    f.put(44, 35, 'U')
    # estudio secreto
    f.put(49, 38, 'M')
    f.put(49, 39, 'S')
    f.put(52, 43, '@')
    f.linea(46, 35, 52, 35, 'L')

    # ---- ala del norte (arriba): pasillo con la escalera que llega del piso de abajo y la que sube al ático
    f.cuarto(6, -3, 52, -2, 'h')
    f.linea(45, -3, 50, -3, '~')
    f.linea(8, -3, 12, -3, '<')
    f.cuarto(6, -17, 18, -5, 'r')
    f.cuarto(20, -17, 31, -5, 's')
    f.cuarto(33, -17, 52, -5, 'r')
    for x, z in ((15, -4), (25, -4), (40, -4), (20, -1), (45, -1)):
        f.put(x, z, 'D')
    # recámara de huéspedes
    f.put(12, -17, 'B')
    f.put(12, -16, 'B')
    f.put(6, -5, 'K')
    closet(f, 6, -17, '>')
    closet(f, 18, -17, '<')
    # cuarto de los criados: catres en hilera
    for x in (21, 24, 27, 30):
        f.put(x, -17, 'A')
        f.put(x, -16, 'A')
    f.put(31, -5, '@')
    f.put(20, -5, 'O')
    # otra recámara grande (la vieja, empolvada)
    f.put(42, -17, 'B')
    f.put(42, -16, 'B')
    f.put(43, -17, 'B')
    f.put(43, -16, 'B')
    f.put(33, -17, 'H')
    f.put(52, -17, '@')
    f.put(52, -5, '@')
    f.put(33, -5, '*')

    # ---- ala del sur (arriba): pasillo con su escalera de servicio y la del ático
    f.cuarto(6, 45, 52, 46, 'h')
    f.linea(45, 45, 50, 45, '~')
    f.linea(8, 46, 12, 46, '<')
    f.cuarto(6, 48, 18, 60, 'r')
    f.cuarto(20, 48, 31, 60, 'u')
    f.cuarto(33, 48, 52, 60, 'p')
    for x, z in ((15, 47), (25, 47), (40, 47), (20, 44), (40, 44)):
        f.put(x, z, 'D')
    f.put(12, 60, 'B')
    f.put(12, 59, 'B')
    closet(f, 6, 60, '>')
    closet(f, 18, 60, '<')
    f.linea(21, 60, 30, 60, 'U')
    f.put(20, 48, '@')
    f.put(31, 54, '@')
    for x in range(35, 51, 5):
        f.put(x, 52, 'V')
        f.put(x, 56, 'V')
    f.put(33, 60, '@')
    f.put(52, 60, '@')
    return f


def atico():
    f = Piso(13, 3)
    # dos áticos (norte y sur), bajos y polvosos, con laberintos de cajas (barriles) y telarañas
    for (z0, z1, zesc) in ((-17, -2, -3), (45, 60, 46)):
        f.cuarto(6, z0, 52, z1, 't')
        # el hueco de la escalera que llega (x 8..12 en el pasillo de abajo)
        f.linea(8, zesc, 12, zesc, '~')
        # pilas de cajas: muros bajitos que hacen pasillos (con huecos para pasar)
        for x in range(16, 52, 6):
            for z in range(z0 + 1, z1, 1):
                if (z - z0) % 5 != 2:
                    f.put(x, z, 'O')
        for x in range(16, 52, 6):
            f.put(x + 3, z0 + 1, '@')
            f.put(x + 3, z1 - 1, '@')
            f.put(x + 2, z0, '*')
        f.put(52, z0, 'U')
        f.put(52, z1, 'U')
    return f


def main():
    pisos = [planta_baja(), planta_alta(), atico()]
    out = [LEYENDA,
           'punto 1 man.entrada 270',
           'punto 2 man.centro 270',
           'marcas man.esc.', '']
    for f in pisos:
        out.append('piso %d alto %d origen %d %d' % (f.y, f.alto, X0, Z0))
        out.extend(f.texto())
        out.append('fin')
        out.append('')
    os.makedirs(os.path.dirname(SALIDA), exist_ok=True)
    with open(SALIDA, 'w', encoding='utf-8') as fh:
        fh.write('\n'.join(out))
    print('plano:', os.path.relpath(SALIDA))


if __name__ == '__main__':
    main()
