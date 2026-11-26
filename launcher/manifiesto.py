#!/usr/bin/env python3
"""Arma launcher/manifiesto.json: lo que el launcher instala (Forge, los mods del evento, la configuración y el servidor).

- el mod del evento sale de la rama jar-coleccionista-1.20.1 (el Core ya va adentro del jar); se calcula su sha1 con git
- Simple Voice Chat, de su descarga oficial (coleccionista/real_mods_1.20.1.txt)
- el servidor: --servidor <dirección> (si no se da, se queda el que ya tenía)

Uso: python3 launcher/manifiesto.py [--servidor mc.ejemplo.com:25565]
"""
import hashlib
import json
import os
import subprocess
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.join(AQUI, '..')
ARCHIVO = os.path.join(AQUI, 'manifiesto.json')
RAW = 'https://raw.githubusercontent.com/Axelis705/minecraft-mods/'
FORGE = '1.20.1-forge-47.4.10'


def git(*a):
    return subprocess.run(['git', '-C', RAIZ] + list(a), capture_output=True, check=True).stdout


def jar_de_rama(rama):
    """El jar de la rama, fijado en la rama evento-jars (que solo crece, nunca se reescribe; la rama del jar sí se reescribe con cada
    compilación): la dirección lleva el commit, así los amigos bajan justo el jar que tiene el servidor aunque después se compile otro."""
    git('fetch', '-q', 'origin', '%s:refs/remotes/origin/%s' % (rama, rama))
    commit = git('rev-parse', 'origin/' + rama).decode().strip()
    arbol = git('rev-parse', commit + '^{tree}').decode().strip()
    padre = None
    try:
        git('fetch', '-q', 'origin', 'evento-jars:refs/remotes/origin/evento-jars')
        padre = git('rev-parse', 'origin/evento-jars').decode().strip()
    except subprocess.CalledProcessError:
        pass
    if padre and git('rev-parse', padre + '^{tree}').decode().strip() == arbol:
        fijo = padre
    else:
        args = ['commit-tree', arbol, '-m', 'jar del evento (%s @ %s)' % (rama, commit[:10])]
        if padre:
            args[2:2] = ['-p', padre]
        fijo = git(*args).decode().strip()
        git('push', '-q', 'origin', fijo + ':refs/heads/evento-jars')
    nombres = [n for n in git('ls-tree', '--name-only', fijo).decode().split() if n.endswith('.jar')]
    nombres = [n for n in nombres if '-dev' not in n and '-sources' not in n]
    n = sorted(nombres)[-1]
    datos = git('show', '%s:%s' % (fijo, n))
    return {'archivo': n, 'url': RAW + fijo + '/' + n, 'sha1': hashlib.sha1(datos).hexdigest(), 'tam': len(datos)}


def externos():
    out = []
    with open(os.path.join(RAIZ, 'coleccionista', 'real_mods_1.20.1.txt'), encoding='utf-8') as f:
        for linea in f:
            linea = linea.strip()
            if linea and not linea.startswith('#'):
                out.append({'archivo': linea.rsplit('/', 1)[-1], 'url': linea})
    return out


def main(a):
    viejo = {}
    if os.path.exists(ARCHIVO):
        with open(ARCHIVO, encoding='utf-8') as f:
            viejo = json.load(f)
    servidor = viejo.get('servidor', '')
    if '--servidor' in a:
        servidor = a[a.index('--servidor') + 1]
    sha_viejos = {m['archivo']: m.get('sha1') for m in viejo.get('mods', [])}
    mods = [jar_de_rama('jar-coleccionista-1.20.1')]
    for m in externos():
        # el sha1 de los de afuera lo pone la prueba del launcher (la nube de Claude no puede bajarlos); si ya se sabía, se queda
        if sha_viejos.get(m['archivo']):
            m['sha1'] = sha_viejos[m['archivo']]
        mods.append(m)
    man = {
        'version': int(viejo.get('version', 0)) + 1,
        'launcher': '1.0.0',
        'nombre': 'El Coleccionista',
        'servidor': servidor,
        'forge': {
            'version': FORGE,
            'instalador': 'https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-installer.jar',
            'sha1': viejo.get('forge', {}).get('sha1', ''),
        },
        'mods': mods,
        # las opciones de Minecraft la primera vez (luego cada quien cambia lo suyo en Configurar)
        'opciones': {
            'lang': 'es_mx',
            'guiScale': '3',
            'onboardAccessibility': 'false',
            'skipMultiplayerWarning': 'true',
            'joinedFirstServer': 'true',
            'tutorialStep': 'none',
            'narrator': '0',
            'renderDistance': '10',
            'simulationDistance': '8',
            'soundCategory_music': '0.6',
            'soundCategory_voice': '1.0',
        },
    }
    with open(ARCHIVO, 'w', encoding='utf-8') as f:
        json.dump(man, f, indent=2, ensure_ascii=False)
        f.write('\n')
    print('manifiesto v%d: %s' % (man['version'], ', '.join(m['archivo'] for m in mods)), '· servidor:', servidor or '(sin servidor)')


if __name__ == '__main__':
    main(sys.argv[1:])
