import numpy as np, math, hand, itertools
from java_check import aim_java
EAR_DIR = (-0.35, -1.0, -0.2)
arm = aim_java(True, *EAR_DIR, 1, 0, 0)
E = np.array([-4.7, -4.4, -0.2])
best = None
for rx, ry, rz in itertools.product(range(-180, 180, 10), repeat=3):
    up, n, c = hand.dirs(hand.chain(arm, (rx, ry, rz), (0, 0, 0), 0.45))
    s = -up[1] + n[0] - 0.4 * abs(n[2])
    if best is None or s > best[0]:
        best = (s, (rx, ry, rz), up.round(2), n.round(2))
print("rotación", best)
rot = best[1]
# traslación para que el centro quede en la oreja
M0 = hand.chain(arm, (0, 0, 0), (0, 0, 0), 1)
F = M0[:3, :3]
c0 = hand.dirs(hand.chain(arm, rot, (0, 0, 0), 0.45))[2]
tr = F.T @ (E - c0)
print("traslación", tr.round(2))
up, n, c = hand.dirs(hand.chain(arm, rot, tuple(tr), 0.45))
print("centro", c.round(2), "arriba", up.round(2), "pantalla", n.round(2), "brazo", np.round(arm, 2))
up, n, c = hand.dirs(hand.chain(aim_java(False, -EAR_DIR[0], EAR_DIR[1], EAR_DIR[2], -1, 0, 0), rot, tuple(tr), 0.45, right=False))
print("izquierda: centro", c.round(2), "pantalla", n.round(2))
