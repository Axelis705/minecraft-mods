#!/usr/bin/env python3
"""Iconos de 16x16 de los objetos de Navidad de Axelis (pixel art en texto + generadores para tazas, discos y suéteres) y sus modelos.
Uso: python3 herramientas/iconos.py"""
import json, os
import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')
T = os.path.join(A, 'textures', 'item')
rs = np.random.RandomState(4)

PAL = {'a': '#4a2a14', 'b': '#b8743a', 'c': '#d8944e', 'w': '#f6f6f2', 'r': '#d42a2a', 'R': '#9a1a20', 'g': '#2a9a3a', 'G': '#1a6a28',
       'k': '#1a1a1a', 'y': '#f0c43a', 'Y': '#b88a18', 'o': '#f07a1c', 'O': '#b8500e', 'p': '#e86aa8', 'v': '#8a3ad6', 'z': '#3a7ae0',
       's': '#c8c8cc', 'S': '#8a8a90', 'n': '#7a4a2a', 'N': '#5a3418', 'l': '#ecd8a0', 'L': '#c8a860', 'e': '#a8d860', 'x': '#6a4020'}


def hexc(h):
    h = h.lstrip('#')
    return [int(h[i:i + 2], 16) for i in (0, 2, 4)]


def dibujo(nombre, filas, pal=None, var=6):
    pal = dict(PAL, **(pal or {}))
    img = np.zeros((16, 16, 4), np.uint8)
    for y, f in enumerate(filas):
        for x, ch in enumerate(f[:16]):
            if ch == '.' or ch == ' ':
                continue
            c = np.clip(np.array(hexc(pal[ch])) + rs.randint(-var, var + 1), 0, 255)
            img[y, x, :3] = c
            img[y, x, 3] = 255
    guardar(nombre, img)


def guardar(nombre, img):
    os.makedirs(T, exist_ok=True)
    Image.fromarray(img, 'RGBA').save(os.path.join(T, nombre + '.png'))
    md = os.path.join(A, 'models', 'item')
    os.makedirs(md, exist_ok=True)
    json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': f'navidad:item/{nombre}'}},
              open(os.path.join(md, nombre + '.json'), 'w'), indent=1)


def taza(nombre, liquido, espuma=None, fruta=None):
    filas = ['....w..w........', '.....w..w.......', '....w..w........', '................',
             '..kkkkkkkkkk....', '..kLLLLLLLLk....', '..kssssssssk....', '..kRRRRRRRRkkk..',
             '..kRRwRRRRRk.k..', '..kRRRRRRwRk.k..', '..kRRRRRRRRkkk..', '..kRwRRRRRRk....',
             '...kRRRRRRk.....', '...kkkkkkkk.....', '................', '................']
    pal = {'L': liquido, 's': espuma or liquido, 'R': '#c8202e', 'w': '#f4f4f0', 'k': '#3a1410'}
    if fruta:
        filas[5] = '..kLLfLLLfLk....'
        pal['f'] = fruta
    dibujo(nombre, filas, pal)


def disco(nombre, etiqueta, brillo='#3a3a44'):
    img = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 7.2:
                continue
            if d < 0.9:
                continue
            if d < 3.0:
                c = hexc(etiqueta)
            elif abs(d - 5.0) < 0.4 or abs(d - 6.3) < 0.3:
                c = hexc(brillo)
            else:
                c = [24, 24, 30]
            if 3.0 <= d and x - y > 6 and d < 6.8:
                c = [min(255, v + 40) for v in c]
            img[y, x, :3] = c
            img[y, x, 3] = 255
    guardar(nombre, img)


def sueter(nombre, base, oscuro, banda, dibujito):
    filas = ['................', '................', '....kk....kk....', '...kBBk..kBBk...',
             '..kBBBBkkBBBBk..', '.kBBBBBBBBBBBBk.', 'kBBBkBBBBBBkBBBk', 'kBBkkPPPPPPkkBBk',
             'kWWk.BBBBBB.kWWk', 'kDDk.BBBBBB.kDDk', '.kk..BBBBBB..kk.', '.....BBBBBB.....',
             '.....DDDDDD.....', '................', '................', '................']
    pal = {'B': base, 'D': oscuro, 'P': banda, 'W': banda, 'k': '#2a1410'}
    for (x, y, ch, col) in dibujito:
        f = list(filas[y])
        f[x] = ch
        filas[y] = ''.join(f)
        pal[ch] = col
    dibujo(nombre, filas, pal)


if __name__ == '__main__':
    dibujo('galleta_jengibre', [
        '................', '......aaaa......', '.....abccba.....', '.....akbbka.....', '.....abwwba.....', '......abba......',
        '..aaaaabbaaaaa..', '.abbbbbrrbbbbba.', '.awbbbbbbbbbbwa.', '..aaaaabgbaaaa..', '.....abbbba.....', '.....abrrba.....',
        '....abbaabba....', '....abba.abba...', '....awwa..awwa..', '.....aa....aa...'])
    dibujo('baston_caramelo', [
        '................', '.......Rrw......', '......rww.rw....', '.....rw....rw...', '.....wR....wr...', '......k.....rw..',
        '............wr..', '...........rw...', '..........wr....', '.........rw.....', '........wr......', '.......rw.......',
        '......wr........', '.....rw.........', '.....w..........', '................'])
    dibujo('dulce', [
        '................', '................', '................', '..y..........y..', '..yy.......yyy..', '..yyyRrrrRyyy...',
        '...yrrwrrrrry...', '....rwrrrrrr....', '....rrrrrrwr....', '...yrrrrrwrry...', '..yyyRrrrRyyy...', '..yy.......yy...',
        '..y..........y..', '................', '................', '................'])
    dibujo('tejocote', [
        '................', '................', '.......G........', '......Gg........', '.....gGx........', '....OOoxooO.....',
        '...OooooooooO...', '...ooyooooooo...', '..Ooyyoooooooo..', '..Ooooooooooo...', '..OOoooooooooO..', '...OOooooooOO...',
        '....OOOooOOO....', '......OOOO......', '................', '................'],
        {'o': '#f0a028', 'O': '#c0701a', 'y': '#ffe08a'})
    dibujo('mandarina', [
        '................', '.......x........', '......gxG.......', '.....GggG.......', '....OOoxoOO.....', '...Oooooooooo...',
        '..Oooyoooooooo..', '..Ooyyoooooooo..', '.Ooooooooooooo..', '.Oooooooooooooo.', '.OOoooooooooooO.', '..OOoooooooooO..',
        '..OOOoooooooOO..', '...OOOOooOOOO...', '.....OOOOOO.....', '................'])
    dibujo('cana', [
        '...........gG...', '..........gG.g..', '.........eGg....', '.........le.....', '........lle.....', '........eL......',
        '.......lle......', '.......Le.......', '......lle.......', '......eL........', '.....lle........', '.....Le.........',
        '....lle.........', '....eL..........', '...lL...........', '................'], {'e': '#6a8a3a', 'l': '#d8c070', 'L': '#a88a3a'})
    dibujo('tamal', [
        '................', '................', '.........ll.....', '.......llLLl....', '.....llLccLLl...', '....lLLcyyyclL..',
        '...lLcyyyyyycL..', '..lLcyyrryyyl...', '..lLcyyyrryycL..', '...lLccyyyycL...', '....lLLcccclL...', '.....llLLLLl....',
        '.......lllL.....', '.........l......', '................', '................'], {'c': '#e8d080', 'y': '#f4e098', 'r': '#c83a1a'})
    dibujo('bunuelo', [
        '................', '................', '.....LLLLLL.....', '...LLcccccccL...', '..LcccwccccccL..', '..LcbcccccwccL..',
        '.LccccccbcccccL.', '.LcwcccccccccL..', '.LcccccwcccbccL.', '.LccbccccccccL..', '..LccccccwcccL..', '..LLccwcccccLL..',
        '....LLcccccLL...', '......LLLL......', '................', '................'], {'c': '#e8b060', 'L': '#a8702a', 'b': '#7a4a1a', 'w': '#fff4d8'})
    dibujo('pavo_navideno', [
        '................', '................', '..w.......w.....', '..wn....nnw.....', '...nnnnnnnn.....', '..nccccccccn....',
        '.nccncccccccn...', '.ncccccnccccn....', '.nccccccccncn...', '.ncncccccccccn..', '..nccccnccccn...', '...nnccccccn..g.',
        '....nnnnnnn..gGg', '.sssssssssssss.g', '..sssssssssss...', '................'], {'c': '#c87a2a', 'n': '#8a4a1a', 's': '#d8d8dc'})
    taza('chocolate_caliente', '#5a2a14', '#f4ece0')
    taza('ponche', '#d8781a', None, '#f0c43a')
    taza('champurrado', '#8a5a34', '#c89870')
    dibujo('regalo_misterioso', [
        '................', '.....yy..yy.....', '....y.yyyy.y....', '....yy.yy.yy....', '.....yyyyyy.....', '..vvvvvyyvvvvv..',
        '..vVVVVyyVVVVv..', '..vvvvvyyvvvvv..', '..yyyyyyyyyyyy..', '..vvvvvyyvvvvv..', '..vVVwVyyVVVVv..', '..vVVVVyyVwVVv..',
        '..vVwVVyyVVVVv..', '..vVVVVyyVVVwv..', '..vvvvvyyvvvvv..', '................'], {'V': '#a052e8', 'v': '#6a2aa8'})
    dibujo('pinata', [
        '.......k........', '.......r........', '......rrr.......', '......rrr.......', '..g..yyyyy..z...', '..ggyyyyyyyzz...',
        '...gyyyoyyyz....', '....yyyyyyyy....', '....yyyyyoyy....', '...pyyyyyyyyv...', '..ppyyyoyyyyvv..', '..p..yyyyyy..v..',
        '......ooo.......', '......ooo.......', '.......o........', '.......r........'])
    dibujo('palo_pinata', [
        '................', '.............rw.', '............rw..', '...........wr...', '..........rw....', '.........wr.....',
        '........nN......', '.......nN.......', '......nN........', '.....nN.........', '....nN..........', '...nN...........',
        '..nN............', '.nN.............', '................', '................'])
    dibujo('venda', [
        '................', '................', '................', '................', '.k............k.', '.kk..........kk.',
        '..kkkkkkkkkkkk..', '..kKKKKKKKKKKk..', '..kKkKKKKKKkKk..', '..kkkkkkkkkkkk..', '.k............k.', 'k..............k',
        '................', '................', '................', '................'], {'K': '#3a3a48'})
    dibujo('gorro_santa', [
        '................', '...........ww...', '..........www...', '.........rr.....', '........rRr.....', '.......rrRr.....',
        '......rrrRrr....', '.....rrrrrRrr...', '....rrrrrrrRr...', '...rrrrrrrrrrr..', '..wwwwwwwwwwwww.', '..wwswwwwswwwww.',
        '..wwwwwswwwwsww.', '................', '................', '................'])
    dibujo('gorro_duende', [
        '..........yy....', '..........yy....', '..........g.....', '.........gG.....', '........ggG.....', '........gG......',
        '.......ggG......', '......gggG......', '.....ggggGG.....', '....ggggggGG....', '...gggggggggG...', '..rrrrrrrrrrrr..',
        '..rRrrrRrrrRrr..', '................', '................', '................'])
    dibujo('cuernos_reno', [
        '................', '.n.n......n.n...', '.nnn.n..n.nnn...', '..nnn....nnn....', '...n......n.....', '...n......n.....',
        '...nn....nn.....', '....kkkkkk......', '...k......k.....', '..k........k....', '..k........k....', '..k...rr...k....',
        '......rr........', '................', '................', '................'], {'k': '#5a5a60'})
    sueter('sueter_reno', '#b81e2a', '#841420', '#f2f2ee', [(7, 9, 'n', '#7a4a2a'), (8, 9, 'n', '#7a4a2a'), (7, 10, 'r', '#ff3030'),
                                                           (6, 8, 'h', '#d8b878'), (9, 8, 'h', '#d8b878')])
    sueter('sueter_pinos', '#e8e0cc', '#b8ae98', '#c81e2a', [(7, 8, 'p', '#1e7a34'), (8, 8, 'p', '#1e7a34'), (7, 9, 'p', '#1e7a34'),
                                                           (8, 9, 'p', '#1e7a34'), (6, 10, 'p', '#1e7a34'), (9, 10, 'p', '#1e7a34'),
                                                           (7, 10, 'p', '#1e7a34'), (8, 10, 'p', '#1e7a34')])
    sueter('sueter_copos', '#2a4a8a', '#1a3060', '#f2f2ee', [(7, 9, 'w', '#f2f2ee'), (8, 10, 'w', '#f2f2ee'), (6, 10, 'w', '#f2f2ee'),
                                                           (9, 9, 'w', '#f2f2ee'), (7, 11, 'w', '#f2f2ee')])
    dibujo('traje_santa', [
        '................', '................', '....kk....kk....', '...kRRk..kRRk...', '..kRRRRkkRRRRk..', '.kRRRRRwwRRRRRk.',
        'kRRRkRRwwRRkRRRk', 'kRRkkRRwwRRkkRRk', 'kwwk.bbyybb.kwwk', 'kRRk.RRwwRR.kRRk', '.kk..RRwwRR..kk.', '.....RRwwRR.....',
        '.....wwwwww.....', '.....kk..kk.....', '................', '................'], {'R': '#c41e2a', 'b': '#1a1a1a'})
    disco('disco_campanas', '#f0c43a')
    disco('disco_navidad', '#c8202e')
    disco('disco_noche_paz', '#2a4a8a')
    disco('disco_adornen', '#2a8a40')
    dibujo('caja_adornos', [
        '................', '................', '...r....y...g...', '..rRr..yYy.gGg..', '..rrr..yyy.ggg..', '...z..p.....v...',
        '..zZz.pPp..vVv..', '.nnnnnnnnnnnnnn.', '.nNNNNNNNNNNNNn.', '.nNnnnnnnnnnnNn.', '.nNNNNNNNNNNNNn.', '.nNnnnnnnnnnnNn.',
        '.nNNNNNNNNNNNNn.', '.nnnnnnnnnnnnnn.', '................', '................'],
        {'Z': '#2a5ab0', 'P': '#b8408a', 'V': '#6a2aa8', 'N': '#a87a4a'})
    dibujo('campana_krampus', [
        '................', '.......kk.......', '......k..k......', '.......kk.......', '......kKKk......', '.....kKKKKk.....',
        '.....kKrKKk.....', '....kKKKKKKk....', '....kKKKKKKk....', '...kKKKKKrKKk...', '...kKrKKKKKKk...', '..kkkkkkkkkkkk..',
        '..kYYYYYYYYYYk..', '...kkkkkkkkkk...', '.......kk.......', '.......kk.......'], {'k': '#141012', 'K': '#3a2a30', 'r': '#c82020', 'Y': '#8a6a20'})
    dibujo('cuerno_krampus', [
        '................', '..........ll....', '.........lLl....', '........lLl.....', '.......lLl......', '.......lLl......',
        '......lLLl......', '......lLLl......', '.....lLLLl......', '.....lLLLl......', '....lLLLLl......', '....lLLLLLl.....',
        '...lLLLLLLl.....', '...kkkkkkkk.....', '................', '................'], {'l': '#e8dcc0', 'L': '#b8a07a', 'k': '#3a2a20'})
    for h in ('huevo_muneco_malvado', 'huevo_reno', 'huevo_hombre_jengibre', 'huevo_krampus'):
        json.dump({'parent': 'minecraft:item/template_spawn_egg'}, open(os.path.join(A, 'models', 'item', h + '.json'), 'w'), indent=1)
    # la caja y el calendario usan el modelo de su bloque
    for b in ('caja_regalo', 'calendario_adviento'):
        json.dump({'parent': f'navidad:block/{b}'}, open(os.path.join(A, 'models', 'item', b + '.json'), 'w'), indent=1)
    print('ok')
