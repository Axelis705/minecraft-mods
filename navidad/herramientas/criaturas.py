#!/usr/bin/env python3
"""Criaturas de Navidad de Axelis (modelo, textura y animaciones):
- muneco_malvado: muñeco de nieve con sombrero de copa, bufanda, ojos rojos y brazos de palo (lanza bolas de nieve).
- reno: hecho sobre la Bestia del Core (mismos huesos y animaciones), con pelaje café, pecho blanco y cornamenta.
- krampus: hecho sobre el Coloso del Core (mismos huesos y animaciones): peludo, negro, cuernos de cabra, lengua y cadenas.
Uso: python3 herramientas/criaturas.py"""
import os, re, sys

AQUI = os.path.dirname(os.path.abspath(__file__))
CORE = os.path.join(AQUI, '..', '..', 'axelis_core', 'herramientas')
sys.path.insert(0, CORE)
import criaturas as CC  # noqa: E402
from geo import Anim, Creature, save_anims  # noqa: E402

A = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'navidad')
S, C = CC.S, CC.C
mix, cyc = CC.mix, CC.cyc


def out(name):
    return os.path.join(A, 'geo', name + '.geo.json'), os.path.join(A, 'textures', 'entity', name + '.png')


def derivar(funcion, cambios):
    """Copia una función de las criaturas del Core, le aplica cambios de texto y la corre con las rutas de este mod."""
    src = open(os.path.join(CORE, 'criaturas.py'), encoding='utf-8').read()
    i = src.index(f'def {funcion}(')
    j = src.index('\ndef ', i + 10)
    j2 = src.find('\n# ====', i + 10)
    if j2 != -1 and j2 < j:
        j = j2
    cuerpo = src[i:j]
    for a, b in cambios:
        if a not in cuerpo:
            raise SystemExit(f'no encontré en {funcion}: {a[:60]}')
        cuerpo = cuerpo.replace(a, b)
    ns = dict(vars(CC))
    ns.update(out=out, A=A, Anim=Anim, Creature=Creature, save_anims=save_anims)
    exec(cuerpo, ns)
    return ns[funcion]


# ---------------------------------------------------------------- reno

ASTAS = '''
    asta = (196, 168, 120)
    def asta_p(face, x, y, w, h):
        return mix(asta, (150, 120, 80), 0.3) if face == "bottom" else asta
    for lado, s in (("r", -1), ("l", 1)):
        b = "antler_" + lado
        c.bone(b, [2.2 * s, 20, -11], "head", [-10, 0, 22 * -s])
        c.part(b, [2.2 * s - 0.6, 20, -11.6], [1.2, 6, 1.2], asta_p)
        c.part(b, [2.2 * s - 0.5, 24, -14.6], [1.0, 1.0, 3.2], asta_p)
        c.part(b, [2.2 * s - 0.5, 25, -13.2], [1.0, 2.4, 1.0], asta_p)
        c.part(b, [2.2 * s - 0.5, 26, -11.5], [1.0, 1.0, 3.0], asta_p)
        c.part(b, [2.2 * s - 0.5, 26.8, -9.0], [1.0, 2.6, 1.0], asta_p)
        c.part(b, [2.2 * s - 0.5 + 1.6 * s, 25.4, -11.0], [1.0, 2.2, 1.0], asta_p)
    c.bone("eyes"'''

RENO = [
    ('fur, dark, belly, mane, ember = (128, 124, 120), (74, 70, 70), (176, 170, 160), (52, 48, 52), (255, 136, 48)',
     'fur, dark, belly, mane, ember = (140, 96, 58), (86, 56, 34), (228, 214, 186), (236, 230, 216), (250, 248, 240)'),
    ('"geometry.axelis_core.bestia"', '"geometry.navidad.reno"'),
    ('return ember + (255,) if', 'return (26, 20, 18, 255) if'),
    ('    c.bone("eyes"', ASTAS),
    ('c.build(*out("bestia"))', 'c.build(*out("reno"))'),
    ('save_anims("bestia", an, os.path.join(A, "animations", "bestia.animation.json"))',
     'save_anims("reno", an, os.path.join(A, "animations", "reno.animation.json"))'),
    ('    a.sound(0.6, "minecraft:entity.wolf.howl")\n', ''),
    ('c.part("tail1", [-1.5, 13.5, 9], [3, 3, 4], tail(0.0))', 'c.part("tail1", [-1.3, 13.6, 9], [2.6, 2.6, 2.6], tail(0.8))'),
    ('c.part("tail2", [-1.4, 13.6, 13], [2.8, 2.8, 4], tail(0.2))', 'c.part("tail2", [-0.2, 14.8, 10.5], [0.4, 0.4, 0.4], tail(0.9))'),
    ('c.part("tail3", [-1.2, 13.8, 17], [2.4, 2.4, 3.5], tail(0.75))', 'c.part("tail3", [-0.2, 14.8, 10.5], [0.4, 0.4, 0.4], tail(0.9))'),
]

# ---------------------------------------------------------------- krampus

CUERNOS = '''
    cuerno = (214, 200, 168)
    def cuerno_p(face, x, y, w, h):
        return mix(cuerno, (120, 100, 80), (y % 3) / 3)
    def lengua(face, x, y, w, h):
        return (190, 40, 60)
    for lado, s in (("r", -1), ("l", 1)):
        c.part("head", [3 * s - 2, 42, -6.5], [4, 6, 4], cuerno_p, rotation=[-20, 0, 25 * s], pivot=[3 * s, 42, -4.5])
        c.part("head", [6 * s - 1.5, 46, -4], [3, 6, 3], cuerno_p, rotation=[-45, 0, 50 * s], pivot=[6 * s, 46, -2.5])
        c.part("head", [10 * s - 1, 48, 0], [2, 6, 2], cuerno_p, rotation=[-75, 0, 70 * s], pivot=[10 * s, 48, 1])
    c.part("head", [-0.9, 31.5, -9.4], [1.8, 4, 0.6], lengua, rotation=[15, 0, 0], pivot=[0, 35, -9])
    c.bone("eyes"'''

KRAMPUS = [
    ('st, st2, moss = (104, 102, 112), (72, 70, 82), (78, 112, 62)', 'st, st2, moss = (52, 38, 34), (26, 18, 18), (74, 54, 44)'),
    ('"geometry.axelis_core.coloso"', '"geometry.navidad.krampus"'),
    ('return (mix(cry, cry2, (x + y) / max(1, w + h)) + (255,))', 'return (mix((150, 150, 160), (90, 90, 100), (x + y) / max(1, w + h)) + (255,))'),
    ('return (mix(cry2, cry, d / 3) + (255,)) if d <= 3 else None', 'return (mix((255, 220, 90), (190, 130, 30), d / 3) + (255,)) if d <= 3 else None'),
    ('    c.bone("eyes"', CUERNOS),
    ('c.glow("core", "eyes", "crystal_r", "crystal_l")', 'c.glow("core", "eyes")'),
    ('geo_p, png_p = out("coloso")', 'geo_p, png_p = out("krampus")'),
    ('save_anims("coloso", an, os.path.join(A, "animations", "coloso.animation.json"))',
     'save_anims("krampus", an, os.path.join(A, "animations", "krampus.animation.json"))'),
    ('"minecraft:entity.ravager.roar"', '"navidad:krampus_ruge"'),
    ('return st2 if edge else mix(st, (140, 138, 150), 0.3)', 'return st2 if edge else mix(st, (96, 70, 60), 0.4)'),
]

# ---------------------------------------------------------------- muñeco de nieve malvado


def muneco():
    nieve, sombra, carbon, palo = (240, 244, 250), (200, 212, 228), (34, 32, 36), (110, 72, 40)
    c = Creature('geometry.navidad.muneco_malvado', 128, 64, seed=21)

    def bola(face, x, y, w, h):
        if face == 'bottom':
            return sombra
        k = ((x * 5 + y * 3) % 7) / 20
        return mix(nieve, sombra, k)

    def torso(face, x, y, w, h):
        if face == 'front' and x in (3, 4) and y in (1, 3, 5):
            return carbon
        return bola(face, x, y, w, h)

    def cara(face, x, y, w, h):
        if face == 'front':
            if y == 1 and x in (1, 2, 4, 5):
                return carbon  # cejas enojadas
            if y == 5 and x in (1, 2, 3, 4, 5) and (x + y) % 2 == 0:
                return carbon  # boca de carbón
            if y == 6 and x in (2, 4):
                return carbon
        return bola(face, x, y, w, h)

    def negro(face, x, y, w, h):
        return (30, 28, 34) if face != 'top' else (44, 42, 50)

    def cinta(face, x, y, w, h):
        return (170, 26, 36)

    def bufanda(face, x, y, w, h):
        return (196, 36, 44) if (x // 2 + y) % 2 == 0 else (40, 140, 60)

    def zanahoria(face, x, y, w, h):
        return (240, 120, 30) if (x + y) % 3 else (210, 96, 20)

    def palo_p(face, x, y, w, h):
        return palo

    def ojos(face, x, y, w, h):
        return (255, 40, 30, 255) if x in (0, 4) else None

    c.bone('root', [0, 0, 0])
    c.bone('base', [0, 4, 0], 'root')
    c.part('base', [-5, 0, -5], [10, 8, 10], bola)
    c.bone('torso', [0, 8, 0], 'base')
    c.part('torso', [-4, 8, -4], [8, 7, 8], torso)
    c.part('torso', [-4.4, 14, -4.4], [8.8, 1.6, 8.8], bufanda)
    c.part('torso', [1.5, 9.5, -4.8], [2, 4.5, 0.8], bufanda)
    c.bone('head', [0, 15, 0], 'torso')
    c.part('head', [-3.5, 15, -3.5], [7, 7, 7], cara)
    c.part('head', [-0.5, 17.5, -6.5], [1, 1, 3], zanahoria)
    c.bone('hat', [0, 22, 0], 'head', [0, 0, -8])
    c.part('hat', [-4.5, 22, -4.5], [9, 1, 9], negro)
    c.part('hat', [-3, 23, -3], [6, 5, 6], negro)
    c.part('hat', [-3.05, 23, -3.05], [6.1, 1, 6.1], cinta)
    c.bone('eyes', [0, 19, -3.55], 'head')
    c.plane('eyes', [-2.5, 19, -3.56], 5, 1, ojos)
    c.glow('eyes')
    for lado, s in (('r', -1), ('l', 1)):
        b = 'arm_' + lado
        c.bone(b, [4 * s, 13, 0], 'torso', [0, 0, -30 * s])
        x0 = 4 if s > 0 else -12
        c.part(b, [x0, 12.5, -0.5], [8, 1, 1], palo_p)
        c.part(b, [x0 + (6 if s > 0 else 0), 13.2, -0.5], [2, 1, 1], palo_p, rotation=[0, 0, 40 * s], pivot=[x0 + (6 if s > 0 else 2), 13, 0])
    c.build(*out('muneco_malvado'))

    an = []
    a = Anim('idle', 3.0, True)
    a.rot('head', cyc([[0, 0, 0], [0, 8, 2], [0, 0, 0], [0, -8, -2]], 3.0))
    a.rot('arm_r', cyc([[0, 0, 0], [0, 0, 8]], 3.0)).rot('arm_l', cyc([[0, 0, 0], [0, 0, -8]], 3.0, 0.5))
    a.rot('torso', cyc([[0, 0, 2], [0, 0, -2]], 3.0))
    an.append(a)
    L = 0.6
    a = Anim('walk', L, True)
    a.pos('base', cyc([[0, 0, 0], [0, 1.6, 0], [0, 0, 0], [0, 1.6, 0]], L))
    a.rot('base', cyc([[0, 0, 6], [0, 0, 0], [0, 0, -6], [0, 0, 0]], L))
    a.rot('torso', cyc([[0, 0, -4], [0, 0, 0], [0, 0, 4], [0, 0, 0]], L))
    a.rot('arm_r', cyc([[0, 0, 15], [0, 0, -5]], L)).rot('arm_l', cyc([[0, 0, -15], [0, 0, 5]], L, 0.5))
    an.append(a)
    a = Anim('attack', 0.8, False)
    a.rot('arm_r', {0: [0, 0, 0], 0.3: ([0, -60, 60], S), 0.42: ([0, 40, -20], 'easeOutQuad'), 0.8: ([0, 0, 0], S)})
    a.rot('torso', {0: [0, 0, 0], 0.3: ([0, -20, 0], S), 0.42: ([0, 15, 0], 'easeOutQuad'), 0.8: ([0, 0, 0], S)})
    a.rot('head', {0: [0, 0, 0], 0.3: ([-8, 0, 0], S), 0.5: ([6, 0, 0], S), 0.8: ([0, 0, 0], S)})
    a.event(0.42, 'lanzar').sound(0.42, 'minecraft:entity.snowball.throw')
    an.append(a)
    a = Anim('laugh', 1.2, False)
    for t0 in (0, 0.3, 0.6, 0.9):
        pass
    a.rot('head', {0: [0, 0, 0], 0.15: ([-15, 0, 0], S), 0.3: ([-5, 0, 0], S), 0.45: ([-15, 0, 0], S), 0.6: ([-5, 0, 0], S),
                   0.75: ([-15, 0, 0], S), 1.2: ([0, 0, 0], S)})
    a.pos('base', {0: [0, 0, 0], 0.15: ([0, 0.8, 0], S), 0.3: ([0, 0, 0], S), 0.45: ([0, 0.8, 0], S), 0.6: ([0, 0, 0], S), 1.2: [0, 0, 0]})
    an.append(a)
    a = Anim('hurt', 0.4, False)
    a.rot('torso', {0: [0, 0, 0], 0.08: ([8, 0, 6], 'easeOutQuad'), 0.4: ([0, 0, 0], S)})
    a.rot('head', {0: [0, 0, 0], 0.08: ([-12, 10, 0], 'easeOutQuad'), 0.4: ([0, 0, 0], S)})
    an.append(a)
    a = Anim('death', 1.4, 'hold')
    a.rot('head', {0: [0, 0, 0], 0.4: ([0, 0, 60], 'easeInQuad'), 1.4: ([0, 0, 90], S)})
    a.pos('head', {0: [0, 0, 0], 0.5: ([6, -8, 0], 'easeInQuad'), 0.7: ([8, -12, 0], S), 1.4: ([9, -13, 0], S)})
    a.pos('torso', {0: [0, 0, 0], 0.6: ([0, -3, 0], S), 1.4: ([0, -5, 0], S)})
    a.rot('arm_r', {0: [0, 0, 0], 0.6: ([0, 0, 60], S)}).rot('arm_l', {0: [0, 0, 0], 0.6: ([0, 0, -60], S)})
    a.rot('hat', {0: [0, 0, -8], 0.5: ([0, 0, 40], S), 1.4: ([0, 0, 80], S)})
    an.append(a)
    save_anims('muneco_malvado', an, os.path.join(A, 'animations', 'muneco_malvado.animation.json'))


if __name__ == '__main__':
    muneco()
    derivar('bestia', RENO)()
    derivar('coloso', KRAMPUS)(cry=(255, 190, 40))
    # el Krampus furioso (segunda fase): ojos rojos
    k2 = KRAMPUS + [('c.build(geo_p, png_p)', 'c.build(os.path.join(os.path.dirname(geo_p), "_tmp.geo.json"), png_p[:-4] + "_f2.png"); return')]
    k2 = [(a, b) for a, b in k2]
    derivar('coloso', k2)(cry=(255, 50, 30))
    os.remove(os.path.join(A, 'geo', '_tmp.geo.json'))
    print('ok')
