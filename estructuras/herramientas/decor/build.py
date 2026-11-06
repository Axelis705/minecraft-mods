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
# constantes en Java para los disenos (evitan errores de escritura) y para que Soporte conozca las reglas de apoyo de cada bloque
java = os.path.join(root, '..', 'java', 'com', 'mundosdeaxelis', 'world', 'EstIds.java')
if len(sys.argv) <= 1:
    L = ['package com.mundosdeaxelis.world;', '', '/** GENERADO por herramientas/decor/build.py - no editar a mano. Nombres de la decoracion nueva de las estructuras y sus reglas de apoyo. */',
         'public final class EstIds {', '    private EstIds() {', '    }', '']
    for f in ficha:
        L.append('    public static final String %s = "mundos_de_axelis:%s";' % (f['id'][4:].upper(), f['id']))
    L += ['', '    /** Tipo de apoyo: "wall" (necesita muro detras), "soft" (tapiz: no necesita), "hang" (techo), "floor" (suelo), "" si no es de estas. */',
          '    public static String support(String name) {', '        if (name.startsWith("mundos_de_axelis:")) {', '            name = name.substring(17);', '        }', '        return switch (name) {']
    for f in ficha:
        kind = 'soft' if f.get('soft') else ('wall' if f['kind'] == 'wall' else ('hang' if f['kind'] == 'hang' else 'floor'))
        L.append('            case "%s" -> "%s";' % (f['id'], kind))
    L += ['            default -> "";', '        };', '    }', '', '    /** true si se puede atravesar (tapices, farolillos, jaulas colgantes...): el recorrido de comprobacion lo cuenta como aire. */',
          '    public static boolean passable(String name) {', '        if (name.startsWith("mundos_de_axelis:")) {', '            name = name.substring(17);', '        }', '        return switch (name) {']
    nc = [f['id'] for f in ficha if not f['collide']]
    L.append('            case ' + ', '.join('"%s"' % t for t in nc) + ' -> true;')
    L += ['            default -> false;', '        };', '    }', '', '    public static boolean isTall(String name) {', '        if (name.startsWith("mundos_de_axelis:")) {',
          '            name = name.substring(17);', '        }', '        return switch (name) {']
    tall = [f['id'] for f in ficha if f['tall']]
    L.append('            case ' + ', '.join('"%s"' % t for t in tall) + ' -> true;')
    L += ['            default -> false;', '        };', '    }', '}', '']
    open(os.path.abspath(java), 'w').write('\n'.join(L))
print(len(ficha), 'decoraciones ->', root)
