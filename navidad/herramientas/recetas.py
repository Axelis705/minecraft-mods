#!/usr/bin/env python3
"""Recetas de Navidad de Axelis: comida, regalos, ropa y una "caja de adornos" que en el cortapiedras se vuelve cualquiera de los
bloques de decoración (así no hacen falta 57 recetas distintas). Uso: python3 herramientas/recetas.py"""
import json, os

AQUI = os.path.dirname(os.path.abspath(__file__))
R = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'data', 'navidad', 'recipes')
DECO = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'data', 'navidad', 'deco.json')


def it(x):
    if x.startswith('#'):
        return {'tag': x[1:]}
    return {'item': x if ':' in x else 'minecraft:' + x}


def sin_forma(nombre, resultado, n, ingredientes):
    j = {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [it(i) for i in ingredientes],
         'result': {'item': 'navidad:' + resultado, 'count': n}}
    json.dump(j, open(os.path.join(R, nombre + '.json'), 'w'), indent=1)


def con_forma(nombre, resultado, n, patron, llaves):
    j = {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': patron, 'key': {k: it(v) for k, v in llaves.items()},
         'result': {'item': 'navidad:' + resultado, 'count': n}}
    json.dump(j, open(os.path.join(R, nombre + '.json'), 'w'), indent=1)


def corte(nombre, resultado, n):
    j = {'type': 'minecraft:stonecutting', 'ingredient': {'item': 'navidad:caja_adornos'}, 'result': 'navidad:' + resultado, 'count': n}
    json.dump(j, open(os.path.join(R, nombre + '.json'), 'w'), indent=1)


if __name__ == '__main__':
    os.makedirs(R, exist_ok=True)
    for f in os.listdir(R):
        os.remove(os.path.join(R, f))
    # comida
    sin_forma('galleta_jengibre', 'galleta_jengibre', 4, ['wheat', 'wheat', 'sugar', 'cocoa_beans', 'honey_bottle'])
    sin_forma('baston_caramelo', 'baston_caramelo', 3, ['sugar', 'sugar', 'red_dye'])
    sin_forma('dulce', 'dulce', 6, ['sugar', 'sugar', 'honey_bottle'])
    sin_forma('tamal', 'tamal', 3, ['wheat', 'wheat', 'cooked_porkchop', 'red_dye', 'paper'])
    sin_forma('bunuelo', 'bunuelo', 4, ['wheat', 'sugar', 'egg', 'honey_bottle'])
    sin_forma('chocolate_caliente', 'chocolate_caliente', 3, ['cocoa_beans', 'cocoa_beans', 'sugar', 'milk_bucket'])
    sin_forma('champurrado', 'champurrado', 3, ['cocoa_beans', 'wheat', 'sugar', 'milk_bucket'])
    sin_forma('ponche', 'ponche', 3, ['navidad:tejocote', 'navidad:mandarina', 'navidad:cana', 'apple', 'sugar', 'water_bucket'])
    sin_forma('ponche_manzana', 'ponche', 2, ['apple', 'apple', 'sweet_berries', 'sugar', 'water_bucket'])
    sin_forma('rosca_reyes', 'rosca_reyes', 1, ['wheat', 'wheat', 'wheat', 'sugar', 'egg', 'sweet_berries', 'glow_berries'])
    sin_forma('bengala', 'bengala', 4, ['gunpowder', 'stick', 'glowstone_dust'])
    sin_forma('pavo_navideno', 'pavo_navideno', 1, ['cooked_chicken', 'cooked_chicken', 'baked_potato', 'carrot', 'apple'])
    # regalos y posadas
    sin_forma('pinata', 'pinata', 1, ['paper', 'paper', 'paper', 'paper', 'red_dye', 'yellow_dye', 'green_dye', 'string', 'sugar'])
    sin_forma('palo_pinata', 'palo_pinata', 1, ['stick', 'stick', 'red_dye', 'white_dye'])
    sin_forma('venda', 'venda', 1, ['black_carpet', 'string'])
    sin_forma('caja_regalo', 'caja_regalo', 1, ['chest', 'paper', 'paper', 'red_dye', 'string'])
    con_forma('calendario_adviento', 'calendario_adviento', 1, [' G ', 'PAP', 'PAP'], {'G': 'gold_nugget', 'P': '#minecraft:planks', 'A': 'paper'})
    sin_forma('caja_adornos', 'caja_adornos', 4, ['paper', 'red_dye', 'green_dye', 'gold_nugget'])
    # ropa
    sin_forma('gorro_santa', 'gorro_santa', 1, ['red_wool', 'red_wool', 'white_wool'])
    sin_forma('gorro_duende', 'gorro_duende', 1, ['green_wool', 'green_wool', 'red_wool', 'gold_nugget'])
    sin_forma('cuernos_reno', 'cuernos_reno', 1, ['stick', 'stick', 'brown_dye', 'string', 'red_dye'])
    sin_forma('sueter_reno', 'sueter_reno', 1, ['red_wool', 'red_wool', 'red_wool', 'red_wool', 'white_wool', 'brown_dye'])
    sin_forma('sueter_pinos', 'sueter_pinos', 1, ['white_wool', 'white_wool', 'white_wool', 'white_wool', 'green_wool', 'red_dye'])
    sin_forma('sueter_copos', 'sueter_copos', 1, ['blue_wool', 'blue_wool', 'blue_wool', 'blue_wool', 'white_wool', 'snowball'])
    sin_forma('traje_santa', 'traje_santa', 1, ['red_wool', 'red_wool', 'red_wool', 'red_wool', 'red_wool', 'white_wool', 'white_wool',
                                                'black_wool', 'gold_ingot'])
    # invierno y Año Nuevo
    con_forma('trineo_madera', 'trineo_madera', 1, ['S S', 'PPP', 'I I'], {'S': 'stick', 'P': '#minecraft:planks', 'I': 'iron_ingot'})
    sin_forma('patines', 'patines', 1, ['leather', 'leather', 'white_wool', 'iron_ingot', 'iron_ingot'])
    sin_forma('uvas', 'uvas', 6, ['sweet_berries', 'sweet_berries', 'purple_dye'])
    con_forma('buzon_santa', 'buzon_santa', 1, ['IRI', 'IPI', ' S '], {'I': 'iron_ingot', 'R': 'red_dye', 'P': 'paper', 'S': 'stick'})
    # decoración: caja de adornos -> cualquier bloque en el cortapiedras
    deco = json.load(open(DECO, encoding='utf-8'))['blocks']
    for b in deco:
        n = 8 if b['kind'] in ('cubo', 'alfombra', 'vidrio') else 1
        corte('adorno_' + b['id'], b['id'], n)
    print(len(os.listdir(R)), 'recetas')
