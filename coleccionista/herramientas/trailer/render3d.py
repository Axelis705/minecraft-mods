"""Graba las tomas del Coleccionista 3D cuadro por cuadro (Playwright + la página web3d).

Uso: python3 render3d.py <salida> <toma>:<segundos> [...]  [--prueba t1,t2,...]
Cada toma queda como un video <salida>/<toma>.mp4 a 30 cps (o, con --prueba, unas fotos sueltas).
"""
import http.server
import os
import socketserver
import subprocess
import sys
import threading

from playwright.sync_api import sync_playwright

AQUI = os.path.dirname(os.path.abspath(__file__))
WEB = os.path.join(AQUI, 'web3d')
FPS = 30


def servir():
    class H(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *a, **k):
            super().__init__(*a, directory=WEB, **k)

        def log_message(self, *a):
            pass
    s = socketserver.TCPServer(('127.0.0.1', 0), H)
    threading.Thread(target=s.serve_forever, daemon=True).start()
    return s.server_address[1]


def main():
    salida = sys.argv[1]
    os.makedirs(salida, exist_ok=True)
    tomas, prueba = [], None
    args = sys.argv[2:]
    vert = '--vertical' in args
    if vert:
        args.remove('--vertical')
    suf = '_v' if vert else ''
    W, H = (1080, 1920) if vert else (1920, 1080)
    if '--prueba' in args:
        i = args.index('--prueba')
        prueba = [float(x) for x in args[i + 1].split(',')]
        args = args[:i] + args[i + 2:]
    for a in args:
        n, s = a.split(':')
        tomas.append((n, float(s)))
    puerto = servir()
    with sync_playwright() as p:
        b = p.chromium.launch(args=['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'])
        pg = b.new_page(viewport={'width': W, 'height': H})
        pg.goto(f'http://127.0.0.1:{puerto}/index.html' + ('?v=1' if vert else ''))
        pg.wait_for_function('window.listo3d === true', timeout=60000)
        for nombre, seg in tomas:
            if prueba:
                for t in prueba:
                    pg.evaluate(f"dibujar('{nombre}', {t}, {seg})")
                    pg.screenshot(path=os.path.join(salida, f'{nombre}{suf}_{t:.2f}.jpg'), type='jpeg', quality=90)
                continue
            n = int(round(seg * FPS))
            tmp = os.path.join(salida, nombre + suf + '.tmp.mp4')
            ff = subprocess.Popen(['ffmpeg', '-v', 'error', '-y', '-f', 'image2pipe', '-framerate', str(FPS), '-c:v', 'mjpeg', '-i', '-',
                                   '-c:v', 'libx264', '-crf', '16', '-preset', 'veryfast', '-pix_fmt', 'yuv420p', tmp], stdin=subprocess.PIPE)
            for i in range(n):
                pg.evaluate(f"dibujar('{nombre}', {i / FPS}, {seg})")
                ff.stdin.write(pg.screenshot(type='jpeg', quality=92))
            ff.stdin.close()
            if ff.wait() == 0:
                os.replace(tmp, os.path.join(salida, nombre + suf + '.mp4'))
                print('ok', nombre + suf, n, 'cuadros')
            else:
                print('FALLO', nombre)
        b.close()


if __name__ == '__main__':
    main()
