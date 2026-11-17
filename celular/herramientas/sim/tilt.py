import numpy as np, math, hand, poses
from poses import aim, look, norm, EYE

def euler_xyz(R):
    # R = Rx·Ry·Rz  -> ángulos (como ItemTransform.rotationXYZ)
    y = math.asin(max(-1, min(1, R[0, 2])))
    x = math.atan2(-R[1, 2], R[2, 2])
    z = math.atan2(-R[0, 1], R[0, 0])
    return np.degrees([x, y, z])

base = (hand.Rx(0) @ hand.Ry(math.radians(-90)) @ hand.Rz(math.radians(90)))[:3, :3]

def pose_list():
    out = []
    for pitch in (0, 0.5, -0.4):
        arm, P = poses.text(pitch, 0)
        out.append(("texto", arm))
    out.append(("oreja", aim((0.15, -0.85, -0.5), (1, 0, 0))))
    for pitch in (0, -0.4):
        piv = np.array([-5, 2, 0.]); d = norm(look(pitch, 0) + np.array([0.25, -0.35, 0])); P = piv + d * 10
        out.append(("foto", aim(d, EYE - P)))
    return out

best = None
for a in range(-70, 71, 5):
    R = base @ hand.Rx(math.radians(a))[:3, :3]
    rot = tuple(euler_xyz(R))
    sc = 0; info = []
    for name, arm in pose_list():
        up, n, c = hand.dirs(hand.chain(arm, rot, poses.TR, poses.SC))
        d = n @ norm(EYE - c)
        sc += d - 0.3 * max(0, up[1])     # pantalla hacia los ojos y la parte de arriba no hacia abajo
        info.append(f"{name}:{d:.2f}/{up[1]:.2f}")
    if best is None or sc > best[0]:
        best = (sc, a, rot, info)
    print(a, np.round(rot, 1), round(sc, 2), " ".join(info))
print("MEJOR", best)
