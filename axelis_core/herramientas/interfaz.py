"""Texturas de la interfaz estilo "Minecraft pulido": fondos de panel (madera, piedra, pergamino, noche, mar) que se repiten sin
cortes, el botón en grises (se tiñe con el color del tema) y la ranura de objeto.

python3 herramientas/interfaz.py
Cada fondo es de 128x128 y se dibuja al doble (un pixel de textura = 2 de la interfaz, como el fondo de tierra de Minecraft).
"""
import os
import random

from PIL import Image

A = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "axelis_core", "textures", "gui")
N = 128


def clamp(v):
    return max(0, min(255, int(v)))


def tono(c, k):
    """Aclara (k > 0) u oscurece (k < 0) un color."""
    if k >= 0:
        return tuple(clamp(x + (255 - x) * k) for x in c)
    return tuple(clamp(x * (1 + k)) for x in c)


def ruido(r, a):
    return r.uniform(-a, a)


def madera():
    """Tablones oscuros (como el roble oscuro): tablas de 16 px de alto con vetas, juntas y clavos."""
    r = random.Random(11)
    im = Image.new("RGB", (N, N))
    base = (74, 52, 33)
    for y in range(N):
        tabla = y // 16
        fila = y % 16
        # cada tabla con su tono y su desfase de veta
        rt = random.Random(tabla * 97 + 3)
        kt = rt.uniform(-0.10, 0.08)
        corte = rt.randrange(0, N)
        for x in range(N):
            k = kt
            # veta: ondas a lo largo de la tabla
            v = ((x * 3 + fila * 11 + tabla * 37) % 23)
            if v < 2:
                k -= 0.10
            elif v < 4:
                k -= 0.05
            k += ruido(r, 0.035)
            c = tono(base, k)
            if fila == 0:
                c = tono(base, -0.45)  # junta oscura
            elif fila == 1:
                c = tono(base, 0.10 + kt)  # filo con luz
            elif fila == 15:
                c = tono(base, -0.25 + kt)
            # corte entre tablas (una por tabla, en lugares distintos)
            if (x - corte) % N == 0 and 0 < fila:
                c = tono(base, -0.45)
            elif (x - corte) % N == 1 and 0 < fila:
                c = tono(base, 0.08 + kt)
            im.putpixel((x, y), c)
        # clavos junto al corte
        if fila == 0:
            for dx in (-3, 3):
                for yy in (y + 4, y + 11):
                    xx = (corte + dx) % N
                    im.putpixel((xx, yy % N), (40, 30, 22))
                    im.putpixel(((xx + 1) % N, (yy - 1) % N), (150, 128, 96))
    return im


def piedra():
    """Ladrillos de piedra oscura (como pizarra profunda) con musgo de vez en cuando."""
    r = random.Random(23)
    im = Image.new("RGB", (N, N))
    base = (58, 60, 66)
    for y in range(N):
        fila = y // 16
        dy = y % 16
        off = 0 if fila % 2 == 0 else 16
        for x in range(N):
            bx = (x + off) % N
            ladrillo = (bx // 32, fila)
            rl = random.Random(ladrillo[0] * 131 + ladrillo[1] * 7)
            k = rl.uniform(-0.08, 0.08) + ruido(r, 0.06)
            ex = bx % 32
            if dy == 0 or ex == 0:
                c = tono(base, -0.55)
            elif dy == 1 or ex == 1:
                c = tono(base, 0.18 + k)
            elif dy == 15 or ex == 31:
                c = tono(base, -0.30 + k)
            else:
                c = tono(base, k)
                # grietas finas
                if rl.random() < 0.004:
                    c = tono(base, -0.35)
            im.putpixel((x, y), c)
    # musgo en algunas juntas
    for _ in range(26):
        x, y = r.randrange(N), (r.randrange(N // 16)) * 16
        for i in range(r.randrange(2, 6)):
            xx, yy = (x + i) % N, (y + r.choice((0, 1, -1))) % N
            im.putpixel((xx, yy), r.choice([(70, 104, 46), (88, 126, 54), (58, 88, 40)]))
    return im


def pergamino():
    """Papel claro con fibras, manchitas de tinta y bordes de hoja (manchas suaves que no se notan al repetir)."""
    r = random.Random(5)
    im = Image.new("RGB", (N, N))
    base = (232, 214, 172)
    for y in range(N):
        for x in range(N):
            k = ruido(r, 0.025)
            # fibras horizontales
            if (x * 7 + y * 13) % 41 == 0:
                k -= 0.05
            if (x * 5 + y * 3) % 59 == 0:
                k += 0.04
            im.putpixel((x, y), tono(base, k))
    # manchas suaves
    for _ in range(9):
        cx, cy, rad = r.randrange(N), r.randrange(N), r.randrange(4, 11)
        for y in range(-rad, rad + 1):
            for x in range(-rad, rad + 1):
                d = (x * x + y * y) / (rad * rad)
                if d <= 1:
                    xx, yy = (cx + x) % N, (cy + y) % N
                    c = im.getpixel((xx, yy))
                    im.putpixel((xx, yy), tono(c, -0.05 * (1 - d)))
    return im


def noche():
    """Azul de noche con un tejido en diagonal muy suave y algunas estrellas (las que titilan se dibujan aparte)."""
    r = random.Random(31)
    im = Image.new("RGB", (N, N))
    base = (30, 36, 56)
    for y in range(N):
        for x in range(N):
            k = ruido(r, 0.03)
            if (x + y) % 8 == 0:
                k += 0.05
            if (x - y) % 8 == 0:
                k -= 0.04
            im.putpixel((x, y), tono(base, k))
    for _ in range(18):
        x, y = r.randrange(N), r.randrange(N)
        im.putpixel((x, y), tono(base, 0.35))
    return im


def mar():
    """Azul verdoso como el prismarino, con olas suaves."""
    r = random.Random(41)
    im = Image.new("RGB", (N, N))
    base = (22, 70, 84)
    for y in range(N):
        for x in range(N):
            k = ruido(r, 0.03)
            ola = (y + int(3 * __import__("math").sin(x / 128 * 6.2832 * 3))) % 12
            if ola == 0:
                k += 0.10
            elif ola == 1:
                k += 0.04
            elif ola == 6:
                k -= 0.06
            cuadro = ((x // 16) + (y // 16)) % 2
            k += 0.025 if cuadro else -0.01
            im.putpixel((x, y), tono(base, k))
    return im


def boton():
    """Botón en grises de 200x20 (se tiñe con el color del tema): borde negro con esquinas recortadas, luz arriba, sombra abajo y un
    poco de ruido, como los de Minecraft."""
    r = random.Random(7)
    w, h = 200, 20
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y in range(h):
        for x in range(w):
            borde = x == 0 or y == 0 or x == w - 1 or y == h - 1
            esquina = (x in (0, w - 1)) and (y in (0, h - 1))
            if esquina:
                continue
            if borde:
                im.putpixel((x, y), (0, 0, 0, 255))
                continue
            v = 160 + ruido(r, 7)
            if y == 1 or x == 1:
                v = 236
            elif y >= h - 3 or x == w - 2:
                v = 92 if y >= h - 3 else 110
            elif y == 2:
                v = 196 + ruido(r, 4)
            im.putpixel((x, y), (clamp(v), clamp(v), clamp(v), 255))
    return im


def ranura():
    """Ranura de objeto de 18x18 como la del inventario (en grises, se tiñe)."""
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for y in range(18):
        for x in range(18):
            if x == 0 or y == 0:
                c = 55
            elif x == 17 or y == 17:
                c = 255
            else:
                c = 139
            im.putpixel((x, y), (c, c, c, 255))
    return im


def main():
    os.makedirs(A, exist_ok=True)
    for nombre, f in (("madera", madera), ("piedra", piedra), ("pergamino", pergamino), ("noche", noche), ("mar", mar)):
        f().save(os.path.join(A, "fondo_" + nombre + ".png"))
    boton().save(os.path.join(A, "boton.png"))
    ranura().save(os.path.join(A, "ranura.png"))
    print("listo:", A)


if __name__ == "__main__":
    main()
