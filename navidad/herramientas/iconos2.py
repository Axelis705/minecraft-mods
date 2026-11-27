#!/usr/bin/env python3
"""Íconos y archivos sueltos de la etapa del Libro de Navidad y el Pino para decorar: ícono del libro, partícula del pino, estado de
bloque, modelos, botín (el pino sale con sus adornos) y recetas. Uso: python3 herramientas/iconos2.py"""
import json
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
from iconos import dibujo, A  # noqa: E402

D = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'data', 'navidad')


def escribir(ruta, obj):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, 'w', encoding='utf-8') as fh:
        json.dump(obj, fh, indent=1, ensure_ascii=False)


# el libro: pasta roja con esquinas doradas, un pinito dorado al frente y un listón verde
dibujo('libro_navidad', [
    '................',
    '..RRRRRRRRRRRw..',
    '.RrrrrrrrrrrRww.',
    '.RryrrrrrrryRww.',
    '.RrrrrryrrrrRww.',
    '.RrrrryyyrrrRww.',
    '.RrrrryyyrrrRww.',
    '.RrrryyyyyrrRww.',
    '.RrrryyyyyrrRww.',
    '.RrryyyyyyyrRww.',
    '.RrrrrrYrrrrRww.',
    '.RryrrrrrrryRww.',
    '.RrrrrrrrrrrRww.',
    '.RRRRRRRRRRRRwl.',
    '..ggg......LLl..',
    '...g............',
], pal={'l': '#d8c89c', 'L': '#b8a878'})

# la partícula al romper el pino: agujas
dibujo('pino_navidad_hojas', [
    'GgGgGGgGgGGgGgGg',
    'gGeGgGGgeGgGGgGe',
    'GgGgGgeGgGgGeGgG',
    'gGGgGgGgGGgGgGgG',
    'GgeGgGgGgeGgGgGe',
    'gGgGGgGeGgGgGGgG',
    'GgGgGgGgGgGeGgGg',
    'geGgGGgGgGgGgGeG',
    'GgGgeGgGgGGgGgGg',
    'gGgGgGgGeGgGgGgG',
    'GgGeGgGgGgGgeGgG',
    'gGgGgGeGgGgGgGgG',
    'GeGgGgGgGgeGgGgG',
    'gGgGgGgGgGgGgGeg',
    'GgGgeGgGgGgGgGgG',
    'gGgGgGgGeGgGgGgG',
], var=8)
os.remove(os.path.join(A, 'models', 'item', 'pino_navidad_hojas.json'))
os.replace(os.path.join(A, 'textures', 'item', 'pino_navidad_hojas.png'), os.path.join(A, 'textures', 'block', 'pino_navidad_hojas.png'))

# el pino: estado de bloque (las dos mitades solo dan la partícula: lo dibuja el renderizador), modelo y objeto 3D
escribir(os.path.join(A, 'blockstates', 'pino_navidad.json'), {'variants': {
    'half=lower': {'model': 'navidad:block/pino_navidad'}, 'half=upper': {'model': 'navidad:block/pino_navidad'}}})
escribir(os.path.join(A, 'models', 'block', 'pino_navidad.json'), {'textures': {'particle': 'navidad:block/pino_navidad_hojas'}})
escribir(os.path.join(A, 'models', 'item', 'pino_navidad.json'), {
    'parent': 'minecraft:builtin/entity', 'gui_light': 'front',
    'textures': {'particle': 'navidad:block/pino_navidad_hojas'},
    'display': {
        'gui': {'rotation': [20, 30, 0], 'translation': [0, -3.6, 0], 'scale': [0.36, 0.36, 0.36]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.25, 0.25, 0.25]},
        'fixed': {'rotation': [0, 0, 0], 'translation': [0, -3, 0], 'scale': [0.4, 0.4, 0.4]},
        'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 1, 2], 'scale': [0.25, 0.25, 0.25]},
        'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 1, 2], 'scale': [0.25, 0.25, 0.25]},
        'firstperson_righthand': {'rotation': [0, 30, 0], 'translation': [0, -1, 0], 'scale': [0.3, 0.3, 0.3]},
        'firstperson_lefthand': {'rotation': [0, 30, 0], 'translation': [0, -1, 0], 'scale': [0.3, 0.3, 0.3]},
    }})

# botín: solo de la mitad de abajo, con sus adornos puestos
escribir(os.path.join(D, 'loot_tables', 'blocks', 'pino_navidad.json'), {'type': 'minecraft:block', 'pools': [{
    'rolls': 1,
    'entries': [{'type': 'minecraft:item', 'name': 'navidad:pino_navidad', 'functions': [{
        'function': 'minecraft:copy_nbt', 'source': 'block_entity', 'ops': [
            {'source': 'Adornos', 'target': 'BlockEntityTag.Adornos', 'op': 'replace'},
            {'source': 'Luces', 'target': 'BlockEntityTag.Luces', 'op': 'replace'},
            {'source': 'Guirnaldas', 'target': 'BlockEntityTag.Guirnaldas', 'op': 'replace'},
            {'source': 'Punta', 'target': 'BlockEntityTag.Punta', 'op': 'replace'}]}]}],
    'conditions': [{'condition': 'minecraft:block_state_property', 'block': 'navidad:pino_navidad', 'properties': {'half': 'lower'}},
                   {'condition': 'minecraft:survives_explosion'}]}]})

# recetas (con nombres "arbol_..." para no chocar con las del cortapiedras)
R = os.path.join(D, 'recipes')


def it(x):
    return {'tag': x[1:]} if x.startswith('#') else {'item': x if ':' in x else 'minecraft:' + x}


def sin_forma(nombre, resultado, n, ingr):
    escribir(os.path.join(R, nombre + '.json'), {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                                 'ingredients': [it(i) for i in ingr], 'result': {'item': 'navidad:' + resultado, 'count': n}})


def con_forma(nombre, resultado, n, patron, llaves):
    escribir(os.path.join(R, nombre + '.json'), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': patron,
                                                 'key': {k: it(v) for k, v in llaves.items()}, 'result': {'item': 'navidad:' + resultado, 'count': n}})


con_forma('arbol_pino_navidad', 'pino_navidad', 1, [' S ', 'LLL', ' T '], {'S': 'spruce_sapling', 'L': 'spruce_leaves', 'T': 'spruce_log'})
sin_forma('arbol_libro_navidad', 'libro_navidad', 1, ['book', 'red_dye', 'spruce_sapling'])
sin_forma('arbol_esfera_roja', 'adorno_esfera_roja', 3, ['glass_pane', 'gold_nugget', 'red_dye'])
sin_forma('arbol_esfera_azul', 'adorno_esfera_azul', 3, ['glass_pane', 'gold_nugget', 'blue_dye', 'snowball'])
sin_forma('arbol_esfera_dorada', 'adorno_esfera_dorada', 3, ['glass_pane', 'gold_nugget', 'gold_nugget', 'yellow_dye'])
sin_forma('arbol_baston', 'adorno_baston', 3, ['navidad:baston_caramelo', 'string'])
sin_forma('arbol_mono', 'adorno_mono', 2, ['red_wool', 'string'])
sin_forma('arbol_campanita', 'adorno_campanita', 2, ['gold_nugget', 'gold_nugget', 'gold_nugget', 'red_dye', 'string'])
sin_forma('arbol_copo', 'adorno_copo', 2, ['snowball', 'snowball', 'quartz', 'string'])
sin_forma('arbol_galleta', 'adorno_galleta', 2, ['navidad:galleta_jengibre', 'navidad:galleta_jengibre', 'string'])
sin_forma('arbol_pina', 'adorno_pina', 2, ['spruce_sapling', 'snowball', 'string'])
sin_forma('arbol_luces', 'adorno_luces', 1, ['glowstone_dust', 'glowstone_dust', 'red_dye', 'blue_dye', 'yellow_dye', 'string', 'string'])
sin_forma('arbol_guirnalda', 'adorno_guirnalda', 1, ['gold_nugget', 'gold_nugget', 'gold_nugget', 'gold_nugget', 'string', 'string'])
con_forma('arbol_estrella', 'punta_estrella', 1, [' G ', 'GLG', ' I '], {'G': 'gold_ingot', 'L': 'glowstone', 'I': 'iron_nugget'})
print('libro, pino, botín y recetas listos')
