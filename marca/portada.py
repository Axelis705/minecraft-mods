# Portada de X (1500x500) con la marca de Axelis: letras al centro, A gigante de fondo y guías de construcción.
import sys
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.boundsPen import BoundsPen
F = '/usr/share/fonts/truetype/google-fonts/Poppins-%s.ttf'
A = 'M50 4L97 96H73L50 51L27 96H3Z M50 65L59 75L50 85L41 75Z'
W, H = 1500, 500

def palabra(peso, txt, cap, x0, base, track=0.02):
    f = TTFont(F % peso); gs = f.getGlyphSet(); cmap = f.getBestCmap()
    s = cap / f['OS/2'].sCapHeight
    def medir(x0):
        pen = SVGPathPen(gs); x = 0; lo = hi = None
        for i, ch in enumerate(txt):
            g = cmap[ord(ch)]
            gs[g].draw(TransformPen(pen, (s, 0, 0, -s, x0 + x, base)))
            bp = BoundsPen(gs); gs[g].draw(bp)
            if bp.bounds:
                l = x0 + x + bp.bounds[0] * s; r = x0 + x + bp.bounds[2] * s
                lo = l if lo is None else min(lo, l); hi = r if hi is None else max(hi, r)
            x += gs[g].width * s + (track * cap * 1.43 if i < len(txt) - 1 else 0)
        return pen.getCommands(), lo, hi
    _, lo, hi = medir(0)
    return medir, lo, hi

def portada(con_studio, out):
    partes = []; guias_x = []; guias_y = []
    C = 92 if con_studio else 110
    G = C * 0.32
    alto = C * 2 + G if con_studio else C
    top = (H - alto) / 2 - 10
    m1, lo1, hi1 = palabra('Bold', 'AXELIS', C, 0, top + C)
    w = hi1 - lo1
    if con_studio:
        m2, lo2, hi2 = palabra('Light', 'STUDIO', C, 0, top + 2 * C + G)
        w = max(w, hi2 - lo2)
    x = (W - w) / 2
    p1, a1, b1 = m1(x - lo1)
    partes.append(f'<path d="{p1}"/>')
    derecha = b1
    if con_studio:
        p2, a2, b2 = m2(x - lo2)
        partes.append(f'<path d="{p2}"/>')
        derecha = max(b1, b2)
        guias_y += [top + C, top + C + G]
    abajo = top + alto
    u = alto / 3.2 if con_studio else alto / 2.2
    guias_x += [x - u, x, derecha, derecha + u]
    guias_y += [top - u, top, abajo, abajo + u]
    # lema pequeño
    m3, lo3, hi3 = palabra('Medium', 'MODS  ·  EVENTOS  ·  VIDEOS', 15, 0, 0, track=0.25)
    ylema = abajo + u + 34
    p3, _, _ = m3((W - (hi3 - lo3)) / 2 - lo3)
    lema = f'<path d="{p3}" transform="translate(0 {ylema})" fill="#8c8c8c"/>'
    # A gigante de fondo, cortada a la derecha
    k = 6.2; ax = 1050; ay = 40
    fondo = f'<path transform="matrix({k} 0 0 {k} {ax} {ay})" d="{A}" fill="#121212" fill-rule="evenodd"/>'
    lineas = ''.join(f'<line x1="{gx:.1f}" y1="0" x2="{gx:.1f}" y2="{H}"/>' for gx in guias_x)
    lineas += ''.join(f'<line x1="0" y1="{gy:.1f}" x2="{W}" y2="{gy:.1f}"/>' for gy in guias_y)
    cuadros = ''.join(f'<rect x="{cx:.1f}" y="{cy:.1f}" width="{u:.1f}" height="{u:.1f}" fill="#1c1c1c"/>'
                      for cx in (x - u, derecha) for cy in (top - u, abajo))
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">'
           f'<rect width="{W}" height="{H}" fill="#000"/>{fondo}{cuadros}'
           f'<g stroke="#2e2e2e" stroke-width="1.2" stroke-dasharray="7 6">{lineas}</g>'
           f'<g fill="#fff">{"".join(partes)}</g>{lema}</svg>')
    open(out, 'w').write(svg)

portada(True, sys.argv[1] + '/portada_axelis_studio.svg')
portada(False, sys.argv[1] + '/portada_axelis.svg')
