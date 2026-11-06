#!/usr/bin/env python3
"""Uso: shoot.py <carpeta_baked> <salida_prefijo> x,y,z,yaw,pitch[,fov] [...]
yaw estilo Minecraft (0 = mira al sur +z, 90 = oeste -x, 180 = norte, 270 = este); pitch positivo mira hacia abajo."""
import sys, os, base64, subprocess, time, shutil
from playwright.sync_api import sync_playwright
d, pre = sys.argv[1], sys.argv[2]
shots = sys.argv[3:]
here = os.path.dirname(os.path.abspath(__file__))
srv_dir = os.path.abspath(os.path.join(d, '..'))
# servidor local que sirve viewer.html, three.module.js y la carpeta
work = '/tmp/visor_srv'; os.makedirs(work, exist_ok=True)
for f in ('viewer.html', 'three.module.js'):
    src = os.path.join(here, f)
    if os.path.exists(src): shutil.copy(src, os.path.join(work, f))
link = os.path.join(work, 'out_' + os.path.basename(os.path.abspath(d)))
if os.path.islink(link) or os.path.exists(link):
    try: os.remove(link)
    except IsADirectoryError: pass
os.symlink(os.path.abspath(d), link)
port = 8765
srv = subprocess.Popen([sys.executable, '-m', 'http.server', str(port), '--directory', work], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
time.sleep(1)
try:
    with sync_playwright() as p:
        b = p.chromium.launch(args=['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'])
        pg = b.new_page(viewport={'width': 1280, 'height': 720})
        logs = []
        pg.on('console', lambda m: logs.append(m.text))
        pg.on('pageerror', lambda e: logs.append('ERR ' + str(e)))
        pg.goto(f'http://localhost:{port}/viewer.html?d=out_' + os.path.basename(os.path.abspath(d)))
        try:
            pg.wait_for_function('window.ready===true', timeout=180000)
        except Exception:
            print('\n'.join(logs[-10:])); raise
        for i, s in enumerate(shots):
            v = [float(t) for t in s.split(',')]
            while len(v) < 6: v.append(70)
            url = pg.evaluate(f'window.shot({v[0]},{v[1]},{v[2]},{v[3]},{v[4]},{v[5]})')
            open(f'{pre}_{i}.png', 'wb').write(base64.b64decode(url.split(',')[1]))
            print('ok', f'{pre}_{i}.png')
        b.close()
finally:
    srv.terminate()
