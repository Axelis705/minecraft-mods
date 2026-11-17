"""La misma matemática de PhonePoses.aim (Java), para probarla en la simulación."""
import math, numpy as np, hand
def aim_java(right, dx, dy, dz, px, py, pz):
    dl = math.sqrt(dx*dx+dy*dy+dz*dz); dx/=dl; dy/=dl; dz/=dl
    dot = px*dx+py*dy+pz*dz; px-=dot*dx; py-=dot*dy; pz-=dot*dz
    pl = math.sqrt(px*px+py*py+pz*pz); s = 1 if right else -1
    c0x, c0y, c0z = s*px/pl, s*py/pl, s*pz/pl
    c2z = c0x*dy - c0y*dx
    return (math.atan2(dz, c2z), math.asin(max(-1, min(1, -c0z))), math.atan2(c0y, c0x))

if __name__ == "__main__":
    ROT=(0,-50,90); TR=(-0.5,-1,-1.2); SC=0.45
    EYE=np.array([0,-4.5,-4.])
    def rep(name, arm, right=True):
        up,n,c = hand.dirs(hand.chain(arm, ROT, TR, SC, right))
        e = EYE - c; e/=np.linalg.norm(e)
        print(f"{name:14s} centro {c.round(1)} pantalla·ojo {n@e:.2f} arriba {up.round(2)}")
    for right in (True, False):
        ax,ay,az = (-5 if right else 5), 2, 0
        bx = -1.2 if right else 1.2; by = 7; bz = -6.5
        rep(f"texto {'der' if right else 'izq'}", aim_java(right, bx-ax, by-ay, bz-az, -bx, -4.5-by, -4-bz), right)
        i = 1 if right else -1
        rep(f"oreja {'der' if right else 'izq'}", aim_java(right, 0.15*i, -0.85, -0.5, i, 0, 0), right)
        l = (0, 0, -1.)
        rep(f"luz {'der' if right else 'izq'}", aim_java(right, *l, 0.4*i, -1, 0), right)
        dx,dy,dz = l[0]+0.25*i, l[1]-0.35, l[2]; dl=math.sqrt(dx*dx+dy*dy+dz*dz)
        hx,hy,hz = ax+dx/dl*10, ay+dy/dl*10, az+dz/dl*10
        rep(f"foto {'der' if right else 'izq'}", aim_java(right, dx,dy,dz, -hx, -4.5-hy, -4-hz), right)
