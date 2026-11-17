"""Simulación de cómo Minecraft pone un ítem en la mano (tercera persona) para elegir la rotación del modelo."""
import numpy as np, math, itertools

def Rx(a):
    c,s=math.cos(a),math.sin(a); return np.array([[1,0,0,0],[0,c,-s,0],[0,s,c,0],[0,0,0,1.]])
def Ry(a):
    c,s=math.cos(a),math.sin(a); return np.array([[c,0,s,0],[0,1,0,0],[-s,0,c,0],[0,0,0,1.]])
def Rz(a):
    c,s=math.cos(a),math.sin(a); return np.array([[c,-s,0,0],[s,c,0,0],[0,0,1,0],[0,0,0,1.]])
def T(x,y,z):
    m=np.eye(4); m[:3,3]=[x,y,z]; return m
def S(s):
    m=np.eye(4); m[0,0]=m[1,1]=m[2,2]=s; return m

def chain(arm_rot, disp_rot, disp_tr, disp_sc, right=True):
    """Matriz desde el modelo del ítem (unidades de bloque, centrado) al espacio del modelo de la entidad (y hacia abajo, -z = frente)."""
    xr, yr, zr = arm_rot
    px = -5 if right else 5
    M = T(px/16, 2/16, 0) @ Rz(zr) @ Ry(yr) @ Rx(xr)          # ModelPart.translateAndRotate (ZYX)
    M = M @ Rx(math.radians(-90)) @ Ry(math.radians(180)) @ T((1 if right else -1)/16, 0.125, -0.625)
    rx, ry, rz = disp_rot
    tx, ty, tz = disp_tr
    if not right:
        tx = -tx; ry = -ry; rz = -rz
    M = M @ T(tx/16, ty/16, tz/16) @ Rx(math.radians(rx)) @ Ry(math.radians(ry)) @ Rz(math.radians(rz)) @ S(disp_sc)
    return M

def dirs(M):
    R = M[:3,:3]
    up = R @ np.array([0,1,0.]); n = R @ np.array([0,0,1.])
    c = M @ np.array([0,0,0,1.])
    return up/np.linalg.norm(up), n/np.linalg.norm(n), c[:3]*16   # en píxeles del modelo

if __name__ == "__main__":
    # buscar la rotación del modelo para que, con el brazo quieto, la parte de arriba del celular apunte hacia los dedos (+y, abajo)
    # y la pantalla mire hacia la palma (+x, hacia el cuerpo, mano derecha)
    best = []
    for rx, ry, rz in itertools.product(range(-180, 181, 90), repeat=3):
        up, n, c = dirs(chain((0,0,0), (rx,ry,rz), (0,0,0), 1))
        score = up @ np.array([0,1,0]) + n @ np.array([1,0,0])
        best.append((score, (rx,ry,rz)))
    best.sort(reverse=True)
    print(best[:6])
    for rot in [b[1] for b in best[:2]]:
        up, n, c = dirs(chain((0,0,0), rot, (0,0,0), 1))
        print(rot, "arriba", up.round(2), "pantalla", n.round(2), "centro px", c.round(2))

def hand_frame_rot():
    M = chain((0,0,0), (0,0,0), (0,0,0), 1)
    return M[:3,:3]

def solve_translation(target, rot=(0,-90,90)):
    c0 = dirs(chain((0,0,0), rot, (0,0,0), 1))[2]
    F = hand_frame_rot()
    return F.T @ (np.array(target) - c0)
