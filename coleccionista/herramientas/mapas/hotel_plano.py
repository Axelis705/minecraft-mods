#!/usr/bin/env python3
"""El plano del hotel del Coleccionista (minijuego "el hotel", al principio del capítulo 1), piso por piso, hecho a mano.

Diez pisos de 51 x 51 alrededor de un patio central abierto (con barandal) que se ve desde todos lados. En cada piso un pasillo con
alfombra da la vuelta al patio; afuera del pasillo, los cuartos (tres por lado y una suite en cada esquina, a la que solo se llega por los
cuartos de al lado). Las escaleras están en las esquinas del pasillo, pero entre cada par de pisos solo hay dos (en esquinas opuestas;
las otras están tapadas con escombros): para bajar hay que cruzar el piso. Cada piso tiene su tema (vestíbulo, salón de baile,
habitaciones, lavandería, spa, oficinas, inundado, suites, ático).

Marcas (las pone Casa.java; el resto lo arma Hotel.java):
  &  hotel.cab.N     ascensor de llegada (adentro; mira al pasillo)        $  hotel.serv.N   puerta de servicio (mira al clóset)
  *  hotel.rata.N    nido de ratas                                         +  hotel.tubo.N   un tubo en el piso (para defenderse)
  !  hotel.pua.N     piso con púas                                         =  hotel.cand.N   candelabro que se cae
  ,  hotel.charco.N  charco con electricidad                               ?  hotel.nota.N   lugar para una pista
  ;  hotel.botiquin.N un botiquín

Escribe src/main/resources/data/coleccionista/planos/hotel.txt y revisa (sin Minecraft) que desde cada ascensor se llegue a cada puerta
de servicio. Uso: python3 herramientas/mapas/hotel_plano.py
"""
import os
import random
from collections import deque

AQUI = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(AQUI, '..', '..', 'src', 'main', 'resources', 'data', 'coleccionista', 'planos', 'hotel.txt')
N = 51
PISOS = 10
ALTO = 4
C = 'coleccionista:'

ENCABEZADO = f"""# El hotel del Coleccionista (lo arma herramientas/mapas/hotel_plano.py; lo lee Casa.java y lo termina Hotel.java).
# Cada letra es una columna de bloques. Diez pisos de {N} x {N}, de {ALTO} de alto por dentro.
estilo c nombre=Pasillo piso={C}parquet|{C}madera_pulida alfombra={C}alfombra_roja pared={C}papel_tapiz luz=10
estilo h nombre=Habitación piso={C}parquet pared={C}papel_verde luz=9
estilo j nombre=Habitación piso={C}parquet pared={C}papel_rosa luz=9
estilo i nombre=Habitación piso={C}losa_tablon pared={C}papel_azul luz=9
estilo b nombre=Baño piso={C}azulejo|{C}azulejo_sucio pared={C}azulejo zoclo={C}azulejo_remate luz=8
estilo s nombre=Suite piso={C}marmol_crema|{C}parquet alfombra={C}alfombra_cafe pared={C}papel_dorado luz=10
estilo a nombre=Vestíbulo piso={C}marmol_negro|{C}marmol_crema pared={C}papel_tapiz zoclo={C}marmol_negro luz=12
estilo r nombre=Salón piso={C}parquet|{C}losa_marmol pared={C}papel_dorado zoclo={C}marmol_negro luz=11
estilo q nombre=Cocina piso={C}azulejo|{C}azulejo_sucio pared={C}azulejo_sucio luz=10
estilo l nombre=Lavandería piso={C}linoleo pared={C}azulejo_sucio zoclo={C}concreto luz=9
estilo o nombre=Oficina piso={C}linoleo_verde pared={C}panel_madera luz=9
estilo e nombre=Servicio piso={C}concreto_piso pared={C}concreto zoclo={C}concreto techo={C}concreto luz=7
estilo k nombre=Clóset piso={C}concreto_piso pared={C}concreto zoclo={C}concreto techo={C}concreto luz=5
estilo p nombre=Spa piso={C}azulejo pared={C}azulejo zoclo={C}azulejo_remate luz=9
estilo m nombre=Inundado piso={C}concreto_humedo|{C}lodo pared={C}papel_verde luz=8
estilo t nombre=Ático piso={C}tablon|{C}losa_tablon pared={C}viga_madera zoclo={C}tablon techo={C}viga_madera luz=7
estilo u nombre=Quemado piso={C}losa_tablon|{C}brasas pared={C}ladrillo_oscuro zoclo={C}ladrillo_oscuro luz=6
mueble B {C}cama_antigua cama
mueble R {C}ropero doble
mueble K {C}cajon encima={C}lampara_mesa[facing={{mira}}]
mueble M {C}mesa
mueble S {C}silla mira=mesa
mueble A {C}sillon doble
mueble T {C}tina
mueble I {C}retrete
mueble V {C}lavabo
mueble E {C}espejo y=2
mueble P {C}planta doble
mueble F {C}retrato_dama y=2
mueble G {C}retrato_caballero y=2
mueble H {C}retrato_nina y=2
mueble C {C}candelabro doble
mueble J {C}maleta
mueble O {C}papeles
mueble U {C}mancha
mueble Q {C}telarana y=3
mueble Y {C}grava alto=2
mueble Z {C}tablon alto=3
mueble N {C}maniqui doble
mueble L {C}archivero doble
mueble Ñ {C}estante_archivo doble
mueble ¡ {C}lavabo
mueble ¿ {C}reloj_pie doble
mueble Ç {C}piano_pared doble
mueble Ö {C}silla_ruedas
mueble Ü {C}camilla
mueble Ä {C}maquina_coser
mueble Æ {C}barril
marca & hotel.cab. lejos=c
marca $ hotel.serv. puerta hacia=k
marca * hotel.rata.
marca + hotel.tubo.
marca ! hotel.pua.
marca = hotel.cand.
marca , hotel.charco.
marca ? hotel.nota.
marca ; hotel.botiquin.
"""
# (las letras con acento son muebles de más: Casa solo pide que sean mayúsculas definidas)

BLOQUEA = set('#XW') | set('BRKMSATIVEPCJYZNLÑ¡¿ÇÖÜÄÆ')   # lo que no se camina (las marcas y estilos sí)
NO_BLOQUEA = set('OUQFGH')


def grid():
    return [[' '] * N for _ in range(N)]


def s(g, x, z, c):
    if 0 <= x < N and 0 <= z < N:
        g[z][x] = c


def en(g, x, z):
    return g[z][x] if 0 <= x < N and 0 <= z < N else ' '


def fill(g, x0, z0, x1, z1, c):
    for z in range(min(z0, z1), max(z0, z1) + 1):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            s(g, x, z, c)


# --- los cuatro lados: (u, d) -> (x, z). u corre a lo largo del lado (15..35), d es la profundidad desde el muro de afuera (1..13);
#     el muro del pasillo está en d = 14 y el pasillo en d = 15..18
def lado(nombre):
    return {
        'N': lambda u, d: (u, d),
        'S': lambda u, d: (N - 1 - u, N - 1 - d),
        'O': lambda u, d: (d, N - 1 - u),
        'E': lambda u, d: (N - 1 - d, u),
    }[nombre]


LADOS = 'NESO'
CUARTOS = [(15, 20), (22, 28), (30, 35)]   # los tres cuartos de cada lado (u0..u1); muros en 21 y 29


def esqueleto(k):
    g = grid()
    fill(g, 0, 0, N - 1, N - 1, 'X')
    for i in range(3, N - 3, 4):
        for x, z in ((i, 0), (i, N - 1), (0, i), (N - 1, i)):
            s(g, x, z, 'W')
    fill(g, 1, 1, N - 2, N - 2, '#')
    fill(g, 15, 15, 35, 35, 'c')
    if k == 1:
        fill(g, 19, 19, 31, 31, 'a')
    else:
        fill(g, 19, 19, 31, 31, ':')
        fill(g, 20, 20, 30, 30, '~')
    return g


def cuarto(g, L, u0, u1, estilo, rs, tipo='normal', puerta=True, bano=True):
    """Un cuarto de hotel en el lado L, de u0 a u1: recámara (d 5..13) y baño (d 1..3) con su puerta; puerta al pasillo en d 14."""
    T = lado(L)
    for u in range(u0, u1 + 1):
        for d in range(1, 14):
            x, z = T(u, d)
            if bano and d <= 3:
                s(g, x, z, 'b')
            elif bano and d == 4:
                s(g, x, z, '#')
            else:
                s(g, x, z, estilo)
    if bano:
        x, z = T(u0 + 1, 4)
        s(g, x, z, 'D')
        # el baño: tina, retrete y lavabo
        s(g, *T(u1, 1), 'I')
        s(g, *T(u1, 3), 'V')
        if u1 - u0 >= 4:
            s(g, *T(u0 + 3, 1), 'T')
    if puerta:
        s(g, *T(u0 + 1, 14), 'D')
    # la recámara
    y = 6
    if tipo in ('normal', 'revuelto'):
        s(g, *T(u0, y + 3), 'B')
        s(g, *T(u0 + 1, y + 3), 'B')
        s(g, *T(u0, y + 2), 'K')
        s(g, *T(u1, 5), 'R')
        s(g, *T(u1, 11), 'M')
        s(g, *T(u1 - 1, 11), 'S')
        s(g, *T(u0 + 2, 5), rs.choice('FGHE'))
    if tipo == 'revuelto':
        for _ in range(5):
            s(g, *T(rs.randint(u0, u1), rs.randint(6, 13)), rs.choice('OOUJ'))
    if tipo == 'maniquies':
        for u in range(u0, u1 + 1, 2):
            s(g, *T(u, 8), 'N')
            s(g, *T(u + 1 if u + 1 <= u1 else u, 11), 'N')
    if tipo == 'quemado':
        for _ in range(4):
            s(g, *T(rs.randint(u0, u1), rs.randint(5, 13)), 'Y')


def suite(g, esq, estilo, rs):
    """La suite de una esquina (13 x 13): sala, recámara y baño grande."""
    x0, z0 = {'NO': (1, 1), 'NE': (37, 1), 'SE': (37, 37), 'SO': (1, 37)}[esq]
    fill(g, x0, z0, x0 + 12, z0 + 12, estilo)
    # un muro que parte la suite en sala y recámara, con paso
    fill(g, x0 + 6, z0, x0 + 6, z0 + 12, '#')
    s(g, x0 + 6, z0 + 6, '|')
    s(g, x0 + 6, z0 + 7, '|')
    s(g, x0 + 2, z0 + 2, 'B')
    s(g, x0 + 3, z0 + 2, 'B')
    s(g, x0 + 9, z0 + 9, 'A')
    s(g, x0 + 10, z0 + 9, 'A')
    s(g, x0 + 9, z0 + 3, 'P')
    s(g, x0 + 11, z0 + 11, 'C')
    s(g, x0 + 1, z0 + 11, 'R')
    s(g, x0 + 4, z0 + 6, rs.choice('FGH'))


def conectar_suites(g):
    """Cada suite se abre a los dos cuartos de al lado (no al pasillo)."""
    for x, z in ((14, 9), (9, 14), (36, 9), (41, 14), (36, 41), (41, 36), (14, 41), (9, 36)):
        s(g, x, z, 'D')


def escaleras(g, k, esquinas):
    """Las escaleras que suben de este piso al de arriba (en las esquinas que tocan); las otras esquinas, con escombros."""
    for esq in 'NO NE SE SO'.split():
        if esq in esquinas:
            if esq == 'NO':
                for x in range(15, 19):
                    s(g, x, 15, '>')
            elif esq == 'NE':
                for z in range(15, 19):
                    s(g, 35, z, 'v')
            elif esq == 'SE':
                for x in range(35, 31, -1):
                    s(g, x, 35, '<')
            else:
                for z in range(35, 31, -1):
                    s(g, 15, z, '^')
        elif k < PISOS:
            # escombros donde iría (para que se note que hubo escalera)
            x, z = {'NO': (16, 16), 'NE': (34, 16), 'SE': (34, 34), 'SO': (16, 34)}[esq]
            s(g, x, z, 'Y')


def nicho_cabina(g, L, u):
    """Un ascensor de llegada en el muro del pasillo: adentro en d = 13, la puerta en d = 14 (la pone Hotel.java)."""
    T = lado(L)
    s(g, *T(u, 13), '&')
    s(g, *T(u - 1, 13), '#')
    s(g, *T(u + 1, 13), '#')
    s(g, *T(u, 12), '#')
    s(g, *T(u, 14), '|')       # el hueco de la puerta (Hotel.java pone ahí la puerta del ascensor)


def closet(g, L, u):
    """Una puerta de servicio en el muro del pasillo (d = 14) y su clóset de 2 x 2 atrás (d 12..13)."""
    T = lado(L)
    for uu in (u, u + 1):
        for d in (12, 13):
            s(g, *T(uu, d), 'k')
    for uu in (u - 1, u + 2):
        for d in (11, 12, 13):
            s(g, *T(uu, d), '#')
    for uu in (u, u + 1):
        s(g, *T(uu, 11), '#')
    s(g, *T(u, 14), '$')
    s(g, *T(u + 1, 14), '#')


def poner(g, L, u, d, c):
    s(g, *lado(L)(u, d), c)


# ---------------------------------------------------------------- los pisos
def piso(k):
    rs = random.Random(1000 + k)
    g = esqueleto(k)
    estilos_hab = 'hji'
    # las escaleras: entre k y k+1 solo dos esquinas opuestas (alternan)
    if k < PISOS:
        escaleras(g, k, ('NO', 'SE') if k % 2 == 1 else ('NE', 'SO'))
    tema = {1: 'vestibulo', 2: 'salon', 3: 'hab', 4: 'lavanderia', 5: 'hab_quemado', 6: 'spa', 7: 'oficinas', 8: 'inundado',
            9: 'suites', 10: 'atico'}[k]
    for i, L in enumerate(LADOS):
        for j, (u0, u1) in enumerate(CUARTOS):
            est = estilos_hab[(k + i + j) % 3]
            tipo = rs.choice(['normal', 'normal', 'revuelto', 'normal'])
            cuarto(g, L, u0, u1, est, rs, tipo)
    for esq in ('NO', 'NE', 'SE', 'SO'):
        suite(g, esq, 's', rs)
    conectar_suites(g)
    # puertas entre cuartos de al lado (para rodear: en d 8)
    for L in LADOS:
        if rs.random() < 0.6:
            poner(g, L, 21, 9, 'D')
        if rs.random() < 0.6:
            poner(g, L, 29, 9, 'D')

    if tema == 'vestibulo':
        # el vestíbulo: el patio es el piso de mármol con la fuente seca; recepción al norte, comedor al sur, cocina al este
        fill(g, 23, 23, 27, 27, 'a')
        for x, z in ((24, 24), (26, 24), (24, 26), (26, 26)):
            s(g, x, z, 'Æ')
        s(g, 25, 25, 'C')
        for L, est in (('S', 'r'), ('E', 'q')):
            for u0, u1 in CUARTOS:
                cuarto(g, L, u0, u1, est, rs, 'nada', bano=False)
        for u in range(16, 35, 3):
            poner(g, 'S', u, 8, 'M')
            poner(g, 'S', u, 7, 'S')
        for u in range(16, 35, 4):
            poner(g, 'E', u, 6, 'M')
            poner(g, 'E', u, 10, '¡')
        # la recepción: mostrador frente al patio
        for x in range(22, 29):
            s(g, x, 18, 'M')
        s(g, 25, 17, '¿')
        for x in (20, 30):
            s(g, x, 20, 'A')
    elif tema == 'salon':
        # el salón de baile ocupa todo el lado sur (sin muros) con candelabros que caen; sala de música al norte
        for L in ('S',):
            for u in range(15, 36):
                for d in range(1, 14):
                    poner(g, L, u, d, 'r')
            poner(g, L, 25, 14, '|')
            poner(g, L, 18, 14, 'D')
            poner(g, L, 32, 14, 'D')
            for u, d in ((19, 5), (25, 8), (31, 5), (22, 11), (28, 11)):
                poner(g, L, u, d, '=')
            for u in range(16, 35, 6):
                poner(g, L, u, 2, 'C')
        poner(g, 'N', 25, 9, 'Ç')
        poner(g, 'N', 24, 12, 'S')
    elif tema == 'lavanderia':
        for u0, u1 in CUARTOS:
            cuarto(g, 'E', u0, u1, 'l', rs, 'nada', bano=False)
        for u in range(16, 35, 2):
            poner(g, 'E', u, 3, 'Ä' if u % 4 == 0 else 'Æ')
            poner(g, 'E', u, 9, 'J')
        # el ducto de la ropa: un hueco que baja al piso de abajo (cae sobre sábanas)
        poner(g, 'E', 25, 6, '~')
        poner(g, 'E', 26, 6, '~')
    elif tema == 'hab_quemado':
        for u0, u1 in CUARTOS:
            cuarto(g, 'O', u0, u1, 'u', rs, 'quemado')
        # escombros en el pasillo: montones que lo tapan casi todo, dejando el hueco de un lado y del otro (zigzag)
        for i, u in enumerate((19, 23, 27, 31)):
            for d in ((15, 16, 17) if i % 2 == 0 else (16, 17, 18)):
                poner(g, 'O', u, d, 'Y')
    elif tema == 'spa':
        # el spa: la alberca vacía (un hueco al piso de abajo con charcos) y vestidores
        for u in range(15, 36):
            for d in range(1, 14):
                poner(g, 'S', u, d, 'p')
        for u in range(18, 33):
            for d in range(4, 11):
                poner(g, 'S', u, d, ',' if (u + d) % 7 == 0 else 'p')
        poner(g, 'S', 25, 14, '|')
        poner(g, 'S', 19, 14, 'D')
        poner(g, 'S', 31, 14, 'D')
        for u in (16, 34):
            for d in (3, 6, 9, 12):
                poner(g, 'S', u, d, 'Ü')
    elif tema == 'oficinas':
        for L in ('N', 'O'):
            for u0, u1 in CUARTOS:
                cuarto(g, L, u0, u1, 'o', rs, 'nada', bano=False)
                for u in range(u0, u1 + 1, 2):
                    poner(g, L, u, 2, 'L')
                    poner(g, L, u, 3, 'Ñ' if u % 4 == 1 else 'L')
                poner(g, L, (u0 + u1) // 2, 8, 'M')
                poner(g, L, (u0 + u1) // 2, 9, 'S')
                poner(g, L, u0 + 1, 11, '?')
                poner(g, L, u1 - 1, 6, 'O')
    elif tema == 'inundado':
        for L in ('E', 'S'):
            for u0, u1 in CUARTOS:
                cuarto(g, L, u0, u1, 'm', rs, 'revuelto')
                poner(g, L, (u0 + u1) // 2, 10, ',')
                poner(g, L, u0 + 1, 7, ',')
        for u in (20, 26, 31):
            poner(g, 'E', u, 16, ',')
    elif tema == 'suites':
        for L in LADOS:
            for u0, u1 in CUARTOS:
                cuarto(g, L, u0, u1, 's', rs, 'normal')
        for L in ('N', 'S'):
            poner(g, L, 18, 10, 'N')
            poner(g, L, 32, 10, 'N')
    elif tema == 'atico':
        for L in LADOS:
            for u0, u1 in CUARTOS:
                cuarto(g, L, u0, u1, 't', rs, 'nada', bano=False)
                for u in range(u0, u1 + 1, 3):
                    poner(g, L, u, 3, rs.choice('JÆZ'))
                poner(g, L, (u0 + u1) // 2, 9, '*')
        for esq in ('NO', 'NE', 'SE', 'SO'):
            suite(g, esq, 't', rs)
        conectar_suites(g)
    elif tema == 'hab':
        for u0, u1 in CUARTOS:
            cuarto(g, 'N', u0, u1, 'h', rs, 'maniquies')
    # ---- los que van en todos los pisos
    # 3 ascensores de llegada (en lados distintos según el piso)
    lados_cab = [LADOS[(k + i) % 4] for i in range(3)]
    for i, L in enumerate(lados_cab):
        nicho_cabina(g, L, 21 if i % 2 == 0 else 29)
    # dos pisos tienen uno más (32 en total, como la torre de la sala grande)
    if k in (1, 6):
        nicho_cabina(g, LADOS[(k + 3) % 4], 25)
    # 2 puertas de servicio (en los otros lugares)
    lados_serv = [LADOS[(k + 3) % 4], LADOS[(k + 1) % 4]]
    for i, L in enumerate(lados_serv):
        closet(g, L, 19 if i == 0 else 33)       # (en u 18 la puerta quedaría frente al primer escalón)
    # nidos de ratas, tubos, púas, pistas y un botiquín (en el pasillo y los cuartos)
    for i in range(2):
        L = LADOS[(k + i * 2) % 4]
        poner(g, L, 25, 12 - i, '*')
    for i in range(3):
        L = LADOS[(k + i) % 4]
        poner(g, L, 16 + i * 7, 17, '+')
    poner(g, LADOS[(k + 2) % 4], 24, 16, '!')
    poner(g, LADOS[(k + 2) % 4], 24, 17, '!')
    poner(g, LADOS[(k + 1) % 4], 17, 7, '?')
    poner(g, LADOS[(k + 3) % 4], 33, 9, '?')
    poner(g, LADOS[k % 4], 33, 12, ';')
    despejar(g)
    return g


# ---------------------------------------------------------------- revisión (sin Minecraft)
def caminable(c):
    return c not in BLOQUEA and c not in ' ~'


PASA = set('D|$&^v<>')
MURO = set('#XW')


def estilo_de(g, x, z):
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
        c = en(g, x + dx, z + dz)
        if c.islower() and c.isalpha():
            return c
    return 'c'


def componente(g, inicio):
    vis = {inicio}
    q = deque([inicio])
    while q:
        a = q.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            b = (a[0] + dx, a[1] + dz)
            cb = en(g, *b)
            if b not in vis and (caminable(cb) or cb in PASA):
                vis.add(b)
                q.append(b)
    return vis


def despejar(g):
    """Que ningún mueble tape una puerta ni deje una parte del piso encerrada (quita el mueble que estorba)."""
    for z in range(N):
        for x in range(N):
            if g[z][x] in 'D$|':
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    c = en(g, x + dx, z + dz)
                    if c in BLOQUEA and c not in MURO:
                        s(g, x + dx, z + dz, estilo_de(g, x + dx, z + dz))
    for _ in range(200):
        main = componente(g, (25, 16))
        aislado = {(x, z) for z in range(N) for x in range(N)
                   if (x, z) not in main and (caminable(g[z][x]) or g[z][x] in PASA)}
        if not aislado:
            return
        quitado = False
        for z in range(N):
            for x in range(N):
                c = g[z][x]
                if c in BLOQUEA and c not in MURO:
                    vec = [(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))]
                    if any(v in main for v in vec) and any(v in aislado for v in vec):
                        s(g, x, z, estilo_de(g, x, z))
                        quitado = True
                        break
            if quitado:
                break
        if not quitado:
            return


def revisar(pisos):
    """Desde cada ascensor, ¿se llega a cada puerta de servicio? (escaleras: de un piso al de arriba; huecos: al de abajo)."""
    cabs, servs = [], []
    for k, g in enumerate(pisos, 1):
        for z in range(N):
            for x in range(N):
                if g[z][x] == '&':
                    cabs.append((k, x, z))
                elif g[z][x] == '$':
                    servs.append((k, x, z))

    def vecinos(k, x, z):
        g = pisos[k - 1]
        c = g[z][x]
        out = []
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            n = en(g, nx, nz)
            if n == 'D' or n == '|' or n == '$' or caminable(n) or n in '^v<>':
                out.append((k, nx, nz))
        # la escalera: de su último escalón al piso de arriba (y de ahí se puede bajar)
        if c in '^v<>':
            dx, dz = {'>': (1, 0), '<': (-1, 0), 'v': (0, 1), '^': (0, -1)}[c]
            if en(g, x + dx, z + dz) != c and k < PISOS:
                out.append((k + 1, x + dx, z + dz))
        # arriba de una escalera del piso de abajo: se baja
        if k > 1:
            gb = pisos[k - 2]
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                cb = en(gb, x - dx, z - dz)
                if cb in '^v<>' and {'>': (1, 0), '<': (-1, 0), 'v': (0, 1), '^': (0, -1)}[cb] == (dx, dz) \
                        and en(gb, x, z) != cb:
                    out.append((k - 1, x - dx, z - dz))
        # un hueco al piso de abajo (ducto, alberca)
        if k > 1:
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if en(g, x + dx, z + dz) == '~' and not (19 <= x + dx <= 31 and 19 <= z + dz <= 31):
                    out.append((k - 1, x + dx, z + dz))
        return out

    malos = []
    for cab in cabs:
        vistos = {cab}
        q = deque([cab])
        while q:
            a = q.popleft()
            for b in vecinos(*a):
                if b not in vistos:
                    vistos.add(b)
                    q.append(b)
        falta = [sv for sv in servs if sv not in vistos]
        if falta:
            malos.append((cab, falta))
    return cabs, servs, malos


def main():
    pisos = [piso(k) for k in range(1, PISOS + 1)]
    cabs, servs, malos = revisar(pisos)
    with open(SALIDA, 'w', encoding='utf-8') as f:
        f.write(ENCABEZADO)
        for k, g in enumerate(pisos, 1):
            f.write(f'\n# piso {k}\npiso {(k - 1) * (ALTO + 1)} alto {ALTO} origen 0 0\n')
            for fila in g:
                f.write(''.join(fila).rstrip() + '\n')
            f.write('fin\n')
    print(f'{len(cabs)} ascensores, {len(servs)} puertas de servicio; sin camino: {len(malos)}')
    for cab, falta in malos[:5]:
        print('  desde', cab, 'no llega a', falta[:4])


if __name__ == '__main__':
    main()
