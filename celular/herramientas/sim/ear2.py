"""Pose de llamada nueva: el brazo sube hacia adelante (como el catalejo) con la mano delante de la mejilla, y el celular va de la
boca a la oreja, pegado al costado de la cabeza con la pantalla hacia la cara. Calcula la rotación y traslación del modelo."""
import itertools, math, sys
import numpy as np
import hand
from java_check import aim_java

def norm(v):
    v = np.array(v, float); return v / np.linalg.norm(v)

def solve(dirv, palm, C, U, N=(1, 0, 0), sc=0.45):
    arm = aim_java(True, *dirv, *palm)
    best = None
    for rx, ry, rz in itertools.product(range(-180, 180, 5), repeat=3):
        up, n, _ = hand.dirs(hand.chain(arm, (rx, ry, rz), (0, 0, 0), sc))
        s = up @ norm(U) + n @ norm(N)
        if best is None or s > best[0]:
            best = (s, (rx, ry, rz))
    rot = best[1]
    F = hand.chain(arm, (0, 0, 0), (0, 0, 0), 1)[:3, :3]
    c0 = hand.dirs(hand.chain(arm, rot, (0, 0, 0), sc))[2]
    tr = F.T @ (np.array(C, float) - c0)
    up, n, c = hand.dirs(hand.chain(arm, rot, tuple(tr), sc))
    return arm, rot, tr, up, n, c, best[0]

if __name__ == "__main__":
    piv = np.array([-5, 2, 0.])
    target_hand = np.array([float(a) for a in sys.argv[1:4]]) if len(sys.argv) > 3 else np.array([-4.5, -4.0, -5.0])
    d = target_hand - piv
    C = (-4.5, -4.2, -1.6)            # centro del celular: entre la boca y la oreja, pegado a la cabeza
    U = (0, -0.55, 1)                 # la parte de arriba hacia la oreja (atrás y un poco arriba)
    arm, rot, tr, up, n, c, s = solve(d, (1, 0, 0), C, U)
    hand_end = piv + norm(d) * 10
    print("brazo", np.round(arm, 3), "mano", hand_end.round(1))
    print("rot", rot, "tr", tr.round(2), "score", round(s, 3))
    print("centro", c.round(2), "arriba", up.round(2), "pantalla", n.round(2))
