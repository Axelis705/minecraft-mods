"""Quita el parpadeo ("z-fighting") de los modelos: si dos elementos tienen una cara mirando al mismo lado en el mismo plano y se
enciman, la cara del elemento posterior se saca 0.01 hacia afuera. Uso: python3 zfix.py [carpeta_de_modelos]"""
import json, glob, itertools, os, sys

OUT = {'east': (0, 1), 'west': (0, -1), 'up': (1, 1), 'down': (1, -1), 'south': (2, 1), 'north': (2, -1)}


def faces(els):
    r = []
    for i, e in enumerate(els):
        if 'rotation' in e and e['rotation'].get('angle', 0) != 0:
            continue
        a, b = e['from'], e['to']
        for d in (e.get('faces') or {}):
            ax, sg = OUT[d]
            v = b[ax] if sg > 0 else a[ax]
            o = [k for k in range(3) if k != ax]
            r.append((i, d, ax, v, (a[o[0]], b[o[0]]), (a[o[1]], b[o[1]])))
    return r


def ov(p, q):
    return min(p[1], q[1]) - max(p[0], q[0]) > 1e-4


def fix(m):
    els = m.get('elements') or []
    n = 0
    for _ in range(20):
        hit = None
        for x, y in itertools.combinations(faces(els), 2):
            if x[0] != y[0] and x[1] == y[1] and abs(x[3] - y[3]) < 1e-4 and ov(x[4], y[4]) and ov(x[5], y[5]):
                hit = y
                break
        if not hit:
            break
        i, d = hit[0], hit[1]
        ax, sg = OUT[d]
        key = 'to' if sg > 0 else 'from'
        els[i][key][ax] = round(els[i][key][ax] + 0.01 * sg, 3)
        n += 1
    return n


def run(base):
    total = 0
    for f in glob.glob(os.path.join(base, '**/*.json'), recursive=True):
        try:
            m = json.load(open(f))
        except Exception:
            continue
        n = fix(m)
        if n:
            json.dump(m, open(f, 'w'), indent=1)
            total += n
            print(n, os.path.relpath(f, base))
    return total


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    base = sys.argv[1] if len(sys.argv) > 1 else os.path.join(here, '..', 'src/main/resources/assets/casino/models')
    print('caras corregidas:', run(base))
