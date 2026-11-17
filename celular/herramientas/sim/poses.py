import numpy as np, math, hand
TR = (-0.5, -1.0, -1.2); ROT = (0, -90, 90); SC = 0.45

def norm(v): v = np.array(v, float); return v / np.linalg.norm(v)

def aim(dirv, palm, right=True):
    """Igual que PhonePoses.aim en Java: ángulos ZYX del brazo para que apunte a dirv con la palma hacia palm."""
    d = norm(dirv); p = np.array(palm, float); p = p - (p @ d) * d
    if np.linalg.norm(p) < 1e-4:
        p = np.array([1.0 if right else -1.0, 0, 0]); p = p - (p @ d) * d
    p = norm(p)
    c0 = p * (1 if right else -1); c1 = d; c2 = np.cross(c0, c1)
    y = math.asin(max(-1, min(1, -c0[2]))); x = math.atan2(c1[2], c2[2]); z = math.atan2(c0[1], c0[0])
    return (x, y, z)

def look(pitch, yaw):
    return np.array([-math.sin(yaw)*math.cos(pitch), math.sin(pitch), -math.cos(yaw)*math.cos(pitch)])

EYE = np.array([0, -4.5, -4.0])

def text(pitch, yaw, right=True):
    piv = np.array([-5 if right else 5, 2, 0.])
    P = np.array([-1.2 if right else 1.2, 7 + 3*math.sin(pitch), -6.5])
    return aim(P - piv, EYE - P, right), P

def report(name, arm, right=True):
    up, n, c = hand.dirs(hand.chain(arm, ROT, TR, SC, right))
    to_eye = norm(EYE - c)
    print(f"{name:10s} brazo {np.round(arm,2)} centro {c.round(1)} arriba {up.round(2)} pantalla {n.round(2)} pantalla·ojo {n@to_eye:.2f}")

if __name__ == "__main__":
    for pitch in (0, 0.5, -0.5):
        arm, P = text(pitch, 0)
        report(f"texto p{pitch}", arm)
    # linterna: brazo hacia donde mira, palma arriba y un poco adentro
    for pitch in (0, 0.6):
        arm = aim(look(pitch, 0), (0.5, -1, 0))
        report(f"luz p{pitch}", arm)
    # oreja
    arm = aim((0.15, -0.85, -0.5), (1, 0, 0))
    report("oreja", arm)
    # foto: dos manos hacia arriba y adelante, palmas hacia la cara
    for pitch in (0, -0.4):
        piv = np.array([-5, 2, 0.])
        d = norm(look(pitch, 0) + np.array([0.25, -0.35, 0]))
        P = piv + d * 10
        arm = aim(d, EYE - P)
        report(f"foto p{pitch}", arm)
