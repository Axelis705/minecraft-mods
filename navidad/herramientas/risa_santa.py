#!/usr/bin/env python3
"""La risa de Santa ("¡jo, jo, jo!") a partir de una grabación de dominio público (CC0, BigSoundBank: ver herramientas/fuentes/LEEME.md).
Saca tres versiones para que no suene siempre igual: un poquito más grave (más Santa), otra más grave y lenta, y una corta.
A cada una: tono más bajo (rubberband), un poco de cuerpo en los graves, un eco chiquito de cuarto, volumen parejo y salida mono
(para que se oiga de dónde viene el trineo).  Uso: python3 herramientas/risa_santa.py  (necesita ffmpeg con rubberband)"""
import os
import subprocess

AQUI = os.path.dirname(os.path.abspath(__file__))
FUENTE = os.path.join(AQUI, 'fuentes', 'santa_oh_oh_oh_4_bigsoundbank_cc0.wav')
SALIDA = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad', 'sounds')

VERSIONES = [
    # (archivo, tono, tempo, desde, hasta)
    ('risa_santa1', 0.93, 1.0, 0.0, None),
    ('risa_santa2', 0.88, 0.96, 0.0, None),
    ('risa_santa3', 0.91, 1.0, 0.36, None),
]

for nombre, tono, tempo, desde, hasta in VERSIONES:
    filtros = []
    if desde or hasta:
        filtros.append(f'atrim=start={desde}' + (f':end={hasta}' if hasta else '') + ',asetpts=PTS-STARTPTS')
    filtros += [
        f'rubberband=pitch={tono}:tempo={tempo}',
        'highpass=f=70',
        'equalizer=f=160:t=q:w=1.2:g=3',
        'equalizer=f=3200:t=q:w=1.5:g=1.5',
        'aecho=0.85:0.5:38|71:0.16|0.10',
        'loudnorm=I=-13:TP=-1.2:LRA=9',
        'afade=t=in:d=0.02',
        'areverse,afade=t=in:d=0.12,areverse',
    ]
    destino = os.path.join(SALIDA, nombre + '.ogg')
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', FUENTE, '-af', ','.join(filtros), '-ac', '1', '-ar', '44100',
                    '-c:a', 'libvorbis', '-q:a', '5', destino], check=True)
    print(nombre, 'listo')
