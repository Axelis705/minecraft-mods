"""Genera los dibujos de los peces (16x16, en diagonal como los de Minecraft: cola abajo a la izquierda, cabeza arriba a la derecha).

Cada pez se describe con pocos datos (forma del cuerpo, cola, aletas, colores y dibujo) y aquí se dibuja pixel por pixel con
contorno y sombras. Uso:
    python3 peces.py <carpeta_de_salida> [--hoja hoja.png]
"""
import math
import os
import sys

from PIL import Image

N = 16          # tamaño del dibujo
SS = 6          # submuestras por pixel para decidir qué parte cae en cada pixel


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, k):
    """k>0 aclara, k<0 oscurece."""
    return mix(c, (255, 255, 255), k) if k > 0 else mix(c, (0, 0, 0), -k)


def body_half(u, f):
    """Media altura del cuerpo en la posición u (0 = base de la cola, 1 = punta de la nariz)."""
    if u < 0 or u > 1:
        return 0.0
    shape = f.get("forma", "normal")
    peak = f.get("pico", 0.55)           # dónde es más alto
    nose = f.get("nariz", 0.55)          # qué tan redonda la nariz (más = más roma)
    if u < peak:
        t = u / peak
        h = math.sin(t * math.pi / 2) ** f.get("cintura", 0.8)
        h = 0.32 + 0.68 * h               # la base de la cola no es un punto
    else:
        t = (u - peak) / (1 - peak)
        h = (1 - t ** (1.0 / max(0.15, nose))) ** 0.5 if t < 1 else 0
        h = max(0.0, min(1.0, (1 - t * t) ** nose))
    if shape == "anguila":
        h = 0.55 if 0.02 < u < 0.97 else 0.3
    return h * f.get("alto", 0.2)


def inside_poly(x, y, poly):
    c = False
    j = len(poly) - 1
    for i in range(len(poly)):
        xi, yi = poly[i]
        xj, yj = poly[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi:
            c = not c
        j = i
    return c


def tail_poly(f):
    """La cola en coordenadas (u, v): u < 0 detrás del cuerpo."""
    k = f.get("cola", "horquilla")
    L = f.get("cola_largo", 0.24)
    H = f.get("cola_alto", 0.2)
    b = body_half(0.0, f) * 0.9
    if k == "horquilla":
        return [(0.02, b), (-L, H), (-L * 0.55, 0.0), (-L, -H), (0.02, -b)]
    if k == "redonda":
        pts = [(0.02, b)]
        for i in range(9):
            a = math.pi / 2 - i * math.pi / 8
            pts.append((-L * 0.25 - L * 0.75 * math.cos(a) * 1.0, H * math.sin(a)))
        pts.append((0.02, -b))
        return pts
    if k == "luna":
        return [(0.02, b), (-L * 0.7, H * 1.15), (-L * 0.45, H * 0.4), (-L * 0.3, 0.0), (-L * 0.45, -H * 0.4), (-L * 0.7, -H * 1.15),
                (0.02, -b)]
    if k == "punta":
        return [(0.02, b), (-L * 1.2, 0.0), (0.02, -b)]
    if k == "abanico":
        return [(0.02, b * 0.8), (-L, H), (-L * 1.05, 0.0), (-L, -H), (0.02, -b * 0.8)]
    return []


def fins(f):
    """Aletas de arriba (dorsal), abajo (anal) y del costado (pectoral) en (u, v)."""
    out = []
    d = f.get("dorsal", "normal")
    top = lambda u: body_half(u, f)
    if d == "normal":
        out.append(("aleta", [(0.30, top(0.30) * 0.9), (0.42, top(0.42) + 0.09), (0.62, top(0.62) + 0.03), (0.66, top(0.66) * 0.9)]))
    elif d == "vela":
        out.append(("aleta", [(0.15, top(0.15) * 0.9), (0.25, top(0.25) + 0.18), (0.62, top(0.62) + 0.15), (0.72, top(0.72) * 0.9)]))
    elif d == "espinas":
        pts = [(0.25, top(0.25) * 0.9)]
        for i in range(5):
            u = 0.28 + i * 0.08
            pts += [(u, top(u) + 0.09), (u + 0.04, top(u + 0.04) + 0.02)]
        pts.append((0.70, top(0.70) * 0.9))
        out.append(("aleta", pts))
    elif d == "larga":
        out.append(("aleta", [(0.10, top(0.10) * 0.9), (0.14, top(0.14) + 0.05), (0.80, top(0.80) + 0.04), (0.84, top(0.84) * 0.9)]))
    a = f.get("anal", "normal")
    if a == "normal":
        out.append(("aleta", [(0.18, -top(0.18) * 0.9), (0.26, -top(0.26) - 0.07), (0.40, -top(0.40) - 0.03), (0.42, -top(0.42) * 0.9)]))
    elif a == "larga":
        out.append(("aleta", [(0.10, -top(0.10) * 0.9), (0.16, -top(0.16) - 0.05), (0.62, -top(0.62) - 0.04), (0.66, -top(0.66) * 0.9)]))
    elif a == "vela":
        out.append(("aleta", [(0.15, -top(0.15) * 0.9), (0.25, -top(0.25) - 0.16), (0.55, -top(0.55) - 0.12), (0.62, -top(0.62) * 0.9)]))
    return out


def classify(u, v, f):
    """Qué parte del pez hay en (u, v): None, 'cuerpo', 'aleta', 'cola', 'ojo', 'boca'."""
    h = body_half(u, f)
    if 0 <= u <= 1 and abs(v) <= h:
        eu, ev, er = f.get("ojo_u", 0.82), f.get("ojo_v", 0.30), f.get("ojo_r", 0.06)
        eye_v = ev * body_half(eu, f)
        if (u - eu) ** 2 + (v - eye_v) ** 2 <= er * er:
            return "ojo"
        return "cuerpo"
    tp = tail_poly(f)
    if tp and inside_poly(u, v, tp):
        return "cola"
    for kind, poly in fins(f):
        if inside_poly(u, v, poly):
            return kind
    if f.get("bigotes") and 0.95 < u < 1.08 and abs(v + 0.05 + (u - 0.95) * 0.9) < 0.03:
        return "aleta"
    if f.get("espada") and 0.95 < u < 1.35 and abs(v - 0.02) < 0.03:
        return "cuerpo"
    return None


def pattern(u, v, f, h):
    """Color del cuerpo según el dibujo."""
    base, belly = hexc(f["color"]), hexc(f.get("panza", f["color"]))
    t = (v / h + 1) / 2 if h > 0 else 0.5   # 0 = panza, 1 = lomo
    c = mix(belly, base, min(1, t * 1.25))
    p = f.get("dibujo", "liso")
    p2 = hexc(f.get("color2", "#000000"))
    if p == "rayas":                       # rayas verticales
        n = f.get("n", 4)
        if 0.12 < u < 0.86 and int(u * n * 2) % 2 == 0:
            c = p2
    elif p == "franja":                    # franja a lo largo
        if abs(v / max(h, 1e-6) - f.get("franja_v", 0.05)) < 0.22:
            c = p2
    elif p == "manchas":
        n = f.get("n", 7)
        cell = (int(u * n), int((v + 0.5) * n))
        hsh = (cell[0] * 73856093 ^ cell[1] * 19349663 ^ f.get("semilla", 1) * 83492791) & 0xFFFF
        if hsh % 3 == 0 and u > 0.08:
            c = p2
    elif p == "degradado":
        c = mix(c, p2, max(0.0, min(1.0, (u - 0.15) * 1.3)))
    elif p == "lomo":                      # lomo de otro color
        if t > 0.62:
            c = p2
    elif p == "anillos":
        n = f.get("n", 3)
        if 0.15 < u < 0.82 and int(u * n * 2) % 2 == 0:
            c = p2
        elif 0.15 < u < 0.82:
            c = mix(c, (255, 255, 255), 0.25)
    return c


def draw_fish(f, n=16):
    """Dibuja un pez de n x n (RGBA): 16 para el objeto, 32 para el libro."""
    N = n
    k_ = n / 16.0
    img = [[None] * N for _ in range(N)]
    L = f.get("largo", 13.0) * k_            # largo del cuerpo en pixeles (en diagonal)
    ang = math.radians(f.get("angulo", 45))
    ca, sa = math.cos(ang), math.sin(ang)
    cx, cy = N / 2 + f.get("dx", 0.0) * k_, N / 2 + f.get("dy", 0.0) * k_
    u0 = f.get("centro", 0.42)
    for py in range(N):
        for px in range(N):
            votes = {}
            for sy in range(SS):
                for sx in range(SS):
                    x = px + (sx + 0.5) / SS - cx
                    y = -(py + (sy + 0.5) / SS - cy)       # arriba = +
                    # girar al sistema del pez
                    a = (x * ca + y * sa) / L + u0
                    b = (-x * sa + y * ca) / L
                    k = classify(a, b, f)
                    votes[k] = votes.get(k, 0) + 1
            k = max(votes, key=votes.get)
            if votes.get(None, 0) > SS * SS * 0.56:
                k = None
            elif k is None:
                k = max((kk for kk in votes if kk), key=votes.get)
            if k:
                # el centro del pixel en (u, v)
                x = px + 0.5 - cx
                y = -(py + 0.5 - cy)
                a = (x * ca + y * sa) / L + u0
                b = (-x * sa + y * ca) / L
                img[py][px] = (k, a, b)
    out = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    fin = hexc(f.get("aleta_color", f.get("color2", f["color"])))
    for py in range(N):
        for px in range(N):
            cell = img[py][px]
            if not cell:
                continue
            k, a, b = cell
            if k == "ojo":
                c = (16, 16, 24)
                nb = img[py - 1][px] if py > 0 else None
                if N >= 32 and (nb is None or nb[0] != "ojo"):
                    c = hexc(f.get("ojo_color", "#F4F4F4"))
            elif k in ("aleta", "cola"):
                c = fin
                if f.get("aleta_rayas") and int((a * 30 + b * 10)) % 2 == 0:
                    c = shade(fin, -0.25)
                c = shade(c, 0.12 if b > 0 else -0.05)
            else:
                h = max(body_half(min(max(a, 0), 1), f), 1e-6)
                c = pattern(a, b, f, h)
            out.putpixel((px, py), c + (255,))
    if N < 32:
        for py in range(N):
            for px in range(N):
                if img[py][px] and img[py][px][0] == "ojo":
                    qx, qy = px - 1, py
                    if 0 <= qx and img[qy][qx] and img[qy][qx][0] == "cuerpo":
                        out.putpixel((qx, qy), hexc(f.get("ojo_color", "#F4F4F4")) + (255,))
                        img[qy][qx] = ("ojo", 0, 0)
    # sombras: borde de arriba-derecha más claro, de abajo-izquierda más oscuro (luz desde arriba)
    px_ = out.load()
    for py in range(N):
        for px in range(N):
            if px_[px, py][3] == 0 or img[py][px][0] == "ojo":
                continue
            up = py == 0 or px_[px, py - 1][3] == 0
            down = py == N - 1 or px_[px, py + 1][3] == 0
            left = px == 0 or px_[px - 1, py][3] == 0
            c = px_[px, py][:3]
            if up:
                c = shade(c, 0.22)
            elif down or left:
                c = shade(c, -0.22)
            px_[px, py] = c + (255,)
    # contorno oscuro por fuera
    outline = hexc(f["contorno"]) if "contorno" in f else mix(shade(hexc(f["color"]), -0.72), (27, 31, 42), 0.45)
    edge = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    ep = edge.load()
    for py in range(N):
        for px in range(N):
            if px_[px, py][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                qx, qy = px + dx, py + dy
                if 0 <= qx < N and 0 <= qy < N and px_[qx, qy][3]:
                    ep[px, py] = outline + (255,)
                    break
    edge.alpha_composite(out)
    return edge


MUESTRA = [
    dict(id="mojarra_del_rio", color="#8FA7B5", panza="#E7EEF0", color2="#5E7482", dibujo="rayas", n=4, aleta_color="#C99A5B"),
    dict(id="trucha_arcoiris", color="#6E8B5E", panza="#F0E6D8", color2="#E06A8A", dibujo="franja", franja_v=0.0, aleta_color="#A7B58E",
         cola="redonda"),
    dict(id="pez_luna", color="#D8A23A", panza="#F6E3A8", color2="#5B8FD0", dibujo="degradado", alto=0.275, pico=0.5, nariz=0.8,
         aleta_color="#5B8FD0", cola="abanico", dorsal="vela", anal="vela", largo=11.0),
    dict(id="bagre_de_lodo", color="#6B5A45", panza="#B8A487", color2="#3E3328", dibujo="manchas", n=8, aleta_color="#4E4234",
         cola="redonda", bigotes=True, alto=0.2, nariz=0.95, pico=0.45),
    dict(id="anguila_de_cueva", forma="anguila", color="#3F5A55", panza="#9FB8A6", color2="#9BE3C8", dibujo="lomo", aleta_color="#2E4440",
         cola="punta", dorsal="larga", anal="larga", alto=0.088, largo=15.0),
    dict(id="pez_payaso_de_coral", color="#F08A24", panza="#FFC27A", color2="#FFFFFF", dibujo="anillos", n=3, aleta_color="#1E1E1E",
         cola="redonda", alto=0.231, largo=12.0),
    dict(id="pez_espada", color="#4C6C9C", panza="#D6E2F0", color2="#2E4570", dibujo="lomo", aleta_color="#2E4570", cola="luna",
         espada=True, alto=0.143, largo=12.0, dx=-1.0, dy=1.0),
    dict(id="carpa_dorada", color="#E8A93A", panza="#FBE3A0", color2="#C8602A", dibujo="manchas", n=6, semilla=3, aleta_color="#F0C062",
         cola="abanico", alto=0.22, aleta_rayas=True),
    dict(id="pez_pirana", color="#6F7E8A", panza="#D94A3A", color2="#4D5964", dibujo="liso", aleta_color="#C0453A", alto=0.253, nariz=0.9,
         cola="horquilla", largo=12.0),
    dict(id="pez_linterna", color="#2A2F55", panza="#5C63A0", color2="#9CF3FF", dibujo="manchas", n=9, semilla=7, aleta_color="#3A4290",
         cola="horquilla", ojo_r=0.1, alto=0.187),
    dict(id="salmon_real", color="#7E8C96", panza="#F2A08A", color2="#C9544C", dibujo="degradado", aleta_color="#6A7780", cola="horquilla",
         largo=12.0),
    dict(id="pez_fantasma", color="#CFE6F2", panza="#FFFFFF", color2="#8FC7E6", dibujo="franja", franja_v=0.0, aleta_color="#BFE0F5",
         dorsal="vela", anal="vela", cola="abanico", ojo_color="#8FE7FF"),
]


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "peces_out"
    os.makedirs(out, exist_ok=True)
    imgs = []
    os.makedirs(os.path.join(out, "grande"), exist_ok=True)
    for f in MUESTRA:
        im = draw_fish(f)
        im.save(os.path.join(out, f["id"] + ".png"))
        big = draw_fish(f, 32)
        big.save(os.path.join(out, "grande", f["id"] + ".png"))
        imgs.append((f["id"], im, big))
    if "--hoja" in sys.argv:
        path = sys.argv[sys.argv.index("--hoja") + 1]
        cols, sc = 6, 12
        rows = (len(imgs) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * N * sc + (cols + 1) * 8, rows * N * sc + (rows + 1) * 8), (46, 52, 64, 255))
        rows = rows * 2
        sheet = Image.new("RGBA", (cols * N * sc + (cols + 1) * 8, rows * N * sc + (rows + 1) * 8), (46, 52, 64, 255))
        for i, (_, im, big) in enumerate(imgs):
            for j, pic in enumerate((im, big)):
                x = 8 + (i % cols) * (N * sc + 8)
                y = 8 + ((i // cols) * 2 + j) * (N * sc + 8)
                bg = Image.new("RGBA", (N * sc, N * sc), (139, 139, 139, 255) if j == 0 else (60, 52, 40, 255))
                bg.alpha_composite(pic.resize((N * sc, N * sc), Image.NEAREST))
                sheet.alpha_composite(bg, (x, y))
        sheet.save(path)
        print("hoja:", path)


if __name__ == "__main__":
    main()
