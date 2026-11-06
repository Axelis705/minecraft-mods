#!/usr/bin/env python3
"""Fabrica toda la decoracion de las estructuras.

Uso: build.py [carpeta_de_recursos]   (por defecto estructuras/src/main/resources)
Escribe assets/<ns>/{blockstates,models,textures}, data/<ns>/loot_tables, el manifiesto est_decor.json y los textos en es/en.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import kit  # noqa: E402
import props  # noqa: E402

root = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'src', 'main', 'resources')
root = os.path.abspath(root)
ficha = []
for p in props.all_props():
    ficha.append(kit.write_prop(p, root))
    print('ok', p.id)
os.makedirs(os.path.join(root, 'data', kit.NS), exist_ok=True)
json.dump({'blocks': ficha}, open(os.path.join(root, 'data', kit.NS, 'est_decor.json'), 'w'), indent=1, ensure_ascii=False)
# textos: espacio de nombres propio para no mezclarse con los archivos de idioma del mod
lang_es = {'itemGroup.mundos_estructuras': 'Mundos de Axelis: Decoración de estructuras'}
lang_en = {'itemGroup.mundos_estructuras': 'Mundos de Axelis: Structure decor'}
for f in ficha:
    lang_es['block.mundos_estructuras.' + f['id']] = f['es']
    lang_en['block.mundos_estructuras.' + f['id']] = f['en']
ld = os.path.join(root, 'assets', 'mundos_estructuras', 'lang')
os.makedirs(ld, exist_ok=True)
for code, d in (('es_es', lang_es), ('es_mx', lang_es), ('es_ar', lang_es), ('es_cl', lang_es), ('es_ve', lang_es), ('es_uy', lang_es), ('en_us', lang_en), ('en_gb', lang_en)):
    json.dump(d, open(os.path.join(ld, code + '.json'), 'w'), indent=1, ensure_ascii=False)
print(len(ficha), 'decoraciones ->', root)
