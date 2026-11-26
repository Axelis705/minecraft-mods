"""Monta el tráiler de El Coleccionista: tomas del juego, el Coleccionista 3D, letreros, cortes con estática, voz y música.

Uso: python3 montar.py [horizontal|vertical] [--prueba t1,t2,...] [--hasta s]
Sale out/trailer_<formato>.mp4 (con sonido). Con --prueba solo guarda esos cuadros como JPEG para revisarlos.
"""
import json
import os
import subprocess
import sys

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
FPS = 30
TOMAS = os.path.join(AQUI, 'tomas')        # las del juego (rama trailer-coleccionista)
T3D = os.path.join(AQUI, 'out3d')          # las del Coleccionista 3D
FOTOS = os.path.join(os.path.dirname(AQUI), 'fotos_col')
CARTAS = os.path.join(AQUI, 'cartas')
AUDIO = os.path.join(AQUI, 'audio')
OUT = os.path.join(AQUI, 'out')

# ------------------------------------------------------------------------------------------------ el guion (segundos)
# cada escena: (inicio, duración, fuente, desde (s dentro de la fuente), velocidad, efectos)
CUENTA = os.environ.get('CUENTA', '7')

def _video(clips, voz, carta, final3d=False):
    """Tres tomas con la voz encima, un golpe y el letrero de cuántos días faltan (sobre negro o sobre los ojos)."""
    esc = [(0.0, 0.5, ('negro',), 0, 1, {'estatica': 0.05})]
    t = 0.5
    for nombre, dur, desde, ex in clips:
        e = {'glitch_in': True}
        e.update(ex)
        esc.append((t, dur, ('juego', nombre), desde, 1.0, e))
        t += dur
    corte = t
    if final3d:
        esc.append((corte, 0.4, ('negro',), 0, 1, {}))
        esc.append((corte + 0.4, 5.0, ('3d', 'ojos'), 0.5, 1.0, {'fade_in': 0.6}))
    else:
        esc.append((corte, 5.4, ('negro',), 0, 1, {'estatica': 0.03}))
    total = corte + 5.4
    letreros = [(carta, corte + 0.45, total - 0.5, {'golpe': True})]
    sonidos = [
        ('musica.ogg', 0.0, 0.5, {'fade_in': 1.0, 'hasta': corte, 'fade_out': 0.2, 'lenta': True}),
        ('ambiente.ogg', 0.0, 0.45, {'bucle': True, 'hasta': total, 'fade_out': 1.5}),
        (voz, 0.75, 1.0, {'voz': True}),
        ('golpe.ogg', corte, 1.0, {}),
        ('latido.ogg', corte + 1.4, 0.6, {}), ('latido.ogg', corte + 2.3, 0.5, {}),
    ]
    a = 0.5
    for nombre, dur, desde, ex in clips:
        sonidos.append(('estatica.ogg', a - 0.05, 0.3, {}))
        a += dur
    return esc, letreros, sonidos, total


VIDEOS = {
    # 7 días: la colección (las celdas y la cera)
    '7': ([('26b_reja_cerrada', 1.9, 0.5, {}), ('c1_66_vitrinas', 2.0, 0.5, {}), ('c1_67_cera', 2.1, 0.3, {'cx': -0.3})],
          'vitrina_piezas.ogg', 'faltan7'),
    # 5 días: la mascota (Cascabel)
    '5': ([('c2_06_cascabel', 2.2, 0.8, {}), ('c2_03_jaula', 1.6, 0.6, {}), ('c2_08_ruge', 2.2, 0.4, {'flash': True})],
          'mansion_cascabel.ogg', 'faltan5'),
    # 3 días: Resortes y el té
    '3': ([('c4_05_salta', 2.4, 1.2, {}), ('c5_06_manos', 1.8, 1.0, {'cx': -0.33}), ('c5_09_veneno', 1.9, 0.6, {'cx': -0.2})],
          'sorpresa_hambre.ogg', 'faltan3'),
    # 1 día: la bailarina y la mesa del veintiuno; el letrero sobre los ojos
    '1': ([('c6_03_bailarina', 2.0, 1.5, {}), ('c7_03_mesa', 2.3, 0.8, {'cx': -0.28}), ('c7_08_aprieta', 1.6, 0.1, {'flash': True})],
          'veintiuno_dos.ogg', 'manana'),
}
_c = VIDEOS[CUENTA]
ESCENAS, LETREROS, SONIDOS, TOTAL = _video(_c[0], _c[1], _c[2], final3d=(CUENTA == '1'))
VOZ = 3.0

# ------------------------------------------------------------------------------------------------ lectura de fuentes


class Lector:
    """Lee los cuadros de un video en orden (desde un segundo), a 30 cps."""

    def __init__(self, ruta, desde, w, h, cx=0.0):
        self.w, self.h = w, h
        # cx: cuánto correr el recorte a un lado (−0.5 a 0.5 del ancho sobrante), para el vertical
        vf = (f'scale={w}:{h}:force_original_aspect_ratio=increase,'
              f'crop={w}:{h}:(iw-{w})/2+(iw-{w})*{cx:.3f}:(ih-{h})/2')
        self.p = subprocess.Popen(['ffmpeg', '-v', 'error', '-ss', f'{desde:.3f}', '-i', ruta, '-vf', vf, '-r', str(FPS),
                                   '-f', 'rawvideo', '-pix_fmt', 'rgb24', '-'], stdout=subprocess.PIPE)
        self.idx = -1
        self.ult = np.zeros((h, w, 3), np.uint8)

    def cuadro(self, i):
        while self.idx < i:
            b = self.p.stdout.read(self.w * self.h * 3)
            if len(b) < self.w * self.h * 3:
                break
            self.ult = np.frombuffer(b, np.uint8).reshape(self.h, self.w, 3)
            self.idx += 1
        return self.ult

    def cerrar(self):
        try:
            self.p.kill()
        except Exception:
            pass


def foto_kb(nombre, k, w, h):
    """Una foto de la prueba con un zoom lento (si todavía no hay toma de video)."""
    ruta = os.path.join(FOTOS, f'coleccionista_{nombre}.png')
    if not os.path.exists(ruta):
        return np.zeros((h, w, 3), np.uint8)
    im = Image.open(ruta).convert('RGB')
    s = 1.0 + 0.07 * k
    W, H = im.size
    if W / H > w / h:      # recorta al formato de salida (en vertical, el centro)
        nw = H * w / h
        im = im.crop((int((W - nw) / 2), 0, int((W + nw) / 2), H))
        W = im.size[0]
    cw, ch = W / s, H / s
    x0, y0 = (W - cw) / 2, (H - ch) / 2
    im = im.crop((int(x0), int(y0), int(x0 + cw), int(y0 + ch))).resize((w, h), Image.BICUBIC)
    return np.asarray(im)


# ------------------------------------------------------------------------------------------------ efectos


def mascaras(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    nx, ny = (xx / w - 0.5) * 2, (yy / h - 0.5) * 2
    d = np.sqrt(nx ** 2 * 0.8 + ny ** 2)
    vin = np.clip(1.0 - 0.55 * np.clip(d - 0.45, 0, None) ** 1.5, 0.25, 1.0)
    return vin[..., None]


def gradar(x):
    lum = x[..., 0] * 0.299 + x[..., 1] * 0.587 + x[..., 2] * 0.114
    x = lum[..., None] + (x - lum[..., None]) * 0.82
    x = x * np.array([1.05, 0.97, 0.92], np.float32)
    x = np.clip(x, 0, 1)
    # curva suave (contraste) con negros apenas levantados
    x = x * x * (3 - 2 * x) * 0.55 + x * 0.45
    return x * 0.96 + 0.012


def glitch(x, fuerza, rng):
    h, w, _ = x.shape
    s = int(14 * fuerza)
    y = x.copy()
    y[..., 0] = np.roll(x[..., 0], s, axis=1)
    y[..., 2] = np.roll(x[..., 2], -s, axis=1)
    for _ in range(int(6 * fuerza) + 1):
        a = rng.integers(0, h - 20)
        b = a + rng.integers(8, 90)
        y[a:b] = np.roll(y[a:b], rng.integers(-60, 60), axis=1)
    ruido = rng.random((h // 4, w // 4), dtype=np.float32)
    ruido = np.repeat(np.repeat(ruido, 4, 0), 4, 1)[..., None]
    return y * (1 - 0.35 * fuerza) + ruido * 0.35 * fuerza


def estatica(h, w, nivel, rng):
    r = rng.random((h // 3, w // 3), dtype=np.float32)
    r = np.repeat(np.repeat(r, 3, 0), 3, 1)[:h, :w, None]
    return np.repeat(r * nivel, 3, axis=2)


class Carta:
    def __init__(self, nombre, w, h, sufijo=''):
        im = Image.open(os.path.join(CARTAS, nombre + sufijo + '.png')).convert('RGBA')
        if im.size != (w, h):
            im = im.resize((w, h), Image.LANCZOS)
        a = np.asarray(im).astype(np.float32) / 255
        self.rgb, self.a = a[..., :3], a[..., 3:4]

    def poner(self, x, op, dx=0, dy=0, escala=1.0):
        rgb, a = self.rgb, self.a
        if escala != 1.0:
            h, w = a.shape[:2]
            im = Image.fromarray((np.concatenate([rgb, a], 2) * 255).astype(np.uint8), 'RGBA')
            nw, nh = int(w * escala), int(h * escala)
            im = im.resize((nw, nh), Image.BILINEAR)
            x0, y0 = (nw - w) // 2, (nh - h) // 2
            im = im.crop((x0, y0, x0 + w, y0 + h))
            arr = np.asarray(im).astype(np.float32) / 255
            rgb, a = arr[..., :3], arr[..., 3:4]
        if dx or dy:
            rgb = np.roll(np.roll(rgb, dy, 0), dx, 1)
            a = np.roll(np.roll(a, dy, 0), dx, 1)
        a = a * op
        return x * (1 - a) + rgb * a


# ------------------------------------------------------------------------------------------------ el video


def fuente_ruta(f, suf=''):
    if f[0] == 'juego':
        return os.path.join(TOMAS, f[1] + '.mp4')
    if f[0] == '3d':
        v = os.path.join(T3D, f[1] + suf + '.mp4')
        return v if os.path.exists(v) else os.path.join(T3D, f[1] + '.mp4')
    return None


def render(formato, prueba=None, hasta=None):
    w, h = (1920, 1080) if formato == 'horizontal' else (1080, 1920)
    suf = '' if formato == 'horizontal' else '_v'
    os.makedirs(OUT, exist_ok=True)
    vin = mascaras(w, h)
    rng = np.random.default_rng(5)
    cartas = {n: Carta(n, w, h, suf) for n, *_ in LETREROS}
    barra = 0 if formato == 'vertical' else 132
    total = hasta or TOTAL
    n = int(round(total * FPS))
    cuadros = range(n) if prueba is None else [int(round(t * FPS)) for t in prueba]
    ff = None
    if prueba is None:
        tmp = os.path.join(OUT, f'cvideo{CUENTA}_{formato}.tmp.mp4')
        ff = subprocess.Popen(['ffmpeg', '-v', 'error', '-y', '-f', 'rawvideo', '-pix_fmt', 'rgb24', '-s', f'{w}x{h}', '-r', str(FPS),
                               '-i', '-', '-c:v', 'libx264', '-crf', '17', '-preset', 'veryfast', '-pix_fmt', 'yuv420p', tmp],
                              stdin=subprocess.PIPE)
    lectores = {}
    for f in cuadros:
        t = f / FPS
        esc = None
        for e in ESCENAS:
            if e[0] <= t < e[0] + e[1]:
                esc = e
                break
        x = np.zeros((h, w, 3), np.float32)
        efe = {}
        if esc:
            ini, dur, src, desde, vel, efe = esc
            local = t - ini
            ruta = fuente_ruta(src, suf)
            cx = efe.get('cx', 0.0) if formato == 'vertical' else 0.0
            if ruta and os.path.exists(ruta):
                clave = (ini, ruta)
                if prueba is not None:
                    lec = Lector(ruta, desde + local * vel, w, h, cx)
                    x = lec.cuadro(0).astype(np.float32) / 255
                    lec.cerrar()
                else:
                    if clave not in lectores:
                        for l in lectores.values():
                            l.cerrar()
                        lectores = {clave: Lector(ruta, desde, w, h, cx)}
                    x = lectores[clave].cuadro(int(local * vel * FPS)).astype(np.float32) / 255
            elif src[0] == 'juego':
                x = foto_kb(src[1], local / dur, w, h).astype(np.float32) / 255
            if src[0] == 'juego':
                # las tomas del juego salen muy oscuras: se levantan un poco los medios tonos
                x = np.clip(np.power(x, 0.74) * 1.06, 0, 1)
            if src[0] != 'negro':
                x = gradar(x)
            if efe.get('oscuro'):
                x = x * (1 - efe['oscuro'])
            if efe.get('fade_in') and local < efe['fade_in']:
                x = x * (local / efe['fade_in'])
            if efe.get('estatica'):
                x = x + estatica(h, w, efe['estatica'] * (0.6 + 0.4 * rng.random()), rng)
            if efe.get('glitch_in') and local < 4 / FPS:
                x = glitch(x, 1.0 - local * FPS / 4, rng)
            if efe.get('flash') and local < 2 / FPS:
                x = np.clip(x + 0.6 * np.array([1, 0.25, 0.25], np.float32), 0, 1)
        # letreros
        for nombre, a, b, le in LETREROS:
            if a <= t < b:
                k = (t - a) / (b - a)
                op = min(1, (t - a) / 0.35, (b - t) / 0.45)
                dx = dy = 0
                if le.get('golpe') and t - a < 0.12:
                    dx = int(rng.integers(-18, 18))
                if le.get('tiembla') and rng.random() < 0.18:
                    dx, dy = int(rng.integers(-4, 4)), int(rng.integers(-3, 3))
                x = cartas[nombre].poner(x, op, dx, dy, 1.0 + 0.035 * k)
                if le.get('golpe') and t - a < 3 / FPS:
                    x = glitch(x, 0.8, rng)
        # al final: vineta, grano y las franjas de cine
        x = x * vin
        x = x + (rng.standard_normal((h // 2, w // 2), dtype=np.float32).repeat(2, 0).repeat(2, 1)[..., None] * 0.018)
        if t >= TOTAL - 1.2:
            x = x * max(0, (TOTAL - t) / 1.2)
        if barra:
            x[:barra] = 0
            x[h - barra:] = 0
        img = (np.clip(x, 0, 1) * 255).astype(np.uint8)
        if prueba is not None:
            Image.fromarray(img).save(os.path.join(OUT, f'prueba_{formato}_{t:05.2f}.jpg'), quality=90)
        else:
            ff.stdin.write(img.tobytes())
            if f % 150 == 0:
                print(f'{t:5.1f}s', flush=True)
    for l in lectores.values():
        l.cerrar()
    if ff:
        ff.stdin.close()
        if ff.wait() == 0:
            os.replace(os.path.join(OUT, f'cvideo{CUENTA}_{formato}.tmp.mp4'), os.path.join(OUT, f'cvideo{CUENTA}_{formato}.mp4'))
            return True
    return False


# ------------------------------------------------------------------------------------------------ el sonido


def sonido(formato, total=None):
    total = total or TOTAL
    entradas, filtros, voces, resto = [], [], [], []
    for i, (arch, ini, vol, ex) in enumerate(SONIDOS):
        ruta = os.path.join(AUDIO, arch)
        if ex.get('bucle'):
            entradas += ['-stream_loop', '-1', '-i', ruta]
        else:
            entradas += ['-i', ruta]
        cad = f'[{i}:a]aformat=sample_rates=48000:channel_layouts=stereo'
        if ex.get('lenta'):
            # la caja de música: un poquito más lenta y grave (como si se estuviera quedando sin cuerda)
            cad += ',asetrate=48000*0.94,aresample=48000'
        if ex.get('hasta'):
            cad += f',atrim=0:{ex["hasta"] - ini:.3f}'
        if ex.get('fade_in'):
            cad += f',afade=t=in:d={ex["fade_in"]}'
        if ex.get('fade_out') and ex.get('hasta'):
            cad += f',afade=t=out:st={ex["hasta"] - ini - ex["fade_out"]:.3f}:d={ex["fade_out"]}'
        if ex.get('voz'):
            # la voz: un poco de cuerpo y un eco de sala vieja
            cad += (',highpass=f=70,acompressor=threshold=0.08:ratio=4:attack=5:release=120:makeup=2,'
                    'aecho=0.8:0.5:60|140:0.2|0.1')
        cad += f',volume={vol * (VOZ if ex.get("voz") else 1)},adelay={int(ini * 1000)}|{int(ini * 1000)}[s{i}]'
        filtros.append(cad)
        (voces if ex.get('voz') else resto).append(f'[s{i}]')
    filtros.append(''.join(voces) + f'amix=inputs={len(voces)}:normalize=0,apad=whole_dur={total}[voz]')
    filtros.append('[voz]asplit=2[voz1][vozsc]')
    filtros.append(''.join(resto) + f'amix=inputs={len(resto)}:normalize=0[fondo]')
    # la música y el fondo bajan cuando él habla
    filtros.append('[fondo][vozsc]sidechaincompress=threshold=0.02:ratio=9:attack=15:release=450[fondo2]')
    filtros.append('[fondo2]asplit=2[fondo3][fondo_solo]')
    filtros.append('[voz1]asplit=2[voz2][voz_sola]')
    filtros.append(f'[voz2][fondo3]amix=inputs=2:normalize=0,atrim=0:{total},afade=t=out:st={total - 1.5}:d=1.5[mez]')
    crudo = os.path.join(OUT, f'caudio{CUENTA}_{formato}_crudo.wav')
    cmd = ['ffmpeg', '-v', 'error', '-y'] + entradas + ['-filter_complex', ';'.join(filtros), '-map', '[mez]', '-ar', '48000', crudo,
                                                    '-map', '[voz_sola]', '-ar', '48000', os.path.join(OUT, 'pista_voz.wav'),
                                                    '-map', '[fondo_solo]', '-ar', '48000', os.path.join(OUT, 'pista_fondo.wav')]
    if subprocess.run(cmd).returncode != 0:
        return False
    # a un volumen de video normal (-14 LUFS) sin pasarse del tope
    med = subprocess.run(['ffmpeg', '-hide_banner', '-nostats', '-i', crudo, '-af', 'ebur128', '-f', 'null', '-'], capture_output=True,
                         text=True).stderr
    integ = float(med.split('Integrated loudness:')[1].split('I:')[1].split('LUFS')[0])
    gan = -14.0 - integ
    ruta = os.path.join(OUT, f'caudio{CUENTA}_{formato}.wav')
    r = subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', crudo, '-af', f'volume={gan:.2f}dB,alimiter=limit=0.89:attack=3:release=60',
                        '-ar', '48000', ruta])
    os.remove(crudo)
    print(f'sonido: {integ:.1f} LUFS, +{gan:.1f} dB')
    return r.returncode == 0


def juntar(formato):
    v = os.path.join(OUT, f'cvideo{CUENTA}_{formato}.mp4')
    a = os.path.join(OUT, f'caudio{CUENTA}_{formato}.wav')
    fin = os.path.join(OUT, f'ElColeccionista_Faltan{CUENTA}_{formato}.mp4')
    tmp = fin + '.tmp.mp4'
    r = subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', v, '-i', a, '-map', '0:v', '-map', '1:a', '-c:v', 'copy', '-c:a', 'aac',
                        '-b:a', '256k', '-shortest', '-movflags', '+faststart', tmp])
    if r.returncode == 0:
        os.replace(tmp, fin)
        # el video sin sonido ya no hace falta (ocupa mucho)
        os.remove(v)
        print('listo', fin)


if __name__ == '__main__':
    formato = 'horizontal'
    prueba = hasta = None
    args = sys.argv[1:]
    if args and args[0] in ('horizontal', 'vertical'):
        formato = args.pop(0)
    if '--prueba' in args:
        prueba = [float(x) for x in args[args.index('--prueba') + 1].split(',')]
    if '--hasta' in args:
        hasta = float(args[args.index('--hasta') + 1])
    if prueba:
        render(formato, prueba)
    else:
        if render(formato, None, hasta) and sonido(formato, hasta):
            juntar(formato)
