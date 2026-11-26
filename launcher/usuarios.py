#!/usr/bin/env python3
"""Los usuarios del launcher (cada amigo con su usuario y contraseña). Se guardan solo con su "huella" (PBKDF2 con sal), nunca la
contraseña. Uso:

    python3 launcher/usuarios.py agregar <usuario> <contraseña>
    python3 launcher/usuarios.py quitar <usuario>
    python3 launcher/usuarios.py lista
    python3 launcher/usuarios.py probar <usuario> <contraseña>

Luego se sube usuarios.json al repositorio (el launcher lo lee de ahí). Ojo: el repositorio es público, así que la lista de nombres se
ve; las contraseñas no. Lo que de verdad deja entrar al servidor es la lista blanca de Minecraft del servidor.
"""
import hashlib
import json
import os
import secrets
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
ARCHIVO = os.environ.get('COLECCIONISTA_USUARIOS') or os.path.join(AQUI, 'usuarios.json')
ITERACIONES = 200000


def leer():
    if os.path.exists(ARCHIVO):
        with open(ARCHIVO, encoding='utf-8') as f:
            return json.load(f)
    return {'iteraciones': ITERACIONES, 'usuarios': {}}


def guardar(d):
    with open(ARCHIVO, 'w', encoding='utf-8') as f:
        json.dump(d, f, indent=2, ensure_ascii=False, sort_keys=True)
        f.write('\n')


def agregar(usuario, clave):
    d = leer()
    sal = secrets.token_bytes(16)
    h = hashlib.pbkdf2_hmac('sha256', clave.encode('utf-8'), sal, int(d.get('iteraciones', ITERACIONES))).hex()
    d['usuarios'][usuario.strip().lower()] = {'sal': sal.hex(), 'hash': h}
    guardar(d)
    print('agregado:', usuario.strip().lower())


def main(a):
    if len(a) >= 3 and a[0] == 'agregar':
        agregar(a[1], a[2])
    elif len(a) >= 2 and a[0] == 'quitar':
        d = leer()
        print('quitado' if d['usuarios'].pop(a[1].strip().lower(), None) else 'no existe', a[1])
        guardar(d)
    elif a and a[0] == 'lista':
        for u in sorted(leer()['usuarios']):
            print(u)
    elif len(a) >= 3 and a[0] == 'probar':
        sys.path.insert(0, AQUI)
        import instalador
        print('bien' if instalador.revisar_clave(a[1], a[2], leer()) else 'mal')
    else:
        print(__doc__)


if __name__ == '__main__':
    main(sys.argv[1:])
