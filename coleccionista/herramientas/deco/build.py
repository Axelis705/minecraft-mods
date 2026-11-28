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
  reja      puerta corrediza de reja (celdas): solo la abre el evento; el juego la anima (se corre a un lado)
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
import props_complejo  # noqa: E402
import props_mansion  # noqa: E402
import props_salas  # noqa: E402
import props_hotel  # noqa: E402
import materiales_mansion as MM  # noqa: E402

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


CONTINUOS = {'azulejo', 'azulejo_sucio', 'azulejo_remate', 'yeso', 'concreto', 'concreto_piso', 'linoleo', 'ladrillo_viejo', 'ladrillo_cloaca',
             'ladrillo_oscuro', 'piso_cloaca', 'lodo', 'concreto_humedo', 'papel_tapiz', 'marmol_negro', 'marmol_crema', 'marmol_oro', 'acolchado',
             'linoleo_verde', 'piedra_capilla', 'losa_piedra', 'parquet', 'madera_pulida', 'tapete_rojo'}


def cubo(bid, es, en, fn, variantes=4, sonido='stone', top=None, render=None, dureza=2.0, luz=0):
    """Bloque entero con `variantes` texturas al azar (fn(seed) -> imagen). `top`: funcion aparte para arriba y abajo. Los de CONTINUOS
    (muros y pisos) comparten las orillas: la variante 0 es la base y las demas solo cambian en el centro, asi no se ven los bloques."""
    models = []
    base = fn(1) if bid in CONTINUOS and variantes > 1 else None
    for i in range(variantes):
        img = fn(i * 13 + 1)
        if base is not None and i > 0:
            img = M.continuo(base, img)
        t = tex(f'{bid}_{i}' if variantes > 1 else bid, img)
        if top:
            tt = tex(f'{bid}_tapa_{i}' if variantes > 1 else f'{bid}_tapa', top(i * 13 + 1))
            m = {'parent': 'minecraft:block/cube_column', 'textures': {'side': t, 'end': tt}}
        else:
            m = {'parent': 'minecraft:block/cube_all', 'textures': {'all': t}}
        if render:
            m['render_type'] = 'minecraft:' + render
        mn = f'{bid}_{i}' if variantes > 1 else bid
        dump(f'models/block/{mn}.json', m)
        models.append({'model': f'{NS}:block/{mn}'} if base is None or i > 0 else {'model': f'{NS}:block/{mn}', 'weight': 2})
    dump(f'blockstates/{bid}.json', {'variants': {'': models if len(models) > 1 else models[0]}})
    dump(f'models/item/{bid}.json', {'parent': models[0]['model']})
    ficha.append({'id': bid, 'kind': 'cubo', 'sound': sonido, 'strength': dureza, 'light': luz})
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


def puerta_reja(p, corre=15, sale=2.5, ascensor=False, icono=None):
    """Puerta corrediza de reja (kit, dos bloques de alto). Modelos cerrados (los que dibuja el juego moviendolos) y abiertos (ya corrida
    a un lado y un poco hacia afuera: los usa el visor y el bloque si no hay animacion). bisagra izquierda corre a +x, derecha a -x."""
    import copy
    from PIL import Image, ImageDraw
    models, img, alpha = kit.build(p)
    img.save(os.path.join(A, 'textures/block', p.id + '.png'))

    def modelo(els):
        return {'textures': {'0': f'{NS}:block/{p.id}', 'particle': f'{NS}:block/{p.id}'}, 'elements': els, 'display': kit.DISPLAY,
                'ambientocclusion': False, 'render_type': 'minecraft:cutout'}

    def movido(els, dx, dz):
        out = copy.deepcopy(els)
        for e in out:
            for k in ('from', 'to'):
                e[k] = [round(e[k][0] + dx, 4), e[k][1], round(e[k][2] + dz, 4)]
            if 'rotation' in e:
                o = e['rotation']['origin']
                e['rotation']['origin'] = [o[0] + dx, o[1], o[2] + dz]
            for f in e['faces'].values():
                f.pop('cullface', None)
        return out
    for part in ('lower', 'upper'):
        dump(f'models/block/{p.id}_{part}.json', modelo(models[part]))
        dump(f'models/block/{p.id}_{part}_abierta_i.json', modelo(movido(models[part], corre, sale)))
        dump(f'models/block/{p.id}_{part}_abierta_d.json', modelo(movido(models[part], -corre, sale)))
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    v = {}
    for f, y in rot.items():
        for part in ('lower', 'upper'):
            for hinge in ('left', 'right'):
                for op in ('false', 'true'):
                    m = f'{NS}:block/{p.id}_{part}' + ('' if op == 'false' else ('_abierta_i' if hinge == 'left' else '_abierta_d'))
                    e = {'model': m}
                    if y:
                        e['y'] = y
                    v[f'facing={f},half={part},hinge={hinge},open={op}'] = e
    dump(f'blockstates/{p.id}.json', {'variants': v})
    # el objeto: dibujo plano de la reja (o el que se pida)
    ic = Image.new('RGBA', (32, 32))
    if icono:
        icono(ic)
        tex(p.id + '_objeto', ic)
        dump(f'models/item/{p.id}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:block/{p.id}_objeto'}})
        ficha.append({'id': p.id, 'kind': 'reja', 'sound': 'metal', 'strength': 5.0, 'ascensor': ascensor})
        nombre(p.id, p.es, p.en)
        return
    d = ImageDraw.Draw(ic)
    marco, barra, chapa = (58, 64, 62, 255), (44, 48, 46, 255), (64, 72, 64, 255)
    d.rectangle((9, 1, 22, 31), outline=marco, width=2)
    d.rectangle((11, 22, 20, 28), fill=chapa)
    for y in (12, 20):
        d.line((10, y, 21, y), fill=marco)
    for x in (13, 15, 17, 19):
        d.line((x, 3, x, 21), fill=barra)
    d.rectangle((10, 13, 13, 17), fill=(120, 122, 118, 255))
    d.point((11, 15), fill=(12, 12, 12, 255))
    tex(p.id + '_objeto', ic)
    dump(f'models/item/{p.id}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:block/{p.id}_objeto'}})
    ficha.append({'id': p.id, 'kind': 'reja', 'sound': 'metal', 'strength': 5.0})
    nombre(p.id, p.es, p.en)
    # quita los modelos de la puerta vieja (la de Minecraft con textura)
    for parte in ('bottom_left', 'bottom_left_open', 'bottom_right', 'bottom_right_open', 'top_left', 'top_left_open', 'top_right', 'top_right_open'):
        try:
            os.remove(os.path.join(A, f'models/block/{p.id}_{parte}.json'))
        except FileNotFoundError:
            pass
    for t in ('_abajo', '_arriba'):
        try:
            os.remove(os.path.join(A, f'textures/block/{p.id}{t}.png'))
        except FileNotFoundError:
            pass


def icono_puerta(fondo, filete):
    """El dibujo del objeto de una puerta corrediza opaca: dos hojas con su filete."""
    def f(ic):
        from PIL import ImageDraw
        d = ImageDraw.Draw(ic)
        d.rectangle((7, 1, 24, 31), fill=fondo, outline=(30, 28, 25, 255))
        d.line((15, 2, 15, 30), fill=(30, 28, 25, 255))
        d.line((16, 2, 16, 30), fill=(30, 28, 25, 255))
        for x0 in (9, 18):
            d.rectangle((x0, 5, x0 + 4, 14), outline=filete)
            d.rectangle((x0, 18, x0 + 4, 27), outline=filete)
    return f


def capas(bid, es, en, fn, sonido='wool', dureza=0.6):
    """Bloque por capas (como la nieve, 8 capas de 2 px): la cera que sube en las vitrinas. No se derrite ni se cae."""
    t = tex(bid, fn(1))
    v = {}
    for n in range(1, 9):
        h = 2 * n
        caras = {}
        for f in ('north', 'south', 'east', 'west'):
            caras[f] = {'texture': '#0', 'uv': [0, 16 - h, 16, 16], 'cullface': f}
        caras['down'] = {'texture': '#0', 'uv': [0, 0, 16, 16], 'cullface': 'down'}
        caras['up'] = {'texture': '#0', 'uv': [0, 0, 16, 16]}
        if h == 16:
            caras['up']['cullface'] = 'up'
        dump(f'models/block/{bid}_{n}.json', {'parent': 'minecraft:block/block', 'textures': {'0': t, 'particle': t},
                                              'elements': [{'from': [0, 0, 0], 'to': [16, h, 16], 'faces': caras}]})
        v[f'layers={n}'] = {'model': f'{NS}:block/{bid}_{n}'}
    dump(f'blockstates/{bid}.json', {'variants': v})
    dump(f'models/item/{bid}.json', {'parent': f'{NS}:block/{bid}_2'})
    ficha.append({'id': bid, 'kind': 'capas', 'sound': sonido, 'strength': dureza})
    nombre(bid, es, en)


def cadena_blk(bid, es, en, fn):
    """Cadena que cuelga (o va acostada): el modelo cruzado de la cadena de Minecraft con textura propia."""
    from PIL import Image
    im = fn(1)
    t = tex(bid, im)
    m = json.load(open('/tmp/mcassets/assets/minecraft/models/block/chain.json'))
    m['textures'] = {'particle': t, 'all': t}
    m['parent'] = 'minecraft:block/block'
    m['render_type'] = 'minecraft:cutout'
    dump(f'models/block/{bid}.json', m)
    vb = json.load(open('/tmp/mcassets/assets/minecraft/blockstates/chain.json'))
    dump(f'blockstates/{bid}.json', json.loads(json.dumps(vb).replace('minecraft:block/chain', f'{NS}:block/{bid}')))
    ic = Image.new('RGBA', (32, 32))
    ic.paste(im.crop((0, 0, 6, 32)), (13, 0))
    tex(bid + '_objeto', ic)
    dump(f'models/item/{bid}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{NS}:block/{bid}_objeto'}})
    ficha.append({'id': bid, 'kind': 'cadena', 'sound': 'chain', 'strength': 2.0})
    nombre(bid, es, en)


def trampilla_blk(bid, es, en, fn):
    """Trampilla de acero (como la de hierro: solo la abre el evento)."""
    t = tex(bid, fn(1))
    for suf in ('bottom', 'top', 'open'):
        dump(f'models/block/{bid}_{suf}.json', {'parent': f'minecraft:block/template_trapdoor_{suf}', 'textures': {'texture': t},
                                               'render_type': 'minecraft:cutout'})
    vb = json.load(open('/tmp/mcassets/assets/minecraft/blockstates/iron_trapdoor.json'))
    dump(f'blockstates/{bid}.json', json.loads(json.dumps(vb).replace('minecraft:block/iron_trapdoor', f'{NS}:block/{bid}')))
    dump(f'models/item/{bid}.json', {'parent': f'{NS}:block/{bid}_bottom'})
    ficha.append({'id': bid, 'kind': 'trampilla', 'sound': 'metal', 'strength': 5.0})
    nombre(bid, es, en)


def tubo(bid, es, en, fn, sonido='metal', a=5.5, b=10.5, brida=1.0):
    """Tubo con bridas que va en cualquier eje (por el techo y las paredes). Con a/b mas gordos sirve de poste."""
    t = tex(bid, fn(1))
    els = [{'from': [a, 0, a], 'to': [b, 16, b], 'faces': {f: {'texture': '#t'} for f in ('north', 'south', 'west', 'east')}},
           {'from': [a - brida, 0, a - brida], 'to': [b + brida, 1.5, b + brida], 'faces': {f: {'texture': '#t'} for f in ('north', 'south', 'west', 'east', 'down', 'up')}},
           {'from': [a - brida, 14.5, a - brida], 'to': [b + brida, 16, b + brida], 'faces': {f: {'texture': '#t'} for f in ('north', 'south', 'west', 'east', 'down', 'up')}}]
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
    cubo('azulejo_sucio', 'Azulejo sucio', 'Dirty tile', lambda s: M.azulejo(s, sucio=0.65, faltan=0.12))
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
    barrotes('barrotes_dorados', 'Barrotes dorados', 'Golden bars', lambda s: jaula_tex(s))
    ficha[-1]['alto'] = True  # choca como cerca: nadie se sube
    puerta_reja(props_celdas.puerta_reja())
    puerta_reja(props_celdas.puerta_ascensor(), ascensor=True, icono=icono_puerta((150, 117, 58, 255), (210, 172, 90, 255)))
    puerta_reja(props_celdas.puerta_metal(), ascensor=True, icono=icono_puerta((58, 67, 58, 255), (40, 46, 40, 255)))
    puerta_reja(props_salas.puerta_autobus(), ascensor=True, icono=icono_puerta((90, 28, 30, 255), (20, 26, 32, 255)))
    alfombra('alfombra_roja', 'Alfombra roja', 'Red carpet', lambda s: M.alfombra(s))
    tubo('tubo', 'Tubería oxidada', 'Rusty pipe', lambda s: M.metal_placa(s, '#4d5249', pintado=0.5, remaches=False))
    tubo('poste', 'Poste de acero', 'Steel post', lambda s: poste_tex(s), a=4.5, b=11.5, brida=0.6)
    tubo('poste_liso', 'Poste de acero (liso)', 'Steel post (plain)', lambda s: M.metal_placa(s, '#5a5d55', pintado=0.6, remaches=False), a=4.5, b=11.5, brida=0.6)
    palanca('palanca', 'Palanca', 'Lever')
    # ---- las Cloacas y lo que se usa en todo el complejo
    cubo('ladrillo_cloaca', 'Ladrillo de alcantarilla', 'Sewer brick', lambda s: M.ladrillo_cloaca(s))
    cubo('ladrillo_oscuro', 'Sillar negro', 'Black ashlar', lambda s: M.ladrillo_oscuro(s), variantes=2)
    cubo('piso_cloaca', 'Laja de alcantarilla', 'Sewer flagstone', lambda s: M.piso_cloaca(s), variantes=3)
    cubo('lodo', 'Lodo', 'Mud', lambda s: M.lodo(s), variantes=2, sonido='wool', dureza=0.8)
    cubo('concreto_humedo', 'Concreto húmedo', 'Damp concrete', lambda s: M.concreto_humedo(s), variantes=3)
    cubo('cobre_viejo', 'Lámina de cobre vieja', 'Old copper plate', lambda s: M.cobre_viejo(s), variantes=3, sonido='metal')
    cubo('carroceria', 'Carrocería vino', 'Wine-red bus body', lambda s: M.metal_placa(s, '#561a1d', pintado=0.94, remaches=False),
         variantes=2, sonido='metal')
    cubo('carroceria_crema', 'Carrocería crema', 'Cream bus body', lambda s: M.metal_placa(s, '#b7a47f', pintado=0.92, remaches=False),
         variantes=2, sonido='metal')
    cubo('tablon', 'Tablones viejos', 'Old planks', lambda s: M.tablon(s), variantes=2, sonido='wood')
    losa('losa_tablon', 'Losa de tablones', 'Plank slab', lambda s: M.tablon(s), sonido='wood')
    escalera('escalera_tablon', 'Escalón de tablones', 'Plank stairs', lambda s: M.tablon(s), 'tablon', sonido='wood')
    escalera('escalera_ladrillo', 'Escalón de ladrillo', 'Brick stairs', lambda s: M.ladrillo_cloaca(s), 'ladrillo_cloaca', sonido='stone')
    cadena_blk('cadena', 'Cadena oxidada', 'Rusty chain', lambda s: M.cadena(s))
    trampilla_blk('trampilla', 'Trampilla de acero', 'Steel trapdoor', lambda s: M.trampilla(s))
    alfombra('alfombra_negra', 'Alfombra negra', 'Black carpet', lambda s: M.alfombra_negra(s))
    # ---- el Archivo
    cubo('parquet', 'Parquet viejo', 'Old parquet', lambda s: M.parquet(s), variantes=2, sonido='wood')
    cubo('estante_archivo', 'Estante de archivo', 'Archive shelf', lambda s: M.estante_archivo(s), sonido='wood', top=lambda s: M.madera_tapa(s))
    cubo('archivero', 'Archivero de metal', 'Filing cabinet', lambda s: M.archivero(s), variantes=2, sonido='metal',
         top=lambda s: M.metal_placa(s, '#4b5a4c', pintado=0.95, remaches=False))
    cubo('lingotes', 'Lingotes de oro', 'Gold bars', lambda s: M.lingotes(s), variantes=2, sonido='metal')
    pilar('viga_madera', 'Columna de madera', 'Wooden column', lambda s: M.viga_madera_lado(s), lambda s: M.viga_madera_tapa(s), sonido='wood')
    losa('losa_madera', 'Repisa de madera', 'Wooden slab', lambda s: M.madera_tapa(s), sonido='wood')
    alfombra('alfombra_cafe', 'Alfombra café', 'Brown carpet', lambda s: M.alfombra_cafe(s))
    # ---- el Salon
    cubo('papel_tapiz', 'Papel tapiz de damasco', 'Damask wallpaper', lambda s: M.papel_tapiz(s), variantes=3, sonido='wool')
    cubo('marmol_negro', 'Mármol negro', 'Black marble', lambda s: M.marmol(s, brillo=1.0), variantes=3)
    cubo('marmol_crema', 'Mármol crema', 'Cream marble', lambda s: M.marmol(s, base='#8f8572', veta='#4a443a', brillo=1.0), variantes=2)
    cubo('tapete_rojo', 'Tapete rojo', 'Red rug', lambda s: M.tapete(s), variantes=2, sonido='wool')
    cubo('tapete_orilla', 'Orilla de tapete', 'Rug border', lambda s: M.tapete(s, orilla=True), variantes=1, sonido='wool')
    cubo('marco_dorado', 'Marco dorado', 'Gilded frame', lambda s: M.marco_dorado(s), variantes=2, sonido='metal')
    # ---- los Laberintos (cuartos acolchados)
    cubo('acolchado', 'Pared acolchada', 'Padded wall', lambda s: M.acolchado(s), sonido='wool')
    cubo('linoleo_verde', 'Linóleo de hospital', 'Hospital linoleum', lambda s: M.linoleo_verde(s), variantes=2)
    cubo('vidrio_armado', 'Vidrio armado', 'Wired glass', lambda s: M.vidrio_armado(s), variantes=1, sonido='glass', render='translucent', dureza=0.5)
    ficha[-1]['kind'] = 'vidrio'
    cubo('panel_luz', 'Panel de luz', 'Ceiling light panel', lambda s: M.panel_luz(s), variantes=1, sonido='glass', luz=15)
    cubo('marca_roja', 'Marca roja en el piso', 'Red floor mark', lambda s: M.pintura_piso(s), variantes=1)
    cubo('marca_verde', 'Marca verde en el piso', 'Green floor mark', lambda s: M.pintura_piso(s, '#2f7a34'), variantes=1)
    # ---- la Capilla
    cubo('piedra_capilla', 'Sillar de capilla', 'Chapel stone', lambda s: M.piedra_capilla(s))
    cubo('losa_piedra', 'Losa de piedra', 'Stone flagstone', lambda s: M.losa_piedra(s), variantes=2)
    cubo('vitral', 'Vitral', 'Stained glass window', lambda s: M.vitral(s), variantes=1, sonido='glass', render='translucent', dureza=0.5)
    ficha[-1]['kind'] = 'vidrio'
    # ---- la Camara final
    cubo('marmol_oro', 'Mármol negro y oro', 'Gold-veined marble', lambda s: M.marmol(s, veta='#b08a3a', brillo=1.0), variantes=3)
    cubo('madera_pulida', 'Madera pulida', 'Polished wood', lambda s: M.madera_pulida(s), variantes=2, sonido='wood')
    losa('losa_marmol', 'Losa de mármol', 'Marble slab', lambda s: M.marmol(s, brillo=1.0), sonido='stone')
    # ---- el teatro de la colección (final del capítulo 1)
    capas('cera', 'Cera', 'Wax', lambda s: M.cera(s))
    cubo('telon', 'Telón de terciopelo', 'Velvet stage curtain', lambda s: M.telon(s), variantes=2, sonido='wool', dureza=0.8)
    cubo('telon_borde', 'Telón (orilla con fleco)', 'Stage curtain (fringe)', lambda s: M.telon(s, borde=True), variantes=1, sonido='wool', dureza=0.8)
    # ---- la Sala de las puertas: marcos de luz que cambian de color
    cubo('marco_luz_apagado', 'Marco de luz (apagado)', 'Light frame (off)', lambda s: M.marco_luz(s), variantes=1, sonido='metal')
    for cid, ces, cen, col in (('roja', 'rojo', 'red', '#ff3b3b'), ('azul', 'azul', 'blue', '#4a86ff'), ('verde', 'verde', 'green', '#6cff4a'),
                               ('amarilla', 'amarillo', 'yellow', '#ffe14a'), ('morada', 'morado', 'purple', '#c14aff'), ('blanca', 'blanco', 'white', '#f4f4ee')):
        cubo('marco_luz_' + cid, 'Marco de luz ' + ces, cen.capitalize() + ' light frame', lambda s, col=col: M.marco_luz(s, col), variantes=1, sonido='metal', luz=9)
    # ---- la mansión gótica (capítulo 2): techo de pizarra, cantera, madera fina, papel tapiz de cada cuarto y el jardín de noche
    cubo('pizarra', 'Tejas de pizarra', 'Slate shingles', lambda s: MM.pizarra(s), variantes=3)
    escalera('escalera_pizarra', 'Escalón de pizarra', 'Slate shingle stairs', lambda s: MM.pizarra(s), 'pizarra', sonido='stone')
    losa('losa_pizarra', 'Losa de pizarra', 'Slate shingle slab', lambda s: MM.pizarra(s), sonido='stone')
    escalera('escalera_sillar', 'Escalón de sillar', 'Ashlar stairs', lambda s: M.piedra_capilla(s), 'piedra_capilla', sonido='stone')
    losa('losa_sillar', 'Losa de sillar', 'Ashlar slab', lambda s: M.piedra_capilla(s), sonido='stone')
    cubo('cornisa', 'Cornisa de cantera', 'Carved stone cornice', lambda s: MM.cornisa(s), variantes=2)
    escalera('escalera_madera', 'Escalón de madera fina', 'Polished wood stairs', lambda s: M.madera_pulida(s), 'madera_pulida', sonido='wood')
    barrotes('balaustrada', 'Balaustrada de madera', 'Wooden balustrade', lambda s: MM.balaustrada(s), sonido='wood')
    ficha[-1]['alto'] = True  # choca como cerca: nadie se sube
    cubo('techo_yeso', 'Plafón de yeso', 'Plaster ceiling', lambda s: MM.techo_yeso(s), variantes=3)
    cubo('papel_verde', 'Papel tapiz verde', 'Green wallpaper', lambda s: MM.papel(s, '#142218', '#2c4228'), variantes=3, sonido='wool')
    cubo('papel_azul', 'Papel tapiz azul', 'Blue wallpaper', lambda s: MM.papel(s, '#141a2a', '#2c3550'), variantes=3, sonido='wool')
    cubo('papel_rosa', 'Papel tapiz rosa', 'Faded pink wallpaper', lambda s: MM.papel(s, '#5a3a3c', '#7a5658'), variantes=3, sonido='wool')
    cubo('papel_dorado', 'Papel tapiz dorado', 'Ochre wallpaper', lambda s: MM.papel(s, '#3a2c12', '#6a5226'), variantes=3, sonido='wool')
    cubo('pasto', 'Pasto seco', 'Withered grass', lambda s: MM.pasto_lado(s), variantes=3, sonido='wool', top=lambda s: MM.pasto(s), dureza=0.8)
    cubo('tierra', 'Tierra negra', 'Black soil', lambda s: MM.tierra(s), variantes=3, sonido='wool', dureza=0.8)
    cubo('grava', 'Grava', 'Gravel path', lambda s: MM.grava(s), variantes=2, dureza=0.8)
    cubo('seto', 'Seto', 'Hedge', lambda s: MM.seto(s), variantes=3, sonido='wool', dureza=0.6)
    pilar('tronco', 'Tronco seco', 'Dead tree trunk', lambda s: MM.corteza(s), lambda s: MM.corte_tronco(s), sonido='wood')
    barrotes('ramas', 'Ramas secas', 'Dry branches', lambda s: MM.ramas(s), sonido='wood')
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
    for p in (P.rejilla_ventilacion(), P.camara(), P.bocina(), P.reloj(), P.telarana(), P.grilletes(), P.silla_ruedas(), P.camilla()):
        prop(p)
    prop(P.calcomania('mancha', 'Mancha', 'Stain', P.mancha_mat(), 40))
    prop(P.calcomania('charco', 'Charco', 'Puddle', P.mancha_mat('#28343a', 150, 41), 41), {'goteo': True})
    prop(P.calcomania('papeles', 'Papeles tirados', 'Scattered papers', P.papeles_mat(), 42))
    prop(P.calcomania('sangre', 'Salpicadura roja', 'Red splatter', P.sangre_mat(), 44))
    prop(P.vitrina())
    prop(P.candelabro())
    prop(P.cortina())
    prop(P.placa(), {'texto': 'placa'})
    prop(P.letrero(), {'texto': 'letrero'})
    prop(P.rayon(), {'texto': 'rayon'})
    prop(P.mesa())
    prop(P.muneca())
    for t in ('venda', 'cinta', 'esposas'):
        prop(P.objeto(t), {'objeto': t})
    Q = props_complejo
    con_apagado(Q.farol(True), Q.farol(False), {'parpadeo': True})
    prop(Q.vela())
    prop(Q.musgo())
    prop(Q.argolla())
    prop(Q.barril(), {'cajon': True})
    prop(Q.placa_presion())
    prop(Q.atril())
    prop(Q.cofre_boveda())
    prop(Q.mesa_tapete())
    prop(Q.sillon())
    prop(Q.estandarte())
    prop(Q.estandarte('#151314', 'estandarte_negro', 'Estandarte negro', 'Black banner'))
    prop(Q.arana())
    prop(Q.lampara_billar())
    prop(Q.placa_altar())
    prop(Q.trono())
    prop(Q.craneo())
    prop(Q.zapatos())
    # la mansión gótica del capítulo 2
    for p in props_mansion.todos():
        prop(p)
    # las salas de los capítulos, los pasillos y las transiciones
    for p in props_salas.todos():
        prop(p)
    # el hotel del capítulo 1 y su sala de espera (el colchón rebota)
    for p in props_hotel.todos():
        prop(p, {'brinco': True} if p.id == 'colchon_brinco' else None)
    cama(props_mansion.cama_antigua())
    ficha[-1]['adorno'] = True  # no se duerme en ella (ni guarda el punto de aparición)
    ficha[-1]['sound'] = 'wood'
    for p in P.focos():
        solo_modelos(p)
    solo_modelos(P.candado())
    solo_modelos(P.candado_ruedas())
    candado_en_reja()


def candado_en_reja():
    """El candado de la celda va colgado de los barrotes: los barrotes estan al centro de su bloque (7 px atras de la orilla), asi que el
    modelo del candado de ruedas se recorre 7 px hacia atras (el teclado de la boveda sigue pegado al muro con su modelo de botones)."""
    m = json.load(open(os.path.join(A, 'models/block/candado_ruedas.json')))
    for e in m['elements']:
        e['from'][2] = round(e['from'][2] + 7, 4)
        e['to'][2] = round(e['to'][2] + 7, 4)
        if 'rotation' in e:
            e['rotation']['origin'][2] += 7
        for f in e['faces'].values():
            f.pop('cullface', None)
    dump('models/block/candado_reja.json', m)
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    v = {}
    for f, y in rot.items():
        x = {'model': f'{NS}:block/candado_reja'}
        if y:
            x['y'] = y
        v[f'facing={f}'] = x
    dump('blockstates/candado.json', {'variants': v})


def palanca(bid, es, en):
    """Palanca de pared (abre rejas en los juegos): la de Minecraft con base de acero y mango rojo."""
    import numpy as np
    from PIL import Image
    base = tex(bid + '_base', M.metal_placa(3, '#2c2d2a', pintado=0.6, remaches=False))
    y, x = np.mgrid[0:32, 0:32]
    img = np.zeros((32, 32, 4), np.uint8)
    mango = (x >= 14) & (x <= 17) & (y >= 12)
    bola = (x >= 14) & (x <= 17) & (y >= 12) & (y <= 16)
    img[mango] = (88, 88, 84, 255)
    img[(x == 14) & (y >= 12)] = (120, 120, 114, 255)
    img[bola] = (150, 24, 20, 255)
    img[(y == 12) & (x >= 14) & (x <= 17)] = (190, 50, 40, 255)
    lv = tex(bid + '_mango', Image.fromarray(img, 'RGBA'))
    for suf, par in (('', 'lever'), ('_on', 'lever_on')):
        dump(f'models/block/{bid}{suf}.json', {'parent': f'minecraft:block/{par}', 'textures': {'particle': base, 'base': base, 'lever': lv}})
    vb = json.load(open('/tmp/mcassets/assets/minecraft/blockstates/lever.json'))
    txt = json.dumps(vb).replace('minecraft:block/lever', f'{NS}:block/{bid}')
    dump(f'blockstates/{bid}.json', json.loads(txt))
    dump(f'models/item/{bid}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': lv}})
    ficha.append({'id': bid, 'kind': 'palanca', 'sound': 'metal', 'strength': 1.0})
    nombre(bid, es, en)


def poste_tex(seed):
    """Poste pintado de amarillo y negro abajo (como en las fabricas), gris y oxidado arriba."""
    import numpy as np
    from PIL import Image
    base = np.array(M.metal_placa(seed, '#5a5d55', pintado=0.6, remaches=False)).astype(np.float32)
    y, x = np.mgrid[0:32, 0:32]
    franja = (((x + y) // 6) % 2 == 0)
    peligro = np.where(franja[..., None], np.array([196, 160, 40, 255], np.float32), np.array([30, 28, 24, 255], np.float32))
    base = np.where((y >= 20)[..., None], peligro * 0.8 + base * 0.2, base)
    return Image.fromarray(np.clip(base, 0, 255).astype('uint8'), 'RGBA')


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


def jaula_tex(seed):
    """Las jaulas de pájaro del salón de té: varillas doradas muy delgadas (dos por bloque) y un aro abajo; casi todo transparente, para
    que desde adentro se vea bien la mesa."""
    import numpy as np
    from PIL import Image
    y, x = np.mgrid[0:32, 0:32]
    varilla = (x == 0) | (x == 31) | (x == 15) | (x == 16)
    aro = y >= 30
    solid = varilla | aro
    rgb = np.zeros((32, 32, 3), np.float32)
    rgb[...] = (212, 168, 56)
    luz = (x == 15) | (x == 0)
    rgb = np.where(luz[..., None], np.array((246, 214, 112), np.float32), rgb)
    sombra = (x == 16) | (x == 31)
    rgb = np.where(sombra[..., None], np.array((150, 112, 34), np.float32), rgb)
    rgb = np.where((aro & (y == 30))[..., None], np.array((240, 205, 100), np.float32), rgb)
    rgb = np.where((aro & (y == 31))[..., None], np.array((138, 102, 30), np.float32), rgb)
    # un poco de brillo que cambia a lo largo de la varilla (como metal pulido)
    rgb = rgb * (0.92 + 0.08 * np.sin(y / 3.0 + seed))[..., None]
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
