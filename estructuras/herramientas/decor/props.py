"""Catalogo de decoracion de las estructuras. Cada funcion devuelve un Prop (ver kit.py). Orientacion de los modelos: el FRENTE mira al NORTE (-z);
los de pared tienen la espalda pegada a z = 16; los colgantes cuelgan de y = 16."""
from kit import *  # noqa: F401,F403

# ---------------------------------------------------------------- paletas
BRASS = Ramp('#4a3210', '#7a5719', '#a77a26', '#d1a43e', '#efd070')
IRON = Ramp('#1c1c24', '#2c2c38', '#3f3f4e', '#585869', '#7a7a8c')
DWOOD = Ramp('#1e130b', '#2d1d10', '#3f2a17', '#553822', '#6b4a2e')
OAK = Ramp('#4a3320', '#614329', '#7a5733', '#94703f', '#ad8a50')
WAX = Ramp('#cfc6a4', '#e0d8b8', '#eee7cc', '#f7f2de')
SLATE = Ramp('#25252d', '#30303a', '#3c3c48', '#4a4a58', '#5b5b6b')
PURPLE = Ramp('#2a1346', '#3d1c63', '#552a85', '#7440b0', '#9a63d6')
AMETHYST = Ramp('#3a1a66', '#5a2ea0', '#7b4fd0', '#a37af0', '#d3b8ff')
SOUL = Ramp('#0f5d6b', '#17909c', '#2fc4cc', '#7ae8ec', '#d6ffff')
FIRE = Ramp('#7a1d08', '#b8400f', '#e8741c', '#ffb02e', '#ffe58a')
GREEN_GLOW = Ramp('#0e5a2a', '#1b8c3c', '#32c25a', '#7ae88c', '#d2ffd9')


def candelabro():
    p = Prop('candelabro', 'Candelabro de pie', 'Standing candelabra', 'floor', light=14,
             shape=[(3, 0, 3, 13, 2, 13), (6, 2, 6, 10, 16, 10), (1, 8, 6.5, 15, 16, 9.5)],
             particles=[{'type': 'flame', 'pos': [8, 16.3, 8], 'rate': 0.35}, {'type': 'flame', 'pos': [2.5, 15.3, 8], 'rate': 0.25},
                        {'type': 'flame', 'pos': [13.5, 15.3, 8], 'rate': 0.25}, {'type': 'small_flame', 'pos': [8, 16.5, 8], 'rate': 0.2}])
    br = metal(BRASS, 0.55, 3, 0.07)
    p.box(3, 0, 3, 13, 1, 13, br, bevel=.15, skip=['down'])
    p.box(4, 1, 4, 12, 2, 12, br, bevel=.15)
    p.box(5.5, 2, 5.5, 10.5, 3, 10.5, br, bevel=.15)
    p.box(7, 3, 7, 9, 11, 9, br, bevel=.12)
    p.box(6, 4.5, 6, 10, 6, 10, br, bevel=.15)
    p.box(6.5, 7.5, 6.5, 9.5, 8.5, 9.5, br, bevel=.15)
    p.box(2, 8.5, 7.25, 14, 9.5, 8.75, br, bevel=.12)
    for cx in (2.5, 13.5):
        p.box(cx - 1.5, 9.5, 6.5, cx + 1.5, 10.5, 9.5, br, bevel=.15)
    p.box(6, 11, 6, 10, 12, 10, br, bevel=.15)
    wax = cloth('#efe7cb', 0.0, 5)
    p.box(7, 12, 7, 9, 14.5, 9, wax)
    for cx in (2.5, 13.5):
        p.box(cx - 0.75, 10.5, 7.25, cx + 0.75, 13, 8.75, wax)
    fl = flame()
    for (cx, y0, h) in ((8, 14.5, 2.2), (2.5, 13, 2.0), (13.5, 13, 2.0)):
        p.box(cx - 1.2, y0, 8, cx + 1.2, y0 + h, 8, fl)
        p.box(cx, y0, 8 - 1.2, cx, y0 + h, 8 + 1.2, fl)
    return p


# ---------------------------------------------------------------- materiales propios
def chainmat():
    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        row = np.floor(Y) % 3
        t = np.where(row == 0, 0.22, np.where(row == 1, 0.62, 0.42)) + (rnd(X, Y, Z, 3) - 0.5) * 0.2
        return with_alpha(IRON(t))
    return f


def glassy(rim='#cfe8ef', hl=True):
    """Cristal de pixel art: casi todo transparente; solo los bordes de arriba/abajo y una raya de brillo son opacos,
    asi el liquido de dentro se ve (los frascos miden pocos pixeles)."""
    rc = np.array(hexc(rim), np.float32)
    hc = np.array([244, 253, 255], np.float32)

    def f(P):
        U, V, W, H = P['U'], P['V'], P['W'], P['H']
        out = np.zeros(U.shape + (4,), np.float32)
        out[..., :3] = rc
        if P['face'] in ('up', 'down'):
            ring = (U == 0) | (V == 0) | (U == W - 1) | (V == H - 1)
            out[..., 3] = np.where(ring, 255, 0)
            return out
        rims = (V == 0) | (V == H - 1)
        stripe = (U == 0) & (V > 0) & (V < H - 1) & ((V % 3) != 2) if hl else np.zeros_like(rims)
        out[..., 3] = np.where(rims | stripe, 255, 0)
        out[..., :3] = np.where(stripe[..., None], hc, rc)
        return out
    f.cutout = True
    return f


def globe_mat(seed=4):
    ocean = Ramp('#14407e', '#1b5aa6', '#2674c4', '#3a8fdc')
    land = Ramp('#2e6b2e', '#438a3a', '#6aa646', '#a7b05a', '#c2a46a')

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        dx, dy, dz = X - 8, Y - 9.5, Z - 8
        r = np.sqrt(dx * dx + dy * dy + dz * dz) + 1e-6
        nx, ny, nz = dx / r, dy / r, dz / r
        t = fbm(nx * 2.6 + 11, ny * 2.6, nz * 2.6, 1.0, P['seed'] + seed, 4)
        is_land = t > 0.52
        base = np.where(is_land[..., None], land(np.clip((t - 0.52) * 4.0, 0, 1)), ocean(np.clip(t * 1.3, 0, 1)))
        ice = np.abs(ny) > 0.86
        base = np.where(ice[..., None], np.array([232, 242, 250], np.float32), base)
        base = base * (0.9 + (rnd(X, Y, Z, 2) - 0.5) * 0.12)[..., None]
        return with_alpha(np.clip(base, 0, 255))
    return f


def coins_mat(seed=21):
    ramp = Ramp('#7a4e0c', '#a8741a', '#d49f28', '#f2c445', '#fff2a4')

    def f(P):
        X, Y, Z = P['X'], P['Y'], P['Z']
        t = noise3(X, Y, Z, 1.6, P['seed'] + seed) * 0.8 + (rnd(X, Y, Z, seed) - 0.5) * 0.5
        return with_alpha(ramp(t))
    return f


def book(p, x, y, z, w, d, h, color, turn=0):
    """Libro acostado: lomo a la izquierda, tapas de color y paginas por los otros tres lados."""
    cov = cloth(color, 0.0, 5)
    pg = parchment(0.5)
    p.box(x, y, z, x + w, y + 0.6, z + d, cov)
    p.box(x, y + h - 0.6, z, x + w, y + h, z + d, cov)
    p.box(x, y + 0.6, z, x + 0.8, y + h - 0.6, z + d, cov)
    p.box(x + 0.8, y + 0.6, z + 0.2, x + w - 0.1, y + h - 0.6, z + d - 0.2, pg)


# ---------------------------------------------------------------- araña de luces
def arana():
    p = Prop('arana', 'Araña de luces', 'Chandelier', 'hang', light=15, collide=False, sound='metal',
             shape=[(2, 3, 2, 14, 16, 14)],
             particles=[{'type': 'flame', 'pos': [14.4, 11.3, 8], 'rate': 0.12}, {'type': 'flame', 'pos': [1.6, 11.3, 8], 'rate': 0.12},
                        {'type': 'flame', 'pos': [8, 11.3, 14.4], 'rate': 0.12}, {'type': 'flame', 'pos': [8, 11.3, 1.6], 'rate': 0.12}])
    br = metal(BRASS, 0.55, 3, 0.07)
    ch = chainmat()
    p.box(7.5, 11, 7.5, 8.5, 16, 8.5, ch)
    p.box(6, 10, 6, 10, 11, 10, br, bevel=.15)
    p.box(5, 8, 5, 11, 10, 11, br, bevel=.15)
    p.box(6, 7, 6, 10, 8, 10, br, bevel=.15)
    p.box(7, 5, 7, 9, 7, 9, br, bevel=.15)
    p.box(7.5, 4, 7.5, 8.5, 5, 8.5, br)
    p.box(7.25, 2.2, 7.25, 8.75, 4, 8.75, gem('#8a5bf0'))
    wax = cloth('#efe7cb', 0.0, 5)
    fl = flame()

    def arm(cx, rot, r0, r1, ypos):
        """Brazo en +x (despues girado): barra, copa, vela y llama."""
        kw = {'rot': ('y', rot, (8, 8, 8))} if rot else {}
        p.box(8 + r0, ypos, 7.25, 8 + r1, ypos + 1, 8.75, br, bevel=.12, **kw)
        p.box(8 + r1 - 1.3, ypos + 1, 6.7, 8 + r1 + 1.3, ypos + 2, 9.3, br, bevel=.15, **kw)
        p.box(8 + r1 - 0.75, ypos + 2, 7.25, 8 + r1 + 0.75, ypos + 4.3, 8.75, wax, **kw)
        p.box(8 + r1 - 1.2, ypos + 4.3, 8, 8 + r1 + 1.2, ypos + 6.4, 8, fl, **kw)
        p.box(8 + r1, ypos + 4.3, 8 - 1.2, 8 + r1, ypos + 6.4, 8 + 1.2, fl, **kw)

    # cuatro brazos rectos (+x, -x con giro de 180 no existe, asi que se usan cajas a ambos lados)
    for sgn in (1, -1):
        for axis in ('x', 'z'):
            pass
    # +x y -x
    arm(0, 0, 2.5, 6.0, 8)
    # -x: caja espejo
    def arm_neg(r0, r1, ypos):
        p.box(8 - r1, ypos, 7.25, 8 - r0, ypos + 1, 8.75, br, bevel=.12)
        p.box(8 - r1 - 1.3, ypos + 1, 6.7, 8 - r1 + 1.3, ypos + 2, 9.3, br, bevel=.15)
        p.box(8 - r1 - 0.75, ypos + 2, 7.25, 8 - r1 + 0.75, ypos + 4.3, 8.75, wax)
        p.box(8 - r1 - 1.2, ypos + 4.3, 8, 8 - r1 + 1.2, ypos + 6.4, 8, fl)
        p.box(8 - r1, ypos + 4.3, 8 - 1.2, 8 - r1, ypos + 6.4, 8 + 1.2, fl)
    arm_neg(2.5, 6.0, 8)

    def arm_z(sgn, r0, r1, ypos):
        a, b = (8 + r0, 8 + r1) if sgn > 0 else (8 - r1, 8 - r0)
        cz = 8 + sgn * r1
        p.box(7.25, ypos, a, 8.75, ypos + 1, b, br, bevel=.12)
        p.box(6.7, ypos + 1, cz - 1.3, 9.3, ypos + 2, cz + 1.3, br, bevel=.15)
        p.box(7.25, ypos + 2, cz - 0.75, 8.75, ypos + 4.3, cz + 0.75, wax)
        p.box(8, ypos + 4.3, cz - 1.2, 8, ypos + 6.4, cz + 1.2, fl)
        p.box(8 - 1.2, ypos + 4.3, cz, 8 + 1.2, ypos + 6.4, cz, fl)
    arm_z(1, 2.5, 6.0, 8)
    arm_z(-1, 2.5, 6.0, 8)
    # cuatro brazos en diagonal, mas cortos y mas bajos
    for sgn, ang in ((1, 45), (1, -45)):
        arm(0, ang, 2.5, 4.6, 6.5)
    for ang in (45, -45):
        r1 = 4.6
        p.box(8 - r1, 6.5, 7.25, 8 - 2.5, 7.5, 8.75, br, bevel=.12, rot=('y', ang, (8, 8, 8)))
        p.box(8 - r1 - 1.3, 7.5, 6.7, 8 - r1 + 1.3, 8.5, 9.3, br, bevel=.15, rot=('y', ang, (8, 8, 8)))
        p.box(8 - r1 - 0.75, 8.5, 7.25, 8 - r1 + 0.75, 10.8, 8.75, wax, rot=('y', ang, (8, 8, 8)))
        p.box(8 - r1 - 1.2, 10.8, 8, 8 - r1 + 1.2, 12.9, 8, fl, rot=('y', ang, (8, 8, 8)))
        p.box(8 - r1, 10.8, 8 - 1.2, 8 - r1, 12.9, 8 + 1.2, fl, rot=('y', ang, (8, 8, 8)))
    return p


# ---------------------------------------------------------------- globo terráqueo
def globo():
    p = Prop('globo', 'Globo terráqueo', 'Terrestrial globe', 'floor', light=0, shape=[(3, 0, 3, 13, 15, 13)])
    wd = wood(DWOOD, 'h', 2.0, 0, 5)
    br = metal(BRASS, 0.55, 3, 0.07)
    p.box(3, 0, 3, 13, 1, 13, wd, bevel=.15, skip=['down'])
    p.box(4, 1, 4, 12, 2, 12, wd, bevel=.15)
    p.box(7, 2, 7, 9, 4.5, 9, br, bevel=.12)
    p.box(5.5, 4.5, 6.5, 10.5, 5.2, 9.5, br, bevel=.15)
    gm = globe_mat()
    cy, R = 10.0, 4.9
    for k in range(10):
        y0 = 5.0 + k
        dy = (y0 + 0.5) - cy
        if abs(dy) >= R:
            continue
        hw = math.sqrt(R * R - dy * dy)
        hw = max(1.0, round(hw * 2) / 2)
        p.box(8 - hw, y0, 8 - hw, 8 + hw, y0 + 1, 8 + hw, gm)
    # meridiano de laton (arco en el plano x-y, delante y detras de la esfera no cabe: se pone a un lado)
    for ang in range(-60, 241, 20):
        a = math.radians(ang)
        cx, cyy = 8 + 6.3 * math.cos(a), cy + 6.3 * math.sin(a)
        p.box(cx - 0.6, cyy - 0.6, 7.4, cx + 0.6, cyy + 0.6, 8.6, br)
    p.box(7.6, 14.8, 7.6, 8.4, 16, 8.4, br)
    return p


# ---------------------------------------------------------------- telescopio
def telescopio():
    p = Prop('telescopio', 'Telescopio de latón', 'Brass telescope', 'floor', light=0, shape=[(3, 0, 3, 13, 16, 13)], sound='metal')
    br = metal(BRASS, 0.55, 3, 0.07)
    dk = metal(IRON, 0.5, 5, 0.04)
    wd = wood(DWOOD, 'v', 2.0, 0, 5)
    # trípode
    p.box(7, 0.5, 7, 9, 8, 9, wd, bevel=.1)
    p.box(5, 0, 8.5, 6.2, 1.2, 11.5, wd)
    for sgn in (1, -1):
        p.box(8 - 0.75 + sgn * 0, 0, 8, 8 + 0.75, 7.5, 9.5, wd, rot=('z', 22.5 * sgn, (8, 7.5, 8.75)))
    p.box(7.25, 0, 8.25, 8.75, 7.5, 9.75, wd, rot=('x', -22.5, (8, 7.5, 9)))
    # horquilla y tubo
    p.box(5.5, 8, 7, 6.5, 11, 9, br, bevel=.15)
    p.box(9.5, 8, 7, 10.5, 11, 9, br, bevel=.15)
    p.box(6, 7.5, 6.5, 10, 8.5, 9.5, br, bevel=.15)
    tube = ('x', 45, (8, 9.5, 8))
    tube2 = tube
    p.box(6.8, 8.3, 0.5, 9.2, 10.7, 15, dk, rot=tube2, bevel=.1)
    p.box(6.4, 7.9, 3.2, 9.6, 11.1, 4.2, br, rot=tube2, bevel=.15)
    p.box(6.4, 7.9, 8.5, 9.6, 11.1, 9.5, br, rot=tube2, bevel=.15)
    p.box(6.2, 7.7, 0.3, 9.8, 11.3, 1.6, br, rot=tube2, bevel=.15)
    p.box(7.0, 8.5, 0.15, 9.0, 10.5, 0.4, glow(SOUL), rot=tube2)
    p.box(7.3, 8.8, 15, 8.7, 10.2, 16.6, br, rot=tube2)
    return p


# ---------------------------------------------------------------- mesa de alquimia
def mesa_alquimia():
    p = Prop('mesa_alquimia', 'Mesa de alquimia', 'Alchemy table', 'floor', light=9, shape=[(0, 0, 0, 16, 12.5, 16)],
             particles=[{'type': 'witch', 'pos': [4.5, 16, 4.5], 'rate': 0.08}, {'type': 'happy_villager', 'pos': [11.5, 16.5, 4.5], 'rate': 0.05}])
    wd = wood(DWOOD, 'h', 2.0, 0, 5)
    ok = wood(OAK, 'h', 2.2, 5, 7)
    br = metal(BRASS, 0.55, 3, 0.07)
    for x, z in ((0.5, 0.5), (12.5, 0.5), (0.5, 12.5), (12.5, 12.5)):
        p.box(x, 0, z, x + 3, 11, z + 3, wd, bevel=.12, skip=['down'])
    p.box(0.5, 8, 3.5, 15.5, 10.5, 12.5, wd, skip=['up', 'down'])
    p.box(0.5, 3, 0.5, 15.5, 4, 15.5, wd, bevel=.1)
    p.box(0, 11, 0, 16, 12.5, 16, ok, bevel=.12)
    # matraz grande (bulbo con liquido verde)
    p.box(3, 12.5, 3, 6, 15, 6, glassy('#bfe3e6'))
    p.box(3.4, 12.5, 3.4, 5.6, 14.2, 5.6, glow(GREEN_GLOW))
    p.box(4, 15, 4, 5, 16.5, 5, glassy('#bfe3e6'))
    p.box(3.8, 16.5, 3.8, 5.2, 17, 5.2, wd)
    # probeta alta con liquido morado
    p.box(10, 12.5, 3.5, 12, 17, 5.5, glassy('#c8d0f0'))
    p.box(10.4, 12.5, 3.9, 11.6, 15.2, 5.1, glow(Ramp('#3a1470', '#5a2ca8', '#8a55e0', '#c096ff', '#f0e0ff')))
    # mortero con maja
    p.box(10, 12.5, 10, 14, 14.5, 14, stone(SLATE), bevel=.12)
    p.box(11, 14.5, 11, 13, 15, 13, stone(Ramp('#2a2a32', '#3a3a46')))
    p.box(11.7, 14.5, 11.5, 12.3, 17, 12.1, wd, rot=('z', 22.5, (12, 14.5, 11.8)))
    # libro de recetas y vela
    book(p, 1.5, 12.5, 9.5, 6, 5, 1.6, '#4d2a7a')
    p.box(7.5, 12.5, 7.5, 8.7, 14.5, 8.7, cloth('#efe7cb', 0.0, 5))
    p.box(7.1, 14.5, 8.1, 9.1, 16.3, 8.1, flame())
    p.box(8.1, 14.5, 7.1, 8.1, 16.3, 9.1, flame())
    return p


def all_props():
    import props_b
    return [candelabro(), arana(), globo(), telescopio(), mesa_alquimia()] + props_b.all_b()
