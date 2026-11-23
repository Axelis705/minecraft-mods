#!/usr/bin/env python3
"""Fabrica todos los bloques nuevos del complejo del Coleccionista (texturas, modelos, estados de bloque, objetos, textos y el
manifiesto que lee el mod para registrarlos).

Uso: python3 herramientas/deco/build.py
Escribe en coleccionista/src/main/resources: assets/coleccionista/{blockstates,models,textures}, data/coleccionista/deco.json,
data/minecraft/tags/blocks/beds.json y los nombres en los archivos de idioma.

Tipos (campo "kind" del manifiesto):
  cubo      bloque entero (con variantes al azar para que las paredes no se repitan)
  pilar     bloque con eje (vigas)
  losa      media losa (rejilla de las pasarelas)
  escalera  escaleras
  barrotes  barrotes que se unen como los de hierro (rejas, barandales)
  puerta    puerta de dos bloques que solo abre el evento (como la de hierro)
  vidrio    vidrio
  alfombra  alfombra (1 px)
  prop      mueble o adorno 3D (catre, retrete, lamparas, camaras...), ver props_celdas.py
"""
import json
import os
import random
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
import kit  # noqa: E402
import materiales as M  # noqa: E402
import props_celdas  # noqa: E402

NS = 'coleccionista'
ROOT = os.path.abspath(os.path.join(AQUI, '..', '..', 'src', 'main', 'resources'))
A = os.path.join(ROOT, 'assets', NS)
for sub in ('blockstates', 'models/block', 'models/item', 'textures/block'):
    os.makedirs(os.path.join(A, sub), exist_ok=True)

ficha = []
nombres = {}


def dump(rel, data):
    path = os.path.join(A, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=1, ensure_ascii=False)


def tex(name, image):
    image.save(os.path.join(A, 'textures/block', name + '.png'))
    return f'{NS}:block/{name}'


def nombre(bid, es, en):
    nombres[bid] = (es, en)


# ---------------------------------------------------------------- bloques enteros


def cubo(bid, es, en, fn, variantes=4, sonido='stone', top=None, render=None, dureza=2.0):
    """Bloque entero con `variantes` texturas al azar (fn(seed) -> imagen). `top`: funcion aparte para arriba y abajo."""
    models = []
    for i in range(variantes):
        t = tex(f'{bid}_{i}' if variantes > 1 else bid, fn(i * 13 + 1))
        if top:
            tt = tex(f'{bid}_tapa_{i}' if variantes > 1 else f'{bid}_tapa', top(i * 13 + 1))
            m = {'parent': 'minecraft:block/cube_column', 'textures': {'side': t, 'end': tt}}
        else:
            m = {'parent': 'minecraft:block/cube_all', 'textures': {'all': t}}
        if render:
            m['render_type'] = 'minecraft:' + render
        mn = f'{bid}_{i}' if variantes > 1 else bid
        dump(f'models/block/{mn}.json', m)
        models.append({'model': f'{NS}:block/{mn}'})
    dump(f'blockstates/{bid}.json', {'variants': {'': models if len(models) > 1 else models[0]}})
    dump(f'models/item/{bid}.json', {'parent': models[0]['model']})
    ficha.append({'id': bid, 'kind': 'cubo', 'sound': sonido, 'strength': dureza})
    nombre(bid, es, en)


def pilar(bid, es, en, lado, tapa, sonido='metal'):
    s = tex(bid, lado(1))
    e = tex(bid + '_tapa', tapa(1))
    dump(f'models/block/{bid}.json', {'parent': 'minecraft:block/cube_column', 'textures': {'side': s, 'end': e}})
    dump(f'models/block/{bid}_horizontal.json', {'parent': 'minecraft:block/cube_column_horizontal', 'textures': {'side': s, 'end': e}})
    m, h = f'{NS}:block/{bid}', f'{NS}:block/{bid}_horizontal'
    dump(f'blockstates/{bid}.json', {'variants': {'axis=y': {'model': m}, 'axis=z': {'model': h, 'x': 90}, 'axis=x': {'model': h, 'x': 90, 'y': 90}}})
    dump(f'models/item/{bid}.json', {'parent': m})
    ficha.append({'id': bid, 'kind': 'pilar', 'sound': sonido, 'strength': 3.0})
    nombre(bid, es, en)


def losa(bid, es, en, fn, sonido='metal', render=None):
    t = tex(bid, fn(1))
    base = {'textures': {'bottom': t, 'top': t, 'side': t}}
    if render:
        base['render_type'] = 'minecraft:' + render
    dump(f'models/block/{bid}.json', dict(base, parent='minecraft:block/slab'))
    dump(f'models/block/{bid}_top.json', dict(base, parent='minecraft:block/slab_top'))
    d = {'parent': 'minecraft:block/cube_all', 'textures': {'all': t}}
    if render:
        d['render_type'] = 'minecraft:' + render
    dump(f'models/block/{bid}_doble.json', d)
    dump(f'blockstates/{bid}.json', {'variants': {'type=bottom': {'model': f'{NS}:block/{bid}'}, 'type=top': {'model': f'{NS}:block/{bid}_top'},
                                                  'type=double': {'model': f'{NS}:block/{bid}_doble'}}})
    dump(f'models/item/{bid}.json', {'parent': f'{NS}:block/{bid}'})
    ficha.append({'id': bid, 'kind': 'losa', 'sound': sonido, 'strength': 2.0, 'transparente': render is not None})
    nombre(bid, es, en)


def escalera(bid, es, en, fn, base, sonido='metal'):
    t = tex(bid, fn(1))
    tx = {'bottom': t, 'top': t, 'side': t}
    for suf, par in (('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')):
        dump(f'models/block/{bid}{suf}.json', {'parent': f'minecraft:block/{par}', 'textures': tx})
    vb = json.load(open('/tmp/mcassets/assets/minecraft/blockstates/oak_stairs.json'))
    txt = json.dumps(vb).replace('minecraft:block/oak_stairs', f'{NS}:block/{bid}')
    dump(f'blockstates/{bid}.json', json.loads(txt))
    dump(f'models/item/{bid}.json', {'parent': f'{NS}:block/{bid}'})
    ficha.append({'id': bid, 'kind': 'escalera', 'base': base, 'sound': sonido, 'strength': 2.0})
    nombre(bid, es, en)


def barrotes(bid, es, en, fn, sonido='metal'):
    t = tex(bid, fn(1))
    for parte in ('cap', 'cap_alt', 'post', 'post_ends', 'side', 'side_alt'):
        m = json.load(open(f'/tmp/mcassets/assets/minecraft/models/block/iron_bars_{parte}.json'))
        m['textures'] = {'particle': t, 'bars': t, 'edge': t}
        m['render_type'] = 'minecraft:cutout'
        dump(f'models/block/{bid}_{parte}.json', m)
    vb = json.load(open('/tmp/mcassets/assets/minecraft/blockstates/iron_bars.json'))
    txt = json.dumps(vb).replace('minecraft:block/iron_bars', f'{NS}:block/{bid}')
    dump(f'blockstates/{bid}.json', json.loads(txt))
    dump(f'models/item/{bid}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': t}})
    ficha.append({'id': bid, 'kind': 'barrotes', 'sound': sonido, 'strength': 5.0})
    nombre(bid, es, en)


def puerta(bid, es, en, abajo, arriba, sonido='metal'):
    b = tex(bid + '_abajo', abajo(1))
    u = tex(bid + '_arriba', arriba(1))
    for parte in ('bottom_left', 'bottom_left_open', 'bottom_right', 'bottom_right_open', 'top_left', 'top_left_open', 'top_right', 'top_right_open'):
        dump(f'models/block/{bid}_{parte}.json', {'parent': f'minecraft:block/door_{parte}', 'textures': {'bottom': b, 'top': u},
                                                  'render_type': 'minecraft:cutout'})
    vb = json.load(open('/tmp/mcassets/assets/minecraft/blockstates/iron_door.json'))
    txt = json.dumps(vb).replace('minecraft:block/iron_door', f'{NS}:block/{bid}')
    dump(f'blockstates/{bid}.json', json.loads(txt))
    # el objeto: dibujo de la puerta entera
    from PIL import Image
    ic = Image.new('RGBA', (32, 64))
    ic.paste(arriba(1), (0, 0))
    ic.paste(abajo(1), (0, 32))
    ic = ic.resize((16, 32)).crop((0, 0, 16, 32))
    icon = Image.new('RGBA', (32, 32))
    icon.paste(ic.resize((16, 32)), (8, 0))
    tex(bid + '_objeto', icon)
    dump(f'models/item/{bid}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:block/{bid}_objeto'}})
    ficha.append({'id': bid, 'kind': 'puerta', 'sound': sonido, 'strength': 5.0})
    nombre(bid, es, en)


def tubo(bid, es, en, fn, sonido='metal'):
    """Tubo con bridas que va en cualquier eje (por el techo y las paredes)."""
    t = tex(bid, fn(1))
    els = [{'from': [5.5, 0, 5.5], 'to': [10.5, 16, 10.5], 'faces': {f: {'texture': '#t'} for f in ('north', 'south', 'west', 'east')}},
           {'from': [4.5, 0, 4.5], 'to': [11.5, 1.5, 11.5], 'faces': {f: {'texture': '#t'} for f in ('north', 'south', 'west', 'east', 'down', 'up')}},
           {'from': [4.5, 14.5, 4.5], 'to': [11.5, 16, 11.5], 'faces': {f: {'texture': '#t'} for f in ('north', 'south', 'west', 'east', 'down', 'up')}}]
    els[1]['faces']['down']['cullface'] = 'down'
    els[2]['faces']['up']['cullface'] = 'up'
    dump(f'models/block/{bid}.json', {'textures': {'t': t, 'particle': t}, 'elements': els})
    m = f'{NS}:block/{bid}'
    dump(f'blockstates/{bid}.json', {'variants': {'axis=y': {'model': m}, 'axis=z': {'model': m, 'x': 90}, 'axis=x': {'model': m, 'x': 90, 'y': 90}}})
    dump(f'models/item/{bid}.json', {'parent': m})
    ficha.append({'id': bid, 'kind': 'tubo', 'sound': sonido, 'strength': 2.0})
    nombre(bid, es, en)


def alfombra(bid, es, en, fn):
    t = tex(bid, fn(1))
    dump(f'models/block/{bid}.json', {'parent': 'minecraft:block/carpet', 'textures': {'wool': t}})
    dump(f'blockstates/{bid}.json', {'variants': {'': {'model': f'{NS}:block/{bid}'}}})
    dump(f'models/item/{bid}.json', {'parent': f'{NS}:block/{bid}'})
    ficha.append({'id': bid, 'kind': 'alfombra', 'sound': 'wool', 'strength': 0.2})
    nombre(bid, es, en)


# ---------------------------------------------------------------- muebles (kit)


def prop(p, extra=None):
    f = kit.write_prop(p, ROOT)
    f['kind'] = 'prop'
    f['sub'] = p.kind
    f.update(extra or {})
    ficha.append(f)
    nombre(p.id, p.es, p.en)
    return f


def cama(p):
    """Catre: dos partes (cabecera y pies) como una cama de verdad (se puede dormir: el evento acuesta a las piezas)."""
    models, img, alpha = kit.build(p)
    img.save(os.path.join(A, 'textures/block', p.id + '.png'))
    for part in ('head', 'foot'):
        j = {'textures': {'0': f'{NS}:block/{p.id}', 'particle': f'{NS}:block/{p.id}'}, 'elements': models[part], 'display': kit.DISPLAY}
        if alpha:
            j['render_type'] = 'minecraft:cutout'
        dump(f'models/block/{p.id}_{part}.json', j)
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    v = {}
    for f, y in rot.items():
        for part in ('head', 'foot'):
            e = {'model': f'{NS}:block/{p.id}_{part}'}
            if y:
                e['y'] = y
            v[f'facing={f},part={part}'] = e
    dump(f'blockstates/{p.id}.json', {'variants': v})
    dump(f'models/item/{p.id}.json', {'parent': f'{NS}:block/{p.id}_foot'})
    ficha.append({'id': p.id, 'kind': 'cama', 'sound': 'metal', 'strength': 1.0})
    nombre(p.id, p.es, p.en)


def con_apagado(on, off, extra=None):
    """Lampara con dos modelos (encendida y apagada): el bloque cambia solo de uno a otro (parpadeo) o lo cambia el evento."""
    f = prop(on, extra)
    kit.write_prop(off, ROOT)
    for rel in (f'blockstates/{off.id}.json', f'models/item/{off.id}.json'):
        try:
            os.remove(os.path.join(A, rel))
        except FileNotFoundError:
            pass
    lt = os.path.join(ROOT, 'data', NS, 'loot_tables/blocks', off.id + '.json')
    if os.path.exists(lt):
        os.remove(lt)
    bs = json.load(open(os.path.join(A, f'blockstates/{on.id}.json')))
    nv = {}
    fallas = ('false', 'true') if (extra or {}).get('parpadeo') else (None,)
    for k, v in bs['variants'].items():
        for enc in ('true', 'false'):
            for fa in fallas:
                e = dict(v)
                prendida = enc == 'true' and fa != 'true'
                e['model'] = v['model'] if prendida else v['model'].replace(on.id, off.id)
                key = (k + ',' if k else '') + 'encendida=' + enc + (',falla=' + fa if fa else '')
                nv[key] = e
    dump(f'blockstates/{on.id}.json', {'variants': nv})
    f['apagable'] = True
    return f


def solo_modelos(p):
    """Solo el modelo y la textura (para bloques que ya tienen su estado, como el foco o el candado): no toca su estado ni su objeto."""
    guardar = {}
    for rel in (f'blockstates/{p.id}.json', f'models/item/{p.id}.json'):
        path = os.path.join(A, rel)
        if os.path.exists(path):
            guardar[path] = open(path, encoding='utf-8').read()
    kit.write_prop(p, ROOT)
    for rel in (f'blockstates/{p.id}.json', f'models/item/{p.id}.json'):
        path = os.path.join(A, rel)
        if path in guardar:
            open(path, 'w', encoding='utf-8').write(guardar[path])
        elif os.path.exists(path):
            os.remove(path)
    lt = os.path.join(ROOT, 'data', NS, 'loot_tables/blocks', p.id + '.json')
    if os.path.exists(lt):
        os.remove(lt)


# ---------------------------------------------------------------- el catalogo

def catalogo():
    # paredes y pisos
    cubo('azulejo', 'Azulejo de hospital', 'Hospital tile', lambda s: M.azulejo(s))
    cubo('azulejo_sucio', 'Azulejo sucio', 'Dirty tile', lambda s: M.azulejo(s, sucio=0.55, bajo=0.55, faltan=0.12))
    cubo('azulejo_remate', 'Azulejo de remate', 'Tile trim', lambda s: M.remate(s))
    cubo('yeso', 'Yeso viejo', 'Old plaster', lambda s: M.yeso(s))
    cubo('concreto', 'Concreto', 'Concrete', lambda s: M.concreto(s))
    cubo('concreto_piso', 'Piso de concreto', 'Concrete floor', lambda s: M.concreto_piso(s))
    cubo('linoleo', 'Piso de cuadros', 'Checkered floor', lambda s: M.linoleo(s))
    cubo('ladrillo_viejo', 'Ladrillo viejo', 'Old brick', lambda s: M.ladrillo(s))
    cubo('metal_placa', 'Lámina de acero', 'Steel plate', lambda s: M.metal_placa(s), sonido='metal')
    cubo('metal_negro', 'Metal negro', 'Black metal', lambda s: M.metal_negro(s), variantes=2, sonido='metal')
    cubo('panel_madera', 'Panel de madera', 'Wood panel', lambda s: M.madera_panel(s), variantes=2, sonido='wood')
    cubo('coladera', 'Coladera', 'Floor drain', lambda s: M.concreto_piso(s), variantes=1, top=lambda s: coladera_tapa(s))
    cubo('vidrio_sucio', 'Vidrio sucio', 'Dirty glass', lambda s: M.vidrio_sucio(s), variantes=1, sonido='glass', render='translucent', dureza=0.3)
    ficha[-1]['kind'] = 'vidrio'
    pilar('viga', 'Viga de acero', 'Steel beam', lambda s: M.viga_lado(s), lambda s: M.viga_tapa(s))
    losa('rejilla_piso', 'Rejilla de pasarela', 'Catwalk grating', lambda s: M.rejilla(s), render='cutout')
    escalera('escalera_metal', 'Escalera de metal', 'Metal stairs', lambda s: M.metal_placa(s, '#2e2f2c', pintado=0.3), 'metal_placa')
    barrotes('barrotes', 'Barrotes oxidados', 'Rusty bars', lambda s: M.barrotes(s))
    barrotes('barandal', 'Barandal', 'Railing', lambda s: barandal_tex(s))
    puerta('puerta_celda', 'Puerta de celda', 'Cell door', lambda s: M.puerta(s, 'abajo'), lambda s: M.puerta(s, 'arriba'))
    alfombra('alfombra_roja', 'Alfombra roja', 'Red carpet', lambda s: M.alfombra(s))
    tubo('tubo', 'Tubería oxidada', 'Rusty pipe', lambda s: M.metal_placa(s, '#4d5249', pintado=0.5, remaches=False))
    # muebles y adornos
    P = props_celdas
    cama(P.catre())
    prop(P.retrete())
    prop(P.lavabo())
    prop(P.espejo())
    prop(P.cajon(), {'cajon': True})
    prop(P.silla())
    con_apagado(P.lampara_tubo(True), P.lampara_tubo(False), {'parpadeo': True, 'zumbido': True})
    con_apagado(P.lampara_jaula(True), P.lampara_jaula(False), {'parpadeo': True})
    for p in (P.rejilla_ventilacion(), P.camara(), P.bocina(), P.reloj(), P.telarana()):
        prop(p)
    prop(P.calcomania('mancha', 'Mancha', 'Stain', P.mancha_mat(), 40))
    prop(P.calcomania('charco', 'Charco', 'Puddle', P.mancha_mat('#28343a', 150, 41), 41), {'goteo': True})
    prop(P.calcomania('papeles', 'Papeles tirados', 'Scattered papers', P.papeles_mat(), 42))
    prop(P.vitrina())
    prop(P.candelabro())
    prop(P.cortina())
    prop(P.placa(), {'texto': 'placa'})
    prop(P.letrero(), {'texto': 'letrero'})
    prop(P.rayon(), {'texto': 'rayon'})
    for p in P.focos():
        solo_modelos(p)
    solo_modelos(P.candado())


def coladera_tapa(seed):
    import numpy as np
    base = np.array(M.concreto_piso(seed)).astype(np.float32)
    y, x = np.mgrid[0:32, 0:32]
    d = np.maximum(np.abs(x - 15.5), np.abs(y - 15.5))
    ring = (d >= 9) & (d <= 11)
    inner = d < 9
    slot = inner & ((x % 4) < 2)
    base[..., :3] = np.where(ring[..., None], base[..., :3] * 0.5 + np.array([30, 28, 26]), base[..., :3])
    base[..., :3] = np.where(inner[..., None], np.where(slot[..., None], np.array([12, 11, 10]), np.array([62, 58, 52])), base[..., :3])
    from PIL import Image
    return Image.fromarray(np.clip(base, 0, 255).astype('uint8'), 'RGBA')


def barandal_tex(seed):
    import numpy as np
    from PIL import Image
    y, x = np.mgrid[0:32, 0:32]
    rail = (y <= 3) | ((y >= 15) & (y <= 16))
    post = (x >= 14) & (x <= 17)
    bal = ((x % 8) >= 3) & ((x % 8) <= 4) & (y > 3)
    solid = rail | post | bal
    rgb = np.array(M.metal_placa(seed, '#2e3a33', pintado=0.8, remaches=False)).astype(np.float32)[..., :3]
    rgb = np.where((y == 0)[..., None], rgb * 1.3 + 8, rgb)
    out = np.zeros((32, 32, 4), np.uint8)
    out[..., :3] = np.clip(rgb, 0, 255)
    out[..., 3] = np.where(solid, 255, 0)
    return Image.fromarray(out, 'RGBA')


def main():
    random.seed(1)
    catalogo()
    os.makedirs(os.path.join(ROOT, 'data', NS), exist_ok=True)
    with open(os.path.join(ROOT, 'data', NS, 'deco.json'), 'w', encoding='utf-8') as f:
        json.dump({'blocks': ficha}, f, indent=1, ensure_ascii=False)
    # el catre cuenta como cama (el evento acuesta a las piezas)
    tg = os.path.join(ROOT, 'data', 'minecraft', 'tags', 'blocks')
    os.makedirs(tg, exist_ok=True)
    with open(os.path.join(tg, 'beds.json'), 'w') as f:
        json.dump({'replace': False, 'values': [f'{NS}:catre']}, f, indent=1)
    # nombres en los idiomas del mod
    ld = os.path.join(A, 'lang')
    for code in ('es_mx', 'es_es', 'en_us'):
        p = os.path.join(ld, code + '.json')
        d = json.load(open(p, encoding='utf-8'))
        for bid, (es, en) in nombres.items():
            d[f'block.{NS}.{bid}'] = en if code == 'en_us' else es
        d[f'itemGroup.{NS}.deco'] = 'Collector: decoration' if code == 'en_us' else 'Coleccionista: decoración'
        json.dump(d, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
    print(len(ficha), 'bloques ->', ROOT)


if __name__ == '__main__':
    main()
