"""Las portadas (miniaturas) de cada video: horizontal 1280x720 (YouTube) y vertical 1080x1920 (TikTok, Reels, Shorts).

Fondo: un cuadro de la toma del video (aclarado como en el tráiler); encima, el Coleccionista en 3D recortado (sin fondo) y los letreros.
Uso: python3 portadas.py [nombre ...]   -> out/portadas/<nombre>_horizontal.jpg y _vertical.jpg
"""
import base64
import http.server
import io
import os
import socketserver
import subprocess
import sys
import threading

import numpy as np
from PIL import Image
from playwright.sync_api import sync_playwright

AQUI = os.path.dirname(os.path.abspath(__file__))
TOMAS = os.path.join(AQUI, 'tomas')
T3D = os.path.join(AQUI, 'out3d')
SAL = os.path.join(AQUI, 'out', 'portadas')
WEB = os.path.join(AQUI, 'web3d')

# fondo: ('juego', toma, segundo, cx vertical) o ('3d', toma, segundo) · coleccionista: None, 'retrato' o 'cara'
PORTADAS = {
    'trailer': dict(fondo=('juego', '27_pasillo', 2.5, 0.0), col='retrato', arriba='EL', grande='COLECCIONISTA',
                    etiqueta='TRÁILER OFICIAL', abajo='JUEVES 10 DE DICIEMBRE · 7 PM'),
    'teaser': dict(fondo=('3d', 'ojos', 3.0), col=None, arriba='', grande='¿QUÉ TENEMOS AQUÍ?', etiqueta='',
                   abajo='10 · 12 · 2026', teaser=True),
    'faltan7': dict(fondo=('juego', 'c1_67_cera', 1.0, -0.3), hx=120, col=None, numero='7', grande='DÍAS', etiqueta='FALTAN',
                    arriba='EL COLECCIONISTA', abajo='JUEVES 10 DE DICIEMBRE · 7 PM'),
    'faltan5': dict(fondo=('juego', 'c2_08_ruge', 1.4, 0.0), hx=300, col=None, numero='5', grande='DÍAS', etiqueta='FALTAN',
                    arriba='EL COLECCIONISTA', abajo='JUEVES 10 DE DICIEMBRE · 7 PM'),
    'faltan3': dict(fondo=('juego', 'c4_05_salta', 2.6, 0.0), hx=320, col=None, numero='3', grande='DÍAS', etiqueta='FALTAN',
                    arriba='EL COLECCIONISTA', abajo='JUEVES 10 DE DICIEMBRE · 7 PM'),
    'manana': dict(fondo=('juego', 'c6_03_bailarina', 2.5, 0.0), col='retrato', grande='MAÑANA', etiqueta='',
                   arriba='EL COLECCIONISTA', abajo='JUEVES 10 DE DICIEMBRE · 7 PM', rojo=True),
}


def gradar(x):
    x = np.clip(np.power(x, 0.74) * 1.06, 0, 1)
    lum = x[..., 0] * 0.299 + x[..., 1] * 0.587 + x[..., 2] * 0.114
    x = lum[..., None] + (x - lum[..., None]) * 0.85
    x = x * np.array([1.05, 0.97, 0.92], np.float32)
    x = np.clip(x, 0, 1)
    return x * x * (3 - 2 * x) * 0.5 + x * 0.5


def cuadro(f):
    if f[0] == 'juego':
        ruta = os.path.join(TOMAS, f[1] + '.mp4')
    else:
        ruta = os.path.join(T3D, f[1] + '.mp4')
    r = subprocess.run(['ffmpeg', '-v', 'error', '-ss', str(f[2]), '-i', ruta, '-frames:v', '1', '-vf', 'scale=1920:1080', '-f', 'rawvideo',
                        '-pix_fmt', 'rgb24', '-'], capture_output=True)
    x = np.frombuffer(r.stdout, np.uint8).reshape(1080, 1920, 3).astype(np.float32) / 255
    if f[0] == 'juego':
        x = gradar(x)
    return Image.fromarray((np.clip(x, 0, 1) * 255).astype(np.uint8))


def b64(im, fmt='PNG'):
    b = io.BytesIO()
    im.save(b, fmt, quality=92) if fmt == 'JPEG' else im.save(b, fmt)
    return 'data:image/' + fmt.lower() + ';base64,' + base64.b64encode(b.getvalue()).decode()


def servir():
    class H(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *a, **k):
            super().__init__(*a, directory=WEB, **k)

        def log_message(self, *a):
            pass
    s = socketserver.TCPServer(('127.0.0.1', 0), H)
    threading.Thread(target=s.serve_forever, daemon=True).start()
    return s.server_address[1]


def recortes(b):
    """El Coleccionista en 3D sin fondo: de medio cuerpo (horizontal y vertical)."""
    puerto = servir()
    out = {}
    for nombre, (w, h, q, pos) in {'retrato_h': (1920, 1080, '', (8, 44, -170, 36)),
                                   'retrato_v': (1080, 1920, '?v=1', (6, 40, -200, 34))}.items():
        pg = b.new_page(viewport={'width': w, 'height': h})
        pg.goto(f'http://127.0.0.1:{puerto}/index.html{q}')
        pg.wait_for_function('window.listo3d === true', timeout=60000)
        pg.add_style_tag(content='#fondo,#luz,#rayas,#grano,#vineta,#negro{display:none!important} html,body{background:transparent!important}')
        pg.evaluate(f'window.RX={pos[0]};window.RY={pos[1]};window.RZ={pos[2]};window.RTY={pos[3]};dibujar("retrato", 1.0, 2)')
        out[nombre] = Image.open(io.BytesIO(pg.screenshot(omit_background=True))).convert('RGBA')
        pg.close()
    return out


ESTILO = """
<style>
  html, body { margin: 0; background: #000; overflow: hidden; }
  .lienzo { position: relative; overflow: hidden; }
  .fondo { position: absolute; inset: 0; background-size: cover; }
  .sombra { position: absolute; inset: 0; }
  .col { position: absolute; }
  .txt { position: absolute; display: flex; flex-direction: column; }
  .arriba { font: 600 30px 'Latin Modern Roman Caps', 'Latin Modern Roman', serif; letter-spacing: 12px; color: #c9a25a;
    text-shadow: 0 0 14px #000, 0 0 4px #000; }
  .grande { font: 700 120px 'Latin Modern Roman', serif; letter-spacing: 4px; color: #efe3cc; line-height: 1;
    text-shadow: 6px 6px 0 #3a0008, 0 0 40px rgba(142,23,38,.85), 0 0 6px #000; }
  .numero { font: 700 420px 'Latin Modern Roman', serif; color: #c0182c; line-height: .82;
    text-shadow: 10px 10px 0 #2a0006, 0 0 80px rgba(200,20,40,.7), 0 0 8px #000; }
  .etiqueta { display: inline-block; font: 700 34px 'Latin Modern Roman', serif; letter-spacing: 8px; color: #fff;
    background: #8e1726; padding: 8px 22px 6px; box-shadow: 0 0 30px rgba(142,23,38,.7); align-self: flex-start; }
  .abajo { font: 600 34px 'Latin Modern Roman', serif; letter-spacing: 3px; color: #efe3cc; text-shadow: 0 0 12px #000, 0 0 4px #000; }
  .linea { height: 4px; background: linear-gradient(90deg, #8e1726, #d9a93a 50%, #8e1726); }
  .rojo { color: #c0182c; }
</style>
"""


def html_h(p, fondo, col):
    """Horizontal 1280x720: el letrero a la izquierda, la escena o el Coleccionista a la derecha."""
    W, H = 1280, 720
    partes = [f'<div class="lienzo" style="width:{W}px;height:{H}px">',
              ]
    hx = p.get('hx', 0)
    z = 1.0 + (2 * abs(hx) / W if hx else 0)
    partes.append(f'<img src="{fondo}" style="position:absolute;width:{W * z:.0f}px;height:{H * z:.0f}px;'
                  f'left:{(W - W * z) / 2 + hx:.0f}px;top:{(H - H * z) / 2:.0f}px">')
    if p.get('teaser'):
        partes.append('<div class="sombra" style="background:radial-gradient(ellipse at 50% 30%, transparent 20%, rgba(0,0,0,.6) 80%)"></div>')
        partes.append(f'<div class="txt" style="left:0;right:0;top:400px;align-items:center;gap:22px">'
                      f'<div class="grande" style="font-size:80px;letter-spacing:6px;white-space:nowrap;text-align:center">{p["grande"]}</div>'
                      f'<div class="linea" style="width:520px"></div><div class="abajo" style="font-size:40px;letter-spacing:14px">{p["abajo"]}</div></div>')
    else:
        partes.append('<div class="sombra" style="background:linear-gradient(90deg, rgba(0,0,0,.92) 0%, rgba(0,0,0,.75) 38%, '
                      'rgba(0,0,0,.1) 70%, rgba(0,0,0,.25) 100%)"></div>')
        partes.append('<div class="sombra" style="background:radial-gradient(ellipse at 50% 50%, transparent 55%, rgba(0,0,0,.6) 100%)"></div>')
        if col:
            partes.append(f'<img class="col" src="{col}" style="left:455px;top:45px;width:1200px">')
        if p.get('numero'):
            partes.append(f'<div class="txt" style="left:70px;top:70px;gap:6px">'
                          f'<div class="arriba">{p["arriba"]}</div>'
                          f'<div style="display:flex;align-items:flex-end;gap:26px;margin-top:14px">'
                          f'<div class="numero">{p["numero"]}</div>'
                          f'<div style="display:flex;flex-direction:column;gap:14px;padding-bottom:30px">'
                          f'<div class="etiqueta">{p["etiqueta"]}</div><div class="grande" style="font-size:130px">{p["grande"]}</div></div></div>'
                          f'<div class="linea" style="width:560px;margin-top:26px"></div>'
                          f'<div class="abajo" style="margin-top:18px">{p["abajo"]}</div></div>')
        else:
            g = f'<span class="rojo">{p["grande"]}</span>' if p.get('rojo') else p['grande']
            tam = 98 if p.get("rojo") else 64
            partes.append(f'<div class="txt" style="left:70px;top:{150 if p.get("rojo") else 170}px;gap:10px">'
                          + (f'<div class="etiqueta" style="margin-bottom:26px">{p["etiqueta"]}</div>' if p['etiqueta'] else '')
                          + f'<div class="arriba" style="font-size:{32 if p.get("rojo") else 40}px;letter-spacing:{12 if p.get("rojo") else 30}px">{p["arriba"]}</div>'
                          f'<div class="grande" style="font-size:{tam}px">{g}</div>'
                          f'<div class="linea" style="width:520px;margin-top:22px"></div>'
                          f'<div class="abajo" style="margin-top:16px;font-size:30px">{p["abajo"]}</div></div>')
    partes.append('</div>')
    return ''.join(partes)


def html_v(p, fondo, col, cx):
    """Vertical 1080x1920: la escena llena todo; el letrero arriba de la mitad (abajo y a la derecha van los botones de la app)."""
    W, H = 1080, 1920
    alto = 1400
    ancho = alto * 16 / 9
    izq = (W - ancho) / 2 - cx * ancho
    partes = [f'<div class="lienzo" style="width:{W}px;height:{H}px">']
    if not p.get('teaser'):
        partes.append(f'<img src="{fondo}" style="position:absolute;height:{alto}px;width:{ancho:.0f}px;left:{izq:.0f}px;top:{H - alto}px">')
        partes.append('<div class="sombra" style="background:linear-gradient(180deg, #000 0%, #000 26%, rgba(0,0,0,.7) 40%, '
                      'rgba(0,0,0,.15) 62%, rgba(0,0,0,.2) 85%, rgba(0,0,0,.6) 100%)"></div>')
    else:
        partes.append(f'<div class="fondo" style="background-image:url({fondo});background-position:center"></div>')
    if col:
        partes.append(f'<img class="col" src="{col}" style="left:0;top:560px;width:{W}px">')
    if p.get('teaser'):
        partes.append(f'<div class="txt" style="left:0;right:0;top:900px;align-items:center;gap:26px">'
                      f'<div class="grande" style="font-size:84px;text-align:center;line-height:1.1">¿QUÉ TENEMOS<br>AQUÍ?</div>'
                      f'<div class="linea" style="width:600px"></div><div class="abajo" style="font-size:48px;letter-spacing:16px">{p["abajo"]}</div></div>')
    elif p.get('numero'):
        partes.append(f'<div class="txt" style="left:0;right:0;top:150px;align-items:center;gap:10px">'
                      f'<div class="arriba" style="font-size:40px">{p["arriba"]}</div>'
                      f'<div class="etiqueta" style="align-self:center;margin-top:26px;font-size:46px">{p["etiqueta"]}</div>'
                      f'<div class="numero" style="font-size:440px;margin-top:10px">{p["numero"]}</div>'
                      f'<div class="grande" style="font-size:130px">{p["grande"]}</div>'
                      f'<div class="linea" style="width:640px;margin-top:30px"></div>'
                      f'<div class="abajo" style="margin-top:20px;font-size:38px">{p["abajo"]}</div></div>')
    else:
        g = f'<span class="rojo">{p["grande"]}</span>' if p.get('rojo') else p['grande']
        partes.append(f'<div class="txt" style="left:0;right:0;top:{170 if p.get("rojo") else 150}px;align-items:center;gap:12px">'
                      + (f'<div class="etiqueta" style="align-self:center;margin-bottom:30px;font-size:42px">{p["etiqueta"]}</div>'
                         if p['etiqueta'] else '')
                      + f'<div class="arriba" style="font-size:{46 if p.get("rojo") else 64}px;letter-spacing:{14 if p.get("rojo") else 40}px">'
                      f'{p["arriba"]}</div>'
                      f'<div class="grande" style="font-size:{150 if p.get("rojo") else 92}px">{g}</div>'
                      f'<div class="linea" style="width:700px;margin-top:26px"></div>'
                      f'<div class="abajo" style="margin-top:20px;font-size:40px">{p["abajo"]}</div></div>')
    partes.append('</div>')
    return ''.join(partes)


def main():
    solo = sys.argv[1:]
    os.makedirs(SAL, exist_ok=True)
    with sync_playwright() as pw:
        b = pw.chromium.launch(args=['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'])
        rec = recortes(b)
        for n, p in PORTADAS.items():
            if solo and n not in solo:
                continue
            fondo = b64(cuadro(p['fondo']), 'JPEG')
            cx = p['fondo'][3] if len(p['fondo']) > 3 else 0.0
            for fmt, (w, h) in (('horizontal', (1280, 720)), ('vertical', (1080, 1920))):
                col = None
                if p.get('col'):
                    col = b64(rec['retrato_h' if fmt == 'horizontal' else 'retrato_v'])
                html = html_h(p, fondo, col) if fmt == 'horizontal' else html_v(p, fondo, col, cx)
                pg = b.new_page(viewport={'width': w, 'height': h})
                pg.set_content('<!doctype html><html><head><meta charset="utf-8">' + ESTILO + '</head><body>' + html + '</body></html>')
                pg.wait_for_timeout(300)
                ruta = os.path.join(SAL, f'{n}_{fmt}.jpg')
                pg.screenshot(path=ruta, type='jpeg', quality=93)
                pg.close()
                print('ok', ruta)
        b.close()


if __name__ == '__main__':
    main()
