# Arma el logo de Axelis (A con diamante + letras Poppins convertidas a trazos) y guarda SVG limpios.
import json, sys
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.boundsPen import BoundsPen
OUT = sys.argv[1]
F = '/usr/share/fonts/truetype/google-fonts/Poppins-%s.ttf'
A = 'M50 4L97 96H73L50 51L27 96H3Z M50 65L59 75L50 85L41 75Z'   # la A: lambda + diamante (caja 3..97 x 4..96)
AX0, AY0, AW, AH = 3, 4, 94, 92

def palabra(peso, txt, cap, x0, base, track=0.02):
    f = TTFont(F % peso); gs = f.getGlyphSet(); cmap = f.getBestCmap()
    capu = f['OS/2'].sCapHeight; s = cap / capu
    pen = SVGPathPen(gs); x = 0; bmin = None; bmax = None
    for i, ch in enumerate(txt):
        g = cmap[ord(ch)]
        tp = TransformPen(pen, (s, 0, 0, -s, x0 + x, base))
        gs[g].draw(tp)
        bp = BoundsPen(gs); gs[g].draw(bp)
        if bp.bounds:
            lx = x0 + x + bp.bounds[0] * s; rx = x0 + x + bp.bounds[2] * s
            bmin = lx if bmin is None else min(bmin, lx); bmax = rx if bmax is None else max(bmax, rx)
        x += gs[g].width * s + (track * cap * 1.43 if i < len(txt) - 1 else 0)
    return pen.getCommands(), bmin, bmax

def icono(x, y, h):
    k = h / AH
    return f'<path transform="matrix({k:.4f} 0 0 {k:.4f} {x - AX0 * k:.3f} {y - AY0 * k:.3f})" d="{A}" fill-rule="evenodd"/>', AW * k

def svg(nombre, w, h, cuerpo, color):
    for c, suf in ((color, ''),):
        pass
    for col, suf in (('#ffffff', '_blanco'), ('#000000', '_negro')):
        open(f'{OUT}/{nombre}{suf}.svg', 'w').write(
            f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w:.2f} {h:.2f}" width="{w:.0f}" height="{h:.0f}"><g fill="{col}">{cuerpo}</g></svg>')

info = {}
# 1) AXELIS / STUDIO (dos renglones; el icono mide lo que miden los dos renglones)
C = 64; G = C * 0.32
H = C * 2 + G
ic, iw = icono(0, 0, H)
gap = H * 0.30
tx = iw + gap
p1, a1, b1 = palabra('Bold', 'AXELIS', C, tx, C)
p2, a2, b2 = palabra('Light', 'STUDIO', C, tx, C * 2 + G)
# alinear la orilla izquierda real de las letras con tx
d1 = tx - a1; d2 = tx - a2
p1, a1, b1 = palabra('Bold', 'AXELIS', C, tx + d1, C)
p2, a2, b2 = palabra('Light', 'STUDIO', C, tx + d2, C * 2 + G)
W = max(b1, b2)
svg('axelis_studio', W, H, ic + f'<path d="{p1}"/><path d="{p2}"/>', '#fff')
info['studio'] = dict(w=W, h=H, icono=iw, texto=tx, linea1=C, linea2=C + G, ancho1=b1, ancho2=b2)
# 2) AXELIS solo (un renglón; el icono un poco más alto que las mayúsculas)
C2 = 84; H2 = C2 * 1.5
ic2, iw2 = icono(0, 0, H2)
gap2 = H2 * 0.28; tx2 = iw2 + gap2
base2 = (H2 + C2) / 2
p3, a3, b3 = palabra('Bold', 'AXELIS', C2, tx2, base2)
p3, a3, b3 = palabra('Bold', 'AXELIS', C2, tx2 + (tx2 - a3), base2)
svg('axelis', b3, H2, ic2 + f'<path d="{p3}"/>', '#fff')
info['solo'] = dict(w=b3, h=H2, icono=iw2, texto=tx2, arriba=base2 - C2, base=base2)
# 3) la A sola
ic3, iw3 = icono(0, 0, 92)
svg('axelis_A', iw3, 92, ic3, '#fff')
print(json.dumps(info, indent=1))
